# Ziyara Dashboards — Critic Report
**Company Dashboard (Port 7050) · Provider Portal (Port 7060)**
**Date reviewed:** 2026-05-31
**Reviewed by:** External critic — first-time admin & provider-partner perspective

---

## Executive Summary

Both dashboards share a coherent, professional dark-navy design system and a well-thought-out role-based architecture. The visual foundation is solid: the primary color (`#1e4d6b`), gold accent (`#ac9e78`), glassmorphism cards, and smooth sidebar transitions create a polished first impression.

But beneath that polish, both surfaces reveal the same patterns: UUID-based inputs in place of searchable dropdowns, raw database enum strings shown directly to users, missing confirmations on destructive actions, page-level state bugs (summary cards computed from the current page only), and dozens of hardcoded English strings that bypass the i18n system entirely.

The company dashboard feels like a complete admin suite that was rushed to 80% and left there. The provider portal is more focused and cleaner, but suffers from read-only pages that should have actions, missing payout history, and a staff-linking workflow so poor that most real partners will not be able to use it.

---

## Overall Ratings

| Category | Company (7050) | Provider (7060) | Notes |
|---|---|---|---|
| Visual Design | **8.0 / 10** | **8.0 / 10** | Consistent, premium system |
| Navigation & IA | **5.5 / 10** | **7.0 / 10** | Company sidebar overcrowded; portal is clean |
| Workflow Clarity | **4.5 / 10** | **5.5 / 10** | UUID inputs, raw enums, missing confirmations |
| Feature Completeness | **5.0 / 10** | **4.5 / 10** | Many half-built pages |
| i18n Coverage | **5.0 / 10** | **3.5 / 10** | Dozens of hardcoded English strings |
| Data Display | **5.5 / 10** | **6.0 / 10** | Enums raw, no charts, misleading summaries |
| Mobile Experience | **3.0 / 10** | **4.0 / 10** | Desktop-only; tables overflow |
| Error Handling | **5.5 / 10** | **6.0 / 10** | Generic banners, no retry, no field hints |
| **Overall** | **5.3 / 10** | **5.6 / 10** | Strong bones, unfinished interior |

---

## Company Dashboard (Port 7050)

---

### 1. Layout & Navigation

**What works:**
- Collapsible sidebar with smooth 300ms transition is genuinely satisfying.
- RTL flip (`pl-60` ↔ `pr-60`) for Arabic is architecturally correct.
- The ambient background glows in the main content area add depth without being distracting.
- Active nav item highlighting via `NavLink` is clear.

**Issues:**

**[CRITICAL] No mobile layout at all.**
The sidebar is a fixed-width 240px panel (`pl-60` content offset). On a tablet or phone there is no hamburger, no collapse, no overlay. The content is partially hidden behind the sidebar on small screens. A manager checking bookings from a phone gets a broken layout.

**[HIGH] Sidebar has 5 services entries taking up permanent space.**
Hotels, Resorts, Restaurants, Taxis, and Trips each occupy their own sidebar slot under "SERVICES" for every role that can see them. This is 5 navigation entries for what is conceptually one section ("Manage Services"). A grouped accordion — or a single "Services" entry that expands — would reduce sidebar height by ~40%.

**[HIGH] No unread/pending badges on sidebar items.**
There is no count badge on "Complaints" showing open tickets, no badge on "Media Approvals" showing pending submissions, no badge on "Reviews" showing items awaiting moderation. An admin has no way to know at a glance what needs attention without clicking every section individually.

**[MEDIUM] No quick-jump or command palette.**
A platform with 15+ admin sections and potentially thousands of records provides no keyboard shortcut or command palette (Ctrl+K equivalent) for jumping to a specific booking, provider, or page. The header has a global search component, but it is not keyboard-triggered from anywhere visible.

**[LOW] DashboardFooter exists but adds no value.**
The footer renders at the bottom of every page with the Ziyara copyright. On a dense admin dashboard it consumes vertical space that could be used for data.

---

### 2. Dashboard / Home Page

**What works:**
- Animated stat card skeletons on load are a professional touch.
- The live WebSocket connection indicator ("Live" badge on Service Health) is a nice detail.
- The 45-second polling with `document.visibilityState` guard is thoughtful.
- Commission analysis + payout summary in the lower cards give a meaningful 30-day snapshot.

**Issues:**

**[HIGH] Date range is hardcoded to last 30 days with no picker.**
The dashboard calculates `defaultDateRange()` — always the last 30 days — and the user cannot change it. An admin who wants to see last week, last quarter, or year-to-date has no option. The `start`/`end` variables are in the code but there is no date range UI input.

