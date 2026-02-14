package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import np.com.bimalkafle.firebaseauthdemoapp.model.AgeGroupInsight
import np.com.bimalkafle.firebaseauthdemoapp.model.AudienceInsights
import np.com.bimalkafle.firebaseauthdemoapp.model.Brand
import np.com.bimalkafle.firebaseauthdemoapp.model.BrandCategory
import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.model.Category
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.GenderSplit
import np.com.bimalkafle.firebaseauthdemoapp.model.Influencer
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.model.LocationInsight
import np.com.bimalkafle.firebaseauthdemoapp.model.Platform
import np.com.bimalkafle.firebaseauthdemoapp.model.Pricing
import np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo
import np.com.bimalkafle.firebaseauthdemoapp.model.PreferredPlatform
import np.com.bimalkafle.firebaseauthdemoapp.model.TargetAudience
import np.com.bimalkafle.firebaseauthdemoapp.network.GraphQLClient
import np.com.bimalkafle.firebaseauthdemoapp.network.BrandRepository
import org.json.JSONArray
import org.json.JSONObject

class BrandViewModel : ViewModel() {

    private val _collaborations = MutableLiveData<List<Collaboration>>()
    val collaborations: LiveData<List<Collaboration>> = _collaborations

    private val _influencers = MutableLiveData<List<InfluencerProfile>>()
    val influencers: LiveData<List<InfluencerProfile>> = _influencers

    private val _brandProfile = MutableLiveData<Brand?>()
    val brandProfile: LiveData<Brand?> = _brandProfile
    
    private val _myCampaigns = MutableLiveData<List<Campaign>>()
    val myCampaigns: LiveData<List<Campaign>> = _myCampaigns

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchInfluencers(token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
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
                    location
                    categories {
                      category
                      subCategory
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
                        } else {
                            _influencers.postValue(emptyList())
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
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("BrandViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
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
                    categories.add(Category(cObj.optString("category"), cObj.optString("subCategory")))
                }
            }
        }

