# Google Play Data Safety worksheet — AriAI

**For the 2.3.0-stable / target API 36 release; review against the exact release artifact before submitting.** This is an engineering worksheet, not a submitted declaration or legal advice. The publisher remains responsible for answers in Play Console. Re-check Google's live [Data safety form guidance](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en) and its data categories when completing the form.

## Do not answer “no data collected” just because AriAI has no backend

Google defines collection as transmitting data from the app off the device. AriAI sends content directly to user-configured AI and search endpoints, and may hand voice input to Android's speech-recognition service. These are user-initiated, feature-specific transmissions. Google has a user-initiated-action exception to its **sharing** definition; confirm how the current form applies that exception. Do not confuse “not shared” with “not transmitted/collected.” The external services' retention and processing are not controlled by AriAI.

## Code-verified practices

- No AriAI account or developer-operated chat server.
- No ads, analytics SDK, crash-reporting SDK, or app-generated device identifier.
- Prompts, conversation context, selected attachments, and enabled-tool inputs/results can be sent to the configured AI provider when the user sends a message or authorizes a tool-enabled agent.
- Optional search queries go to the search service the user selects. Search results may then be included in an AI-provider request.
- For an OAuth-configured AI provider, the user starts sign-in; AriAI exchanges the authorization code at the configured HTTPS token endpoint using PKCE, sends the access token with provider requests, and refreshes it at that endpoint when needed.
- The app can send user-selected attachments to a compatible AI provider. Supported user files include images, videos, PDFs, DOCX, and supported text/code formats; audio-file attachments are not supported. Scanned PDF pages can be OCRed on-device in Persian or English using bundled models; OCR itself sends no data to a service. The resulting text is sent to the selected AI provider only when the user sends the message. Local generated/user files remain in app-private storage unless the user sends them to a service.
- Voice input invokes Android speech recognition; the selected recognition service receives audio and returns text. AriAI does not intentionally persist the microphone recording. The app does not control whether that recognition service processes audio locally or remotely.
- MCP sends tool arguments to the user-configured MCP server and receives tool output. Static credentials and OAuth access tokens are sent in request headers; OAuth authorization-code exchange and refresh requests go to the configured token endpoint. Remote MCP and OAuth endpoints require HTTPS; loopback development endpoints may use HTTP.
- User-entered provider, search, and MCP credentials, OAuth access/refresh-token state, and temporary pending OAuth authorization data (including the PKCE verifier) are stored on-device encrypted with Android Keystore. Chat history and files are in app-private storage but are not separately encrypted by AriAI. Android backup is disabled.
- App HTTP networking blocks cleartext except for loopback/local-development addresses. Remote endpoints use validated HTTPS connections.

## Likely data types to review in Play Console

| Play Console data type | What triggers it in AriAI | Purpose | User control / notes |
| --- | --- | --- | --- |
| **Messages → Other in-app messages** and/or **App activity → Other user-generated content** | Prompts and conversation text sent to the selected AI endpoint; enabled agent/tool context. Review the current category definitions and select the closest applicable category rather than declaring no content. | App functionality | User chooses provider and initiates each send. Users can use the app without configuring a provider, but provider-backed chat requires a prompt to be sent. Decide optional/required in the live form consistently with that scope. |
| **App activity → In-app search history** | A query sent to a selected web-search service. | App functionality | Optional; only when the user invokes search. Query text may also be part of a chat prompt. |
| **Photos and videos → Photos / Videos** | User-selected images/videos sent with a prompt to a compatible provider. | App functionality | Optional; user selects files and sends the message. |
| **Files and docs → Files and docs** | User-selected text/documents sent with a prompt to a compatible provider. | App functionality | Optional; user selects files and sends the message. |
| **Audio files → Voice or sound recordings** | Microphone audio handed to the Android speech-recognition service for transcription. AriAI does not support user-selected audio-file attachments. | App functionality | Voice input is user-initiated. Verify the behavior of the speech service on target Android devices and apply the current Play rules for platform-service handoffs. |
| **Authentication information / credentials** (use the closest current Play Console data type, if applicable) | Provider/search/MCP credentials, OAuth access/refresh tokens, and secrets entered in custom provider headers, request bodies, or model-specific headers are encrypted locally. Access tokens are sent in `Authorization` headers; refresh requests go to the configured OAuth token endpoint. | App functionality | User initiates OAuth sign-in and provider/tool use. Do not claim the secrets never leave the device. Review the current Console categories/policy; keys are not listed as a standalone data type in every version of Google's category table. |

Local chat history, local files, preferences, and encrypted credentials that are never used by an off-device feature do not, by themselves, count as off-device collection under Google's stated definition. If any future SDK or endpoint starts transmitting them, update this worksheet, privacy policy, and Play declaration.

## Sharing and security answers to verify

- **Sharing:** Provider, OAuth, search, MCP, and speech-recognition transmissions are initiated by the user and reasonably expected for the feature. Google's guidance lists user-initiated transfers as an exception to “sharing”; verify the current form and do not describe this as a blanket no-transmission claim. AriAI does not sell data.
- **Encryption in transit:** AriAI's remote HTTP requests use HTTPS with certificate validation; plaintext HTTP is permitted only for loopback/local development. The Android speech service's downstream processing is not controlled by AriAI. Answer based on the release artifact and actual system/service integrations.
- **Deletion:** There is no AriAI account or developer server holding conversations. Users can delete chats/files and can clear app storage or uninstall to remove local data. AriAI cannot delete copies retained by a provider, search service, speech service, GitHub, or MCP server; users must use that service's deletion controls. In the form, describe the in-app local deletion path accurately and do not promise remote deletion.
- **Retention:** External providers may retain prompts, queries, attachments, credentials, OAuth token requests, or tool calls under their own terms. AriAI does not set those retention periods.

## Release checklist

1. Review the dependency/SDK inventory and final merged manifest for any new data collection.
2. Test the exact release build with a normal provider, a custom endpoint, provider/MCP OAuth authorization and token refresh, search, microphone recognition, attachment upload, and an MCP server; record where each request goes and whether it uses HTTPS.
3. Complete the Play Console data types, purposes, optional/required, sharing, encryption, and deletion questions from the live definitions. Check each third party's current retention terms, especially any default provider/search endpoint.
4. Set the Play Console privacy-policy URL to `https://github.com/redxbox/ariai/blob/main/PRIVACY_POLICY.md` after that file is present on the public default branch. The same policy is linked inside Settings → About.
5. Revisit both documents whenever a provider, SDK, network endpoint, data type, or retention behavior changes.
