-- ============================================================================
-- V45 — Add payment_method column to bkg_bookings.
--
-- Stores the customer's chosen payment method at booking time.
-- NULL is allowed so existing rows are unaffected.
-- ============================================================================
ALTER TABLE bkg_bookings
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50);
