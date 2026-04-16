# Ziyara Front App – Project Context (Chat Memory)

Use this file to continue development from where you left off. Share it or reference it in a new chat.

---

## Project Overview

- **Location:** `front/my-app/`
- **Stack:** Vite 7, React 19, TypeScript, Tailwind CSS v4
- **Design source:** Figma (Ziyara dashboard design)
- **Multi-domain builds:** `VITE_APP_SURFACE=company | provider | landing` (see `src/config/appSurface.ts`, `docs/DOCKER_TESTING.md`, Docker Compose profile `multidomain`)

---

## What’s Built

### Layout & Structure

- **MainLayout** (`src/layouts/MainLayout.tsx`): Sidebar + header + main + footer
- **Sidebar** (`src/components/Sidebar.tsx`): Fixed left, dark (slate-900), role-based nav, scrollable
- **DashboardHeader** (`src/components/DashboardHeader.tsx`): Role label, page title, notifications, avatar, role switcher dropdown
- **Logo** (`src/components/Logo.tsx`): Uses `/logo.png`
- **Avatar** (`src/components/Avatar.tsx`): Uses `/default-avatar.svg` when no profile image (suitcase icon + “Ziyara” wordmark)

### Auth & Role-Based UI

- **AuthContext** (`src/context/AuthContext.tsx`): `user`, `setUser`, `isAuthenticated`
- **Roles:** `super_admin` | `admin` | `finance` | `support` | `provider` | `user`
- **Sidebar config** (`src/config/sidebar.ts`): Sections and items per role
- **Role switcher:** Avatar dropdown in header to preview different roles (sidebar updates)

### Routing

- **React Router** (`react-router-dom`): SPA navigation
- **PageLayout** (`src/layouts/PageLayout.tsx`): Wraps MainLayout, derives page title from route
- **Route config** (`src/config/routes.ts`): Path-to-title mapping for header
- **Pages:** `DashboardPage` (stats), role-specific pages under `src/pages/`, **provider portal** real pages: `ClientPortalOverview`, `PortalListingsPage`, `PortalListingFormPage` (`/portal/listings/new` and `/portal/listings/:id`), `PortalBookingsPage`, `PortalEarningsPage`, `PortalProfilePage` (`GET/PUT /providers/me`); `PortalStaffPage` / `PortalSupportPage` are roadmap/cross-link placeholders until Phase 4–5.

### Components

- **Card** (`src/components/Card.tsx`): Reusable card with hover effects
- **StatCard** (`src/components/StatCard.tsx`): Icon, label, value, trend
- **SidebarIcons** (`src/components/SidebarIcons.tsx`): Icons per nav item
- **Sidebar** uses `NavLink` for active state and SPA navigation
- **Barrel:** `src/components/index.ts` exports Card, DashboardHeader, Logo, Sidebar, StatCard, StatCardIcons

### Design Tokens (Tailwind)

- **Colors:** `primary: #1e4d6b`, `secondary: #ac9e78`
- **Font:** Inter, sans-serif
- **Base font size:** 16px
- **Spacing:** 8px base (1=8px, 2=16px, etc.)

### Assets

- **Logo:** `public/logo.png`
- **Default avatar:** `public/default-avatar.svg` – suitcase icon + “Ziyara” in secondary
- **Global CSS:** `src/global.css` – Tailwind import, body styles, sidebar scrollbar

---

## Key File Map

| Purpose              | Path                                      |
|----------------------|-------------------------------------------|
| Entry                | `src/main.tsx` (wraps App in AuthProvider) |
| App                  | `src/App.tsx` (MainLayout + stat cards)   |
| Auth types           | `src/types/auth.ts`                       |
| Sidebar config       | `src/config/sidebar.ts`                   |
| Tailwind config      | `tailwind.config.ts`                      |

---

## Possible Next Steps

- Provider portal: optional listing media/menu editors using existing `portalServicesAPI`
- Plan Phase 2: admin `SettingsPage` persistence, public contact/leads API
- Implement hamburger menu for mobile sidebar
- Add tests

## Provider API client

- `portalAPI` in `src/services/api.ts`: `/portal/dashboard`, `/portal/services` CRUD, `/portal/bookings`, `/portal/earnings`
- `providersAPI.getMe` / `updateMe`: `/providers/me`

---

## Figma Reference

- **Channel:** `7s9roivi` (Talk to Figma plugin)
- **Logo node:** `51:4766` (icon + “Ziyara” text, primary/secondary colors)
- **Dashboard design:** Sidebar (dark), header (white), cards, audit logs table
