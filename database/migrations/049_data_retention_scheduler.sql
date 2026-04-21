-- ============================================================================
-- Migration 049: Data Retention Scheduler and Automated Cleanup
-- Phase: 4.1 - Compliance & Monitoring
-- Prerequisites: Migration 034 (audit log partitioning), Migration 043 (soft deletes)
-- Rollback: Remove scheduled jobs and retention function
-- ============================================================================

BEGIN;

-- Set statement timeout for safety
SET LOCAL statement_timeout = '30min';

-- ----------------------------------------------------------------------------
-- Create Archive Tables for Long-term Storage
-- ----------------------------------------------------------------------------

-- Audit logs archive (for compliance beyond operational retention)
CREATE TABLE IF NOT EXISTS sys_audit_logs_archive (
    id UUID,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100),
    entity_id UUID,
    user_id UUID,
    provider_id UUID,
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retention_until DATE NOT NULL,
    
    PRIMARY KEY (id, archived_at)
) PARTITION BY RANGE (archived_at);

-- Create annual partitions for audit archive
CREATE TABLE IF NOT EXISTS sys_audit_logs_archive_2024 
    PARTITION OF sys_audit_logs_archive
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE IF NOT EXISTS sys_audit_logs_archive_2025 
    PARTITION OF sys_audit_logs_archive
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE TABLE IF NOT EXISTS sys_audit_logs_archive_2026 
    PARTITION OF sys_audit_logs_archive
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');

-- Indexes for archive queries
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_archive_entity
    ON sys_audit_logs_archive (entity_type, entity_id, archived_at);

CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_archive_user
    ON sys_audit_logs_archive (user_id, archived_at);

CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_archive_retention
    ON sys_audit_logs_archive (retention_until);

COMMENT ON TABLE sys_audit_logs_archive IS 
    'Long-term archive of audit logs for compliance (7+ years retention)';

