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
 * Client-side OpenAI caller.
 *
 * ⚠️ TEMPORARY / PRIVATE-TESTING SETUP ONLY:
 * Android app -> OpenAI API (directly)
 *
 * The OpenAI API key is bundled inside the built APK (BuildConfig.OPENAI_API_KEY).
 * Anyone who has the APK file can extract this key. This is fine for your own
 * device / testing, but do NOT publish this build anywhere public (Play Store,
 * APK sharing sites, GitHub release, etc.) while the key is embedded this way.
 * Before any public release, move this call back behind a backend server so
 * the key never ships inside the app.
 */
class NeuraBackendService(private val context: Context) {

    private val apiKey: String
        get() = BuildConfig.OPENAI_API_KEY

    private val model = "gpt-5-mini"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isBackendConfigured(): Boolean =
        apiKey.isNotBlank() && apiKey != "OPENAI_API_KEY_NOT_SET"

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildMessages(
        conversationHistory: List<Pair<String, String>>,
        userPrompt: String,
        systemInstruction: String
    ): JSONArray {
        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", systemInstruction)
        })
        conversationHistory.takeLast(16).forEach { (role, content) ->
            messages.put(JSONObject().apply {
                put("role", if (role.equals("assistant", true) || role.equals("model", true)) "assistant" else "user")
                put("content", content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        })
        return messages
    }

    /**
     * Streams the OpenAI response chunk-by-chunk using OpenAI's own SSE format
     * (each line is `data: {"choices":[{"delta":{"content":"..."}}]}`, ending
     * with `data: [DONE]`).
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
            emit("⚠️ **OpenAI Not Configured**: OPENAI_API_KEY was not set at build time.")
            return@flow
        }

        val payload = JSONObject().apply {
            put("model", model)
            put("messages", buildMessages(conversationHistory, userPrompt, systemInstruction))
            put("max_completion_tokens", maxTokens)
            put("temperature", 0.7)
            put("stream", true)
        }.toString()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    emit("⚠️ **OpenAI Error (${response.code})**: ${extractError(body)}")
                    return@use
                }

                val source = response.body?.source()
                if (source == null) {
                    emit("⚠️ **OpenAI Error**: Empty response.")
                    return@use
                }

                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue

                    val raw = line.removePrefix("data:").trim()
                    if (raw.isBlank() || raw == "[DONE]") continue

                    try {
                        val obj = JSONObject(raw)
                        val choices = obj.optJSONArray("choices")
                        val delta = choices?.optJSONObject(0)?.optJSONObject("delta")
                        val text = delta?.optString("content", "") ?: ""
                        if (text.isNotEmpty()) emit(text)
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
        if (!isBackendConfigured()) return@withContext "⚠️ OpenAI is not configured (OPENAI_API_KEY not set at build time)."

        val payload = JSONObject().apply {
            put("model", model)
            put("messages", buildMessages(emptyList(), prompt, systemInstruction))
            put("max_completion_tokens", maxTokens)
            put("temperature", 0.7)
        }.toString()

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    "⚠️ **OpenAI Error (${response.code})**: ${extractError(body)}"
                } else {
                    val choices = JSONObject(body).optJSONArray("choices")
                    val message = choices?.optJSONObject(0)?.optJSONObject("message")
                    message?.optString("content", "No response generated.") ?: "No response generated."
                }
            }
        } catch (ex: Exception) {
            "⚠️ **Connection Error**: ${friendlyError(ex)}"
        }
    }

    private fun extractError(body: String): String {
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message")
                ?: body.ifBlank { "Unknown OpenAI error." }
        } catch (_: Exception) {
            body.ifBlank { "Unknown OpenAI error." }.take(500)
        }
    }

    private fun friendlyError(ex: Exception): String = when (ex) {
        is SocketTimeoutException -> "Request timed out. Please try again."
        is UnknownHostException -> "Cannot reach OpenAI. Check your internet connection."
        is IOException -> "Network communication failed."
        else -> ex.message ?: "Unexpected error."
    }
}
