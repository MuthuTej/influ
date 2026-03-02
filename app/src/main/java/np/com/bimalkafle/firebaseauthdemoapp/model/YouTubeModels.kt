package np.com.bimalkafle.firebaseauthdemoapp.model

data class YouTubeChannelData(
    val channelId: String,
    val title: String,
    val description: String?,
    val customUrl: String?,
    val publishedAt: String?,
    val thumbnail: String?,
    val bannerImageUrl: String?,
    val subscriberCount: Int,
    val viewCount: Int,
    val totalVideoViews: Int?,
    val videoCount: Int,
    val uploadsPlaylistId: String?,
    val privacyStatus: String?,
    val keywords: String?,
    val country: String?,
    val topics: List<String> = emptyList(),
    val syncedAt: String?
)

data class YouTubeOAuthResponse(
    val success: Boolean,
    val channelData: YouTubeChannelData?,
    val error: String?
)
