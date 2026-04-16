# Ziyarah System Technology and Server Install Report

This report summarizes:
- technologies used by the current system
- what must be installed on the deployment server
- what is optional

## 1) System Technologies (Current Repo)

### Backend API
- Framework: Spring Boot `3.5.12`
- Language: Java `17`
- Build tool: Gradle (`build.gradle.kts`)
- Data access: Spring Data JPA + jOOQ
- Security/Auth: Spring Security + JWT (`jjwt`)
- API docs: springdoc OpenAPI
- Runtime in production: containerized Java app (JRE image)

### Frontend
- Framework: React `19` + TypeScript
- Build tool: Vite
- Runtime in production: static files served by Nginx container

### Database
- PostgreSQL `15` (container image `postgres:15.17-alpine3.23`)
- Includes schema, migrations, and seed scripts at image build/init

### Reverse Proxy / Web Serving
- Nginx (used by frontend image and optional multi-domain proxy service)

### Container Orchestration
- Docker Compose (`docker-compose.yml`)

### Optional Services
- pgAdmin (database UI) on port `5050`
- Multi-domain proxy/service profile (`multidomain`)

## 2) What You Need to Install on the Server (Docker Deployment)

For Docker-based deployment, install only:

1. Docker Engine (20.10+ recommended)
2. Docker Compose plugin (v2+)

Also required:
- Access to pull/build images from Docker Hub
- Enough server resources (minimum practical baseline):
  - RAM: 4 GB+ (8 GB recommended for smoother builds)
  - CPU: 2 vCPU+
  - Disk: enough for images, containers, DB volume, logs

You do **not** need to install directly on host:
- Java
- Gradle
- Node.js / npm
- PostgreSQL
- Nginx

These are provided by container images during build/runtime.

## 3) Environment Variables Required for Compose

At minimum, set:
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `PGADMIN_DEFAULT_PASSWORD` (if pgAdmin service is enabled)

Optional:
- `PGADMIN_DEFAULT_EMAIL` (defaults to `admin@example.com` if not set)

## 4) Network/Port Requirements

Default exposed ports from `docker-compose.yml`:
- `80` (frontend or proxy depending on profile)
- `8080` (backend API)
- `5432` (PostgreSQL)
- `5050` (pgAdmin, if used)
- `3000` (legacy dashboard profile)

Open only the ports you actually need publicly. In production, typically expose `80/443` publicly and restrict DB/admin ports.

## 5) Backend Build Clarification

Current backend Docker build uses **Gradle**:
- `core/Dockerfile` uses `gradle:8.7-jdk17` and runs `gradle bootJar`

## 6) Recommended Production Extras (Host Level)

- TLS termination (Nginx/Traefik/Caddy or cloud load balancer)
- Firewall rules (limit public ports)
- Backup policy for PostgreSQL volume
- Monitoring/logging (container logs + host metrics)
- Automatic restart policy and update procedure

---

## Quick Answer

For your server deployment with Docker: install **Docker + Docker Compose only** and provide required env vars.  
No host-level language/build tool installation is needed on the server.
