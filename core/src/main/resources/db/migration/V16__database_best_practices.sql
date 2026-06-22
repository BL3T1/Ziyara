-- ============================================================================
-- V16 — Database Best-Practice Hardening
--
-- Fixes identified through full schema audit of V0–V15:
--
--  [A] Referential integrity gaps   — missing FK constraints & indexes
--  [B] Redundant indexes            — indexes that duplicate UNIQUE constraint indexes
--  [C] CHECK constraints            — unconstrained status/type VARCHAR columns
--  [D] Partial indexes              — soft-delete & status-filtered hot paths
--  [E] Composite indexes            — common multi-column query patterns
--  [F] NULL-safety                  — nullable columns that should have NOT NULL defaults
--  [G] Timestamp type               — TIMESTAMP → TIMESTAMPTZ for web_content_pages
--  [H] Missing UNIQUE constraints   — duplicates that should be impossible
--  [I] Updated_at trigger           — DB-level enforcement of updated_at stamping
--  [J] Data type improvements       — DOUBLE PRECISION → NUMERIC for rating
--
-- All changes are idempotent. Destructive changes (DROP INDEX) use IF EXISTS.
-- ============================================================================

-- ============================================================================
-- [A] REFERENTIAL INTEGRITY GAPS
-- ============================================================================

-- A1. hotel_reviews.booking_id declared NOT NULL but has NO FK to bkg_bookings.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_hotel_reviews_booking_id'
          AND table_name      = 'hotel_reviews'
    ) THEN
        ALTER TABLE hotel_reviews
            ADD CONSTRAINT fk_hotel_reviews_booking_id
            FOREIGN KEY (booking_id) REFERENCES bkg_bookings (id)
            ON DELETE SET NULL;
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_hotel_reviews_booking_id
    ON hotel_reviews (booking_id);

-- A2. bkg_taxi_bookings.booking_id FK exists but has no index.
CREATE INDEX IF NOT EXISTS idx_bkg_taxi_bookings_booking_id
    ON bkg_taxi_bookings (booking_id);

-- A3. bkg_taxi_bookings.driver_id referenced logically but no FK or index.
CREATE INDEX IF NOT EXISTS idx_bkg_taxi_bookings_driver_id
    ON bkg_taxi_bookings (driver_id)
    WHERE driver_id IS NOT NULL;

-- A4. pay_refunds.payment_id FK exists but has no dedicated index.
CREATE INDEX IF NOT EXISTS idx_pay_refunds_payment_id
    ON pay_refunds (payment_id);

-- A5. bkg_bookings.cancelled_by / rejected_by — user references without FK or index.
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_cancelled_by
    ON bkg_bookings (cancelled_by)
    WHERE cancelled_by IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_bkg_bookings_rejected_by
    ON bkg_bookings (rejected_by)
    WHERE rejected_by IS NOT NULL;

-- A6. pay_refunds.processed_by — user reference without FK or index.
CREATE INDEX IF NOT EXISTS idx_pay_refunds_processed_by
    ON pay_refunds (processed_by)
    WHERE processed_by IS NOT NULL;

-- A7. support_complaints.assigned_agent_id — high-cardinality column, no index.
CREATE INDEX IF NOT EXISTS idx_support_complaints_assigned_agent
    ON support_complaints (assigned_agent_id)
    WHERE assigned_agent_id IS NOT NULL;

-- A8. disc_discount_codes.provider_id — FK exists, no index.
CREATE INDEX IF NOT EXISTS idx_disc_discount_codes_provider_id
    ON disc_discount_codes (provider_id)
    WHERE provider_id IS NOT NULL;

-- A9. sys_password_reset_tokens — token column is the primary lookup key, not indexed.
CREATE INDEX IF NOT EXISTS idx_sys_password_reset_tokens_token
    ON sys_password_reset_tokens (token);

-- A10. sessions.expires_at — needed for cleanup jobs and validity checks.
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at
    ON sessions (expires_at)
    WHERE is_invalidated IS NOT TRUE;

-- ============================================================================
-- [B] REDUNDANT INDEXES
--     UNIQUE constraints automatically create a B-tree index on the constrained
--     column(s). A separate CREATE INDEX on the same column(s) wastes storage
--     and adds write overhead with zero query benefit.
-- ============================================================================

-- B1. idx_sys_users_email duplicates the UNIQUE constraint on sys_users(email).
DROP INDEX IF EXISTS idx_sys_users_email;

