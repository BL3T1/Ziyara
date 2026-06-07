# build-portmode.ps1
# Builds all three frontend surfaces locally, then packages each into a Docker image.
# Run this instead of docker compose --profile portmode up -d --build
#
# Usage:
#   .\build-portmode.ps1                      # defaults: localhost, standard ports
#   .\build-portmode.ps1 -Host 10.45.30.145   # LAN access

param(
    [string]$Host    = $env:PORTMODE_HOST -ne "" ? $env:PORTMODE_HOST : "localhost",
    [string]$CompanyPort  = $env:COMPANY_PORT  -ne "" ? $env:COMPANY_PORT  : "7050",
    [string]$ProviderPort = $env:PROVIDER_PORT -ne "" ? $env:PROVIDER_PORT : "7060",
    [string]$LandingPort  = $env:LANDING_PORT  -ne "" ? $env:LANDING_PORT  : "7070"
)

$frontDir = Join-Path $PSScriptRoot "front\my-app"
Push-Location $frontDir

$ErrorActionPreference = "Stop"

function Build-Surface {
    param([string]$Surface, [string]$Tag, [hashtable]$Env)

    Write-Host ""
    Write-Host "==> Building $Surface ($Tag)..." -ForegroundColor Cyan

    foreach ($kv in $Env.GetEnumerator()) {
        [System.Environment]::SetEnvironmentVariable($kv.Key, $kv.Value, "Process")
    }

    npm run build
    if ($LASTEXITCODE -ne 0) { throw "npm run build failed for $Surface" }

    docker build -t $Tag --file Dockerfile.prebuilt .
    if ($LASTEXITCODE -ne 0) { throw "docker build failed for $Tag" }

    Write-Host "    $Tag ready" -ForegroundColor Green
}

# Install dependencies once (runs locally — fast, no Docker network issues)
Write-Host "==> Installing dependencies..." -ForegroundColor Cyan
npm ci --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw "npm ci failed" }

Build-Surface "company" "ziyarah-company-portmode:latest" @{
    VITE_APP_SURFACE      = "company"
    VITE_API_URL          = "/api/v1"
    VITE_PROVIDER_APP_URL = "http://${Host}:${ProviderPort}"
}

Build-Surface "provider" "ziyarah-provider-portmode:latest" @{
    VITE_APP_SURFACE = "provider"
    VITE_API_URL     = "/api/v1"
}

Build-Surface "landing" "ziyarah-landing-portmode:latest" @{
    VITE_APP_SURFACE      = "landing"
    VITE_API_URL          = "/api/v1"
    VITE_COMPANY_APP_URL  = "http://${Host}:${CompanyPort}"
    VITE_PROVIDER_APP_URL = "http://${Host}:${ProviderPort}"
}

Pop-Location

Write-Host ""
Write-Host "All frontend images built. Starting services..." -ForegroundColor Cyan
docker compose --profile portmode up -d
Write-Host ""
Write-Host "Done." -ForegroundColor Green
Write-Host "  Company:  http://${Host}:${CompanyPort}"
Write-Host "  Provider: http://${Host}:${ProviderPort}"
Write-Host "  Landing:  http://${Host}:${LandingPort}"
