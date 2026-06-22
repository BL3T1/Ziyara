# Ziyara Landing Site — Implementation Plan
**Based on:** `LANDING_UI_UX_REPORT.md` · 2026-05-31
**Scope:** Port 7070 — customer-facing booking surface
**Goal:** Raise overall score from 7.2 → 9.0+

---

## How to Read This Plan

Issues are grouped into **6 phases** ordered by user impact. Each phase can be shipped independently. Within each phase, items are ordered from highest to lowest impact. Every item includes:
- The exact files to change
- What to do
- Why it matters

Phases 1–3 are must-ship before any marketing push. Phases 4–6 are polish and completeness.

---

## Phase 1 — Critical Broken Flows
*Things that are actively wrong or misleading right now. Fix first.*

---

### 1.1 Change the checkout button label (CRITICAL)

**Files:** `LandingCheckoutPage.tsx`, `translations.ts`

**Problem:** The button says `"Confirm booking · USD 450"`. No payment is collected. This is a false promise.

**Fix:** Change the confirm button label to `"Reserve Now · USD X"` until a real payment gateway is integrated. Update the translation key `checkout.confirmPay` in both `en` and `ar`.

```ts
// translations.ts — en
checkout: {
  confirmPay: 'Reserve now',   // was: 'Confirm booking'
}

// translations.ts — ar
checkout: {
  confirmPay: 'احجز الآن',
}
```

In `LandingCheckoutPage.tsx`, the button renders:
```tsx
`${t('checkout.confirmPay')} · ${currency} ${subtotal.toLocaleString()}`
```
This is fine — it will now read "Reserve now · USD 450".

Also add a small note below the button explaining when payment is collected:
```tsx
<p className="text-center text-xs" style={{ color: 'var(--ink-faint)' }}>
  {t('checkout.paymentNote')}
</p>
```
Add `checkout.paymentNote` to translations:
- EN: `'Payment is collected at the property upon arrival.'`
- AR: `'يُسدَّد المبلغ في الموقع عند الوصول.'`

---

### 1.2 Fix Services Browse page — card links + skeleton (CRITICAL)

**Files:** `LandingServicesPage.tsx`, `landing-public.css`

**Problem A:** `<Link to={SERVICE_TYPE_TO_LINK[card.type] ?? '/services'}>` goes to the category list, not the specific service. A user clicking "Grand Beirut Hotel" lands on the hotels category.

**Fix A:** Change the link to the specific service detail URL:
```tsx
// Replace:
<Link to={SERVICE_TYPE_TO_LINK[card.type] ?? '/services'} ...>

// With:
<Link to={`/services/${(card.type ?? 'HOTEL').toLowerCase()}s/${card.id}`} ...>
// OR map correctly:
const typeToCategory: Record<string, string> = {
  HOTEL: 'hotels', RESORT: 'resorts', RESTAURANT: 'restaurants',
  TAXI: 'taxis', TRIP: 'trips',
}
<Link to={`/services/${typeToCategory[card.type] ?? 'hotels'}/${card.id}`} ...>
```

**Problem B:** No skeleton while `useLandingLiveData()` loads. Page is completely empty.

**Fix B:** Add a `loading` state export from `useLandingLiveData` and render 6 skeleton cards:

```tsx
// In LandingServicesPage.tsx
function ServiceCardSkeleton() {
  return (
    <div className="lp-glass-card animate-pulse !p-5">
      <div className="mb-4 h-44 w-full rounded-xl bg-slate-200/60" />
      <div className="h-5 w-3/4 rounded-md bg-slate-200/60" />
      <div className="mt-2 h-4 w-full rounded-md bg-slate-200/50" />
      <div className="mt-1 h-4 w-2/3 rounded-md bg-slate-200/50" />
    </div>
  )
}

// In the component:
const { services, loading } = useLandingLiveData()
// ...
{loading ? (
  <div className="mt-8 grid gap-6 md:grid-cols-3">
    {Array.from({ length: 6 }, (_, i) => <ServiceCardSkeleton key={i} />)}
  </div>
) : (
  // existing card grid
)}
```

---

### 1.3 Fix city chip navigation (HIGH)

**Files:** `LandingHomePage.tsx`

**Problem:** City chips navigate to `/services?city=Beirut` but `LandingServicesPage` ignores URL params. The city filter is never applied.

**Fix:** Navigate city chips to the hotels category with a city pre-filter, which `LandingServiceTypePage` does read from URL params:

```tsx
// In LandingHomePage.tsx — city chip onClick:
onClick={() => navigate(`/hotels?city=${encodeURIComponent(String(city))}`)}
```

This routes through `LandingServiceTypePage` (which handles `/hotels`) and reads `searchParams.get('city')` to pre-populate the city filter.

---

### 1.4 Fix deal tile links to carry city context (MEDIUM)

**Files:** `LandingHomePage.tsx`

**Problem:** All deal tiles link to generic `/services` with no city or category context.

**Fix:** Extract the service type from each deal tile and link to the appropriate category with city:

```tsx
// Deal tiles already have: { city, site, ... }
// Add a type field to each deal tile object:
const priceDeals = services
  .filter(item => typeof item.basePrice === 'number' && item.basePrice > 0)
  .slice(0, 4)
  .map(item => {
    const current = Number(item.basePrice ?? 0)
    const old = Math.round(current * 1.2)
    const categoryMap: Record<string, string> = {
      HOTEL: 'hotels', RESORT: 'resorts',
      RESTAURANT: 'restaurants', TAXI: 'taxis', TRIP: 'trips',
    }
    return {
      city: item.city || item.name,
      site: item.type,
      oldPrice: `$${old}`,
      newPrice: `$${current}`,
      diff: '20% lower',
      href: `/${categoryMap[item.type] ?? 'hotels'}?city=${encodeURIComponent(item.city || '')}`,
    }
  })

// Then in the render:
<Link key={deal.city} to={deal.href} className="lp-solution-card" ...>
```

---

### 1.5 Add hero image skeleton / fallback (HIGH)

