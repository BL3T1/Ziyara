# Ziyara Backend — Pre-Launch Analysis Report

> **Branch:** V1 | **Analysed:** 2026-05-25 | **Stage:** Pre-launch / stabilisation
>
> Coverage: Architecture · Security · API Design · Testing · Documentation · Infrastructure · Code Quality

---

## Executive Summary

| Area | Score | Headline |
|---|---|---|
| Architecture & DDD | **8.5 / 10** | Excellent discipline; one known port coupling |
| Security | **7.5 / 10** | Solid core; IDOR audit and PII enforcement needed |
| API Design | **7.0 / 10** | Correct semantics; missing per-resource rate limiting |
| Testing | **5.5 / 10** | ArchUnit is outstanding; service unit-test coverage is thin |
| Documentation | **4.0 / 10** | Swagger complete; no README, no developer guide |
| Infrastructure | **8.0 / 10** | Production-ready; Java version mismatch to fix |
| Code Quality | **7.5 / 10** | Clean conventions; a few God-class and bare-catch risks |
| **Overall** | **6.9 / 10** | **Ready with required action items — ~75 % of the way there** |

---

## 1. Architecture & DDD — 8.5 / 10

### Codebase snapshot

| Artefact | Count |
|---|---|
| REST Controllers | 37 |
| Application Services | 55 |
| Domain Entities (pure Java) | 45 |
| Domain Repository Interfaces | 47 |
| Domain Use Cases | 35 |
| JPA Entities | 48 |
| Repository Adapters | 47 |
| Module API Interfaces | 10 |
| Flyway Migrations | 26 |

### What is done well

**Clean Architecture boundaries are real, not cosmetic.**
Every domain entity is pure Java — zero `import org.springframework.*` or `import jakarta.*` found across `domain/entity/`. Services receive domain repository interfaces (ports), never JPA repos. The 47 `*RepositoryAdapter.java` classes are the only code that touches JPA. ArchUnit's 10 rule groups (984 lines, `CleanArchitectureDddTest.java`) enforce this at build time and fail the build on any violation.

**Domain Use Cases are properly modelled.**
35 use cases live under `domain/usecase/`, covering booking, complaint, discount, notification, payment, provider, refund, review, service, subscription, taxi, and user. Each has a single `execute()` method. They are not Spring beans — they are instantiated and orchestrated by application services.

**Module API pattern prevents cross-service coupling.**
`modules/{booking,notification,payment,pricing,service,subscription,sys}/api/` defines interface contracts (10 files). Application services that span modules depend on those interfaces, not on sibling service implementations. `ModuleBoundariesArchTest.java` enforces the boundaries.

**Entities carry behaviour.**
`Booking.java` has 84 public methods; `AuditLog.java` has 41. Domain objects are not anemic data holders — business rules live inside them.

**No layer violations in production code.**
All 6 ArchUnit tests pass. The 12 documented carve-outs (security, config, web helpers, messaging, media, persistence utilities, payment gateway) are each justified in a comment and tracked as accepted technical debt.

### Issues

| # | Severity | Description |
|---|---|---|
| A-1 | Medium | `domain.payment.PaymentProvider` (a hexagonal outbound port) directly references `application.dto.payment.*` DTOs. This inverts the dependency direction. The port should define its own domain-level command/response value objects. Acknowledged in ArchUnit but not yet fixed. |
| A-2 | Low | The `modules/` package sits alongside `application/`, `domain/`, `infrastructure/`, and `presentation/`. It is not one of the four canonical layers so ArchUnit's layered-architecture rule ignores it. If modules grow, add an explicit layer or move their API interfaces into `domain/port/`. |
| A-3 | Low | `DddLayeringArchitectureTest.java` is marked `@deprecated` and superseded by `CleanArchitectureDddTest`. It adds 12 s to the build for zero additional coverage. Delete it. |

---

## 2. Security — 7.5 / 10

### What is done well

