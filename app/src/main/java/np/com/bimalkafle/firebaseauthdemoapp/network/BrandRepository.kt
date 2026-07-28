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
        logoUrl: String,
        location: String = ""
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
        val inputVariables = mutableMapOf<String, Any>(
            "name" to name,
            "profileUrl" to (profileUrl ?: ""),
            "location" to location,
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
        // Only send logoUrl when the user actually typed one here — the server treats any
        // provided value (including "") as an explicit overwrite, which would wipe out a
        // photo already uploaded during signup.
        if (logoUrl.isNotBlank()) inputVariables["logoUrl"] = logoUrl

        val variables = mapOf("input" to inputVariables)

        val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
        
        result.isSuccess && result.getOrNull()?.optJSONObject("data")?.optJSONObject("setupBrandProfile") != null
    }
}
