# Ziyarah Backend - Requirements Analysis Summary

## 1. System Purpose and Scope

**Ziyarah** is a secure, on-premise digital ecosystem designed to streamline bookings across multiple hospitality verticals including Hotels, Resorts, Restaurants, Taxis, and Trips. The platform serves as a centralized hub connecting end customers with service providers while providing comprehensive back-office management capabilities.

### Business Objectives
- **Revenue Generation**: 10% commission on all bookings
- **Customer Autonomy**: Users manage bookings and cancellations via mobile app with automated refund logic
- **System Architecture**: Secure, server-based solution using Apache Kafka for high-volume communications
- **Scalability**: Architecture supporting millions of concurrent users

### Scope
**In-Scope:**
- Multi-service booking platform (Hotels, Resorts, Restaurants, Taxis, Trips)
- **Landing Page**: Dual-purpose web entry point for customers (service discovery/search) and service providers (ecosystem overview and public service view).
- **Client Portal (React SPA)**: Dedicated management dashboard for service providers.
- **Advanced RBAC**: 7-group hierarchy with 50+ granular `resource:action` permissions
- **Multilingual Support**: Full Arabic (AR) support including RTL layout and content
- Multi-currency transaction support
- Automated cancellation and refund processing
- Real-time messaging via Apache Kafka
- Taxi aggregation via third-party API integration

**Out-of-Scope:**
- Cloud-based hosting (strictly on-premise)
- Automated payroll processing (manual calculation only)
- Direct hotel property management system integration

---

## 2. Main Entities and Attributes

### Core Entities

| Entity | Description | Key Attributes |
|--------|-------------|----------------|
| **User** | System users (customers and staff) | id, email, phone, password_hash, role, status, created_at, updated_at, deleted_at |
| **Customer** | End customer profile | user_id, first_name, last_name, id_document_url, preferred_currency |
| **Employee** | Company staff | user_id, department_id, employee_code, level, hire_date |
| **Department** | Organizational units | id, name, code, description |
| **ServiceProvider** | Hotels, restaurants, etc. | id, company_name, contact_email, contact_phone, address, status |
| **Service** | Bookable services | id, type, name, description, location, base_price, status, provider_id, attributes |
| **ServiceImage** | Service images | id, service_id, image_url, caption, display_order |
| **Booking** | Reservation records | id, customer_id, service_id, discount_code_id, check_in, check_out, guests, base_amount, discount_amount, total_amount, status, special_requests |
| **Payment** | Transaction records | id, booking_id, amount, currency, exchange_rate, method, status, transaction_ref, gateway_response |
| **Refund** | Refund records | id, payment_id, booking_id, amount, penalty_amount, reason, status, processed_by |
| **TaxiBooking** | Taxi add-on bookings | id, booking_id, pickup_location, dropoff_location, pickup_time, vehicle_type, fare, external_ref, status |
| **Complaint** | Customer complaints | id, customer_id, booking_id, subject, description, priority, status, assigned_agent_id, resolution_notes |
| **InternalTicket** | Bug reports & feature requests | id, ticket_number, reporter_id, type, subject, description, priority, status, module, assigned_to_id, resolution_notes |
| **TicketComment** | Ticket comments | id, ticket_id, user_id, comment, is_internal, is_resolution |
| **ComplaintComment** | Complaint comments | id, complaint_id, user_id, comment, is_internal |
| **DiscountCode** | Promotion codes | id, code, percentage, max_discount, min_spend, expiry_date, usage_limit, used_count, created_by, approved_by, status |
| **Review** | Service reviews | id, booking_id, customer_id, service_id, rating, comment, status |
| **Notification** | User notifications | id, user_id, type, channel, title, content, metadata, status, sent_at, read_at |
| **AuditLog** | System activity tracking | id, user_id, action, entity_type, entity_id, changes, ip_address, user_agent |
| **Session** | User sessions | id, user_id, token, device_type, device_info, ip_address, expires_at |
| **ExchangeRate** | Currency rates | id, from_currency, to_currency, rate, effective_date |
| **I18nLabel** | Static translations | id, key, en, ar, module |
| **DBMasterPermission** | Table access per role | id, role_id, table_name, can_read, can_write |

