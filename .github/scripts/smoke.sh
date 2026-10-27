#!/usr/bin/env bash
# Walks every user-reachable screen and the major chat/settings/provider sheets on an Android emulator.
# External provider calls are skipped by default. A one-shot commit-message gate opts into an isolated
# RadoxAI-compatible model-list/repository-review test; its key is never written to screenshots, UI dumps or logs.
set -euo pipefail
# Keep the Actions secret out of ordinary adb/logging subprocesses; pass it only to the
# isolated live-test helper when the one-shot commit-message gate is enabled.
LIVE_API_KEY="${ARIAI_TEST_API_KEY:-}"
unset ARIAI_TEST_API_KEY
PKG=com.ariai.app
APK=app/build/outputs/apk/debug/app-debug.apk
OUT=smoke-out
mkdir -p "$OUT"
exec > >(tee -a "$OUT/walkthrough.log") 2>&1
FAILED=0
STEP_NO=0
if [[ "${ARIAI_LIVE_API_TEST:-false}" == "true" ]]; then
  echo "::notice title=Smoke bootstrap::Isolated live-research path selected; the normal UI walkthrough remains covered by ordinary emulator runs."
else
  echo "::notice title=Smoke bootstrap::Full providerless UI walkthrough selected."
fi

# Keep ADB failures bounded so a wedged emulator command cannot stall the workflow indefinitely.
adb() {
  local seconds=90
  if [[ "${1:-}" == "wait-for-device" ]]; then seconds=120; fi
  local status=0
  timeout --signal=TERM --kill-after=10s "${seconds}s" adb "$@" || status=$?
  if [[ "$status" -ne 0 ]]; then
    echo "::error title=ADB command failed::adb $* exited with status $status"
  fi
  return "$status"
}

step() {
  STEP_NO=$((STEP_NO + 1))
  echo "::group::[$STEP_NO] $1"
  echo "== $1"
}
end_step() { echo "::endgroup::"; }

expect_text() {
  local label="$1"
  local output
  if ! output="$(python3 .github/scripts/ui_tap.py --assert --scroll "$label" 2>&1)"; then
    echo "$output"
    local detail="${output//$'\n'/ | }"
    echo "::error title=UI assertion failed::${label}; ${detail:0:1200}"
    FAILED=1
    capture_failure "$label"
    return 1
  fi
  echo "$output"
}

expect_absent_text() {
  local label="$1"
  local output
  local status=0
  output="$(python3 .github/scripts/ui_tap.py --assert "$label" 2>&1)" || status=$?
  if [[ "$status" -eq 0 ]]; then
    echo "::error title=Unexpected UI target::$label is present"
    FAILED=1
    capture_failure "$label"
    return 1
  elif [[ "$status" -ne 1 ]]; then
    echo "$output"
    echo "::error title=UI absence check failed::$label; ui_tap.py exited with status $status"
    FAILED=1
    capture_failure "$label"
    return 1
  fi
  echo "Confirmed absent: $label"
}

tap() {
  local label="$1"
  local output
  if ! output="$(python3 .github/scripts/ui_tap.py "$label" 2>&1)"; then
    echo "$output"
    local detail="${output//$'\n'/ | }"
    echo "::error title=UI target missing::${label}; ${detail:0:1200}"
    FAILED=1
    capture_failure "$label"
    return 1
  fi
  echo "$output"
  sleep 0.8
}

tap_exact() {
  local label="$1"
  local output
  if ! output="$(python3 .github/scripts/ui_tap.py --exact "$label" 2>&1)"; then
    echo "$output"
    local detail="${output//$'\n'/ | }"
    echo "::error title=Exact UI target missing::${label}; ${detail:0:1200}"
    FAILED=1
    capture_failure "$label"
    return 1
  fi
  echo "$output"
  sleep 0.8
}

tap_scroll() {
  local label="$1"
  if ! python3 .github/scripts/ui_tap.py --scroll "$label"; then
    echo "::error title=UI target missing::$label"
    FAILED=1
    capture_failure "$label"
    return 1
  fi
  sleep 0.8
}

tap_scroll_up() {
  local label="$1"
  if ! python3 .github/scripts/ui_tap.py --scroll-up "$label"; then
    echo "::error title=UI target missing::$label"
    FAILED=1
    capture_failure "$label"
    return 1
  fi
  sleep 0.8
}

back() {
  adb shell input keyevent KEYCODE_BACK >/dev/null
  sleep 1
}