-- ----------------------------------------------------------------------------
-- MFA Attempt Logs Archive
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_mfa_attempt_logs_archive (
    id UUID,
    user_id UUID NOT NULL,
    attempt_type VARCHAR(50) NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    failure_reason VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (archived_at);

CREATE TABLE IF NOT EXISTS sys_mfa_attempt_logs_archive_2024 
    PARTITION OF sys_mfa_attempt_logs_archive
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE IF NOT EXISTS sys_mfa_attempt_logs_archive_2025 
    PARTITION OF sys_mfa_attempt_logs_archive
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

CREATE INDEX IF NOT EXISTS idx_sys_mfa_attempt_logs_archive_user
    ON sys_mfa_attempt_logs_archive (user_id, archived_at);

COMMENT ON TABLE sys_mfa_attempt_logs_archive IS 
    'Archived MFA attempt logs for security analysis';

-- ----------------------------------------------------------------------------
-- Data Retention Execution Function
-- Implements automated cleanup based on retention policies
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION execute_data_retention()
RETURNS TABLE(
    entity_type TEXT, 
    action_taken TEXT, 
    rows_affected BIGINT,
    execution_time TIMESTAMPTZ
) AS $$
DECLARE
    v_policy RECORD;
    v_cutoff_date TIMESTAMPTZ;
    v_rows BIGINT;
    v_sql TEXT;
    v_archive_table TEXT;
BEGIN
    -- Process each enabled retention policy
    FOR v_policy IN 
        SELECT * FROM sys_data_retention_policies 
        WHERE enabled = TRUE 
          AND (next_execution IS NULL OR next_execution <= CURRENT_TIMESTAMP)
        ORDER BY priority ASC
    LOOP
        v_cutoff_date := CURRENT_TIMESTAMP - (v_policy.retention_period_days || ' days')::INTERVAL;
        v_rows := 0;
        
        BEGIN
            CASE v_policy.action
                WHEN 'DELETE' THEN
                    -- Soft delete preferred, hard delete for GDPR requests
                    IF v_policy.hard_delete = TRUE THEN
                        v_sql := format(
                            'DELETE FROM %I WHERE created_at < $1',
                            v_policy.entity_type
                        );
                        EXECUTE v_sql USING v_cutoff_date;
                        GET DIAGNOSTICS v_rows = ROW_COUNT;
                        
                        RAISE NOTICE 'Hard deleted % rows from %', v_rows, v_policy.entity_type;
                    ELSE
                        -- Soft delete with tracking
                        v_sql := format(
                            'UPDATE %I SET deleted_at = $1, deleted_by = ''$2''::uuid 
                             WHERE created_at < $3 AND deleted_at IS NULL',
                            v_policy.entity_type
                        );
                        EXECUTE v_sql USING CURRENT_TIMESTAMP, '00000000-0000-0000-0000-000000000000', v_cutoff_date;
                        GET DIAGNOSTICS v_rows = ROW_COUNT;
                        
                        RAISE NOTICE 'Soft deleted % rows from %', v_rows, v_policy.entity_type;
                    END IF;
                    
                WHEN 'ARCHIVE' THEN
                    -- Archive then delete for audit logs
                    IF v_policy.entity_type = 'sys_audit_logs' THEN
                        v_archive_table := 'sys_audit_logs_archive';
                        
                        -- Move old records to archive
                        v_sql := format($fmt$
                            INSERT INTO %I (
                                id, action, entity_type, entity_id, user_id, provider_id,
                                old_value, new_value, ip_address, user_agent, created_at,
                                archived_at, retention_until
                            )
                            SELECT 
                                id, action, entity_type, entity_id, user_id, provider_id,
                                old_value, new_value, ip_address, user_agent, created_at,
                                CURRENT_TIMESTAMP,
                                CURRENT_DATE + (%L || ' years')::INTERVAL
                            FROM %I
                            WHERE created_at < $1
                            ON CONFLICT DO NOTHING
                        $fmt$, 
                        v_archive_table, 
                        v_policy.archive_retention_years,
                        v_policy.entity_type
                        );
                        
                        EXECUTE v_sql USING v_cutoff_date;
                        GET DIAGNOSTICS v_rows = ROW_COUNT;
                        
                        -- Delete from source after successful archive
                        IF v_rows > 0 THEN
                            DELETE FROM sys_audit_logs
                            WHERE created_at < v_cutoff_date
                              AND id IN (
                                  SELECT id FROM sys_audit_logs_archive 
                                  WHERE archived_at >= CURRENT_TIMESTAMP - INTERVAL '1 hour'
                              );
                            
                            RAISE NOTICE 'Archived % audit log entries', v_rows;
                        END IF;
                    END IF;
                    
                    -- Archive MFA logs
                    IF v_policy.entity_type = 'sys_mfa_attempt_logs' THEN
                        INSERT INTO sys_mfa_attempt_logs_archive (
                            id, user_id, attempt_type, success, ip_address, 
                            user_agent, failure_reason, created_at, archived_at
                        )
                        SELECT 
                            id, user_id, attempt_type, success, ip_address,
                            user_agent, failure_reason, created_at, CURRENT_TIMESTAMP
                        FROM sys_mfa_attempt_logs
                        WHERE created_at < v_cutoff_date
                        ON CONFLICT DO NOTHING;
                        
                        GET DIAGNOSTICS v_rows = ROW_COUNT;
                        
                        IF v_rows > 0 THEN
                            DELETE FROM sys_mfa_attempt_logs
                            WHERE created_at < v_cutoff_date;
                            
                            RAISE NOTICE 'Archived % MFA attempt logs', v_rows;
                        END IF;
                    END IF;
                    
                WHEN 'ANONYMIZE' THEN
                    -- Anonymize PII for inactive users (GDPR compliance)
                    IF v_policy.entity_type = 'customers' THEN
                        UPDATE customers
                        SET 
                            first_name = '[REDACTED]',
                            last_name = '[REDACTED]',
                            email = CONCAT('redacted_', id::text, '@redacted.local'),
                            phone_number = NULL,
                            id_document_number = NULL,
                            id_document_number_cipher = NULL,
                            pii_encryption_version = 99,
                            anonymized_at = CURRENT_TIMESTAMP
                        WHERE created_at < v_cutoff_date
                          AND (
                              SELECT COUNT(*) FROM bkg_bookings 
                              WHERE customer_id = customers.id
                          ) = 0
                          AND anonymized_at IS NULL;
                        
                        GET DIAGNOSTICS v_rows = ROW_COUNT;
                        RAISE NOTICE 'Anonymized % inactive customer records', v_rows;
                    END IF;
                    
                ELSE
                    v_rows := 0;
            END CASE;
            
            -- Update policy execution tracking
            UPDATE sys_data_retention_policies
            SET 
                last_execution = CURRENT_TIMESTAMP,
                next_execution = CURRENT_TIMESTAMP + v_policy.execution_frequency,
                total_rows_processed = COALESCE(total_rows_processed, 0) + v_rows
            WHERE entity_type = v_policy.entity_type;
            
            -- Return results
            RETURN QUERY SELECT v_policy.entity_type::TEXT, v_policy.action::TEXT, v_rows, CURRENT_TIMESTAMP;
            
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING 'Error processing retention policy for %: %', 
                v_policy.entity_type, SQLERRM;
            
            -- Log error but continue with other policies
            INSERT INTO sys_data_retention_errors (
                entity_type, error_message, failed_at
            ) VALUES (
                v_policy.entity_type, 
                SQLERRM, 
                CURRENT_TIMESTAMP
            );
            
            CONTINUE;
        END;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION execute_data_retention() IS 
    'Executes all enabled data retention policies with archiving and cleanup';

-- ----------------------------------------------------------------------------
-- Error Tracking Table for Retention Jobs
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_data_retention_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    failed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID
);

