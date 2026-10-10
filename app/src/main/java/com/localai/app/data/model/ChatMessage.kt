package com.localai.app.data.model

import java.util.UUID

enum class Sender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokensPerSec: Float? = null,
    val latencyMs: Long? = null,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null
)
