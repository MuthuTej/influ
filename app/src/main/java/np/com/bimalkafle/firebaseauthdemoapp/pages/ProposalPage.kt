@file:OptIn(ExperimentalMaterial3Api::class)

package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.bimalkafle.firebaseauthdemoapp.R

enum class ProposalStatus(val displayName: String, val color: Color, val icon: ImageVector) {
    ACCEPTED("Accepted", Color(0xFF4CAF50), Icons.Default.CheckCircle),
    REJECTED("Rejected", Color(0xFFF44336), Icons.Default.HighlightOff),
    PENDING("Pending", Color(0xFFFFC107), Icons.Default.HourglassEmpty)
}

enum class ProposalType {
    SENT,
    RECEIVED
}

data class Proposal(
    val id: Int,
    val brandName: String,
    val brandLogo: Int,
    val campaignName: String,
    val budget: String,
    val deliverable: String,
    val duration: String,
    val status: ProposalStatus,
    val type: ProposalType
)

val sampleProposals = listOf(
    Proposal(1, "Myntra", R.drawable.brand_profile, "Christmas special colab", "₹ 50K - 60K", "2 post | 2 stories", "Feb 14 - Mar 14", ProposalStatus.ACCEPTED, ProposalType.SENT),
    Proposal(2, "Myntra", R.drawable.brand_profile, "Christmas special colab", "₹ 50K - 60K", "2 post | 2 stories", "Feb 14 - Mar 14", ProposalStatus.PENDING, ProposalType.SENT),
    Proposal(3, "Myntra", R.drawable.brand_profile, "Christmas special colab", "₹ 50K - 60K", "2 post | 2 stories", "Feb 14 - Mar 14", ProposalStatus.REJECTED, ProposalType.SENT),
    Proposal(4, "Myntra", R.drawable.brand_profile, "Christmas special colab", "₹ 50K - 60K", "2 post | 2 stories", "Feb 14 - Mar 14", ProposalStatus.ACCEPTED, ProposalType.RECEIVED),
    Proposal(5, "Myntra", R.drawable.brand_profile, "Christmas special colab", "₹ 50K - 60K", "2 post | 2 stories", "Feb 14 - Mar 14", ProposalStatus.PENDING, ProposalType.RECEIVED),
    Proposal(6, "Myntra", R.drawable.brand_profile, "Christmas special colab", "₹ 50K - 60K", "2 post | 2 stories", "Feb 14 - Mar 14", ProposalStatus.REJECTED, ProposalType.RECEIVED),
)

@Composable
fun ProposalPage(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(ProposalType.SENT) }
    var selectedStatus by remember { mutableStateOf<ProposalStatus?>(null) }

    val proposals = sampleProposals.filter { it.type == selectedTab && (selectedStatus == null || it.status == selectedStatus) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val headerHeight = screenHeight * 0.15f
    val contentPaddingTop = headerHeight - 30.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFF8383))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 24.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Proposals",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Content Card
        Column(
            modifier = Modifier
                .padding(top = contentPaddingTop)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
        ) {
            ProposalToggle(selectedTab = selectedTab, onTabSelected = { 
                selectedTab = it
                selectedStatus = null // Reset status filter when tab changes
            })
            ProposalSummary(proposals = sampleProposals.filter { it.type == selectedTab }, selectedStatus = selectedStatus, onStatusSelected = { status ->
                selectedStatus = if (selectedStatus == status) null else status
            })
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(proposals) { proposal ->
                    ProposalCard(proposal = proposal)
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
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFF8383).copy(alpha = 0.1f))
            .padding(4.dp),
    ) {
        Button(
            onClick = { onTabSelected(ProposalType.SENT) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedTab == ProposalType.SENT) Color(0xFFFF8383) else Color.Transparent,
                contentColor = if (selectedTab == ProposalType.SENT) Color.White else Color.Black
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Sent")
        }
        Button(
            onClick = { onTabSelected(ProposalType.RECEIVED) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedTab == ProposalType.RECEIVED) Color(0xFFFF8383) else Color.Transparent,
                contentColor = if (selectedTab == ProposalType.RECEIVED) Color.White else Color.Black
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Received")
        }
    }
}

@Composable
fun ProposalSummary(proposals: List<Proposal>, selectedStatus: ProposalStatus?, onStatusSelected: (ProposalStatus) -> Unit) {
    val summary = proposals.groupingBy { it.status }.eachCount()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        FilterChip(
            selected = selectedStatus == ProposalStatus.ACCEPTED,
            onClick = { onStatusSelected(ProposalStatus.ACCEPTED) },
            label = { Text("Accepted ${summary[ProposalStatus.ACCEPTED] ?: 0}") },
            leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            border = BorderStroke(1.dp, if (selectedStatus == ProposalStatus.ACCEPTED) Color(0xFF4CAF50) else Color.Gray)
        )
        FilterChip(
            selected = selectedStatus == ProposalStatus.REJECTED,
            onClick = { onStatusSelected(ProposalStatus.REJECTED) },
            label = { Text("Rejected ${summary[ProposalStatus.REJECTED] ?: 0}") },
            leadingIcon = { Icon(Icons.Default.HighlightOff, contentDescription = null) },
            border = BorderStroke(1.dp, if (selectedStatus == ProposalStatus.REJECTED) Color(0xFFF44336) else Color.Gray)
        )
        FilterChip(
            selected = selectedStatus == ProposalStatus.PENDING,
            onClick = { onStatusSelected(ProposalStatus.PENDING) },
            label = { Text("Pending ${summary[ProposalStatus.PENDING] ?: 0}") },
            leadingIcon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null) },
            border = BorderStroke(1.dp, if (selectedStatus == ProposalStatus.PENDING) Color(0xFFFFC107) else Color.Gray)
        )
    }
}

@Composable
fun ProposalCard(proposal: Proposal) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = proposal.brandLogo),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(proposal.brandName, fontWeight = FontWeight.Bold)
                    Text(proposal.campaignName, fontSize = 12.sp, color = Color.Gray)
                }
                StatusBadge(status = proposal.status)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Budget", fontSize = 12.sp, color = Color.Gray)
                    Text(proposal.budget, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Deliverable", fontSize = 12.sp, color = Color.Gray)
                    Text(proposal.deliverable, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Duration", fontSize = 12.sp, color = Color.Gray)
                    Text(proposal.duration, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { /*TODO*/ }) {
                    Text("View Campaign", color = Color.Gray)
                }
                if (proposal.type == ProposalType.SENT) {
                    when (proposal.status) {
                        ProposalStatus.PENDING -> {
                            Button(
                                onClick = { /*TODO*/ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Withdraw")
                            }
                        }
                        else -> {}
                    }
                } else { // RECEIVED
                    if (proposal.status == ProposalStatus.PENDING) {
                        Button(
                            onClick = { /*TODO*/ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Accept")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { /*TODO*/ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reject")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ProposalStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(status.color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = status.icon,
            contentDescription = status.displayName,
            tint = status.color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = status.displayName,
            color = status.color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProposalPagePreview() {
    ProposalPage(onBack = {})
}

@Preview
@Composable
fun StatusBadgePreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBadge(status = ProposalStatus.ACCEPTED)
        StatusBadge(status = ProposalStatus.REJECTED)
        StatusBadge(status = ProposalStatus.PENDING)
    }
}
