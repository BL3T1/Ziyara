# Database Best Practices — Implementation Plan

**Migration:** `V16__database_best_practices.sql`  
**Audit covers:** V0 → V15 (all existing migrations)  
**PostgreSQL target:** 15+  
**Author:** Ziyara Engineering  
**Date:** 2026-05-21

---

## Executive Summary

A full audit of migrations V0–V15 identified **10 categories of structural deficiencies** that increase data-corruption risk, degrade query performance, and reduce operational maintainability. All fixes are delivered as a single idempotent Flyway migration (`V16`) using `IF NOT EXISTS` guards and conditional `DO $$` blocks — safe to re-apply and safe to run against databases that already have partial fixes applied.

No application Java code changes are required by this migration. It is purely additive (indexes, constraints, triggers) and corrective (type casts, default values).

---

## Category Overview

| # | Category | Issues Found | SQL Objects Changed |
|---|---|---|---|
| A | Referential integrity gaps | 10 | 1 FK + 9 indexes added |
| B | Redundant indexes | 2 | 2 indexes dropped |
| C | CHECK constraints on status columns | 13 | 13 constraints added |
| D | Partial indexes (soft-delete / hot-path) | 7 | 7 indexes added |
| E | Composite indexes (query patterns) | 7 | 7 indexes added |
| F | NULL-safety fixes | 7 | 4 tables altered |
| G | TIMESTAMP → TIMESTAMPTZ | 2 columns | 1 table altered |
| H | Missing UNIQUE constraints | 3 | 3 constraints added |
| I | DB-level `updated_at` trigger | 31 tables | 1 function + up to 31 triggers |
| J | Data type improvements | 2 columns | 2 tables altered |
| — | Materialized view refresh | 1 view | `mv_pay_daily_totals` |

---

## Category A — Referential Integrity Gaps

### Problem

Foreign-key columns were declared `NOT NULL` or carry semantic references to other tables, but several had **no FK constraint** and/or **no index on the referencing column**. This causes:

- Silent orphan rows when referenced records are deleted
- Full sequential scans on FK joins (PostgreSQL does not auto-index FK columns)

### Fixes Applied

| Fix | Table | Column | Action |
|---|---|---|---|
| A1 | `hotel_reviews` | `booking_id` | Added `FOREIGN KEY → bkg_bookings(id) ON DELETE SET NULL` + index |
| A2 | `bkg_taxi_bookings` | `booking_id` | Added index (FK existed, index missing) |
| A3 | `bkg_taxi_bookings` | `driver_id` | Added partial index `WHERE driver_id IS NOT NULL` |
| A4 | `pay_refunds` | `payment_id` | Added index (FK existed, index missing) |
| A5 | `bkg_bookings` | `cancelled_by` | Added partial index `WHERE cancelled_by IS NOT NULL` |
| A5 | `bkg_bookings` | `rejected_by` | Added partial index `WHERE rejected_by IS NOT NULL` |
| A6 | `pay_refunds` | `processed_by` | Added partial index `WHERE processed_by IS NOT NULL` |
| A7 | `support_complaints` | `assigned_agent_id` | Added partial index `WHERE assigned_agent_id IS NOT NULL` |
| A8 | `disc_discount_codes` | `provider_id` | Added partial index `WHERE provider_id IS NOT NULL` |
| A9 | `sys_password_reset_tokens` | `token` | Added index (primary lookup key was unindexed) |
| A10 | `sessions` | `expires_at` | Added partial index `WHERE is_invalidated IS NOT TRUE` |

### Verification

```sql
-- Confirm FK exists
SELECT constraint_name FROM information_schema.table_constraints
WHERE constraint_name = 'fk_hotel_reviews_booking_id';

-- Confirm indexes exist
SELECT indexname FROM pg_indexes
WHERE tablename IN ('hotel_reviews','bkg_taxi_bookings','pay_refunds','bkg_bookings',
                    'support_complaints','disc_discount_codes',
                    'sys_password_reset_tokens','sessions')
  AND indexname LIKE 'idx_%';
```

---

## Category B — Redundant Indexes

### Problem

