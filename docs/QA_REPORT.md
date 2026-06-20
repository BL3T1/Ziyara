# QA Audit Report — Ziyara Backend

**Date:** 2026-06-07  
**Auditor:** Senior QA / Test Architect (DDD + Clean Architecture)  
**Project:** `core` — Spring Boot 3.5.12 / Java 21 / Modular Monolith

---

## Executive Summary

| Metric | Current | Target |
|---|---|---|
| Service unit test coverage | 17 / 64 (26.6%) | 80% |
| Controller @WebMvcTest coverage | 13 / 42 (31%) | 70% |
| ArchUnit rules | 57 rules / 10 groups | ✓ |
| JaCoCo gate | 60% on `application.service.*` | 80% |
| @PreAuthorize coverage | 39 / 42 controllers (93%) | ✓ |
| Testcontainers (DB) | PostgreSQL only | + Redis + MailHog |
| Critical missing scenarios | 5 | 0 |

---

## 1. Build & Dependency Configuration

**File:** `core/build.gradle.kts`

| Item | Value |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.12 |
| ArchUnit | 1.3.0 |
| Testcontainers | 1.19.8 (PostgreSQL, Kafka) |
| Spring Security Test | 3.5.12 |

### JaCoCo Gate

```
element = PACKAGE
includes = ["com.ziyara.backend.application.service.*"]
counter  = LINE
minimum  = 0.60
```

Gate is enforced by `./gradlew check`. Excludes: DTOs, domain entities, JPA entities,
`*Configuration`, `*Properties`, `*Application` classes.

### Test Execution Commands

```bash
./gradlew test                   # Unit + arch tests (excludes @Tag("docker"))
./gradlew test -PrunDockerTests  # + Testcontainers integration tests
./gradlew check                  # test + JaCoCo 60% gate
./gradlew jacocoTestReport       # HTML report → build/reports/jacoco/
```

---

## 2. Existing Test Inventory

**Total test files:** 47

### 2.1 Architecture Tests

| File | Rules | Groups |
|---|---|---|
| `CleanArchitectureDddTest.java` | 56 rules | 10 @Nested groups, 57 @Test methods |
| `ModuleBoundariesArchTest.java` | 1 smoke rule | Module service isolation |

**CleanArchitectureDddTest groups:**

| # | Group | Focus |
|---|---|---|
| 1 | Core Layered Architecture | Dependency direction enforcement |
| 2 | Domain Layer Purity | Prohibits Spring/JPA/Lombok/Jackson in domain |
| 3 | Domain Use-Case Conventions | Naming, Spring isolation, execute() method |
| 4 | Repository Port-Adapter Pattern | Interface requirements, adapter impl |
| 5 | JPA Entity Isolation | @Entity confinement, annotation enforcement |
| 6 | Naming Conventions | Service/Controller/Repository suffixes |
| 7 | Application Service Rules | @Service, @Transactional scoping, JPA isolation |
| 8 | Presentation/Controller Rules | @RestController, entity instantiation prohibition |
| 9 | Module Boundary Rules | API-only cross-module communication |
| 10 | Cyclic Dependency Checks | Slice-based cycle detection |

**Accepted technical debt carve-outs (14 items):**
- `application.* → infrastructure.security.*` (JWT, TOTP, PII crypto)
- `application.* → infrastructure.config.*` (executor, misc)
- `application.* → infrastructure.payment.*` (PaymentGatewayProperties)
- `application.* → infrastructure.messaging.*` (StaffNotificationCommandPublisher)
- `application.* → infrastructure.media.*` (MediaStorageService)
- `application.* → infrastructure.web.*` (AuditRequestHolder)
- `application.* → infrastructure.persistence.json.*` (jOOQ UUID deserializer)
- `application.* → infrastructure.persistence.mapper.*` (NotificationMapper)
- `presentation.* → infrastructure.security.*` (cookie helper, JWT claims)
- `presentation.* → infrastructure.config.*` (misc config)
- `presentation.* → infrastructure.payment.*` (PayWebhookController)
- `presentation.* → infrastructure.web.*` (web utility beans)
- `presentation.* → infrastructure.messaging.*` (staff event publishing)
- `infrastructure.security.JwtAuthenticationFilter → application.service.JwtTokenBlocklistService`

