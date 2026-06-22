# Ziyara — System Changes Report
**Date:** 2026-05-22  
**Session type:** Gap-fill implementation + feature completion  
**Scope:** Full-stack (Spring Boot backend + React frontend)  
**Compiled by:** Claude Sonnet 4.6

---

## Executive Summary

This report documents all code changes applied to the Ziyara platform during a single implementation session. The work was driven by a gap analysis against the V1 architecture report. Seven distinct phases were executed, touching the landing app, company dashboard, provider portal, backend services, and shared infrastructure.

All TypeScript changes pass `tsc --noEmit` with zero errors.  
All Java changes pass `./gradlew compileJava` with zero errors.

---

## Dependency Changes

### Frontend (`front/my-app/package.json`)

| Package | Version | Reason |
|---|---|---|
| `zod` | `^4.4.3` | Client-side form validation |
| `@stomp/stompjs` | latest | WebSocket/STOMP client for live dashboard |
| `sockjs-client` | latest | SockJS transport fallback for STOMP |
| `@types/sockjs-client` | latest | TypeScript declarations for sockjs-client |

---

## Phase 0 — Broken Routes Fixed

**Motivation:** Sidebar links to `/support/tickets` and `/support/tickets/:ticketId` resolved to a 404 because no matching `<Route>` existed and the page components were never imported lazily.

### Files Changed

#### `front/my-app/src/apps/company/AppCompanyRoutes.tsx`
- Added lazy imports for `TicketsPage` and `TicketDetailPage`
- Added routes:
  ```tsx
  <Route path="/support/tickets" element={<TicketsPage />} />
  <Route path="/support/tickets/:ticketId" element={<TicketDetailPage />} />
  ```

#### `front/my-app/src/config/sidebar.ts`
- Added `{ id: 'tickets', label: 'Tickets', href: '/support/tickets' }` to the `support` section

---

## Phase 1 — Security Hardening

**Motivation:** JWTs stored in `localStorage` are accessible to any JavaScript on the page (XSS persistent theft). The 401 interceptor silently swallowed the `MFA_CODE_REQUIRED` error code by redirecting before the calling component could inspect it.

### Files Changed

#### `front/my-app/src/context/AuthContext.tsx`
- **JWT storage moved from `localStorage` to `sessionStorage`** — token is cleared automatically when the browser tab is closed, reducing the XSS persistence window
- Added `const store = sessionStorage` alias
- All `localStorage.getItem/setItem/removeItem` calls for `TOKEN_KEY`, `USER_KEY`, and `COOKIE_SESSION_KEY` replaced with `store.*`
- Exported `getStoredToken()` and `setStoredToken()` now read/write `sessionStorage` directly

#### `front/my-app/src/services/api.ts`
- Added `interface RetryableConfig extends InternalAxiosRequestConfig { _retried?: boolean }` to avoid TypeScript errors when tagging retried requests
- Added `_refreshing: boolean` flag and `_refreshQueue: Array<(token: string | null) => void>` — prevents multiple concurrent 401s from each triggering their own refresh attempt (thundering-herd guard)
- Added `clearSession()` helper that wipes all three sessionStorage keys atomically
- **Request interceptor** now reads the bearer token from `sessionStorage` on every request
- **401 response interceptor** rewritten with three branches:
  1. `MFA_CODE_REQUIRED` error code → passes the error through to the caller unchanged (so `LoginPage` can render the TOTP screen)
  2. Auth endpoints (`/auth/login`, `/auth/refresh`) → propagates error to caller, no redirect loop
  3. All other 401s → attempts silent token refresh, replays original request; on refresh failure calls `clearSession()` and redirects to `/login`
- `authAPI.login()` type extended: `mfaCode?: string`

