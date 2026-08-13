# Test Strategy

The module is divided into a pure policy/configuration layer and an Android/Xposed adapter layer. Pure behavior belongs in JVM tests; Android process and telephony behavior requires instrumentation or rooted-device evidence. This separation prevents the presence of unit tests from being mistaken for proof of hook compatibility.

| Risk | Primary automated test | Runtime validation |
|---|---|---|
| Invalid package configuration widens hook scope | `SmscFixerConfigTest` | Verify malformed XML on a rooted LSPosed device. |
| Unknown/conflicting routing changes SMSC | `SmscSelectorTest` | Trigger unknown/conflicting signals where the device permits controlled simulation. |
| Unsupported method is hooked | `HookSignatureRegistryTest` | Confirm registered signatures only in LSPosed logs. |
| Cache returns stale/unbounded data | `BoundedTtlCacheTest` | Profile repeated sends across subscription changes. |
| Settings persistence/accessibility | Android instrumentation test | Confirm with TalkBack and process restart. |
| XSharedPreferences compatibility | Not fully JVM-testable | Validate targeted Android/LSPosed versions and document residual risk. |
| Actual carrier delivery | Not suitable for CI | Controlled staged SMS test using approved SIM/destination. |

Every behavior-changing patch must add a regression test at the lowest layer that can prove it. The rooted-device cases in `validation-matrix.md` remain mandatory release evidence and cannot be substituted by a green unit-test run.
