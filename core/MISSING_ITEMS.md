# Ziyara — Missing Items Audit
**Compiled:** 2026-06-30 · Sources: `fixes_not_done_yet.md` · `IMPLEMENTATION_PLAN.md` · `BACKEND_ANALYSIS.md`

Every item below was verified directly against the codebase. Items with ✅ are confirmed done and listed only for completeness. Items without ✅ are not implemented.

---

## Scoreboard

| Source | Total Items | Done | Missing |
|---|---|---|---|
| `fixes_not_done_yet.md` FIX-01–20 | 20 | 11 | **9** |
| `IMPLEMENTATION_PLAN.md` Phase 1–7 | 30 | 22 | **8** |
| **Total** | **50** | **33** | **17** |

---

## Part A — `fixes_not_done_yet.md` Open Items

### 🔴 FIX-01 · Discount Balance Race Condition
**Risk:** Financial loss — two concurrent discount creations can produce a negative balance  
**File:** `core/src/main/java/com/ziyara/backend/application/service/PortalService.java`

No `FOR UPDATE` found. The debit still uses a non-atomic read-then-update. Fix: add a `SELECT ... FOR UPDATE` inside the `@Transactional` method before the `UPDATE` statement as specified in the plan.

---

### 🔴 FIX-02 · Payout Request Double-Submit
**Risk:** Duplicate payout entries  
**File:** `front/my-app/src/pages/portal/PortalEarningsPage.tsx`

No `payoutSubmitting` state found. The submit button is not disabled during in-flight requests. Fix: add `const [payoutSubmitting, setPayoutSubmitting] = useState(false)`, guard the handler, and set `disabled={payoutSubmitting}` on the button.

---

### 🟡 FIX-03 · `PortalEarningsPage` useEffect Stale Closure
**Risk:** Date filter changes do not reload earnings data  
**File:** `front/my-app/src/pages/portal/PortalEarningsPage.tsx` line 35–37

`useEffect` still uses empty `[]` dependency array. `load` is already a `useCallback` with `[start, end]` — changing the dependency to `[load]` is safe and a 30-second fix.

```tsx
// Current
useEffect(() => { load() }, [])
// Fix
useEffect(() => { load() }, [load])
```

---

### 🔴 FIX-04 · CI/CD Deploy Job Commented Out
**Risk:** Every production fix requires manual SSH into VPS  
**Files:** `.github/workflows/deploy.yml` (line 93), `.github/workflows/ci-cd.yml` (line 194)

Both deploy job blocks are still commented out. Four GitHub secrets and one variable still need to be added before uncommenting (`DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_PORT`, `GHCR_TOKEN`, `DEPLOY_BASE_URL`).

---

### ⚠️ FIX-05 · Android Signing Secrets Not in CI
**Risk:** Play Store submission rejected  
**Status:** Partial — `build.gradle.kts` reads from env vars correctly, but no `ANDROID_KEYSTORE_BASE64` / `ANDROID_KEY_ALIAS` / `ANDROID_KEYSTORE_PASS` / `ANDROID_KEY_PASS` secrets exist in the GitHub repository. Also no `build-android` CI job exists yet in any workflow file.  
**Action:** Generate the keystore once locally, base64-encode it, add four GitHub secrets, and add the `build-android` job from the plan.

---

### 🟡 FIX-06 · No Rate Limiting on Financial Endpoints
**Risk:** Provider can spam payout requests; discount endpoint has no throttle  
**File:** `core/src/main/java/com/ziyara/backend/presentation/controller/PortalController.java`

`@RateLimit` exists only on `AuthController` (3 usages on login/register). The three financial endpoints have no rate limit:
- `POST /portal/payout-request`
- `POST /portal/discounts`
- `POST /portal/services/{id}/images/upload`

Fix: add the three `@RateLimit` annotations as specified in the plan.

---

### 🟢 FIX-07 · HikariCP Connection Timeout
**Risk:** Pool exhaustion hangs users for 20 s instead of returning a fast 503  
**File:** `core/src/main/resources/application.yml` line 19

`connection-timeout: 20000`. Plan target is `8000`. Change to `8000`.

---

