# Ziyara Database Schema Reference

Schema managed by Flyway migrations (V0–V25). PostgreSQL 15+ required.  
Default schema: `public`. All tables use `UUID` primary keys with `gen_random_uuid()`.

---

## RBAC & Organizational Structure

### `sys_groups`
Organizational slices. Platform-reserved codes: `Z1`–`Z7` (Ziyara internal departments). Custom groups use `C1`, `C2`, ... auto-assigned.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `name` | VARCHAR(100) NOT NULL | |
| `name_ar` | VARCHAR(100) | Arabic translation |
| `code` | VARCHAR(20) NOT NULL UNIQUE | `Z1`–`Z7` reserved, `C1+` custom |
| `description` | TEXT | |
| `created_at` | TIMESTAMPTZ | |

### `sys_departments`
Internal company departments (HR, Finance, etc.).

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `name` | VARCHAR(100) NOT NULL UNIQUE | |
| `manager_id` | UUID | References `sys_users.id` (soft FK) |

### `sys_permissions`
Immutable permission catalog. Locked permissions (`is_locked=true`) can only be assigned to system roles.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `code` | VARCHAR(100) UNIQUE | e.g. `payments:read` |
| `resource` | VARCHAR(100) | e.g. `payments` |
| `action` | VARCHAR(50) | e.g. `read`, `write` |
| `scope` | VARCHAR(50) | `ALL`, `OWN`, etc. |
| `is_locked` | BOOLEAN | If true, custom roles cannot hold it |

### `sys_roles`
Role definitions. System roles (`is_system_role=true`) are seeded by `V17__reference_data.sql` and cannot be deleted.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `name` | VARCHAR(50) UNIQUE | |
| `code` | VARCHAR(30) UNIQUE | |
| `level` | VARCHAR(30) | `PLATFORM`, `EMPLOYEE`, etc. |
| `group_id` | UUID FK → `sys_groups` | |
| `is_system_role` | BOOLEAN | |
| `navigation_item_ids` | JSONB | Sidebar menu items granted to the role |

### `sys_role_permissions`
Many-to-many join between roles and permissions.

| Column | Type |
|---|---|
| `role_id` | UUID FK → `sys_roles` |
| `permission_id` | UUID FK → `sys_permissions` |

### `sys_user_role_assignments`
Assigns a user to a role within a specific group. One user can hold multiple assignments.

| Column | Type |
|---|---|
| `id` | UUID PK |
| `user_id` | UUID FK → `sys_users` |
| `role_id` | UUID FK → `sys_roles` |
| `group_id` | UUID FK → `sys_groups` |
| `assigned_at` | TIMESTAMPTZ |
| `assigned_by` | UUID |

---

## Core Users

### `sys_users`
All system users: customers, company staff, and provider portal accounts.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `email` | VARCHAR(255) UNIQUE NOT NULL | Normalized to lowercase |
| `phone` | VARCHAR(50) UNIQUE | |
| `password_hash` | VARCHAR(255) NOT NULL | BCrypt |
| `role` | VARCHAR(50) NOT NULL | `UserRole` enum value |
| `status` | VARCHAR(50) NOT NULL | `ACTIVE`, `INACTIVE`, `FROZEN`, `PENDING_VERIFICATION`, `DELETED` |
| `failed_login_attempts` | INTEGER | Incremented on wrong password |
| `locked_until` | TIMESTAMPTZ | Account lockout expiry |
| `token_version` | INTEGER | Incremented on password reset to invalidate existing JWTs |
| `mfa_enabled` | BOOLEAN | TOTP MFA enrolled |
| `mfa_secret_cipher` | TEXT | AES-256 encrypted TOTP secret |
| `mfa_backup_codes_cipher` | TEXT | Encrypted backup codes |
| `gdpr_consent_given` | BOOLEAN | |
| `right_to_erasure_requested` | BOOLEAN | |
| `deleted_at` | TIMESTAMPTZ | Soft-delete |

### `sys_user_password_history`
Tracks previous password hashes to enforce password reuse policy (added by V11).

| Column | Type |
|---|---|
| `id` | UUID PK |
| `user_id` | UUID FK → `sys_users` |
| `password_hash` | VARCHAR(255) |
| `changed_at` | TIMESTAMPTZ |

### `sys_user_sessions`
Active refresh token sessions (used with blocklist for token invalidation).

---

## Bookings & Payments

