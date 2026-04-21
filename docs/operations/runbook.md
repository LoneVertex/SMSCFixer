# Operations runbook

## Deploy
1. Build artifact (`assembleRelease` for unsigned policy).
2. Run unit/lint/build quality-gates.
3. Execute smoke and staged device checks.

## Smoke
- Validate module scope, reboot/restart, send SMS from each SIM, inspect event logs.

## Rollback
- Disable module scope or module, reinstall last known-good artifact, reboot, re-run smoke.

## Incident triage
- Use incident template and attach reproducible logs/device metadata.
