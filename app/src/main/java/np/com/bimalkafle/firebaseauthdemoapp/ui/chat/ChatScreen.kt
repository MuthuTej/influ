package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun ChatScreen(
    chatId: String?,
    chatNameParam: String?,
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: ChatViewModel = viewModel(),
    onBack: () -> Unit = {},
    onCreateProposal: (String) -> Unit = {}
) {
    val authState = authViewModel.authState.observeAsState()
    var isBrand by remember { mutableStateOf(false) }

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Authenticated) {
            val role = (authState.value as AuthState.Authenticated).role
            isBrand = role.equals("BRAND", ignoreCase = true)
        }
    }

    LaunchedEffect(chatId) {
        chatId?.let { 
            viewModel.initChat(it, chatNameParam ?: "Chat") 
        }
    }

    val messages by viewModel.messages.collectAsState()
    val chatName by viewModel.chatName.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()

    var isProfileExpanded by remember { mutableStateOf(false) }
    var modificationMessage by remember { mutableStateOf<ChatMessage?>(null) }


    BackHandler(enabled = isProfileExpanded) {
        isProfileExpanded = false
    }
    
    // Modification Dialogs
    if (modificationMessage != null) {
        val msg = modificationMessage!!
        when (msg.type) {
            "NEGOTIATION" -> {
                val currentAmount = msg.metadata["amount"]?.toString()?.toIntOrNull() ?: 0
                NegotiationDialog(
                    initialAmount = currentAmount,
                    onDismiss = { modificationMessage = null },
                    onSend = { amount ->
                        viewModel.sendMessage(
                            text = "Proposed Budget: $$amount", 
                            type = "NEGOTIATION", 
                            metadata = mapOf("amount" to amount)
                        )
                        viewModel.updateMessageStatus(msg.id, "MODIFIED")
                        modificationMessage = null
                    }
                )
            }
            "DELIVERABLES" -> {
                @Suppress("UNCHECKED_CAST")
                val currentItems = msg.metadata["items"] as? Map<String, Int> ?: emptyMap()
                DeliverablesDialog(
                    initialDeliverables = currentItems,
                    onDismiss = { modificationMessage = null },
                    onSend = { deliverables ->
                        val text = "Deliverables: ${deliverables.entries.joinToString { "${it.key} (x${it.value})" }}"
                        viewModel.sendMessage(
                            text = text, 
                            type = "DELIVERABLES", 
                            metadata = mapOf("items" to deliverables)
                        )
                        viewModel.updateMessageStatus(msg.id, "MODIFIED")
                        modificationMessage = null
                    }
                )
            }
        }
    }

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "Connect",
                onItemSelected = { /* Handled in component */ },
                navController = navController,
                isBrand = isBrand
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF2F2F7))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
            ) {
                ChatTopBar(
                    chatName = chatName,
                    onBackClick = onBack,
                    onProfileClick = {
                        isProfileExpanded = true
                    }
                )

                MessagesList(
                    messages = messages,
                    onReply = { message ->
                        viewModel.setReplyingTo(message)
                    },
                    onUpdateStatus = { messageId, status ->
                        viewModel.updateMessageStatus(messageId, status)
                    },
                    onModify = { message ->
                        modificationMessage = message
                    },
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shadowElevation = 8.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RestrictedActionPanel(
                        onSend = { text, type, metadata ->
                            viewModel.sendMessage(text, type, metadata)
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = isProfileExpanded,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isProfileExpanded = false },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// Sealed interface for List Items
private sealed interface ChatUiItem {
    data class MessageItem(val message: ChatMessage) : ChatUiItem
    data class DateHeader(val date: String) : ChatUiItem
}

@Composable
fun MessagesList(
    messages: List<ChatMessage>,
    onReply: (ChatMessage) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onModify: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Process messages to add headers
    val uiItems = remember(messages) {
        val items = mutableListOf<ChatUiItem>()
        var lastDate = ""
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        messages.forEach { message ->
            val date = try {
                val dateObj = Date(message.timestamp) // Assuming timestamp is millis
                dateFormat.format(dateObj)
            } catch (e: Exception) {
                ""
            }

            // Check for specific readable dates like Today/Yesterday
            val readableDate = calculateReadableDate(message.timestamp)
            
            if (readableDate != lastDate) {
                items.add(ChatUiItem.DateHeader(readableDate))
                lastDate = readableDate
            }
            items.add(ChatUiItem.MessageItem(message))
        }
        items
    }

    LaunchedEffect(uiItems.size) {
        if (uiItems.isNotEmpty()) {
            listState.animateScrollToItem(uiItems.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(uiItems) { item ->
            when (item) {
                is ChatUiItem.DateHeader -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = item.date,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                is ChatUiItem.MessageItem -> {
                    MessageBubble(
                        message = item.message,
                        allMessages = messages,
                        onSwipeToReply = { onReply(item.message) },
                        onUpdateStatus = onUpdateStatus,
                        onModify = onModify
                    )
                }
            }
        }
    }
}

private fun calculateReadableDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) -> "Today"
        
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
        
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(msgTime.time)
    }
}
