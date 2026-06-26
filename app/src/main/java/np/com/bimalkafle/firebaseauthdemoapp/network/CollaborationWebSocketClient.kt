package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import np.com.bimalkafle.firebaseauthdemoapp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WebSocket client implementing the graphql-transport-ws protocol (Apollo Server 3+).
 *
 * Two modes:
 *   collaborationId != null  → subscribes to `collaborationUpdated(collaborationId)` for one
 *                              specific collaboration (used by ChatScreen).
 *   collaborationId == null  → subscribes to `anyCollaborationUpdated` for all collaborations
 *                              belonging to the auth user (used by ProposalPage).
 *
 * connect()    — opens the socket and starts the subscription
 * disconnect() — sends a clean close frame and tears down the connection
 * onUpdate fires whenever the server pushes a "next" message.
 */
class CollaborationWebSocketClient(
    private val token: String,
    private val collaborationId: String?,
    private val onUpdate: () -> Unit
) {
    companion object {
        private const val TAG = "CollabWsClient"
        private const val SUB_ID = "collab_sub"
        private val WS_URL = BuildConfig.BACKEND_BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)

    fun connect() {
        if (isConnected.getAndSet(true)) return
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Sec-WebSocket-Protocol", "graphql-transport-ws")
            .build()
        webSocket = client.newWebSocket(request, Listener())
        Log.d(TAG, "Connecting [${if (collaborationId != null) "collab=$collaborationId" else "global"}]")
    }

    fun disconnect() {
        webSocket?.send(JSONObject().apply {
            put("type", "complete")
            put("id", SUB_ID)
        }.toString())
        webSocket?.close(1000, "Screen closed")
        webSocket = null
        isConnected.set(false)
    }

    private fun buildSubscribePayload(): String {
        return if (collaborationId != null) {
            // Specific collaboration — used by ChatScreen
            JSONObject().apply {
                put("type", "subscribe")
                put("id", SUB_ID)
                put("payload", JSONObject().apply {
                    put("query", """
                        subscription OnCollaborationUpdated(${'$'}collaborationId: ID!) {
                            collaborationUpdated(collaborationId: ${'$'}collaborationId) { id status }
                        }
                    """.trimIndent())
                    put("variables", JSONObject().apply {
                        put("collaborationId", collaborationId)
                    })
                })
            }.toString()
        } else {
            // Global — used by ProposalPage; backend uses JWT to scope to current user
            JSONObject().apply {
                put("type", "subscribe")
                put("id", SUB_ID)
                put("payload", JSONObject().apply {
                    put("query", """
                        subscription {
                            anyCollaborationUpdated { id status }
                        }
                    """.trimIndent())
                    put("variables", JSONObject())
                })
            }.toString()
        }
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
                        Log.d(TAG, "Collaboration update received")
                        onUpdate()
                    }
                }
                "ping" -> ws.send(JSONObject().apply { put("type", "pong") }.toString())
                "error" -> Log.w(TAG, "Server error: $text")
                "complete" -> { isConnected.set(false); Log.d(TAG, "Subscription completed") }
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "WS failure: ${t.message}")
            isConnected.set(false)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            isConnected.set(false)
        }
    }
}
