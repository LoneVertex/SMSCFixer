# Operations Runbook

## Release candidate and signing

The CI release workflow produces an **unsigned release candidate** together with a SHA-256 file and JSON manifest. It is not a production artifact. A controlled external signing step must verify the manifest’s tag, version, commit SHA, and candidate digest; sign the APK; verify the signing certificate and digest; and retain the resulting signed APK as the only deployable artifact.

## Pre-rollout gates

Run the Gradle quality gates, review the validation matrix, and confirm a supported rooted-device profile has passed the applicable D-01 through D-10 cases. Confirm that the release tag matches `versionName`, the signed artifact digest has been recorded, a previous known-good signed artifact is available, and the rollback rehearsal has been completed for the release family.

## Smoke validation

Scope LSPosed to `android` and the intended messaging package, reboot or restart scoped processes, and run the smoke test once per routing case. For example, use `TEST_CASE_ID=D-02 EXPECTED_DECISION=SLOT_PRIMARY ./scripts/smoke_test_prod_like.sh` for SIM1 and `TEST_CASE_ID=D-03 EXPECTED_DECISION=SLOT_SECONDARY ./scripts/smoke_test_prod_like.sh` for SIM2. The script validates redacted decision events, not raw SMSC values. The operator must separately confirm controlled-message delivery without recording message bodies, raw destination numbers, or raw SMSC values in logs.

## Staged rollout

Begin with the internal rooted-device cohort, then a small trusted cohort, and finally the approved full target population. Do not advance a stage until hook-install evidence, expected decision events, delivery evidence, crash review, and compatibility-registry updates are complete. Unknown or conflicting routing signals must show preservation of the original SMSC rather than a forced fallback.

## Rollback and incident triage

Stop the rollout immediately on routing mismatch, repeated hook-install failure, unexpected package scope, crash spike, or failed preservation behavior. Disable the affected LSPosed scope or the module, reinstall the last known-good **signed** APK, reboot, and rerun the relevant smoke case. Record the incident using the repository template with redacted device, scope, routing-decision, release-manifest, and rollback evidence.
