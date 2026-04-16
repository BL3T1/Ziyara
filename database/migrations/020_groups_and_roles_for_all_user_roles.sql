-- Every UserRole enum value has a sys_roles row linked to an organizational group (Groups admin page).
-- Adds G7 B2C Customers; assigns CUSTOMER to G7; inserts missing system roles idempotently.

INSERT INTO sys_groups (id, name, code, description) VALUES
    ('b0000000-0000-0000-0000-000000000007', 'B2C Customers', 'G7', 'End-customer accounts (bookings and profile)')
ON CONFLICT (code) DO NOTHING;

UPDATE sys_roles SET group_id = 'b0000000-0000-0000-0000-000000000001'::uuid WHERE code = 'SUPER_ADMIN';
UPDATE sys_roles SET group_id = 'b0000000-0000-0000-0000-000000000002'::uuid WHERE code = 'SALES_MANAGER';
UPDATE sys_roles SET group_id = 'b0000000-0000-0000-0000-000000000007'::uuid WHERE code = 'CUSTOMER';

INSERT INTO sys_roles (id, name, code, description, level, group_id, is_system_role, status) VALUES
    ('c0000000-0000-0000-0000-000000000010', 'CEO', 'CEO', 'Chief Executive Officer', 'EXECUTIVE', 'b0000000-0000-0000-0000-000000000001', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000011', 'General Manager', 'GENERAL_MANAGER', 'General management', 'EXECUTIVE', 'b0000000-0000-0000-0000-000000000001', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000012', 'Sales Representative', 'SALES_REPRESENTATIVE', 'Sales representative', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000002', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000013', 'Finance Manager', 'FINANCE_MANAGER', 'Finance team lead', 'MANAGER', 'b0000000-0000-0000-0000-000000000003', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000014', 'Accountant', 'ACCOUNTANT', 'Accounting', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000003', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000015', 'Support Manager', 'SUPPORT_MANAGER', 'Support team lead', 'MANAGER', 'b0000000-0000-0000-0000-000000000004', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000016', 'Support Agent', 'SUPPORT_AGENT', 'Customer support agent', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000004', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000017', 'HR Manager', 'HR_MANAGER', 'Human resources manager', 'MANAGER', 'b0000000-0000-0000-0000-000000000005', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000018', 'Provider Manager', 'PROVIDER_MANAGER', 'Partner account manager', 'MANAGER', 'b0000000-0000-0000-0000-000000000006', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-000000000019', 'Provider Finance', 'PROVIDER_FINANCE', 'Partner finance', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000006', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-00000000001a', 'Provider Staff', 'PROVIDER_STAFF', 'Partner staff', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000006', true, 'ACTIVE'),
    ('c0000000-0000-0000-0000-00000000001b', 'Taxi Operator', 'TAXI_OPERATOR', 'Taxi operations', 'EMPLOYEE', 'b0000000-0000-0000-0000-000000000006', true, 'ACTIVE')
ON CONFLICT (code) DO UPDATE SET
    group_id = EXCLUDED.group_id,
    level = EXCLUDED.level,
    description = EXCLUDED.description,
    name = EXCLUDED.name;
