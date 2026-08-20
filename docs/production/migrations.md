# Production Migration and Release Gate Policy

The project has no backend database migrations. A release still has a compatibility and artifact-identity migration gate because it changes LSPosed hook behavior across Android and vendor telephony APIs.

For each release, the maintainer must ensure that `versionCode` is incremented, `versionName` matches the `v<versionName>` release tag, and the release workflow has produced a passing unsigned candidate manifest. The external signing owner must verify that manifest against the candidate APK before signing, then record the signed APK SHA-256 and signing-certificate digest.

Before advancing a rollout stage, confirm Android manifest/module metadata, exact supported hook signatures, the applicable rooted-device validation-matrix cases, a controlled carrier delivery test, and a rollback rehearsal using the preceding signed artifact. A release that lacks this evidence remains a candidate and must not be distributed as production-ready.
