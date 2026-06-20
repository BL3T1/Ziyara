# Backend Implementation Plan — V2 Endpoints

Follows the project's **Clean Architecture + DDD** rules enforced by ArchUnit (56 rules, CLAUDE.md).
Each section is ordered by layer: Domain → Application → Infrastructure → Presentation.
Tests are listed alongside the code they cover.

---

## Findings from Codebase Audit

Before writing the plan the following was verified:

| Concern | Finding |
|---|---|
| `PORTAL_MANAGER` / `PORTAL_FINANCE` constants | **Already exist** in `ApiAuthorizationExpressions.java` lines 211–213. No new constants needed. |
| `portal:access`, `portal:manage`, `portal:finance` permissions | Added by `V55__dynamic_roles_migration.sql`. Already seeded. |
| `PaymentMethod` enum | Has `CASH_ON_SERVICE`, `CASH_ON_ARRIVAL`, `BANK_TRANSFER` but **not** plain `CASH`, `CHEQUE`, or `OTHER`. |
| `PaymentStatus` enum | Has `PENDING`,`PROCESSING`,`COMPLETED`,`FAILED`,`CANCELLED`,`REFUNDED`. **Missing** `COLLECTED`, `RECORDED`. |
| `Booking.paymentMethod` | Field **exists**. `Booking.paymentStatus` does **not**. |
| `ServiceProvider` lat/lng | **Not present**. Must be added. |
| `PaymentRepository.findByBookingId` | Returns `Optional<Payment>` (single row). Need `List<Payment>` for payment history. |
| Latest migration | **V55**. Next is V56. |
| `requireCurrentProviderId()` | Already in `PortalController` — copy the pattern to `PortalStaffController`. |

---

## Layer 0 — Domain  *(no Spring, no JPA)*

### 0.1 `PaymentMethod` enum — add missing values

File: `domain/enums/PaymentMethod.java`

Add three values without removing existing ones:
```java
CASH("Cash"),
CHEQUE("Cheque"),
OTHER("Other");
```
`CASH_ON_SERVICE` and `CASH_ON_ARRIVAL` remain unchanged.

### 0.2 `PaymentStatus` enum — add portal-payment statuses

File: `domain/enums/PaymentStatus.java`

Add:
```java
COLLECTED("Collected"),   // cash physically received by provider
RECORDED("Recorded");     // offline payment manually entered by provider
```
Add helper methods:
```java
public boolean isPortalRecorded() { return this == COLLECTED || this == RECORDED; }
```

### 0.3 `Booking` entity — add `paymentStatus` + domain method

File: `domain/entity/Booking.java`

Add field (plain Java, no annotations):
```java
private BookingPaymentStatus paymentStatus = BookingPaymentStatus.UNPAID;
```

New inner-package enum `domain/enums/BookingPaymentStatus.java`:
```java
public enum BookingPaymentStatus { UNPAID, PARTIALLY_PAID, PAID }
```

Add domain methods:
```java
public void markPaid() {
    this.paymentStatus = BookingPaymentStatus.PAID;
    this.updatedAt = LocalDateTime.now();
}

public void markPartiallyPaid() {
    this.paymentStatus = BookingPaymentStatus.PARTIALLY_PAID;
    this.updatedAt = LocalDateTime.now();
}

public boolean isPaymentPending() {
    return paymentStatus == BookingPaymentStatus.UNPAID
        || paymentStatus == BookingPaymentStatus.PARTIALLY_PAID;
}
```

**DDD rule:** the domain entity decides what "paid" means — the application service only calls `booking.markPaid()` and does not manipulate `paymentStatus` directly.

### 0.4 `ServiceProvider` entity — add coordinates

File: `domain/entity/ServiceProvider.java`

Add:
```java
private Double latitude;
private Double longitude;
```
With getters/setters (pure Java style matching the file).

### 0.5 `PaymentRepository` — add `findAllByBookingId`

File: `domain/repository/PaymentRepository.java`

Add:
```java
/** All payments for a booking, ordered by createdAt ascending. */
List<Payment> findAllByBookingId(UUID bookingId);
```
`findByBookingId` (Optional) remains for backward compatibility.

### 0.6 `ServiceRepository` — add map-pin queries

File: `domain/repository/ServiceRepository.java`

Add:
```java
/** Active services with coordinates, optionally filtered by type. */
List<Service> findActiveWithCoordinates(List<String> types);

/** Active services for a specific provider that have coordinates. */
List<Service> findByProviderIdWithCoordinates(UUID providerId);
```

