-- V24: Fix hotel_services status constraint to match ServiceStatus Java enum.
--
-- V16 allowed: PENDING_APPROVAL, ACTIVE, INACTIVE, SUSPENDED, REJECTED, ARCHIVED
-- ServiceStatus enum: ACTIVE, INACTIVE, SUSPENDED, PENDING_APPROVAL,
--                     AVAILABLE, UNAVAILABLE, MAINTENANCE, DISCONTINUED, HIDDEN
-- Missing from constraint: AVAILABLE, UNAVAILABLE, MAINTENANCE, DISCONTINUED, HIDDEN
-- Extra in constraint: REJECTED, ARCHIVED (kept for backward compat)

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_hotel_services_status'
          AND table_name      = 'hotel_services'
    ) THEN
        ALTER TABLE hotel_services DROP CONSTRAINT chk_hotel_services_status;
    END IF;

    ALTER TABLE hotel_services
        ADD CONSTRAINT chk_hotel_services_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE',
            'SUSPENDED',
            'PENDING_APPROVAL',
            'AVAILABLE',
            'UNAVAILABLE',
            'MAINTENANCE',
            'DISCONTINUED',
            'HIDDEN',
            -- legacy values kept for backward compatibility
            'REJECTED',
            'ARCHIVED'
        ));
END $$;
