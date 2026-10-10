package com.localai.app.data.model

/**
 * Metadata for a curated open-source model available for 1-tap download.
 */
data class CuratedModel(
    val id: String,
    val name: String,
    val publisher: String,
    val description: String,
    val parameterSize: String,
    val quantization: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val recommendedRam: String,
    val downloadUrl: String,
    val targetFileName: String
)

object CuratedModels {
    /**
     * Curated list of verified open-source models optimized for mobile devices.
     * All models are standard GGUF weights hosted on official Hugging Face repositories.
     */
    val list: List<CuratedModel> = listOf(
        CuratedModel(
            id = "qwen2.5-0.5b-instruct",
            name = "Qwen 2.5 0.5B Instruct",
            publisher = "Alibaba Cloud",
            description = "Ultra-fast, responsive chat assistant. Runs effortlessly on any Android phone (2–4GB RAM).",
            parameterSize = "0.5B",
            quantization = "Q4_K_M",
            fileSizeBytes = 398_000_000L,
            formattedSize = "398 MB",
            recommendedRam = "~800 MB RAM",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            targetFileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        ),
        CuratedModel(
            id = "qwen2.5-1.5b-instruct",
            name = "Qwen 2.5 1.5B Instruct",
            publisher = "Alibaba Cloud",
            description = "Exceptional reasoning, code understanding, and multilingual capabilities in a compact footprint.",
            parameterSize = "1.5B",
            quantization = "Q4_K_M",
            fileSizeBytes = 986_000_000L,
            formattedSize = "986 MB",
            recommendedRam = "~1.5 GB RAM",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            targetFileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
        ),
        CuratedModel(
            id = "llama3.2-1b-instruct",
            name = "Llama 3.2 1B Instruct",
            publisher = "Meta",
            description = "Meta's official lightweight edge model designed for conversational speed and instruction following.",
            parameterSize = "1B",
            quantization = "Q4_K_M",
            fileSizeBytes = 808_000_000L,
            formattedSize = "808 MB",
            recommendedRam = "~1.2 GB RAM",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            targetFileName = "llama-3.2-1b-instruct-q4_k_m.gguf"
        ),
        CuratedModel(
            id = "smollm2-1.7b-instruct",
            name = "SmolLM2 1.7B Instruct",
            publisher = "Hugging Face",
            description = "High-performing compact LLM by Hugging Face, outperforming many larger models on common reasoning benchmarks.",
            parameterSize = "1.7B",
            quantization = "Q4_K_M",
            fileSizeBytes = 1_060_000_000L,
            formattedSize = "1.06 GB",
            recommendedRam = "~1.6 GB RAM",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf",
            targetFileName = "smollm2-1.7b-instruct-q4_k_m.gguf"
        ),
        CuratedModel(
            id = "phi3.5-mini-instruct",
            name = "Phi-3.5 Mini 3.8B Instruct",
            publisher = "Microsoft",
            description = "State-of-the-art small language model with desktop-grade reasoning. Recommended for flagship phones (8GB+ RAM).",
            parameterSize = "3.8B",
            quantization = "Q4_K_M",
            fileSizeBytes = 2_390_000_000L,
            formattedSize = "2.39 GB",
            recommendedRam = "~3.5 GB RAM",
            downloadUrl = "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf",
            targetFileName = "phi-3.5-mini-instruct-q4_k_m.gguf"
        )
    )
}
