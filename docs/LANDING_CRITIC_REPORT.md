# Ziyara Landing Site — Critic Report
**Port 7070 · B2C Customer-Facing Booking Surface**
**Date reviewed:** 2026-05-31
**Reviewed by:** External critic (first-time user perspective)

---

## Executive Summary

Ziyara's landing site has a strong visual foundation — the glassmorphism aesthetic with warm neutral tones (`#f5f2ec` sand, `#3d7080` teal, `#a07b56` tan) is distinctive, tasteful, and genuinely looks premium. The GSAP entrance animations add polish. The tech is solid.

But once you move past the homepage, cracks appear fast. Pages outside the main shell are stripped of navigation entirely. Several features show fake/hardcoded data presented as real. i18n is half-finished. The "My Bookings" page looks like it was designed by a completely different team. The checkout has no payment form. The footer's links point to wrong pages.

The site looks like a confident draft that hasn't been walked through end-to-end by a real user.

---

## Overall Ratings

| Category | Score | Verdict |
|---|---|---|
| Visual Design & Aesthetics | **8.5 / 10** | Genuinely beautiful, cohesive warm palette |
| Animation & Motion | **7.5 / 10** | Polished on load, but JS-dependent with no fallback |
| Navigation & Information Architecture | **4.5 / 10** | Logged-in state is chaotic; orphaned pages have no nav |
| Booking Workflow | **5.0 / 10** | Functional skeleton, but fake data breaks trust |
| Design Consistency | **4.0 / 10** | My Bookings and checkout pages look like a different site |
| Internationalization | **4.5 / 10** | Dozens of hardcoded English strings throughout |
| Mobile Experience | **5.5 / 10** | No hamburger menu; some pages overflow on small screens |
| Content Clarity | **5.5 / 10** | Footer links mislabeled; empty pages; thin About section |
| Error Handling & Feedback | **6.5 / 10** | Error states exist but loading states are bare |
| **Overall** | **5.7 / 10** | Strong bones, not yet production-ready for real users |

---

## Page-by-Page Analysis

---

### 1. Homepage (`/`)

**What works well:**
- The hero composition is stunning. The glassmorphism card, gradient background, warm-to-teal color range, and GSAP staggered entrance animation create a premium first impression.
- The stats strip (active services, cities covered, average price) is a smart replacement for a fake search bar.
- The deal tiles, city chips, and trust pillar sections create a sense of a real, populated platform.
- The partner CTA band at the bottom is well-styled and ends the page cleanly.

**Issues:**

**[CRITICAL] Hero is invisible until JS runs.**
All hero elements (`lp-eyebrow`, `#hero-heading`, `.lp-hero-lede`, `.lp-cta`, `.lp-ziyara-hero__visual`) start at `opacity: 0` via CSS and are revealed by GSAP. If the script is slow, blocked, or fails, the user stares at a blank hero card. There is no `<noscript>` fallback and no CSS-only reveal path.

**[HIGH] Stats label mismatch.**
When `averageBasePrice` is available, the third stat item displays the average price with the label `statsRatingLabel` — which in context likely says "Avg. Rating" or similar. You are showing a dollar figure under a "rating" label. This is a data/label mismatch that confuses users.

```tsx
// landing/LandingHomePage.tsx ~line 117
// averageBasePrice branch uses statsRatingLabel — should be a price label, not a rating label
```

**[HIGH] Deal cards and city chips are not clickable.**
Both the deal tiles (`.lp-solution-card`) and city chips (`.lp-city-chip`) have hover lift effects, implying they are interactive. But they have no `onClick`, no `<Link>`, and no `href`. Users will click them expecting to navigate and nothing will happen. This is a broken affordance.

**[MEDIUM] "20% lower" deal discount is fabricated.**
The price calculation is:
```ts
const old = Math.round(current * 1.2)
// then displays: old = "original", current = "deal price", diff = "20% lower"
```
This always shows exactly 20% off, computed backwards from the listed price. It is not a real discount — it is marketing theatre that could mislead users. If no real discount data exists, don't show a fake one.

