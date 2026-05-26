# Ziyara Backend

Ziyara is a multi-tenant hospitality and travel-services platform. This module is the Spring Boot backend (REST API + WebSocket) serving three client surfaces: the **traveler mobile app**, the **partner provider portal**, and the **internal company dashboard**.

---

## Architecture Overview

The codebase follows **Clean Architecture with DDD** enforced by ArchUnit (56 rules in `CleanArchitectureDddTest`).

```
Presentation Layer  ─── Controllers, WebSocket handlers, Exception handlers
        │ (depends on)
Application Layer   ─── Use-case services, DTOs, Application exceptions, Annotations
        │ (depends on)
Domain Layer        ─── Entities, Repository interfaces, Domain enums (no framework deps)
        │
Infrastructure Layer ── JPA adapters, JwtService, Security, Persistence, AOP, Config
```

Cross-module communication uses the **Module API pattern**: each feature module (`payment`, `booking`, `sys`, `portal`, etc.) exposes only a `modules/<module>/api/` interface. Other modules and the presentation layer depend only on that interface.

---

## Prerequisites

| Tool | Min version |
|---|---|
| Java | 21 |
| PostgreSQL | 15+ |
| Redis | 7+ (optional — login rate limiting; in-memory fallback used otherwise) |
| Kafka | 3+ (optional — staff notification delivery) |
| Docker | 24+ (optional — for `docker-compose` and Testcontainers) |

---

## Quick Start

### 1. Start infrastructure

```bash
# Spin up Postgres, Redis, MailHog (SMTP dev server)
docker compose up db redis mail -d
```

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env — at minimum set JWT_SECRET
```

### 3. Run the application

```bash
./gradlew bootRun
```

The API is available at `http://localhost:8080/api/v1`.  
Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`

### 4. Run tests

```bash
./gradlew test                                          # all tests (excludes Docker/Testcontainers)
./gradlew test -PrunDockerTests                         # include Testcontainers tests (Docker required)
./gradlew test --tests "*.CleanArchitectureDddTest"     # architecture rules only
./gradlew check                                         # tests + JaCoCo 60% line-coverage gate
```

---

## Environment Variables Reference

| Variable | Required | Default | Description |
|---|---|---|---|
| `JWT_SECRET` | **yes** | — | HS256 signing key. Generate: `openssl rand -base64 64` |
| `JWT_EXPIRATION` | no | `86400000` | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRATION` | no | `604800000` | Refresh token TTL (ms) |
| `JWT_COOKIE_ENABLED` | no | `true` | Return tokens as HttpOnly cookies |
| `JWT_COOKIE_SECURE` | no | `false` | Set `Secure` flag (enable in prod) |
| `ZIYARA_PII_ENCRYPTION_KEY_BASE64` | prod | — | AES-256 key for MFA secrets. `openssl rand -base64 32` |
| `ZIYARA_CORS_ALLOWED_ORIGINS` | no | _(localhost defaults)_ | Comma-separated allowed origins |
| `ZIYARA_SECURITY_MFA_REQUIRED_ROLES` | no | _(empty)_ | Roles forced to enrol TOTP before login |
| `ZIYARA_RLS_ENABLED` | no | `false` | Enable PostgreSQL Row-Level Security |
| `ZIYARA_DATA_RETENTION_ENABLED` | no | `false` | Run GDPR data-retention cron |
| `PAYMENT_GATEWAY_PROVIDER` | no | `stub` | `stub` or `stripe` |
| `PAYMENT_GATEWAY_API_KEY` | prod | — | Payment gateway API key |
| `APP_MEDIA_STORAGE_BACKEND` | no | `local` | `local` or `s3` |
| `APP_NOTIFICATIONS_EMAIL_ENABLED` | no | `false` | Enable SMTP email sending |
| `SPRING_MAIL_HOST` | no | `localhost` | SMTP host |
| `SPRING_DATA_REDIS_HOST` | no | `localhost` | Redis host |
| `ZIYARA_RATE_LIMIT_LOGIN_REDIS_ENABLED` | no | `false` | Use Redis for login rate-limit counters |
| `MANAGEMENT_PORT` | no | `8081` | Actuator port (separate from API port) |

See `.env.example` for the full list with descriptions and defaults.

---

## Running with Docker

Build and run everything (API + infrastructure):

```bash
docker compose up --build
```

The application image is built from `Dockerfile` (multi-stage, Eclipse Temurin 21). The HEALTHCHECK gives Flyway 60 s to run all migrations before health checks begin.

---

## API Documentation

Swagger UI is available in **dev/docker** profiles at:

```
http://localhost:8080/api/v1/swagger-ui.html
```

Swagger is **disabled** in `prod` profile (see `application-prod.yml`).

---

## Notable Configuration Files

| File | Purpose |
|---|---|
| `src/main/resources/application.yml` | Default config; env-var overrides for every setting |
| `src/main/resources/application-prod.yml` | Production hardening (HSTS, no Swagger, strict cookies) |
| `src/main/resources/application-dev.yml` | Dev extras (SQL logging, relaxed security) |
| `src/main/resources/db/migration/` | Flyway migrations V0–V25 |
