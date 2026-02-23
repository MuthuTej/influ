package np.com.bimalkafle.firebaseauthdemoapp.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatUser

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun getUsers(currentUserRole: String): Flow<List<ChatUser>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid
        
        val targetCollection = if (currentUserRole.equals("BRAND", ignoreCase = true)) {
            "influencers"
        } else {
            "brands"
        }

        val subscription = db.collection(targetCollection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching users from $targetCollection: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val users = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val uid = doc.getString("id") ?: doc.id
                        val name = doc.getString("name") ?: "Unknown"
                        val email = doc.getString("email") ?: ""
                        val photoUrl = doc.getString("logoUrl")
                        
                        if (uid != currentUserId) {
                            ChatUser(
                                uid = uid,
                                name = name,
                                email = email,
                                profileImageUrl = photoUrl
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error mapping document to ChatUser", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(users)
            }
        awaitClose { subscription.remove() }
    }

    fun getMessages(otherUserId: String, collaborationId: String? = null): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            return@callbackFlow
        }

        val subscription = db.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching messages: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) }
                    ?.filter {
                        val isBetweenUsers = (it.senderId == currentUserId && it.receiverId == otherUserId) ||
                                           (it.senderId == otherUserId && it.receiverId == currentUserId)
                        
                        val isSameCollaboration = it.collaborationId == collaborationId
                        
                        isBetweenUsers && isSameCollaboration
                    } ?: emptyList()
                
                trySend(messages)
            }
        awaitClose { subscription.remove() }
    }

    fun getAllMyMessages(): Flow<List<ChatMessage>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: run {
            trySend(emptyList())
            return@callbackFlow
        }

        var sentMessages: List<ChatMessage> = emptyList()
        var receivedMessages: List<ChatMessage> = emptyList()

        fun emitCombined() {
            val allMessages = (sentMessages + receivedMessages).sortedByDescending { it.timestamp }
            trySend(allMessages)
        }

        val sentRegistration = db.collection("messages")
            .whereEqualTo("senderId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching sent messages: ${error.message}")
                    return@addSnapshotListener
                }
                sentMessages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                emitCombined()
            }

        val receivedRegistration = db.collection("messages")
            .whereEqualTo("receiverId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error fetching received messages: ${error.message}")
                    return@addSnapshotListener
                }
                receivedMessages = snapshot?.documents?.mapNotNull { it.toObject(ChatMessage::class.java) } ?: emptyList()
                emitCombined()
            }

        awaitClose {
            sentRegistration.remove()
            receivedRegistration.remove()
        }
    }

    fun markMessagesAsRead(senderId: String, collaborationId: String? = null) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("messages")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .whereEqualTo("collaborationId", collaborationId)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.update("isRead", true)
                }
            }
    }

    fun sendMessage(
        receiverId: String, 
        text: String, 
        replyToId: String? = null,
        type: String = "TEXT",
        metadata: Map<String, Any> = emptyMap(),
        collaborationId: String? = null
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val messageId = db.collection("messages").document().id
        val timestamp = System.currentTimeMillis()

        val message = ChatMessage(
            id = messageId,
            text = text,
            senderId = currentUserId,
            receiverId = receiverId,
            timestamp = timestamp,
            timeFormatted = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp)),
            replyToId = replyToId,
            isRead = false,
            type = type,
            metadata = metadata,
            collaborationId = collaborationId
        )

        db.collection("messages").document(messageId).set(message)
    }

    fun ensureUserExistsInFirestore() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val email = currentUser.email ?: ""
        val name = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"
        val photoUrl = currentUser.photoUrl?.toString()

        val userRef = db.collection("users").document(uid)
        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val chatUser = ChatUser(
                    uid = uid,
                    email = email,
                    name = name,
                    profileImageUrl = photoUrl
                )
                userRef.set(chatUser)
            }
        }
    }

    fun updateMessageStatus(messageId: String, status: String) {
        db.collection("messages").document(messageId)
            .update("status", status)
            .addOnFailureListener { e ->
                Log.e("ChatRepository", "Error updating message status", e)
            }
    }
}
