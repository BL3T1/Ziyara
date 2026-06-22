-- ============================================================================
-- V34 — Remove HR_MANAGER + GENERAL_MANAGER roles, rename tickets permissions,
--        seed mobile test provider + service + customer + booking + payment.
-- ============================================================================

-- ============================================================================
-- [1] Delete users whose enum role is HR_MANAGER or GENERAL_MANAGER.
--     CASCADE on sys_users removes sessions, audit rows, notifications, etc.
-- ============================================================================
DELETE FROM sys_users WHERE role IN ('HR_MANAGER', 'GENERAL_MANAGER');

-- ============================================================================
-- [2] Drop role-permission assignments for these roles, then the roles.
-- ============================================================================
DELETE FROM sys_role_permissions
WHERE role_id IN (SELECT id FROM sys_roles WHERE code IN ('HR_MANAGER', 'GENERAL_MANAGER'));

DELETE FROM sys_user_roles
WHERE role_id IN (SELECT id FROM sys_roles WHERE code IN ('HR_MANAGER', 'GENERAL_MANAGER'));

DELETE FROM sys_roles WHERE code IN ('HR_MANAGER', 'GENERAL_MANAGER');

-- ============================================================================
-- [3] Rename tickets:read / tickets:write → providers_messages:read / write.
--     All existing role-permission rows referencing these IDs are preserved
--     (the permission ID doesn't change, only the code / name / resource).
-- ============================================================================
UPDATE sys_permissions
SET code = 'providers_messages:read',
    name = 'Provider Messages Read',
    resource = 'providers_messages'
WHERE code = 'tickets:read';

UPDATE sys_permissions
SET code = 'providers_messages:write',
    name  = 'Provider Messages Write',
    resource = 'providers_messages'
WHERE code = 'tickets:write';

-- ============================================================================
-- [4] Mobile test data — dummy provider + hotel service + customer + booking
--     + payment.  All rows use fixed UUIDs so this block is idempotent.
--
--     Mobile test credentials (password set by MobileTestDataSeeder on startup):
--       Email  : mobile_test@ziyarah.com
--       Phone  : +966500000099
--       Password: same as APP_DEMO_PASSWORD env var (or startup-log generated)
-- ============================================================================

-- Dummy hotel provider (no owning user required — created_by nullable)
INSERT INTO hotel_service_providers
    (id, company_name, contact_email, contact_phone, address,
     provider_type, status, verified, commission_rate, created_at)
VALUES
    ('f0000000-0000-0000-0000-000000000001',
     'Ziyara Test Hotel Co.',
     'test.hotel@ziyarah.com',
     '+966500000001',
     'King Fahd Rd, Riyadh, Saudi Arabia',
     'HOTEL',
     'ACTIVE',
     true,
     10.00,
     now())
ON CONFLICT (id) DO NOTHING;

-- Dummy hotel service belonging to the provider above
INSERT INTO hotel_services
    (id, provider_id, type, name, description,
     location, city, country, base_price, currency, status, created_at)
VALUES
    ('f0000000-0000-0000-0000-000000000002',
     'f0000000-0000-0000-0000-000000000001',
     'HOTEL',
     'Ziyara Grand Hotel (Test)',
     'A luxury demo hotel used for mobile-app testing.',
     'Downtown Riyadh',
     'Riyadh',
     'Saudi Arabia',
     250.00,
     'USD',
     'ACTIVE',
     now())
ON CONFLICT (id) DO NOTHING;

-- Mobile test customer account.
-- Password hash placeholder — MobileTestDataSeeder resets it to APP_DEMO_PASSWORD on startup.
-- Bcrypt placeholder: $2a$10$replacedBySeeder (intentionally invalid so login only works after seeder runs)
INSERT INTO sys_users
    (id, email, username, password_hash, role,
     status, email_verified, phone_verified, phone, created_at)
VALUES
    ('f0000000-0000-0000-0000-000000000003',
     'mobile_test@ziyarah.com',
     'MobileTestUser',
     '$2a$10$replacedByMobileTestDataSeederOnStartup_placeholder___',
     'CUSTOMER',
     'ACTIVE',
     true,
     true,
     '+966500000099',
     now())
ON CONFLICT (id) DO NOTHING;

-- Hotel booking for the test customer
INSERT INTO bkg_bookings
    (id, booking_reference, customer_id, service_id,
     check_in_date, check_out_date, guests, rooms,
     base_amount, discount_amount, tax_amount, total_amount,
     currency, status, confirmed_at, created_at)
VALUES
    ('f0000000-0000-0000-0000-000000000004',
     'ZYR-MOBILE-001',
     'f0000000-0000-0000-0000-000000000003',
     'f0000000-0000-0000-0000-000000000002',
     CURRENT_DATE + INTERVAL '30 days',
     CURRENT_DATE + INTERVAL '33 days',
     2,
     1,
     750.00,
     0.00,
     75.00,
     825.00,
     'USD',
     'CONFIRMED',
     now(),
     now())
ON CONFLICT (id) DO NOTHING;

-- Successful payment for the booking above
INSERT INTO pay_payments
    (id, booking_id, amount, currency, method, status,
     transaction_ref, idempotency_key, processed_at, created_at)
VALUES
    ('f0000000-0000-0000-0000-000000000005',
     'f0000000-0000-0000-0000-000000000004',
     825.00,
     'USD',
     'CARD',
     'COMPLETED',
     'TXN-MOBILE-TEST-001',
     'idmp-mobile-test-001',
     now(),
     now())
ON CONFLICT (id) DO NOTHING;