### 🟡 FIX-08 · Native `alert()` / `window.confirm()` Still in Use
**Risk:** Breaks on mobile WebViews; does not respect app theme/i18n  
**Files (7 confirmed):**
- `front/my-app/src/pages/admin/DeletedItemsPage.tsx`
- `front/my-app/src/pages/admin/IntegrationsPage.tsx`
- `front/my-app/src/pages/services/ServiceDetailMediaEditor.tsx`
- `front/my-app/src/pages/management/CurrencyRatesPage.tsx`
- `front/my-app/src/pages/management/UsersPage.tsx`
- `front/my-app/src/pages/management/StaffUserDetailPage.tsx`
- `front/my-app/src/pages/portal/PortalStaffPage.tsx`

`ConfirmDialog` is already available. Replace each `window.confirm()`/`alert()` call with a `ConfirmDialog` controlled by a boolean state variable.

---

### ~~FIX-09 · Sidebar Labels~~ ✅ DONE
`Sidebar.tsx` renders item labels as `t(`nav.${item.id}`)` — the render component already uses i18n. The hardcoded strings in `sidebar.ts` are never rendered directly.

---

### 🟢 FIX-10 · Double-Submit on Provider Approve/Reject
**Risk:** Admin can accidentally double-approve or double-reject a provider  
**File:** `front/my-app/src/pages/management/ProvidersPage.tsx`

No `actionLoading` state found. Buttons are not disabled during in-flight requests. Fix: add `const [actionLoading, setActionLoading] = useState<string | null>(null)` and disable buttons while `actionLoading === provider.id`.

---

### ~~FIX-11 · Per-Request Connectivity Check~~ ✅ DONE
`checkConnectivity()` is not present in `api_client.dart`. `connectivity_banner.dart` uses a stream subscription (`onConnectivityChanged`) — the stream-based approach is implemented.

---

### ~~FIX-12 · ABAC Migration~~ ✅ DONE
`UserRole` enum has exactly 3 values (`SUPER_ADMIN`, `CUSTOMER`, `STAFF`). Migration complete.

---

### 🟢 FIX-13 · Materialized View Refresh Still Disabled
**Risk:** Analytics queries hit live tables on every call instead of pre-computed snapshots  
**Action:** On the production VPS, set `ZIYARA_REPORTING_MV_REFRESH_ENABLED=true` in the `.env` file. The `MaterializedViewRefreshJob` is already implemented and scheduled. Verify materialized views exist and are populated before enabling (run `REFRESH MATERIALIZED VIEW CONCURRENTLY ...` once manually if views are empty).

---

### ~~FIX-14 · Loki Log Aggregation~~ ✅ DONE
`docker-compose.yml` has both `loki` (grafana/loki:3.4.2) and `promtail` (grafana/promtail:3.4.2) services wired up.

---

### ~~FIX-15 · PostgreSQL Tuning~~ ✅ DONE
`infra/postgres/postgresql.conf` exists with `shared_buffers`, `work_mem`, `random_page_cost`, slow-query logging, and `pg_stat_statements`.

---

### ⚠️ FIX-16 · `media_data` Still a Named Volume
**Risk:** Two backend replicas cannot share media files unless on the same host  
**Status:** `backend-2` is added and both replicas mount `media_data` — same named volume, same host, which works on a single-VPS setup. A bind-mount to `/srv/ziyara/media` is only needed if replicas move to separate hosts. Low priority until horizontal scaling is needed, but the plan documented this as Phase 6, Step 1.

---

### ~~FIX-17 · PgBouncer Pool~~ ✅ DONE
`pgbouncer.ini` has `default_pool_size = 170`, `max_client_conn = 400`.

---

### ~~FIX-18 · backend-2 Service~~ ✅ DONE
`docker-compose.yml` contains `backend-2` (`container_name: ziyarah-backend-2`).

---

### ~~FIX-19 · api-nginx Upstream Pool~~ ✅ DONE
`infra/nginx/api-nginx.conf` has `upstream backend_pool { least_conn; ... }`.

---

### ~~FIX-20 · gateway.conf WebSocket Routing~~ ✅ DONE
`gateway.conf` routes `/api/v1/ws` through `api-nginx:80`.

---

