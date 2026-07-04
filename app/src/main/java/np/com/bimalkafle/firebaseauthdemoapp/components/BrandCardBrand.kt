package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import kotlin.math.abs

// Blocks intrinsic height propagation from AsyncImage so IntrinsicSize.Min
// on the parent Row is driven by the text Column, not the loaded image size.
private object SuppressIntrinsicHeight : LayoutModifier {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val p = measurable.measure(constraints)
        return layout(p.width, p.height) { p.place(0, 0) }
    }
    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int) = 0
    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurable: IntrinsicMeasurable, width: Int) = 0
}

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
    val parts = name.trim().split(Regex("\\s+"))
    return if (parts.size >= 2)
        "${parts[0].first()}${parts[1].first()}".uppercase()
    else
        name.take(2).uppercase()
}

// ─── Main card ──────────────────────────────────────────────────────────────

@Composable
fun BrandCardBrand(
    influencer: InfluencerProfile,
    isWishlisted: Boolean = false,
    onWishlistToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit,
    selectedPlatform: String? = null
) {
    val themeColor = MaterialTheme.colorScheme.primary

    val platformToShow = if (selectedPlatform != null)
        influencer.platforms?.find { it.platform.equals(selectedPlatform, ignoreCase = true) }
    else influencer.platforms?.firstOrNull()

    val effectiveEr = influencer.instagramMetrics?.engagementRate?.toDouble()
        ?: influencer.engagementRate
        ?: platformToShow?.engagement?.toDouble()

    val effectiveAvgViews = influencer.instagramMetrics?.avgViews?.toLong()
        ?: platformToShow?.avgViews?.toLong()

    val totalFollowers = influencer.totalFollowers
        ?: influencer.platforms?.sumOf { it.followers ?: 0 }

    val initials = remember(influencer.name) { initialsFor(influencer.name) }
    val avatarBg = remember(influencer.name) { avatarColorFor(influencer.name) }

    // Audience demographics
    val genderSplit  = influencer.audienceInsights?.genderSplit
    val topAgeGroup  = influencer.audienceInsights?.ageGroups?.maxByOrNull { it.percentage }
    val topLocation  = influencer.audienceInsights?.topLocations?.maxByOrNull { it.percentage }
    val hasDemographics = genderSplit != null || topAgeGroup != null || topLocation != null

    // Analytics extras (avg likes / comments from Instagram metrics)
    val avgLikes    = influencer.instagramMetrics?.avgLikes?.let { formatCount(it.toLong()) }
    val avgComments = influencer.instagramMetrics?.avgComments?.let { formatCount(it.toLong()) }

    // Stat items built up-front so the middle column can render them inline
    val statItems = buildList<Pair<ImageVector, String>> {
        effectiveAvgViews?.let { add(Icons.Default.Visibility to formatCount(it)) }
        influencer.collaborationCount?.let { add(Icons.Default.Handshake to it.toString()) }
        effectiveEr?.let { add(Icons.Default.Insights to String.format("%.1f%%", it)) }
        avgLikes?.let { add(Icons.Default.ThumbUp to it) }
        avgComments?.let { add(Icons.Default.ChatBubbleOutline to it) }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // ── Main row ─────────────────────────────────────────────────────
            // IntrinsicSize.Min makes the Row adopt the Column's natural height.
            // SuppressIntrinsicHeight on the image Box prevents AsyncImage from
            // reporting its loaded pixel dimensions as the intrinsic height
            // (which would make the Row enormous). The image fills the Row height
            // via fillMaxHeight(), cropping to fit without forcing a square.
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .then(SuppressIntrinsicHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (!influencer.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = influencer.logoUrl,
                            contentDescription = influencer.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Middle column — 3 compact rows totalling ~53dp to match image
                Column(modifier = Modifier.weight(1f)) {

                    // Row 1: Name + verified icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = influencer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (influencer.isVerified == true) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                Icons.Default.CheckCircle, "Verified",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        if (influencer.averageRating != null && influencer.averageRating > 0f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(String.format("%.1f", influencer.averageRating), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }

                    // Row 2: location · category  |  [MACRO][Available] right-pinned
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!influencer.location.isNullOrBlank()) {
                                Icon(
                                    Icons.Default.LocationOn, null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = influencer.location,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            val cat = influencer.categories?.firstOrNull()?.category
                            if (!cat.isNullOrBlank()) {
                                if (!influencer.location.isNullOrBlank()) {
                                    Text(" · ", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                }
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    color = themeColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(5.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tier = influencer.tier?.takeIf { it.isNotBlank() }
                            if (tier != null) TierBadge(tier)
                            AvailabilityBadge(available = influencer.availability == true)
                        }
                    }

                    // Row 3: all stats — full width, no badges competing for space
                    if (statItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            statItems.forEachIndexed { idx, (icon, value) ->
                                if (idx > 0) {
                                    Text("·", fontSize = 8.sp, color = Color(0xFFCBD5E1),
                                        modifier = Modifier.padding(horizontal = 3.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(icon, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(10.dp))
                                    Text(value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Followers count
                if (totalFollowers != null && totalFollowers > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCount(totalFollowers.toLong()),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "followers",
                            fontSize = 9.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                }

                // Wishlist heart — 40dp touch target
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    IconButton(onClick = { onWishlistToggle() }) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                            tint = if (isWishlisted) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Bio — appears after the avatar row ────────────────────────────
            if (!influencer.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = influencer.bio,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Audience demographics ─────────────────────────────────────────
            if (hasDemographics) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.People, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                    if (genderSplit != null) {
                        val (label, color) = if (genderSplit.female >= genderSplit.male)
                            "♀ ${genderSplit.female.toInt()}%" to Color(0xFFDB2777)
                        else
                            "♂ ${genderSplit.male.toInt()}%" to Color(0xFF2563EB)
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
                    }
                    if (topAgeGroup != null) {
                        Text("·", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                        Text(
                            text = "${topAgeGroup.range} (${topAgeGroup.percentage.toInt()}%)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (topLocation != null) {
                        Text("·", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                        Text(
                            text = topLocation.city,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─── Badges ─────────────────────────────────────────────────────────────────

@Composable
private fun TierBadge(tier: String) {
    val (bg, fg) = when (tier.uppercase()) {
        "MEGA"  -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "MACRO" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "MICRO" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
        else    -> Color(0xFFE8F5E9) to Color(0xFF2E7D32) // NANO / MINI
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            text = tier.uppercase(),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
        )
    }
}

@Composable
private fun AvailabilityBadge(available: Boolean) {
    val bg  = if (available) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
    val fg  = if (available) Color(0xFF15803D) else Color(0xFF94A3B8)
    val label = if (available) "Available" else "Busy"
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
        )
    }
}

// ─── Formatters ─────────────────────────────────────────────────────────────

private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000     -> String.format("%.1fK", count / 1_000.0)
    else               -> count.toString()
}

private fun formatCount(count: Int): String = formatCount(count.toLong())
