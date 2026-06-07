-- ============================================================================
-- V54 — Ensure all company staff users have a sys_user_roles row.
--
-- If autoAssignPrimaryRoleByUserRole silently skipped a user (because
-- sys_roles had no row matching their UserRole enum at the time of creation),
-- findPermissionCodesByUserId returns an empty list and every endpoint returns
-- 403.  This migration back-fills the missing rows so all existing staff can
-- log in immediately after deployment.
--
-- Logic:
--   For each company staff user (role NOT in the excluded set) who has NO
--   entry in sys_user_roles, insert a row pointing to the sys_roles row whose
--   code matches the user's enum role column.
--
-- Idempotent: ON CONFLICT DO NOTHING + NOT EXISTS guard.
-- ============================================================================

INSERT INTO sys_user_roles (user_id, role_id, group_id, assigned_at)
SELECT u.id, r.id, r.group_id, now()
FROM sys_users u
JOIN sys_roles r ON r.code = u.role
WHERE u.role NOT IN (
        'CUSTOMER',
        'PROVIDER_MANAGER',
        'PROVIDER_FINANCE',
        'PROVIDER_STAFF',
        'TAXI_OPERATOR'
    )
  AND NOT EXISTS (
        SELECT 1 FROM sys_user_roles ur WHERE ur.user_id = u.id
    )
ON CONFLICT DO NOTHING;
