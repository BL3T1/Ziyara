# Frontend Implementation Plan: Company Dashboard & Client Portal

This plan builds the **missing frontend** for both the **Company Dashboard** (G1–G6) and the **Client Portal**—the **Provider Dashboard** (G7) where hotels, taxis, trips, and restaurants manage their part of the work under the company’s control—using the **current design** (Figma: Ziyara dashboard design; `front/my-app` design tokens). Where a UI element is not yet in the design (e.g. language switcher, ⌘K search modal), the plan asks you to **provide the Figma** for it when that phase starts.

**References:** [FRONTEND_GAP_REPORT.md](./FRONTEND_GAP_REPORT.md), [DASHBOARD_DESIGN_REPORT.md](./DASHBOARD_DESIGN_REPORT.md), [ROLE_MANAGEMENT_REPORT.md](./ROLE_MANAGEMENT_REPORT.md), `front/my-app/PROJECT_CONTEXT.md`.

---

## Strategy Summary

| Target | Approach |
|--------|----------|
| **Company Dashboard** | Implement in **`front/my-app/`** (Vite, React 19, TypeScript, Tailwind). Use existing layout (Sidebar, DashboardHeader), design tokens (primary #1e4d6b, secondary #ac9e78), and Figma dashboard design. Wire real auth and backend API; replace placeholders with real pages per role (G1–G6). |
| **Client Portal (Provider Dashboard)** | A **semi–company dashboard** for **service providers** (hotels, taxis, trips, restaurants). Same dashboard structure—sidebar, modules, overview, tables—but **scoped to the provider**: they manage *their* listings, *their* bookings, *their* staff, and *their* earnings **under the company’s control** (permissions and data filtered by `provider_id`). Unblock first in CRA; then build full portal in chosen app (Phase 0). |
| **Design** | Use current Figma (channel `7s9roivi`) and existing components. When a control is missing (theme toggle, language switcher, ⌘K modal, etc.), the phase will state: **“Figma needed: [description].”** |

### Client Portal = Provider Dashboard (G7)

The **Client Portal** is the **provider-facing dashboard**. It is not a simple “my bookings” page; it is a **full dashboard experience** for providers (hotels, taxis, trips, restaurants) to run their part of the business while the company retains control:

- **Same UX pattern as company dashboard:** Sidebar navigation, header with breadcrumbs, overview (KPIs), and module-based pages (Listings, Bookings, Staff, Earnings, Profile, Support).
- **Scoped to the logged-in provider:** All data is filtered by `provider_id` (enforced by backend). Providers only see their own services, bookings, staff, and revenue.
- **Company-controlled:** The company defines what providers can do via G7 roles and permissions (e.g. **PROVIDER_MANAGER**: listings, staff, earnings; **PROVIDER_STAFF**: check-in/out, booking schedule). No access to company-wide data, roles, or financials.
- **Provider roles (G7):** PROVIDER_MANAGER (full management of their properties), PROVIDER_STAFF (guest check-in/out, booking schedule). Sidebar and pages can vary by provider role within the portal.

---

## UX pattern: Group-first list view

Use this pattern on **every page that lists entities that belong to a natural group or category**: first show **groups as cards**, then after the user selects one, show **items in that group in a table**.

### Flow

1. **Step 1 – Cards view:** Page shows **group/category cards** (e.g. roles, verticals, statuses). Each card can show a label and optional count or short summary.
2. **Step 2 – Selection:** User clicks one card (e.g. “G2 – Sales”, “Hotels”, “Pending”).
3. **Step 3 – Table view:** Page shows a **table** of only the items in the selected group. Breadcrumb or back control returns to the cards view. Table supports the usual filters, sort, and actions.

### Where to apply

| Page | Groups (cards) | Then table shows |
|------|-----------------|------------------|
| **Management > Users** | **Groups/Roles** (G1–G7 or role names, e.g. Super Admin, Sales, Finance, Support, Executive, HR, Provider) | Users in the selected group/role |
| **Management > Providers** | **Verticals** (Hotels, Restaurants, Taxis, Trips) or **Status** (Approved, Pending, Suspended) | Providers in the selected vertical or status |
| **Management > Bookings** | **Status** (Pending, Confirmed, Completed, Cancelled) or **Service type** | Bookings in the selected status or type |
| **Management > Payments** | **Status** (Pending, Completed, Failed, Refunded) or **Method** (Card, Transfer, etc.) | Payments in the selected status or method |
| **Support > Tickets** | **Priority** (Low, Medium, High, Urgent) or **Status** (Open, In progress, Resolved, Closed) | Tickets in the selected priority or status |
| **Support > Complaints** | **Priority** or **Status** | Complaints in the selected priority or status |
| **Admin > Audit logs** (optional) | **Entity type** (User, Role, Booking, Payment, etc.) or **Action** | Audit entries in the selected type/action |

Use the same card and table styling across these pages so the experience is consistent. **Users page (Super Admin)** is the primary example: groups (roles) as cards → select one → users in that group in a table.

---

## Phase 0: Scope & Design Baseline (Week 0)

**Goal:** Lock app choice, design baseline, and Figma checklist so Phases 1+ can proceed without rework.

### 0.1 App assignment

- **Company Dashboard:** `front/my-app/` is the canonical app. All G1–G6 dashboard routes and features go here.
- **Client Portal (Provider Dashboard):** Decide one of:
  - **Option A:** Keep provider dashboard in CRA (`frontend/`). Phase 1 fixes broken layout/overview; Phase 5 adds full provider modules (Listings, Bookings, Staff, Earnings) under the same ClientPortalLayout.
  - **Option B:** Build provider dashboard in `front/my-app/` under `/portal` with a dedicated **ClientPortalLayout** (same dashboard pattern, provider-scoped nav). Phase 1 still fixes CRA imports (redirect or remove portal routes there).

*Recommendation:* Option B so one stack and one design system serve both company dashboard and provider dashboard; providers get the same “dashboard feel” with different data and permissions.

### 0.2 Design tokens

- Align with current design: **primary** `#1e4d6b`, **secondary** `#ac9e78`, **Inter** (or Figma font), spacing base 8px.
- If DASHBOARD_DESIGN_REPORT palette (Royal Blue #1A237E, Success/Warning/Danger) is adopted later, add CSS variables and map Tailwind; no Phase 0 change required if Figma uses current tokens.

### 0.3 Figma checklist (for later phases)

- Confirm you have (or will provide) Figma for:
  - Theme toggle (dark/light) in header.
  - Global search modal or command palette (⌘K).
  - Language switcher (EN/AR) for i18n phase.
  - Refund modal (reason field, confirm/cancel).
  - Provider dashboard (Client Portal) nav/sidebar and overview if different from company dashboard.
- **Figma needed (Phase 0):** None. Only confirm the above exist or will be provided when requested in the relevant phase.

### Deliverables

- [ ] Document: “Company Dashboard = front/my-app; Client Portal = [Option A | B].”
- [ ] Design token doc or Tailwind config updated if needed.

---

## Phase 1: Foundation & Unblock Portal (Weeks 1–2)

**Goal:** Real auth and API in dashboard app; theme toggle; unblock portal routes (no runtime errors).

### 1.1 Fix Client Portal (Provider Dashboard) in CRA (critical)

- Add **`frontend/src/layouts/ClientPortalLayout.js`**: **Provider dashboard layout**—same structural pattern as company dashboard (sidebar + header + main). Sidebar: Overview, Listings, Bookings, Staff, Earnings, Profile, Support. Header: title, breadcrumbs, notifications, avatar. Accept `title`, `breadcrumbs`, `children`. Reuse or adapt CRA dashboard header/sidebar styles so it feels like a semi–company dashboard.
- Add **`frontend/src/pages/portal/ClientPortalOverview.js`**: **Provider overview** (command center for the provider): welcome, KPIs scoped to *their* business (e.g. upcoming bookings, revenue this period, pending tasks). Use provider-scoped dashboard API when available; otherwise placeholder stats and links to Listings, Bookings, Support.
- Verify routes: `/portal`, `/portal/listings`, `/my-bookings`, `/profile`, `/support`, `/support/tickets/:id`, `/support/tickets/new` render without import errors.

### 1.2 Company Dashboard (front/my-app): Real auth & API

- **Auth:** Replace mock `AuthContext` with real login (call `POST /api/v1/auth/login`). Store token (e.g. localStorage), set user from response (id, email, name, role/group). Add logout (invalidate token, clear user). Optional: refresh token, 401 interceptor redirect to `/login`.
- **API client:** Create `src/services/api.ts` (or reuse pattern from CRA `api.js`): base URL from env, axios/fetch with `Authorization: Bearer <token>`, interceptors for 401. Export modules: `authAPI`, `dashboardAPI`, `providersAPI`, `rolesAPI`, `auditLogsAPI`, etc., matching backend.
- **Role mapping:** Map backend role/group (e.g. G1–G6) to app role type so sidebar and route guards use it.

### 1.3 Theme toggle (dashboard)

- Add dark/light theme toggle in **DashboardHeader** (e.g. icon button). Toggle updates `document.documentElement` class or data attribute and persists to localStorage; Tailwind dark mode via `class` strategy.
- **Figma needed:** If the header theme toggle is not in the current Figma, please provide a frame or component for “Theme toggle (dark/light) in dashboard header” so we match the design.

### 1.4 Sidebar collapse (dashboard)

- Make sidebar collapsible to icon-only on smaller screens or via a toggle. Use existing Sidebar component; add state and narrow width when collapsed. Match DASHBOARD_DESIGN_REPORT “collapses to slim icon-only bar.”

### Deliverables

- [ ] CRA: `ClientPortalLayout.js`, `ClientPortalOverview.js`; portal routes load.
- [ ] front/my-app: Login → real API; token in localStorage; API client with auth.
- [ ] front/my-app: Theme toggle in header; sidebar collapse.
- [ ] **Figma request:** Theme toggle in header (if not already in design).

---

## Phase 2: Company Dashboard – Global Overview & Provider Management (Weeks 3–4)

**Goal:** Command Center (KPIs, activity feed, service health) and Provider Management (list, filters, commission override) wired to backend.

### 2.1 Global Overview (Command Center)

- **DashboardPage:** Replace mock stats with `dashboardAPI.getKpis()` (revenue, active bookings, total providers, pending complaints). Use existing `StatCard`; add trend if API provides it.
- **Activity feed:** Call `dashboardAPI.getActivity(limit)` and show scrolling/list of events (e.g. “New booking”, “Provider payout”) with timestamp and user/action.
- **Service health:** If backend exposes health (e.g. `/actuator/health` or custom), show mini-charts or status for Hotels/Taxis/Restaurants. Otherwise keep a small placeholder “Service health” widget and hook up when API exists.
- **Real-time (optional):** Simple polling (e.g. refetch KPIs every 60s) when tab focused; no Figma needed for this.

### 2.2 Users (Management > Users) – group-first

- **Users page** (Super Admin / HR / Admin): Use **group-first list view** (see UX pattern above). **Step 1:** Show **groups (roles)** as cards (e.g. G1 Super Admin, G2 Sales, G3 Finance, G4 Support, G5 Executive, G6 HR, G7 Provider). **Step 2:** Super Admin selects one card. **Step 3:** Show **table of users** in that group/role (columns: name, email, status, department if applicable, actions). Breadcrumb or “Back to groups” returns to cards. Use `rolesAPI.getGroups()` or similar for cards; `userAPI.list({ groupId })` or filter by role for the table.
- **Personnel Directory** (G6 HR) and any other “list users by role” view use the same pattern: roles/groups as cards → then users table.

### 2.3 Provider Management – group-first

- **Providers list page** (Management > Providers): Use **group-first list view**. **Step 1:** Show **verticals** (Hotels, Restaurants, Taxis, Trips) or **status** (Approved, Pending, Suspended) as cards. **Step 2:** User selects one. **Step 3:** **Table** of providers in that vertical/status: name, type, status, commission rate; filters and detail/commission override as below.
- **Provider row actions:** Link or button to “Details” (modal or side panel). In details: performance metrics (booking volume, review average) if API provides; **commission override**: standard input, save via `providersAPI.updateCommission(id, { commissionRate })`.
- **Document verification:** If backend returns document URLs, add a small “Documents” section (gallery/list of business license, ID). If not in API yet, add placeholder and note for backend.
- **Figma needed:** Only if you want a specific “Provider detail” panel; otherwise we use current Card/input/table styles.

### 2.4 Bookings (Management > Bookings) – group-first

- **Bookings list page:** Use **group-first list view**. **Step 1:** Show **status** (Pending, Confirmed, Completed, Cancelled) or **service type** (Hotel, Restaurant, Taxi, Trip) as cards. **Step 2:** User selects one. **Step 3:** **Table** of bookings in that status/type (customer, service, dates, amount, status, actions). Use `bookingAPI.list(params)`. Breadcrumb or “Back” returns to cards.

### Deliverables

- [ ] DashboardPage: real KPIs, activity feed, optional service health.
- [ ] **Users page:** groups (roles) as cards → select → users in selected group in table.
- [ ] **Providers page:** verticals or status as cards → select → providers table; detail view, commission override.
- [ ] **Bookings page** (Management > Bookings): **group-first** – status (Pending, Confirmed, Completed, Cancelled) or service type as cards → select → table of bookings.
- [ ] Optional: polling for KPIs/activity.

---

## Phase 3: Company Dashboard – Financials & RBAC (Weeks 5–6)

**Goal:** Transaction ledger, commission analysis, refund flow; Role Architect and Audit logs.

### 3.1 Financials & Payments – group-first

- **Payments / Transaction ledger** (Management > Payments or Finance > Ledger): Use **group-first list view**. **Step 1:** Show **status** (Pending, Completed, Failed, Refunded) or **method** (Card, Transfer, etc.) as cards. **Step 2:** User selects one. **Step 3:** **Table**: payment id, booking ref, amount, currency, method, status, 3DS status, gateway reference, date. Use `paymentsAPI.list(params)`; support search and filters (date range). Breadcrumb or “Back” returns to cards.
- **Commission analysis:** Section or page “Commission Analysis”: visual breakdown “Base Collected” vs “Ziyarah Delta” (e.g. bar or stacked chart). Use dashboard revenue/commission APIs if available; otherwise placeholder with note for backend.
- **Refund flow:** From ledger row or payment detail: “Refund” button → modal with **mandatory “Reason”** field and confirm. Call `paymentAPI.refund(id, { reason })`. Show success/error toast; refresh list. Audit trail is backend; frontend only sends reason.
- **Payout summary:** If backend has “scheduled payouts” or “payout ledger,” add a small widget or table. Otherwise placeholder.
- **Figma needed:** Refund modal (reason text area, primary/secondary buttons). If you have a “Refund” or “Confirm action” modal in Figma, please share it.

### 3.2 RBAC & Security

- **Roles page** (replace Admin > Roles placeholder): List roles (from `rolesAPI.list()`), create new role (name, description, group), edit permissions (grid of resource:action toggles from `rolesAPI.getPermissionCatalogue()`), delete with reassignment (from ROLE_MANAGEMENT_REPORT). Reuse logic/patterns from CRA `RoleManagementPage.js`; port to TypeScript and current design.
- **Audit logs:** Page or section “Audit logs”: search by user, IP, or text; table with user, action, entity, old/new value, timestamp. Use `auditLogsAPI.getRecent({ search })`. Restrict to G1 (or by permission) if required.
- **Permission-based visibility:** Where backend returns user permissions, hide “Commission” or “Payment details” for roles without `pay:view_commission`; hide “Audit” for roles without `sys:audit_read`. Optional for Phase 3; can be Phase 6.

### Deliverables

- [ ] Payments/ledger page: table, 3DS/gateway columns, search/filters.
- [ ] Commission analysis widget or page.
- [ ] Refund modal with mandatory reason; call refund API.
- [ ] Roles page: list, create, edit permissions, delete with reassignment.
- [ ] Audit logs page: search, table.
- [ ] **Figma request:** Refund modal (reason field + actions).

---

## Phase 4: Company Dashboard – Support & Role-Specific Views (Weeks 7–8)

**Goal:** Ticket queue by priority, resolution stats; G2–G6 specific dashboards and pages.

### 4.1 Customer Support (The Bridge) – group-first

- **Ticket queue** (Support > Tickets): Use **group-first list view**. **Step 1:** Show **priority** (Low, Medium, High, Urgent) or **status** (Open, In progress, Resolved, Closed) as cards. **Step 2:** User selects one. **Step 3:** **Table** of tickets in that priority/status; filters (assignee, date). Use `ticketAPI.list(params)`, `ticketAPI.getStats()` for counts on cards if available.
- **Complaints** (Support > Complaints): Same pattern. **Step 1:** Priority or status as cards. **Step 2:** Select one. **Step 3:** Table of complaints. Use `complaintAPI.list(params)`.
- **Conversation:** Ticket detail page: comments thread, add comment (internal), resolve/close actions. Use existing TicketDetail pattern from CRA; port to front/my-app.
- **Resolution dashboard:** Widget or section: open tickets by priority (doughnut chart), average resolution time, optional CSAT. Use `ticketAPI.getStats()` and complaint stats if API provides.
- **Escalation:** On ticket/complaint detail, “Escalate” button (reassign to manager). Backend endpoint if available.
- **Figma needed:** Priority badges (Low/Medium/High/Urgent) and Escalation button/style if you want them to match a specific design.

### 4.2 Role-specific dashboards (G2–G6)

- **G2 Sales:** Sales dashboard: new provider applications count, top “hot” verticals, leads list (pending → approved). Links to Leads Manager (provider list filtered by status) and Contract Viewer (document list) if APIs exist.
- **G3 Finance:** Finance dashboard: total revenue vs net commission, pending payouts value, refund request volume. Links to Payout Ledger, Commission Controller (reuse Providers commission UI), Tax & Compliance placeholder.
- **G4 Support:** Support dashboard: open tickets by priority (chart), avg resolution time, CSAT. Links to Ticket Queue and Escalation Hub (ticket list with escalate action).
- **G5 Executive:** Executive dashboard: YoY revenue growth, market share (Hotels vs Taxis vs Trips), commission per vertical. Links to BI Reports placeholder and Global Approvals (e.g. discount approvals, provider onboarding approvals) if backend supports.
- **G6 HR:** HR dashboard: staff activity pulse, new hire onboarding progress, employee status (Active/Frozen). Links to **Personnel Directory** (uses **group-first**: roles/groups as cards → users in selected group in table), Access Control (assign role to user; no role editing).

Use **sidebar config** to show only the sections relevant to each role (already partially in place); add these pages and wire to dashboard APIs where available. Placeholders for APIs that don’t exist yet.

### Deliverables

- [ ] Ticket queue: priority sort, filters, resolution stats.
- [ ] Ticket detail: comments, resolve, escalate.
- [ ] G2–G6 dashboard pages with real or placeholder widgets and links to unique pages.

---

## Phase 5: Client Portal (Provider Dashboard) – Full Experience (Weeks 9–10)

**Goal:** Complete the **provider dashboard** (semi–company dashboard): same layout pattern as company dashboard, with modules for providers to manage their work under company control. All data and actions are scoped by `provider_id`; G7 roles (PROVIDER_MANAGER, PROVIDER_STAFF) define what each user can see and do.

### 5.1 Provider dashboard layout and shell

- **ClientPortalLayout** (in chosen app): Same structural pattern as Company Dashboard—**static sidebar (left)** with modules, **header** with breadcrumbs and user/notifications, **main content**. Sidebar sections (company-controlled via permissions):
  - **Overview** – provider’s command center (KPIs).
  - **Listings** – their services (hotels, restaurants, taxis, trips).
  - **Bookings** – their bookings and schedule.
  - **Staff** – their team (PROVIDER_MANAGER only if applicable).
  - **Earnings** – their revenue, commission, payouts.
  - **Profile** – provider account.
  - **Support** – tickets/complaints.
- **Role-based nav within portal:** PROVIDER_STAFF may see a reduced set (e.g. Bookings, Profile, Support) vs PROVIDER_MANAGER (full set). Sidebar visibility follows backend permissions.
- Routes (examples): `/portal`, `/portal/listings`, `/portal/bookings`, `/portal/staff`, `/portal/earnings`, `/portal/profile`, `/portal/support`. If portal lives in `front/my-app`, use the same design system; only nav items and data scope differ from company dashboard.

### 5.2 Provider dashboard modules (under company control)

- **Overview:** Provider-scoped KPIs: upcoming bookings, revenue in period, pending actions (e.g. documents to submit, reviews to respond). Use provider-scoped dashboard API.
- **Listings:** CRUD for *their* services only (add/edit/disable listings). Backend enforces `provider_id`; company policies (e.g. approval workflow) apply.
- **Bookings:** Table of bookings for their services (guest, dates, status). Check-in/out actions for PROVIDER_STAFF where applicable.
- **Staff:** List and manage provider’s sub-users (if backend supports). PROVIDER_MANAGER only; company may restrict what can be changed.
- **Earnings:** Their view of revenue, commission deducted by company, and payouts. Read-only or as allowed by company; no access to company-wide financials.
- **Profile & Support:** Provider profile; support tickets in provider context (e.g. “My tickets” or “Contact company support”).

### 5.3 Figma

- **Figma needed:** Provider dashboard (Client Portal) frames: sidebar with provider modules, overview with provider KPIs, listings table. If the design is “same as company dashboard but with provider nav labels,” we reuse company dashboard components; otherwise please provide the Provider Dashboard frames so we match.

### Deliverables

- [ ] ClientPortalLayout with provider-dashboard sidebar and header (company-controlled module list).
- [ ] Overview, Listings, Bookings, Staff (if applicable), Earnings—all provider-scoped and permission-aware.
- [ ] **Figma request:** Provider dashboard nav/sidebar and overview (or confirm reuse of company dashboard design).

---

## Phase 6: Cross-Cutting – Search, Tables, Permissions (Weeks 11–12)

**Goal:** Two search entry points (global search + search in deleted), robust data tables, permission-based visibility.

### 6.1 Search – two entry points (dashboard style)

- **1) Global search (⌘K or button):** For **Super Admin and managers**. Opens a modal/overlay: single input, type to search; show results for Bookings, Providers, Users (and optionally Services). Call backend search endpoint or aggregate existing APIs. Navigate to entity on select. Available in header or via ⌘K (Ctrl+K on Windows). **Audience:** G1 (Super Admin) + managers (roles that need to find anything).
- **2) “Search in deleted” (button, Super Admin only):** A **second button** in the dashboard header (or near global search), **visible only to G1 (Super Admin)**. Opens a similar modal that searches **only within deleted/archived records** (soft-deleted bookings, providers, users, etc.). Backend must expose an endpoint or param like `?includeDeleted=true` or `GET /api/v1/search/deleted?q=...`.
- **Design:** Build both modals in the same style as the dashboard (no separate Figma). Two clear actions: “Search” (global) and “Search in deleted” (G1 only).

