# Migrations vs Plans: Data Storage Analysis

This document analyzes whether the **current database migrations (001–012)** provide the storage needed for the system as described in the plan documents. It maps plan-derived requirements to the schema and migrations and calls out any remaining gaps.

**Plans analyzed:**  
DASHBOARD_DESIGN_REPORT, DYNAMIC_COMMISSION_REPORT, FIGMA_NEEDS_BY_PHASE, FRONTEND_GAP_REPORT, FRONTEND_IMPLEMENTATION_PLAN, implementation_plan, INFRASTRUCTURE_REPORT, MODULAR_MONOLITH_STRUCTURE, MONOLITH_IMPLEMENTATION, PAYMENT_METHODS, PRICING_METHODS, REQUIREMENTS_ANALYSIS, ROLE_MANAGEMENT_REPORT, SYSTEM_EVOLUTION_REPORT.

---

## 1. Executive Summary

| Conclusion | Detail |
|------------|--------|
| **Core storage: covered** | Migrations 001–012, together with the base schema, provide the database structures needed for the **current** application and for most features described in the plans (RBAC, dynamic commission, payments/refunds, i18n, auth, notifications, tickets, complaints, bookings, services, providers). |
| **Remaining gaps** | A few plan-derived features expect storage that is **not** yet in the schema or migrations: **provider payouts**, **provider document verification**, **provider–user linkage** for portal scoping, and optionally **DB master permissions** for G1. |

**Verdict:** The migrations **do** provide what is needed for the system to **store data** for the functions that are already implemented or that rely on the existing schema and JPA entities. For the full vision in the plans (e.g. Payout Ledger, Document Verification gallery, provider-scoped staff), additional tables or columns will be needed and should be added via **future migrations** (013+).

---

## 2. Plan Requirements vs Current Schema + Migrations

### 2.1 Dynamic Commission & Pricing (DYNAMIC_COMMISSION_REPORT, PRICING_METHODS)

| Requirement | Storage | Status |
|-------------|---------|--------|
| Provider-level commission override (default 10%) | `service_providers.commission_rate` | ✅ **003** (nullable; NULL = platform default) |
| Audit trail for commission changes | `audit_logs` (user_id, action, entity_type, entity_id, old_values, new_values) | ✅ Schema |
| Seasonal multiplier, tax on services | `services.seasonal_multiplier`, `services.tax_rate` | ✅ **003** |
| Base price, currency on services | `services.base_price`, `services.currency` | ✅ Schema |
| Hierarchical pricing (provider discount → company discount → commission) | Stored in bookings/payments (base_amount, discount_amount, commission_amount, total_amount) | ✅ Schema |

**Conclusion:** Commission and pricing storage needed by the plans is covered by schema + 003.

---

### 2.2 Payments & Refunds (PAYMENT_METHODS, DASHBOARD_DESIGN_REPORT)

| Requirement | Storage | Status |
|-------------|---------|--------|
| Multiple payment methods (Visa, Bank Transfer, Wallet, Cash on Service) | `payment_method_enum` | ✅ Schema + **003** (adds CASH_ON_SERVICE) |
| Idempotency for financial requests | `payments.idempotency_key` | ✅ **003**, **008** |
| Gateway response, transaction ref, 3DS | `payments.gateway_response`, `gateway_transaction_id`, `transaction_ref`; **008** adds `gateway_name`, `payment_token`, `error_message` | ✅ Schema + **008** |
| Refund with reason and audit | `refunds.reason`, `refunds.status`; audit_logs for trail | ✅ Schema; **011** (refund status VARCHAR); **012** (currency, transaction_reference) |
| Refund status (e.g. REQUESTED) for JPA | `refunds.status` as VARCHAR | ✅ **011** |

**Conclusion:** Payment and refund storage is covered.

---

### 2.3 RBAC & Role Management (ROLE_MANAGEMENT_REPORT, REQUIREMENTS_ANALYSIS)

