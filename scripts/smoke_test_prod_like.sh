#!/usr/bin/env bash
set -euo pipefail

PACKAGE_NAME="${PACKAGE_NAME:-io.github.lonevertex.smscguard}"
LOG_TAG="${LOG_TAG:-SmscGuard}"
EXPECTED_DECISION="${EXPECTED_DECISION:-SLOT_PRIMARY}"
TEST_CASE_ID="${TEST_CASE_ID:-D-02}"

case "${EXPECTED_DECISION}" in
  SLOT_PRIMARY|SLOT_SECONDARY|MCCMNC_FALLBACK|CARRIER_FALLBACK|UNKNOWN_ROUTING_SIGNALS|AMBIGUOUS_CARRIER_SIGNALS)
    ;;
  *)
    echo "Unsupported EXPECTED_DECISION: ${EXPECTED_DECISION}" >&2
    exit 64
    ;;
esac

echo "[1/6] Checking adb availability"
command -v adb >/dev/null 2>&1

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

echo "[5/6] Manual operator action required"
printf 'Run %s: send one controlled test SMS for the intended SIM/path, then verify delivery through the approved test destination. Press Enter only after completion. ' "${TEST_CASE_ID}"
read -r _

echo "[6/6] Checking redacted decision evidence"
LOGS="$(adb logcat -d -s Xposed | grep "${LOG_TAG}" || true)"
if [[ -z "${LOGS}" ]]; then
  echo "No ${LOG_TAG} logs found." >&2
  exit 1
fi

if [[ "${EXPECTED_DECISION}" == "UNKNOWN_ROUTING_SIGNALS" || "${EXPECTED_DECISION}" == "AMBIGUOUS_CARRIER_SIGNALS" ]]; then
  if ! printf '%s\n' "${LOGS}" | grep -Eq "event=replacement_preserved reason=${EXPECTED_DECISION}"; then
    echo "Expected preserved-original decision ${EXPECTED_DECISION} was not found." >&2
    exit 1
  fi
else
  if ! printf '%s\n' "${LOGS}" | grep -Eq "event=smsc_replaced reason=${EXPECTED_DECISION}"; then
    echo "Expected replacement decision ${EXPECTED_DECISION} was not found." >&2
    exit 1
  fi
fi

printf 'Smoke test passed: case=%s decision=%s. Record delivery evidence outside logs without storing message contents or raw SMSC values.\n' \
  "${TEST_CASE_ID}" "${EXPECTED_DECISION}"
