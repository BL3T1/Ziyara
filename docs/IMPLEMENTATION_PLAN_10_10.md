# Implementation Plan — Ziyara 10 / 10
**Date:** 2026-05-22 | **Baseline:** 8.4 / 10 (backend) + 4.5 / 10 (mobile)
**Target:** Every category ≥ 9.0 / 10 across both surfaces

---

## Gap Analysis — What Prevents 10 / 10

### Backend (currently 8.4 / 10)

| Gap | Current Score Impact | Category |
|---|---|---|
| Unit/integration test depth unknown | −3.5 | Test Coverage (6.5) |
| CD deploy step is a scaffold (TODO) | −3.0 | CI/CD (7.0) |
| OTLP collector not configured | −2.5 | Observability (7.5) |
| 3 controllers still import domain repos directly | −0.5 | Architecture (9.5) |
| `pay_exchange_rates` unique constraint unverified | −0.5 | Database (9.5) |
| `@EnableMethodSecurity` for STOMP unverified | −0.5 | Security (8.5) |
| Redis AOF persistence not configured | −0.5 | Infrastructure (8.0) |

### Mobile (currently 4.5 / 10)

| Gap | Severity |
|---|---|
| 4 feature domains use fake repositories (Hotels, Restaurants, Tours, Transport) | Critical |
| Live driver tracking has no STOMP client and no real map | Critical |
| Logout doesn't clear tokens or call backend | Critical |
| Biometric login bypasses real auth when no token exists | Critical |
| 5 API endpoint paths don't match backend routes | Critical |
| No TOTP/MFA challenge screen | High |
| No push notifications (FCM/APNs) | High |
| GetIt DI declared but never wired | High |
| Exchange rate bar hardcoded | Medium |
| No structured error parsing | Medium |
| Android package name is `com.example.*` | Medium |
| CI APK build URL hardcoded | Medium |
| No connectivity check (package unused) | Low |

---

## Phase 1 — Backend: Remaining DDD Violations (4–6 h)

**Scope:** `UserController`, `ServiceController`, `InternalTicketController` still import
domain repos directly — the same pattern fixed for `BookingController` in v1.0.0.
Fix all three with the same approach: expand service interfaces, move logic, controller
becomes a one-line delegator.

### 1.1 `UserServiceApi.java` — expand interface

Add methods currently handled in `UserController`:
```java
UserResponse getCurrentUser(UUID userId);
UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
void changePassword(UUID userId, ChangePasswordRequest request);
Page<UserResponse> listUsers(UserRole role, int page, int size);   // admin
UserResponse getUser(UUID userId);                                  // admin
void deactivateUser(UUID userId);                                   // admin
```

### 1.2 `UserService.java` — implement all new methods

Move the logic from `UserController`:
- `getCurrentUser` → delegates to `UserRepository.findById()`
- `changePassword` → `PasswordEncoder.matches()` check → `passwordEncoder.encode()` → save
  → increment token version (invalidates old JWTs)
- `listUsers` / `deactivateUser` → admin-only queries

### 1.3 `UserController.java` — strip all direct repo injections

After: only `UserServiceApi` + `JwtService` as injections.

### 1.4 Repeat pattern for `ServiceController` and `InternalTicketController`

Same approach — expand respective service interfaces, implement logic in services,
controller becomes a thin delegation layer.

### 1.5 Verify `DddLayeringArchitectureTest.java`

All 6 arch tests must remain green after the refactor. If a stricter "presentation may
not import persistence" rule is now safe to add, add it as test #7.

**Verification:**
```bash
./gradlew test --tests "*.DddLayeringArchitectureTest"
# → 6+ green
```

---

## Phase 2 — Backend: Database Constraint Verification (30 min)

### 2.1 Verify `pay_exchange_rates` unique constraint

`FxRateRefreshJob` uses:
```sql
ON CONFLICT (from_currency, to_currency, effective_date) DO UPDATE SET rate = EXCLUDED.rate
```

Open `core/src/main/resources/db/migration/V17__*.sql` and confirm this constraint exists:
```sql
CONSTRAINT uq_pay_exchange_rates UNIQUE (from_currency, to_currency, effective_date)
```

If it does not exist, create `V18__add_exchange_rate_unique_constraint.sql`:
```sql
ALTER TABLE pay_exchange_rates
    ADD CONSTRAINT uq_pay_exchange_rates UNIQUE (from_currency, to_currency, effective_date);
```

### 2.2 Verify `@EnableMethodSecurity` is active for STOMP `@PreAuthorize`

