-- Matches core Flyway V4: RBAC role sidebar nav item ids (JSONB). Required before 029 cleanup runs.
ALTER TABLE sys_roles
    ADD COLUMN IF NOT EXISTS navigation_item_ids JSONB;
