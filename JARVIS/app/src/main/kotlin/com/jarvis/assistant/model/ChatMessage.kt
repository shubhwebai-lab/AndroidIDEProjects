package com.jarvis.assistant.model

import java.util.UUID

enum class Role {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)