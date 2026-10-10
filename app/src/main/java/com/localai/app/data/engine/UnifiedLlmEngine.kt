package com.localai.app.data.engine

import android.content.Context
import com.localai.app.data.model.InferenceConfig
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Composite engine that routes inference to either LlamaEngine (.gguf)
 * or MediaPipeLlmInferenceEngine (.bin / .task) based on the model file format.
 */
class UnifiedLlmEngine(
    private val context: Context,
    private val llamaEngine: ILlmEngine = LlamaEngine(context),
    private val mediaPipeEngine: ILlmEngine = MediaPipeLlmInferenceEngine(context)
) : ILlmEngine {

    private var activeEngine: ILlmEngine? = null

    override val isModelLoaded: Boolean
        get() = activeEngine?.isModelLoaded == true

    override val currentModelPath: String?
        get() = activeEngine?.currentModelPath

    override suspend fun loadModel(
        modelPath: String,
        config: InferenceConfig
    ): Result<Unit> {
        close()
        val extension = File(modelPath).extension.lowercase()
        val targetEngine = if (extension == "gguf") {
            llamaEngine
        } else {
            mediaPipeEngine
        }

        val result = targetEngine.loadModel(modelPath, config)
        if (result.isSuccess) {
            activeEngine = targetEngine
        }
        return result
    }

    override fun generateResponseAsync(prompt: String): Flow<InferenceEvent> {
        val engine = activeEngine
            ?: throw IllegalStateException("No model loaded. Please select a model before generating.")
        return engine.generateResponseAsync(prompt)
    }

    override fun stopGeneration() {
        activeEngine?.stopGeneration()
    }

    override fun close() {
        activeEngine?.close()
        activeEngine = null
    }
}