### `bookings`
Core booking records linking a customer to a service.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → `sys_users` | Customer |
| `service_id` | UUID FK → `services` | |
| `provider_id` | UUID FK → `service_providers` | |
| `status` | VARCHAR(50) | `PENDING`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `check_in` | DATE | |
| `check_out` | DATE | |
| `total_amount` | NUMERIC | |
| `currency` | VARCHAR(3) | ISO 4217 |
| `booking_number` | VARCHAR(50) UNIQUE | Human-readable reference |

### `payments`
Payment records (one per booking attempt; multiple allowed for retries).

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `booking_id` | UUID FK → `bookings` | |
| `user_id` | UUID | Customer |
| `amount` | NUMERIC | |
| `currency` | VARCHAR(3) | |
| `status` | VARCHAR(50) | `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED` |
| `gateway` | VARCHAR(50) | `stripe`, `stub`, etc. |
| `transaction_reference` | VARCHAR(255) UNIQUE | Gateway transaction ID |
| `gateway_reference` | VARCHAR(255) | Normalised gateway ref |
| `idempotency_key` | VARCHAR(255) UNIQUE | Client-provided; prevents double-charge |
| `three_ds_status` | VARCHAR(50) | 3DS result |

### `refunds`
Refund records linked to a payment.

| Column | Type |
|---|---|
| `id` | UUID PK |
| `payment_id` | UUID FK → `payments` |
| `amount` | NUMERIC |
| `reason` | TEXT |
| `performed_by` | UUID |
| `status` | VARCHAR(50) |

---

## Service Providers

### `service_providers`
Provider partner accounts (hotels, restaurants, taxi operators).

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `user_id` | UUID FK → `sys_users` | Owner account |
| `name` | VARCHAR(255) | |
| `status` | VARCHAR(50) | `PENDING`, `ACTIVE`, `SUSPENDED` |
| `provider_type` | VARCHAR(50) | `HOTEL`, `RESTAURANT`, `TAXI`, etc. |
| `subscription_plan_id` | UUID | |

### `services`
Individual services offered by providers (hotel rooms, restaurant menus, taxi routes).

### `service_images`
Media attachments for services.

---

## System / Audit

### `sys_audit_logs`
Immutable audit trail for all state-changing operations.

| Column | Type | Notes |
|---|---|---|
| `id` | UUID PK | |
| `action` | TEXT | Human-readable description |
| `entity_type` | VARCHAR(100) | e.g. `users`, `roles`, `payments` |
| `entity_id` | VARCHAR(255) | |
| `actor_id` | UUID | Who performed the action |
| `ip_address` | VARCHAR(45) | |
| `created_at` | TIMESTAMPTZ | |
| `old_value` | JSONB | Before-state |
| `new_value` | JSONB | After-state |

`sys_audit_logs_archive` (V11): partitioned table for long-term retention.

### `sys_rate_limit_counters`
Per-IP per-endpoint per-minute request counters (PostgreSQL fallback when Redis is unavailable).

### `sys_security_events`
Login failure events, MFA failures, and anomalous access patterns.

### `sys_security_alert_rules` / `sys_security_alerts`
Configurable threshold rules and triggered alerts (e.g. 5+ login failures from same IP).

### `sys_notifications`
In-app notifications queue for company staff.

---

## Flyway Migration History

| Version | Summary |
|---|---|
| V0 | Full baseline schema |
| V1 | Event publication table (Spring Modulith compatibility) |
| V2 | CMS web content pages |
| V3 | System settings + contact leads |
| V4 | Role navigation item IDs (JSONB column) |
| V5 | Portal support requests |
| V6 | Provider workflow status columns |
| V7 | Discount scope columns |
| V8 | Dashboard performance indexes |
| V9 | Organizational groups Z-code constraints |
| V10 | Kafka staff notification delivered tracking |
| V11 | Database hardening: password history, MFA columns, security event tables |
| V12 | Rate limiting + security alert tables |
| V13 | RLS policies + audit log partitioning |
| V14 | Employee soft-delete support |
| V15 | Subscription plans |
| V16 | Database best-practices indexes and constraints |
| V17 | Reference data seed (system roles, groups, permissions) |
| V18 | FCM push token column on sys_users |
| V19 | Portal payout requests |
| V20–V25 | Constraint fixes (user status, ticket number length, complaint/booking/service/review status) |
