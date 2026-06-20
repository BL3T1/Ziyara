-- Provider payout requests: providers submit withdrawal requests, ops team processes them
CREATE TABLE IF NOT EXISTS portal_payout_requests (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id      UUID        NOT NULL REFERENCES hotel_service_providers(id) ON DELETE CASCADE,
    amount           NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(10) NOT NULL DEFAULT 'USD',
    notes            TEXT,
    status           VARCHAR(32) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','PROCESSING','COMPLETED','REJECTED')),
    requested_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at     TIMESTAMPTZ,
    processed_by     UUID REFERENCES sys_users(id),
    rejection_reason TEXT
);

CREATE INDEX IF NOT EXISTS idx_portal_payout_provider ON portal_payout_requests(provider_id, status);
CREATE INDEX IF NOT EXISTS idx_portal_payout_requested_at ON portal_payout_requests(requested_at DESC);
