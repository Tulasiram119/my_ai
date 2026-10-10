package com.localai.app.ui.chat

import com.localai.app.data.engine.ILlmEngine
import com.localai.app.data.engine.InferenceEvent
import com.localai.app.data.model.CuratedModels
import com.localai.app.data.model.InferenceConfig
import com.localai.app.data.model.ModelInfo
import com.localai.app.data.model.Sender
import com.localai.app.repository.DownloadProgress
import com.localai.app.repository.ModelManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var llmEngine: ILlmEngine
    private lateinit var modelManager: ModelManager
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        llmEngine = mockk(relaxed = true)
        modelManager = mockk(relaxed = true)

        coEvery { modelManager.getAvailableModels() } returns listOf(
            ModelInfo("test-model.gguf", "/models/test-model.gguf", 1024L)
        )

        viewModel = ChatViewModel(llmEngine, modelManager)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsAvailableModels() = runTest {
        val state = viewModel.uiState.value
        assertEquals(1, state.availableModels.size)
        assertEquals("test-model.gguf", state.availableModels[0].name)
        assertNull(state.activeModel)
        assertFalse(state.isGenerating)
    }

    @Test
    fun curatedModels_exposesQwenAndOtherModels() {
        assertTrue(viewModel.curatedModels.isNotEmpty())
        assertTrue(viewModel.curatedModels.any { it.id.contains("qwen") })
    }

    @Test
    fun loadModel_success_setsActiveModel() = runTest {
        val model = ModelInfo("test-model.gguf", "/models/test-model.gguf", 1024L)
        coEvery { llmEngine.loadModel(model.filePath, any()) } returns Result.success(Unit)

        viewModel.loadModel(model)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.activeModel)
        assertEquals(model.name, state.activeModel?.name)
        assertTrue(state.activeModel?.isLoaded == true)
    }

    @Test
    fun downloadCuratedModel_startsDownload() = runTest {
        val curated = CuratedModels.list.first()
        every { modelManager.downloadModel(curated.downloadUrl, curated.targetFileName) } returns flowOf(
            DownloadProgress(0, 100, 0f, false)
        )

        viewModel.downloadCuratedModel(curated)
        advanceUntilIdle()

        coVerify { modelManager.downloadModel(curated.downloadUrl, curated.targetFileName) }
    }

    @Test
    fun deleteModel_removesModelAndClearsActiveIfMatched() = runTest {
        val model = ModelInfo("test-model.gguf", "/models/test-model.gguf", 1024L, isLoaded = true)
        coEvery { modelManager.deleteModel(model) } returns true
        coEvery { llmEngine.loadModel(any(), any()) } returns Result.success(Unit)

        viewModel.loadModel(model)
        advanceUntilIdle()
        assertEquals(model.name, viewModel.uiState.value.activeModel?.name)

        viewModel.deleteModel(model)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeModel)
    }

    @Test
    fun sendMessage_whenNoModelLoaded_showsError() = runTest {
        coEvery { llmEngine.isModelLoaded } returns false

        viewModel.sendMessage("Hello")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertNotNull(state.errorMessage)
    }

    @Test
    fun sendMessage_whenModelLoaded_streamsResponse() = runTest {
        coEvery { llmEngine.isModelLoaded } returns true
        coEvery { llmEngine.generateResponseAsync("Hi") } returns flow {
            emit(InferenceEvent.TokenChunk("Hey", "Hey", true))
            emit(InferenceEvent.TokenChunk(" there", "Hey there", false))
            emit(InferenceEvent.Completed("Hey there", 150, 15.0f, 30))
        }

        viewModel.sendMessage("Hi")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals(Sender.USER, state.messages[0].sender)
        assertEquals("Hi", state.messages[0].content)

        assertEquals(Sender.ASSISTANT, state.messages[1].sender)
        assertEquals("Hey there", state.messages[1].content)
        assertFalse(state.messages[1].isStreaming)
        assertEquals(15.0f, state.messages[1].tokensPerSec ?: 0f, 0.01f)
        assertFalse(state.isGenerating)
    }

    @Test
    fun clearChat_resetsMessages() = runTest {
        coEvery { llmEngine.isModelLoaded } returns true
        coEvery { llmEngine.generateResponseAsync("Hi") } returns flow {
            emit(InferenceEvent.Completed("Hello", 50, 10f, 10))
        }

        viewModel.sendMessage("Hi")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearChat()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }
}
