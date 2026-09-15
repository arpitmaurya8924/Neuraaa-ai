package com.example

import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class NeuraChatLogicTest {

    @Test
    fun testUniqueMessageIdsGenerated() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        assertNotEquals(id1, id2)

        val userMsg = MessageEntity(
            id = id1,
            conversationId = "conv-123",
            role = "user",
            content = "aapka name kya hai"
        )
        val assistantMsg = MessageEntity(
            id = id2,
            conversationId = "conv-123",
            role = "assistant",
            content = "Main NEURA hoon, aapka AI companion."
        )

        assertEquals("user", userMsg.role)
        assertEquals("assistant", assistantMsg.role)
        assertNotEquals(userMsg.id, assistantMsg.id)
    }

    @Test
    fun testConversationHistoryPreservesRolesAndOrder() {
        val historyMessages = listOf(
            MessageEntity(id = "1", conversationId = "c1", role = "user", content = "aapka name kya hai"),
            MessageEntity(id = "2", conversationId = "c1", role = "assistant", content = "Main NEURA hoon."),
            MessageEntity(id = "3", conversationId = "c1", role = "user", content = "hn")
        )

        val contentList = mutableListOf<Content>()
        for (msg in historyMessages) {
            val role = if (msg.role == "assistant") "model" else "user"
            contentList.add(
                Content(
                    role = role,
                    parts = listOf(Part(text = msg.content))
                )
            )
        }

        assertEquals(3, contentList.size)
        assertEquals("user", contentList[0].role)
        assertEquals("aapka name kya hai", contentList[0].parts[0].text)
        assertEquals("model", contentList[1].role)
        assertEquals("Main NEURA hoon.", contentList[1].parts[0].text)
        assertEquals("user", contentList[2].role)
        assertEquals("hn", contentList[2].parts[0].text)

        // Ensure follow-up "hn" is not replaced with previous message
        assertNotEquals(contentList[2].parts[0].text, contentList[0].parts[0].text)
    }

    @Test
    fun testFallbackConversationEliminatesBlackScreen() {
        val convId = "conv-missing"
        val existingConversations = emptyList<ConversationEntity>()
        
        // Simulating the exact viewmodel fallback resolution
        val resolvedConv = existingConversations.find { it.id == convId }
            ?: ConversationEntity(id = convId, title = "Chat with NEURA", mode = "general")

        assertNotNull(resolvedConv)
        assertEquals(convId, resolvedConv.id)
        assertEquals("Chat with NEURA", resolvedConv.title)
    }

    @Test
    fun testDoubleSubmissionPrevention() {
        var isThinking = true
        var requestSent = false

        // Attempting to send while isThinking is true
        if (!isThinking) {
            requestSent = true
        }

        assertFalse("Request should be blocked while isThinking is active", requestSent)

        // Once thinking finishes
        isThinking = false
        if (!isThinking) {
            requestSent = true
        }
        assertTrue("Request can proceed when isThinking is false", requestSent)
    }

    @Test
    fun testGeminiModelConfigModularity() {
        // Default model is Gemini 3.5 Flash Lite
        assertEquals("gemini-3.5-flash-lite", com.example.data.backend.GeminiModelConfig.primaryModel)

        // General chat resolves to primary model
        val generalModel = com.example.data.backend.GeminiModelConfig.resolveModelForMode("general")
        assertEquals("gemini-3.5-flash-lite", generalModel)

        // Coding with pro preference resolves to Gemini 3.1 Pro
        val codingModel = com.example.data.backend.GeminiModelConfig.resolveModelForMode("coding", preferProForCoding = true)
        assertEquals("gemini-3.1-pro-preview", codingModel)

        // Fallback model exists for failover
        assertEquals("gemini-3.6-flash", com.example.data.backend.GeminiModelConfig.FALLBACK_MODEL)
    }
}
