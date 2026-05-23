-- Admin activity audit log: immutable record of every privileged action
CREATE TABLE IF NOT EXISTS sys_admin_activity_log (
    id            UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id      UUID    NOT NULL REFERENCES sys_users(id) ON DELETE SET NULL,
    action        VARCHAR(100) NOT NULL,
    target_type   VARCHAR(50),
    target_id     VARCHAR(255),
    description   TEXT,
    ip_address    VARCHAR(45),
    user_agent    TEXT,
    performed_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    metadata      JSONB
);

CREATE INDEX IF NOT EXISTS idx_sys_admin_log_admin
    ON sys_admin_activity_log (admin_id, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_sys_admin_log_action
    ON sys_admin_activity_log (action, performed_at DESC);

CREATE INDEX IF NOT EXISTS idx_sys_admin_log_target
    ON sys_admin_activity_log (target_type, target_id);

COMMENT ON TABLE  sys_admin_activity_log            IS 'Immutable audit trail of all privileged admin actions';
COMMENT ON COLUMN sys_admin_activity_log.action     IS 'Machine-readable action name, e.g. BOOKING_REJECTED, USER_SUSPENDED';
COMMENT ON COLUMN sys_admin_activity_log.target_type IS 'Entity type acted upon, e.g. BOOKING, USER, DISCOUNT';
COMMENT ON COLUMN sys_admin_activity_log.metadata   IS 'Arbitrary JSON payload capturing before/after state or extra context';
