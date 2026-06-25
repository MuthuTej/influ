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
import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val brandThemeColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllCampaignPage(
    navController: NavController,
    brandViewModel: BrandViewModel
) {
    val myCampaigns by brandViewModel.myCampaigns.observeAsState(emptyList())
    val isLoading by brandViewModel.loading.observeAsState(false)
    val error by brandViewModel.error.observeAsState()
    val brandProfile by brandViewModel.brandProfile.observeAsState()

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
                        "MY CAMPAIGNS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 12.dp),
                        color = brandThemeColor,
                        textAlign = TextAlign.Center
                    )

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
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(myCampaigns) { campaign ->
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
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            
            // Platforms Section
            if (!campaign.platforms.isNullOrEmpty()) {
                campaign.platforms.forEach { platform ->
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconRes = when (platform.platform.uppercase()) {
                            "INSTAGRAM" -> R.drawable.ic_instagram
                            "YOUTUBE" -> R.drawable.ic_youtube
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = platform.platform, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 12.sp, 
                            color = brandThemeColor
                        )
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
                CampaignInfoRow(
                    modifier = Modifier.weight(1.1f),
                    icon = Icons.Default.CurrencyRupee,
                    label = "Budget",
                    value = if (campaign.budgetMin != null && campaign.budgetMax != null) {
                        "₹${campaign.budgetMin} - ₹${campaign.budgetMax}"
                    } else if (campaign.budgetMin != null) {
                        "₹${campaign.budgetMin}+"
                    } else if (campaign.budgetMax != null) {
                        "Up to ₹${campaign.budgetMax}"
                    } else "TBD"
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
            text = status.uppercase(),
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
