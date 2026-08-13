# Configuration-Sharing Decision Record

## Decision

SMSC Guard adopts LSPosed’s managed XSharedPreferences path as the preferred configuration-sharing mechanism. The manifest declares `xposedsharedprefs=true`, and Settings requests `MODE_WORLD_READABLE` when obtaining its module preferences. On LSPosed API 93+ environments that support the feature, LSPosed manages the preference location and readability; the hook continues to load configuration by module package and preference name.

The module retains a narrowly scoped compatibility fallback for older or unavailable managers. If requesting `MODE_WORLD_READABLE` throws `SecurityException`, Settings falls back to private preferences and changes readability only on the single preferences XML file. It never broadens the application data directory or the `shared_prefs` directory. All values are validated after read, and `ConfigurationRepository` rejects an unreadable `XSharedPreferences` file by returning safe defaults.

## Why a permission-protected provider is not used now

A signature-protected provider would prevent arbitrary callers but an injected LSPosed hook runs under the identity of the scoped target process, which is not signed with the module certificate. An exported unprotected provider would instead make the configuration broadly readable. Without target-device evidence that a provider permission can distinguish the intended hook process from arbitrary apps, it would not be a clear least-privilege improvement over LSPosed’s documented managed preference support.

## Migration behavior

Version 2.0.0 uses the `smscguard_prefs` preference name under the new `io.github.lonevertex.smscguard` package. Because the Android package migration is a new installation, old `smscfixer_prefs` values are intentionally not copied automatically; the operator re-enters validated settings after the new module is enabled. The managed LSPosed path may store the new values outside the legacy application `shared_prefs` directory. The code therefore does not assume a direct legacy path when managed preferences are active. If managed mode is rejected, the narrow single-file fallback remains available for the new preference file.

## Required validation

| Case | Required evidence |
|---|---|
| LSPosed API 93+ managed mode | Save settings, restart scoped process, confirm `XSharedPreferences` file readability and correct validated configuration load without legacy directory permission changes. |
| Legacy/unsupported manager | Confirm `SecurityException` fallback writes a readable single XML file without directory traversal changes and configuration still loads. |
| Invalid/malformed stored values | Confirm safe default target scope and no arbitrary package interception. |
| Configuration change | Confirm changed valid settings load after process restart; do not treat a hot reload as guaranteed unless listener behavior is explicitly verified. |
| Permission boundary | Confirm no app-data or `shared_prefs` directory permissions are broadened; capture redacted mode/path evidence only. |

## Residual risk

This decision improves the module’s posture where LSPosed API 93+ support is present, but cross-process configuration remains dependent on the target manager and Android SELinux behavior. The fallback is retained until rooted-device evidence covers all supported profiles. No production claim is valid until the required cases are recorded in the compatibility registry.

## Reference

The behavior is based on the LSPosed New XSharedPreferences guidance, which documents `xposedsharedprefs`, `MODE_WORLD_READABLE`, package/name-based reads, file-readability checks, and API 93+ support. [1]

[1]: https://github.com/LSPosed/LSPosed/wiki/New-XSharedPreferences "LSPosed New XSharedPreferences"
