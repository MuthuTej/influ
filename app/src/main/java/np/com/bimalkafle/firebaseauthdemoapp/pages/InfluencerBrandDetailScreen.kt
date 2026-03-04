package np.com.bimalkafle.firebaseauthdemoapp.pages

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
import androidx.compose.material.icons.filled.Wc
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
private val softGray = Color(0xFFF8F9FE)
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
                CollaborateButton(campaign = campaign!!, navController = navController)
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
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 14.sp
                )
            } else if (campaign != null) {
                InfluencerBrandDetailContent(campaign = campaign!!, navController = navController)
            } else {
                Text("Campaign not found.", modifier = Modifier.align(Alignment.Center), fontSize = 14.sp)
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

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            DetailSectionTitle("ABOUT CAMPAIGN")
            CampaignInfoSection(campaign = campaign)

            Spacer(modifier = Modifier.height(16.dp))

            DetailSectionTitle("BUDGET & TIMELINE")
            BudgetAndTimelineSection(campaign = campaign)

            Spacer(modifier = Modifier.height(16.dp))

            DetailSectionTitle("REQUIREMENTS")
            CampaignRequirementsSection(campaign = campaign)

            Spacer(modifier = Modifier.height(16.dp))

            DetailSectionTitle("BRAND DETAILS")
            BrandInfoSection(brand = campaign.brand)
            
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = darkerGray,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
        letterSpacing = 0.5.sp
    )
}

@Composable
fun HeaderSection(campaign: CampaignDetail, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp) // Reduced height
            .background(themeColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.15f),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 8.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            
            Text(
                text = "Campaign Details",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier
                    .size(80.dp) // Smaller logo
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                shadowElevation = 6.dp
            ) {
                AsyncImage(
                    model = campaign.brand?.logoUrl,
                    contentDescription = "Brand Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = campaign.brand?.name ?: "Brand Name",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp, // Smaller text
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingStars(rating = campaign.brand?.averageRating ?: 0.0)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${"%.1f".format(campaign.brand?.averageRating ?: 0.0)} / 5.0",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
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
            val color = if (index < rating.toInt()) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f)
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun CampaignInfoSection(campaign: CampaignDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = themeColor.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(themeColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = themeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = campaign.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = campaign.description,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = darkerGray,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun BudgetAndTimelineSection(campaign: CampaignDetail) {
    fun formatCampaignDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "TBD"
        return try {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            )
            var date: java.util.Date? = null
            for (format in formats) {
                try {
                    date = format.parse(dateString)
                    if (date != null) break
                } catch (e: Exception) { continue }
            }
            if (date != null) {
                SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(date)
            } else dateString
        } catch (e: Exception) { dateString }
    }

    val budgetText = when {
        campaign.budgetMin != null && campaign.budgetMax != null -> "₹${campaign.budgetMin} - ₹${campaign.budgetMax}"
        campaign.budgetMin != null -> "₹${campaign.budgetMin}"
        campaign.budgetMax != null -> "₹${campaign.budgetMax}"
        else -> "N/A"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(
                icon = Icons.Default.CurrencyRupee,
                label = "Budget",
                value = budgetText
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = softGray)
            InfoRow(
                icon = Icons.Default.CalendarMonth,
                label = "Timeline",
                value = "${formatCampaignDate(campaign.startDate)} - ${formatCampaignDate(campaign.endDate)}"
            )
        }
    }
}

@Composable
fun CampaignRequirementsSection(campaign: CampaignDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val categoryText = campaign.categories?.joinToString(", ") { "${it.category} (${it.subCategory})" }
                ?: campaign.brand?.brandCategory?.let { "${it.category} (${it.subCategory})" }
                ?: "N/A"
                
            RequirementRow(
                icon = Icons.Default.Category,
                label = "Category",
                value = categoryText
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = softGray)
            RequirementRow(
                icon = Icons.Default.People,
                label = "Age Range",
                value = if (campaign.targetAudience?.ageMin != null && campaign.targetAudience?.ageMax != null) 
                    "${campaign.targetAudience.ageMin} - ${campaign.targetAudience.ageMax}" 
                    else "Any"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = softGray)
            RequirementRow(
                icon = Icons.Default.Wc,
                label = "Gender Target",
                value = campaign.targetAudience?.gender ?: "Any"
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = softGray)
            RequirementRow(
                icon = Icons.Default.Language,
                label = "Platform",
                value = campaign.platforms?.joinToString(", ") { it.platform } ?: "N/A"
            )
        }
    }
}

@Composable
fun BrandInfoSection(brand: Brand?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Brand Bio",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = brand?.about ?: "No bio available.",
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = darkerGray,
                fontSize = 13.sp
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = softGray)
            
            // Brand Category
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Category, contentDescription = null, tint = darkerGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = brand?.brandCategory?.let { "${it.category} - ${it.subCategory}" } ?: "Not specified",
                    fontSize = 13.sp,
                    color = darkerGray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Brand Target Audience
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = null, tint = darkerGray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val ageRange = if (brand?.targetAudience?.ageMin != null && brand.targetAudience.ageMax != null) {
                    "${brand.targetAudience.ageMin} - ${brand.targetAudience.ageMax}"
                } else "Any"
                Text(
                    text = "Targets: $ageRange, ${brand?.targetAudience?.gender ?: "Any"}",
                    fontSize = 13.sp,
                    color = darkerGray
                )
            }

            if (brand?.profileUrl != null && brand.profileUrl!!.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = darkerGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = brand.profileUrl!!, fontSize = 13.sp, color = darkerGray)
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = themeColor.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.padding(8.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, fontSize = 12.sp, color = darkerGray)
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun RequirementRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = darkerGray.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 14.sp, color = darkerGray, modifier = Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor
        )
    }
}

@Composable
fun CollaborateButton(campaign: CampaignDetail, navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp),
        color = Color.White
    ) {
        Button(
            onClick = {
                navController.navigate("influencer_apply_campaign/${campaign.id}")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Apply Now",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
