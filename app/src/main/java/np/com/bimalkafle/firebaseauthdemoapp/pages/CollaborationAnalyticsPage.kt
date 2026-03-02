package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.CollaborationAnalytics
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel

@Composable
fun CollaborationAnalyticsPage(
    navController: NavController,
    collaborationId: String,
    brandViewModel: BrandViewModel
) {
    val collaborations by brandViewModel.collaborations.observeAsState(emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val collaboration = collaborations.find { it.id == collaborationId }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            result.token?.let { token ->
                brandViewModel.fetchCollaborations(token)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Refined Header matching BrandSearchPage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(Color(0xFFFF8383))
        ) {
            Image(
                painter = painterResource(id = R.drawable.vector),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.2f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { navController.popBackStack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = Color.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Analytics",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (collaboration != null) {
                    Text(
                        text = collaboration.campaign.title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "with ${collaboration.influencer.name}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFFF8383))
            } else if (collaboration == null) {
                Text("Collaboration not found", modifier = Modifier.align(Alignment.Center))
            } else {
                when (collaboration.status) {
                    "PENDING" -> StatusPlaceholder("Proposal not accepted till now", Icons.Default.HourglassEmpty)
                    "ACCEPTED" -> StatusPlaceholder("Advance payment is not completed", Icons.Default.Payment)
                    "COMPLETED" -> AnalyticsContent(collaboration)
                    else -> AnalyticsContent(collaboration) // Show content if possible even for other statuses if analytics exist
                }
            }
        }
    }
}

@Composable
fun StatusPlaceholder(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color(0xFFFF8383).copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AnalyticsContent(collaboration: Collaboration) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            InfluencerMiniProfile(collaboration)
        }

        item {
            Text("Overall Analytics", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            OverallAnalyticsGrid(collaboration)
        }

        item {
            Text("Platform Analytics", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
        }

        items(collaboration.platformAnalytics ?: emptyList()) { analytics ->
            PlatformMetricCard(analytics)
        }
    }
}

@Composable
fun InfluencerMiniProfile(collaboration: Collaboration) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDECEC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                AsyncImage(
                    model = collaboration.influencer.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(collaboration.influencer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Influencer • COMPLETED", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OverallAnalyticsGrid(collaboration: Collaboration) {
    val stats = collaboration.overallAnalytics ?: return
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Impressions", stats.impressions ?: 0, Icons.Default.Visibility, Modifier.weight(1f))
            MetricCard("Views", stats.views ?: 0, Icons.Default.PlayCircle, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Clicks", stats.clicks ?: 0, Icons.Default.AdsClick, Modifier.weight(1f))
            MetricCard("Saves", stats.saves ?: 0, Icons.Default.Bookmark, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Likes", stats.likes ?: 0, Icons.Default.Favorite, Modifier.weight(1f))
            MetricCard("Comments", stats.comments ?: 0, Icons.Default.Comment, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Shares", stats.shares ?: 0, Icons.Default.Share, Modifier.weight(1f))
            MetricCard("Retweets", stats.retweets ?: 0, Icons.Default.Repeat, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Replies", stats.replies ?: 0, Icons.Default.Reply, Modifier.weight(1f))
            Box(Modifier.weight(1f)) // Spacer
        }
    }
}

@Composable
fun MetricCard(label: String, value: Int, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFFFF8383), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun PlatformMetricCard(analytics: CollaborationAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = analytics.platform ?: "Unknown",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF8383)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text("Cost: ₹${analytics.cost ?: 0f}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.LightGray.copy(alpha = 0.3f))
            
            // Grid of metrics for platform
            MetricDetailRow("Impressions", analytics.impressions, "Clicks", analytics.clicks)
            MetricDetailRow("Views", analytics.views, "Likes", analytics.likes)
            MetricDetailRow("Comments", analytics.comments, "Shares", analytics.shares)
            MetricDetailRow("Saves", analytics.saves, "Retweets", analytics.retweets)
            MetricDetailRow("Replies", analytics.replies, "Duration", analytics.duration?.let { "$it days" } ?: "0")
        }
    }
}

@Composable
fun MetricDetailRow(label1: String, value1: Any?, label2: String, value2: Any?) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        MetricDetailItem(label1, value1?.toString() ?: "0", Modifier.weight(1f))
        MetricDetailItem(label2, value2?.toString() ?: "0", Modifier.weight(1f))
    }
}

@Composable
fun MetricDetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}