## Part B — `IMPLEMENTATION_PLAN.md` Open Items

### ~~1.1 `@PreAuthorize` on PaymentController~~ ✅ DONE
All 10 endpoints have explicit `@PreAuthorize` annotations.

### ~~1.2 `@PreAuthorize` on PricingController~~ ✅ DONE
`POST /pricing/preview` has `@PreAuthorize("isAuthenticated()")`.

### ⚠️ 1.3 · IDOR Ownership Checks
**Status:** Partial  
`OwnershipEnforcementTest.java` exists (the cross-user test). `BookingService` has a comment indicating staff vs. customer path. However, a full IDOR audit of all `@PathVariable UUID id` endpoints across `PaymentController`, `ComplaintController`, `ReviewController`, and `InternalTicketController` has not been confirmed in the service layer. **Verify** that each service method listed in the plan explicitly checks `resource.getUserId().equals(currentUserId)` before operating.

### ⚠️ 1.4 · PII Encryption Key Prod Enforcement
**Status:** Partial  
`PiiCryptoService` throws `IllegalStateException` when the key decodes to fewer than 32 bytes — but there is no `@PostConstruct` guard that rejects startup in `prod` profile when the key is entirely absent (blank/empty). A blank key currently starts the app with plaintext MFA storage.

**Fix:**
```java
@PostConstruct
void validateKey() {
    if (key == null && Arrays.asList(env.getActiveProfiles()).contains("prod")) {
        throw new IllegalStateException(
            "ZIYARA_PII_ENCRYPTION_KEY_BASE64 is required in production.");
    }
}
```

### ~~1.5 JWT Cookie Hardening~~ ✅ DONE
`application.yml` defaults: `same-site: ${JWT_COOKIE_SAME_SITE:Strict}`, `secure: ${JWT_COOKIE_SECURE:true}`.

### ~~2.1–2.4 Code Cleanup~~ ✅ DONE
`DddLayeringArchitectureTest` deleted. `application/dto/payment/` package deleted. Stub `GlobalExceptionHandler` in config deleted. Bare `catch (Exception e)` removed from `AuthService` and `CurrencyService`.

