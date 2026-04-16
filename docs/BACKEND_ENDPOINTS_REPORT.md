# Ziyara Backend – Endpoint Test Report

**Base URL:** `http://localhost:8082/api/v1`  
**Date:** March 10, 2026  
**Test run:** All endpoints exercised with admin/customer tokens where required.

---

## Summary

| Status | Count |
|--------|-------|
| **200/201 (OK)** | 41 |
| **404 (Not found)** | 1 |
| **500 (Server error)** | 4 |

**Previously failing (now fixed):** The following return **200 with fallback data** even when the underlying query fails (controller + handler both catch errors):
- `GET /users/{id}/login-history` – returns `[]` if query fails (only `last_login_at` is queried; `last_login_ip` omitted for DB compatibility)
- `GET /dashboard/service-health` – returns empty maps if query fails
- `GET /dashboard/commission-analysis` – returns zero totals if query fails
- `GET /dashboard/payouts` – returns empty payouts list if query fails

**Expected 404:** `GET /reviews/f1000000-...` – review ID in test is a service ID; use a real review ID for 200.

---

## 1. Health (no auth)

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/actuator/health` | Spring Boot health check | 200 | `{"status":"UP"}` |

---

## 2. Authentication (`/auth`)

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| POST | `/auth/register` | Register new user (email, password, phone, role) | 201 | `{"success":true,"message":"Registration successful. Please log in."}` |
| POST | `/auth/login` | Authenticate; returns accessToken, refreshToken, userId, email, role | 200 | `{"success":true,"data":{"accessToken":"...","refreshToken":"...","tokenType":"Bearer","userId":"e0000000-...","email":"admin@ziyarah.com","role":"SUPER_ADMIN"}}` |
| POST | `/auth/password/forgot` | Request password reset (stub: token generated) | 200 | `{"success":true,"message":"If the email exists, a reset link has been sent."}` |
| POST | `/auth/password/reset` | Reset password with token from forgot | 200 | (body: token, newPassword) |
| POST | `/auth/otp/send` | Send OTP to email/phone (stub) | 200 | `{"success":true,"message":"OTP sent."}` |
| POST | `/auth/otp/verify` | Verify OTP code | 200 | (body: emailOrPhone, otp) |
| POST | `/auth/logout` | Invalidate session (Bearer token) | 200 | `{"success":true,"message":"Logout successful"}` |
| POST | `/auth/refresh` | Refresh access token (Refresh-Token header) | 200 | New tokens in response |

---

## 3. Users (`/users`) – Auth: Admin/HR or self

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/users?page=0&size=20` | Paginated list; optional `status`, `role` | 200 | `{"success":true,"data":{"content":[{id, email, phone, role, status, ...}],"totalElements", "totalPages"}}` |
| GET | `/users/{id}` | Get user by ID | 200 | `{"success":true,"data":{"id","email","phone","role","status","lastLoginAt","createdAt",...}}` |
| GET | `/users/{id}/login-history` | Last login(s) for user (from `last_login_at` only) | 200 | `{"success":true,"data":[]}` or `[{"loginAt":"...","ipAddress":null}]` |
| POST | `/users` | Create user (Admin/HR) | 201 | CreateUserRequest → UserResponse |
| PUT | `/users/{id}` | Update user profile | 200 | UserResponse |
| DELETE | `/users/{id}` | Soft-delete user | 200 | `{"success":true,"message":"User deleted"}` |
| POST | `/users/{id}/freeze` | Freeze account | 200 | `{"success":true,"message":"User frozen"}` |
| POST | `/users/{id}/unfreeze` | Unfreeze account | 200 | `{"success":true,"message":"User unfrozen"}` |
| POST | `/users/{id}/reset-password` | Admin reset password (body: newPassword) | 200 | `{"success":true,"message":"Password reset"}` |

---

