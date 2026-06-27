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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                // Avatar
                Surface(shape = CircleShape, color = brandThemeColor.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                    if (!influencer.logoUrl.isNullOrEmpty()) {
                        AsyncImage(model = influencer.logoUrl, contentDescription = influencer.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                    } else {
                        Box(contentAlignment = Alignment.Center) { Text(text = influencer.name.firstOrNull()?.uppercase() ?: "?", color = brandThemeColor, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    }
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = influencer.name, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 15.sp, 
                            color = Color.Black, 
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (influencer.isVerified == true) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF2196F3), modifier = Modifier.size(14.dp).padding(horizontal = 2.dp))
                        }
                        if (influencer.averageRating != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp)) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(text = String.format("%.1f", influencer.averageRating), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                    
                    Text(text = influencer.location ?: "Unknown Location", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = influencer.categories?.firstOrNull()?.category ?: "General", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = brandThemeColor, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(4.dp), color = if (influencer.availability == true) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)) {
                            Text(text = if (influencer.availability == true) "Available" else "Busy", color = if (influencer.availability == true) Color(0xFF2E7D32) else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                // Wishlist Button - Absolute End
                IconButton(onClick = { onWishlistToggle() }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, 
                        contentDescription = "Wishlist", 
                        tint = if (isWishlisted) Color.Red else Color.Gray, 
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Strengths - More Compact
            influencer.strengths?.let { strengths ->
                if (strengths.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        strengths.take(3).forEach { strength ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = brandThemeColor.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, brandThemeColor.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = strength,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp,
                                    color = brandThemeColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            // Bio - Compact
            Text(text = influencer.bio ?: "No bio available", fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))
            
            // Stats Row - Icon + Count only
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val platformToShow = if (selectedPlatform != null) influencer.platforms?.find { it.platform.equals(selectedPlatform, ignoreCase = true) } else influencer.platforms?.firstOrNull()
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val platformType = platformToShow?.platform?.uppercase()
                    when (platformType) {
                        "YOUTUBE" -> Image(painter = painterResource(id = R.drawable.youtube_logo), contentDescription = null, modifier = Modifier.size(18.dp))
                        "INSTAGRAM" -> Image(painter = painterResource(id = R.drawable.instagram_logo), contentDescription = null, modifier = Modifier.size(18.dp))
                        else -> {
                            val platformIcon = if(platformType == "FACEBOOK") R.drawable.ic_facebook else R.drawable.ic_instagram
                            val platformColor = if(platformType == "FACEBOOK") facebookColor else Color.Gray
                            Surface(shape = CircleShape, color = platformColor, modifier = Modifier.size(18.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(id = platformIcon), contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp)) }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = formatFollowersCount(platformToShow?.followers ?: 0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF5C6BC0), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = formatFollowersCount(platformToShow?.avgViews ?: 0), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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
