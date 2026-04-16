# Ziyara – Database Migrations Report

This report documents all database migrations for the Ziyara (Ziyarah) platform. Migrations extend the base schema defined in `database/schema.sql` and must be applied in numerical order.

---

## 1. Overview

| Item | Detail |
|------|--------|
| **Database** | PostgreSQL 15+ |
| **Default schema** | `public` |
| **Base schema** | `database/schema.sql` |
| **Migrations path** | `database/migrations/` |
| **Apply script** | `database/apply-all.ps1` (PowerShell) |
| **Total migrations (numbered)** | **22** files: `001`–`015`, `018`–`022` (see §2; **not** every integer 016–017) |
| **Docker DB image** | `database/Dockerfile` — same order as `apply-all.ps1` (Postgres **15.17** Alpine) |

### Execution order

1. **Base:** `schema.sql` (creates core tables and enums)
2. **Migrations:** `001` → … → `015` (prefix phase uses `015_table_prefix_phase4.sql`; rollback script `015_rollback_table_prefix.sql` is manual only)
3. **Seed:** `database/seed.sql` (expects **prefixed** table names after 015)
4. **Post-seed migrations:** `018` → `019` → `020` → `021` → `022` (aligns with Docker init and `apply-all.ps1`)

**Note:** `apply-all.ps1` runs the full chain above in one run (step labels in the script: 22 steps including seed).

**Repo-only file:** `migrations/019_organizational_groups_g4_g6.sql` exists but is **not** referenced by `apply-all.ps1` or `database/Dockerfile`; use only if you intentionally add it to your pipeline.

---

## 2. Migration Summary

| # | File | Purpose | Tables/objects touched |
|---|------|---------|------------------------|
| 001 | `001_plans_schema_extensions.sql` | i18n labels, RBAC role flag | `i18n_labels` (new), `roles` |
| 002 | `002_role_management_report.sql` | Role status, locked permissions | `roles`, `permissions` |
| 003 | `003_pricing_and_payment_methods.sql` | Commission, pricing, payment idempotency, payment method | `service_providers`, `services`, `payments`, `payment_method_enum` |
| 004 | `004_hibernate_enum_compat.sql` | User role/status as VARCHAR for JPA | `users` |
| 005 | `005_reviews_status_varchar.sql` | Review status as VARCHAR for JPA | `reviews` |
| 006 | `006_discount_codes_jpa_compat.sql` | Discount entity columns + status VARCHAR | `discount_codes` |
| 007 | `007_service_providers_jpa_compat.sql` | Provider columns + status VARCHAR | `service_providers` |
| 008 | `008_employees_payments_enum_compat.sql` | Employees/payments enums + payment columns | `employees`, `payments` |
| 009 | `009_auth_tokens_otp.sql` | Password reset and OTP tables | `password_reset_tokens`, `otp_verification` (new) |
| 010 | `010_ar_columns_i18n.sql` | Arabic (_ar) columns for bilingual support | `departments`, `groups`, `roles`, `permissions`, `services`, `service_providers`, `discount_codes` |
| 011 | `011_hibernate_enum_compat_remaining.sql` | Remaining enum → VARCHAR for JPA | `roles`, `bookings`, `complaints`, `internal_tickets`, `refunds`, `notifications`, `taxi_bookings`, `services` |
| 012 | `012_entity_columns.sql` | Entity-expected columns (idempotent) | `departments`, `notifications`, `refunds` |
| 013 | `013_payment_gateway_3ds_columns.sql` | Gateway ref, 3DS status, gateway response on payments | `payments` |
| 014 | `014_provider_commission_audit.sql` | Commission change audit; refunds `processed_by` | `provider_commission_audit` (new), `refunds` |
| 015 | `015_table_prefix_phase4.sql` | Renames tables to domain prefixes (`sys_*`, `hotel_*`, `bkg_*`, `pay_*`, …) | Most core tables |
| 018 | `018_restaurant_trip_sample_services.sql` | Idempotent sample RESTAURANT/TRIP rows | `hotel_services` |
| 019 | `019_service_image_category_and_restaurant_menu.sql` | Service image column align + menu/category tables | `hotel_service_images`, menu-related tables |
| 020 | `020_groups_and_roles_for_all_user_roles.sql` | Org groups (incl. G7 B2C); system roles per `UserRole` | `sys_groups`, `sys_roles` |
| 021 | `021_system_settings_contact_leads.sql` | System settings KV + landing contact leads | `sys_system_settings`, `support_contact_leads` |
| 022 | `022_rbac_permission_catalogue.sql` | Expands `sys_permissions` catalogue; locked flags | `sys_permissions` |

