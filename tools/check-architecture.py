#!/usr/bin/env python3
"""Verifies clean architecture dependency boundaries (SOLID / DIP)."""

from pathlib import Path
import re
import sys

IMPORT_PATTERN = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)\s*;")

FORBIDDEN_IMPORTS = {
    "core": ("net.minecraft.", "net.neoforged.", "com.mojang.", "org.lwjgl."),
    "physics": (
        "net.minecraft.",
        "net.neoforged.",
        "com.mojang.",
        "org.lwjgl.",
        "com.realisticvehicles.minecraft.",
        "com.realisticvehicles.client.",
        "com.realisticvehicles.render.",
    ),
    "render/api": ("net.minecraft.", "net.neoforged.", "com.mojang."),
}


def find_violations(src_root: Path) -> tuple[list[str], int]:
    violations: list[str] = []
    checked = 0
    if not src_root.exists():
        return violations, checked
    for java_file in sorted(src_root.rglob("*.java")):
        checked += 1
        rel = java_file.relative_to(src_root).as_posix()
        for layer, forbidden_prefixes in FORBIDDEN_IMPORTS.items():
            if not rel.startswith(layer + "/"):
                continue
            for line_no, line in enumerate(
                java_file.read_text(encoding="utf-8").splitlines(), start=1
            ):
                match = IMPORT_PATTERN.match(line)
                if not match:
                    continue
                imported = match.group(1)
                if any(imported.startswith(prefix) for prefix in forbidden_prefixes):
                    violations.append(
                        f"{rel}:{line_no} imports forbidden '{imported}' in layer '{layer}'"
                    )
    return violations, checked


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    src_root = root / "src" / "main" / "java" / "com" / "realisticvehicles"
    violations, checked = find_violations(src_root)
    print(f"[check-architecture] Checked {checked} Java source files.")
    if violations:
        print("[check-architecture] BOUNDARY VIOLATIONS:")
        for violation in violations:
            print(f"  - {violation}")
        return 1
    print("[check-architecture] OK: physics/core layers remain decoupled from Minecraft/rendering.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
