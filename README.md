# Local AI — On-Device LLM Android App

A native Android application built with **Kotlin** and **Jetpack Compose (Material 3)** that runs Large Language Models (LLMs) **100% locally on-device**. Runs completely offline without cloud APIs, telemetry, or data leaving your device.

Includes a curated **1-Tap Model Hub** for open-source weights (Qwen 2.5, Llama 3.2, SmolLM2) and an automated **GitHub Actions CI/CD** pipeline that tests and builds the debug APK.

---

## Features

- ⚡ **100% Local On-Device Inference**: Unified engine routing supporting open-source **`.gguf`** models (Qwen 2.5, Llama 3.2) and MediaPipe **`.bin`/`.task`** weights.
- 🛒 **1-Tap Model Hub**: In-app catalog to browse and download mobile-optimized open-source models directly from official Hugging Face repositories with a single tap.
- 💬 **Streaming Token Output**: Real-time token streaming with throughput (tokens/second) and first-token latency metrics.
- 🛑 **Interrupt Generation**: Instantly stop generation at any point.
- 📁 **Flexible Model Management**:
  - **1-Tap Presets**: Download Qwen 2.5 (0.5B/1.5B), Llama 3.2 (1B), SmolLM2 (1.7B), Phi-3.5 (3.8B).
  - **Custom GGUF URL**: Paste direct HTTP/HTTPS links from Hugging Face.
  - **Storage Access Framework (SAF)**: Import `.gguf`, `.bin`, or `.task` files from device Downloads.
- 🎛️ **Inference Settings**: Real-time adjustment of Temperature (0.1–2.0), Top-K (1–100), and Max Tokens (64–2048).
- 🎨 **Modern Material 3 Theme**: Dark theme with tabbed Model Manager, elevated message bubbles, and smooth micro-interactions.
- 🚀 **Automated GitHub Actions CI**: Compiles debug APK and uploads it as a workflow artifact.

---

## Supported Models

The app supports open-source **GGUF** models (`.gguf`) as well as **MediaPipe Tasks GenAI** files (`.bin` or `.task`):

### Built-in 1-Tap Presets (Model Hub)
1. **Qwen 2.5 0.5B Instruct** (`Q4_K_M`, ~398 MB) — Ultra-fast, ideal for phones with 2–4 GB RAM.
2. **Qwen 2.5 1.5B Instruct** (`Q4_K_M`, ~986 MB) — Outstanding reasoning and code comprehension in a compact footprint.
3. **Llama 3.2 1B Instruct** (`Q4_K_M`, ~808 MB) — Meta's official mobile edge model.
4. **SmolLM2 1.7B Instruct** (`Q4_K_M`, ~1.06 GB) — Hugging Face's high-efficiency lightweight agent.
5. **Phi-3.5 Mini 3.8B Instruct** (`Q4_K_M`, ~2.39 GB) — Desktop-grade reasoning for flagship phones (8 GB+ RAM).

---

## How to Download and Run Models

### Option 1: 1-Tap Download via Model Hub (Easiest)
1. Open the app and tap **"Explore & Download Models"** (or the **Explore compass icon** in the top bar).
2. Browse the **Model Hub** tab to see model sizes and RAM requirements.
3. Tap **"Get"** next to **Qwen 2.5** (or any model). The model downloads with a progress bar and automatically registers.
4. Tap **"Active"** and begin chatting!

### Option 2: Custom GGUF URL
1. In the Model Manager dialog, expand **"+ Custom GGUF URL"**.
2. Paste any direct Hugging Face resolve URL pointing to a `.gguf` file (e.g., `https://huggingface.co/.../resolve/main/...gguf`).
3. Tap **"Download Custom GGUF"**.

### Option 3: Import from Phone Storage
1. Download a `.gguf` or `.bin` file in your browser or transfer it via ADB (`adb push model.gguf /sdcard/Download/`).
2. In the Model Manager dialog under **"Installed"**, tap **"Import .gguf from Device Storage"**.
3. Select the file; it will be imported into app storage and ready to load.

---

## Architecture & Project Structure

```
my_ai/
├── .github/
│   └── workflows/
│       └── build-apk.yml          # GitHub Actions workflow for APK build & artifact upload
├── app/
│   ├── build.gradle.kts           # App dependencies (Compose, MediaPipe, Coroutines)
│   ├── proguard-rules.pro         # Proguard rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Permissions (INTERNET for downloader, largeHeap)
│       │   ├── res/               # App icons, strings, colors
│       │   └── java/com/localai/app/
│       │       ├── MainActivity.kt # Compose entry point
│       │       ├── data/
│       │       │   ├── engine/     # UnifiedLlmEngine, LlamaEngine (GGUF), MediaPipeLlmInferenceEngine
│       │       │   └── model/      # CuratedModel, ChatMessage, InferenceConfig, ModelInfo
│       │       ├── repository/     # ModelManager (GGUF discovery, Hugging Face streaming, SAF import)
│       │       └── ui/
│       │           ├── chat/       # ChatScreen, ChatViewModel, ChatMessageItem, ChatInputBar
│       │           ├── models/     # ModelManagerDialog (Model Hub & Installed tabs)
│       │           ├── settings/   # InferenceSettingsDialog
│       │           └── theme/      # Material 3 Theme, Color, Type
│       └── test/                  # Unit tests (UnifiedEngine, CuratedModel, ModelManager, ViewModel)
├── gradle/
│   ├── libs.versions.toml         # Version catalog
│   └── wrapper/                   # Gradle wrapper configuration
├── build.gradle.kts               # Root build script
├── settings.gradle.kts            # Project settings
└── gradle.properties              # JVM heap settings
```

---

## Building Locally & CI

### Prerequisites
- JDK 17
- Android SDK (API 34)

### Commands
```bash
# Run unit tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug
```
The output APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions CI
The workflow at `.github/workflows/build-apk.yml` runs on every push and pull request, executes unit tests, builds the APK, and uploads the downloadable `local-ai-debug-apk` artifact.
