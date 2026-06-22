-- ============================================================================
-- V36 — Fine-grained per-tab permissions for the deleted items recycle bin.
--       Admins can now grant read-only or restore access per category:
--         deleted_items:company:read  / deleted_items:company:restore
--         deleted_items:providers:read / deleted_items:providers:restore
--         deleted_items:users:read    / deleted_items:users:restore
-- ============================================================================

INSERT INTO sys_permissions (id, code, name, resource, action, scope, is_locked)
VALUES
    ('d1000000-0000-0000-0000-000000000001', 'deleted_items:company:read',     'Deleted Company Items Read',     'deleted_items', 'company_read',     'ALL', false),
    ('d1000000-0000-0000-0000-000000000002', 'deleted_items:company:restore',  'Deleted Company Items Restore',  'deleted_items', 'company_restore',  'ALL', false),
    ('d1000000-0000-0000-0000-000000000003', 'deleted_items:providers:read',   'Deleted Providers Read',         'deleted_items', 'providers_read',   'ALL', false),
    ('d1000000-0000-0000-0000-000000000004', 'deleted_items:providers:restore','Deleted Providers Restore',      'deleted_items', 'providers_restore','ALL', false),
    ('d1000000-0000-0000-0000-000000000005', 'deleted_items:users:read',       'Deleted App Users Read',         'deleted_items', 'users_read',       'ALL', false),
    ('d1000000-0000-0000-0000-000000000006', 'deleted_items:users:restore',    'Deleted App Users Restore',      'deleted_items', 'users_restore',    'ALL', false)
ON CONFLICT (code) DO NOTHING;

-- Grant all 6 to SUPER_ADMIN (already has deleted_items:read / restore; add the specific ones too)
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN (
    'deleted_items:company:read','deleted_items:company:restore',
    'deleted_items:providers:read','deleted_items:providers:restore',
    'deleted_items:users:read','deleted_items:users:restore'
  )
ON CONFLICT DO NOTHING;
