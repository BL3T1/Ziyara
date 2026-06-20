# Ziyara Backend — Implementation Plan: 10/10

> Source: `BACKEND_ANALYSIS.md` (2026-05-25)
> Goal: resolve every finding to bring all 7 categories to 10/10 before launch.
> Estimated total effort: **~15–20 developer-days**

Each item references the analysis finding (S-1, T-1 …) and lists the exact files to touch.

---

## Phase 1 — Security Critical (2–3 days) 🔴

*These block launch. Do them first.*

---

### 1.1 Add `@PreAuthorize` to PaymentController  `S-1`

**File:** `presentation/controller/PaymentController.java`

Every endpoint needs an explicit role or ownership rule. Suggested mapping:

| Endpoint | Annotation |
|---|---|
| `POST /payments` (processPayment / initiatePayment) | `@PreAuthorize("isAuthenticated()")` + ownership enforced in service |
| `POST /{id}/complete` | `@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")` — internal transition only |
| `POST /{id}/fail` | `@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")` — internal transition only |
| `POST /{id}/confirm` | `@PreAuthorize("isAuthenticated()")` + verify caller owns booking |
| `POST /{id}/refund` | `@PreAuthorize("hasRole('ADMIN')")` |
| `GET /payments` | `@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")` |
| `GET /payments/{id}` | `@PreAuthorize("isAuthenticated()")` + ownership check |
| `GET /payments/transaction/{ref}` | `@PreAuthorize("isAuthenticated()")` + ownership check |

**Acceptance criteria:** `PaymentControllerWebMvcTest` covers all roles and forbidden scenarios (403 when wrong role).

---

### 1.2 Add `@PreAuthorize` to PricingController  `S-1`

**File:** `presentation/controller/PricingController.java`

`POST /pricing/preview` is a price-calculation endpoint. Determine if it should be:
- Public (guests checking prices before login) → add to `SecurityConfig` `permitAll` block explicitly
- Authenticated only → `@PreAuthorize("isAuthenticated()")`

Pick one, document it, add the annotation.

---

### 1.3 IDOR audit — ownership checks on all `@PathVariable UUID` endpoints  `S-2`

**Scope:** Every controller method with a `@PathVariable UUID id` that retrieves or mutates a user-owned resource.

**Pattern to enforce in application services:**
```java
// Good — explicit ownership check
Payment payment = paymentRepo.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
if (!payment.getUserId().equals(currentUserId)) {
    throw new UnauthorizedException("Access denied");
}
```

**Files to audit (minimum):**

| Controller | Method | Check needed |
|---|---|---|
| `PaymentController` | `GET /{id}`, `POST /{id}/confirm`, `POST /{id}/refund` | caller owns payment |
| `BookingController` | `GET /{id}`, `PUT /{id}`, `DELETE /{id}` | caller owns booking |
| `ComplaintController` | `GET /{id}`, `PUT /{id}` | caller owns complaint |
| `ReviewController` | `PUT /{id}`, `DELETE /{id}` | caller owns review |
| `InternalTicketController` | `PUT /{id}`, `GET /{id}` | caller is assignee or admin |
| `PortalController` | all `/{id}` service operations | caller is the provider who owns the service |

**Acceptance criteria:** Attempting to access another user's resource returns `403 FORBIDDEN`, not `200 OK` or `404`.

---

### 1.4 Enforce PII encryption key in production  `S-3` `I-2`

**File:** `infrastructure/security/PiiCryptoService.java` (or wherever the key is loaded)

Add a startup guard:
```java
@PostConstruct
void validateKey() {
    if (StringUtils.isBlank(encryptionKeyBase64)
            && activeProfiles.contains("prod")) {
        throw new IllegalStateException(
            "ZIYARA_PII_ENCRYPTION_KEY_BASE64 must be set in production. " +
            "MFA secrets will be stored in plaintext otherwise.");
    }
}
```

Also add to `application-prod.yml`:
```yaml
app:
  security:
    pii-encryption-key-base64: ${ZIYARA_PII_ENCRYPTION_KEY_BASE64}  # required — no default
```

**Acceptance criteria:** Starting the app in `prod` profile without the env var throws `IllegalStateException` and the application refuses to start.

---

