# Next-Stage Independent Review

## Review scope

This review examined the committed remediation branch relative to `main`, then assessed the subsequent LSPosed-managed preference compatibility change before device validation. The source review covered hook registration, argument mutation, routing decisions, configuration loading, cache bounds, diagnostics, settings permissions, tests, workflows, release scripts, and operational documents.

## Confirmed controls

| Review topic | Result | Evidence |
|---|---|---|
| Hook eligibility | Pass | `SmscGuard` iterates declared methods but hooks only when `HookSignatureRegistry.match` returns an exact allowlisted signature. No `startsWith("send")` eligibility heuristic remains. |
| SMSC mutation | Pass | Mutation occurs only after `replacementAuthorized` is true and a non-null candidate is returned. Unknown and conflicting signals return a preserve decision. |
| Target-package scope | Pass | Runtime rejects empty/null application scope outside mandatory `android`; normalization recovers invalid-only stored values to default targets. |
| Default logging | Pass | No runtime output logs old/new SMSC values, destination content, carrier value, device fingerprint, or full raw exception text by default. |
| Diagnostic bounds | Pass | Diagnostic keys are capped and throttled; routing cache has eight-entry and five-minute TTL limits. |
| File permissions | Improved | No application-data or preferences-directory permission broadening remains. A legacy single-file compatibility fallback exists only when LSPosed managed preferences are unavailable. |
| Workflows | Pass | Third-party actions are pinned to immutable commits; release candidates are explicitly unsigned and accompanied by tag/version/checksum manifest evidence. |
| Operations | Pass | Smoke, monitoring, rollback, migration, runbook, and validation matrix use redacted decision evidence and preserve-on-uncertainty semantics. |

## Configuration-sharing assessment

LSPosed documents a managed XSharedPreferences path for API 93+ modules with SDK >27. Enabling `xposedsharedprefs` allows module settings to request `MODE_WORLD_READABLE` through LSPosed’s managed preference storage; hooked processes continue to read by package and preference name rather than a hard-coded file path. The current candidate enables that metadata and requests the managed mode, falling back to a narrow single-file readability adjustment only when the manager rejects the mode. `ConfigurationRepository` now verifies `XSharedPreferences.getFile().canRead()` before consuming a snapshot and fails safely to defaults otherwise.

The candidate does not yet claim universal compatibility. The metadata path, manager behavior, file readability, reload after save, and configuration propagation must be tested on the actual LSPosed API 93+ target as well as any retained legacy manager profile. The current validation matrix explicitly covers malformed-scope recovery (D-09), post-restart configuration loading (D-10), managed LSPosed preferences (D-11), and the narrow legacy fallback (D-12).

## Findings requiring follow-up

| ID | Priority | Status | Required action |
|---|---|---|---|
| NS-01 | High | Device-dependent | Verify LSPosed managed preferences and the fallback across supported manager/API profiles; do not remove the compatibility fallback until the API 93+ path is demonstrated. |
| NS-02 | High | Device-dependent | Execute every validation-matrix rooted-device case before production readiness. |
| NS-03 | Medium | Partially addressed | The instrumentation suite now asserts the settings controls, labels, initial status, and polite live region. Execute it on-device and validate manager-specific persistence and fallback behavior under D-10 through D-12. |
| NS-04 | Medium | Open | Confirm cache behavior after SIM/subscription change; the current TTL bounds staleness but cannot detect change events proactively. |
| NS-05 | Medium | Monitoring item | Android Gradle Plugin 8.4 warns that compile SDK 36 exceeds its tested range. Evaluate a plugin/Gradle upgrade separately from telephony behavior changes. |

## Review conclusion

No repository-level regression was found that reintroduces broad heuristic hooking, silent primary fallback, all-package scope, raw default logging, broad directory traversal permissions, or mutable workflow references. The branch remains **not production-ready** until NS-01 and NS-02 are evidenced on supported rooted devices and controlled carrier validation is completed.

## References

[1]: https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences "LSPosed New XSharedPreferences guidance"
