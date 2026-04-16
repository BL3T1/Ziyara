# Implementation Plan: Closing All Gaps (Backend, Frontend, Database)

This plan addresses **every missing or incomplete item** from [SYSTEM_STATE_REPORT.md](./SYSTEM_STATE_REPORT.md). Work is ordered by dependency and grouped into phases with deliverables and acceptance criteria.

**Reference:** SYSTEM_STATE_REPORT.md §2.2 (Backend gaps), §3.2 (Frontend gaps), §4.3 (DB gaps), §6 (Recommendations).

---

## Strategy Overview

| Principle | Approach |
|-----------|----------|
| **Canonical app** | Company Dashboard = **front/my-app** (Vite). Client Portal = same app under `/portal` with provider-scoped layout, OR separate build; CRA `frontend/` remains until migration complete, then deprecated. |
| **Database first** | Add payment columns and optional audit table without breaking existing schema; **table-prefix migration** is a separate, high-risk phase (Phase 2) with rollback plan. |
| **Backend modularity** | Introduce module packages and interface APIs incrementally; avoid big-bang rewrite. Payment gateway and webhook verification can be done before full modular refactor. |
| **Frontend** | Wire Vite to real API and auth (Phase 4); migrate dashboard and portal features from CRA into Vite phase by phase; i18n/RTL after core features. |

---

## Phase 0: Decisions & Baseline (Week 0)

**Goal:** Lock choices so later phases do not rework.

### 0.1 App assignment

- [ ] **Document:** "Company Dashboard = `front/my-app` (Vite). Client Portal = [Option A: under same app at `/portal` | Option B: separate SPA `front/portal-app`]."
- [ ] **Recommendation:** Option A (single app, two route trees) for one design system and shared auth; Option B if strict host split (providers.*) is required.
- [ ] **CRA:** Decide retention period: keep `frontend/` as reference until Phase 5 complete, then remove or archive.

### 0.2 Design & Figma

- [ ] Align design tokens with DASHBOARD_DESIGN_REPORT or current Figma (primary, secondary, spacing). Document in `front/my-app/PROJECT_CONTEXT.md` or design-tokens doc.
- [ ] Confirm Figma checklist (FIGMA_NEEDS_BY_PHASE.md) for theme toggle, search modal, language switcher, refund modal, provider dashboard—either provided or built in code per plan.

### 0.3 Payment gateway

- [ ] **Decision:** Select payment gateway provider (e.g. Stripe, Flutterwave, or Visa Direct partner). Document in PAYMENT_METHODS or tech spec.
- [ ] Obtain sandbox API keys and webhook signing secret for Phase 3.

### Deliverables

- [ ] One-page decision record: canonical app, portal option, CRA fate, gateway provider, design baseline.
- [ ] No code changes required; unblocks Phases 1–4.

---

## Phase 1: Database – Payment & Audit Extensions (Weeks 1–2)

**Goal:** Add missing payment and optional commission-audit columns without renaming tables. Safe, additive migrations.

### 1.1 Payment table extensions (PAYMENT_METHODS)

- [ ] **Migration 013:** Add to `payments` (if not present):
  - `gateway_reference` VARCHAR(255) – external transaction ID.
  - `three_ds_status` VARCHAR(50) – e.g. AUTHENTICATED, NOT_REQUIRED, FAILED.
  - `gateway_response` JSONB or TEXT – full response for debugging (optional).
- [ ] **Backend:** Update `PaymentJpaEntity`, `Payment` domain, DTOs and mappers to include new fields; expose in GET payment and transaction ref APIs.

### 1.2 Commission audit (optional, DYNAMIC_COMMISSION_REPORT)

- [ ] **Migration 014 (optional):** Create `provider_commission_audit` (or `pay_commission_audit` if preferring prefix-ready names):
  - `id` UUID, `provider_id` UUID, `old_rate` NUMERIC(5,2), `new_rate` NUMERIC(5,2), `changed_by` UUID, `changed_at` TIMESTAMPTZ, `reason` VARCHAR(255).
  - Use for audit trail of commission changes; backend service to append on PATCH `/providers/{id}/commission`.

