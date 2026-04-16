# Ziyarah System State Report: Backend, Frontend & Database Migrations

**Purpose:** Single report on the current state of the Ziyarah platform against plans and requirements, identifying what is implemented and what is missing across **backend**, **frontend**, and **database migrations**.

**Sources analyzed:**
- plans: DASHBOARD_DESIGN_REPORT, DYNAMIC_COMMISSION_REPORT, FIGMA_NEEDS_BY_PHASE, FRONTEND_GAP_REPORT, FRONTEND_IMPLEMENTATION_PLAN, implementation_plan, INFRASTRUCTURE_REPORT, MODULAR_MONOLITH_STRUCTURE, MONOLITH_IMPLEMENTATION, PAYMENT_METHODS, PRICING_METHODS, REQUIREMENTS_ANALYSIS, ROLE_MANAGEMENT_REPORT, SYSTEM_EVOLUTION_REPORT
- docs: SyRS-Ziyarah.tex (System Requirements Specification)
- External refs (not parsed): Ziyarah APP.pdf, Ziyarah Roles.pdf, Dashboard for ZIYARA .pdf

**Date:** 2026-03-16

---

## 1. Executive Summary

| Layer | Status | Summary |
|-------|--------|--------|
| **Backend** | Partial | Core APIs (auth, users, providers, bookings, payments, complaints, tickets, discounts, reviews, dashboard, pricing, refunds, audit) exist. Pricing/commission logic and idempotency/webhook stubs are in place. **Missing:** Modular monolith package layout, table-prefix migration (sys_/hotel_/pay_/bkg_), Visa adapter implementation, ArchUnit boundaries, dedicated PricingEngine module. |
| **Frontend** | Partial | Two UIs: CRA (`frontend/`) with portal (PortalLayout, PortalOverviewPage, dashboard, RBAC, providers commission) and Vite (`front/my-app/`) with shell and placeholders. CRA has been reorganized (app/portal, app/dashboard). **Missing:** Single canonical dashboard app, full G2–G6 role content, global search (⌘K), search-in-deleted, i18n/RTL, Financials commission analysis UI, deployment split (dashboard.* / providers.*). |
| **Database migrations** | Partial | 12 migrations: i18n_labels, RBAC extensions (roles, permissions), commission_rate, idempotency_key, _ar columns, auth tokens/OTP, entity columns. **Missing:** Table renames to domain prefixes (sys_, hotel_, pay_, bkg_, disc_, rest_, taxi_), no cross-module FK removal (still using references). |

---

## 2. Backend State

### 2.1 Implemented (aligned with plans)

| Area | Implementation | Plan reference |
|------|----------------|----------------|
| **Auth** | Login, register, OTP send/verify, password reset, JWT, sessions | REQUIREMENTS_ANALYSIS, SyRS |
| **RBAC** | Roles, permissions, groups, user_roles, role_permissions; role status, is_system_role, is_locked | ROLE_MANAGEMENT_REPORT, 002_role_management_report.sql |
| **Providers** | CRUD, approve, suspend; **commission override** PATCH `/{id}/commission` | DYNAMIC_COMMISSION_REPORT, PRICING_METHODS |
| **Pricing** | `PricingService`: provider/company discounts, default 10% commission, provider override, tax, multi-currency | DYNAMIC_COMMISSION_REPORT, PRICING_METHODS |
| **Payments** | Process, refund, get by ref; **idempotency_key** in DB and API | PAYMENT_METHODS, 003_pricing_and_payment_methods.sql |
| **Webhooks** | `PayWebhookController` at `/pay/webhooks` (signature verification noted for production) | PAYMENT_METHODS |
| **Dashboard** | KPIs, activity feed, commission-analysis, payout summary endpoints | DASHBOARD_DESIGN_REPORT, REQUIREMENTS_ANALYSIS |
| **Audit** | Audit logs entity and API | ROLE_MANAGEMENT_REPORT, SyRS |
| **Bookings** | CRUD, cancel, confirm, voucher, taxi add-on; commission_amount on booking | SyRS, PRICING_METHODS |
| **Complaints & tickets** | Complaints and internal tickets with comments, assign, resolve, escalate | SyRS, DASHBOARD_DESIGN_REPORT |
| **Portal API** | Provider-scoped bookings/earnings (e.g. portal service) | INFRASTRUCTURE_REPORT, FRONTEND_IMPLEMENTATION_PLAN |

### 2.2 Missing or incomplete (backend)

