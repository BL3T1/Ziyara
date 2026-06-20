# ABAC System Report — Ziyara Company Dashboard

---

## 1. Architecture Overview: Two-Track Authorization Model

The system runs **two authorization tracks in parallel**. Which track governs a given user depends on whether they have a custom RBAC role assigned.

### Track A — Static Role Surface (legacy, shrinking)

Every user has a `UserRole` enum value stored in `sys_users.role` (13 variants: `SUPER_ADMIN`, `CEO`, `SALES_MANAGER`, `SALES_REPRESENTATIVE`, `FINANCE_MANAGER`, `ACCOUNTANT`, `SUPPORT_MANAGER`, `SUPPORT_AGENT`, `CUSTOMER`, provider portal roles). The backend maps these to 8 frontend "surfaces" via `backendRoleToFrontend()` in `auth.ts`:

| Backend `UserRole` | Frontend `Role` |
|---|---|
| `SUPER_ADMIN` | `'super_admin'` |
| `CEO` | `'executive'` |
| `SALES_MANAGER`, `SALES_REPRESENTATIVE` | `'admin'` |
| `FINANCE_MANAGER`, `ACCOUNTANT` | `'finance'` |
| `SUPPORT_MANAGER`, `SUPPORT_AGENT` | `'support'` |
| `PROVIDER_*`, `TAXI_OPERATOR` | `'provider'` |
| `CUSTOMER` | `'user'` |
| unknown / custom RBAC role | `'staff'` |

For system-role users who have **never** been assigned a custom RBAC role, the sidebar is derived from `CompanySidebarCatalog.defaultVisibleItemIds(SidebarSurface)` on the backend, which mirrors `ROLE_SIDEBAR_SECTIONS` in the frontend.

### Track B — Attribute-Based (ABAC, current approach)

Any user can be assigned a custom RBAC role stored in `sys_roles`. That role holds:

- A list of **permission codes** in `sys_role_permissions` (e.g., `bookings:read`, `providers:write`)
- An explicit **sidebar nav list** in `sys_roles.navigation_item_ids` (JSONB column, e.g., `["dashboard","bookings","providers"]`)

When a user has a custom role assigned, their sidebar and their API access are fully governed by that role's configuration — not by their `UserRole` enum.

---

## 2. How the Sidebar Is Computed and Rendered

### 2.1 Frontend Primitives (`sidebar.ts`)

There are **three permanent data structures** that link nav items to the permission system.

**`NAV_READ_PERMISSIONS`** (`sidebar.ts:125`) — maps each sidebar item ID to the single `read` permission required to *see* that tab under ABAC:

| Sidebar item | Required permission |
|---|---|
| `dashboard` | *(none — always visible)* |
| `main_find_customer` | `customers:read` |
| `main_deleted_items` | `deleted_items:company:read` |
| `hotels`, `resorts`, `restaurants`, `trips` | `services:read` |
| `taxis`, `taxi_trips` | `taxi:read` |
| `providers` | `providers:read` |
| `bookings` | `bookings:read` |
| `payments` | `payments:read` |
| `payouts` | `payouts:read` |
| `discounts` | `discounts:read` |
| `media_approvals` | `media_submissions:approve` |
| `reports` | `reports:read` |
| `currency_rates` | `currency:read` |
| `complaints` | `complaints:read` |
| `reviews` | `reviews:read` |
| `provider_messages` | `providers_messages:read` |
| `settings`, `integrations` | `settings:read` |
| `users` | `users:read` |
| `roles` | `roles:read` |
| `logs` | `audit:read` |
| `content` | `content:read` |
| `api` | `api_docs:read` |
| `webhooks` | `webhooks:read` |
| `subscriptions` | `providers:read` |

**`NAV_PERMISSION_MAP`** (`RolesPage.tsx:238`) — the richer admin-facing version used in the role editor, listing `read`, `write`, AND `approve` codes per item:

