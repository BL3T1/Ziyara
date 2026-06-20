-- Matches core Flyway V6: columns expected by ServiceProviderJpaEntity (docker init does not run Flyway).
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS provider_type VARCHAR(64);
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS registration_number VARCHAR(128);
