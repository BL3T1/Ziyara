-- Add staff response fields to provider support requests table.

ALTER TABLE hotel_portal_support_requests
    ADD COLUMN IF NOT EXISTS staff_response TEXT,
    ADD COLUMN IF NOT EXISTS responded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS responded_by_user_id UUID REFERENCES sys_users(id) ON DELETE SET NULL;
