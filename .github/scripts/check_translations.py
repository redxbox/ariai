#!/usr/bin/env python3
"""Fail when a literal tx(...) string lacks a Persian entry or map keys are duplicated."""

from collections import Counter
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/java"
FA_STRINGS = SOURCE / "com/ariai/app/util/FaStrings.kt"

TX_CALL = re.compile(r'\btx\(\s*"((?:\\.|[^"\\])*)"\s*\)')
MAP_KEY = re.compile(r'^\s*"((?:\\.|[^"\\])*)"\s+to\s+"', re.MULTILINE)

# These keys are passed through small key-bearing models so their tx() call is not a literal.
DYNAMIC_KEYS = {
    "Pinned",
    "Today",
    "Yesterday",
    "Previous 7 days",
    "Earlier",
    "Research-focused search API",
    "Independent web index",
    "Semantic neural search",
    "Google results via API",
    "Custom",
    "AI news today",
    "Python best practices",
    "Quantum computing",
}

source_text = "\n".join(
    path.read_text(encoding="utf-8")
    for path in SOURCE.rglob("*.kt")
    if path != FA_STRINGS
)
required = set(TX_CALL.findall(source_text)) | DYNAMIC_KEYS
translation_text = FA_STRINGS.read_text(encoding="utf-8")
entries = MAP_KEY.findall(translation_text)
translated = set(entries)
missing = sorted(required - translated)
duplicates = sorted(key for key, count in Counter(entries).items() if count > 1)

if missing or duplicates:
    if missing:
        print("Missing Persian translations:")
        for key in missing:
            print(f"  - {key}")
    if duplicates:
        print("Duplicate Persian translation keys:")
        for key in duplicates:
            print(f"  - {key}")
    sys.exit(1)

print(f"Persian translation coverage OK ({len(required)} UI keys; {len(entries)} entries).")
