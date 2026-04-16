-- ============================================================================
-- ZIYARAH PLATFORM - DUMMY DATA SEED
-- Run after schema.sql and migrations
-- Test credentials: admin@ziyarah.com / password (same for customer@, sales@)
-- ============================================================================

-- Exchange Rates (pay_ after 015)
INSERT INTO pay_exchange_rates (from_currency, to_currency, rate, effective_date) VALUES
('USD', 'EUR', 0.92, CURRENT_DATE),
('USD', 'SAR', 3.75, CURRENT_DATE),
('EUR', 'USD', 1.09, CURRENT_DATE),
('SAR', 'USD', 0.27, CURRENT_DATE)
ON CONFLICT (from_currency, to_currency, effective_date) DO NOTHING;

-- Departments (sys_ after 015)
INSERT INTO sys_departments (id, name, code, description) VALUES
('a0000000-0000-0000-0000-000000000001', 'Sales', 'SALES', 'Sales department'),
('a0000000-0000-0000-0000-000000000002', 'Finance', 'FIN', 'Finance department'),
('a0000000-0000-0000-0000-000000000003', 'Support', 'SUP', 'Customer support'),
('a0000000-0000-0000-0000-000000000004', 'HR', 'HR', 'Human resources')
ON CONFLICT DO NOTHING;

-- Groups (sys_ after 015)
INSERT INTO sys_groups (id, name, code, description) VALUES
('b0000000-0000-0000-0000-000000000001', 'Executive', 'G1', 'Executive group'),
('b0000000-0000-0000-0000-000000000002', 'Sales', 'G2', 'Sales group'),
('b0000000-0000-0000-0000-000000000003', 'Finance', 'G3', 'Finance group'),
('b0000000-0000-0000-0000-000000000004', 'Support', 'G4', 'Customer support and service operations'),
('b0000000-0000-0000-0000-000000000005', 'HR & People', 'G5', 'Human resources and internal people operations'),
('b0000000-0000-0000-0000-000000000006', 'Provider Partner', 'G6', 'Partner and provider-facing accounts'),
('b0000000-0000-0000-0000-000000000007', 'B2C Customers', 'G7', 'End-customer accounts (bookings and profile)')
ON CONFLICT DO NOTHING;