### RBAC Entities

| Entity | Description | Key Attributes |
|--------|-------------|----------------|
| **Role** | User roles | id, name, description, level, department |
| **Group** | Organizational groups | id, name, code, description |
| **Permission** | System permissions | id, code, name, resource, action, scope |
| **UserRole** | User-Role assignment | id, user_id, role_id, group_id, assigned_at, expires_at |
| **RolePermission** | Role-Permission mapping | id, role_id, permission_id |

---

## 3. Key Business Rules and Constraints

### Authentication & Authorization
- Email/Phone + OTP authentication for end customers
- RBAC with granular permissions (feature and data level)
- 2FA via OTP for new device logins
- Account lockout after 5 failed login attempts
- Password complexity: min 8 chars, mixed case, numbers, special chars
- Session expires after 30 minutes of inactivity
- Remember Me extends session to 7 days

### Booking Management
- Services: Hotels, Resorts, Restaurants, Taxis, Trips
- ID document upload required during booking
- Multi-currency support with real-time exchange rates
- **Secure Payment**: Integrated Visa/Credit Card support via 3D Secure (3DS) Gateway.
- **Dynamic Commission**: Default 10% commission on all bookings, adjustable per provider by Admin/Managers.
- **Hierarchical Pricing**: Customer price = (Base - Provider_Disc - Company_Disc) * (1 + Commission).
- Payment must be completed within 15 minutes

### Cancellation & Refund
- 100% refund if cancelled before service start
- 95% refund (5% penalty) if cancelled during service period
- No refund after service completion
- Automated refund processing

### Discount Management
- Sales Rep max: 20% discount
- Finance max: 50% discount
- Manager approval required for all compensatory discounts
- Discount codes must be unique
- Percentage between 1-100
- Expiry date must be in future

### Customer Service
- All complaints must have subject and description
- Auto-acknowledgement sent within 60 seconds
- Complaints assigned within 24 hours
- Resolution notes are mandatory

### Multilingual Support (i18n)
- **Primary Languages**: English (EN), Arabic (AR).
- **RTL Support**: Frontend must support Right-to-Left layout for Arabic.
- **Content Storage**: Bookable entities (Hotels, etc.) must have `_ar` columns for names and descriptions.
- **Static Labels**: All UI text must be managed via `i18n_labels` table and frontend translation files.

### Modular State Machines
Workflows like **Discount Approval** live within their respective modules but publish events upon completion.
- `DiscountModule` -> `ApprovalService` -> `InternalEvent` -> `NotificationModule(Listener)`.

### Dynamic Commission & Discount Hierarchy
The `sys_` module provides an interface for Admins/Managers to set the `provider_commission_pct`.
- **Default**: 10.0%
- **Precedence Logic**:
    1.  **Provider Discount**: Deducted from the base price.
    2.  **Company Discount**: Deducted from the remaining price.
    3.  **Commission**: Calculated on the final adjusted base.
- **Formula**: `displayed_price = (base - disc_P - disc_C) * (1 + (commission_%/100))`

---

## 4. Payment Integration (Visa & Adapters)

The `payment` module uses the **Adapter Pattern** to support multiple gateways.
- **Visa Gateway**: Handled via secure tokenization. The monolith never stores raw card data (PCI Compliance).
- **Callbacks**: Secure webhooks from the payment provider update booking status asynchronously.

### Data Security
- SSL/TLS encryption for all data in transit
- Encryption at rest for identity documents
- Audit logging for delete and permission modifications
- Financial data restricted to Finance department and GM
- **Finance Commission restriction**: Commission data is strictly for Super Admin and CEO only.

---

## 4. API Endpoints Outline

