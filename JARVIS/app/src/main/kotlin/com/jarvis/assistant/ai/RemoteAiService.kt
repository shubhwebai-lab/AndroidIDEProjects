package com.jarvis.assistant.ai

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import com.jarvis.assistant.model.ChatMessage
import com.jarvis.assistant.model.Role
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RemoteAiService(
    private val context: Context,
    private val baseUrl: String
) : AiService {

    override val displayName: String = "Online"
    override val isMock: Boolean = false

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun generateReply(history: List<ChatMessage>, callback: AiCallback) {
        if (baseUrl.isBlank()) {
            postError(callback, "AI backend URL is not configured.")
            return
        }

        executor.execute {
            if (!isOnline()) {
                postError(callback, "No internet connection. Please check your network and try again.")
                return@execute
            }

            var connection: HttpURLConnection? = null
            try {
                val url = URL(baseUrl.trimEnd('/') + "/chat")
                val conn = url.openConnection() as HttpURLConnection
                connection = conn
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("Accept", "application/json")

                val body = buildRequestBody(history)
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                if (code !in 200..299) {
                    postError(callback, "Server error ($code). Please try again later.")
                    return@execute
                }

                val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val reply = JSONObject(raw).optString("reply", "")
                if (reply.isBlank()) {
                    postError(callback, "JARVIS returned an empty response.")
                } else {
                    mainHandler.post { callback.onSuccess(reply) }
                }
            } catch (e: UnknownHostException) {
                postError(callback, "Cannot reach the server. Please check your internet connection.")
            } catch (e: SocketTimeoutException) {
                postError(callback, "The request timed out. Please try again.")
            } catch (e: IOException) {
                postError(callback, "Network request failed. Please try again.")
            } catch (e: Exception) {
                postError(callback, "Unexpected error. Please try again.")
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun buildRequestBody(history: List<ChatMessage>): String {
        val messages = JSONArray()
        for (m in history) {
            if (m.isError) continue
            val role = if (m.role == Role.USER) "user" else "assistant"
            messages.put(JSONObject().put("role", role).put("content", m.text))
        }
        return JSONObject().put("messages", messages).toString()
    }

    @Suppress("DEPRECATION")
    private fun isOnline(): Boolean {
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val info = cm.activeNetworkInfo
        return info != null && info.isConnected
    }

    private fun postError(callback: AiCallback, message: String) {
        mainHandler.post { callback.onError(message) }
    }
}