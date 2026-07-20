@file:OptIn(ExperimentalMaterial3Api::class)

package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import np.com.bimalkafle.firebaseauthdemoapp.components.AiChatFab
import np.com.bimalkafle.firebaseauthdemoapp.components.AppPullToRefreshBox
import np.com.bimalkafle.firebaseauthdemoapp.components.ReportDownloadButton
import np.com.bimalkafle.firebaseauthdemoapp.utils.InfluencerReportCsvGenerator
import np.com.bimalkafle.firebaseauthdemoapp.utils.InfluencerReportPdfGenerator
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.utils.RazorpayService
import android.app.Activity
import androidx.compose.ui.platform.LocalContext

import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.EmptyState
import np.com.bimalkafle.firebaseauthdemoapp.components.LoadingState

enum class ProposalStatus(val displayName: String, val color: Color, val icon: ImageVector) {
    PENDING("Pending", Color(0xFFFFC107), Icons.Default.HourglassEmpty),
    NEGOTIATION("Negotiation", Color(0xFFFF9800), Icons.Default.ChatBubbleOutline),
    ACCEPTED("Accepted", Color(0xFF4CAF50), Icons.Default.CheckCircle),
    REJECTED("Rejected", Color(0xFFF44336), Icons.Default.HighlightOff),
    REVOKED("Revoked", Color(0xFF9E9E9E), Icons.Default.Undo),
    IN_PROGRESS("In Progress", Color(0xFF2196F3), Icons.Default.PlayArrow),
    WAITING_FOR_PAYMENT("Wait Payment", Color(0xFF9C27B0), Icons.Default.AccountBalanceWallet),
    COMPLETED("Completed", Color(0xFF388E3C), Icons.Default.TaskAlt)
}

enum class ProposalType {
    SENT,
    RECEIVED
}

data class Proposal(
    val id: String,
    val otherPartyName: String,
    val influencerId: String,
    val brandId: String,
    val campaignTitle: String,
    val budget: String,
    val deliverable: String,
    val platform: String,
    val status: ProposalStatus,
    val type: ProposalType,
    val logoUrl: String?,
    val date: String,
    val totalAmount: String,
    val paymentStatus: String,
    val selectedInstagramProfileId: String? = null
)

fun Collaboration.toProposal(isBrand: Boolean): Proposal {
    val pricingList = this.pricing ?: emptyList()
    val pricing = pricingList.firstOrNull()
    
    val proposalType = if (isBrand) {
        if (this.initiatedBy == "BRAND") ProposalType.SENT else ProposalType.RECEIVED
    } else {
        if (this.initiatedBy == "INFLUENCER") ProposalType.SENT else ProposalType.RECEIVED
    }

    // Calculate total from pricing list if totalAmount is null
    val totalSum = this.totalAmount ?: pricingList.sumOf { it.price.toDouble() }

    return Proposal(
        id = this.id,
        otherPartyName = if (isBrand) this.influencer.name else this.brand?.name ?: "Unknown Brand",
        influencerId = this.influencerId,
        brandId = this.brandId,
        campaignTitle = this.campaign.title,
        budget = if (pricing != null) "${pricing.currency} ${pricing.price}" else "N/A",
        deliverable = pricing?.deliverable ?: "N/A",
        platform = pricing?.platform ?: "N/A",
        status = try {
            ProposalStatus.valueOf(this.status.uppercase())
        } catch (e: Exception) {
            ProposalStatus.PENDING
        },
        type = proposalType,
        logoUrl = if (isBrand) this.influencer.logoUrl else this.brand?.logoUrl,
        date = this.createdAt.take(10),
        totalAmount = if (totalSum > 0) "₹$totalSum" else "N/A",
        paymentStatus = this.paymentStatus ?: "pending",
        selectedInstagramProfileId = this.selectedInstagramProfileId
    )
}

