# Ziyara — V2 Implementation Plan
**Compiled:** 2026-06-30 · Covers all 17 items from `MISSING_ITEMS.md`

Every item includes the exact file, exact current code, exact fix, and a definition of done.
Items are ordered by execution priority — work through phases in sequence.

---

## Phase 0 — Financial Risk (Do Today)

Three items. One commit, < 30 minutes total.

---

### ITEM-01 · Discount Balance Race Condition
**Source:** FIX-01 · **File:** `core/src/main/java/com/ziyara/backend/application/service/PortalService.java` · **Lines:** 644–658

**Current code:**
```java
@Transactional
public DiscountResponse createProviderDiscount(UUID providerId, CreatePortalDiscountRequest req) {
    ensureProviderExists(providerId);
    BigDecimal debitAmount = req.getValue();
    // Atomic balance debit: succeeds only if available >= debitAmount
    int updated = jdbcTemplate.update(
            "UPDATE provider_discount_balance SET spent_amount = spent_amount + ?, updated_at = CURRENT_TIMESTAMP " +
            "WHERE provider_id = ? AND (allocated_amount - spent_amount) >= ?",
            debitAmount, providerId, debitAmount
    );
    if (updated == 0) {
        PortalDiscountBalanceResponse bal = getDiscountBalance(providerId);
        throw new com.ziyara.backend.application.exception.BusinessException(
                "Insufficient discount balance. Available: " + bal.getAvailableAmount() + " " + bal.getCurrency());
    }
```

**Problem:** Under concurrent requests at READ COMMITTED isolation, two transactions can both enter the UPDATE simultaneously. PostgreSQL row locks prevent a true double-debit here, but adding an explicit `SELECT ... FOR UPDATE` makes the lock pessimistic and avoids the post-UPDATE balance re-read for the error message (which is a second round-trip that could read a stale value).

**Fix — replace the entire debit block with pessimistic locking:**
```java
@Transactional
public DiscountResponse createProviderDiscount(UUID providerId, CreatePortalDiscountRequest req) {
    ensureProviderExists(providerId);
    BigDecimal debitAmount = req.getValue();

    // Lock the row for this transaction — blocks any concurrent debit until we commit.
    PortalDiscountBalanceResponse bal = jdbcTemplate.queryForObject(
            "SELECT currency, allocated_amount, spent_amount, " +
            "(allocated_amount - spent_amount) AS available_amount " +
            "FROM provider_discount_balance WHERE provider_id = ? FOR UPDATE",
            (rs, r) -> PortalDiscountBalanceResponse.builder()
                    .currency(rs.getString("currency"))
                    .allocatedAmount(rs.getBigDecimal("allocated_amount"))
                    .spentAmount(rs.getBigDecimal("spent_amount"))
                    .availableAmount(rs.getBigDecimal("available_amount"))
                    .build(),
            providerId);

    if (bal == null || bal.getAvailableAmount().compareTo(debitAmount) < 0) {
        String available = bal == null ? "0" : bal.getAvailableAmount().toPlainString();
        String currency  = bal == null ? ""  : bal.getCurrency();
        throw new com.ziyara.backend.application.exception.BusinessException(
                "Insufficient discount balance. Available: " + available + " " + currency);
    }

    jdbcTemplate.update(
            "UPDATE provider_discount_balance SET spent_amount = spent_amount + ?, " +
            "updated_at = CURRENT_TIMESTAMP WHERE provider_id = ?",
            debitAmount, providerId);
```

The rest of the method (`CreateDiscountRequest createReq = ...`) is unchanged.

**Definition of done:** Two concurrent POST `/portal/discounts` requests for the same provider with insufficient balance for both — exactly one returns 201, the other returns 400. No negative `(allocated_amount - spent_amount)` in the DB.

---

### ITEM-02 · Payout Request UI — Add Modal with Double-Submit Guard
**Source:** FIX-02 · **File:** `front/my-app/src/pages/portal/PortalEarningsPage.tsx`

**Current state:** The file (113 lines) displays earnings summary only. There is no payout request button or modal. `portalAPI.requestPayout({ amount, notes })` already exists in `api.ts` line 317.

**Fix — add the full payout request UI to the page:**

