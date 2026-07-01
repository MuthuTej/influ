package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile

@OptIn(ExperimentalLayoutApi::class)
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Row 1: Avatar + identity + wishlist ──────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                // Avatar
                Surface(shape = CircleShape, color = themeColor.copy(alpha = 0.1f), modifier = Modifier.size(52.dp)) {
                    if (!influencer.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = influencer.logoUrl,
                            contentDescription = influencer.name,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(influencer.name.firstOrNull()?.uppercase() ?: "?", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Name + verified + rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = influencer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (influencer.isVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(15.dp))
                        }
                        if (influencer.averageRating != null && influencer.averageRating > 0f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(13.dp))
                                Text(String.format("%.1f", influencer.averageRating), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                    // Location + category
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!influencer.location.isNullOrBlank()) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Text(influencer.location, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        }
                        val primaryCat = influencer.categories?.firstOrNull()?.category
                        if (!primaryCat.isNullOrBlank()) {
                            if (!influencer.location.isNullOrBlank()) Text(" · ", fontSize = 11.sp, color = Color.Gray)
                            Text(primaryCat, fontSize = 11.sp, color = themeColor, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    // Tier + availability badges
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        val tier = influencer.tier?.takeIf { it.isNotBlank() }
                        if (tier != null) {
                            TierBadge(tier, themeColor)
                        }
                        val avail = influencer.availability
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (avail == true) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
                        ) {
                            Text(
                                text = if (avail == true) "Available" else "Busy",
                                color = if (avail == true) Color(0xFF2E7D32) else Color.Gray,
                                fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                // Wishlist button
                IconButton(onClick = { onWishlistToggle() }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // ── Row 2: Key performance stats ────────────────────────────────
            val platformToShow = if (selectedPlatform != null)
                influencer.platforms?.find { it.platform.equals(selectedPlatform, ignoreCase = true) }
            else influencer.platforms?.firstOrNull()

            // Engagement rate: prefer instagramMetrics or backend engagementRate over platform.engagement
            val effectiveEr = influencer.instagramMetrics?.engagementRate?.toDouble()
                ?: influencer.engagementRate
                ?: platformToShow?.engagement?.toDouble()

            // Avg views: prefer instagramMetrics avgViews
            val effectiveAvgViews = influencer.instagramMetrics?.avgViews?.toLong()
                ?: platformToShow?.avgViews?.toLong()

            val totalFollowers = influencer.totalFollowers
                ?: influencer.platforms?.sumOf { it.followers ?: 0 }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricPill(
                    icon = R.drawable.ic_instagram,
                    iconTint = Color(0xFFE1306C),
                    label = "Followers",
                    value = totalFollowers?.let { formatCount(it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = null,
                    iconVector = Icons.Default.Insights,
                    iconTint = Color(0xFF7B1FA2),
                    label = "Eng Rate",
                    value = effectiveEr?.let { String.format("%.1f%%", it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = null,
                    iconVector = Icons.Default.Visibility,
                    iconTint = Color(0xFF1565C0),
                    label = "Avg Views",
                    value = effectiveAvgViews?.let { formatCount(it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    icon = null,
                    iconVector = Icons.Default.Handshake,
                    iconTint = Color(0xFF2E7D32),
                    label = "Collabs",
                    value = influencer.collaborationCount?.toString() ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Row 3: Bio ───────────────────────────────────────────────────
            if (!influencer.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = influencer.bio,
                    fontSize = 12.sp,
                    color = Color(0xFF444444),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // ── Row 4: Audience demographics (only when data exists) ─────────
            val genderSplit = influencer.audienceInsights?.genderSplit
            val topAgeGroup = influencer.audienceInsights?.ageGroups
                ?.maxByOrNull { it.percentage }
            val topLocation = influencer.audienceInsights?.topLocations
                ?.maxByOrNull { it.percentage }

            if (genderSplit != null || topAgeGroup != null || topLocation != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.People, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(14.dp))
                    if (genderSplit != null) {
                        val dominantGender = if (genderSplit.female >= genderSplit.male) "Female ${genderSplit.female.toInt()}%" else "Male ${genderSplit.male.toInt()}%"
                        Text(dominantGender, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                    }
                    if (topAgeGroup != null) {
                        Text("·", fontSize = 11.sp, color = Color.Gray)
                        Text("Age ${topAgeGroup.range} (${topAgeGroup.percentage.toInt()}%)", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                    }
                    if (topLocation != null) {
                        Text("·", fontSize = 11.sp, color = Color.Gray)
                        Text(topLocation.city, fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ── Row 5: Strengths ─────────────────────────────────────────────
            val strengths = influencer.strengths?.takeIf { it.isNotEmpty() }
            if (strengths != null) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    strengths.take(4).forEach { strength ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = themeColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = strength,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                color = themeColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── Row 6: AI insights ───────────────────────────────────────────
            val ai = influencer.aiInsights
            if (ai?.primaryNiche != null || ai?.brandSuitability != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = CircleShape, color = Color(0xFFF3E5F5), modifier = Modifier.size(22.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(12.dp))
                        }
                    }
                    Column {
                        if (ai.primaryNiche != null) {
                            Text(ai.primaryNiche, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7B1FA2), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (ai.brandSuitability != null) {
                            Text(ai.brandSuitability, fontSize = 11.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // ── Row 7: Platform icons row (when multiple platforms) ──────────
            val platforms = influencer.platforms?.takeIf { it.size > 1 }
            if (platforms != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    platforms.forEach { plat ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PlatformIcon(plat.platform)
                            Text(
                                text = plat.followers?.let { formatCount(it.toLong()) } ?: "—",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TierBadge(tier: String, themeColor: Color) {
    val (bg, fg) = when (tier.uppercase()) {
        "MEGA"  -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "MACRO" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "MICRO" -> Color(0xFFF3E5F5) to Color(0xFF6A1B9A)
        else    -> Color(0xFFE8F5E9) to Color(0xFF2E7D32) // NANO
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            text = tier.uppercase(),
            fontSize = 9.sp, fontWeight = FontWeight.Bold, color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun MetricPill(
    icon: Int? = null,
    iconVector: ImageVector? = null,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                iconVector != null -> Icon(iconVector, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
                icon != null -> Image(painter = painterResource(id = icon), contentDescription = null, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
        Text(label, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
    }
}

@Composable
private fun PlatformIcon(platform: String) {
    when (platform.uppercase()) {
        "INSTAGRAM" -> Image(painter = painterResource(id = R.drawable.instagram_logo), contentDescription = null, modifier = Modifier.size(14.dp))
        "YOUTUBE"   -> Image(painter = painterResource(id = R.drawable.youtube_logo), contentDescription = null, modifier = Modifier.size(14.dp))
        else -> {
            Surface(shape = CircleShape, color = Color(0xFF1877F2), modifier = Modifier.size(14.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(id = R.drawable.ic_facebook), contentDescription = null, tint = Color.White, modifier = Modifier.size(8.dp))
                }
            }
        }
    }
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000     -> String.format("%.1fK", count / 1_000.0)
    else               -> count.toString()
}

private fun formatCount(count: Int): String = formatCount(count.toLong())
