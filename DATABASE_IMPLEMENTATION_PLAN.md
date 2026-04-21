# ZIYARAH Platform - Database Implementation Plan

**Document Version:** 1.0  
**Date:** 2026-02-05  
**Based on:** DATABASE_ASSESSMENT_REPORT.md  
**Target Environment:** PostgreSQL 15+  

---

## Executive Summary

This implementation plan addresses the weaknesses identified in the database assessment report through a phased approach prioritizing security, data integrity, and performance. The plan includes SQL migrations, application code changes, and operational procedures.

### Implementation Phases

| Phase | Focus Area | Duration | Risk Level |
|-------|-----------|----------|------------|
| Phase 0 | Preparation & Backup | 1 week | LOW |
| Phase 1 | Critical Security (P0) | 2-3 weeks | HIGH |
| Phase 2 | Data Integrity (P1) | 2 weeks | MEDIUM |
| Phase 3 | Performance Optimization | 2 weeks | LOW |
| Phase 4 | Compliance & Monitoring | 2 weeks | LOW |

**Total Estimated Timeline:** 9-10 weeks

---

## Phase 0: Preparation & Backup

### 0.1 Pre-Implementation Checklist

- [ ] Full database backup completed and verified
- [ ] Point-in-time recovery (PITR) configured
- [ ] Staging environment synchronized with production
- [ ] Rollback procedures documented and tested
- [ ] Maintenance window scheduled (if required)
- [ ] Team briefed on changes and rollback procedures

### 0.2 Backup Verification Script

```sql
-- Verify backup integrity before starting
SELECT 
    pg_size_pretty(pg_database_size(current_database())) AS database_size,
    current_setting('server_version') AS postgres_version,
    current_setting('data_directory') AS data_directory;

-- Check replication status if applicable
SELECT * FROM pg_stat_replication;
```

---

## Phase 1: Critical Security Fixes (P0)

### 1.1 Mandatory PII Encryption

**Objective:** Encrypt all sensitive PII fields at application layer with database storing only ciphertext.

#### Migration: 040_mandatory_pii_encryption.sql

```sql
-- ============================================================================
-- Migration 040: Mandatory PII Encryption Enforcement
-- Prerequisites: Application encryption service deployed
-- Rollback: 040_rollback_pii_encryption.sql
-- ============================================================================

-- Add NOT NULL constraints to cipher columns (after backfill)
ALTER TABLE customers 
    ALTER COLUMN id_document_number_cipher SET NOT NULL;

ALTER TABLE hotel_service_providers 
    ALTER COLUMN bank_account_number_cipher SET NOT NULL,
    ALTER COLUMN tax_id_cipher SET NOT NULL;

-- Add encryption version tracking
ALTER TABLE sys_users 
    ADD COLUMN IF NOT EXISTS pii_encryption_version SMALLINT NOT NULL DEFAULT 0;

-- Create index for encryption audit
CREATE INDEX IF NOT EXISTS idx_customers_pii_version 
    ON customers(pii_encryption_version);

CREATE INDEX IF NOT EXISTS idx_providers_pii_version 
    ON hotel_service_providers(pii_encryption_version);

-- Add trigger to prevent plaintext PII insertion
CREATE OR REPLACE FUNCTION enforce_pii_encryption()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if plaintext PII is being inserted/updated
    IF TG_TABLE_NAME = 'customers' THEN
        IF NEW.id_document_number IS NOT NULL 
           AND NEW.id_document_number_cipher IS NULL THEN
            RAISE EXCEPTION 'PII encryption required: id_document_number must be encrypted';
        END IF;
    ELSIF TG_TABLE_NAME = 'hotel_service_providers' THEN
        IF (NEW.bank_account_number IS NOT NULL 
            AND NEW.bank_account_number_cipher IS NULL)
           OR (NEW.tax_id IS NOT NULL 
            AND NEW.tax_id_cipher IS NULL) THEN
            RAISE EXCEPTION 'PII encryption required: financial/tax data must be encrypted';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers
DROP TRIGGER IF EXISTS trg_enforce_customer_pii_encryption ON customers;
CREATE TRIGGER trg_enforce_customer_pii_encryption
    BEFORE INSERT OR UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION enforce_pii_encryption();

DROP TRIGGER IF EXISTS trg_enforce_provider_pii_encryption ON hotel_service_providers;
CREATE TRIGGER trg_enforce_provider_pii_encryption
    BEFORE INSERT OR UPDATE ON hotel_service_providers
    FOR EACH ROW EXECUTE FUNCTION enforce_pii_encryption();

COMMENT ON FUNCTION enforce_pii_encryption() IS 
    'Prevents insertion of unencrypted PII data';
```

#### Application Changes Required

```java
// PiiEncryptionService.java
@Service
public class PiiEncryptionService {
    
    @Value("${ziyara.pii.encryption-key-base64}")
    private String encryptionKeyBase64;
    
    public String encrypt(String plaintext) {
        // Implement AES-256-GCM encryption
        // Return base64-encoded ciphertext
    }
    
    public String decrypt(String ciphertext) {
        // Decrypt and return plaintext
        // Log access for audit
    }
}

// CustomerJpaEntity.java - Add @PrePersist and @PreUpdate hooks
@PrePersist
@PreUpdate
private void encryptPiiFields() {
    if (idDocumentNumber != null && idDocumentNumberCipher == null) {
        idDocumentNumberCipher = piiEncryptionService.encrypt(idDocumentNumber);
        piiEncryptionVersion++;
    }
}
```