**Files:** `ZiyaraHeroComposition.tsx`, `landing-public.css`

**Problem:** If `/ziyara-hero-reference.png` is missing or slow to load, the entire right side of the hero is blank with no placeholder.

**Fix:** Add a loading skeleton that shows while the image loads:

```tsx
// In ZiyaraHeroComposition.tsx
const [imgLoaded, setImgLoaded] = useState(false)
const [imgError, setImgError] = useState(false)

// Wrap the image:
<div className="lp-ziyara-hero__art-frame" style={{ position: 'relative' }}>
  {!imgLoaded && !imgError && (
    <div className="absolute inset-0 animate-pulse rounded-[22px] bg-gradient-to-br from-slate-200/60 to-slate-300/40" />
  )}
  <img
    src="/ziyara-hero-reference.png"
    className="lp-ziyara-hero__art"
    style={{ opacity: imgLoaded ? 1 : 0, transition: 'opacity 0.4s ease' }}
    onLoad={() => setImgLoaded(true)}
    onError={() => { setImgError(true); setImgLoaded(true) }}
    // ... rest of attributes
  />
  {imgError && <LandingHeroArt />}
</div>
```

---

## Phase 2 — Core Missing Features
*Features every booking platform must have. These block the site from feeling complete.*

---

### 2.1 Image Lightbox for Service Gallery (CRITICAL)

**Files:** `LandingServiceDetailPage.tsx`, `landing-public.css`, new `LandingImageLightbox.tsx`

**Problem:** Service images cannot be viewed full-size. This is the single biggest missing visual feature.

**Create `src/apps/landing/LandingImageLightbox.tsx`:**

```tsx
import { useEffect } from 'react'

interface Props {
  images: { src: string; alt: string }[]
  activeIndex: number
  onClose: () => void
  onNext: () => void
  onPrev: () => void
}

export function LandingImageLightbox({ images, activeIndex, onClose, onNext, onPrev }: Props) {
  // Close on Escape
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
      if (e.key === 'ArrowRight') onNext()
      if (e.key === 'ArrowLeft') onPrev()
    }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [onClose, onNext, onPrev])

  const current = images[activeIndex]
  if (!current) return null

  return (
    <div
      className="lp-lightbox"
      role="dialog"
      aria-modal
      aria-label="Image viewer"
      onClick={onClose}
    >
      <button className="lp-lightbox__close" onClick={onClose} aria-label="Close">✕</button>
      <button className="lp-lightbox__prev" onClick={(e) => { e.stopPropagation(); onPrev() }} aria-label="Previous">‹</button>
      <img
        src={current.src}
        alt={current.alt}
        className="lp-lightbox__img"
        onClick={(e) => e.stopPropagation()}
      />
      <button className="lp-lightbox__next" onClick={(e) => { e.stopPropagation(); onNext() }} aria-label="Next">›</button>
      <p className="lp-lightbox__counter">{activeIndex + 1} / {images.length}</p>
    </div>
  )
}
```

**Add to `landing-public.css`:**
```css
/* ─── Lightbox ───────────────────────────────────────────────────────── */
.lp-lightbox {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(6, 10, 18, 0.92);
  backdrop-filter: blur(12px);
  animation: lp-card-enter 0.2s var(--ease-out) both;
}
.lp-lightbox__img {
  max-width: 90vw;
  max-height: 85vh;
  object-fit: contain;
  border-radius: 12px;
  box-shadow: 0 32px 80px rgba(0,0,0,0.6);
}
.lp-lightbox__close {
  position: absolute; top: 20px; right: 24px;
  font-size: 28px; color: rgba(255,255,255,0.8);
  background: none; border: none; cursor: pointer;
  transition: color 0.15s; line-height: 1;
}
.lp-lightbox__close:hover { color: #fff; }
.lp-lightbox__prev, .lp-lightbox__next {
  position: absolute; top: 50%; transform: translateY(-50%);
  font-size: 48px; color: rgba(255,255,255,0.7);
  background: none; border: none; cursor: pointer;
  transition: color 0.15s; line-height: 1; padding: 0 16px;
}
.lp-lightbox__prev { left: 12px; }
.lp-lightbox__next { right: 12px; }
.lp-lightbox__prev:hover, .lp-lightbox__next:hover { color: #fff; }
.lp-lightbox__counter {
  position: absolute; bottom: 20px;
  color: rgba(255,255,255,0.6); font-size: 13px;
}
```

**Wire it into `LandingServiceDetailPage.tsx`:** Add a `lightboxIndex` state and open it when any image in the gallery is clicked. Pass an `onImageClick` prop down to `ServiceDetailView`.

---

### 2.2 Customer Reviews Section on Service Detail (CRITICAL)

**Files:** `LandingServiceDetailPage.tsx`, `landing-public.css`, `services/api.ts`

**Problem:** Zero social proof on the most decision-critical page.

**Add a reviews fetch** after the service loads:

```tsx
// In LandingServiceDetailPage.tsx
const [reviews, setReviews] = useState<ReviewDto[]>([])

// In the useEffect Promise.all:
Promise.resolve().then(() => {
  return Promise.all([
    servicesAPI.get(id),
    servicesAPI.getImages(id).catch(() => ...),
    servicesAPI.getMenu(id).catch(() => ...),
    servicesAPI.getReviews(id).catch(() => ({ data: [] })),  // add this
  ])
}).then(([svcRes, imgRes, menuRes, reviewRes]) => {
  // ...existing
  setReviews(Array.isArray(reviewRes.data) ? reviewRes.data : [])
})
```

