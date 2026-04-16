# Domain-Driven Modular Monolith: Technical Guide

This document defines the implementation standards for the Ziyarah "Pro" Monolith, ensuring strict domain isolation and readiness for future microservices extraction.

## 1. Module Structure & Boundaries

Each business domain (Hotel, Taxi, Payment, etc.) is implemented as a self-contained module.

### Package Hierarchy
```
com.ziyarah
├── core (Shared kernel, Security, Utils)
├── modules
│   ├── hotel (Public API, domain logic, tables: hotel_*)
│   ├── taxi (Public API, domain logic, tables: taxi_*)
│   ├── booking (Public API, orchestrator, tables: bkg_*)
│   └── payment (Public API, 3rd party adapters, tables: pay_*)
└── api (Unified REST controllers)
```

### Communication Rules
- **Interface-Only**: Modules must interact only through `@Service` interfaces defined in the module's public package.
- **Dataroot Isolation**: A module cannot query another module's database tables or JPA entities.
- **DTO Exchange**: Data passed between modules must be converted to DTOs or Value Objects to prevent entity leakage.

---

## 2. Database Independence (Table Prefixes)

To simulate separate databases within a single PostgreSQL instance, we use strict table prefixes.

| Domain | Prefix | Example Tables |
| :--- | :--- | :--- |
| Core/Auth | `sys_` | `sys_users`, `sys_roles`, `sys_permissions` |
| Hotels | `hotel_` | `hotel_listings`, `hotel_rooms` |
| Taxis | `taxi_` | `taxi_providers`, `taxi_rides` |
| Bookings | `bkg_` | `bkg_reservations`, `bkg_vouchers` |
| Payments | `pay_` | `pay_transactions`, `pay_refunds` |

**Rule**: Cross-module JOINs are strictly prohibited. Relationships are maintained via UUIDs in the application code.

---

## 3. Advanced RBAC & Approval Workflows

### Unified Security Module (`sys_`)
RBAC is a core "cross-cutting" concern. All modules use the `sys_` prefixed roles and permissions for authorization.
- **Super Admin**: Has `sys:admin` permission, allowing role creation and global status overrides.
- **Custom Roles**: Created in the `sys_` module; permissions are mapped across all domain prefixes (e.g., `hotel:read`, `pay:refund`).

### Modular State Machines
Workflows like **Discount Approval** live within their respective modules but publish events upon completion.
- `DiscountModule` -> `ApprovalService` -> `InternalEvent` -> `NotificationModule(Listener)`.

---

## 4. Frontend Integration (Three-Tier Ecosystem)

The system supports three distinct frontend interfaces, each serving a specific niche and routed via the host header.

- **Landing Page (`ziyarah.com`)**: Next.js app for end-user discovery and provider outreach.
- **Dashboard (`dashboard.*`)**: React SPA for G1-G6 internal company operations.
- **Client Portal (`providers.*`)**: React SPA for G7 providers to manage their specific services.

---

## 5. Payment Integration (Visa & Adapters)

The `payment` module uses the **Adapter Pattern** to support multiple gateways.
- **Visa Gateway**: Handled via secure tokenization. The monolith never stores raw card data (PCI Compliance).
- **Callbacks & Idempotency**: Secure webhooks are verified via signature; idempotent request keys are mandatory to prevent duplicate transactions.
- **PCI Compliance**: All card capture happens on Gateway-hosted fields.
- **Webhook Security**: Inbound webhooks for payment success/failure are verified with a HMAC signature and processed by the `PaymentEventListener`.
- **Idempotency**: All payment requests require a unique `Idempotency-Key` stored in the `pay_` records to prevent double-charging on retries.

```java
public class VisaPaymentAdapter implements PaymentProvider {
    // Gateway-specific SDK integration
}
```

## 6. Pricing & Commission Hierarchy

The system follows a strict order for applying discounts and calculating service costs to maintain financial integrity.

### Calculation Sequence ( PricingEngine )
1.  **Provider Discount**: Deducted from the initial base price provided by the vertical module (Hotel, Taxi, etc.).
2.  **Company Discount**: Deducted from the price remaining after Step 1.
3.  **Ziyarah Commission**: The provider-specific percentage (default 10%) is applied to the final adjusted base from Step 2.

```java
// Logic within com.ziyarah.core.pricing.PricingEngine
BigDecimal effectiveBase = providerBase.subtract(providerDiscount).subtract(companyDiscount);
BigDecimal commissionScale = commissionRate.divide(new BigDecimal("100"));
BigDecimal finalPrice = effectiveBase.multiply(BigDecimal.ONE.add(commissionScale));
```

---
*Technical adaptation by Antigravity.*
