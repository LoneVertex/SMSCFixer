# Rollback Plan

If a staged rollout shows a routing mismatch, unexpected scope expansion, repeated hook-install failure, crash spike, or failure to preserve the original SMSC under unknown/conflicting signals, stop the rollout immediately and do not advance stages.

Disable the affected LSPosed scope or the module, then reinstall the recorded last known-good **signed** APK. Reboot the device or restart scoped processes and run the applicable redacted smoke case from `scripts/smoke_test_prod_like.sh`. Confirm both the expected decision event and controlled delivery before considering the rollback complete. Capture redacted logs and the signed artifact digest in the incident record before retrying a release.

## Rollback readiness checklist

| Requirement | Evidence |
|---|---|
| Last known-good signed APK | Signed filename, SHA-256 digest, signing-certificate digest, storage location. |
| Candidate release identity | Release tag, commit SHA, candidate manifest, signed artifact digest. |
| Supported-device evidence | Completed validation-matrix cases for the affected profile. |
| Rollback rehearsal | Reinstall/reboot/smoke result and UTC timestamp. |
| Incident capture path | Maintainer channel and secured evidence location. |
