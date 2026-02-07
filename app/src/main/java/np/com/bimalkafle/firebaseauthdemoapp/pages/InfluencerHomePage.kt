package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme

// Data classes
data class Campaign(
    val id: Int,
    val brandName: String,
    val brandLogo: Int,
    val campaignName: String,
    val progress: Float
)

data class Brand(
    val id: Int,
    val name: String,
    val logo: Int,
    val description: String,
    val isVerified: Boolean = false,
    var isFavorite: Boolean = false
)

// Sample Data with corrected logos
val sampleCampaigns = listOf(
    Campaign(1, "Coca Cola", R.drawable.brand_profile, "Christmas special colab", 0.7f),
    Campaign(2, "Nike", R.drawable.brand_profile, "Christmas special colab", 0.5f),
    Campaign(3, "McDonald's", R.drawable.brand_profile, "Christmas special colab", 0.9f)
)

val sampleBrands = listOf(
    Brand(1, "COCO COLA", R.drawable.brand_profile, "Tech colab | chennai", isVerified = true),
    Brand(2, "NIKE", R.drawable.brand_profile, "Tech colab | US", isVerified = true),
    Brand(3, "McDonald's", R.drawable.brand_profile, "Top Creator | Quick Response"),
    Brand(4, "Starbucks", R.drawable.brand_profile, "Top Creator"),
)

// Color Theme from InfluencerRegistrationScreen
val themeColor = Color(0xFFFF8383)


@Composable
fun InfluencerHomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val authState = authViewModel.authState.observeAsState()
    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo("influencer_home") { inclusive = true }
            }
        }
    }

    InfluencerHomePageContent(
        modifier = modifier,
        onSignOut = { authViewModel.signout() },
        onCreateProposal = { navController.navigate("influencer_create_proposal") }
    )
}

@Composable
fun InfluencerHomePageContent(
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit,
    onCreateProposal: () -> Unit
) {
    var selectedBottomNavItem by remember { mutableStateOf("Home") }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.3f
    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = { selectedBottomNavItem = it }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateProposal,
                containerColor = themeColor,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Proposal", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            item {
                HeaderAndReachSection()
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-40).dp) // Overlap effect
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(Color.White)
                        .padding(top = 16.dp)
                ) {
                    ActiveCampaignsSection(sampleCampaigns)
                    TopPicksSection()
                }
            }

            items(sampleBrands.chunked(2)) { brandRow ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    brandRow.forEach { brand ->
                        BrandCard(brand = brand, modifier = Modifier.weight(1f))
                    }
                    if (brandRow.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp).background(Color.White))
            }
        }
    }
}

@Composable
fun HeaderAndReachSection() {

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.5f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .background(color = themeColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.2f),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_profile),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hello!", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                    Text("A2D NANDA", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = "Favorites", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Total Reach Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Reach", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("2.4 M", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChip("Engagement", "6.1%")
                        StatChip("Leads", "1.2 K")
                        StatChip("Earnings", "₹18.4K")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Find Brands Button
            Button(
                onClick = { /*TODO*/ },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(50.dp)
            ) {
                Text("Find Brands", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(40.dp)) // To create space for the overlap
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun ActiveCampaignsSection(campaigns: List<Campaign>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Active campaigns", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = campaigns.size.toString(),
                    color = themeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(campaigns) { campaign ->
                CampaignCard(campaign = campaign)
            }
        }
    }
}

@Composable
fun CampaignCard(campaign: Campaign) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.width(280.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = campaign.brandLogo),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(campaign.brandName, fontWeight = FontWeight.Bold)
                Text(campaign.campaignName, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { campaign.progress },
                    color = themeColor,
                    trackColor = themeColor.copy(alpha = 0.2f)
                )
            }
            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        }
    }
}

@Composable
fun TopPicksSection() {
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
                contentColor = themeColor,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(it[platforms.indexOf(selectedPlatform)]),
                        color = themeColor
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
fun BrandCard(brand: Brand, modifier: Modifier = Modifier) {
    var isFavorite by remember { mutableStateOf(brand.isFavorite) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column {
            Box(contentAlignment = Alignment.TopEnd) {
                Image(
                    painter = painterResource(id = brand.logo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(brand.name, fontWeight = FontWeight.Bold)
                    if (brand.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(brand.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedItem: String, onItemSelected: (String) -> Unit) {
    val items = listOf("Home", "Search", "History", "Profile")
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
            items.forEachIndexed { index, item ->
                if (index == 2) {
                    // Placeholder for the FAB
                    NavigationBarItem(
                        selected = false,
                        onClick = { },
                        icon = { },
                        enabled = false
                    )
                }

                NavigationBarItem(
                    icon = { Icon(icons[item]!!, contentDescription = item) },
                    label = { Text(item) },
                    selected = selectedItem == item,
                    onClick = { onItemSelected(item) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = themeColor,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = themeColor,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = themeColor.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun InfluencerHomePagePreview() {
    FirebaseAuthDemoAppTheme {
        InfluencerHomePageContent(
            onSignOut = {},
            onCreateProposal = {}
        )
    }
}
