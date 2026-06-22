# Frontend ABAC Implementation Plan

Attribute-Based Access Control on the frontend means: **always show data** (if the user can reach the page), **never show write/approve action buttons** unless the user holds the matching permission code.

The backend is the source of truth — it enforces `@PreAuthorize` on every endpoint. The frontend gates are UX only: they hide affordances the user cannot use, not a security boundary.

---

## 1. Infrastructure Already in Place

| Piece | File | Status |
|---|---|---|
| `PermissionsContext` | `src/context/PermissionsContext.tsx` | Done — fetches `GET /users/me/permissions`, caches in sessionStorage, refreshes on focus |
| `usePermission(code)` | `src/hooks/usePermission.ts` | Done — returns `boolean`, super_admin always returns `true` |
| Route guard (auth) | `src/components/RequireAuth.tsx` | Done — redirects to `/login` if unauthenticated |
| Surface guard (role) | `src/components/RequireSurfaceRole.tsx` | Done — blocks provider users from company surface and vice versa |

**The only missing piece** before page work begins is a `<PermissionGate>` wrapper component.

---

## 2. Add `<PermissionGate>` Component

**File:** `src/components/PermissionGate.tsx`

```tsx
import { usePermission } from '../hooks/usePermission'

interface Props {
  code: string
  children: React.ReactNode
  fallback?: React.ReactNode   // optional read-only substitute
}

export function PermissionGate({ code, children, fallback = null }: Props) {
  const allowed = usePermission(code)
  return allowed ? <>{children}</> : <>{fallback}</>
}
```

Use it anywhere a button/form should be hidden without the permission:

```tsx
<PermissionGate code="payouts:approve">
  <button onClick={() => handlePay(id)}>Mark Paid</button>
</PermissionGate>
```

For inline boolean needs (e.g. conditional columns), keep using `usePermission` directly — the component is for JSX blocks.

---

## 3. The Three Permission Tiers

Every resource follows this pattern:

| Code suffix | What it gates |
|---|---|
| `:read` | Seeing the page/data at all (route access + API calls) |
| `:write` | Create / Edit / Delete / Update buttons and forms |
| `:approve` | Approve / Reject / Suspend / Moderate / Release buttons |

A user may have `payouts:read` without `payouts:approve` — they see the payout list but the "Pay" and "Hold" buttons are hidden.

---

## 4. Route-Level Guards

Add a `requiredPermission` prop to the route wrappers (or wrap pages in a `<RequirePermission>` component) so that users who somehow navigate to a URL they lack access to get a proper "Access Denied" screen rather than a broken page.

**File to create:** `src/components/RequirePermission.tsx`

```tsx
import { usePermission } from '../hooks/usePermission'
import { AccessDenied } from './AccessDenied'   // simple "You don't have access" panel

interface Props {
  code: string
  children: React.ReactNode
}

export function RequirePermission({ code, children }: Props) {
  const allowed = usePermission(code)
  if (!allowed) return <AccessDenied />
  return <>{children}</>
}
```

Wrap each route's page element in the router file:

```tsx
<Route
  path="/management/payouts"
  element={
    <RequireAuth>
      <RequirePermission code="payouts:read">
        <PayoutsPage />
      </RequirePermission>
    </RequireAuth>
  }
/>
```

---

## 5. Company Dashboard — Page-by-Page Work

### 5.1 Pages Already Gated (verify completeness)

| Page | Permission codes in use | Gaps to check |
|---|---|---|
| `ProvidersPage` | `providers:write`, `providers:approve`, `payments:read` | Suspend button not gated — add `providers:approve` |
| `CreateProviderPage` | `providers:write` (blocks whole page) | OK |
| `EditProviderPage` | `providers:read`, `providers:approve`, `payments:read`, `media_submissions:approve` | Save button itself not gated — should check `providers:write` |
| `DiscountsPage` | `discounts:write`, `discounts:approve` | OK |
| `UsersPage` | `users:read`, `users:write` | Reset-password button needs `users:reset_password` |
| `GroupMembersPage` | `users:read` | Add/remove member needs `users:write` |
| `StaffUserDetailPage` | `has()` from context | Verify write actions check `users:write` |
| `ServiceTypePage` | `providers:write` (add partner only) | Edit/delete service type needs `services:write` |
| `PartnerAccountsSection` | `providers:write`, `payments:read` | OK |
| `RoleMembersPage` | `roles:read` | Add/remove member needs `roles:write` |
| `RolesPage` | Refresh only — **no write gates** | Edit permissions, edit details, delete role buttons all need `roles:write` |
| `ApiPage` | `settings:read` | Regenerate key needs `settings:write` |
| `ContentPagesPage` | `content:write` | OK |
| `CustomerProfilePage` | `customers:read` | Edit/block actions need `customers:write` |
| `CustomerSearchPage` | `customers:read` | OK |
| `DeletedItemsPage` | `deleted_items:company:read`, `deleted_items:company:restore` | OK |
| `IntegrationsPage` | `settings:write` | OK |
| `CompanyDashboardPage` | `has()` — stat widgets | Verify each widget uses correct code |