#### Rollback Procedure

```sql
-- 040_rollback_pii_encryption.sql
DROP TRIGGER IF EXISTS trg_enforce_customer_pii_encryption ON customers;
DROP TRIGGER IF EXISTS trg_enforce_provider_pii_encryption ON hotel_service_providers;
DROP FUNCTION IF EXISTS enforce_pii_encryption();

ALTER TABLE customers 
    ALTER COLUMN id_document_number_cipher DROP NOT NULL;

ALTER TABLE hotel_service_providers 
    ALTER COLUMN bank_account_number_cipher DROP NOT NULL,
    ALTER COLUMN tax_id_cipher DROP NOT NULL;
```

---

### 1.2 Password History Enforcement

**Objective:** Prevent users from reusing their last 5 passwords.

#### Migration: 041_password_history_enforcement.sql

```sql
-- ============================================================================
-- Migration 041: Password History Enforcement Support
-- Enforces minimum password history and age
-- ============================================================================

-- Ensure password history table exists (from migration 032)
CREATE TABLE IF NOT EXISTS sys_user_password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_user_password_history_user 
    ON sys_user_password_history (user_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_password_history_created 
    ON sys_user_password_history (created_at DESC);

-- Function to check password history
CREATE OR REPLACE FUNCTION check_password_history(
    p_user_id UUID,
    p_new_password_hash VARCHAR(255),
    p_history_count INTEGER DEFAULT 5
)
RETURNS BOOLEAN AS $$
DECLARE
    v_recent_hashes TEXT[];
BEGIN
    -- Get last N password hashes
    SELECT ARRAY_AGG(password_hash ORDER BY created_at DESC)
    INTO v_recent_hashes
    FROM sys_user_password_history
    WHERE user_id = p_user_id
    LIMIT p_history_count;
    
    -- Check if new hash matches any recent
    IF v_recent_hashes IS NOT NULL AND p_new_password_hash = ANY(v_recent_hashes) THEN
        RETURN FALSE; -- Password reused
    END IF;
    
    RETURN TRUE; -- Password is unique
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to archive old password
CREATE OR REPLACE FUNCTION archive_password_and_set_new(
    p_user_id UUID,
    p_new_password_hash VARCHAR(255)
)
RETURNS VOID AS $$
DECLARE
    v_old_hash VARCHAR(255);
BEGIN
    -- Get current password
    SELECT password_hash INTO v_old_hash
    FROM sys_users
    WHERE id = p_user_id;
    
    -- Archive old password
    IF v_old_hash IS NOT NULL THEN
        INSERT INTO sys_user_password_history (user_id, password_hash)
        VALUES (p_user_id, v_old_hash);
    END IF;
    
    -- Update password and metadata
    UPDATE sys_users
    SET 
        password_hash = p_new_password_hash,
        last_password_change = CURRENT_TIMESTAMP,
        password_expires_at = CURRENT_TIMESTAMP + INTERVAL '90 days',
        token_version = token_version + 1  -- Invalidate existing sessions
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION check_password_history(UUID, VARCHAR, INTEGER) IS 
    'Validates new password against user''s password history';

COMMENT ON FUNCTION archive_password_and_set_new(UUID, VARCHAR) IS 
    'Archives old password and sets new one with proper metadata updates';
```

#### Application Service Implementation

```java
@Service
public class PasswordManagementService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        // Validate old password
        String currentHash = getCurrentPasswordHash(userId);
        if (!passwordEncoder.matches(oldPassword, currentHash)) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        
        String newHash = passwordEncoder.encode(newPassword);
        
        // Check password history via database function
        boolean isUnique = jdbcTemplate.queryForObject(
            "SELECT check_password_history(?, ?, 5)",
            Boolean.class,
            userId,
            newHash
        );
        
        if (!isUnique) {
            throw new PasswordReuseException(
                "Cannot reuse any of your last 5 passwords"
            );
        }
        
        // Archive and set new password
        jdbcTemplate.update(
            "SELECT archive_password_and_set_new(?, ?)",
            userId,
            newHash
        );
    }
}
```

---

### 1.3 Complete MFA Implementation

**Objective:** Implement full TOTP-based multi-factor authentication.

#### Migration: 042_mfa_complete_implementation.sql