### 1.5 JWT cookie hardening in prod profile  `S-5`

**File:** `src/main/resources/application-prod.yml`

```yaml
app:
  jwt:
    cookie:
      secure: true
      same-site: Strict
```

**Acceptance criteria:** A `@SpringBootTest` with `prod` profile active verifies `Set-Cookie` header contains `Secure; SameSite=Strict`.

---

## Phase 2 — Code Cleanup & Architecture (1–2 days) 🟡

---

### 2.1 Delete deprecated `DddLayeringArchitectureTest`  `A-3`

**File to delete:** `src/test/java/com/ziyara/backend/architecture/DddLayeringArchitectureTest.java`

This test is superseded by `CleanArchitectureDddTest`, adds ~12 s to the build, and duplicates every rule already enforced.

**Acceptance criteria:** `./gradlew test` still passes after deletion. Build time decreases.

---

### 2.2 Delete orphaned `application.dto.payment` package  *(new finding)*

**Directory to delete:** `src/main/java/com/ziyara/backend/application/dto/payment/`

Contains three classes (`GatewayPaymentResponse`, `GatewayRefundResult`, `TokenizedPaymentCommand`) that have **zero usages** anywhere in the codebase. The domain layer already has its own equivalent records in `domain.payment/`. This is dead code that also confused the ArchUnit carve-out analysis.

Verify first: `grep -r "GatewayPaymentResponse\|TokenizedPaymentCommand" src/main/java --include="*.java"` — should return zero results outside the package itself.

**Acceptance criteria:** Package deleted, all tests still pass. The ArchUnit `ignoreDependency` carve-out for `domain.payment → application.dto.payment` in both arch test files can then also be removed (the ignore is now vacuous).

---

### 2.3 Delete stub `GlobalExceptionHandler` file  `D-5` `Q-4`

**File to delete:** `src/main/java/com/ziyara/backend/infrastructure/config/exception/GlobalExceptionHandler.java`

This is a comment-only stub with no class definition. It exists only to say "the real one is over there". Delete it.

**Acceptance criteria:** File is gone. No test or import references it.

---

### 2.4 Fix bare `catch (Exception e)` in non-aggregator services  `Q-3`

**Files:**

- `application/service/AuthService.java:175` — identify the specific operation (likely an external call or reflection), replace with the specific checked exception it can throw.
- `application/service/CurrencyService.java:60` — likely a REST call to an exchange-rate provider; catch `RestClientException` or `HttpClientErrorException`.

**Pattern:**
```java
// Before
try { ... } catch (Exception e) { log.error(...); }

// After — catch what can actually be thrown
try { ... } catch (RestClientException | JsonProcessingException e) { log.error(...); }
```

**Note:** `DashboardService` bare catches are intentional fail-safe fallbacks for a dashboard aggregator — leave those but add a `DashboardMetricException` wrapper (see item 5.1).

---

### 2.5 Split `RoleManagementService` into focused sub-services  `Q-2`

**File:** `application/service/RoleManagementService.java` (579 lines, 16 public methods)

The service currently owns three distinct concerns. Split along those lines:

| New Service | Responsibility | Methods |
|---|---|---|
| `RoleManagementService` (keep) | Role CRUD: create, update, delete, list | `createCustomRole`, `updateRole`, `updateRoleNavigation`, `updateRolePermissions`, `deleteRole`, `listRoles`, `getRole` |
| `GroupManagementService` (new) | Group CRUD + membership | `createGroup`, `updateGroup`, `deleteGroup`, `listGroupSummaries`, `listGroupMembers` |
| `PermissionCatalogueService` (new) | Permission catalogue reads | `getPermissionCatalogue`, `getUnlockedPermissions` |

**Steps:**
1. Create `GroupManagementService` and `PermissionCatalogueService` in `application/service/`.
2. Move methods and their dependencies (injected repos, mappers).
3. Update `RoleManagementController` to inject all three services as needed.
4. The `RoleServiceApi` module interface likely needs splitting too — check `modules/sys/api/`.
5. Update `RoleManagementServicePermissionsTest` and `RoleManagementServiceGroupCodeTest` to target the new classes.

**Acceptance criteria:** All three services are under 250 lines. All existing tests pass. `CleanArchitectureDddTest` passes.

---

