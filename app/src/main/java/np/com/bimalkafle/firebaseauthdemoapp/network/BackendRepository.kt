package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import np.com.bimalkafle.firebaseauthdemoapp.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class InstagramProfileSummary(val username: String, val followers: Int?, val source: String?)

object BackendRepository {
    private val BACKEND_URL = BuildConfig.BACKEND_BASE_URL

    suspend fun fetchInfluencerDashboard(token: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val query = """
            query GetInfluencerDashboard {
              getInfluencerDashboard {
                profile {
                  id email name role profileCompleted updatedAt bio location gender motherTongue languagesKnown
                  categories { category subCategories }
                  platforms { platform profileUrl followers avgViews engagement formats connected }
                  strengths pricing { platform deliverable price currency }
                  availability logoUrl isVerified averageRating
                  youtubeInsights { channelId title description subscribers totalViews totalVideos demographics { ageGroup gender percentage } lastSynced }
                  instagramMetrics { avgComments avgLikes avgViews postingFrequencyDays totalPostsAnalyzed updatedAt engagementRate }
                  instagramProfiles { id profileUrl username followers isDefault connectedAt metrics { avgLikes avgComments avgViews postingFrequencyDays totalPostsAnalyzed updatedAt engagementRate } aiInsights { primaryNiche brandSuitability aiSummary } }
                  aiInsights { primaryNiche secondaryNiche tone brandSuitability aiSummary }
                }
                collaborations {
                  id status message pricing { platform deliverable price currency } initiatedBy createdAt updatedAt paymentStatus totalAmount
                  brand { id name logoUrl }
                  campaign { id title status }
                  selectedInstagramProfileId
                }
                recommendedCampaigns {
                  id title description brandId budgetMin budgetMax createdAt aiScore
                  categories { category subCategories }
                  platforms { platform formats }
                }
                unreadNotificationCount
              }
            }
        """.trimIndent()
        GraphQLClient.query(query = query, token = token)
    }

    suspend fun fetchBrandDashboard(token: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val query = """
            query GetBrandDashboard {
              getBrandDashboard {
                profile {
                  id email name role profileCompleted updatedAt about profileUrl logoUrl averageRating isVerified
                  brandCategories { category subCategories }
                }
                collaborations {
                  id status message pricing { platform deliverable price currency } initiatedBy createdAt updatedAt paymentStatus totalAmount
                  campaign { id title status }
                  influencer { name logoUrl }
                }
                topPicks {
                  id name bio location logoUrl isVerified averageRating tier totalFollowers engagementRate
                  categories { category subCategories }
                  platforms { platform followers engagement }
                  aiInsights { primaryNiche brandSuitability aiSummary }
                }
                activity {
                  id title body isRead createdAt
                }
              }
            }
        """.trimIndent()
        GraphQLClient.query(query = query, token = token)
    }

    suspend fun fetchInfluencerHomeDashboard(token: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val query = """
            query GetInfluencerHomeDashboard {
              me {
                id name email role profileCompleted updatedAt bio location gender motherTongue languagesKnown
                categories { category subCategories }
                platforms { platform profileUrl followers avgViews engagement formats connected }
                strengths pricing { platform deliverable price currency }
                availability logoUrl isVerified averageRating
                youtubeInsights { channelId title description subscribers totalViews totalVideos demographics { ageGroup gender percentage } lastSynced }
                instagramMetrics { avgComments avgLikes avgViews postingFrequencyDays totalPostsAnalyzed updatedAt }
                instagramProfiles { id profileUrl username followers isDefault connectedAt metrics { avgLikes avgComments avgViews postingFrequencyDays totalPostsAnalyzed updatedAt } }
              }
              getCollaborations {
                id status message pricing { platform deliverable price currency } initiatedBy createdAt updatedAt paymentStatus totalAmount
                brand { id name logoUrl }
                campaign { id title status }
                selectedInstagramProfileId
              }
              getCampaigns(limit: 5) {
                id title description budgetMin budgetMax startDate endDate status createdAt
                brand { name logoUrl averageRating isVerified }
                categories { category }
                platforms { platform }
              }
              getUnreadNotificationCount
            }
        """.trimIndent()
        GraphQLClient.query(query = query, token = token)
    }

