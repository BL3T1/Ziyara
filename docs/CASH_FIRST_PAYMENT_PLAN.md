# Cash-First Payment Implementation Plan

**Status:** Draft
**Target branch:** V1 → cash-first feature branch
**Author:** Engineering
**Last updated:** 2026-06-07

---

## 1. Context

US/EU sanctions block Stripe and most international gateways from operating in Syria. The current architecture treats `CASH` as a fallback (`PortalBookingsPage.tsx` "Approve Cash" button, `PaymentMethod.CASH_ON_SERVICE`). For the Syrian market launch, cash must become the **primary, default, first-class** payment path. Card gateways stay in the codebase but are disabled by config and hidden from the customer UI by default.

This plan does **not** remove Stripe — it makes the system functional without it.

---

## 2. Goals & Non-Goals

### Goals
- Cash is the default `PaymentMethod` for new bookings.
- A booking with `paymentMethod = CASH` can be confirmed **without any gateway call**.
- Providers can record cash collected on the ground; admins can reconcile cash against bookings.
- Commission owed by providers on cash bookings is tracked and deducted from payouts (or invoiced).
- No-show / cancellation policy works without a card hold.
- Receipts are printable + verifiable (QR code → public verification endpoint).

### Non-Goals
- Removing Stripe / `StripePaymentProvider`. Keep it gated behind `PAYMENT_GATEWAY_ENABLED=false`.
- Building a new wallet / Z-Pay. That stays as a future track.
- Crypto, PayPal, Apple/Google Pay flows.
- Replacing the existing `approve-cash` endpoint — it stays; this plan generalises around it.

---

## 3. Current State Audit

| Area | Today | Gap |
|---|---|---|
| `PaymentMethod` enum | `CASH`, `CASH_ON_SERVICE`, `CASH_ON_ARRIVAL` exist | No canonical default; three overlapping cash values cause confusion |
| `PaymentStatus` enum | `PENDING`, `COMPLETED`, `COLLECTED`, `RECORDED`, `REFUNDED` | No `RECONCILED` state for admin-confirmed cash; no `NO_SHOW_FORFEIT` |
| `PaymentService.initiatePayment` | Only calls gateway if `isCardMethod` and token present | Cash branch returns a `PENDING` payment with no follow-up workflow attached |
| Booking confirmation | Coupled to payment completion for card flow | Cash bookings need confirmation **without** payment completion |
| Portal | `approveCashPayment` exists, `addPayment` exists | No daily cash sheet, no reconciliation, no commission deduction |
| Admin | No cash reconciliation page | Required for finance ops |
| Mobile (Flutter) | Card flow only in checkout | Must support cash as default + show receipt + QR |
| Web checkout | Uses Stripe-style flow | Must support cash-only path |
| Config | `PAYMENT_GATEWAY_PROVIDER=stripe\|stub` | No `cashOnly` mode flag |
| Receipt | None | Need printable HTML + QR verification |

---

## 4. Domain Model Changes

### 4.1 Enums (`domain/enums/`)

**`PaymentMethod.java`** — collapse cash variants to one canonical value, keep aliases for migration:
- Promote `CASH` as canonical primary method.
- Deprecate `CASH_ON_SERVICE` and `CASH_ON_ARRIVAL` (kept for read-back of legacy rows; not selectable in new bookings).
- Add Javadoc marking them deprecated.

**`PaymentStatus.java`** — add two states:
- `RECONCILED` — admin has matched the cash payment against actual cash received from provider during payout cycle.
- `NO_SHOW_FORFEIT` — booking marked no-show; cash deposit (if any) forfeit to platform.

Update `isSuccessful()` to include `RECONCILED`. Update `isPortalRecorded()` accordingly.

### 4.2 New domain entity: `CashCollection`

