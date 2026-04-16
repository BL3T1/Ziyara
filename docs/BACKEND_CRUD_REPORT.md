# Ziyara Backend – CRUD Functions Report

This report lists **CRUD (Create, Read, Update, Delete)** and related operations in the backend, by resource. It includes what exists and what is **missing**.

**Conventions:**
- **Read (list)** = GET collection (e.g. `GET /users`)
- **Read (one)** = GET by ID or key (e.g. `GET /users/{id}`)
- **Create** = POST to collection
- **Update** = PUT or PATCH on resource
- **Delete** = DELETE on resource

---

## 1. CRUD Matrix by Resource

| Resource | Base path | Create | Read (list) | Read (one) | Update | Delete | Notes |
|----------|-----------|:------:|:-----------:|:----------:|:------:|:------:|-------|
| **Users** | `/users` | ✅ POST | ✅ GET | ✅ GET /me, GET /{id} | ✅ PUT /me, PUT /{id} | ✅ DELETE /{id} | Freeze, unfreeze, reset-password, login-history. Full CRUD. |
| **Services** | `/services` | ✅ POST | ✅ GET | ✅ GET /{id}, GET /search, GET /{id}/availability, GET /{id}/images | ✅ PUT /{id} | ✅ DELETE /{id} | Availability check and images are implemented. |
| **Service providers** | `/providers` | ✅ POST | ✅ GET | ✅ GET /{id}, GET /me | ✅ PUT /{id}, PUT /me, PATCH /{id}/commission | ✅ DELETE /{id} | Approve, suspend. Current provider is exposed via GET/PUT /providers/me. |
| **Bookings** | `/bookings` | ✅ POST | ✅ GET, GET /admin | ✅ GET /{id}, GET /reference/{ref} | ✅ PUT /{id} | ❌ | Confirm, cancel, voucher, taxi. No soft-delete. |
| **Payments** | `/payments` | ✅ POST, POST /initiate | ✅ GET | ✅ GET /{id}, GET /transaction/{ref} | ❌ | ❌ | Complete, fail, refund. Payments are immutable after create. |
| **Discounts** | `/discounts` | ✅ POST | ✅ GET | ✅ GET /{id} | ✅ PUT /{id} | ✅ DELETE /{id} | Approve, deactivate, validate, **POST /apply** (apply to booking). |
| **Complaints** | `/complaints` | ✅ POST | ✅ GET | ✅ GET /{id} | ✅ PUT /{id} | ❌ | Assign, resolve, escalate, close, comments. No delete (by design). |
| **Internal tickets** | `/tickets` | ✅ POST | ✅ GET | ✅ GET /{id} | ✅ PUT /{id} | ✅ DELETE /{id} | Full workflow + comments, stats, overdue. Full CRUD. |
| **Reviews** | `/reviews` | ✅ POST | ✅ **GET** (admin paginated), GET /service/{serviceId} | ✅ GET /{id} | ✅ PUT /{id} | ✅ DELETE /{id} | Moderate, respond. `GET /reviews` requires company staff JWT. |
| **Roles** | `/roles` | ✅ POST | ✅ GET | ✅ GET /{id} | ✅ **PUT /{id}** (name/description), **PUT /{id}/permissions** | ✅ DELETE /{id} | Groups, permission catalogue, navigation; system vs custom permission rules. |
| **Employees** | `/employees` | ✅ POST | ✅ GET | ✅ GET /{id} | ✅ PUT /{id} | ✅ DELETE /{id} | Offboard implemented via DELETE /{id}. |
| **Departments** | `/departments` | ✅ POST | ✅ GET | ✅ GET /{id} | ✅ PUT /{id} | ✅ DELETE /{id} | Full CRUD, including get/update/delete by ID. |
| **Notifications** | `/notifications` | ✅ POST | ✅ GET | ✅ GET /{id} | ❌ (PATCH /{id}/read only) | ❌ | Mark read, read-all. No update/delete (by design). |
| **Currency / exchange rates** | `/currency` | ✅ POST /rates | ✅ GET /rates | ❌ | ✅ PUT /rates/{id} | ❌ | **Missing:** GET /rates/{id}, DELETE /rates/{id}. Convert is utility. |
| **Audit logs** | `/audit-logs` | ❌ | ✅ GET, GET /entity/..., GET /user/... | ❌ | ❌ | ❌ | Read-only (by design). |
| **Dashboard / Reports** | `/dashboard`, `/reports` | ❌ | GET only | — | ❌ | ❌ | Query/read-only. |
| **Auth** | `/auth` | Register, login, etc. | — | — | — | — | Not CRUD resource. |
| **Pricing** | `/pricing` | — | — | — | — | — | POST /preview only. |
| **Taxi bookings** | `/taxi-bookings` | ❌ | ✅ GET /active | ❌ | ✅ PATCH /{id}/status | ❌ | **Missing:** GET /{id}, list all, create (create may be via booking). |

