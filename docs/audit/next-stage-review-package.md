# Next-Stage Reviewer Package

## Branch and scope

**Branch:** `manus/smscfixer-remediation`  
**Head at package preparation:** `85d4a5a`  
**Base:** `main` at `6e3de4124aa8ac3ac00cf98675d3b0d0d0fbcc43`

This branch contains the original remediation program plus the next-stage assurance work. It has not been pushed, submitted as a pull request, signed, released, or deployed.

## Change map

| Commit range | Review purpose |
|---|---|
| `a618d28` through `4c02019` | Safety controls, architecture extraction, cache bounds, settings hardening, test matrix, release-candidate integrity, operations reconciliation, and remediation record. |
| `037a85e` | AndroidX and instrumentation-test scaffolding for manifest/configuration checks. |
| `be0c7c9` | LSPosed managed shared-preferences preference path with guarded legacy fallback and unreadable-config safe default. |
| `e72f14d` | One-device instrumentation runner plus LSPosed API 93+/legacy preference test cases. |
| `85d4a5a` | External signing handoff verifier and signed-candidate/rollback evidence worksheet. |

The branch changes 43 files relative to `main`, adding testable routing/configuration components, three focused JVM test classes, one instrumentation test class, release/validation scripts, and operational/security artifacts.

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
| Remote branch / PR | Not performed | Explicit publication authorization; PR must include risk, testing evidence, and rollback notes. |
| AGP 8.4 / compile SDK 36 warning | Open monitoring item | Evaluate plugin/Gradle upgrade in an isolated compatibility change. |

## Review outcome

The branch is ready for code review and controlled device validation. It is **not ready for production deployment** until the gated device, carrier, signing, and rollback evidence is complete. The final enhanced local assurance suite additionally passed YAML and shell validation, exact-version and secret-pattern scans, immutable-action checks, lint, JVM tests, debug/release assembly, instrumentation-test APK compilation, artifact checks, and Git diff checks.
