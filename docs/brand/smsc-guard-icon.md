# SMSC Guard Icon Asset Record: "The Routed Shield"

## Asset Identity & Visual Metaphor

The SMSC Guard launcher identity is **"The Routed Shield"**, designed for the v2.0.0 modern libxposed release and Android API 26–36+:
- **Protective Shield**: Represents security, fail-safe interception, and robust system-level protection in the telephony stack.
- **SMS Chat Bubble**: Encapsulated within the shield, establishing unambiguous domain context as a messaging-layer controller.
- **Dual Parallel Routing Vectors**: Two subtle forward routing arrows inside the bubble representing clean dual-SIM slot routing:
  - **Slot 0 (Vodafone)**: Electric Teal (`#14C8D1`) node and routing vector.
  - **Slot 1 (Orange)**: Vibrant Amber (`#FF9800`) node and routing vector.
- **Dual SIM Hub Base**: Tonal indicators and telemetry traces at the shield apex reinforcing multi-SIM routing fidelity.

## Technical Specifications

- **Adaptive Viewport**: $108 \times 108$ dp total canvas (`android:viewportWidth="108"`, `android:viewportHeight="108"`), center at $(54, 54)$.
- **Safe Zone Conformance**: Foregrounds sit strictly within the canonical $66$ dp safe diameter (max radius $31.11$ dp from center at shoulders $(76, 32)$ and $(32, 32)$), guaranteeing 100% visibility without clipping on circular, squircle, or rounded-rectangle OEM masks.
- **Material You Dynamic Theming**: Fully supports Android 13+ (API 33+) dynamic theming through `<monochrome>` layer (`ic_launcher_monochrome.xml`), cleanly tinting the shield, bubble, and routing paths to match wallpaper palette tokens.

## Source and Derivatives

| Asset | Path | Purpose |
|---|---|---|
| Master Vector Source | `assets/brand/smsc-guard-icon.svg` | Scalable $1920 \times 1920$ vector design master. |
| Master Raster Master | `assets/brand/smsc-guard-icon-master.png` | Canonical high-resolution $1920 \times 1920$ raster reference. |
| Deterministic Generator | `scripts/generate_launcher_assets.py` | Python script resizing the master into legacy density mipmaps with circular masking for round icons. |
| Adaptive Foreground | `app/src/main/res/drawable/ic_launcher_foreground.xml` | API 26+ vector foreground featuring The Routed Shield. |
| Adaptive Background | `app/src/main/res/drawable/ic_launcher_background.xml` | API 26+ Material 3 dark surface vector background (`#07152B`, `#0E223D`, `#142C4C`). |
| Material You Monochrome | `app/src/main/res/drawable/ic_launcher_monochrome.xml` | API 33+ vector silhouette for dynamic wallpaper theming. |
| Adaptive Configurations | `app/src/main/res/mipmap-anydpi-v26/` | XML descriptors linking background, foreground, and monochrome layers. |
| Legacy Densities | `app/src/main/res/mipmap-*/ic_launcher.png` and `ic_launcher_round.png` | Pre-API 26 legacy launcher mipmaps (`mdpi` through `xxxhdpi`). |

## Usage Rules

Use the mark only for SMSC Guard application and LSPosed module identity. Do not add raw carrier text or sensitive configuration strings to the icon. Any future visual refresh must preserve Android adaptive-icon safe-zone constraints and undergo AAPT2 vector verification.
