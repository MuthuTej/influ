package np.com.bimalkafle.firebaseauthdemoapp.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BrandRepository {

    suspend fun setupBrandProfile(
        token: String,
        name: String,
        brandCategory: String,
        subCategory: String,
        about: String,
        preferredPlatforms: List<String>,
        ageMin: Int?,
        ageMax: Int?,
        gender: String,
        profileUrl: String?,
        logoUrl: String
    ): Boolean = withContext(Dispatchers.IO) {

        val mutation = """
            mutation SetupBrandProfile(${'$'}input: BrandProfileInput!) {
              setupBrandProfile(input: ${'$'}input) {
                id
                name
                profileCompleted
              }
            }
        """.trimIndent()

        val platformsArray = JSONArray()
        preferredPlatforms.forEach { platformName ->
            val platformObj = JSONObject()
            val formatsArray = JSONArray()
            formatsArray.put("Reels")

            platformObj.put("platform", platformName)
            platformObj.put("formats", formatsArray)
            platformObj.put("minFollowers", 1000)
            platformObj.put("minEngagement", 2.5)
            platformsArray.put(platformObj)
        }

        val inputVariables = mutableMapOf<String, Any>(
            "name" to name,
            "profileUrl" to (profileUrl ?: ""),
            "logoUrl" to logoUrl,
            "about" to about,
            "brandCategories" to listOf(
                mapOf(
                    "category" to brandCategory,
                    "subCategories" to listOf(subCategory)
                )
            ),
            "preferredPlatforms" to parsePlatformsToMapList(platformsArray),
            "targetAudience" to mutableMapOf<String, Any>(
                "gender" to gender
            )
        )
        
        (inputVariables["targetAudience"] as MutableMap<String, Any>).apply {
            if (ageMin != null) put("ageMin", ageMin)
            if (ageMax != null) put("ageMax", ageMax)
        }

        val variables = mapOf("input" to inputVariables)

        val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
        
        result.isSuccess && result.getOrNull()?.optJSONObject("data")?.optJSONObject("setupBrandProfile") != null
    }

    private fun parsePlatformsToMapList(jsonArray: JSONArray): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val map = mutableMapOf<String, Any>()
            map["platform"] = obj.getString("platform")
            
            val formatsJson = obj.getJSONArray("formats")
            val formatsList = mutableListOf<String>()
            for (j in 0 until formatsJson.length()) {
                formatsList.add(formatsJson.getString(j))
            }
            map["formats"] = formatsList
            map["minFollowers"] = obj.getInt("minFollowers")
            map["minEngagement"] = obj.getDouble("minEngagement")
            list.add(map)
        }
        return list
    }
}
