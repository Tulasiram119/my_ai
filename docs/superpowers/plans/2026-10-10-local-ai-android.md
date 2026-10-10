# Local AI Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a native Android application in Kotlin using Jetpack Compose that runs LLMs locally on-device with MediaPipe Tasks GenAI, supports importing and downloading model weights, and includes a GitHub Actions CI workflow to build the debug APK.

**Architecture:** Clean architecture using MVVM and Jetpack Compose. A dedicated `LlmInferenceEngine` manages the MediaPipe native inference lifecycle on background dispatchers and streams tokens via Kotlin `Flow`. A `ModelManager` manages internal model storage, SAF content copying, and HTTP streaming downloads.

**Tech Stack:** Kotlin 2.0.21, Android Gradle Plugin 8.7.0, Jetpack Compose (Material 3), Google MediaPipe Tasks GenAI (`com.google.mediapipe:tasks-genai:0.10.14`), Kotlin Coroutines & Flow, GitHub Actions.

**Spec:** [docs/superpowers/specs/2026-10-10-local-ai-android-design.md](file:///home/hst000267/Downloads/personal_projects/my_ai/docs/superpowers/specs/2026-10-10-local-ai-android-design.md)

## Global Constraints
- Target SDK: 34 (Android 14)
- Minimum SDK: 26 (Android 8.0 Oreo, required for MediaPipe GenAI)
- JDK Version: Java 17
- App package name: `com.localai.app`
- UI Toolkit: Jetpack Compose with Material 3 (no XML layouts)
- On-Device Engine: `com.google.mediapipe:tasks-genai:0.10.14`

---

### Task 1: Gradle Project Setup & Build Configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`

**Interfaces:**
- Produces: Root and app-level Gradle build files configured with AGP 8.7.0, Kotlin 2.0.21, Jetpack Compose BOM, Coroutines, and MediaPipe Tasks GenAI dependencies.

- [ ] **Step 1: Create `settings.gradle.kts`**
```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MyAI"
include(":app")
```

- [ ] **Step 2: Create root `build.gradle.kts` and `gradle.properties`**
`build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}
```
`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
```

- [ ] **Step 3: Create version catalog `gradle/libs.versions.toml` & wrapper properties**
`gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.7.0"
kotlin = "2.0.21"
coreKtx = "1.13.1"
lifecycleRuntimeKtx = "2.8.6"
activityCompose = "1.9.3"
composeBom = "2024.09.03"
coroutines = "1.8.1"
mediapipeGenai = "0.10.14"
junit = "4.13.2"
mockk = "1.13.12"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
mediapipe-tasks-genai = { group = "com.google.mediapipe", name = "tasks-genai", version.ref = "mediapipeGenai" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

`gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 4: Create `app/build.gradle.kts` and `app/proguard-rules.pro`**
`app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.localai.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.localai.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)

    // Local LLM on-device engine
    implementation(libs.mediapipe.tasks.genai)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    debugImplementation(libs.androidx.ui.tooling)
}
```

`app/proguard-rules.pro`:
```pro
# Keep MediaPipe JNI native classes
-keep class com.google.mediapipe.tasks.genai.** { *; }
-keepclassmembers class com.google.mediapipe.tasks.genai.** { *; }
```

- [ ] **Step 5: Setup Gradle wrapper executable script `gradlew`**
Ensure `gradlew` script exists and has executable permissions.

- [ ] **Step 6: Commit build configuration**
```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ app/build.gradle.kts app/proguard-rules.pro gradlew
git commit -m "chore: setup gradle build configuration with compose and mediapipe genai"
```

---

### Task 2: Android Manifest, Resources & Theme

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/java/com/localai/app/ui/theme/Color.kt`
- Create: `app/src/main/java/com/localai/app/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/localai/app/ui/theme/Type.kt`

**Interfaces:**
- Produces: Base Android app environment with `largeHeap` enabled, Material 3 Dark/Light themes, application strings, and internet permission for the model downloader.

- [ ] **Step 1: Create `app/src/main/AndroidManifest.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@android:drawable/sym_def_app_icon"
        android:label="@string/app_name"
        android:roundIcon="@android:drawable/sym_def_app_icon"
        android:supportsRtl="true"
        android:largeHeap="true"
        android:theme="@android:style/Theme.Material.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize"
            android:theme="@android:style/Theme.Material.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 2: Create strings and color resources**
`app/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Local AI</string>
    <string name="no_model_selected">No model selected</string>
    <string name="prompt_placeholder">Type a message...</string>
    <string name="send">Send</string>
    <string name="stop">Stop</string>
    <string name="load_model">Load Model</string>
    <string name="download_model">Download Model</string>
    <string name="import_model">Import Model File</string>
</resources>
```
`app/src/main/res/values/colors.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="primary">#6750A4</color>
    <color name="background">#1C1B1F</color>
</resources>
```

- [ ] **Step 3: Create Compose Theme files**
`Color.kt`, `Type.kt`, and `Theme.kt` providing a modern dark/light Material 3 theme.

- [ ] **Step 4: Commit UI resources and theme**
```bash
git add app/src/main/AndroidManifest.xml app/src/main/res/ app/src/main/java/com/localai/app/ui/theme/
git commit -m "feat: setup android manifest, resources and material 3 theme"
```

---

### Task 3: Data Models & Configuration

**Files:**
- Create: `app/src/main/java/com/localai/app/data/model/ChatMessage.kt`
- Create: `app/src/main/java/com/localai/app/data/model/InferenceConfig.kt`
- Create: `app/src/main/java/com/localai/app/data/model/ModelInfo.kt`
- Test: `app/src/test/java/com/localai/app/data/model/InferenceConfigTest.kt`

**Interfaces:**
- Produces:
  - `ChatMessage(id: String, sender: Sender, content: String, timestamp: Long, tokensPerSec: Float?, latencyMs: Long?, isStreaming: Boolean)`
  - `InferenceConfig(maxTokens: Int = 512, temperature: Float = 0.8f, topK: Int = 40, randomSeed: Int = 0)`
  - `ModelInfo(name: String, filePath: String, sizeBytes: Long, isLoaded: Boolean)`

- [ ] **Step 1: Write unit test for `InferenceConfig` validation**
Validate that bounds clamping works for temperature (0.1f..2.0f), topK (1..100), and maxTokens (32..4096).

- [ ] **Step 2: Implement `ChatMessage.kt`, `InferenceConfig.kt`, `ModelInfo.kt`**
Implement data classes and validation methods.

- [ ] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest --tests com.localai.app.data.model.InferenceConfigTest`

- [ ] **Step 4: Commit data models**
```bash
git add app/src/main/java/com/localai/app/data/model/ app/src/test/java/com/localai/app/data/model/
git commit -m "feat: add chat message, inference config and model info data classes"
```

---

### Task 4: Model Manager & File/Download Repository

**Files:**
- Create: `app/src/main/java/com/localai/app/repository/ModelManager.kt`
- Test: `app/src/test/java/com/localai/app/repository/ModelManagerTest.kt`

**Interfaces:**
- Consumes: `android.content.Context`, `ModelInfo`
- Produces:
  - `getAvailableModels(): List<ModelInfo>`
  - `importModelFromUri(uri: Uri, fileName: String, onProgress: (Float) -> Unit): Result<ModelInfo>`
  - `downloadModel(url: String, fileName: String): Flow<DownloadProgress>`
  - `deleteModel(modelInfo: ModelInfo): Boolean`

- [ ] **Step 1: Write unit test for `ModelManager` directory resolution and model discovery**
Write tests validating file scanning and progress calculations.

- [ ] **Step 2: Implement `ModelManager`**
- Manages `context.filesDir/models/`.
- Imports models via `contentResolver.openInputStream(uri)` with byte streaming.
- Implements HTTP download via `HttpURLConnection` or streaming with temporary `.part` file, emitting `DownloadProgress(bytesDownloaded, totalBytes, percent, isComplete, error)`.

- [ ] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest --tests com.localai.app.repository.ModelManagerTest`

- [ ] **Step 4: Commit ModelManager**
```bash
git add app/src/main/java/com/localai/app/repository/ app/src/test/java/com/localai/app/repository/
git commit -m "feat: implement ModelManager for SAF file import and http downloads"
```

---

### Task 5: MediaPipe Local LLM Inference Engine

**Files:**
- Create: `app/src/main/java/com/localai/app/data/engine/LlmInferenceEngine.kt`
- Test: `app/src/test/java/com/localai/app/data/engine/LlmInferenceEngineTest.kt`

**Interfaces:**
- Consumes: `InferenceConfig`, `ModelInfo`
- Produces:
  - `initModel(modelPath: String, config: InferenceConfig): Result<Unit>`
  - `generateResponseAsync(prompt: String): Flow<InferenceEvent>`
  - `stopGeneration()`
  - `close()`

- [ ] **Step 1: Write unit test for `LlmInferenceEngine` state handling and event dispatching**
Test that event classes (`TokenChunk`, `Completed`, `Error`) are emitted correctly.

- [ ] **Step 2: Implement `LlmInferenceEngine`**
- Uses `com.google.mediapipe.tasks.genai.llminference.LlmInference`.
- Initializes `LlmInferenceOptions` with specified model path, temperature, topK, and maxTokens.
- Implements async token streaming using `callbackFlow` or `Channel` with `resultListener` and `errorListener`.
- Calculates first token latency in milliseconds and tokens/second generation speed.
- Implements thread-safe resource cleanup on `close()`.

- [ ] **Step 3: Run unit tests**
Run: `./gradlew testDebugUnitTest --tests com.localai.app.data.engine.LlmInferenceEngineTest`

- [ ] **Step 4: Commit LlmInferenceEngine**
```bash
git add app/src/main/java/com/localai/app/data/engine/ app/src/test/java/com/localai/app/data/engine/
git commit -m "feat: implement LlmInferenceEngine wrapping MediaPipe GenAI"
```

---

### Task 6: UI Components & ViewModel

**Files:**
- Create: `app/src/main/java/com/localai/app/ui/chat/ChatViewModel.kt`
- Create: `app/src/main/java/com/localai/app/ui/chat/ChatScreen.kt`
- Create: `app/src/main/java/com/localai/app/ui/chat/ChatMessageItem.kt`
- Create: `app/src/main/java/com/localai/app/ui/chat/ChatInputBar.kt`
- Create: `app/src/main/java/com/localai/app/ui/models/ModelManagerDialog.kt`
- Create: `app/src/main/java/com/localai/app/ui/settings/InferenceSettingsDialog.kt`
- Create: `app/src/main/java/com/localai/app/MainActivity.kt`
- Test: `app/src/test/java/com/localai/app/ui/chat/ChatViewModelTest.kt`

**Interfaces:**
- Consumes: `LlmInferenceEngine`, `ModelManager`
- Produces:
  - `ChatUiState(messages, isGenerating, activeModel, inferenceConfig, availableModels, downloadProgress, errorMessage)`
  - Modern Jetpack Compose UI with top app bar, message list, token speed chips, bottom bar, and dialogs.

- [ ] **Step 1: Write unit test for `ChatViewModel` message queueing and state transitions**
Test sending message, receiving token updates, clearing history, and error states.

- [ ] **Step 2: Implement `ChatViewModel`**
- Manages `_uiState: MutableStateFlow<ChatUiState>`.
- Coroutine-driven inference execution with `viewModelScope`.
- Handles model selection, download triggers, and settings updates.

- [ ] **Step 3: Implement Compose UI components**
- `ChatMessageItem`: User and Assistant bubbles, markdown styling, token generation metrics badge (e.g., `⚡ 18.4 tokens/s (240ms)`).
- `ChatInputBar`: Text input, send button, stop button during generation.
- `ModelManagerDialog`: Dialog showing available models, file picker button, URL download field with progress bar.
- `InferenceSettingsDialog`: Sliders for temperature, topK, max tokens.
- `ChatScreen`: Scaffold with TopAppBar, menu actions, lazy list of messages, bottom bar.
- `MainActivity`: Jetpack Compose entry point initializing ViewModel and setting content.

- [ ] **Step 4: Run unit tests**
Run: `./gradlew testDebugUnitTest --tests com.localai.app.ui.chat.ChatViewModelTest`

- [ ] **Step 5: Commit UI and ViewModel**
```bash
git add app/src/main/java/com/localai/app/ui/ app/src/main/java/com/localai/app/MainActivity.kt app/src/test/java/com/localai/app/ui/
git commit -m "feat: implement ChatViewModel, Compose screens and dialogs"
```

---

### Task 7: GitHub Actions CI Workflow for Building APK

**Files:**
- Create: `.github/workflows/build-apk.yml`

**Interfaces:**
- Consumes: Entire git repository with Gradle setup.
- Produces: GitHub Actions workflow compiling `assembleDebug` and uploading `app-debug.apk` artifact.

- [ ] **Step 1: Create `.github/workflows/build-apk.yml`**
```yaml
name: Build Android APK

on:
  push:
    branches: [ "main", "master" ]
  pull_request:
    branches: [ "main", "master" ]
  workflow_dispatch:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  build:
    name: Build Debug APK
    runs-on: ubuntu-latest

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.9'

      - name: Grant Execute Permission for Gradlew
        run: chmod +x gradlew

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest --no-daemon --stacktrace

      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      - name: Upload Debug APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: local-ai-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
          retention-days: 30
```

- [ ] **Step 2: Commit GitHub Actions workflow**
```bash
git add .github/workflows/build-apk.yml
git commit -m "ci: add GitHub Actions workflow to run tests, build debug APK, and upload artifact"
```

---

### Task 8: Verification & Documentation

**Files:**
- Create: `README.md`
- Test: Local build and compilation verification

**Interfaces:**
- Produces: Complete setup instructions, supported models guide (Gemma 2B, Phi-2, etc.), and verification of project integrity.

- [ ] **Step 1: Create comprehensive `README.md`**
Document architecture, how to run the app, how to download or transfer `.bin`/`.task` models (Gemma 2B, Gemma-2 2B), MediaPipe format instructions, and CI workflow details.

- [ ] **Step 2: Run verification**
Verify all Kotlin files, Gradle build files, and CI YAML files are structurally sound.

- [ ] **Step 3: Commit README and final project changes**
```bash
git add README.md
git commit -m "docs: add README with architecture, model setup guide, and CI instructions"
```
