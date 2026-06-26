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

    // ── Message history ───────────────────────────────────────────────────────
    // Primary: backend GraphQL query (getChatMessages).
    // Fallback: Firestore one-time get() from the collaboration subcollection.

    suspend fun getMessageHistory(
        otherUserId: String,
        collaborationId: String?,
        token: String
    ): List<ChatMessage> {
        val currentUserId = auth.currentUser?.uid ?: return emptyList()

        val backendResult = BackendRepository.getChatMessages(otherUserId, collaborationId, token)
        if (backendResult.isSuccess) {
            return backendResult.getOrDefault(emptyList()).map { jsonToMessage(it, currentUserId) }
        }

        Log.w("ChatRepository", "Backend getChatMessages failed — falling back to Firestore get()")

        if (collaborationId == null) return emptyList()

        return try {
            val snap = db.collection("collaborations")
                .document(collaborationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val senderId = data["senderId"] as? String ?: return@mapNotNull null
                    val receiverId = data["receiverId"] as? String ?: ""
                    val text = (data["text"] as? String) ?: (data["content"] as? String) ?: ""
                    ChatMessage(
                        id              = doc.id,
                        text            = text,
                        senderId        = senderId,
                        receiverId      = receiverId,
                        timestamp       = (data["timestamp"] as? Long) ?: 0L,
                        timeFormatted   = (data["timeFormatted"] as? String) ?: "",
                        type            = (data["type"] as? String) ?: "TEXT",
                        metadata        = (data["metadata"] as? Map<String, Any>) ?: emptyMap(),
                        collaborationId = collaborationId,
                        replyToId       = data["replyToId"] as? String,
                        isRead          = (data["isRead"] as? Boolean) ?: false,
                        isMe            = senderId == currentUserId
                    )
                } catch (e: Exception) { null }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Firestore fallback failed: ${e.message}")
            emptyList()
        }
    }

    // ── Send message ──────────────────────────────────────────────────────────

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

    // ── Mark as read ──────────────────────────────────────────────────────────
    // Messages are in collaborations/{collabId}/messages/ — not a root collection.

    fun markMessagesAsRead(senderId: String, collaborationId: String? = null) {
        if (collaborationId == null) return
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("collaborations")
            .document(collaborationId)
            .collection("messages")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
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

private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resumeWith(Result.success(it)) }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }
