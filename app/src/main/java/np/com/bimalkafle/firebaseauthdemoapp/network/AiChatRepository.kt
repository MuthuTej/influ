package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import np.com.bimalkafle.firebaseauthdemoapp.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class ChatEntity(val type: String, val id: String, val label: String)

data class AiChatResponse(val reply: String, val entities: List<ChatEntity>)

/** One turn of the Gemini `Content[]` history shape the backend expects back on each call. */
data class AiChatTurn(val role: String, val text: String)

/**
 * Plain REST client for the chatbot endpoint — deliberately not a GraphQL
 * call like the rest of this package's repositories, since `/api/chat` is a
 * plain Express route on the same backend (alongside /api/ml, /api/youtube,
 * /api/analytics), not part of the Apollo schema.
 */
object AiChatRepository {
    // BACKEND_BASE_URL points at .../graphql — swap that suffix for the REST chat route.
    private val CHAT_URL = BuildConfig.BACKEND_BASE_URL.removeSuffix("/graphql") + "/api/chat"

    suspend fun sendMessage(message: String, history: List<AiChatTurn>, token: String): Result<AiChatResponse> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(CHAT_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.doOutput = true
                // A single chat turn can chain several tool calls (resolve a
                // campaign, pull its status, check each collaboration), each
                // a full Gemini round trip — and the backend's free-tier host
                // can take 30-60s to wake from a cold start. Defaults are too
                // short for that and the connection gets aborted mid-wait.
                connection.connectTimeout = 30_000
                connection.readTimeout = 90_000

                val historyJson = JSONArray()
                history.forEach { turn ->
                    historyJson.put(
                        JSONObject().apply {
                            put("role", turn.role)
                            put("parts", JSONArray().put(JSONObject().apply { put("text", turn.text) }))
                        }
                    )
                }

                val requestBody = JSONObject().apply {
                    put("message", message)
                    put("history", historyJson)
                }.toString()

                connection.outputStream.use { it.write(requestBody.toByteArray()) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    SessionManager.notifySessionExpired()
                    return@withContext Result.failure(UnauthorizedException())
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val entities = mutableListOf<ChatEntity>()
                    val entitiesJson = json.optJSONArray("entities")
                    if (entitiesJson != null) {
                        for (i in 0 until entitiesJson.length()) {
                            val e = entitiesJson.getJSONObject(i)
                            entities.add(ChatEntity(e.getString("type"), e.getString("id"), e.optString("label", e.getString("id"))))
                        }
                    }
                    Result.success(AiChatResponse(reply = json.optString("reply", ""), entities = entities))
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("AiChatRepository", "Error Response: $errorResponse")
                    Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
                }
            } catch (e: SocketTimeoutException) {
                Log.e("AiChatRepository", "Timeout: ${e.message}", e)
                Result.failure(Exception("That question took too long to answer — the server may be waking up from idle. Please try again."))
            } catch (e: IOException) {
                Log.e("AiChatRepository", "Network error: ${e.message}", e)
                Result.failure(Exception("Lost connection while waiting for a reply. Check your connection and try again."))
            } catch (e: Exception) {
                Log.e("AiChatRepository", "Exception: ${e.message}", e)
                Result.failure(e)
            }
        }
}
