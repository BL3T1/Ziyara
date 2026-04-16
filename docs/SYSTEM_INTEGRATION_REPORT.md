# Ziyara System Integration Report

**Generated:** 2026-03-12 · **DB/migrations note refreshed:** 2026-04-04  
**Scope:** Backend (Spring Boot), Frontend (historically CRA; current app is **Vite** under `front/my-app`), Database (PostgreSQL), and their integration.

**Prefer for current paths and verification:** [SYSTEM_STATE_REPORT.md](SYSTEM_STATE_REPORT.md) §2 and [FULL_SYSTEM_REPORT.md](FULL_SYSTEM_REPORT.md).

---

## 1. Executive Summary

| Layer | Status | Base Path / Stack |
|-------|--------|-------------------|
| **Backend** | Implemented | `/api/v1` (context-path), Spring Boot, REST controllers |
| **Frontend** | Implemented | React (CRA), React Router, `REACT_APP_API_URL` → backend |
| **Database** | Implemented | PostgreSQL 15+, `database/schema.sql` + **001–015**, seed, **018–022** (see `database/apply-all.ps1`, `database/Dockerfile`) |
| **Integration** | Aligned | Frontend `services/api.js` uses axios → `lib/api.js` → backend; backend uses JPA → DB |

**Gaps addressed in this report:**
- Backend: ReportController role fix; optional ticket “my tickets” filter.
- Database: Migrations 011/012 already exist; no new migration required for current features.
- Frontend: Missing **Reports** (date range + API), **Users** (list + CRUD/freeze/reset), **Provider detail** (GET by id), **Portal Listings** (full CRUD), **Portal Earnings** display; **Booking voucher** button.

---

## 2. Backend Overview

### 2.1 API Base and Security

- **Context path:** `application.yml` → `context-path: /api/v1`. All endpoints are under `http://<host>:8080/api/v1/...`.
- **Auth:** JWT (Bearer). Login: `POST /auth/login`; Register: `POST /auth/register`; Refresh: `POST /auth/refresh`.
- **Roles:** Stored in `users.role` (user_role_enum). Used in `@PreAuthorize` (e.g. `SUPER_ADMIN`, `HR_MANAGER`, `FINANCE_MANAGER`). **Note:** Some controllers reference `ADMIN` which is not in `UserRole` enum; reports should use `SUPER_ADMIN` or manager roles.

### 2.2 Controllers and Endpoints (Implemented)

| Controller | Path | Key Endpoints | Integration with Frontend |
|------------|------|----------------|---------------------------|
| **AuthController** | `/auth` | login, register, logout, refresh, password/forgot, password/reset, otp/send, otp/verify | ✅ authAPI |
| **UserController** | `/users` | GET/PUT /me, POST /me/change-password, GET/POST/PUT/DELETE /users, GET /{id}, login-history, freeze, unfreeze, reset-password | ⚠️ userAPI missing list, get, create, update, delete, freeze(id), unfreeze(id), resetPassword(id) for admin |
| **ServiceController** | `/services` | GET, GET /search, GET /{id}, GET /{id}/availability, GET /{id}/images, POST, PUT /{id}, DELETE /{id} | ✅ serviceAPI |
| **ServiceProviderController** | `/providers` | GET, GET /me, PUT /me, GET /{id}, POST, PUT /{id}, DELETE /{id}, approve, suspend | ✅ providersAPI, portalAPI (profile = /providers/me) |
| **BookingController** | `/bookings` | GET, GET /admin, GET /{id}, GET /reference/{ref}, POST, PUT /{id}, confirm, taxi, voucher, cancel | ✅ bookingAPI |
| **PaymentController** | `/payments` | POST, initiate, complete, fail, refund, GET, GET /{id}, GET /transaction/{ref} | ✅ paymentAPI |
| **DiscountController** | `/discounts` | GET, GET /{id}, POST, PUT /{id}, DELETE, approve, deactivate, validate, **apply** | ✅ discountAPI |
| **PortalController** | `/portal` | GET /dashboard, GET/POST/PUT/DELETE /services, GET /bookings, GET /earnings | ✅ portalAPI |
| **DashboardController** | `/dashboard` | revenue, bookings, customers, providers, kpis, activity, service-health, commission-analysis, payouts | ✅ dashboardAPI |
| **ReportController** | `/reports` | GET /revenue (start, end), GET /bookings (start, end) | ✅ reportsAPI; ⚠️ @PreAuthorize uses 'ADMIN' (not in UserRole) |
| **ComplaintController** | `/complaints` | Full CRUD + assign, resolve, escalate, close, comments | ✅ complaintAPI |
| **InternalTicketController** | `/tickets` | Full CRUD + workflow (acknowledge, assign, start-progress, resolve, close, reopen, cancel), comments, stats, overdue | ✅ ticketAPI; frontend uses list/get; no “my tickets” filter by creator |
| **RoleManagementController** | `/roles` | GET, GET /{id}, groups, permissions/catalogue, permissions/unlocked, POST, **PUT /{id}**, PUT /{id}/permissions, DELETE | ✅ rolesAPI |
| **Others** | departments, employees, notifications, currency, pricing, audit-logs, taxi-bookings, reviews, pay webhooks | Partially used by frontend |

