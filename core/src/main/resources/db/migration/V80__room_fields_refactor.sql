-- V80: Remove suite_tier (redundant/buggy), add bed_type, area_sqm, view_type,
--      smoking_allowed, is_accessible.
ALTER TABLE hotel_service_rooms
    DROP COLUMN IF EXISTS suite_tier;

ALTER TABLE hotel_service_rooms
    ADD COLUMN bed_type     varchar(16)    NULL,
    ADD COLUMN area_sqm     numeric(5,1)   NULL,
    ADD COLUMN view_type    varchar(16)    NULL,
    ADD COLUMN smoking_allowed boolean     NOT NULL DEFAULT false,
    ADD COLUMN is_accessible   boolean     NOT NULL DEFAULT false;

ALTER TABLE hotel_service_rooms
    ADD CONSTRAINT chk_bed_type  CHECK (bed_type  IS NULL OR bed_type  = ANY (ARRAY['SINGLE','DOUBLE','TWIN','KING','QUEEN'])),
    ADD CONSTRAINT chk_view_type CHECK (view_type IS NULL OR view_type = ANY (ARRAY['CITY','GARDEN','POOL','SEA','NONE']));
