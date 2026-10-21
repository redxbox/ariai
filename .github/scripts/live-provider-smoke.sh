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
  local purpose="${2:-non-secret test value}"
  local chunk escaped quoted
  echo "::notice title=Live provider input::Entering $purpose; value suppressed."
  # Long prompts can overflow a single Android `input text` event; send exact ASCII chunks in order.
  # Quote each remote shell argument so punctuation in prompts is typed, not interpreted by /system/bin/sh.
  while [[ -n "$text" ]]; do
    chunk="${text:0:48}"
    text="${text:${#chunk}}"
    escaped="${chunk// /%s}"
    quoted="$(printf '%s' "$escaped" | python3 -c 'import shlex,sys; print(shlex.quote(sys.stdin.read()), end="")')"
    if ! adb_quiet shell "input text $quoted"; then
      echo "::error title=Live provider test::Could not enter $purpose; value suppressed."
      return 1
    fi
    sleep 0.15
  done
  sleep 0.25
}

enter_secret() {
  local secret="$1"
  local escaped="${secret// /%s}"
  local quoted="$(printf '%s' "$escaped" | python3 -c 'import shlex,sys; print(shlex.quote(sys.stdin.read()), end="")')"
  # Never print the command or its args; the API key remains out of logs, artifacts and shell traces.
  if ! timeout --signal=TERM --kill-after=5s 35s adb shell "input text $quoted" >/dev/null 2>&1; then
    unset secret escaped quoted
    echo "::error title=Live provider test::Could not enter the Actions API key; its value was suppressed."
    return 1
  fi
  unset secret escaped quoted
  sleep 0.4
}

