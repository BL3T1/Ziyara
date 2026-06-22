-- ============================================================================
-- V35 — Add deleted_at to hotel_service_providers (enables recycle-bin flow),
--        fix dummy payment method from 'CARD' → 'CREDIT_CARD'.
-- ============================================================================

-- [1] soft-delete column for providers
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_hotel_service_providers_deleted_at
    ON hotel_service_providers (deleted_at)
    WHERE deleted_at IS NOT NULL;

-- [2] fix the mobile test dummy payment: 'CARD' is not a valid PaymentMethod enum value
UPDATE pay_payments
SET method = 'CREDIT_CARD'
WHERE id = 'f0000000-0000-0000-0000-000000000005'
  AND method = 'CARD';