-- B2. idx_web_content_pages_slug duplicates the UNIQUE constraint on web_content_pages(slug).
DROP INDEX IF EXISTS idx_web_content_pages_slug;

-- ============================================================================
-- [C] CHECK CONSTRAINTS ON STATUS / ENUM COLUMNS
--     Prevents invalid values from entering the DB even if application
--     validation is bypassed (migration scripts, admin tools, bugs).
-- ============================================================================

-- C1. sys_users.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_sys_users_status'
          AND table_name      = 'sys_users'
    ) THEN
        ALTER TABLE sys_users
            ADD CONSTRAINT chk_sys_users_status
            CHECK (status IN (
                'ACTIVE','INACTIVE','SUSPENDED',
                'PENDING_VERIFICATION','PENDING_APPROVAL','LOCKED'
            ));
    END IF;
END $$;

-- C2. hotel_service_providers.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_hotel_service_providers_status'
          AND table_name      = 'hotel_service_providers'
    ) THEN
        ALTER TABLE hotel_service_providers
            ADD CONSTRAINT chk_hotel_service_providers_status
            CHECK (status IN (
                'PENDING_APPROVAL','ACTIVE','SUSPENDED',
                'REJECTED','DEACTIVATED'
            ));
    END IF;
END $$;

-- C3. hotel_services.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_hotel_services_status'
          AND table_name      = 'hotel_services'
    ) THEN
        ALTER TABLE hotel_services
            ADD CONSTRAINT chk_hotel_services_status
            CHECK (status IN (
                'PENDING_APPROVAL','ACTIVE','INACTIVE',
                'SUSPENDED','REJECTED','ARCHIVED'
            ));
    END IF;
END $$;

-- C4. bkg_bookings.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_bkg_bookings_status'
          AND table_name      = 'bkg_bookings'
    ) THEN
        ALTER TABLE bkg_bookings
            ADD CONSTRAINT chk_bkg_bookings_status
            CHECK (status IN (
                'PENDING','CONFIRMED','ACTIVE','COMPLETED',
                'CANCELLED','REJECTED','EXPIRED','NO_SHOW'
            ));
    END IF;
END $$;

-- C5. pay_payments.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_pay_payments_status'
          AND table_name      = 'pay_payments'
    ) THEN
        ALTER TABLE pay_payments
            ADD CONSTRAINT chk_pay_payments_status
            CHECK (status IN (
                'PENDING','PROCESSING','COMPLETED',
                'FAILED','REFUNDED','CANCELLED','DISPUTED'
            ));
    END IF;
END $$;

-- C6. pay_refunds.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_pay_refunds_status'
          AND table_name      = 'pay_refunds'
    ) THEN
        ALTER TABLE pay_refunds
            ADD CONSTRAINT chk_pay_refunds_status
            CHECK (status IN ('REQUESTED','PROCESSING','COMPLETED','REJECTED','CANCELLED'));
    END IF;
END $$;

-- C7. support_complaints.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_support_complaints_status'
          AND table_name      = 'support_complaints'
    ) THEN
        ALTER TABLE support_complaints
            ADD CONSTRAINT chk_support_complaints_status
            CHECK (status IN (
                'SUBMITTED','OPEN','IN_PROGRESS','ESCALATED',
                'RESOLVED','CLOSED','REJECTED'
            ));
    END IF;
END $$;

-- C8. sys_notifications.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_sys_notifications_status'
          AND table_name      = 'sys_notifications'
    ) THEN
        ALTER TABLE sys_notifications
            ADD CONSTRAINT chk_sys_notifications_status
            CHECK (status IN ('PENDING','SENT','FAILED','READ','ARCHIVED'));
    END IF;
END $$;

-- C9. hotel_reviews.status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_hotel_reviews_status'
          AND table_name      = 'hotel_reviews'
    ) THEN
        ALTER TABLE hotel_reviews
            ADD CONSTRAINT chk_hotel_reviews_status
            CHECK (status IN ('PENDING','APPROVED','REJECTED','HIDDEN'));
    END IF;
END $$;

-- C10. hotel_reviews.rating range enforcement
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_hotel_reviews_rating_range'
          AND table_name      = 'hotel_reviews'
    ) THEN
        ALTER TABLE hotel_reviews
            ADD CONSTRAINT chk_hotel_reviews_rating_range
            CHECK (rating BETWEEN 1 AND 5);
    END IF;