- **Authentication:** Stateless JWT with HMAC-SHA256. Secret is mandatory (`JWT_SECRET`) and validated for minimum 32 bytes at startup. Refresh tokens tracked in DB.
- **Passwords:** BCrypt, configurable policy (min length, upper/lower/digit/special, optional zxcvbn score), history tracking via `UserPasswordHistoryJpaEntity`, rotation invalidates JWTs via `tokenVersion`.
- **Account protection:** 5 failed attempts → 30-minute lock (`failedLoginAttempts`). Login rate limiting: 40 req/min per IP, Redis-backed with PostgreSQL fallback.
- **MFA:** Full TOTP enrolment, backup codes, per-role enforcement via `ZIYARA_SECURITY_MFA_REQUIRED_ROLES`. Secrets encrypted via `PiiCryptoService`.
- **CSRF:** `CookieCsrfTokenRepository.withHttpOnlyFalse()` with correct exemption for Bearer JWT requests. Covered by `CsrfProtectionTest.java`.
- **CORS:** Origins from `ZIYARA_CORS_ALLOWED_ORIGINS`; defaults to deny-all. `ZIYARA_CORS_ALLOW_ALL` defaults to `false`.
- **Global auth net:** `SecurityConfig` ends with `.anyRequest().authenticated()` so every unspecified endpoint requires a valid JWT — no accidental open endpoints.
- **Error messages:** "Invalid email or password" for auth failures — no information leakage.
- **Webhook:** `PayWebhookController` validates signature via `PAYMENT_WEBHOOK_SECRET`.

### Issues

| # | Severity | Description |
|---|---|---|
| S-1 | **High** | **5 controllers carry zero `@PreAuthorize` annotations:** `AuthController`, `PaymentController`, `PayWebhookController`, `PricingController`, `PublicContactController`. `AuthController` and `PayWebhookController` are intentionally public. But `PaymentController` (8 endpoints covering `POST /payments`, `POST /{id}/complete`, `POST /{id}/fail`, `GET /payments`, etc.) and `PricingController` rely entirely on `SecurityConfig`'s `.anyRequest().authenticated()` for access control. There is **no role or ownership check** — any authenticated user can call `failPayment` or `completePayment` on any payment ID. Add `@PreAuthorize` on every sensitive method. |
| S-2 | **High** | **IDOR risk across resource endpoints.** Controllers that accept a `UUID id` path variable (PaymentController, BookingController, and others) do not uniformly verify that the resource belongs to the requesting user. Ownership must be enforced in the application service or via Spring Security's domain object security, not left to the caller. Audit every `@PathVariable UUID` endpoint. |
| S-3 | Medium | `ZIYARA_PII_ENCRYPTION_KEY_BASE64` is optional. When absent, TOTP secrets and backup codes are stored in plaintext. This must be a required startup assertion for any production deployment. |
| S-4 | Medium | Rate limiting covers login only. There is no rate limiting on `POST /bookings`, `POST /payments`, `POST /reviews`, or report-export endpoints. A malicious authenticated user can enumerate or spam these freely. |
| S-5 | Low | `JWT_COOKIE_SECURE` defaults to `false` and `JWT_COOKIE_SAME_SITE` defaults to `Lax`. Ensure production overrides both (`SECURE=true`, `SAME_SITE=Strict`) and document this in the deployment guide. |
| S-6 | Low | `LoginRateLimitService` fails open (`allowLogin()` returns `true`) when the DB/Redis is unavailable. This is acceptable for availability, but the warn log should trigger an alert in production monitoring. |

---

## 3. API Design — 7.0 / 10

### What is done well

- **HTTP semantics are correct.** `POST` returns `201 CREATED`, `DELETE` returns `204 NO CONTENT`, `GET` returns `200 OK`. Verified across all 37 controllers.
- **Consistent response envelope.** All responses use `ApiResponse<T>` containing `success`, `message`, `data`, `code`, and `correlationId`. Errors follow the same shape.
- **OpenAPI documentation.** All 37 controllers have `@Tag` and `@Operation` annotations. Swagger UI is available at `/swagger-ui.html`. `ApiDocsEndpointTest.java` and `OpenApiEndpointSmokeTest.java` verify the spec is reachable.
- **API versioning.** All routes are under `/api/v1` via `server.servlet.context-path`. A future `/api/v2` can be introduced without breaking existing clients.
- **Pagination.** `page` / `size` parameters on list endpoints, backed by Spring's `Page<T>` abstraction and domain `PagedResult` via `PageConverter`.
- **Public vs authenticated.** Public endpoints (`/auth/**`, `/public/**`, `/services` GET, `/media/**`) are explicitly listed in `SecurityConfig` — no reliance on convention.

### Issues

| # | Severity | Description |
|---|---|---|
| API-1 | Medium | `PaymentController` exposes `POST /{id}/complete` and `POST /{id}/fail`. These look like internal state-machine transitions, not client-facing actions. If they are only meant to be called by the payment gateway callback flow, they should be removed from the public API surface or protected by an internal role. |
| API-2 | Medium | No rate limiting on write endpoints (see S-4). A payment can be initiated in a tight loop. |
| API-3 | Low | `PATCH` is used in some controllers but not consistently. Some update operations still use `PUT` for partial updates. Standardise: `PUT` for full replacement, `PATCH` for partial update. |
| API-4 | Low | No API deprecation or sunset headers on older endpoints. As features evolve, add `Deprecation:` and `Sunset:` headers to guide clients. |
| API-5 | Low | No Postman collection or example-request documentation beyond Swagger schemas. Swagger shows structure but not realistic workflows. |