#### `front/my-app/src/pages/MfaChallengePage.tsx` *(new file)*
- Inline 6-digit TOTP input — each digit occupies its own `<input>` in a `digits: string[]` array
- `useRef` array of 6 input elements for programmatic focus management
- Paste handler on the first field: distributes a 6-character paste across all fields automatically
- `onKeyDown` backspace: clears current field and moves focus to the previous one
- On submit: calls `authAPI.login({ email, password, mfaCode })` with the full 6-digit code
- On success: calls `setStoredToken`, `setUser`, then `onSuccess(getDashboardRouteForRole(role))`

#### `front/my-app/src/pages/LoginPage.tsx`
- Added `mfaCredentials` state: `{ email: string; password: string } | null`
- In the `catch` block: detects `error.response?.data?.code === 'MFA_CODE_REQUIRED'` and sets `mfaCredentials` instead of showing a generic error
- Renders `<MfaChallengePage>` when credentials are set; wraps existing form in a conditional so both cannot appear simultaneously

---

## Phase 2 — Provider Revenue Chart & Payout Requests

**Motivation:** The provider portal overview had no earnings visualisation, and there was no mechanism for providers to request payouts. Both were called out as missing features in the gap analysis.

### Backend Files Changed

#### `core/.../infrastructure/persistence/repository/PaymentJpaRepository.java`
- Added JPQL query `findCompletedByBookingIdsSince`:
  ```java
  @Query("SELECT p FROM PaymentJpaEntity p WHERE p.status = :status AND p.bookingId IN :bookingIds AND p.createdAt >= :since")
  List<PaymentJpaEntity> findCompletedByBookingIdsSince(
      @Param("status") PaymentStatus status,
      @Param("bookingIds") List<UUID> bookingIds,
      @Param("since") LocalDateTime since);
  ```

#### `core/.../domain/repository/PaymentRepository.java`
- Added interface method: `List<Payment> findCompletedByBookingIdsSince(List<UUID> bookingIds, LocalDateTime since)`

#### `core/.../infrastructure/persistence/adapter/PaymentRepositoryAdapter.java`
- Implemented `findCompletedByBookingIdsSince` — guards against empty `bookingIds` list to avoid JPQL `IN ()` syntax error; maps results via `paymentMapper`

#### `core/.../application/dto/response/PortalDashboardResponse.java`
- Added `List<WeeklyRevenueItem> weeklyRevenue` field
- Added static inner class:
  ```java
  @Data @Builder @NoArgsConstructor @AllArgsConstructor
  public static class WeeklyRevenueItem {
      private String week;    // ISO Monday date, e.g. "2025-05-12"
      private BigDecimal amount;
  }
  ```

#### `core/.../application/service/PortalService.java`
- Added `JdbcTemplate jdbcTemplate` field (constructor-injected)
- Added `buildWeeklyRevenue(List<UUID> bookingIds)`:
  - Fetches completed payments for the last 8 weeks via `paymentRepository.findCompletedByBookingIdsSince`
  - Groups payments by their ISO week Monday using `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)`
  - Fills all 8 week buckets — weeks with zero revenue appear as `0.00` rather than being absent
- Added `buildEmptyWeeks()` helper — generates the 8-week window anchored to today's Monday
- Added `createPayoutRequest(UUID providerId, PayoutRequestPayload payload)` — uses `JdbcTemplate` direct INSERT into `portal_payout_requests`; avoids full JPA entity overhead for a simple write
- `getDashboard()` now includes `weeklyRevenue` in the response builder

#### `core/.../resources/db/migration/V19__portal_payout_requests.sql` *(new file)*
```sql
CREATE TABLE IF NOT EXISTS portal_payout_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id     UUID NOT NULL REFERENCES service_providers(id) ON DELETE CASCADE,
    amount          NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    currency        VARCHAR(10) NOT NULL DEFAULT 'USD',
    notes           TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','PROCESSING','COMPLETED','REJECTED')),
    requested_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,
    processed_by    UUID REFERENCES sys_users(id),
    rejection_reason TEXT
);
```

#### `core/.../application/dto/request/PayoutRequestPayload.java` *(new file)*
- `@NotNull @DecimalMin("0.01") BigDecimal amount`
- `String notes` (optional)

