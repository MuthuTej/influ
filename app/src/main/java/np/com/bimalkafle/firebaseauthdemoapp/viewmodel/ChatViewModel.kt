package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatUser
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.repository.ChatRepository
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import org.json.JSONObject
import android.util.Log

data class ChatListEntry(
    val user: ChatUser,
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

    init {
        repository.ensureUserExistsInFirestore()
    }

    fun loadChatList(currentUserRole: String) {
        viewModelScope.launch {
            combine(
                repository.getUsers(currentUserRole),
                repository.getAllMyMessages()
            ) { users, messages ->
                val currentUserId = getCurrentUserId()
                
                val interactedUserIds = messages.flatMap { listOf(it.senderId, it.receiverId) }
                    .filter { it != currentUserId }
                    .toSet()

                val activeUsers = users.filter { it.uid in interactedUserIds }

                activeUsers.map { user ->
                    val conversationMessages = messages.filter { 
                        (it.senderId == user.uid && it.receiverId == currentUserId) ||
                        (it.senderId == currentUserId && it.receiverId == user.uid)
                    }
                    
                    val unreadCount = conversationMessages.count { 
                         it.senderId == user.uid && !it.isRead 
                    }
                    
                    val lastMsgObj = conversationMessages.maxByOrNull { it.timestamp }
                    val lastMessageText = lastMsgObj?.text ?: ""

                    ChatListEntry(
                        user = user,
                        unreadCount = unreadCount,
                        lastMessage = lastMessageText
                    )
                }
            }.collectLatest { 
                _chatList.value = it 
            }
        }
    }

    fun initChat(otherUserId: String, otherUserName: String = "Chat", collaborationId: String? = null) {
        currentOtherUserId = otherUserId
        currentCollaborationId = collaborationId
        _chatName.value = otherUserName
        
        viewModelScope.launch {
            val currentUserId = getCurrentUserId()
            repository.getMessages(otherUserId, collaborationId).collectLatest { list ->
                _messages.value = list.map { it.copy(isMe = it.senderId == currentUserId) }
                repository.markMessagesAsRead(otherUserId, collaborationId)
            }
        }
    }

    fun setReplyingTo(message: ChatMessage?) {
        _replyingTo.value = message
    }

    fun sendMessage(
        text: String,
        type: String = "TEXT",
        metadata: Map<String, Any> = emptyMap()
    ) {
        val otherUserId = currentOtherUserId ?: return
        if (text.isBlank() && type == "TEXT") return

        repository.sendMessage(
            receiverId = otherUserId,
            text = text,
            replyToId = _replyingTo.value?.id,
            type = type,
            metadata = metadata,
            collaborationId = currentCollaborationId
        )
        _replyingTo.value = null
    }

    fun sendUpload(link: String, platform: String = "YouTube") {
        val otherUserId = currentOtherUserId ?: return
        val collaborationId = currentCollaborationId ?: run {
            sendMessage("Content Uploaded", "UPLOAD", mapOf("link" to link, "platform" to platform))
            return
        }

        val currentUserId = getCurrentUserId()
        
        viewModelScope.launch {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                val token = result.token ?: return@addOnSuccessListener
                
                viewModelScope.launch {
                    if (platform.equals("Instagram", ignoreCase = true)) {
                        // Instagram Scraper Flow
                        val scrapeResult = BackendRepository.scrapeInstagramPost(
                            postUrl = link,
                            collaborationId = collaborationId,
                            token = token
                        )
                        
                        scrapeResult.onSuccess { success ->
                            if (success) {
                                sendMessage(
                                    text = "Instagram Post Uploaded",
                                    type = "UPLOAD",
                                    metadata = mapOf(
                                        "link" to link,
                                        "platform" to "Instagram",
                                        "status" to "SCRAPED"
                                    )
                                )
                            } else {
                                Log.e("ChatViewModel", "Instagram scrape returned false")
                                sendMessage("Instagram Post Uploaded (Pending verification)", "UPLOAD", mapOf("link" to link, "platform" to "Instagram"))
                            }
                        }.onFailure { e ->
                            Log.e("ChatViewModel", "Instagram scrape failed: ${e.message}")
                            sendMessage("Instagram Post Uploaded", "UPLOAD", mapOf("link" to link, "platform" to "Instagram"))
                        }
                    } else {
                        // YouTube (Default) Flow
                        val videoResult = BackendRepository.getVideoByUrl(
                            videoUrl = link,
                            userId = currentUserId,
                            collaborationId = collaborationId,
                            token = token
                        )
                        
                        videoResult.onSuccess { data ->
                            val title = data.optString("title", "Video")
                            val viewCount = data.optInt("viewCount", 0)
                            
                            sendMessage(
                                text = "Content Uploaded: $title",
                                type = "UPLOAD",
                                metadata = mapOf(
                                    "link" to link,
                                    "title" to title,
                                    "viewCount" to viewCount,
                                    "platform" to "YouTube"
                                )
                            )
                        }.onFailure { e ->
                            Log.e("ChatViewModel", "Failed to fetch video info: ${e.message}")
                            sendMessage("Content Uploaded", "UPLOAD", mapOf("link" to link, "platform" to "YouTube"))
                        }
                    }
                }
            }?.addOnFailureListener {
                sendMessage("Content Uploaded", "UPLOAD", mapOf("link" to link, "platform" to platform))
            }
        }
    }
    
    fun updateMessageStatus(messageId: String, status: String) {
        repository.updateMessageStatus(messageId, status)
    }

    fun getCurrentUserId(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
}