## Phase 3 — API Hardening (1–2 days) 🟡

---

### 3.1 Rate limiting on write endpoints  `S-4` `API-2`

**Current state:** Rate limiting exists only in `LoginRateLimitService`. It uses Redis + PostgreSQL fallback.

**Approach:** Extend the existing rate limiter or add a Spring Filter/Interceptor that applies per-endpoint limits.

**Suggested limits:**

| Endpoint | Limit |
|---|---|
| `POST /payments` | 20 req/min per user |
| `POST /bookings` | 30 req/min per user |
| `POST /reviews` | 10 req/min per user |
| `POST /complaints` | 10 req/min per user |
| `GET /reports/**` | 5 req/min per user |

**Implementation options (in order of preference):**
1. Annotate methods with a custom `@RateLimit(max=20, window=60)` annotation processed by an AOP aspect that delegates to the existing `LoginRateLimitService` logic generalised for any key.
2. Add a `RateLimitingFilter` that reads a config map of `pattern → limit`.

**Files to create/modify:**
- `infrastructure/security/RateLimitingAspect.java` (new)
- `application/annotation/RateLimit.java` (new)
- `infrastructure/config/RateLimitConfig.java` (modify/extend)
- Controllers: add `@RateLimit` to the methods listed above.

**Acceptance criteria:** `RateLimitingAspectTest` verifies the 429 response after exceeding the limit.

---

### 3.2 Protect or remove `POST /{id}/complete` and `POST /{id}/fail`  `API-1`

**File:** `presentation/controller/PaymentController.java`

These endpoints are internal payment state-machine transitions triggered by the gateway, not by end users.

**Decision:**
- If they are called by the payment gateway webhook → remove them from `PaymentController` entirely. The `PayWebhookController` already handles gateway callbacks and is the correct entry point.
- If they are legitimately admin-only operations → protect with `@PreAuthorize("hasRole('ADMIN')")` and add an audit log entry.

After decision: delete or annotate. Do not leave them accessible to any authenticated user.

---

### 3.3 Standardise `PUT` → `PATCH` for partial updates  `API-3`

All 30 `@PutMapping` endpoints currently perform partial updates (they accept request DTOs with only the changed fields). This violates the REST semantics of `PUT` (full replacement).

**Options:**
1. Change annotations to `@PatchMapping` on endpoints where the DTO is clearly partial (most of them).
2. Keep `@PutMapping` but make all update DTOs fully replace the resource and use `@PatchMapping` for partial variants.

Option 1 is simpler. Change all `@PutMapping("/{id}")` style partial-update endpoints to `@PatchMapping("/{id}")`. Leave `@PutMapping` only on endpoints that truly replace the full resource (e.g., `PUT /admin/feature-flags`, `PUT /admin/system-settings`).

**Files to change:** All controllers listed in the analysis (section 3 PUT list — ~28 controllers).

**Acceptance criteria:** A `@WebMvcTest` for each controller verifies the correct HTTP method. Update Swagger `@Operation` summaries to say "update" not "replace" where appropriate.

---

## Phase 4 — Testing (4–6 days) 🟡

*Biggest effort block. Prioritise in order: security tests → core service tests → coverage gate.*

---

### 4.1 Security tests for PaymentController  `T-2`

**New file:** `src/test/java/com/ziyara/backend/presentation/controller/PaymentControllerWebMvcTest.java`

Minimum test cases:
- Unauthenticated `POST /payments` → `401`
- Customer role `POST /{id}/fail` → `403`
- Admin role `POST /{id}/fail` → `200`
- Customer accessing another customer's payment → `403`
- Customer accessing own payment → `200`

---

### 4.2 IDOR test suite  `T-2`

**New file:** `src/test/java/com/ziyara/backend/presentation/security/OwnershipEnforcementTest.java`

Use `@SpringBootTest` + Testcontainers. Create two users (A and B), create resources owned by A, then verify user B gets `403` on all resource endpoints. Minimum resources: payment, booking, complaint, review.

---

### 4.3 Unit tests for untested core services  `T-1`

Create one test class per service listed. Use Mockito (`@ExtendWith(MockitoExtension.class)`).

