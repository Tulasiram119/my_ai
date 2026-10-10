package com.localai.app.data.engine

import com.localai.app.data.model.InferenceConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedLlmEngineTest {

    private val mockLlamaEngine = mockk<ILlmEngine>(relaxed = true)
    private val mockMediaPipeEngine = mockk<ILlmEngine>(relaxed = true)

    @Test
    fun routesGgufToLlamaEngine() = runBlocking {
        val unifiedEngine = UnifiedLlmEngine(
            context = mockk(relaxed = true),
            llamaEngine = mockLlamaEngine,
            mediaPipeEngine = mockMediaPipeEngine
        )

        coEvery { mockLlamaEngine.loadModel(any(), any()) } returns Result.success(Unit)
        every { mockLlamaEngine.isModelLoaded } returns true
        every { mockLlamaEngine.currentModelPath } returns "/path/to/qwen2.5.gguf"

        val result = unifiedEngine.loadModel("/path/to/qwen2.5.gguf", InferenceConfig.DEFAULT)
        assertTrue(result.isSuccess)
        assertTrue(unifiedEngine.isModelLoaded)
        assertEquals("/path/to/qwen2.5.gguf", unifiedEngine.currentModelPath)

        coVerify(exactly = 1) { mockLlamaEngine.loadModel("/path/to/qwen2.5.gguf", any()) }
        coVerify(exactly = 0) { mockMediaPipeEngine.loadModel(any(), any()) }
    }

    @Test
    fun routesBinToMediaPipeEngine() = runBlocking {
        val unifiedEngine = UnifiedLlmEngine(
            context = mockk(relaxed = true),
            llamaEngine = mockLlamaEngine,
            mediaPipeEngine = mockMediaPipeEngine
        )

        coEvery { mockMediaPipeEngine.loadModel(any(), any()) } returns Result.success(Unit)
        every { mockMediaPipeEngine.isModelLoaded } returns true
        every { mockMediaPipeEngine.currentModelPath } returns "/path/to/gemma-2b.bin"

        val result = unifiedEngine.loadModel("/path/to/gemma-2b.bin", InferenceConfig.DEFAULT)
        assertTrue(result.isSuccess)
        assertTrue(unifiedEngine.isModelLoaded)
        assertEquals("/path/to/gemma-2b.bin", unifiedEngine.currentModelPath)

        coVerify(exactly = 1) { mockMediaPipeEngine.loadModel("/path/to/gemma-2b.bin", any()) }
        coVerify(exactly = 0) { mockLlamaEngine.loadModel(any(), any()) }
    }
}
