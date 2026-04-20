#!/usr/bin/env bash
set -euo pipefail

PACKAGE_NAME="com.smscfixer"
LOG_TAG="SmscFixer"
EXPECTED_SMSC="+20105996500"

echo "[1/5] checking adb availability"
command -v adb >/dev/null 2>&1

echo "[2/5] ensuring at least one device is connected"
DEVICE_COUNT="$(adb devices | awk 'NR>1 && $2=="device"{count++} END{print count+0}')"
if [[ "${DEVICE_COUNT}" -lt 1 ]]; then
  echo "No connected device found."
  exit 1
fi

echo "[3/5] verifying module APK is installed"
if ! adb shell pm list packages | grep -q "${PACKAGE_NAME}"; then
  echo "Package ${PACKAGE_NAME} is not installed."
  exit 1
fi

echo "[4/5] collecting recent logs"
LOGS="$(adb logcat -d -s Xposed | grep "${LOG_TAG}" || true)"
if [[ -z "${LOGS}" ]]; then
  echo "No ${LOG_TAG} logs found."
  exit 1
fi

echo "[5/5] verifying forced SMSC evidence in logs"
if ! echo "${LOGS}" | grep -q "${EXPECTED_SMSC}"; then
  echo "Expected SMSC ${EXPECTED_SMSC} not found in logs."
  exit 1
fi

echo "Smoke test passed."
