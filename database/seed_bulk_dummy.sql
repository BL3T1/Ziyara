-- ============================================================================
-- ZIYARAH – LARGE BULK DUMMY DATA (PostgreSQL)
-- Run AFTER: schema.sql, all migrations (including 015 table prefixes), seed.sql
--
-- Load (from repo root):
--   Bash:  docker compose exec -T postgres psql -U ziyarah_user -d ziyarah -f - < database/seed_bulk_dummy.sql
--   PowerShell:
--     Get-Content -Raw database/seed_bulk_dummy.sql | docker compose exec -T postgres psql -U ziyarah_user -d ziyarah
--
-- Volume (edit generate_series upper bounds to scale):
--   ~200 providers, ~2,000 services, ~10,000 customer users, ~50,000 bookings,
--   payments for confirmed/completed bookings, ~10,000 reviews.
--
-- Identifiers:
--   Providers: contact_email = bulk.provider.{n}@dummy.ziyarah.test
--   Customers: email = bulk.{n}@dummy.ziyarah.test
--   Bookings:  booking_reference = BULK + 10-digit sequence
--   Payments:  transaction_ref = BULK-TXN-{booking_reference}
--
-- Password hash matches seed.sql (Demo123!): bcrypt below.
-- ============================================================================

BEGIN;

-- Same bcrypt as database/seed.sql (password: Demo123!)

-- ---------------------------------------------------------------------------
-- 1) Service providers
-- ---------------------------------------------------------------------------
INSERT INTO hotel_service_providers (
    id, company_name, contact_email, contact_phone, address, city, country, status, commission_rate
)
SELECT
    gen_random_uuid(),
    'Bulk Provider ' || g,
    'bulk.provider.' || g || '@dummy.ziyarah.test',
    '+200000' || LPAD(g::text, 7, '0'),
    'Bulk HQ street ' || g || ', test district',
    (ARRAY['Dubai', 'Riyadh', 'Jeddah', 'Cairo', 'Doha'])[1 + ((g - 1) % 5)],
    (ARRAY['UAE', 'Saudi Arabia', 'Saudi Arabia', 'Egypt', 'Qatar'])[1 + ((g - 1) % 5)],
    'ACTIVE'::provider_status_enum,
    (5 + (g % 15))::numeric(5, 2)
FROM generate_series(1, 200) AS g;

-- ---------------------------------------------------------------------------
-- 2) Map providers to row numbers for service assignment
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_bulk_providers ON COMMIT DROP AS
SELECT
    id,
    ROW_NUMBER() OVER (ORDER BY contact_email) AS k
FROM hotel_service_providers
WHERE contact_email LIKE 'bulk.provider.%@dummy.ziyarah.test';

-- ---------------------------------------------------------------------------
-- 3) Services (spread across bulk providers)
-- ---------------------------------------------------------------------------
INSERT INTO hotel_services (
    id, provider_id, type, name, description, location, city, country,
    base_price, currency, status, star_rating, max_guests, total_rooms, available_rooms
)
SELECT
    gen_random_uuid(),
    p.id,
    (ARRAY[
        'HOTEL'::service_type_enum,
        'RESORT'::service_type_enum,
        'RESTAURANT'::service_type_enum,
        'TAXI'::service_type_enum,
        'TRIP'::service_type_enum
    ])[1 + ((g - 1) % 5)],
    'Bulk Test Service ' || g,
    'Auto-generated listing for load testing. Index ' || g || '.',
    'District ' || ((g % 50) + 1),
    (ARRAY['Dubai', 'Riyadh', 'Jeddah'])[1 + ((g - 1) % 3)],
    'UAE',
    (50 + (g % 450))::numeric(12, 2),
    'USD',
    'ACTIVE'::service_status_enum,
    CASE WHEN ((g - 1) % 5) >= 3 THEN NULL ELSE 1 + (g % 5) END,
    1 + (g % 8),
    CASE WHEN ((g - 1) % 5) >= 3 THEN NULL ELSE 20 + (g % 80) END,
    CASE WHEN ((g - 1) % 5) >= 3 THEN NULL ELSE 5 + (g % 40) END
