CREATE TABLE IF NOT EXISTS delivery_locations (
    id          UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID             NOT NULL REFERENCES bkg_bookings(id),
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    status      VARCHAR(50),
    recorded_at TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_delivery_loc_booking
    ON delivery_locations(booking_id, recorded_at DESC);
