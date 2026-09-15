package com.example.data.auth

import com.example.BuildConfig

object AuthConfig {
    /**
     * Web Client ID from Google Cloud Console / Firebase Console
     * (Type: Web application OAuth 2.0 Client ID)
     * e.g., 760692900373-xxxxxxxxxx.apps.googleusercontent.com
     */
    val GOOGLE_WEB_CLIENT_ID: String
        get() {
            return try {
                val key = BuildConfig.GOOGLE_WEB_CLIENT_ID
                if (!key.isNullOrBlank() && key != "null" && key != "not_configured" && key != "DEFAULT_CLIENT_ID") key else ""
            } catch (_: Exception) {
                ""
            }
        }

    fun isGoogleAuthReady(): Boolean {
        return GOOGLE_WEB_CLIENT_ID.isNotBlank()
    }
}
