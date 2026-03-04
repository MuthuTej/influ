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
import androidx.compose.ui.text.style.TextOverflow
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
    onCreateProposal: (String) -> Unit,
    onConnect: (String, String) -> Unit,
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
        onBack = onBack,
        onCreateProposal = { onCreateProposal(influencerId) },
        onConnect = { id, name -> onConnect(id, name) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandInfluencerDetailContent(
    influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onCreateProposal: () -> Unit,
    onConnect: (String, String) -> Unit
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
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
                        NewEnhancedProfileHeader(influencer, onCreateProposal, onConnect)
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

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

                    influencer.youtubeInsights?.let { ytInsights ->
                        YouTubeInsightsSection(ytInsights)
                        Spacer(modifier = Modifier.height(32.dp))
                    }

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

                    PlatformsCard(influencer)

                    Spacer(modifier = Modifier.height(32.dp))

                    if (!influencer.strengths.isNullOrEmpty()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Priorities", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                influencer.strengths!!.forEach { strength ->
                                    PriorityTag(strength)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    influencer.audienceInsights?.let { insights ->
                        InfluencerGenderSplitCard(insights)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    PricingTable(influencer)

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun NewEnhancedProfileHeader(
    influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile,
    onCreateProposal: () -> Unit,
    onConnect: (String, String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
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
                    .padding(top = 210.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = influencer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (influencer.isVerified == true) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "Dealing with ${influencer.categories?.firstOrNull()?.category ?: "Content"}",
                    color = detailDarkerGray,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = detailDarkerGray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatInfluencerCount(influencer.platforms?.sumOf { it.followers ?: 0 } ?: 0), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, tint = detailDarkerGray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("48", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onConnect(influencer.id, influencer.name) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Connect", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onCreateProposal,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = influencerDetailThemeColor),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Proposal", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(y = (-40).dp)
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Text(text = platform.platform.uppercase(), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryColor)
                Text(text = formatInfluencerCount(platform.followers ?: 0) + " Followers", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Avg Views", fontSize = 12.sp, color = detailDarkerGray)
                    Text(text = formatInfluencerCount(platform.avgViews ?: 0), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Engagement", fontSize = 12.sp, color = detailDarkerGray)
                    Text(text = "${platform.engagement ?: 0f}%", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                imageVector = Icons.Default.Add,
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
    val colors = listOf(Color(0xFF1E63E9), Color(0xFFF7943D), Color(0xFF3CC18E), Color(0xFFF4B73B)).take(values.size)
    val total = values.sum()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Locations", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                InfluencerDonutChart(values = values, colors = colors, modifier = Modifier.fillMaxSize(), strokeWidth = 12.dp)
                Text(text = formatInfluencerCount(total.toInt()), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                locations.take(3).forEachIndexed { index, loc ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(colors[index], CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = loc.city ?: loc.country ?: "Unknown", fontSize = 10.sp, color = detailDarkerGray, fontWeight = FontWeight.Medium)
                        }
                        Text(text = "${String.format("%.0f", loc.percentage)}%", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
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
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Gender", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                GenderBar(insights.genderSplit?.male ?: 0f, Color(0xFF64B5F6), "Male")
                GenderBar(insights.genderSplit?.female ?: 0f, Color(0xFF81C784), "Female")
            }
        }
    }
}

@Composable
private fun GenderBar(percentage: Float, color: Color, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.width(80.dp).fillMaxHeight(percentage / 100f).background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = detailDarkerGray)
    }
}

@Composable
private fun PricingTable(influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Payments & Deliverables", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                influencer.pricing?.forEachIndexed { index, pricing ->
                    val platformColor = platformsColors[pricing.platform.uppercase()] ?: influencerDetailThemeColor
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(color = platformColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(imageVector = getDeliverableIcon(pricing.deliverable), contentDescription = null, tint = platformColor, modifier = Modifier.size(22.dp)) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = pricing.deliverable, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                Text(text = pricing.platform, fontSize = 12.sp, color = detailDarkerGray, fontWeight = FontWeight.Medium)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "₹${formatInfluencerCount(pricing.price)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black)
                            Text(text = "Negotiable", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (index < (influencer.pricing?.size ?: 0) - 1) { HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp), color = detailSoftGray, thickness = 1.dp) }
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
            drawArc(color = colors[index], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round))
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
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

@Composable
private fun YouTubeInsightsSection(insights: np.com.bimalkafle.firebaseauthdemoapp.model.YouTubeInsights) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("YouTube Insights", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Channel: ${insights.title ?: "N/A"}", color = detailDarkerGray, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            YouTubeStatCard(label = "Subscribers", value = formatInfluencerCount(insights.subscribers ?: 0), icon = Icons.Default.People, modifier = Modifier.weight(1f))
            YouTubeStatCard(label = "Total Views", value = formatInfluencerCount(insights.totalViews?.toInt() ?: 0), icon = Icons.Default.Visibility, modifier = Modifier.weight(1f))
            YouTubeStatCard(label = "Videos", value = (insights.totalVideos ?: 0).toString(), icon = Icons.Default.VideoLibrary, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (!insights.demographics.isNullOrEmpty()) { YouTubeDemographicsCard(insights.demographics!!) }
        if (!insights.lastSynced.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Last Synced: ${insights.lastSynced}", color = detailDarkerGray, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun YouTubeStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = platformsColors["YOUTUBE"] ?: Color.Red, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, fontSize = 11.sp, color = detailDarkerGray)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YouTubeDemographicsCard(demographics: List<np.com.bimalkafle.firebaseauthdemoapp.model.YoutubeDemographics>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("YouTube Audience Demographics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Age Groups", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val ageData = demographics.groupBy { d -> d.ageGroup ?: "Other" }.mapValues { entry -> entry.value.sumOf { (it.percentage ?: 0f).toDouble() }.toFloat() }
                    val values = ageData.values.toList()
                    val labels = ageData.keys.toList()
                    val colors = listOf(Color(0xFF6C63FF), Color(0xFFFF8383), Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFF2196F3), Color(0xFF9C27B0)).take(values.size)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        InfluencerDonutChart(values, colors, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp)
                        Text("${values.sum().toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEachIndexed { index, label ->
                            Row(modifier = Modifier.fillMaxWidth(0.45f).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(colors[index], CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label.removePrefix("age"), fontSize = 10.sp, color = detailDarkerGray)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gender", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val genderData = demographics.groupBy { d -> d.gender ?: "Other" }.mapValues { entry -> entry.value.sumOf { (it.percentage ?: 0f).toDouble() }.toFloat() }
                    val values = genderData.values.toList()
                    val labels = genderData.keys.toList()
                    val colors = listOf(Color(0xFF64B5F6), Color(0xFFF06292), Color(0xFF9E9E9E)).take(values.size)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        InfluencerDonutChart(values, colors, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp)
                        Text("${values.sum().toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        labels.forEachIndexed { index, label ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(colors[index], CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, color = detailDarkerGray)
                            }
                        }
                    }
                }
            }
        }
    }
}
