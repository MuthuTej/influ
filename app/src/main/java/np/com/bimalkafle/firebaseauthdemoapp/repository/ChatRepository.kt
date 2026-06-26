package np.com.bimalkafle.firebaseauthdemoapp.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatUser
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import org.json.JSONObject

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Users ─────────────────────────────────────────────────────────────────

    fun getUsersOnce(
        currentUserRole: String,
        onError: (String) -> Unit = {},
        callback: (List<ChatUser>) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid
        val collection = if (currentUserRole.equals("BRAND", ignoreCase = true)) "influencers" else "brands"

        db.collection(collection).get()
            .addOnSuccessListener { snapshot ->
                val users = snapshot.documents.mapNotNull { doc ->
                    try {
                        val uid = doc.getString("id") ?: doc.id
                        if (uid == currentUserId) return@mapNotNull null
                        ChatUser(
                            uid = uid,
                            name = doc.getString("name") ?: "Unknown",
                            email = doc.getString("email") ?: "",
                            profileImageUrl = doc.getString("logoUrl")
                        )
                    } catch (e: Exception) { null }
                }
                callback(users)
            }
            .addOnFailureListener {
                onError(it.message ?: "Failed to load contacts")
                callback(emptyList())
            }
    }

    // ── Message history ───────────────────────────────────────────────────────
    // Primary: backend GraphQL query (getChatMessages).
    // Fallback: Firestore one-time get() — used if the backend query fails or
    // the endpoint is not yet deployed. Neither path uses addSnapshotListener.

    suspend fun getMessageHistory(
        otherUserId: String,
        collaborationId: String?,
        token: String
    ): List<ChatMessage> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()

        // Try backend query first
        val backendResult = BackendRepository.getChatMessages(otherUserId, collaborationId, token)
        if (backendResult.isSuccess) {
            return backendResult.getOrDefault(emptyList()).map { jsonToMessage(it, currentUserId) }
        }

        Log.w("ChatRepository", "Backend getChatMessages failed — falling back to Firestore get()")

        // Firestore one-time get() fallback (no listener)
        return try {
            val snap = db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()
            snap.documents
                .mapNotNull { it.toObject(ChatMessage::class.java) }
                .filter {
                    val between = (it.senderId == currentUserId && it.receiverId == otherUserId) ||
                                  (it.senderId == otherUserId && it.receiverId == currentUserId)
                    between && it.collaborationId == collaborationId
                }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Firestore fallback failed: ${e.message}")
            emptyList()
        }
    }

    // ── Send message ──────────────────────────────────────────────────────────
    // Routes through the backend mutation so the backend can broadcast the
    // message to the receiver's WebSocket subscription immediately.
    // Returns the confirmed message (with its backend-assigned ID) so the
    // caller can do an accurate optimistic insert.

    suspend fun sendMessage(
        receiverId: String,
        text: String,
        replyToId: String? = null,
        type: String = "TEXT",
        metadata: Map<String, Any> = emptyMap(),
        collaborationId: String? = null,
        token: String
    ): Result<ChatMessage> {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val result = BackendRepository.sendChatMessage(
            receiverId = receiverId,
            text = text,
            type = type,
            metadata = metadata,
            collaborationId = collaborationId,
            replyToId = replyToId,
            token = token
        )
        return result.map { jsonToMessage(it, currentUserId) }
    }

    // ── Chat list (one-time) ──────────────────────────────────────────────────
    // No listener — initial snapshot only. Real-time updates come from the
    // WebSocket subscription managed by ChatViewModel.

    fun getAllMessagesOnce(
        onError: (String) -> Unit = {},
        callback: (List<ChatMessage>) -> Unit
    ) {
        val currentUserId = auth.currentUser?.uid ?: run { callback(emptyList()); return }

        db.collection("messages").whereEqualTo("senderId", currentUserId).get()
            .addOnSuccessListener { sentSnap ->
                val sent = sentSnap.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                db.collection("messages").whereEqualTo("receiverId", currentUserId).get()
                    .addOnSuccessListener { receivedSnap ->
                        val received = receivedSnap.documents.mapNotNull { it.toObject(ChatMessage::class.java) }
                        callback((sent + received).sortedByDescending { it.timestamp })
                    }
                    .addOnFailureListener {
                        onError(it.message ?: "Failed to load received messages")
                        callback(sent.sortedByDescending { it.timestamp })
                    }
            }
            .addOnFailureListener {
                onError(it.message ?: "Failed to load messages")
                callback(emptyList())
            }
    }

    // ── Mark as read ──────────────────────────────────────────────────────────

    fun markMessagesAsRead(senderId: String, collaborationId: String? = null) {
        val currentUserId = auth.currentUser?.uid ?: return
        db.collection("messages")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .whereEqualTo("collaborationId", collaborationId)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) doc.reference.update("isRead", true)
            }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun jsonToMessage(json: JSONObject, currentUserId: String): ChatMessage {
        val meta = mutableMapOf<String, Any>()
        json.optJSONObject("metadata")?.let { m -> m.keys().forEach { k -> meta[k] = m.get(k) } }
        return ChatMessage(
            id              = json.optString("id"),
            text            = json.optString("text"),
            senderId        = json.optString("senderId"),
            receiverId      = json.optString("receiverId"),
            timestamp       = json.optLong("timestamp"),
            timeFormatted   = json.optString("timeFormatted"),
            type            = json.optString("type", "TEXT"),
            metadata        = meta,
            collaborationId = json.optString("collaborationId").takeIf { it.isNotBlank() },
            replyToId       = json.optString("replyToId").takeIf { it.isNotBlank() },
            isRead          = json.optBoolean("isRead", false),
            isMe            = json.optString("senderId") == currentUserId
        )
    }

    fun ensureUserExistsInFirestore() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val userRef = db.collection("users").document(uid)
        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                userRef.set(
                    ChatUser(
                        uid = uid,
                        email = currentUser.email ?: "",
                        name = currentUser.displayName
                            ?: currentUser.email?.substringBefore("@") ?: "User",
                        profileImageUrl = currentUser.photoUrl?.toString()
                    )
                )
            }
        }
    }
}

// Firestore Task<QuerySnapshot>.await() — inline coroutine bridge without
// requiring the full kotlinx-coroutines-play-services artifact.
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resumeWith(Result.success(it)) }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }
