# Focused threat model (hooks/reflection/logging/settings)

## Assets
- SMSC routing correctness by slot/subscription.
- Stored configuration values and target package scope.
- Diagnostic logs that may reveal runtime device/carrier context.

## Threats
1. Reflection/API variance causes wrong slot/subId inference.
2. Ambiguous carrier signals force wrong SMSC.
3. Overly verbose logs leak sensitive runtime data.
4. Corrupt preferences cause unsafe fallback behavior.

## Mitigations
- Slot-first deterministic selection with explicit ambiguous-signal safe fallback.
- Throttled reflection-failure diagnostics with controlled event taxonomy.
- Input validation + schema versioning before persistence.
- Config self-check and explicit status outcomes in Settings UI.

## Residual risks
- Vendor telephony behavior can still limit confidence in inferred signals.
- Some diagnostics remain necessary for troubleshooting and must be protected operationally.
