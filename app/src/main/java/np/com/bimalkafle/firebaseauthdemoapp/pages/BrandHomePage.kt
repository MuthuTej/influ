package np.com.bimalkafle.firebaseauthdemoapp.pages

import coil.compose.AsyncImage
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import np.com.bimalkafle.firebaseauthdemoapp.components.AiChatFab
import np.com.bimalkafle.firebaseauthdemoapp.components.AppPullToRefreshBox
import np.com.bimalkafle.firebaseauthdemoapp.components.HeroStatColumnData
import np.com.bimalkafle.firebaseauthdemoapp.components.HomeHeroCard
import np.com.bimalkafle.firebaseauthdemoapp.components.PerformanceStatsSection
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.computeBrandHeroStats
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.computeBrandSpendBuckets
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.computePerformanceStats
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SpendBucket
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SpendBucketPeriod
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel
import np.com.bimalkafle.firebaseauthdemoapp.utils.formatCompactCount
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import np.com.bimalkafle.firebaseauthdemoapp.R
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.vector.ImageVector
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.components.BrandCardBrand
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.EmptyState
import np.com.bimalkafle.firebaseauthdemoapp.components.LoadingState

private val brandThemeColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary

@Composable
fun BrandHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel,
    notificationViewModel: NotificationViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val collaborations by brandViewModel.collaborations.observeAsState(initial = emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val errorMsg by brandViewModel.error.observeAsState()
    val wishlistedInfluencers by brandViewModel.wishlistedInfluencers.observeAsState(initial = emptyList())
    
    // Recommendations from ViewModel
    val overallTopInfluencers by brandViewModel.overallTopInfluencers.observeAsState(initial = emptyList())
    val youtubeTopInfluencers by brandViewModel.youtubeTopInfluencers.observeAsState(initial = emptyList())
    val instagramTopInfluencers by brandViewModel.instagramTopInfluencers.observeAsState(initial = emptyList())
    
    var firebaseToken by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    // Note: Activity feed can also be unified, but keep notification unread count separately if needed
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)
    val brandProfile by brandViewModel.brandProfile.observeAsState()

    LaunchedEffect(errorMsg) {
        if (!errorMsg.isNullOrBlank()) {
            Toast.makeText(context, "Server Error: $errorMsg", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                firebaseToken = result.token
                firebaseToken?.let { token ->
                    // Unified dashboard call
                    brandViewModel.fetchHomeDashboard(token)
                }
            }
    }

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo("brand_home") { inclusive = true }
            }
        }
    }

    var selectedBottomNavItem by remember { mutableStateOf("Home") }

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "Home",
                onItemSelected = { selectedBottomNavItem = it },
                navController = navController,
                isBrand = true
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AiChatFab(navController, size = 40.dp)
                FloatingActionButton(
                    onClick = { navController.navigate("create_campaign") },
                    containerColor = brandThemeColor,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Campaign", tint = Color.White)
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        if (isLoading && collaborations.isEmpty() && overallTopInfluencers.isEmpty()) {
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
                            brandViewModel.fetchHomeDashboard(token, force = true)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(bottom = paddingValues.calculateBottomPadding())
                        .background(Color(0xFFF8F9FE))
                ) {
                    item {
                        val heroStats = remember(collaborations) { computeBrandHeroStats(collaborations) }
                        BrandHeaderAndReachSection(brandProfile, navController, unreadCount, heroStats)
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
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        SpendBreakdownSection(collaborations = collaborations)
                    }
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            ActiveCampaignSection(
                                collaborations = collaborations,
                                brandViewModel = brandViewModel,
                                brandName = brandProfile?.name ?: "Brand",
                                onCollaborationClick = { id ->
                                    navController.navigate("collaboration_analytics/$id")
                                },
                                onViewAllClick = {
                                    navController.navigate("brand_history")
                                }
                            )
                            TopPicksSectionBrand(
                                overallTopInfluencers = overallTopInfluencers,
                                youtubeTopInfluencers = youtubeTopInfluencers,
                                instagramTopInfluencers = instagramTopInfluencers,
                                wishlistedInfluencers = wishlistedInfluencers,
                                onWishlistToggle = { influencer ->
                                    firebaseToken?.let { token ->
                                        brandViewModel.toggleWishlist(influencer, token)
                                    }
                                },
                                navController = navController,
                                brandCategories = brandProfile?.brandCategories?.map { it.category } ?: emptyList()
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
fun BrandHeaderAndReachSection(
    brandProfile: np.com.bimalkafle.firebaseauthdemoapp.model.Brand?,
    navController: NavController,
    unreadCount: Int,
    heroStats: np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandHeroStats
) {
    HomeHeroCard(
        greetingName = brandProfile?.name ?: "Guest",
        profileLogoUrl = brandProfile?.logoUrl,
        unreadCount = unreadCount,
        amountLabel = "Total Spend",
        amount = heroStats.totalSpent,
        trendPercent = heroStats.spendTrendPercent,
        statColumns = listOf(
            HeroStatColumnData("Pending", heroStats.pendingApplicationsCount.toString()) {
                navController.navigate("all_campaigns?filter=PENDING")
            },
            HeroStatColumnData("Active", heroStats.activeCollaborationsCount.toString()) {
                navController.navigate("all_campaigns?filter=ONGOING")
            },
            HeroStatColumnData("Completed", heroStats.completedCount.toString()) {
                navController.navigate("all_campaigns?filter=COMPLETED")
            }
        ),
        ctaLabel = "Launch Campaign",
        onCtaClick = { navController.navigate("create_campaign") },
        onHeartClick = { navController.navigate("brand_wishlist") },
        onBellClick = { navController.navigate("notifications") }
    )
}

@Composable
fun ActiveCampaignSection(
    collaborations: List<Collaboration>,
    brandViewModel: BrandViewModel,
    brandName: String,
    onCollaborationClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    val activeCollabs = collaborations.filter { it.status == "ACCEPTED" || it.status == "IN_PROGRESS" }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(text = "Active Collaborations", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(brandThemeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeCollabs.size.toString(),
                    color = brandThemeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
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

        if (activeCollabs.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Campaign,
                title = "No active campaigns",
                subtitle = "Invite influencers to your campaigns to see them here."
            )
        } else {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val cardWidth = (screenWidth * 0.85f).coerceIn(280.dp, 320.dp)

            LazyRow(
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(activeCollabs) { collaboration ->
                    val pricing = collaboration.pricing?.firstOrNull()
                    Box(modifier = Modifier.width(cardWidth)) {
                        CollaborationItemBrand(
                            brandName = brandName,
                            brandLogo = null,
                            campaignTitle = collaboration.campaign.title,
                            status = collaboration.status,
                            deliverable = pricing?.deliverable ?: "N/A",
                            platform = pricing?.platform ?: "N/A",
                            price = pricing?.price ?: 0,
                            currency = pricing?.currency ?: "USD",
                            time = "Updated ${formatTime(collaboration.updatedAt)}",
                            onClick = { onCollaborationClick(collaboration.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CollaborationItemBrand(
    brandName: String,
    brandLogo: String?,
    campaignTitle: String,
    status: String,
    deliverable: String,
    platform: String,
    price: Int,
    currency: String,
    time: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = brandThemeColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = brandName.take(1).uppercase(),
                            color = brandThemeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = brandName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = when (status) {
                        "ACCEPTED" -> Color(0xFFE8F5E9)
                        "IN_PROGRESS" -> Color(0xFFE3F2FD)
                        else -> Color(0xFFF5F5F5)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = status.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (status) {
                            "ACCEPTED" -> Color(0xFF2E7D32)
                            "IN_PROGRESS" -> Color(0xFF1976D2)
                            else -> Color.Gray
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = campaignTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$deliverable on $platform",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$currency $price",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = brandThemeColor
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
        }
    }
}

private fun formatTime(updatedAt: String?): String {
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

@Composable
fun TopPicksSectionBrand(
    overallTopInfluencers: List<InfluencerProfile>,
    youtubeTopInfluencers: List<InfluencerProfile>,
    instagramTopInfluencers: List<InfluencerProfile>,
    wishlistedInfluencers: List<InfluencerProfile>,
    onWishlistToggle: (InfluencerProfile) -> Unit,
    navController: NavController,
    brandCategories: List<String>
) {
    var selectedPlatform by remember { mutableStateOf("All") }
    val platforms = listOf("All", "YouTube", "Instagram")

    val influencersToShow = when (selectedPlatform) {
        "YouTube" -> youtubeTopInfluencers
        "Instagram" -> instagramTopInfluencers
        else -> overallTopInfluencers
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
        Text(
            text = "Influencer Picks for You",
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
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[platforms.indexOf(selectedPlatform)]),
                        color = brandThemeColor
                    )
                }
            ) {
                platforms.forEach { platform ->
                    Tab(
                        selected = selectedPlatform == platform,
                        onClick = { selectedPlatform = platform },
                        text = { Text(text = platform, fontWeight = FontWeight.Medium) },
                        icon = {
                            if (platform == "All") {
                                Icon(Icons.Default.Star, contentDescription = null)
                            } else {
                                Image(
                                    painter = painterResource(id = if (platform == "YouTube") R.drawable.youtube_logo else R.drawable.instagram_logo),
                                    contentDescription = platform,
                                    modifier = Modifier.size(20.dp).alpha(if (selectedPlatform == platform) 1f else 0.5f)
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (influencersToShow.isEmpty()) {
            EmptyState(
                icon = Icons.Default.PersonSearch,
                title = "No recommendations yet",
                subtitle = "Complete your brand profile to get personalized picks."
            )
        } else {
            influencersToShow.take(10).forEach { influencer ->
                BrandCardBrand(
                    influencer = influencer,
                    isWishlisted = wishlistedInfluencers.any { it.id == influencer.id },
                    onWishlistToggle = { onWishlistToggle(influencer) },
                    onCardClick = { navController.navigate("brand_influencer_detail/${influencer.id}") },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            Button(
                onClick = { navController.navigate("brand_search") },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = brandThemeColor)
            ) {
                Text("Explore More Influencers")
            }
        }
    }
}

@Composable
fun SpendBreakdownSection(collaborations: List<Collaboration>) {
    var selectedPeriod by remember { mutableStateOf(SpendBucketPeriod.MONTHLY) }
    val buckets = remember(collaborations, selectedPeriod) { 
        computeBrandSpendBuckets(collaborations, selectedPeriod) 
    }
    val totalAmount = remember(buckets) { buckets.sumOf { it.amount } }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Spend Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(
                            text = when(selectedPeriod) {
                                SpendBucketPeriod.WEEKLY -> "Last 8 Weeks"
                                SpendBucketPeriod.MONTHLY -> "Last 6 Months"
                                SpendBucketPeriod.YEARLY -> "Last 5 Years"
                            },
                            color = brandThemeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = brandThemeColor)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        SpendBucketPeriod.values().forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedPeriod = period
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "₹${formatCompactCount(totalAmount.toInt())}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
            
            Text(
                text = "Total spend in the selected period",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}
