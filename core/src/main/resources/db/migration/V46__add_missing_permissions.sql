-- ============================================================================
-- V46 — Add missing permissions referenced in COMPANY_STAFF expression
--        but absent from sys_permissions; also seed built-in role assignments.
-- ============================================================================

-- providers_messages (provider chat / messaging) was referenced in COMPANY_STAFF
-- but never inserted into sys_permissions, making it unassignable via the admin UI.
INSERT INTO sys_permissions (id, code, name, description, resource, action, scope, is_locked)
VALUES
    ('f0000000-0000-0000-0000-000000000001', 'providers_messages:read',  'View provider messages',  'Read provider-sent messages and threads',  'providers_messages', 'read',  'ALL', false),
    ('f0000000-0000-0000-0000-000000000002', 'providers_messages:write', 'Manage provider messages','Reply to and manage provider message threads','providers_messages', 'write', 'ALL', false)
ON CONFLICT (code) DO NOTHING;

-- Grant providers_messages to SUPER_ADMIN
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('providers_messages:read', 'providers_messages:write')
ON CONFLICT DO NOTHING;

-- Grant providers_messages:read/write to roles that handle provider-facing work
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code IN ('CEO', 'SALES_MANAGER', 'SUPPORT_MANAGER', 'SUPPORT_AGENT')
  AND p.code IN ('providers_messages:read', 'providers_messages:write')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code IN ('SALES_REPRESENTATIVE')
  AND p.code = 'providers_messages:read'
ON CONFLICT DO NOTHING;

-- Grant approval/publish/moderate permissions to CEO (V39 only gave them to SUPER_ADMIN)
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'CEO'
  AND p.code IN (
    'providers:approve',
    'media_submissions:approve',
    'services:publish',
    'reviews:moderate'
  )
ON CONFLICT DO NOTHING;

-- Grant media_submissions:approve and services:publish to SALES_MANAGER
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SALES_MANAGER'
  AND p.code IN ('providers:approve', 'media_submissions:approve', 'services:publish', 'reviews:moderate')
ON CONFLICT DO NOTHING;