        val platformsArray = obj.optJSONArray("platforms")
        val platforms = mutableListOf<Platform>()
        if (platformsArray != null) {
            for (i in 0 until platformsArray.length()) {
                val pObj = platformsArray.optJSONObject(i)
                if (pObj != null) {
                    platforms.add(
                        Platform(
                            platform = pObj.optString("platform"),
                            profileUrl = pObj.optString("profileUrl"),
                            followers = pObj.optInt("followers", 0),
                            avgViews = pObj.optInt("avgViews", 0),
                            engagement = pObj.optDouble("engagement", 0.0).toFloat(),
                            formats = null,
                            connected = pObj.optBoolean("connected")
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
            for (i in 0 until strengthsArray.length()) {
                strengths.add(strengthsArray.getString(i))
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

        return InfluencerProfile(
            id = obj.optString("id"),
            email = obj.optString("email"),
            name = obj.optString("name"),
            role = obj.optString("role"),
            profileCompleted = obj.optBoolean("profileCompleted"),
            updatedAt = obj.optString("updatedAt"),
            bio = obj.optString("bio"),
            location = obj.optString("location"),
            categories = categories,
            platforms = platforms,
            audienceInsights = audienceInsights,
            strengths = strengths,
            pricing = pricing,
            availability = obj.optBoolean("availability"),
            logoUrl = obj.optString("logoUrl")
        )
    }

    fun fetchBrandDetails(token: String) {
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
                      brandCategory {
                        category
                        subCategory
                      }
                      about
                      primaryObjective
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
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("BrandViewModel", "Network error", it)
                _error.postValue("Network error: ${'$'}{it.message}")
            }
            _loading.postValue(false)
        }
    }

    private fun parseBrand(obj: JSONObject): Brand {
        val categoryObj = obj.optJSONObject("brandCategory")
        val brandCategory = if (categoryObj != null) {
            BrandCategory(
                category = categoryObj.optString("category", ""),
                subCategory = categoryObj.optString("subCategory", "")
            )
        } else null

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
                gender = targetAudienceObj.optString("gender", null),
                locations = locations
            )
        } else null

        return Brand(
            id = obj.optString("id", ""),
            email = obj.optString("email", ""),
            name = obj.optString("name", ""),
            role = obj.optString("role", ""),
            profileCompleted = obj.optBoolean("profileCompleted"),
            updatedAt = obj.optString("updatedAt", null),
            brandCategory = brandCategory,
            about = obj.optString("about", null),
            primaryObjective = obj.optString("primaryObjective", null),
            profileUrl = obj.optString("profileUrl", null),
            logoUrl = obj.optString("logoUrl", null),
            preferredPlatforms = preferredPlatforms,
            targetAudience = targetAudience
        )
    }

    fun updateBrandProfile(
        token: String,
        name: String,
        brandCategory: String,
        subCategory: String,
        about: String,
        primaryObjective: String,
        preferredPlatforms: List<String>,
        ageMin: Int?,
        ageMax: Int?,
        gender: String,
        profileUrl: String?,
        logoUrl: String,
        onComplete: (Boolean) -> Unit
    ) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val success = BrandRepository.setupBrandProfile(
                token = token,
                name = name,
                brandCategory = brandCategory,
                subCategory = subCategory,
                about = about,
                primaryObjective = primaryObjective,
                preferredPlatforms = preferredPlatforms,
                ageMin = ageMin,
                ageMax = ageMax,
                gender = gender,
                profileUrl = profileUrl,
                logoUrl = logoUrl
            )
            if (success) {
                fetchBrandDetails(token)
            } else {
                _error.postValue("Failed to update profile")
            }
            onComplete(success)
            _loading.postValue(false)
        }
    }

    fun fetchMyCampaigns(token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetMyCampaigns {
                  getMyCampaigns {
                    id
                    brandId
                    title
                    description
                    objective
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
                            val obj = campaignsArray.getJSONObject(i)
                            list.add(
                                Campaign(
                                    id = obj.optString("id"),
                                    brandId = obj.optString("brandId"),
                                    title = obj.optString("title"),
                                    description = obj.optString("description"),
                                    objective = obj.optString("objective"),
                                    budgetMin = null,
                                    budgetMax = null,
                                    startDate = null,
                                    endDate = null,
                                    status = null,
                                    createdAt = null,
                                    updatedAt = null
                                )
                            )
                        }
                        _myCampaigns.postValue(list)
                    } else {
                        _myCampaigns.postValue(emptyList())
                    }
                } catch (e: Exception) {
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                _error.postValue("Network error: ${'$'}{it.message}")
            }
            _loading.postValue(false)
        }
    }

    fun inviteInfluencer(token: String, influencerId: String, campaignId: String, message: String, pricing: List<Map<String, Any>>, onComplete: (Boolean) -> Unit) {
        _loading.value = true
        _error.value = null
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
                    val errors = jsonObject.optJSONArray("errors")
                    val errorMsg = errors?.optJSONObject(0)?.optString("message") ?: "Invitation failed"
                    _error.postValue(errorMsg)
                    onComplete(false)
                }
            }.onFailure {
                _error.postValue("Network error: ${'$'}{it.message}")
                onComplete(false)
            }
            _loading.postValue(false)
        }
    }

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
                    initiatedBy
                    createdAt
                    updatedAt
                    campaign {
                      id
                      brandId
                      title
                      description
                      objective
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
                    paymentStatus
                    razorpayOrderId
                    advancePaid
                    finalPaid
                    totalAmount
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
                    Log.e("BrandViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${'$'}{e.message}")
                }
            }.onFailure {
                Log.e("BrandViewModel", "Network error", it)
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
                    objective = campaignObj.optString("objective"),
                    budgetMin = if (campaignObj.isNull("budgetMin")) null else campaignObj.optInt("budgetMin"),
                    budgetMax = if (campaignObj.isNull("budgetMax")) null else campaignObj.optInt("budgetMax"),
                    startDate = campaignObj.optString("startDate"),
                    endDate = campaignObj.optString("endDate"),
                    status = campaignObj.optString("status"),
                    createdAt = campaignObj.optString("createdAt"),
                    updatedAt = campaignObj.optString("updatedAt")
                )
            } else {
                Campaign("unknown", null, "Unknown Campaign", null, null, null, null, null, null, null, null, null)
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
                    paymentStatus = obj.optString("paymentStatus"),
                    razorpayOrderId = obj.optString("razorpayOrderId"),
                    advancePaid = if (obj.isNull("advancePaid")) null else obj.optBoolean("advancePaid"),
                    finalPaid = if (obj.isNull("finalPaid")) null else obj.optBoolean("finalPaid"),
                    totalAmount = if (obj.isNull("totalAmount")) null else obj.optInt("totalAmount")
                )
            )
        }
        return list
    }
}
