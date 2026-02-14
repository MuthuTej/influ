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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.roundToInt

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
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Accept", fontSize = 12.sp)
            }

            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reject", fontSize = 12.sp)
            }

            Button(
                onClick = onModify,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Modify", fontSize = 12.sp)
            }
        }
    } else if (status != "PENDING") {
        Text(
            text = "Status: $status",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (status == "ACCEPTED") Color(0xFF4CAF50) else Color(0xFFF44336),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ChatListItem(
    chat: ChatItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 👤 Profile icon/image
        Box(
            modifier = Modifier
                .size(48.dp)
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
                    modifier = Modifier.size(28.dp),
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 📝 Name + last message
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.name,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = chat.lastMessage,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // ⏰ Time + unread count
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = chat.time, // Note: Time formatting logic might be needed if raw timestamp
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (chat.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(20.dp)
                        .background(Color(0xFFFF8383), CircleShape), // Matching brand color
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unreadCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp
                    )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // 🔙 Back button
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        // 👤 Profile image (clickable)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 👤 Name + status
        Column {
            Text(
                text = chatName,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Online", // Static for now
                fontSize = 12.sp,
                color = Color.Gray
            )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
    ) {
        if (offsetX.value > 0) {
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Reply",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(24.dp),
                tint = if (offsetX.value > swipeThreshold) Color(0xFFFF8383) else Color.Gray
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
            Column(
                modifier = Modifier
                    .background(
                        if (isMe) Color(0xFFFF8383) else Color(0xFFEEEEEE),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp)
                    .widthIn(max = 260.dp)
            ) {
                // --- Quotation Box for Replies ---
                if (repliedMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(
                                color = if (isMe) Color.Black.copy(alpha = 0.1f) else Color.White,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = if (repliedMessage.isMe) "You" else "Other",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isMe) Color.White else Color(0xFFFF8383)
                        )
                        Text(
                            text = repliedMessage.text,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isMe) Color.White.copy(alpha = 0.8f) else Color.Gray
                        )
                    }
                }
                // ---------------------------------

                if (message.type == "TEXT") {
                    Text(
                        text = message.text,
                        color = if (isMe) Color.White else Color.Black
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

@Composable
fun MessageInputBar(
    replyingTo: ChatMessage?,
    chatName: String,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit,
    onCreateProposal: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        AnimatedVisibility(visible = replyingTo != null) {
            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to ${if (replyingTo.isMe) "You" else chatName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFFF8383)
                        )
                        Text(
                            text = replyingTo.text,
                            maxLines = 1,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onCancelReply) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Write something...") },
                textStyle = TextStyle(color = Color.Black),
                modifier = Modifier.weight(1f),
                shape = if (replyingTo != null) 
                    RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp) 
                else RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFFFF8383)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onCreateProposal,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFF8383), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Proposal",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFF8383), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White
                )
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
    Column {
        Text(
            text = "💰 Proposal",
            fontWeight = FontWeight.Bold,
            color = if (isMe) Color.White else Color.Black
        )
        Text(
            text = "Budget: $$amount",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isMe) Color.White else Color.Black
        )
        
        ActionButtons(
            status = message.status,
            isMe = isMe,
            onAccept = { onUpdateStatus(message.id, "ACCEPTED") },
            onReject = { onUpdateStatus(message.id, "REJECTED") },
            onModify = onModify
        )
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
    
    // Fallback for legacy list format if needed, though we switched to map
    val displayItems = if (itemsMap.isNotEmpty()) {
        itemsMap.entries.map { "${it.key} (x${it.value})" }
    } else {
        message.metadata?.get("items") as? List<String> ?: emptyList()
    }

    Column {
        Text(
            text = "📋 Deliverables",
            fontWeight = FontWeight.Bold,
            color = if (isMe) Color.White else Color.Black
        )
        displayItems.forEach { item ->
            Text(
                text = "• $item",
                color = if (isMe) Color.White else Color.Black
            )
        }
        
        ActionButtons(
            status = message.status,
            isMe = isMe,
            onAccept = { onUpdateStatus(message.id, "ACCEPTED") },
            onReject = { onUpdateStatus(message.id, "REJECTED") },
            onModify = onModify
        )
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

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isMe) Color.White else Color(0xFFFF8383),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (isMe) Color.White else Color.Black
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            color = if (isMe) Color.White else Color.Black
        )
        
        ActionButtons(
            status = message.status,
            isMe = isMe,
            onAccept = { onUpdateStatus(message.id, "ACCEPTED") },
            onReject = { onUpdateStatus(message.id, "REJECTED") },
            onModify = onModify
        )
    }
}
