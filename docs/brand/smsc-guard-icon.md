# SMSC Guard Icon Asset Record

## Asset identity

The SMSC Guard icon is a custom modern application mark for the v2.0.0 release. It uses a deep midnight navy field, electric teal protected routing paths, and a compact amber routing node. The symbol is intentionally abstract: it communicates controlled telecom routing and safety without using literal message, phone, SIM-card, or surveillance imagery.

## Source and derivatives

| Asset | Path | Purpose |
|---|---|---|
| Approved master | `assets/brand/smsc-guard-icon-master.png` | High-resolution 1920×1920 visual source. |
| Deterministic generator | `scripts/generate_launcher_assets.py` | Resizes the approved master into Android launcher resource densities. |
| Legacy launcher resources | `app/src/main/res/mipmap-*/ic_launcher.png` and `ic_launcher_round.png` | Android launchers below adaptive-icon handling. |
| Adaptive foreground | `app/src/main/res/drawable/ic_launcher_foreground.png` | Android 8.0+ adaptive icon foreground. |
| Adaptive resources | `app/src/main/res/mipmap-anydpi-v26/` | XML references combining foreground and midnight-navy background. |

## Usage rules

Use the mark only for SMSC Guard application/module identity. Do not add text, raw SMSC values, carrier branding, or sensitive routing data to the icon. Do not replace the master with arbitrary generated variants without recording the new source and regenerating the Android derivatives. Any future visual refresh must preserve Android adaptive-icon safe-zone padding and must be checked at small launcher sizes.
