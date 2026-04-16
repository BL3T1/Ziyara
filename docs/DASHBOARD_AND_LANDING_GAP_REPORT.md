# Ziyara – Gap Report: Company Dashboard, Providers Dashboard & Company Landing

This report lists **missing backend endpoints/functions** and **missing frontend pages/buttons** required for a fully functional company dashboard, providers (portal) dashboard, and company landing experience. It is based on the current `backend` and `front/my-app` (and where relevant `frontend`) codebases.

---

## 1. Executive Summary

| Area | Backend gaps | Frontend gaps |
|------|--------------|---------------|
| **Company dashboard** | Reports API not used; no settings/API config endpoints | Discounts page is placeholder; Reports page missing; Settings/API placeholders; some action buttons missing |
| **Providers dashboard** | No provider-scoped APIs (my services, my bookings, my earnings, staff) | Portal list/bookings/earnings/staff/profile/support are placeholder or use company-wide KPIs |
| **Company landing** | N/A (static/marketing) | No dedicated B2B/company landing page; only customer HomePage and role-select |

---

## 2. Company Dashboard

### 2.1 Backend – Already Present

- **Dashboard:** `/dashboard/kpis`, `/dashboard/activity`, `/dashboard/service-health`, `/dashboard/commission-analysis`, `/dashboard/payouts`, `/dashboard/revenue`, `/dashboard/bookings`, `/dashboard/customers`, `/dashboard/providers`
- **Reports:** `/reports/revenue`, `/reports/bookings` (date range)
- **Management:** Users (list, get, create, update, delete, freeze, unfreeze, reset-password), Providers (list, get, create, update, PATCH commission, approve, suspend, delete), Bookings (list, admin list, get, confirm, cancel, voucher, taxi), Payments (list, get, refund), Discounts (list, get, create, update, delete, approve, deactivate, validate)
- **Support:** Complaints (list, get, create, update, assign, resolve, escalate, close, comments), Tickets (full workflow + stats, overdue, comments)
- **Admin:** Roles (list, get, groups, permission catalogue, create, update permissions, delete), Audit logs (entity, user, recent with search)
- **Other:** Services, Auth, Notifications (list, get, mark read, read-all), Currency (rates, convert)

### 2.2 Backend – Missing or Incomplete

| Item | Description |
|------|-------------|
| **Reports usage** | Report endpoints exist but are not exposed in `front/my-app` API client; no dedicated “Reports” UI flow (export, date range, download). |
| **Settings / API config** | No backend endpoints for “Admin > Settings” or “Admin > API” (e.g. API keys, feature flags, company name). These are placeholders on front; backend would need at least a minimal config or API-key resource. |
| **User “me”** | `frontend` `api.js` calls `/users/me`, `/users/me/change-password`, `/users/me/freeze`. Backend has `/users/{id}` and `/users/{id}/login-history` plus freeze/unfreeze by `{id}`. No `/users/me` (current user profile) or `/users/me/change-password` – needed if the same API serves profile/account screens. |

### 2.3 Frontend (Company Dashboard) – Missing Pages / Features

| Page / Feature | Status | What’s missing |
|----------------|--------|------------------|
| **Discounts** | Placeholder | Full page: list, filters, create/edit/delete, approve/deactivate; wire to `discountsAPI` (not in `front/my-app` `api.ts`). |
| **Reports** | Missing | No route or page for revenue/bookings reports; no date-range picker, table, or export; `reportsAPI` not in `front/my-app` `api.ts`. |
| **Settings** | Placeholder | Real Settings page and any backend for app/company settings. |
| **API (Admin)** | Placeholder | API keys or API documentation page and any backend. |
| **Ticket detail** | Placeholder | Real ticket detail page with comments, assign, status workflow (acknowledge, start-progress, resolve, close, etc.). |
| **Support Chat** | Placeholder | Real chat UI and backend (if required). |
| **Bookings** | List only | No row actions: View detail, Confirm, Cancel, Voucher link. |
| **Complaints** | List only | No row actions: View detail, Assign, Resolve, Escalate, Close, Comments. |
| **Tickets** | List only | No link to ticket detail; no Assign / Resolve / Close etc. from list. |
| **Users** | List by group | No Create User, Edit User, Freeze/Unfreeze, Reset password buttons; no user detail modal/page. |
| **Roles** | List only | No Create role, Edit permissions, Delete (with reassignment) modals/flows. |
| **Payments** | List only | No Refund button or payment detail modal. |
| **Providers** | List + commission | No Approve / Suspend buttons; no “View provider” detail page. |

