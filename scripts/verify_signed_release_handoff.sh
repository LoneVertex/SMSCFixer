#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 3 ]]; then
  echo "Usage: $0 <candidate-manifest.json> <unsigned-candidate.apk> <signed-release.apk>" >&2
  exit 64
fi

MANIFEST="$1"
UNSIGNED_APK="$2"
SIGNED_APK="$3"

for file in "${MANIFEST}" "${UNSIGNED_APK}" "${SIGNED_APK}"; do
  [[ -f "${file}" ]] || { echo "Required file not found: ${file}" >&2; exit 66; }
done
[[ "${UNSIGNED_APK}" != "${SIGNED_APK}" ]] || {
  echo "Signed release must be a distinct file from the unsigned candidate." >&2
  exit 67
}

EXPECTED_CANDIDATE_SHA="$(sed -n 's/^[[:space:]]*"artifact_sha256"[[:space:]]*:[[:space:]]*"\([0-9a-f][0-9a-f]*\)".*/\1/p' "${MANIFEST}" | head -n 1)"
EXPECTED_STATUS="$(sed -n 's/^[[:space:]]*"artifact_status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${MANIFEST}" | head -n 1)"
if [[ -z "${EXPECTED_CANDIDATE_SHA}" || "${EXPECTED_STATUS}" != "unsigned_release_candidate" ]]; then
  echo "Manifest is missing expected unsigned candidate identity." >&2
  exit 68
fi

ACTUAL_CANDIDATE_SHA="$(sha256sum "${UNSIGNED_APK}" | awk '{print $1}')"
if [[ "${ACTUAL_CANDIDATE_SHA}" != "${EXPECTED_CANDIDATE_SHA}" ]]; then
  echo "Unsigned candidate SHA-256 does not match the manifest." >&2
  exit 69
fi

APKSIGNER="${APKSIGNER:-apksigner}"
command -v "${APKSIGNER}" >/dev/null 2>&1 || {
  echo "apksigner is required to verify the signed release." >&2
  exit 127
}

"${APKSIGNER}" verify --verbose --print-certs "${SIGNED_APK}"
printf 'unsigned_candidate_sha256=%s\n' "${ACTUAL_CANDIDATE_SHA}"
printf 'signed_release_sha256=%s\n' "$(sha256sum "${SIGNED_APK}" | awk '{print $1}')"
printf 'verified_at_utc=%s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
