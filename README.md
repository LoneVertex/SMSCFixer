# SMSC Guard

<p align="center">
  <img src="assets/brand/smsc-guard-icon-master.png" alt="SMSC Guard Icon" width="128" height="128" />
</p>

<p align="center">
  <strong>Deterministic, privacy-first Android LSPosed module that guarantees correct Short Message Service Center (SMSC) routing on dual-SIM devices.</strong>
</p>

<p align="center">
  <a href="https://github.com/LoneVertex/SMSCFixer/actions/workflows/ci.yml"><img src="https://github.com/LoneVertex/SMSCFixer/actions/workflows/ci.yml/badge.svg" alt="CI Status" /></a>
  <a href="https://github.com/LoneVertex/SMSCFixer/releases/latest"><img src="https://img.shields.io/badge/Release-v2.0.0-0080FF?logo=github" alt="Latest Release: v2.0.0" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-API%2021--36-3DDC84?logo=android&logoColor=white" alt="Android API 21-36" /></a>
  <a href="https://github.com/libxposed"><img src="https://img.shields.io/badge/libxposed-API%20102-8A2BE2" alt="Libxposed API 102" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT" /></a>
  <a href="https://github.com/LoneVertex/SMSCFixer"><img src="https://img.shields.io/badge/Tests-81%20Passing-success" alt="Tests" /></a>
</p>

---

## Overview

On dual-SIM Android smartphones, modern messaging applications and telephony frameworks frequently suffer from silent SMS dispatch failures. When sending text messages from secondary SIM slots, the system may inadvertently query the active data SIM's Service Center Address (SMSC) or supply an empty routing address, resulting in carrier error code 28/38 or silent transmission drops.

**SMSC Guard** (`io.github.lonevertex.smscguard`) resolves this by intercepting public `android.telephony.SmsManager` dispatch calls at the framework level via **LSPosed**. It deterministically verifies the active subscription and physical SIM slot, maps it to the verified carrier SMSC, and replaces the routing address in-flight before the packet reaches the Radio Interface Layer (RIL).

If carrier signals are conflicting or slot evidence is indeterminate, SMSC Guard enforces a **strict fail-safe preservation policy**: the original SMSC is never modified or guessed.

---

## Architectural Highlights

- **Libxposed API 102 Native:** Completely modernized to `io.github.libxposed:api:102.0.0` and `service:102.0.0`, leveraging protective exception modes, modern dex hook engines, and OkHttp-style composable interceptor chains (`SmscGuardModule.kt`).
- **Secure Service-Based IPC:** Permanently eliminates deprecated `xposedsharedprefs` and obsolete `Context.MODE_WORLD_READABLE` filesystem permissions. Cross-process configuration is synchronized via `io.github.libxposed.service.XposedProvider` and `RemotePreferences`.
- **Zero-Dependency Reflection Engine (`ReflectUtils`):** Clean separation from legacy `XposedHelpers`. Built on thread-safe, cached Java reflection with comprehensive primitive-to-wrapper assignability and null-argument resolution.
- **Material 3 Dynamic Settings UI:** A modern Jetpack Compose dashboard (`SettingsDashboard.kt`, `SettingsViewModel.kt`) supporting dynamic theming (Material You), live LSPosed framework connection telemetry badges, per-slot E.164 phone number validation, and target scope management.
- **Privacy by Design (Zero PII):** SMS message content, recipient phone numbers, IMSI/IMEI, and personal data are **never** inspected, captured, or logged. Diagnostic logs are strictly redacted and throttled.
- **Hermetic Testing Gate:** Verified by 81 automated unit tests, strict Android Lint analysis, and signed release verification scripts.

---

## Default Carrier Configuration

While fully customizable for any worldwide carrier, SMSC Guard comes pre-configured with default values for Egyptian dual-SIM setups:

| SIM Slot | Default Carrier | Default SMSC (E.164) | MCC / MNC |
| :--- | :--- | :--- | :--- |
| **SIM 1 (Slot 0)** | Vodafone Egypt | `+20105996500` | `602 02` |
| **SIM 2 (Slot 1)** | Orange Egypt | `+20122000020` | `602 01` |

