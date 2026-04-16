-- Feature flags (structured toggles) and integration API keys (hashed secrets) for admin Phase 3.
-- Run after 023. Idempotent.

CREATE TABLE IF NOT EXISTS sys_feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT false,
    description TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID REFERENCES sys_users(id),
    CONSTRAINT uk_sys_feature_flags_key UNIQUE (flag_key)
);

CREATE TABLE IF NOT EXISTS sys_integration_api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_sys_integration_api_keys_revoked ON sys_integration_api_keys(revoked_at);

COMMENT ON TABLE sys_feature_flags IS 'Named boolean feature toggles (beyond key/value JSON in sys_system_settings)';
COMMENT ON TABLE sys_integration_api_keys IS 'Server-side integration API keys; store bcrypt hash only';
