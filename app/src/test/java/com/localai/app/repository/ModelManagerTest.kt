package com.localai.app.repository

import android.content.Context
import com.localai.app.data.model.CuratedModel
import com.localai.app.data.model.ModelInfo
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var modelsDir: File
    private lateinit var modelManager: ModelManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        modelsDir = tempFolder.newFolder("models")
        modelManager = ModelManager(context, modelsDir)
    }

    @Test
    fun getAvailableModels_emptyDirectory_returnsEmptyList() = runBlocking {
        val models = modelManager.getAvailableModels()
        assertTrue(models.isEmpty())
    }

    @Test
    fun getAvailableModels_findsGgufBinAndTaskFiles() = runBlocking {
        File(modelsDir, "qwen2.5-0.5b.gguf").writeBytes(ByteArray(4096))
        File(modelsDir, "gemma-2b.bin").writeBytes(ByteArray(1024))
        File(modelsDir, "phi-2.task").writeBytes(ByteArray(2048))
        File(modelsDir, "ignore_me.txt").writeText("hello")

        val models = modelManager.getAvailableModels()
        assertEquals(3, models.size)
        assertTrue(models.any { it.name == "qwen2.5-0.5b.gguf" && it.sizeBytes == 4096L })
        assertTrue(models.any { it.name == "gemma-2b.bin" && it.sizeBytes == 1024L })
        assertTrue(models.any { it.name == "phi-2.task" && it.sizeBytes == 2048L })
        assertFalse(models.any { it.name == "ignore_me.txt" })
    }

    @Test
    fun isModelInstalled_checksFileExistenceAndNonZeroSize() {
        val targetName = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
        assertFalse(modelManager.isModelInstalled(targetName))

        val modelFile = File(modelsDir, targetName)
        modelFile.writeBytes(ByteArray(1024))
        assertTrue(modelManager.isModelInstalled(targetName))
    }

    @Test
    fun deleteModel_removesFileInsideDirectory() = runBlocking {
        val testFile = File(modelsDir, "to_delete.gguf")
        testFile.writeBytes(ByteArray(512))
        assertTrue(testFile.exists())

        val modelInfo = ModelInfo(
            name = testFile.name,
            filePath = testFile.absolutePath,
            sizeBytes = 512L
        )

        val deleted = modelManager.deleteModel(modelInfo)
        assertTrue(deleted)
        assertFalse(testFile.exists())
    }

    @Test
    fun deleteModel_refusesToDeleteOutsideDirectory() = runBlocking {
        val externalFile = tempFolder.newFile("external.gguf")
        externalFile.writeBytes(ByteArray(512))

        val modelInfo = ModelInfo(
            name = externalFile.name,
            filePath = externalFile.absolutePath,
            sizeBytes = 512L
        )

        val deleted = modelManager.deleteModel(modelInfo)
        assertFalse(deleted)
        assertTrue(externalFile.exists())
    }
}