**[HIGH] Activity feed has no "View all" link.**
The feed shows 15 items. There is no link to an expanded activity log or the audit logs page. The feed just stops.

**[MEDIUM] Commission analysis shows only two line items.**
"Base collected" and "Ziyarah delta (commission)" — two numbers. No breakdown by provider, no trend line, no comparison to previous period. For a finance dashboard this is the minimum viable display.

**[MEDIUM] Payout summary slices at 5 entries with no "View all".**
The top 5 provider payouts are shown, then nothing. There is a "View payments" link at the bottom but it links to the full payments ledger, not a filtered payout view.

**[LOW] Role-based welcome text is hardcoded per role.**
`roleBlurb_super_admin`, `roleBlurb_finance`, etc. are fine for differentiation, but the intro paragraph never updates based on what's actually happening (e.g., "You have 3 open complaints" or "2 providers pending approval"). It's static text that ignores real system state.

---

### 3. Providers Page

**Issues:**

**[CRITICAL] Actions column is a wall of pipe-separated text links.**
Each provider row renders up to 7 inline text links separated by `|` pipe characters: `View / edit | Edit profit | Approve | Reject | Suspend | Reset pwd | Delete`. On a 1080p monitor this overflows. There is no visual hierarchy between primary and destructive actions. "Delete" (red) sits immediately next to "Reset pwd" (amber) with only a `|` separator — one misclick deletes a provider.

**[HIGH] Status displayed as raw database enum.**
The status column shows `PENDING_APPROVAL`, `ACTIVE`, `SUSPENDED`, `INACTIVE` — raw enum strings directly from the database. Users see `PENDING_APPROVAL` instead of "Pending Approval". Every status column in every table on the dashboard has this same issue.

**[HIGH] No search or filter by name/email.**
With 20 providers per page and potentially hundreds in the system, there is no search input. An admin looking for "Beirut Taxi Co." must click through pages sequentially. The filter pills are for status only.

**[HIGH] Inline commission editing in the table row is poor UX.**
Clicking "Edit profit" replaces the cell in that row with an input field and Save/Cancel buttons. This is functional but fragile — it stores `editingId` state at the page level, so navigating away or re-rendering loses the edit silently. A modal would be safer.

**[MEDIUM] No sort on any column.**
The table has no sortable headers. Providers are returned in backend order with no way to sort by name, status, subscription tier, or commission rate from the UI.

**[MEDIUM] Subscription plan badge inconsistent.**
The subscription plan badge shows `p.subscriptionPlan === 'PRO'` in primary color but `p.subscriptionPlan === 'PROFESSIONAL'` would not match this condition if the backend uses the full word. The badge logic is fragile string comparison.

**[LOW] `window.confirm()` for delete, but no confirmation at all for suspend.**
Delete pops a browser confirm dialog (which is jarring and non-styled). Suspend (`handleSuspend`) executes immediately with no confirmation — suspending an active provider's portal access requires zero additional input.

---

### 4. Bookings Page

**Issues:**

**[HIGH] Provider filter uses a raw text input for provider ID.**
The filter row has a `providerIdFilter` text input where the user must type a provider's UUID. No admin knows their providers' UUIDs. This should be a searchable dropdown populated from the providers API.

**[HIGH] Booking detail modal loads synchronously then is lost on re-render.**
When a booking ID is in the URL query param (`?bookingId=...`), the detail opens and then the URL is cleaned. If the admin refreshes the page the detail is gone and the URL gives no way to restore it.

**[HIGH] Cancel booking inside detail modal asks for a reason field but shows no confirmation.**
The `cancelReason` input collects a reason, but submitting "Cancel Booking" in the modal executes immediately with no second-confirmation step. Cancelling a confirmed booking with a customer is irreversible.

**[MEDIUM] Status in filter pills is fine, but status display in table rows is raw enum.**
`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED` — raw strings. Same issue as everywhere else.

**[MEDIUM] Bulk cancel has no partial-failure handling.**
`bulkCancel` calls `Promise.allSettled` on all selected IDs but silently ignores failures. If 3 of 5 selected bookings fail to cancel, the user sees no error — the table just re-renders with mixed states.

**[LOW] "Voucher" button in the detail modal has no spinner during download.**
The voucher download button shows `t('ui.viewVoucher')` regardless of loading state. If the download takes time, there is no feedback.

---

### 5. Payments Page

