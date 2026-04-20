# SmscFixer (LSPosed Module)

This repository now contains a build-ready Android Studio LSPosed/Xposed module that forces the SMSC to `+20105996500` for every `SmsManager` send path.

## Project files

- `app/src/main/java/com/smscfixer/SmscFixer.java`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/xposed_init`
- `app/build.gradle`
- `build.gradle`
- `settings.gradle`

## Build

1. Open the project root in Android Studio.
2. Sync Gradle.
3. Build debug APK:
   - `./gradlew assembleDebug`

APK output:
- `app/build/outputs/apk/debug/app-debug.apk`

## Production readiness

Production controls and runbooks are available at:

- Deployment config: `/home/runner/work/v1/v1/deploy/production.config.yml`
- Secrets template: `/home/runner/work/v1/v1/deploy/.env.production.example`
- Migration policy: `/home/runner/work/v1/v1/docs/production/migrations.md`
- Monitoring and alerts: `/home/runner/work/v1/v1/docs/production/monitoring-alerts.md`
- Rollback plan: `/home/runner/work/v1/v1/docs/production/rollback.md`
- Prod-like smoke test: `/home/runner/work/v1/v1/scripts/smoke_test_prod_like.sh`

## Install and enable

1. Install APK: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. In LSPosed Manager, enable module **SmscFixer**.
3. Set scope at least for:
   - `android` (System Framework)
   - `com.google.android.apps.messaging`
   - `com.android.mms` (if present)
4. Reboot device.

## Verify

Send an SMS and check logs:

- `adb logcat -s Xposed | grep SmscFixer`

You should see `scAddress` being replaced with `+20105996500`.