```
domain/entity/CashCollection.java
```
Fields: `id`, `paymentId`, `providerId`, `collectedAt`, `collectedByUserId`, `amount`, `currency`, `receiptNumber`, `notes`, `reconciledAt`, `reconciledByUserId`, `status` (`OPEN | RECONCILED | DISPUTED`).

### 4.3 Repository interfaces

- `domain/repository/CashCollectionRepository.java` — `findByProviderId`, `findByPaymentId`, `findOpenForProvider`, `sumOpenForProvider`.

### 4.4 Use cases (`domain/usecase/payment/`)

- `ConfirmCashBookingUseCase` — creates a `Payment(PENDING, method=CASH)` linked to the booking; does **not** call gateway; emits booking-confirmed event.
- `RecordCashCollectionUseCase` — provider portal action; transitions `Payment` to `COLLECTED`, creates a `CashCollection(OPEN)`.
- `ReconcileCashCollectionUseCase` — admin action; transitions `CashCollection` to `RECONCILED`, transitions `Payment` to `RECONCILED`.
- `ForfeitNoShowDepositUseCase` — booking no-show + deposit exists → transitions `Payment` to `NO_SHOW_FORFEIT`.

All four are pure domain classes (no Spring).

---

## 5. Infrastructure / Persistence

### 5.1 Flyway migration `V58__cash_first_payments.sql`

```sql
-- Add new payment status values to the check constraint (rebuild if needed)
ALTER TABLE payments DROP CONSTRAINT IF EXISTS payments_status_check;
ALTER TABLE payments ADD CONSTRAINT payments_status_check CHECK (
  status IN ('PENDING','PROCESSING','COMPLETED','FAILED','CANCELLED',
             'REFUNDED','COLLECTED','RECORDED','RECONCILED','NO_SHOW_FORFEIT')
);

-- Cash collections table
CREATE TABLE cash_collections (
  id UUID PRIMARY KEY,
  payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE RESTRICT,
  provider_id UUID NOT NULL REFERENCES service_providers(id),
  collected_at TIMESTAMPTZ NOT NULL,
  collected_by_user_id UUID NOT NULL REFERENCES users(id),
  amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
  currency CHAR(3) NOT NULL DEFAULT 'USD',
  receipt_number VARCHAR(32) NOT NULL UNIQUE,
  notes TEXT,
  reconciled_at TIMESTAMPTZ,
  reconciled_by_user_id UUID REFERENCES users(id),
  status VARCHAR(16) NOT NULL CHECK (status IN ('OPEN','RECONCILED','DISPUTED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cash_collections_provider_status ON cash_collections(provider_id, status);
CREATE INDEX idx_cash_collections_payment ON cash_collections(payment_id);

-- Default booking payment method = CASH where null (legacy rows untouched)
UPDATE bookings SET payment_method = 'CASH'
 WHERE payment_method IS NULL;

-- New permissions
INSERT INTO permissions (id, code, description) VALUES
  (gen_random_uuid(), 'payments:cash-record',    'Record cash collection in portal'),
  (gen_random_uuid(), 'payments:cash-reconcile', 'Reconcile cash collections (admin/finance)')
ON CONFLICT (code) DO NOTHING;
```

> Note: follow the repo's existing migration style for constraint rebuilds (see `V20`, `V22`, `V51`).

### 5.2 JPA + adapters

- `CashCollectionJpa.java`, `CashCollectionJpaRepository.java`, `CashCollectionRepositoryAdapter.java`, `CashCollectionMapper.java`.
- **No JPA relationships** per `core/CLAUDE.md` — store `paymentId`, `providerId`, `collectedByUserId` as raw `UUID`.

### 5.3 Config

`infrastructure/payment/PaymentGatewayProperties.java`:
- Add `boolean cashOnlyMode` (default `true`).
- When `cashOnlyMode=true`: gateway provider bean must not be selected for new payments even if Stripe is configured.

