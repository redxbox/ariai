#!/usr/bin/env bash
# One-shot, opt-in RadoxAI-compatible provider test. The API key is supplied only through the
# Actions environment, typed directly into the emulator, and never written to logs/artifacts.
set -euo pipefail
set +x

PKG=com.ariai.app
APK=app/build/outputs/apk/debug/app-debug.apk
BASE_URL="${ARIAI_TEST_BASE_URL:-https://radoxai.com/v1}"
API_KEY="${ARIAI_TEST_API_KEY:-}"
unset ARIAI_TEST_API_KEY
CLEANUP_DONE=false

if [[ -z "$API_KEY" ]]; then
  echo "::error title=Live provider test::The Actions secret ARIAI_TEST_API_KEY is empty or unavailable; its value was not read or logged."
  exit 1
fi
if [[ "$API_KEY" =~ [[:space:]] ]]; then
  unset API_KEY
  echo "::error title=Live provider test::ARIAI_TEST_API_KEY must be a single-line key; the value was not logged."
  exit 1
fi
if [[ -z "$BASE_URL" ]]; then
  unset API_KEY
  echo "::error title=Live provider test::ARIAI_TEST_BASE_URL is empty."
  exit 1
fi

adb_quiet() {
  timeout --signal=TERM --kill-after=5s 35s adb "$@" >/dev/null 2>&1
}

# Keep all UI automation output suppressed. On error, ui_tap.py normally prints every visible
# label, which is inappropriate while a password field has been populated.
live_tap() {
  local label="$1"
  if ! python3 .github/scripts/ui_tap.py "$label" >/dev/null 2>&1; then
    echo "::error title=Live provider test::Required app control was not available: $label. UI details were suppressed."
    return 1
  fi
  sleep 0.7
}

live_tap_scrolling() {
  local label="$1"
  if python3 .github/scripts/ui_tap.py "$label" >/dev/null 2>&1; then
    sleep 0.7
    return 0
  fi
  if ! python3 .github/scripts/ui_tap.py --scroll "$label" >/dev/null 2>&1; then
    echo "::error title=Live provider test::Required app control was not available: $label. UI details were suppressed."
    return 1
  fi
  sleep 0.7
}

live_assert() {
  local label="$1"
  python3 .github/scripts/ui_tap.py --assert "$label" >/dev/null 2>&1
}

live_wait_for_text() {
  local label="$1"
  local timeout_seconds="${2:-60}"
  local deadline=$((SECONDS + timeout_seconds))
  while (( SECONDS < deadline )); do
    if live_assert "$label"; then
      echo "Reached live-test UI state: $label"
      return 0
    fi
    sleep 2
  done
  echo "::error title=Live provider test::Timed out waiting for '$label'; screen contents were not logged."
  return 1
}

enter_text() {
  local text="$1"
  local escaped="${text// /%s}"
  if ! adb_quiet shell input text "$escaped"; then
    echo "::error title=Live provider test::Could not enter a non-secret test value."
    return 1
  fi
  sleep 0.4
}

enter_secret() {
  local secret="$1"
  local escaped="${secret// /%s}"
  # Do not use the main smoke script's logging adb wrapper and never print this command or its args.
  if ! timeout --signal=TERM --kill-after=5s 35s adb shell input text "$escaped" >/dev/null 2>&1; then
    unset secret escaped
    echo "::error title=Live provider test::Could not enter the Actions API key; its value was suppressed."
    return 1
  fi
  unset secret escaped
  sleep 0.4
}

cleanup_live_test() {
  [[ "$CLEANUP_DONE" == "true" ]] && return 0
  local failed=0
  adb_quiet shell am force-stop "$PKG" || failed=1
  # Uninstalling also removes the app-scoped Android Keystore alias used to encrypt provider data.
  if adb_quiet shell pm uninstall "$PKG"; then
    adb_quiet install -r "$APK" || failed=1
  else
    adb_quiet shell pm clear "$PKG" || failed=1
    failed=1
  fi
  # uiautomator writes here while interacting with the password field; remove its temporary dump.
  adb_quiet shell rm -f /sdcard/ui.xml /sdcard/live-provider.xml || failed=1
  CLEANUP_DONE=true
  if (( failed )); then
    echo "::error title=Live provider cleanup::Could not confirm the emulator app data and UI dump were cleared."
    return 1
  fi
}
trap 'cleanup_live_test || true' EXIT

printf 'Testing model discovery at %s (credential value hidden).\n' "$BASE_URL"

# Keep the emulator clean if any UI/API step fails.
adb_quiet shell am force-stop "$PKG"
adb_quiet shell pm clear "$PKG"
adb_quiet shell am start -W -n "$PKG/.MainActivity"
live_wait_for_text "How can I help you?" 60

