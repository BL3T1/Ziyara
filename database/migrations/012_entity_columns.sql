-- ============================================================================
-- Entity columns - add columns required by JPA entities (idempotent)
-- Run after: migration 011
-- ============================================================================

-- departments: manager_id for DepartmentJpaEntity
ALTER TABLE departments ADD COLUMN IF NOT EXISTS manager_id UUID REFERENCES users(id);
COMMENT ON COLUMN departments.manager_id IS 'Optional manager (user) for this department';

-- notifications: message, template_name, updated_at for NotificationJpaEntity
-- Schema has 'content'; entity uses 'message'. Add message and backfill from content.
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS message TEXT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS template_name VARCHAR(255);
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

UPDATE notifications SET message = content WHERE message IS NULL AND content IS NOT NULL;

COMMENT ON COLUMN notifications.message IS 'Notification body (entity field); content retained for compatibility';
COMMENT ON COLUMN notifications.template_name IS 'Optional template identifier';
COMMENT ON COLUMN notifications.updated_at IS 'Last update timestamp';

-- refunds: currency, transaction_reference for RefundJpaEntity
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'USD';
ALTER TABLE refunds ADD COLUMN IF NOT EXISTS transaction_reference VARCHAR(100);
COMMENT ON COLUMN refunds.currency IS 'Refund currency';
COMMENT ON COLUMN refunds.transaction_reference IS 'External or gateway transaction reference';
