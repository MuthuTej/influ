package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.*
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SearchFilters
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SearchMeta
import np.com.bimalkafle.firebaseauthdemoapp.components.AiChatFab
import np.com.bimalkafle.firebaseauthdemoapp.components.BrandCardBrand
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.EmptyState
import np.com.bimalkafle.firebaseauthdemoapp.components.FilterDropdown
import np.com.bimalkafle.firebaseauthdemoapp.components.IconBubbleSearch
import np.com.bimalkafle.firebaseauthdemoapp.components.SearchSuggestionsPopup
import np.com.bimalkafle.firebaseauthdemoapp.components.SkeletonCard
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel

@Composable
fun BrandSearchPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel,
    notificationViewModel: NotificationViewModel,
    initialCategories: List<String> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatforms by remember { mutableStateOf(setOf("All")) }
    var selectedCategories by remember {
        mutableStateOf(if (initialCategories.isNotEmpty()) initialCategories.toSet() else setOf("All"))
    }
    var selectedFollowerRanges by remember { mutableStateOf(setOf("All")) }
    var selectedGenders by remember { mutableStateOf(setOf("All")) }
    var selectedMotherTongues by remember { mutableStateOf(setOf("All")) }
    var selectedLanguagesKnown by remember { mutableStateOf(setOf("All")) }
    var selectedLocations by remember { mutableStateOf(setOf("All")) }
    var selectedMinRating by remember { mutableStateOf("All") }

    val searchResults by brandViewModel.searchResults.observeAsState(initial = emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val isLoadingMore by brandViewModel.loadingMore.observeAsState(initial = false)
    val searchMeta by brandViewModel.searchMeta.observeAsState(initial = SearchMeta())
    val wishlistedInfluencers by brandViewModel.wishlistedInfluencers.observeAsState(initial = emptyList())
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)

    var firebaseToken by remember { mutableStateOf<String?>(null) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val minRatingThreshold = when (selectedMinRating) {
        "4.5+ ★" -> 4.5
        "4+ ★" -> 4.0
        "3.5+ ★" -> 3.5
        "3+ ★" -> 3.0
        else -> null
    }

    // Derive current filters from UI state
    val currentFilters = remember(
        searchQuery, selectedPlatforms, selectedCategories, selectedFollowerRanges,
        selectedGenders, selectedMotherTongues, selectedLanguagesKnown, selectedLocations, minRatingThreshold
    ) {
        SearchFilters(
            query = searchQuery,
            platforms = selectedPlatforms,
            categories = selectedCategories,
            followerRange = selectedFollowerRanges,
            gender = selectedGenders,
            motherTongue = selectedMotherTongues,
            languagesKnown = selectedLanguagesKnown,
            location = selectedLocations,
            minRating = minRatingThreshold
        )
    }

    // Fetch token on launch + fetch wishlist + initial search
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)?.addOnSuccessListener { result ->
            firebaseToken = result.token
            result.token?.let { tok ->
                brandViewModel.fetchWishlist(tok)
                notificationViewModel.fetchUnreadCount(user.uid, tok)
            }
        }
    }

    // Trigger a fresh page-0 search whenever token becomes available or filters change.
    // 300ms debounce only for text changes to avoid firing per-keystroke.
    LaunchedEffect(firebaseToken, currentFilters) {
        val tok = firebaseToken ?: return@LaunchedEffect
        if (currentFilters.query.isNotBlank()) delay(300L)
        brandViewModel.searchInfluencers(tok, currentFilters, 0, false)
    }

    // Suggestions for the search autocomplete popup
    val suggestions = remember(searchQuery, searchResults) {
        if (searchQuery.length < 2) emptyList()
        else {
            val names = searchResults.map { it.name }.filter { it.contains(searchQuery, ignoreCase = true) }
            val cats = searchResults.flatMap { it.categories?.map { c -> c.category } ?: emptyList() }
                .distinct().filter { it.contains(searchQuery, ignoreCase = true) }
            val locs = searchResults.mapNotNull { it.location }.filter { it.contains(searchQuery, ignoreCase = true) }
            (names + cats + locs).distinct().take(5)
        }
    }

    fun toggleFilter(currentSet: Set<String>, option: String): Set<String> =
        if (option == "All") setOf("All")
        else {
            val next = currentSet.toMutableSet().also { it.remove("All") }
            if (next.contains(option)) { next.remove(option); if (next.isEmpty()) setOf("All") else next }
            else { next.add(option); next }
        }

    val listState = rememberLazyListState()

    // Infinite scroll: load next page when near the bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3 && total > 0 && searchMeta.hasMore && !isLoadingMore && !isLoading
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            firebaseToken?.let { tok ->
                brandViewModel.searchInfluencers(tok, currentFilters, searchMeta.page + 1, true)
            }
        }
    }

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "Search",
                onItemSelected = { },
                navController = navController,
                isBrand = true
            )
        },
        floatingActionButton = { AiChatFab(navController) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(Color(0xFFF8F9FE)),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // Header
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
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp).clickable { navController.popBackStack() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            Row {
                                IconBubbleSearch(Icons.Default.Favorite, Color.Red, contentDescription = "View wishlist") { navController.navigate("brand_wishlist") }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box {
                                    IconBubbleSearch(Icons.Default.Notifications, Color.Black, contentDescription = "Notifications") { navController.navigate("notifications") }
                                    if (unreadCount > 0) {
                                        Badge(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                                            containerColor = Color.Red,
                                            contentColor = Color.White
                                        ) { Text(if (unreadCount > 9) "9+" else unreadCount.toString(), fontSize = 10.sp) }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Discover", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Find influencers for your campaign",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .shadow(4.dp, RoundedCornerShape(22.dp))
                                    .background(Color.White, RoundedCornerShape(22.dp))
                                    .onFocusChanged { isSearchFocused = it.isFocused },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                decorationBox = { innerTextField ->
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                            if (searchQuery.isEmpty()) Text("Search influencers...", color = Color.Gray, fontSize = 14.sp)
                                            innerTextField()
                                        }
                                    }
                                }
                            )
                            SearchSuggestionsPopup(
                                suggestions = suggestions,
                                onSuggestionClick = { searchQuery = it; focusManager.clearFocus() },
                                isVisible = isSearchFocused && suggestions.isNotEmpty(),
                                modifier = Modifier.padding(top = 48.dp)
                            )
                        }
                    }
                }
            }

            // Filters
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val categoriesList = listOf("All", "Agriculture", "Arts & Creativity", "Automotive", "Business & Entrepreneurship",
                            "Education", "Entertainment", "Fashion & Beauty", "Finance", "Fitness", "Food",
                            "Gaming", "Health & Wellness", "Lifestyle", "Music", "Parenting & Family",
                            "Spirituality & Religion", "Sports", "Technology", "Travel")
                        val platformsList = listOf("All", "INSTAGRAM", "YOUTUBE", "FACEBOOK")
                        val followerRanges = listOf("All", "0-10K", "10K-100K", "100K-1M", "1M+")
                        val gendersList = listOf("All", "Male", "Female", "Other")
                        val languagesList = listOf("All", "Assamese", "Bengali", "Bodo", "Dogri","English", "Gujarati", "Hindi", "Kannada",
                            "Kashmiri", "Konkani", "Maithili", "Malayalam", "Manipuri (Meitei)", "Marathi", "Nepali",
                            "Odia", "Punjabi", "Sanskrit", "Santali", "Sindhi", "Tamil", "Telugu", "Urdu")
                        val hardcodedLocations = listOf("All", "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem",
                            "Tirunelveli", "Tiruppur", "Erode", "Vellore", "Thoothukudi")
                        val ratingOptions = listOf("All", "3+ ★", "3.5+ ★", "4+ ★", "4.5+ ★")

                        FilterDropdown("Category", selectedCategories, categoriesList.map { it to null }, { selectedCategories = toggleFilter(selectedCategories, it) }, Modifier.width(120.dp), searchable = true)
                        FilterDropdown("Platform", selectedPlatforms, platformsList.map { it to null }, { selectedPlatforms = toggleFilter(selectedPlatforms, it) }, Modifier.width(110.dp), searchable = false)
                        FilterDropdown("Followers", selectedFollowerRanges, followerRanges.map { it to null }, { selectedFollowerRanges = toggleFilter(selectedFollowerRanges, it) }, Modifier.width(110.dp), searchable = false)
                        FilterDropdown("Rating", setOf(selectedMinRating), ratingOptions.map { it to null }, { selectedMinRating = it }, Modifier.width(100.dp), searchable = false)
                        FilterDropdown("Gender", selectedGenders, gendersList.map { it to null }, { selectedGenders = toggleFilter(selectedGenders, it) }, Modifier.width(100.dp), searchable = false)
                        FilterDropdown("Mother Tongue", selectedMotherTongues, languagesList.map { it to null }, { selectedMotherTongues = toggleFilter(selectedMotherTongues, it) }, Modifier.width(140.dp), searchable = true)
                        FilterDropdown("Languages", selectedLanguagesKnown, languagesList.map { it to null }, { selectedLanguagesKnown = toggleFilter(selectedLanguagesKnown, it) }, Modifier.width(120.dp), searchable = true)
                        FilterDropdown("Location", selectedLocations, hardcodedLocations.map { it to null }, { selectedLocations = toggleFilter(selectedLocations, it) }, Modifier.width(120.dp), searchable = true)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val hasActiveFilter = !selectedPlatforms.contains("All") || !selectedCategories.contains("All") ||
                            !selectedFollowerRanges.contains("All") || !selectedGenders.contains("All") ||
                            !selectedMotherTongues.contains("All") || !selectedLanguagesKnown.contains("All") ||
                            !selectedLocations.contains("All") || selectedMinRating != "All" || searchQuery.isNotEmpty()
                        val countLabel = if (searchMeta.total > 0) "${searchMeta.total} Influencers Found" else if (!isLoading) "No influencers found" else "Searching..."
                        Text(countLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        if (hasActiveFilter) {
                            Text("Clear All", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    searchQuery = ""; selectedPlatforms = setOf("All"); selectedCategories = setOf("All")
                                    selectedFollowerRanges = setOf("All"); selectedGenders = setOf("All")
                                    selectedMotherTongues = setOf("All"); selectedLanguagesKnown = setOf("All")
                                    selectedLocations = setOf("All"); selectedMinRating = "All"
                                })
                        }
                    }
                }
            }

            // Results
            if (isLoading && searchResults.isEmpty()) {
                items(4) {
                    SkeletonCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), height = 160.dp)
                }
            } else if (searchResults.isEmpty() && !isLoading) {
                item {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No influencers found",
                        subtitle = "Try adjusting your filters or search terms."
                    )
                }
            } else {
                items(searchResults, key = { it.id }) { influencer ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        BrandCardBrand(
                            influencer = influencer,
                            isWishlisted = wishlistedInfluencers.any { it.id == influencer.id },
                            onWishlistToggle = {
                                firebaseToken?.let { tok -> brandViewModel.toggleWishlist(influencer, tok) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            onCardClick = { navController.navigate("brand_influencer_detail/${influencer.id}") },
                            selectedPlatform = if (selectedPlatforms.contains("All")) null
                                              else selectedPlatforms.firstOrNull { it != "All" }
                        )
                    }
                }

                // Loading-more indicator at the bottom
                if (isLoadingMore) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                } else if (!searchMeta.hasMore && searchResults.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                            Text("All ${searchMeta.total} influencers loaded", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
