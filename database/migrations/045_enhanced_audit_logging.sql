-- Migration: 045_enhanced_audit_logging.sql
-- Phase: 1 (Critical Security & Integrity)
-- Description: Expands audit logging to capture detailed changes (OLD/NEW values) 
--              for sensitive tables and adds retention policies.

BEGIN;

-- 1. Enhance audit_logs table to store JSONB diffs
ALTER TABLE audit_logs 
    ADD COLUMN IF NOT EXISTS old_data JSONB,
    ADD COLUMN IF NOT EXISTS new_data JSONB,
    ADD COLUMN IF NOT EXISTS client_ip INET,
    ADD COLUMN IF NOT EXISTS user_agent TEXT;

-- Index for searching within JSONB data (GIN Index)
CREATE INDEX IF NOT EXISTS idx_audit_logs_old_data_gin 
    ON audit_logs USING GIN (old_data jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_audit_logs_new_data_gin 
    ON audit_logs USING GIN (new_data jsonb_path_ops);

-- Index for IP address searches
CREATE INDEX IF NOT EXISTS idx_audit_logs_client_ip 
    ON audit_logs(client_ip);

-- 2. Create a reusable trigger function for auditing changes
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
DECLARE
    v_old_data JSONB;
    v_new_data JSONB;
    v_operation TEXT;
BEGIN
    v_operation := TG_OP;
    
    IF TG_OP = 'INSERT' THEN
        v_old_data := NULL;
        v_new_data := to_jsonb(NEW);
    ELSIF TG_OP = 'UPDATE' THEN
        v_old_data := to_jsonb(OLD);
        v_new_data := to_jsonb(NEW);
        -- Optimization: If no actual change, skip logging (optional)
        IF v_old_data = v_new_data THEN
            RETURN NEW;
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        v_old_data := to_jsonb(OLD);
        v_new_data := NULL;
    ELSIF TG_OP = 'TRUNCATE' THEN
        v_old_data := NULL;
        v_new_data := NULL;
    END IF;

    INSERT INTO audit_logs (
        table_name, 
        operation, 
        old_data, 
        new_data, 
        created_by, 
        created_at,
        client_ip,
        user_agent
    ) VALUES (
        TG_TABLE_NAME,
        v_operation,
        v_old_data,
        v_new_data,
        COALESCE(current_setting('app.current_user_id', TRUE), 'system'),
        NOW(),
        current_setting('app.client_ip', TRUE)::INET,
        current_setting('app.user_agent', TRUE)
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Apply Audit Triggers to Sensitive Tables
-- Helper to apply trigger
DO $$
DECLARE
    t_name TEXT;
BEGIN
    FOREACH t_name IN ARRAY ARRAY['users', 'organizations', 'projects', 'tasks', 'roles', 'permissions']
    LOOP
        EXECUTE format(
            'DROP TRIGGER IF EXISTS audit_trigger_%I ON %I',
            t_name, t_name
        );
        
        EXECUTE format(
            'CREATE TRIGGER audit_trigger_%I 
             AFTER INSERT OR UPDATE OR DELETE ON %I 
             FOR EACH ROW EXECUTE FUNCTION audit_trigger_function()',
            t_name, t_name
        );
    END LOOP;
END $$;

-- 4. Create Partitioning for Audit Logs (Optional but recommended for high volume)
-- Note: Actual partitioning requires superuser and specific setup. 
-- Here we create a view for "Recent Audits" (last 90 days) for performance
CREATE OR REPLACE VIEW recent_audit_logs AS
SELECT * FROM audit_logs
WHERE created_at >= NOW() - INTERVAL '90 days';

-- 5. Create Function to Purge Old Audit Logs (Retention Policy)
CREATE OR REPLACE FUNCTION purge_old_audit_logs(retention_days INTEGER DEFAULT 365)
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER;
BEGIN
    DELETE FROM audit_logs
    WHERE created_at < NOW() - (retention_days || ' days')::INTERVAL;
    
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;
    
    -- Log the purge action itself (manually to avoid recursion if trigger exists)
    INSERT INTO audit_logs (table_name, operation, created_at, created_by)
    VALUES ('audit_logs', 'PURGE', NOW(), 'system');
    
    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. Create View for Security Incident Investigation
-- Aggregates failed logins and suspicious activities
CREATE OR REPLACE VIEW security_incident_summary AS
SELECT 
    DATE(created_at) as incident_date,
    client_ip,
    COUNT(*) FILTER (WHERE table_name = 'auth_attempts' AND operation = 'FAILURE') as failed_logins,
    COUNT(*) FILTER (WHERE table_name = 'users' AND operation = 'UPDATE' AND new_data->>'password_hash' IS NOT NULL) as password_changes,
    COUNT(*) FILTER (WHERE operation = 'DELETE') as deletions
FROM audit_logs
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY DATE(created_at), client_ip
HAVING COUNT(*) > 5; -- Threshold for investigation

COMMIT;

-- Usage Example for App Layer:
-- SET LOCAL app.current_user_id = 'uuid-here';
-- SET LOCAL app.client_ip = '192.168.1.1';
-- SET LOCAL app.user_agent = 'Mozilla/5.0...';

-- ROLLBACK SCRIPT
/*
BEGIN;
DROP VIEW IF EXISTS security_incident_summary;
DROP FUNCTION IF EXISTS purge_old_audit_logs(INTEGER);
DROP VIEW IF EXISTS recent_audit_logs;
DROP TRIGGER IF EXISTS audit_trigger_users ON users;
DROP TRIGGER IF EXISTS audit_trigger_organizations ON organizations;
DROP TRIGGER IF EXISTS audit_trigger_projects ON projects;
DROP TRIGGER IF EXISTS audit_trigger_tasks ON tasks;
DROP TRIGGER IF EXISTS audit_trigger_roles ON roles;
DROP TRIGGER IF EXISTS audit_trigger_permissions ON permissions;
DROP FUNCTION IF EXISTS audit_trigger_function();
DROP INDEX IF EXISTS idx_audit_logs_client_ip;
DROP INDEX IF EXISTS idx_audit_logs_new_data_gin;
DROP INDEX IF EXISTS idx_audit_logs_old_data_gin;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS user_agent;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS client_ip;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS new_data;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS old_data;
COMMIT;
*/
