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
import kotlin.math.abs

// ── Design tokens ─────────────────────────────────────────────────────────────
private val influencerDetailThemeColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary

private val PageBg       = Color(0xFFF5F0EE)   // warm off-white matching design
private val CardBg       = Color.White
private val HeroCardBg   = Color(0xFFFDEAE6)   // warm peach from design
private val SubLabel     = Color(0xFF9A8F8C)
private val DividerColor = Color(0x14000000)   // rgba(0,0,0,0.08)

private val platformsColors = mapOf(
    "INSTAGRAM" to Color(0xFFE1306C),
    "YOUTUBE"   to Color(0xFFFF0000),
    "X"         to Color(0xFF000000),
    "TWITTER"   to Color(0xFF1DA1F2)
)

// ─── Avatar colour palette (hash-based, stable per influencer name) ─────────
private val AvatarPalette = listOf(
    Color(0xFF5E4AE3),
    Color(0xFFE84393),
    Color(0xFF0EA5E9),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
)

private fun avatarColorFor(name: String) =
    AvatarPalette[abs(name.hashCode()) % AvatarPalette.size]

private fun initialsFor(name: String): String {
    val words = name.trim().split("\\s+".toRegex())
    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}

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
                    ProfileHero(influencer, onBack, onCreateProposal)

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Spacer(Modifier.height(4.dp))

                        if (!influencer.bio.isNullOrBlank() || !influencer.about.isNullOrBlank() || !influencer.creatorName.isNullOrBlank()) {
                            AboutSection(
                                bio = influencer.bio,
                                about = influencer.about,
                                creatorName = influencer.creatorName
                            )
                        }

                        influencer.aiInsights?.let { AiProfileSummarySection(it) }

                        influencer.youtubeInsights?.let { YouTubeInsightsSection(it) }

                        val igProfiles = influencer.instagramProfiles
                        if (!igProfiles.isNullOrEmpty()) {
                            BrandInstagramProfilesSection(igProfiles, influencer.followers)
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
    onCreateProposal: () -> Unit
) {
    val themeColor = influencerDetailThemeColor

    // Precompute all stats for the unified 4×2 grid
    val totalFollowers = influencer.totalFollowers
        ?: influencer.followers
        ?: influencer.platforms?.sumOf { it.followers ?: 0 }
        ?: 0
    val engRate      = influencer.engagementRate
    val avgViews     = influencer.instagramMetrics?.avgViews?.toLong()
        ?: influencer.platforms?.firstOrNull()?.avgViews?.toLong()
    val collabCount  = influencer.collaborationCount
    val avgLikes     = influencer.instagramMetrics?.avgLikes?.toInt()
    val avgComments  = influencer.instagramMetrics?.avgComments?.toInt()
    val postFreq     = influencer.instagramMetrics?.postingFrequencyDays
    val tier         = influencer.tier

    data class HeroStat(val value: String, val label: String, val color: Color)
    // Row 1: core reach   Row 2: engagement detail
    val statRows: List<List<HeroStat>> = listOf(
        listOf(
            HeroStat(formatInfluencerCount(totalFollowers), "Followers",   Color(0xFF1976D2)),
            HeroStat(engRate?.let { "${"%.1f".format(it)}%" } ?: "—",     "Engagement",    Color(0xFF388E3C)),
            HeroStat(avgViews?.let { formatInfluencerCount(it.toInt()) } ?: "—", "Avg Views", Color(0xFF7B1FA2)),
            HeroStat(collabCount?.toString() ?: "—",                       "Collabs",       Color(0xFFE65100))
        ),
        listOf(
            HeroStat(avgLikes?.let { formatInfluencerCount(it) } ?: "—",  "Avg Likes",     Color(0xFFE91E63)),
            HeroStat(avgComments?.let { formatInfluencerCount(it) } ?: "—", "Avg Comments", Color(0xFF0288D1)),
            HeroStat(postFreq?.let { "${"%.0f".format(it)}d" } ?: "—",    "Post Freq",     Color(0xFF00897B)),
            HeroStat(tier ?: "—",                                          "Tier",          Color(0xFFF57F17))
        )
    )

    val handle = influencer.instagramProfiles?.firstOrNull()?.username
    val initials = remember(influencer.name) { initialsFor(influencer.name) }
    val avatarBg = remember(influencer.name) { avatarColorFor(influencer.name) }

    Box(modifier = Modifier.fillMaxWidth()) {

        // Gradient header strip — back button lives here
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp).background(themeColor)
        ) {
            Image(
                painter      = painterResource(R.drawable.vector),
                contentDescription = null,
                modifier     = Modifier.fillMaxSize().alpha(0.13f),
                contentScale = ContentScale.Crop
            )
            IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
        }

        // Hero card — vertical layout matches design
        Card(
            modifier  = Modifier.fillMaxWidth().padding(top = 64.dp).padding(horizontal = 16.dp),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = HeroCardBg),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // ── Identity row ────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Square avatar 60×60
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        val hasImage = influencer.logoUrl?.startsWith("http", ignoreCase = true) == true
                        if (hasImage) {
                            AsyncImage(
                                model              = influencer.logoUrl,
                                contentDescription = influencer.name,
                                modifier           = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                                contentScale       = ContentScale.Crop
                            )
                        } else {
                            Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        // Category dot label
                        val category = influencer.categories?.firstOrNull()?.category
                        if (!category.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Box(Modifier.size(6.dp).background(themeColor, CircleShape))
                                Text(category.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold,
                                    color = themeColor, letterSpacing = 0.5.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                        }
                        // Name + verified badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text       = influencer.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 19.sp,
                                color      = Color(0xFF1E1E1E),
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis,
                                modifier   = Modifier.weight(1f, fill = false)
                            )
                            if (influencer.isVerified == true) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, "Verified", tint = Color(0xFF1976D2), modifier = Modifier.size(15.dp))
                            }
                        }
                        if (!handle.isNullOrBlank()) {
                            Text("@$handle", fontSize = 12.5.sp, color = SubLabel)
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Compact Proposal button — sits beside the avatar at card top
                    Button(
                        onClick  = onCreateProposal,
                        modifier = Modifier.height(34.dp),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = themeColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(13.dp), tint = Color.White)
                        Spacer(Modifier.width(5.dp))
                        Text("Proposal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }

                // ── Divider ─────────────────────────────────────────────────
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(12.dp))

                // ── Unified 4×2 stats grid ──────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    statRows.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { stat ->
                                Column(
                                    modifier            = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text       = stat.value,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 14.sp,
                                        color      = stat.color,
                                        maxLines   = 1
                                    )
                                    Text(
                                        text      = stat.label,
                                        fontSize  = 9.5.sp,
                                        color     = SubLabel,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
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
private fun AboutSection(
    bio: String? = null,
    about: String? = null,
    creatorName: String? = null
) {
    SectionCard {
        SectionHeader(Icons.Default.Info, "About the Creator")
        Spacer(Modifier.height(10.dp))

        // Creator name badge
        if (!creatorName.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF7B1FA2),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = creatorName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF37474F)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // Short bio line
        if (!bio.isNullOrBlank()) {
            Text(bio, fontSize = 14.sp, color = Color(0xFF546E7A), lineHeight = 22.sp, fontWeight = FontWeight.Medium)
        }

        // Long about paragraph
        if (!about.isNullOrBlank()) {
            if (!bio.isNullOrBlank()) Spacer(Modifier.height(8.dp))
            Text(about, fontSize = 13.sp, color = Color(0xFF78909C), lineHeight = 20.sp)
        }
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
private fun BrandInstagramProfilesSection(profiles: List<InstagramProfile>, primaryFollowers: Int? = null) {
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
                            val displayFollowers = profile.followers
                                ?: if (profile.isDefault) primaryFollowers else null
                            if (displayFollowers != null) {
                                Text("${formatInfluencerCount(displayFollowers)} followers", color = SubLabel, fontSize = 12.sp)
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Column {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(label, fontSize = 9.sp, color = SubLabel, lineHeight = 11.sp)
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(Icons.Default.CreditCard, "Payments & Deliverables")
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                influencer.pricing?.forEachIndexed { index, pricing ->
                    val platformType  = pricing.platform.uppercase()
                    val platformColor = platformsColors[platformType] ?: themeColor
                    Row(
                        modifier  = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(color = platformColor.copy(alpha = 0.10f), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(36.dp)) {
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    when (platformType) {
                                        "YOUTUBE"   -> Image(painterResource(R.drawable.youtube_logo),   null, modifier = Modifier.size(18.dp))
                                        "INSTAGRAM" -> Image(painterResource(R.drawable.instagram_logo), null, modifier = Modifier.size(18.dp))
                                        else        -> Icon(getDeliverableIcon(pricing.deliverable), null, tint = platformColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(pricing.deliverable, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("~ ₹${formatInfluencerCount(pricing.price)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    if (index < (influencer.pricing?.size ?: 0) - 1) {
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

// ── YouTube Demographics ──────────────────────────────────────────────────────
// Replaces the space-hungry donut with a compact horizontal-bar-per-age-group
// layout: all 6 age bands visible at once in roughly the same height as the
// old donut alone used to need for just the dominant one.
@Composable
private fun YouTubeDemographicsCard(demographics: List<np.com.bimalkafle.firebaseauthdemoapp.model.YoutubeDemographics>) {
    // Aggregate age and gender from the flat list
    val ageData = demographics
        .groupBy { it.ageGroup ?: "Other" }
        .mapValues { e -> e.value.sumOf { (it.percentage ?: 0f).toDouble() }.toFloat() }
        .entries.sortedByDescending { it.value }
    val genderData = demographics
        .groupBy { it.gender?.lowercase() ?: "other" }
        .mapValues { e -> e.value.sumOf { (it.percentage ?: 0f).toDouble() }.toFloat() }
    val femaleRaw = genderData["female"] ?: 0f
    val maleRaw   = genderData["male"]   ?: 0f

    val barColor  = MaterialTheme.colorScheme.primary
    val maxAge    = ageData.firstOrNull()?.value ?: 1f

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Audience Demographics", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            // ── Age Distribution: compact bar rows ─────────────────────────
            if (ageData.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("Age Distribution", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SubLabel)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    ageData.take(6).forEach { (rawLabel, pct) ->
                        val label = rawLabel.removePrefix("age")
                        val frac  = (pct / maxAge).coerceIn(0f, 1f)
                        Row(
                            verticalAlignment       = Alignment.CenterVertically,
                            horizontalArrangement   = Arrangement.spacedBy(8.dp)
                        ) {
                            // Age label — fixed 38dp so all bars align
                            Text(
                                text     = label,
                                fontSize = 11.sp,
                                color    = Color(0xFF333333),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(38.dp)
                            )
                            // Thin bar (6dp height, scaled to max, rounded caps)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barColor.copy(alpha = 0.10f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(frac)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(barColor)
                                )
                            }
                            // Percentage — fixed 38dp right-aligned
                            Text(
                                text      = "${"%.1f".format(pct)}%",
                                fontSize  = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color     = SubLabel,
                                modifier  = Modifier.width(38.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // ── Gender Split: two-segment bar ──────────────────────────────
            val gTotal = femaleRaw + maleRaw
            if (gTotal > 0f) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(14.dp))
                Text("Gender Split", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = SubLabel)
                Spacer(Modifier.height(10.dp))
                val femaleFrac = femaleRaw / gTotal
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(52.dp)) {
                        Text("Female", fontSize = 11.sp, color = Color(0xFFF06292), fontWeight = FontWeight.SemiBold)
                        Text("${"%.1f".format(femaleRaw)}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF06292))
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF64B5F6))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(femaleFrac).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF06292))
                        )
                    }
                    Column(modifier = Modifier.width(52.dp)) {
                        Text("Male", fontSize = 11.sp, color = Color(0xFF64B5F6), fontWeight = FontWeight.SemiBold)
                        Text("${"%.1f".format(maleRaw)}%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF64B5F6))
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
