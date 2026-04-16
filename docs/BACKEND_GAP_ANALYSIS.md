# Backend Report vs Plans – Gap Analysis

This document compares **BACKEND_REPORT.md** (current backend capabilities) against the requirements from all plan documents and external specs.

---

## Executive Summary

| Category | Status | Notes |
|----------|--------|-------|
| **Core API** | ~70% | Auth, bookings, payments, tickets, roles largely covered |
| **Auth & Users** | ~95% | Implemented: register, OTP, forgot/reset password, user CRUD, freeze, login-history (Phase 1) |
| **Services & Providers** | ~50% | Missing services CRUD, provider approve/suspend/delete |
| **Complaints** | ~20% | Only comments; missing full lifecycle |
| **Discounts** | ~15% | Only validate; missing CRUD, approve, apply |
| **Refunds** | ~0% | No refund endpoint documented |
| **Dashboard** | ~80% | KPIs covered; missing service health, group-specific data |
| **Architecture** | ~30% | No table prefixes (sys_, pay_, hotel_); different structure |
| **Pricing & Commission** | ~90% | Formula, commission override, seasonal multiplier covered |

---

## 1. Authentication (REQUIREMENTS_ANALYSIS, SyRS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| POST /auth/login | ✅ | Covered |
| POST /auth/register | ✅ | **Implemented** (Phase 1) |
| POST /auth/otp/send | ✅ | **Implemented** (Phase 1, stub) |
| POST /auth/otp/verify | ✅ | **Implemented** (Phase 1) |
| POST /auth/logout | ✅ | Covered |
| POST /auth/refresh | ✅ | Covered |
| POST /auth/password/forgot | ✅ | **Implemented** (Phase 1, stub) |
| POST /auth/password/reset | ✅ | **Implemented** (Phase 1) |

---

## 2. User Management (REQUIREMENTS_ANALYSIS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /users | ✅ | **Implemented** (Phase 1, jOOQ) |
| POST /users | ✅ | **Implemented** (Phase 1) |
| GET /users/{id} | ✅ | **Implemented** (Phase 1) |
| PUT /users/{id} | ✅ | **Implemented** (Phase 1) |
| DELETE /users/{id} | ✅ | **Implemented** (Phase 1, soft-delete) |
| POST /users/{id}/freeze | ✅ | **Implemented** (Phase 1) |
| POST /users/{id}/unfreeze | ✅ | **Implemented** (Phase 1) |
| POST /users/{id}/reset-password | ✅ | **Implemented** (Phase 1) |
| GET /users/{id}/login-history | ✅ | **Implemented** (Phase 1, last login from users table) |

---

## 3. Service Providers (REQUIREMENTS_ANALYSIS, DASHBOARD_DESIGN)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /providers | ✅ | Covered |
| POST /providers | ✅ | Covered |
| GET /providers/{id} | ✅ | Covered |
| PUT /providers/{id} | ✅ | Covered |
| DELETE /providers/{id} | ✅ | **Implemented** (Phase 3, soft → INACTIVE) |
| POST /providers/{id}/approve | ✅ | **Implemented** (Phase 3) |
| POST /providers/{id}/suspend | ✅ | **Implemented** (Phase 3) |
| PATCH /providers/{id}/commission | ✅ | Covered (DYNAMIC_COMMISSION) |
| Provider filtering (vertical, status) | ❌ | Not documented |
| Document verification gallery | ❌ | **Missing** |

---

## 4. Services (REQUIREMENTS_ANALYSIS, PRICING_METHODS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /services | ✅ | **Implemented** (Phase 2, ServiceController, jOOQ) |
| POST /services | ✅ | **Implemented** (Phase 2) |
| GET /services/{id} | ✅ | **Implemented** (Phase 2) |
| PUT /services/{id} | ✅ | **Implemented** (Phase 2) |
| DELETE /services/{id} | ✅ | **Implemented** (Phase 2, soft-delete) |
| GET /services/search | ✅ | **Implemented** (Phase 2) |
| GET /services/{id}/availability | ❌ | **Missing** |
| POST /services/{id}/images | ❌ | **Missing** |
| DELETE /services/{id}/images/{imageId} | ❌ | **Missing** |

---