```sql
-- ============================================================================
-- Migration 042: Complete MFA Implementation
-- Adds MFA attempt logging, lockout, and backup code rotation
-- ============================================================================

-- Enhance sys_users MFA columns (from migration 032)
ALTER TABLE sys_users 
    ADD COLUMN IF NOT EXISTS mfa_failed_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS mfa_locked_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS mfa_backup_code_rotation_date DATE;

-- Create MFA attempt log table
CREATE TABLE IF NOT EXISTS sys_mfa_attempt_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES sys_users(id) ON DELETE CASCADE,
    attempt_type VARCHAR(50) NOT NULL, -- 'TOTP', 'BACKUP_CODE', 'RECOVERY'
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    failure_reason VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_mfa_attempt_logs_user 
    ON sys_mfa_attempt_logs(user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_sys_mfa_attempt_logs_ip 
    ON sys_mfa_attempt_logs(ip_address, created_at DESC);

-- Function to check MFA lockout status
CREATE OR REPLACE FUNCTION check_mfa_lockout(p_user_id UUID)
RETURNS TABLE(is_locked BOOLEAN, lockout_reason VARCHAR(100), unlock_at TIMESTAMPTZ) AS $$
DECLARE
    v_user_record RECORD;
    v_recent_failures INTEGER;
BEGIN
    SELECT * INTO v_user_record
    FROM sys_users
    WHERE id = p_user_id;
    
    -- Check time-based lockout
    IF v_user_record.mfa_locked_until > CURRENT_TIMESTAMP THEN
        RETURN QUERY SELECT 
            TRUE::BOOLEAN,
            'Temporary lockout due to failed attempts'::VARCHAR(100),
            v_user_record.mfa_locked_until;
        RETURN;
    END IF;
    
    -- Reset lockout if expired
    IF v_user_record.mfa_locked_until IS NOT NULL 
       AND v_user_record.mfa_locked_until <= CURRENT_TIMESTAMP THEN
        UPDATE sys_users
        SET mfa_locked_until = NULL,
            mfa_failed_attempts = 0
        WHERE id = p_user_id;
    END IF;
    
    -- Count recent failures (last 30 minutes)
    SELECT COUNT(*) INTO v_recent_failures
    FROM sys_mfa_attempt_logs
    WHERE user_id = p_user_id
      AND success = FALSE
      AND created_at > CURRENT_TIMESTAMP - INTERVAL '30 minutes';
    
    IF v_recent_failures >= 5 THEN
        -- Lock account for 30 minutes
        UPDATE sys_users
        SET mfa_locked_until = CURRENT_TIMESTAMP + INTERVAL '30 minutes',
            mfa_failed_attempts = v_recent_failures
        WHERE id = p_user_id;
        
        RETURN QUERY SELECT 
            TRUE::BOOLEAN,
            'Too many failed MFA attempts'::VARCHAR(100),
            (CURRENT_TIMESTAMP + INTERVAL '30 minutes');
        RETURN;
    END IF;
    
    -- Not locked
    RETURN QUERY SELECT 
        FALSE::BOOLEAN,
        NULL::VARCHAR(100),
        NULL::TIMESTAMPTZ;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to log MFA attempt
CREATE OR REPLACE FUNCTION log_mfa_attempt(
    p_user_id UUID,
    p_attempt_type VARCHAR(50),
    p_success BOOLEAN,
    p_ip_address VARCHAR(45),
    p_user_agent TEXT,
    p_failure_reason VARCHAR(100) DEFAULT NULL
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO sys_mfa_attempt_logs 
        (user_id, attempt_type, success, ip_address, user_agent, failure_reason)
    VALUES 
        (p_user_id, p_attempt_type, p_success, p_ip_address, p_user_agent, p_failure_reason);
    
    -- Increment failure counter on failure
    IF NOT p_success THEN
        UPDATE sys_users
        SET mfa_failed_attempts = mfa_failed_attempts + 1
        WHERE id = p_user_id;
    ELSE
        -- Reset on success
        UPDATE sys_users
        SET mfa_failed_attempts = 0,
            mfa_locked_until = NULL
        WHERE id = p_user_id;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to rotate backup codes
CREATE OR REPLACE FUNCTION rotate_mfa_backup_codes(
    p_user_id UUID,
    p_encrypted_backup_codes TEXT
)
RETURNS VOID AS $$
BEGIN
    UPDATE sys_users
    SET 
        mfa_backup_codes_cipher = p_encrypted_backup_codes,
        mfa_backup_code_rotation_date = CURRENT_DATE
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION check_mfa_lockout(UUID) IS 
    'Checks if user MFA is locked and returns lockout details';

COMMENT ON FUNCTION log_mfa_attempt(UUID, VARCHAR, BOOLEAN, VARCHAR, TEXT, VARCHAR) IS 
    'Logs MFA attempt and updates failure counters';
```

---

### 1.4 Deploy Row-Level Security (RLS)

**Objective:** Enforce data isolation at database level for multi-tenant safety.

#### Migration: 043_rls_production_deployment.sql

