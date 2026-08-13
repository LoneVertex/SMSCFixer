# Dependency and Build Compatibility Review

## Resolved dependency surfaces

The debug application runtime classpath resolves no runtime dependencies. The Xposed API is compile-only and the production APK does not bundle a third-party runtime dependency from the Gradle declarations. The Android instrumentation test runtime contains the declared AndroidX Test JUnit/runner artifacts and their transitive test support libraries; this dependency graph is confined to the test APK.

Declared dependency coordinates are exact versions. No `+`, `latest`, or other dynamic version selector was found in project Gradle files. The CI workflow compiles the instrumentation test APK on every push and pull request so incompatibilities in the test dependency surface are detected before review.

## Build-tool compatibility decision

| Component | Current validated baseline | Research conclusion | Decision |
|---|---|---|---|
| Java | 17 | AGP 8.9 and AGP 9.0 both use JDK 17. | Retain Java 17. |
| Gradle wrapper | 8.6 | AGP 9.0 requires Gradle 9.1.0. | Retain 8.6 in this branch; upgrade only with AGP 9 work. |
| AGP | 8.4 | Its local warning indicates SDK 36 exceeds its tested compile SDK range. AGP 8.9 supports only API 35; AGP 9.0 supports API 36.1. | Do not suppress the warning or perform a major upgrade in a telephony assurance branch. |
| compile/target SDK | 36 | Covered by AGP 9.0 but not AGP 8.9. | Retain SDK 36; schedule isolated AGP 9.0/Gradle 9.1 test branch. |

## Release compatibility conclusion

The current baseline is reproducibly buildable with Java 17, the local Android SDK 36, and Gradle 8.6; it passes lint, unit tests, application assembly, and instrumentation-test APK compilation. The AGP 8.4 warning is a known compatibility-monitoring item, not a suppressed error. An upgrade is justified only as a discrete build-system migration with the existing unit, lint, instrumentation-build, rooted-device, release-candidate, and rollback gates rerun.

## References

[1]: https://developer.android.com/build/releases/agp-8-9-0-release-notes "Android Gradle Plugin 8.9 release notes"
[2]: https://developer.android.com/build/releases/agp-9-0-0-release-notes "Android Gradle Plugin 9.0 release notes"
