package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.*
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar

@Composable
fun BrandSearchPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedFollowerRange by remember { mutableStateOf("All") }

    val influencers by brandViewModel.influencers.observeAsState(initial = emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val firebaseToken = result.token
            if (firebaseToken != null) {
                brandViewModel.fetchInfluencers(firebaseToken)
            }
        }
    }

    val filteredInfluencers = influencers.filter { influencer ->
        val matchesSearch = influencer.name.contains(searchQuery, ignoreCase = true) ||
                influencer.bio?.contains(searchQuery, ignoreCase = true) == true ||
                influencer.categories?.any { it.category.contains(searchQuery, ignoreCase = true) } == true

        val matchesPlatform = selectedPlatform == "All" ||
                influencer.platforms?.any { it.platform.equals(selectedPlatform, ignoreCase = true) } == true

        val matchesCategory = selectedCategory == "All" ||
                influencer.categories?.any { it.category.equals(selectedCategory, ignoreCase = true) } == true

        val matchesFollowers = when (selectedFollowerRange) {
            "All" -> true
            "0-10K" -> influencer.platforms?.any { (it.followers ?: 0) < 10000 } == true
            "10K-100K" -> influencer.platforms?.any { (it.followers ?: 0) in 10000..100000 } == true
            "100K-1M" -> influencer.platforms?.any { (it.followers ?: 0) in 100000..1000000 } == true
            "1M+" -> influencer.platforms?.any { (it.followers ?: 0) > 1000000 } == true
            else -> true
        }

        matchesSearch && matchesPlatform && matchesCategory && matchesFollowers
    }

    BrandSearchPageContent(
        modifier = modifier,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        selectedPlatform = selectedPlatform,
        onPlatformSelected = { selectedPlatform = it },
        selectedCategory = selectedCategory,
        onCategorySelected = { selectedCategory = it },
        selectedFollowerRange = selectedFollowerRange,
        onFollowerRangeSelected = { selectedFollowerRange = it },
        filteredInfluencers = filteredInfluencers,
        isLoading = isLoading,
        onBackClick = { navController.popBackStack() },
        onInfluencerClick = { id -> navController.navigate("brand_influencer_detail/$id") },
        onCreateCampaignClick = { navController.navigate("create_campaign") },
        navController = navController
    )
}

@Composable
fun BrandSearchPageContent(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    selectedFollowerRange: String,
    onFollowerRangeSelected: (String) -> Unit,
    filteredInfluencers: List<InfluencerProfile>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onInfluencerClick: (String) -> Unit,
    onCreateCampaignClick: () -> Unit,
    navController: NavController
) {

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "Search",
                onItemSelected = { /* Handled in the component already */ },
                navController = navController,
                isBrand = true
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateCampaignClick,
                containerColor = Color(0xFFFF8383),
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ---------------- REFINED HEADER ----------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                    .background(Color(0xFFFF8383))
            ) {
                // Wavy background pattern
                Image(
                    painter = painterResource(id = R.drawable.vector),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.2f),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onBackClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Back",
                                    tint = Color.Black
                                )
                            }
                        }

                        Row {
                            IconBubbleSearch(Icons.Default.Favorite, Color.Red)
                            Spacer(modifier = Modifier.width(10.dp))
                            IconBubbleSearch(Icons.Default.Notifications, Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Discover",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Find influencers for your next campaign",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Search Bar
                    TextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(28.dp)),
                        placeholder = { Text("Search", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        shape = RoundedCornerShape(28.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFFFF8383)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // ---------------- FILTERS SECTION ----------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterDropdown(
                    label = "Platform",
                    selectedOption = selectedPlatform,
                    options = listOf("All", "INSTAGRAM", "YOUTUBE", "FACEBOOK", "TIKTOK"),
                    onOptionSelected = onPlatformSelected,
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Category",
                    selectedOption = selectedCategory,
                    options = listOf("All", "Tech", "Fashion", "Food", "Lifestyle", "Beauty", "Sports"),
                    onOptionSelected = onCategorySelected,
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Followers",
                    selectedOption = selectedFollowerRange,
                    options = listOf("All", "0-10K", "10K-100K", "100K-1M", "1M+"),
                    onOptionSelected = onFollowerRangeSelected,
                    modifier = Modifier.weight(1f)
                )

                // Filter Settings Icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFF8383).copy(alpha = 0.8f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = { /* Open detailed filters */ }) {
                            Icon(Icons.Default.Tune, contentDescription = "Filters", tint = Color.White)
                        }
                    }
                }
            }

            // ---------------- RESULTS SECTION ----------------
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF8383))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredInfluencers) { influencer ->
                        BrandCardBrand(
                            influencer = influencer,
                            modifier = Modifier.fillMaxWidth(),
                            onCardClick = { onInfluencerClick(influencer.id) }
                        )
                    }
                    if (filteredInfluencers.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No influencers found.", color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFEAEA), // Light pinkish for the chip
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedOption == "All") label else selectedOption,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.Black
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun IconBubbleSearch(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BrandSearchPagePreview() {
    val sampleInfluencer = InfluencerProfile(
        id = "1",
        email = "test@example.com",
        name = "Karthick Gopinath",
        role = "INFLUENCER",
        profileCompleted = true,
        updatedAt = "2024-03-20T10:00:00Z",
        bio = "Tech Enthusiast & Content Creator",
        location = "Chennai, India",
        categories = listOf(Category("Tech", "Gadgets")),
        platforms = listOf(
            Platform(
                platform = "YOUTUBE",
                profileUrl = "https://youtube.com/c/test",
                followers = 1500000,
                avgViews = 500000,
                engagement = 4.5f,
                formats = null,
                connected = true
            )
        ),
        audienceInsights = null,
        strengths = listOf("Product Reviews", "Educational Content"),
        pricing = listOf(
            PricingInfo(
                platform = "YOUTUBE",
                deliverable = "Video",
                price = 50000,
                currency = "INR"
            )
        ),
        availability = true,
        logoUrl = null
    )

    BrandSearchPageContent(
        searchQuery = "",
        onSearchQueryChange = {},
        selectedPlatform = "All",
        onPlatformSelected = {},
        selectedCategory = "All",
        onCategorySelected = {},
        selectedFollowerRange = "All",
        onFollowerRangeSelected = {},
        filteredInfluencers = listOf(sampleInfluencer),
        isLoading = false,
        onBackClick = {},
        onInfluencerClick = {},
        onCreateCampaignClick = {},
        navController = rememberNavController()
    )
}
