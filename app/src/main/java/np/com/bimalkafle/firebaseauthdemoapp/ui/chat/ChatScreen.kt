package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.utils.RazorpayService
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.network.CollaborationWebSocketClient
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.RatingPromptDialog
import np.com.bimalkafle.firebaseauthdemoapp.model.ChatMessage
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.ChatViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import java.text.SimpleDateFormat
import java.util.*

private val TlRed = Color(0xFFE63946)
private val TlGreen = Color(0xFF4CAF50)
private val TlGray = Color(0xFFBDBDBD)

private val STATUS_ORDER = listOf(
    "PENDING", "NEGOTIATION", "ACCEPTED",
    "BRIEF_SENT", "BRIEF_FINALIZED",
    "SCRIPT_SENT",
    "WAITING_FOR_PAYMENT", "IN_PROGRESS", "COMPLETED"
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    val brandCollabs by brandViewModel.collaborations.observeAsState(initial = emptyList())
    val influencerCollabs by influencerViewModel.collaborations.observeAsState(initial = emptyList())
    val allCollaborations = if (isBrand) brandCollabs else influencerCollabs

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

    DisposableEffect(collaborationId, authState.value) {
        if (collaborationId == null || authState.value !is AuthState.Authenticated) {
            return@DisposableEffect onDispose {}
        }
        val isBrandUser = (authState.value as AuthState.Authenticated).role.equals("BRAND", ignoreCase = true)

        fun refresh() {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { r ->
                r.token?.let { token ->
                    if (isBrandUser) brandViewModel.fetchCollaborations(token, force = true)
                    else influencerViewModel.fetchCollaborations(token, force = true)
                }
            }
        }

        // WebSocket for real-time collaboration status updates.
        var wsClient: CollaborationWebSocketClient? = null
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { r ->
            r.token?.let { token ->
                wsClient = CollaborationWebSocketClient(
                    token = token,
                    collaborationId = collaborationId,
                    onUpdate = ::refresh
                )
                wsClient?.connect()
            }
        }

        onDispose { wsClient?.disconnect() }
    }

    LaunchedEffect(chatId, collaborationId) {
        chatId?.let { viewModel.initChat(it, chatNameParam ?: "Chat", collaborationId) }
    }

    // When the other party sends a brief or script the collaboration-status WebSocket fires first.
    // Re-fetch messages so the BRIEF/SCRIPT message is guaranteed to be in the list even if
    // the chat-message WebSocket missed it.  A short delay lets any concurrent Firestore write
    // propagate before we query.
    LaunchedEffect(currentCollaboration?.status) {
        val s = currentCollaboration?.status
        if (s == "BRIEF_SENT" || s == "SCRIPT_SENT") {
            kotlinx.coroutines.delay(500)
            viewModel.refreshMessages()
        }
    }

    DisposableEffect(chatId) {
        onDispose { viewModel.stopConversationWebSocket() }
    }

    val messages by viewModel.messages.collectAsState()
    val chatName by viewModel.chatName.collectAsState()
    val chatError by viewModel.error.collectAsState()
    val contactInfoWarning by viewModel.contactInfoWarning.collectAsState()
    val localContext = LocalContext.current
    val activity = localContext as? Activity

    val brandError by brandViewModel.error.observeAsState()
    val influencerError by influencerViewModel.error.observeAsState()

    LaunchedEffect(chatError) {
        chatError?.let {
            android.widget.Toast.makeText(localContext, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(contactInfoWarning) {
        contactInfoWarning?.let {
            android.widget.Toast.makeText(localContext, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearContactInfoWarning()
        }
    }
    LaunchedEffect(brandError) {
        brandError?.let {
            android.widget.Toast.makeText(localContext, it, android.widget.Toast.LENGTH_LONG).show()
            brandViewModel.clearError()
        }
    }
    LaunchedEffect(influencerError) {
        influencerError?.let {
            android.widget.Toast.makeText(localContext, it, android.widget.Toast.LENGTH_LONG).show()
            influencerViewModel.clearError()
        }
    }

    // Prompt for a post-collaboration rating right here in the negotiation chat,
    // where the WebSocket above already keeps currentCollaboration live — so this
    // reacts to the COMPLETED transition in real time instead of depending on a
    // stale home-page snapshot. Captured once into stable state so a mid-rating
    // refetch can't swap out or dismiss the dialog before its thank-you timer ends.
    var activeReviewCollaboration by remember { mutableStateOf<Collaboration?>(null) }
    LaunchedEffect(currentCollaboration) {
        if (activeReviewCollaboration == null &&
            currentCollaboration?.status == "COMPLETED" &&
            currentCollaboration.hasReviewed == false
        ) {
            activeReviewCollaboration = currentCollaboration
        }
    }
    activeReviewCollaboration?.let { collaboration ->
        RatingPromptDialog(
            revieweeName = if (isBrand) collaboration.influencer.name else (collaboration.brand?.name ?: "the brand"),
            onSubmit = { rating, comment ->
                val idToken = kotlinx.coroutines.suspendCancellableCoroutine<String?> { cont ->
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) {
                        cont.resume(null, onCancellation = null)
                    } else {
                        user.getIdToken(true)
                            .addOnSuccessListener { cont.resume(it.token, onCancellation = null) }
                            .addOnFailureListener { cont.resume(null, onCancellation = null) }
                    }
                }
                if (idToken == null) {
                    Result.failure(Exception("Not authenticated"))
                } else if (isBrand) {
                    brandViewModel.addReview(
                        collaborationId = collaboration.id,
                        revieweeId = collaboration.influencerId,
                        rating = rating,
                        comment = comment,
                        token = idToken
                    )
                } else {
                    influencerViewModel.addReview(
                        collaborationId = collaboration.id,
                        revieweeId = collaboration.brandId,
                        rating = rating,
                        comment = comment,
                        token = idToken
                    )
                }
            },
            onDismiss = { activeReviewCollaboration = null }
        )
    }

    var isActionLoading by remember { mutableStateOf(false) }

    val onStatusUpdate: (String) -> Unit = { newStatus ->
        if (!isActionLoading) {
            isActionLoading = true
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.addOnSuccessListener { result ->
                    val token = result.token ?: run { isActionLoading = false; return@addOnSuccessListener }
                    if (isBrand) brandViewModel.updateCollaborationStatus(token, collaborationId ?: "", newStatus) { isActionLoading = false }
                    else influencerViewModel.updateCollaborationStatus(token, collaborationId ?: "", newStatus) { isActionLoading = false }
                }
                ?.addOnFailureListener { isActionLoading = false }
        }
    }

    Scaffold(
        bottomBar = {
            Surface(color = Color.White, modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    CmnBottomNavigationBar(
                        selectedItem = "Negotiation",
                        onItemSelected = {},
                        navController = navController,
                        isBrand = isBrand
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(Color(0xFFF5F5F5))
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Surface(shadowElevation = 2.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                    Text(
                        text = chatName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        color = Color(0xFFF2F2F7),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = "Profile", modifier = Modifier.size(24.dp), tint = Color.Gray)
                        }
                    }
                }
            }

            if (collaborationId == null || currentCollaboration == null) {
                // ── Empty / Loading state ────────────────────────────────────
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
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.WorkOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = TlRed.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = if (collaborationId != null) "Loading..." else "No active collaboration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Select a collaboration to view its progress",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // ── Campaign Status Banner ───────────────────────────────────
                CampaignStatusBanner(collaboration = currentCollaboration)

                // ── Campaign Info Card ───────────────────────────────────────
                CampaignInfoCard(collaboration = currentCollaboration)

                // ── Collaboration Timeline ───────────────────────────────────
                CollaborationTimeline(
                    messages = messages,
                    collaboration = currentCollaboration,
                    isBrand = isBrand,
                    onStatusUpdate = onStatusUpdate,
                    onSend = { text, type, metadata -> viewModel.sendMessage(text, type, metadata) },
                    onSendUpload = { link, platform, onDone -> viewModel.sendUpload(link, platform, onDone) },
                    isActionLoading = isActionLoading,
                    brandViewModel = if (isBrand) brandViewModel else null,
                    activity = activity,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Campaign Status Banner ───────────────────────────────────────────────────

@Composable
private fun CampaignStatusBanner(collaboration: Collaboration) {
    val status = collaboration.status
    val statusText = when (status) {
        "PENDING" -> "Awaiting Response"
        "NEGOTIATION" -> "Negotiation in Progress"
        "ACCEPTED" -> "Proposal Accepted"
        "BRIEF_SENT" -> "Brief Sent"
        "BRIEF_FINALIZED" -> "Brief Approved"
        "SCRIPT_SENT" -> "Script Under Review"
        "WAITING_FOR_PAYMENT" -> "Awaiting Payment"
        "IN_PROGRESS" -> "In Progress"
        "COMPLETED" -> "Completed"
        "REJECTED" -> "Rejected"
        "REVOKED" -> "Withdrawn"
        else -> status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    Surface(color = Color(0xFFFFF0F0), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Campaign status",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TlRed,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(TlRed, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(statusText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                }
            }

        }
    }
}

// ── Campaign Info Card ───────────────────────────────────────────────────────

@Composable
private fun CampaignInfoCard(collaboration: Collaboration) {
    val totalBudget = collaboration.totalAmount?.toInt()
        ?: collaboration.pricing?.sumOf { it.price }
        ?: 0
    val deliverables = collaboration.pricing
        ?.map { it.deliverable }
        ?.distinct()
        ?.joinToString("/")
        ?: "N/A"

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(TlRed, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WorkOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collaboration.campaign.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text("$deliverables • Budget: INR$totalBudget", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// ── Collaboration Timeline ───────────────────────────────────────────────────

@Composable
fun CollaborationTimeline(
    messages: List<ChatMessage>,
    collaboration: Collaboration,
    isBrand: Boolean,
    onStatusUpdate: (String) -> Unit,
    onSend: (String, String, Map<String, Any>) -> Unit,
    onSendUpload: (String, String, () -> Unit) -> Unit = { _, _, done -> done() },
    isActionLoading: Boolean = false,
    brandViewModel: BrandViewModel? = null,
    activity: Activity? = null,
    modifier: Modifier = Modifier
) {
    val status = collaboration.status
    val statusIndex = STATUS_ORDER.indexOf(status).coerceAtLeast(0)

    val briefMessage = remember(messages) { messages.firstOrNull { it.type == "BRIEF" } }
    // lastOrNull, not firstOrNull: after a rejection + resubmission there can be
    // more than one SCRIPT message, and the timeline should always show the
    // most recently submitted script, not the original rejected one.
    val scriptMessage = remember(messages) { messages.lastOrNull { it.type == "SCRIPT" } }
    val rejectionMessage = remember(messages) { messages.lastOrNull { it.type == "SCRIPT_REJECTED" } }
    // Whichever of these two happened most recently tells us whether content
    // is sitting with the brand for review, or was just kicked back to the
    // influencer to resubmit.
    val latestContentEvent = remember(messages) {
        messages.lastOrNull { it.type == "CONTENT_SUBMITTED" || it.type == "CONTENT_REJECTED" }
    }
    val negotiationMessages = remember(messages) { messages.filter { it.type == "NEGOTIATION" }.sortedBy { it.timestamp } }

    var showBriefDialog by remember { mutableStateOf(false) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showRejectScriptDialog by remember { mutableStateOf(false) }
    var showNegotiationDialog by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var showRejectContentDialog by remember { mutableStateOf(false) }

    // Step state flags
    val briefStepShown  = statusIndex >= STATUS_ORDER.indexOf("ACCEPTED")
    val briefContentAvail = briefMessage != null || statusIndex >= STATUS_ORDER.indexOf("BRIEF_SENT")
    val briefDone        = statusIndex >= STATUS_ORDER.indexOf("BRIEF_FINALIZED")
    val scriptStepShown  = statusIndex >= STATUS_ORDER.indexOf("BRIEF_FINALIZED")
    val scriptContentAvail = scriptMessage != null || statusIndex >= STATUS_ORDER.indexOf("SCRIPT_SENT")
    val scriptDone       = statusIndex >= STATUS_ORDER.indexOf("WAITING_FOR_PAYMENT")
    // True while the brand's most recent action on this script was a rejection
    // that the influencer hasn't resubmitted for yet (status is back to
    // BRIEF_FINALIZED and no newer SCRIPT message exists than the rejection).
    val rejectionPendingResubmit = status == "BRIEF_FINALIZED" &&
        rejectionMessage != null &&
        (scriptMessage == null || rejectionMessage.timestamp > scriptMessage.timestamp)
    val paymentActive    = status == "WAITING_FOR_PAYMENT"
    val paymentDone      = statusIndex >= STATUS_ORDER.indexOf("IN_PROGRESS")
    val contentDeliveryActive = status == "IN_PROGRESS"
    val contentDeliveryDone   = status == "COMPLETED"
    // Content stays IN_PROGRESS through the whole review cycle (so the
    // analytics-sync cron, which only polls IN_PROGRESS collaborations, never
    // stalls while the brand is reviewing) — these two flags branch the UI
    // on top of that single status rather than needing extra status values.
    val contentAwaitingReview = status == "IN_PROGRESS" && latestContentEvent?.type == "CONTENT_SUBMITTED"
    val contentRejectedPendingResubmit = status == "IN_PROGRESS" && latestContentEvent?.type == "CONTENT_REJECTED"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // ── Step 1: Initial Proposal ─────────────────────────────────────
        TimelineStepCard(
            title = "Initial Proposal",
            time = formatCollabDate(collaboration.createdAt),
            isActive = false,
            isDone = true,
            isLocked = false,
            isLast = false,
            badge = null
        ) {
            Text(
                text = collaboration.message?.takeIf { it.isNotBlank() }
                    ?: "The campaign was initiated with a focus on high-impact visual storytelling.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF555555),
                lineHeight = 20.sp
            )

            // Negotiation history — rendered for every type:NEGOTIATION message
            if (negotiationMessages.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "NEGOTIATIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                negotiationMessages.forEach { msg ->
                    val amount   = msg.metadata["amount"]?.toString() ?: ""
                    val platform = msg.metadata["platform"]?.toString() ?: ""
                    Surface(
                        color = if (msg.isMe) Color(0xFFFFEBEB) else Color(0xFFEBF5FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (msg.isMe) "You proposed" else "They proposed",
                                    fontSize = 11.sp, color = Color.Gray
                                )
                                if (amount.isNotBlank()) {
                                    Text(
                                        "₹$amount${if (platform.isNotBlank()) " · $platform" else ""}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                            Text(msg.timeFormatted, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Action buttons — depend on status, role, and who made the last proposal
            val myRole = if (isBrand) "BRAND" else "INFLUENCER"
            // isMyTurn = true when the OTHER party sent the last proposal (I need to respond)
            val isMyTurn = collaboration.initiatedBy != myRole

            when {
                status == "PENDING" -> {
                    Spacer(Modifier.height(12.dp))
                    if (isMyTurn) {
                        // It's my turn to respond to the other party's proposal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStatusUpdate("ACCEPTED") },
                                enabled = !isActionLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = TlGreen),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isActionLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Accept", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (!isBrand) {
                                Button(
                                    onClick = { showNegotiationDialog = true },
                                    enabled = !isActionLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = TlRed),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Negotiate", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Button(
                            onClick = { onStatusUpdate("REJECTED") },
                            enabled = !isActionLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFF0F0),
                                contentColor = Color(0xFFFF5252)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isBrand) "Reject" else "Decline", fontWeight = FontWeight.Medium)
                        }
                    } else {
                        // Waiting for the other party to respond to my proposal
                        Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Waiting for the other party to respond…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onStatusUpdate("REVOKED") },
                            enabled = !isActionLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFF0F0),
                                contentColor = Color(0xFFFF5252)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Withdraw Proposal", fontWeight = FontWeight.Medium)
                        }
                    }
                }
                status == "NEGOTIATION" -> {
                    Spacer(Modifier.height(12.dp))
                    if (isMyTurn) {
                        // Other party counter-proposed — I can accept or counter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onStatusUpdate("ACCEPTED") },
                                enabled = !isActionLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = TlGreen),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isActionLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Accept", fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { showNegotiationDialog = true },
                                enabled = !isActionLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = TlRed),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Counter", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // I sent the last counter-proposal — waiting for their response
                        Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Waiting for the other party to accept or counter your proposal…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { showNegotiationDialog = true },
                            enabled = !isActionLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TlRed),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Revise Offer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Step 2: Campaign Brief ───────────────────────────────────────
        TimelineStepCard(
            title = "Campaign Brief",
            time = briefMessage?.timeFormatted ?: "",
            isActive = briefStepShown && !briefDone,
            isDone = briefDone,
            isLocked = !briefStepShown,
            isLast = false,
            badge = if (briefDone) "ACCEPTED" else null
        ) {
            if (briefContentAvail) {
                val briefContent = briefMessage?.metadata?.get("link")?.toString()
                    ?: briefMessage?.text
                    ?: ""
                if (briefMessage == null) {
                    // Status is already BRIEF_SENT but message hasn't arrived yet — refreshing
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = TlRed)
                } else if (briefContent.isNotBlank()) {
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"$briefContent\"",
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = Color(0xFF333333),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                // Influencer can approve the brief
                if (status == "BRIEF_SENT" && !isBrand && briefMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onStatusUpdate("BRIEF_FINALIZED") },
                        enabled = !isActionLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = TlGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isActionLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Approve Brief", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (briefStepShown) {
                // Status is ACCEPTED — brand needs to send brief
                if (isBrand) {
                    Button(
                        onClick = { showBriefDialog = true },
                        enabled = !isActionLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = TlRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send Brief", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("Waiting for brand to send the brief.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else {
                Text("Available after proposal acceptance", style = MaterialTheme.typography.bodySmall, color = TlGray)
            }
        }

        // ── Step 3: Script Revision ──────────────────────────────────────
        val canSubmitScript = status == "BRIEF_FINALIZED" && !isBrand
        TimelineStepCard(
            title = "Script Revision",
            time = (if (rejectionPendingResubmit) rejectionMessage else scriptMessage)?.timeFormatted ?: "",
            isActive = scriptStepShown && !scriptDone,
            isDone = scriptDone,
            isLocked = !scriptStepShown && !canSubmitScript,
            isLast = false,
            badge = if (rejectionPendingResubmit) "REJECTED" else null
        ) {
            if (rejectionPendingResubmit) {
                Text(
                    text = "Script rejected",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(6.dp))
                val reason = rejectionMessage?.metadata?.get("reason")?.toString() ?: rejectionMessage?.text ?: ""
                if (reason.isNotBlank()) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333)
                    )
                }
                Spacer(Modifier.height(14.dp))
                if (!isBrand) {
                    Button(
                        onClick = { showScriptDialog = true },
                        enabled = !isActionLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = TlRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isActionLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Resubmit Script", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("Waiting for influencer to resubmit the script.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else if (scriptContentAvail) {
                val scriptContent = scriptMessage?.metadata?.get("content")?.toString()
                    ?: scriptMessage?.text
                    ?: ""
                if (scriptMessage == null) {
                    // Status is already SCRIPT_SENT but message hasn't arrived yet — refreshing
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = TlRed)
                } else {
                    Text(
                        text = "Script content",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    if (scriptContent.isNotBlank()) {
                        Text(
                            text = scriptContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }
                }
                // Brand can accept or reject the script
                if (status == "SCRIPT_SENT" && isBrand && scriptMessage != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onStatusUpdate("WAITING_FOR_PAYMENT") },
                            enabled = !isActionLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TlGreen),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Accept", fontWeight = FontWeight.Bold)
                            }
                        }
                        Button(
                            onClick = { showRejectScriptDialog = true },
                            enabled = !isActionLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (canSubmitScript) {
                Button(
                    onClick = { showScriptDialog = true },
                    enabled = !isActionLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = TlRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Submit Script", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (scriptStepShown) {
                Text("Waiting for influencer to submit script.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                Text("Available after brief approval", style = MaterialTheme.typography.bodySmall, color = TlGray)
            }
        }

        // ── Step 4: Final Payment ────────────────────────────────────────
        TimelineStepCard(
            title = "Final Payment",
            time = "",
            isActive = paymentActive,
            isDone = paymentDone,
            isLocked = !paymentActive && !paymentDone,
            isLast = false,
            badge = if (paymentDone) "PAID" else null
        ) {
            when {
                paymentDone -> Text(
                    "Payment completed. Collaboration is in progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF555555)
                )
                paymentActive -> {
                    Text(
                        "Script approved. Complete the payment to start work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF555555)
                    )
                    if (isBrand && brandViewModel != null && activity != null) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (!isActionLoading) {
                                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                                        ?.addOnSuccessListener { result ->
                                            val token = result.token ?: return@addOnSuccessListener
                                            brandViewModel.createCollaborationPaymentOrder(
                                                token, collaboration.id, "FULL"
                                            ) { orderData ->
                                                if (orderData != null) {
                                                    RazorpayService.startPayment(
                                                        activity = activity,
                                                        orderData = orderData,
                                                        userEmail = FirebaseAuth.getInstance().currentUser?.email,
                                                        userContact = null
                                                    )
                                                }
                                            }
                                        }
                                }
                            },
                            enabled = !isActionLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TlGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Pay Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (!isBrand) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Awaiting brand payment to begin work.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                else -> Text(
                    "Available after script approval",
                    style = MaterialTheme.typography.bodySmall,
                    color = TlGray
                )
            }
        }

        // ── Step 5: Content Delivery ─────────────────────────────────────────
        TimelineStepCard(
            title = "Content Delivery",
            time = "",
            isActive = contentDeliveryActive,
            isDone = contentDeliveryDone,
            isLocked = !contentDeliveryActive && !contentDeliveryDone,
            isLast = true,
            badge = when {
                contentDeliveryDone -> "DONE"
                contentAwaitingReview -> "In review"
                contentRejectedPendingResubmit -> "REJECTED"
                else -> null
            }
        ) {
            when {
                contentDeliveryDone -> Text(
                    "Content delivered. Collaboration completed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF555555)
                )
                contentAwaitingReview -> {
                    Text(
                        text = "Content submitted",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    if (isBrand) {
                        Text(
                            "Review the submitted content above and accept or reject it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onStatusUpdate("COMPLETED") },
                                enabled = !isActionLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = TlGreen),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isActionLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Accept", fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { showRejectContentDialog = true },
                                enabled = !isActionLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reject", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("Waiting for brand to review your submission.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                contentRejectedPendingResubmit -> {
                    Text(
                        text = "Content rejected",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5252),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    val reason = latestContentEvent?.metadata?.get("reason")?.toString() ?: latestContentEvent?.text ?: ""
                    if (reason.isNotBlank()) {
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (!isBrand) {
                        Button(
                            onClick = { showUploadDialog = true },
                            enabled = !isActionLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TlRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Resubmit Content", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("Waiting for influencer to resubmit content.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                contentDeliveryActive -> {
                    if (!isBrand) {
                        Button(
                            onClick = { showUploadDialog = true },
                            enabled = !isActionLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TlRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isActionLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Submit Content", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("Waiting for influencer to deliver content.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
                else -> Text("Available after payment", style = MaterialTheme.typography.bodySmall, color = TlGray)
            }
        }

    }

    // ── Dialogs ──────────────────────────────────────────────────────────────
    if (showBriefDialog) {
        TextInputDialog(
            title = "Share Campaign Brief",
            label = "Brief Link or Description",
            onDismiss = { showBriefDialog = false },
            onSend = { link ->
                onSend("Campaign Brief Shared", "BRIEF", mapOf("link" to link))
                onStatusUpdate("BRIEF_SENT")
                showBriefDialog = false
            }
        )
    }
    if (showScriptDialog) {
        TextInputDialog(
            title = "Submit Script",
            label = "Script Content",
            multiline = true,
            onDismiss = { showScriptDialog = false },
            onSend = { content ->
                onSend("Script Submitted", "SCRIPT", mapOf("content" to content))
                onStatusUpdate("SCRIPT_SENT")
                showScriptDialog = false
            }
        )
    }
    if (showRejectScriptDialog) {
        TextInputDialog(
            title = "Reject Script",
            label = "Reason for rejection",
            multiline = true,
            onDismiss = { showRejectScriptDialog = false },
            onSend = { reason ->
                onSend("Script Rejected", "SCRIPT_REJECTED", mapOf("reason" to reason))
                onStatusUpdate("BRIEF_FINALIZED")
                showRejectScriptDialog = false
            }
        )
    }
    if (showNegotiationDialog) {
        NegotiationDialog(
            onDismiss = { showNegotiationDialog = false },
            collaboration = collaboration,
            onSend = { amount, platform, deliverables ->
                val delStr = deliverables.entries.joinToString { "${it.key} (x${it.value})" }
                onSend(
                    "Negotiated Proposal: ₹$amount on $platform - $delStr",
                    "NEGOTIATION",
                    mapOf("amount" to amount, "platform" to platform, "items" to deliverables)
                )
                onStatusUpdate("NEGOTIATION")
                showNegotiationDialog = false
            }
        )
    }
    if (showUploadDialog) {
        ContentUploadDialog(
            onDismiss = { showUploadDialog = false },
            onSend = { links, platform ->
                showUploadDialog = false
                // Only mark it submitted-for-review once every link has been saved to
                // the backend, so the analytics page already sees the video/post data
                // by the time the brand opens it. Status stays IN_PROGRESS — the brand's
                // own Accept action is what actually completes the collaboration.
                val pending = java.util.concurrent.atomic.AtomicInteger(links.size)
                links.forEach { link ->
                    onSendUpload(link, platform) {
                        if (pending.decrementAndGet() == 0) {
                            onSend("Content Submitted for Review", "CONTENT_SUBMITTED", emptyMap())
                        }
                    }
                }
            }
        )
    }
    if (showRejectContentDialog) {
        TextInputDialog(
            title = "Reject Content",
            label = "Reason for rejection",
            multiline = true,
            onDismiss = { showRejectContentDialog = false },
            onSend = { reason ->
                onSend("Content Rejected", "CONTENT_REJECTED", mapOf("reason" to reason))
                showRejectContentDialog = false
            }
        )
    }
}

// ── Timeline Step Card ───────────────────────────────────────────────────────

@Composable
private fun TimelineStepCard(
    title: String,
    time: String,
    isActive: Boolean,
    isDone: Boolean,
    isLocked: Boolean,
    isLast: Boolean,
    badge: String?,
    content: @Composable ColumnScope.() -> Unit
) {
    val dotFill   = if (!isLocked && (isDone || isActive)) TlRed else Color.White
    val dotBorder = if (isLocked) TlGray else TlRed
    val lineColor = if (isLocked) TlGray else TlRed
    val titleAlpha = if (isLocked) 0.35f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Left column: dot + connecting line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(dotFill, CircleShape)
                    .border(2.dp, dotBorder, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(lineColor)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Right column: title + content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 8.dp else 24.dp)
        ) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black.copy(alpha = titleAlpha)
                )
                badge?.let { badgeText ->
                    Spacer(Modifier.width(8.dp))
                    Surface(color = TlGreen, shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = badgeText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (isActive && !isDone) {
                    Text("Active", fontSize = 12.sp, color = TlRed, fontWeight = FontWeight.SemiBold)
                } else if (time.isNotBlank()) {
                    Text(time, fontSize = 12.sp, color = Color.LightGray)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Step body
            Column(content = content)
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatCollabDate(dateStr: String): String {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    return try {
        var date: Date? = null
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                date = sdf.parse(dateStr)
                if (date != null) break
            } catch (_: Exception) { }
        }
        date ?: return dateStr

        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }
        when {
            now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
                    && now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> "Today"
            now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
                    && now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
        }
    } catch (_: Exception) {
        dateStr
    }
}