---

### Domain Tests

**`BookingPaymentStatusTest.java`**
Location: `test/.../domain/entity/BookingPaymentStatusTest.java`

```java
@Test void initialStatus_isUnpaid() { assertThat(new Booking().getPaymentStatus()).isEqualTo(UNPAID); }
@Test void markPaid_setsStatusPaid() { Booking b = new Booking(); b.markPaid(); assertThat(b.getPaymentStatus()).isEqualTo(PAID); }
@Test void markPaid_updatesTimestamp() { Booking b = new Booking(); b.markPaid(); assertThat(b.getUpdatedAt()).isNotNull(); }
@Test void isPaymentPending_trueWhenUnpaid() { assertThat(new Booking().isPaymentPending()).isTrue(); }
@Test void isPaymentPending_falseAfterMarkPaid() { Booking b = new Booking(); b.markPaid(); assertThat(b.isPaymentPending()).isFalse(); }
```

**`PaymentStatusTest.java`** — `domain/entity/PaymentStatusTest.java`
```java
@Test void collected_isPortalRecorded() { assertThat(PaymentStatus.COLLECTED.isPortalRecorded()).isTrue(); }
@Test void completed_isNotPortalRecorded() { assertThat(PaymentStatus.COMPLETED.isPortalRecorded()).isFalse(); }
```

---

## Layer 1 — Application  *(Spring @Service, no JPA, no HTTP)*

### 1.1 New request DTOs

Location: `application/dto/request/`

**`ApproveCashPaymentRequest.java`**
```java
@NotNull @Positive BigDecimal amount;
@NotBlank @Size(max = 3) String currency;
@Size(max = 500) String notes;   // nullable
```

**`RecordPaymentRequest.java`**
```java
@NotNull @Positive BigDecimal amount;
@NotBlank @Size(max = 3) String currency;
@NotNull PaymentMethod method;
@Size(max = 255) String transactionReference;  // nullable
@Size(max = 500) String notes;                 // nullable
```

**`ResetPortalStaffPasswordRequest.java`**
```java
@NotBlank @Size(min = 6, max = 128) String newPassword;
```

### 1.2 New response DTO

Location: `application/dto/response/`

**`ProviderMapPinResponse.java`**
```java
UUID id;
String name;
String type;
Double latitude;
Double longitude;
String status;
String thumbnailUrl;  // nullable
```

**`DeliveryLocationResponse.java`** *(for the deferred stub)*
```java
UUID bookingId;
Double latitude;
Double longitude;
String status;
String updatedAt;
```

### 1.3 Update `BookingResponse`

File: `application/dto/BookingResponse.java`

Add field:
```java
String paymentStatus;   // mirrors BookingPaymentStatus.name()
```

The mapper that converts `Booking` → `BookingResponse` must populate this field.

### 1.4 New `PortalPaymentService`

File: `application/service/PortalPaymentService.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
public class PortalPaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceProviderRepository serviceProviderRepository;

    // ── read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PaymentResponse> listBookingPayments(UUID bookingId, UUID providerId) {
        verifyBookingOwnership(bookingId, providerId);
        return paymentRepository.findAllByBookingId(bookingId)
                .stream().map(this::toResponse).toList();
    }

    // ── cash approval ─────────────────────────────────────────────────────────

    public PaymentResponse approveCashPayment(
            UUID bookingId, UUID providerId, ApproveCashPaymentRequest request) {

        Booking booking = verifyBookingOwnership(bookingId, providerId);

        if (booking.getPaymentMethod() == null
                || !booking.getPaymentMethod().name().startsWith("CASH")) {
            throw new BusinessException("Booking payment method is not cash");
        }
        if (!booking.isPaymentPending()) {
            throw new BusinessException("Booking is already marked as paid");
        }

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(bookingId);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.COLLECTED);
        payment.setCategory("PORTAL_CASH_APPROVAL");
        if (request.getNotes() != null) payment.setTransactionReference(request.getNotes());
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        booking.markPaid();
        bookingRepository.save(booking);

        return toResponse(saved);
    }

    // ── manual payment ────────────────────────────────────────────────────────

    public PaymentResponse recordPayment(
            UUID bookingId, UUID providerId, RecordPaymentRequest request) {

        Booking booking = verifyBookingOwnership(bookingId, providerId);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setBookingId(bookingId);
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.RECORDED);
        payment.setCategory("PORTAL_MANUAL_ENTRY");
        payment.setTransactionReference(request.getTransactionReference());
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        // Auto-mark paid when recorded amount covers booking total
        BigDecimal recorded = paymentRepository.findAllByBookingId(bookingId).stream()
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (booking.getTotalAmount() != null
                && recorded.compareTo(booking.getTotalAmount()) >= 0) {
            booking.markPaid();
            bookingRepository.save(booking);
        }

        return toResponse(saved);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Booking verifyBookingOwnership(UUID bookingId, UUID providerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        Service service = serviceRepository.findById(booking.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!providerId.equals(service.getProviderId())) {
            throw new AccessDeniedException("Booking does not belong to this provider");
        }
        return booking;
    }

    private PaymentResponse toResponse(Payment p) { /* map fields */ }
}
```

