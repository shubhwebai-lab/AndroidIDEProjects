package com.jarvis.assistant.ai

import android.os.Handler
import android.os.Looper
import com.jarvis.assistant.model.ChatMessage
import com.jarvis.assistant.model.Role
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * यह सिर्फ़ LOCAL DEMO है। इसमें कोई real AI नहीं है।
 */
class MockAiService : AiService {

    override val displayName: String = "Local demo mode"
    override val isMock: Boolean = true

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun generateReply(history: List<ChatMessage>, callback: AiCallback) {
        executor.execute {
            try {
                Thread.sleep(1000)
                val lastUserText = history.lastOrNull { it.role == Role.USER }?.text.orEmpty()
                val reply = buildReply(lastUserText)
                mainHandler.post { callback.onSuccess(reply) }
            } catch (e: Exception) {
                mainHandler.post { callback.onError("Unexpected error. Please try again.") }
            }
        }
    }

    private fun buildReply(userText: String): String {
        val lower = userText.trim().lowercase()
        return if (lower == "hi" || lower.startsWith("hi ") ||
            lower.startsWith("hello") || userText.trim().startsWith("नमस्ते")
        ) {
            "Hello! I am JARVIS. Right now I am running in local demo mode, so my replies are not from a real AI yet."
        } else {
            "Local demo mode: मुझे आपका message मिल गया:\n\n\"" + userText + "\"\n\n" +
                "अभी real AI connected नहीं है। Backend जुड़ने के बाद मैं असली जवाब दूँगा।"
        }
    }
}
