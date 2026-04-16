# Ziyara – Missing Features Report (Updated)

This report lists what is **still missing** after recent implementations. It supersedes the original gap report for items that have been completed and provides a single checklist for remaining work.

**Reference:** Previous analysis in `DASHBOARD_AND_LANDING_GAP_REPORT.md`. Completed work includes **Phase 5** — provider **portal support requests**: migration **025** (`hotel_portal_support_requests`), **`GET/POST /portal/support-requests`** (`PortalSupportRequestsController`), **`portalSupportAPI`** + form and history on **`PortalSupportPage`** (distinct from company **`/tickets`** internal desk).

---

## 1. Executive Summary

| Area | Backend still missing | Frontend still missing |
|------|------------------------|------------------------|
| **Company dashboard** | — (structured flags + integration API keys: **024** + admin APIs) | **Support > Chat** — **deferred / out of scope** (see §7) |
| **Providers dashboard** | Optional: advanced payouts | Optional: company UI to triage **`hotel_portal_support_requests`** (data captured in Phase 5) |
| **Company landing** | — | — (**Contact** uses **`POST /public/contact`**) |

---

## 2. Completed (No Longer Missing)

### Backend
- **GET /users/me** – current user profile
- **PUT /users/me** – update current user (email, phone)
- **POST /users/me/change-password** – change password (current + new)
- **GET /reviews** – paginated admin list (company JWT)
- **PUT /roles/{id}** – role name/description metadata
- **PUT /roles/{id}/permissions** – permission sets; **system** roles may include locked permissions; **custom** roles cannot
- **GET /admin/settings**, **PUT /admin/settings** – platform settings (`AdminSystemSettingsController`; roles: SUPER_ADMIN, CEO, GENERAL_MANAGER)
- **POST /public/contact** – landing contact / demo requests → `support_contact_leads` (migration **021**)
- Service **image** CRUD + multipart upload on service routes (see `ServiceController` / OpenAPI)
- **GET /portal/dashboard** – provider-scoped KPIs for portal home
- **Portal staff** – **`GET/POST/PUT/DELETE /portal/staff`** (migration **023**, `hotel_provider_staff`)
- **Currency** – **`GET /currency/rates/{id}`**, **`DELETE /currency/rates/{id}`** (optional admin flows)
- **Taxi** – **`GET /taxi-bookings/{id}`** (optional admin/detail)
- **Feature flags** – **`GET /admin/feature-flags`**, **`PUT /admin/feature-flags`** (`AdminFeatureFlagsController`; roles: SUPER_ADMIN, CEO, GENERAL_MANAGER); table **`sys_feature_flags`**
- **Integration API keys** – **`GET/POST/DELETE /admin/integration-api-keys`** (`AdminIntegrationApiKeysController`; **Super Admin only**); bcrypt-hashed secrets; table **`sys_integration_api_keys`**
- **Portal support requests** – **`GET /portal/support-requests`**, **`POST /portal/support-requests`** (migration **025**, `hotel_portal_support_requests`; provider JWT; not internal **`/tickets`**)