END $$;

-- C11. sys_roles.status — currently nullable with no constraint
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_sys_roles_status'
          AND table_name      = 'sys_roles'
    ) THEN
        ALTER TABLE sys_roles
            ADD CONSTRAINT chk_sys_roles_status
            CHECK (status IN ('ACTIVE','INACTIVE','DEPRECATED'));
    END IF;
END $$;

-- C12. Booking financial sanity: amounts must be non-negative
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_bkg_bookings_amounts'
          AND table_name      = 'bkg_bookings'
    ) THEN
        ALTER TABLE bkg_bookings
            ADD CONSTRAINT chk_bkg_bookings_amounts
            CHECK (
                base_amount   >= 0 AND
                discount_amount >= 0 AND
                tax_amount    >= 0 AND
                total_amount  >= 0
            );
    END IF;
END $$;

-- C13. pay_payments.amount must be positive
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_pay_payments_amount_positive'
          AND table_name      = 'pay_payments'
    ) THEN
        ALTER TABLE pay_payments
            ADD CONSTRAINT chk_pay_payments_amount_positive
            CHECK (amount > 0);
    END IF;
END $$;

-- ============================================================================
-- [D] PARTIAL INDEXES FOR SOFT-DELETE AND HOT-PATH STATUS FILTERS
-- ============================================================================

-- D1. Active users (soft-deleted users excluded from all application queries).
CREATE INDEX IF NOT EXISTS idx_sys_users_active_email
    ON sys_users (email)
    WHERE deleted_at IS NULL;

-- D2. Active services (deleted_at IS NULL queries on hotel_services).
CREATE INDEX IF NOT EXISTS idx_hotel_services_active
    ON hotel_services (provider_id, type, status)
    WHERE deleted_at IS NULL;

-- D3. Active employees (V14 introduced offboarded_at — already indexed in V14).
-- No additional index needed here; see V14.

-- D4. Pending/open bookings dashboard (most common operator query).
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_pending_created
    ON bkg_bookings (created_at DESC)
    WHERE status IN ('PENDING', 'CONFIRMED');

-- D5. Non-invalidated sessions — complement to V11's idx_sessions_active_user.
CREATE INDEX IF NOT EXISTS idx_sessions_valid_expires
    ON sessions (expires_at DESC)
    WHERE is_invalidated IS NOT TRUE;

-- D6. Unread notifications per user — the most common notification query.
CREATE INDEX IF NOT EXISTS idx_sys_notifications_unread
    ON sys_notifications (user_id, created_at DESC)
    WHERE read_at IS NULL AND status != 'ARCHIVED';

-- D7. Active discount codes — online booking validation path.
CREATE INDEX IF NOT EXISTS idx_disc_discount_codes_active_code
    ON disc_discount_codes (code)
    WHERE status = 'ACTIVE' AND approval_status = 'APPROVED';

-- ============================================================================
-- [E] COMPOSITE INDEXES FOR COMMON QUERY PATTERNS
-- ============================================================================

-- E1. Audit log — deletion-log component filter (entity_type + action + created_at).
--     Matches the JPQL query added in V16-adjacent service code.
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_type_action_created
    ON sys_audit_logs (entity_type, action, created_at DESC);

-- E2. Audit log — actor lookup (who did what, when).
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_user_created
    ON sys_audit_logs (user_id, created_at DESC)
    WHERE user_id IS NOT NULL;

-- E3. Security events — rate-limit window query (type + ip + time window).
CREATE INDEX IF NOT EXISTS idx_sys_security_events_type_ip_created
    ON sys_security_events (event_type, ip_address, created_at DESC)
    WHERE ip_address IS NOT NULL;

-- E4. Bookings — service availability check (service_id + check_in_date + status).
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_service_checkin_status
    ON bkg_bookings (service_id, check_in_date, status)
    WHERE status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED');

-- E5. Payments — gateway reference lookup (common in webhook processing).
CREATE INDEX IF NOT EXISTS idx_pay_payments_gateway_ref
    ON pay_payments (gateway_reference)
    WHERE gateway_reference IS NOT NULL;

-- E6. Rate limit counters — window cleanup.
CREATE INDEX IF NOT EXISTS idx_sys_rate_limit_window_start
    ON sys_rate_limit_counters (window_end, identifier_type);

