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
                    _error.postValue("Parsing error: ${e.message}")
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${it.message}")
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
                    _error.postValue("Parsing error: ${e.message}")
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${it.message}")
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
                    categoriesList.add(
                        Category(
                            category = catObj.optString("category", ""),
                            subCategory = catObj.optString("subCategory", "")
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
            audienceInsights = audienceInsights
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
                    Log.e("InfluencerViewModel", "Parsing error", e)
                    _error.postValue("Parsing error: ${e.message}")
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${it.message}")
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

            // Influencer object is not returned in this query based on user request, creating a dummy or handling nulls if needed.
            // Actually the query in user request DOES NOT have influencer field in GetCollaborations for InfluencerHomePage.
            // But Collaboration model needs it. We can create a partial one or empty one.
            val influencer = Influencer("Me", null, null, null)
            
            val brandObj = obj.optJSONObject("brand")
            val brand = if (brandObj != null) {
                // Reuse logic from parseBrands but tailored for single object if needed, or duplicate slightly for simplicity in this context
                // Parse Brand Category
                val categoryObj = brandObj.optJSONObject("brandCategory")
                val brandCategory = if (categoryObj != null) {
                    BrandCategory(
                        category = categoryObj.optString("category", ""),
                        subCategory = categoryObj.optString("subCategory", "")
                    )
                } else null

                Brand(
                    id = brandObj.optString("id"),
                    email = brandObj.optString("email"),
                    name = brandObj.optString("name"),
                    role = brandObj.optString("role"),
                    profileCompleted = if(brandObj.has("profileCompleted")) brandObj.optBoolean("profileCompleted") else null,
                    updatedAt = brandObj.optString("updatedAt"),
                    brandCategory = brandCategory,
                    about = brandObj.optString("about"),
                    primaryObjective = brandObj.optString("primaryObjective"),
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
                    totalAmount = if (obj.isNull("totalAmount")) null else obj.optInt("totalAmount"),
                    brand = brand
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
                    brandCategory {
                      category
                      subCategory
                    }
                    about
                    primaryObjective
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
                    _error.postValue("Parsing error: ${e.message}")
                }
            }.onFailure {
                Log.e("InfluencerViewModel", "Network error", it)
                _error.postValue("Network error: ${it.message}")
            }
            _loading.postValue(false)
        }
    }

    private fun parseBrands(jsonArray: JSONArray): List<Brand> {
        val list = mutableListOf<Brand>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue

            val categoryObj = obj.optJSONObject("brandCategory")
            val brandCategory = if (categoryObj != null) {
                BrandCategory(
                    category = categoryObj.optString("category", ""),
                    subCategory = categoryObj.optString("subCategory", "")
                )
            } else null

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
                    brandCategory = brandCategory,
                    about = obj.optString("about"),
                    primaryObjective = obj.optString("primaryObjective"),
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
