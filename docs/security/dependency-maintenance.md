# Dependency and vulnerability maintenance policy

## Cadence
- Review build/tool dependencies monthly.
- Review high/critical advisories on every release candidate.

## Triage SLA
- Critical: triage within 24h, remediate or document exception before next release.
- High: triage within 3 business days.
- Medium/Low: triage within 14 days.

## CI policy
- Security findings with high confidence should block merge unless exception is documented with expiry and owner.