### 5.2 Pages Not Yet Gated

#### `BookingsPage`

| Action | Permission code |
|---|---|
| View list / details | `bookings:read` (route guard) |
| Confirm booking button | `bookings:write` |
| Cancel booking button | `bookings:write` |
| Reject booking button | `bookings:write` |
| Bulk confirm / bulk cancel | `bookings:write` |
| Export CSV button | `reports:read` |

```tsx
const canWrite = usePermission('bookings:write')
// then: {canWrite && <button onClick={() => handleConfirm(b.id)}>Confirm</button>}
```

---

#### `PayoutsPage`

| Action | Permission code |
|---|---|
| View list | `payouts:read` (route guard) |
| Pay / Mark Paid buttons | `payouts:approve` |
| Hold / Release hold buttons | `payouts:approve` |
| Cancel / Retry buttons | `payouts:approve` |
| Bulk Pay / Bulk Hold buttons | `payouts:approve` |
| Create manual payout button | `payouts:write` |
| Export button | `reports:read` |

```tsx
const canApprove = usePermission('payouts:approve')
const canWrite   = usePermission('payouts:write')
const canExport  = usePermission('reports:read')
```

---

#### `ComplaintsPage`

| Action | Permission code |
|---|---|
| View list / details | `complaints:read` (route guard) |
| Assign to agent button | `complaints:write` |
| Resolve button | `complaints:write` |
| Close button | `complaints:write` |

```tsx
const canWrite = usePermission('complaints:write')
```

---

#### `TicketsPage` + `TicketDetailPage`

| Action | Permission code |
|---|---|
| View tickets list | `support:read` (route guard) |
| Send reply / respond | `support:write` |

```tsx
const canReply = usePermission('support:write')
// hide send button and textarea when !canReply
```

---

#### `ReviewsPage`

| Action | Permission code |
|---|---|
| View reviews | `reviews:moderate` (route guard — this page is moderation-only) |
| Approve review button | `reviews:moderate` |
| Remove / hide review button | `reviews:moderate` |

```tsx
const canModerate = usePermission('reviews:moderate')
```

---

#### `MediaSubmissionsPage`

| Action | Permission code |
|---|---|
| View submissions | `media_submissions:approve` (route guard) |
| Approve button | `media_submissions:approve` |
| Reject button | `media_submissions:approve` |

```tsx
const canApprove = usePermission('media_submissions:approve')
```

---

#### `PaymentsPage`

| Action | Permission code |
|---|---|
| View payments list | `payments:read` (route guard) |
| Refund button (if present) | `payments:write` |
| Export button | `reports:read` |

```tsx
const canWrite  = usePermission('payments:write')
const canExport = usePermission('reports:read')
```

---

#### `SettingsPage`

| Action | Permission code |
|---|---|
| View settings | `settings:read` (route guard) |
| Save / update any setting | `settings:write` |
| All input fields | disable or hide save when `!canWrite` |

```tsx
const canRead  = usePermission('settings:read')
const canWrite = usePermission('settings:write')
// Render inputs as read-only text when !canWrite, or hide Save button
```

---

#### `CurrencyRatesPage`

| Action | Permission code |
|---|---|
| View rates | `settings:read` (route guard) |
| Edit rate / save button | `settings:write` |

---

#### `WebhookSubscriptionsPage`

| Action | Permission code |
|---|---|
| View webhooks | `settings:read` (route guard) |
| Add / delete webhook | `settings:write` |

---

#### `AuditLogsPage`

| Action | Permission code |
|---|---|
| View audit logs | `audit:read` (route guard) |
| Export button | `reports:read` |
| No write actions | — |

---

#### `AnalyticsPage` / `SalesDashboardPage`

| Action | Permission code |
|---|---|
| View page | `analytics:read` (route guard) |
| No write actions | — |

---

#### `ReportsPage`

| Action | Permission code |
|---|---|
| View / download reports | `reports:read` (route guard) |
| No write actions | — |

---

#### `TaxiTripsPage`

| Action | Permission code |
|---|---|
| View trips | `bookings:read` (route guard) |
| Assign driver (if present) | `bookings:write` |

---

#### `SalesProvidersPage`

| Action | Permission code |
|---|---|
| View providers (read-only sales view) | `providers:read` (route guard) |
| No write actions | — |

---

#### `SubscriptionsPage`

| Action | Permission code |
|---|---|
| View subscriptions | `settings:read` (route guard) |
| Create / edit subscription plan | `settings:write` |

---

#### `ServiceDetailPage` + `ServiceDetailMediaEditor`

| Action | Permission code |
|---|---|
| View service detail | `services:read` (route guard) |
| Edit service info (save button) | `services:write` |
| Add / remove media | `services:write` |
| Approve media in editor | `media_submissions:approve` |

```tsx
const canEdit         = usePermission('services:write')
const canApproveMedia = usePermission('media_submissions:approve')
```

---

### 5.3 RolesPage Fix (high priority — write buttons are completely ungated)

