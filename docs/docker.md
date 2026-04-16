# Docker (multidomain stack)

**Run from the repository root** (`Ziyara/`), not from `core/`. Compose loads `./docker-compose.yml` and `.env` from the current directory. From `core/`, Docker may use the wrong project or only partial services.

```powershell
cd C:\Users\BL3T\Documents\GitHub\Ziyara
docker compose --profile multidomain up -d --build
```

Equivalent explicit file:

```powershell
docker compose -f docker-compose.yml --profile multidomain up -d --build
```

Ensure `.env` at the repo root defines at least: `POSTGRES_PASSWORD`, `JWT_SECRET`, `PGADMIN_DEFAULT_PASSWORD`.

Show bootstrap password hints:

```powershell
docker logs ziyarah-backend 2>&1 | Select-String -Pattern "bootstrap password|Super admin|APP_DEMO" | Select-Object -Last 20
```

Backend integration tests (Testcontainers):

```powershell
cd core
.\gradlew.bat test -PrunDockerTests
```
