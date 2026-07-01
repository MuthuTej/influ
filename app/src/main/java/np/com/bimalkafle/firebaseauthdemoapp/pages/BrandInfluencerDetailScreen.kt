package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.components.ErrorState
import np.com.bimalkafle.firebaseauthdemoapp.components.LoadingState
import np.com.bimalkafle.firebaseauthdemoapp.model.InstagramProfile
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel

// ── Design tokens ─────────────────────────────────────────────────────────────
private val influencerDetailThemeColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary

private val PageBg       = Color(0xFFF5F5F7)
private val CardBg       = Color.White
private val SubLabel     = Color(0xFF8E8E93)
private val DividerColor = Color(0xFFEEEEEE)

private val platformsColors = mapOf(
    "INSTAGRAM" to Color(0xFFE1306C),
    "YOUTUBE"   to Color(0xFFFF0000),
    "X"         to Color(0xFF000000),
    "TWITTER"   to Color(0xFF1DA1F2)
)

// ── Entry point ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrandInfluencerDetailScreen(
    influencerId: String,
    onBack: () -> Unit,
    onCreateProposal: (String) -> Unit,
    onConnect: (String, String) -> Unit,
    influencerViewModel: InfluencerViewModel
) {
    val influencer by influencerViewModel.influencerProfile.observeAsState(initial = null)
    val isLoading  by influencerViewModel.loading.observeAsState(initial = false)
    val error      by influencerViewModel.error.observeAsState(initial = null)

    LaunchedEffect(influencerId) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { r ->
            r.token?.let { influencerViewModel.fetchInfluencerById(influencerId, it) }
        }
    }

    BrandInfluencerDetailContent(
        influencer       = influencer,
        isLoading        = isLoading,
        error            = error,
        onBack           = onBack,
        onCreateProposal = { onCreateProposal(influencerId) },
        onConnect        = onConnect
    )
}

// ── Main content ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BrandInfluencerDetailContent(
    influencer: InfluencerProfile?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onCreateProposal: () -> Unit,
    onConnect: (String, String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(PageBg)) {
        when {
            isLoading        -> Box(Modifier.fillMaxSize(), Alignment.Center) { LoadingState(message = "Loading profile…") }
            error != null    -> Box(Modifier.fillMaxSize(), Alignment.Center) { ErrorState(message = error) }
            influencer != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    ProfileHero(influencer, onBack, onCreateProposal, onConnect)

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))

                        if (!influencer.bio.isNullOrBlank()) {
                            AboutSection(influencer.bio!!)
                        }

                        influencer.aiInsights?.let { AiProfileSummarySection(it) }

                        influencer.youtubeInsights?.let { YouTubeInsightsSection(it) }

                        val igProfiles = influencer.instagramProfiles
                        if (!igProfiles.isNullOrEmpty()) {
                            BrandInstagramProfilesSection(igProfiles)
                        } else {
                            influencer.instagramMetrics?.let { InstagramInsightsSection(it) }
                        }

                        if (!influencer.strengths.isNullOrEmpty()) {
                            PrioritiesSection(influencer.strengths!!)
                        }

                        if (!influencer.pricing.isNullOrEmpty()) {
                            PricingTable(influencer)
                        }
                    }
                }
            }
        }
    }
}

