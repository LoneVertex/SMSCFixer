<!--
  Thank you for contributing to SMSC Guard!
  Please ensure your PR description includes the required sections below.
  The CI workflow automatically enforces that ## Risk, ## Testing Evidence, and ## Rollback Notes are present.
-->

## Description
<!-- Provide a clear, concise summary of the proposed changes, the rationale, and relevant issue links. -->

## Changes
<!-- Bullet-point list of the specific files, classes, or behaviors modified. -->
- 

## Risk
<!--
  Required for CI quality gates.
  Explain potential regression risks, telephony impact, backwards compatibility, or edge cases.
  If minimal, explain why (e.g. "Low risk: documentation/test-only change with no runtime telephony impact").
-->

## Testing Evidence
<!--
  Required for CI quality gates.
  Provide proof of local verification, such as test execution output, lint results, or APK verification.
  Example:
  - `./gradlew --no-daemon clean lint test assembleDebug` passed with 0 errors.
  - All 81 unit tests passed hermetically.
-->

## Rollback Notes
<!--
  Required for CI quality gates.
  Document rollback steps if this change introduces an unexpected defect in production.
  Example:
  - Revert this commit/PR cleanly.
  - Reinstall previous known-good release APK.
-->

## Checklist
- [ ] Code follows the project's formatting and architectural conventions.
- [ ] No private or personal data (PII) is introduced or logged.
- [ ] All unit tests pass cleanly (`./gradlew testDebugUnitTest`).
- [ ] Android Lint reports 0 errors (`./gradlew lint`).
- [ ] Conventional Commit messages are used.