### 1.3 Backend: Refund reason and audit

- [ ] Ensure `refunds` has `reason` (and optional `processed_by`); RefundRequest and RefundController require reason; audit log entry on refund (if not already).

### Deliverables

- [ ] Migrations 013 (and optionally 014) applied; backward compatible.
- [ ] Payment API returns gateway_reference and three_ds_status; refund API requires reason and writes audit.

### Acceptance criteria

- New payment columns exist; existing code still runs. Refund flow has mandatory reason and audit.

---

## Phase 2: Backend – Payment Gateway & Webhook Security (Weeks 3–5)

**Goal:** Implement Visa (or chosen) adapter and webhook signature verification (PAYMENT_METHODS).

### 2.1 Payment provider interface

- [x] Define `PaymentProvider` (or `PaymentGateway`) interface in `pay_` or `payment` package: `initiatePayment(...)`, `refund(...)`, optional `getStatus(...)`.
- [x] DTOs for tokenized payment request (no raw card data); response with redirect URL for 3DS, transaction ref, status.

### 2.2 VisaPaymentAdapter (or gateway-specific adapter)

- [x] Implement adapter using gateway SDK or HTTP client: tokenization flow, 3DS redirect URL, capture/authorize. *(Stub adapter implemented; Stripe/Visa adapter can be added with same interface.)*
- [x] Configuration: API base URL, API key, webhook secret from env/Secrets Manager.
- [x] **PCI:** Card capture only via gateway-hosted fields or SDK; backend never stores raw card data.

### 2.3 Webhook signature verification

- [x] In `PayWebhookController` (or equivalent): verify request body with HMAC/signature using webhook secret before processing.
- [x] Return 401/403 if verification fails; idempotent processing by `gateway_reference` or idempotency key to avoid duplicate application of callbacks.
- [x] Document header name and verification algorithm (e.g. Stripe-style `Stripe-Signature`) in PAYMENT_METHODS or code.

### 2.4 3DS and callback flow

- [ ] Frontend (booking/checkout): redirect to gateway 3DS URL when required; callback URL points to backend or frontend that then calls backend to confirm payment.
- [x] Backend endpoint for "confirm payment after 3DS" (e.g. POST `/payments/{id}/confirm` or webhook only) and update booking status.

### Deliverables

- [ ] `PaymentProvider` interface and one production-ready adapter (Visa or chosen gateway).
- [ ] Webhook endpoint verifies signature and processes success/failure idempotently.
- [ ] 3DS flow documented; sandbox-tested.

### Acceptance criteria

- Payment with card token and 3DS redirect works in sandbox; webhook with invalid signature is rejected.

---

## Phase 3: Backend – Modular Monolith Structure (Weeks 6–10)

**Goal:** Introduce module packages and interface-only cross-module calls (MODULAR_MONOLITH_STRUCTURE, MONOLITH_IMPLEMENTATION). This phase does **not** rename DB tables yet (that is Phase 4).

### 3.1 Package layout

- [x] Create package hierarchy under current root (`com.ziyara.backend`): `modules.payment.api` added; other modules (sys, hotel, booking, disc, support, core) to be added incrementally.
- [ ] Per module: `api` (public interfaces), `model`, `service`, `repository`, `web` (controllers), `events` (if any).

### 3.2 Module API interfaces

- [x] **sys:** `AuditServiceApi`, `RoleServiceApi` in `modules.sys.api`; `AuditLogService` and `RoleManagementService` implement them; controllers and payment use APIs only.
- [x] **booking:** `BookingServiceApi` in `modules.booking.api` with `getBooking(UUID)`; `application.service.BookingService` implements it.
- [x] **payment:** `PaymentServiceApi` in `modules.payment.api`; controllers depend on this interface; implementation remains in `application.service.PaymentService` (implements `PaymentServiceApi`).
- [x] **pricing:** `PricingEngineApi` in `core.api`; `PricingService` implements it; booking and pricing controllers use it.

