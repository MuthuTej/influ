package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.CollaborationAnalytics
import np.com.bimalkafle.firebaseauthdemoapp.model.OverallAnalytics
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel

// App theme colors
private val themeColor_campaign = Color(0xFFFF8383)
private val textGray = Color(0xFF8E8E93)

@Composable
fun CollaborationAnalyticsPage(
    navController: NavController,
    collaborationId: String,
    brandViewModel: BrandViewModel
) {
    val collaborations by brandViewModel.collaborations.observeAsState(emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val collaboration = collaborations.find { it.id == collaborationId }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            result.token?.let { token ->
                brandViewModel.fetchCollaborations(token)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FE))
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = themeColor_campaign)
        } else if (collaboration == null) {
            Text("Collaboration not found", modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AnalyticsHeader(navController, collaboration)
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        InfluencerProfileCard(collaboration)
                    }
                }

                if (collaboration.status == "PENDING") {
                    item { StatusPlaceholder("Proposal not accepted till now", Icons.Default.HourglassEmpty) }
                } else if (collaboration.status == "ACCEPTED") {
                    item { StatusPlaceholder("Advance payment is not completed", Icons.Default.Payment) }
                } else {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionTitle("OVERALL PERFORMANCE")
                        }
                    }

                    item {
                        val duration = collaboration.platformAnalytics?.firstOrNull()?.duration ?: 0
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            TotalImpressionsCard(
                                impressions = String.format("%,d", collaboration.overallAnalytics?.impressions ?: 0),
                                subtext = "Reached across $duration days"
                            )
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            OverallStatsGrid(collaboration)
                        }
                    }

                    item {
                        collaboration.overallAnalytics?.let { stats ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EngagementBreakdownCard(
                                    likes = stats.likes ?: 0,
                                    comments = stats.comments ?: 0,
                                    shares = stats.shares ?: 0,
                                    saves = stats.saves ?: 0,
                                    title = "Engagement Breakdown"
                                )
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionTitle("PLATFORM BREAKDOWN")
                        }
                    }

                    val platformAnalytics = collaboration.platformAnalytics ?: emptyList()
                    items(platformAnalytics) { analytics ->
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PlatformMetricCard(analytics, expandedDefault = platformAnalytics.size == 1)
                        }
                    }

                    item {
                        val durationValue = collaboration.platformAnalytics?.firstOrNull()?.duration ?: 0
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            CampaignDurationCard(durationValue.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsHeader(navController: NavController, collaboration: Collaboration?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(themeColor_campaign, themeColor_campaign.copy(alpha = 0.9f))
                )
            )
            .padding(bottom = 24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.25f),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "ANALYTICS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (collaboration != null) {
                Text(
                    collaboration.campaign.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp
                )
                
                Text(
                    "with ${collaboration.influencer.name}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InfluencerProfileCard(collaboration: Collaboration) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.LightGray.copy(alpha = 0.1f)
            ) {
                AsyncImage(
                    model = collaboration.influencer.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.brand_profile)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(collaboration.influencer.name, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Influencer · Instagram", color = textGray, fontSize = 12.sp)
            }
            if (collaboration.status == "COMPLETED") {
                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DONE", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPlaceholder(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = themeColor_campaign.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            title,
            color = themeColor_campaign.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun TotalImpressionsCard(impressions: String, subtext: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColor_campaign)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "TOTAL IMPRESSIONS",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        impressions,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Text(
                    subtext,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun OverallStatsGrid(collaboration: Collaboration) {
    val stats = collaboration.overallAnalytics ?: return
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(Icons.Default.PlayArrow, Color(0xFF4285F4), String.format("%,d", stats.views ?: 0), "Views", Color(0xFF4285F4), Modifier.weight(1f))
            StatCard(Icons.Default.AdsClick, Color(0xFF9E9E9E), String.format("%,d", stats.clicks ?: 0), "Clicks", Color(0xFF9E9E9E), Modifier.weight(1f))
        }
    }
}

@Composable
fun EngagementBreakdownCard(likes: Int, comments: Int, shares: Int, saves: Int, title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            val total = (likes + comments + shares + saves).toFloat()
            EngagementBarItem("Likes", likes, total, Color(0xFFFF5252), Icons.Default.Favorite)
            EngagementBarItem("Comments", comments, total, Color(0xFFFFD740), Icons.Default.ChatBubble)
            EngagementBarItem("Shares", shares, total, Color(0xFF448AFF), Icons.Default.IosShare)
            EngagementBarItem("Saves", saves, total, Color(0xFF1DE9B6), Icons.Default.Label)
        }
    }
}

@Composable
fun EngagementBarItem(label: String, value: Int, total: Float, color: Color, icon: ImageVector) {
    val progress = if (total > 0) value / total else 0f
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = textGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(String.format("%,d", value), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun StatCard(icon: ImageVector, iconColor: Color, value: String, label: String, topAccentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(95.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(topAccentColor))
            Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = RoundedCornerShape(8.dp), color = iconColor.copy(alpha = 0.1f), modifier = Modifier.size(28.dp)) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(6.dp))
                }
                Column {
                    Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text(label.uppercase(), fontSize = 9.sp, color = textGray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CampaignDurationCard(duration: String) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor_campaign)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("CAMPAIGN DURATION", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$duration days", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PlatformMetricCard(analytics: CollaborationAnalytics, expandedDefault: Boolean = false) {
    var expanded by remember { mutableStateOf(expandedDefault) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = themeColor_campaign.copy(alpha = 0.1f)) {
                    Icon(imageVector = getPlatformIcon(analytics.platform), contentDescription = null, tint = themeColor_campaign, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = analytics.platform?.uppercase() ?: "UNKNOWN", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                    Text(text = "${String.format("%,d", analytics.impressions ?: 0)} impressions", fontSize = 12.sp, color = textGray)
                }
                Surface(color = Color(0xFFFFF4F4), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFDEDE))) {
                    Text(text = "₹${analytics.cost ?: 0f}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB35A5A))
                }
                Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = textGray, modifier = Modifier.padding(start = 8.dp))
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PlatformStatsDetails(analytics)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    EngagementBreakdownCard(
                        likes = analytics.likes ?: 0,
                        comments = analytics.comments ?: 0,
                        shares = analytics.shares ?: 0,
                        saves = analytics.saves ?: 0,
                        title = "Platform Engagement"
                    )
                }
            }
        }
    }
}

@Composable
fun PlatformStatsDetails(analytics: CollaborationAnalytics) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStatCard("Views", String.format("%,d", analytics.views ?: 0), Icons.Default.PlayArrow, Color(0xFF4285F4), Modifier.weight(1f))
            MiniStatCard("Clicks", String.format("%,d", analytics.clicks ?: 0), Icons.Default.AdsClick, Color(0xFF9E9E9E), Modifier.weight(1f))
        }
    }
}

@Composable
fun MiniStatCard(label: String, value: String, icon: ImageVector, iconColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FE))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(28.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(6.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = textGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

fun getPlatformIcon(platform: String?): ImageVector {
    return when (platform?.lowercase()) {
        "instagram" -> Icons.Default.CameraAlt
        "youtube" -> Icons.Default.PlayCircle
        "facebook" -> Icons.Default.Facebook
        else -> Icons.Default.Language
    }
}
