-- ============================================================================
-- Hibernate enum compatibility - use VARCHAR for role/status
-- JPA @Enumerated(EnumType.STRING) sends VARCHAR; PostgreSQL enum requires cast
-- Run after main schema and prior migrations
-- ============================================================================

ALTER TABLE users
  ALTER COLUMN role TYPE varchar(50) USING role::text,
  ALTER COLUMN status TYPE varchar(50) USING status::text;