### 6.2 Data tables

- **Server-side pagination:** For ledger, providers, tickets, audit logs: page size, next/prev, optional page number. Use API `params`: `page`, `size`.
- **Sort:** Sort by column (e.g. date, amount); pass `sort=date,desc` (or equivalent) to API.
- **Export:** Buttons “Export Excel” and “Export PDF” where applicable. Use backend export endpoint or client-side generation; document which in implementation.

### 6.3 Permission-based UI

- **Visibility:** From auth response or `GET /users/me` with permissions, hide or show: Commission column (pay:view_commission), Audit logs menu (sys:audit_read), Role management (sys:role_write). Apply to sidebar and to table columns.

### Deliverables

- [ ] **Global search** (⌘K or button) for Super Admin + managers; modal wired to backend or aggregated APIs.
- [ ] **“Search in deleted”** button (Super Admin only); modal wired to backend search over deleted/archived records.
- [ ] Pagination and sort on main tables.
- [ ] Export (Excel/PDF) where specified.
- [ ] Permission-based hiding for commission, audit, roles.

---

## Phase 7: i18n & RTL (Weeks 13–14)

**Goal:** Arabic (AR) and RTL layout for dashboard and portal.

### 7.1 i18n setup

- Add i18n library (e.g. react-i18next + i18next). Locale from user preference or browser; store in localStorage. Resource files: `en.json`, `ar.json` for dashboard and portal (shared keys where possible).
- Replace hardcoded strings with `t('key')` in Company Dashboard and Client Portal (priority: nav, buttons, form labels, table headers, toasts).

