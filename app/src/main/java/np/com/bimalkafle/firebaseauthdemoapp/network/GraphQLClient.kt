package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GraphQLClient {

    private const val BASE_URL = "https://connect-backend-e22a.onrender.com/graphql"

    suspend fun query(query: String, variables: Map<String, Any>? = null, token: String? = null): Result<JSONObject> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(BASE_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
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
                val stream = if (responseCode == HttpURLConnection.HTTP_OK) connection.inputStream else connection.errorStream
                
                if (stream != null) {
                    val reader = BufferedReader(InputStreamReader(stream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    val jsonResponse = JSONObject(response.toString())
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        Result.success(jsonResponse)
                    } else {
                        Log.e("GraphQLClient", "Server Error Body: $response")
                        Result.failure(Exception("HTTP Error: $responseCode - $response"))
                    }
                } else {
                    Result.failure(Exception("HTTP Error: $responseCode (No error body)"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
