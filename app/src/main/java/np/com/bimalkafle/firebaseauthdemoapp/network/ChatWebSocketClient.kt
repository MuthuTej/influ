package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.BuildConfig
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * OkHttp WebSocket for real-time chat using graphql-transport-ws protocol.
 *
 * Two modes:
 *   connectForConversation(collaborationId) — subscribes to messageAdded
 *   connectForChatList()                    — subscribes to anyMessageAdded
 *
 * Automatically reconnects on unexpected disconnects using exponential backoff.
 */
class ChatWebSocketClient(
    private val token: String,
    private val currentUserId: String,
    private val onMessage: (ChatMessage) -> Unit
) {
    companion object {
        private const val TAG = "ChatWsClient"
        private const val SUB_ID = "chat_sub"
        private val WS_URL = BuildConfig.BACKEND_BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
    }

    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null

    private var webSocket: WebSocket? = null
    private val connected = AtomicBoolean(false)
    private val generation = AtomicInteger(0)  // incremented every openSocket(); stale callbacks are ignored
    private var shouldReconnect = true
    private var reconnectAttempts = 0

    private var isConversationMode = false
    private var collaborationId: String? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun connectForConversation(collaborationId: String?) {
        this.isConversationMode = true
        this.collaborationId = collaborationId
        shouldReconnect = true
        reconnectAttempts = 0
        openSocket()
    }

    fun connectForChatList() {
        this.isConversationMode = false
        shouldReconnect = true
        reconnectAttempts = 0
        openSocket()
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        generation.incrementAndGet()   // invalidate in-flight listener
        webSocket?.send(JSONObject().apply {
            put("type", "complete")
            put("id", SUB_ID)
        }.toString())
        webSocket?.close(1000, "Closed by client")
        webSocket = null
        connected.set(false)
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun openSocket() {
        if (connected.getAndSet(true)) return
        val gen = generation.incrementAndGet()
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Sec-WebSocket-Protocol", "graphql-transport-ws")
            .build()
        webSocket = httpClient.newWebSocket(request, Listener(gen))
        Log.d(TAG, "Connecting gen=$gen [${if (isConversationMode) "conversation collab=$collaborationId" else "chatList"}]")
    }

    private fun scheduleReconnect(gen: Int) {
        if (!shouldReconnect) return
        if (gen != generation.get()) return  // already superseded
        reconnectJob?.cancel()
        val delayMs = minOf(1500L * (1L shl reconnectAttempts), 30_000L)
        reconnectAttempts = minOf(reconnectAttempts + 1, 6)
        Log.d(TAG, "Reconnecting in ${delayMs}ms (attempt $reconnectAttempts)")
        reconnectJob = scope.launch {
            delay(delayMs)
            if (shouldReconnect && !connected.get()) {
                openSocket()
            }
        }
    }

    private fun buildSubscribePayload(): String {
        val query: String
        val variables: JSONObject

        if (isConversationMode) {
            query = """
                subscription OnMessageAdded(${'$'}collaborationId: String) {
                    messageAdded(collaborationId: ${'$'}collaborationId) {
                        id text senderId receiverId timestamp timeFormatted
                        type collaborationId replyToId isRead metadata
                    }
                }
            """.trimIndent()
            variables = JSONObject().apply {
                put("collaborationId", collaborationId ?: JSONObject.NULL)
            }
        } else {
            query = """
                subscription {
                    anyMessageAdded {
                        id text senderId receiverId timestamp timeFormatted
                        type collaborationId isRead metadata
                    }
                }
            """.trimIndent()
            variables = JSONObject()
        }

        return JSONObject().apply {
            put("type", "subscribe")
            put("id", SUB_ID)
            put("payload", JSONObject().apply {
                put("query", query)
                put("variables", variables)
            })
        }.toString()
    }

    private fun parseMessage(payload: JSONObject): ChatMessage? {
        val data = payload.optJSONObject("data") ?: return null
        val obj = data.optJSONObject("messageAdded")
            ?: data.optJSONObject("anyMessageAdded")
            ?: return null

        val meta = mutableMapOf<String, Any>()
        obj.optJSONObject("metadata")?.let { m -> m.keys().forEach { k -> meta[k] = m.get(k) } }

        return ChatMessage(
            id              = obj.optString("id"),
            text            = obj.optString("text"),
            senderId        = obj.optString("senderId"),
            receiverId      = obj.optString("receiverId"),
            timestamp       = obj.optLong("timestamp"),
            timeFormatted   = obj.optString("timeFormatted"),
            type            = obj.optString("type", "TEXT"),
            metadata        = meta,
            collaborationId = obj.optString("collaborationId").takeIf { it.isNotBlank() },
            replyToId       = obj.optString("replyToId").takeIf { it.isNotBlank() },
            isRead          = obj.optBoolean("isRead", false),
            isMe            = obj.optString("senderId") == currentUserId
        )
    }

    private inner class Listener(private val gen: Int) : WebSocketListener() {

        private fun isStale() = gen != generation.get()

        override fun onOpen(ws: WebSocket, response: Response) {
            if (isStale()) return
            reconnectAttempts = 0
            ws.send(JSONObject().apply {
                put("type", "connection_init")
                put("payload", JSONObject().apply {
                    put("Authorization", "Bearer $token")
                })
            }.toString())
        }

        override fun onMessage(ws: WebSocket, text: String) {
            if (isStale()) return
            val json = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (json.optString("type")) {
                "connection_ack" -> {
                    Log.d(TAG, "ACK gen=$gen — subscribing")
                    ws.send(buildSubscribePayload())
                }
                "next" -> {
                    if (json.optString("id") == SUB_ID) {
                        json.optJSONObject("payload")?.let { parseMessage(it)?.let(onMessage) }
                    }
                }
                "ping" -> ws.send(JSONObject().apply { put("type", "pong") }.toString())
                "error" -> Log.w(TAG, "Server error gen=$gen: $text")
                "complete" -> {
                    if (!isStale()) {
                        connected.set(false)
                        Log.d(TAG, "Subscription completed gen=$gen")
                        scheduleReconnect(gen)
                    }
                }
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            if (isStale()) return
            Log.w(TAG, "WS failure gen=$gen: ${t.message}")
            connected.set(false)
            scheduleReconnect(gen)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            if (isStale()) return
            connected.set(false)
            if (code != 1000) {
                Log.w(TAG, "WS closed unexpectedly gen=$gen code=$code reason=$reason")
                scheduleReconnect(gen)
            }
        }
    }
}
