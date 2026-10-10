# Local AI — On-Device LLM Android App

A native Android application built with **Kotlin** and **Jetpack Compose (Material 3)** that runs Large Language Models (LLMs) **100% locally on-device** using **Google MediaPipe Tasks GenAI**. No cloud APIs, no telemetry, and no data leaving your phone.

Includes an automated **GitHub Actions CI/CD** pipeline that runs tests and builds the ready-to-install debug APK on every commit and PR.

---

## Features

- ⚡ **100% Local On-Device Inference**: Powered by Google MediaPipe Tasks GenAI (`com.google.mediapipe:tasks-genai`), running directly on Android CPU or GPU.
- 💬 **Streaming Token Output**: Real-time token streaming with generation performance metrics (tokens/second throughput and first-token latency).
- 🛑 **Interrupt Generation**: Stop generating response at any token.
- 📁 **Flexible Model Management**:
  - **SAF File Picker**: Import `.bin` or `.task` model files from Downloads or SD card.
  - **In-App Downloader**: Built-in HTTP streaming downloader with real-time progress bar.
- 🎛️ **Inference Settings**: Real-time adjustment of Temperature (0.1–2.0), Top-K (1–100), and Max Tokens (64–2048).
- 🎨 **Modern Material 3 Theme**: Clean dark theme with smooth micro-interactions, elevated message bubbles, and intuitive model switcher.
- 🚀 **Automated GitHub Actions CI**: Compiles debug APK and uploads it as an artifact.

---

## Architecture & Project Structure

```
my_ai/
├── .github/
│   └── workflows/
│       └── build-apk.yml          # GitHub Actions workflow for APK build & artifact upload
├── app/
│   ├── build.gradle.kts           # App-level dependencies (Compose, MediaPipe GenAI, Coroutines)
│   ├── proguard-rules.pro         # Proguard rules for MediaPipe JNI native symbols
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Permissions (INTERNET for downloader, largeHeap)
│       │   ├── res/               # App icons, strings, colors
│       │   └── java/com/localai/app/
│       │       ├── MainActivity.kt # Compose entry point
│       │       ├── data/
│       │       │   ├── engine/     # MediaPipe Tasks GenAI engine wrapper & token streaming
│       │       │   └── model/      # ChatMessage, InferenceConfig, ModelInfo
│       │       ├── repository/     # ModelManager (local storage, SAF import, downloader)
│       │       └── ui/
│       │           ├── chat/       # ChatScreen, ChatViewModel, ChatMessageItem, ChatInputBar
│       │           ├── models/     # ModelManagerDialog
│       │           ├── settings/   # InferenceSettingsDialog
│       │           └── theme/      # Material 3 Theme, Color, Type
│       └── test/                  # Unit tests (ViewModel, ModelManager, Config, Engine)
├── gradle/
│   ├── libs.versions.toml         # Version catalog
│   └── wrapper/                   # Gradle wrapper distribution configuration
├── build.gradle.kts               # Root build script
├── settings.gradle.kts            # Project settings
└── gradle.properties              # JVM heap settings and AndroidX configuration
```

---

## Supported Models

The app supports any model converted to the **MediaPipe Tasks GenAI** format (`.bin` or `.task`), including:

1. **Gemma 2B / Gemma-2 2B** (`gemma-2b-it-cpu.bin`, `gemma-2b-it-gpu.bin`)
2. **Phi-2 (2.7B)**
3. **Falcon 1B**

### Where to get MediaPipe models:
- **Kaggle**: [Google Gemma Models for MediaPipe](https://www.kaggle.com/models/google/gemma/tfLite)
- **HuggingFace**: MediaPipe GenAI quantized `.bin` models (e.g. `google/gemma-2b-it`)
- **Direct conversion**: Use the [MediaPipe Python Model Maker](https://developers.google.com/mediapipe/solutions/genai/llm_inference) to convert PyTorch / HuggingFace Safetensors checkpoints into 4-bit/8-bit quantized `.bin` files.

---

## How to Load Models into the App

1. **Via In-App File Picker**:
   - Download a `.bin` or `.task` model file on your phone (or push via ADB: `adb push gemma-2b.bin /sdcard/Download/`).
   - Open the app, tap the **Layers icon** in the top bar.
   - Tap **"Import File"** and select the `.bin` or `.task` file.
   - The file will be imported into the app's internal sandbox and loaded into memory.

2. **Via In-App Downloader**:
   - Tap the **Layers icon** -> **"Download"**.
   - Paste a direct HTTP/HTTPS URL pointing to a `.bin` or `.task` file.
   - Tap **"Start Download"**.

---

## Building Locally

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
The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## GitHub Actions CI Workflow

The workflow at `.github/workflows/build-apk.yml` automatically triggers on every `push` and `pull_request` to `main`/`master`, as well as on manual trigger (`workflow_dispatch`).

1. Checks out the code and sets up **Java 17 (Temurin)**.
2. Caches Gradle dependencies with `gradle/actions/setup-gradle@v3`.
3. Executes unit tests: `./gradlew testDebugUnitTest`.
4. Assembles the debug APK: `./gradlew assembleDebug`.
5. Uploads the generated `app-debug.apk` as a downloadable artifact named `local-ai-debug-apk`.
