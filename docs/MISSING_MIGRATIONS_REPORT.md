# Ziyara – Missing Migrations Report

This report lists **database migrations that were missing** (and have been addressed by 011/012 and apply script updates). It is based on a comparison of:

- **Existing migrations:** `database/migrations/001_plans_schema_extensions.sql` through `010_ar_columns_i18n.sql`
- **Base schema:** `database/schema.sql`
- **JPA entities:** `backend/.../infrastructure/persistence/entity/*.java`

Entities use `@Enumerated(EnumType.STRING)` for enums, so the database must store these as `VARCHAR`, not PostgreSQL enums. Several tables still use enum types in the schema and have no migration converting them to VARCHAR. Other gaps are missing columns that entities expect.

---

## Implementation status (2026-03-11)

| Item | Status |
|------|--------|
| **011_hibernate_enum_compat_remaining.sql** | Created – converts remaining enum columns to VARCHAR for roles, bookings, complaints, internal_tickets, refunds, notifications, taxi_bookings, services. |
| **012_entity_columns.sql** | Created – adds departments.manager_id; notifications (message, template_name, updated_at + backfill); refunds (currency, transaction_reference). |
| **apply-all.ps1** | Updated – now runs 009, 010, 011, 012 (steps 10–13) before seed. Full run: schema + 001–012 + seed. |
| **DATABASE_MIGRATIONS_REPORT.md** | Updated – includes 011, 012 and revised apply instructions. |

The sections below describe the gaps that were identified and how 011/012 address them.

---

## 1. Executive Summary

| Category | Count | Risk |
|----------|-------|------|
| **Enum → VARCHAR (Hibernate compat)** | 9 tables | High – runtime errors or insert/read failures |
| **Missing columns (entity vs schema)** | 4 tables | Medium – null/column errors or missing features |
| **Apply script gaps** | 2 migrations | Low – 009/010 not in apply-all.ps1 |

**Recommendation:** Add migrations for all enum→VARCHAR conversions and missing columns, then run 009 and 010 from the apply script or document them clearly.

---

## 2. Missing Enum → VARCHAR Migrations

JPA uses `EnumType.STRING`; PostgreSQL enums require explicit casting. The pattern used elsewhere (e.g. 004, 005, 006, 007, 008) is to alter the column type to `VARCHAR(n) USING column_name::text`. The following tables still use enum types in the schema and have **no** such migration.

| # | Table | Column(s) | Schema type | Entity / usage |
|---|--------|-----------|-------------|----------------|
| 1 | **roles** | `level` | `employee_level_enum` | `RoleJpaEntity` – `@Enumerated(EnumType.STRING)` |
| 2 | **bookings** | `status` | `booking_status_enum` | `BookingJpaEntity` – `@Enumerated(EnumType.STRING)` |
| 3 | **complaints** | `priority`, `status` | `complaint_priority_enum`, `complaint_status_enum` | `ComplaintJpaEntity` – both STRING |
| 4 | **internal_tickets** | `type`, `priority`, `status` | `ticket_type_enum`, `ticket_priority_enum`, `ticket_status_enum` | `InternalTicketJpaEntity` – all STRING |
| 5 | **refunds** | `status` | `refund_status_enum` | `RefundJpaEntity` – STRING (includes e.g. REQUESTED) |
| 6 | **notifications** | `type`, `channel`, `status` | `notification_type_enum`, `notification_channel_enum`, `notification_status_enum` | `NotificationJpaEntity` – all STRING |
| 7 | **taxi_bookings** | `vehicle_type`, `status` | `vehicle_type_enum`, `taxi_status_enum` | `TaxiBookingJpaEntity` – both STRING |
| 8 | **services** | `type`, `status` | `service_type_enum`, `service_status_enum` | `ServiceJpaEntity` – both STRING |

**Suggested migration file(s):** e.g. `011_hibernate_enum_compat_remaining.sql` that runs:

- `ALTER TABLE roles ALTER COLUMN level TYPE varchar(50) USING level::text;`
- `ALTER TABLE bookings ALTER COLUMN status TYPE varchar(50) USING status::text;`
- `ALTER TABLE complaints ALTER COLUMN priority TYPE varchar(50) USING priority::text;, ALTER COLUMN status TYPE varchar(50) USING status::text;`
- Same pattern for `internal_tickets`, `refunds`, `notifications`, `taxi_bookings`, `services`.

Run only once per environment; re-running after type is already VARCHAR can cause errors.

---

## 3. Missing Column Migrations