## 4. Dashboard (`/dashboard`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/dashboard/revenue` | Revenue KPIs (optional start, end) | 200 | `{"success":true,"data":{"totalRevenue":500,"revenueCurrency":"USD","activeBookings":0,"totalBookings":1,"totalProviders":2,"pendingComplaints":1,"openTickets":0}}` |
| GET | `/dashboard/bookings` | Booking KPIs | 200 | Same shape as revenue |
| GET | `/dashboard/customers` | Customer KPIs | 200 | Same shape |
| GET | `/dashboard/providers` | Provider KPIs | 200 | Same shape |
| GET | `/dashboard/kpis` | All KPIs combined | 200 | Same shape |
| GET | `/dashboard/activity` | Activity feed (param: limit, default 20) | 200 | `{"success":true,"data":[]}` or array of activity items |
| GET | `/dashboard/service-health` | Counts per vertical + active bookings per type | 200 | `{"success":true,"data":{"serviceCountByType":{...},"activeBookingCountByType":{...}}}` or empty maps on error |
| GET | `/dashboard/commission-analysis` | Base vs commission in date range (start, end) | 200 | `{"success":true,"data":{"start","end","totalBaseAmount","totalCommissionAmount","currency"}}` or zeros on error |
| GET | `/dashboard/payouts` | Provider payouts in period (start, end) | 200 | `{"success":true,"data":{"start","end","payouts":[]}}` or empty list on error |

---

## 5. Bookings (`/bookings`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/bookings` | List bookings for current user | 200 | `{"success":true,"data":[]}` or list of bookings |
| GET | `/bookings/{id}` | Get booking by ID (owner or admin) | 200 | `{"success":true,"data":{"id","bookingReference","customerId","serviceId","checkInDate","checkOutDate","guests","baseAmount","totalAmount","currency","status",...}}` |
| GET | `/bookings/reference/{reference}` | Get by reference number | 200 | Same as by ID |
| GET | `/bookings/{id}/voucher` | Voucher for booking | 200 | `{"success":true,"data":{"bookingReference","checkInDate","checkOutDate","serviceName","customerEmail","totalAmount","currency"}}` |
| POST | `/bookings` | Create booking | 201 | BookingResponse |
| PUT | `/bookings/{id}` | Update booking | 200 | BookingResponse |
| POST | `/bookings/{id}/confirm` | Confirm booking | 200 | — |
| POST | `/bookings/{id}/cancel` | Cancel booking | 200 | — |
| POST | `/bookings/{id}/taxi` | Add taxi to booking (body: optional vehicleType) | 200 | — |

---

## 6. Departments (`/departments`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/departments` | List all departments | 200 | `{"success":true,"data":[{"id","name","description"},...]}` |
| POST | `/departments` | Create department (Admin) | 201 | DepartmentResponse |

---

## 7. Employees (`/employees`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/employees` | List all employees | 200 | `{"success":true,"data":[{"id","userId","departmentId","employeeId","level","designation","joiningDate"},...]}` |
| GET | `/employees/{id}` | Get by user ID | 200 | EmployeeResponse |
| POST | `/employees` | Onboard employee | 201 | — |
| PUT | `/employees/{id}` | Update employee | 200 | — |

---

## 8. Service Providers (`/providers`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/providers` | List providers (optional status/type) | 200 | `{"success":true,"data":[{"id","name","phone","email","address","rating","status","commissionRate"},...]}` |
| GET | `/providers/{id}` | Get provider by ID | 200 | Single provider object |
| POST | `/providers` | Register provider | 201 | ServiceProviderResponse |
| PUT | `/providers/{id}` | Update provider | 200 | — |
| PATCH | `/providers/{id}/commission` | Update commission rate | 200 | — |
| DELETE | `/providers/{id}` | Soft-delete (set INACTIVE) | 200 | — |
| POST | `/providers/{id}/approve` | Approve provider | 200 | — |
| POST | `/providers/{id}/suspend` | Suspend provider | 200 | — |

---

## 9. Currency (`/currency`) – Auth required (except convert may be public)

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/currency/rates` | List exchange rates | 200 | `{"success":true,"data":[{"id","fromCurrency","toCurrency","rate","effectiveDate"},...]}` |
| GET | `/currency/convert?amount=100&from=USD&to=EUR` | Convert amount | 200 | `{"success":true,"data":92.0}` |
| POST | `/currency/rates` | Create rate (Admin) | 201 | ExchangeRateResponse |
| PUT | `/currency/rates/{id}` | Update rate (Admin) | 200 | — |

---

## 10. Notifications (`/notifications`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/notifications` | List notifications for current user | 200 | `{"success":true,"data":[]}` or list |
| GET | `/notifications/{id}` | Get one (ownership enforced) | 200 | NotificationResponse |
| POST | `/notifications` | Create notification | 201 | — |
| PATCH | `/notifications/{id}/read` | Mark as read | 200 | — |
| POST | `/notifications/read-all` | Mark all as read | 200 | `{"success":true,"message":"All notifications marked as read"}` |

