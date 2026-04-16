# Frontend Gap Report: Ziyarah Company Dashboard & Client Portal

**Purpose:** Compare the current frontend implementation against the plan documents and requirements to identify what is implemented and what is missing for the **Company Dashboard** (G1–G6) and **Client Portal** (G7).

**Reference documents used:**
- `plans/DASHBOARD_DESIGN_REPORT.md`
- `plans/DYNAMIC_COMMISSION_REPORT.md`
- `plans/implementation_plan.md`
- `plans/INFRASTRUCTURE_REPORT.md`
- `plans/MODULAR_MONOLITH_STRUCTURE.md`
- `plans/MONOLITH_IMPLEMENTATION.md`
- `plans/PAYMENT_METHODS.md`
- `plans/PRICING_METHODS.md`
- `plans/REQUIREMENTS_ANALYSIS.md`
- `plans/ROLE_MANAGEMENT_REPORT.md`
- `plans/SYSTEM_EVOLUTION_REPORT.md`
- `docs/SyRS-Ziyarah.tex` (partial)
- External refs (not parsed): *Ziyarah APP.pdf*, *Ziyarah Roles.pdf*, *Dashboard for ZIYARA .pdf*

**Date:** 2026-03-10

---

## 1. Executive Summary

| Area | Status | Summary |
|------|--------|--------|
| **Company Dashboard** | Partial | Two codebases exist. The CRA `frontend/` has role dashboards, RBAC UI, provider commission, audit logs, and API integration; the Vite `front/my-app/` has a modern shell and placeholders. Many dashboard modules (Financials, Support queue, role-specific views) are missing or placeholder. |
| **Client Portal** | Broken / Missing | Routes and imports reference `ClientPortalLayout` and `ClientPortalOverview`, but **these files do not exist** in the repo. Portal routes will fail. Provider-scoped UI is not implemented as a separate app. |
| **Design & UX** | Partial | Dark/light theme and search UI exist in CRA; glassmorphism, ⌘K global search, and design tokens from DASHBOARD_DESIGN_REPORT are only partly reflected. |
| **RBAC & Roles** | Partial | G1–G6 role dashboards and role-based sidebar exist; permission matrix UI and group-level (G1–G7) alignment with ROLE_MANAGEMENT_REPORT are incomplete. |
| **i18n / RTL** | Missing | No Arabic or RTL support found in the frontend. |

---

## 2. Frontend Codebase Overview

The repository contains **two** frontend applications. The infrastructure plan calls for **three** interfaces (Landing, Company Dashboard, Client Portal), with the latter two as separate SPAs.

| App | Path | Stack | Intended Use |
|-----|------|--------|--------------|
| **Legacy / Unified** | `frontend/` | CRA, React 18, JavaScript, Axios | Single app mixing landing, dashboard, and portal routes |
| **New Dashboard Shell** | `front/my-app/` | Vite 7, React 19, TypeScript, Tailwind v4 | Company dashboard UI shell; most routes are placeholders |

### 2.1 `frontend/` (CRA)

- **Routing:** Public (Home, Hotels, Restaurants, Trips, Service detail), Auth (Login, Register, Forgot password), Role selection, **Company dashboards** (Super Admin, Sales, Finance, Support, Executive, HR), Admin (Roles, Providers, Payments), Booking flow, **Client portal** routes (Portal, My Bookings, Profile, Support, Tickets).
- **API layer:** Central `api.js` with auth, user, service, booking, payment, complaint, ticket, review, discount, notification, **pricing**, **exchange rate**, **audit logs**, **roles**, **dashboard** (KPIs, activity), **providers** (including `updateCommission`), payments list/getByRef.
- **Auth:** `AuthContext` with token in localStorage; interceptors for Bearer token and 401 redirect.
- **Role dashboards:** SuperAdminDashboard uses real API (audit logs, dashboard KPIs, activity feed); others (Sales, Finance, Support, Executive, HR) are thin wrappers or placeholders.
- **Implemented pages:** RoleManagementPage (roles CRUD, permission catalogue, groups), ProvidersPage (list, filter, **commission override**), PaymentsPage; SupportPage, TicketDetailPage, CreateTicketPage; booking and service discovery pages.

### 2.2 `front/my-app/` (Vite + TS)

