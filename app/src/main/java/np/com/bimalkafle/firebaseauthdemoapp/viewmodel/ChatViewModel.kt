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

data class ChatListEntry(
    val user: ChatUser,
    val unreadCount: Int,
    val lastMessage: String
)

class ChatViewModel : ViewModel() {

    private val repository = ChatRepository()
    private var currentOtherUserId: String? = null

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
        // We will load chat list manually or when role is available
    }

    fun loadChatList(currentUserRole: String) {
        viewModelScope.launch {
            combine(
                repository.getUsers(currentUserRole),
                repository.getAllMyMessages()
            ) { users, messages ->
                val currentUserId = getCurrentUserId()
                
                // Identify users we have interacted with (sent or received messages)
                val interactedUserIds = messages.flatMap { listOf(it.senderId, it.receiverId) }
                    .filter { it != currentUserId }
                    .toSet()

                // Filter the list of users to only include those we have interacted with
                val activeUsers = users.filter { it.uid in interactedUserIds }

                activeUsers.map { user ->
                    // Get all messages between me and this user
                    val conversationMessages = messages.filter { 
                        (it.senderId == user.uid && it.receiverId == currentUserId) ||
                        (it.senderId == currentUserId && it.receiverId == user.uid)
                    }
                    
                    // Count unread messages (received from them and not read)
                    val unreadCount = conversationMessages.count { 
                         it.senderId == user.uid && !it.isRead 
                    }
                    
                    // Get the very last message in the conversation for the preview
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

    fun initChat(otherUserId: String, otherUserName: String = "Chat") {
        currentOtherUserId = otherUserId
        _chatName.value = otherUserName
        
        viewModelScope.launch {
            val currentUserId = getCurrentUserId()
            repository.getMessages(otherUserId).collectLatest { list ->
                _messages.value = list.map { it.copy(isMe = it.senderId == currentUserId) }
                // Mark messages as read whenever the list updates and we're in the chat
                repository.markMessagesAsRead(otherUserId)
            }
        }
    }

    fun setReplyingTo(message: ChatMessage?) {
        _replyingTo.value = message
    }

    fun sendMessage(text: String) {
        val otherUserId = currentOtherUserId ?: return
        if (text.isBlank()) return

        repository.sendMessage(
            receiverId = otherUserId,
            text = text,
            replyToId = _replyingTo.value?.id
        )
        _replyingTo.value = null
    }
    
    fun getCurrentUserId(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
}
