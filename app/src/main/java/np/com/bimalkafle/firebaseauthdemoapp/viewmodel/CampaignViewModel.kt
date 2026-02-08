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
    var objective by mutableStateOf("")
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

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
                    objective
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
                      primaryObjective
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
                "objective" to objective,
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
                    // Manual parsing to CampaignDetail
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
                it.optString("primaryObjective"),
                category,
                platforms,
                audience
            )
        }

        return CampaignDetail(
            json.optString("id"),
            json.optString("title"),
            json.optString("description"),
            json.optString("objective"),
            json.optString("status"),
            json.optString("createdAt"),
            json.optInt("budgetMin").takeIf { it != 0 },
            json.optInt("budgetMax").takeIf { it != 0 },
            json.optString("startDate"),
            json.optString("endDate"),
            null, // targetAudience in root not requested in this mutation yet, but can be added if needed
            brand
        )
    }
    
    fun clearState() {
        title = ""
        description = ""
        objective = ""
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
