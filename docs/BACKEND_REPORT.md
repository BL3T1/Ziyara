# Ziyarah Backend Report

**Version:** 1.0.0-SNAPSHOT  
**Framework:** Spring Boot **3.5.12** (see `core/build.gradle.kts`)  
**Base URL:** `http://localhost:8080/api/v1`  
**Date:** April 2026

---

## 1. API Endpoints

All endpoints are prefixed with `/api/v1` (context path).

### 1.1 Authentication (`/auth`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/register` | Register new user (e.g. customer); request: email, password, phone, role |
| POST | `/auth/login` | Authenticate user, return JWT tokens |
| POST | `/auth/logout` | Invalidate session (requires Bearer token) |
| POST | `/auth/refresh` | Refresh access token (requires Refresh-Token header) |
| POST | `/auth/password/forgot` | Request password reset (stub: token generated, email not sent) |
| POST | `/auth/password/reset` | Reset password using token from forgot-password |
| POST | `/auth/otp/send` | Send OTP to email or phone (stub: logs only) |
| POST | `/auth/otp/verify` | Verify OTP code |

### 1.2 Users (`/users`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users` | List users (paginated; optional status, role; Admin/HR only) |
| GET | `/users/{id}` | Get user by ID (Admin or self) |
| GET | `/users/{id}/login-history` | Get user login history (last login from users table) |
| POST | `/users` | Create user (Admin/HR only) |
| PUT | `/users/{id}` | Update user |
| DELETE | `/users/{id}` | Soft-delete user (Admin/HR only) |
| POST | `/users/{id}/freeze` | Freeze user account |
| POST | `/users/{id}/unfreeze` | Unfreeze user account |
| POST | `/users/{id}/reset-password` | Admin reset user password |

### 1.3 Dashboard (`/dashboard`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/dashboard/revenue` | Revenue stats (KPIs) |
| GET | `/dashboard/bookings` | Booking stats |
| GET | `/dashboard/customers` | Customer stats |
| GET | `/dashboard/providers` | Provider stats |
| GET | `/dashboard/kpis` | All KPIs (revenue, bookings, providers, complaints, tickets) |
| GET | `/dashboard/activity` | Activity feed (limit param, default 20) |
| GET | `/dashboard/service-health` | Counts per vertical and active bookings per type (jOOQ) |
| GET | `/dashboard/commission-analysis` | Aggregate base vs commission in date range (start, end params) |
| GET | `/dashboard/payouts` | Provider payouts in period (start, end params) |

### 1.4 Bookings (`/bookings`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/bookings` | List bookings for authenticated customer |
| GET | `/bookings/{id}` | Get booking by ID |
| GET | `/bookings/reference/{reference}` | Get booking by reference number |
| POST | `/bookings` | Create new booking |
| POST | `/bookings/{id}/cancel` | Cancel booking |
| PUT | `/bookings/{id}` | Update booking |
| POST | `/bookings/{id}/confirm` | Confirm booking |
| GET | `/bookings/{id}/voucher` | Get booking voucher (reference, dates, service, customer, amount) |
| POST | `/bookings/{id}/taxi` | Add taxi to booking (vehicle type STANDARD default) |

### 1.6 Departments (`/departments`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/departments` | List all departments |
| POST | `/departments` | Create department (Admin) |

### 1.7 Employees (`/employees`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/employees` | List all employees |
| GET | `/employees/{id}` | Get employee by ID (user_id) |
| POST | `/employees` | Onboard new employee |
| PUT | `/employees/{id}` | Update employee |

### 1.8 Service Providers (`/providers`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/providers` | List all providers |
| GET | `/providers/{id}` | Get provider by ID |
| POST | `/providers` | Register new provider |
| PUT | `/providers/{id}` | Update provider |
| PATCH | `/providers/{id}/commission` | Update commission rate (Admin) |
| DELETE | `/providers/{id}` | Soft-delete provider (set INACTIVE) |
| POST | `/providers/{id}/approve` | Approve provider |
| POST | `/providers/{id}/suspend` | Suspend provider |

### 1.9 Currency (`/currency`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/currency/rates` | List all exchange rates |
| GET | `/currency/convert` | Convert amount (params: amount, from, to) |
| POST | `/currency/rates` | Create exchange rate (Admin) |
| PUT | `/currency/rates/{id}` | Update exchange rate (Admin) |

