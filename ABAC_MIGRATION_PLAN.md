# Dynamic Roles Migration Plan
## Remove Static Role Enum — Pure Permission-Based Authorization

**Date:** 2026-06-06  
**Branch:** V1  
**Agreed decisions:**
- `sys_users.role` → 3 values only: `SUPER_ADMIN`, `CUSTOMER`, `STAFF`
- All authorization for STAFF is 100% permission-based (from `sys_roles → sys_role_permissions`)
- Portal static roles removed — super admin creates portal roles dynamically
- One role per user (one row in `sys_user_roles`)
- Role picker shows only roles from `sys_roles` (no static enum in the create-user form)
- SUPER_ADMIN stays hardcoded

---

## 1. How Authorization Works After This Migration

```
Login
│
├─ sys_users.role = SUPER_ADMIN
│    → ROLE_SUPER_ADMIN + ALL permissions (query SUPER_ADMIN sys_role permissions from DB)
│
├─ sys_users.role = CUSTOMER
│    → ROLE_CUSTOMER + no company permissions
│
└─ sys_users.role = STAFF
     → ROLE_STAFF
     → look up sys_user_roles (one row) → sys_role_permissions → sys_permissions
     → emit one GrantedAuthority per permission code
     → user has exactly the permissions of their assigned role, nothing more
```

Every `@PreAuthorize("hasAuthority('bookings:read')")` in every controller works
against that list. No static role checks for company or portal staff. No security
tier mapping. No fallback logic needed.

---

## 2. UserRole Enum — Before vs After

**Before (13 values):**
```java
CUSTOMER, SUPER_ADMIN, SALES_MANAGER, SALES_REPRESENTATIVE, FINANCE_MANAGER,
ACCOUNTANT, SUPPORT_MANAGER, SUPPORT_AGENT, CEO,
PROVIDER_MANAGER, PROVIDER_FINANCE, PROVIDER_STAFF, TAXI_OPERATOR
```

**After (3 values):**
```java
public enum UserRole {
    SUPER_ADMIN,
    CUSTOMER,
    STAFF;

    public boolean isCompanyStaff() {
        return this == STAFF;
    }

    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    public boolean isCustomer() {
        return this == CUSTOMER;
    }
}
```

All helper methods on the old enum (`isCompanyDashboardCreatable`,
`isCompanyDirectoryUser`, `getMaxDiscountPercentage`, `canApproveDiscounts`,
`companyDirectoryExcludedRoleNames`, `deriveSecurityUserRoleForCustomRole`) are
removed or replaced with permission/role-attribute checks described below.

---

## 3. Special Case — `getMaxDiscountPercentage`

The old enum had per-role discount limits (SALES_REPRESENTATIVE→20%,
FINANCE_MANAGER→50%, etc.). After removing those enum values, this logic moves
to the `sys_roles` table.

**Add column to `sys_roles`:**
```sql
ALTER TABLE sys_roles ADD COLUMN max_discount_pct SMALLINT NOT NULL DEFAULT 0;
```

The discount service reads this from the user's assigned role at runtime. The super
admin sets the limit when creating or editing a role. SUPER_ADMIN system role gets
`max_discount_pct = 100`.

This makes discount limits as dynamic as everything else — no code change needed
when a new role needs a different limit.

---

## 4. Portal Authorization — Before vs After

**Before:** Portal controllers use `hasAnyRole('PROVIDER_MANAGER', 'PROVIDER_FINANCE', ...)`
which checks the static `sys_users.role` value.

**After:** Portal access is a permission code like any other.

New permission codes added to `sys_permissions`:

| Code | Purpose |
|------|---------|
| `portal:access` | Can log into the partner portal (base gate) |
| `portal:manage` | Can manage portal settings, staff, listings |
| `portal:finance` | Can view earnings, payouts, financial data |
| `portal:taxi` | Can access taxi-specific portal features |

`ApiAuthorizationExpressions` portal constants change:

