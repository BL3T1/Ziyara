-- Discount code commission split and approval workflow
ALTER TABLE disc_discount_codes
    ADD COLUMN IF NOT EXISTS company_share_pct  NUMERIC(5,2) DEFAULT 100.00,
    ADD COLUMN IF NOT EXISTS provider_share_pct NUMERIC(5,2) DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS approval_status    VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN IF NOT EXISTS approved_by        UUID REFERENCES sys_users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS approved_at        TIMESTAMP WITH TIME ZONE;

-- Shares must sum to 100
ALTER TABLE disc_discount_codes
    ADD CONSTRAINT chk_discount_share_sum
        CHECK (company_share_pct + provider_share_pct = 100.00);

-- Valid approval states
ALTER TABLE disc_discount_codes
    ADD CONSTRAINT chk_discount_approval_status
        CHECK (approval_status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED'));

COMMENT ON COLUMN disc_discount_codes.company_share_pct  IS 'Percentage of discount cost borne by the company (0–100)';
COMMENT ON COLUMN disc_discount_codes.provider_share_pct IS 'Percentage of discount cost borne by the provider (0–100)';
COMMENT ON COLUMN disc_discount_codes.approval_status    IS 'Approval state machine: DRAFT → PENDING_APPROVAL → APPROVED | REJECTED';
COMMENT ON COLUMN disc_discount_codes.approved_by        IS 'Admin who approved or rejected this discount';
COMMENT ON COLUMN disc_discount_codes.approved_at        IS 'Timestamp of approval/rejection decision';

CREATE INDEX IF NOT EXISTS idx_disc_codes_approval
    ON disc_discount_codes (approval_status);