**Add a reviews section below the menu:**
```tsx
{reviews.length > 0 && (
  <div className="mt-8">
    <h2 className="text-lg font-semibold mb-4" style={{ color: 'var(--ink-heading)' }}>
      {t('landingReviews.heading')} ({reviews.length})
    </h2>
    <div className="space-y-4">
      {reviews.slice(0, 5).map(review => (
        <div key={review.id} className="lp-glass-card !p-4">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-amber-500">{'★'.repeat(Math.round(review.rating ?? 0))}</span>
            <span className="text-sm font-medium" style={{ color: 'var(--ink-heading)' }}>
              {review.authorName ?? t('landingReviews.anonymous')}
            </span>
            <span className="text-xs ml-auto" style={{ color: 'var(--ink-faint)' }}>
              {review.createdAt ? new Date(review.createdAt).toLocaleDateString() : ''}
            </span>
          </div>
          <p className="text-sm leading-relaxed" style={{ color: 'var(--ink-muted)' }}>
            {review.comment}
          </p>
        </div>
      ))}
    </div>
  </div>
)}
```

**Add translation keys:**
```ts
landingReviews: {
  heading: 'Guest reviews',
  anonymous: 'Anonymous guest',
  noReviews: 'No reviews yet — be the first to book.',
}
// Arabic:
landingReviews: {
  heading: 'آراء الضيوف',
  anonymous: 'ضيف مجهول',
  noReviews: 'لا توجد تقييمات بعد — كن أول من يحجز.',
}
```

---

### 2.3 Global Toast Notification System (HIGH)

**Files:** new `src/apps/landing/LandingToast.tsx`, `landing-public.css`, `LandingShell.tsx`

**Problem:** Every page has its own inline success/error message — positionally inconsistent and easy to miss.

**Create `src/apps/landing/LandingToast.tsx`:**

```tsx
import { createContext, useCallback, useContext, useState, useRef } from 'react'

type ToastType = 'success' | 'error' | 'info'
interface Toast { id: number; message: string; type: ToastType }
interface ToastContextValue { toast: (message: string, type?: ToastType) => void }

const ToastContext = createContext<ToastContextValue>({ toast: () => {} })

export function useToast() { return useContext(ToastContext) }

export function LandingToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([])
  const counter = useRef(0)

  const toast = useCallback((message: string, type: ToastType = 'success') => {
    const id = ++counter.current
    setToasts(prev => [...prev, { id, message, type }])
    setTimeout(() => setToasts(prev => prev.filter(t => t.id !== id)), 4000)
  }, [])

  return (
    <ToastContext.Provider value={{ toast }}>
      {children}
      <div className="lp-toast-stack" aria-live="polite" aria-atomic="false">
        {toasts.map(t => (
          <div
            key={t.id}
            className={`lp-toast lp-toast--${t.type}`}
            role="status"
          >
            {t.type === 'success' && <span className="lp-toast__icon">✓</span>}
            {t.type === 'error' && <span className="lp-toast__icon">✕</span>}
            <span>{t.message}</span>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
```

**Wrap `LandingShell.tsx` children with `LandingToastProvider`:**
```tsx
// In LandingShell.tsx, wrap <Outlet /> with the provider
import { LandingToastProvider } from './LandingToast'
// In the return: wrap <div className="lp-www-inner"> content
```

**Add CSS in `landing-public.css`:**
```css
.lp-toast-stack {
  position: fixed;
  bottom: 24px;
  inset-inline-end: 24px;
  z-index: 200;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}
.lp-toast {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 18px;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 500;
  pointer-events: auto;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.3);
  box-shadow: 0 8px 32px rgba(40,55,68,0.18);
  animation: lp-card-enter 0.25s var(--ease-out) both;
  max-width: 340px;
}
.lp-toast--success {
  background: rgba(34,160,107,0.92);
  color: #fff;
}
.lp-toast--error {
  background: rgba(192,57,43,0.92);
  color: #fff;
}
.lp-toast--info {
  background: rgba(61,112,128,0.92);
  color: #fff;
}
.lp-toast__icon {
  font-size: 16px;
  font-weight: 700;
}
```

**Replace inline messages** in these components with `useToast()`:
- `LandingMyBookingsPage` — booking cancelled, voucher downloaded
- `LandingContactPage` — message sent success
- `LandingAccountPage` — profile saved, password changed
- `LandingCheckoutPage` — booking confirmed (alongside the full screen)

---

### 2.4 My Bookings Mobile Card View (HIGH)

**Files:** `LandingMyBookingsPage.tsx`, `landing-public.css`

**Problem:** 6-column table on a phone screen is unusable.

**Fix:** Render a card list below 640px instead of the table:

```tsx
// Replace the table wrapper with a responsive pattern:
<>
  {/* Desktop: table */}
  <div className="hidden sm:block overflow-x-auto rounded-2xl border" style={...}>
    <table className="w-full min-w-[640px] text-sm">
      {/* existing table code */}
    </table>
  </div>

  {/* Mobile: stacked cards */}
  <div className="space-y-3 sm:hidden">
    {rows.map(b => (
      <div key={b.id} className="lp-glass-card !p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="font-semibold truncate" style={{ color: 'var(--ink-heading)' }}>
              {b.serviceName ?? '—'}
            </p>
            <p className="mt-0.5 font-mono text-xs" style={{ color: 'var(--accent-teal)' }}>
              {b.bookingReference}
            </p>
          </div>
          <StatusBadge status={b.status} />
        </div>
        <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-sm" style={{ color: 'var(--ink-muted)' }}>
          {b.checkInDate && <span>{b.checkInDate}{b.checkOutDate ? ` → ${b.checkOutDate}` : ''}</span>}
          {b.totalAmount != null && (
            <span className="font-semibold" style={{ color: 'var(--ink-heading)' }}>
              {b.currency ?? 'USD'} {Number(b.totalAmount).toFixed(2)}
            </span>
          )}
        </div>
        <div className="mt-3 flex gap-2">
          <button onClick={() => handleDownloadVoucher(b.id)} ...>
            {t('landingMyBookings.actionDownloadVoucher')}
          </button>
          {CANCELLABLE.has(b.status.toUpperCase()) && (
            <button onClick={() => setCancelTarget(b)} ...>
              {t('landingMyBookings.actionCancel')}
            </button>
          )}
        </div>
      </div>
    ))}
  </div>
</>
```

---

### 2.5 Booking Detail View in My Bookings (HIGH)