#### `core/.../application/dto/response/PayoutRequestResponse.java` *(new file)*
- Fields: `id`, `amount`, `currency`, `status`, `requestedAt`

#### `core/.../presentation/controller/PortalController.java`
- Added `POST /portal/payout-request` endpoint — validates payload, delegates to `portalService.createPayoutRequest`

### Frontend Files Changed

#### `front/my-app/src/types/api.ts`
- Added `WeeklyRevenueItem { week: string; amount: number }`
- Added `weeklyRevenue?: WeeklyRevenueItem[]` to `PortalDashboardDto`

#### `front/my-app/src/services/api.ts`
- Added `portalAPI.requestPayout(body: { amount: number; notes?: string })` → `POST /portal/payout-request`

#### `front/my-app/src/pages/portal/ClientPortalOverview.tsx`
- Added recharts bar chart below KPI cards:
  - `ResponsiveContainer + BarChart + Bar` using `weeklyRevenue` data from the dashboard API
  - X-axis formatted as short ISO week dates; tooltip shows `amount`
  - Conditionally rendered — hidden if `weeklyRevenue` is absent or empty

#### `front/my-app/src/pages/portal/PortalEarningsPage.tsx`
- Added `PayoutRequestModal` component (inline) with amount input and optional notes textarea
- Added `showPayoutModal` state
- "Request Payout" button appears next to total earnings when `totalEarnings > 0`
- Modal calls `portalAPI.requestPayout()` on confirm, shows success/error feedback

#### `front/my-app/src/i18n/translations.ts`
- EN: `portalHome.weeklyRevenue: 'Weekly Revenue (last 8 weeks)'`
- AR: `portalHome.weeklyRevenue: 'الإيرادات الأسبوعية (آخر 8 أسابيع)'`

---

## Phase 3 — Landing Booking Flow

**Motivation:** The landing app had no end-to-end booking path. Users could browse services but had no way to select dates, choose guests, or confirm a booking. Unauthenticated users who clicked "Book Now" were dropped to `/login` with no way back to their intent.

### Files Changed

#### `front/my-app/src/services/api.ts`
- Added `bookingsAPI.create(body)` → `POST /bookings` with `serviceId`, `checkInDate`, `checkOutDate`, `guests`, `discountCode`, `specialRequests`, `currency`

#### `front/my-app/src/apps/landing/LandingServiceBookingPanel.tsx` *(full rewrite)*
- Date pickers for hotels/resorts: `checkIn` and `checkOut` with `min` validation (cannot select past dates; check-out cannot precede check-in)
- Guest counter with `+`/`−` buttons, clamped to 1–20
- `nights` computed from date diff with `Math.floor(diff / 86_400_000)`
- `stayTotal = basePrice × nights`
- Room selector (second step for stays): 20-room grid; `BOOKED_ROOM_INDICES` pre-seeds some rooms as unavailable in red
- Taxi route picker: From/To text inputs + Economy/VIP/Van type selector with fare estimate multipliers (VIP ×1.45, Van ×1.25)
- `buildCheckoutUrl()` encodes all booking intent into `/checkout?serviceId=...&checkIn=...&checkOut=...&guests=...`
- `proceed()`: authenticated users navigate directly to checkout; guests navigate to `/login?next=<encoded-checkout-url>`

#### `front/my-app/src/apps/landing/LandingCheckoutPage.tsx` *(new file)*
- Reads `serviceId`, `checkIn`, `checkOut`, `guests`, `discount` from URL search params
- `useEffect` redirects to `/login?next=/checkout?...` if `!isAuthenticated`
- Fetches service details via `servicesAPI.get(serviceId)`
- Calculates `nights = (checkOut - checkIn) / 86_400_000` and `subtotal = pricePerNight × nights`
- Renders: service summary card (name, city, dates, nights×guests, subtotal), discount code input (auto-uppercased), special requests textarea
- Calls `bookingsAPI.create()` on confirm; shows a success screen with the booking reference number when done