```
'discounts':          { read: 'discounts:read',  write: 'discounts:write',  approve: 'discounts:approve' }
'providers':          { read: 'providers:read',  write: 'providers:write',  approve: 'providers:approve' }
'hotels':             { read: 'services:read',   write: 'services:write',   approve: 'services:publish'  }
'main_deleted_items': { tabs: [
    { label: 'Company',   read: 'deleted_items:company:read',   write: 'deleted_items:company:restore'   },
    { label: 'Providers', read: 'deleted_items:providers:read', write: 'deleted_items:providers:restore' },
    { label: 'Users',     read: 'deleted_items:users:read',     write: 'deleted_items:users:restore'     },
]}
```

**`CompanySidebarCatalog.java`** (backend mirror) — mirrors `SIDEBAR_SECTIONS` + `allowedSurfacesOrNull` per item. Used only for Track A default nav computation; does not restrict ABAC nav.

### 2.2 Sidebar Render Decision Tree (`Sidebar.tsx:149`)

```tsx
const sections =
  sidebarNav
    ? filterSectionsByVisibleIds(SIDEBAR_SECTIONS, sidebarNav.visibleItemIds)
    : filterSectionsByVisibleIds(SIDEBAR_SECTIONS, getVisibleItemIdsFromPermissions(has))
```

| `sidebarNav` value | Source | Which items render |
|---|---|---|
| `null` — super_admin | `PermissionsContext.has()` always `true` | All items |
| `null` — network error or non-staff | `PermissionsContext.has()` | All items user holds permissions for |
| `{ visibleItemIds: [...], source: 'rbac_role' }` | Admin-configured nav | Exactly the list the admin chose |
| `{ visibleItemIds: [...], source: 'default_user_role' }` | `CompanySidebarCatalog` defaults | Role-surface defaults |

### 2.3 Bootstrap Sequence (`CompanyNavBootstrap.tsx`)

On every login, `CompanyNavBootstrap.tsx` runs once:

1. Not authenticated or not company staff → `setSidebarNav(null)` → ABAC fallback via `has()`
2. `user.role === 'super_admin'` → `setSidebarNav(null)` → `has()` always `true` → all tabs
3. Otherwise → `GET /users/me/navigation` → `NavigationService.resolveNavigationForUser(userId)`

**Backend `NavigationService` logic:**

```
if user has a custom RBAC role assigned
  AND role.navigation_item_ids IS NOT NULL:
    → return { visibleItemIds: sanitized list, source: "rbac_role" }
else:
    → return { visibleItemIds: CompanySidebarCatalog.defaults, source: "default_user_role" }
```

**Critical semantic distinction — `null` vs `[]`:**

| `navigation_item_ids` DB value | Meaning | Result |
|---|---|---|
| `null` | Role was never customized | Falls back to surface defaults |
| `[]` (empty list) | Admin explicitly cleared all tabs | Returns empty sidebar |
| `["dashboard","bookings",...]` | Admin chose specific items | Returns exactly those items |