**[CRITICAL] Summary cards are computed from the current page of 20 records only.**
The "Total Collected", "Pending", "Refunded" summary cards are calculated via `useMemo` over the `payments` state — which holds only the current 20-item page. This means the summary shows "USD 3,400 collected" when the page happens to have 20 completed payments, but the true platform total could be 10× higher. This is actively misleading financial information.

```tsx
// payments = only current page (PAGE_SIZE = 20)
const completed = payments.filter(p => p.status === 'COMPLETED')
const sum = (arr) => arr.reduce((s, p) => s + Number(p.amount ?? 0), 0)
```

**[HIGH] Refund modal requires a reason but has no minimum length validation.**
The submit button is disabled when `refundReason.trim()` is empty, but a single space is enough to enable it. More critically, there is no second confirmation step ("Refund USD 450 to booking #ZYR-001?"). Refunds are financial operations — they need a double-confirmation.

**[HIGH] No search by booking reference or customer name.**
Finding a specific payment requires paging through 20-per-page results. There is no search input anywhere on the page.

**[MEDIUM] Payment method column shows raw gateway strings.**
Methods like `CARD`, `STRIPE`, `CASH` — raw strings with no formatting or icon.

**[MEDIUM] 3DS column shows `t('ui.yes')` / `t('ui.no')` but the column header is just "3DS".**
Non-finance users will not know what "3DS" means. The label should be "3D Secure" or have a tooltip.

---

### 6. Discounts Page

**What works:**
- The cascading provider → listing dropdown (select a provider, then listings filter to that provider) is well-implemented.
- The sponsor split options (Company / Provider / Both 50/50) are clearly labeled.
- Status approval workflow (pending → approved) is architecturally good.

**Issues:**

**[HIGH] UUID inputs for room types, menu sections, and menu items are unusable.**
Three separate textarea fields ask admins to type comma-separated UUIDs for `applicableRoomTypeIds`, `applicableMenuSectionIds`, and `applicableMenuItemIds`. Nobody operating a dashboard types UUIDs manually. These need to be multi-select dropdowns populated from the API once a service is selected.

**[HIGH] No discount preview or test tool.**
Once a discount code is created and approved, there is no way to simulate what the final price would be for a given booking. Approving a discount is a blind action.

**[MEDIUM] "End date" is required but "Start date" is optional, creating a confusing asymmetry.**
A code with no start date activates immediately. If a manager creates a future-dated campaign and forgets to set the start date, it goes live immediately on approval.

**[MEDIUM] Scope summary column uses `translate('discountsPage.scopeListings').replace('{{count}}', String(nList))`.**
This uses `.replace('{{count}}', ...)` instead of the i18n `t()` parameter substitution, which means if the locale changes to Arabic the replacement pattern may not match.

**[LOW] `toLocalDateTime()` converts ISO dates to `YYYY-MM-DDTHH:mm` for the datetime-local input.**
This silently drops seconds and timezone. If the backend stores timezone-aware timestamps, the conversion to local time can cause the displayed date to be one day off for users in UTC+ timezones.

---

### 7. Reports Page

**[HIGH] Revenue and booking reports show raw totals — no chart or visualization.**
The "Revenue Report" tab returns a `totalRevenue` number and a `byDay` array, but there is no chart. The `byDay` data is fetched and stored but never rendered. The user sees one large number and nothing else.

**[HIGH] "Analytics" tab appears in code as a tab option but renders no content.**
The three tabs are `revenue | bookings | analytics`. The analytics tab exists as a UI option but its content section appears empty in the code — it references no report-specific UI.

**[MEDIUM] Customer search for CUSTOMER scope requires two steps: type → click Search → select from results.**
The flow is: enter email/phone/name → click "Search" button → results table appears → click "Select" next to a user. This is 4 interactions to filter a report. A single autocomplete input would reduce this to 1.

**[MEDIUM] Excel/PDF export buttons have no loading state.**
Clicking "Export Excel" or "Export PDF" dispatches the download. If the server is slow there is no spinner or disabled state — the user cannot tell if the click registered.

---

### 8. Complaints Page

**[CRITICAL] Assign agent uses a raw UUID text input.**
```tsx
<input value={assignAgentId} onChange={(e) => setAssignAgentId(e.target.value)} />
```
An admin must type the exact UUID of a staff agent to assign a complaint. This is functionally broken for any real user — UUIDs are 36-character random strings. It must be a searchable dropdown of support agents.

