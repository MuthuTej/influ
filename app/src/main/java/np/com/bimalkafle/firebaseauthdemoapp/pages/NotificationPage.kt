package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.model.Notification
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val themeColor = Color(0xFFFF8383)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPage(
    navController: NavController,
    notificationViewModel: NotificationViewModel
) {
    val notifications by notificationViewModel.notifications.observeAsState(emptyList())
    val isLoading by notificationViewModel.loading.observeAsState(false)
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)

    val sortedNotifications = remember(notifications) {
        notifications.sortedWith(
            compareBy<Notification> { it.isRead }
                .thenByDescending { it.createdAt }
        )
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            result.token?.let { token ->
                notificationViewModel.fetchNotifications(currentUser.uid, token)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Notifications", fontWeight = FontWeight.Bold)
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount unread",
                                fontSize = 12.sp,
                                color = themeColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        IconButton(onClick = {
                            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                result.token?.let { token ->
                                    notificationViewModel.markAllAsRead(token)
                                }
                            }
                        }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all as read", tint = themeColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FE))
        ) {
            if (isLoading && notifications.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = themeColor)
            } else if (notifications.isEmpty()) {
                EmptyNotifications(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedNotifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = {
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                    result.token?.let { token ->
                                        if (!notification.isRead) {
                                            notificationViewModel.markAsRead(token, notification.id)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = if (!notification.isRead) BorderStroke(1.dp, themeColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(if (notification.isRead) 1.dp else 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (notification.isRead) Color.LightGray.copy(alpha = 0.2f) else themeColor.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (notification.isRead) Icons.Default.NotificationsNone else Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (notification.isRead) Color.Gray else themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatNotificationDate(notification.createdAt),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.body,
                    fontSize = 14.sp,
                    color = if (notification.isRead) Color.Gray else Color.DarkGray,
                    lineHeight = 20.sp
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(10.dp)
                        .background(themeColor, CircleShape)
                )
            }
        }
    }
}

@Composable
fun EmptyNotifications(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.NotificationsNone,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No notifications yet",
            fontSize = 18.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Text(
            "We'll notify you when something important happens",
            fontSize = 14.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}

fun formatNotificationDate(dateString: String): String {
    return try {
        val instant = Instant.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        if (dateString.length > 10) dateString.substring(0, 10) else dateString
    }
}
