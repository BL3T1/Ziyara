# Ziyara Landing Site — UI/UX Beauty & Completeness Report
**Port 7070 · Customer-Facing Booking Surface**
**Date reviewed:** 2026-05-31
**Reviewer:** External UI/UX critic — evaluating visual beauty, polish, and end-to-end completeness

---

## Executive Summary

The Ziyara landing site has a genuinely premium visual identity. The glassmorphism design system — warm cream backgrounds, layered teal and tan gradients, 14–24px backdrop blur, DM Sans headlines — is distinctive and tasteful. The GSAP scroll-reveal animations and spring-eased micro-interactions elevate the first impression well above what most booking platforms deliver.

The problem is that the premium visual shell contains an incomplete interior. Loading states are sparse or absent on most pages. Several core features a user expects from a travel platform are entirely missing (image gallery, wishlist, customer reviews on detail pages, toast notifications). The bookings table is unreadable on a phone. The checkout says "Confirm Booking" but collects no payment. Several pages reuse UI components in the wrong semantic context, creating subtle visual inconsistency.

This is a site that looks 9/10 in a Figma mockup and feels 6.5/10 when you actually try to use it.

---

## Overall Ratings

| Dimension | Score | Verdict |
|---|---|---|
| Visual Design & Aesthetic | **9.0 / 10** | Exceptional — premium glassmorphism, superb color palette |
| Typography & Spacing | **8.5 / 10** | DM Sans + Inter pairing is elegant; scale could be tighter |
| Animation & Motion | **9.0 / 10** | GSAP orchestration is genuinely sophisticated |
| Navigation & Shell | **8.5 / 10** | Sticky glass nav + mobile drawer are beautifully done |
| Loading & Empty States | **4.5 / 10** | Most pages show plain text; skeletons nearly absent |
| Form Design & Validation | **7.5 / 10** | Good baseline; discount code and character counts missing |
| Mobile Experience | **6.0 / 10** | Hero and nav work; tables and detail pages struggle |
| Feature Completeness | **5.5 / 10** | No lightbox, no reviews, no wishlist, no toasts, no payment |
| User Journey End-to-End | **7.0 / 10** | Booking works; no payment, no post-booking editing |
| Accessibility | **6.5 / 10** | Good semantics; focus indicators and traps missing |
| **Overall** | **7.2 / 10** | Visually premium shell over an incomplete feature set |

---

## Design System: What the Site Gets Right

Before listing problems, this deserves acknowledgment — the design system itself is genuinely excellent.

### Color Palette
The combination of `--bg-warm: #f5f2ec` (sand parchment), `--accent-teal: #3d7080` (deep sea teal), and `--accent-tan: #b8966e` (warm leather) is rare in travel platform design. Every competitor defaults to blue-white-gray. Ziyara's palette feels like a boutique hotel lobby — premium and warm. The gradient background (`linear-gradient(160deg, #faf7f2 0%, #ece7dc 40%, #cad4d8 85%, #8a9da6 100%)`) attached as `background-attachment: fixed` creates atmospheric depth as the page scrolls.

### Glassmorphism Execution
The glass cards and surfaces are done correctly — not the cheap semi-transparent-white-blob that most implementations produce. Each glass element has:
- Layered outer + inner shadows (`inset 0 1px 0 rgba(255,255,255,0.9)` + `0 24px 64px -24px rgba(60,80,96,0.1)`)
- A top hairline gradient (`from transparent via primary/35 to secondary/25`) acting as a subtle shine
- Precisely calibrated opacity (0.45–0.78, never pure white)
- `backdrop-filter: blur(14–24px) saturate(1.3–1.4)` with webkit prefix

### Typography
DM Sans for headings with `letter-spacing: -0.038em` and gradient text fill (`background-clip: text`) is modern and deliberate. Inter for body with `font-feature-settings: 'cv02', 'cv03', 'cv04', 'ss01'` shows typographic care most teams skip. The responsive clamp scales feel right: `clamp(36px, 4.5vw, 52px)` for hero, `clamp(1.5rem, 2.2vw, 2rem)` for section headings.

### Animation
The GSAP implementation is production-quality:
- Hero entrance staggered timeline with `delay: 0.08`, natural overlap between elements
- ScrollTrigger batch for `.lp-animate` elements (prevents janky per-element triggers)
- Deal card stagger, city chip pop-in, pillar entrance — all individually tuned
- `prefers-reduced-motion` and low-end mobile detection properly implemented
- Initial opacity set in JS (not CSS) so content is visible if GSAP fails

---

## Page-by-Page Critique

---

### 1. Homepage (`/`)

**Visual Rating: 8.5 / 10 — UX Rating: 7.0 / 10**