`application.yml` / `application-prod.yml`:
```yaml
ziyara:
  payment:
    cash-only-mode: ${PAYMENT_CASH_ONLY_MODE:true}
    gateway-enabled: ${PAYMENT_GATEWAY_ENABLED:false}
```

### 5.4 Receipt numbering

`infrastructure/payment/ReceiptNumberGenerator.java` — format `CR-YYYYMMDD-NNNN` using a Postgres sequence `cash_receipt_seq` (add to migration).

---

## 6. Application Layer

### 6.1 `PaymentService` changes

- `initiatePayment`: when `method == CASH` (or `cashOnlyMode=true`), skip the gateway branch entirely. Create `Payment(PENDING, CASH)` and return. Do **not** require a `paymentToken`.
- New: `recordCashCollection(paymentId, RecordCashRequest, performedByUserId)` → uses `RecordCashCollectionUseCase`. Generates receipt number. Publishes staff notification (`CASH_COLLECTED`).
- New: `reconcileCashCollection(collectionId, performedByUserId)` → uses `ReconcileCashCollectionUseCase`. Updates payout balance for provider.
- New: `listOpenCashForProvider(providerId)` and `sumOpenCashForProvider(providerId)` — used by payout module.
- `getPaymentSummary`: include `totalCollectedCash` and `totalReconciledCash`.

### 6.2 New `CashReconciliationService`

- `dailyCashSheet(providerId, date)` → list of collections for that day.
- `pendingReconciliationReport(filter)` → admin view.
- `forfeitNoShow(bookingId, performedByUserId)` → uses `ForfeitNoShowDepositUseCase`; ties to no-show booking action.

### 6.3 `PortalService` / `PayoutService` integration

When computing provider available balance:
```
available = sumReconciledBookings(provider)
          - sumOpenCash(provider)      // platform commission still owed
          - sumPaidPayouts(provider)
```
This makes the cash collected by the provider count *against* their payout until the platform reconciles it (i.e., until commission is settled with the platform — typically in person or via bank transfer to platform).

### 6.4 DTOs (`application/dto/`)

- `request/RecordCashCollectionRequest.java` — `amount`, `currency`, `collectedAt`, `notes`.
- `request/ReconcileCashCollectionRequest.java` — `notes`.
- `response/CashCollectionResponse.java` — full collection + receipt number + QR token.
- `response/CashReconciliationSummaryResponse.java` — totals per provider, per period.
- `response/CashReceiptResponse.java` — printable receipt payload (booking, customer, amount, QR).

---

## 7. Presentation Layer

### 7.1 New endpoints

| Method | Path | Permission | Purpose |
|---|---|---|---|
| `POST` | `/portal/bookings/{bookingId}/cash/record` | `payments:cash-record` | Provider records cash received |
| `GET` | `/portal/cash/collections` | `payments:cash-record` | Provider's collections list |
| `GET` | `/portal/cash/daily-sheet?date=YYYY-MM-DD` | `payments:cash-record` | Provider's daily cash sheet |
| `GET` | `/admin/cash/pending-reconciliation` | `payments:cash-reconcile` | Admin reconciliation queue |
| `POST` | `/admin/cash/collections/{id}/reconcile` | `payments:cash-reconcile` | Admin marks reconciled |
| `POST` | `/admin/cash/collections/{id}/dispute` | `payments:cash-reconcile` | Admin marks disputed |
| `GET` | `/payments/{id}/receipt` | `payments:read` (or signed-token public) | Receipt JSON |
| `GET` | `/public/receipt/{receiptNumber}?token=...` | _public_ | Verify receipt (QR target) |

### 7.2 Controllers

- `PortalCashController.java` (new) — first 3 routes.
- `AdminCashReconciliationController.java` (new) — next 3 routes.
- Extend `PaymentController.java` for receipt routes.

Every method has `@PreAuthorize(...)` per the repo convention. Use `ApiAuthorizationExpressions` constants.

### 7.3 Existing endpoint adjustments