### Authentication API
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/auth/login | User login |
| POST | /api/v1/auth/register | User registration |
| POST | /api/v1/auth/otp/send | Send OTP |
| POST | /api/v1/auth/otp/verify | Verify OTP |
| POST | /api/v1/auth/logout | User logout |
| POST | /api/v1/auth/refresh | Refresh token |
| POST | /api/v1/auth/password/forgot | Forgot password |
| POST | /api/v1/auth/password/reset | Reset password |

### User Management API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/users | List users (Admin) |
| POST | /api/v1/users | Create user (Admin/HR) |
| GET | /api/v1/users/{id} | Get user details |
| PUT | /api/v1/users/{id} | Update user |
| DELETE | /api/v1/users/{id} | Delete user (Soft delete) |
| POST | /api/v1/users/{id}/freeze | Freeze user account |
| POST | /api/v1/users/{id}/unfreeze | Unfreeze user account |
| POST | /api/v1/users/{id}/reset-password | Admin reset user password |
| GET | /api/v1/users/{id}/login-history | Get user login history |

### Service Provider API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/providers | List service providers |
| POST | /api/v1/providers | Register new provider |
| GET | /api/v1/providers/{id} | Get provider details |
| PUT | /api/v1/providers/{id} | Update provider |
| DELETE | /api/v1/providers/{id} | Delete provider |
| POST | /api/v1/providers/{id}/approve | Approve provider |
| POST | /api/v1/providers/{id}/suspend | Suspend provider |

### Service API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/services | List services |
| POST | /api/v1/services | Create service |
| GET | /api/v1/services/{id} | Get service details |
| PUT | /api/v1/services/{id} | Update service |
| DELETE | /api/v1/services/{id} | Delete service |
| GET | /api/v1/services/search | Search services |
| GET | /api/v1/services/{id}/availability | Check availability |
| POST | /api/v1/services/{id}/images | Add service image |
| DELETE | /api/v1/services/{id}/images/{imageId} | Remove service image |

### Booking API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/bookings | List bookings |
| POST | /api/v1/bookings | Create booking |
| GET | /api/v1/bookings/{id} | Get booking details |
| PUT | /api/v1/bookings/{id} | Update booking |
| POST | /api/v1/bookings/{id}/cancel | Cancel booking |
| POST | /api/v1/bookings/{id}/confirm | Confirm booking |
| GET | /api/v1/bookings/{id}/voucher | Get booking voucher |
| POST | /api/v1/bookings/{id}/taxi | Add taxi to booking |

### Payment API
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/payments | Process payment |
| GET | /api/v1/payments/{id} | Get payment details |
| POST | /api/v1/payments/{id}/refund | Process refund |
| GET | /api/v1/payments/transaction/{ref} | Get payment by transaction ref |

### Complaint API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/complaints | List complaints |
| POST | /api/v1/complaints | Create complaint |
| GET | /api/v1/complaints/{id} | Get complaint details |
| PUT | /api/v1/complaints/{id} | Update complaint |
| POST | /api/v1/complaints/{id}/assign | Assign complaint |
| POST | /api/v1/complaints/{id}/resolve | Resolve complaint |
| POST | /api/v1/complaints/{id}/escalate | Escalate complaint |
| POST | /api/v1/complaints/{id}/close | Close complaint |
| POST | /api/v1/complaints/{id}/comments | Add comment |

### Internal Ticket API (Bug Reports & Feature Requests)
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/tickets | List internal tickets |
| POST | /api/v1/tickets | Create internal ticket |
| GET | /api/v1/tickets/{id} | Get ticket details |
| PUT | /api/v1/tickets/{id} | Update ticket |
| DELETE | /api/v1/tickets/{id} | Delete ticket |
| POST | /api/v1/tickets/{id}/acknowledge | Acknowledge ticket |
| POST | /api/v1/tickets/{id}/assign | Assign ticket |
| POST | /api/v1/tickets/{id}/start-progress | Start work on ticket |
| POST | /api/v1/tickets/{id}/request-info | Request information |
| POST | /api/v1/tickets/{id}/testing | Move to testing |
| POST | /api/v1/tickets/{id}/resolve | Resolve ticket |
| POST | /api/v1/tickets/{id}/verify | Verify resolution |
| POST | /api/v1/tickets/{id}/close | Close ticket |
| POST | /api/v1/tickets/{id}/reopen | Reopen ticket |
| POST | /api/v1/tickets/{id}/cancel | Cancel ticket |
| GET | /api/v1/tickets/{id}/comments | Get ticket comments |
| POST | /api/v1/tickets/{id}/comments | Add comment to ticket |
| GET | /api/v1/tickets/stats | Get ticket statistics |
| GET | /api/v1/tickets/overdue | Get overdue tickets |

