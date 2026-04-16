# Ziyara - Run Frontend + Backend for local testing
# Prerequisites: Docker Desktop running (for Postgres)

Write-Host "=== Ziyara Dev Startup ===" -ForegroundColor Cyan

# 1. Start Postgres
Write-Host "`n[1/3] Starting Postgres..." -ForegroundColor Yellow
docker compose up postgres -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker failed. Is Docker Desktop running?" -ForegroundColor Red
    Write-Host "Start Docker Desktop, then run this script again." -ForegroundColor Red
    exit 1
}
Write-Host "Waiting for Postgres to be ready..." -ForegroundColor Gray
Start-Sleep -Seconds 5

# 2. Start Backend (Spring Boot) - dev profile creates DB schema
Write-Host "`n[2/3] Starting Backend (core) on http://localhost:8080..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\core'; .\gradlew bootRun --args='--spring.profiles.active=dev'" -WindowStyle Normal

# 3. Start Frontend (Vite dashboard)
Write-Host "`n[3/3] Starting Frontend on http://localhost:5173..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot\front\my-app'; npm run dev" -WindowStyle Normal

Write-Host "`n=== Done ===" -ForegroundColor Green
Write-Host "Backend:  http://localhost:8080/api/v1" -ForegroundColor White
Write-Host "Frontend: http://localhost:5173 (Vite)" -ForegroundColor White
Write-Host "Demo login: super_admin@ziyarah.com / Demo123!" -ForegroundColor Gray
