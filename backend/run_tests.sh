#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# TumaGo — Run All Tests
# ─────────────────────────────────────────────────────────────────────────────
# Usage:
#   cd backend && bash run_tests.sh          # run unit tests only
#   cd backend && bash run_tests.sh --all    # run unit + functional tests
# ─────────────────────────────────────────────────────────────────────────────

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0
SKIP=0

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

run_section() {
    echo ""
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
}

pass() {
    echo -e "${GREEN}  PASSED${NC}"
    PASS=$((PASS + 1))
}

fail() {
    echo -e "${RED}  FAILED${NC}"
    FAIL=$((FAIL + 1))
}

skip() {
    echo -e "${YELLOW}  SKIPPED — $1${NC}"
    SKIP=$((SKIP + 1))
}

# ── 1. Django Unit Tests ────────────────────────────────────────────────────

run_section "Django Unit Tests"
if command -v python &>/dev/null; then
    cd "$SCRIPT_DIR/django"
    if python manage.py test TumaGo_Server.tests --settings=TumaGo.test_settings -v 2 2>&1; then
        pass
    else
        fail
    fi
    cd "$SCRIPT_DIR"
else
    skip "python not found"
fi

# ── 2. Go Gateway Tests ────────────────────────────────────────────────────

run_section "Go Gateway Tests"
if command -v go &>/dev/null; then
    cd "$SCRIPT_DIR/go_services/gateway"
    if go test -v ./... 2>&1; then
        pass
    else
        fail
    fi
    cd "$SCRIPT_DIR"
else
    skip "go not found"
fi

# ── 3. Go Location Service Tests ───────────────────────────────────────────

run_section "Go Location Service Tests"
if command -v go &>/dev/null; then
    cd "$SCRIPT_DIR/go_services/location"
    if go test -v ./... 2>&1; then
        pass
    else
        fail
    fi
    cd "$SCRIPT_DIR"
else
    skip "go not found"
fi

# ── 4. Go Matching Service Tests ───────────────────────────────────────────

run_section "Go Matching Service Tests"
if command -v go &>/dev/null; then
    cd "$SCRIPT_DIR/go_services/matching"
    if go test -v ./... 2>&1; then
        pass
    else
        fail
    fi
    cd "$SCRIPT_DIR"
else
    skip "go not found"
fi

# ── 5. Notification Service Tests ──────────────────────────────────────────

run_section "Notification Service Tests"
if command -v python &>/dev/null && python -c "import pytest" 2>/dev/null; then
    cd "$SCRIPT_DIR/notification"
    if python -m pytest test_main.py -v 2>&1; then
        pass
    else
        fail
    fi
    cd "$SCRIPT_DIR"
elif command -v python &>/dev/null; then
    skip "pytest not installed (run: pip install pytest httpx)"
else
    skip "python not found"
fi

# ── 6. Functional Tests (only with --all flag) ─────────────────────────────

if [[ "$1" == "--all" ]]; then
    run_section "Functional Tests (requires Docker services running)"
    if command -v python &>/dev/null && python -c "import requests" 2>/dev/null; then
        cd "$SCRIPT_DIR"
        if python -m pytest tests/functional_tests.py -v 2>&1; then
            pass
        else
            fail
        fi
    elif command -v python &>/dev/null; then
        skip "requests not installed (run: pip install requests)"
    else
        skip "python not found"
    fi
else
    echo ""
    echo -e "${YELLOW}  Skipping functional tests (pass --all to include them)${NC}"
    SKIP=$((SKIP + 1))
fi

# ── Summary ─────────────────────────────────────────────────────────────────

echo ""
echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}  RESULTS${NC}"
echo -e "${CYAN}══════════════════════════════════════════════════════════════${NC}"
echo -e "  ${GREEN}Passed:  $PASS${NC}"
echo -e "  ${RED}Failed:  $FAIL${NC}"
echo -e "  ${YELLOW}Skipped: $SKIP${NC}"
echo ""

if [ $FAIL -gt 0 ]; then
    echo -e "${RED}Some test suites failed.${NC}"
    exit 1
else
    echo -e "${GREEN}All test suites passed.${NC}"
    exit 0
fi