---

## 3. Per-migration detail

### 001 – Plans schema extensions (i18n, RBAC)

- **File:** `database/migrations/001_plans_schema_extensions.sql`
- **Depends on:** `schema.sql` (must have `roles` table)

**Changes:**

- **New table:** `i18n_labels`
  - `id` (UUID PK), `key`, `en`, `ar`, `module`, `created_at`, `updated_at`
  - Unique on `(key, COALESCE(module, ''))`
  - Indexes on `key`, `module`
- **`roles`:** add `is_system_role BOOLEAN NOT NULL DEFAULT true` (if missing)  
  - System roles cannot be deleted; custom roles use `false`.

---

### 002 – Role management report

- **File:** `database/migrations/002_role_management_report.sql`
- **Depends on:** 001 (roles table with possible extensions)

**Changes:**

- **`roles`:** add `status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'` (if missing)  
  - Values: `ACTIVE`, `PENDING_REASSIGNMENT` (JPA EnumType.STRING).
- **`permissions`:** add `is_locked BOOLEAN NOT NULL DEFAULT false` (if missing)  
  - Locked permissions are system-only and not assignable to custom roles.

---

### 003 – Pricing and payment methods

- **File:** `database/migrations/003_pricing_and_payment_methods.sql`
- **Depends on:** Base schema (`service_providers`, `services`, `payments`, `payment_method_enum`)

**Changes:**

- **`service_providers`:** `commission_rate NUMERIC(5,2)` (nullable; NULL = use platform default 10%).
- **`services`:** `seasonal_multiplier NUMERIC(5,2) DEFAULT 1.00`, `tax_rate NUMERIC(5,4) DEFAULT 0`.
- **`payments`:** `idempotency_key VARCHAR(64) UNIQUE` + unique partial index for idempotency.
- **`payment_method_enum`:** add value `CASH_ON_SERVICE` (script may be run with “ignore if exists” logic).

---

### 004 – Hibernate enum compatibility (users)

- **File:** `database/migrations/004_hibernate_enum_compat.sql`
- **Depends on:** Base schema `users` table with enum columns

**Changes:**

- **`users`:**  
  - `role` → `VARCHAR(50)` (from enum, using `::text`).  
  - `status` → `VARCHAR(50)` (from enum).  
  - Aligns with JPA `@Enumerated(EnumType.STRING)`.

---

### 005 – Reviews status VARCHAR

- **File:** `database/migrations/005_reviews_status_varchar.sql`
- **Depends on:** Base schema `reviews` table

**Changes:**

- **`reviews`:** `status` → `VARCHAR(50)` (from enum) for Hibernate `EnumType.STRING`.

---

### 006 – Discount codes JPA compatibility

- **File:** `database/migrations/006_discount_codes_jpa_compat.sql`
- **Depends on:** Base schema `discount_codes` table

**Changes:**

- **New/added columns:** `type`, `value`, `min_booking_amount`, `max_discount_amount`, `start_date`, `end_date`, `usage_count` (with defaults where applicable).
- **Data:** One-time `UPDATE` to backfill from existing columns (e.g. `percentage` → `value`, `min_spend` → `min_booking_amount`, etc.).
- **`status`:** converted to `VARCHAR(50)` for JPA.

---

### 007 – Service providers JPA compatibility

- **File:** `database/migrations/007_service_providers_jpa_compat.sql`
- **Depends on:** Base schema `service_providers` table

**Changes:**

- **New columns (if not exists):** `city`, `country`, `rating`, `review_count`, `verified`.
- **`status`:** converted to `VARCHAR(50)` for Hibernate.

---

### 008 – Employees and payments enum compatibility

- **File:** `database/migrations/008_employees_payments_enum_compat.sql`
- **Depends on:** Base schema `employees`, `payments` tables

**Changes:**

- **`employees`:** `level` → `VARCHAR(50)`.
- **`payments`:** `status` → `VARCHAR(50)`, `method` → `VARCHAR(50)`.
- **`payments` (extra columns):** `gateway_name`, `payment_token`, `idempotency_key` (unique), `error_message` (all `ADD COLUMN IF NOT EXISTS`).  
  - Note: `idempotency_key` may already exist from 003; duplicate add is safe with `IF NOT EXISTS`.

