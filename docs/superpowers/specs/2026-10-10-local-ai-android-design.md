# Design Spec: Local AI Android App (MediaPipe GenAI & GitHub Actions CI)

## 1. Overview
The goal of this project is to create a native Android application built entirely in Kotlin with Jetpack Compose that runs Large Language Models (LLMs) completely on-device without any internet or cloud connection for inference. The project also provides a complete GitHub Actions CI workflow to automatically build and produce the debug APK as an artifact.

## 2. Requirements & Goals
- **Local On-Device LLM**: Run LLM inference locally on Android hardware (CPU/GPU) using Google MediaPipe Tasks GenAI (`com.google.mediapipe:tasks-genai`).
- **Modern Kotlin & Jetpack Compose UI**: Material 3 interface featuring:
  - Real-time token streaming chat interface.
  - Performance metrics display (tokens per second, latency).
  - Stop generation capability.
  - Parameter customization (temperature, top-k, max tokens).
- **Flexible Model Acquisition**:
  - Storage Access Framework (SAF) file picker to import `.bin` or `.task` models (e.g., Gemma 2B, Gemma-2 2B, Phi-2) from device storage into app-private sandbox.
  - In-app downloader with real-time download progress and direct URL support.
- **GitHub Actions CI/CD**: Complete CI pipeline that checks out the repo, sets up JDK 17, builds the debug APK with Gradle, and uploads the `.apk` file as a workflow artifact.

## 3. System Architecture & Components

### 3.1 Directory Structure
```
my_ai/
├── .github/
│   └── workflows/
│       └── build-apk.yml          # GitHub Actions workflow
├── app/
│   ├── build.gradle.kts           # App-level dependencies
│   ├── proguard-rules.pro         # Proguard/R8 rules preserving MediaPipe JNI
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml # Permissions (INTERNET, largeHeap)
│       │   ├── res/               # Icons, strings, themes
│       │   └── java/com/localai/app/
│       │       ├── data/
│       │       │   ├── model/     # ChatMessage, InferenceConfig, ModelInfo
│       │       │   └── engine/    # LlmInferenceEngine (MediaPipe Tasks GenAI wrapper)
│       │       ├── repository/    # ModelManager (local storage, SAF import, HTTP downloader)
│       │       ├── ui/
│       │       │   ├── theme/     # Material 3 Theme (Colors, Typography, Shapes)
│       │       │   ├── chat/      # ChatScreen, ChatViewModel, ChatComponents
│       │       │   ├── models/    # ModelManagerScreen, DownloadDialog
│       │       │   └── settings/  # InferenceSettingsDialog
│       │       └── MainActivity.kt
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts               # Root build script
├── settings.gradle.kts            # Project settings
└── gradle.properties              # JVM options & AndroidX settings
```

### 3.2 Core Modules

#### `LlmInferenceEngine`
- Wraps `com.google.mediapipe.tasks.genai.llminference.LlmInference`.
- Thread isolation: Executes model loading and inference on `Dispatchers.Default`.
- Streaming token pipeline: Implements a Kotlin coroutine callback flow emitting incremental token updates and completion events.
- Generation metrics: Measures start-to-first-token latency (ms) and throughput (tokens/sec).

#### `ModelManager`
- Manages models stored inside `context.filesDir/models/`.
- SAF importer: Safely streams external `.bin`/`.task` files from `content://` URIs to internal storage.
- HTTP Downloader: Streams model downloads via `HttpURLConnection` or OkHttp into temporary `.part` files, renaming on completion, supporting cancellation and progress updates.

#### `ChatViewModel`
- Manages conversation state: `StateFlow<ChatUiState>`.
- Coordinates inference lifecycle: `sendMessage()`, `stopGeneration()`, `clearHistory()`.
- Updates UI state reactively with partial responses and generation stats.

### 3.3 UI Components (Jetpack Compose)
- **ChatScreen**: TopAppBar with model status & settings action, lazy list of chat bubbles (user & AI), markdown/code formatted responses, bottom prompt input field with send and stop buttons.
- **ModelSheet / ModelScreen**: Displays available models, lets the user import a file from device storage, or trigger a direct download with progress indicators.
- **SettingsDialog**: Slider controls for Temperature (0.1–2.0), Top-K (1–100), and Max Tokens (64–2048).

## 4. Error Handling & Robustness
- **OOM Management**: Sets `android:largeHeap="true"` in `AndroidManifest.xml` and validates file existence and size before initializing MediaPipe.
- **Graceful Error Recovery**: Catches engine initialization errors (corrupted weights, unsupported architecture) and surfaces user-friendly snackbars/dialogs.
- **Cancellation**: Cancels active coroutine jobs when the user presses "Stop" or navigates away.
- **Interrupted Downloads**: Incomplete files saved as `.part` are removed if download fails or is cancelled.

## 5. GitHub Actions CI Configuration
- **File**: `.github/workflows/build-apk.yml`
- **Runner**: `ubuntu-latest`
- **JDK**: Java 17 (Eclipse Temurin)
- **Cache**: Gradle caches (`~/.gradle/caches`, `~/.gradle/wrapper`)
- **Step**: `./gradlew assembleDebug --no-daemon --stacktrace`
- **Artifact**: Uploads `app/build/outputs/apk/debug/app-debug.apk` with name `local-ai-debug-apk`.

## 6. Verification Plan
- Verify Gradle project syntax and build files.
- Verify GitHub Actions workflow YAML syntax and step declarations.
- Verify clean code compilation and absence of unresolved references.
