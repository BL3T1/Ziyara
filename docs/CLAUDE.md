# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## Repo Layout

```
core/          Spring Boot 3.5 backend (Java 17, Gradle)
front/my-app/  React 19 + Vite 7 + TypeScript frontend (all 3 surfaces)
database/      Flyway SQL migrations (V0–V19+)
infra/         Nginx configs, TLS certs
ops/           CI/CD pipelines
docker-compose.yml
run-dev.ps1    Quick local start (Postgres + backend + frontend)
run-docker.ps1 Full Docker stack
```

---

## Running the Project

### Quickest local dev (PowerShell)
```powershell
.\run-dev.ps1
# Backend  → http://localhost:8080/api/v1
# Frontend → http://localhost:5173 (company dashboard by default)
# Demo:      super_admin@ziyarah.com / Demo123!
```

### Three-surface Docker stack (recommended)
```powershell
docker compose --profile portmode up -d --build
# http://localhost:7050  → Company dashboard  (VITE_APP_SURFACE=company)
# http://localhost:7060  → Provider portal    (VITE_APP_SURFACE=provider)
# http://localhost:7070  → Landing site       (VITE_APP_SURFACE=landing)
# http://localhost:8080  → API direct
```

Fresh database reset: `.\run-docker.ps1 -Fresh`

### Frontend only (from `front/my-app/`)
```bash
npm run dev          # company surface (default)
VITE_APP_SURFACE=landing npm run dev   # landing surface
npm run build        # tsc + vite build
npm run lint         # eslint
npm run test         # vitest (single run)
npm run test:watch   # vitest watch mode
```

### Backend only (from `core/`)
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
./gradlew test                          # unit + arch tests
./gradlew test -PrunDockerTests         # + Testcontainers (needs Docker)
./gradlew check                         # test + JaCoCo 60% gate
./gradlew test --tests "com.ziyara.backend.architecture.CleanArchitectureDddTest"
```

---

## Frontend Architecture

### Multi-surface compilation
A single codebase compiles to three independent SPAs controlled by `VITE_APP_SURFACE` at build time. The entry router in `src/main.tsx` reads `APP_SURFACE` from `src/config/appSurface.ts` and mounts the matching route tree:

| Surface | Entry routes file | Docker port |
|---|---|---|
| `company` | `src/apps/company/AppCompanyRoutes.tsx` | 7050 |
| `provider` | `src/apps/provider/AppProviderRoutes.tsx` | 7060 |
| `landing` | `src/apps/landing/AppLandingRoutes.tsx` | 7070 |

Pages shared between surfaces live in `src/pages/`. Surface-specific pages (landing shell, portal overview, etc.) live in `src/apps/<surface>/`.

### Design systems — two separate identities
- **Company + provider surfaces**: Tailwind CSS v4 with tokens `primary: #1e4d6b`, `secondary: #ac9e78`. Components under `src/components/` use `dark:` variants. All pages wrap in `PageLayout` → `MainLayout` (sidebar + header).
- **Landing surface**: custom `lp-*` CSS in `src/apps/landing/landing-public.css`. Warm glassmorphism palette (`--bg-warm: #f5f2ec`, `--accent-teal: #3d7080`, `--accent-tan: #b8966e`). Pages inside `LandingShell` get navbar + footer. Dark mode is explicitly disabled (`document.documentElement.classList.remove('dark')`).

When working on landing pages, use `lp-*` classes and CSS custom properties — never raw Tailwind slate/white classes.

### Key shared infrastructure
- **API client**: `src/services/api.ts` — single Axios instance (`/api/v1` proxied by Vite in dev). All API namespaces exported from this file (`authAPI`, `bookingsAPI`, `servicesAPI`, `providersAPI`, etc.).
- **Auth**: `src/context/AuthContext.tsx` — `user`, `setUser`, `logout`. JWT stored in `localStorage` under `ziyara_token`. `RequireAuth` and `RequireSurfaceRole` guard route trees.
- **i18n**: `src/context/LanguageContext.tsx` wraps `src/i18n/translations.ts`. Use `const { t } = useLanguage()` and `t('section.key')` everywhere. Both `en` and `ar` keys must be added together. The `ar` locale flips `dir="rtl"` on `<html>`.
- **Permissions**: `src/context/PermissionsContext.tsx` + `usePermission(code)` hook. Permission codes are strings like `'payments:read'`, `'providers:write'`. Super admin bypasses all checks.
- **Currency display**: `src/context/DisplayCurrencyContext.tsx` — `displayInDefault(amount, sourceCurrency)`.

### `Card` component surface prop
`<Card surface="landing">` renders the glassmorphism `lp-glass-card` variant. Omit `surface` for the default Tailwind card used on dashboard surfaces.

### GSAP animations (landing only)
`useLandingGSAP` and `useLandingMotion` in `src/apps/landing/` drive scroll-reveal. Elements with `.lp-animate` start `opacity: 0; transform: translate3d(0, 28px, 0)` and are revealed by GSAP ScrollTrigger. Add `prefers-reduced-motion` overrides in `landing-public.css` whenever adding new animated elements.

---

## Backend Architecture

Full details in `core/CLAUDE.md`. Summary of the rules that affect all changes:

**Clean Architecture layers** (enforced by ArchUnit — violations break the test suite):
```
presentation → application → domain ← infrastructure
```
- `domain.*` — pure Java, zero framework imports
- `application.*` — Spring `@Service`/`@Transactional`, no JPA entities, no HTTP types
- `infrastructure.*` — JPA, Spring Data, security, Kafka, Redis adapters
- `presentation.*` — `@RestController`, DTOs, `@PreAuthorize` on every method

**No JPA relationships** — `@OneToMany`/`@ManyToOne`/`@JoinColumn` are banned. Use explicit JPQL or jOOQ for multi-table fetches.

**Module API pattern** — cross-module calls go through interface boundaries in `modules/*/api/`. Inject `PaymentServiceApi`, never `PaymentService` directly.

**Naming** — domain entity: `User`; JPA entity: `UserJpa`; repository interface: `UserRepository`; adapter: `UserRepositoryAdapter`; Spring Data repo: `UserJpaRepository`.

---

## Database

Flyway migrations in `database/` (also applied automatically on backend startup via `core/src/main/resources/db/migration/`). Never edit existing migration files — add new `V{n+1}__description.sql`. Migration numbers currently reach V19+.

PostgreSQL 15 on Docker (`localhost:5432`, database `ziyarah`, user `ziyarah_user`). `POSTGRES_PASSWORD` must be set in `.env`.

---

## Adding a Translation Key

1. Add the key to the `en` object in `src/i18n/translations.ts`
2. Add the matching key to the `ar` object in the same file
3. Reference with `t('section.key')` — never hardcode user-visible strings

---

## Environment Variables (frontend)

| Variable | Purpose |
|---|---|
| `VITE_APP_SURFACE` | `company` \| `provider` \| `landing` |
| `VITE_API_URL` | Backend base URL (defaults to `/api/v1` via Vite proxy) |
| `VITE_COMPANY_APP_URL` | Cross-link URL for company dashboard |
| `VITE_PROVIDER_APP_URL` | Cross-link URL for provider portal |
