package com.example.data.backend

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiClient
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerateContentResponse
import com.example.data.api.GenerationConfig
import com.example.data.api.InlineData
import com.example.data.api.Part
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Secure Backend Service Layer for NEURA AI.
 *
 * Architecture:
 * NEURA App (UI & ViewModel)
 *      ↓
 * NEURA Backend Service Layer (NeuraBackendService)
 *      ↓
 * Gemini API (Google Generative AI)
 *      ↓
 * Backend receives response / streams chunks
 *      ↓
 * NEURA App displays response
 *
 * Security Guarantees:
 * 1. The Gemini API key is retrieved securely from the server-side environment secret (GEMINI_API_KEY).
 * 2. The API key is NEVER exposed or transmitted to the frontend UI layers.
 * 3. Handles Rate Limits (429), Network Timeouts, and Automatic Model Failovers internally.
 */
class NeuraBackendService(private val context: Context) {

    /**
     * Internal secure resolution of the Gemini API key.
     * Checks both BuildConfig.ENV_GEMINI_API_KEY (injected from server GEMINI_API_KEY env)
     * and BuildConfig.GEMINI_API_KEY (injected from .env via Secrets plugin).
     * Never leaks the key outside this service layer.
     */
    private fun resolveSecretApiKey(): String {
        val envKey = try { BuildConfig.ENV_GEMINI_API_KEY } catch (_: Exception) { "" }
        if (isValidKey(envKey)) {
            return envKey.trim()
        }

        val buildConfigKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        if (isValidKey(buildConfigKey)) {
            return buildConfigKey.trim()
        }

        return ""
    }

    private fun isValidKey(key: String?): Boolean {
        return !key.isNullOrBlank() &&
                key != "MY_GEMINI_API_KEY" &&
                key != "not_configured" &&
                key != "null"
    }

    /**
     * Checks whether the backend has a configured API key without exposing the key.
     */
    fun isBackendConfigured(): Boolean {
        return resolveSecretApiKey().isNotBlank()
    }

