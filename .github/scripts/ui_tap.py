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


def node_bounds(node: ET.Element) -> tuple[int, int, int, int] | None:
    bounds = node.get("bounds", "")
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not match:
        return None
    return tuple(map(int, match.groups()))


def visible_bounds(node: ET.Element, width: int, height: int) -> tuple[int, int, int, int] | None:
    if node.get("visible-to-user", "true").casefold() == "false":
        return None
    bounds = node_bounds(node)
    if bounds is None:
        return None
    left, top, right, bottom = bounds
    left, top = max(0, left), max(0, top)
    right, bottom = min(width, right), min(height, bottom)
    return (left, top, right, bottom) if right > left and bottom > top else None


def locate(xml: str, label: str, exact_only: bool = False) -> tuple[int, int] | None:
    root = ET.fromstring(xml)
    width, height = screen_size()
    needle = label.casefold()
    exact: list[ET.Element] = []
    partial: list[ET.Element] = []
    for node in root.iter("node"):
        values = node_values(node)
        if any(value.casefold() == needle for value in values):
            exact.append(node)
        elif not exact_only and any(needle in value.casefold() for value in values):
            partial.append(node)

    for node in exact + partial:
        bounds = visible_bounds(node, width, height)
        if bounds is not None:
            left, top, right, bottom = bounds
            return (left + right) // 2, (top + bottom) // 2
    return None


def is_visible(xml: str, label: str, exact_only: bool = False) -> bool:
    root = ET.fromstring(xml)
    width, height = screen_size()
    needle = label.casefold()
    return any(
        (value.casefold() == needle if exact_only else needle in value.casefold())
        for node in root.iter("node")
        if visible_bounds(node, width, height) is not None
        for value in node_values(node)
    )


def is_scrollable(xml: str) -> bool:
    root = ET.fromstring(xml)
    return any(node.get("scrollable", "false").casefold() == "true" for node in root.iter("node"))


def visible_labels(xml: str, limit: int = 80) -> str:
    root = ET.fromstring(xml)
    width, height = screen_size()
    labels = dict.fromkeys(
        value
        for node in root.iter("node")
        if visible_bounds(node, width, height) is not None
        for value in node_values(node)
        if value
    )
    return " | ".join(list(labels)[:limit])


_SCREEN_SIZE: tuple[int, int] | None = None


def screen_size() -> tuple[int, int]:
    global _SCREEN_SIZE
    if _SCREEN_SIZE is None:
        output = run_adb("shell", "wm", "size")
        match = re.search(r"(\d+)x(\d+)", output)
        _SCREEN_SIZE = (int(match.group(1)), int(match.group(2))) if match else (1080, 2400)
    return _SCREEN_SIZE


def scroll_page(up: bool = False) -> None:
    width, height = screen_size()
    x = width // 2
    start_y, end_y = (0.32, 0.82) if up else (0.82, 0.32)
    run_adb(
        "shell", "input", "swipe", str(x), str(int(height * start_y)),
        str(x), str(int(height * end_y)), "350"
    )
    time.sleep(0.35)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    scroll = parser.add_mutually_exclusive_group()
    scroll.add_argument("--scroll", action="store_true", help="scroll down until the label is visible")
    scroll.add_argument("--scroll-up", action="store_true", help="scroll up until the label is visible")
    parser.add_argument("--assert", dest="assert_only", action="store_true", help="check visibility without tapping")
    parser.add_argument("--exact", action="store_true", help="require an exact label match instead of a substring")
    parser.add_argument("label", nargs="+", help="visible text or content description")
    args = parser.parse_args()
    label = " ".join(args.label)
    should_scroll = args.scroll or args.scroll_up
    attempts = 7 if should_scroll else 1

    last_xml = ""
    for attempt in range(attempts):
        try:
            xml = dump_xml()
            last_xml = xml
            if args.assert_only:
                if is_visible(xml, label, exact_only=args.exact):
                    print(f"VISIBLE: {label}")
                    return 0
            else:
                position = locate(xml, label, exact_only=args.exact)
                if position is not None:
                    run_adb("shell", "input", "tap", str(position[0]), str(position[1]))
                    print(f"tapped {label!r} at {position}")
                    return 0
        except Exception as error:  # include adb/parse failures in the run log
            print(f"UI automation error for {label!r}: {error}", file=sys.stderr)
            return 2

        # Never swipe a static screen/dialog: on Android that can trigger the system Home gesture.
        # Only keep searching when the current accessibility tree exposes a real scroll container.
        if attempt + 1 < attempts and should_scroll and is_scrollable(last_xml):
            scroll_page(up=args.scroll_up)
        else:
            break

    action = "not visible" if args.assert_only else "not found"
    print(f"{action}: {label}", file=sys.stderr)
    if last_xml:
        print(f"visible UI labels: {visible_labels(last_xml)}", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