- **Routing:** Role select, Login, then under auth: Dashboard, Sales, Analytics, Services (Hotels/Restaurants/Taxis/Trips), Management (Users, Providers, Bookings, Payments, Discounts), Support (Complaints, Tickets, Chat), Admin (Settings, Roles, Logs, API). All except Dashboard and Sales are **PlaceholderPage**.
- **Auth:** Mock only (`AuthContext` with local state; no API login).
- **Roles:** `super_admin | admin | finance | support | provider | user` (no G1–G7 naming; no `executive`/`hr`).
- **Dashboard:** DashboardPage shows mock stat cards; SalesDashboardPage shows mock Revenue by Service, Bookings by Region, and Recent Bookings table with local search/filter.
- **Design:** Tailwind; primary/secondary colors; Sidebar, DashboardHeader, breadcrumbs, role-based sidebar sections.

---

## 3. Company Dashboard: Implemented vs Required

Requirements are derived from **DASHBOARD_DESIGN_REPORT.md**, **ROLE_MANAGEMENT_REPORT.md**, **REQUIREMENTS_ANALYSIS.md**, and **SyRS**.

### 3.1 Design Aesthetics & UX (DASHBOARD_DESIGN_REPORT §1)

| Requirement | frontend/ (CRA) | front/my-app/ |
|-------------|-----------------|----------------|
| Sleek dark mode (primary) + light toggle | ✅ Theme toggle in DashboardHeader | ❌ No theme toggle |
| Glassmorphism (sidebar, cards) | ❌ | ❌ |
| Micro-animations, transitions | Partial (CSS) | Partial |
| Typography (e.g. Inter/Outfit) | Inter used | Inter in PROJECT_CONTEXT |
| Color palette (Primary Royal Blue/Violet, Success, Warning, Danger) | Partial (custom CSS vars) | Primary #1e4d6b, secondary #ac9e78 |

### 3.2 Page Hierarchy & Modules (DASHBOARD_DESIGN_REPORT §2)

| Module | Required | CRA frontend | front/my-app |
|--------|----------|-------------|-------------|
| **Global Overview (Command Center)** | KPI metrics, live activity feed, service health mini-charts | ✅ SuperAdmin: KPIs + activity feed from API; health metrics mock | ✅ Dashboard: mock stats; Sales: charts + table (mock) |
| **Provider Management** | Listing (vertical, status, commission), details, commission override, document verification gallery | ✅ ProvidersPage: list, status/type filter, commission edit | ❌ Placeholder only |
| **Financials & Payments** | Transaction ledger (3DS, gateway refs), commission analysis, refund (reason + audit), payout summary | Partial: PaymentsPage; no commission breakdown UI, no refund reason/audit UI | ❌ Placeholder |
| **RBAC & Security** | Role Architect, permissions matrix (resource:action), audit logs (user/IP/old–new) | ✅ RoleManagementPage (roles, permissions, groups); SuperAdmin audit table + search | ❌ Roles placeholder |
| **Customer Support** | Ticket queue (priority), conversation portal, resolution stats | Partial: SupportPage, TicketDetail, CreateTicket; no priority queue view, no resolution dashboard | ❌ Placeholders |

### 3.3 Role-Specific Dashboard Views (G1–G6) (DASHBOARD_DESIGN_REPORT §3)

| Group | Required View | CRA | front/my-app |
|-------|----------------|-----|--------------|
| **G1 Super Admin** | System health, Role Architect, Global Audit, DB Explorer | ✅ SuperAdminDashboard (KPIs, activity, audit, health mock); Role Management page | ❌ No G1-specific view; Roles placeholder |
| **G2 Sales** | Provider pipeline, new applications, hot verticals, leads, contract viewer | Placeholder dashboard | Sales route has mock sales dashboard only |
| **G3 Finance** | Revenue vs commission, pending payouts, refund volume, Payout Ledger, Commission Controller, Tax & Compliance | FinanceDashboard placeholder | ❌ |
| **G4 Support** | Ticket/SLA monitor, open by priority, resolution time, CSAT; Ticket Queue, Escalation Hub | SupportDashboard + SupportPage; no SLA/CSAT | ❌ Placeholders |
| **G5 Executive** | YoY revenue, market share, commission per vertical; BI Reports, Global Approvals | ExecutiveDashboard placeholder | ❌ |
| **G6 HR** | Staff activity, onboarding, personnel directory, access control | HRDashboard placeholder | ❌ |

### 3.4 UI Layout (DASHBOARD_DESIGN_REPORT §4)