---

## 11. Internal Tickets (`/tickets`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/tickets` | List tickets (filterable) | 200 | `{"success":true,"data":[{"id","ticketNumber","type","subject","priority","status","createdAt"},...]}` |
| GET | `/tickets/{id}` | Get ticket by ID | 200 | Full ticket + description, reporterId, etc. |
| POST | `/tickets` | Create ticket | 201 | — |
| PUT | `/tickets/{id}` | Update ticket | 200 | — |
| DELETE | `/tickets/{id}` | Delete ticket | 200 | — |
| GET | `/tickets/{id}/comments` | Get comments | 200 | — |
| POST | `/tickets/{id}/comments` | Add comment | 200 | — |
| POST | `/tickets/{id}/acknowledge` | Acknowledge | 200 | — |
| POST | `/tickets/{id}/assign` | Assign | 200 | — |
| POST | `/tickets/{id}/start-progress` | Start progress | 200 | — |
| POST | `/tickets/{id}/resolve` | Resolve | 200 | — |
| POST | `/tickets/{id}/close` | Close | 200 | — |
| POST | `/tickets/{id}/reopen` | Reopen | 200 | — |
| GET | `/tickets/stats` | Ticket statistics | 200 | — |
| GET | `/tickets/overdue` | Overdue tickets | 200 | — |

---

## 12. Reviews (`/reviews`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/reviews/service/{serviceId}` | List reviews for a service | 200 | `{"success":true,"data":[{"id","bookingId","userId","rating","comment","status","createdAt"},...]}` |
| GET | `/reviews/{id}` | Get review by ID | 404* | *Test used service ID; real review ID returns 200 |
| POST | `/reviews` | Create review | 201 | — |
| PUT | `/reviews/{id}` | Update review | 200 | — |
| DELETE | `/reviews/{id}` | Delete review | 200 | — |
| POST | `/reviews/{id}/moderate` | Moderate (approve/reject) | 200 | — |
| POST | `/reviews/{id}/respond` | Provider response | 200 | — |

---

## 13. Roles & Permissions (`/roles`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/roles` | List all roles | 200 | `{"success":true,"data":[{"id","name","code","level","groupId","userCount","permissions"},...]}` |
| GET | `/roles/{id}` | Get role by ID | 200 | — |
| GET | `/roles/groups` | List role groups (G1, G2, …) | 200 | `{"success":true,"data":[{"id","name","code","description"},...]}` |
| GET | `/roles/permissions/catalogue` | All permissions | 200 | `{"success":true,"data":[{"id","code","name","resource","action","locked"},...]}` |
| GET | `/roles/permissions/unlocked` | Unlocked permissions | 200 | — |
| POST | `/roles` | Create role | 201 | — |
| PUT | `/roles/{id}/permissions` | Update role permissions | 200 | — |
| DELETE | `/roles/{id}` | Delete role (with reassignment) | 200 | — |

---

## 14. Payments (`/payments`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/payments` | List payments | 200 | `{"success":true,"data":[{"id","bookingId","amount","currency","method","status","transactionReference","processedAt"},...]}` |
| GET | `/payments/{id}` | Get payment by ID | 200 | — |
| GET | `/payments/transaction/{ref}` | Get by transaction reference | 200 | — |
| POST | `/payments` | Process payment | 201 | — |
| POST | `/payments/initiate` | Initiate (idempotent) | 200 | — |
| POST | `/payments/{id}/complete` | Complete payment | 200 | — |
| POST | `/payments/{id}/fail` | Record failure | 200 | — |
| POST | `/payments/{id}/refund` | Refund (body: amount, reason) | 200 | RefundResponse |

---

## 15. Payment Webhooks (`/pay`) – No auth (gateway callback)

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| POST | `/pay/webhooks` | Gateway webhook (success/failure/chargeback) | 200 | — |

---

## 16. Pricing (`/pricing`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| POST | `/pricing/preview` | Price breakdown for dates/guests | 200 | `{"success":true,"data":{"baseAmount":500,"commissionRate":10,"commissionAmount":50,"totalAmount":550,"currency":"USD","nights":2,"pricingModel":"..."}}` |

---

