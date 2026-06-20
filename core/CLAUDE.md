# Ziyara Backend — Developer Guide

This file is the authoritative reference for architecture conventions. Read it before adding new code.

---

## Layer Rules

The codebase enforces **Clean Architecture** with ArchUnit (56 rules). Violations fail the test suite.

| Layer | Package | Rule |
|---|---|---|
| **Domain** | `domain.*` | No Spring, no JPA, no framework imports. Pure Java only. |
| **Application** | `application.*` | Spring `@Service`, `@Transactional`. No JPA entities, no HTTP types, no `infrastructure.*` imports. |
| **Infrastructure** | `infrastructure.*` | Spring `@Repository`, `@Component`, JPA entities, security, AOP. Implements domain repository interfaces. |
| **Presentation** | `presentation.*` | Spring `@RestController`, DTOs as `@RequestBody`/`@ResponseBody`. May import `application.*` and `infrastructure.security.*` (for `@PreAuthorize` expressions). |

**Dependency direction:** `presentation → application → domain ← infrastructure`

Infrastructure adapts to domain — domain never imports infrastructure.

---

## How to Add a New Feature

1. **Domain entity** — `domain/entity/MyEntity.java` — plain Java, no annotations
2. **Repository interface** — `domain/repository/MyEntityRepository.java` — plain Java interface
3. **Use case** — `application/service/MyService.java` — `@Service @Transactional`, depends on domain interfaces
4. **JPA entity** — `infrastructure/persistence/entity/MyEntityJpa.java` — `@Entity`, mirrors domain entity
5. **Repository adapter** — `infrastructure/persistence/adapter/MyEntityRepositoryAdapter.java` — implements `MyEntityRepository`, wraps `MyEntityJpaRepository`
6. **Controller** — `presentation/controller/MyController.java` — `@RestController`, calls service, `@PreAuthorize` on every method
7. **Test** — `application/service/MyServiceTest.java` (Mockito unit test) + optional `presentation/controller/MyControllerWebMvcTest.java`

---

## Module API Pattern

Cross-module communication uses interface boundaries under `modules/*/api/`. Other modules must never import a module's service or repository directly.

```
modules/
  payment/api/PaymentServiceApi.java      ← interface only
  booking/api/BookingServiceApi.java
  sys/api/AuditServiceApi.java
  sys/api/RoleServiceApi.java
  portal/api/PortalServiceApi.java
```

When module A needs to call module B, it injects `BServiceApi`, not `BService`. The implementation is wired by Spring — the API interface is the contract.

---

## Accepted Technical Debt (ArchUnit Carve-Outs)

The following `ignoreDependency` carve-outs exist in `CleanArchitectureDddTest.java`. They are tracked explicitly so future changes don't silently re-introduce layer violations.

| Carve-out | Reason |
|---|---|
| `presentation.controller → infrastructure.security` | `@PreAuthorize` SpEL references `ApiAuthorizationExpressions` constants — unavoidable without a separate constants module |
| `presentation.controller → infrastructure.web` | `AuthCookieHelper` used in `AuthController` for cookie management |
| `presentation.exception → application.service.AuthService` | `GlobalExceptionHandler` catches `AuthService.AuthenticationException` (inner class) |
| `application.service → infrastructure.security.crypto` | `AuthService` uses `PiiCryptoService` and `TotpService` for MFA — crypto belongs in infrastructure |
| `application.service → infrastructure.security.JwtService` | `AuthService` generates tokens directly — acceptable for an auth service |
| _(see test for full list)_ | |

---

## No JPA Relationships Policy

**Do not add `@OneToMany`, `@ManyToOne`, or `@JoinColumn` to JPA entities.** All multi-table fetches use explicit jOOQ queries or JPQL with `JOIN FETCH`. This eliminates N+1 query risks.

---

## Naming Conventions

| Type | Suffix | Example |
|---|---|---|
| Domain entity | _(none)_ | `User`, `Booking` |
| JPA entity | `Jpa` | `UserJpa`, `BookingJpa` |
| Repository interface (domain) | `Repository` | `UserRepository` |
| Repository adapter (infra) | `RepositoryAdapter` | `UserRepositoryAdapter` |
| Spring Data JPA repo | `JpaRepository` | `UserJpaRepository` |
| Application service | `Service` | `AuthService`, `BookingService` |
| Module API interface | `ServiceApi` | `PaymentServiceApi` |
| ArchUnit test | `ArchTest` | `CleanArchitectureDddTest` |
| Use case (single-method class) | `UseCase` | `CreateBookingUseCase` |

---

## Build Commands

```bash
./gradlew test                        # all unit + arch tests
./gradlew test -PrunDockerTests        # + Testcontainers (requires Docker)
./gradlew check                        # test + JaCoCo 60% gate
./gradlew jacocoTestReport             # HTML coverage report → build/reports/jacoco/
./gradlew bootRun                      # run locally (requires Postgres + Redis)
docker compose up --build              # full stack (Postgres + Redis + MailHog + app)

# Architecture test only (fast):
./gradlew test --tests "com.ziyara.backend.architecture.CleanArchitectureDddTest"
```
