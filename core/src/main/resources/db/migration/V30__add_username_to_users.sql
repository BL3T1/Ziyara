ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS username VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_users_username ON sys_users(username) WHERE username IS NOT NULL;
