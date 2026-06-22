-- V23: Fix provider and booking status CHECK constraints to match Java enums.
--
-- ─── hotel_service_providers.status ───────────────────────────────────────────
-- V16 allowed: PENDING_APPROVAL, ACTIVE, SUSPENDED, REJECTED, DEACTIVATED
-- ProviderStatus enum: PENDING_APPROVAL, PENDING_VERIFICATION, ACTIVE,
--                      SUSPENDED, INACTIVE, REJECTED, BLOCKED
-- Missing from constraint: PENDING_VERIFICATION, INACTIVE, BLOCKED
-- Extra in constraint: DEACTIVATED (kept for backward compat)
--
-- ─── bkg_bookings.status ──────────────────────────────────────────────────────
-- V16 allowed: PENDING, CONFIRMED, ACTIVE, COMPLETED, CANCELLED, REJECTED,
--              EXPIRED, NO_SHOW
-- BookingStatus enum: PENDING, CONFIRMED, ACTIVE, COMPLETED, CANCELLED,
--                     EXPIRED, REFUNDING, REFUNDED, REFUND_FAILED,
--                     MANUAL_REVIEW, REVIEW_PENDING, REVIEWED, CLOSED
-- Missing from constraint: REFUNDING, REFUNDED, REFUND_FAILED, MANUAL_REVIEW,
--                          REVIEW_PENDING, REVIEWED, CLOSED
-- Extra in constraint: REJECTED, NO_SHOW (kept for backward compat)

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_hotel_service_providers_status'
          AND table_name      = 'hotel_service_providers'
    ) THEN
        ALTER TABLE hotel_service_providers DROP CONSTRAINT chk_hotel_service_providers_status;
    END IF;

    ALTER TABLE hotel_service_providers
        ADD CONSTRAINT chk_hotel_service_providers_status
        CHECK (status IN (
            'PENDING_APPROVAL',
            'PENDING_VERIFICATION',
            'ACTIVE',
            'SUSPENDED',
            'INACTIVE',
            'REJECTED',
            'BLOCKED',
            -- legacy values kept for backward compatibility
            'DEACTIVATED'
        ));
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_bkg_bookings_status'
          AND table_name      = 'bkg_bookings'
    ) THEN
        ALTER TABLE bkg_bookings DROP CONSTRAINT chk_bkg_bookings_status;
    END IF;

    ALTER TABLE bkg_bookings
        ADD CONSTRAINT chk_bkg_bookings_status
        CHECK (status IN (
            'PENDING',
            'CONFIRMED',
            'ACTIVE',
            'COMPLETED',
            'CANCELLED',
            'EXPIRED',
            'REFUNDING',
            'REFUNDED',
            'REFUND_FAILED',
            'MANUAL_REVIEW',
            'REVIEW_PENDING',
            'REVIEWED',
            'CLOSED',
            -- legacy values kept for backward compatibility
            'REJECTED',
            'NO_SHOW',
            -- payment-gateway hold state used internally
            'PAYMENT_PENDING'
        ));
END $$;
