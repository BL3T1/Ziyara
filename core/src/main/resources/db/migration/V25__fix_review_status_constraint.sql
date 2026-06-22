-- V25: Fix chk_hotel_reviews_status to match ReviewStatus Java enum.
--
-- V16 allowed: PENDING, APPROVED, REJECTED, HIDDEN
-- ReviewStatus enum: PENDING, PUBLISHED, APPROVED, REJECTED, HIDDEN, REPORTED
--
-- Missing from constraint: PUBLISHED (written by respondToReview()), REPORTED
-- APPROVED is kept (alias for PUBLISHED, noted in enum comment)

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_hotel_reviews_status'
          AND table_name      = 'hotel_reviews'
    ) THEN
        ALTER TABLE hotel_reviews DROP CONSTRAINT chk_hotel_reviews_status;
    END IF;

    ALTER TABLE hotel_reviews
        ADD CONSTRAINT chk_hotel_reviews_status
        CHECK (status IN (
            'PENDING',
            'PUBLISHED',
            'APPROVED',
            'REJECTED',
            'HIDDEN',
            'REPORTED'
        ));
END $$;
