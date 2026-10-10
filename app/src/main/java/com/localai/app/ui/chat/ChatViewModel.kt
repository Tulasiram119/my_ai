package com.localai.app.ui.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localai.app.data.engine.ILlmEngine
import com.localai.app.data.engine.InferenceEvent
import com.localai.app.data.model.ChatMessage
import com.localai.app.data.model.InferenceConfig
import com.localai.app.data.model.ModelInfo
import com.localai.app.data.model.Sender
import com.localai.app.repository.DownloadProgress
import com.localai.app.repository.ModelManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val activeModel: ModelInfo? = null,
    val availableModels: List<ModelInfo> = emptyList(),
    val inferenceConfig: InferenceConfig = InferenceConfig.DEFAULT,
    val downloadProgress: DownloadProgress? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val llmEngine: ILlmEngine,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var inferenceJob: Job? = null
    private var downloadJob: Job? = null

    init {
        refreshAvailableModels()
    }

    fun refreshAvailableModels() {
        viewModelScope.launch {
            val models = modelManager.getAvailableModels()
            _uiState.update { current ->
                val active = current.activeModel?.let { active ->
                    models.find { it.filePath == active.filePath } ?: active
                }
                current.copy(
                    availableModels = models,
                    activeModel = active
                )
            }
        }
    }

    fun loadModel(modelInfo: ModelInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Loading model ${modelInfo.name} into memory…", errorMessage = null) }
            val result = llmEngine.loadModel(modelInfo.filePath, _uiState.value.inferenceConfig)
            if (result.isSuccess) {
                _uiState.update { current ->
                    current.copy(
                        activeModel = modelInfo.copy(isLoaded = true),
                        statusMessage = "Model ${modelInfo.name} loaded successfully",
                        errorMessage = null
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.localizedMessage ?: "Failed to load model"
                _uiState.update { it.copy(errorMessage = error, statusMessage = null) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        if (!llmEngine.isModelLoaded) {
            _uiState.update { it.copy(errorMessage = "Please select or import a model before sending messages.") }
            return
        }

        if (_uiState.value.isGenerating) return

        val userMessage = ChatMessage(
            sender = Sender.USER,
            content = trimmed
        )
        val assistantMessage = ChatMessage(
            sender = Sender.ASSISTANT,
            content = "",
            isStreaming = true
        )

        _uiState.update { current ->
            current.copy(
                messages = current.messages + userMessage + assistantMessage,
                isGenerating = true,
                errorMessage = null
            )
        }

        inferenceJob?.cancel()
        inferenceJob = viewModelScope.launch {
            llmEngine.generateResponseAsync(trimmed).collect { event ->
                when (event) {
                    is InferenceEvent.TokenChunk -> {
                        _uiState.update { current ->
                            val updatedMessages = current.messages.toMutableList()
                            val lastIdx = updatedMessages.lastIndex
                            if (lastIdx >= 0 && updatedMessages[lastIdx].sender == Sender.ASSISTANT) {
                                updatedMessages[lastIdx] = updatedMessages[lastIdx].copy(
                                    content = event.fullText,
                                    isStreaming = true
                                )
                            }
                            current.copy(messages = updatedMessages)
                        }
                    }
                    is InferenceEvent.Completed -> {
                        _uiState.update { current ->
                            val updatedMessages = current.messages.toMutableList()
                            val lastIdx = updatedMessages.lastIndex
                            if (lastIdx >= 0 && updatedMessages[lastIdx].sender == Sender.ASSISTANT) {
                                updatedMessages[lastIdx] = updatedMessages[lastIdx].copy(
                                    content = event.fullText,
                                    isStreaming = false,
                                    tokensPerSec = event.tokensPerSec,
                                    latencyMs = event.firstTokenLatencyMs
                                )
                            }
                            current.copy(messages = updatedMessages, isGenerating = false)
                        }
                    }
                    is InferenceEvent.Error -> {
                        _uiState.update { current ->
                            val updatedMessages = current.messages.toMutableList()
                            val lastIdx = updatedMessages.lastIndex
                            if (lastIdx >= 0 && updatedMessages[lastIdx].sender == Sender.ASSISTANT) {
                                updatedMessages[lastIdx] = updatedMessages[lastIdx].copy(
                                    isStreaming = false,
                                    errorMessage = event.throwable.localizedMessage
                                )
                            }
                            current.copy(
                                messages = updatedMessages,
                                isGenerating = false,
                                errorMessage = event.throwable.localizedMessage
                            )
                        }
                    }
                }
            }
        }
    }

    fun stopGeneration() {
        llmEngine.stopGeneration()
        inferenceJob?.cancel()
        _uiState.update { current ->
            val updatedMessages = current.messages.toMutableList()
            val lastIdx = updatedMessages.lastIndex
            if (lastIdx >= 0 && updatedMessages[lastIdx].sender == Sender.ASSISTANT && updatedMessages[lastIdx].isStreaming) {
                updatedMessages[lastIdx] = updatedMessages[lastIdx].copy(isStreaming = false)
            }
            current.copy(messages = updatedMessages, isGenerating = false)
        }
    }

    fun clearChat() {
        stopGeneration()
        _uiState.update { it.copy(messages = emptyList(), errorMessage = null, statusMessage = null) }
    }

    fun updateInferenceConfig(config: InferenceConfig) {
        val validated = config.validated()
        _uiState.update { it.copy(inferenceConfig = validated) }
        _uiState.value.activeModel?.let { active ->
            loadModel(active)
        }
    }

    fun importModel(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Importing $fileName…", errorMessage = null) }
            val result = modelManager.importModelFromUri(uri, fileName) { progress ->
                _uiState.update {
                    it.copy(
                        downloadProgress = DownloadProgress(
                            bytesDownloaded = 0,
                            totalBytes = 100,
                            percent = progress,
                            isComplete = false
                        )
                    )
                }
            }

            if (result.isSuccess) {
                val model = result.getOrThrow()
                refreshAvailableModels()
                _uiState.update {
                    it.copy(
                        downloadProgress = null,
                        statusMessage = "Imported ${model.name} successfully"
                    )
                }
                loadModel(model)
            } else {
                _uiState.update {
                    it.copy(
                        downloadProgress = null,
                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to import model"
                    )
                }
            }
        }
    }

    fun downloadModel(url: String, fileName: String) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Starting download…", errorMessage = null) }
            modelManager.downloadModel(url, fileName).collect { progress ->
                _uiState.update { it.copy(downloadProgress = progress) }
                if (progress.isComplete) {
                    refreshAvailableModels()
                    _uiState.update {
                        it.copy(
                            statusMessage = "Download completed for $fileName",
                            downloadProgress = null
                        )
                    }
                } else if (progress.error != null) {
                    _uiState.update {
                        it.copy(
                            errorMessage = progress.error,
                            downloadProgress = null,
                            statusMessage = null
                        )
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _uiState.update { it.copy(downloadProgress = null, statusMessage = "Download cancelled") }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        inferenceJob?.cancel()
        downloadJob?.cancel()
        llmEngine.close()
    }
}
