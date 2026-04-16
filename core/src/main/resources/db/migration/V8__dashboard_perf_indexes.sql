-- Dashboard query performance: range scans on bookings, recent audit feed, join keys.

CREATE INDEX IF NOT EXISTS idx_bkg_bookings_created_at ON bkg_bookings (created_at);

CREATE INDEX IF NOT EXISTS idx_bkg_bookings_status_created_at ON bkg_bookings (status, created_at);

CREATE INDEX IF NOT EXISTS idx_bkg_bookings_service_id ON bkg_bookings (service_id);

CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_created_at ON sys_audit_logs (created_at DESC);
