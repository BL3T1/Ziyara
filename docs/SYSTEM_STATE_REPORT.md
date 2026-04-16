# Ziyara — System State Report (Backend, Frontend, Database)

**Last verified against repository:** 2026-04-04 (code paths below)  
**Purpose:** Snapshot of platform state. Prefer this file’s **§2 Codebase verification** over older markdown when they disagree. Companion: [FULL_SYSTEM_REPORT.md](FULL_SYSTEM_REPORT.md), product gaps: [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md).

---

## 1. Sources analyzed

### In-repo (`docs/`)

| Document | Used for |
|----------|----------|
| [ARABIC_I18N.md](ARABIC_I18N.md) | EN/AR pattern (note: implementation uses `LanguageContext` + `translations.ts` in `front/my-app`, not only legacy `locales/en.json`) |
| [BACKEND_CRUD_REPORT.md](BACKEND_CRUD_REPORT.md) | CRUD matrix — **refresh alongside code** (see §2) |
| [BACKEND_ENDPOINTS_REPORT.md](BACKEND_ENDPOINTS_REPORT.md) | Historical endpoint test run |
| [BACKEND_GAP_ANALYSIS.md](BACKEND_GAP_ANALYSIS.md) | Themes only; many rows stale |
| [BACKEND_REPORT.md](BACKEND_REPORT.md) | API inventory — **header version**: align with [core/build.gradle.kts](../core/build.gradle.kts) |
| [DASHBOARD_AND_LANDING_GAP_REPORT.md](DASHBOARD_AND_LANDING_GAP_REPORT.md) | Superseded for UI checklist |
| [DATABASE_MIGRATIONS_REPORT.md](DATABASE_MIGRATIONS_REPORT.md) | Full chain **001–015**, seed, **018–022** (matches Docker + `apply-all.ps1`) |
| [DEMO_ACCOUNTS.md](DEMO_ACCOUNTS.md) | Demo users |
| [DOCKER_TESTING.md](DOCKER_TESTING.md), [DOCKER.md](../DOCKER.md) | Compose, multi-domain |
| [ENDPOINT_TEST_REPORT.json](ENDPOINT_TEST_REPORT.json) | Machine-readable tests |
| [FULL_SYSTEM_REPORT.md](FULL_SYSTEM_REPORT.md) | Narrative synthesis |
| [IMPLEMENTATION_HANDOFF_SUMMARY.md](IMPLEMENTATION_HANDOFF_SUMMARY.md) | Phase 2–4 narrative |
| [MIGRATION_IMPLEMENTATION_PLAN.md](MIGRATION_IMPLEMENTATION_PLAN.md), [MIGRATIONS_VS_PLANS_ANALYSIS.md](MIGRATIONS_VS_PLANS_ANALYSIS.md) | Migrations vs vision |
| [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md) | Product checklist — **aligned with code in §6** |
| [RBAC_NAV_QA.md](RBAC_NAV_QA.md), [RBAC_PERMISSION_EDITOR_QA.md](RBAC_PERMISSION_EDITOR_QA.md) | RBAC QA |
| [SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md](SERVER_TECHNOLOGY_AND_INSTALL_REPORT.md) | Stack / Docker host |
| [SNYK_CI_BASELINE.md](SNYK_CI_BASELINE.md), [SNYK_PENETRATION_TEST_REPORT.md](SNYK_PENETRATION_TEST_REPORT.md) | Security scans |
| [SYSTEM_INTEGRATION_REPORT.md](SYSTEM_INTEGRATION_REPORT.md) | Integration flow |

### External PDFs (Desktop)

SyRS and related PDFs: scope includes Flutter mobile and features not all present in this web monorepo (see §6.4).

---

## 2. Codebase verification (authoritative)

Values below are from **current** project files, not from legacy docs alone.

### 2.1 Backend (`core/`)

