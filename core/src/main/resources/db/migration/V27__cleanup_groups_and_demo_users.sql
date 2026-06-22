-- ============================================================================
-- V27 — Normalize C1 UUID and remove all demo/legacy data.
--
-- Steps:
--   1. Normalize C1 group UUID to the canonical value (b0000000-...-000000000010)
--      in case it was created with a random UUID on pre-V26 databases.
--   2. Remove any remaining Z1-Z7 legacy groups (belt-and-suspenders after V26).
--   3. Delete all business/test data that would block user deletion.
--   4. Delete all users except super_admin@ziyarah.com.
--   5. Ensure super_admin role assignment points to C1.
-- ============================================================================

-- === [1] Normalize C1 UUID to canonical value ===
-- On a fresh database V26 already inserts C1 with the canonical UUID so this
-- block is a no-op.  On existing databases where PlatformOrgGroupCodeMigrationRunner
-- previously created C1 with a random UUID we swap it to the canonical one so that
-- PlatformRbacCatalogValidator (which looks up by UUID) passes on next startup.
DO $$
DECLARE
    old_c1_id         uuid;
    canonical_c1_id   CONSTANT uuid := 'b0000000-0000-0000-0000-000000000010';
BEGIN
    SELECT id INTO old_c1_id FROM sys_groups WHERE code = 'C1';

    -- Nothing to do: C1 is missing (fresh DB) or already has the canonical UUID.
    IF old_c1_id IS NULL OR old_c1_id = canonical_c1_id THEN
        RETURN;
    END IF;

    -- Temporarily drop constraints that prevent a UUID swap.
    ALTER TABLE sys_roles      DROP CONSTRAINT IF EXISTS fk_sys_roles_group;
    ALTER TABLE sys_user_roles DROP CONSTRAINT IF EXISTS sys_user_roles_group_id_fkey;
    ALTER TABLE sys_groups     DROP CONSTRAINT IF EXISTS uk_sys_groups_code;

    -- Insert canonical row, copying name/description from the existing C1.
    INSERT INTO sys_groups (id, name, code, description, created_at)
    SELECT canonical_c1_id, name, code, description, created_at
    FROM sys_groups WHERE id = old_c1_id;

    -- Redirect all FK references from old UUID → canonical UUID.
    UPDATE sys_roles      SET group_id = canonical_c1_id WHERE group_id = old_c1_id;
    UPDATE sys_user_roles SET group_id = canonical_c1_id WHERE group_id = old_c1_id;

    -- Remove the old non-canonical C1 row.
    DELETE FROM sys_groups WHERE id = old_c1_id;

    -- Restore constraints.
    ALTER TABLE sys_groups ADD CONSTRAINT uk_sys_groups_code UNIQUE (code);
    ALTER TABLE sys_roles  ADD CONSTRAINT fk_sys_roles_group
        FOREIGN KEY (group_id) REFERENCES sys_groups(id);
    ALTER TABLE sys_user_roles ADD CONSTRAINT sys_user_roles_group_id_fkey
        FOREIGN KEY (group_id) REFERENCES sys_groups(id);
END $$;

-- === [2] Remove any remaining Z1-Z7 groups (belt-and-suspenders after V26) ===
UPDATE sys_roles
SET group_id = 'b0000000-0000-0000-0000-000000000010'
WHERE group_id IN (SELECT id FROM sys_groups WHERE code IN ('Z1','Z2','Z3','Z4','Z5','Z6','Z7'));

UPDATE sys_user_roles ur
SET group_id = r.group_id
FROM sys_roles r
WHERE ur.role_id = r.id AND (ur.group_id IS DISTINCT FROM r.group_id);

DELETE FROM sys_groups WHERE code IN ('Z1','Z2','Z3','Z4','Z5','Z6','Z7');

-- === [3] Delete all business/test data to unblock user deletion ===

-- RESTRICT FK: employees must go before their users.
DELETE FROM sys_employees;

-- Payment chain (NO ACTION): refunds → payments → bookings.
DELETE FROM pay_refunds;
DELETE FROM pay_payments;

-- Complaints (CASCADE removes support_complaint_comments, clearing that user FK too).
DELETE FROM support_complaints;

-- Reviews must be deleted before bookings — booking_id is NOT NULL in hotel_reviews,
-- so the ON DELETE SET NULL FK would fail if bookings go first.
DELETE FROM hotel_reviews;

-- Bookings (CASCADE removes bkg_taxi_bookings).
DELETE FROM bkg_bookings;

-- Internal tickets (CASCADE removes support_ticket_comments, clearing that user FK too).
DELETE FROM support_internal_tickets;

-- Nullify nullable user references so the user rows can be deleted.
UPDATE disc_discount_codes     SET approved_by = NULL, created_by = NULL;
UPDATE hotel_service_providers SET approved_by = NULL, created_by = NULL;
UPDATE portal_payout_requests  SET processed_by = NULL;
UPDATE sys_consent_audit_log   SET changed_by = NULL;
UPDATE sys_feature_flags       SET updated_by = NULL;

-- === [4] Delete all users except super_admin (CASCADE removes sessions, notifications, etc.) ===
DELETE FROM sys_users WHERE email != 'super_admin@ziyarah.com';

-- === [5] Ensure super_admin role assignment is in C1 ===
UPDATE sys_user_roles
SET group_id = 'b0000000-0000-0000-0000-000000000010'
WHERE user_id  = (SELECT id FROM sys_users WHERE email = 'super_admin@ziyarah.com')
  AND group_id IS DISTINCT FROM 'b0000000-0000-0000-0000-000000000010'::uuid;
