package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val brandProfile by brandViewModel.brandProfile.observeAsState()

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            result.token?.let { token ->
                brandViewModel.fetchMyCampaigns(token)
                brandViewModel.fetchBrandDetails(token)
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.3f
    val formPaddingTop = headerHeight - 80.dp

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(brandThemeColor)
        ) {
            Image(
                painter = painterResource(id = R.drawable.vector),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.2f),
                contentScale = ContentScale.Crop
            )
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(8.dp, CircleShape)
                ) {
                    if (!brandProfile?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = brandProfile?.logoUrl,
                            contentDescription = "Brand Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.brand_profile),
                            contentDescription = "Brand Profile",
                            modifier = Modifier.fillMaxSize().padding(12.dp).clip(CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = brandProfile?.name ?: "My Brand",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "MY CAMPAIGNS",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
                color = brandThemeColor
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
                    Text(text = error ?: "Unknown error", color = Color.Red, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                            result.token?.let { brandViewModel.fetchMyCampaigns(it) }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = brandThemeColor)) {
                        Text("Retry")
                    }
                }
            } else if (myCampaigns.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No campaigns created yet.",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = campaign.title,
                    fontSize = 18.sp,
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
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Platforms & Formats
            if (!campaign.platforms.isNullOrEmpty()) {
                Text("PLATFORMS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = brandThemeColor)
                Spacer(modifier = Modifier.height(8.dp))
                campaign.platforms.forEach { platform ->
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(platform.platform, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            platform.formats?.forEach { format ->
                                Surface(
                                    color = Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = format,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
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
                    text = "Posted on ${formatDate(campaign.createdAt)}",
                    fontSize = 11.sp,
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
            fontSize = 11.sp,
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
            Text(text = label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

fun formatDate(dateString: String?): String {
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
