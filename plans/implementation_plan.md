## 1. Architectural Strategy & Source Blueprints
The system will be transitioned from a basic monolith to a **Domain-Driven Modular Monolith** using the following reports as technical blueprints:

### Key Reports & Blueprints
- **[System Evolution](file:///c:/Users/BL3T/Desktop/Project/docs/SYSTEM_EVOLUTION_REPORT.md)**: Defines the shift from microservices to a "Future-Ready" Modular Monolith.
- **[Modular Structure](file:///c:/Users/BL3T/Desktop/Project/docs/MODULAR_MONOLITH_STRUCTURE.md)**: Establishes package hierarchy and `sys_`, `hotel_`, `pay_` table prefix standards.
- **[Role Management](file:///c:/Users/BL3T/Desktop/Project/docs/ROLE_MANAGEMENT_REPORT.md)**: Details the 7-group RBAC hierarchy and permission management workflows.
- **[Payment Methods](file:///c:/Users/BL3T/Desktop/Project/docs/PAYMENT_METHODS.md)**: Outlines the Visa card integration stack and financial safety protocols.
- **[Pricing Methods](file:///c:/Users/BL3T/Desktop/Project/docs/PRICING_METHODS.md)**: Formalizes vertical-specific pricing models and multi-currency handling.
- **[Dynamic Commission](file:///c:/Users/BL3T/Desktop/Project/docs/DYNAMIC_COMMISSION_REPORT.md)**: Defines the hierarchical discount formula (Provider -> Company -> Commission).
- **[Infrastructure & Frontend](file:///c:/Users/BL3T/Desktop/Project/docs/INFRASTRUCTURE_REPORT.md)**: Details the Docker containerization and dual-portal frontend ecosystem.
- **[Technical Implementation Guide](file:///c:/Users/BL3T/Desktop/Project/docs/MONOLITH_IMPLEMENTATION.md)**: Provides the code-level standards for interfaces and state machines.

---

## 2. Implementation Phases

### Phase 1: Foundation & Security Core (`sys_` Module)
- **Modular Skeleton**: Setup the package hierarchy as defined in [MODULAR_MONOLITH_STRUCTURE.md](file:///c:/Users/BL3T/Desktop/Project/docs/MODULAR_MONOLITH_STRUCTURE.md).
- **Advanced RBAC**: Implement the 7-group hierarchy and the `resource:action` matrix defined in [ROLE_MANAGEMENT_REPORT.md](file:///c:/Users/BL3T/Desktop/Project/docs/ROLE_MANAGEMENT_REPORT.md).
- **Audit System**: Core interceptor for all sensitive operations (deletes, role changes, financial overrides).

### Phase 2: Pricing & Commission Engine (`pay_` / `core` Module)
- **Dynamic Commission Layer**: Implement the provider-level commission overrides (Default 10%).
- **Hierarchical Calculation**: Build the `PricingEngine` to handle the Provider-then-Company discount sequence as outlined in [DYNAMIC_COMMISSION_REPORT.md](file:///c:/Users/BL3T/Desktop/Project/docs/DYNAMIC_COMMISSION_REPORT.md).

### Phase 3: Financial & Payment Integration (`pay_` Module)
- **Visa Integration**: Build the `VisaPaymentAdapter` using the secure tokenization workflow detailed in [PAYMENT_METHODS.md](file:///c:/Users/BL3T/Desktop/Project/docs/PAYMENT_METHODS.md).
- **Webhook Infrastructure**: Dedicated endpoints for Gateway callbacks with HMAC signature verification.
- **Idempotency Layer**: Database-backed request deduplication for all financial transactions.

### Phase 4: Content & Multilingual Strategy
- **Schema Migration**: Add `_ar` columns to bookable entities and populate the `i18n_labels` table.
- **Three-Tier Frontend Deployment**:
    - **Landing Page**: Public marketing and discovery for all users.
    - **Back-Office Dashboard**: Enhanced React Dashboard for G1-G6 staff.
    - **Client Portal**: Dedicated React SPA for G7 Providers.

---

## 3. Migration and Data Safety
1.  **Step 1**: Migrate existing flat tables to prefixed tables (rename operations).
2.  **Step 2**: Extract direct repository imports and replace them with Interface-based service calls.
3.  **Step 3**: Introduce ArchUnit tests to prevent future boundary violations.

---

## 4. Verification Plan

### Automated Tests
- **ArchUnit**: Verify module isolation and DB prefix compliance.
- **Pricing Matrix Tests**: 100+ test cases for various stacked discount and commission scenarios.
- **Mock Gateway Tests**: Simulate 3DS redirects and webhook fallbacks.

### Manual Verification
- **RBAC Audit**: Verify that a Manager cannot view/edit Admin-level commission settings.
- **RTL UI Check**: Validate the Arabic layout in both the Mobile App and Client Portal.

---
*Implementation roadmap finalized by Antigravity.*
