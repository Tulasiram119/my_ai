package com.localai.app.data.engine

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.localai.app.data.model.InferenceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

sealed interface InferenceEvent {
    data class TokenChunk(
        val chunk: String,
        val fullText: String,
        val isFirstToken: Boolean
    ) : InferenceEvent

    data class Completed(
        val fullText: String,
        val elapsedMs: Long,
        val tokensPerSec: Float,
        val firstTokenLatencyMs: Long
    ) : InferenceEvent

    data class Error(val throwable: Throwable) : InferenceEvent
}

interface ILlmEngine {
    val isModelLoaded: Boolean
    val currentModelPath: String?
    suspend fun loadModel(modelPath: String, config: InferenceConfig = InferenceConfig.DEFAULT): Result<Unit>
    fun generateResponseAsync(prompt: String): Flow<InferenceEvent>
    fun stopGeneration()
    fun close()
}

class MediaPipeLlmInferenceEngine(
    private val context: Context
) : ILlmEngine {

    private var llmInference: LlmInference? = null
    override var currentModelPath: String? = null
        private set

    private var currentConfig: InferenceConfig = InferenceConfig.DEFAULT
    private val isGenerating = AtomicBoolean(false)
    private val shouldStop = AtomicBoolean(false)

    override val isModelLoaded: Boolean
        get() = llmInference != null

    override suspend fun loadModel(
        modelPath: String,
        config: InferenceConfig
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val file = File(modelPath)
            if (!file.exists() || !file.canRead()) {
                throw IllegalArgumentException("Model file does not exist or cannot be read: $modelPath")
            }
            if (file.length() < 1024 * 1024) {
                throw IllegalArgumentException("Model file is suspiciously small (${file.length()} bytes)")
            }

            // Close existing instance if any
            close()

            currentConfig = config.validated()
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(file.absolutePath)
                .setMaxTokens(currentConfig.maxTokens)
                .setTemperature(currentConfig.temperature)
                .setTopK(currentConfig.topK)
                .setRandomSeed(currentConfig.randomSeed)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = modelPath
        }
    }

    override fun generateResponseAsync(prompt: String): Flow<InferenceEvent> = callbackFlow {
        val engine = llmInference
        if (engine == null) {
            trySend(InferenceEvent.Error(IllegalStateException("No model loaded. Please select a model first.")))
            close()
            return@callbackFlow
        }

        if (isGenerating.getAndSet(true)) {
            trySend(InferenceEvent.Error(IllegalStateException("Inference already in progress.")))
            close()
            return@callbackFlow
        }

        shouldStop.set(false)
        val startTime = System.currentTimeMillis()
        var firstTokenTime: Long? = null
        val fullTextBuilder = StringBuilder()
        var tokenCount = 0

        // Create temporary options with streaming listeners
        val streamingOptions = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(currentModelPath!!)
            .setMaxTokens(currentConfig.maxTokens)
            .setTemperature(currentConfig.temperature)
            .setTopK(currentConfig.topK)
            .setRandomSeed(currentConfig.randomSeed)
            .setResultListener { partialResult, isDone ->
                if (shouldStop.get()) {
                    isGenerating.set(false)
                    val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                    val firstLatency = firstTokenTime?.let { it - startTime } ?: elapsed
                    val speed = (tokenCount * 1000f) / elapsed
                    trySend(
                        InferenceEvent.Completed(
                            fullText = fullTextBuilder.toString(),
                            elapsedMs = elapsed,
                            tokensPerSec = speed,
                            firstTokenLatencyMs = firstLatency
                        )
                    )
                    close()
                    return@setResultListener
                }

                if (partialResult.isNotEmpty()) {
                    val isFirst = firstTokenTime == null
                    if (isFirst) {
                        firstTokenTime = System.currentTimeMillis()
                    }
                    tokenCount++
                    fullTextBuilder.append(partialResult)
                    trySend(
                        InferenceEvent.TokenChunk(
                            chunk = partialResult,
                            fullText = fullTextBuilder.toString(),
                            isFirstToken = isFirst
                        )
                    )
                }

                if (isDone) {
                    isGenerating.set(false)
                    val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                    val firstLatency = firstTokenTime?.let { it - startTime } ?: elapsed
                    val speed = (tokenCount * 1000f) / elapsed
                    trySend(
                        InferenceEvent.Completed(
                            fullText = fullTextBuilder.toString(),
                            elapsedMs = elapsed,
                            tokensPerSec = speed,
                            firstTokenLatencyMs = firstLatency
                        )
                    )
                    close()
                }
            }
            .setErrorListener { error ->
                isGenerating.set(false)
                trySend(InferenceEvent.Error(error))
                close()
            }
            .build()

        try {
            val streamingEngine = LlmInference.createFromOptions(context, streamingOptions)
            streamingEngine.generateResponseAsync(prompt)
        } catch (e: Throwable) {
            isGenerating.set(false)
            trySend(InferenceEvent.Error(e))
            close()
        }

        awaitClose {
            shouldStop.set(true)
            isGenerating.set(false)
        }
    }.flowOn(Dispatchers.Default)

    override fun stopGeneration() {
        shouldStop.set(true)
        isGenerating.set(false)
    }

    override fun close() {
        stopGeneration()
        llmInference?.close()
        llmInference = null
        currentModelPath = null
    }
}
