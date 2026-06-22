-- V65: Add account expiry date to hotel_service_providers
-- Nullable: existing rows get NULL (no expiry enforced), new rows require a value at the application layer.

ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS expiry_date DATE;

-- Partial index supports fast daily expiry-check job queries.
CREATE INDEX IF NOT EXISTS idx_hsp_expiry_date
    ON hotel_service_providers (expiry_date)
    WHERE deleted_at IS NULL;
