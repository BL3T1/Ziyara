-- Outbound webhook subscriptions: external clients register URLs to receive events
CREATE TABLE webhook_subscriptions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID REFERENCES hotel_service_providers(id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    url         VARCHAR(2000) NOT NULL,
    events      JSONB         NOT NULL DEFAULT '[]',
    secret      VARCHAR(255)  NOT NULL,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ
);
CREATE INDEX idx_webhook_subs_active_events ON webhook_subscriptions(active) WHERE active = TRUE;

-- Delivery log: one row per attempted dispatch
CREATE TABLE webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID         NOT NULL REFERENCES webhook_subscriptions(id) ON DELETE CASCADE,
    event           VARCHAR(100) NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    http_status     INTEGER,
    response_body   TEXT,
    attempt_count   INTEGER      NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_webhook_deliveries_sub    ON webhook_deliveries(subscription_id);
CREATE INDEX idx_webhook_deliveries_failed ON webhook_deliveries(status) WHERE status = 'FAILED';
