package com.example.data.backend

/**
 * Modular configuration for Gemini models.
 * Allows switching or upgrading models in one central location
 * without modifying UI, ViewModel, or repository code.
 */
object GeminiModelConfig {
    // Current supported preview and production Gemini models
    const val MODEL_GEMINI_3_5_FLASH_LITE = "gemini-3.5-flash-lite"
    const val MODEL_GEMINI_3_5_FLASH = "gemini-3.5-flash"
    const val MODEL_GEMINI_3_6_FLASH = "gemini-3.6-flash"
    const val MODEL_GEMINI_3_1_PRO = "gemini-3.1-pro-preview"

    // Default primary model for NEURA general chat and fast tasks (ultra-low latency, vision-ready)
    var primaryModel: String = MODEL_GEMINI_3_5_FLASH_LITE

    // High-availability fallback model if primary model experiences high load or downtime
    const val FALLBACK_MODEL: String = MODEL_GEMINI_3_6_FLASH
    const val SECONDARY_FALLBACK_MODEL: String = MODEL_GEMINI_3_5_FLASH

    data class ModelDescriptor(
        val id: String,
        val displayName: String,
        val description: String,
        val supportsVision: Boolean = true,
        val supportsStreaming: Boolean = true,
        val recommendedFor: String
    )

    val AVAILABLE_MODELS = listOf(
        ModelDescriptor(
            id = MODEL_GEMINI_3_5_FLASH_LITE,
            displayName = "Gemini 3.5 Flash Lite",
            description = "Ultra-fast response speed, multimodal vision, ideal for real-time mobile chatting.",
            supportsVision = true,
            supportsStreaming = true,
            recommendedFor = "Everyday chat, study mode, instant answers"
        ),
        ModelDescriptor(
            id = MODEL_GEMINI_3_6_FLASH,
            displayName = "Gemini 3.6 Flash",
            description = "High-availability backup model with advanced conversational depth.",
            supportsVision = true,
            supportsStreaming = true,
            recommendedFor = "Failover and high throughput"
        ),
        ModelDescriptor(
            id = MODEL_GEMINI_3_5_FLASH,
            displayName = "Gemini 3.5 Flash",
            description = "High capability, multimodal, balanced reasoning and conversational fluency.",
            supportsVision = true,
            supportsStreaming = true,
            recommendedFor = "Deep study, detailed writing"
        ),
        ModelDescriptor(
            id = MODEL_GEMINI_3_1_PRO,
            displayName = "Gemini 3.1 Pro Preview",
            description = "Advanced reasoning, complex STEM, deep architectural and coding analysis.",
            supportsVision = true,
            supportsStreaming = true,
            recommendedFor = "Coding mode, complex research, in-depth proofs"
        )
    )

    /**
     * Resolves the optimal Gemini model based on user mode and preference.
     * Modular: simply update this method or set primaryModel to change defaults.
     */
    fun resolveModelForMode(mode: String, preferProForCoding: Boolean = false): String {
        return when (mode.lowercase()) {
            "coding", "code" -> if (preferProForCoding) MODEL_GEMINI_3_1_PRO else primaryModel
            "research" -> if (preferProForCoding) MODEL_GEMINI_3_1_PRO else primaryModel
            else -> primaryModel
        }
    }
}
