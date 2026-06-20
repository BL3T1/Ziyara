# CRITICAL ISSUES AND FIXES
> SRE Audit · Ziyara · 2026-06-17

---

## Prioritized Issue Table

| # | Layer | Issue Description | Root Cause | Urgency |
|---|---|---|---|---|
| 1 | Infrastructure | JVM OOM kill risk — heap ceiling too close to container limit | `-Xmx512m` leaves < 150 MB for metaspace, stacks, native memory inside a 768 MB container | **P0** |
| 2 | Infrastructure | Redis OOM kill destroys JWT blocklist — security regression | `maxmemory 100mb` / container limit `128m` = 28 MB OS headroom | **P0** |
| 3 | Mobile | Token refresh race condition silently logs users out | `_isRefreshing` boolean cannot queue concurrent 401s; second request propagates error while first succeeds | **P0** |
| 4 | Backend | HikariCP 60-second timeout causes user-facing hangs | `connection-timeout: 60000` ms in application.yml | **P1** |
| 5 | Infrastructure | Flyway `repair-on-migrate: true` can silently accept corrupted migrations in production | Flyway repair rewrites the checksum table instead of aborting | **P1** |
| 6 | Backend | Dashboard parallel query pool (max 4) fires 5 tasks | `DashboardExecutorConfig.setMaxPoolSize(4)` while `getKpis()` submits 5 `CompletableFuture` tasks | **P1** |
| 7 | Mobile | `Connectivity().checkConnectivity()` called on every HTTP request | Async OS syscall inside `_AuthInterceptor.onRequest` — no caching | **P1** |
| 8 | Frontend | All `node_modules` in a single vendor chunk — Recharts, Leaflet, sockjs coexist on every surface | No `manualChunks` in `vite.config.ts`; Vite defaults to one shared chunk for all deps | **P1** |
| 9 | Database | `shared_buffers` limited by 512 MB container cap | PostgreSQL default = 25% RAM → ~128 MB buffer pool. Scans beyond working set hit I/O | **P1** |
| 10 | Mobile | Google Fonts fetched at runtime from CDN | `google_fonts: ^6.3.2` default behavior; can fail in restricted-internet markets | **P2** |
| 11 | Frontend (CI) | npm packages redownloaded on every Docker build | Dockerfile lacks BuildKit `--mount=type=cache` for `node_modules` | **P2** |
| 12 | Infrastructure | No centralized log aggregation | `json-file` driver only; logs live inside containers and are lost on restart | **P2** |
| 13 | Database | Reporting materialized views disabled | `ZIYARA_REPORTING_MV_REFRESH_ENABLED=false`; analytics hits live tables on every query | **P2** |
| 14 | Infrastructure | No database connection pooler (PgBouncer) in front of PostgreSQL | HikariCP holds 10 real PostgreSQL connections directly; no request multiplexing | **P2** |

---

## P0 Detailed Fixes

### P0-1 — JVM OOM Kill Risk

**File:** `core/Dockerfile` (line 49) and `docker-compose.yml` (backend `deploy.resources.limits`)

**Problem:** The JVM is told `-Xmx512m` (heap only). At runtime, the full JVM process also allocates:
- Metaspace: 100–180 MB (grows with class loading, Hibernate proxies, Spring proxies)
- Thread stacks: ~50–100 MB at typical concurrency  
- JIT code cache, direct buffers, native libs: 50+ MB  

Total realistic memory: 750–850 MB against a hard container limit of 768 MB → OOM kill.

**Fix — two-part change:**

```diff
# core/Dockerfile — raise heap ceiling and set metaspace cap
- ENV JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseContainerSupport"
+ ENV JAVA_OPTS="-Xms256m -Xmx560m -XX:MaxMetaspaceSize=160m -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"
```

```diff
# docker-compose.yml — raise container limit to give the JVM room
  backend:
    deploy:
      resources:
        limits:
          cpus: "2.0"
-         memory: 768m
+         memory: 1024m
```

**Trade-off:** +256 MB RAM on the VPS. The `ExitOnOutOfMemoryError` flag causes Docker to restart the container cleanly on OOM rather than running in a degraded heap-exhausted state. The 160 MB metaspace cap prevents unbounded class-loader leaks.

---

### P0-2 — Redis OOM Kill Destroys JWT Blocklist

**File:** `docker-compose.yml` (redis service)

