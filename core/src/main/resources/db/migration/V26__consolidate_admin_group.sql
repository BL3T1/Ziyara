-- ============================================================================
-- V26 — Replace Z1–Z7 organizational groups with a single Admin group (C1)
--
-- Steps:
--   1. Insert the C1 Admin group (idempotent)
--   2. Redirect every sys_roles.group_id that points at a Z-group → C1
--   3. Sync sys_user_roles.group_id to match their role's new group
--   4. Delete the seven Z-group rows (FK references cleared above)
-- ============================================================================

-- [1] Insert Admin group.
-- Use ON CONFLICT ON CONSTRAINT uk_sys_groups_code so this is idempotent even when C1
-- already exists with a non-canonical UUID (created by PlatformOrgGroupCodeMigrationRunner
-- on a pre-V26 database). V27 normalises the UUID to the canonical value afterwards.
INSERT INTO sys_groups (id, name, code, description, created_at)
VALUES ('b0000000-0000-0000-0000-000000000010', 'Admin', 'C1', 'Platform administrative group', now())
ON CONFLICT ON CONSTRAINT uk_sys_groups_code DO NOTHING;

-- [2] Redirect all roles from Z-groups → C1.
-- Use a subquery so the actual C1 UUID is used regardless of whether the row above was
-- inserted fresh (canonical UUID) or already existed (possibly non-canonical UUID).
UPDATE sys_roles
SET group_id = (SELECT id FROM sys_groups WHERE code = 'C1')
WHERE group_id IN (
    'b0000000-0000-0000-0000-000000000001'::uuid,
    'b0000000-0000-0000-0000-000000000002'::uuid,
    'b0000000-0000-0000-0000-000000000003'::uuid,
    'b0000000-0000-0000-0000-000000000004'::uuid,
    'b0000000-0000-0000-0000-000000000005'::uuid,
    'b0000000-0000-0000-0000-000000000006'::uuid,
    'b0000000-0000-0000-0000-000000000007'::uuid
);

-- [3] Sync sys_user_roles.group_id
UPDATE sys_user_roles ur
SET group_id = r.group_id
FROM sys_roles r
WHERE ur.role_id = r.id
  AND (ur.group_id IS DISTINCT FROM r.group_id);

-- [4] Remove the Z1–Z7 groups (no FK references remain)
DELETE FROM sys_groups WHERE code IN ('Z1','Z2','Z3','Z4','Z5','Z6','Z7');
