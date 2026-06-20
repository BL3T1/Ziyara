-- Migrate legacy platform group codes G1..G7 → Z1..Z7 (UUIDs unchanged).
-- By id only: avoids unique(code) conflicts if Z* rows already exist.
-- Pre-flight (run manually if needed): database/scripts/org_groups_inventory.sql

UPDATE sys_groups SET code = 'Z1' WHERE id = 'b0000000-0000-0000-0000-000000000001'::uuid AND code ~ '^G[0-9]+$';
UPDATE sys_groups SET code = 'Z2' WHERE id = 'b0000000-0000-0000-0000-000000000002'::uuid AND code ~ '^G[0-9]+$';
UPDATE sys_groups SET code = 'Z3' WHERE id = 'b0000000-0000-0000-0000-000000000003'::uuid AND code ~ '^G[0-9]+$';
UPDATE sys_groups SET code = 'Z4' WHERE id = 'b0000000-0000-0000-0000-000000000004'::uuid AND code ~ '^G[0-9]+$';
UPDATE sys_groups SET code = 'Z5' WHERE id = 'b0000000-0000-0000-0000-000000000005'::uuid AND code ~ '^G[0-9]+$';
UPDATE sys_groups SET code = 'Z6' WHERE id = 'b0000000-0000-0000-0000-000000000006'::uuid AND code ~ '^G[0-9]+$';
UPDATE sys_groups SET code = 'Z7' WHERE id = 'b0000000-0000-0000-0000-000000000007'::uuid AND code ~ '^G[0-9]+$';

UPDATE sys_user_roles ur
SET group_id = r.group_id
FROM sys_roles r
WHERE ur.role_id = r.id
  AND (ur.group_id IS DISTINCT FROM r.group_id);
