-- ============================================================================
-- V44 — Fix amenities JSONB: convert JSON arrays to JSON objects.
--
-- ServiceJpaEntity.amenities is Map<String,Object>, so Hibernate/Jackson
-- expects a JSON object ({"Wi-Fi": true, ...}).  V43 incorrectly stored
-- amenities as a JSON array (["Wi-Fi", ...]), causing a deserialization
-- error on every booking attempt that reads those service rows.
--
-- Converts ALL rows where amenities/attributes is a JSON array by expanding
-- each array element into a key with boolean true value.
-- Rows that already hold a JSON object are untouched.
-- ============================================================================

-- Fix amenities: ["Wi-Fi", "TV"] → {"Wi-Fi": true, "TV": true}
-- to_jsonb(true) produces the JSON literal `true`; TRUE::jsonb is not valid PostgreSQL syntax.
UPDATE hotel_services
SET amenities = (
    SELECT jsonb_object_agg(elem, to_jsonb(true))
    FROM jsonb_array_elements_text(amenities) AS elem
)
WHERE amenities IS NOT NULL
  AND jsonb_typeof(amenities) = 'array';

-- Fix attributes for good measure (same rule applies)
UPDATE hotel_services
SET attributes = (
    SELECT jsonb_object_agg(elem, to_jsonb(true))
    FROM jsonb_array_elements_text(attributes) AS elem
)
WHERE attributes IS NOT NULL
  AND jsonb_typeof(attributes) = 'array';
