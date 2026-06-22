# Ziyara Platform — Full System Report
**Generated:** 2026-05-23  
**State:** Post Phase 1–7 implementation (as described in CHANGES_REPORT.md)

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Backend (Spring Boot)](#2-backend-spring-boot)
   - 2.1 Tech Stack & Build
   - 2.2 Configuration Layers
   - 2.3 Security Architecture
   - 2.4 API Controllers (37 total)
   - 2.5 Application Services
   - 2.6 WebSocket / Real-time
   - 2.7 Scheduled Jobs & Background Work
3. [Frontend (React + Vite)](#3-frontend-react--vite)
   - 3.1 Three-Surface Architecture
   - 3.2 Company Dashboard Surface
   - 3.3 Provider Portal Surface
   - 3.4 Landing / Booking Surface
   - 3.5 Shared Infrastructure
4. [Database (PostgreSQL)](#4-database-postgresql)
   - 4.1 Flyway Migration History (V0–V19)
   - 4.2 Schema: All Tables
   - 4.3 Materialized Views
   - 4.4 Row-Level Security
   - 4.5 Triggers & Functions
5. [Docker](#5-docker)
   - 5.1 Services Overview
   - 5.2 Docker Profiles
   - 5.3 How to Run Each Profile
   - 5.4 Issues Found & Required Fixes
   - 5.5 Environment Variables Reference
6. [Security Notes](#6-security-notes)
7. [Known Gaps & Recommendations](#7-known-gaps--recommendations)

---

## 1. System Overview

Ziyara is a **B2B travel commerce platform** that manages hotels, resorts, restaurants, trips, and taxi bookings. It serves three distinct user populations via three separately compiled front-end bundles, all backed by a single Spring Boot REST API and PostgreSQL database.

| Layer | Technology |
|---|---|
| Backend API | Spring Boot 3.5.12 / Java 17 / Gradle (Kotlin DSL) |
| Database | PostgreSQL 15 + Flyway V0–V19 migrations |
| Frontend | React 19 + Vite 7 + TailwindCSS 4 (three separate bundles) |
| Cache | Redis (optional; used for login rate-limiting) |
| Messaging | Apache Kafka (staff notifications) |
| Real-time | STOMP over SockJS WebSocket (`/ws`) |
| Media | Local disk (`./data/media`) or AWS S3 |
| Observability | Actuator + Micrometer + Logstash encoder + optional OTLP tracing |
| API Docs | SpringDoc OpenAPI 2.8 / Swagger UI at `/api/v1/swagger-ui.html` |

---

## 2. Backend (Spring Boot)

### 2.1 Tech Stack & Build

- **Build tool:** Gradle 8.7 with Kotlin DSL (`core/build.gradle.kts`)
- **Language level:** Java 17 (`sourceCompatibility = JavaVersion.VERSION_17`)
- **Spring Boot:** 3.5.12
- **JAR artifact:** `core-1.0.0.jar` (non-SNAPSHOT in production)

**Key Dependencies:**

| Library | Version | Purpose |
|---|---|---|
| spring-boot-starter-web | 3.5.12 | REST controllers |
| spring-boot-starter-security | 3.5.12 | Authentication & authorization |
| spring-boot-starter-data-jpa | 3.5.12 | ORM / Hibernate |
| spring-boot-starter-websocket | 3.5.12 | STOMP real-time push |
| spring-boot-starter-validation | 3.5.12 | Bean validation |
| spring-boot-starter-actuator | 3.5.12 | Health, metrics, Prometheus |
| spring-boot-starter-data-redis | 3.5.12 | Distributed rate-limit / JWT blocklist |
| spring-boot-starter-mail | 3.5.12 | Password-reset emails |
| spring-kafka | 3.x | Staff notification events |
| jjwt | 0.12.3 | JWT sign/verify (HMAC-SHA) |
| flyway-core | (via BOM) | Schema migrations |
| springdoc-openapi-starter | 2.8.16 | OpenAPI / Swagger |
| poi-ooxml | 5.2.5 | Excel report export |
| openpdf | 1.3.35 | PDF report export |
| zxcvbn4j | 1.9.0 | Password strength scoring |
| AWS SDK v2 S3 | 2.26.29 | S3 media storage |
| micrometer-tracing | BOM | Distributed tracing (OTLP export) |
| logstash-logback-encoder | latest | JSON log output for ELK |
| testcontainers | latest | Integration tests with real Postgres |

### 2.2 Configuration Layers

The backend uses a layered Spring profile system:

| File | Active when | Key overrides |
|---|---|---|
| `application.yml` | Always (base) | DB URL `localhost:5432`, JWT config, CORS, all `ziyara.*` flags |
| `application-dev.yml` | Profile `dev` | SQL logging, DEBUG level, `out-of-order: false` |
| `application-docker.yml` | Profile `docker` | DB URL `postgres:5432`, Kafka `kafka:9092`, `forward-headers-strategy: framework`, zxcvbn score 2 |
| `application-prod.yml` | Profile `prod` | (not read during audit — expected to disable demo admin, enable HSTS, etc.) |

**Important application.yml settings:**

```yaml
server:
  port: 8080
  servlet:
    context-path: /api/v1        # ALL endpoints live under /api/v1

jwt:
  secret: ${JWT_SECRET}          # REQUIRED — no default
  expiration: 86400000           # 24 hours access token
  refresh-expiration: 604800000  # 7 days refresh token
  cookie:
    also-return-token-in-body: true   # Bearer token in body + cookie

app:
  booking:
    commission-rate: 10.0        # 10% platform commission
    payment-timeout-minutes: 15
    cancellation:
      before-start-refund: 100   # 100% refund
      during-service-refund: 95  # 95% refund
  demo:
    super-admin:
      enabled: true              # DISABLE in production!

ziyara:
  rls:
    enabled: false               # RLS disabled by default — requires env var
  security:
    mfa-required-roles: ""       # Leave empty in dev
```

### 2.3 Security Architecture

**Authentication flow:**
1. `POST /api/v1/auth/login` → validates credentials → returns JWT access token (in body + HttpOnly cookie) + refresh token cookie
2. All protected endpoints read `Authorization: Bearer <token>` header or `ziyara_access` cookie
3. Token refresh: `POST /api/v1/auth/refresh` — rotates access token, returns new pair
4. Logout: `POST /api/v1/auth/logout` — clears cookies, optionally blocks token in Redis blocklist

**MFA flow:**
- If user has `mfa_enabled=true`, login returns HTTP 401 with `{ code: "MFA_CODE_REQUIRED" }`
- Frontend catches this, shows `MfaChallengePage` (6-digit TOTP)
- Second call: `POST /api/v1/auth/login` with `mfaCode` field — succeeds or returns MFA failure

**Authorization model:**
- Role-based: `@PreAuthorize` on each controller/method
- Roles: `SUPER_ADMIN`, `CEO`, `GENERAL_MANAGER`, `SALES_MANAGER`, `SALES_REPRESENTATIVE`, `FINANCE_MANAGER`, `ACCOUNTANT`, `SUPPORT_MANAGER`, `SUPPORT_AGENT`, `HR_MANAGER`, `PROVIDER_MANAGER`, `PROVIDER_FINANCE`, `PROVIDER_STAFF`, `TAXI_OPERATOR`, `CUSTOMER`
- Expression constants in `ApiAuthorizationExpressions`:
  - `COMPANY_STAFF` — any internal staff role (not provider, not customer)
  - `PROVIDER_PORTAL` — provider-facing roles (PROVIDER_MANAGER, PROVIDER_FINANCE, PROVIDER_STAFF, TAXI_OPERATOR)
- RBAC fine-grained: `sys_permissions` + `sys_role_permissions` for feature-level control

**Security hardening (as of V11–V16):**
- Login rate limiting: DB counter or Redis (configurable)
- Account lockout after 5 failed attempts (30-minute lockout)
- Password history enforcement
- MFA backup codes (TOTP)
- Consent audit trail
- Security event logging (`sys_security_events`)
- Security alert rules (brute-force, MFA failures)
- GDPR: `right_to_erasure_requested` flag, data export, PII registry
- Password strength scoring via zxcvbn (min score configurable)
- Token version column (`sys_users.token_version`) for forced logout

### 2.4 API Controllers (37 total)

All routes are prefixed `/api/v1` (context-path).

| Controller | Base Path | Access | Description |
|---|---|---|---|
| `AuthController` | `/auth` | Public | Register, login, logout, refresh, forgot/reset password |
| `MfaController` | `/auth/mfa` | Auth required | Enroll TOTP, verify, disable, generate backup codes |
| `UserController` | `/users` | COMPANY_STAFF | List/get/update/delete users, change password, FCM token |
| `EmployeeController` | `/employees` | COMPANY_STAFF | Staff HR records, onboarding, soft delete/offboard |
| `DepartmentController` | `/departments` | COMPANY_STAFF | Department CRUD |
| `RoleManagementController` | `/roles` | SUPER_ADMIN | Role CRUD, permission assignment, nav item IDs |
| `AdminPermissionsController` | `/admin/permissions` | SUPER_ADMIN | Permission catalogue management |
| `ServiceController` | `/services` | Public (read), COMPANY_STAFF (write) | Service catalogue (hotels, resorts, restaurants, trips, taxis) |
| `ServiceProviderController` | `/providers` | COMPANY_STAFF | Provider onboarding, approval, suspension |
| `BookingController` | `/bookings` | Auth required | Create/list/confirm/cancel bookings, bulk status ops |
| `TaxiBookingController` | `/taxi-bookings` | Auth required | Taxi-specific booking fields |
| `TaxiTrackingController` | `/taxi-tracking` | Auth required | Live driver position updates |
| `PaymentController` | `/payments` | Auth required | Payment initiation, status, refunds |
| `PayWebhookController` | `/payments/webhook` | Public + HMAC signature | Payment gateway callbacks (Stripe or stub) |
| `DiscountController` | `/discounts` | Auth required | Discount code CRUD, approval workflow |
| `ReviewController` | `/reviews` | Auth required | Service reviews moderation |
| `ComplaintController` | `/support/complaints` | Auth required | Customer complaint lifecycle |
| `InternalTicketController` | `/support/tickets` | COMPANY_STAFF | Internal IT/ops ticket system |
| `NotificationController` | `/notifications` | Auth required | In-app notification list, mark-read |
| `AuditLogController` | `/audit` | COMPANY_STAFF | Audit trail search |
| `ReportController` | `/reports` | COMPANY_STAFF | Excel/PDF revenue, booking, provider reports |
| `DashboardController` | `/dashboard` | COMPANY_STAFF | KPIs, activity feed, service health, bootstrap |
| `CurrencyController` | `/currency` | Auth required | FX rate list, manual update |
| `PricingController` | `/pricing` | COMPANY_STAFF | Dynamic pricing configuration |
| `ContentPageController` | `/content` | Public (read), COMPANY_STAFF (write) | CMS pages (home, about, services, contact, FAQ, etc.) |
| `PublicContactController` | `/public/contact` | Public | Marketing lead form submission |
| `SubscriptionController` | `/subscriptions` | PROVIDER_PORTAL | Provider plan/subscription management |
| `PortalController` | `/portal` | PROVIDER_PORTAL | Dashboard, services, bookings, earnings, payout requests, rooms, images, menus |
| `PortalStaffController` | `/portal/staff` | PROVIDER_PORTAL | Provider staff management (hire, list, remove) |
| `PortalSupportRequestsController` | `/portal/support` | PROVIDER_PORTAL | Provider-to-company support requests |
| `UserConsentController` | `/users/consent` | Auth required | GDPR consent give/withdraw |
| `UserDataExportController` | `/users/data-export` | Auth required | GDPR data export request/download |
| `SuperAdminController` | `/admin/super` | SUPER_ADMIN | Break-glass ops, token invalidation, seed admin |
| `AdminSystemSettingsController` | `/admin/settings` | SUPER_ADMIN | Key-value system settings |
| `AdminFeatureFlagsController` | `/admin/feature-flags` | SUPER_ADMIN | Feature flag toggle |
| `AdminIntegrationApiKeysController` | `/admin/api-keys` | SUPER_ADMIN | Integration API key management |
| `AdminPiiRegistryController` | `/admin/pii-registry` | SUPER_ADMIN | PII field registry view |

### 2.5 Application Services

Key service classes in `application/service/`:

| Service | Responsibility |
|---|---|
| `AuthService` | Login, register, token issuance, password reset, logout |
| `JwtService` | JWT generation, validation, claim extraction |
| `DashboardService` | KPI queries (revenue, bookings, providers, complaints) |
| `DashboardBootstrapService` | Bootstrap query combining KPIs + activity + service health; `loadLive()` for WS push |
| `DashboardQueryHandler` | CQRS-style query separation for dashboard |
| `PortalService` | Provider dashboard, services list, bookings, earnings, weekly revenue (8-week window), payout request creation |
| `ServiceProviderService` | Provider onboarding, approval workflow, bulk operations |
| `BookingService` | Booking creation, confirmation, cancellation, status transitions |
| `PaymentService` | Payment gateway integration (stub/Stripe), webhook processing |
| `DiscountService` | Code validation, usage counting, approval workflow |
| `LoginRateLimitService` | IP-based login throttling (DB or Redis backend) |
| `SecurityEventService` | Record security events for audit |
| `SecurityAlertService` | Fire alerts when event thresholds are reached |
| `SubscriptionService` | Provider plan limits, seat-count enforcement |
| `ReportService` | Excel/PDF generation using Apache POI and OpenPDF |
| `NotificationService` | In-app notifications; Kafka-backed staff notifications |
| `MediaStorageService` | Local disk or S3 file storage for images |

### 2.6 WebSocket / Real-time

- **Endpoint:** `ws://{host}/ws` (SockJS fallback)
- **Protocol:** STOMP over SockJS
- **Topic:** `/topic/dashboard/live`
- **Backend broadcaster:** `DashboardLiveBroadcaster` — `@Scheduled(fixedDelay=30_000)` pushes `DashboardLiveResponse` JSON every 30 seconds
- **Frontend hook:** `useDashboardWebSocket(bootstrapQueryKey)` — connects via SockJS, patches React Query cache with live KPIs/activity/serviceHealth, returns `connected: boolean`
- **Graceful degradation:** When `wsConnected = false`, the live React Query falls back to polling (`refetchInterval: 45_000`)

### 2.7 Scheduled Jobs & Background Work

| Job | Schedule | Description |
|---|---|---|
| `DashboardLiveBroadcaster` | Every 30s | Push live KPI data to WebSocket topic |
| Data retention job | `0 0 4 * * SUN` (Sunday 04:00 UTC) | Archive old audit logs, delete expired OTPs/tokens |
| FX rate refresh | `0 0 6 * * *` (daily 06:00 UTC) | Fetch new exchange rates from external API |
| Materialized view refresh | `0 20 2 * * *` (daily 02:20 UTC) | Refresh `mv_pay_daily_totals` |
| Kafka notification consumer | Continuous | Consume `ziyara.notifications.staff` topic, deduplicate via `kafka_staff_notification_delivered` |

All scheduled jobs are **opt-in via environment variables** (disabled by default to avoid side effects in dev).

---

## 3. Frontend (React + Vite)

### 3.1 Three-Surface Architecture

The React app compiles to **three separate Vite bundles**, controlled by `VITE_APP_SURFACE`:

| Surface value | Served to | Default port | Entry point |
|---|---|---|---|
| `company` | Internal staff (company employees) | :80 (Docker) or :5173 (dev) | `AppCompanyRoutes.tsx` |
| `provider` | Partner/provider businesses | :80 (partner container) | `AppProviderRoutes.tsx` |
| `landing` | End customers (B2C) | :80 (landing container) | `AppLandingRoutes.tsx` |

Each surface has its own `RequireSurfaceRole` guard that blocks the wrong user types at the route level, supplementing the backend's role-based access control.

**Shared dependencies:**

| Library | Version | Purpose |
|---|---|---|
| React | 19.2 | UI framework |
| React Router DOM | 7.13 | Client-side routing |
| @tanstack/react-query | 5.99 | Server state / caching |
| Axios | 1.13 | HTTP client |
| Zod | 4.4 | Schema validation |
| Recharts | 3.8 | Charts (revenue bar chart) |
| @stomp/stompjs | 7.3 | STOMP over WebSocket |
| sockjs-client | 1.6 | SockJS transport |
| lucide-react | 0.544 | Icon library |
| TailwindCSS | 4.2 | Utility CSS |

### 3.2 Company Dashboard Surface (`VITE_APP_SURFACE=company`)

**Route tree** (`AppCompanyRoutes.tsx`): All routes require `RequireAuth > RequireSurfaceRole(company)`.

| Route | Page Component | Description |
|---|---|---|
| `/login` | `LoginPage` | Email/password + MFA challenge |
| `/` | `HomeRedirect` | Redirects authenticated users to role dashboard |
| `/dashboard` | `DashboardPage` | Main KPI dashboard (WebSocket live data + React Query bootstrap) |
| `/dashboard/sales` | `SalesDashboardPage` | Sales-focused metrics |
| `/bookings` | `BookingsPage` | Booking list with bulk confirm/cancel |
| `/bookings/:id` | `BookingDetailPage` | Single booking details + status timeline |
| `/providers` | `ProvidersPage` | Provider list with bulk approve/suspend |
| `/providers/:id` | `ProviderDetailPage` | Single provider profile |
| `/users` | `UsersPage` | User management |
| `/users/:id` | `UserDetailPage` | User profile |
| `/employees` | `EmployeesPage` | Staff HR management |
| `/discounts` | `DiscountsPage` | Discount code list + approval |
| `/discounts/new` | `DiscountFormPage` | Create discount code |
| `/discounts/:id` | `DiscountFormPage` | Edit discount code |
| `/payments` | `PaymentsPage` | Payment transaction list |
| `/reports` | `ReportsPage` | Excel/PDF report generation |
| `/complaints` | `ComplaintsPage` | Customer complaint queue |
| `/complaints/:id` | `ComplaintDetailPage` | Complaint detail + comments |
| `/support/tickets` | `TicketsPage` | Internal IT/ops tickets |
| `/support/tickets/:ticketId` | `TicketDetailPage` | Ticket detail + comments |
| `/reviews` | `ReviewsPage` | Service review moderation |
| `/currency` | `CurrencyPage` | FX rate management |
| `/notifications` | `NotificationsPage` | Notification center |
| `/roles` | `RolesPage` | Role/permission management |
| `/audit` | `AuditLogPage` | Audit trail search |
| `/content` | `ContentPagesPage` | CMS page editor |
| `/settings` | `SettingsPage` | System settings |
| `/profile` | `ProfilePage` | Current user profile |
| `/pricing` | `PricingPage` | Dynamic pricing config |

**DashboardPage key features:**
- Bootstrap query: `GET /api/v1/dashboard/bootstrap?start=...&end=...`
- WebSocket: `useDashboardWebSocket` → live KPI patch
- Fallback polling: 45s when WebSocket disconnected
- React Query cache key: `['dashboard', 'bootstrap', start, end]`

### 3.3 Provider Portal Surface (`VITE_APP_SURFACE=provider`)

**Route tree** (`AppProviderRoutes.tsx`): All routes wrapped in `ClientPortalLayout`.

| Route | Page Component | Description |
|---|---|---|
| `/login` | `LoginPage` | Provider login |
| `/portal` | `ClientPortalOverview` | Provider dashboard (KPIs + weekly revenue bar chart) |
| `/portal/listings` | `PortalListingsPage` | My services list |
| `/portal/listings/new` | `PortalListingFormPage` | Create new service listing |
| `/portal/listings/:id` | `PortalListingFormPage` | Edit service listing |
| `/portal/bookings` | `PortalBookingsPage` | My booking list |
| `/portal/staff` | `PortalStaffPage` | Staff management (hire/remove) |
| `/portal/earnings` | `PortalEarningsPage` | Earnings summary + payout request modal |
| `/portal/profile` | `PortalProfilePage` | Provider profile settings |
| `/portal/support` | `PortalSupportPage` | Submit support request to company |

**ClientPortalOverview features:**
- KPI cards: total services, active bookings, total revenue, pending reviews
- Weekly revenue bar chart: last 8 ISO weeks, data from `GET /api/v1/portal/dashboard` → `weeklyRevenue[]`

**PortalEarningsPage features:**
- Shows `totalEarnings`, `pendingPayouts`, `completedPayouts`
- "Request Payout" button → `PayoutRequestModal` → `POST /api/v1/portal/payout-request`

### 3.4 Landing / Booking Surface (`VITE_APP_SURFACE=landing`)

**Route tree** (`AppLandingRoutes.tsx`):

| Route | Page Component | Description |
|---|---|---|
| `/` | `LandingHomePage` | Marketing homepage with hero, search, deals |
| `/login` | `LandingLoginPage` | Customer login (supports `?next=` redirect) |
| `/signup` | `LandingSignUpPage` | Customer registration |
| `/about` | `LandingAboutPage` | CMS about page |
| `/services` | `LandingServicesPage` | CMS services page |
| `/contact` | `LandingContactPage` | Contact form (lead capture) |
| `/faq` | `LandingFaqPage` | CMS FAQ page |
| `/privacy` | `LandingPrivacyPage` | Privacy policy |
| `/terms` | `LandingTermsPage` | Terms and conditions |
| `/services/:id` | `LandingServiceDetailPage` | Service detail + booking panel |
| `/checkout` | `LandingCheckoutPage` | Booking checkout (requires auth) |

**LandingServiceBookingPanel features:**
- Hotels/Resorts: check-in/check-out date pickers, guest counter (1–20), nights calculation
- Taxis: route + vehicle type, fare estimate display
- Room selector grid (20 slots, pre-seeded booked indices)
- Auth guard: unauthenticated users redirected to `/login?next=<checkout-url>`
- Proceeds to `/checkout?serviceId=...&checkIn=...&checkOut=...&guests=...`

**LandingCheckoutPage features:**
- Reads URL params: `serviceId`, `checkIn`, `checkOut`, `guests`, `discount`
- Fetches service details, shows booking summary
- Coupon code field + `specialRequests` textarea (Zod validated, max 500 chars)
- Calls `POST /api/v1/bookings` on confirm
- Shows success screen with `booking_reference`

### 3.5 Shared Infrastructure

**API Client (`services/api.ts`):**
- Base URL: `VITE_API_URL` (default `/api/v1`)
- JWT storage: `sessionStorage` (key: `token`)
- Request interceptor: injects `Authorization: Bearer <token>`
- 401 response interceptor:
  - `MFA_CODE_REQUIRED` → pass through (handled by component)
  - Auth endpoints → propagate error directly
  - Other 401 → attempt token refresh via `POST /api/v1/auth/refresh`
  - Concurrent 401s: `_refreshing` flag + `_refreshQueue` (thundering-herd protection)
  - Refresh failure → `clearSession()` + redirect to `/login`

**API namespaces exported:**

| Namespace | Key Endpoints |
|---|---|
| `authAPI` | `login`, `register`, `logout`, `refresh`, `forgotPassword`, `resetPassword` |
| `servicesAPI` | `list`, `get`, `create`, `update`, `delete`, `uploadImage` |
| `portalAPI` | `getDashboard`, `getServices`, `createService`, `updateService`, `getBookings`, `getEarnings`, `requestPayout` |
| `portalStaffAPI` | `list`, `invite`, `remove` |
| `portalSupportAPI` | `list`, `submit` |
| `portalServicesAPI` | `getRooms`, `createRoom`, `updateRoom`, `getImages`, `uploadImage` |
| `dashboardAPI` | `getBootstrap`, `getRevenue`, `getActivity`, `getServiceHealth` |
| `bookingsAPI` | `list`, `get`, `create`, `confirm`, `cancel`, `bulkConfirm`, `bulkCancel` |
| `providersAPI` | `list`, `get`, `approve`, `suspend`, `getMe`, `bulkApprove`, `bulkSuspend` |
| `usersAPI` | `list`, `get`, `update`, `delete` |
| `rolesAPI` | `list`, `get`, `create`, `update`, `delete`, `assignPermission` |
| `discountsAPI` | `list`, `get`, `create`, `update`, `approve`, `delete` |
| `paymentsAPI` | `list`, `get`, `refund` |
| `reportsAPI` | `downloadRevenue`, `downloadBookings`, `downloadProviders` |
| `currencyAPI` | `list`, `update` |
| `notificationsAPI` | `list`, `markRead`, `markAllRead` |
| `taxiBookingsAPI` | `list`, `get`, `update` |
| `permissionsAPI` | `list`, `create`, `delete` |
| `ticketsAPI` | `list`, `get`, `create`, `update`, `addComment` |

**Context Providers:**
- `AuthContext` — user identity, `setUser`, `clearUser`, `getStoredToken`/`setStoredToken` (sessionStorage)
- `LanguageContext` — `t(key)` translation function, EN/AR toggle
- `ThemeContext` — dark/light mode

**Form Validation (`lib/validation.ts`):**
All forms use Zod `safeParse` with per-field inline error display and red-border highlight.

| Schema | Used by |
|---|---|
| `loginSchema` | `LoginPage`, `LandingLoginPage` |
| `signUpSchema` | `LandingSignUpPage` |
| `listingSchema` | `LandingServiceDetailPage` (if applicable) |
| `portalListingSchema` | `PortalListingFormPage` |
| `checkoutSchema` | `LandingCheckoutPage` |

**Bulk Actions (`components/BulkActionBar.tsx`):**
- Sticky `top-[4.25rem] z-30` bar, hidden when `selectedCount === 0`
- Used in `BookingsPage` (bulk confirm/cancel) and `ProvidersPage` (bulk approve/suspend)
- Operations use `Promise.allSettled` — partial failures don't block the batch

---

## 4. Database (PostgreSQL)

### 4.1 Flyway Migration History (V0–V19)

| Version | File | Summary |
|---|---|---|
| V0 | `V0__initial_schema.sql` | Complete base schema: all core tables (sys_*, hotel_*, bkg_*, pay_*, support_*, disc_*) |
| V1 | `V1__create_event_publication.sql` | Spring Modulith `event_publication` table for async domain events |
| V2 | `V2__create_web_content_pages.sql` | CMS `web_content_pages` table + seed EN/AR content (home, about, services, contact, FAQ, privacy, terms) |
| V3 | `V3__system_settings_contact_leads.sql` | `sys_system_settings` (KV store) + `support_contact_leads` (marketing form) |
| V4 | `V4__roles_navigation_item_ids.sql` | `sys_roles.navigation_item_ids JSONB` — custom sidebar per role |
| V5 | `V5__portal_support_requests.sql` | `hotel_portal_support_requests` — provider → company support requests |
| V6 | `V6__provider_workflow_columns.sql` | `hotel_service_providers`: `provider_type`, `registration_number`, `approved_by`, `approved_at` |
| V7 | `V7__discount_scope.sql` | `disc_discount_codes`: scoped discounts (provider_id, applicable service/menu/room IDs); `bkg_bookings` discount context fields |
| V8 | `V8__dashboard_perf_indexes.sql` | Performance indexes on `bkg_bookings` (created_at, status+created_at, service_id) and `sys_audit_logs` (created_at DESC) |
| V9 | `V9__organizational_groups_z_codes.sql` | Rename legacy group codes G1–G7 → Z1–Z7; fix `sys_user_roles.group_id` references |
| V10 | `V10__kafka_staff_notification_delivered.sql` | `kafka_staff_notification_delivered` — idempotency table for Kafka notification consumers |
| V11 | `V11__database_hardening.sql` | Security metadata: MFA columns, GDPR flags, password history (`sys_user_password_history`), consent tables, security events, data retention policies, PII registry, session hardening, audit archive, 20+ new indexes |
| V12 | `V12__reporting_security_phase2.sql` | Audit enrichment columns, BRIN indexes, case-insensitive email index, covering indexes, materialized view `mv_pay_daily_totals`, rate limit counters (`sys_rate_limit_counters`), security alert rules/alerts tables |
| V13 | `V13__rls_audit_partition_export.sql` | GDPR export payload column; `sys_audit_logs` HASH partitioned into 8 partitions (p0–p7); RLS policies on `hotel_service_providers`, `hotel_services`, `bkg_bookings` |
| V14 | `V14__employee_soft_delete.sql` | `sys_employees` soft-delete columns (`offboarded_at/by/reason`); change `user_id` FK from CASCADE → RESTRICT; partial indexes for active/offboarded employees |
| V15 | `V15__subscription_plans.sql` | Subscription billing: `sys_plans` (FREE/STARTER/PROFESSIONAL/ENTERPRISE), `sys_customer_subscriptions`, `sys_subscription_add_ons` |
| V16 | `V16__database_best_practices.sql` | 60+ hardening changes: FK constraints, redundant index removal, CHECK constraints on all status columns, partial/composite indexes, NULL-safety fixes, TIMESTAMPTZ migration for web_content_pages, UNIQUE constraints, `fn_set_updated_at()` trigger on 31 tables, rating DOUBLE PRECISION → NUMERIC, metadata TEXT → JSONB |
| V17 | `V17__reference_data.sql` | Seed reference data: groups Z1–Z7, 15 system roles, 43 permissions, 4 departments, subscription plans, bootstrap FX rates (USD/EUR/SAR) |
| V18 | `V18__fcm_push_token.sql` | `sys_users.fcm_token VARCHAR(512)` + index for mobile push notifications |
| V19 | `V19__portal_payout_requests.sql` | `portal_payout_requests` table: UUID PK, provider_id FK, amount, currency, notes, status (PENDING/PROCESSING/COMPLETED/REJECTED), timestamps; 2 indexes |

### 4.2 Schema: All Tables

#### System / Auth Tables

| Table | Key Columns | Notes |
|---|---|---|
| `sys_groups` | id, name, code (Z1–Z7), description | Organisational groups |
| `sys_departments` | id, name, code, description | HR departments (SALES, FIN, SUP, HR) |
| `sys_permissions` | id, code, name, resource, action, scope, is_locked | 43 seeded permissions |
| `sys_roles` | id, name, code, level, group_id, navigation_item_ids JSONB, status | 15 system roles |
| `sys_users` | id, email, password_hash, role, status, mfa_enabled, mfa_secret_cipher, gdpr_*, fcm_token, token_version, deleted_at | Soft-delete via Hibernate @SQLDelete |
| `sys_user_roles` | user_id, role_id, group_id, assigned_at | Many-to-many with UNIQUE constraint |
| `sys_role_permissions` | role_id, permission_id | Many-to-many |
| `sys_employees` | id, user_id, department_id, employee_code, job_title, offboarded_at | HR records for staff |
| `sessions` | id, user_id, token, expires_at, is_invalidated, device_fingerprint, geographic_location | Active sessions |
| `sys_audit_logs` | id, action, entity_type, entity_id, user_id, old/new_value, ip_address, correlation_id | HASH-partitioned (8 partitions) |
| `sys_audit_logs_archive` | Same shape as sys_audit_logs | Cold storage for archived rows |
| `sys_notifications` | id, user_id, title, message, type, status, read_at, metadata JSONB | In-app notifications |
| `sys_security_events` | id, user_id, event_type, severity, ip_address, details JSONB | Security event log |
| `sys_security_alert_rules` | id, name, event_type, threshold, time_window_minutes, severity | Alert rule definitions |
| `sys_security_alerts` | id, rule_id, user_id, triggered_by JSONB, status | Fired security alerts |
| `sys_rate_limit_counters` | id, identifier, endpoint, request_count, window_start, window_end | DB-backed rate limiting |
| `sys_user_password_history` | id, user_id, password_hash | Password reuse prevention |
| `sys_user_consents` | id, user_id, consent_type, granted, ip_address | GDPR consent records |
| `sys_consent_audit_log` | id, user_id, action, consent_type, previous/new_value | Consent change audit |
| `sys_data_retention_policies` | id, entity_type, retention_period_days, action, enabled | Retention job configuration |
| `sys_data_export_requests` | id, user_id, status, format, payload_json | GDPR data export tracking |
| `sys_pii_field_registry` | id, table_name, column_name, pii_category, encryption_required | PII documentation |
| `sys_backup_verification_log` | id, backup_date, verification_status | Backup verification records |
| `sys_otp_verification` | (referenced in policies) | One-time passwords |
| `sys_password_reset_tokens` | (referenced in policies) | Password reset tokens |
| `sys_feature_flags` | id, flag_key, enabled, description | Runtime feature switches |
| `sys_integration_api_keys` | id, name, key_prefix, secret_hash, revoked_at | Third-party integration keys |
| `sys_plans` | id, code, name, max_users, monthly_price, allows_overage | Subscription plan catalogue |
| `sys_customer_subscriptions` | id, provider_id, plan_id, status, seat_limit | Active provider subscriptions |
| `sys_subscription_add_ons` | id, subscription_id, add_on_code, extra_seats | Seat expansion add-ons |
| `sys_system_settings` | id, setting_key, value_json | Admin KV configuration |
| `event_publication` | id, listener_id, event_type, serialized_event, status | Spring Modulith event store |
| `kafka_staff_notification_delivered` | event_id, user_id | Kafka consumer deduplication |

#### Customer / Provider Tables

| Table | Key Columns | Notes |
|---|---|---|
| `customers` | id (FK→sys_users), first_name, last_name, phone, nationality, id_document_number_cipher | Customer profile extension |
| `hotel_service_providers` | id, name, email, phone, status, provider_type, rating NUMERIC(3,2), approved_by, bank_account_number_cipher | Service provider account |
| `hotel_provider_staff` | id, provider_id, user_id, role | Provider staff roster |

#### Service / Booking Tables

| Table | Key Columns | Notes |
|---|---|---|
| `hotel_services` | id, provider_id, type (HOTEL/RESORT/RESTAURANT/TRIP/TAXI), name, status, city, country, base_price, star_rating, max_guests | Service catalogue |
| `hotel_service_images` | id, service_id, url, category, is_primary | Service photos |
| `hotel_service_rooms` | id, service_id, room_type, capacity, base_price, quantity_total/available, amenities JSONB | Room inventory |
| `hotel_service_room_images` | id, room_id, url, is_primary, display_order | Room photos |
| `hotel_rest_menu_sections` | id, service_id, title, sort_order | Restaurant menu sections |
| `hotel_rest_menu_items` | id, section_id, name, price, image_url | Menu items |
| `hotel_reviews` | id, booking_id, customer_id, service_id, rating (1–5), comment, status | Service reviews |
| `hotel_portal_support_requests` | id, provider_id, user_id, subject, body | Provider → company support |
| `disc_discount_codes` | id, code, type, value, status, approval_status, provider_id, applicable_*_ids JSONB | Discount codes with scoping |
| `bkg_bookings` | id, booking_reference, customer_id, service_id, check_in_date, check_out_date, guests, rooms, total_amount, status, special_requests | Core booking table |
| `bkg_taxi_bookings` | id, booking_id, driver_id, pickup/destination, scheduled_at, vehicle_type, status | Taxi-specific booking data |
| `portal_payout_requests` | id, provider_id, amount, currency, status (PENDING/PROCESSING/COMPLETED/REJECTED), requested_at | Provider payout requests |

#### Payment Tables

| Table | Key Columns | Notes |
|---|---|---|
| `pay_payments` | id, booking_id, amount, method, status, gateway_reference, idempotency_key, gateway_response_cipher | Payment records |
| `pay_refunds` | id, payment_id, amount, status, processed_by | Refund records |
| `pay_exchange_rates` | id, from_currency, to_currency, rate, effective_date | FX rates (UNIQUE per pair+date) |

#### Support Tables

| Table | Key Columns | Notes |
|---|---|---|
| `support_complaints` | id, ticket_number, customer_id, booking_id, subject, priority, status | Customer complaints |
| `support_complaint_comments` | id, complaint_id, user_id, comment, is_internal | Complaint thread |
| `support_internal_tickets` | id, ticket_number, reporter_id, type, subject, priority, status, assigned_to_id | Internal IT/ops tickets |
| `support_ticket_comments` | id, ticket_id, user_id, comment, is_resolution | Ticket thread |
| `support_contact_leads` | id, name, email, company, message, client_ip | Marketing lead submissions |

#### CMS / Content Tables

| Table | Key Columns | Notes |
|---|---|---|
| `web_content_pages` | id, slug (UNIQUE), content_en JSONB, content_ar JSONB, published | CMS pages (7 seeded) |

### 4.3 Materialized Views

| View | Definition | Indexes |
|---|---|---|
| `mv_pay_daily_totals` | Daily payment totals by currency (revenue_date, currency, count, completed_amount) | `UNIQUE (revenue_date, currency)` — refreshed CONCURRENTLY by scheduled job |

### 4.4 Row-Level Security

RLS is enabled on three tables (V13) and **controlled by session GUC variables**:
- `app.rls_bypass = '1'` → admin/backend bypass (default; set to `''` to enforce)
- `app.current_user_id` → bound to the authenticated user's UUID
- `app.current_provider_id` → bound to provider context

| Table | Policy | Enforcement |
|---|---|---|
| `hotel_service_providers` | Provider can see/write own row or rows where they are staff | RLS bypass default; enable via `ZIYARA_RLS_ENABLED=true` |
| `hotel_services` | Provider can see/write own services | Same |
| `bkg_bookings` | Customer sees own bookings; provider sees bookings for their services | Same |

⚠️ **RLS is disabled by default.** Set `ZIYARA_RLS_ENABLED=true` to activate it. The `RlsAwareDataSource` wrapper sets the session GUCs per connection.

### 4.5 Triggers & Functions

| Function | Purpose |
|---|---|
| `fn_set_updated_at()` | BEFORE UPDATE trigger — stamps `updated_at = CURRENT_TIMESTAMP` |
| `trg_{table}_updated_at` | Applied to 31 tables (V16) |

---

## 5. Docker

### 5.1 Services Overview

The `docker-compose.yml` defines **11 services** across multiple profiles:

| Service | Image | Ports | Profile | Description |
|---|---|---|---|---|
| `postgres` | postgres:15 | 5432 | (base/always) | Primary database |
| `redis` | redis:7-alpine | 6379 | (base/always) | Cache / rate-limit |
| `kafka` | bitnami/kafka:3 | 9092 | (base/always) | Staff notifications |
| `pgadmin` | dpage/pgadmin4 | 5050 | **(no profile — always starts)** | DB admin UI |
| `backend` | ./core (Dockerfile) | 8080 | (base/always) | Spring Boot API |
| `frontend` | ./front/my-app (Dockerfile, SURFACE=company) | 80 | `full` | Company dashboard only |
| `dashboard` | ./front/my-app (Dockerfile, SURFACE=company) | 3000 | `legacy` | Legacy dashboard port |
| `dashboard-company` | ./front/my-app (SURFACE=company) | 3001 | `multidomain` | Company dashboard |
| `dashboard-provider` | ./front/my-app (SURFACE=provider) | 3002 | `multidomain` | Provider portal |
| `landing` | ./front/my-app (SURFACE=landing) | 3003 | `multidomain` | Landing/booking site |
| `proxy` | ./infra/nginx (multi-domain.conf) | 80, 443 | `multidomain` | Nginx reverse proxy |

**Network:** `ziyarah-network` bridge, subnet `172.28.0.0/16`  
**Volumes:** `postgres_data`, `redis_data`, `media_data`, `pgadmin_data`

### 5.2 Docker Profiles

| Profile | What starts | Use case |
|---|---|---|
| (none / base) | postgres, redis, kafka, pgadmin, backend | Backend-only development |
| `full` | base + `frontend` on :80 | Single-surface deployment (company dashboard) |
| `legacy` | base + `dashboard` on :3000 | Backward compatibility with old port |
| `multidomain` | base + 3 frontend containers + nginx proxy on :80 | Full production-like multi-surface setup |

### 5.3 How to Run Each Profile

#### Prerequisites

1. **Copy and configure `.env`:**
   ```bash
   cp .env.example .env
   # Edit .env — fill in required values:
   # POSTGRES_PASSWORD=your-strong-password
   # JWT_SECRET=your-64+-char-secret
   # PGADMIN_DEFAULT_PASSWORD=your-pgadmin-password
   ```

2. **Ensure Docker Engine 20.10+ and Docker Compose 2.0+ are installed.**

---

#### Mode 1: Backend Only (fastest startup)
```bash
docker compose up -d --build postgres redis kafka backend
# pgadmin also starts (no profile tag — cannot be excluded without --profiles override)
```
Access:
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/swagger-ui.html
- pgAdmin: http://localhost:5050

---

#### Mode 2: Full Stack — Company Dashboard Only (`full` profile)
```bash
docker compose --profile full up -d --build
```
Access:
- Company Dashboard: http://localhost:80
- API: http://localhost:8080/api/v1

---

#### Mode 3: Legacy Port (`legacy` profile)
```bash
docker compose --profile legacy up -d --build
```
Access:
- Company Dashboard: http://localhost:3000

---

#### Mode 4: Multi-Domain (all 3 surfaces) (`multidomain` profile)

**Step 1 — Add hosts file entries** (one-time setup):

*Linux/macOS:* Edit `/etc/hosts`  
*Windows:* Edit `C:\Windows\System32\drivers\etc\hosts`

```
127.0.0.1  app.local
127.0.0.1  partners.local
127.0.0.1  www.local
```

**Step 2 — Start:**
```bash
docker compose --profile multidomain up -d --build
```

Access:
- Company Dashboard: http://app.local
- Provider Portal: http://partners.local
- Landing / Booking: http://www.local
- API (via proxy): http://app.local/api/v1 (or any domain)

---

#### Local Development (no Docker)

**Backend (Gradle — NOT Maven):**
```bash
# Requires PostgreSQL running locally on localhost:5432
# Create DB first (see below)

cd core
./gradlew bootRun --args='--spring.profiles.active=dev'
# Or build and run JAR:
./gradlew build -x test
java -jar build/libs/core-1.0.0.jar --spring.profiles.active=dev
```

**Create local database:**
```sql
psql -U postgres
CREATE USER ziyarah_user WITH PASSWORD 'ziyarah_password';
CREATE DATABASE ziyarah OWNER ziyarah_user;
GRANT ALL PRIVILEGES ON DATABASE ziyarah TO ziyarah_user;
\q
```

**Frontend:**
```bash
cd front/my-app
npm install

# Company dashboard:
VITE_APP_SURFACE=company npm run dev

# Provider portal:
VITE_APP_SURFACE=provider npm run dev

# Landing:
VITE_APP_SURFACE=landing npm run dev
```
Dev server runs on http://localhost:5173 (Vite default).

---

#### Useful Docker Commands
```bash
# View all running containers
docker compose ps

# Stream logs
docker compose logs -f backend
docker compose logs -f frontend

# Stop without removing volumes (preserves DB data)
docker compose down

# Stop AND remove volumes (wipes database)
docker compose down -v

# Rebuild a single service
docker compose --profile full up -d --build frontend

# Open psql in running postgres container
docker compose exec postgres psql -U ziyarah_user -d ziyarah

# Check backend health
curl http://localhost:8080/api/v1/actuator/health
```

### 5.4 Issues Found & Required Fixes

#### 🔴 Issue 1 — nginx missing WebSocket upgrade headers (STOMP broken in Docker)

**File:** `infra/nginx/multi-domain.conf`  
**Problem:** The nginx config has no `/ws` location block with WebSocket upgrade headers. The STOMP dashboard real-time push will silently fail to connect in the `multidomain` Docker profile.

**Fix — add a `/ws` proxy location to each server block in `multi-domain.conf`:**

```nginx
# Add inside EACH server block (app.local, partners.local, www.local):
location /ws {
    proxy_pass http://backend:8080/api/v1/ws;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
}
```

---

#### 🔴 Issue 2 — Backend Dockerfile copies `*-SNAPSHOT.jar` but project version is `1.0.0`

**File:** `core/Dockerfile`  
**Problem:** The runtime stage runs:
```dockerfile
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
```
But `core/build.gradle.kts` sets `version = "1.0.0"`, so the JAR built is `core-1.0.0.jar` (no `-SNAPSHOT` suffix). **The Docker build will fail** with "no such file" at the COPY step.

**Fix:**
```dockerfile
# Option A: Use a wildcard that matches both:
COPY --from=build /app/build/libs/core-*.jar app.jar

# Option B: Hardcode the exact name:
COPY --from=build /app/build/libs/core-1.0.0.jar app.jar
```

---

#### 🟡 Issue 3 — pgadmin service has no profile (starts unconditionally)

**File:** `docker-compose.yml`  
**Problem:** The `pgadmin` service has no `profiles:` key, so it starts with every `docker compose up -d` command, even in production-like mode where it's unnecessary and a potential security exposure.

**Fix — add a profile to pgadmin:**
```yaml
pgadmin:
  profiles: [full, legacy, dev]   # or a dedicated "tools" profile
  ...
```

---

#### 🟡 Issue 4 — `frontend` service (profile `full`) not started by bare `docker compose up -d`

**Behavior:** `docker compose up -d --build` (without `--profile full`) starts only the base services (postgres, redis, kafka, pgadmin, backend). The frontend is silently absent.

**Mitigation:** Always specify the profile explicitly:
```bash
docker compose --profile full up -d --build
```

---

#### 🟡 Issue 5 — RUN.md uses `mvn` commands throughout

**File:** `RUN.md`  
**Problem:** The project uses **Gradle**, not Maven. RUN.md tells developers to run `mvn clean package`, `mvn spring-boot:run`, etc. These commands will fail.

**Correct Gradle equivalents:**
```bash
# Build (skip tests):
./gradlew build -x test

# Run with Spring Boot plugin:
./gradlew bootRun

# Run with specific profile:
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run tests:
./gradlew test
```

---

#### 🟡 Issue 6 — `/etc/hosts` entries required for multidomain profile (not documented)

The nginx `multi-domain.conf` routes traffic by `Host` header to `app.local`, `partners.local`, and `www.local`. Without local hosts file entries pointing these to `127.0.0.1`, the browser will not resolve these names.

**Fix:** Document the required `/etc/hosts` additions in `RUN.md` (see Mode 4 instructions above).

---

#### 🟢 Issue 7 — Redis & Kafka start unconditionally but are optional features

Redis is only used when `ZIYARA_RATE_LIMIT_LOGIN_REDIS_ENABLED=true`. Kafka is only consumed when `ZIYARA_NOTIFICATIONS_KAFKA_ENABLED=true`. Both start regardless.

**Impact:** Minor — adds ~100 MB RAM overhead in dev. No functional breakage; the backend handles missing connections gracefully.

---

### 5.5 Environment Variables Reference

#### Required (must set in `.env` before first run)

| Variable | Description | Example |
|---|---|---|
| `POSTGRES_PASSWORD` | PostgreSQL superuser password | `openssl rand -hex 24` |
| `JWT_SECRET` | HMAC signing key (min 64 chars) | `openssl rand -hex 48` |
| `PGADMIN_DEFAULT_PASSWORD` | pgAdmin web UI password | any strong password |

#### Required in Production

| Variable | Description |
|---|---|
| `ZIYARA_PII_ENCRYPTION_KEY_BASE64` | AES-256 key for MFA secrets (`openssl rand -base64 32`) |
| `PAYMENT_WEBHOOK_SECRET` | HMAC secret for payment gateway callbacks |
| `JWT_COOKIE_SECURE=true` | HTTPS-only cookie flag |
| `ZIYARA_CORS_ALLOWED_ORIGINS` | Comma-separated production domain list |
| `APP_DEMO_SUPER_ADMIN_ENABLED=false` | Disable seeded demo admin after first login |

#### Optional but Recommended

| Variable | Default | Description |
|---|---|---|
| `ZIYARA_RLS_ENABLED` | `false` | Enable PostgreSQL row-level security |
| `ZIYARA_SECURITY_HSTS_ENABLED` | `false` | Enable HSTS header |
| `ZIYARA_SECURITY_MFA_REQUIRED_ROLES` | `""` | Roles requiring TOTP (empty in dev!) |
| `ZIYARA_PASSWORD_MIN_ZXCVBN_SCORE` | `0` | Password strength: 0=off, 3=prod recommended |
| `ZIYARA_RATE_LIMIT_LOGIN_REDIS_ENABLED` | `false` | Use Redis for distributed rate-limiting |
| `APP_MEDIA_STORAGE_BACKEND` | `local` | `local` or `s3` |
| `ZIYARA_FX_REFRESH_ENABLED` | `false` | Auto-refresh FX rates |
| `ZIYARA_REPORTING_MV_REFRESH_ENABLED` | `false` | Auto-refresh payment materialized view |
| `ZIYARA_DATA_RETENTION_ENABLED` | `false` | Enable data retention/archive job |
| `APP_NOTIFICATIONS_EMAIL_ENABLED` | `false` | Enable SMTP email notifications |

---

## 6. Security Notes

⚠️ **Critical: Items that must be done before any non-dev deployment:**

1. **`.env` secrets are placeholders** — `POSTGRES_PASSWORD`, `JWT_SECRET`, `PGADMIN_DEFAULT_PASSWORD` must all be replaced with real strong values.

2. **`seed_dev.sql` must NEVER run in staging or production** — it inserts demo users with known passwords.

3. **`PAYMENT_WEBHOOK_SECRET` must not be weak or empty** in a live payment integration. An empty secret means any POST to `/api/v1/payments/webhook` bypasses HMAC validation.

4. **`APP_DEMO_SUPER_ADMIN_ENABLED=false` after first production login** — the seeded `super_admin@ziyarah.com / Demo123!` account is created on every startup when this flag is `true`.

5. **Leave `ZIYARA_SECURITY_MFA_REQUIRED_ROLES` EMPTY in dev** — setting it before TOTP enrollment will lock out all seeded admin accounts.

6. **JWT stored in `sessionStorage`** (not `localStorage`) to reduce XSS persistence window. Tokens are cleared on tab close.

7. **CORS**: Default config allows localhost origins in dev. Set `ZIYARA_CORS_ALLOWED_ORIGINS` to your actual domains in production.

8. **RLS is opt-in** (`ZIYARA_RLS_ENABLED=false` default). Enable for multi-tenant data isolation in production.

---

## 7. Known Gaps & Recommendations

### Infrastructure

| # | Priority | Gap | Recommendation |
|---|---|---|---|
| 1 | 🔴 High | nginx missing WebSocket upgrade headers | Add `/ws` location block with `Upgrade` + `Connection` headers (see §5.4 Issue 1) |
| 2 | 🔴 High | Backend Dockerfile SNAPSHOT mismatch | Change COPY glob to `core-*.jar` (see §5.4 Issue 2) |
| 3 | 🟡 Medium | RUN.md references `mvn` | Replace all Maven commands with Gradle equivalents |
| 4 | 🟡 Medium | pgadmin has no profile | Add `profiles: [dev, tools]` to prevent production exposure |
| 5 | 🟡 Medium | `/etc/hosts` entries undocumented | Add to RUN.md for multidomain mode |

### Backend

| # | Priority | Gap | Recommendation |
|---|---|---|---|
| 6 | 🟡 Medium | Swagger UI enabled by default | Disable in `application-prod.yml`: `springdoc.swagger-ui.enabled: false` |
| 7 | 🟡 Medium | No HTTPS/TLS in Docker | Add TLS termination to nginx proxy with Let's Encrypt or self-signed cert |
| 8 | 🟢 Low | Kafka always starts but optional | Consider adding `kafka` to a `messaging` profile |
| 9 | 🟢 Low | `application-prod.yml` not audited | Verify it sets `app.demo.super-admin.enabled: false` and `ziyara.security.headers.hsts-enabled: true` |

### Frontend

| # | Priority | Gap | Recommendation |
|---|---|---|---|
| 10 | 🟡 Medium | Password reset flow is a no-op | "Forgot Password" button exists in LoginPage but only if SMTP is configured. Consider showing a message or disabling the button when email is disabled |
| 11 | 🟡 Medium | No customer account page in landing surface | Customers can book but have no self-service booking history page |
| 12 | 🟢 Low | Landing surface has no password reset page | Add `/reset-password?token=...` route to landing surface for email link flow |

### Database

| # | Priority | Gap | Recommendation |
|---|---|---|---|
| 13 | 🟢 Low | `sys_service_providers` FK in V15 tries wrong table name | V15 uses `sys_service_providers` but table is `hotel_service_providers`. The FK is applied conditionally (DO block) so it silently skips if table name doesn't match — verify FK is applied. |
| 14 | 🟢 Low | Materialized view refresh requires CONCURRENTLY which needs at least one row | Empty DB will skip refresh silently — fine for dev, but log message is swallowed. |