Back in `CompanyNavBootstrap.tsx`:
- `source === 'rbac_role'` → `setSidebarNav(...)` **even if the list is empty** (respects the admin's explicit empty choice)
- `source === 'default_user_role'` and list is non-empty → `setSidebarNav(...)`
- Everything else → `setSidebarNav(null)` → falls through to ABAC `has()` check

### 2.4 Permission Loading (`PermissionsContext.tsx`)

On user login, `PermissionsContext` calls `GET /users/me/permissions` which returns the user's permission code array, stored in `sessionStorage` under `ziyara_perms`. Exposes `has(code: string): boolean`.

Special case: `user.role === 'super_admin'` → `has()` always returns `true` without consulting the stored set.

---

## 3. Full Flow: Admin Configures Role → New User's Experience

### Step 1 — Admin Creates a Custom Role

`POST /roles` → `RoleManagementController` → `RoleManagementService.createCustomRole()`

- New `sys_roles` row: `is_system_role = false`, `navigation_item_ids = null`, `code = CUSTOM_<NAME>`
- Auto-creates a group (or links to an existing one) → `sys_groups`
- Cache evicted: `staffRoleCatalog`

### Step 2 — Admin Opens "Edit Permissions" Modal

`GET /roles/{id}` → returns `RoleResponse` with `navigationItemIds` and `permissionIds`.

In `EditRoleNavigationModal` (`RolesPage.tsx`):
- For system roles with `null` nav, backend `toRoleResponse()` falls back to `defaultVisibleItemIdsForUserRole(role.code)` — editor opens pre-populated
- For new custom roles, `null` → modal opens with nothing checked
- Admin checks a sidebar item → two things happen automatically:
  - Item added to `selected` set (controls which nav items are stored)
  - Item's `read` permission code auto-added to `permChecked`
  - Admin can optionally enable `write` / `approve` sub-checkboxes for granular write access

### Step 3 — Admin Saves

```
PUT /roles/{id}/navigation    → { visibleItemIds: ["dashboard","bookings","providers",...] }
PUT /roles/{id}/permissions   → { permissionIds: [uuid1, uuid2, ...] }
```

Both calls: `@CacheEvict(cacheNames = {"staffRoleCatalog", "userPermissions"}, allEntries = true)` — clears the Spring cache so existing sessions pick up new permissions on next re-authentication.

Backend writes:
- `sys_roles.navigation_item_ids` = JSONB array of item IDs
- `sys_role_permissions` = one row per permission UUID

### Step 4 — Admin Creates a New User with This Role

`POST /users` with `{ primaryRbacRoleId: "...", email: "...", password: "..." }`

`UserCommandHandler.create()`:
1. Loads the RBAC role from `sys_roles`
2. `CompanyStaffRoleCatalogService.resolveSecurityUserRoleForPrimaryRbacRole(rbac)` → maps the role's group code to a `UserRole` enum (used as the "security tier" for Spring Security's `ROLE_` authority)
3. Creates `sys_users` row with the resolved `UserRole`
4. Creates `sys_user_roles` row: `(user_id, role_id, assigned_at)`

### Step 5 — New User Logs In

**JWT generation (`CustomUserDetailsService.loadUserByUsername`):**

1. Loads `User` → adds `ROLE_<UserRole.name()>` (e.g., `ROLE_SALES_REPRESENTATIVE`)
2. Runs SQL:
   ```sql
   SELECT DISTINCT p.code
   FROM sys_user_roles ur
   JOIN sys_role_permissions rp ON rp.role_id = ur.role_id
   JOIN sys_permissions p ON p.id = rp.permission_id
   WHERE ur.user_id = ?
   ```
3. Each code becomes a `SimpleGrantedAuthority` (e.g., `bookings:read`, `providers:write`)
4. JWT is signed with all authorities

**Frontend on login:**
1. `GET /users/me/permissions` → `["bookings:read","providers:write",...]` → cached in `sessionStorage`
2. `GET /users/me/navigation` → `{ visibleItemIds: ["dashboard","bookings","providers"], source: "rbac_role" }`
3. `setSidebarNav({ visibleItemIds: [...], source: 'rbac_role' })`

**Result:** User sees exactly the sidebar the admin configured. `usePermission()` checks reflect exactly the permissions the admin assigned.

### Key Invariant — Nav and Permissions Are Independent

The admin sets **two independent things**:

| What | Where stored | Controls |
|---|---|---|
| Navigation list | `sys_roles.navigation_item_ids` | Which tabs **appear** in the sidebar |
| Permission codes | `sys_role_permissions` | Which API calls **succeed** + in-page feature flags |

These can be mismatched: an admin can show a sidebar tab but forget to assign the corresponding `read` permission → tab appears, user navigates to the page, page makes an API call, backend returns **403 Access Denied**.

---

## 4. Remaining RBAC Bugs

---

### Bug A — `GroupMembersPage.tsx:54` hardcodes `user.role !== 'super_admin'` *(Active)*

**File:** `front/my-app/src/pages/management/GroupMembersPage.tsx:54`

```typescript
if (user.role !== 'super_admin') {
    return <div>Super Admin Only</div>
}
```

**What breaks:** A custom-role user who has `users:read` in their permission set:

1. `users → 'users:read'` in `NAV_READ_PERMISSIONS` → **Groups tab shows in sidebar** ✓
2. User navigates to `/management/users` → Groups list loads fine ✓
3. User clicks a group → `/management/users/{groupId}/members`
4. `GroupMembersPage` fires the role check → `user.role === 'staff'` → **renders "Super Admin Only" block** ✗

