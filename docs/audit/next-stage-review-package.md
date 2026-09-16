# Next-Stage Reviewer Package

> [!NOTE]
> **Archival Audit Baseline (PR #4):** This record describes the original remediation and next-stage assurance preparation merged into `main` via PR #4 (`edbe127`). The project has since completed the **Libxposed API 102 modernization** (merged via PR #5) and published the official cryptographically signed release **`v2.0.0`** (`smscguard-v2.0.0-release-signed.apk`). Legacy `XSharedPreferences` and world-readable modes have been superseded by modern Service IPC (`XposedService` / `RemotePreferences` / `XposedProvider`) detailed in [ADR-002](../security/configuration-sharing-decision.md).

## Branch and scope

**Historical preparation branch:** `manus/smscfixer-remediation`

**Head at package preparation:** `85d4a5a`

**Base:** `main` at `6e3de4124aa8ac3ac00cf98675d3b0d0d0fbcc43`

**Current source:** [`main`](https://github.com/LoneVertex/SMSCFixer/tree/main) at merged commit `edbe127fceaea63152592708b46a4cc2812887e7`

**Review history:** [pull request #4](https://github.com/LoneVertex/SMSCFixer/pull/4) is merged.

This record describes the original remediation and next-stage assurance preparation. That preparation was later extended with the SMSC Guard v2.0.0 identity migration and settings redesign, merged into main, and published as the public v2.0.0 pre-release. The production-target candidate has not been externally signed, stable deployment remains gated, and device/carrier evidence remains controlled.

## Change map

| Commit range | Review purpose |
|---|---|
| `a618d28` through `4c02019` | Safety controls, architecture extraction, cache bounds, settings hardening, test matrix, release-candidate integrity, operations reconciliation, and remediation record. |
| `037a85e` | AndroidX and instrumentation-test scaffolding for manifest/configuration checks. |
| `be0c7c9` | LSPosed managed shared-preferences preference path with guarded legacy fallback and unreadable-config safe default. |
| `e72f14d` | One-device instrumentation runner plus LSPosed API 93+/legacy preference test cases. |
| `85d4a5a` | External signing handoff verifier and signed-candidate/rollback evidence worksheet. |
| `b63101a` through `e85e643` | SMSC Guard v2.0.0 identity migration, package rename, adaptive icon, and release/migration records. |
| `14181a7` | Native settings UI redesign with retained behavioral and accessibility contracts. |

At the historical package-preparation head, the branch changed 43 files relative to `main`, adding testable routing/configuration components, three focused JVM test classes, one instrumentation test class, release/validation scripts, and operational/security artifacts. The current SMSC Guard review branch extends that snapshot with the v2.0.0 identity migration, settings UI redesign, and associated documentation.

## Completed repository-level validation

| Check | Status | Evidence |
|---|---|---|
| JVM unit tests | Pass | `./gradlew test` passed in the Java 17 / Android SDK 36 environment. |
| Lint and application APK assembly | Pass | `lint`, `assembleDebug`, and `assembleRelease` passed. |
| Instrumentation APK compilation | Pass | `assembleDebugAndroidTest` produced `app-debug-androidTest.apk`; execution requires a connected device. |
| Workflow/deployment YAML | Pass | CI, release, and deployment YAML passed lint validation. |
| Shell scripts | Pass | Smoke, release candidate, signing handoff, instrumentation runner, and configuration scripts passed `bash -n`. |
| Release candidate metadata | Pass | Tag/version validation and unsigned candidate manifest generation passed locally. |
| Safety regression scans | Pass | No source reintroduction of raw default SMSC output, broad directory permission changes, or mutable action pins was found. |
| Working tree | Clean | No uncommitted changes at review-package preparation. |
| CI instrumentation coverage | Pass | CI now compiles and verifies the debug instrumentation-test APK on every push and pull request. |
| Dependency/build review | Pass with monitored upgrade item | Production debug runtime has no resolved runtime dependencies; declared versions are exact. AGP 8.4/SDK 36 remains a documented compatibility warning pending an isolated AGP 9.0/Gradle 9.1 migration. |

## Required reviewer focus

Review `HookSignatureRegistry`, `SmscSelector`, `RoutingSignalResolver`, `ConfigurationRepository`, `SettingsActivity`, and `DiagnosticLogger` first. Confirm that exact signature matching is retained, unknown/conflicting signals preserve the original address, no empty scope becomes all-package scope, caches stay bounded, and diagnostics remain redacted by default.

Review the `xposedsharedprefs` manifest metadata and `MODE_WORLD_READABLE` request as a compatibility improvement, not an unconditional removal of legacy risk. The fallback remains intentionally guarded until actual LSPosed profiles verify managed storage and legacy behavior.

## Gated actions and residual risks

| Action or risk | Current status | Required gate |
|---|---|---|
| Android instrumentation execution | Not run | One connected device; run `scripts/run_instrumentation_validation.sh`. |
| Rooted LSPosed validation | Not run | Complete D-01 through D-12 for each supported profile. |
| Real carrier delivery | Not run | Approved test SIM/destination and separate explicit authorization before send. |
| Signed artifact | Not created | Controlled external signing owner must use manifest/checksum/certificate handoff. |
| Rollback rehearsal | Not run | Signed candidate and known-good signed artifact on controlled device. |
| Main branch / PR | Complete | The change set is merged into [`main`](https://github.com/LoneVertex/SMSCFixer/tree/main) through [pull request #4](https://github.com/LoneVertex/SMSCFixer/pull/4); the public `v2.0.0` pre-release contains the controlled-test artifacts and release-state boundaries. |
| AGP 8.4 / compile SDK 36 warning | Open monitoring item | Evaluate plugin/Gradle upgrade in an isolated compatibility change. |

## Review outcome

The SMSC Guard implementation is merged into [`main`](https://github.com/LoneVertex/SMSCFixer/tree/main) and available as the public `v2.0.0` pre-release. It is **not ready for stable production deployment** until the gated device, carrier, signing, and rollback evidence is complete. The final enhanced local assurance suite additionally passed YAML and shell validation, exact-version and secret-pattern scans, immutable-action checks, lint, JVM tests, debug/release assembly, instrumentation-test APK compilation, artifact checks, and Git diff checks.
