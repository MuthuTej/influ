package np.com.bimalkafle.firebaseauthdemoapp.model

data class CollaborationResponse(
    val data: Data
)

data class Data(
    val getCollaborations: List<Collaboration>
)

data class YouTubeVideoSummary(
    val views: Int?,
    val likes: Int?,
    val comments: Int?,
    val shares: Int?,
    val watchTimeMinutes: Double?,
    val subscribersGained: Int?,
    val averageViewDurationSeconds: Int?,
    val engagementRate: String?
)

data class YouTubeVideoData(
    val videoId: String,
    val title: String,
    val viewCount: String?,
    val likeCount: String?,
    val thumbnail: String?,
    val analytics: YouTubeVideoSummary?,
    val videoUrl: String?,
    val fetchedAt: String?
)

data class InstagramPostData(
    val postId: String?,
    val caption: String?,
    val likeCount: Int?,
    val commentCount: Int?,
    val viewCount: Int?,
    val mediaUrl: String?,
    val timestamp: String?,
    val fetchedAt: String?
)

data class CollaborationAnalytics(
    val platform: String?,
    val duration: Int?,
    val cost: Float?,
    val impressions: Int?,
    val clicks: Int?,
    val likes: Int?,
    val comments: Int?,
    val shares: Int?,
    val saves: Int?,
    val views: Int?,
    val retweets: Int? = null,
    val replies: Int? = null
)

data class OverallAnalytics(
    val impressions: Int?,
    val clicks: Int?,
    val likes: Int?,
    val comments: Int?,
    val shares: Int?,
    val saves: Int?,
    val views: Int?,
    val retweets: Int? = null,
    val replies: Int? = null
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
    val totalAmount: Double?,
    val brand: Brand? = null,
    val overallAnalytics: OverallAnalytics? = null,
    val platformAnalytics: List<CollaborationAnalytics>? = null,
    val yt: List<YouTubeVideoData>? = null,
    val ig: List<InstagramPostData>? = null,
    val youtubeVideoId: String? = null
)

data class Campaign(
    val id: String,
    val brandId: String?,
    val title: String,
    val description: String?,
    val budgetMin: Int?,
    val budgetMax: Int?,
    val startDate: String?,
    val endDate: String?,
    val status: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val platforms: List<Platform>? = null
)

data class Pricing(
    val platform: String,
    val deliverable: String,
    val count: Int? = null,
    val price: Int,
    val currency: String,
    val status: String? = null,
    val totalAmount: Double? = null,
    val updatedAt: String? = null,
    val youtubeVideoId: String? = null
)

data class Influencer(
    val name: String,
    val bio: String?,
    val logoUrl: String?,
    val updatedAt: String?
)

data class BrandCategory(
    val category: String,
    val subCategories: List<String>
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
    val brandCategories: List<BrandCategory>?,
    val about: String?,
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