| Requirement | Storage | Status |
|-------------|---------|--------|
| Roles with is_system_role, status (ACTIVE / PENDING_REASSIGNMENT) | `roles.is_system_role`, `roles.status` | ✅ **001**, **002** |
| Permissions with is_locked (system-only) | `permissions.is_locked` | ✅ **002** |
| Role level (hierarchy) as VARCHAR for JPA | `roles.level` | ✅ **011** |
| Groups, role–permission and user–role mapping | `groups`, `role_permissions`, `user_roles` | ✅ Schema |
| Custom roles, permission catalogue | Same tables | ✅ Schema |
| Role name/description in Arabic | `roles.name_ar`, `roles.description_ar` | ✅ **010** |

**Conclusion:** RBAC and role management storage is covered.

---

### 2.4 i18n & Multilingual (REQUIREMENTS_ANALYSIS, implementation_plan Phase 4)

| Requirement | Storage | Status |
|-------------|---------|--------|
| Static UI labels (EN/AR) | `i18n_labels` (key, en, ar, module) | ✅ **001** |
| Arabic columns on bookable/display entities | `*_ar` on departments, groups, roles, permissions, services, service_providers, discount_codes | ✅ **010** |
| Customer preferred currency | `customers.preferred_currency` | ✅ Schema |

**Conclusion:** i18n and multilingual storage is covered.

---

### 2.5 Auth & Sessions (REQUIREMENTS_ANALYSIS, PAYMENT_METHODS)

| Requirement | Storage | Status |
|-------------|---------|--------|
| Password reset tokens | `password_reset_tokens` | ✅ **009** |
| OTP verification (email/phone) | `otp_verification` | ✅ **009** |
| User sessions (token, device, expiry) | `sessions` | ✅ Schema |
| Login tracking | `users.last_login_at`, `users.last_login_ip` | ✅ Schema |
| User role/status as VARCHAR for JPA | `users.role`, `users.status` | ✅ **004** |

**Conclusion:** Auth and session storage is covered.

---

### 2.6 Dashboard & KPIs (DASHBOARD_DESIGN_REPORT, FRONTEND_IMPLEMENTATION_PLAN)

| Requirement | Storage | Status |
|-------------|---------|--------|
| Revenue, bookings, providers, complaints (KPIs) | Derived from `payments`, `bookings`, `service_providers`, `complaints` | ✅ Schema |
| Activity feed | Derived from audit_logs or event tables | ✅ audit_logs |
| Service health (by vertical) | Derived from `services` (type), `bookings` | ✅ Schema |
| Commission analysis (base vs commission) | `bookings.base_amount`, `commission_amount`, `total_amount`; `payments` | ✅ Schema |
| Transaction ledger (payments list, 3DS, gateway refs) | `payments` + **008** columns | ✅ |
| Refund reason and audit | `refunds.reason` + audit_logs | ✅ |
| Audit logs (user, IP, old/new, entity) | `audit_logs` | ✅ Schema |
| Ticket/complaint queue by priority and status | `complaints`, `internal_tickets`; **011** (priority/status VARCHAR) | ✅ **011** |
| Notifications (type, channel, status, message) | `notifications`; **011** (enum→VARCHAR); **012** (message, template_name, updated_at) | ✅ **011**, **012** |

**Conclusion:** Dashboard and KPI storage is covered by existing tables and 011/012.

---

### 2.7 Provider Management & Portal (DASHBOARD_DESIGN_REPORT, FRONTEND_IMPLEMENTATION_PLAN, INFRASTRUCTURE_REPORT)

| Requirement | Storage | Status |
|-------------|---------|--------|
| Provider list (vertical, status, commission) | `service_providers` (status, commission_rate); **007**, **011** (status VARCHAR) | ✅ |
| Commission override per provider | `service_providers.commission_rate` | ✅ **003** |
| Provider details (city, country, rating, verified) | `service_providers`; **007** (city, country, rating, review_count, verified) | ✅ **007** |
| Provider Arabic name/description | `service_providers.company_name_ar`, `description_ar` | ✅ **010** |
| Listings (services) per provider | `services.provider_id` | ✅ Schema |
| Bookings per provider | `bookings` → `services` → `provider_id` | ✅ Schema |
| **Provider payouts (money sent to providers)** | **No dedicated table** | ❌ **Gap** |
| **Provider document verification (licenses, contracts)** | **No table or columns** | ❌ **Gap** |
| **Provider–user linkage (which users belong to which provider for portal)** | **user_roles has no provider_id** | ❌ **Gap** |

