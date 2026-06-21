# Load Balancer Implementation Plan — Ziyara

**Date:** 2026-06-21  
**Branch:** V1  
**Status:** Plan (not yet implemented)

---

## System Study Summary

Before the plan, a full read of `docker-compose.yml`, `infra/nginx/api-nginx.conf`, `infra/nginx/gateway.conf`, and `front/my-app/nginx.conf` revealed the following:

| Component | Current State | Load-balancing impact |
|---|---|---|
| `ziyarah-backend` | 1 container, 2 CPU / 1536MB | Primary target: scale to N replicas |
| `api-nginx` | Proxies to single `backend:8080` via `set $var` | Already in place — just needs an upstream pool |
| `gateway` (ipmode) | TLS termination, routes to `api-nginx` | No change needed |
| `pgbouncer` | SESSION mode, pool_size=90 | pool_size must be raised to N × 80 |
| `redis` | Single instance, JWT blocklist | No change — all replicas share it correctly |
| `media_data` | Local Docker volume | Must become a bind-mount on same host (or NFS/S3 for multi-host) |
| Kafka | Single-node KRaft | Not a LB target; SPOF — out of scope for this plan |
| Frontend containers | 3 static SPA containers | Low traffic; no LB needed now |

### Critical constraints

1. **PgBouncer SESSION mode** — `RlsAwareDataSource` sets `app.current_user_id` as a session-level GUC at connection acquire time. PgBouncer SESSION mode assigns one Postgres server connection per client session, so multiple backends sharing one PgBouncer pool is safe. The GUC travels with the server connection, not with the pool. **No special handling required — but pool_size must be raised.**

2. **WebSocket (STOMP)** at `/api/v1/ws` — 3600-second timeout, session state is in-memory per JVM. Round-robin LB would cause reconnects to hit a different backend and lose STOMP subscriptions. **Sticky routing (IP hash) is required for WebSocket connections.**

3. **JWT is stateless** — HttpOnly cookies validated against Redis blocklist. Every replica shares the same Redis → no sticky sessions needed for REST endpoints.

4. **media_data** — uploaded files are served back from the same container that wrote them. A second backend replica cannot read files written by the first. **Shared storage is required before adding a second replica.**

5. **HikariCP** — max-pool-size=80 per backend. With N replicas, PgBouncer pool_size must be ≥ N × 80. At N=3: set pool_size=250.

---

## Architecture After Implementation

```
Internet
   │
   ▼
gateway (nginx, TLS)          ← unchanged, ipmode only
   │
   ▼
api-nginx (nginx, upstream)   ← CHANGED: upstream pool of N backends
   ├──────────────────────────────────────┐
   ▼                                      ▼
backend-1 (JVM)               backend-2 (JVM)      ...backend-N
   │                                      │
   └──────────────┬───────────────────────┘
                  ▼
              pgbouncer (SESSION mode, pool_size raised)
                  ▼
              postgres
              
              redis (shared, unchanged)
              media_data (bind-mount → same dir on host)
```

WebSocket connections (`/api/v1/ws`) → routed to the same backend via `ip_hash`.  
REST connections (`/`) → round-robin across all healthy backends.

---

## Phase 1 — Shared Storage (prerequisite)

`media_data` is currently a named Docker volume. Named volumes cannot be shared between containers on the same host without converting to a bind-mount pointing to the same host directory.

**Change `docker-compose.yml`:** Replace the `media_data` volume definition and all `backend` volume mounts:

```yaml
# REMOVE from the volumes: section at the bottom:
#   media_data:
#     name: ziyara-media-data

# REPLACE backend volume mount with a bind-mount:
services:
  backend:
    volumes:
      - /srv/ziyara/media:/data/media    # host path → container path
```

Create the directory on the host before first start:
```bash
mkdir -p /srv/ziyara/media
chown 1000:1000 /srv/ziyara/media   # match the JVM user inside the container
```

All backend replicas will mount the same host directory, so files written by any replica are visible to all others.

> **Future (multi-host):** Replace the bind-mount with an NFS mount or migrate uploads to S3/MinIO. The `StoragePort` in the application layer already abstracts the file backend — only the infrastructure adapter needs to change.

---

## Phase 2 — PgBouncer Tuning

Edit `infra/pgbouncer/pgbouncer.ini`:

```ini
# Current:
pool_size = 90

# With 2 replicas (N=2), HikariCP max_pool=80 per replica:
pool_size = 170   # 2 × 80 + 10 headroom

# With 3 replicas (N=3):
pool_size = 250   # 3 × 80 + 10 headroom
```

Also confirm `max_client_conn` is at least `pool_size + 20`:
```ini
max_client_conn = 280
```

No changes to SESSION mode — it stays as-is.

---

## Phase 3 — Add Backend Replicas to Docker Compose

