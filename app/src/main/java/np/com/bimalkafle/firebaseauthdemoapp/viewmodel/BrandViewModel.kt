package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

import np.com.bimalkafle.firebaseauthdemoapp.model.*
import np.com.bimalkafle.firebaseauthdemoapp.network.GraphQLClient
import org.json.JSONArray
import org.json.JSONObject

class BrandViewModel : ViewModel() {

    private val _collaborations = MutableLiveData<List<Collaboration>>()
    val collaborations: LiveData<List<Collaboration>> = _collaborations

    private val _influencers = MutableLiveData<List<InfluencerProfile>>()
    val influencers: LiveData<List<InfluencerProfile>> = _influencers

    // Separate LiveData for the search page — server-ranked+paginated results
    // from searchInfluencers, so home-page recommendation data in _influencers
    // is never overwritten by a partial page.
    private val _searchResults = MutableLiveData<List<InfluencerProfile>>(emptyList())
    val searchResults: LiveData<List<InfluencerProfile>> = _searchResults

    private val _searchMeta = MutableLiveData(SearchMeta())
    val searchMeta: LiveData<SearchMeta> = _searchMeta

    private val _loadingMore = MutableLiveData(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

    private val _overallTopInfluencers = MutableLiveData<List<InfluencerProfile>>()
    val overallTopInfluencers: LiveData<List<InfluencerProfile>> = _overallTopInfluencers

    private val _youtubeTopInfluencers = MutableLiveData<List<InfluencerProfile>>()
    val youtubeTopInfluencers: LiveData<List<InfluencerProfile>> = _youtubeTopInfluencers

    private val _instagramTopInfluencers = MutableLiveData<List<InfluencerProfile>>()
    val instagramTopInfluencers: LiveData<List<InfluencerProfile>> = _instagramTopInfluencers

    private val _brandProfile = MutableLiveData<Brand?>()
    val brandProfile: LiveData<Brand?> = _brandProfile
    
    private val _myCampaigns = MutableLiveData<List<Campaign>>()
    val myCampaigns: LiveData<List<Campaign>> = _myCampaigns

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun clearError() { _error.value = null }

    private val _wishlistedInfluencers = MutableLiveData<List<InfluencerProfile>>(emptyList())
    val wishlistedInfluencers: LiveData<List<InfluencerProfile>> = _wishlistedInfluencers

    // Stale-while-revalidate: skips a refetch (and its loading flash) if the same
    // data was fetched within the last minute, so switching tabs doesn't discard
    // good cached data. Pass force = true (e.g. pull-to-refresh) to bypass it.
    private val fetchThrottle = FetchThrottle()

    fun fetchInfluencerRecommendations(token: String, allInfluencers: List<InfluencerProfile>? = null) {
        viewModelScope.launch {
            val query = """
                query GetInfluencerRecommendations {
                  getOverallTopInfluencers(topN: 10) { id score }
                  getTopYoutubeInfluencers(topN: 10) { id score }
                  getTopInstagramInfluencers(topN: 10) { id score }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null) {
                    val overallRecs = parseRecommendations(data.optJSONArray("getOverallTopInfluencers"))
                    val youtubeRecs = parseRecommendations(data.optJSONArray("getTopYoutubeInfluencers"))
                    val instagramRecs = parseRecommendations(data.optJSONArray("getTopInstagramInfluencers"))

                    val allInf = allInfluencers ?: _influencers.value ?: emptyList()
                    
                    _overallTopInfluencers.postValue(sortInfluencers(allInf, overallRecs))
                    _youtubeTopInfluencers.postValue(sortInfluencers(allInf, youtubeRecs))
                    _instagramTopInfluencers.postValue(sortInfluencers(allInf, instagramRecs))
                }
            }.onFailure {
                Log.e("BrandViewModel", "Failed to fetch recommendations", it)
            }
        }
    }

    fun fetchHomeRecommendations(token: String, force: Boolean = false) {
        if (!fetchThrottle.shouldFetch("homeRecs", force)) return
        _loading.value = true
        viewModelScope.launch {
            launch { fetchWishlist(token) }
            val allDef   = async { runHomeSearchQuery(token, null) }
            val ytDef    = async { runHomeSearchQuery(token, "YOUTUBE") }
            val igDef    = async { runHomeSearchQuery(token, "INSTAGRAM") }
            _overallTopInfluencers.postValue(allDef.await())
            _youtubeTopInfluencers.postValue(ytDef.await())
            _instagramTopInfluencers.postValue(igDef.await())
            _loading.postValue(false)
        }
    }

    private suspend fun runHomeSearchQuery(token: String, platform: String?): List<InfluencerProfile> {
        val filterArg = if (platform != null) "filters: { platforms: [\"$platform\"] }, " else ""
        val query = """
            query HomeRecs {
              searchInfluencers(${filterArg}page: 0, limit: 10) {
                influencers {
                  id email name bio about creatorName location gender availability logoUrl isVerified averageRating
                  tier totalFollowers engagementRate collaborationCount
                  categories { category subCategories }
                  platforms { platform profileUrl followers avgViews engagement formats connected }
                  strengths
                  instagramMetrics { avgLikes avgComments avgViews engagementRate }
                  aiInsights { primaryNiche brandSuitability }
                }
                total page hasMore
              }
            }
        """.trimIndent()
        return try {
            val result = GraphQLClient.query(query = query, token = token)
            result.getOrNull()
                ?.optJSONObject("data")
                ?.optJSONObject("searchInfluencers")
                ?.optJSONArray("influencers")
                ?.let { parseInfluencers(it) }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("BrandViewModel", "homeSearch error", e)
            emptyList()
        }
    }

    private fun parseRecommendations(array: JSONArray?): List<Pair<String, Double>> {
        val list = mutableListOf<Pair<String, Double>>()
        if (array != null) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                list.add(obj.optString("id") to obj.optDouble("score"))
            }
        }
        return list
    }

    private fun sortInfluencers(all: List<InfluencerProfile>, recs: List<Pair<String, Double>>): List<InfluencerProfile> {
        val idToScore = recs.toMap()
        return all.filter { idToScore.containsKey(it.id) }
            .sortedByDescending { idToScore[it.id] }
    }

    fun fetchInfluencers(token: String, force: Boolean = false) {
        if (!fetchThrottle.shouldFetch("influencers", force)) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            fetchWishlist(token)
            val query = """
                query GetInfluencers {
                  getInfluencers {
                    id
                    email
                    name
                    role
                    profileCompleted
                    updatedAt
                    bio
                    about
                    creatorName
                    location
                    gender
                    motherTongue
                    languagesKnown
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
                    strengths
                    pricing {
                      platform
                      deliverable
                      price
                      currency
                    }
                    availability
                    logoUrl
                    averageRating
                    isVerified
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    if (data != null) {
                        val influencersArray = data.optJSONArray("getInfluencers")
                        if (influencersArray != null) {
                            val list = parseInfluencers(influencersArray)
                            _influencers.postValue(list)
                            fetchInfluencerRecommendations(token, list)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BrandViewModel", "Parsing error", e)
                }
            }
            _loading.postValue(false)
        }
    }

    private fun parseInfluencers(jsonArray: JSONArray): List<InfluencerProfile> {
        val list = mutableListOf<InfluencerProfile>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i)
            if (obj != null) {
                list.add(parseInfluencerProfile(obj))
            }
        }
        return list
    }

    private fun parseInfluencerProfile(obj: JSONObject): InfluencerProfile {
        val categoriesArray = obj.optJSONArray("categories")
        val categories = mutableListOf<Category>()
        if (categoriesArray != null) {
            for (i in 0 until categoriesArray.length()) {
                val cObj = categoriesArray.optJSONObject(i)
                if (cObj != null) {
                    val subCatsArray = cObj.optJSONArray("subCategories")
                    val subCats = mutableListOf<String>()
                    if (subCatsArray != null) {
                        for (j in 0 until subCatsArray.length()) {
                            subCats.add(subCatsArray.getString(j))
                        }
                    }
                    categories.add(Category(cObj.optString("category"), subCats))
                }
            }
        }

        val platformsArray = obj.optJSONArray("platforms")
        val platforms = mutableListOf<Platform>()
        if (platformsArray != null) {
            for (i in 0 until platformsArray.length()) {
                val pObj = platformsArray.optJSONObject(i)
                if (pObj != null) {
                    val formatsArray = pObj.optJSONArray("formats")
                    val formatsList = mutableListOf<String>()
                    if (formatsArray != null) {
                        for (j in 0 until formatsArray.length()) {
                            formatsList.add(formatsArray.getString(j))
                        }
                    }
                    platforms.add(
                        Platform(
                            platform = pObj.optString("platform"),
                            profileUrl = pObj.optString("profileUrl", ""),
                            followers = if (pObj.has("followers")) pObj.optInt("followers") else null,
                            avgViews = if (pObj.has("avgViews")) pObj.optInt("avgViews") else null,
                            engagement = if (pObj.has("engagement")) pObj.optDouble("engagement").toFloat() else null,
                            formats = formatsList,
                            connected = if (pObj.has("connected")) pObj.optBoolean("connected") else null
                        )
                    )
                }
            }
        }

        val pricingArray = obj.optJSONArray("pricing")
        val pricing = mutableListOf<PricingInfo>()
        if (pricingArray != null) {
            for (i in 0 until pricingArray.length()) {
                val pObj = pricingArray.optJSONObject(i)
                if (pObj != null) {
                    pricing.add(
                        PricingInfo(
                            platform = pObj.optString("platform"),
                            deliverable = pObj.optString("deliverable"),
                            price = pObj.optInt("price"),
                            currency = pObj.optString("currency")
                        )
                    )
                }
            }
        }

        val strengthsArray = obj.optJSONArray("strengths")
        val strengths = mutableListOf<String>()
        if (strengthsArray != null) {
            for (i in 0 until strengthsArray.length()) { strengths.add(strengthsArray.getString(i))
            }
        }

        val aiObj = obj.optJSONObject("audienceInsights")
        val audienceInsights = if (aiObj != null) {
            val topLocationsArray = aiObj.optJSONArray("topLocations")
            val topLocations = mutableListOf<LocationInsight>()
            if (topLocationsArray != null) {
                for (i in 0 until topLocationsArray.length()) {
                    val lObj = topLocationsArray.optJSONObject(i)
                    if (lObj != null) {
                        topLocations.add(LocationInsight(lObj.optString("city"), lObj.optString("country"), lObj.optDouble("percentage").toFloat()))
                    }
                }
            }

            val gsObj = aiObj.optJSONObject("genderSplit")
            val genderSplit = if (gsObj != null) {
                GenderSplit(gsObj.optDouble("male").toFloat(), gsObj.optDouble("female").toFloat())
            } else null

            val ageGroupsArray = aiObj.optJSONArray("ageGroups")
            val ageGroups = mutableListOf<AgeGroupInsight>()
            if (ageGroupsArray != null) {
                for (i in 0 until ageGroupsArray.length()) {
                    val aObj = ageGroupsArray.optJSONObject(i)
                    if (aObj != null) {
                        ageGroups.add(AgeGroupInsight(aObj.optString("range"), aObj.optDouble("percentage").toFloat()))
                    }
                }
            }

            AudienceInsights(topLocations, genderSplit, ageGroups)
        } else null

        val languagesArray = obj.optJSONArray("languagesKnown")
        val languagesList = mutableListOf<String>()
        if (languagesArray != null) {
            for (i in 0 until languagesArray.length()) {
                languagesList.add(languagesArray.getString(i))
            }
        }

        return InfluencerProfile(
            id = obj.optString("id"),
            email = obj.optString("email"),
            name = obj.optString("name"),
            role = obj.optString("role"),
            profileCompleted = if (obj.has("profileCompleted")) obj.optBoolean("profileCompleted") else null,
            updatedAt = obj.optString("updatedAt"),
            bio = obj.optString("bio").takeIf { it.isNotBlank() },
            about = obj.optString("about").takeIf { it.isNotBlank() },
            creatorName = obj.optString("creatorName").takeIf { it.isNotBlank() },
            location = obj.optString("location"),
            gender = obj.optString("gender").takeIf { it.isNotBlank() },
            motherTongue = obj.optString("motherTongue").takeIf { it.isNotBlank() },
            languagesKnown = if (languagesList.isNotEmpty()) languagesList else null,
            categories = categories,
            platforms = platforms,
            audienceInsights = audienceInsights,
            strengths = strengths,
            pricing = pricing,
            availability = if (obj.has("availability")) obj.optBoolean("availability") else null,
            logoUrl = obj.optString("logoUrl"),
            averageRating = if (obj.has("averageRating") && !obj.isNull("averageRating")) obj.optDouble("averageRating").toFloat() else null,
            isVerified = obj.optBoolean("isVerified", false),
            engagementRate = if (obj.has("engagementRate") && !obj.isNull("engagementRate")) obj.optDouble("engagementRate") else null,
            collaborationCount = if (obj.has("collaborationCount") && !obj.isNull("collaborationCount")) obj.optInt("collaborationCount") else null,
            tier = obj.optString("tier").takeIf { it.isNotBlank() },
            totalFollowers = if (obj.has("totalFollowers") && !obj.isNull("totalFollowers")) obj.optInt("totalFollowers") else null,
            instagramMetrics = obj.optJSONObject("instagramMetrics")?.let { im ->
                InstagramMetrics(
                    avgLikes = if (im.has("avgLikes") && !im.isNull("avgLikes")) im.optDouble("avgLikes").toFloat() else null,
                    avgComments = if (im.has("avgComments") && !im.isNull("avgComments")) im.optDouble("avgComments").toFloat() else null,
                    avgViews = if (im.has("avgViews") && !im.isNull("avgViews")) im.optDouble("avgViews").toFloat() else null,
                    postingFrequencyDays = if (im.has("postingFrequencyDays") && !im.isNull("postingFrequencyDays")) im.optDouble("postingFrequencyDays").toFloat() else null,
                    totalPostsAnalyzed = if (im.has("totalPostsAnalyzed") && !im.isNull("totalPostsAnalyzed")) im.optInt("totalPostsAnalyzed") else null,
                    updatedAt = im.optString("updatedAt").takeIf { it.isNotBlank() },
                    engagementRate = if (im.has("engagementRate") && !im.isNull("engagementRate")) im.optDouble("engagementRate").toFloat() else null
                )
            },
            aiInsights = obj.optJSONObject("aiInsights")?.let { ai ->
                AiInsights(
                    primaryNiche = ai.optString("primaryNiche").takeIf { it.isNotBlank() },
                    secondaryNiche = ai.optString("secondaryNiche").takeIf { it.isNotBlank() },
                    contentStyle = ai.optString("contentStyle").takeIf { it.isNotBlank() },
                    tone = ai.optString("tone").takeIf { it.isNotBlank() },
                    audienceInterests = ai.optJSONArray("audienceInterests")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    },
                    topics = null,
                    brandSuitability = ai.optString("brandSuitability").takeIf { it.isNotBlank() },
                    strengths = ai.optJSONArray("strengths")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    },
                    weaknesses = null,
                    professionalSummary = ai.optString("professionalSummary").takeIf { it.isNotBlank() },
                    aiSummary = ai.optString("aiSummary").takeIf { it.isNotBlank() }
                )
            }
        )
    }

    fun fetchBrandDetails(token: String, force: Boolean = false) {
        if (!fetchThrottle.shouldFetch("brandDetails", force)) return
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetMe {
                  me {
                    ... on Brand {
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
                      profileUrl
                      logoUrl
                      preferredPlatforms {
                        platform
                        formats
                        minFollowers
                        minEngagement
                      }
                      targetAudience {
                        ageMin
                        ageMax
                        gender
                        locations
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
                        val meObj = data.optJSONObject("me")
                        if (meObj != null) {
                            val brand = parseBrand(meObj)
                            _brandProfile.postValue(brand)
                        } else {
                            _brandProfile.postValue(null)
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
                    Log.e("BrandViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${e.message}")
                }
            }.onFailure {
                Log.e("BrandViewModel", "Network error", it)
                _error.postValue("Network error: ${it.message}")
            }
            _loading.postValue(false)
        }
    }

    private fun parseBrand(obj: JSONObject): Brand {
        val categoriesArray = obj.optJSONArray("brandCategories")
        val brandCategories = mutableListOf<BrandCategory>()
        if (categoriesArray != null) {
            for (i in 0 until categoriesArray.length()) {
                val categoryObj = categoriesArray.optJSONObject(i)
                if (categoryObj != null) {
                    val subCatsArray = categoryObj.optJSONArray("subCategories")
                    val subCats = mutableListOf<String>()
                    if (subCatsArray != null) {
                        for (j in 0 until subCatsArray.length()) {
                            subCats.add(subCatsArray.getString(j))
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

        val preferredPlatformsArray = obj.optJSONArray("preferredPlatforms")
        val preferredPlatforms = if (preferredPlatformsArray != null) {
            val list = mutableListOf<PreferredPlatform>()
            for (i in 0 until preferredPlatformsArray.length()) {
                val pObj = preferredPlatformsArray.optJSONObject(i)
                if (pObj != null) {
                    val formatsArray = pObj.optJSONArray("formats")
                    val formats = if (formatsArray != null) {
                        val fList = mutableListOf<String>()
                        for (j in 0 until formatsArray.length()) {
                            fList.add(formatsArray.getString(j))
                        }
                        fList
                    } else null
                    list.add(
                        PreferredPlatform(
                            platform = pObj.optString("platform"),
                            profileUrl = null,
                            followers = null,
                            avgViews = null,
                            engagement = null,
                            formats = formats,
                            connected = null,
                            minFollowers = if (pObj.isNull("minFollowers")) null else pObj.optInt("minFollowers"),
                            minEngagement = if (pObj.isNull("minEngagement")) null else pObj.optDouble("minEngagement").toFloat()
                        )
                    )
                }
            }
            list
        } else null

        val targetAudienceObj = obj.optJSONObject("targetAudience")
        val targetAudience = if (targetAudienceObj != null) {
            val locationsArray = targetAudienceObj.optJSONArray("locations")
            val locations = if (locationsArray != null) {
                val lList = mutableListOf<String>()
                for (i in 0 until locationsArray.length()) {
                    lList.add(locationsArray.getString(i))
                }
                lList
            } else null
            
            TargetAudience(
                ageMin = if (targetAudienceObj.isNull("ageMin")) null else targetAudienceObj.optInt("ageMin"),
                ageMax = if (targetAudienceObj.isNull("ageMax")) null else targetAudienceObj.optInt("ageMax"),
                gender = if (targetAudienceObj.isNull("gender")) null else targetAudienceObj.optString("gender"),
                locations = locations
            )
        } else null

        return Brand(
            id = obj.optString("id", ""),
            email = obj.optString("email", ""),
            name = obj.optString("name", ""),
            role = obj.optString("role", ""),
            profileCompleted = if (obj.has("profileCompleted")) obj.optBoolean("profileCompleted") else null,
            updatedAt = if (obj.isNull("updatedAt")) null else obj.optString("updatedAt"),
            brandCategories = brandCategories,
            about = if (obj.isNull("about")) null else obj.optString("about"),
            profileUrl = if (obj.isNull("profileUrl")) null else obj.optString("profileUrl"),
            logoUrl = if (obj.isNull("logoUrl")) null else obj.optString("logoUrl"),
            preferredPlatforms = preferredPlatforms,
            targetAudience = targetAudience
        )
    }

    fun fetchCollaborations(token: String, force: Boolean = false) {
        if (!fetchThrottle.shouldFetch("collaborations", force)) return
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
                    influencer {
                      name
                      bio
                      logoUrl
                      updatedAt
                    }
                    brand {
                      id
                      email
                      name
                      role
                      about
                      profileUrl
                      logoUrl
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
                      platform duration cost impressions clicks likes comments shares saves views retweets
                    }
                    yt {
                      videoId
                      title
                      authorName
                      channelUrl
                      description
                      duration
                      publishedAt
                      viewCount
                      likeCount
                      commentCount
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
                        engagementRate
                      }
                    }
                    ig {
                      postId
                      caption
                      likeCount
                      commentCount
                      viewCount
                      mediaUrl
                      timestamp
                      fetchedAt
                    }
                    performanceMilestones {
                      label
                      hoursAfterPost
                      views
                      likes
                      comments
                      capturedAt
                    }
                    totalViewsDelivered
                    viewsGrowthSincePosting
                    selectedInstagramProfileId
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    val collaborationsArray = data?.optJSONArray("getCollaborations")
                    if (collaborationsArray != null) {
                        val list = parseCollaborations(collaborationsArray)
                        _collaborations.postValue(list)
                    }
                } catch (e: Exception) {
                    Log.e("BrandViewModel", "fetchCollaborations parse error", e)
                    _error.postValue("Failed to load collaborations: ${e.message}")
                }
            }.onFailure { err ->
                Log.e("BrandViewModel", "fetchCollaborations network error", err)
                _error.postValue(err.message)
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

            val influencerObj = obj.optJSONObject("influencer")
            val influencer = if (influencerObj != null) {
                 Influencer(
                    name = influencerObj.optString("name", "Unknown"),
                    bio = if (influencerObj.isNull("bio")) null else influencerObj.optString("bio"),
                    logoUrl = if (influencerObj.isNull("logoUrl")) null else influencerObj.optString("logoUrl"),
                    updatedAt = if (influencerObj.isNull("updatedAt")) null else influencerObj.optString("updatedAt")
                )
            } else {
                 Influencer("Unknown", null, null, null)
            }

            val brandObj = obj.optJSONObject("brand")
            val brand = if (brandObj != null) {
                Brand(
                    id = brandObj.optString("id", ""),
                    email = brandObj.optString("email", ""),
                    name = brandObj.optString("name", "Unknown Brand"),
                    role = brandObj.optString("role", ""),
                    profileCompleted = null,
                    updatedAt = null,
                    brandCategories = null,
                    about = if (brandObj.isNull("about")) null else brandObj.optString("about"),
                    profileUrl = if (brandObj.isNull("profileUrl")) null else brandObj.optString("profileUrl"),
                    logoUrl = if (brandObj.isNull("logoUrl")) null else brandObj.optString("logoUrl"),
                    preferredPlatforms = null,
                    targetAudience = null
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

            // Parse Analytics
            val overallObj = obj.optJSONObject("overallAnalytics")
            val overallAnalytics = if (overallObj != null) {
                OverallAnalytics(
                    impressions = if (overallObj.isNull("impressions")) null else overallObj.optInt("impressions"),
                    clicks = if (overallObj.isNull("clicks")) null else overallObj.optInt("clicks"),
                    likes = if (overallObj.isNull("likes")) null else overallObj.optInt("likes"),
                    comments = if (overallObj.isNull("comments")) null else overallObj.optInt("comments"),
                    shares = if (overallObj.isNull("shares")) null else overallObj.optInt("shares"),
                    saves = if (overallObj.isNull("saves")) null else overallObj.optInt("saves"),
                    views = if (overallObj.isNull("views")) null else overallObj.optInt("views"),
                    retweets = if (overallObj.isNull("retweets")) null else overallObj.optInt("retweets"),
                    replies = null
                )
            } else null

            val platformAnalyticsList = mutableListOf<CollaborationAnalytics>()
            val platformAnalyticsArray = obj.optJSONArray("platformAnalytics")
            if (platformAnalyticsArray != null) {
                for (j in 0 until platformAnalyticsArray.length()) {
                    val paObj = platformAnalyticsArray.optJSONObject(j) ?: continue
                    platformAnalyticsList.add(
                        CollaborationAnalytics(
                            platform = paObj.optString("platform"),
                            duration = if (paObj.isNull("duration")) null else paObj.optInt("duration"),
                            cost = if (paObj.isNull("cost")) null else paObj.optDouble("cost").toFloat(),
                            impressions = if (paObj.isNull("impressions")) null else paObj.optInt("impressions"),
                            clicks = if (paObj.isNull("clicks")) null else paObj.optInt("clicks"),
                            likes = if (paObj.isNull("likes")) null else paObj.optInt("likes"),
                            comments = if (paObj.isNull("comments")) null else paObj.optInt("comments"),
                            shares = if (paObj.isNull("shares")) null else paObj.optInt("shares"),
                            saves = if (paObj.isNull("saves")) null else paObj.optInt("saves"),
                            views = if (paObj.isNull("views")) null else paObj.optInt("views"),
                            retweets = if (paObj.isNull("retweets")) null else paObj.optInt("retweets"),
                            replies = null
                        )
                    )
                }
            }

            // Parse YouTube Data
            val ytList = mutableListOf<YouTubeVideoData>()
            val ytArray = obj.optJSONArray("yt")
            if (ytArray != null) {
                for (j in 0 until ytArray.length()) {
                    val ytObj = ytArray.optJSONObject(j) ?: continue
                    val analyticsObj = ytObj.optJSONObject("analytics")
                    val ytAnalytics = if (analyticsObj != null) {
                        YouTubeVideoSummary(
                            views = if (analyticsObj.isNull("views")) null else analyticsObj.optInt("views"),
                            likes = if (analyticsObj.isNull("likes")) null else analyticsObj.optInt("likes"),
                            comments = if (analyticsObj.isNull("comments")) null else analyticsObj.optInt("comments"),
                            shares = if (analyticsObj.isNull("shares")) null else analyticsObj.optInt("shares"),
                            watchTimeMinutes = if (analyticsObj.isNull("watchTimeMinutes")) null else analyticsObj.optDouble("watchTimeMinutes"),
                            subscribersGained = if (analyticsObj.isNull("subscribersGained")) null else analyticsObj.optInt("subscribersGained"),
                            averageViewDurationSeconds = null,
                            engagementRate = if (analyticsObj.isNull("engagementRate")) null else analyticsObj.optString("engagementRate")
                        )
                    } else null

                    ytList.add(
                        YouTubeVideoData(
                            videoId = ytObj.optString("videoId"),
                            title = ytObj.optString("title"),
                            authorName = if (ytObj.isNull("authorName")) null else ytObj.optString("authorName"),
                            channelUrl = if (ytObj.isNull("channelUrl")) null else ytObj.optString("channelUrl"),
                            description = if (ytObj.isNull("description")) null else ytObj.optString("description"),
                            duration = if (ytObj.isNull("duration")) null else ytObj.optString("duration"),
                            publishedAt = if (ytObj.isNull("publishedAt")) null else ytObj.optString("publishedAt"),
                            viewCount = if (ytObj.isNull("viewCount")) null else ytObj.optString("viewCount"),
                            likeCount = if (ytObj.isNull("likeCount")) null else ytObj.optString("likeCount"),
                            commentCount = if (ytObj.isNull("commentCount")) null else ytObj.optString("commentCount"),
                            thumbnail = if (ytObj.isNull("thumbnail")) null else ytObj.optString("thumbnail"),
                            analytics = ytAnalytics,
                            videoUrl = if (ytObj.isNull("videoUrl")) null else ytObj.optString("videoUrl"),
                            fetchedAt = if (ytObj.isNull("fetchedAt")) null else ytObj.optString("fetchedAt")
                        )
                    )
                }
            }

            // Parse performanceMilestones
            val milestoneList = mutableListOf<PerformanceMilestone>()
            val milestonesArray = obj.optJSONArray("performanceMilestones")
            if (milestonesArray != null) {
                for (j in 0 until milestonesArray.length()) {
                    val m = milestonesArray.optJSONObject(j) ?: continue
                    milestoneList.add(
                        PerformanceMilestone(
                            label = m.optString("label"),
                            hoursAfterPost = m.optInt("hoursAfterPost"),
                            views = if (m.isNull("views")) null else m.optInt("views"),
                            likes = if (m.isNull("likes")) null else m.optInt("likes"),
                            comments = if (m.isNull("comments")) null else m.optInt("comments"),
                            capturedAt = if (m.isNull("capturedAt")) null else m.optString("capturedAt")
                        )
                    )
                }
            }

            // Parse Instagram Data (Attempt to parse if present in JSON, even if removed from query)
            val igList = mutableListOf<InstagramPostData>()
            val igArray = obj.optJSONArray("ig")
            if (igArray != null) {
                for (j in 0 until igArray.length()) {
                    val igObj = igArray.optJSONObject(j) ?: continue
                    igList.add(
                        InstagramPostData(
                            postId = if (igObj.isNull("postId")) null else igObj.optString("postId"),
                            caption = if (igObj.isNull("caption")) null else igObj.optString("caption"),
                            likeCount = if (igObj.isNull("likeCount")) null else igObj.optInt("likeCount"),
                            commentCount = if (igObj.isNull("commentCount")) null else igObj.optInt("commentCount"),
                            viewCount = if (igObj.isNull("viewCount")) null else igObj.optInt("viewCount"),
                            mediaUrl = if (igObj.isNull("mediaUrl")) null else igObj.optString("mediaUrl"),
                            timestamp = if (igObj.isNull("timestamp")) null else igObj.optString("timestamp"),
                            fetchedAt = if (igObj.isNull("fetchedAt")) null else igObj.optString("fetchedAt")
                        )
                    )
                }
            }

            val performanceTargets = parsePerformanceTargets(obj.optJSONObject("performanceTargets"))
            val performanceTracking = parsePerformanceTracking(obj.optJSONObject("performanceTracking"))

            list.add(
                Collaboration(
                    id = obj.optString("id"),
                    campaignId = obj.optString("campaignId"),
                    brandId = obj.optString("brandId"),
                    influencerId = obj.optString("influencerId"),
                    status = obj.optString("status"),
                    message = if (obj.isNull("message")) null else obj.optString("message"),
                    pricing = pricingList,
                    initiatedBy = obj.optString("initiatedBy"),
                    createdAt = obj.optString("createdAt"),
                    updatedAt = obj.optString("updatedAt"),
                    campaign = campaign,
                    influencer = influencer,
                    brand = brand,
                    paymentStatus = if (obj.isNull("paymentStatus")) null else obj.optString("paymentStatus"),
                    razorpayOrderId = if (obj.isNull("razorpayOrderId")) null else obj.optString("razorpayOrderId"),
                    advancePaid = if (obj.isNull("advancePaid")) null else obj.optBoolean("advancePaid"),
                    finalPaid = if (obj.isNull("finalPaid")) null else obj.optBoolean("finalPaid"),
                    totalAmount = if (obj.isNull("totalAmount")) null else obj.optDouble("totalAmount"),
                    overallAnalytics = overallAnalytics,
                    platformAnalytics = if (platformAnalyticsList.isEmpty()) null else platformAnalyticsList,
                    yt = if (ytList.isEmpty()) null else ytList,
                    ig = if (igList.isEmpty()) null else igList,
                    performanceMilestones = if (milestoneList.isEmpty()) null else milestoneList,
                    totalViewsDelivered = if (obj.isNull("totalViewsDelivered")) null else obj.optInt("totalViewsDelivered"),
                    viewsGrowthSincePosting = if (obj.isNull("viewsGrowthSincePosting")) null else obj.optInt("viewsGrowthSincePosting"),
                    selectedInstagramProfileId = obj.optString("selectedInstagramProfileId").takeIf { it.isNotBlank() },
                    performanceTargets = performanceTargets,
                    performanceTracking = performanceTracking
                )
            )
        }
        return list
    }

    private fun parsePerformanceTargets(obj: JSONObject?): PerformanceTargets? {
        if (obj == null) return null
        fun d(key: String): Double? = if (obj.isNull(key)) null else obj.optDouble(key)
        return PerformanceTargets(
            targetViews = d("targetViews"),
            targetReach = d("targetReach"),
            targetEngagementRate = d("targetEngagementRate"),
            targetLikes = d("targetLikes"),
            targetComments = d("targetComments"),
            targetShares = d("targetShares"),
            targetSaves = d("targetSaves"),
            setAt = if (obj.isNull("setAt")) null else obj.optString("setAt"),
            setBy = if (obj.isNull("setBy")) null else obj.optString("setBy")
        )
    }

    private fun parseActualMetrics(obj: JSONObject?): ActualMetrics? {
        if (obj == null) return null
        fun d(key: String): Double? = if (obj.isNull(key)) null else obj.optDouble(key)
        return ActualMetrics(
            views = d("views"),
            reach = d("reach"),
            engagementRate = d("engagementRate"),
            likes = d("likes"),
            comments = d("comments"),
            shares = d("shares"),
            saves = d("saves")
        )
    }

    private fun parsePerformanceTracking(obj: JSONObject?): PerformanceTracking? {
        if (obj == null) return null

        val achievements = mutableListOf<PerformanceAchievement>()
        val achievementsArray = obj.optJSONArray("achievements")
        if (achievementsArray != null) {
            for (j in 0 until achievementsArray.length()) {
                val aObj = achievementsArray.optJSONObject(j) ?: continue
                achievements.add(
                    PerformanceAchievement(
                        metric = aObj.optString("metric"),
                        target = aObj.optDouble("target"),
                        actual = if (aObj.isNull("actual")) null else aObj.optDouble("actual"),
                        achievedPercent = if (aObj.isNull("achievedPercent")) null else aObj.optDouble("achievedPercent"),
                        status = aObj.optString("status"),
                        tracked = aObj.optBoolean("tracked")
                    )
                )
            }
        }

        val history = mutableListOf<PerformanceSnapshot>()
        val historyArray = obj.optJSONArray("history")
        if (historyArray != null) {
            for (j in 0 until historyArray.length()) {
                val hObj = historyArray.optJSONObject(j) ?: continue
                history.add(
                    PerformanceSnapshot(
                        capturedAt = hObj.optString("capturedAt"),
                        actual = parseActualMetrics(hObj.optJSONObject("actual")),
                        targets = null,
                        performanceScore = if (hObj.isNull("performanceScore")) null else hObj.optDouble("performanceScore"),
                        campaignOutcome = if (hObj.isNull("campaignOutcome")) null else hObj.optString("campaignOutcome"),
                        isFinal = if (hObj.isNull("isFinal")) null else hObj.optBoolean("isFinal")
                    )
                )
            }
        }

        return PerformanceTracking(
            achievements = achievements,
            overallAchievedPercent = if (obj.isNull("overallAchievedPercent")) null else obj.optDouble("overallAchievedPercent"),
            performanceScore = if (obj.isNull("performanceScore")) null else obj.optDouble("performanceScore"),
            campaignOutcome = if (obj.isNull("campaignOutcome")) null else obj.optString("campaignOutcome"),
            history = history
        )
    }

    fun fetchWishlist(token: String, force: Boolean = false) {
        if (!fetchThrottle.shouldFetch("wishlist", force)) return
        viewModelScope.launch {
            val query = """
                query GetWishlist {
                  getWishlist {
                    ... on Influencer {
                      id
                      email
                      name
                      role
                      profileCompleted
                      updatedAt
                      bio
                      location
                      gender
                      motherTongue
                      languagesKnown
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
                      averageRating
                      isVerified
                    }
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    val wishlistArray = data?.optJSONArray("getWishlist")
                    if (wishlistArray != null) {
                        val list = mutableListOf<InfluencerProfile>()
                        for (i in 0 until wishlistArray.length()) {
                            val obj = wishlistArray.optJSONObject(i)
                            if (obj != null && obj.has("id") && obj.optString("role") == "INFLUENCER") {
                                list.add(parseInfluencerProfile(obj))
                            }
                        }
                        _wishlistedInfluencers.postValue(list)
                    }
                } catch (e: Exception) {
                    Log.e("BrandViewModel", "Wishlist parsing error", e)
                }
            }
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
                val errors = jsonObject.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    val msg = errors.optJSONObject(0)?.optString("message") ?: "Action failed"
                    _error.postValue(msg)
                    onComplete(false)
                    return@launch
                }
                val data = jsonObject.optJSONObject("data")
                if (data != null && data.optJSONObject("updateCollaboration") != null) {
                    fetchCollaborations(token, force = true)
                    pushCollaborationStatusUpdate(collaborationId, status)
                    onComplete(true)
                } else {
                    _error.postValue("Action failed. Please try again.")
                    onComplete(false)
                }
            }.onFailure {
                _error.postValue(it.message ?: "Action failed")
                onComplete(false)
            }
            _loading.postValue(false)
        }
    }

    fun fetchMyCampaigns(token: String, force: Boolean = false) {
        if (!fetchThrottle.shouldFetch("myCampaigns", force)) return
        _loading.value = true
        viewModelScope.launch {
            val query = """
                query GetMyCampaigns {
                  getMyCampaigns {
                    id
                    title
                    description
                    budgetMin
                    budgetMax
                    startDate
                    endDate
                    status
                    createdAt
                    updatedAt
                    platforms {
                      platform
                      formats
                    }
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    val campaignsArray = data?.optJSONArray("getMyCampaigns")
                    if (campaignsArray != null) {
                        val list = mutableListOf<Campaign>()
                        for (i in 0 until campaignsArray.length()) {
                            val obj = campaignsArray.optJSONObject(i) ?: continue
                            val platformsArray = obj.optJSONArray("platforms")
                            val platforms = mutableListOf<Platform>()
                            if (platformsArray != null) {
                                for (j in 0 until platformsArray.length()) {
                                    val pObj = platformsArray.optJSONObject(j)
                                    if (pObj != null) {
                                        val formatsArray = pObj.optJSONArray("formats")
                                        val formatsList = mutableListOf<String>()
                                        if (formatsArray != null) {
                                            for (k in 0 until formatsArray.length()) {
                                                formatsList.add(formatsArray.getString(k))
                                            }
                                        }
                                        platforms.add(
                                            Platform(
                                                platform = pObj.optString("platform"),
                                                profileUrl = "",
                                                followers = null,
                                                avgViews = null,
                                                engagement = null,
                                                formats = formatsList,
                                                connected = null
                                            )
                                        )
                                    }
                                }
                            }

                            list.add(
                                Campaign(
                                    id = obj.optString("id"),
                                    brandId = null,
                                    title = obj.optString("title"),
                                    description = if (obj.isNull("description")) null else obj.optString("description"),
                                    budgetMin = if (obj.isNull("budgetMin")) null else obj.optInt("budgetMin"),
                                    budgetMax = if (obj.isNull("budgetMax")) null else obj.optInt("budgetMax"),
                                    startDate = if (obj.isNull("startDate")) null else obj.optString("startDate"),
                                    endDate = if (obj.isNull("endDate")) null else obj.optString("endDate"),
                                    status = if (obj.isNull("status")) null else obj.optString("status"),
                                    createdAt = if (obj.isNull("createdAt")) null else obj.optString("createdAt"),
                                    updatedAt = if (obj.isNull("updatedAt")) null else obj.optString("updatedAt"),
                                    platforms = platforms
                                )
                            )
                        }
                        _myCampaigns.postValue(list)
                    }
                } catch (e: Exception) {
                    Log.e("BrandViewModel", "Error fetching campaigns", e)
                }
            }
            _loading.postValue(false)
        }
    }

    fun inviteInfluencer(token: String, influencerId: String, campaignId: String, message: String, pricing: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        _loading.value = true
        viewModelScope.launch {
            val mutation = """
                mutation InviteInfluencer(${"$"}input: InviteInfluencerInput!) {
                  inviteInfluencer(input: ${"$"}input) {
                    id
                  }
                }
            """.trimIndent()

            val variables = mapOf(
                "input" to mapOf(
                    "influencerId" to influencerId,
                    "campaignId" to campaignId,
                    "message" to message,
                    "pricing" to pricing
                )
            )

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null && data.optJSONObject("inviteInfluencer") != null) {
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }.onFailure {
                onComplete(false)
            }
            _loading.postValue(false)
        }
    }

    fun toggleWishlist(influencer: InfluencerProfile, token: String) {
        viewModelScope.launch {
            val mutation = """
                mutation ToggleWishlist(${'$'}targetId: ID!) {
                  toggleWishlist(targetId: ${'$'}targetId)
                }
            """.trimIndent()

            val variables = mapOf("targetId" to influencer.id)
            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)

            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null) {
                    val isWishlistedResult = data.optBoolean("toggleWishlist")
                    
                    val currentList = _wishlistedInfluencers.value?.toMutableList() ?: mutableListOf()
                    if (isWishlistedResult) {
                        if (!currentList.any { it.id == influencer.id }) {
                            currentList.add(influencer)
                        }
                    } else {
                        currentList.removeAll { it.id == influencer.id }
                    }
                    _wishlistedInfluencers.postValue(currentList)
                }
            }.onFailure {
                Log.e("BrandViewModel", "Error toggling wishlist", it)
            }
        }
    }

    /**
     * Brand-only: sets/updates the internal performance targets for a collaboration
     * (setCollaborationTargets mutation, src/graphql/modules/collaboration/index.js).
     * Suspends and returns the Result directly (rather than fire-and-forget +
     * LiveData) so the calling dialog can show its own saving/error state without
     * a round trip through a shared LiveData — re-saving the same outcome twice in
     * a row wouldn't reliably re-trigger a LiveData-observing composable since
     * Compose skips state updates for structurally-equal values. Re-fetches
     * collaborations on success so performanceTargets/performanceTracking reflect
     * the change immediately.
     */
    suspend fun setCollaborationTargets(
        collaborationId: String,
        targetViews: Double?,
        targetReach: Double?,
        targetEngagementRate: Double?,
        targetLikes: Double?,
        targetComments: Double?,
        targetShares: Double?,
        targetSaves: Double?,
        token: String
    ): Result<Unit> {
        val mutation = """
            mutation SetCollaborationTargets(${'$'}collaborationId: ID!, ${'$'}targets: PerformanceTargetsInput!) {
              setCollaborationTargets(collaborationId: ${'$'}collaborationId, targets: ${'$'}targets) {
                id
              }
            }
        """.trimIndent()

        val targets = mutableMapOf<String, Any?>()
        targetViews?.let { targets["targetViews"] = it }
        targetReach?.let { targets["targetReach"] = it }
        targetEngagementRate?.let { targets["targetEngagementRate"] = it }
        targetLikes?.let { targets["targetLikes"] = it }
        targetComments?.let { targets["targetComments"] = it }
        targetShares?.let { targets["targetShares"] = it }
        targetSaves?.let { targets["targetSaves"] = it }

        val variables = mapOf(
            "collaborationId" to collaborationId,
            "targets" to targets
        )

        val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
        return result.fold(
            onSuccess = {
                fetchCollaborations(token, force = true)
                Result.success(Unit)
            },
            onFailure = {
                Log.e("BrandViewModel", "Error setting collaboration targets", it)
                Result.failure(it)
            }
        )
    }

    fun updateBrandProfile(
        token: String,
        name: String,
        brandCategory: String,
        subCategory: String,
        about: String,
        preferredPlatforms: List<String>,
        ageMin: Int?,
        ageMax: Int?,
        gender: String,
        profileUrl: String?,
        logoUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        _loading.value = true
        viewModelScope.launch {
            val mutation = """
                mutation SetupBrandProfile(${"$"}input: BrandProfileInput!) {
                  setupBrandProfile(input: ${"$"}input) {
                    id
                    name
                  }
                }
            """.trimIndent()

            val variables = mapOf(
                "input" to mapOf(
                    "name" to name,
                    "brandCategory" to mapOf("category" to brandCategory, "subCategory" to subCategory),
                    "about" to about,
                    "preferredPlatforms" to preferredPlatforms.map { mapOf("platform" to it) },
                    "targetAudience" to mapOf(
                        "ageMin" to ageMin,
                        "ageMax" to ageMax,
                        "gender" to gender,
                        "locations" to emptyList<String>()
                    ),
                    "profileUrl" to profileUrl,
                    "logoUrl" to logoUrl
                )
            )

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null && data.optJSONObject("setupBrandProfile") != null) {
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }.onFailure {
                onComplete(false)
            }
            _loading.postValue(false)
        }
    }

    fun createCollaborationPaymentOrder(token: String, collaborationId: String, paymentType: String, onComplete: (JSONObject?) -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val mutation = """
                mutation CreateCollaborationPaymentOrder(${'$'}collaborationId: ID!, ${'$'}paymentType: PaymentType!) {
                    createCollaborationPaymentOrder(collaborationId: ${'$'}collaborationId, paymentType: ${'$'}paymentType) {
                        success
                        collaborationId
                        razorpayOrderId
                        totalAmount
                    }
                }
            """.trimIndent()

            val variables = mapOf(
                "collaborationId" to collaborationId,
                "paymentType" to paymentType
            )

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")?.optJSONObject("createCollaborationPaymentOrder")
                if (data != null && data.optBoolean("success")) {
                    onComplete(data)
                } else {
                    val errorMsg = jsonObject.optJSONArray("errors")?.optJSONObject(0)?.optString("message") ?: "Failed to create order"
                    _error.postValue(errorMsg)
                    onComplete(null)
                }
            }.onFailure {
                _error.postValue(it.message ?: "Network error")
                onComplete(null)
            }
            _loading.postValue(false)
        }
    }

    fun verifyPayment(token: String, collaborationId: String, razorpayPaymentId: String, razorpaySignature: String, paymentType: String, onComplete: (Boolean) -> Unit) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val mutation = """
                mutation VerifyPayment(
                    ${'$'}collaborationId: ID!, 
                    ${'$'}razorpayPaymentId: String!, 
                    ${'$'}razorpaySignature: String!, 
                    ${'$'}paymentType: PaymentType!
                ) {
                    verifyPayment(
                        collaborationId: ${'$'}collaborationId, 
                        razorpayPaymentId: ${'$'}razorpayPaymentId, 
                        razorpaySignature: ${'$'}razorpaySignature, 
                        paymentType: ${'$'}paymentType
                    ) {
                        id
                        status
                        paymentStatus
                    }
                }
            """.trimIndent()

            val variables = mapOf(
                "collaborationId" to collaborationId,
                "razorpayPaymentId" to razorpayPaymentId,
                "razorpaySignature" to razorpaySignature,
                "paymentType" to paymentType
            )

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")?.optJSONObject("verifyPayment")
                if (data != null) {
                    fetchCollaborations(token, force = true)
                    onComplete(true)
                } else {
                    val errorMsg = jsonObject.optJSONArray("errors")?.optJSONObject(0)?.optString("message") ?: "Verification failed"
                    _error.postValue(errorMsg)
                    onComplete(false)
                }
            }.onFailure {
                _error.postValue(it.message ?: "Network error")
                onComplete(false)
            }
            _loading.postValue(false)
        }
    }
    private fun pushCollaborationStatusUpdate(collaborationId: String, status: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        FirebaseFirestore.getInstance()
            .collection("collaboration_updates")
            .document(collaborationId)
            .set(mapOf("status" to status, "lastUpdated" to System.currentTimeMillis(), "updatedBy" to uid))
    }

    fun searchInfluencers(
        token: String,
        filters: SearchFilters = SearchFilters(),
        page: Int = 0,
        append: Boolean = false
    ) {
        if (append) _loadingMore.value = true
        else { _loading.value = true; _error.value = null }

        viewModelScope.launch {
            val filtersArg = buildFiltersArg(filters)
            val filtersParam = if (filtersArg.isEmpty()) "" else "filters: $filtersArg, "
            val query = """
                query SearchInfluencers {
                  searchInfluencers(${filtersParam}page: $page, limit: 10) {
                    influencers {
                      id email name bio location gender motherTongue languagesKnown
                      availability logoUrl isVerified averageRating
                      tier totalFollowers engagementRate collaborationCount
                      categories { category subCategories }
                      platforms { platform profileUrl followers avgViews engagement formats connected }
                      strengths
                      audienceInsights {
                        topLocations { city country percentage }
                        genderSplit { male female }
                        ageGroups { range percentage }
                      }
                      instagramMetrics {
                        avgLikes avgComments avgViews postingFrequencyDays totalPostsAnalyzed engagementRate
                      }
                      aiInsights {
                        primaryNiche secondaryNiche brandSuitability strengths weaknesses
                        contentStyle tone audienceInterests topics professionalSummary aiSummary
                      }
                    }
                    total page hasMore
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val payload = jsonObject.optJSONObject("data")?.optJSONObject("searchInfluencers")
                    if (payload != null) {
                        val arr = payload.optJSONArray("influencers")
                        val newList = if (arr != null) parseInfluencers(arr) else emptyList()
                        if (append) {
                            _searchResults.postValue((_searchResults.value ?: emptyList()) + newList)
                        } else {
                            _searchResults.postValue(newList)
                        }
                        _searchMeta.postValue(
                            SearchMeta(
                                total = payload.optInt("total"),
                                page = payload.optInt("page"),
                                hasMore = payload.optBoolean("hasMore")
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e("BrandViewModel", "searchInfluencers parse error", e)
                }
            }.onFailure { _error.postValue(it.message) }

            if (append) _loadingMore.postValue(false)
            else _loading.postValue(false)
        }
    }

    private fun buildFiltersArg(f: SearchFilters): String {
        val parts = mutableListOf<String>()
        if (f.query.isNotBlank()) parts.add("query: \"${f.query.replace("\"", "\\\"")}\"")
        val platforms = f.platforms.filter { it != "All" }
        if (platforms.isNotEmpty()) parts.add("platforms: [${platforms.joinToString(",") { "\"$it\"" }}]")
        val categories = f.categories.filter { it != "All" }
        if (categories.isNotEmpty()) parts.add("categories: [${categories.joinToString(",") { "\"$it\"" }}]")
        val followerRanges = f.followerRange.filter { it != "All" }
        if (followerRanges.isNotEmpty()) parts.add("followerRanges: [${followerRanges.joinToString(",") { "\"$it\"" }}]")
        val genders = f.gender.filter { it != "All" }
        if (genders.isNotEmpty()) parts.add("genders: [${genders.joinToString(",") { "\"$it\"" }}]")
        val motherTongues = f.motherTongue.filter { it != "All" }
        if (motherTongues.isNotEmpty()) parts.add("motherTongues: [${motherTongues.joinToString(",") { "\"$it\"" }}]")
        val langs = f.languagesKnown.filter { it != "All" }
        if (langs.isNotEmpty()) parts.add("languagesKnown: [${langs.joinToString(",") { "\"$it\"" }}]")
        val locations = f.location.filter { it != "All" }
        if (locations.isNotEmpty()) parts.add("locations: [${locations.joinToString(",") { "\"$it\"" }}]")
        return if (parts.isEmpty()) "" else "{ ${parts.joinToString(", ")} }"
    }
}

data class SearchFilters(
    val query: String = "",
    val platforms: Set<String> = setOf("All"),
    val categories: Set<String> = setOf("All"),
    val followerRange: Set<String> = setOf("All"),
    val gender: Set<String> = setOf("All"),
    val motherTongue: Set<String> = setOf("All"),
    val languagesKnown: Set<String> = setOf("All"),
    val location: Set<String> = setOf("All")
)

data class SearchMeta(
    val total: Int = 0,
    val page: Int = 0,
    val hasMore: Boolean = false
)
