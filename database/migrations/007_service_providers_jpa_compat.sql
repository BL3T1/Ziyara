-- ============================================================================
-- Service providers - add columns for JPA, fix status for Hibernate
-- ============================================================================
ALTER TABLE service_providers
  ADD COLUMN IF NOT EXISTS city VARCHAR(100),
  ADD COLUMN IF NOT EXISTS country VARCHAR(100),
  ADD COLUMN IF NOT EXISTS rating DECIMAL(3, 2) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS review_count INTEGER DEFAULT 0,
  ADD COLUMN IF NOT EXISTS verified BOOLEAN DEFAULT FALSE;

ALTER TABLE service_providers ALTER COLUMN status TYPE varchar(50) USING status::text;
