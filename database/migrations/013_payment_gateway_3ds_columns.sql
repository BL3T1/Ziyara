-- ============================================================================
-- PAYMENT_METHODS – gateway reference, 3DS status, gateway response (Phase 1)
-- Run after: 012. Backward compatible; additive only.
-- ============================================================================

-- External transaction ID from gateway (for idempotency and reconciliation)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_reference VARCHAR(255);
COMMENT ON COLUMN payments.gateway_reference IS 'External gateway transaction ID (e.g. for 3DS callback and webhook idempotency)';

-- 3DS status: AUTHENTICATED, NOT_REQUIRED, FAILED, etc.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS three_ds_status VARCHAR(50);
COMMENT ON COLUMN payments.three_ds_status IS '3DS authentication status from gateway';

-- Full gateway response for debugging (optional)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_response TEXT;
COMMENT ON COLUMN payments.gateway_response IS 'Full gateway response payload for debugging (optional)';

CREATE INDEX IF NOT EXISTS idx_payments_gateway_reference ON payments(gateway_reference) WHERE gateway_reference IS NOT NULL;
