-- ============================================================================
-- V0 — Full baseline schema for Ziyara backend (PostgreSQL 15+)
--
-- Creates every table that JPA entities map to and that is NOT created by
-- V1–V13 migrations.  Uses CREATE TABLE IF NOT EXISTS throughout so it is
-- fully idempotent and safe to run against an already-populated database.
--
-- Tables created by later migrations are NOT listed here:
--   V2  : web_content_pages
--   V3  : sys_system_settings, support_contact_leads
--   V5  : hotel_portal_support_requests
--   V10 : kafka_staff_notification_delivered
--   V11 : sys_user_password_history, sys_user_consents, sys_consent_audit_log,
--          sys_security_events, sys_data_retention_policies,
--          sys_data_export_requests, sys_pii_field_registry,
--          sys_backup_verification_log, sys_audit_logs_archive
--   V12 : sys_rate_limit_counters, sys_security_alert_rules, sys_security_alerts
--
-- Existing databases (with Flyway baseline-on-migrate=true): V0 is marked as
-- applied without execution; V1-V13 re-run harmlessly (all IF NOT EXISTS).
-- ============================================================================

-- ---------------------------------------------------------------------------
-- RBAC & organisational structure
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_groups (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)  NOT NULL,
    name_ar     VARCHAR(100),
    code        VARCHAR(20)   NOT NULL,
    description TEXT,
    description_ar TEXT,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ,
    CONSTRAINT uk_sys_groups_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_departments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(100) NOT NULL,
    name_ar        VARCHAR(100),
    description    TEXT,
    description_ar TEXT,
    manager_id     UUID,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ,
    CONSTRAINT uk_sys_departments_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS sys_permissions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code           VARCHAR(100) NOT NULL,
    name           VARCHAR(100) NOT NULL,
    name_ar        VARCHAR(100),
    description    TEXT,
    description_ar TEXT,
    resource       VARCHAR(100) NOT NULL,
    action         VARCHAR(50)  NOT NULL,
    scope          VARCHAR(50)  NOT NULL DEFAULT 'ALL',
    is_locked      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_sys_permissions_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_roles (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(50)  NOT NULL,
    name_ar           VARCHAR(100),
    code              VARCHAR(30)  NOT NULL,
    description       TEXT,
    description_ar    TEXT,
    level             VARCHAR(30)  NOT NULL DEFAULT 'EMPLOYEE',
    group_id          UUID,
    is_system_role    BOOLEAN      NOT NULL DEFAULT TRUE,
    status            VARCHAR(20)  DEFAULT 'ACTIVE',
    navigation_item_ids JSONB,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ,
    CONSTRAINT uk_sys_roles_name UNIQUE (name),
    CONSTRAINT uk_sys_roles_code UNIQUE (code),
    CONSTRAINT fk_sys_roles_group FOREIGN KEY (group_id) REFERENCES sys_groups (id)
);

-- ---------------------------------------------------------------------------
-- Core users
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_users (
    id                           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                        VARCHAR(255) NOT NULL,
    phone                        VARCHAR(50),
    password_hash                VARCHAR(255) NOT NULL,
    role                         VARCHAR(50)  NOT NULL,
    status                       VARCHAR(50)  NOT NULL,
    email_verified               BOOLEAN      NOT NULL DEFAULT FALSE,
    phone_verified               BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts        INTEGER      NOT NULL DEFAULT 0,
    locked_until                 TIMESTAMPTZ,
    last_login_at                TIMESTAMPTZ,
    last_login_ip                VARCHAR(45),
    created_at                   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMPTZ,
    deleted_at                   TIMESTAMPTZ,
    -- added by V11 (idempotent ALTER TABLE guards already there):
    token_version                INTEGER      NOT NULL DEFAULT 0,
    last_password_change         TIMESTAMPTZ,
    password_expires_at          TIMESTAMPTZ,
    mfa_enabled                  BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_type                     VARCHAR(20),
    mfa_secret_cipher            TEXT,
    mfa_backup_codes_cipher      TEXT,
    mfa_last_used_at             TIMESTAMPTZ,
    mfa_enrolled_at              TIMESTAMPTZ,
    gdpr_consent_given           BOOLEAN      NOT NULL DEFAULT FALSE,
    gdpr_consent_date            TIMESTAMPTZ,
    marketing_opt_in             BOOLEAN      NOT NULL DEFAULT FALSE,
    right_to_erasure_requested   BOOLEAN      NOT NULL DEFAULT FALSE,
    right_to_erasure_completed_at TIMESTAMPTZ,
    CONSTRAINT uk_sys_users_email UNIQUE (email),
    CONSTRAINT uk_sys_users_phone UNIQUE (phone)
);

