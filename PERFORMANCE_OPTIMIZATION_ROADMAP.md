# PERFORMANCE OPTIMIZATION ROADMAP
> SRE Audit · Ziyara · 2026-06-17

---

## 30-Day Plan — Stabilize (Do Not Break Production)

**Goal:** Eliminate the three P0s and the two highest-impact P1s before any marketing push.

| Day | Action | Owner | Risk |
|---|---|---|---|
| 1–2 | Apply P0-2 (Redis memory fix) — lowest risk, one env-var change + compose limit | SRE | None — no code change |
| 3–4 | Apply P0-1 (JVM heap + container limit) | SRE | Container restart; test in staging first |
| 5–7 | Apply P0-3 (Flutter token refresh race condition) | Mobile | Needs regression test of login/logout flows |
| 8–9 | Apply P1-4 (HikariCP 60s → 8s timeout) | Backend | Monitor 503 rate in Grafana for 48h post-deploy |
| 10 | Apply P1-5 (Flyway repair-on-migrate: false) | Backend | Startup will fail if any migration file was silently repaired in prod; audit Flyway history first |
| 11–14 | Apply P1-6 (Dashboard executor pool 4→6) + P1-8 (vendor chunk splitting) | Backend + Frontend | Low risk; bundle size reduction measurable in Lighthouse |
| 15–20 | Mobile: Replace per-request connectivity check with a global stream listener | Mobile | Remove `connectivity_plus` from `onRequest`; add a ConnectivityBloc/Cubit |
| 21–30 | Add Loki log aggregation to the monitoring stack | SRE | Additive; no service changes needed |

---

## 60-Day Plan — Performance Hardening

### Database

**Add PgBouncer in transaction-pooling mode** in front of PostgreSQL. HikariCP keeps 10 real connections open; with PgBouncer, the actual PostgreSQL `max_connections` can stay at 20 while the backend sees a logical pool of 40+.

```yaml
# Add to docker-compose.yml (base profile)
pgbouncer:
  image: pgbouncer/pgbouncer:1.23.1
  environment:
    DATABASES_HOST: postgres
    DATABASES_PORT: 5432
    DATABASES_DBNAME: ziyarah
    DATABASES_USER: ziyarah_user
    DATABASES_PASSWORD: ${POSTGRES_PASSWORD}
    PGBOUNCER_POOL_MODE: transaction
    PGBOUNCER_MAX_CLIENT_CONN: 100
    PGBOUNCER_DEFAULT_POOL_SIZE: 20
  ports:
    - "127.0.0.1:7002:5432"
  networks:
    - ziyarah-network
```

Then point HikariCP at `pgbouncer:5432` instead of `postgres:5432`. **Trade-off:** PgBouncer in transaction mode is incompatible with `SET LOCAL`, advisory locks, and `LISTEN/NOTIFY`. Verify the RLS session-variable mechanism works in transaction mode.

**Enable materialized view refresh** for the reporting module. Change `ZIYARA_REPORTING_MV_REFRESH_ENABLED=true` and schedule the cron at 02:20 AM daily (already configured). This shifts analytics queries from live tables to pre-computed snapshots.

**Tune PostgreSQL inside the container.** Add a `postgresql.conf` override mount:

```sql
-- infra/postgres/postgresql.conf
shared_buffers          = 256MB   -- raise from default ~128MB; allocate half of 512MB cap
work_mem               = 4MB      -- per-sort/per-hash; 10 connections × 4MB = 40MB max
maintenance_work_mem   = 64MB
effective_cache_size   = 384MB
random_page_cost       = 1.1      -- SSD/overlay FS in Docker
checkpoint_completion_target = 0.9
wal_buffers            = 16MB
```

```diff
# docker-compose.yml — postgres service
  postgres:
    image: postgres:15.17-alpine3.23
+   command: postgres -c config_file=/etc/postgresql/postgresql.conf
    volumes:
      - postgres_data:/var/lib/postgresql/data
+     - ./infra/postgres/postgresql.conf:/etc/postgresql/postgresql.conf:ro
```

**Trade-off:** `shared_buffers = 256MB` uses half the container's memory for the buffer pool. The PostgreSQL process will use ~350–400 MB total. The container limit of 512 MB is still adequate, but monitor OOM headroom carefully.

### Backend

**Increase HikariCP pool to 15** once PgBouncer is in place:

```yaml
maximum-pool-size: ${SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE:15}
```

**Add health-based alerting in Grafana** for the HikariCP pool:
- Alert on `hikaricp_connections_pending > 0` sustained for 30 s → pool pressure is building
- Alert on `hikaricp_connections_timeout_total > 0` → connections are being rejected

**Enable Spring Boot's virtual-thread executor** (Java 21 Project Loom) for request handling. This is a single config change that removes the 200-thread Tomcat cap and makes each request a cheap virtual thread:

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true
```

**Trade-off:** Virtual threads serialize on DB operations (which are blocking). HikariCP still needs a real thread for each active DB call. The benefit is that 200 concurrent requests can each be a virtual thread — only the 10–15 actually waiting on the DB hold a real OS thread.

### Mobile

**Bundle fonts locally** instead of fetching from Google Fonts CDN:

```yaml
# pubspec.yaml
flutter:
  fonts:
    - family: YourFont
      fonts:
        - asset: assets/fonts/YourFont-Regular.ttf
        - asset: assets/fonts/YourFont-Bold.ttf
          weight: 700