```java
// Before:
public static final String PROVIDER_PORTAL =
    "hasAnyRole('PROVIDER_MANAGER','PROVIDER_FINANCE','PROVIDER_STAFF','TAXI_OPERATOR')";
public static final String PORTAL_MANAGER =
    "hasRole('PROVIDER_MANAGER') or hasAuthority('PROVIDER_ROLE_MANAGER')";
public static final String PORTAL_FINANCE =
    "hasRole('PROVIDER_FINANCE') or hasAuthority('PROVIDER_ROLE_FINANCIAL_ADMIN')";

// After:
public static final String PROVIDER_PORTAL   = "hasAuthority('portal:access')";
public static final String PORTAL_MANAGER    = "hasAuthority('portal:manage')";
public static final String PORTAL_FINANCE    = "hasAuthority('portal:finance')";
public static final String PORTAL_TAXI       = "hasAuthority('portal:taxi')";
```

The super admin creates portal roles (e.g. "Provider Manager", "Provider Finance",
"Taxi Operator") with the relevant portal permission codes and assigns them to portal
users.

---

## 5. One Role Per User

**Before:** `sys_user_roles` could have multiple rows per user (primary + secondary
assignments). `findNewestRoleIdForUser` picks the latest one.

**After:** Enforce one row per user. Add a unique constraint:

```sql
ALTER TABLE sys_user_roles ADD CONSTRAINT uq_sys_user_roles_user_id UNIQUE (user_id);
```

`UserRoleAssignmentRepositoryAdapter.setPrimaryRoleForUser()` already deletes all
rows for the user before inserting the new one — this behavior is correct and kept.
Methods referencing multiple assignments per user are simplified.

---

## 6. Full File Change List

### 6.1 — New Files (create)

| File | Purpose |
|------|---------|
| `infrastructure/persistence/converter/UserRoleAttributeConverter.java` | Converts `sys_users.role` safely; old values map to `STAFF` during transition |
| `core/src/main/resources/db/migration/V55__dynamic_roles_migration.sql` | Full data migration (see §7) |

### 6.2 — Modified Files (backend)

| File | Change |
|------|--------|
| `domain/enums/UserRole.java` | 13 → 3 values; replace helper methods |
| `infrastructure/persistence/entity/UserJpaEntity.java` | Replace `@Enumerated` with `@Convert(converter = UserRoleAttributeConverter.class)` |
| `infrastructure/security/CustomUserDetailsService.java` | 3-branch logic; remove portal staff branch; remove `providerStaffRepository` dependency |
| `infrastructure/security/ApiAuthorizationExpressions.java` | Update portal constants; update `isCompanyStaff()` to check `ROLE_STAFF`; remove `ROLES_READ/WRITE` if unused |
| `application/command/UserCommandHandler.java` | Remove security-tier resolution; create always sets role=STAFF; remove `RoleRepository`, `CompanyStaffRoleCatalogService` dependencies |
| `application/dto/request/CreateUserRequest.java` | Remove `role` (UserRole) field; keep `primaryRbacRoleId`; `sys_users.role` defaults to `STAFF` server-side |
| `application/dto/request/UpdateUserRequest.java` | Remove `role` (UserRole) field; role changes go through a separate role-assignment endpoint |
| `application/dto/UserResponse.java` | `role` field type stays String but only ever SUPER_ADMIN / CUSTOMER / STAFF |
| `application/service/CompanyStaffRoleCatalogService.java` | Remove `deriveSecurityUserRoleForCustomRole`, `resolveSecurityUserRoleForPrimaryRbacRole`; simplify `listDashboardCreatableRoles` to return all active `sys_roles` (no UserRole filter) |
| `application/service/RoleManagementService.java` | Remove UserRole-based filters from role listing; add `max_discount_pct` to create/update; remove SUPER_ADMIN level guard (now handled by system-role flag) |
| `application/query/UserQueryHandler.java` | Update company-staff filter: `WHERE role = 'STAFF'` instead of the old exclusion list |
| `application/query/GroupMembersQueryHandler.java` | Same filter update |
| `infrastructure/persistence/adapter/UserRoleAssignmentRepositoryAdapter.java` | Remove multi-row logic; simplify; keep `setPrimaryRoleForUser` and `findPermissionCodesByUserId` |
| `domain/repository/UserRoleAssignmentRepository.java` | Remove `findNewestRoleIdForUser`, `reassignAllToRole`; keep `setPrimaryRoleForUser`, `findPermissionCodesByUserId`, `clearAssignmentsForUser` |
| `infrastructure/persistence/repository/UserRoleJpaRepository.java` | Remove multi-row queries; enforce single-row assumption |

### 6.3 — Deleted Files (backend)

