package com.localai.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InferenceConfigTest {

    @Test
    fun defaultValues_areValid() {
        val config = InferenceConfig.DEFAULT
        assertEquals(512, config.maxTokens)
        assertEquals(0.8f, config.temperature, 0.001f)
        assertEquals(40, config.topK)
    }

    @Test
    fun validated_clampsOutOfRangeValues() {
        val invalidLow = InferenceConfig(
            maxTokens = 10,
            temperature = 0.01f,
            topK = 0,
            randomSeed = -5
        ).validated()

        assertEquals(32, invalidLow.maxTokens)
        assertEquals(0.1f, invalidLow.temperature, 0.001f)
        assertEquals(1, invalidLow.topK)
        assertEquals(0, invalidLow.randomSeed)

        val invalidHigh = InferenceConfig(
            maxTokens = 10000,
            temperature = 5.0f,
            topK = 500,
            randomSeed = 42
        ).validated()

        assertEquals(4096, invalidHigh.maxTokens)
        assertEquals(2.0f, invalidHigh.temperature, 0.001f)
        assertEquals(100, invalidHigh.topK)
        assertEquals(42, invalidHigh.randomSeed)
    }
}