`TaxiTrackingController` uses `@PreAuthorize("isAuthenticated()")` and
`@PreAuthorize("hasRole('SUPER_ADMIN')")` on `@MessageMapping` methods.
Spring Security's method security must be explicitly enabled for these to fire on
STOMP messages (it is separate from HTTP method security).

Search `ZiyarahApplication.java` and all `@Configuration` classes for:
```java
@EnableMethodSecurity   // Spring Security 6.x
// or
@EnableGlobalMethodSecurity(prePostEnabled = true)  // older style
```

If absent, add to the main security config:
```java
@EnableMethodSecurity(prePostEnabled = true)
```

---

## Phase 3 — Backend: Test Coverage (ongoing, target ≥ 8.5 / 10)

### 3.1 Unit tests — `BookingService`

```java
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository bookingRepository;
    @Mock ServiceRepository serviceRepository;
    @Mock PricingEngineApi pricingService;
    @InjectMocks BookingService bookingService;

    @Test void createBooking_serviceNotFound_throws() { ... }
    @Test void createBooking_conflictingDates_throws() { ... }
    @Test void createBooking_happyPath_savesAndReturnsResponse() { ... }
    @Test void cancelBooking_notOwner_throwsUnauthorized() { ... }
    @Test void confirmBooking_alreadyConfirmed_throws() { ... }
}
```

### 3.2 Unit tests — `AuthService`

```java
@Test void authenticate_validCredentials_returnsTokenPair() { ... }
@Test void authenticate_wrongPassword_throwsAuthenticationException() { ... }
@Test void authenticate_mfaRequiredRoleNotEnrolled_throwsMfaEnrollmentRequired() { ... }
@Test void refreshToken_validToken_returnsNewPair() { ... }
@Test void refreshToken_blocklisted_throws() { ... }
@Test void refreshToken_tokenVersionMismatch_throws() { ... }
```

### 3.3 Unit tests — `FxRateRefreshJob`

```java
@ExtendWith(MockitoExtension.class)
class FxRateRefreshJobTest {
    @Mock ExchangeRateRepository exchangeRateRepository;
    @Mock RestTemplate restTemplate;

    @Test void refresh_successfulApiResponse_upsertsAllPairs() { ... }
    @Test void refresh_apiReturnsSuccessFalse_logsWarnAndReturns() { ... }
    @Test void refresh_networkException_logsErrorAndDoesNotThrow() { ... }
}
```

### 3.4 Integration tests — booking flow end-to-end

Using `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestContainers` (Postgres + Redis):
```java
@Test void fullBookingLifecycle_createConfirmCancel() {
    // 1. Register customer
    // 2. Login → get JWT
    // 3. Create booking → assert PENDING
    // 4. Admin confirms → assert CONFIRMED
    // 5. Customer cancels → assert CANCELLED
}
```

### 3.5 Integration tests — MFA flow

```java
@Test void login_mfaRequiredRoleNotEnrolled_returns403WithCode() { ... }
@Test void login_mfaEnrolled_requiresTotpCode() { ... }
```

**Target:** ≥ 80% line coverage on `application.service` and `domain` packages.

---

## Phase 4 — Backend: Observability (2 h)

### 4.1 Add OpenTelemetry Collector to `docker-compose.yml`

```yaml
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.97.0
    container_name: ziyara-otel
    restart: unless-stopped
    command: ["--config=/etc/otelcol/config.yaml"]
    volumes:
      - ./ops/otel-collector.yaml:/etc/otelcol/config.yaml:ro
    ports:
      - "4318:4318"   # OTLP HTTP receiver
      - "8888:8888"   # Collector metrics
    networks:
      - ziyarah-network
```

### 4.2 `ops/otel-collector.yaml` — create config file

```yaml
receivers:
  otlp:
    protocols:
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 5s

exporters:
  logging:
    verbosity: detailed
  # Uncomment and configure for Jaeger, Zipkin, or cloud provider:
  # jaeger:
  #   endpoint: jaeger:14250

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [logging]
```

### 4.3 `application-docker.yml` — point OTLP exporter at the collector

```yaml
management:
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
  tracing:
    sampling:
      probability: 0.1
```

### 4.4 Add Prometheus + Grafana (optional but recommended)

```yaml
  prometheus:
    image: prom/prometheus:v2.50.0
    volumes:
      - ./ops/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    ports: ["9090:9090"]
    networks: [ziyarah-network]

  grafana:
    image: grafana/grafana:10.3.0
    ports: ["3001:3000"]
    networks: [ziyarah-network]
```

