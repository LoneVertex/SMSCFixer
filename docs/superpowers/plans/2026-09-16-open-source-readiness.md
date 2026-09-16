# Professional Open Source Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the SMSC Guard (`SMSCFixer`) repository into a tier-1, professional open-source project ready for public launch, complete with open-source licensing, CI/status badges, modernized technical documentation, community health files, issue/PR templates, and sanitized governance records.

**Architecture:** Establish standard GitHub community health protocols (MIT License, Contributor Covenant v2.1, Security Policy, Conventional PR template matching CI assertions). Overhaul user-facing and technical documentation (`README.md`, `CHANGELOG.md`, `CONTRIBUTING.md`, `docs/`) to reflect the latest `v2.0.0` signed production release, libxposed API 102 architecture, and Jetpack Compose Material 3 UI.

**Tech Stack:** Markdown, YAML, GitHub Actions, Kotlin, Android Platform API 36, LSPosed / Libxposed API 102.

**Spec:** Open Source Standard Community Health, GitHub Actions CI enforcement, Android Open Source Project conventions.

## Global Constraints
- **Git identity:** `LoneVertex <minaalaa141@gmail.com>`.
- **Absolute ban on AI attribution:** Never mention AI, LLMs, or co-authorship anywhere. All authorship is LoneVertex.
- **Commit standard:** Conventional Commits (`docs:`, `chore:`, `feat:`, `fix:`, `ci:`).
- **CI Contract Compliance:** `.github/workflows/ci.yml` strictly enforces that every pull request description contains `## Risk`, `## Testing Evidence`, and `## Rollback Notes`. The PR template must pre-populate these exact headers.
- **Verification Gate:** Zero lint errors, 100% passing tests (80/80 unit tests) on `./gradlew --no-daemon clean lint test assembleDebug assembleRelease assembleDebugAndroidTest`.

---

### Task 1: Legal & Governance Foundation (LICENSE, CODE_OF_CONDUCT.md, SECURITY.md)

**Files:**
- Create: `LICENSE`
- Create: `CODE_OF_CONDUCT.md`
- Create: `SECURITY.md`

**Interfaces:**
- Consumes: LoneVertex open source standards (MIT License, Security email `minaalaa141@gmail.com`).
- Produces: GitHub community health compliance files recognized by GitHub API.

- [ ] **Step 1: Create `LICENSE` with standard MIT License**
- [ ] **Step 2: Create `CODE_OF_CONDUCT.md` using Contributor Covenant v2.1**
- [ ] **Step 3: Create `SECURITY.md` defining supported versions (2.0.x), vulnerability reporting, and PII protection**
- [ ] **Step 4: Verify files exist and contain correct copyright and contact details**
- [ ] **Step 5: Commit `chore: add MIT license, code of conduct, and security policy`**

---

### Task 2: Issue & Pull Request Workflows (`.github/ISSUE_TEMPLATE/`, `PULL_REQUEST_TEMPLATE.md`)

**Files:**
- Create: `.github/ISSUE_TEMPLATE/bug_report.yml`
- Create: `.github/ISSUE_TEMPLATE/feature_request.yml`
- Create: `.github/ISSUE_TEMPLATE/device_compatibility.yml`
- Create: `.github/ISSUE_TEMPLATE/config.yml`
- Create: `.github/PULL_REQUEST_TEMPLATE.md`

**Interfaces:**
- Consumes: CI enforcement from `.github/workflows/ci.yml` (`## Risk`, `## Testing Evidence`, `## Rollback Notes`).
- Produces: GitHub form-based issue intake and CI-compliant PR checklist.

- [ ] **Step 1: Create `.github/ISSUE_TEMPLATE/bug_report.yml` with structured fields (Android version, LSPosed version, device model, carrier, log snippet)**
- [ ] **Step 2: Create `.github/ISSUE_TEMPLATE/feature_request.yml` for carrier presets and enhancements**
- [ ] **Step 3: Create `.github/ISSUE_TEMPLATE/device_compatibility.yml` for logging verified hardware/ROM profiles**
- [ ] **Step 4: Create `.github/ISSUE_TEMPLATE/config.yml` enabling discussions or reporting guidance**
- [ ] **Step 5: Create `.github/PULL_REQUEST_TEMPLATE.md` with mandatory headers `## Risk`, `## Testing Evidence`, and `## Rollback Notes`**
- [ ] **Step 6: Commit `ci: add GitHub issue forms and CI-compliant pull request template`**

---

### Task 3: Flagship Open Source README & Changelog

**Files:**
- Modify: `README.md`
- Create: `CHANGELOG.md`

**Interfaces:**
- Consumes: Official `v2.0.0` release hashes, libxposed API 102 architecture, Material 3 Compose UI.
- Produces: First-class repository landing page with badges, visual identity, quick start, architecture, carrier guide, security, and verification.

