package com.jarvis.assistant.ai

import com.jarvis.assistant.model.ChatMessage

/**
 * AI का जवाब मिलने पर इसके callbacks हमेशा MAIN thread पर आते हैं।
 */
interface AiCallback {
    fun onSuccess(reply: String)
    fun onError(message: String)
}

/**
 * यह वो अलग layer है जहाँ AI request/response handling रहती है।
 * बाद में कोई भी नया AI model जोड़ने के लिए बस इस interface का नया class बनाएँ।
 */
interface AiService {
    val displayName: String
    val isMock: Boolean
    fun generateReply(history: List<ChatMessage>, callback: AiCallback)
}