# Testing Implementation Plan

## Current State

| Category | Count | Tools |
|---|---|---|
| Unit tests (Mockito) | 14 | JUnit 5, Mockito |
| Controller slice tests | 12 | `@WebMvcTest`, MockMvc |
| Architecture tests | 2 | ArchUnit 1.3 |
| Integration / functional tests | 8 | `@SpringBootTest`, Testcontainers (Postgres + Kafka) |
| **Untested controllers** | **28 of 40** | — |
| **Missing containers** | Redis, MailHog, full Kafka pipeline | — |
| JaCoCo gate | 60 % service-layer line coverage | — |

---

## Phase 1 — Test Infrastructure (do first, everything else builds on this)

### 1.1 — Base integration test class

Create `src/test/java/com/ziyara/backend/AbstractIntegrationTest.java`.

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Tag("docker")
@Transactional          // each test rolls back automatically
@Import({
    TestcontainersConfiguration.class,   // already exists
    RedisTestcontainersConfiguration.class,
    KafkaTestcontainersConfiguration.class
})
public abstract class AbstractIntegrationTest {
    @Autowired protected TestRestTemplate rest;
    @Autowired protected JdbcTemplate jdbc;
    // shared helpers: loginAs(role), seedProvider(), etc.
}
```

Every new integration test extends this. No boilerplate repeated.

### 1.2 — Expand TestcontainersConfiguration

**File:** `TestcontainersConfiguration.java` (already exists — add Redis and MailHog beans)

```java
@Bean @ServiceConnection
RedisContainer redis() {
    return new RedisContainer(DockerImageName.parse("redis:7.2-alpine"));
}

@Bean
GenericContainer<?> mailhog() {
    return new GenericContainer<>("mailhog/mailhog:v1.0.1")
        .withExposedPorts(1025, 8025)   // SMTP, web UI
        .waitingFor(Wait.forHttp("/").forPort(8025));
}
```

Kafka is already in `TestcontainersConfiguration` (Postgres + Kafka declared). Wire MailHog SMTP into `spring.mail.*` via `DynamicPropertySource`.

### 1.3 — Test data builders

Create `src/test/java/com/ziyara/backend/support/` package:

| Class | Responsibility |
|---|---|
| `UserFixtures` | `superAdmin()`, `providerOwner()`, `portalStaff()`, `customer()` — returns seeded `UserJpa` rows + JWT |
| `ProviderFixtures` | `approvedProvider()`, `pendingProvider()` |
| `ServiceFixtures` | `activeService(providerId)`, `draftService(providerId)` |
| `BookingFixtures` | `confirmedBooking(userId, serviceId)` |
| `JwtHelper` | `tokenFor(userId, role, permissions)` — signs with test secret |

These replace one-off SQL in individual tests and keep data consistent.

### 1.4 — `application-test.yml`

```yaml
spring:
  flyway:
    baseline-on-migrate: true
  jpa:
    show-sql: false
  mail:
    host: ${MAILHOG_HOST}
    port: ${MAILHOG_SMTP_PORT}
app:
  notifications:
    email:
      enabled: true     # turn on so email tests fire
  pii:
    encryption-key-base64: dGVzdGtleXRlc3RrZXl0ZXN0a2V5dGVzdGtleTA=  # 32-byte test key
  demo:
    super-admin:
      enabled: true
