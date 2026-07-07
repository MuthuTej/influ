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
        ageMin: Int,
        ageMax: Int,
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

        // AudienceInput.ageMin/ageMax are required (Int!) server-side, so they must always
        // be present here — the caller is responsible for validating both parse before calling.
        val inputVariables = mapOf(
            "name" to name,
            "profileUrl" to (profileUrl ?: ""),
            "logoUrl" to logoUrl,
            "about" to about,
            "brandCategories" to categories,
            "preferredPlatforms" to preferredPlatforms,
            "targetAudience" to mapOf(
                "ageMin" to ageMin,
                "ageMax" to ageMax,
                "gender" to gender,
                "locations" to emptyList<String>()
            )
        )

        val variables = mapOf("input" to inputVariables)

        val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
        
        result.isSuccess && result.getOrNull()?.optJSONObject("data")?.optJSONObject("setupBrandProfile") != null
    }
}
