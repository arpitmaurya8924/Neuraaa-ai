package com.example

import com.example.data.config.NeuraConfig
import com.example.data.engine.NeuraIntelligenceEngine
import com.example.ui.components.Screen
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun bottomNavItems_areAllNonNullAndValid() {
    val items = Screen.bottomNavItems
    assertEquals(5, items.size)
    items.forEach { screen ->
      assertNotNull(screen)
      assertNotNull(screen.route)
      assertNotNull(screen.title)
      assertNotNull(screen.selectedIcon)
      assertNotNull(screen.unselectedIcon)
      assertTrue(screen.route.isNotEmpty())
    }
  }

  @Test
  fun fastIntent_routesThroughToRealGemini() {
    val res1 = NeuraIntelligenceEngine.evaluateFastIntent("2 + 2?", emptyList())
    assertTrue(res1 is NeuraIntelligenceEngine.FastIntentResult.StandardPassThrough)

    val res2 = NeuraIntelligenceEngine.evaluateFastIntent("Who developed you?", emptyList())
    assertTrue(res2 is NeuraIntelligenceEngine.FastIntentResult.StandardPassThrough)

    val res3 = NeuraIntelligenceEngine.evaluateFastIntent("Kal exam hai aur kuch nahi padha 😭", emptyList())
    assertTrue(res3 is NeuraIntelligenceEngine.FastIntentResult.StandardPassThrough)
  }

  @Test
  fun followUpContext_resolvesPronouns() {
    val history = listOf("assistant: Integration is the reverse process of differentiation.")
    val res = NeuraIntelligenceEngine.evaluateFastIntent("basic se", history)
    assertTrue(res is NeuraIntelligenceEngine.FastIntentResult.ContextEnriched)
    val enriched = (res as NeuraIntelligenceEngine.FastIntentResult.ContextEnriched)
    assertTrue(enriched.resolvedPrompt.contains("foundational basics"))
  }

  @Test
  fun systemPrompt_embedsCoreDirectives() {
    val prompt = NeuraIntelligenceEngine.buildMasterSystemPrompt(
      personality = "Friendly 😄",
      languagePreference = "English / Hindi / Hinglish",
      responseLength = "Balanced",
      memoryContext = "- Language Preference: Hinglish",
      customModeInstruction = null
    )
    assertTrue(prompt.contains(NeuraConfig.TAGLINE))
    assertTrue(prompt.contains("FAST & DIRECT"))
    assertTrue(prompt.contains("THOUGHTFUL WHEN COMPLEX"))
    assertTrue(prompt.contains("Hinglish"))
    assertTrue(prompt.contains("Arpit"))
  }

  @Test
  fun versionAndReleaseConfig_areValid() {
    assertEquals("1.0.0", NeuraConfig.VERSION_NAME)
    assertEquals("14 September 2026", NeuraConfig.RELEASE_DATE)
    assertEquals("NEURA", NeuraConfig.APP_NAME)
    assertEquals("Your Intelligent Companion", NeuraConfig.TAGLINE)
    assertEquals("Arpit", NeuraConfig.DEVELOPER_NAME)
  }

  @Test
  fun authValidation_emailAndPasswords() {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    assertTrue(emailRegex.matches("arpitmaurya8924@gmail.com"))
    assertTrue(emailRegex.matches("user@example.co"))
    assertFalse(emailRegex.matches("invalid-email"))
    assertFalse(emailRegex.matches("@missinguser.com"))

    val validPassword = "SecurePassword123"
    assertTrue(validPassword.length >= 6)
    assertTrue(validPassword.any { it.isDigit() })
    assertTrue(validPassword.any { it.isLetter() })

    val weakPasswordNoDigit = "abcdef"
    assertFalse(weakPasswordNoDigit.any { it.isDigit() })
  }
}