| Item | Source in repo |
|------|----------------|
| **Spring Boot** | **3.5.12** — [core/build.gradle.kts](../core/build.gradle.kts) (`org.springframework.boot` plugin) |
| **Java** | **17** — same file |
| **API base path** | **`/api/v1`** — [core/src/main/resources/application.yml](../core/src/main/resources/application.yml) `server.servlet.context-path` |
| **Controllers** | **26** REST controllers under `core/src/main/java/com/ziyara/backend/presentation/controller/` (auth, users, services, providers, bookings, payments, discounts, complaints, tickets, reviews, notifications, currency, dashboard, reports, audit, roles, portal, public contact, content, pricing, webhooks, employees, departments, admin settings, super admin, taxi) |
| **JWT authorities** | Single `ROLE_{UserRole}` only — [CustomUserDetailsService.java](../core/src/main/java/com/ziyara/backend/infrastructure/security/CustomUserDetailsService.java) (DB permission codes **not** on `UserDetails`) |
| **RBAC admin** | [RoleManagementController.java](../core/src/main/java/com/ziyara/backend/presentation/controller/RoleManagementController.java): `PUT /roles/{id}` (name/description), `PUT /roles/{id}/permissions` (system vs custom + locked rules), catalogue endpoints, Super Admin only |
| **Admin settings API** | [AdminSystemSettingsController.java](../core/src/main/java/com/ziyara/backend/presentation/controller/AdminSystemSettingsController.java) + service (company display name, default currency, maintenance) |
| **Public contact / leads** | `POST /public/contact` — [PublicContactController.java](../core/src/main/java/com/ziyara/backend/presentation/controller/PublicContactController.java), [ContactLeadService.java](../core/src/main/java/com/ziyara/backend/application/service/ContactLeadService.java) |
| **Reviews admin list** | `GET /reviews` paginated (company staff) — [ReviewController.java](../core/src/main/java/com/ziyara/backend/presentation/controller/ReviewController.java) |
| **JPA ddl-auto** | **`none`** in default [application.yml](../core/src/main/resources/application.yml); Docker profile uses **`update`** in [docker-compose.yml](../docker-compose.yml) — schema drift risk in containerized dev |

### 2.2 Frontend (`front/my-app/`)

| Item | Source in repo |
|------|----------------|
| **React** | **19.2.x** — [package.json](../front/my-app/package.json) |
| **Vite** | **7.3.x** — same |
| **Tailwind** | **4.2.x** — devDependencies |
| **Surfaces** | **Company** — [AppCompanyRoutes.tsx](../front/my-app/src/apps/company/AppCompanyRoutes.tsx); **Provider** — [AppProviderRoutes.tsx](../front/my-app/src/apps/provider/AppProviderRoutes.tsx); **Landing** — [AppLandingRoutes.tsx](../front/my-app/src/apps/landing/AppLandingRoutes.tsx) |
| **Admin Settings** | **Implemented** — [SettingsPage.tsx](../front/my-app/src/pages/admin/SettingsPage.tsx) → `settingsAPI` / `GET|PUT /admin/settings` |
| **Admin API** | **OpenAPI viewer** (super_admin) — [ApiPage.tsx](../front/my-app/src/pages/admin/ApiPage.tsx) + [OpenApiDocView](../front/my-app/src/components/openapi/OpenApiDocView.tsx) |
| **Landing contact** | **Wired** — [LandingContactPage.tsx](../front/my-app/src/apps/landing/LandingContactPage.tsx) → `publicAPI.submitContact` → `POST /public/contact` |
| **Portal Staff** | **Identity / org summary** (not full team CRUD) — [PortalStaffPage.tsx](../front/my-app/src/pages/portal/PortalStaffPage.tsx) |
| **Portal Support** | **Hub + FAQ links** — [PortalSupportPage.tsx](../front/my-app/src/pages/portal/PortalSupportPage.tsx) |
| **Company sidebar** | No **Chat** route — [sidebar.ts](../front/my-app/src/config/sidebar.ts) lists tickets, complaints, reviews under Support only |

### 2.3 Database (`database/`)

| Item | Source in repo |
|------|----------------|
| **PostgreSQL image** | **15.17-alpine** — [database/Dockerfile](../database/Dockerfile) |
| **Init order** | `schema.sql` → **001–015** → `seed.sql` → **018–022** — same Dockerfile (`22-022` = RBAC permission catalogue) |
| **Host apply script** | [database/apply-all.ps1](../database/apply-all.ps1) through **022** |

---

## 3. Executive summary

