-- Remove obsolete sidebar id "sales_providers" (Partner onboarding) from custom role layouts.
-- Safe to re-run: only updates rows that still contain that id.
-- Column is created by the app (JPA ddl) on some deployments; fresh docker-init DB may not have it yet.
DO $migration$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.table_name = 'sys_roles'
      AND c.column_name = 'navigation_item_ids'
  ) THEN
    UPDATE sys_roles r
    SET navigation_item_ids = (
      SELECT COALESCE(jsonb_agg(to_jsonb(elem)), '[]'::jsonb)
      FROM jsonb_array_elements_text(r.navigation_item_ids) AS t(elem)
      WHERE elem <> 'sales_providers'
    )
    WHERE navigation_item_ids IS NOT NULL
      AND EXISTS (
        SELECT 1
        FROM jsonb_array_elements_text(r.navigation_item_ids) AS t(elem)
        WHERE elem = 'sales_providers'
      );
  END IF;
END $migration$;
