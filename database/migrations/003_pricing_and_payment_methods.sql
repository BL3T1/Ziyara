-- ============================================================================
-- PRICING_METHODS & PAYMENT_METHODS (plans) - Schema extensions
-- Run after main schema and prior migrations
-- ============================================================================

-- Provider-level commission override (default 10% applied in app)
ALTER TABLE service_providers
ADD COLUMN IF NOT EXISTS commission_rate NUMERIC(5,2) DEFAULT NULL;
COMMENT ON COLUMN service_providers.commission_rate IS 'Override commission % for this provider; NULL = use platform default 10%';

-- Service pricing: seasonal multiplier and tax (inclusive pricing)
ALTER TABLE services
ADD COLUMN IF NOT EXISTS seasonal_multiplier NUMERIC(5,2) NOT NULL DEFAULT 1.00,
ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5,4) NOT NULL DEFAULT 0;
COMMENT ON COLUMN services.seasonal_multiplier IS 'Multiplier for peak/season (e.g. 1.2 = +20%)';
COMMENT ON COLUMN services.tax_rate IS 'Tax rate applied (e.g. 0.10 = 10%); inclusive in customer price';

-- Payment idempotency (prevent duplicate charges)
ALTER TABLE payments
ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) UNIQUE;
COMMENT ON COLUMN payments.idempotency_key IS 'Client idempotency key to prevent duplicate payment submissions';

CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_idempotency_key
ON payments(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Payment method enum: add CASH_ON_SERVICE (PAYMENT_METHODS.md). Run once; ignore error if value exists.
-- Add CASH_ON_SERVICE (PAYMENT_METHODS.md). Omit if already present.
ALTER TYPE payment_method_enum ADD VALUE 'CASH_ON_SERVICE';