- `POST /portal/bookings/{id}/payments/cash-approve` (already exists per V2 memo): keep as alias → delegates to new `recordCashCollection` internally. Mark deprecated in OpenAPI.

---

## 8. Frontend (React) Changes

### 8.1 Checkout flow

- `BookingCheckoutPage.tsx`: default selected method = `CASH`. Card section gated by `featureFlags.paymentGatewayEnabled` (read from `/config/public`).
- When `CASH` selected: skip card form entirely; submit creates booking + cash-pending payment in one call; show confirmation + receipt link.
- Add legal copy: "Pay X amount in cash on arrival. The provider will issue a receipt."

### 8.2 Portal — provider side

- `PortalBookingsPage.tsx`: the existing "Approve Cash" button stays but now opens a `RecordCashModal` that captures actual amount + notes + (optional) collected date. Submit calls the new endpoint.
- New page `PortalCashSheetPage.tsx` at `/portal/cash`: shows today's collections, weekly totals, link to print daily sheet.
- Sidebar item `portal_cash` gated by `payments:cash-record`.

### 8.3 Admin — finance side

- New `AdminCashReconciliationPage.tsx` at `/admin/cash`. Two tabs: **Pending** and **Reconciled**. Bulk reconcile action. Filter by provider / date / amount range.
- Sidebar item `admin_cash` gated by `payments:cash-reconcile`.

### 8.4 Receipt component

- `ReceiptView.tsx` — printable layout (A6 thermal-friendly): logo, booking ID, customer name, amount, currency, date, provider name, receipt number, QR code (linking to public verify URL).
- Print button uses `window.print()` with a print stylesheet.

### 8.5 i18n

Add EN + AR keys for all new strings under `payment.cash.*` and `admin.cash.*`.

### 8.6 API client (`api.ts`)

Add `cashAPI`: `recordCollection`, `listCollections`, `dailySheet`, `pendingReconciliation`, `reconcile`, `dispute`, `getReceipt`.

---

## 9. Mobile (Flutter) Changes

- `lib/features/booking/checkout_page.dart`: default + only method is `CASH` when `cashOnlyMode` is on. Card section hidden.
- New `lib/features/booking/receipt_page.dart`: shows receipt + QR. Save-to-gallery via `image_gallery_saver`.
- `lib/data/api_client.dart`: add cash endpoints (`recordCollection` only — providers using mobile portal).
- Provider role users: portal staff get a basic "record cash" screen in the mobile app (Phase 2; not blocking V1 launch).
- Strings file: EN + AR cash keys.

---

## 10. No-Show / Cancellation Policy (Without Card Hold)

Card holds aren't available, so we substitute with:

1. **Optional bank-transfer deposit** for high-value bookings (hotels > N nights). Provider configures threshold in `ProviderSettings.depositPolicy`. Customer pays deposit via local bank transfer; deposit recorded as a separate `Payment(BANK_TRANSFER, COMPLETED)`. On no-show, `ForfeitNoShowDepositUseCase` reclassifies it.
2. **OTP confirmation 24h before**: send SMS/WhatsApp; un-confirmed → auto-cancel with no penalty.
3. **Customer reliability score**: track no-shows per phone number; below threshold blocks new bookings. New entity `CustomerReliabilityScore` (out of scope here — referenced for follow-up plan).

For V1 launch, ship (2) only. (1) and (3) are follow-up tracks.

---

## 11. Testing

### 11.1 Unit tests (Mockito)

- `ConfirmCashBookingUseCaseTest` — happy path, idempotency, blocked methods.
- `RecordCashCollectionUseCaseTest` — amount mismatch, double-record prevention, receipt number generation.
- `ReconcileCashCollectionUseCaseTest` — transitions, idempotency.
- `PaymentServiceCashTest` — extends current `PaymentServiceTest`; covers `cashOnlyMode=true` skipping gateway.
- `CashReconciliationServiceTest` — daily sheet aggregation, dispute flow.

