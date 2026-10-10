# Open-Source Models Hub & GGUF Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide an effortless 1-tap Model Hub to browse and download popular open-source models (Qwen 2.5, Llama 3.2, SmolLM2) directly in-app, backed by an engine abstraction supporting GGUF weights.

**Architecture:** An interface-driven engine layer (`LlmEngine`) decouples inference logic from Android UI/ViewModel. A curated repository catalog (`CuratedModels`) defines verified mobile-optimized Hugging Face models. `ModelManager` streams and validates `.gguf` downloads with progress tracking and cancellation. Jetpack Compose UI provides a two-tab dialog ("Model Hub" for 1-tap download and "Installed" for loading/managing).

**Tech Stack:** Kotlin 2.0, Jetpack Compose (Material 3), Coroutines Flow, Android ContentResolver/SAF, HttpURLConnection, JUnit 4, MockK.

**Spec:** [`docs/superpowers/specs/2026-10-10-open-source-models-catalog-design.md`](file:///home/hst000267/Downloads/personal_projects/my_ai/docs/superpowers/specs/2026-10-10-open-source-models-catalog-design.md)

## Global Constraints
- Supported Model Formats: `.gguf`, `.bin`, `.task`
- Target JVM: Java 17
- Minimum Android SDK: 26 (compileSdk: 34)
- Unit tests must run on JVM without native NDK dependencies via `FakeLlmEngine`
- Zero placeholders (`TODO`, `TBD`) in production code

---

### Task 1: Core Engine Abstraction (`LlmEngine`) & Fake Engine for Testing

**Files:**
- Create: `app/src/main/java/com/localai/app/data/engine/LlmEngine.kt`
- Create: `app/src/test/java/com/localai/app/data/engine/FakeLlmEngine.kt`
- Modify: `app/src/main/java/com/localai/app/data/engine/LlmInferenceEngine.kt`
- Create: `app/src/test/java/com/localai/app/data/engine/LlmEngineTest.kt`

**Interfaces:**
- Produces: `LlmEngine` interface with `isLoaded: Boolean`, `currentModelPath: String?`, `loadModel(modelPath: String, config: InferenceConfig): Result<Unit>`, `generateStreaming(prompt: String, config: InferenceConfig): Flow<GenerationEvent>`, `stopGeneration()`, `release()`.

- [ ] **Step 1: Write the failing test for `LlmEngine` contract**

```kotlin
// app/src/test/java/com/localai/app/data/engine/LlmEngineTest.kt
package com.localai.app.data.engine

import com.localai.app.data.model.GenerationEvent
import com.localai.app.data.model.InferenceConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LlmEngineTest {
    @Test
    fun fakeEngine_loadAndStreamGeneration() = runTest {
        val engine = FakeLlmEngine()
        assertFalse(engine.isLoaded)

        val loadResult = engine.loadModel("/path/to/qwen2.5.gguf")
        assertTrue(loadResult.isSuccess)
        assertTrue(engine.isLoaded)
        assertEquals("/path/to/qwen2.5.gguf", engine.currentModelPath)

        val events = engine.generateStreaming("Hello").toList()
        assertTrue(events.isNotEmpty())
        assertTrue(events.any { it is GenerationEvent.Token })
        assertTrue(events.last() is GenerationEvent.Completed)

        engine.release()
        assertFalse(engine.isLoaded)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.localai.app.data.engine.LlmEngineTest"`  
Expected: Compilation failure because `LlmEngine` and `FakeLlmEngine` do not exist yet.

- [ ] **Step 3: Implement `LlmEngine.kt` and `FakeLlmEngine.kt`, and adapt `LlmInferenceEngine.kt`**

```kotlin
// app/src/main/java/com/localai/app/data/engine/LlmEngine.kt
package com.localai.app.data.engine

import com.localai.app.data.model.GenerationEvent
import com.localai.app.data.model.InferenceConfig
import kotlinx.coroutines.flow.Flow

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

```kotlin
// app/src/test/java/com/localai/app/data/engine/FakeLlmEngine.kt
package com.localai.app.data.engine

import com.localai.app.data.model.GenerationEvent
import com.localai.app.data.model.InferenceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeLlmEngine : LlmEngine {
    override var isLoaded: Boolean = false
        private set

    override var currentModelPath: String? = null
        private set

    var shouldFailLoad: Boolean = false
    var simulatedTokens: List<String> = listOf("Hello", " there", "!")

    override suspend fun loadModel(modelPath: String, config: InferenceConfig): Result<Unit> {
        if (shouldFailLoad) {
            return Result.failure(IllegalStateException("Simulated engine load failure"))
        }
        isLoaded = true
        currentModelPath = modelPath
        return Result.success(Unit)
    }

    override fun generateStreaming(prompt: String, config: InferenceConfig): Flow<GenerationEvent> = flow {
        simulatedTokens.forEach { token ->
            emit(GenerationEvent.Token(token))
        }
        emit(GenerationEvent.Completed(fullText = simulatedTokens.joinToString(""), promptTokens = 5, responseTokens = 3, latencyMs = 120L, tokensPerSecond = 25.0))
    }

    override fun stopGeneration() {}

    override fun release() {
        isLoaded = false
        currentModelPath = null
    }
}
```

Update `LlmInferenceEngine.kt` to implement `LlmEngine`.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.localai.app.data.engine.LlmEngineTest"`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/localai/app/data/engine/ app/src/test/java/com/localai/app/data/engine/
git commit -m "feat: add LlmEngine interface and FakeLlmEngine test double"
```

---

### Task 2: Curated Models Catalog Data Models & Presets

**Files:**
- Create: `app/src/main/java/com/localai/app/data/model/CuratedModel.kt`
- Create: `app/src/test/java/com/localai/app/data/model/CuratedModelTest.kt`

**Interfaces:**
- Produces: `CuratedModel` data class with all catalog metadata and `CuratedModels.list` containing verified Qwen 2.5, Llama 3.2, and SmolLM2 models.

- [ ] **Step 1: Write the failing test for `CuratedModel` validation**

```kotlin
// app/src/test/java/com/localai/app/data/model/CuratedModelTest.kt
package com.localai.app.data.model

import org.junit.Assert.*
import org.junit.Test

class CuratedModelTest {
    @Test
    fun curatedModels_containsQwenAndValidHuggingFaceUrls() {
        val models = CuratedModels.list
        assertTrue(models.isNotEmpty())

        val qwen = models.find { it.id.contains("qwen", ignoreCase = true) }
        assertNotNull(qwen)
        assertEquals("Q4_K_M", qwen?.quantization)
        assertTrue(qwen!!.downloadUrl.startsWith("https://huggingface.co/"))
        assertTrue(qwen.targetFileName.endsWith(".gguf"))
        assertTrue(qwen.fileSizeBytes > 0)
    }

    @Test
    fun allCuratedModels_haveUniqueIdsAndValidMetadata() {
        val ids = CuratedModels.list.map { it.id }
        assertEquals(ids.size, ids.distinct().size)

        CuratedModels.list.forEach { model ->
            assertTrue(model.name.isNotBlank())
            assertTrue(model.publisher.isNotBlank())
            assertTrue(model.downloadUrl.startsWith("https://"))
            assertTrue(model.targetFileName.endsWith(".gguf"))
            assertTrue(model.recommendedRam.isNotBlank())
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.localai.app.data.model.CuratedModelTest"`  
Expected: Compilation failure because `CuratedModel` and `CuratedModels` are not yet created.

- [ ] **Step 3: Implement `CuratedModel.kt` with curated presets**

```kotlin
// app/src/main/java/com/localai/app/data/model/CuratedModel.kt
package com.localai.app.data.model

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

object CuratedModels {
    val list: List<CuratedModel> = listOf(
        CuratedModel(
            id = "qwen2.5-0.5b-instruct",
            name = "Qwen 2.5 0.5B Instruct",
            publisher = "Alibaba Cloud",
            description = "Ultra-fast and lightweight chat assistant. Ideal for phones with 2-4GB RAM.",
            parameterSize = "0.5B",
            quantization = "Q4_K_M",
            fileSizeBytes = 398_000_000L,
            formattedSize = "398 MB",
            recommendedRam = "~800 MB RAM",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            targetFileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        ),
        CuratedModel(
            id = "qwen2.5-1.5b-instruct",
            name = "Qwen 2.5 1.5B Instruct",
            publisher = "Alibaba Cloud",
            description = "Strong reasoning, coding, and multilingual knowledge in a compact mobile package.",
            parameterSize = "1.5B",
            quantization = "Q4_K_M",
            fileSizeBytes = 986_000_000L,
            formattedSize = "986 MB",
            recommendedRam = "~1.5 GB RAM",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            targetFileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
        ),
        CuratedModel(
            id = "llama3.2-1b-instruct",
            name = "Llama 3.2 1B Instruct",
            publisher = "Meta",
            description = "Meta's highly optimized on-device lightweight conversational model.",
            parameterSize = "1B",
            quantization = "Q4_K_M",
            fileSizeBytes = 808_000_000L,
            formattedSize = "808 MB",
            recommendedRam = "~1.2 GB RAM",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            targetFileName = "llama-3.2-1b-instruct-q4_k_m.gguf"
        ),
        CuratedModel(
            id = "smollm2-1.7b-instruct",
            name = "SmolLM2 1.7B Instruct",
            publisher = "Hugging Face",
            description = "High-performing, responsive compact model tuned for local mobile execution.",
            parameterSize = "1.7B",
            quantization = "Q4_K_M",
            fileSizeBytes = 1_060_000_000L,
            formattedSize = "1.06 GB",
            recommendedRam = "~1.6 GB RAM",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
            targetFileName = "smollm2-1.7b-instruct-q4_k_m.gguf"
        ),
        CuratedModel(
            id = "phi3.5-mini-instruct",
            name = "Phi-3.5 Mini 3.8B Instruct",
            publisher = "Microsoft",
            description = "Flagship-grade reasoning and mathematics. Recommended for 8GB+ RAM devices.",
            parameterSize = "3.8B",
            quantization = "Q4_K_M",
            fileSizeBytes = 2_390_000_000L,
            formattedSize = "2.39 GB",
            recommendedRam = "~3.5 GB RAM",
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            targetFileName = "phi-3.5-mini-instruct-q4_k_m.gguf"
        )
    )
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.localai.app.data.model.CuratedModelTest"`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/localai/app/data/model/CuratedModel.kt app/src/test/java/com/localai/app/data/model/CuratedModelTest.kt
git commit -m "feat: add CuratedModel data model and official presets"
```

---

### Task 3: ModelManager Enhancements (GGUF Discovery, Hugging Face Streaming, Status Check)

**Files:**
- Modify: `app/src/main/java/com/localai/app/repository/ModelManager.kt`
- Modify: `app/src/test/java/com/localai/app/repository/ModelManagerTest.kt`

**Interfaces:**
- Consumes: `CuratedModel`, `CuratedModels.list`
- Produces: `getAvailableModels(): List<ModelInfo>` (supports `.gguf`, `.bin`, `.task`), `isModelInstalled(targetFileName: String): Boolean`, `downloadCuratedModel(model: CuratedModel): Flow<DownloadProgress>`.

- [ ] **Step 1: Write test for GGUF model scanning and curated status check**

```kotlin
// In app/src/test/java/com/localai/app/repository/ModelManagerTest.kt
@Test
fun discoversGgufFilesCorrectly() = runTest {
    val tempDir = createTempDir()
    val ggufFile = File(tempDir, "qwen2.5-0.5b.gguf").apply { writeText("dummy gguf") }
    val binFile = File(tempDir, "gemma.bin").apply { writeText("dummy bin") }
    val txtFile = File(tempDir, "ignore.txt").apply { writeText("dummy txt") }

    val manager = ModelManager(context, baseDir = tempDir)
    val models = manager.getAvailableModels()

    assertEquals(2, models.size)
    assertTrue(models.any { it.name == "qwen2.5-0.5b.gguf" })
    assertTrue(models.any { it.name == "gemma.bin" })
    assertFalse(models.any { it.name == "ignore.txt" })
    assertTrue(manager.isModelInstalled("qwen2.5-0.5b.gguf"))
    assertFalse(manager.isModelInstalled("not-installed.gguf"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.localai.app.repository.ModelManagerTest"`  
Expected: FAIL (`isModelInstalled` not defined, `.gguf` extension not recognized).

- [ ] **Step 3: Update `ModelManager.kt`**

Update `validExtensions = listOf("gguf", "bin", "task")`.  
Add helper `isModelInstalled(targetFileName: String): Boolean`:
```kotlin
fun isModelInstalled(targetFileName: String): Boolean {
    val file = File(modelsDirectory, targetFileName)
    return file.exists() && file.length() > 0
}
```
Add `downloadCuratedModel(model: CuratedModel): Flow<DownloadProgress>`:
```kotlin
fun downloadCuratedModel(model: CuratedModel): Flow<DownloadProgress> {
    return downloadModel(model.downloadUrl, model.targetFileName)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.localai.app.repository.ModelManagerTest"`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/localai/app/repository/ModelManager.kt app/src/test/java/com/localai/app/repository/ModelManagerTest.kt
git commit -m "feat: add .gguf extension support and curated model status checking to ModelManager"
```

---

### Task 4: ChatViewModel Refactoring with Engine Abstraction & Model State

**Files:**
- Modify: `app/src/main/java/com/localai/app/ui/chat/ChatViewModel.kt`
- Modify: `app/src/test/java/com/localai/app/ui/chat/ChatViewModelTest.kt`

**Interfaces:**
- Consumes: `LlmEngine`, `ModelManager`, `CuratedModels`
- Produces: Updated `ChatViewModel(engine: LlmEngine, modelManager: ModelManager)` and `ChatUiState` with download progress, curated models list, and model installation status.

- [ ] **Step 1: Write unit tests in `ChatViewModelTest.kt` using `FakeLlmEngine`**

```kotlin
// In app/src/test/java/com/localai/app/ui/chat/ChatViewModelTest.kt
@Test
fun selectModel_loadsViaEngineInterface() = runTest {
    val fakeEngine = FakeLlmEngine()
    val viewModel = ChatViewModel(fakeEngine, modelManager)

    val model = ModelInfo("qwen.gguf", "/path/qwen.gguf", 1000L)
    viewModel.selectModel(model)

    advanceUntilIdle()
    assertEquals(model, viewModel.uiState.value.activeModel)
    assertTrue(fakeEngine.isLoaded)
}
```

- [ ] **Step 2: Run test to verify it fails or needs constructor update**

Run: `./gradlew testDebugUnitTest --tests "com.localai.app.ui.chat/ChatViewModelTest"`  
Expected: Test fails if `ChatViewModel` expects concrete `LlmInferenceEngine`.

- [ ] **Step 3: Update `ChatViewModel.kt` to depend on `LlmEngine` interface**

Change constructor:
```kotlin
class ChatViewModel(
    private val engine: LlmEngine,
    private val modelManager: ModelManager
) : ViewModel() {
    // expose curated models and download progress
    val curatedModels: List<CuratedModel> = CuratedModels.list
    // ...
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.localai.app.ui.chat.ChatViewModelTest"`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/localai/app/ui/chat/ChatViewModel.kt app/src/test/java/com/localai/app/ui/chat/ChatViewModelTest.kt
git commit -m "refactor: update ChatViewModel to use LlmEngine interface"
```

---

### Task 5: Tabbed Model Manager Dialog with 1-Tap Download Hub

**Files:**
- Modify: `app/src/main/java/com/localai/app/ui/models/ModelManagerDialog.kt`
- Modify: `app/src/main/java/com/localai/app/ui/chat/ChatScreen.kt`

**Interfaces:**
- Consumes: `CuratedModel`, `CuratedModels.list`, `ModelInfo`, `DownloadProgress`
- Produces: Two-tab `ModelManagerDialog` featuring:
  - Tab 0: **"Model Hub"** with Qwen, Llama, and SmolLM2 cards + 1-tap Download buttons.
  - Tab 1: **"Installed"** with list of locally available `.gguf` models, active selection, delete button, and "Import File" action.

- [ ] **Step 1: Build the curated model item card component in `ModelManagerDialog.kt`**

Add `CuratedModelCard`:
- Model title + publisher tag.
- Size badge (`398 MB`), RAM badge (`~800 MB RAM`), quantization badge (`Q4_K_M`).
- Description text.
- Action button:
  - If installed: Outlined badge with `CheckCircle` "Installed".
  - If currently downloading: LinearProgressIndicator + "Downloading X%".
  - Else: Filled button with `CloudDownload` "Download".

- [ ] **Step 2: Add `PrimaryTabRow` / `TabRow` to `ModelManagerDialog`**

Tab 0: "Model Hub" (shows `CuratedModels.list` and expandable "Custom URL" section).  
Tab 1: "Installed Models" (shows current local `.gguf`/`.bin` files with "Import File" button).

- [ ] **Step 3: Add Empty State Shortcut in `ChatScreen.kt`**

When no model is loaded (`uiState.activeModel == null`), display a prominent banner/button in the center of the chat:
`"Browse Open-Source Models (Qwen, Llama, etc.)"` which directly opens `ModelManagerDialog` to the Model Hub tab.

- [ ] **Step 4: Verify Compose layout builds and unit tests pass**

Run: `./gradlew testDebugUnitTest`  
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/localai/app/ui/models/ModelManagerDialog.kt app/src/main/java/com/localai/app/ui/chat/ChatScreen.kt
git commit -m "feat: add tabbed Model Hub UI with 1-tap download cards for open source models"
```

---

### Task 6: End-to-End Build & CI Verification

**Files:**
- Verify: `.github/workflows/build-apk.yml`
- Verify: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 1: Run complete unit test suite**

Run: `./gradlew testDebugUnitTest`  
Expected: All unit tests execute and pass with 0 failures.

- [ ] **Step 2: Build Debug APK**

Run: `./gradlew assembleDebug`  
Expected: `BUILD SUCCESSFUL` and APK generated at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Commit and push or finalize repository state**

```bash
git status
```
Verify working tree is clean.