FROM generate_series(1, 2000) AS g
CROSS JOIN LATERAL (
    SELECT id
    FROM tmp_bulk_providers
    WHERE k = 1 + ((g - 1) % (SELECT COUNT(*)::int FROM tmp_bulk_providers))
) AS p;

-- ---------------------------------------------------------------------------
-- 4) Customer users + profiles + RBAC (CUSTOMER role from seed)
-- ---------------------------------------------------------------------------
INSERT INTO sys_users (
    id, email, phone, password_hash, role, status, email_verified, phone_verified
)
SELECT
    gen_random_uuid(),
    'bulk.' || u || '@dummy.ziyarah.test',
    '+100000' || LPAD(u::text, 7, '0'),
    '$2a$10$uwlPRc1V5/43DDPrY2LPaevi7OMAloKlVFi2iOqgqUSGA.ptFnxb6',
    'CUSTOMER'::user_role_enum,
    'ACTIVE'::user_status_enum,
    true,
    true
FROM generate_series(1, 10000) AS u;

INSERT INTO customers (user_id, first_name, last_name, preferred_currency)
SELECT
    su.id,
    'Bulk',
    'User' || split_part(split_part(su.email, '@', 1), '.', 2),
    'USD'
FROM sys_users su
WHERE su.email ~ '^bulk\.[0-9]+@dummy\.ziyarah\.test$'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO sys_user_roles (user_id, role_id)
SELECT su.id, r.id
FROM sys_users su
CROSS JOIN LATERAL (
    SELECT id FROM sys_roles WHERE code = 'CUSTOMER' LIMIT 1
) AS r
WHERE su.email ~ '^bulk\.[0-9]+@dummy\.ziyarah\.test$'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 5) Temp maps for bookings (bulk customers only; all services)
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE tmp_bulk_customers ON COMMIT DROP AS
SELECT
    id,
    ROW_NUMBER() OVER (ORDER BY email) AS k
FROM sys_users
WHERE email ~ '^bulk\.[0-9]+@dummy\.ziyarah\.test$';

CREATE TEMP TABLE tmp_all_services ON COMMIT DROP AS
SELECT
    id,
    ROW_NUMBER() OVER (ORDER BY id) AS k
FROM hotel_services;

-- ---------------------------------------------------------------------------
-- 6) Bookings
-- ---------------------------------------------------------------------------
INSERT INTO bkg_bookings (
    id,
    booking_reference,
    customer_id,
    service_id,
    check_in_date,
    check_out_date,
    guests,
    rooms,
    base_amount,
    discount_amount,
    tax_amount,
    commission_amount,
    total_amount,
    currency,
    status
)
SELECT
    gen_random_uuid(),
    'BULK' || LPAD(n::text, 10, '0'),
    c.id,
    s.id,
    CURRENT_DATE + make_interval(days => (n % 180)),
    CURRENT_DATE + make_interval(days => (n % 180) + 1 + (n % 7)),
    1 + (n % 6),
    1 + (n % 3),
    (80 + (n % 900))::numeric(12, 2),
    CASE WHEN n % 11 = 0 THEN (10 + (n % 50))::numeric(12, 2) ELSE 0::numeric(12, 2) END,
    (5 + (n % 40))::numeric(12, 2),
    (8 + (n % 35))::numeric(12, 2),
    (80 + (n % 900) + (5 + (n % 40)) - CASE WHEN n % 11 = 0 THEN (10 + (n % 50)) ELSE 0 END + (8 + (n % 35)))::numeric(12, 2),
    'USD',
    (ARRAY[
        'PENDING'::booking_status_enum,
        'CONFIRMED'::booking_status_enum,
        'COMPLETED'::booking_status_enum,
        'CANCELLED'::booking_status_enum,
        'ACTIVE'::booking_status_enum
    ])[1 + ((n - 1) % 5)]
