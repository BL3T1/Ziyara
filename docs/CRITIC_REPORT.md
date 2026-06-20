# Ziyara Platform — UX & Functionality Critic Report
**Date:** 2026-05-31  
**Surfaces reviewed:** Company Dashboard (`localhost:7050`) · Provider Portal (`localhost:7060`)  
**Credential used:** `super_admin@ziyarah.com` / `Demo123!`  
**Method:** Playwright full-page screenshots of every navigable route + mobile viewport (375 px) + dark-mode + Arabic toggle

---

## Overall Ratings

| Surface | Score | Summary |
|---|---|---|
| **Company Dashboard** | **5.5 / 10** | Solid bones, plagued by data leaks, missing polish, and ~8 incomplete pages |
| **Provider Portal** | **N/A** | Cannot be rated — company credentials are correctly rejected; no provider seed account exists to audit the portal |

---

## Company Dashboard (7050)

### Section 1 — Critical Bugs (fix before any release)

#### 1.1 Raw i18n key displayed as page title — `title.tickets`
`/support/tickets` renders the literal string **"title.tickets"** as the `<h1>` and in the browser tab. It is also showing breadcrumb as "Dashboard" instead of the correct page name. A user who lands here sees machine-readable garbage.

#### 1.2 API page stuck forever
`/admin/api` shows **"Loading API specification…"** indefinitely. The spinner never resolves — no error, no retry button, no timeout message. The page is a dead end.

#### 1.3 Activity feed exposes raw backend enum values
The dashboard home displays internal event names verbatim:
```
PROVIDER_PASSWORD_RESET   2026-05-31T09:48:06.124358
PROVIDER_CREATE_ACTIVE    2026-05-31T09:43:46.504383
PROVIDER_PASSWORD_RESET   2026-05-31T09:40:35.154215
```
These are backend `AuditEvent` enum identifiers, not human labels. A CEO looking at their dashboard should see **"Provider password reset"** and **"Provider activated"**, not snake-case system codes. Additionally, the timestamps include millisecond precision in ISO-8601 format — a business admin should see **"Today at 9:48 AM"**, not `2026-05-31T09:48:06.124358`.

#### 1.4 Activity feed bottom row clipped
The last visible row in the activity feed widget is partially cut off by the card boundary. There is no "See all" link or scroll affordance — the user cannot read the full entry or navigate to the full log from here.

#### 1.5 Footer links are broken in the dashboard
The footer on every dashboard page has **Privacy · Terms · Contact** links. All three redirect to the dashboard home (`/`) rather than any real page. Clicking them silently fails.

#### 1.6 Audit Logs — data columns missing
The `/admin/logs` page has four columns — TIME, ACTION, ENTITY, USER, OLD VALUE, NEW VALUE — but ENTITY, USER, OLD VALUE, and NEW VALUE are all `—` for every row. The log is useless for tracing who changed what.

---

### Section 2 — Navigation & Information Architecture

#### 2.1 Sidebar collapsed by default — icon-only is hostile to new users
The sidebar opens in a collapsed state showing only icons. A new admin has no idea that the icon that looks like a fork-and-spoon opens Restaurants, or that the briefcase icon opens Trips. Hover tooltips exist but require discovery. An admin dashboard is not a consumer app — **expanded by default with labels** is the correct pattern here.

#### 2.2 Section headers invisible when collapsed
"MAIN", "SERVICES", "MANAGEMENT" labels are invisible in collapsed mode. The icon groupings give no hint that some icons are services and others are management tools. A new user will click randomly.

#### 2.3 No active-state feedback on icon-only sidebar
In the collapsed view, the currently active route highlights the icon with a teal accent, but the very small size makes the distinction difficult to perceive at a glance.

#### 2.4 Sidebar section "Collapse sidebar" label
The expand/collapse control is labeled **"Collapse sidebar"** even when already collapsed (it should say "Expand sidebar" or be a toggle without text). Minor, but confusing.

---

### Section 3 — Inconsistencies

#### 3.1 Login field label mismatch between surfaces
| Surface | Field label | Accepts |
|---|---|---|
| Company (7050) | **Username** | Email address |
| Provider (7060) | **Email** | Email address |

Both surfaces authenticate with the same email address but label the field differently. Pick one and use it everywhere. "Email" is the correct choice — the placeholder even says `you@example.com` on the provider side.

#### 3.2 CTA naming is inconsistent across service pages
Every service page uses a different button label for the same action (creating a provider account for that category):

| Page | Button text |
|---|---|
| Providers | "Create provider" |
| Hotels | "Create new hotel account" |
| Resorts | "Create new resort account" |
| Restaurants | "Create new restaurant account" |
| Taxis | "Create new taxi account" |
| Trips | "Create new trip account" |

This is six different phrasings for one concept. Standardize to something like **"Add provider"** or **"New [service type]"**.

#### 3.3 Three different date formats in three pages
| Page | Format seen | Example |
|---|---|---|
| Audit logs | `DD Mon YYYY, HH:mm` | `01 May 2026, 09:48` |
| Discounts | `M/D/YYYY` | `5/29/2026` |
| Currency rates | `YYYY-MM-DD` | `2026-05-23` |

