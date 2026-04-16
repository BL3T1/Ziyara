# Figma Needs by Phase

What to provide from Figma for each phase of the [Frontend Implementation Plan](./FRONTEND_IMPLEMENTATION_PLAN.md). **Provide these when the phase starts** (or confirm we can reuse existing design).

**Providing a link to the code:** You can provide a **link or path to the code** where each design is needed (e.g. the component or page file). That’s helpful: it tells the designer exactly which screen/component to design for. Add the link in the **Code location** column in the checklist below (or next to each phase section).

---

## Design decisions (provided)

Decisions below are locked in; no extra Figma is required for these unless you change your mind.

| Item | Decision | Reference / note |
|------|----------|------------------|
| **Theme toggle (dark/light)** | Use [Dark Mode Toggle – lndev/ui](https://ui.lndev.me/components/dark-mode-toggle#code) as design/code reference. | Phase 1 – dashboard header. |
| **Commission slider/input** | Use a **standard input** (no custom slider). | Phase 2. |
| **Provider detail panel** | **Same as company dashboard** (reuse layout/components). | Phase 2. |
| **Priority badges** | Use the **same style for the whole project**; nothing special necessary. | Phase 4. |
| **Refund modal** | Use the **same style as the dashboard**; build a new, pretty modal in code (no separate Figma). | Phase 3. |
| **Provider dashboard** (sidebar, overview, listings table) | **Same as company dashboard** (reuse; only nav labels and data scope differ). | Phase 5. |
| **Search (Phase 6)** | **Two entry points:** (1) **Global search** (⌘K or button) for **Super Admin + managers** – search whatever they need (bookings, providers, users, etc.). (2) **“Search in deleted”** – **Super Admin only** – separate button that searches within deleted/archived records. Build both in dashboard style; two buttons in the UI (global search + search deleted for G1). | See Phase 6 in implementation plan. |
| **Language switcher (EN/AR)** | Use **Lucide icon** for the change-language control (e.g. `Languages` from `lucide-react`). Place in header or user menu; no separate Figma icon needed. | Phase 7. |

**Still needed from Figma (or confirm in design):** Layout/placement of language switcher in header or menu (optional; we use Lucide icon). Optional: Escalation button style (Phase 4), glassmorphism / micro-animations (Phase 8).

---

## Phase 0 – Scope & Design Baseline

| Need | Required? | Notes |
|------|-----------|--------|
| Nothing | — | Only confirm you have (or will provide) the items listed in Phases 1–7 when those phases start. |

---

## Phase 1 – Foundation & Unblock Portal

| # | What to provide | Required? | Notes |
|---|-----------------|-----------|--------|
| 1 | **Theme toggle (dark/light)** in the dashboard header | ✅ **Decided** | Use [Dark Mode Toggle – lndev/ui](https://ui.lndev.me/components/dark-mode-toggle#code) as reference. |

---

## Phase 2 – Company Dashboard: Global Overview & Provider Management

| # | What to provide | Required? | Notes |
|---|-----------------|-----------|--------|
| 1 | **Provider detail panel** | ✅ **Decided** | Same as company dashboard (reuse). |
| 2 | **Commission slider / input** | ✅ **Decided** | Use standard input. |

---

## Phase 3 – Company Dashboard: Financials & RBAC

| # | What to provide | Required? | Notes |
|---|-----------------|-----------|--------|
| 1 | **Refund modal** | ✅ **Decided** | Use same style as dashboard; create a new pretty modal in code (reason field + Confirm/Cancel). No separate Figma. |

---

## Phase 4 – Company Dashboard: Support & Role-Specific Views

| # | What to provide | Required? | Notes |
|---|-----------------|-----------|--------|
| 1 | **Priority badges** (Low / Medium / High / Urgent) | ✅ **Decided** | Use the same style for all the project; nothing special necessary. |
| 2 | **Escalation button** | Optional | Style or component for “Escalate” (e.g. reassign to manager). |

---

## Phase 5 – Client Portal (Provider Dashboard)

| # | What to provide | Required? | Notes |
|---|-----------------|-----------|--------|
| 1 | **Provider dashboard: sidebar** | ✅ **Decided** | Same as company dashboard. |
| 2 | **Provider dashboard: overview** | ✅ **Decided** | Same as company dashboard. |
| 3 | **Provider dashboard: listings table** | ✅ **Decided** | Same as company dashboard. |

---

## Phase 6 – Cross-Cutting: Search, Tables, Permissions

| # | What to provide | Required? | Notes |
|---|-----------------|-----------|--------|
| 1 | **Global search** | ✅ **Decided** | **Two buttons:** (1) **Global search** (⌘K or button) for **Super Admin + managers** – search bookings, providers, users, etc. (2) **“Search in deleted”** – **Super Admin only** – separate button to search within deleted/archived records. Build both in dashboard style (no separate Figma). |

---

## Phase 7 – i18n & RTL

| # | What to provide | Required? | Notes |
|---|-----------------|-----------|--------|
| 1 | **Language switcher (EN / AR)** | ✅ **Decided** | Use **Lucide icon** for the change-language control (e.g. `Languages` from `lucide-react`). Place in header or user/profile menu; toggle or dropdown for EN ↔ AR. No separate Figma icon needed. |

---

## Phase 8 – Polish (Optional)

| # | What to provide | Required? | Notes |
|---|-----------------|-----------|--------|
| 1 | **Glassmorphism** (frosted-glass sidebar/cards) | Optional | If your Figma uses frosted-glass for sidebar or cards, we’ll match it (backdrop-blur, transparency). |
| 2 | **Micro-animations** (transitions, hover, health indicators) | Optional | We can follow DASHBOARD_DESIGN_REPORT or your Figma for subtle motion. |

---

## One-Page Checklist (for designer or PM)

| Phase | When to provide | Figma needed | Code location (link when you provide) |
|-------|-----------------|--------------|----------------------------------------|
| **1** | Start of Phase 1 | ✅ Theme toggle: use [lndev/ui Dark Mode Toggle](https://ui.lndev.me/components/dark-mode-toggle#code) | `front/my-app/src/components/DashboardHeader.tsx` |
| **2** | Start of Phase 2 | ✅ Provider detail = company dashboard; Commission = standard input | Providers page / provider detail |
| **3** | Start of Phase 3 | ✅ Refund modal: same dashboard style, build in code | Payments / ledger page |
| **4** | Start of Phase 4 | ✅ Priority badges: same style project-wide; *(optional)* Escalation button | Ticket queue, ticket detail |
| **5** | Start of Phase 5 | ✅ Provider dashboard: same as company dashboard | ClientPortalLayout, portal Overview |
| **6** | Start of Phase 6 | ✅ Two buttons: Global search (G1+managers) + “Search in deleted” (G1 only); build in dashboard style | Dashboard header / layout |
| **7** | Start of Phase 7 | ✅ **Lucide icon** for language switcher (e.g. `Languages`) in header or user menu | DashboardHeader or user dropdown |
| **8** | Start of Phase 8 | *(Optional)* Glassmorphism, micro-animations | Sidebar, Card components |

---

**How to use the Code location column:** Paste a link to the repo (e.g. GitHub file URL or Cursor link) or the file path where that design will be implemented. Example: `https://github.com/.../blob/main/front/my-app/src/components/DashboardHeader.tsx` for the theme toggle.

*Source: [FRONTEND_IMPLEMENTATION_PLAN.md](./FRONTEND_IMPLEMENTATION_PLAN.md). Current Figma ref: channel `7s9roivi` (Talk to Figma).*
