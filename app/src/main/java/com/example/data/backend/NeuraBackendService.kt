package com.example.data.backend

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Client-side proxy for the NEURA backend.
 *
 * Android app -> NEURA backend -> BazaarLink/Gemini -> Android app
 *
 * Provider API keys NEVER live in the Android APK.
 */
class NeuraBackendService(private val context: Context) {

    private val baseUrl: String
        get() = BuildConfig.NEURA_BACKEND_URL.trim().trimEnd('/')

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isBackendConfigured(): Boolean =
        baseUrl.startsWith("https://") && !baseUrl.contains("YOUR-NEURA-BACKEND")

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildPayload(
        conversationHistory: List<Pair<String, String>>,
        userPrompt: String,
        imageBase64: String?,
        systemInstruction: String,
        mode: String,
        maxTokens: Int
    ): String {
        val history = JSONArray()
        conversationHistory.takeLast(16).forEach { (role, content) ->
            history.put(JSONObject().apply {
                put("role", if (role.equals("assistant", true) || role.equals("model", true)) "assistant" else "user")
                put("content", content)
            })
        }

        return JSONObject().apply {
            put("prompt", userPrompt)
            put("conversationHistory", history)
            put("systemInstruction", systemInstruction)
            put("mode", mode)
            put("maxTokens", maxTokens)
            if (!imageBase64.isNullOrBlank()) put("imageBase64", imageBase64)
        }.toString()
    }

    /**
     * Streams normalized SSE from /api/chat/stream.
     * The backend owns BazaarLink/Gemini credentials and provider selection.
     */
    fun streamChatResponse(
        conversationHistory: List<Pair<String, String>>,
        userPrompt: String,
        imageBase64: String? = null,
        systemInstruction: String,
        mode: String = "general",
        maxTokens: Int = 2000
    ): Flow<String> = flow {
        if (!isNetworkAvailable()) {
            emit("⚠️ **Network Unavailable**: Please check your internet connection.")
            return@flow
        }

        if (!isBackendConfigured()) {
            emit("⚠️ **NEURA Backend Configuration Required**: Backend URL is not configured.")
            return@flow
        }

        val payload = buildPayload(
            conversationHistory, userPrompt, imageBase64,
            systemInstruction, mode, maxTokens
        )

        val request = Request.Builder()
            .url("$baseUrl/api/chat/stream")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    emit("⚠️ **NEURA Backend Error (${response.code})**: ${extractError(body)}")
                    return@use
                }

                val source = response.body?.source()
                if (source == null) {
                    emit("⚠️ **NEURA Backend Error**: Empty response.")
                    return@use
                }

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue

                    val raw = line.removePrefix("data:").trim()
                    if (raw.isBlank() || raw == "[DONE]") continue

                    try {
                        val obj = JSONObject(raw)
                        val text = obj.optString("text", "")
                        val error = obj.optString("error", "")
                        if (text.isNotEmpty()) emit(text)
                        if (error.isNotEmpty()) emit("⚠️ **NEURA Backend Error**: $error")
                    } catch (_: Exception) {
                        // Ignore malformed/partial SSE frames.
                    }
                }
            }
        } catch (ex: Exception) {
            emit("⚠️ **Connection Error**: ${friendlyError(ex)}")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String,
        mode: String = "general",
        maxTokens: Int = 1000
    ): String = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext "⚠️ Network unavailable."
        if (!isBackendConfigured()) return@withContext "⚠️ NEURA backend is not configured."

        val payload = buildPayload(
            emptyList(), prompt, null, systemInstruction, mode, maxTokens
        )

        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    "⚠️ **NEURA Backend Error (${response.code})**: ${extractError(body)}"
                } else {
                    JSONObject(body).optString("response", "No response generated.")
                }
            }
        } catch (ex: Exception) {
            "⚠️ **Connection Error**: ${friendlyError(ex)}"
        }
    }

    private fun extractError(body: String): String {
        return try {
            JSONObject(body).optString("error", body.ifBlank { "Unknown backend error." })
        } catch (_: Exception) {
            body.ifBlank { "Unknown backend error." }.take(500)
        }
    }

    private fun friendlyError(ex: Exception): String = when (ex) {
        is SocketTimeoutException -> "Request timed out. Please try again."
        is UnknownHostException -> "Cannot reach the NEURA backend. Check your internet connection."
        is IOException -> "Network communication failed."
        else -> ex.message ?: "Unexpected error."
    }
}