dismiss_external_picker() {
  local label
  for label in Cancel Close; do
    if python3 .github/scripts/ui_tap.py --assert "$label" >/dev/null 2>&1; then
      tap "$label"
      return 0
    fi
  done
  back
}

return_to_chat_after_external() {
  local origin="$1"
  local activity
  if python3 .github/scripts/ui_tap.py --assert "Choose model" >/dev/null 2>&1; then
    return 0
  fi

  activity="$(adb shell dumpsys activity activities 2>/dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity' || true)"
  if [[ "$activity" != *NexusLauncherActivity* ]]; then
    dismiss_external_picker
  fi
  if python3 .github/scripts/ui_tap.py --assert "Choose model" >/dev/null 2>&1; then
    return 0
  fi

  activity="$(adb shell dumpsys activity activities 2>/dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity' || true)"
  if [[ "$activity" == *NexusLauncherActivity* ]]; then
    echo "::warning title=External intent return::$origin did not return directly to AriAI; reopening the app to continue the walkthrough. Foreground: ${activity:0:400}"
    adb shell am start -W -n "$PKG/.MainActivity" | tee "$OUT/relaunch-after-${origin}.txt"
    wait_for_text "Choose model" 30
  else
    wait_for_text "Choose model" 20
  fi
}

hide_keyboard() {
  adb shell input keyevent KEYCODE_BACK >/dev/null
  sleep 0.5
}

type_text() {
  adb shell input text "$1"
  sleep 0.4
}

shot() {
  local name="$1"
  adb exec-out screencap -p > "$OUT/$name.png"
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/ui.xml "$OUT/$name.xml" >/dev/null 2>&1 || true
  if [ -f "$OUT/$name.xml" ]; then
    python3 .github/scripts/screen_texts.py "$OUT/$name.xml" "$name"
  else
    echo "::warning title=$name::UI hierarchy could not be captured"
  fi
}

capture_failure() {
  local label="$1"
  local safe
  safe="$(printf '%s' "$label" | tr -cs '[:alnum:]_-' '_')"
  shot "failure-${STEP_NO}-${safe:0:40}"
  adb logcat -d -t 500 > "$OUT/failure-${STEP_NO}-logcat.txt" || true
  local relevant activity
  relevant="$(grep -E 'FATAL EXCEPTION|ANR in|isn.t responding|com\.ariai\.app' "$OUT/failure-${STEP_NO}-logcat.txt" | tail -25 | tr '\n' ' ' | cut -c1-1200 || true)"
  activity="$(adb shell dumpsys activity activities 2>/dev/null | grep -m1 -E 'topResumedActivity|mResumedActivity' | cut -c1-500 || true)"
  if [[ -n "$relevant" ]]; then echo "::error title=Failure logcat::$relevant"; fi
  if [[ -n "$activity" ]]; then echo "::error title=Foreground activity after failure::$activity"; fi
}

wait_for_text() {
  local label="$1"
  local timeout_seconds="${2:-90}"
  local deadline=$((SECONDS + timeout_seconds))
  while (( SECONDS < deadline )); do
    if python3 .github/scripts/ui_tap.py --assert "$label" >/dev/null 2>&1; then
      echo "VISIBLE after startup wait: $label"
      return 0
    fi
    if python3 .github/scripts/ui_tap.py --assert "Pixel Launcher isn't responding" >/dev/null 2>&1; then
      echo "::warning title=Emulator startup::Pixel Launcher stalled; asking Android to wait."
      python3 .github/scripts/ui_tap.py "Wait" >/dev/null 2>&1 || true
      sleep 5
    else
      sleep 2
    fi
  done
  local detail
  detail="$(python3 .github/scripts/ui_tap.py --assert "$label" 2>&1 || true)"
  detail="${detail//$'\n'/ | }"
  echo "::error title=UI startup timeout::${label} was not visible within ${timeout_seconds}s; ${detail:0:1200}"
  capture_failure "$label"
  return 1
}