#### What's Beautiful
The hero composition is the best-designed section on the site. The two-column layout (copy left, art right) with the tilt-responsive `ZiyaraHeroComposition` creates genuine interactivity. The hotspot overlays on the hero image (invisible clickable areas for Hotels, Dining, Transport, Experiences) are a clever navigation pattern. The stats strip below the hero — glass cells showing service count, city count, and average price — replaces the typical fake search bar with something honest.

The trust pillars section (three-column `lp-pillars` container) with icon, title, and body paragraph is visually clean. The partner band CTA at the bottom is well-styled and ends the page purposefully.

#### Issues

**[HIGH] Hero image may not exist — the entire right side of the page could fail silently.**
`ZiyaraHeroComposition.tsx` loads `/ziyara-hero-reference.png` (the main hero photo) from the public directory. If this file is missing or not yet uploaded, the `<img>` element renders with no fallback visible. The `LandingHeroArt.tsx` SVG fallback exists but only triggers on `img.onError` — and on initial render there is a flash of empty space while the image loads. No skeleton or placeholder fills that area.

**[HIGH] No loading state for the live data (`useLandingLiveData`).**
The deal tiles, city chips, stats numbers, and trust block bodies all depend on `useLandingLiveData()`. While this data loads:
- Stats strip shows `100+`, `15+`, `4.8★` placeholder strings (fine)
- Deal tiles fall back to hardcoded city/price pairs (fine)
- Trust block bodies use `t()` translations (fine)

But there is zero visual feedback that the data is loading or has loaded. The numbers silently swap from placeholder to real without any animation or transition. A brief number-count animation on the stats would reinforce the "live data" feel.

**[MEDIUM] Deal tiles have no hover destination.**
The deal tiles now correctly use `<Link to={servicesBrowse}>` so they are clickable, but they all link to the same generic `/services` page regardless of which city or deal type was clicked. A user clicking "Beirut Hotel — $168" expects to see Beirut hotels. The link should go to `/hotels?city=Beirut` or similar.

**[MEDIUM] City chips navigate but carry no city pre-filter.**
City chip `onClick` navigates to `/services?city=${encodeURIComponent(city)}`. However, the `/services` page (`LandingServicesPage.tsx`) does not read URL query params and cannot apply a city filter — it just shows the generic service grid. The filter lives on `LandingServiceTypePage`, not `LandingServicesPage`. So the city navigation promise is broken.

**[MEDIUM] Hero hotspots are invisible and undiscoverable.**
The four hotspot buttons on the hero image (`lp-ziyara-hero__hotspot`) are transparent `position: absolute` elements with no visual affordance. A user with no prior context does not know they can click on the image corners to navigate to service categories. There is no hover tooltip, no icon, no label.

**[LOW] The final CTA band and hero both offer "Sign in to book" — duplicated primary CTA.**
The hero has two CTAs: "Sign in to book" (primary, tan gradient) and "Browse services" (outline). The bottom CTA band repeats "Browse services" + "Sign in to book". The journey from hero to bottom of page circles back to the same two actions without building momentum.

---

### 2. Navigation Shell (`LandingShell`)

**Visual Rating: 9.5 / 10 — UX Rating: 8.0 / 10**

#### What's Beautiful
The sticky glass navbar with `border-radius: 18px`, `backdrop-filter: blur(20px) saturate(1.4)`, and the subtle `0 1px 0 rgba(255,255,255,0.9) inset` top shine is one of the most refined navbar implementations I have seen. It doesn't feel like a typical sticky header — it feels like a floating island above the page content.

The user avatar dropdown with its spring-in animation (`lp-dropdown-enter`: `opacity 0 → 1`, `translateY(-6px) → 0`, `scale(0.97) → 1`) is better than most commercial dashboards.

The mobile hamburger (3 bars → X animation) is smooth and the full-width drawer slides open with a `max-height` transition.

#### Issues

**[HIGH] Logged-in state nav still has too many active elements on mid-size screens.**
When authenticated, the nav actions area contains: Browse button + user dropdown trigger + currency switcher + language toggle + (on mobile) hamburger. On a 768px tablet these compete for space. The currency switcher should move inside the user dropdown on viewports below 960px.

**[MEDIUM] No active state on navigation links reflects current service category.**
When a user browses `/hotels`, the nav link "Services" shows active (which is `to="/services"` with `end={false}`). But there is no visual distinction between being on the generic services page vs a specific category page. The nav never tells the user "you are currently in Hotels."

**[MEDIUM] Language toggle in shell and footer are separate components.**
The footer has an independently-implemented language toggle button. The shell has the `LanguageToggleButton` component. They look slightly different. One switch should be definitive; two is confusing.

