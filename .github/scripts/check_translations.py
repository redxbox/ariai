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
    "Connect with OAuth before making requests.",
    "Enter valid OAuth settings and connect this MCP server.",
    "OAuth authorization expired. Reconnect to continue.",
    "OAuth settings changed. Connect again to continue.",
    "OAuth token refresh timed out. Try again.",
    "The saved OAuth state is invalid. Reconnect to continue.",
    "OAuth access was denied. You can try signing in again.",
    "OAuth authorization expired or was rejected. Start sign-in again.",
    "OAuth token exchange failed. Check your connection and try again.",
    "OAuth token refresh failed. Check your connection and try again.",
    "The OAuth authorization code expired or was already used. Start sign-in again.",
    "The OAuth client was rejected. Check the client ID and OAuth server configuration.",
    "The OAuth refresh token expired or was revoked. Reconnect to continue.",
    "The OAuth server rejected the requested scopes. Check the configured scopes.",
    "The OAuth server rejected the request. Check the OAuth configuration.",
    "The OAuth server is temporarily unavailable. Try again later.",
}

ERROR_MAPPER = SOURCE / "com/ariai/app/data/remote/ErrorMapper.kt"

def error_mapper_dynamic_keys():
    """Keys ErrorMapper passes through tx() by variable, invisible to the tx("...") scan.

    ErrorMapper is where provider errors become user-facing text, so its
    messageKey/hintKey pairs and SAFE_* composite lists must all have Persian entries.
    """
    try:
        text = ERROR_MAPPER.read_text(encoding="utf-8")
    except OSError:
        print("::warning title=Translation check::ErrorMapper.kt not found; skipping dynamic error keys")
        return set()

    keys = set()
    for message_key, hint_key in re.findall(
        r'MappedProviderError\(\s*\n?\s*"((?:\\.|[^"\\])*)"\s*,\s*\n?\s*"((?:\\.|[^"\\])*)"',
        text,
    ):
        keys.add(message_key)
        keys.add(hint_key)

    def list_block_strings(marker):
        block_start = text.find(marker)
        if block_start == -1:
            return []
        block_end = text.find("\n    )", block_start)
        if block_end == -1:
            block_end = len(text)
        return re.findall(r'"((?:\\.|[^"\\])*)"', text[block_start:block_end])

    keys.update(list_block_strings("SAFE_LOCAL_MESSAGES ="))
    keys.update(list_block_strings("SAFE_LOCAL_COMPOSITE_MESSAGES ="))
    return keys

ERROR_MAPPER_KEYS = error_mapper_dynamic_keys()

source_text = "\n".join(
    path.read_text(encoding="utf-8")
    for path in SOURCE.rglob("*.kt")
    if path != FA_STRINGS
)
required = set(TX_CALL.findall(source_text)) | DYNAMIC_KEYS | ERROR_MAPPER_KEYS
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
