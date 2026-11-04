#!/usr/bin/env bash
# Invoked as one line by android-emulator-runner; keep Gradle output available as a CI artifact.
set -euo pipefail

LOG="${GITHUB_WORKSPACE:-$PWD}/instrumentation.log"
exec > >(tee -a "$LOG") 2>&1

echo "Starting connected UI tests at $(date -Is)"
echo "Working directory: $PWD"
adb devices -l || true
./gradlew --no-daemon :app:connectedDebugAndroidTest --stacktrace