PostgreSQL automatically creates a B-tree index when a `UNIQUE` constraint is defined. Manually adding a separate `CREATE INDEX` on the same column(s) **wastes storage, slows writes (two identical structures updated on every INSERT/UPDATE), and confuses the query planner**.

### Fixes Applied

| Fix | Index Dropped | Reason |
|---|---|---|
| B1 | `idx_sys_users_email` | Duplicate of `UNIQUE(email)` on `sys_users` |
| B2 | `idx_web_content_pages_slug` | Duplicate of `UNIQUE(slug)` on `web_content_pages` |

### Verification

```sql
-- These should return 0 rows after migration
SELECT indexname FROM pg_indexes
WHERE indexname IN ('idx_sys_users_email', 'idx_web_content_pages_slug');
```

---

## Category C — CHECK Constraints on Status/Enum Columns

### Problem

Status columns are `VARCHAR` with no domain constraint. Invalid values can be inserted by:
- Direct SQL (admin tools, psql scripts, data migrations)
- Application bugs that skip validation layers
- Future developers unaware of the allowed set

Once bad data exists, filtering queries silently miss rows.

### Fixes Applied

| Fix | Table | Column | Allowed Values |
|---|---|---|---|
| C1 | `sys_users` | `status` | ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION, PENDING_APPROVAL, LOCKED |
| C2 | `hotel_service_providers` | `status` | PENDING_APPROVAL, ACTIVE, SUSPENDED, REJECTED, DEACTIVATED |
| C3 | `hotel_services` | `status` | PENDING_APPROVAL, ACTIVE, INACTIVE, SUSPENDED, REJECTED, ARCHIVED |
| C4 | `bkg_bookings` | `status` | PENDING, CONFIRMED, ACTIVE, COMPLETED, CANCELLED, REJECTED, EXPIRED, NO_SHOW |
| C5 | `pay_payments` | `status` | PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, CANCELLED, DISPUTED |
| C6 | `pay_refunds` | `status` | REQUESTED, PROCESSING, COMPLETED, REJECTED, CANCELLED |
| C7 | `support_complaints` | `status` | SUBMITTED, OPEN, IN_PROGRESS, ESCALATED, RESOLVED, CLOSED, REJECTED |
| C8 | `sys_notifications` | `status` | PENDING, SENT, FAILED, READ, ARCHIVED |
| C9 | `hotel_reviews` | `status` | PENDING, APPROVED, REJECTED, HIDDEN |
| C10 | `hotel_reviews` | `rating` | `BETWEEN 1 AND 5` |
| C11 | `sys_roles` | `status` | ACTIVE, INACTIVE, DEPRECATED |
| C12 | `bkg_bookings` | amounts | `base_amount >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0` |
| C13 | `pay_payments` | `amount` | `amount > 0` |

> **Important:** If the application currently writes any value not in these lists, the constraint addition will **fail with a CHECK violation error** — which immediately reveals the bad data. Fix the data before re-running, or add the missing value to the constraint.

### Verification

```sql
SELECT constraint_name, check_clause
FROM information_schema.check_constraints
WHERE constraint_name LIKE 'chk_%'
ORDER BY constraint_name;
```

---

## Category D — Partial Indexes for Soft-Delete and Hot-Path Filters

### Problem

Soft-delete patterns (`deleted_at IS NULL`, `offboarded_at IS NULL`) and status filters (`status = 'PENDING'`) are applied on nearly every query but were not index-assisted. Full or near-full table scans were the result — exponentially expensive as row counts grow.

### Fixes Applied

| Index | Table | Condition | Use Case |
|---|---|---|---|
| `idx_sys_users_active_email` | `sys_users` | `WHERE deleted_at IS NULL` | Login / user lookup (skips deleted users) |
| `idx_hotel_services_active` | `hotel_services` | `WHERE deleted_at IS NULL` | Service browsing (multi-column: provider_id, type, status) |
| `idx_bkg_bookings_pending_created` | `bkg_bookings` | `WHERE status IN ('PENDING','CONFIRMED')` | Operator dashboard — open bookings list |
| `idx_sessions_valid_expires` | `sessions` | `WHERE is_invalidated IS NOT TRUE` | Session validation (orders by expires_at) |
| `idx_sys_notifications_unread` | `sys_notifications` | `WHERE read_at IS NULL AND status != 'ARCHIVED'` | Notification bell — unread count per user |
| `idx_disc_discount_codes_active_code` | `disc_discount_codes` | `WHERE status='ACTIVE' AND approval_status='APPROVED'` | Booking flow discount-code lookup |

