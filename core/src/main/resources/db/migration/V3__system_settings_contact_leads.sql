-- Admin key-value settings and public marketing contact leads (Phase 2).
-- updated_by is a soft reference to sys_users(id) when present in full schema; no FK for Flyway-only test DBs.

CREATE TABLE IF NOT EXISTS sys_system_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(128) NOT NULL UNIQUE,
    value_json TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID NULL
);

CREATE TABLE IF NOT EXISTS support_contact_leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    message TEXT NOT NULL,
    client_ip VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_support_contact_leads_email_created ON support_contact_leads (email, created_at DESC);
