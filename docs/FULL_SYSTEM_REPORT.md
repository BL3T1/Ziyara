# Ziyara — Full System Report

**Purpose:** Single narrative that describes the platform, lists technologies, deployment needs, and assesses **project completeness** by reconciling the documentation set below. Where sources disagree (dates, stack versions, or “missing” vs “done”), this report calls that out explicitly.

**Source documents (inputs to this report):**

| Document | Role in this synthesis |
|----------|-------------------------|
| [ARABIC_I18N.md](ARABIC_I18N.md) | Bilingual UI + API behavior (`Accept-Language`, `_ar` columns, extension pattern) |
| [BACKEND_CRUD_REPORT.md](BACKEND_CRUD_REPORT.md) | CRUD matrix, portal coverage, optional API gaps |
| [BACKEND_ENDPOINTS_REPORT.md](BACKEND_ENDPOINTS_REPORT.md) | Human-readable endpoint test run (counts, known 404/500) |
| [BACKEND_GAP_ANALYSIS.md](BACKEND_GAP_ANALYSIS.md) | Historical comparison vs plan specs; **partially superseded** by later work |
| [BACKEND_REPORT.md](BACKEND_REPORT.md) | Canonical-style API inventory; **version line may lag** `core/build.gradle.kts` |
| [DASHBOARD_AND_LANDING_GAP_REPORT.md](DASHBOARD_AND_LANDING_GAP_REPORT.md) | UI/backend gaps; **largely superseded** by [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md) |
| [DATABASE_MIGRATIONS_REPORT.md](DATABASE_MIGRATIONS_REPORT.md) | Migrations **001–015**, seed, **018–022**; matches `apply-all.ps1` and `database/Dockerfile` |
| [DEMO_ACCOUNTS.md](DEMO_ACCOUNTS.md) | Seeded demo users and prod safety notes |
| [DOCKER_TESTING.md](DOCKER_TESTING.md) | Multi-domain Docker workflow, ports, troubleshooting; align with Dockerfile for latest migration count |
| [ENDPOINT_TEST_REPORT.json](ENDPOINT_TEST_REPORT.json) | Machine-readable endpoint test results (paired with markdown report) |
| [IMPLEMENTATION_HANDOFF_SUMMARY.md](IMPLEMENTATION_HANDOFF_SUMMARY.md) | Phases 2–4 backend completion narrative; paths may reference legacy `backend/` layout |
| [MIGRATION_IMPLEMENTATION_PLAN.md](MIGRATION_IMPLEMENTATION_PLAN.md) | Plan for 011/012 + apply-all alignment |
| [MIGRATIONS_VS_PLANS_ANALYSIS.md](MIGRATIONS_VS_PLANS_ANALYSIS.md) | Storage vs plan vision; payout ledger, docs, provider–user linkage gaps |
| [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md) | **Current** remaining feature checklist (post–gap-report fixes) |
| [MISSING_MIGRATIONS_REPORT.md](MISSING_MIGRATIONS_REPORT.md) | Historical gap list; 011/012 **marked addressed** |
| [SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md](SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md) | Stack + server install (Docker-only host) |
| [SNYK_CI_BASELINE.md](SNYK_CI_BASELINE.md) | Optional Snyk baseline / CI gating notes |
| [SYSTEM_INTEGRATION_REPORT.md](SYSTEM_INTEGRATION_REPORT.md) | End-to-end integration snapshot; some frontend paths/names may predate `front/my-app` |

---

## 1. Executive summary

### 1.1 What the system is

Ziyara is a **booking and operations platform**: Spring Boot API (`/api/v1`), PostgreSQL, React (Vite) dashboards for **company staff** and **providers**, optional **public landing** and **multi-domain** Nginx routing, JWT auth, RBAC, bookings/payments/discounts/complaints/tickets/reviews/notifications, dashboards and reports, and **English/Arabic** support (UI + localized API fields where implemented).

### 1.2 Completeness (high level)

