#!/usr/bin/env bash
set -euo pipefail

PACKAGE_NAME="${PACKAGE_NAME:-io.github.lonevertex.smscguard}"
LOG_TAG="${LOG_TAG:-SmscGuard}"
EXPECTED_DECISION="${EXPECTED_DECISION:-SLOT_PRIMARY}"
TEST_CASE_ID="${TEST_CASE_ID:-D-02}"
NON_INTERACTIVE="${NON_INTERACTIVE:-0}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-10}"

for arg in "$@"; do
  case "${arg}" in
    --non-interactive|--ci)
      NON_INTERACTIVE=1
      ;;
    --help|-h)
      echo "Usage: $0 [--non-interactive|--ci]"
      echo "Environment variables:"
      echo "  PACKAGE_NAME         Target package (default: io.github.lonevertex.smscguard)"
      echo "  LOG_TAG              Log tag (default: SmscGuard)"
      echo "  EXPECTED_DECISION    Expected decision enum (default: SLOT_PRIMARY)"
      echo "  TEST_CASE_ID         Test case ID from validation matrix (default: D-02)"
      echo "  NON_INTERACTIVE      Set to 1 to skip prompt for automated testing"
      echo "  MAX_WAIT_SECONDS     Log search polling timeout in seconds (default: 10)"
      exit 0
      ;;
  esac
done

case "${EXPECTED_DECISION}" in
  SLOT_PRIMARY|SLOT_SECONDARY|MCCMNC_FALLBACK|CARRIER_FALLBACK|UNKNOWN_ROUTING_SIGNALS|AMBIGUOUS_CARRIER_SIGNALS)
    ;;
  *)
    echo "Unsupported EXPECTED_DECISION: ${EXPECTED_DECISION}" >&2
    exit 64
    ;;
esac

echo "[1/6] Checking adb availability"
command -v adb >/dev/null 2>&1 || {
  echo "adb is required but not found in PATH." >&2
  exit 127
}

echo "[2/6] Ensuring exactly one target device is connected"
DEVICE_COUNT="$(adb devices | awk 'NR>1 && $2=="device"{count++} END{print count+0}')"
if [[ "${DEVICE_COUNT}" -ne 1 ]]; then
  echo "Expected exactly one connected device; found ${DEVICE_COUNT}." >&2
  exit 1
fi

echo "[3/6] Verifying module APK is installed"
if ! adb shell pm list packages | grep -q "${PACKAGE_NAME}"; then
  echo "Package ${PACKAGE_NAME} is not installed." >&2
  exit 1
fi

echo "[4/6] Clearing logcat to exclude stale results"
adb logcat -c

echo "[5/6] Operator action / Dispatch trigger"
if [[ "${NON_INTERACTIVE}" -eq 1 || "${CI:-false}" == "true" ]]; then
  echo "Non-interactive mode enabled: skipping manual prompt for ${TEST_CASE_ID}."
else
  printf 'Run %s: send one controlled test SMS for the intended SIM/path, then verify delivery through the approved test destination. Press Enter only after completion. ' "${TEST_CASE_ID}"
  read -r _
fi

echo "[6/6] Checking redacted decision evidence (timeout: ${MAX_WAIT_SECONDS}s)"
ELAPSED=0
LOGS=""
FOUND=0

while [[ "${ELAPSED}" -lt "${MAX_WAIT_SECONDS}" ]]; do
  # Query both SmscGuard tag and legacy Xposed tag for backward/forward compatibility
  LOGS="$(adb logcat -d -s "${LOG_TAG}" -s Xposed | grep "${LOG_TAG}" || true)"
  if [[ -z "${LOGS}" ]]; then
    LOGS="$(adb logcat -d | grep "${LOG_TAG}" || true)"
  fi

  if [[ -n "${LOGS}" ]]; then
    if [[ "${EXPECTED_DECISION}" == "UNKNOWN_ROUTING_SIGNALS" || "${EXPECTED_DECISION}" == "AMBIGUOUS_CARRIER_SIGNALS" ]]; then
      if printf '%s\n' "${LOGS}" | grep -Eq "event=replacement_preserved reason=${EXPECTED_DECISION}"; then
        FOUND=1
        break
      fi
    else
      if printf '%s\n' "${LOGS}" | grep -Eq "event=smsc_replaced reason=${EXPECTED_DECISION}"; then
        FOUND=1
        break
      fi
    fi
  fi

  sleep 1
  ELAPSED=$((ELAPSED + 1))
done

if [[ "${FOUND}" -ne 1 ]]; then
  if [[ -z "${LOGS}" ]]; then
    echo "No ${LOG_TAG} logs found within ${MAX_WAIT_SECONDS} seconds." >&2
  else
    echo "Expected decision ${EXPECTED_DECISION} was not found in ${LOG_TAG} logs." >&2
    echo "Recent logs:" >&2
    printf '%s\n' "${LOGS}" | tail -n 10 >&2
  fi
  exit 1
fi

printf 'Smoke test passed: case=%s decision=%s. Record delivery evidence outside logs without storing message contents or raw SMSC values.\n' \
  "${TEST_CASE_ID}" "${EXPECTED_DECISION}"
