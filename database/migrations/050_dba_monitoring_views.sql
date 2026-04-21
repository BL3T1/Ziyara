-- ============================================================================
-- Migration 050: DBA Monitoring Views and Database Health Dashboard
-- Phase: 4.2 - Compliance & Monitoring
-- Prerequisites: None
-- Rollback: Drop created views and functions
-- ============================================================================

BEGIN;

-- Set statement timeout for safety
SET LOCAL statement_timeout = '30min';

-- ----------------------------------------------------------------------------
-- Table Growth and Storage Monitoring
-- Tracks table sizes, row counts, and bloat indicators
-- ----------------------------------------------------------------------------
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
    END AS dead_ratio_percent,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;

COMMENT ON VIEW dba_table_growth IS 
    'Monitors table storage, row counts, vacuum status, and bloat indicators';

-- ----------------------------------------------------------------------------
-- Index Usage Statistics
-- Identifies unused indexes and optimization opportunities
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW dba_index_usage AS
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched,
    CASE 
        WHEN idx_scan = 0 AND idx_tup_read = 0 THEN 'UNUSED'
        WHEN idx_scan < 100 THEN 'LOW_USAGE'
        ELSE 'ACTIVE'
    END AS usage_status,
    indexdef
FROM pg_stat_user_indexes
JOIN pg_indexes USING (schemaname, tablename, indexname)
ORDER BY idx_scan ASC;

COMMENT ON VIEW dba_index_usage IS 
    'Tracks index usage patterns to identify unused or underutilized indexes';

-- ----------------------------------------------------------------------------
-- Active Queries Monitoring
-- Real-time view of running queries with duration tracking
-- ----------------------------------------------------------------------------
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
    query_start,
    backend_start,
    xact_start,
    CASE 
        WHEN EXTRACT(EPOCH FROM (NOW() - query_start)) > 300 THEN 'LONG_RUNNING'
        WHEN EXTRACT(EPOCH FROM (NOW() - query_start)) > 60 THEN 'MODERATE'
        ELSE 'NORMAL'
    END AS duration_category
FROM pg_stat_activity
WHERE state != 'idle'
  AND query NOT ILIKE '%pg_stat_activity%'
  AND backend_type = 'client backend'
ORDER BY query_start ASC;

COMMENT ON VIEW dba_active_queries IS 
    'Real-time monitoring of active queries with duration categorization';

-- ----------------------------------------------------------------------------
-- Lock Monitoring and Blocking Detection
-- Identifies blocking chains and deadlock risks
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW dba_locks AS
SELECT 
    blocked_locks.pid AS blocked_pid,
    blocked_activity.usename AS blocked_user,
    blocked_activity.application_name AS blocked_application,
    blocking_locks.pid AS blocking_pid,
    blocking_activity.usename AS blocking_user,
    blocking_activity.application_name AS blocking_application,
    blocked_activity.query AS blocked_query,
    blocking_activity.query AS blocking_query,
    blocked_activity.state AS blocked_state,
    EXTRACT(EPOCH FROM (NOW() - blocked_activity.query_start))::INTEGER AS blocked_duration_seconds,
    blocked_locks.locktype,
    blocked_locks.mode,
    blocked_locks.GRANTED
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity 
    ON blocked_activity.pid = blocked_locks.pid
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
JOIN pg_catalog.pg_stat_activity blocking_activity 
    ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;

COMMENT ON VIEW dba_locks IS 
    'Detects blocking chains showing which queries are blocked by which';

-- ----------------------------------------------------------------------------
-- Connection Pool Monitoring
-- Tracks connection usage and limits
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW dba_connection_stats AS
SELECT 
    count(*) FILTER (WHERE state = 'active') AS active_connections,
    count(*) FILTER (WHERE state = 'idle') AS idle_connections,
    count(*) FILTER (WHERE state = 'idle in transaction') AS idle_in_transaction,
    count(*) FILTER (WHERE state = 'fastpath function call') AS fastpath_calls,
    count(*) FILTER (WHERE backend_type = 'background worker') AS background_workers,
    count(*) AS total_connections,
    current_setting('max_connections')::int AS max_connections,
    current_setting('max_connections')::int - count(*) AS available_connections,
    round(100.0 * count(*) / current_setting('max_connections')::int, 2) AS utilization_percent,
    count(*) FILTER (WHERE state = 'idle in transaction' 
        AND query_start < NOW() - INTERVAL '5 minutes') AS long_idle_transactions
FROM pg_stat_activity
WHERE backend_type = 'client backend';