---

### 3. Services Browse (`/services`)

**Visual Rating: 7.5 / 10 — UX Rating: 5.0 / 10**

#### What's Beautiful
The 3-column card grid with image overlay, gradient scrim at the bottom, and hover scale effect (`group-hover/card:scale-[1.03]`) is clean and readable.

#### Issues

**[CRITICAL] This page is not a useful browse experience — it shows at most 6 services.**
`services.slice(0, 6)` hard-limits to 6 cards. There is no pagination, no "load more", no count indicator, no link to the full category pages. A user clicking "Services" in the nav expecting to browse all available hotels/resorts/restaurants sees 6 items and believes that's the full catalog.

**[CRITICAL] Cards link to category type pages, not to the specific service.**
Each card's "View details →" link goes to `/hotels`, `/resorts`, etc. — the category list — not to the specific service shown. A user sees "Grand Beirut Hotel" with a photo and clicks "View details" expecting to see that hotel — instead they land on the hotels category list. The displayed data and the navigation destination are mismatched.

**[HIGH] No loading skeleton while services load.**
The page renders nothing while `useLandingLiveData()` fetches. No spinner, no skeleton grid, no "Loading…" text. On a slow connection the user stares at the shell with a completely empty content area.

**[MEDIUM] Empty state uses generic fallback text.**
If no services are returned, the fallback card shows `t('landingBusiness.servicesTitle')` as the heading and `t('landingBusiness.heroBody')` as the body — both generic marketing copy that gives no useful guidance about why the page is empty.

---

### 4. Service Category Pages (`/hotels`, `/resorts`, etc.)

**Visual Rating: 8.0 / 10 — UX Rating: 7.5 / 10**

#### What's Beautiful
The filter bar design is clean and functional. URL-synced filters (refreshable, shareable) are a genuine UX win. The `ServiceGallery` grid of `ServiceCard` components with consistent aspect ratios and hover lifts feels like a real product.

#### Issues

**[HIGH] Filter bar uses raw Tailwind, not the landing design tokens.**
The `ServicesFilterBar` inputs use:
```tsx
className="lp-input w-full"
```
— good, they use `lp-input`. But the select and number inputs look slightly different from the standard inputs. The city dropdown `<select>` doesn't inherit the correct font on all browsers. On Safari, select elements render with system chrome that breaks the glass input aesthetic.

**[HIGH] No "results count" shown while filters are active.**
When a user types "Beirut" in search and gets 3 results from 40, there is a tiny `text-xs text-slate-400` count message: "3 of 40 results". This is nearly invisible. A user might think the search failed rather than succeeded with filtered results.

**[MEDIUM] Price range filter has no currency label.**
The min/max price inputs have no indication of what currency to enter. A user who looks at a listing priced in USD and enters `500` in Lebanese Pounds has no feedback that this is wrong.

**[MEDIUM] Star rating filter uses `★` characters which render differently per OS.**
On Windows the filled star character `★` renders as a thin outline on some system fonts. The filter shows `★★★★★` for 5 stars but `★` for 1 star — the visual weight difference is extreme and the characters appear misaligned.

**[LOW] No "sort by" control.**
Users can filter but cannot sort. No price ascending/descending, no rating order, no alphabetical. The grid shows items in whatever order the backend returns.

---

### 5. Service Detail Page (`/services/:category/:id`)

**Visual Rating: 8.5 / 10 — UX Rating: 7.0 / 10**

#### What's Beautiful
The skeleton loading state — `animate-pulse` blocks for breadcrumb, hero image, title, and description — is the best loading UX on the entire site. The breadcrumb navigation (`Services / Hotels / Hotel Name`) is clear and correctly uses button elements for navigation.

#### Issues

**[CRITICAL] No customer reviews displayed anywhere on the service detail.**
A hotel detail page shows images, description, amenities — but zero customer reviews or ratings. The `ServiceDetailView` component renders the service data but there is no reviews section, no star rating display, no review count. A customer evaluating a hotel booking sees no social proof whatsoever.

**[CRITICAL] Service gallery has no lightbox — images cannot be viewed full-size.**
The `ServiceGallery` component renders a responsive grid of `ServiceCard` items or thumbnail images. Clicking an image does nothing. A user wanting to inspect a hotel room photo in detail has no way to do so.

**[HIGH] Menu section for restaurants renders fine but item images are tiny (64×64px).**
Restaurant menu items render with `h-16 w-16 shrink-0 rounded-md object-cover` images — a 64px square. A food photo at 64px tells the user almost nothing about the dish. These should be at least 120px, ideally expandable.