---

### 009 – Auth tokens and OTP

- **File:** `database/migrations/009_auth_tokens_otp.sql`
- **Depends on:** Base schema `users` table

**Changes:**

- **New table:** `password_reset_tokens`  
  - `id`, `user_id` (FK to `users`), `token`, `expires_at`, `created_at`  
  - Indexes on `token`, `expires_at`.
- **New table:** `otp_verification`  
  - `id`, `email_or_phone`, `otp`, `expires_at`, `created_at`  
  - Indexes on `email_or_phone`, `expires_at`.

**Runtime behavior:** The application can create these tables on startup if missing via `AuthTablesMigration` (`core/.../infrastructure/config/AuthTablesMigration.java`), so 009 can be applied either manually or by running the app. If 009 is not applied, forgot-password and OTP flows will fail until the migration (or app runner) is executed.

---

### 010 – Arabic (i18n) columns

- **File:** `database/migrations/010_ar_columns_i18n.sql`
- **Script:** `scripts/run-ar-migration.ps1` (runs 010 only)
- **Depends on:** Tables from schema + any prior migrations that add those tables

**Changes:**

- Add `*_ar` (and where applicable `description_ar`) columns for bilingual support (e.g. `Accept-Language: ar`):
  - **departments:** `name_ar`, `description_ar`
  - **groups:** `name_ar`, `description_ar`
  - **roles:** `name_ar`, `description_ar`
  - **permissions:** `name_ar`, `description_ar`
  - **services:** `name_ar`, `description_ar`
  - **service_providers:** `company_name_ar`, `description_ar`
  - **discount_codes:** `description_ar`
- All use `ADD COLUMN IF NOT EXISTS` and comments for clarity.

---

### 011 – Hibernate enum compatibility (remaining)

- **File:** `database/migrations/011_hibernate_enum_compat_remaining.sql`
- **Depends on:** schema + 001–010

**Changes:**

- Converts remaining enum columns to `VARCHAR(50)` for JPA `@Enumerated(EnumType.STRING)`:
  - **roles:** `level`
  - **bookings:** `status`
  - **complaints:** `priority`, `status`
  - **internal_tickets:** `type`, `priority`, `status`
  - **refunds:** `status`
  - **notifications:** `type`, `channel`, `status`
  - **taxi_bookings:** `vehicle_type`, `status`
  - **services:** `type`, `status`
- **Not idempotent:** run once per environment; re-running after columns are already VARCHAR may error.

---

### 012 – Entity columns

- **File:** `database/migrations/012_entity_columns.sql`
- **Depends on:** 011

**Changes:**

- **departments:** `manager_id UUID REFERENCES users(id)` (for DepartmentJpaEntity).
- **notifications:** `message TEXT`, `template_name VARCHAR(255)`, `updated_at TIMESTAMP WITH TIME ZONE`; backfill `message` from `content` where applicable (for NotificationJpaEntity).
- **refunds:** `currency VARCHAR(3) DEFAULT 'USD'`, `transaction_reference VARCHAR(100)` (for RefundJpaEntity).
- **Idempotent:** uses `ADD COLUMN IF NOT EXISTS` and safe UPDATE.

---

### 013 – Payment gateway / 3DS columns

- **File:** `database/migrations/013_payment_gateway_3ds_columns.sql`
- **Depends on:** 012 (`payments` table)

**Changes:** `gateway_reference`, `three_ds_status`, `gateway_response` on `payments` (additive); index on `gateway_reference`.

---

### 014 – Provider commission audit

- **File:** `database/migrations/014_provider_commission_audit.sql`
- **Depends on:** 013

**Changes:** `refunds.processed_by`; new `provider_commission_audit` table for commission rate changes.

---

### 015 – Table prefix phase 4

- **File:** `database/migrations/015_table_prefix_phase4.sql`
- **Depends on:** 014  
- **Rollback:** `015_rollback_table_prefix.sql` (not run by Docker/apply-all)

**Changes:** Renames application tables to prefixed names (`sys_users`, `hotel_services`, `bkg_bookings`, `pay_payments`, …). **Backup before run** on brownfield DBs. `seed.sql` and later migrations assume prefixed names.

---

### 018 – Sample restaurant/trip services

- **File:** `database/migrations/018_restaurant_trip_sample_services.sql`
- **Depends on:** `seed.sql` (or at least schema + 015 + provider rows)

