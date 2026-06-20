-- Add rejection_reason to hotel_services so providers can see why a listing was rejected
ALTER TABLE hotel_services ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