| Element | Required | CRA | front/my-app |
|---------|----------|-----|--------------|
| Static sidebar (left), collapse to icons | ✅ DashboardLayout + Sidebar | ✅ Sidebar, no collapse to slim |
| Breadcrumb (top) | ✅ | ✅ PageLayout + DashboardHeader |
| Search bar (⌘K) | Placeholder "Search (⌘K)" in header; no global search behavior | ❌ |
| Notification bell + badges | ✅ In header | ✅ |
| User profile (avatar + role badge) | ✅ | ✅ Avatar + role label |
| Theme toggle | ✅ | ❌ |

### 3.5 Technical UI Features (DASHBOARD_DESIGN_REPORT §5)

| Feature | CRA | front/my-app |
|---------|-----|--------------|
| Server-side pagination, multi-column sort, Excel/PDF export | Partial (tables exist; export not verified) | Sales table: client-side filter only |
| Global Search (⌘K) for Booking/Provider/User | ❌ | ❌ |
| Real-time sync (webhooks/polling) | ❌ | ❌ |

---

## 4. Client Portal: Implemented vs Required

**INFRASTRUCTURE_REPORT** and **MONOLITH_IMPLEMENTATION**: Client Portal (`providers.ziyarah.com`) is a separate React SPA for **G7 Service Providers**, with provider-scoped data.

| Requirement | Status | Notes |
|-------------|--------|--------|
| Dedicated SPA for G7 | ❌ | No separate app; portal routes live in CRA under `/portal`, `/my-bookings`, etc. |
| ClientPortalLayout | ❌ **Missing** | `frontend/src/layouts/ClientPortalLayout.js` **does not exist**; imported in App.js. |
| ClientPortalOverview | ❌ **Missing** | `frontend/src/pages/portal/ClientPortalOverview.js` **does not exist**; imported in App.js. |
| Provider-scoped data (all queries by provider_id) | N/A | Backend would enforce; frontend has no dedicated provider portal shell. |
| Provider features: listings, staff, bookings, earnings | Partial | My Bookings, Profile, Support/Tickets exist as shared pages; no provider-specific “listings management” or “earnings” UI. |

**Critical:** Any navigation to `/portal`, `/my-bookings`, `/profile`, or support routes that expect `ClientPortalLayout` will fail at runtime due to missing components.

---

## 5. Alignment with Other Plan Documents

### 5.1 DYNAMIC_COMMISSION_REPORT & PRICING_METHODS

- **Commission override:** ✅ CRA ProvidersPage allows editing provider commission; API `providersAPI.updateCommission` exists.
- **Pricing preview:** ✅ `pricingAPI.preview` in api.js.
- **Commission analysis UI (Base vs Ziyarah Delta):** ❌ Not present in Financials/Payments UI.

### 5.2 ROLE_MANAGEMENT_REPORT

- **Role catalogue (G1–G7):** CRA has 6 role dashboards (Super Admin, Sales, Finance, Support, Executive, HR); front/my-app uses 6 roles (no explicit G7 “provider” dashboard as separate portal).
- **Create/Modify roles, permission matrix:** ✅ CRA RoleManagementPage: create role, permission catalogue, groups; update permissions; delete with reassignment.
- **Restricted permissions (e.g. pay:view_commission):** Not visibly enforced in UI (e.g. Finance sees Payments but commission visibility not scoped by permission).

### 5.3 PAYMENT_METHODS & Financial Controls

- **Transaction ledger, 3DS status, gateway refs:** PaymentsPage exists; level of detail not verified.
- **Refund with mandatory reason and audit:** paymentAPI.refund exists; no confirmation of “reason” field or audit trail in UI.
- **Secure payment UI / 3DS redirect:** Not assessed (checkout flow in booking).

### 5.4 REQUIREMENTS_ANALYSIS & SyRS

- **RBAC with granular permissions:** Backend-oriented; frontend shows role-based dashboards and role-based sidebar, not full resource:action enforcement per widget.
- **Multilingual (EN/AR) and RTL:** ❌ No i18n or RTL implementation found.
- **Dashboard/Analytics API usage:** CRA uses dashboard KPIs and activity; other dashboard endpoints (revenue, bookings, customers, providers) are in api.js but not all wired to role views.

### 5.5 INFRASTRUCTURE_REPORT (Three-Tier Frontend)

- **Landing (ziyarah.com):** CRA HomePage + services act as landing; not a separate Next.js/SSG app.
- **Company Dashboard (dashboard.*):** Implemented inside CRA and partially in front/my-app; not deployed as a separate SPA on dashboard.*.
- **Client Portal (providers.*):** Not a separate SPA; portal routes in CRA and layout/overview files missing.

