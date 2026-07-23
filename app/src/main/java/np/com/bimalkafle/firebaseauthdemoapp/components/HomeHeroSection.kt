package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.Dimens
import np.com.bimalkafle.firebaseauthdemoapp.utils.formatCompactCount
import np.com.bimalkafle.firebaseauthdemoapp.utils.formatCompactCurrency

/**
 * Shared hero card + performance row for both InfluencerHomePage and
 * BrandHomePage — same visual system, fed by role-specific real data
 * (InfluencerHeroStats / BrandHeroStats both reduce to the same
 * amount+trend+three-counts shape from HeroStats.kt). One implementation
 * keeps the two roles visually identical by construction instead of by
 * convention.
 */
private val performanceIconBg = Color(0xFFFFE4E1)
private val performanceIconTint = Color(0xFFE11D48)

data class HeroStatColumnData(val label: String, val value: String, val onClick: (() -> Unit)? = null)

@Composable
fun HomeHeroCard(
    greetingName: String,
    profileLogoUrl: String?,
    unreadCount: Int,
    amountLabel: String,
    amount: Double,
    trendPercent: Double?,
    statColumns: List<HeroStatColumnData>,
    ctaLabel: String,
    onCtaClick: () -> Unit,
    onHeartClick: () -> Unit,
    onBellClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcons: @Composable RowScope.() -> Unit = {}
) {
    val themeColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(themeColor, RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
    ) {
        // Same faint background pattern used on every other screen's themed
        // header (search pages, analytics pages) — keeps this card visually
        // consistent with the rest of the app instead of a one-off gradient.
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .alpha(0.2f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = Dimens.space20, vertical = Dimens.space16)
        ) {
            // Avatar + greeting + icon bubbles
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(48.dp)) {
                    if (!profileLogoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = profileLogoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.brand_profile),
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp).clip(CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(Dimens.space12))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hello,", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                    Text(
                        greetingName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                leadingIcons()
                HeroIconBubble(Icons.Default.Favorite, "Wishlist", onHeartClick)
                Spacer(modifier = Modifier.width(Dimens.space8))
                Box {
                    HeroIconBubble(Icons.Default.Notifications, "Notifications", onBellClick)
                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd),
                            containerColor = Color(0xFFB71C1C),
                            contentColor = Color.White
                        ) {
                            Text(if (unreadCount > 9) "9+" else unreadCount.toString(), fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space20))

            // Amount + trend + CTA
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            amountLabel.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        if (trendPercent != null) {
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            TrendPill(trendPercent)
                        }
                    }
                    Text(
                        formatCompactCurrency(amount),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.space12))
                Button(
                    onClick = onCtaClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(horizontal = Dimens.space16, vertical = Dimens.space12)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(Dimens.space4))
                    Text(ctaLabel, color = themeColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space20))
            HorizontalDivider(color = Color.White.copy(alpha = 0.25f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(Dimens.space16))

            // Awaiting / Active / Completed
            Row(modifier = Modifier.fillMaxWidth()) {
                statColumns.forEach { col ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .let { if (col.onClick != null) it.clickable(onClick = col.onClick) else it },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(col.label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(Dimens.space4))
                        Text(col.value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroIconBubble(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.22f),
        modifier = Modifier.size(Dimens.minTouchTarget).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TrendPill(trendPercent: Double) {
    val isUp = trendPercent >= 0
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.22f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Dimens.space8, vertical = Dimens.space2)
        ) {
            Icon(
                if (isUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                "${if (isUp) "+" else ""}${String.format("%.0f", trendPercent)}%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** "Performance · last 30 days" heading + three real stat cards (Views, Engagement, Impressions). */
@Composable
fun PerformanceStatsSection(
    views: Long,
    engagementRatePercent: Double,
    impressions: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = Dimens.space16)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("Performance", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.width(Dimens.space4))
            Text("· last 30 days", fontSize = 13.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(Dimens.space12))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            PerformanceStatCard(Icons.Default.Visibility, formatCompactCount(views), "Views", Modifier.weight(1f))
            PerformanceStatCard(Icons.Default.Insights, "${String.format("%.1f", engagementRatePercent)}%", "Engagement", Modifier.weight(1.2f))
            PerformanceStatCard(Icons.Default.BarChart, formatCompactCount(impressions), "Impressions", Modifier.weight(1.1f))
        }
    }
}

@Composable
private fun PerformanceStatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = performanceIconBg, modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = performanceIconTint, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 14.sp
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    lineHeight = 11.sp
                )
            }
        }
    }
}