final_process_check() {
  step "Final process and crash check"
  adb logcat -d > "$OUT/logcat.txt"
  if grep -A12 "FATAL EXCEPTION" "$OUT/logcat.txt" | grep -q "$PKG"; then
    grep -A30 "FATAL EXCEPTION" "$OUT/logcat.txt" > "$OUT/crash.txt"
    crash_detail="$(tr '\n' ' ' < "$OUT/crash.txt" | cut -c1-1500)"
    echo "::error title=Crash detected::$crash_detail"
    FAILED=1
  fi
  if ! adb shell pidof "$PKG" >/dev/null; then
    echo "::error title=App stopped::AriAI is not running at the end of the walkthrough."
    FAILED=1
  fi
  printf 'result failed=%s steps=%s\n' "$FAILED" "$STEP_NO"
  end_step
  [[ "$FAILED" -eq 0 ]]
}

echo "::notice title=Smoke bootstrap::Waiting for emulator and installing the debug APK."
adb wait-for-device
adb shell pm uninstall "$PKG" >/dev/null 2>&1 || true
adb logcat -c
adb install -r "$APK" | tee "$OUT/install.txt"
grep -q Success "$OUT/install.txt" || { echo "install failed"; exit 1; }
echo "::notice title=Smoke bootstrap::Debug APK installed; proceeding with the selected test path."
adb shell am force-stop "$PKG"

# Keep the one-shot API credential test isolated from the long, providerless UI walkthrough.
# The normal CI path still exercises every UI section; this branch exists only on the explicit marker.
if [[ "${ARIAI_LIVE_API_TEST:-false}" == "true" ]]; then
  step "One-shot RadoxAI GitHub research test"
  live_status=0
  ARIAI_TEST_API_KEY="$LIVE_API_KEY" bash .github/scripts/live-provider-smoke.sh || live_status=$?
  unset LIVE_API_KEY
  end_step
  if (( live_status != 0 )); then exit "$live_status"; fi
  final_process_check
  exit 0
fi

adb shell am start -W -n "$PKG/.MainActivity" | tee "$OUT/launch.txt"
wait_for_text "How can I help you?" 90
shot 00-app-home

step "Chat home and model picker"
expect_text "How can I help you?"
expect_text "Send"
shot 01-chat-home
# The compact model dropdown is centered directly below the chat top bar, clear of the composer.
tap "Choose model"
wait_for_text "No models available" 30
shot 02-model-picker
back
end_step

step "Providerless send, thinking controls, and rename dialog"
tap "Message AriAI"
type_text "EmulatorSmokeTestWithoutProvider"
hide_keyboard
tap "Send"
expect_text "No provider configured"
shot 02a-send-without-provider
tap "Off"
expect_text "Thinking depth"
expect_text "Not every model supports this"
shot 03-thinking-sheet
back
tap "More"
tap "Rename chat"
expect_text "Save"
expect_text "Cancel"
shot 04-rename-dialog
tap "Cancel"
end_step

step "Chat overflow actions"
tap "More"
expect_text "Copy conversation"
expect_text "Image chat"
expect_text "Compress history"
shot 05-chat-overflow
tap "Copy conversation"
shot 06-copy-conversation
tap "More"
tap "Image chat"
# The missing-model snackbar is transient; capture the chat state without relying on its timing.
shot 07-image-chat-without-provider
# Without a provider, compression must not make a request or alter the conversation.
tap "More"
tap "Compress history"
shot 08-compress-no-provider
end_step

step "Web search chooser and inline provider setup"
tap "Web search"
expect_text "Google Gemini"
expect_text "Anthropic Claude"
expect_absent_text "Local Search"
expect_absent_text "Google Search"
expect_absent_text "Brave Search"
expect_absent_text "Tavily"
expect_absent_text "Exa"
shot 09-web-search-sheet
tap "Configure Gemini web search"
expect_text "Provider API key"
expect_text "API key"
expect_text "No separate search key is needed"
shot 10-inline-gemini-search-settings
tap "Cancel"
back
end_step

step "Attachment picker and generation modes"
tap "Add to message"
expect_text "Photo"
expect_text "Camera"
expect_text "Text file"
expect_text "Image"
expect_text "Video"
expect_text "Audio"
expect_text "Compress history"
expect_text "Extensions"
shot 12-attachment-sheet

# Exercise the Android photo picker, document picker and camera intent; the document picker uses
# the UI hierarchy file created by this smoke run, so no user data is accessed.
# The preceding checks leave the long attachment sheet scrolled near its bottom.
tap_scroll_up "Photo"
sleep 2
shot 13-photo-picker
dismiss_external_picker
wait_for_text "Choose model" 20
tap "Add to message"
tap "Text file"
sleep 2
shot 14-document-picker
tap "ui.xml"
if python3 .github/scripts/ui_tap.py --assert --exact "Open" >/dev/null 2>&1; then
  shot 14-document-selected
  tap_exact "Open"