```

---

## Phase 2 — Architecture Tests (ArchUnit)

**File:** `CleanArchitectureDddTest.java` (56 rules already, add the below)

### 2.1 — Layer dependency rules (add to existing test)

| Rule | ArchUnit expression |
|---|---|
| Controllers only import `application.*` and `infrastructure.security.*` | `noClasses().that().resideInPackage("..presentation..")` `.should().dependOnClassesThat().resideInPackage("..domain..")` |
| Use cases have no Spring annotations | `noClasses().that().resideInPackage("..domain.usecase..")` `.should().beAnnotatedWith(Service.class)` |
| Infrastructure does not import presentation | `noClasses().that().resideInPackage("..infrastructure..")` `.should().dependOnClassesThat().resideInPackage("..presentation..")` |
| Domain has zero framework imports | `noClasses().that().resideInPackage("..domain..")` `.should().dependOnClassesThat().resideInPackage("org.springframework..")` |

### 2.2 — Naming convention rules (new test class: `NamingConventionArchTest`)

| Rule |
|---|
| Classes ending `Service` live in `application.service` or `domain` |
| Classes ending `Controller` are annotated `@RestController` |
| Classes ending `Repository` are interfaces |
| Classes ending `RepositoryAdapter` implement a domain `*Repository` interface |
| Classes ending `UseCase` live in `domain.usecase` |
| `Jpa`-suffixed classes live in `infrastructure.persistence.entity` |

### 2.3 — Module API boundary rules (extend `ModuleBoundariesArchTest`)

Each module under `modules/` may only be accessed through its `api/` interface, never by importing its internal classes directly. The existing test covers this — verify it covers all 9 modules (booking, payment, notification, subscription, pricing, service, sys, webhook, taxi).

### 2.4 — Circular dependency rule

```java
SlicesRuleDefinition.slices()
    .matching("com.ziyara.backend.(*)..")
    .should().beFreeOfCycles()
