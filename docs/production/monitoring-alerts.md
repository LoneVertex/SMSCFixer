# Monitoring and Alerts

## Health signals

The primary runtime signals are redacted Xposed events. A supported scope should emit `hook_applied` and a `hook_summary` after process initialization. A controlled send should emit either `smsc_replaced` with a valid decision reason or `replacement_preserved` when routing evidence is unknown or conflicting. Repeated `hook_install_failed`, `config_load_failed`, or `reflection_failure` events require investigation.

## Log review

Use the following command only on controlled test devices and retain redacted extracts:

```bash
adb logcat -d -s Xposed | grep SmscFixer
```

Default logs must not contain message bodies, raw destination numbers, previous/replacement SMSC values, full method signatures, carrier names, or device fingerprints. Detailed diagnostics are opt-in and should be disabled after investigation.

## Alert conditions

Stop or hold the current rollout stage when there is a crash spike involving `com.smscfixer`, repeated hook-install failure, no `hook_applied` evidence after a supported-device reboot, an unexpected package-scope event, a routing mismatch, or a missing `replacement_preserved` event in an unknown/conflicting-signal test. Alert routing is the maintainer channel first, with the optional on-call integration configured outside the repository.

## Evidence retention

Record release tag, signed artifact digest, device/ROM/API/LSPosed version, scoped package, test-case ID, event result, UTC timestamp, and operator. Keep detailed carrier or user-identifying data in the approved secured incident channel only.
