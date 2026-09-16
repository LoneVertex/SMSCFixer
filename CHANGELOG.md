# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.1.0] - 2026-09-16

### Added
- **"The Routed Shield" Launcher Identity:** Production-ready Adaptive Launcher Icon and Material You Themed Icon (`ic_launcher_monochrome`) conforming to Android API 36 adaptive icon guidelines with 108dp viewport and safe-zone circular masking.
- **Canonical LSPosed Scope Declaration:** Configured `<meta-data android:name="xposedscope" android:resource="@array/xposed_scope" />` in `AndroidManifest.xml` with companion `@array/xposed_scope` (`android`, `com.google.android.apps.messaging`, `com.android.mms`) to enable automated module target recommendations in LSPosed Manager.
- **Open Source Community Architecture:** Added full open source standards including `CONTRIBUTING.md` developer guide, GitHub Issue Forms (`bug_report.yml`, `feature_request.yml`, `device_compatibility.yml`), and CI-enforced Pull Request Template.

### Changed
- **Config Schema Decoupling:** Centralized default scope target resolution into `SmscConfigSchema.defaultTargetPackages()`, breaking circular compile dependencies from `SmscRuntimeConfig`.
- **Encapsulated Preferences Persistence:** Migrated raw `SharedPreferences.Editor` manipulations out of `SettingsViewModel` into `PreferencesManager.saveSettings()`.
- **UI Localization:** Extracted all user-facing action and status feedback strings to `res/values/strings.xml` with dynamic format argument support in `UiStatus.ResourceMessage`.

### Refactored
- **Anti-Koshary Architecture Hardening:** Flattened nested try-catch reflection logic in `RoutingSignalResolver` into modular discrete helper functions (`getTelephonyManager`, `resolveScopedTelephony`, `readSimOperatorName`, `readSimOperator`).
- **Dead Code Pruning:** Purged deprecated `isLsposedManaged` and `makePrefsReadableForXposed()` shims and pruned obsolete 5-arg constructors in `ConfigurationRepository`.
- **Build Configuration:** Documented upstream `libxposed:service:102.0.0` `minCompileSdk=37` mitigation via `checkAarMetadata` suppression for Android Platform API 36 builds.

---

## [2.0.0] - 2026-09-16

### Added
- **Libxposed API 102 Migration:** Complete upgrade from legacy `de.robv.android.xposed:api:82` to `io.github.libxposed:api:102.0.0` and `io.github.libxposed:service:102.0.0`.
- **Modern Packaging Metadata:** Registered `META-INF/xposed/module.prop`, `java_init.list`, and `scope.list` defining module properties and process scoping.
- **Service IPC & RemotePreferences:** Implemented `io.github.libxposed.service.XposedProvider` with `XposedServiceHelper` listener for secure cross-process preference synchronization without filesystem permission mutations.
- **Zero-Dependency Reflection Core:** Added `ReflectUtils.java` providing cached Java reflection with primitive widening, wrapper assignability, and null-argument matching, decoupling the codebase entirely from `XposedHelpers`.
- **Material 3 Jetpack Compose Dashboard:** Completely replaced legacy XML activities with modern Compose Material You dashboard (`SettingsDashboard.kt`, `SettingsViewModel.kt`).
- **Live Telemetry State:** Bound real-time framework connection status (`isLsposedBound`, `frameworkInfo`) directly into the UI header badge.
- **Comprehensive Quality Gate:** Added hermetic unit tests (`DiagnosticLoggerConcurrencyTest`, `HookSignatureRegistryPlatformTest`, `ReflectUtilsTest`, `RoutingSignalResolverTest`, `SettingsUiLogicTest`, `SmscConfigSecurityTest`, `SmscGuardHookInterceptorTest`, `SmscSelectorBoundaryTest`) expanding test coverage to 80/80 passing tests.
- **Production Release Signing:** Integrated automated page-alignment (`zipalign`) and cryptographic signing (`apksigner`) with a dedicated 4096-bit RSA key validating APK Signature Schemes v1, v2, and v3.
- **Community Health Infrastructure:** Added MIT License, Contributor Covenant v2.1 Code of Conduct, Security Policy, GitHub Issue Forms, and CI-aligned Pull Request Template.

### Changed
- **Package Identity:** Migrated namespace from legacy `com.smscfixer` to official `io.github.lonevertex.smscguard`.
- **Hook Pipeline:** Refactored `SmscGuard` to `SmscGuardModule.kt` utilizing OkHttp-style composable interceptor chains (`TestHookChain`).
- **Target Platform:** Upgraded compilation toolchain to target Android Platform API 36 (Android 16 DP) and Java 17.

### Removed
- **Legacy Xposed Artifacts:** Permanently removed `assets/xposed_init`, `de.robv.android.xposed.XposedBridge`, and `de.robv.android.xposed.XposedHelpers`.
- **Deprecated Filesystem Sharing:** Permanently removed `xposedsharedprefs`, `Context.MODE_WORLD_READABLE`, and manual file permission modification scripts.
- **Legacy XML Views:** Removed `res/layout/activity_settings.xml` and deprecated legacy button drawables.

---

## [1.0.0] - 2026-08-20

### Added
- Initial release of SMSCFixer (`com.smscfixer`).
- Basic Xposed hook for `SmsManager.sendTextMessage` and `sendMultipartTextMessage`.
- Slot-based SMSC routing for dual-SIM devices with default Egyptian carrier configurations (Vodafone Egypt and Orange Egypt).
- Fail-safe preservation logic when carrier signals or slot evidence are ambiguous.
- XML-based configuration activity for custom SMSC numbers.

[2.1.0]: https://github.com/LoneVertex/SMSCFixer/compare/v2.0.0...v2.1.0
[2.0.0]: https://github.com/LoneVertex/SMSCFixer/compare/v1.0.0...v2.0.0
[1.0.0]: https://github.com/LoneVertex/SMSCFixer/releases/tag/v1.0.0