### 1.5 New `MapService`

File: `application/service/MapService.java`

```java
@Service
@RequiredArgsConstructor
public class MapService {

    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<ProviderMapPinResponse> getProviderPins(List<String> types) {
        return serviceRepository.findActiveWithCoordinates(types)
                .stream().map(this::toPin).toList();
    }

    @Transactional(readOnly = true)
    public List<ProviderMapPinResponse> getPortalPins(UUID providerId) {
        return serviceRepository.findByProviderIdWithCoordinates(providerId)
                .stream().map(this::toPin).toList();
    }

    private ProviderMapPinResponse toPin(Service s) {
        return new ProviderMapPinResponse(
                s.getId(), s.getName(), s.getType().name(),
                s.getLatitude(), s.getLongitude(), s.getStatus().name(), null);
    }
}
```

### 1.6 Extend `PortalStaffService` — `resetStaffPassword`

File: `application/service/PortalStaffService.java`

Inject `UserRepository` (already used elsewhere, just add to constructor if not present) and `PasswordEncoder`.

Add method:
```java
public void resetStaffPassword(UUID providerId, UUID targetUserId, String newPassword) {
    // 1. Verify target is a staff member of this provider
    PortalStaffMember member = portalStaffMemberRepository
            .findByProviderIdAndUserId(providerId, targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Staff member not found for this provider"));

    // 2. Cannot reset the owner's password through this endpoint
    if (member.isOwner()) {
        throw new BusinessException(
                "Cannot reset the portal owner password through this endpoint");
    }

    // 3. Hash and persist
    String hashed = passwordEncoder.encode(newPassword);
    userRepository.updatePassword(targetUserId, hashed);
}
```

`UserRepository.updatePassword` likely already exists (used by `AuthService`). If not, add:
```java
void updatePassword(UUID userId, String hashedPassword);
```

---

### Application Tests

**`PortalPaymentServiceTest.java`**  
Location: `test/.../application/service/PortalPaymentServiceTest.java`

Pattern matches `BookingServiceTest` — `@ExtendWith(MockitoExtension.class)`:

```java
@Mock BookingRepository bookingRepository;
@Mock PaymentRepository paymentRepository;
@Mock ServiceRepository serviceRepository;
@InjectMocks PortalPaymentService service;

// listBookingPayments ─────────────────────────────────────────────────────
@Test void listPayments_bookingNotFound_throwsNotFound() { ... }
@Test void listPayments_bookingNotOwnedByProvider_throwsAccessDenied() { ... }
@Test void listPayments_happyPath_returnsMappedList() { ... }

// approveCashPayment ──────────────────────────────────────────────────────
@Test void approveCash_nonCashMethod_throwsBusiness() { ... }
@Test void approveCash_alreadyPaid_throwsBusiness() { ... }
@Test void approveCash_happyPath_savesPaymentAndMarksBookingPaid() {
    // verify bookingRepository.save called with paymentStatus == PAID
}

// recordPayment ───────────────────────────────────────────────────────────
@Test void recordPayment_savesWithStatusRecorded() { ... }
@Test void recordPayment_whenTotalCovered_marksBookingPaid() { ... }
@Test void recordPayment_whenNotCovered_doesNotMarkPaid() { ... }
```

**`MapServiceTest.java`**  
Location: `test/.../application/service/MapServiceTest.java`

```java
@Test void getProviderPins_emptyTypes_returnsAllActive() { ... }
@Test void getProviderPins_filteredByType_returnsOnlyMatchingType() { ... }
@Test void getPortalPins_returnsPinsForProvider() { ... }
@Test void toPin_mapsAllFields() { ... }
```

**`PortalStaffServiceResetPasswordTest.java`**

