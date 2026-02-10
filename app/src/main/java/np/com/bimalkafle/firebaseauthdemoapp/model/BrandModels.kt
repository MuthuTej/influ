package np.com.bimalkafle.firebaseauthdemoapp.model

data class CollaborationResponse(
    val data: Data
)

data class Data(
    val getCollaborations: List<Collaboration>
)

data class Collaboration(
    val id: String,
    val campaignId: String,
    val brandId: String,
    val influencerId: String,
    val status: String,
    val message: String?,
    val pricing: List<Pricing>?,
    val initiatedBy: String,
    val createdAt: String,
    val updatedAt: String,
    val campaign: Campaign,
    val influencer: Influencer,
    val paymentStatus: String?,
    val razorpayOrderId: String?,
    val advancePaid: Boolean?,
    val finalPaid: Boolean?,
    val totalAmount: Int?,
    val brand: Brand? = null
)

data class Campaign(
    val id: String,
    val brandId: String?,
    val title: String,
    val description: String?,
    val objective: String?,
    val budgetMin: Int?,
    val budgetMax: Int?,
    val startDate: String?,
    val endDate: String?,
    val status: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class Pricing(
    val platform: String,
    val deliverable: String,
    val price: Int,
    val currency: String
)

data class Influencer(
    val name: String,
    val bio: String?,
    val logoUrl: String?,
    val updatedAt: String?
)

data class BrandCategory(
    val category: String,
    val subCategory: String
)

data class PreferredPlatform(
    val platform: String,
    val profileUrl: String?,
    val followers: Int?,
    val avgViews: Int?,
    val engagement: Float?,
    val formats: List<String>?,
    val connected: Boolean?,
    val minFollowers: Int?,
    val minEngagement: Float?
)

data class TargetAudience(
    val ageMin: Int?,
    val ageMax: Int?,
    val gender: String?,
    val locations: List<String>?
)

data class Reviewer(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val profileCompleted: Boolean?,
    val updatedAt: String?,
    val govtId: String?,
    val isVerified: Boolean?,
    val fcmToken: String?,
    val averageRating: Double?
)

data class Review(
    val id: String,
    val collaborationId: String?,
    val reviewerId: String,
    val revieweeId: String,
    val reviewerRole: String,
    val rating: Double,
    val comment: String?,
    val createdAt: String,
    val reviewer: Reviewer?,
    val reviewee: Reviewer?
)

data class Brand(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val profileCompleted: Boolean?,
    val updatedAt: String?,
    val brandCategory: BrandCategory?,
    val about: String?,
    val primaryObjective: String?,
    val profileUrl: String?,
    val logoUrl: String?,
    val govtId: String? = null,
    val isVerified: Boolean? = null,
    val reviews: List<Review>? = null,
    val averageRating: Double? = null,
    val fcmToken: String? = null,
    val preferredPlatforms: List<PreferredPlatform>? = null,
    val targetAudience: TargetAudience? = null
)
