# AI Project Engineering Pack v1.1 — SMSC Guard Issue Registry

## Registry purpose

This registry applies the supplied **AI Project Engineering Pack Ultra v1.1** to SMSC Guard without duplicating the earlier deep audit or treating every pack domain as applicable. It is the canonical correlation point for pack findings, existing remediation evidence, remaining validation gaps, and any follow-up change set.

The registry is based on the current local branch `manus/smsc-guard-v2-release-candidate` at commit `1aafc9c7b671e5af2b11be44ea3e5c814fe4d008`, with pull request #4 open against `main`. Source discovery found a small Java/Android LSPosed module with one settings Activity, pure routing/configuration components, JVM tests, one instrumentation test class, two GitHub workflows, release scripts, and no database, server, or network service.

## Evidence labels

| Label | Meaning |
|---|---|
| **VERIFIED** | Directly observed in source, workflow, artifact, or completed automated validation. |
| **INFERRED** | Strongly supported by implementation evidence but not directly exercised in the target runtime. |
| **ASSUMED** | Required working assumption that still needs confirmation. |
| **UNKNOWN** | Evidence is insufficient; do not claim the behavior. |

## Applicability matrix

| Pack domain | Applicability | Current conclusion | Evidence boundary |
|---|---|---|---|
| Project discovery | Applicable | **VERIFIED** | Android Gradle project, Java 17, SDK 36, min SDK 21, LSPosed entry point, settings UI, tests, workflows, scripts, and operational records were inspected. |
| Code quality and cleanup | Applicable | **VERIFIED / P2 follow-up** | Remediation extracted configuration, diagnostics, routing, cache, and signature responsibilities; remaining work is evidence and small cleanup only. |
| Performance | Applicable | **VERIFIED / UNKNOWN** | Bounded TTL cache and bounded diagnostic throttling are present; no production profiler or device benchmark is available. |
| Security | Applicable | **VERIFIED / P1 device gap** | Exact hook allowlist, scope validation, fail-safe routing, redacted diagnostics, and narrow preference fallback are implemented; target-device preference and hook behavior remain unverified. |
| Architecture | Applicable | **VERIFIED** | Thin LSPosed orchestration plus pure policy/configuration components is proportionate to the module; no rewrite is justified. |
| Database/data layer | Not applicable, with preferences substitute | **VERIFIED** | No database, ORM, SQL, server data store, or migration engine exists. Android preferences and bounded in-memory cache are the relevant state layers. |
| UI/UX | Applicable | **PARTIALLY VALIDATED** | Native dark-tech settings UI, retained IDs, labels, status region, and instrumentation assertions are present; physical-device accessibility and lifecycle execution remain pending. |
| Testing/QA | Applicable | **PARTIALLY VALIDATED** | JVM and build/instrumentation compilation coverage is present; rooted LSPosed, dual-SIM, carrier, rollback, and physical accessibility execution remain pending. |
| Reliability/edge cases | Applicable | **VERIFIED / UNKNOWN** | Null, malformed, empty, conflicting, and unsupported paths have policy/tests; device/vendor interruption and carrier behavior remain unknown. |
| Observability | Applicable but intentionally small | **VERIFIED** | Opt-in redacted Xposed events and throttled failures exist; server metrics/tracing are not appropriate to this offline module. |
| Dependencies/supply chain | Applicable | **VERIFIED / P2 monitor** | Dependency surface is minimal; immutable action pins and wrapper validation are present; AGP 8.4 compile SDK 36 warning remains monitored. |
| Configuration/environments | Applicable | **PARTIALLY VALIDATED** | Development/CI/release configuration is inspectable; managed LSPosed and controlled-device environments remain external. |
| CI/CD and DevOps | Applicable | **VERIFIED** | Current CI checks pass on the review head; test-tag classification prevents pre-merge tags from entering production-candidate validation. |
| Production readiness | Applicable | **YES — WITH CONDITIONS** | Automated repository gates pass, but signing, rooted-device compatibility, carrier delivery, and rollback evidence are not complete. |
| Documentation | Applicable | **IN PROGRESS** | Existing records are extensive and current; this registry and the final pack record complete the pack-specific correlation. |

## Canonical findings

