package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.model.*
import np.com.bimalkafle.firebaseauthdemoapp.network.GraphQLClient
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CampaignViewModel : ViewModel() {

    // Screen 1 Fields
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    
    // Multiselect Categories
    var selectedCategories by mutableStateOf(setOf<String>())
    var selectedSubCategories by mutableStateOf(mapOf<String, Set<String>>())
    
    var selectedPlatforms by mutableStateOf(setOf<String>())
    var platformFormats by mutableStateOf(mapOf<String, Set<String>>())
    var startDate by mutableStateOf<Date?>(null)
    var endDate by mutableStateOf<Date?>(null)

    // Screen 2 Fields
    var budgetMin by mutableStateOf(50)
    var budgetMax by mutableStateOf(200)
    var selectedLocations by mutableStateOf(setOf<String>())
    var ageMin by mutableStateOf(18)
    var ageMax by mutableStateOf(60)
    var selectedGender by mutableStateOf("Any")

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _createCampaignSuccess = MutableLiveData<Boolean>()
    val createCampaignSuccess: LiveData<Boolean> = _createCampaignSuccess

    private val _createdCampaign = MutableLiveData<CampaignDetail?>()
    val createdCampaign: LiveData<CampaignDetail?> = _createdCampaign

    private val _campaigns = MutableLiveData<List<CampaignDetail>>()
    val campaigns: LiveData<List<CampaignDetail>> = _campaigns

    private val _campaign = MutableLiveData<CampaignDetail?>()
    val campaign: LiveData<CampaignDetail?> = _campaign

    private val _wishlistedCampaigns = MutableLiveData<List<CampaignDetail>>(emptyList())
    val wishlistedCampaigns: LiveData<List<CampaignDetail>> = _wishlistedCampaigns

    // Recommendation Streams
    private val _overallRecommendedCampaigns = MutableLiveData<List<CampaignDetail>>()
    val overallRecommendedCampaigns: LiveData<List<CampaignDetail>> = _overallRecommendedCampaigns

    private val _youtubeRecommendedCampaigns = MutableLiveData<List<CampaignDetail>>()
    val youtubeRecommendedCampaigns: LiveData<List<CampaignDetail>> = _youtubeRecommendedCampaigns

    private val _instagramRecommendedCampaigns = MutableLiveData<List<CampaignDetail>>()
    val instagramRecommendedCampaigns: LiveData<List<CampaignDetail>> = _instagramRecommendedCampaigns

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun fetchRecommendedCampaigns(token: String, allCampaigns: List<CampaignDetail>? = null) {
        viewModelScope.launch {
            val query = """
                query GetCampaignRecommendations {
                  getRecommendedCampaigns(topN: 30) { id score }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null) {
                    val recs = parseRecs(data.optJSONArray("getRecommendedCampaigns"))
                    val availableCampaigns = allCampaigns ?: _campaigns.value ?: emptyList()

                    val sortedList = sortCampaigns(availableCampaigns, recs)

                    _overallRecommendedCampaigns.postValue(sortedList)
                    _youtubeRecommendedCampaigns.postValue(sortedList.filter { it.platforms?.any { p -> p.platform.equals("YouTube", true) } == true })
                    _instagramRecommendedCampaigns.postValue(sortedList.filter { it.platforms?.any { p -> p.platform.equals("Instagram", true) } == true })
                }
            }.onFailure {
                Log.e("CampaignViewModel", "Failed to fetch recommendations", it)
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

    private fun sortCampaigns(all: List<CampaignDetail>, recs: List<Pair<String, Double>>): List<CampaignDetail> {
        val idToScore = recs.toMap()
        return all.filter { idToScore.containsKey(it.id) }
            .sortedByDescending { idToScore[it.id] }
    }

    fun toggleWishlist(campaign: CampaignDetail, token: String) {
        viewModelScope.launch {
            val mutation = """
                mutation ToggleWishlist(${'$'}targetId: ID!) {
                  toggleWishlist(targetId: ${'$'}targetId)
                }
            """.trimIndent()

            val variables = mapOf("targetId" to campaign.id)
            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)

            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                if (data != null) {
                    val isWishlistedResult = data.optBoolean("toggleWishlist")
                    
                    val currentList = _wishlistedCampaigns.value?.toMutableList() ?: mutableListOf()
                    if (isWishlistedResult) {
                        if (!currentList.any { it.id == campaign.id }) {
                            currentList.add(campaign)
                        }
                    } else {
                        currentList.removeAll { it.id == campaign.id }
                    }
                    _wishlistedCampaigns.postValue(currentList)
                } else {
                    val errors = jsonObject.optJSONArray("errors")
                    val msg = errors?.optJSONObject(0)?.optString("message") ?: "Unknown error"
                    Log.e("CampaignViewModel", "Toggle error: ${'$'}msg")
                    _error.postValue(msg)
                }
            }.onFailure {
                Log.e("CampaignViewModel", "Error toggling wishlist", it)
                _error.postValue(it.message ?: "Error toggling wishlist")
            }
        }
    }

    fun isWishlisted(campaignId: String): Boolean {
        return _wishlistedCampaigns.value?.any { it.id == campaignId } ?: false
    }

    fun fetchWishlist(token: String) {
        viewModelScope.launch {
            val query = """
                query GetWishlist {
                  getWishlist {
                    ... on Campaign {
                      id
                      title
                      description
                      status
                      createdAt
                      budgetMin
                      budgetMax
                      startDate
                      endDate
                      categories {
                        category
                        subCategories
                      }
                      platforms {
                        platform
                        formats
                      }
                      targetAudience {
                        ageMin
                        ageMax
                        gender
                        locations
                      }
                      brand {
                        id
                        name
                        about
                        profileUrl
                        logoUrl
                        isVerified
                        averageRating
                        brandCategories {
                          category
                          subCategories
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
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
                try {
                    val data = jsonObject.optJSONObject("data")
                    val wishlistArray = data?.optJSONArray("getWishlist")
                    if (wishlistArray != null) {
                        val list = mutableListOf<CampaignDetail>()
                        for (i in 0 until wishlistArray.length()) {
                            val obj = wishlistArray.optJSONObject(i)
                            if (obj != null && obj.has("id") && obj.has("title")) {
                                list.add(parseCampaignDetail(obj))
                            }
                        }
                        _wishlistedCampaigns.postValue(list)
                    } else {
                         _wishlistedCampaigns.postValue(emptyList())
                    }
                } catch (e: Exception) {
                    Log.e("CampaignViewModel", "Wishlist parsing error", e)
                    _error.postValue("Error processing wishlist: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("CampaignViewModel", "Wishlist network error", it)
                 _error.postValue("Network error fetching wishlist: ${'$'}{it.message}")
            }
        }
    }

    fun fetchCampaigns(token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            fetchWishlist(token)

            val query = """
                query GetCampaigns {
                  getCampaigns {
                    id
                    title
                    description
                    status
                    createdAt
                    budgetMin
                    budgetMax
                    startDate
                    endDate
                    categories {
                      category
                      subCategories
                    }
                    platforms {
                      platform
                      formats
                    }
                    targetAudience {
                      ageMin
                      ageMax
                      gender
                      locations
                    }
                    brand {
                      id
                      name
                      email
                      role
                      profileCompleted
                      updatedAt
                      about
                      profileUrl
                      logoUrl
                      isVerified
                      averageRating
                      brandCategories {
                        category
                        subCategories
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
                val errors = jsonObject.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    val errorMsg = errors.getJSONObject(0).optString("message", "Unknown GraphQL Error")
                    Log.e("CampaignViewModel", "GraphQL Error: ${'$'}errorMsg")
                    _error.postValue(errorMsg)
                    _loading.postValue(false)
                    return@onSuccess
                }

                try {
                    val data = jsonObject.optJSONObject("data")
                    val campaignsArray = data?.optJSONArray("getCampaigns")
                    if (campaignsArray != null) {
                        val list = mutableListOf<CampaignDetail>()
                        for (i in 0 until campaignsArray.length()) {
                            val obj = campaignsArray.optJSONObject(i)
                            if (obj != null) {
                                list.add(parseCampaignDetail(obj))
                            }
                        }
                        _campaigns.postValue(list)
                        fetchRecommendedCampaigns(token, list)
                    } else {
                        _campaigns.postValue(emptyList())
                    }
                } catch (e: Exception) {
                    Log.e("CampaignViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
                _loading.postValue(false)
            }.onFailure {
                Log.e("CampaignViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
                _loading.postValue(false)
            }
        }
    }

    fun fetchCampaignById(id: String, token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetCampaignById(${'$'}id: ID!) {
                  getCampaignById(id: ${'$'}id) {
                    id
                    title
                    description
                    status
                    createdAt
                    budgetMin
                    budgetMax
                    startDate
                    endDate
                    categories {
                      category
                      subCategories
                    }
                    platforms {
                      platform
                      formats
                    }
                    targetAudience {
                      ageMin
                      ageMax
                      gender
                      locations
                    }
                    brand {
                      id
                      name
                      email
                      role
                      profileCompleted
                      updatedAt
                      about
                      profileUrl
                      logoUrl
                      isVerified
                      averageRating
                      brandCategories {
                        category
                        subCategories
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

            val variables = mapOf("id" to id)
            val result = GraphQLClient.query(query = query, variables = variables, token = token)
            result.onSuccess { jsonObject ->
                val errors = jsonObject.optJSONArray("errors")
                if (errors != null && errors.length() > 0) {
                    val errorMsg = errors.getJSONObject(0).optString("message", "Unknown GraphQL Error")
                    _error.postValue(errorMsg)
                    _loading.postValue(false)
                    return@onSuccess
                }

                try {
                    val data = jsonObject.optJSONObject("data")
                    val campaignObj = data?.optJSONObject("getCampaignById")
                    if (campaignObj != null) {
                        _campaign.postValue(parseCampaignDetail(campaignObj))
                    } else {
                        _error.postValue("Campaign not found")
                    }
                } catch (e: Exception) {
                    Log.e("CampaignViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
                _loading.postValue(false)
            }.onFailure {
                Log.e("CampaignViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
                _loading.postValue(false)
            }
        }
    }

    fun createCampaign(token: String) {
        _loading.value = true
        _error.value = null
        _createCampaignSuccess.value = false
        _createdCampaign.value = null

        viewModelScope.launch {
            val mutation = """
                mutation CreateCampaign(${'$'}input: CampaignInput!) {
                  createCampaign(input: ${'$'}input) {
                    id
                    title
                    description
                    status
                    createdAt
                    budgetMin
                    budgetMax
                    startDate
                    endDate
                    categories {
                      category
                      subCategories
                    }
                    platforms {
                      platform
                      formats
                    }
                    targetAudience {
                      ageMin
                      ageMax
                      gender
                      locations
                    }
                    brand {
                      id
                      name
                      email
                      role
                      profileCompleted
                      updatedAt
                      about
                      profileUrl
                      logoUrl
                      isVerified
                      averageRating
                      brandCategories {
                        category
                        subCategories
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

            val platformsJson = selectedPlatforms.map { platform ->
                mapOf(
                    "platform" to platform,
                    "formats" to (platformFormats[platform]?.toList() ?: emptyList<String>())
                )
            }
            val audienceJson = mapOf(
                "ageMin" to ageMin,
                "ageMax" to ageMax,
                "gender" to selectedGender,
                "locations" to selectedLocations.toList()
            )

            val categoriesJson = selectedCategories.map { cat ->
                val subCats = selectedSubCategories[cat] ?: emptySet()
                mapOf(
                    "category" to cat,
                    "subCategories" to if (subCats.isEmpty()) listOf("General") else subCats.toList()
                )
            }

            val input = mutableMapOf<String, Any>(
                "title" to title,
                "description" to description,
                "categories" to categoriesJson,
                "platforms" to platformsJson,
                "budgetMin" to budgetMin,
                "budgetMax" to budgetMax,
                "targetAudience" to audienceJson
            )

            startDate?.let { input["startDate"] = dateFormatter.format(it) }
            endDate?.let { input["endDate"] = dateFormatter.format(it) }

            val variables = mapOf("input" to input)

            val result = GraphQLClient.query(query = mutation, variables = variables, token = token)
            
            result.onSuccess { jsonObject ->
                val data = jsonObject.optJSONObject("data")
                val createCampaignJson = data?.optJSONObject("createCampaign")
                if (createCampaignJson != null) {
                    val campaign = parseCampaignDetail(createCampaignJson)
                    _createdCampaign.postValue(campaign)
                    _createCampaignSuccess.postValue(true)
                } else {
                    val errors = jsonObject.optJSONArray("errors")
                    val errorMsg = errors?.optJSONObject(0)?.optString("message") ?: "Failed to create campaign"
                    _error.postValue(errorMsg)
                }
                _loading.postValue(false)
            }.onFailure {
                Log.e("CampaignViewModel", "Error creating campaign", it)
                _error.postValue(it.message ?: "Network error")
                _loading.postValue(false)
            }
        }
    }

    private fun parseCampaignDetail(json: JSONObject): CampaignDetail {
        val brandJson = json.optJSONObject("brand")
        val brand = brandJson?.let {
            val categoriesArray = it.optJSONArray("brandCategories")
            val brandCategories = mutableListOf<BrandCategory>()
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
                        brandCategories.add(BrandCategory(catObj.optString("category"), subCats))
                    }
                }
            }

            val targetAudienceObj = it.optJSONObject("targetAudience")
            val brandTargetAudience = if (targetAudienceObj != null) {
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
                    gender = targetAudienceObj.optString("gender", null as String?),
                    locations = locations
                )
            } else null
            
            Brand(
                id = it.optString("id"),
                email = it.optString("email"),
                name = it.optString("name"),
                role = it.optString("role"),
                profileCompleted = if (it.has("profileCompleted")) it.optBoolean("profileCompleted") else null,
                updatedAt = it.optString("updatedAt", null as String?),
                brandCategories = brandCategories,
                about = it.optString("about"),
                profileUrl = it.optString("profileUrl"),
                logoUrl = it.optString("logoUrl"),
                govtId = it.optString("govtId"),
                isVerified = if (it.has("isVerified")) it.optBoolean("isVerified") else null,
                reviews = null,
                averageRating = if (it.has("averageRating")) it.optDouble("averageRating").takeIf { it != 0.0 } else null,
                fcmToken = it.optString("fcmToken"),
                preferredPlatforms = null, // Use campaign level platforms
                targetAudience = brandTargetAudience
            )
        }

        val platformsArray = json.optJSONArray("platforms")
        val platforms = mutableListOf<Platform>()
        if (platformsArray != null) {
            for (i in 0 until platformsArray.length()) {
                val p = platformsArray.getJSONObject(i)
                val formatsArray = p.optJSONArray("formats")
                val formats = if (formatsArray != null) {
                    val fList = mutableListOf<String>()
                    for (j in 0 until formatsArray.length()) {
                        fList.add(formatsArray.getString(j))
                    }
                    fList
                } else null
                
                platforms.add(
                    Platform(
                        platform = p.optString("platform"),
                        profileUrl = null,
                        followers = null,
                        avgViews = null,
                        engagement = null,
                        formats = formats,
                        connected = null,
                        minFollowers = null,
                        minEngagement = null
                    )
                )
            }
        }

        val audienceJson = json.optJSONObject("targetAudience")
        val audience = audienceJson?.let { aud ->
            val locationsArray = aud.optJSONArray("locations")
            val locations = mutableListOf<String>()
            if (locationsArray != null) {
                for (i in 0 until locationsArray.length()) {
                    locations.add(locationsArray.getString(i))
                }
            }
            CampaignAudienceResponse(
                aud.optInt("ageMin").takeIf { it != 0 },
                aud.optInt("ageMax").takeIf { it != 0 },
                aud.optString("gender"),
                locations
            )
        }

        val categoriesArray = json.optJSONArray("categories")
        val categories = mutableListOf<BrandCategory>()
        if (categoriesArray != null) {
            for (i in 0 until categoriesArray.length()) {
                val cat = categoriesArray.getJSONObject(i)
                val subCatsArray = cat.optJSONArray("subCategories")
                val subCats = mutableListOf<String>()
                if (subCatsArray != null) {
                    for (j in 0 until subCatsArray.length()) {
                        subCats.add(subCatsArray.getString(j))
                    }
                }
                categories.add(BrandCategory(cat.optString("category"), subCats))
            }
        }

        return CampaignDetail(
            json.optString("id"),
            json.optString("title"),
            json.optString("description"),
            json.optString("status"),
            json.optString("createdAt"),
            json.optInt("budgetMin").takeIf { it != 0 },
            json.optInt("budgetMax").takeIf { it != 0 },
            json.optString("startDate"),
            json.optString("endDate"),
            audience,
            platforms,
            brand,
            categories
        )
    }
    
    fun clearState() {
        title = ""
        description = ""
        selectedCategories = emptySet()
        selectedSubCategories = emptyMap()
        selectedPlatforms = emptySet()
        platformFormats = emptyMap()
        startDate = null
        endDate = null
        budgetMin = 50
        budgetMax = 200
        selectedLocations = emptySet()
        ageMin = 18
        ageMax = 60
        selectedGender = "Any"
        _createCampaignSuccess.value = false
        _error.value = null
    }
}
