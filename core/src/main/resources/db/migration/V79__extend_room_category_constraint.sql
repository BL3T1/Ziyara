-- V79: Extend chk_room_category to include DELUXE and EXECUTIVE room categories.
ALTER TABLE hotel_service_rooms
    DROP CONSTRAINT chk_room_category;

ALTER TABLE hotel_service_rooms
    ADD CONSTRAINT chk_room_category CHECK (
        room_category::text = ANY (ARRAY[
            'STANDARD', 'DELUXE', 'SUITE', 'EXECUTIVE'
        ]::text[])
    );