**[CRITICAL] Escalate also uses a raw UUID input.**
Same problem as assignment: `escalateToUserId` is a free-text UUID input. Escalation requires knowing the exact ID of the escalation target.

**[HIGH] No complaint creation path from the admin side.**
Complaints can only come in from customers. If a support agent wants to log an internal complaint or note, there is no form. The page is purely a consumer of externally-submitted complaints.

**[HIGH] No priority filter.**
Complaints have a `priority` field (LOW, MEDIUM, HIGH, URGENT per the translation keys) but the filter pills only show status. There is no way to view only URGENT open complaints.

**[MEDIUM] Detail modal renders all comments in a simple list with no threading.**
Internal comments and customer comments appear in the same chronological list with only a badge differentiator. On a busy complaint with 20+ comments, the conversation is hard to follow.

**[MEDIUM] Complaint list has no pagination.**
`complaintsAPI.list()` returns results with no page parameter. If there are 300 open complaints, all 300 load into the page at once.

---

### 9. Reviews Page

**[HIGH] Publish / Reject / Hide execute on first click with zero confirmation.**
```tsx
<button onClick={() => handlePublish(review.id)}>Publish</button>
<button onClick={() => handleReject(review.id)}>Reject</button>
<button onClick={() => handleHide(review.id)}>Hide</button>
```
Three action buttons in the same table row, any of which changes the published state of a customer review immediately. One misclick publishes a review that should have been rejected, or hides a legitimate review. These need at minimum a confirmation tooltip or dialog.

**[HIGH] Truncated comments with no expand.**
Long reviews are truncated in the table. There is no way to read the full text without a detail view — and there is no detail view. The table `comment` column truncates at cell width.

**[MEDIUM] Status dropdown shows all statuses but only PENDING items need moderation.**
A moderator working through the pending queue switches back to "All" or specific status filters, but the workflow does not auto-advance: after publishing a review, the page stays on the current filtered list, forcing a manual refresh to see the next pending item.

---

### 10. Media Submissions Page

**[HIGH] Status badges hardcode English strings outside the i18n system.**
```tsx
function statusBadge(status: string) {
  if (status === 'APPROVED') return <span className="badge badge-success">Approved</span>
  if (status === 'REJECTED') return <span className="badge badge-danger">Rejected</span>
  return <span className="badge badge-warning">Pending</span>
}
```
"Approved", "Rejected", "Pending" — all hardcoded. When the user switches to Arabic, these stay in English.

**[HIGH] No image preview modal.**
The submissions table shows a small inline thumbnail (`<img>` in the table cell). There is no way to open the image full-size. For approving a logo or service photo, the approver is making a decision based on a 40×40 pixel thumbnail.

**[HIGH] No filter by status or provider.**
The page loads all submissions at once with no way to filter to "Pending only" or "by provider." A platform with 50 providers uploading media produces an unsorted list.

**[MEDIUM] No bulk approve.**
Each submission must be approved one at a time. With 30 pending images from a media upload session, the admin clicks Approve 30 times.

**[MEDIUM] Reject note is an inline textarea that appears below the row.**
When rejecting, a note textarea renders inline below the table row. This breaks the table layout on narrow screens and is visually jarring.

---

### 11. Audit Logs Page

**[HIGH] Hard limit of 50 entries with no pagination.**
```tsx
auditLogsAPI.getRecent({ limit: 50, search: search || undefined })
```
The audit log fetches 50 entries and stops. There is no way to load more, no pagination, no date range filter. For compliance purposes this is inadequate.

**[HIGH] Timestamps displayed as raw ISO strings.**
`log.createdAt` is rendered directly: `2026-05-31T10:23:45.123456Z`. No date formatting, no relative time ("3 minutes ago"), no locale-aware display. A column of 50 ISO timestamps is difficult to scan.

**[HIGH] Old/New value column shows raw JSON.**
The `colOldNew` column displays raw JSON diff strings from the backend. An audit entry for a price change reads `{"basePrice":150}` → `{"basePrice":175}`. A human-readable diff ("Base price: $150 → $175") would be far more useful.

**[MEDIUM] Search only searches within the 50 loaded records, not the full database.**
The search input passes `search` to the API, which is correct — but the UX implies you're searching "all audit logs" when you may only be searching the most recent 50. This is unclear.

---

### 12. Settings Page

**[MEDIUM] Only 3 settings exist for an entire platform configuration.**
- Company display name
- Default currency (ISO code)
- Maintenance mode checkbox

