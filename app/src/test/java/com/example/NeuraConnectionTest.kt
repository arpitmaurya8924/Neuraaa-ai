package com.example
 
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class NeuraConnectionTest {

    @Test
    fun testServerSecretGeminiConnection() {
        val apiKey = System.getenv("GEMINI_API_KEY") ?: ""
        // Skip network test if running in environment where internet or key is not provided
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return
        }

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 8000
        conn.readTimeout = 8000

        val payload = """
            {
                "contents": [
                    {
                        "parts": [
                            {"text": "Reply with 'NEURA_CONNECTED'"}
                        ]
                    }
                ]
            }
        """.trimIndent()

        conn.outputStream.use { os ->
            os.write(payload.toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        assertTrue("Expected HTTP 200 from Gemini API, got $responseCode", responseCode == 200)

        val responseBody = conn.inputStream.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }

        assertTrue("Response should contain candidates array", responseBody.contains("\"candidates\""))
        assertTrue("Response should contain text part", responseBody.contains("\"text\""))
        assertTrue("Response should contain model role", responseBody.contains("\"model\""))
    }
}
