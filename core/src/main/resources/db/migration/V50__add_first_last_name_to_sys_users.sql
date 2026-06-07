-- ============================================================================
-- V50 — Add first_name / last_name to sys_users
--
-- Previously, names were only stored in the `customers` table (B2C accounts).
-- Staff and admin users had no name fields in sys_users, so their display
-- name in the dashboard was always null/empty.
--
-- These columns are nullable so existing rows are unaffected.
-- ============================================================================

ALTER TABLE sys_users
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_name  VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_sys_users_first_last_name ON sys_users (first_name, last_name)
    WHERE first_name IS NOT NULL OR last_name IS NOT NULL;