There are no settings for: email notification templates, booking confirmation text, refund policy, cancellation policy, supported payment methods, platform fee structure, or any operational configuration. The settings page exists but provides almost no value.

---

### 13. Users / Groups Page

**[HIGH] Creating a user requires knowing the correct role code.**
The "Create User" modal has a role dropdown populated from backend roles, but the developer comment (`roleGroupBuiltIn`, `roleGroupCustom`) implies the dropdown groups are hand-curated. If a new custom role is added to the system, the dropdown may not show it correctly.

**[HIGH] RBAC assignment requires typing a staff email — no staff directory visible.**
The HR variant of the Users page allows "Assign RBAC sidebar role" via a staff email text input and a role dropdown. An HR manager doesn't necessarily know every staff member's email address. There should be a searchable staff directory instead.

**[MEDIUM] Group cards show `roleCount` and `userCount` but no quick list of who's in the group.**
To see members, you click "View members" which opens a paginated modal. For a group of 3 people, a modal table is overkill — an inline expansion would be cleaner.

---

## Provider Portal (Port 7060)

---

### 1. Overview (Dashboard)

**What works:**
- 4 KPI cards with skeletons is professional.
- Weekly revenue Recharts bar chart is the right choice.
- Quick action links at the bottom are helpful.

**Issues:**

**[HIGH] Weekly chart only renders if `weeklyRevenue` data exists.**
If the provider has no completed bookings, the chart section renders nothing — no empty state, no "No data yet" message, no placeholder. The page silently skips the chart section. A provider onboarding their first week sees a dashboard with 4 zero-stat cards and 3 nav links. It looks broken.

**[HIGH] No pending-action alerts anywhere.**
If a provider has 3 bookings that just came in and need review, there is no notification, no badge, no alert on the overview. The provider must navigate to Bookings manually and discover them.

**[MEDIUM] Revenue KPI always shows "Revenue (completed payments)" trend label.**
The trend label is the same regardless of value — even if revenue is 0, the trend says `revenueLabel` ("From completed payments") with `trendPositive: true`. A positive-colored trend on 0 revenue is misleading.

---

### 2. Listings Page

**What works:**
- Status badges (ACTIVE green, PENDING_APPROVAL amber, INACTIVE/REJECTED red) are clearly color-coded.
- Delete confirmation dialog (`window.confirm`) prevents accidental deletion.
- Pagination with prev/next works correctly.

**Issues:**

**[HIGH] Service status dropdown in listing form exposes 9 internal statuses to providers.**
```tsx
const SERVICE_STATUSES: ServiceStatusDto[] = [
  'ACTIVE', 'INACTIVE', 'PENDING_APPROVAL', 'AVAILABLE', 'UNAVAILABLE',
  'MAINTENANCE', 'HIDDEN', 'SUSPENDED', 'DISCONTINUED',
]
```
A provider should only be able to set `ACTIVE`, `INACTIVE`, or request approval. `SUSPENDED`, `DISCONTINUED`, `HIDDEN` are administrative statuses that should only be set by the company. Showing all 9 to a provider is confusing and potentially allows them to self-suspend or mark themselves as discontinued.

**[HIGH] No image upload in the listing form.**
The listing form has no image upload. Providers must create the listing first, then navigate to the separate "Media" page and submit images for approval. The form has no indication of this two-step requirement. A new provider completing the listing form believes it is complete — but their listing has no photos.

**[HIGH] Currency field is locked after creation but still renders as an editable input.**
The form renders `<input type="text" value={currency}>` with a hint saying currency is locked. But visually the field looks fully editable. A provider will type in it, see their input accepted visually, submit the form, and be confused when the currency doesn't change.

**[MEDIUM] "Star Rating" field appears for hotels/resorts with no validation.**
A provider can enter any decimal (e.g., `9.5`) for their own star rating. There is field-level validation in the schema but no visual hint of valid range (1.0–5.0) until submission fails.

**[LOW] Listing status displayed as raw enum in the table.**
Same issue as company dashboard — `PENDING_APPROVAL`, `ACTIVE`, `INACTIVE`, `REJECTED` shown directly.

---

### 3. Bookings Page (Provider)

**[HIGH] Completely read-only with no actions — even view-detail is absent.**
The provider bookings table shows: reference, service name, check-in, check-out, amount, status. There are no action buttons at all — no "View", no "Contact customer", no "Confirm", nothing. A provider cannot click into a booking to see customer details, special requests, or any other context. This is below minimum viable functionality for a booking management page.