```tsx
import { useCallback, useEffect, useState } from 'react'
import { useLanguage } from '../../context/LanguageContext'
import { getApiErrorMessage, portalAPI } from '../../services/api'
import type { PortalEarningsDto } from '../../types/api'
import { Card } from '../../components/Card'
import { Modal } from '../../components/Modal'   // add this import

export function PortalEarningsPage() {
  const { t } = useLanguage()
  const [start, setStart] = useState('')
  const [end, setEnd] = useState('')
  const [data, setData] = useState<PortalEarningsDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // ── Payout modal state ────────────────────────────────────────────────────
  const [payoutOpen, setPayoutOpen] = useState(false)
  const [payoutAmount, setPayoutAmount] = useState('')
  const [payoutNotes, setPayoutNotes] = useState('')
  const [payoutSubmitting, setPayoutSubmitting] = useState(false)
  const [payoutError, setPayoutError] = useState<string | null>(null)

  const load = useCallback(() => { /* unchanged */ }, [start, end])

  useEffect(() => { load() }, [load])   // FIX-03 also applied here

  async function handlePayoutSubmit() {
    if (payoutSubmitting) return
    const amount = parseFloat(payoutAmount)
    if (!amount || amount <= 0) {
      setPayoutError(t('portalPages.payoutAmountRequired'))
      return
    }
    setPayoutSubmitting(true)
    setPayoutError(null)
    try {
      await portalAPI.requestPayout({ amount, notes: payoutNotes.trim() || undefined })
      setPayoutOpen(false)
      setPayoutAmount('')
      setPayoutNotes('')
      setError(null)
      load()
    } catch (e) {
      setPayoutError(getApiErrorMessage(e))
    } finally {
      setPayoutSubmitting(false)
    }
  }
```

Add the payout button next to the date-range Apply button, and the modal below the Card:

```tsx
{/* In the button row, after the Apply Range button: */}
{data?.availableForPayout != null && data.availableForPayout > 0 && (
  <button
    type="button"
    onClick={() => setPayoutOpen(true)}
    className="dashboard-btn-primary"
  >
    {t('portalPages.requestPayout')}
  </button>
)}

{/* Payout modal — place after the closing </Card> tag: */}
<Modal
  open={payoutOpen}
  onClose={payoutSubmitting ? () => {} : () => setPayoutOpen(false)}
  title={t('portalPages.requestPayoutTitle')}
  size="sm"
  footer={
    <>
      <button
        type="button"
        onClick={() => setPayoutOpen(false)}
        disabled={payoutSubmitting}
        className="dashboard-btn-secondary"
      >
        {t('confirm.cancelBtn')}
      </button>
      <button
        type="button"
        onClick={handlePayoutSubmit}
        disabled={payoutSubmitting || !payoutAmount}
        className="dashboard-btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {payoutSubmitting ? t('ui.submitting') : t('portalPages.submitPayout')}
      </button>
    </>
  }
>
  <div className="space-y-4">
    <div>
      <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
        {t('portalPages.payoutAmount')} ({data?.currency ?? 'USD'})
      </label>
      <input
        type="number"
        min="1"
        step="0.01"
        value={payoutAmount}
        onChange={(e) => setPayoutAmount(e.target.value)}
        className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
      />
    </div>
    <div>
      <label className="block text-sm font-medium text-slate-700 dark:text-slate-200">
        {t('portalPages.payoutNotes')}
      </label>
      <textarea
        rows={2}
        value={payoutNotes}
        onChange={(e) => setPayoutNotes(e.target.value)}
        className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100"
      />
    </div>
    {payoutError && <p className="text-sm text-red-600 dark:text-red-400">{payoutError}</p>}
  </div>
</Modal>
```

**i18n keys to add** in `front/my-app/src/i18n/translations.ts` under both `en` and `ar` `portalPages`:
```ts
requestPayout: 'Request Payout',           // ar: 'طلب صرف'
requestPayoutTitle: 'Request a Payout',    // ar: 'طلب صرف الأرباح'
payoutAmount: 'Amount',                    // ar: 'المبلغ'
payoutNotes: 'Notes (optional)',           // ar: 'ملاحظات (اختياري)'
submitPayout: 'Submit Request',            // ar: 'إرسال الطلب'
payoutAmountRequired: 'Please enter a valid amount',  // ar: 'يرجى إدخال مبلغ صحيح'
```

**Also add under `ui`:**
```ts
submitting: 'Submitting…',   // ar: 'جارٍ الإرسال…'
```

**Definition of done:** Clicking "Submit Request" twice quickly sends exactly one POST to `/portal/payout-request`. Button is visibly disabled and shows "Submitting…" during the request.

---

### ITEM-03 · PortalEarningsPage useEffect Dependency
**Source:** FIX-03 · **File:** `front/my-app/src/pages/portal/PortalEarningsPage.tsx` line 35–37

This is done as part of ITEM-02 above (`useEffect(() => { load() }, [load])`). If applying this fix alone without the payout modal:

```tsx
// Line 35 — change:
useEffect(() => {
  load()
}, [])

// To:
useEffect(() => {
  load()
}, [load])
```

**Definition of done:** Changing the date inputs triggers a new network request without a manual refresh.

---

## Phase 1 — Security & Deployment

---

### ITEM-04 · Uncomment the CI/CD Deploy Job
**Source:** FIX-04

**Step 1 — Add these secrets in GitHub → Settings → Secrets → Actions:**

