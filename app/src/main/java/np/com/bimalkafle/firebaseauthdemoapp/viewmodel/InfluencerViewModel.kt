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
                val catObj = categoriesArray.getJSONObject(i)
                categoriesList.add(
                    Category(
                        category = catObj.optString("category", ""),
                        subCategory = catObj.optString("subCategory", "")
                    )
                )
            }
        }

        val platformsList = mutableListOf<Platform>()
        val platformsArray = obj.optJSONArray("platforms")
        if (platformsArray != null) {
            for (i in 0 until platformsArray.length()) {
                val platObj = platformsArray.getJSONObject(i)
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
                val prObj = pricingArray.getJSONObject(i)
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

        val audienceInsightsObj = obj.optJSONObject("audienceInsights")
        var audienceInsights: AudienceInsights? = null
        if (audienceInsightsObj != null) {
            val topLocations = mutableListOf<LocationInsight>()
            val locationsArray = audienceInsightsObj.optJSONArray("topLocations")
            if (locationsArray != null) {
                for (i in 0 until locationsArray.length()) {
                    val locObj = locationsArray.getJSONObject(i)
                    topLocations.add(LocationInsight(
                        city = locObj.optString("city"),
                        country = locObj.optString("country"),
                        percentage = locObj.optDouble("percentage").toFloat()
                    ))
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
                    val ageObj = ageArray.getJSONObject(i)
                    ageGroups.add(AgeGroupInsight(
                        range = ageObj.optString("range"),
                        percentage = ageObj.optDouble("percentage").toFloat()
                    ))
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
}
