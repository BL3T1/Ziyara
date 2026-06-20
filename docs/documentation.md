# Ziyara Platform — Documentation

Ziyara is a multi-tenant Syria-focused tourism and hospitality marketplace. This document covers local setup, environment variables, Docker deployment, and subsystem overviews.

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Local Development Setup](#local-development-setup)
3. [Docker Deployment](#docker-deployment)
4. [Environment Variables Reference](#environment-variables-reference)
5. [Mobile App Setup](#mobile-app-setup)
6. [API Overview](#api-overview)
7. [User Accounts](#user-accounts)

---

## Architecture Overview

| Component | Technology | Path |
|---|---|---|
| Backend API | Spring Boot 3.5 / Java 17 | `core/` |
| Database | PostgreSQL 15 + Flyway | `database/` |
| Web App (3 surfaces) | React 19 + Vite + TypeScript | `front/my-app/` |
| Mobile App | Flutter / Dart | `SYRIA-TOURISM-APP-main/` |
| Infrastructure | Docker Compose + Nginx | `docker-compose.yml`, `infra/` |

**Three web surfaces** are built from a single codebase via `VITE_APP_SURFACE`:
- `company` — Internal admin dashboard (http://app.local)
- `provider` — Provider self-service portal (http://partners.local)
- `landing` — Public consumer site (http://www.local)

---

## Local Development Setup

### Prerequisites
- Docker Desktop
- Java 17 (for backend-only dev)
- Node 20 (for frontend-only dev)
- Flutter 3.32+ (for mobile dev)

### hosts file (required for multi-domain mode)

Add to `/etc/hosts` (Linux/macOS) or `C:\Windows\System32\drivers\etc\hosts` (Windows):

```
127.0.0.1 app.local
127.0.0.1 partners.local
127.0.0.1 www.local
```

### Copy and configure environment

```bash
cp .env.example .env
# Edit .env — set POSTGRES_PASSWORD, JWT_SECRET (min 64 chars), PGADMIN_DEFAULT_PASSWORD
```

### Start the full stack (multi-domain mode — all 3 surfaces)

```bash
docker compose --profile multidomain up -d --build
```

Access points:
- Admin: http://app.local
- Provider portal: http://partners.local
- Consumer site: http://www.local
- pgAdmin: http://localhost:5050
- API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/api/v1/swagger-ui.html

### Start single-surface mode (company dashboard only)

```bash
docker compose --profile full up -d --build
# Admin available at http://localhost:80
```

### Backend only (no Docker)

```bash
# Requires PostgreSQL running locally
export JWT_SECRET="your-at-least-64-character-secret-key-here-change-me"
export SPRING_PROFILES_ACTIVE=dev
cd core && ./gradlew bootRun
```

### Frontend only

```bash
cd front/my-app
cp .env.example .env       # set VITE_API_URL and VITE_APP_SURFACE
npm install
npm run dev
```

---

## Docker Deployment

### Profiles

| Profile | Command | Description |
|---|---|---|
| *(default)* | `docker compose up -d` | postgres + kafka + backend + pgadmin |
| `full` | `docker compose --profile full up -d` | + single Vite frontend (company) on :80 |
| `multidomain` | `docker compose --profile multidomain up -d` | + nginx + 3 frontend surfaces |
| `legacy` | `docker compose --profile legacy up -d` | + dashboard on :3000 |

### First-time database initialization

The database image auto-runs `schema.sql` and `seed.sql` on first start. If you need to apply a specific migration manually:

```bash
docker compose exec postgres psql -U ziyarah_user -d ziyarah -f /migrations/022_rbac_permission_catalogue.sql
```

---

## Environment Variables Reference

### Required (must be set in `.env` or environment)

| Variable | Description |
|---|---|
| `POSTGRES_PASSWORD` | PostgreSQL password for `ziyarah_user` |
| `JWT_SECRET` | HMAC-SHA256 signing key — **minimum 64 characters**, must be random |
| `PGADMIN_DEFAULT_PASSWORD` | pgAdmin login password |

### Optional — Security

| Variable | Default | Description |
|---|---|---|
| `ZIYARA_RLS_ENABLED` | `false` (prod: `true`) | Enable PostgreSQL row-level security for multi-tenant isolation |
| `ZIYARA_PII_ENCRYPTION_KEY_BASE64` | *(empty)* | AES-256 key for PII field encryption. Generate: `openssl rand -base64 32`. **Required in production.** |
| `ZIYARA_CORS_ALLOWED_ORIGINS` | *(empty)* | Comma-separated CORS origins. Empty = localhost defaults in dev. |

### Optional — Payment

| Variable | Default | Description |
|---|---|---|
| `PAYMENT_GATEWAY_PROVIDER` | `stub` | `stub` or `stripe` |
| `PAYMENT_GATEWAY_API_KEY` | *(empty)* | Stripe secret key (`sk_live_...` or `sk_test_...`) |
| `PAYMENT_WEBHOOK_SECRET` | `whsec_change_me` | Stripe webhook signing secret |

### Optional — Media Storage

| Variable | Default | Description |
|---|---|---|
| `APP_MEDIA_STORAGE_BACKEND` | `local` | `local` or `s3` |
| `APP_MEDIA_S3_BUCKET` | *(empty)* | S3 bucket name (required when backend=s3) |
| `APP_MEDIA_S3_REGION` | `us-east-1` | AWS region |
| `APP_MEDIA_PUBLIC_BASE_URL` | *(empty)* | CDN base URL for stored media |

### Optional — Email

| Variable | Default | Description |
|---|---|---|
| `APP_NOTIFICATIONS_EMAIL_ENABLED` | `false` | Enable email notifications |
| `SPRING_MAIL_HOST` | `localhost` | SMTP host |
| `SPRING_MAIL_PORT` | `1025` | SMTP port |

---

## Mobile App Setup

The Flutter app is located in `SYRIA-TOURISM-APP-main/SYRIA-TOURISM-APP-main/`.

### API URL configuration

The base URL is set at build time via `--dart-define`:

```bash
# Local development (Android emulator — 10.0.2.2 maps to host localhost)
flutter run --dart-define=ZIYARA_API_URL=http://10.0.2.2:8080/api/v1

# Staging / production
flutter run --dart-define=ZIYARA_API_URL=https://api.ziyara.example.com/api/v1

# Build release APK
flutter build apk --release --dart-define=ZIYARA_API_URL=https://api.ziyara.example.com/api/v1
```

### Prerequisites

```bash
flutter pub get
flutter analyze   # should report no issues
flutter test
```

### Token storage

JWT tokens are stored in platform secure storage (`flutter_secure_storage`):
- Android: EncryptedSharedPreferences
- iOS: Keychain

---

## API Overview

Full OpenAPI 3 specification: `swagger-openapi.json` (6,500+ lines)

Interactive Swagger UI: http://localhost:8080/api/v1/swagger-ui.html

| Domain | Base Path | Description |
|---|---|---|
| Authentication | `/auth/**` | Login, register, refresh, logout, MFA |
| Users | `/users/**` | User management, RBAC |
| Services | `/services/**` | Hotel, resort, restaurant, trip listings |
| Bookings | `/bookings/**` | Full booking lifecycle |
| Payments | `/payments/**` | Payment initiation, webhooks, refunds |
| Dashboard | `/dashboard/**` | KPIs and analytics |
| Reports | `/reports/**` | Booking, revenue, commission reports |
| Portal | `/portal/**` | Provider self-service |
| Admin | `/admin/**` | System settings, feature flags, audit logs |

---

## User Accounts

Default accounts created by the seed data (change passwords in production):

| Role | Email | Password | Access |
|---|---|---|---|
| Super Admin | super_admin@ziyarah.com | Demo123! | http://app.local |
| Provider | provider@ziyarah.com | Demo123! | http://partners.local |
| Customer | landing@ziyarah.com | Demo123! | http://www.local |

---

## Subsystem READMEs

- Backend architecture: [ARCHITECTURAL_RATIONALE.md](ARCHITECTURAL_RATIONALE.md)
- Docker setup: [DOCKER.md](DOCKER.md)
- Running locally: [RUN.md](RUN.md)
- Database hardening: [database/DATABASE_HARDENING_INVENTORY.md](database/DATABASE_HARDENING_INVENTORY.md)
- Infrastructure: [infra/README.md](infra/README.md)
