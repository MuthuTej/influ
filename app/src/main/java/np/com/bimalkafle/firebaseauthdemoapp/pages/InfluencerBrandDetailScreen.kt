package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.components.ErrorState
import np.com.bimalkafle.firebaseauthdemoapp.components.LoadingState
import np.com.bimalkafle.firebaseauthdemoapp.model.Brand
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignDetail
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignAudienceResponse
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

// ─── Shared palette (same as campaign card) ──────────────────────────────────
private val DetailAvatarPalette = listOf(
    Color(0xFF5E4AE3), Color(0xFFE84393), Color(0xFF0EA5E9),
    Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
)
private fun detailAvatarColor(name: String) =
    DetailAvatarPalette[abs(name.hashCode()) % DetailAvatarPalette.size]

// ─── Date helpers ─────────────────────────────────────────────────────────────
private fun detailFmtDate(s: String?): String {
    if (s.isNullOrEmpty()) return "Open"
    val fmts = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd")
    for (f in fmts) {
        try {
            val d = SimpleDateFormat(f, Locale.getDefault()).parse(s) ?: continue
            return SimpleDateFormat("MMM d", Locale.getDefault()).format(d)
        } catch (_: Exception) {}
    }
    return s
}

private fun detailPostedAgo(createdAt: String): String {
    val fmts = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd")
    for (f in fmts) {
        try {
            val d = SimpleDateFormat(f, Locale.getDefault()).parse(createdAt) ?: continue
            val days = ((System.currentTimeMillis() - d.time) / 86_400_000).toInt()
            return when {
                days == 0  -> "Today"
                days == 1  -> "Yesterday"
                days < 30  -> "$days days ago"
                days < 365 -> "${days / 30} mo ago"
                else       -> "${days / 365} yr ago"
            }
        } catch (_: Exception) {}
    }
    return ""
}

private fun detailUpdatedAgo(updatedAt: String?): String? {
    if (updatedAt.isNullOrEmpty()) return null
    return detailPostedAgo(updatedAt).takeIf { it.isNotEmpty() }?.let { "Updated $it" }
}

private fun detailBudgetFmt(n: Int): String = when {
    n >= 1_000_000 -> "₹${String.format("%.1f", n / 1_000_000.0).trimEnd('0').trimEnd('.')}M"
    n >= 1_000     -> "₹${n / 1_000}k"
    else           -> "₹$n"
}

// ─── Platform helpers ─────────────────────────────────────────────────────────
private fun platformFullName(p: String) = when (p.uppercase()) {
    "INSTAGRAM" -> "Instagram"; "YOUTUBE" -> "YouTube"
    "FACEBOOK"  -> "Facebook";  "TIKTOK"  -> "TikTok"
    else        -> p.replaceFirstChar { it.uppercase() }
}

private fun platformColor(p: String) = when (p.uppercase()) {
    "INSTAGRAM" -> Color(0xFFE1306C); "YOUTUBE" -> Color(0xFFFF0000)
    "FACEBOOK"  -> Color(0xFF1877F2); "TIKTOK"  -> Color(0xFF010101)
    else        -> Color(0xFF6B7280)
}

private fun platformAbbr(p: String) = when (p.uppercase()) {
    "INSTAGRAM" -> "IG"; "YOUTUBE" -> "YT"
    "FACEBOOK"  -> "FB"; "TIKTOK"  -> "TT"
    else        -> p.take(2).uppercase()
}

// ─── Screen ──────────────────────────────────────────────────────────────────

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
            it.token?.let { tok -> campaignViewModel.fetchCampaignById(campaignId, tok) }
        }
    }

    Scaffold(
        bottomBar = {
            if (campaign != null) {
                DetailApplyBar(campaign = campaign!!, navController = navController)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(Color(0xFFF8F9FE))
        ) {
            when {
                isLoading -> LoadingState(modifier = Modifier.align(Alignment.Center), message = "Loading campaign…")
                error != null -> ErrorState(message = error ?: "Unknown error", modifier = Modifier.align(Alignment.Center))
                campaign != null -> DetailContent(campaign = campaign!!, navController = navController)
                else -> Text("Campaign not found.", modifier = Modifier.align(Alignment.Center), fontSize = 14.sp)
            }
        }
    }
}