#### `front/my-app/src/apps/landing/AppLandingRoutes.tsx`
- Added lazy import for `LandingCheckoutPage`
- Added `<Route path="/checkout" element={<LandingCheckoutPage />} />` outside `LandingShell` (no nav chrome on checkout)

#### `front/my-app/src/i18n/translations.ts`
- Added to `landingBooking` (EN + AR): `totalForNights`, `confirmBooking`, `selectDates`, `checkIn`, `checkOut`, `guests`, `selectRoom`, `chooseRoomTitle`, `roomLegend`, `ridePlanTitle`, `fromLabel`, `toLabel`, `fromPlaceholder`, `toPlaceholder`, `vehicleType`, `estimateLabel`, `requestRide`, `bookNow`, `confirmContinue`, `pricePerNight`, `startingFrom`
- Added new `checkout` section (EN + AR): `title`, `back`, `basePrice`, `discountCode`, `specialRequests`, `specialRequestsPlaceholder`, `confirming`, `confirmPay`, `successTitle`, `successBody`, `browseMore`, `serviceNotFound`, `termsNote`

---

## Phase 4 — Notification Badge Pre-fetch

**Motivation:** The notification bell icon in the dashboard header showed no unread count until the user opened the notification panel, creating a broken first impression.

### Files Changed

#### `front/my-app/src/components/DashboardHeader.tsx`
- Added `import { notificationsAPI }` and `import type { NotificationInboxDto }`
- Added `useEffect` that fires on mount (when `showNotifications` is truthy) and calls `notificationsAPI.list({ page: 0, size: 1 })`:
  - On success: sets `unreadNotificationCount` from `inbox.unreadCount`
  - On error: silently ignored — badge simply shows nothing rather than crashing
- Result: the red unread-count badge is populated immediately on page load

---

## Phase 5 — Zod Form Validation

**Motivation:** All forms used hand-rolled `if (!field)` guards with `setError(singleMessage)`. This gives no per-field feedback and is inconsistent across forms.

### Files Changed

#### `front/my-app/src/lib/validation.ts` *(new file)*

Five schemas exported:

| Schema | Fields validated |
|---|---|
| `loginSchema` | `email` (valid email), `password` (required) |
| `signUpSchema` | `email`, `password` (min 8), `confirmPassword` + cross-field match refine |
| `listingSchema` | `name` (min 2 / max 120), `description` (min 10 / max 2000), `price` (positive number), `currency`, `category` enum |
| `portalListingSchema` | `name` (min 2 / max 120), `description` (max 2000), `basePrice` (parseable non-negative string), `currency` |
| `checkoutSchema` | `discountCode` (optional), `specialRequests` (max 500) |

The `portalListingSchema` uses a `z.string().refine()` for `basePrice` to match the existing `num()` parse function already in the form, keeping both paths consistent.

#### Forms wired

All five forms follow the same pattern:
1. `setFieldErrors({})` at the top of `handleSubmit`
2. `schema.safeParse(...)` — on failure, extracts `flatten().fieldErrors` and calls `setFieldErrors`
3. Early `return` if invalid — API is never called with bad data
4. Each input gets `style={{ borderColor: fieldErrors.x ? '#f87171' : undefined }}` for red-border highlight
5. `{fieldErrors.x ? <p className="mt-1 text-xs text-red-600">…</p> : null}` for inline message

| File | Schema used |
|---|---|
| `front/my-app/src/pages/LoginPage.tsx` | `loginSchema` |
| `front/my-app/src/apps/landing/LandingLoginPage.tsx` | `loginSchema` |
| `front/my-app/src/apps/landing/LandingSignUpPage.tsx` | `signUpSchema` |
| `front/my-app/src/apps/landing/LandingCheckoutPage.tsx` | `checkoutSchema` |
| `front/my-app/src/pages/portal/PortalListingFormPage.tsx` | `portalListingSchema` |

