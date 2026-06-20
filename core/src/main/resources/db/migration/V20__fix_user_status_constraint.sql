-- V20: Fix chk_sys_users_status constraint to align with UserStatus Java enum.
--
-- V16 added: ACTIVE, INACTIVE, SUSPENDED, PENDING_VERIFICATION, PENDING_APPROVAL, LOCKED
-- Java enum has: ACTIVE, INACTIVE, FROZEN, PENDING_VERIFICATION, DELETED
--
-- Missing: FROZEN (used by UserCommandHandler.freeze()), DELETED (used by softDelete())
-- Extra (not in enum, never written by app): SUSPENDED, PENDING_APPROVAL, LOCKED
--
-- We DROP the old constraint and recreate it with the full correct set so that
-- both freeze() and softDelete() succeed, and any legacy rows with SUSPENDED /
-- PENDING_APPROVAL / LOCKED (e.g. seeded data) are also valid.

DO $$
BEGIN
    -- Drop old constraint if present
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'chk_sys_users_status'
          AND table_name      = 'sys_users'
    ) THEN
        ALTER TABLE sys_users DROP CONSTRAINT chk_sys_users_status;
    END IF;

    -- Recreate with all values the application can write
    ALTER TABLE sys_users
        ADD CONSTRAINT chk_sys_users_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE',
            'FROZEN',
            'PENDING_VERIFICATION',
            'DELETED',
            -- legacy values kept for backward compatibility with any existing rows
            'SUSPENDED',
            'PENDING_APPROVAL',
            'LOCKED'
        ));
END $$;
