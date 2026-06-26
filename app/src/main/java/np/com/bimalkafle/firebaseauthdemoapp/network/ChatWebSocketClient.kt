package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
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

/**
 * OkHttp WebSocket for real-time chat using graphql-transport-ws protocol.
 *
 * The backend identifies the subscriber from the JWT passed in connection_init,
 * so subscriptions do not need explicit senderId / receiverId arguments.
 *
 * Two modes:
 *   connectForConversation(collaborationId)
 *       Subscribes to `messageAdded(collaborationId)` — all messages in one
 *       collaboration addressed to the current user.
 *
 *   connectForChatList()
 *       Subscribes to `anyMessageAdded` — any message addressed to the current
 *       user, across all conversations. Used to update the chat list live.
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
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val connected = AtomicBoolean(false)

    private var isConversationMode = false
    private var collaborationId: String? = null

    // ── Public API ────────────────────────────────────────────────────────────

    fun connectForConversation(collaborationId: String?) {
        this.isConversationMode = true
        this.collaborationId = collaborationId
        openSocket()
    }

    fun connectForChatList() {
        this.isConversationMode = false
        openSocket()
    }

    fun disconnect() {
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
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Sec-WebSocket-Protocol", "graphql-transport-ws")
            .build()
        webSocket = httpClient.newWebSocket(request, Listener())
        Log.d(TAG, "Connecting [${if (isConversationMode) "conversation collab=$collaborationId" else "chatList"}]")
    }

    private fun buildSubscribePayload(): String {
        val query: String
        val variables: JSONObject

        if (isConversationMode) {
            // Backend uses JWT to know the receiver; collaborationId scopes the conversation.
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
            // No args — backend delivers any message addressed to the auth user.
            query = """
                subscription {
                    anyMessageAdded {
                        id text senderId receiverId timestamp timeFormatted
                        type collaborationId isRead
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

    private inner class Listener : WebSocketListener() {

        override fun onOpen(ws: WebSocket, response: Response) {
            ws.send(JSONObject().apply {
                put("type", "connection_init")
                put("payload", JSONObject().apply {
                    put("Authorization", "Bearer $token")
                })
            }.toString())
        }

        override fun onMessage(ws: WebSocket, text: String) {
            val json = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (json.optString("type")) {
                "connection_ack" -> {
                    Log.d(TAG, "ACK — subscribing")
                    ws.send(buildSubscribePayload())
                }
                "next" -> {
                    if (json.optString("id") == SUB_ID) {
                        json.optJSONObject("payload")?.let { parseMessage(it)?.let(onMessage) }
                    }
                }
                "ping" -> ws.send(JSONObject().apply { put("type", "pong") }.toString())
                "error" -> Log.w(TAG, "Server error: $text")
                "complete" -> { connected.set(false); Log.d(TAG, "Subscription completed") }
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "WS failure: ${t.message}")
            connected.set(false)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            connected.set(false)
        }
    }
}