Currently the Edit Permissions, Edit Details, and Delete buttons in `RolesPage` have **no permission gate**. Any logged-in staff can click them.

```tsx
// Add to RolesPage.tsx
const canWrite = usePermission('roles:write')

// Then gate each button:
{canWrite && (
  <button onClick={() => setEditNavRole(role)}>Edit Permissions</button>
)}
{canWrite && (
  <button onClick={() => setEditDetailsRole(role)}>Edit Details</button>
)}
{canWrite && (
  <button onClick={() => setDeleteRole(role)}>Delete</button>
)}
```

---

## 6. Provider Portal Surface

The portal uses a separate permission namespace: `portal:access`, `portal:manage`, `portal:finance`.

| Page | Route guard | Hidden without |
|---|---|---|
| `ClientPortalOverview` | `portal:access` | — (read-only) |
| `PortalBookingsPage` | `portal:access` | — (read-only) |
| `PortalSupportPage` | `portal:access` | Reply button: `portal:access` (any authenticated provider) |
| `PortalMediaPage` | `portal:access` | Upload/delete media: `portal:manage` |
| `PortalProfilePage` | `portal:access` | Save profile: `portal:manage` |
| `PortalListingsPage` | `portal:access` | Create new listing button: `portal:manage` |
| `PortalListingFormPage` | `portal:manage` | Entire page (creation/edit form) |
| `PortalDiscountsPage` | `portal:finance` | Create/edit/delete discount: `portal:finance` |
| `PortalEarningsPage` | `portal:finance` | Entire page (financial data) |
| `PortalStaffPage` | `portal:manage` | Invite / remove staff: `portal:manage` |

---

## 7. Implementation Order (by risk/impact)

### Phase 1 — Critical fixes (ungated write buttons that exist today)
1. `RolesPage` — gate Edit/Delete buttons with `roles:write`
2. `ProvidersPage` — gate Suspend button with `providers:approve`
3. `EditProviderPage` — gate Save button with `providers:write`
4. `UsersPage` — gate Reset Password with `users:reset_password`

### Phase 2 — Add PermissionGate component + RequirePermission route wrapper
5. Create `src/components/PermissionGate.tsx`
6. Create `src/components/RequirePermission.tsx`
7. Wrap all existing routes in the router file with `<RequirePermission code="...">` using the read code for each resource

### Phase 3 — Gate write/approve actions on ungated pages
8. `PayoutsPage` — `payouts:approve`, `payouts:write`, `reports:read`
9. `BookingsPage` — `bookings:write`
10. `ComplaintsPage` — `complaints:write`
11. `MediaSubmissionsPage` — `media_submissions:approve`
12. `ReviewsPage` — `reviews:moderate`
13. `TicketsPage` / `TicketDetailPage` — `support:write`

### Phase 4 — Settings and read-only page guards
14. `SettingsPage` — `settings:write` for save button, disable inputs
15. `CurrencyRatesPage`, `WebhookSubscriptionsPage` — `settings:write`
16. `ServiceDetailPage` / `ServiceDetailMediaEditor` — `services:write`, `media_submissions:approve`
17. `PaymentsPage` — `payments:write` (refund), `reports:read` (export)

### Phase 5 — Provider portal pages
18. All portal pages — route guards and action button gates as listed in Section 6

---

## 8. Patterns Reference

### Hide a single button
```tsx
const canWrite = usePermission('bookings:write')
// ...
{canWrite && <button onClick={handleConfirm}>Confirm</button>}
```

### Hide a block of buttons (component wrapper)
```tsx
<PermissionGate code="payouts:approve">
  <button onClick={() => handlePay(id)}>Pay</button>
  <button onClick={() => handleHold(id)}>Hold</button>
</PermissionGate>
```

### Show read-only fallback instead of edit form
```tsx
<PermissionGate code="settings:write" fallback={<p className="text-slate-500">{value}</p>}>
  <input value={value} onChange={...} />
  <button type="submit">Save</button>
</PermissionGate>
```

### Disable inputs but keep them visible
```tsx
const canWrite = usePermission('settings:write')
<input value={value} onChange={canWrite ? handleChange : undefined} disabled={!canWrite} />
```

### Route guard in router
```tsx
<Route path="/management/payouts" element={
  <RequireAuth>
    <RequirePermission code="payouts:read">
      <PayoutsPage />
    </RequirePermission>
  </RequireAuth>
} />
```

---

## 9. Testing Checklist

For each page, test these three user scenarios:

| Scenario | Expected result |
|---|---|
| Super admin | All buttons visible, all data visible |
| Staff with `:read` only | Data visible, **zero write/approve buttons visible** |
| Staff with `:read` + `:write` | Data visible, create/edit/delete buttons visible, no approve buttons |
| Staff with `:read` + `:approve` | Data visible, approve/reject buttons visible, no create/edit buttons |
| Staff with no permission | Route guard shows Access Denied screen |
| Provider with `portal:access` only | Can see listings/bookings, cannot edit listings, no earnings page |

Use the **RolesPage** in the app to create test roles with the above combinations and verify each page behaves correctly.
