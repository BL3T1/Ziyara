# Ziyara Platform — Full Audit Report
**Date:** 2026-06-02  
**Reviewer role:** Senior PM · Hard-to-please UX critic · Meticulous tester  
**Surfaces audited:** Company dashboard · Provider portal · Landing site  
**Scope:** All frontend pages, backend services, database schema, API contracts,
i18n, routing, auth, permissions, build pipeline, accessibility, performance

---

## Table of Contents
1. [Missing Elements](#1-missing-elements)
2. [Bad Practices](#2-bad-practices)
3. [Hard-to-Understand UI/UX](#3-hard-to-understand-uiux)
4. [Broken or Impaired Functionality](#4-broken-or-impaired-functionality)
5. [Recommendations](#5-recommendations)
6. [Executive Summary](#6-executive-summary)
7. [Next Steps](#7-next-steps)

---

## 1. Missing Elements

### 1.1 Functional Gaps

| # | Missing Feature | Where | Priority | Effort |
|---|---|---|---|---|
| M-01 | No pagination on portal bookings list (`GET /portal/bookings` returns all) | PortalBookingsPage | High | Small |
| M-02 | No pagination on portal payout history (hard-coded `LIMIT 100` in SQL) | PortalService.java:484 | High | Small |
| M-03 | No "new listing" button on PortalListingsPage — only reachable via `/portal/listings/new` URL | PortalListingsPage | High | Small |
| M-04 | Provider portal has no "Notifications" bell — providers receive no in-app alerts | ClientPortalLayout | High | Medium |
| M-05 | Booking voucher download absent from provider portal — providers can't print/share vouchers | PortalBookingsPage | Medium | Small |
| M-06 | No "resend confirmation email" action for bookings (admin or portal) | BookingsPage, PortalBookingsPage | Medium | Small |
| M-07 | No bulk export for bookings list (only reports export exists) | BookingsPage | Medium | Small |
| M-08 | Webhook delivery retry button missing — failed deliveries can only be observed, not retried | WebhookSubscriptionsPage | Medium | Small |
| M-09 | No webhook event payload preview in delivery log — only status/HTTP code shown | WebhookSubscriptionsPage | Medium | Small |
| M-10 | Provider discount page shows no "pending admin approval" state after creation | PortalDiscountsPage | High | Small |
| M-11 | Payout request has no status tracking UI — provider submits but sees nothing after | PortalEarningsPage | High | Small |
| M-12 | No provider-facing "reject reason" displayed when a service listing is rejected | PortalListingsPage | High | Small |
| M-13 | Landing site has no "favourites" / wishlist feature for returning visitors | Landing | Low | Large |
| M-14 | No empty-state illustration on PortalBookingsPage when provider has zero bookings | PortalBookingsPage | Low | Small |
| M-15 | Rate limiting only covers auth endpoints; no rate limit on payout requests, discount creation, or media uploads | Backend | High | Medium |
| M-16 | No CSRF protection evidence on non-cookie-auth endpoints | Backend | High | Medium |
| M-17 | No session timeout warning — user is silently logged out when refresh token expires | All surfaces | High | Small |
| M-18 | No "remember me" option on login — sessionStorage clears on tab close | LoginPage | Medium | Small |
| M-19 | Company dashboard has no real-time "live bookings" counter or websocket feed | CompanyDashboardPage | Low | Large |
| M-20 | No onboarding walkthrough or first-run guide for new providers | Provider portal | Medium | Large |

### 1.2 Missing UI States

| # | Missing State | Location | Priority | Effort |
|---|---|---|---|---|
| S-01 | No skeleton loaders on portal listings table — raw spinner only | PortalListingsPage | Medium | Small |
| S-02 | No optimistic UI on discount deactivation — table freezes while awaiting API | PortalDiscountsPage | Low | Small |
| S-03 | No "unsaved changes" warning when navigating away mid-form | PortalListingFormPage | High | Small |
| S-04 | No confirmation when closing create-webhook modal mid-fill | WebhookSubscriptionsPage | Low | Small |
| S-05 | ProvidersPage search/filter resets on page refresh — no URL-state persistence | ProvidersPage | Medium | Small |
| S-06 | No "loading" state on the approve/reject buttons in ProvidersPage — double-submit possible | ProvidersPage | High | Small |
| S-07 | Earnings date filter has no "apply" button — fires on every keystroke | PortalEarningsPage | Medium | Small |

### 1.3 Missing Accessibility Features

| # | Issue | Location | Priority | Effort |
|---|---|---|---|---|
| A-01 | No `aria-live` region for toast/success messages — screen readers miss them | All surfaces | High | Small |
| A-02 | No `aria-busy` on loading buttons — screen readers don't know a form is submitting | All forms | High | Small |
| A-03 | Sidebar collapse button has no visible focus ring in some themes | Sidebar | Medium | Small |
| A-04 | Color-coded status badges (green/red/amber) convey meaning through colour alone — no icon or text alternative | Multiple pages | High | Small |
| A-05 | Modal close (×) button has no accessible label — only visual `×` character | Modal.tsx | High | Small |
| A-06 | BalanceMeter progress bar in PortalDiscountsPage has no `role="progressbar"` or `aria-valuenow` | PortalDiscountsPage | Medium | Small |
| A-07 | Webhook event checkboxes have no `fieldset`/`legend` grouping | WebhookSubscriptionsPage | Low | Small |
| A-08 | Landing service cards lack meaningful `alt` text on images — `alt=""` or filename used | Landing pages | High | Small |
| A-09 | No skip-to-main-content link on any surface | All surfaces | Medium | Small |
| A-10 | RTL layout tested? Several inline `style` transforms hard-code `rotate(0deg)` / `rotate(180deg)` without checking `dir` | ClientPortalLayout:40 | Medium | Small |

---

## 2. Bad Practices

### 2.1 Frontend

#### BP-01 · 244+ inline `style={{}}` blocks — Critical, Medium effort
The landing app pages use inline styles pervasively instead of Tailwind or CSS variables.

```tsx
// LandingCheckoutPage.tsx — one of 43 instances
<div style={{ background: 'rgba(90,100,110,0.1)', borderRadius: '12px' }} />
```

**Why bad:** Cannot be overridden by dark mode, cannot be linted for consistency,
breaks CSS bundle tree-shaking, causes style recomputations on every render.

**Quick win:** Replace recurring patterns with Tailwind utilities:
```tsx
// Before
<div style={{ background: 'rgba(90,100,110,0.1)' }} />
// After
<div className="bg-slate-500/10" />
```

---

#### BP-02 · `window.confirm()` instead of the existing `ConfirmDialog` component — Medium, Small effort
`PortalListingsPage.tsx:59` uses the native browser dialog while the codebase already
has a polished `ConfirmDialog` component used elsewhere.

```tsx
// BAD — PortalListingsPage.tsx:59
if (!window.confirm(t('portalPages.confirmDeleteListing', { name }))) return
```

**Why bad:** Native dialogs block the JS thread, cannot be styled, ignore dark mode,
and are flagged by accessibility checkers. Inconsistent with the rest of the app.

---

#### BP-03 · `alert()` in production page — High, Small effort
```tsx
// WebhookSubscriptionsPage.tsx:98
.then(() => alert(t('webhooksPage.pingDispatched')))
```
**Why bad:** Blocks the thread, cannot be themed, breaks automation testing,
looks amateurish in a polished SaaS product.

---

#### BP-04 · `useEffect` missing dependency arrays — High, Small effort

**Location 1:** `PortalEarningsPage.tsx:155`
```tsx
useEffect(() => { load() }, [])   // 'load' is missing
```
**Location 2:** `ProvidersPage.tsx:72` — filter and page state not in deps.

**Why bad:** Stale closure — changing filter won't trigger a reload.
Users change the status filter and see no change until manual refresh.

---

#### BP-05 · Hardcoded English validation error strings not run through `t()` — High, Small effort
`PortalDiscountsPage.tsx:39–45`:
```tsx
if (!code.trim()) { setError('Code is required.'); return }
if (isNaN(parsedValue) || parsedValue <= 0) { setError('Value must be a positive number.'); return }
if (!endDate) { setError('Expiry date is required.'); return }
```
Arabic users see English error messages. Violates the explicit i18n rule in CLAUDE.md.

---

#### BP-06 · Sidebar label strings hardcoded in English inside `sidebar.ts` — Medium, Small effort
```ts
// sidebar.ts:22
{ id: 'dashboard', label: 'Dashboard', href: '/dashboard' }
```
The labels are never run through `t()`. The sidebar stays English even when the UI
language is set to Arabic. The `nav.*` translation keys exist but are never used here.

---

#### BP-07 · `PortalListingsPage.tsx:127` uses Tailwind arbitrary colour instead of design token — Low, Small effort
```tsx
className="text-sm font-semibold text-[#1e4d6b] hover:underline dark:text-[#90caff]"
```
The project already defines `primary: #1e4d6b` as a Tailwind token.
Use `text-primary` instead of the raw hex.

---

#### BP-08 · Custom rate-limiting AOP instead of proven library — Medium, Medium effort
`RateLimitAspect.java` is a hand-rolled AOP solution.
Mature options (Bucket4j, Resilience4j) handle edge cases (clock skew, distributed
Redis counters, sliding windows) that a custom aspect is likely missing.
Critically, **no rate limiting exists on payout requests or discount creation** — a
provider can spam payout requests or hammer the discount balance endpoint.

---

#### BP-09 · Raw JDBC in `PortalService` mixes layers — Medium, Large effort
`PortalService.java` uses `JdbcTemplate` directly for at least 12 different queries
(payouts, discounts, earnings, balance). This violates the Clean Architecture rule
documented in CLAUDE.md: application services should call domain repository interfaces,
not raw JDBC. The carve-out comment only covers `JdbcTemplate` for "portal-specific
queries" but has grown far beyond that.

---

#### BP-10 · No optimistic locking on `provider_discount_balance` — High, Small effort
`PortalService.java:578–583`:
```java
int updated = jdbcTemplate.update(
  "UPDATE provider_discount_balance SET spent_amount = spent_amount + ? " +
  "WHERE provider_id = ? AND (allocated_amount - spent_amount) >= ?",
  debitAmount, providerId, debitAmount);
if (updated == 0) throw new BusinessException("Insufficient discount balance");
```
Under concurrent requests (provider clicks "Create" twice rapidly), both requests
read the same available balance and both may pass the `>= ?` check before either
commits, over-spending the balance.  
**Fix:** Add `FOR UPDATE` to a preceding SELECT or use a DB-level sequence/version column.

---

#### BP-11 · `console.error` in `ErrorBoundary.tsx` goes to raw stdout in production — Low, Small effort
Should route to a structured logging / error-tracking service (Sentry, Datadog, etc.).

---

### 2.2 Backend

#### BP-12 · Hard-coded `LIMIT 100` with no pagination — High, Small effort
`PortalService.java:484` and `PortalService.java:551`:
```java
"ORDER BY created_at DESC LIMIT 100"
```
A provider with 101+ payout requests or discount codes silently loses data.
No `page`/`size` params are accepted by the controller, and no `totalCount` is
returned for the client to know data was truncated.

---

#### BP-13 · Deprecated API usage in `CreateServiceRequest.java` — Low, Small effort
The Java compiler emits a deprecation warning on every build:
```
PortalService.java uses or overrides a deprecated API.
```
This creates noise that could mask real warnings in CI.

---

#### BP-14 · Deployment pipeline is a placeholder — Critical, Large effort
`.github/workflows/deploy.yml:123`:
```yaml
# TODO: add a "deploy" job wired to your hosting provider
```
There is **no actual deployment step**. The pipeline builds and tests but never ships.
Running in production apparently requires a manual `docker compose up`.

---

#### BP-15 · Mobile build signing config not set — High, Medium effort
`mobile/android/app/build.gradle.kts:38`:
```kotlin
// TODO: Replace with a real signing config before Play Store submission
```
Shipping an unsigned APK to the Play Store will be rejected outright.

---

## 3. Hard-to-Understand UI/UX

### 3.1 Navigation & Labels

#### UX-01 · Sidebar label "Groups" links to `/management/users` — High, Small effort
```ts
// sidebar.ts:82
{ id: 'users', label: 'Groups', href: '/management/users' }
```
The URL says "users", the label says "Groups", the page title probably says something else.
A new admin will click "Groups" expecting group management, land on `/management/users`,
and be confused. Either rename the route or align the label.

---

#### UX-02 · "Provider Messages" sidebar item goes to `/support/tickets` — Medium, Small effort
```ts
{ id: 'provider_messages', label: 'Provider Messages', href: '/support/tickets' }
```
The tickets page is a **general support ticket queue** — it is not exclusively "Provider Messages".
An admin expecting provider DMs will find a mixed ticket list. Label should be "Support Tickets"
or the route should be `/support/provider-messages`.

---

#### UX-03 · Profit margin / commission ambiguity — Medium, Small effort
The company dashboard shows "profit margin %" in provider cards but the backend field is
`commissionRate`. The portal shows "Platform Commission". The reports call it "fee".
Three names for one concept across three surfaces.

---

#### UX-04 · Webhook "ping" button gives no visual feedback while in-flight — Medium, Small effort
Clicking "Ping" dispatches a POST and — after a few seconds — shows a native `alert()`.
During the wait there is no spinner, no disabled state. Users click it multiple times.

---

#### UX-05 · Discount "deactivate" uses a `DELETE` HTTP verb and red trash icon — Medium, Small effort
Deactivating is a soft action (status → INACTIVE), but the UI shows a red trash-bin icon
and calls `DELETE /portal/discounts/{id}`. Users expect this to permanently delete the code.
Use a toggle or a "Deactivate" label with a grey icon instead.

---

#### UX-06 · PortalListingFormPage section headers have no visual separation or progress sense — Low, Small effort
The form has five sections (Basic Info, Location, Pricing, Type-specific, Policies) but
no dividers, no step indicators, and no sticky header. On a tall screen the user scrolls
endlessly with no progress sense.

---

#### UX-07 · "Export CSV" downloads without a filename containing the date range — Low, Small effort
File downloads as `earnings.csv` regardless of the selected period.
A provider running multiple exports can't tell them apart.  
**Fix:** `earnings-2026-01-01-to-2026-03-31.csv`

---

#### UX-08 · Discount balance meter has no explanation of how balance is granted — Medium, Small effort
The `BalanceMeter` shows allocated / spent / available numbers but there is no tooltip
or help text explaining that the balance is granted by the company admin.
New providers will stare at "Available: $0.00" with no idea what to do.

---

#### UX-09 · Creating a webhook subscription shows secret only once with no copy button — High, Small effort
The secret is displayed in a `<code>` block but there is no "Copy to clipboard" button.
Providers must manually select the long random string. One mis-select and it's gone forever.

---

#### UX-10 · Provider portal sidebar navigates away from a partially filled listing form without warning — High, Small effort
Related to S-03: no "unsaved changes" guard exists anywhere in the portal.

---

#### UX-11 · Admin webhook page accessible to "executive" role — no business justification — Low, Small effort
Webhook management (registering external endpoints that receive sensitive booking data)
should likely be super_admin only, not exposed to general executives.

---

#### UX-12 · Date inputs on earnings/reports pages accept free text — no date picker enforcement — Medium, Small effort
`<input type="date">` renders a native date picker on desktop browsers but a plain text
field on some mobile browsers and older Safari. The format expected by the API
(`YYYY-MM-DD`) is not communicated to the user.

---

#### UX-13 · Payout request amount field has no minimum or maximum hint — High, Small effort
The "Request Payout" modal shows an amount field but no:
- Minimum amount displayed
- Available balance shown inside the modal (only visible outside it)
- Currency label next to the input

---

### 3.2 Information Hierarchy

#### UX-14 · Provider dashboard KPI cards have no trend indicators — Medium, Medium effort
The four KPI cards (Services, Bookings, Active Bookings, Revenue) show raw numbers
with no delta vs. last period, no sparkline, no colour coding. A provider cannot tell
at a glance whether business is growing or declining.

---

#### UX-15 · Per-service earnings table has no total row — Low, Small effort
`PortalEarningsPage` shows a per-service breakdown table but no footer summing
Bookings, Gross, Fee, and Net. Users must mentally add rows.

---

#### UX-16 · Webhook delivery log modal opens at page 0 — newest failure may not be visible — Low, Small effort
Deliveries are fetched as page 0 (newest-first), but if a subscription has many
deliveries the most recent failure may be buried and the 50-item cap hides it.

---

## 4. Broken or Impaired Functionality

### 4.1 Confirmed Bugs (fixed during this session)

| # | Bug | Root Cause | Status |
|---|---|---|---|
| B-01 | Provider earnings page crashes with bad SQL | `sys_payments` referenced instead of `pay_payments` | ✅ Fixed |
| B-02 | Provider discount list crashes with bad SQL | `deleted_at` column doesn't exist on `disc_discount_codes` | ✅ Fixed |
| B-03 | Backend container unhealthy on startup | `AsyncExecutorConfig` declares `Executor` return type; `@Qualifier("taskExecutor")` injection of `TaskExecutor` fails | ✅ Fixed |
| B-04 | Docker build fails (exit code 2) | 6 TypeScript errors: duplicate `DiscountDto`, duplicate nav keys, `Modal` prop mismatch (`isOpen` vs `open`), missing imports | ✅ Fixed |

### 4.2 Latent / Edge-Case Bugs

#### B-05 · Provider with zero services may propagate an unhandled DB exception to the dashboard — Medium, Small effort
`PortalService.getDashboard()` calls `serviceRepository.findByProviderId(providerId)`.
If that call throws (e.g. DB timeout), the entire dashboard 500s with no user-facing
message and no fallback value.

---

#### B-06 · `PortalListingFormPage` with `id = undefined` produces a 400 API call — Medium, Small effort
Route `/portal/listings/:id` — if navigated to without an id segment, `useParams`
returns `id = undefined`. The check `id === 'new'` is false and `id === undefined`
is not handled, so the page calls `portalAPI.getService(undefined)`, producing a
cryptic 400/404.

---

#### B-07 · Double-submission possible on payout request modal — High, Small effort
The "Submit" button is not disabled while the API call is in flight.
A slow connection combined with an impatient user can create duplicate payout requests.

---

#### B-08 · Webhook `dispatchEvent` swallows subscription query errors silently — Low, Small effort
`WebhookService.java:89`:
```java
} catch (Exception ex) {
    log.warn("webhook: subscription query failed...");
    return;
}
```
If the DB is momentarily unavailable the webhook fires nothing and logs a warning
that nobody is watching. Events are permanently lost with no dead-letter queue.

---

#### B-09 · Provider discount balance check is not atomic under concurrency — High, Small effort
Two simultaneous POST requests to `/portal/discounts` can both pass the balance check
before either commits, resulting in negative available balance.  
See also **BP-10** for the code reference and fix approach.

---

#### B-10 · Earnings CSV export has English column headers regardless of UI language — Medium, Small effort
`PortalService.buildEarningsCsv()` hard-codes:
```java
"Service,Bookings,Gross Revenue,Platform Fee,Your Net\n"
```
An Arabic-locale provider downloads a CSV with English headers.

---

#### B-11 · Empty date-range export returns a blank file with no user feedback — Medium, Small effort
When the requested period has no completed payments, the CSV/Excel/PDF export
returns an empty file. The frontend triggers a download of a 0-byte file with no
toast, error, or explanation.

---

#### B-12 · Earnings breakdown shows "no payments" even when bookings exist — Low, Small effort
The LEFT JOIN is correct, but `perServiceBreakdown` being empty causes the frontend
to render "No completed payments in this period." even when the provider has
confirmed bookings — just none that have been paid yet. Misleading message.

---

## 5. Recommendations

### 5.1 Visual Design

#### R-VD-01 · Centralise status badge colours into a shared `<StatusBadge>` component — High, Small effort
Currently 7+ pages define their own status colour mapping inline.

```tsx
// Proposed: src/components/StatusBadge.tsx
const BADGE: Record<string, string> = {
  ACTIVE:           'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  PENDING_APPROVAL: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200',
  INACTIVE:         'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300',
  SUSPENDED:        'bg-red-100   text-red-800   dark:bg-red-900   dark:text-red-200',
  FAILED:           'bg-red-100   text-red-800',
  DELIVERED:        'bg-green-100 text-green-800',
  PENDING:          'bg-yellow-100 text-yellow-800',
}
```

---

#### R-VD-02 · Consolidate landing inline styles into `landing-public.css` utility classes — Medium, Medium effort
Create utility classes for the ~8 recurring inline style patterns in the landing app:
```css
/* landing-public.css */
.lp-skeleton   { background: rgba(90,100,110,0.1); border-radius: 12px; }
.lp-card-outline { border: 1px solid rgba(61,112,128,0.3); }
```
This reduces the 244 inline style blocks to near zero.

---

#### R-VD-03 · Add responsive typography scale for KPI figures — Low, Small effort
`PortalEarningsPage.tsx:226` uses `clamp(1.75rem, 5vw, 2.75rem)` inline.
Move it to a Tailwind custom class in `tailwind.config`:
```js
theme: { extend: { fontSize: { kpi: ['clamp(1.75rem,5vw,2.75rem)', { lineHeight: '1' }] } } }
```

---

### 5.2 Functionality

#### R-F-01 · Add "unsaved changes" guard to `PortalListingFormPage` — High, Small effort
Use React Router's `useBlocker`:
```tsx
const blocker = useBlocker(isDirty)
// render ConfirmDialog when blocker.state === 'blocked'
```
`isDirty` = any field differs from its last-saved value.

---

#### R-F-02 · Replace `alert()` / `window.confirm()` with a global Toast + `ConfirmDialog` system — High, Small effort
The `ConfirmDialog` component already exists. Add a `useToast()` hook backed by a
portal-rendered container and replace every native dialog call.

---

#### R-F-03 · Add "Copy to clipboard" button to webhook secret display — High, Small effort
```tsx
<button onClick={() => navigator.clipboard.writeText(secret)}>
  {copied ? '✓ Copied' : 'Copy'}
</button>
```
The secret is shown exactly once and can never be retrieved again.

---

#### R-F-04 · Render payout request history and status in the portal — High, Small effort
`portalAPI.listPayouts()` exists and is already called in `PortalEarningsPage`.
The response data is simply never rendered. Add a status table below the request form:
`Requested $500 · 2026-06-01 · ⏳ PENDING`.

---

#### R-F-05 · Show "pending approval" badge on newly created discounts — High, Small effort
After a provider creates a discount it enters `PENDING_APPROVAL`. Currently it renders
identically to `ACTIVE`. Add an amber badge and a tooltip:
> "Awaiting admin approval before customers can use this code."

---

#### R-F-06 · Add rate limits to financial endpoints — High, Medium effort
Minimum additions to `PortalController`:
```java
@RateLimit(key = "POST:/portal/payout-request", maxPerMinute = 3)
@RateLimit(key = "POST:/portal/discounts",       maxPerMinute = 10)
```

---

#### R-F-07 · Paginate portal discounts and payout history — High, Small effort
Backend: add `page`/`size` params to `listProviderDiscounts()` and `listPayoutRequests()`,
return a paged envelope with `totalCount`.  
Frontend: add a "Load more" button or simple page selector.

---

#### R-F-08 · Add a dead-letter / retry mechanism for failed webhook deliveries — Medium, Large effort
Add a `@Scheduled` job that queries:
```sql
SELECT * FROM webhook_deliveries
WHERE status = 'FAILED' AND attempt_count < 3
ORDER BY last_attempt_at ASC
LIMIT 50
```
and re-dispatches with exponential back-off (attempt 1 → 5 min, attempt 2 → 30 min,
attempt 3 → 2 h). Mark as `PERMANENTLY_FAILED` after the third attempt.

---

### 5.3 User Experience

#### R-UX-01 · Add session-expiry warning toast — High, Small effort
30 seconds before the refresh token expires, show:
> "Your session will expire in 30 s. [Stay logged in]"
Currently the user is silently redirected to `/login` mid-task with no warning.

---

#### R-UX-02 · Persist filter/sort state in URL query params — Medium, Small effort
`ProvidersPage`, `BookingsPage`, `DiscountsPage` all lose their filter state on refresh.
Encode state in the URL: `/management/providers?status=ACTIVE&page=2`.
Enables sharing filtered views and correct back-button behaviour.

---

#### R-UX-03 · Show inline rejection reason on portal listing cards — High, Small effort
When a listing is `status=REJECTED`, display the admin's rejection note directly on the
listing card. Currently the provider sees "Rejected" with no actionable feedback.

---

#### R-UX-04 · Add a prominent "+ New listing" CTA to `PortalListingsPage` — High, Small effort
There is no create button on the listings page. The only path to `/portal/listings/new`
is knowing the URL or finding a hidden link. Add a primary `+ New listing` button in the
page header — standard SaaS pattern.

---

#### R-UX-05 · Show available balance inside the payout request modal — Medium, Small effort
Duplicate the available-balance figure inside the modal so the provider knows the cap
without closing it and re-reading the dashboard card above.

---

#### R-UX-06 · Add a totals footer row to the per-service earnings table — Low, Small effort
```tsx
<tfoot>
  <tr>
    <td>Total</td>
    <td>{totalBookings}</td>
    <td>{fmt(grossRevenue)}</td>
    <td>{fmt(platformFee)}</td>
    <td>{fmt(providerNet)}</td>
  </tr>
</tfoot>
```

---

#### R-UX-07 · Include date range in the CSV export filename — Low, Small effort
```java
String filename = String.format("earnings-%s-to-%s.csv",
    start != null ? start : "all",
    end   != null ? end   : "now");
response.setHeader("Content-Disposition",
    "attachment; filename=\"" + filename + "\"");
```

---

### 5.4 Performance & Accessibility

#### R-PA-01 · Add `aria-live="polite"` to all error/success message containers — High, Small effort
```tsx
<div role="status" aria-live="polite">
  {error && <p className="text-red-600">{error}</p>}
</div>
```

---

#### R-PA-02 · Add icon + text alternative to all status badges — High, Small effort
```tsx
<StatusBadge status="ACTIVE">
  <span aria-hidden="true">●</span>
  <span>Active</span>
</StatusBadge>
```
Both sighted and screen-reader users now receive the meaning.

---

#### R-PA-03 · Add accessible label to the Modal close button — High, Small effort
```tsx
// Modal.tsx — current
<button onClick={onClose}>×</button>

// Fix
<button onClick={onClose} aria-label={t('common.closeModal')}>
  <span aria-hidden="true">×</span>
</button>
```

---

#### R-PA-04 · Fix `useEffect` dependency arrays and enforce via ESLint — High, Small effort
`PortalEarningsPage.tsx:155` and `ProvidersPage.tsx:72`.
Enable the rule in CI so future violations are caught before they ship:
```json
"rules": { "react-hooks/exhaustive-deps": "error" }
```

---

#### R-PA-05 · Add `role="progressbar"` to `BalanceMeter` — Medium, Small effort
```tsx
<div
  role="progressbar"
  aria-valuenow={spent}
  aria-valuemin={0}
  aria-valuemax={allocated}
  aria-label={t('portalPages.discountBalanceMeter')}
  style={{ width: `${pct}%` }}
/>
```

---

#### R-PA-06 · Add per-route `<title>` updates — Medium, Small effort
All three surfaces show only the app name. Route changes never update `document.title`,
which hurts browser history readability and screen-reader announcements.
```tsx
useEffect(() => {
  document.title = `${t('title.earnings')} — Ziyara`
}, [])
```

---

#### R-PA-07 · Add skip-to-main-content link — Medium, Small effort
```tsx
// First child of MainLayout / LandingShell
<a
  href="#main-content"
  className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 z-50 bg-primary text-white px-4 py-2 rounded"
>
  Skip to main content
</a>
<main id="main-content">
```

---

#### R-PA-08 · Complete the deployment pipeline — Critical, Large effort
`.github/workflows/deploy.yml` has a `# TODO` placeholder. Without a real deploy
step the CI/CD claim is cosmetic. Minimum viable addition:
`docker compose push` + SSH deploy or a managed platform hook (Render, Railway, etc.).

---

#### R-PA-09 · Route `console.error` in `ErrorBoundary.tsx` to a structured logger — Low, Small effort
```tsx
import * as Sentry from '@sentry/react'
// In componentDidCatch:
Sentry.captureException(error, { extra: { componentStack: info.componentStack } })
```

---

## 6. Executive Summary

**Overall verdict: Solid foundation, several sharp edges.**

The Ziyara platform has a well-structured codebase with Clean Architecture enforcement,
a clear multi-surface frontend strategy, comprehensive API coverage, and working
authentication with RBAC. The core reservation flows function end-to-end.

However, the issues discovered during this review would meaningfully hurt the
experience of real users and operators in production:

| Severity | Count | Top examples |
|---|---|---|
| **Critical** | 2 | No deployment pipeline; no session-expiry warning |
| **High** | 22 | Missing "+ New listing" CTA; double-submit on payouts; no unsaved-changes guard; English error strings in Arabic UI; broken `useEffect` deps; payout status invisible to provider |
| **Medium** | 19 | Native alert/confirm dialogs; no pagination on discounts/payouts; accessibility gaps; UX copy confusion |
| **Low** | 14 | Console leaks; inline style accumulation; missing CSV filename; totals row |

**Three things working well:**
1. Clean Architecture with ArchUnit enforcement keeps backend layer boundaries honest.
2. The i18n system is correctly set up; the discipline issues are isolated misses, not systemic rot.
3. Auth refresh-token logic is correctly implemented with queued request replay on 401.

**Three things to fix immediately before any public launch:**
1. **The deployment pipeline (BP-14)** — without it, "CI/CD" is theatre.
2. **The provider portal UX black holes** — no payout status, no rejection reason, no "+ New listing" button (M-03, M-11, M-12, UX-03, UX-04).
3. **The concurrent balance debit race condition (B-09 / BP-10)** — exploitable from the browser with two rapid clicks.

---

## 7. Next Steps

### Sprint 1 — "Stop the bleeding" (1–2 days, 1 dev)
- [ ] Fix `useEffect` dependency arrays (`PortalEarningsPage`, `ProvidersPage`) — **BP-04 / R-PA-04**
- [ ] Replace `alert()` and `window.confirm()` with Toast + ConfirmDialog — **BP-03, BP-02 / R-F-02**
- [ ] Fix English validation strings in `PortalDiscountsPage` — **BP-05**
- [ ] Add "Copy secret" button to webhook creation modal — **R-F-03 / UX-09**
- [ ] Add `aria-label` to Modal close button — **R-PA-03**
- [ ] Add `+ New Listing` CTA on `PortalListingsPage` — **R-UX-04 / M-03**

### Sprint 2 — "Provider portal trust" (3–5 days, 1–2 devs)
- [ ] Render payout request history & status in portal — **M-11 / R-F-04**
- [ ] Show rejection reason on listing card — **M-12 / R-UX-03**
- [ ] Show "pending approval" badge on new discounts — **M-10 / R-F-05**
- [ ] Add unsaved-changes navigation guard to listing form — **S-03 / R-F-01**
- [ ] Fix concurrent discount balance race condition — **B-09 / BP-10**
- [ ] Show available balance inside payout request modal — **R-UX-05**
- [ ] Disable submit button on payout modal while in-flight — **B-07**

### Sprint 3 — "Polish & accessibility" (3–5 days, 1 dev)
- [ ] Create shared `<StatusBadge>` component — **R-VD-01**
- [ ] Add `aria-live` to all error/success containers — **R-PA-01**
- [ ] Add icon + text to all status badges — **R-PA-02**
- [ ] Add `role="progressbar"` to `BalanceMeter` — **R-PA-05**
- [ ] Wire `react-hooks/exhaustive-deps` as a CI error — **R-PA-04**
- [ ] Add per-route `document.title` updates — **R-PA-06**
- [ ] Persist filter state in URL params — **R-UX-02**
- [ ] Paginate portal discounts and payout history — **R-F-07**
- [ ] Include date range in CSV export filename — **R-UX-07 / B-10**

### Sprint 4 — "Infrastructure" (1 week, DevOps + 1 dev)
- [ ] Implement real deployment step in CI — **R-PA-08 / BP-14**
- [ ] Complete Android signing config — **BP-15**
- [ ] Add rate limits to `/portal/payout-request` and `/portal/discounts` — **R-F-06 / M-15**
- [ ] Add webhook delivery retry scheduler — **R-F-08 / B-08**
- [ ] Add session-expiry warning toast — **R-UX-01 / M-17**

### Backlog (low priority / future cycles)
- [ ] Migrate landing page inline styles to CSS utility classes — **BP-01 / R-VD-02**
- [ ] Replace custom rate-limiting AOP with Bucket4j — **BP-08**
- [ ] Add skip-to-main-content link — **R-PA-07**
- [ ] Fetch supported webhook event types from backend — **M-08-adjacent**
- [ ] Replace `console.error` in ErrorBoundary with structured logger — **BP-11 / R-PA-09**
- [ ] Add provider KPI trend indicators (delta vs. last period) — **UX-14**
- [ ] Add per-service earnings totals footer row — **R-UX-06 / UX-15**

---

*This report reflects the codebase state as of 2026-06-02.
Re-score after Sprint 2 to validate the provider-portal trust items.*
