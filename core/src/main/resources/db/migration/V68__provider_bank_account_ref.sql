-- G3: Store provider bank account details for automated payout disbursement.
ALTER TABLE hotel_service_providers
    ADD COLUMN IF NOT EXISTS bank_account_ref     VARCHAR(120),
    ADD COLUMN IF NOT EXISTS bank_account_name    VARCHAR(200),
    ADD COLUMN IF NOT EXISTS bank_account_country CHAR(2);
