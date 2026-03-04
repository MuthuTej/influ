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
    var currentPage by remember { mutableStateOf(1) }

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
        onSearchQueryChange = { 
            searchQuery = it
            currentPage = 1
        },
        selectedPlatform = selectedPlatform,
        onPlatformSelected = { 
            selectedPlatform = it
            currentPage = 1
        },
        selectedCategory = selectedCategory,
        onCategorySelected = { 
            selectedCategory = it
            currentPage = 1
        },
        selectedFollowerRange = selectedFollowerRange,
        onFollowerRangeSelected = { 
            selectedFollowerRange = it
            currentPage = 1
        },
        filteredInfluencers = filteredInfluencers,
        allInfluencers = influencers,
        isLoading = isLoading,
        onBackClick = { navController.popBackStack() },
        onInfluencerClick = { id -> navController.navigate("brand_influencer_detail/$id") },
        onCreateCampaignClick = { navController.navigate("create_campaign") },
        navController = navController,
        wishlistedInfluencers = wishlistedInfluencers,
        unreadCount = unreadCount,
        currentPage = currentPage,
        onPageChange = { currentPage = it },
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
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    wishlistedInfluencers: List<InfluencerProfile> = emptyList(),
    onWishlistToggle: (InfluencerProfile) -> Unit = {}
) {
    val itemsPerPage = 10
    val totalPages = (filteredInfluencers.size + itemsPerPage - 1).coerceAtLeast(0) / itemsPerPage.coerceAtLeast(1)
    val paginatedInfluencers = filteredInfluencers
        .drop((currentPage - 1) * itemsPerPage)
        .take(itemsPerPage)

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "Search",
                onItemSelected = { /* Handled in component */ },
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
                Icon(Icons.Default.Add, contentDescription = "Create Campaign", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(Color(0xFFF8F9FE)),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ---------------- REFINED HEADER ----------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                        .background(Color(0xFFFF8383))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vector),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().alpha(0.2f),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.padding(24.dp).padding(top = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(40.dp).clickable { onBackClick() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = Color.Black)
                                }
                            }

                            Row {
                                IconBubbleSearch(Icons.Default.Favorite, Color.Red) { navController.navigate("brand_wishlist") }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box {
                                    IconBubbleSearch(Icons.Default.Notifications, Color.Black) { navController.navigate("notifications") }
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
                        Text("Discover", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Find influencers for your next campaign", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(20.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(28.dp)),
                            placeholder = { Text("Search influencers...", color = Color.Gray) },
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
            }

            // ---------------- FILTERS SECTION ----------------
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categories = listOf("All", "Tech", "Fashion", "Food", "Lifestyle", "Beauty", "Sports")
                        val categoryOptions = categories.map { cat -> cat to if (cat == "All") allInfluencers.size else allInfluencers.count { it.categories?.any { c -> c.category.equals(cat, ignoreCase = true) } == true } }

                        val platforms = listOf("All", "INSTAGRAM", "YOUTUBE", "FACEBOOK", "TIKTOK")
                        val platformOptions = platforms.map { plat -> plat to if (plat == "All") allInfluencers.size else allInfluencers.count { it.platforms?.any { p -> p.platform.equals(plat, ignoreCase = true) } == true } }

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

                        FilterDropdown("Platform", selectedPlatform, platformOptions, onPlatformSelected, Modifier.weight(1f))
                        FilterDropdown("Category", selectedCategory, categoryOptions, onCategorySelected, Modifier.weight(1f))
                        FilterDropdown("Followers", selectedFollowerRange, followerOptions, onFollowerRangeSelected, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${filteredInfluencers.size} Influencers Found", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                        if (selectedPlatform != "All" || selectedCategory != "All" || selectedFollowerRange != "All" || searchQuery.isNotEmpty()) {
                            Text("Clear All", color = Color(0xFFFF8383), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                onPlatformSelected("All")
                                onCategorySelected("All")
                                onFollowerRangeSelected("All")
                                onSearchQueryChange("")
                            })
                        }
                    }
                }
            }

            // ---------------- RESULTS SECTION ----------------
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFFF8383))
                    }
                }
            } else if (filteredInfluencers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No influencers match your criteria.", color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                items(paginatedInfluencers) { influencer ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        BrandCardBrand(
                            influencer = influencer,
                            isWishlisted = wishlistedInfluencers.any { it.id == influencer.id },
                            onWishlistToggle = { onWishlistToggle(influencer) },
                            modifier = Modifier.fillMaxWidth(),
                            onCardClick = { onInfluencerClick(influencer.id) }
                        )
                    }
                }

                if (totalPages > 1) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                                enabled = currentPage > 1,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
                            ) { Text("Previous") }

                            Spacer(modifier = Modifier.width(16.dp))
                            Text("$currentPage / $totalPages", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                                enabled = currentPage < totalPages,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
                            ) { Text("Next") }
                        }
                    }
                }
            }
        }
    }
}
