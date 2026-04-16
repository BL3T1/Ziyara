# Modular Monolith: Package & Database Structure

This document establishes the structural standards for the Ziyarah Modular Monolith. Consistency here is critical to preventing technical debt and ensuring the system is "Future-Ready".

## 1. Package Structure Standards

Every domain module must follow this internal package structure to maintain isolation.

```
com.ziyarah.modules.[domain]
├── api             # Public Interface (e.g., [Domain]Service.java)
├── model           # Domain Entities and DTOs
├── service         # Internal Implementation
├── repository      # Data access (Table Prefixed)
├── web             # Module-specific Controllers (if any)
└── events          # Producers and Consumers for cross-module events
```

### Hierarchy Rules
- **No Direct Access**: `com.ziyarah.modules.hotel.service` must NOT be imported by the `payment` module.
- **Entry Points**: Only classes in `com.ziyarah.modules.[domain].api` are accessible from outside the module.

---

## 2. Database Naming Conventions

All tables must use the following prefixes to enforce logical database independence.

| Domain Module | Table Prefix | Logic |
| :--- | :--- | :--- |
| **System/Core** | `sys_` | Users, Roles, Audits, i18n |
| **Hotels** | `hotel_` | Listings, Bookings (Hotel specific), Reviews |
| **Restaurants** | `rest_` | Tables, Menus, Reservations |
| **Taxis** | `taxi_` | Drivers, Rides, Pricing |
| **Payments** | `pay_` | Transactions, Escrows, Refunds |
| **Discounts** | `disc_` | Coupons, Campaigns, Approvals |

### Schema Rules
- **No Foreign Keys**: Use IDs (UUIDs) for cross-module relationships.
- **No Cross-Module JOINs**: Data aggregation must happen at the Application or API layer.

---

## 3. Communication Patterns

### Synchronous (Request/Response)
- **Pattern**: Module A calls `ModuleBService.get(...)`.
- **Constraint**: Input and output must be DTOs, never JPA Entities.

### Asynchronous (Events)
- **Pattern**: `ModuleA` publishes `DomainEvent`. `ModuleB` listens and reacts.
- **Usage**: Use for notifications, audit streams, or eventual consistency updates.

```java
// Example
applicationEventPublisher.publishEvent(new BookingCreatedEvent(bookingId));
```

---
*Standards defined by Antigravity.*
