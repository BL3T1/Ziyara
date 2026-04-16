# Ziyarah Database - PostgreSQL with schema + migrations + seed
# Scripts run in lexicographic order: 01-schema … 17-seed … 21-021 … 25-025

FROM postgres:15.17-alpine3.23

LABEL maintainer="Ziyarah Team"
LABEL description="Ziyarah PostgreSQL Database"

ENV POSTGRES_DB=ziyarah
ENV POSTGRES_USER=ziyarah_user
# Placeholder only: docker-compose must set POSTGRES_PASSWORD (required). Do not use this default in production.
ENV POSTGRES_PASSWORD=change-me-at-runtime

COPY schema.sql /docker-entrypoint-initdb.d/01-schema.sql
COPY migrations/001_plans_schema_extensions.sql /docker-entrypoint-initdb.d/02-001.sql
COPY migrations/002_role_management_report.sql /docker-entrypoint-initdb.d/03-002.sql
COPY migrations/003_pricing_and_payment_methods.sql /docker-entrypoint-initdb.d/04-003.sql
COPY migrations/004_hibernate_enum_compat.sql /docker-entrypoint-initdb.d/05-004.sql
COPY migrations/005_reviews_status_varchar.sql /docker-entrypoint-initdb.d/06-005.sql
COPY migrations/006_discount_codes_jpa_compat.sql /docker-entrypoint-initdb.d/07-006.sql
COPY migrations/007_service_providers_jpa_compat.sql /docker-entrypoint-initdb.d/08-007.sql
COPY migrations/008_employees_payments_enum_compat.sql /docker-entrypoint-initdb.d/09-008.sql
COPY migrations/009_auth_tokens_otp.sql /docker-entrypoint-initdb.d/10-009.sql
COPY migrations/010_ar_columns_i18n.sql /docker-entrypoint-initdb.d/11-010.sql
COPY migrations/011_hibernate_enum_compat_remaining.sql /docker-entrypoint-initdb.d/12-011.sql
COPY migrations/012_entity_columns.sql /docker-entrypoint-initdb.d/13-012.sql
COPY migrations/013_payment_gateway_3ds_columns.sql /docker-entrypoint-initdb.d/14-013.sql
COPY migrations/014_provider_commission_audit.sql /docker-entrypoint-initdb.d/15-014.sql
COPY migrations/015_table_prefix_phase4.sql /docker-entrypoint-initdb.d/16-015.sql
COPY seed.sql /docker-entrypoint-initdb.d/17-seed.sql
COPY migrations/018_restaurant_trip_sample_services.sql /docker-entrypoint-initdb.d/18-018.sql
COPY migrations/019_service_image_category_and_restaurant_menu.sql /docker-entrypoint-initdb.d/19-019.sql
COPY migrations/020_groups_and_roles_for_all_user_roles.sql /docker-entrypoint-initdb.d/20-020.sql
COPY migrations/021_system_settings_contact_leads.sql /docker-entrypoint-initdb.d/21-021.sql
COPY migrations/022_rbac_permission_catalogue.sql /docker-entrypoint-initdb.d/22-022.sql
COPY migrations/023_provider_portal_staff.sql /docker-entrypoint-initdb.d/23-023.sql
COPY migrations/024_feature_flags_and_api_keys.sql /docker-entrypoint-initdb.d/24-024.sql
COPY migrations/025_portal_support_requests.sql /docker-entrypoint-initdb.d/25-025.sql
COPY migrations/026_service_type_resort_enum.sql /docker-entrypoint-initdb.d/26-026.sql
COPY migrations/027_disc_discount_codes_legacy_nullable.sql /docker-entrypoint-initdb.d/27-027.sql
COPY migrations/028_disc_discount_codes_sponsor.sql /docker-entrypoint-initdb.d/28-028.sql
COPY migrations/029_sys_roles_remove_sales_providers_nav.sql /docker-entrypoint-initdb.d/29-029.sql
COPY migrations/030_disc_discount_scope.sql /docker-entrypoint-initdb.d/30-030.sql

EXPOSE 5432

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
    CMD pg_isready -U $POSTGRES_USER -d $POSTGRES_DB || exit 1
