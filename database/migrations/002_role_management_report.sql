-- ============================================================================
-- ROLE MANAGEMENT REPORT - Schema extensions
-- Run after 001_plans_schema_extensions.sql
-- ============================================================================

-- Role operational status (ACTIVE, PENDING_REASSIGNMENT) - VARCHAR for JPA EnumType.STRING
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'roles' AND column_name = 'status'
    ) THEN
        ALTER TABLE roles ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
    END IF;
END $$;
COMMENT ON COLUMN roles.status IS 'ACTIVE = functional; PENDING_REASSIGNMENT = custom role marked for deletion, read-only until users reassigned';

-- Permissions: locked permissions cannot be assigned to custom roles (e.g. sys:db_write)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema() AND table_name = 'permissions' AND column_name = 'is_locked'
    ) THEN
        ALTER TABLE permissions ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT false;
    END IF;
END $$;
COMMENT ON COLUMN permissions.is_locked IS 'If true, cannot be assigned to custom roles (system-only)';