**Breaking change removed:** `LandingSignUpPage` previously validated `password.length < 6`. The `signUpSchema` raises the minimum to 8 characters, which is the correct security baseline.

---

## Phase 6 — Bulk Actions

**Motivation:** Admins managing large booking and provider lists had to act on records one at a time. High-volume operations (e.g., confirming all pending bookings after a payment processor batch) required O(n) clicks.

### Files Changed

#### `front/my-app/src/components/BulkActionBar.tsx` *(new file)*

Reusable sticky action bar:
- Returns `null` when `selectedCount === 0` — zero layout cost when nothing is selected
- Sticky position: `top-[4.25rem]` — sits just below the fixed dashboard header
- `z-30` — above table content, below modals
- Danger variant (red) for destructive actions; default variant (primary blue) for safe actions
- "Clear" link right-aligned to dismiss selection

Props:
```typescript
interface BulkActionBarProps {
  selectedCount: number
  actions: Array<{
    label: string
    onClick: () => void
    variant?: 'danger' | 'default'
    disabled?: boolean
  }>
  onClearSelection: () => void
}
```

#### `front/my-app/src/pages/management/BookingsPage.tsx`

- Added `selectedIds: Set<string>` state
- `isAllSelected` computed: `bookings.every(b => selectedIds.has(b.id))`
- `toggleAll()` — selects all on current page if any are deselected; deselects all otherwise
- `toggleOne(id)` — adds/removes a single ID using functional set update
- `bulkConfirm()` — filters to PENDING bookings in selection, calls `bookingsAPI.confirm` for each via `Promise.allSettled`, optimistically updates status in state, clears selection
- `bulkCancel()` — cancels all selected bookings via `Promise.allSettled`, optimistically updates state, clears selection
- `BulkActionBar` rendered between the error banner and the status pills; "Confirm selected" disabled when no PENDING bookings are selected
- Checkbox `<th>` added to table header (select-all)
- Checkbox `<td>` added to each row; selected rows get `bg-primary/5` highlight

#### `front/my-app/src/pages/management/ProvidersPage.tsx`

- Same checkbox/selection pattern as BookingsPage
- `bulkApprove()` — filters to `PENDING_APPROVAL` providers only; calls `providersAPI.approve`
- `bulkSuspend()` — filters to `ACTIVE` providers only; calls `providersAPI.suspend`
- `BulkActionBar` rendered with "Approve selected" (disabled if no PENDING_APPROVAL in selection) and "Suspend selected" danger action (disabled if no ACTIVE in selection)

#### `front/my-app/src/i18n/translations.ts`

Added keys (EN + AR):

| Key | EN | AR |
|---|---|---|
| `bookingsPage.bulkConfirm` | Confirm selected | تأكيد المحدد |
| `bookingsPage.bulkCancel` | Cancel selected | إلغاء المحدد |
| `providersPage.bulkApprove` | Approve selected | موافقة على المحدد |
| `providersPage.bulkSuspend` | Suspend selected | تعليق المحدد |

---

## Phase 7 — WebSocket Live Dashboard

**Motivation:** The company dashboard polled `GET /dashboard/live` every 45 seconds regardless of tab visibility or network cost. The backend already had a STOMP/WebSocket endpoint (`/ws`) configured; it was unused by the dashboard.

### Backend Files Changed

#### `core/.../infrastructure/job/DashboardLiveBroadcaster.java` *(new file)*

```java
@Component @RequiredArgsConstructor @Slf4j
public class DashboardLiveBroadcaster {

    private static final String TOPIC = "/topic/dashboard/live";

    @Scheduled(fixedDelay = 30_000, initialDelay = 30_000)
    public void broadcast() {
        try {
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(30);
            DashboardLiveResponse payload = dashboardBootstrapService.loadLive(start, end, 15);
            messagingTemplate.convertAndSend(TOPIC, payload);
        } catch (Exception e) {
            log.warn("Dashboard live broadcast failed: {}", e.getMessage());
        }
    }
}
```

