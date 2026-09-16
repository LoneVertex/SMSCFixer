# SMSC Guard v2.0.0 Migration Guide

## Scope

SMSC Guard v2.0.0 replaces the previous application identity with the display name **SMSC Guard** and Android package **`io.github.lonevertex.smscguard`**. The release introduces a new adaptive launcher icon, new LSPosed module identity, and a new configuration preference namespace (`smscguard_prefs`).

> This is a **new installation**, not an Android in-place update. Android treats `com.smscfixer` and `io.github.lonevertex.smscguard` as distinct applications, even if a future controlled signing operation uses the same certificate.

## Before installing

Retain the previous known-good APK and record its SHA-256 in the rollback evidence worksheet. Do not delete the only rollback artifact. Confirm that the new candidate manifest reports the following identity:

| Field | Required v2.0.0 value |
|---|---|
| Release tag | `v2.0.0` |
| Version name | `2.0.0` |
| Version code | `2000000` |
| Application ID | `io.github.lonevertex.smscguard` |
| Candidate status | Official signed production release (`smscguard-v2.0.0-release-signed.apk`) |

## Controlled migration procedure

1. Stop the old module from intercepting traffic by disabling its LSPosed scope or disabling the old module. Do not leave both packages enabled for the same `android` or messaging-process scope.
2. Install the controlled, signed SMSC Guard artifact only after its candidate manifest, digest, and signing evidence have been verified.
3. In LSPosed Manager, enable **SMSC Guard**. The module declares canonical `xposedscope` metadata, so LSPosed Manager will automatically pre-select recommended targets (`System Framework (android)`, `Google Messages (com.google.android.apps.messaging)`, and `MMS (com.android.mms)`). Confirm the scope or add custom OEM messaging packages if applicable.
4. Open **SMSC Guard Settings**. Enter and save validated SMSC values, target package list, and diagnostic preference. Settings do not carry automatically from the old package because the package and preference namespace changed.
5. Reboot the device or restart every scoped process.
6. Run the configuration self-check. Then run the non-delivery Android instrumentation validation and the applicable rooted-device cases in `docs/testing/validation-matrix.md`.
7. Run only separately authorized controlled carrier-delivery cases. Record decision events and delivery evidence without raw SMSC values, message contents, recipient numbers, SIM identifiers, or device fingerprints.

## Required v2.0.0 validation cases

The package migration makes the following cases mandatory before broad rollout: scope recognition (D-01), primary/secondary routing (D-02/D-03 where authorized), preserve-on-uncertainty behavior (D-06/D-07), invalid configuration protection (D-09), post-restart configuration loading (D-10), Libxposed API 102 Service IPC (D-11), and standalone fallback behavior (D-12).

## Rollback

If SMSC Guard fails a required validation case, disable its LSPosed module scope first. Reinstall or re-enable the recorded known-good previous package, restore its former LSPosed scope, reboot/restart relevant processes, and run the associated smoke check. Do not attempt to copy the new package’s preference file into the old package’s storage. Complete the rollback section of `docs/production/release-and-rollback-evidence.md`.

## Release gates that remain external

This guide does not authorize signing, remote publication, device installation, LSPosed enablement, or SMS delivery. A controlled signing owner, a rooted-device test operator, and a release reviewer must provide the associated evidence before release or deployment.
