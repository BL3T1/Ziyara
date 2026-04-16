-- ============================================================================
-- DYNAMIC_COMMISSION_REPORT – commission change audit (Phase 1, optional)
-- Run after: 013. Used when PATCH /providers/{id}/commission is called.
-- ============================================================================

-- Ensure refunds has processed_by for audit (optional per plan)
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS processed_by UUID REFERENCES users(id);
COMMENT ON COLUMN refunds.processed_by IS 'User who created the refund (for audit)';

CREATE TABLE IF NOT EXISTS provider_commission_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    old_rate NUMERIC(5,2),
    new_rate NUMERIC(5,2) NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255)
);

COMMENT ON TABLE provider_commission_audit IS 'Audit trail of provider commission rate changes';
CREATE INDEX IF NOT EXISTS idx_provider_commission_audit_provider_id ON provider_commission_audit(provider_id);
CREATE INDEX IF NOT EXISTS idx_provider_commission_audit_changed_at ON provider_commission_audit(changed_at);
