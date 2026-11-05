# Release APK and signed AAB

The `Release APK and Signed AAB` workflow (`.github/workflows/release-aab.yml`) runs `:app:assembleRelease` and `:app:bundleRelease`, verifies the APK signature with `apksigner` and the bundle signature with `jarsigner`, and produces `SHA256SUMS.txt` covering both files.

- On a pushed `v*` tag (for example `v2.3.0`, matching `versionName` in `app/build.gradle.kts`) it additionally creates a GitHub Release whose notes are generated automatically from merged pull requests and commit titles, and attaches `app-release.apk`, `app-release.aab`, and `SHA256SUMS.txt`.
- On `workflow_dispatch` it uploads the same files as a workflow artifact only; no release is created.

It does **not** publish to Google Play. Tag commands are documented in the top-level `README.md` under "Releases (GitHub Releases)".

## Required repository secrets

The workflow prefers these canonical names:

- `ANDROID_KEYSTORE_BASE64` — base64-encoded contents of the private release keystore file.
- `ANDROID_KEYSTORE_PASSWORD` — keystore password.
- `ANDROID_KEY_ALIAS` — alias for the release signing key.
- `ANDROID_KEY_PASSWORD` — password for that key.

For compatibility with common existing names, the workflow also recognizes `RELEASE_KEYSTORE_BASE64`, `KEYSTORE_BASE64`, or `ANDROID_KEYSTORE`; `RELEASE_KEYSTORE_PASSWORD`, `KEYSTORE_PASSWORD`, or `ANDROID_STORE_PASSWORD`; `RELEASE_KEY_ALIAS` or `KEY_ALIAS`; and `RELEASE_KEY_PASSWORD` or `KEY_PASSWORD` respectively. If none of the names in a group is configured, the workflow fails with a message naming the canonical secret only; it never prints a secret value. GitHub does not make secret values readable after saving, so keep the exact values private and use the Actions error only to identify a missing name.

The keystore is decoded into the runner's temporary directory with restrictive permissions, passed to Gradle through process environment variables, and removed when the build step exits. The keystore itself is not uploaded or committed. Release builds outside this workflow remain unsigned unless the four `ARIAI_RELEASE_*` environment variables are provided together.