| Gap | Priority | Plan reference |
|-----|----------|----------------|
| **Modular monolith package structure** | High | MODULAR_MONOLITH_STRUCTURE, MONOLITH_IMPLEMENTATION, implementation_plan Phase 1 |
| **Table prefixes** | High | All tables still flat (users, roles, bookings, payments). Plans require sys_, hotel_, pay_, bkg_, disc_, rest_, taxi_. |
| **Interface-only cross-module calls** | High | Direct service/repository use; no strict module API layer (e.g. com.ziyarah.modules.hotel.api). |
| **VisaPaymentAdapter / gateway implementation** | High | PAYMENT_METHODS: tokenization, 3DS, production gateway integration; webhook is stub. |
| **Webhook signature verification** | High | PAYMENT_METHODS: HMAC verification of inbound webhooks not implemented. |
| **ArchUnit (module boundaries, prefix compliance)** | Medium | implementation_plan verification. |
| **Domain events for cross-module consistency** | Medium | MONOLITH_IMPLEMENTATION, SYSTEM_EVOLUTION_REPORT: use events instead of cross-module JOINs/transactions. |
| **PricingEngine as dedicated module** | Low | MONOLITH_IMPLEMENTATION: logic exists in PricingService but not as a separate module with public API. |
| **Reconciliation / fraud (velocity checks)** | Low | PAYMENT_METHODS: nightly reconciliation, 15-min velocity checks. |

---

## 3. Frontend State

### 3.1 Implemented

| Area | Location | Notes |
|------|----------|------|
| **Company dashboard (CRA)** | frontend/src/app/dashboard | RoleSelection, DashboardHome, Users, Providers, ProviderDetail (commission), Bookings, Payments, Discounts, Reports, RoleManagement, Settings, Api, Support (tickets). |
| **Portal (CRA)** | frontend/src/app/portal | PortalLayout, PortalOverviewPage, Listings, Bookings, Earnings, Profile, Support. |
| **Public & browsing (CRA)** | frontend/src/app/public, app/browsing | Landing, auth, Hotels/Restaurants/Trips, ServiceDetail, Booking, BookingConfirmation. |
| **Customer (CRA)** | frontend/src/app/customer | MyBookings, Profile, Support, TicketDetail, CreateTicket. |
| **Dashboard shell (Vite)** | front/my-app | Sidebar, DashboardHeader, PageLayout, RoleSelect, Login, DashboardPage (mock), SalesDashboardPage (mock), PlaceholderPage for most routes. |
| **Portal in Vite** | front/my-app | ClientPortalLayout, ClientPortalOverview, PortalPlaceholderPage. |
| **API integration (CRA)** | frontend services/api | Auth, dashboard KPIs/activity, providers (updateCommission), payments, roles, audit, tickets, complaints, pricing preview. |
| **Theme (CRA)** | DashboardHeader | Dark/light toggle in CRA. |
| **i18n context (CRA)** | LocaleContext | Locale provider present; AR/RTL not implemented. |

### 3.2 Missing or incomplete (frontend)

| Gap | Priority | Plan reference |
|-----|----------|----------------|
| **Single canonical dashboard app** | High | Two codebases (CRA vs Vite); plan is one Company Dashboard (e.g. front/my-app) and clear Client Portal. |
| **Real auth & API in Vite app** | High | front/my-app uses mock AuthContext; no real login or API client. |
| **Theme toggle in Vite** | Medium | DASHBOARD_DESIGN_REPORT; CRA has it, Vite does not. |
| **Role-specific content G2–G6** | High | Sales pipeline, Finance treasury, Support SLA/CSAT, Executive BI, HR personnel (DASHBOARD_DESIGN_REPORT). |
| **Financials UI** | High | Commission analysis (Base vs Ziyarah Delta), refund with mandatory reason + audit, payout summary (DASHBOARD_DESIGN_REPORT, DYNAMIC_COMMISSION). |
| **Support** | Medium | Ticket queue by priority, resolution dashboard, escalation hub (DASHBOARD_DESIGN_REPORT). |
| **Global search (⌘K)** | Medium | Search bookings/providers/users from header (DASHBOARD_DESIGN_REPORT, FRONTEND_IMPLEMENTATION_PLAN Phase 6). |
| **Search in deleted (G1 only)** | Medium | FRONTEND_IMPLEMENTATION_PLAN Phase 6. |
| **Server-side pagination, sort, Excel/PDF export** | Medium | DASHBOARD_DESIGN_REPORT §5. |
| **Permission-based visibility** | Medium | pay:view_commission, sys:audit_read to hide commission/audit (ROLE_MANAGEMENT_REPORT). |
| **i18n & RTL (AR)** | High | REQUIREMENTS_ANALYSIS, implementation_plan Phase 4; no AR bundles or RTL layout. |
| **Three-tier deployment** | High | Landing (ziyarah.com), Dashboard (dashboard.*), Client Portal (providers.*) as separate SPAs/routes (INFRASTRUCTURE_REPORT). |
| **Glassmorphism, design tokens** | Low | DASHBOARD_DESIGN_REPORT; optional polish. |