Custom carrier addresses can be entered directly in the settings dashboard using international E.164 notation (e.g. `+1...`, `+44...`, `+20...`).

---

## Installation & Setup

### Prerequisites
1. Rooted Android device running **Android 5.0 (API 21)** up to **Android 16 Developer Preview (Platform API 36)**.
2. An active LSPosed framework installation (e.g., [LSPosed](https://github.com/LSPosed/LSPosed) or modern Zygisk-based variants).

### Quick Start
1. Download the latest official signed release APK (`smscguard-v2.0.0-release-signed.apk`) from [GitHub Releases](https://github.com/LoneVertex/SMSCFixer/releases/latest).
2. Install the APK onto your device.
3. Open **LSPosed Manager**, navigate to the **Modules** tab, and toggle **SMSC Guard** ON.
4. LSPosed Manager will automatically detect the declared `xposedscope` and pre-select the recommended applications (`System Framework (android)`, `Google Messages (com.google.android.apps.messaging)`, and `MMS (com.android.mms)`). Confirm the scope or add custom OEM messaging packages if applicable.
5. Open **SMSC Guard** from your app launcher.
6. Verify the **LSPosed Active** status badge in the header, review your per-slot SMSC numbers, and tap **Save Configuration**.
7. Reboot your device or restart the scoped processes to activate hooks.

---

## Cryptographic Release Verification

All official production builds are signed using a dedicated 4096-bit RSA release key. You can independently verify the integrity and authenticity of downloaded APKs:

```bash
# Verify signature schemes (v1, v2, v3) and certificate fingerprint
apksigner verify --verbose --print-certs smscguard-v2.0.0-release-signed.apk
```

**Official Signer Certificate Identity:**
```text
Signer #1 certificate DN: CN=LoneVertex, OU=Android Security, O=LoneVertex, C=EG
Signer #1 key algorithm: RSA 4096-bit
Signer #1 certificate SHA-256: 80:5F:3E:1F:6C:04:E5:A2:0F:10:20:54:85:E7:E5:F0:72:E1:E9:EE:CA:F4:06:92:E5:5C:DB:2D:34:FD:29:53
Signer #1 certificate SHA-1:   DB:13:52:64:A2:8F:40:C7:7A:DE:E4:15:26:E2:11:F3:95:14:C0:C4
```

### v2.0.0 Artifact Checksums
```text
1c73b8d85deca0dabc55e815cbdc2dfcc07b2457e1d45f88c17b107134238104  smscguard-v2.0.0-release-signed.apk
50cc93ecbe33cb439c4ffc2a347301e57d90b9c373a9016b9dc9205e0b5e4480  app-release-unsigned.apk
47538f020d8a023327d193c80dfbd92a01738c098c6e5f305c7f91e6ccb6b9ed  smscguard-v2.0.0-debug.apk
78e8e5ca0ac5dde30fb17c336a1e5691add256acf8e906771c4b6fee6fec8fe7  smscguard-release-candidate.json
```

---

## Building from Source

### Requirements
- **JDK:** OpenJDK 17 or Eclipse Temurin 17
- **Android SDK:** Platform API 36 with Build-Tools 35.0.0+
- **Gradle:** 8.6 (managed automatically via `./gradlew`)

### Build Commands
```bash
# Clone the repository
git clone git@github.com:LoneVertex/SMSCFixer.git
cd SMSCFixer

# Run Android Lint, hermetic unit tests, and compile debug & release candidate APKs
./gradlew --no-daemon clean lint test assembleDebug assembleRelease assembleDebugAndroidTest
```

Outputs are generated under `app/build/outputs/apk/`:
- `debug/app-debug.apk`: Debug build with live debug logging.
- `release/app-release-unsigned.apk`: Unsigned release candidate.
- `androidTest/debug/app-debug-androidTest.apk`: Instrumentation test APK.

---

## Verification & Testing

### Automated Quality Gate
The repository enforces a 100% hermetic unit testing gate covering all critical components:

```bash
./gradlew --no-daemon testDebugUnitTest
```

| Test Class | Focus Area |
| :--- | :--- |
| `SmscGuardHookInterceptorTest` | LSPosed interceptor execution, short-circuiting, and argument replacement |
| `RoutingSignalResolverTest` | Cached reflection resolution of SIM slot / subId signals |
| `ReflectUtilsTest` | Cache concurrency, null safety, and method signature matching |
| `SmscSelectorBoundaryTest` | Dual-SIM boundary conditions, whitespace, and carrier normalizations |
| `SmscConfigSecurityTest` | Strict E.164 boundary validation and injection attack prevention |
| `DiagnosticLoggerConcurrencyTest` | Throttled memory ring buffer and multithreaded log safety |
| `SettingsUiLogicTest` | Jetpack Compose ViewModel state, telemetry transitions, and validation |
| `BoundedTtlCacheTest` | LRU eviction policies and TTL expiry |

### Smoke Testing on Rooted Devices
To run production-like verification on a connected rooted Android test device:

```bash
# Run non-interactive smoke test suite
./scripts/smoke_test_prod_like.sh --non-interactive
```

---

## Repository Structure

```text
SMSCFixer/
├── app/
│   ├── src/main/
│   │   ├── java/io/github/lonevertex/smscguard/
│   │   │   ├── SmscGuardModule.kt          # Libxposed API 102 entry point & hook chains
│   │   │   ├── PreferencesManager.kt       # XposedService IPC & RemotePreferences StateFlow
│   │   │   ├── SettingsDashboard.kt        # Jetpack Compose Material 3 UI root
│   │   │   ├── SettingsViewModel.kt        # UI telemetry, state & validation logic
│   │   │   ├── ReflectUtils.java           # Zero-dependency cached Java reflection engine
│   │   │   ├── RoutingSignalResolver.java  # Reflection-based SIM slot signal resolver
│   │   │   ├── SmscConfigSchema.java       # Canonical schema keys & E.164 validation
│   │   │   ├── SmscSelector.java           # Core carrier routing decision policy
│   │   │   └── ui/components/              # Modular Compose UI cards & theme tokens
│   │   └── resources/META-INF/xposed/
│   │       ├── module.prop                 # API 102 module descriptor
│   │       ├── java_init.list              # Entry point registration
│   │       └── scope.list                  # Target process scope allowlist
│   └── src/test/                           # 81 hermetic unit test suites
├── docs/                                   # Architecture, security, operations & audit docs
│   ├── operations/                         # Device compatibility registry & runbooks
│   ├── production/                         # Migration & rollback evidence
│   ├── security/                           # Threat models & configuration sharing ADRs
│   └── testing/                            # Test strategies & validation matrices
├── scripts/                                # Verification, release, and signing scripts
├── .github/                                # GitHub Actions CI, release workflows & issue forms
├── CHANGELOG.md                            # Complete version changelog
├── CONTRIBUTING.md                         # Contributor onboarding & PR guidelines
├── CODE_OF_CONDUCT.md                      # Contributor Covenant v2.1
├── SECURITY.md                             # Vulnerability disclosure policy
└── LICENSE                                 # MIT License
```

---

## Community & Contributing

Contributions are welcome! Please review our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before opening issues or submitting pull requests.

- **Found a bug?** Submit a [Bug Report](https://github.com/LoneVertex/SMSCFixer/issues/new?template=bug_report.yml).
- **Need a new carrier preset?** Request one via [Feature Request](https://github.com/LoneVertex/SMSCFixer/issues/new?template=feature_request.yml).
- **Verified a device or ROM?** Help others by submitting a [Device Compatibility Report](https://github.com/LoneVertex/SMSCFixer/issues/new?template=device_compatibility.yml).

---

## Security & Privacy

For vulnerability reporting procedures, please consult our [Security Policy](SECURITY.md).

---

## License

SMSC Guard is licensed under the [MIT License](LICENSE).  
Copyright © 2026 **LoneVertex**. All rights reserved.