@Composable
fun ProposalPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel,
    influencerViewModel: InfluencerViewModel
) {
    var selectedTab by remember { mutableStateOf(ProposalType.RECEIVED) }
    var selectedStatus by remember { mutableStateOf<ProposalStatus?>(null) }

    val authState = authViewModel.authState.observeAsState()
    var isBrand by remember { mutableStateOf(false) }

    // History must read from whichever ViewModel actually holds this user's
    // collaborations — previously this always read brandViewModel, so influencers
    // saw a permanently empty History tab regardless of their real data.
    val brandCollaborations by brandViewModel.collaborations.observeAsState(initial = emptyList())
    val influencerCollaborations by influencerViewModel.filteredCollaborations.observeAsState(initial = emptyList())
    val collaborations = if (isBrand) brandCollaborations else influencerCollaborations

    // Unfiltered by active Instagram profile — an earnings report should cover
    // everything ever received, not just the currently selected profile's view.
    val allInfluencerCollaborations by influencerViewModel.collaborations.observeAsState(initial = emptyList())

    val brandLoading by brandViewModel.loading.observeAsState(initial = false)
    val influencerLoading by influencerViewModel.loading.observeAsState(initial = false)
    val isLoading = if (isBrand) brandLoading else influencerLoading

    val brandError by brandViewModel.error.observeAsState()
    val influencerError by influencerViewModel.error.observeAsState()
    val error = if (isBrand) brandError else influencerError

    LaunchedEffect(authState.value) {
        if (authState.value is np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated) {
             val role = (authState.value as np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated).role
             isBrand = role.equals("BRAND", ignoreCase = true)

             FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                 val firebaseToken = result.token
                 if (firebaseToken != null) {
                     if (isBrand) brandViewModel.fetchCollaborations(firebaseToken)
                     else influencerViewModel.fetchCollaborations(firebaseToken)
                 }
             }
        }
    }

    // Real-time: WebSocket subscription anyCollaborationUpdated fires whenever any
    // collaboration for this user is updated by the other party.
    DisposableEffect(authState.value) {
        if (authState.value !is np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated) {
            return@DisposableEffect onDispose {}
        }
        val isBrandRole = (authState.value as np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated)
            .role.equals("BRAND", ignoreCase = true)

        fun refresh() {
            FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { r ->
                r.token?.let { token ->
                    if (isBrandRole) brandViewModel.fetchCollaborations(token, force = true)
                    else influencerViewModel.fetchCollaborations(token, force = true)
                }
            }
        }

        var wsClient: np.com.bimalkafle.firebaseauthdemoapp.network.CollaborationWebSocketClient? = null
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { r ->
            r.token?.let { token ->
                // collaborationId = null → subscribes to anyCollaborationUpdated (all collabs)
                wsClient = np.com.bimalkafle.firebaseauthdemoapp.network.CollaborationWebSocketClient(
                    token = token,
                    collaborationId = null,
                    onUpdate = ::refresh
                )
                wsClient?.connect()
            }
        }

        onDispose { wsClient?.disconnect() }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    error?.let {
        LaunchedEffect(it) {
            snackbarHostState.showSnackbar(it)
        }
    }

    val proposals = collaborations.map { it.toProposal(isBrand) }
        .filter { it.type == selectedTab && (selectedStatus == null || it.status == selectedStatus) }

    val headerColor = MaterialTheme.colorScheme.primary
    val headerHeight = 100.dp
    val contentPaddingTop = headerHeight - 20.dp

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "History",
                onItemSelected = { /* Handled in the component */ },
                navController = navController,
                isBrand = isBrand
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AiChatFab(navController, size = if (isBrand) 40.dp else 56.dp)
                if (isBrand) {
                    FloatingActionButton(
                        onClick = { navController.navigate("create_campaign") },
                        containerColor = headerColor,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Campaign",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(headerColor)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vector),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.15f),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "History & Proposals",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (!isBrand) {
                    ReportDownloadButton(
                        enabled = allInfluencerCollaborations.isNotEmpty(),
                        fileBaseName = "earnings_report",
                        generatePdf = { InfluencerReportPdfGenerator.generate(allInfluencerCollaborations) },
                        generateCsv = { InfluencerReportCsvGenerator.generate(allInfluencerCollaborations) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                }
            }

            // Content Card
            Column(
                modifier = Modifier
                    .padding(top = contentPaddingTop + 30.dp) // Adjust for status bar
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White)
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                ProposalToggle(selectedTab = selectedTab, onTabSelected = {
                    selectedTab = it
                    selectedStatus = null
                })

                if (isLoading && collaborations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingState(message = "Loading your proposals…")
                    }
                } else {
                    StatusFilterRow(
                        selectedStatus = selectedStatus,
                        onStatusSelected = { status ->
                            selectedStatus = if (selectedStatus == status) null else status
                        }
                    )

                    AppPullToRefreshBox(
                        isRefreshing = isLoading,
                        onRefresh = {
                            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                result.token?.let { token ->
                                    if (isBrand) brandViewModel.fetchCollaborations(token, force = true)
                                    else influencerViewModel.fetchCollaborations(token, force = true)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (proposals.isEmpty()) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    EmptyState(
                                        icon = Icons.Default.Inbox,
                                        title = "No proposals found",
                                        subtitle = if (selectedTab == ProposalType.RECEIVED) "Proposals from brands will show up here." else "Proposals you send will show up here."
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(proposals) { proposal ->
                                    PremiumProposalCard(
                                        proposal = proposal,
                                        isBrand = isBrand,
                                        brandViewModel = brandViewModel,
                                        influencerViewModel = if (!isBrand) influencerViewModel else null,
                                        onClick = {
                                            navController.navigate("collaboration_analytics/${proposal.id}")
                                        },
                                        onChat = {
                                            val otherUserId = if (isBrand) proposal.influencerId else proposal.brandId
                                            val otherUserName = proposal.otherPartyName
                                            navController.navigate("chat/$otherUserId/$otherUserName?collaborationId=${proposal.id}")
                                        },
                                        onAction = { status ->
                                            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                                val token = result.token
                                                if (token != null) {
                                                    if (isBrand) {
                                                        brandViewModel.updateCollaborationStatus(token, proposal.id, status) { }
                                                    } else {
                                                        influencerViewModel.updateCollaborationStatus(token, proposal.id, status) { }
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
        }
    }
}

@Composable
fun ProposalToggle(selectedTab: ProposalType, onTabSelected: (ProposalType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F7))
            .padding(2.dp),
    ) {
        val tabs = listOf(ProposalType.RECEIVED to "Received", ProposalType.SENT to "Sent")
        tabs.forEach { (tab, label) ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun StatusFilterRow(selectedStatus: ProposalStatus?, onStatusSelected: (ProposalStatus) -> Unit) {
    Box(modifier = Modifier.padding(bottom = 4.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ProposalStatus.entries.toTypedArray()) { status ->
                val isSelected = selectedStatus == status
                FilterChip(
                    selected = isSelected,
                    onClick = { onStatusSelected(status) },
                    label = { Text(status.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = status.color.copy(alpha = 0.15f),
                        selectedLabelColor = status.color,
                        labelColor = Color.Gray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) status.color else Color.LightGray,
                        selectedBorderColor = status.color,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.2.dp,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}

@Composable
fun PremiumProposalCard(
    proposal: Proposal,
    isBrand: Boolean,
    brandViewModel: BrandViewModel,
    onClick: () -> Unit,
    onChat: () -> Unit,
    onAction: (String) -> Unit,
    influencerViewModel: InfluencerViewModel? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val igProfileUsername = remember(proposal.selectedInstagramProfileId) {
        if (proposal.selectedInstagramProfileId == null) null
        else influencerViewModel?.influencerProfile?.value?.instagramProfiles
            ?.find { it.id == proposal.selectedInstagramProfileId }?.username
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8F9FA))
                ) {
                    if (proposal.logoUrl != null) {
                        AsyncImage(
                            model = proposal.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.brand_profile)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.brand_profile),
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proposal.otherPartyName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF1D1D1F)
                    )
                    Text(
                        text = proposal.campaignTitle,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    if (!isBrand && igProfileUsername != null) {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFE1306C).copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFE1306C),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "@$igProfileUsername",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE1306C),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                PremiumStatusBadge(status = proposal.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFF2F2F7), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                InfoItem("Budget", proposal.totalAmount, Icons.Default.CurrencyRupee)
                InfoItem("Platform", proposal.platform, Icons.Default.Public)
                InfoItem("Date", proposal.date, Icons.Default.CalendarToday)
            }

            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chat Button is always available for active/pending collaborations
                OutlinedButton(
                    onClick = { onChat() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (proposal.type == ProposalType.RECEIVED && proposal.status == ProposalStatus.PENDING) {
                    Button(
                        onClick = { onAction("ACCEPTED") },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Accept", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (proposal.type == ProposalType.SENT && proposal.status == ProposalStatus.PENDING) {
                    Button(
                        onClick = { onAction("REVOKED") },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D1D1F)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Revoke", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (isBrand && (proposal.status == ProposalStatus.ACCEPTED || proposal.status == ProposalStatus.WAITING_FOR_PAYMENT)) {
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                val token = result.token
                                if (token != null) {
                                    brandViewModel.createCollaborationPaymentOrder(token, proposal.id, "FULL") { orderData ->
                                        if (orderData != null && activity != null) {
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
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.width(3.dp))
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D1D1F),
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
fun PremiumStatusBadge(status: ProposalStatus) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(status.color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(status.icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = status.color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.displayName,
                color = status.color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProposalPagePreview() {
    // Preview with dummy data
}
