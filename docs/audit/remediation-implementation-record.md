# Remediation Implementation Record

## Scope and branch

The remediation program was implemented on branch `manus/smscfixer-remediation` from audited base commit `6e3de4124aa8ac3ac00cf98675d3b0d0d0fbcc43`. The changes are intentionally separated into reviewable commits. No pull request, remote branch push, release, deployment, signing action, or real-SMS operation was performed.

| Commit | Purpose |
|---|---|
| `a618d28` | Immediate hook-safety and target-scope controls. |
| `439357a` | Extract configuration, diagnostics, and routing-signal responsibilities. |
| `f5a1dd5` | Add bounded TTL routing metadata cache and tests. |
| `c8a1360` | Minimize preference exposure and improve settings safety/accessibility. |
| `5d2d124` | Add test strategy and rooted-device validation matrix. |
| `4ced569` | Pin CI actions and add unsigned-candidate release verification. |
| `e51f711` | Align operations, monitoring, threat model, smoke test, and rollback gates. |

## Delivered controls

| Audit risk | Delivered control | Verification |
|---|---|---|
| Invalid target configuration widened scope | Invalid-only configuration falls back to the validated default target set; empty runtime scope fails closed outside the mandatory `android` scope. | Expanded `SmscFixerConfigTest`; Gradle tests pass. |
| Heuristic send-method hooking | `HookSignatureRegistry` allowlists exact public SMSC-bearing signatures and rejects close/unknown variants. | `HookSignatureRegistryTest`; Gradle tests pass. |
| Unsafe fallback under unknown/conflicting signals | `SmscSelector` authorizes replacement only for confident signals and otherwise preserves the original SMSC. | Updated `SmscSelectorTest`; validation cases D-04 through D-07. |
| Raw/default log leakage | Centralized `DiagnosticLogger` emits redacted lifecycle events by default and gates detailed diagnostics. | Static safety-pattern scan found no `scAddress=` output in source/docs/scripts. |
| Repeated reflection and unbounded logging state | `RoutingSignalResolver` caches known subscription signals with a bounded TTL cache; diagnostic throttling is bounded. | `BoundedTtlCacheTest`; Gradle tests pass. |
| Broad preference directory exposure | Settings now changes readability only for the preferences XML file and avoids data-directory/prefs-directory traversal changes. | Source review; device compatibility remains required. |
| Unsigned release ambiguity and mutable workflow actions | Immutable action pins, tag/version checks, unsigned candidate manifest, digest generation, external controlled signing requirement. | Workflow YAML lint and local manifest-generation checks pass. |
| Weak operational evidence | Parameterized redacted smoke test, validation matrix, test strategy, updated monitoring, rollback, migration, and runbook documents. | Script syntax and YAML lint pass; rooted-device execution pending. |

## Automated validation evidence

The following command completed successfully after the full remediation set:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
./gradlew --no-daemon lint test assembleDebug assembleRelease
```

The run completed with `BUILD SUCCESSFUL` and produced both `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release-unsigned.apk`. Workflow and deployment YAML passed `yamllint`; release helper and smoke scripts passed `bash -n`; `./scripts/verify_release_config.sh v1.0.0` passed; and a local unsigned candidate manifest was generated successfully.

The only build warning retained is that Android Gradle Plugin 8.4.0 reports compile SDK 36 as newer than the plugin’s tested compile SDK range. The project nevertheless built, linted, and tested successfully in the configured Java 17 / Android SDK 36 environment. Treat AGP/compile-SDK compatibility as a monitored upgrade item rather than suppressing the warning without validation.

## Required follow-up before production rollout

The repository cannot prove LSPosed or carrier behavior in the sandbox. A production rollout remains blocked on the applicable rooted-device and controlled-carrier cases in `docs/testing/validation-matrix.md`, including configuration visibility through XSharedPreferences, exact supported API signature behavior, SIM1/SIM2 routing, unknown/conflicting-signal preservation, malformed preference recovery, reboot/restart behavior, controlled delivery, and a signed-artifact rollback rehearsal.

The configuration-sharing design is improved but still carries platform-specific residual risk because XSharedPreferences requires compatibility-oriented file readability. Do not claim least-privilege IPC completion until a supported provider/IPC solution is prototyped and validated on the target LSPosed/Android environments. The current code validates all values on read and avoids changing application or preferences directory traversal permissions.
