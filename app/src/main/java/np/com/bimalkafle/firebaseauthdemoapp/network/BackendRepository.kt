package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BackendRepository {
    private const val BACKEND_URL = "https://connect-backend-e22a.onrender.com/graphql"

    suspend fun signUp(name: String, role: String, token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            // GraphQL Mutation
            val query = """
                mutation SignUp(${'$'}name: String!, ${'$'}role: Role!) {
                    signUp(name: ${'$'}name, role: ${'$'}role) {
                        __typename
                        id
                        name
                        role
                        email
                    }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("name", name)
                put("role", role)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    Result.success(response)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("BackendRepository", "Error Response: $errorResponse")
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRole(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                query {
                    me {
                        __typename
                        role
                        profileCompleted
                    }
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("query", query)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val data = jsonResponse.getJSONObject("data").getJSONObject("me")
                    val role = data.getString("role")
                    val isProfileCompleted = data.optBoolean("profileCompleted", false)
                    // Return both role and completion status as a formatted string or pair
                    // Since Result returns String, we'll pack it: "ROLE|TRUE"
                    Result.success("$role|$isProfileCompleted")
                }
            } else {
                Result.failure(Exception("Server returned code $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setupInfluencerProfile(input: JSONObject, token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation SetupInfluencerProfile(${'$'}input: InfluencerProfileInput!) {
                    setupInfluencerProfile(input: ${'$'}input) {
                        id
                        name
                        role
                        profileCompleted
                    }
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply {
                    put("input", input)
                })
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    Result.success(response)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("BackendRepository", "Error Response: $errorResponse")
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    suspend fun updateFcmToken(token: String, fcmToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation UpdateFcmToken(${'$'}fcmToken: String!) {
                    updateFcmToken(fcmToken: ${'$'}fcmToken) {
                        success
                        message
                    }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("fcmToken", fcmToken)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val data = jsonResponse.optJSONObject("data")
                    val updateResult = data?.optJSONObject("updateFcmToken")
                    if (updateResult != null && updateResult.optBoolean("success")) {
                         Result.success(true)
                    } else {
                         Result.failure(Exception("Update failed"))
                    }
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("BackendRepository", "Error Response: $errorResponse")
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
