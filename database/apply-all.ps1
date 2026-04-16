# Ziyarah Database - Apply schema, migrations 001-015, seed, then 018-026
# Migration order: schema -> 001-015 -> seed -> 018-025 (see database/Dockerfile)
# Requires: PostgreSQL client (psql) and env PGPASSWORD or .pgpass
# Usage: .\apply-all.ps1
# Or: $env:PGPASSWORD='ziyarah_password'; .\apply-all.ps1

$ErrorActionPreference = "Stop"
$DB_HOST = if ($env:PGHOST) { $env:PGHOST } else { "localhost" }
$DB_PORT = if ($env:PGPORT) { $env:PGPORT } else { "5432" }
$DB_NAME = if ($env:PGDATABASE) { $env:PGDATABASE } else { "ziyarah" }
$DB_USER = if ($env:PGUSER) { $env:PGUSER } else { "ziyarah_user" }
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "Applying Ziyarah database to $DB_NAME@$DB_HOST:$DB_PORT as $DB_USER"

# 1. Schema
Write-Host "`n[1/16] Applying schema.sql..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\schema.sql"

# 2. Migration 001
Write-Host "`n[2/16] Applying migration 001..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\001_plans_schema_extensions.sql"

# 3. Migration 002
Write-Host "`n[3/16] Applying migration 002..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\002_role_management_report.sql"

# 4. Migration 003
Write-Host "`n[4/16] Applying migration 003..."
try { psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\003_pricing_and_payment_methods.sql" 2>$null } catch { Write-Host "  (003 may have partial apply - continuing)" }

# 5-8. Migrations 004-008 (Hibernate compat, JPA alignments)
Write-Host "`n[5/16] Applying migration 004..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\004_hibernate_enum_compat.sql"
Write-Host "`n[6/16] Applying migration 005..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\005_reviews_status_varchar.sql"
Write-Host "`n[7/16] Applying migration 006..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\006_discount_codes_jpa_compat.sql"
Write-Host "`n[8/16] Applying migration 007..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\007_service_providers_jpa_compat.sql"
Write-Host "`n[9/16] Applying migration 008..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\008_employees_payments_enum_compat.sql"

# 10. Migration 009 (auth tables)
Write-Host "`n[10/16] Applying migration 009 (auth tables)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\009_auth_tokens_otp.sql"

# 11. Migration 010 (Arabic i18n)
Write-Host "`n[11/16] Applying migration 010 (Arabic i18n)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\010_ar_columns_i18n.sql"

# 12. Migration 011 (remaining Hibernate enum compat)
Write-Host "`n[12/16] Applying migration 011 (enum compat)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\011_hibernate_enum_compat_remaining.sql"

# 13. Migration 012 (entity columns)
Write-Host "`n[13/17] Applying migration 012 (entity columns)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\012_entity_columns.sql"

# 14. Migration 013 (payment gateway / 3DS columns)
Write-Host "`n[14/17] Applying migration 013 (payment gateway 3DS)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\013_payment_gateway_3ds_columns.sql"

# 15. Migration 014 (provider commission audit, refunds processed_by)
Write-Host "`n[15/17] Applying migration 014 (commission audit)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\014_provider_commission_audit.sql"

# 16. Migration 015 (Phase 4 table prefixes – run only on DB that has 001-014; backup first)
Write-Host "`n[16/17] Applying migration 015 (table prefixes)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\015_table_prefix_phase4.sql"

# 17. Seed (uses prefixed table names after 015)
Write-Host "`n[17/22] Applying seed.sql..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\seed.sql"

# 18–21 (align with database/Dockerfile init order after seed)
Write-Host "`n[18/22] Applying migration 018..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\018_restaurant_trip_sample_services.sql"
Write-Host "`n[19/22] Applying migration 019..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\019_service_image_category_and_restaurant_menu.sql"
Write-Host "`n[20/22] Applying migration 020..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\020_groups_and_roles_for_all_user_roles.sql"
Write-Host "`n[21/22] Applying migration 021 (settings + contact leads)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\021_system_settings_contact_leads.sql"

Write-Host "`n[22/23] Applying migration 022 (RBAC permission catalogue)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\022_rbac_permission_catalogue.sql"

Write-Host "`n[23/24] Applying migration 023 (provider portal staff)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\023_provider_portal_staff.sql"

Write-Host "`n[24/26] Applying migration 024 (feature flags + API keys)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\024_feature_flags_and_api_keys.sql"

Write-Host "`n[25/26] Applying migration 025 (portal support requests)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\025_portal_support_requests.sql"

Write-Host "`n[26/27] Applying migration 026 (service_type RESORT enum if missing)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\026_service_type_resort_enum.sql"

Write-Host "`n[27/30] Applying migration 027 (disc_discount_codes legacy columns nullable)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\027_disc_discount_codes_legacy_nullable.sql"

Write-Host "`n[28/30] Applying migration 028 (disc_discount_codes sponsor)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\028_disc_discount_codes_sponsor.sql"

Write-Host "`n[29/30] Applying migration 029 (strip sales_providers from role nav)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\029_sys_roles_remove_sales_providers_nav.sql"

Write-Host "`n[30/30] Applying migration 030 (discount provider/listing/menu/room scope)..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$SCRIPT_DIR\migrations\030_disc_discount_scope.sql"

Write-Host "`nDone! Test login: admin@ziyarah.com / password"
