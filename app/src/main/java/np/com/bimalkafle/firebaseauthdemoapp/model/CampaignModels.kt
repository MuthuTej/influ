package np.com.bimalkafle.firebaseauthdemoapp.model

data class CampaignInput(
    val title: String,
    val description: String,
    val objective: String,
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
    val objective: String,
    val status: String,
    val createdAt: String
)

data class GraphQLError(
    val message: String
)
