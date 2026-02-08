package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel

private val influencerDetailThemeColor = Color(0xFFFF8383)
private val detailSoftGray = Color(0xFFF8F9FA)
private val detailDarkerGray = Color(0xFF6C757D)
private val platformsColors = mapOf(
    "INSTAGRAM" to Color(0xFFF8CA43),
    "YOUTUBE" to Color(0xFFFA4A4A),
    "X" to Color(0xFF000000),
    "TWITTER" to Color(0xFF1DA1F2)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrandInfluencerDetailScreen(
    influencerId: String,
    onBack: () -> Unit,
    influencerViewModel: InfluencerViewModel
) {
    val influencer by influencerViewModel.influencerProfile.observeAsState()
    val isLoading by influencerViewModel.loading.observeAsState(false)
    val error by influencerViewModel.error.observeAsState()

    LaunchedEffect(influencerId) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            if (token != null) {
                influencerViewModel.fetchInfluencerById(influencerId, token)
            }
        }
    }

    BrandInfluencerDetailContent(
        influencer = influencer,
        isLoading = isLoading,
        error = error,
        onBack = onBack
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandInfluencerDetailContent(
    influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(detailSoftGray)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = influencerDetailThemeColor)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = Color.Red)
            }
        } else if (influencer != null) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    // Header Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(influencerDetailThemeColor)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.vector),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.2f),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(top = 12.dp, start = 8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(100.dp))
                        // Profile Header Card
                        NewEnhancedProfileHeader(influencer)
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
//                    Spacer(modifier = Modifier.height(32.dp))
//
//                    // Metrics Section
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceEvenly
//                    ) {
//                        MetricItem(Icons.Default.Person, "Followers")
//                        MetricItem(Icons.Default.ShowChart, "Engagements")
//                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // About Section
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("About", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = influencer.bio ?: "No bio available.",
                            fontSize = 15.sp,
                            color = detailDarkerGray,
                            lineHeight = 24.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Analytics Row: Viewers & Top Locations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        influencer.platforms?.let { platforms ->
                            ViewersDonutCard(
                                platforms = platforms,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        influencer.audienceInsights?.let { insights ->
                            InfluencerTopLocationsCard(
                                insights = insights,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Platforms Progress Bars
                    PlatformsCard(influencer)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Priorities (Strengths)
                    if (!influencer.strengths.isNullOrEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Priorities", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                influencer.strengths!!.forEach { strength ->
                                    PriorityTag(strength)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // Gender Split
                    influencer.audienceInsights?.let { insights ->
                        InfluencerGenderSplitCard(insights)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Payments (Pricing)
                    PricingTable(influencer)

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewBrandInfluencerDetailScreen() {
    val mockInfluencer = np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile(
        id = "5a67bHtWTjehwjEoRtN6FZJOkLj2",
        email = "rohit@social.com",
        name = "Rohit Fitness",
        role = "INFLUENCER",
        profileCompleted = true,
        updatedAt = "2026-02-08T19:48:46.479Z",
        bio = "Fitness enthusiast and marathon runner. Helping you stay fit!",
        location = "Mumbai, India",
        categories = listOf(
            np.com.bimalkafle.firebaseauthdemoapp.model.Category("Health", "Fitness"),
            np.com.bimalkafle.firebaseauthdemoapp.model.Category("Technology", "Gadgets"),
            np.com.bimalkafle.firebaseauthdemoapp.model.Category("Lifestyle", "Travel")
        ),
        platforms = listOf(
            np.com.bimalkafle.firebaseauthdemoapp.model.Platform(
                platform = "INSTAGRAM",
                profileUrl = "https://ig.com/rohit_fitness",
                followers = 55000,
                avgViews = 12000,
                engagement = 4.2f,
                formats = listOf("Reel", "Story", "Post"),
                connected = true,
                minFollowers = 1000,
                minEngagement = 2.0f
            ),
            np.com.bimalkafle.firebaseauthdemoapp.model.Platform(
                platform = "YOUTUBE",
                profileUrl = "https://youtube.com/c/rohit_vlogs",
                followers = 120000,
                avgViews = 45000,
                engagement = 8.5f,
                formats = listOf("Video", "Shorts", "Community Post"),
                connected = true,
                minFollowers = 5000,
                minEngagement = 3.0f
            )
        ),
        audienceInsights = np.com.bimalkafle.firebaseauthdemoapp.model.AudienceInsights(
            topLocations = listOf(
                np.com.bimalkafle.firebaseauthdemoapp.model.LocationInsight("Mumbai", "India", 40.5f),
                np.com.bimalkafle.firebaseauthdemoapp.model.LocationInsight("Delhi", "India", 30.2f),
                np.com.bimalkafle.firebaseauthdemoapp.model.LocationInsight("Bangalore", "India", 20.3f)
            ),
            genderSplit = np.com.bimalkafle.firebaseauthdemoapp.model.GenderSplit(male = 45f, female = 55f),
            ageGroups = listOf(
                np.com.bimalkafle.firebaseauthdemoapp.model.AgeGroupInsight("18-24", 50f),
                np.com.bimalkafle.firebaseauthdemoapp.model.AgeGroupInsight("25-34", 35f),
                np.com.bimalkafle.firebaseauthdemoapp.model.AgeGroupInsight("35-44", 15f)
            )
        ),
        strengths = listOf("Authenticity", "High Engagement"),
        pricing = listOf(
            np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo("INSTAGRAM", "Reel", 15000, "INR"),
            np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo("INSTAGRAM", "Story", 5000, "INR"),
            np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo("INSTAGRAM", "Post", 8000, "INR"),
            np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo("YOUTUBE", "Video", 50000, "INR"),
            np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo("YOUTUBE", "Shorts", 12000, "INR"),
            np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo("YOUTUBE", "Sponsorship", 75000, "INR")
        ),
        availability = true,
        logoUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop"
    )

    BrandInfluencerDetailContent(
        influencer = mockInfluencer,
        isLoading = false,
        error = null,
        onBack = {}
    )
}

@Composable
private fun NewEnhancedProfileHeader(
    influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {

        // Main Card
        Card(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 210.dp, bottom = 24.dp), // space for image
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // Name & Verified Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = influencer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Dealing with ${
                        influencer.categories?.firstOrNull()?.category ?: "Content"
                    }",
                    color = detailDarkerGray,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Stats and Follow Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Followers",
                            tint = detailDarkerGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            formatInfluencerCount(
                                influencer.platforms?.sumOf { it.followers ?: 0 } ?: 0
                            ),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = "Engagement",
                            tint = detailDarkerGray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "48",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = detailSoftGray
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(
                            horizontal = 24.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            "Collaborate",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Floating Profile Image (POP-OUT EFFECT)
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(y = (-40).dp) // pull image upward
                .clip(RoundedCornerShape(32.dp))
                .background(detailSoftGray)
                .shadow(12.dp, RoundedCornerShape(32.dp)),
        ) {
            if (!influencer.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = influencer.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = influencer.name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = influencerDetailThemeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = detailDarkerGray, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = detailDarkerGray, fontSize = 14.sp)
    }
}

@Composable
private fun ViewersDonutCard(
    platforms: List<np.com.bimalkafle.firebaseauthdemoapp.model.Platform>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Viewers", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            val totalViews = platforms.sumOf { it.followers ?: 0 }.toFloat()
            val values = platforms.map { (it.followers ?: 0).toFloat() }
            val colors = platforms.map { platformsColors[it.platform.uppercase()] ?: Color.Gray }

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                InfluencerDonutChart(
                    values = values,
                    colors = colors,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp
                )
                Text(
                    text = formatInfluencerCount(totalViews.toInt()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                platforms.take(3).forEach { platform ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(platformsColors[platform.platform.uppercase()] ?: Color.Gray, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(platform.platform, fontSize = 10.sp, color = detailDarkerGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformsCard(influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Platforms", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        influencer.platforms?.forEach { platform ->
            PlatformProgressItem(platform)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PlatformProgressItem(
    platform: np.com.bimalkafle.firebaseauthdemoapp.model.Platform
) {
    val primaryColor = platformsColors[platform.platform.uppercase()] ?: Color.Gray
    val bgColor = primaryColor.copy(alpha = 0.08f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = platform.platform.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primaryColor
                )

                Text(
                    text = formatInfluencerCount(platform.followers ?: 0) + " Followers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = "Avg Views",
                        fontSize = 12.sp,
                        color = detailDarkerGray
                    )
                    Text(
                        text = formatInfluencerCount(platform.avgViews ?: 0),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Engagement",
                        fontSize = 12.sp,
                        color = detailDarkerGray
                    )
                    Text(
                        text = "${platform.engagement ?: 0f}%",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityTag(label: String) {
    Surface(
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_input_add), // Placeholder for "+" icon
                contentDescription = null,
                tint = Color(0xFFF57C00),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color(0xFFE65100), fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
    }
}

@Composable
private fun InfluencerTopLocationsCard(
    insights: np.com.bimalkafle.firebaseauthdemoapp.model.AudienceInsights,
    modifier: Modifier = Modifier
) {
    val locations = insights.topLocations?.take(4) ?: emptyList()
    val values = locations.map { it.percentage }
    val colors = listOf(
        Color(0xFF1E63E9),   // Blue
        Color(0xFFF7943D),   // Orange
        Color(0xFF3CC18E),   // Green
        Color(0xFFF4B73B)    // Yellow
    ).take(values.size)

    val total = values.sum()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Locations",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Donut Chart
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                InfluencerDonutChart(
                    values = values,
                    colors = colors,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp
                )

                Text(
                    text = formatInfluencerCount(total.toInt()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                locations.take(3).forEachIndexed { index, loc ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(colors[index], CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = loc.city ?: loc.country ?: "Unknown",
                                fontSize = 10.sp,
                                color = detailDarkerGray,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Display percentage value
                        Text(
                            text = "${String.format("%.0f", loc.percentage)}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfluencerGenderSplitCard(insights: np.com.bimalkafle.firebaseauthdemoapp.model.AudienceInsights) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Gender", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(24.dp))
            
            val male = insights.genderSplit?.male ?: 0f
            val female = insights.genderSplit?.female ?: 0f
            
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                GenderBar(male, Color(0xFF64B5F6), "Male")
                GenderBar(female, Color(0xFF81C784), "Female")
            }
        }
    }
}

@Composable
private fun GenderBar(percentage: Float, color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(percentage / 100f)
                .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = detailDarkerGray)
    }
}

@Composable
private fun PricingTable(influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Payments & Deliverables",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                influencer.pricing?.forEachIndexed { index, pricing ->
                    val platformColor = platformsColors[pricing.platform.uppercase()] ?: influencerDetailThemeColor
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Icon with soft background
                            Surface(
                                color = platformColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = getDeliverableIcon(pricing.deliverable),
                                        contentDescription = null,
                                        tint = platformColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column {
                                Text(
                                    text = pricing.deliverable,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = pricing.platform,
                                    fontSize = 12.sp,
                                    color = detailDarkerGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "₹${formatInfluencerCount(pricing.price)}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "Negotiable",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (index < (influencer.pricing?.size ?: 0) - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = detailSoftGray,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

private fun getDeliverableIcon(deliverable: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (deliverable.lowercase()) {
        "reel" -> Icons.Default.Movie
        "story" -> Icons.Default.History
        "post" -> Icons.Default.Image
        "video" -> Icons.Default.PlayCircle
        "shorts" -> Icons.Default.MovieFilter
        "sponsorship" -> Icons.Default.Star
        else -> Icons.Default.AutoAwesome
    }
}

@Composable
private fun InfluencerDonutChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 20.dp
) {
    val total = values.sum()
    if (total == 0f) return
    
    var startAngle = -90f

    Canvas(modifier = modifier) {
        values.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

fun formatInfluencerCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fK", count / 1_000f)
        else -> count.toString()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}