**Files:** `LandingMyBookingsPage.tsx`

**Problem:** Users can't view full booking details — only cancel.

**Fix:** Make each booking row/card clickable to open a detail modal:

```tsx
// Add state:
const [detailBooking, setDetailBooking] = useState<BookingDto | null>(null)

// On row click: setDetailBooking(b)

// Add detail modal:
{detailBooking && (
  <div className="fixed inset-0 z-50 flex items-center justify-center p-4"
       style={{ background: 'rgba(10,18,30,0.45)', backdropFilter: 'blur(6px)' }}>
    <div className="lp-auth-card w-full max-w-lg" style={{ animation: 'lp-card-enter 0.25s var(--ease-out) both' }}>
      <div className="flex items-center justify-between mb-4">
        <h2 className="lp-h1 !text-xl">{t('landingMyBookings.detailTitle')}</h2>
        <button onClick={() => setDetailBooking(null)} style={{ color: 'var(--ink-faint)', background: 'none', border: 'none', cursor: 'pointer', fontSize: 20 }}>✕</button>
      </div>
      <dl className="space-y-3 text-sm">
        <div className="flex justify-between">
          <dt style={{ color: 'var(--ink-muted)' }}>{t('landingMyBookings.colRef')}</dt>
          <dd className="font-mono font-semibold" style={{ color: 'var(--accent-teal)' }}>{detailBooking.bookingReference}</dd>
        </div>
        <div className="flex justify-between">
          <dt style={{ color: 'var(--ink-muted)' }}>{t('landingMyBookings.colService')}</dt>
          <dd style={{ color: 'var(--ink-heading)' }}>{detailBooking.serviceName ?? '—'}</dd>
        </div>
        {detailBooking.checkInDate && (
          <div className="flex justify-between">
            <dt style={{ color: 'var(--ink-muted)' }}>{t('landingBooking.checkIn')}</dt>
            <dd>{detailBooking.checkInDate}{detailBooking.checkOutDate ? ` → ${detailBooking.checkOutDate}` : ''}</dd>
          </div>
        )}
        <div className="flex justify-between">
          <dt style={{ color: 'var(--ink-muted)' }}>{t('landingMyBookings.colStatus')}</dt>
          <dd><StatusBadge status={detailBooking.status} /></dd>
        </div>
        {detailBooking.totalAmount != null && (
          <div className="flex justify-between">
            <dt style={{ color: 'var(--ink-muted)' }}>{t('landingMyBookings.colTotal')}</dt>
            <dd className="font-semibold" style={{ color: 'var(--ink-heading)' }}>
              {detailBooking.currency ?? 'USD'} {Number(detailBooking.totalAmount).toFixed(2)}
            </dd>
          </div>
        )}
        {detailBooking.specialRequests && (
          <div>
            <dt className="mb-1" style={{ color: 'var(--ink-muted)' }}>{t('checkout.specialRequests')}</dt>
            <dd className="rounded-xl p-3 text-sm" style={{ background: 'rgba(61,112,128,0.06)' }}>
              {detailBooking.specialRequests}
            </dd>
          </div>
        )}
      </dl>
      <div className="mt-5 flex justify-end gap-2">
        {CANCELLABLE.has(detailBooking.status.toUpperCase()) && (
          <button onClick={() => { setCancelTarget(detailBooking); setDetailBooking(null) }} ...>
            {t('landingMyBookings.actionCancel')}
          </button>
        )}
        <button onClick={() => setDetailBooking(null)} className="lp-btn lp-btn-outline lp-btn-sm">
          {t('ui.close')}
        </button>
      </div>
    </div>
  </div>
)}
```

Add `landingMyBookings.detailTitle` → `'Booking details'` / `'تفاصيل الحجز'` to translations.

---

## Phase 3 — Form & Feedback Polish
*Fixes that make interactions feel complete and professional.*

---

### 3.1 Inline Discount Code Validation (HIGH)

**Files:** `LandingCheckoutPage.tsx`, `services/api.ts`, `translations.ts`

**Problem:** No feedback when entering a coupon code — discovered only on final submit failure.

**Fix:** Add an "Apply" button that calls the discount validation endpoint:

```tsx
// State:
const [couponStatus, setCouponStatus] = useState<'idle' | 'checking' | 'valid' | 'invalid'>('idle')
const [couponDiscount, setCouponDiscount] = useState<number>(0)

// Handler:
async function applyCode() {
  if (!coupon.trim()) return
  setCouponStatus('checking')
  try {
    const res = await discountsAPI.validate({ code: coupon, serviceId })
    const data = res.data as { valid: boolean; discountAmount?: number }
    if (data.valid) {
      setCouponStatus('valid')
      setCouponDiscount(data.discountAmount ?? 0)
    } else {
      setCouponStatus('invalid')
    }
  } catch {
    setCouponStatus('invalid')
  }
}

// UI:
<div className="flex gap-2">
  <input
    value={coupon}
    onChange={(e) => { setCoupon(e.target.value.toUpperCase()); setCouponStatus('idle') }}
    className="flex-1 rounded-xl border px-3 py-2.5 text-sm font-mono uppercase outline-none focus:ring-2"
    style={{ borderColor: couponStatus === 'valid' ? 'var(--accent-teal)' : couponStatus === 'invalid' ? '#e74c3c' : 'rgba(90,122,130,0.25)' }}
  />
  <button type="button" onClick={applyCode} disabled={!coupon || couponStatus === 'checking'} className="lp-btn lp-btn-outline lp-btn-sm shrink-0">
    {couponStatus === 'checking' ? '…' : t('checkout.applyCode')}
  </button>
</div>
{couponStatus === 'valid' && <p className="text-xs font-medium mt-1" style={{ color: 'var(--accent-teal)' }}>✓ {t('checkout.codeApplied', { amount: couponDiscount })}</p>}
{couponStatus === 'invalid' && <p className="text-xs font-medium mt-1" style={{ color: '#c0392b' }}>✕ {t('checkout.codeInvalid')}</p>}
```

