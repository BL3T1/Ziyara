# Mobile Application — Integration State Report
**Date:** 2026-05-22 | **Flutter SDK:** ≥ 3.8.1 | **Rated against backend v1.0.0**

---

## Overall Mobile Rating: 4.5 / 10

The mobile app has an excellent foundation — Clean Architecture with BLoC, secure token
storage, a production-quality `ApiClient` with silent refresh, biometrics, PDF generation,
and multi-language support. However, **the majority of feature screens are powered by fake
in-memory repositories**, the live tracking screen has no real WebSocket connection, several
API paths do not match the backend, and two significant security flaws exist in the auth
flow. The app is not ready for production in its current state.

---

## Scores by Category

| Category | Score | Notes |
|---|---|---|
| Architecture (Flutter) | 7.5 / 10 | Clean BLoC + feature folders; DI container (GetIt) declared but never wired |
| API Client / Networking | 8.0 / 10 | Dio + interceptor + silent 401 refresh + secure storage — solid |
| Auth Integration | 5.0 / 10 | Login/register/logout wired; biometric bypass flaw; no TOTP screen |
| Feature Integration | 2.0 / 10 | 4 of 6 feature domains use fake repositories with no backend calls |
| Real-Time Tracking | 1.0 / 10 | STOMP dependency missing; driver page uses a fake timed stream |
| Payments | 4.0 / 10 | Client code exists but 2 endpoints don't match backend paths |
| Notifications | 0.5 / 10 | Page shows 3 hardcoded strings; no FCM/APNs; no backend call |
| Exchange Rates | 1.0 / 10 | Bar shows hardcoded "1 USD = 14,500"; no API fetch |
| Error Handling | 3.0 / 10 | `e.toString()` fallback everywhere; no structured error-code parsing |
| CI / Build Config | 3.0 / 10 | APK build URL hardcoded; package name still `com.example.*` |

---

## What IS Wired to the Real Backend

### ApiClient (`lib/core/api/api_client.dart`)
- Base URL driven by `--dart-define=ZIYARA_API_URL` at build time; defaults to
  `http://10.0.2.2:8080/api/v1` (Android emulator localhost alias) ✅
- `_AuthInterceptor` injects `Authorization: Bearer <access>` on every non-public request ✅
- On HTTP 401 → fetches refresh token from `flutter_secure_storage`, calls `POST /auth/refresh`,
  stores new pair, retries original request — full rotation cycle ✅
- Public path detection skips auth header for `/auth/login`, `/auth/register`,
  `/auth/refresh`, `/auth/forgot-password`, `/auth/reset-password` ✅

### TokenStorageService (`lib/core/services/token_storage_service.dart`)
- Uses `flutter_secure_storage` → EncryptedSharedPreferences on Android, Keychain on iOS ✅
- Separate keys for access and refresh tokens; `clearAll()` used on logout ✅

### Auth (`lib/features/auth/`)
- `POST /auth/login` → stores both tokens → returns `UserModel` ✅
- `POST /auth/register` ✅
- `POST /auth/logout` → backend JTI blocklist + local token wipe ✅
- Biometric trigger (`local_auth`) wired and checks device capability on init ✅

### Bookings (`lib/features/bookings/`)
- `GET /bookings/my` → renders active vs past tabs ✅
- `POST /bookings/{id}/cancel` ✅
- Booking card shows status, price, cancel option with refund dialog ✅

### Payments (`lib/features/payment/`)
- `POST /payments/coupon/validate` → applies discount (URL mismatch — see §Gaps) ⚠️
- `POST /payments/process` (multipart with ID image) ⚠️
- Person-count multiplier and coupon logic handled client-side in BLoC ✅

### Profile (`lib/features/profile/`)
- `GET /profile` → renders profile header, verification badge, wallet balance ⚠️ (URL mismatch)
- `PUT /profile` → profile update ⚠️ (URL mismatch)
- `POST /profile/verify` (multipart ID front + back) ⚠️ (URL mismatch)

---

## Critical Gaps — Not Wired / Not Working

### 1. Fake Repositories — 4 Feature Domains Have No Backend Connection

| Feature | Repository Used | Status |
|---|---|---|
| Hotels | `FakeHotelsRepository` | Returns `HotelModel.dummyHotels` after a `2s` delay |
| Restaurants | `FakeRestaurantsRepository` | Returns hardcoded `RestaurantModel` list |
| Tours | `FakeToursRepository` | Returns hardcoded `TourModel` list |
| Transport booking | `FakeTransportRepository` | Returns 3 hardcoded vehicle types; `bookTransport()` returns `"booking_123"` |

These need real repository implementations calling the backend service endpoints
(`GET /services?type=HOTEL`, `GET /services?type=RESTAURANT`, etc.) or hotel/restaurant-specific
endpoints if they exist.

