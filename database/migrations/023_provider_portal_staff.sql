-- Additional provider portal users linked to the same organization (beyond the primary owner in hotel_service_providers.created_by).
-- Run after 022. Idempotent.

CREATE TABLE IF NOT EXISTS hotel_provider_staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES hotel_service_providers(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES sys_users(id) ON DELETE CASCADE,
    title VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hotel_provider_staff_provider_user UNIQUE (provider_id, user_id),
    CONSTRAINT uk_hotel_provider_staff_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_hotel_provider_staff_provider_id ON hotel_provider_staff(provider_id);

COMMENT ON TABLE hotel_provider_staff IS 'Provider portal team members linked to a service provider org';