## 5. Bookings (REQUIREMENTS_ANALYSIS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /bookings | ✅ | Covered |
| POST /bookings | ✅ | Covered |
| GET /bookings/{id} | ✅ | Covered |
| PUT /bookings/{id} | ✅ | **Implemented** (Phase 3) |
| POST /bookings/{id}/cancel | ✅ | Covered |
| POST /bookings/{id}/confirm | ✅ | **Implemented** (Phase 3) |
| GET /bookings/{id}/voucher | ✅ | **Implemented** (Phase 3) |
| POST /bookings/{id}/taxi | ✅ | **Implemented** (Phase 3, vehicle STANDARD) |

---

## 6. Payments & Refunds (PAYMENT_METHODS, REQUIREMENTS_ANALYSIS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| POST /payments | ✅ | Covered |
| GET /payments/{id} | ✅ | Covered |
| POST /payments/{id}/refund | ✅ | **Implemented** (Phase 2) |
| GET /payments/transaction/{ref} | ✅ | Covered |
| POST /pay/webhooks | ✅ | Covered |
| Idempotency (idempotency_key) | ✅ | Schema supports |
| Visa/3DS integration | ⚠️ | Adapter pattern mentioned in plans; not in report |
| Bank Transfer, Wallet, Cash on Service | ⚠️ | Schema supports; API not detailed |

---

## 7. Complaints (REQUIREMENTS_ANALYSIS, DASHBOARD_DESIGN)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /complaints | ✅ | **Implemented** (Phase 2, jOOQ) |
| POST /complaints | ✅ | **Implemented** (Phase 2) |
| GET /complaints/{id} | ✅ | **Implemented** (Phase 2) |
| PUT /complaints/{id} | ✅ | **Implemented** (Phase 2) |
| POST /complaints/{id}/assign | ✅ | **Implemented** (Phase 2) |
| POST /complaints/{id}/resolve | ✅ | **Implemented** (Phase 2) |
| POST /complaints/{id}/escalate | ✅ | **Implemented** (Phase 2) |
| POST /complaints/{id}/close | ✅ | **Implemented** (Phase 2) |
| GET /complaints/{id}/comments | ✅ | Covered |
| POST /complaints/{id}/comments | ✅ | Covered |

---

## 8. Discounts (REQUIREMENTS_ANALYSIS, PRICING_METHODS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /discounts | ✅ | **Implemented** (Phase 2, jOOQ) |
| POST /discounts | ✅ | **Implemented** (Phase 2) |
| GET /discounts/{id} | ✅ | **Implemented** (Phase 2) |
| PUT /discounts/{id} | ✅ | **Implemented** (Phase 2) |
| DELETE /discounts/{id} | ✅ | **Implemented** (Phase 2) |
| POST /discounts/{id}/approve | ✅ | **Implemented** (Phase 2) |
| POST /discounts/{id}/deactivate | ✅ | **Implemented** (Phase 2) |
| POST /discounts/validate | ✅ | Covered |
| POST /discounts/apply | ❌ | **Missing** (validate only) |

---

## 9. Reviews (REQUIREMENTS_ANALYSIS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /reviews | ✅ | By service; list with filters (Phase 3) |
| POST /reviews | ✅ | Covered |
| GET /reviews/{id} | ✅ | **Implemented** (Phase 3) |
| PUT /reviews/{id} | ✅ | **Implemented** (Phase 3) |
| DELETE /reviews/{id} | ✅ | **Implemented** (Phase 3) |
| POST /reviews/{id}/moderate | ✅ | **Implemented** (Phase 3) |
| POST /reviews/{id}/respond | ✅ | Covered |

---

## 10. Notifications (REQUIREMENTS_ANALYSIS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /notifications | ✅ | Covered |
| GET /notifications/{id} | ✅ | **Implemented** (Phase 3, ownership enforced) |
| POST /notifications/{id}/read | ✅ | Covered (PATCH) |
| POST /notifications/read-all | ✅ | **Implemented** (Phase 3) |

---

## 11. Exchange Rates (REQUIREMENTS_ANALYSIS)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /exchange-rates | ⚠️ | Exposed as GET /currency/rates |
| POST /exchange-rates | ✅ | **Implemented** as POST /currency/rates (Phase 3) |
| PUT /exchange-rates/{id} | ✅ | **Implemented** as PUT /currency/rates/{id} (Phase 3) |
| GET /exchange-rates/convert | ✅ | As GET /currency/convert |

