#!/usr/bin/env python3
"""Generate Android launcher resource densities from the approved icon master."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
MASTER = ROOT / "assets" / "brand" / "smsc-guard-icon-master.png"
OUTPUTS = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
FOREGROUND_SIZE = 432


def resize_square(image: Image.Image, size: int) -> Image.Image:
    return image.resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    if not MASTER.is_file():
        raise FileNotFoundError(f"Missing approved icon master: {MASTER}")

    with Image.open(MASTER) as source:
        master = source.convert("RGBA")

    for density, size in OUTPUTS.items():
        output_dir = ROOT / "app" / "src" / "main" / "res" / density
        output_dir.mkdir(parents=True, exist_ok=True)
        icon = resize_square(master, size)
        icon.save(output_dir / "ic_launcher.png", optimize=True)
        icon.save(output_dir / "ic_launcher_round.png", optimize=True)

    drawable_dir = ROOT / "app" / "src" / "main" / "res" / "drawable"
    drawable_dir.mkdir(parents=True, exist_ok=True)
    resize_square(master, FOREGROUND_SIZE).save(
        drawable_dir / "ic_launcher_foreground.png", optimize=True
    )


if __name__ == "__main__":
    main()
