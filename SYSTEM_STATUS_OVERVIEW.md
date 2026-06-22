# SYSTEM STATUS OVERVIEW
> SRE Audit · Ziyara · 2026-06-17

---

## Executive Summary

Ziyara is a well-structured modular monolith deployed on a single VPS via Docker Compose. The Clean Architecture (ArchUnit-enforced) is genuinely good discipline for the team size. The monitoring stack (Prometheus + Grafana + cAdvisor + node-exporter) is production-ready. Most database hot paths are properly indexed.

**However, three issues require immediate action before production traffic increases:**

1. The JVM heap ceiling (`-Xmx512m`) inside a 768 MB container leaves insufficient headroom for non-heap memory, making OOM kills likely under load.
2. Redis sits 28 MB below its container kill boundary — if it is killed, the JWT blocklist is lost and previously-revoked tokens become valid again (a security regression, not just an outage).
3. A race condition in the Flutter token-refresh interceptor causes concurrent 401 responses to log users out even when the refresh succeeds.

At the current (pre-production / low-traffic) scale, the system is stable. The issues are latent and will surface quickly as concurrent sessions increase.

---

## Health Traffic Light

| Layer | Status | Key Signal |
|---|---|---|
| **Backend** | 🟡 Yellow | JVM heap vs. container limit is tight; HikariCP timeout (60s) causes long hangs under pool exhaustion |
| **Frontend (Web)** | 🟡 Yellow | Routes correctly lazy-loaded; vendor chunk un-split (Recharts + Leaflet bundled together even when only one is needed per route) |
| **Database** | 🟢 Green | Good index coverage (V8, V35, V59); Flyway `repair-on-migrate: true` is a risk in prod |
| **Mobile** | 🔴 Red | Token-refresh race condition causes silent logouts; per-request connectivity syscall drains battery |
| **Docker / Infrastructure** | 🟡 Yellow | Redis container OOM boundary too tight (security risk); no log aggregation; solid Nginx TLS setup |

---

## System Scores (out of 10)

| Dimension | Score | Rationale |
|---|---|---|
| **Performance** | 7 / 10 | Parallel dashboard queries, Redis caching, proper indexes. Loses points for 60s pool timeout and un-split vendor chunk. |
| **Scalability** | 5 / 10 | Single PostgreSQL, no read replicas, no connection pooler (PgBouncer), Kafka single-node. Fine for now; hard ceiling at ~50 concurrent sessions. |
| **Security** | 6 / 10 | TLS gateway, HttpOnly JWT cookies, PII encryption, RBAC, RLS, rate limiting — all present. Redis OOM kills the blocklist (P0 security issue). |
| **Code Maintainability** | 8 / 10 | ArchUnit-enforced Clean Architecture is excellent. 60+ Flyway migrations are tightly numbered. Deductions for `repair-on-migrate: true` masking drift. |

---

## Architecture Snapshot

```
Browser / Mobile App
        │
        ▼
   Nginx Gateway (TLS 7005/7050/7060/7070)        [ipmode profile]
        │
   ┌────┴────────────────────────────┐
   │  Frontend nginx containers (×3) │  ← company, provider, landing
   │  React 19 / Vite / TailwindCSS  │     each ~80 routes, lazy-loaded
   └────────────────────────────────-┘
                    │  /api/*
                    ▼
          Spring Boot 3 / Java 21
          (HikariCP pool: max 10)
          (Redis cache + JWT blocklist)
          (Kafka producer → staff notifs)
            │           │           │
      PostgreSQL 15   Redis 7.2   Kafka 3.7 KRaft
      (512 MB cap)  (128 MB cap)  (256 MB cap)

Monitoring: Prometheus → Grafana, cAdvisor, node-exporter
Deploy: GitHub Actions → SSH → docker compose --profile portmode up -d --build
```
