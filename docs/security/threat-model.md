# Threat Model

## Assets

The protected assets are SMSC routing correctness by subscription/slot, the validated target-package scope, configuration integrity, diagnostic privacy, release-artifact identity, and the operator’s ability to restore a known-good signed release.

## Principal threats and mitigations

| Threat | Mitigation | Residual validation requirement |
|---|---|---|
| Vendor/API variance changes method argument meaning | Only an explicit public-API signature registry is eligible for hooks; unknown `send*` methods are ignored. | Rooted-device signature validation on every supported profile. |
| Slot/subscription/carrier inference is unavailable or contradictory | Unknown or conflicting signals preserve the original SMSC; no implicit forced-primary fallback occurs. | Dual-SIM and conflicting-signal cases D-04 through D-07. |
| Malformed configuration broadens hook scope | Runtime normalization falls back to the default package set; empty scope fails closed outside `android`. | Malformed preference case D-09. |
| Slow reflection harms the send path or retries failures | Routing signals use a bounded TTL cache; diagnostics are throttled and bounded. | Device profiling and subscription-change validation. |
| Logs reveal routing or user data | Default logs are redacted; diagnostics are opt-in and limited to stable metadata. | Review captured evidence against `logging-policy.md`. |
| Preference sharing exposes configuration too broadly | Only the specific preferences XML file is made readable for compatibility; application and preferences directories are not broadened. All data remains revalidated on read. | Test configuration visibility on supported Android/LSPosed versions. |
| Release candidate is mistaken for a production artifact | CI labels candidates unsigned, emits a manifest/digest, and requires controlled external signing/verification. | Release and rollback rehearsal with signed APK evidence. |

## Residual risks

Cross-process preference sharing remains platform-dependent because the module relies on XSharedPreferences compatibility. Vendor telephony implementation variance and real carrier delivery cannot be proven by repository tests. A supported profile is therefore defined by the evidence in the device validation matrix rather than by the presence of a generic hook alone.
