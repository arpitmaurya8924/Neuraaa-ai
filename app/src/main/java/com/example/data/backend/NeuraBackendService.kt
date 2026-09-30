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
 * Client-side Gemini caller (free tier).
 *
 * ⚠️ TEMPORARY / PRIVATE-TESTING SETUP ONLY:
 * Android app -> Gemini API (directly)
 *
 * The Gemini API key is bundled inside the built APK (BuildConfig.GEMINI_API_KEY).
 * Anyone who has the APK file can extract this key. This is fine for your own
 * device / testing, but do NOT publish this build anywhere public (Play Store,
 * APK sharing sites, GitHub release, etc.) while the key is embedded this way.
 * Before any public release, move this call back behind a backend server so
 * the key never ships inside the app.
 *
 * IMPORTANT: Newer Google "Auth key" format keys (starting with "AQ.") do NOT
 * work as a ?key= URL query parameter — they must be sent via the
 * x-goog-api-key HTTP header. This also works fine for older AIzaSy... keys,
 * so the header is used unconditionally here.
 */
class NeuraBackendService(private val context: Context) {

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    // Free-tier Gemini models, tried in order. If the first is unavailable
    // or rate-limited, the next one is tried automatically.
    private val modelChain = listOf("gemini-3.5-flash", "gemini-3.6-flash", "gemini-2.5-flash-lite")

    private val geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isBackendConfigured(): Boolean =
        apiKey.isNotBlank() && apiKey != "GEMINI_API_KEY_NOT_SET" && apiKey != "MY_GEMINI_API_KEY"

    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildGeminiBody(
        conversationHistory: List<Pair<String, String>>,
        userPrompt: String,
        systemInstruction: String,
        maxTokens: Int
    ): String {
        val contents = JSONArray()
        conversationHistory.takeLast(16).forEach { (role, content) ->
            contents.put(JSONObject().apply {
                put("role", if (role.equals("assistant", true) || role.equals("model", true)) "model" else "user")
                put("parts", JSONArray().put(JSONObject().put("text", content)))
            })
        }
        contents.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
        })

        return JSONObject().apply {
            put("contents", contents)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", maxTokens)
            })
        }.toString()
    }

    /**
     * Streams the Gemini response chunk-by-chunk, trying each model in
     * modelChain in turn until one starts streaming successfully.
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
            emit("⚠️ **Gemini Not Configured**: GEMINI_API_KEY was not set at build time.")
            return@flow
        }

        val body = buildGeminiBody(conversationHistory, userPrompt, systemInstruction, maxTokens)
        val failures = mutableListOf<String>()

        for (model in modelChain) {
            val request = Request.Builder()
                .url("$geminiBaseUrl/$model:streamGenerateContent?alt=sse")
                .addHeader("x-goog-api-key", apiKey)
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .build()

            var streamedAny = false
            var shouldTryNext = false

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string().orEmpty()
                        failures.add("[$model] ${response.code}: ${extractError(errBody)}")
                        shouldTryNext = true
                        return@use
                    }

                    val source = response.body?.source()
                    if (source == null) {
                        failures.add("[$model] Empty response.")
                        shouldTryNext = true
                        return@use
                    }

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val raw = line.removePrefix("data:").trim()
                        if (raw.isBlank()) continue
                        try {
                            val obj = JSONObject(raw)
                            val candidates = obj.optJSONArray("candidates")
                            val parts = candidates?.optJSONObject(0)
                                ?.optJSONObject("content")
                                ?.optJSONArray("parts")
                            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""
                            if (text.isNotEmpty()) {
                                streamedAny = true
                                emit(text)
                            }
                        } catch (_: Exception) {
                            // Ignore malformed/partial SSE frames.
                        }
                    }
                }
            } catch (ex: Exception) {
                failures.add("[$model] ${friendlyError(ex)}")
                shouldTryNext = true
            }

            if (streamedAny) return@flow // success, stop trying further models
            if (!shouldTryNext) return@flow // stream started but produced nothing; don't loop forever
        }

        emit("⚠️ **Gemini Error**: All models failed. ${failures.joinToString(" | ")}")
    }.flowOn(Dispatchers.IO)

    suspend fun generateContent(
        prompt: String,
        systemInstruction: String,
        mode: String = "general",
        maxTokens: Int = 1000
    ): String = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext "⚠️ Network unavailable."
        if (!isBackendConfigured()) return@withContext "⚠️ Gemini is not configured (GEMINI_API_KEY not set at build time)."

        val body = buildGeminiBody(emptyList(), prompt, systemInstruction, maxTokens)
        val failures = mutableListOf<String>()

        for (model in modelChain) {
            val request = Request.Builder()
                .url("$geminiBaseUrl/$model:generateContent")
                .addHeader("x-goog-api-key", apiKey)
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Accept", "application/json")
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val respBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        failures.add("[$model] ${response.code}: ${extractError(respBody)}")
                        return@use
                    }
                    val candidates = JSONObject(respBody).optJSONArray("candidates")
                    val parts = candidates?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text", "")
                    if (!text.isNullOrEmpty()) return@withContext text
                    failures.add("[$model] Empty response.")
                }
            } catch (ex: Exception) {
                failures.add("[$model] ${friendlyError(ex)}")
            }
        }

        "⚠️ **Gemini Error**: All models failed. ${failures.joinToString(" | ")}"
    }

    private fun extractError(body: String): String {
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message")
                ?: body.ifBlank { "Unknown Gemini error." }
        } catch (_: Exception) {
            body.ifBlank { "Unknown Gemini error." }.take(500)
        }
    }

    private fun friendlyError(ex: Exception): String = when (ex) {
        is SocketTimeoutException -> "Request timed out. Please try again."
        is UnknownHostException -> "Cannot reach Gemini. Check your internet connection."
        is IOException -> "Network communication failed."
        else -> ex.message ?: "Unexpected error."
    }
}
