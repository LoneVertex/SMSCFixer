# SmscFixer (LSPosed Module)

This repository now contains a build-ready Android Studio LSPosed/Xposed module that forces SMSC by SIM slot for compatible `SmsManager` send paths:

- SIM1 / primary slot: Vodafone Egypt `+20105996500`
- SIM2 / secondary slot: Orange Egypt `+20122000020`

It includes dynamic hook discovery to improve compatibility across vendor/custom Android builds (including newer Android API variants).

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
4. Optional production artifact build (unsigned unless release signing is configured in your environment):
   - `./gradlew assembleRelease`

APK output:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

## Production readiness

Production controls and runbooks are available at:

- Deployment config: `deploy/production.config.yml`
- Secrets template: `deploy/.env.production.example`
- Migration policy: `docs/production/migrations.md`
- Monitoring and alerts: `docs/production/monitoring-alerts.md`
- Rollback plan: `docs/production/rollback.md`
- Prod-like smoke test: `scripts/smoke_test_prod_like.sh`

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

You should see `scAddress` being replaced with:

- `+20105996500` for SIM1/primary slot
- `+20122000020` for SIM2/secondary slot

### ROM diagnostics mode (A21s / Infinity X)

On detected A21s/Infinity-X style ROM identifiers, the module enables extra diagnostic logs automatically to help tune compatibility.

Use:

- `adb logcat -d -s Xposed | grep "SmscFixer: diag"`

## Troubleshooting

### How to confirm active SIM/slot mapping

1. Enable module scope for `android` and your messaging app.
2. Send one SMS from SIM1 and one SMS from SIM2.
3. Check:
   - `adb logcat -s Xposed | grep SmscFixer`
4. Confirm logs show expected `slotIndex`/carrier details and corresponding forced SMSC.

### Quick test configuration checklist

1. Save primary/secondary SMSC values in **SmscFixer Settings**.
2. Ensure LSPosed scope includes `android` and the SMS app you are testing.
3. Reboot device (or restart scoped apps/processes).
4. Send one SMS from each SIM and inspect logs:
   - `adb logcat -s Xposed | grep SmscFixer`
5. Validate `slotIndex`, carrier/subscription mapping, and forced `scAddress`.

### If SIM2 still uses SIM1 SMSC

- Open **SmscFixer Settings** app and verify SIM2 SMSC is saved correctly.
- Reboot device (or restart scoped apps/processes) after changing settings.
- Ensure the SMS send path uses compatible `SmsManager` APIs (check `hooked` and `diag` logs).
- Verify carrier fallback signals (`mccmnc`, `carrier`) are detected in logs when slot is unresolved.

### Common LSPosed scope mistakes

- Module enabled but no `android` framework scope selected.
- Messaging app not included in LSPosed scope.
- App/process cache not restarted after enabling scope.
- Multiple messaging apps in use while only one is scoped.
