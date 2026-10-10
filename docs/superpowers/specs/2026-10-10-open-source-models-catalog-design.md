# Design Spec: Open-Source Models Hub & GGUF Support (Qwen, Llama, etc.)

## 1. Overview
The goal of this design is to make downloading and using popular open-source Large Language Models (LLMs)—specifically **Qwen 2.5**, **Llama 3.2**, **SmolLM2**, and others—effortless on Android. Instead of hunting for obscure formats or pasting manual URLs, users get an in-app **Model Hub** with 1-tap downloads and native **GGUF** model execution.

## 2. Problem Statement
The initial implementation relied strictly on Google MediaPipe Tasks GenAI with proprietary `.bin`/`.task` files and manual URL entry:
- Popular models like Qwen 2.5, DeepSeek, and modern open-source weights are not natively supported by MediaPipe GenAI.
- Users had to manually discover compatible `.bin` files, figure out direct URLs, or transfer files using ADB/PC.
- The app provided no in-app catalog or guidance on which models fit mobile device RAM.

## 3. Goals & Requirements
1. **Universal Open-Source Model Support**: Support `.gguf` quantized models (Q4_K_M, Q8_0, etc.) via a decoupled engine interface (`LlmEngine`).
2. **1-Tap Curated Model Hub**: In-app catalog featuring mobile-ready presets:
   - **Qwen 2.5 0.5B Instruct** (Q4_K_M, ~398 MB, ideal for any phone)
   - **Qwen 2.5 1.5B Instruct** (Q4_K_M, ~986 MB, strong general reasoning)
   - **Llama 3.2 1B Instruct** (Q4_K_M, ~800 MB, Meta mobile LLM)
   - **SmolLM2 1.7B Instruct** (Q4_K_M, ~1.0 GB, Hugging Face lightweight model)
   - **Phi-3.5 Mini 3.8B Instruct** (Q4_K_M, ~2.2 GB, flagship tier)
3. **Seamless Download Pipeline**: Direct streaming from official Hugging Face repositories with progress bars, cancellation, and storage validation.
4. **Custom GGUF Flexibility**: Retain system file picker (SAF) and custom URL input for any arbitrary `.gguf` file.
5. **Modern Material 3 UX**: Tabbed Model Manager dialog ("Model Hub" vs "Installed Models") and non-intrusive download status.
6. **Robust Testing & CI**: Unit test coverage for catalog, downloads, and chat lifecycle; guaranteed green GitHub Actions CI.

## 4. Architecture & Component Design

### 4.1 Engine Abstraction (`LlmEngine`)
Decouples inference from the UI and ViewModel:
```kotlin
interface LlmEngine {
    val isLoaded: Boolean
    val currentModelPath: String?

    suspend fun loadModel(
        modelPath: String,
        config: InferenceConfig = InferenceConfig()
    ): Result<Unit>

    fun generateStreaming(
        prompt: String,
        config: InferenceConfig = InferenceConfig()
    ): Flow<GenerationEvent>

    fun stopGeneration()
    fun release()
}
```

Implementations:
- `LlamaEngine`: Native GGUF inference implementation wrapping `llama.cpp` / JNI bindings for Android.
- `FakeLlmEngine`: Mock/stub engine used in JVM unit tests to avoid native library dependencies.

### 4.2 Curated Catalog Data Model
Defined in `com.localai.app.data.model.CuratedModel`:
```kotlin
data class CuratedModel(
    val id: String,
    val name: String,
    val publisher: String,
    val description: String,
    val parameterSize: String,
    val quantization: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val recommendedRam: String,
    val downloadUrl: String,
    val targetFileName: String
)
```

`CuratedModels.list` provides the predefined list of models with direct, verified Hugging Face download URLs.

### 4.3 Model Manager & Storage (`ModelManager`)
- **Directory**: `context.filesDir/models/` storing `.gguf` files.
- **Listing**: Discovers `.gguf` files and checks which curated models are currently installed.
- **Download**: Streams binary data using `HttpURLConnection` following HTTP 301/302 redirects, writes to `.download` temporary file, and atomically moves to target destination. Emits real-time `DownloadProgress(bytesDownloaded, totalBytes, percent, isComplete, error)`.
- **Space Check**: Verifies free disk space before initiating download.
- **Import**: SAF URI stream copy into app models directory.

### 4.4 User Interface (Jetpack Compose)
- **ModelManagerDialog**:
  - Two tabs: **Model Hub** (Browse & 1-tap download) and **Installed** (Activate & delete models).
  - Hub cards show publisher tag, memory recommendation, file size, and status:
    - *Download* button if absent.
    - *Progress bar + Cancel* button if active.
    - *Installed* badge if present on device.
  - Expandable Custom URL input for arbitrary URLs.
  - "Import File" button with system file picker launcher.
- **ChatScreen**:
  - Empty state when no model is active prompts the user with a "Browse Models" shortcut.
  - Top bar active model pill indicates loaded model status.

## 5. Error Handling & Edge Cases
- **Network Interruption**: Incomplete downloads clean up temporary `.download` files. User can re-try.
- **Insufficient Disk Space**: Download fails early with an explicit error message before starting file write.
- **Unsupported Architecture / Corruption**: Load failure surfaces a clear error snackbar without crashing the app.
- **Concurrent Operations**: Prevents starting multiple downloads simultaneously.

## 6. Testing & CI Strategy
- **JVM Unit Tests**:
  - `ModelManagerTest`: Validates model scanning, curated catalog list structure, simulated download flow, deletion safety.
  - `ChatViewModelTest`: Tests engine load, token streaming events, cancellation, and error handling using `FakeLlmEngine`.
- **CI Pipeline**:
  - Validates `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` on GitHub Actions ubuntu-latest runner.
