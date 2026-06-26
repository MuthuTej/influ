package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.BuildConfig
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
 * WebSocket client for real-time collaboration status updates (graphql-transport-ws).
 *
 * Always subscribes to `anyCollaborationUpdated` (backend scopes it to the auth user via JWT).
 * When [collaborationId] is provided, only fires [onUpdate] for updates matching that ID.
 * When null, fires for every update (used by ProposalPage).
 *
 * Reconnects automatically on unexpected disconnects with exponential backoff.
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
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var reconnectJob: Job? = null

    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val generation = AtomicInteger(0)
    private var shouldReconnect = true
    private var reconnectAttempts = 0

    fun connect() {
        shouldReconnect = true
        reconnectAttempts = 0
        openSocket()
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectJob?.cancel()
        reconnectJob = null
        generation.incrementAndGet()
        webSocket?.send(JSONObject().apply {
            put("type", "complete")
            put("id", SUB_ID)
        }.toString())
        webSocket?.close(1000, "Screen closed")
        webSocket = null
        isConnected.set(false)
    }

    private fun openSocket() {
        if (isConnected.getAndSet(true)) return
        val gen = generation.incrementAndGet()
        val request = Request.Builder()
            .url(WS_URL)
            .addHeader("Sec-WebSocket-Protocol", "graphql-transport-ws")
            .build()
        webSocket = client.newWebSocket(request, Listener(gen))
        Log.d(TAG, "Connecting gen=$gen [${if (collaborationId != null) "collab=$collaborationId" else "global"}]")
    }

    private fun scheduleReconnect(gen: Int) {
        if (!shouldReconnect) return
        if (gen != generation.get()) return
        reconnectJob?.cancel()
        val delayMs = minOf(1500L * (1L shl reconnectAttempts), 30_000L)
        reconnectAttempts = minOf(reconnectAttempts + 1, 6)
        Log.d(TAG, "Reconnecting in ${delayMs}ms (attempt $reconnectAttempts)")
        reconnectJob = scope.launch {
            delay(delayMs)
            if (shouldReconnect && !isConnected.get()) {
                openSocket()
            }
        }
    }

    private val subscribePayload: String = JSONObject().apply {
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
                    ws.send(subscribePayload)
                }
                "next" -> {
                    if (json.optString("id") == SUB_ID) {
                        val receivedId = json
                            .optJSONObject("payload")
                            ?.optJSONObject("data")
                            ?.optJSONObject("anyCollaborationUpdated")
                            ?.optString("id")
                        // Filter by collaborationId when scoped; always fire when global
                        if (collaborationId == null || receivedId == collaborationId || receivedId.isNullOrBlank()) {
                            Log.d(TAG, "Collaboration update gen=$gen id=$receivedId")
                            onUpdate()
                        }
                    }
                }
                "ping"     -> ws.send(JSONObject().apply { put("type", "pong") }.toString())
                "error"    -> Log.w(TAG, "Server error gen=$gen: $text")
                "complete" -> {
                    if (!isStale()) {
                        isConnected.set(false)
                        Log.d(TAG, "Subscription completed gen=$gen")
                        scheduleReconnect(gen)
                    }
                }
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            if (isStale()) return
            Log.w(TAG, "WS failure gen=$gen: ${t.message}")
            isConnected.set(false)
            scheduleReconnect(gen)
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            if (isStale()) return
            isConnected.set(false)
            if (code != 1000) {
                Log.w(TAG, "WS closed unexpectedly gen=$gen code=$code reason=$reason")
                scheduleReconnect(gen)
            }
        }
    }
}
