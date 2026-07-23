package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatUser
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.repository.ChatRepository
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import np.com.bimalkafle.firebaseauthdemoapp.network.ChatWebSocketClient
import np.com.bimalkafle.firebaseauthdemoapp.utils.ContactInfoFilter
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ChatListEntry(
    val user: ChatUser,
    val collaborationId: String,
    val unreadCount: Int,
    val lastMessage: String
)

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private var currentOtherUserId: String? = null
    private var currentCollaborationId: String? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _chatName = MutableStateFlow("")
    val chatName: StateFlow<String> = _chatName

    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    val replyingTo: StateFlow<ChatMessage?> = _replyingTo

    private val _chatList = MutableStateFlow<List<ChatListEntry>>(emptyList())
    val chatList: StateFlow<List<ChatListEntry>> = _chatList

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _contactInfoWarning = MutableStateFlow<String?>(null)
    val contactInfoWarning: StateFlow<String?> = _contactInfoWarning

    private var conversationWsClient: ChatWebSocketClient? = null
    private var chatListWsClient: ChatWebSocketClient? = null

    init {
        repository.ensureUserExistsInFirestore()
    }

    fun clearError() { _error.value = null }
    fun clearContactInfoWarning() { _contactInfoWarning.value = null }

    // ── Chat list ─────────────────────────────────────────────────────────────

    fun loadChatList(currentUserRole: String) {
        viewModelScope.launch {
            val token = getToken() ?: run { _error.value = "Authentication error"; return@launch }

            val result = BackendRepository.getChatList(token)
            result.onSuccess { jsonList ->
                _chatList.value = jsonList.map { json ->
                    ChatListEntry(
                        user = ChatUser(
                            uid = json.optString("otherUserId"),
                            name = json.optString("otherUserName", "Unknown"),
                            email = "",
                            profileImageUrl = json.optString("otherUserImageUrl")
                                .takeIf { it.isNotBlank() }
                        ),
                        collaborationId = json.optString("collaborationId"),
                        unreadCount = json.optInt("unreadCount", 0),
                        lastMessage = json.optString("lastMessage", "")
                    )
                }
            }.onFailure { err ->
                Log.e("ChatViewModel", "loadChatList failed: ${err.message}")
                _error.value = err.message ?: "Failed to load chats"
            }

            // WebSocket for live chat-list updates (anyMessageAdded subscription)
            val currentUserId = getCurrentUserId()
            chatListWsClient?.disconnect()
            chatListWsClient = ChatWebSocketClient(
                token = token,
                currentUserId = currentUserId,
                onMessage = { msg -> updateChatListWithMessage(msg, currentUserId) }
            )
            chatListWsClient?.connectForChatList()
        }
    }

    private fun updateChatListWithMessage(msg: ChatMessage, currentUserId: String) {
        val current = _chatList.value.toMutableList()

        // Match by collaborationId when available, otherwise by otherUserId
        val idx = if (msg.collaborationId != null) {
            current.indexOfFirst { it.collaborationId == msg.collaborationId }
        } else {
            val otherUserId = if (msg.senderId == currentUserId) msg.receiverId else msg.senderId
            current.indexOfFirst { it.user.uid == otherUserId }
        }

        if (idx >= 0) {
            val entry = current.removeAt(idx)
            current.add(0, entry.copy(
                lastMessage = msg.text,
                unreadCount = if (msg.senderId != currentUserId) entry.unreadCount + 1 else entry.unreadCount
            ))
            _chatList.value = current
        }
        // If not found (new collaboration), reload the full list next tick
    }

    // ── Conversation ──────────────────────────────────────────────────────────

    fun initChat(
        otherUserId: String,
        otherUserName: String = "Chat",
        collaborationId: String? = null
    ) {
        currentOtherUserId = otherUserId
        currentCollaborationId = collaborationId
        _chatName.value = otherUserName

        val currentUserId = getCurrentUserId()

        viewModelScope.launch {
            val token = getToken() ?: run { _error.value = "Authentication error"; return@launch }

            val history = repository.getMessageHistory(otherUserId, collaborationId, token)
            _messages.value = history.map { it.copy(isMe = it.senderId == currentUserId) }
            repository.markMessagesAsRead(otherUserId, collaborationId)

            conversationWsClient?.disconnect()
            conversationWsClient = ChatWebSocketClient(
                token = token,
                currentUserId = currentUserId,
                onMessage = { msg -> addIncoming(msg, currentUserId, otherUserId, collaborationId) }
            )
            conversationWsClient?.connectForConversation(collaborationId)
        }
    }

    private fun addIncoming(
        msg: ChatMessage,
        currentUserId: String,
        otherUserId: String,
        collaborationId: String?
    ) {
        val message = msg.copy(isMe = msg.senderId == currentUserId)
        val existing = _messages.value
        if (existing.none { it.id == message.id }) {
            _messages.value = (existing + message).sortedBy { it.timestamp }
            repository.markMessagesAsRead(otherUserId, collaborationId)
        }
    }

    fun stopConversationWebSocket() {
        conversationWsClient?.disconnect()
        conversationWsClient = null
    }

    fun refreshMessages() {
        val otherUserId = currentOtherUserId ?: return
        val collabId = currentCollaborationId
        val currentUserId = getCurrentUserId()
        viewModelScope.launch {
            val token = getToken() ?: return@launch
            val history = repository.getMessageHistory(otherUserId, collabId, token)
            val incoming = history.map { it.copy(isMe = it.senderId == currentUserId) }
            // Merge: keep existing messages, add any new ones, replace by id
            val merged = (_messages.value + incoming)
                .groupBy { it.id }
                .map { (_, msgs) -> msgs.last() }
                .sortedBy { it.timestamp }
            _messages.value = merged
        }
    }

    // ── Message sending ───────────────────────────────────────────────────────

    fun setReplyingTo(message: ChatMessage?) { _replyingTo.value = message }

    fun sendMessage(text: String, type: String = "TEXT", metadata: Map<String, Any> = emptyMap()) {
        val otherUserId = currentOtherUserId ?: return
        if (text.isBlank() && type == "TEXT") return

        if (type == "TEXT") {
            val reason = ContactInfoFilter.detect(text)
            if (reason != null) {
                _contactInfoWarning.value =
                    "Sharing $reason isn't allowed in chat to keep your collaboration protected on the platform."
                return
            }
        }

        val currentUserId = getCurrentUserId()
        val replyId = _replyingTo.value?.id
        _replyingTo.value = null

        val tempId = "tmp_${UUID.randomUUID()}"
        val optimistic = ChatMessage(
            id              = tempId,
            text            = text,
            senderId        = currentUserId,
            receiverId      = otherUserId,
            timestamp       = System.currentTimeMillis(),
            timeFormatted   = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
            type            = type,
            metadata        = metadata,
            collaborationId = currentCollaborationId,
            replyToId       = replyId,
            isMe            = true
        )
        _messages.value = (_messages.value + optimistic).sortedBy { it.timestamp }

        viewModelScope.launch {
            val token = getToken() ?: run {
                _messages.value = _messages.value.filter { it.id != tempId }
                _error.value = "Authentication error"
                return@launch
            }

            val result = repository.sendMessage(
                receiverId      = otherUserId,
                text            = text,
                replyToId       = replyId,
                type            = type,
                metadata        = metadata,
                collaborationId = currentCollaborationId,
                token           = token
            )

            result.onSuccess { confirmed ->
                val real = confirmed.copy(isMe = true)
                val updated = _messages.value.toMutableList()
                val idx = updated.indexOfFirst { it.id == tempId }
                when {
                    idx >= 0 -> updated[idx] = real
                    updated.none { it.id == real.id } -> updated.add(real)
                }
                _messages.value = updated.sortedBy { it.timestamp }
                updateChatListWithMessage(real, currentUserId)
            }.onFailure {
                Log.e("ChatViewModel", "sendMessage failed: ${it.message}")
                _messages.value = _messages.value.filter { msg -> msg.id != tempId }
                _error.value = "Failed to send: ${it.message}"
            }
        }
    }

    fun sendUpload(link: String, platform: String = "YouTube", onDone: (() -> Unit)? = null) {
        val otherUserId = currentOtherUserId ?: return
        val collaborationId = currentCollaborationId ?: run {
            sendMessage("Content Uploaded", "UPLOAD", mapOf("link" to link, "platform" to platform))
            onDone?.invoke()
            return
        }
        val currentUserId = getCurrentUserId()

        viewModelScope.launch {
            val token = getToken() ?: run { onDone?.invoke(); return@launch }

            when {
                platform.equals("Instagram", ignoreCase = true) -> {
                    val scrapeResult = BackendRepository.scrapeInstagramPost(
                        postUrl = link, collaborationId = collaborationId, token = token
                    )
                    scrapeResult.onSuccess { success ->
                        if (success) {
                            sendMessage("Instagram Post Uploaded", "UPLOAD",
                                mapOf("link" to link, "platform" to "Instagram", "status" to "SCRAPED"))
                        } else {
                            sendMessage("Instagram Post Uploaded (Pending verification)", "UPLOAD",
                                mapOf("link" to link, "platform" to "Instagram"))
                        }
                    }.onFailure {
                        sendMessage("Instagram Post Uploaded", "UPLOAD",
                            mapOf("link" to link, "platform" to "Instagram"))
                    }
                }
                platform.equals("YouTube", ignoreCase = true) -> {
                    val videoResult = BackendRepository.getVideoByUrl(
                        videoUrl = link, userId = currentUserId, collaborationId = collaborationId, token = token
                    )
                    videoResult.onSuccess { data ->
                        sendMessage(
                            text = "Content Uploaded: ${data.optString("title", "Video")}",
                            type = "UPLOAD",
                            metadata = mapOf(
                                "link" to link,
                                "title" to data.optString("title", "Video"),
                                "viewCount" to data.optInt("viewCount", 0),
                                "platform" to "YouTube"
                            )
                        )
                    }.onFailure {
                        sendMessage("Content Uploaded", "UPLOAD", mapOf("link" to link, "platform" to "YouTube"))
                    }
                }
                else -> {
                    // Facebook (and any other future platform) has no scraping/analytics
                    // integration yet — just record the submitted link, same as the
                    // Instagram/YouTube failure fallbacks above.
                    sendMessage("Content Uploaded", "UPLOAD", mapOf("link" to link, "platform" to platform))
                }
            }
            onDone?.invoke()
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        conversationWsClient?.disconnect()
        chatListWsClient?.disconnect()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    fun getCurrentUserId(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private suspend fun getToken(): String? {
        val deferred = CompletableDeferred<String?>()
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(false)
            ?.addOnSuccessListener { deferred.complete(it.token) }
            ?.addOnFailureListener { deferred.complete(null) }
            ?: deferred.complete(null)
        return deferred.await()
    }
}
