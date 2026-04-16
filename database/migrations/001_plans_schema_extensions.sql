-- ============================================================================
-- ZIYARAH PLANS - Schema extensions (i18n, RBAC role attributes)
-- Run after main schema.sql
-- ============================================================================

-- i18n_labels: static UI translations (EN/AR) per REQUIREMENTS_ANALYSIS & Phase 4
CREATE TABLE IF NOT EXISTS i18n_labels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key VARCHAR(255) NOT NULL,
    en TEXT NOT NULL,
    ar TEXT,
    module VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_i18n_labels_key_module ON i18n_labels (key, COALESCE(module, ''));

CREATE INDEX IF NOT EXISTS idx_i18n_labels_key ON i18n_labels(key);
CREATE INDEX IF NOT EXISTS idx_i18n_labels_module ON i18n_labels(module);

COMMENT ON TABLE i18n_labels IS 'Static UI translations for EN/AR (i18n)';

-- roles: add is_system_role for RBAC (ROLE_MANAGEMENT_REPORT) - system roles cannot be deleted
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'roles' AND column_name = 'is_system_role'
    ) THEN
        ALTER TABLE roles ADD COLUMN is_system_role BOOLEAN NOT NULL DEFAULT true;
    END IF;
END $$;
COMMENT ON COLUMN roles.is_system_role IS 'If true, role is pre-defined and cannot be deleted; custom roles are false';
