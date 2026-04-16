# Docker Testing Guide

## Quick Start

**Default (multi-domain on this machine):** add hosts entries **first** (see below), then:

```powershell
.\run-docker.ps1
```

This builds and starts: Postgres, backend, **three front-end images** (company / provider / landing), **edge proxy on port 80**, and pgAdmin.

```powershell
# Fresh DB: reset volumes and re-run migrations + seed
.\run-docker.ps1 -Fresh
```

## What Runs

| Service | Port | Description |
|---------|------|-------------|
| **PostgreSQL** | 5432 | Database with schema, migrations through 019 (service images + restaurant menu), and seed data |
| **Backend** | 8080 | Spring Boot API |
| **Proxy** (default via `run-docker.ps1`) | **80** | Nginx routes by `Host` → company / provider / landing containers |
| **pgAdmin** | 5050 | PostgreSQL admin UI (browser) |

There is **no** default app on **localhost:3000** — use **http://app.local** (after hosts file) so local testing matches production routing.

## Multi-domain (company / provider / landing)

Three separate front-end **bundles** (same repo, different `VITE_APP_SURFACE` build args) plus an **edge Nginx** proxy that routes by `Host`. Compose **profile** `multidomain` (always used by `.\run-docker.ps1`).

### 1. Hosts file (Windows: `C:\Windows\System32\drivers\etc\hosts`)

**Required before using http://app.local etc.** Run Notepad as Administrator, open the hosts file, add:

```text
127.0.0.1 app.local
127.0.0.1 partners.local
127.0.0.1 www.local
```

### 2. Start stack

```powershell
.\run-docker.ps1
```

Equivalent manual command:

```powershell
docker compose --profile multidomain up -d --build postgres backend dashboard-company dashboard-provider landing proxy pgadmin
```

| Host | App |
|------|-----|
| http://app.local | Company internal dashboard |
| http://partners.local | Provider partner portal |
| http://www.local | Public marketing landing (CTAs point at `app.local` / `partners.local`) |

Each host proxies `/api/` to the backend (same pattern as `front/my-app/nginx.conf`). **Port 80** must be free on the machine.

### 3. Production

- Point real DNS (e.g. `app.example.com`, `partners.example.com`, `www.example.com`) to your server.
- Edit [`infra/nginx/multi-domain.conf`](../infra/nginx/multi-domain.conf): replace `server_name` values and add `listen 443 ssl` blocks with certificates (e.g. Let’s Encrypt).
- Rebuild **landing** with production base URLs: set `VITE_COMPANY_APP_URL` and `VITE_PROVIDER_APP_URL` build args to your HTTPS app URLs (see `docker-compose.yml` `landing` service).

### 4. Optional: legacy dashboard on port 3000 (not prod-like)

Only if you explicitly need a single port for debugging:

```powershell
docker compose --profile legacy up -d --build dashboard
```

Then open **http://localhost:3000**. This service is **not** started by `.\run-docker.ps1`.

## Database Init (First Run)

When the Postgres container starts with an **empty volume**, it automatically runs:

1. `01-schema.sql` – base schema
2. `02-001.sql` … `16-015.sql` – migrations
3. `17-seed.sql` – demo data (users, departments, bookings, etc.)

To force a fresh init (migrations + seed), use:

```powershell
.\run-docker.ps1 -Fresh
```

## Test Credentials

| Email | Password | Role |
|-------|----------|------|
| `super_admin@ziyarah.com` | `Demo123!` | Super Admin (full access to all endpoints) |
| `admin@ziyarah.com` | `Demo123!` | Super Admin |
| `customer@ziyarah.com` | `Demo123!` | Customer |
| `sales@ziyarah.com` | `Demo123!` | Sales Manager |

## URLs

