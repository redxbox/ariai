#!/usr/bin/env bash
# Launches AriAI on the emulator, walks through the main screens, takes screenshots,
# and fails if the app crashes. Output goes to smoke-out/ (uploaded as an artifact).
set -u
PKG=com.ariai.app
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=smoke-out
mkdir -p "$OUT"
FAILED=0
shot() {
  adb exec-out screencap -p > "$OUT/$1.png"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1
  # Put the visible texts in the run log as annotations, so results can be read without downloading artifacts.
  python3 .github/scripts/screen_texts.py "$OUT/$1.xml" "$1"
}
step() { echo "== $1"; }

adb wait-for-device
adb shell input keyevent 82 || true
adb logcat -c
adb install -r "$APK" | tee "$OUT/install.txt"
grep -q Success "$OUT/install.txt" || { echo "install failed"; exit 1; }

step "launch"
adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null
sleep 10
shot 01-launch

step "open attachment menu"
python3 .github/scripts/ui_tap.py "Add to message" || FAILED=1
sleep 2; shot 02-attachments
adb shell input keyevent 4; sleep 1

step "open web search sheet"
python3 .github/scripts/ui_tap.py "Web search" || FAILED=1
sleep 2; shot 03-search
adb shell input keyevent 4; sleep 1

step "open side menu"
python3 .github/scripts/ui_tap.py "Open menu" || FAILED=1
sleep 2; shot 04-menu
adb shell input keyevent 4; sleep 1

step "crash check"
adb logcat -d > "$OUT/logcat.txt"
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then
  grep -A30 "FATAL EXCEPTION" "$OUT/logcat.txt" > "$OUT/crash.txt"
  echo "CRASH FOUND"; FAILED=1
fi
adb shell pidof $PKG >/dev/null || { echo "app is not running at the end"; FAILED=1; }

echo "result failed=$FAILED"
exit $FAILED
