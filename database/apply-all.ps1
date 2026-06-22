# ==============================================================================
# DEPRECATED — DO NOT USE
# ==============================================================================
#
# This script applied manual SQL migrations from database/migrations/ and is no
# longer needed. Database schema management has been consolidated into Flyway.
#
# HOW IT WORKS NOW:
#   Flyway runs automatically when the Spring Boot application starts.
#   Migration files:  core/src/main/resources/db/migration/V0__*.sql … V17__*.sql
#   Dev seed data:    database/seed_dev.sql  (run manually after first app start)
#
# QUICK START (dev):
#   1. docker compose up postgres -d          # start plain postgres:15 container
#   2. docker compose up backend              # Flyway creates schema + V17 reference data
#   3. psql ... -f database/seed_dev.sql      # optional: load demo services/bookings
#
# PRODUCTION:
#   Flyway runs automatically. No manual SQL steps are needed.
#   See core/src/main/resources/db/migration/ for all versioned migrations.
#   See ops/FIRST_RUN_PROD.md for the production first-run checklist.
#
# ==============================================================================

Write-Error "apply-all.ps1 is deprecated. See the comment at the top of this file for the current workflow."
exit 1
