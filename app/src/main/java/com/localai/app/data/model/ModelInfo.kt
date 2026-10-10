package com.localai.app.data.model

data class ModelInfo(
    val name: String,
    val filePath: String,
    val sizeBytes: Long,
    val isLoaded: Boolean = false
) {
    val formattedSize: String
        get() {
            val mb = sizeBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024) {
                String.format(java.util.Locale.US, "%.2f GB", mb / 1024)
            } else {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            }
        }
}
