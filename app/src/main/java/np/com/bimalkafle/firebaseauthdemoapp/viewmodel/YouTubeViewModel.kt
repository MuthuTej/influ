package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.model.YouTubeChannelData
import np.com.bimalkafle.firebaseauthdemoapp.network.GraphQLClient
import org.json.JSONObject

class YouTubeViewModel : ViewModel() {

    private val _channelData = MutableLiveData<YouTubeChannelData?>()
    val channelData: LiveData<YouTubeChannelData?> = _channelData

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    fun initializeGoogleSignIn(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("61884125308-s3uiiss031jvqaje7thsu58027dckp8b.apps.googleusercontent.com")
            .requestScopes(
                Scope("https://www.googleapis.com/auth/youtube.readonly"),
                Scope("https://www.googleapis.com/auth/youtube.upload"),
                Scope("https://www.googleapis.com/auth/youtube.analytics.readonly")
            )
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    fun connectYouTube(authCode: String, firebaseToken: String) {
        _loading.value = true
        _error.value = null
        _success.value = false

        viewModelScope.launch {
            try {
                val mutation = """
                    mutation ConnectYouTube(${'$'}code: String!) {
                      connectYouTube(code: ${'$'}code) {
                        channelId
                        title
                        description
                        customUrl
                        publishedAt
                        thumbnail
                        bannerImageUrl
                        subscriberCount
                        viewCount
                        totalVideoViews
                        videoCount
                        uploadsPlaylistId
                        privacyStatus
                        keywords
                        country
                        topics
                        syncedAt
                      }
                    }
                """.trimIndent()

                val variables = mapOf("code" to authCode)

                val result = GraphQLClient.query(
                    query = mutation,
                    variables = variables,
                    token = firebaseToken
                )

                result.onSuccess { jsonObject ->
                    try {
                        val data = jsonObject.optJSONObject("data")
                        val errors = jsonObject.optJSONArray("errors")

                        when {
                            data != null -> {
                                val youtubeData = data.optJSONObject("connectYouTube")
                                if (youtubeData != null) {
                                    val channelData = parseYouTubeChannelData(youtubeData)
                                    _channelData.postValue(channelData)
                                    _success.postValue(true)
                                    Log.d("YouTubeViewModel", "YouTube connected successfully")
                                } else {
                                    _error.postValue("No channel data returned")
                                }
                            }
                            errors != null && errors.length() > 0 -> {
                                val message = errors.getJSONObject(0).optString(
                                    "message",
                                    "Failed to connect YouTube"
                                )
                                _error.postValue(message)
                            }
                            else -> {
                                _error.postValue("Unknown error occurred")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("YouTubeViewModel", "Parsing error", e)
                        _error.postValue("Parsing error: ${e.message}")
                    }
                }.onFailure {
                    Log.e("YouTubeViewModel", "Network error", it)
                    _error.postValue("Network error: ${it.message}")
                }
            } finally {
                _loading.value = false
            }
        }
    }

    private fun parseYouTubeChannelData(json: JSONObject): YouTubeChannelData {
        return YouTubeChannelData(
            channelId = json.optString("channelId", ""),
            title = json.optString("title", ""),
            description = json.optString("description"),
            customUrl = json.optString("customUrl"),
            publishedAt = json.optString("publishedAt"),
            thumbnail = json.optString("thumbnail"),
            bannerImageUrl = json.optString("bannerImageUrl"),
            subscriberCount = json.optInt("subscriberCount", 0),
            viewCount = json.optInt("viewCount", 0),
            totalVideoViews = json.optInt("totalVideoViews", 0),
            videoCount = json.optInt("videoCount", 0),
            uploadsPlaylistId = json.optString("uploadsPlaylistId"),
            privacyStatus = json.optString("privacyStatus"),
            keywords = json.optString("keywords"),
            country = json.optString("country"),
            topics = json.optJSONArray("topics")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            } ?: emptyList(),
            syncedAt = json.optString("syncedAt")
        )
    }

    fun clearError() {
        _error.value = null
    }

    fun resetState() {
        _channelData.value = null
        _loading.value = false
        _error.value = null
        _success.value = false
    }
}
