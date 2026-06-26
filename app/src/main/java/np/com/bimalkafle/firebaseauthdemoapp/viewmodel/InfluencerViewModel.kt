package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.model.*
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import np.com.bimalkafle.firebaseauthdemoapp.network.GraphQLClient
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager
import org.json.JSONArray
import org.json.JSONObject

class InfluencerViewModel : ViewModel() {

    private val _influencerProfile = MutableLiveData<InfluencerProfile?>()
    val influencerProfile: LiveData<InfluencerProfile?> = _influencerProfile

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun clearError() { _error.value = null }

    // ── Active Instagram Profile (account switcher) ───────────────────────────

    private val _activeInstagramProfileId = MutableLiveData<String?>(null)
    val activeInstagramProfileId: LiveData<String?> = _activeInstagramProfileId

    val activeInstagramProfile: LiveData<InstagramProfile?> = MediatorLiveData<InstagramProfile?>().also { med ->
        fun update() {
            val profiles = _influencerProfile.value?.instagramProfiles ?: emptyList()
            val activeId = _activeInstagramProfileId.value
            med.value = if (activeId != null) profiles.find { it.id == activeId }
                        else profiles.firstOrNull { it.isDefault } ?: profiles.firstOrNull()
        }
        med.addSource(_influencerProfile) { update() }
        med.addSource(_activeInstagramProfileId) { update() }
    }

    fun loadActiveProfile(context: Context, uid: String) {
        val saved = PrefsManager(context).getActiveInstagramProfileId(uid)
        _activeInstagramProfileId.value = saved
    }

    fun switchProfile(context: Context, uid: String, profileId: String?) {
        PrefsManager(context).saveActiveInstagramProfileId(uid, profileId)
        _activeInstagramProfileId.value = profileId
    }

    // ── Instagram Profile Management ──────────────────────────────────────────

