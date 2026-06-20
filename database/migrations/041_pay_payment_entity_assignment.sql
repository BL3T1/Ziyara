-- Payment entity assignment: link payments to bookings, providers, or other domain entities
ALTER TABLE pay_payments
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS entity_id   UUID,
    ADD COLUMN IF NOT EXISTS category    VARCHAR(50);

COMMENT ON COLUMN pay_payments.entity_type IS 'Domain type owning this payment: BOOKING | PROVIDER | CUSTOMER';
COMMENT ON COLUMN pay_payments.entity_id   IS 'UUID of the owning entity';
COMMENT ON COLUMN pay_payments.category    IS 'Payment purpose: SERVICE_FEE | COMMISSION | REFUND | PENALTY';

CREATE INDEX IF NOT EXISTS idx_pay_payments_entity
    ON pay_payments (entity_type, entity_id);

-- Payment history audit table
CREATE TABLE IF NOT EXISTS pay_payment_history (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id    UUID NOT NULL REFERENCES pay_payments(id) ON DELETE CASCADE,
    status_from   VARCHAR(30),
    status_to     VARCHAR(30) NOT NULL,
    changed_by    UUID REFERENCES sys_users(id) ON DELETE SET NULL,
    changed_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    note          TEXT
);

CREATE INDEX IF NOT EXISTS idx_pay_payment_history_payment
    ON pay_payment_history (payment_id, changed_at DESC);

COMMENT ON TABLE pay_payment_history IS 'Immutable status-change log for every payment transition';