---

## 3. Providers Dashboard (Portal)

### 3.1 Backend – Missing (Provider-Scoped APIs)

Provider portal currently uses **company-wide** `/dashboard/kpis` (and same dashboard API). There are **no provider-scoped** endpoints for:

| Endpoint / area | Purpose |
|-----------------|---------|
| **GET /portal/dashboard** or **GET /dashboard/portal** | KPIs for the logged-in provider only (upcoming bookings, revenue, pending tasks). |
| **GET /portal/services** or **GET /services?providerId=current** | List only the current provider’s services (listings). |
| **POST /portal/services**, **PUT /portal/services/{id}**, **DELETE /portal/services/{id}** | Create/update/delete provider’s own listings (or reuse `/services` with provider context). |
| **GET /portal/bookings** | Bookings for the current provider’s services only. |
| **GET /portal/earnings** or **GET /portal/payouts** | Earnings/payouts for the current provider. |
| **GET /portal/staff**, **POST /portal/staff**, etc. | Staff management for the provider (if required). |
| **GET /portal/profile**, **PUT /portal/profile** | Provider’s own profile (may be covered by **GET/PUT /providers/{id}** with `id = current` if backend supports “current provider” from JWT). |

So: **provider-scoped dashboard, listings, bookings, earnings, and optionally staff and profile** are the main backend gaps for a full provider dashboard.

### 3.2 Frontend (Portal) – Missing Pages / Features

| Page / Feature | Status | What’s missing |
|----------------|--------|------------------|
| **Portal overview** | Exists | Uses company KPIs; should use provider-scoped KPIs when backend exists. |
| **Listings** | Placeholder | Full page: list provider’s services, add/edit/delete listings; needs provider-scoped services API. |
| **Bookings** | Placeholder | List and manage provider’s bookings; needs provider-scoped bookings API. |
| **Staff** | Placeholder | UI and backend for provider staff (if in scope). |
| **Earnings** | Placeholder | Earnings/payouts table and filters; needs provider-scoped earnings/payouts API. |
| **Profile** | Placeholder | View/edit provider profile; can use `/providers/{id}` with current provider id once available. |
| **Support** | Placeholder | Provider-facing support (tickets or chat) and any provider-scoped support API. |

---

## 4. Landing Page for the Company

### 4.1 Current State

- **Customer-facing:** `frontend` has a **HomePage** (hero, search, service categories, featured services) – this is the main “landing” for end users.
- **Post-login entry:** In `front/my-app`, **“/”** is **RoleSelectPage** (choose company role or provider portal), then redirect to dashboard or portal. There is no public marketing landing before login.

### 4.2 Missing for a “Company Landing” (B2B / Marketing)

A dedicated **company landing page** (e.g. “Ziyara for Business” / “Partner with us”) does not exist. Missing pieces:

| Item | Description |
|------|-------------|
| **Public company/partner landing route** | A route like `/company`, `/for-business`, or `/partners` that is public (no auth). |
| **Page content** | Hero, value proposition for companies/partners, provider benefits, CTA (e.g. “Become a provider”, “Contact sales”), maybe testimonials or logos. |
| **Optional backend** | “Contact us” or “Request demo” form could require a simple backend endpoint (e.g. POST `/public/contact` or `/lead`) if you want to store leads. |

So: **no dedicated company/partner landing page or CTA flow** in the current codebase.

---

## 5. Backend – Consolidated List of Missing / To-Add