- Runs every 30 seconds with a 30-second initial delay (avoids firing during application startup)
- Uses the existing `DashboardBootstrapService.loadLive()` — assembles KPIs + activity + service health in parallel on the dashboard executor thread pool
- Broadcasts to `/topic/dashboard/live` — all subscribed clients receive the update simultaneously
- Errors are caught and logged as warnings — a failed broadcast does not crash the scheduler

**Why `@Scheduled` rather than `@MessageMapping`:** A subscription-triggered approach would query the database once per connected client. A scheduled broadcast queries once and fans out to all clients, which is more efficient under load.

### Frontend Files Changed

#### `front/my-app/src/hooks/useDashboardWebSocket.ts` *(new file)*

```typescript
export function useDashboardWebSocket(
  bootstrapQueryKey: readonly [string, string, string, string],
): boolean  // returns `connected` flag
```

- Derives WS URL from `VITE_API_URL` by stripping `/api/v1` suffix: `apiBase.replace(/\/api\/v1\/?$/, '') + '/ws'`
- Creates a `@stomp/stompjs` `Client` with a `SockJS` factory for transport fallback
- Attaches the Bearer token from `sessionStorage` in `connectHeaders`
- On connect: subscribes to `/topic/dashboard/live`; on each frame, calls `queryClient.setQueryData` to patch the existing bootstrap cache entry with `{ kpis, activity, serviceHealth }` from the live payload — no extra React re-render cycle from a polling refetch
- On disconnect or STOMP error: sets `connected = false`
- Returns `connected` boolean so the caller can disable polling

#### `front/my-app/src/pages/DashboardPage.tsx`

- Added `import { useDashboardWebSocket }`
- `const wsConnected = useDashboardWebSocket(bootstrapQueryKey)` called after the query client setup
- Live query `enabled` changed from `bootstrapQuery.isSuccess` to `bootstrapQuery.isSuccess && !wsConnected`

**Fallback behaviour:** If the WebSocket fails to connect (backend down, proxy blocks WS upgrades), `wsConnected` stays `false` and the existing 45-second polling resumes automatically. No configuration needed.

---

## Architecture Impact Summary

| Area | Before | After |
|---|---|---|
| JWT persistence | `localStorage` (persists across tabs) | `sessionStorage` (cleared on tab close) |
| MFA flow | 401 swallowed by interceptor, TOTP screen unreachable | `MFA_CODE_REQUIRED` passes through; inline TOTP screen renders |
| Token refresh | Single attempt, no queue | Queue-and-retry; one refresh for N concurrent 401s |
| Provider earnings | No chart | 8-week Recharts bar chart in portal overview |
| Payout requests | No mechanism | `portal_payout_requests` table (V19), API endpoint, portal UI modal |
| Landing booking | Browse-only | Full date/guest selection → checkout → booking confirmation |
| Form validation | Hand-rolled single error per form | Zod per-field errors with red-border highlight on all 5 key forms |
| Bulk operations | One-at-a-time clicks | Select-all checkbox + bulk confirm/cancel/approve/suspend |
| Dashboard updates | HTTP polling every 45s | STOMP WebSocket push every 30s; polling fallback if WS fails |

---

## Security Notes

- **`seed_dev.sql` must never be applied in staging or production** — seeded credentials and disabled constraints are development-only
- **`.env` placeholder values must be replaced** before first run (`JWT_SECRET`, `PAYMENT_WEBHOOK_SECRET`, database credentials)
- **`APP_DEMO_SUPER_ADMIN_ENABLED=false`** must be set after initial login in production
- **`ZIYARA_SECURITY_MFA_REQUIRED_ROLES`** — leave empty in development to avoid locking seeded admin accounts before TOTP enrolment
- **`PAYMENT_WEBHOOK_SECRET`** must not be a weak or default value in any deployed environment
- JWT storage is now `sessionStorage` — this trades some UX convenience (user must re-login after closing all tabs) for a meaningfully reduced XSS persistence window. Refresh tokens in `HttpOnly` cookies remain unaffected.

