#!/usr/bin/env python3
"""Enforces the strict 200-line maximum length rule for project source files."""

from pathlib import Path
import sys

MAX_LINES = 200
SOURCE_EXTENSIONS = {".java", ".kt", ".kts", ".py", ".groovy"}
EXCLUDED_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    "build",
    "out",
    "run",
    "gradle",
    "__pycache__",
}


def is_excluded(path: Path, root: Path) -> bool:
    relative_parts = path.relative_to(root).parts
    return any(part in EXCLUDED_DIRS for part in relative_parts)


def count_lines(file_path: Path) -> int:
    with file_path.open("r", encoding="utf-8", errors="replace") as handle:
        return sum(1 for _ in handle)


def scan_repository(root: Path) -> tuple[list[tuple[Path, int]], int]:
    violations: list[tuple[Path, int]] = []
    checked = 0
    for path in sorted(root.rglob("*")):
        if not path.is_file() or is_excluded(path, root):
            continue
        if path.suffix.lower() not in SOURCE_EXTENSIONS:
            continue
        checked += 1
        line_count = count_lines(path)
        if line_count > MAX_LINES:
            violations.append((path.relative_to(root), line_count))
    return violations, checked


def main() -> int:
    root = Path(__file__).resolve().parent.parent
    violations, checked = scan_repository(root)
    print(f"[check-file-length] Checked {checked} source files (limit: {MAX_LINES} lines).")
    if violations:
        print("[check-file-length] VIOLATIONS FOUND:")
        for rel_path, lines in violations:
            print(f"  - {rel_path.as_posix()}: {lines} lines (+{lines - MAX_LINES})")
        return 1
    print("[check-file-length] OK: all source files comply with the 200-line limit.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
