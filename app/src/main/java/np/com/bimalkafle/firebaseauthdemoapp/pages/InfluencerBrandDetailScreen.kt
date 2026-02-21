package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.Brand
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignDetail
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val themeColor = Color(0xFFFF8383)
private val softGray = Color(0xFFF8F9FA)
private val darkerGray = Color(0xFF6C757D)
private val cardBg = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluencerBrandDetailScreen(
    navController: NavController,
    campaignId: String,
    campaignViewModel: CampaignViewModel
) {
    val campaign by campaignViewModel.campaign.observeAsState()
    val isLoading by campaignViewModel.loading.observeAsState(false)
    val error by campaignViewModel.error.observeAsState()

    LaunchedEffect(campaignId) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener {
            val token = it.token
            if (token != null) {
                campaignViewModel.fetchCampaignById(campaignId, token)
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (campaign != null) {
                CollaborateButton(campaign = campaign!!)
            }
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(bottom = padding.calculateBottomPadding())
            .background(softGray)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = themeColor)
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (campaign != null) {
                InfluencerBrandDetailContent(campaign = campaign!!, navController = navController)
            } else {
                Text("Campaign not found.", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun InfluencerBrandDetailContent(campaign: CampaignDetail, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HeaderSection(campaign = campaign, navController = navController)

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ABOUT CAMPAIGN",
                style = MaterialTheme.typography.labelLarge,
                color = darkerGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            CampaignInfoSection(campaign = campaign)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BUDGET & TIMELINE",
                style = MaterialTheme.typography.labelLarge,
                color = darkerGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            BudgetAndTimelineSection(campaign = campaign)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BRAND DETAILS",
                style = MaterialTheme.typography.labelLarge,
                color = darkerGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            BrandInfoSection(brand = campaign.brand)
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun HeaderSection(campaign: CampaignDetail, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(themeColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.2f),
            contentScale = ContentScale.Crop
        )
        
        // Top Bar
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 16.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Text(
                text = "Campaign Details",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(100.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                shadowElevation = 8.dp
            ) {
                AsyncImage(
                    model = campaign.brand?.logoUrl,
                    contentDescription = "Brand Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = campaign.brand?.name ?: "Brand Name",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingStars(rating = campaign.brand?.averageRating ?: 0.0)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rating: ${"%.1f".format(campaign.brand?.averageRating ?: 0.0)} / 5.0",
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RatingStars(rating: Double) {
    Row {
        repeat(5) { index ->
            val color = if (index < rating.toInt()) Color(0xFFFFD700) else Color.White.copy(alpha = 0.5f)
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CampaignInfoSection(campaign: CampaignDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Surface(
                color = themeColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(themeColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE CAMPAIGN",
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = campaign.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = themeColor
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = campaign.description,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = darkerGray
            )
        }
    }
}

@Composable
fun BudgetAndTimelineSection(campaign: CampaignDetail) {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    val outputFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())

    fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "TBD"
        return try {
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString ?: "TBD"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = themeColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = themeColor)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("BUDGET RANGE", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = darkerGray)
                    Text(
                        text = "₹${campaign.budgetMin} — ₹${campaign.budgetMax}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimelineBox(
                    label = "START DATE",
                    date = formatDate(campaign.startDate),
                    modifier = Modifier.weight(1f)
                )
                TimelineBox(
                    label = "END DATE",
                    date = formatDate(campaign.endDate),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TimelineBox(label: String, date: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = themeColor.copy(alpha = 0.05f),
        border = BorderStroke(width = 1.dp, color = themeColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth, 
                contentDescription = null, 
                tint = themeColor, 
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = label, fontSize = 10.sp, color = darkerGray, fontWeight = FontWeight.Bold)
                Text(text = date, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Black)
            }
        }
    }
}

@Composable
fun BrandInfoSection(brand: Brand?) {
    if (brand == null) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header with color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColor)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Brand Overview", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        val categoryText = brand.brandCategory?.let { 
                            if (it.subCategory.isNotEmpty()) "${it.category} • ${it.subCategory}" else it.category 
                        } ?: "N/A"
                        Text(categoryText, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                val categoryValue = brand.brandCategory?.let { 
                    if (it.subCategory.isNotEmpty()) "${it.category} (${it.subCategory})" else it.category 
                } ?: "N/A"
                BrandDetailRow(Icons.Default.Category, "CATEGORY", categoryValue)
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = softGray)
                
                BrandDetailRow(Icons.Default.Language, "PREFERRED PLATFORMS", brand.preferredPlatforms?.joinToString(", ") { it.platform } ?: "N/A")
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = softGray)
                
                val target = brand.targetAudience?.let { "${it.ageMin}-${it.ageMax} yrs, ${it.gender}" } ?: "N/A"
                BrandDetailRow(Icons.Default.People, "TARGET AUDIENCE", target)
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = softGray)
                
                BrandDetailRow(Icons.Default.LocationOn, "LOCATIONS", brand.targetAudience?.locations?.joinToString(", ") ?: "N/A")
            }
        }
    }
}

@Composable
fun BrandDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = themeColor.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 10.sp, color = darkerGray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CollaborateButton(campaign: CampaignDetail) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = { /* TODO: Implement collaboration action */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "COLLABORATE NOW", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