**Conclusion:** Core provider and portal data is covered; **payouts**, **provider documents**, and **provider–user linkage** are gaps (see Section 3).

---

### 2.8 Bookings, Services, Discounts, Reviews, Taxi

| Requirement | Storage | Status |
|-------------|---------|--------|
| Bookings (status, amounts, dates, customer, service) | `bookings`; **011** (status VARCHAR) | ✅ |
| Services (type, status, seasonal_multiplier, tax_rate, name_ar) | `services`; **003**, **011**, **010** | ✅ |
| Discount codes (type, value, dates, usage, status) | `discount_codes`; **006**, **010**, **011** (status already in 006) | ✅ |
| Reviews (status VARCHAR) | `reviews`; **005** | ✅ |
| Taxi bookings (vehicle_type, status VARCHAR) | `taxi_bookings`; **011** | ✅ |
| Employees (level VARCHAR) | `employees`; **008** | ✅ |

**Conclusion:** All covered.

---

### 2.9 Departments, Notifications, Refunds (Entity Columns)

| Requirement | Storage | Status |
|-------------|---------|--------|
| Department manager | `departments.manager_id` | ✅ **012** |
| Notification message, template, updated_at | `notifications.message`, `template_name`, `updated_at`; backfill from content | ✅ **012** |
| Refund currency, transaction_reference | `refunds.currency`, `refunds.transaction_reference` | ✅ **012** |

**Conclusion:** Covered by **012**.

---

### 2.10 Optional / Future (Plans Mentioned, Not Required for Current Storage)

| Item | Plan reference | Status |
|------|----------------|--------|
| Table prefixes (sys_, pay_, hotel_) | MODULAR_MONOLITH_STRUCTURE, SYSTEM_EVOLUTION_REPORT | Intentionally not implemented; current schema is non-prefixed. Future migration if/when refactoring to prefixed tables. |
| DBMasterPermission (table-level access per role) | REQUIREMENTS_ANALYSIS (DB Explorer for G1) | Not in schema. Optional future migration if G1 “DB Explorer” needs table-level permissions. |
| Search in deleted/archived | FRONTEND_IMPLEMENTATION_PLAN Phase 6 | Schema already has `deleted_at` on users, service_providers, services; no migration change needed. |

---

## 3. Identified Gaps (Storage Not Yet Provided)

These plan-derived features expect data that the current schema and migrations **do not** provide. They can be addressed in **future migrations (013+)**.

### 3.1 Provider Payouts

- **Plans:** DASHBOARD_DESIGN_REPORT (Payout Ledger, scheduled payout summary), FRONTEND_IMPLEMENTATION_PLAN (Finance: Payout Ledger, pending payouts).
- **Gap:** No table for “money paid to providers” (amount, provider_id, date, status, reference). `payments` is for customer→company payments only.
- **Suggestion:** Add migration **013** (or similar) with a `provider_payouts` (or `payouts`) table: e.g. `id`, `provider_id`, `amount`, `currency`, `status`, `period_start`, `period_end`, `reference`, `processed_at`, `created_at`, etc.

### 3.2 Provider Document Verification

- **Plans:** DASHBOARD_DESIGN_REPORT (Document Verification: gallery for business licenses and ID documents), FRONTEND_IMPLEMENTATION_PLAN (Contract Viewer, document list).
- **Gap:** No table or columns for provider documents (e.g. license URL, contract URL, verification status, type).
- **Suggestion:** Either add columns on `service_providers` (e.g. `license_url`, `contract_url`, `documents_verified_at`) or add a `provider_documents` table (provider_id, document_type, file_url, verified_at, verified_by) in a future migration.

