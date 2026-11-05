# AriAi

AriAi is an Android chat client for connecting to AI providers with your own credentials. It stores conversations on the device and supports streaming chat, model selection, attachments, provider-dependent media generation, and optional web search.

> AriAi does not bundle provider API keys or guarantee free model access. Availability, pricing, limits, and data handling depend on the provider you configure.

## Current features

- **Providers:** OpenAI, Google Gemini, Anthropic, Ollama, and OpenAI-compatible endpoints. Add your own endpoint and credentials, or import provider settings from JSON/a QR image in the gallery; compatible providers can use OAuth 2.0 Authorization Code with PKCE for public clients. The app does not automatically create a free demo provider.
- **Chat:** streaming preference for ordinary tool-free replies, searchable model switching, provider health/latency checks, a context-use gauge using provider-reported token counts when available and a labeled estimate otherwise, conversation folders, branching, retry, per-chat temperature/top-P/system-prompt controls, and Markdown for headings, lists, links, simple tables, and fenced code blocks with basic syntax coloring and line numbers. Tool-routed replies use a non-streaming request path.
- **Agents:** built-in and custom agents, prompt variables/examples, provider/model binding, temperature/top-P defaults, and opt-in calculator/web-search/MCP tools. Agent settings are stored locally; unsupported arbitrary code execution, persistent memory, and callable image generation are visibly unavailable.
- **Workspace:** AI-assisted generation and updating of one text file at a time using the selected text model, small UTF-8 files in private app storage, a static HTML preview with JavaScript and network requests blocked, and a read-only console limited to listing/reading those workspace files.
- **Tools:** request-scoped calculator, configured web search, reading a URL supplied by the user, read-only access to public GitHub repository files, and remote MCP tools where the selected model supports tool calls. MCP supports Streamable HTTP and legacy HTTP+SSE, JSON-RPC initialization, tool discovery and calls, encrypted Bearer/custom-header credentials and OAuth 2.0 PKCE tokens, and per-server/per-tool opt-in. Remote endpoints require HTTPS; HTTP is limited to loopback development. MCP tool outputs are treated as untrusted data.
- **Web search:** off by default. Gemini and Anthropic can use built-in web search with the AI provider's existing API key. Choose either provider in the chat search sheet; its gear opens inline key settings, and the same key is saved to the provider and reused for search. The Search service settings page manages these two providers—no separate search API key is needed. Results and availability depend on the provider and account limits.
- **Attachments:** scaled images, videos up to 12 MB, supported plain-text/code files, and bounded on-device text extraction from PDFs and DOCX files, including offline Persian/English OCR for scanned PDF pages (other OCR scripts are not bundled). OCR is limited to 20 scanned pages per file; PDF extraction reads at most the first 20 pages and 60,000 characters. Video bytes are sent only to compatible Gemini requests; other providers receive a note that the video cannot be viewed. Extracted text remains on-device until you send the message to your selected provider. Audio-file attachments are not supported.
- **Media:** image, video, or audio generation is available only when a configured provider/model exposes the relevant capability. Provider API support and costs vary. Image editing is not available for Gemini providers.
- **Appearance and language:** light/dark/system theme, optional Android 12+ dynamic accent colors, and translated UI strings in English and Persian. Some unkeyed descriptions and provider-returned error details remain in English.
- **Privacy:** provider API keys, custom headers/bodies/model headers, search keys, MCP credentials, OAuth token state, and pending sign-in data are encrypted with Android Keystore at rest. PDF/DOCX extraction and Persian/English OCR run on-device; chat inference is sent to the provider you select when you send a message. See the [privacy policy](PRIVACY_POLICY.md) and [Google Play Data Safety worksheet](PLAY_DATA_SAFETY.md).

## Configure a provider

1. Open **Settings → Providers**.
2. Choose a provider preset or add an OpenAI-compatible endpoint.
3. Enter the API key and base URL required by that provider, then fetch its model catalog and save.
4. Select the provider and model in a chat, or choose a default model in **Settings → Default model**.

The app does not provide keys, credits, or a built-in free demo account. Use only endpoints and credentials you trust.

### Configure an OAuth 2.0 provider

1. In the provider editor, choose **OAuth 2.0** authentication and enter the HTTPS authorization endpoint, token endpoint, public client ID, and optional space-separated scopes.
2. Register AriAI as a public/native client with the OAuth service and allow this exact redirect URI: `com.ariai.app://oauth2redirect`.
3. Tap **Connect with OAuth** and complete sign-in in AppAuth's secure Android Custom Tab (or its compatible-browser fallback). AriAI uses Authorization Code with PKCE (S256), verifies the returned `state`, and does not request or store a client secret. Tokens are encrypted with Android Keystore and refreshed before provider or MCP requests when required.