| Dimension | Assessment | Notes |
|-----------|------------|--------|
| **Core backend REST API** | **High (~85–92%)** | Auth, users, services, providers, bookings, payments/refunds, discounts (incl. apply), complaints, tickets, reviews, notifications, currency, pricing preview, dashboard KPIs + Phase 4 analytics, reports, audit logs, portal endpoints per [BACKEND_CRUD_REPORT.md](BACKEND_CRUD_REPORT.md) and [IMPLEMENTATION_HANDOFF_SUMMARY.md](IMPLEMENTATION_HANDOFF_SUMMARY.md). |
| **Backend vs original plan docs** | **Moderate (~70% “vision”, high “MVP”)** | [BACKEND_GAP_ANALYSIS.md](BACKEND_GAP_ANALYSIS.md) still lists architectural gaps (table prefixes, modular monolith boundaries, Kafka, etc.) and some product depth (e.g. group-specific KPIs, payment gateway depth). Many line items in that file are **out of date** relative to implemented Phases 2–4. |
| **Database storage vs plans** | **High for current app; partial for “full vision”** | [MIGRATIONS_VS_PLANS_ANALYSIS.md](MIGRATIONS_VS_PLANS_ANALYSIS.md): core storage covered; gaps include **dedicated provider payout ledger**, **document verification** storage, **provider–user linkage** for richer portal RBAC. |
| **Frontend (company + portal + landing)** | **High with clear optional gaps** | [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md): major flows wired; **Admin Settings** and **Admin API** (OpenAPI viewer) are implemented; **landing contact** posts to **`POST /public/contact`**. Remaining: **Support chat**, optional **ticket/complaints polish**, **portal staff/support**. |
| **i18n (EN/AR)** | **Implemented pattern; content coverage varies** | [ARABIC_I18N.md](ARABIC_I18N.md): locale in storage, RTL, `Accept-Language`, `_ar` columns + `RequestLocaleHolder`; extending to more entities is documented. |
| **Operations / security** | **Baseline present** | Docker deploy ([SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md](SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md), [DOCKER_TESTING.md](DOCKER_TESTING.md)); Snyk in CI ([SNYK_CI_BASELINE.md](SNYK_CI_BASELINE.md)). |

**Single “percent complete” number is misleading** because “complete” depends on whether you mean **shippable MVP**, **parity with all internal plan PDFs**, or **production hardening** (real email/SMS, PCI, full 3DS, etc.). Treat the tables above as the honest breakdown.

### 1.3 Documentation health

- **Prefer** [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md) over [DASHBOARD_AND_LANDING_GAP_REPORT.md](DASHBOARD_AND_LANDING_GAP_REPORT.md) for “what is still missing” on the UI.
- **Prefer** [BACKEND_CRUD_REPORT.md](BACKEND_CRUD_REPORT.md) + live code for API surface; treat [BACKEND_GAP_ANALYSIS.md](BACKEND_GAP_ANALYSIS.md) as **historical** unless refreshed.
- **Database:** [DATABASE_MIGRATIONS_REPORT.md](DATABASE_MIGRATIONS_REPORT.md) is aligned with **`database/Dockerfile`** and **`database/apply-all.ps1`**: **001–015**, **seed**, **018–022**.

---

## 2. Technology stack (current repository)

Consolidated from [SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md](SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md) and `core/build.gradle.kts` (authoritative for backend dependency versions).

| Layer | Technologies |
|-------|----------------|
| **Backend** | Java 17, Spring Boot 3.5.x, Spring Security, JWT (jjwt), Spring Data JPA, jOOQ (ad-hoc queries), springdoc OpenAPI, Lombok, PostgreSQL driver, Testcontainers (optional tests) |
| **Frontend** | React 19, TypeScript, Vite 7, Tailwind 4, React Router, axios, nginx static hosting in container |
| **Database** | PostgreSQL 15 (Alpine image in Docker) |
| **Infra** | Docker Compose, Nginx (edge proxy for multi-domain profile), optional pgAdmin |
| **Build** | **Gradle** for backend (`core/`); **npm** inside frontend image build stage only |

**Note:** [BACKEND_REPORT.md](BACKEND_REPORT.md) header matches `core/build.gradle.kts` (Spring Boot **3.5.12** as of April 2026).

---

## 3. Server deployment (what to install)

From [SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md](SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md):

- **On the host:** Docker Engine + Docker Compose **only** (no Java/Gradle/Node/Postgres on host for containerized deploy).
- **Required env (Compose):** `POSTGRES_PASSWORD`, `JWT_SECRET`, and `PGADMIN_DEFAULT_PASSWORD` if pgAdmin is enabled.
- **Production extras:** TLS, firewall, backups, monitoring, secrets management.

Operational detail: [DOCKER_TESTING.md](DOCKER_TESTING.md) (hosts file for local multi-domain, `run-docker.ps1`, troubleshooting, media uploads, `APP_MEDIA_*`).

---

## 4. Backend — capabilities and gaps