Add translation keys: `checkout.applyCode`, `checkout.codeApplied`, `checkout.codeInvalid` (both EN + AR).

---

### 3.2 Contact Form Per-Field Validation (MEDIUM)

**Files:** `LandingContactPage.tsx`

**Problem:** The submit button is disabled but no field shows which input failed validation.

**Fix:** Add per-field error state and blur-triggered validation:

```tsx
const [fieldErrors, setFieldErrors] = useState<{ name?: string; email?: string; message?: string }>({})

function validateField(field: string, value: string) {
  if (field === 'name' && value.trim().length < 2) return t('landingContact.errName')
  if (field === 'email' && !value.includes('@')) return t('landingContact.errEmail')
  if (field === 'message' && value.trim().length < 10) return t('landingContact.errMessage')
  return undefined
}

// On each input: onBlur={() => setFieldErrors(prev => ({ ...prev, [field]: validateField(field, value) }))}
// Show errors below each field with the standard red text pattern
```

Add translations: `landingContact.errName`, `landingContact.errEmail`, `landingContact.errMessage`.

Also add `(optional)` label to the Subject field and visual required asterisk to Name, Email, Message.

---

### 3.3 Character Counters on All Textareas (MEDIUM)

**Files:** `LandingCheckoutPage.tsx`, `LandingContactPage.tsx`, `LandingMyBookingsPage.tsx`

**Fix:** Add a character counter below each textarea that has a max length:

```tsx
// Reusable pattern — add below any textarea:
<div className="flex justify-end mt-0.5">
  <span className="text-xs" style={{ color: value.length > MAX * 0.9 ? '#c0392b' : 'var(--ink-faint)' }}>
    {value.length} / {MAX}
  </span>
</div>
```

Apply to:
- `LandingCheckoutPage` special requests (max 500 chars — add `maxLength={500}`)
- `LandingContactPage` message (max 2000 chars — add `maxLength={2000}`)
- `LandingMyBookingsPage` cancel reason (max 500 chars — add `maxLength={500}`)

---

### 3.4 Fix Cancel Modal Button Labels (LOW)

**Files:** `LandingMyBookingsPage.tsx`, `translations.ts`

**Problem:** The "Go back / don't cancel" button uses `t('ui.cancel')` → "Cancel". A "Cancel" button inside a cancel dialog means "cancel the cancellation" — this is cognitively backwards.

**Fix:** Use a dedicated key:
```ts
// translations.ts:
landingMyBookings: {
  cancelKeepBooking: 'Keep my booking',  // EN
  // AR: 'الإبقاء على حجزي'
}
```
```tsx
// In the modal footer:
<button onClick={() => setCancelTarget(null)} className="lp-btn lp-btn-outline lp-btn-sm">
  {t('landingMyBookings.cancelKeepBooking')}
</button>
```

---

### 3.5 Translate Password Strength Labels (MEDIUM)

**Files:** `LandingSignUpPage.tsx`, `translations.ts`

**Problem:** Strength labels "Weak", "Fair", "Good", "Strong" are hardcoded English strings.

**Fix:** Move to translations:
```ts
// translations.ts:
landingAuth: {
  // add:
  strengthWeak: 'Weak',
  strengthFair: 'Fair',
  strengthGood: 'Good',
  strengthStrong: 'Strong',
}
// AR:
  strengthWeak: 'ضعيف',
  strengthFair: 'مقبول',
  strengthGood: 'جيد',
  strengthStrong: 'قوي',
```
```tsx
// In passwordStrength():
const { t } = useLanguage()
// Return t('landingAuth.strengthWeak') etc. based on score
```

---

### 3.6 Fix "Remember me" Default to Unchecked (LOW)

**Files:** `LandingLoginPage.tsx`

```tsx
// Change:
const [rememberMe, setRememberMe] = useState(true)
// To:
const [rememberMe, setRememberMe] = useState(false)
```

---

### 3.7 Contact Form: Replace Form with Success State (LOW)

**Files:** `LandingContactPage.tsx`

**Problem:** After sending, the empty form + success message coexist.

**Fix:** Replace the right-column form card content:
```tsx
{showSuccess ? (
  <div className="flex h-full flex-col items-center justify-center py-8 text-center gap-4">
    <div className="flex h-14 w-14 items-center justify-center rounded-full" style={{ background: 'rgba(34,160,107,0.12)' }}>
      <svg className="h-7 w-7" fill="none" stroke="#22a06b" strokeWidth="2.2" viewBox="0 0 24 24">
        <polyline points="20 6 9 17 4 12" />
      </svg>
    </div>
    <p className="lp-h1 !text-xl">{t('landingBusiness.contactSuccess')}</p>
    <button type="button" onClick={() => setShowSuccess(false)} className="lp-btn lp-btn-outline lp-btn-sm">
      {t('landingContact.sendAnother')}
    </button>
  </div>
) : (
  <form>{/* existing form */}</form>
)}
```
Add `landingContact.sendAnother` → `'Send another message'` / `'إرسال رسالة أخرى'`.

---

## Phase 4 — Layout & Structure Fixes
*Page-level structure issues that affect usability.*

---

### 4.1 Sticky Booking Panel on Desktop (HIGH)

**Files:** `LandingServiceDetailPage.tsx`, `landing-public.css`

**Problem:** On long restaurant or hotel pages, the booking CTA is unreachable without extensive scrolling.

**Fix:** On desktop (≥768px), change the service detail layout to a two-column grid with the booking panel as a sticky sidebar:

```tsx
// In LandingServiceDetailPage.tsx:
<div className="lp-service-detail-layout">
  <div className="lp-service-detail-main">
    <ServiceDetailView service={service} />
    {/* restaurant menu */}
  </div>
  <aside className="lp-service-detail-sidebar">
    <div className="lp-service-booking-sticky">
      <LandingServiceBookingPanel service={service} />
    </div>
  </aside>
</div>
```