---

## 4. Database Migrations State

### 4.1 Existing migrations (in order)

| # | File | Purpose |
|---|------|--------|
| 1 | 001_plans_schema_extensions.sql | i18n_labels; roles.is_system_role |
| 2 | 002_role_management_report.sql | roles.status; permissions.is_locked |
| 3 | 003_pricing_and_payment_methods.sql | service_providers.commission_rate; services seasonal_multiplier, tax_rate; payments.idempotency_key; payment_method_enum CASH_ON_SERVICE |
| 4 | 004_hibernate_enum_compat.sql | Enum compatibility |
| 5 | 005_reviews_status_varchar.sql | Reviews status as varchar |
| 6 | 006_discount_codes_jpa_compat.sql | Discount codes JPA compatibility |
| 7 | 007_service_providers_jpa_compat.sql | Service providers JPA compatibility |
| 8 | 008_employees_payments_enum_compat.sql | Employees/payments enum compatibility |
| 9 | 009_auth_tokens_otp.sql | password_reset_tokens; otp_verification |
| 10 | 010_ar_columns_i18n.sql | _ar columns (departments, groups, roles, permissions, services, service_providers, discount_codes) |
| 11 | 011_hibernate_enum_compat_remaining.sql | Remaining Hibernate enum compatibility |
| 12 | 012_entity_columns.sql | departments.manager_id; notifications.message, template_name, updated_at; refunds.currency, transaction_reference |

### 4.2 Schema characteristics (current)

- **Table names:** Flat (users, roles, permissions, groups, departments, service_providers, services, bookings, payments, refunds, discount_codes, complaints, internal_tickets, audit_logs, etc.). No domain prefixes.
- **Cross-module references:** Migrations and schema still use `REFERENCES` (e.g. password_reset_tokens → users). Plans (MODULAR_MONOLITH_STRUCTURE) require no FKs across modules; relationships by UUID in application layer.
- **i18n:** i18n_labels and _ar columns on main entities are in place (001, 010).

### 4.3 Missing migrations

| Gap | Priority | Plan reference |
|-----|----------|----------------|
| **Rename tables to prefixed** | High | sys_users, sys_roles, sys_permissions, hotel_* (listings, rooms), pay_* (transactions, refunds), bkg_* (reservations), disc_*, rest_*, taxi_* (MODULAR_MONOLITH_STRUCTURE, implementation_plan Phase 2). |
| **Remove cross-module FKs** | High | Replace with UUID columns only; enforce relationships in code (MODULAR_MONOLITH_STRUCTURE). |
| **provider_commission / pay_ management table** | Medium | DYNAMIC_COMMISSION_REPORT mentions provider_commission record; currently commission_rate on service_providers suffices but plan may imply separate audit table. |
| **3DS / gateway reference columns** | Medium | PAYMENT_METHODS: transaction status, gateway refs; add if not present on payments. |

---

## 5. Cross-Cutting Alignment

### 5.1 Requirements (SyRS / REQUIREMENTS_ANALYSIS)

- **RBAC granular:** Backend has roles/permissions; frontend has role dashboards; full resource:action UI enforcement and permission-based hiding (e.g. pay:view_commission) incomplete.
- **Multilingual EN/AR + RTL:** Backend _ar columns and i18n_labels exist; frontend i18n and RTL missing.
- **10% commission / dynamic override:** Backend and DB implemented; dashboard commission override UI in CRA.
- **Refund with reason and audit:** Backend refund API exists; frontend “mandatory reason” and audit display incomplete.
- **Three-tier frontend:** Not deployed; single CRA app contains landing, dashboard, and portal.

### 5.2 Infrastructure (INFRASTRUCTURE_REPORT)

- **Containers:** Backend, DB, Redis, Kafka, Nginx, frontend-core, frontend-portal are specified; actual docker-compose and host-based routing (dashboard.*, providers.*) not verified in this report.
- **URL mapping:** dashboard.* → Company Dashboard, providers.* → Client Portal; requires separate build/deploy or route split.

