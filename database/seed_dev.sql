-- ============================================================================
-- ZIYARA — DEVELOPMENT / DEMO SEED DATA
-- ============================================================================
-- PURPOSE : Load demo services, bookings, and other sample content so
--           developers get a realistic dataset immediately.
--
-- WHEN TO RUN:
--   After the application has started at least once (Flyway must have created
--   the schema) and DemoDataSeeder has seeded the demo users.
--
--   psql -h localhost -U ziyarah_user -d ziyarah -f database/seed_dev.sql
--
-- DO NOT USE IN PRODUCTION.
-- Users (admin, customer, sales …) are created at startup by DemoDataSeeder
-- (@Profile("!prod")) and SuperAdminSeeder. This script does NOT insert users.
-- ============================================================================

\echo '==> [1/8] Service providers'
INSERT INTO hotel_service_providers (id, company_name, contact_email, contact_phone, address, city, country, status) VALUES
('f0000000-0000-0000-0000-000000000001', 'Sunrise Hotels',  'contact@sunrise.com', '+1111111111', '123 Beach Rd', 'Dubai',  'UAE',          'ACTIVE'),
('f0000000-0000-0000-0000-000000000002', 'City Taxi Co',    'info@citytaxi.com',   '+2222222222', '456 Main St',  'Riyadh', 'Saudi Arabia', 'ACTIVE')
ON CONFLICT DO NOTHING;

