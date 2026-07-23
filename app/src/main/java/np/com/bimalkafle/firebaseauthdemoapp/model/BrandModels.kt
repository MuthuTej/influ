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
    val authorName: String? = null,
    val channelUrl: String? = null,
    val description: String? = null,
    val duration: String? = null,
    val publishedAt: String? = null,
    val viewCount: String?,
    val likeCount: String?,
    val commentCount: String? = null,
    val thumbnail: String?,
    val analytics: YouTubeVideoSummary?,
    val videoUrl: String?,
    val fetchedAt: String?
)

data class PerformanceMilestone(
    val label: String,
    val hoursAfterPost: Int,
    val views: Int?,
    val likes: Int?,
    val comments: Int?,
    val capturedAt: String?
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

// Brand-only performance targets for a collaboration. The backend never
// returns these fields for an influencer-authenticated request (role-gated
// field resolvers on Collaboration.performanceTargets/.performanceTracking),
// but the UI must also avoid requesting/rendering them for that role.
data class PerformanceTargets(
    val targetViews: Double?,
    val targetReach: Double?,
    val targetEngagementRate: Double?,
    val targetLikes: Double?,
    val targetComments: Double?,
    val targetShares: Double?,
    val targetSaves: Double?,
    val setAt: String? = null,
    val setBy: String? = null
)

// tracked == false means there's no live data source for this metric yet
// (e.g. Instagram shares/saves, or reach in general) — render "Not tracked"
// rather than a misleading 0%.
data class PerformanceAchievement(
    val metric: String,
    val target: Double,
    val actual: Double?,
    val achievedPercent: Double?,
    val status: String,
    val tracked: Boolean
)

data class ActualMetrics(
    val views: Double?,
    val reach: Double?,
    val engagementRate: Double?,
    val likes: Double?,
    val comments: Double?,
    val shares: Double?,
    val saves: Double?
)

data class PerformanceSnapshot(
    val capturedAt: String,
    val actual: ActualMetrics?,
    val targets: PerformanceTargets?,
    val performanceScore: Double?,
    val campaignOutcome: String?,
    val isFinal: Boolean?
)

data class PerformanceTracking(
    val achievements: List<PerformanceAchievement>,
    val overallAchievedPercent: Double?,
    val performanceScore: Double?,
    val campaignOutcome: String?,
    val history: List<PerformanceSnapshot>
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
    val paymentStatus: String? = null,
    val razorpayOrderId: String? = null,
    val advancePaid: Boolean? = null,
    val finalPaid: Boolean? = null,
    val totalAmount: Double? = null,
    val brand: Brand? = null,
    val overallAnalytics: OverallAnalytics? = null,
    val platformAnalytics: List<CollaborationAnalytics>? = null,
    val yt: List<YouTubeVideoData>? = null,
    val ig: List<InstagramPostData>? = null,
    val performanceMilestones: List<PerformanceMilestone>? = null,
    val youtubeVideoId: String? = null,
    // Latest synced view count / growth across submitted content (see the
    // daily content-performance cron job and Collaboration.totalViewsDelivered
    // / viewsGrowthSincePosting resolvers on the backend).
    val totalViewsDelivered: Int? = null,
    val viewsGrowthSincePosting: Int? = null,
    val selectedInstagramProfileId: String? = null,
    // Brand-only — always null when this collaboration was fetched by the
    // InfluencerViewModel (the backend won't return them for that role).
    val performanceTargets: PerformanceTargets? = null,
    val performanceTracking: PerformanceTracking? = null,
    // Whether the current user has already reviewed this collaboration —
    // used to decide whether to prompt the post-collaboration rating dialog.
    val hasReviewed: Boolean? = null,
    // Set while a BRAND or INFLUENCER participant has asked an admin to end
    // this collaboration (requestCollaborationCancellation mutation). Null
    // when no request has ever been made.
    val cancellationRequest: CancellationRequest? = null
)

// Mirrors the backend's CancellationRequest type (collaboration module) —
// an admin-reviewed request to terminate a collaboration that has already
// passed ACCEPTED. Never set/cleared directly by either party; only the
// admin's reviewCollaborationCancellation mutation (admin webapp) resolves it.
data class CancellationRequest(
    val requestedBy: String,
    val requestedByRole: String,
    val reason: String,
    val status: String,
    val requestedAt: String,
    val resolvedAt: String? = null,
    val resolvedBy: String? = null,
    val adminNote: String? = null
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
    val platforms: List<Platform>? = null,
    val collaborations: List<CampaignCollaborationSummary>? = null
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
    val location: String? = null,
    val govtId: String? = null,
    val isVerified: Boolean? = null,
    val reviews: List<Review>? = null,
    val averageRating: Double? = null,
    val fcmToken: String? = null,
    val preferredPlatforms: List<PreferredPlatform>? = null,
    val targetAudience: TargetAudience? = null,
    val gstNumber: String? = null,
    val verificationRequest: BrandVerificationRequest? = null
)

// Mirrors the backend's BrandVerificationRequest type (brand module) — the
// brand's most recent submission to get its "verified" badge, reviewed by an
// admin via the (admin-webapp-only) reviewBrandVerification mutation.
data class BrandVerificationRequest(
    val method: String,
    val gstNumber: String? = null,
    val documentUrl: String? = null,
    val status: String,
    val submittedAt: String,
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val adminNote: String? = null
)