```sql
-- ============================================================================
-- Migration 043: Production RLS Deployment
-- Enables RLS on all multi-tenant tables with comprehensive policies
-- Requires: app.rls_bypass, app.current_user_id, app.current_provider_id GUCs
-- ============================================================================

-- Enable RLS on all relevant tables
ALTER TABLE hotel_service_providers ENABLE ROW LEVEL SECURITY;
ALTER TABLE hotel_services ENABLE ROW LEVEL SECURITY;
ALTER TABLE bkg_bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE hotel_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE support_complaints ENABLE ROW LEVEL SECURITY;
ALTER TABLE pay_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE disc_discount_codes ENABLE ROW LEVEL SECURITY;

-- Force RLS even for table owners (extra safety)
ALTER TABLE hotel_service_providers FORCE ROW LEVEL SECURITY;
ALTER TABLE hotel_services FORCE ROW LEVEL SECURITY;
ALTER TABLE bkg_bookings FORCE ROW LEVEL SECURITY;

-- Drop existing pilot policies
DROP POLICY IF EXISTS hotel_service_providers_rls ON hotel_service_providers;
DROP POLICY IF EXISTS hotel_services_rls ON hotel_services;
DROP POLICY IF EXISTS bkg_bookings_rls ON bkg_bookings;

-- ============================================================================
-- Hotel Service Providers Policy
-- Users can see providers they: own, manage, or are staff of
-- ============================================================================
CREATE POLICY hotel_service_providers_rls ON hotel_service_providers
    FOR ALL
    TO PUBLIC
    USING (
        -- Super admin bypass
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        
        -- Provider owner
        OR created_by::text = NULLIF(current_setting('app.current_user_id', true), '')
        
        -- Staff member
        OR EXISTS (
            SELECT 1 FROM hotel_provider_staff ps
            WHERE ps.provider_id = hotel_service_providers.id
              AND ps.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        )
        
        -- Platform admin roles (check via user_roles)
        OR EXISTS (
            SELECT 1 FROM sys_user_roles ur
            JOIN sys_roles r ON r.id = ur.role_id
            WHERE ur.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
              AND r.code IN ('SUPER_ADMIN', 'PROVIDER_MANAGER')
        )
    )
    WITH CHECK (
        -- Same conditions for INSERT/UPDATE
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR created_by::text = NULLIF(current_setting('app.current_user_id', true), '')
    );

-- ============================================================================
-- Hotel Services Policy
-- Users can see services from providers they have access to
-- ============================================================================
CREATE POLICY hotel_services_rls ON hotel_services
    FOR ALL
    TO PUBLIC
    USING (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR provider_id::text = NULLIF(current_setting('app.current_provider_id', true), '')
        OR EXISTS (
            SELECT 1 FROM hotel_provider_staff ps
            WHERE ps.provider_id = hotel_services.provider_id
              AND ps.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        )
        OR EXISTS (
            SELECT 1 FROM sys_user_roles ur
            JOIN sys_roles r ON r.id = ur.role_id
            WHERE ur.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
              AND r.code IN ('SUPER_ADMIN', 'PROVIDER_MANAGER')
        )
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR provider_id::text = NULLIF(current_setting('app.current_provider_id', true), '')
    );

-- ============================================================================
-- Bookings Policy
-- Customers see their bookings; providers see bookings for their services
-- ============================================================================
CREATE POLICY bkg_bookings_rls ON bkg_bookings
    FOR ALL
    TO PUBLIC
    USING (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        
        -- Customer viewing their own booking
        OR customer_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        
        -- Provider viewing bookings for their services
        OR EXISTS (
            SELECT 1 FROM hotel_services hs
            WHERE hs.id = bkg_bookings.service_id
              AND hs.provider_id::text = NULLIF(current_setting('app.current_provider_id', true), '')
        )
        
        -- Platform admin
        OR EXISTS (
            SELECT 1 FROM sys_user_roles ur
            JOIN sys_roles r ON r.id = ur.role_id
            WHERE ur.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
              AND r.code IN ('SUPER_ADMIN', 'SUPPORT_MANAGER', 'FINANCE_MANAGER')
        )
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR customer_id::text = NULLIF(current_setting('app.current_user_id', true), '')
    );

-- Create indexes to optimize RLS policy checks
CREATE INDEX IF NOT EXISTS idx_hotel_provider_staff_user_lookup 
    ON hotel_provider_staff(user_id, provider_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_roles_user_role_lookup 
    ON sys_user_roles(user_id, role_id);

COMMENT ON POLICY hotel_service_providers_rls IS 
    'Multi-tenant isolation for service providers';

COMMENT ON POLICY hotel_services_rls IS 
    'Service visibility based on provider access';

COMMENT ON POLICY bkg_bookings_rls IS 
    'Booking access for customers and associated providers';
```

#### Application DataSource Configuration

```java
// RlsAwareDataSource.java
@Configuration
public class RlsAwareDataSourceConfig {
    
    @Bean
    public DataSource rlsAwareDataSource(
        @Qualifier("primaryDataSource") DataSource targetDataSource
    ) {
        return new TransactionAwareDataSourceProxy(
            new RlsRoutingDataSource(targetDataSource)
        );
    }
}

// RlsRoutingDataSource.java
public class RlsRoutingDataSource extends AbstractRoutingDataSource {
    
    @Override
    protected Object determineCurrentLookupKey() {
        return "primary";
    }
    
    public void setRlsContext(RlsContext context) {
        try (Connection conn = getConnection()) {
            Statement stmt = conn.createStatement();
            
            // Set RLS session variables
            stmt.execute("SET LOCAL app.rls_bypass = '" + context.isBypass() + "'");
            stmt.execute("SET LOCAL app.current_user_id = '" + context.getUserId() + "'");
            stmt.execute("SET LOCAL app.current_provider_id = '" + 
                context.getProviderId() + "'");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to set RLS context", e);
        }
    }
}

// Usage in service layer
@Transactional
public List<Booking> getUserBookings(UUID userId) {
    RlsContext context = new RlsContext(userId, null, false);
    rlsDataSource.setRlsContext(context);
    return bookingRepository.findByCustomerId(userId);
}
```