---

### 2. Live Driver Tracking — STOMP WebSocket Missing

**Current state:**
```dart
// FakeTransportRepository.trackDriver() — no network call, just a countdown loop
Stream<DriverModel> trackDriver(String bookingId) async* {
  for (int i = 5; i >= 0; i--) {
    await Future.delayed(const Duration(seconds: 2));
    yield DriverModel(lat: 33.5138, lng: 36.2765, etaMinutes: i, ...);
  }
}
```
`DriverTrackingPage` hard-codes `FakeTransportRepository()` in its `BlocProvider.create` —
even if a real repository existed, it would never be used.

**Backend:**
- STOMP WebSocket at `ws://<host>/ws` (SockJS)
- Driver pushes to `STOMP /app/taxi/location/{bookingId}`
- Subscribers receive `TaxiLocationBroadcast` JSON on `/topic/tracking/{bookingId}`

**Missing in pubspec.yaml:** `stomp_dart_client` or `web_socket_channel` — neither package
is declared. Without it the real-time feature cannot be implemented.

**Also missing:** A real map widget (`google_maps_flutter` or `flutter_map`) — the current
`_buildMapBackground()` is a grey container with an icon.

---

### 3. API URL Mismatches

| Feature | App calls | Backend exposes | Fix |
|---|---|---|---|
| Profile fetch | `GET /profile` | `GET /users/me` | Change path |
| Profile update | `PUT /profile` | `PUT /users/me` | Change path |
| ID verification | `POST /profile/verify` | `POST /users/me/verification` (likely) | Change path |
| Coupon validation | `POST /payments/coupon/validate` | `POST /discount-codes/validate` | Change path |
| Payment process | `POST /payments/process` | Booking creation + payment is `POST /bookings` + `POST /payments` | Restructure |
| Legacy constant | `app_constants.dart` `baseUrl = 'https://api.ziyara-app.com/v1'` | Not used (ApiClient uses dart-define) | Delete or align |

---

### 4. Auth Security Flaws

#### 4a. Biometric Login Bypasses Backend
```dart
// LoginPage._loginWithBiometrics()
Future<void> _loginWithBiometrics() async {
  if (await BiometricHelper.authenticate() && mounted) {
    context.go('/home');  // ← navigates WITHOUT a valid JWT in storage
  }
}
```
If the user has no stored access token (e.g., first install), biometric "success" grants
access to the full app with no valid session. Any API call will immediately fail with 401.

**Fix:** Biometric should only be offered after a first successful password login (token
already stored). On subsequent opens, biometric re-authenticates locally — the JWT is
already in secure storage. The current `context.go('/home')` call is correct IF a token
is confirmed present. A guard is needed:
```dart
final token = await TokenStorageService().getAccessToken();
if (token == null) { /* force password login */ return; }
```

#### 4b. Logout Doesn't Clear Tokens
```dart
// ProfilePage
TextButton.icon(
  onPressed: () => context.go('/login'),  // ← no AuthBloc event dispatched
  ...
)
```
Navigating directly to `/login` without dispatching `LogoutRequested` means:
1. `POST /auth/logout` is never called → backend never blocklists the refresh token
2. `TokenStorageService.clearAll()` is never called → tokens remain in secure storage
3. The next user who opens the app (or the same user pressing back) can re-enter
   the app without credentials

---

### 5. No TOTP / MFA Challenge Screen

The backend enforces MFA for privileged roles and returns:
```json
{ "code": "MFA_ENROLLMENT_REQUIRED", "status": 403 }
```
or may require a TOTP code on login when `mfa_enabled = true`.

The app has no:
- TOTP challenge page (enter 6-digit code during login)
- MFA enrollment wizard (`POST /users/me/mfa/enroll/start` → QR → `POST /users/me/mfa/enroll/confirm`)
- Handling of the `MFA_ENROLLMENT_REQUIRED` 403 response (falls through as a generic `AuthError`)

While regular customers will not have MFA enforced, any admin user who installs the app
and logs in will get a 403 with no meaningful message.

---

### 6. No Push Notifications

`NotificationsPage` shows 3 hardcoded Arabic strings. In `pubspec.yaml` there is no
`firebase_messaging`, `flutter_local_notifications`, or any push notification package.

The app cannot receive booking confirmations, status changes, or promotional pushes.
`AndroidManifest.xml` has no FCM service registration.

---

### 7. Exchange Rate Bar — Hardcoded

```dart
// exchange_rate_bar.dart
Text('1 USD = 14,500', ...)  // hardcoded — never fetches from backend
```

The backend exposes `GET /exchange-rates` (or similar) and `FxRateRefreshJob` keeps the
rates fresh nightly. The app ignores this completely. The `CurrencyExchangeSheet` bottom
sheet also likely uses static data.

