# SMSC Guard (LSPosed Module)

**SMSC Guard** is an Android LSPosed module that applies a configured SMSC only to an explicit allowlist of compatible public `SmsManager` send-method signatures. It uses slot-first routing, then validated MCC/MNC or carrier fallbacks. If the routing signal is unknown or contradictory, it **preserves the original SMSC** rather than forcing a primary fallback.

## v2.0.0 identity migration

Version **2.0.0** introduces the public SMSC Guard identity, a new launcher icon, and Android application ID **`io.github.lonevertex.smscguard`**. This is a **new Android application installation**, not an in-place update of the retired `com.smscfixer` package. Before installing, retain the previous known-good APK for rollback; disable or remove the old module, install and enable SMSC Guard in LSPosed, recreate its scope, re-enter configuration, then reboot or restart scoped processes.

The implementation is merged into [`main`](https://github.com/LoneVertex/SMSCFixer/tree/main) through [pull request #4](https://github.com/LoneVertex/SMSCFixer/pull/4). The [public v2.0.0 pre-release](https://github.com/LoneVertex/SMSCFixer/releases/tag/v2.0.0) is available for controlled testing; it is not a stable production release. The v2.0.0 release candidate remains unsigned.

The default configuration is intended for a controlled Egyptian dual-SIM setup:

| Routing evidence | Default SMSC |
|---|---|
| SIM1 / slot 0 / Vodafone Egypt | `+20105996500` |
| SIM2 / slot 1 / Orange Egypt | `+20122000020` |

The defaults are configurable in the settings activity. Invalid target-package configuration never expands the module to arbitrary applications; it recovers to the validated default target scope.

## Build

Use Java 17 and an Android SDK that provides platform API 36. The repository quality gate compiles the app, JVM tests, lint, release candidate, and Android instrumentation-test APK:

```bash
./gradlew --no-daemon lint test assembleDebug assembleRelease assembleDebugAndroidTest
```

The output paths are `app/build/outputs/apk/debug/app-debug.apk`, `app/build/outputs/apk/release/app-release-unsigned.apk`, and `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`. The release APK is an **unsigned candidate**, not a production artifact. The release workflow creates an unsigned candidate manifest and SHA-256; a controlled external signing step must verify, sign, and record the final deployable APK identity.

## Controlled device validation and scope

Use the debug APK only for local UI or instrumentation testing. For rooted LSPosed and routing validation, install the controlled **externally signed** candidate only after verifying its candidate manifest, SHA-256 digest, and signing evidence. In LSPosed Manager, enable **SMSC Guard**, scope it at least to `android` and the intended messaging package, then reboot or restart scoped processes. The default package targets are `com.google.android.apps.messaging` and `com.android.mms` when present.

> Do not infer successful routing from a green build alone. The package migration changes LSPosed identity and stored preferences, so hook compatibility and carrier delivery require the rooted-device and controlled delivery evidence defined in the validation matrix.

## Verification and diagnostics

Run the redacted smoke script separately for each expected routing case. For example:

```bash
TEST_CASE_ID=D-02 EXPECTED_DECISION=SLOT_PRIMARY ./scripts/smoke_test_prod_like.sh
TEST_CASE_ID=D-03 EXPECTED_DECISION=SLOT_SECONDARY ./scripts/smoke_test_prod_like.sh
```

The script validates events such as `smsc_replaced reason=SLOT_PRIMARY` or `replacement_preserved reason=AMBIGUOUS_CARRIER_SIGNALS`; it does not log raw SMSC values. Delivery must be confirmed through an approved test destination and recorded without message bodies, raw recipient numbers, or raw SMSC values.

Detailed diagnostics are opt-in from Settings and may be automatically enabled for documented compatibility investigation. Disable diagnostics after troubleshooting. Default logs are redacted and should not include message contents, prior/replacement SMSC values, carrier names, subscription identifiers, or device fingerprints.

## Production controls

| Control | Location |
|---|---|
| Release candidate and external signing policy | `.github/workflows/release.yml`, `deploy/production.config.yml` |
| Current implementation and public pre-release | [`main`](https://github.com/LoneVertex/SMSCFixer/tree/main), [pull request #4](https://github.com/LoneVertex/SMSCFixer/pull/4), [v2.0.0 pre-release](https://github.com/LoneVertex/SMSCFixer/releases/tag/v2.0.0) |
| v2.0.0 migration and rollback evidence | `docs/production/smsc-guard-v2-migration.md`, `docs/production/release-and-rollback-evidence.md` |
| Rooted-device and carrier validation cases | `docs/testing/validation-matrix.md` |
| Test-layer responsibilities | `docs/testing/test-strategy.md` |
| Operations and staged rollout | `docs/operations/runbook.md` |
| Monitoring and redacted evidence | `docs/production/monitoring-alerts.md` |
| Rollback requirements | `docs/production/rollback.md` |
| Threat model and logging policy | `docs/security/` |
| Engineering-pack issue registry and applicability decisions | `docs/audit/ai-engineering-pack-issue-registry.md` |

## Troubleshooting

If an expected replacement does not occur, first verify the SMSC Guard LSPosed scope and process restart, then run the matching validation-matrix case. A preserved-original decision under unknown or conflicting signals is a safety behavior, not a defect. Do not add broad heuristic hooks for vendor `send*` methods; add a documented exact signature to `HookSignatureRegistry` with unit and rooted-device evidence.