| ID | Domain | Severity | Finding and evidence | Root cause / impact | Recommended remediation | Validation | Status |
|---|---|---:|---|---|---|---|---|
| PACK-001 | Discovery | P2 | The prior audit and remediation records were comprehensive but not expressed as one pack-specific canonical registry. | Cross-domain traceability could drift as the v2 branch and PR evolved. | Maintain this registry and link new change sets to stable IDs. | Review registry against pack manifest and current source. | **VERIFIED** |
| PACK-002 | Architecture | P3 | The current architecture has a thin `SmscGuard` orchestration layer with pure `SmscSelector`, schema/configuration, cache, logger, and signal resolver components. | Further abstraction would add complexity without a demonstrated defect. | Keep the current boundary; do not rewrite. | JVM tests, source review, dependency direction check. | **VERIFIED** |
| PACK-003 | Security | P1 | `HookSignatureRegistry` is exact-signature based; `SmscGuard` returns without changing arguments when the signature or routing evidence is unsupported. | Runtime safety depends on vendor/API compatibility that cannot be established in the sandbox. | Validate supported signatures and no-hook behavior on target rooted profiles. | D-01, D-02, D-03, D-06, D-07, D-08 with redacted evidence. | **BLOCKED** |
| PACK-004 | Security / configuration | P1 | `ConfigurationRepository` and `SettingsActivity` support managed preferences plus a narrow single-file fallback, with schema normalization and path safety. | XSharedPreferences readability and manager behavior are platform/LSPosed dependent. | Execute D-11 and D-12 on supported LSPosed/API profiles; do not broaden permissions. | D-11, D-12, source path-safety review. | **BLOCKED** |
| PACK-005 | Reliability / routing | P1 | `SmscSelector` preserves the original SMSC for null config, unknown signals, and conflicting MCC/MNC/carrier mappings. | Real framework/vendor signal resolution, SIM state, and process lifecycle can differ from JVM assumptions. | Validate SIM1/SIM2, unknown slot, conflicting mappings, restart, and reboot behavior. | D-02 through D-07 and D-10. | **BLOCKED** |
| PACK-006 | Operations / release | P1 | Carrier delivery and signed-artifact rollback cannot be proven by repository automation. | The sandbox has no approved test SIM, destination, production signer, or controlled rooted device. | Perform controlled delivery and rollback rehearsal under explicit human authorization. | Validation matrix carrier and rollback rows; signed handoff evidence. | **BLOCKED** |
| PACK-007 | Performance | P2 | Routing metadata uses a bounded TTL cache and diagnostics use bounded throttling; no device profiler or hot-path benchmark exists. | Performance claims beyond source-level bounds require a target runtime. | Add targeted benchmark/profile only if device evidence indicates a problem. | Cache tests, optional rooted-device trace, before/after measurement. | **PARTIALLY VALIDATED** |
| PACK-008 | Observability | P2 | Diagnostics are opt-in, event-oriented, redacted, and throttled; there is no central telemetry pipeline. | An LSPosed module should avoid network telemetry and sensitive logging, but field diagnosis is necessarily limited. | Keep redacted event vocabulary and document evidence collection outside logs. | Static sensitive-log scan, D-case log review, incident template. | **VERIFIED** |
| PACK-009 | Dependencies / supply chain | P2 | The app uses a small dependency surface: compile-only/test Xposed API, JUnit, AndroidX test runner/extensions; CI actions are immutable-pinned. | AGP 8.4 reports compile SDK 36 as newer than its tested range; blindly upgrading may introduce compatibility risk. | Monitor AGP/SDK compatibility and update only with a dedicated compatibility change set. | Gradle dependency reports, wrapper validation, CI build, compatibility research. | **ACCEPTED / MONITORED** |
| PACK-010 | CI/CD | P1 | Release workflow previously attempted production-candidate validation for the pre-merge tag `v2.0.0-pr4-test.1`. | One workflow matched all `v*` tags while the release verifier requires exact `v2.0.0`. | Classify tags first and run production candidate work only for the exact production tag. | `.github/workflows/release.yml`; current PR CI success at `1aafc9c`. | **VERIFIED** |
| PACK-011 | UI/UX and accessibility | P2 | Settings redesign retains view IDs, labels, live status, validation, and action contracts; instrumentation compiles and asserts the surface. | Physical TalkBack, keyboard, focus, lifecycle, and API variation remain unexecuted. | Run the settings and accessibility portion of the controlled-device matrix. | Instrumentation test plus physical-device evidence. | **PARTIALLY VALIDATED** |
| PACK-012 | Testing/QA | P1 | Critical tests exist for configuration, signatures, routing, cache, settings/manifest surface, and build artifacts. | Instrumentation is compiled but no connected Android/LSPosed target is available in this environment. | Do not claim E2E or telephony compatibility until device evidence exists. | Full Gradle gate, D-01 through D-12, secured redacted records. | **PARTIALLY VALIDATED** |
| PACK-013 | Documentation | P2 | README, audit, migration, release, runbook, validation matrix, rollback, brand, and security records are present and recently reconciled. | The pack-specific issue registry and final review record were missing. | Add this registry and final application report; keep historical records labeled. | Documentation link/legacy scan and reviewer inspection. | **VERIFIED** |
| PACK-014 | Not applicable domains | P3 | No database, ORM, API server, authentication system, cloud runtime, queue, or network client is present. | Applying those prompts mechanically would produce speculative findings and unnecessary architecture. | Preserve explicit non-applicability decisions in the final report. | Source/dependency scan and repository map. | **VERIFIED** |

## Change-set authorization summary

| Change set | Scope | Decision |
|---|---|---|
| A | Canonical registry and pack-specific final report | Authorized; documentation-only and reversible. |
| B | Targeted automated test additions | Not currently justified by verified source gap; revisit only if the re-audit finds missing assertions. |
| C | Runtime routing/configuration refactor | Not authorized; existing controls are materially aligned with the pack and further changes require a demonstrated defect. |
| D | CI/release workflow | Existing tag-classification correction is already implemented and validated; no additional change currently justified. |
| E | Controlled-device operations | Blocked pending explicit human-controlled device and SMS authorization. |

## Current verdict

**Production readiness: YES — WITH CONDITIONS.** Repository-level source, test, build, workflow, and documentation evidence is strong. The verdict is conditional because rooted LSPosed compatibility, managed preference visibility, dual-SIM behavior, approved carrier delivery, external signing, and rollback rehearsal remain outside the available automated evidence.

## Next registry update

After any approved change set or controlled-device test, update the affected finding’s evidence, confidence, validation result, and status. Do not mark a device or release gate **VERIFIED** from source inspection alone.