---

## Phase 2: Data Integrity Improvements (P1)

### 2.1 Uniform Soft Delete Pattern

**Objective:** Add `deleted_at` column to all entity tables for consistent soft deletion.

#### Migration: 044_uniform_soft_delete.sql

```sql
-- ============================================================================
-- Migration 044: Uniform Soft Delete Pattern
-- Adds deleted_at to all entity tables lacking it
-- ============================================================================

-- Add deleted_at to tables missing it
ALTER TABLE customers 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE employees 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE pay_payments 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE pay_refunds 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE support_complaints 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE hotel_reviews 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE sys_notifications 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE discount_codes 
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Create composite indexes for soft-delete-aware queries
CREATE INDEX IF NOT EXISTS idx_customers_not_deleted 
    ON customers(id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_employees_not_deleted 
    ON employees(user_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_payments_not_deleted 
    ON pay_payments(booking_id) WHERE deleted_at IS NULL;

-- Function to perform soft delete
CREATE OR REPLACE FUNCTION soft_delete_entity(
    p_table_name VARCHAR(100),
    p_id UUID,
    p_deleted_by UUID DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    v_sql TEXT;
BEGIN
    -- Verify table has deleted_at column
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = p_table_name AND column_name = 'deleted_at'
    ) THEN
        RAISE EXCEPTION 'Table % does not support soft delete', p_table_name;
    END IF;
    
    -- Perform soft delete
    v_sql := format(
        'UPDATE %I SET deleted_at = CURRENT_TIMESTAMP WHERE id = $1',
        p_table_name
    );
    
    EXECUTE v_sql USING p_id;
    
    IF FOUND THEN
        -- Log to audit
        INSERT INTO sys_audit_logs (action, entity_type, entity_id, user_id)
        VALUES ('SOFT_DELETE', p_table_name, p_id, p_deleted_by);
        
        RETURN TRUE;
    END IF;
    
    RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- View abstraction for non-deleted records
CREATE OR REPLACE VIEW active_customers AS
SELECT * FROM customers WHERE deleted_at IS NULL;

CREATE OR REPLACE VIEW active_employees AS
SELECT * FROM employees WHERE deleted_at IS NULL;

CREATE OR REPLACE VIEW active_payments AS
SELECT * FROM pay_payments WHERE deleted_at IS NULL;

COMMENT ON FUNCTION soft_delete_entity(VARCHAR, UUID, UUID) IS 
    'Performs soft delete with audit logging';
```

---

### 2.2 Composite Unique Constraints

**Objective:** Add natural unique constraints to prevent logical duplicates.

#### Migration: 045_composite_unique_constraints.sql

```sql
-- ============================================================================
-- Migration 045: Composite Unique Constraints
-- Prevents logical duplicate records
-- ============================================================================

-- Hotel service rooms: prevent duplicate room types per service
ALTER TABLE hotel_service_rooms
    ADD CONSTRAINT uk_hotel_service_rooms_service_room_type
    UNIQUE (service_id, room_type);

-- Hotel service providers: prevent duplicate company registrations
ALTER TABLE hotel_service_providers
    ADD CONSTRAINT uk_hotel_service_providers_company_tax
    UNIQUE (company_name, tax_id);

-- Notifications: prevent duplicate notifications within same minute
ALTER TABLE sys_notifications
    ADD CONSTRAINT uk_notifications_user_type_minute
    UNIQUE (user_id, type, title, date_trunc('minute', created_at));

-- Discount codes: enhance existing constraint
ALTER TABLE disc_discount_codes
    ADD CONSTRAINT uk_disc_discount_codes_code_provider
    UNIQUE (code, provider_id);

-- Taxi bookings: one active taxi booking per booking
CREATE UNIQUE INDEX IF NOT EXISTS idx_taxi_bookings_one_active_per_booking
    ON bkg_taxi_bookings(booking_id)
    WHERE status NOT IN ('COMPLETED', 'CANCELLED');

-- User consents: latest consent per type (partial unique index)
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_user_consents_latest
    ON sys_user_consents(user_id, consent_type)
    WHERE withdrawn_at IS NULL;
```

---

### 2.3 Enhanced Ticket Number Generation

**Objective:** Increase ticket number capacity and reduce collision risk.

#### Migration: 046_enhanced_ticket_generation.sql

