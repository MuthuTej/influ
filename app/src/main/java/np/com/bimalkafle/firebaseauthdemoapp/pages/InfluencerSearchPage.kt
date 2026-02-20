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
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.components.FilterDropdown
import np.com.bimalkafle.firebaseauthdemoapp.components.IconBubbleSearch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluencerSearchPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    campaignViewModel: CampaignViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val campaigns by campaignViewModel.campaigns.observeAsState(initial = emptyList())
    val isLoading by campaignViewModel.loading.observeAsState(initial = false)
    val wishlistedCampaigns by campaignViewModel.wishlistedCampaigns.observeAsState(initial = emptyList())
    var firebaseToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                firebaseToken = result.token
                if (firebaseToken != null) {
                    campaignViewModel.fetchCampaigns(firebaseToken!!)
                }
            }
    }

    var selectedPlatform by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedBudgetRange by remember { mutableStateOf("All") }

    // Filter campaigns based on search query and selected filters
    val filteredCampaigns = campaigns.filter { campaign ->
        val matchesSearch = campaign.title.contains(searchQuery, ignoreCase = true) ||
                campaign.description.contains(searchQuery, ignoreCase = true) ||
                (campaign.brand?.name?.contains(searchQuery, ignoreCase = true) == true)

        val matchesPlatform = selectedPlatform == "All" ||
                campaign.brand?.preferredPlatforms?.any { it.platform.equals(selectedPlatform, ignoreCase = true) } == true

        val matchesCategory = selectedCategory == "All" ||
                campaign.brand?.brandCategory?.category.equals(selectedCategory, ignoreCase = true)

        val matchesBudget = when (selectedBudgetRange) {
            "All" -> true
            "0-100" -> (campaign.budgetMax ?: 0) <= 100
            "100-500" -> (campaign.budgetMin ?: 0) >= 100 && (campaign.budgetMax ?: 0) <= 500
            "500-1000" -> (campaign.budgetMin ?: 0) >= 500 && (campaign.budgetMax ?: 0) <= 1000
            "1000+" -> (campaign.budgetMin ?: 0) >= 1000
            else -> true
        }

        matchesSearch && matchesPlatform && matchesCategory && matchesBudget
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
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
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
                                .clickable { navController.popBackStack() }
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
                                onClick = { navController.navigate("wishlist") }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            IconBubbleSearch(Icons.Default.Notifications, Color.Black)
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
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(28.dp)),
                        placeholder = { Text("Search Campaigns", color = Color.Gray) },
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
                val categories = listOf("All", "Tech", "Fashion", "Food", "Lifestyle", "Beauty", "Sports")
                val categoryOptions = categories.map { category ->
                    val count = if (category == "All") {
                        campaigns.size
                    } else {
                        campaigns.count { it.brand?.brandCategory?.category.equals(category, ignoreCase = true) }
                    }
                    category to count
                }

                val platforms = listOf("All", "INSTAGRAM", "YOUTUBE", "FACEBOOK", "TIKTOK")
                val platformOptions = platforms.map { platform ->
                    val count = if (platform == "All") {
                         campaigns.size
                    } else {
                        campaigns.count { campaign -> 
                             campaign.brand?.preferredPlatforms?.any { it.platform.equals(platform, ignoreCase = true) } == true
                        }
                    }
                    platform to count
                }
                
                val budgetOptions = listOf("All", "0-100", "100-500", "500-1000", "1000+").map { it to null }

                FilterDropdown(
                    label = "Platform",
                    selectedOption = selectedPlatform,
                    options = platformOptions,
                    onOptionSelected = { selectedPlatform = it },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Category",
                    selectedOption = selectedCategory,
                    options = categoryOptions,
                    onOptionSelected = { selectedCategory = it },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Budget",
                    selectedOption = selectedBudgetRange,
                    options = budgetOptions,
                    onOptionSelected = { selectedBudgetRange = it },
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

            // ---------------- PAGINATION LOGIC ----------------
            var currentPage by remember { mutableStateOf(1) }
            val itemsPerPage = 5
            val totalPages = (filteredCampaigns.size + itemsPerPage - 1) / itemsPerPage
            
            LaunchedEffect(filteredCampaigns) {
                currentPage = 1
            }

            val paginatedCampaigns = filteredCampaigns
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
                     contentPadding = PaddingValues(bottom = 80.dp)
                ) {
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
                                .padding(vertical = 8.dp), 
                            onCardClick = {
                                 navController.navigate("campaign_detail/${campaign.id}")
                            }
                        )
                    }
                    
                    if (filteredCampaigns.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No campaigns found matching your search.", color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else if (totalPages > 1) {
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
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
