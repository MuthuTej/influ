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
import np.com.bimalkafle.firebaseauthdemoapp.components.AiChatFab
import np.com.bimalkafle.firebaseauthdemoapp.components.AppPullToRefreshBox
import np.com.bimalkafle.firebaseauthdemoapp.components.HeroStatColumnData
import np.com.bimalkafle.firebaseauthdemoapp.components.HomeHeroCard
import np.com.bimalkafle.firebaseauthdemoapp.components.PerformanceStatsSection
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
import np.com.bimalkafle.firebaseauthdemoapp.components.EmptyState
import np.com.bimalkafle.firebaseauthdemoapp.components.LoadingState
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.computeInfluencerHeroStats
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.computePerformanceStats
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private val brandThemeColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary
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
        floatingActionButton = { AiChatFab(navController) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->

        if (isLoading && campaigns.isEmpty() && collaborations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingState(message = "Loading your dashboard…")
            }
        } else {
            AppPullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                        firebaseToken = result.token
                        firebaseToken?.let { token ->
                            influencerViewModel.fetchInfluencerDetails(token, force = true)
                            influencerViewModel.fetchCollaborations(token, force = true)
                            campaignViewModel.fetchCampaigns(token, force = true)
                            notificationViewModel.fetchUnreadCount(currentUser.uid, token, force = true)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding())
                        .background(Color(0xFFF8F9FE))
                ) {

                    item {
                        val heroStats = remember(collaborations) { computeInfluencerHeroStats(collaborations) }
                        InfluencerHeaderAndReachSection(influencerProfile, navController, unreadCount, heroStats)
                    }

                    item {
                        val performanceStats = remember(collaborations) { computePerformanceStats(collaborations) }
                        Spacer(modifier = Modifier.height(20.dp))
                        PerformanceStatsSection(
                            views = performanceStats.views,
                            engagementRatePercent = performanceStats.engagementRatePercent,
                            impressions = performanceStats.impressions
                        )
                    }

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
                                                } else if (platform == "YouTube" || platform == "Instagram") {
                                                    val iconRes = if (platform == "YouTube") R.drawable.youtube_logo else R.drawable.instagram_logo
                                                    Image(
                                                        painter = painterResource(id = iconRes),
                                                        contentDescription = platform,
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .alpha(if (selectedPlatform == platform) 1f else 0.5f)
                                                    )
                                                } else {
                                                    val iconRes = when (platform) {
                                                        "Facebook" -> R.drawable.ic_facebook
                                                        else -> R.drawable.ic_youtube
                                                    }
                                                    val iconColor = when (platform) {
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
                            EmptyState(
                                icon = Icons.Default.Campaign,
                                title = "No campaigns yet",
                                subtitle = if (campaigns.isEmpty()) "Check back soon for new campaigns." else "No campaigns found for $selectedPlatform."
                            )
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
fun InfluencerHeaderAndReachSection(
    influencerProfile: InfluencerProfile?,
    navController: NavController,
    unreadCount: Int,
    heroStats: np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerHeroStats
) {
    HomeHeroCard(
        greetingName = influencerProfile?.name ?: "Guest",
        profileLogoUrl = influencerProfile?.logoUrl,
        unreadCount = unreadCount,
        amountLabel = "Total Earnings",
        amount = heroStats.totalEarnings,
        trendPercent = heroStats.earningsTrendPercent,
        statColumns = listOf(
            HeroStatColumnData("Awaiting", heroStats.awaitingResponseCount.toString()),
            HeroStatColumnData("Active", heroStats.activeCollaborationsCount.toString()),
            HeroStatColumnData("Completed", heroStats.completedCount.toString())
        ),
        ctaLabel = "Find Campaigns",
        onCtaClick = { navController.navigate("influencer_search") },
        onHeartClick = { navController.navigate("wishlist") },
        onBellClick = { navController.navigate("notifications") }
    )
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
            EmptyState(
                icon = Icons.Default.WorkOutline,
                title = "No active collaborations yet",
                subtitle = "Apply to a campaign to get started."
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

private val CampaignAvatarPalette = listOf(
    Color(0xFF5E4AE3), Color(0xFFE84393), Color(0xFF0EA5E9),
    Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444),
)

private fun campaignBudgetFmt(n: Int): String = when {
    n >= 1_000_000 -> "₹${String.format("%.1f", n / 1_000_000.0).trimEnd('0').trimEnd('.')}M"
    n >= 1_000     -> "₹${n / 1_000}k"
    else           -> "₹$n"
}

private fun campaignDateFmt(s: String?): String? {
    if (s.isNullOrEmpty()) return null
    val fmts = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd")
    for (f in fmts) {
        try {
            val d = SimpleDateFormat(f, Locale.getDefault()).parse(s) ?: continue
            return SimpleDateFormat("MMM d", Locale.getDefault()).format(d)
        } catch (_: Exception) {}
    }
    return null
}

@Composable
fun CampaignCardInfluencer(
    campaign: CampaignDetail,
    isWishlisted: Boolean = false,
    onWishlistToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit
) {
    val themeColor = MaterialTheme.colorScheme.primary
    val brandName = campaign.brand?.name ?: "Brand"
    val avatarBg = remember(brandName) { CampaignAvatarPalette[abs(brandName.hashCode()) % CampaignAvatarPalette.size] }

    val budgetText = when {
        campaign.budgetMin != null && campaign.budgetMax != null ->
            "${campaignBudgetFmt(campaign.budgetMin)} - ${campaignBudgetFmt(campaign.budgetMax)}"
        campaign.budgetMin != null -> "${campaignBudgetFmt(campaign.budgetMin)}+"
        else -> null
    }

    val dateText = when {
        campaign.startDate != null && campaign.endDate != null ->
            "${campaignDateFmt(campaign.startDate)} - ${campaignDateFmt(campaign.endDate)}"
        campaign.startDate != null -> "${campaignDateFmt(campaign.startDate)} - Open"
        else -> "Flexible dates"
    }

    val (statusBg, statusFg) = when (campaign.status.uppercase()) {
        "ACTIVE" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "PAUSED" -> Color(0xFFFEF9C3) to Color(0xFFB45309)
        "CLOSED" -> Color(0xFFF1F5F9) to Color(0xFF64748B)
        else     -> Color(0xFFF1F5F9) to Color(0xFF64748B)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // ── Row 1: Avatar · Title/Brand · Heart ──────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (!campaign.brand?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = campaign.brand?.logoUrl,
                            contentDescription = brandName,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(brandName.firstOrNull()?.uppercase() ?: "B",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            campaign.title,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = Color(0xFF0F172A), maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (campaign.brand?.isVerified == true) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2196F3), modifier = Modifier.size(13.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(brandName, fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }

                // Rating + Wishlist grouped together
                val rating = campaign.brand?.averageRating
                if (rating != null && rating > 0) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${"%.1f".format(rating)}", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                }

                IconButton(onClick = onWishlistToggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        null,
                        tint = if (isWishlisted) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Row 2: Status badge · Budget · Dates ─────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = statusBg) {
                    Text(campaign.status.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        color = statusFg, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                if (budgetText != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(budgetText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = themeColor)
                }
                Spacer(Modifier.width(6.dp))
                Text("·", fontSize = 10.sp, color = Color(0xFFCBD5E1))
                Spacer(Modifier.width(6.dp))
                Text(dateText, fontSize = 11.sp, color = Color(0xFF94A3B8),
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }

            // ── Row 3: Description ────────────────────────────────────────────
            if (campaign.description.isNotBlank()) {
                Text(campaign.description, fontSize = 12.sp, color = Color(0xFF64748B),
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }

            // ── Row 4: Platform chips · Category · Audience ───────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                campaign.platforms?.forEach { plat ->
                    val abbr = when (plat.platform.uppercase()) {
                        "INSTAGRAM" -> "IG"; "YOUTUBE" -> "YT"
                        "FACEBOOK" -> "FB"; "TIKTOK" -> "TT"
                        else -> plat.platform.take(2).uppercase()
                    }
                    val pColor = when (plat.platform.uppercase()) {
                        "INSTAGRAM" -> Color(0xFFE1306C); "YOUTUBE" -> Color(0xFFFF0000)
                        "FACEBOOK" -> Color(0xFF1877F2); "TIKTOK" -> Color(0xFF010101)
                        else -> themeColor
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = pColor.copy(alpha = 0.10f)) {
                        Text(abbr, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = pColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                val cat = campaign.categories?.firstOrNull()?.category
                    ?: campaign.brand?.brandCategories?.firstOrNull()?.category
                if (!cat.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF1F5F9)) {
                        Text(cat, fontSize = 9.5.sp, color = Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
                val aud = campaign.targetAudience
                if (aud != null) {
                    val ageStr = if (aud.ageMin != null && aud.ageMax != null) "${aud.ageMin}-${aud.ageMax}" else null
                    val gStr = aud.gender?.let { if (it.equals("BOTH", true)) "Both" else it.replaceFirstChar { c -> c.uppercase() } }
                    val audText = listOfNotNull(ageStr, gStr).joinToString(" · ")
                    if (audText.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF1F5F9)) {
                            Text(audText, fontSize = 9.5.sp, color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
