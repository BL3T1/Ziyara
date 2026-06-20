# Implementation Plan V2

## Overview

Five independent feature tracks. Each track lists backend changes first, then frontend.
Permission codes follow the existing ABAC scheme (`portal:manage`, `portal:finance`, etc.).

---

## Track 1 — Cash Payment Approval + Manual Payment Entry

### Goal
Providers can see when a customer chose cash as the payment method, mark that cash has been collected, and also record a new payment manually (e.g. offline bank transfer, cheque).

### 1.1 Backend

**BookingDto** — add `paymentMethod` field if not already returned by `/portal/bookings`.

**New endpoint: approve cash payment**
```
POST /portal/bookings/{bookingId}/payments/cash-approve
```
- Body: `{ amount: number, currency: string, notes?: string }`
- Creates a `Payment` record with `method=CASH`, `status=COLLECTED`, links to booking.
- Sets booking `paymentStatus = PAID` (or equivalent field).
- Guard: caller must be a portal staff member for this provider.

**New endpoint: record manual payment**
```
POST /portal/bookings/{bookingId}/payments
```
- Body: `{ amount: number, currency: string, method: string, transactionReference?: string, notes?: string }`
- `method` enum values: `CASH | BANK_TRANSFER | CHEQUE | OTHER`
- Creates a `Payment` record with `status=RECORDED`.
- Guard: same as above, requires `portal:finance` permission.

**New endpoint: list payments for a booking**
```
GET /portal/bookings/{bookingId}/payments
```
- Returns `PaymentDto[]`.

**`BookingDto`** — add fields:
```java
String paymentMethod;      // e.g. "CASH", "CARD", "BANK_TRANSFER"
String paymentStatus;      // e.g. "PENDING", "PAID", "PARTIALLY_PAID"
```

### 1.2 Frontend — `api.ts`

Add to `portalAPI`:
```ts
listBookingPayments: (bookingId: string) =>
  client.get<PaymentDto[]>(`/portal/bookings/${bookingId}/payments`),
approveCashPayment: (bookingId: string, body: { amount: number; currency: string; notes?: string }) =>
  client.post<PaymentDto>(`/portal/bookings/${bookingId}/payments/cash-approve`, body),
addPayment: (bookingId: string, body: AddPaymentPayload) =>
  client.post<PaymentDto>(`/portal/bookings/${bookingId}/payments`, body),
```

Add to `types/api.ts`:
```ts
export interface AddPaymentPayload {
  amount: number
  currency: string
  method: 'CASH' | 'BANK_TRANSFER' | 'CHEQUE' | 'OTHER'
  transactionReference?: string
  notes?: string
}
```

Update `BookingDto`:
```ts
paymentMethod?: string
paymentStatus?: string
```

### 1.3 Frontend — `PortalBookingsPage.tsx`

**Booking row changes:**
- Show `paymentMethod` badge in the row (e.g. "Cash", "Card").
- Show `paymentStatus` badge (Pending / Paid).
- When `paymentMethod === 'CASH'` and `paymentStatus !== 'PAID'`:  
  Show a green **"Approve Cash"** button in the Actions column, gated with `portal:finance`.

**Booking detail modal changes:**
- Add a "Payments" section that calls `listBookingPayments(booking.id)` and lists them.
- Add **"Record Payment"** button at the bottom of the modal, gated with `portal:finance`.

**New `RecordPaymentModal` component** (`src/components/portal/RecordPaymentModal.tsx`):
```
Fields: amount, currency, method (select), transactionReference (optional), notes (optional)
Submit → portalAPI.addPayment(bookingId, payload)
```

**Cash approve flow:**
- Clicking "Approve Cash" opens a small confirm modal pre-filled with `booking.totalAmount` and `booking.currency`.
- On confirm → `portalAPI.approveCashPayment(bookingId, { amount, currency })` → refresh row.

**i18n keys to add** (both `en` and `ar`):
```
portalBookings.paymentMethod
portalBookings.paymentStatus
portalBookings.approveCash
portalBookings.approveCashConfirm
portalBookings.addPayment
portalBookings.paymentsHistory
portalBookings.method.cash
portalBookings.method.bankTransfer
portalBookings.method.cheque
portalBookings.method.other
portalBookings.status.paid
portalBookings.status.pending
portalBookings.status.partial
portalBookings.transactionRef
```

