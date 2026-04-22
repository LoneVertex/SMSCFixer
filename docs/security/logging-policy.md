# Secure logging policy

## Severity levels
- `error`: configuration load failures, repeated reflection failures, hook install failures.
- `info`: successful hook summary, non-sensitive lifecycle events.
- `diag` (opt-in/auto ROM): troubleshooting-only event detail.

## Environment policy
- Production/default: no per-method verbose hook logs unless diagnostics is enabled.
- Diagnostics mode: include event taxonomy fields needed for reproducible triage.

## Redaction/minimization
- Do not log SMS body/content.
- Avoid persistent identifiers beyond package/process/slot/subId/carrier signal needed for debugging.
- Throttle repeated reflection and fallback logs.
