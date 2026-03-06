package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.ChatViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun ChatScreen(
    chatId: String?,
    chatNameParam: String?,
    collaborationId: String? = null,
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: ChatViewModel = viewModel(),
    brandViewModel: BrandViewModel = viewModel(),
    influencerViewModel: InfluencerViewModel = viewModel(),
    onBack: () -> Unit = {},
    onCreateProposal: (String) -> Unit = {}
) {
    val authState = authViewModel.authState.observeAsState()
    var isBrand by remember { mutableStateOf(false) }

    // Dropdown state
    var showCollabDropdown by remember { mutableStateOf(false) }
    
    val brandCollabs by brandViewModel.collaborations.observeAsState(initial = emptyList())
    val influencerCollabs by influencerViewModel.collaborations.observeAsState(initial = emptyList())
    
    val allCollaborations = if (isBrand) brandCollabs else influencerCollabs
    
    // Filter collaborations for this specific user
    val relevantCollabs = remember(allCollaborations, chatId) {
        allCollaborations.filter { it.influencerId == chatId || it.brandId == chatId }
    }

    val currentCollaboration = remember(relevantCollabs, collaborationId) {
        relevantCollabs.find { it.id == collaborationId }
    }

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Authenticated) {
            val role = (authState.value as AuthState.Authenticated).role
            isBrand = role.equals("BRAND", ignoreCase = true)
            
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                result.token?.let { token ->
                    if (isBrand) brandViewModel.fetchCollaborations(token)
                    else influencerViewModel.fetchCollaborations(token)
                }
            }
        }
    }

    LaunchedEffect(chatId, collaborationId) {
        chatId?.let { 
            viewModel.initChat(it, chatNameParam ?: "Chat", collaborationId) 
        }
    }

    val messages by viewModel.messages.collectAsState()
    val chatName by viewModel.chatName.collectAsState()

    var isProfileExpanded by remember { mutableStateOf(false) }
    var modificationMessage by remember { mutableStateOf<ChatMessage?>(null) }


    BackHandler(enabled = isProfileExpanded) {
        isProfileExpanded = false
    }
    
    // Modification Dialogs
    if (modificationMessage != null) {
        val msg = modificationMessage!!
        when (msg.type) {
            "NEGOTIATION", "DELIVERABLES" -> {
                val currentAmount = msg.metadata["amount"]?.toString()?.toIntOrNull() ?: 
                                   currentCollaboration?.pricing?.firstOrNull()?.price ?: 0
                val currentPlatform = msg.metadata["platform"]?.toString() ?: 
                                     currentCollaboration?.pricing?.firstOrNull()?.platform ?: "Instagram"
                @Suppress("UNCHECKED_CAST")
                val currentItems = msg.metadata["items"] as? Map<String, Int> ?: emptyMap()

                NegotiationDialog(
                    initialAmount = currentAmount,
                    initialPlatform = currentPlatform,
                    initialDeliverables = currentItems,
                    collaboration = currentCollaboration,
                    onDismiss = { modificationMessage = null },
                    onSend = { amount, platform, deliverables ->
                        val delStr = deliverables.entries.joinToString { "${it.key} (x${it.value})" }
                        viewModel.sendMessage(
                            text = "Negotiated Proposal: ₹$amount on $platform - $delStr", 
                            type = "NEGOTIATION", 
                            metadata = mapOf(
                                "amount" to amount,
                                "platform" to platform,
                                "items" to deliverables
                            )
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
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    CmnBottomNavigationBar(
                        selectedItem = "Connect",
                        onItemSelected = { /* Handled in component */ },
                        navController = navController,
                        isBrand = isBrand
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(Color(0xFFF2F2F7))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // Primary Project Selector Top Bar
                Surface(
                    shadowElevation = 2.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = chatName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // Context (Collaboration) Selector - Attractive Pill Design
                            Surface(
                                onClick = { showCollabDropdown = true },
                                shape = RoundedCornerShape(16.dp),
                                color = if (collaborationId == null) Color(0xFFFFEAEA) else Color(0xFFF5F5F5),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = if (collaborationId == null) Color(0xFFFF8383) else Color.Gray
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = currentCollaboration?.campaign?.title ?: "Select Project",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (collaborationId == null) Color(0xFFFF8383) else Color.DarkGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 180.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = showCollabDropdown,
                                    onDismissRequest = { showCollabDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.75f).background(Color.White)
                                ) {
                                    Text(
                                        text = "SWITCH COLLABORATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                    
                                    if (relevantCollabs.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No active collaborations", fontSize = 14.sp) },
                                            onClick = { },
                                            enabled = false
                                        )
                                    } else {
                                        relevantCollabs.forEach { collab ->
                                            DropdownMenuItem(
                                                text = { 
                                                    Column {
                                                        Text(collab.campaign.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text(collab.status, fontSize = 10.sp, color = Color(0xFFFF8383), fontWeight = FontWeight.Bold)
                                                    }
                                                },
                                                onClick = {
                                                    showCollabDropdown = false
                                                    navController.navigate("chat/$chatId/$chatName?collaborationId=${collab.id}") {
                                                        popUpTo("chat/$chatId/$chatName") { inclusive = false }
                                                        launchSingleTop = true
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.WorkOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                                                }
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF0F0F0))
                                    
                                    DropdownMenuItem(
                                        text = { Text("General Discussion", color = Color.Gray, fontSize = 14.sp) },
                                        onClick = {
                                            showCollabDropdown = false
                                            navController.navigate("chat/$chatId/$chatName") {
                                                popUpTo("chat/$chatId/$chatName") { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { isProfileExpanded = true },
                            color = Color(0xFFF2F2F7),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile",
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }

                if (collaborationId == null) {
                    // Modern Empty State
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                shadowElevation = 4.dp,
                                modifier = Modifier.size(100.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.WorkOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color(0xFFFF8383).copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(32.dp))
                            Text(
                                "No project context",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Select an active collaboration from the top bar to start discussing specific deliverables and milestones.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                lineHeight = 22.sp
                            )
                            Spacer(Modifier.height(40.dp))
                            Button(
                                onClick = { showCollabDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(54.dp).fillMaxWidth(0.8f),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Text("Select Collaboration", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }

                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    if (isBrand) {
                                        navController.navigate("influencer_create_proposal/$chatId")
                                    } else {
                                        navController.navigate("influencer_search")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(54.dp).fillMaxWidth(0.8f),
                                border = BorderStroke(1.5.dp, Color(0xFFFF8383)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8383))
                            ) {
                                Text(
                                    if (isBrand) "Invite to Campaign" else "Find Campaigns", 
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                } else {
                    // Collaboration Messages
                    MessagesList(
                        messages = messages,
                        collaboration = currentCollaboration,
                        isBrand = isBrand,
                        onReply = { _ ->
                            // Replying functionality placeholder
                        },
                        onUpdateStatus = { messageId, status ->
                            viewModel.updateMessageStatus(messageId, status)
                        },
                        onModify = { message ->
                            modificationMessage = message
                        },
                        onCollaborationStatusUpdate = { newStatus ->
                            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                val token = result.token ?: return@addOnSuccessListener
                                if (isBrand) {
                                    brandViewModel.updateCollaborationStatus(token, collaborationId, newStatus) { }
                                } else {
                                    influencerViewModel.updateCollaborationStatus(token, collaborationId, newStatus) { }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shadowElevation = 12.dp,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RestrictedActionPanel(
                            status = currentCollaboration?.status,
                            collaborationId = collaborationId,
                            isBrand = isBrand,
                            onSend = { text, type, metadata ->
                                viewModel.sendMessage(text, type, metadata)
                            },
                            onSendUpload = { link, platform ->
                                viewModel.sendUpload(link, platform)
                            },
                            onStatusUpdate = { newStatus ->
                                FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                    val token = result.token ?: return@addOnSuccessListener
                                    if (isBrand) {
                                        brandViewModel.updateCollaborationStatus(token, collaborationId, newStatus) { }
                                    } else {
                                        influencerViewModel.updateCollaborationStatus(token, collaborationId, newStatus) { }
                                    }
                                }
                            },
                            collaboration = currentCollaboration,
                            brandViewModel = brandViewModel
                        )
                    }
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
                        .background(Color.Black.copy(alpha = 0.8f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isProfileExpanded = false },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        shadowElevation = 8.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().padding(40.dp),
                            tint = Color.LightGray
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
    data class ProposalHeader(val collaboration: Collaboration) : ChatUiItem
}

@Composable
fun MessagesList(
    messages: List<ChatMessage>,
    collaboration: Collaboration?,
    isBrand: Boolean,
    onReply: (ChatMessage) -> Unit,
    onUpdateStatus: (String, String) -> Unit,
    onModify: (ChatMessage) -> Unit,
    onCollaborationStatusUpdate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Process items to add proposal header and date headers
    val uiItems = remember(messages, collaboration) {
        val items = mutableListOf<ChatUiItem>()
        
        // 1. Add Initial Proposal at the very top
        collaboration?.let {
            items.add(ChatUiItem.ProposalHeader(it))
        }

        var lastDate = ""

        messages.forEach { message ->
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
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(uiItems) { item ->
            when (item) {
                is ChatUiItem.ProposalHeader -> {
                    ProposalSummaryCard(
                        collaboration = item.collaboration,
                        isBrand = isBrand,
                        onAction = onCollaborationStatusUpdate
                    )
                }
                is ChatUiItem.DateHeader -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = item.date,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
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

@Composable
fun ProposalSummaryCard(
    collaboration: Collaboration,
    isBrand: Boolean,
    onAction: (String) -> Unit
) {
    val pricing = collaboration.pricing?.firstOrNull()
    val status = collaboration.status
    val initiatedByMe = if (isBrand) collaboration.initiatedBy == "BRAND" else collaboration.initiatedBy == "INFLUENCER"

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFFFF8383).copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.WorkOutline, null, tint = Color(0xFFFF8383), modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("INITIAL PROPOSAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    Text(collaboration.campaign.title, fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color.Black)
                }
                
                Surface(
                    color = Color(0xFFF2F2F7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("DELIVERABLE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(pricing?.deliverable ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BUDGET", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${pricing?.currency ?: "INR"} ${pricing?.price ?: 0}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFFF8383))
                }
            }

            // CTAs for PENDING status
            if (status == "PENDING") {
                Spacer(Modifier.height(20.dp))
                if (!initiatedByMe) {
                    // I am the receiver - show Accept/Reject
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onAction("ACCEPTED") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Accept", fontWeight = FontWeight.ExtraBold)
                        }
                        OutlinedButton(
                            onClick = { onAction("REJECTED") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFF44336)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reject", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                } else {
                    // I am the sender - show Revoke
                    Button(
                        onClick = { onAction("REVOKED") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Withdraw Proposal", fontWeight = FontWeight.ExtraBold)
                    }
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