---

## Track 2 — New Listings Skip Approval; Images Still Require It

### Goal
When a provider creates or updates a listing the status should default to `ACTIVE` (no waiting for admin approval). Only image/media uploads remain subject to the existing approval workflow.

### 2.1 Backend

**Service creation/update** (`POST /portal/services`, `PATCH /portal/services/{id}`):
- Change default status from `PENDING_APPROVAL` → `ACTIVE` for providers updating their own listings.
- Image upload endpoint (`POST /portal/media/*`) keeps the existing `PENDING_APPROVAL` / media-review flow.

### 2.2 Frontend — `PortalListingFormPage.tsx`

- Change the initial state default: `useState<ServiceStatusDto>('ACTIVE')` (was `'PENDING_APPROVAL'`).
- Remove `'PENDING_APPROVAL'` from the `SERVICE_STATUSES` constant — providers should not be able to manually set this status.
- Updated list: `['ACTIVE', 'INACTIVE']`.

> Note: The media page (`PortalMediaPage.tsx`) already handles image upload + approval separately — no change needed there.

---

## Track 3 — Maps Integration

### Goal
Interactive maps showing:
1. Hotel and restaurant locations (static pins from provider lat/lng fields).
2. Real-time delivery tracking (WebSocket or polling for delivery-type bookings).

### 3.1 Dependencies

Use **Leaflet** with **React Leaflet** (open-source, no API key required for OpenStreetMap tiles):
```
npm install leaflet react-leaflet
npm install --save-dev @types/leaflet
```

Alternative: **Mapbox GL JS** if the project already has a Mapbox token (better styling, required for real-time).
Decision: start with Leaflet/OSM; swap tile provider later if needed.

### 3.2 Backend

**Providers list with coordinates**
```
GET /map/providers?type=HOTEL,RESTAURANT&lat=...&lng=...&radius=...
```
Response: `ProviderMapPinDto[]`
```java
record ProviderMapPinDto(
  String id,
  String name,
  String type,           // HOTEL | RESTAURANT | DELIVERY | etc.
  Double latitude,
  Double longitude,
  String thumbnailUrl,
  String status
)
```

**Delivery tracking (real-time)**
```
GET /map/delivery/{bookingId}       // current driver lat/lng + status
// or WebSocket topic: /topic/delivery/{bookingId}
```
- Driver app (out of scope for this plan) publishes location updates.
- For now: polling endpoint returning `{ latitude, longitude, status, updatedAt }`.

### 3.3 Frontend — New Pages

**`src/pages/admin/MapPage.tsx`** — Company dashboard map
- Full-screen Leaflet map.
- Layers panel: toggle Hotels / Restaurants / Delivery.
- Clicks a pin → popup with provider name, type, status, link to provider detail.
- Route: `/map` in `AppCompanyRoutes.tsx`, gated with `providers:read`.

**`src/pages/portal/PortalMapPage.tsx`** — Provider portal map (own listings only)
- Shows only listings owned by the signed-in provider.
- Route: `/portal/map` in `AppProviderRoutes.tsx`.

**`src/components/maps/ProviderMap.tsx`** — Shared map component
```tsx
interface Props {
  pins: ProviderMapPinDto[]
  center?: [number, number]
  zoom?: number
  liveDeliveryBookingId?: string  // enables polling for delivery pin
}
```

**`src/components/maps/DeliveryLivePin.tsx`**
- Polls `GET /map/delivery/{bookingId}` every 10 s.
- Animates marker to new position.

**Sidebar nav additions:**
- Company dashboard nav: "Map" entry (icon: map-pin).
- Provider portal nav: "Map" entry.

**i18n keys:**
```
nav.map
mapPage.title
mapPage.layerHotels
mapPage.layerRestaurants
mapPage.layerDelivery
mapPage.pinStatus
mapPage.viewProvider
mapPage.liveTracking
mapPage.lastUpdated
```

---

## Track 4 — Provider Reset Password (from Admin)