The backend endpoint `GET /roles/groups/{groupId}/users` is gated by `ROLES_READ = hasAuthority('roles:read')`. Note this is `roles:read`, not `users:read` — the sidebar nav item and the backend gate use different permission codes, which is a secondary issue.

**Fix:** Replace line 54 with `usePermission('users:read')` (and align the backend gate to `users:read` if that is the intended permission).

---

### Bug B — `COMPANY_STAFF` expression missing `webhooks:read` / `webhooks:write` *(Active)*

**File:** `core/src/main/java/com/ziyara/backend/infrastructure/security/ApiAuthorizationExpressions.java:26`

`WebhookSubscriptionController` is gated at class-level with `@PreAuthorize(COMPANY_STAFF)`. The `COMPANY_STAFF` constant and the `isCompanyStaff()` runtime helper do **not** include `webhooks:read` or `webhooks:write` in their permission lists.

**What breaks:** A custom role that grants only `webhooks:read` (and no other permission from the COMPANY_STAFF list):

1. `webhooks → 'webhooks:read'` in `NAV_READ_PERMISSIONS` → **Webhooks tab shows in sidebar** ✓
2. User navigates to `/admin/webhooks`
3. API call `GET /webhooks` → `@PreAuthorize(COMPANY_STAFF)` → **403 Access Denied** ✗

**Fix:** Add to the `COMPANY_STAFF` constant:
```java
+ " or hasAuthority('webhooks:read') or hasAuthority('webhooks:write')"
```
And add `a.startsWith("webhooks:")` to the `isCompanyStaff()` prefix list.

---

### Bug C — `api_docs:read` sidebar permission decoupled from `settings:read` backend gate *(Active)*

**Files:** `front/my-app/src/pages/admin/ApiPage.tsx:15`, `core/.../AdminIntegrationApiKeysController.java:34`

- `NAV_READ_PERMISSIONS`: `api → 'api_docs:read'` → sidebar shows API tab for users with `api_docs:read`
- `ApiPage.tsx`: `const canView = usePermission('api_docs:read')` → page renders
- `AdminIntegrationApiKeysController` GET: `@PreAuthorize(SETTINGS_READ)` = `hasAuthority('settings:read')`

**What breaks:** A user with `api_docs:read` but **without** `settings:read`:

1. API tab shows in sidebar ✓
2. Page renders (canView is true) ✓
3. Every data fetch to the API keys endpoints → **403 Access Denied** ✗

**Fix (option 1):** Change `NAV_READ_PERMISSIONS` to `api: 'settings:read'` so the tab only shows for users who can actually load the data.
**Fix (option 2):** Add a `settings:read` check inside `ApiPage.tsx` that shows an "Access Denied" banner when `canView` is true but `has('settings:read')` is false.

---

### Bug D — `sessionStorage` permissions not refreshed when admin changes role mid-session *(Active)*

**File:** `front/my-app/src/context/PermissionsContext.tsx:59-67`

```typescript
useEffect(() => {
    fetchPermissions()
}, [user?.id, isAuthenticated])
```

Permissions are only re-fetched when `user.id` or `isAuthenticated` changes. If an admin edits a user's role permissions while that user is logged in:

- Backend correctly evicts `userPermissions` cache via `@CacheEvict`
- Frontend never knows — continues operating on stale `sessionStorage` permissions until log-out

**Scenarios:**
- Permission was **removed**: user can still use features and call APIs that should now be denied (backend will 403 correctly, but the UI shows the feature and makes the call)
- Permission was **added**: user cannot see newly-granted features or tabs until they re-login

**Fix:** Expose a `refreshPermissions()` from `PermissionsContext`. Call it (or trigger a re-fetch) on a short polling interval or WebSocket event after role changes. At minimum, document that permission changes require the affected user to re-login to take effect.

---

### Bug E — `CompanyDashboardPage.tsx:21` routes dashboard by role, not permission *(Minor)*