---

## 2. Sub-Resources and Nested CRUD

| Parent | Sub-resource | Create | Read list | Read one | Update | Delete |
|--------|--------------|:------:|:---------:|:--------:|:------:|:------:|
| Complaints | Comments | ✅ POST /{id}/comments | ✅ GET /{id}/comments | — | — | ❌ |
| Tickets | Comments | ✅ POST /{id}/comments | ✅ GET /{id}/comments | — | — | ❌ |

**Service images (implemented on `ServiceController`):** `GET|POST|PUT|DELETE /services/{id}/images`, `POST /services/{id}/images/upload` (multipart).

---

## 3. Missing CRUD (Consolidated)

### 3.1 Missing “Read” operations

| Resource | Missing | Suggested endpoint |
|----------|---------|---------------------|
| Services | Availability check | — (implemented as **GET /services/{id}/availability**) |
| Services | Service images | — (implemented as **GET /services/{id}/images**) |
| Departments | Get by ID | — (implemented as **GET /departments/{id}**) |
| Currency | Get rate by ID | **GET /currency/rates/{id}** (optional) |
| Service providers | Current provider | — (implemented as **GET /providers/me**) |
| Reviews | List all (admin) | — (**GET /reviews** implemented) |
| Taxi bookings | Get by ID | **GET /taxi-bookings/{id}** (optional) |

### 3.2 Missing “Create” operations

| Resource | Missing | Suggested endpoint |
|----------|---------|---------------------|
| Discounts (usage) | Apply to booking | — (implemented as **POST /discounts/apply**) |
| Portal | Provider-scoped resources | — (implemented as **POST /portal/services** – create own listing) |

### 3.3 Missing “Update” operations

| Resource | Missing | Suggested endpoint |
|----------|---------|---------------------|
| Roles | Update role name/description | — (**PUT /roles/{id}** implemented) |
| Departments | Update department | — (implemented as **PUT /departments/{id}**) |

### 3.4 Missing “Delete” operations

| Resource | Missing | Suggested endpoint |
|----------|---------|---------------------|
| Departments | Delete department | — (implemented as **DELETE /departments/{id}**) |
| Employees | Offboard employee | — (implemented as **DELETE /employees/{id}**) |
| Currency | Delete rate | **DELETE /currency/rates/{id}** |
| Services | Delete image | **DELETE /services/{id}/images/{imageId}** (if images CRUD is added) |

---

## 4. Provider Portal (Scoped CRUD)

These are **provider-scoped** resources (current provider from JWT). Core endpoints for dashboard, services, bookings, earnings, and provider profile are implemented.

