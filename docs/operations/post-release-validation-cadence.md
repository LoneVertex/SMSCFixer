# Post-release validation cadence

## Staged checkpoints
1. Internal stage: hook install + SMS send from each SIM + fallback decision verification.
2. Limited stage: repeat on mixed OEM/API devices.
3. Full stage: monitor hook/failure indicators for 24h before close.

## Pass/fail criteria
- Pass: deterministic slot mapping, no sustained hook errors, rollback artifact available.
- Fail: repeated ambiguous/failure events or SMS routing mismatch.
