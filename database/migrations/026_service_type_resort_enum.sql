-- Ensure RESORT exists on service_type_enum (idempotent for DBs created before RESORT was added).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON e.enumtypid = t.oid
        WHERE t.typname = 'service_type_enum'
          AND e.enumlabel = 'RESORT'
    ) THEN
        ALTER TYPE service_type_enum ADD VALUE 'RESORT';
    END IF;
END$$;
