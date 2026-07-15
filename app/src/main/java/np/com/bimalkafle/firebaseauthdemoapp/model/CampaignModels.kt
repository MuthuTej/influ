package np.com.bimalkafle.firebaseauthdemoapp.model

data class CampaignInput(
    val title: String,
    val description: String,
    val categories: List<BrandCategory>,
    val platforms: List<CampaignPlatformInput>,
    val budgetMin: Int,
    val budgetMax: Int,
    val startDate: String?,
    val endDate: String?,
    val targetAudience: CampaignAudienceInput,
    val hosting: HostingPricingInput? = null
)

data class HostingPricingInput(
    val price: Int
)

data class CampaignPlatformInput(
    val platform: String,
    val formats: List<String>
)

data class CampaignAudienceInput(
    val ageMin: Int,
    val ageMax: Int,
    val gender: String,
    val locations: List<String>
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
    val platforms: List<Platform>?,
    val brand: Brand?,
    val categories: List<BrandCategory>? = null,
    val collaborations: List<CampaignCollaborationSummary>? = null,
    val overallAnalytics: CampaignOverallAnalytics? = null,
    val hosting: HostingPricingResponse? = null
)

data class HostingPricingResponse(
    val price: Int?
)

/** One collaboration as seen from the campaign's combined-analytics view — just
 * enough to render a status breakdown, total spend, and a tappable list row;
 * full detail still lives behind collaboration_analytics/{id}. */
data class CampaignCollaborationSummary(
    val id: String,
    val status: String,
    val influencerName: String?,
    val influencerHandle: String?,
    val totalPrice: Int,
    val rating: Double? = null,
    val paymentStatus: String? = null,
    val totalAmount: Double? = null
) {
    /** Amount actually paid, 0 until payment is confirmed. `totalAmount` is only
     * persisted once the real Pay Now flow runs — older/manual collaborations can
     * reach paymentStatus "paid" without it, so fall back to the negotiated price
     * sum, same as HeroStats.bestKnownAmount(). */
    val amountPaid: Int
        get() = if (paymentStatus == "paid") (totalAmount?.toInt() ?: totalPrice) else 0
}

/** Sum of OverallAnalytics across every collaboration in the campaign — same
 * shape as a single collaboration's overallAnalytics, just aggregated. */
data class CampaignOverallAnalytics(
    val likes: Int?,
    val comments: Int?,
    val views: Int?,
    val shares: Int?,
    val retweets: Int?,
    val replies: Int?,
    val impressions: Int?,
    val clicks: Int?,
    val saves: Int?
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
