-- V18: Add FCM push token column to sys_users for mobile push notifications
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);
CREATE INDEX IF NOT EXISTS idx_sys_users_fcm_token ON sys_users (fcm_token) WHERE fcm_token IS NOT NULL;
