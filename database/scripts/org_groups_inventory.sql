-- Inventory: platform organizational groups (run before/after G→Z migration).
-- PostgreSQL: LIKE 'Z%' is broader than reserved slice; use ~ for Z + digits only.

SELECT g.id, g.code, g.name,
       (SELECT coalesce(json_agg(json_build_object('id', r.id, 'code', r.code, 'system', r.is_system_role)), '[]'::json)
        FROM sys_roles r WHERE r.group_id = g.id) AS roles
FROM sys_groups g
WHERE g.code ~ '^G[0-9]+$'
ORDER BY g.code;

SELECT g.id, g.code, g.name,
       (SELECT coalesce(json_agg(json_build_object('id', r.id, 'code', r.code, 'system', r.is_system_role)), '[]'::json)
        FROM sys_roles r WHERE r.group_id = g.id) AS roles
FROM sys_groups g
WHERE g.code ~ '^Z[0-9]+$'
ORDER BY g.code;
