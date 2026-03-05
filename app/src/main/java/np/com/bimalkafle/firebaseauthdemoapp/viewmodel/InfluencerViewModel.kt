package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.model.*
import np.com.bimalkafle.firebaseauthdemoapp.network.GraphQLClient
import org.json.JSONArray
import org.json.JSONObject

class InfluencerViewModel : ViewModel() {

    private val _influencerProfile = MutableLiveData<InfluencerProfile?>()
    val influencerProfile: LiveData<InfluencerProfile?> = _influencerProfile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchInfluencerDetails(token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetMe {
                  me {
                    id
                    name
                    profileCompleted
                    email
                    role
                    updatedAt
                    ... on Influencer {
                      id
                      email
                      name
                      role
                      profileCompleted
                      updatedAt
                      bio
                      location
                      categories {
                        category
                        subCategories
                      }
                      platforms {
                        platform
                        profileUrl
                        followers
                        avgViews
                        engagement
                        formats
                        connected
                      }
                      strengths
                      pricing {
                        platform
                        deliverable
                        price
                        currency
                      }
                      availability
                      logoUrl
                    }
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    if (data != null) {
                        val meObj = data.optJSONObject("me")
                        if (meObj != null) {
                            val influencer = parseInfluencer(meObj)
                            _influencerProfile.postValue(influencer)
                        } else {
                            _influencerProfile.postValue(null)
                        }
                    } else {
                        val errors = jsonObject.optJSONArray("errors")
                        if (errors != null && errors.length() > 0) {
                            val message = errors.getJSONObject(0).optString("message", "Unknown GraphQL Error")
                            _error.postValue(message)
                        } else {
                            _error.postValue("No data returned")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InfluencerViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
            }
            _loading.postValue(false)
        }
    }

    fun updateInfluencerProfile(
        token: String,
        name: String,
        bio: String,
        location: String,
        logoUrl: String,
        categories: List<Category>,
        platforms: List<Platform>,
        pricing: List<PricingInfo>,
        availability: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val mutation = """
                mutation SetupInfluencerProfile(${'$'}input: InfluencerProfileInput!) {
                  setupInfluencerProfile(input: ${'$'}input) {
                    id
                    name
                    profileCompleted
                  }
                }
            """.trimIndent()

            val variables = mapOf(
                "input" to mapOf(
                    "name" to name,
                    "bio" to bio,
                    "location" to location,
                    "logoUrl" to logoUrl,
                    "categories" to categories.map { mapOf("category" to it.category, "subCategories" to it.subCategories) },
                    "platforms" to platforms.map { 
                        mapOf(
                            "platform" to it.platform,
                            "profileUrl" to (it.profileUrl ?: ""),
                            "followers" to (it.followers ?: 0),
                            "avgViews" to (it.avgViews ?: 0),
                            "engagement" to (it.engagement ?: 0f).toDouble(),
                            "formats" to (it.formats ?: emptyList<String>())
                        )
                    },
                    "pricing" to pricing.map { 
                        mapOf(
                            "platform" to it.platform,
                            "deliverable" to it.deliverable,
                            "price" to it.price,
                            "currency" to it.currency
                        )
                    },
                    "availability" to availability
                )
            )

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null && data.optJSONObject("setupInfluencerProfile") != null) {
                    fetchInfluencerDetails(token)
                    onComplete(true)
                } else {
                    val errors = jsonObject.optJSONArray("errors")
                    val errorMsg = errors?.optJSONObject(0)?.optString("message") ?: "Update failed"
                    _error.postValue(errorMsg)
                    onComplete(false)
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
                onComplete(false)
            }
            _loading.postValue(false)
        }
    }

