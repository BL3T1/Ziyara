# Domain Migration Guide

Migrate from the self-signed IP setup (`ipmode`) to a real domain with automatic Let's Encrypt
certificates via Nginx Proxy Manager (`portmode` + NPM).

---

## Architecture after migration

```
Internet
   │
   ├── 80  (HTTP)  ─→  NPM (Let's Encrypt ACME challenge + redirect to HTTPS)
   └── 443 (HTTPS) ─→  NPM
                          ├── app.yourdomain.com      → company-portmode:80
                          ├── portal.yourdomain.com   → provider-portmode:80
                          ├── www.yourdomain.com      → landing-portmode:80
                          ├── api.yourdomain.com      → backend:8080
                          ├── grafana.yourdomain.com  → grafana:3000
                          ├── prometheus.yourdomain.com → prometheus:9090
                          └── pgadmin.yourdomain.com  → pgadmin:80
```

Each frontend container's built-in nginx already proxies `/api/` → `backend:8080` on the
internal Docker network, so API calls from `app.yourdomain.com` work without a separate
`api.` subdomain. The `api.` host exists for mobile apps, Swagger UI, and direct API access.

---

## Prerequisites

- A domain name you control (e.g. `yourdomain.com`)
- The server has a **public IP** reachable from the internet
- You can create DNS A records for subdomains

---

## Step 1 — DNS

Create A records pointing all subdomains to your server's public IP:

| Subdomain | Points to |
|-----------|-----------|
| `app.yourdomain.com` | `<server-public-ip>` |
| `portal.yourdomain.com` | `<server-public-ip>` |
| `www.yourdomain.com` | `<server-public-ip>` |
| `api.yourdomain.com` | `<server-public-ip>` |
| `grafana.yourdomain.com` | `<server-public-ip>` |
| `prometheus.yourdomain.com` | `<server-public-ip>` |
| `pgadmin.yourdomain.com` | `<server-public-ip>` |

DNS propagation can take a few minutes to a few hours. You can check with:
```bash
dig +short app.yourdomain.com
```

---

## Step 2 — Stop the IP-mode stack (if running)

```bash
docker compose --profile ipmode down
```

---

## Step 3 — Change NPM port bindings to 80 / 443

Let's Encrypt's HTTP-01 challenge requires port **80** to be reachable from the internet.
NPM currently binds `7080:80` and `7082:443`. Change these to standard ports.

In `docker-compose.yml`, find the `nginx-proxy-manager` service and update its ports:

```yaml
# Before
ports:
  - "7080:80"
  - "7082:443"
  - "127.0.0.1:7081:81"

# After
ports:
  - "80:80"
  - "443:443"
  - "127.0.0.1:7081:81"   # Admin UI stays loopback-only
```

> If port 80/443 are already used by another service on the host, use `iptables` to NAT
> `80 → 7080` and `443 → 7082` instead of changing the bindings.

---

## Step 4 — Update `.env`

```env
# ── NPM credentials ──────────────────────────────────────────
NPM_ADMIN_EMAIL=your-real-email@example.com
NPM_ADMIN_PASS=your-strong-npm-password

# ── CORS ─────────────────────────────────────────────────────
ZIYARA_CORS_ALLOWED_ORIGINS=https://app.yourdomain.com,https://portal.yourdomain.com,https://www.yourdomain.com

# ── Landing page CTA links ───────────────────────────────────
PORTMODE_HOST=app.yourdomain.com   # used by landing → "Sign In" button
```

No `BIND_IP`, `API_EXTERNAL_URL`, or cert variables needed — NPM handles all of that.

---

## Step 5 — Start the domain stack

```bash
docker compose --profile portmode up -d --build
```

This starts: `postgres`, `redis`, `kafka`, `backend`, 3 frontend containers,
`prometheus`, `grafana`, `cadvisor`, `node-exporter`, `pgadmin`, `nginx-proxy-manager`,
and `npm-init` (which auto-creates the proxy hosts).

---

## Step 6 — Update proxy host domains in NPM

The `npm-init` container auto-creates proxy hosts with `*.ziyara.local` placeholder domains.
You need to replace those with your real domains.

1. Open NPM Admin UI: `http://localhost:7081`
   (or via SSH tunnel: `ssh -L 7081:127.0.0.1:7081 user@server`)
2. Log in with the credentials from Step 4
3. Go to **Proxy Hosts**
4. For each host, click **Edit** and update the **Domain Names** field:

   | Replace | With |
   |---------|------|
   | `app.ziyara.local` | `app.yourdomain.com` |
   | `partners.ziyara.local` | `portal.yourdomain.com` |
   | `www.ziyara.local` | `www.yourdomain.com` |
   | `api.ziyara.local` | `api.yourdomain.com` |
   | `grafana.ziyara.local` | `grafana.yourdomain.com` |
   | `prometheus.ziyara.local` | `prometheus.yourdomain.com` |
   | `pgadmin.ziyara.local` | `pgadmin.yourdomain.com` |

---

## Step 7 — Issue Let's Encrypt certificates

For each proxy host in NPM:

1. Click **Edit** → **SSL** tab
2. Select **Request a new SSL Certificate**
3. Enable **Force SSL** and **HTTP/2 Support**
4. Enter your email address and agree to the Let's Encrypt ToS
5. Click **Save**

NPM issues and renews all certificates automatically.

> **Tip:** Issue certs in this order — `app`, `portal`, `www`, `api` first (main surfaces),
> then `grafana`, `prometheus`, `pgadmin`. If a cert fails, DNS hasn't propagated yet —
> wait a few minutes and retry.

---

## Step 8 — Verify

```bash
# API health
curl https://api.yourdomain.com/api/v1/actuator/health

# Check all containers are healthy
docker compose --profile portmode ps
```

Then open each surface in a browser:
- `https://app.yourdomain.com` — Company dashboard (login works)
- `https://portal.yourdomain.com` — Provider portal
- `https://www.yourdomain.com` — Landing page (CTA links point to app/portal)
- `https://grafana.yourdomain.com` — Grafana (Prometheus datasource green)
- `https://prometheus.yourdomain.com` — Prometheus (Targets all UP)
- `https://pgadmin.yourdomain.com` — pgAdmin (connects to DB)

---

## Security hardening after go-live

- **NPM Admin UI (port 7081)**: stays loopback-only. Access via SSH tunnel only.
- **Prometheus**: consider adding an **Access List** in NPM (HTTP Basic Auth) so the
  metrics endpoint isn't publicly readable.
- **pgAdmin**: consider the same — it exposes your database UI to the internet.
- **Firewall**: close all `7xxx` ports to the public. Only 80 and 443 need to be open.
  Internal ports (7001 DB, 7003 Redis, 7004 Kafka) are already loopback-only.
- **HSTS**: enable in `.env`: `ZIYARA_SECURITY_HSTS_ENABLED=true`
- **Cookie security**: `JWT_COOKIE_SECURE=true` (HTTPS only)
- **MFA** (optional): `ZIYARA_SECURITY_MFA_REQUIRED_ROLES=SUPER_ADMIN,CEO,FINANCE_MANAGER`

---

## Reverting to IP mode

```bash
docker compose --profile portmode down
# Restore NPM port bindings to 7080/7082 in docker-compose.yml
docker compose --profile ipmode up -d --build
```
