-- V76: Add identity_verifications nav item to SUPER_ADMIN and ADMIN roles.

UPDATE sys_roles
SET navigation_item_ids = navigation_item_ids || '["identity_verifications"]'::jsonb
WHERE code IN ('SUPER_ADMIN', 'ADMIN')
  AND navigation_item_ids IS NOT NULL
  AND NOT (navigation_item_ids @> '["identity_verifications"]'::jsonb);
