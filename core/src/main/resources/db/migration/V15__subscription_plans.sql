-- ============================================================================
-- V15: Subscription & billing model.
--
-- Design:
--   sys_plans                  — catalogue of available plans (FREE, STARTER, …)
--   sys_customer_subscriptions — one active subscription row per ServiceProvider
--   sys_subscription_add_ons   — optional seat-expansion add-ons per subscription
--
-- Default behaviour: a new ServiceProvider has no subscription row. The
-- SubscriptionService treats the absence of a row as FREE plan (max 6 users).
-- Seeded plans are immutable reference data; the app never writes to sys_plans.
-- ============================================================================

-- -------------------------------------------------------------------------
-- Plan catalogue
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_plans (
    id                      UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    code                    VARCHAR(50)   NOT NULL UNIQUE,
    name                    VARCHAR(100)  NOT NULL,
    description             TEXT,
    max_users               INTEGER       NOT NULL DEFAULT 6,   -- -1 = unlimited
    monthly_price           NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    currency                VARCHAR(3)    NOT NULL DEFAULT 'USD',
    allows_overage          BOOLEAN       NOT NULL DEFAULT false,
    overage_price_per_user  NUMERIC(12,2),
    is_active               BOOLEAN       NOT NULL DEFAULT true,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_plan_max_users CHECK (max_users = -1 OR max_users > 0)
);

-- Seed catalogue (idempotent via ON CONFLICT DO NOTHING)
INSERT INTO sys_plans (code, name, description, max_users, monthly_price, currency, allows_overage, overage_price_per_user)
VALUES
    ('FREE',         'Free',         'Default plan — up to 6 users, no charge',           6,  0.00,   'USD', false, NULL),
    ('STARTER',      'Starter',      'Up to 15 portal users',                             15, 29.99,  'USD', false, NULL),
    ('PROFESSIONAL', 'Professional', 'Up to 50 portal users with overage billing',        50, 99.99,  'USD', true,  5.00),
    ('ENTERPRISE',   'Enterprise',   'Unlimited portal users, dedicated support',         -1, 299.99, 'USD', false, NULL)
ON CONFLICT (code) DO NOTHING;

-- -------------------------------------------------------------------------
-- Per-provider subscription (one ACTIVE row per provider at any time)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_customer_subscriptions (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    provider_id           UUID         NOT NULL,   -- FK to sys_service_providers added below
    plan_id               UUID         NOT NULL REFERENCES sys_plans (id),
    status                VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    seat_limit            INTEGER      NOT NULL DEFAULT 6,
    current_period_start  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    current_period_end    TIMESTAMPTZ,
    trial_ends_at         TIMESTAMPTZ,
    cancelled_at          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_subscription_status
        CHECK (status IN ('TRIAL','ACTIVE','PAST_DUE','CANCELLED','EXPIRED')),
    CONSTRAINT chk_subscription_seat_limit CHECK (seat_limit > 0)
);

-- Defer FK to sys_service_providers (table may use a different schema name in
-- some deployments; guard with IF EXISTS / DO block).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_name = 'sys_service_providers'
    ) THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'sys_customer_subscriptions_provider_id_fkey'
              AND table_name      = 'sys_customer_subscriptions'
        ) THEN
            ALTER TABLE sys_customer_subscriptions
                ADD CONSTRAINT sys_customer_subscriptions_provider_id_fkey
                FOREIGN KEY (provider_id)
                REFERENCES sys_service_providers (id)
                ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

-- Only one non-cancelled subscription per provider at a time.
CREATE UNIQUE INDEX IF NOT EXISTS uq_provider_active_subscription
    ON sys_customer_subscriptions (provider_id)
    WHERE status IN ('TRIAL','ACTIVE','PAST_DUE');

CREATE INDEX IF NOT EXISTS idx_sys_subscriptions_provider
    ON sys_customer_subscriptions (provider_id);
CREATE INDEX IF NOT EXISTS idx_sys_subscriptions_status
    ON sys_customer_subscriptions (status);

-- -------------------------------------------------------------------------
-- Seat-expansion add-ons
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_subscription_add_ons (
    id               UUID          NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    subscription_id  UUID          NOT NULL REFERENCES sys_customer_subscriptions (id) ON DELETE CASCADE,
    add_on_code      VARCHAR(50)   NOT NULL,
    display_name     VARCHAR(100)  NOT NULL,
    extra_seats      INTEGER       NOT NULL DEFAULT 0,
    price            NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    status           VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    activated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_add_on_status  CHECK (status IN ('ACTIVE','CANCELLED','EXPIRED')),
    CONSTRAINT chk_add_on_seats   CHECK (extra_seats >= 0)
);

CREATE INDEX IF NOT EXISTS idx_sys_add_ons_subscription
    ON sys_subscription_add_ons (subscription_id);
CREATE INDEX IF NOT EXISTS idx_sys_add_ons_code_status
    ON sys_subscription_add_ons (add_on_code, status);
