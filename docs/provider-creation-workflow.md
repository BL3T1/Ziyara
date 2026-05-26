# Provider Creation Workflow

## Overview

A **service provider** (hotel, taxi company, restaurant, etc.) is the central entity in the Ziyara platform. Every bookable service belongs to a provider, and every provider has exactly one **manager account** (a user who can log in and manage that provider's listings).

---

## Who Can Create a Provider

| Role | Can Create? | Initial Status |
|------|-------------|----------------|
| Platform Admin | Yes | `ACTIVE` |
| Company Manager | Yes | `ACTIVE` |
| Company Staff | Yes | `ACTIVE` |
| Sales Agent | Yes | `PENDING_APPROVAL` |

Sales agents create providers on behalf of clients. The provider goes into `PENDING_APPROVAL` and must be approved by a company staff member or admin before it becomes visible.

---

## Step-by-Step: Creating a Provider (Management)

### 1. Navigate to the Providers List

Go to **Management → Providers**. Click **"Create Provider"** (top-right).

### 2. Fill in the Provider Details Form

The form is split into two sections:

#### Section 1 — Provider Details
| Field | Required | Notes |
|-------|----------|-------|
| Name | Yes | Display name (e.g. "Grand Palace Hotel") |
| Type | Yes | HOTEL, TAXI, RESTAURANT, etc. |
| Phone | Yes | Primary contact number |
| Contact Email | No | Public-facing email |
| Address | Yes | Physical or registered address |
| Registration Number | No | Commercial registration / CR number |
| Company Profit Margin | No (defaults to 10%) | % of each booking revenue kept by the platform |
| Description | No | Shown on the provider profile page |
| Logo URL | No | Absolute URL or relative `/media/…` path |

#### Section 2 — Manager Account

Choose one of two modes:

- **Create new account**: Enter manager email, password, and optional phone. A new user is created with the `PROVIDER_MANAGER` role, linked to this provider.
- **Link existing account**: Enter the email of a user who already exists in the system. That user is assigned the `PROVIDER_MANAGER` role for this provider.

### 3. Submit

On success:
- The provider record is created with status `ACTIVE` (management users) or `PENDING_APPROVAL` (sales agents).
- The manager receives access to the Provider Portal (`/portal/…`).
- The provider appears immediately in the **Providers list** and in the relevant **service page** (e.g. Hotels page, Taxis page).

---

## Company Profit Margin

The profit margin is how much of each booking's revenue the platform retains before paying out to the provider.

- **Default**: 10%
- **Who sets it**: Platform Admin or Company Manager/Staff at creation time, or any time via **Edit Profit** in the Providers list or the provider's edit page.
- **Who can see it**: Only roles with `canViewProviderCommission` permission (Platform Admin, Company Manager, Company Staff). Providers and customers never see this value.
- **Backend field**: `commissionRate` (the UI label says "Company Profit Margin").

---

## How the Provider Appears on Service Pages

Once created, the provider shows up automatically on the relevant service page (e.g. **Hotels**, **Taxis**, **Restaurants**). The service page has two sections:

### Section 1 — Services/Listings
A table or grid of the actual bookable items (hotel rooms, taxi vehicles, menu items) owned by this provider. Initially empty until the manager adds listings.

### Section 2 — Partner Accounts
A card grid showing every provider linked to that service type. Each card displays:
- Provider name and type
- Status badge (ACTIVE = green, SUSPENDED = red, PENDING = amber)
- Contact email and phone
- Company profit margin (visible to permitted roles only)
- Average rating (if available)
- **"Edit Account"** link (for company staff) or **"View Account"** link (for other roles) — goes directly to `/management/providers/{id}`

This means staff can jump straight from a service page to the provider's edit page without going through the provider list.

---

## Provider Status Lifecycle

```
                    ┌─────────────────┐
Sales agent creates │ PENDING_APPROVAL │
                    └────────┬────────┘
                             │ Approve (admin/staff)
                             ▼
              ┌──────────────────────────┐
              │          ACTIVE          │◄────────────────┐
              └──────┬──────────┬────────┘                 │
                     │          │ Suspend                   │ Reinstate
              Reject │          ▼                           │
                     │   ┌───────────┐                     │
                     │   │ SUSPENDED │─────────────────────┘
                     │   └───────────┘
                     ▼
              ┌──────────────┐
              │   INACTIVE   │
              └──────────────┘
```

Additional statuses: `PENDING_VERIFICATION`, `REJECTED`, `BLOCKED`.

---

## Provider Edit Page (`/management/providers/{id}`)

The edit page is organized into sections:

1. **Account Info** (read-only): Type, registration number, rating, creation date
2. **Contact Details**: Name, phone, email, address
3. **Branding**: Description, logo URL with live preview
4. **Financial** (permitted roles only): Company profit margin (%)
5. **Governance** (admin/manager only): Status dropdown, verified checkbox

Changes are saved immediately on submit. The manager user can also edit their own provider's contact and branding details from the Provider Portal.

---

## API Reference

| Action | Method | Endpoint |
|--------|--------|----------|
| List providers | GET | `/api/providers?page=&size=&status=` |
| Get provider | GET | `/api/providers/{id}` |
| Create provider | POST | `/api/providers` |
| Update provider | PATCH | `/api/providers/{id}` |
| Approve | POST | `/api/providers/{id}/approve` |
| Reject | POST | `/api/providers/{id}/reject` |
| Suspend | POST | `/api/providers/{id}/suspend` |
| Update commission | PUT | `/api/providers/{id}/commission` |
