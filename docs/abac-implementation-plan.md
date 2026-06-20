# ABAC Bug Fix — Implementation Plan

## Answered Open Questions

| Question | Decision |
|---|---|
| GroupMembersPage gate | **`users:read`** — align both sidebar and backend to the same permission |
| webhooks:read / webhooks:write in DB | **Seed them** in a new Flyway migration; treat webhooks as first-class ABAC permissions |
| Super admin nav bypass | **Keep** — super admin always sees all tabs; bypass stays |
| Select-All preset scope | **Exclude locked permissions** — Full Access only grants items whose `read` code is not locked |
| Custom role UserRole (from code) | `EMPLOYEE` level → `SALES_REPRESENTATIVE` → frontend `'admin'`; `MANAGER` → `SALES_MANAGER` → `'admin'`; `EXECUTIVE` → `CEO` → `'executive'`. All custom roles resolve to a known `UserRole`, so `'staff'` never appears in practice |

---

## Sprint 1 — P0 Blockers

---

### Task 1.1 — Fix GroupMembersPage RBAC gate

**Symptom:** Any non-super-admin with `users:read` can see the Groups tab, click a group, and hit "Super Admin Only" — because the page checks `user.role !== 'super_admin'` instead of the permission.

#### Backend — `RoleManagementController.java`

Change `listGroupMembers` from `ROLES_READ` to `USERS_READ`:

```java
// BEFORE
@GetMapping("/groups/{groupId}/users")
@PreAuthorize(ApiAuthorizationExpressions.ROLES_READ)

// AFTER
@GetMapping("/groups/{groupId}/users")
@PreAuthorize(ApiAuthorizationExpressions.USERS_READ)
```

#### Frontend — `GroupMembersPage.tsx`

Replace the hard role check (lines 52–68):

```tsx
// BEFORE
if (user.role !== 'super_admin') {
    return (
        <div className="...">
            <h2>...</h2>
            ...
        </div>
    )
}

// AFTER
import { usePermission } from '../../hooks/usePermission'

const canView = usePermission('users:read')

if (!canView) {
    return (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-8 text-center dark:border-amber-800 dark:bg-amber-900/20">
            <h2 className="text-xl font-semibold text-amber-800 dark:text-amber-200">{t('access.restrictedTitle')}</h2>
            <p className="mt-2 text-amber-700 dark:text-amber-300">{t('groupMembersPage.superAdminOnly')}</p>
            <button type="button" onClick={() => navigate('/management/users')} ...>
                {t('groupMembersPage.backToGroups')}
            </button>
        </div>
    )
}
```

---

### Task 1.2 — Seed webhooks permissions + fix COMPANY_STAFF gate

**Symptom:** A custom role with `webhooks:read/write` passes the frontend sidebar check but hits 403 on all webhook API calls because `COMPANY_STAFF` doesn't include those authorities.

#### Step A — New Flyway migration `V52__seed_webhooks_permissions.sql`

```sql
-- Seed webhooks:read and webhooks:write into sys_permissions
INSERT INTO sys_permissions (id, code, name, resource, action, locked, created_at)
VALUES
    (gen_random_uuid(), 'webhooks:read',  'View webhooks',   'webhooks', 'read',  false, now()),
    (gen_random_uuid(), 'webhooks:write', 'Manage webhooks', 'webhooks', 'write', false, now())
ON CONFLICT (code) DO NOTHING;

-- Grant to SUPER_ADMIN role
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r
CROSS JOIN sys_permissions p
WHERE r.code IN ('SUPER_ADMIN')
  AND p.code IN ('webhooks:read', 'webhooks:write')
ON CONFLICT DO NOTHING;
```

#### Step B — `ApiAuthorizationExpressions.java`

Add to the `COMPANY_STAFF` constant (end of the chain):

```java
// Add before the final semicolon:
+ " or hasAuthority('webhooks:read') or hasAuthority('webhooks:write')"
```

Add to the `isCompanyStaff()` helper prefix list:

```java
// In the inner if-block:
|| a.startsWith("webhooks:")
```

---

## Sprint 2 — P1 Fixes

---

### Task 2.1 — Fix `api` sidebar permission mismatch

**Symptom:** The API tab requires `api_docs:read` to appear, but the backend endpoints require `settings:read`. A user with only `api_docs:read` sees the tab but every data load returns 403.

**Decision:** Align everything to `settings:read`. The `api_docs:read` permission becomes unused by the sidebar (it can remain in the DB as a legacy code but is no longer the gate).

#### `sidebar.ts` — `NAV_READ_PERMISSIONS`

```typescript
// BEFORE
api: 'api_docs:read',

// AFTER
api: 'settings:read',
```

#### `RolesPage.tsx` — `NAV_PERMISSION_MAP`

```typescript
// BEFORE
api: { read: 'api_docs:read' },

// AFTER
api: { read: 'settings:read' },
```

#### `ApiPage.tsx`