- **Company dashboard:** http://app.local (same-origin `/api/v1` via edge proxy)
- **Provider portal:** http://partners.local
- **Landing:** http://www.local
- **API:** http://localhost:8080/api/v1
- **Swagger:** http://localhost:8080/api/v1/swagger-ui.html (or via company app: http://app.local/api/v1/swagger-ui.html)
- **pgAdmin:** http://localhost:5050  
  - Sign in with `PGADMIN_DEFAULT_EMAIL` / `PGADMIN_DEFAULT_PASSWORD` (defaults: `admin@example.com` / `ziyarah_pgadmin`, overridable via env).  
  - **Register server:** right-click *Servers* → *Register* → *Server* → **General:** name `ziyarah` → **Connection:** Host `postgres`, Port `5432`, Database `ziyarah`, Username `ziyarah_user`, Password `ziyarah_password`. Use host `postgres` (Docker network name), not `localhost`.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| **pgAdmin cannot connect** | In the server dialog, set **Host** to `postgres` (not `localhost`). pgAdmin runs in a container on the same Docker network as Postgres. |
| **Dashboard "network error"** | Use **http://app.local** (not raw container port). SPA uses `/api/v1`; edge nginx proxies to backend. Rebuild: `docker compose --profile multidomain up -d --build dashboard-company`. |
| **Swagger UI blank / "Failed to load API definition"** | (1) **Nginx:** use **`location ^~ /api/`** so Swagger’s `.js` / `.css` under `/api/` are proxied. Rebuild front containers after nginx changes. (2) **OpenAPI servers:** include context path `/api/v1` (`OpenApiConfig`). Try http://app.local/api/v1/swagger-ui.html. |
| **GET /users/me returns 500 (`relation "users" does not exist`)** | The DB table is `sys_users` (see JPA `UserJpaEntity`). jOOQ reads use `UserQueryHandler` with that table name. |
| **Super admin sees "Access denied" (403) on API calls** | Backend uses JWT + `RequestAttributeSecurityContextRepository`; anonymous auth is disabled so Bearer tokens apply cleanly. Nginx must forward `Authorization` (see `front/my-app/nginx.conf`). For Vite dev on another port, CORS preflight uses `OPTIONS` (permitted in `SecurityConfig`). Rebuild backend + dashboard after security changes. |
| **403 on dashboard KPIs after backend update** | Company dashboard APIs require **company staff** roles (`ApiAuthorizationExpressions.COMPANY_STAFF`). Customers and provider accounts receive 403 on `/api/v1/dashboard/**`. Use a staff seed user (e.g. `super_admin@ziyarah.com`) on the company app. |
| **Multi-domain: wrong site / login message** | Company and provider apps are different bundles (`VITE_APP_SURFACE`). Staff must use **app.local** (or company URL); providers **partners.local**. |
| **502 on app.local after recreating front containers** | Nginx used to cache Docker DNS; config now uses `resolver 127.0.0.11` + variables. If you still see 502: `docker exec ziyarah-proxy nginx -s reload` or `docker compose --profile multidomain restart proxy`. |

## Service photos & restaurant menus

- **Photos:** Paste **URLs** (CDN/HTTPS) or **upload** JPEG/PNG/WebP/GIF (max 10MB) from **Manage photos & menu** on the service detail page; providers use the same panel on **partners.local**.
- **Database:** Migration `019` adds image `category` / `context_key` and `hotel_rest_menu_*` tables. For a clean DB run `.\run-docker.ps1 -Fresh`. On an existing Postgres volume, apply `database/migrations/019_service_image_category_and_restaurant_menu.sql` or recreate the volume.
- **Multipart uploads:** `POST /api/v1/services/{id}/images/upload` (and `/portal/services/{id}/images/upload`) save files under `APP_MEDIA_STORAGE_ROOT` (default in Compose: `/data/media` via volume `media_data`). Images are served at `GET /api/v1/media/**` (no auth). Override public URLs with `APP_MEDIA_PUBLIC_BASE_URL` if the API is behind another origin.

## Manual Docker Commands

```powershell
# Start prod-like stack (recommended)
docker compose --profile multidomain up -d postgres backend dashboard-company dashboard-provider landing proxy pgadmin

# Stop all
docker compose down

# Remove volumes (fresh DB on next up)
docker compose down -v

# View logs
docker compose logs -f backend
```
