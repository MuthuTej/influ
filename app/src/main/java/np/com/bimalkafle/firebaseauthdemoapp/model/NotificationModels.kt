package np.com.bimalkafle.firebaseauthdemoapp.model

data class Notification(
    val id: String,
    val userId: String,
    val title: String,
    val body: String,
    val data: String?,
    val isRead: Boolean,
    val createdAt: String
)

data class NotificationResponse(
    val getNotifications: List<Notification>
)

data class UnreadCountResponse(
    val getUnreadNotificationCount: Int
)