| File | Why |
|------|-----|
| `domain/enums/RoleLevel.java` | Only used for security-tier mapping (`deriveSecurityUserRoleForCustomRole`); removed |
| `infrastructure/security/SecurityRoleUtils.java` | Only references old enum values |

> **Note:** `RoleLevel` is currently a column on `sys_roles` (`level VARCHAR(30)`). The
> column stays in the DB (it's useful as a display label: EMPLOYEE / MANAGER / EXECUTIVE)
> but the Java enum `RoleLevel` that drove the security tier mapping is deleted. The
> column value becomes purely informational — no code branches on it for authorization.

### 6.4 — Modified Files (frontend)

| File | Change |
|------|--------|
| `src/types/api.ts` | `UserDto.role` type: `'SUPER_ADMIN' \| 'CUSTOMER' \| 'STAFF'`; remove `primaryRbacRoleId` from UpdateUserRequest type; keep it on CreateUserRequest |
| `src/pages/management/UsersPage.tsx` | Create-user form: replace static UserRole dropdown with a role picker that calls `GET /api/admin/roles` (sys_roles list); remove security-tier display |
| `src/pages/management/StaffUserDetailPage.tsx` | Role display shows the assigned `sys_roles` name, not the UserRole enum value |
| `src/i18n/translations.ts` | Remove references to old role enum display names (CEO, Sales Manager, etc.) that were shown in the role picker |
| Navigation / sidebar | Remove any `isCompanyDirectoryUser` / `isProviderPortalRole` checks based on old enum; replace with permission checks |

---

## 7. V55 Migration — Full Detail

```sql
-- ============================================================================
-- V55 — Dynamic Roles Migration
-- Converts sys_users.role from 13-value enum to 3-value system.
-- Seeds portal permissions and portal system roles.
-- Enforces one-role-per-user.
-- ============================================================================

-- ── 1. Add max_discount_pct to sys_roles ─────────────────────────────────────

ALTER TABLE sys_roles
  ADD COLUMN IF NOT EXISTS max_discount_pct SMALLINT NOT NULL DEFAULT 0;

-- Set sensible defaults for existing system roles
UPDATE sys_roles SET max_discount_pct = 100 WHERE code = 'SUPER_ADMIN';
UPDATE sys_roles SET max_discount_pct = 100 WHERE code = 'CEO';
UPDATE sys_roles SET max_discount_pct = 50  WHERE code = 'FINANCE_MANAGER';
UPDATE sys_roles SET max_discount_pct = 30  WHERE code = 'ACCOUNTANT';
UPDATE sys_roles SET max_discount_pct = 20  WHERE code = 'SALES_REPRESENTATIVE';
UPDATE sys_roles SET max_discount_pct = 20  WHERE code = 'SALES_MANAGER';

-- ── 2. Seed portal permission codes ──────────────────────────────────────────

INSERT INTO sys_permissions (code, name, resource, action, scope, is_locked)
VALUES
  ('portal:access',  'Portal Access',         'portal', 'access',  'ALL', FALSE),
  ('portal:manage',  'Portal Management',     'portal', 'manage',  'ALL', FALSE),
  ('portal:finance', 'Portal Finance Access', 'portal', 'finance', 'ALL', FALSE),
  ('portal:taxi',    'Portal Taxi Access',    'portal', 'taxi',    'ALL', FALSE)
ON CONFLICT (code) DO NOTHING;

-- ── 3. Create portal system roles in sys_roles ───────────────────────────────
-- These replace the static enum values and give the super admin
-- a sensible starting set to assign to portal users.

INSERT INTO sys_roles (name, name_ar, code, level, is_system_role, status)
VALUES
  ('Provider Manager', 'مدير الشريك',      'PROVIDER_MANAGER', 'MANAGER',  TRUE, 'ACTIVE'),
  ('Provider Finance', 'مالية الشريك',     'PROVIDER_FINANCE', 'EMPLOYEE', TRUE, 'ACTIVE'),
  ('Provider Staff',   'موظف الشريك',      'PROVIDER_STAFF',   'EMPLOYEE', TRUE, 'ACTIVE'),
  ('Taxi Operator',    'مشغل سيارات الأجرة','TAXI_OPERATOR',   'EMPLOYEE', TRUE, 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- Assign portal permissions to portal system roles
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sys_roles r, sys_permissions p
WHERE r.code = 'PROVIDER_MANAGER'
  AND p.code IN ('portal:access', 'portal:manage', 'portal:finance')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sys_roles r, sys_permissions p
WHERE r.code = 'PROVIDER_FINANCE'
  AND p.code IN ('portal:access', 'portal:finance')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sys_roles r, sys_permissions p
WHERE r.code = 'PROVIDER_STAFF'
  AND p.code IN ('portal:access')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM sys_roles r, sys_permissions p
WHERE r.code = 'TAXI_OPERATOR'
  AND p.code IN ('portal:access', 'portal:taxi')
ON CONFLICT DO NOTHING;

-- ── 4. Create sys_user_roles rows for existing portal users ──────────────────
-- Portal users had no sys_user_roles rows (their auth was via sys_users.role).
-- Give each one a row pointing to the matching portal system role.

INSERT INTO sys_user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, now()
FROM sys_users u
JOIN sys_roles r ON r.code = u.role AND r.is_system_role = TRUE
WHERE u.role IN ('PROVIDER_MANAGER','PROVIDER_FINANCE','PROVIDER_STAFF','TAXI_OPERATOR')
  AND NOT EXISTS (SELECT 1 FROM sys_user_roles ur WHERE ur.user_id = u.id)
ON CONFLICT DO NOTHING;

-- ── 5. Deduplicate sys_user_roles — keep only the newest row per user ─────────

DELETE FROM sys_user_roles
WHERE id NOT IN (
  SELECT DISTINCT ON (user_id) id
  FROM sys_user_roles
  ORDER BY user_id, assigned_at DESC
);

-- ── 6. Add unique constraint — one role per user ──────────────────────────────

ALTER TABLE sys_user_roles
  ADD CONSTRAINT uq_sys_user_roles_user_id UNIQUE (user_id);

-- ── 7. Migrate sys_users.role to 3-value system ───────────────────────────────
-- SUPER_ADMIN and CUSTOMER stay as-is.
-- Everything else (all 11 other values) becomes STAFF.

UPDATE sys_users
SET role = 'STAFF'
WHERE role NOT IN ('SUPER_ADMIN', 'CUSTOMER');

-- ── 8. Add CHECK constraint to prevent future unknown values ──────────────────

ALTER TABLE sys_users
  ADD CONSTRAINT ck_sys_users_role
  CHECK (role IN ('SUPER_ADMIN', 'CUSTOMER', 'STAFF'));
```

---

## 8. UserRoleAttributeConverter (handles in-flight transition)

```java
@Converter
public class UserRoleAttributeConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return UserRole.STAFF;
        return switch (dbData.trim()) {
            case "SUPER_ADMIN" -> UserRole.SUPER_ADMIN;
            case "CUSTOMER"    -> UserRole.CUSTOMER;
            default            -> UserRole.STAFF; // covers old values during migration window
        };
    }
}
```

After V55 runs, no row will ever hit the `default` branch. It is purely a safety net
for the window between app deploy and migration run.

---

## 9. CustomUserDetailsService — After

```java
@Override
public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
    UUID id = UUID.fromString(userId);
    User user = userRepository.findById(id)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

    List<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

    switch (user.getRole()) {
        case SUPER_ADMIN -> {
            // SUPER_ADMIN gets all permissions from its sys_roles entry
            for (String code : userRoleAssignmentRepository.findPermissionCodesBySystemRoleCode("SUPER_ADMIN")) {
                authorities.add(new SimpleGrantedAuthority(code));
            }
        }
        case STAFF -> {
            // Permissions come entirely from the user's assigned role
            for (String code : userRoleAssignmentRepository.findPermissionCodesByUserId(id)) {
                authorities.add(new SimpleGrantedAuthority(code));
            }
        }
        case CUSTOMER -> {
            // No company permissions — customer portal uses its own auth
        }
    }

    return new UserPrincipal(
        user.getId(),
        user.getPasswordHash(),
        user.getTokenVersion(),
        user.getStatus().canLogin(),
        !user.isLocked(),
        authorities
    );
}
```

`providerStaffRepository` dependency is removed entirely. No more `PROVIDER_ROLE_X`
authorities. Portal checks use `hasAuthority('portal:access')` etc.

---

## 10. UserCommandHandler.create() — After

```java
@Transactional
public UUID create(CreateUserRequest request) {
    // primaryRbacRoleId is now always required for staff creation
    if (request.getPrimaryRbacRoleId() == null) {
        throw new IllegalArgumentException("primaryRbacRoleId is required.");
    }
    Role rbac = roleRepository.findById(request.getPrimaryRbacRoleId())
        .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    if (rbac.getStatus() != RoleStatus.ACTIVE) {
        throw new IllegalArgumentException("Role is not active.");
    }
    // sys_users.role is always STAFF — the role name/permissions come from sys_user_roles
    User saved = createInternal(request, UserRole.STAFF, rbac.getId());
    // ... notification publish
    return saved.getId();
}
```

`CompanyStaffRoleCatalogService`, `RoleRepository` injected into `UserCommandHandler`
are removed. The only role lookup needed is `roleRepository.findById()` to validate
the role exists and is active.

---

## 11. Execution Order

```
Step 1   Shrink UserRole enum to 3 values; rewrite helper methods
Step 2   Create UserRoleAttributeConverter; patch UserJpaEntity (@Convert)
Step 3   Rewrite CustomUserDetailsService (3-branch switch)
Step 4   Add findPermissionCodesBySystemRoleCode to:
           UserRoleAssignmentRepository (interface)
           UserRoleAssignmentRepositoryAdapter
           UserRoleJpaRepository (native SQL)
Step 5   Simplify UserCommandHandler.create() and update()
Step 6   Simplify CompanyStaffRoleCatalogService
           (remove deriveSecurityUserRole; listDashboardCreatableRoles returns all active sys_roles)
Step 7   Update ApiAuthorizationExpressions (portal constants + isCompanyStaff)
Step 8   Update UserQueryHandler + GroupMembersQueryHandler (role filter: STAFF only)
Step 9   Delete RoleLevel.java, SecurityRoleUtils.java
Step 10  Add max_discount_pct field to Role domain entity + RoleJpaEntity + RoleManagementService
Step 11  Update CreateUserRequest (remove role field, require primaryRbacRoleId)
Step 12  Update UpdateUserRequest (remove role field)
Step 13  Frontend — update api.ts types
Step 14  Frontend — update UsersPage create-user form (role picker from sys_roles)
Step 15  Frontend — update StaffUserDetailPage role display
Step 16  Write V55 migration SQL
Step 17  Run test suite (./gradlew test)
Step 18  Apply V55 to the database
Step 19  Restart backend — all users load cleanly from the new 3-value column
```

---

## 12. What Does NOT Change

- `sys_roles`, `sys_permissions`, `sys_role_permissions`, `sys_user_roles`, `sys_groups` — kept
- All `@PreAuthorize("hasAuthority('...')")` expressions in every controller — zero changes
- `RoleManagementController`, `AdminPermissionsController` — kept (role/permission CRUD UI)
- `RoleManagementService` create/update/delete role — kept (minus security-tier logic)
- `GroupManagementService` — kept entirely
- All frontend RBAC pages (`RolesPage`, `RoleMembersPage`, `GroupMembersPage`) — kept
- The permission assignment workflow in the UI — kept
- `@userSecurity.isSelf(#id)` self-access checks — kept
- Password, MFA, audit-log, booking, payment, notification tables — untouched

---

## 13. Risk Notes

| Risk | Mitigation |
|------|-----------|
| Portal users have no `sys_user_roles` row → locked out after V55 runs `UPDATE sys_users.role` | V55 Step 4 creates `sys_user_roles` rows for portal users BEFORE Step 7 changes `sys_users.role` |
| `sys_user_roles` deduplication (Step 5) deletes rows — user loses permissions | Dedup keeps the newest row (ORDER BY assigned_at DESC); only duplicates are removed |
| `RoleLevel` enum deleted but `level` column on `sys_roles` still has values | Column stays as a VARCHAR display label; only the Java enum is removed |
| Old frontend code sends `role: 'CEO'` in create-user request | Backend `CreateUserRequest` removes the `role` field; API returns 400 if sent; frontend updated in Step 14 |
| `max_discount_pct` column added — discount service must be updated to read it | Discount service reads from the role at runtime; old hard-coded enum logic removed in same PR |
| V55 `CHECK CONSTRAINT` blocks app startup if Spring tries to write an old enum value | Converter deployed in Step 2 ensures only SUPER_ADMIN/CUSTOMER/STAFF are ever written; constraint added last in V55 |
