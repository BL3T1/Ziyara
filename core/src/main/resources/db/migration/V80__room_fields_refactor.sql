-- V80: Remove suite_tier (redundant/buggy), add bed_type, area_sqm, view_type,
--      smoking_allowed, is_accessible.
-- Uses IF EXISTS / IF NOT EXISTS — safe to run whether or not changes were pre-applied manually.

ALTER TABLE hotel_service_rooms DROP COLUMN IF EXISTS suite_tier;

ALTER TABLE hotel_service_rooms
    ADD COLUMN IF NOT EXISTS bed_type        varchar(16)  NULL,
    ADD COLUMN IF NOT EXISTS area_sqm        numeric(5,1) NULL,
    ADD COLUMN IF NOT EXISTS view_type       varchar(16)  NULL,
    ADD COLUMN IF NOT EXISTS smoking_allowed boolean      NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_accessible   boolean      NOT NULL DEFAULT false;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_bed_type' AND conrelid = 'hotel_service_rooms'::regclass
    ) THEN
        ALTER TABLE hotel_service_rooms
            ADD CONSTRAINT chk_bed_type CHECK (
                bed_type IS NULL OR bed_type = ANY (ARRAY['SINGLE','DOUBLE','TWIN','KING','QUEEN'])
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'chk_view_type' AND conrelid = 'hotel_service_rooms'::regclass
    ) THEN
        ALTER TABLE hotel_service_rooms
            ADD CONSTRAINT chk_view_type CHECK (
                view_type IS NULL OR view_type = ANY (ARRAY['CITY','GARDEN','POOL','SEA','NONE'])
            );
    END IF;
END $$;