| New Test File | Priority | Key scenarios to cover |
|---|---|---|
| `AuthServiceTest.java` | **P0** | login success, login fail, account lock, MFA required, token refresh, password reset |
| `BookingServiceTest.java` | **P0** | create booking, cancel booking (use case), status transitions, IDOR rejection |
| `RoleManagementServiceTest.java` | **P1** | create role, assign permission, delete role with members check |
| `DiscountCodeServiceTest.java` | **P1** | apply discount, expiry check, max-usage enforcement |
| `PortalServiceTest.java` | **P2** | provider profile update, service image upload |
| `PaymentServiceTest.java` | extend existing | gateway failure path, refund on cancelled booking |

---

### 4.4 MFA and password policy tests  `T-2`

**New files:**
- `src/test/java/com/ziyara/backend/application/service/UserMfaServiceTest.java` — enrolment, TOTP verification, backup code redemption, backup code invalidation after use.
- Extend `PasswordPolicyServiceTest.java` to cover API-level rejection (via `@WebMvcTest` on `AuthController` with `POST /auth/register` using a weak password → `400 BAD_REQUEST`).

---

### 4.5 JaCoCo coverage gate  `T-4`

**File:** `build.gradle.kts`

Add after the `jacocoTestReport` task:
```kotlin
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("com.ziyara.backend.application.service.*")
            limit {
                counter = "LINE"
                value   = "COVEREDRATIO"
                minimum = "0.60".toBigDecimal()
            }
        }
    }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
```

**Acceptance criteria:** `./gradlew check` fails if `application/service/` drops below 60 % line coverage.

---

### 4.6 N+1 query detection tests  `T-3`

**File:** `src/test/java/com/ziyara/backend/infrastructure/persistence/QueryCountTest.java`

Use `datasource-proxy` or Hypersistence Optimizer to assert maximum query counts per operation:
```java
// Example
assertThat(queryCount).isLessThanOrEqualTo(3); // list bookings should not exceed 3 queries
```

Key operations to cover: `listBookings`, `getUserWithRoles`, `getPaymentsPage`.

---

### 4.7 Rate limiting tests  `T-2`

**New file:** `src/test/java/com/ziyara/backend/infrastructure/security/RateLimitingAspectTest.java`

Mock the `RateLimitService.isAllowed()` to return `false`, call the endpoint, verify `429 TOO_MANY_REQUESTS` with `Retry-After` header.

---

## Phase 5 — Documentation (2–3 days) 🟡

---

### 5.1 Create `README.md`  `D-1`

**File:** `README.md` (project root — `core/`)

Sections to include:
1. **What is Ziyara** — one paragraph, what the system does
2. **Architecture overview** — 4-layer diagram (domain → application → infrastructure ← presentation), module API pattern
3. **Prerequisites** — Java 21, PostgreSQL 15+, Redis (optional), Kafka (optional)
4. **Quick start** — env vars to set, how to run with `./gradlew bootRun`, how to run tests
5. **Environment variables reference** — table of every `ZIYARA_*` / `JWT_*` / `APP_*` variable, required/optional, default
6. **Running with Docker** — `docker build`, Docker Compose section (once 5.4 done)
7. **Running tests** — `./gradlew test`, what Testcontainers needs, how to run arch tests alone
8. **API documentation** — link to Swagger UI at `http://localhost:8080/swagger-ui.html`

---

### 5.2 Create `CLAUDE.md`  `D-2`

**File:** `CLAUDE.md` (project root)

Sections:
1. **Layer rules** — the 4 layers, what belongs where, the port-adapter pattern
2. **How to add a new feature** — step-by-step: domain entity → use case → repository interface → adapter → application service → controller → test
3. **Module API pattern** — how cross-module communication works via `modules/*/api/` interfaces
4. **Accepted technical debt** — copy the 12 carve-outs from `CleanArchitectureDddTest.java` with their reasons
5. **No JPA relationships policy** — document that all joins are explicit in jOOQ / JPQL, and why (N+1 prevention)
6. **Naming conventions** — `*UseCase`, `*RepositoryAdapter`, `*JpaEntity`, `*ServiceApi`
7. **Build commands** — test, build, arch-test, docker

---

### 5.3 Create `SCHEMA.md`  `D-3`

**File:** `docs/SCHEMA.md`

