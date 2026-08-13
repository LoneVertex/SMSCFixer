# Secure Logging Policy

## Default events

Default production logs contain only stable lifecycle and failure events such as `hook_applied`, `hook_summary`, `config_load_failed`, and `hook_install_failed`. They must not include message bodies, destination numbers, original or replacement SMSC values, carrier names, subscription identifiers, full device fingerprints, or full reflective method details.

## Opt-in diagnostics

Diagnostics can be enabled temporarily from Settings or automatically for documented compatibility investigation. Diagnostic events use a restricted taxonomy: `hook_installed`, `smsc_replaced`, `replacement_preserved`, and `reflection_failure`. They contain only the decision reason, operation name, or a registered signature identifier. Disable diagnostics after the investigation and retain only redacted extracts in incident evidence.

## Retention and rate limiting

Repeated diagnostic/failure events are throttled and bounded in memory. Operators must use controlled devices and delete local log captures after the incident-retention period required by the maintainer policy. Detailed carrier or user-identifying data belongs only in the approved secured incident channel, never in repository issues, CI output, or release artifacts.