> Note: The `idx_sys_employees_active` partial index was already created in V14 (`WHERE offboarded_at IS NULL`). No duplicate is needed here.

### Estimated Impact

Partial indexes on soft-delete columns typically reduce index size by **60–90 %** compared to a full-table index, because deleted/offboarded rows are a small fraction of total rows in a healthy system. The query planner will prefer these smaller, denser structures.

---

## Category E — Composite Indexes for Common Query Patterns

### Problem

Several high-frequency queries filter on two or three columns simultaneously. Single-column indexes force PostgreSQL to either pick one and filter the rest in memory, or fall back to a sequential scan. Composite indexes covering all filter columns eliminate this.

### Fixes Applied

| Index | Table | Columns | Targeted Query Pattern |
|---|---|---|---|
| `idx_sys_audit_logs_type_action_created` | `sys_audit_logs` | `(entity_type, action, created_at DESC)` | Audit log filter endpoint (`/audit-logs/filter`) |
| `idx_sys_audit_logs_user_created` | `sys_audit_logs` | `(user_id, created_at DESC)` | Actor activity lookup (`/audit-logs/user/{userId}`) |
| `idx_sys_security_events_type_ip_created` | `sys_security_events` | `(event_type, ip_address, created_at DESC)` | Rate-limit / brute-force detection window queries |
| `idx_bkg_bookings_service_checkin_status` | `bkg_bookings` | `(service_id, check_in_date, status)` | Availability checks excluding cancelled/expired |
| `idx_pay_payments_gateway_ref` | `pay_payments` | `(gateway_reference)` | Payment gateway webhook reconciliation |
| `idx_sys_rate_limit_window_start` | `sys_rate_limit_counters` | `(window_end, identifier_type)` | Rate-limit window cleanup job |
| `idx_sys_otp_expires_at` | `sys_otp_verification` | `(expires_at)` | OTP expiry check + cleanup |

### Column Order Rule

For composite indexes, **equality columns come first, range/sort column comes last**. This matches the `(entity_type, action, created_at DESC)` pattern: filter by type and action first (equality), then order by time.

---

## Category F — NULL-Safety Fixes

### Problem

Columns that are semantically required (audit timestamps, role assignment times, status fields) were defined as nullable with no default, meaning:
- INSERTs that omit the column silently store NULL
- Reporting queries that sort or aggregate on these columns produce incorrect results
- JPA `@PrePersist` / `@PreUpdate` lifecycle callbacks are the only guard — bypassed by native SQL

### Fixes Applied

| Fix | Table | Column | Change |
|---|---|---|---|
| F1 | `sys_employees` | `created_at` | SET DEFAULT CURRENT_TIMESTAMP; backfill NULLs; SET NOT NULL |
| F2 | `sys_employees` | `updated_at` | SET DEFAULT CURRENT_TIMESTAMP; backfill NULLs |
| F3 | `sys_user_roles` | `assigned_at` | SET DEFAULT CURRENT_TIMESTAMP; backfill NULLs; SET NOT NULL |
| F4 | `sys_roles` | `status` | Backfill NULLs → 'ACTIVE'; SET NOT NULL; SET DEFAULT 'ACTIVE' |
| F5 | `sys_departments` | `updated_at` | SET DEFAULT CURRENT_TIMESTAMP |
| F6 | `hotel_service_providers` | `updated_at` | SET DEFAULT CURRENT_TIMESTAMP |
| F7 | `customers` | `updated_at` | SET DEFAULT CURRENT_TIMESTAMP |

> The `updated_at` columns in F5–F7 are not forced `NOT NULL` because the trigger in Category I will handle all future writes. Forcing NOT NULL on `updated_at` is addressed by the trigger mechanism.

---

## Category G — TIMESTAMP → TIMESTAMPTZ

