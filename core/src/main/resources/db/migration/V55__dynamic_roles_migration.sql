-- ============================================================================
-- V55 — Dynamic Roles Migration
--
-- 1. Add max_discount_pct column to sys_roles
-- 2. Seed portal permission codes
-- 3. Create portal system roles (replaces static enum values)
-- 4. Assign portal permissions to portal system roles
-- 5. Create sys_user_roles rows for existing portal users (had none before)
-- 6. Deduplicate sys_user_roles — one row per user
-- 7. Add UNIQUE constraint: one role per user
-- 8. Migrate sys_users.role: all non-SUPER_ADMIN / non-CUSTOMER → STAFF
-- 9. Add CHECK constraint to lock in the 3-value system
-- ============================================================================

-- ── 1. max_discount_pct on sys_roles ─────────────────────────────────────────

ALTER TABLE sys_roles
    ADD COLUMN IF NOT EXISTS max_discount_pct SMALLINT NOT NULL DEFAULT 0;

UPDATE sys_roles SET max_discount_pct = 100 WHERE code = 'SUPER_ADMIN';
UPDATE sys_roles SET max_discount_pct = 100 WHERE code = 'CEO';
UPDATE sys_roles SET max_discount_pct = 50  WHERE code = 'FINANCE_MANAGER';
UPDATE sys_roles SET max_discount_pct = 30  WHERE code = 'ACCOUNTANT';
UPDATE sys_roles SET max_discount_pct = 20  WHERE code = 'SALES_MANAGER';
UPDATE sys_roles SET max_discount_pct = 20  WHERE code = 'SALES_REPRESENTATIVE';

-- ── 2. Portal permission codes ───────────────────────────────────────────────

INSERT INTO sys_permissions (code, name, resource, action, scope, is_locked)
VALUES
    ('portal:access',  'Portal Access',         'portal', 'access',  'ALL', FALSE),
    ('portal:manage',  'Portal Management',     'portal', 'manage',  'ALL', FALSE),
    ('portal:finance', 'Portal Finance Access', 'portal', 'finance', 'ALL', FALSE),
    ('portal:taxi',    'Portal Taxi Access',    'portal', 'taxi',    'ALL', FALSE)
ON CONFLICT (code) DO NOTHING;

-- ── 3. Portal system roles ───────────────────────────────────────────────────
-- These replace the four static UserRole enum values that are being removed.
-- Super admin can edit or replace these roles at any time.

INSERT INTO sys_roles (name, name_ar, code, level, is_system_role, status)
VALUES
    ('Provider Manager', 'مدير الشريك',       'PROVIDER_MANAGER', 'MANAGER',  TRUE, 'ACTIVE'),
    ('Provider Finance', 'مالية الشريك',      'PROVIDER_FINANCE', 'EMPLOYEE', TRUE, 'ACTIVE'),
    ('Provider Staff',   'موظف الشريك',       'PROVIDER_STAFF',   'EMPLOYEE', TRUE, 'ACTIVE'),
    ('Taxi Operator',    'مشغل سيارة الأجرة', 'TAXI_OPERATOR',    'EMPLOYEE', TRUE, 'ACTIVE')
ON CONFLICT (code) DO NOTHING;

-- ── 4. Assign portal permissions to portal system roles ──────────────────────

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'PROVIDER_MANAGER'
  AND p.code IN ('portal:access', 'portal:manage', 'portal:finance')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'PROVIDER_FINANCE'
  AND p.code IN ('portal:access', 'portal:finance')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'PROVIDER_STAFF'
  AND p.code IN ('portal:access')
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'TAXI_OPERATOR'
  AND p.code IN ('portal:access', 'portal:taxi')
ON CONFLICT DO NOTHING;

-- ── 5. Create sys_user_roles rows for existing portal users ──────────────────
-- Portal users previously had no sys_user_roles row (auth was via sys_users.role).
-- Match them to the new portal system roles by code.

INSERT INTO sys_user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, now()
FROM sys_users u
JOIN sys_roles r
    ON r.code = u.role
   AND r.is_system_role = TRUE
WHERE u.role IN ('PROVIDER_MANAGER', 'PROVIDER_FINANCE', 'PROVIDER_STAFF', 'TAXI_OPERATOR')
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_roles ur WHERE ur.user_id = u.id
  )
ON CONFLICT DO NOTHING;

-- ── 6. Deduplicate sys_user_roles — keep newest row per user ─────────────────

DELETE FROM sys_user_roles
WHERE id NOT IN (
    SELECT DISTINCT ON (user_id) id
    FROM sys_user_roles
    ORDER BY user_id, assigned_at DESC NULLS LAST, id DESC
);

-- ── 7. Unique constraint: one role per user ───────────────────────────────────

ALTER TABLE sys_user_roles
    DROP CONSTRAINT IF EXISTS uq_sys_user_roles_user_id;

ALTER TABLE sys_user_roles
    ADD CONSTRAINT uq_sys_user_roles_user_id UNIQUE (user_id);

-- ── 8. Migrate sys_users.role to 3-value system ───────────────────────────────

UPDATE sys_users
SET role = 'STAFF'
WHERE role NOT IN ('SUPER_ADMIN', 'CUSTOMER');

-- ── 9. Lock the column to 3 values ───────────────────────────────────────────

ALTER TABLE sys_users
    DROP CONSTRAINT IF EXISTS ck_sys_users_role;

ALTER TABLE sys_users
    ADD CONSTRAINT ck_sys_users_role
    CHECK (role IN ('SUPER_ADMIN', 'CUSTOMER', 'STAFF'));
