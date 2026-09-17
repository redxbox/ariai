# AriAi - RikkaHub Inspired AI Client 🤖

<div align="center">

**قدرتمندترین کلاینت هوش مصنوعی برای اندروید - الهام گرفته از RikkaHub**

*The Ultimate AI Client for Android - Inspired by RikkaHub*

![Version](https://img.shields.io/badge/version-2.0.0--rikkahub-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-green)
![License](https://img.shields.io/badge/license-AGPL--3.0-orange)

</div>

---

## 🌟 معرفی / Introduction

**AriAi** یک کلاینت حرفه‌ای هوش مصنوعی برای اندروید است که دقیقا مانند **RikkaHub** عمل می‌کند. کاربران می‌توانند با API خودشان به هر مدل هوش مصنوعی متصل شوند، چت کنند، تصویر بسازند و از جستجوی واقعی وب استفاده کنند.

**AriAi** is a professional AI client for Android, working exactly like **RikkaHub**. Users connect with their own API keys to any AI model, chat, generate images, and use real web search.

### ✨ ویژگی‌های کلیدی / Key Features (RikkaHub Parity)

#### 🔌 **Multi-Provider Support**
- ✅ OpenAI (GPT-4o, GPT-4o-mini, o1-preview, DALL·E 3)
- ✅ Google Gemini (2.0 Flash, 1.5 Pro, Imagen 3)
- ✅ Anthropic Claude (3.5 Sonnet, 3 Opus, 3 Haiku)
- ✅ Ollama (Llama 3.2, Qwen 2.5, Mistral, LLaVA)
- ✅ Any OpenAI-Compatible API (Custom endpoints)
- ✅ QR Code Import/Export for providers

#### 💬 **Chat Experience**
- 🔄 **Streaming responses** - Real-time token streaming
- 🌿 **Message branching** - Explore alternative replies (RikkaHub feature)
- 📝 **Markdown rendering** - Code highlighting, tables, LaTeX, quotes
- 🖼️ **Multimodal** - Images, PDFs, DOCX support (planned)
- 🧠 **Memory** - ChatGPT-like memory feature
- 🔀 **Regenerate & Branch** - RikkaHub-style conversation branching

#### 🔍 **Real Web Search (NEW)**
- 🌐 **Tavily** - 1000 free searches, advanced depth
- 🦁 **Brave Search** - 2000 free searches
- 🔮 **Exa** - AI-optimized search
- 🎯 **Serper (Google)** - Google search API
- 💡 Auto-injects search results into LLM context

#### 🎨 **Image Generation**
- 🖼️ DALL·E 3, DALL·E 2
- 🌟 GPT-Image-1, Imagen 3.0
- 🔥 Flux Pro support
- 📐 Multiple sizes: 1024x1024, 1792x1024, etc.

#### 🤖 **Custom Agents (RikkaHub Style)**
- 👤 **Built-in Agents:**
  - General Assistant 🤖
  - Code Expert 💻
  - Creative Writer ✍️
  - Translator Pro 🌐
  - Research Assistant 🔍
  - Image Creator 🎨
- 🛠️ Custom system prompts with variables: `{time}`, `{model}`, `{date}`, `{user}`
- 🌡️ Temperature & Top-P control
- 🔧 Tools: Web Search, Image Gen, Memory, MCP

#### 🌍 **6 Languages**
- 🇺🇸 English
- 🇮🇷 فارسی (Persian) - کامل
- 🇸🇦 العربية (Arabic)
- 🇹🇷 Türkçe (Turkish)
- 🇩🇪 Deutsch (German)
- 🇫🇷 Français (French)

#### 🎨 **Material You Design**
- 🌗 Light / Dark / System themes
- 🎨 Dynamic color from wallpaper (Android 12+)
- ✨ Smooth animations, predictive back
- 📱 Native Android, Jetpack Compose

#### 🛠️ **Power Features**
- 📋 Prompt variables
- 🔑 Custom HTTP headers & body
- 📤 Export chats as Markdown/JSON (planned)
- 🔗 MCP (Model Context Protocol) support (planned)
- 💾 Room DB + DataStore persistence

---

## 📸 Screenshots (RikkaHub Style UI)

```
Chats Tab       | Chat Screen     | Providers      | Agents Grid
- Pinned chats  | - Markdown      | - OpenAI       | - 6 built-in
- Model badges  | - Branch/Regen  | - Gemini       | - Custom create
- Time ago      | - Web search    | - Claude       | - Tools display
                | - Streaming     | - Ollama       |
```

---

## 🚀 نصب و راه‌اندازی / Installation

### پیش‌نیازها / Requirements
- Android 8.0+ (API 26+)
- Android Studio Hedgehog+
- JDK 17+

### Build

```bash
git clone https://github.com/redxbox/ariai.git
cd ariai
# Open in Android Studio
# Place google-services.json if needed (optional)
./gradlew :app:assembleDebug
```

### تنظیم Provider / Setup Provider

1. **AriAi** را باز کنید
2. به `Settings` > `Providers` بروید
3. `Add Provider` را بزنید:
   - **OpenAI:** `https://api.openai.com/v1` + API Key از `platform.openai.com`
   - **Gemini:** `https://generativelanguage.googleapis.com/v1beta` + Key از `aistudio.google.com`
   - **Anthropic:** `https://api.anthropic.com` + Key از `console.anthropic.com`
   - **Custom:** هر endpoint سازگار با OpenAI

4. مدل را انتخاب کنید و چت را شروع کنید!

---

## 🔍 تنظیم جستجوی وب / Web Search Setup

برای فعال‌سازی جستجوی واقعی وب مثل RikkaHub:

1. به `Settings` > `Web Search` بروید
2. یکی از کلیدها را اضافه کنید:
   - **Tavily:** [tavily.com](https://tavily.com) - 1000 جستجوی رایگان
   - **Brave:** [brave.com/search/api](https://brave.com/search/api) - 2000 رایگان
   - **Exa:** [exa.ai](https://exa.ai) - 1000 رایگان
   - **Serper:** [serper.dev](https://serper.dev) - Google Search

3. حالا در چت بپرسید: "آخرین اخبار هوش مصنوعی را جستجو کن" و AI از نتایج وب استفاده می‌کند!

---

## 🏗️ معماری / Architecture

```
com.ariai.app
├── data
│   ├── models          # Provider, Chat, Message, Agent, Search
│   ├── local           # Room DB, DataStore Preferences
│   ├── remote          # AIClient (OpenAI/Gemini/Anthropic), SearchClient
│   └── repository      # ChatRepository (single source of truth)
├── ui
│   ├── theme           # Material You, Dynamic Color
│   ├── screens         # ChatList, Chat, Provider, Agent, Settings, ImageGen, Welcome
│   └── components      # MarkdownText, MessageBubble, SearchIntegration
└── util                # Localization (6 langs), QRCodeUtil
```

### تکنولوژی‌ها / Tech Stack
- **UI:** Jetpack Compose + Material3 + Navigation Compose
- **DI:** Manual (App container) - Koin ready
- **DB:** Room + DataStore Preferences
- **Network:** OkHttp + Kotlinx Serialization + Coroutines Flow
- **Images:** Coil
- **QR:** ZXing
- **Markdown:** Custom lightweight parser (RikkaHub-style)

---

## 🌐 مقایسه با RikkaHub / Comparison

| Feature | RikkaHub | AriAi (This) |
|---------|----------|--------------|
| Multi-provider | ✅ | ✅ |
| OpenAI/Gemini/Claude | ✅ | ✅ |
| Custom API | ✅ | ✅ |
| QR Import/Export | ✅ | ✅ (util ready) |
| Message Branching | ✅ | ✅ |
| Web Search (Tavily/Brave/Exa) | ✅ | ✅ |
| Image Generation | ✅ | ✅ |
| Markdown + LaTeX + Mermaid | ✅ | ✅ (Markdown done, LaTeX/Mermaid planned) |
| Agents/Personas | ✅ | ✅ |
| Memory | ✅ | ✅ |
| 6+ Languages | ✅ (7 langs) | ✅ (6 langs) |
| Material You | ✅ | ✅ |
| MCP Support | ✅ | 🚧 Planned |
| Linux Workspace (proot) | ✅ | 🚧 Planned |
| TTS/ASR | ✅ | 🚧 Planned |

**AriAi aims for 100% RikkaHub parity + Persian-first experience!**

---

## 📝 Roadmap - رسیدن به RikkaHub کامل

- [x] Core chat with streaming
- [x] Multi-provider (OpenAI, Gemini, Claude, Ollama, Custom)
- [x] Provider management + models
- [x] 6 languages (EN, FA, AR, TR, DE, FR)
- [x] Material You + Dynamic Color
- [x] Message branching & regenerate
- [x] Real web search (Tavily, Brave, Exa, Serper)
- [x] Image generation (DALL·E 3, Imagen)
- [x] Custom agents with prompt variables
- [ ] LaTeX & Mermaid rendering (use multiplatform-markdown-renderer)
- [ ] File attachments (PDF, DOCX, Images vision)
- [ ] QR code scanner UI for provider import
- [ ] Export chats as Markdown/JSON
- [ ] MCP (Model Context Protocol) integration
- [ ] Linux workspace via proot (like RikkaHub)
- [ ] TTS (Text-to-Speech) & STT (Speech-to-Text)
- [ ] Voice conversation mode
- [ ] Local LLM via llama.cpp / LiteRT
- [ ] Wear OS companion

---

## 🤝 مشارکت / Contributing

ما عاشق مشارکت هستیم! Like RikkaHub, we are opinionated but open.

- Translation improvements are always welcome!
- Bug fixes and performance improvements
- Please open an issue before large features

---

## 📄 لایسنس / License

AGPL-3.0 - Same as RikkaHub, to keep it open and free.

---

## 🙏 تشکر / Credits

- **RikkaHub** - The original inspiration - [github.com/rikkahub/rikkahub](https://github.com/rikkahub/rikkahub) - Best AI client for Android!
- **Material You** - Google's design system
- **Jetpack Compose** - Modern Android UI toolkit

---

<div align="center">

**ساخته شده با ❤️ برای جامعه فارسی‌زبان و جهانی**

**Built with ❤️ for Persian and Global Community**

*If you like RikkaHub, you'll love AriAi - Persian Edition!*

</div>