| Secret | Value |
|---|---|
| `DEPLOY_HOST` | VPS IP or hostname |
| `DEPLOY_USER` | SSH user (e.g. `ubuntu`) |
| `DEPLOY_SSH_KEY` | Private key whose public key is in `~/.ssh/authorized_keys` on VPS |
| `DEPLOY_PORT` | SSH port (usually `22`) |
| `GHCR_TOKEN` | GitHub PAT with `read:packages` scope |

**Step 2 — Add this variable in GitHub → Settings → Variables → Actions:**

| Variable | Value |
|---|---|
| `DEPLOY_BASE_URL` | `https://your-domain.com` |

**Step 3 — File: `.github/workflows/deploy.yml`**

Find the commented-out deploy block starting at line 93 (`#  deploy:`) and uncomment it by removing the leading `#` from each line. The job content is already correct — it SSH-deploys via `appleboy/ssh-action` and updates the backend image tag in `.env`.

**Step 4 — Update `smoke-test` job's `needs` line:**
```yaml
# Find:
needs: build-push
# Change to:
needs: [build-push, deploy]
```

**Definition of done:** Pushing a tag `v*` triggers build → push → SSH deploy → smoke test. The workflow completes green end-to-end.

---

### ITEM-05 · Android Release Signing in CI
**Source:** FIX-05

**Step 1 — Generate the keystore once locally:**
```bash
keytool -genkey -v \
  -keystore ziyara-release.jks \
  -alias ziyara-release \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass YOUR_STORE_PASS -keypass YOUR_KEY_PASS \
  -dname "CN=Ziyara, OU=Mobile, O=Ziyara LLC, L=City, ST=State, C=US"

# Base64-encode for GitHub secret
base64 -w 0 ziyara-release.jks > ziyara-release.jks.b64
```

**Step 2 — Add to GitHub Secrets:**

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Contents of `ziyara-release.jks.b64` |
| `ANDROID_KEY_ALIAS` | `ziyara-release` |
| `ANDROID_KEYSTORE_PASS` | Store password |
| `ANDROID_KEY_PASS` | Key password |

**Step 3 — Create `.github/workflows/mobile.yml`:**
```yaml
name: Mobile Build

on:
  push:
    tags: ['v*']
  workflow_dispatch:

jobs:
  build-android:
    name: Build Android release AAB
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.x'
          channel: stable

      - name: Decode keystore
        run: |
          echo "${{ secrets.ANDROID_KEYSTORE_BASE64 }}" | base64 --decode \
            > mobile/android/ziyara-release.jks

      - name: Write local.properties
        run: |
          cat > mobile/android/local.properties << EOF
          ANDROID_KEYSTORE_PATH=../ziyara-release.jks
          ANDROID_KEY_ALIAS=${{ secrets.ANDROID_KEY_ALIAS }}
          ANDROID_KEYSTORE_PASS=${{ secrets.ANDROID_KEYSTORE_PASS }}
          ANDROID_KEY_PASS=${{ secrets.ANDROID_KEY_PASS }}
          EOF

      - name: Build AAB
        working-directory: mobile
        run: flutter build appbundle --release

      - uses: actions/upload-artifact@v4
        with:
          name: android-release-aab
          path: mobile/build/app/outputs/bundle/release/app-release.aab
```

**Definition of done:** CI produces a `.aab` artifact. Upload to Play Console without a signing-configuration rejection.

---

### ITEM-06 · Rate Limiting on Financial Portal Endpoints
**Source:** FIX-06 · **File:** `core/src/main/java/com/ziyara/backend/presentation/controller/PortalController.java`

The `@RateLimit` annotation uses fields `key` (String) and `maxPerMinute` (int). Add to three methods:

**Line 427 — payout request:**
```java
// Add above @PostMapping("/payout-request"):
@RateLimit(key = "portal-payout-request", maxPerMinute = 3)
@PostMapping("/payout-request")
@PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
```

**Line 457 — discount creation:**
```java
// Add above @PostMapping("/discounts"):
@RateLimit(key = "portal-discount-create", maxPerMinute = 10)
@PostMapping("/discounts")
@PreAuthorize(ApiAuthorizationExpressions.PORTAL_FINANCE)
```

**Line 170 — image upload:**
```java
// Add above @PostMapping(value = "/services/{id}/images/upload", ...):
@RateLimit(key = "portal-image-upload", maxPerMinute = 20)
@PostMapping(value = "/services/{id}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
```

Also add the import at the top of the file (if not already present):
```java
import com.ziyara.backend.application.annotation.RateLimit;
```

**Definition of done:** Sending 4 POST requests to `/portal/payout-request` within 60 seconds from the same authenticated session returns HTTP 429 on the 4th request.

---

### ITEM-07 · PII Encryption Key — Prod-Profile Startup Assertion
**Source:** 1.4 · **File:** `core/src/main/java/com/ziyara/backend/infrastructure/security/crypto/PiiCryptoService.java`

**Current state:** The constructor (line 30–40) validates key length when a key is provided but silently allows a blank key — leaving MFA secrets in plaintext. No profile check exists.