```

Remove `google_fonts` from dependencies. This eliminates a CDN dependency that blocks rendering in restricted-network markets.

**Add offline cache using `shared_preferences` or `hive`** for the last-known state of the most-used screens (bookings list, services list). On mount, return cached data immediately, then refresh in the background.

### Frontend (CI)

**Add BuildKit cache to the frontend Dockerfile:**

```diff
# front/my-app/Dockerfile
  FROM node:20-alpine AS build
  WORKDIR /app
  COPY package*.json ./
- RUN npm ci --no-audit --no-fund
+ RUN --mount=type=cache,target=/root/.npm \
+     npm ci --no-audit --no-fund
```

This caches `~/.npm` (the npm download cache) across builds. Combined with the unchanged `package-lock.json` as a cache key, subsequent CI builds install from cache instead of the network. **Trade-off:** None — pure build-time win, no effect on the runtime image.

---

## 90-Day Plan — Horizontal Readiness

### Database — Read Replica

Deploy a PostgreSQL streaming replica (same VPS or second VPS):

```yaml
# docker-compose.yml (second server or additional service)
postgres-replica:
  image: postgres:15.17-alpine3.23
  environment:
    POSTGRES_REPLICATION_USER: replicator
    POSTGRES_REPLICATION_PASSWORD: ${PG_REPLICA_PASSWORD}
    POSTGRES_MASTER_HOST: postgres
  # configure recovery.conf / primary_conninfo
```

Route all `@Transactional(readOnly = true)` JPA calls to the replica by configuring Spring's `AbstractRoutingDataSource` with a read/write split. **Trade-off:** Replica lag (~50–200 ms on same LAN) means reads can be slightly stale. This is acceptable for dashboards and analytics; keep writes and auth queries on primary.

### Redis — Sentinel or Cluster

If JWT security is a hard requirement for production uptime, upgrade from a single Redis node to Redis Sentinel (3 nodes: 1 primary + 2 replicas + 3 sentinels). Sentinel provides automatic failover in ~15 s; the JWT blocklist survives a primary crash. **Trade-off:** +2 Redis containers. Spring's `RedisConnectionFactory` supports Sentinel natively via `spring.data.redis.sentinel.*` config.

### CDN for Static Assets

Put a CDN (Cloudflare Free tier) in front of the nginx frontend containers. All JS/CSS/image assets have `Cache-Control: public, immutable, max-age=31536000` — they will be served from edge PoPs globally. The backend API is still proxied through the origin. **Trade-off:** DNS is now managed by Cloudflare; certificate renewal is handled by them (simpler than the current self-signed cert chain for `ipmode`).

---

## Monitoring Metrics to Track Immediately

Add these Grafana panels to your existing Prometheus data source:

| Metric | Alert Threshold | What It Tells You |
|---|---|---|
| `hikaricp_connections_pending` | > 0 for 30s | HikariCP pool under pressure |
| `hikaricp_connections_timeout_total` | Any increase | Connections being dropped; users getting 503 |
| `jvm_memory_used_bytes{area="heap"}` | > 450 MB | Approaching -Xmx560m ceiling |
| `jvm_memory_used_bytes{area="nonheap"}` | > 140 MB | Approaching MaxMetaspaceSize |
| `redis_memory_used_bytes` | > 170 MB | Approaching 200MB maxmemory |
| `process_cpu_usage{job="ziyara-backend"}` | > 0.8 for 2 min | Backend CPU-bound (GC pressure?) |
| `http_server_requests_active_seconds_count` | > 50 | Requests piling up (Tomcat thread pool saturation) |
| `spring_data_repository_invocations_seconds_max` | > 2s | Slow repository calls (unindexed query or lock) |
| `kafka_consumer_fetch_latency_avg` | > 200ms | Kafka falling behind |
| `pg_stat_activity_count` (via postgres exporter) | > 15 | Connection count approaching pool max |

**Add `pg_stat_statements` extension to PostgreSQL** to track actual slow queries:

```sql
-- Run once on the database
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

Then query it weekly:
```sql
SELECT query, calls, mean_exec_time, total_exec_time
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

This surfaces query patterns that indexes missed — the most honest source of truth for DB performance.

---

## Scaling Decision Tree

```
Current load: low / pre-production
│
├── < 50 concurrent sessions?
│   → Current single VPS is fine. Focus on P0/P1 fixes.
│
├── 50–200 concurrent sessions?
│   → Add PgBouncer (Day 30–60 plan)
│   → Enable virtual threads (single config change)
│   → Add read replica for dashboard queries
│
└── > 200 concurrent sessions / > 1 M bookings in DB?
    → Evaluate splitting Kafka into a dedicated broker VPS
    → Add a second backend instance behind a load balancer
    → PostgreSQL requires dedicated hardware (separate VPS, tuned OS params)
    → Kubernetes becomes worth the operational overhead at this point
```

Kubernetes is **not** recommended at current scale. The Docker Compose deployment is operationally appropriate and the CI/CD pipeline already handles zero-downtime rolling restarts via `--remove-orphans`. Kubernetes would add 40+ hours of migration work for no measurable gain until you need multi-region or more than 3 backend replicas.