### Problem

`web_content_pages.created_at` and `web_content_pages.updated_at` were typed as `TIMESTAMP WITHOUT TIME ZONE`. All other timestamp columns in the schema use `TIMESTAMPTZ` (with timezone). The inconsistency means:

- Comparisons between `web_content_pages` times and other table times are implicit casts (slow, error-prone)
- When the database server timezone changes, stored times are misinterpreted
- Daylight-saving-time transitions cause silent 1-hour offsets

### Fix Applied

```sql
ALTER TABLE web_content_pages
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE web_content_pages
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';
```

Existing data is cast to UTC (the assumed server timezone for this system). Both columns are also made `NOT NULL` after the cast.

Additionally, `web_content_pages.id` was missing `DEFAULT gen_random_uuid()` — fixed to ensure INSERTs without explicit IDs work correctly.

### Verification

```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'web_content_pages'
  AND column_name IN ('created_at', 'updated_at');
-- Expected: 'timestamp with time zone' for both
```

---

## Category H — Missing UNIQUE Constraints

### Problem

Three scenarios where duplicate rows are semantically impossible but were not enforced at the database level:

| Fix | Table | Scenario |
|---|---|---|
| H1 | `sys_user_roles` | Same user assigned the same role in the same group twice |
| H2 | `pay_exchange_rates` | Two exchange rates for the same currency pair on the same date |
| H3 | `sys_integration_api_keys` | Two API keys with the same `key_prefix` (used as lookup key) |

### Fix Details

**H1 — `sys_user_roles`**: The `group_id` column is nullable, so standard `UNIQUE(user_id, role_id, group_id)` would allow multiple NULLs (PostgreSQL treats NULLs as distinct in standard UNIQUE). PostgreSQL 15's `UNIQUE NULLS NOT DISTINCT` treats NULLs as equal for uniqueness purposes:

```sql
ADD CONSTRAINT uk_sys_user_roles_user_role_group
UNIQUE NULLS NOT DISTINCT (user_id, role_id, group_id);
```

**H2 — `pay_exchange_rates`**:
```sql
ADD CONSTRAINT uk_pay_exchange_rates_pair_date
UNIQUE (from_currency, to_currency, effective_date);
```

**H3 — `sys_integration_api_keys`**:
```sql
ADD CONSTRAINT uk_sys_integration_api_keys_prefix
UNIQUE (key_prefix);
```

> **Pre-condition:** If duplicate rows already exist, the constraint addition will fail. Run the duplicate-detection queries below before applying:
> ```sql
> -- Detect duplicates in sys_user_roles
> SELECT user_id, role_id, group_id, COUNT(*)
> FROM sys_user_roles GROUP BY 1,2,3 HAVING COUNT(*) > 1;
>
> -- Detect duplicates in pay_exchange_rates
> SELECT from_currency, to_currency, effective_date, COUNT(*)
> FROM pay_exchange_rates GROUP BY 1,2,3 HAVING COUNT(*) > 1;
>
> -- Detect duplicates in sys_integration_api_keys
> SELECT key_prefix, COUNT(*) FROM sys_integration_api_keys
> GROUP BY 1 HAVING COUNT(*) > 1;
> ```

---

## Category I — DB-Level `updated_at` Trigger

### Problem

`updated_at` columns are currently stamped only by JPA `@PreUpdate` lifecycle callbacks. This means:

- Direct SQL updates (Flyway migrations, psql admin scripts, data patches) silently leave `updated_at` stale
- Bulk operations that use `UPDATE ... WHERE ...` via JDBC bypass JPA entirely
- Future developers using non-JPA tools will produce inaccurate audit timestamps

### Fix Applied

A reusable trigger function is created once, then applied to every table that has an `updated_at` column:

```sql
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;
```

The trigger is applied to **31 tables** via a `DO` loop — only created if a trigger with the same name does not already exist (idempotent):

