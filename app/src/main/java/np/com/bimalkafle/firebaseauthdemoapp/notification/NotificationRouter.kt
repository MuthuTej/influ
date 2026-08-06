package np.com.bimalkafle.firebaseauthdemoapp.notification

import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository

/**
 * Resolves a notification's `type` (+ its id fields) to the in-app route it should redirect
 * to, or null if it shouldn't navigate anywhere at all. Shared by the FCM tap handler
 * (MainActivity, via NotificationNavigationEvent) and the in-app notification list
 * (NotificationPage), so both send the user to the same place.
 * Keep the `type` values in sync with connect-backend's src/constants/notificationTypes.js.
 */
object NotificationRouter {

    // Notification types that refer to a specific collaboration and should redirect straight
    // into that collaboration's chat thread rather than just the proposals/history list.
    private val COLLABORATION_CHAT_TYPES = setOf(
        "PROPOSAL_RECEIVED",
        "CAMPAIGN_INVITATION",
        "CANCELLATION_REQUESTED",
        "CANCELLATION_RESOLVED",
        "COLLABORATION_STATUS_UPDATE",
        "COLLABORATION_SECURED",
        "CAMPAIGN_REMINDER",
    )

    // Payment-release notifications always go to the proposals/history list — never the chat
    // thread — regardless of whether a collaborationId is present.
    private val HISTORY_TYPES = setOf(
        "PAYMENT_RELEASE_REQUESTED",
        "PAYMENT_RELEASE_RESOLVED",
    )

    // Types that should never navigate anywhere on tap — the notification text is enough.
    private val STATIC_TYPES = setOf(
        "REVIEW_RECEIVED",
    )

    suspend fun resolveRoute(type: String?, data: Map<String, String?>, token: String?): String? {
        if (type in STATIC_TYPES) {
            return null
        }

        if (type == "CAMPAIGN_MATCH") {
            return data["campaignId"]?.let { "campaign_detail/$it" } ?: "notifications"
        }

        if (type in HISTORY_TYPES) {
            return "proposals"
        }

        val collaborationId = data["collaborationId"]
        if (type in COLLABORATION_CHAT_TYPES && collaborationId != null) {
            chatRouteFor(collaborationId, token)?.let { return it }
            return "proposals"
        }

        return "notifications"
    }

    // The chat route needs the *other* participant's id/name, not the collaboration id, so
    // this looks the collaboration up and figures out who that is relative to the signed-in
    // user (works for both BRAND and INFLUENCER — getCollaborationById authorizes either).
    // Returns null if we're signed out, offline, or the lookup fails — callers fall back to
    // the proposals list in that case.
    private suspend fun chatRouteFor(collaborationId: String, token: String?): String? {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        if (token == null) return null

        val collab = BackendRepository.getCollaborationById(collaborationId, token).getOrNull() ?: return null
        val brandId = collab.optString("brandId", "")
        val influencerId = collab.optString("influencerId", "")
        val isBrand = currentUid == brandId

        val otherUserId = if (isBrand) influencerId else brandId
        if (otherUserId.isBlank()) return null

        val otherUserName = (
            if (isBrand) collab.optJSONObject("influencer")?.optString("name")
            else collab.optJSONObject("brand")?.optString("name")
        )?.takeIf { it.isNotBlank() } ?: "Chat"

        return "chat/$otherUserId/$otherUserName?collaborationId=$collaborationId"
    }
}