---

## 4. Testing — 5.5 / 10

### Test inventory

| Type | Count |
|---|---|
| Architecture tests (ArchUnit) | 4 |
| WebMVC slice tests (`@WebMvcTest`) | 10 |
| Application service unit tests | 11 |
| Integration tests (`@SpringBootTest`) | 17 |
| Kafka / Docker tests | 2 |
| Domain / infrastructure unit tests | 6 |
| **Total** | **37** (+ 3 arch = **37 total**) |

### What is done well

**ArchUnit is the strongest testing asset in the project.** `CleanArchitectureDddTest.java` (984 lines, 10 rule groups) enforces naming conventions, layer boundaries, DDD patterns, module contracts, and cycle-freedom at compile time. This prevents architectural rot better than any manual code review.

**Integration tests use real infrastructure.** `UserCreateAndLoginFunctionalTest` and `EndpointFunctionalTest` spin up a real Spring context with Testcontainers (PostgreSQL). Kafka integration tests (`StaffNotificationKafkaBrokerDockerTest`) use a real broker container.

**WebMVC slice tests cover controller security correctly.** `AdminFeatureFlagsControllerWebMvcTest`, `PortalStaffControllerWebMvcTest`, `RoleManagementControllerWebMvcTest` etc. test role enforcement without a full context.

### Issues

| # | Severity | Description |
|---|---|---|
| T-1 | **High** | **~80 % of application services have no unit test.** 55 services exist; only 11 have a dedicated test class. `AuthService` (394 lines), `BookingService` (351 lines), `RoleManagementService` (579 lines), `PortalService` (442 lines), and `DiscountCodeService` (316 lines) are all untested. Any regression in these flows will not be caught until a functional test or production incident. |
| T-2 | High | **No security-focused tests for the gaps identified in section 2.** There are no tests for: IDOR (ownership checks), role enforcement on `PaymentController`, MFA enrolment / verification, password policy validation at the API level, or rate limiting. |
| T-3 | Medium | No performance / query tests. N+1 queries from un-joined repository calls cannot be detected until load testing. Consider Hypersistence Optimizer or `@DataJpaTest` with query count assertions. |
| T-4 | Medium | JaCoCo is configured but no minimum coverage threshold is enforced. The build will pass at 0 % coverage. Add a `jacocoTestCoverageVerification` rule (e.g., minimum 60 % line coverage on `application/service/`). |
| T-5 | Low | `DashboardServiceTest.java` exists but `DashboardService.java` contains 8 bare `catch (Exception e)` blocks (see Code Quality section). The tests do not exercise those error paths. |

---

## 5. Documentation — 4.0 / 10

### What is done well

- **Swagger / OpenAPI is comprehensive.** All 37 controllers carry `@Tag`, `@Operation`, and `@Schema` annotations. The bearer JWT security scheme is declared in `OpenApiConfig`. Two smoke tests verify the spec is reachable on startup.
- **Configuration is self-documenting.** `application.yml` (238 lines) uses descriptive environment variable names (`ZIYARA_PASSWORD_MIN_LENGTH`, `ZIYARA_CORS_ALLOWED_ORIGINS`). Defaults are present where safe, absent where the value is secrets.
- **Architecture tests double as documentation.** The 12 carve-out comments in `CleanArchitectureDddTest.java` describe the accepted design trade-offs in detail.

### Issues

| # | Severity | Description |
|---|---|---|
| D-1 | **Critical** | **No README.** There is no `README.md` in the project root. A new developer joining the team has no starting point: how to run the project, what environment variables to set, how to run tests, what the architecture is. This must exist before launch. |
| D-2 | High | **No CLAUDE.md / developer guide.** There is no document describing architectural decisions, how to add a new module, naming conventions, or common pitfalls. As the team grows this knowledge lives only in people's heads. |
| D-3 | Medium | **No database schema documentation.** 26 Flyway migrations exist with no accompanying ER diagram or `SCHEMA.md`. Understanding the data model requires reading all migrations in sequence. |
| D-4 | Low | **No Postman collection or integration example workflows.** Swagger shows the schema; it does not show a realistic multi-step workflow (register → login → book → pay). |
| D-5 | Low | **Stub file in wrong place.** `infrastructure/config/exception/GlobalExceptionHandler.java` exists as a comment-only stub pointing to the real class in `presentation/exception/`. This is confusing noise. Delete it. |

