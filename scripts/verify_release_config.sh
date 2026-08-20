#!/usr/bin/env bash
set -euo pipefail

if [[ "$#" -ne 1 ]]; then
  echo "Usage: $0 <release-tag>" >&2
  exit 64
fi

RELEASE_TAG="$1"
if [[ ! "${RELEASE_TAG}" =~ ^v[0-9]+(\.[0-9]+){1,3}([-.][A-Za-z0-9.]+)?$ ]]; then
  echo "Release tag must use v<version> format: ${RELEASE_TAG}" >&2
  exit 65
fi

VERSION_NAME="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle | head -n 1)"
VERSION_CODE="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*\([0-9][0-9]*\).*/\1/p' app/build.gradle | head -n 1)"
if [[ -z "${VERSION_NAME}" || -z "${VERSION_CODE}" ]]; then
  echo "Unable to parse versionName/versionCode from app/build.gradle" >&2
  exit 66
fi
if [[ "${RELEASE_TAG}" != "v${VERSION_NAME}" ]]; then
  echo "Tag ${RELEASE_TAG} does not match versionName ${VERSION_NAME}" >&2
  exit 67
fi
if [[ "${VERSION_CODE}" -le 0 ]]; then
  echo "versionCode must be positive" >&2
  exit 68
fi

if grep -RIn --exclude-dir=.git --exclude='*.jar' --exclude='*.apk' \
    -E '^[[:space:]]*(RELEASE_KEYSTORE_BASE64|RELEASE_KEYSTORE_PASSWORD|RELEASE_KEY_ALIAS|RELEASE_KEY_PASSWORD)=.+$' .; then
  echo "A non-empty release signing secret was found in the repository." >&2
  exit 69
fi

echo "Release configuration verified: tag=${RELEASE_TAG} versionName=${VERSION_NAME} versionCode=${VERSION_CODE}"
