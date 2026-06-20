-- ============================================================================
-- V43 — Seed dummy listing data for the hotel provider named "tttt".
--
-- Inserts 3 hotel-service listings attached to whatever hotel_service_providers
-- row has company_name = 'tttt'.  If no such provider exists, the block is
-- silently skipped — safe to run on any environment.
--
-- All rows use fixed UUIDs so the migration is idempotent (ON CONFLICT DO NOTHING).
-- ============================================================================

DO $$
DECLARE
    v_provider_id UUID;
BEGIN
    SELECT id INTO v_provider_id
    FROM hotel_service_providers
    WHERE LOWER(company_name) = 'tttt'
    LIMIT 1;

    IF v_provider_id IS NULL THEN
        RAISE NOTICE 'V43: No provider named "tttt" found — skipping listing seed.';
        RETURN;
    END IF;

    -- ── Listing 1: Standard Room ────────────────────────────────────────────
    INSERT INTO hotel_services (
        id, provider_id, type, name, description,
        location, address, city, country,
        latitude, longitude,
        base_price, currency, status,
        attributes, amenities, policies,
        star_rating, total_rooms, available_rooms, max_guests,
        seasonal_multiplier, tax_rate,
        check_in_time, check_out_time,
        created_at
    ) VALUES (
        'e1000000-0000-0000-0000-000000000001',
        v_provider_id,
        'HOTEL',
        'tttt Hotel — Standard Room',
        'A comfortable standard room with a queen bed, city view, complimentary Wi-Fi and daily housekeeping. Perfect for business travellers and short stays.',
        'Downtown District',
        '12 Al Corniche Street, Downtown',
        'Dubai',
        'United Arab Emirates',
        25.20480, 55.27080,
        120.00, 'USD', 'ACTIVE',
        '{"bedType":"Queen","floor":3,"smokingAllowed":false,"extraBedAvailable":true}'::jsonb,
        '["Free Wi-Fi","Air Conditioning","Flat-Screen TV","Mini Fridge","Safe","Hair Dryer","Coffee Machine","Daily Housekeeping","Room Service"]'::jsonb,
        'Cancellation free up to 48 hours before check-in. No-show charged at full rate. Pets not allowed.',
        4,
        50, 18, 2,
        1.00, 0.0500,
        '14:00', '12:00',
        NOW()
    ) ON CONFLICT (id) DO NOTHING;

    -- ── Listing 2: Deluxe Suite ─────────────────────────────────────────────
    INSERT INTO hotel_services (
        id, provider_id, type, name, description,
        location, address, city, country,
        latitude, longitude,
        base_price, currency, status,
        attributes, amenities, policies,
        star_rating, total_rooms, available_rooms, max_guests,
        seasonal_multiplier, tax_rate,
        check_in_time, check_out_time,
        created_at
    ) VALUES (
        'e1000000-0000-0000-0000-000000000002',
        v_provider_id,
        'HOTEL',
        'tttt Hotel — Deluxe Suite',
        'Spacious deluxe suite featuring a separate living area, king bed, panoramic city and sea views, premium toiletries, and access to the executive lounge.',
        'Downtown District',
        '12 Al Corniche Street, Downtown',
        'Dubai',
        'United Arab Emirates',
        25.20480, 55.27080,
        280.00, 'USD', 'ACTIVE',
        '{"bedType":"King","floor":12,"smokingAllowed":false,"balcony":true,"livingArea":true,"extraBedAvailable":true}'::jsonb,
        '["Free Wi-Fi","Air Conditioning","55-Inch Smart TV","Kitchenette","Mini Bar","Safe","Jacuzzi","Executive Lounge Access","Concierge","Daily Housekeeping","24h Room Service","Airport Transfer","Complimentary Breakfast"]'::jsonb,
        'Free cancellation up to 72 hours before check-in. Early check-in subject to availability. Smoking strictly prohibited.',
        4,
        12, 5, 3,
        1.15, 0.0500,
        '14:00', '12:00',
        NOW()
    ) ON CONFLICT (id) DO NOTHING;

    -- ── Listing 3: Family Apartment ─────────────────────────────────────────
    INSERT INTO hotel_services (
        id, provider_id, type, name, description,
        location, address, city, country,
        latitude, longitude,
        base_price, currency, status,
        attributes, amenities, policies,
        star_rating, total_rooms, available_rooms, max_guests,
        seasonal_multiplier, tax_rate,
        check_in_time, check_out_time,
        created_at
    ) VALUES (
        'e1000000-0000-0000-0000-000000000003',
        v_provider_id,
        'HOTEL',
        'tttt Hotel — Family Apartment',
        'Two-bedroom serviced apartment ideal for families. Features a fully equipped kitchen, two bathrooms, a spacious lounge, children''s bedding available on request.',
        'Downtown District',
        '12 Al Corniche Street, Downtown',
        'Dubai',
        'United Arab Emirates',
        25.20480, 55.27080,
        390.00, 'USD', 'ACTIVE',
        '{"bedrooms":2,"bedTypes":["King","Twin"],"floor":7,"smokingAllowed":false,"fullyEquippedKitchen":true,"washingMachine":true}'::jsonb,
        '["Free Wi-Fi","Air Conditioning","Two Smart TVs","Full Kitchen","Washing Machine","Dishwasher","Two Bathrooms","Children Cots Available","Daily Housekeeping","Parking","Pool Access","Gym Access"]'::jsonb,
        'Free cancellation up to 5 days before check-in. Long-stay discount (7+ nights) applied automatically at checkout.',
        4,
        8, 3, 5,
        1.10, 0.0500,
        '15:00', '11:00',
        NOW()
    ) ON CONFLICT (id) DO NOTHING;

    RAISE NOTICE 'V43: Inserted 3 listings for provider "tttt" (id = %).',  v_provider_id;
END $$;
