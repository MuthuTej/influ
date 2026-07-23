package np.com.bimalkafle.firebaseauthdemoapp.model

data class InfluencerProfile(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val profileCompleted: Boolean?,
    val updatedAt: String?,
    val bio: String?,
    val about: String? = null,
    val creatorName: String? = null,
    val location: String?,
    val gender: String?,
    val motherTongue: String?,
    val languagesKnown: List<String>?,
    val categories: List<Category>?,
    val platforms: List<Platform>?,
    val audienceInsights: AudienceInsights?,
    val strengths: List<String>?,
    val pricing: List<PricingInfo>?,
    val availability: Boolean?,
    val logoUrl: String?,
    val averageRating: Float? = null,
    val isVerified: Boolean? = false,
    val youtubeInsights: YouTubeInsights? = null,
    val instagramMetrics: InstagramMetrics? = null,
    val instagramProfiles: List<InstagramProfile>? = null,
    // New Fields
    val username: String? = null,
    val followers: Int? = null,
    val following: Int? = null,
    val totalPosts: Int? = null,
    val website: String? = null,
    val languages: List<String>? = null,
    val recentPosts: List<RecentPost>? = null,
    val aiInsights: AiInsights? = null,
    // Computed by backend resolvers (engagementRate, tier, totalFollowers from
    // contentAnalytics/platforms; collaborationCount from collaborations collection)
    val engagementRate: Double? = null,
    val collaborationCount: Int? = null,
    val tier: String? = null,
    val totalFollowers: Int? = null
)

data class RecentPost(
    val id: String?,
    val thumbnail: String?,
    val caption: String?,
    val likes: Int?,
    val comments: Int?,
    val views: Int?,
    val uploadDate: String?,
    val url: String?
)

data class AiInsights(
    val primaryNiche: String?,
    val secondaryNiche: String?,
    val contentStyle: String?,
    val tone: String?,
    val audienceInterests: List<String>?,
    val topics: List<String>?,
    val brandSuitability: String?,
    val strengths: List<String>?,
    val weaknesses: List<String>?,
    val professionalSummary: String?,
    val aiSummary: String?,
    val weightingLabel: String? = null,
    val summarySource: String? = null
)

data class InstagramProfile(
    val id: String,
    val profileUrl: String,
    val username: String,
    val followers: Int?,
    val isDefault: Boolean,
    val connectedAt: String,
    val metrics: InstagramMetrics?,
    val aiInsights: AiInsights? = null
)

data class InstagramMetrics(
    val avgComments: Float?,
    val avgLikes: Float?,
    val avgViews: Float?,
    val postingFrequencyDays: Float?,
    val totalPostsAnalyzed: Int?,
    val updatedAt: String?,
    val engagementRate: Float? = null
)

data class YouTubeInsights(
    val channelId: String?,
    val title: String?,
    val description: String?,
    val subscribers: Int?,
    val totalViews: Long?,
    val totalVideos: Int?,
    val demographics: List<YoutubeDemographics>?,
    val revenue: YouTubeRevenue?,
    val lastSynced: String?
)

data class YoutubeDemographics(
    val ageGroup: String?,
    val gender: String?,
    val percentage: Float?
)

data class YouTubeRevenue(
    val estimatedRevenue: Double?
)

data class AudienceInsights(
    val topLocations: List<LocationInsight>?,
    val genderSplit: GenderSplit?,
    val ageGroups: List<AgeGroupInsight>?
)

data class LocationInsight(
    val city: String,
    val country: String,
    val percentage: Float
)

data class GenderSplit(
    val male: Float,
    val female: Float
)

data class AgeGroupInsight(
    val range: String,
    val percentage: Float
)

data class Category(
    val category: String,
    val subCategories: List<String>
)

data class Platform(
    val platform: String,
    val profileUrl: String?,
    val followers: Int?,
    val avgViews: Int?,
    val engagement: Float?,
    val formats: List<String>?,
    val connected: Boolean?,
    val minFollowers: Int? = null,
    val minEngagement: Float? = null
)

data class PricingInfo(
    val platform: String,
    val deliverable: String,
    val count: Int? = null,
    val price: Int,
    val currency: String
)

data class InfluencerProfileInput(
    val name: String,
    val bio: String,
    val location: String,
    val gender: String? = null,
    val motherTongue: String? = null,
    val languagesKnown: List<String>? = null,
    val categories: List<CategoryInput>,
    val platforms: List<PlatformInput>,
    val pricing: List<PricingInput>,
    val availability: Boolean,
    val logoUrl: String? = null,
    val audienceInsights: AudienceInsightsInput? = null,
    val strengths: List<String>? = null
)

data class CategoryInput(
    val category: String,
    val subCategories: List<String>
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
    val count: Int? = null,
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
