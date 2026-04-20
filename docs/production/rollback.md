# Rollback plan

If a production rollout shows failures:

1. Stop rollout immediately (do not advance stages).
2. Disable module scope for affected apps or disable module entirely in LSPosed.
3. Reinstall last known-good APK on test and affected devices.
4. Reboot and verify previous behavior with smoke checks.
5. Capture logs and open a fix issue before retrying rollout.

## Rollback readiness checklist

- Last known-good signed APK retained
- Release notes include changed hook paths
- Device validation matrix available