```css
/* landing-public.css */
.lp-service-detail-layout {
  display: grid;
  gap: 28px;
  align-items: start;
}

@media (min-width: 768px) {
  .lp-service-detail-layout {
    grid-template-columns: 1fr 380px;
  }
  .lp-service-booking-sticky {
    position: sticky;
    top: calc(60px + 16px); /* navbar height + gap */
  }
}

@media (max-width: 767px) {
  .lp-service-detail-sidebar {
    order: -1; /* booking panel first on mobile */
  }
}
```

---

### 4.2 Fix Privacy & Terms Pages (HIGH)

**Files:** `LandingPrivacyPage.tsx`, `LandingTermsPage.tsx`, `landing-public.css`

**Problem:** Both pages use `lp-city-chip` (designed for one-line city names) as section containers for legal text.

**Fix A:** Replace `lp-city-chip` with a proper legal section container. Add to `landing-public.css`:
```css
.lp-legal-section {
  padding: clamp(20px, 2.5vw, 32px) clamp(20px, 2.5vw, 32px);
  border-radius: 20px;
  background: linear-gradient(145deg, rgba(255,255,255,0.42) 0%, rgba(255,255,255,0.18) 100%);
  border: 1px solid var(--border-glass);
}
.lp-legal-section h2 {
  margin: 0 0 10px;
  font-family: 'DM Sans', Inter, sans-serif;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--ink-heading);
}
.lp-legal-section p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--ink-muted);
}
```

**Fix B:** Add a sticky table of contents on desktop (same two-column layout as contact page):
- Left column: sticky list of anchor links (section titles)
- Right column: scrollable content sections

**Fix C:** Add "Last updated" date to both pages from translations:
```ts
landingBusiness: {
  lastUpdated: 'Last updated: {date}',
}
```

---

### 4.3 Expand the About Page (CRITICAL)

**Files:** `LandingAboutPage.tsx`, `translations.ts`

**Problem:** 3 numbers + 80 words does not build trust with a prospective user evaluating whether to book.

**Add sections to the About page:**

1. **Mission statement** — a 2-sentence "why we exist" block
2. **How it works** — 3-step visual (Browse → Book → Enjoy) using icons + short copy
3. **Coverage map** — a simple city list organized by region (or a visual grid)
4. **Platform numbers** — existing 3-stat pillars (already there, keep)
5. **A closing CTA** — "Ready to book?" with Browse services button

```tsx
// LandingAboutPage.tsx — add after stats pillars:

{/* How it works */}
<section className="lp-section lp-animate">
  <p className="lp-eyebrow lp-eyebrow--tight">{t('landingAbout.howEyebrow')}</p>
  <h2 className="lp-h1" style={{ marginTop: 8 }}>{t('landingAbout.howTitle')}</h2>
  <div className="lp-pillars" style={{ marginTop: 20 }}>
    {[
      { icon: '🔍', title: t('landingAbout.step1Title'), body: t('landingAbout.step1Body') },
      { icon: '✓', title: t('landingAbout.step2Title'), body: t('landingAbout.step2Body') },
      { icon: '🌟', title: t('landingAbout.step3Title'), body: t('landingAbout.step3Body') },
    ].map((step, i) => (
      <div key={i} className="lp-pillar">
        <div className="lp-pillar-icon" style={{ fontSize: 24 }}>{step.icon}</div>
        <h3>{step.title}</h3>
        <p>{step.body}</p>
      </div>
    ))}
  </div>
</section>

{/* Closing CTA */}
<div className="lp-section" style={{ textAlign: 'center' }}>
  <Link to="/services" className="lp-btn lp-btn-primary">
    {t('landingTraveler.ctaBrowse')}
  </Link>
</div>
```

Add translation keys `landingAbout.howEyebrow`, `howTitle`, `step1/2/3Title`, `step1/2/3Body` (EN + AR).

---

### 4.4 Improve Availability Status Visibility (MEDIUM)

**Files:** `LandingServiceBookingPanel.tsx`, `landing-public.css`

**Problem:** `text-xs` gray/green text is too subtle for information this critical.

**Fix:** Replace the tiny status line with a prominent status banner:

```tsx
// Replace the status text with a banner block:
{avail !== 'idle' && (
  <div className="lp-avail-banner" data-status={avail}>
    {avail === 'checking' && <span>{t('landingBooking.checkingAvailability')}</span>}
    {avail === 'available' && <span>✓ {t('landingBooking.available')}</span>}
    {avail === 'unavailable' && <span>✕ {availMsg || t('landingBooking.unavailable')}</span>}
  </div>
)}
```

```css
/* landing-public.css */
.lp-avail-banner {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}
.lp-avail-banner[data-status="checking"] {
  background: rgba(61,112,128,0.08);
  color: var(--accent-teal);
}
.lp-avail-banner[data-status="available"] {
  background: rgba(34,160,107,0.1);
  color: #22a06b;
}
.lp-avail-banner[data-status="unavailable"] {
  background: rgba(192,57,43,0.08);
  color: #c0392b;
}
```

---

### 4.5 Hero Hotspot Visual Affordance (MEDIUM)

**Files:** `ZiyaraHeroComposition.tsx`, `landing-public.css`

**Problem:** 4 invisible clickable areas on the hero image are undiscoverable.

**Fix:** Add a visible label with icon on each hotspot that appears on hover:

```css
/* landing-public.css */
.lp-ziyara-hero__hotspot {
  /* existing positioning */
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-bottom: 8px;
}
.lp-ziyara-hero__hotspot-label {
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.2s, transform 0.2s;
  background: rgba(6,10,18,0.7);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 4px 8px;
  border-radius: 8px;
  backdrop-filter: blur(8px);
  pointer-events: none;
}
.lp-ziyara-hero__hotspot:hover .lp-ziyara-hero__hotspot-label,
.lp-ziyara-hero__hotspot:focus-visible .lp-ziyara-hero__hotspot-label {
  opacity: 1;
  transform: translateY(0);
}
```

In `ZiyaraHeroComposition.tsx`, add a `<span className="lp-ziyara-hero__hotspot-label">` inside each hotspot with the appropriate service category name.