```java
@Test void resetPassword_memberNotFound_throwsNotFound() { ... }
@Test void resetPassword_targetIsOwner_throwsBusiness() { ... }
@Test void resetPassword_happyPath_callsUpdatePasswordWithHash() {
    // verify passwordEncoder.encode called
    // verify userRepository.updatePassword called with hashed value
}
```

---

## Layer 2 — Infrastructure  *(JPA, adapters)*

### 2.1 JPA entity changes

**`BookingJpa.java`** — add column:
```java
@Column(name = "payment_status")
@Enumerated(EnumType.STRING)
private BookingPaymentStatus paymentStatus = BookingPaymentStatus.UNPAID;
```

**`ServiceProviderJpa.java`** — add columns:
```java
@Column(name = "latitude")
private Double latitude;

@Column(name = "longitude")
private Double longitude;
```

### 2.2 Repository adapters

**`PaymentRepositoryAdapter.java`** — implement `findAllByBookingId`:
```java
@Override
public List<Payment> findAllByBookingId(UUID bookingId) {
    return jpaRepository.findAllByBookingIdOrderByCreatedAtAsc(bookingId)
            .stream().map(mapper::toDomain).toList();
}
```

Add to Spring Data JPA interface `PaymentJpaRepository`:
```java
List<PaymentJpa> findAllByBookingIdOrderByCreatedAtAsc(UUID bookingId);
```

**`ServiceRepositoryAdapter.java`** — implement the two new map-pin queries:
```java
@Override
public List<Service> findActiveWithCoordinates(List<String> types) {
    return jpaRepository.findActiveWithCoordinates(types)
            .stream().map(mapper::toDomain).toList();
}
```

Add to `ServiceJpaRepository` (Spring Data or `@Query`):
```java
@Query("""
    SELECT s FROM ServiceJpa s
    WHERE s.latitude IS NOT NULL AND s.longitude IS NOT NULL
      AND s.status = 'ACTIVE'
      AND (:types IS NULL OR s.type IN :types)
    """)
List<ServiceJpa> findActiveWithCoordinates(@Param("types") List<String> types);

@Query("""
    SELECT s FROM ServiceJpa s
    WHERE s.latitude IS NOT NULL AND s.longitude IS NOT NULL
      AND s.providerId = :providerId AND s.status = 'ACTIVE'
    """)
List<ServiceJpa> findByProviderIdWithCoordinates(@Param("providerId") UUID providerId);
```

### 2.3 Flyway — Migration V56

File: `resources/db/migration/V56__portal_payments_and_map.sql`

```sql
-- 1. Booking payment status
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';

-- 2. Provider coordinates (for map)
ALTER TABLE service_providers
    ADD COLUMN IF NOT EXISTS latitude  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- 3. Indexes for map queries
CREATE INDEX IF NOT EXISTS idx_services_coords
    ON services(latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_services_provider_coords
    ON services(provider_id, latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
```

### 2.4 Flyway — Migration V57 *(delivery tracking table)*

File: `resources/db/migration/V57__delivery_locations.sql`

```sql
CREATE TABLE IF NOT EXISTS delivery_locations (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID         NOT NULL REFERENCES bookings(id),
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    status      VARCHAR(50),
    recorded_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_delivery_loc_booking
    ON delivery_locations(booking_id, recorded_at DESC);
```

---

## Layer 3 — Presentation  *(Controllers — no business logic)*

### 3.1 Add payment endpoints to `PortalController`

New imports needed:
```java
import com.ziyara.backend.application.dto.request.ApproveCashPaymentRequest;
import com.ziyara.backend.application.dto.request.RecordPaymentRequest;
import com.ziyara.backend.application.dto.response.PaymentResponse;
import com.ziyara.backend.application.service.PortalPaymentService;
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.PORTAL_FINANCE;
```

Inject `PortalPaymentService` alongside existing services.

Add three methods (note: class-level `@PreAuthorize(PROVIDER_PORTAL)` already covers access; the two write endpoints add a stricter `@PreAuthorize(PORTAL_FINANCE)` override):

