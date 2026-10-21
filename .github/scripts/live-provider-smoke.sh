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
# Match the app's apiKey.trim(): tolerate accidental whitespace only around the pasted token.
API_KEY="${API_KEY#"${API_KEY%%[![:space:]]*}"}"
API_KEY="${API_KEY%"${API_KEY##*[![:space:]]}"}"
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

# Read only the latest assistant message from the disposable debug app database. Database snapshots
# stay under /tmp, are deleted immediately, and never include provider rows in logs or artifacts.
extract_latest_assistant() (
  set -euo pipefail
  local snapshot
  snapshot="$(mktemp -d /tmp/ariai-research-db.XXXXXX)"
  trap 'rm -rf "$snapshot"' EXIT
  for suffix in "" "-wal" "-shm"; do
    if ! timeout --signal=TERM --kill-after=5s 35s adb exec-out run-as "$PKG" cat "databases/ariai_db$suffix" \
      > "$snapshot/ariai_db$suffix" 2>/dev/null; then
      rm -f "$snapshot/ariai_db$suffix"
    fi
  done
  python3 - "$snapshot/ariai_db" <<'PY'
import sqlite3
import sys

try:
    connection = sqlite3.connect(f"file:{sys.argv[1]}?mode=ro", uri=True, timeout=5)
    row = connection.execute(
        "SELECT content FROM messages WHERE role = 'ASSISTANT' "
        "AND TRIM(content) <> '' ORDER BY timestamp DESC LIMIT 1"
    ).fetchone()
    connection.close()
    if not row or not row[0].strip():
        raise SystemExit(1)
    sys.stdout.write(row[0])
except Exception:
    raise SystemExit(1)
PY
)

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
# The app was cleared before setup, so this chat is already fresh; keep the selected model on it.
live_tap "Message AriAI"
research_prompt='Review the public GitHub repository https://github.com/redxbox/ariai on its default branch using the GitHub repository tools. First list the files, then actually read the app entry point and Gradle build configuration. In Persian and in at most 150 words, explain what the app currently does, two concrete limitations or risks, and two practical recommendations. Cite exact file paths and code identifiers for your claims. Finally, quote the exact Persian UI string assigned in MainActivity.onCreate. If you cannot read the files from GitHub, say so plainly and do not guess.'
enter_text "$research_prompt"
adb_quiet shell input keyevent KEYCODE_BACK
live_tap "Send"

# Wait for the final assistant message. The app writes completed (or error) replies to Room only
# after the agent/tool loop ends; no provider row is selected or printed from the snapshot.
research_deadline=$((SECONDS + 600))
research_response=""
while (( SECONDS < research_deadline )); do
  if ! live_assert "Stop"; then
    candidate="$(extract_latest_assistant 2>/dev/null || true)"
    if [[ -n "$candidate" ]]; then
      research_response="$candidate"
      break
    fi
  fi
  sleep 3
done
if [[ -z "$research_response" ]]; then
  echo "::error title=Live provider test::No completed research reply was saved within 10 minutes."
  exit 1
fi

# Stop the disposable app, take one final messages-only snapshot, and publish just the model's
# repository review (never the provider table or API key) for the user to inspect in Actions.
adb_quiet shell am force-stop "$PKG"
research_response="$(extract_latest_assistant 2>/dev/null)" || {
  echo "::error title=Live provider test::The completed research reply could not be read from the disposable app database."
  exit 1
}
research_response="$(python3 -c 'import sys; text=sys.stdin.read(); print(text[:6000])' <<<"$research_response")"
mkdir -p smoke-out
printf '%s\n' "$research_response" > smoke-out/live-research-response.txt
escaped_response="$(python3 -c 'import sys; text=sys.stdin.read(); print(text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A"))' <<<"$research_response")"
printf '::notice title=RadoxAI GitHub review::%s\n' "$escaped_response"

if [[ "$research_response" == *"app/src/main/java/com/ariai/app/MainActivity.kt"* && \
      "$research_response" == *"app/build.gradle.kts"* && \
      "$research_response" == *"اپلیکیشن خالص AriAi با موفقیت اجرا شد"* ]]; then
  echo "Repository-specific evidence confirmed: source paths and the exact UI string were returned."
else
  echo "::warning title=Live GitHub research::The model replied, but did not include every requested repository fingerprint; inspect the attached response before treating its findings as verified."
fi

# Clear the temporary provider/key before the main smoke script's final process check.
if ! cleanup_live_test; then
  exit 1
fi
trap - EXIT
adb_quiet shell am start -W -n "$PKG/.MainActivity"
live_wait_for_text "How can I help you?" 45
printf 'Live provider smoke passed; emulator provider data was cleared.\n'
