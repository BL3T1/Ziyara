-- V51: Expand portal_payout_requests status CHECK constraint
-- V19 only allowed PENDING/PROCESSING/COMPLETED/REJECTED.
-- AdminPayoutService also uses ON_HOLD, SCHEDULED, CANCELLED, FAILED.

ALTER TABLE portal_payout_requests
    DROP CONSTRAINT IF EXISTS portal_payout_requests_status_check;

ALTER TABLE portal_payout_requests
    ADD CONSTRAINT portal_payout_requests_status_check
    CHECK (status IN ('PENDING','PROCESSING','COMPLETED','REJECTED','ON_HOLD','SCHEDULED','CANCELLED','FAILED'));
