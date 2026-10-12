"""Finds a UI element by its text or content description in a uiautomator dump and taps it."""
import re
import subprocess
import sys
import xml.etree.ElementTree as ET

DUMP = "/sdcard/ui.xml"


def dump():
    subprocess.run(["adb", "shell", "uiautomator", "dump", DUMP], check=False, capture_output=True)
    xml = subprocess.run(["adb", "shell", "cat", DUMP], capture_output=True, text=True).stdout
    return xml


def find(label, xml):
    root = ET.fromstring(xml)
    for node in root.iter("node"):
        text = node.get("text", "")
        desc = node.get("content-desc", "")
        if label.lower() in text.lower() or label.lower() in desc.lower():
            m = re.findall(r"\d+", node.get("bounds", ""))
            if len(m) == 4:
                x1, y1, x2, y2 = map(int, m)
                return (x1 + x2) // 2, (y1 + y2) // 2
    return None


def tap(label):
    pos = find(label, dump())
    if pos is None:
        print(f"NOT FOUND: {label}")
        return False
    subprocess.run(["adb", "shell", "input", "tap", str(pos[0]), str(pos[1])], check=False)
    print(f"tapped {label} at {pos}")
    return True


if __name__ == "__main__":
    ok = tap(sys.argv[1])
    sys.exit(0 if ok else 1)
