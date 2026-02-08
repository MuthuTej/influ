package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.Influencer
import np.com.bimalkafle.firebaseauthdemoapp.model.Pricing
import np.com.bimalkafle.firebaseauthdemoapp.network.GraphQLClient
import org.json.JSONArray
import org.json.JSONObject

class BrandViewModel : ViewModel() {

    private val _collaborations = MutableLiveData<List<Collaboration>>()
    val collaborations: LiveData<List<Collaboration>> = _collaborations

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun fetchCollaborations(token: String) {
        _loading.value = true
        _error.value = null
        viewModelScope.launch {
            val query = """
                query GetCollaborations {
                  getCollaborations {
                    id
                    status
                    message
                    createdAt
                    campaign {
                      id
                      title
                    }
                    pricing {
                      currency
                      deliverable
                      platform
                      price
                    }
                    initiatedBy
                    influencer {
                      name
                      bio
                      logoUrl
                      updatedAt
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

    private fun parseCollaborations(jsonArray: JSONArray): List<Collaboration> {
        val list = mutableListOf<Collaboration>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            
            val campaignObj = obj.getJSONObject("campaign")
            val campaign = Campaign(
                id = campaignObj.getString("id"),
                title = campaignObj.getString("title")
            )

            val influencerObj = obj.optJSONObject("influencer")
            val influencer = if (influencerObj != null) {
                 Influencer(
                    name = influencerObj.getString("name"),
                    bio = influencerObj.optString("bio"),
                    logoUrl = influencerObj.optString("logoUrl"),
                    updatedAt = influencerObj.optString("updatedAt")
                )
            } else {
                // Fallback or skip
                 Influencer("Unknown", null, null, null)
            }


            val pricingList = mutableListOf<Pricing>()
            val pricingArray = obj.optJSONArray("pricing")
            if (pricingArray != null) {
                for (j in 0 until pricingArray.length()) {
                    val pObj = pricingArray.getJSONObject(j)
                    pricingList.add(
                        Pricing(
                            currency = pObj.getString("currency"),
                            deliverable = pObj.getString("deliverable"),
                            platform = pObj.getString("platform"),
                            price = pObj.getInt("price")
                        )
                    )
                }
            }


            list.add(
                Collaboration(
                    id = obj.getString("id"),
                    status = obj.getString("status"),
                    message = obj.optString("message"),
                    createdAt = obj.getString("createdAt"),
                    campaign = campaign,
                    pricing = pricingList,
                    initiatedBy = obj.getString("initiatedBy"),
                    influencer = influencer
                )
            )
        }
        return list
    }
}