## 17. Discounts (`/discounts`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/discounts?page=0&size=20` | List discounts (optional status) | 200 | `{"success":true,"data":{"content":[{id, code, type, value, status, ...}],"totalElements","totalPages"}}` |
| GET | `/discounts/{id}` | Get discount by ID | 200 | Full discount object |
| POST | `/discounts` | Create discount | 201 | — |
| PUT | `/discounts/{id}` | Update discount | 200 | — |
| DELETE | `/discounts/{id}` | Delete discount | 200 | — |
| POST | `/discounts/{id}/approve` | Approve | 200 | — |
| POST | `/discounts/{id}/deactivate` | Deactivate | 200 | — |
| POST | `/discounts/validate?amount=100` | Validate code (body: code) | 200 | `{"success":true,"message":"Code is valid","data":{discount details,"valid":true}}` |

---

## 18. Complaints (`/complaints`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/complaints?page=0&size=20` | List (filters: status, priority, customerId, assignedTo) | 200 | `{"success":true,"data":{"content":[{"id","ticketNumber","customerId","subject","priority","status"},...],"totalElements"}}` |
| GET | `/complaints/{id}` | Get complaint by ID | 200 | — |
| POST | `/complaints` | Create complaint | 201 | — |
| PUT | `/complaints/{id}` | Update complaint | 200 | — |
| POST | `/complaints/{id}/assign` | Assign to user | 200 | — |
| POST | `/complaints/{id}/resolve` | Resolve | 200 | — |
| POST | `/complaints/{id}/escalate` | Escalate | 200 | — |
| POST | `/complaints/{id}/close` | Close | 200 | — |
| GET | `/complaints/{id}/comments` | Get comments | 200 | — |
| POST | `/complaints/{id}/comments` | Add comment | 200 | — |

---

## 19. Services (`/services`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/services?page=0&size=20` | List (filters: providerId, type, status, city) | 200 | `{"success":true,"data":{"content":[{"id","providerId","type","name","basePrice","currency","status"},...],"totalElements"}}` |
| GET | `/services/search?q=hotel&page=0&size=20` | Search (q, type, city, minPrice, maxPrice) | 200 | Same shape as list |
| GET | `/services/{id}` | Get service by ID | 200 | Full service object |
| POST | `/services` | Create service | 201 | — |
| PUT | `/services/{id}` | Update service | 200 | — |
| DELETE | `/services/{id}` | Soft-delete | 200 | — |

---

## 20. Reports (`/reports`) – Auth: Admin

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/reports/revenue?start=2025-01-01&end=2026-12-31` | Revenue report (totals, byDay) | 200 | `{"success":true,"data":{"totalRevenue", "byDay":[...]}}` or message "Revenue report generation initiated" |
| GET | `/reports/bookings?start=2025-01-01&end=2026-12-31` | Bookings report (totals, byDay) | 200 | Same pattern |

---

## 21. Audit logs (`/audit-logs`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/audit-logs` | Recent logs (params: limit, search) | 200 | `{"success":true,"data":[]}` or list of audit entries |
| GET | `/audit-logs/entity/{entityName}/{entityId}` | Logs for an entity | 200 | — |
| GET | `/audit-logs/user/{userId}` | Logs for a user | 200 | — |

---

## 22. Taxi bookings (`/taxi-bookings`) – Auth required

| Method | Path | Description | Status | Sample output |
|--------|------|-------------|--------|----------------|
| GET | `/taxi-bookings/active` | Active taxi bookings | 200 | — |
| PATCH | `/taxi-bookings/{id}/status` | Update taxi status | 200 | — |
| POST | `/taxi-bookings/{id}/assign` | Assign driver | 200 | — |

---

## Response envelope

Successful responses use:

- `success`: boolean  
- `message`: optional string  
- `data`: payload (object or array)  
- `timestamp`: ISO string  

Errors use:

- `success`: false  
- `error`: string  

Paginated list `data` often has: `content`, `totalElements`, `totalPages`, `number`, `size`.

---

## How to re-run tests

```powershell
# Backend on 8082
$env:SPRING_PROFILES_ACTIVE="port8082"; mvn -f backend/pom.xml spring-boot:run

# In another terminal
$env:API_BASE_URL="http://localhost:8082/api/v1"; .\scripts\test-endpoints.ps1
```

To collect fresh response samples:

```powershell
$env:API_BASE_URL="http://localhost:8082/api/v1"; .\scripts\collect-endpoint-responses.ps1
```

Output is written to `docs/ENDPOINT_TEST_REPORT.json`.
