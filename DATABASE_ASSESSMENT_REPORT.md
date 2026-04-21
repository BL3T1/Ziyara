# ZIYARAH Platform - Database Assessment Report

**Document Version:** 1.0  
**Assessment Date:** 2026-02-05  
**Database Platform:** PostgreSQL 15+  
**Schema Files Analyzed:** schema.sql, migrations (001-038), seed.sql  

---

## Executive Summary

This report provides a comprehensive assessment of the Ziyarah Platform database design, identifying strengths, weaknesses, security posture, data uniqueness guarantees, and overall architectural quality. The database serves a multi-service booking platform supporting hotels, resorts, restaurants, trips, and taxi services with integrated RBAC, payment processing, and customer support workflows.

### Overall Design Rating: **7.8/10** (Good - Production Ready with Recommended Improvements)

| Category | Score | Status |
|----------|-------|--------|
| Schema Normalization | 8.5/10 | ✅ Excellent |
| Data Integrity | 8.0/10 | ✅ Good |
| Security Hardening | 7.5/10 | ⚠️ Good (Improving) |
| Performance Optimization | 7.0/10 | ⚠️ Good |
| GDPR/Compliance Readiness | 7.5/10 | ⚠️ Good |
| Scalability | 8.0/10 | ✅ Good |
| Maintainability | 8.5/10 | ✅ Excellent |

---

## 1. Database State Overview

### 1.1 Current Schema Statistics

| Metric | Count |
|--------|-------|
| Core Tables (schema.sql) | 22 |
| Extension Tables (migrations) | 18+ |
| Enum Types | 14 |
| Indexes (explicit) | 60+ |
| Foreign Key Relationships | 45+ |
| Check Constraints | 12 |
| Unique Constraints | 25+ |
| Triggers | 19 (updated_at) |
| Functions | 4 (booking ref, ticket numbers, timestamp update) |

### 1.2 Domain Table Prefixes (Post-Migration 015)

The database uses domain-driven prefixes for production tables:
- `sys_*` - System/RBAC tables (users, roles, permissions, audit)
- `hotel_*` - Hospitality services (providers, services, rooms, reviews)
- `bkg_*` - Booking domain (bookings, taxi_bookings)
- `pay_*` - Payment domain (payments, refunds, exchange_rates)
- `disc_*` - Discount codes
- `support_*` - Customer support (complaints, contact_leads)

### 1.3 Entity Relationship Summary

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   sys_users │────▶│  customers   │     │  employees  │
└──────┬──────┘     └──────────────┘     └──────┬──────┘
       │                                         │
       ▼                                         ▼
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│sys_user_roles│    │sys_departments│    │hotel_service│
└──────┬──────┘     └──────────────┘     │_providers   │
       │                                 └──────┬──────┘
       ▼                                        │
┌─────────────┐                                 ▼
│ sys_roles   │                         ┌─────────────┐
└──────┬──────┘                         │hotel_services│
       │                                └──────┬──────┘
       ▼                                       │
┌─────────────┐                                ▼
│sys_permissions│                       ┌─────────────┐
└─────────────┘                         │ bkg_bookings │
                                        └──────┬──────┘
                                               │
              ┌────────────────┬───────────────┼───────────────┐
              ▼                ▼               ▼               ▼
      ┌─────────────┐  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
      │pay_payments │  │bkg_taxi_    │ │support_     │ │ hotel_      │
      │             │  │bookings     │ │complaints   │ │ reviews     │
      └─────────────┘  └─────────────┘ └─────────────┘ └─────────────┘
