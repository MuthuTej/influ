package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignDetail
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val brandThemeColor = Color(0xFFFF8383)
private val youtubeColor = Color(0xFFFF0000)
private val instagramColor = Color(0xFFE1306C)
private val facebookColor = Color(0xFF1877F2)
private val instaGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFD1D1D))
)

@Composable
fun InfluencerHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    influencerViewModel: InfluencerViewModel,
    campaignViewModel: CampaignViewModel,
    notificationViewModel: NotificationViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val collaborations by influencerViewModel.collaborations.observeAsState(initial = emptyList())
    val campaigns by campaignViewModel.campaigns.observeAsState(initial = emptyList())
    
    // Recommended Campaign streams
    val overallRecommendedCampaigns by campaignViewModel.overallRecommendedCampaigns.observeAsState(initial = emptyList())
    val youtubeRecommendedCampaigns by campaignViewModel.youtubeRecommendedCampaigns.observeAsState(initial = emptyList())
    val instagramRecommendedCampaigns by campaignViewModel.instagramRecommendedCampaigns.observeAsState(initial = emptyList())
    
    // Combine loading states
    val isInfluencerLoading by influencerViewModel.loading.observeAsState(initial = false)
    val isCampaignLoading by campaignViewModel.loading.observeAsState(initial = false)
    val isLoading = isInfluencerLoading || isCampaignLoading
    
    // Combine error states
    val influencerError by influencerViewModel.error.observeAsState()
    val campaignError by campaignViewModel.error.observeAsState()
    
    val influencerProfile by influencerViewModel.influencerProfile.observeAsState()
    val wishlistedCampaigns by campaignViewModel.wishlistedCampaigns.observeAsState(initial = emptyList())
    var firebaseToken by remember { mutableStateOf<String?>(null) }
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)

    // State to handle debug visibility
    var showDebugInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                firebaseToken = result.token
                firebaseToken?.let { token ->
                    influencerViewModel.fetchInfluencerDetails(token)
                    influencerViewModel.fetchCollaborations(token)
                    campaignViewModel.fetchCampaigns(token)
                    notificationViewModel.fetchUnreadCount(currentUser.uid, token)
                }
            }
    }

    // Effect to show debug info briefly when an error occurs or data is empty
    LaunchedEffect(influencerError, campaignError, collaborations, campaigns) {
        if (influencerError != null || campaignError != null || collaborations.isEmpty() || campaigns.isEmpty()) {
            showDebugInfo = true
            delay(100) // Show for 100ms (as close to "a millisecond" while still being visible/removable)
            showDebugInfo = false
        }
    }

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo("influencer_home") { inclusive = true }
            }
        }
    }

    var selectedBottomNavItem by remember { mutableStateOf("Home") }
    var selectedPlatform by remember { mutableStateOf("All") }
    val platforms = listOf("All", "YouTube", "Instagram", "Facebook")

    val filteredCampaigns = remember(selectedPlatform, campaigns, overallRecommendedCampaigns, youtubeRecommendedCampaigns, instagramRecommendedCampaigns) {
        val list = when (selectedPlatform) {
            "All" -> overallRecommendedCampaigns.ifEmpty { campaigns }
            "YouTube" -> youtubeRecommendedCampaigns.ifEmpty {
                campaigns.filter { it.platforms?.any { p -> p.platform.equals("YouTube", true) } == true }
            }
            "Instagram" -> instagramRecommendedCampaigns.ifEmpty {
                campaigns.filter { it.platforms?.any { p -> p.platform.equals("Instagram", true) } == true }
            }
            "Facebook" -> campaigns.filter { it.platforms?.any { p -> p.platform.equals("Facebook", true) } == true }
            else -> campaigns
        }
        list.take(10)
    }

    Scaffold(
        bottomBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    CmnBottomNavigationBar(
                        selectedItem = selectedBottomNavItem,
                        onItemSelected = { selectedBottomNavItem = it },
                        navController = navController
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->

        if (isLoading && campaigns.isEmpty() && collaborations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = brandThemeColor)
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .background(Color(0xFFF8F9FE))
            ) {

                item { InfluencerHeaderAndReachSection(influencerProfile, navController, unreadCount) }

                // Checklist item for debugging - Controlled by showDebugInfo
                item {
                    if (showDebugInfo) {
                        FetchStatusChecklist(
                            influencerId = influencerProfile?.id,
                            tokenPresent = firebaseToken != null,
                            collabCount = collaborations.size,
                            campaignCount = campaigns.size,
                            influencerError = influencerError,
                            campaignError = campaignError
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        ActiveCollaborationsSection(
                            collaborations = collaborations,
                            navController = navController,
                            onViewAllClick = {
                                navController.navigate("proposals")
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Top Picks for You",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            TabRow(
                                selectedTabIndex = platforms.indexOf(selectedPlatform),
                                containerColor = Color.Transparent,
                                indicator = { tabPositions ->
                                    val currentTabColor = when (selectedPlatform) {
                                        "YouTube" -> youtubeColor
                                        "Instagram" -> instagramColor
                                        "Facebook" -> facebookColor
                                        "All" -> brandThemeColor
                                        else -> brandThemeColor
                                    }
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[platforms.indexOf(selectedPlatform)]),
                                        color = currentTabColor
                                    )
                                }
                            ) {
                                platforms.forEach { platform ->
                                    Tab(
                                        selected = selectedPlatform == platform,
                                        onClick = { selectedPlatform = platform },
                                        icon = {
                                            if (platform == "All") {
                                                Icon(
                                                    imageVector = Icons.Default.Campaign,
                                                    contentDescription = "All",
                                                    modifier = Modifier.size(24.dp),
                                                    tint = if (selectedPlatform == platform) brandThemeColor else Color.Gray
                                                )
                                            } else {
                                                val iconRes = when (platform) {
                                                    "YouTube" -> R.drawable.ic_youtube
                                                    "Instagram" -> R.drawable.ic_instagram
                                                    "Facebook" -> R.drawable.ic_facebook
                                                    else -> R.drawable.ic_youtube
                                                }
                                                val iconColor = when (platform) {
                                                    "YouTube" -> youtubeColor
                                                    "Instagram" -> instagramColor
                                                    "Facebook" -> facebookColor
                                                    else -> Color.Gray
                                                }
                                                Icon(
                                                    painter = painterResource(id = iconRes),
                                                    contentDescription = platform,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = if (selectedPlatform == platform) iconColor else iconColor.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (filteredCampaigns.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyMessage = if (campaigns.isEmpty()) "No campaigns available." else "No campaigns found for $selectedPlatform"
                            Text(emptyMessage, color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredCampaigns) { campaign ->
                        CampaignCardInfluencer(
                            campaign = campaign,
                            isWishlisted = wishlistedCampaigns.any { it.id == campaign.id },
                            onWishlistToggle = { 
                                firebaseToken?.let { token ->
                                    campaignViewModel.toggleWishlist(campaign, token)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            onCardClick = {
                                 navController.navigate("campaign_detail/${campaign.id}")
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = {
                            authViewModel.signout()
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandThemeColor)
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FetchStatusChecklist(
    influencerId: String?,
    tokenPresent: Boolean,
    collabCount: Int,
    campaignCount: Int,
    influencerError: String?,
    campaignError: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
        border = BorderStroke(1.dp, Color(0xFFFFB74D))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Debug: Fetch Status", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            Spacer(modifier = Modifier.height(8.dp))
            StatusItem("Influencer ID Detected", influencerId != null)
            StatusItem("Auth Token Present", tokenPresent)
            StatusItem("Collaborations Count", collabCount > 0, suffix = ": $collabCount")
            StatusItem("Campaigns Count", campaignCount > 0, suffix = ": $campaignCount")
            
            if (influencerError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Influencer Error: $influencerError", color = Color.Red, fontSize = 12.sp)
            }
            if (campaignError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Campaign Error: $campaignError", color = Color.Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatusItem(label: String, success: Boolean, suffix: String = "") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (success) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (success) Color(0xFF4CAF50) else Color.Red,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label$suffix", fontSize = 14.sp)
    }
}

@Composable
fun InfluencerHeaderAndReachSection(influencerProfile: InfluencerProfile?, navController: NavController, unreadCount: Int) {
    Box(modifier = Modifier.fillMaxWidth()) {
        // Pink background area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(brandThemeColor)
                .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.vector),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.15f),
                contentScale = ContentScale.Crop
            )
        }

        // Content Column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Profile + Hello + Icons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp)
            ) {
                // Icons pinned to TopEnd and moved up
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-4).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBubbleInfluencer(
                        icon = Icons.Default.Favorite,
                        tint = instagramColor,
                        onClick = { navController.navigate("wishlist") }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box {
                        IconBubbleInfluencer(
                            icon = Icons.Default.Notifications,
                            tint = Color.Black,
                            onClick = { navController.navigate("notifications") }
                        )
                        if (unreadCount > 0) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ) {
                                Text(if (unreadCount > 9) "9+" else unreadCount.toString(), fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Profile and Welcome Text
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.65f)
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(54.dp),
                        shadowElevation = 4.dp
                    ) {
                        if (!influencerProfile?.logoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = influencerProfile?.logoUrl,
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

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text("Hello!", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                        Text(
                            influencerProfile?.name ?: "Guest",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            lineHeight = 22.sp,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Earnings Box: Attached to the header row with a little gap
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 27.dp),
                    shape = RoundedCornerShape(30.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 15.dp)
                ) {
                    Box(
                        modifier = Modifier.background(
                            brush = Brush.verticalGradient(listOf(Color(0xFFFFAFBD), brandThemeColor))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Earnings", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("₹ 18.4K", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                InfluencerStatChip("Engagement", "6.1 %", Modifier.weight(1f))
                                InfluencerStatChip("Leads", "1.2 K", Modifier.weight(1f))
                                InfluencerStatChip("Reach", "2.4 M", Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Find Campaigns Button attached to the bottom
                Button(
                    onClick = { navController.navigate("influencer_search") },
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(54.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
                ) {
                    Text("Find Campaigns", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IconBubbleInfluencer(icon: ImageVector, tint: Color, onClick: () -> Unit = {}) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFF5F5F5),
        modifier = Modifier
            .size(42.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InfluencerStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = value, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            }
        }
    }
}

@Composable
fun ActiveCollaborationsSection(
    collaborations: List<Collaboration>, 
    navController: NavController,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(text = "Active Collaborations", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(brandThemeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = collaborations.size.toString(), color = brandThemeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "View All",
                color = brandThemeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (collaborations.isEmpty()) {
            Text(
                text = "No active collaborations found.",
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val cardWidth = (screenWidth * 0.85f).coerceIn(280.dp, 320.dp)

            LazyRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(collaborations) { collaboration ->
                    val pricing = collaboration.pricing?.firstOrNull()
                    val updatedAt = collaboration.updatedAt

                    fun calculateDaysAgo(updatedAt: String?): String {
                        return try {
                            val instant = Instant.parse(updatedAt)
                            val updatedDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                            val today = LocalDateTime.now().toLocalDate()
                            val days = ChronoUnit.DAYS.between(updatedDate, today)
                            when {
                                days == 0L -> "Today"
                                days == 1L -> "1 day ago"
                                else -> "$days days ago"
                            }
                        } catch (e: Exception) { "Recently" }
                    }

                    Box(modifier = Modifier.width(cardWidth)) {
                        CollaborationItem(
                            brandName = collaboration.brand?.name ?: "Brand",
                            brandLogo = collaboration.brand?.logoUrl,
                            campaignTitle = collaboration.campaign.title,
                            status = collaboration.status,
                            deliverable = pricing?.deliverable ?: "N/A",
                            platform = pricing?.platform ?: "N/A",
                            price = pricing?.price ?: 0,
                            currency = pricing?.currency ?: "USD",
                            time = calculateDaysAgo(updatedAt),
                            onClick = { navController.navigate("collaboration_analytics/${collaboration.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollaborationItem(
    brandName: String,
    campaignTitle: String,
    status: String,
    deliverable: String,
    platform: String,
    price: Int,
    currency: String,
    time: String,
    brandLogo: String?,
    onClick: () -> Unit
) {
    val statusColor = when (status) {
        "ACCEPTED" -> Color(0xFF4CAF50)
        "PENDING" -> Color(0xFFFFB74D)
        "REJECTED" -> Color(0xFFE57373)
        "IN_PROGRESS" -> Color(0xFF42A5F5)
        "COMPLETED" -> Color(0xFF66BB6A)
        else -> Color.Gray
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFFF0F0F0), modifier = Modifier.size(40.dp)) {
                        if (!brandLogo.isNullOrEmpty()) {
                            AsyncImage(model = brandLogo, contentDescription = brandName, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(contentAlignment = Alignment.Center) { Text(text = if (brandName.isNotEmpty()) brandName.first().uppercase() else "B", color = brandThemeColor, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = brandName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(text = campaignTitle, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                Surface(shape = RoundedCornerShape(25), color = statusColor.copy(alpha = 0.15f)) {
                    Text(text = status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "$deliverable • $platform", fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(text = "$currency $price", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = brandThemeColor)
                }
                Text(text = time, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun CampaignCardInfluencer(
    campaign: CampaignDetail,
    isWishlisted: Boolean = false,
    onWishlistToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = brandThemeColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(56.dp)
                ) {
                    if (!campaign.brand?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = campaign.brand?.logoUrl,
                            contentDescription = campaign.brand?.name,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = campaign.brand?.name?.firstOrNull()?.uppercase() ?: "?",
                                color = brandThemeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = campaign.brand?.name ?: "Unknown Brand",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = { onWishlistToggle() }) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) instagramColor else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = campaign.description,
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Budget", fontSize = 11.sp, color = Color.Gray)
                    
                    val formatBudget = { amount: Int? ->
                        when {
                            amount == null -> "-"
                            amount >= 1000000 -> "${String.format("%.1f", amount / 1000000.0)}M"
                            amount >= 1000 -> "${amount / 1000}k"
                            else -> amount.toString()
                        }
                    }
                    
                    val budgetRange = when {
                        campaign.budgetMin != null && campaign.budgetMax != null -> "₹${formatBudget(campaign.budgetMin)} - ₹${formatBudget(campaign.budgetMax)}"
                        campaign.budgetMin != null -> "₹${formatBudget(campaign.budgetMin)}+"
                        else -> "N/A"
                    }
                    
                    Text(text = budgetRange, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = brandThemeColor)
                }
            }
        }
    }
}
