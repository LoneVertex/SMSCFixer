# Post-Release Validation Cadence

## Preconditions

Begin this cadence only after the signed artifact, its verified digest, a known-good rollback artifact, the completed rooted-device validation matrix, and rollback-rehearsal evidence are available. For the official production v2.0.0 release, the signed artifact (`smscguard-v2.0.0-release-signed.apk`) is published with cryptographic checksums and verified against the candidate manifest.

## Staged checkpoints

1. **Internal rooted-device stage:** Confirm hook installation, scope behavior, D-06/D-07 preserve-on-uncertainty evidence, and approved SIM-specific routing cases. Conduct controlled SMS delivery only after its separate authorization and only with an approved test SIM and destination.
2. **Limited compatibility stage:** Repeat the applicable validation cases on mixed approved OEM/API/LSPosed profiles. Update the device compatibility registry with redacted outcomes before advancing.
3. **Full approved stage:** Monitor redacted hook and failure indicators for 24 hours before closure. Do not expand the rollout while crash, scope, routing, configuration-readability, or preservation anomalies remain open.

## Pass/fail criteria

| Result | Required condition |
|---|---|
| Pass | Deterministic verified-slot mapping, preserved original SMSC for unknown/conflicting evidence, no sustained hook errors, validated configuration readability, and a verified rollback artifact. |
| Hold | Incomplete device-case evidence, unsigned artifact, missing rollback evidence, or a pending compatibility finding. |
| Fail | Repeated ambiguous/failure events, unexpected package scope, configuration-readability regression, SMS routing mismatch, or failure to preserve the original SMSC. |

## Evidence handling

Record only the release tag, signed artifact digest, device/ROM/API/LSPosed versions, test-case ID, redacted decision event, operator, UTC time, and secured-evidence reference. Do not record SMS body, destination number, raw SMSC value, IMSI/ICCID, full carrier identifier, or full device fingerprint in repository or CI records.
