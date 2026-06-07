-- Booking payment status tracking
ALTER TABLE bkg_bookings
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';

-- Provider coordinates for map views
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS latitude  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Indexes for map queries on services
CREATE INDEX IF NOT EXISTS idx_services_coords
    ON hotel_services(latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_services_provider_coords
    ON hotel_services(provider_id, latitude, longitude)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
