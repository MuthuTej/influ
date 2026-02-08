package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrandInfluencerDetailScreen(
    influencerId: String,
    onBack: () -> Unit,
    influencerViewModel: InfluencerViewModel
) {
    val influencer by influencerViewModel.influencerProfile.observeAsState()
    val isLoading by influencerViewModel.loading.observeAsState(false)
    val error by influencerViewModel.error.observeAsState()

    LaunchedEffect(influencerId) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            if (token != null) {
                influencerViewModel.fetchInfluencerById(influencerId, token)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Influencer Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF8383))
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $error", color = Color.Red)
            }
        } else if (influencer != null) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Profile Header Card
                ProfileHeaderSection(influencer!!)

                Spacer(modifier = Modifier.height(16.dp))

                // Bio Section
                InfoCard(title = "About") {
                    Text(
                        text = influencer!!.bio ?: "No bio available.",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Section
                StatsSection(influencer!!)

                Spacer(modifier = Modifier.height(16.dp))

                // Audience Insights
                influencer!!.audienceInsights?.let { insights ->
                    AudienceInsightsSection(insights)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Pricing Section
                PricingSection(influencer!!)

                Spacer(modifier = Modifier.height(16.dp))

                // Strengths
                if (!influencer!!.strengths.isNullOrEmpty()) {
                    InfoCard(title = "Strengths") {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            influencer!!.strengths!!.forEach { strength ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text(strength) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderSection(influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFF8383).copy(alpha = 0.1f),
                modifier = Modifier.size(80.dp)
            ) {
                if (!influencer.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = influencer.logoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = influencer.name.firstOrNull()?.uppercase() ?: "?",
                            color = Color(0xFFFF8383),
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(influencer.name, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(influencer.location ?: "Unknown", color = Color.Gray, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (influencer.availability == true) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Available", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StatsSection(influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile) {
    InfoCard(title = "Metrics") {
        influencer.platforms?.forEach { platform ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = if (platform.platform == "YOUTUBE") R.drawable.ic_youtube else R.drawable.ic_instagram),
                        contentDescription = null,
                        tint = if (platform.platform == "YOUTUBE") Color.Red else Color(0xFFE4405F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(platform.platform, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${formatCount(platform.followers ?: 0)} Followers", fontWeight = FontWeight.Bold)
                    Text("Avg Views: ${formatCount(platform.avgViews ?: 0)}", color = Color.Gray, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = Color.Faded(0.1f))
        }
    }
}

@Composable
fun AudienceInsightsSection(insights: np.com.bimalkafle.firebaseauthdemoapp.model.AudienceInsights) {
    InfoCard(title = "Audience Insights") {
        Text("Gender Split", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        insights.genderSplit?.let { split ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Box(modifier = Modifier.weight(split.male).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF2196F3)))
                Spacer(modifier = Modifier.width(4.dp))
                Box(modifier = Modifier.weight(split.female).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE91E63)))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Male: ${split.male}%", fontSize = 12.sp, color = Color(0xFF2196F3))
                Text("Female: ${split.female}%", fontSize = 12.sp, color = Color(0xFFE91E63))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Top Locations", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        insights.topLocations?.forEach { loc ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${loc.city}, ${loc.country}", fontSize = 13.sp)
                Text("${loc.percentage}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PricingSection(influencer: np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile) {
    InfoCard(title = "Pricing") {
        influencer.pricing?.forEach { pricing ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(pricing.deliverable, fontWeight = FontWeight.SemiBold)
                    Text(pricing.platform, fontSize = 12.sp, color = Color.Gray)
                }
                Text("₹${String.format("%,d", pricing.price)} ${pricing.currency}", fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
        }
    }
}

private fun Color.Companion.Faded(alpha: Float) = Color.LightGray.copy(alpha = alpha)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}
