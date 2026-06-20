-- Booking lifecycle tracking: rejection/delay reasons and internal notes
ALTER TABLE bkg_bookings
    ADD COLUMN IF NOT EXISTS rejection_reason  TEXT,
    ADD COLUMN IF NOT EXISTS delay_reason      TEXT,
    ADD COLUMN IF NOT EXISTS internal_notes    TEXT,
    ADD COLUMN IF NOT EXISTS rejected_at       TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS rejected_by       UUID REFERENCES sys_users(id) ON DELETE SET NULL;

COMMENT ON COLUMN bkg_bookings.rejection_reason IS 'Provider-supplied reason when booking is rejected';
COMMENT ON COLUMN bkg_bookings.delay_reason     IS 'Explanation when service delivery is delayed';
COMMENT ON COLUMN bkg_bookings.internal_notes   IS 'Internal staff notes, not visible to customer';
COMMENT ON COLUMN bkg_bookings.rejected_at      IS 'Timestamp of rejection';
COMMENT ON COLUMN bkg_bookings.rejected_by      IS 'Staff member who rejected the booking';
