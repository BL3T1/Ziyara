-- Granular permission matrix: role × module × action
CREATE TABLE IF NOT EXISTS sys_permission_matrix (
    id          UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id     UUID    NOT NULL REFERENCES sys_roles(id) ON DELETE CASCADE,
    module      VARCHAR(60)  NOT NULL,
    action      VARCHAR(60)  NOT NULL,
    granted     BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_by  UUID REFERENCES sys_users(id) ON DELETE SET NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_permission_matrix_role_module_action UNIQUE (role_id, module, action)
);

CREATE INDEX IF NOT EXISTS idx_sys_permission_matrix_role
    ON sys_permission_matrix (role_id);

CREATE INDEX IF NOT EXISTS idx_sys_permission_matrix_module
    ON sys_permission_matrix (module, action);

COMMENT ON TABLE  sys_permission_matrix         IS 'Fine-grained permission overrides per role, module, and action';
COMMENT ON COLUMN sys_permission_matrix.module  IS 'UI/API module, e.g. BOOKINGS, REPORTS, PAYMENTS, USERS';
COMMENT ON COLUMN sys_permission_matrix.action  IS 'Operation, e.g. VIEW, CREATE, EDIT, DELETE, EXPORT, APPROVE';
COMMENT ON COLUMN sys_permission_matrix.granted IS 'TRUE = explicitly allowed; FALSE = explicitly denied';
