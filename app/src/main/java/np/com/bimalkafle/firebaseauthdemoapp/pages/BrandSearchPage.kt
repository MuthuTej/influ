package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import np.com.bimalkafle.firebaseauthdemoapp.components.FilterDropdown
import np.com.bimalkafle.firebaseauthdemoapp.components.IconBubbleSearch
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel

@Composable
fun BrandSearchPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel,
    notificationViewModel: NotificationViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedFollowerRange by remember { mutableStateOf("All") }

    val influencers by brandViewModel.influencers.observeAsState(initial = emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val wishlistedInfluencers by brandViewModel.wishlistedInfluencers.observeAsState(initial = emptyList())
    var firebaseToken by remember { mutableStateOf<String?>(null) }
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            firebaseToken = result.token
            if (firebaseToken != null) {
                brandViewModel.fetchInfluencers(firebaseToken!!)
                notificationViewModel.fetchUnreadCount(currentUser.uid, firebaseToken!!)
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
        allInfluencers = influencers,
        isLoading = isLoading,
        onBackClick = { navController.popBackStack() },
        onInfluencerClick = { id -> navController.navigate("brand_influencer_detail/$id") },
        onCreateCampaignClick = { navController.navigate("create_campaign") },
        navController = navController,
        wishlistedInfluencers = wishlistedInfluencers,
        unreadCount = unreadCount,
        onWishlistToggle = { influencer ->
            firebaseToken?.let { token ->
                brandViewModel.toggleWishlist(influencer, token)
            }
        }
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
    allInfluencers: List<InfluencerProfile>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onInfluencerClick: (String) -> Unit,
    onCreateCampaignClick: () -> Unit,
    navController: NavController,
    unreadCount: Int,
    wishlistedInfluencers: List<InfluencerProfile> = emptyList(),
    onWishlistToggle: (InfluencerProfile) -> Unit = {}
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
                            IconBubbleSearch(
                                icon = Icons.Default.Favorite,
                                tint = Color.Red,
                                onClick = { navController.navigate("brand_wishlist") }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box {
                                IconBubbleSearch(
                                    icon = Icons.Default.Notifications, 
                                    tint = Color.Black,
                                    onClick = { navController.navigate("notifications") }
                                )
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
                // Calculate counts for Categories
                val categories = listOf("All", "Tech", "Fashion", "Food", "Lifestyle", "Beauty", "Sports")
                val categoryOptions = categories.map { category ->
                    val count = if (category == "All") {
                        allInfluencers.size
                    } else {
                        allInfluencers.count { influencer ->
                            influencer.categories?.any { it.category.equals(category, ignoreCase = true) } == true
                        }
                    }
                    category to count
                }

                // Calculate counts for Platforms
                val platforms = listOf("All", "INSTAGRAM", "YOUTUBE", "FACEBOOK", "TIKTOK")
                val platformOptions = platforms.map { platform ->
                    val count = if (platform == "All") {
                         allInfluencers.size
                    } else {
                        allInfluencers.count { influencer ->
                             influencer.platforms?.any { it.platform.equals(platform, ignoreCase = true) } == true
                        }
                    }
                    platform to count
                }
                
                // Calculate counts for Followers
                val followerRanges = listOf("All", "0-10K", "10K-100K", "100K-1M", "1M+")
                val followerOptions = followerRanges.map { range ->
                    val count = when (range) {
                        "All" -> allInfluencers.size
                        "0-10K" -> allInfluencers.count { inf -> inf.platforms?.any { (it.followers ?: 0) < 10000 } == true }
                        "10K-100K" -> allInfluencers.count { inf -> inf.platforms?.any { (it.followers ?: 0) in 10000..100000 } == true }
                        "100K-1M" -> allInfluencers.count { inf -> inf.platforms?.any { (it.followers ?: 0) in 100000..1000000 } == true }
                        "1M+" -> allInfluencers.count { inf -> inf.platforms?.any { (it.followers ?: 0) > 1000000 } == true }
                        else -> 0
                    }
                    range to count
                }

                FilterDropdown(
                    label = "Platform",
                    selectedOption = selectedPlatform,
                    options = platformOptions,
                    onOptionSelected = onPlatformSelected,
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Category",
                    selectedOption = selectedCategory,
                    options = categoryOptions,
                    onOptionSelected = onCategorySelected,
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Followers",
                    selectedOption = selectedFollowerRange,
                    options = followerOptions,
                    onOptionSelected = onFollowerRangeSelected,
                    modifier = Modifier.weight(1f)
                )
            }

            // Results count and header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredInfluencers.size} Influencers Found",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )

                // Optional: Clear Filters Button
                if (selectedPlatform != "All" || selectedCategory != "All" || selectedFollowerRange != "All" || searchQuery.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        color = Color(0xFFFF8383),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            onPlatformSelected("All")
                            onCategorySelected("All")
                            onFollowerRangeSelected("All")
                            onSearchQueryChange("")
                        }
                    )
                }
            }

            // ---------------- PAGINATION LOGIC ----------------
            var currentPage by remember { mutableStateOf(1) }
            val itemsPerPage = 10 // Increased for better UX
            val totalPages = (filteredInfluencers.size + itemsPerPage - 1) / itemsPerPage
            
            LaunchedEffect(filteredInfluencers) {
                currentPage = 1
            }

            val paginatedInfluencers = filteredInfluencers
                .drop((currentPage - 1) * itemsPerPage)
                .take(itemsPerPage)

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
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(paginatedInfluencers) { influencer ->
                        BrandCardBrand(
                            influencer = influencer,
                            isWishlisted = wishlistedInfluencers.any { it.id == influencer.id },
                            onWishlistToggle = { onWishlistToggle(influencer) },
                            modifier = Modifier.fillMaxWidth(),
                            onCardClick = { onInfluencerClick(influencer.id) }
                        )
                    }
                    if (filteredInfluencers.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No influencers match your criteria.", color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else if (totalPages > 1) {
                         item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { if (currentPage > 1) currentPage-- },
                                    enabled = currentPage > 1,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
                                ) {
                                    Text("Previous")
                                }

                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = "$currentPage / $totalPages", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(16.dp))

                                Button(
                                    onClick = { if (currentPage < totalPages) currentPage++ },
                                    enabled = currentPage < totalPages,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
                                ) {
                                    Text("Next")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