else
  shot 14-document-selected
fi
wait_for_text "Choose model" 20
expect_text "ui.xml"
tap "Remove attachment"
camera_handler="$(adb shell cmd package resolve-activity --brief -a android.media.action.IMAGE_CAPTURE 2>&1 | tail -n1 || true)"
if [[ -z "$camera_handler" || "$camera_handler" == *"No activity found"* ]]; then
  echo "::warning title=Camera intent unavailable::No IMAGE_CAPTURE activity is installed in this emulator; checking the Camera tile only. Resolver: ${camera_handler:0:400}"
  tap "Add to message"
  expect_text "Camera"
  shot 15-camera-unavailable
  back
  wait_for_text "Choose model" 20
else
  tap "Add to message"
  tap "Camera"
  sleep 3
  shot 15-camera-intent
  return_to_chat_after_external "Camera"
fi

# With no configured provider, these modes should fail safely with an explanatory notice.
for mode in Image Video Audio; do
  tap "Add to message"
  tap_scroll "$mode"
  sleep 1
  shot "16-generate-$mode-lacking-provider"
done
end_step

step "MCP extension screen and add/remove flow"
tap "Add to message"
tap_scroll "Extensions"
wait_for_text "Model Context Protocol" 20
expect_text "No MCP servers yet"
shot 19-mcp-empty

wait_for_text "Add server" 10
tap_exact "Add server"
expect_text "Add MCP server"
shot 20-mcp-add-form
tap "Name"
type_text "ArenaSmokeServer"
hide_keyboard
tap "Server URL"
type_text "https://example.org/mcp"
hide_keyboard
tap "SSE"
shot 21-mcp-sse-selected
tap "Save"
expect_text "ArenaSmokeServer"
expect_text "not connected"
shot 22-mcp-server-added
tap "Remove server"
expect_text "No MCP servers yet"
shot 23-mcp-server-removed
back
expect_text "Choose model"
end_step

step "Voice input permission surface"
tap "Voice input"
sleep 2
shot 24-voice-input
# Dismiss a possible Android permission/recognizer surface without granting microphone access.
back
expect_text "Choose model"
end_step

step "Navigation drawer and provider screens"
tap "Open menu"
expect_text "Search conversations"
expect_text "Providers"
expect_text "MCP"
expect_text "Settings"
expect_text "About"
expect_text "Workspace"
shot 25-navigation-drawer
tap "Workspace"
expect_text "No project files yet."
expect_text "Console"
tap "Generate with AriAI"
expect_text "Generate code"
expect_text "Uses the selected default AI provider and model."
tap "Describe what to build or change"
type_text "Create a small static HTML page"
hide_keyboard
tap "Generate"
expect_text "No provider configured"
shot 25-workspace-generate-without-provider
expect_text "Cancel"
tap "Cancel"
shot 25-workspace-empty
back
expect_text "Choose model"
tap "Open menu"
tap "About"
expect_text "AriAI v1.0 - Personal AI"
shot 25-drawer-about

tap "Open menu"
tap "Search conversations"
type_text "no-match-arena-smoke"
hide_keyboard
expect_text "No matches"
shot 25-drawer-search-no-matches
tap "MCP"
expect_text "Model Context Protocol"
shot 25-mcp-from-drawer
back
tap "Open menu"
tap "Providers"
expect_text "No providers yet"
expect_text "Tap the star icon for recommended providers"
shot 26-providers-empty

tap "Import provider"
expect_text "Import provider"
expect_text "Paste a provider as JSON"
shot 27-import-provider-dialog
tap "Cancel"

tap "Recommended providers"
expect_text "Recommended providers"
expect_text "OpenAI"
shot 28-recommended-providers
tap_scroll "Gemini"
expect_text "Add provider"
expect_text "Connection"
shot 29-add-gemini-provider

# Exercise connection presets without saving a provider or transmitting credentials.
for provider in OpenAI Anthropic Ollama Custom Gemini; do
  tap "$provider"
done
expect_text "Base URL"
expect_text "API key"
shot 30-provider-presets

