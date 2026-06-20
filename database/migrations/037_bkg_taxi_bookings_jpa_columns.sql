-- ============================================================================
-- Align bkg_taxi_bookings with TaxiBookingJpaEntity
-- Legacy taxi_bookings used dropoff_*, pickup_time, fare, vehicle_plate, etc.
-- Run after 015_table_prefix_phase4.sql. Safe to re-run (idempotent checks).
-- ============================================================================

DO $body$
BEGIN
  IF to_regclass('public.bkg_taxi_bookings') IS NULL THEN
    RETURN;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'driver_id'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings ADD COLUMN driver_id UUID REFERENCES public.sys_users(id);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'estimated_distance'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings ADD COLUMN estimated_distance NUMERIC(8, 2);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'actual_distance'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings ADD COLUMN actual_distance NUMERIC(8, 2);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'actual_price'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings ADD COLUMN actual_price NUMERIC(12, 2);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'dropoff_location'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'destination_location'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings RENAME COLUMN dropoff_location TO destination_location;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'dropoff_latitude'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'destination_latitude'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings RENAME COLUMN dropoff_latitude TO destination_latitude;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'dropoff_longitude'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'destination_longitude'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings RENAME COLUMN dropoff_longitude TO destination_longitude;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'pickup_time'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'scheduled_at'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings RENAME COLUMN pickup_time TO scheduled_at;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'actual_pickup_time'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'started_at'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings RENAME COLUMN actual_pickup_time TO started_at;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'actual_dropoff_time'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'completed_at'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings RENAME COLUMN actual_dropoff_time TO completed_at;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'vehicle_plate'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'license_plate'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings RENAME COLUMN vehicle_plate TO license_plate;
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'fare'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'bkg_taxi_bookings' AND column_name = 'estimated_price'
  ) THEN
    ALTER TABLE public.bkg_taxi_bookings RENAME COLUMN fare TO estimated_price;
    ALTER TABLE public.bkg_taxi_bookings
      ALTER COLUMN estimated_price TYPE NUMERIC(12, 2) USING estimated_price::NUMERIC(12, 2);
  END IF;
END
$body$;

-- Map legacy status strings to TaxiStatus enum names used by JPA (@Enumerated STRING)
UPDATE public.bkg_taxi_bookings SET status = 'SEARCHING' WHERE status::text = 'PENDING';
UPDATE public.bkg_taxi_bookings SET status = 'ASSIGNED' WHERE status::text IN ('CONFIRMED', 'DRIVER_ASSIGNED');
UPDATE public.bkg_taxi_bookings SET status = 'EN_ROUTE_TO_PICKUP' WHERE status::text = 'EN_ROUTE';
UPDATE public.bkg_taxi_bookings SET status = 'ARRIVED_AT_PICKUP' WHERE status::text = 'ARRIVED';
