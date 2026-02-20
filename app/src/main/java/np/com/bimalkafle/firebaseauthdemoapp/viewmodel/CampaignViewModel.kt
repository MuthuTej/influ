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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CampaignViewModel : ViewModel() {

    // Screen 1 Fields
    var title by mutableStateOf("")
    var description by mutableStateOf("")

    var selectedPlatforms by mutableStateOf(setOf<String>())
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

    private val _wishlistedCampaigns = MutableLiveData<List<CampaignDetail>>(emptyList())
    val wishlistedCampaigns: LiveData<List<CampaignDetail>> = _wishlistedCampaigns

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
            // Updated query to use inline fragment for 'WishlistUnion'
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
                      brand {
                        id
                        name
                        logoUrl
                        brandCategory {
                          category
                          subCategory
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
                            // Only add if it's a Campaign (has an id and title)
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
                    brand {
                      id
                      name
                      logoUrl
                      brandCategory {
                        category
                        subCategory
                      }
                    }
                  }
                }
            """.trimIndent()

            val result = GraphQLClient.query(query = query, token = token)
            result.onSuccess { jsonObject ->
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
                    } else {
                        _campaigns.postValue(emptyList())
                    }
                } catch (e: Exception) {
                    Log.e("CampaignViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("CampaignViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
            }
            _loading.postValue(false)
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
                    brand {
                      brandCategory {
                        category
                        subCategory
                      }
                      about
                      preferredPlatforms {
                        platform
                      }
                      targetAudience {
                        ageMin
                        ageMax
                        gender
                        locations
                      }
                      name
                      logoUrl
                    }
                  }
                }
            """.trimIndent()

            val platformsJson = selectedPlatforms.map { mapOf("platform" to it) }
            val audienceJson = mapOf(
                "ageMin" to ageMin,
                "ageMax" to ageMax,
                "gender" to selectedGender,
                "locations" to selectedLocations.toList()
            )

            val input = mutableMapOf<String, Any>(
                "title" to title,
                "description" to description,
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
            }.onFailure {
                Log.e("CampaignViewModel", "Error creating campaign", it)
                _error.postValue(it.message ?: "Network error")
            }
            _loading.postValue(false)
        }
    }

    private fun parseCampaignDetail(json: JSONObject): CampaignDetail {
        val brandJson = json.optJSONObject("brand")
        val brand = brandJson?.let {
            val categoryJson = it.optJSONObject("brandCategory")
            val category = categoryJson?.let { cat ->
                BrandCategory(cat.optString("category"), cat.optString("subCategory"))
            }
            
            val platformsArray = it.optJSONArray("preferredPlatforms")
            val platforms = mutableListOf<CampaignPlatformInput>()
            if (platformsArray != null) {
                for (i in 0 until platformsArray.length()) {
                    val p = platformsArray.getJSONObject(i)
                    platforms.add(CampaignPlatformInput(p.optString("platform")))
                }
            }

            val audienceJson = it.optJSONObject("targetAudience")
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

            BrandResponse(
                it.optString("name"),
                it.optString("about"),
                it.optString("logoUrl"),
                category,
                platforms,
                audience,
                it.optString("id")
            )
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
            null,
            brand
        )
    }
    
    fun clearState() {
        title = ""
        description = ""

        selectedPlatforms = emptySet()
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
