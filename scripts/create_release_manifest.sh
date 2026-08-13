#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 3 ]]; then
  echo "Usage: $0 <release-tag> <unsigned-apk-path> <output-manifest-path>" >&2
  exit 64
fi

RELEASE_TAG="$1"
APK_PATH="$2"
OUTPUT_PATH="$3"

if [[ ! "${RELEASE_TAG}" =~ ^v[0-9]+(\.[0-9]+){1,3}([-.][A-Za-z0-9.]+)?$ ]]; then
  echo "Release tag must use v<version> format: ${RELEASE_TAG}" >&2
  exit 65
fi
if [[ ! -f "${APK_PATH}" ]]; then
  echo "Unsigned release candidate not found: ${APK_PATH}" >&2
  exit 66
fi

VERSION_NAME="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle | head -n 1)"
VERSION_CODE="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*\([0-9][0-9]*\).*/\1/p' app/build.gradle | head -n 1)"
if [[ -z "${VERSION_NAME}" || -z "${VERSION_CODE}" ]]; then
  echo "Unable to read Android version metadata from app/build.gradle" >&2
  exit 67
fi
if [[ "${RELEASE_TAG}" != "v${VERSION_NAME}" ]]; then
  echo "Tag ${RELEASE_TAG} does not match versionName ${VERSION_NAME}" >&2
  exit 68
fi

COMMIT_SHA="$(git rev-parse HEAD)"
ARTIFACT_SHA256="$(sha256sum "${APK_PATH}" | awk '{print $1}')"
ARTIFACT_NAME="$(basename "${APK_PATH}")"
GENERATED_AT="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

cat > "${OUTPUT_PATH}" <<EOF
{
  "release_tag": "${RELEASE_TAG}",
  "version_name": "${VERSION_NAME}",
  "version_code": ${VERSION_CODE},
  "commit_sha": "${COMMIT_SHA}",
  "artifact_name": "${ARTIFACT_NAME}",
  "artifact_sha256": "${ARTIFACT_SHA256}",
  "artifact_status": "unsigned_release_candidate",
  "signing_owner": "external_controlled_signing_step",
  "generated_at_utc": "${GENERATED_AT}"
}
EOF

echo "Wrote unsigned release-candidate manifest: ${OUTPUT_PATH}"
