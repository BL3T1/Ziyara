-- Mark roles assignable to provider portal staff (independent of code-prefix guessing).
ALTER TABLE sys_roles
    ADD COLUMN is_provider_role BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE sys_roles SET is_provider_role = TRUE
WHERE code IN ('PROVIDER_MANAGER', 'PROVIDER_FINANCE', 'PROVIDER_STAFF', 'TAXI_OPERATOR');
