#!/usr/bin/env python3
"""Find, assert, or tap a visible Android UI label through uiautomator."""
import argparse
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

DUMP = "/sdcard/ui.xml"


def run_adb(*args: str, timeout: int = 30) -> str:
    result = subprocess.run(
        ["adb", *args], capture_output=True, text=True, timeout=timeout, check=False
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip() or "adb command failed")
    return result.stdout


def dump_xml() -> str:
    run_adb("shell", "uiautomator", "dump", DUMP)
    xml = run_adb("shell", "cat", DUMP)
    if "<hierarchy" not in xml:
        raise RuntimeError("uiautomator did not return a UI hierarchy")
    return xml


def node_values(node: ET.Element) -> tuple[str, str]:
    return node.get("text", "").strip(), node.get("content-desc", "").strip()


def locate(xml: str, label: str) -> tuple[int, int] | None:
    root = ET.fromstring(xml)
    needle = label.casefold()
    exact: list[ET.Element] = []
    partial: list[ET.Element] = []
    for node in root.iter("node"):
        values = node_values(node)
        if any(value.casefold() == needle for value in values):
            exact.append(node)
        elif any(needle in value.casefold() for value in values):
            partial.append(node)

    for node in exact + partial:
        bounds = node.get("bounds", "")
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if match:
            left, top, right, bottom = map(int, match.groups())
            if right > left and bottom > top:
                return (left + right) // 2, (top + bottom) // 2
    return None


def is_visible(xml: str, label: str) -> bool:
    root = ET.fromstring(xml)
    needle = label.casefold()
    return any(
        needle in value.casefold()
        for node in root.iter("node")
        for value in node_values(node)
    )


def screen_size() -> tuple[int, int]:
    output = run_adb("shell", "wm", "size")
    match = re.search(r"(\d+)x(\d+)", output)
    return (int(match.group(1)), int(match.group(2))) if match else (1080, 2400)


def scroll_page() -> None:
    width, height = screen_size()
    x = width // 2
    run_adb(
        "shell", "input", "swipe", str(x), str(int(height * 0.82)),
        str(x), str(int(height * 0.32)), "350"
    )
    time.sleep(0.35)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--scroll", action="store_true", help="scroll down until the label is visible")
    parser.add_argument("--assert", dest="assert_only", action="store_true", help="check visibility without tapping")
    parser.add_argument("label", nargs="+", help="visible text or content description")
    args = parser.parse_args()
    label = " ".join(args.label)
    attempts = 7 if args.scroll else 1

    for attempt in range(attempts):
        try:
            xml = dump_xml()
            if args.assert_only:
                if is_visible(xml, label):
                    print(f"VISIBLE: {label}")
                    return 0
            else:
                position = locate(xml, label)
                if position is not None:
                    run_adb("shell", "input", "tap", str(position[0]), str(position[1]))
                    print(f"tapped {label!r} at {position}")
                    return 0
        except Exception as error:  # include adb/parse failures in the run log
            print(f"UI automation error for {label!r}: {error}", file=sys.stderr)
            return 2

        if attempt + 1 < attempts:
            scroll_page()

    action = "not visible" if args.assert_only else "not found"
    print(f"{action}: {label}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