COMMENT ON VIEW dba_connection_stats IS 
    'Monitors connection pool utilization and identifies connection leaks';

-- ----------------------------------------------------------------------------
-- Cache Hit Ratio Monitoring
-- Critical performance metric for buffer efficiency
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW dba_cache_health AS
SELECT 
    schemaname,
    tablename,
    heap_blks_read AS heap_blocks_read,
    heap_blks_hit AS heap_blocks_hit,
    CASE 
        WHEN heap_blks_read + heap_blks_hit > 0 
        THEN round(100.0 * heap_blks_hit / (heap_blks_read + heap_blks_hit), 2)
        ELSE NULL
    END AS cache_hit_ratio_percent,
    idx_blks_read AS index_blocks_read,
    idx_blks_hit AS index_blocks_hit,
    CASE 
        WHEN idx_blks_read + idx_blks_hit > 0 
        THEN round(100.0 * idx_blks_hit / (idx_blks_read + idx_blks_hit), 2)
        ELSE NULL
    END AS index_cache_hit_ratio_percent,
    toast_blks_read,
    toast_blks_hit,
    tidx_blks_read,
    tidx_blks_hit
FROM pg_statio_user_tables
ORDER BY 
    CASE 
        WHEN heap_blks_read + heap_blks_hit > 0 
        THEN 100.0 * heap_blks_hit / (heap_blks_read + heap_blks_hit)
        ELSE 0
    END ASC;

COMMENT ON VIEW dba_cache_health IS 
    'Monitors buffer cache hit ratios - should be >99% for optimal performance';

-- ----------------------------------------------------------------------------
-- Long-term Statistics Aggregation
-- Historical trends for capacity planning
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dba_statistics_history (
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    snapshot_type VARCHAR(50) NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_value NUMERIC,
    metric_text TEXT,
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_dba_statistics_history_time 
    ON dba_statistics_history (recorded_at DESC, snapshot_type, metric_name);

CREATE INDEX IF NOT EXISTS idx_dba_statistics_history_metric 
    ON dba_statistics_history (snapshot_type, metric_name, recorded_at DESC);

COMMENT ON TABLE dba_statistics_history IS 
    'Historical storage of database statistics for trend analysis';

-- ----------------------------------------------------------------------------
-- Function to Capture Statistics Snapshot
-- Scheduled execution for historical tracking
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION capture_dba_statistics()
RETURNS VOID AS $$
DECLARE
    v_conn_stats RECORD;
    v_cache_stats RECORD;
BEGIN
    -- Capture connection stats
    SELECT * INTO v_conn_stats FROM dba_connection_stats;
    
    INSERT INTO dba_statistics_history (
        snapshot_type, metric_name, metric_value, metadata
    ) VALUES 
    ('connections', 'active', (SELECT active_connections FROM dba_connection_stats), NULL),
    ('connections', 'idle', (SELECT idle_connections FROM dba_connection_stats), NULL),
    ('connections', 'idle_in_transaction', (SELECT idle_in_transaction FROM dba_connection_stats), NULL),
    ('connections', 'utilization_pct', (SELECT utilization_percent FROM dba_connection_stats), NULL),
    ('connections', 'long_idle_transactions', (SELECT long_idle_transactions FROM dba_connection_stats), NULL);
    
    -- Capture overall cache hit ratio
    INSERT INTO dba_statistics_history (
        snapshot_type, metric_name, metric_value
    )
    SELECT 
        'cache',
        'overall_hit_ratio',
        round(100.0 * sum(heap_blks_hit) / NULLIF(sum(heap_blks_hit) + sum(heap_blks_read), 0), 2)
    FROM pg_statio_user_tables;
    
    -- Capture table count and total size
    INSERT INTO dba_statistics_history (
        snapshot_type, metric_name, metric_value
    )
    SELECT 
        'storage',
        'total_database_size_bytes',
        pg_database_size(current_database());
    
    INSERT INTO dba_statistics_history (
        snapshot_type, metric_name, metric_value
    )
    SELECT 
        'storage',
        'table_count',
        count(*)
    FROM pg_tables
    WHERE schemaname = 'public';
    
    RAISE NOTICE 'DBA statistics snapshot captured at %', CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION capture_dba_statistics() IS 
    'Captures key database metrics for historical trend analysis';

-- ----------------------------------------------------------------------------
-- Schedule Statistics Collection via pg_cron
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        -- Remove existing schedule if any
        PERFORM cron.unschedule('dba-stats-hourly');
        
        -- Run every hour
        PERFORM cron.schedule(
            'dba-stats-hourly',
            '0 * * * *',
            'SELECT capture_dba_statistics()'
        );
        
        RAISE NOTICE 'Scheduled hourly DBA statistics collection';
    ELSE
        RAISE NOTICE 'pg_cron not available - use application scheduler for capture_dba_statistics()';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- Database Health Summary View
-- Single-pane overview for dashboards
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW dba_health_summary AS
SELECT 
    current_database() AS database_name,
    current_setting('server_version') AS postgres_version,
    pg_postmaster_start_time() AS server_start_time,
    EXTRACT(EPOCH FROM (NOW() - pg_postmaster_start_time())) / 86400 AS uptime_days,
    (SELECT active_connections FROM dba_connection_stats) AS active_connections,
    (SELECT utilization_percent FROM dba_connection_stats) AS connection_utilization_pct,
    (SELECT count(*) FROM dba_active_queries WHERE duration_category = 'LONG_RUNNING') AS long_running_queries,
    (SELECT count(*) FROM dba_locks) AS blocked_queries,
    (SELECT count(*) FROM sys_data_retention_errors WHERE resolved = FALSE) AS unresolved_retention_errors,
    pg_size_pretty(pg_database_size(current_database())) AS database_size,
    (SELECT count(*) FROM pg_matviews WHERE schemaname = 'public') AS materialized_views_count,
    (SELECT count(*) FROM pg_tables WHERE schemaname = 'public') AS tables_count,
    (SELECT count(*) FROM pg_indexes WHERE schemaname = 'public') AS indexes_count;

COMMENT ON VIEW dba_health_summary IS 
    'Single-pane health dashboard for database monitoring';

-- ----------------------------------------------------------------------------
-- Alert Thresholds Configuration
-- Configurable thresholds for automated alerting
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS dba_alert_thresholds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_name VARCHAR(100) NOT NULL UNIQUE,
    warning_threshold NUMERIC,
    critical_threshold NUMERIC,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    alert_channel VARCHAR(50) DEFAULT 'email',
    description TEXT
);

