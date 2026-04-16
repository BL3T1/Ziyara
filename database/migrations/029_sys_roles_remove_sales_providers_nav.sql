-- Remove obsolete sidebar id "sales_providers" (Partner onboarding) from custom role layouts.
-- Safe to re-run: only updates rows that still contain that id.

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
