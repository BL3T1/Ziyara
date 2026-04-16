-- Allow JPA inserts without legacy columns Hibernate does not map (percentage, expiry_date, created_by).
-- New shape uses type, value, end_date; created_by is set by application when column exists.

ALTER TABLE disc_discount_codes
  ALTER COLUMN percentage DROP NOT NULL,
  ALTER COLUMN expiry_date DROP NOT NULL,
  ALTER COLUMN created_by DROP NOT NULL;