**Add these imports and the `@PostConstruct` method:**
```java
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import java.util.Arrays;

@Service
public class PiiCryptoService {

    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();
    private final Environment env;   // add this field

    public PiiCryptoService(
            @Value("${ziyara.pii.encryption-key-base64:}") String keyBase64,
            Environment env) {        // add env parameter
        this.env = env;
        if (keyBase64 == null || keyBase64.isBlank()) {
            this.secretKey = null;
            return;
        }
        byte[] raw = Base64.getDecoder().decode(keyBase64.trim());
        if (raw.length != 32) {
            throw new IllegalStateException(
                "ziyara.pii.encryption-key-base64 must decode to 32 bytes for AES-256");
        }
        this.secretKey = new SecretKeySpec(raw, AES);
    }

    @PostConstruct
    void validateProdKey() {
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (isProd && secretKey == null) {
            throw new IllegalStateException(
                "ZIYARA_PII_ENCRYPTION_KEY_BASE64 is required in the prod profile. " +
                "MFA secrets would be stored in plaintext without it.");
        }
    }
```

**Definition of done:** Starting the app with `--spring.profiles.active=prod` and no `ZIYARA_PII_ENCRYPTION_KEY_BASE64` env var throws `IllegalStateException` and the app refuses to start. Starting with a valid 32-byte base64 key starts normally.

---

### ITEM-08 · Rate Limiting on Core Write Endpoints
**Source:** 3.1

Add `@RateLimit` to these four controllers. The annotation is already wired via `Bucket4jRateLimitAspect`.

**`BookingController.java` — `POST /bookings` (line 111):**
```java
@RateLimit(key = "booking-create", maxPerMinute = 30)
@PostMapping
@Operation(summary = "Create booking", ...)
public ResponseEntity<ApiResponse<BookingResponse>> createBooking(...)
```

**`ReviewController.java` — `POST /reviews` (line 65):**
```java
@RateLimit(key = "review-create", maxPerMinute = 10)
@PostMapping
@PreAuthorize("isAuthenticated()")
@Operation(summary = "Submit review", ...)
public ResponseEntity<ApiResponse<ReviewResponse>> createReview(...)
```

**`ComplaintController.java` — `POST /complaints` (line 68):**
```java
@RateLimit(key = "complaint-create", maxPerMinute = 10)
@PostMapping
```

**`PaymentController.java` — `POST /payments` (line 41):**
```java
@RateLimit(key = "payment-initiate", maxPerMinute = 20)
@PostMapping
@PreAuthorize("isAuthenticated()")
```

Add `import com.ziyara.backend.application.annotation.RateLimit;` to each file that doesn't already have it.

**Definition of done:** `./gradlew test` passes. Sending 31 booking creation requests from the same IP within a minute returns 429 on the 31st.

---

## Phase 2 — UX Integrity (Before Provider Onboarding)

---

### ITEM-09 · Replace All `window.confirm()` / `alert()` Calls
**Source:** FIX-08

`ConfirmDialog` is at `front/my-app/src/components/ConfirmDialog.tsx`. It handles its own loading state internally — `onConfirm` receives a `() => Promise<void>`, no external loading flag needed.

Pattern for every replacement below:
1. Add `const [confirmXxx, setConfirmXxx] = useState(false)` (or `useState<T | null>(null)` when you need to pass data)
2. Replace the `window.confirm()` guard with `setConfirmXxx(true)` and `return`
3. Move the action body into the `onConfirm` async callback
4. Render `<ConfirmDialog open={confirmXxx} onClose={() => setConfirmXxx(false)} ... />`

---

#### `front/my-app/src/pages/admin/DeletedItemsPage.tsx` — line 92

```tsx
// Add state:
const [deleteTarget, setDeleteTarget] = useState<DeletedItemRow | null>(null)

// Replace (line ~90-94):
// BEFORE:
if (!window.confirm(`Permanently delete ${row.entityType} "${row.label}"? This cannot be undone.`)) return
await hardDeleteItem(row)

// AFTER:
setDeleteTarget(row)
// (move hardDeleteItem call into ConfirmDialog.onConfirm)

// Add JSX (before closing return fragment):
<ConfirmDialog
  open={!!deleteTarget}
  onClose={() => setDeleteTarget(null)}
  title={t('deletedItems.confirmDeleteTitle')}
  description={`Permanently delete ${deleteTarget?.entityType} "${deleteTarget?.label}"?`}
  confirmLabel={t('deletedItems.confirmDeleteBtn')}
  variant="danger"
  onConfirm={async () => {
    await hardDeleteItem(deleteTarget!)
    setDeleteTarget(null)
  }}
/>
```

---

#### `front/my-app/src/pages/admin/IntegrationsPage.tsx` — line 122