CREATE INDEX IF NOT EXISTS idx_sys_data_retention_errors_unresolved
    ON sys_data_retention_errors (entity_type, failed_at DESC)
    WHERE resolved = FALSE;

COMMENT ON TABLE sys_data_retention_errors IS 
    'Tracks errors during data retention job execution';

-- ----------------------------------------------------------------------------
-- Schedule via pg_cron (if available)
-- Falls back to application-level scheduling if pg_cron not installed
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        -- Remove existing schedule if any
        PERFORM cron.unschedule('data-retention-daily');
        
        -- Run daily at 2 AM UTC
        PERFORM cron.schedule(
            'data-retention-daily',
            '0 2 * * *',
            'SELECT execute_data_retention()'
        );
        
        RAISE NOTICE 'Scheduled daily data retention job at 2 AM UTC';
    ELSE
        RAISE NOTICE 'pg_cron not available - use application scheduler for execute_data_retention()';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- Manual Execution Helper Function
-- For testing and on-demand execution
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION run_data_retention(
    p_entity_type TEXT DEFAULT NULL,
    p_dry_run BOOLEAN DEFAULT TRUE
)
RETURNS TABLE(
    entity_type TEXT,
    action TEXT,
    estimated_rows BIGINT,
    cutoff_date TIMESTAMPTZ
) AS $$
DECLARE
    v_policy RECORD;
    v_cutoff TIMESTAMPTZ;
    v_estimate BIGINT;
    v_sql TEXT;
BEGIN
    FOR v_policy IN 
        SELECT * FROM sys_data_retention_policies 
        WHERE enabled = TRUE 
          AND (p_entity_type IS NULL OR entity_type = p_entity_type)
    LOOP
        v_cutoff := CURRENT_TIMESTAMP - (v_policy.retention_period_days || ' days')::INTERVAL;
        
        -- Estimate affected rows
        IF v_policy.action = 'DELETE' THEN
            v_sql := format(
                'SELECT COUNT(*) FROM %I WHERE created_at < $1',
                v_policy.entity_type
            );
            EXECUTE v_sql USING v_cutoff INTO v_estimate;
        ELSIF v_policy.action = 'ARCHIVE' THEN
            v_sql := format(
                'SELECT COUNT(*) FROM %I WHERE created_at < $1',
                v_policy.entity_type
            );
            EXECUTE v_sql USING v_cutoff INTO v_estimate;
        ELSE
            v_estimate := 0;
        END IF;
        
        RETURN QUERY SELECT 
            v_policy.entity_type::TEXT,
            v_policy.action::TEXT,
            v_estimate,
            v_cutoff;
    END LOOP;
    
    IF NOT p_dry_run AND p_entity_type IS NULL THEN
        RAISE NOTICE 'Executing actual retention cleanup...';
        RETURN QUERY SELECT * FROM execute_data_retention();
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION run_data_retention(TEXT, BOOLEAN) IS 
    'Preview or execute data retention for specific entity type';

-- ----------------------------------------------------------------------------
-- Verification Queries
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_archive_tables INTEGER;
    v_policies_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_archive_tables
    FROM pg_tables
    WHERE tablename LIKE '%_archive%';
    
    SELECT COUNT(*) INTO v_policies_count
    FROM sys_data_retention_policies
    WHERE enabled = TRUE;
    
    RAISE NOTICE 'Archive tables: %, Active retention policies: %', 
        v_archive_tables, v_policies_count;
END $$;

-- Show retention policy status
SELECT 
    entity_type,
    action,
    retention_period_days,
    enabled,
    last_execution,
    next_execution,
    total_rows_processed
FROM sys_data_retention_policies
ORDER BY priority;

COMMIT;

-- ============================================================================
-- ROLLBACK SCRIPT
-- Execute this section to rollback migration 049
-- ============================================================================
/*
BEGIN;

-- Unschedule cron jobs
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.unschedule('data-retention-daily');
    END IF;
END $$;

DROP FUNCTION IF EXISTS run_data_retention(TEXT, BOOLEAN);
DROP FUNCTION IF EXISTS execute_data_retention();

DROP TABLE IF EXISTS sys_data_retention_errors CASCADE;
DROP TABLE IF EXISTS sys_mfa_attempt_logs_archive CASCADE;
DROP TABLE IF EXISTS sys_audit_logs_archive CASCADE;

COMMIT;
*/