```java
@GetMapping("/bookings/{bookingId}/payments")
@Operation(summary = "List payments for a booking")
public ResponseEntity<ApiResponse<List<PaymentResponse>>> listBookingPayments(
        @PathVariable UUID bookingId) {
    UUID providerId = requireCurrentProviderId();
    return ResponseEntity.ok(ApiResponse.success(
            portalPaymentService.listBookingPayments(bookingId, providerId)));
}

@PostMapping("/bookings/{bookingId}/payments/cash-approve")
@PreAuthorize(PORTAL_FINANCE)
@Operation(summary = "Approve cash payment collection for a booking")
public ResponseEntity<ApiResponse<PaymentResponse>> approveCashPayment(
        @PathVariable UUID bookingId,
        @Valid @RequestBody ApproveCashPaymentRequest request) {
    UUID providerId = requireCurrentProviderId();
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                    portalPaymentService.approveCashPayment(bookingId, providerId, request)));
}

@PostMapping("/bookings/{bookingId}/payments")
@PreAuthorize(PORTAL_FINANCE)
@Operation(summary = "Record an offline/manual payment for a booking")
public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
        @PathVariable UUID bookingId,
        @Valid @RequestBody RecordPaymentRequest request) {
    UUID providerId = requireCurrentProviderId();
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                    portalPaymentService.recordPayment(bookingId, providerId, request)));
}
```

### 3.2 Add reset-password endpoint to `PortalStaffController`

New imports + inject `PortalStaffService` (already there), add:

```java
import static com.ziyara.backend.infrastructure.security.ApiAuthorizationExpressions.PORTAL_MANAGER;

@PostMapping("/{userId}/reset-password")
@PreAuthorize(PORTAL_MANAGER)
@Operation(summary = "Reset a staff member's password (portal owner only)")
public ResponseEntity<Void> resetStaffPassword(
        @PathVariable UUID userId,
        @Valid @RequestBody ResetPortalStaffPasswordRequest request) {
    UUID providerId = requireCurrentProviderId();   // copy helper from PortalController
    portalStaffService.resetStaffPassword(providerId, userId, request.getNewPassword());
    return ResponseEntity.noContent().build();
}
```

Add `requireCurrentProviderId()` + `getCurrentUserId()` private helpers to `PortalStaffController`
(identical to the ones in `PortalController` — or extract to a shared `PortalControllerSupport` base class to avoid duplication).

### 3.3 New `MapController`

File: `presentation/controller/MapController.java`

```java
@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
@Tag(name = "Map", description = "Geographic pins for admin map and portal map views")
@SecurityRequirement(name = "bearerAuth")
public class MapController {

    private final MapService mapService;

    @GetMapping("/providers")
    @PreAuthorize(PROVIDERS_READ)
    @Operation(summary = "All active provider locations (admin map)")
    public ResponseEntity<ApiResponse<List<ProviderMapPinResponse>>> getProviderPins(
            @RequestParam(required = false) String types) {
        List<String> typeFilter = types != null
                ? Arrays.asList(types.split(","))
                : List.of();
        return ResponseEntity.ok(ApiResponse.success(mapService.getProviderPins(typeFilter)));
    }

    @GetMapping("/portal/pins")
    @PreAuthorize(PROVIDER_PORTAL)
    @Operation(summary = "Provider's own listing locations (portal map)")
    public ResponseEntity<ApiResponse<List<ProviderMapPinResponse>>> getPortalPins() {
        UUID providerId = requireCurrentProviderId();
        return ResponseEntity.ok(ApiResponse.success(mapService.getPortalPins(providerId)));
    }

    @GetMapping("/delivery/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Latest delivery location for a booking (stub — requires driver app)")
    public ResponseEntity<DeliveryLocationResponse> getDeliveryLocation(
            @PathVariable UUID bookingId) {
        // Returns 404 until a driver app writes rows to delivery_locations
        return ResponseEntity.notFound().build();
    }

    private UUID requireCurrentProviderId() { /* same pattern as PortalController */ }
    private UUID getCurrentUserId() { /* same pattern */ }
}
```

> **Note on ABAC for `/map/portal/pins`:** The frontend calls `GET /portal/map/pins` but that path collides with `PortalController`'s `@RequestMapping("/portal")`. Either:
> - Move the portal-map endpoint into `PortalController` at `GET /portal/map/pins`, or
> - Keep it in `MapController` at `GET /map/portal/pins` and update the frontend `api.ts` `getPortalPins` URL accordingly.
>
> **Recommended:** keep in `PortalController` for consistency — all `/portal/*` endpoints live there.

---

### Presentation Tests (WebMvc slice — optional, light-touch)

**`PortalControllerPaymentWebMvcTest.java`**