CREATE INDEX IF NOT EXISTS idx_sys_users_email ON sys_users (email);
CREATE INDEX IF NOT EXISTS idx_sys_users_status ON sys_users (status);

-- Customer extended profile (user_id PK references sys_users)
CREATE TABLE IF NOT EXISTS customers (
    user_id              UUID PRIMARY KEY REFERENCES sys_users (id) ON DELETE CASCADE,
    first_name           VARCHAR(100) NOT NULL,
    last_name            VARCHAR(100) NOT NULL,
    id_document_url      VARCHAR(500),
    id_document_type     VARCHAR(50),
    id_document_number   VARCHAR(100),
    preferred_currency   VARCHAR(3)   DEFAULT 'USD',
    profile_image_url    VARCHAR(500),
    date_of_birth        DATE,
    nationality          VARCHAR(100),
    -- added by V11:
    id_document_number_cipher TEXT,
    pii_encryption_version    SMALLINT NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_customers_name ON customers (first_name, last_name);

-- Session tracking (referenced by V11 for hardening columns)
CREATE TABLE IF NOT EXISTS sessions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID         NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    token_hash           VARCHAR(255) NOT NULL,
    refresh_token_hash   VARCHAR(255),
    device_type          VARCHAR(50),
    device_info          TEXT,
    ip_address           VARCHAR(45),
    user_agent           TEXT,
    expires_at           TIMESTAMPTZ  NOT NULL,
    refresh_expires_at   TIMESTAMPTZ,
    -- added by V11:
    is_invalidated       BOOLEAN      NOT NULL DEFAULT FALSE,
    invalidated_at       TIMESTAMPTZ,
    invalidated_reason   VARCHAR(255),
    created_from_ip      VARCHAR(45),
    geographic_location  VARCHAR(100),
    device_fingerprint   VARCHAR(255),
    last_activity_ip     VARCHAR(45),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sessions_token_hash         UNIQUE (token_hash),
    CONSTRAINT uk_sessions_refresh_token_hash UNIQUE (refresh_token_hash)
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_expires ON sessions (user_id, expires_at);

CREATE TABLE IF NOT EXISTS sys_employees (
    user_id       UUID PRIMARY KEY REFERENCES sys_users (id) ON DELETE CASCADE,
    department_id UUID REFERENCES sys_departments (id),
    employee_code VARCHAR(20)  NOT NULL,
    level         VARCHAR(30)  NOT NULL DEFAULT 'EMPLOYEE',
    job_title     VARCHAR(100),
    hire_date     DATE         NOT NULL,
    created_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ,
    CONSTRAINT uk_sys_employees_code UNIQUE (employee_code)
);

CREATE TABLE IF NOT EXISTS sys_role_permissions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id       UUID NOT NULL REFERENCES sys_roles (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES sys_permissions (id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_permissions UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_user_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    role_id     UUID        NOT NULL REFERENCES sys_roles (id) ON DELETE CASCADE,
    group_id    UUID        REFERENCES sys_groups (id),
    assigned_at TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_sys_user_roles_user_id  ON sys_user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_roles_role_id  ON sys_user_roles (role_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_roles_group_id ON sys_user_roles (group_id);

-- Audit log (base shape; V11 adds archive table; V12 adds enrichment columns idempotently)
CREATE TABLE IF NOT EXISTS sys_audit_logs (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    action         VARCHAR(255) NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_name    TEXT,
    entity_id      VARCHAR(255),
    user_id        UUID,
    old_value      TEXT,
    new_value      TEXT,
    ip_address     VARCHAR(50),
    user_agent     TEXT,
    -- V12 columns included here so fresh DBs skip the ALTER TABLE:
    correlation_id VARCHAR(100),
    request_id     VARCHAR(100),
    session_id     UUID,
    provider_id    UUID,
    tenant_id      UUID,
    risk_score     INTEGER,
    duration_ms    INTEGER,
    tags           TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_user       ON sys_audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_entity_type ON sys_audit_logs (entity_type);
CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_created    ON sys_audit_logs (created_at DESC);

-- OTP and password-reset tokens (base tables; needed before V11 FK references)
CREATE TABLE IF NOT EXISTS sys_otp_verification (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email_or_phone VARCHAR(255) NOT NULL,
    otp            VARCHAR(10)  NOT NULL,
    expires_at     TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_otp_email_or_phone ON sys_otp_verification (email_or_phone);

CREATE TABLE IF NOT EXISTS sys_password_reset_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sys_password_reset_tokens_user ON sys_password_reset_tokens (user_id);

-- Notifications
CREATE TABLE IF NOT EXISTS sys_notifications (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    type          VARCHAR(100) NOT NULL,
    channel       VARCHAR(50)  NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    title         VARCHAR(200),
    message       TEXT,
    template_name VARCHAR(255),
    metadata      TEXT,
    sent_at       TIMESTAMPTZ,
    read_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_sys_notifications_user   ON sys_notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_sys_notifications_status ON sys_notifications (status);

-- ---------------------------------------------------------------------------
-- Provider & service catalogue
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS hotel_service_providers (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name        VARCHAR(255) NOT NULL,
    company_name_ar     VARCHAR(255),
    contact_email       VARCHAR(255) NOT NULL,
    contact_phone       VARCHAR(20)  NOT NULL,
    address             TEXT         NOT NULL,
    website             VARCHAR(255),
    logo_url            VARCHAR(500),
    description         TEXT,
    description_ar      TEXT,
    provider_type       VARCHAR(64),
    registration_number VARCHAR(128),
    rating              DOUBLE PRECISION DEFAULT 0.0,
    review_count        INTEGER          DEFAULT 0,
    status              VARCHAR(50)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    verified            BOOLEAN          DEFAULT FALSE,
    commission_rate     DECIMAL(5, 2),
    created_by          UUID         REFERENCES sys_users (id),
    approved_by         UUID         REFERENCES sys_users (id),
    approved_at         TIMESTAMPTZ,
    -- added by V11:
    bank_account_number_cipher TEXT,
    tax_id_cipher              TEXT,
    pii_encryption_version     SMALLINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_hotel_service_providers_status ON hotel_service_providers (status);

CREATE TABLE IF NOT EXISTS hotel_provider_staff (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID        NOT NULL REFERENCES hotel_service_providers (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES sys_users (id) ON DELETE CASCADE,
    title       VARCHAR(100),
    provider_role VARCHAR(30),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_hotel_provider_staff_provider_user UNIQUE (provider_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_hotel_provider_staff_provider_id ON hotel_provider_staff (provider_id);
CREATE INDEX IF NOT EXISTS idx_hotel_provider_staff_user_id     ON hotel_provider_staff (user_id);

CREATE TABLE IF NOT EXISTS hotel_services (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id         UUID        NOT NULL REFERENCES hotel_service_providers (id),
    type                VARCHAR(50)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    description         TEXT,
    location            VARCHAR(255),
    address             TEXT,
    city                VARCHAR(100),
    country             VARCHAR(100),
    latitude            DECIMAL(10, 8),
    longitude           DECIMAL(11, 8),
    base_price          DECIMAL(12, 2) NOT NULL,
    currency            VARCHAR(3)   DEFAULT 'USD',
    status              VARCHAR(50)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    attributes          JSONB,
    amenities           JSONB,
    policies            TEXT,
    star_rating         INTEGER,
    total_rooms         INTEGER,
    available_rooms     INTEGER,
    max_guests          INTEGER      DEFAULT 1,
    seasonal_multiplier DECIMAL(5, 2) DEFAULT 1.00,
    tax_rate            DECIMAL(5, 4) DEFAULT 0.0000,
    check_in_time       TIME,
    check_out_time      TIME,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ,
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_hotel_services_provider_id ON hotel_services (provider_id);
CREATE INDEX IF NOT EXISTS idx_hotel_services_status      ON hotel_services (status);
CREATE INDEX IF NOT EXISTS idx_hotel_services_type        ON hotel_services (type);

CREATE TABLE IF NOT EXISTS hotel_service_images (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id    UUID        NOT NULL REFERENCES hotel_services (id) ON DELETE CASCADE,
    url           VARCHAR(500) NOT NULL,
    alt_text      VARCHAR(255),
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    category      VARCHAR(32)  NOT NULL DEFAULT 'PROPERTY',
    context_key   VARCHAR(100),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_hotel_service_images_service_id ON hotel_service_images (service_id);

CREATE TABLE IF NOT EXISTS hotel_service_rooms (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id         UUID        NOT NULL REFERENCES hotel_services (id) ON DELETE CASCADE,
    room_type          VARCHAR(64)  NOT NULL,
    room_name          VARCHAR(255) NOT NULL,
    description        TEXT,
    capacity           INTEGER      NOT NULL DEFAULT 1,
    base_price         DECIMAL(12, 2),
    currency           VARCHAR(3),
    quantity_total     INTEGER      NOT NULL DEFAULT 0,
    quantity_available INTEGER      NOT NULL DEFAULT 0,
    amenities          JSONB,
    status             VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    sort_order         INTEGER      NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hotel_service_rooms_service_id ON hotel_service_rooms (service_id);

CREATE TABLE IF NOT EXISTS hotel_service_room_images (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id       UUID        NOT NULL REFERENCES hotel_service_rooms (id) ON DELETE CASCADE,
    url           VARCHAR(500) NOT NULL,
    alt_text      VARCHAR(255),
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hotel_service_room_images_room_id ON hotel_service_room_images (room_id);

CREATE TABLE IF NOT EXISTS hotel_rest_menu_sections (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID        NOT NULL REFERENCES hotel_services (id) ON DELETE CASCADE,
    title      VARCHAR(255) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hotel_rest_menu_sections_service_id ON hotel_rest_menu_sections (service_id);

CREATE TABLE IF NOT EXISTS hotel_rest_menu_items (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id  UUID        NOT NULL REFERENCES hotel_rest_menu_sections (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       DECIMAL(12, 2),
    currency    VARCHAR(3),
    image_url   VARCHAR(500),
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_hotel_rest_menu_items_section_id ON hotel_rest_menu_items (section_id);

CREATE TABLE IF NOT EXISTS hotel_reviews (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID        NOT NULL,
    customer_id UUID        NOT NULL REFERENCES sys_users (id),
    service_id  UUID        NOT NULL REFERENCES hotel_services (id),
    rating      INTEGER     NOT NULL,
    comment     TEXT,
    response    TEXT,
    status      VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_hotel_reviews_service_id  ON hotel_reviews (service_id);
CREATE INDEX IF NOT EXISTS idx_hotel_reviews_customer_id ON hotel_reviews (customer_id);

-- ---------------------------------------------------------------------------
-- Bookings
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS disc_discount_codes (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code                        VARCHAR(50)  NOT NULL,
    description                 TEXT,
    description_ar              TEXT,
    type                        VARCHAR(20)  NOT NULL,
    value                       DECIMAL(12, 2) NOT NULL,
    min_booking_amount          DECIMAL(12, 2) DEFAULT 0.00,
    max_discount_amount         DECIMAL(12, 2),
    start_date                  TIMESTAMPTZ,
    end_date                    TIMESTAMPTZ,
    usage_limit                 INTEGER,
    usage_count                 INTEGER      DEFAULT 0,
    status                      VARCHAR(50)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_by                  UUID         REFERENCES sys_users (id),
    sponsor                     VARCHAR(20)  NOT NULL DEFAULT 'COMPANY',
    provider_id                 UUID         REFERENCES hotel_service_providers (id),
    company_share_pct           DECIMAL(5, 2) DEFAULT 100.00,
    provider_share_pct          DECIMAL(5, 2) DEFAULT 0.00,
    approval_status             VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    approved_by                 UUID         REFERENCES sys_users (id),
    approved_at                 TIMESTAMPTZ,
    applicable_service_ids      JSONB,
    applicable_menu_section_ids JSONB,
    applicable_menu_item_ids    JSONB,
    applicable_room_type_ids    JSONB,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ,
    CONSTRAINT uk_disc_discount_codes_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS bkg_bookings (
    id                              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_reference               VARCHAR(20)  NOT NULL,
    customer_id                     UUID        NOT NULL REFERENCES sys_users (id),
    service_id                      UUID        NOT NULL REFERENCES hotel_services (id),
    discount_code_id                UUID        REFERENCES disc_discount_codes (id),
    check_in_date                   DATE        NOT NULL,
    check_out_date                  DATE,
    guests                          INTEGER      NOT NULL DEFAULT 1,
    rooms                           INTEGER      DEFAULT 1,
    base_amount                     DECIMAL(12, 2) NOT NULL,
    discount_amount                 DECIMAL(12, 2) DEFAULT 0.00,
    tax_amount                      DECIMAL(12, 2) DEFAULT 0.00,
    commission_amount               DECIMAL(12, 2) DEFAULT 0.00,
    total_amount                    DECIMAL(12, 2) NOT NULL,
    currency                        VARCHAR(3)   DEFAULT 'USD',
    status                          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    special_requests                TEXT,
    id_document_url                 VARCHAR(500),
    id_document_verified            BOOLEAN      DEFAULT FALSE,
    confirmed_at                    TIMESTAMPTZ,
    cancelled_at                    TIMESTAMPTZ,
    cancellation_reason             TEXT,
    cancelled_by                    UUID,
    rejection_reason                TEXT,
    delay_reason                    TEXT,
    internal_notes                  TEXT,
    rejected_at                     TIMESTAMPTZ,
    rejected_by                     UUID,
    discount_context_menu_item_ids  JSONB,
    discount_context_menu_section_ids JSONB,
    discount_context_room_type_id   UUID,
    created_at                      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMPTZ,
    CONSTRAINT uk_bkg_bookings_reference UNIQUE (booking_reference)
);

CREATE INDEX IF NOT EXISTS idx_bkg_bookings_customer_id ON bkg_bookings (customer_id);
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_service_id  ON bkg_bookings (service_id);
CREATE INDEX IF NOT EXISTS idx_bkg_bookings_status      ON bkg_bookings (status);

CREATE TABLE IF NOT EXISTS bkg_taxi_bookings (
    id                    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id            UUID        NOT NULL REFERENCES bkg_bookings (id) ON DELETE CASCADE,
    driver_id             UUID,
    vehicle_type          VARCHAR(50),
    pickup_location       TEXT,
    destination_location  TEXT,
    pickup_latitude       DOUBLE PRECISION,
    pickup_longitude      DOUBLE PRECISION,
    destination_latitude  DOUBLE PRECISION,
    destination_longitude DOUBLE PRECISION,
    scheduled_at          TIMESTAMPTZ,
    started_at            TIMESTAMPTZ,
    completed_at          TIMESTAMPTZ,
    estimated_distance    DECIMAL(8, 2),
    actual_distance       DECIMAL(8, 2),
    estimated_price       DECIMAL(12, 2),
    actual_price          DECIMAL(12, 2),
    status                VARCHAR(50)  NOT NULL DEFAULT 'SEARCHING',
    license_plate         VARCHAR(20),
    driver_name           VARCHAR(255),
    vehicle_model         VARCHAR(255),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ
);

-- ---------------------------------------------------------------------------
-- Payments
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS pay_payments (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id       UUID        NOT NULL REFERENCES bkg_bookings (id),
    amount           DECIMAL(12, 2) NOT NULL,
    currency         VARCHAR(3)   DEFAULT 'USD',
    method           VARCHAR(50)  NOT NULL,
    status           VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    transaction_ref  VARCHAR(255),
    gateway_reference VARCHAR(255),
    three_ds_status  VARCHAR(50),
    gateway_response TEXT,
    gateway_name     VARCHAR(255),
    payment_token    VARCHAR(255),
    idempotency_key  VARCHAR(64),
    error_message    TEXT,
    entity_type      VARCHAR(30),
    entity_id        UUID,
    category         VARCHAR(50),
    processed_at     TIMESTAMPTZ,
    -- added by V11:
    gateway_response_cipher TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ,
    CONSTRAINT uk_pay_payments_transaction_ref  UNIQUE (transaction_ref),
    CONSTRAINT uk_pay_payments_idempotency_key  UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_pay_payments_booking_id ON pay_payments (booking_id);
CREATE INDEX IF NOT EXISTS idx_pay_payments_status     ON pay_payments (status);

CREATE TABLE IF NOT EXISTS pay_refunds (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id           UUID        NOT NULL REFERENCES pay_payments (id),
    amount               DECIMAL(12, 2) NOT NULL,
    currency             VARCHAR(3)   DEFAULT 'USD',
    status               VARCHAR(50)  NOT NULL DEFAULT 'REQUESTED',
    reason               TEXT,
    processed_by         UUID,
    transaction_reference VARCHAR(255),
    processed_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS pay_exchange_rates (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency  VARCHAR(3)  NOT NULL,
    to_currency    VARCHAR(3)  NOT NULL,
    rate           DECIMAL(18, 6) NOT NULL,
    effective_date DATE        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ
);

-- ---------------------------------------------------------------------------
-- Support
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS support_complaints (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number     VARCHAR(20)  NOT NULL,
    customer_id       UUID        NOT NULL REFERENCES sys_users (id),
    booking_id        UUID        REFERENCES bkg_bookings (id),
    subject           VARCHAR(255) NOT NULL,
    description       TEXT        NOT NULL,
    priority          VARCHAR(30)  NOT NULL DEFAULT 'MEDIUM',
    status            VARCHAR(50)  NOT NULL DEFAULT 'SUBMITTED',
    category          VARCHAR(100),
    assigned_agent_id UUID,
    assigned_at       TIMESTAMPTZ,
    resolution_notes  TEXT,
    resolved_at       TIMESTAMPTZ,
    resolved_by       UUID,
    escalated_at      TIMESTAMPTZ,
    escalated_to      UUID,
    closed_at         TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ,
    CONSTRAINT uk_support_complaints_ticket_number UNIQUE (ticket_number)
);

CREATE INDEX IF NOT EXISTS idx_support_complaints_customer_id ON support_complaints (customer_id);
CREATE INDEX IF NOT EXISTS idx_support_complaints_status      ON support_complaints (status);

CREATE TABLE IF NOT EXISTS support_complaint_comments (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    complaint_id UUID        NOT NULL REFERENCES support_complaints (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES sys_users (id),
    comment      TEXT        NOT NULL,
    is_internal  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS support_internal_tickets (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number       VARCHAR(20)  NOT NULL,
    reporter_id         UUID        NOT NULL REFERENCES sys_users (id),
    type                VARCHAR(50)  NOT NULL,
    subject             VARCHAR(255) NOT NULL,
    description         TEXT        NOT NULL,
    priority            VARCHAR(30)  NOT NULL DEFAULT 'MEDIUM',
    status              VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    module              VARCHAR(100),
    sub_module          VARCHAR(100),
    environment         VARCHAR(50),
    browser             VARCHAR(100),
    operating_system    VARCHAR(100),
    steps_to_reproduce  TEXT,
    expected_behavior   TEXT,
    actual_behavior     TEXT,
    attachments         JSONB,
    assigned_to_id      UUID,
    assigned_at         TIMESTAMPTZ,
    estimated_hours     DECIMAL(5, 2),
    actual_hours        DECIMAL(5, 2),
    due_date            TIMESTAMPTZ,
    resolution_notes    TEXT,
    resolution_summary  TEXT,
    resolved_at         TIMESTAMPTZ,
    resolved_by         UUID,
    verified_at         TIMESTAMPTZ,
    verified_by         UUID,
    closed_at           TIMESTAMPTZ,
    closed_by           UUID,
    cancelled_at        TIMESTAMPTZ,
    cancelled_by        UUID,
    cancellation_reason TEXT,
    related_ticket_id   UUID,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_support_internal_tickets_number UNIQUE (ticket_number)
);

CREATE TABLE IF NOT EXISTS support_ticket_comments (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id     UUID        NOT NULL REFERENCES support_internal_tickets (id) ON DELETE CASCADE,
    user_id       UUID        NOT NULL REFERENCES sys_users (id),
    comment       TEXT        NOT NULL,
    is_internal   BOOLEAN     NOT NULL DEFAULT TRUE,
    is_resolution BOOLEAN     NOT NULL DEFAULT FALSE,
    attachments   JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------------
-- Feature flags & integration keys (added by database/migrations/024; not in V1-V13)
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sys_feature_flags (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key    VARCHAR(128) NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    description TEXT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  UUID         REFERENCES sys_users (id),
    CONSTRAINT uk_sys_feature_flags_key UNIQUE (flag_key)
);

CREATE TABLE IF NOT EXISTS sys_integration_api_keys (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    key_prefix  VARCHAR(32)  NOT NULL,
    secret_hash VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at  TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_sys_integration_api_keys_revoked ON sys_integration_api_keys (revoked_at);
