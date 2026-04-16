-- ============================================================================
-- Employees level + Payments status/method for Hibernate
-- ============================================================================
ALTER TABLE employees ALTER COLUMN level TYPE varchar(50) USING level::text;
ALTER TABLE payments ALTER COLUMN status TYPE varchar(50) USING status::text;
ALTER TABLE payments ALTER COLUMN method TYPE varchar(50) USING method::text;

-- Add columns PaymentJPA expects (schema has gateway_response, gateway_transaction_id)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_name VARCHAR(100);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_token VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) UNIQUE;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS error_message TEXT;