---

## File Index — All Changed Files

### New Files Created

| Path | Description |
|---|---|
| `core/.../infrastructure/job/DashboardLiveBroadcaster.java` | Scheduled STOMP broadcaster |
| `core/.../resources/db/migration/V19__portal_payout_requests.sql` | Payout requests table migration |
| `core/.../application/dto/request/PayoutRequestPayload.java` | Payout request input DTO |
| `core/.../application/dto/response/PayoutRequestResponse.java` | Payout request output DTO |
| `front/my-app/src/pages/MfaChallengePage.tsx` | TOTP 6-digit challenge screen |
| `front/my-app/src/apps/landing/LandingCheckoutPage.tsx` | Landing checkout page |
| `front/my-app/src/components/BulkActionBar.tsx` | Reusable bulk action sticky bar |
| `front/my-app/src/lib/validation.ts` | Zod schema library |
| `front/my-app/src/hooks/useDashboardWebSocket.ts` | STOMP WebSocket hook |

### Modified Files

| Path | Summary of changes |
|---|---|
| `core/.../PaymentJpaRepository.java` | Added `findCompletedByBookingIdsSince` JPQL query |
| `core/.../PaymentRepository.java` | Added domain repository method |
| `core/.../PaymentRepositoryAdapter.java` | Implemented adapter method |
| `core/.../PortalDashboardResponse.java` | Added `weeklyRevenue` + `WeeklyRevenueItem` inner class |
| `core/.../PortalService.java` | Added weekly revenue builder + payout request insert |
| `core/.../PortalController.java` | Added POST /portal/payout-request endpoint |
| `front/my-app/src/context/AuthContext.tsx` | sessionStorage for JWT, exported get/set helpers |
| `front/my-app/src/services/api.ts` | Silent refresh, MFA pass-through, bookingsAPI.create, portalAPI.requestPayout |
| `front/my-app/src/pages/LoginPage.tsx` | MFA challenge integration, Zod validation |
| `front/my-app/src/apps/landing/LandingLoginPage.tsx` | Zod validation, field errors |
| `front/my-app/src/apps/landing/LandingSignUpPage.tsx` | Zod validation, field errors, password min raised to 8 |
| `front/my-app/src/apps/landing/LandingCheckoutPage.tsx` | Zod validation on submit |
| `front/my-app/src/apps/landing/LandingServiceBookingPanel.tsx` | Full rewrite — dates, guests, taxi, room selector, checkout URL |
| `front/my-app/src/apps/landing/AppLandingRoutes.tsx` | Added /checkout route |
| `front/my-app/src/apps/company/AppCompanyRoutes.tsx` | Added /support/tickets routes |
| `front/my-app/src/components/DashboardHeader.tsx` | Unread count pre-fetch on mount |
| `front/my-app/src/pages/DashboardPage.tsx` | WebSocket hook wired, polling disabled when WS connected |
| `front/my-app/src/pages/portal/ClientPortalOverview.tsx` | Weekly revenue bar chart |
| `front/my-app/src/pages/portal/PortalEarningsPage.tsx` | Payout request modal |
| `front/my-app/src/pages/portal/PortalListingFormPage.tsx` | Zod validation, field errors |
| `front/my-app/src/pages/management/BookingsPage.tsx` | Checkbox column, bulk confirm/cancel |
| `front/my-app/src/pages/management/ProvidersPage.tsx` | Checkbox column, bulk approve/suspend |
| `front/my-app/src/types/api.ts` | WeeklyRevenueItem, PortalDashboardDto.weeklyRevenue |
| `front/my-app/src/config/sidebar.ts` | Added tickets nav item |
| `front/my-app/src/i18n/translations.ts` | ~40 new translation keys across EN + AR |

---

*Report generated 2026-05-22. All changes committed in a single session against the post-production Ziyara codebase.*
