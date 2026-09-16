# Security Policy

## Supported Versions

Security updates and patches are actively maintained for the following versions of SMSC Guard:

| Version | Supported          | Security State |
| ------- | ------------------ | -------------- |
| 2.0.x   | :white_check_mark: | Active (Libxposed API 102 & Service IPC) |
| < 2.0.0 | :x:                | Deprecated / EOL (`com.smscfixer` legacy) |

---

## Reporting a Vulnerability

We take the security and privacy of SMSC Guard seriously. Because SMSC Guard operates as a high-privilege Android LSPosed module intercepting telephony framework operations, strict security and privacy boundaries are enforced by design.

If you believe you have discovered a vulnerability, security flaw, or privacy violation (such as unintended PII logging or privilege boundary bypass), **please do not open a public GitHub issue**.

### Reporting Channel
- **Email:** [minaalaa141@gmail.com](mailto:minaalaa141@gmail.com)
- **Subject Line:** `[SMSC Guard Security Advisory] <Short Description>`
- **GitHub Private Vulnerability Reporting:** If available on the repository, you may also submit reports via the GitHub Advisory Database tab.

### Report Contents
Please include the following details in your report:
1. **Description:** High-level summary of the issue.
2. **Impact:** Attack vector, affected components, or data exposure risks.
3. **Steps to Reproduce:** Exact steps, POC code, or log excerpts (ensure all personal PII is redacted).
4. **Environment:** Android version, ROM / OS, LSPosed framework release, and device model.
5. **Proposed Fix / Remediation:** If you have developed or identified a potential mitigation.

### Response Timeline
- **Initial Acknowledgment:** Within 48 hours.
- **Triage & Reproduction:** Within 5 business days.
- **Resolution & Release:** A patched release will be published alongside an advisory crediting the reporter (unless anonymity is requested).

---

## Security Principles & Privacy Guarantees

1. **Zero PII Logging:** The module is strictly prohibited from persisting or logging message bodies, recipient phone numbers, IMSI/IMEI, carrier secrets, or subscriber identifiers.
2. **Fail-Safe Preserving:** In any ambiguous state, exception, or carrier mismatch, the module preserves the system's original SMSC without interfering or failing silently.
3. **Protected IPC:** All configuration synchronization operates exclusively through official `XposedService` contracts. Legacy world-readable modes (`Context.MODE_WORLD_READABLE`) and filesystem permission overrides are permanently banned.
4. **Scoped Injection:** Injection is explicitly restricted via `META-INF/xposed/scope.list` to authorized telephony and messaging packages.