**Changes:** Idempotent `INSERT` of fixed-UUID RESTAURANT and TRIP rows into `hotel_services`.

---

### 019 – Service images and restaurant menu

- **File:** `database/migrations/019_service_image_category_and_restaurant_menu.sql`
- **Depends on:** 018

**Changes:** Aligns `hotel_service_images` columns with JPA; adds/aligns menu and image category structures (see file).

---

### 020 – Groups and roles for all user roles

- **File:** `database/migrations/020_groups_and_roles_for_all_user_roles.sql`
- **Depends on:** seed + prior RBAC tables

**Changes:** G7 B2C Customers group; links roles to org groups; inserts/updates system roles for each `UserRole` enum value.

---

### 021 – System settings and contact leads

- **File:** `database/migrations/021_system_settings_contact_leads.sql`

**Changes:** `sys_system_settings` (key/value JSON); `support_contact_leads` for `POST /public/contact` persistence.

---

### 022 – RBAC permission catalogue

- **File:** `database/migrations/022_rbac_permission_catalogue.sql`

**Changes:** Expands `sys_permissions` with domain-aligned codes; `is_locked` semantics for custom vs system roles (enforced in application layer).

---

## 4. How to apply migrations

### Full apply (schema + 001–015 + seed + 018–022)

From project root, with PostgreSQL available and credentials set:

```powershell
$env:PGPASSWORD = 'ziyarah_password'   # or use .pgpass
.\database\apply-all.ps1
```

This runs schema, migrations **001–015**, **seed**, then **018–022**. Environment variables (optional): `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` (defaults: localhost, 5432, ziyarah, ziyarah_user).

### Applying a single migration (e.g. 010 only)

- **010 (Arabic i18n):** `.\scripts\run-ar-migration.ps1` or run `database/migrations/010_ar_columns_i18n.sql` with psql.
- **009 (auth):** Run `database/migrations/009_auth_tokens_otp.sql` with psql, or let the backend create tables on startup via `AuthTablesMigration` if missing.

---

## 5. Dependency and ordering

- **001** must run after `schema.sql` (needs `roles`).
- **002** should run after 001 (extends `roles`/`permissions`).
- **003** requires base `service_providers`, `services`, `payments`, and `payment_method_enum`.
- **004–008** assume base schema tables exist; 006/007/008 align with JPA entities.
- **009** requires `users` (from base schema).
- **010** requires all tables it alters (from schema + earlier migrations).
- **011** requires 001–010 (converts enum columns; run once per environment).
- **012** requires 011 (adds entity columns; idempotent).
- **013–015** extend payments/audit and apply table prefixes; **015** is destructive rename — run once with backup on existing DBs.
- **018–022** run **after** `seed.sql` (Docker and `apply-all.ps1` both follow this order).

Recommended order for a fresh database:  
`schema.sql` → 001 → … → 015 → `seed.sql` → 018 → 019 → 020 → 021 → 022.

---

## 6. Idempotency and safety

- Most migrations use `IF NOT EXISTS`, `ADD COLUMN IF NOT EXISTS`, or conditional `DO $$ ... END $$` blocks so they can be re-run without failing.
- **004, 005, 011:** `ALTER COLUMN ... TYPE` is not idempotent if run twice after type change (second run may error); run once per environment.
- **003:** `ALTER TYPE ... ADD VALUE` may error if value already exists; script may need to ignore that error or check enum values first.
- **009:** `CREATE TABLE IF NOT EXISTS` is idempotent.
- **012:** Uses `ADD COLUMN IF NOT EXISTS` and safe UPDATE; idempotent.
- **015:** Table renames are **not** idempotent; do not re-run after prefixes are applied.

---

## 7. Related code and docs

| Item | Location | Note |
|------|----------|------|
| Base schema | `database/schema.sql` | Core tables and enums |
| Seed data | `database/seed.sql` | After 015 in full apply; uses prefixed table names |
| Auth tables fallback | `core/src/main/java/com/ziyara/backend/infrastructure/config/AuthTablesMigration.java` | Creates 009 tables on startup if missing |
| Apply script | `database/apply-all.ps1` | Schema + 001–015 + seed + 018–022 |
| Docker init | `database/Dockerfile` | Same migration order as apply-all (lexicographic `01-`…`22-` filenames) |
| Arabic migration script | `scripts/run-ar-migration.ps1` | Applies 010 only |

---

*Report generated from `database/migrations/` and project scripts. Last updated: 2026-04-04.*