-- Insert default thresholds
INSERT INTO dba_alert_thresholds (metric_name, warning_threshold, critical_threshold, description) VALUES
('connection_utilization_pct', 70, 90, 'Connection pool utilization percentage'),
('cache_hit_ratio', 95, 90, 'Buffer cache hit ratio percentage'),
('long_running_queries', 5, 20, 'Number of queries running > 5 minutes'),
('blocked_queries', 3, 10, 'Number of blocked queries'),
('dead_ratio_percent', 20, 50, 'Table bloat - dead tuple ratio'),
('idle_in_transaction', 5, 20, 'Idle transactions count')
ON CONFLICT (metric_name) DO NOTHING;

COMMENT ON TABLE dba_alert_thresholds IS 
    'Configurable thresholds for database health alerting';

-- ----------------------------------------------------------------------------
-- Verification Queries
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_view_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_view_count
    FROM pg_views
    WHERE schemaname = 'public'
      AND viewname LIKE 'dba_%';
    
    RAISE NOTICE 'Created % DBA monitoring views', v_view_count;
    
    IF v_view_count < 6 THEN
        RAISE WARNING 'Expected at least 6 DBA views, found %', v_view_count;
    END IF;
END $$;

-- Display all DBA views
SELECT 
    schemaname,
    viewname,
    pg_get_viewdef(schemaname || '.' || viewname, true) AS view_definition
FROM pg_views
WHERE schemaname = 'public'
  AND viewname LIKE 'dba_%'
ORDER BY viewname;

COMMIT;

-- ============================================================================
-- ROLLBACK SCRIPT
-- Execute this section to rollback migration 050
-- ============================================================================
/*
BEGIN;

-- Unschedule cron jobs
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_cron') THEN
        PERFORM cron.unschedule('dba-stats-hourly');
    END IF;
END $$;

DROP VIEW IF EXISTS dba_health_summary CASCADE;
DROP VIEW IF EXISTS dba_connection_stats CASCADE;
DROP VIEW IF EXISTS dba_cache_health CASCADE;
DROP VIEW IF EXISTS dba_active_queries CASCADE;
DROP VIEW IF EXISTS dba_locks CASCADE;
DROP VIEW IF EXISTS dba_index_usage CASCADE;
DROP VIEW IF EXISTS dba_table_growth CASCADE;

DROP FUNCTION IF EXISTS capture_dba_statistics();

DROP TABLE IF EXISTS dba_alert_thresholds CASCADE;
DROP TABLE IF EXISTS dba_statistics_history CASCADE;

COMMIT;
*/