The cleanest approach for Docker Compose (without Swarm) is to declare explicit named replicas sharing the same image and env vars.

### `docker-compose.yml` changes

```yaml
# Rename the existing service to backend-1 (or keep as backend and add backend-2):
# Easiest: keep 'backend' as backend-1, add backend-2 with identical config.

  backend-2:
    image: ${BACKEND_IMAGE:-ziyarah-backend:latest}
    build:
      context: ./core
      dockerfile: Dockerfile
    container_name: ziyarah-backend-2
    restart: unless-stopped
    depends_on:
      pgbouncer:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    environment:
      # Identical to backend-1 — copy all env vars from the backend service block
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      # ... all other env vars identical to backend-1
    volumes:
      - /srv/ziyara/media:/data/media    # same bind-mount as backend-1
    networks:
      - ziyara-network
    deploy:
      resources:
        limits:
          cpus: "2"
          memory: 1536M
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 3
      start_period: 60s
    profiles:
      - portmode
      - ipmode
```

> Note: The original `backend` container name stays `ziyarah-backend`. Add `backend-2` as a second service. This avoids renaming the existing service and breaking existing `depends_on` references in `api-nginx` and frontend containers.

---

## Phase 4 — Update api-nginx (the load balancer)

This is the core change. Replace the single `set $backend` variable with a proper nginx `upstream` block.

### `infra/nginx/api-nginx.conf` — full replacement

```nginx
# Docker's embedded DNS — re-resolve upstreams every 15 s
resolver 127.0.0.11 valid=15s ipv6=off;

# ── Upstream pool ─────────────────────────────────────────────────────────────
upstream backend_pool {
    # Least-connections distributes traffic to the least busy backend
    least_conn;

    server backend:8080   max_fails=3 fail_timeout=10s;
    server backend-2:8080 max_fails=3 fail_timeout=10s;
    # Add backend-3:8080 here if/when a third replica is added

    keepalive 64;   # persistent connections from nginx to backends
}

# ── Sticky upstream for WebSocket (IP hash) ───────────────────────────────────
upstream backend_ws_pool {
    ip_hash;   # same client IP always routes to the same backend

    server backend:8080;
    server backend-2:8080;
}

# ── Connection-limit zone ─────────────────────────────────────────────────────
limit_conn_zone $binary_remote_addr zone=api_conn:10m;

# ── Request-rate zone ─────────────────────────────────────────────────────────
limit_req_zone  $binary_remote_addr zone=api_req:10m  rate=2000r/s;

server {
    listen 80;
    server_name _;

    limit_conn_status 503;
    limit_req_status  503;

    # ── WebSocket (STOMP/SockJS) — sticky routing, no rate limit ─────────────
    location ^~ /api/v1/ws {
        proxy_pass         http://backend_ws_pool;
        proxy_http_version 1.1;
        proxy_set_header   Upgrade    $http_upgrade;
        proxy_set_header   Connection "upgrade";
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 3600s;
        proxy_send_timeout 3600s;
    }

    # ── Actuator health — no limits so Docker healthcheck always succeeds ─────
    location = /api/v1/actuator/health {
        proxy_pass         http://backend_pool;
        proxy_http_version 1.1;
        proxy_set_header   Connection "";
    }

    # ── All REST API traffic — round-robin with limits ────────────────────────
    location / {
        limit_conn api_conn 300;
        limit_req  zone=api_req burst=1000 nodelay;

        proxy_pass         http://backend_pool;
        proxy_http_version 1.1;
        proxy_set_header   Connection        "";
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;

        proxy_connect_timeout 10s;
        proxy_read_timeout    65s;
        proxy_send_timeout    65s;

        proxy_buffering   on;
        proxy_buffer_size 8k;
        proxy_buffers     16 8k;
    }

    # ── Graceful overload response ─────────────────────────────────────────────
    error_page 503 @overloaded;
    location @overloaded {
        add_header Retry-After  5 always;
        add_header Content-Type "application/json" always;
        return 503 '{"code":"SERVICE_OVERLOADED","message":"Server is temporarily overloaded. Please retry in a few seconds."}';
    }
}
```

**Key decisions:**
- `least_conn` for REST (better than round-robin for JVM backends with variable request cost)
- `ip_hash` for WebSocket (sticky, prevents STOMP subscription loss on reconnect)
- `max_fails=3 fail_timeout=10s` — after 3 consecutive failures, that backend is removed from the pool for 10 seconds; health is re-checked automatically
- `keepalive 64` — reuse upstream TCP connections (reduces JVM accept overhead)

---

## Phase 5 — Update gateway.conf (ipmode WebSocket route)

`gateway.conf` currently hard-codes `set $backend backend:8080` for the WebSocket location. It must route to `api-nginx` instead (which already does sticky routing). The REST location already routes through `api-nginx` — only the WebSocket location needs fixing:

**`infra/nginx/gateway.conf` — change the `/api/v1/ws` block:**

```nginx
# Current (gateway routes WebSocket directly to backend, bypassing api-nginx):
location ^~ /api/v1/ws {
    set $backend backend:8080;
    ...
}

# New (route through api-nginx so sticky ip_hash is applied):
location ^~ /api/v1/ws {
    set $apinginx api-nginx:80;
    proxy_pass http://$apinginx/api/v1/ws;
    proxy_http_version 1.1;
    proxy_set_header Upgrade    $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host                $http_host;
    proxy_set_header X-Real-IP           $remote_addr;
    proxy_set_header X-Forwarded-For     $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto   https;
    proxy_read_timeout 3600s;
    proxy_send_timeout 3600s;
}
```

The REST location in `gateway.conf` (`location /`) already routes to `api-nginx` — no change needed there.

---

## Phase 6 — Monitoring Updates

The existing Prometheus + Grafana stack already scrapes cAdvisor (container metrics). Two additional steps:

### 6.1 Scrape both backends
`infra/prometheus/prometheus.yml` — add `backend-2` to the backend scrape job:

```yaml
- job_name: 'spring-backend'
  metrics_path: '/api/v1/actuator/prometheus'
  static_configs:
    - targets:
        - 'backend:8080'
        - 'backend-2:8080'
  labels:
    app: ziyara-backend
```

### 6.2 nginx upstream metrics (optional but recommended)
Enable `ngx_http_stub_status_module` in `api-nginx` by adding to the server block:
```nginx
location = /nginx_status {
    stub_status;
    allow 172.0.0.0/8;   # Docker network only
    deny all;
}
```
Add a Prometheus nginx-exporter container to scrape `/nginx_status` and expose upstream health metrics to Grafana.

---

## Rollout Steps (Zero-Downtime)

Perform on the production host with `docker compose --profile ipmode`:

```
Step 1: Create shared media directory
  mkdir -p /srv/ziyara/media
  cp -a <current volume data> /srv/ziyara/media/   ← migrate existing uploads

Step 2: Update pgbouncer.ini (pool_size, max_client_conn)
  docker compose --profile ipmode restart pgbouncer

Step 3: Deploy api-nginx config change only (no new backends yet)
  docker compose --profile ipmode restart api-nginx
  # Verify: curl -I http://localhost/api/v1/actuator/health → 200

Step 4: Update gateway.conf WebSocket route
  docker compose --profile ipmode restart gateway

Step 5: Start backend-2 while backend-1 is still serving
  docker compose --profile ipmode up -d backend-2
  # Wait for healthcheck to go green (~60 s):
  docker compose --profile ipmode ps backend-2

Step 6: Apply api-nginx upstream config (adds backend-2 to the pool)
  docker compose --profile ipmode restart api-nginx
  # Both backends now receive traffic

Step 7: Verify
  # Watch logs from both backends simultaneously:
  docker compose --profile ipmode logs -f backend backend-2
  # Confirm requests are distributed — look for request IDs alternating between containers
  # Check Grafana: both backend JVM metric series should appear

Step 8: Update media_data mounts
  # Update docker-compose.yml backend + backend-2 volume mounts to bind-mount
  docker compose --profile ipmode up -d --force-recreate backend backend-2
```

---

## Files to Change

| File | Change |
|---|---|
| `docker-compose.yml` | Add `backend-2` service; change `media_data` volume to bind-mount on both backends; raise `pgbouncer` pool_size |
| `infra/nginx/api-nginx.conf` | Replace `set $backend` with `upstream backend_pool` (least_conn) and `upstream backend_ws_pool` (ip_hash); add WebSocket location |
| `infra/nginx/gateway.conf` | Route `/api/v1/ws` through `api-nginx` instead of directly to `backend:8080` |
| `infra/pgbouncer/pgbouncer.ini` | Raise `pool_size` and `max_client_conn` |
| `infra/prometheus/prometheus.yml` | Add `backend-2:8080` to backend scrape targets |

No application code changes required. All load-balancing is infrastructure-only.

---

## Out of Scope (Future Work)

| Item | Notes |
|---|---|
| Kafka multi-broker | Single-node KRaft is a SPOF but unrelated to API load balancing |
| Redis Sentinel / Cluster | Single Redis is fine for current scale; upgrade if Redis becomes the bottleneck |
| Multi-host deployment | Requires Docker Swarm / Kubernetes and NFS/S3 for media storage |
| Frontend load balancing | 3 static SPA containers serve pre-built HTML — CDN caching is a better fit than LB |
| Auto-scaling | Requires Swarm or K8s; the Compose config above is the foundation for extracting to K8s later |