### 7.2 RTL

- When locale is `ar`, set `dir="rtl"` on root or main content; flip layout (sidebar right if needed, margins, flex order). Use CSS logical properties or RTL-aware Tailwind where possible.
- **Language switcher:** Use **Lucide icon** for the change-language control (e.g. `Languages` from `lucide-react`). Place in header or user/profile menu; clicking toggles EN ↔ AR or opens a small dropdown. No separate Figma icon needed.

### Deliverables

- [ ] en/ar bundles and t() in key screens.
- [ ] RTL layout for Arabic.
- [ ] Language switcher in header or user menu using **Lucide icon** (e.g. `Languages`).

---

## Phase 8: Polish & Optional (Week 15+)

**Goal:** Glassmorphism (if in design), micro-animations, real-time refresh, G1-only pages.

### 8.1 Optional UI polish

- **Glassmorphism:** If Figma uses frosted-glass sidebar/cards, add backdrop-blur and transparency to Sidebar and Card components.
- **Micro-animations:** Page transitions, hover reveals, pulsing health indicators. Low risk; can follow Figma or DASHBOARD_DESIGN_REPORT.

### 8.2 Optional features

- **Real-time:** WebSocket or short-interval polling for KPIs and activity feed (already partially in Phase 2).
- **G1-only:** DB Explorer (read-only schema/row counts) and Security Exception log (top 5) if backend exposes them.