```
sys_users, sys_roles, sys_groups, sys_departments, sys_employees,
sys_notifications, sys_data_retention_policies, sys_data_export_requests,
sys_plans, sys_customer_subscriptions,
hotel_service_providers, hotel_services, hotel_service_images,
hotel_service_rooms, hotel_service_room_images,
hotel_rest_menu_sections, hotel_rest_menu_items, hotel_reviews,
disc_discount_codes, bkg_bookings, bkg_taxi_bookings,
pay_payments, pay_refunds, pay_exchange_rates,
support_complaints, support_complaint_comments,
support_internal_tickets, support_ticket_comments,
customers, sessions, web_content_pages
```

### Interaction with JPA

The trigger and JPA `@PreUpdate` are **not in conflict**. When JPA sets `updated_at` before the UPDATE, the trigger overwrites it with `CURRENT_TIMESTAMP` at statement execution time — producing the same value (within microseconds). JPA sets the field; the DB guarantees it even when JPA is bypassed.

### Verification

```sql
-- List all installed updated_at triggers
SELECT trigger_name, event_object_table
FROM information_schema.triggers
WHERE trigger_name LIKE 'trg_%_updated_at'
ORDER BY event_object_table;
-- Expected: 31 rows
```

---

## Category J — Data Type Improvements

### J1 — `hotel_service_providers.rating`: DOUBLE PRECISION → NUMERIC(3,2)

| | Before | After |
|---|---|---|
| Type | `DOUBLE PRECISION` (binary float) | `NUMERIC(3,2)` (exact decimal) |
| Example stored | `4.29999999...` | `4.30` |
| Storage | 8 bytes | 5 bytes |
| Arithmetic | Floating-point rounding | Exact |

A `CHECK (rating BETWEEN 0.00 AND 5.00)` constraint is added simultaneously.

### J2 — `sys_notifications.metadata`: TEXT → JSONB

| | Before | After |
|---|---|---|
| Type | `TEXT` | `JSONB` |
| Server-side JSON ops | None (must deserialise in app) | `->`, `->>`, `@>`, `jsonpath` operators |
| Indexing | Full-text only | GIN index on keys |
| Storage | Raw text | Binary-parsed (faster read, ~same size) |

The migration handles legacy data safely:

- `NULL` or empty string → stored as `NULL`
- JSON objects (`{...}`) → cast directly to `JSONB`
- Non-JSON legacy strings → wrapped as `{"raw": "original value"}`

---

## Application Layer Notes

This migration requires **no changes to application Java code** beyond what was already implemented in V14 (soft-delete) and V15 (subscriptions). However, the following application behaviours are now enforced at the DB level and should be verified in integration tests:

| DB Constraint Added | Application Code That Must Respect It |
|---|---|
| `chk_sys_users_status` | `UserService`, `AuthService` — any status setter |
| `chk_bkg_bookings_status` | `BookingService` — state machine transitions |
| `chk_pay_payments_amount_positive` | `PaymentService` — payment creation |
| `chk_hotel_reviews_rating_range` | `ReviewService` — review submission |
| `uk_sys_user_roles_user_role_group` | `RoleService` — role assignment must check for existing rows |
| `fk_hotel_reviews_booking_id` | `ReviewService` — must not accept `booking_id` referencing deleted bookings |

---

## Execution Order & Risk Assessment

All changes in V16 are **non-destructive to data** (no DROP TABLE, no DROP COLUMN, no data removal). Execution order within the file follows dependency requirements:

```
A (indexes/FKs) — no dependencies
B (drop indexes) — safe at any point
C (CHECK constraints) — requires clean data; may fail if violations exist
D (partial indexes) — no dependencies
E (composite indexes) — no dependencies
F (NOT NULL defaults) — must run before [I] trigger (ensures no NULLs remain)
G (type cast) — conditional on column type; idempotent
H (UNIQUE constraints) — requires no existing duplicates
I (trigger) — no dependencies; safe to run multiple times
J (type changes) — conditional on current type; data-safe cast
```

**Estimated downtime:** Near-zero on a live PostgreSQL instance. Index creation uses `CREATE INDEX IF NOT EXISTS` (non-blocking in PG12+ can be done with `CONCURRENTLY` if needed for large tables). `ALTER TABLE ... ADD CONSTRAINT CHECK` acquires `ACCESS EXCLUSIVE` briefly.

