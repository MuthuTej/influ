package np.com.bimalkafle.firebaseauthdemoapp.model

data class CollaborationResponse(
    val data: Data
)

data class Data(
    val getCollaborations: List<Collaboration>
)

data class Collaboration(
    val id: String,
    val status: String,
    val message: String?,
    val createdAt: String,
    val campaign: Campaign,
    val pricing: List<Pricing>?,
    val initiatedBy: String,
    val influencer: Influencer
)

data class Campaign(
    val id: String,
    val title: String
)

data class Pricing(
    val currency: String,
    val deliverable: String,
    val platform: String,
    val price: Int
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
    val logoUrl: String?
)