**[HIGH] No search or filter of any kind.**
A provider with 200 historical bookings and no way to filter by status, date range, or service listing must scroll through all of them. This is unusable at scale.

**[MEDIUM] No pagination visible in the code read.**
It is unclear if the bookings page paginates. If it returns all bookings at once, this is a performance and UX problem.

---

### 4. Earnings Page

**[CRITICAL] All UI strings hardcoded in English — the most translation-incomplete page on both surfaces.**
The entire `PayoutRequestModal` and surrounding UI uses hardcoded English:
- Modal title: `"Request Payout"` — not `t()`
- Cancel button: `"Cancel"` — not `t()`
- Submit button: `"Submit Request"` / `"Submitting…"` — not `t()`
- Close button after success: `"Close"` — not `t()`
- Validation error: `"Enter a valid amount"` — not `t()`
- Max-amount error: `` `Amount cannot exceed ${currency} ${maxAmount.toLocaleString()}` `` — not `t()`
- Success message: `"Payout request submitted. Our team will process it within 3–5 business days."` — not `t()`
- Form label: `` `Amount (${currency})` `` — not `t()`
- Hint text: `"Bank account, reference, or any note for our team"` — not `t()`
- Button on earnings card: `"Request Payout"` — not `t()`

An Arabic-speaking provider interacts with a fully English payout flow embedded in an otherwise Arabic UI.

**[HIGH] No payout history.**
After submitting a payout request, the provider has no way to see their submitted requests, their status (pending / processed / rejected), or the amounts. The `PayoutRequestModal` shows a success message and closes — that is the only feedback. Previous requests are invisible.

**[HIGH] Payout max = total earnings with no deduction for fees or previous payouts.**
```tsx
maxAmount={totalEarnings}
```
The maximum payout amount equals `totalEarnings` for the selected period. This ignores platform commission, previous partial payouts, or pending requests. A provider could theoretically request their full gross earnings including the platform's margin.

**[MEDIUM] "Apply Range" button must be clicked — date change does not auto-refresh.**
The date inputs do not trigger a reload on change. The provider changes the dates and must remember to click "Apply range". If they change dates and forget, they see stale data.

---

### 5. Staff Page

**[HIGH] "Link existing user" requires typing a UUID.**
```tsx
<input value={addUserId} placeholder={t('portalStaffPage.userUuidPlaceholder')} />
```
The placeholder is literally "User UUID". A real provider manager at a restaurant who wants to give their chef access to the portal must know the chef's exact UUID. This is functionally impossible for a non-technical user.

**[HIGH] Creating a new staff user requires choosing from hardcoded role codes.**
The new-user form has a role dropdown with values: `PROVIDER_STAFF`, `PROVIDER_FINANCE`, `TAXI_OPERATOR`, `PROVIDER_MANAGER`. These are hardcoded in the frontend, not fetched from the roles API. If the backend introduces a new provider role, the portal form is stale.

**[MEDIUM] No indication of what each role can do.**
The role dropdown shows `Provider Staff`, `Provider Finance`, etc. with no description of what access each grants. A provider manager choosing between "Provider Finance" and "Provider Staff" has no guidance.

**[LOW] Owner badge is visually the same style as regular team member rows.**
The "Owner" badge is a small text label but the owner row otherwise looks identical to a regular staff row. The owner row should be visually distinguished (e.g., top position, different background).

---

### 6. Profile Page

No deep code analysis was performed on this page. From navigation structure, it exists and calls `providersAPI.getMe()` and `updateMe()`. Known gaps based on DTO analysis:

- No bank account details editing visible from the portal.
- Logo upload redirected to Media tab (same problem as listings — split across two pages).
- KYC / verification status shown read-only but provider cannot initiate verification.

---

### 7. Support Page

**What works:**
- Two-column layout (form left, links right) is one of the better-designed pages in the portal.
- Recent support request history is shown inline.
- FAQ collapsible section reduces noise.

**Issues:**

**[MEDIUM] Subject and message fields have no character counter displayed.**
The subject field has `maxLength={500}` and message has `maxLength={8000}`, but no counter is visible. A provider typing a long message doesn't know how close they are to the limit until they hit it.

**[MEDIUM] Reply history not shown inline with requests.**
The history table shows submitted requests with a "Replied" / "Awaiting reply" badge, but to read the actual reply the provider must... look elsewhere. The staff reply (`staffReply` label) is referenced in translations but it's unclear from the code whether the reply text is rendered in the table row or requires clicking into detail.