### 5.3 Implementation plan phases (implementation_plan.md)

| Phase | Backend | Frontend | DB |
|-------|---------|----------|-----|
| 1 – Foundation & Security (sys_) | Partially (RBAC, audit); no module layout | — | RBAC columns done; no sys_ prefix |
| 2 – Pricing & Commission | PricingService + commission override done | Commission UI in CRA | commission_rate, idempotency done |
| 3 – Financial & Payment | Idempotency, webhook stub; no Visa adapter | Refund/reason UI incomplete | idempotency_key done |
| 4 – Content & i18n, three-tier frontend | _ar in migrations | i18n/RTL missing; three-tier not deployed | _ar columns done |

---

## 6. Recommendations (prioritized)

### Critical / high

1. **Backend:** Introduce modular package layout (com.ziyarah.modules.*) and interface-only cross-module APIs; add migrations to rename tables to domain prefixes and remove cross-module FKs (per MODULAR_MONOLITH_STRUCTURE and implementation_plan).
2. **Frontend:** Choose one canonical Company Dashboard app (CRA or Vite); migrate features into it, wire real auth and API in Vite if chosen; implement or fix Client Portal (single SPA or clear route split) and align with dashboard.* / providers.*.
3. **Frontend:** Implement Financials UI: commission analysis (Base vs Ziyarah Delta), refund flow with mandatory reason and audit trail, payout summary.
4. **Frontend:** Add i18n (EN/AR) and RTL for dashboard and portal (REQUIREMENTS_ANALYSIS, implementation_plan Phase 4).
5. **Backend:** Implement payment gateway integration (VisaPaymentAdapter or equivalent) and webhook signature verification (PAYMENT_METHODS).

### Medium

6. **Frontend:** Implement role-specific dashboards and pages for G2–G6 (Sales, Finance, Support, Executive, HR) with real or placeholder data.
7. **Frontend:** Global search (⌘K) and “Search in deleted” (G1); server-side pagination, sort, and export where specified.
8. **Frontend:** Permission-based visibility (commission, audit, roles) from backend permissions.
9. **Backend:** Add ArchUnit tests for module boundaries and table-prefix compliance.
10. **Database:** Add any missing payment columns (3DS status, gateway reference) if required by PAYMENT_METHODS.

### Low

11. **Frontend:** Theme toggle and sidebar collapse in Vite; glassmorphism and micro-animations if adopting DASHBOARD_DESIGN_REPORT.
12. **Backend:** Domain events for cross-module workflows; reconciliation and fraud velocity checks.

---

## 7. Document Reference Index

| Document | Primary focus |
|----------|----------------|
| DASHBOARD_DESIGN_REPORT.md | Company dashboard UX, G1–G6 views, layout, KPIs, financials, RBAC UI |
| DYNAMIC_COMMISSION_REPORT.md | Commission hierarchy, provider override, audit |
| FIGMA_NEEDS_BY_PHASE.md | Design deliverables per frontend phase |
| FRONTEND_GAP_REPORT.md | Implemented vs missing frontend (Company Dashboard & Client Portal) |
| FRONTEND_IMPLEMENTATION_PLAN.md | Phased plan for dashboard and portal (Phases 0–8) |
| implementation_plan.md | Phases 1–4: foundation, pricing, financial, content & frontend |
| INFRASTRUCTURE_REPORT.md | Docker, URLs, three-tier frontend |
| MODULAR_MONOLITH_STRUCTURE.md | Package layout, table prefixes, communication rules |
| MONOLITH_IMPLEMENTATION.md | Technical guide: modules, RBAC, pricing, payment adapters |
| PAYMENT_METHODS.md | Visa, webhooks, idempotency, PCI, security |
| PRICING_METHODS.md | Vertical pricing, commission, currency, discounts |
| REQUIREMENTS_ANALYSIS.md | Backend scope, entities, APIs, roles, stack |
| ROLE_MANAGEMENT_REPORT.md | G1–G7, permissions, role lifecycle |
| SYSTEM_EVOLUTION_REPORT.md | Transition to modular monolith |
| SyRS-Ziyarah.tex | System requirements, use cases, NFRs, architecture |

---

*Report generated from the listed plans, SyRS, and codebase inspection. PDFs (Ziyarah APP, Ziyarah Roles, Dashboard for ZIYARA) were not parsed.*
