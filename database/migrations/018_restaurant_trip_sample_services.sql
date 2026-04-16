-- ============================================================================
-- Sample RESTAURANT + TRIP services (UI: /services/restaurants, /services/trips)
-- Idempotent: fixed UUIDs, skips if row already exists.
-- Matches seed.sql extras (012–016 restaurants, 021–025 trips).
-- ============================================================================

-- Restaurants (6 total: 010–016)
INSERT INTO hotel_services (id, provider_id, type, name, description, city, country, base_price, currency, status, max_guests)
SELECT * FROM (VALUES
    ('f1000000-0000-0000-0000-000000000010'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'RESTAURANT'::service_type_enum, 'Marina Terrace Restaurant', 'Seafood and international cuisine with marina views.', 'Dubai', 'UAE', 75.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 8),
    ('f1000000-0000-0000-0000-000000000012'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'RESTAURANT'::service_type_enum, 'Olea Garden', 'Mediterranean mezze and wood-fired dishes.', 'Dubai', 'UAE', 65.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 10),
    ('f1000000-0000-0000-0000-000000000013'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'RESTAURANT'::service_type_enum, 'Saffron Table', 'Contemporary Indian fusion.', 'Dubai', 'UAE', 55.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 8),
    ('f1000000-0000-0000-0000-000000000014'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'RESTAURANT'::service_type_enum, 'Downtown Grill', 'Premium steaks and grill classics.', 'Dubai', 'UAE', 95.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 6),
    ('f1000000-0000-0000-0000-000000000015'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'RESTAURANT'::service_type_enum, 'Harbor Sushi', 'Omakase and seasonal Japanese.', 'Dubai', 'UAE', 110.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 6),
    ('f1000000-0000-0000-0000-000000000016'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'RESTAURANT'::service_type_enum, 'Oasis Café', 'Levantine breakfast and light bites.', 'Riyadh', 'Saudi Arabia', 35.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 12)
) AS v(id, provider_id, type, name, description, city, country, base_price, currency, status, max_guests)
WHERE NOT EXISTS (SELECT 1 FROM hotel_services s WHERE s.id = v.id);

-- Trips (6 total: 011, 021–025)
INSERT INTO hotel_services (id, provider_id, type, name, description, city, country, base_price, currency, status, max_guests)
SELECT * FROM (VALUES
    ('f1000000-0000-0000-0000-000000000011'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'TRIP'::service_type_enum, 'Heritage City Tour', 'Half-day guided cultural and heritage experience.', 'Dubai', 'UAE', 120.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 12),
    ('f1000000-0000-0000-0000-000000000021'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'TRIP'::service_type_enum, 'Desert Dunes Safari', 'Full-day 4x4 dunes, camp dinner, entertainment.', 'Dubai', 'UAE', 189.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 14),
    ('f1000000-0000-0000-0000-000000000022'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'TRIP'::service_type_enum, 'Coral Reef Snorkel', 'Boat trip with guided snorkeling.', 'Dubai', 'UAE', 95.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 10),
    ('f1000000-0000-0000-0000-000000000023'::uuid, 'f0000000-0000-0000-0000-000000000001'::uuid, 'TRIP'::service_type_enum, 'Old Souk Walking Tour', 'Guided heritage walk and tastings.', 'Dubai', 'UAE', 45.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 15),
    ('f1000000-0000-0000-0000-000000000024'::uuid, 'f0000000-0000-0000-0000-000000000002'::uuid, 'TRIP'::service_type_enum, 'Mountain Day Hike', 'Scenic trail with picnic lunch.', 'Riyadh', 'Saudi Arabia', 135.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 12),
    ('f1000000-0000-0000-0000-000000000025'::uuid, 'f0000000-0000-0000-0000-000000000002'::uuid, 'TRIP'::service_type_enum, 'Sunset Yacht Cruise', 'Evening coastal cruise with refreshments.', 'Jeddah', 'Saudi Arabia', 160.00::numeric, 'USD', 'ACTIVE'::service_status_enum, 20)
) AS v(id, provider_id, type, name, description, city, country, base_price, currency, status, max_guests)
WHERE NOT EXISTS (SELECT 1 FROM hotel_services s WHERE s.id = v.id);