### 3.3 Provider–User Linkage (Portal Scoping)

- **Plans:** INFRASTRUCTURE_REPORT (Client Portal: “Automatically filters all backend queries by the provider_id tied to the provider's token”), FRONTEND_IMPLEMENTATION_PLAN (Provider-scoped staff, “their” team).
- **Gap:** `user_roles` has `user_id`, `role_id`, `group_id` but no `provider_id`. To scope “this user belongs to provider X” (for portal and “my staff”), either:
  - Add `provider_id` to `user_roles` (nullable; set when role is PROVIDER_*), or
  - Add a `provider_staff` (or `provider_users`) table: `user_id`, `provider_id`, optional role/scope.
- **Suggestion:** Add `provider_id` to `user_roles` in a future migration (nullable, FK to service_providers), or introduce `provider_staff`; then backend can filter by provider_id for portal APIs.

### 3.4 DBMasterPermission (Optional)

- **Plans:** REQUIREMENTS_ANALYSIS (entity “DBMasterPermission – Table access per role”), DASHBOARD_DESIGN_REPORT (G1 DB Explorer).
- **Gap:** No table for “role_id, table_name, can_read, can_write” (or similar).
- **Suggestion:** Only add if G1 DB Explorer is implemented and needs table-level access control; otherwise not required for current storage.

---

## 4. Summary Table

| Plan area | Storage need | Covered by schema + 001–012? | Gap? |
|-----------|--------------|------------------------------|------|
| Dynamic commission | provider commission_rate, audit | Yes (003, audit_logs) | No |
| Pricing (seasonal, tax) | services columns | Yes (003) | No |
| Payment methods, idempotency, gateway | payments columns | Yes (003, 008) | No |
| Refunds (reason, status, currency, ref) | refunds columns | Yes (011, 012) | No |
| RBAC (roles, permissions, status, level) | roles, permissions, user_roles | Yes (001, 002, 011) | No |
| i18n (_ar, i18n_labels) | i18n_labels, *_ar columns | Yes (001, 010) | No |
| Auth (reset, OTP, sessions) | password_reset_tokens, otp_verification, sessions | Yes (009, schema) | No |
| Dashboard KPIs, ledger, audit | bookings, payments, audit_logs, etc. | Yes (011, 012) | No |
| Tickets/complaints (priority, status) | complaints, internal_tickets | Yes (011) | No |
| Notifications (message, template, updated_at) | notifications | Yes (011, 012) | No |
| Provider payouts | — | No | **Yes** |
| Provider documents (licenses, contracts) | — | No | **Yes** |
| Provider–user linkage (portal scope) | — | No | **Yes** |
| DBMasterPermission | — | No | Optional |

---

## 5. Conclusion

- **Migrations 001–012**, together with the base **schema**, provide the database structures needed for:
  - Core business data (users, roles, permissions, groups, departments, employees, providers, services, bookings, payments, refunds, discounts, reviews, taxi bookings, complaints, internal tickets, notifications, audit logs, sessions, exchange rates).
  - Plan features: dynamic commission, pricing (seasonal, tax), payment methods and idempotency, refunds with reason, RBAC and role management, i18n and Arabic columns, auth (reset, OTP), dashboard KPIs and ledger, and JPA enum compatibility (011, 012).

- **Gaps** that require **future migrations**:
  1. **Provider payouts** – a table to record payouts to providers.
  2. **Provider document verification** – columns or a table for provider documents (licenses, contracts, verification).
  3. **Provider–user linkage** – a way to associate users with a specific provider (e.g. `provider_id` on `user_roles` or a `provider_staff` table) for portal scoping and “my staff”.

Once these are added (e.g. 013–015), the database will support storing data for the full set of functions described in the analyzed plans.

---

*Analysis based on schema.sql, migrations 001–012, and the listed plan documents. Last updated: 2026-03-11.*