```sql
-- ============================================================================
-- Migration 046: Enhanced Ticket Number Generation
-- Increases randomness from 4 to 6 digits (1M/day capacity)
-- ============================================================================

-- Replace complaint ticket generator
CREATE OR REPLACE FUNCTION generate_ticket_number()
RETURNS VARCHAR(20) AS $$
DECLARE
    ticket VARCHAR(20);
    v_attempts INTEGER := 0;
BEGIN
    LOOP
        ticket := 'TKT' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || 
                  LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
        
        -- Check for uniqueness
        IF NOT EXISTS (
            SELECT 1 FROM support_complaints WHERE ticket_number = ticket
        ) THEN
            RETURN ticket;
        END IF;
        
        v_attempts := v_attempts + 1;
        IF v_attempts > 10 THEN
            RAISE EXCEPTION 'Unable to generate unique ticket number after % attempts', v_attempts;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Replace internal ticket generator
CREATE OR REPLACE FUNCTION generate_internal_ticket_number()
RETURNS VARCHAR(20) AS $$
DECLARE
    ticket VARCHAR(20);
    v_attempts INTEGER := 0;
BEGIN
    LOOP
        ticket := 'ITK' || TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDD') || 
                  LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
        
        IF NOT EXISTS (
            SELECT 1 FROM internal_tickets WHERE ticket_number = ticket
        ) THEN
            RETURN ticket;
        END IF;
        
        v_attempts := v_attempts + 1;
        IF v_attempts > 10 THEN
            RAISE EXCEPTION 'Unable to generate unique internal ticket number';
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

## Phase 3: Performance Optimization

### 3.1 JSONB Query Optimization

**Objective:** Add GIN indexes for frequently queried JSONB paths.

#### Migration: 047_jsonb_gin_indexes.sql

```sql
-- ============================================================================
-- Migration 047: JSONB GIN Indexes
-- Optimizes queries on semi-structured data
-- ============================================================================

-- Gateway response queries (payment debugging)
CREATE INDEX IF NOT EXISTS idx_pay_payments_gateway_response_gin
    ON pay_payments USING GIN (gateway_response jsonb_path_ops);

-- Service attributes filtering
CREATE INDEX IF NOT EXISTS idx_hotel_services_attributes_gin
    ON hotel_services USING GIN (attributes jsonb_path_ops);

-- Notification metadata
CREATE INDEX IF NOT EXISTS idx_sys_notifications_metadata_gin
    ON sys_notifications USING GIN (metadata jsonb_path_ops);

-- Audit logs changes
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_changes_gin
    ON sys_audit_logs USING GIN (new_value jsonb_path_ops);

-- Expression index for specific JSON path (amenities contains wifi)
CREATE INDEX IF NOT EXISTS idx_hotel_services_has_wifi
    ON hotel_services ((attributes->>'has_wifi'))
    WHERE attributes ? 'has_wifi';
```

---

### 3.2 Audit Log Partitioning Activation

**Objective:** Activate HASH partitioning for audit logs to improve query performance.

#### Note: Already implemented in migration 034

Verify and monitor:

```sql
-- Check partition status
SELECT 
    schemaname,
    tablename,
    partitioned
FROM pg_tables
WHERE tablename = 'sys_audit_logs';

-- View partition details
SELECT 
    inhparent::regclass AS parent_table,
    inhrelid::regclass AS partition_table
FROM pg_inherits
WHERE inhparent = 'sys_audit_logs'::regclass;

-- Monitor partition sizes
SELECT 
    schemaname || '.' || tablename AS table_name,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) AS size
FROM pg_tables
WHERE tablename LIKE 'sys_audit_logs_p%'
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;
```

---

### 3.3 Materialized View Refresh Strategy

**Objective:** Implement automated refresh for reporting views.

#### Migration: 048_materialized_view_refresh.sql

```sql
-- ============================================================================
-- Migration 048: Materialized View Management
-- Creates additional reporting views and refresh schedule
-- ============================================================================

-- Daily booking totals by service type
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_bkg_daily_totals_by_type AS
SELECT 
    (created_at AT TIME ZONE 'UTC')::date AS booking_date,
    service_id,
    COUNT(*) AS booking_count,
    SUM(total_amount) FILTER (WHERE status NOT IN ('CANCELLED', 'EXPIRED')) AS revenue,
    AVG(total_amount) FILTER (WHERE status NOT IN ('CANCELLED', 'EXPIRED')) AS avg_booking_value
FROM bkg_bookings
GROUP BY 1, 2;

CREATE UNIQUE INDEX ON mv_bkg_daily_totals_by_type (booking_date, service_id);

-- Provider performance summary
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_provider_performance AS
SELECT 
    sp.id AS provider_id,
    sp.company_name,
    COUNT(DISTINCT b.id) AS total_bookings,
    COUNT(DISTINCT b.id) FILTER (WHERE b.status = 'COMPLETED') AS completed_bookings,
    SUM(b.total_amount) FILTER (WHERE b.status = 'COMPLETED') AS total_revenue,
    AVG(r.rating) FILTER (WHERE r.status = 'APPROVED') AS avg_rating,
    COUNT(r.id) FILTER (WHERE r.status = 'APPROVED') AS review_count
FROM hotel_service_providers sp
LEFT JOIN hotel_services hs ON hs.provider_id = sp.id
LEFT JOIN bkg_bookings b ON b.service_id = hs.id
LEFT JOIN hotel_reviews r ON r.service_id = hs.id
GROUP BY sp.id, sp.company_name;

CREATE UNIQUE INDEX ON mv_provider_performance (provider_id);

