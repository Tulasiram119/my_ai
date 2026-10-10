package com.localai.app.data.engine

import com.localai.app.data.model.InferenceConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLlmEngine : ILlmEngine {
    override var isModelLoaded: Boolean = false
    override var currentModelPath: String? = null
    var stopped = false

    override suspend fun loadModel(modelPath: String, config: InferenceConfig): Result<Unit> {
        isModelLoaded = true
        currentModelPath = modelPath
        return Result.success(Unit)
    }

    override fun generateResponseAsync(prompt: String): Flow<InferenceEvent> = flow {
        emit(InferenceEvent.TokenChunk("Hello", "Hello", true))
        emit(InferenceEvent.TokenChunk(" world", "Hello world", false))
        emit(
            InferenceEvent.Completed(
                fullText = "Hello world",
                elapsedMs = 100,
                tokensPerSec = 20.0f,
                firstTokenLatencyMs = 25
            )
        )
    }

    override fun stopGeneration() {
        stopped = true
    }

    override fun close() {
        isModelLoaded = false
        currentModelPath = null
    }
}

class LlmInferenceEngineTest {

    @Test
    fun mockEngine_emitsTokensAndCompletion() = runBlocking {
        val engine = MockLlmEngine()
        engine.loadModel("/path/to/model.bin")
        assertTrue(engine.isModelLoaded)
        assertEquals("/path/to/model.bin", engine.currentModelPath)

        val events = engine.generateResponseAsync("Hi").toList()
        assertEquals(3, events.size)

        val chunk1 = events[0] as InferenceEvent.TokenChunk
        assertEquals("Hello", chunk1.chunk)
        assertTrue(chunk1.isFirstToken)

        val chunk2 = events[1] as InferenceEvent.TokenChunk
        assertEquals(" world", chunk2.chunk)
        assertEquals("Hello world", chunk2.fullText)

        val completed = events[2] as InferenceEvent.Completed
        assertEquals("Hello world", completed.fullText)
        assertEquals(20.0f, completed.tokensPerSec, 0.01f)

        engine.close()
        assertTrue(!engine.isModelLoaded)
    }
}