`ops/prometheus.yml`:
```yaml
scrape_configs:
  - job_name: ziyara-backend
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['backend:8081']
```

---

## Phase 5 — Backend: Redis AOF Persistence (15 min)

In `docker-compose.yml`, change the Redis command:
```yaml
  redis:
    command: redis-server --appendonly yes --appendfsync everysec --loglevel warning
```

This guarantees the JWT blocklist survives a Redis restart without losing any
blocklisted JTI entries (important: a non-persisted Redis means revoked tokens become
valid again after a crash).

---

## Phase 6 — Backend: CD Pipeline Deploy Step (2–4 h)

Choose one hosting approach and fill in the TODO in `.github/workflows/deploy.yml`.

### Option A — SSH + Docker Compose (simplest, single VPS)

```yaml
  deploy:
    name: Deploy to server
    needs: build-push
    runs-on: ubuntu-latest
    environment: ${{ github.event.inputs.environment || 'staging' }}
    steps:
      - name: SSH deploy
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DEPLOY_HOST }}
          username: ${{ secrets.DEPLOY_USER }}
          key: ${{ secrets.DEPLOY_SSH_KEY }}
          script: |
            cd /srv/ziyara
            echo "BACKEND_IMAGE=ghcr.io/${{ github.repository }}/backend:${{ needs.build-push.outputs.backend-tag }}" \
              >> .env
            docker compose pull backend frontend
            docker compose up -d --no-deps backend frontend
            docker compose ps
```