    fun fetchInfluencerById(id: String, token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetInfluencerById(${"$"}getInfluencerByIdId: ID!){
                  getInfluencerById(id: ${"$"}getInfluencerByIdId) {
                    id
                    email
                    name
                    role
                    profileCompleted
                    updatedAt
                    bio
                    location
                    categories {
                      category
                      subCategories
                    }
                    platforms {
                      platform
                      profileUrl
                      followers
                      avgViews
                      engagement
                      formats
                      connected
                      minFollowers
                      minEngagement
                    }
                    audienceInsights {
                      topLocations {
                        city
                        country
                        percentage
                      }
                      genderSplit {
                        male
                        female
                      }
                      ageGroups {
                        range
                        percentage
                      }
                    }
                    youtubeInsights {
                      channelId
                      title
                      description
                      subscribers
                      totalViews
                      totalVideos
                      demographics {
                        ageGroup
                        gender
                        percentage
                      }
                      revenue {
                        estimatedRevenue
                      }
                      lastSynced
                    }
                    strengths
                    pricing {
                      platform
                      deliverable
                      price
                      currency
                    }
                    availability
                    logoUrl
                  }
                }
            """.trimIndent()

            val variables = mapOf(
                "getInfluencerByIdId" to id
            )

            val result = GraphQLClient.query(query = query, variables = variables as Map<String, Any>?, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    if (data != null) {
                        val influencerObj = data.optJSONObject("getInfluencerById")
                        if (influencerObj != null) {
                            val influencer = parseInfluencer(influencerObj)
                            _influencerProfile.postValue(influencer)
                        } else {
                            _influencerProfile.postValue(null)
                        }
                    } else {
                        val errors = jsonObject.optJSONArray("errors")
                        if (errors != null && errors.length() > 0) {
                            val message = errors.getJSONObject(0).optString("message", "Unknown GraphQL Error")
                            _error.postValue(message)
                        } else {
                            _error.postValue("No data returned")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InfluencerViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
            }
            _loading.postValue(false)
        }
    }

    private fun parseInfluencer(obj: JSONObject): InfluencerProfile {
        val categoriesList = mutableListOf<Category>()
        val categoriesArray = obj.optJSONArray("categories")
        if (categoriesArray != null) {
            for (i in 0 until categoriesArray.length()) {
                val catObj = categoriesArray.optJSONObject(i)
                if (catObj != null) {
                    val subCatsArray = catObj.optJSONArray("subCategories")
                    val subCats = mutableListOf<String>()
                    if (subCatsArray != null) {
                        for (j in 0 until subCatsArray.length()) {
                            subCats.add(subCatsArray.getString(j))
                        }
                    }
                    categoriesList.add(
                        Category(
                            category = catObj.optString("category", ""),
                            subCategories = subCats
                        )
                    )
                }
            }
        }

        val platformsList = mutableListOf<Platform>()
        val platformsArray = obj.optJSONArray("platforms")
        if (platformsArray != null) {
            for (i in 0 until platformsArray.length()) {
                val platObj = platformsArray.optJSONObject(i)
                if (platObj != null) {
                    val formatsArray = platObj.optJSONArray("formats")
                    val formatsList = mutableListOf<String>()
                    if (formatsArray != null) {
                        for (j in 0 until formatsArray.length()) {
                            formatsList.add(formatsArray.getString(j))
                        }
                    }
                    platformsList.add(
                        Platform(
                            platform = platObj.optString("platform", ""),
                            profileUrl = platObj.optString("profileUrl", ""),
                            followers = platObj.optInt("followers", 0),
                            avgViews = platObj.optInt("avgViews", 0),
                            engagement = platObj.optDouble("engagement", 0.0).toFloat(),
                            formats = formatsList,
                            connected = if (platObj.has("connected")) platObj.optBoolean("connected") else null
                        )
                    )
                }
            }
        }

        val strengthsList = mutableListOf<String>()
        val strengthsArray = obj.optJSONArray("strengths")
        if (strengthsArray != null) {
            for (i in 0 until strengthsArray.length()) {
                strengthsList.add(strengthsArray.getString(i))
            }
        }

        val pricingList = mutableListOf<PricingInfo>()
        val pricingArray = obj.optJSONArray("pricing")
        if (pricingArray != null) {
            for (i in 0 until pricingArray.length()) {
                val prObj = pricingArray.optJSONObject(i)
                if (prObj != null) {
                    pricingList.add(
                        PricingInfo(
                            platform = prObj.optString("platform", ""),
                            deliverable = prObj.optString("deliverable", ""),
                            price = prObj.optInt("price", 0),
                            currency = prObj.optString("currency", "")
                        )
                    )
                }
            }
        }

        val audienceInsightsObj = obj.optJSONObject("audienceInsights")
        var audienceInsights: AudienceInsights? = null
        if (audienceInsightsObj != null) {
            val topLocations = mutableListOf<LocationInsight>()
            val locationsArray = audienceInsightsObj.optJSONArray("topLocations")
            if (locationsArray != null) {
                for (i in 0 until locationsArray.length()) {
                    val locObj = locationsArray.optJSONObject(i)
                    if (locObj != null) {
                        topLocations.add(LocationInsight(
                            city = locObj.optString("city"),
                            country = locObj.optString("country"),
                            percentage = locObj.optDouble("percentage").toFloat()
                        ))
                    }
                }
            }

            val genderSplitObj = audienceInsightsObj.optJSONObject("genderSplit")
            val genderSplit = if (genderSplitObj != null) {
                GenderSplit(
                    male = genderSplitObj.optDouble("male").toFloat(),
                    female = genderSplitObj.optDouble("female").toFloat()
                )
            } else null

            val ageGroups = mutableListOf<AgeGroupInsight>()
            val ageArray = audienceInsightsObj.optJSONArray("ageGroups")
            if (ageArray != null) {
                for (i in 0 until ageArray.length()) {
                    val ageObj = ageArray.optJSONObject(i)
                    if (ageObj != null) {
                        ageGroups.add(AgeGroupInsight(
                            range = ageObj.optString("range"),
                            percentage = ageObj.optDouble("percentage").toFloat()
                        ))
                    }
                }
            }

            audienceInsights = AudienceInsights(
                topLocations = topLocations,
                genderSplit = genderSplit,
                ageGroups = ageGroups
            )
        }

        val ytObj = obj.optJSONObject("youtubeInsights")
        var youtubeInsights: YouTubeInsights? = null
        if (ytObj != null) {
            val demographics = mutableListOf<YoutubeDemographics>()
            val demoArray = ytObj.optJSONArray("demographics")
            if (demoArray != null) {
                for (i in 0 until demoArray.length()) {
                    val dObj = demoArray.optJSONObject(i)
                    if (dObj != null) {
                        demographics.add(
                            YoutubeDemographics(
                                ageGroup = dObj.optString("ageGroup"),
                                gender = dObj.optString("gender"),
                                percentage = dObj.optDouble("percentage").toFloat()
                            )
                        )
                    }
                }
            }

            val revenueObj = ytObj.optJSONObject("revenue")
            val revenue = if (revenueObj != null) {
                YouTubeRevenue(estimatedRevenue = revenueObj.optDouble("estimatedRevenue"))
            } else null

            youtubeInsights = YouTubeInsights(
                channelId = ytObj.optString("channelId"),
                title = ytObj.optString("title"),
                description = ytObj.optString("description"),
                subscribers = ytObj.optInt("subscribers"),
                totalViews = ytObj.optLong("totalViews"),
                totalVideos = ytObj.optInt("totalVideos"),
                demographics = demographics,
                revenue = revenue,
                lastSynced = ytObj.optString("lastSynced")
            )
        }

        return InfluencerProfile(
            id = obj.optString("id", ""),
            email = obj.optString("email", ""),
            name = obj.optString("name", ""),
            role = obj.optString("role", ""),
            profileCompleted = if (obj.has("profileCompleted")) obj.optBoolean("profileCompleted") else null,
            updatedAt = obj.optString("updatedAt", null),
            bio = obj.optString("bio", null),
            location = obj.optString("location", null),
            categories = categoriesList,
            platforms = platformsList,
            strengths = strengthsList,
            pricing = pricingList,
            availability = if (obj.has("availability")) obj.optBoolean("availability") else null,
            logoUrl = obj.optString("logoUrl", null),
            audienceInsights = audienceInsights,
            youtubeInsights = youtubeInsights
        )
    }

    private val _collaborations = MutableLiveData<List<Collaboration>>()
    val collaborations: LiveData<List<Collaboration>> = _collaborations

    fun fetchCollaborations(token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetCollaborations {
                  getCollaborations {
                    id
                    campaignId
                    brandId
                    influencerId
                    status
                    message
                    pricing {
                      platform
                      deliverable
                      price
                      currency
                    }
                    brand {
                      id
                      name
                      logoUrl
                    }
                    initiatedBy
                    createdAt
                    updatedAt
                    campaign {
                      id
                      brandId
                      title
                      description
                      budgetMin
                      budgetMax
                      startDate
                      endDate
                      status
                      createdAt
                      updatedAt
                    }
                    paymentStatus
                    razorpayOrderId
                    advancePaid
                    finalPaid
                    totalAmount
                    overallAnalytics {
                      impressions
                      clicks
                      likes
                      comments
                      shares
                      saves
                      views
                      retweets
                    }
                    platformAnalytics {
                      platform
                      duration
                      cost
                      impressions
                      clicks
                      likes
                      comments
                      shares
                      saves
                      views
                      retweets
                    }
                    yt {
                      videoId
                      title
                      viewCount
                      likeCount
                      thumbnail
                      videoUrl
                      fetchedAt
                      analytics {
                        views
                        likes
                        comments
                        shares
                        watchTimeMinutes
                        subscribersGained
                        averageViewDurationSeconds
                        engagementRate
                      }
                    }
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    if (data != null) {
                        val collaborationsArray = data.optJSONArray("getCollaborations")
                        if (collaborationsArray != null) {
                            val list = parseCollaborations(collaborationsArray)
                            _collaborations.postValue(list)
                        } else {
                            _collaborations.postValue(emptyList())
                        }
                    } else {
                        val errors = jsonObject.optJSONArray("errors")
                        if (errors != null && errors.length() > 0) {
                            val message = errors.getJSONObject(0).optString("message", "Unknown GraphQL Error")
                            _error.postValue(message)
                        } else {
                            _error.postValue("No data returned")
                        }
                    }

                } catch (e: Exception) {
                    Log.e("InfluencerViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
            }
            _loading.postValue(false)
        }
    }

    fun updateCollaborationStatus(token: String, collaborationId: String, status: String, message: String? = null, onComplete: (Boolean) -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val mutation = """
                mutation UpdateCollaboration(${'$'}input: UpdateCollaborationInput!) {
                  updateCollaboration(input: ${'$'}input) {
                    id
                    status
                    message
                  }
                }
            """.trimIndent()

            val variables = mapOf(
                "input" to mapOf(
                    "collaborationId" to collaborationId,
                    "status" to status,
                    "message" to message
                )
            )

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null && data.optJSONObject("updateCollaboration") != null) {
                    fetchCollaborations(token) // Refresh the list
                    onComplete(true)
                } else {
                    val errors = jsonObject.optJSONArray("errors")
                    val errorMsg = errors?.optJSONObject(0)?.optString("message") ?: "Update failed"
                    _error.postValue(errorMsg)
                    onComplete(false)
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
            }
            _loading.postValue(false)
        }
    }

    fun applyToCampaign(token: String, campaignId: String, message: String, pricing: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val mutation = """
                mutation ApplyToCampaign(${'$'}input: CreateProposalInput!) {
                  applyToCampaign(input: ${'$'}input) {
                    id
                    status
                  }
                }
            """.trimIndent()

            val variables = mapOf(
                "input" to mapOf(
                    "campaignId" to campaignId,
                    "message" to message,
                    "pricing" to pricing
                )
            )

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null && data.optJSONObject("applyToCampaign") != null) {
                    onComplete(true)
                } else {
                    val errors = jsonObject.optJSONArray("errors")
                    val errorMsg = errors?.optJSONObject(0)?.optString("message") ?: "Application failed"
                    _error.postValue(errorMsg)
                    onComplete(false)
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
                onComplete(false)
            }
            _loading.postValue(false)
        }
    }

    private fun parseCollaborations(jsonArray: JSONArray): List<Collaboration> {
        val list = mutableListOf<Collaboration>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue

            val campaignObj = obj.optJSONObject("campaign")
            val campaign = if (campaignObj != null) {
                Campaign(
                    id = campaignObj.optString("id"),
                    brandId = campaignObj.optString("brandId"),
                    title = campaignObj.optString("title"),
                    description = campaignObj.optString("description"),
                    budgetMin = if (campaignObj.isNull("budgetMin")) null else campaignObj.optInt("budgetMin"),
                    budgetMax = if (campaignObj.isNull("budgetMax")) null else campaignObj.optInt("budgetMax"),
                    startDate = campaignObj.optString("startDate"),
                    endDate = campaignObj.optString("endDate"),
                    status = campaignObj.optString("status"),
                    createdAt = campaignObj.optString("createdAt"),
                    updatedAt = campaignObj.optString("updatedAt")
                )
            } else {
                Campaign("unknown", null, "Unknown Campaign", null, null, null, null, null, null, null, null)
            }

            val influencer = Influencer("Me", null, null, null)
            
            val brandObj = obj.optJSONObject("brand")
            val brand = if (brandObj != null) {
                Brand(
                    id = brandObj.optString("id"),
                    email = brandObj.optString("email"),
                    name = brandObj.optString("name"),
                    role = brandObj.optString("role"),
                    profileCompleted = if(brandObj.has("profileCompleted")) brandObj.optBoolean("profileCompleted") else null,
                    updatedAt = brandObj.optString("updatedAt"),
                    brandCategories = null,
                    about = brandObj.optString("about"),
                    profileUrl = brandObj.optString("profileUrl"),
                    logoUrl = brandObj.optString("logoUrl"),
                    govtId = brandObj.optString("govtId"),
                    isVerified = if(brandObj.has("isVerified")) brandObj.optBoolean("isVerified") else null,
                    reviews = null, // Simplified
                    averageRating = if(brandObj.has("averageRating")) brandObj.optDouble("averageRating") else null,
                    fcmToken = brandObj.optString("fcmToken"),
                    preferredPlatforms = null, // Simplified
                    targetAudience = null // Simplified
                )
            } else null

            val pricingList = mutableListOf<Pricing>()
            val pricingArray = obj.optJSONArray("pricing")
            if (pricingArray != null) {
                for (j in 0 until pricingArray.length()) {
                    val pObj = pricingArray.optJSONObject(j)
                    if (pObj != null) {
                        pricingList.add(
                            Pricing(
                                platform = pObj.optString("platform"),
                                deliverable = pObj.optString("deliverable"),
                                price = pObj.optInt("price"),
                                currency = pObj.optString("currency")
                            )
                        )
                    }
                }
            }

            val oaObj = obj.optJSONObject("overallAnalytics")
            val overallAnalytics = if (oaObj != null) {
                OverallAnalytics(
                    impressions = if (oaObj.isNull("impressions")) null else oaObj.optInt("impressions"),
                    clicks = if (oaObj.isNull("clicks")) null else oaObj.optInt("clicks"),
                    likes = if (oaObj.isNull("likes")) null else oaObj.optInt("likes"),
                    comments = if (oaObj.isNull("comments")) null else oaObj.optInt("comments"),
                    shares = if (oaObj.isNull("shares")) null else oaObj.optInt("shares"),
                    saves = if (oaObj.isNull("saves")) null else oaObj.optInt("saves"),
                    views = if (oaObj.isNull("views")) null else oaObj.optInt("views"),
                    retweets = if (oaObj.isNull("retweets")) null else oaObj.optInt("retweets"),
                    replies = null
                )
            } else null

            val paArray = obj.optJSONArray("platformAnalytics")
            val platformAnalytics = if (paArray != null) {
                val pList = mutableListOf<CollaborationAnalytics>()
                for (j in 0 until paArray.length()) {
                    val pObj = paArray.optJSONObject(j)
                    if (pObj != null) {
                        pList.add(
                            CollaborationAnalytics(
                                platform = pObj.optString("platform"),
                                duration = if (pObj.isNull("duration")) null else pObj.optInt("duration"),
                                cost = if (pObj.isNull("cost")) null else pObj.optDouble("cost").toFloat(),
                                impressions = if (pObj.isNull("impressions")) null else pObj.optInt("impressions"),
                                clicks = if (pObj.isNull("clicks")) null else pObj.optInt("clicks"),
                                likes = if (pObj.isNull("likes")) null else pObj.optInt("likes"),
                                comments = if (pObj.isNull("comments")) null else pObj.optInt("comments"),
                                shares = if (pObj.isNull("shares")) null else pObj.optInt("shares"),
                                saves = if (pObj.isNull("saves")) null else pObj.optInt("saves"),
                                views = if (pObj.isNull("views")) null else pObj.optInt("views"),
                                retweets = if (pObj.isNull("retweets")) null else pObj.optInt("retweets"),
                                replies = null
                            )
                        )
                    }
                }
                pList
            } else null

            val ytArray = obj.optJSONArray("yt")
            val ytList = if (ytArray != null) {
                val list = mutableListOf<YouTubeVideoData>()
                for (j in 0 until ytArray.length()) {
                    val yObj = ytArray.optJSONObject(j) ?: continue
                    val aObj = yObj.optJSONObject("analytics")
                    val summary = if (aObj != null) {
                        YouTubeVideoSummary(
                            views = if (aObj.isNull("views")) null else aObj.optInt("views"),
                            likes = if (aObj.isNull("likes")) null else aObj.optInt("likes"),
                            comments = if (aObj.isNull("comments")) null else aObj.optInt("comments"),
                            shares = if (aObj.isNull("shares")) null else aObj.optInt("shares"),
                            watchTimeMinutes = if (aObj.isNull("watchTimeMinutes")) null else aObj.optDouble("watchTimeMinutes"),
                            subscribersGained = if (aObj.isNull("subscribersGained")) null else aObj.optInt("subscribersGained"),
                            averageViewDurationSeconds = if (aObj.isNull("averageViewDurationSeconds")) null else aObj.optInt("averageViewDurationSeconds"),
                            engagementRate = aObj.optString("engagementRate", null)
                        )
                    } else null

                    list.add(
                        YouTubeVideoData(
                            videoId = yObj.optString("videoId"),
                            title = yObj.optString("title"),
                            viewCount = yObj.optString("viewCount", null),
                            likeCount = yObj.optString("likeCount", null),
                            thumbnail = yObj.optString("thumbnail", null),
                            analytics = summary,
                            videoUrl = yObj.optString("videoUrl", null),
                            fetchedAt = yObj.optString("fetchedAt", null)
                        )
                    )
                }
                list
            } else null

            list.add(
                Collaboration(
                    id = obj.optString("id"),
                    campaignId = obj.optString("campaignId"),
                    brandId = obj.optString("brandId"),
                    influencerId = obj.optString("influencerId"),
                    status = obj.optString("status"),
                    message = obj.optString("message"),
                    pricing = pricingList,
                    initiatedBy = obj.optString("initiatedBy"),
                    createdAt = obj.optString("createdAt"),
                    updatedAt = obj.optString("updatedAt"),
                    campaign = campaign,
                    influencer = influencer,
                    paymentStatus = obj.optString("paymentStatus"),
                    razorpayOrderId = obj.optString("razorpayOrderId"),
                    advancePaid = if (obj.isNull("advancePaid")) null else obj.optBoolean("advancePaid"),
                    finalPaid = if (obj.isNull("finalPaid")) null else obj.optBoolean("finalPaid"),
                    totalAmount = if (obj.isNull("totalAmount")) null else obj.optDouble("totalAmount"),
                    brand = brand,
                    overallAnalytics = overallAnalytics,
                    platformAnalytics = platformAnalytics,
                    yt = ytList
                )
            )
        }
        return list
    }

    private val _brands = MutableLiveData<List<Brand>>()
    val brands: LiveData<List<Brand>> = _brands

    fun fetchBrands(token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetBrands {
                  getBrands {
                    id
                    email
                    name
                    role
                    profileCompleted
                    updatedAt
                    brandCategories {
                      category
                      subCategories
                    }
                    about
                    preferredPlatforms {
                      platform
                      profileUrl
                      followers
                      avgViews
                      engagement
                      formats
                      connected
                      minFollowers
                      minEngagement
                    }
                    targetAudience {
                      ageMin
                      ageMax
                      gender
                      locations
                    }
                    profileUrl
                    logoUrl
                    govtId
                    isVerified
                    reviews {
                      id
                      collaborationId
                      reviewerId
                      revieweeId
                      reviewerRole
                      rating
                      comment
                      createdAt
                      reviewer {
                        id
                        email
                        name
                        role
                        profileCompleted
                        updatedAt
                        govtId
                        isVerified
                        fcmToken
                        averageRating
                      }
                      reviewee {
                        id
                        email
                        name
                        role
                        profileCompleted
                        updatedAt
                        govtId
                        isVerified
                        fcmToken
                        averageRating
                      }
                    }
                    averageRating
                    fcmToken
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    if (data != null) {
                        val brandsArray = data.optJSONArray("getBrands")
                        if (brandsArray != null) {
                            val list = parseBrands(brandsArray)
                            _brands.postValue(list)
                        } else {
                            _brands.postValue(emptyList())
                        }
                    } else {
                        val errors = jsonObject.optJSONArray("errors")
                        if (errors != null && errors.length() > 0) {
                            val message = errors.getJSONObject(0).optString("message", "Unknown GraphQL Error")
                            _error.postValue(message)
                        } else {
                            _error.postValue("No data returned")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InfluencerViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
            }
            _loading.postValue(false)
        }
    }

    private fun parseBrands(jsonArray: JSONArray): List<Brand> {
        val list = mutableListOf<Brand>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue

            val categoriesArray = obj.optJSONArray("brandCategories")
            val brandCategories = mutableListOf<BrandCategory>()
            if (categoriesArray != null) {
                for (j in 0 until categoriesArray.length()) {
                    val categoryObj = categoriesArray.optJSONObject(j)
                    if (categoryObj != null) {
                        val subCatsArray = categoryObj.optJSONArray("subCategories")
                        val subCats = mutableListOf<String>()
                        if (subCatsArray != null) {
                            for (k in 0 until subCatsArray.length()) {
                                subCats.add(subCatsArray.getString(k))
                            }
                        }
                        brandCategories.add(
                            BrandCategory(
                                category = categoryObj.optString("category", ""),
                                subCategories = subCats
                            )
                        )
                    }
                }
            }

            // preferredPlatforms
            val prefPlatforms = mutableListOf<PreferredPlatform>()
            val prefArray = obj.optJSONArray("preferredPlatforms")
            if (prefArray != null) {
                for (j in 0 until prefArray.length()) {
                    val pObj = prefArray.optJSONObject(j)
                    if (pObj != null) {
                        val formatsArray = pObj.optJSONArray("formats")
                        val formatsList = mutableListOf<String>()
                        if (formatsArray != null) {
                            for (k in 0 until formatsArray.length()) {
                                formatsList.add(formatsArray.getString(k))
                            }
                        }
                        
                        prefPlatforms.add(
                            PreferredPlatform(
                                platform = pObj.optString("platform"),
                                profileUrl = pObj.optString("profileUrl"),
                                followers = if(pObj.has("followers")) pObj.optInt("followers") else null,
                                avgViews = if(pObj.has("avgViews")) pObj.optInt("avgViews") else null,
                                engagement = if(pObj.has("engagement")) pObj.optDouble("engagement").toFloat() else null,
                                formats = formatsList,
                                connected = if(pObj.has("connected")) pObj.optBoolean("connected") else null,
                                minFollowers = if(pObj.has("minFollowers")) pObj.optInt("minFollowers") else null,
                                minEngagement = if(pObj.has("minEngagement")) pObj.optDouble("minEngagement").toFloat() else null
                            )
                        )
                    }
                }
            }

            // targetAudience
            val taObj = obj.optJSONObject("targetAudience")
            val targetAudience = if (taObj != null) {
                val locationsArray = taObj.optJSONArray("locations")
                val locationsList = mutableListOf<String>()
                if (locationsArray != null) {
                    for (k in 0 until locationsArray.length()) {
                        locationsList.add(locationsArray.getString(k))
                    }
                }
                
                TargetAudience(
                    ageMin = if(taObj.has("ageMin")) taObj.optInt("ageMin") else null,
                    ageMax = if(taObj.has("ageMax")) taObj.optInt("ageMax") else null,
                    gender = taObj.optString("gender"),
                    locations = locationsList
                )
            } else null
            
            // reviews
            val reviewsList = mutableListOf<Review>()
            val reviewsArray = obj.optJSONArray("reviews")
            if (reviewsArray != null) {
                for (j in 0 until reviewsArray.length()) {
                    val rObj = reviewsArray.optJSONObject(j)
                    if (rObj != null) {
                        val reviewerObj = rObj.optJSONObject("reviewer")
                        val reviewer = if (reviewerObj != null) {
                            Reviewer(
                                id = reviewerObj.optString("id"),
                                email = reviewerObj.optString("email"),
                                name = reviewerObj.optString("name"),
                                role = reviewerObj.optString("role"),
                                profileCompleted = if(reviewerObj.has("profileCompleted")) reviewerObj.optBoolean("profileCompleted") else null,
                                updatedAt = reviewerObj.optString("updatedAt"),
                                govtId = reviewerObj.optString("govtId"),
                                isVerified = if(reviewerObj.has("isVerified")) reviewerObj.optBoolean("isVerified") else null,
                                fcmToken = reviewerObj.optString("fcmToken"),
                                averageRating = if(reviewerObj.has("averageRating")) reviewerObj.optDouble("averageRating") else null
                            )
                        } else null
                        
                        val revieweeObj = rObj.optJSONObject("reviewee")
                        val reviewee = if (revieweeObj != null) {
                             Reviewer(
                                id = revieweeObj.optString("id"),
                                email = revieweeObj.optString("email"),
                                name = revieweeObj.optString("name"),
                                role = revieweeObj.optString("role"),
                                profileCompleted = if(revieweeObj.has("profileCompleted")) revieweeObj.optBoolean("profileCompleted") else null,
                                updatedAt = revieweeObj.optString("updatedAt"),
                                govtId = revieweeObj.optString("govtId"),
                                isVerified = if(revieweeObj.has("isVerified")) revieweeObj.optBoolean("isVerified") else null,
                                fcmToken = revieweeObj.optString("fcmToken"),
                                averageRating = if(revieweeObj.has("averageRating")) revieweeObj.optDouble("averageRating") else null
                            )
                        } else null

                        reviewsList.add(
                            Review(
                                id = rObj.optString("id"),
                                collaborationId = if(rObj.isNull("collaborationId")) null else rObj.optString("collaborationId"),
                                reviewerId = rObj.optString("reviewerId"),
                                revieweeId = rObj.optString("revieweeId"),
                                reviewerRole = rObj.optString("reviewerRole"),
                                rating = rObj.optDouble("rating"),
                                comment = rObj.optString("comment"),
                                createdAt = rObj.optString("createdAt"),
                                reviewer = reviewer,
                                reviewee = reviewee
                            )
                        )
                    }
                }
            }
            
            list.add(
                Brand(
                    id = obj.optString("id"),
                    email = obj.optString("email"),
                    name = obj.optString("name"),
                    role = obj.optString("role"),
                    profileCompleted = if(obj.has("profileCompleted")) obj.optBoolean("profileCompleted") else null,
                    updatedAt = obj.optString("updatedAt"),
                    brandCategories = brandCategories,
                    about = obj.optString("about"),
                    profileUrl = obj.optString("profileUrl"),
                    logoUrl = obj.optString("logoUrl"),
                    govtId = obj.optString("govtId"),
                    isVerified = if(obj.has("isVerified")) obj.optBoolean("isVerified") else null,
                    reviews = reviewsList,
                    averageRating = if(obj.has("averageRating")) obj.optDouble("averageRating") else null,
                    fcmToken = obj.optString("fcmToken"),
                    preferredPlatforms = prefPlatforms,
                    targetAudience = targetAudience
                )
            )
        }
        return list
    }
}
