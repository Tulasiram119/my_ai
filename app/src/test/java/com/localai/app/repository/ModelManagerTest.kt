package com.localai.app.repository

import android.content.Context
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
    fun getAvailableModels_findsBinAndTaskFiles() = runBlocking {
        File(modelsDir, "gemma-2b.bin").writeBytes(ByteArray(1024))
        File(modelsDir, "phi-2.task").writeBytes(ByteArray(2048))
        File(modelsDir, "ignore_me.txt").writeText("hello")

        val models = modelManager.getAvailableModels()
        assertEquals(2, models.size)
        assertEquals("gemma-2b.bin", models[0].name)
        assertEquals("phi-2.task", models[1].name)
        assertEquals(1024L, models[0].sizeBytes)
        assertEquals(2048L, models[1].sizeBytes)
    }

    @Test
    fun deleteModel_removesFileInsideDirectory() = runBlocking {
        val testFile = File(modelsDir, "to_delete.bin")
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
        val externalFile = tempFolder.newFile("external.bin")
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