**Required GitHub Secrets:**
- `DEPLOY_HOST` — server IP / hostname
- `DEPLOY_USER` — SSH user (e.g., `deploy`)
- `DEPLOY_SSH_KEY` — private key (the server's `~/.ssh/authorized_keys` holds the public key)

### Option B — Fly.io

```yaml
      - uses: superfly/flyctl-actions/setup-flyctl@master
      - run: flyctl deploy --image ghcr.io/${{ github.repository }}/backend:${{ needs.build-push.outputs.backend-tag }}
        env:
          FLY_API_TOKEN: ${{ secrets.FLY_API_TOKEN }}
```

### Option C — AWS ECS

```yaml
      - uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ops/ecs-task-definition.json
          service: ziyara-backend
          cluster: ziyara-cluster
          wait-for-service-stability: true
```

---

## Phase 7 — Mobile: Dependency Injection Setup (2 h)

**Every page currently instantiates `ApiClient()` inline — wire GetIt properly.**

### 7.1 `lib/core/di/injection_container.dart` — create service locator

```dart
import 'package:get_it/get_it.dart';
import '../api/api_client.dart';
import '../services/token_storage_service.dart';
import '../../features/auth/data/repositories/auth_repository_impl.dart';
import '../../features/auth/domain/repositories/auth_repository.dart';
// ... all other repos

final sl = GetIt.instance;

Future<void> initDependencies() async {
  // Core
  sl.registerLazySingleton<TokenStorageService>(() => const TokenStorageService());
  sl.registerLazySingleton<ApiClient>(() => ApiClient(tokenStorage: sl()));

  // Auth
  sl.registerLazySingleton<AuthRepository>(
    () => AuthRepositoryImpl(apiClient: sl(), tokenStorage: sl()));

  // Bookings
  sl.registerLazySingleton<BookingRepository>(
    () => BookingRepositoryImpl(apiClient: sl()));

  // Hotels, Restaurants, Tours, Transport
  sl.registerLazySingleton<HotelsRepository>(
    () => HotelsRepositoryImpl(apiClient: sl()));
  sl.registerLazySingleton<RestaurantsRepository>(
    () => RestaurantsRepositoryImpl(apiClient: sl()));
  sl.registerLazySingleton<ToursRepository>(
    () => ToursRepositoryImpl(apiClient: sl()));
  sl.registerLazySingleton<TransportRepository>(
    () => TransportRepositoryImpl(apiClient: sl()));

  // Payment, Profile
  sl.registerLazySingleton<PaymentRepository>(
    () => PaymentRepositoryImpl(apiClient: sl()));
  sl.registerLazySingleton<ProfileRepository>(
    () => ProfileRepositoryImpl(apiClient: sl()));
}
```

### 7.2 `lib/main.dart` — call `initDependencies()` before `runApp`

```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initDependencies();
  // determine initialLocation from secure storage (token present → /home)
  final token = await sl<TokenStorageService>().getAccessToken();
  runApp(ZiyaraApp(initialLocation: token != null ? '/home' : '/onboarding'));
}
```

### 7.3 All pages — use `sl<T>()` instead of `ApiClient()`

```dart
// Before
create: (context) => AuthBloc(
  repository: AuthRepositoryImpl(apiClient: ApiClient()),
)

// After
create: (context) => AuthBloc(repository: sl<AuthRepository>())
```

---

## Phase 8 — Mobile: Fix Auth Security Flaws (1 h)

### 8.1 Fix biometric login — require existing token

```dart
Future<void> _loginWithBiometrics() async {
  final token = await sl<TokenStorageService>().getAccessToken();
  if (token == null) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('يرجى تسجيل الدخول بكلمة المرور أولاً')),
      );
    }
    return;
  }
  if (await BiometricHelper.authenticate() && mounted) {
    context.go('/home');
  }
}
```

### 8.2 Fix logout — dispatch `LogoutRequested` from `AuthBloc`

In `ProfilePage` (and anywhere else with a logout button):
```dart
// Before
onPressed: () => context.go('/login')

// After
onPressed: () => context.read<AuthBloc>().add(LogoutRequested())
```

The `AuthBloc` must be provided above `HomePage` in the widget tree
(wrap `MaterialApp` or the root widget, not individual pages).

### 8.3 `AuthRepositoryImpl` — add `forgotPassword` and `resetPassword`

```dart
@override
Future<void> forgotPassword(String email) async {
  await apiClient.post('/auth/forgot-password', data: {'email': email});
}

@override
Future<void> resetPassword(String token, String newPassword) async {
  await apiClient.post('/auth/reset-password', data: {
    'token': token,
    'newPassword': newPassword,
  });
}
```

Wire these in `AuthRepository` interface, `AuthBloc` events/states, and the
`ForgotPasswordPage` / `ResetPasswordPage` UI (which currently have no API calls).

---

## Phase 9 — Mobile: Fix API URL Mismatches (30 min)

| File | Change |
|---|---|
| `profile_repository_impl.dart` | `'/profile'` → `'/users/me'` |
| `profile_repository_impl.dart` | `'/profile'` (PUT) → `'/users/me'` |
| `profile_repository_impl.dart` | `'/profile/verify'` → `'/users/me/verification'` |
| `payment_repository_impl.dart` | `'/payments/coupon/validate'` → `'/discount-codes/validate'` |
| `payment_repository_impl.dart` | `'/payments/process'` → restructure (see §9.1) |
| `app_constants.dart` | Delete stale `baseUrl` constant (unreferenced) |

### 9.1 Restructure payment flow to match backend

Backend flow:
1. `POST /bookings` → create booking → returns `bookingId`
2. `POST /payments` (or Stripe intent) → process payment with `bookingId`

Client flow must become:
```dart
@override
Future<PaymentModel> processPayment(String bookingId, double amount, File idImage) async {
  final formData = FormData.fromMap({
    'bookingId': bookingId,
    'amount': amount,
    'id_image': await MultipartFile.fromFile(idImage.path),
  });
  final response = await apiClient.post('/payments', data: formData);
  return PaymentModel.fromJson(response.data['data']);
}
```

The backend `ApiResponse` wrapper means response data is always at `response.data['data']`,
not `response.data` directly — fix all `fromJson(response.data)` calls to
`fromJson(response.data['data'])`.

---

## Phase 10 — Mobile: Wire Real Feature Repositories (3–4 h)

### 10.1 `HotelsRepositoryImpl` — replace fake

```dart
class HotelsRepositoryImpl implements HotelsRepository {
  final ApiClient apiClient;
  HotelsRepositoryImpl({required this.apiClient});

  @override
  Future<List<HotelModel>> getHotels() async {
    final response = await apiClient.get(
      '/services',
      queryParameters: {'type': 'HOTEL', 'page': 0, 'size': 20},
    );
    final list = response.data['data']['content'] as List;
    return list.map((e) => HotelModel.fromJson(e)).toList();
  }

  @override
  Future<HotelModel?> getHotelById(String id) async {
    final response = await apiClient.get('/services/$id');
    return HotelModel.fromJson(response.data['data']);
  }
}
```

Update `HotelModel.fromJson()` to map backend field names (camelCase, backend field names).

### 10.2 `RestaurantsRepositoryImpl` — same pattern

```dart
queryParameters: {'type': 'RESTAURANT', 'page': 0, 'size': 20}
```

### 10.3 `ToursRepositoryImpl` — same pattern

```dart
queryParameters: {'type': 'TOUR', 'page': 0, 'size': 20}
```

### 10.4 `TransportRepositoryImpl` — wire to real taxi endpoints

```dart
@override
Future<List<TransportModel>> getTransportTypes() async {
  final response = await apiClient.get('/taxi/vehicle-types');
  return (response.data['data'] as List)
      .map((e) => TransportModel.fromJson(e))
      .toList();
}

@override
Future<String> bookTransport(String bookingId, String vehicleType) async {
  final response = await apiClient.post('/taxi/bookings', data: {
    'bookingId': bookingId,
    'vehicleType': vehicleType,
  });
  return response.data['data']['id'] as String;
}
```

---

## Phase 11 — Mobile: Real-Time Tracking via STOMP WebSocket (3 h)

### 11.1 Add dependencies to `pubspec.yaml`

```yaml
dependencies:
  stomp_dart_client: ^3.1.0
  google_maps_flutter: ^2.7.0   # or flutter_map: ^6.1.0 (open-source alternative)
```

### 11.2 `TransportRepositoryImpl.trackDriver()` — real STOMP stream

```dart
@override
Stream<DriverModel> trackDriver(String taxiBookingId) async* {
  final controller = StreamController<DriverModel>();

  StompClient? client;
  client = StompClient(
    config: StompConfig(
      url: 'ws://${ApiClient.host}/ws/websocket',
      onConnect: (frame) {
        client!.subscribe(
          destination: '/topic/tracking/$taxiBookingId',
          callback: (frame) {
            if (frame.body != null) {
              final json = jsonDecode(frame.body!);
              controller.add(DriverModel.fromJson(json));
            }
          },
        );
      },
      beforeConnect: () async {
        // Attach JWT as STOMP header
        final token = await sl<TokenStorageService>().getAccessToken();
        client!.config.stompConnectHeaders['Authorization'] = 'Bearer $token';
      },
      onDisconnect: (_) => controller.close(),
      onStompError: (frame) => controller.addError(Exception(frame.body)),
      onWebSocketError: (e) => controller.addError(e),
    ),
  );
  client.activate();

  yield* controller.stream;
  client.deactivate();
}
```

### 11.3 `DriverTrackingPage` — real map + STOMP data

Replace the grey placeholder container with a `GoogleMap` widget:
```dart
GoogleMap(
  initialCameraPosition: CameraPosition(
    target: LatLng(driver.lat, driver.lng), zoom: 15),
  markers: {
    Marker(
      markerId: const MarkerId('driver'),
      position: LatLng(driver.lat, driver.lng),
      icon: driverMarkerIcon,
    ),
  },
)
```

Update the marker position on every `TransportTrackingUpdate` state.

### 11.4 Android — add Maps API key

`android/app/src/main/AndroidManifest.xml`:
```xml
<meta-data
  android:name="com.google.android.geo.API_KEY"
  android:value="${MAPS_API_KEY}"/>
```

`android/app/build.gradle.kts`:
```kotlin
buildConfigField("String", "MAPS_API_KEY", "\"${project.findProperty("MAPS_API_KEY")}\"")
```

---

## Phase 12 — Mobile: Push Notifications (FCM) (2–3 h)

### 12.1 Add dependencies

```yaml
dependencies:
  firebase_core: ^3.4.0
  firebase_messaging: ^15.1.0
  flutter_local_notifications: ^17.2.0
```

### 12.2 `lib/core/services/push_notification_service.dart`

```dart
class PushNotificationService {
  static Future<void> init() async {
    await Firebase.initializeApp();
    final messaging = FirebaseMessaging.instance;
    await messaging.requestPermission();
    
    final token = await messaging.getToken();
    if (token != null) {
      // Register token with backend
      await sl<ApiClient>().post('/users/me/push-token', data: {'token': token});
    }
    
    FirebaseMessaging.onMessage.listen((message) {
      // Show local notification when app is in foreground
      _showLocalNotification(message);
    });
    
    FirebaseMessaging.onMessageOpenedApp.listen((message) {
      // Navigate based on message.data['type'] (BOOKING_CONFIRMED, etc.)
    });
  }
}
```

### 12.3 `AndroidManifest.xml` — add FCM service

```xml
<service
  android:name="com.google.firebase.messaging.FirebaseMessagingService"
  android:exported="false">
  <intent-filter>
    <action android:name="com.google.firebase.MESSAGING_EVENT"/>
  </intent-filter>
</service>
```

### 12.4 Backend — add push token endpoint (`UserController`)

```java
@PutMapping("/users/me/push-token")
public ResponseEntity<ApiResponse<Void>> savePushToken(
    @RequestBody Map<String, String> body,
    Authentication auth) {
  userService.savePushToken(UUID.fromString(auth.getName()), body.get("token"));
  return ResponseEntity.ok(ApiResponse.success(null));
}
```

Add `push_token VARCHAR(512)` column to users table via `V18__add_push_token.sql`
(or V19 if V18 is used for the exchange rate constraint).

---

## Phase 13 — Mobile: Exchange Rate Live Fetch (1 h)

### 13.1 `ExchangeRateRepository` (Flutter)

```dart
abstract class ExchangeRateRepository {
  Future<Map<String, double>> getRates(String baseCurrency);
}

class ExchangeRateRepositoryImpl implements ExchangeRateRepository {
  final ApiClient apiClient;
  ExchangeRateRepositoryImpl({required this.apiClient});

  @override
  Future<Map<String, double>> getRates(String baseCurrency) async {
    final response = await apiClient.get(
      '/exchange-rates',
      queryParameters: {'base': baseCurrency},
    );
    final data = response.data['data'] as Map<String, dynamic>;
    return data.map((k, v) => MapEntry(k, (v as num).toDouble()));
  }
}
```

### 13.2 `ExchangeRateBloc` — fetch on first open

```dart
class ExchangeRateBloc extends Bloc<ExchangeRateEvent, ExchangeRateState> {
  ExchangeRateBloc({required this.repository}) : super(ExchangeRateInitial()) {
    on<FetchRates>(_onFetchRates);
  }
  Future<void> _onFetchRates(FetchRates event, Emitter<ExchangeRateState> emit) async {
    emit(ExchangeRateLoading());
    try {
      final rates = await repository.getRates(event.base);
      emit(ExchangeRateLoaded(rates));
    } catch (_) {
      emit(ExchangeRateError());
    }
  }
}
```

### 13.3 `ExchangeRateBar` — replace hardcoded string

```dart
BlocBuilder<ExchangeRateBloc, ExchangeRateState>(
  builder: (context, state) {
    final rate = state is ExchangeRateLoaded
        ? state.rates['SYP']?.toStringAsFixed(0) ?? '—'
        : '…';
    return Text('1 USD = $rate SYP', ...);
  },
)
```

---

## Phase 14 — Mobile: Error Handling & Connectivity (1 h)

### 14.1 `ApiClient` — parse structured backend errors

```dart
@override
Future<void> onError(DioException err, ErrorInterceptorHandler handler) async {
  // Try to extract backend error code
  final data = err.response?.data;
  if (data is Map && data['success'] == false) {
    final code = data['code'] as String? ?? 'UNKNOWN_ERROR';
    final message = data['message'] as String? ?? 'حدث خطأ غير متوقع';
    handler.reject(DioException(
      requestOptions: err.requestOptions,
      error: BackendException(code: code, message: message),
      response: err.response,
      type: err.type,
    ));
    return;
  }
  // ... existing 401 refresh logic
}
```

### 14.2 `BackendException` — new class

```dart
class BackendException implements Exception {
  final String code;
  final String message;
  const BackendException({required this.code, required this.message});

  /// Maps backend error codes to user-friendly Arabic strings
  String get userMessage => switch (code) {
    'INVALID_CREDENTIALS'       => 'البريد أو كلمة المرور غير صحيحة',
    'MFA_ENROLLMENT_REQUIRED'   => 'يجب تفعيل المصادقة الثنائية أولاً',
    'BOOKING_CONFLICT'          => 'هذا التاريخ محجوز مسبقاً',
    'DISCOUNT_CODE_INVALID'     => 'رمز الخصم غير صالح أو منتهي',
    'RESOURCE_NOT_FOUND'        => 'العنصر المطلوب غير موجود',
    _                           => message,
  };
}
```

### 14.3 `ConnectivityService` — use the already-declared package

```dart
class ConnectivityService {
  static Future<bool> hasConnection() async {
    final result = await Connectivity().checkConnectivity();
    return result != ConnectivityResult.none;
  }
}
```

Add a check in `ApiClient.onRequest`:
```dart
if (!await ConnectivityService.hasConnection()) {
  handler.reject(DioException(
    requestOptions: options,
    error: const BackendException(code: 'NO_INTERNET', message: 'لا يوجد اتصال بالإنترنت'),
    type: DioExceptionType.connectionError,
  ));
  return;
}
```

---

## Phase 15 — Mobile: Android / iOS Production Config (2 h)

### 15.1 Rename Android application ID

`android/app/build.gradle.kts`:
```kotlin
android {
  namespace = "com.ziyara.app"
  defaultConfig {
    applicationId = "com.ziyara.app"
    // ...
  }
}
```

`android/app/src/main/kotlin/` — rename package folder from
`com/example/syria_tourism_app/` → `com/ziyara/app/`

Update `MainActivity.kt` package declaration.

### 15.2 Add Network Security Config (Android)

`android/app/src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
  <debug-overrides>
    <trust-anchors>
      <certificates src="system"/>
    </trust-anchors>
    <!-- Allow cleartext only in debug builds (emulator) -->
    <base-config cleartextTrafficPermitted="true"/>
  </debug-overrides>
  <base-config cleartextTrafficPermitted="false">
    <trust-anchors>
      <certificates src="system"/>
    </trust-anchors>
  </base-config>
</network-security-config>
```

`AndroidManifest.xml`:
```xml
<application
  android:networkSecurityConfig="@xml/network_security_config"
  ...>
```

### 15.3 Add ProGuard rules

`android/app/proguard-rules.pro`:
```
-keep class com.ziyara.app.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
```

`android/app/build.gradle.kts`:
```kotlin
buildTypes {
  release {
    isMinifyEnabled = true
    isShrinkResources = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  }
}
```

### 15.4 iOS bundle identifier

In `ios/Runner.xcodeproj/project.pbxproj` change:
`PRODUCT_BUNDLE_IDENTIFIER = com.example.syriaTourismApp;`
→ `PRODUCT_BUNDLE_IDENTIFIER = com.ziyara.app;`

### 15.5 Fix CI APK URL

`.github/workflows/ci.yml` — in the Flutter mobile job, replace the hardcoded URL:
```yaml
- name: Build debug APK
  run: flutter build apk --debug --dart-define=ZIYARA_API_URL=${{ secrets.API_URL || 'http://10.0.2.2:8080/api/v1' }}
  working-directory: SYRIA-TOURISM-APP-main/SYRIA-TOURISM-APP-main
```

Add `API_URL` as a GitHub Actions repository variable (not secret, it can be the staging URL).

---

## Phase 16 — Mobile: TOTP / MFA Challenge Screen (2 h)

Required if any admin or privileged user logs in via mobile.

### 16.1 New page: `MfaChallengeScreen`

After login returns `MFA_REQUIRED` (HTTP 200 with `mfaRequired: true` in the response,
or catch a specific backend status), show:

```dart
class MfaChallengeScreen extends StatelessWidget {
  // 6-digit OTP input field (auto-submit on 6th digit)
  // "Verify" button → POST /auth/mfa/verify { code, tempToken }
  // On success → store tokens → navigate to /home
}
```

### 16.2 `AuthBloc` — add new events/states

```dart
// Events
class SubmitMfaCode extends AuthEvent { final String code; ... }

// States
class AuthMfaRequired extends AuthState { final String tempToken; ... }
class AuthMfaError extends AuthState { final String message; ... }
```

---

## Phase 17 — Mobile: Notifications Page (1 h)

### 17.1 `NotificationModel` + `NotificationsRepositoryImpl`

```dart
class NotificationsRepositoryImpl {
  Future<List<NotificationModel>> getNotifications() async {
    final response = await apiClient.get('/notifications');
    return (response.data['data']['content'] as List)
        .map((e) => NotificationModel.fromJson(e))
        .toList();
  }

  Future<void> markAsRead(String id) async {
    await apiClient.put('/notifications/$id/read');
  }
}
```

### 17.2 `NotificationsBloc` — wire to repository

Replace hardcoded list in `NotificationsPage` with BLoC state rendering.

---

## Execution Order & Effort Estimates

| Phase | Area | Effort | Priority |
|---|---|---|---|
| 1 | Backend: remaining DDD violations | 4–6 h | High |
| 2 | Backend: DB constraint + `@EnableMethodSecurity` | 30 min | High |
| 3 | Backend: unit + integration tests | ongoing | High |
| 4 | Backend: observability (OTel + Prometheus) | 2 h | Medium |
| 5 | Backend: Redis AOF | 15 min | Medium |
| 6 | Backend: CD deploy step | 2–4 h | High |
| 7 | Mobile: DI container (GetIt) | 2 h | High — do this first, all others depend on it |
| 8 | Mobile: auth security flaws | 1 h | Critical |
| 9 | Mobile: API URL mismatches | 30 min | Critical |
| 10 | Mobile: real feature repositories | 3–4 h | Critical |
| 11 | Mobile: STOMP WebSocket + real map | 3 h | High |
| 12 | Mobile: FCM push notifications | 2–3 h | High |
| 13 | Mobile: live exchange rate | 1 h | Medium |
| 14 | Mobile: error handling + connectivity | 1 h | High |
| 15 | Mobile: Android/iOS production config | 2 h | High |
| 16 | Mobile: TOTP challenge screen | 2 h | Medium |
| 17 | Mobile: notifications page | 1 h | Medium |

**Total estimated effort: ~31–40 hours**

---

## Recommended Execution Order

```
Week 1 — Critical path (mobile + backend blockers)
  Day 1: Phase 7 (GetIt DI) — foundation for all mobile phases
  Day 1: Phase 8 (auth security fixes)
  Day 2: Phase 9 + 10 (URL fixes + real repos for Hotels/Restaurants/Tours/Transport)
  Day 2: Phase 2 (backend DB constraint + EnableMethodSecurity)
  Day 3: Phase 11 (STOMP WebSocket + map)
  Day 3: Phase 15 (Android/iOS prod config)
  Day 4: Phase 1 (backend DDD: UserController + ServiceController + InternalTicketController)
  Day 5: Phase 6 (CD deploy step)

Week 2 — Hardening and polish
  Phase 3 (backend tests — BookingService, AuthService, FxRateRefreshJob)
  Phase 12 (FCM push notifications)
  Phase 14 (error handling + connectivity)
  Phase 4 (OTel collector + Prometheus + Grafana)
  Phase 5 (Redis AOF)
  Phase 13 (exchange rate live fetch)
  Phase 16 (TOTP challenge screen)
  Phase 17 (notifications page)
```

---

## Expected Scores After Completion

### Backend

| Category | Current | After |
|---|---|---|
| Architecture | 9.5 | **10.0** — all 3 remaining controllers DDD-clean |
| Security | 8.5 | **9.5** — STOMP method security confirmed, Redis AOF |
| Database | 9.5 | **10.0** — unique constraint verified/added |
| Infrastructure | 8.0 | **9.5** — OTel collector + Prometheus + Grafana live |
| API Surface | 9.0 | **9.5** — push token endpoint added |
| Configuration | 9.0 | **9.5** — CD fully deployed |
| Observability | 7.5 | **9.5** — full OTel pipeline + dashboard |
| CI/CD | 7.0 | **9.5** — CD deploy step implemented |
| Code Quality | 8.5 | **9.0** — consistent across all controllers |
| Test Coverage | 6.5 | **9.0** — BookingService, AuthService, FxJob tested; E2E flow |

**Backend projected: 9.5 / 10**

### Mobile

| Category | Current | After |
|---|---|---|
| Architecture (Flutter) | 7.5 | **9.5** — GetIt DI, proper BLoC scoping |
| API Client / Networking | 8.0 | **9.5** — structured error parsing, connectivity guard |
| Auth Integration | 5.0 | **9.5** — biometric fix, logout fix, forgot-password wired |
| Feature Integration | 2.0 | **9.5** — all 4 fake repos replaced |
| Real-Time Tracking | 1.0 | **9.0** — STOMP + real map |
| Payments | 4.0 | **9.0** — endpoint paths fixed, ApiResponse wrapper handled |
| Notifications | 0.5 | **8.5** — FCM + backend API |
| Exchange Rates | 1.0 | **9.5** — live backend fetch |
| Error Handling | 3.0 | **9.0** — BackendException + Arabic messages |
| CI / Build Config | 3.0 | **9.5** — package renamed, URL parametrised, ProGuard |

**Mobile projected: 9.3 / 10**

### Combined System

| Surface | Before | After |
|---|---|---|
| Backend | 8.4 / 10 | **9.5 / 10** |
| Mobile | 4.5 / 10 | **9.3 / 10** |
| **System** | **6.5 / 10** | **9.4 / 10** |

The remaining 0.6 points represent:
- Email / OTP / SMS (explicitly out of scope for this phase)
- App Store / Play Store review and submission
- Real Stripe integration (currently stub)
- Performance profiling and bundle size optimization
- Accessibility audit (screen reader, contrast ratios)