FROM generate_series(1, 50000) AS n
CROSS JOIN LATERAL (SELECT COUNT(*)::int AS cnt FROM tmp_bulk_customers) AS cc
CROSS JOIN LATERAL (SELECT COUNT(*)::int AS scnt FROM tmp_all_services) AS ss
CROSS JOIN LATERAL (
    SELECT id FROM tmp_bulk_customers WHERE k = 1 + ((n - 1) % cc.cnt)
) AS c
CROSS JOIN LATERAL (
    SELECT id FROM tmp_all_services WHERE k = 1 + ((n - 1) % ss.scnt)
) AS s;

-- ---------------------------------------------------------------------------
-- 7) Payments (one per confirmed/completed bulk booking; idempotent pattern)
-- ---------------------------------------------------------------------------
INSERT INTO pay_payments (
    id, booking_id, amount, currency, method, status, transaction_ref, processed_at
)
SELECT
    gen_random_uuid(),
    b.id,
    b.total_amount,
    b.currency,
    'CREDIT_CARD'::payment_method_enum,
    'COMPLETED'::payment_status_enum,
    'BULK-TXN-' || b.booking_reference,
    CURRENT_TIMESTAMP - ((random() * 48) || ' hours')::interval
FROM bkg_bookings b
WHERE b.booking_reference LIKE 'BULK%'
  AND b.status IN ('CONFIRMED', 'COMPLETED')
  AND NOT EXISTS (SELECT 1 FROM pay_payments p WHERE p.booking_id = b.id);

-- ---------------------------------------------------------------------------
-- 8) Reviews (~1 per 5 bulk bookings; unique booking_id)
-- ---------------------------------------------------------------------------
INSERT INTO hotel_reviews (
    id, booking_id, customer_id, service_id, rating, comment, status
)
SELECT
    gen_random_uuid(),
    x.id,
    x.customer_id,
    x.service_id,
    1 + (abs(hashtext(x.id::text)) % 5),
    'Bulk load-test review #' || x.rn,
    'APPROVED'::review_status_enum
FROM (
    SELECT b.*, ROW_NUMBER() OVER (ORDER BY b.created_at, b.id) AS rn
    FROM bkg_bookings b
    WHERE b.booking_reference LIKE 'BULK%'
) AS x
WHERE x.rn % 5 = 0
  AND NOT EXISTS (SELECT 1 FROM hotel_reviews r WHERE r.booking_id = x.id);

COMMIT;

-- Help the planner after bulk load
ANALYZE sys_users;
ANALYZE customers;
ANALYZE hotel_service_providers;
ANALYZE hotel_services;
ANALYZE bkg_bookings;
ANALYZE pay_payments;
ANALYZE hotel_reviews;

-- ============================================================================
-- OPTIONAL: remove bulk dummy data before re-running (uncomment & execute)
-- Order respects FKs. Run outside the transaction above.
-- ============================================================================
-- DELETE FROM hotel_reviews WHERE comment LIKE 'Bulk load-test review %';
-- DELETE FROM pay_payments WHERE transaction_ref LIKE 'BULK-TXN-BULK%';
-- DELETE FROM bkg_bookings WHERE booking_reference LIKE 'BULK%';
-- DELETE FROM sys_user_roles WHERE user_id IN (
--   SELECT id FROM sys_users WHERE email ~ '^bulk\.[0-9]+@dummy\.ziyarah\.test$'
-- );
-- DELETE FROM customers WHERE user_id IN (
--   SELECT id FROM sys_users WHERE email ~ '^bulk\.[0-9]+@dummy\.ziyarah\.test$'
-- );
-- DELETE FROM sys_users WHERE email ~ '^bulk\.[0-9]+@dummy\.ziyarah\.test$';
-- DELETE FROM hotel_services WHERE name LIKE 'Bulk Test Service %';
-- DELETE FROM hotel_service_providers WHERE contact_email LIKE 'bulk.provider.%@dummy.ziyarah.test';