- [ ] **Step 1: Draft comprehensive `README.md` featuring:**
  - Status badges (CI passing, Latest release v2.0.0, Android API 21-36, LSPosed API 102, MIT License, 80 tests passing).
  - High-impact project overview solving dual-SIM SMS routing bugs in Egypt/worldwide.
  - Architecture summary (Libxposed 102, Service IPC via XposedServiceHelper/RemotePreferences, Zero-dependency cached reflection, Compose M3).
  - Default carrier profiles (Vodafone Egypt `+20105996500`, Orange Egypt `+20122000020`) and custom carrier setup.
  - Installation & LSPosed configuration walkthrough (scope recommendations: `com.android.phone`, `com.google.android.apps.messaging`, `com.android.mms.service`).
  - Source build & test commands.
  - Cryptographic verification section (RSA 4096-bit fingerprint, SHA-256 digests).
  - Navigation links to docs, contributing, security, and license.
- [ ] **Step 2: Create `CHANGELOG.md` following Keep a Changelog standard documenting `[2.0.0] - 2026-09-16` release and legacy history**
- [ ] **Step 3: Verify markdown formatting and relative file links**
- [ ] **Step 4: Commit `docs: revamp README with modern badges and add Keep-a-Changelog CHANGELOG`**

---

### Task 4: Contributor Guide (`CONTRIBUTING.md`)

**Files:**
- Create: `CONTRIBUTING.md`

**Interfaces:**
- Consumes: Project testing strategy, Android build setup, and conventional commit rules.
- Produces: Actionable onboarding guide for third-party open-source contributors.

- [ ] **Step 1: Author `CONTRIBUTING.md` covering:**
  - Code of Conduct link
  - Development prerequisites (JDK 17, Android SDK 36, CMake/Build Tools)
  - Branching strategy & Git conventional commits
  - Non-negotiable PR requirements (the 3 mandatory sections to satisfy CI)
  - Unit testing standard (100% hermetic, zero PII, zero network)
  - Hook safety guidelines (protective mode, fail-safe preserve-on-uncertainty)
- [ ] **Step 2: Verify all paths and commands in `CONTRIBUTING.md`**
- [ ] **Step 3: Commit `docs: add comprehensive CONTRIBUTING guide for open source contributors`**

---

### Task 5: Internal Technical Documentation Realignment

**Files:**
- Modify: `docs/releases/v2.0.0.md`
- Modify: `docs/releases/v2.0.0-identity-evidence.md`
- Modify: `docs/brand/settings-ui-design.md`
- Modify: `docs/security/configuration-sharing-decision.md`

**Interfaces:**
- Consumes: Signed `v2.0.0` production artifacts, modern `SmscGuardModule.kt`, Compose M3 architecture, and `XposedService` IPC.
- Produces: Accurate, non-stale institutional engineering records.

- [ ] **Step 1: Update `docs/releases/v2.0.0.md` to record the official signed production release status, cert SHA-256, and artifact checksums**
- [ ] **Step 2: Update `docs/releases/v2.0.0-identity-evidence.md` reflecting `SmscGuardModule.kt` and `META-INF/xposed/` metadata**
- [ ] **Step 3: Modernize `docs/brand/settings-ui-design.md` documenting Jetpack Compose Material 3 UI (`SettingsDashboard.kt`, `SettingsViewModel.kt`, `ui/components/`)**
- [ ] **Step 4: Update `docs/security/configuration-sharing-decision.md` documenting ADR-002: Migration from legacy world-readable preferences to Libxposed API 102 Service IPC**
- [ ] **Step 5: Verify cross-links across all modified docs**
- [ ] **Step 6: Commit `docs: update technical records with libxposed 102 and signed v2.0.0 release details`**

---

### Task 6: Comprehensive Quality Gate & Obsidian Vault Sync

**Files:**
- Build outputs: `app/build/`
- Vault: `/home/lonevertex/Programs/LoneVertex/01 - Projects/SMSCFixer/SMSCFixer - Master Institutional Memory.md`

**Interfaces:**
- Consumes: Entire modified tree.
- Produces: 100% clean build, clean git status, updated Obsidian knowledge base.

- [ ] **Step 1: Run full verification gate: `./gradlew --no-daemon clean lint test assembleDebug assembleRelease assembleDebugAndroidTest`**
- [ ] **Step 2: Verify `git status` and commit history**
- [ ] **Step 3: Push changes to `origin/main`**
- [ ] **Step 4: Synchronize Obsidian vault master note via `obsidian` MCP tool**
- [ ] **Step 5: Emit `<!-- GOAL_COMPLETE -->`**
