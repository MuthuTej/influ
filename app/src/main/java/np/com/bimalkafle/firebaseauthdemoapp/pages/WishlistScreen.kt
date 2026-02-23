package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(navController: NavController, campaignViewModel: CampaignViewModel) {
    val themeColor = Color(0xFFFF8383)
    var selectedBottomNavItem by remember { mutableStateOf("History") }
    val wishlistedCampaigns by campaignViewModel.wishlistedCampaigns.observeAsState(initial = emptyList())
    var firebaseToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                firebaseToken = result.token
                firebaseToken?.let { token ->
                    campaignViewModel.fetchWishlist(token)
                }
            }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColor)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vector),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.2f),
                    contentScale = ContentScale.Crop
                )
                Column {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                                Text("Wishlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            IconButton(onClick = { /*TODO: Implement notification logic*/ }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Saved campaigns you're interested in collaborating with",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { /*TODO*/ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Saved ${wishlistedCampaigns.size}", color = themeColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        bottomBar = {
            val items = listOf("Home", "Search", "", "History", "Profile")
            val icons = mapOf(
                "Home" to Icons.Default.Home,
                "Search" to Icons.Default.Search,
                "History" to Icons.Default.History,
                "Profile" to Icons.Default.Person
            )

            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                items.forEach { item ->
                    if (item.isEmpty()) {
                        FloatingActionButton(
                            onClick = { /* Navigate to create proposal or similar */ },
                            containerColor = themeColor,
                            shape = CircleShape,
                            modifier = Modifier.offset(y = (-16).dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    } else {
                        NavigationBarItem(
                            icon = { Icon(icons[item]!!, contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedBottomNavItem == item,
                            onClick = { 
                                selectedBottomNavItem = item 
                                if (item == "Home") {
                                    navController.navigate("influencer_home")
                                } else if (item == "Search") {
                                    navController.navigate("discover")
                                } else if (item == "Profile") {
                                    navController.navigate("influencerProfile")
                                }
                            },
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
    ) { paddingValues ->
        if (wishlistedCampaigns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No wishlisted campaigns yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(wishlistedCampaigns) { campaign ->
                    CampaignCardInfluencer(
                        campaign = campaign,
                        isWishlisted = true,
                        onWishlistToggle = { 
                            firebaseToken?.let { token ->
                                campaignViewModel.toggleWishlist(campaign, token)
                            }
                        },
                        onCardClick = {
                            navController.navigate("campaign_detail/${campaign.id}")
                        }
                    )
                }
            }
        }
    }
}