---

### 4.6 FAQ Accordion + Expand Content (MEDIUM)

**Files:** `LandingFaqPage.tsx`, `landing-public.css`, `translations.ts`

**Problem:** 3 always-visible items is not a real FAQ page.

**Fix A — Accordion:** Add open/close state per item:

```tsx
const [openIndex, setOpenIndex] = useState<number | null>(null)

{faqItems.map((item, i) => (
  <div key={item.question} className="lp-faq-item">
    <button
      type="button"
      className="lp-faq-trigger"
      onClick={() => setOpenIndex(openIndex === i ? null : i)}
      aria-expanded={openIndex === i}
    >
      <span>{item.question}</span>
      <span className={`lp-faq-chevron ${openIndex === i ? 'lp-faq-chevron--open' : ''}`}>›</span>
    </button>
    {openIndex === i && (
      <div className="lp-faq-answer">
        <p>{item.answer}</p>
      </div>
    )}
  </div>
))}
```

**Fix B — More questions:** Add 4 more FAQ items covering:
- "What payment methods are accepted?" → "Payment is collected at the property. Accepted methods vary by service provider."
- "How do I get my booking confirmation?" → "A confirmation email is sent immediately. You can also download a voucher from My Bookings."
- "Can I book for multiple guests?" → "Yes — use the guest counter in the booking panel. Maximum 20 guests per booking."
- "What if I need to cancel?" → "Cancel from My Bookings before your check-in date. Cancellation policies vary by service."

Add these keys to `translations.ts` in `landingBusiness` for both `en` and `ar`.

---

## Phase 5 — Loading States
*Every async page needs meaningful loading UI.*

---

### 5.1 Service Type Page Skeleton Grid (MEDIUM)

**Files:** `LandingServiceTypePage.tsx`

Replace `<p className="mt-4 lp-muted">{t('ui.loading')}</p>` with a skeleton grid matching the `ServiceGallery` layout:

```tsx
function GallerySkeletonGrid() {
  return (
    <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {Array.from({ length: 6 }, (_, i) => (
        <div key={i} className="lp-glass-card animate-pulse !p-0 overflow-hidden">
          <div className="h-44 w-full bg-slate-200/60" />
          <div className="p-4 space-y-2">
            <div className="h-5 w-3/4 rounded-md bg-slate-200/60" />
            <div className="h-4 w-full rounded-md bg-slate-200/50" />
            <div className="h-4 w-1/2 rounded-md bg-slate-200/50" />
          </div>
        </div>
      ))}
    </div>
  )
}

// In LandingServiceTypePage:
{loading ? <GallerySkeletonGrid /> : /* existing gallery */}
```

---

### 5.2 Checkout Loading Skeleton (MEDIUM)

**Files:** `LandingCheckoutPage.tsx`

Replace `<p className="lp-muted mt-4">{t('ui.loading')}</p>` with:

```tsx
function CheckoutSkeleton() {
  return (
    <div className="mt-6 space-y-4 animate-pulse">
      <div className="lp-sheet !p-5 space-y-3">
        <div className="h-5 w-1/2 rounded-md bg-slate-200/60" />
        <div className="h-4 w-1/3 rounded-md bg-slate-200/50" />
        <div className="h-16 w-full rounded-xl bg-slate-200/50 mt-3" />
      </div>
      <div className="lp-sheet !p-5 space-y-2">
        <div className="h-4 w-1/4 rounded-md bg-slate-200/60" />
        <div className="h-10 w-full rounded-xl bg-slate-200/50" />
      </div>
      <div className="h-14 w-full rounded-xl bg-slate-300/60" />
    </div>
  )
}
```

---

### 5.3 About Page Stat Skeletons (LOW)

**Files:** `LandingAboutPage.tsx`

Add a loading state from `useLandingLiveData`:
```tsx
const { totalCities, totalServices, averageBasePrice, loading } = useLandingLiveData()

// In stats:
value: loading ? '…' : (totalCities ? `${totalCities}+` : '—')
```

Add `loading: boolean` export to `useLandingLiveData.ts`.

---

### 5.4 Account Page Form Skeletons (LOW)

**Files:** `LandingAccountPage.tsx`

Replace `{t('ui.loading') || 'Loading…'}` with skeleton fields:
```tsx
{profileLoading ? (
  <div className="animate-pulse space-y-4">
    <div className="grid grid-cols-2 gap-3">
      <div className="h-10 rounded-xl bg-slate-200/60" />
      <div className="h-10 rounded-xl bg-slate-200/60" />
    </div>
    <div className="h-10 rounded-xl bg-slate-200/60" />
  </div>
) : (
  <form>{/* existing form */}</form>
)}
```

---

## Phase 6 — Design System Cleanup
*Consistency and maintainability improvements.*

---

### 6.1 Replace Hardcoded Colors with CSS Tokens (MEDIUM)

**Files:** `LandingSignUpPage.tsx`, `LandingHeroArt.tsx`, `ZiyaraHeroComposition.tsx`, `landing-public.css`

**Add to `landing-public.css` inside `.lp-www-root`:**
```css
--status-weak:   #e74c3c;
--status-fair:   #e67e22;
--status-good:   #eab308;
--status-strong: #22a06b;
```

**In `LandingSignUpPage.tsx`:**
```tsx
// Replace hardcoded hex colors in passwordStrength():
if (score <= 1) return { score, label: ..., color: 'var(--status-weak)' }
if (score <= 2) return { score, label: ..., color: 'var(--status-fair)' }
if (score <= 3) return { score, label: ..., color: 'var(--status-good)' }
return { score, label: ..., color: 'var(--status-strong)' }
```

**In `LandingHeroArt.tsx`:** Replace `fill="#ac9e78"` with `fill="var(--accent-tan)"` and `stroke="#ac9e78"` similarly.

**In `ZiyaraHeroComposition.tsx`:** Replace `stroke="#3d7080"` with `stroke="var(--accent-teal)"`.

---

### 6.2 Standardize Empty States (MEDIUM)

