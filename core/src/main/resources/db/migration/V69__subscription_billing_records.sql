-- G7: Billing event ledger for subscription activations and renewals.
-- Separate from pay_payments (which requires a booking_id FK) to support non-booking payment events.
CREATE TABLE IF NOT EXISTS sub_billing_records (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID        NOT NULL,
    provider_id     UUID        NOT NULL REFERENCES hotel_service_providers (id) ON DELETE CASCADE,
    plan_code       VARCHAR(50) NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    currency        CHAR(3)     NOT NULL DEFAULT 'USD',
    billing_cycle   VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    status          VARCHAR(30) NOT NULL DEFAULT 'RECORDED',
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sub_billing_provider  ON sub_billing_records (provider_id);
CREATE INDEX IF NOT EXISTS idx_sub_billing_sub_id    ON sub_billing_records (subscription_id);
