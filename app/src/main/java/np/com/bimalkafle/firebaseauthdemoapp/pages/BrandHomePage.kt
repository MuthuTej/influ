package np.com.bimalkafle.firebaseauthdemoapp.pages

import coil.compose.AsyncImage

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.compose.ui.tooling.preview.Preview
import np.com.bimalkafle.firebaseauthdemoapp.R
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.Influencer
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.model.Pricing
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar

private val brandThemeColor = Color(0xFFFF8383)

@Composable
fun BrandHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel
) {

    val authState = authViewModel.authState.observeAsState()
    val collaborations by brandViewModel.collaborations.observeAsState(initial = emptyList())
    val influencers by brandViewModel.influencers.observeAsState(initial = emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val error by brandViewModel.error.observeAsState()
    val wishlistedInfluencers by brandViewModel.wishlistedInfluencers.observeAsState(initial = emptyList())
    var firebaseToken by remember { mutableStateOf<String?>(null) }

    val brandProfile by brandViewModel.brandProfile.observeAsState()

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                firebaseToken = result.token
                firebaseToken?.let { token ->
                    brandViewModel.fetchCollaborations(token)
                    brandViewModel.fetchInfluencers(token)
                    brandViewModel.fetchBrandDetails(token)
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
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create Campaign",
                    tint = Color.White
                )
            }
        }
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.White)
            ) {

                item { BrandHeaderAndReachSection(brandProfile, navController) }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        ActiveCampaignSection(collaborations)
                        TopPicksSectionBrand()
                    }

                }
                items(influencers.take(10)) { influencer ->
                    BrandCardBrand(
                        influencer = influencer,
                        isWishlisted = wishlistedInfluencers.any { it.id == influencer.id },
                        onWishlistToggle = {
                            firebaseToken?.let { token ->
                                brandViewModel.toggleWishlist(influencer, token)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onCardClick = {
                            navController.navigate("brand_influencer_detail/${influencer.id}")
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = {
                            authViewModel.signout()
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun BrandHeaderAndReachSection(brandProfile: np.com.bimalkafle.firebaseauthdemoapp.model.Brand?, navController: NavController) {

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.32f
    val cardHeight = screenHeight * 0.28f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight + cardHeight * 0.6f) // dynamic total height
    ) {

        // ---------------- HEADER ----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(54.dp)
                ) {
                    Log.d("LOGO_DEBUG", "Logo URL: ${brandProfile?.logoUrl}")
                    if (!brandProfile?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = brandProfile?.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.brand_profile),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Hello!", fontSize = 14.sp, color = Color.Black.copy(alpha = 0.9f))
                    Text(
                        "${brandProfile?.name ?: "Guest"} 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                IconBubble(Icons.Default.Favorite, Color.Red) {
                    navController.navigate("brand_wishlist")
                }
                Spacer(modifier = Modifier.width(10.dp))
                IconBubble(Icons.Default.Notifications, Color.Black)
            }
        }

        // ---------------- STATS CARD ----------------
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerHeight - (cardHeight * 0.75f)) // 🔥 dynamic overlap
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(cardHeight),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 25.dp)
        ) {

            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFAFBD), brandThemeColor)
                        )
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "Total Reach",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        "2.4 M",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        BrandStatChip("Engagement", "6.1 %", Modifier.weight(1f))
                        BrandStatChip("Leads", "1.2 K", Modifier.weight(1f))
                        BrandStatChip("Spent", "18.4K", Modifier.weight(1f))

                    }
                }
            }
        }

        // ---------------- FLOATING BUTTON ----------------
        Button(
            onClick = { },
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5252)
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerHeight + (cardHeight * 0.20f))
                .fillMaxWidth(0.65f)
                .height(52.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text(
                "Find Influencer",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            width = 0.dp,
            color = Color.Transparent
        ),
        modifier = modifier
            .aspectRatio(1f)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 18.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = label,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = value,
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun ActiveCampaignSection(collaborations: List<Collaboration>) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Active Campaigns",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(brandThemeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = collaborations.size.toString(),
                    color = brandThemeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (collaborations.isEmpty()) {
            Text(
                text = "No active campaigns found.",
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            val maxSectionHeight = screenHeight * 0.20f
            val singleCardHeight = 140.dp

            val finalHeight = if (maxSectionHeight > singleCardHeight) {
                maxSectionHeight
            } else {
                singleCardHeight
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(finalHeight)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(collaborations) { collaboration ->

                    val pricing = collaboration.pricing?.firstOrNull()
                    val updatedAt = collaboration.influencer.updatedAt

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
                        } catch (e: Exception) {
                            "Recently"
                        }
                    }

                    val timeAgo = calculateDaysAgo(updatedAt)

                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    ) {
                        CampaignItem(
                            influencerName = collaboration.influencer.name,
                            influencerLogo = collaboration.influencer.logoUrl,
                            campaignTitle = collaboration.campaign.title,
                            status = collaboration.status,
                            deliverable = pricing?.deliverable ?: "N/A",
                            platform = pricing?.platform ?: "N/A",
                            price = pricing?.price ?: 0,
                            currency = pricing?.currency ?: "USD",
                            time = timeAgo
                        )
                    }
                }
            }
        }
    }
}

