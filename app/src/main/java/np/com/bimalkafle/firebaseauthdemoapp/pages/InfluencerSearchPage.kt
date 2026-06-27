package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.components.AiChatFab
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.components.EmptyState
import np.com.bimalkafle.firebaseauthdemoapp.components.FilterDropdown
import np.com.bimalkafle.firebaseauthdemoapp.components.IconBubbleSearch
import np.com.bimalkafle.firebaseauthdemoapp.components.SkeletonCard
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignDetail
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel

private val brandThemeColor: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val instagramColor = Color(0xFFE1306C)

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

    var selectedPlatforms by remember { mutableStateOf(setOf("All")) }
    var selectedCategories by remember { mutableStateOf(setOf("All")) }
    var currentPage by remember { mutableIntStateOf(1) }

    val filteredCampaigns = remember(searchQuery, selectedPlatforms, selectedCategories, campaigns) {
        campaigns.filter { campaign ->
            val campaignCategories = campaign.categories?.map { it.category } ?: emptyList()
            val campaignPlatforms = campaign.platforms?.map { it.platform } ?: emptyList()

            val matchesSearch = campaign.title.contains(searchQuery, ignoreCase = true) ||
                    campaign.description.contains(searchQuery, ignoreCase = true) ||
                    (campaign.brand?.name?.contains(searchQuery, ignoreCase = true) == true) ||
                    campaignCategories.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesPlatform = selectedPlatforms.contains("All") ||
                    campaignPlatforms.any { plat -> selectedPlatforms.any { sel -> plat.equals(sel, ignoreCase = true) } }

            val matchesCategory = selectedCategories.contains("All") ||
                    campaignCategories.any { cat -> selectedCategories.any { sel -> cat.equals(sel, ignoreCase = true) } }

            matchesSearch && matchesPlatform && matchesCategory
        }
    }

    fun toggleFilter(currentSet: Set<String>, option: String): Set<String> {
        return if (option == "All") {
            setOf("All")
        } else {
            val next = currentSet.toMutableSet()
            next.remove("All")
            if (next.contains(option)) {
                next.remove(option)
                if (next.isEmpty()) setOf("All") else next
            } else {
                next.add(option)
                next
            }
        }
    }
    
    var selectedBottomNavItem by remember { mutableStateOf("Search") }

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = { selectedBottomNavItem = it },
                navController = navController,
                isBrand = false
            )
        },
        floatingActionButton = { AiChatFab(navController) }
    ) { padding ->
        val itemsPerPage = 8
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
                .background(Color(0xFFF8F9FE)),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 8.dp)
        ) {
            // ---------------- REFINED HEADER ----------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vector),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().alpha(0.2f),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 8.dp)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(40.dp).clickable { navController.popBackStack() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(24.dp))
                                }
                            }

                            Row {
                                IconBubbleSearch(Icons.Default.Favorite, Color.Red, contentDescription = "View wishlist") { navController.navigate("wishlist") }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box {
                                    IconBubbleSearch(Icons.Default.Notifications, Color.Black, contentDescription = "View notifications") { navController.navigate("notifications") }
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

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Discover", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Find active campaigns to collaborate with", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(10.dp))

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it 
                                currentPage = 1
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .shadow(4.dp, RoundedCornerShape(22.dp))
                                .background(Color.White, RoundedCornerShape(22.dp)),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (searchQuery.isEmpty()) {
                                            Text("Search campaigns...", color = Color.Gray, fontSize = 14.sp)
                                        }
                                        innerTextField()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categoriesList = listOf("All", "Tech", "Fashion", "Food", "Lifestyle", "Beauty", "Sports")
                        val categoryOptions = categoriesList.map { category ->
                            val count = if (category == "All") {
                                campaigns.size
                            } else {
                                campaigns.count { camp -> camp.categories?.any { it.category.equals(category, ignoreCase = true) } == true }
                            }
                            category to count
                        }

                        val platformsList = listOf("All", "Instagram", "YouTube", "Facebook", "TikTok")
                        val platformOptions = platformsList.map { platform ->
                            val count = if (platform == "All") {
                                 campaigns.size
                            } else {
                                campaigns.count { camp -> camp.platforms?.any { it.platform.equals(platform, ignoreCase = true) } == true }
                            }
                            platform to count
                        }

                        FilterDropdown("Platform", selectedPlatforms, platformOptions, { selectedPlatforms = toggleFilter(selectedPlatforms, it); currentPage = 1 }, Modifier.weight(1f))
                        FilterDropdown("Category", selectedCategories, categoryOptions, { selectedCategories = toggleFilter(selectedCategories, it); currentPage = 1 }, Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${filteredCampaigns.size} Campaigns", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        if (!selectedPlatforms.contains("All") || !selectedCategories.contains("All") || searchQuery.isNotEmpty()) {
                            Text("Clear All", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                selectedPlatforms = setOf("All")
                                selectedCategories = setOf("All")
                                searchQuery = ""
                                currentPage = 1
                            })
                        }
                    }
                }
            }

            if (isLoading) {
                items(3) {
                    SkeletonCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), height = 120.dp)
                }
            } else if (filteredCampaigns.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No campaigns found",
                        subtitle = "Try adjusting your filters."
                    )
                }
            } else {
                items(paginatedCampaigns) { campaign ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        CampaignCardSearch(
                            campaign = campaign,
                            isWishlisted = wishlistedCampaigns.any { it.id == campaign.id },
                            onWishlistToggle = { 
                                if (firebaseToken != null) {
                                    campaignViewModel.toggleWishlist(campaign, firebaseToken!!)
                                } 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            onCardClick = { navController.navigate("campaign_detail/${campaign.id}") }
                        )
                    }
                }
                
                if (totalPages > 1) {
                     item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 1) currentPage-- },
                                enabled = currentPage > 1
                            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous") }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$currentPage / $totalPages", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { if (currentPage < totalPages) currentPage++ },
                                enabled = currentPage < totalPages
                            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CampaignCardSearch(
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
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Surface(
                    shape = CircleShape,
                    color = brandThemeColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
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
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = campaign.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = campaign.brand?.name ?: "Unknown Brand",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = campaign.description,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = { onWishlistToggle() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isWishlisted) instagramColor else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    val formatBudget = { amount: Int? ->
                        when {
                            amount == null -> "-"
                            amount >= 1000000 -> "${String.format("%.1f", amount / 1000000.0)}M"
                            amount >= 1000 -> "${amount / 1000}k"
                            else -> amount.toString()
                        }
                    }
                    
                    val budgetRange = when {
                        campaign.budgetMin != null && campaign.budgetMax != null -> "₹${formatBudget(campaign.budgetMin)} - ₹${formatBudget(campaign.budgetMax)}"
                        campaign.budgetMin != null -> "₹${formatBudget(campaign.budgetMin)}+"
                        else -> "N/A"
                    }
                    
                    Text(text = budgetRange, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = brandThemeColor)
                }
            }
        }
    }
}
