-- V30: Add global_rate column to hotel_service_providers
-- Stores the provider's official classification (e.g. 3-star, 4-star hotel).
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS global_rate NUMERIC(3, 1) NOT NULL DEFAULT 0.0;
