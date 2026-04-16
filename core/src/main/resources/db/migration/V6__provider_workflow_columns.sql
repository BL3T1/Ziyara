-- Provider onboarding workflow: type/registration on hotel_service_providers, approval audit columns.
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS provider_type VARCHAR(64);
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS registration_number VARCHAR(128);
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS approved_by UUID REFERENCES users(id);
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;
