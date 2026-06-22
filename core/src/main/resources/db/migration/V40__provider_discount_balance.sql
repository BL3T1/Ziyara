-- Provider discount balance: tracks admin-allocated budget vs provider-spent amount
CREATE TABLE provider_discount_balance (
    provider_id      UUID PRIMARY KEY REFERENCES hotel_service_providers(id) ON DELETE CASCADE,
    currency         VARCHAR(3)     NOT NULL DEFAULT 'USD',
    allocated_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    spent_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_prov_disc_bal_non_negative CHECK (allocated_amount >= 0 AND spent_amount >= 0),
    CONSTRAINT chk_prov_disc_bal_spent_lte_allocated CHECK (spent_amount <= allocated_amount)
);

-- Debit ledger: one row per discount code that consumed balance
CREATE TABLE provider_discount_debits (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id      UUID           NOT NULL REFERENCES hotel_service_providers(id) ON DELETE CASCADE,
    discount_code_id UUID           REFERENCES disc_discount_codes(id) ON DELETE SET NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    description      VARCHAR(255),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_prov_disc_debits_provider ON provider_discount_debits(provider_id);