**[MEDIUM] Partner CTA buttons are confusing.**
The partner band at the bottom says "Are you a business? Partner with us." but the two CTA buttons are "Sign In" (→ `/login`) and "Contact us" (→ `/contact`). A prospective partner clicking "Sign In" will land on the customer login page with no path to becoming a partner. The primary CTA for partners should be "Become a Partner" → `/contact`, not a generic sign-in.

---

### 2. Navigation (Shell)

**What works well:**
- Sticky frosted-glass navbar is visually excellent.
- Active link indicator (`.lp-nav-active`) is subtle but clear.
- Language and currency switchers are accessible from the nav.

**Issues:**

**[CRITICAL] Logged-in navbar is severely overcrowded.**
When a `user` role is authenticated, the nav actions row shows:
1. Truncated display name
2. "My Bookings" button
3. "Account" button
4. "Browse" button
5. "Log out" button
6. Currency switcher
7. Language toggle

That is 7 elements in a flex row. On any viewport below 1200px these collide, overlap, or push nav links to a second row. An authenticated user's nav is more chaotic than the unauthenticated one.

**[CRITICAL] No mobile navigation menu.**
Below ~960px the nav links (`lp-nav-links`) wrap to a full-width centered row below the logo and action buttons, creating a 3-row header on tablets. On phones below 640px this becomes unusable — no hamburger drawer, no collapse. The entire nav is exposed as a wrapping flex mess.

**[HIGH] My Bookings / Checkout / Account have NO shell.**
These three routes are excluded from `<LandingShell>` in `AppLandingRoutes.tsx`:
```tsx
<Route path="/checkout" element={<LandingCheckoutPage />} />
<Route path="/my-bookings" element={<LandingMyBookingsPage />} />
<Route path="/account" element={<LandingAccountPage />} />
```
A logged-in user on `/my-bookings` has **no navbar, no footer**. The only way to leave is the browser back button or manually typing a URL. This is a critical navigation dead-end for your most valuable users.

**[MEDIUM] Language toggle duplicated.**
The language toggle appears in the navbar AND in the footer. The footer one (`<div class="lang-toggle" aria-hidden>`) has `aria-hidden` — it is purely decorative and does nothing when clicked. It should either be functional or removed.

---

### 3. Services Browse (`/services`)

**Issues:**

**[HIGH] Only 6 services shown, with no "View all" link.**
```tsx
const cards = services.slice(0, 6)
```
The page hard-caps at 6 cards and provides no pagination, no "show more," and no way to see the rest. A user browsing services sees a seemingly random 6 and has no idea there are more.

**[MEDIUM] Cards link to the category type page, not to service details.**
Each card's CTA (→) goes to `/hotels`, `/resorts`, etc. rather than to the specific service. The card shows a specific listing (name, image, description) but clicking it takes the user to a generic list page. This breaks the mental model — the card looks specific, but behaves as a category link.

**[LOW] Empty state is generic.**
When the API returns 0 services, the fallback card shows the generic `landingBusiness.servicesTitle` and `landingBusiness.heroBody` — marketing copy that tells the user nothing about why the list is empty or when to come back.

---

### 4. Service Type Pages (`/hotels`, `/resorts`, etc.)

**What works well:**
- The filter bar (search, city dropdown, price range, star rating) is a genuinely useful feature.
- URL-synced filters (filters persist on refresh/share) is a great UX touch.
- "Clear filters" button only appears when filters are active — good.

**Issues:**

**[HIGH] Filter bar is entirely in English — not translated.**
Every label and placeholder in `ServicesFilterBar.tsx` is a hardcoded English string:
- `"Search"`, `"Name or description…"`, `"City"`, `"All cities"`
- `"Min price"`, `"Max price"`, `"Min stars"`, `"Any"`, `"Clear"`

These bypass the i18n system completely. An Arabic user sees English filter labels on an otherwise Arabic page.