| Resource | Suggested base path | Create | Read list | Read one | Update | Delete |
|----------|---------------------|:------:|:---------:|:--------:|:------:|:------:|
| Portal dashboard | `/portal/dashboard` | — | — | ✅ (KPIs) | — | — |
| Portal services (listings) | `/portal/services` | ✅ POST | ✅ GET | ✅ GET /{id} (via service detail) | ✅ PUT /{id} | ✅ DELETE /{id} |
| Portal bookings | `/portal/bookings` | — | ✅ GET | — | — | — |
| Portal earnings/payouts | `/portal/earnings` | — | ✅ GET | — | — | — |
| Portal profile | `GET/PUT /providers/me` | — | ✅ GET | — | ✅ PUT | — |
| Portal staff | `/portal/staff` (if in scope) | ❌ | ❌ | ❌ | ❌ | ❌ |

**Implemented (summary):**
- **GET /portal/dashboard** – KPIs for current provider.
- **GET /portal/services** – list current provider’s services.
- **POST /portal/services**, **PUT /portal/services/{id}**, **DELETE /portal/services/{id}** – CUD for own listings.
- **GET /portal/bookings** – list bookings for current provider’s services.
- **GET /portal/earnings** – list earnings/payouts for current provider.
- **GET/PUT /providers/me** – current provider profile for portal.

---

## 5. Resources With No Dedicated CRUD API

These domain entities either have no REST CRUD or are handled only as part of another resource:

| Entity | Current exposure | Note |
|--------|-------------------|------|
| **Customer** | Indirect (e.g. booking customerId) | No `/customers` CRUD. May be acceptable if “customer” is a user profile. |
| **ServiceImage** | None | Service images not exposed; add under `/services/{id}/images` if needed. |
| **Refund** | Via Payment refund | Refund created via POST /payments/{id}/refund; no standalone Refund CRUD. |
| **TaxiBooking** | Partial | Only GET /taxi-bookings/active, PATCH status, POST assign; no full CRUD. |
| **Group** (roles) | Read-only via `/roles/groups` | No create/update/delete for groups. |
| **Permission** | Read-only catalogue | No CRUD; permissions are system-defined. |
| **AuditLog** | Read-only | By design, no create/update/delete. |

---

## 6. Checklist: Missing CRUD to Implement

### High impact (core resources)
- [x] **GET /services/{id}/availability** – availability check.
- [x] **GET /services/{id}/images** – list images.
- [x] **POST /discounts/apply** – apply discount to a booking.
- [x] **GET /providers/me** – current provider profile for portal.
- [x] **PUT /departments/{id}** – update department.
- [x] **GET /departments/{id}** – get department by ID.
- [x] **DELETE /departments/{id}** – delete department (with constraints).
- [x] **DELETE /employees/{id}** – offboard employee.

### Portal (provider-scoped)
- [x] **GET /portal/dashboard**.
- [x] **GET /portal/services** (+ **POST**, **PUT /portal/services/{id}**, **DELETE**).
- [x] **GET /portal/bookings**.
- [x] **GET /portal/earnings**.
- [x] **GET /portal/profile**, **PUT /portal/profile** (via **GET/PUT /providers/me**).

### Optional / lower priority
- [ ] **PUT /roles/{id}** – update role name/description.
- [ ] **GET /reviews** – list all reviews (admin) with filters.
- [ ] **GET /currency/rates/{id}**, **DELETE /currency/rates/{id}**.
- [ ] **GET /taxi-bookings/{id}** (and full list if needed).
- [ ] **Services images:** POST/DELETE for `/services/{id}/images` if managing images via API.

---

## 7. Summary Table

| Category | Present | Missing |
|----------|---------|---------|
| **Full CRUD (list, get, create, update, delete)** | Users, Services, Service providers, Discounts, Internal tickets, Reviews, Roles (partial: no PUT role body), Employees (no delete) | — |
| **Create + Read only** | Bookings, Payments, Complaints, Notifications, Departments (no get/update/delete), Currency rates (no get-by-id/delete) | — |
| **Read only** | Dashboard, Reports, Audit logs | — |
| **Missing CRUD or sub-resources** | — | Services (availability, images), Discounts (apply), Departments (get, update, delete), Employees (delete), Currency (get/delete rate), Roles (PUT body), Portal (all scoped CRUD) |

This report can be used as the single reference for backend CRUD coverage and missing operations.
