-- Additional organizational groups so the admin Groups page lists the full G1–G6-style set.
-- Idempotent: safe to re-run.
INSERT INTO sys_groups (id, name, code, description) VALUES
    ('b0000000-0000-0000-0000-000000000004', 'Support', 'G4', 'Customer support and service operations'),
    ('b0000000-0000-0000-0000-000000000005', 'HR & People', 'G5', 'Human resources and internal people operations'),
    ('b0000000-0000-0000-0000-000000000006', 'Provider Partner', 'G6', 'Partner and provider-facing accounts')
ON CONFLICT (code) DO NOTHING;
