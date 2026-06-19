package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import np.com.bimalkafle.firebaseauthdemoapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL

object GraphQLClient {

    private val BASE_URL = BuildConfig.BACKEND_BASE_URL
    private const val MAX_RETRIES = 3
    private const val RETRY_DELAY_MS = 1000L

    suspend fun query(query: String, variables: Map<String, Any>? = null, token: String? = null): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            for (attempt in 1..MAX_RETRIES) {
                try {
                    val result = executeRequest(query, variables, token)
                    return@withContext result
                } catch (e: SocketException) {
                    Log.w("GraphQLClient", "SocketException on attempt $attempt: ${e.message}")
                    lastException = e
                    if (attempt < MAX_RETRIES) {
                        delay(RETRY_DELAY_MS * attempt)
                    }
                } catch (e: Exception) {
                    Log.e("GraphQLClient", "Non-retryable Exception: ${e.message}")
                    return@withContext Result.failure(e)
                }
            }
            Result.failure(lastException ?: Exception("Unknown network error after $MAX_RETRIES retries"))
        }
    }

    private fun executeRequest(query: String, variables: Map<String, Any>? = null, token: String? = null): Result<JSONObject> {
        val url = URL(BASE_URL)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Connection", "close") // Try disabling keep-alive to avoid stale connections
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            connection.doOutput = true

            val jsonBody = JSONObject()
            jsonBody.put("query", query)
            if (variables != null) {
                val variablesJson = JSONObject(variables)
                jsonBody.put("variables", variablesJson)
            }

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                Log.w("GraphQLClient", "Auth rejected by backend (HTTP $responseCode) — notifying session expiry")
                SessionManager.notifySessionExpired()
                return Result.failure(UnauthorizedException())
            }

            val stream = if (responseCode == HttpURLConnection.HTTP_OK) connection.inputStream else connection.errorStream

            if (stream != null) {
                val reader = BufferedReader(InputStreamReader(stream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("GraphQLClient", "Server Error Body: $response")
                    return Result.failure(ServerException(responseCode, "HTTP Error: $responseCode - $response"))
                }

                return try {
                    Result.success(JSONObject(response.toString()))
                } catch (e: org.json.JSONException) {
                    Log.e("GraphQLClient", "Malformed JSON response: $response", e)
                    Result.failure(MalformedResponseException("Server returned an unreadable response", e))
                }
            } else {
                return Result.failure(ServerException(responseCode, "HTTP Error: $responseCode (No error body)"))
            }
        } finally {
            connection.disconnect()
        }
    }
}