Pick a single format and enforce it globally. Recommendation: `MMM D, YYYY` (e.g., `May 29, 2026`).

#### 3.4 Status badge casing is inconsistent
- Providers table: **"Active"** (title case) with a green badge
- Discounts table: **"ACTIVE"** (all caps), plain text

Should use the same badge component with consistent casing.

#### 3.5 "Ziyara delta (commission)" — internal jargon in user-facing UI
The dashboard commission widget labels one line **"Ziyara delta (commission)"**. "Delta" is an engineering/accounting internal term. A business admin reading this doesn't know what delta means here. Rename to **"Platform commission"** or **"Ziyara fee"**.

---

### Section 4 — Missing or Incomplete Functionality

#### 4.1 Currency rates — no "Edit" action
The currency rates table offers only **View** and **Delete** per row. To change a rate, an admin must delete the existing rate and re-create it. This is an unnecessary multi-step workflow for what should be a single "Edit" action. This is also destructive — deleting a rate while bookings reference it could cause data integrity problems.

#### 4.2 Taxi trips page — no filters, no search, no export
`/management/taxi-trips` shows a single "Refresh" button and an empty state. There are no date filters, provider filters, status filters, or export options. Every other management page has at least tab filters. This page looks unfinished.

#### 4.3 Service health widget is a permanent placeholder
The dashboard home shows a "Service health" panel that always says **"No service health data yet"**. There is no configuration path, no documentation about when it populates, and no way to dismiss it. Either implement it or remove it — a placeholder that cannot be filled adds noise and erodes trust.

#### 4.4 Reports — Analytics tab is empty with no explanation
The Reports page has three tabs: Revenue, Bookings, Analytics. The Analytics tab renders a blank area with no content, no "coming soon" notice, and no error. The user clicks it and sees nothing.

#### 4.5 Empty states across the platform give no guidance
Pages with no data (Resorts, Restaurants, Taxis, Trips, Bookings, Payments, Reviews, Complaints, Tickets) all display a single line of text: "No [things] listed yet." or "No [things]." There is no illustration, no call to action, no explanation of what the user should do first. Good empty states say **why** it's empty and **what to do** about it.

#### 4.6 Partner rating shown as text, not stars
Provider/hotel cards display **"0.0 / 5"** as plain text. A rating is immediately more comprehensible as a star component (even with 0 stars filled). The text-only format looks like a data row, not a quality indicator.

#### 4.7 Payout summary always shows "No payouts for period"
The dashboard payout widget exists but always shows no data. Based on known backend gaps, the payout endpoints are not yet implemented, but users see the widget with placeholder text — a broken promise of functionality.

#### 4.8 Audit logs — ENTITY and USER are always empty
As noted in the bug section, the audit log shows ACTION but never identifies ENTITY or USER. This makes the log useless for accountability. "Role permissions updated (custom)" is recorded but there is no way to know which role was changed or who changed it.

#### 4.9 Settings page — minimal to the point of feeling unfinished
`/admin/settings` contains three fields: company display name, default currency (a raw text input — not a dropdown), and a maintenance mode checkbox. The entire content fits in a tiny card in the top-left of an otherwise empty page. There is no section for notification preferences, email templates, branding, or any other admin-level configuration an operator would need.

#### 4.10 Content page is a raw JSON editor
`/admin/content` exposes the landing page copy as two raw JSON textareas (English and Arabic). An operator managing copy should not need to write or read JSON. This page needs a proper form-based CMS interface or at minimum field-by-field inputs instead of a monolithic JSON blob.

---

### Section 5 — Aesthetics & Visual Design

#### 5.1 General visual quality
The design system is coherent: the dark sidebar against a light content area, the `primary: #1e4d6b` blue for CTAs, the tan gold `#ac9e78` accent on the "Sign in" button — all feel intentional and on-brand. The card shadows are subtle and professional. Overall it reads as a serious admin tool, not a toy.

#### 5.2 KPI cards on mobile (375 px) are too large
On mobile, each KPI card takes nearly the full viewport height, meaning a user sees only one card at a time and must scroll far to see the activity feed. The cards should be 2×2 grid on mobile or use a condensed numeric style.

#### 5.3 Icon-only sidebar on desktop wastes nothing but adds friction
See 2.1 above. Aesthetically the collapsed sidebar is clean, but function beats form in an admin tool.

#### 5.4 Dark mode is well executed — with one issue
Dark mode toggles correctly and the design holds up. The sidebar in dark mode auto-expands to show text labels (inconsistent with light mode where it defaults to collapsed). The two modes have different default sidebar behaviors — pick one.

#### 5.5 Arabic toggle behavior unclear
After clicking the Arabic/language toggle button, the screenshot captured the same dark-mode English layout — the page did not switch to RTL Arabic. Either the toggle clicked the wrong element, or the Arabic switch does not apply correctly after dark mode is enabled. If RTL switching is broken in dark mode, that is a high-severity i18n regression.

