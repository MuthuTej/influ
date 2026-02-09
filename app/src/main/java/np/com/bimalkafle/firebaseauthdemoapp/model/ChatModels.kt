package np.com.bimalkafle.firebaseauthdemoapp.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class ChatItem(
    val chatId: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int,
    val profileImageUrl: String? = null // Changed from Res Int to String URL
)

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val timestamp: Long = 0,
    val timeFormatted: String = "",
    val replyToId: String? = null,
    
    @get:Exclude
    val isMe: Boolean = false,

    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false
)

data class ChatUser(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val profileImageUrl: String? = null
)
