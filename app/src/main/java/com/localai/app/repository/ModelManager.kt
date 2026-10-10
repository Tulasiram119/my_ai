package com.localai.app.repository

import android.content.Context
import android.net.Uri
import com.localai.app.data.model.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percent: Float,
    val isComplete: Boolean,
    val error: String? = null
)

class ModelManager(
    private val context: Context,
    baseDir: File? = null
) {
    val modelsDirectory: File = baseDir ?: File(context.filesDir, "models").apply {
        if (!exists()) mkdirs()
    }

    init {
        if (!modelsDirectory.exists()) {
            modelsDirectory.mkdirs()
        }
    }

    suspend fun getAvailableModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        val validExtensions = listOf("bin", "task")
        val files = modelsDirectory.listFiles { file ->
            file.isFile && validExtensions.contains(file.extension.lowercase())
        } ?: emptyArray()

        files.map { file ->
            ModelInfo(
                name = file.name,
                filePath = file.absolutePath,
                sizeBytes = file.length(),
                isLoaded = false
            )
        }.sortedBy { it.name }
    }

    suspend fun importModelFromUri(
        uri: Uri,
        fileName: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<ModelInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val sanitizedName = if (fileName.endsWith(".bin", true) || fileName.endsWith(".task", true)) {
                fileName
            } else {
                "$fileName.bin"
            }
            val destinationFile = File(modelsDirectory, sanitizedName)
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")

            val totalBytes = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            }.getOrDefault(-1L)

            val tempFile = File(modelsDirectory, "$sanitizedName.tmp")
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (totalBytes > 0 && onProgress != null) {
                            val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            if (!tempFile.renameTo(destinationFile)) {
                throw IllegalStateException("Failed to move imported file into place")
            }

            ModelInfo(
                name = destinationFile.name,
                filePath = destinationFile.absolutePath,
                sizeBytes = destinationFile.length(),
                isLoaded = false
            )
        }
    }

    fun downloadModel(urlStr: String, destinationFileName: String): Flow<DownloadProgress> = flow {
        val sanitizedName = if (destinationFileName.endsWith(".bin", true) || destinationFileName.endsWith(".task", true)) {
            destinationFileName
        } else {
            "$destinationFileName.bin"
        }
        val destinationFile = File(modelsDirectory, sanitizedName)
        val tempFile = File(modelsDirectory, "$sanitizedName.download")

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                emit(
                    DownloadProgress(
                        bytesDownloaded = 0,
                        totalBytes = 0,
                        percent = 0f,
                        isComplete = false,
                        error = "HTTP error $responseCode: ${connection.responseMessage}"
                    )
                )
                return@flow
            }

            val contentLength = connection.contentLengthLong

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    var lastReportedTime = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastReportedTime > 200) {
                            val percent = if (contentLength > 0) {
                                (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            } else {
                                -1f
                            }
                            emit(
                                DownloadProgress(
                                    bytesDownloaded = totalBytesRead,
                                    totalBytes = contentLength,
                                    percent = percent,
                                    isComplete = false
                                )
                            )
                            lastReportedTime = now
                        }
                    }
                    output.flush()
                }
            }

            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            if (!tempFile.renameTo(destinationFile)) {
                throw IllegalStateException("Failed to finalize downloaded file")
            }

            emit(
                DownloadProgress(
                    bytesDownloaded = destinationFile.length(),
                    totalBytes = destinationFile.length(),
                    percent = 1.0f,
                    isComplete = true
                )
            )
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            emit(
                DownloadProgress(
                    bytesDownloaded = 0,
                    totalBytes = 0,
                    percent = 0f,
                    isComplete = false,
                    error = e.localizedMessage ?: "Unknown download error"
                )
            )
        } finally {
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun deleteModel(modelInfo: ModelInfo): Boolean = withContext(Dispatchers.IO) {
        val file = File(modelInfo.filePath)
        if (file.exists() && file.parentFile?.absolutePath == modelsDirectory.absolutePath) {
            file.delete()
        } else {
            false
        }
    }
}