Auto-generate a table from the Flyway migrations listing all tables, their columns, indexes, and foreign key relationships. Key tables to describe in narrative form:
- `users`, `user_roles`, `user_sessions`, `user_password_history`
- `bookings`, `payments`, `refunds`
- `service_providers`, `services`, `service_images`
- `sys_audit_logs`, `sys_rate_limit_counters`, `sys_notifications`

---

### 5.4 Create `docker-compose.yml`  `I-5`

**File:** `docker-compose.yml` (project root)

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ziyarah
      POSTGRES_USER: ziyarah
      POSTGRES_PASSWORD: ziyarah
    ports: ["5432:5432"]
    volumes: [pgdata:/var/lib/postgresql/data]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  mail:
    image: mailhog/mailhog
    ports: ["1025:1025", "8025:8025"]

volumes:
  pgdata:
```

Add a `.env.example` file listing every required environment variable with placeholder values.

---

## Phase 6 — Infrastructure Polish (1 day) 🟢

---

### 6.1 Update Java version to 21  `I-1`

**File:** `build.gradle.kts`
```kotlin
// Change
sourceCompatibility = JavaVersion.VERSION_17
targetCompatibility = JavaVersion.VERSION_17
// To
sourceCompatibility = JavaVersion.VERSION_21
targetCompatibility = JavaVersion.VERSION_21
```

**File:** `Dockerfile`
```dockerfile
# Change
FROM eclipse-temurin:17-jdk-alpine AS builder
FROM eclipse-temurin:17-jre-alpine
# To
FROM eclipse-temurin:21-jdk-alpine AS builder
FROM eclipse-temurin:21-jre-alpine
```

**Acceptance criteria:** `./gradlew build` succeeds. Docker image builds and the health check passes.

---

### 6.2 Tune Docker HEALTHCHECK  `I-3`

**File:** `Dockerfile`

```dockerfile
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
```

`start-period=60s` gives Flyway time to run all migrations before the health check kicks in.

---

### 6.3 DashboardService — typed exception hierarchy  `Q-1`

**File:** `application/service/DashboardService.java`

Create `application/exception/DashboardMetricException.java`:
```java
public class DashboardMetricException extends RuntimeException {
    public final String metricName;
    public DashboardMetricException(String metricName, Throwable cause) {
        super("Failed to compute dashboard metric: " + metricName, cause);
        this.metricName = metricName;
    }
}
```

Replace each `catch (Exception e)` block:
```java
// Before
try { result = computeX(); } catch (Exception e) { log.warn(...); return fallback; }

