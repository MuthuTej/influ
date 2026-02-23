package np.com.bimalkafle.firebaseauthdemoapp.model

data class CampaignInput(
    val title: String,
    val description: String,
    val platforms: List<CampaignPlatformInput>,
    val budgetMin: Int,
    val budgetMax: Int,
    val startDate: String?,
    val endDate: String?,
    val targetAudience: CampaignAudienceInput
)

data class CampaignPlatformInput(
    val platform: String
)

data class CampaignAudienceInput(
    val ageMin: Int,
    val ageMax: Int,
    val gender: String
)

data class CreateCampaignResponse(
    val data: CreateCampaignData? = null,
    val errors: List<GraphQLError>? = null
)

data class CreateCampaignData(
    val createCampaign: CampaignDetail
)

data class CampaignDetail(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val createdAt: String,
    val budgetMin: Int?,
    val budgetMax: Int?,
    val startDate: String?,
    val endDate: String?,
    val targetAudience: CampaignAudienceResponse?,
    val brand: Brand?
)

data class CampaignAudienceResponse(
    val ageMin: Int?,
    val ageMax: Int?,
    val gender: String?,
    val locations: List<String>?
)

data class BrandResponse(
    val name: String?,
    val about: String?,
    val logoUrl: String?,
    val brandCategory: BrandCategory?,
    val preferredPlatforms: List<CampaignPlatformInput>?,
    val targetAudience: CampaignAudienceResponse?,
    val id: String? = null
)

data class GraphQLError(
    val message: String
)
