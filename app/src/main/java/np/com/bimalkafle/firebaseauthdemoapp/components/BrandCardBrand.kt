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
    val words = name.trim().split("\\s+".toRegex())

    return when {
        words.isEmpty() -> ""
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
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

    // Analytics extras
    val avgLikes    = influencer.instagramMetrics?.avgLikes?.let { formatCount(it.toLong()) }

    // Stat items: Followers replaced Comments, now includes all key metrics
    val statItems = buildList<Pair<ImageVector, String>> {
        effectiveAvgViews?.let { add(Icons.Default.Visibility to formatCount(it)) }
        influencer.collaborationCount?.let { add(Icons.Default.Handshake to it.toString()) }
        effectiveEr?.let { add(Icons.Default.Insights to String.format("%.1f%%", it)) }
        avgLikes?.let { add(Icons.Default.ThumbUp to it) }
        totalFollowers?.let { if (it > 0) add(Icons.Default.Groups to formatCount(it.toLong())) }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Parent Row: Avatar + Info (Header + Stats)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar Box: Fills height to match Info Column
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .fillMaxHeight()
                        .then(SuppressIntrinsicHeight)
                        .clip(RoundedCornerShape(12.dp))
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    val hasImage = influencer.logoUrl?.startsWith("http", ignoreCase = true) == true

                    if (hasImage) {
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
                            fontSize = 24.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Info Column (Header + Stats)
                Column(modifier = Modifier.weight(1f)) {

                    // Row 1: Name + verified icon + rating + Wishlist
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = influencer.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
                        }

                        // Wishlist button at top right
                        Box(
                            modifier = Modifier.size(32.dp).offset(y = (-12).dp, x = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { onWishlistToggle() }) {
                                Icon(
                                    imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isWishlisted) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Row 2: location · category + tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-5).dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Location and Category part that can truncate
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!influencer.location.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                                    Spacer(Modifier.width(1.dp))
                                    Text(
                                        text = influencer.location,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            val cat = influencer.categories?.firstOrNull()?.category
                            if (!cat.isNullOrBlank()) {
                                if (!influencer.location.isNullOrBlank()) {
                                    Text("·", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    color = themeColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                        
                        // Badges immediately after category
                        val tier = influencer.tier?.takeIf { it.isNotBlank() }
                        if (tier != null) TierBadge(tier)
                        AvailabilityBadge(available = influencer.availability == true)
                    }

                    // Stats Row: Occupies entire width below header, now closely spaced
                    if (statItems.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().offset(y = (-3).dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            statItems.forEach { (icon, value) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(11.dp))
                                    Text(
                                        text = value,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bio & Demographics - Outside the top Row so Image ends at Stats level
            if (!influencer.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = influencer.bio,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasDemographics) {
                Spacer(modifier = Modifier.height(4.dp))
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
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
                    }
                    if (topAgeGroup != null) {
                        Text("·", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                        Text(
                            text = "${topAgeGroup.range} (${topAgeGroup.percentage.toInt()}%)",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    if (topLocation != null) {
                        Text("·", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                        Text(
                            text = topLocation.city,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B),
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
    Surface(shape = RoundedCornerShape(2.dp), color = bg) {
        Text(
            text = tier.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun AvailabilityBadge(available: Boolean) {
    val bg  = if (available) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
    val fg  = if (available) Color(0xFF15803D) else Color(0xFF94A3B8)
    val label = if (available) "Available" else "Busy"
    Surface(shape = RoundedCornerShape(2.dp), color = bg) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