**[HIGH] Hardcoded English strings in results area.**
```tsx
// LandingServiceTypePage.tsx line 127
'No results match your filters.'  // hardcoded

// line 137
`${filtered.length} of ${services.length} results`  // hardcoded
```

**[MEDIUM] Price filter has no currency indicator.**
The min/max price fields have no indication of what currency to enter. A user might type `100` expecting Lebanese pounds when the listings are in USD.

**[LOW] Loading state is one line of muted text.**
```tsx
<p className="mt-4 lp-muted">{t('ui.loading')}</p>
```
No skeleton, no spinner. For a page that fetches live service data, this is too minimal.

---

### 5. Service Detail Page (`/services/:category/:id`)

**What works well:**
- Breadcrumb navigation with `Services / Hotels / Hotel Name` is clear.
- Menu section rendering for restaurants (with images, prices, descriptions) is well-structured.
- Dynamic page `<title>` and meta description for SEO.

**Issues:**

**[HIGH] Double back navigation — redundant and confusing.**
The page renders both:
1. A `<nav>` breadcrumb with `Services / Hotels / Hotel Name`
2. A `← Back to list` button immediately below it

Two ways to go back on the same page is redundant. Pick one.

**[HIGH] Hardcoded English strings.**
```tsx
"Services"  // breadcrumb root label
"← Back to list"  // back button
"Menu"  // restaurant menu heading
"No menu sections yet."  // empty menu state
"Back"  // error state button
"Could not load service."  // error heading
```
None of these go through `t()`.

**[HIGH] Loading state is invisible.**
```tsx
if (loading) return <p className="lp-muted">Loading…</p>
```
When navigating to a service detail, the page briefly flashes a tiny muted paragraph before content appears. No skeleton, no card placeholder, no spinner. On a slow connection this is a bad experience.

**[MEDIUM] Room selector uses hardcoded fake data.**
```tsx
// LandingServiceBookingPanel.tsx line 12
const BOOKED_ROOM_INDICES = new Set([2, 5, 8, 9, 15])
```
Rooms 3, 6, 9, 10, and 16 are **always shown as booked** regardless of the actual property, actual dates, or actual availability. This is hardcoded fake data rendered as if it were real inventory. A user selecting dates and seeing "rooms taken" is being shown fiction. This fundamentally breaks trust if anyone notices.

**[MEDIUM] Taxi fare estimate is local math, not real.**
```tsx
const mult = taxiType === 'VIP' ? 1.45 : taxiType === 'Van' ? 1.25 : 1
return Math.round(Math.max(basePrice, 12) * mult)
```
The fare estimate is calculated entirely in the browser with a hardcoded multiplier. There is no routing API, no distance calculation, and no acknowledgment that this is an estimate. It is displayed as a price (`${service.currency} ${taxiEstimate}`), not as "~estimated".

**[LOW] No image gallery lightbox.**
Service images are displayed via `ServiceDetailView` / `ServiceGallery` but there is no lightbox or full-screen view. Clicking an image does nothing.

---

### 6. Checkout (`/checkout`)

**What works well:**
- The booking summary card (service name, dates, night count, guests, price breakdown) is clear and readable.
- The discount code field auto-uppercases input — good UX touch.
- The confirmation screen has a clean success state with a reference number and voucher download.

**Issues:**

**[CRITICAL] No payment form — "Confirm & Pay" confirms with no payment.**
The button label is `t('checkout.confirmPay')` which translates to "Confirm & Pay", but there is no credit card input, no payment gateway widget, no PayPal button, nothing. The button calls `bookingsAPI.create()` directly. A user reading "Confirm & Pay $150" expects to enter card details. This is a silent expectation mismatch that will cause confusion.

**[CRITICAL] Checkout has no navbar or footer.**
The checkout page is outside `LandingShell`. A user who navigates here sees no site header and no way to go back except pressing browser back. The `← Back` button calls `navigate(-1)` which has no guaranteed destination. If the user arrived from an email link, this button does nothing useful.