AriAI uses manually configured endpoints and client IDs; authorization-server discovery, dynamic client registration, and provider-specific non-standard OAuth parameters are not implemented. OAuth token state is bound to the configured provider API or MCP server URL; changing that URL requires signing in again.

## Configure web search

1. In a chat, open the **Web search** sheet and use the gear beside **Google Gemini** or **Anthropic Claude** to enter the AI provider's API key inline. Saving it connects the provider, selects its text model, and enables web search with that same key. Adding or editing a Gemini/Anthropic provider from **AI Providers** also activates native search.
2. You can manage the same Gemini and Anthropic keys from **Settings → Search service**. Tap the selected provider again in the chat sheet to turn web search off.

## Configure MCP tools

1. Open **Settings → MCP servers** and add a server's HTTPS endpoint. The legacy SSE transport is available for existing servers; Streamable HTTP is preferred for new deployments.
2. Choose no authentication, a Bearer token, a custom HTTP header, or OAuth 2.0. Static credentials and OAuth token state are encrypted locally with Android Keystore; credentials are never placed in the URL.
3. For OAuth, enter the HTTPS authorization and token endpoints, public client ID, and optional scopes. Register the redirect URI `com.ariai.app://oauth2redirect` with the OAuth service, then tap **Connect with OAuth**. AppAuth opens a secure Android Custom Tab and uses Authorization Code with PKCE S256; AriAI does not accept client secrets.
4. Test the connection to initialize the MCP session and list tools. New servers start disabled; enabling a server requires an explicit trust confirmation. Select only the discovered tools you want to make available to chats.
5. For a custom agent, also enable **MCP tools** in that agent's available-tools list. A normal chat may use only the enabled servers and individually allowed tools.

AriAI supports OAuth 2.0 Authorization Code with PKCE for compatible MCP servers using manually entered authorization/token endpoints and a pre-registered public client. Authorization-server discovery and dynamic client registration are not implemented, and client secrets are not supported. Local `stdio` processes, MCP resources/prompts, and subscriptions are also unavailable. MCP servers can read or change data when their enabled tools are called. Review a server before trusting it; server descriptions and results are untrusted.

## Privacy and Google Play data safety

The current privacy policy is [PRIVACY_POLICY.md](PRIVACY_POLICY.md); the **Settings → About → Privacy policy** row opens its public GitHub page. The [Play Data Safety worksheet](PLAY_DATA_SAFETY.md) records the app's current data flows and questions to verify in Play Console. Review both whenever services, dependencies, or data handling change. Sending prompts, files, search queries, or MCP tool arguments to a configured service is user-initiated but still leaves the device; each recipient's retention practices are its own.

## Requirements and build

- Android 8.0 (API 26) or later
- JDK 17
- Compile SDK 36 and target SDK 36
- Android Gradle Plugin 8.13.2 / Gradle 8.13

```bash
git clone https://github.com/redxbox/ariai.git
cd ariai
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The app version is **2.3.0-stable** (`versionCode` 5). The Gradle build uses Kotlin 2.2.20 with the official Jetpack Compose compiler plugin.

## Signed release bundle

The **Signed Android App Bundle** GitHub Actions workflow builds and verifies a signed `.aab`, then uploads it with a SHA-256 checksum; it does not publish to Google Play. It runs manually (`workflow_dispatch`) or for an approved `v*` tag. Configure the release keystore secrets as documented in [`.github/RELEASE_SIGNING.md`](.github/RELEASE_SIGNING.md). Never commit the keystore or put secret values in logs or chat.

## Known limitations

- The workspace console is deliberately not a general-purpose terminal: it has no shell, PRoot, package manager, or code execution. The HTML preview is static and does not run JavaScript, remote assets, or app frameworks.
- OAuth endpoint discovery and dynamic client registration are not implemented; provider-specific non-standard OAuth parameters and client secrets are not supported. MCP also does not support local `stdio` processes, resource/prompt APIs, or subscriptions. Connected servers may perform consequential actions through tools that the user explicitly enabled.
- Persistent memory controls are not implemented. Provider QR import from a gallery image and live camera scanning, plus offline LaTeX/Mermaid rendering, are supported.
- Provider-specific features may fail or be unavailable when an endpoint does not implement the expected API. Check that provider's documentation and model capabilities.
- The repository checkout has no top-level project `LICENSE` file. `THIRD_PARTY_LICENSES` contains dependency notices; it does not declare the project's own license.

## Project layout

- `app/src/main/java/com/ariai/app/data` — Room/DataStore models, repository, and provider/search clients
- `app/src/main/java/com/ariai/app/ui` — Jetpack Compose screens, chat UI, and theme
- `app/src/test` — JVM unit tests
- `.github/workflows` — Android build, test, and emulator workflows