### 5.6 implementation_plan (Phase 4 – Frontend)

- **Three-tier deployment (Landing, Back-Office Dashboard, Client Portal):** Only partially reflected; one CRA app contains both dashboard and portal routes, and portal layout is missing.
- **Back-Office Dashboard “enhanced React” for G1–G6:** Partially done in CRA; front/my-app is a modern shell with placeholders.

---

## 6. Gap Summary Tables

### 6.1 Company Dashboard – Missing or Incomplete

| Gap | Priority | Document |
|-----|----------|----------|
| Role-specific content for G2–G6 (Sales pipeline, Finance treasury, Support SLA, Executive BI, HR personnel) | High | DASHBOARD_DESIGN_REPORT |
| Financials: Commission analysis (Base vs Ziyarah Delta), refund reason + audit, payout summary | High | DASHBOARD_DESIGN_REPORT, DYNAMIC_COMMISSION |
| Support: Ticket queue by priority, resolution dashboard, escalation hub | Medium | DASHBOARD_DESIGN_REPORT |
| Global Search (⌘K) for Booking/Provider/User | Medium | DASHBOARD_DESIGN_REPORT |
| Real-time updates (polling or webhooks) for KPIs/activity | Medium | DASHBOARD_DESIGN_REPORT |
| Data tables: server-side pagination, sort, Excel/PDF export | Medium | DASHBOARD_DESIGN_REPORT |
| Glassmorphism, design token alignment (e.g. Royal Blue/Violet) | Low | DASHBOARD_DESIGN_REPORT |
| Theme toggle in front/my-app | Low | DASHBOARD_DESIGN_REPORT |
| DB Explorer (G1), Tax & Compliance, Contract Viewer (G2) | Low | DASHBOARD_DESIGN_REPORT |

### 6.2 Client Portal – Missing or Incomplete

| Gap | Priority | Document |
|-----|----------|----------|
| Create `ClientPortalLayout.js` and `ClientPortalOverview.js` (or remove broken routes) | **Critical** | App.js imports |
| Separate Client Portal SPA (or clear separation under one app) for G7 | High | INFRASTRUCTURE_REPORT |
| Provider-specific: listings management, staff, earnings view | High | REQUIREMENTS_ANALYSIS, INFRASTRUCTURE |
| Provider-scoped navigation and branding for portal | Medium | INFRASTRUCTURE_REPORT |

### 6.3 Cross-Cutting

| Gap | Priority | Document |
|-----|----------|----------|
| Arabic (AR) + RTL support | High | REQUIREMENTS_ANALYSIS, implementation_plan |
| Permission-driven UI (hide commission/payments by permission) | Medium | ROLE_MANAGEMENT_REPORT |
| Consolidate or choose dashboard app (CRA vs Vite) and align routes with backend | High | All |

---

## 7. Recommendations

1. **Fix Client Portal imports:** Either add `ClientPortalLayout.js` and `ClientPortalOverview.js` under `frontend/src` or remove/redirect portal routes so the app does not reference missing files.
2. **Define single Company Dashboard app:** Choose either `frontend/` or `front/my-app/` as the canonical dashboard; migrate API integration and role-specific screens into it, and align routes with backend (e.g. `/api/v1/dashboard/*`, `/api/v1/roles/*`, etc.).
3. **Implement G2–G6 dashboard content:** Add real data and views for Sales (pipeline, leads), Finance (revenue, payouts, refunds, commission breakdown), Support (ticket queue by priority, resolution stats), Executive (BI, approvals), HR (personnel, access control).
4. **Add Financials UI:** Transaction ledger with 3DS/gateway info, commission analysis (Base vs Ziyarah Delta), refund flow with mandatory reason and audit, and payout summary.
5. **Implement global search (⌘K):** Backend endpoint for searching bookings/providers/users and wire header search to it.
6. **Introduce i18n and RTL:** Per REQUIREMENTS_ANALYSIS and implementation_plan; at least Arabic and RTL layout for dashboard and portal.
7. **Enforce permission-based visibility:** Use backend permission set to show/hide commission data, payment details, and sensitive admin sections (e.g. pay:view_commission, sys:audit_read).
8. **Client Portal as dedicated experience:** Either a separate SPA for `providers.*` or a clearly separated “portal” area with provider-scoped navigation, listings, staff, bookings, and earnings.

---

*Report generated from plans and codebase inspection. External PDFs (Ziyarah APP, Ziyarah Roles, Dashboard for ZIYARA) were referenced but not parsed.*
