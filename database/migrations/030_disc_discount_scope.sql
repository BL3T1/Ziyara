-- Provider- and listing-scoped discount codes + optional restaurant menu / room-type scope.
-- provider_id NULL = company-wide (any provider). Non-null = only that provider's offerings.
-- JSONB arrays: empty or NULL = no restriction for that dimension.

ALTER TABLE disc_discount_codes
  ADD COLUMN IF NOT EXISTS provider_id UUID REFERENCES hotel_service_providers(id) ON DELETE SET NULL;

ALTER TABLE disc_discount_codes
  ADD COLUMN IF NOT EXISTS applicable_service_ids JSONB;

ALTER TABLE disc_discount_codes
  ADD COLUMN IF NOT EXISTS applicable_menu_section_ids JSONB;

ALTER TABLE disc_discount_codes
  ADD COLUMN IF NOT EXISTS applicable_menu_item_ids JSONB;

ALTER TABLE disc_discount_codes
  ADD COLUMN IF NOT EXISTS applicable_room_type_ids JSONB;

CREATE INDEX IF NOT EXISTS idx_disc_discount_codes_provider_id ON disc_discount_codes(provider_id);

-- Snapshot at booking time for post-hoc discount apply / audit (optional fields).
ALTER TABLE bkg_bookings
  ADD COLUMN IF NOT EXISTS discount_context_menu_item_ids JSONB;

ALTER TABLE bkg_bookings
  ADD COLUMN IF NOT EXISTS discount_context_menu_section_ids JSONB;

ALTER TABLE bkg_bookings
  ADD COLUMN IF NOT EXISTS discount_context_room_type_id UUID;
