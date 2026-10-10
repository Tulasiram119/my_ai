package com.localai.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
            assertTrue(model.fileSizeBytes > 0)
        }
    }
}