---

## 6. Infrastructure — 8.0 / 10

### What is done well

- **Flyway** manages schema (26 migrations, `validate-on-migrate: true`, `ddl-auto: none`). Safe for zero-downtime deployments.
- **HikariCP** pool: 20 max, 5 min idle. Batch inserts enabled (`batch_size: 25`). `open-in-view: false`.
- **Redis caching** (optional) with 1-hour TTL on `staffRoleCatalog` and `permissionCatalogue`, `transactionAware()` for correct cache invalidation on rollback. Falls back to in-memory when Redis is absent.
- **Kafka** (optional) for staff notifications with DLQ (`ziyara.notifications.staff.dlq`). Disabled by default.
- **Multi-stage Docker image:** Gradle build stage → Eclipse Temurin 17 JRE Alpine runtime. Non-root user `ziyarah:ziyarah`. Health check via `/actuator/health`. JVM tuning `-Xms256m -Xmx512m`.
- **Multi-profile:** `application.yml` + `dev`, `prod`, `docker`, `test` profiles.
- **Secrets management:** `JWT_SECRET`, `DB_PASSWORD`, `ZIYARA_PII_ENCRYPTION_KEY_BASE64` are all environment-variable-driven with no hardcoded defaults.
- **Observability:** Micrometer + OTLP export (optional), Prometheus metrics at `/actuator/metrics`, 10 % trace sampling (configurable).

### Issues

| # | Severity | Description |
|---|---|---|
| I-1 | **High** | **Java version mismatch.** `build.gradle.kts` declares `sourceCompatibility = JavaVersion.VERSION_17` and the Dockerfile uses `eclipse-temurin:17-jre-alpine`. If the project is developed and run on Java 21, the bytecode target is 17 but the JDK compiling it is 21. This is currently harmless (17 bytecode runs on 21), but it means Java 21 language features (`record patterns`, `virtual threads`, `sequenced collections`) cannot be used. Update to `VERSION_21` and `eclipse-temurin:21-jre-alpine` to align build, Docker, and runtime. |
| I-2 | Medium | `ZIYARA_PII_ENCRYPTION_KEY_BASE64` is optional (no startup failure when absent). MFA secrets are then stored in plaintext. Add a `@PostConstruct` assertion in `PiiCryptoService` that throws `IllegalStateException` when the key is absent and `spring.profiles.active` contains `prod`. |
| I-3 | Medium | The Docker image has no explicit `HEALTHCHECK` interval or failure threshold tuning — defaults (`30s` interval, 3 retries) may cause premature container restarts during slow cold starts with large schema migrations. |
| I-4 | Low | Data retention (`ZIYARA_DATA_RETENTION_ENABLED`) is disabled by default. GDPR compliance requires a documented retention policy and a tested retention run before launch. |
| I-5 | Low | No `docker-compose.yml` for local development is present in the repository root. Developers must manually provision PostgreSQL and Redis. |

---

## 7. Code Quality — 7.5 / 10

### What is done well

- **No TODO / FIXME comments** anywhere in `src/main/java/`. Zero results.
- **Constructor injection throughout.** All services use `@RequiredArgsConstructor` with `final` fields. Zero `@Autowired` on fields.
- **`@Transactional` coverage is complete.** 232 `@Transactional` usages across 55 services. Every service that modifies state has at least one.
- **No web-layer leakage into application services.** Zero occurrences of `ResponseEntity`, `HttpServletRequest`, `@RequestBody`, or `@PathVariable` in `application/service/`.
- **No SQL injection vectors.** All repository queries use named parameters (`:paramName`). jOOQ is used for complex reporting. Zero `String.format()` or string concatenation in query construction.
- **Consistent patterns.** `Optional<T>` for nullable returns, Lombok `@Builder` for DTOs, domain enums instead of magic strings.

### Issues

