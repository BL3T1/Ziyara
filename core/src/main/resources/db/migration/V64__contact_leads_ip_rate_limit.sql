-- Rename client_ip → ip_address, shrink to IPv6 max (45 chars), add composite rate-limit index.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns
             WHERE table_name = 'support_contact_leads' AND column_name = 'client_ip') THEN
    ALTER TABLE support_contact_leads RENAME COLUMN client_ip TO ip_address;
  END IF;
END $$;

-- Truncate any legacy values that exceed the new width before altering the type
UPDATE support_contact_leads
SET ip_address = LEFT(ip_address, 45)
WHERE LENGTH(ip_address) > 45;

ALTER TABLE support_contact_leads ALTER COLUMN ip_address TYPE VARCHAR(45);

-- Supports the combined email+IP rate-limit query (countByEmailAndIpAddressSince)
CREATE INDEX IF NOT EXISTS idx_contact_leads_rl
    ON support_contact_leads (email, ip_address, created_at DESC);
