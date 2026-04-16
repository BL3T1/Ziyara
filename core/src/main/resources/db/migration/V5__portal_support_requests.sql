-- Aligns with database/migrations/025_portal_support_requests.sql (Flyway for non-Docker DBs).

CREATE TABLE IF NOT EXISTS hotel_portal_support_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES hotel_service_providers(id) ON DELETE CASCADE,
    user_id UUID NULL REFERENCES sys_users(id) ON DELETE SET NULL,
    subject VARCHAR(500) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hotel_portal_support_provider_created
    ON hotel_portal_support_requests (provider_id, created_at DESC);
