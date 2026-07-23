package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.components.ReportDownloadButton
import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.utils.CampaignReportCsvGenerator
import np.com.bimalkafle.firebaseauthdemoapp.utils.CampaignReportPdfGenerator
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val brandThemeColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary

private fun formatBudget(amount: Int?): String {
    if (amount == null) return "—"
    return when {
        amount >= 100_000 -> "${"%.1f".format(amount / 100_000.0)}L"
        amount >= 1_000 -> "${"%.1f".format(amount / 1_000.0)}K"
        else -> "$amount"
    }
}

/** Collaboration statuses that indicate real work is underway on a campaign —
 * anything past the applicant/negotiation stage. Mirrors the admin
 * dashboard's "Active" collaboration group. */
private val ONGOING_COLLABORATION_STATUSES = setOf(
    "ACCEPTED", "BRIEF_SENT", "BRIEF_FINALIZED", "SCRIPT_SENT", "IN_PROGRESS", "WAITING_FOR_PAYMENT", "COMPLETED"
)

private enum class CampaignFilter(val label: String) {
    ALL("All"), PENDING("Pending"), ONGOING("Ongoing"), COMPLETED("Completed")
}

/** Campaigns only ever carry status OPEN/CLOSED on the backend, so the
 * Pending/Ongoing split for an OPEN campaign is derived from whether any of
 * its collaborations have progressed past the applicant stage. CLOSED always
 * means Completed. */
