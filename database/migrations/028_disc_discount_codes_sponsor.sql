-- Who funds the discount at pricing time: company, provider, or split (both).
ALTER TABLE disc_discount_codes
  ADD COLUMN IF NOT EXISTS sponsor VARCHAR(20) NOT NULL DEFAULT 'COMPANY';

UPDATE disc_discount_codes SET sponsor = 'COMPANY' WHERE sponsor IS NULL;