// ── Profile hero ──────────────────────────────────────────────────────────────
@Composable
private fun ProfileHero(
    influencer: InfluencerProfile,
    onBack: () -> Unit,
    onCreateProposal: () -> Unit,
    onConnect: (String, String) -> Unit
) {
    val themeColor = influencerDetailThemeColor

    Box(modifier = Modifier.fillMaxWidth()) {

        // Thin pink strip — only for the back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(themeColor)
        ) {
            Image(
                painter      = painterResource(R.drawable.vector),
                contentDescription = null,
                modifier     = Modifier.fillMaxSize().alpha(0.13f),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick  = onBack,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
        }

        // Horizontal card overlapping the banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 64.dp)
                .padding(horizontal = 16.dp),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

                // ── Left: large influencer image ───────────────────────────
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                        .background(themeColor.copy(alpha = 0.10f))
                ) {
                    if (!influencer.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model        = influencer.logoUrl,
                            contentDescription = null,
                            modifier     = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text(
                                text       = influencer.name.firstOrNull()?.uppercase() ?: "?",
                                fontSize   = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = themeColor
                            )
                        }
                    }
                }

                // ── Right: identity + stats + buttons ─────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    // • CATEGORY dot badge
                    val category = influencer.categories?.firstOrNull()?.category
                    if (category != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(themeColor, CircleShape)
                            )
                            Text(
                                text       = category.uppercase(),
                                fontSize   = 10.sp,
                                color      = themeColor,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                    }

                    // Name + verified
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = influencer.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 18.sp,
                            color      = Color.Black,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis,
                            lineHeight = 22.sp,
                            modifier   = Modifier.weight(1f, fill = false)
                        )
                        if (influencer.isVerified == true) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, "Verified", tint = Color(0xFF1976D2), modifier = Modifier.size(16.dp))
                        }
                    }

                    // @handle from primary Instagram profile
                    val handle = influencer.instagramProfiles?.firstOrNull()?.username
                    if (!handle.isNullOrBlank()) {
                        Text(
                            text     = "@$handle",
                            fontSize = 12.sp,
                            color    = SubLabel
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Stats — 2×2 grid
                    val totalFollowers = influencer.platforms?.sumOf { it.followers ?: 0 } ?: 0
                    val avgViews = influencer.instagramMetrics?.avgViews?.toLong()
                        ?: influencer.platforms?.firstOrNull()?.avgViews?.toLong()
                    val collabCount = influencer.collaborationCount

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HeroStatItem(formatInfluencerCount(totalFollowers), "Followers", Modifier.weight(1f))
                            Box(Modifier.width(1.dp).height(24.dp).background(DividerColor))
                            HeroStatItem("48", "Engagement", Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            HeroStatItem(avgViews?.let { formatInfluencerCount(it.toInt()) } ?: "—", "Avg Views", Modifier.weight(1f))
                            Box(Modifier.width(1.dp).height(24.dp).background(DividerColor))
                            HeroStatItem(collabCount?.toString() ?: "—", "Collabs", Modifier.weight(1f))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Buttons — Proposal (filled) left, Connect (outlined) right
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick  = onCreateProposal,
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = themeColor),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Proposal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                        OutlinedButton(
                            onClick  = { onConnect(influencer.id, influencer.name) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape    = RoundedCornerShape(10.dp),
                            border   = BorderStroke(1.5.dp, Color(0xFF6C63FF)),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6C63FF)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Connect", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Black, maxLines = 1)
        Text(label, fontSize = 10.sp, color = SubLabel)
    }
}

// ── About ─────────────────────────────────────────────────────────────────────
@Composable
private fun AboutSection(bio: String) {
    SectionCard {
        SectionHeader(Icons.Default.Info, "About")
        Spacer(Modifier.height(10.dp))
        Text(bio, fontSize = 14.sp, color = Color(0xFF37474F), lineHeight = 22.sp)
    }
}

// ── AI Summary + Insights ─────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiProfileSummarySection(ai: np.com.bimalkafle.firebaseauthdemoapp.model.AiInsights) {
    val purpleAccent = Color(0xFF7B1FA2)
    val softPurple   = Color(0xFFF3E5F5)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ── Summary with Read More ──────────────────────────────────────────
        val summary = ai.professionalSummary ?: ai.aiSummary
        if (summary != null) {
            var expanded by remember { mutableStateOf(false) }
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = softPurple),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = purpleAccent, modifier = Modifier.size(17.dp))
                        Text("AI Profile Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = purpleAccent)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text     = summary,
                        fontSize = 14.sp,
                        color    = Color(0xFF37474F),
                        lineHeight = 22.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 3,
                        overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis,
                        modifier = Modifier.animateContentSize(animationSpec = tween(250))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text       = if (expanded) "Show less" else "Read more",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = purpleAccent,
                        modifier   = Modifier.clickable { expanded = !expanded }
                    )
                }
            }
        }

        // ── Primary Niche + Tone ──────────────────────────────────────────
        if (ai.primaryNiche != null || ai.tone != null) {
            SectionHeader(Icons.Default.AutoAwesome, "AI Insights")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ai.primaryNiche?.let { niche ->
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Star, null, tint = purpleAccent, modifier = Modifier.size(18.dp))
                            Text("Primary Niche", fontSize = 11.sp, color = SubLabel, fontWeight = FontWeight.Medium)
                            Text(niche, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
                ai.tone?.let { tone ->
                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.MusicNote, null, tint = purpleAccent, modifier = Modifier.size(18.dp))
                            Text("Tone", fontSize = 11.sp, color = SubLabel, fontWeight = FontWeight.Medium)
                            Text(tone, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }

        // ── Audience Interests + Content Style ────────────────────────────
        if (!ai.audienceInterests.isNullOrEmpty() || ai.contentStyle != null) {
            SectionCard {
                if (!ai.audienceInterests.isNullOrEmpty()) {
                    Text("Audience Interests", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SubLabel)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ai.audienceInterests.forEach { interest ->
                            Surface(shape = RoundedCornerShape(20.dp), color = softPurple) {
                                Text(
                                    text     = interest,
                                    fontSize = 13.sp,
                                    color    = purpleAccent,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                ai.contentStyle?.let { style ->
                    if (!ai.audienceInterests.isNullOrEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(Modifier.height(12.dp))
                    }
                    Text("Content Style", fontSize = 13.sp, color = SubLabel, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(style, fontSize = 14.sp, color = Color.Black, lineHeight = 22.sp)
                }
            }
        }

        // ── Strengths ──────────────────────────────────────────────────────
        if (!ai.strengths.isNullOrEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFF388E3C), modifier = Modifier.size(17.dp))
                        Text("Strengths", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ai.strengths.forEach { strength ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                Text(strength, fontSize = 14.sp, color = Color(0xFF1B5E20), lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── Brand Suitability ──────────────────────────────────────────────
        ai.brandSuitability?.let { suitability ->
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF3949AB).copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Icon(Icons.Default.Handshake, null, tint = Color(0xFF3949AB), modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text("Brand Suitability", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF3949AB))
                        Spacer(Modifier.height(4.dp))
                        Text(suitability, fontSize = 14.sp, color = Color(0xFF1A237E), lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}

// ── YouTube Insights ──────────────────────────────────────────────────────────
@Composable
private fun YouTubeInsightsSection(insights: np.com.bimalkafle.firebaseauthdemoapp.model.YouTubeInsights) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            SectionHeader(icon = null, title = "YouTube Insights", logoPainter = R.drawable.youtube_logo)
            Text("Channel: ${insights.title ?: "N/A"}", color = SubLabel, fontSize = 13.sp, modifier = Modifier.padding(start = 2.dp, top = 2.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlatformStatCard("Subscribers", formatInfluencerCount(insights.subscribers ?: 0),       Modifier.weight(1f))
            PlatformStatCard("Total Views",  formatInfluencerCount(insights.totalViews?.toInt() ?: 0), Modifier.weight(1f))
            PlatformStatCard("Videos",        (insights.totalVideos ?: 0).toString(),                   Modifier.weight(1f))
        }
        if (!insights.demographics.isNullOrEmpty()) {
            YouTubeDemographicsCard(insights.demographics!!)
        }
        if (!insights.lastSynced.isNullOrEmpty()) {
            Text("Last Synced: ${insights.lastSynced}", color = SubLabel, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PlatformStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = SubLabel, textAlign = TextAlign.Center, lineHeight = 14.sp)
        }
    }
}

// ── Instagram Profiles ────────────────────────────────────────────────────────
@Composable
private fun BrandInstagramProfilesSection(profiles: List<InstagramProfile>) {
    val instaColor = Color(0xFFE1306C)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            SectionHeader(icon = null, title = "Instagram Profiles", logoPainter = R.drawable.instagram_logo)
            Text(
                "${profiles.size} connected profile${if (profiles.size > 1) "s" else ""}",
                color = SubLabel, fontSize = 13.sp
            )
        }
        profiles.forEach { profile ->
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(if (profile.isDefault) 3.dp else 1.dp),
                border    = if (profile.isDefault) BorderStroke(1.5.dp, instaColor) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = instaColor.copy(alpha = 0.10f), modifier = Modifier.size(40.dp)) {
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Icon(Icons.Default.PhotoCamera, null, tint = instaColor, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("@${profile.username}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (profile.isDefault) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = instaColor) {
                                        Text("PRIMARY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            if (profile.followers != null) {
                                Text("${formatInfluencerCount(profile.followers)} followers", color = SubLabel, fontSize = 12.sp)
                            }
                        }
                    }
                    profile.metrics?.let { m ->
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MetricItem(Icons.Default.Favorite,   formatInfluencerCount(m.avgLikes?.toInt()    ?: 0), "Avg Likes",    Color(0xFFE91E63))
                            Box(Modifier.width(1.dp).height(36.dp).background(DividerColor))
                            MetricItem(Icons.Default.Comment,    formatInfluencerCount(m.avgComments?.toInt() ?: 0), "Avg Comments", Color(0xFF2196F3))
                            Box(Modifier.width(1.dp).height(36.dp).background(DividerColor))
                            MetricItem(Icons.Default.Visibility, formatInfluencerCount(m.avgViews?.toInt()    ?: 0), "Avg Views",    Color(0xFF9C27B0))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, fontSize = 10.sp, color = SubLabel)
    }
}

// ── Instagram Insights (fallback) ─────────────────────────────────────────────
@Composable
private fun InstagramInsightsSection(metrics: np.com.bimalkafle.firebaseauthdemoapp.model.InstagramMetrics) {
    val instaColor = Color(0xFFE1306C)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            SectionHeader(icon = null, title = "Instagram Insights", logoPainter = R.drawable.instagram_logo)
            Text("Platform analytics", color = SubLabel, fontSize = 13.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PlatformStatCard("Avg Likes",    formatInfluencerCount(metrics.avgLikes?.toInt()    ?: 0), Modifier.weight(1f))
            PlatformStatCard("Avg Comments", formatInfluencerCount(metrics.avgComments?.toInt() ?: 0), Modifier.weight(1f))
            PlatformStatCard("Avg Views",    formatInfluencerCount(metrics.avgViews?.toInt()    ?: 0), Modifier.weight(1f))
        }
        SectionCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Posting Frequency", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Avg days between posts", color = SubLabel, fontSize = 12.sp)
                }
                Text("${metrics.postingFrequencyDays ?: "N/A"} days", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = instaColor)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = DividerColor)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Posts Analyzed", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Sample size for metrics", color = SubLabel, fontSize = 12.sp)
                }
                Text((metrics.totalPostsAnalyzed ?: 0).toString(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
        if (!metrics.updatedAt.isNullOrEmpty()) {
            Text("Last Updated: ${metrics.updatedAt}", color = SubLabel, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Priorities ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrioritiesSection(strengths: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(Icons.Default.FlashOn, "Priorities")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp)
        ) {
            strengths.forEach { strength ->
                Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FlashOn, null, tint = Color(0xFFF57C00), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(strength, color = Color(0xFFE65100), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Pricing ───────────────────────────────────────────────────────────────────
@Composable
private fun PricingTable(influencer: InfluencerProfile) {
    val themeColor = influencerDetailThemeColor
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(Icons.Default.CreditCard, "Payments & Deliverables")
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                influencer.pricing?.forEachIndexed { index, pricing ->
                    val platformType  = pricing.platform.uppercase()
                    val platformColor = platformsColors[platformType] ?: themeColor
                    Row(
                        modifier  = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(color = platformColor.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)) {
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    when (platformType) {
                                        "YOUTUBE"   -> Image(painterResource(R.drawable.youtube_logo),   null, modifier = Modifier.size(22.dp))
                                        "INSTAGRAM" -> Image(painterResource(R.drawable.instagram_logo), null, modifier = Modifier.size(22.dp))
                                        else        -> Icon(getDeliverableIcon(pricing.deliverable), null, tint = platformColor, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(pricing.deliverable, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(pricing.platform, fontSize = 12.sp, color = SubLabel, fontWeight = FontWeight.Medium)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${formatInfluencerCount(pricing.price)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("Negotiable", fontSize = 11.sp, color = Color(0xFF43A047), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (index < (influencer.pricing?.size ?: 0) - 1) {
                        HorizontalDivider(color = DividerColor, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

// ── YouTube Demographics ──────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YouTubeDemographicsCard(demographics: List<np.com.bimalkafle.firebaseauthdemoapp.model.YoutubeDemographics>) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Audience Demographics", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                // Age groups
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Age Groups", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SubLabel)
                    Spacer(Modifier.height(12.dp))
                    val ageData = demographics
                        .groupBy { it.ageGroup ?: "Other" }
                        .mapValues { e -> e.value.sumOf { (it.percentage ?: 0f).toDouble() }.toFloat() }
                    val values = ageData.values.toList()
                    val labels = ageData.keys.toList()
                    val colors = listOf(Color(0xFF6C63FF), MaterialTheme.colorScheme.primary, Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFF2196F3), Color(0xFF9C27B0)).take(values.size)
                    Box(Modifier.size(90.dp), Alignment.Center) {
                        InfluencerDonutChart(values, colors, modifier = Modifier.fillMaxSize(), strokeWidth = 9.dp)
                        Text("${values.sum().toInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        labels.forEachIndexed { i, label ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).background(colors[i], CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text(label.removePrefix("age"), fontSize = 10.sp, color = SubLabel)
                            }
                        }
                    }
                }
                // Gender
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gender", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SubLabel)
                    Spacer(Modifier.height(12.dp))
                    val genderData = demographics
                        .groupBy { it.gender ?: "Other" }
                        .mapValues { e -> e.value.sumOf { (it.percentage ?: 0f).toDouble() }.toFloat() }
                    val values = genderData.values.toList()
                    val labels = genderData.keys.toList()
                    val colors = listOf(Color(0xFF64B5F6), Color(0xFFF06292), Color(0xFF9E9E9E)).take(values.size)
                    Box(Modifier.size(90.dp), Alignment.Center) {
                        InfluencerDonutChart(values, colors, modifier = Modifier.fillMaxSize(), strokeWidth = 9.dp)
                        Text("${values.sum().toInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        labels.forEachIndexed { i, label ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).background(colors[i], CircleShape))
                                Spacer(Modifier.width(4.dp))
                                Text(label.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, color = SubLabel)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Shared components ─────────────────────────────────────────────────────────
@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    logoPainter: Int? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            logoPainter != null -> Image(painterResource(logoPainter), null, modifier = Modifier.size(20.dp))
            icon != null        -> Icon(icon, null, tint = influencerDetailThemeColor, modifier = Modifier.size(20.dp))
        }
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

// ── Donut chart ───────────────────────────────────────────────────────────────
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
                color      = colors[index],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter  = false,
                style      = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────
fun formatInfluencerCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
    count >= 1_000     -> String.format("%.1fK", count / 1_000f)
    else               -> count.toString()
}

private fun getDeliverableIcon(deliverable: String): androidx.compose.ui.graphics.vector.ImageVector =
    when (deliverable.lowercase()) {
        "reel"        -> Icons.Default.Movie
        "story"       -> Icons.Default.History
        "post"        -> Icons.Default.Image
        "video"       -> Icons.Default.PlayCircle
        "shorts"      -> Icons.Default.MovieFilter
        "sponsorship" -> Icons.Default.Star
        else          -> Icons.Default.AutoAwesome
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
        modifier              = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement   = verticalArrangement,
        content               = content
    )
}