**[HIGH] No "rooms" display for hotels.**
The hotel detail view shows service metadata but not the individual room types (standard, deluxe, suite), their capacities, amenities lists, or specific pricing. A hotel booking experience that doesn't show room types is critically incomplete.

**[MEDIUM] The booking panel sits below ALL content including the restaurant menu.**
On a restaurant page with 4 menu sections of 10 items each, the user must scroll past 40+ menu items to reach the booking panel. The booking CTA should be sticky or sidebar-positioned.

**[LOW] "Back to list" breadcrumb and back arrow are now consolidated (good) but the breadcrumb has no visual separator icon.**
`Services / Hotels / Hotel Name` uses `<span className="mx-2 text-slate-400">/</span>` for the separator. The slash separator is barely visible against the lp-sheet background.

---

### 6. Booking Panel (`LandingServiceBookingPanel`)

**Visual Rating: 9.0 / 10 — UX Rating: 8.0 / 10**

#### What's Beautiful
The `BookingCard` wrapper with `linear-gradient(145deg, rgba(255,255,255,0.72) 0%, rgba(255,255,255,0.44) 100%)`, `boxShadow: 0 8px 32px rgba(40,55,68,0.07), inset 0 1px 0 rgba(255,255,255,0.9)`, and `backdropFilter: blur(14px)` is some of the best glassmorphism in the entire site. The guest counter with circular +/- buttons in teal is delightful. The price summary section with `clamp`-sized bold price and CTA button side-by-side feels premium.

The taxi type selector (Economy / VIP / Van) with toggle-style buttons is well-executed.

#### Issues

**[HIGH] Room selector shows 20 generic numbered rooms with no information.**
The room grid shows buttons numbered 1–20. There is no room type label, no floor number, no amenity summary, no actual capacity — just numbers. A hotel guest selecting "Room 7" has no idea if that's a standard room or a suite. The room selector needs real room data from the API.

**[HIGH] Availability check message is barely visible.**
The "Checking availability…" message is `text-xs text-slate-400` — tiny gray text above the guest counter. "Available for your dates" appears in `text-xs font-medium text-emerald-600` — slightly better but still very small. The availability status is one of the most important pieces of information in the booking flow and it should be prominent.

**[MEDIUM] No price breakdown before the "Confirm booking" button.**
The summary panel shows a total price (e.g., "USD 450 · 3 nights") but no line-item breakdown. There is no display of taxes, fees, or the platform's service charge. The user commits to a number without seeing how it's composed.

**[MEDIUM] Taxi fare shows `~` prefix to indicate estimation but only in the price display.**
The `~USD 180` format correctly signals an estimate. But there is no explanation of how the estimate was calculated or what it's based on (distance? base rate?). The "Estimated fare" label below is in tiny faint text.

**[LOW] Date inputs use the browser native date picker.**
`<input type="date">` renders the operating system's date picker — which looks completely different on Windows (calendar widget), macOS (inline spinner), and iOS/Android (wheel picker). The design is inconsistent across platforms and cannot be styled to match the glass aesthetic.

---

### 7. Checkout (`/checkout`)

**Visual Rating: 8.5 / 10 — UX Rating: 7.5 / 10**

#### What's Beautiful
The booking confirmation screen is the highlight. The green check icon in an emerald-tinted circle, the large monospace booking reference with copy button, the clean service summary card, and the three-action CTA row (Download Voucher → View Bookings → Browse More) form a satisfying end-of-flow screen.

#### Issues

**[CRITICAL] "Confirm Booking · USD 450" button collects no payment.**
The button says "Confirm Booking · USD 450" and a user who reads this believes they are paying. No card input, no PayPal redirect, no payment confirmation — the booking is placed for free. This is a fundamental contract mismatch. Until a real payment gateway is integrated, the button label must be changed to something like "Reserve (Pay at property)" or "Send Booking Request."

**[HIGH] Discount code has zero validation feedback until submission.**
A user types `SUMMER20` in the discount field and cannot know if it's valid. There is no "Apply" button, no inline checkmark, no price update. They only discover the code is invalid when the full booking fails after clicking the confirm button. Every modern booking platform validates the coupon inline.

**[HIGH] No price breakdown visible before confirm.**
The checkout summary shows `USD [price] × [N] nights = [total]` but no taxes, no service fee, no platform charges are shown. The user has no idea what the final itemized price breakdown is.

**[MEDIUM] The checkout page has no navigation back to the service.**
The `← Back` button calls `navigate(-1)` which relies on browser history. If the user arrived via a direct URL (email link, shared link), history is empty and the back button does nothing. There should always be an explicit `← Back to [service name]` link.

**[MEDIUM] Special requests textarea has no character counter.**
The placeholder says "Any special needs, early check-in, dietary requirements…" — helpful context — but there is no visible character limit indicator. If the backend imposes a limit, the user discovers it on submission failure.