#### 5.6 Topbar icon cluster is visually noisy
The topbar right section shows: search · archive/bookmark · dark mode · accessibility (the `عربي` pill) · language flags · notifications · user avatar. Seven distinct controls crammed into one row with no visual grouping or separator. Consider grouping: [search] [notifications bookmark] [settings cluster] [user].

#### 5.7 "SUPER ADMIN" micro-label above page title
Every page header shows a very small "SUPER ADMIN" label above the page title (e.g., "SUPER ADMIN / Providers"). This role indicator is useful in principle but the font size and placement make it feel like system metadata leaking into the UI. It could be shown in the user avatar dropdown instead.

---

### Section 6 — Clarity of Labels and Copy

| Location | Current text | Issue |
|---|---|---|
| Providers table → action | "Edit profit" | Should be "Edit margin" to match the column header "Profit margin" |
| Roles table → action | "Sidebar" | Cryptic — clicking "Sidebar" presumably edits the sidebar navigation config for that role; label should be "Edit sidebar" |
| Dashboard widget | "Ziyara delta (commission)" | Internal jargon; use "Platform commission" |
| Activity feed | "PROVIDER_CREATE_ACTIVE" | Raw enum; show "Provider activated" |
| Settings | "Default currency (ISO 4217)" | Subtitle "(ISO 4217)" is a spec reference — unhelpful to admins; use a dropdown of common currencies instead |
| Support tickets page | "title.tickets" | Raw i18n key; must be fixed |
| Discounts table | Status "ACTIVE" | All-caps; use title case + badge |
| Commission widget | "View transaction ledger →" | "Ledger" is accountant vocabulary; consider "View payment history" |
| Hotels/Resorts/etc. page subtitle | "Onboarded providers for this category. They appear here before their first bookable listing is published." | First sentence is not really a subtitle; restructure as "Partner accounts — providers appear here before publishing their first listing." |

---

### Section 7 — Accessibility Basics

- **Icon-only navigation** has no persistent text alternative — only tooltip on hover; fails WCAG SC 1.1.1 for keyboard-only users.
- **Date inputs** on Bookings use native `<input type="date">` with a raw `mm/dd/yyyy` placeholder — these are not keyboard-accessible in all browsers.
- **No visible focus ring** observed on sidebar icons in the screenshots (may be present but not visible in screenshots).
- **Color alone for status** — the ACTIVE/Pending distinction in the Discounts and Providers table relies on text color (green vs grey); no additional icon or shape used.

---

## Provider Portal (7060)

### Cannot be fully audited

The super_admin account is correctly blocked from the provider portal with the message:  
> *"Company staff must use the company dashboard to sign in, not the partner portal."*

This is a well-worded, actionable error — good work.

However, **no provider-level seed account exists** to test the portal. Everything after the login gate is invisible. The items below are observations from the login page only.

### Provider Portal — Login Page Observations

| # | Finding |
|---|---|
| P-1 | Field label is **"Email"** — inconsistent with company dashboard which calls it **"Username"** (both accept email) |
| P-2 | After failed login, credentials are retained in the form — good UX, the user doesn't have to retype |
| P-3 | The visual design is **identical** to the company login — same card, same button, same luggage icon. There is no visual signal that this is a different portal for a different role. A URL bar is the only differentiator |
| P-4 | **"Forgot password?"** link exists — the flow was not tested |
| P-5 | All `/dashboard`, `/bookings`, `/services`, `/staff`, `/payments`, `/payouts`, `/profile`, `/settings`, `/reviews`, `/reports`, `/support` routes silently redirect to the login page with no error — a user who bookmarks a deep link lands on login with no explanation |

---

## Priority Summary

### P0 — Must fix (data quality / credibility issues)
1. `title.tickets` raw i18n key on support tickets page
2. API page stuck loading forever
3. Activity feed raw enum values + ISO millisecond timestamps
4. Audit log ENTITY / USER columns all empty
5. Footer Privacy / Terms / Contact links broken

### P1 — Should fix (usability blockers)
6. Currency rates — add Edit action
7. Sidebar collapsed by default — expand by default
8. Inconsistent CTA labels across service pages (6 different phrasings)
9. Three different date formats across the platform
10. Service health widget permanently shows placeholder
11. Taxi trips page — needs filters and export
12. Arabic RTL broken in dark mode (verify and fix)

### P2 — Polish (quality of life)
13. Activity feed clipped bottom row + missing "see all" link
14. Status badge casing inconsistency (ACTIVE vs Active)
15. "Edit profit" → "Edit margin" (matches column header)
16. "Sidebar" action in Roles table → "Edit sidebar"
17. "Ziyara delta (commission)" → "Platform commission"
18. Empty states need guidance / illustration
19. Settings page needs more settings
20. Content page needs form inputs, not raw JSON
21. KPI cards too large on mobile 375 px
22. Topbar icon cluster needs visual grouping
23. Partner rating "0.0 / 5" text → star component
24. Reports → Analytics tab shows nothing (add placeholder or content)
25. Provider portal deep-link redirects need an explanatory message

---

*Report generated by automated Playwright audit + manual screenshot review.*  
*Screenshots archived at `C:\Users\BL3T\AppData\Local\Temp\ziyara-audit\screenshots\`*