---

### 8. Dependency Injection Not Wired

`get_it: ^9.0.5` is declared in `pubspec.yaml` but there is no `service_locator.dart` or
`injection_container.dart` anywhere in the codebase. Every page creates its dependencies
inline:
```dart
// LoginPage
create: (context) => AuthBloc(
  repository: AuthRepositoryImpl(apiClient: ApiClient()),  // new instance every render
)
```
This means:
- A new `Dio` instance (and new interceptor) is created for every page navigation
- Token storage is instantiated multiple times — no shared singleton
- Impossible to inject mocks cleanly for widget tests

---

### 9. No Structured Error Response Parsing

Backend returns structured errors:
```json
{ "success": false, "code": "INVALID_CREDENTIALS", "message": "..." }
```

App only does:
```dart
} catch (e) {
  emit(AuthError(e.toString()));  // shows "DioException [...]" to the user
}
```

`connectivity_plus` is declared in `pubspec.yaml` but is never imported or used anywhere —
the app does not detect offline state before making requests.

---

### 10. Android Package Name & Build Config

| Issue | Current | Required for Production |
|---|---|---|
| Application ID | `com.example.syria_tourism_app` | `com.ziyara.app` (or brand name) |
| App label | `"ZIYARA App"` | `"Ziyara"` (clean) |
| CI APK base URL | `https://api.ziyara.example.com/api/v1` (hardcoded in `ci.yml`) | `--dart-define=ZIYARA_API_URL=${{ secrets.API_URL }}` |
| ProGuard/R8 rules | None configured | Required to prevent Dio/Retrofit class stripping |
| Network security config | Not present | Required to allow HTTP in debug, enforce HTTPS in release |
| iOS bundle identifier | Not checked — likely `com.example.syriaTourismApp` | Must be set before App Store submission |

---

## What Works End-to-End (Happy Path)

```
User opens app → Onboarding (first run) → Login page
→ Enters credentials → POST /auth/login → tokens stored → Home screen

My Bookings tab → GET /bookings/my → list rendered
Cancel booking → POST /bookings/{id}/cancel → confirmed

Payment page → POST /payments/coupon/validate → discount applied
→ Photo upload → POST /payments/process → BookingSuccessPage

Profile page → GET /profile → name, email, balance, verified badge shown
Verify account → POST /profile/verify (multipart upload)

Logout button (when wired) → POST /auth/logout → tokens cleared → back to /login
```

---

## What is Completely Broken / Unavailable

| Feature | Broken Because |
|---|---|
| Hotels page | `FakeHotelsRepository` — dummy data |
| Restaurants page | `FakeRestaurantsRepository` — dummy data |
| Tours page | `FakeToursRepository` — dummy data |
| Transport booking | `FakeTransportRepository` — hardcoded results |
| Live driver tracking | `FakeTransportRepository` + no map + no STOMP dependency |
| Notifications | Hardcoded list — no API, no FCM |
| Exchange rate display | Hardcoded "1 USD = 14,500" |
| TOTP/MFA | No screen, no handling |
| Biometric (no token) | Bypasses auth silently |
| Logout (token cleanup) | `context.go('/login')` — tokens never cleared |
| Forgot password | UI page exists; `AuthRepository` has no method; no API call |
| Password reset | Same as above |

---

## Before Going Live — Mobile Minimum Checklist

```
[ ] Wire Hotels, Restaurants, Tours to real backend service endpoints
[ ] Wire Transport booking to real backend taxi endpoints
[ ] Replace FakeTransportRepository with STOMP WebSocket client for live tracking
[ ] Add google_maps_flutter (or flutter_map) for real map rendering
[ ] Fix biometric login: require existing token in secure storage first
[ ] Fix logout: dispatch LogoutRequested to AuthBloc (not context.go('/login'))
[ ] Fix all 5 API URL mismatches (profile, verify, coupon, payment)
[ ] Add TOTP challenge screen + MFA enrollment flow (if admin users log in on mobile)
[ ] Integrate firebase_messaging for push notifications
[ ] Wire exchange rate fetch from GET /exchange-rates
[ ] Set up GetIt injection container — remove inline ApiClient() creation
[ ] Parse backend error codes into user-friendly Arabic messages
[ ] Enable connectivity_plus — show "لا يوجد اتصال" before API calls
[ ] Rename Android application ID from com.example.* to com.ziyara.app
[ ] Set iOS bundle identifier
[ ] Add ProGuard rules for release APK
[ ] Add Network Security Config (Android) — HTTP allowed in debug, HTTPS-only in release
[ ] Parametrise CI APK URL via --dart-define + GitHub Actions secret
[ ] Write widget tests for AuthBloc, BookingBloc using GetIt-injected mock repos
```