### 4.1 Endpoint inventory and tests

- [BACKEND_REPORT.md](BACKEND_REPORT.md): broad endpoint catalog (auth, users, dashboard, bookings, departments, employees, providers, currency, pricing, discounts, payments, webhooks, tickets, reports, complaints, services, taxi, reviews, notifications, audit, roles, actuator).
- [BACKEND_ENDPOINTS_REPORT.md](BACKEND_ENDPOINTS_REPORT.md): dated test run — **41× 200/201**, **1× 404** (test data issue), **4× 500** (with notes on fallbacks/fixes for some dashboard paths).
- [ENDPOINT_TEST_REPORT.json](ENDPOINT_TEST_REPORT.json): structured results for automation (same campaign as the markdown report; sample rows show occasional **500** on login-history in that snapshot — compare with markdown “now returns 200 with fallback” narrative).

### 4.2 CRUD and “optional API” checklist

From [BACKEND_CRUD_REPORT.md](BACKEND_CRUD_REPORT.md):

- **Strong coverage:** users, services, providers, discounts, internal tickets, employees, departments, portal service CUD, portal dashboard/bookings/earnings, many booking/payment flows.
- **By design partial:** bookings (no delete), payments (immutable after create), complaints (no delete), notifications (read-focused), audit read-only, dashboard/reports read-only.
- **Implemented since older snapshots:** `PUT /roles/{id}` (metadata), `PUT /roles/{id}/permissions` (system vs custom rules), admin **`GET /reviews`** (paginated), **service image** CRUD/upload on `ServiceController`, **`GET|PUT /admin/settings`**, **`POST /public/contact`**.  
- **Optional / lower priority (still):** `GET/DELETE /currency/rates/{id}`, `GET /taxi-bookings/{id}`, portal staff CRUD, live chat — see [BACKEND_CRUD_REPORT.md](BACKEND_CRUD_REPORT.md).

### 4.3 Gap analysis vs plans (stale sections)

[BACKEND_GAP_ANALYSIS.md](BACKEND_GAP_ANALYSIS.md) is valuable for **themes** (architecture, infrastructure, payment gateway depth, group KPIs) but **many row-level statuses are outdated** relative to [IMPLEMENTATION_HANDOFF_SUMMARY.md](IMPLEMENTATION_HANDOFF_SUMMARY.md) (e.g. services CRUD, complaints lifecycle, discounts CRUD + refund endpoint, dashboard extensions). Use it for **roadmap**, not as a live checklist without re-auditing code.

---

## 5. Frontend — dashboards, portal, landing

### 5.1 Historical gap report

[DASHBOARD_AND_LANDING_GAP_REPORT.md](DASHBOARD_AND_LANDING_GAP_REPORT.md) described missing provider-scoped APIs, discounts/reports pages, `/users/me`, availability/images, etc. **Most of that backend and much of the frontend has since been implemented.**

### 5.2 Current remaining work

[MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md) is the up-to-date checklist:

- **Company dashboard:** **Settings** (`GET|PUT /admin/settings`) and **Admin > API** (OpenAPI doc viewer, super_admin) implemented; **Support > Chat** still placeholder; optional complaints detail/escalate/comments polish.
- **Portal:** Optional staff UI + APIs; optional support; optional switch to richer provider-scoped KPIs if backend adds them.
- **Landing:** **Contact** form wired to **`POST /public/contact`** (leads stored per migration **021**).

### 5.3 Integration snapshot

[SYSTEM_INTEGRATION_REPORT.md](SYSTEM_INTEGRATION_REPORT.md) describes request flow (JWT, `ApiResponse` envelope, axios), controller↔client mapping, and fixes such as **ReportController** role annotations — still useful; verify file paths (`front/my-app` vs older `frontend` references) when following it literally.

---

## 6. Database and migrations

### 6.1 Documented migration set

[DATABASE_MIGRATIONS_REPORT.md](DATABASE_MIGRATIONS_REPORT.md) and [MIGRATION_IMPLEMENTATION_PLAN.md](MIGRATION_IMPLEMENTATION_PLAN.md) cover the numbered chain through **022**: RBAC, pricing/3DS columns, **table prefixes (015)**, sample services, service images/menus, org groups/roles, **system settings + contact leads**, **expanded permission catalogue**.

[MISSING_MIGRATIONS_REPORT.md](MISSING_MIGRATIONS_REPORT.md) explained the **pre-011/012** gaps; implementation status there marks **011, 012, apply-all updates** as done.