These are columns that JPA entities expect but that are **not** present in `schema.sql` and **not** added by any existing migration.

### 3.1 departments.manager_id

- **Entity:** `DepartmentJpaEntity` has `@Column(name = "manager_id")`.
- **Schema:** `departments` has no `manager_id`.
- **Migration:** `ALTER TABLE departments ADD COLUMN IF NOT EXISTS manager_id UUID REFERENCES users(id);`

### 3.2 notifications – message, template_name, updated_at

- **Entity:** `NotificationJpaEntity` has `message`, `template_name`, `updated_at`.
- **Schema:** `notifications` has `content` (not `message`), no `template_name`, no `updated_at`.
- **Options:**
  - **Option A:** Add `message` and `template_name`, add `updated_at`; keep `content` for backward compatibility or migrate data into `message` and deprecate `content`.
  - **Option B:** Add `message` as alias/copy of `content`, add `template_name` and `updated_at`, then later drop `content` if desired.
- **Suggested migration:**  
  - `ALTER TABLE notifications ADD COLUMN IF NOT EXISTS message TEXT;`  
  - `UPDATE notifications SET message = content WHERE message IS NULL AND content IS NOT NULL;` (if keeping both)  
  - `ALTER TABLE notifications ADD COLUMN IF NOT EXISTS template_name VARCHAR(255);`  
  - `ALTER TABLE notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;`  
  - Optionally add trigger for `updated_at` to match other tables.

### 3.3 refunds – currency, transaction_reference

- **Entity:** `RefundJpaEntity` has `currency`, `transactionReference`.
- **Schema:** `refunds` has no `currency` or `transaction_reference` (it has `gateway_refund_id`).
- **Migration:**  
  - `ALTER TABLE refunds ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'USD';`  
  - `ALTER TABLE refunds ADD COLUMN IF NOT EXISTS transaction_reference VARCHAR(100);`  
  (Entity does not map `booking_id`, `penalty_amount`, `net_refund_amount`, `processed_by`, `gateway_refund_id` – those are schema-only; no migration required unless the entity is extended.)

---

## 4. Apply Script Gaps

These migrations **exist** but are **not** run by `database/apply-all.ps1`:

| Migration | Purpose | Current situation |
|-----------|---------|-------------------|
| **009** | Auth tables (`password_reset_tokens`, `otp_verification`) | Not in apply-all.ps1. Can be created at runtime by `AuthTablesMigration` if missing. |
| **010** | Arabic i18n columns (`*_ar`) | Not in apply-all.ps1. Must be run manually or via `scripts/run-ar-migration.ps1`. |

**Recommendation:** Either add steps for 009 and 010 in `apply-all.ps1` (after 008, before seed) or document in a single “Database setup” doc that 009 and 010 must be applied separately and how.

---

## 5. Optional / Schema–Entity Mismatches (no migration required here)

- **departments.code:** Schema has `code`; `DepartmentJpaEntity` does not map it. Fix by adding the column to the entity or accepting that code is DB-only.
- **refunds.booking_id, penalty_amount, net_refund_amount, processed_by, gateway_refund_id:** Schema has these; entity does not. Either add to entity or leave as schema-only; no migration needed for existing columns.
- **taxi_bookings:** Entity uses `destination_location`, `scheduled_at`, `actual_price`, etc.; schema uses `dropoff_location`, `pickup_time`, `fare`. Names differ; if the entity is mapped via `@Column(name = "dropoff_location")` etc., no migration is needed. If not, either align entity column names with schema or add a migration to add/rename columns.

---

## 6. Suggested Migration Order

1. **011_hibernate_enum_compat_remaining.sql** – All remaining enum → VARCHAR for: roles, bookings, complaints, internal_tickets, refunds, notifications, taxi_bookings, services.
2. **012_entity_columns.sql** – departments.manager_id; notifications (message, template_name, updated_at); refunds (currency, transaction_reference).
3. **apply-all.ps1** – Add 009 and 010 (and 011, 012 if created) so a single run applies a full, consistent schema.

---

## 7. References

- Existing migrations: `docs/DATABASE_MIGRATIONS_REPORT.md`
- Base schema: `database/schema.sql`
- JPA entities: `backend/src/main/java/com/ziyara/backend/infrastructure/persistence/entity/`
- Apply script: `database/apply-all.ps1`
- Auth fallback: `backend/.../config/AuthTablesMigration.java`

---

*Report generated from schema, migrations, and JPA entities. Last updated: 2026-03-11.*
