# SMSC Guard Settings UI Design

## Purpose

SMSC Guard v2.0.0 presents a narrow, safety-first configuration surface for an LSPosed module. The settings redesign replaces the previous utilitarian form treatment with a **dark-technical, Android-native visual system** that makes routing controls, target scope, diagnostics, actions, and live configuration status easier to distinguish without changing the module’s behavior.

> **Design principle:** Visual confidence must not imply broader authority. The interface explains and configures guarded routing behavior; it does not claim that it can detect every carrier, device, or LSPosed condition.

The design uses native Android XML resources and the existing platform Material theme. It deliberately introduces **no Material Components, Compose, telemetry, navigation, network access, or new persistence state**.

## Design Read

| Dimension | Decision |
|---|---|
| Product category | Rooted power-user safety utility |
| Visual language | Restrained dark-technical, trust-first, Android-native |
| Design variance | 4 of 10: structured and deliberate rather than expressive |
| Motion | 2 of 10: native pressed feedback only; no decorative animation |
| Information density | 5 of 10: grouped configuration detail with readable breathing room |
| Platform baseline | Native Android views, API 21 minimum, `Theme.Material.NoActionBar` |

## Token System

The UI derives its identity from the v2 SMSC Guard launcher icon: midnight navy for the environment, electric teal for verified action and focus, and amber only for diagnostic attention.

| Token | Role | Value / behavior |
|---|---|---|
| `sg_background` | App canvas and system-bar base | `#07152B` midnight navy |
| `sg_surface` | Section group surface | `#0D203A` deep blue slate |
| `sg_field` | Editable field surface | `#0A1B31` blue-black |
| `sg_teal` | Primary action and focused field state | `#14C8D1` electric teal |
| `sg_amber` | Diagnostics attention cue only | `#FFB547` amber |
| `sg_text_primary` | Titles, labels, primary control text | cool white |
| `sg_text_secondary` | Supporting explanatory copy | cool blue-grey |
| `sg_text_muted` | Low-emphasis helper text and hints | subdued blue-grey |
| `sg_status_surface` | Persistent status panel | dark, bounded informational surface |

The shape rule is equally deliberate: **16dp** section groups, **12dp** fields and action controls, and no decorative pill proliferation. The UI remains flat enough to feel native while using restrained boundaries to make a safety-critical form scannable.

## Screen Architecture

| Area | Visual treatment | Behavior preserved |
|---|---|---|
| Branded header | Launcher icon, context label, product title, concise routing-safety framing | No navigation or new state |
| Routing configuration | Grouped surface for primary and secondary SMSC fields, labels above inputs, factual helper copy | Phone input types, existing view IDs, validation, normalization, and fallback semantics |
| Target scope | Separate surface clarifying the explicit package-scope requirement | Existing package field, package validation, and normalized storage |
| Diagnostics | Clearly labelled checkbox with privacy-aware helper copy and reserved amber cue | Existing opt-in boolean preference and logging semantics |
| Action stack | Teal full-width save action followed by outlined self-check action | Existing IDs, listeners, disabled-save behavior, async save thread, and self-check logic |
| Status and safety note | Persistent low-emphasis status surface and preserve-on-uncertainty explanation | Existing `statusText` ID, polite live-region behavior, and status announcements |

The form remains inside a vertically scrolling root so it remains reachable with the soft keyboard and on compact devices. Every interactive control retains at least a 48dp minimum height.

## Accessibility and Safety Preservation

The redesign retains explicit `labelFor` associations for the three editable fields and preserves a polite `accessibilityLiveRegion` on `statusText`. The primary and secondary actions have distinct visual hierarchy and explicit text labels. Diagnostics is understandable through written labels and helper copy; amber is a supplementary attention cue, not the only communication channel.

All user-facing explanatory copy is scoped to known behavior. In particular, the terminal safety note reinforces the routing policy:

> Unknown or conflicting routing evidence preserves the original SMSC.

The redesign does **not** surface raw SMSC values outside the user-controlled input fields and does not change the module’s existing diagnostic privacy boundary.

## Implementation Artifacts

| Artifact | Purpose |
|---|---|
| `res/values/colors.xml` | Opaque SMSC Guard color tokens |
| `res/values/styles.xml` | Native app theme and `TextAppearance.SmscGuard.*` typography hierarchy |
| `res/values-v23/styles.xml` | API 23+ dark status-bar icon behavior |
| `res/values-v27/styles.xml` | API 27+ dark navigation-bar icon behavior |
| `res/drawable/bg_section.xml` | Section surface treatment |
| `res/drawable/bg_input.xml` | Field default and focused states |
| `res/drawable/bg_primary_button.xml` | Enabled, pressed, and disabled save action states |
| `res/drawable/bg_secondary_button.xml` | Secondary self-check action states |
| `res/drawable/bg_status.xml` | Persistent configuration-status panel |
| `res/layout/activity_settings.xml` | Redesigned visual hierarchy with retained runtime IDs |
| `docs/brand/assets/smsc-guard-settings-v2-mockup.png` | Visual mockup of the design direction; not a device screenshot |

## Automated Validation Evidence

The redesign was validated locally with the following full quality gate using Java 17:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
./gradlew --no-daemon clean lint test assembleDebug assembleRelease assembleDebugAndroidTest
```

The quality gate completed successfully after two compatibility-only fixes: an explicit `TextAppearance.SmscGuard` base style for Android dotted-style inheritance and version-qualified system-bar theme resources for attributes introduced after API 21. The project retains its documented AGP 8.4 / compileSdk 36 warning; this is a pre-existing toolchain compatibility notice, not a redesign failure.

The packaged debug candidate was inspected and reports the expected v2 identity.

| Package check | Verified value |
|---|---|
| Application ID | `io.github.lonevertex.smscguard` |
| Version code | `2000000` |
| Version name | `2.0.0` |
| Application label | `SMSC Guard` |
| Minimum SDK | `21` |
| Target SDK | `36` |
| Launcher icon | Adaptive `ic_launcher.xml` resource |
| Application theme | `style/AppTheme` |

Instrumentation coverage asserts that all seven settings controls remain resolvable, that the live status region remains polite, and that the primary and secondary action labels and initial status copy are present. JVM tests remain responsible for routing, configuration normalization, package scoping, cache, and fallback-policy behavior.

## Deferred Controlled-Device Checks

No rooted-device testing, LSPosed activation, device installation, carrier interaction, or SMS transmission was performed for this redesign. A separately authorized controlled-device test should verify the following before operational deployment:

1. Render the screen on small and large Android devices at the supported API range.
2. Confirm keyboard navigation, visible field focus treatment, and reachable lower actions.
3. Confirm TalkBack field labels, diagnostic explanation, and polite status announcements.
4. Confirm invalid SMSC and invalid package-list errors retain their existing location and wording.
5. Confirm valid save, the transient disabled-save interval, self-check outcomes, and reboot/restart reminder behavior.
6. Confirm LSPosed can read the expected managed preference configuration under the project’s documented fallback behavior.
7. Confirm no routing action occurs where signal evidence is unknown or conflicting.

## Scope Boundary

This document records a UI-only change. It does not authorize signing, release publication, installation, configuration scoping, rooting changes, carrier changes, or SMS transmission. The corresponding code changes are published for review on [`manus/smsc-guard-v2-release-candidate`](https://github.com/LoneVertex/SMSCFixer/tree/manus/smsc-guard-v2-release-candidate) through [pull request #4](https://github.com/LoneVertex/SMSCFixer/pull/4); review publication does not authorize deployment actions.
