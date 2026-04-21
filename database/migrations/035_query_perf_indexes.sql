-- Hot-path list filters and FK lookups (idempotent).
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_customer_created
    ON bkg_bookings (customer_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_bkg_bookings_service_status
    ON bkg_bookings (service_id, status);

CREATE INDEX IF NOT EXISTS idx_hotel_services_provider_status
    ON hotel_services (provider_id, status)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_sys_users_email_lower_active
    ON sys_users (lower(email))
    WHERE deleted_at IS NULL;