**Files:** `LandingServicesPage.tsx`, `LandingServiceTypePage.tsx`, `landing-public.css`

Create a shared `LandingEmptyState` component used consistently across all landing pages:

```tsx
// src/apps/landing/LandingEmptyState.tsx
interface Props { icon?: string; title: string; body?: string; action?: React.ReactNode }
export function LandingEmptyState({ icon = '🔍', title, body, action }: Props) {
  return (
    <div className="lp-empty-state">
      <span className="lp-empty-state__icon">{icon}</span>
      <h3 className="lp-empty-state__title">{title}</h3>
      {body && <p className="lp-empty-state__body">{body}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  )
}
```

```css
.lp-empty-state {
  padding: clamp(40px, 6vw, 64px) 24px;
  text-align: center;
  border-radius: 24px;
  background: linear-gradient(145deg, rgba(255,255,255,0.38) 0%, rgba(255,255,255,0.14) 100%);
  border: 1px solid var(--border-glass);
}
.lp-empty-state__icon { font-size: 40px; line-height: 1; }
.lp-empty-state__title {
  margin: 16px 0 0;
  font-family: 'DM Sans', Inter, sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--ink-heading);
}
.lp-empty-state__body {
  margin: 8px 0 0;
  font-size: 14px;
  color: var(--ink-muted);
  max-width: 36ch;
  margin-inline: auto;
}
```

Use `LandingEmptyState` in: Services Browse, Service Type pages, My Bookings empty, FAQ if no items, Contact success state.

---

### 6.3 Form Field Label Consistency (LOW)

**Files:** `LandingSignUpPage.tsx`, `LandingContactPage.tsx`

Both pages use raw Tailwind classes for field labels:
```tsx
className="mb-1 block text-xs font-semibold uppercase tracking-wide"
```
instead of the defined `lp-field-label` class.

**Fix:** Replace all inline label class strings with `className="lp-field-label"` across all landing forms.

---

### 6.4 Extract Inline Styles to CSS Classes (LOW)

**Files:** `LandingHomePage.tsx`, `landing-public.css`

The homepage has ~30 instances of `style={{ marginTop: 20 }}`, `style={{ fontSize: '...', marginBottom: 0 }}` etc. These should be CSS modifier classes:

```css
/* Add utility classes to landing-public.css: */
.lp-section--tight  { margin-top: 28px; }
.lp-section--flush  { margin-top: 16px; }
.lp-title--sm { font-size: clamp(1.5rem, 2.2vw, 2rem); margin-bottom: 0; }
.lp-title--xs { font-size: clamp(1.35rem, 2vw, 1.75rem); margin-bottom: 12px; }
```

Replace `style={{ marginTop: 20 }}` on `lp-deal-grid` with `className="lp-deal-grid lp-section--tight"`, etc.

---

## Translation Additions Summary

All new keys required across phases — add to **both `en` and `ar`** in `translations.ts`:

| Section | New Keys (EN) |
|---|---|
| `checkout` | `paymentNote`, `applyCode`, `codeApplied`, `codeInvalid` |
| `landingMyBookings` | `detailTitle`, `cancelKeepBooking` |
| `landingAuth` | `strengthWeak`, `strengthFair`, `strengthGood`, `strengthStrong` |
| `landingContact` | `errName`, `errEmail`, `errMessage`, `sendAnother` |
| `landingAbout` | `howEyebrow`, `howTitle`, `step1Title`, `step1Body`, `step2Title`, `step2Body`, `step3Title`, `step3Body` |
| `landingBusiness` | 4 new FAQ items (Q+A for faqFour through faqSeven), `lastUpdated` |
| `landingReviews` | `heading`, `anonymous`, `noReviews` |

---

## Implementation Order (Recommended Sprint Sequence)

### Sprint 1 (1–2 days) — Critical Broken
- 1.1 Fix checkout button label + add payment note
- 1.2 Fix Services Browse card links + add skeleton
- 1.3 Fix city chip navigation
- 3.4 Fix cancel modal button label
- 3.6 Fix remember-me default

### Sprint 2 (2–3 days) — Major UX Features  
- 2.1 Image lightbox
- 2.2 Customer reviews on service detail
- 2.4 My Bookings mobile card view
- 2.5 My Bookings detail modal

### Sprint 3 (2 days) — Toast System + Form Polish
- 2.3 Global toast system
- 3.1 Inline discount code validation
- 3.2 Contact form per-field validation
- 3.3 Character counters
- 3.5 Translate password strength labels
- 3.7 Contact form success state

### Sprint 4 (2–3 days) — Layout Fixes
- 4.1 Sticky booking panel sidebar layout
- 4.2 Fix Privacy/Terms pages
- 4.3 Expand About page
- 4.4 Availability status banner
- 4.5 Hero hotspot labels
- 4.6 FAQ accordion + more questions

### Sprint 5 (1 day) — Loading States
- 5.1 Service type page skeleton grid
- 5.2 Checkout loading skeleton
- 5.3 About page stat skeletons
- 5.4 Account page form skeletons
- 1.5 Hero image skeleton fallback

### Sprint 6 (1 day) — Design System Cleanup
- 6.1 Replace hardcoded colors with CSS tokens
- 6.2 Standardize empty states
- 6.3 Form field label consistency
- 6.4 Extract inline styles to CSS classes

---

## Expected Score Improvement After Each Phase

| After Phase | Expected Overall Score |
|---|---|
| Baseline | 7.2 / 10 |
| Phase 1 | 7.8 — no more broken flows |
| Phase 2 | 8.4 — lightbox, reviews, toasts, mobile bookings |
| Phase 3 | 8.7 — forms feel complete |
| Phase 4 | 9.0 — layout and structure solid |
| Phase 5 | 9.1 — loading states everywhere |
| Phase 6 | 9.2 — design system unified |

---

*Plan authored from `LANDING_UI_UX_REPORT.md`. Every item is traceable to a specific report finding. Implementation should be validated against the report's priority list after each sprint.*