### 2.2 Application Service Unit Tests (17 files)

All use `@ExtendWith(MockitoExtension.class)`.

| Test File | Service Under Test |
|---|---|
| `AuthServiceTest.java` | AuthService — email normalization, password validation, MFA |
| `BookingServiceTest.java` | BookingService |
| `ContactLeadServiceTest.java` | ContactLeadService |
| `DashboardServiceTest.java` | DashboardService |
| `EmployeeServiceTest.java` | EmployeeService |
| `MapServiceTest.java` | MapService |
| `NotificationServiceTest.java` | NotificationService |
| `PasswordPolicyServiceTest.java` | PasswordPolicyService |
| `PaymentServiceTest.java` | PaymentService |
| `PortalPaymentServiceTest.java` | PortalPaymentService |
| `PortalStaffServiceResetPasswordTest.java` | PortalStaffService (reset flow only) |
| `RestaurantMenuServiceTest.java` | RestaurantMenuService |
| `RoleManagementServiceGroupCodeTest.java` | RoleManagementService — group code validation |
| `RoleManagementServicePermissionsTest.java` | RoleManagementService — permission assignment |
| `ServiceImageServiceTest.java` | ServiceImageService |
| `ServiceProviderServiceTest.java` | ServiceProviderService |
| `UserMfaServiceTest.java` | UserMfaService |

### 2.3 Controller @WebMvcTest Tests (13 files)

| Test File | Controller |
|---|---|
| `AllEndpointsSecurityWebMvcTest.java` | All 29 controllers — 401/403 posture only |
| `AdminFeatureFlagsControllerWebMvcTest.java` | AdminFeatureFlagsController |
| `AdminIntegrationApiKeysControllerWebMvcTest.java` | AdminIntegrationApiKeysController |
| `AdminSystemSettingsControllerWebMvcTest.java` | AdminSystemSettingsController |
| `CurrencyControllerRatesByIdWebMvcTest.java` | CurrencyController (one endpoint) |
| `NotificationControllerTest.java` | NotificationController |
| `PaymentControllerWebMvcTest.java` | PaymentController |
| `PortalStaffControllerWebMvcTest.java` | PortalStaffController |
| `PortalSupportRequestsControllerWebMvcTest.java` | PortalSupportRequestsController |
| `PublicContactControllerWebMvcTest.java` | PublicContactController |
| `RoleManagementControllerWebMvcTest.java` | RoleManagementController |
| `TaxiBookingControllerGetByIdWebMvcTest.java` | TaxiBookingController (one endpoint) |

### 2.4 Integration / Docker Tests (@Tag("docker"), 7 files)

| Test File | Covers |
|---|---|
| `EndpointFunctionalTest.java` | Full stack — Postgres, auth flow |
| `UserCreateAndLoginFunctionalTest.java` | Registration + login cycle |
| `OwnershipEnforcementTest.java` | IDOR prevention — cross-user access blocking |
| `ApiDocsEndpointTest.java` | OpenAPI/Swagger endpoint availability |
| `OpenApiEndpointSmokeTest.java` | Swagger UI smoke |
| `StaffNotificationKafkaBrokerDockerTest.java` | Kafka broker connectivity |
| `CoreApplicationTests.java` | Application context load |

### 2.5 Infrastructure Tests (4 files)

| Test File | Covers |
|---|---|
| `StaffNotificationInboxProcessorTest.java` | Kafka message processor (Mockito) |
| `Bucket4jRateLimitAspectTest.java` | Rate-limiting AOP aspect |
| `CsrfProtectionTest.java` | CSRF token validation |
| `StaffNotificationKafkaBrokerDockerTest.java` | Kafka broker reachability |

### 2.6 Domain Tests (3 files)

- `NotificationTest.java` — domain entity logic
- `BookingPaymentStatusTest.java` — status enum/value object
- `PaymentStatusPortalTest.java` — status transitions

### 2.7 Test Configuration

| File | Purpose |
|---|---|
| `TestcontainersConfiguration.java` | `@Bean PostgreSQLContainer<?>` with `@ServiceConnection` |
| `WebMvcSecuritySliceConfiguration.java` | Security filter chain for slice tests |
| `TestCoreApplication.java` | Test suite entry point |

