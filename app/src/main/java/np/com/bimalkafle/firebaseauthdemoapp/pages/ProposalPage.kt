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

import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar

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
    val campaignTitle: String,
    val budget: String,
    val deliverable: String,
    val platform: String,
    val status: ProposalStatus,
    val type: ProposalType,
    val logoUrl: String?,
    val date: String,
    val totalAmount: String,
    val paymentStatus: String
)

fun Collaboration.toProposal(isBrandView: Boolean): Proposal {
    val pricing = this.pricing?.firstOrNull()
    val isInitiatedByBrand = this.initiatedBy == "BRAND"
    
    val proposalType = if (isBrandView) {
        if (isInitiatedByBrand) ProposalType.SENT else ProposalType.RECEIVED
    } else {
        if (isInitiatedByBrand) ProposalType.RECEIVED else ProposalType.SENT
    }

    val otherPartyName = if (isBrandView) this.influencer.name else this.brand?.name ?: "Unknown Brand"
    val otherPartyLogo = if (isBrandView) this.influencer.logoUrl else this.brand?.logoUrl

    return Proposal(
        id = this.id,
        otherPartyName = otherPartyName,
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
        logoUrl = otherPartyLogo,
        date = this.createdAt.take(10),
        totalAmount = if (this.totalAmount != null) "₹${this.totalAmount}" else "N/A",
        paymentStatus = this.paymentStatus ?: "pending"
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
    val authState by authViewModel.authState.observeAsState()
    var isBrand by remember { mutableStateOf(false) }
    
    LaunchedEffect(authState) {
        if (authState is np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated) {
             val role = (authState as np.com.bimalkafle.firebaseauthdemoapp.AuthState.Authenticated).role
             isBrand = role.equals("BRAND", ignoreCase = true)
        }
    }

    var selectedTab by remember { mutableStateOf(ProposalType.RECEIVED) }
    var selectedStatus by remember { mutableStateOf<ProposalStatus?>(null) }

    val brandCollaborations by brandViewModel.collaborations.observeAsState(initial = emptyList())
    val influencerCollaborations by influencerViewModel.collaborations.observeAsState(initial = emptyList())
    
    val collaborations = if (isBrand) brandCollaborations else influencerCollaborations
    val isLoading = if (isBrand) brandViewModel.loading.observeAsState(false).value else influencerViewModel.loading.observeAsState(false).value
    val error = if (isBrand) brandViewModel.error.observeAsState().value else influencerViewModel.error.observeAsState().value

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isBrand) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val firebaseToken = result.token
            if (firebaseToken != null) {
                if (isBrand) brandViewModel.fetchCollaborations(firebaseToken)
                else influencerViewModel.fetchCollaborations(firebaseToken)
            }
        }
    }

    error?.let {
        LaunchedEffect(it) {
            snackbarHostState.showSnackbar(it)
        }
    }

    val proposals = collaborations.map { it.toProposal(isBrandView = isBrand) }
        .filter { it.type == selectedTab && (selectedStatus == null || it.status == selectedStatus) }

    val headerHeight = 120.dp
    val contentPaddingTop = headerHeight - 20.dp

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "History",
                onItemSelected = { /* Handled in component */ },
                navController = navController,
                isBrand = isBrand
            )
        },
        floatingActionButton = {
            if (isBrand) {
                FloatingActionButton(
                    onClick = { navController.navigate("create_campaign") },
                    containerColor = Color(0xFFFF8383),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Campaign", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFF8383))
        ) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth().height(headerHeight)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vector),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.15f),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "History & Proposals",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Content Card
            Column(
                modifier = Modifier
                    .padding(top = contentPaddingTop)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color.White)
            ) {
                ProposalToggle(selectedTab = selectedTab, onTabSelected = {
                    selectedTab = it
                    selectedStatus = null
                })

                if (isLoading && collaborations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF8383))
                    }
                } else {
                    StatusFilterRow(
                        selectedStatus = selectedStatus,
                        onStatusSelected = { status ->
                            selectedStatus = if (selectedStatus == status) null else status
                        }
                    )

                    if (proposals.isEmpty()) {
                        EmptyState(isLoading)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(proposals) { proposal ->
                                PremiumProposalCard(
                                    proposal = proposal,
                                    onAction = { status ->
                                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                            val token = result.token
                                            if (token != null) {
                                                if (isBrand) {
                                                    brandViewModel.updateCollaborationStatus(token, proposal.id, status) { /* Refresh in VM */ }
                                                } else {
                                                    influencerViewModel.updateCollaborationStatus(token, proposal.id, status) { /* Refresh in VM */ }
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

@Composable
fun ProposalToggle(selectedTab: ProposalType, onTabSelected: (ProposalType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F7))
            .padding(4.dp),
    ) {
        val tabs = listOf(ProposalType.RECEIVED to "Received", ProposalType.SENT to "Sent")
        tabs.forEach { (tab, label) ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFFFF8383) else Color.Gray,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun StatusFilterRow(selectedStatus: ProposalStatus?, onStatusSelected: (ProposalStatus) -> Unit) {
    Box(modifier = Modifier.padding(bottom = 8.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ProposalStatus.entries.toTypedArray()) { status ->
                val isSelected = selectedStatus == status
                FilterChip(
                    selected = isSelected,
                    onClick = { onStatusSelected(status) },
                    label = { Text(status.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = status.color.copy(alpha = 0.15f),
                        selectedLabelColor = status.color,
                        labelColor = Color.Gray
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) status.color else Color.LightGray,
                        selectedBorderColor = status.color,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }
    }
}

@Composable
fun PremiumProposalCard(proposal: Proposal, onAction: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8F9FA))
                ) {
                    if (proposal.logoUrl != null) {
                        AsyncImage(
                            model = proposal.logoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.brand_profile),
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = proposal.otherPartyName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1D1D1F)
                    )
                    Text(
                        text = proposal.campaignTitle,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                PremiumStatusBadge(status = proposal.status)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = Color(0xFFF2F2F7))
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                InfoItem("Total Amount", proposal.totalAmount, Icons.Default.CurrencyRupee)
                InfoItem("Platform", proposal.platform, Icons.Default.Public)
                InfoItem("Date", proposal.date, Icons.Default.CalendarToday)
            }

            if (proposal.type == ProposalType.RECEIVED && proposal.status == ProposalStatus.PENDING) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onAction("ACCEPTED") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Accept", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAction("REJECTED") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                        border = BorderStroke(1.dp, Color(0xFFF44336)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Reject", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (proposal.type == ProposalType.SENT && proposal.status == ProposalStatus.PENDING) {
                 Spacer(modifier = Modifier.height(20.dp))
                 Button(
                    onClick = { onAction("REVOKED") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D1D1F)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Withdraw Proposal", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, icon: ImageVector) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, size = 12.dp, tint = Color.Gray)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1D1D1F),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun Icon(icon: ImageVector, contentDescription: String?, size: Dp, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = tint
    )
}

@Composable
fun PremiumStatusBadge(status: ProposalStatus) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(status.color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(status.icon, contentDescription = null, size = 14.dp, tint = status.color)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.displayName,
                color = status.color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyState(isLoading: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("No proposals found", color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}