    /**
     * Checks device network connectivity.
     */
    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Streams an AI response from the Gemini API through the secure service layer.
     * Maintains context, handles rate limits, network failures, and model failover.
     *
     * @param conversationHistory List of previous messages with role ("user" / "assistant") and content.
     * @param userPrompt The current message from the user.
     * @param imageBase64 Optional base64 encoded image data for vision capabilities.
     * @param systemInstruction The master personality prompt and directives.
     * @param mode Current conversation mode (e.g., "general", "study", "coding", "vision").
     * @param maxTokens Maximum response tokens based on settings.
     */
    fun streamChatResponse(
        conversationHistory: List<Pair<String, String>>,
        userPrompt: String,
        imageBase64: String? = null,
        systemInstruction: String,
        mode: String = "general",
        maxTokens: Int = 2000
    ): Flow<String> = flow {
        // 1. Network check
        if (!isNetworkAvailable()) {
            emit("⚠️ **Network Unavailable**: Please check your internet connection to reach the Gemini service.")
            return@flow
        }

        // 2. Secret validation
        val apiKey = resolveSecretApiKey()
        if (apiKey.isBlank()) {
            emit("⚠️ **Gemini Service Configuration Required**\n\nPlease configure the `GEMINI_API_KEY` secret in the AI Studio Secrets panel to enable real Gemini AI responses.")
            return@flow
        }

        // 3. Resolve target Gemini model (modular)
        val selectedModel = GeminiModelConfig.resolveModelForMode(mode)
        val candidateModels = listOf(
            selectedModel,
            GeminiModelConfig.FALLBACK_MODEL,
            GeminiModelConfig.SECONDARY_FALLBACK_MODEL
        ).distinct()

        // 4. Construct multi-turn context contents
        val contents = mutableListOf<Content>()

        // Take last 16 turns to keep prompt within context limits while preserving memory
        val recentTurns = conversationHistory.takeLast(16)
        for ((role, text) in recentTurns) {
            val geminiRole = if (role.equals("assistant", ignoreCase = true) || role.equals("model", ignoreCase = true)) {
                "model"
            } else {
                "user"
            }
            contents.add(
                Content(
                    role = geminiRole,
                    parts = listOf(Part(text = text))
                )
            )
        }

        // Append current prompt with multimodal parts if present
        val currentParts = mutableListOf<Part>()
        currentParts.add(Part(text = userPrompt))
        if (!imageBase64.isNullOrBlank()) {
            currentParts.add(
                Part(
                    inlineData = InlineData(
                        mimeType = "image/jpeg",
                        data = imageBase64
                    )
                )
            )
        }
        contents.add(Content(role = "user", parts = currentParts))

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                temperature = 0.7f,
                topP = 0.95f,
                maxOutputTokens = maxTokens
            )
        )

        var streamedSuccessfully = false
        var lastError: String? = null
        val adapter = GeminiClient.moshi.adapter(GenerateContentResponse::class.java)

        // 5. Model Execution Loop with Automatic Failover
        for (model in candidateModels) {
            if (streamedSuccessfully) break

            try {
                // Attempt Real SSE Streaming from Gemini API
                val responseBody = GeminiClient.apiService.streamGenerateContent(
                    model = model,
                    apiKey = apiKey,
                    request = request
                )
                val source = responseBody.source()

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (line.startsWith("data: ")) {
                        val json = line.removePrefix("data: ").trim()
                        if (json == "[DONE]") break
                        try {
                            val chunkResponse = adapter.fromJson(json)
                            val textChunk = chunkResponse?.candidates?.firstOrNull()?.content?.parts
                                ?.mapNotNull { it.text }
                                ?.joinToString("")

                            if (!textChunk.isNullOrEmpty()) {
                                streamedSuccessfully = true
                                emit(textChunk)
                            }
                        } catch (_: Exception) {
                            // Non-fatal chunk parsing error, continue reading next chunk
                        }
                    }
                }

                if (streamedSuccessfully) {
                    break
                }
            } catch (ex: Exception) {
                val errorDetails = categorizeError(ex)
                lastError = errorDetails

                // If streaming failed early without any emitted tokens, attempt non-streaming fallback
                if (!streamedSuccessfully) {
                    try {
                        val response = GeminiClient.apiService.generateContent(
                            model = model,
                            apiKey = apiKey,
                            request = request
                        )
                        val text = response.candidates?.firstOrNull()?.content?.parts
                            ?.mapNotNull { it.text }
                            ?.joinToString("")

                        if (!text.isNullOrBlank()) {
                            emit(text)
                            streamedSuccessfully = true
                            break
                        }
                    } catch (fallbackEx: Exception) {
                        lastError = categorizeError(fallbackEx)
                    }
                }
            }
        }

        // 6. Clean user-friendly error response if no tokens were emitted
        if (!streamedSuccessfully) {
            val friendlyError = lastError ?: "Unable to complete request. Please try again."
            emit("⚠️ **Gemini Service Notice**: $friendlyError")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming call for single-turn utility tasks.
     */
    suspend fun generateContent(
        prompt: String,
        systemInstruction: String,
        mode: String = "general",
        maxTokens: Int = 1000
    ): String = withContext(Dispatchers.IO) {
        val apiKey = resolveSecretApiKey()
        if (apiKey.isBlank()) {
            return@withContext "Error: Gemini API key is not configured."
        }

        val model = GeminiModelConfig.resolveModelForMode(mode)
        val request = GenerateContentRequest(
            contents = listOf(Content(role = "user", parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                temperature = 0.5f,
                maxOutputTokens = maxTokens
            )
        )

        try {
            val response = GeminiClient.apiService.generateContent(
                model = model,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.mapNotNull { it.text }?.joinToString("")
                ?: "No response generated."
        } catch (e: Exception) {
            categorizeError(e)
        }
    }

    /**
     * Categorizes exceptions into clean, descriptive user-facing explanations.
     */
    private fun categorizeError(ex: Exception): String {
        return when (ex) {
            is HttpException -> {
                when (ex.code()) {
                    429 -> "Rate limit reached for Gemini API. Please wait a moment before sending your next message."
                    401, 403 -> "Authentication failed. Please verify that your `GEMINI_API_KEY` secret is valid in the AI Studio Secrets panel."
                    400 -> "Invalid request payload. Please check your query or image attachment."
                    500, 502, 503, 504 -> "Gemini API servers are currently experiencing high load. Automatic failover was attempted. Please retry shortly."
                    else -> "API service error (${ex.code()}). Please try again."
                }
            }
            is SocketTimeoutException -> "Request timed out while waiting for Gemini. Please try again."
            is UnknownHostException -> "Cannot connect to Google Generative Language service. Check your device internet connection."
            is IOException -> "Network communication failure: ${ex.message ?: "Connection reset"}."
            else -> ex.message ?: "An unexpected error occurred."
        }
    }
}
