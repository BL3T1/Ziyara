-- ============================================================================
-- Phase 2 hardening (no TDE): audit enrichment, BRIN, materialized views,
-- rate limiting, security alerting schema, expression indexes, DBA helpers.
-- Post-015 prefixed tables. Idempotent.
-- ============================================================================

-- --- Audit trail enrichment (sys_audit_logs + archive) ----------------------
ALTER TABLE sys_audit_logs ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);
ALTER TABLE sys_audit_logs ADD COLUMN IF NOT EXISTS request_id VARCHAR(100);
ALTER TABLE sys_audit_logs ADD COLUMN IF NOT EXISTS session_id UUID;
ALTER TABLE sys_audit_logs ADD COLUMN IF NOT EXISTS provider_id UUID;
ALTER TABLE sys_audit_logs ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE sys_audit_logs ADD COLUMN IF NOT EXISTS risk_score INTEGER;
ALTER TABLE sys_audit_logs ADD COLUMN IF NOT EXISTS duration_ms INTEGER;
ALTER TABLE sys_audit_logs ADD COLUMN IF NOT EXISTS tags TEXT;

ALTER TABLE sys_audit_logs_archive ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100);
ALTER TABLE sys_audit_logs_archive ADD COLUMN IF NOT EXISTS request_id VARCHAR(100);
ALTER TABLE sys_audit_logs_archive ADD COLUMN IF NOT EXISTS session_id UUID;
ALTER TABLE sys_audit_logs_archive ADD COLUMN IF NOT EXISTS provider_id UUID;
ALTER TABLE sys_audit_logs_archive ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE sys_audit_logs_archive ADD COLUMN IF NOT EXISTS risk_score INTEGER;
ALTER TABLE sys_audit_logs_archive ADD COLUMN IF NOT EXISTS duration_ms INTEGER;
ALTER TABLE sys_audit_logs_archive ADD COLUMN IF NOT EXISTS tags TEXT;

CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_correlation ON sys_audit_logs (correlation_id);
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_request ON sys_audit_logs (request_id);

-- --- BRIN for large time-series tables --------------------------------------
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_created_brin ON sys_audit_logs USING BRIN (created_at);
CREATE INDEX IF NOT EXISTS idx_sys_notifications_created_brin ON sys_notifications USING BRIN (created_at);
CREATE INDEX IF NOT EXISTS idx_pay_payments_created_brin ON pay_payments USING BRIN (created_at);

-- --- Case-insensitive email lookup ------------------------------------------
CREATE INDEX IF NOT EXISTS idx_sys_users_email_lower ON sys_users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_customers_first_last_lower ON customers (LOWER(first_name), LOWER(last_name));

-- --- Covering index: customer booking list ----------------------------------
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_customer_created_cover
    ON bkg_bookings (customer_id, created_at DESC)
    INCLUDE (booking_reference, status, total_amount);

-- --- Materialized view: daily payment totals (refresh CONCURRENTLY) ---------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_class c
        JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'm' AND n.nspname = 'public' AND c.relname = 'mv_pay_daily_totals'
    ) THEN
        EXECUTE $mv$
            CREATE MATERIALIZED VIEW mv_pay_daily_totals AS
            SELECT (created_at AT TIME ZONE 'UTC')::date AS revenue_date,
                   currency,
                   COUNT(*)::bigint AS payment_count,
                   COALESCE(SUM(amount) FILTER (WHERE status = 'COMPLETED'), 0)::numeric(14, 2) AS completed_amount
            FROM pay_payments
            GROUP BY 1, 2
        $mv$;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_mv_pay_daily_totals ON mv_pay_daily_totals (revenue_date, currency);

-- --- API / login rate limiting ----------------------------------------------
CREATE TABLE IF NOT EXISTS sys_rate_limit_counters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier VARCHAR(255) NOT NULL,
    identifier_type VARCHAR(50) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 1,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_sys_rate_limit_window UNIQUE (identifier, identifier_type, endpoint, window_start)
);
CREATE INDEX IF NOT EXISTS idx_sys_rate_limit_identifier ON sys_rate_limit_counters (identifier, identifier_type);
CREATE INDEX IF NOT EXISTS idx_sys_rate_limit_window_end ON sys_rate_limit_counters (window_end);

-- --- Security alerting (rules + fired alerts) -------------------------------
CREATE TABLE IF NOT EXISTS sys_security_alert_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    event_type VARCHAR(100) NOT NULL,
    threshold INTEGER NOT NULL DEFAULT 1,
    time_window_minutes INTEGER NOT NULL DEFAULT 5,
    severity VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cooldown_minutes INTEGER NOT NULL DEFAULT 60,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_security_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id UUID REFERENCES sys_security_alert_rules (id) ON DELETE SET NULL,
    user_id UUID REFERENCES sys_users (id) ON DELETE SET NULL,
    triggered_by JSONB,
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_security_alerts_status ON sys_security_alerts (status);
CREATE INDEX IF NOT EXISTS idx_sys_security_alerts_severity ON sys_security_alerts (severity, created_at DESC);

INSERT INTO sys_security_alert_rules (name, description, event_type, threshold, time_window_minutes, severity, enabled)
SELECT 'Brute force login', 'Many failed logins from one IP', 'LOGIN_FAILED', 8, 5, 'HIGH', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_security_alert_rules WHERE name = 'Brute force login');

INSERT INTO sys_security_alert_rules (name, description, event_type, threshold, time_window_minutes, severity, enabled)
SELECT 'MFA failures', 'Repeated invalid MFA codes', 'MFA_FAILED', 5, 10, 'MEDIUM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_security_alert_rules WHERE name = 'MFA failures');
