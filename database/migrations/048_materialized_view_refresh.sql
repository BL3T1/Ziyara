-- ============================================================================
-- Migration 048: Materialized View Management and Refresh Strategy
-- Phase: 3.3 - Performance Optimization
-- Prerequisites: Migration 034 (audit log partitioning)
-- Rollback: Drop created materialized views and functions
-- ============================================================================

BEGIN;

-- Set statement timeout for safety
SET LOCAL statement_timeout = '30min';

-- ----------------------------------------------------------------------------
-- Daily Booking Totals by Service Type
-- Provides quick access to booking metrics without scanning full table
-- ----------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS mv_bkg_daily_totals_by_type CASCADE;

CREATE MATERIALIZED VIEW mv_bkg_daily_totals_by_type AS
SELECT 
    (created_at AT TIME ZONE 'UTC')::date AS booking_date,
    service_id,
    COUNT(*) AS booking_count,
    SUM(total_amount) FILTER (WHERE status NOT IN ('CANCELLED', 'EXPIRED')) AS revenue,
    AVG(total_amount) FILTER (WHERE status NOT IN ('CANCELLED', 'EXPIRED')) AS avg_booking_value,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_bookings,
    COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_bookings
FROM bkg_bookings
GROUP BY 1, 2;

CREATE UNIQUE INDEX idx_mv_bkg_daily_totals_pk 
    ON mv_bkg_daily_totals_by_type (booking_date, service_id);

CREATE INDEX idx_mv_bkg_daily_totals_service 
    ON mv_bkg_daily_totals_by_type (service_id);

COMMENT ON MATERIALIZED VIEW mv_bkg_daily_totals_by_type IS 
    'Daily aggregated booking statistics by service for reporting dashboards';

-- ----------------------------------------------------------------------------
-- Provider Performance Summary
-- Aggregates provider KPIs for partner management and analytics
-- ----------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS mv_provider_performance CASCADE;

CREATE MATERIALIZED VIEW mv_provider_performance AS
SELECT 
    sp.id AS provider_id,
    sp.company_name,
    sp.status AS provider_status,
    COUNT(DISTINCT hs.id) AS total_services,
    COUNT(DISTINCT b.id) AS total_bookings,
    COUNT(DISTINCT b.id) FILTER (WHERE b.status = 'COMPLETED') AS completed_bookings,
    COUNT(DISTINCT b.id) FILTER (WHERE b.status IN ('PENDING', 'CONFIRMED')) AS pending_bookings,
    SUM(b.total_amount) FILTER (WHERE b.status = 'COMPLETED') AS total_revenue,
    AVG(b.total_amount) FILTER (WHERE b.status = 'COMPLETED') AS avg_booking_value,
    AVG(r.rating) FILTER (WHERE r.status = 'APPROVED') AS avg_rating,
    COUNT(r.id) FILTER (WHERE r.status = 'APPROVED') AS review_count,
    COUNT(r.id) FILTER (WHERE r.status = 'PENDING') AS pending_reviews,
    MAX(b.created_at) AS last_booking_date
FROM hotel_service_providers sp
LEFT JOIN hotel_services hs ON hs.provider_id = sp.id
LEFT JOIN bkg_bookings b ON b.service_id = hs.id
LEFT JOIN hotel_reviews r ON r.service_id = hs.id
GROUP BY sp.id, sp.company_name, sp.status;

CREATE UNIQUE INDEX idx_mv_provider_performance_pk 
    ON mv_provider_performance (provider_id);

CREATE INDEX idx_mv_provider_performance_status 
    ON mv_provider_performance (provider_status);

CREATE INDEX idx_mv_provider_performance_revenue 
    ON mv_provider_performance (total_revenue DESC NULLS LAST);

COMMENT ON MATERIALIZED VIEW mv_provider_performance IS 
    'Comprehensive provider performance metrics for partner management';

-- ----------------------------------------------------------------------------
-- Payment Analytics Summary
-- Tracks payment success rates, refund ratios, and gateway performance
-- ----------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS mv_payment_analytics CASCADE;

CREATE MATERIALIZED VIEW mv_payment_analytics AS
SELECT 
    DATE_TRUNC('day', created_at) AS payment_date,
    payment_method,
    status,
    gateway_used,
    COUNT(*) AS transaction_count,
    SUM(amount) AS total_amount,
    AVG(amount) AS avg_transaction_value,
    COUNT(*) FILTER (WHERE status = 'COMPLETED') AS successful_transactions,
    COUNT(*) FILTER (WHERE status IN ('FAILED', 'DECLINED')) AS failed_transactions,
    ROUND(
        100.0 * COUNT(*) FILTER (WHERE status = 'COMPLETED') / 
        NULLIF(COUNT(*), 0), 
        2
    ) AS success_rate_pct
