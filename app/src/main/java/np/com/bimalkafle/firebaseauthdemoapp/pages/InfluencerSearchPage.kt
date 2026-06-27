package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.components.AiChatFab
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.components.EmptyState
import np.com.bimalkafle.firebaseauthdemoapp.components.FilterDropdown
import np.com.bimalkafle.firebaseauthdemoapp.components.IconBubbleSearch
import np.com.bimalkafle.firebaseauthdemoapp.components.SkeletonCard
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluencerSearchPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    campaignViewModel: CampaignViewModel,
    notificationViewModel: NotificationViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val campaigns by campaignViewModel.campaigns.observeAsState(initial = emptyList())
    val isLoading by campaignViewModel.loading.observeAsState(initial = false)
    val wishlistedCampaigns by campaignViewModel.wishlistedCampaigns.observeAsState(initial = emptyList())
    var firebaseToken by remember { mutableStateOf<String?>(null) }
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                firebaseToken = result.token
                if (firebaseToken != null) {
                    campaignViewModel.fetchCampaigns(firebaseToken!!)
                    notificationViewModel.fetchUnreadCount(currentUser.uid, firebaseToken!!)
                }
            }
    }

    var selectedPlatform by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var currentPage by remember { mutableIntStateOf(1) }

    // Filter campaigns based on search query and selected filters
    val filteredCampaigns = remember(searchQuery, selectedPlatform, selectedCategory, campaigns) {
        campaigns.filter { campaign ->
            val campaignCategories = campaign.categories?.map { it.category } ?: emptyList()
            val campaignPlatforms = campaign.platforms?.map { it.platform } ?: emptyList()

            val matchesSearch = campaign.title.contains(searchQuery, ignoreCase = true) ||
                    campaign.description.contains(searchQuery, ignoreCase = true) ||
                    (campaign.brand?.name?.contains(searchQuery, ignoreCase = true) == true) ||
                    campaignCategories.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesPlatform = selectedPlatform == "All" ||
                    campaignPlatforms.any { it.equals(selectedPlatform, ignoreCase = true) }

            val matchesCategory = selectedCategory == "All" ||
                    campaignCategories.any { it.equals(selectedCategory, ignoreCase = true) }

            matchesSearch && matchesPlatform && matchesCategory
        }
    }
    
    var selectedBottomNavItem by remember { mutableStateOf("Search") }

    Scaffold(
        bottomBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    CmnBottomNavigationBar(
                        selectedItem = selectedBottomNavItem,
                        onItemSelected = { selectedBottomNavItem = it },
                        navController = navController,
                        isBrand = false
                    )
                }
            }
        },
        floatingActionButton = { AiChatFab(navController) }
    ) { padding ->
        // ---------------- PAGINATION LOGIC ----------------
        val itemsPerPage = 5
        val totalPages = ((filteredCampaigns.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
        
        LaunchedEffect(filteredCampaigns) {
            currentPage = 1
        }

        val paginatedCampaigns = filteredCampaigns
            .drop((currentPage - 1) * itemsPerPage)
            .take(itemsPerPage)

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp)
        ) {
            // ---------------- HEADER ----------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                        .background(MaterialTheme.colorScheme.primary)
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
                            .statusBarsPadding()
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
                                    .clickable { navController.popBackStack() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Back",
                                        tint = Color.Black
                                    )
                                }
                            }

                            Row {
                                IconBubbleSearch(
                                    icon = Icons.Default.Favorite,
                                    tint = Color.Red,
                                    contentDescription = "View wishlist",
                                    onClick = { navController.navigate("wishlist") }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box {
                                    IconBubbleSearch(
                                        icon = Icons.Default.Notifications,
                                        tint = Color.Black,
                                        contentDescription = "View notifications",
                                        onClick = { navController.navigate("notifications") }
                                    )
                                    if (unreadCount > 0) {
                                        Badge(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
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
                            text = "Discover Campaigns",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Find active campaigns to collaborate with",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Search Bar
                        TextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it 
                                currentPage = 1
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(8.dp, RoundedCornerShape(28.dp)),
                            placeholder = { Text("Search Campaigns", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                            shape = RoundedCornerShape(28.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                disabledContainerColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            // ---------------- FILTERS SECTION ----------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Calculate counts for Categories
                    val categoriesList = listOf("All", "Tech", "Fashion", "Food", "Lifestyle", "Beauty", "Sports")
                    val categoryOptions = categoriesList.map { category ->
                        val count = if (category == "All") {
                            campaigns.size
                        } else {
                            campaigns.count { camp -> 
                                camp.categories?.any { it.category.equals(category, ignoreCase = true) } == true 
                            }
                        }
                        category to count
                    }

                    // Calculate counts for Platforms
                    val platformsList = listOf("All", "Instagram", "YouTube", "Facebook", "TikTok")
                    val platformOptions = platformsList.map { platform ->
                        val count = if (platform == "All") {
                             campaigns.size
                        } else {
                            campaigns.count { camp -> 
                                 camp.platforms?.any { it.platform.equals(platform, ignoreCase = true) } == true
                            }
                        }
                        platform to count
                    }

                    FilterDropdown(
                        label = "Platform",
                        selectedOption = selectedPlatform,
                        options = platformOptions,
                        onOptionSelected = { 
                            selectedPlatform = it 
                            currentPage = 1
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterDropdown(
                        label = "Category",
                        selectedOption = selectedCategory,
                        options = categoryOptions,
                        onOptionSelected = { 
                            selectedCategory = it 
                            currentPage = 1
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Results count and header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filteredCampaigns.size} Campaigns Found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )

                    // Clear Filters Button
                    if (selectedPlatform != "All" || selectedCategory != "All" || searchQuery.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                selectedPlatform = "All"
                                selectedCategory = "All"
                                searchQuery = ""
                                currentPage = 1
                            }
                        )
                    }
                }
            }

            // ---------------- RESULTS SECTION ----------------
            if (isLoading) {
                items(3) {
                    SkeletonCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), height = 180.dp)
                }
            } else if (filteredCampaigns.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No campaigns found",
                        subtitle = "Try adjusting your filters or search terms."
                    )
                }
            } else {
                items(paginatedCampaigns) { campaign ->
                    CampaignCardInfluencer(
                        campaign = campaign,
                        isWishlisted = wishlistedCampaigns.any { it.id == campaign.id },
                        onWishlistToggle = { 
                            if (firebaseToken != null) {
                                campaignViewModel.toggleWishlist(campaign, firebaseToken!!)
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
                
                if (totalPages > 1) {
                     item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { if (currentPage > 1) currentPage-- },
                                enabled = currentPage > 1,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Previous")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Text(
                                text = "Page $currentPage of $totalPages",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = { if (currentPage < totalPages) currentPage++ },
                                enabled = currentPage < totalPages,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