**[LOW] Booking reference copy button shows "Copied!" for only 2 seconds.**
The 2-second timeout on `setCopied(false)` is fine, but the transition between "Copy" and "Copied!" states is instant (no animation). A brief scale or color transition would feel more satisfying.

---

### 8. My Bookings (`/my-bookings`)

**Visual Rating: 8.0 / 10 — UX Rating: 6.5 / 10**

#### What's Beautiful
The restyled page uses the landing glass system correctly. Status badges with warm amber, emerald, teal, and red tones match the site palette. The loading skeleton (3 pulsing rows) is the right approach. The cancel modal with glassmorphism card and backdrop blur is consistent with the rest of the site.

#### Issues

**[HIGH] Table layout is unacceptable on mobile.**
Even with `overflow-x-auto`, the bookings table has 6 columns on a phone screen. The user must horizontally scroll a table inside a card inside a scrolling page — a terrible interaction. On mobile, bookings should be rendered as stacked cards, not a table.

**[HIGH] No booking detail view.**
Clicking a booking reference shows nothing. A user who wants to see the full details of a past booking (hotel address, check-in instructions, special requests confirmation, payment receipt) has no way to access this information. "Ref #ZYR-001" is the only identifier shown.

**[HIGH] Status displays via string manipulation, not proper labels.**
```tsx
status.charAt(0) + status.slice(1).toLowerCase().replace('_', ' ')
```
This produces "Pending", "Confirmed", "Completed", "Cancelled" — which is correct for single-word statuses. But for "PENDING_APPROVAL" it produces "Pending approval" (the underscore replacement only handles the first one). For "IN_PROGRESS" it would produce "In progress" only if the status happens to have the second word without a leading underscore. This is fragile.

**[MEDIUM] Voucher download shows "…" spinner but does not disable the button.**
During the `voucherLoading === b.id` state, the button text changes to `"…"` but the button is `disabled={voucherLoading === b.id}` — so it is actually disabled. However visually there is no way to distinguish the disabled state from the enabled state (same border, same background). The user cannot tell if the download is in progress.

**[LOW] Cancel button label says the i18n translation `t('ui.cancel')` if confirmed.**
The "Go back" / "Keep booking" button in the cancel modal uses `t('ui.cancel')` which translates to "Cancel" in English. This creates the paradox of a "Cancel" button that means "don't cancel" — the button labels inside the cancel confirmation modal are semantically backwards.

---

### 9. Login Page (`/login`)

**Visual Rating: 9.0 / 10 — UX Rating: 8.5 / 10**

#### What's Beautiful
The login page is among the most beautiful auth screens I've reviewed. Three layered ambient orbs in the background (sky blue, warm tan, dark teal at different positions) create a dimensional light effect. The auth card with `lp-card-enter` animation, gradient topline decoration (`inset-inline: 20%`, `height: 1px`, `background: linear-gradient(90deg, transparent, rgba(160,123,86,0.5), transparent)`), and `backdrop-filter: blur(24px) saturate(1.35)` is exceptional.

The password show/hide toggle with SVG eye icons is correctly implemented. The "Forgot password" link is correctly positioned inline with the password label (not below the field where it disrupts form flow).

#### Issues

**[HIGH] No password strength indicator on login.**
The login form doesn't need strength indication — that's correct. But the signup form's strength indicator uses hardcoded colors (`#e74c3c`, `#e67e22`, `#f1c40f`, `#22a06b`) instead of the design token palette. "Weak" should map to `--accent-red` or similar, not a raw hex.

**[MEDIUM] No "Continue as guest" option.**
A returning user who wants to browse without logging in can click Browse, but the auth card presents no guest path. First-time visitors who are not ready to create an account are funneled toward sign-in or sign-up only.

**[MEDIUM] Wrong-portal error messages contain raw URL strings.**
```tsx
setError(`${t('landingAuth.onlyPartners')} ${partnerPortalHint()}`)
```
This produces: "Partner accounts must use the partner portal to sign in. http://partners.local/login". A raw URL in a styled error message box looks unpolished. It should be a styled `<Link>` element.

**[LOW] "Remember me" is checked by default (`useState(true)`).**
Pre-checking "Remember me" without user intent is an accessibility and privacy concern. GDPR guidelines suggest persistent sessions should require explicit opt-in.

---

### 10. Sign-Up Page (`/signup`)

**Visual Rating: 8.5 / 10 — UX Rating: 8.0 / 10**

The password strength meter with 4 progress bars and color transitions is the best form feature on the entire site. The `passwordStrength()` function evaluating length, uppercase, digits, and special characters is a solid implementation.

