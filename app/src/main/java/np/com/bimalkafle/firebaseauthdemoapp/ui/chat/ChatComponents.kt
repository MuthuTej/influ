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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatItem
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
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

val BrandThemeColor = Color(0xFFFF8383)
val ChatBubbleSelfColor = BrandThemeColor
val ChatBubbleOtherColor = Color(0xFFF2F2F7) // Light Gray for iOS-like feel

@Composable
fun ActionButtons(
    status: String,
    isMe: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onModify: () -> Unit
) {
    if (status == "PENDING" && !isMe) {
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Accept", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)), // Softer Red
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reject", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onModify,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF42A5F5)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF42A5F5)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Modify", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    } else if (status != "PENDING") {
        Surface(
            color = if (status == "ACCEPTED") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "${status.lowercase().replaceFirstChar { it.uppercase() }}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (status == "ACCEPTED") Color(0xFF2E7D32) else Color(0xFFC62828),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ChatListItem(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 👤 Profile icon/image
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (chat.profileImageUrl != null) {
                    AsyncImage(
                        model = chat.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 📝 Name + last message
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A1A)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chat.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (chat.unreadCount > 0) Color.Black else Color.Gray,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }

            // ⏰ Time + unread count
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = chat.time,
                    fontSize = 12.sp,
                    color = if (chat.unreadCount > 0) BrandThemeColor else Color.Gray,
                    fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(BrandThemeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
    onSwipeToReply: () -> Unit = {},
    onUpdateStatus: (String, String) -> Unit = { _, _ -> },
    onModify: (ChatMessage) -> Unit = {}
) {
    val isMe = message.isMe
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 100f

    // Find the message being replied to
    val repliedMessage = remember(message.replyToId, allMessages) {
        allMessages.find { it.id == message.replyToId }
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 12.dp)
    ) {
        if (offsetX.value > 0) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Reply",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(24.dp),
                tint = if (offsetX.value > swipeThreshold) BrandThemeColor else Color.Gray
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
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // --- Quotation Box for Replies ---
                    if (repliedMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .background(
                                    color = if (isMe) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(30.dp)
                                    .background(if (isMe) Color.White.copy(alpha = 0.7f) else BrandThemeColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (repliedMessage.isMe) "You" else "Responder",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isMe) Color.White.copy(alpha = 0.9f) else BrandThemeColor
                                )
                                Text(
                                    text = repliedMessage.text,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                                )
                            }
                        }
                    }
                    // ---------------------------------

                    if (message.type == "TEXT") {
                        Text(
                            text = message.text,
                            color = if (isMe) Color.White else Color.Black,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                    } else {
                        when (message.type) {
                            "NEGOTIATION" -> NegotiationBubble(message, isMe, onUpdateStatus) { onModify(message) }
                            "DELIVERABLES" -> DeliverablesBubble(message, isMe, onUpdateStatus) { onModify(message) }
                            "BRIEF" -> ActionBubble(message, "Campaign Brief", Icons.Default.Description, isMe, onUpdateStatus) { onModify(message) }
                            "SCRIPT" -> ActionBubble(message, "Script", Icons.Default.EditNote, isMe, onUpdateStatus) { onModify(message) }
                            "FEEDBACK" -> ActionBubble(message, "Feedback", Icons.Default.Feedback, isMe, onUpdateStatus) { onModify(message) }
                            "UPLOAD" -> ActionBubble(message, "Upload", Icons.Default.CloudUpload, isMe, onUpdateStatus) { onModify(message) }
                            else -> Text(
                                text = message.text,
                                color = if (isMe) Color.White else Color.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timeFormatted,
                        fontSize = 10.sp,
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                        modifier = Modifier.align(Alignment.End)
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


@Composable
fun NegotiationBubble(
    message: ChatMessage, 
    isMe: Boolean,
    onUpdateStatus: (String, String) -> Unit,
    onModify: () -> Unit
) {
    val amount = message.metadata?.get("amount")?.toString() ?: "0"
    
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if(isMe) Color.White.copy(alpha = 0.2f) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AttachMoney, 
                    contentDescription = null, 
                    tint = if(isMe) Color.White else BrandThemeColor,
                    modifier = Modifier.background(
                        if(isMe) Color.Transparent else BrandThemeColor.copy(alpha = 0.1f), 
                        CircleShape
                    ).padding(4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Proposal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isMe) Color.White else Color.Black
                    )
                    Text(
                        text = "$$amount",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = if (isMe) Color.White else BrandThemeColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = if(isMe) Color.White.copy(alpha=0.2f) else Color.LightGray.copy(alpha=0.2f))
            
            ActionButtons(
                status = message.status,
                isMe = isMe,
                onAccept = { onUpdateStatus(message.id, "ACCEPTED") },
                onReject = { onUpdateStatus(message.id, "REJECTED") },
                onModify = onModify
            )
        }
    }
}

@Composable
fun DeliverablesBubble(
    message: ChatMessage, 
    isMe: Boolean,
    onUpdateStatus: (String, String) -> Unit,
    onModify: () -> Unit
) {
    val itemsMap = message.metadata?.get("items") as? Map<String, Any> ?: emptyMap()
    
    val displayItems = if (itemsMap.isNotEmpty()) {
        itemsMap.entries.map { "${it.key} k(x${it.value})" }
    } else {
        message.metadata?.get("items") as? List<String> ?: emptyList()
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if(isMe) Color.White.copy(alpha = 0.2f) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.List, 
                    contentDescription = null, 
                    tint = if(isMe) Color.White else BrandThemeColor,
                    modifier = Modifier.background(
                        if(isMe) Color.Transparent else BrandThemeColor.copy(alpha = 0.1f), 
                        CircleShape
                    ).padding(4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Deliverables",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isMe) Color.White else Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            displayItems.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(if(isMe) Color.White.copy(alpha=0.7f) else Color.Gray, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item,
                        color = if (isMe) Color.White.copy(alpha = 0.9f) else Color.DarkGray,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = if(isMe) Color.White.copy(alpha=0.2f) else Color.LightGray.copy(alpha=0.2f))

            ActionButtons(
                status = message.status,
                isMe = isMe,
                onAccept = { onUpdateStatus(message.id, "ACCEPTED") },
                onReject = { onUpdateStatus(message.id, "REJECTED") },
                onModify = onModify
            )
        }
    }
}

@Composable
fun ActionBubble(
    message: ChatMessage, 
    title: String, 
    icon: ImageVector, 
    isMe: Boolean,
    onUpdateStatus: (String, String) -> Unit,
    onModify: () -> Unit
) {
    val contentKey = when (message.type) {
        "BRIEF", "UPLOAD" -> "link"
        "SCRIPT" -> "content"
        "FEEDBACK" -> "feedback"
        else -> "text"
    }
    val content = message.metadata?.get(contentKey)?.toString() ?: message.text

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if(isMe) Color.White.copy(alpha = 0.2f) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isMe) Color.White else BrandThemeColor,
                    modifier = Modifier.background(
                        if(isMe) Color.Transparent else BrandThemeColor.copy(alpha = 0.1f), 
                        CircleShape
                    ).padding(4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isMe) Color.White else Color.Black
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                color = if (isMe) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = if(isMe) Color.White.copy(alpha=0.2f) else Color.LightGray.copy(alpha=0.2f))
            
            ActionButtons(
                status = message.status,
                isMe = isMe,
                onAccept = { onUpdateStatus(message.id, "ACCEPTED") },
                onReject = { onUpdateStatus(message.id, "REJECTED") },
                onModify = onModify
            )
        }
    }
}