> For production deployment on large tables (millions of rows), consider splitting Section C into a separate migration and using `ALTER TABLE ... ADD CONSTRAINT ... NOT VALID` followed by a background `VALIDATE CONSTRAINT` to avoid lock contention.

---

## Verification Checklist

Run these queries after applying V16 to confirm all changes landed:

```sql
-- 1. FKs
SELECT COUNT(*) FROM information_schema.table_constraints
WHERE constraint_type = 'FOREIGN KEY'
  AND constraint_name = 'fk_hotel_reviews_booking_id';
-- Expected: 1

-- 2. No redundant indexes remain
SELECT COUNT(*) FROM pg_indexes
WHERE indexname IN ('idx_sys_users_email', 'idx_web_content_pages_slug');
-- Expected: 0

-- 3. CHECK constraints (13 total)
SELECT COUNT(*) FROM information_schema.table_constraints
WHERE constraint_type = 'CHECK'
  AND constraint_name LIKE 'chk_%';
-- Expected: >= 13

-- 4. Partial and composite indexes
SELECT COUNT(*) FROM pg_indexes WHERE indexname IN (
    'idx_sys_users_active_email',
    'idx_hotel_services_active',
    'idx_bkg_bookings_pending_created',
    'idx_sessions_valid_expires',
    'idx_sys_notifications_unread',
    'idx_disc_discount_codes_active_code',
    'idx_sys_audit_logs_type_action_created',
    'idx_sys_audit_logs_user_created',
    'idx_sys_security_events_type_ip_created',
    'idx_bkg_bookings_service_checkin_status',
    'idx_pay_payments_gateway_ref',
    'idx_sys_rate_limit_window_start',
    'idx_sys_otp_expires_at'
);
-- Expected: 13

-- 5. NOT NULL on key columns
SELECT is_nullable FROM information_schema.columns
WHERE table_name = 'sys_employees' AND column_name = 'created_at';
-- Expected: 'NO'

-- 6. TIMESTAMPTZ
SELECT data_type FROM information_schema.columns
WHERE table_name = 'web_content_pages' AND column_name = 'created_at';
-- Expected: 'timestamp with time zone'

-- 7. UNIQUE constraints
SELECT COUNT(*) FROM information_schema.table_constraints
WHERE constraint_name IN (
    'uk_sys_user_roles_user_role_group',
    'uk_pay_exchange_rates_pair_date',
    'uk_sys_integration_api_keys_prefix'
);
-- Expected: 3

-- 8. updated_at triggers
SELECT COUNT(*) FROM information_schema.triggers
WHERE trigger_name LIKE 'trg_%_updated_at';
-- Expected: 31

-- 9. Trigger function exists
SELECT COUNT(*) FROM pg_proc WHERE proname = 'fn_set_updated_at';
-- Expected: 1

-- 10. Rating column type
SELECT data_type, numeric_precision, numeric_scale
FROM information_schema.columns
WHERE table_name = 'hotel_service_providers' AND column_name = 'rating';
-- Expected: 'numeric', 3, 2

-- 11. Notifications metadata type
SELECT data_type FROM information_schema.columns
WHERE table_name = 'sys_notifications' AND column_name = 'metadata';
-- Expected: 'jsonb'
```

---

## Items Not Covered (Future Migrations)

The following improvements were considered but deferred to keep V16 focused on structural fixes:

| Item | Reason Deferred | Recommended As |
|---|---|---|
| `sys_audit_logs` HASH→RANGE re-partitioning | Requires table rebuild (data migration); high risk | V17 with pg_partman |
| GIN index on `sys_notifications.metadata` | Only valuable once JSONB cast (J2) lands in production and query patterns are confirmed | V17 |
| Row-level security (RLS) on tenant-scoped tables | Policy design requires input from security team | V18 |
| `bkg_bookings` table partitioning by `check_in_date` | Large effort; range partitioning is correct approach | V18 |
| Index on `sys_audit_logs.correlation_id` | Only needed if correlation-based filtering is added to the API | On-demand |
| `CLUSTER` on `bkg_bookings (check_in_date)` | One-time physical sort; must be re-run periodically | On-demand via DBA |
