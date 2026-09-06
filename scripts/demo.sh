#!/usr/bin/env bash
# =============================================================================
# Kafsys — End-to-end smoke test.
#
# Exercises the full SAGA: authenticate → look up seeded accounts → transfer →
# poll for terminal status → list alerts. Prints a one-line PASS/FAIL banner.
#
# Prerequisites:
#   • Kafsys is running locally (docker compose up  OR  mvn spring-boot:run)
#   • jq and curl are on PATH
#   • Optional: GATEWAY overrides the base URL (default http://localhost:8080)
# =============================================================================
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
USERNAME="${DEMO_USER:-admin}"
PASSWORD="${DEMO_PASS:-Admin@Kafsys1}"
AMOUNT="${DEMO_AMOUNT:-125.75}"
CURRENCY="${DEMO_CURRENCY:-USD}"
IDEMPOTENCY_KEY="demo-$(date +%s)-$RANDOM"
POLL_ATTEMPTS=15
POLL_INTERVAL=2

# --- helpers ---------------------------------------------------------------
red()   { printf "\033[31m%s\033[0m\n" "$1"; }
green() { printf "\033[32m%s\033[0m\n" "$1"; }
blue()  { printf "\033[34m%s\033[0m\n" "$1"; }

die() { red "ERROR: $1"; exit 1; }

require() {
  command -v "$1" >/dev/null 2>&1 || die "$1 is required but not installed"
}

require jq
require curl

# --- 1. login --------------------------------------------------------------
blue "▶ Authenticating as ${USERNAME}"
LOGIN_RESPONSE=$(curl -s -X POST "${GATEWAY}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")
TOKEN=$(echo "${LOGIN_RESPONSE}" | jq -r '.data.accessToken // empty')
[ -n "${TOKEN}" ] || die "login failed: ${LOGIN_RESPONSE}"
green "  ✓ authenticated"

AUTH=(-H "Authorization: Bearer ${TOKEN}" -H "X-Correlation-Id: ${IDEMPOTENCY_KEY}")

# --- 2. discover seeded accounts ------------------------------------------
blue "▶ Fetching seeded accounts"
ACCOUNTS_JSON=$(curl -s "${AUTH[@]}" "${GATEWAY}/api/v1/accounts?page=0&size=10")
SOURCE=$(echo "${ACCOUNTS_JSON}" | jq -r '.data.content[0].id // empty')
DEST=$(echo "${ACCOUNTS_JSON}" | jq -r '.data.content[1].id // empty')

if [ -z "${SOURCE}" ] || [ -z "${DEST}" ]; then
  die "expected ≥2 seeded accounts. Got: $(echo "${ACCOUNTS_JSON}" | jq -c '.data.content | length')"
fi
green "  ✓ source=${SOURCE:0:8}…  dest=${DEST:0:8}…"

# --- 3. initiate transfer -------------------------------------------------
blue "▶ Initiating transfer (${AMOUNT} ${CURRENCY}, idempotencyKey=${IDEMPOTENCY_KEY})"
TRANSFER_RESPONSE=$(curl -s -X POST "${GATEWAY}/api/v1/transactions/transfer" \
  "${AUTH[@]}" -H "Content-Type: application/json" \
  -d "{
    \"idempotencyKey\": \"${IDEMPOTENCY_KEY}\",
    \"sourceAccountId\": \"${SOURCE}\",
    \"destinationAccountId\": \"${DEST}\",
    \"amount\": ${AMOUNT},
    \"currency\": \"${CURRENCY}\",
    \"referenceNote\": \"kafsys demo\"
  }")
TX_ID=$(echo "${TRANSFER_RESPONSE}" | jq -r '.data.id // empty')
[ -n "${TX_ID}" ] || die "transfer POST failed: ${TRANSFER_RESPONSE}"
green "  ✓ transfer initiated: ${TX_ID}"

# --- 4. poll for terminal status ------------------------------------------
blue "▶ Waiting for SAGA to complete"
TERMINAL=""
for i in $(seq 1 ${POLL_ATTEMPTS}); do
  STATUS=$(curl -s "${AUTH[@]}" "${GATEWAY}/api/v1/transactions/${TX_ID}" | jq -r '.data.status // empty')
  printf "  attempt %02d/%d: %s\n" "$i" "${POLL_ATTEMPTS}" "${STATUS:-<no data>}"
  case "${STATUS}" in
    COMPLETED|ROLLED_BACK|FAILED|ACCOUNT_REJECTED|PAYMENT_FAILED)
      TERMINAL="${STATUS}"
      break
      ;;
  esac
  sleep "${POLL_INTERVAL}"
done

[ -n "${TERMINAL}" ] || die "transaction did not reach terminal status within $((POLL_ATTEMPTS * POLL_INTERVAL))s"
green "  ✓ terminal status: ${TERMINAL}"

# --- 5. verify alerts fanned out ------------------------------------------
blue "▶ Checking alerts for source account"
ALERTS=$(curl -s "${AUTH[@]}" \
  "${GATEWAY}/api/v1/alerts?accountId=${SOURCE}&page=0&size=5" | jq -c '.data.content | length')
green "  ✓ ${ALERTS} alert(s) visible for source account"

# --- 6. summary -----------------------------------------------------------
echo
if [ "${TERMINAL}" = "COMPLETED" ]; then
  green "═══════════════════════════════════════════════"
  green "  ✅ DEMO PASSED — SAGA completed end-to-end"
  green "═══════════════════════════════════════════════"
  exit 0
else
  red   "═══════════════════════════════════════════════"
  red   "  ⚠️  DEMO ENDED — transaction status: ${TERMINAL}"
  red   "═══════════════════════════════════════════════"
  exit 1
fi
