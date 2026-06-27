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
    val brandThemeColor = MaterialTheme.colorScheme.primary
    val facebookColor = Color(0xFF1877F2)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(shape = CircleShape, color = brandThemeColor.copy(alpha = 0.1f), modifier = Modifier.size(56.dp)) {
                    if (!influencer.logoUrl.isNullOrEmpty()) {
                        AsyncImage(model = influencer.logoUrl, contentDescription = influencer.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Box(contentAlignment = Alignment.Center) { Text(text = influencer.name.firstOrNull()?.uppercase() ?: "?", color = brandThemeColor, fontWeight = FontWeight.Bold, fontSize = 22.sp) }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = influencer.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        if (influencer.isVerified == true) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp).padding(horizontal = 4.dp))
                        }
                        
                        if (influencer.averageRating != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = String.format("%.1f", influencer.averageRating), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        IconButton(onClick = { onWishlistToggle() }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Wishlist", tint = if (isWishlisted) Color.Red else Color.Gray, modifier = Modifier.size(24.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = influencer.location ?: "Unknown Location", fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(text = influencer.categories?.firstOrNull()?.category ?: "General", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = brandThemeColor, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(6.dp), color = if (influencer.availability == true) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)) {
                            Text(text = if (influencer.availability == true) "Available" else "Busy", color = if (influencer.availability == true) Color(0xFF2E7D32) else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            influencer.strengths?.let { strengths ->
                if (strengths.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        strengths.take(3).forEach { strength ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = brandThemeColor.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, brandThemeColor.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = strength,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    color = brandThemeColor,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Text(text = influencer.bio ?: "No bio available", fontSize = 13.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val platformToShow = if (selectedPlatform != null) influencer.platforms?.find { it.platform.equals(selectedPlatform, ignoreCase = true) } else influencer.platforms?.firstOrNull()
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val platformType = platformToShow?.platform?.uppercase()

                    when (platformType) {
                        "YOUTUBE" -> {
                            Image(
                                painter = painterResource(id = R.drawable.youtube_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        "INSTAGRAM" -> {
                            Image(
                                painter = painterResource(id = R.drawable.instagram_logo),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            val platformIcon = when(platformType) {
                                "FACEBOOK" -> R.drawable.ic_facebook
                                else -> R.drawable.ic_instagram
                            }
                            val platformColor = when(platformType) {
                                "FACEBOOK" -> facebookColor
                                else -> Color.Gray
                            }
                            Surface(shape = CircleShape, color = platformColor, modifier = Modifier.size(24.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painter = painterResource(id = platformIcon), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${formatFollowersCount(platformToShow?.followers ?: 0)} Followers", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF5C6BC0), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Avg: ${formatFollowersCount(platformToShow?.avgViews ?: 0)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val relevantPricing = if (selectedPlatform != null) influencer.pricing?.filter { it.platform.equals(selectedPlatform, ignoreCase = true) } else influencer.pricing
            if (!relevantPricing.isNullOrEmpty()) {
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                relevantPricing.take(1).forEach { pricingInfo ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = brandThemeColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${pricingInfo.deliverable}: ₹${pricingInfo.price}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun formatFollowersCount(count: Int): String {
    return when {
        count >= 1000000 -> "${String.format("%.1f", count / 1000000.0)}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}
