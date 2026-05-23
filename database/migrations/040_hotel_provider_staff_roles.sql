-- Provider staff role differentiation
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'provider_staff_role') THEN
        CREATE TYPE provider_staff_role AS ENUM (
            'FINANCIAL_ADMIN',
            'SALES_ADMIN',
            'RECEPTION',
            'MANAGER'
        );
    END IF;
END $$;

ALTER TABLE hotel_provider_staff
    ADD COLUMN IF NOT EXISTS provider_role provider_staff_role,
    ADD COLUMN IF NOT EXISTS max_staff_override INT;

-- Each provider may have at most 6 active staff members
-- Enforced at application layer; document the limit here
COMMENT ON COLUMN hotel_provider_staff.provider_role      IS 'Functional role within the provider org: FINANCIAL_ADMIN | SALES_ADMIN | RECEPTION | MANAGER';
COMMENT ON COLUMN hotel_provider_staff.max_staff_override IS 'Override global 6-staff cap for this provider (null = use global default)';

CREATE INDEX IF NOT EXISTS idx_hotel_provider_staff_role
    ON hotel_provider_staff (provider_id, provider_role);