select_review_model() {
  local model_id="$1"
  live_tap "Open models"
  live_wait_for_text "Search models" 30
  live_tap "Search models"
  # The picker may retain a previous query within a chat; clear it before typing this exact model ID.
  adb_quiet shell input keyevent 113 29 67 || true
  enter_text "$model_id" "selected model ID"
  adb_quiet shell input keyevent KEYCODE_BACK
  if ! python3 - "$model_id" <<'PY'
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

wanted = sys.argv[1].casefold()
for _ in range(20):
    try:
        subprocess.run(
            ["adb", "shell", "uiautomator", "dump", "/sdcard/live-provider.xml"],
            check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=25
        )
        xml = subprocess.run(
            ["adb", "shell", "cat", "/sdcard/live-provider.xml"],
            check=True, capture_output=True, text=True, timeout=25
        ).stdout
        candidates = []
        for node in ET.fromstring(xml).iter("node"):
            label = node.get("text", "").strip()
            class_name = node.get("class", "").lower()
            if not label or "edittext" in class_name or "autocompletetextview" in class_name:
                continue
            if node.get("visible-to-user", "true").casefold() == "false":
                continue
            if wanted in label.casefold():
                match = re.search(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
                if match:
                    x1, y1, x2, y2 = map(int, match.groups())
                    if x2 > x1 and y2 > y1:
                        candidates.append((node.get("clickable") == "true", x1, y1, x2, y2))
        if candidates:
            candidates.sort(reverse=True)
            _, x1, y1, x2, y2 = candidates[0]
            subprocess.run(
                ["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)],
                check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=20
            )
            sys.exit(0)
    except Exception:
        pass
    time.sleep(1)
sys.exit(1)
PY
  then
    echo "::error title=Live provider test::The selected model ID did not appear as a selectable row; UI details were suppressed."
    return 1
  fi
  live_wait_for_text "Message AriAI" 30
}

# Discover only public model IDs; the credential is passed through the child environment and never printed.
discover_review_models() {
  ARIAI_REVIEW_API_KEY="$API_KEY" python3 - "$BASE_URL" <<'PY'
import json
import os
import re
import sys
import urllib.error
import urllib.request

base = sys.argv[1].rstrip("/")
key = os.environ.get("ARIAI_REVIEW_API_KEY", "")
if not base or not key:
    raise SystemExit(1)
request = urllib.request.Request(
    base + "/models",
    headers={
        "Accept": "application/json",
        "Authorization": "Bearer " + key,
        "User-Agent": "AriAI-Android",
    },
)
try:
    with urllib.request.urlopen(request, timeout=30) as response:
        payload = json.loads(response.read())
except urllib.error.HTTPError as error:
    print(f"Model catalog request returned HTTP {error.code}; credential details suppressed.", file=sys.stderr)
    raise SystemExit(1)
except Exception:
    print("Could not read the model catalog; response details suppressed.", file=sys.stderr)
    raise SystemExit(1)
if not isinstance(payload, dict):
    print("The model catalog had an unexpected response shape.", file=sys.stderr)
    raise SystemExit(1)

items = payload.get("data") or payload.get("models") or []
if not isinstance(items, list):
    raise SystemExit(1)
ranked = []
positive = {
    "opus": 140, "gpt-5": 130, "gpt5": 130, "gemini-3-pro": 130,
    "gemini-2.5-pro": 120, "deepseek-r1": 115, "qwen3-235b": 115,
    "claude-4": 110, "gpt-4.1": 100, "gpt-4.5": 100, "deepseek-v3": 95,
    "gemini-2.0-pro": 90, "gpt-4o": 85, "claude-3.7": 85,
    "claude-3-7": 85, "claude-3.5": 70, "claude-3-5": 70,
    "qwen2.5-72b": 80, "llama-3.3-70b": 75, "mistral-large": 75,
    "405b": 105, "235b": 100, "reasoner": 100, "reasoning": 90,
    "o3-pro": 105, "o3": 95, "o4": 95, "gemini-pro": 90,
    "sonnet": 75, "max": 65, "70b": 65, "72b": 65, "pro": 50, "large": 35,
}
negative = {
    "embedding": 100, "embed": 100, "moderation": 100, "whisper": 90,
    "tts": 90, "audio": 80, "image": 80, "video": 80,
    "mini": 45, "flash": 45, "haiku": 40, "lite": 40, "nano": 50,
    "small": 35, "instant": 30, "fast": 25, "8b": 15, "7b": 20,
}
for position, item in enumerate(items):
    if isinstance(item, str):
        model_id = item
        metadata = {}
    elif isinstance(item, dict):
        model_id = str(item.get("id", ""))
        metadata = item
    else:
        continue
    model_id = model_id.strip()
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._:/-]{0,200}", model_id):
        continue
    low = model_id.lower()
    modalities = metadata.get("output_modalities") or metadata.get("modalities")
    if isinstance(modalities, list) and modalities and not any(str(value).lower() == "text" for value in modalities):
        continue
    score = sum(points for token, points in positive.items() if token in low)
    score -= sum(points for token, points in negative.items() if token in low)
    context = metadata.get("context_length") or metadata.get("context_window") or metadata.get("max_input_tokens") or 0
    try:
        score += min(30, int(context) // 32_000)
    except (TypeError, ValueError):
        pass
    ranked.append((score, -position, model_id))

ranked.sort(reverse=True)
selected = []
seen = set()
for _, _, model_id in ranked:
    if model_id not in seen:
        selected.append(model_id)
        seen.add(model_id)
    if len(selected) == 2:
        break
if not selected:
    print("No text-capable models were listed by the provider.", file=sys.stderr)
    raise SystemExit(1)
for model_id in selected:
    print(model_id)
PY
}

review_models_raw="$(discover_review_models)" || {
  unset API_KEY
  echo "::error title=Live provider test::Could not discover selectable text models; response and credential details were suppressed."
  exit 1
}
REVIEW_MODELS=()
mapfile -t REVIEW_MODELS <<<"$review_models_raw"
expected_review_count=${#REVIEW_MODELS[@]}
if ((expected_review_count < 2)); then
  unset API_KEY
  echo "::error title=Live provider test::At least two selectable text models are required for independent reviews."
  exit 1
fi
safe_selected_models="$(python3 -c 'import re,sys; print(re.sub(r"[^A-Za-z0-9._/-]+", "_", sys.stdin.read()).strip("_"))' <<<"${REVIEW_MODELS[*]}")"
echo "::notice title=RadoxAI model selection::Selected two highest-ranked available text models: $safe_selected_models"
unset review_models_raw

# Read only the latest assistant message from the disposable debug app database. Database snapshots
# stay under /tmp, are deleted immediately, and never include provider rows in logs or artifacts.
extract_latest_assistant() (
  set -euo pipefail
  local cutoff="${1:-0}"
  local snapshot
  snapshot="$(mktemp -d /tmp/ariai-research-db.XXXXXX)"
  trap 'rm -rf "$snapshot"' EXIT
  for suffix in "" "-wal" "-shm"; do
    if ! timeout --signal=TERM --kill-after=5s 35s adb exec-out run-as "$PKG" cat "databases/ariai_db$suffix" \
      > "$snapshot/ariai_db$suffix" 2>/dev/null; then
      rm -f "$snapshot/ariai_db$suffix"
    fi
  done
  python3 - "$snapshot/ariai_db" "$cutoff" <<'PY'
import sqlite3
import sys

try:
    connection = sqlite3.connect(f"file:{sys.argv[1]}?mode=ro", uri=True, timeout=5)
    row = connection.execute(
        "SELECT content FROM messages WHERE role = 'ASSISTANT' "
        "AND TRIM(content) <> '' AND timestamp > ? ORDER BY timestamp DESC, rowid DESC LIMIT 1",
        (int(sys.argv[2]),),
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
live_wait_for_text "Message AriAI" 120

live_tap "Open menu"
live_tap "Providers"
live_tap "Add provider"
live_wait_for_text "Add provider" 30
live_tap "Custom"
live_tap "Base URL"
enter_text "$BASE_URL" "provider base URL"
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
review_prompt='Review only the branch named by the URL below. First call github_list_files. Then read GitHubRepositoryClient.kt, DefaultAgentTools.kt, MainActivity.kt and ChatRepository.kt from the returned tree with github_read_file. You may try AppViewModel.kt once, but if it exceeds the reader limit, skip it and do not guess. In Persian, provide at most three high-impact concrete findings, under 350 words total. For each, cite an exact path and symbol or one short code line, explain impact, give a minimal fix and a regression test. State the exact ref and at least one successfully read path. If a tool fails, say so. Do not infer beyond returned source. Repository URL: https://github.com/redxbox/ariai?ref=arena%2F5f5dd5fd-ariai'
review_repo_url='https://github.com/redxbox/ariai?ref=arena%2F5f5dd5fd-ariai'
mkdir -p smoke-out
completed_review_count=0
verified_review_count=0

for model_index in 0 1; do
  model_id="${REVIEW_MODELS[$model_index]}"
  review_number=$((model_index + 1))
  safe_model_id="$(python3 -c 'import re,sys; print(re.sub(r"[^A-Za-z0-9._/-]+", "_", sys.stdin.read()).strip("_"))' <<<"$model_id" | cut -c1-60)"
  echo "::notice title=Review loop::Starting independent review $review_number/$expected_review_count with model $safe_model_id"
  if (( model_index > 0 )); then
    live_tap "Open menu"
    live_tap "New chat"
    live_wait_for_text "How can I help you?" 30
  fi

  printf 'Starting independent review %d with selected model ID: %s\n' "$review_number" "$model_id"
  select_review_model "$model_id"
  live_tap "Message AriAI"
  enter_text "$review_prompt" "branch review prompt"
  if ! live_assert "$review_repo_url"; then
    echo "::error title=Live GitHub research::The complete feature-branch URL was not visible in the composed request; nothing was sent."
    exit 1
  fi
  adb_quiet shell input keyevent KEYCODE_BACK
  request_started_at="$(date +%s%3N)"
  live_tap "Send"

  # Scope extraction to messages created after this turn starts so a prior model's answer cannot pass.
  research_deadline=$((SECONDS + 600))
  research_response=""
  while (( SECONDS < research_deadline )); do
    if ! live_assert "Stop"; then
      candidate="$(extract_latest_assistant "$request_started_at" 2>/dev/null || true)"
      if [[ -n "$candidate" ]]; then
        research_response="$candidate"
        break
      fi
    fi
    sleep 3
  done
  if [[ -z "$research_response" ]]; then
    echo "::error title=Live provider test::No completed review reply was saved within 10 minutes."
    exit 1
  fi
  research_response="$(extract_latest_assistant "$request_started_at" 2>/dev/null)" || {
    echo "::error title=Live provider test::The completed review reply could not be read from the temporary messages-only snapshot."
    exit 1
  }

  printf '%s\n' "$research_response" > "smoke-out/live-research-review-${review_number}.txt"
  completed_review_count=$((completed_review_count + 1))
  annotation_parts=()
  mapfile -t annotation_parts < <(printf '%s' "$research_response" | python3 -c 'import sys; text=sys.stdin.read().replace("\t", " "); chunks=[text[i:i+1000] for i in range(0, len(text), 1000)] or ["(empty reply)"]; print("\n".join(chunk.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A") for chunk in chunks))')
  for part_index in "${!annotation_parts[@]}"; do
    printf '::notice title=RadoxAI review %d %s part %d/%d::%s\n' \
      "$review_number" "$safe_model_id" "$((part_index + 1))" "${#annotation_parts[@]}" "${annotation_parts[$part_index]}"
  done
  unset annotation_parts

  if [[ "$research_response" == *"arena/5f5dd5fd-ariai"* && \
        ( "$research_response" == *"GitHubRepositoryClient.kt"* ||
          "$research_response" == *"DefaultAgentTools.kt"* ||
          "$research_response" == *"ChatRepository.kt"* ||
          "$research_response" == *"MainActivity.kt"* ) ]]; then
    verified_review_count=$((verified_review_count + 1))
    echo "::notice title=Review source evidence $review_number::The response states the requested ref and cites a source file."
  else
    echo "::warning title=Review source evidence $review_number::The response did not prove it reviewed the requested feature-branch ref; inspect it before applying its findings."
  fi
done

printf '%s\n' "$completed_review_count/$expected_review_count" > smoke-out/live-review-count.txt
if (( completed_review_count != expected_review_count )); then
  echo "::error title=Live GitHub research::Completed $completed_review_count of $expected_review_count independent model reviews."
  exit 1
fi
echo "::notice title=Live GitHub research::Completed $completed_review_count independent model reviews; $verified_review_count cited the target ref and a source file."

# Clear the temporary provider/key before the main smoke script's final process check.
if ! cleanup_live_test; then
  exit 1
fi
trap - EXIT
# Clear emulator logs from the authenticated flow before smoke.sh captures its final diagnostics.
adb_quiet logcat -c
adb_quiet shell am start -W -n "$PKG/.MainActivity"
live_wait_for_text "How can I help you?" 45
printf 'Live provider smoke passed; emulator provider data was cleared.\n'