```tsx
// Add state:
const [revokeTarget, setRevokeTarget] = useState<Integration | null>(null)

// Replace (line 122):
// BEFORE:
if (!window.confirm(t('integrationsPage.confirmRevoke'))) return
await revokeIntegration(item)

// AFTER:
setRevokeTarget(item)

// Add JSX:
<ConfirmDialog
  open={!!revokeTarget}
  onClose={() => setRevokeTarget(null)}
  title={t('integrationsPage.confirmRevoke')}
  variant="danger"
  onConfirm={async () => {
    await revokeIntegration(revokeTarget!)
    setRevokeTarget(null)
  }}
/>
```

---

#### `front/my-app/src/pages/services/ServiceDetailMediaEditor.tsx` — lines 142, 204, 281, 339

Four separate destructive actions. One state variable per action:

```tsx
const [confirmRemoveImage, setConfirmRemoveImage] = useState<string | null>(null)   // holds image id
const [confirmDeleteSection, setConfirmDeleteSection] = useState<string | null>(null)
const [confirmRemoveMenuItem, setConfirmRemoveMenuItem] = useState<string | null>(null)
const [confirmDeleteRoomType, setConfirmDeleteRoomType] = useState<string | null>(null)

// Line 142 — remove image:
// BEFORE: if (!window.confirm('Remove this image?')) return
// AFTER:  setConfirmRemoveImage(imageId); return

// Line 204 — delete section:
// BEFORE: if (!window.confirm('Delete this section and all its items?')) return
// AFTER:  setConfirmDeleteSection(sectionId); return

// Line 281 — remove menu item:
// BEFORE: if (!window.confirm('Remove this menu item?')) return
// AFTER:  setConfirmRemoveMenuItem(itemId); return

// Line 339 — delete room type:
// BEFORE: if (!window.confirm('Delete this room type?')) return
// AFTER:  setConfirmDeleteRoomType(roomTypeId); return

// Add four <ConfirmDialog> elements in JSX, each wired to the corresponding
// state and async delete handler.
```

---

#### `front/my-app/src/pages/management/CurrencyRatesPage.tsx` — line 138

```tsx
const [deleteRateTarget, setDeleteRateTarget] = useState<CurrencyRate | null>(null)

// BEFORE: if (!window.confirm(t('currencyRatesPage.confirmDelete'))) return
// AFTER:  setDeleteRateTarget(rate); return

<ConfirmDialog
  open={!!deleteRateTarget}
  onClose={() => setDeleteRateTarget(null)}
  title={t('currencyRatesPage.confirmDelete')}
  variant="danger"
  onConfirm={async () => {
    await deleteRate(deleteRateTarget!.id)
    setDeleteRateTarget(null)
  }}
/>
```

---

#### `front/my-app/src/pages/management/UsersPage.tsx` — line 284

```tsx
const [confirmUserAction, setConfirmUserAction] = useState<{ user: UserRow; msg: string } | null>(null)

// BEFORE:
const ok = window.confirm(msg)
if (!ok) return
await doAction(user)

// AFTER:
setConfirmUserAction({ user, msg })

<ConfirmDialog
  open={!!confirmUserAction}
  onClose={() => setConfirmUserAction(null)}
  title={confirmUserAction?.msg ?? ''}
  variant="danger"
  onConfirm={async () => {
    await doAction(confirmUserAction!.user)
    setConfirmUserAction(null)
  }}
/>
```

---

#### `front/my-app/src/pages/management/StaffUserDetailPage.tsx` — lines 124 and 143

```tsx
const [confirmFreeze, setConfirmFreeze] = useState(false)
const [confirmDelete, setConfirmDelete] = useState(false)

// Line 124 — freeze/unfreeze:
// BEFORE: const ok = window.confirm(frozen ? t(...) : t(...)); if (!ok) return
// AFTER:  setConfirmFreeze(true); return

// Line 143 — delete user:
// BEFORE: const ok = window.confirm(t('staffUserPage.confirmDelete')); if (!ok) return
// AFTER:  setConfirmDelete(true); return

<ConfirmDialog
  open={confirmFreeze}
  onClose={() => setConfirmFreeze(false)}
  title={frozen ? t('staffUserPage.confirmUnfreeze') : t('staffUserPage.confirmFreeze')}
  variant="danger"
  onConfirm={async () => { await doFreeze(); setConfirmFreeze(false) }}
/>
<ConfirmDialog
  open={confirmDelete}
  onClose={() => setConfirmDelete(false)}
  title={t('staffUserPage.confirmDelete')}
  variant="danger"
  onConfirm={async () => { await doDelete(); setConfirmDelete(false) }}
/>
```

---

#### `front/my-app/src/pages/portal/PortalStaffPage.tsx` — line 173

