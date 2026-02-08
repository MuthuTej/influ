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

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val firebaseToken = result.token
                if (firebaseToken != null) {
                    brandViewModel.fetchCollaborations(firebaseToken)
                    brandViewModel.fetchInfluencers(firebaseToken)
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
            BrandBottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = { selectedBottomNavItem = it },
                onCreateCampaign = { navController.navigate("create_campaign") }
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

                item { BrandHeaderAndReachSection() }

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
                items(influencers) { influencer ->
                    BrandCardBrand(
                        influencer = influencer, 
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
                            FirebaseAuth.getInstance().signOut()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun BrandHeaderAndReachSection() {

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
                .background(brandThemeColor)
        ) {

            Image(
                painter = painterResource(id = R.drawable.vector),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.15f),
                contentScale = ContentScale.Crop
            )

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
                    Image(
                        painter = painterResource(id = R.drawable.brand_profile),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Hello!", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                    Text(
                        "Myntra 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconBubble(Icons.Default.Favorite, Color.Red)
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
                            listOf(Color(0xFF4FACFE), Color(0xFF6C63FF))
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
                containerColor = Color(0xFFFF6B6B)
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
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }
    }
}

@Composable
fun IconBubble(icon: ImageVector, tint: Color) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.25f),
        modifier = Modifier.size(42.dp)
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
            containerColor = Color.White.copy(alpha = 0.15f)
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = Color.White.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .aspectRatio(1f)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
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
                    color = Color(0xFF4A2E2E),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = value,
                    color = Color.White,
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
            val singleCardHeight = 150.dp

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
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = if (influencerName.isNotEmpty()) influencerName.first().uppercase() else "?",
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
                                fontSize = 16.sp
                            )
                            Text(
                                text = campaignTitle,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
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

            Surface(
                shape = RoundedCornerShape(25),
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
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
    }
}

@Composable
fun TopPicksSectionBrand() {
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
fun BrandCardBrand(
    influencer: InfluencerProfile, 
    modifier: Modifier = Modifier, 
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { onCardClick() }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            // "Available" tag in top right
            if (influencer.availability == true) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Available",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Column {
                // Top Section: Profile info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = brandThemeColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        if (!influencer.logoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = influencer.logoUrl,
                                contentDescription = influencer.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
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

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = influencer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = influencer.location ?: "Location N/A",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = influencer.bio ?: "No bio available.",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp,
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val categoriesText = influencer.categories?.joinToString(" • ") { it.category } ?: ""
                        Text(
                            text = categoriesText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = brandThemeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Stats Section
                val mainPlatform = influencer.platforms?.firstOrNull { it.platform == "INSTAGRAM" } 
                    ?: influencer.platforms?.firstOrNull()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (mainPlatform != null) {
                        StatIconText(
                            iconRes = if (mainPlatform.platform == "YOUTUBE") R.drawable.ic_youtube else R.drawable.ic_instagram,
                            text = "${formatCount(mainPlatform.followers ?: 0)} Followers",
                            iconTint = if (mainPlatform.platform == "YOUTUBE") Color.Red else Color(0xFFE4405F)
                        )
                        
                        StatIconText(
                            iconRes = R.drawable.vector, 
                            text = "Avg Views: ${formatCount(mainPlatform.avgViews ?: 0)}",
                            iconTint = Color(0xFF1E3A8A)
                        )
                    }
                }

                // Strengths Section
                if (!influencer.strengths.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Strengths", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF4B5563))
                        Text(" | ", color = Color.LightGray)
                        Text(
                            text = influencer.strengths.joinToString(" | "),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Pricing Section
                if (!influencer.pricing.isNullOrEmpty()) {
                    Text(
                        text = "Pricing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    influencer.pricing.take(2).forEach { pricing ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_instagram),
                                contentDescription = null,
                                tint = Color(0xFFE4405F),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${pricing.deliverable}: ",
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "₹${String.format("%,d", pricing.price)} ${pricing.currency}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatIconText(iconRes: Int, text: String, iconTint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF4B5563)
        )
    }
}

fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${count / 1000000}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

@Composable
fun BrandBottomNavigationBar(selectedItem: String, onItemSelected: (String) -> Unit, onCreateCampaign: () -> Unit) {
    val items = listOf("Home", "Search", "", "History", "Profile")
    val icons = mapOf(
        "Home" to Icons.Default.Home,
        "Search" to Icons.Default.Search,
        "History" to Icons.Default.History,
        "Profile" to Icons.Default.Person
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            items.forEach { item ->
                if (item.isEmpty()) {
                    FloatingActionButton(
                        onClick = onCreateCampaign,
                        containerColor = brandThemeColor,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .offset(y = (-7).dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Campaign", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else {
                    NavigationBarItem(
                        icon = { Icon(icons[item]!!, contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedItem == item,
                        onClick = { onItemSelected(item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = brandThemeColor,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = brandThemeColor,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = brandThemeColor.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BrandHomePagePreview() {
    val sampleCollaborations = listOf(
        Collaboration(
            id = "1",
            status = "ACCEPTED",
            message = "Excited to collaborate!",
            createdAt = "",
            initiatedBy = "BRAND",
            campaign = Campaign(
                id = "c1",
                title = "Summer Launch 2024"
            ),
            pricing = listOf(
                Pricing(
                    currency = "USD",
                    deliverable = "Reel",
                    platform = "INSTAGRAM",
                    price = 600
                )
            ),
            influencer = Influencer(
                name = "testinfluencer",
                bio = null,
                logoUrl = null,
                updatedAt = "2026-02-07T11:35:52.789Z"
            )
        )
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            BrandHeaderAndReachSection()
            ActiveCampaignSection(sampleCollaborations)
            TopPicksSectionBrand()
        }
    }
}