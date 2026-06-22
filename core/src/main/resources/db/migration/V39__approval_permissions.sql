-- V39: New granular approval/publish/moderate permissions.
-- Allows admins to delegate specific approval workflows to non-super-admin roles.

INSERT INTO sys_permissions (id, code, name, description, resource, action, scope, is_locked)
VALUES
    ('e1000000-0000-0000-0000-000000000001', 'media_submissions:approve', 'Approve Media Uploads',
     'Approve or reject provider image submissions',          'media_submissions', 'approve',  'ALL', false),
    ('e1000000-0000-0000-0000-000000000002', 'providers:approve',         'Approve Providers',
     'Approve or reject new provider onboarding requests',   'providers',         'approve',  'ALL', false),
    ('e1000000-0000-0000-0000-000000000003', 'services:publish',          'Publish Services',
     'Publish or unpublish a service listing',               'services',          'publish',  'ALL', false),
    ('e1000000-0000-0000-0000-000000000004', 'reviews:moderate',          'Moderate Reviews',
     'Approve or reject customer reviews',                   'reviews',           'moderate', 'ALL', false)
ON CONFLICT (id) DO NOTHING;

-- Grant all 4 to SUPER_ADMIN automatically
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN (
    'media_submissions:approve',
    'providers:approve',
    'services:publish',
    'reviews:moderate'
  )
ON CONFLICT DO NOTHING;
