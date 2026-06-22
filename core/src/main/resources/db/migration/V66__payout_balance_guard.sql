-- G2: Speeds up the per-provider pending-payout balance check used by the advisory-lock guard in createPayoutRequest()
CREATE INDEX IF NOT EXISTS idx_payout_pending_by_provider
    ON portal_payout_requests (provider_id)
    WHERE status = 'PENDING';
