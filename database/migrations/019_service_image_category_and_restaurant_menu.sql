-- ============================================================================
-- Service image categories + restaurant menu tables (service media & menus plan)
-- Run after: 018
-- ============================================================================

-- Align legacy service_images columns with JPA (fresh Docker uses schema.sql names)
DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'hotel_service_images' AND column_name = 'image_url'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'hotel_service_images' AND column_name = 'url'
  ) THEN
    ALTER TABLE hotel_service_images RENAME COLUMN image_url TO url;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'hotel_service_images' AND column_name = 'caption'
  ) AND NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'hotel_service_images' AND column_name = 'alt_text'
  ) THEN
    ALTER TABLE hotel_service_images RENAME COLUMN caption TO alt_text;
  END IF;
END $$;

ALTER TABLE hotel_service_images ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;
UPDATE hotel_service_images SET updated_at = COALESCE(created_at, CURRENT_TIMESTAMP) WHERE updated_at IS NULL;

ALTER TABLE hotel_service_images ADD COLUMN IF NOT EXISTS category VARCHAR(32) NOT NULL DEFAULT 'PROPERTY';
ALTER TABLE hotel_service_images ADD COLUMN IF NOT EXISTS context_key VARCHAR(100);

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_hotel_service_images_category') THEN
    ALTER TABLE hotel_service_images ADD CONSTRAINT chk_hotel_service_images_category
      CHECK (category IN ('PROPERTY', 'ROOM', 'TRIP', 'OTHER'));
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS hotel_rest_menu_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES hotel_services(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS hotel_rest_menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES hotel_rest_menu_sections(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12, 2),
    currency VARCHAR(3),
    image_url VARCHAR(500),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hotel_rest_menu_sections_service_id ON hotel_rest_menu_sections(service_id);
CREATE INDEX IF NOT EXISTS idx_hotel_rest_menu_items_section_id ON hotel_rest_menu_items(section_id);
