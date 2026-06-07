# Ziyara — Roles & Permissions Reference

> Generated from source: `core/src/main/java`, `core/src/main/resources/db/migration`
> Last updated: 2026-05-30

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Organizational Groups](#2-organizational-groups)
3. [Built-in System Roles](#3-built-in-system-roles)
4. [Permission Catalog](#4-permission-catalog)
5. [Role → Permission Matrix](#5-role--permission-matrix)
6. [Sidebar Navigation Access](#6-sidebar-navigation-access)
7. [Custom RBAC Roles](#7-custom-rbac-roles)
8. [Authorization Patterns](#8-authorization-patterns)
9. [Super Admin Seeded Accounts](#9-super-admin-seeded-accounts)

---

## 1. Architecture Overview

Ziyara uses a **two-track authorization model**:

| Track | Applies to | How permissions are checked |
|---|---|---|
| **Built-in JWT roles** | All 15 `UserRole` enum values | Permissions seeded to `sys_role_permissions` via DB migrations; loaded into JWT |
| **Custom RBAC roles** | Company staff with a custom role assigned in `sys_user_roles` | Permissions assigned at role creation / edit through the Roles UI |

Both tracks are evaluated identically at runtime via `hasAuthority('permission:code')` SpEL expressions. `hasRole('SUPER_ADMIN')` is reserved only for the most sensitive break-glass operations (role management, PII registry, API keys).

### Data model

```
sys_groups (id, name, code)
    └── sys_roles (id, name, code, level, group_id, is_system_role, navigation_item_ids)
            └── sys_role_permissions (role_id, permission_id)
                    └── sys_permissions (id, code, resource, action, is_locked)

sys_users (id, email, username, role [enum], ...)
    └── sys_user_roles (user_id, role_id, group_id)   ← RBAC assignment
```

---

## 2. Organizational Groups

Groups are organizational units. Every system role belongs to one group; custom roles can optionally belong to a group. Users are directory-listed under their role's group.

| Code | Name | Purpose |
|---|---|---|
| **C1** | Admin | Platform administration (Super Admin accounts) |
| *(ungrouped)* | — | Custom roles without a group assignment |

> Earlier releases had Z1–Z7 groups (Executive, Sales, Finance, Support, HR, Provider, B2C). These were consolidated into C1 (Admin) in migration V26. Custom groups can be created by the Super Admin from the Roles page.

---

## 3. Built-in System Roles

These roles are seeded by migration V17 and cannot be deleted. They are identified by `is_system_role = true` and fixed UUIDs (`c0000000-...`).

### 3.1 Platform Roles

| Role code | Display name | Level | Group | Notes |
|---|---|---|---|---|
| `SUPER_ADMIN` | Super Admin | SUPER_ADMIN | C1 – Admin | All 41 permissions. Full system access. |
| `CEO` | CEO | EXECUTIVE | C1 – Admin | Full business access; no `roles:write`, no `system:*`. |
| `GENERAL_MANAGER` | General Manager | EXECUTIVE | C1 – Admin | Broad access; no `users:write`, no `discounts:approve`. |

### 3.2 Functional Roles

| Role code | Display name | Level | Group |
|---|---|---|---|
| `SALES_MANAGER` | Sales Manager | MANAGER | C1 – Admin |
| `SALES_REPRESENTATIVE` | Sales Representative | EMPLOYEE | C1 – Admin |
| `FINANCE_MANAGER` | Finance Manager | MANAGER | C1 – Admin |
| `ACCOUNTANT` | Accountant | EMPLOYEE | C1 – Admin |
| `SUPPORT_MANAGER` | Support Manager | MANAGER | C1 – Admin |
| `SUPPORT_AGENT` | Support Agent | EMPLOYEE | C1 – Admin |
| `HR_MANAGER` | HR Manager | MANAGER | C1 – Admin |

### 3.3 Provider Portal Roles

These roles are for the partner/provider dashboard. They are NOT company directory users.

| Role code | Display name | Level | Notes |
|---|---|---|---|
| `PROVIDER_MANAGER` | Provider Manager | MANAGER | Provider portal admin |
| `PROVIDER_FINANCE` | Provider Finance | EMPLOYEE | Provider billing access |
| `PROVIDER_STAFF` | Provider Staff | EMPLOYEE | Limited portal access |
| `TAXI_OPERATOR` | Taxi Operator | EMPLOYEE | Taxi booking tracking |

### 3.4 B2C Role

| Role code | Display name | Level | Notes |
|---|---|---|---|
| `CUSTOMER` | Customer | EMPLOYEE | End-user booking app. No company dashboard access. |

---

## 4. Permission Catalog

All 41 permissions are seeded by migration V17 into `sys_permissions`. The `is_locked` flag historically restricted a permission to system roles only; **this restriction has been removed** — all permissions can now be assigned to any custom role.

### 4.1 Domain Permissions (39 — unlocked)

| Permission code | Resource | Action | Description |
|---|---|---|---|
| `bookings:read` | bookings | read | View bookings |
| `bookings:write` | bookings | write | Create / modify / cancel bookings |
| `users:read` | users | read | View company staff directory |
| `users:write` | users | write | Create / modify / delete staff accounts |
| `providers:read` | providers | read | View provider listings and profiles |
| `providers:write` | providers | write | Approve / reject / suspend providers |
| `payments:read` | payments | read | View payment transactions |
| `payments:write` | payments | write | Process refunds and manual payments |
| `discounts:read` | discounts | read | View discount codes |
| `discounts:write` | discounts | write | Create / modify discount codes |
| `discounts:approve` | discounts | approve | Approve / activate discount codes |
| `reports:read` | reports | read | View revenue and booking reports |
| `analytics:read` | analytics | read | View analytics dashboard |
| `roles:read` | roles | read | View roles and permission catalog |
| `roles:write` | roles | write | Create / edit / delete custom roles |
| `settings:read` | settings | read | View system settings |
| `settings:write` | settings | write | Modify system settings |
| `audit:read` | audit | read | View audit logs |
| `content:read` | content | read | View CMS content pages |
| `content:write` | content | write | Edit CMS content pages |
| `services:read` | services | read | View service listings (hotels, restaurants, etc.) |
| `services:write` | services | write | Create / modify / approve services |
| `taxi:read` | taxi | read | View taxi trips and operators |
| `taxi:write` | taxi | write | Manage taxi bookings and operators |
| `currency:read` | currency | read | View exchange rates |
| `currency:write` | currency | write | Update exchange rates |
| `tickets:read` | tickets | read | View provider support messages |
| `tickets:write` | tickets | write | Reply to provider support messages |
| `complaints:read` | complaints | read | View customer complaints |
| `complaints:write` | complaints | write | Resolve / escalate complaints |
| `reviews:read` | reviews | read | View service reviews |
| `reviews:write` | reviews | write | Moderate reviews |
| `customers:read` | customers | read | View B2C customer accounts |
| `customers:write` | customers | write | Manage B2C customer accounts |
| `internal_tickets:read` | internal_tickets | read | View internal bug / task tickets |
| `internal_tickets:write` | internal_tickets | write | Create and manage internal tickets |
| `portal:read` | portal | read | View provider portal data |
| `api_docs:read` | api_docs | read | Access Swagger / API documentation |
| `leads:read` | leads | read | View sales leads |
| `leads:write` | leads | write | Manage sales leads |
| `deleted_items:read` | deleted_items | read | View soft-deleted records |
| `deleted_items:restore` | deleted_items | restore | Restore soft-deleted records |

### 4.2 System Permissions (2 — previously locked)

> These were formerly restricted to the SUPER_ADMIN built-in role only. The lock has been removed; they can now be assigned to any custom role via the Roles UI.

| Permission code | Resource | Action | Description |
|---|---|---|---|
| `system:super_ops` | system | super_ops | Break-glass system operations (PII registry, recovery) |
| `system:bulk_export` | system | bulk_export | Full database / bulk data export |

---

## 5. Role → Permission Matrix

`✓` = permission assigned by default. SUPER_ADMIN has all permissions (implicit).

| Permission | CEO | GM | Sales Mgr | Sales Rep | Finance Mgr | Accountant | Support Mgr | Support Agent | HR Mgr |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `bookings:read` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | |
| `bookings:write` | ✓ | ✓ | ✓ | | | | ✓ | | |
| `users:read` | ✓ | ✓ | | | | | ✓ | | ✓ |
| `users:write` | ✓ | | | | | | | | ✓ |
| `providers:read` | ✓ | ✓ | ✓ | ✓ | ✓ | | | | |
| `providers:write` | ✓ | ✓ | ✓ | | | | | | |
| `payments:read` | ✓ | ✓ | | | ✓ | ✓ | | | |
| `payments:write` | ✓ | ✓ | | | ✓ | | | | |
| `discounts:read` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | | |
| `discounts:write` | ✓ | ✓ | ✓ | ✓ | | | | | ✓ |
| `discounts:approve` | ✓ | | | | ✓ | | | | |
| `reports:read` | ✓ | ✓ | ✓ | | ✓ | ✓ | | | |
| `analytics:read` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | |
| `roles:read` | ✓ | ✓ | | | | | | | ✓ |
| `roles:write` | | | | | | | | | |
| `settings:read` | ✓ | ✓ | | | | | | | |
| `settings:write` | ✓ | ✓ | | | | | | | |
| `audit:read` | ✓ | ✓ | | | ✓ | | | | ✓ |
| `content:read` | ✓ | ✓ | ✓ | ✓ | | | | | |
| `content:write` | ✓ | ✓ | ✓ | ✓ | | | | | |
| `services:read` | ✓ | ✓ | ✓ | ✓ | | | | | |
| `services:write` | ✓ | ✓ | ✓ | ✓ | | | | | |
| `taxi:read` | ✓ | ✓ | ✓ | ✓ | | | | | |
| `taxi:write` | ✓ | ✓ | ✓ | | | | | | |
| `currency:read` | ✓ | ✓ | | | ✓ | ✓ | | | |
| `currency:write` | ✓ | ✓ | | | ✓ | ✓ | | | |
| `tickets:read` | ✓ | ✓ | | | | | ✓ | ✓ | |
| `tickets:write` | ✓ | ✓ | | | | | ✓ | ✓ | |
| `complaints:read` | ✓ | ✓ | | | | | ✓ | ✓ | |
| `complaints:write` | ✓ | ✓ | | | | | ✓ | ✓ | |
| `reviews:read` | ✓ | ✓ | ✓ | ✓ | | | ✓ | ✓ | |
| `reviews:write` | ✓ | ✓ | ✓ | | | | ✓ | | |
| `customers:read` | ✓ | ✓ | ✓ | | | | ✓ | ✓ | |
| `customers:write` | ✓ | ✓ | | | | | ✓ | | |
| `internal_tickets:read` | ✓ | ✓ | | | | | ✓ | | ✓ |
| `internal_tickets:write` | ✓ | | | | | | ✓ | | ✓ |
| `portal:read` | ✓ | ✓ | | | | | | | |
| `api_docs:read` | ✓ | ✓ | | | | | | | |
| `leads:read` | ✓ | ✓ | ✓ | ✓ | | | | | |
| `leads:write` | ✓ | ✓ | ✓ | ✓ | | | | | |
| `deleted_items:read` | ✓ | ✓ | ✓ | | | | | | |
| `deleted_items:restore` | ✓ | | | | | | | | |
| `system:super_ops` | | | | | | | | | |
| `system:bulk_export` | | | | | | | | | |

> **GM** = General Manager. `roles:write` and both `system:*` are SUPER_ADMIN–only by default.

---

## 6. Sidebar Navigation Access

The company dashboard sidebar is controlled by two mechanisms:

- **Built-in roles** → use `SidebarSurface` enum → maps to a fixed set of section IDs
- **Custom RBAC roles** → use `navigation_item_ids` JSONB on `sys_roles` → super admin assigns individual items

### 6.1 Default sidebar sections per built-in role

| SidebarSurface | Built-in roles mapped | Sections |
|---|---|---|
| `SUPER_ADMIN` | SUPER_ADMIN | main, services, management, **admin** |
| `EXECUTIVE` | CEO, GENERAL_MANAGER | main, services, management, support |
| `ADMIN` | SALES_MANAGER, SALES_REP, GENERAL_MANAGER | main, services, management, support |
| `FINANCE` | FINANCE_MANAGER, ACCOUNTANT | main, management |
| `SUPPORT` | SUPPORT_MANAGER, SUPPORT_AGENT | main, support |
| `HR` | HR_MANAGER | main, management, **admin** (groups + logs only) |
| `PROVIDER` | PROVIDER_MANAGER, PROVIDER_FINANCE, PROVIDER_STAFF, TAXI_OPERATOR | main, services |
| `USER` | CUSTOMER | main, services |

### 6.2 Navigation items in the catalog

These are all items that can be assigned to a custom role's visible navigation (`navigation_item_ids`):

| ID | Label | Section | Restricted to |
|---|---|---|---|
| `dashboard` | Dashboard | MAIN | — |
| `main_find_customer` | Search users | MAIN | SUPER_ADMIN |
| `main_deleted_items` | Search deleted items | MAIN | SUPER_ADMIN |
| `hotels` | Hotels | SERVICES | — |
| `resorts` | Resorts | SERVICES | — |
| `restaurants` | Restaurants | SERVICES | — |
| `taxis` | Taxis | SERVICES | — |
| `trips` | Trips | SERVICES | — |
| `providers` | Providers | MANAGEMENT | — |
| `bookings` | Bookings | MANAGEMENT | — |
| `payments` | Payments | MANAGEMENT | — |
| `discounts` | Discounts | MANAGEMENT | — |
| `reports` | Reports | MANAGEMENT | — |
| `taxi_trips` | Taxi trips | MANAGEMENT | — |
| `currency_rates` | Currency rates | MANAGEMENT | SUPER_ADMIN, EXECUTIVE, FINANCE |
| `complaints` | Complaints | SUPPORT | — |
| `reviews` | Reviews | SUPPORT | — |
| `provider_messages` | Provider Messages | SUPPORT | — |
| `settings` | Settings | ADMIN | — |
| `users` | Groups | ADMIN | SUPER_ADMIN, HR |
| `roles` | Roles | ADMIN | — |
| `logs` | Logs | ADMIN | — |
| `content` | Content | ADMIN | SUPER_ADMIN, ADMIN |
| `api` | API | ADMIN | SUPER_ADMIN |
| `integrations` | Integrations | ADMIN | SUPER_ADMIN, EXECUTIVE, ADMIN |
| `subscriptions` | Subscriptions | ADMIN | SUPER_ADMIN, EXECUTIVE, ADMIN |

---

## 7. Custom RBAC Roles

Custom roles are created by the Super Admin from **Admin → Roles**. They have `is_system_role = false` and a generated code prefixed with `CUSTOM_`.

### Properties

| Field | Description |
|---|---|
| `name` | Display name (EN) |
| `nameAr` | Display name (AR) |
| `code` | Auto-generated from name: `CUSTOM_<SLUG>` |
| `level` | Always `EMPLOYEE` |
| `groupId` | Optional — assign to an org group |
| `permissionIds` | Any of the 41 permissions (all unlocked) |
| `navigationItemIds` | Ordered list of sidebar item IDs |
| `status` | `ACTIVE` or `PENDING_REASSIGNMENT` |

### Lifecycle

1. Super Admin creates role → selects permissions + optional navigation items
2. Super Admin assigns role to a staff user from the user detail page
3. User logs in → JWT includes custom role authorities from `sys_role_permissions`
4. Sidebar renders based on `navigation_item_ids` JSONB

### Deletion rules

- If users are assigned to the role, a `targetRoleId` must be provided to reassign them before deletion
- Platform Z-group roles cannot be deleted

---

## 8. Authorization Patterns

### Endpoint-level gates

```java
// Class-level: any authenticated company staff member
@PreAuthorize(COMPANY_STAFF)

// Method-level: specific permission
@PreAuthorize(BOOKINGS_WRITE)       // hasAuthority('bookings:write')
@PreAuthorize(USERS_READ)           // hasAuthority('users:read')
@PreAuthorize(REPORTS_READ)         // hasAuthority('reports:read')

// Super-admin only (break-glass)
@PreAuthorize("hasRole('SUPER_ADMIN')")

// Provider portal
@PreAuthorize(PROVIDER_PORTAL)
```

### Permission constant map (`ApiAuthorizationExpressions.java`)

| Constant | SpEL expression |
|---|---|
| `BOOKINGS_READ` | `hasAuthority('bookings:read')` |
| `BOOKINGS_WRITE` | `hasAuthority('bookings:write')` |
| `USERS_READ` | `hasAuthority('users:read')` |
| `USERS_WRITE` | `hasAuthority('users:write')` |
| `USERS_SELF_OR_READ` | `#id == principal.id or hasAuthority('users:read')` |
| `USERS_SELF_OR_WRITE` | `#id == principal.id or hasAuthority('users:write')` |
| `CONTENT_PAGE_EDITOR` | `hasAuthority('content:write')` |
| `DISCOUNT_CREATE` | `hasAuthority('discounts:write')` |
| `DISCOUNT_APPROVE` | `hasAuthority('discounts:approve')` |
| `PROVIDER_SUBMIT` | `hasAuthority('providers:write')` |
| `PROVIDER_APPROVE_OR_REJECT` | `hasAuthority('providers:write')` |
| `PROVIDER_COMMISSION` | `hasAuthority('providers:write')` |
| `PAYMENTS_READ` | `hasAuthority('payments:read')` |
| `PAYMENTS_WRITE` | `hasAuthority('payments:write')` |
| `PAYMENTS_REFUND` | `hasAuthority('payments:write')` |
| `CURRENCY_READ` | `hasAuthority('currency:read')` |
| `CURRENCY_WRITE` | `hasAuthority('currency:write')` |
| `REPORTS_READ` | `hasAuthority('reports:read')` |
| `SETTINGS_READ` | `hasAuthority('settings:read')` |
| `SETTINGS_WRITE` | `hasAuthority('settings:write')` |
| `SUBSCRIPTIONS_READ` | `hasAuthority('providers:read')` |
| `PORTAL_MANAGER` | `hasAuthority('portal:write')` |
| `PORTAL_FINANCE` | `hasAuthority('portal:read')` |

---

## 9. Super Admin Seeded Accounts

Three SUPER_ADMIN accounts are automatically created (or password-reset) on every backend startup by `SuperAdminSeeder.java`. All share the same bootstrap password.

| Email | Username | Role | Group |
|---|---|---|---|
| `super_admin@ziyarah.com` | Administrator | SUPER_ADMIN | C1 – Admin |
| `admin@ziyarah.com` | Admin | SUPER_ADMIN | C1 – Admin |
| `developer@ziyarah.com` | Developer | SUPER_ADMIN | C1 – Admin |

**Password source** (in priority order):
1. `APP_DEMO_PASSWORD` environment variable (set this for a stable password)
2. Auto-generated random password — printed to the startup log:
   ```
   Generated APP bootstrap password for local/test usage (...): <password>
   ```

> In production (`spring.profiles.active=prod`) the `APP_DEMO_PASSWORD` env var is **required**. If it is not set the application will fail to start.
