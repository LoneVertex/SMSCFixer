# SmscFixer (LSPosed Module)

SmscFixer is an Android LSPosed module that applies a configured SMSC only to an explicit allowlist of compatible public `SmsManager` send-method signatures. It uses slot-first routing, then validated MCC/MNC or carrier fallbacks. If the routing signal is unknown or contradictory, it **preserves the original SMSC** rather than forcing a primary fallback.

The default configuration is intended for a controlled Egyptian dual-SIM setup:

| Routing evidence | Default SMSC |
|---|---|
| SIM1 / slot 0 / Vodafone Egypt | `+20105996500` |
| SIM2 / slot 1 / Orange Egypt | `+20122000020` |

The defaults are configurable in the settings activity. Invalid target-package configuration never expands the module to arbitrary applications; it recovers to the validated default target scope.

## Build

Use Java 17 and an Android SDK that provides platform API 36. The repository has been validated with the following quality gate:

```bash
./gradlew --no-daemon lint test assembleDebug assembleRelease
```

The output paths are `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release-unsigned.apk`. The release APK is an **unsigned candidate**, not a production artifact. The release workflow creates an unsigned candidate manifest and SHA-256; a controlled external signing step must verify, sign, and record the final deployable APK identity.

## Install and scope

Install a controlled debug/test APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. In LSPosed Manager, enable **SmscFixer**, scope it at least to `android` and the intended messaging package, then reboot or restart scoped processes. The default package targets are `com.google.android.apps.messaging` and `com.android.mms` when present.

> Do not infer successful routing from a green build alone. Hook compatibility and carrier delivery require the rooted-device and controlled delivery evidence defined in the validation matrix.

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
| Rooted-device and carrier validation cases | `docs/testing/validation-matrix.md` |
| Test-layer responsibilities | `docs/testing/test-strategy.md` |
| Operations and staged rollout | `docs/operations/runbook.md` |
| Monitoring and redacted evidence | `docs/production/monitoring-alerts.md` |
| Rollback requirements | `docs/production/rollback.md` |
| Threat model and logging policy | `docs/security/` |

## Troubleshooting

If an expected replacement does not occur, first verify the LSPosed scope and process restart, then run the matching validation-matrix case. A preserved-original decision under unknown or conflicting signals is a safety behavior, not a defect. Do not add broad heuristic hooks for vendor `send*` methods; add a documented exact signature to `HookSignatureRegistry` with unit and rooted-device evidence.
