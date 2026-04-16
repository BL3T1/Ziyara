-- ============================================================================
-- RBAC: expand sys_permissions catalogue (aligns with company API domains).
-- Existing rows d001–d003 unchanged (bookings:*, users:read). Idempotent via ON CONFLICT (code).
-- Locked permissions (is_locked=true): assignable on system roles only (backend rule).
-- See comments per resource for controller alignment (Spring still uses UserRole @PreAuthorize).
-- ============================================================================

INSERT INTO sys_permissions (id, code, name, description, resource, action, scope, is_locked) VALUES
-- Users & HR (UserController HR/SUPER_ADMIN)
('d0000000-0000-0000-0000-000000000004', 'users:write', 'Manage users', 'Create and update users', 'users', 'write', 'ALL', false),
-- Providers (management)
('d0000000-0000-0000-0000-000000000005', 'providers:read', 'View providers', 'View service providers', 'providers', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000006', 'providers:write', 'Manage providers', 'Create and update providers', 'providers', 'write', 'ALL', false),
-- Payments
('d0000000-0000-0000-0000-000000000007', 'payments:read', 'View payments', 'View payments', 'payments', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000008', 'payments:write', 'Manage payments', 'Manage payment operations', 'payments', 'write', 'ALL', false),
-- Discounts
('d0000000-0000-0000-0000-000000000009', 'discounts:read', 'View discounts', 'View discount codes', 'discounts', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000000a', 'discounts:write', 'Manage discounts', 'Create and edit discounts', 'discounts', 'write', 'ALL', false),
('d0000000-0000-0000-0000-00000000000b', 'discounts:approve', 'Approve discounts', 'Activate pending discounts', 'discounts', 'approve', 'ALL', false),
-- Reports & analytics
('d0000000-0000-0000-0000-00000000000c', 'reports:read', 'View reports', 'Access management reports', 'reports', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000000d', 'analytics:read', 'View analytics', 'Dashboard analytics', 'analytics', 'read', 'ALL', false),
-- RBAC / roles admin (RoleManagementController is SUPER_ADMIN-only at HTTP layer)
('d0000000-0000-0000-0000-00000000000e', 'roles:read', 'View roles', 'List roles and permission catalogue', 'roles', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000000f', 'roles:write', 'Manage roles', 'Create and update roles and permissions', 'roles', 'write', 'ALL', false),
-- Settings & audit
('d0000000-0000-0000-0000-000000000010', 'settings:read', 'View settings', 'Read system settings', 'settings', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000011', 'settings:write', 'Manage settings', 'Update system settings', 'settings', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000012', 'audit:read', 'View audit logs', 'Read audit trail', 'audit', 'read', 'ALL', false),
-- CMS / content
('d0000000-0000-0000-0000-000000000013', 'content:read', 'View content pages', 'Read landing/CMS content', 'content', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000014', 'content:write', 'Edit content pages', 'Edit landing/CMS content', 'content', 'write', 'ALL', false),
-- Bookable services (company staff)
('d0000000-0000-0000-0000-000000000015', 'services:read', 'View services', 'View catalog services', 'services', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000016', 'services:write', 'Manage services', 'Create and update services', 'services', 'write', 'ALL', false),
-- Taxi & currency
('d0000000-0000-0000-0000-000000000017', 'taxi:read', 'View taxi bookings', 'Taxi operations read', 'taxi', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000018', 'taxi:write', 'Manage taxi bookings', 'Taxi operations write', 'taxi', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000019', 'currency:read', 'View exchange rates', 'Read FX rates', 'currency', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000001a', 'currency:write', 'Manage exchange rates', 'Update FX rates', 'currency', 'write', 'ALL', false),
-- Support: tickets, complaints, reviews
('d0000000-0000-0000-0000-00000000001b', 'tickets:read', 'View tickets', 'Support tickets read', 'tickets', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000001c', 'tickets:write', 'Manage tickets', 'Support tickets write', 'tickets', 'write', 'ALL', false),
('d0000000-0000-0000-0000-00000000001d', 'complaints:read', 'View complaints', 'Complaints read', 'complaints', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000001e', 'complaints:write', 'Manage complaints', 'Complaints write', 'complaints', 'write', 'ALL', false),
('d0000000-0000-0000-0000-00000000001f', 'reviews:read', 'View reviews', 'Reviews read', 'reviews', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000020', 'reviews:write', 'Moderate reviews', 'Reviews moderation', 'reviews', 'write', 'ALL', false),
-- Admin customer search / B2C
('d0000000-0000-0000-0000-000000000021', 'customers:read', 'Search customers', 'Admin customer lookup', 'customers', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000022', 'customers:write', 'Manage customers', 'Admin customer updates', 'customers', 'write', 'ALL', false),
-- Internal IT tickets
('d0000000-0000-0000-0000-000000000023', 'internal_tickets:read', 'View internal tickets', 'IT/internal tickets read', 'internal_tickets', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000024', 'internal_tickets:write', 'Manage internal tickets', 'IT/internal tickets write', 'internal_tickets', 'write', 'ALL', false),
-- Provider portal
('d0000000-0000-0000-0000-000000000025', 'portal:read', 'Portal read', 'Provider portal read', 'portal', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000026', 'portal:write', 'Portal write', 'Provider portal write', 'portal', 'write', 'ALL', false),
-- API docs / developer
('d0000000-0000-0000-0000-000000000027', 'api_docs:read', 'View API docs', 'OpenAPI / API reference', 'api_docs', 'read', 'ALL', false),
-- Leads / contact (system settings)
('d0000000-0000-0000-0000-000000000028', 'leads:read', 'View leads', 'Contact and lead submissions', 'leads', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000029', 'leads:write', 'Manage leads', 'Update lead status', 'leads', 'write', 'ALL', false),
-- Soft-delete admin
('d0000000-0000-0000-0000-00000000002a', 'deleted_items:read', 'View deleted items', 'Admin deleted items search', 'deleted_items', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000002b', 'deleted_items:restore', 'Restore deleted items', 'Restore soft-deleted entities', 'deleted_items', 'restore', 'ALL', false),
-- Locked: system-only (custom roles cannot assign; Super Admin on system roles can)
('d0000000-0000-0000-0000-0000000000ee', 'system:super_ops', 'Super operations', 'Break-glass / highly sensitive ops', 'system', 'super_ops', 'ALL', true),
('d0000000-0000-0000-0000-0000000000ef', 'system:bulk_export', 'Bulk data export', 'Full database or bulk export', 'system', 'bulk_export', 'ALL', true)
ON CONFLICT (code) DO NOTHING;
