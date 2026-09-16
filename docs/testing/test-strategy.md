# Test Strategy

The module is divided into a pure policy/configuration layer, an Android reflection adapter layer, an LSPosed interceptor execution layer, and an Android system/service adapter layer. Pure and simulated behaviors belong in hermetic JVM tests; Android framework binding and physical telephony behavior require instrumentation or rooted-device evidence.

| Risk Area | Primary Automated Test Suite | Runtime / Device Validation |
|---|---|---|
| Invalid package configuration or injection attempts | `SmscGuardConfigTest`, `SmscConfigSecurityTest` | Verify package filtering on rooted device with test messaging apps. |
| Ambiguous, conflicting, or unknown routing signals | `SmscSelectorTest`, `SmscSelectorBoundaryTest` | Trigger unknown/conflicting signals where device permits controlled simulation. |
| Unsupported or altered method hooked | `HookSignatureRegistryTest`, `HookSignatureRegistryPlatformTest` | Introspect `android.telephony.SmsManager` class and confirm registered signatures in LSPosed logs. |
| Reflection adapter failure or subId/slot resolution failure | `RoutingSignalResolverTest`, `ReflectUtilsTest` | Profile repeated sends across subscription and SIM slot changes. |
| Cache returns stale or unbounded data | `BoundedTtlCacheTest` | Verify cache expiration and bounded eviction under load. |
| Interceptor replacement pipeline & idempotency | `SmscGuardHookInterceptorTest` | Verify that identical target SMSC is never replaced and invalid/ambiguous decisions fail closed. |
| Diagnostic logging leak or race condition | `DiagnosticLoggerConcurrencyTest` | Audit logs under high concurrency for zero PII exposure and bounded key limits. |
| UI state management & framework telemetry binding | `SettingsUiLogicTest` | Verify state transitions between standalone and bound LSPosed service. |
| Settings persistence, manifest declaration, and assets | `SettingsAndManifestInstrumentedTest` | Run instrumentation suite on emulator or physical Android 15/16 device. |
| Live carrier delivery and slot-specific routing | Controlled device smoke test (`scripts/smoke_test_prod_like.sh`) | Controlled staged SMS test using approved SIM and test destination. |

Every behavior-changing patch must add a regression test at the lowest layer that can prove it. The rooted-device cases in `validation-matrix.md` remain mandatory release evidence and cannot be substituted solely by a green unit-test run.
