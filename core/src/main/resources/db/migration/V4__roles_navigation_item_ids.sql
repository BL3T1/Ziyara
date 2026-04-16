-- Custom RBAC role sidebar: ordered list of dashboard nav item IDs (matches front/my-app sidebar item ids).
ALTER TABLE sys_roles
    ADD COLUMN IF NOT EXISTS navigation_item_ids JSONB;