**Critical gap:** `TestcontainersConfiguration` declares only a PostgreSQL bean. Redis and MailHog containers are absent.

---

## 3. Coverage Gap Analysis

### 3.1 Services Without Unit Tests (47 / 64 = 73% gap)

```
AdminActivityLogService      AdminPayoutService           AuditLogService
AuthEmailNotificationService CompanyStaffRoleCatalogService ComplaintCommentService
ComplaintService             ContentPageService           CurrencyService
DashboardBootstrapService    DataExportService            DepartmentService
DiscountApprovalService      DiscountCodeService          DiscountScopeService
FeatureFlagService           GroupManagementService       HotelRoomService
IntegrationApiKeyService     InternalTicketService        JwtTokenBlocklistService
LoginRateLimitService        MailDispatchService          NavigationService
PasswordHistoryService       PermissionQueryService       PiiRegistryService
PortalService                PortalStaffService (base)    PortalSupportRequestService
PricingService               ProviderMediaSubmissionService ProviderSubscriptionService
ProviderWorkflowEmailService RbacAssignmentQueryService   ReportExportService
ReportService                ReviewService                RoleManagementService (base)
SecurityAlertService         SecurityEventService         ServiceService
SubscriptionService          SuperAdminRecoveryService    SystemSettingsService
TaxiBookingService           UserConsentService           UserPasswordService
UserRbacAssignmentService    WebhookService
```

**Risk ranking (test first):**

| Priority | Services | Reason |
|---|---|---|
| P1 | JwtTokenBlocklistService, LoginRateLimitService, UserPasswordService | Security-critical |
| P1 | TaxiBookingService, DiscountCodeService, PricingService | Revenue/booking path |
| P1 | WebhookService, ProviderWorkflowEmailService | External integrations |
| P2 | AuditLogService, SecurityAlertService, SecurityEventService | Compliance |
| P2 | ReviewService, ComplaintService, PortalSupportRequestService | User-facing |
| P3 | All remaining administrative services | Admin operations |

### 3.2 Controllers Without Dedicated @WebMvcTest (29 / 42 = 69% gap)

```
AdminPayoutController          AdminPermissionsController      AdminPiiRegistryController
AuditLogController             AuthController                  BookingController
ComplaintController            ContentPageController           DashboardController
DepartmentController           DiscountController              EmployeeController
InternalTicketController       MapController                   MfaController
PayWebhookController           PortalController                PricingController
ProviderMediaSubmissionController ProviderSubscriptionController ReportController
ReviewController               ServiceController               ServiceProviderController
SubscriptionController         SuperAdminController            TaxiBookingController (base)
TaxiTrackingController         UserConsentController           UserController
UserDataExportController       WebhookSubscriptionController
```

**Note:** `AllEndpointsSecurityWebMvcTest` covers 401/403 posture for all controllers but does not test business logic, request/response shapes, or validation constraints.

**Risk ranking:**

| Priority | Controllers | Reason |
|---|---|---|
| P1 | AuthController | Login, register, refresh — token-critical |
| P1 | UserController | 21 @PreAuthorize endpoints |
| P1 | ServiceController | 20 @PreAuthorize endpoints |
| P1 | BookingController, TaxiBookingController | Core business flow |
| P1 | PayWebhookController | Webhook signature verification |
| P2 | DiscountController, ReviewController, ServiceProviderController | Complex business logic |
| P2 | SuperAdminController, AdminPayoutController | Admin-critical operations |
| P3 | All remaining admin controllers | Lower risk |

---

## 4. Security & Authorization Audit

### 4.1 @PreAuthorize Coverage

**182 annotations across 39 / 42 controllers (93%)**

| Controller | Annotations |
|---|---|
| UserController | 21 |
| ServiceController | 20 |
| RoleManagementController | 16 |
| AdminPayoutController | 16 |
| ServiceProviderController | 14 |
| PaymentController | 10 |
| DiscountController | 9 |
| SuperAdminController | 7 |
| ReviewController | 8 |
| SubscriptionController | 5 |
| UserDataExportController | 4 |
| MfaController | 3 |
| MapController | 3 |
| PortalController | 3 |
| PortalSupportRequestsController | 3 |
| ProviderMediaSubmissionController | 3 |
| CurrencyController | 4 |
| UserConsentController | 3 |
| AdminIntegrationApiKeysController | 3 |
| All others | 1–2 each |