### 3.3 Move code incrementally

- [ ] Move existing services/repositories into module packages (optional).
- [x] Replace direct cross-module service calls with interface calls; controllers use module APIs.
- [x] No cross-module JPA entity references in APIs; use UUIDs and DTOs.

### 3.4 ArchUnit tests

- [x] Add ArchUnit dependency (archunit-junit5 1.2.1).
- [x] Rule: `modules.*.service` must not be accessed from application, presentation, or infrastructure; only `modules.*.api` is allowed from outside the module.
- [x] Run in CI: ArchUnit runs with `mvn test` (ModuleBoundariesArchTest); include in any CI pipeline that runs tests.

### Deliverables

- [x] New package structure in place; existing tables unchanged.
- [x] Cross-module calls only via module API interfaces (payment, sys, booking, pricing).
- [x] ArchUnit tests passing.

### Acceptance criteria

- [x] No direct dependency from payment to booking.repository; booking uses `BookingServiceApi`/`PricingEngineApi`. ArchUnit enforces boundaries.

---

## Phase 4: Database – Table Prefix Migration (Weeks 11–14)

**Goal:** Rename tables to domain prefixes and remove cross-module FKs (MODULAR_MONOLITH_STRUCTURE). High risk; requires backup and rollback plan.

### 4.1 Mapping (current → prefixed)

- [ ] **sys_** – users, roles, permissions, groups, departments, user_roles, role_permissions, audit_logs, sessions, password_reset_tokens, otp_verification, i18n_labels.
- [ ] **hotel_** – services, service_images, service_providers (or keep as shared; decide per plan – e.g. `sys_providers` vs `hotel_providers`). Map per MODULAR_MONOLITH_STRUCTURE.
- [ ] **bkg_** – bookings, taxi_bookings.
- [ ] **pay_** – payments, refunds.
- [ ] **disc_** – discount_codes.
- [ ] **support_** or **sys_** – complaints, complaint_comments, internal_tickets, ticket_comments.
- [ ] **rest_** / **taxi_** – if separate tables exist for restaurants/taxis; otherwise leave under hotel_ or bkg_.

### 4.2 Migration strategy

- [ ] **Option A:** New migrations that `ALTER TABLE x RENAME TO prefix_x` and update sequences/indexes/FK names. Then update all JPA `@Table(name = "prefix_x")` and repository queries.
- [ ] **Option B:** Create new prefixed tables, copy data, switch application to new tables in a release, then drop old tables (allows rollback by pointing app back to old names).
- [ ] **FK removal:** Replace `REFERENCES other_table(id)` with plain UUID columns; enforce referential integrity in application code. Do in same migration or follow-up.

### 4.3 Migrations

- [ ] **Migration 015 (or 015–020):** Rename tables to prefixed names; update FKs to UUID-only where cross-module.
- [ ] **Backend:** Update every `@Table(name = "...")` and any native SQL/jOOQ to use new table names. Full regression test.

### Deliverables

- [ ] All tables renamed to sys_, hotel_, pay_, bkg_, disc_, etc. Cross-module FKs removed.
- [ ] Backend entities and queries updated; tests and smoke tests pass.

### Acceptance criteria

- No table without prefix in target set; no FK from pay_ to bkg_ in DB; app runs against new schema.

---

## Phase 5: Frontend – Canonical App Foundation (Weeks 15–17)

**Goal:** Make `front/my-app` the single Company Dashboard app with real auth, API client, theme, and three-tier build/deploy.

### 5.1 Auth and API in Vite