### 1.10 Pricing (`/pricing`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/pricing/preview` | Price breakdown with discounts and commission |

### 1.11 Discounts (`/discounts`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/discounts` | List discounts (paginated; optional status filter) |
| GET | `/discounts/{id}` | Get discount by ID |
| POST | `/discounts` | Create discount |
| PUT | `/discounts/{id}` | Update discount |
| DELETE | `/discounts/{id}` | Delete discount |
| POST | `/discounts/{id}/approve` | Approve discount |
| POST | `/discounts/{id}/deactivate` | Deactivate discount |
| POST | `/discounts/validate` | Validate discount code (query param: amount) |

### 1.12 Payments (`/payments`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/payments` | Process payment |
| POST | `/payments/initiate` | Initiate payment (idempotent) |
| POST | `/payments/{id}/complete` | Complete payment |
| POST | `/payments/{id}/fail` | Record failed payment |
| GET | `/payments` | List payments |
| GET | `/payments/{id}` | Get payment by ID |
| GET | `/payments/transaction/{ref}` | Get payment by transaction reference |
| POST | `/payments/{id}/refund` | Refund payment (amount, reason) |

### 1.13 Payment Webhooks (`/pay`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/pay/webhooks` | Gateway webhook (success/failure/chargeback) |

### 1.14 Internal Tickets (`/tickets`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/tickets` | Create ticket |
| GET | `/tickets` | List tickets (filterable) |
| GET | `/tickets/{id}` | Get ticket by ID |
| PUT | `/tickets/{id}` | Update ticket |
| DELETE | `/tickets/{id}` | Delete ticket |
| POST | `/tickets/{id}/acknowledge` | Acknowledge ticket |
| POST | `/tickets/{id}/assign` | Assign ticket |
| POST | `/tickets/{id}/start-progress` | Start progress |
| POST | `/tickets/{id}/request-info` | Request information |
| POST | `/tickets/{id}/testing` | Move to testing |
| POST | `/tickets/{id}/resolve` | Resolve ticket |
| POST | `/tickets/{id}/verify` | Verify resolution |
| POST | `/tickets/{id}/close` | Close ticket |
| POST | `/tickets/{id}/reopen` | Reopen ticket |
| POST | `/tickets/{id}/cancel` | Cancel ticket |
| GET | `/tickets/{id}/comments` | Get ticket comments |
| POST | `/tickets/{id}/comments` | Add comment |
| GET | `/tickets/stats` | Ticket statistics |
| GET | `/tickets/overdue` | Overdue tickets |

### 1.15 Reports (`/reports`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/reports/revenue` | Revenue report (query: start, end; totalRevenue, byDay) |
| GET | `/reports/bookings` | Bookings report (query: start, end; totalBookings, byDay) |

### 1.16 Complaints (`/complaints`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/complaints` | List complaints (paginated; filters: status, priority, customerId, assignedTo) |
| GET | `/complaints/{id}` | Get complaint by ID |
| POST | `/complaints` | Create complaint |
| PUT | `/complaints/{id}` | Update complaint |
| POST | `/complaints/{id}/assign` | Assign complaint |
| POST | `/complaints/{id}/resolve` | Resolve complaint |
| POST | `/complaints/{id}/escalate` | Escalate complaint |
| POST | `/complaints/{id}/close` | Close complaint |

### 1.17 Services (`/services`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/services` | List services (filters: providerId, type, status, city, country) |
| GET | `/services/search` | Search (q, type, city, minPrice, maxPrice) |
| GET | `/services/{id}` | Get service by ID |
| POST | `/services` | Create service |
| PUT | `/services/{id}` | Update service |
| DELETE | `/services/{id}` | Soft-delete service |

### 1.18 Taxi Bookings (`/taxi-bookings`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/taxi-bookings/active` | List active trips |
| PATCH | `/taxi-bookings/{id}/status` | Update trip status |
| POST | `/taxi-bookings/{id}/assign` | Assign driver |

### 1.14 Complaints (`/complaints`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/complaints/{id}/comments` | Get complaint comments |
| POST | `/complaints/{id}/comments` | Add comment |

### 1.15 Reviews (`/reviews`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/reviews/service/{serviceId}` | Get service reviews |
| POST | `/reviews` | Submit review |
| POST | `/reviews/{id}/respond` | Provider response to review |