**[HIGH] Discount code has no live validation.**
The coupon input has no "Apply" button and no feedback until the final submit. The user enters a code, sees nothing, and only discovers it's invalid after clicking "Confirm & Pay." Compare this to any modern booking platform where coupon codes show a green checkmark or red error inline.

**[HIGH] "Confirm & Pay" label is misleading.**
With no actual payment step, the button should say "Confirm Booking" or "Reserve Now." Calling it "Pay" when nothing financial is collected is deceptive labeling.

**[MEDIUM] The confirmation reference number is unstyled for a human reader.**
```tsx
<p className="mt-4 rounded-xl bg-slate-50 px-4 py-3 font-mono text-sm font-bold" style={{ color: 'var(--accent-teal)' }}>
  {bookingRef}
```
The reference is shown in a small monospaced code block on a white card. It looks like a UUID debug output. A booking reference number is the most important piece of information the user receives — it deserves larger type, a label, and copy-to-clipboard functionality.

**[LOW] Night count hardcodes "night"/"nights" in English.**
```tsx
{nights} night{nights !== 1 ? 's' : ''}
```
This is hardcoded English pluralization, not going through `t()`.

---

### 7. My Bookings (`/my-bookings`)

**[CRITICAL] Completely different visual design — looks like a different website.**
The rest of the landing site uses `lp-*` glassmorphism classes, warm tones, `border-glass`, and frosted surfaces. The My Bookings page uses:
```tsx
<div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
```
It is a plain white Tailwind table on a light background. No glassmorphism. No warm palette. No `lp-*` classes. This page screams "I was copy-pasted from the admin dashboard." A user who was just on the beautiful homepage will think they accidentally navigated to a different product.

**[CRITICAL] No navigation — stranded page.**
As noted, no navbar, no footer. The user is stranded. There is no `<Link to="/">Home</Link>` anywhere on the page.

**[HIGH] Cancel modal is styled inconsistently.**
```tsx
<div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
  <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
```
Plain white modal with `bg-black/40` scrim — matches neither the landing glassmorphism nor a consistent design system.

**[HIGH] Table overflows on mobile.**
The bookings table has 6 columns (Ref, Service, Dates, Status, Total, Actions). On a phone screen this will overflow horizontally with no scroll indicator. The table has no `overflow-x-auto` wrapper.

**[MEDIUM] `lp-section-pad` CSS class is used but not defined in `landing-public.css`.**
```tsx
<div className="lp-section-pad mx-auto max-w-2xl text-center">
<div className="lp-section-pad mx-auto max-w-4xl">
```
This class appears nowhere in `landing-public.css`. It is either a global class or it silently does nothing — in either case, it is suspect.

---

### 8. Login Page (`/login`)

**What works well:**
- The ambient orbs background is beautiful.
- The auth card entrance animation (`lp-card-enter`) is smooth.
- Field-level validation errors (red border + message) are clear.
- "Forgot password" link is correctly positioned inline with the label.
- The wrong-portal error messages are clear and helpful.

**Issues:**

**[MEDIUM] No password visibility toggle.**
Both login and signup have `type="password"` inputs with no toggle to reveal the password. This is a basic usability feature that every major auth form now includes.

**[MEDIUM] Signup has no password strength indicator.**
The `signUpSchema` validates minimum length, but there is no visual strength meter, no real-time feedback, and no guidance on what "strong" means. A user can register with a weak password and only discover it at form submission.

**[LOW] Signup phone field has no format hint or validation.**
The phone field is `type="tel"` but there is no placeholder showing expected format (e.g., `+961 70 123456`) and no client-side validation. International users will guess formats.

---

### 9. Footer

**Issues:**

**[HIGH] Footer links point to wrong destinations.**
The Product column:
- "Pricing" → `/faq` ❌ (should go to a pricing page or at least an FAQ section about pricing)
- "Case Studies" → `/about` ❌ (the About page has no case studies)

The Company column:
- "Careers" → `/contact` ❌ (careers ≠ contact)
- "Contact" → `/contact` ✓

