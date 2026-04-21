-- ============================================================================
-- Migration 041: Password History Enforcement Support
-- Enforces minimum password history (5 passwords) and prevents reuse
-- Prerequisites: Migration 032 (database_hardening.sql) completed
-- Rollback: 041_rollback_password_history.sql
-- Phase: 1 (Critical Security)
-- Estimated Duration: 15 minutes
-- ============================================================================

BEGIN;

-- Ensure password history table exists (from migration 032, but ensuring completeness)
CREATE TABLE IF NOT EXISTS sys_user_password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Metadata for audit
    password_changed_by UUID REFERENCES sys_users(id),
    change_reason VARCHAR(100) DEFAULT 'USER_INITIATED',
    ip_address VARCHAR(45),
    
    CONSTRAINT chk_change_reason CHECK (
        change_reason IN ('USER_INITIATED', 'ADMIN_RESET', 'SECURITY_POLICY', 'ACCOUNT_RECOVERY')
    )
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_sys_user_password_history_user 
    ON sys_user_password_history (user_id);

CREATE INDEX IF NOT EXISTS idx_sys_user_password_history_created 
    ON sys_user_password_history (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_sys_user_password_history_user_created 
    ON sys_user_password_history (user_id, created_at DESC);

-- Function to check if password matches any in history
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

-- Function to archive old password and set new one atomically
CREATE OR REPLACE FUNCTION archive_password_and_set_new(
    p_user_id UUID,
    p_new_password_hash VARCHAR(255),
    p_changed_by UUID DEFAULT NULL,
    p_change_reason VARCHAR(100) DEFAULT 'USER_INITIATED',
    p_ip_address VARCHAR(45) DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    v_old_hash VARCHAR(255);
BEGIN
    -- Get current password
    SELECT password_hash INTO v_old_hash
    FROM sys_users
    WHERE id = p_user_id;
    
    -- Archive old password if it exists
    IF v_old_hash IS NOT NULL THEN
        INSERT INTO sys_user_password_history (
            user_id, 
            password_hash,
            password_changed_by,
            change_reason,
            ip_address
        )
        VALUES (
            p_user_id, 
            v_old_hash,
            p_changed_by,
            p_change_reason,
            p_ip_address
        );
    END IF;
    
    -- Update password and metadata
    UPDATE sys_users
    SET 
        password_hash = p_new_password_hash,
        last_password_change = CURRENT_TIMESTAMP,
        password_expires_at = CURRENT_TIMESTAMP + INTERVAL '90 days',
        token_version = token_version + 1,  -- Invalidate existing sessions
        mfa_failed_attempts = 0,            -- Reset MFA failures on password change
        mfa_locked_until = NULL             -- Clear MFA lockout
    WHERE id = p_user_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to get password history for a user (for admin audit)
CREATE OR REPLACE FUNCTION get_user_password_history(
    p_user_id UUID,
    p_limit INTEGER DEFAULT 10
)
RETURNS TABLE (
    id UUID,
    created_at TIMESTAMPTZ,
    change_reason VARCHAR(100),
    changed_by_email VARCHAR(255)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        ph.id,
        ph.created_at,
        ph.change_reason,
        COALESCE(u.email, 'SYSTEM')::VARCHAR(255)
    FROM sys_user_password_history ph
    LEFT JOIN sys_users u ON ph.password_changed_by = u.id
    WHERE ph.user_id = p_user_id
    ORDER BY ph.created_at DESC
    LIMIT p_limit;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger to automatically archive password on update (alternative approach)
CREATE OR REPLACE FUNCTION auto_archive_password_on_change()
RETURNS TRIGGER AS $$
BEGIN
    -- Only archive if password actually changed
    IF OLD.password_hash IS DISTINCT FROM NEW.password_hash THEN
        INSERT INTO sys_user_password_history (
            user_id,
            password_hash,
            password_changed_by,
            change_reason
        )
        VALUES (
            NEW.id,
            OLD.password_hash,
            NEW.id,  -- Self-initiated
            'AUTO_ARCHIVE'
        );
    END IF;
    
    -- Reset security counters on password change
    NEW.token_version := COALESCE(OLD.token_version, 0) + 1;
    NEW.last_password_change := CURRENT_TIMESTAMP;
    NEW.password_expires_at := CURRENT_TIMESTAMP + INTERVAL '90 days';
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Add comment to password history table
COMMENT ON TABLE sys_user_password_history IS 
    'Stores historical password hashes to prevent password reuse. Retains last 5+ passwords per user.';

COMMENT ON FUNCTION check_password_history(UUID, VARCHAR, INTEGER) IS 
    'Validates new password against user''s password history. Returns FALSE if password was recently used.';

COMMENT ON FUNCTION archive_password_and_set_new(UUID, VARCHAR, UUID, VARCHAR, VARCHAR) IS 
    'Atomically archives old password and sets new one with proper metadata updates. Invalidates sessions.';

COMMENT ON FUNCTION get_user_password_history(UUID, INTEGER) IS 
    'Returns password change history for audit purposes. Does not return actual hashes.';

COMMIT;

-- ============================================================================
-- POST-MIGRATION VALIDATION
-- Run these queries to verify migration success:
-- ============================================================================

-- Verify password history table exists with correct structure
SELECT 
    column_name,
    data_type,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'sys_user_password_history'
ORDER BY ordinal_position;

-- Verify functions exist
SELECT 
    routine_name,
    routine_type
FROM information_schema.routines
WHERE routine_name IN (
    'check_password_history',
    'archive_password_and_set_new',
    'get_user_password_history'
);

-- Test password history function (example - replace with actual user ID)
-- SELECT check_password_history('00000000-0000-0000-0000-000000000000'::UUID, 'test_hash', 5);

-- ============================================================================
-- ROLLBACK SCRIPT: 041_rollback_password_history.sql
-- ============================================================================
/*
BEGIN;

DROP FUNCTION IF EXISTS get_user_password_history(UUID, INTEGER);
DROP FUNCTION IF EXISTS auto_archive_password_on_change();
DROP FUNCTION IF EXISTS archive_password_and_set_new(UUID, VARCHAR, UUID, VARCHAR, VARCHAR);
DROP FUNCTION IF EXISTS check_password_history(UUID, VARCHAR, INTEGER);

DROP INDEX IF EXISTS idx_sys_user_password_history_user_created;
DROP INDEX IF EXISTS idx_sys_user_password_history_created;
DROP INDEX IF EXISTS idx_sys_user_password_history_user;

DROP TABLE IF EXISTS sys_user_password_history;

COMMIT;
*/
