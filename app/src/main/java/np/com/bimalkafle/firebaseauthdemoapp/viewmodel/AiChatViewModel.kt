package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import np.com.bimalkafle.firebaseauthdemoapp.network.AiChatRepository
import np.com.bimalkafle.firebaseauthdemoapp.network.AiChatTurn
import np.com.bimalkafle.firebaseauthdemoapp.network.ChatEntity
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

data class AiChatMessage(
    val text: String,
    val isUser: Boolean,
    val entities: List<ChatEntity> = emptyList()
)

// Keeps the backend's per-call payload bounded — older turns just drop off.
private const val MAX_HISTORY_TURNS = 10

// How many messages to keep on disk — a chat history is small, but this caps
// it so a long-running conversation doesn't grow the stored blob forever.
private const val MAX_STORED_MESSAGES = 60

/**
 * AndroidViewModel (not plain ViewModel) specifically so it can read/write
 * local chat history via PrefsManager — Compose's default viewModel() factory
 * knows how to supply the Application for this automatically, no custom
 * factory needed at the call site.
 */
class AiChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PrefsManager(application)
    private val uid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var history = listOf<AiChatTurn>()

    init {
        restoreFromDisk()
    }

    fun clearError() {
        _error.value = null
    }

    fun clearChat() {
        _messages.value = emptyList()
        history = emptyList()
        uid?.let { prefs.clearAiChatHistory(it) }
    }

    private fun restoreFromDisk() {
        val currentUid = uid ?: return
        val stored = prefs.getAiChatHistory(currentUid) ?: return
        val restored = runCatching { decodeMessages(stored) }.getOrDefault(emptyList())
        if (restored.isEmpty()) return

        _messages.value = restored
        history = restored.takeLast(MAX_HISTORY_TURNS).map {
            AiChatTurn(if (it.isUser) "user" else "model", it.text)
        }
    }

    private fun persistToDisk() {
        val currentUid = uid ?: return
        prefs.saveAiChatHistory(currentUid, encodeMessages(_messages.value.takeLast(MAX_STORED_MESSAGES)))
    }

    private suspend fun getIdToken(): String? = suspendCancellableCoroutine { cont ->
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }
        user.getIdToken(false)
            .addOnSuccessListener { cont.resume(it.token) }
            .addOnFailureListener { cont.resume(null) }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        _messages.value = _messages.value + AiChatMessage(text = text, isUser = true)
        persistToDisk()
        _isLoading.value = true

        viewModelScope.launch {
            val token = getIdToken()

            if (token == null) {
                _error.value = "You're not signed in."
                _isLoading.value = false
                return@launch
            }

            val result = AiChatRepository.sendMessage(text, history, token)
            _isLoading.value = false

            result.onSuccess { response ->
                _messages.value = _messages.value + AiChatMessage(
                    text = response.reply,
                    isUser = false,
                    entities = response.entities
                )
                persistToDisk()
                history = (history + AiChatTurn("user", text) + AiChatTurn("model", response.reply))
                    .takeLast(MAX_HISTORY_TURNS)
            }.onFailure { e ->
                _error.value = e.message ?: "Something went wrong"
            }
        }
    }
}

private fun encodeMessages(messages: List<AiChatMessage>): String {
    val array = JSONArray()
    messages.forEach { message ->
        val entitiesJson = JSONArray()
        message.entities.forEach { entity ->
            entitiesJson.put(JSONObject().apply {
                put("type", entity.type)
                put("id", entity.id)
                put("label", entity.label)
            })
        }
        array.put(JSONObject().apply {
            put("text", message.text)
            put("isUser", message.isUser)
            put("entities", entitiesJson)
        })
    }
    return array.toString()
}

private fun decodeMessages(json: String): List<AiChatMessage> {
    val array = JSONArray(json)
    return (0 until array.length()).map { i ->
        val obj = array.getJSONObject(i)
        val entitiesJson = obj.optJSONArray("entities")
        val entities = entitiesJson?.let { arr ->
            (0 until arr.length()).map { j ->
                val e = arr.getJSONObject(j)
                ChatEntity(e.getString("type"), e.getString("id"), e.optString("label", e.getString("id")))
            }
        } ?: emptyList()
        AiChatMessage(text = obj.getString("text"), isUser = obj.getBoolean("isUser"), entities = entities)
    }
}
