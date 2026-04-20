# Monitoring and alerts

## Health signals

- Module load logs present: `SmscFixer: hooked ...`
- Runtime replacement logs present when SMS is sent
- No recurring `hook error` logs

## Log checks

```bash
adb logcat -d -s Xposed | grep SmscFixer
```

## Alert conditions

- Repeated hook failures after deployment
- No hook success logs after reboot on rollout devices
- Crash reports involving `com.smscfixer`

## Alert routing

- Primary: LSPosed/module maintainer channel
- Secondary: on-call email/webhook defined in production secrets