**[LOW] "Open company tickets" link points to the company app URL.**
This cross-surface link is correct behavior but may confuse a provider who doesn't know they have access to the company dashboard (many won't).

---

### 8. Media Page

Based on code exploration:

**[HIGH] No image preview before submission.**
Providers upload images and submit them for admin review. There is no preview of what the submitted image looks like in context (e.g., how it would appear on the listing card). The provider submits blind.

**[HIGH] No ability to delete a rejected submission and resubmit.**
If an image is rejected, the provider can see the rejection note in the submissions table, but there is no "Replace" or "Delete and resubmit" action. The rejected submission stays in the list indefinitely.

**[MEDIUM] Logo and service images are on the same page but in separate sections.**
Uploading a logo and uploading service gallery images use different API endpoints and different UI sections, which is architecturally correct but creates confusion about which section to use for what purpose.

---

## Cross-Cutting Issues (Both Surfaces)

---

### A. UUID Inputs Throughout — The Biggest UX Failure

The following admin workflows require typing raw 36-character UUIDs:

| Page | Action | UUID field |
|---|---|---|
| Complaints | Assign agent | Agent ID text input |
| Complaints | Escalate | Target user ID text input |
| Provider Portal Staff | Link existing user | User UUID text input |
| Bookings (company) | Filter by provider | Provider ID text input |

None of these should exist. Every UUID input must be replaced with a searchable dropdown or autocomplete fetching from the relevant API.

---

### B. Raw Database Enums Displayed to Users

The following status values appear directly in the UI as unformatted enum strings:

| Surface | Page | Raw values |
|---|---|---|
| Company | Providers | `PENDING_APPROVAL`, `ACTIVE`, `SUSPENDED`, `INACTIVE` |
| Company | Bookings | `PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED` |
| Company | Discounts | `ACTIVE`, `PENDING_APPROVAL`, `INACTIVE` |
| Company | Complaints | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| Company | Taxi Trips | `SEARCHING`, `ASSIGNED`, `EN_ROUTE_TO_PICKUP`, etc. |
| Provider | Listings | `PENDING_APPROVAL`, `ACTIVE`, `INACTIVE`, `REJECTED` |

All enum values must be mapped to human-readable labels through the i18n system.

---

### C. Hardcoded English Strings (Not Using `t()`)

Beyond the already-documented Earnings page, these are additional hardcoded strings found in company dashboard pages:

**`MediaSubmissionsPage.tsx`:**
```tsx
<span className="badge badge-success">Approved</span>
<span className="badge badge-danger">Rejected</span>
<span className="badge badge-warning">Pending</span>
```

**`AuditLogsPage.tsx`:**
- Timestamps: raw ISO string output
- Old/New values: raw JSON

**`PortalEarningsPage.tsx`:**
- All modal strings as documented (9 strings)

**Various pages:**
- `window.confirm()` dialogs use string concatenation: `t('providersPage.confirmDelete')` — correct. But some confirm dialogs skip `t()` entirely.

**Total estimated untranslated strings across both surfaces: ~30+**

---

### D. Missing Confirmation on Destructive Actions

| Page | Action | Confirmation |
|---|---|---|
| Reviews | Publish / Reject / Hide | ❌ None — single click |
| Providers | Suspend | ❌ None — single click |
| Providers | Reset password | ✓ `window.confirm()` |
| Providers | Delete | ✓ `window.confirm()` |
| Payments | Refund | ✓ Modal with reason (no double-confirm) |
| Discounts | Delete | ✓ `window.confirm()` |
| Discounts | Deactivate | ❌ None — single click |

---

### E. Missing Features — Summary Table

| Feature | Company (7050) | Provider (7060) |
|---|---|---|
| Mobile layout | ❌ None | ❌ None |
| Sidebar item badges (unread counts) | ❌ | N/A |
| Searchable provider dropdown in bookings filter | ❌ | N/A |
| Dashboard date range picker | ❌ | N/A |
| Revenue chart in reports | ❌ (data fetched, not rendered) | N/A |
| Booking detail view for providers | N/A | ❌ |
| Bookings filter/search for providers | N/A | ❌ |
| Payout history | N/A | ❌ |
| Payout fee deduction from max amount | N/A | ❌ |
| Image full-screen preview | ❌ | ❌ |
| Media delete / resubmit after rejection | N/A | ❌ |
| Complaint priority filter | ❌ | N/A |
| Complaint pagination | ❌ | N/A |
| Audit log pagination + date filter | ❌ | N/A |
| Review expand / full text | ❌ | N/A |
| Bulk approve media submissions | ❌ | N/A |
| Character counter on text areas | Partial | ❌ |
| Agent searchable dropdown (complaints assign) | ❌ | N/A |
| Staff UUID input replaced with search | N/A | ❌ |
| Service status locked to provider-appropriate options | N/A | ❌ |
| Discount code preview/test | ❌ | N/A |
| Settings: email templates, policies | ❌ | N/A |
| Analytics tab content | ❌ (empty) | N/A |

