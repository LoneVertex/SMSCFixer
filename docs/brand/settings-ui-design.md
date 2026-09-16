# SMSC Guard Settings UI Design

## Purpose

SMSC Guard v2.0.0 features a safety-first, dark-technical configuration dashboard engineered using **Jetpack Compose** and **Material 3 (Material You)**. The settings interface provides clear visibility into active LSPosed framework connectivity, per-slot SMSC configurations, target package scoping, and redacted diagnostics while maintaining strict zero-PII security boundaries.

> **Design Principle:** Visual clarity and confidence must not compromise telephony stability. The interface configures and monitors guarded routing behavior; it does not claim to detect every carrier, device, or LSPosed condition.

---

## Architecture & Technology Baseline

| Dimension | Specification |
|---|---|
| Product Category | Rooted Android Telephony & Security Utility |
| UI Framework | **Jetpack Compose** with **Material 3** (`androidx.compose.material3:material3`) |
| Platform Baseline | Android API 21 (minSdk 21) to Android API 36 (compileSdk 36, targetSdk 36) |
| Architecture Pattern | Unidirectional Data Flow (UDF) via `SettingsViewModel` and `StateFlow<SettingsUiState>` |
| IPC Mechanism | Real-time `XposedServiceHelper` listener and `RemotePreferences` synchronization |
| Visual Language | Dark-technical, restrained, high-contrast, telemetry-aware |

---

## Screen Architecture & Components

The interface is structured into modular Compose components living under `io.github.lonevertex.smscguard.ui.components`:

```text
SettingsDashboard (Root Scaffold)
├── StatusHeaderCard
│   ├── App Branding & Icon
│   ├── LSPosed Active / Service Pending Telemetry Badge
│   └── API Version / Architecture Indicator
├── SlotRoutingCard
│   ├── Slot 1 SMSC Input Field (E.164 Strict Validation)
│   ├── Slot 2 SMSC Input Field (E.164 Strict Validation)
│   └── Helper Text & Canonical Carrier Normalization
├── TargetScopeSection
│   ├── Explanatory Guidance on Process Scoping
│   ├── Active Target Package Chips (Removable)
│   └── Add Custom Package Dialog / Action
├── DiagnosticsSection
│   ├── Redacted Diagnostics Toggle
│   ├── Privacy Warning & Zero-PII Guarantee
│   └── Test Hook Chain / Self-Check Action
└── Action Bar & Persistent Status Bar
    ├── Save Configuration Action (Full-width Teal)
    └── Live Status Announcement Panel
```

---

## Design Tokens & Palette

The design is anchored in a cyber-technical palette optimized for dark system themes:

| Token | Role | Hex Value |
|---|---|---|
| `Primary` / `Teal` | Primary brand accent, verified status, active buttons | `#14C8D1` |
| `Background` | Deep canvas base | `#07152B` |
| `Surface` | Grouped card surface | `#0D203A` |
| `SurfaceVariant` | Secondary surface, input field background | `#0A1B31` |
| `StatusActive` | Connected LSPosed framework status badge | `#14C8D1` |
| `StatusPending` | Pending / standalone service status badge | `#FFB547` (Amber) |
| `TextPrimary` | High-contrast cool white | `#F0F4F8` |
| `TextSecondary` | Readable cool blue-grey | `#94A3B8` |

---

## Reactive Telemetry & Live State Binding

`SettingsViewModel` observes `PreferencesManager.isLsposedBound` and `PreferencesManager.frameworkInfo`. When the LSPosed service is actively connected:
- The header displays a green/teal **LSPosed Active** badge with the framework version (e.g. `LSPosed 1.9.3 (102)`).
- When operating in standalone mode or before the service binds, it displays a polite amber **Module Standalone / Service Pending** badge.
- Telemetry badge strings are localized via Android string resources (`R.string.status_lsposed_active`, `R.string.status_lsposed_pending`).

---

## Accessibility & Safety Preservation

1. **Touch Targets:** All interactive elements (save button, self-check button, package chip delete actions) maintain at least a 48dp touch target.
2. **Strict Validation:** Input fields validate against strict E.164 international phone number formats (`+[1-9][0-9]{1,14}`). Invalid input disables the save action and renders assistive error messages.
3. **Fail-Safe Preserving:** Explanatory text clearly communicates that unknown or contradictory signals preserve the system's original SMSC without blocking delivery.
4. **Privacy:** No raw SMS messages, recipient numbers, or personal data are ever surfaced or stored.
