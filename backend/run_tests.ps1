# ─────────────────────────────────────────────────────────────────────────────
# TumaGo — Run All Tests (PowerShell)
# ─────────────────────────────────────────────────────────────────────────────
# Usage:
#   cd backend; .\run_tests.ps1            # unit tests only
#   cd backend; .\run_tests.ps1 -All       # unit + functional tests
# ─────────────────────────────────────────────────────────────────────────────

param([switch]$All)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Pass = 0; $Fail = 0; $Skip = 0

function Run-Section($title) {
    Write-Host ""
    Write-Host "==============================================================" -ForegroundColor Cyan
    Write-Host "  $title" -ForegroundColor Cyan
    Write-Host "==============================================================" -ForegroundColor Cyan
}

function Mark-Pass   { $script:Pass++; Write-Host "  PASSED" -ForegroundColor Green }
function Mark-Fail   { $script:Fail++; Write-Host "  FAILED" -ForegroundColor Red }
function Mark-Skip($reason) { $script:Skip++; Write-Host "  SKIPPED - $reason" -ForegroundColor Yellow }

# ── 1. Django Unit Tests ────────────────────────────────────────────────────

Run-Section "Django Unit Tests"
if (Get-Command python -ErrorAction SilentlyContinue) {
    Push-Location "$ScriptDir\django"
    python manage.py test TumaGo_Server.tests --settings=TumaGo.test_settings -v 2 2>&1
    if ($LASTEXITCODE -eq 0) { Mark-Pass } else { Mark-Fail }
    Pop-Location
} else {
    Mark-Skip "python not found"
}

# ── 2. Go Gateway Tests ────────────────────────────────────────────────────

Run-Section "Go Gateway Tests"
if (Get-Command go -ErrorAction SilentlyContinue) {
    Push-Location "$ScriptDir\go_services\gateway"
    go test -v ./... 2>&1
    if ($LASTEXITCODE -eq 0) { Mark-Pass } else { Mark-Fail }
    Pop-Location
} else {
    Mark-Skip "go not found"
}

# ── 3. Go Location Service Tests ───────────────────────────────────────────

Run-Section "Go Location Service Tests"
if (Get-Command go -ErrorAction SilentlyContinue) {
    Push-Location "$ScriptDir\go_services\location"
    go test -v ./... 2>&1
    if ($LASTEXITCODE -eq 0) { Mark-Pass } else { Mark-Fail }
    Pop-Location
} else {
    Mark-Skip "go not found"
}

# ── 4. Go Matching Service Tests ───────────────────────────────────────────

Run-Section "Go Matching Service Tests"
if (Get-Command go -ErrorAction SilentlyContinue) {
    Push-Location "$ScriptDir\go_services\matching"
    go test -v ./... 2>&1
    if ($LASTEXITCODE -eq 0) { Mark-Pass } else { Mark-Fail }
    Pop-Location
} else {
    Mark-Skip "go not found"
}

# ── 5. Notification Service Tests ──────────────────────────────────────────

Run-Section "Notification Service Tests"
if (Get-Command python -ErrorAction SilentlyContinue) {
    $depsCheck = python -c "import pytest; import fastapi" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Push-Location "$ScriptDir\notification"
        python -m pytest test_main.py -v 2>&1
        if ($LASTEXITCODE -eq 0) { Mark-Pass } else { Mark-Fail }
        Pop-Location
    } else {
        Mark-Skip "requires: pip install pytest httpx fastapi firebase-admin"
    }
} else {
    Mark-Skip "python not found"
}

# ── 6. Functional Tests (only with -All flag) ─────────────────────────────

if ($All) {
    Run-Section "Functional Tests (requires Docker services running)"
    if (Get-Command python -ErrorAction SilentlyContinue) {
        $reqCheck = python -c "import requests" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Push-Location $ScriptDir
            python -m pytest tests\functional_tests.py -v 2>&1
            if ($LASTEXITCODE -eq 0) { Mark-Pass } else { Mark-Fail }
            Pop-Location
        } else {
            Mark-Skip "requests not installed (run: pip install requests)"
        }
    } else {
        Mark-Skip "python not found"
    }
} else {
    Write-Host ""
    Write-Host "  Skipping functional tests (pass -All to include them)" -ForegroundColor Yellow
    $Skip++
}

# ── Summary ─────────────────────────────────────────────────────────────────

Write-Host ""
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  RESULTS" -ForegroundColor Cyan
Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host "  Passed:  $Pass" -ForegroundColor Green
Write-Host "  Failed:  $Fail" -ForegroundColor Red
Write-Host "  Skipped: $Skip" -ForegroundColor Yellow
Write-Host ""

if ($Fail -gt 0) {
    Write-Host "Some test suites failed." -ForegroundColor Red
    exit 1
} else {
    Write-Host "All test suites passed." -ForegroundColor Green
    exit 0
}
