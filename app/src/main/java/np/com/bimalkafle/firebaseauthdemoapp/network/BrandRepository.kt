package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object BrandRepository {

    suspend fun setupBrandProfile(
        token: String,
        name: String,
        brandCategory: String,
        subCategory: String,
        about: String,
        primaryObjective: String,
        preferredPlatforms: List<String>,
        ageMin: Int?,
        ageMax: Int?,
        gender: String,
        profileUrl: String?,
        logoUrl: String
    ): Boolean = withContext(Dispatchers.IO) {

        val url = URL("https://connect-backend-e22a.onrender.com/graphql")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.doOutput = true

        val mutation = """
            mutation SetupBrandProfile(${'$'}input: BrandProfileInput!) {
              setupBrandProfile(input: ${'$'}input) {
                id
                name
                profileCompleted
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("input", JSONObject().apply {
                put("name", name)
                put("profileUrl", profileUrl)
                put("logoUrl" , logoUrl)
                put("about", about)
                put("primaryObjective", primaryObjective)

                put("brandCategory", JSONObject().apply {
                    put("category", brandCategory)
                    put("subCategory", subCategory)
                })

                val platformsArray = org.json.JSONArray()

                preferredPlatforms.forEach { platformName ->
                    val platformObj = JSONObject()

                    val formatsArray = org.json.JSONArray()
                    formatsArray.put("Reels") // this is now a real JSON array

                    platformObj.put("platform", platformName)
                    platformObj.put("formats", formatsArray)
                    platformObj.put("minFollowers", 1000)
                    platformObj.put("minEngagement", 2.5)

                    platformsArray.put(platformObj)
                }

                put("preferredPlatforms", platformsArray)
                put("targetAudience", JSONObject().apply {
                    put("ageMin", ageMin)
                    put("ageMax", ageMax)
                    put("gender", gender)
                })
            })
        }

        val body = JSONObject().apply {
            put("query", mutation)
            put("variables", variables)
        }

        OutputStreamWriter(connection.outputStream).use {
            it.write(body.toString())
            it.flush()
        }

        val responseCode = connection.responseCode

        Log.d("BRAND_API", "HTTP Response Code: $responseCode")

        val responseStream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }

        val responseText = responseStream?.bufferedReader()?.use { it.readText() }
        Log.d("BRAND_API", "Raw Response: $responseText")

        return@withContext responseCode == 200
    }
}