### 2.3 Missing or Inconsistent Backend Items

| Item | Severity | Action |
|------|----------|--------|
| **ReportController** `@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")` | Medium | Change to roles that exist: e.g. `SUPER_ADMIN`, `FINANCE_MANAGER`, `GENERAL_MANAGER`, `CEO` so reports are accessible to dashboard roles. |
| **Tickets “my tickets”** | Low | Optional: add query param `createdBy=me` or filter by current user when role is not support/admin so customers see only their tickets. |

---

## 3. Frontend Overview

### 3.1 Structure (Post–Plan)

- **Layouts:** PublicLayout, AuthLayout, PortalLayout, DashboardLayout (with DashboardSidebar).
- **Zones:** Public (`/`, `/about`, `/pricing`, `/hotels`, `/restaurants`, `/trips`, `/services/:id`, auth), Browsing/Booking (protected), Dashboard (`/dashboard/*`), Portal (`/portal/*`), Customer (`/my-bookings`, `/profile`, `/support`).
- **API client:** `lib/api.js` (axios + interceptors), `services/api.js` (method objects). Response envelope: backend returns `ApiResponse` (success, data, message); frontend often uses `response?.data` or `response?.data?.data` depending on interceptor.

### 3.2 Frontend API vs Backend

| Frontend API | Backend Endpoint | Status |
|--------------|------------------|--------|
| authAPI | /auth/* | ✅ |
| userAPI | /users/me, /users/me/change-password, freeze (self only) | ⚠️ Missing admin: list, get(id), create, update(id), delete(id), freeze(id), unfreeze(id), resetPassword(id) |
| serviceAPI | /services/* including availability, images | ✅ |
| bookingAPI | /bookings/* including voucher | ✅ |
| paymentAPI | /payments/* | ✅ |
| discountAPI | /discounts/* including apply | ✅ |
| portalAPI | /portal/dashboard, /portal/services, /portal/bookings, /portal/earnings; providersAPI.getMe, updateMe | ✅ |
| reportsAPI | /reports/revenue, /reports/bookings | ✅ |
| providersAPI | /providers/*, /providers/me | ✅ |
| ticketAPI | /tickets (list, get, create, comments, etc.) | ✅ (getMyTickets = list with optional filter) |

### 3.3 Missing Frontend Pages / Buttons

| Page / Area | Missing | Backend Ready |
|-------------|---------|----------------|
| **Dashboard > Reports** | Date range picker, call reportsAPI.getRevenueReport/getBookingReport, show tables | ✅ |
| **Dashboard > Users** | List users (userAPI.list), Create/Edit/Delete/Freeze/Unfreeze/Reset password buttons and modals | ✅ (backend has all) |
| **Dashboard > Provider detail** | Load provider by id (providersAPI.get), display info; link from Providers table | ✅ |
| **Dashboard > Bookings** | Optional: “Voucher” button calling bookingAPI.getVoucher(id) | ✅ |
| **Portal > Listings** | Full list (portalAPI.getServices), Add/Edit/Delete service (createService, updateService, deleteService) | ✅ |
| **Portal > Earnings** | Display portalAPI.getEarnings() with date range | ✅ |
| **Portal > Profile** | Form using providersAPI.getMe/updateMe | ✅ (partially present) |

---

## 4. Database Overview

### 4.1 Schema and Migrations

- **Base schema:** `database/schema.sql` (enums, core tables: users, roles, permissions, departments, employees, customers, service_providers, services, service_images, bookings, payments, refunds, discount_codes, complaints, reviews, internal_tickets, notifications, audit_logs, taxi_bookings, exchange_rates).
- **Migrations:** Numbered scripts through **022** (incl. table prefixes **015**, post-seed **018–022** for sample data, settings/contact leads, RBAC catalogue). Apply order: schema → **001–015** → seed → **018–022** (`database/apply-all.ps1` and `database/Dockerfile`).

### 4.2 Entity–Schema Alignment

- **011:** Enum columns converted to VARCHAR for Hibernate (roles, bookings, complaints, internal_tickets, refunds, notifications, taxi_bookings, services).
- **012:** departments.manager_id; notifications (message, template_name, updated_at); refunds (currency, transaction_reference).

No additional migrations are required for the backend/frontend features listed in this report; 011/012 cover current entity expectations.

---

## 5. Integration Summary

### 5.1 Request Flow

1. **Frontend:** User action → component calls `services/api.js` (e.g. reportsAPI.getRevenueReport(start, end)) → `lib/api.js` (axios) adds `Authorization: Bearer <token>` and `Accept-Language` → HTTP GET `http://<API_URL>/api/v1/reports/revenue?start=...&end=...`.
2. **Backend:** Security validates JWT and roles → Controller → Service → Repository (JPA) → Database.
3. **Response:** Backend returns `ApiResponse.success(data)` → frontend receives body; axios interceptor may return `response.data` so payload is `{ success, data, message }`; 401 triggers redirect to login.

### 5.2 Envelope Handling

- Backend: `ApiResponse.success(payload)` → `{ "success": true, "data": payload, "message": null }`.
- Frontend: Use `res?.data` for payload when interceptor returns full body; for list endpoints, `res?.data` may be the list or `res?.data?.data` if backend nests (confirm per endpoint).

### 5.3 Cross-Cutting Gaps

| Gap | Where | Recommendation |
|-----|--------|----------------|
| Role name `ADMIN` | ReportController | Use existing roles (e.g. SUPER_ADMIN, FINANCE_MANAGER, GENERAL_MANAGER, CEO). |
| userAPI admin methods | services/api.js | Add list, get(id), create, update(id), delete(id), freeze(id), unfreeze(id), resetPassword(id). |
| Reports page | app/dashboard/reports/ReportsPage.jsx | Implement UI + reportsAPI calls. |
| Users page | app/dashboard/users/UsersPage.jsx | Implement list + CRUD and freeze/unfreeze/reset actions. |
| Provider detail | app/dashboard/providers/ProviderDetailPage.jsx | Implement providersAPI.get(id) + display. |
| Portal Listings | app/portal/listings/PortalListingsPage.jsx | Implement list + create/edit/delete with portalAPI. |
| Portal Earnings | app/portal/earnings/PortalEarningsPage.jsx | Implement getEarnings with optional date range and display. |
| Booking voucher | Bookings list/detail | Add “Voucher” button using bookingAPI.getVoucher(id). |

---

## 6. Implemented Fixes and Additions (This Session)

- **Backend:** ReportController `@PreAuthorize` updated to valid roles: `SUPER_ADMIN`, `FINANCE_MANAGER`, `GENERAL_MANAGER`, `CEO` (removed non-existent `ADMIN`).
- **Frontend – userAPI:** Added list, get, create, update, delete, freeze, unfreeze, resetPassword (by id) for admin user management.
- **Frontend – Reports page:** Date range picker, Run reports button, revenue report (total + by-day table), booking report (total + by-day table) via reportsAPI.
- **Frontend – Users page:** List users (userAPI.list), Create user (email, password, phone, role), Edit (email, phone, status), Freeze/Unfreeze, Reset password, Delete with modals.
- **Frontend – Provider detail page:** Load provider by id (providersAPI.get), display company name, contact, status, commission, address; Back link to /dashboard/providers. Providers table “View” link updated to `/dashboard/providers/:id` (React Router Link).
- **Frontend – Portal Listings:** List (portalAPI.getServices, handle paginated content), Add listing (createService with providerId from getMe), Edit/Delete with modals.
- **Frontend – Portal Earnings:** Date range, Apply button, display totalEarnings and currency from portalAPI.getEarnings(start, end).
- **Frontend – Portal overview:** Stats use backend field names (totalRevenue, revenueCurrency, totalBookings, serviceCount).
- **Database:** No new migration added; 011/012 cover current entity/schema alignment.

---

## 7. Checklist: Remaining Optional Items

- [ ] Tickets: backend filter “my tickets” for non-admin users (optional).
- [ ] Dashboard Bookings: voucher button/link (optional).
- [ ] Settings / API key management pages and backend (optional).
- [ ] Company/partner landing page and contact form (optional).

---

*Report generated from backend controllers, frontend `App.js` and `services/api.js`, and `database/schema.sql` + migrations.*
