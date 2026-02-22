package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val brandThemeColor = Color(0xFFFF8383)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllCampaignPage(
    navController: NavController,
    brandViewModel: BrandViewModel
) {
    val myCampaigns by brandViewModel.myCampaigns.observeAsState(emptyList())
    val isLoading by brandViewModel.loading.observeAsState(false)
    val error by brandViewModel.error.observeAsState()

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            result.token?.let { token ->
                brandViewModel.fetchMyCampaigns(token)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Campaigns", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = brandThemeColor)
            } else if (error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = error ?: "Unknown error", color = Color.Red)
                    Button(onClick = {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                            result.token?.let { brandViewModel.fetchMyCampaigns(it) }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = brandThemeColor)) {
                        Text("Retry")
                    }
                }
            } else if (myCampaigns.isEmpty()) {
                Text(
                    text = "No campaigns created yet.",
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 18.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(myCampaigns) { campaign ->
                        CampaignDetailCard(campaign)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampaignDetailCard(campaign: Campaign) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = campaign.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                
                campaign.status?.let { status ->
                    StatusBadge(status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = campaign.description ?: "No description provided",
                fontSize = 14.sp,
                color = Color.DarkGray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Platforms & Formats
            if (!campaign.platforms.isNullOrEmpty()) {
                Text("Platforms & Formats", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                campaign.platforms.forEach { platform ->
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(platform.platform, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = brandThemeColor)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            platform.formats?.forEach { format ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(format, fontSize = 10.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Budget Section
            CampaignInfoRow(
                icon = Icons.Default.CurrencyRupee,
                label = "Budget",
                value = if (campaign.budgetMin != null && campaign.budgetMax != null) {
                    "₹${campaign.budgetMin} - ₹${campaign.budgetMax}"
                } else if (campaign.budgetMin != null) {
                    "From ₹${campaign.budgetMin}"
                } else if (campaign.budgetMax != null) {
                    "Up to ₹${campaign.budgetMax}"
                } else "Not specified"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dates Section
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    CampaignInfoRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Starts",
                        value = formatDate(campaign.startDate)
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CampaignInfoRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Ends",
                        value = formatDate(campaign.endDate)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Footer with Created Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Created on ${formatDate(campaign.createdAt)}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val backgroundColor = when (status.uppercase()) {
        "ACTIVE" -> Color(0xFFE8F5E9)
        "DRAFT" -> Color(0xFFFFF3E0)
        "COMPLETED" -> Color(0xFFE3F2FD)
        else -> Color(0xFFF5F5F5)
    }
    val textColor = when (status.uppercase()) {
        "ACTIVE" -> Color(0xFF2E7D32)
        "DRAFT" -> Color(0xFFEF6C00)
        "COMPLETED" -> Color(0xFF1565C0)
        else -> Color(0xFF757575)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun CampaignInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(brandThemeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = brandThemeColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

fun formatDate(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return "N/A"
    return try {
        // Handle format like "2023-10-15T..." or "2023-10-15"
        val instantStr = if (dateString.contains("T")) dateString else "${dateString}T00:00:00Z"
        val instant = Instant.parse(instantStr)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        dateString.split("T").firstOrNull() ?: dateString
    }
}
