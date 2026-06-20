-- V61: Granular provider portal permissions + maxPayoutRequestAmount on roles
-- Adds 10 new portal:* permission codes and a per-role payout cap attribute.

-- ── 1. New permission codes ───────────────────────────────────────────────────
INSERT INTO sys_permissions (id, code, name, resource, action, scope, is_locked)
VALUES
  (gen_random_uuid(), 'portal:bookings:read',    'Portal – View Bookings',         'portal', 'bookings:read',    'ALL', FALSE),
  (gen_random_uuid(), 'portal:bookings:manage',  'Portal – Manage Bookings',       'portal', 'bookings:manage',  'ALL', FALSE),
  (gen_random_uuid(), 'portal:services:manage',  'Portal – Manage Listings',       'portal', 'services:manage',  'ALL', FALSE),
  (gen_random_uuid(), 'portal:staff:manage',     'Portal – Manage Staff',          'portal', 'staff:manage',     'ALL', FALSE),
  (gen_random_uuid(), 'portal:reports:read',     'Portal – View Reports',          'portal', 'reports:read',     'ALL', FALSE),
  (gen_random_uuid(), 'portal:payouts:request',  'Portal – Request Payouts',       'portal', 'payouts:request',  'ALL', FALSE),
  (gen_random_uuid(), 'portal:discounts:manage', 'Portal – Manage Discounts',      'portal', 'discounts:manage', 'ALL', FALSE),
  (gen_random_uuid(), 'portal:media:submit',     'Portal – Submit Media',          'portal', 'media:submit',     'ALL', FALSE),
  (gen_random_uuid(), 'portal:support:write',    'Portal – Open Support Tickets',  'portal', 'support:write',    'ALL', FALSE),
  (gen_random_uuid(), 'portal:menu:manage',      'Portal – Manage Restaurant Menu','portal', 'menu:manage',      'ALL', FALSE)
ON CONFLICT (code) DO NOTHING;

-- ── 2. Add max_payout_request_amount to sys_roles (NULL = unlimited) ─────────
ALTER TABLE sys_roles
    ADD COLUMN IF NOT EXISTS max_payout_request_amount NUMERIC(15, 2) NULL;

-- ── 3. Grant new permissions to existing system portal roles ──────────────────
-- PROVIDER_MANAGER: all portal permissions
INSERT INTO sys_role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM sys_roles r
CROSS JOIN sys_permissions p
WHERE r.code = 'PROVIDER_MANAGER'
  AND p.code IN (
    'portal:bookings:read', 'portal:bookings:manage',
    'portal:services:manage', 'portal:staff:manage',
    'portal:reports:read', 'portal:payouts:request',
    'portal:discounts:manage', 'portal:media:submit',
    'portal:support:write', 'portal:menu:manage'
  )
ON CONFLICT DO NOTHING;

-- PROVIDER_FINANCE: reports + payouts
INSERT INTO sys_role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM sys_roles r
CROSS JOIN sys_permissions p
WHERE r.code = 'PROVIDER_FINANCE'
  AND p.code IN ('portal:reports:read', 'portal:payouts:request')
ON CONFLICT DO NOTHING;

-- PROVIDER_STAFF: bookings read, service/menu/media/support management
INSERT INTO sys_role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM sys_roles r
CROSS JOIN sys_permissions p
WHERE r.code = 'PROVIDER_STAFF'
  AND p.code IN (
    'portal:bookings:read',
    'portal:services:manage',
    'portal:menu:manage',
    'portal:media:submit',
    'portal:support:write'
  )
ON CONFLICT DO NOTHING;

-- TAXI_OPERATOR: bookings read + manage
INSERT INTO sys_role_permissions (id, role_id, permission_id)
SELECT gen_random_uuid(), r.id, p.id
FROM sys_roles r
CROSS JOIN sys_permissions p
WHERE r.code = 'TAXI_OPERATOR'
  AND p.code IN ('portal:bookings:read', 'portal:bookings:manage')
ON CONFLICT DO NOTHING;