FROM pay_payments
GROUP BY 1, 2, 3, 4;

CREATE UNIQUE INDEX idx_mv_payment_analytics_pk 
    ON mv_payment_analytics (payment_date, payment_method, status, gateway_used);

CREATE INDEX idx_mv_payment_analytics_date 
    ON mv_payment_analytics (payment_date DESC);

COMMENT ON MATERIALIZED VIEW mv_payment_analytics IS 
    'Daily payment analytics by method, status, and gateway';

-- ----------------------------------------------------------------------------
-- Customer Activity Summary
-- Identifies active customers and engagement patterns
-- ----------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS mv_customer_activity CASCADE;

CREATE MATERIALIZED VIEW mv_customer_activity AS
SELECT 
    c.id AS customer_id,
    c.user_id,
    COUNT(DISTINCT b.id) AS total_bookings,
    COUNT(DISTINCT b.id) FILTER (WHERE b.created_at >= CURRENT_DATE - INTERVAL '30 days') AS bookings_last_30d,
    COUNT(DISTINCT b.id) FILTER (WHERE b.created_at >= CURRENT_DATE - INTERVAL '90 days') AS bookings_last_90d,
    SUM(b.total_amount) FILTER (WHERE b.status = 'COMPLETED') AS lifetime_value,
    SUM(b.total_amount) FILTER (WHERE b.created_at >= CURRENT_DATE - INTERVAL '90 days' AND b.status = 'COMPLETED') AS revenue_last_90d,
    MAX(b.created_at) AS last_booking_date,
    MIN(b.created_at) AS first_booking_date,
    COUNT(DISTINCT hs.provider_id) AS unique_providers_booked,
    COUNT(DISTINCT b.service_id) AS unique_services_booked
FROM customers c
LEFT JOIN bkg_bookings b ON b.customer_id = c.id
LEFT JOIN hotel_services hs ON hs.id = b.service_id
GROUP BY c.id, c.user_id;

CREATE UNIQUE INDEX idx_mv_customer_activity_pk 
    ON mv_customer_activity (customer_id);

CREATE INDEX idx_mv_customer_activity_last_booking 
    ON mv_customer_activity (last_booking_date DESC NULLS LAST);

CREATE INDEX idx_mv_customer_activity_ltv 
    ON mv_customer_activity (lifetime_value DESC NULLS LAST);

COMMENT ON MATERIALIZED VIEW mv_customer_activity IS 
    'Customer engagement and lifetime value metrics';

-- ----------------------------------------------------------------------------
-- Review Sentiment Analysis
-- Aggregates review ratings and identifies trends
-- ----------------------------------------------------------------------------
DROP MATERIALIZED VIEW IF EXISTS mv_review_sentiment CASCADE;

CREATE MATERIALIZED VIEW mv_review_sentiment AS
SELECT 
    hs.provider_id,
    sp.company_name AS provider_name,
    hs.id AS service_id,
    hs.name_en AS service_name,
    COUNT(r.id) AS total_reviews,
    ROUND(AVG(r.rating)::numeric, 2) AS avg_rating,
    COUNT(r.id) FILTER (WHERE r.rating >= 4) AS positive_reviews,
    COUNT(r.id) FILTER (WHERE r.rating = 3) AS neutral_reviews,
    COUNT(r.id) FILTER (WHERE r.rating <= 2) AS negative_reviews,
    ROUND(
        100.0 * COUNT(r.id) FILTER (WHERE r.rating >= 4) / 
        NULLIF(COUNT(r.id), 0), 
        2
    ) AS positive_pct,
    MAX(r.created_at) AS last_review_date,
    MIN(r.created_at) AS first_review_date
FROM hotel_reviews r
JOIN hotel_services hs ON hs.id = r.service_id
JOIN hotel_service_providers sp ON sp.id = hs.provider_id
WHERE r.status = 'APPROVED'
GROUP BY hs.provider_id, sp.company_name, hs.id, hs.name_en;

CREATE UNIQUE INDEX idx_mv_review_sentiment_pk 
    ON mv_review_sentiment (service_id);

CREATE INDEX idx_mv_review_sentiment_provider 
    ON mv_review_sentiment (provider_id);

CREATE INDEX idx_mv_review_sentiment_rating 
    ON mv_review_sentiment (avg_rating DESC);