Three of five footer links are mislabeled or misdirected.

**[MEDIUM] Footer language toggle is non-functional.**
```tsx
<div className="lang-toggle" aria-hidden>
  <span className={locale === 'en' ? 'active' : ''}>EN</span>
  <span className={locale === 'ar' ? 'active' : ''}>AR</span>
</div>
```
This is a `<div>` with `aria-hidden`. It shows the current language but clicking it does nothing. It is decorative UI that looks interactive. Remove it or make it functional.

**[MEDIUM] Footer column headings are not translated.**
The column headings (`Product`, `Company`, `Legal`) are rendered via `t('landingPublicFooter.product')` etc., so they are in the i18n system — but verify the Arabic translations exist, because other sections have missing keys.

---

### 10. About Page (`/about`)

**Issues:**

**[HIGH] Extremely thin — barely a page.**
The entire About page is: eyebrow label + h1 title + one paragraph + 3 stat chips. There is no team section, no mission statement depth, no brand story, no images. For a travel platform positioning itself as a premium product, the "About" page is a severe liability. A skeptical user who clicks "About" to vet the company will leave unimpressed.

**[HIGH] Hardcoded English copy in the stat chips.**
```tsx
<p className="lp-label">Cities</p>
<p className="lp-muted">Active destination cities</p>

<p className="lp-label">Listings</p>
<p className="lp-muted">Live listings in the platform</p>

<p className="lp-label">Pricing</p>
<p className="lp-muted">Average starting base price</p>
```
All six text labels are hardcoded English, bypassing `t()`.

**[MEDIUM] `lp-city-chip` is used as a stat container.**
City chips are designed to display a city name — a single short string. Using them as stat containers (with label, value, and description stacked) misuses the component and the result is visually awkward (chips with multiline content, no padding adjustments for the stat structure).

---

## Cross-Cutting Issues

---

### A. Internationalization (i18n) — Incomplete

The i18n system is architecturally correct (`useLanguage()` / `t()` with EN/AR keys) but applied inconsistently. The following strings are hardcoded English and bypass translation:

**`ServicesFilterBar.tsx`**
- `"Search"`, `"Name or description…"`, `"City"`, `"All cities"`, `"Min price"`, `"Max price"`, `"Min stars"`, `"Any"`, `"Clear"`

**`LandingServiceDetailPage.tsx`**
- `"Services"` (breadcrumb root), `"← Back to list"`, `"Menu"`, `"No menu sections yet."`, `"Back"`, `"Could not load service."`, `"Service not found."`

**`LandingServiceTypePage.tsx`**
- `"No results match your filters."`, `"{n} of {m} results"`

**`LandingAboutPage.tsx`**
- `"Cities"`, `"Active destination cities"`, `"Listings"`, `"Live listings in the platform"`, `"Pricing"`, `"Average starting base price"`

**`LandingCheckoutPage.tsx`**
- `"night"` / `"nights"` pluralization

**Total: ~25 untranslated strings across core user-facing pages.**

---

### B. Design Consistency — Two Distinct Personalities

The site has two clearly separate design systems:

| Feature | Landing Glassmorphism | Tailwind Slate (Admin-inherited) |
|---|---|---|
| Background | Warm `#f5f2ec` gradient | White `bg-white` |
| Cards | `lp-glass-card`, `lp-sheet` | `rounded-2xl border-slate-200 bg-white shadow-sm` |
| Text | `var(--ink-heading)` teal gradient | `text-slate-900`, `text-slate-600` |
| Buttons | `lp-btn lp-btn-primary` (tan gradient) | Inline `bg-red-600`, `border-slate-300` |
| Used on | All shell pages | My Bookings, cancel modal, error fallbacks |

The "My Bookings" page is the most egregious — it was clearly ported from the admin dashboard with no restyling.

---

### C. Fake / Placeholder Data Presented as Real

Three places present fabricated data as genuine platform information:

1. **Room availability** — Rooms 3, 6, 9, 10, 16 are hardcoded as "booked" regardless of property, dates, or real inventory.
2. **Deal prices** — "20% lower" deals are computed backwards from the listed price (`old = current * 1.2`), always exactly 20% off.
3. **Taxi fare estimate** — A hardcoded local multiplier (Economy ×1, VIP ×1.45, Van ×1.25) with no real routing, displayed as an actual price quote.

These undermine the platform's credibility. If any user notices, trust is permanently damaged.

---

### D. Missing Core Features for a Booking Platform

| Feature | Status |
|---|---|
| Real payment form | ❌ Missing — button says "Pay" but no payment |
| Map view for services | ❌ Missing |
| Image lightbox / gallery viewer | ❌ Missing |
| Wishlist / Save service | ❌ Missing |
| Reviews from customers | ❌ Missing on detail page |
| Real room availability API | ❌ Replaced by hardcoded fake data |
| Password show/hide toggle | ❌ Missing |
| Password strength meter | ❌ Missing |
| Coupon code live validation | ❌ Missing |
| Mobile navigation drawer | ❌ Missing |
| Copy-to-clipboard for booking ref | ❌ Missing |
| Search from homepage | ❌ Missing (removed, stats strip is replacement) |

---

## Priority Fix List

Listed in order of user impact:

1. **Wrap `/my-bookings`, `/checkout`, `/account` inside `LandingShell`** — Users are stranded without navigation. This is the single most damaging UX issue.

2. **Remove or clearly label fake data** — Room selector, deal discounts, and taxi fare estimates must either be real or disclosed as estimates/illustrative.

3. **Add real payment step to checkout** — The "Confirm & Pay" button must be renamed or a payment flow must be added. Never tell a user they're paying when they're not.

4. **Fix the logged-in navbar** — Consolidate the 7 action elements into a compact user menu or avatar dropdown.

5. **Add a mobile hamburger menu** — The current wrapped nav is unusable on phones.

6. **Complete i18n for `ServicesFilterBar`, `ServiceDetailPage`, and `AboutPage`** — ~25 untranslated strings all in Arabic-relevant flows.

7. **Restyle "My Bookings" to match the landing design system** — Replace Tailwind slate classes with `lp-*` glassmorphism classes to eliminate the visual whiplash.

8. **Fix footer links** — "Pricing," "Case Studies," and "Careers" all point to wrong pages.

9. **Make deal cards and city chips clickable** — They have hover effects suggesting interactivity. Add navigation or remove hover effects.

10. **Fix the stats strip label mismatch** — The average price stat uses a "rating" label when `averageBasePrice` is set.

11. **Expand the About page** — Three stat chips is not a credible About page for a travel platform.

12. **Add coupon validation feedback before form submission**.

13. **Add password visibility toggle and strength meter to signup**.

14. **Add `overflow-x-auto` to the My Bookings table for mobile**.

15. **Fix the GSAP hero flash-of-invisible** — Add a CSS fallback or `<noscript>` path.

---

## What Is Genuinely Good

It would be unfair not to acknowledge what lands well:

- **The visual design language** is coherent and premium. The warm neutral palette with teal accents is rare and tasteful in the travel space — most competitors default to blue/white.
- **The GSAP entrance animations** on the homepage create a memorable first impression.
- **The auth flow** (login, signup, wrong-portal error messages, remember-me) is logically clean and the error states are helpful.
- **URL-synced filters** on service type pages are a genuinely good engineering and UX choice.
- **The voucher download** after booking is a strong closing action.
- **Availability check debounce** on the booking panel is thoughtful — it doesn't fire on every keystroke.
- **The `prefers-reduced-motion` support** in CSS is a professional accessibility touch that most teams skip.
- **Document meta** (`<title>` and description) is dynamic on every page — good for SEO.
- **The checkout success state** (green check, reference number, download voucher, view bookings, browse more) gives users clear next actions.

---

*Report generated by external critic review of source code at `front/my-app/src/apps/landing/`. All line references are approximate and may shift with ongoing edits.*
