# Infrastructure manifests (Snyk IaC)

This folder holds a **copy** of the root [`docker-compose.yml`](../docker-compose.yml) so `snyk iac test` (or `snyk iac test infra`) can run **without** traversing `front/my-app/node_modules` (which otherwise triggers SNYK-CLI-0012 on some setups).

**Maintenance:** When you change compose services, ports, or env wiring at the repo root, update this copy (or re-copy the file) so scans and docs stay accurate.

**Snyk IaC:** Some Snyk CLI versions still report “no valid IaC files” for Compose (`SNYK-CLI-0012`). The copy includes `version: "3.9"` and paths adjusted for `docker compose -f infra/docker-compose.yml` run from the **repo root** (`context: ../core`, etc.). Use manual review or a future Snyk release if automated Compose scanning is unavailable.
