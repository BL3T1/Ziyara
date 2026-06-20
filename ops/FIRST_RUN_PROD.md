# Production First-Run Checklist

> Follow this guide in order on every fresh production environment.
> Flyway runs automatically on backend startup — no manual SQL steps are needed.

---

## 1. Pre-flight — Required Secrets

All values below **must** be set in the environment (`.env` or your secrets manager)
before starting any service.

| Variable | How to generate | Notes |
|---|---|---|
| `POSTGRES_PASSWORD` | `openssl rand -hex 24` | Avoid `!` and `$` |
| `JWT_SECRET` | `openssl rand -hex 48` | Min 64 chars |
| `ZIYARA_PII_ENCRYPTION_KEY_BASE64` | `openssl rand -base64 32` | Required — app refuses to start without it in prod |
| `PAYMENT_WEBHOOK_SECRET` | `openssl rand -hex 32` | Required if `PAYMENT_GATEWAY_PROVIDER != stub` |
| `PGADMIN_DEFAULT_PASSWORD` | any strong password | pgAdmin login |
| `ZIYARA_CORS_ALLOWED_ORIGINS` | e.g. `https://app.example.com,https://partners.example.com` | Comma-separated |

Optional but recommended:

```bash
ZIYARA_RLS_ENABLED=true
ZIYARA_SECURITY_HSTS_ENABLED=true
MANAGEMENT_PORT=8081
```

---

## 2. First-Start Sequence

```bash
# 1. Start the database and wait for it to be healthy
docker compose up postgres -d
docker compose ps   # wait until postgres shows "healthy"

# 2. Start Redis and Kafka
docker compose up redis kafka -d
docker compose ps   # wait until both are healthy / started

# 3. Start the backend — Flyway applies V0–V17 automatically
docker compose up backend -d
docker compose logs -f backend   # watch for "Started ZiyarahApplication"
```

---

## 3. Verify Flyway Migrations

```bash
# Check the last applied migration (replace 8081 with your MANAGEMENT_PORT):
curl -s http://localhost:8081/actuator/flyway \
  | jq '.contexts[].flywayBeans[].migrations[-1]'

# Expected output: { "version": "17", "state": "SUCCESS", ... }
```

---

## 4. Verify Actuator Health

```bash
curl -s http://localhost:8081/actuator/health | jq .
# All components should show "UP":
# { "status": "UP", "components": { "db": ..., "redis": ..., ... } }
```

---

## 5. Super Admin First Login

The demo super admin is created on first startup when `app.demo.super-admin.enabled=true`.

**Credentials:**
- Email: `super_admin@ziyarah.com`
- Password: value of `APP_DEMO_PASSWORD` (default: `Demo123!`)

> **Change the password immediately** via `PUT /users/me/password`.

After the first login, disable the seeder:
```
APP_DEMO_SUPER_ADMIN_ENABLED=false
```
then restart the backend.

---

## 6. Enrol TOTP (MFA) — Critical for Privileged Accounts

Before enabling MFA enforcement, the super admin must enrol TOTP:

```bash
# 1. Log in and get the access token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"super_admin@ziyarah.com","password":"Demo123!"}' \
  | jq -r '.data.accessToken')

# 2. Start TOTP enrolment
curl -s -X POST http://localhost:8080/api/v1/users/me/mfa/enroll/start \
  -H "Authorization: Bearer $TOKEN" | jq .
# → Returns otpauth:// URI — scan with Google Authenticator / Authy

# 3. Confirm enrolment with the first 6-digit code
curl -s -X POST http://localhost:8080/api/v1/users/me/mfa/enroll/confirm \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"code":"123456"}' | jq .
```

Once all privileged accounts have TOTP enrolled, enable enforcement:
```
ZIYARA_SECURITY_MFA_REQUIRED_ROLES=SUPER_ADMIN,CEO,FINANCE_MANAGER,GENERAL_MANAGER
```
then restart the backend.

---

## 7. Smoke Tests

```bash
BASE=http://localhost:8080/api/v1

# Health (public)
curl -sf $BASE/actuator/health | jq .status

# Login
curl -sf -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"super_admin@ziyarah.com","password":"Demo123!"}' \
  | jq .data.role

# List services (public)
curl -sf $BASE/services | jq .data.totalElements

# OpenAPI must be 404 in production
curl -o /dev/null -w "%{http_code}\n" $BASE/swagger-ui.html
# → 404
```

---

## 8. Rollback Procedure

```bash
# 1. Stop the backend
docker compose stop backend

# 2. Update the image tag in .env (or docker-compose.yml override)
#    BACKEND_IMAGE=ghcr.io/<org>/backend:v0.9.0

# 3. Restart with the previous image
docker compose up -d backend

# 4. If a Flyway migration needs reverting:
#    - Do NOT run Flyway repair / baseline manually.
#    - Add a new Flyway migration (V18__revert_...) that reverts the schema change.
#    - Restart the backend — Flyway applies it automatically.
```

---

## 9. Secrets Rotation

| Secret | Rotation impact |
|---|---|
| `JWT_SECRET` | All existing tokens become invalid — all users are logged out |
| `POSTGRES_PASSWORD` | Backend loses DB connection until restarted with the new value |
| `ZIYARA_PII_ENCRYPTION_KEY_BASE64` | **Cannot be changed without re-encrypting all PII fields** — plan carefully |
| `PAYMENT_WEBHOOK_SECRET` | Update in your payment provider dashboard simultaneously |

---

## Reference

- Flyway migrations: `core/src/main/resources/db/migration/V0__*.sql … V17__*.sql`
- Dev seed data (never in prod): `database/seed_dev.sql`
- Environment template: `.env.example`


- session id: 2841e41a-891d-4b7f-816e-b4b5dd34c32c