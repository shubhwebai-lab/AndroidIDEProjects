package com.jarvis.assistant.data

import android.content.Context
import com.jarvis.assistant.model.ChatMessage
import com.jarvis.assistant.model.Role
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Chat history local storage (SharedPreferences) में save करता है।
 * conversationId की वजह से future में कई conversations जोड़ना आसान रहेगा।
 */
class ChatStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("jarvis_chat_storage", Context.MODE_PRIVATE)

    fun load(conversationId: String): List<ChatMessage> {
        return try {
            val raw = prefs.getString(keyFor(conversationId), null)
            if (raw.isNullOrEmpty()) {
                emptyList()
            } else {
                val array = JSONArray(raw)
                val result = ArrayList<ChatMessage>()
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val role = if (o.optString("role") == "assistant") Role.ASSISTANT else Role.USER
                    result.add(
                        ChatMessage(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            role = role,
                            text = o.optString("text", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            isError = o.optBoolean("isError", false)
                        )
                    )
                }
                result
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(conversationId: String, messages: List<ChatMessage>) {
        try {
            val array = JSONArray()
            for (m in messages) {
                val o = JSONObject()
                o.put("id", m.id)
                o.put("role", if (m.role == Role.USER) "user" else "assistant")
                o.put("text", m.text)
                o.put("timestamp", m.timestamp)
                o.put("isError", m.isError)
                array.put(o)
            }
            prefs.edit().putString(keyFor(conversationId), array.toString()).apply()
        } catch (e: Exception) {
            // Save fail होने पर भी app crash नहीं होगा।
        }
    }

    fun clear(conversationId: String) {
        try {
            prefs.edit().remove(keyFor(conversationId)).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun keyFor(conversationId: String): String = "conversation_" + conversationId

    companion object {
        const val DEFAULT_CONVERSATION_ID = "main"
    }
}