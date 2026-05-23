-- ============================================================================
-- V17 — Essential Reference Data
--
-- Contains the minimum seed rows required for the application to operate
-- correctly in ANY environment (dev, staging, production).
--
-- Rules:
--   • Every INSERT uses ON CONFLICT DO NOTHING (idempotent; safe to re-run).
--   • UUIDs use the fixed 'x0000000-…' series so downstream scripts and tests
--     can reference them by stable ID.
--   • This migration does NOT insert demo users — those are handled by:
--       dev/staging : DemoDataSeeder (@Profile("!prod"))
--       production  : first-run playbook (ops/FIRST_RUN_PROD.md)
-- ============================================================================

-- ============================================================================
-- [1] Organisational groups (Z1–Z7)
--     Validated at startup by app.rbac-catalog-validation. All seven must
--     exist or the application will warn/fail the catalog check.
-- ============================================================================
INSERT INTO sys_groups (id, name, code, description) VALUES
('b0000000-0000-0000-0000-000000000001', 'Executive',        'Z1', 'Executive leadership accounts'),
('b0000000-0000-0000-0000-000000000002', 'Sales',            'Z2', 'Sales team accounts'),
('b0000000-0000-0000-0000-000000000003', 'Finance',          'Z3', 'Finance and accounting accounts'),
('b0000000-0000-0000-0000-000000000004', 'Support',          'Z4', 'Customer support and service operations'),
('b0000000-0000-0000-0000-000000000005', 'HR & People',      'Z5', 'Human resources and internal people operations'),
('b0000000-0000-0000-0000-000000000006', 'Provider Partner', 'Z6', 'Partner and provider-facing accounts'),
('b0000000-0000-0000-0000-000000000007', 'B2C Customers',    'Z7', 'End-customer accounts (bookings and profile)')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- [2] System roles (one per UserRole enum value)
--     These rows back the RBAC catalogue. sys_users.role stores the enum name
--     directly; sys_roles provides display metadata and group membership.
-- ============================================================================
INSERT INTO sys_roles (id, name, code, description, level, group_id) VALUES
('c0000000-0000-0000-0000-000000000001', 'Super Admin',           'SUPER_ADMIN',           'Full system access',                    'SUPER_ADMIN', 'b0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000002', 'Sales Manager',         'SALES_MANAGER',         'Sales team lead',                       'MANAGER',     'b0000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000003', 'Customer',              'CUSTOMER',              'End customer (B2C)',                     'EMPLOYEE',    'b0000000-0000-0000-0000-000000000007'),
('c0000000-0000-0000-0000-000000000010', 'CEO',                   'CEO',                   'Chief Executive Officer',               'EXECUTIVE',   'b0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000011', 'General Manager',       'GENERAL_MANAGER',       'General management',                    'EXECUTIVE',   'b0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000012', 'Sales Representative',  'SALES_REPRESENTATIVE',  'Sales representative',                  'EMPLOYEE',    'b0000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000013', 'Finance Manager',       'FINANCE_MANAGER',       'Finance team lead',                     'MANAGER',     'b0000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000014', 'Accountant',            'ACCOUNTANT',            'Accounting',                            'EMPLOYEE',    'b0000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000015', 'Support Manager',       'SUPPORT_MANAGER',       'Support team lead',                     'MANAGER',     'b0000000-0000-0000-0000-000000000004'),
('c0000000-0000-0000-0000-000000000016', 'Support Agent',         'SUPPORT_AGENT',         'Customer support agent',                'EMPLOYEE',    'b0000000-0000-0000-0000-000000000004'),
('c0000000-0000-0000-0000-000000000017', 'HR Manager',            'HR_MANAGER',            'Human resources manager',               'MANAGER',     'b0000000-0000-0000-0000-000000000005'),
('c0000000-0000-0000-0000-000000000018', 'Provider Manager',      'PROVIDER_MANAGER',      'Partner account manager',               'MANAGER',     'b0000000-0000-0000-0000-000000000006'),
('c0000000-0000-0000-0000-000000000019', 'Provider Finance',      'PROVIDER_FINANCE',      'Partner finance',                       'EMPLOYEE',    'b0000000-0000-0000-0000-000000000006'),
('c0000000-0000-0000-0000-00000000001a', 'Provider Staff',        'PROVIDER_STAFF',        'Partner staff',                         'EMPLOYEE',    'b0000000-0000-0000-0000-000000000006'),
('c0000000-0000-0000-0000-00000000001b', 'Taxi Operator',         'TAXI_OPERATOR',         'Taxi operations',                       'EMPLOYEE',    'b0000000-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- [3] Permission catalogue
--     Full set of resource:action pairs. is_locked=true → only assignable to
--     system roles from the backend; never via the admin UI.
-- ============================================================================
INSERT INTO sys_permissions (id, code, name, description, resource, action, scope, is_locked) VALUES
('d0000000-0000-0000-0000-000000000001', 'bookings:read',            'View bookings',            'View all bookings',                    'bookings',         'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000002', 'bookings:write',           'Manage bookings',          'Create and update bookings',           'bookings',         'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000003', 'users:read',               'View users',               'View user list',                       'users',            'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000004', 'users:write',              'Manage users',             'Create and update users',              'users',            'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000005', 'providers:read',           'View providers',           'View service providers',               'providers',        'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000006', 'providers:write',          'Manage providers',         'Create and update providers',          'providers',        'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000007', 'payments:read',            'View payments',            'View payments',                        'payments',         'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000008', 'payments:write',           'Manage payments',          'Manage payment operations',            'payments',         'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000009', 'discounts:read',           'View discounts',           'View discount codes',                  'discounts',        'read',      'ALL', false),
('d0000000-0000-0000-0000-00000000000a', 'discounts:write',          'Manage discounts',         'Create and edit discounts',            'discounts',        'write',     'ALL', false),
('d0000000-0000-0000-0000-00000000000b', 'discounts:approve',        'Approve discounts',        'Activate pending discounts',           'discounts',        'approve',   'ALL', false),
('d0000000-0000-0000-0000-00000000000c', 'reports:read',             'View reports',             'Access management reports',            'reports',          'read',      'ALL', false),
('d0000000-0000-0000-0000-00000000000d', 'analytics:read',           'View analytics',           'Dashboard analytics',                  'analytics',        'read',      'ALL', false),
('d0000000-0000-0000-0000-00000000000e', 'roles:read',               'View roles',               'List roles and permission catalogue',  'roles',            'read',      'ALL', false),
('d0000000-0000-0000-0000-00000000000f', 'roles:write',              'Manage roles',             'Create and update roles/permissions',  'roles',            'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000010', 'settings:read',            'View settings',            'Read system settings',                 'settings',         'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000011', 'settings:write',           'Manage settings',          'Update system settings',               'settings',         'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000012', 'audit:read',               'View audit logs',          'Read audit trail',                     'audit',            'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000013', 'content:read',             'View content pages',       'Read landing/CMS content',             'content',          'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000014', 'content:write',            'Edit content pages',       'Edit landing/CMS content',             'content',          'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000015', 'services:read',            'View services',            'View catalog services',                'services',         'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000016', 'services:write',           'Manage services',          'Create and update services',           'services',         'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000017', 'taxi:read',                'View taxi bookings',       'Taxi operations read',                 'taxi',             'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000018', 'taxi:write',               'Manage taxi bookings',     'Taxi operations write',                'taxi',             'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000019', 'currency:read',            'View exchange rates',      'Read FX rates',                        'currency',         'read',      'ALL', false),
('d0000000-0000-0000-0000-00000000001a', 'currency:write',           'Manage exchange rates',    'Update FX rates',                      'currency',         'write',     'ALL', false),
('d0000000-0000-0000-0000-00000000001b', 'tickets:read',             'View tickets',             'Support tickets read',                 'tickets',          'read',      'ALL', false),
('d0000000-0000-0000-0000-00000000001c', 'tickets:write',            'Manage tickets',           'Support tickets write',                'tickets',          'write',     'ALL', false),
('d0000000-0000-0000-0000-00000000001d', 'complaints:read',          'View complaints',          'Complaints read',                      'complaints',       'read',      'ALL', false),
('d0000000-0000-0000-0000-00000000001e', 'complaints:write',         'Manage complaints',        'Complaints write',                     'complaints',       'write',     'ALL', false),
('d0000000-0000-0000-0000-00000000001f', 'reviews:read',             'View reviews',             'Reviews read',                         'reviews',          'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000020', 'reviews:write',            'Moderate reviews',         'Reviews moderation',                   'reviews',          'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000021', 'customers:read',           'Search customers',         'Admin customer lookup',                'customers',        'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000022', 'customers:write',          'Manage customers',         'Admin customer updates',               'customers',        'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000023', 'internal_tickets:read',    'View internal tickets',    'IT/internal tickets read',             'internal_tickets', 'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000024', 'internal_tickets:write',   'Manage internal tickets',  'IT/internal tickets write',            'internal_tickets', 'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000025', 'portal:read',              'Portal read',              'Provider portal read',                 'portal',           'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000026', 'portal:write',             'Portal write',             'Provider portal write',                'portal',           'write',     'ALL', false),
('d0000000-0000-0000-0000-000000000027', 'api_docs:read',            'View API docs',            'OpenAPI / API reference',              'api_docs',         'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000028', 'leads:read',               'View leads',               'Contact and lead submissions',         'leads',            'read',      'ALL', false),
('d0000000-0000-0000-0000-000000000029', 'leads:write',              'Manage leads',             'Update lead status',                   'leads',            'write',     'ALL', false),
('d0000000-0000-0000-0000-00000000002a', 'deleted_items:read',       'View deleted items',       'Admin deleted items search',           'deleted_items',    'read',      'ALL', false),
('d0000000-0000-0000-0000-00000000002b', 'deleted_items:restore',    'Restore deleted items',    'Restore soft-deleted entities',        'deleted_items',    'restore',   'ALL', false),
('d0000000-0000-0000-0000-0000000000ee', 'system:super_ops',         'Super operations',         'Break-glass / highly sensitive ops',   'system',           'super_ops', 'ALL', true),
('d0000000-0000-0000-0000-0000000000ef', 'system:bulk_export',       'Bulk data export',         'Full database or bulk export',         'system',           'bulk_export','ALL', true)
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- [4] Role → Permission assignments for SUPER_ADMIN (gets all permissions)
-- ============================================================================
INSERT INTO sys_role_permissions (role_id, permission_id)
SELECT
    'c0000000-0000-0000-0000-000000000001'::uuid,
    id
FROM sys_permissions
ON CONFLICT DO NOTHING;

-- ============================================================================
-- [5] Departments
--     Used by sys_employees. Pre-populated so HR can onboard staff immediately
--     without a setup step. Company can rename or add via the admin UI later.
-- ============================================================================
-- Note: sys_departments has no 'code' column (not in V0 schema, not in JPA entity).
INSERT INTO sys_departments (id, name, description) VALUES
('a0000000-0000-0000-0000-000000000001', 'Sales',    'Sales department'),
('a0000000-0000-0000-0000-000000000002', 'Finance',  'Finance department'),
('a0000000-0000-0000-0000-000000000003', 'Support',  'Customer support'),
('a0000000-0000-0000-0000-000000000004', 'HR',       'Human resources')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- [6] Subscription plans
--     V15 creates the table; these rows define the commercial tiers the
--     application references via SubscriptionService.FREE_PLAN_DEFAULT_SEAT_LIMIT.
--     Already seeded by V15 — kept here with ON CONFLICT for idempotency.
-- ============================================================================
INSERT INTO sys_plans (code, name, max_users, monthly_price, currency, allows_overage, overage_price_per_user) VALUES
('FREE',         'Free',         6,  0.00,   'USD', false, null),
('STARTER',      'Starter',      15, 29.99,  'USD', false, null),
('PROFESSIONAL', 'Professional', 50, 99.99,  'USD', true,  5.00),
('ENTERPRISE',   'Enterprise',   -1, 299.99, 'USD', false, null)
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- [7] Seed exchange rates
--     Operational bootstrap — rates can be updated via the currency admin UI.
--     V16 added ON CONFLICT UNIQUE uk_pay_exchange_rates_pair_date.
-- ============================================================================
INSERT INTO pay_exchange_rates (from_currency, to_currency, rate, effective_date) VALUES
('USD', 'EUR', 0.92, CURRENT_DATE),
('USD', 'SAR', 3.75, CURRENT_DATE),
('EUR', 'USD', 1.09, CURRENT_DATE),
('SAR', 'USD', 0.27, CURRENT_DATE)
ON CONFLICT (from_currency, to_currency, effective_date) DO NOTHING;
