-- V22: Fix chk_support_complaints_status to match ComplaintStatus Java enum.
--
-- V16 allowed: SUBMITTED, OPEN, IN_PROGRESS, ESCALATED, RESOLVED, CLOSED, REJECTED
-- Java enum has: SUBMITTED, ACKNOWLEDGED, ASSIGNED, IN_PROGRESS, PENDING_INFO,
--                ESCALATED, RESOLVED, REJECTED, CLOSED, REOPENED
--
-- Missing from constraint: ACKNOWLEDGED, ASSIGNED, PENDING_INFO, REOPENED
-- Extra in constraint only: OPEN (not in Java enum, kept for backward compat)
--
-- Application fails when assigning (→ ASSIGNED) or acknowledging (→ ACKNOWLEDGED)
-- because the DB constraint rejects those values.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_support_complaints_status'
          AND table_name      = 'support_complaints'
    ) THEN
        ALTER TABLE support_complaints DROP CONSTRAINT chk_support_complaints_status;
    END IF;

    ALTER TABLE support_complaints
        ADD CONSTRAINT chk_support_complaints_status
        CHECK (status IN (
            'SUBMITTED',
            'ACKNOWLEDGED',
            'ASSIGNED',
            'IN_PROGRESS',
            'PENDING_INFO',
            'ESCALATED',
            'RESOLVED',
            'REJECTED',
            'CLOSED',
            'REOPENED',
            -- legacy value kept for backward compatibility
            'OPEN'
        ));
END $$;
