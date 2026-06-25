-- V78: Extend provider_type CHECK constraint to include all ServiceType values.
ALTER TABLE hotel_service_providers
    DROP CONSTRAINT chk_provider_type;

ALTER TABLE hotel_service_providers
    ADD CONSTRAINT chk_provider_type CHECK (
        provider_type::text = ANY (ARRAY[
            'HOTEL', 'RESORT', 'APARTMENT', 'EVENT_SPACE',
            'TOUR_OPERATOR', 'RESTAURANT', 'TAXI', 'TRIP'
        ]::text[])
    );
