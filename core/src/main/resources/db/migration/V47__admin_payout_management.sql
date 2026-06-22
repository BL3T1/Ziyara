-- V47: Admin payout management – extend portal_payout_requests for ops workflow
-- Adds new status values, admin-side columns, and indexes for the Finance > Payouts page.

-- Add admin workflow columns to portal_payout_requests
ALTER TABLE portal_payout_requests
    ADD COLUMN IF NOT EXISTS processed_at      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS processed_by      UUID,
    ADD COLUMN IF NOT EXISTS rejection_reason  TEXT,
    ADD COLUMN IF NOT EXISTS notes             TEXT,
    ADD COLUMN IF NOT EXISTS transaction_id    TEXT,
    ADD COLUMN IF NOT EXISTS scheduled_at      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS is_manual         BOOLEAN NOT NULL DEFAULT FALSE;

-- Index for admin list queries (status + recency)
CREATE INDEX IF NOT EXISTS idx_payout_req_status_requested
    ON portal_payout_requests (status, requested_at DESC);

-- Index for provider-scoped admin queries
CREATE INDEX IF NOT EXISTS idx_payout_req_provider_status
    ON portal_payout_requests (provider_id, status);

-- Add payouts:read and payouts:write permissions to the permission catalogue
INSERT INTO sys_permissions (id, code, name, resource, action)
VALUES
    (gen_random_uuid(), 'payouts:read',    'View payouts',    'payouts', 'read'),
    (gen_random_uuid(), 'payouts:write',   'Manage payouts',  'payouts', 'write'),
    (gen_random_uuid(), 'payouts:approve', 'Approve payouts', 'payouts', 'approve')
ON CONFLICT (code) DO NOTHING;

-- Grant payouts:read + payouts:write to finance and super_admin system roles
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code IN ('SUPER_ADMIN', 'ADMIN', 'FINANCE')
  AND p.code IN ('payouts:read', 'payouts:write', 'payouts:approve')
ON CONFLICT DO NOTHING;

-- Grant payouts:read to executive
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'EXECUTIVE'
  AND p.code = 'payouts:read'
ON CONFLICT DO NOTHING;