-- E7. OTP expiry — cleanup job and validity check.
-- Note: CURRENT_TIMESTAMP is STABLE/VOLATILE, not IMMUTABLE, so it cannot be used
-- in a partial index predicate. A full index on expires_at serves both the cleanup
-- job (scan all rows) and validity queries (planner applies the run-time predicate).
CREATE INDEX IF NOT EXISTS idx_sys_otp_expires_at
    ON sys_otp_verification (expires_at);

-- ============================================================================
-- [F] NULL-SAFETY — NOT NULL WITH DEFAULTS ON KEY COLUMNS
-- ============================================================================

-- F1. sys_employees.created_at is nullable in V0 schema.
ALTER TABLE sys_employees
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP;
UPDATE sys_employees SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
ALTER TABLE sys_employees
    ALTER COLUMN created_at SET NOT NULL;

-- F2. sys_employees.updated_at
ALTER TABLE sys_employees
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;
UPDATE sys_employees SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

-- F3. sys_user_roles.assigned_at — should stamp when the role was granted.
ALTER TABLE sys_user_roles
    ALTER COLUMN assigned_at SET DEFAULT CURRENT_TIMESTAMP;
UPDATE sys_user_roles SET assigned_at = CURRENT_TIMESTAMP WHERE assigned_at IS NULL;
ALTER TABLE sys_user_roles
    ALTER COLUMN assigned_at SET NOT NULL;

-- F4. sys_roles.status — default exists but column is nullable; set NOT NULL.
UPDATE sys_roles SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE sys_roles ALTER COLUMN status SET NOT NULL;
ALTER TABLE sys_roles ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- F5. sys_departments.updated_at — nullable; give it a default so UPDATEs stamp it.
ALTER TABLE sys_departments ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- F6. hotel_service_providers.updated_at — same pattern.
ALTER TABLE hotel_service_providers ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- F7. customers.updated_at
ALTER TABLE customers ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- ============================================================================
-- [G] TIMESTAMP → TIMESTAMPTZ FOR web_content_pages
--     TIMESTAMP stores no timezone; all other tables use TIMESTAMPTZ.
--     We cast the existing data and change the column type.
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name  = 'web_content_pages'
          AND column_name = 'created_at'
          AND data_type   = 'timestamp without time zone'
    ) THEN
        ALTER TABLE web_content_pages
            ALTER COLUMN created_at TYPE TIMESTAMPTZ
            USING created_at AT TIME ZONE 'UTC';
        ALTER TABLE web_content_pages
            ALTER COLUMN updated_at TYPE TIMESTAMPTZ
            USING updated_at AT TIME ZONE 'UTC';
        ALTER TABLE web_content_pages
            ALTER COLUMN created_at SET NOT NULL;
        ALTER TABLE web_content_pages
            ALTER COLUMN updated_at SET NOT NULL;
    END IF;
END $$;

-- web_content_pages.id was declared without DEFAULT gen_random_uuid().
-- Add the default so INSERTs without explicit id work correctly.
DO $$
BEGIN
    IF (SELECT column_default IS NULL
        FROM information_schema.columns
        WHERE table_name = 'web_content_pages' AND column_name = 'id') THEN
        ALTER TABLE web_content_pages
            ALTER COLUMN id SET DEFAULT gen_random_uuid();
    END IF;
END $$;

-- ============================================================================
-- [H] MISSING UNIQUE CONSTRAINTS
-- ============================================================================

-- H1. sys_user_roles — user should not be assigned the same role in the same
--     group context more than once.  Covers (user_id, role_id, group_id) where
--     group_id may be NULL (use UNIQUE NULLS NOT DISTINCT on PG15+, else partial).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uk_sys_user_roles_user_role_group'
          AND table_name      = 'sys_user_roles'
    ) THEN
        -- PG 15 supports UNIQUE NULLS NOT DISTINCT for nullable columns.
        -- Use a conditional approach for broader compatibility.
        ALTER TABLE sys_user_roles
            ADD CONSTRAINT uk_sys_user_roles_user_role_group
            UNIQUE NULLS NOT DISTINCT (user_id, role_id, group_id);
    END IF;
END $$;

-- H2. pay_exchange_rates — only one rate per currency pair per date.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uk_pay_exchange_rates_pair_date'
          AND table_name      = 'pay_exchange_rates'
    ) THEN
        ALTER TABLE pay_exchange_rates
            ADD CONSTRAINT uk_pay_exchange_rates_pair_date
            UNIQUE (from_currency, to_currency, effective_date);
    END IF;
