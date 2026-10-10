package com.localai.app.data.model

data class InferenceConfig(
    val maxTokens: Int = 512,
    val temperature: Float = 0.8f,
    val topK: Int = 40,
    val randomSeed: Int = 0
) {
    fun validated(): InferenceConfig {
        return copy(
            maxTokens = maxTokens.coerceIn(32, 4096),
            temperature = temperature.coerceIn(0.1f, 2.0f),
            topK = topK.coerceIn(1, 100),
            randomSeed = randomSeed.coerceAtLeast(0)
        )
    }

    companion object {
        val DEFAULT = InferenceConfig()
    }
}