- [x] Replace mock `AuthContext` with real login: `POST /api/v1/auth/login`, store token (e.g. localStorage), set user (id, email, name, role/group). Logout, optional refresh token, 401 → redirect to login.
- [x] **API client:** `src/services/api.ts` (or similar): base URL from env, Axios/fetch with `Authorization: Bearer <token>`, 401 interceptor. Export `authAPI`, `dashboardAPI`, `providersAPI`, `rolesAPI`, `auditLogsAPI`, `paymentsAPI`, etc., matching backend.
- [x] **Role mapping:** Map backend role/group (G1–G6) to app role so sidebar and route guards use it.

### 5.2 Theme toggle and sidebar

- [x] Dark/light theme toggle in DashboardHeader; persist to localStorage; Tailwind dark mode via `class` strategy.
- [x] Sidebar collapsible to icon-only (slim bar) on small screens or via toggle (DASHBOARD_DESIGN_REPORT).

### 5.3 Three-tier deployment

- [ ] **Builds:** (1) Landing – if separate Next.js/SSG, or build of `frontend/` public routes only; (2) Company Dashboard = build of `front/my-app` (or subset of routes); (3) Client Portal = same app with base path `/portal` or separate build with only portal routes.
- [ ] **Docker/nginx:** `frontend-core` serves dashboard (and optionally landing); `frontend-portal` serves portal OR same build with different base path. Host-based routing: dashboard.* → dashboard app, providers.* → portal (INFRASTRUCTURE_REPORT).
- [ ] Document in INFRASTRUCTURE_REPORT or deploy runbook.

### Deliverables

- [x] Login in Vite uses real API; token and user in context; API client used by all data-fetching.
- [x] Theme toggle and collapsible sidebar in place.
- [ ] Build and deploy steps for dashboard and portal (two outputs or two entry points).

### Acceptance criteria

- User can log in to Vite app against real backend; dashboard and portal routes load; theme and sidebar work.

---

## Phase 6: Frontend – Dashboard Content (Weeks 18–22)

**Goal:** Replace placeholders with real pages: Global Overview, Provider Management, Financials, RBAC, Support, and role-specific G2–G6 content.

### 6.1 Global Overview and Management (group-first where specified)

- [x] **DashboardPage:** Real KPIs from `dashboardAPI.getKpis()`; activity feed from `dashboardAPI.getActivity()`; optional service health widget.
- [ ] **Users:** Group-first list: cards by role/group → select → table of users (Management > Users).
- [ ] **Providers:** Group-first by vertical or status → table; provider detail with commission override input; document verification section if API provides URLs.
- [ ] **Bookings:** Group-first by status or service type → table (Management > Bookings).
- [x] **Payments / Transaction ledger:** Group-first by status or method → table with columns: payment id, booking ref, amount, currency, method, status, 3DS status, gateway reference, date. Refund button → **Refund modal with mandatory reason**; call refund API; show success/error.
- [x] **Commission analysis:** Page or widget "Base Collected" vs "Ziyarah Delta" (chart or table) from dashboard commission-analysis API.
- [x] **Payout summary:** Widget or table from payout summary API if available.

### 6.2 RBAC and Audit

- [x] **Roles page:** List roles, create/edit role, permission matrix (resource:action toggles), delete with reassignment (ROLE_MANAGEMENT_REPORT). Port logic from CRA RoleManagementPage.
- [x] **Audit logs:** Search by user, IP, or text; table with user, action, entity, old/new value, timestamp. Restrict to G1 or by permission.

### 6.3 Support

- [x] **Ticket queue:** Group-first by priority or status → table; ticket detail with comments, resolve, **Escalate** (reassign to manager).
- [x] **Complaints:** Same pattern. Resolution dashboard: open by priority (doughnut), average resolution time, optional CSAT if API provides.

### 6.4 Role-specific dashboards (G2–G6)

