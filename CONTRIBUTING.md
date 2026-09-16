# Contributing to SMSC Guard

Thank you for your interest in contributing to **SMSC Guard**! As an Android system-level LSPosed module dealing with telephony framework operations, our project adheres to strict standards of code safety, deterministic testing, and privacy protection.

Please take a moment to review this guide before submitting issues or code.

---

## Code of Conduct

This project and everyone participating in it is governed by the [SMSC Guard Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to [minaalaa141@gmail.com](mailto:minaalaa141@gmail.com).

---

## Architecture & Design Principles

Before writing code, familiarize yourself with our core architectural tenets:

1. **Zero PII Protection:** We never read, log, or persist SMS message bodies, recipient numbers, or subscriber identifiers (IMSI/IMEI). All diagnostic logging must be redacted and throttled.
2. **Fail-Safe Preserving Logic:** If carrier evidence or slot indicators are conflicting, absent, or ambiguous, the module **must preserve the original SMSC**. It must never crash, throw unhandled exceptions, or guess an arbitrary carrier.
3. **Libxposed API 102 Standard:** All hooks use modern `io.github.libxposed:api:102.0.0` and `service:102.0.0`. Legacy `de.robv.android.xposed` APIs (`XposedHelpers`, `XposedBridge`, `XSharedPreferences`) and world-readable filesystem modes (`Context.MODE_WORLD_READABLE`) are strictly prohibited.
4. **Hermetic Testing:** Every code change affecting reflection, schema validation, hook interceptors, or carrier selection must be accompanied by hermetic unit tests.

---

## Development Environment Setup

### Requirements
- **JDK:** OpenJDK 17 or Eclipse Temurin 17 (`java -version` should report 17)
- **Android SDK:** Platform API 36 with Build-Tools 35.0.0+
- **Gradle:** 8.6 (use the included `./gradlew` wrapper)
- **OS:** Linux, macOS, or Windows (WSL recommended)

### Getting the Code
```bash
# Clone the repository
git clone git@github.com:LoneVertex/SMSCFixer.git
cd SMSCFixer

# Verify the environment and build
./gradlew --no-daemon clean lint test assembleDebug
```

---

## Git Workflow & Commit Guidelines

### Branching Convention
- Create a focused topic branch from `main`:
  - `feat/feature-name`
  - `fix/bug-description`
  - `docs/documentation-update`
  - `refactor/subsystem-name`

### Conventional Commits
We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:
- `feat:` A new feature or capability
- `fix:` A bug fix
- `docs:` Documentation changes only
- `test:` Adding or refactoring tests
- `refactor:` Code change that neither fixes a bug nor adds a feature
- `perf:` A code change that improves performance
- `chore:` Changes to build process, dependencies, or auxiliary tools
- `ci:` Changes to CI configuration files and scripts

*Example:* `feat: add support for carrier MCC 602 MNC 04 preset`

### Authorship Rules
- All commits must use your verified identity:
  ```bash
  git config user.name "Your Name"
  git config user.email "your.email@example.com"
  ```
- Do not include AI attribution, watermarks, or co-author trailers in commit messages.

---

## Pull Request Requirements (Mandatory)

Our GitHub Actions CI pipeline enforces high quality gates. In particular, `.github/workflows/ci.yml` strictly checks for three mandatory markdown sections in every pull request description:

1. `## Risk`: An honest assessment of potential regression risks, telephony impact, or device compatibility implications.
2. `## Testing Evidence`: Concrete output demonstrating that tests and lint passed locally (e.g. `80/80 unit tests passing`).
3. `## Rollback Notes`: Clear instructions explaining how to revert the change or recover if an issue is discovered.

*PRs missing any of these three sections will automatically fail CI.* We provide `.github/PULL_REQUEST_TEMPLATE.md` to help you fill them out easily.

---

## Quality Gates & Verification

Before submitting your pull request, ensure the entire repository validation gate passes locally:

```bash
# 1. Run Android Lint
./gradlew --no-daemon lint

# 2. Run all unit tests
./gradlew --no-daemon test

# 3. Assemble all APK artifacts
./gradlew --no-daemon assembleDebug assembleRelease assembleDebugAndroidTest
```

### Adding New Tests
- Unit tests live under `app/src/test/java/io/github/lonevertex/smscguard/`.
- Use the established patterns in `SmscGuardHookInterceptorTest.kt`, `SmscSelectorBoundaryTest.java`, and `ReflectUtilsTest.java`.
- Tests must be deterministic and fast (< 10 seconds total).

---

## Proposing New Carrier Presets

If you want to contribute a new default carrier preset:
1. Ensure the SMSC address is the official, publicly documented international Service Center Address for that carrier (e.g., in strict E.164 format `+<country><number>`).
2. Provide the MCC (Mobile Country Code) and MNC (Mobile Network Code).
3. Add a test case in `SmscSelectorBoundaryTest.java` verifying that the carrier values are normalized and correctly routed.

---

## Questions or Assistance?

- Open an issue using our [GitHub Issue Templates](https://github.com/LoneVertex/SMSCFixer/issues/new/choose).
- Reach out to the maintainer via [minaalaa141@gmail.com](mailto:minaalaa141@gmail.com).