#### Issues

**[MEDIUM] Password strength labels are English-only.**
The strength labels `"Weak"`, `"Fair"`, `"Good"`, `"Strong"` are hardcoded strings in the component's `passwordStrength()` function — not going through `t()`. An Arabic user sees English strength indicators in an otherwise Arabic form.

**[MEDIUM] Phone field has no country code selector.**
The `type="tel"` input with placeholder accepts any format. Lebanon users would type `+961 70...`, Saudi users `+966 5...`. Without a country code prefix selector, phone numbers stored in the database will be inconsistently formatted.

**[LOW] After successful signup, user lands on `/login?registered=1` without animation.**
The registered hint ("Account created. Check your email for a verification code") appears as a static green message. A brief entrance animation on this message would feel more celebratory.

---

### 11. About Page (`/about`)

**Visual Rating: 7.0 / 10 — UX Rating: 4.5 / 10**

#### What's There
Three stat pillars (Cities, Listings, Avg. Price) using the correct `lp-pillar` layout, an eyebrow label, a heading, and one paragraph of body text.

#### Issues

**[CRITICAL] This is not an About page — it is a stat card.**
The entire page content is: eyebrow + h1 + one paragraph + 3 numbers. There is no story, no team, no mission statement beyond the generic translation copy, no imagery, no history, no vision. A user who clicks "About" to evaluate whether Ziyara is a trustworthy company to book with will leave within 5 seconds. The page does not build trust.

**[HIGH] No imagery anywhere on the About page.**
No team photo, no office, no brand imagery, no map of coverage areas, no partner logos. The page is 3 numbers and ~80 words of text.

**[HIGH] Stat chips use live data but show `—` when the backend is offline.**
If the API is down or returns no data, all three stats show `—`. The page becomes meaningless. A reasonable fallback for "Cities: —" is to show a static minimum value until live data loads, not a dash.

---

### 12. Contact Page (`/contact`)

**Visual Rating: 8.5 / 10 — UX Rating: 7.5 / 10**

#### What's Beautiful
The two-column layout (info card left, form card right) with matching glass surfaces is clean. The "Response time" and "We can help with" info cells inside `lp-search-cell` containers add useful context without cluttering the form.

#### Issues

**[MEDIUM] Form submits without showing which fields failed.**
`isFormValid` is computed from: name > 1 char, email has `@`, message > 10 chars. But if submission is blocked (button disabled), the user doesn't know which field is invalid — there is no per-field error message or visual indicator. They see a grayed-out "Send message" button with no explanation.

**[MEDIUM] "Subject" field is marked optional in code but appears identical to required fields.**
There is no visual distinction (asterisk, "(optional)" label) between required fields (name, email, message) and the optional subject field.

**[LOW] Success message appears inline below the form without replacing it.**
On successful submission, `setShowSuccess(true)` renders a green message below the button while the (now cleared) form remains visible. This creates a confusing UI state: an empty form + a success message. On success, the form should either be replaced by the success message or the button should change to "Send another message."

---

### 13. FAQ Page (`/faq`)

**Visual Rating: 7.0 / 10 — UX Rating: 5.5 / 10**

#### What's There
Three `Card` items with a question heading and answer paragraph. Clean typography, correct glass card.

#### Issues

**[HIGH] No accordion — all FAQ answers always visible.**
A FAQ page with only 3 items is short enough that an accordion is unnecessary. But for future scalability, all answers being simultaneously visible means there's no progressive disclosure. More importantly, 3 FAQ items is not nearly enough for a booking platform. Users expect answers to: payment methods, cancellation policy, refund timeline, how to report an issue, minimum booking age, group bookings, modification policy.

**[MEDIUM] The FAQ page looks nearly identical to a single card section.**
Three cards in a column is not a distinctive FAQ design. There is no eyebrow differentiation by topic, no search, no "Was this helpful?" feedback mechanism.

---

### 14. Privacy & Terms Pages

**Visual Rating: 6.5 / 10 — UX Rating: 5.5 / 10**

#### Issues

**[HIGH] Section cards use `lp-city-chip` class — semantically and visually wrong.**
Both `LandingPrivacyPage` and `LandingTermsPage` use:
```tsx
<div className="lp-city-chip">
  <h2>...</h2>
  <p>...</p>
</div>
```
`lp-city-chip` is designed for one-line city name labels. Using it as a multi-paragraph section wrapper produces an element styled for a name tag that now contains a legal section. The padding, border-radius, and hover effects are inappropriate for legal text sections.

**[HIGH] No table of contents or anchor links.**
A Privacy Policy or Terms page should have anchor links to each section. Users looking for the specific "Cancellation Policy" section must read the entire document linearly.

