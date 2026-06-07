-- ============================================================================
-- V33 — Seed permission assignments for all built-in system roles
--
-- V17 seeded SUPER_ADMIN with every permission.
-- This migration seeds the remaining built-in roles so that Spring Security
-- @PreAuthorize(hasAuthority('permission:code')) works for ALL users —
-- whether they carry a built-in JWT role or a custom RBAC role.
--
-- Role IDs are resolved by CODE (not hardcoded UUID) so this migration is
-- safe to run on any database regardless of when V17 ran or whether older
-- role rows were already present with different UUIDs.
--
-- Every INSERT uses ON CONFLICT DO NOTHING (idempotent / safe to re-run).
-- ============================================================================

-- ============================================================================
-- CEO — Full business access; excludes system:super_ops, system:bulk_export,
--       and roles:write (role management is SUPER_ADMIN only).
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'CEO'
  AND p.code IN (
    'bookings:read','bookings:write',
    'users:read','users:write',
    'providers:read','providers:write',
    'payments:read','payments:write',
    'discounts:read','discounts:write','discounts:approve',
    'reports:read','analytics:read',
    'roles:read',
    'settings:read','settings:write',
    'audit:read',
    'content:read','content:write',
    'services:read','services:write',
    'taxi:read','taxi:write',
    'currency:read','currency:write',
    'tickets:read','tickets:write',
    'complaints:read','complaints:write',
    'reviews:read','reviews:write',
    'customers:read','customers:write',
    'internal_tickets:read','internal_tickets:write',
    'portal:read',
    'api_docs:read',
    'leads:read','leads:write',
    'deleted_items:read','deleted_items:restore'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- GENERAL_MANAGER — Broad management; no users:write, no discounts:approve,
--                   no roles:write.
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'GENERAL_MANAGER'
  AND p.code IN (
    'bookings:read','bookings:write',
    'users:read',
    'providers:read','providers:write',
    'payments:read','payments:write',
    'discounts:read','discounts:write',
    'reports:read','analytics:read',
    'roles:read',
    'settings:read','settings:write',
    'audit:read',
    'content:read','content:write',
    'services:read','services:write',
    'taxi:read','taxi:write',
    'currency:read','currency:write',
    'tickets:read','tickets:write',
    'complaints:read','complaints:write',
    'reviews:read','reviews:write',
    'customers:read','customers:write',
    'internal_tickets:read',
    'portal:read',
    'api_docs:read',
    'leads:read','leads:write',
    'deleted_items:read'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- SALES_MANAGER
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SALES_MANAGER'
  AND p.code IN (
    'bookings:read','bookings:write',
    'providers:read','providers:write',
    'discounts:read','discounts:write',
    'reports:read','analytics:read',
    'content:read','content:write',
    'services:read','services:write',
    'taxi:read','taxi:write',
    'reviews:read','reviews:write',
    'customers:read',
    'leads:read','leads:write',
    'deleted_items:read'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- SALES_REPRESENTATIVE
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SALES_REPRESENTATIVE'
  AND p.code IN (
    'bookings:read',
    'providers:read',
    'discounts:read','discounts:write',
    'analytics:read',
    'content:read','content:write',
    'services:read','services:write',
    'taxi:read',
    'reviews:read',
    'leads:read','leads:write'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- FINANCE_MANAGER
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'FINANCE_MANAGER'
  AND p.code IN (
    'bookings:read',
    'providers:read',
    'payments:read','payments:write',
    'discounts:read','discounts:approve',
    'reports:read','analytics:read',
    'audit:read',
    'currency:read','currency:write'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- ACCOUNTANT
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'ACCOUNTANT'
  AND p.code IN (
    'bookings:read',
    'payments:read',
    'discounts:read',
    'reports:read','analytics:read',
    'currency:read','currency:write'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- SUPPORT_MANAGER
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SUPPORT_MANAGER'
  AND p.code IN (
    'bookings:read','bookings:write',
    'users:read',
    'analytics:read',
    'tickets:read','tickets:write',
    'complaints:read','complaints:write',
    'reviews:read','reviews:write',
    'customers:read','customers:write',
    'internal_tickets:read','internal_tickets:write'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- SUPPORT_AGENT
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'SUPPORT_AGENT'
  AND p.code IN (
    'bookings:read',
    'tickets:read','tickets:write',
    'complaints:read','complaints:write',
    'reviews:read',
    'customers:read'
  )
ON CONFLICT DO NOTHING;

-- ============================================================================
-- HR_MANAGER
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM sys_roles r, sys_permissions p
WHERE r.code = 'HR_MANAGER'
  AND p.code IN (
    'users:read','users:write',
    'discounts:write',
    'roles:read',
    'audit:read',
    'internal_tickets:read','internal_tickets:write'
  )
ON CONFLICT DO NOTHING;
