CREATE TABLE IF NOT EXISTS provider_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL UNIQUE,
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    staff_limit INT NOT NULL DEFAULT 10,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO provider_subscriptions (provider_id, plan, staff_limit)
SELECT id, 'FREE', 10
FROM hotel_service_providers
ON CONFLICT (provider_id) DO NOTHING;