| # | Area | Missing endpoint / function |
|---|------|-----------------------------|
| 1 | Users | `GET /users/me` – current user profile. |
| 2 | Users | `PUT /users/me` – update current user profile (or document that frontend uses `PUT /users/{id}` with self). |
| 3 | Users | `POST /users/me/change-password` – change password for current user (or use existing reset by id for self). |
| 4 | Services | `GET /services/{id}/availability` – check availability (frontend `api.js` calls it; not in ServiceController). |
| 5 | Services | `GET /services/{id}/images` – service images (frontend `api.js` calls it; not in ServiceController). |
| 6 | Discounts | `POST /discounts/apply` – apply discount to a booking (frontend has `discountAPI.apply(code, bookingId)`; backend has only validate). |
| 7 | Provider portal | Provider-scoped dashboard: e.g. `GET /dashboard/portal` or `GET /portal/dashboard` (KPIs for current provider). |
| 8 | Provider portal | Provider-scoped listings: e.g. `GET /portal/services` and CUD for own services (or enforce providerId in existing `/services`). |
| 9 | Provider portal | Provider-scoped bookings: e.g. `GET /portal/bookings`. |
| 10 | Provider portal | Provider-scoped earnings/payouts: e.g. `GET /portal/earnings` or `GET /portal/payouts`. |
| 11 | Provider portal | Optional: provider staff CRUD and `GET/PUT /portal/profile` (or “current provider” from JWT on existing `/providers/{id}`). |
| 12 | Settings/API | Optional: endpoints for Admin Settings and API keys/config (if you want non-placeholder Admin > Settings / API). |
| 13 | Company landing | Optional: e.g. `POST /public/contact` or `POST /leads` for “Contact us” / “Request demo” on company landing. |

---

## 6. Frontend – Consolidated List of Missing Pages & Buttons

### 6.1 Company dashboard (front/my-app)

| # | Item | Type |
|---|------|------|
| 1 | **Discounts** | Full page (list, create, edit, delete, approve, deactivate) + add `discountsAPI` to `api.ts`. |
| 2 | **Reports** | New page (revenue/bookings, date range, table/export) + add `reportsAPI` to `api.ts`. |
| 3 | **Settings** | Replace placeholder with real page (and optional backend). |
| 4 | **Admin API** | Replace placeholder with API keys or docs (and optional backend). |
| 5 | **Ticket detail** | Replace placeholder with full ticket view + comments + workflow actions. |
| 6 | **Support Chat** | Replace placeholder if chat is in scope. |
| 7 | **Bookings** | Row actions: View, Confirm, Cancel, Voucher. |
| 8 | **Complaints** | Row actions: View, Assign, Resolve, Escalate, Close, Comments. |
| 9 | **Tickets** | Link to ticket detail + row actions (Assign, Resolve, Close, etc.). |
| 10 | **Users** | Create, Edit, Freeze/Unfreeze, Reset password (+ user detail). |
| 11 | **Roles** | Create role, Edit permissions, Delete with reassignment. |
| 12 | **Payments** | Refund action + detail view. |
| 13 | **Providers** | Approve, Suspend + provider detail page. |

### 6.2 Provider portal (front/my-app)

| # | Item | Type |
|---|------|------|
| 14 | **Portal Listings** | Full page + provider-scoped services API. |
| 15 | **Portal Bookings** | Full page + provider-scoped bookings API. |
| 16 | **Portal Staff** | Full page (if in scope) + backend. |
| 17 | **Portal Earnings** | Full page + provider-scoped earnings/payouts API. |
| 18 | **Portal Profile** | Full page (view/edit) using current provider. |
| 19 | **Portal Support** | Full page (tickets or chat) + provider-scoped support if needed. |
| 20 | **Portal overview** | Switch to provider-scoped dashboard API when available. |

### 6.3 Company landing

| # | Item | Type |
|---|------|------|
| 21 | **Company/partner landing** | New public page (e.g. `/company` or `/for-business`) with hero, value prop, CTA. |
| 22 | **Optional** | “Contact us” / “Request demo” form + backend for leads. |

---

## 7. API Client Gaps (front/my-app `api.ts`)

- **discountsAPI:** Add list, get, create, update, delete, approve, deactivate, validate (and optionally apply when backend exists).
- **reportsAPI:** Add e.g. `getRevenueReport(start, end)`, `getBookingReport(start, end)`.
- **usersAPI:** Add create, update, delete, freeze, unfreeze, resetPassword if management UI will call them (backend already has these).

---

## 8. Summary Table

| Scope | Backend missing | Frontend missing |
|-------|-----------------|------------------|
| **Company dashboard** | Reports not wired; no Settings/API config; no `/users/me` (and related) if needed | Discounts page; Reports page; Settings/API pages; Ticket detail; Chat; Row actions on Bookings, Complaints, Tickets; User/Role/Payment/Provider actions |
| **Providers dashboard** | All provider-scoped APIs (dashboard, listings, bookings, earnings, staff, profile) | All portal pages beyond overview (listings, bookings, staff, earnings, profile, support); overview should use provider KPIs |
| **Company landing** | Optional contact/lead endpoint | Dedicated company/partner landing page and optional form |

This report can be used as a checklist for implementation and prioritization.