live_tap "Open menu"
live_tap "Providers"
live_tap "Add provider"
live_wait_for_text "Add provider" 30
live_tap "Custom"
live_tap "Base URL"
enter_text "$BASE_URL"
adb_quiet shell input keyevent KEYCODE_BACK
live_tap "API key"
enter_secret "$API_KEY"
unset API_KEY
adb_quiet shell input keyevent KEYCODE_BACK
live_tap "Fetch models"

if ! live_wait_for_text "Models are up to date" 170; then
  # The app only exposes an HTTP status here; report that code and suppress all other UI data.
  status_code="$(python3 - <<'PY'
import re
import subprocess
import xml.etree.ElementTree as ET

try:
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/live-provider.xml"],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)
    xml = subprocess.run(["adb", "shell", "cat", "/sdcard/live-provider.xml"],
                         check=True, capture_output=True, text=True, timeout=30).stdout
    for node in ET.fromstring(xml).iter("node"):
        match = re.search(r"Could not load models: HTTP (\d{3}) from ", node.get("text", ""))
        if match:
            print(match.group(1))
            break
except Exception:
    pass
PY
)"
  if [[ "$status_code" =~ ^[0-9]{3}$ ]]; then
    echo "::error title=Live provider test::Model discovery returned HTTP $status_code from the configured endpoint."
  else
    echo "::error title=Live provider test::Model discovery failed or returned no selectable models; response details were suppressed."
  fi
  exit 1
fi

# Save the discovered provider into the disposable emulator so the real chat flow exercises
# the app's encrypted provider storage and its normal request path.
live_tap_scrolling "Save"
live_wait_for_text "Providers" 45
live_wait_for_text "Custom" 45
adb_quiet shell input keyevent KEYCODE_BACK
live_wait_for_text "How can I help you?" 45
live_tap "Open models"
live_wait_for_text "Search models" 30

# The compatible /models endpoint populates displayName with the model ID. The first model is
# already visible in the provider section; read only its public label, never the credential field.
model_label="$(python3 - <<'PY'
import subprocess
import sys
import xml.etree.ElementTree as ET

try:
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/live-provider.xml"],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=30)
    xml = subprocess.run(["adb", "shell", "cat", "/sdcard/live-provider.xml"],
                         check=True, capture_output=True, text=True, timeout=30).stdout
    labels = [node.get("text", "").strip() for node in ET.fromstring(xml).iter("node")]
    start = next(i for i, label in enumerate(labels) if label == "Custom")
    ignored = {"All", "Custom", "Search models", "Collapse", "Expand", "Vision", "Tools", "Image"}
    for label in labels[start + 1:]:
        if label and label not in ignored and not label.isdigit():
            print(label)
            sys.exit(0)
except Exception:
    pass
sys.exit(1)
PY
)" || {
  echo "::error title=Live provider test::Models were discovered, but no selectable model label was available."
  exit 1
}
if [[ -z "$model_label" ]]; then
  echo "::error title=Live provider test::Models were discovered, but no selectable model label was available."
  exit 1
fi
printf 'Discovered model ID: %s\n' "$model_label"
live_tap "$model_label"
live_wait_for_text "Message AriAI" 30
live_tap "Message AriAI"
enter_text "What is the capital of Greece? Reply with the city name only."
adb_quiet shell input keyevent KEYCODE_BACK
live_tap "Send"

# Check for a real assistant answer, while failing early on common provider errors without
# dumping the message or any other UI content into the workflow log/artifact.
reply_deadline=$((SECONDS + 180))
while (( SECONDS < reply_deadline )); do
  if live_assert "Athens"; then
    echo "Live chat completion succeeded (expected answer received)."
    break
  fi
  if live_assert "Invalid API key"; then
    echo "::error title=Live provider test::The provider rejected the configured API key (HTTP 401)."
    exit 1
  fi
  if live_assert "Rate limit exceeded"; then
    echo "::error title=Live provider test::The provider rate-limited the completion request (HTTP 429)."
    exit 1
  fi
  if live_assert "No internet connection"; then
    echo "::error title=Live provider test::The emulator could not reach the provider endpoint."
    exit 1
  fi
  if live_assert "Please check your API key and provider settings."; then
    echo "::error title=Live provider test::The app reported a provider request error; response details were suppressed."
    exit 1
  fi
  sleep 2
done
if ! live_assert "Athens"; then
  echo "::error title=Live provider test::No expected assistant reply arrived within 180 seconds; message details were suppressed."
  exit 1
fi

# Clear the temporary provider/key before the main smoke script's final process check.
if ! cleanup_live_test; then
  exit 1
fi
trap - EXIT
adb_quiet shell am start -W -n "$PKG/.MainActivity"
live_wait_for_text "How can I help you?" 45
printf 'Live provider smoke passed; emulator provider data was cleared.\n'
