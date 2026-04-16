-- ============================================================================
-- Add _ar (Arabic) columns for system-wide bilingual support
-- When client sends Accept-Language: ar, API returns name_ar / description_ar
-- ============================================================================

-- departments
ALTER TABLE departments ADD COLUMN IF NOT EXISTS name_ar VARCHAR(100);
ALTER TABLE departments ADD COLUMN IF NOT EXISTS description_ar TEXT;
COMMENT ON COLUMN departments.name_ar IS 'Arabic display name';
COMMENT ON COLUMN departments.description_ar IS 'Arabic description';

-- groups
ALTER TABLE groups ADD COLUMN IF NOT EXISTS name_ar VARCHAR(100);
ALTER TABLE groups ADD COLUMN IF NOT EXISTS description_ar TEXT;
COMMENT ON COLUMN groups.name_ar IS 'Arabic display name';
COMMENT ON COLUMN groups.description_ar IS 'Arabic description';

-- roles (name/description)
ALTER TABLE roles ADD COLUMN IF NOT EXISTS name_ar VARCHAR(100);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS description_ar TEXT;
COMMENT ON COLUMN roles.name_ar IS 'Arabic display name';
COMMENT ON COLUMN roles.description_ar IS 'Arabic description';

-- permissions
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS name_ar VARCHAR(100);
ALTER TABLE permissions ADD COLUMN IF NOT EXISTS description_ar TEXT;
COMMENT ON COLUMN permissions.name_ar IS 'Arabic display name';
COMMENT ON COLUMN permissions.description_ar IS 'Arabic description';

-- services
ALTER TABLE services ADD COLUMN IF NOT EXISTS name_ar VARCHAR(255);
ALTER TABLE services ADD COLUMN IF NOT EXISTS description_ar TEXT;
COMMENT ON COLUMN services.name_ar IS 'Arabic display name';
COMMENT ON COLUMN services.description_ar IS 'Arabic description';

-- service_providers (company_name)
ALTER TABLE service_providers ADD COLUMN IF NOT EXISTS company_name_ar VARCHAR(255);
ALTER TABLE service_providers ADD COLUMN IF NOT EXISTS description_ar TEXT;
COMMENT ON COLUMN service_providers.company_name_ar IS 'Arabic company name';
COMMENT ON COLUMN service_providers.description_ar IS 'Arabic description';

-- discount_codes (description)
ALTER TABLE discount_codes ADD COLUMN IF NOT EXISTS description_ar TEXT;
COMMENT ON COLUMN discount_codes.description_ar IS 'Arabic description';
