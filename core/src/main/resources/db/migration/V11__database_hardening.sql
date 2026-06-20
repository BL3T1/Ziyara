-- ============================================================================
-- Database hardening: security metadata, consent, password history, indexes,
-- audit archive, retention/export/PII registry, session columns.
-- Targets POST-015 prefixed tables (sys_*, bkg_*, hotel_*, support_*, pay_*).
-- Idempotent: safe to re-run with IF NOT EXISTS.
-- ============================================================================

-- --- Column additions required before index creation -----------------------
-- hotel_reviews.responded_by was omitted from V0; add it here idempotently.
ALTER TABLE hotel_reviews ADD COLUMN IF NOT EXISTS responded_by UUID REFERENCES sys_users (id) ON DELETE SET NULL;
ALTER TABLE hotel_reviews ADD COLUMN IF NOT EXISTS responded_at TIMESTAMPTZ;
ALTER TABLE hotel_reviews ADD COLUMN IF NOT EXISTS provider_response TEXT;

-- --- Indexes (no CONCURRENTLY inside explicit transaction in Flyway) -------
CREATE INDEX IF NOT EXISTS idx_sys_user_roles_role_id ON sys_user_roles (role_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_roles_group_id ON sys_user_roles (group_id);
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_discount_code_id ON bkg_bookings (discount_code_id);
CREATE INDEX IF NOT EXISTS idx_support_complaints_resolved_by ON support_complaints (resolved_by);
CREATE INDEX IF NOT EXISTS idx_hotel_reviews_responded_by ON hotel_reviews (responded_by);

CREATE INDEX IF NOT EXISTS idx_bkg_bookings_customer_status
    ON bkg_bookings (customer_id, status)
    WHERE status NOT IN ('CANCELLED', 'EXPIRED');

CREATE INDEX IF NOT EXISTS idx_bkg_bookings_check_in_status
    ON bkg_bookings (check_in_date, status)
    WHERE status IN ('PENDING', 'CONFIRMED', 'ACTIVE');

CREATE INDEX IF NOT EXISTS idx_pay_payments_booking_status ON pay_payments (booking_id, status);
CREATE INDEX IF NOT EXISTS idx_pay_payments_pending_created
    ON pay_payments (booking_id, created_at)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX IF NOT EXISTS idx_hotel_services_provider_status_type
    ON hotel_services (provider_id, status, type);

CREATE INDEX IF NOT EXISTS idx_hotel_services_active_location
    ON hotel_services (provider_id, type, city)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_support_complaints_open_queue
    ON support_complaints (status, priority, created_at)
    WHERE status NOT IN ('CLOSED', 'RESOLVED', 'REJECTED');

CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_entity_timeline
    ON sys_audit_logs (entity_type, entity_id, created_at DESC);

-- --- sys_users: token invalidation, MFA, GDPR flags, password metadata -------
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS last_password_change TIMESTAMPTZ;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS password_expires_at TIMESTAMPTZ;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS mfa_type VARCHAR(20);
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS mfa_secret_cipher TEXT;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS mfa_backup_codes_cipher TEXT;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS mfa_last_used_at TIMESTAMPTZ;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS mfa_enrolled_at TIMESTAMPTZ;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS gdpr_consent_given BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS gdpr_consent_date TIMESTAMPTZ;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS marketing_opt_in BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS right_to_erasure_requested BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sys_users ADD COLUMN IF NOT EXISTS right_to_erasure_completed_at TIMESTAMPTZ;

-- --- Password history --------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_password_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_user_password_history_user ON sys_user_password_history (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_password_history_created ON sys_user_password_history (created_at DESC);

-- --- Consent -----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    consent_type VARCHAR(100) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    granted BOOLEAN NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawn_at TIMESTAMPTZ,
    withdrawal_reason TEXT,
    version INTEGER NOT NULL DEFAULT 1,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata JSONB,
    CONSTRAINT uk_sys_user_consents UNIQUE (user_id, consent_type, version)
);
CREATE INDEX IF NOT EXISTS idx_sys_user_consents_user ON sys_user_consents (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_consents_type ON sys_user_consents (consent_type);

CREATE TABLE IF NOT EXISTS sys_consent_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    consent_type VARCHAR(100) NOT NULL,
    previous_value BOOLEAN,
    new_value BOOLEAN,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by UUID REFERENCES sys_users (id),
    ip_address VARCHAR(45),
    user_agent TEXT
);
CREATE INDEX IF NOT EXISTS idx_sys_consent_audit_user ON sys_consent_audit_log (user_id);

-- --- Security events (application-populated) --------------------------------
CREATE TABLE IF NOT EXISTS sys_security_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES sys_users (id) ON DELETE SET NULL,
    event_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20),
    ip_address VARCHAR(45),
    user_agent TEXT,
    details JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_security_events_user ON sys_security_events (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_security_events_type ON sys_security_events (event_type);
CREATE INDEX IF NOT EXISTS idx_sys_security_events_created ON sys_security_events (created_at DESC);

-- --- Retention / export / PII registry --------------------------------------
CREATE TABLE IF NOT EXISTS sys_data_retention_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL UNIQUE,
    retention_period_days INTEGER NOT NULL,
    retention_condition VARCHAR(255),
    action VARCHAR(50) NOT NULL DEFAULT 'DELETE',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_execution TIMESTAMPTZ,
    next_execution TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_data_export_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    format VARCHAR(20) NOT NULL DEFAULT 'JSON',
    export_path VARCHAR(1000),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    failure_reason TEXT
);
CREATE INDEX IF NOT EXISTS idx_sys_data_export_user ON sys_data_export_requests (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_data_export_status ON sys_data_export_requests (status);

CREATE TABLE IF NOT EXISTS sys_pii_field_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    pii_category VARCHAR(50) NOT NULL,
    encryption_required BOOLEAN NOT NULL DEFAULT FALSE,
    gdpr_article VARCHAR(100),
    last_reviewed_at TIMESTAMPTZ,
    CONSTRAINT uk_sys_pii_field UNIQUE (table_name, column_name)
);

