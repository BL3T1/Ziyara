-- V75: Add profile_edit_requests to SUPER_ADMIN role nav and fix admin_cash gap.
UPDATE sys_roles
SET navigation_item_ids = navigation_item_ids || '["profile_edit_requests"]'::jsonb
WHERE code = 'SUPER_ADMIN'
  AND navigation_item_ids IS NOT NULL
  AND NOT (navigation_item_ids @> '["profile_edit_requests"]'::jsonb);