**Problem:** Redis is configured with `maxmemory 100mb` but the container limit is `128m`. The 28 MB headroom is consumed by:
- Redis process overhead (~10 MB)
- Allocator fragmentation (can be 10–30% overhead under heavy write workload)

When the container is killed by the OOM reaper, the JWT blocklist (revoked tokens), login rate-limit counters, and all permission caches are wiped. Any token that was revoked (logout, forced expiry, security block) becomes valid again until the backend re-populates caches.

**Fix:**

```diff
# docker-compose.yml — redis service
  redis:
    command: >
      redis-server
      --save 60 1
      --loglevel warning
-     --maxmemory 100mb
+     --maxmemory 200mb
      --maxmemory-policy allkeys-lru
    deploy:
      resources:
        limits:
          cpus: "0.25"
-         memory: 128m
+         memory: 256m
```

**Trade-off:** +128 MB RAM. The 200 MB maxmemory / 256 MB limit gives a 56 MB buffer (~28%) — sufficient headroom. `allkeys-lru` is still the right policy; it will evict permission caches first (they can be re-populated) while JWT blocklist keys have explicit TTLs that protect them from LRU eviction if they were recently accessed.

---

### P0-3 — Mobile Token Refresh Race Condition

**File:** `mobile/lib/core/api/api_client.dart` (`_AuthInterceptor.onError`)

**Problem:** If two API calls both receive a 401 simultaneously:
1. Request A: `_isRefreshing` is `false` → enters the refresh branch, sets `_isRefreshing = true`
2. Request B (arrives 1ms later): `_isRefreshing` is now `true` → falls into the `else` branch → calls `handler.next(err)` → propagates a 401 error to the caller even though Request A is about to successfully refresh the token

The user sees an auth error / logout toast for the second request even though the session is fine.

**Fix — replace the boolean flag with a completer queue:**

```dart
// mobile/lib/core/api/api_client.dart

class _AuthInterceptor extends Interceptor {
  final TokenStorageService _storage;
  final Dio _dio;

  // Queue of completers waiting for an in-progress refresh to finish
  Completer<String?>? _refreshCompleter;

  _AuthInterceptor(this._storage, this._dio);

  @override
  Future<void> onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    // ... (connectivity check and public path check unchanged)
    final token = await _storage.getAccessToken();
    if (token != null) options.headers['Authorization'] = 'Bearer $token';
    handler.next(options);
  }

  @override
  Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
    // Structured backend error parsing (unchanged)...

    if (err.response?.statusCode != 401) {
      handler.next(err);
      return;
    }

    // If a refresh is already in progress, wait for its result
    if (_refreshCompleter != null && !_refreshCompleter!.isCompleted) {
      final newToken = await _refreshCompleter!.future;
      if (newToken != null) {
        final retryOptions = err.requestOptions;
        retryOptions.headers['Authorization'] = 'Bearer $newToken';
        handler.resolve(await _dio.fetch(retryOptions));
      } else {
        handler.next(err);
      }
      return;
    }

    // This request is first — start the refresh
    _refreshCompleter = Completer<String?>();
    try {
      final refreshToken = await _storage.getRefreshToken();
      if (refreshToken == null) {
        await _storage.clearAll();
        _refreshCompleter!.complete(null);
        handler.next(err);
        return;
      }
      final refreshResponse = await _dio.post(
        '/auth/refresh',
        data: {'refreshToken': refreshToken},
        options: Options(headers: {'Authorization': null}),
      );
      final newAccess = refreshResponse.data['accessToken'] as String?;
      final newRefresh = refreshResponse.data['refreshToken'] as String?;
      if (newAccess != null) await _storage.saveAccessToken(newAccess);
      if (newRefresh != null) await _storage.saveRefreshToken(newRefresh);

      _refreshCompleter!.complete(newAccess);

      if (newAccess != null) {
        final retryOptions = err.requestOptions;
        retryOptions.headers['Authorization'] = 'Bearer $newAccess';
        handler.resolve(await _dio.fetch(retryOptions));
      } else {
        await _storage.clearAll();
        handler.next(err);
      }
    } catch (_) {
      await _storage.clearAll();
      _refreshCompleter!.complete(null);
      handler.next(err);
    } finally {
      // Reset after a short delay so future 401s can trigger a fresh refresh
      Future.delayed(const Duration(seconds: 2), () => _refreshCompleter = null);
    }
  }
  // ...
}
```