// ─── Main scrollable content ──────────────────────────────────────────────────

@Composable
private fun DetailContent(campaign: CampaignDetail, navController: NavController) {
    val themeColor = MaterialTheme.colorScheme.primary
    var wishlisted by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColor)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                "Campaign Details",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(onClick = { wishlisted = !wishlisted }, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    if (wishlisted) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    "Wishlist",
                    tint = if (wishlisted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Brand identity row ────────────────────────────────────────────────
        Surface(color = themeColor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val brandName = campaign.brand?.name ?: "Brand"
                val avatarBg = remember(brandName) { detailAvatarColor(brandName) }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!campaign.brand?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = campaign.brand?.logoUrl,
                            contentDescription = brandName,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(avatarBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(brandName.firstOrNull()?.uppercase() ?: "B",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(brandName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (campaign.brand?.isVerified == true) {
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(15.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Brand", color = Color.White.copy(0.75f), fontSize = 12.sp)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height((-16).dp)) // pull up into the theme color band

            // ── Campaign summary card ──────────────────────────────────────────
            DetailCard {
                val (statusBg, statusFg) = when (campaign.status.uppercase()) {
                    "ACTIVE" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
                    "PAUSED" -> Color(0xFFFEF9C3) to Color(0xFFB45309)
                    else     -> Color(0xFFF1F5F9) to Color(0xFF64748B)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                    Text(campaign.status.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        color = statusFg, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(campaign.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(Modifier.height(6.dp))
                Text(campaign.description, fontSize = 13.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
                val ago = detailPostedAgo(campaign.createdAt)
                if (ago.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Posted $ago", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Campaign details ───────────────────────────────────────────────
            DetailSectionLabel("CAMPAIGN DETAILS")
            Spacer(Modifier.height(6.dp))
            DetailCard {
                val budgetText = when {
                    campaign.budgetMin != null && campaign.budgetMax != null ->
                        "${detailBudgetFmt(campaign.budgetMin)} - ${detailBudgetFmt(campaign.budgetMax)}"
                    campaign.budgetMin != null -> "${detailBudgetFmt(campaign.budgetMin)}+"
                    else -> null
                }
                val timelineText = when {
                    campaign.startDate != null && campaign.endDate != null ->
                        "${detailFmtDate(campaign.startDate)} - ${detailFmtDate(campaign.endDate)}"
                    campaign.startDate != null -> "${detailFmtDate(campaign.startDate)} - Open"
                    else -> "Flexible"
                }
                val categories = campaign.categories
                    ?.flatMap { listOf(it.category) + it.subCategories.take(2) }
                    ?.joinToString(", ") ?: campaign.brand?.brandCategories
                    ?.flatMap { listOf(it.category) + it.subCategories.take(1) }
                    ?.joinToString(", ")

                if (budgetText != null) {
                    DetailRow(Icons.Default.CurrencyRupee, "Budget", budgetText, themeColor)
                    DetailDivider()
                }
                DetailRow(Icons.Default.CalendarMonth, "Timeline", timelineText, Color(0xFF0F172A))
                if (!categories.isNullOrBlank()) {
                    DetailDivider()
                    DetailRow(Icons.Default.Category, "Category", categories, themeColor)
                }
                val aud = campaign.targetAudience
                if (aud != null) {
                    val ageStr = if (aud.ageMin != null && aud.ageMax != null) "${aud.ageMin} - ${aud.ageMax}" else "Any"
                    val gStr = aud.gender?.let {
                        if (it.equals("BOTH", true)) "Both" else it.replaceFirstChar { c -> c.uppercase() }
                    } ?: "Any"
                    DetailDivider()
                    DetailRow(Icons.Default.People, "Age & Gender", "$ageStr · $gStr", Color(0xFF0F172A))
                    val locs = aud.locations?.filterNot { it.isBlank() }?.joinToString(", ")
                    if (!locs.isNullOrBlank()) {
                        DetailDivider()
                        DetailRow(Icons.Default.LocationOn, "Locations", locs, Color(0xFF0F172A))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Platforms & formats ────────────────────────────────────────────
            val platforms = campaign.platforms
            if (!platforms.isNullOrEmpty()) {
                DetailSectionLabel("PLATFORMS & FORMATS")
                Spacer(Modifier.height(6.dp))
                DetailCard {
                    platforms.forEachIndexed { idx, plat ->
                        if (idx > 0) DetailDivider()
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            val pColor = platformColor(plat.platform)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = pColor.copy(alpha = 0.12f)
                            ) {
                                Text(platformAbbr(plat.platform), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = pColor, modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(platformFullName(plat.platform), fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            if (!plat.formats.isNullOrEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    plat.formats.joinToString(", "),
                                    fontSize = 13.sp, color = Color(0xFF64748B),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Brand section ─────────────────────────────────────────────────
            DetailSectionLabel("BRAND")
            Spacer(Modifier.height(6.dp))
            DetailCard {
                val brand = campaign.brand
                val brandName = brand?.name ?: "Brand"
                val avatarBg = remember(brandName) { detailAvatarColor(brandName) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!brand?.logoUrl.isNullOrEmpty()) {
                            AsyncImage(brand?.logoUrl, brandName,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop)
                        } else {
                            Text(brandName.firstOrNull()?.uppercase() ?: "B",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(brandName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            if (brand?.isVerified == true) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp))
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (brand?.profileCompleted == true) {
                                Text("Profile 100%", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }

                if (!brand?.about.isNullOrBlank()) {
                    DetailDivider()
                    Text(brand?.about ?: "", fontSize = 13.sp, color = Color(0xFF64748B), lineHeight = 20.sp)
                }

                val cats = brand?.brandCategories
                    ?.flatMap { listOf(it.category) + it.subCategories.take(1) }
                    ?.joinToString(" · ")
                if (!cats.isNullOrBlank()) {
                    DetailDivider()
                    DetailRow(Icons.Default.Category, "Brand categories", cats, themeColor)
                }

                val taud = brand?.targetAudience
                if (taud != null) {
                    val ageStr = if (taud.ageMin != null && taud.ageMax != null) "${taud.ageMin}-${taud.ageMax}" else "Any"
                    val gStr = taud.gender?.let {
                        if (it.equals("BOTH", true)) "Any" else it.replaceFirstChar { c -> c.uppercase() }
                    } ?: "Any"
                    DetailDivider()
                    DetailRow(Icons.Default.People, "Usual audience", "$ageStr · $gStr", Color(0xFF0F172A))
                    val locs = taud.locations?.filterNot { it.isBlank() }?.joinToString(", ")
                    if (!locs.isNullOrBlank()) {
                        DetailDivider()
                        DetailRow(Icons.Default.LocationOn, "Usual locations", locs, Color(0xFF0F172A))
                    }
                }

                val updatedStr = detailUpdatedAgo(brand?.updatedAt)
                if (updatedStr != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(updatedStr, fontSize = 11.sp, color = Color(0xFFCBD5E1))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Reusable detail components ───────────────────────────────────────────────

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DetailSectionLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8), letterSpacing = 0.8.sp)
}

@Composable
private fun DetailDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9), thickness = 1.dp)
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String, valueColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = Color(0xFF64748B), modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor,
            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.2f))
    }
}

// ─── Apply Now bottom bar ─────────────────────────────────────────────────────

@Composable
fun DetailApplyBar(campaign: CampaignDetail, navController: NavController) {
    val themeColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Button(
            onClick = { navController.navigate("influencer_apply_campaign/${campaign.id}") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Apply Now", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
        }
    }
}