| Layer | State | Confidence |
|-------|--------|------------|
| **Backend** | Spring Boot **3.5.12**, Java 17, broad REST surface, portal + public contact + admin settings + full RBAC role API. | **High** (verified paths in §2.1) |
| **Frontend** | React 19 + Vite 7; company, portal, landing apps; Settings and API docs implemented; landing contact live. | **High** (§2.2) |
| **Database** | Postgres 15; Docker/init and `apply-all.ps1` include **022**. | **High** (§2.3) |

**RBAC:** DB `sys_permissions` / role assignments are **not** enforced by `@PreAuthorize` today — only `UserRole` enum on JWT ([RBAC_PERMISSION_EDITOR_QA.md](RBAC_PERMISSION_EDITOR_QA.md)).

---

## 4. Backend state (summary)

- Core domains and portal endpoints match [BACKEND_CRUD_REPORT.md](BACKEND_CRUD_REPORT.md) after corrections (reviews list, roles `PUT /{id}`, service images — see updated CRUD doc).
- [BACKEND_GAP_ANALYSIS.md](BACKEND_GAP_ANALYSIS.md): use for **architecture** only.

---

## 5. Frontend state (summary)

- Management flows: discounts, reports, users, bookings, complaints, tickets (+ detail), reviews, roles, etc. per routes in `AppCompanyRoutes.tsx`.
- **Not implemented:** dedicated **company Support → Live Chat** page/route (sidebar has no chat item). Optional complaints polish remains product choice.

---

## 6. Features still missing (consolidated, code-aligned)

### 6.1 Product / optional (still accurate)

| Area | Gap |
|------|-----|
| **Backend** | Optional **portal staff CRUD** API (no `/portal/staff` CRUD in code grep); optional **GET/DELETE /currency/rates/{id}**, **GET /taxi-bookings/{id}**; optional **API keys** resource (distinct from OpenAPI viewer). |
| **Frontend — company** | **Live chat** UI + backend if required (no route today). Optional **complaints** detail / escalate / comments depth. |
| **Frontend — portal** | **Provider-scoped dashboard KPIs** if product wants metrics beyond current overview; **staff** page is informational, not multi-user provisioning. |
| **Architecture / vision** | Table prefixes, Kafka/events, deep payment/3DS, group KPIs, payout ledger, document verification — per plan docs. |

### 6.2 Corrected vs old MISSING_FEATURES list

The following are **implemented in code** and should **not** be listed as missing:

- **Admin Settings** (real page + `/admin/settings` API).
- **Admin API** (OpenAPI doc view for super_admin — not “API keys”, but not a blank placeholder).
- **Landing contact** → **`POST /public/contact`**.
- **`PUT /roles/{id}`** (role metadata).
- **`GET /reviews`** (admin paginated list).

### 6.3 SyRS / mobile scope

Flutter app, social login, FAB chat, geographic pipeline, HR leave/attendance, fleet/iCal, push/offline — **out of scope** for this repo unless a mobile project is added.

---

## 7. Risks

| Risk | Note |
|------|------|
| **DB permissions ≠ Spring Security** | [CustomUserDetailsService](../core/src/main/java/com/ziyara/backend/infrastructure/security/CustomUserDetailsService.java) — roadmap: load permission authorities. |
| **JPA `ddl-auto: update` in Docker** | Can drift from SQL migrations — use Flyway/Liquibase or `none` for prod-like DBs. |
| **Doc drift** | Keep [BACKEND_REPORT.md](BACKEND_REPORT.md), [DATABASE_MIGRATIONS_REPORT.md](DATABASE_MIGRATIONS_REPORT.md), [MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md) in sync with §2. |

---

## 8. Recommended next steps

1. Keep **[MISSING_FEATURES_REPORT.md](MISSING_FEATURES_REPORT.md)** updated with §6.2 corrections.
2. Bump **[BACKEND_REPORT.md](BACKEND_REPORT.md)** header to Spring Boot **3.5.12**.
3. Extend **[DATABASE_MIGRATIONS_REPORT.md](DATABASE_MIGRATIONS_REPORT.md)** through **022** and document Docker init order.
4. Re-run `scripts/test-endpoints.ps1` and refresh [BACKEND_ENDPOINTS_REPORT.md](BACKEND_ENDPOINTS_REPORT.md) when the API surface changes.

---

*This report is a synthesis; production behavior should be validated with integration tests and OpenAPI.*