```java
@WebMvcTest(PortalController.class)
class PortalControllerPaymentWebMvcTest {

    @Test
    @WithMockUser(authorities = "portal:access")
    void listBookingPayments_withPortalAccess_returns200() { ... }

    @Test
    @WithMockUser(authorities = "portal:access")
    void approveCashPayment_withoutPortalFinance_returns403() { ... }

    @Test
    @WithMockUser(authorities = {"portal:access", "portal:finance"})
    void approveCashPayment_withPortalFinance_returns201() { ... }

    @Test
    @WithMockUser(authorities = {"portal:access", "portal:finance"})
    void approveCashPayment_invalidBody_returns400() { /* missing amount */ }
}
```

**`MapControllerWebMvcTest.java`**

```java
@Test @WithMockUser(authorities = "providers:read")
void getProviderPins_withProvidersRead_returns200() { ... }

@Test @WithAnonymousUser
void getProviderPins_anonymous_returns401() { ... }

@Test @WithMockUser(authorities = "portal:access")
void getPortalPins_withPortalAccess_returns200() { ... }
```

---

## All New Files Summary

| File | Layer | Notes |
|---|---|---|
| `domain/enums/BookingPaymentStatus.java` | Domain | New enum: `UNPAID`, `PARTIALLY_PAID`, `PAID` |
| `application/dto/request/ApproveCashPaymentRequest.java` | Application | Bean-validated request body |
| `application/dto/request/RecordPaymentRequest.java` | Application | Bean-validated request body |
| `application/dto/request/ResetPortalStaffPasswordRequest.java` | Application | Bean-validated request body |
| `application/dto/response/ProviderMapPinResponse.java` | Application | Map pin projection |
| `application/dto/response/DeliveryLocationResponse.java` | Application | Delivery tracking (stub) |
| `application/service/PortalPaymentService.java` | Application | Cash approve + manual payment + list |
| `application/service/MapService.java` | Application | Provider and portal map pins |
| `presentation/controller/MapController.java` | Presentation | `GET /map/providers`, `/map/delivery/{id}` |
| `V56__portal_payments_and_map.sql` | Infrastructure | `payment_status` col, lat/lng cols, indexes |
| `V57__delivery_locations.sql` | Infrastructure | Delivery tracking table |
| **Tests** | | |
| `domain/entity/BookingPaymentStatusTest.java` | Test | 5 domain tests |
| `domain/entity/PaymentStatusTest.java` | Test | 2 domain tests |
| `application/service/PortalPaymentServiceTest.java` | Test | 8 service tests |
| `application/service/MapServiceTest.java` | Test | 4 service tests |
| `application/service/PortalStaffServiceResetPasswordTest.java` | Test | 3 service tests |
| `presentation/controller/PortalControllerPaymentWebMvcTest.java` | Test | 4 WebMvc slice tests |
| `presentation/controller/MapControllerWebMvcTest.java` | Test | 3 WebMvc slice tests |

## Files Modified

| File | Change |
|---|---|
| `domain/enums/PaymentMethod.java` | Add `CASH`, `CHEQUE`, `OTHER` |
| `domain/enums/PaymentStatus.java` | Add `COLLECTED`, `RECORDED`, `isPortalRecorded()` |
| `domain/entity/Booking.java` | Add `paymentStatus`, `markPaid()`, `markPartiallyPaid()`, `isPaymentPending()` |
| `domain/entity/ServiceProvider.java` | Add `latitude`, `longitude` |
| `domain/repository/PaymentRepository.java` | Add `findAllByBookingId` |
| `domain/repository/ServiceRepository.java` | Add two coordinate-query methods |
| `application/dto/BookingResponse.java` | Add `paymentStatus` field |
| `application/service/PortalStaffService.java` | Add `resetStaffPassword` |
| `infrastructure/persistence/entity/BookingJpa.java` | Map `payment_status` column |
| `infrastructure/persistence/entity/ServiceProviderJpa.java` | Map `latitude`, `longitude` |
| `infrastructure/persistence/adapter/PaymentRepositoryAdapter.java` | Implement `findAllByBookingId` |
| `infrastructure/persistence/adapter/ServiceRepositoryAdapter.java` | Implement coordinate queries |
| `infrastructure/persistence/jpa/PaymentJpaRepository.java` | Add `findAllByBookingIdOrderByCreatedAtAsc` |
| `infrastructure/persistence/jpa/ServiceJpaRepository.java` | Add two `@Query` methods |
| `presentation/controller/PortalController.java` | Add 3 payment endpoints + inject `PortalPaymentService` |
| `presentation/controller/PortalStaffController.java` | Add reset-password endpoint + `requireCurrentProviderId` helper |
