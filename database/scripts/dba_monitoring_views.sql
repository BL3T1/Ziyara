-- ============================================================================
-- Optional DBA monitoring views (no TDE). Run as superuser or role with
-- pg_read_all_stats. Safe to re-run (CREATE OR REPLACE).
-- ============================================================================

CREATE OR REPLACE VIEW v_ziyara_table_sizes AS
SELECT schemaname,
       relname AS table_name,
       pg_size_pretty(pg_total_relation_size(schemaname || '.' || relname::text)) AS total_size,
       pg_total_relation_size(schemaname || '.' || relname::text) AS total_bytes,
       n_live_tup AS live_rows,
       n_dead_tup AS dead_rows,
       last_vacuum,
       last_autovacuum,
       last_analyze,
       last_autoanalyze
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname || '.' || relname::text) DESC;

CREATE OR REPLACE VIEW v_ziyara_index_usage AS
SELECT schemaname,
       relname AS table_name,
       indexrelname AS index_name,
       idx_scan,
       idx_tup_read,
       idx_tup_fetch,
       pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan ASC, pg_relation_size(indexrelid) DESC;

CREATE OR REPLACE VIEW v_ziyara_connection_health AS
SELECT COUNT(*) AS total_connections,
       COUNT(*) FILTER (WHERE state = 'active') AS active,
       COUNT(*) FILTER (WHERE state = 'idle') AS idle,
       COUNT(*) FILTER (WHERE state = 'idle in transaction') AS idle_in_transaction,
       MAX(NOW() - query_start) FILTER (WHERE state <> 'idle') AS longest_running_query
FROM pg_stat_activity
WHERE datname = current_database();
