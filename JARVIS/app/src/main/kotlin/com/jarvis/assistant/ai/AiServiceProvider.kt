package com.jarvis.assistant.ai

import android.content.Context

/**
 * बाद में AI backend जोड़ने की जगह यही है।
 * BACKEND_BASE_URL में अपने server का https URL डालें, जैसे "https://your-server.example.com"
 * यह URL secret नहीं है। API key कभी यहाँ मत डालना, key सिर्फ़ आपके server पर रहेगी।
 * खाली रहने पर app local demo mode में चलेगा।
 */
object AiServiceProvider {

    private const val BACKEND_BASE_URL = ""

    private var instance: AiService? = null

    fun get(context: Context): AiService {
        val existing = instance
        if (existing != null) {
            return existing
        }
        val created: AiService = if (BACKEND_BASE_URL.isBlank()) {
            MockAiService()
        } else {
            RemoteAiService(context.applicationContext, BACKEND_BASE_URL)
        }
        instance = created
        return created
    }
}