### Discount Code API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/discounts | List discount codes |
| POST | /api/v1/discounts | Create discount code |
| GET | /api/v1/discounts/{id} | Get discount details |
| PUT | /api/v1/discounts/{id} | Update discount |
| DELETE | /api/v1/discounts/{id} | Delete discount |
| POST | /api/v1/discounts/{id}/approve | Approve discount |
| POST | /api/v1/discounts/{id}/deactivate | Deactivate discount |
| POST | /api/v1/discounts/validate | Validate discount code |
| POST | /api/v1/discounts/apply | Apply discount to booking |

### Review API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/reviews | List reviews |
| POST | /api/v1/reviews | Create review |
| GET | /api/v1/reviews/{id} | Get review details |
| PUT | /api/v1/reviews/{id} | Update review |
| DELETE | /api/v1/reviews/{id} | Delete review |
| POST | /api/v1/reviews/{id}/moderate | Moderate review |

### Notification API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/notifications | List notifications |
| GET | /api/v1/notifications/{id} | Get notification |
| POST | /api/v1/notifications/{id}/read | Mark as read |
| POST | /api/v1/notifications/read-all | Mark all as read |

### Exchange Rate API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/exchange-rates | List exchange rates |
| POST | /api/v1/exchange-rates | Create exchange rate |
| PUT | /api/v1/exchange-rates/{id} | Update exchange rate |
| GET | /api/v1/exchange-rates/convert | Convert currency |

### Audit Log API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/audit-logs | List audit logs |
| GET | /api/v1/audit-logs/{id} | Get audit log details |

### Dashboard/Analytics API
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/dashboard/revenue | Revenue statistics |
| GET | /api/v1/dashboard/bookings | Booking statistics |
| GET | /api/v1/dashboard/customers | Customer statistics |
| GET | /api/v1/dashboard/providers | Provider performance |

---

## 5. User Roles and Permissions

### Organizational Groups (G1-G7)

| Group | Roles | Hierarchy Level |
|-------|-------|-----------------|
| G1 - IT | **SUPER_ADMIN** | Super Admin |
| G2 - Sales | **SALES_REPRESENTATIVE, SALES_MANAGER** | Manager/Employee |
| G3 - Finance | **FINANCE_MANAGER, ACCOUNTANT** | Manager/Employee |
| G4 - Support | **SUPPORT_MANAGER, SUPPORT_AGENT** | Manager/Employee |
| G5 - Executive | **CEO, GENERAL_MANAGER** | Executive Oversight |
| G6 - HR | **HR_MANAGER** | Personnel Admin |
| G7 - Providers | **PROVIDER_MANAGER, PROVIDER_FINANCE, PROVIDER_STAFF** | Client Portal Admin |

### Permission Hierarchy (Example)
- `user:read:all`: HR/Admin
- `user:delete:hard`: SUPER_ADMIN only (Non-delegatable)
- `payment:view-commission`: SUPER_ADMIN/CEO only
- `provider:approve`: SUPER_ADMIN only

---

## 6. Technology Stack

| Layer | Technology |
|-------|------------|
| Backend Framework | Spring Boot 3.x |
| Language | Java 17+ |
| Database | PostgreSQL |
| Cache | Redis |
| Message Queue | Apache Kafka |
| Authentication | JWT + Spring Security |
| API Documentation | OpenAPI/Swagger |
| Build Tool | Maven |
| Architecture | Clean Architecture |