### 1.16 Notifications (`/notifications`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/notifications` | Get user notifications |
| POST | `/notifications` | Create notification (Admin) |
| PATCH | `/notifications/{id}/read` | Mark as read |

### 1.17 Audit Logs (`/audit-logs`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/audit-logs` | Recent audit logs |
| GET | `/audit-logs/entity/{name}/{id}` | Entity history |
| GET | `/audit-logs/user/{userId}` | User activity |

### 1.18 Role Management (`/roles`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/roles` | List all roles |
| GET | `/roles/{id}` | Get role by ID |
| GET | `/roles/permissions/catalogue` | Permission catalogue |
| GET | `/roles/permissions/unlocked` | Unlocked permissions |
| GET | `/roles/groups` | List groups |
| POST | `/roles` | Create custom role |
| PUT | `/roles/{id}/permissions` | Update role permissions |
| DELETE | `/roles/{id}` | Delete custom role |

### 1.19 Reports (`/reports`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/reports/revenue` | Generate revenue report |
| GET | `/reports/bookings` | Generate booking report |

### 1.20 Actuator

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/info` | Application info |
| GET | `/actuator/metrics` | Metrics |

---

## 2. Application Services (Business Logic)

| Service | Purpose |
|---------|---------|
| **AuthService** | Login, logout, token refresh, password verification |
| **DashboardService** | KPIs (revenue, bookings, providers, tickets, complaints), activity feed |
| **BookingController** (logic in services) | Booking CRUD, cancellation |
| **DepartmentService** | Department management |
| **EmployeeService** | Employee onboarding, updates, listing |
| **ServiceProviderService** | Provider CRUD, commission |
| **CurrencyService** | Exchange rates, currency conversion |
| **PricingService** | Price breakdown, seasonal multiplier, tax, commission, discounts |
| **DiscountCodeService** | Validate discount codes, record usage |
| **PaymentService** | Initiate, complete, fail payments |
| **TaxiBookingService** | Taxi trip status, driver assignment |
| **NotificationService** | Create, list, mark read notifications |
| **ReviewService** | Create reviews, respond, list by service |
| **ComplaintCommentService** | Complaint comments |
| **AuditLogService** | Audit log retrieval |
| **RoleManagementService** | Roles, permissions, groups |
| **ReportService** | Revenue and booking reports |
| **InternalTicketController** (logic in services) | Ticket lifecycle, comments |
| **JwtService** | JWT generation, validation |

---

## 3. Domain Repositories (Data Access)

| Repository | Entity | Purpose |
|------------|--------|---------|
| **UserRepository** | User | findByEmail, findById, findByRole |
| **DepartmentRepository** | Department | CRUD |
| **EmployeeRepository** | Employee | findByUserId, findByEmployeeCode, findByDepartmentId |
| **ServiceProviderRepository** | ServiceProvider | CRUD |
| **ServiceRepository** | Service | CRUD, findOverlappingBookings |
| **BookingRepository** | Booking | findByCustomerId, findById, countByStatus |
| **PaymentRepository** | Payment | findByBookingId, sumCompletedAmountBetween |
| **ExchangeRateRepository** | ExchangeRate | findByFromCurrencyAndToCurrency |
| **DiscountCodeRepository** | DiscountCode | findByCode |
| **NotificationRepository** | Notification | CRUD |
| **ReviewRepository** | Review | findByServiceIdAndStatus |
| **ComplaintRepository** | Complaint | countOpenComplaints |
| **ComplaintCommentRepository** | ComplaintComment | CRUD |
| **InternalTicketRepository** | InternalTicket | countByStatus, countByStatusIn |
| **AuditLogRepository** | AuditLog | findRecent |
| **RoleRepository** | Role | CRUD |
| **PermissionRepository** | Permission | Catalogue |
| **GroupRepository** | Group | List groups |
| **UserRoleAssignmentRepository** | UserRole | Assignments |
| **TaxiBookingRepository** | TaxiBooking | Active trips |

---

## 4. Database

### 4.1 Technology

- **RDBMS:** PostgreSQL 15+
- **Connection:** `jdbc:postgresql://localhost:5432/ziyarah`
- **Schema:** `public`
- **Hibernate:** `ddl-auto: none` (schema managed by SQL scripts)

