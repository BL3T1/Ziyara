-- Additional organizational groups so the admin Groups page lists the full Z1–Z6-style set.
-- Idempotent: safe to re-run.
INSERT INTO sys_groups (id, name, code, description) VALUES
    ('b0000000-0000-0000-0000-000000000004', 'Support', 'Z4', 'Customer support and service operations'),
    ('b0000000-0000-0000-0000-000000000005', 'HR & People', 'Z5', 'Human resources and internal people operations'),
    ('b0000000-0000-0000-0000-000000000006', 'Provider Partner', 'Z6', 'Partner and provider-facing accounts')
ON CONFLICT (code) DO NOTHING;