```tsx
const [confirmRemoveStaff, setConfirmRemoveStaff] = useState<StaffMember | null>(null)

// BEFORE: if (!window.confirm(t('portalStaffPage.confirmRemove'))) return
// AFTER:  setConfirmRemoveStaff(member); return

<ConfirmDialog
  open={!!confirmRemoveStaff}
  onClose={() => setConfirmRemoveStaff(null)}
  title={t('portalStaffPage.confirmRemove')}
  variant="danger"
  onConfirm={async () => {
    await removeStaff(confirmRemoveStaff!.id)
    setConfirmRemoveStaff(null)
  }}
/>
```

**Definition of done:**
```bash
grep -rn "window\.confirm\|alert(" front/my-app/src/pages --include="*.tsx"
```
Returns zero results.

---

### ITEM-10 · Loading State on Provider Approve/Reject Buttons
**Source:** FIX-10 · **File:** `front/my-app/src/pages/management/ProvidersPage.tsx`

**Current handlers** (lines 86–110): `handleApprove` and `handleReject` have no loading guard. The buttons at lines 382–396 have no `disabled` attribute.

**Fix — add `actionLoading` state and update handlers and buttons:**

```tsx
// Add alongside other state declarations (line ~56):
const [actionLoading, setActionLoading] = useState<string | null>(null)  // holds provider id

// Replace handleApprove (line 86):
const handleApprove = async (id: string) => {
  if (actionLoading) return
  setActionLoading(id)
  setError(null)
  try {
    await providersAPI.approve(id)
    setProviders((prev) => prev.map((p) => (p.id === id ? { ...p, status: 'ACTIVE' } : p)))
    load()
  } catch (e) {
    setError(getApiErrorMessage(e))
  } finally {
    setActionLoading(null)
  }
}

// Replace handleReject (line 99):
const handleReject = async (id: string) => {
  if (actionLoading) return
  setActionLoading(id)
  setError(null)
  try {
    await providersAPI.reject(id)
    setProviders((prev) => prev.map((p) => (p.id === id ? { ...p, status: 'INACTIVE' } : p)))
    load()
  } catch (e) {
    setError(getApiErrorMessage(e))
  } finally {
    setActionLoading(null)
  }
}
```

**Update the Approve button (line 382) and Reject button (line 390):**
```tsx
<button
  type="button"
  onClick={() => handleApprove(p.id)}
  disabled={actionLoading === p.id}
  className="text-green-600 hover:underline dark:text-green-400 disabled:opacity-50 disabled:cursor-not-allowed"
>
  {actionLoading === p.id ? t('ui.processing') : t('providersPage.approve')}
</button>
<span className="mx-2 text-slate-300 dark:text-slate-600">|</span>
<button
  type="button"
  onClick={() => handleReject(p.id)}
  disabled={actionLoading === p.id}
  className="text-red-600 hover:underline dark:text-red-400 disabled:opacity-50 disabled:cursor-not-allowed"
>
  {actionLoading === p.id ? t('ui.processing') : t('providersPage.reject')}
</button>
```

Also add `processing: 'Processing…'` / `'جارٍ المعالجة…'` to `translations.ts` under `ui` if not present.

**Definition of done:** Clicking Approve disables both Approve and Reject for that provider row until the response returns. Rapid double-click sends exactly one request.

---

## Phase 3 — Configuration

---

### ITEM-11 · HikariCP Connection Timeout
**Source:** FIX-07 · **File:** `core/src/main/resources/application.yml` line 19

```yaml
# Change:
connection-timeout: 20000
# To:
connection-timeout: 8000
```

**Definition of done:** Under pool exhaustion, requests return a 503 within 8–9 seconds rather than 20.

---

### ITEM-12 · Enable Materialized View Refresh in Production
**Source:** FIX-13 · **Action:** On the production VPS, edit `/opt/ziyara/.env`

```env
# Change:
ZIYARA_REPORTING_MV_REFRESH_ENABLED=false
# To:
ZIYARA_REPORTING_MV_REFRESH_ENABLED=true
```

**Before enabling**, verify views exist and are populated:
```sql
-- Run inside the postgres container:
SELECT matviewname, ispopulated FROM pg_matviews;
-- If ispopulated = f, trigger a manual refresh first:
REFRESH MATERIALIZED VIEW CONCURRENTLY payment_summary_mv;
```

**Restart the backend** after editing `.env`:
```bash
docker compose --profile ipmode restart backend
```

**Definition of done:** The next morning (after 02:20 AM), `docker compose logs backend | grep "Refresh complete"` shows a successful run.

---

## Phase 4 — Testing Debt

---

### ITEM-13 · N+1 Query Detection Test
**Source:** 4.6 · **New file:** `core/src/test/java/com/ziyara/backend/infrastructure/persistence/QueryCountTest.java`

Add `datasource-proxy` to `build.gradle.kts` test dependencies:
```kotlin
testImplementation("net.ttddyy:datasource-proxy:1.10")
```

