-- V40: Ensure every SUPER_ADMIN user has a sys_user_roles row pointing to the SUPER_ADMIN sys_role.
--
-- Root cause: SuperAdminSeeder only called autoAssignPrimaryRoleByUserRole on first-create.
-- If the user existed before that logic was added (or if the row was deleted),
-- findPermissionCodesByUserId returns empty and every hasAuthority() check fails (403).
--
-- This migration is idempotent (ON CONFLICT DO NOTHING) and safe to re-run.

INSERT INTO sys_user_roles (user_id, role_id, group_id, assigned_at)
SELECT
    u.id,
    r.id,
    r.group_id,
    now()
FROM sys_users u
JOIN sys_roles r ON r.code = 'SUPER_ADMIN'
WHERE u.role = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_roles ur
      WHERE ur.user_id = u.id
  )
ON CONFLICT DO NOTHING;