**Controllers intentionally public (no @PreAuthorize):**

| Controller | Reason |
|---|---|
| `AuthController` | Public login / register / refresh endpoints |
| `PayWebhookController` | Uses webhook signature validation instead |
| `PublicContactController` | Public form submission |

### 4.2 Security Test Coverage

| Scenario | Test | Status |
|---|---|---|
| Anonymous → 401 | `AllEndpointsSecurityWebMvcTest` | ✓ |
| Wrong role → 403 | `AllEndpointsSecurityWebMvcTest` | ✓ |
| Correct permission → pass | `AllEndpointsSecurityWebMvcTest` | ✓ |
| IDOR cross-user access | `OwnershipEnforcementTest` | ✓ |
| CSRF protection | `CsrfProtectionTest` | ✓ |
| Rate limiting | `Bucket4jRateLimitAspectTest` | ✓ |
| Webhook signature | No dedicated test | ✗ |

---

## 5. Integration Test Gaps

| Concern | Test | Status | Notes |
|---|---|---|---|
| PostgreSQL (Testcontainers) | `TestcontainersConfiguration` | ✓ | v15-alpine |
| Kafka processing | `StaffNotificationInboxProcessorTest`, `StaffNotificationKafkaBrokerDockerTest` | ✓ | Unit + broker test |
| IDOR / ownership | `OwnershipEnforcementTest` | ✓ | Full stack |
| Rate limiting | `Bucket4jRateLimitAspectTest` | ✓ | Unit |
| JWT blocklist (Redis) | `AuthServiceTest` (mocked) | ⚠️ Mocked only | No Redis container test |
| Email sending | `AuthServiceTest`, `ServiceProviderServiceTest` (mocked) | ⚠️ Mocked only | No MailHog test |
| Flyway migrations | Implicit in `EndpointFunctionalTest` | ⚠️ Implied | No explicit migration test |
| PostgreSQL RLS policies | Not found | ✗ Missing | |
| Concurrent booking | Not found | ✗ Missing | |
| Webhook delivery | Not found | ✗ Missing | |
| GDPR data export | Not found | ✗ Missing | |

---

## 6. Domain Model Summary

| Layer | Count |
|---|---|
| Domain entities (pure Java, no JPA) | 47 |
| Domain repository ports (plain Java interfaces) | 50 |
| Module API contracts (`modules.*.api`) | 6 known |

**Module APIs:**
- `modules.booking.api.BookingServiceApi`
- `modules.payment.api.PaymentServiceApi`
- `modules.pricing.api.PricingEngineApi`
- `modules.sys.api.AuditServiceApi`
- `modules.sys.api.RoleServiceApi`
- `modules.portal.api.PortalServiceApi`

Cross-module access enforced: callers may only use `modules.*.api` interfaces. Direct access to `modules.*.service` is forbidden by ArchUnit GROUP 9.

---

## 7. Findings & Recommendations

### 7.1 Critical (P1 — Blocking Coverage Gate Increase)

| # | Finding | Action |
|---|---|---|
| C1 | 47 / 64 services (73%) have no unit tests | Add Mockito unit tests; prioritize security and booking path first |
| C2 | JwtTokenBlocklistService has no Redis integration test | Add Testcontainers Redis bean to `TestcontainersConfiguration`; write blocklist eviction test |
| C3 | No concurrent booking / race-condition test | Add `@Tag("docker")` test with two simultaneous booking requests; verify optimistic lock exception |
| C4 | No PostgreSQL RLS enforcement test | Add Docker test connecting as different DB roles and asserting row-level policy blocks |
| C5 | `PayWebhookController` has no webhook signature validation test | Add @WebMvcTest with invalid/valid HMAC payloads |

### 7.2 High (P2 — Significant Risk)

