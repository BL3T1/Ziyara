-- ============================================================================
-- Hibernate enum compatibility (remaining tables)
-- JPA @Enumerated(EnumType.STRING) expects VARCHAR; convert PostgreSQL enums
-- Run after: schema + migrations 001-010
-- Run once per environment; re-running after columns are already VARCHAR may error
-- ============================================================================

-- roles
ALTER TABLE roles ALTER COLUMN level TYPE varchar(50) USING level::text;

-- bookings
ALTER TABLE bookings ALTER COLUMN status TYPE varchar(50) USING status::text;

-- complaints
ALTER TABLE complaints
  ALTER COLUMN priority TYPE varchar(50) USING priority::text,
  ALTER COLUMN status TYPE varchar(50) USING status::text;

-- internal_tickets
ALTER TABLE internal_tickets
  ALTER COLUMN type TYPE varchar(50) USING type::text,
  ALTER COLUMN priority TYPE varchar(50) USING priority::text,
  ALTER COLUMN status TYPE varchar(50) USING status::text;

-- refunds
ALTER TABLE refunds ALTER COLUMN status TYPE varchar(50) USING status::text;

-- notifications
ALTER TABLE notifications
  ALTER COLUMN type TYPE varchar(50) USING type::text,
  ALTER COLUMN channel TYPE varchar(50) USING channel::text,
  ALTER COLUMN status TYPE varchar(50) USING status::text;

-- taxi_bookings (skip if schema already uses varchar — see JPA-aligned taxi_bookings in schema.sql)
DO $taxi_enum_to_varchar$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'taxi_bookings'
      AND column_name = 'vehicle_type' AND data_type = 'USER-DEFINED'
  ) THEN
    ALTER TABLE taxi_bookings
      ALTER COLUMN vehicle_type TYPE varchar(50) USING vehicle_type::text,
      ALTER COLUMN status TYPE varchar(50) USING status::text;
  END IF;
END
$taxi_enum_to_varchar$;

-- services
ALTER TABLE services
  ALTER COLUMN type TYPE varchar(50) USING type::text,
  ALTER COLUMN status TYPE varchar(50) USING status::text;
