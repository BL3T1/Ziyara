-- ============================================================================
-- RLS productization (session GUCs set by app), native HASH partitioning for
-- sys_audit_logs, GDPR export payload column.
-- Idempotent where possible. Requires app datasource to set:
--   app.rls_bypass, app.current_user_id, app.current_provider_id (see RlsAwareDataSource).
-- ============================================================================

-- --- Data export: full JSON payload (replaces truncated export_path-only snapshots) ----
ALTER TABLE sys_data_export_requests ADD COLUMN IF NOT EXISTS payload_json JSONB;
ALTER TABLE sys_data_export_requests ADD COLUMN IF NOT EXISTS record_count INTEGER;

-- --- sys_audit_logs: native HASH partitioning on id (keeps single-column PK for JPA) -----
DO $$
DECLARE
    is_partitioned BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relname = 'sys_audit_logs'
          AND c.relkind = 'p'
    ) INTO is_partitioned;

    IF is_partitioned THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'sys_audit_logs'
    ) THEN
        RETURN;
    END IF;

    ALTER TABLE public.sys_audit_logs RENAME TO sys_audit_logs_legacy_hashpart;

    -- schema.sql audit_logs shape: old_values/new_values JSONB, no entity_name / old_value / new_value TEXT
    ALTER TABLE public.sys_audit_logs_legacy_hashpart ADD COLUMN IF NOT EXISTS entity_name TEXT;
    ALTER TABLE public.sys_audit_logs_legacy_hashpart ADD COLUMN IF NOT EXISTS old_value TEXT;
    ALTER TABLE public.sys_audit_logs_legacy_hashpart ADD COLUMN IF NOT EXISTS new_value TEXT;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'sys_audit_logs_legacy_hashpart'
          AND column_name = 'old_values'
    ) THEN
        EXECUTE $audit_backfill$
            UPDATE public.sys_audit_logs_legacy_hashpart SET
                old_value = COALESCE(old_value, old_values::text),
                new_value = COALESCE(new_value, new_values::text)
        $audit_backfill$;
    END IF;

    CREATE TABLE public.sys_audit_logs (
        id UUID NOT NULL,
        action VARCHAR(255) NOT NULL,
        entity_type VARCHAR(100) NOT NULL,
        entity_name TEXT,
        entity_id VARCHAR(255),
        user_id UUID,
        old_value TEXT,
        new_value TEXT,
        ip_address VARCHAR(50),
        user_agent TEXT,
        correlation_id VARCHAR(100),
        request_id VARCHAR(100),
        session_id UUID,
        provider_id UUID,
        tenant_id UUID,
        risk_score INTEGER,
        duration_ms INTEGER,
        tags TEXT,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id)
    ) PARTITION BY HASH (id);

    FOR i IN 0..7 LOOP
        EXECUTE format(
            'CREATE TABLE public.sys_audit_logs_p%s PARTITION OF public.sys_audit_logs FOR VALUES WITH (MODULUS 8, REMAINDER %s)',
            i, i
        );
    END LOOP;

    INSERT INTO public.sys_audit_logs (
        id, action, entity_type, entity_name, entity_id, user_id, old_value, new_value,
        ip_address, user_agent, correlation_id, request_id, session_id, provider_id,
        tenant_id, risk_score, duration_ms, tags, created_at
    )
    SELECT
        id, action, entity_type, entity_name, entity_id, user_id, old_value, new_value,
        ip_address, user_agent, correlation_id, request_id, session_id, provider_id,
        tenant_id, risk_score, duration_ms, tags, created_at::timestamp
    FROM public.sys_audit_logs_legacy_hashpart;

    DROP TABLE public.sys_audit_logs_legacy_hashpart;

    CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_entity_timeline
        ON public.sys_audit_logs (entity_type, entity_id, created_at DESC);
    CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_correlation ON public.sys_audit_logs (correlation_id);
    CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_request ON public.sys_audit_logs (request_id);
    CREATE INDEX IF NOT EXISTS idx_sys_audit_logs_created_brin ON public.sys_audit_logs USING BRIN (created_at);
END $$;

-- --- Row-level security (defense in depth; app must set session variables per connection) --
ALTER TABLE IF EXISTS public.hotel_service_providers ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.hotel_service_providers FORCE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.hotel_services ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.hotel_services FORCE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.bkg_bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE IF EXISTS public.bkg_bookings FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS hotel_service_providers_rls ON public.hotel_service_providers;
CREATE POLICY hotel_service_providers_rls ON public.hotel_service_providers
    FOR ALL
    TO PUBLIC
    USING (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR id::text = NULLIF(current_setting('app.current_provider_id', true), '')
        OR created_by::text = NULLIF(current_setting('app.current_user_id', true), '')
        OR EXISTS (
            SELECT 1 FROM public.hotel_provider_staff ps
            WHERE ps.provider_id = hotel_service_providers.id
              AND ps.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        )
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR id::text = NULLIF(current_setting('app.current_provider_id', true), '')
        OR created_by::text = NULLIF(current_setting('app.current_user_id', true), '')
        OR EXISTS (
            SELECT 1 FROM public.hotel_provider_staff ps
            WHERE ps.provider_id = hotel_service_providers.id
              AND ps.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        )
    );

DROP POLICY IF EXISTS hotel_services_rls ON public.hotel_services;
CREATE POLICY hotel_services_rls ON public.hotel_services
    FOR ALL
    TO PUBLIC
    USING (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR provider_id::text = NULLIF(current_setting('app.current_provider_id', true), '')
        OR EXISTS (
            SELECT 1 FROM public.hotel_provider_staff ps
            WHERE ps.provider_id = hotel_services.provider_id
              AND ps.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        )
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR provider_id::text = NULLIF(current_setting('app.current_provider_id', true), '')
        OR EXISTS (
            SELECT 1 FROM public.hotel_provider_staff ps
            WHERE ps.provider_id = hotel_services.provider_id
              AND ps.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        )
    );

DROP POLICY IF EXISTS bkg_bookings_rls ON public.bkg_bookings;
CREATE POLICY bkg_bookings_rls ON public.bkg_bookings
    FOR ALL
    TO PUBLIC
    USING (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR customer_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        OR EXISTS (
            SELECT 1 FROM public.hotel_services hs
            WHERE hs.id = bkg_bookings.service_id
              AND hs.provider_id::text = NULLIF(current_setting('app.current_provider_id', true), '')
        )
        OR EXISTS (
            SELECT 1 FROM public.hotel_services hs
            JOIN public.hotel_provider_staff ps ON ps.provider_id = hs.provider_id
            WHERE hs.id = bkg_bookings.service_id
              AND ps.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        )
    )
    WITH CHECK (
        COALESCE(NULLIF(current_setting('app.rls_bypass', true), ''), '1') = '1'
        OR customer_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        OR EXISTS (
            SELECT 1 FROM public.hotel_services hs
            WHERE hs.id = bkg_bookings.service_id
              AND hs.provider_id::text = NULLIF(current_setting('app.current_provider_id', true), '')
        )
        OR EXISTS (
            SELECT 1 FROM public.hotel_services hs
            JOIN public.hotel_provider_staff ps ON ps.provider_id = hs.provider_id
            WHERE hs.id = bkg_bookings.service_id
              AND ps.user_id::text = NULLIF(current_setting('app.current_user_id', true), '')
        )
    );
