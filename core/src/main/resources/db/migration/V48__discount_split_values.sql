-- When sponsor = 'BOTH', each side's explicit discount amount.
-- Null for COMPANY-only and PROVIDER-only discounts.
ALTER TABLE disc_discount_codes
    ADD COLUMN IF NOT EXISTS company_value NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS provider_value NUMERIC(12, 2);
