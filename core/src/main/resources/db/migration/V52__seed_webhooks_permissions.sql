-- V52: Seed webhooks:read and webhooks:write as first-class ABAC permissions.
-- These were missing from sys_permissions, causing the COMPANY_STAFF gate to reject
-- custom roles that held only webhook permissions.

INSERT INTO sys_permissions (id, code, name, description, resource, action, scope, is_locked)
VALUES
    ('a3000000-0000-0000-0000-000000000001', 'webhooks:read',  'View Webhooks',   'View webhook subscriptions and delivery logs', 'webhooks', 'read',  'ALL', false),
    ('a3000000-0000-0000-0000-000000000002', 'webhooks:write', 'Manage Webhooks', 'Create, update, and delete webhook subscriptions', 'webhooks', 'write', 'ALL', false)
ON CONFLICT (id) DO NOTHING;

-- Grant both to SUPER_ADMIN
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r
CROSS JOIN sys_permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('webhooks:read', 'webhooks:write')
ON CONFLICT DO NOTHING;