**Trade-off:** Slightly more complex interceptor code. In exchange, all concurrent requests that get a 401 wait for a single refresh and then retry with the new token — no user-visible logout. The `Completer<String?>` approach is the Flutter idiom for this pattern (used by Dio's own documentation examples).

---

## P1 Fixes (no full diff — targeted changes)

### P1-4 — HikariCP 60-Second Timeout

**File:** `core/src/main/resources/application.yml`

```diff
  hikari:
    maximum-pool-size: ${SPRING_DATASOURCE_HIKARI_MAX_POOL_SIZE:10}
    minimum-idle: ${SPRING_DATASOURCE_HIKARI_MIN_IDLE:2}
    idle-timeout: 300000
-   connection-timeout: 60000
+   connection-timeout: 8000
    max-lifetime: 1800000
```

Fail fast at 8 s. Users get a 503 immediately rather than hanging for a full minute. This also surfaces pool starvation earlier for monitoring.

### P1-5 — Flyway `repair-on-migrate` in Production

**File:** `core/src/main/resources/application.yml`

```diff
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
-   repair-on-migrate: true
+   repair-on-migrate: false
    out-of-order: false
    mixed: false
```

With `repair-on-migrate: false`, if a deployed migration file is later edited, Flyway will abort startup with a checksum mismatch error. This is the correct behavior — it forces the engineer to explicitly run `flyway repair` with eyes open rather than silently accepting a potentially corrupted migration history.

### P1-6 — Dashboard Executor Pool: 5 Tasks, Max 4 Threads

**File:** `core/src/main/java/com/ziyara/backend/infrastructure/config/DashboardExecutorConfig.java`

```diff
  ex.setCorePoolSize(2);
- ex.setMaxPoolSize(4);
+ ex.setMaxPoolSize(6);
  ex.setQueueCapacity(50);
```

The 5 parallel KPI queries (revenue, bookings×2, providers, tickets, complaints) each need a thread simultaneously. Setting max to 6 gives one spare for concurrent dashboard loads.

### P1-7 — Mobile Per-Request Connectivity Check

**File:** `mobile/lib/core/api/api_client.dart`

Remove the connectivity check from `onRequest`. Dio will throw `DioExceptionType.connectionError` when the OS has no route, which the BLoC error handlers already catch. The explicit check is redundant, adds latency, and uses the radio unnecessarily.

```diff
  @override
  Future<void> onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
-   final connectivity = await Connectivity().checkConnectivity();
-   if (connectivity.contains(ConnectivityResult.none) || connectivity.isEmpty) {
-     handler.reject(...);
-     return;
-   }
    if (_isPublicPath(options.path)) { handler.next(options); return; }
    final token = await _storage.getAccessToken();
    if (token != null) options.headers['Authorization'] = 'Bearer $token';
    handler.next(options);
  }
```

Show an offline banner at the app level via a stream listener on `Connectivity().onConnectivityChanged` instead.

### P1-8 — Vendor Chunk Splitting (Frontend)

**File:** `front/my-app/vite.config.ts`

```diff
  export default defineConfig({
-   plugins: [react(), tailwindcss()],
+   plugins: [react(), tailwindcss()],
+   build: {
+     rollupOptions: {
+       output: {
+         manualChunks: {
+           'vendor-react':    ['react', 'react-dom', 'react-router-dom'],
+           'vendor-query':    ['@tanstack/react-query'],
+           'vendor-charts':   ['recharts'],
+           'vendor-map':      ['leaflet', 'react-leaflet'],
+           'vendor-stomp':    ['@stomp/stompjs', 'sockjs-client'],
+           'vendor-pdf':      ['jspdf'],
+           'vendor-gsap':     ['gsap'],
+           'vendor-ui':       ['lucide-react', 'zod', 'axios'],
+         },
+       },
+     },
+   },
    server: { ... }
  })
```

The landing surface only loads `vendor-react`, `vendor-gsap`, `vendor-pdf`, and its own lazy route chunks. The company surface loads `vendor-charts`, `vendor-stomp`, `vendor-query`. Each surface only fetches what it uses, cached in the browser separately. Users on the landing page never download Recharts (~90 KB gz).

**Trade-off:** More HTTP round-trips on first load (more chunk files), but subsequent navigations and repeat visits are faster due to granular cache invalidation (only the changed chunk is re-fetched on deploy).