tap "Models"
expect_text "No models yet"
shot 31-provider-models-empty
tap_scroll "Add model"
expect_text "Model ID"
expect_text "Built-in Tools"
shot 32-add-model-basic
tap "Model ID"
type_text "smoke-model"
hide_keyboard
tap "Built-in Tools"
expect_text "Google Search grounding"
expect_text "URL context"
shot 33-add-model-built-in-tools
tap "Search"
tap "URL context"
tap "Advanced"
expect_text "Extra headers for this model only"
shot 34-add-model-advanced
tap "Basic"
tap_exact "Add"
wait_for_text "Connection" 20
expect_text "smoke-model"
shot 35-unsaved-model-added

tap_exact "Connection"
tap_scroll "Advanced"
expect_text "Custom headers"
shot 36-provider-advanced
# Add a blank header row to check that editor, without saving the provider.
tap_scroll "Add header"
expect_text "Header"
expect_text "Value"
shot 37-provider-header-editor
expect_text "Custom body (JSON)"
back
expect_text "No providers yet"
# Also verify the direct + route to the provider editor.
tap "Add provider"
expect_text "Add provider"
expect_text "Save"
shot 38-add-provider-direct
# The provider editor now presents connection checking as the labeled Test action.
tap_exact "Test"
wait_for_text "No API key entered" 10
expect_text "Close"
shot 38a-provider-connection-dialog
tap "Close"
back
back
expect_text "Choose model"
end_step

step "Search-service settings for Gemini and Anthropic"
tap "Open menu"
tap "Search"
expect_text "Search service"
expect_text "Built-in web search"
expect_text "Google Gemini"
expect_text "Anthropic Claude"
expect_absent_text "Google Search"
expect_absent_text "Brave Search"
expect_absent_text "Tavily"
expect_absent_text "Exa"
shot 39-search-service

tap "Configure Anthropic web search"
expect_text "Provider API key"
expect_text "No separate search key is needed"
shot 40-inline-anthropic-search-settings
tap "Cancel"
back
expect_text "Choose model"
end_step

step "Settings, appearance, preferences, and default model"
tap "Open menu"
tap "Settings"
expect_text "General"
shot 42-settings

tap_scroll "Dark"
shot 43-settings-dark-theme
tap_scroll "System"
shot 44-settings-system-theme

tap_scroll "Preferences"
expect_text "Stream replies"
expect_text "Default thinking depth"
shot 47-preferences
# Verify preference chips respond, then restore the defaults for this test run.
tap_scroll "High"
shot 48-preferences-high-reasoning
tap_scroll "Off"
tap_scroll "Large"
shot 49-preferences-large-text
tap "Medium"
expect_text "Preview: the quick brown fox"
back

expect_text "Language"
tap "Language"
expect_text "زبان"
shot 45-settings-persian-rtl
# Toggle back to English so the remaining test selectors are stable.
tap "زبان"
expect_text "Settings"
shot 46-settings-english

tap_scroll "Default model"
expect_text "New chats start with this model"
expect_text "No models yet"
shot 50-default-model-empty
back

tap_scroll "Search service"
tap "Search service"
expect_text "Built-in web search"
expect_text "Google Gemini"
expect_text "Anthropic Claude"
expect_absent_text "Local Search"
shot 51-search-service-from-settings
back

tap_scroll "MCP servers"
expect_text "Model Context Protocol"
shot 52-mcp-from-settings
back
end_step

step "Storage, export chooser, guarded delete dialog, and About"
tap_scroll "Data & storage"
expect_text "Stored locally on this device"
shot 53-storage

tap_scroll "Export chat list"
sleep 2
shot 54-export-share-sheet
back
# Open the destructive confirmation, capture it, and cancel; no chat data is deleted.
if python3 .github/scripts/ui_tap.py --scroll "Delete all chats"; then
  sleep 0.8
  if python3 .github/scripts/ui_tap.py --assert "Delete all chats?"; then
    shot 53-delete-chats-confirmation
    tap "Cancel"
  else
    echo "::notice title=Storage test::Delete confirmation was unavailable (there were no chats to delete)."
  fi
else
  echo "::notice title=Storage test::Delete all chats was not available on this screen."
fi

back
expect_text "About AriAI"
tap_exact "About AriAI"
wait_for_text "AriAI v1.0 - Personal AI" 8
shot 54-settings-about
back
expect_text "Choose model"

# Start a clean conversation after the intentional missing-provider error at the beginning.
tap "Open menu"
tap "New chat"
expect_text "How can I help you?"
shot 56-new-chat-from-drawer
end_step

unset LIVE_API_KEY
echo "Live provider test skipped (no one-shot commit-message opt-in)."
final_process_check