```tsx
// BEFORE
const canView = usePermission('api_docs:read')

// AFTER
const canView = usePermission('settings:read')
```

---

### Task 2.2 — Add `refreshPermissions()` + on-focus re-fetch

**Symptom:** When an admin changes a user's role permissions, the affected user's `sessionStorage` cache is stale until they log out and back in. Backend cache is correctly evicted but the frontend never re-fetches.

#### `PermissionsContext.tsx`

1. Add `refreshPermissions` to the context interface and value.
2. Add a `window` focus listener that re-fetches when the tab regains focus.

```typescript
interface PermissionsContextValue {
    has: (code: string) => boolean
    permissions: string[]
    loading: boolean
    refreshPermissions: () => void   // ADD
}

// Inside PermissionsProvider:
// 1. Expose fetchPermissions as refreshPermissions
const value: PermissionsContextValue = {
    has,
    permissions: Array.from(perms),
    loading,
    refreshPermissions: fetchPermissions,   // ADD
}

// 2. Re-fetch on tab focus
useEffect(() => {
    window.addEventListener('focus', fetchPermissions)
    return () => window.removeEventListener('focus', fetchPermissions)
}, [fetchPermissions])
```

3. Export `refreshPermissions` from `usePermissions()` — callers destructure it when they need to trigger an immediate refresh (e.g., after the role editor saves).

#### `RolesPage.tsx` — `EditRoleNavigationModal.handleSubmit`

After a successful save, call `refreshPermissions()` so that if the currently logged-in admin is editing their own role, their permissions update immediately:

```tsx
const { refreshPermissions } = usePermissions()

// Inside handleSubmit, after onSuccess():
refreshPermissions()
```

---

## Sprint 3 — P2 Fixes

---

### Task 3.1 — Fix Select-All preset to exclude locked permissions

**Symptom:** The "Full Access" preset in the role editor grants ALL nav items including those backed by locked permissions (e.g., `system:super_ops`). Any admin can accidentally grant a custom role super-admin-level nav.

**Fact:** `PermissionSummaryResponse` already has a `locked: boolean` field. The frontend `PermissionCatalogueItemDto` type must expose it.

#### `RolesPage.tsx` — `EditRoleNavigationModal`

1. Ensure `PermissionCatalogueItemDto` includes `locked: boolean` in `types/api.ts`.

2. Build an `unlockedCodes` set from the catalogue:

```typescript
const unlockedCodes = new Set(catalogue.filter((p) => !p.locked).map((p) => p.code))
```

3. In the "Full Access" select-all button handler, replace `catalogueCodes.has(...)` with `unlockedCodes.has(...)`:

```tsx
// BEFORE
if (m.read && catalogueCodes.has(m.read)) allPerms.add(m.read)
if (m.write && catalogueCodes.has(m.write)) allPerms.add(m.write)
if (m.approve && catalogueCodes.has(m.approve)) allPerms.add(m.approve)
m.tabs?.forEach((tab) => {
    if (tab.read && catalogueCodes.has(tab.read)) allPerms.add(tab.read)
    if (tab.write && catalogueCodes.has(tab.write)) allPerms.add(tab.write)
})

// AFTER
if (m.read && unlockedCodes.has(m.read)) allPerms.add(m.read)
if (m.write && unlockedCodes.has(m.write)) allPerms.add(m.write)
if (m.approve && unlockedCodes.has(m.approve)) allPerms.add(m.approve)
m.tabs?.forEach((tab) => {
    if (tab.read && unlockedCodes.has(tab.read)) allPerms.add(tab.read)
    if (tab.write && unlockedCodes.has(tab.write)) allPerms.add(tab.write)
})
```

4. Also filter nav items added to `selected` — only include an item in the select-all if at least its `read` permission is unlocked (or it has no permission gate, like `dashboard`):

```tsx
ALL_SIDEBAR_ITEM_IDS.forEach((id) => {
    const m: NavPermEntry = NAV_PERMISSION_MAP[id] ?? {}
    const readIsUnlocked = !m.read || unlockedCodes.has(m.read)
    if (readIsUnlocked) newSelected.add(id)
    // ... add perms as above
})
```

---

### Task 3.2 — Delete legacy role-based helper functions from `auth.ts`

**Symptom:** Five exported functions check `user.role` (which is `'staff'` or a surface string) rather than permissions. Any future caller gets the wrong answer for all custom-role users.

#### `auth.ts` — Delete these five exports entirely

```typescript
// DELETE ALL OF THESE:
export function canCreateDiscount(role: Role): boolean { ... }
export function canApproveDiscount(role: Role): boolean { ... }
export function canCreateProvider(role: Role): boolean { ... }
export function canApproveRejectProvider(role: Role): boolean { ... }
export function canViewProviderCommission(role: Role): boolean { ... }
```

Search for any remaining callers:

```
grep -r "canCreateDiscount\|canApproveDiscount\|canCreateProvider\|canApproveRejectProvider\|canViewProviderCommission" src/
```

Replace any found caller with the equivalent `usePermission()` call:

| Deleted function | Replacement |
|---|---|
| `canCreateDiscount(role)` | `usePermission('discounts:write')` |
| `canApproveDiscount(role)` | `usePermission('discounts:approve')` |
| `canCreateProvider(role)` | `usePermission('providers:write')` |
| `canApproveRejectProvider(role)` | `usePermission('providers:approve')` |
| `canViewProviderCommission(role)` | `usePermission('payments:read')` |

---

## Sprint 4 — P3 Cleanup

---

### Task 4.1 — Remove `allowedRoles` dead code from `SIDEBAR_SECTIONS`

**Symptom:** `SidebarItem.allowedRoles` field has zero runtime effect — `Sidebar.tsx` never reads it. Developers may add it to new items believing it restricts access.

#### `sidebar.ts`

1. Remove `allowedRoles?: Role[]` from the `SidebarItem` interface.
2. Remove all `allowedRoles: [...]` entries from `SIDEBAR_SECTIONS` items.
3. Add a comment above `NAV_READ_PERMISSIONS`:

```typescript
/**
 * THIS is the authoritative access gate for ABAC sidebar visibility.
 * Items without an entry here are always visible.
 * Do NOT use allowedRoles on SidebarItem — it has no effect.
 */
export const NAV_READ_PERMISSIONS: Record<string, string | undefined> = {
```

---

### Task 4.2 — Delete `ROLE_SIDEBAR_SECTIONS` and `getSidebarSectionsForRole()`

**Symptom:** Both exports are never called by `Sidebar.tsx`. `staff: []` would cause an empty sidebar if ever re-introduced.

#### `sidebar.ts`

1. Delete the `ROLE_SIDEBAR_SECTIONS` constant (lines 109–118).
2. Delete the `getSidebarSectionsForRole()` function (lines 165–173).
3. Check `sidebar.test.ts` — remove or rewrite any tests that reference these exports.

---

### Task 4.3 — `CompanyDashboardPage` permission-based routing

**Symptom:** `user.role === 'admin'` routes to `SalesDashboardPage`. Custom EMPLOYEE/MANAGER roles resolve to `'admin'` surface, so they always see the sales dashboard regardless of their actual permissions. A finance-focused custom role sees sales KPIs.

#### `CompanyDashboardPage.tsx`

Replace the role check with a permission signal. Route to sales dashboard only when the user's permissions suggest a sales/operations context:

```tsx
// BEFORE
if (user?.role === 'admin') {
    return <SalesDashboardPage />
}

// AFTER
const { has } = usePermissions()
const showSalesDashboard = user?.role === 'admin' && has('bookings:read') && !has('reports:read')

if (showSalesDashboard) {
    return <SalesDashboardPage />
}
```

This keeps existing `'admin'` surface users on the sales dashboard when they have booking access but no analytics/reports access. Users with `reports:read` (finance-flavored custom roles) see the executive dashboard instead.

---

## Full Change Matrix

| Task | Priority | Files changed | Bug fixed |
|---|---|---|---|
| 1.1 | P0 | `GroupMembersPage.tsx`, `RoleManagementController.java` | Bug A |
| 1.2 | P0 | `V52__.sql`, `ApiAuthorizationExpressions.java` | Bug D |
| 2.1 | P1 | `sidebar.ts`, `RolesPage.tsx`, `ApiPage.tsx` | Bug C |
| 2.2 | P1 | `PermissionsContext.tsx`, `RolesPage.tsx` | Bug I |
| 3.1 | P2 | `RolesPage.tsx`, `types/api.ts` | Select-All scope |
| 3.2 | P2 | `auth.ts` | Bug F |
| 4.1 | P3 | `sidebar.ts` | Bug G |
| 4.2 | P3 | `sidebar.ts`, `sidebar.test.ts` | Bug H |
| 4.3 | P3 | `CompanyDashboardPage.tsx` | Bug E |

---

## Rule Card — Patterns to Never Reintroduce

After this plan is implemented, these patterns are permanently banned:

1. **Never** check `user.role === 'something'` in a page component for access control. Always use `usePermission('code')`.
2. **Never** use `hasRole('...')` in a `@PreAuthorize` expression for company staff endpoints. Always use `hasAuthority('permission:code')`.
3. **Never** add a new sidebar item without a corresponding entry in BOTH `NAV_READ_PERMISSIONS` (sidebar.ts) AND `NAV_PERMISSION_MAP` (RolesPage.tsx).
4. **Never** add a new controller/endpoint gated by `@PreAuthorize(COMPANY_STAFF)` without first verifying the relevant permission code is in the `COMPANY_STAFF` constant.
5. **Never** add a new permission code to `NAV_PERMISSION_MAP` without seeding it in `sys_permissions` via a Flyway migration.