### Goal
Company admins can reset the password for any provider's primary account from the Edit Provider page, and from the Provider Staff detail view.

### 4.1 Status

`providersAPI.resetPassword(id)` already exists in `api.ts` (`POST /providers/{id}/reset-password`).

The **Edit Provider** page (`EditProviderPage.tsx`) and **Providers list** (`ProvidersPage.tsx`) already expose a reset-password button (confirmed via grep). This feature is mostly done on the company-admin side.

### 4.2 What is Missing

**Provider portal self-service reset**:
- A provider staff member who is locked out cannot currently reset their own password from within the portal.

**New: `PortalAccountPage.tsx`** (or extend `PortalProfilePage.tsx`)
- "Security" section with **"Change Password"** button → navigates to `/account/change-password` (already exists).
- "Forgot Password" link visible on the login page (already exists via `/forgot-password`).

**New endpoint for portal admin (owner) to reset a staff member's password:**
```
POST /portal/staff/{userId}/reset-password
Body: { newPassword: string }
```
- Guard: caller must be the portal owner or have `portal:manage`.

Add to `portalStaffAPI` in `api.ts`:
```ts
resetPassword: (userId: string, body: { newPassword: string }) =>
  client.post<void>(`/portal/staff/${userId}/reset-password`, body),
```

**`PortalStaffPage.tsx`** — add a **"Reset Password"** button in each staff row (alongside existing Edit/Remove), gated with `portal:manage`. Opens a modal with a new-password field.

**i18n keys:**
```
portalStaff.resetPassword
portalStaff.resetPasswordTitle
portalStaff.newPassword
portalStaff.resetPasswordSuccess
```

---

## Implementation Order

| # | Track | Effort | Depends on |
|---|-------|--------|------------|
| 1 | Track 2 — Listing auto-approve | XS (2 lines FE, 1 line BE) | — |
| 2 | Track 4 — Staff reset password | S (modal + endpoint) | — |
| 3 | Track 1 — Cash approve button | M (new modal, API calls) | BookingDto update |
| 4 | Track 1 — Manual payment entry | M (form + endpoint) | Cash approve done |
| 5 | Track 3 — Static maps | L (new dep, new pages) | Provider lat/lng data |
| 6 | Track 3 — Live delivery map | L (WebSocket/polling) | Driver app integration |

---

## Files Changed Summary

| File | Change |
|------|--------|
| `src/services/api.ts` | Add `listBookingPayments`, `approveCashPayment`, `addPayment` to `portalAPI`; add `resetPassword` to `portalStaffAPI` |
| `src/types/api.ts` | Add `paymentMethod/paymentStatus` to `BookingDto`; add `AddPaymentPayload`; add `ProviderMapPinDto` |
| `src/pages/portal/PortalBookingsPage.tsx` | Cash approve button, payments history in modal, Record Payment modal trigger |
| `src/pages/portal/PortalListingFormPage.tsx` | Default status `ACTIVE`, remove `PENDING_APPROVAL` from status list |
| `src/pages/portal/PortalStaffPage.tsx` | Add Reset Password button + modal per row |
| `src/components/portal/RecordPaymentModal.tsx` | New — manual payment form |
| `src/pages/admin/MapPage.tsx` | New — admin map with provider pins |
| `src/pages/portal/PortalMapPage.tsx` | New — portal map (own listings only) |
| `src/components/maps/ProviderMap.tsx` | New — shared Leaflet map component |
| `src/components/maps/DeliveryLivePin.tsx` | New — polling delivery marker |
| `src/apps/company/AppCompanyRoutes.tsx` | Add `/map` route |
| `src/apps/provider/AppProviderRoutes.tsx` | Add `/portal/map` route |
| `src/i18n/translations.ts` | New keys for all tracks |
| Backend: service handler | Default status `ACTIVE` for provider-created listings |
| Backend: new controllers | `POST /portal/bookings/{id}/payments/cash-approve`, `POST /portal/bookings/{id}/payments`, `GET /portal/bookings/{id}/payments`, `POST /portal/staff/{userId}/reset-password`, `GET /map/providers`, `GET /map/delivery/{bookingId}` |