### Frontend (front/my-app)
- **discountsAPI** – list, get, create, update, delete, approve, deactivate, validate
- **reportsAPI** – getRevenueReport(start, end), getBookingReport(start, end)
- **usersAPI** – getMe, updateMe, changePassword, create, update, delete, freeze, unfreeze, resetPassword
- **settingsAPI** – **Admin > Settings** page (`SettingsPage.tsx`) wired to `/admin/settings`
- **Admin > API** – **`ApiPage.tsx`**: OpenAPI doc viewer (`OpenApiDocView`) for **super_admin**
- **Roles** – `RolesPage` with permission editor aligned to catalogue (super_admin)
- **Discounts page** – full page at `/management/discounts`
- **Reports page** – at `/management/reports`
- **Providers** – Approve and Suspend row actions
- **Bookings** – View (detail modal), Confirm, Cancel (with reason in modal)
- **Complaints** – Assign (modal with agent ID), Resolve (modal with notes), Close, **detail modal**, **Escalate (with target user UUID)**, **comments** (optional internal thread toggle when API supports it)
- **Payments** – Refund already present (modal + reason)
- **Landing contact** – `LandingContactPage` submits via **`publicAPI.submitContact`** → **`POST /public/contact`**
- **Portal overview** – `ClientPortalOverview` uses **`GET /portal/dashboard`**
- **Portal staff** – `PortalStaffPage` + **`portalStaffAPI`** wired to **`/portal/staff`**
- **Integrations** – `IntegrationsPage.tsx` at **`/admin/integrations`**; **`integrationsAPI`** (feature flags for super_admin / executive / admin UI; API keys UI **super_admin** only)
- **Currency rates** – **View** on `CurrencyRatesPage` calls **`GET /currency/rates/{id}`** (finance JWT); list/create/delete unchanged
- **Portal support** – **`PortalSupportPage`**: submit + recent list via **`portalSupportAPI`** → **`/portal/support-requests`**

---

## 3. Backend – Still Missing

| # | Area | Missing endpoint / function |
|---|------|-------------------------------|
| — | — | *No required gaps; optional items in §7.* |

---

## 4. Frontend – Still Missing

### 4.1 Company dashboard (front/my-app)

| # | Item | Notes |
|---|------|--------|
| — | **Support > Chat** | **Deferred / out of scope** — no sidebar route; product chose not to implement realtime chat in this phase. |

### 4.2 Provider portal (front/my-app)

Core portal pages (listings, bookings, earnings, profile, overview, staff) exist. Remaining items are optional:

| # | Item | Notes |
|---|------|--------|
| — | **Portal > Support** | **Phase 5:** hub + FAQ + **`POST/GET /portal/support-requests`** + UI form/history. Optional later: company triage screen or chat. |

### 4.3 Company landing

| # | Item | Notes |
|---|------|--------|
| — | **Contact / Request demo** | **Done** — wired to **`POST /public/contact`**. |

---

## 5. Consolidated Checklist

### Backend
- [x] Portal staff CRUD (`/portal/staff`, migration 023)
- [x] API keys + feature flags (migration **024**, admin controllers above)
- [x] Currency rate by-id get/delete; taxi booking by-id read
- [x] Portal support requests (`/portal/support-requests`, migration **025**)

### Frontend – Company dashboard
- [ ] Support Chat — **deferred** (§7)
- [x] Complaints: detail, Escalate (with `escalateToId`), comments

### Frontend – Provider portal
- [x] Portal Staff page + API
- [x] Portal Support — submit + list (Phase 5); optional company triage UI / chat later
- [x] Portal overview: provider-scoped KPIs (`/portal/dashboard`)

### Frontend – Landing
- [x] Wire Contact / Request demo form to backend (**`POST /public/contact`**)

---

## 6. Priority Suggestions

1. **Deferred:** **Support chat** — explicitly out of scope for current phase; revisit with product (embed vs custom vs omit).
2. **Done:** Complaints escalation payload, detail/comments polish as implemented.
3. **Done:** Portal staff end-to-end (`/portal/staff` + UI).
4. **Done:** Optional REST (currency/taxi by-id) + **WebMvc** coverage (`CurrencyControllerRatesByIdWebMvcTest`, `TaxiBookingControllerGetByIdWebMvcTest`); currency **View** uses GET by id in the UI.
5. **Done:** Dedicated feature-flag rows + integration API keys (migration 024); runtime **validation** of integration keys against DB is a separate follow-up if inbound API auth should use these secrets.
6. **Done (Phase 5):** Provider portal support messages persisted (**025** + **`/portal/support-requests`** + **`PortalSupportPage`**). Follow-up: admin list/workflow on `hotel_portal_support_requests` if product wants in-app triage.

---

## 7. Deferred / out of scope (this phase)

- **Support > Chat:** No company dashboard route today; no WebSocket/third-party integration planned in this release.

This report can be used as the single source of truth for remaining missing features and prioritization.
