package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import np.com.bimalkafle.firebaseauthdemoapp.network.ChatEntity
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.Dimens
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.LocalAppColors
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.AiChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.AiChatViewModel

private fun routeForEntity(entity: ChatEntity): String? = when (entity.type) {
    "influencer" -> "brand_influencer_detail/${entity.id}"
    "campaign" -> "brand_campaign_detail/${entity.id}"
    "collaboration" -> "collaboration_analytics/${entity.id}"
    else -> null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    navController: NavHostController,
    aiChatViewModel: AiChatViewModel = viewModel()
) {
    val appColors = LocalAppColors.current
    val messages by aiChatViewModel.messages.collectAsState()
    val isLoading by aiChatViewModel.isLoading.collectAsState()
    val error by aiChatViewModel.error.collectAsState()
    val listState = rememberLazyListState()
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
    }

    // The header band stays a fixed color behind the status bar (matches the
    // "History & Proposals" header pattern) instead of leaving a white/black
    // gap there — imePadding sits on the Column below so only the message
    // list + input shift for the keyboard, not the header.
    Box(modifier = Modifier.fillMaxSize().background(appColors.brandPrimary)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.space16, vertical = Dimens.space16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(Dimens.minTouchTarget)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(Dimens.space12))
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(Dimens.space8))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(
                        "Campaigns, collaborations & influencers",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearConfirm = true },
                        modifier = Modifier
                            .size(Dimens.minTouchTarget)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear chat", tint = Color.White)
                    }
                }
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("Clear chat?") },
                    text = { Text("This deletes your conversation with the AI assistant from this device. This can't be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            aiChatViewModel.clearChat()
                            showClearConfirm = false
                        }) {
                            Text("Clear", color = appColors.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(appColors.surfaceElevated)
                    .imePadding()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        messages.isEmpty() -> AiChatEmptyState()
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = Dimens.space16)
                        ) {
                            items(messages) { message ->
                                AiChatBubble(message = message, onEntityClick = { entity ->
                                    routeForEntity(entity)?.let { navController.navigate(it) }
                                })
                            }
                            if (isLoading) {
                                item { TypingIndicatorBubble() }
                            }
                        }
                    }
                }

                if (error != null) {
                    Surface(color = appColors.error.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.space16, vertical = Dimens.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = appColors.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text(error ?: "", color = appColors.error, fontSize = 13.sp)
                        }
                    }
                }

                AiChatInputBar(enabled = !isLoading, onSend = { aiChatViewModel.sendMessage(it) })
            }
        }
    }
}

@Composable
private fun AiChatEmptyState() {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(appColors.brandPrimary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = appColors.brandPrimary, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(Dimens.space16))
        Text(
            "Ask about your campaigns, collaborations, or get influencer recommendations.",
            color = appColors.textSecondary,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun TypingIndicatorBubble() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space16, vertical = Dimens.space4)) {
        Surface(shape = RoundedCornerShape(18.dp), color = ChatBubbleOtherColor) {
            Row(
                modifier = Modifier.padding(horizontal = Dimens.space16, vertical = Dimens.space12),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = LocalAppColors.current.brandPrimary)
                Spacer(modifier = Modifier.width(Dimens.space8))
                Text("Thinking…", color = LocalAppColors.current.textSecondary, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiChatBubble(message: AiChatMessage, onEntityClick: (ChatEntity) -> Unit) {
    val isMe = message.isUser
    val bubbleColor = if (isMe) ChatBubbleSelfColor else ChatBubbleOtherColor
    val onBubbleColor = if (isMe) Color.White else LocalAppColors.current.textPrimary

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space16, vertical = Dimens.space4),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = bubbleColor,
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    color = onBubbleColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8)
                )
            }
        }

        if (message.entities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.space8))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8, Alignment.End),
                modifier = Modifier.fillMaxWidth()
            ) {
                message.entities.forEach { entity ->
                    AssistChip(
                        onClick = { onEntityClick(entity) },
                        label = { Text(entity.label, fontSize = 13.sp, maxLines = 1) },
                        trailingIcon = { Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = LocalAppColors.current.brandPrimary.copy(alpha = 0.1f),
                            labelColor = LocalAppColors.current.brandPrimary,
                            trailingIconContentColor = LocalAppColors.current.brandPrimary
                        ),
                        border = null
                    )
                }
            }
        }
    }
}

@Composable
private fun AiChatInputBar(enabled: Boolean, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val appColors = LocalAppColors.current

    Surface(shadowElevation = 8.dp, color = appColors.surfaceElevated, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = Dimens.space12, horizontal = Dimens.space16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Ask the AI assistant…", color = appColors.textSecondary) },
                textStyle = androidx.compose.ui.text.TextStyle(color = appColors.textPrimary, fontSize = 15.sp),
                modifier = Modifier.weight(1f).heightIn(min = Dimens.minTouchTarget),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = appColors.surfaceSubtle,
                    unfocusedContainerColor = appColors.surfaceSubtle,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = appColors.brandPrimary
                ),
                maxLines = 4,
                enabled = enabled
            )

            Spacer(modifier = Modifier.width(Dimens.space8))

            val isTextEmpty = text.isBlank()
            IconButton(
                onClick = {
                    if (!isTextEmpty && enabled) {
                        onSend(text)
                        text = ""
                    }
                },
                modifier = Modifier
                    .size(Dimens.minTouchTarget)
                    .background(if (isTextEmpty || !enabled) appColors.textDisabled else appColors.brandPrimary, CircleShape),
                enabled = !isTextEmpty && enabled
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