END $$;

-- H3. sys_integration_api_keys — key_prefix should be unique (used as lookup).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'uk_sys_integration_api_keys_prefix'
          AND table_name      = 'sys_integration_api_keys'
    ) THEN
        ALTER TABLE sys_integration_api_keys
            ADD CONSTRAINT uk_sys_integration_api_keys_prefix
            UNIQUE (key_prefix);
    END IF;
END $$;

-- ============================================================================
-- [I] DB-LEVEL updated_at TRIGGER
--     Ensures updated_at is always stamped when a row changes, even via
--     direct SQL (psql, migrations, admin tools) that bypasses JPA lifecycle.
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

-- Apply the trigger to every table that has an updated_at column.
DO $$
DECLARE
    tbl TEXT;
    tbl_list TEXT[] := ARRAY[
        'sys_users',
        'sys_roles',
        'sys_groups',
        'sys_departments',
        'sys_employees',
        'sys_notifications',
        'sys_data_retention_policies',
        'sys_data_export_requests',
        'sys_plans',
        'sys_customer_subscriptions',
        'hotel_service_providers',
        'hotel_services',
        'hotel_service_images',
        'hotel_service_rooms',
        'hotel_service_room_images',
        'hotel_rest_menu_sections',
        'hotel_rest_menu_items',
        'hotel_reviews',
        'disc_discount_codes',
        'bkg_bookings',
        'bkg_taxi_bookings',
        'pay_payments',
        'pay_refunds',
        'pay_exchange_rates',
        'support_complaints',
        'support_complaint_comments',
        'support_internal_tickets',
        'support_ticket_comments',
        'customers',
        'sessions',
        'web_content_pages'
    ];
BEGIN
    FOREACH tbl IN ARRAY tbl_list LOOP
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = tbl
        ) AND NOT EXISTS (
            SELECT 1 FROM information_schema.triggers
            WHERE trigger_name = 'trg_' || tbl || '_updated_at'
              AND event_object_table = tbl
        ) THEN
            EXECUTE format(
                'CREATE TRIGGER trg_%I_updated_at
                 BEFORE UPDATE ON %I
                 FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at()',
                tbl, tbl
            );
        END IF;
    END LOOP;
END $$;

-- ============================================================================
-- [J] DATA TYPE IMPROVEMENTS
-- ============================================================================

-- J1. hotel_service_providers.rating: DOUBLE PRECISION → NUMERIC(3,2)
--     DOUBLE PRECISION has floating-point imprecision (e.g. 4.3 stored as
--     4.29999999...). Ratings are displayed to 1 decimal place and aggregated;
--     NUMERIC(3,2) is exact and prevents UI rounding artefacts.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name  = 'hotel_service_providers'
          AND column_name = 'rating'
          AND data_type   = 'double precision'
    ) THEN
        ALTER TABLE hotel_service_providers
            ALTER COLUMN rating TYPE NUMERIC(3,2)
            USING ROUND(rating::NUMERIC, 2);

        ALTER TABLE hotel_service_providers
            ADD CONSTRAINT chk_provider_rating_range
            CHECK (rating BETWEEN 0.00 AND 5.00);
    END IF;
END $$;

-- J2. sys_notifications.metadata: TEXT → JSONB
--     metadata was defined as TEXT but is semantically structured JSON.
--     JSONB allows indexed key access and server-side JSON operators.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name  = 'sys_notifications'
          AND column_name = 'metadata'
          AND data_type   = 'text'
    ) THEN
        ALTER TABLE sys_notifications
            ALTER COLUMN metadata TYPE JSONB
            USING CASE
                WHEN metadata IS NULL OR metadata = '' THEN NULL
                WHEN metadata LIKE '{%' THEN metadata::JSONB
                ELSE jsonb_build_object('raw', metadata)
            END;
    END IF;
END $$;

-- ============================================================================
-- Maintenance: refresh the materialized view created in V12 to pick up any
-- new payment data loaded between V12 and V16.
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_class c
        JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
        WHERE c.relkind = 'm' AND c.relname = 'mv_pay_daily_totals'
    ) THEN
        REFRESH MATERIALIZED VIEW CONCURRENTLY mv_pay_daily_totals;
    END IF;
EXCEPTION WHEN OTHERS THEN
    -- CONCURRENTLY requires at least one row; skip silently on empty DB
    NULL;
END $$;
