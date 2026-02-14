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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    chatId: String?,
    chatNameParam: String?,
    viewModel: ChatViewModel = viewModel(),
    onBack: () -> Unit = {},
    onCreateProposal: (String) -> Unit = {}
) {
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
            // Add other modification dialogs as needed
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
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

            RestrictedActionPanel(
                onSend = { text, type, metadata ->
                    viewModel.sendMessage(text, type, metadata)
                }
            )
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

@Composable
fun MessagesList(
    messages: List<ChatMessage>,
    onReply: (ChatMessage) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onModify: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        items(messages) { message ->
            MessageBubble(
                message = message,
                allMessages = messages,
                onSwipeToReply = { onReply(message) },
                onUpdateStatus = onUpdateStatus,
                onModify = onModify
            )
        }
    }
}