\echo '==> [2/8] Services'
INSERT INTO hotel_services (id, provider_id, type, name, description, base_price, currency, status, city, country, max_guests) VALUES
('f1000000-0000-0000-0000-000000000001', 'f0000000-0000-0000-0000-000000000001', 'RESORT',     'Sunrise Beach Resort',      'Luxury beachfront resort',                              250.00, 'USD', 'ACTIVE', 'Dubai',  'UAE',          4),
('f1000000-0000-0000-0000-000000000026', 'f0000000-0000-0000-0000-000000000001', 'HOTEL',      'Sunrise City Hotel',        'Downtown business and leisure hotel',                   180.00, 'USD', 'ACTIVE', 'Dubai',  'UAE',          3),
('f1000000-0000-0000-0000-000000000002', 'f0000000-0000-0000-0000-000000000002', 'TAXI',       'Airport Transfer',          'Standard airport pickup',                               45.00,  'USD', 'ACTIVE', 'Riyadh', 'Saudi Arabia', 4),
('f1000000-0000-0000-0000-000000000010', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Marina Terrace Restaurant', 'Seafood and international cuisine',                     75.00,  'USD', 'ACTIVE', 'Dubai',  'UAE',          8),
('f1000000-0000-0000-0000-000000000011', 'f0000000-0000-0000-0000-000000000001', 'TRIP',       'Heritage City Tour',        'Half-day guided cultural tour',                         120.00, 'USD', 'ACTIVE', 'Dubai',  'UAE',          12),
('f1000000-0000-0000-0000-000000000012', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Olea Garden',               'Mediterranean mezze and wood-fired dishes',             65.00,  'USD', 'ACTIVE', 'Dubai',  'UAE',          10),
('f1000000-0000-0000-0000-000000000013', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Saffron Table',             'Contemporary Indian fusion',                            55.00,  'USD', 'ACTIVE', 'Dubai',  'UAE',          8),
('f1000000-0000-0000-0000-000000000014', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Downtown Grill',            'Premium steaks and grill classics',                     95.00,  'USD', 'ACTIVE', 'Dubai',  'UAE',          6),
('f1000000-0000-0000-0000-000000000015', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Harbor Sushi',              'Omakase and seasonal Japanese',                         110.00, 'USD', 'ACTIVE', 'Dubai',  'UAE',          6),
('f1000000-0000-0000-0000-000000000016', 'f0000000-0000-0000-0000-000000000001', 'RESTAURANT', 'Oasis Café',                'Levantine breakfast and light bites',                   35.00,  'USD', 'ACTIVE', 'Riyadh', 'Saudi Arabia', 12),
('f1000000-0000-0000-0000-000000000021', 'f0000000-0000-0000-0000-000000000001', 'TRIP',       'Desert Dunes Safari',       'Full-day 4x4 dunes, camp dinner, entertainment',        189.00, 'USD', 'ACTIVE', 'Dubai',  'UAE',          14),
('f1000000-0000-0000-0000-000000000022', 'f0000000-0000-0000-0000-000000000001', 'TRIP',       'Coral Reef Snorkel',        'Boat trip with guided snorkeling',                      95.00,  'USD', 'ACTIVE', 'Dubai',  'UAE',          10),
('f1000000-0000-0000-0000-000000000023', 'f0000000-0000-0000-0000-000000000001', 'TRIP',       'Old Souk Walking Tour',     'Guided heritage walk and tastings',                     45.00,  'USD', 'ACTIVE', 'Dubai',  'UAE',          15),
('f1000000-0000-0000-0000-000000000024', 'f0000000-0000-0000-0000-000000000002', 'TRIP',       'Mountain Day Hike',         'Scenic trail with picnic lunch',                        135.00, 'USD', 'ACTIVE', 'Riyadh', 'Saudi Arabia', 12),
('f1000000-0000-0000-0000-000000000025', 'f0000000-0000-0000-0000-000000000002', 'TRIP',       'Sunset Yacht Cruise',       'Evening coastal cruise with refreshments',               160.00, 'USD', 'ACTIVE', 'Jeddah', 'Saudi Arabia', 20)
ON CONFLICT DO NOTHING;

\echo '==> [3/8] Discount codes'
INSERT INTO disc_discount_codes
    (id, code, description, percentage, expiry_date, usage_limit, status, approval_status, created_by)
SELECT
    'f2000000-0000-0000-0000-000000000001',
    'WELCOME10',
    'Welcome discount',
    10.00,
    CURRENT_DATE + INTERVAL '90 days',
    100,
    'ACTIVE',
    'APPROVED',
    (SELECT id FROM sys_users WHERE role = 'SUPER_ADMIN' ORDER BY created_at LIMIT 1)
WHERE EXISTS (SELECT 1 FROM sys_users WHERE role = 'SUPER_ADMIN')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- Remaining demo data: bookings, payments, complaints, reviews, notifications.
-- Each block resolves user IDs dynamically by role/email so it works regardless
-- of what UUID DemoDataSeeder assigned.
-- ============================================================================
DO $$
DECLARE
    v_customer_id   UUID;
    v_admin_id      UUID;
    v_sales_id      UUID;
    v_booking_id    UUID := 'f3000000-0000-0000-0000-000000000001';
    v_payment_id    UUID := 'f4000000-0000-0000-0000-000000000001';
    v_complaint_id  UUID := 'f5000000-0000-0000-0000-000000000001';
    v_ticket_id     UUID := 'f6000000-0000-0000-0000-000000000001';
    v_review_id     UUID := 'f7000000-0000-0000-0000-000000000001';
    v_notif_id      UUID := 'f8000000-0000-0000-0000-000000000001';
BEGIN
    -- Resolve demo user IDs inserted by DemoDataSeeder / SuperAdminSeeder
    SELECT id INTO v_customer_id FROM sys_users WHERE role = 'CUSTOMER'    ORDER BY created_at LIMIT 1;
    SELECT id INTO v_admin_id    FROM sys_users WHERE role = 'SUPER_ADMIN' ORDER BY created_at LIMIT 1;
    SELECT id INTO v_sales_id    FROM sys_users WHERE role = 'SALES_MANAGER' ORDER BY created_at LIMIT 1;

    IF v_customer_id IS NULL THEN
        RAISE NOTICE 'No CUSTOMER user found — skipping demo bookings/reviews/notifications. '
                     'Start the application first so DemoDataSeeder can create users, then re-run.';
        RETURN;
    END IF;

    -- ------------------------------------------------------------------ --
    -- [4] Customers table (customer profile record)
    -- ------------------------------------------------------------------ --
    INSERT INTO customers (user_id, first_name, last_name, preferred_currency)
    VALUES (v_customer_id, 'John', 'Doe', 'USD')
    ON CONFLICT DO NOTHING;

    -- ------------------------------------------------------------------ --
    -- [5] Employees (sys_employees)
    -- ------------------------------------------------------------------ --
    IF v_admin_id IS NOT NULL THEN
        INSERT INTO sys_employees (user_id, department_id, employee_code, level, hire_date, job_title)
        VALUES (v_admin_id, 'a0000000-0000-0000-0000-000000000001', 'EMP001', 'SUPER_ADMIN', '2024-01-01', 'System Administrator')
        ON CONFLICT DO NOTHING;
    END IF;

    IF v_sales_id IS NOT NULL THEN
        INSERT INTO sys_employees (user_id, department_id, employee_code, level, hire_date, job_title)
        VALUES (v_sales_id, 'a0000000-0000-0000-0000-000000000001', 'EMP002', 'MANAGER', '2024-02-01', 'Sales Manager')
        ON CONFLICT DO NOTHING;
    END IF;

    -- ------------------------------------------------------------------ --
    -- [6] Booking
    -- ------------------------------------------------------------------ --
    INSERT INTO bkg_bookings
        (id, booking_reference, customer_id, service_id,
         check_in_date, check_out_date, guests,
         base_amount, total_amount, currency, status)
    VALUES
        (v_booking_id, 'ZYA-2024-001', v_customer_id,
         'f1000000-0000-0000-0000-000000000001',
         CURRENT_DATE + 7, CURRENT_DATE + 9, 2,
         500.00, 500.00, 'USD', 'CONFIRMED')
    ON CONFLICT DO NOTHING;

    -- ------------------------------------------------------------------ --
    -- [7] Payment
    -- ------------------------------------------------------------------ --
    IF EXISTS (SELECT 1 FROM bkg_bookings WHERE id = v_booking_id) THEN
        INSERT INTO pay_payments
            (id, booking_id, amount, currency, method, status, transaction_ref, processed_at)
        VALUES
            (v_payment_id, v_booking_id, 500.00, 'USD', 'CREDIT_CARD', 'COMPLETED', 'TXN-001', CURRENT_TIMESTAMP)
        ON CONFLICT DO NOTHING;
    END IF;

    -- ------------------------------------------------------------------ --
    -- [8a] Complaint
    -- ------------------------------------------------------------------ --
    INSERT INTO support_complaints
        (id, ticket_number, customer_id, subject, description, priority, status)
    VALUES
        (v_complaint_id, 'CMP-001', v_customer_id,
         'Late check-in', 'Room was not ready at 2pm', 'MEDIUM', 'SUBMITTED')
    ON CONFLICT DO NOTHING;

    -- ------------------------------------------------------------------ --
    -- [8b] Internal ticket
    -- ------------------------------------------------------------------ --
    IF v_admin_id IS NOT NULL THEN
        INSERT INTO support_internal_tickets
            (id, ticket_number, reporter_id, type, subject, description, priority, status)
        VALUES
            (v_ticket_id, 'TKT-001', v_admin_id,
             'BUG_REPORT', 'Login issue', 'Users cannot login with special chars in password', 'HIGH', 'SUBMITTED')
        ON CONFLICT DO NOTHING;
    END IF;

    -- ------------------------------------------------------------------ --
    -- [8c] Review
    -- ------------------------------------------------------------------ --
    IF EXISTS (SELECT 1 FROM bkg_bookings WHERE id = v_booking_id) THEN
        INSERT INTO hotel_reviews
            (id, booking_id, customer_id, service_id, rating, comment, status)
        VALUES
            (v_review_id, v_booking_id, v_customer_id,
             'f1000000-0000-0000-0000-000000000001', 5, 'Excellent stay!', 'APPROVED')
        ON CONFLICT DO NOTHING;
    END IF;

    -- ------------------------------------------------------------------ --
    -- [8d] Notification
    -- ------------------------------------------------------------------ --
    INSERT INTO sys_notifications
        (id, user_id, type, channel, title, message, status)
    VALUES
        (v_notif_id, v_customer_id,
         'BOOKING_CONFIRMATION', 'IN_APP',
         'Booking Confirmed', 'Your booking ZYA-2024-001 has been confirmed.', 'SENT')
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'Dev seed complete. Customer ID used: %', v_customer_id;
END $$;
