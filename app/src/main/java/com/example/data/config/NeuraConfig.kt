package com.example.data.config

object NeuraConfig {
    const val APP_NAME = "NEURA"
    const val TAGLINE = "Your Intelligent Companion"
    const val DEVELOPER_NAME = "Arpit"
    const val DEVELOPER_EMAIL = "arpitmaurya8924@gmail.com"
    const val VERSION_NAME = "1.0.0"
    const val RELEASE_DATE = "14 September 2026"
    const val OFFICIAL_BRAND_DISCLAIMER = "The AI and application name is strictly NEURA. Never use 'Arpit AI'."

    fun getDeveloperResponse(languageHint: String): String {
        val isHindi = languageHint.contains("Hindi", ignoreCase = true) || languageHint.contains("Hinglish", ignoreCase = true)
        return if (isHindi) {
            "Mujhe **$DEVELOPER_NAME** ne develop kiya hai. Mera application name **$APP_NAME — “$TAGLINE”** hai (Version $VERSION_NAME, Release: $RELEASE_DATE).\n\nAap developer support ke liye **$DEVELOPER_EMAIL** par sampark kar sakte hain."
        } else {
            "I was developed by **$DEVELOPER_NAME**. My application name is **$APP_NAME — “$TAGLINE”** (Version $VERSION_NAME, released on $RELEASE_DATE).\n\nFor questions or support, you can reach out to **$DEVELOPER_EMAIL**."
        }
    }
}
