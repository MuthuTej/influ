package np.com.bimalkafle.firebaseauthdemoapp.pages

import coil.compose.AsyncImage
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
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel
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
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar

private val brandThemeColor = Color(0xFFFF8383)

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
    val influencers by brandViewModel.influencers.observeAsState(initial = emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val errorMsg by brandViewModel.error.observeAsState()
    val wishlistedInfluencers by brandViewModel.wishlistedInfluencers.observeAsState(initial = emptyList())
    var firebaseToken by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)
    val brandProfile by brandViewModel.brandProfile.observeAsState()

    LaunchedEffect(errorMsg) {
        errorMsg?.let {
            if (it.isNotEmpty()) {
                Toast.makeText(context, "Server Error: $it", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                firebaseToken = result.token
                firebaseToken?.let { token ->
                    brandViewModel.fetchCollaborations(token)
                    brandViewModel.fetchInfluencers(token)
                    brandViewModel.fetchBrandDetails(token)
                    notificationViewModel.fetchNotifications(currentUser.uid, token)
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
                selectedItem = selectedBottomNavItem,
                onItemSelected = { selectedBottomNavItem = it },
                navController = navController,
                isBrand = true
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_campaign") },
                containerColor = brandThemeColor,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Campaign", tint = Color.White)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        if (isLoading) {
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
                item { BrandHeaderAndReachSection(brandProfile, navController, unreadCount) }
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
                            influencers = influencers,
                            wishlistedInfluencers = wishlistedInfluencers,
                            onWishlistToggle = { influencer ->
                                firebaseToken?.let { token ->
                                    brandViewModel.toggleWishlist(influencer, token)
                                }
                            },
                            navController = navController
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = { authViewModel.signout() },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandThemeColor)
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun BrandHeaderAndReachSection(brandProfile: np.com.bimalkafle.firebaseauthdemoapp.model.Brand?, navController: NavController, unreadCount: Int) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.35f
    val cardHeight = screenHeight * 0.28f

    Box(modifier = Modifier.fillMaxWidth().height(headerHeight + cardHeight * 0.75f)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(brandThemeColor)
                .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.vector),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.15f),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(54.dp)
                ) {
                    if (!brandProfile?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = brandProfile?.logoUrl,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hello!", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                    Text(
                        brandProfile?.name ?: "Guest",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconBubble(Icons.Default.Campaign, Color.Blue) { navController.navigate("all_campaigns") }
                Spacer(modifier = Modifier.width(10.dp))
                IconBubble(Icons.Default.Favorite, Color.Red) { navController.navigate("brand_wishlist") }
                Spacer(modifier = Modifier.width(10.dp))
                Box {
                    IconBubble(Icons.Default.Notifications, Color.Black) { navController.navigate("notifications") }
                    if (unreadCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text(if (unreadCount > 9) "9+" else unreadCount.toString(), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerHeight - (cardHeight * 0.50f))
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(cardHeight),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 25.dp)
        ) {
            Box(modifier = Modifier.background(brush = Brush.verticalGradient(listOf(Color(0xFFFFAFBD), brandThemeColor)))) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Reach", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("2.4 M", fontSize = 50.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrandStatChip("Engagement", "6.1 %", Modifier.weight(1f))
                        BrandStatChip("Leads", "1.2 K", Modifier.weight(1f))
                        BrandStatChip("Spent", "18.4K", Modifier.weight(1f))
                    }
                }
            }
        }
        Button(
            onClick = { navController.navigate("brand_search") },
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerHeight + (cardHeight * 0.40f))
                .fillMaxWidth(0.65f)
                .height(52.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text("Find Influencer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun IconBubble(icon: ImageVector, tint: Color, onClick: () -> Unit = {}) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFF5F5F5),
        modifier = Modifier.size(42.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun BrandStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveCampaignSection(
    collaborations: List<Collaboration>,
    brandViewModel: BrandViewModel,
    brandName: String,
    onCollaborationClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Active Campaigns", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(brandThemeColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(collaborations.size.toString(), color = brandThemeColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        Spacer(modifier = Modifier.height(8.dp))
        if (collaborations.isEmpty()) {
            Text("No active campaigns found.", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.Gray, modifier = Modifier.padding(16.dp))
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
                    val updatedAt = collaboration.influencer.updatedAt
                    fun calculateDaysAgo(updatedAt: String?): String {
                        return try {
                            val instant = Instant.parse(updatedAt)
                            val updatedDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                            val days = ChronoUnit.DAYS.between(updatedDate, LocalDateTime.now().toLocalDate())
                            when {
                                days == 0L -> "Today"
                                days == 1L -> "1 day ago"
                                else -> "$days days ago"
                            }
                        } catch (e: Exception) { "Recently" }
                    }
                    Box(modifier = Modifier.width(cardWidth)) {
                        CampaignItem(
                            collaborationId = collaboration.id,
                            influencerName = collaboration.influencer.name,
                            influencerLogo = collaboration.influencer.logoUrl,
                            campaignTitle = collaboration.campaign.title,
                            status = collaboration.status,
                            deliverable = pricing?.deliverable ?: "N/A",
                            platform = pricing?.platform ?: "N/A",
                            price = pricing?.price ?: 0,
                            currency = pricing?.currency ?: "USD",
                            time = calculateDaysAgo(updatedAt),
                            brandViewModel = brandViewModel,
                            brandName = brandName,
                            onClick = { onCollaborationClick(collaboration.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CampaignItem(
    collaborationId: String,
    influencerName: String,
    campaignTitle: String,
    status: String,
    deliverable: String,
    platform: String,
    price: Int,
    currency: String,
    time: String,
    influencerLogo: String?,
    brandViewModel: BrandViewModel,
    brandName: String,
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
                    Surface(shape = CircleShape, color = brandThemeColor.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                         if (!influencerLogo.isNullOrEmpty()) {
                            AsyncImage(model = influencerLogo, contentDescription = influencerName, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        } else {
                            Box(contentAlignment = Alignment.Center) { Text(text = influencerName.firstOrNull()?.uppercase() ?: "I", color = brandThemeColor, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = influencerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = campaignTitle, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(shape = RoundedCornerShape(25), color = statusColor.copy(alpha = 0.15f)) {
                    Text(text = status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = statusColor)
                }
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "$deliverable • $platform", fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "$currency $price", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = brandThemeColor)
                }
                Text(text = time, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun TopPicksSectionBrand(
    influencers: List<InfluencerProfile>,
    wishlistedInfluencers: List<InfluencerProfile>,
    onWishlistToggle: (InfluencerProfile) -> Unit,
    navController: NavController
) {
    var selectedPlatform by remember { mutableStateOf("YouTube") }
    val platforms = listOf("YouTube", "Instagram", "Facebook")
    val filteredInfluencers = remember(selectedPlatform, influencers) {
        influencers.filter { influencer -> influencer.platforms?.any { it.platform.equals(selectedPlatform, ignoreCase = true) } == true }.take(10)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Top Influencers", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
            TabRow(selectedTabIndex = platforms.indexOf(selectedPlatform), containerColor = Color.Transparent, indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(modifier = Modifier.tabIndicatorOffset(tabPositions[platforms.indexOf(selectedPlatform)]), color = brandThemeColor)
                }
            ) {
                platforms.forEach { platform ->
                    val iconRes = when (platform) {
                        "YouTube" -> R.drawable.ic_youtube
                        "Instagram" -> R.drawable.ic_instagram
                        "Facebook" -> R.drawable.ic_facebook
                        else -> R.drawable.ic_youtube
                    }
                    Tab(selected = selectedPlatform == platform, onClick = { selectedPlatform = platform }, icon = { Icon(painter = painterResource(id = iconRes), contentDescription = platform, modifier = Modifier.size(24.dp)) })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (filteredInfluencers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("No influencers found for $selectedPlatform", color = Color.Gray) }
        } else {
            filteredInfluencers.forEach { influencer ->
                BrandCardBrand(
                    influencer = influencer,
                    isWishlisted = wishlistedInfluencers.any { it.id == influencer.id },
                    onWishlistToggle = { onWishlistToggle(influencer) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    onCardClick = { navController.navigate("brand_influencer_detail/${influencer.id}") },
                    selectedPlatform = selectedPlatform
                )
            }
        }
    }
}

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
            
            // Strengths using FlowRow for responsiveness - UPDATED COLORS
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
                    val platformIcon = when(platformToShow?.platform?.uppercase()) {
                        "YOUTUBE" -> R.drawable.ic_youtube
                        "INSTAGRAM" -> R.drawable.ic_instagram
                        "FACEBOOK" -> R.drawable.ic_facebook
                        else -> R.drawable.ic_instagram 
                    }
                    Surface(shape = CircleShape, color = brandThemeColor, modifier = Modifier.size(24.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(painter = painterResource(id = platformIcon), contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${formatCount(platformToShow?.followers ?: 0)} Followers", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF5C6BC0), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Avg: ${formatCount(platformToShow?.avgViews ?: 0)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
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

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${String.format("%.1f", count / 1000000.0)}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}