-- Function to refresh materialized views
CREATE OR REPLACE FUNCTION refresh_reporting_views()
RETURNS TABLE(view_name TEXT, refresh_duration INTERVAL, rows_affected BIGINT) AS $$
DECLARE
    v_start TIMESTAMPTZ;
    v_end TIMESTAMPTZ;
    v_rows BIGINT;
BEGIN
    -- Refresh payment totals
    v_start := clock_timestamp();
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pay_daily_totals;
    v_end := clock_timestamp();
    SELECT COUNT(*) INTO v_rows FROM mv_pay_daily_totals;
    RETURN NEXT;
    
    -- Refresh booking totals
    v_start := clock_timestamp();
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_bkg_daily_totals_by_type;
    v_end := clock_timestamp();
    SELECT COUNT(*) INTO v_rows FROM mv_bkg_daily_totals_by_type;
    RETURN NEXT;
    
    -- Refresh provider performance (daily)
    IF EXTRACT(HOUR FROM CURRENT_TIME) < 2 THEN
        v_start := clock_timestamp();
        REFRESH MATERIALIZED VIEW CONCURRENTLY mv_provider_performance;
        v_end := clock_timestamp();
        SELECT COUNT(*) INTO v_rows FROM mv_provider_performance;
        RETURN NEXT;
    END IF;
END;
$$ LANGUAGE plpgsql;
```

---

## Phase 4: Compliance & Monitoring

### 4.1 Data Retention Job Implementation

**Objective:** Automated cleanup based on retention policies.

#### Migration: 049_data_retention_scheduler.sql

```sql
-- ============================================================================
-- Migration 049: Data Retention Scheduler
-- Implements automated data cleanup based on policies
-- ============================================================================

-- Function to execute retention policies
CREATE OR REPLACE FUNCTION execute_data_retention()
RETURNS TABLE(entity_type TEXT, action_taken TEXT, rows_affected BIGINT) AS $$
DECLARE
    v_policy RECORD;
    v_cutoff_date DATE;
    v_rows BIGINT;
    v_sql TEXT;
BEGIN
    FOR v_policy IN 
        SELECT * FROM sys_data_retention_policies 
        WHERE enabled = TRUE 
          AND (next_execution IS NULL OR next_execution <= CURRENT_TIMESTAMP)
    LOOP
        v_cutoff_date := CURRENT_DATE - (v_policy.retention_period_days || ' days')::INTERVAL;
        
        CASE v_policy.action
            WHEN 'DELETE' THEN
                v_sql := format(
                    'DELETE FROM %I WHERE created_at < $1',
                    v_policy.entity_type
                );
                EXECUTE v_sql USING v_cutoff_date;
                GET DIAGNOSTICS v_rows = ROW_COUNT;
                
                -- Update last execution
                UPDATE sys_data_retention_policies
                SET 
                    last_execution = CURRENT_TIMESTAMP,
                    next_execution = CURRENT_TIMESTAMP + INTERVAL '1 day'
                WHERE entity_type = v_policy.entity_type;
                
            WHEN 'ARCHIVE' THEN
                -- Archive logic for audit logs
                IF v_policy.entity_type = 'sys_audit_logs' THEN
                    INSERT INTO sys_audit_logs_archive
                    SELECT *, CURRENT_TIMESTAMP AS archived_at
                    FROM sys_audit_logs
                    WHERE created_at < v_cutoff_date;
                    
                    GET DIAGNOSTICS v_rows = ROW_COUNT;
                    
                    DELETE FROM sys_audit_logs
                    WHERE created_at < v_cutoff_date;
                    
                    UPDATE sys_data_retention_policies
                    SET 
                        last_execution = CURRENT_TIMESTAMP,
                        next_execution = CURRENT_TIMESTAMP + INTERVAL '7 days'
                    WHERE entity_type = v_policy.entity_type;
                END IF;
                
            ELSE
                v_rows := 0;
        END CASE;
        
        RETURN QUERY SELECT v_policy.entity_type, v_policy.action, v_rows;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Schedule via pg_cron (if available)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        -- Run daily at 2 AM UTC
        PERFORM cron.schedule(
            'data-retention-daily',
            '0 2 * * *',
            'SELECT execute_data_retention()'
        );
    END IF;
END $$;
```

---

### 4.2 Database Health Monitoring Views

**Objective:** Provide DBA visibility into database health.

#### Migration: 050_dba_monitoring_views.sql

```sql
-- ============================================================================
-- Migration 050: DBA Monitoring Views
-- Operational visibility into database health
-- ============================================================================

-- Table growth monitoring
CREATE OR REPLACE VIEW dba_table_growth AS
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname || '.' || tablename)) AS total_size,
    pg_size_pretty(pg_relation_size(schemaname || '.' || tablename)) AS table_size,
    pg_size_pretty(pg_indexes_size(schemaname || '.' || tablename)) AS index_size,
    n_live_tup AS row_count,
    n_dead_tup AS dead_row_count,
    CASE WHEN n_live_tup > 0 
         THEN round(100.0 * n_dead_tup / n_live_tup, 2)
         ELSE 0 
    END AS dead_ratio_percent
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;