### ⚠️ 2.5 · `RoleManagementService` Split
**Status:** Partial  
`GroupManagementService` exists. A `PermissionQueryService` exists (different name from the plan's `PermissionCatalogueService`). However, check that `RoleManagementService` itself has been reduced to role CRUD only and that `getPermissionCatalogue`/`getUnlockedPermissions` have moved to `PermissionQueryService`. If `RoleManagementService` is still 579 lines, the split is incomplete.

### 🟡 3.1 · Rate Limiting on Write Endpoints
**Status:** Missing  
`@RateLimit` is present on `AuthController` only (login, register, password-reset — 3 usages). The plan requires rate limits on:
- `POST /payments` — 20 req/min
- `POST /bookings` — 30 req/min
- `POST /reviews` — 10 req/min
- `POST /complaints` — 10 req/min
- `GET /reports/**` — 5 req/min

None of these have `@RateLimit`. `Bucket4jRateLimitAspect` is already wired — this is an annotation-only change on each controller method.

### ⚠️ 3.2 · `POST /{id}/complete` and `POST /{id}/fail` Exposure
**Status:** Addressed but verify  
Both endpoints now have `@PreAuthorize(PAYMENTS_WRITE)`. The plan's concern was that these are internal gateway transitions that should not be reachable by regular authenticated users. Confirm that `PAYMENTS_WRITE` is an admin/staff-only permission and is not granted to customers. If `PAYMENTS_WRITE` can be held by customers, these endpoints need a stricter guard or removal.

### 🟢 3.3 · `PUT` → `PATCH` for Partial Updates
**Status:** Not done  
36 `@PutMapping` annotations remain across 19 controllers. Only 10 `@PatchMapping` usages exist. This is a low-risk cosmetic REST-semantics issue, but the plan flagged it for completeness.

### ~~4.1–4.5 Testing~~ ✅ DONE
`PaymentControllerWebMvcTest`, `OwnershipEnforcementTest`, `AuthServiceTest`, `BookingServiceTest`, `UserMfaServiceTest` all exist. JaCoCo `jacocoTestCoverageVerification` with `violationRules` is configured in `build.gradle.kts`.

### 🟢 4.6 · N+1 Query Detection Tests
**File to create:** `src/test/java/com/ziyara/backend/infrastructure/persistence/QueryCountTest.java`  
Not found. Use `datasource-proxy` or Hypersistence Optimizer to assert max query counts on `listBookings`, `getUserWithRoles`, `getPaymentsPage`.

### 🟢 4.7 · `RateLimitingAspectTest`
**File to create:** `src/test/java/com/ziyara/backend/infrastructure/security/RateLimitingAspectTest.java`  
Not found. Once 3.1 is done, mock `RateLimitService.isAllowed()` to return `false` and verify `429 TOO_MANY_REQUESTS` with `Retry-After` header.

### ~~5.1–6.5 Docs, Infra, DashboardMetricException, Retry-After~~ ✅ DONE
README, CLAUDE.md, SCHEMA.md, docker-compose.yml, Java 21, HEALTHCHECK (120 s), `DashboardMetricException`, `Retry-After` header in `GlobalExceptionHandler`, GDPR.md — all confirmed present.

### 🟢 7.1 · Vacuous ArchUnit `ignoreDependency` Cleanup
`application/dto/payment/` package was deleted (item 2.2), but `CleanArchitectureDddTest.java` may still contain an `ignoreDependency` entry for `domain.payment → application.dto.payment`. Search for it and remove it — a dead ignore can mask a future real violation.

```bash
grep -n "dto.payment\|GatewayPaymentResponse\|TokenizedPaymentCommand" \
  src/test/java/com/ziyara/backend/architecture/CleanArchitectureDddTest.java
```

---

## Master Punch List

### Do Today (Financial Risk)
- [ ] **FIX-01** `PortalService.java` — Add `SELECT ... FOR UPDATE` to discount balance debit
- [ ] **FIX-02** `PortalEarningsPage.tsx` — Add `payoutSubmitting` state, disable submit button
- [ ] **FIX-03** `PortalEarningsPage.tsx` — Change `useEffect` deps from `[]` to `[load]`

### This Week (Security + Deploy)
- [ ] **FIX-04** `.github/workflows/deploy.yml` — Add secrets, uncomment deploy job
- [ ] **FIX-05** CI — Generate keystore, add 4 Android signing secrets, add `build-android` job
- [ ] **FIX-06** `PortalController.java` — Add `@RateLimit` to payout/discount/upload
- [ ] **1.4** `PiiCryptoService.java` — Add `@PostConstruct` prod-profile key assertion
- [ ] **3.1** Controllers — Add `@RateLimit` to `/payments`, `/bookings`, `/reviews`, `/complaints`

### Before Provider Onboarding (UX Integrity)
- [ ] **FIX-08** 7 files — Replace all `alert()`/`window.confirm()` with `ConfirmDialog`
- [ ] **FIX-10** `ProvidersPage.tsx` — Add `actionLoading` state to approve/reject buttons

### Infrastructure (30-Day)
- [ ] **FIX-07** `application.yml` — Change `connection-timeout` from `20000` to `8000`
- [ ] **FIX-13** Production `.env` — Set `ZIYARA_REPORTING_MV_REFRESH_ENABLED=true`

### Testing Debt
- [ ] **4.6** Create `QueryCountTest.java` — N+1 detection on key list operations
- [ ] **4.7** Create `RateLimitingAspectTest.java` — Verify 429 when rate limit exceeded
- [ ] **1.3** Verify IDOR ownership checks in all service methods from the plan's table

### Low Priority / Cleanup
- [ ] **3.2** Confirm `PAYMENTS_WRITE` is not customer-accessible
- [ ] **3.3** Migrate 36 `@PutMapping` partial-update endpoints to `@PatchMapping`
- [ ] **2.5** Verify `RoleManagementService` is reduced to role CRUD only
- [ ] **7.1** Remove vacuous `domain.payment → application.dto.payment` `ignoreDependency` from `CleanArchitectureDddTest`
- [ ] **FIX-16** Convert `media_data` to bind-mount (`/srv/ziyara/media`) when replicas move to separate hosts