```

---

## Phase 3 — Controller Slice Tests (`@WebMvcTest`)

Pattern for every new test class:

```java
@WebMvcTest(XyzController.class)
@Import({WebMvcSecuritySliceConfiguration.class, WebMvcConfigurationPropertiesImport.class})
class XyzControllerWebMvcTest {
    @MockitoBean XyzService service;
    @Autowired MockMvc mvc;
    // ...
}
```

### 3.1 — Controllers with zero coverage (28 to add)

Priority order — highest risk first:

| # | Controller | Key scenarios to test |
|---|---|---|
| 1 | `AuthController` | Login 200 + JWT in cookie, wrong password 401, locked account 403, OTP flow, refresh token |
| 2 | `UserController` | `GET /me` returns user, `PATCH /me` updates, `GET /me/permissions` returns list, password change validates old password |
| 3 | `ServiceProviderController` | Admin approve/reject/suspend, non-admin 403, `GET /me` scoped to authenticated provider |
| 4 | `BookingController` | Create booking 201, confirm, cancel, IDOR check (can't cancel other user's booking) |
| 5 | `PaymentController` | Initiate payment, complete, webhook signature verification 401 on bad sig |
| 6 | `AdminPayoutController` | All payout state transitions, bulk approve, staff-only access |
| 7 | `DashboardController` | All 11 KPI endpoints return 200 for admin, 403 for customer |
| 8 | `RoleManagementController` | CRUD roles, assign permissions, permission catalogue |
| 9 | `PortalController` | Provider-scoped — GET /services returns only own services |
| 10 | `PortalStaffController` | Create staff, scoped to provider, duplicate email 409 |
| 11 | `ReviewController` | Create review, moderate, IDOR (can't edit other user's review) |
| 12 | `DiscountController` | Create, validate, apply, approve, deactivate |
| 13 | `ComplaintController` | Create, escalate, resolve, IDOR |
| 14 | `NotificationController` | `GET /me` returns own notifications, mark-read, read-all |
| 15 | `AuditLogController` | Admin-only, filter by entity/user, pagination |
| 16 | `ReportController` | Date range validation, export format, admin-only |
| 17 | `CurrencyController` | CRUD rates, conversion formula, unauthenticated 401 |
| 18 | `ContentPageController` | Public GET /{slug}, admin-only PUT /{slug} |
| 19 | `MfaController` | Enroll start → confirm → disable lifecycle |
| 20 | `EmployeeController` | CRUD employees, role assignment |
| 21 | `DepartmentController` | CRUD departments |
| 22 | `InternalTicketController` | Create ticket, lifecycle transitions, IDOR |
| 23 | `TaxiBookingController` | Get active, assign driver, status update |
| 24 | `SubscriptionController` | View plans, activate, add-ons |
| 25 | `AdminFeatureFlagsController` | *(already tested — verify coverage)* |
| 26 | `AdminSystemSettingsController` | *(already tested — verify coverage)* |
| 27 | `SuperAdminController` | Super-admin-only guard, search, restore/permanent-delete |
| 28 | `WebhookSubscriptionController` | CRUD webhooks, ping, delivery log |

### 3.2 — Security test class (`AllEndpointsSecurityWebMvcTest` — already exists, extend it)

Add assertions for every new endpoint added since the test was written:

- Every non-public endpoint returns `401` when no JWT is provided.
- Every admin endpoint returns `403` for a `CUSTOMER` role JWT.
- Every provider-scoped endpoint returns `403` for a `CUSTOMER` role JWT.
- CSRF: mutating endpoints (`POST`/`PATCH`/`DELETE`) without CSRF token return `403` (for cookie-based clients).

### 3.3 — Validation tests (add to each controller test)

For every `@RequestBody` DTO with `@Valid`:

- Send empty body → `400 Bad Request` with field errors in response.
- Send invalid field values (negative amounts, past dates, oversized strings) → `400`.
- Send unexpected extra fields → ignored (Jackson default), still `200/201`.

---

## Phase 4 — Integration Tests (Testcontainers)

All classes extend `AbstractIntegrationTest` from Phase 1.

### 4.1 — Database integration

**`FlywayMigrationIntegrationTest`**
- All migrations run cleanly on a fresh container with no errors.
- Schema version after migration matches the highest V-number in `db/migration/`.
- Rollback simulation: drop a table, verify Flyway detects checksum mismatch.

**`RepositoryAdapterIntegrationTest`** (one per aggregate root)
- `UserRepositoryAdapterTest` — CRUD via the domain `UserRepository` interface.
- `BookingRepositoryAdapterTest` — create, findById, updateStatus, pagination.
- `ServiceProviderRepositoryAdapterTest` — approve/suspend state persisted.
- `PaymentRepositoryAdapterTest` — state machine transitions persisted.
- Verify `@Transactional` rollback: simulate exception mid-write, assert no partial row.

### 4.2 — Auth & security integration

**`AuthIntegrationTest`** extends `AbstractIntegrationTest`
- Full register → login → get `/me` cycle with real JWT.
- Login with wrong password 5× triggers rate-limit (Bucket4j) → `429 Too Many Requests`.
- Expired JWT returns `401`.
- Refresh token issues new access token.
- Logout invalidates JWT (Redis blocklist check).
- MFA enroll → generate TOTP code → `/login` with OTP succeeds.

### 4.3 — Booking flow integration

**`BookingLifecycleIntegrationTest`** extends `AbstractIntegrationTest`
- Customer creates booking → provider confirms → customer cancels: each state persisted.
- Double-booking prevention: same slot, two concurrent requests → one `409 Conflict`.
- IDOR: customer A cannot cancel customer B's booking → `403`.
- Booking created event published to Kafka; consumer receives it within 5 s (use `Awaitility`).

### 4.4 — Payment integration

**`PaymentIntegrationTest`** extends `AbstractIntegrationTest`
- Create booking → initiate payment → mock payment gateway callback → booking status becomes `PAID`.
- Webhook with invalid signature → `401 Unauthorized`, booking status unchanged.
- Refund flow: paid booking → refund initiated → status transitions to `REFUNDED`.

### 4.5 — Kafka integration

**`KafkaNotificationPipelineIntegrationTest`** extends `AbstractIntegrationTest`
- Approve a provider → `BookingCreatedEvent` published.
- `StaffNotificationInboxProcessor` consumes it → notification row inserted in DB.
- Message lands in DLQ after 3 failed retries (inject a mock that throws).
- Use `KafkaConsumer` directly to assert topic offset advances.

### 4.6 — Redis integration

**`RateLimitingIntegrationTest`** extends `AbstractIntegrationTest` (uses Redis container)
- Login endpoint: 5 failed attempts from same IP within 1 min → `429`.
- After TTL expires → can attempt again.
- Redis key expires correctly: `TTL ziyara:rate:login:<ip>` decrements.

**`JwtBlocklistIntegrationTest`**
- Logout → JWT added to Redis blocklist.
- Using blocklisted token → `401`.
- Blocklist entry expires after JWT TTL (assert Redis key gone after expiry).

### 4.7 — Email notification integration

**`EmailNotificationIntegrationTest`** extends `AbstractIntegrationTest` (uses MailHog container)
- Register new user → welcome email arrives in MailHog inbox (`GET http://mailhog:8025/api/v2/messages`).
- Forgot password → reset email with token link received.
- Booking confirmation → confirmation email received.
- Assert email `To:`, `Subject:`, and body contain expected values.