### 4.2 Tables

| Table | Purpose |
|-------|---------|
| **exchange_rates** | Currency conversion rates (from_currency, to_currency, effective_date) |
| **departments** | Organization departments |
| **groups** | Organizational groups (G1–G6) |
| **roles** | RBAC roles |
| **permissions** | Permission catalogue |
| **role_permissions** | Role–permission mapping |
| **users** | Base user accounts (email, password_hash, role, status) |
| **user_roles** | User–role assignments |
| **customers** | Customer profiles (extends users) |
| **employees** | Employee profiles (extends users, user_id PK) |
| **service_providers** | Hotels, taxis, etc. |
| **services** | Bookable services |
| **service_images** | Service images |
| **discount_codes** | Promo codes |
| **bookings** | Reservations |
| **payments** | Payment transactions |
| **refunds** | Refund records |
| **taxi_bookings** | Taxi-specific booking data |
| **complaints** | Customer complaints |
| **complaint_comments** | Complaint discussion |
| **internal_tickets** | Bug reports, feature requests |
| **ticket_comments** | Ticket discussion |
| **reviews** | Service reviews |
| **notifications** | User notifications |
| **sessions** | JWT/session tracking |
| **audit_logs** | Audit trail |

### 4.3 Enums (PostgreSQL Types)

| Enum | Values |
|------|--------|
| **user_role_enum** | CUSTOMER, SUPER_ADMIN, SALES_MANAGER, … |
| **user_status_enum** | ACTIVE, INACTIVE, FROZEN, PENDING_VERIFICATION, DELETED |
| **employee_level_enum** | SUPER_ADMIN, MANAGER, EMPLOYEE, EXECUTIVE |
| **service_type_enum** | HOTEL, RESORT, RESTAURANT, TAXI, TRIP |
| **service_status_enum** | ACTIVE, INACTIVE, SUSPENDED, PENDING_APPROVAL |
| **provider_status_enum** | ACTIVE, INACTIVE, SUSPENDED, PENDING_APPROVAL |
| **booking_status_enum** | PENDING, CONFIRMED, ACTIVE, COMPLETED, CANCELLED, … |
| **payment_method_enum** | CREDIT_CARD, DEBIT_CARD, WALLET, BANK_TRANSFER, CASH |
| **payment_status_enum** | PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED |
| **taxi_status_enum** | PENDING, CONFIRMED, DRIVER_ASSIGNED, EN_ROUTE, … |
| **vehicle_type_enum** | STANDARD, PREMIUM, LUXURY, SUV, VAN |
| **complaint_status_enum** | SUBMITTED, ACKNOWLEDGED, ASSIGNED, … |
| **ticket_status_enum** | SUBMITTED, ACKNOWLEDGED, ASSIGNED, … |
| **review_status_enum** | PENDING, APPROVED, REJECTED, HIDDEN |
| **notification_type_enum** | BOOKING_CONFIRMATION, PAYMENT_RECEIVED, … |

### 4.4 Migrations

| Migration | Purpose |
|-----------|---------|
| **001_plans_schema_extensions** | Plans schema extensions |
| **002_role_management_report** | Role management report |
| **003_pricing_and_payment_methods** | Seasonal multiplier, tax rate, idempotency, CASH_ON_SERVICE |
| **004_hibernate_enum_compat** | users.role, users.status → VARCHAR |
| **005_reviews_status_varchar** | reviews.status → VARCHAR |
| **006_discount_codes_jpa_compat** | Add type, value, min_booking_amount, etc. |
| **007_service_providers_jpa_compat** | Add rating, review_count, verified; status → VARCHAR |
| **008_employees_payments_enum_compat** | employees.level, payments.status/method → VARCHAR |

### 4.5 Indexes

Indexes on: users (email, phone, status, role), customers (name), employees (department, code), providers (status), services (provider, type, status, location), bookings (customer, service, status, dates, reference), payments (booking, status, transaction_ref), refunds, taxi_bookings, complaints, internal_tickets, reviews, notifications, audit_logs.

---

## 5. Architecture

- **Pattern:** Clean Architecture (Domain, Application, Infrastructure, Presentation)
- **Auth:** JWT (Bearer token)
- **Docs:** SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`)
- **Cache:** Redis (sessions, optional)
- **Validation:** Jakarta Validation (Bean Validation)
