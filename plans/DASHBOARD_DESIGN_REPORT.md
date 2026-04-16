# Company Dashboard Design Report: Ziyarah Pro

This report outlines the user interface, experience, and functional requirements for the Ziyarah Company Dashboard (G1-G6 internal staff).

## 1. Design Aesthetics & UX Principles

The dashboard must feel **"Premium, Live, and Dynamic"**. It is the nerve center of the company.

- **Theme**: Sleek Dark Mode (Primary) with a high-contrast Light Mode toggle.
- **Glassmorphism**: Use subtle frosted-glass effects for sidebars and floating cards.
- **Micro-animations**: Smooth transitions between views, pulsing health indicators for server status, and hover-triggered data reveals.
- **Typography**: Modern sans-serif (e.g., *Inter* or *Outfit*) for maximum readability.
- **Color Palette**: 
    - *Primary*: Deep Royal Blue (#1A237E) or Vibrant Violet.
    - *Success*: Emerald Green.
    - *Warning*: Amber Gold.
    - *Danger*: Crimson Red.

---

## 2. Page Hierarchy & Modules

### 2.1 Global Overview (The Command Center)
- **KPI Metrics**: Real-time revenue, active bookings, total providers, and pending complaints.
- **Live Activity Feed**: Scrolling stream of system events (New booking, Provider payout, Security alert).
- **Service Health**: Mini-charts showing the load on Hotels, Taxis, and Restaurants.

### 2.2 Provider Management (The Onboarding Hub)
- **Listing**: Table with filtering by vertical, status (Approved/Pending/Suspended), and commission rate.
- **Provider Details**: 
    - Performance metrics (Booking volume, Review average).
    - **Commission Override**: A secure slider/input to set provider-specific percentages.
    - Document Verification: Gallery view for business licenses and ID documents.

### 2.3 Financials & Payments (The Vault)
- **Transaction Ledger**: Searchable list of all payments with 3DS status and gateway references.
- **Commission Analysis**: Visual breakdown of "Base Collected" vs. "Ziyarah Delta".
- **Refund Management**: One-click refund triggers with mandatory "Reason" field and audit trail.
- **Payouts**: Scheduled payout summary for providers.

### 2.4 RBAC & Security (The Shield)
- **Role Architect**: Visual interface to Create/Modify roles (G1-G6).
- **Permissions Matrix**: A grid of `resource:action` toggles (e.g., `hotel:delete`, `sys:audit`).
- **Audit Logs**: Granular search by user, IP, or specific data change (Old vs. New value).

### 2.5 Customer Support (The Bridge)
- **Ticket Queue**: Sorted by priority (Low/Medium/High/Urgent).
- **Conversation Portal**: Real-time chat interface for internal ticket comments.
- **Resolution Dashboard**: Stats on average resolution time and agent performance.

---

## 3. Role-Specific Dashboard Views (G1 - G6)

The dashboard dynamically adjusts its sidebar, widgets, and data based on the user's Group (G).

### 3.1 G1 - Super Admin (The Architect)
- **Primary View**: System Health & Infrastructure.
- **Key Widgets**: 
    - Server Load & Docker Container status (Live).
    - Failed Job Queue (Kafka/Background tasks).
    - Security Exception log (Top 5).
- **Unique Pages**: 
    - **Role Architect**: Create/Edit roles and assign permissions.
    - **Global Audit**: Searchable history of every system-wide change.
    - **DB Explorer**: Read-only view of schema metadata and table row counts.

### 3.2 G2 - Sales (The Growth Hub)
- **Primary View**: Provider Acquisition Pipeline.
- **Key Widgets**: 
    - New Provider Applications (Count).
    - Top 5 "Hot" Verticals (where providers are needed).
    - Sales Rep conversion rates (leaderboard).
- **Unique Pages**: 
    - **Leads Manager**: Track potential providers from "Pending" to "Approved".
    - **Contract Viewer**: Review uploaded business licenses and agreements.

### 3.3 G3 - Finance (The Treasury)
- **Primary View**: Revenue & Payout Forecast.
- **Key Widgets**: 
    - Total Gross Revenue vs. Net Commission.
    - Pending Provider Payouts (Value).
    - Refund Request volume.
- **Unique Pages**: 
    - **Payout Ledger**: Detailed logs of money sent to providers.
    - **Commission Controller**: Set provider-specific commission overrides (Requires Manager Approval).
    - **Tax & Compliance**: Generate VAT and other financial reports.

### 3.4 G4 - Support (The Help Desk)
- **Primary View**: Ticket & SLA Monitor.
- **Key Widgets**: 
    - Open Tickets by Priority (Doughnut chart).
    - Average Resolution Time (Live trend).
    - Customer Satisfaction (CSAT) score.
- **Unique Pages**: 
    - **Ticket Queue**: Unified inbox for Customer and Internal tickets.
    - **Escalation Hub**: Rapid reassignment of complex complaints to Managers.

### 3.5 G5 - Executive (The Strategy Room)
- **Primary View**: Macro Business Intelligence.
- **Key Widgets**: 
    - Year-over-Year Revenue Growth.
    - Market Share (Hotels vs Taxis vs Trips).
    - Commission yield per vertical.
- **Unique Pages**: 
    - **BI Reports**: Deep-dive analytics into long-term trends.
    - **Global Approvals**: Approve large discounts (up to 50%) or major provider onboardings.

### 3.6 G6 - HR (The Personnel Hub)
- **Primary View**: Employee & Team Overview.
- **Key Widgets**: 
    - Staff Activity Pulse (Logins/Actions).
    - New Hire Onboarding Progress.
    - Employee status (Active/Frozen).
- **Unique Pages**: 
    - **Personnel Directory**: Manage staff accounts and department assignments.
    - **Access Control**: Provision accounts with specific roles (cannot modify the roles themselves).

---

## 4. UI Layout Definition

### 4.1 The Global Frame
1.  **Static Sidebar (Left)**: 
    - Icons grouped by module (Identity, Finance, Verticals, Support).
    - Collapses to a slim icon-only bar on smaller screens.
2.  **Breadcrumb Nav (Top)**: 
    - Current path (e.g., `Home > Finance > Payouts`).
    - Search Bar (⌘K).
    - Notification bell with real-time badges.
3.  **User Profile (Top Right)**: 
    - Avatar + Role badge (e.g., "G1: Super Admin").
    - Theme Toggle (Dark/Light).
4.  **Actionable Content Area (Center)**: 
    - Dynamic grid layout for widgets and tables.

---

## 5. Technical UI Features

- **Responsive sidebar**: Collapsible navigation with context-aware icons.
- **Data Tables**: Server-side pagination, multi-column sorting, and Excel/PDF export.
- **Global Search**: Command-K (⌘K) style search bar to find a Booking/Provider/User from anywhere.
- **Real-time Sync**: Webhooks or Polling to update live numbers without page refresh.

---

## 6. Design Mockup Strategy
*Antigravity suggests using vibrant HSL-tailored colors and smooth gradients to ensure the dashboard looks state-of-the-art upon first glance.*

---
*Report generated by Antigravity.*