---

### F. Performance / Data Issues

**Payments summary cards from page data only.**
The most egregious data bug: collecting "Total collected" from the current 20-item page produces numbers that mean nothing to a finance user. The backend must return aggregate totals separately from the paginated list.

**Complaints loaded without pagination.**
All complaints matching a status filter are loaded in one request. At scale this will cause slow page loads and high memory usage.

**Audit logs capped at 50 with no pagination.**
50 entries represents roughly 1 hour of activity on a busy platform.

**Weekly chart data: `Week of ${d.toLocaleDateString(...)}` hardcoded English label.**
The tooltip `labelFormatter` in the portal overview chart hardcodes "Week of" in English regardless of locale.

---

## What Is Genuinely Good

**Company Dashboard:**
- The design system is consistent and premium. Sidebar, cards, tables, and modals all feel cohesive.
- Role-based navigation is architecturally correct — different roles see different sections.
- `BulkActionBar` for provider approve/suspend is a well-designed pattern.
- The WebSocket live dashboard with fallback to HTTP polling is sophisticated infrastructure.
- Sidebar collapse and RTL support are correct and smooth.
- React Query-like `staleTime` patterns on the analytics page are thoughtful.
- The `PermissionMatrixPage` (roles × permissions grid) is a genuinely useful admin tool for a complex RBAC system.
- `RequireAuth` + `RequireSurfaceRole` guards at the route level are the right security architecture.

**Provider Portal:**
- The overview KPI cards with skeletons are the best-implemented page on the portal.
- The `PortalSupportPage` layout (form + FAQ + quick links) is one of the cleanest designs in the codebase.
- Listing form service-type-aware fields (rooms for hotels, passengers for taxis) show careful domain modeling.
- The Staff page's two-path approach (link existing / create new) covers the real-world workflow.
- `PortalListingsPage` delete confirmation and status badge colors are done correctly.

---

## Priority Fix List

Ordered by user impact across both surfaces:

1. **Replace all UUID text inputs with searchable dropdowns** — Complaints assign/escalate, bookings provider filter, portal staff link. This is the single highest-impact UX fix.

2. **Add confirmation dialogs to all destructive single-click actions** — Reviews (Publish/Reject/Hide), Providers (Suspend), Discounts (Deactivate).

3. **Fix payments summary cards to use backend-provided aggregates** — Remove the page-slice computation; add an aggregate API call for totals.

4. **Translate all hardcoded strings in `PortalEarningsPage`** — 9+ English-only strings in a page that Arabic providers heavily use.

5. **Fix `MediaSubmissionsPage` status badge strings** — Replace hardcoded "Approved"/"Rejected"/"Pending" with `t()` calls.

6. **Add provider bookings detail view and basic filters** — A provider managing 50 bookings with no actions and no search is stranded.

7. **Add sidebar badge counts for pending complaints, reviews, and media approvals** — Admins need at-a-glance awareness of what needs action.

8. **Render the revenue byDay chart on the Reports page** — The data is fetched, the array exists; add a `<BarChart>` component.

9. **Lock the listing status dropdown on the provider portal to provider-appropriate values** — Remove `SUSPENDED`, `DISCONTINUED`, `HIDDEN`, `MAINTENANCE` from provider-facing form.

10. **Add payout history to the provider Earnings page** — Providers need to see the status of previous payout requests.

11. **Fix raw enum display** — Map all status enums to translated labels across both surfaces. This touches ~8 pages.

12. **Add mobile layout (hamburger menu) to both dashboards** — The sidebar-offset layout has no fallback on small screens.

13. **Add audit log pagination and date range filter** — 50 entries with no navigation is insufficient for compliance use.

14. **Format audit log timestamps and Old/New JSON diffs** — Human-readable output instead of raw ISO strings and JSON blobs.

15. **Implement the Analytics tab content** — The tab exists in the Reports page UI with no content.

---

*Report generated by external critic review of source code at `front/my-app/src/` — company surface `apps/company/`, provider surface `apps/provider/`, shared pages under `pages/`. All line references are approximate.*
