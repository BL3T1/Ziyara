-- V77: Grant every permission to SUPER_ADMIN that was added after V17.
-- Idempotent: ON CONFLICT DO NOTHING skips already-assigned rows.
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT
    'c0000000-0000-0000-0000-000000000001'::uuid,
    id
FROM sys_permissions
ON CONFLICT DO NOTHING;