| # | Severity | Description |
|---|---|---|
| Q-1 | Medium | **`DashboardService.java` swallows exceptions broadly.** 8 occurrences of `catch (Exception e)` in a single file (lines 85, 97, 106, 118, 130, 155, 188 and one more). The pattern logs a warning and returns a partial/fallback result. While defensible for a dashboard aggregator, it hides bugs during development. Replace with typed catches or an explicit `DashboardMetricException` hierarchy. |
| Q-2 | Medium | **God-class risk.** The five largest services by line count: `RoleManagementService` (579), `PortalService` (442), `AuthService` (394), `ServiceProviderService` (359), `BookingService` (351). `RoleManagementService` in particular likely handles permission assignment, group management, role hierarchy, and audit — all mixed. Consider splitting along bounded-context boundaries. |
| Q-3 | Low | `AuthService.java:175` and `CurrencyService.java:60` both have bare `catch (Exception e)` blocks that are not dashboard-aggregation patterns. These should catch specific exceptions. |
| Q-4 | Low | **Stub file is dead code.** `infrastructure/config/exception/GlobalExceptionHandler.java` contains only a comment. Delete it to avoid confusing anyone who lands on it. |
| Q-5 | Low | JPA entities have no relationship annotations (`@OneToMany`, `@ManyToOne`) across all 48 entities. All joins are done in jOOQ / JPQL explicitly. This is intentional and avoids N+1 queries — but it should be documented as a deliberate architectural choice to prevent future developers from adding JPA relationships. |

---

## 8. Priority Action List

### Must-do before launch

| Priority | Item | Section |
|---|---|---|
| 🔴 1 | Add `@PreAuthorize` to all `PaymentController` and `PricingController` endpoints — remove reliance on `.anyRequest().authenticated()` alone | S-1 |
| 🔴 2 | Audit every `@PathVariable UUID id` endpoint for ownership checks (IDOR) | S-2 |
| 🔴 3 | Write a `README.md` covering: architecture overview, prerequisites, env vars, running tests, Docker setup | D-1 |
| 🔴 4 | Enforce `ZIYARA_PII_ENCRYPTION_KEY_BASE64` as a required startup property in the `prod` profile | S-3, I-2 |
| 🔴 5 | Align Java version: set `sourceCompatibility = JavaVersion.VERSION_21` in `build.gradle.kts` and update the Dockerfile to `eclipse-temurin:21-jre-alpine` | I-1 |

### Should-do before launch

| Priority | Item | Section |
|---|---|---|
| 🟡 6 | Add unit tests for `AuthService`, `BookingService`, `RoleManagementService` | T-1 |
| 🟡 7 | Add rate limiting to `POST /payments`, `POST /bookings`, `POST /reviews` | S-4, API-2 |
| 🟡 8 | Set `JWT_COOKIE_SECURE=true` and `JWT_COOKIE_SAME_SITE=Strict` in the production profile | S-5 |
| 🟡 9 | Add a JaCoCo minimum coverage rule (recommended: 60 % line coverage on `application/service/`) | T-4 |
| 🟡 10 | Delete `DddLayeringArchitectureTest.java` (deprecated, superseded) and the stub `infrastructure/config/exception/GlobalExceptionHandler.java` | A-3, D-5 |
| 🟡 11 | Create a `docker-compose.yml` for local development (PostgreSQL + Redis + MailHog) | I-5 |
| 🟡 12 | Create `CLAUDE.md` / developer guide: how to add a module, naming rules, use-case pattern, accepted debt list | D-2 |

### Nice-to-have

| Priority | Item | Section |
|---|---|---|
| 🟢 13 | Refactor `DashboardService` bare exception catches to typed exceptions | Q-1 |
| 🟢 14 | Split `RoleManagementService` (579 lines) into focused sub-services | Q-2 |
| 🟢 15 | Move `domain.payment.PaymentProvider` method signatures to domain-level value objects instead of application DTOs | A-1 |
| 🟢 16 | Add ER diagram or `SCHEMA.md` documenting the 26-migration database schema | D-3 |
| 🟢 17 | Add a Postman collection or Swagger examples for multi-step workflows (register → login → book → pay) | API-5, D-4 |
| 🟢 18 | Add N+1 detection tests using `@DataJpaTest` with query count assertions | T-3 |
| 🟢 19 | Document the no-JPA-relationships architectural decision in `CLAUDE.md` | Q-5 |
| 🟢 20 | Enforce data retention policy and document GDPR compliance steps | I-4 |

---

## 9. Final Verdict

The Ziyara backend is **structurally mature**. The architecture is genuinely clean — not just labelled clean. Domain entities carry real behaviour, ports and adapters are properly separated, and ArchUnit makes violations impossible to merge. The security foundation (JWT, BCrypt, CSRF, lockout, MFA, rate-limited login) is well above average for a pre-launch backend.

The two gaps that need resolving before going live are **access-control completeness** (IDOR checks, `@PreAuthorize` on all payment/pricing endpoints) and **documentation** (a README alone would unblock onboarding). Everything else is technical debt that can be tracked and addressed in the first month post-launch.

**Readiness estimate: 75 %**
With items 1–5 from the Must-do list resolved: **90 %**.
