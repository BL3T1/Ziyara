-- V28 — Remove group assignment from all roles except SUPER_ADMIN.
-- Only the super_admin role belongs to C1; all other roles are unassigned (group_id = NULL).
UPDATE sys_roles
SET group_id = NULL
WHERE code != 'SUPER_ADMIN';
