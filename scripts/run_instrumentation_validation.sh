#!/usr/bin/env bash
set -euo pipefail

APP_APK="${APP_APK:-app/build/outputs/apk/debug/app-debug.apk}"
TEST_APK="${TEST_APK:-app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk}"
APP_PACKAGE="${APP_PACKAGE:-io.github.lonevertex.smscguard}"
TEST_RUNNER="${TEST_RUNNER:-io.github.lonevertex.smscguard.test/androidx.test.runner.AndroidJUnitRunner}"
EVIDENCE_FILE="${EVIDENCE_FILE:-/tmp/smscguard-instrumentation-evidence.txt}"

command -v adb >/dev/null 2>&1 || {
  echo "adb is required." >&2
  exit 127
}

DEVICE_COUNT="$(adb devices | awk 'NR>1 && $2=="device"{count++} END{print count+0}')"
if [[ "${DEVICE_COUNT}" -ne 1 ]]; then
  echo "Expected exactly one connected Android test device; found ${DEVICE_COUNT}." >&2
  exit 1
fi
if [[ ! -f "${APP_APK}" || ! -f "${TEST_APK}" ]]; then
  echo "Build both debug and debugAndroidTest APKs before running this script." >&2
  exit 1
fi

{
  echo "timestamp_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "android_release=$(adb shell getprop ro.build.version.release | tr -d '\r')"
  echo "android_api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "device_model=$(adb shell getprop ro.product.model | tr -d '\r')"
  echo "device_manufacturer=$(adb shell getprop ro.product.manufacturer | tr -d '\r')"
  echo "note=No SMS, carrier, SIM, subscriber, or device-fingerprint data is collected by this script."
} > "${EVIDENCE_FILE}"

adb install -r "${APP_APK}"
adb install -r "${TEST_APK}"
adb shell am instrument -w "${TEST_RUNNER}" | tee -a "${EVIDENCE_FILE}"

echo "Instrumentation validation complete. Review ${EVIDENCE_FILE} and then execute the rooted-device cases in docs/testing/validation-matrix.md."