**[MEDIUM] No "Last updated" date.**
Neither page shows when it was last updated — a legal requirement in many jurisdictions (GDPR, CCPA).

---

### 15. Account Page (`/account`)

**Visual Rating: 7.5 / 10 — UX Rating: 7.0 / 10**

The profile and password sections use `lp-glass-card` correctly with `lp-field-label` styling. The two-section layout is logical.

#### Issues

**[MEDIUM] Profile save has no confirmation feedback beyond a green paragraph.**
After saving profile, `setProfileSuccess(true)` renders a green message: "Profile updated successfully." This appears below the submit button and fades only on the next form interaction. There is no toast, no animation, and the message is easy to miss.

**[MEDIUM] No avatar/profile photo upload.**
The account page allows editing name and phone but there is no profile photo. The user menu in the nav shows an initial-letter avatar. No way to personalize it.

**[LOW] Password change form has no current-password visibility toggle.**
The current password field is `type="password"` with no show/hide button — unlike the login and signup forms that have eye icons. Inconsistent behavior across auth-adjacent forms.

---

## Cross-Cutting Issues

---

### A. No Toast Notification System

The entire site has zero toast/snackbar notifications. Every action either:
1. Renders an inline message inside the page (`setSuccess(true)` → green paragraph)
2. Changes page state (navigates to confirmation screen)
3. Shows an error banner

Inline messages are:
- Easy to miss (below the fold, small text)
- Positionally inconsistent (different location in every component)
- Not dismissible

A single global toast system would unify all feedback and dramatically improve polish. Every important action — booking confirmed, message sent, voucher downloaded, booking cancelled, profile saved — deserves a toast.

---

### B. No Loading Skeletons on Most Pages

| Page | Loading State | Should Be |
|---|---|---|
| Homepage | Placeholder text values (fine) | Fine |
| Services Browse | **No loading state at all** | 6 skeleton cards |
| Service Type Pages | `<p className="lp-muted">Loading…</p>` | Gallery skeleton grid |
| Service Detail | ✅ DetailSkeleton (good) | — |
| Checkout | `<p>Loading…</p>` (one line) | Service summary skeleton |
| My Bookings | ✅ 3 skeleton rows (good) | — |
| About | No loading | Stat pillar skeletons |
| Account | `<p>Loading…</p>` | Form field skeletons |

---

### C. Missing Core Travel Platform Features

These are features that a user booking travel accommodation expects to find and will notice their absence:

| Feature | Status | Impact |
|---|---|---|
| Customer reviews on service detail page | ❌ Missing | Critical — no social proof |
| Image lightbox / gallery zoom | ❌ Missing | High — can't inspect photos |
| Wishlist / save for later | ❌ Missing | Medium |
| Share service link | ❌ Missing | Low |
| Real payment form | ❌ Missing | Critical — button says "pay" |
| Hotel room type comparison | ❌ Missing | High |
| Booking modification | ❌ Missing | High |
| Newsletter signup | ❌ Missing | Low |
| Map/location view for services | ❌ Missing | Medium |
| Service availability calendar | ❌ Missing | Medium |
| Cancellation policy visible before booking | ❌ Missing | High |
| Review submission from users | ❌ Missing | High |

---

### D. Hardcoded Colors Outside the Design System

Several components use raw hex colors instead of `--` CSS custom properties:

| File | Hardcoded Color | Correct Token |
|---|---|---|
| `LandingSignUpPage.tsx` | `#e74c3c` (weak), `#e67e22` (fair), `#f1c40f` (good), `#22a06b` (strong) | Should use design token or named variable |
| `LandingHeroArt.tsx` | `#ac9e78` | `var(--accent-tan)` |
| `ClientPortalOverview` (used on landing) | `#1e4d6b` fill on bar chart | `var(--accent-teal)` or primary token |
| `ZiyaraHeroComposition.tsx` | `#3d7080` in SVG arc | `var(--accent-teal)` |

While these specific values align with the palette, hardcoding them means a future rebrand or dark mode variant would miss them.

---

### E. Image Strategy Is Undefined

The site has no explicit image strategy:
- Hero image: `public/ziyara-hero-reference.png` — one file that must exist
- Service images: From API `imageUrl` — format and dimensions unspecified
- Provider logos: From API `logoUrl` — may be missing for many providers
- No image CDN optimization (`?w=640&format=webp` type params)
- No `srcset` for responsive images anywhere
- No placeholder image component with consistent fallback styling

When a service has no image, the `ServiceCard` skips the image block entirely, leaving a shorter card that breaks grid visual rhythm.

---

### F. Inline Style Overuse