**File:** `front/my-app/src/pages/CompanyDashboardPage.tsx:21`

```typescript
if (user?.role === 'admin') {
    return <SalesDashboardPage />
}
return <DashboardPage />
```

**What breaks:** All custom-role users have `user.role === 'staff'`. A custom role modeled after a sales manager (has `bookings:read`, `providers:read`, etc.) will always see the executive/KPI dashboard, never the sales pipeline dashboard.

This is not an access-denied error but a UX regression for any custom role that models a sales surface.

**Fix:** Route by a permission signal instead of role, for example:
```typescript
const isSalesView = !has('reports:read') && has('bookings:read')
```
Or expose a `dashboardView` field from the nav response.

---

### Bug F — Legacy role-based helper functions still exported from `auth.ts` *(Risk)*

**File:** `front/my-app/src/types/auth.ts:69-97`

```typescript
export function canCreateDiscount(role: Role): boolean { ... }
export function canApproveDiscount(role: Role): boolean { ... }
export function canCreateProvider(role: Role): boolean { ... }
export function canApproveRejectProvider(role: Role): boolean { ... }
export function canViewProviderCommission(role: Role): boolean { ... }
```

All five functions check `user.role` which is `'staff'` for every custom-role user. Any page that calls one of these returns `false` for ALL custom-role users regardless of actual permissions.

**Current status:** Main pages have already migrated to `usePermission()`:
- `ProvidersPage.tsx` → `usePermission('providers:approve')`, `usePermission('payments:read')` ✓
- `DiscountsPage.tsx` → `usePermission('discounts:approve')`, `usePermission('discounts:write')` ✓

These functions are dead but their presence is dangerous — a developer adding a new feature might reach for them by mistake.

**Fix:** Delete all five functions from `auth.ts`. If any callers still exist outside of already-audited pages, replace with the equivalent `usePermission(code)` call.

---

### Bug G — `allowedRoles` on `SidebarItem` is vestigial dead code *(Misleading)*

**File:** `front/my-app/src/config/sidebar.ts:56, 84-100`

```typescript
{ id: 'payouts', ..., allowedRoles: ['super_admin', 'admin', 'finance', 'executive'] },
{ id: 'media_approvals', ..., allowedRoles: ['super_admin', 'admin'] },
```

`Sidebar.tsx` never calls `getSidebarSectionsForRole()`. It only calls `filterSectionsByVisibleIds()` which ignores `allowedRoles` entirely. These fields have **zero runtime effect** on what is rendered to any user.

**Risk:** A developer adding a new sidebar item may set `allowedRoles` believing it will restrict visibility for custom-role users. It will not. The real gate is `NAV_READ_PERMISSIONS`.

**Fix:** Remove the `allowedRoles` field from every item in `SIDEBAR_SECTIONS`. Add a comment above `NAV_READ_PERMISSIONS` stating it is the authoritative access gate for ABAC nav.

---

### Bug H — `ROLE_SIDEBAR_SECTIONS` and `getSidebarSectionsForRole()` are unreachable dead code *(Misleading)*

**File:** `front/my-app/src/config/sidebar.ts:109-173`

```typescript
export const ROLE_SIDEBAR_SECTIONS: Record<Role, string[]> = {
    staff: [],
    ...
}
export function getSidebarSectionsForRole(role: Role): SidebarSection[] { ... }
```

Neither is called by `Sidebar.tsx`. `staff: []` is particularly misleading — if this function were ever re-introduced, all custom-role users would get an empty sidebar.

**Fix:** Delete both exports.

---

### Bug I — `PROVIDER_PORTAL`, `PORTAL_MANAGER`, `PORTAL_FINANCE` use `hasRole()` *(By design, document it)*

**File:** `core/.../ApiAuthorizationExpressions.java:209-216`

```java
public static final String PROVIDER_PORTAL = "hasAnyRole('PROVIDER_MANAGER','PROVIDER_FINANCE','PROVIDER_STAFF','TAXI_OPERATOR')";
public static final String PORTAL_MANAGER  = "hasRole('PROVIDER_MANAGER') or hasAuthority('PROVIDER_ROLE_MANAGER')";
public static final String PORTAL_FINANCE  = "hasRole('PROVIDER_FINANCE') or hasAuthority('PROVIDER_ROLE_FINANCIAL_ADMIN')";
```

