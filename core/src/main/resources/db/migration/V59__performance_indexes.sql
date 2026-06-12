-- ============================================================================
-- V59 — Performance Indexes
--
-- Adds indexes identified during production readiness audit.
-- All use IF NOT EXISTS so re-running is safe.
-- ============================================================================

-- Roles by group — used by listGroupSummaries, listRoles, findByGroupIdOrderByName
CREATE INDEX IF NOT EXISTS idx_sys_roles_group_id
    ON sys_roles (group_id);

-- Booking date-range overlap queries — availability checks scan service_id + dates
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_dates
    ON bkg_bookings (service_id, check_in_date, check_out_date);

-- Unread notification count (partial index — only unread rows, much smaller)
CREATE INDEX IF NOT EXISTS idx_sys_notifications_user_unread
    ON sys_notifications (user_id, created_at DESC)
    WHERE read_at IS NULL;

-- Payments by booking — verify idempotency lookups are fast
CREATE INDEX IF NOT EXISTS idx_pay_payments_booking_id
    ON pay_payments (booking_id);
