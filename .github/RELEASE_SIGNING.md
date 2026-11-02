# Signed release AAB

The `Signed Android App Bundle` workflow runs `:app:bundleRelease`, verifies the bundle signature, and uploads the `.aab` plus SHA-256 checksum as a GitHub Actions artifact. It does **not** publish to Google Play or create a GitHub Release. Start it with `workflow_dispatch` after this workflow is present on the selected branch, or push a `v*` tag when the release is approved.

## Required repository secrets

The workflow prefers these canonical names:

- `ANDROID_KEYSTORE_BASE64` — base64-encoded contents of the private release keystore file.
- `ANDROID_KEYSTORE_PASSWORD` — keystore password.
- `ANDROID_KEY_ALIAS` — alias for the release signing key.
- `ANDROID_KEY_PASSWORD` — password for that key.

For compatibility with common existing names, the workflow also recognizes `RELEASE_KEYSTORE_BASE64`, `KEYSTORE_BASE64`, or `ANDROID_KEYSTORE`; `RELEASE_KEYSTORE_PASSWORD`, `KEYSTORE_PASSWORD`, or `ANDROID_STORE_PASSWORD`; `RELEASE_KEY_ALIAS` or `KEY_ALIAS`; and `RELEASE_KEY_PASSWORD` or `KEY_PASSWORD` respectively. If none of the names in a group is configured, the workflow fails with a message naming the canonical secret only; it never prints a secret value. GitHub does not make secret values readable after saving, so keep the exact values private and use the Actions error only to identify a missing name.

The keystore is decoded into the runner's temporary directory with restrictive permissions, passed to Gradle through process environment variables, and removed when the build step exits. The keystore itself is not uploaded or committed. Release builds outside this workflow remain unsigned unless the four `ARIAI_RELEASE_*` environment variables are provided together.