    fun addInstagramProfile(token: String, profileUrl: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = BackendRepository.addInstagramProfile(profileUrl, token)
            result.onSuccess { profileJson ->
                val current = _influencerProfile.value ?: run { onComplete(false, null); return@launch }
                val newProfile = jsonToInstagramProfile(profileJson)
                val updated = (current.instagramProfiles ?: emptyList()) + newProfile
                _influencerProfile.postValue(current.copy(instagramProfiles = updated))
                onComplete(true, null)
            }.onFailure {
                onComplete(false, it.message)
            }
        }
    }

    fun removeInstagramProfile(token: String, profileId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = BackendRepository.removeInstagramProfile(profileId, token)
            result.onSuccess {
                val current = _influencerProfile.value ?: run { onComplete(false); return@launch }
                val updated = current.instagramProfiles?.filter { it.id != profileId }
                // If we removed the default, mark the first remaining as default
                val reIndexed = if (updated != null && updated.isNotEmpty() && updated.none { it.isDefault }) {
                    listOf(updated[0].copy(isDefault = true)) + updated.drop(1)
                } else updated
                _influencerProfile.postValue(current.copy(instagramProfiles = reIndexed))
                onComplete(true)
            }.onFailure { onComplete(false) }
        }
    }

    fun setDefaultInstagramProfile(token: String, profileId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = BackendRepository.setDefaultInstagramProfile(profileId, token)
            result.onSuccess {
                val current = _influencerProfile.value ?: run { onComplete(false); return@launch }
                val updated = current.instagramProfiles?.map { it.copy(isDefault = it.id == profileId) }
                _influencerProfile.postValue(current.copy(instagramProfiles = updated))
                onComplete(true)
            }.onFailure { onComplete(false) }
        }
    }

    fun refreshInstagramProfileMetrics(token: String, profileId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = BackendRepository.refreshInstagramProfileMetrics(profileId, token)
            result.onSuccess { profileJson ->
                val current = _influencerProfile.value ?: run { onComplete(false, null); return@launch }
                val refreshed = jsonToInstagramProfile(profileJson)
                val updated = current.instagramProfiles?.map { if (it.id == profileId) refreshed else it }
                _influencerProfile.postValue(current.copy(instagramProfiles = updated))
                onComplete(true, null)
            }.onFailure {
                onComplete(false, it.message)
            }
        }
    }

    private fun jsonToInstagramProfile(json: org.json.JSONObject): InstagramProfile {
        val mObj = json.optJSONObject("metrics")
        val metrics = if (mObj != null) InstagramMetrics(
            avgComments = if (mObj.has("avgComments")) mObj.optDouble("avgComments").toFloat() else null,
            avgLikes = if (mObj.has("avgLikes")) mObj.optDouble("avgLikes").toFloat() else null,
            avgViews = if (mObj.has("avgViews")) mObj.optDouble("avgViews").toFloat() else null,
            postingFrequencyDays = if (mObj.has("postingFrequencyDays")) mObj.optDouble("postingFrequencyDays").toFloat() else null,
            totalPostsAnalyzed = if (mObj.has("totalPostsAnalyzed")) mObj.optInt("totalPostsAnalyzed") else null,
            updatedAt = mObj.optString("updatedAt")
        ) else null
        return InstagramProfile(
            id = json.optString("id"),
            profileUrl = json.optString("profileUrl"),
            username = json.optString("username"),
            followers = if (json.has("followers") && !json.isNull("followers")) json.optInt("followers") else null,
            isDefault = json.optBoolean("isDefault", false),
            connectedAt = json.optString("connectedAt"),
            metrics = metrics
        )
    }

    private val _brands = MutableLiveData<List<Brand>>()
    val brands: LiveData<List<Brand>> = _brands

    // Recommendation Streams
    private val _overallRecommendedBrands = MutableLiveData<List<Brand>>()
    val overallRecommendedBrands: LiveData<List<Brand>> = _overallRecommendedBrands

    private val _youtubeRecommendedBrands = MutableLiveData<List<Brand>>()
    val youtubeRecommendedBrands: LiveData<List<Brand>> = _youtubeRecommendedBrands

    private val _instagramRecommendedBrands = MutableLiveData<List<Brand>>()
    val instagramRecommendedBrands: LiveData<List<Brand>> = _instagramRecommendedBrands

    // See FetchThrottle.kt — skips a refetch (and its loading flash) if the same
    // data was fetched within the last minute, so switching tabs doesn't discard
    // good cached data. Pass force = true (e.g. pull-to-refresh) to bypass it.
    private val fetchThrottle = FetchThrottle()

    fun fetchRecommendedBrands(token: String, allBrands: List<Brand>? = null) {
        viewModelScope.launch {
            val query = """
                query GetBrandRecommendations {
                  getOverallRecommendedBrands(topN: 10) { id score }
                  getTopYoutubeRecommendedBrands(topN: 10) { id score }
                  getTopInstagramRecommendedBrands(topN: 10) { id score }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null) {
                    val overallRecs = parseRecs(data.optJSONArray("getOverallRecommendedBrands"))
                    val youtubeRecs = parseRecs(data.optJSONArray("getTopYoutubeRecommendedBrands"))
                    val instagramRecs = parseRecs(data.optJSONArray("getTopInstagramRecommendedBrands"))

                    val availableBrands = allBrands ?: _brands.value ?: emptyList()

                    _overallRecommendedBrands.postValue(sortBrands(availableBrands, overallRecs))
                    _youtubeRecommendedBrands.postValue(sortBrands(availableBrands, youtubeRecs))
                    _instagramRecommendedBrands.postValue(sortBrands(availableBrands, instagramRecs))
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Failed to fetch recommendations", it)
            }
        }
    }

    private fun parseRecs(array: JSONArray?): List<Pair<String, Double>> {
        val list = mutableListOf<Pair<String, Double>>()
        if (array != null) {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                list.add(obj.optString("id") to obj.optDouble("score"))
            }
        }
        return list
    }

    private fun sortBrands(all: List<Brand>, recs: List<Pair<String, Double>>): List<Brand> {
        val idToScore = recs.toMap()
        return all.filter { idToScore.containsKey(it.id) }
            .sortedByDescending { idToScore[it.id] }
    }

    fun fetchInfluencerDetails(token: String, force: Boolean = false) {
        if (!fetchThrottle.shouldFetch("influencerDetails", force)) return
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
                      isVerified
                      averageRating
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
                        lastSynced
                      }
                      instagramMetrics {
                        avgComments
                        avgLikes
                        avgViews
                        postingFrequencyDays
                        totalPostsAnalyzed
                        updatedAt
                      }
                      instagramProfiles {
                        id
                        profileUrl
                        username
                        followers
                        isDefault
                        connectedAt
                        metrics {
                          avgLikes
                          avgComments
                          avgViews
                          postingFrequencyDays
                          totalPostsAnalyzed
                          updatedAt
                        }
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
                            val influencer = parseInfluencer(meObj)
                            _influencerProfile.postValue(influencer)
                        } else {
                            _influencerProfile.postValue(null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InfluencerViewModel", "Parsing error", e)
                }
            }
            _loading.postValue(false)
        }
    }

    fun fetchInfluencerById(influencerId: String, token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetInfluencerById(${'$'}id: ID!) {
                  getInfluencerById(id: ${'$'}id) {
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
                    isVerified
                    averageRating
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
                      lastSynced
                    }
                    instagramMetrics {
                      avgComments
                      avgLikes
                      avgViews
                      postingFrequencyDays
                      totalPostsAnalyzed
                      updatedAt
                    }
                    instagramProfiles {
                      id
                      profileUrl
                      username
                      followers
                      isDefault
                      connectedAt
                      metrics {
                        avgLikes
                        avgComments
                        avgViews
                        postingFrequencyDays
                        totalPostsAnalyzed
                        updatedAt
                      }
                    }
                  }
                }
            """.trimIndent()

            val variables = mapOf("id" to influencerId)
            val result = GraphQLClient.query(query = query, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    if (data != null) {
                        val influencerObj = data.optJSONObject("getInfluencerById")
                        if (influencerObj != null) {
                            val influencer = parseInfluencer(influencerObj)
                            _influencerProfile.postValue(influencer)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InfluencerViewModel", "Parsing error", e)
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue(it.message)
            }
            _loading.postValue(false)
        }
    }

    fun fetchBrands(token: String, force: Boolean = false) {
        if (!fetchThrottle.shouldFetch("brands", force)) return
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
                    profileUrl
                    logoUrl
                    averageRating
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
                            fetchRecommendedBrands(token, list)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("InfluencerViewModel", "Parsing error", e)
                }
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
                    val catObj = categoriesArray.optJSONObject(j)
                    val subCatsArray = catObj.optJSONArray("subCategories")
                    val subCats = mutableListOf<String>()
                    if (subCatsArray != null) {
                        for (k in 0 until subCatsArray.length()) { subCats.add(subCatsArray.getString(k)) }
                    }
                    brandCategories.add(BrandCategory(catObj.optString("category"), subCats))
                }
            }
            list.add(Brand(
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
                averageRating = if(obj.has("averageRating")) obj.optDouble("averageRating") else null
            ))
        }
        return list
    }

    private fun parseInfluencer(obj: JSONObject): InfluencerProfile {
        val categoriesList = mutableListOf<Category>()
        val categoriesArray = obj.optJSONArray("categories")
        if (categoriesArray != null) {
            for (i in 0 until categoriesArray.length()) {
                val catObj = categoriesArray.optJSONObject(i) ?: continue
                val subCatsArray = catObj.optJSONArray("subCategories")
                val subCats = mutableListOf<String>()
                if (subCatsArray != null) {
                    for (j in 0 until subCatsArray.length()) { subCats.add(subCatsArray.getString(j)) }
                }
                categoriesList.add(Category(catObj.optString("category"), subCats))
            }
        }

        val platformsList = mutableListOf<Platform>()
        val platformsArray = obj.optJSONArray("platforms")
        if (platformsArray != null) {
            for (i in 0 until platformsArray.length()) {
                val platObj = platformsArray.optJSONObject(i) ?: continue
                val formatsArray = platObj.optJSONArray("formats")
                val formatsList = mutableListOf<String>()
                if (formatsArray != null) {
                    for (j in 0 until formatsArray.length()) { formatsList.add(formatsArray.getString(j)) }
                }
                platformsList.add(Platform(
                    platform = platObj.optString("platform"),
                    profileUrl = platObj.optString("profileUrl"),
                    followers = if (platObj.has("followers")) platObj.optInt("followers") else null,
                    avgViews = if (platObj.has("avgViews")) platObj.optInt("avgViews") else null,
                    engagement = if (platObj.has("engagement")) platObj.optDouble("engagement").toFloat() else null,
                    formats = formatsList,
                    connected = if (platObj.has("connected")) platObj.optBoolean("connected") else null
                ))
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
                val pObj = pricingArray.optJSONObject(i) ?: continue
                pricingList.add(PricingInfo(
                    platform = pObj.optString("platform"),
                    deliverable = pObj.optString("deliverable"),
                    price = pObj.optInt("price"),
                    currency = pObj.optString("currency")
                ))
            }
        }

        val ytInsightsObj = obj.optJSONObject("youtubeInsights")
        val youtubeInsights = if (ytInsightsObj != null) {
            val demographicsArray = ytInsightsObj.optJSONArray("demographics")
            val demographics = mutableListOf<YoutubeDemographics>()
            if (demographicsArray != null) {
                for (i in 0 until demographicsArray.length()) {
                    val dObj = demographicsArray.optJSONObject(i) ?: continue
                    demographics.add(YoutubeDemographics(
                        ageGroup = dObj.optString("ageGroup"),
                        gender = dObj.optString("gender"),
                        percentage = dObj.optDouble("percentage").toFloat()
                    ))
                }
            }
            YouTubeInsights(
                channelId = ytInsightsObj.optString("channelId"),
                title = ytInsightsObj.optString("title"),
                description = ytInsightsObj.optString("description"),
                subscribers = if (ytInsightsObj.has("subscribers")) ytInsightsObj.optInt("subscribers") else null,
                totalViews = if (ytInsightsObj.has("totalViews")) ytInsightsObj.optLong("totalViews") else null,
                totalVideos = if (ytInsightsObj.has("totalVideos")) ytInsightsObj.optInt("totalVideos") else null,
                demographics = demographics,
                revenue = null,
                lastSynced = ytInsightsObj.optString("lastSynced")
            )
        } else null

        val instaMetricsObj = obj.optJSONObject("instagramMetrics")
        val instagramMetrics = if (instaMetricsObj != null) {
            InstagramMetrics(
                avgComments = if (instaMetricsObj.has("avgComments")) instaMetricsObj.optDouble("avgComments").toFloat() else null,
                avgLikes = if (instaMetricsObj.has("avgLikes")) instaMetricsObj.optDouble("avgLikes").toFloat() else null,
                avgViews = if (instaMetricsObj.has("avgViews")) instaMetricsObj.optDouble("avgViews").toFloat() else null,
                postingFrequencyDays = if (instaMetricsObj.has("postingFrequencyDays")) instaMetricsObj.optDouble("postingFrequencyDays").toFloat() else null,
                totalPostsAnalyzed = if (instaMetricsObj.has("totalPostsAnalyzed")) instaMetricsObj.optInt("totalPostsAnalyzed") else null,
                updatedAt = instaMetricsObj.optString("updatedAt")
            )
        } else null

        val igProfilesList = mutableListOf<InstagramProfile>()
        val igProfilesArray = obj.optJSONArray("instagramProfiles")
        if (igProfilesArray != null) {
            for (i in 0 until igProfilesArray.length()) {
                val p = igProfilesArray.optJSONObject(i) ?: continue
                val mObj = p.optJSONObject("metrics")
                val metrics = if (mObj != null) InstagramMetrics(
                    avgComments = if (mObj.has("avgComments")) mObj.optDouble("avgComments").toFloat() else null,
                    avgLikes = if (mObj.has("avgLikes")) mObj.optDouble("avgLikes").toFloat() else null,
                    avgViews = if (mObj.has("avgViews")) mObj.optDouble("avgViews").toFloat() else null,
                    postingFrequencyDays = if (mObj.has("postingFrequencyDays")) mObj.optDouble("postingFrequencyDays").toFloat() else null,
                    totalPostsAnalyzed = if (mObj.has("totalPostsAnalyzed")) mObj.optInt("totalPostsAnalyzed") else null,
                    updatedAt = mObj.optString("updatedAt")
                ) else null
                igProfilesList.add(InstagramProfile(
                    id = p.optString("id"),
                    profileUrl = p.optString("profileUrl"),
                    username = p.optString("username"),
                    followers = if (p.has("followers")) p.optInt("followers") else null,
                    isDefault = p.optBoolean("isDefault", false),
                    connectedAt = p.optString("connectedAt"),
                    metrics = metrics
                ))
            }
        }

        return InfluencerProfile(
            id = obj.optString("id"),
            email = obj.optString("email"),
            name = obj.optString("name"),
            role = obj.optString("role"),
            profileCompleted = if (obj.has("profileCompleted")) obj.optBoolean("profileCompleted") else null,
            updatedAt = obj.optString("updatedAt"),
            bio = obj.optString("bio"),
            location = obj.optString("location"),
            categories = categoriesList,
            platforms = platformsList,
            audienceInsights = null,
            strengths = strengthsList,
            pricing = pricingList,
            availability = if (obj.has("availability")) obj.optBoolean("availability") else null,
            logoUrl = obj.optString("logoUrl"),
            isVerified = if (obj.has("isVerified")) obj.optBoolean("isVerified") else null,
            averageRating = if (obj.has("averageRating")) obj.optDouble("averageRating").toFloat() else null,
            youtubeInsights = youtubeInsights,
            instagramMetrics = instagramMetrics,
            instagramProfiles = igProfilesList.ifEmpty { null }
        )
    }

    private val _collaborations = MutableLiveData<List<Collaboration>>()
    val collaborations: LiveData<List<Collaboration>> = _collaborations

    // Shows only collaborations belonging to the active Instagram profile.
    // Collaborations with no selectedInstagramProfileId are always visible (non-Instagram / legacy).
    val filteredCollaborations: LiveData<List<Collaboration>> = MediatorLiveData<List<Collaboration>>().also { med ->
        fun update() {
            val all = _collaborations.value ?: emptyList()
            val activeId = _activeInstagramProfileId.value
            med.value = if (activeId == null) all
                        else all.filter { it.selectedInstagramProfileId == null || it.selectedInstagramProfileId == activeId }
        }
        med.addSource(_collaborations) { update() }
        med.addSource(_activeInstagramProfileId) { update() }
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
                    pricing { platform deliverable price currency }
                    initiatedBy
                    createdAt
                    updatedAt
                    paymentStatus
                    totalAmount
                    brand { id email name role about profileUrl logoUrl }
                    campaign { id brandId title description budgetMin budgetMax startDate endDate status createdAt updatedAt }
                    influencer { name bio logoUrl updatedAt }
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
                        _collaborations.postValue(parseCollaborations(collaborationsArray))
                    }
                } catch (e: Exception) {
                    Log.e("InfluencerViewModel", "Collab parsing error", e)
                }
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
                    bio = influencerObj.optString("bio"),
                    logoUrl = influencerObj.optString("logoUrl"),
                    updatedAt = influencerObj.optString("updatedAt")
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
                    about = brandObj.optString("about", null),
                    profileUrl = brandObj.optString("profileUrl", null),
                    logoUrl = brandObj.optString("logoUrl", null),
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
                    brand = brand,
                    paymentStatus = obj.optString("paymentStatus"),
                    razorpayOrderId = obj.optString("razorpayOrderId"),
                    advancePaid = if (obj.isNull("advancePaid")) null else obj.optBoolean("advancePaid"),
                    finalPaid = if (obj.isNull("finalPaid")) null else obj.optBoolean("finalPaid"),
                    totalAmount = if (obj.isNull("totalAmount")) null else obj.optDouble("totalAmount"),
                    totalViewsDelivered = if (obj.isNull("totalViewsDelivered")) null else obj.optInt("totalViewsDelivered"),
                    viewsGrowthSincePosting = if (obj.isNull("viewsGrowthSincePosting")) null else obj.optInt("viewsGrowthSincePosting"),
                    selectedInstagramProfileId = obj.optString("selectedInstagramProfileId").takeIf { it.isNotBlank() }
                )
            )
        }
        return list
    }

    fun applyToCampaign(
        token: String,
        campaignId: String,
        message: String,
        pricing: List<Map<String, Any>>,
        selectedInstagramProfileId: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
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

            val inputMap = mutableMapOf<String, Any>(
                "campaignId" to campaignId,
                "message" to message,
                "pricing" to pricing
            )
            if (selectedInstagramProfileId != null) inputMap["selectedInstagramProfileId"] = selectedInstagramProfileId

            val variables = mapOf("input" to inputMap)

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                val applyResult = data?.optJSONObject("applyToCampaign")
                if (applyResult != null) {
                    onComplete(true)
                } else {
                    val errors = jsonObject.optJSONArray("errors")
                    val firstError = errors?.optJSONObject(0)?.optString("message") ?: "Failed to apply"
                    _error.postValue(firstError)
                    onComplete(false)
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Apply error", it)
                _error.postValue(it.message)
                onComplete(false)
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
                mutation UpdateInfluencer(${'$'}input: UpdateInfluencerInput!) {
                  updateInfluencer(input: ${'$'}input) {
                    id
                    name
                  }
                }
            """.trimIndent()

            val categoriesInput = categories.map { mapOf("category" to it.category, "subCategories" to it.subCategories) }
            val platformsInput = platforms.map { mapOf(
                "platform" to it.platform,
                "profileUrl" to it.profileUrl,
                "followers" to it.followers,
                "avgViews" to it.avgViews,
                "engagement" to it.engagement,
                "formats" to it.formats,
                "connected" to it.connected
            ) }
            val pricingInput = pricing.map { mapOf(
                "platform" to it.platform,
                "deliverable" to it.deliverable,
                "price" to it.price,
                "currency" to it.currency
            ) }

            val input = mutableMapOf<String, Any>(
                "name" to name,
                "bio" to bio,
                "location" to location,
                "logoUrl" to logoUrl,
                "categories" to categoriesInput,
                "platforms" to platformsInput,
                "pricing" to pricingInput,
                "availability" to availability
            )

            val result = GraphQLClient.query(query = mutation, variables = mapOf("input" to input), token = token)
            result.onSuccess { onComplete(true) }.onFailure { 
                _error.postValue(it.message)
                onComplete(false) 
            }
            _loading.postValue(false)
        }
    }

    fun updateCollaborationStatus(
        token: String,
        collaborationId: String,
        status: String,
        message: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val mutation = """
                mutation UpdateCollaborationStatus(${'$'}input: UpdateCollaborationInput!) {
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
    private fun pushCollaborationStatusUpdate(collaborationId: String, status: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
        FirebaseFirestore.getInstance()
            .collection("collaboration_updates")
            .document(collaborationId)
            .set(mapOf("status" to status, "lastUpdated" to System.currentTimeMillis(), "updatedBy" to uid))
    }
}