Provider portal staff are identified by their `UserRole` enum, not by company permission codes. This is intentional. However, it means:
- `ReviewController.java:119` (`@PreAuthorize(PORTAL_MANAGER)`) cannot be reached by company-side custom roles even if given provider review permissions
- Any future cross-portal feature will hit this wall

This is **not a current bug** but should be documented as a design constraint: provider-side authorization is role-based, company-side is permission-based, and they do not mix.

---

## 5. Summary Table

| Priority | Bug | File | Symptom | Fix |
|---|---|---|---|---|
| **P0** | GroupMembersPage RBAC gate | `GroupMembersPage.tsx:54` | Tab shows, page blocks with "Super Admin Only" | Replace role check with `usePermission('users:read')` |
| **P0** | COMPANY_STAFF missing webhooks perms | `ApiAuthorizationExpressions.java:26` | Webhooks tab shows, data returns 403 | Add `webhooks:read/write` to COMPANY_STAFF |
| **P1** | api_docs:read / settings:read mismatch | `ApiPage.tsx:15`, `AdminIntegrationApiKeysController.java:34` | API tab shows, data returns 403 | Align nav perm with backend gate |
| **P1** | Stale sessionStorage permissions | `PermissionsContext.tsx:59` | Role changes don't take effect until re-login | Add `refreshPermissions()` or require re-login after admin role edit |
| **P2** | Dashboard routed by role not permission | `CompanyDashboardPage.tsx:21` | Custom roles always see executive dashboard | Route by permission signal |
| **P2** | Legacy canCreate/canApprove functions | `auth.ts:69` | Dead RBAC helpers; wrong for custom roles if ever called | Delete all five functions |
| **P3** | `allowedRoles` dead code on items | `sidebar.ts:56+` | Misleads developers about how sidebar access works | Remove `allowedRoles` field; document `NAV_READ_PERMISSIONS` |
| **P3** | `ROLE_SIDEBAR_SECTIONS` dead code | `sidebar.ts:109` | `staff: []` would break sidebar if re-introduced | Delete both exports |

---

## 6. Open Questions

1. **GroupMembersPage backend gate** — The page calls `GET /roles/groups/{groupId}/users` (gated by `roles:read`) but the sidebar nav item maps `users → 'users:read'`. A custom role can hold `users:read` without `roles:read`. Should the page require `users:read` or `roles:read`? The two permission codes are currently misaligned.

2. **`webhooks:read` in `sys_permissions`** — Is `webhooks:read` actually seeded in the `sys_permissions` table? If the permission code doesn't exist in the catalogue, it can never be assigned to a custom role via the editor, and Bug B cannot occur in practice. Please confirm which permissions in `NAV_PERMISSION_MAP` exist in the database.

3. **`resolveSecurityUserRoleForPrimaryRbacRole()` default** — What `UserRole` is assigned to users created with a fully custom RBAC role (non-system group code)? If it defaults to `SALES_REPRESENTATIVE`, those users get `user.role === 'admin'` on the frontend, and `CompanyDashboardPage` routes them to `SalesDashboardPage` — contradicting the ABAC model where their dashboard experience should be permission-driven.

4. **Super admin nav bypass** — `CompanyNavBootstrap.tsx` short-circuits to `setSidebarNav(null)` for super admins (line 21). This means even if a super admin somehow has a custom role assigned with a restricted nav list, they always see all tabs. Is this intentional, or should super admins also be able to have a restricted nav for audit/testing purposes?

5. **"Select All" preset and locked items** — The "Full Access" preset in `EditRoleNavigationModal` adds ALL items from `ALL_SIDEBAR_ITEM_IDS` including `main_find_customer` (customer search) and `main_deleted_items` sub-tabs — items that are restricted to `SUPER_ADMIN` surface in `CompanySidebarCatalog`. Should system-locked items be excluded from the select-all preset, or is it intentional to allow any admin to grant a custom role super-admin-level nav?
