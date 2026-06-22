-- V40: Add granular reset-password permission.
-- Allows delegating password-reset capability without granting full users:write.
-- Bearer of this permission cannot reset SUPER_ADMIN accounts (enforced in UserController).

INSERT INTO sys_permissions (id, code, name, description, resource, action, scope, is_locked)
VALUES
    ('e2000000-0000-0000-0000-000000000001', 'users:reset_password', 'Reset User Passwords',
     'Reset passwords for any non-Super-Admin user account', 'users', 'reset_password', 'ALL', false)
ON CONFLICT (id) DO NOTHING;

-- Grant to SUPER_ADMIN automatically
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code = 'users:reset_password'
ON CONFLICT DO NOTHING;