    suspend fun signUp(name: String, role: String, token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

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
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
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

    suspend fun uploadProfilePhoto(imageBase64: String, mimeType: String, token: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val mutation = """
                    mutation UploadProfilePhoto(${'$'}imageBase64: String!, ${'$'}mimeType: String!) {
                        uploadProfilePhoto(imageBase64: ${'$'}imageBase64, mimeType: ${'$'}mimeType)
                    }
                """.trimIndent()
                val variables = mapOf("imageBase64" to imageBase64, "mimeType" to mimeType)
                val result = GraphQLClient.query(mutation, variables, token)
                result.map { json -> json.getJSONObject("data").getString("uploadProfilePhoto") }
            } catch (e: Exception) {
                Log.e("BackendRepository", "uploadProfilePhoto: ${e.message}", e)
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
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val data = jsonResponse.optJSONObject("data")
                    if (data == null || data.isNull("me")) {
                        return@withContext Result.failure(Exception("User not found"))
                    }
                    val me = data.getJSONObject("me")
                    val role = me.getString("role")
                    val isProfileCompleted = me.optBoolean("profileCompleted", false)
                    Result.success("$role|$isProfileCompleted")
                }
            } else {
                Result.failure(Exception("Server returned code $responseCode"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "getUserRole error", e)
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
                        gender
                        motherTongue
                        languagesKnown
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
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
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
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
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

    suspend fun getNotifications(token: String, limit: Int = 20): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                query GetNotifications(${'$'}limit: Int) {
                    getNotifications(limit: ${'$'}limit) {
                        id
                        userId
                        title
                        body
                        data
                        isRead
                        createdAt
                    }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("limit", limit)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
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
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadNotificationCount(token: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                query {
                    getUnreadNotificationCount
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("query", query)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val count = jsonResponse.getJSONObject("data").getInt("getUnreadNotificationCount")
                    Result.success(count)
                }
            } else {
                Result.failure(Exception("Server error ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationAsRead(token: String, id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation MarkNotificationAsRead(${'$'}id: ID!) {
                    markNotificationAsRead(id: ${'$'}id)
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("id", id)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    Result.success(jsonResponse.getJSONObject("data").getBoolean("markNotificationAsRead"))
                }
            } else {
                Result.failure(Exception("Server error ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsAsRead(token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation {
                    markAllNotificationsAsRead
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("query", query)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    Result.success(jsonResponse.getJSONObject("data").getBoolean("markAllNotificationsAsRead"))
                }
            } else {
                Result.failure(Exception("Server error ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            val query = """
                mutation RequestPasswordReset(${'$'}email: String!) {
                    requestPasswordReset(email: ${'$'}email) {
                        success
                        message
                    }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("email", email)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val data = jsonResponse.getJSONObject("data").getJSONObject("requestPasswordReset")
                    if (data.getBoolean("success")) {
                        Result.success(data.getString("message"))
                    } else {
                        Result.failure(Exception(data.getString("message")))
                    }
                }
            } else {
                Result.failure(Exception("Server returned code $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // query { instagramAuthUrl } -> String! — starts real Instagram OAuth
    // ("Instagram API with Instagram Login"). The backend records a
    // single-use state doc for this influencer and returns the full Meta
    // authorize URL; launch it in a Custom Tab and wait for the
    // np.com.bimalkafle.firebaseauthdemoapp://instagram-callback redirect
    // (see MainActivity.onNewIntent / InstagramAuthResult).
    suspend fun instagramAuthUrl(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                query InstagramAuthUrl {
                    instagramAuthUrl
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("query", query)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val authUrl = jsonResponse.optJSONObject("data")?.optString("instagramAuthUrl")
                    if (authUrl.isNullOrBlank()) {
                        Result.failure(Exception("No Instagram authorization URL returned"))
                    } else {
                        Result.success(authUrl)
                    }
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("BackendRepository", "instagramAuthUrl Error Response: $errorResponse")
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "instagramAuthUrl Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Lightweight refetch used right after an Instagram OAuth redirect
    // completes, so InfluencerRegistrationScreen can show the real connected
    // username/source without pulling in the full InfluencerViewModel (this
    // screen talks to BackendRepository directly, same as connectYouTube above).
    suspend fun fetchInstagramProfiles(token: String): Result<List<InstagramProfileSummary>> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(BACKEND_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.doOutput = true

                val query = """
                    query FetchInstagramProfiles {
                        me {
                            ... on Influencer {
                                instagramProfiles { username followers source }
                            }
                        }
                    }
                """.trimIndent()

                val requestBody = JSONObject().apply { put("query", query) }.toString()
                connection.outputStream.use { it.write(requestBody.toByteArray()) }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    SessionManager.notifySessionExpired()
                    return@withContext Result.failure(UnauthorizedException())
                }
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.has("errors")) {
                        val errors = jsonResponse.getJSONArray("errors")
                        Result.failure(Exception(errors.getJSONObject(0).getString("message")))
                    } else {
                        val profilesArray = jsonResponse.optJSONObject("data")
                            ?.optJSONObject("me")
                            ?.optJSONArray("instagramProfiles")
                        val profiles = mutableListOf<InstagramProfileSummary>()
                        if (profilesArray != null) {
                            for (i in 0 until profilesArray.length()) {
                                val p = profilesArray.optJSONObject(i) ?: continue
                                profiles.add(
                                    InstagramProfileSummary(
                                        username = p.optString("username"),
                                        followers = if (p.has("followers") && !p.isNull("followers")) p.optInt("followers") else null,
                                        source = if (p.has("source") && !p.isNull("source")) p.optString("source") else null
                                    )
                                )
                            }
                        }
                        Result.success(profiles)
                    }
                } else {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    Log.e("BackendRepository", "fetchInstagramProfiles Error Response: $errorResponse")
                    Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
                }
            } catch (e: Exception) {
                Log.e("BackendRepository", "fetchInstagramProfiles Exception: ${e.message}", e)
                Result.failure(e)
            }
        }

    suspend fun connectYouTube(authCode: String, token: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("BackendRepository", "connectYouTube - AuthCode: $authCode")
                val url = URL(BACKEND_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.doOutput = true

                val query = """
                mutation ConnectYouTube(${'$'}code: String!) {
                    connectYouTube(code: ${'$'}code)
                }
            """.trimIndent()

                val variables = JSONObject().apply {
                    put("code", authCode)
                }

                val requestBody = JSONObject().apply {
                    put("query", query)
                    put("variables", variables)
                }.toString()

                Log.d("BackendRepository", "connectYouTube - Request Body: $requestBody")

                connection.outputStream.use {
                    it.write(requestBody.toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    SessionManager.notifySessionExpired()
                    return@withContext Result.failure(UnauthorizedException())
                }
                Log.d("BackendRepository", "connectYouTube - Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("BackendRepository", "connectYouTube - Response: $response")
                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.has("errors")) {
                        val errorMsg = jsonResponse
                            .getJSONArray("errors")
                            .getJSONObject(0)
                            .getString("message")
                        Log.e("BackendRepository", "connectYouTube - GraphQL Error: $errorMsg")
                        Result.failure(Exception(errorMsg))
                    } else {
                        Log.d("BackendRepository", "connectYouTube - SUCCESS")
                        Result.success(true)
                    }
                } else {
                    val errorResponse =
                        connection.errorStream?.bufferedReader()?.use { it.readText() }
                            ?: "Unknown error"
                    Log.e("BackendRepository", "connectYouTube - Server Error: $errorResponse")
                    Result.failure(Exception("Server error $responseCode: $errorResponse"))
                }

            } catch (e: Exception) {
                Log.e("BackendRepository", "connectYouTube - Exception: ${e.message}", e)
                Result.failure(e)
            }
        }

    suspend fun getVideoByUrl(
        videoUrl: String,
        userId: String,
        collaborationId: String,
        token: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                query GetVideoByUrl(${'$'}videoUrl: String!, ${'$'}userId: ID!, ${'$'}collaborationId: ID!) {
                    getVideoByUrl(
                        videoUrl: ${'$'}videoUrl,
                        userId: ${'$'}userId,
                        collaborationId: ${'$'}collaborationId
                    ) {
                        title
                        viewCount
                    }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("videoUrl", videoUrl)
                put("userId", userId)
                put("collaborationId", collaborationId)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val data = jsonResponse.getJSONObject("data").getJSONObject("getVideoByUrl")
                    Result.success(data)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("BackendRepository", "getVideoByUrl Error Response: $errorResponse")
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "getVideoByUrl Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun refreshCollaborationAnalytics(
        collaborationId: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val mutation = """
                mutation RefreshCollaborationAnalytics(${'$'}collaborationId: ID!) {
                    refreshCollaborationAnalytics(collaborationId: ${'$'}collaborationId)
                }
            """.trimIndent()
            val result = GraphQLClient.query(
                query = mutation,
                variables = mapOf("collaborationId" to collaborationId),
                token = token
            )
            result.map { json ->
                json.optJSONObject("data")?.optBoolean("refreshCollaborationAnalytics", false) ?: false
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scrapeInstagramProfile(
        profileUrl: String,
        influencerId: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation ScrapeInstagramProfile(${'$'}profileUrl: String!, ${'$'}influencerId: String!) {
                    scrapeInstagramProfile(profileUrl: ${'$'}profileUrl, influencerId: ${'$'}influencerId)
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("profileUrl", profileUrl)
                put("influencerId", influencerId)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val data = jsonResponse.optJSONObject("data")
                    val result = data?.optBoolean("scrapeInstagramProfile", false) ?: false
                    Result.success(result)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e("BackendRepository", "scrapeInstagramProfile Error Response: $errorResponse")
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "scrapeInstagramProfile Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun scrapeInstagramPost(
        postUrl: String,
        collaborationId: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation ScrapeInstagramPost(${'$'}postUrl: String!, ${'$'}collaborationId: String!) {
                    scrapeInstagramPost(postUrl: ${'$'}postUrl, collaborationId: ${'$'}collaborationId)
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("postUrl", postUrl)
                put("collaborationId", collaborationId)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val data = jsonResponse.optJSONObject("data")
                    val result = data?.optBoolean("scrapeInstagramPost", false) ?: false
                    Result.success(result)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "scrapeInstagramPost Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private val INSTAGRAM_PROFILE_FIELDS = """
        id
        profileUrl
        username
        followers
        isDefault
        connectedAt
        metrics {
          avgLikes
          avgComments
          avgViews
          postingFrequencyDays
          totalPostsAnalyzed
          updatedAt
        }
    """.trimIndent()

    suspend fun addInstagramProfile(profileUrl: String, token: String): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val mutation = """
                    mutation AddInstagramProfile(${'$'}profileUrl: String!) {
                        addInstagramProfile(profileUrl: ${'$'}profileUrl) {
                            $INSTAGRAM_PROFILE_FIELDS
                        }
                    }
                """.trimIndent()
                val variables = JSONObject().apply { put("profileUrl", profileUrl) }
                val requestBody = JSONObject().apply {
                    put("query", mutation)
                    put("variables", variables)
                }.toString()
                executeGraphQL(requestBody, token) { json ->
                    val errs = json.optJSONArray("errors")
                    if (errs != null && errs.length() > 0) {
                        Result.failure(Exception(errs.getJSONObject(0).getString("message")))
                    } else {
                        val profile = json.optJSONObject("data")?.optJSONObject("addInstagramProfile")
                            ?: return@executeGraphQL Result.failure(Exception("No profile returned"))
                        Result.success(profile)
                    }
                }
            } catch (e: Exception) {
                Log.e("BackendRepository", "addInstagramProfile: ${e.message}", e)
                Result.failure(e)
            }
        }

    suspend fun removeInstagramProfile(profileId: String, token: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val mutation = """
                    mutation RemoveInstagramProfile(${'$'}profileId: ID!) {
                        removeInstagramProfile(profileId: ${'$'}profileId)
                    }
                """.trimIndent()
                val variables = JSONObject().apply { put("profileId", profileId) }
                val requestBody = JSONObject().apply {
                    put("query", mutation)
                    put("variables", variables)
                }.toString()
                executeGraphQL(requestBody, token) { json ->
                    val errs = json.optJSONArray("errors")
                    if (errs != null && errs.length() > 0) {
                        Result.failure(Exception(errs.getJSONObject(0).getString("message")))
                    } else {
                        Result.success(json.optJSONObject("data")?.optBoolean("removeInstagramProfile", false) ?: false)
                    }
                }
            } catch (e: Exception) {
                Log.e("BackendRepository", "removeInstagramProfile: ${e.message}", e)
                Result.failure(e)
            }
        }

    suspend fun setDefaultInstagramProfile(profileId: String, token: String): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val mutation = """
                    mutation SetDefaultInstagramProfile(${'$'}profileId: ID!) {
                        setDefaultInstagramProfile(profileId: ${'$'}profileId) {
                            $INSTAGRAM_PROFILE_FIELDS
                        }
                    }
                """.trimIndent()
                val variables = JSONObject().apply { put("profileId", profileId) }
                val requestBody = JSONObject().apply {
                    put("query", mutation)
                    put("variables", variables)
                }.toString()
                executeGraphQL(requestBody, token) { json ->
                    val errs = json.optJSONArray("errors")
                    if (errs != null && errs.length() > 0) {
                        Result.failure(Exception(errs.getJSONObject(0).getString("message")))
                    } else {
                        val profile = json.optJSONObject("data")?.optJSONObject("setDefaultInstagramProfile")
                            ?: return@executeGraphQL Result.failure(Exception("No profile returned"))
                        Result.success(profile)
                    }
                }
            } catch (e: Exception) {
                Log.e("BackendRepository", "setDefaultInstagramProfile: ${e.message}", e)
                Result.failure(e)
            }
        }

    suspend fun refreshInstagramProfileMetrics(profileId: String, token: String): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            try {
                val mutation = """
                    mutation RefreshInstagramProfileMetrics(${'$'}profileId: ID!) {
                        refreshInstagramProfileMetrics(profileId: ${'$'}profileId) {
                            $INSTAGRAM_PROFILE_FIELDS
                        }
                    }
                """.trimIndent()
                val variables = JSONObject().apply { put("profileId", profileId) }
                val requestBody = JSONObject().apply {
                    put("query", mutation)
                    put("variables", variables)
                }.toString()
                executeGraphQL(requestBody, token) { json ->
                    val errs = json.optJSONArray("errors")
                    if (errs != null && errs.length() > 0) {
                        Result.failure(Exception(errs.getJSONObject(0).getString("message")))
                    } else {
                        val profile = json.optJSONObject("data")?.optJSONObject("refreshInstagramProfileMetrics")
                            ?: return@executeGraphQL Result.failure(Exception("No profile returned"))
                        Result.success(profile)
                    }
                }
            } catch (e: Exception) {
                Log.e("BackendRepository", "refreshInstagramProfileMetrics: ${e.message}", e)
                Result.failure(e)
            }
        }

    suspend fun getDistinctInfluencerCategories(token: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    query {
                        distinctInfluencerCategories
                    }
                """.trimIndent()
                val requestBody = JSONObject().apply { put("query", query) }.toString()
                executeGraphQL(requestBody, token) { json ->
                    val errs = json.optJSONArray("errors")
                    if (errs != null && errs.length() > 0) {
                        Result.failure(Exception(errs.getJSONObject(0).getString("message")))
                    } else {
                        val arr = json.getJSONObject("data").getJSONArray("distinctInfluencerCategories")
                        Result.success((0 until arr.length()).map { arr.getString(it) })
                    }
                }
            } catch (e: Exception) {
                Log.e("BackendRepository", "getDistinctInfluencerCategories: ${e.message}", e)
                Result.failure(e)
            }
        }

    private fun <T> executeGraphQL(requestBody: String, token: String, parse: (JSONObject) -> Result<T>): Result<T> {
        val url = URL(BACKEND_URL)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.outputStream.use { it.write(requestBody.toByteArray()) }
            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                Result.failure(UnauthorizedException())
            } else if (code == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parse(JSONObject(response))
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown"
                Result.failure(Exception("HTTP $code: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    suspend fun createCollaborationPaymentOrder(
        collaborationId: String,
        paymentType: String,
        token: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation CreateCollaborationPaymentOrder(${'$'}collaborationId: ID!, ${'$'}paymentType: PaymentType!) {
                    createCollaborationPaymentOrder(collaborationId: ${'$'}collaborationId, paymentType: ${'$'}paymentType) {
                        success
                        collaborationId
                        razorpayOrderId
                        totalAmount
                    }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("collaborationId", collaborationId)
                put("paymentType", paymentType)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val data = jsonResponse.getJSONObject("data").getJSONObject("createCollaborationPaymentOrder")
                    Result.success(data)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPayment(
        collaborationId: String,
        razorpayPaymentId: String,
        razorpaySignature: String,
        paymentType: String,
        token: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation VerifyPayment(
                    ${'$'}collaborationId: ID!, 
                    ${'$'}razorpayPaymentId: String!, 
                    ${'$'}razorpaySignature: String!, 
                    ${'$'}paymentType: PaymentType!
                ) {
                    verifyPayment(
                        collaborationId: ${'$'}collaborationId, 
                        razorpayPaymentId: ${'$'}razorpayPaymentId, 
                        razorpaySignature: ${'$'}razorpaySignature, 
                        paymentType: ${'$'}paymentType
                    ) {
                        id
                        status
                        paymentStatus
                    }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("collaborationId", collaborationId)
                put("razorpayPaymentId", razorpayPaymentId)
                put("razorpaySignature", razorpaySignature)
                put("paymentType", paymentType)
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                if (jsonResponse.has("errors")) {
                    val errors = jsonResponse.getJSONArray("errors")
                    val message = errors.getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    Result.success(true)
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Result.failure(Exception("Server returned code $responseCode: $errorResponse"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatList(token: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                query {
                    getChatList {
                        collaborationId
                        otherUserId
                        otherUserName
                        otherUserImageUrl
                        lastMessage
                        lastMessageTime
                        unreadCount
                        status
                    }
                }
            """.trimIndent()

            val requestBody = JSONObject().apply { put("query", query) }.toString()
            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("errors")) {
                    val message = jsonResponse.getJSONArray("errors").getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val arr = jsonResponse.getJSONObject("data").getJSONArray("getChatList")
                    val list = (0 until arr.length()).map { arr.getJSONObject(it) }
                    Result.success(list)
                }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Result.failure(Exception("Server returned $responseCode: $err"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "getChatList: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Used to resolve a notification's collaborationId into the chat partner's id/name
    // (see notification/NotificationRouter.kt) — getCollaborationById already authorizes
    // either participant (brand or influencer) on the collaboration.
    suspend fun getCollaborationById(collaborationId: String, token: String): Result<JSONObject> {
        val query = """
            query GetCollaborationById(${'$'}id: ID!) {
                getCollaborationById(id: ${'$'}id) {
                    id
                    brandId
                    influencerId
                    brand { name }
                    influencer { name }
                }
            }
        """.trimIndent()
        val result = GraphQLClient.query(query, mapOf("id" to collaborationId), token)
        return result.mapCatching { it.getJSONObject("data").getJSONObject("getCollaborationById") }
    }

    suspend fun sendChatMessage(
        receiverId: String,
        text: String,
        type: String = "TEXT",
        metadata: Map<String, Any> = emptyMap(),
        collaborationId: String? = null,
        replyToId: String? = null,
        token: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                mutation SendChatMessage(${'$'}input: SendMessageInput!) {
                    sendChatMessage(input: ${'$'}input) {
                        id
                        text
                        senderId
                        receiverId
                        timestamp
                        timeFormatted
                        type
                        collaborationId
                        replyToId
                        isRead
                        metadata
                    }
                }
            """.trimIndent()

            val inputObj = JSONObject().apply {
                put("receiverId", receiverId)
                put("text", text)
                put("type", type)
                put("collaborationId", collaborationId ?: JSONObject.NULL)
                put("replyToId", replyToId ?: JSONObject.NULL)
                if (metadata.isNotEmpty()) put("metadata", JSONObject(metadata as Map<*, *>))
            }

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply { put("input", inputObj) })
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("errors")) {
                    val message = jsonResponse.getJSONArray("errors").getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    Result.success(jsonResponse.getJSONObject("data").getJSONObject("sendChatMessage"))
                }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Result.failure(Exception("Server returned $responseCode: $err"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "sendChatMessage: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getChatMessages(
        otherUserId: String,
        collaborationId: String?,
        token: String
    ): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = URL(BACKEND_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true

            val query = """
                query GetChatMessages(${'$'}otherUserId: String!, ${'$'}collaborationId: String) {
                    getChatMessages(otherUserId: ${'$'}otherUserId, collaborationId: ${'$'}collaborationId) {
                        id
                        text
                        senderId
                        receiverId
                        timestamp
                        timeFormatted
                        type
                        collaborationId
                        replyToId
                        isRead
                        metadata
                    }
                }
            """.trimIndent()

            val requestBody = JSONObject().apply {
                put("query", query)
                put("variables", JSONObject().apply {
                    put("otherUserId", otherUserId)
                    put("collaborationId", collaborationId ?: JSONObject.NULL)
                })
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                SessionManager.notifySessionExpired()
                return@withContext Result.failure(UnauthorizedException())
            }
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("errors")) {
                    val message = jsonResponse.getJSONArray("errors").getJSONObject(0).getString("message")
                    Result.failure(Exception(message))
                } else {
                    val arr = jsonResponse.getJSONObject("data").getJSONArray("getChatMessages")
                    val list = (0 until arr.length()).map { arr.getJSONObject(it) }
                    Result.success(list)
                }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Result.failure(Exception("Server returned $responseCode: $err"))
            }
        } catch (e: Exception) {
            Log.e("BackendRepository", "getChatMessages: ${e.message}", e)
            Result.failure(e)
        }
    }
}
