-- ============================================================================
-- Migration 040: Mandatory PII Encryption Enforcement
-- Prerequisites: Application encryption service deployed
-- Rollback: 040_rollback_pii_encryption.sql
-- Phase: 1 (Critical Security)
-- Estimated Duration: 30 minutes (excluding backfill)
-- ============================================================================

BEGIN;

-- Add NOT NULL constraints to cipher columns (after backfill is complete)
-- These ensure no new unencrypted PII can be inserted
ALTER TABLE customers 
    ALTER COLUMN id_document_number_cipher SET NOT NULL;

ALTER TABLE hotel_service_providers 
    ALTER COLUMN bank_account_number_cipher SET NOT NULL,
    ALTER COLUMN tax_id_cipher SET NOT NULL;

-- Add encryption version tracking to monitor encryption key rotations
ALTER TABLE sys_users 
    ADD COLUMN IF NOT EXISTS pii_encryption_version SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS pii_encryption_version SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS pii_encryption_version SMALLINT NOT NULL DEFAULT 0;

-- Create indexes for encryption audit queries
CREATE INDEX IF NOT EXISTS idx_customers_pii_version 
    ON customers(pii_encryption_version);

CREATE INDEX IF NOT EXISTS idx_providers_pii_version 
    ON hotel_service_providers(pii_encryption_version);

CREATE INDEX IF NOT EXISTS idx_sys_users_pii_version 
    ON sys_users(pii_encryption_version);

-- Create function to prevent plaintext PII insertion
CREATE OR REPLACE FUNCTION enforce_pii_encryption()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if plaintext PII is being inserted/updated without corresponding cipher
    IF TG_TABLE_NAME = 'customers' THEN
        IF NEW.id_document_number IS NOT NULL 
           AND NEW.id_document_number_cipher IS NULL THEN
            RAISE EXCEPTION 'PII_ENCRYPTION_REQUIRED: id_document_number must be encrypted before storage';
        END IF;
        -- Update encryption version on change
        IF OLD.id_document_number_cipher IS DISTINCT FROM NEW.id_document_number_cipher THEN
            NEW.pii_encryption_version := COALESCE(NEW.pii_encryption_version, 0) + 1;
        END IF;
        
    ELSIF TG_TABLE_NAME = 'hotel_service_providers' THEN
        IF (NEW.bank_account_number IS NOT NULL 
            AND NEW.bank_account_number_cipher IS NULL)
           OR (NEW.tax_id IS NOT NULL 
            AND NEW.tax_id_cipher IS NULL) THEN
            RAISE EXCEPTION 'PII_ENCRYPTION_REQUIRED: financial/tax data must be encrypted before storage';
        END IF;
        -- Update encryption version on change
        IF (OLD.bank_account_number_cipher IS DISTINCT FROM NEW.bank_account_number_cipher
            OR OLD.tax_id_cipher IS DISTINCT FROM NEW.tax_id_cipher) THEN
            NEW.pii_encryption_version := COALESCE(NEW.pii_encryption_version, 0) + 1;
        END IF;
        
    ELSIF TG_TABLE_NAME = 'sys_users' THEN
        -- For sys_users, check phone/email if they have cipher columns
        IF NEW.phone IS NOT NULL AND NEW.phone_cipher IS NULL THEN
            RAISE NOTICE 'Phone encryption recommended but not enforced';
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Apply triggers to enforce encryption
DROP TRIGGER IF EXISTS trg_enforce_customer_pii_encryption ON customers;
CREATE TRIGGER trg_enforce_customer_pii_encryption
    BEFORE INSERT OR UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION enforce_pii_encryption();

DROP TRIGGER IF EXISTS trg_enforce_provider_pii_encryption ON hotel_service_providers;
CREATE TRIGGER trg_enforce_provider_pii_encryption
    BEFORE INSERT OR UPDATE ON hotel_service_providers
    FOR EACH ROW EXECUTE FUNCTION enforce_pii_encryption();

DROP TRIGGER IF EXISTS trg_enforce_sys_user_pii_encryption ON sys_users;
CREATE TRIGGER trg_enforce_sys_user_pii_encryption
    BEFORE INSERT OR UPDATE ON sys_users
    FOR EACH ROW EXECUTE FUNCTION enforce_pii_encryption();

-- Add comments for documentation
COMMENT ON FUNCTION enforce_pii_encryption() IS 
    'Prevents insertion of unencrypted PII data. Raises exception if plaintext PII detected without cipher.';

COMMENT ON COLUMN customers.pii_encryption_version IS 
    'Tracks encryption key rotation version for audit and re-encryption needs';

COMMENT ON COLUMN hotel_service_providers.pii_encryption_version IS 
    'Tracks encryption key rotation version for audit and re-encryption needs';

COMMENT ON COLUMN sys_users.pii_encryption_version IS 
    'Tracks encryption key rotation version for audit and re-encryption needs';

COMMIT;

-- ============================================================================
-- POST-MIGRATION VALIDATION
-- Run these queries to verify migration success:
-- ============================================================================

-- Verify NOT NULL constraints
SELECT 
    table_name,
    column_name,
    is_nullable
FROM information_schema.columns
WHERE table_name IN ('customers', 'hotel_service_providers')
  AND column_name IN ('id_document_number_cipher', 'bank_account_number_cipher', 'tax_id_cipher');

-- Verify triggers exist
SELECT 
    trigger_name,
    event_object_table,
    action_timing
FROM information_schema.triggers
WHERE trigger_name LIKE 'trg_enforce_%_pii_encryption';

-- Verify encryption version columns exist
SELECT 
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE column_name = 'pii_encryption_version';

-- ============================================================================
-- ROLLBACK SCRIPT: 040_rollback_pii_encryption.sql
-- ============================================================================
/*
BEGIN;

DROP TRIGGER IF EXISTS trg_enforce_customer_pii_encryption ON customers;
DROP TRIGGER IF EXISTS trg_enforce_provider_pii_encryption ON hotel_service_providers;
DROP TRIGGER IF EXISTS trg_enforce_sys_user_pii_encryption ON sys_users;

DROP FUNCTION IF EXISTS enforce_pii_encryption();

ALTER TABLE customers 
    ALTER COLUMN id_document_number_cipher DROP NOT NULL,
    ALTER COLUMN pii_encryption_version DROP NOT NULL;

ALTER TABLE hotel_service_providers 
    ALTER COLUMN bank_account_number_cipher DROP NOT NULL,
    ALTER COLUMN tax_id_cipher DROP NOT NULL,
    ALTER COLUMN pii_encryption_version DROP NOT NULL;

ALTER TABLE sys_users 
    ALTER COLUMN pii_encryption_version DROP NOT NULL;

DROP INDEX IF EXISTS idx_customers_pii_version;
DROP INDEX IF EXISTS idx_providers_pii_version;
DROP INDEX IF EXISTS idx_sys_users_pii_version;

COMMIT;
*/