// After
try { result = computeX(); } catch (Exception e) {
    log.warn("Dashboard metric '{}' failed: {}", "X", e.getMessage());
    throw new DashboardMetricException("X", e);  // let aggregator handle fallback
}
```

Update the aggregator method to catch `DashboardMetricException` and substitute the fallback — keeping fail-safe behaviour but with a traceable type.

---

### 6.4 Add `Retry-After` header to rate-limit responses  `S-6`

**File:** `presentation/exception/GlobalExceptionHandler.java`

The existing handler for `RateLimitedException` should include the header:
```java
@ExceptionHandler(RateLimitedException.class)
public ResponseEntity<ApiResponse<?>> handleRateLimited(RateLimitedException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .body(ApiResponse.error("TOO_MANY_REQUESTS", ex.getMessage()));
}
```

This also enables client-side back-off.

---

### 6.5 Document data retention / GDPR steps  `I-4`

**File:** `docs/GDPR.md`

Minimum content:
- What personal data is stored (users PII, booking details, IP addresses in audit logs)
- Retention periods per category
- How `ZIYARA_DATA_RETENTION_ENABLED` / cron works
- Right to erasure: which tables are cleared on user deletion
- Where to find the retention cron config and how to test it

---

## Phase 7 — Final ArchUnit Update (0.5 days) 🟢

After completing phases 1–6, two ArchUnit carve-outs become vacuous and should be removed:

1. **Remove the `domain.payment → application.dto.payment` ignore** from `CleanArchitectureDddTest.java` (and `DddLayeringArchitectureTest.java` if it hasn't been deleted yet). Once `application/dto/payment/` is deleted (item 2.2), the ignore is dead code that could mask a future real violation.

2. **Run `CleanArchitectureDddTest` after every phase** as a regression check:
   ```
   ./gradlew test --tests "com.ziyara.backend.architecture.CleanArchitectureDddTest"
   ```

---

## Summary Checklist

### Phase 1 — Security Critical
- [ ] 1.1 `@PreAuthorize` on all `PaymentController` endpoints
- [ ] 1.2 `@PreAuthorize` on `PricingController`
- [ ] 1.3 IDOR ownership checks on all `@PathVariable UUID` endpoints
- [ ] 1.4 PII key enforcement in prod profile startup
- [ ] 1.5 JWT cookie `Secure + SameSite=Strict` in prod profile

### Phase 2 — Code Cleanup
- [ ] 2.1 Delete `DddLayeringArchitectureTest.java`
- [ ] 2.2 Delete `application/dto/payment/` dead package
- [ ] 2.3 Delete stub `infrastructure/config/exception/GlobalExceptionHandler.java`
- [ ] 2.4 Fix bare `catch (Exception)` in `AuthService` and `CurrencyService`
- [ ] 2.5 Split `RoleManagementService` → `GroupManagementService` + `PermissionCatalogueService`

### Phase 3 — API Hardening
- [ ] 3.1 Rate limiting on write endpoints (`@RateLimit` AOP)
- [ ] 3.2 Protect or remove `POST /{id}/complete` and `POST /{id}/fail`
- [ ] 3.3 `@PutMapping` → `@PatchMapping` for partial updates (28 controllers)

### Phase 4 — Testing
- [ ] 4.1 `PaymentControllerWebMvcTest` (role + IDOR scenarios)
- [ ] 4.2 `OwnershipEnforcementTest` (cross-user access = 403)
- [ ] 4.3 Unit tests: `AuthService`, `BookingService`, `RoleManagementService`, `DiscountCodeService`, `PortalService`
- [ ] 4.4 MFA and password-policy API tests
- [ ] 4.5 JaCoCo 60 % gate on `application/service/`
- [ ] 4.6 N+1 query count tests
- [ ] 4.7 Rate limiting aspect test

### Phase 5 — Documentation
- [ ] 5.1 `README.md`
- [ ] 5.2 `CLAUDE.md`
- [ ] 5.3 `docs/SCHEMA.md`
- [ ] 5.4 `docker-compose.yml` + `.env.example`

### Phase 6 — Infrastructure Polish
- [ ] 6.1 Java 21 in `build.gradle.kts` + Dockerfile
- [ ] 6.2 Docker `HEALTHCHECK` `--start-period=60s`
- [ ] 6.3 `DashboardMetricException` hierarchy
- [ ] 6.4 `Retry-After` header in rate-limit responses
- [ ] 6.5 `docs/GDPR.md`

### Phase 7 — ArchUnit Cleanup
- [ ] 7.1 Remove vacuous `domain.payment → application.dto.payment` ignore from arch tests

---

## Effort estimate

| Phase | Items | Est. Days |
|---|---|---|
| 1 — Security Critical | 5 | 2–3 |
| 2 — Code Cleanup | 5 | 1–2 |
| 3 — API Hardening | 3 | 1–2 |
| 4 — Testing | 7 | 4–6 |
| 5 — Documentation | 4 | 2–3 |
| 6 — Infrastructure Polish | 5 | 1 |
| 7 — ArchUnit Cleanup | 1 | 0.5 |
| **Total** | **30** | **~12–18 days** |

Testing (phase 4) is the largest block. It can be done in parallel with phases 5–6 by a second developer.

After completing all 30 items the projected scores are:

| Area | Current | Target |
|---|---|---|
| Architecture & DDD | 8.5 | 10.0 |
| Security | 7.5 | 10.0 |
| API Design | 7.0 | 9.5 |
| Testing | 5.5 | 9.5 |
| Documentation | 4.0 | 10.0 |
| Infrastructure | 8.0 | 10.0 |
| Code Quality | 7.5 | 10.0 |
| **Overall** | **6.9** | **~9.9** |

> API Design and Testing land at 9.5 rather than 10 because some items (full `PATCH` semantics migration across 28 controllers, 100 % service test coverage) have diminishing returns and could push into 10 with additional polish beyond what is scoped here.
