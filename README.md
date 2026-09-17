# AriAi - Ultimate AI Client 🤖

<div align="center">

**The Most Powerful AI Client for Android**

*Fast, Private, Beautiful, Free Demo Included*

![Version](https://img.shields.io/badge/version-2.1.0--premium-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-green)
![License](https://img.shields.io/badge/license-AGPL--3.0-orange)

</div>

---

## 🌟 Introduction

**AriAi** is a professional AI client for Android. Users can connect with their own API keys to any AI model, chat, generate images, and use real web search. Includes **free demo providers** that work without any API key.

### ✨ Key Features

#### 🔌 **Multi-Provider Support**
- ✅ OpenAI (GPT-4o, GPT-4o-mini, o1-preview, DALL·E 3)
- ✅ Google Gemini (2.0 Flash, 1.5 Pro, Imagen 3) - **FREE 1500/day**
- ✅ Anthropic Claude (3.5 Sonnet, 3 Opus, 3 Haiku)
- ✅ Groq (Llama 3.3 70B ultra-fast) - **FREE 14.4K/day**
- ✅ Ollama (Llama 3.2, Qwen 2.5, Mistral, LLaVA)
- ✅ Any OpenAI-Compatible API
- ✅ **FREE DEMO: Pollinations, LLM7 - No key required!**
- ✅ QR Code Import/Export

#### 💬 **Chat Experience**
- 🔄 **Streaming responses** - Real-time
- 🌿 **Message branching** - Explore alternatives
- 📝 **Markdown rendering** - Code highlighting, tables, quotes
- 🧠 **Memory** - Remembers preferences
- 🔀 **Regenerate & Branch**

#### 🔍 **Real Web Search**
- 🌐 **Tavily** - 1000 free searches
- 🦁 **Brave Search** - 2000 free
- 🔮 **Exa** - AI-optimized
- 🎯 **Serper (Google)** - Google search API

#### 🎨 **Image Generation**
- 🖼️ DALL·E 3, DALL·E 2, GPT-Image-1
- 🌟 Imagen 3.0, Flux Pro
- 🆓 Pollinations Free - No key needed

#### 🤖 **Custom Agents**
- 👤 **Built-in:**
  - General Assistant 🤖
  - Code Expert 💻
  - Creative Writer ✍️
  - Translator Pro 🌐
  - Research Assistant 🔍
  - Image Creator 🎨
- 🛠️ Custom prompts with variables: `{time}`, `{model}`, `{date}`
- 🌡️ Temperature & Top-P control

#### 🌍 **6 Languages**
- 🇺🇸 English
- 🇮🇷 Persian - Full support
- 🇸🇦 Arabic
- 🇹🇷 Turkish
- 🇩🇪 German
- 🇫🇷 French

#### 🎨 **Premium Design**
- 🌗 Light / Dark / System themes
- 🎨 Dynamic color from wallpaper (Android 12+)
- ✨ Smooth animations, gradients, premium cards
- 📱 Native Android, Jetpack Compose

---

## 🆓 Free Demo APIs - No Key Required!

AriAi includes **built-in free providers** so users can start instantly:

| Provider | Models | Limit | Key Needed? | Best For |
|----------|--------|-------|-------------|----------|
| **Pollinations** 🌸 | openai, mistral, claude, qwen | Unlimited | **NO** | Instant demo, images |
| **LLM7** 🚀 | gpt-4o-mini, llama-3.1-70b | Free | **NO** | Quick testing |
| **Groq** ⚡ | llama-3.3-70b, mixtral | 14.4K/day | Yes (free) | Ultra-fast, Recommended |
| **Gemini** ✨ | gemini-2.0-flash, 1.5-pro | 1,500/day | Yes (free) | Best free tier, Recommended |
| **OpenRouter** 🌐 | 200+ models, free variants | 50/day | Yes (free) | Model variety |
| **Cerebras** 🧠 | llama-3.3-70b @ 2000 tok/s | 1M tokens/day | Yes (free) | Fastest |
| **GitHub Models** 🐙 | gpt-4o, claude, llama | 150/day | GitHub account | GitHub users |

**How to get free keys (30 seconds):**
- **Groq:** console.groq.com/keys → Sign up → Create key (free, no credit card)
- **Gemini:** aistudio.google.com/app/apikey → Get key (free, 1500/day)
- **OpenRouter:** openrouter.ai/keys → Sign up → Free models

AriAi auto-adds **Pollinations free demo** on first launch, so app works immediately!

---

## 📸 Screenshots - Premium UI

```
Chats Tab       | Chat Screen     | Providers (Graphical) | Agents Grid
- Pinned chats  | - Markdown      | - Gradient cards      | - Premium cards
- Model badges  | - Branch/Regen  | - Free demo section   | - Tools display
- Time ago      | - Web search    | - Recommended badges  | - Custom create
- Premium cards | - Streaming     | - Live status         |
```

**New in v2.1:**
- 🎨 Graphical provider cards with gradients
- 🚀 Free demo header with animation
- 💎 Premium message bubbles
- 🌟 Smooth animations, better performance
- 🆓 Auto free demo provider

---

## 🚀 Installation

### Requirements
- Android 8.0+ (API 26+)
- Android Studio Hedgehog+
- JDK 17+

### Build

```bash
git clone https://github.com/redxbox/ariai.git
cd ariai
./gradlew :app:assembleDebug
```

APK will be at `app/build/outputs/apk/debug/app-debug.apk`

Or download from **GitHub Actions > Artifacts**

### Setup

1. Open AriAi - Free demo (Pollinations) works instantly!
2. For better models, go to `Settings > Providers`
3. Add free provider:
   - **Groq (Recommended):** Base URL `https://api.groq.com/openai/v1` + Key from console.groq.com
   - **Gemini (Recommended):** Base URL `https://generativelanguage.googleapis.com/v1beta` + Key from aistudio.google.com
   - **Pollinations (Free, no key):** Already added! Base URL `https://text.pollinations.ai/openai`

4. Select model and start chatting!

---

## 🔍 Web Search Setup

1. Go to `Settings > Web Search`
2. Add key from:
   - **Tavily:** tavily.com - 1000 free
   - **Brave:** brave.com/search/api - 2000 free
   - **Exa:** exa.ai - 1000 free
   - **Serper:** serper.dev

3. Ask: "Search latest AI news" and AI uses live results!

---

## 🏗️ Architecture

```
com.ariai.app
├── data
│   ├── models          # Provider, Chat, Message, Agent, Search, FreeProviders
│   ├── local           # Room DB, DataStore
│   ├── remote          # AIClient, SearchClient, MCPClient
│   └── repository      # ChatRepository
├── ui
│   ├── theme           # Premium colors, gradients, Material You
│   ├── screens         # ChatList, Chat, Provider (graphical), Agent, Settings, ImageGen, Welcome
│   └── components      # MarkdownText (premium), SearchIntegration
└── util                # Localization (6 langs), QRCodeUtil
```

### Tech Stack
- **UI:** Jetpack Compose + Material3 + Navigation
- **DB:** Room + DataStore
- **Network:** OkHttp + Coroutines Flow
- **Images:** Coil
- **QR:** ZXing

---

## 📝 Roadmap

- [x] Core chat with streaming
- [x] Multi-provider (OpenAI, Gemini, Claude, Groq, Ollama, Custom)
- [x] Free demo providers (Pollinations, LLM7 - no key)
- [x] Graphical provider cards with gradients
- [x] 6 languages
- [x] Material You + Dynamic Color + Premium animations
- [x] Message branching & regenerate
- [x] Real web search (Tavily, Brave, Exa, Serper)
- [x] Image generation (DALL·E 3, Imagen, Pollinations free)
- [x] Custom agents with prompt variables
- [x] Premium UI - smooth, beautiful
- [ ] LaTeX & Mermaid rendering
- [ ] File attachments (PDF, DOCX, Vision)
- [ ] QR scanner UI
- [ ] Export chats
- [ ] MCP integration
- [ ] TTS & STT
- [ ] Local LLM via llama.cpp

---

## 📄 License

AGPL-3.0 - Open and free.

---

<div align="center">

**Built with ❤️ for Global Community**

**Fast, Private, Beautiful - The Ultimate AI Client**

</div>
