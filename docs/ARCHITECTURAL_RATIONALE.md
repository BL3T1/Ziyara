# Architectural Documentation and Rationale

This document addresses the design patterns used in the Ziyarah project restoration and the impact of business logic changes across modules.

## 1. Design Pattern Analysis: Clean Architecture

### Why This Pattern?
The Ziyarah project follows **Clean Architecture** (specifically the Onion/Hexagonal variant). I maintained this pattern during restoration because:

1.  **Independence of Frameworks**: The core business logic (Domain Layer) does not depend on Spring Boot, JPA, or any external library. This makes it highly portable and testable.
2.  **Testability**: By separating the domain from infrastructure, we can unit test business rules without needing a database or web server.
3.  **Independence of Database**: The domain layer is agnostic of the persistence layer. We use Repository Interfaces in the domain and Adapters in the infrastructure layer.
4.  **Scalability**: New feature areas (e.g., adding a new service type like 'Flights') can be added by creating new domain entities and services without touching existing ones.

### Are there better alternatives?
- **For Small Projects**: A simple "Fat Controller" or "Transaction Script" pattern might be faster to implement but becomes unmanageable as complexity grows.
- **Microservices**: If the project grows to serve millions of users, splitting these layers into separate services (e.g., `payment-service`, `booking-service`) would be a logical next step, but Clean Architecture within each microservice is still a best practice.
- **Domain-Driven Design (DDD)**: The current architecture already incorporates DDD principles (Entities, Value Objects, Repositories). A "better" version would involve more strict Bounded Contexts if the domain becomes too large.

---

## 2. Impact Analysis: Business Logic Changes

In a Clean Architecture setup, the impact of changing business logic depends on where the change occurs:

### A. Changes in Domain Entities
- **High Impact**: Changing a core rule in a Domain Entity (e.g., `Booking.calculateTotal()`) will automatically affect all services that use it.
- **Propagation**: Since Services use these entities to perform logic, any change in return types or method signatures in the Domain will require updates in the Application Layer (Services).

### B. Changes in Application Services
- **Medium Impact**: Changing a service method (e.g., `PaymentService.initiatePayment()`) impacts the Presentation Layer (Controllers).
- **Isolation**: It typically *does not* affect the Domain Layer or the Infrastructure Layer (Persistence), as long as the repository interface remains the same.

### C. Changes in Infrastructure (Mappers/Adapters)
- **Low Impact**: Switching from one database to another (e.g., PostgreSQL to MongoDB) only requires a new Repository Adapter implementation. The business logic remains untouched.
- **Mappers**: Changing a mapping rule between JPA and Domain affects how data is persisted but does not change the core logic.

### D. Changes in DTOs
- **Presentation Impact**: Changing a Response DTO forces a change in the API contract, which impacts the Frontend.
- **Internal Safety**: Tools like MapStruct ensure that if a DTO is changed, the mapping will break at compile-time rather than run-time, providing a safety net.

## Summary table of Impacts

| Change Location | primary module Impacted | Ripple Effect | Risk Level |
|-----------------|-------------------------|---------------|------------|
| **Domain**      | Application (Services)  | Infrastructure, Frontend | **High** |
| **Application** | Presentation (APIs)     | Frontend | **Medium** |
| **Infrastructure** | Database Schema      | None (Logic-wise) | **Low** |
| **Presentation** | Frontend (React)       | None (Logic-wise) | **Medium** |

---
Documentation created by Antigravity.