COMMENT ON MATERIALIZED VIEW mv_review_sentiment IS 
    'Review sentiment analysis and rating trends per service';

-- ----------------------------------------------------------------------------
-- Function to Refresh All Materialized Views
-- Returns refresh statistics for monitoring
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION refresh_reporting_views(
    p_view_pattern TEXT DEFAULT '%'
)
RETURNS TABLE(
    view_name TEXT, 
    refresh_duration INTERVAL, 
    rows_count BIGINT,
    refresh_timestamp TIMESTAMPTZ
) AS $$
DECLARE
    v_start TIMESTAMPTZ;
    v_end TIMESTAMPTZ;
    v_rows BIGINT;
    v_view RECORD;
BEGIN
    FOR v_view IN 
        SELECT matviewname
        FROM pg_matviews
        WHERE schemaname = 'public'
          AND matviewname LIKE p_view_pattern
          AND matviewname LIKE 'mv_%'
        ORDER BY matviewname
    LOOP
        v_start := clock_timestamp();
        
        EXECUTE format('REFRESH MATERIALIZED VIEW CONCURRENTLY %I', v_view.matviewname);
        
        v_end := clock_timestamp();
        
        EXECUTE format('SELECT COUNT(*) FROM %I', v_view.matviewname) INTO v_rows;
        
        view_name := v_view.matviewname;
        refresh_duration := v_end - v_start;
        rows_count := v_rows;
        refresh_timestamp := v_end;
        
        RETURN NEXT;
        
        RAISE NOTICE 'Refreshed %: % rows in %', 
            v_view.matviewname, v_rows, refresh_duration;
    END LOOP;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION refresh_reporting_views(TEXT) IS 
    'Refreshes materialized views matching pattern with concurrent refresh';

-- ----------------------------------------------------------------------------
-- Scheduled Refresh Function (for cron/pg_cron integration)
-- Automatically refreshes views based on time of day
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION scheduled_view_refresh()
RETURNS VOID AS $$
DECLARE
    v_hour INTEGER;
BEGIN
    v_hour := EXTRACT(HOUR FROM CURRENT_TIME);
    
    -- High-frequency views (every hour during business hours)
    IF v_hour BETWEEN 6 AND 22 THEN
        PERFORM refresh_reporting_views('mv_payment_analytics');
        PERFORM refresh_reporting_views('mv_bkg_daily_totals_by_type');
    END IF;
    
    -- Medium-frequency views (every 4 hours)
    IF v_hour % 4 = 0 THEN
        PERFORM refresh_reporting_views('mv_customer_activity');
        PERFORM refresh_reporting_views('mv_review_sentiment');
    END IF;
    
    -- Low-frequency views (once daily at 2 AM)
    IF v_hour = 2 AND EXTRACT(MINUTE FROM CURRENT_TIME) < 5 THEN
        PERFORM refresh_reporting_views('mv_provider_performance');
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION scheduled_view_refresh() IS 
    'Time-based scheduled refresh for materialized views';

-- ----------------------------------------------------------------------------
-- Verification Queries
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_view_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_view_count
    FROM pg_matviews
    WHERE schemaname = 'public'
      AND matviewname LIKE 'mv_%';
    
    RAISE NOTICE 'Created/refreshed % materialized views', v_view_count;
    
    IF v_view_count < 5 THEN
        RAISE WARNING 'Expected at least 5 materialized views, found %', v_view_count;
    END IF;
END $$;

-- Display materialized view sizes
SELECT 
    matviewname,
    pg_size_pretty(pg_total_relation_size(matviewname::regclass)) AS total_size,
    pg_size_pretty(pg_relation_size(matviewname::regclass)) AS data_size
FROM pg_matviews
WHERE schemaname = 'public'
  AND matviewname LIKE 'mv_%'
ORDER BY pg_total_relation_size(matviewname::regclass) DESC;

COMMIT;

-- ============================================================================
-- ROLLBACK SCRIPT
-- Execute this section to rollback migration 048
-- ============================================================================
/*
BEGIN;

DROP FUNCTION IF EXISTS scheduled_view_refresh();
DROP FUNCTION IF EXISTS refresh_reporting_views(TEXT);

DROP MATERIALIZED VIEW IF EXISTS mv_review_sentiment CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_customer_activity CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_payment_analytics CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_provider_performance CASCADE;
DROP MATERIALIZED VIEW IF EXISTS mv_bkg_daily_totals_by_type CASCADE;

COMMIT;
*/