### 6.2 Docker-init vs `apply-all.ps1`

The **`database/Dockerfile`** and **`database/apply-all.ps1`** use the same order: `schema.sql` → **001–015** → `seed.sql` → **018–022** (Postgres **15.17** in Docker). For brownfield DBs, apply any missing files or recreate the volume per [DOCKER_TESTING.md](DOCKER_TESTING.md) / [DOCKER.md](DOCKER.md).

### 6.3 Plans vs storage

[MIGRATIONS_VS_PLANS_ANALYSIS.md](MIGRATIONS_VS_PLANS_ANALYSIS.md): data model supports most implemented features; **future migrations** may be needed for payout ledger, provider documents, and explicit provider–user linkage if product scope requires them.

---

## 7. Internationalization (Arabic)

[ARABIC_I18N.md](ARABIC_I18N.md):

- **Frontend:** `ziyarah_locale`, `en.json` / `ar.json`, RTL, `Accept-Language` on requests.
- **Backend:** `LocaleFilter`, `RequestLocaleHolder`, localized DTO fields using `_ar` columns where wired.
- **DB:** Migration **010** (and Docker init) for `_ar` columns on key tables.
- **Extending:** Clear steps for new entities and jOOQ handlers.

**Completeness:** Pattern is **complete**; **coverage** depends on how many screens and endpoints use `t(...)` and `localized(...)` consistently.

---

## 8. Testing, demos, and security

| Topic | Source |
|-------|--------|
| **Demo logins** | [DEMO_ACCOUNTS.md](DEMO_ACCOUNTS.md) — password `Demo123!`; prod profile disables seeding; super-admin behavior documented |
| **Docker E2E-style testing** | [DOCKER_TESTING.md](DOCKER_TESTING.md) |
| **Endpoint script** | [IMPLEMENTATION_HANDOFF_SUMMARY.md](IMPLEMENTATION_HANDOFF_SUMMARY.md) references `scripts/test-endpoints.ps1` (adjust port/profile to your environment) |
| **Snyk / CI** | [SNYK_CI_BASELINE.md](SNYK_CI_BASELINE.md) — workflow exists; baseline optional |

---

## 9. Consolidated “completeness” matrix (documentation vs reality)

| Area | Doc says “done” | Doc says “gap” | Reconciliation |
|------|-----------------|----------------|----------------|
| Backend Phases 2–4 | [IMPLEMENTATION_HANDOFF_SUMMARY.md](IMPLEMENTATION_HANDOFF_SUMMARY.md) | [BACKEND_GAP_ANALYSIS.md](BACKEND_GAP_ANALYSIS.md) still lists some as missing | Trust handoff + CRUD report; **refresh gap analysis** |
| Portal APIs | [BACKEND_CRUD_REPORT.md](BACKEND_CRUD_REPORT.md) checklist ✅ | [DASHBOARD_AND_LANDING_GAP_REPORT.md](DASHBOARD_AND_LANDING_GAP_REPORT.md) says missing | **Portal implemented**; gap report outdated |
| Frontend discounts/reports/users | [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md) ✅ | Older integration/gap docs ⚠️ | **Missing features** supersedes |
| DB migration count | [DATABASE_MIGRATIONS_REPORT.md](DATABASE_MIGRATIONS_REPORT.md) through **022** | `database/Dockerfile` matches apply-all | **Aligned** |
| Backend version | [BACKEND_REPORT.md](BACKEND_REPORT.md) | Gradle **3.5.12** | Keep header in sync with `core/build.gradle.kts` |

---

## 10. Recommended next actions (documentation + product)

1. **Refresh** [BACKEND_GAP_ANALYSIS.md](BACKEND_GAP_ANALYSIS.md) against current controllers (or mark header “superseded — see FULL_SYSTEM_REPORT”).
2. **Re-run** endpoint smoke tests and refresh [BACKEND_ENDPOINTS_REPORT.md](BACKEND_ENDPOINTS_REPORT.md) / [ENDPOINT_TEST_REPORT.json](ENDPOINT_TEST_REPORT.json) when the API surface changes.
3. **Product:** Support chat, optional portal staff/support, and complaints polish per [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md); decide on ledger/docs/linkage per [MIGRATIONS_VS_PLANS_ANALYSIS.md](MIGRATIONS_VS_PLANS_ANALYSIS.md).

---

*This file is a synthesis only; authoritative detail remains in the linked source documents.*
