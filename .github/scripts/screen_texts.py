"""Prints the visible texts of a uiautomator dump as a GitHub notice annotation."""
import sys
import xml.etree.ElementTree as ET

path, name = sys.argv[1], sys.argv[2]
try:
    root = ET.parse(path).getroot()
except Exception as e:  # missing or broken dump
    print(f"::warning title={name}::no UI dump ({e})")
    sys.exit(0)
texts = []
for node in root.iter("node"):
    for value in (node.get("text", ""), node.get("content-desc", "")):
        value = " ".join(value.split())
        if value and value not in texts:
            texts.append(value)
level = "warning" if name in {"13-photo-picker", "14-document-picker", "14-document-selected", "15-camera-intent", "15-camera-unavailable"} else "notice"
print(f"::{level} title={name}::" + " | ".join(texts)[:1500])