### 4.8 — Row-Level Security integration

**`RowLevelSecurityIntegrationTest`** extends `AbstractIntegrationTest`
- Enable RLS in test profile (`app.rls.enabled=true`).
- Provider A's services are invisible when queried with Provider B's session GUC.
- Admin user has no GUC set → can see all rows.
- Verifies PostgreSQL policy `ziyara_tenant_isolation` by running raw SQL through `JdbcTemplate` with `SET SESSION ziyara.current_provider_id = X`.

---

## Phase 5 — End-to-End Functional Tests

These run the real server (`@SpringBootTest(webEnvironment = RANDOM_PORT)`), Flyway-migrated DB, and real JWT.

### 5.1 — Extend existing `EndpointFunctionalTest`

Already tests auth, `/me`, and register. Add:

- `GET /dashboard/kpis` → super-admin gets data, customer gets `403`.
- `POST /providers` + `POST /providers/{id}/approve` → provider status becomes `ACTIVE`.
- `GET /portal/services` → returns only the authenticated provider's services.
- `POST /discounts` + `POST /discounts/{id}/approve` → discount becomes `ACTIVE`.
- `POST /bookings` + `POST /bookings/{id}/confirm` → booking status becomes `CONFIRMED`.

### 5.2 — New: `PermissionScopingFunctionalTest`

Verifies that RBAC permissions are enforced end-to-end (not just mocked):

- Custom role with `bookings:read` only → `GET /bookings` succeeds, `POST /bookings` returns `403`.
- Custom role with no `users:write` → `PATCH /users/{id}` returns `403`.
- Revoking a permission from a role → token issued before revocation still works until cache TTL expires.

### 5.3 — New: `OwnershipEnforcementTest` (already exists — verify these scenarios)

Confirm coverage of:
- Customer cannot view another customer's booking.
- Provider cannot view another provider's services.
- Staff user with limited scope cannot access super-admin endpoints.
- Provider cannot impersonate another provider via `providerId` path variable.

---

## Phase 6 — Coverage Gates

Update `build.gradle.kts`:

```kotlin
jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf(
                "com.ziyara.backend.application.service.*",
                "com.ziyara.backend.domain.usecase.*",
                "com.ziyara.backend.presentation.controller.*",
                "com.ziyara.backend.infrastructure.persistence.adapter.*"
            )
            limit {
                counter = "LINE"
                value   = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()   // raise from 60%
            }
        }
        rule {
            // Critical paths must be near-complete
            element = "CLASS"
            includes = listOf(
                "com.ziyara.backend.application.service.AuthService",
                "com.ziyara.backend.application.service.PaymentService",
                "com.ziyara.backend.application.service.BookingService"
            )
            limit {
                counter = "LINE"
                value   = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}
```

---

## Implementation Order

| Phase | Description | Effort | Blocks |
|---|---|---|---|
| **1** | Base class + TestcontainersConfiguration + fixtures | 1 day | All integration work |
| **2** | ArchUnit additions | 0.5 days | Nothing |
| **3.1–3.2** | High-priority controller tests (auth, user, booking, payment) | 2 days | Nothing |
| **3.3–3.4** | Remaining 24 controller tests + validation | 3 days | Nothing |
| **4.1–4.2** | DB + auth integration tests | 1 day | Phase 1 |
| **4.3–4.5** | Booking + payment + Kafka pipeline | 1.5 days | Phase 1 |
| **4.6–4.8** | Redis + email integration | 1 day | Phase 1 |
| **5** | Functional test expansions | 1 day | Phase 1, 4 |
| **6** | JaCoCo gate raise to 80 % | 0.5 days | Phase 3, 4 |
| **Total** | | **~11 days** | |

---

## File Layout After All Phases