- [x] **G2 Sales:** Dashboard with new provider applications count, hot verticals, leads; links to Leads Manager and Contract Viewer (or placeholders).
- [x] **G3 Finance:** Revenue vs commission, pending payouts, refund volume; links to Payout Ledger, Commission Controller, Tax & Compliance placeholder.
- [x] **G4 Support:** Open tickets by priority, avg resolution time, CSAT; links to Ticket Queue and Escalation Hub.
- [x] **G5 Executive:** YoY revenue, market share, commission per vertical; links to BI Reports and Global Approvals placeholders.
- [x] **G6 HR:** Staff activity, onboarding progress, employee status; Personnel Directory (group-first: roles → users table), Access Control (assign role to user).

### Deliverables

- [x] All Management and Admin pages in Vite use real API; Refund modal with reason; Commission analysis and Payout summary.
- [x] Role-specific dashboard pages for G2–G6 with real or placeholder widgets and links.

### Acceptance criteria

- Finance user sees commission analysis and can refund with reason; Support user sees ticket queue by priority; G2–G6 see correct dashboard and nav.

---

## Phase 7: Frontend – Search, Tables, Permissions (Weeks 23–24)

**Goal:** Global search (⌘K), Search in deleted (G1), server-side pagination/sort/export, permission-based visibility.

### 7.1 Search

- [ ] **Global search (⌘K or button):** Modal/command palette; single input; call backend search (aggregate bookings, providers, users) or existing list APIs with query; navigate to entity on select. Available to G1 and managers.
- [ ] **Search in deleted (G1 only):** Second button in header; modal that searches only deleted/archived records; backend `?includeDeleted=true` or dedicated endpoint.

### 7.2 Data tables

- [ ] Server-side pagination (`page`, `size`) for ledger, providers, tickets, audit logs, bookings, users.
- [ ] Sort by column (e.g. date, amount); pass `sort=field,asc|desc` to API.
- [ ] Export: "Export Excel" and "Export PDF" where specified; use backend export endpoint or client-side generation; document which.

### 7.3 Permission-based visibility

- [ ] From auth or `GET /users/me` with permissions: hide Commission column and commission analysis for users without `pay:view_commission`; hide Audit logs menu for users without `sys:audit_read`; hide Role management for users without `sys:role_write`. Apply to sidebar and table columns.

### Deliverables

- [ ] Global search and Search in deleted wired; main tables have pagination and sort; export where specified; sidebar and columns respect permissions.

### Acceptance criteria

- G1 can use both searches; Finance without pay:view_commission does not see commission data; tables paginate and sort via API.

---

## Phase 8: Frontend – i18n & RTL (Weeks 25–26)

**Goal:** Arabic (AR) and RTL for dashboard and portal (REQUIREMENTS_ANALYSIS, implementation_plan Phase 4).

### 8.1 i18n setup

- [ ] Add react-i18next + i18next (or equivalent). Locale from user preference or browser; store in localStorage. Resource files: `en.json`, `ar.json` for dashboard and portal.
- [ ] Replace hardcoded strings with `t('key')` in nav, buttons, form labels, table headers, toasts (priority screens first).

### 8.2 RTL

- [ ] When locale is `ar`, set `dir="rtl"` on root or main content; flip layout (sidebar right if needed, margins, flex order). Use CSS logical properties or RTL-aware Tailwind.
- [ ] **Language switcher:** Lucide `Languages` icon in header or user menu; toggle EN ↔ AR.

### Deliverables

- [ ] en/ar bundles; key screens translated; RTL layout for Arabic; language switcher visible.

### Acceptance criteria

- User can switch to AR; layout flips to RTL; critical labels and buttons show in Arabic.

---

## Phase 9: Frontend – Client Portal Completion (Weeks 27–28)

**Goal:** Full provider dashboard in chosen app (Vite): provider-scoped Overview, Listings, Bookings, Staff, Earnings, Profile, Support (FRONTEND_IMPLEMENTATION_PLAN Phase 5).

