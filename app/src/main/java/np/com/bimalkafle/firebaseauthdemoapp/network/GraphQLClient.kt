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
            connection.setRequestProperty("Connection", "close")
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

                val jsonResponse = try {
                    JSONObject(response.toString())
                } catch (e: org.json.JSONException) {
                    Log.e("GraphQLClient", "Malformed JSON response: $response", e)
                    return Result.failure(MalformedResponseException("Server returned an unreadable response", e))
                }

                // GraphQL errors: if there is also a "data" field, treat as partial success
                // so one bad field (e.g. invalid enum in a nested type) doesn't wipe
                // out all the successfully-resolved data. Hard-fail only when there is
                // no data at all (the query itself was rejected entirely).
                val errors = jsonResponse.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    val firstErrorMessage = errors.optJSONObject(0)?.optString("message") ?: "Unknown GraphQL Error"
                    val hasData = !jsonResponse.isNull("data")
                    Log.e("GraphQLClient", "GraphQL Errors (hasData=$hasData): $firstErrorMessage")
                    if (!hasData) {
                        return Result.failure(Exception(firstErrorMessage))
                    }
                    // Partial: data is present alongside errors — proceed with what we have
                }

                return Result.success(jsonResponse)
            } else {
                return Result.failure(ServerException(responseCode, "HTTP Error: $responseCode (No error body)"))
            }
        } finally {
            connection.disconnect()
        }
    }
}
