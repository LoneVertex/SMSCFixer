# Release and Rollback Evidence

This worksheet is completed by the controlled signing owner and test operator. It does not authorize a release, deploy an APK, or send an SMS message by itself.

> **Repository status:** This is a reusable evidence template, so its operator-completed fields remain blank in source control. The SMSC Guard v2.0.0 implementation is merged into [`main`](https://github.com/LoneVertex/SMSCFixer/tree/main) through [pull request #4](https://github.com/LoneVertex/SMSCFixer/pull/4) and is available as a public pre-release. The production-target candidate remains unsigned. Populate and retain completed evidence in the approved secure release channel after controlled signing and device validation.

## Candidate identity

| Field | Record |
|---|---|
| Release tag | |
| Version name / code | |
| Source commit SHA | |
| Unsigned candidate filename | |
| Unsigned candidate SHA-256 | |
| Candidate manifest filename | |
| Candidate manifest verification operator / UTC time | |

## Controlled signing handoff

The signing owner must verify that the unsigned candidate SHA-256 matches the candidate manifest, then sign outside the repository and record the signed APK identity. Use `scripts/verify_signed_release_handoff.sh` to verify the signed artifact and print certificate details; retain its redacted output in the secured evidence channel.

| Field | Record |
|---|---|
| Signed APK filename | |
| Signed APK SHA-256 | |
| Signing certificate SHA-256 digest | |
| Signing verification operator / UTC time | |
| Secure evidence location | |

## Device installation and smoke cases

| Case | Device/profile | Artifact digest | Result | Redacted evidence location |
|---|---|---|---|---|
| Instrumentation test APK | | | | |
| D-01 hook scope | | | | |
| D-02 SIM1 primary | | | | |
| D-03 SIM2 secondary | | | | |
| D-06 unknown signals preserved | | | | |
| D-07 conflicting signals preserved | | | | |
| D-11 managed LSPosed preferences | | | | |
| D-12 legacy preference fallback, if supported | | | | |

## Rollback rehearsal

1. Record the known-good signed APK digest.
2. Install the candidate on the controlled device and run only approved validation cases.
3. Trigger the rollback procedure without sending unapproved messages: disable scope/module if necessary, reinstall known-good signed APK, reboot/restart, and execute the relevant smoke case.
4. Record the restored artifact digest, redacted event result, operator, and UTC timestamp.

| Field | Record |
|---|---|
| Known-good signed APK SHA-256 | |
| Candidate-to-known-good rollback result | |
| Post-rollback smoke case and decision event | |
| Operator / UTC time | |
| Incident or evidence link | |