### 11.2 Controller WebMvc tests

- `PortalCashControllerWebMvcTest` — auth gating + happy paths.
- `AdminCashReconciliationControllerWebMvcTest` — auth gating + bulk reconcile.
- Extend `PaymentControllerWebMvcTest` for receipt endpoints (signed token).

### 11.3 Architecture tests

- Add `CleanArchitectureDddTest` rule: new `usecase.payment.*` classes have no Spring imports (auto-covered by existing rules).

### 11.4 Integration tests (Testcontainers)

- `CashFlowIntegrationTest` — end-to-end: create booking → record collection → reconcile → payout balance updates correctly.

---

## 12. Configuration & Rollout

### 12.1 Env vars

| Var | Default | Effect |
|---|---|---|
| `PAYMENT_CASH_ONLY_MODE` | `true` | Hides card UI, skips gateway. |
| `PAYMENT_GATEWAY_ENABLED` | `false` | Disables Stripe/stub bean. |
| `PAYMENT_DEPOSIT_BANK_TRANSFER_ENABLED` | `false` | Phase-2 deposit policy. |

### 12.2 Feature flag

- `cashOnlyMode` exposed via `GET /config/public` so the FE/mobile can render accordingly.

### 12.3 Rollout

1. Migration applied on staging.
2. Backfill: existing `PENDING` Stripe payments left alone; `cashOnlyMode=true` only affects new bookings.
3. Seed test data: one provider with 5 open cash collections.
4. Run cash workflow end-to-end on staging with the finance team.
5. Production deploy with `PAYMENT_CASH_ONLY_MODE=true` and `PAYMENT_GATEWAY_ENABLED=false`.

---

## 13. Operational Concerns

- **Receipt fraud**: receipt QR encodes `receiptNumber` + HMAC signature (`receipt-token`). Public verify endpoint checks signature before showing.
- **Cash skimming by provider staff**: every `RecordCashCollection` is audited via `AuditServiceApi.logAction("CASH_COLLECTED", ...)` with `collectedByUserId`. Admin reconciliation report cross-checks against bookings marked completed.
- **Currency**: cash-collected amount stored in booking currency (default USD per finance team). FE shows both USD and SYP at current exchange rate (display-only).
- **Reporting**: existing `RevenueReportResponse` extended with `cashCollected`, `cashReconciled`, `cashOpen`.

---

## 14. Out of Scope (Follow-up Plans)

- Customer reliability score system.
- Local Syrian payment processors (Sham Cash, MTN MobiMoney) — separate `LocalProvider` adapter.
- Deposit via bank transfer (track for V2).
- Wallet (`Z-Pay`) top-up via cash — depends on this plan.

---

## 15. Acceptance Criteria

A reviewer should be able to verify the launch by:

1. Setting `PAYMENT_CASH_ONLY_MODE=true`, deploying, and **creating a booking end-to-end without any gateway call in the logs**.
2. Logging into the provider portal and recording a cash collection; receipt PDF/print view renders with a working QR.
3. Logging into admin and reconciling that collection; provider's payout balance updates correctly.
4. Scanning the QR with a phone takes them to a verification page showing the receipt as valid.
5. All new ArchUnit, unit, controller and integration tests pass: `./gradlew check`.

---

## 16. Estimated Effort

| Track | Effort |
|---|---|
| Domain + migration + use cases | 2 d |
| Service layer + DTOs | 2 d |
| Controllers + permissions | 1 d |
| Tests (unit + controller + integration) | 2 d |
| FE checkout + portal cash sheet + admin reconciliation | 4 d |
| Receipt component + QR verify | 1 d |
| Mobile checkout + receipt | 2 d |
| Config + rollout + smoke | 1 d |
| **Total** | **~15 dev-days (1 engineer) or ~1 week with 3 engineers** |