### 9.1 Portal layout and routes

- [ ] **ClientPortalLayout** in Vite (or ensure existing one is complete): sidebar with Overview, Listings, Bookings, Staff, Earnings, Profile, Support; header with breadcrumbs, notifications, avatar. Role-based nav (e.g. PROVIDER_STAFF sees reduced set).
- [ ] Routes: `/portal`, `/portal/listings`, `/portal/bookings`, `/portal/staff`, `/portal/earnings`, `/portal/profile`, `/portal/support`. All data from provider-scoped APIs (`/providers/me/*` or equivalent).

### 9.2 Portal modules

- [ ] **Overview:** Provider KPIs (upcoming bookings, revenue in period, pending tasks) from provider dashboard API.
- [ ] **Listings:** CRUD for provider’s services (backend enforces provider_id).
- [ ] **Bookings:** Table of bookings for their services; check-in/out actions for staff role if applicable.
- [ ] **Staff:** List and manage provider sub-users (if backend supports).
- [ ] **Earnings:** Revenue, commission deducted, payouts (read-only as per company policy).
- [ ] **Profile & Support:** Provider profile; support tickets in provider context.

### Deliverables

- [ ] Portal layout and all portal pages in Vite wired to provider-scoped APIs; role-based nav within portal.

### Acceptance criteria

- Provider user logs in and sees only their listings, bookings, earnings; Staff sees reduced nav.

---

## Phase 10: Backend – Domain Events & Optional Features (Weeks 29–30)

**Goal:** Domain events for cross-module consistency; optional reconciliation and fraud checks (MONOLITH_IMPLEMENTATION, PAYMENT_METHODS).

### 10.1 Domain events

- [ ] Define events: e.g. `BookingCreatedEvent`, `PaymentCompletedEvent`, `RefundProcessedEvent`. Publish from respective modules after state change.
- [ ] Listeners in other modules (e.g. notification, audit) react without direct service calls. Use Spring `ApplicationEventPublisher` or Kafka if already in stack.

### 10.2 Reconciliation and fraud (optional)

- [ ] **Nightly reconciliation:** Job that compares `pay_*` (or `payments`) with gateway transaction list; log discrepancies.
- [ ] **Velocity check:** Same card/identifier within 15 minutes – flag or block if configured (PAYMENT_METHODS).

### Deliverables

- [ ] At least one cross-module flow (e.g. booking → payment → notification) uses domain events.
- [ ] Optional: reconciliation job and velocity check documented or implemented.

### Acceptance criteria

- Booking confirmation triggers notification via event; no direct call from booking to notification module.

---

## Phase 11: Frontend – Polish (Week 31+)

**Goal:** Optional UI polish and G1-only features (DASHBOARD_DESIGN_REPORT, FRONTEND_IMPLEMENTATION_PLAN Phase 8).

### 11.1 Optional UI

