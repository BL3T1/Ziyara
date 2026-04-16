-- ============================================================================
-- Discount codes - add columns for JPA entity compatibility
-- ============================================================================
ALTER TABLE discount_codes
  ADD COLUMN IF NOT EXISTS type VARCHAR(20) DEFAULT 'PERCENTAGE',
  ADD COLUMN IF NOT EXISTS value DECIMAL(12, 2),
  ADD COLUMN IF NOT EXISTS min_booking_amount DECIMAL(12, 2),
  ADD COLUMN IF NOT EXISTS max_discount_amount DECIMAL(12, 2),
  ADD COLUMN IF NOT EXISTS start_date TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS end_date TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS usage_count INTEGER DEFAULT 0;

-- Populate from existing columns
UPDATE discount_codes SET
  type = COALESCE(type, 'PERCENTAGE'),
  value = COALESCE(value, percentage),
  min_booking_amount = COALESCE(min_booking_amount, min_spend, 0),
  max_discount_amount = COALESCE(max_discount_amount, max_discount),
  start_date = COALESCE(start_date, created_at),
  end_date = COALESCE(end_date, expiry_date),
  usage_count = COALESCE(usage_count, used_count, 0);

-- Status column for Hibernate
ALTER TABLE discount_codes ALTER COLUMN status TYPE varchar(50) USING status::text;
