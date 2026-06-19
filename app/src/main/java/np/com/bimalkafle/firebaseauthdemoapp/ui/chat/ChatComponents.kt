package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.components.NotificationBadge
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatItem
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.Dimens
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Remove

val BrandThemeColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary
val ChatBubbleSelfColor: Color
    @Composable get() = BrandThemeColor
val ChatBubbleOtherColor = Color(0xFFF2F2F7) // Light Gray for iOS-like feel

@Composable
fun ChatListItem(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Dimens.space16, vertical = Dimens.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Profile icon/image
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (chat.profileImageUrl != null) {
                    AsyncImage(
                        model = chat.profileImageUrl,
                        contentDescription = "${chat.name}'s profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "${chat.name}'s profile photo",
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.space16))

            // Name + last message
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimens.space4))
                Text(
                    text = chat.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Time + unread count
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = chat.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(Dimens.space8))

                NotificationBadge(count = chat.unreadCount)
            }
        }
    }
}

@Composable
fun ChatTopBar(
    chatName: String,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Surface(
        shadowElevation = 4.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 🔙 Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            // 👤 Profile image (clickable)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    tint = Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 👤 Name + status
            Column {
                Text(
                    text = chatName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.Black
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Online", 
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    allMessages: List<ChatMessage>,
    isGroupStart: Boolean = true,
    isGroupEnd: Boolean = true,
    onSwipeToReply: () -> Unit = {}
) {
    val isMe = message.isMe
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 100f

    // Find the message being replied to
    val repliedMessage = remember(message.replyToId, allMessages) {
        allMessages.find { it.id == message.replyToId }
    }

    // The sharp "tail" corner only appears on the message that starts a
    // consecutive run from the same sender; corners facing a same-sender
    // neighbor are squared off so the run reads as one continuous block
    // instead of a stack of identical standalone bubbles.
    val cornerLarge = 18.dp
    val cornerSmall = 4.dp
    val bubbleShape = if (isMe) {
        RoundedCornerShape(
            topStart = cornerLarge,
            topEnd = if (isGroupStart) cornerSmall else cornerLarge,
            bottomStart = cornerLarge,
            bottomEnd = if (isGroupEnd) cornerLarge else cornerSmall
        )
    } else {
        RoundedCornerShape(
            topStart = if (isGroupStart) cornerSmall else cornerLarge,
            topEnd = cornerLarge,
            bottomStart = if (isGroupEnd) cornerLarge else cornerSmall,
            bottomEnd = cornerLarge
        )
    }
    val onBubbleColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val maxBubbleWidth = (LocalConfiguration.current.screenWidthDp * 0.78f).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (isGroupStart) Dimens.space8 else Dimens.space2,
                bottom = Dimens.space2,
                start = Dimens.space12,
                end = Dimens.space12
            )
    ) {
        if (offsetX.value > 0) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Reply",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = Dimens.space16)
                    .size(24.dp),
                tint = if (offsetX.value > swipeThreshold) BrandThemeColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (delta > 0 || offsetX.value > 0) {
                            scope.launch {
                                offsetX.snapTo((offsetX.value + delta).coerceIn(0f, 150f))
                            }
                        }
                    },
                    onDragStopped = {
                        if (offsetX.value > swipeThreshold) {
                            onSwipeToReply()
                        }
                        scope.launch {
                            offsetX.animateTo(0f)
                        }
                    }
                ),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            Surface(
                shape = bubbleShape,
                color = if (isMe) ChatBubbleSelfColor else ChatBubbleOtherColor,
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = maxBubbleWidth)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8)
                ) {
                    // --- Quotation Box for Replies ---
                    if (repliedMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Dimens.space8)
                                .background(
                                    color = if (isMe) Color.Black.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.04f),
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(Dimens.space8)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(30.dp)
                                    .background(if (isMe) onBubbleColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Column {
                                Text(
                                    text = if (repliedMessage.isMe) "You" else "Responder",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) onBubbleColor.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = repliedMessage.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = onBubbleColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    // ---------------------------------

                    if (message.type == "TEXT") {
                        Text(
                            text = message.text,
                            color = onBubbleColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        when (message.type) {
                            "NEGOTIATION" -> NegotiationBubble(message, isMe)
                            "DELIVERABLES" -> DeliverablesBubble(message, isMe)
                            "BRIEF" -> RichContentCard(Icons.Default.Description, "Campaign Brief", isMe, content = message.metadata["link"]?.toString() ?: message.text)
                            "SCRIPT" -> RichContentCard(Icons.Default.EditNote, "Script", isMe, content = message.metadata["content"]?.toString() ?: message.text)
                            "FEEDBACK" -> RichContentCard(Icons.Default.Feedback, "Feedback", isMe, content = message.metadata["feedback"]?.toString() ?: message.text)
                            "UPLOAD" -> RichContentCard(Icons.Default.CloudUpload, "Upload", isMe, content = message.metadata["link"]?.toString() ?: message.text)
                            else -> Text(text = message.text, color = onBubbleColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimens.space4))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = message.timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = onBubbleColor.copy(alpha = 0.65f)
                        )
                        if (isMe) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                modifier = Modifier.size(12.dp),
                                tint = onBubbleColor.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standalone "Replying to X" banner, extracted from MessageInputBar so it can sit
 * above whichever input/action bar a screen actually uses (RestrictedActionPanel),
 * since MessageInputBar's own TextField isn't the one wired up in ChatScreen.
 */
@Composable
fun ReplyPreviewBanner(
    replyingTo: ChatMessage?,
    chatName: String,
    onCancelReply: () -> Unit
) {
    AnimatedVisibility(visible = replyingTo != null) {
        if (replyingTo != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Replying to ${if (replyingTo.isMe) "You" else chatName}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = BrandThemeColor
                    )
                    Text(
                        text = replyingTo.text,
                        maxLines = 1,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel reply",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MessageInputBar(
    replyingTo: ChatMessage?,
    chatName: String,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit,
    onCreateProposal: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Surface(
        shadowElevation = 8.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            AnimatedVisibility(visible = replyingTo != null) {
                if (replyingTo != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${if (replyingTo.isMe) "You" else chatName}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = BrandThemeColor
                            )
                            Text(
                                text = replyingTo.text,
                                maxLines = 1,
                                fontSize = 12.sp,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.Gray
                            )
                        }
                        IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCreateProposal,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF5F5F5), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Proposal",
                        tint = BrandThemeColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Write a message...", color = Color.Gray) },
                    textStyle = TextStyle(color = Color.Black, fontSize = 15.sp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BrandThemeColor
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                val isTextEmpty = text.isBlank()
                IconButton(
                    onClick = {
                        if (!isTextEmpty) {
                            onSend(text)
                            text = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isTextEmpty) Color(0xFFE0E0E0) else BrandThemeColor,
                            CircleShape
                        ),
                    enabled = !isTextEmpty
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


/**
 * Shared shell for every "rich" message type (Brief, Script, Negotiation,
 * Deliverables, Feedback, Upload) — one consistent icon/title/content/list
 * treatment instead of four near-identical hand-rolled cards. Purely
 * informational: the real accept/reject/progress actions live in the
 * collaboration's Actions panel, not here.
 */
@Composable
fun RichContentCard(
    icon: ImageVector,
    title: String,
    isMe: Boolean,
    content: String? = null,
    highlight: String? = null,
    items: List<String> = emptyList()
) {
    val onCardColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) onCardColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Dimens.space12)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isMe) onCardColor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            if (isMe) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .padding(Dimens.space4)
                )
                Spacer(modifier = Modifier.width(Dimens.space8))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = onCardColor
                )
            }

            if (highlight != null) {
                Spacer(modifier = Modifier.height(Dimens.space4))
                Text(
                    text = highlight,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isMe) onCardColor else MaterialTheme.colorScheme.primary
                )
            }

            if (content != null) {
                Spacer(modifier = Modifier.height(Dimens.space4))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCardColor.copy(alpha = 0.85f)
                )
            }

            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.space8))
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(onCardColor.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space8))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = onCardColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NegotiationBubble(message: ChatMessage, isMe: Boolean) {
    val amount = message.metadata?.get("amount")?.toString() ?: "0"
    val platform = message.metadata?.get("platform")?.toString() ?: ""
    val itemsMap = message.metadata?.get("items") as? Map<String, Any> ?: emptyMap()
    val displayItems = itemsMap.entries.map { "${it.key} (x${it.value})" }

    RichContentCard(
        icon = Icons.Default.AttachMoney,
        title = if (platform.isNotEmpty()) "Proposal on $platform" else "Proposal",
        isMe = isMe,
        highlight = "₹$amount",
        items = displayItems
    )
}

@Composable
fun DeliverablesBubble(message: ChatMessage, isMe: Boolean) {
    val itemsMap = message.metadata?.get("items") as? Map<String, Any> ?: emptyMap()
    val displayItems = if (itemsMap.isNotEmpty()) {
        itemsMap.entries.map { "${it.key} (x${it.value})" }
    } else {
        message.metadata?.get("items") as? List<String> ?: emptyList()
    }

    RichContentCard(
        icon = Icons.Default.List,
        title = "Deliverables",
        isMe = isMe,
        items = displayItems
    )
}
