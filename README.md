# AriAi

AriAi is an Android chat client for connecting to AI providers with your own credentials. It stores conversations on the device and supports streaming chat, model selection, attachments, provider-dependent media generation, and optional web search.

> AriAi does not bundle provider API keys or guarantee free model access. Availability, pricing, limits, and data handling depend on the provider you configure.

## Current features

- **Providers:** OpenAI, Google Gemini, Anthropic, Ollama, and OpenAI-compatible endpoints. Add your own endpoint and credentials; the app does not automatically create a free demo provider.
- **Chat:** streaming preference for ordinary tool-free replies, searchable model switching, provider health/latency checks, a context-use gauge using provider-reported token counts when available and a labeled estimate otherwise, conversation folders, branching, retry, per-chat temperature/top-P/system-prompt controls, and Markdown for headings, lists, links, simple tables, and fenced code blocks with basic syntax coloring and line numbers. Tool-routed replies use a non-streaming request path.
- **Agents:** built-in and custom agents, prompt variables/examples, provider/model binding, temperature/top-P defaults, and opt-in calculator/web-search tools. Settings are stored locally; unsupported tools (MCP, arbitrary code execution, persistent memory, callable image generation) are visibly unavailable.
- **Workspace:** AI-assisted generation and updating of one text file at a time using the selected text model, small UTF-8 files in private app storage, a static HTML preview with JavaScript and network requests blocked, and a read-only console limited to listing/reading those workspace files.
- **Tools:** request-scoped calculator, configured web search, reading a URL supplied by the user, and read-only access to public GitHub repository files where the selected model supports tool calls. MCP server setup is currently a preview; it does not connect to servers or expose MCP tools to chats.
- **Web search:** off by default. Gemini, Anthropic, and supported OpenRouter models can use built-in web search with the AI provider's existing API key; optional Google Search (via Serper), Brave Search, Tavily, or Exa keys support local search and fallback. Search API keys are saved on the device. Results and availability depend on the provider and account limits.
- **Attachments:** scaled images, videos up to 12 MB, and supported plain-text/code files. Video bytes are sent only to compatible Gemini requests; other providers receive a note that the video cannot be viewed. PDF and DOCX extraction are not implemented.
- **Media:** image, video, or audio generation is available only when a configured provider/model exposes the relevant capability. Provider API support and costs vary. Image editing is not available for Gemini providers.
- **Appearance and language:** light/dark/system theme, optional Android 12+ dynamic accent colors, and translated UI strings in English and Persian. Some unkeyed descriptions and provider-returned error details remain in English.

## Configure a provider

1. Open **Settings → Providers**.
2. Choose a provider preset or add an OpenAI-compatible endpoint.
3. Enter the API key and base URL required by that provider, then fetch its model catalog and save.
4. Select the provider and model in a chat, or choose a default model in **Settings → Default model**.

The app does not provide keys, credits, or a built-in free demo account. Use only endpoints and credentials you trust.

## Configure web search

1. In a chat, tap the composer search icon and choose a connected Gemini, Anthropic, or supported OpenRouter model to use its built-in search with the same AI provider API key; this stays off until selected.
2. To use Local Search or provide a fallback key, open **Settings → Search service**, expand **Optional local search API keys**, and configure Google Search (through Serper), Brave Search, Tavily, or Exa.
3. In the chat sheet, choose **Local Search** to use that separate search key, or **Off** to disable web search.

## Requirements and build

- Android 8.0 (API 26) or later
- JDK 17

```bash
git clone https://github.com/redxbox/ariai.git
cd ariai
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The app version is **2.2.1-stable** (`versionCode` 4). The Gradle build uses Kotlin 1.9.22 and Jetpack Compose.

## Known limitations

- The workspace console is deliberately not a general-purpose terminal: it has no shell, PRoot, package manager, or code execution. The HTML preview is static and does not run JavaScript, remote assets, or app frameworks.
- MCP configuration is preview-only; server discovery and tool calls are not implemented.
- PDF/DOCX text extraction, persistent memory controls, QR scanning, and LaTeX/Mermaid rendering are not implemented.
- Provider-specific features may fail or be unavailable when an endpoint does not implement the expected API. Check that provider's documentation and model capabilities.
- The repository checkout has no top-level project `LICENSE` file. `THIRD_PARTY_LICENSES` contains dependency notices; it does not declare the project's own license.

## Project layout

- `app/src/main/java/com/ariai/app/data` — Room/DataStore models, repository, and provider/search clients
- `app/src/main/java/com/ariai/app/ui` — Jetpack Compose screens, chat UI, and theme
- `app/src/test` — JVM unit tests
- `.github/workflows` — Android build, test, and emulator workflows