Many sections mix `lp-*` classes with inline `style` props for spacing adjustments:
```tsx
<h2 className="lp-hero-title" style={{ fontSize: 'clamp(1.5rem, 2.2vw, 2rem)', marginBottom: 0 }}>
```
```tsx
<div className="lp-deal-grid" style={{ marginTop: 20 }}>
```

This pattern is used ~30 times across the homepage alone. It works but makes the CSS system difficult to maintain — any redesign requires touching both the stylesheet and JSX files. These should be utility classes or CSS custom properties.

---

## Visual Consistency Scorecard

| Element | Consistent? | Notes |
|---|---|---|
| Button variants (`lp-btn-primary`, `lp-btn-outline`) | ✅ Consistent | Applied everywhere |
| Input styling (`lp-input`) | ✅ Consistent | Login/signup/forms all aligned |
| Card surfaces (`lp-glass-card`, `lp-sheet`) | ⚠️ Mostly | Privacy/Terms misuse `lp-city-chip` |
| Error states | ⚠️ Mostly | Colors consistent; positioning varies |
| Loading states | ❌ Inconsistent | Range from skeleton to plain text to nothing |
| Typography scale | ✅ Consistent | Clamp functions applied correctly |
| Status badge colors | ✅ Consistent | Booking status palette is well-defined |
| Form field labels | ⚠️ Mostly | `lp-field-label` used on login; raw `text-xs` on signup |
| Empty states | ❌ Inconsistent | Each page handles its own empty state differently |
| Success messages | ❌ Inconsistent | Some inline, some modal, no toasts |

---

## Priority Fix List (Aesthetic & UX)

Ordered by visual and user-experience impact:

1. **Add image lightbox/gallery to service detail** — this is the single biggest missing visual feature on a booking platform.

2. **Add customer reviews section to service detail** — users cannot make informed booking decisions without social proof.

3. **Change checkout button label to reflect reality** — "Confirm Booking" or "Reserve Now" instead of anything implying payment until a payment gateway is wired.

4. **Add a global toast notification system** — replace all inline success/error messages with consistent positioned toasts.

5. **Add mobile card view for My Bookings table** — the 6-column table is unreadable on phones; collapse to card layout below 640px.

6. **Add skeleton loading to Services Browse and Service Type pages** — the empty content flash on load is jarring on slow connections.

7. **Fix Privacy and Terms pages to use correct semantic containers** — replace `lp-city-chip` with proper section/card components and add a table of contents.

8. **Add inline discount code validation** — "Apply" button + live feedback (green checkmark or red error) instead of silent field with post-submit discovery.

9. **Move booking panel to sticky sidebar on desktop service detail** — the user shouldn't have to scroll past 40 menu items to book a restaurant.

10. **Fix city chip navigation to actually pre-filter the destination** — clicking "Beirut" should open a service list filtered to Beirut, not a generic services page.

11. **Add room type information to hotel room selector** — numbered grid of 20 rooms means nothing; show room types with capacity and amenities.

12. **Expand the About page** — at minimum: a mission statement, a brief story, a team credit or founder note, and coverage map.

13. **Move booking panel availability status to a prominent visual indicator** — tiny `text-xs` text is insufficient for the most important booking signal.

14. **Add book-detail view in My Bookings** — clicking a booking row should open the full booking details, not just the cancel option.

15. **Translate password strength labels** — `"Weak"`, `"Fair"`, `"Good"`, `"Strong"` are hardcoded English in the signup component.

---

## What the Site Gets Definitively Right

It would be unfair to end without emphasizing what lands:

- **The glassmorphism system is production-quality** — not a tutorial copy but a genuinely designed, consistently applied visual language.
- **GSAP scroll-reveal orchestration** — the staggered entrance animations and parallax tilt on the hero are sophisticated and smooth.
- **Sticky glass navbar** — floating island design, smooth transitions, mobile hamburger. All well-executed.
- **Booking panel** — the guest counter, date picker, availability check, taxi type buttons, and price summary panel are a complete, well-designed booking UI.
- **Checkout confirmation screen** — green check + large monospace reference + copy button + three clear CTAs is a clean end-of-flow.
- **Sign-up password strength meter** — 4-level visual indicator with colored bars, a feature most competitors skip.
- **My Bookings cancel modal** — glassmorphism modal with backdrop blur, reason textarea, and correct button hierarchy.
- **`prefers-reduced-motion` support** — all GSAP animations correctly disable and snap to final state, which almost no landing page gets right.
- **The overall color palette** — rare, distinctive, and appropriate for a Lebanese travel platform.

---

*Report generated by external UI/UX critic review of source code at `front/my-app/src/apps/landing/`. Screenshots would be needed to verify rendering-specific issues; findings based on CSS and JSX analysis.*
