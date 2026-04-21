# Pull-request portfolio deep audit

## Inventory snapshot

| PR | State | Type | Risk | Touched areas | Unresolved review threads | CI/check blockers |
|---|---|---|---|---|---:|---|
| #1 | merged | correctness bugfix + behavior change + ops/docs | high | hook runtime, manifest, build scaffolding, runbooks, smoke script | 5 | check-runs API unavailable to integration; no branch CI run history for this PR branch |
| #2 | merged | behavior change + safety hardening + tests/docs | high | dual-SIM selector, runtime config, settings UI, unit tests, docs | 4 (1 resolved) | check-runs API unavailable to integration; no branch CI run history for this PR branch |

Notes:
- Direct `get_check_runs` calls returned `403 Resource not accessible by integration` for PR heads, so CI status used Actions workflow-run history fallback.
- Current CI workflow exists now, but historical runs for PR #1/#2 branches are not present under `.github/workflows/ci.yml`.

## PR classification and blocker detail

### PR #1
- Classification: correctness bugfix, behavior change, docs/ops.
- Merge blockers observed during review:
  - Hook idempotency race concern.
  - Excessive non-diagnostic hook logs.
  - Signing policy ambiguity in deployment config.
  - Smoke test stale-log false-positive risk.
  - `xposedminversion` mismatch concern.
- Status: merged; blockers were partially addressed in follow-up work.

### PR #2
- Classification: behavior change, safety hardening, refactor/test improvements.
- Merge blockers observed during review:
  - Ambiguous integer argument scanning could mis-detect `subId`.
  - Diagnostic flag disable behavior concern.
  - Manifest localization concern.
  - Defensive copy concern for `targetPackages`.
- Status: merged; one thread resolved, others remained open at merge time.

## Consolidation matrix

| Area | PR #1 | PR #2 | Duplication/conflict | Merge-order dependency |
|---|---|---|---|---|
| SMS hook lifecycle/idempotency | introduced | extended | medium overlap | #1 before #2 (already satisfied) |
| Fallback selection semantics | basic hardcoded | slot + MCC/MNC + carrier fallback | additive; potential behavior conflict without tests | #1 before #2 |
| Settings/config | none | introduced UI + prefs | new surface; no direct conflict | independent |
| Diagnostics logging | ROM diagnostics | runtime diagnostics toggle | overlap; logging policy needed | #2 depends on #1 diagnostics base |
| Production runbooks | introduced | README troubleshooting additions | duplicate troubleshooting paths possible | independent |

## Close/merge/supersede strategy

1. Keep PR #1 and #2 as merged historical base.
2. Supersede unresolved PR feedback via dedicated hardening commits on mainline (not reopen old PRs).
3. Enforce single-path release policy (explicit unsigned distribution unless signing pipeline is fully wired).
4. Apply branch protection requirements before future merges:
   - required CI quality-gates workflow
   - required owner approval
   - blocked merge on failing security checks
5. Track future changes in phased execution (A→D) with acceptance + rollback checklist per phase.