| # | Finding | Action |
|---|---|---|
| H1 | 29 / 42 controllers (69%) lack business-logic @WebMvcTest | Add at minimum: AuthController, UserController, BookingController, ServiceController, TaxiBookingController |
| H2 | `TestcontainersConfiguration` only declares PostgreSQL | Add `RedisContainer` and `MailHogContainer` (or GenericContainer) beans |
| H3 | No explicit Flyway migration isolation test | Add `@Tag("docker")` test that runs only Flyway against a fresh container and checks expected table count |
| H4 | Email dispatch untested end-to-end | Add MailHog container; assert dispatched email subject/body via MailHog API |
| H5 | JaCoCo gate only 60% and only on `application.service.*` | Raise to 80% for `application.service.*`; add 70% gate on `infrastructure.inbound.rest.*` and `infrastructure.persistence.*` |

### 7.3 Medium (P3 — Technical Debt)

| # | Finding | Action |
|---|---|---|
| M1 | 14 ArchUnit carve-outs accepted | Document each in a tracking issue; schedule quarterly review for refactor candidates |
| M2 | `PortalStaffServiceResetPasswordTest` tests reset flow only | Expand to cover all PortalStaffService methods |
| M3 | `CurrencyControllerRatesByIdWebMvcTest` tests one endpoint only | Expand to cover all CurrencyController endpoints |
| M4 | `TaxiBookingControllerGetByIdWebMvcTest` tests one endpoint only | Expand to cover all TaxiBookingController endpoints |
| M5 | `CoreApplicationTests` is a context-load smoke test only | Add assertion on critical beans (e.g., security filter chain, JPA repositories) |

### 7.4 Low (P4 — Improvements)

| # | Finding | Action |
|---|---|---|
| L1 | No test data builders / fixtures | Create `UserFixtures`, `ProviderFixtures`, `BookingFixtures` utility classes |
| L2 | No `AbstractIntegrationTest` base class | Extract common `@SpringBootTest` + `@Import(TestcontainersConfiguration.class)` into a base class |
| L3 | No `application-test.yml` | Add test profile with shorter JWT expiry, deterministic seeds, disabled rate limiting |
| L4 | No GDPR data export test | Add unit test for `DataExportService` covering all personal-data fields |
| L5 | No `WebhookService` test | Add unit test for webhook registration, delivery, and retry logic |

---

## 8. Recommended Implementation Order

```
Phase 1 — Foundation (1–2 days)
  - Add RedisContainer + MailHogContainer to TestcontainersConfiguration
  - Create AbstractIntegrationTest base class
  - Create application-test.yml
  - Create test data builders (UserFixtures, ProviderFixtures, BookingFixtures)

Phase 2 — P1 Service Tests (3–4 days)
  - JwtTokenBlocklistService + Redis integration test
  - LoginRateLimitService, UserPasswordService, TaxiBookingService
  - WebhookService, PricingService, DiscountCodeService

Phase 3 — P1 Controller Tests (2–3 days)
  - AuthController @WebMvcTest (login, register, refresh, MFA)
  - BookingController @WebMvcTest
  - PayWebhookController @WebMvcTest (HMAC validation)
  - UserController @WebMvcTest (sample endpoints)
  - ServiceController @WebMvcTest

Phase 4 — Integration Scenarios (2 days)
  - Concurrent booking race-condition test
  - PostgreSQL RLS enforcement test
  - Flyway migration isolation test
  - Email dispatch via MailHog

Phase 5 — Coverage Gate Increase (1 day)
  - Raise JaCoCo to 80% on application.service.*
  - Add 70% gate on infrastructure.inbound.rest.*

Phase 6 — Remaining Service Tests (ongoing)
  - Remaining 37 services (P2 and P3)
  - Controller business-logic tests for remaining 24 controllers
```

**Total estimated effort:** 9–12 development days

---

## 9. Strengths Summary

The project has a strong architecture foundation that makes testing straightforward:

- **Zero architecture violations detected** — ArchUnit's 56 rules pass cleanly
- **Domain is framework-free** — pure Java, trivially unit-testable without a Spring context
- **Repository ports are plain interfaces** — easy to mock in service unit tests
- **Strong security posture** — 93% of controllers are annotated, IDOR test exists
- **Rate limiting and Kafka processing** — already tested at the unit level
- **Clean separation of concerns** — sliced @WebMvcTest tests work in isolation without wiring the full context
- **Testcontainers already configured** — PostgreSQL container is in place; extending to Redis and MailHog is low-effort
