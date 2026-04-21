-- ============================================================================
-- Hotel room inventory and room image tables
-- ============================================================================

CREATE TABLE IF NOT EXISTS hotel_service_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES hotel_services(id) ON DELETE CASCADE,
    room_type VARCHAR(64) NOT NULL,
    room_name VARCHAR(255) NOT NULL,
    description TEXT,
    capacity INTEGER NOT NULL DEFAULT 1,
    base_price DECIMAL(12, 2),
    currency VARCHAR(3),
    quantity_total INTEGER NOT NULL DEFAULT 0,
    quantity_available INTEGER NOT NULL DEFAULT 0,
    amenities JSONB,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hotel_service_room_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES hotel_service_rooms(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hotel_service_rooms_service_id ON hotel_service_rooms(service_id);
CREATE INDEX IF NOT EXISTS idx_hotel_service_room_images_room_id ON hotel_service_room_images(room_id);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_hotel_service_rooms_status') THEN
    ALTER TABLE hotel_service_rooms ADD CONSTRAINT chk_hotel_service_rooms_status
      CHECK (status IN ('ACTIVE', 'INACTIVE'));
  END IF;
END $$;
