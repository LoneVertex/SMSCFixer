# SMSCFixer remediation baseline

- Branch: manus/smscfixer-remediation
- Base commit: 6e3de4124aa8ac3ac00cf98675d3b0d0d0fbcc43
- Recorded: 2026-08-13T04:29:34Z
- Gradle wrapper: gradle-8.6-bin.zip
- Android Gradle Plugin: 8.4.0
- Java runtime: openjdk version "21.0.11" 2026-04-21

## Validation availability

- Android SDK: unavailable in sandbox during baseline.
- javac: unavailable in sandbox during baseline.
- Rooted LSPosed device: unavailable in sandbox.

## Baseline checks

- `git diff --check`: pending final validation.
- `bash -n scripts/smoke_test_prod_like.sh`: pending final validation.
- `./gradlew lint test assembleDebug assembleRelease`: blocked until Android SDK is configured.