// Data class moved/used from model, or mapped directly.
// We are using the domain model directly in the composable for simplicity.

@Composable
fun CampaignItem(
    influencerName: String,
    campaignTitle: String,
    status: String,
    deliverable: String,
    platform: String,
    price: Int,
    currency: String,
    time: String,
    influencerLogo: String?
) {
    val primaryColor = brandThemeColor

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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                         if (!influencerLogo.isNullOrEmpty()) {
                            AsyncImage(
                                model = influencerLogo,
                                contentDescription = influencerName,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (influencerName.isNotEmpty()) influencerName.first().uppercase() else "I",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = influencerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            text = campaignTitle,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(25),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            Text(
                text = "$deliverable • $platform",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$currency $price",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = primaryColor
                    )


                }
                Text(
                    text = time,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun TopPicksSectionBrand() {
    var selectedPlatform by remember { mutableStateOf("YouTube") }
    val platforms = listOf("YouTube", "Instagram", "Facebook")

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Top Influencers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            TabRow(
                selectedTabIndex = platforms.indexOf(selectedPlatform),
                containerColor = Color.Transparent,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(it[platforms.indexOf(selectedPlatform)]),
                        color = brandThemeColor
                    )
                }
            ) {
                platforms.forEach { platform ->
                    val iconRes = when (platform) {
                        "YouTube" -> R.drawable.ic_youtube
                        "Instagram" -> R.drawable.ic_instagram
                        "Facebook" -> R.drawable.ic_facebook
                        else -> R.drawable.ic_youtube
                    }
                    Tab(
                        selected = selectedPlatform == platform,
                        onClick = { selectedPlatform = platform },
                        icon = {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = platform,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BrandCardBrand(
    influencer: InfluencerProfile,
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
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Profile Image
                Surface(
                    shape = CircleShape,
                    color = brandThemeColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(64.dp)
                ) {
                    if (!influencer.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = influencer.logoUrl,
                            contentDescription = influencer.name,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = influencer.name.firstOrNull()?.uppercase() ?: "?",
                                color = brandThemeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name, Location, Bio, Category
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = influencer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        if (influencer.isVerified == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (influencer.averageRating != null) {
                            Spacer(modifier = Modifier.weight(1f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format("%.1f", influencer.averageRating),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    Text(
                        text = influencer.location ?: "Unknown Location",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = influencer.bio ?: "No bio available",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = influencer.categories?.firstOrNull()?.category ?: "General",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = brandThemeColor
                    )
                }

                // Heart Icon and Available tag
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(
                        onClick = { onWishlistToggle() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) Color.Red else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp)) // Increased spacing to move Available tag lower
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Available",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            // Strengths Chips
            influencer.strengths?.let { strengths ->
                if (strengths.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        strengths.take(3).forEach { strength ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE3F2FD),
                                border = BorderStroke(1.dp, Color(0xFF90CAF9))
                            ) {
                                Text(
                                    text = strength,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Followers
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    val firstPlatform = influencer.platforms?.firstOrNull()
                    val platformIcon = when(firstPlatform?.platform?.uppercase()) {
                        "YOUTUBE" -> R.drawable.ic_youtube
                        "INSTAGRAM" -> R.drawable.ic_instagram
                        "FACEBOOK" -> R.drawable.ic_facebook
                        else -> R.drawable.ic_instagram 
                    }

                    Surface(
                        shape = CircleShape,
                        color = brandThemeColor,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = platformIcon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatCount(firstPlatform?.followers ?: 0)} Followers",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Avg Views
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF5C6BC0),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Avg Views: ${formatCount(influencer.platforms?.firstOrNull()?.avgViews ?: 0)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Pricing Section
            Text(
                text = "Pricing",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            influencer.pricing?.forEach { pricingInfo ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val platformIcon = when(pricingInfo.platform.uppercase()) {
                        "YOUTUBE" -> R.drawable.ic_youtube
                        "INSTAGRAM" -> R.drawable.ic_instagram
                        "FACEBOOK" -> R.drawable.ic_facebook
                        else -> R.drawable.ic_youtube
                    }
                    
                    Surface(
                        shape = CircleShape,
                        color = brandThemeColor,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = platformIcon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${pricingInfo.deliverable}: ₹${pricingInfo.price} ${pricingInfo.currency}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
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