CREATE TABLE IF NOT EXISTS sys_backup_verification_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backup_date TIMESTAMPTZ NOT NULL,
    backup_type VARCHAR(50) NOT NULL,
    backup_size_bytes BIGINT,
    backup_location VARCHAR(500),
    verification_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    verification_started_at TIMESTAMPTZ,
    verification_completed_at TIMESTAMPTZ,
    verification_result TEXT,
    restore_test_performed BOOLEAN NOT NULL DEFAULT FALSE,
    checksum_valid BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- --- Optional PII ciphertext columns (customers not renamed in 015) --------
ALTER TABLE customers ADD COLUMN IF NOT EXISTS id_document_number_cipher TEXT;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS pii_encryption_version SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE hotel_service_providers ADD COLUMN IF NOT EXISTS bank_account_number_cipher TEXT;
ALTER TABLE hotel_service_providers ADD COLUMN IF NOT EXISTS tax_id_cipher TEXT;
ALTER TABLE hotel_service_providers ADD COLUMN IF NOT EXISTS pii_encryption_version SMALLINT NOT NULL DEFAULT 0;

ALTER TABLE pay_payments ADD COLUMN IF NOT EXISTS gateway_response_cipher TEXT;

-- --- sessions hardening (table name unchanged) ------------------------------
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS is_invalidated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS invalidated_at TIMESTAMPTZ;
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS invalidated_reason VARCHAR(255);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS created_from_ip VARCHAR(45);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS geographic_location VARCHAR(100);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS device_fingerprint VARCHAR(255);
ALTER TABLE sessions ADD COLUMN IF NOT EXISTS last_activity_ip VARCHAR(45);
CREATE INDEX IF NOT EXISTS idx_sessions_active_user
    ON sessions (user_id, expires_at)
    WHERE is_invalidated IS NOT TRUE;

-- --- Audit archive (cold storage; same shape as JPA sys_audit_logs) ----------
CREATE TABLE IF NOT EXISTS sys_audit_logs_archive (
    id UUID PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_name VARCHAR(255),
    entity_id VARCHAR(255),
    user_id UUID,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_archive_created ON sys_audit_logs_archive (created_at DESC);

-- Seed retention policies (job respects enabled flag)
INSERT INTO sys_data_retention_policies (entity_type, retention_period_days, retention_condition, action, enabled)
VALUES
    ('sys_otp_verification', 7, 'after expires_at', 'DELETE', TRUE),
    ('sys_password_reset_tokens', 7, 'after expires_at', 'DELETE', TRUE),
    ('sys_audit_logs', 730, 'archive rows older than retention', 'ARCHIVE', TRUE)
ON CONFLICT (entity_type) DO NOTHING;

-- Seed PII registry (subset; extend over time)
INSERT INTO sys_pii_field_registry (table_name, column_name, pii_category, encryption_required, gdpr_article) VALUES
    ('sys_users', 'email', 'DIRECT', TRUE, 'Art. 4(1)'),
    ('sys_users', 'phone', 'DIRECT', TRUE, 'Art. 4(1)'),
    ('sys_users', 'password_hash', 'SENSITIVE', TRUE, 'Art. 32'),
    ('customers', 'id_document_number', 'SENSITIVE', TRUE, 'Art. 9'),
    ('hotel_service_providers', 'bank_account_number', 'SENSITIVE', TRUE, 'Art. 9'),
    ('hotel_service_providers', 'tax_id', 'SENSITIVE', TRUE, 'Art. 9'),
    ('pay_payments', 'gateway_response', 'SENSITIVE', TRUE, 'Art. 9')
ON CONFLICT (table_name, column_name) DO NOTHING;
