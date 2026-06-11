-- ============================================================================
-- V58 — Cash-First Payments
--
-- Adds the cash_collections table, a receipt-number sequence, default
-- payment_method=CASH for new bookings, and two new permissions.
--
-- pay_payments.status is plain VARCHAR(50) (no CHECK constraint), so the new
-- statuses RECONCILED / NO_SHOW_FORFEIT do not require a constraint rebuild.
-- ============================================================================

-- 1) Receipt sequence (used by ReceiptNumberGenerator → "CR-YYYYMMDD-NNNN").
CREATE SEQUENCE IF NOT EXISTS pay_cash_receipt_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- 2) Cash collections — per-payment ledger of cash received by providers.
CREATE TABLE IF NOT EXISTS pay_cash_collections (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id             UUID         NOT NULL REFERENCES pay_payments (id) ON DELETE RESTRICT,
    provider_id            UUID         NOT NULL REFERENCES hotel_service_providers (id),
    collected_at           TIMESTAMPTZ  NOT NULL,
    collected_by_user_id   UUID         NOT NULL REFERENCES sys_users (id),
    amount                 NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency               CHAR(3)      NOT NULL DEFAULT 'USD',
    receipt_number         VARCHAR(32)  NOT NULL,
    notes                  TEXT,
    reconciled_at          TIMESTAMPTZ,
    reconciled_by_user_id  UUID         REFERENCES sys_users (id),
    status                 VARCHAR(16)  NOT NULL DEFAULT 'OPEN'
                               CHECK (status IN ('OPEN','RECONCILED','DISPUTED')),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pay_cash_collections_receipt_number UNIQUE (receipt_number)
);

CREATE INDEX IF NOT EXISTS idx_pay_cash_collections_provider_status
    ON pay_cash_collections (provider_id, status);

CREATE INDEX IF NOT EXISTS idx_pay_cash_collections_payment
    ON pay_cash_collections (payment_id);

CREATE INDEX IF NOT EXISTS idx_pay_cash_collections_collected_at
    ON pay_cash_collections (collected_at);

-- 3) Default existing NULL booking payment methods to CASH (legacy rows untouched
--    when non-null). New bookings created post-deploy will inherit CASH via the
--    application default when cashOnlyMode=true.
UPDATE bkg_bookings
   SET payment_method = 'CASH'
 WHERE payment_method IS NULL;

-- 4) New permissions for cash workflow.
INSERT INTO sys_permissions (id, code, name, description, resource, action, scope, is_locked)
VALUES
    ('a8000000-0000-0000-0000-000000000001',
     'payments:cash-record',
     'Record cash collection',
     'Record cash received from a customer against a booking (provider portal).',
     'payments', 'cash-record', 'ALL', false),
    ('a8000000-0000-0000-0000-000000000002',
     'payments:cash-reconcile',
     'Reconcile cash collections',
     'Match cash collections against platform commission settlement (admin/finance).',
     'payments', 'cash-reconcile', 'ALL', false)
ON CONFLICT (code) DO NOTHING;

-- 5) Grant both permissions to SUPER_ADMIN.
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM sys_roles r, sys_permissions p
 WHERE r.code = 'SUPER_ADMIN'
   AND p.code IN ('payments:cash-record', 'payments:cash-reconcile')
ON CONFLICT DO NOTHING;

-- 6) Grant cash-reconcile to finance roles.
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM sys_roles r, sys_permissions p
 WHERE r.code IN ('CEO', 'FINANCE_MANAGER', 'ACCOUNTANT')
   AND p.code = 'payments:cash-reconcile'
ON CONFLICT DO NOTHING;

-- 7) Grant cash-record to COMPANY_STAFF (admin staff who may record cash on behalf of providers).
--    Note: provider portal users record cash via portal:finance — PortalCashController uses PORTAL_FINANCE.
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM sys_roles r, sys_permissions p
 WHERE r.code = 'COMPANY_STAFF'
   AND p.code = 'payments:cash-record'
ON CONFLICT DO NOTHING;
