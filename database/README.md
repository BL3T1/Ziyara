# Ziyarah — Database Management

## Overview

Schema management is handled **exclusively by Flyway**, which runs automatically when the Spring Boot application starts. There is no custom Dockerfile build step and no manual SQL scripts to run.

```
core/src/main/resources/db/migration/
    V0__initial_schema.sql          ← full baseline schema
    V1__...  through  V16__...      ← incremental DDL migrations
    V17__reference_data.sql         ← groups, roles, permissions, departments, plans, FX rates
```

---

## Quick Start — Dev / Docker

```bash
# 1. Start a plain postgres container
docker compose up postgres -d

# 2. Start the backend — Flyway creates the full schema + V17 reference data
docker compose up backend

# 3. (Optional) Load demo services, bookings and notifications
psql -h localhost -U ziyarah_user -d ziyarah -f database/seed_dev.sql
```

> **Tip:** `seed_dev.sql` resolves user IDs dynamically. Run it *after* the backend
> starts at least once so `DemoDataSeeder` has created the demo accounts first.

---

## Writing a New Migration

1. Create a file in `core/src/main/resources/db/migration/`:

   ```
   V18__your_description.sql
   ```

2. Use `ON CONFLICT DO NOTHING` / `IF NOT EXISTS` for idempotent operations.

3. Never modify a migration that has already been applied to any environment — add a new version instead.

---

## Reference Data vs Dev Seed Data

| File | Environment | What it contains |
|------|------------|-----------------|
| `V17__reference_data.sql` | All (runs via Flyway) | Groups (Z1–Z7), roles, permissions, departments, plans, exchange rates |
| `database/seed_dev.sql` | Dev only (run manually) | Demo service providers, services, one booking, payment, complaint, review, notification |

`seed_dev.sql` must **never** be applied in staging or production.

---

## Production First Run

Flyway runs automatically on startup. No manual SQL steps are required.

Reference: `ops/FIRST_RUN_PROD.md` for the full production checklist (secrets, TLS, etc.).

---

## Legacy Files (Archived)

The `database/migrations/001–037` scripts and `database/schema.sql` are kept for historical reference only. Their changes are fully absorbed into `V0__initial_schema.sql` and subsequent Flyway migrations. Do not run them on any environment.

`database/apply-all.ps1` is deprecated and will exit with an error if invoked.