- [ ] **Glassmorphism:** Frosted-glass sidebar/cards (backdrop-blur, transparency) if in design.
- [ ] **Micro-animations:** Page transitions, hover reveals, pulsing health indicators.
- [ ] **Design tokens:** Align with DASHBOARD_DESIGN_REPORT palette (Royal Blue #1A237E, Success/Warning/Danger) if adopting.

### 11.2 G1-only (optional)

- [ ] **DB Explorer:** Read-only schema metadata and table row counts if backend exposes.
- [ ] **Security exception log:** Top 5 security exceptions widget for G1 if backend provides.

### Deliverables

- [ ] Optional polish applied; G1-only widgets if backend ready.

---

## Summary: Phase → Gaps Addressed

| Phase | Backend | Frontend | Database |
|-------|---------|----------|----------|
| 0 | — | — | — |
| 1 | Refund reason/audit | — | 3DS/gateway columns; optional commission audit table |
| 2 | VisaPaymentAdapter; webhook verification | — | — |
| 3 | Modular package layout; interface APIs; ArchUnit | — | — |
| 4 | Entity/query updates | — | Table renames to prefixes; FK removal |
| 5 | — | Real auth & API in Vite; theme; sidebar; three-tier deploy | — |
| 6 | — | Financials UI; G2–G6 dashboards; Support; RBAC/Audit | — |
| 7 | — | Global search; Search deleted; pagination/sort/export; permission visibility | — |
| 8 | — | i18n & RTL | — |
| 9 | — | Client Portal full (Vite) | — |
| 10 | Domain events; reconciliation; fraud | — | — |
| 11 | — | Polish; G1-only widgets | — |

---

## Dependency Graph (high level)

```
Phase 0 → Phase 1, 2, 5
Phase 1 → Phase 2
Phase 2 → (standalone)
Phase 3 → Phase 4
Phase 4 → (backend complete for modular schema)
Phase 5 → Phase 6, 7, 8, 9
Phase 6, 7 → Phase 8 (can overlap)
Phase 8 → Phase 9 (can overlap)
Phase 10, 11 → anytime after 3–4 and 6–7
```

---

## Risk and Rollback

- **Phase 4 (table renames):** High impact. Require DB backup, feature flag or dual-write if needed, and rollback script (rename back to old names).
- **Phase 3 (modular layout):** Medium. Do incrementally; keep tests green each step; revert commits if boundary violations are too costly.
- **Phase 5 (canonical app):** If Vite is chosen, keep CRA runnable until Vite has parity for critical paths.

---

## Traceability: Report Gap → Phase

Every gap from SYSTEM_STATE_REPORT.md is addressed as follows.

| Report § | Gap | Phase |
|----------|-----|-------|
| 2.2 Backend | Modular monolith package structure | 3 |
| 2.2 Backend | Table prefixes (sys_, hotel_, pay_, …) | 4 |
| 2.2 Backend | Interface-only cross-module calls | 3 |
| 2.2 Backend | VisaPaymentAdapter / gateway implementation | 2 |
| 2.2 Backend | Webhook signature verification | 2 |
| 2.2 Backend | ArchUnit (module boundaries, prefix compliance) | 3 |
| 2.2 Backend | Domain events for cross-module consistency | 10 |
| 2.2 Backend | PricingEngine as dedicated module | 3 (optional in core) |
| 2.2 Backend | Reconciliation / fraud (velocity checks) | 10 |
| 3.2 Frontend | Single canonical dashboard app | 0 (decision), 5 (execute) |
| 3.2 Frontend | Real auth & API in Vite app | 5 |
| 3.2 Frontend | Theme toggle in Vite | 5 |
| 3.2 Frontend | Role-specific content G2–G6 | 6 |
| 3.2 Frontend | Financials UI (commission analysis, refund reason, payout) | 6 |
| 3.2 Frontend | Support (queue by priority, resolution, escalation) | 6 |
| 3.2 Frontend | Global search (⌘K) | 7 |
| 3.2 Frontend | Search in deleted (G1 only) | 7 |
| 3.2 Frontend | Server-side pagination, sort, Excel/PDF export | 7 |
| 3.2 Frontend | Permission-based visibility | 7 |
| 3.2 Frontend | i18n & RTL (AR) | 8 |
| 3.2 Frontend | Three-tier deployment | 5 |
| 3.2 Frontend | Glassmorphism, design tokens | 11 |
| 4.3 Database | Rename tables to prefixed | 4 |
| 4.3 Database | Remove cross-module FKs | 4 |
| 4.3 Database | provider_commission / pay_ audit table | 1 (optional 014) |
| 4.3 Database | 3DS / gateway reference columns | 1 |

---

*Plan version 1.0. Tied to SYSTEM_STATE_REPORT.md and source plans (MODULAR_MONOLITH_STRUCTURE, PAYMENT_METHODS, FRONTEND_IMPLEMENTATION_PLAN, etc.).*