```
src/test/java/com/ziyara/backend/
│
├── AbstractIntegrationTest.java                   ← Phase 1
├── TestcontainersConfiguration.java               ← Phase 1 (expand)
│
├── support/
│   ├── UserFixtures.java
│   ├── ProviderFixtures.java
│   ├── ServiceFixtures.java
│   ├── BookingFixtures.java
│   └── JwtHelper.java
│
├── arch/
│   ├── CleanArchitectureDddTest.java              ← Phase 2 (expand)
│   ├── ModuleBoundariesArchTest.java              ← Phase 2 (expand)
│   └── NamingConventionArchTest.java              ← Phase 2 (new)
│
├── presentation/controller/
│   ├── AllEndpointsSecurityWebMvcTest.java        ← Phase 3 (expand)
│   ├── AuthControllerWebMvcTest.java
│   ├── UserControllerWebMvcTest.java
│   ├── BookingControllerWebMvcTest.java
│   ├── PaymentControllerWebMvcTest.java
│   ├── AdminPayoutControllerWebMvcTest.java
│   ├── DashboardControllerWebMvcTest.java
│   ├── RoleManagementControllerWebMvcTest.java    ← already exists
│   ├── PortalControllerWebMvcTest.java
│   ├── PortalStaffControllerWebMvcTest.java       ← already exists
│   ├── ReviewControllerWebMvcTest.java
│   ├── DiscountControllerWebMvcTest.java
│   ├── ComplaintControllerWebMvcTest.java
│   ├── NotificationControllerTest.java            ← already exists
│   ├── AuditLogControllerWebMvcTest.java
│   ├── ReportControllerWebMvcTest.java
│   ├── CurrencyControllerWebMvcTest.java          ← already exists (partial)
│   ├── ContentPageControllerWebMvcTest.java
│   ├── MfaControllerWebMvcTest.java
│   ├── EmployeeControllerWebMvcTest.java
│   ├── DepartmentControllerWebMvcTest.java
│   ├── InternalTicketControllerWebMvcTest.java
│   ├── TaxiBookingControllerWebMvcTest.java       ← already exists (partial)
│   ├── SubscriptionControllerWebMvcTest.java
│   ├── SuperAdminControllerWebMvcTest.java
│   ├── WebhookSubscriptionControllerWebMvcTest.java
│   ├── PortalSupportRequestsControllerWebMvcTest.java  ← already exists
│   ├── AdminFeatureFlagsControllerWebMvcTest.java      ← already exists
│   ├── AdminSystemSettingsControllerWebMvcTest.java    ← already exists
│   └── AdminIntegrationApiKeysControllerWebMvcTest.java ← already exists
│
├── integration/
│   ├── FlywayMigrationIntegrationTest.java        ← Phase 4.1
│   ├── UserRepositoryAdapterTest.java             ← Phase 4.1
│   ├── BookingRepositoryAdapterTest.java          ← Phase 4.1
│   ├── PaymentRepositoryAdapterTest.java          ← Phase 4.1
│   ├── AuthIntegrationTest.java                   ← Phase 4.2
│   ├── BookingLifecycleIntegrationTest.java       ← Phase 4.3
│   ├── PaymentIntegrationTest.java                ← Phase 4.4
│   ├── KafkaNotificationPipelineIntegrationTest.java ← Phase 4.5
│   ├── RateLimitingIntegrationTest.java           ← Phase 4.6
│   ├── JwtBlocklistIntegrationTest.java           ← Phase 4.6
│   ├── EmailNotificationIntegrationTest.java      ← Phase 4.7
│   └── RowLevelSecurityIntegrationTest.java       ← Phase 4.8
│
└── functional/
    ├── EndpointFunctionalTest.java                ← already exists (expand)
    ├── PermissionScopingFunctionalTest.java       ← Phase 5.2
    └── OwnershipEnforcementTest.java              ← already exists (verify)
```

---

## Quick-Reference: Running Tests

```bash
# Fast (no Docker) — unit + slice + arch tests only
./gradlew test

# Full suite including Testcontainers
./gradlew test -PrunDockerTests

# Coverage report (opens build/reports/jacoco/test/html/index.html)
./gradlew jacocoTestReport

# Coverage gate check (fails build if < 80%)
./gradlew check

# Single test class
./gradlew test --tests "com.ziyara.backend.integration.AuthIntegrationTest"
```