### Deliverables

- [ ] Optional: glassmorphism and animations per design.
- [ ] Optional: DB Explorer, Security log for G1.

---

## Figma Request Summary (Ask When Phase Starts)

| Phase | Figma needed |
|-------|----------------|
| 1 | ✅ Theme toggle: use [lndev/ui Dark Mode Toggle](https://ui.lndev.me/components/dark-mode-toggle#code). |
| 2 | ✅ Provider detail = company dashboard; commission = standard input. |
| 3 | ✅ Refund modal: same dashboard style, build in code. |
| 4 | ✅ Priority badges: same style project-wide; Escalation button (optional). |
| 5 | ✅ Provider dashboard: same as company dashboard. |
| 6 | ✅ Global search + “Search in deleted” (G1 only): two buttons, build in dashboard style. |
| 7 | ✅ Language switcher: **Lucide icon** (e.g. `Languages`) in header or user menu. |

---

## Dependency Overview

- **Backend (Company Dashboard):** Auth, dashboard KPIs/activity, providers (list, commission), payments (list, refund), roles/permissions, audit logs, tickets, complaints must be available. Commission analysis and payout summary may need new endpoints.
- **Backend (Provider Dashboard):** Provider-scoped APIs: e.g. `GET /providers/me`, `GET /providers/me/services`, `GET /providers/me/bookings`, `GET /providers/me/earnings`, provider dashboard KPIs. All must filter by the authenticated provider’s `provider_id`; company controls exposure via G7 permissions.
- **Design:** Use current Figma; request specific components at the start of each phase as listed above.

---

*Plan version: 1.0. Tied to FRONTEND_GAP_REPORT.md and DASHBOARD_DESIGN_REPORT.md.*
