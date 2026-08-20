# Historical SMSCFixer Remediation Baseline

> This is a pre-rebrand baseline captured before the project became **SMSC Guard v2.0.0**. The historical branch and package terminology below are retained to preserve audit traceability; current implementation and review status are documented in the [remediation implementation record](remediation-implementation-record.md) and [pull request #4](https://github.com/LoneVertex/SMSCFixer/pull/4).

- Historical branch: `manus/smscfixer-remediation`
- Base commit: `6e3de4124aa8ac3ac00cf98675d3b0d0d0fbcc43`
- Recorded: `2026-08-13T04:29:34Z`
- Gradle wrapper: `gradle-8.6-bin.zip`
- Android Gradle Plugin: `8.4.0`
- Java runtime: `openjdk version "21.0.11" 2026-04-21`

## Validation availability at baseline

- Android SDK: unavailable in the sandbox during baseline capture.
- `javac`: unavailable in the sandbox during baseline capture.
- Rooted LSPosed device: unavailable in the sandbox during baseline capture.

## Baseline checks

- `git diff --check`: pending final validation at the time of capture.
- `bash -n scripts/smoke_test_prod_like.sh`: pending final validation at the time of capture.
- `./gradlew lint test assembleDebug assembleRelease`: blocked until the Android SDK was configured.

Subsequent Java 17 / Android SDK 36 validation, v2.0.0 identity migration, settings UI redesign, and review publication are recorded separately so this original baseline remains historically accurate.
