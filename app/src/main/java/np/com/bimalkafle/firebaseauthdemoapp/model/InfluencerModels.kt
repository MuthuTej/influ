package np.com.bimalkafle.firebaseauthdemoapp.model

data class InfluencerProfile(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val profileCompleted: Boolean?,
    val updatedAt: String?,
    val bio: String?,
    val location: String?,
    val categories: List<Category>?,
    val platforms: List<Platform>?,
    val strengths: List<String>?,
    val pricing: List<PricingInfo>?,
    val availability: String?,
    val logoUrl: String?
)

data class Category(
    val category: String,
    val subCategory: String
)

data class Platform(
    val platform: String,
    val profileUrl: String,
    val formats: List<String>?,
    val connected: Boolean?
)

data class PricingInfo(
    val platform: String,
    val deliverable: String,
    val price: Int,
    val currency: String
)

data class InfluencerProfileInput(
    val name: String,
    val bio: String,
    val location: String,
    val categories: List<CategoryInput>,
    val platforms: List<PlatformInput>,
    val pricing: List<PricingInput>,
    val availability: String,
    val logoUrl: String? = null,
    val audienceInsights: AudienceInsightsInput? = null,
    val strengths: List<String>? = null
)

data class CategoryInput(
    val category: String,
    val subCategory: String
)

data class PlatformInput(
    val platform: String,
    val profileUrl: String,
    val followers: Int,
    val avgViews: Int,
    val engagement: Float,
    val formats: List<String> = emptyList()
)

data class PricingInput(
    val platform: String,
    val deliverable: String,
    val price: Int,
    val currency: String
)

data class AudienceInsightsInput(
    val topLocations: List<LocationInsightInput>,
    val genderSplit: GenderSplitInput,
    val ageGroups: List<AgeGroupInsightInput>
)

data class LocationInsightInput(
    val city: String,
    val country: String,
    val percentage: Float
)

data class GenderSplitInput(
    val male: Float,
    val female: Float
)

data class AgeGroupInsightInput(
    val range: String,
    val percentage: Float
)
