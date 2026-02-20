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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val brandThemeColor = Color(0xFFFF8383) // Reusing the same color variable name from BrandHomePage for consistency

@Composable
fun InfluencerHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    influencerViewModel: InfluencerViewModel,
    campaignViewModel: CampaignViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val collaborations by influencerViewModel.collaborations.observeAsState(initial = emptyList())
    val campaigns by campaignViewModel.campaigns.observeAsState(initial = emptyList())
    val isLoading by influencerViewModel.loading.observeAsState(initial = false)
    val error by influencerViewModel.error.observeAsState()
    val influencerProfile by influencerViewModel.influencerProfile.observeAsState()
    val wishlistedCampaigns by campaignViewModel.wishlistedCampaigns.observeAsState(initial = emptyList())
    var firebaseToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                firebaseToken = result.token
                firebaseToken?.let { token ->
                    influencerViewModel.fetchInfluencerDetails(token)
                    influencerViewModel.fetchCollaborations(token)
                    campaignViewModel.fetchCampaigns(token)
                }
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

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = { selectedBottomNavItem = it },
                navController = navController
            )
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

                item { InfluencerHeaderAndReachSection(influencerProfile, navController) }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        ActiveCollaborationsSection(collaborations)
                        TopPicksSectionInfluencer()
                    }

                }
                items(campaigns.take(10)) { campaign ->
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
fun InfluencerHeaderAndReachSection(influencerProfile: InfluencerProfile?, navController: NavController) {

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
                    if (!influencerProfile?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = influencerProfile?.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.brand_profile), // Fallback image
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
                        "${influencerProfile?.name ?: "Guest"} \ud83d\udc4b",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                IconBubbleInfluencer(
                    icon = Icons.Default.Favorite,
                    tint = Color.Red,
                    onClick = { navController.navigate("wishlist") }
                )
                Spacer(modifier = Modifier.width(10.dp))
                IconBubbleInfluencer(
                    icon = Icons.Default.Notifications,
                    tint = Color.Black,
                    onClick = { /* TODO: Navigate to Notifications */ }
                )
            }
        }

        // ---------------- STATS CARD ----------------
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerHeight - (cardHeight * 0.75f)) // \ud83d\udd25 dynamic overlap
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
                        "Total Earnings",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        "\u20b9 18.4K", // Mock data for now as per previous design, or can map real data if available
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

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

        // ---------------- FLOATING BUTTON ----------------
        Button(
            onClick = { navController.navigate("influencer_search") },
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
                "Find Campaigns",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
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
fun ActiveCollaborationsSection(collaborations: List<Collaboration>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Active Collaborations",
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
                text = "No active collaborations found.",
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
                        CollaborationItem(
                            brandName = collaboration.brand?.name ?: "Brand",
                            brandLogo = collaboration.brand?.logoUrl,
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
    brandLogo: String?
) {
    val primaryColor = Color(0xFF4CAF50)

    val statusColor = when (status) {
        "ACCEPTED" -> primaryColor
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
                        if (!brandLogo.isNullOrEmpty()) {
                            AsyncImage(
                                model = brandLogo,
                                contentDescription = brandName,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (brandName.isNotEmpty()) brandName.first().uppercase() else "B",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = brandName, // Showing Brand Name as main text
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            text = campaignTitle, // Campaign Title as subtext
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
                text = "$deliverable \u2022 $platform",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currency $price",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = primaryColor
                )
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
fun TopPicksSectionInfluencer() {
    var selectedPlatform by remember { mutableStateOf("YouTube") }
    val platforms = listOf("YouTube", "Instagram", "Facebook")

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Top Picks", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
fun CampaignCardInfluencer(
    campaign: CampaignDetail,
    isWishlisted: Boolean = false,
    onWishlistToggle: () -> Unit = {},
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = campaign.brand?.name ?: "Unknown Brand",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = { onWishlistToggle() }) {
                    Icon(
                        imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Wishlist",
                        tint = if (isWishlisted) Color.Red else Color.Gray
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Budget",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${campaign.budgetMin ?: '-'} - ${campaign.budgetMax ?: '-'}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

            }
        }
    }
}