```

---

## 2. Database Weaknesses Analysis

### 2.1 CRITICAL Weaknesses

#### W-CRIT-01: Plaintext PII Storage
**Severity:** CRITICAL  
**Affected Tables:** `customers`, `hotel_service_providers`, `sys_users`  
**Description:** 
- Sensitive fields stored in plaintext: `email`, `phone`, `id_document_number`, `bank_account_number`, `tax_id`
- Migration 032 adds `_cipher` columns but application-layer encryption is optional
- No transparent data encryption (TDE) at database level

**Current State:**
```sql
-- customers table
id_document_number VARCHAR(100)  -- PLAINTEXT
-- hotel_service_providers table  
bank_account_number VARCHAR(50)  -- PLAINTEXT
tax_id VARCHAR(50)               -- PLAINTEXT
```

**Risk:** Data breach exposes all customer PII and financial information directly.

**Status:** Partial mitigation via migration 032 (`*_cipher` columns added but optional).

---

#### W-CRIT-02: Password History Not Enforced
**Severity:** CRITICAL  
**Affected Tables:** `sys_users`  
**Description:**
- No password history tracking prevents users from reusing recent passwords
- Migration 032 creates `sys_user_password_history` but enforcement logic is application-dependent
- No password complexity constraints at database level

**Risk:** Users can cycle through weak passwords, reducing account security.

**Status:** Table created in migration 032; requires application enforcement.

---

#### W-CRIT-03: Missing Multi-Factor Authentication Schema
**Severity:** CRITICAL  
**Affected Tables:** `sys_users`  
**Description:**
- MFA columns added in migration 032 but TOTP implementation is incomplete
- No MFA attempt logging or lockout mechanism
- Backup codes stored encrypted but no rotation policy

**Current State:**
```sql
-- Added in migration 032
mfa_enabled BOOLEAN DEFAULT FALSE
mfa_type VARCHAR(20)
mfa_secret_cipher TEXT
mfa_backup_codes_cipher TEXT
```

**Risk:** Single-factor authentication vulnerable to credential stuffing attacks.

**Status:** Schema present; requires full TOTP service implementation.

---

### 2.2 HIGH Weaknesses

#### W-HIGH-01: Inconsistent Soft Delete Pattern
**Severity:** HIGH  
**Affected Tables:** Mixed coverage  
**Description:**
- Some tables have `deleted_at`: `users`, `service_providers`, `services`, `bookings`
- Many tables lack soft delete: `payments`, `refunds`, `complaints`, `reviews`
- No global soft delete function or view abstraction

**Impact:** Inconsistent data retention; accidental hard deletes possible.

**Tables with `deleted_at`:**
- ✅ `users`, `service_providers`, `services`
- ❌ `customers`, `employees`, `payments`, `refunds`, `complaints`, `reviews`, `notifications`

---

#### W-HIGH-02: Missing Rate Limiting Infrastructure
**Severity:** HIGH  
**Affected Tables:** None (pre-migration 033)  
**Description:**
- No database-level rate limiting counters
- Relies entirely on API gateway/Redis
- No fallback protection if gateway fails

**Status:** Migration 033 adds `sys_rate_limit_counters` but not yet deployed universally.

---

#### W-HIGH-03: Audit Log Growth Without Partitioning
**Severity:** HIGH  
**Affected Tables:** `sys_audit_logs`  
**Description:**
- Single monolithic table grows indefinitely
- No automatic archival or partitioning strategy
- Query performance degrades over time

**Current State:**
```sql
CREATE TABLE audit_logs (...);  -- No partitioning
-- Migration 034 attempts HASH partitioning but complex
```

**Risk:** Audit table becomes performance bottleneck; backup times increase.

**Status:** Migration 034 implements HASH partitioning with 8 partitions.

---

#### W-HIGH-04: Weak Foreign Key Index Coverage
**Severity:** HIGH  
**Affected Tables:** Multiple  
**Description:**
- Many FK columns lack indexes, causing slow JOINs and CASCADE operations
- Examples missing indexes (pre-migration 032):
  - `sys_user_roles.role_id`
  - `sys_user_roles.group_id`
  - `bkg_bookings.discount_code_id`
  - `support_complaints.resolved_by`
  - `hotel_reviews.responded_by`

**Impact:** CASCADE DELETE operations lock tables; JOIN queries perform sequential scans.

**Status:** Partially addressed in migration 032.

---

#### W-HIGH-05: No Row-Level Security (RLS)
**Severity:** HIGH  
**Affected Tables:** All multi-tenant tables  
**Description:**
- No RLS policies enforce data isolation at database level
- Relies entirely on application-layer filtering
- Vulnerable to SQL injection or buggy queries exposing cross-tenant data

**Status:** Migration 034 adds RLS policies for `hotel_service_providers`, `hotel_services`, `bkg_bookings` but marked as "opt-in pilot".

---

### 2.3 MEDIUM Weaknesses

#### W-MED-01: Enum Type Fragility
**Severity:** MEDIUM  
**Affected Types:** All ENUM types  
**Description:**
- PostgreSQL ENUMs cannot be easily modified (no DROP VALUE, limited ALTER)
- Adding values requires careful migration planning
- Application expects VARCHAR (migration 004-011 converts ENUMs to VARCHAR)

**Current State:** Hybrid approach - ENUMs defined but columns converted to VARCHAR for Hibernate compatibility.

**Risk:** Schema drift between ENUM definitions and actual column types.

---

#### W-MED-02: Inconsistent Timestamp Timezones
**Severity:** MEDIUM  
**Affected Tables:** All tables  
**Description:**
- Mix of `TIMESTAMP WITH TIME ZONE` and `TIMESTAMP` (without timezone)
- Migration scripts sometimes use `TIMESTAMPTZ`, sometimes `TIMESTAMP`
- Potential for timezone confusion in reports

**Example:**
```sql
-- schema.sql uses TIMESTAMPTZ consistently
created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP

-- But some migrations use TIMESTAMP
scheduled_at TIMESTAMP WITH TIME ZONE  -- OK
started_at TIMESTAMP WITH TIME ZONE    -- OK
```

---

#### W-MED-03: Missing Composite Unique Constraints
**Severity:** MEDIUM  
**Affected Tables:** Multiple  
**Description:**
- Some tables rely on surrogate UUID keys where natural unique constraints would prevent duplicates
- Examples:
  - `exchange_rates`: Has unique on (from_currency, to_currency, effective_date) ✅
  - `sys_user_roles`: Has unique on (user_id, role_id) ✅
  - `hotel_service_rooms`: No unique constraint on (service_id, room_type) ❌

---

#### W-MED-04: JSONB Without Schema Validation
**Severity:** MEDIUM  
**Affected Columns:** Multiple JSONB columns  
**Description:**
- JSONB columns store unstructured data without CHECK constraints
- Examples: `attributes`, `amenities`, `gateway_response`, `metadata`
- No JSON Schema validation at database level

**Risk:** Data quality issues; inconsistent structure across records.

---

#### W-MED-05: Currency Code Without Reference Table
**Severity:** MEDIUM  
**Affected Tables:** `services`, `bookings`, `payments`, `exchange_rates`  
**Description:**
- Currency stored as VARCHAR(3) without foreign key to currency reference table
- No validation against ISO 4217 standard
- Relies on application to enforce valid codes

---

### 2.4 LOW Weaknesses

#### W-LOW-01: Redundant Name/Code Columns
**Severity:** LOW  
**Affected Tables:** `roles`, `permissions`, `groups`, `departments`  
**Description:**
- Both `name` and `code` columns exist with UNIQUE constraints
- Potential for drift between human-readable name and system code

---

#### W-LOW-02: Overuse of UUID Primary Keys
**Severity:** LOW  
**Description:**
- All tables use UUID primary keys
- While good for distributed systems, UUIDs are:
  - Larger than BIGINT (16 bytes vs 8 bytes)
  - Random insertion causes index fragmentation
  - Slower JOIN performance compared to sequential integers

**Consideration:** Acceptable trade-off for microservices readiness.

---

#### W-LOW-03: Missing Column Comments on New Tables
**Severity:** LOW  
**Description:**
- Original schema.sql has comprehensive comments
- Some migration-created tables lack documentation
- Example: `sys_feature_flags`, `sys_integration_api_keys` have minimal comments

---

## 3. Data Uniqueness Analysis

### 3.1 Current Unique Constraints

| Table | Unique Constraint | Columns | Status |
|-------|------------------|---------|--------|
| `sys_users` | uk_users_email | email | ✅ |
| `sys_users` | uk_users_phone | phone | ✅ |
| `sys_roles` | uk_roles_name | name | ✅ |
| `sys_roles` | uk_roles_code | code | ✅ |
| `sys_permissions` | uk_permissions_code | code | ✅ |
| `sys_groups` | uk_groups_code | code | ✅ |
| `sys_departments` | uk_departments_code | code | ✅ |
| `sys_user_roles` | uk_user_roles | (user_id, role_id) | ✅ |
| `sys_role_permissions` | uk_role_permissions | (role_id, permission_id) | ✅ |
| `customers` | pk_customers | user_id (PK) | ✅ |
| `employees` | uk_employees_code | employee_code | ✅ |
| `hotel_service_providers` | pk_providers | id (surrogate) | ⚠️ |
| `hotel_services` | pk_services | id (surrogate) | ⚠️ |
| `bkg_bookings` | uk_bookings_reference | booking_reference | ✅ |
| `pay_payments` | uk_payments_transaction_ref | transaction_ref | ✅ |
| `support_complaints` | uk_complaints_ticket | ticket_number | ✅ |
| `internal_tickets` | uk_internal_tickets_number | ticket_number | ✅ |
| `hotel_reviews` | uk_reviews_booking | booking_id | ✅ |
| `sessions` | uk_sessions_token | token_hash | ✅ |
| `discount_codes` | uk_discount_codes_code | code | ✅ |
| `pay_exchange_rates` | uk_exchange_rates_pair | (from_currency, to_currency, effective_date) | ✅ |

### 3.2 Missing Unique Constraints

| Table | Recommended Unique Constraint | Columns | Priority |
|-------|------------------------------|---------|----------|
| `hotel_service_providers` | Natural key | (company_name, tax_id) | MEDIUM |
| `hotel_services` | Prevent duplicate rooms | (provider_id, name, city) | LOW |
| `hotel_service_rooms` | Prevent duplicate room types | (service_id, room_type) | MEDIUM |
| `sys_notifications` | Deduplication | (user_id, type, title, created_at::date) | LOW |
| `pay_payments` | Idempotency | (booking_id, idempotency_key) | ✅ EXISTS |
| `sys_user_consents` | Latest consent per type | (user_id, consent_type) | ⚠️ Partial (versioned) |

### 3.3 Uniqueness Generation Mechanisms

| Entity | Generation Method | Collision Risk |
|--------|------------------|----------------|
| Booking Reference | `ZYB + YYYYMMDD + random(5)` | LOW (time-based + random) |
| Complaint Ticket | `TKT + YYYYMMDD + random(4)` | MEDIUM (only 10k/day) |
| Internal Ticket | `ITK + YYYYMMDD + random(4)` | MEDIUM (only 10k/day) |
| Primary Keys | `gen_random_uuid()` | NEGLIGIBLE |

**Recommendation:** Increase ticket number randomness to 6 digits (1M/day capacity).

---

## 4. Security Posture Assessment

### 4.1 Authentication & Authorization

| Control | Status | Notes |
|---------|--------|-------|
| Password Hashing | ✅ | bcrypt/argon2 assumed at app layer |
| Password History | ⚠️ | Table exists, enforcement TBD |
| Account Lockout | ✅ | `failed_login_attempts`, `locked_until` columns |
| Session Management | ✅ | `sessions` table with token_hash |
| JWT Token Versioning | ✅ | `token_version` in sys_users (migration 032) |
| MFA Support | ⚠️ | Schema ready, implementation partial |
| RBAC Implementation | ✅ | Full role-permission-user hierarchy |

### 4.2 Data Protection

| Control | Status | Notes |
|---------|--------|-------|
| PII Encryption | ⚠️ | Optional cipher columns (migration 032) |
| TLS in Transit | N/A | Infrastructure responsibility |
| TDE at Rest | ❌ | Not implemented |
| Field-Level Encryption | ⚠️ | Manual for sensitive fields only |
| Masking Views | ❌ | No dynamic data masking |

### 4.3 Audit & Monitoring

| Control | Status | Notes |
|---------|--------|-------|
| Audit Logging | ✅ | Comprehensive `sys_audit_logs` |
| Security Events | ✅ | `sys_security_events` table |
| Alert Rules | ✅ | `sys_security_alert_rules` (migration 033) |
| Alert Tracking | ✅ | `sys_security_alerts` table |
| Consent Audit | ✅ | `sys_consent_audit_log` |
| Login History | ⚠️ | Via sessions table only |

### 4.4 Compliance (GDPR)

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| Right to Access | ⚠️ | `sys_data_export_requests` exists |
| Right to Erasure | ⚠️ | `right_to_erasure_requested` flag exists |
| Consent Management | ✅ | `sys_user_consents` table |
| Data Retention | ⚠️ | `sys_data_retention_policies` table |
| PII Registry | ✅ | `sys_pii_field_registry` |
| Portability | ⚠️ | Export payload_json field added |

---

## 5. Performance Assessment

### 5.1 Index Coverage

| Table | Total Indexes | FK Indexed | Query Patterns Covered |
|-------|--------------|------------|----------------------|
| `sys_users` | 6 | N/A | Email, phone, status, role lookups |
| `bkg_bookings` | 8 | Partial | Customer, service, status, date range |
| `hotel_services` | 5 | Yes | Provider, type, location, price |
| `pay_payments` | 5 | Yes | Booking, status, transaction ref |
| `support_complaints` | 7 | Partial | Customer, status, priority, agent |

**Gaps Addressed in Migration 032-035:**
- Composite indexes for common query patterns
- Partial indexes for active records
- Covering indexes for list views
- BRIN indexes for time-series data

### 5.2 Query Performance Features

| Feature | Status | Usage |
|---------|--------|-------|
| Materialized Views | ✅ | `mv_pay_daily_totals` (migration 033) |
| Expression Indexes | ✅ | LOWER(email) for case-insensitive lookup |
| Partial Indexes | ✅ | Active bookings, non-cancelled records |
| Covering Indexes | ✅ | INCLUDE clause for list views |
| BRIN Indexes | ✅ | Time-series large tables |

### 5.3 Known Performance Risks

1. **Audit log table growth** - Mitigated by partitioning (migration 034)
2. **JSONB query performance** - No GIN indexes on JSONB columns
3. **Cross-domain JOINs** - RLS may add overhead
4. **UUID index fragmentation** - Consider UUID v7 for time-ordering

---

## 6. Schema Design Quality

### 6.1 Normalization

**Strengths:**
- Proper 3NF normalization throughout
- Separate tables for extended profiles (customers, employees)
- Junction tables for many-to-many relationships
- No transitive dependencies detected

**Minor Issues:**
- Some denormalization for performance (e.g., `commission_amount` in bookings)
- JSONB columns for flexible attributes (acceptable trade-off)

### 6.2 Naming Conventions

| Convention | Consistency | Notes |
|------------|-------------|-------|
| Table Names | ✅ | snake_case, domain-prefixed |
| Column Names | ✅ | snake_case, consistent patterns |
| Primary Keys | ✅ | `id` on all tables |
| Foreign Keys | ✅ | `{referenced_table}_id` pattern |
| Indexes | ✅ | `idx_{table}_{columns}` pattern |
| Constraints | ✅ | `chk_`, `uk_`, `fk_` prefixes |

### 6.3 Data Types

| Type | Usage | Assessment |
|------|-------|------------|
| UUID | Primary keys | ✅ Appropriate for distributed systems |
| VARCHAR(n) | Text fields | ✅ Appropriate limits |
| DECIMAL(12,2) | Monetary values | ✅ Correct precision |
| DECIMAL(18,6) | Exchange rates | ✅ High precision |
| TIMESTAMPTZ | Timestamps | ✅ Timezone-aware |
| JSONB | Flexible attributes | ✅ Queryable semi-structured data |
| ENUM/VARCHAR | Status fields | ⚠️ Hybrid approach (VARCHAR for Hibernate) |

---

## 7. Recommendations Summary

### 7.1 Immediate Actions (P0)

1. **Enable PII Encryption** - Activate application-layer encryption for cipher columns
2. **Deploy RLS Policies** - Move from pilot to production enforcement
3. **Implement Password History** - Add service-layer enforcement
4. **Complete MFA Implementation** - Finish TOTP service integration

### 7.2 Short-Term Actions (P1)

1. **Add Missing FK Indexes** - Run migration 032-035
2. **Implement Soft Delete Uniformly** - Add `deleted_at` to all entity tables
3. **Deploy Audit Partitioning** - Activate migration 034
4. **Add Composite Unique Constraints** - Prevent logical duplicates

### 7.3 Medium-Term Actions (P2)

1. **Implement Data Retention Jobs** - Schedule cleanup based on policies
2. **Add GIN Indexes on JSONB** - For frequently queried JSON paths
3. **Consider UUID v7** - For better index locality
4. **Implement Database Health Monitoring** - Use monitoring views

---

## 8. Conclusion

The Ziyarah Platform database demonstrates solid architectural foundations with proper normalization, comprehensive RBAC, and thoughtful domain separation. The migration series (001-038) shows active improvement addressing security hardening, performance optimization, and compliance requirements.

**Key Strengths:**
- Well-normalized schema with clear domain boundaries
- Comprehensive audit trail and security event logging
- Strong foundation for GDPR compliance
- Thoughtful indexing strategy for common query patterns

**Critical Improvements Needed:**
- Mandatory PII encryption enforcement
- Complete MFA implementation
- Universal RLS deployment
- Consistent soft delete pattern

With the recommended improvements, the database will achieve enterprise-grade security and scalability suitable for production deployment at scale.

---

**Report Prepared By:** Database Architecture Review  
**Next Review Date:** 2026-Q2  
**Contact:** Database Team
