-- ============================================================================
-- Reviews status - use VARCHAR for Hibernate @Enumerated(EnumType.STRING)
-- ============================================================================
ALTER TABLE reviews
  ALTER COLUMN status TYPE varchar(50) USING status::text;
