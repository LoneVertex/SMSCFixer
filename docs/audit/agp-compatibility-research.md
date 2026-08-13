# Android Gradle Plugin Compatibility Research

The current branch builds with Android Gradle Plugin 8.4, Gradle 8.6, Java 17, and compile/target SDK 36, but AGP 8.4 reports that SDK 36 is newer than its tested compile SDK range.

Official Android release documentation shows that AGP 8.9 supports a maximum API level of 35 and requires Gradle 8.11.1 with JDK 17. It would therefore not remove the project’s SDK 36 compatibility warning. AGP 9.0 supports API level 36.1 and requires Gradle 9.1.0; this is the first documented major-version path that covers the project’s current SDK target.

The AGP 9.0 release notes also describe the release as a major upgrade with API and behavior changes. Because the project—then named SMSCFixer and now SMSC Guard—uses a small Java Android module with Xposed API dependencies, an AGP 9 migration should be isolated from telephony behavior changes and validated with the existing lint, unit, instrumentation-build, rooted-device, and release-candidate gates.

## Decision

Do not silently suppress the SDK 36 warning or perform a major AGP upgrade in the current assurance patch. Maintain the validated AGP 8.4 / Gradle 8.6 / Java 17 build as the current baseline, and schedule an isolated AGP 9.0 / Gradle 9.1 compatibility branch after rooted-device validation establishes the baseline behavior.

## References

[1]: https://developer.android.com/build/releases/agp-8-9-0-release-notes "Android Gradle Plugin 8.9 release notes"
[2]: https://developer.android.com/build/releases/agp-9-0-0-release-notes "Android Gradle Plugin 9.0 release notes"