```java
package com.ziyara.backend.infrastructure.persistence;

import net.ttddyy.dsproxy.QueryCountHolder;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class QueryCountTest {

    @Autowired DataSource dataSource;
    @Autowired BookingRepositoryAdapter bookingRepo;
    @Autowired UserRepositoryAdapter userRepo;

    @BeforeEach
    void reset() { QueryCountHolder.clear(); }

    @Test
    void listBookings_doesNotExceedThreeQueries() {
        bookingRepo.findAll(new PageQuery(0, 20));
        long count = QueryCountHolder.getGrandTotal().getSelect();
        assertThat(count).as("listBookings should not exceed 3 SELECT queries").isLessThanOrEqualTo(3);
    }

    @Test
    void getUserWithRoles_doesNotExceedTwoQueries() {
        // load a known test user id from seed data or create one in @BeforeEach
        userRepo.findById(TestData.SEED_USER_ID);
        long count = QueryCountHolder.getGrandTotal().getSelect();
        assertThat(count).as("getUserWithRoles should not exceed 2 SELECT queries").isLessThanOrEqualTo(2);
    }
}
```

**Note:** You will need to wrap the `DataSource` bean with a `ProxyDataSource` in a `@TestConfiguration`. See the [datasource-proxy Spring Boot integration guide](https://github.com/ttddyy/datasource-proxy) for the `@Bean` setup.

**Definition of done:** `./gradlew test -PrunDockerTests` runs `QueryCountTest` green. Any future N+1 regression fails the build.

---

### ITEM-14 · Rate Limit Aspect Test
**Source:** 4.7 · **New file:** `core/src/test/java/com/ziyara/backend/infrastructure/security/RateLimitingAspectTest.java`

```java
package com.ziyara.backend.infrastructure.security;

import com.ziyara.backend.presentation.controller.BookingController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(BookingController.class)
class RateLimitingAspectTest {

    @Autowired MockMvc mvc;
    @MockBean  Bucket4jRateLimitAspect rateLimitAspect;   // or the service it delegates to
    @MockBean  /* BookingServiceApi */ bookingService;

    @Test
    @WithMockUser
    void whenRateLimitExceeded_returns429WithRetryAfterHeader() throws Exception {
        // Arrange: make the rate limiter reject the call
        when(rateLimitAspect.isAllowed(any())).thenReturn(false);

        mvc.perform(post("/api/v1/bookings")
                .contentType("application/json")
                .content("{}"))
           .andExpect(status().isTooManyRequests())
           .andExpect(header().exists("Retry-After"));
    }
}
```

Adjust mock setup to match how `Bucket4jRateLimitAspect` exposes its allow/deny logic (check the actual class for the correct method to stub).

**Definition of done:** The test fails if `Retry-After` is missing or if the status is not 429.

---

## Phase 5 — Low Priority Cleanup

---

### ITEM-15 · Verify IDOR Ownership Checks
**Source:** 1.3

`OwnershipEnforcementTest.java` exists, but verify that these four service methods enforce ownership before operating:

| Controller | Method | Service check to confirm |
|---|---|---|
| `PaymentController` | `GET /payments/{id}` | `payment.getUserId().equals(currentUserId)` or staff bypass |
| `ComplaintController` | `PUT /complaints/{id}` | `complaint.getCreatedBy().equals(currentUserId)` or staff |
| `ReviewController` | `DELETE /reviews/{id}` | `review.getCustomerId().equals(currentUserId)` or admin |
| `InternalTicketController` | `PUT /tickets/{id}` | caller is assignee or admin |

Run the existing test:
```bash
./gradlew test --tests "*OwnershipEnforcementTest"
```

If any `403` assertion fails, add the ownership check to the relevant service method using the pattern:
```java
if (!resource.getOwnerId().equals(currentUserId) && !isStaff()) {
    throw new UnauthorizedException("Access denied");
}
```

**Definition of done:** `OwnershipEnforcementTest` passes green with assertions covering all four resource types.

---

### ITEM-16 · Verify `PAYMENTS_WRITE` Scope
**Source:** 3.2

`POST /{id}/complete` and `POST /{id}/fail` are guarded by `hasAuthority('payments:write')`.

Check which `sys_roles` rows have `payments:write` in their permission set:
```sql
SELECT r.name, r.code
FROM sys_roles r
JOIN sys_role_permissions rp ON rp.role_id = r.id
JOIN sys_permissions p ON p.id = rp.permission_id
WHERE p.code = 'payments:write';
```

**Expected result:** Only `SUPER_ADMIN` and internal staff roles. If `CUSTOMER` or any provider role appears, remove `payments:write` from that role via the admin permissions UI or directly in the DB, and add a Flyway migration to enforce it permanently:

```sql
-- New migration V9X__restrict_payments_write.sql
DELETE FROM sys_role_permissions
WHERE permission_id = (SELECT id FROM sys_permissions WHERE code = 'payments:write')
  AND role_id IN (
      SELECT id FROM sys_roles WHERE code IN ('CUSTOMER', 'PROVIDER_MANAGER', 'PROVIDER_FINANCE')
  );
```

**Definition of done:** No customer or provider role holds `payments:write`. `PaymentControllerWebMvcTest` has a test asserting that a customer-role JWT receives 403 on `POST /{id}/complete`.

---

### ITEM-17 · `PUT` → `PATCH` for Partial-Update Endpoints
**Source:** 3.3

36 `@PutMapping` annotations exist across 19 controllers. This is a low-risk REST semantics fix. Change all partial-update endpoints from `@PutMapping` to `@PatchMapping`.

**Controllers to change and count of `@PutMapping` each:**

| Controller | Count |
|---|---|
| `UserController` | 5 |
| `ServiceController` | 5 |
| `PortalController` | 5 |
| `ServiceProviderController` | 4 |
| `RoleManagementController` | 2 |
| `AdminSystemSettingsController` | 2 |
| `ContentPageController` | 2 |
| `BookingController` | 1 |
| `ComplaintController` | 1 |
| `CurrencyController` | 1 |
| `DepartmentController` | 1 |
| `DiscountController` | 1 |
| `EmployeeController` | 1 |
| `InternalTicketController` | 1 |
| `NotificationController` | 1 |
| `PortalStaffController` | 1 |
| `ReviewController` | 1 |
| `TaxiBookingController` | 1 |
| `WebhookSubscriptionController` | 1 |

**Exceptions — keep as `@PutMapping`:**
- `AdminSystemSettingsController` endpoints that fully replace a settings object
- `AdminFeatureFlagsController` (already uses `@PatchMapping`)

**Mechanically:**
```bash
# In each file, change @PutMapping to @PatchMapping.
# The Swagger @Operation summaries that say "update" are already correct.
# Update any frontend API calls that use PUT to use PATCH instead.
```

**Frontend impact:** Search for `method: 'put'` or `.put(` in `front/my-app/src/services/api.ts` and change each call that corresponds to a now-PATCH endpoint to `.patch(`.

**Definition of done:** `grep -rn "@PutMapping" core/src/main/java` returns only endpoints that truly do full replacement (< 5 results). All existing `@WebMvcTest` tests updated to use `MockMvcRequestBuilders.patch(...)`.

---

### ITEM-18 · Remove Vacuous ArchUnit `ignoreDependency`
**Source:** 7.1 · **File:** `core/src/test/java/com/ziyara/backend/architecture/CleanArchitectureDddTest.java`

First, confirm the ignore is still present:
```bash
grep -n "dto.payment\|GatewayPaymentResponse\|application.dto.payment" \
  src/test/java/com/ziyara/backend/architecture/CleanArchitectureDddTest.java
```

If any result is returned, delete that `ignoreDependency(...)` line. The `application/dto/payment/` package was deleted in a previous cleanup — a live ignore for a nonexistent package masks future real violations in that namespace.

**Definition of done:** The grep above returns zero results. `./gradlew test --tests "*CleanArchitectureDddTest"` still passes.

---

## Execution Order Summary

| # | Item | File(s) | Effort |
|---|---|---|---|
| 01 | Discount balance FOR UPDATE | `PortalService.java` | 10 min |
| 02 | Payout modal + double-submit guard | `PortalEarningsPage.tsx`, `translations.ts` | 45 min |
| 03 | useEffect dependency | `PortalEarningsPage.tsx` | 1 min (done in 02) |
| 04 | Uncomment CI deploy job | `.github/workflows/deploy.yml` + secrets | 30 min |
| 05 | Android signing CI | `.github/workflows/mobile.yml` + secrets | 45 min |
| 06 | @RateLimit financial endpoints | `PortalController.java` | 5 min |
| 07 | PII prod-profile assertion | `PiiCryptoService.java` | 10 min |
| 08 | @RateLimit write endpoints | 4 controllers | 10 min |
| 09 | Replace alert/confirm (7 files) | 7 TSX files | 90 min |
| 10 | Approve/reject loading state | `ProvidersPage.tsx` | 15 min |
| 11 | HikariCP timeout 8000ms | `application.yml` | 1 min |
| 12 | Enable MV refresh in production | VPS `.env` | 5 min |
| 13 | N+1 query detection test | `QueryCountTest.java` (new) | 2 h |
| 14 | Rate limit aspect test | `RateLimitingAspectTest.java` (new) | 1 h |
| 15 | Verify IDOR ownership checks | audit + potential service fixes | 1 h |
| 16 | Verify PAYMENTS_WRITE scope | SQL query + optional migration | 30 min |
| 17 | PUT → PATCH (19 controllers) | 19 controllers + api.ts | 2 h |
| 18 | Remove vacuous ArchUnit ignore | `CleanArchitectureDddTest.java` | 2 min |

**Total estimated effort: ~10 developer-hours**

All items in Phase 0 (items 01–03) can ship as a single commit. Items 04–08 are a second commit. Items 09–10 are a third. The rest can each be their own PR or bundled by theme.
