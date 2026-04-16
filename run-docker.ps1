# Ziyara - Run full stack via Docker (DB + migrations + seed + backend + Adminer)
#
# Always starts the PRODUCTION-LIKE multi-domain stack:
#   Edge proxy on :80 + company / provider / landing SPA bundles.
#   Add to hosts (as Administrator): 127.0.0.1 app.local partners.local www.local
#   See docs/DOCKER_TESTING.md
#
# Fresh DB: .\run-docker.ps1 -Fresh

param(
    [switch]$Fresh
)

Write-Host "=== Ziyara Docker Stack ===" -ForegroundColor Cyan

if ($Fresh) {
    Write-Host "`n[0] Resetting database (removing volumes)..." -ForegroundColor Yellow
    docker compose down -v
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: docker compose down failed" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n[1] Building & starting multi-domain stack (proxy :80 + company / provider / landing)..." -ForegroundColor Yellow
Write-Host "    Ensure port 80 is free and hosts file has: 127.0.0.1 app.local partners.local www.local" -ForegroundColor Gray
docker compose --profile multidomain up -d --build postgres backend dashboard-company dashboard-provider landing proxy pgadmin

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: docker compose up failed" -ForegroundColor Red
    exit 1
}

Write-Host "`nWaiting for backend to be healthy (up to 2 min)..." -ForegroundColor Gray
$maxWait = 120
$elapsed = 0
while ($elapsed -lt $maxWait) {
    $health = docker inspect --format='{{.State.Health.Status}}' ziyarah-backend 2>$null
    if ($health -eq "healthy") {
        Write-Host "Backend is ready!" -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 5
    $elapsed += 5
    Write-Host "  ... waiting ($elapsed s)" -ForegroundColor Gray
}

Write-Host "`n=== Stack Running ===" -ForegroundColor Green
Write-Host "Use hostnames (prod-like routing via edge proxy):" -ForegroundColor White
Write-Host "  Company:   http://app.local" -ForegroundColor White
Write-Host "  Provider:  http://partners.local" -ForegroundColor White
Write-Host "  Landing:   http://www.local" -ForegroundColor White
Write-Host "Backend:    http://localhost:8080/api/v1" -ForegroundColor White
Write-Host "Swagger:    http://localhost:8080/api/v1/swagger-ui.html" -ForegroundColor White
Write-Host "pgAdmin:    http://localhost:5050 (login: admin@example.com / ziyarah_pgadmin — then add server host postgres)" -ForegroundColor White
Write-Host "`nDemo login: super_admin@ziyarah.com / Demo123!" -ForegroundColor Cyan
Write-Host "`nIf hostnames do not resolve, edit C:\Windows\System32\drivers\etc\hosts as Administrator." -ForegroundColor Yellow
Write-Host "Fresh DB: .\run-docker.ps1 -Fresh" -ForegroundColor Gray
Write-Host "Optional legacy dashboard on :3000 (not prod-like): docker compose --profile legacy up -d dashboard" -ForegroundColor DarkGray
