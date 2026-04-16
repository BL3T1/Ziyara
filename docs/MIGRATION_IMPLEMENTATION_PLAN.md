# Ziyara – Migration Implementation Plan

This document is an **implementation plan** for creating and updating database migrations so the database stays in sync with the application (JPA entities and features). It builds on:

- **Existing migrations:** `docs/DATABASE_MIGRATIONS_REPORT.md`
- **Gaps identified:** `docs/MISSING_MIGRATIONS_REPORT.md`

---

## 1. Goals and Scope

| Goal | Description |
|------|-------------|
| **Align DB with JPA** | All entities using `@Enumerated(EnumType.STRING)` must have corresponding VARCHAR columns; missing entity columns must exist in the DB. |
| **Single apply path** | One script (`apply-all.ps1`) applies base schema + all migrations (001–012) + seed so any environment can be built consistently. |
| **Safe application** | New migrations are idempotent where possible; enum→VARCHAR steps are applied in a defined order and documented for re-run behavior. |
| **Maintainability** | Clear process for adding future migrations and updating the apply script. |

**Out of scope (for this plan):** Changing `schema.sql` to match migrations (e.g. defining enums as VARCHAR from the start), introducing Flyway/Liquibase, or modifying JPA entities except where a small fix is needed for a missing column.

---

## 2. Conventions and Prerequisites

### 2.1 Conventions

- **Naming:** Migrations are `NNN_short_snake_name.sql` (e.g. `011_hibernate_enum_compat_remaining.sql`).
- **Order:** Run in numerical order; dependencies are documented in each file header.
- **Style:** Follow existing migrations: header comment block, one logical change per file (or one “theme”), use `ADD COLUMN IF NOT EXISTS` and conditional logic where it avoids duplicate-run failures.
- **Location:** All migration files live under `database/migrations/`.

### 2.2 Prerequisites

- PostgreSQL 15+ client (`psql`) or Docker with Postgres image.
- Access to the target database (e.g. `ziyarah` on localhost or CI).
- Backup of any existing database before applying 011/012 on non-fresh instances.
- Optional: a small test database to validate “fresh run” and “existing DB run” paths.

### 2.3 Environment Variables (for apply script)

- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` (optional; defaults: localhost, 5432, ziyarah, ziyarah_user).
- `PGPASSWORD` or `.pgpass` for authentication.

---

## 3. Implementation Phases

### Phase 1: Create new migration files

#### 1.1 Create `011_hibernate_enum_compat_remaining.sql`

**Purpose:** Convert remaining enum columns to `VARCHAR(50)` for Hibernate `EnumType.STRING`.

**Tables and columns:**

| Table | Column(s) |
|-------|-----------|
| roles | level |
| bookings | status |
| complaints | priority, status |
| internal_tickets | type, priority, status |
| refunds | status |
| notifications | type, channel, status |
| taxi_bookings | vehicle_type, status |
| services | type, status |

**Implementation steps:**

1. Create `database/migrations/011_hibernate_enum_compat_remaining.sql`.
2. Add header comment: run-after (schema + 001–010), purpose, “run once per environment” note.
3. For each table, add:
   - `ALTER TABLE <table> ALTER COLUMN <col> TYPE varchar(50) USING <col>::text;`
   - Use separate statements for multiple columns (e.g. complaints: two ALTER COLUMN lines or two ALTER TABLE statements).
4. **Idempotency:** These ALTERs are **not** idempotent once the column is already VARCHAR. Options:
   - **A (recommended):** Leave as one-time ALTERs; document in the file and in this plan that 011 must only be run once per DB. Rely on apply order (011 runs after schema + 001–010) so fresh DBs get it once.
   - **B:** Wrap each in a `DO $$ ... IF data_type = 'USER-DEFINED' ... THEN ALTER ... END IF; $$` using `information_schema.columns`. Use if you need to support re-running the apply script on DBs that already had 011.
5. Save the file and add it to version control.

**Validation:** On a DB that has schema + 001–010 applied, run 011 once; verify no errors and that the listed columns are `character varying(50)`.

---

#### 1.2 Create `012_entity_columns.sql`

**Purpose:** Add columns required by JPA entities but missing from the base schema and existing migrations.

**Changes:**

| Target | Change |
|--------|--------|
| departments | Add `manager_id UUID REFERENCES users(id)` |
| notifications | Add `message TEXT`, `template_name VARCHAR(255)`, `updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`; backfill `message` from `content` where applicable |
| refunds | Add `currency VARCHAR(3) DEFAULT 'USD'`, `transaction_reference VARCHAR(100)` |

**Implementation steps:**

1. Create `database/migrations/012_entity_columns.sql`.
2. Add header comment: run-after (011), purpose.
3. **departments:**
   - `ALTER TABLE departments ADD COLUMN IF NOT EXISTS manager_id UUID REFERENCES users(id);`
4. **notifications:**
   - `ALTER TABLE notifications ADD COLUMN IF NOT EXISTS message TEXT;`
   - `ALTER TABLE notifications ADD COLUMN IF NOT EXISTS template_name VARCHAR(255);`
   - `ALTER TABLE notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;`
   - `UPDATE notifications SET message = content WHERE message IS NULL AND content IS NOT NULL;` (so existing rows get message from content)
   - Optional: add trigger to set `updated_at` on UPDATE (match other tables).
5. **refunds:**
   - `ALTER TABLE refunds ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'USD';`
   - `ALTER TABLE refunds ADD COLUMN IF NOT EXISTS transaction_reference VARCHAR(100);`
6. All steps use `IF NOT EXISTS` / non-destructive UPDATE so the migration is idempotent for re-runs.
7. Save and commit.

**Validation:** Run 012 on a DB that has 011 applied; verify new columns exist and, for notifications, that existing rows have `message` populated from `content` where applicable.

---

### Phase 2: Update the apply script

**Purpose:** Ensure one full run of the apply script applies schema + 001–012 + seed, including 009 and 010.

**File:** `database/apply-all.ps1`.

**Implementation steps:**

1. **Insert 009 and 010** after 008 and before seed:
   - After the block that runs 008, add:
     - Step for migration 009: apply `migrations\009_auth_tokens_otp.sql`.
     - Step for migration 010: apply `migrations\010_ar_columns_i18n.sql`.
2. **Add 011 and 012** after 010 and before seed:
   - Step for migration 011: apply `migrations\011_hibernate_enum_compat_remaining.sql`.
   - Step for migration 012: apply `migrations\012_entity_columns.sql`.
3. **Adjust step labels/counts** so the script reports the correct step numbers (e.g. [1/14] through [14/14] if you count schema + 12 migrations + seed).
4. **Keep 003 behavior:** Leave the try/catch or special handling for 003 unchanged (it may have partial-apply behavior).
5. **Optional:** Add a short comment at the top of the script listing the migration order (001–012) and pointing to `docs/DATABASE_MIGRATIONS_REPORT.md` and `docs/MISSING_MIGRATIONS_REPORT.md`.
6. Test the script on a **fresh** database (drop/create or use a new DB name) and confirm:
   - No errors.
   - All tables and columns from schema + 001–012 exist.
   - Seed data loads; test login works (e.g. admin@ziyarah.com).

---

### Phase 3: Documentation and rollout

#### 3.1 Update documentation

| Document | Update |
|----------|--------|
| `docs/DATABASE_MIGRATIONS_REPORT.md` | Add 011 and 012 to the summary table and “Per-migration detail”; update “Execution order” and “How to apply” to include 009–012; bump “Last updated”. |
| `docs/MISSING_MIGRATIONS_REPORT.md` | Add a short “Implementation status” section: 011 and 012 created, apply-all.ps1 updated; optionally mark the missing-items sections as “Addressed by 011/012”. |
| `README` or `docs/` setup guide | Ensure “Database setup” describes running `database/apply-all.ps1` (and env vars) as the single path; remove or qualify any “run 010 manually” instructions. |

#### 3.2 Rollout

- **New / greenfield environments:** Run `apply-all.ps1` once; no extra steps.
- **Existing environments that already have 001–008 (and possibly 009/010):**
  - Backup the database.
  - Run 009 and 010 if not already applied.
  - Run 011 **once** (enum→VARCHAR; not idempotent if re-run).
  - Run 012 (idempotent).
  - Restart the application and smoke-test (login, key flows).
- **CI/CD:** If CI creates a fresh DB for tests, use `apply-all.ps1` as the single source of truth; no separate 009/010 scripts needed in CI.

---

## 4. Testing Strategy

| Scenario | Action | Expected |
|----------|--------|----------|
| **Fresh DB** | Run `apply-all.ps1` from scratch (schema + 001–012 + seed). | All steps succeed; DB matches entity expectations; app starts and login works. |
| **Existing DB (has 001–008 only)** | Apply 009, 010, 011, 012 in order. | No errors; new columns and types present; app works. |
| **Re-run 012** | Run 012 again on same DB. | No errors (idempotent). |
| **Re-run 011** | Run 011 again after it was already applied. | May error (column already VARCHAR). Mitigate by documenting “run 011 once” or by implementing optional idempotent checks in 011. |

Optional: add a small script or checklist that queries `information_schema.columns` to verify key columns (e.g. `bookings.status`, `notifications.message`) exist and have the expected types.

---

## 5. Future Migrations: Process

When adding a new migration later:

1. **Create** `database/migrations/NNN_descriptive_name.sql` (next number after 012).
2. **Implement** using the same conventions: header comment, run-after, idempotent where possible (e.g. `ADD COLUMN IF NOT EXISTS`).
3. **Append** to `apply-all.ps1` a new step that runs this file (before seed).
4. **Update** `docs/DATABASE_MIGRATIONS_REPORT.md` (summary table + detail for NNN).
5. **Test** on a fresh DB and, if relevant, on an existing DB that has 012 already.

---

## 6. Checklist Summary

Use this as a quick checklist when executing the plan:

- [ ] Create `011_hibernate_enum_compat_remaining.sql` and add all enum→VARCHAR ALTERs.
- [ ] Create `012_entity_columns.sql` (departments.manager_id; notifications message/template_name/updated_at + backfill; refunds currency/transaction_reference).
- [ ] Update `apply-all.ps1` to run 009, 010, 011, 012 (and adjust step labels).
- [ ] Test apply script on a fresh database.
- [ ] Test 011+012 on an existing DB (with backup) if applicable.
- [ ] Update `docs/DATABASE_MIGRATIONS_REPORT.md` and `docs/MISSING_MIGRATIONS_REPORT.md`.
- [ ] Update any README or setup docs that reference manual 009/010 steps.
- [ ] Commit migration files and script/docs changes.

---

## 7. References

- **Migrations report:** `docs/DATABASE_MIGRATIONS_REPORT.md`
- **Missing migrations:** `docs/MISSING_MIGRATIONS_REPORT.md`
- **Base schema:** `database/schema.sql`
- **Apply script:** `database/apply-all.ps1`
- **Auth fallback:** `AuthTablesMigration.java` (still valid; 009 in apply script ensures tables exist even without app run)

---

*Plan created 2026-03-11. Update this document when adding new phases or changing the migration process.*