-- Index usage statistics
CREATE OR REPLACE VIEW dba_index_usage AS
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched,
    CASE WHEN idx_scan > 0 THEN 'ACTIVE' ELSE 'UNUSED' END AS usage_status
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Long-running queries
CREATE OR REPLACE VIEW dba_active_queries AS
SELECT 
    pid,
    usename,
    datname,
    client_addr,
    application_name,
    state,
    wait_event_type,
    wait_event,
    query,
    EXTRACT(EPOCH FROM (NOW() - query_start))::INTEGER AS duration_seconds,
    query_start
FROM pg_stat_activity
WHERE state != 'idle'
  AND query NOT ILIKE '%pg_stat_activity%'
ORDER BY query_start;

-- Lock monitoring
CREATE OR REPLACE VIEW dba_locks AS
SELECT 
    blocked_locks.pid AS blocked_pid,
    blocked_activity.usename AS blocked_user,
    blocking_locks.pid AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocked_activity.query AS blocked_query,
    blocking_activity.query AS blocking_query
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks 
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
    AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
    AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
    AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
    AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
    AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
    AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
    AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
    AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

-- Connection pool monitoring
CREATE OR REPLACE VIEW dba_connection_stats AS
SELECT 
    count(*) AS total_connections,
    count(*) FILTER (WHERE state = 'active') AS active_connections,
    count(*) FILTER (WHERE state = 'idle') AS idle_connections,
    count(*) FILTER (WHERE state = 'idle in transaction') AS idle_in_transaction,
    count(*) FILTER (WHERE backend_type = 'client backend') AS client_backends,
    max_conn.setting::INTEGER AS max_connections,
    count(*) * 100 / max_conn.setting::INTEGER AS connection_utilization_percent
FROM pg_stat_activity,
     (SELECT setting FROM pg_settings WHERE name = 'max_connections') max_conn
GROUP BY max_conn.setting;

COMMENT ON VIEW dba_table_growth IS 'Monitor table sizes and bloat';
COMMENT ON VIEW dba_index_usage IS 'Identify unused indexes for removal';
COMMENT ON VIEW dba_active_queries IS 'Find long-running queries';
COMMENT ON VIEW dba_locks IS 'Detect blocking locks';
COMMENT ON VIEW dba_connection_stats IS 'Monitor connection pool utilization';
```

---

## Testing Strategy

### Pre-Deployment Testing

1. **Unit Tests**
   - Test each function independently
   - Verify constraint enforcement
   - Validate trigger behavior

2. **Integration Tests**
   - Test RLS policies with different user contexts
   - Verify encryption/decryption round-trip
   - Test password history enforcement

3. **Performance Tests**
   - Benchmark query performance before/after
   - Load test with concurrent users
   - Measure RLS overhead

### Rollback Procedures

Each migration includes a corresponding rollback script:
- `040_rollback_pii_encryption.sql`
- `041_rollback_password_history.sql`
- `042_rollback_mfa.sql`
- etc.

**Rollback Decision Matrix:**

| Issue Severity | Rollback Trigger | Approval Required |
|---------------|------------------|-------------------|
| Critical (data loss) | Immediate | CTO |
| High (service outage) | 15 min timeout | Engineering Manager |
| Medium (degraded perf) | 1 hour timeout | Tech Lead |
| Low (minor bugs) | Next business day | Team consensus |

---

## Success Metrics

| Metric | Baseline | Target | Measurement Method |
|--------|----------|--------|-------------------|
| PII fields encrypted | 0% | 100% | Audit query on cipher columns |
| MFA enrollment rate | 0% | 80% | sys_users.mfa_enabled count |
| RLS policy coverage | 0 tables | 7 tables | pg_policies count |
| Password reuse incidents | N/A | 0 | Failed change attempts |
| Query latency (p95) | Current | -10% | APM metrics |
| Audit log query time | Current | -50% | EXPLAIN ANALYZE |

---

## Deployment Schedule

### Week 1-2: Phase 1 (Critical Security)
- Day 1-3: PII encryption deployment
- Day 4-7: Password history rollout
- Day 8-10: MFA implementation
- Day 11-14: RLS pilot → production

### Week 3-4: Phase 2 (Data Integrity)
- Day 15-17: Soft delete uniform pattern
- Day 18-21: Composite unique constraints

### Week 5-6: Phase 3 (Performance)
- Day 22-25: JSONB GIN indexes
- Day 26-28: Materialized view optimization

### Week 7-8: Phase 4 (Compliance)
- Day 29-32: Data retention jobs
- Day 33-35: Monitoring views
- Day 36-40: Documentation and training

---

## Conclusion

This implementation plan provides a structured approach to addressing the database weaknesses identified in the assessment. Each phase builds upon the previous, ensuring stability while progressively improving security, integrity, and performance.

**Key Success Factors:**
1. Thorough testing in staging environment
2. Clear rollback procedures for each migration
3. Application code changes coordinated with database changes
4. Monitoring and alerting throughout deployment
5. Team training on new features (MFA, RLS)

**Next Steps:**
1. Review and approve this plan
2. Set up staging environment mirror
3. Begin Phase 0 preparation
4. Schedule maintenance windows

---

**Plan Prepared By:** Database Architecture Team  
**Approved By:** [Pending]  
**Implementation Start Date:** [TBD]
