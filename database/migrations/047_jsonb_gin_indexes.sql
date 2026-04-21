-- ============================================================================
-- Migration 047: JSONB GIN Indexes for Performance Optimization
-- Phase: 3.1 - Performance Optimization
-- Prerequisites: None
-- Rollback: Drop created indexes
-- ============================================================================

BEGIN;

-- Set statement timeout for safety
SET LOCAL statement_timeout = '30min';

-- ----------------------------------------------------------------------------
-- Payment Gateway Response Indexes
-- Optimizes payment debugging and reconciliation queries
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_pay_payments_gateway_response_gin
    ON pay_payments USING GIN (gateway_response jsonb_path_ops);

COMMENT ON INDEX idx_pay_payments_gateway_response_gin IS 
    'Accelerates JSONB path queries on payment gateway responses';

-- Index for specific payment status extraction
CREATE INDEX IF NOT EXISTS idx_pay_payments_gateway_status
    ON pay_payments ((gateway_response->>'status'))
    WHERE gateway_response IS NOT NULL;

-- ----------------------------------------------------------------------------
-- Hotel Service Attributes Indexes
-- Enables fast filtering by service amenities and features
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_hotel_services_attributes_gin
    ON hotel_services USING GIN (attributes jsonb_path_ops);

COMMENT ON INDEX idx_hotel_services_attributes_gin IS 
    'Supports filtering hotels by amenities (wifi, pool, parking, etc.)';

-- Expression index for common amenity queries
CREATE INDEX IF NOT EXISTS idx_hotel_services_has_wifi
    ON hotel_services (id)
    WHERE attributes->>'wifi' = 'true';

CREATE INDEX IF NOT EXISTS idx_hotel_services_has_parking
    ON hotel_services (id)
    WHERE attributes->>'parking' = 'true';

-- ----------------------------------------------------------------------------
-- Notification Metadata Indexes
-- Speeds up notification routing and filtering
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_sys_notifications_metadata_gin
    ON sys_notifications USING GIN (metadata jsonb_path_ops);

COMMENT ON INDEX idx_sys_notifications_metadata_gin IS 
    'Enables fast metadata-based notification filtering';

-- Index for campaign-based notifications
CREATE INDEX IF NOT EXISTS idx_sys_notifications_campaign_id
    ON sys_notifications ((metadata->>'campaign_id'))
    WHERE metadata IS NOT NULL AND metadata->>'campaign_id' IS NOT NULL;

-- ----------------------------------------------------------------------------
-- Audit Log Changes Indexes
-- Accelerates audit trail searches and compliance queries
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_changes_gin
    ON sys_audit_logs USING GIN (new_value jsonb_path_ops);

CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_old_value_gin
    ON sys_audit_logs USING GIN (old_value jsonb_path_ops);

COMMENT ON INDEX idx_sys_audit_logs_changes_gin IS 
    'Supports searching audit logs by changed field values';

-- Index for specific entity type + action combinations
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_entity_action
    ON sys_audit_logs (entity_type, action, created_at DESC);

-- ----------------------------------------------------------------------------
-- Discount Code Rules Indexes
-- Optimizes discount eligibility checks
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_disc_discount_codes_rules_gin
    ON disc_discount_codes USING GIN (rules jsonb_path_ops);

COMMENT ON INDEX idx_disc_discount_codes_rules_gin IS 
    'Accelerates discount rule evaluation queries';

-- Index for discount type filtering
CREATE INDEX IF NOT EXISTS idx_disc_discount_codes_discount_type
    ON disc_discount_codes ((rules->>'discount_type'))
    WHERE rules IS NOT NULL;

-- ----------------------------------------------------------------------------
-- Customer Preferences Indexes
-- Enables personalized service recommendations
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_customers_preferences_gin
    ON customers USING GIN (preferences jsonb_path_ops);

COMMENT ON INDEX idx_customers_preferences_gin IS 
    'Supports customer preference-based queries';

-- Index for language preference
CREATE INDEX IF NOT EXISTS idx_customers_preferred_language
    ON customers ((preferences->>'language'))
    WHERE preferences IS NOT NULL AND preferences->>'language' IS NOT NULL;

-- ----------------------------------------------------------------------------
-- Taxi Booking Route Data Indexes
-- Optimizes route-based queries and analytics
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_bkg_taxi_bookings_route_gin
    ON bkg_taxi_bookings USING GIN (route_data jsonb_path_ops);

COMMENT ON INDEX idx_bkg_taxi_bookings_route_gin IS 
    'Accelerates taxi route analysis queries';

-- ----------------------------------------------------------------------------
-- Verification Queries
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_index_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_index_count
    FROM pg_indexes
    WHERE indexname IN (
        'idx_pay_payments_gateway_response_gin',
        'idx_hotel_services_attributes_gin',
        'idx_sys_notifications_metadata_gin',
        'idx_sys_audit_logs_changes_gin',
        'idx_disc_discount_codes_rules_gin',
        'idx_customers_preferences_gin',
        'idx_bkg_taxi_bookings_route_gin'
    );
    
    RAISE NOTICE 'Created % JSONB GIN indexes', v_index_count;
    
    IF v_index_count < 7 THEN
        RAISE WARNING 'Some JSONB indexes may not have been created';
    END IF;
END $$;

-- Display index sizes
SELECT 
    indexname,
    pg_size_pretty(pg_relation_size(indexname::regclass)) AS index_size
FROM pg_indexes
WHERE indexname LIKE 'idx_%_gin'
ORDER BY pg_relation_size(indexname::regclass) DESC;

COMMIT;

-- ============================================================================
-- ROLLBACK SCRIPT
-- Execute this section to rollback migration 047
-- ============================================================================
/*
BEGIN;

DROP INDEX IF EXISTS idx_pay_payments_gateway_response_gin;
DROP INDEX IF EXISTS idx_pay_payments_gateway_status;
DROP INDEX IF EXISTS idx_hotel_services_attributes_gin;
DROP INDEX IF EXISTS idx_hotel_services_has_wifi;
DROP INDEX IF EXISTS idx_hotel_services_has_parking;
DROP INDEX IF EXISTS idx_sys_notifications_metadata_gin;
DROP INDEX IF EXISTS idx_sys_notifications_campaign_id;
DROP INDEX IF EXISTS idx_sys_audit_logs_changes_gin;
DROP INDEX IF EXISTS idx_sys_audit_logs_old_value_gin;
DROP INDEX IF EXISTS idx_sys_audit_logs_entity_action;
DROP INDEX IF EXISTS idx_disc_discount_codes_rules_gin;
DROP INDEX IF EXISTS idx_disc_discount_codes_discount_type;
DROP INDEX IF EXISTS idx_customers_preferences_gin;
DROP INDEX IF EXISTS idx_customers_preferred_language;
DROP INDEX IF EXISTS idx_bkg_taxi_bookings_route_gin;

COMMIT;
*/