private fun campaignFilterBucket(campaign: Campaign): CampaignFilter {
    if (campaign.status?.uppercase() == "CLOSED") return CampaignFilter.COMPLETED
    val hasOngoingCollaboration = campaign.collaborations?.any {
        it.status.uppercase() in ONGOING_COLLABORATION_STATUSES
    } ?: false
    return if (hasOngoingCollaboration) CampaignFilter.ONGOING else CampaignFilter.PENDING
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllCampaignPage(
    navController: NavController,
    brandViewModel: BrandViewModel,
    initialFilter: String? = null
) {
    val myCampaigns by brandViewModel.myCampaigns.observeAsState(emptyList())
    val isLoading by brandViewModel.loading.observeAsState(false)
    val error by brandViewModel.error.observeAsState()
    val brandProfile by brandViewModel.brandProfile.observeAsState()

    var selectedFilter by remember {
        mutableStateOf(
            CampaignFilter.values().firstOrNull { it.name.equals(initialFilter, ignoreCase = true) }
                ?: CampaignFilter.ALL
        )
    }
    val filteredCampaigns = remember(myCampaigns, selectedFilter) {
        if (selectedFilter == CampaignFilter.ALL) myCampaigns
        else myCampaigns.filter { campaignFilterBucket(it) == selectedFilter }
    }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            result.token?.let { token ->
                brandViewModel.fetchMyCampaigns(token)
                brandViewModel.fetchBrandDetails(token)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(brandThemeColor)) {
        // Background Decorative Image
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .alpha(0.2f),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    ReportDownloadButton(
                        enabled = myCampaigns.isNotEmpty(),
                        fileBaseName = "all_campaigns_report",
                        generatePdf = { CampaignReportPdfGenerator.generateOverall(myCampaigns) },
                        generateCsv = { CampaignReportCsvGenerator.generateOverall(myCampaigns) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(70.dp)
                        .shadow(8.dp, CircleShape)
                ) {
                    if (!brandProfile?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = brandProfile?.logoUrl,
                            contentDescription = "Brand Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.brand_profile),
                            contentDescription = "Brand Profile",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = brandProfile?.name ?: "My Brand",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Campaigns List Card
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        "My campaigns",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 12.dp),
                        color = brandThemeColor,
                        textAlign = TextAlign.Center
                    )

                    if (!isLoading && error == null && myCampaigns.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CampaignFilter.values().forEach { filter ->
                                LocationChip(
                                    name = filter.label,
                                    isSelected = selectedFilter == filter,
                                    onSelected = { selectedFilter = filter }
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = brandThemeColor)
                        }
                    } else if (error != null) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = error ?: "Unknown error", color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                                        result.token?.let { brandViewModel.fetchMyCampaigns(it) }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = brandThemeColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (myCampaigns.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No campaigns created yet.",
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else if (filteredCampaigns.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No ${selectedFilter.label.lowercase()} campaigns.",
                                    fontSize = 16.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredCampaigns) { campaign ->
                                CampaignDetailCard(campaign, onClick = {
                                    navController.navigate("brand_campaign_detail/${campaign.id}")
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CampaignDetailCard(campaign: Campaign, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF2F2F2))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = campaign.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                campaign.status?.let { status ->
                    StatusBadge(status)
                }
            }

            Text(
                text = campaign.description ?: "No description provided",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            
            // Platforms Section
            if (!campaign.platforms.isNullOrEmpty()) {
                campaign.platforms.forEach { platform ->
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val platformType = platform.platform.uppercase()
                        if (platformType == "INSTAGRAM") {
                            Image(
                                painter = painterResource(id = R.drawable.instagram_logo),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (platformType == "YOUTUBE") {
                            Image(
                                painter = painterResource(id = R.drawable.youtube_logo),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            val iconRes = when (platformType) {
                                "FACEBOOK" -> R.drawable.ic_facebook
                                "TWITTER" -> R.drawable.ic_twitter
                                else -> R.drawable.ic_instagram
                            }
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = brandThemeColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            platform.formats?.forEach { format ->
                                Surface(
                                    color = Color(0xFFF1F3F5),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = format,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Medium,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF8F8F8), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            // Info Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val budgetValue = if (campaign.budgetMin != null || campaign.budgetMax != null) {
                    "${formatBudget(campaign.budgetMin)} - ${formatBudget(campaign.budgetMax)}"
                } else "TBD"

                CampaignInfoRow(
                    modifier = Modifier.weight(1.1f),
                    icon = Icons.Default.CurrencyRupee,
                    label = "Budget",
                    value = budgetValue
                )
                CampaignInfoRow(
                    modifier = Modifier.weight(0.9f),
                    icon = Icons.Default.CalendarToday,
                    label = "Timeline",
                    value = if (!campaign.startDate.isNullOrEmpty() && !campaign.endDate.isNullOrEmpty()) {
                        "${formatDateShort(campaign.startDate)} - ${formatDateShort(campaign.endDate)}"
                    } else "N/A"
                )
            }

            Text(
                text = "Created ${formatDateFull(campaign.createdAt)}",
                fontSize = 9.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val isPrimaryStyle = status.uppercase() == "ACTIVE" || status.uppercase() == "OPEN"
    val backgroundColor = if (isPrimaryStyle) brandThemeColor.copy(alpha = 0.1f) else when (status.uppercase()) {
        "DRAFT" -> Color(0xFFFFF3E0)
        "COMPLETED" -> Color(0xFFE3F2FD)
        else -> Color(0xFFF5F5F5)
    }
    val textColor = if (isPrimaryStyle) brandThemeColor else when (status.uppercase()) {
        "DRAFT" -> Color(0xFFEF6C00)
        "COMPLETED" -> Color(0xFF1565C0)
        else -> Color(0xFF757575)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = textColor
        )
    }
}

@Composable
fun CampaignInfoRow(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(brandThemeColor.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = brandThemeColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
        }
    }
}

fun formatDateShort(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return "N/A"
    return try {
        val instant = if (dateString.contains("T")) Instant.parse(dateString) else Instant.parse("${dateString}T00:00:00Z")
        val formatter = DateTimeFormatter.ofPattern("d MMM")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        dateString.split("T").firstOrNull() ?: dateString
    }
}

fun formatDateFull(dateString: String?): String {
    if (dateString.isNullOrEmpty()) return "N/A"
    return try {
        val instant = if (dateString.contains("T")) Instant.parse(dateString) else Instant.parse("${dateString}T00:00:00Z")
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        dateString.split("T").firstOrNull() ?: dateString
    }
}