---

## 12. Dashboard (DASHBOARD_DESIGN_REPORT)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| GET /dashboard/revenue | ✅ | Covered |
| GET /dashboard/bookings | ✅ | Covered |
| GET /dashboard/customers | ✅ | Covered |
| GET /dashboard/providers | ✅ | Covered |
| GET /dashboard/kpis | ✅ | Covered |
| GET /dashboard/activity | ✅ | Covered |
| Service health (Hotels/Taxis/Restaurants load) | ✅ | **Implemented** GET /dashboard/service-health (Phase 4, jOOQ) |
| Group-specific KPIs (G1–G6) | ❌ | Not documented |
| Commission analysis (Base vs Ziyarah Delta) | ✅ | **Implemented** GET /dashboard/commission-analysis (Phase 4) |
| Payout summary for providers | ✅ | **Implemented** GET /dashboard/payouts (Phase 4) |

---

## 13. RBAC & Role Management (ROLE_MANAGEMENT_REPORT)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| Roles, permissions, groups | ✅ | Covered |
| Create custom role | ✅ | Covered |
| Update role permissions | ✅ | Covered |
| Delete role with reassignment | ✅ | Covered |
| Permission catalogue | ✅ | Covered |
| 50+ resource:action permissions | ⚠️ | Report mentions; actual count not listed |
| G1–G7 hierarchy | ⚠️ | Schema has roles; group mapping not detailed |

---

## 14. Pricing & Commission (PRICING_METHODS, DYNAMIC_COMMISSION_REPORT)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| Hierarchical formula (Provider → Company → Commission) | ✅ | Covered |
| Default 10% commission | ✅ | Covered |
| Provider-level commission override | ✅ | Covered |
| Seasonal multiplier | ✅ | Covered |
| Multi-currency / exchange rates | ✅ | Covered |
| Vertical-specific pricing (Hotel, Restaurant, Taxi, Trip) | ⚠️ | Logic in PricingService; not fully documented |

---

## 15. Architecture (MODULAR_MONOLITH_STRUCTURE, SYSTEM_EVOLUTION)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| Table prefixes (sys_, hotel_, pay_, taxi_, disc_) | ❌ | **Not implemented** – flat schema |
| Module isolation (no cross-module JOINs) | ❌ | **Not implemented** – shared schema |
| Interface-only inter-module communication | ❌ | **Not implemented** |
| Domain events (Kafka/Spring events) | ❌ | **Not documented** |
| Package structure (modules.hotel, modules.payment) | ❌ | **Different** – Clean Architecture layers |

---

## 16. Infrastructure (INFRASTRUCTURE_REPORT)

| Required | In Backend Report | Status |
|----------|-------------------|--------|
| Docker containerization | ❌ | Not in backend report (deployment) |
| Kafka broker | ❌ | Not in backend report |
| Redis cache | ⚠️ | Mentioned in application.yml; not in report |
| Nginx proxy routing | ❌ | Not in backend report |

---

## 17. Additional Gaps from Plans

| Source | Gap |
|--------|-----|
| **PAYMENT_METHODS** | Visa adapter, 3DS flow, HMAC webhook verification |
| **REQUIREMENTS_ANALYSIS** | 2FA/OTP, account lockout, password complexity |
| **REQUIREMENTS_ANALYSIS** | i18n_labels, _ar columns for bookable entities |
| **DASHBOARD_DESIGN** | Global search (⌘K), data table export (Excel/PDF) |
| **implementation_plan** | ArchUnit tests, pricing matrix tests |

---

## Recommendations

1. **High priority** – Add missing core APIs:  
   - User management (list, create, update, freeze)  
   - Complaints full lifecycle  
   - Discounts CRUD + approve  
   - Refunds  
   - Services CRUD + search + availability  

2. **Medium priority** – Extend existing APIs:  
   - Auth: register, OTP, password reset  
   - Providers: approve, suspend, delete  
   - Bookings: confirm, voucher, taxi add-on  
   - Notifications: read-all  

3. **Lower priority / future** – Architectural alignment:  
   - Table prefix migration (sys_, pay_, hotel_)  
   - Module boundaries and events  
   - Group-specific dashboard endpoints  

4. **Update BACKEND_REPORT.md** – Add a “Gap vs Plans” section and keep it in sync with this analysis.