-- Roles (sys_ after 015) — one row per UserRole enum, each tied to an organizational group
INSERT INTO sys_roles (id, name, code, description, level, group_id) VALUES
('c0000000-0000-0000-0000-000000000001', 'Super Admin', 'SUPER_ADMIN', 'Full system access', 'SUPER_ADMIN', 'b0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000002', 'Sales Manager', 'SALES_MANAGER', 'Sales team lead', 'MANAGER', 'b0000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000003', 'Customer', 'CUSTOMER', 'End customer', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000007'),
('c0000000-0000-0000-0000-000000000010', 'CEO', 'CEO', 'Chief Executive Officer', 'EXECUTIVE', 'b0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000011', 'General Manager', 'GENERAL_MANAGER', 'General management', 'EXECUTIVE', 'b0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000012', 'Sales Representative', 'SALES_REPRESENTATIVE', 'Sales representative', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000013', 'Finance Manager', 'FINANCE_MANAGER', 'Finance team lead', 'MANAGER', 'b0000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000014', 'Accountant', 'ACCOUNTANT', 'Accounting', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000003'),
('c0000000-0000-0000-0000-000000000015', 'Support Manager', 'SUPPORT_MANAGER', 'Support team lead', 'MANAGER', 'b0000000-0000-0000-0000-000000000004'),
('c0000000-0000-0000-0000-000000000016', 'Support Agent', 'SUPPORT_AGENT', 'Customer support agent', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000004'),
('c0000000-0000-0000-0000-000000000017', 'HR Manager', 'HR_MANAGER', 'Human resources manager', 'MANAGER', 'b0000000-0000-0000-0000-000000000005'),
('c0000000-0000-0000-0000-000000000018', 'Provider Manager', 'PROVIDER_MANAGER', 'Partner account manager', 'MANAGER', 'b0000000-0000-0000-0000-000000000006'),
('c0000000-0000-0000-0000-000000000019', 'Provider Finance', 'PROVIDER_FINANCE', 'Partner finance', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000006'),
('c0000000-0000-0000-0000-00000000001a', 'Provider Staff', 'PROVIDER_STAFF', 'Partner staff', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000006'),
('c0000000-0000-0000-0000-00000000001b', 'Taxi Operator', 'TAXI_OPERATOR', 'Taxi operations', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000006')
ON CONFLICT DO NOTHING;

-- Permissions (sys_ after 015) — full catalogue; is_locked=true only assignable on system roles (backend)
INSERT INTO sys_permissions (id, code, name, description, resource, action, scope, is_locked) VALUES
('d0000000-0000-0000-0000-000000000001', 'bookings:read', 'View bookings', 'View all bookings', 'bookings', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000002', 'bookings:write', 'Manage bookings', 'Create and update bookings', 'bookings', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000003', 'users:read', 'View users', 'View user list', 'users', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000004', 'users:write', 'Manage users', 'Create and update users', 'users', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000005', 'providers:read', 'View providers', 'View service providers', 'providers', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000006', 'providers:write', 'Manage providers', 'Create and update providers', 'providers', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000007', 'payments:read', 'View payments', 'View payments', 'payments', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000008', 'payments:write', 'Manage payments', 'Manage payment operations', 'payments', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000009', 'discounts:read', 'View discounts', 'View discount codes', 'discounts', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000000a', 'discounts:write', 'Manage discounts', 'Create and edit discounts', 'discounts', 'write', 'ALL', false),
('d0000000-0000-0000-0000-00000000000b', 'discounts:approve', 'Approve discounts', 'Activate pending discounts', 'discounts', 'approve', 'ALL', false),
('d0000000-0000-0000-0000-00000000000c', 'reports:read', 'View reports', 'Access management reports', 'reports', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000000d', 'analytics:read', 'View analytics', 'Dashboard analytics', 'analytics', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000000e', 'roles:read', 'View roles', 'List roles and permission catalogue', 'roles', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000000f', 'roles:write', 'Manage roles', 'Create and update roles and permissions', 'roles', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000010', 'settings:read', 'View settings', 'Read system settings', 'settings', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000011', 'settings:write', 'Manage settings', 'Update system settings', 'settings', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000012', 'audit:read', 'View audit logs', 'Read audit trail', 'audit', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000013', 'content:read', 'View content pages', 'Read landing/CMS content', 'content', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000014', 'content:write', 'Edit content pages', 'Edit landing/CMS content', 'content', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000015', 'services:read', 'View services', 'View catalog services', 'services', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000016', 'services:write', 'Manage services', 'Create and update services', 'services', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000017', 'taxi:read', 'View taxi bookings', 'Taxi operations read', 'taxi', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000018', 'taxi:write', 'Manage taxi bookings', 'Taxi operations write', 'taxi', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000019', 'currency:read', 'View exchange rates', 'Read FX rates', 'currency', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000001a', 'currency:write', 'Manage exchange rates', 'Update FX rates', 'currency', 'write', 'ALL', false),
('d0000000-0000-0000-0000-00000000001b', 'tickets:read', 'View tickets', 'Support tickets read', 'tickets', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000001c', 'tickets:write', 'Manage tickets', 'Support tickets write', 'tickets', 'write', 'ALL', false),
('d0000000-0000-0000-0000-00000000001d', 'complaints:read', 'View complaints', 'Complaints read', 'complaints', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000001e', 'complaints:write', 'Manage complaints', 'Complaints write', 'complaints', 'write', 'ALL', false),
('d0000000-0000-0000-0000-00000000001f', 'reviews:read', 'View reviews', 'Reviews read', 'reviews', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000020', 'reviews:write', 'Moderate reviews', 'Reviews moderation', 'reviews', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000021', 'customers:read', 'Search customers', 'Admin customer lookup', 'customers', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000022', 'customers:write', 'Manage customers', 'Admin customer updates', 'customers', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000023', 'internal_tickets:read', 'View internal tickets', 'IT/internal tickets read', 'internal_tickets', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000024', 'internal_tickets:write', 'Manage internal tickets', 'IT/internal tickets write', 'internal_tickets', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000025', 'portal:read', 'Portal read', 'Provider portal read', 'portal', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000026', 'portal:write', 'Portal write', 'Provider portal write', 'portal', 'write', 'ALL', false),
('d0000000-0000-0000-0000-000000000027', 'api_docs:read', 'View API docs', 'OpenAPI / API reference', 'api_docs', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000028', 'leads:read', 'View leads', 'Contact and lead submissions', 'leads', 'read', 'ALL', false),
('d0000000-0000-0000-0000-000000000029', 'leads:write', 'Manage leads', 'Update lead status', 'leads', 'write', 'ALL', false),
('d0000000-0000-0000-0000-00000000002a', 'deleted_items:read', 'View deleted items', 'Admin deleted items search', 'deleted_items', 'read', 'ALL', false),
('d0000000-0000-0000-0000-00000000002b', 'deleted_items:restore', 'Restore deleted items', 'Restore soft-deleted entities', 'deleted_items', 'restore', 'ALL', false),
('d0000000-0000-0000-0000-0000000000ee', 'system:super_ops', 'Super operations', 'Break-glass / highly sensitive ops', 'system', 'super_ops', 'ALL', true),
('d0000000-0000-0000-0000-0000000000ef', 'system:bulk_export', 'Bulk data export', 'Full database or bulk export', 'system', 'bulk_export', 'ALL', true)
ON CONFLICT (code) DO NOTHING;

-- Role-Permissions (sys_ after 015)
INSERT INTO sys_role_permissions (role_id, permission_id) VALUES
('c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000001'),
('c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000002'),
('c0000000-0000-0000-0000-000000000001', 'd0000000-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;

-- Users (sys_ after 015)
-- Password for all: Demo123! (backend SuperAdminSeeder resets super_admin on startup)
INSERT INTO sys_users (id, email, phone, password_hash, role, status, email_verified) VALUES
('e0000000-0000-0000-0000-000000000001', 'admin@ziyarah.com', '+1234567890', '$2a$10$uwlPRc1V5/43DDPrY2LPaevi7OMAloKlVFi2iOqgqUSGA.ptFnxb6', 'SUPER_ADMIN', 'ACTIVE', true),
('e0000000-0000-0000-0000-000000000002', 'customer@ziyarah.com', '+1234567891', '$2a$10$uwlPRc1V5/43DDPrY2LPaevi7OMAloKlVFi2iOqgqUSGA.ptFnxb6', 'CUSTOMER', 'ACTIVE', true),
('e0000000-0000-0000-0000-000000000003', 'sales@ziyarah.com', '+1234567892', '$2a$10$uwlPRc1V5/43DDPrY2LPaevi7OMAloKlVFi2iOqgqUSGA.ptFnxb6', 'SALES_MANAGER', 'ACTIVE', true),
('e0000000-0000-0000-0000-000000000004', 'super_admin@ziyarah.com', '+1234567893', '$2a$10$uwlPRc1V5/43DDPrY2LPaevi7OMAloKlVFi2iOqgqUSGA.ptFnxb6', 'SUPER_ADMIN', 'ACTIVE', true)
ON CONFLICT DO NOTHING;

-- Ensure test users have correct password (fixes re-seed or existing DB)
UPDATE sys_users SET password_hash = '$2a$10$uwlPRc1V5/43DDPrY2LPaevi7OMAloKlVFi2iOqgqUSGA.ptFnxb6'
WHERE email IN ('admin@ziyarah.com', 'customer@ziyarah.com', 'sales@ziyarah.com', 'super_admin@ziyarah.com');

-- User-Roles (sys_ after 015)
INSERT INTO sys_user_roles (user_id, role_id) VALUES
('e0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001'),
('e0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000003'),
('e0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002'),
('e0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- Customers
INSERT INTO customers (user_id, first_name, last_name, preferred_currency) VALUES
('e0000000-0000-0000-0000-000000000002', 'John', 'Doe', 'USD')
ON CONFLICT DO NOTHING;

-- Employees (sys_ after 015)
INSERT INTO sys_employees (user_id, department_id, employee_code, level, hire_date, job_title) VALUES
('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'EMP001', 'SUPER_ADMIN', '2024-01-01', 'System Administrator'),
('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'EMP002', 'MANAGER', '2024-02-01', 'Sales Manager'),
('e0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'EMP000', 'SUPER_ADMIN', '2024-01-01', 'Super Administrator')
ON CONFLICT DO NOTHING;

-- Service Providers (hotel_ after 015)
INSERT INTO hotel_service_providers (id, company_name, contact_email, contact_phone, address, city, country, status) VALUES
('f0000000-0000-0000-0000-000000000001', 'Sunrise Hotels', 'contact@sunrise.com', '+1111111111', '123 Beach Rd', 'Dubai', 'UAE', 'ACTIVE'),
('f0000000-0000-0000-0000-000000000002', 'City Taxi Co', 'info@citytaxi.com', '+2222222222', '456 Main St', 'Riyadh', 'Saudi Arabia', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- Services (hotel_ after 015)
INSERT INTO hotel_services (id, provider_id, type, name, description, base_price, currency, status, city, country, max_guests) VALUES
('f1000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'RESORT', 'Sunrise Beach Resort', 'Luxury beachfront resort', 250.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 4),
('f1000000-0000-0000-0000-000000000026', 'f0000000-0000-0000-0000-000000000001', 'HOTEL', 'Sunrise City Hotel', 'Downtown business and leisure hotel', 180.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 3),
('f1000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000002', 'TAXI', 'Airport Transfer', 'Standard airport pickup', 45.00, 'USD', 'ACTIVE', 'Riyadh', 'Saudi Arabia', 4),
('f1000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Marina Terrace Restaurant', 'Seafood and international cuisine', 75.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 8),
('f1000000-0000-0000-0000-000000000011', 'f0000000-0000-0000-0000-000000000001', 'TRIP', 'Heritage City Tour', 'Half-day guided cultural tour', 120.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 12),
('f1000000-0000-0000-0000-000000000012', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Olea Garden', 'Mediterranean mezze and wood-fired dishes', 65.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 10),
('f1000000-0000-0000-0000-000000000013', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Saffron Table', 'Contemporary Indian fusion', 55.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 8),
('f1000000-0000-0000-0000-000000000014', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Downtown Grill', 'Premium steaks and grill classics', 95.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 6),
('f1000000-0000-0000-0000-000000000015', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Harbor Sushi', 'Omakase and seasonal Japanese', 110.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 6),
('f1000000-0000-0000-0000-000000000016', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Oasis Café', 'Levantine breakfast and light bites', 35.00, 'USD', 'ACTIVE', 'Riyadh', 'Saudi Arabia', 12),
('f1000000-0000-0000-0000-000000000021', 'f0000000-0000-0000-0000-000000000001', 'TRIP', 'Desert Dunes Safari', 'Full-day 4x4 dunes, camp dinner, entertainment', 189.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 14),
('f1000000-0000-0000-0000-000000000022', 'f0000000-0000-0000-0000-000000000001', 'TRIP', 'Coral Reef Snorkel', 'Boat trip with guided snorkeling', 95.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 10),
('f1000000-0000-0000-0000-000000000023', 'f0000000-0000-0000-0000-000000000001', 'TRIP', 'Old Souk Walking Tour', 'Guided heritage walk and tastings', 45.00, 'USD', 'ACTIVE', 'Dubai', 'UAE', 15),
('f1000000-0000-0000-0000-000000000024', 'f0000000-0000-0000-0000-000000000002', 'TRIP', 'Mountain Day Hike', 'Scenic trail with picnic lunch', 135.00, 'USD', 'ACTIVE', 'Riyadh', 'Saudi Arabia', 12),
('f1000000-0000-0000-0000-000000000025', 'f0000000-0000-0000-0000-000000000002', 'TRIP', 'Sunset Yacht Cruise', 'Evening coastal cruise with refreshments', 160.00, 'USD', 'ACTIVE', 'Jeddah', 'Saudi Arabia', 20)
ON CONFLICT DO NOTHING;

-- Discount Codes (disc_ after 015)
INSERT INTO disc_discount_codes (id, code, description, percentage, expiry_date, usage_limit, status, created_by) VALUES
('f2000000-0000-0000-0000-000000000001', 'WELCOME10', 'Welcome discount', 10.00, CURRENT_DATE + INTERVAL '30 days', 100, 'ACTIVE', 'e0000000-0000-0000-0000-000000000001')
ON CONFLICT DO NOTHING;

-- Bookings (bkg_ after 015)
INSERT INTO bkg_bookings (id, booking_reference, customer_id, service_id, check_in_date, check_out_date, guests, base_amount, total_amount, currency, status) VALUES
('f3000000-0000-0000-0000-000000000001', 'ZYA-2024-001', 'e0000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000001', CURRENT_DATE + 7, CURRENT_DATE + 9, 2, 500.00, 500.00, 'USD', 'CONFIRMED')
ON CONFLICT DO NOTHING;

-- Payments (pay_ after 015)
INSERT INTO pay_payments (id, booking_id, amount, currency, method, status, transaction_ref, processed_at) VALUES
('f4000000-0000-0000-0000-000000000001', 'f3000000-0000-0000-0000-000000000001', 500.00, 'USD', 'CREDIT_CARD', 'COMPLETED', 'TXN-001', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Complaints (support_ after 015)
INSERT INTO support_complaints (id, ticket_number, customer_id, subject, description, priority, status) VALUES
('f5000000-0000-0000-0000-000000000001', 'CMP-001', 'e0000000-0000-0000-0000-000000000002', 'Late check-in', 'Room was not ready at 2pm', 'MEDIUM', 'SUBMITTED')
ON CONFLICT DO NOTHING;

-- Internal Tickets (support_ after 015)
INSERT INTO support_internal_tickets (id, ticket_number, reporter_id, type, subject, description, priority, status) VALUES
('f6000000-0000-0000-0000-000000000001', 'TKT-001', 'e0000000-0000-0000-0000-000000000001', 'BUG_REPORT', 'Login issue', 'Users cannot login with special chars in password', 'HIGH', 'SUBMITTED')
ON CONFLICT DO NOTHING;

-- Reviews (hotel_ after 015)
INSERT INTO hotel_reviews (id, booking_id, customer_id, service_id, rating, comment, status) VALUES
('f7000000-0000-0000-0000-000000000001', 'f3000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000001', 5, 'Excellent stay!', 'APPROVED')
ON CONFLICT DO NOTHING;

-- Notifications (sys_ after 015)
INSERT INTO sys_notifications (id, user_id, type, channel, title, content, status) VALUES
('f8000000-0000-0000-0000-000000000001', 'e0000000-0000-0000-0000-000000000002', 'BOOKING_CONFIRMATION', 'IN_APP', 'Booking Confirmed', 'Your booking ZYA-2024-001 has been confirmed.', 'SENT')
ON CONFLICT DO NOTHING;
