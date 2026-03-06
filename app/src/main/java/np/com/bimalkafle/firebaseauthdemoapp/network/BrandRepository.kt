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
        categories: List<Map<String, Any>>,
        about: String,
        preferredPlatforms: List<Map<String, Any>>,
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

        val inputVariables = mutableMapOf<String, Any>(
            "name" to name,
            "profileUrl" to (profileUrl ?: ""),
            "logoUrl" to logoUrl,
            "about" to about,
            "brandCategories" to categories,
            "preferredPlatforms" to preferredPlatforms,
            "targetAudience" to mutableMapOf<String, Any>(
                "gender" to gender,
                "locations" to emptyList<String>()
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
}
