package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.*
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
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
    notificationViewModel: NotificationViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedPlatforms by remember { mutableStateOf(setOf("All")) }
    var selectedCategories by remember { mutableStateOf(setOf("All")) }
    var selectedFollowerRanges by remember { mutableStateOf(setOf("All")) }
    var selectedGenders by remember { mutableStateOf(setOf("All")) }
    var selectedMotherTongues by remember { mutableStateOf(setOf("All")) }
    var selectedLanguagesKnown by remember { mutableStateOf(setOf("All")) }
    var selectedLocations by remember { mutableStateOf(setOf("All")) }
    var currentPage by remember { mutableIntStateOf(1) }

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

    val filteredInfluencers = remember(
        searchQuery, selectedPlatforms, selectedCategories, selectedFollowerRanges,
        selectedGenders, selectedMotherTongues, selectedLanguagesKnown, selectedLocations,
        influencers
    ) {
        influencers.filter { influencer ->
            val matchesSearch = influencer.name.contains(searchQuery, ignoreCase = true) ||
                    influencer.bio?.contains(searchQuery, ignoreCase = true) == true ||
                    influencer.categories?.any { it.category.contains(searchQuery, ignoreCase = true) } == true

            val matchesPlatform = selectedPlatforms.contains("All") ||
                    influencer.platforms?.any { plat -> 
                        selectedPlatforms.any { sel -> plat.platform.equals(sel, ignoreCase = true) } 
                    } == true

            val matchesCategory = selectedCategories.contains("All") ||
                    influencer.categories?.any { cat -> 
                        selectedCategories.any { sel -> cat.category.equals(sel, ignoreCase = true) } 
                    } == true

            val matchesFollowers = if (selectedFollowerRanges.contains("All")) true else {
                influencer.platforms?.any { plat ->
                    selectedFollowerRanges.any { range ->
                        when (range) {
                            "0-10K" -> (plat.followers ?: 0) < 10000
                            "10K-100K" -> (plat.followers ?: 0) in 10000..100000
                            "100K-1M" -> (plat.followers ?: 0) in 100000..1000000
                            "1M+" -> (plat.followers ?: 0) > 1000000
                            else -> false
                        }
                    }
                } == true
            }

            val matchesGender = selectedGenders.contains("All") || selectedGenders.contains(influencer.gender)
            val matchesMotherTongue = selectedMotherTongues.contains("All") || selectedMotherTongues.contains(influencer.motherTongue)
            val matchesLanguagesKnown = selectedLanguagesKnown.contains("All") || 
                    influencer.languagesKnown?.any { selectedLanguagesKnown.contains(it) } == true
            val matchesLocation = selectedLocations.contains("All") || selectedLocations.contains(influencer.location)

            matchesSearch && matchesPlatform && matchesCategory && matchesFollowers && 
                    matchesGender && matchesMotherTongue && matchesLanguagesKnown && matchesLocation
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

    BrandSearchPageContent(
        modifier = modifier,
        searchQuery = searchQuery,
        onSearchQueryChange = { 
            searchQuery = it
            currentPage = 1
        },
        selectedPlatforms = selectedPlatforms,
        onPlatformToggle = { 
            selectedPlatforms = toggleFilter(selectedPlatforms, it)
            currentPage = 1
        },
        selectedCategories = selectedCategories,
        onCategoryToggle = { 
            selectedCategories = toggleFilter(selectedCategories, it)
            currentPage = 1
        },
        selectedFollowerRanges = selectedFollowerRanges,
        onFollowerRangeToggle = { 
            selectedFollowerRanges = toggleFilter(selectedFollowerRanges, it)
            currentPage = 1
        },
        selectedGenders = selectedGenders,
        onGenderToggle = {
            selectedGenders = toggleFilter(selectedGenders, it)
            currentPage = 1
        },
        selectedMotherTongues = selectedMotherTongues,
        onMotherTongueToggle = {
            selectedMotherTongues = toggleFilter(selectedMotherTongues, it)
            currentPage = 1
        },
        selectedLanguagesKnown = selectedLanguagesKnown,
        onLanguagesKnownToggle = {
            selectedLanguagesKnown = toggleFilter(selectedLanguagesKnown, it)
            currentPage = 1
        },
        selectedLocations = selectedLocations,
        onLocationToggle = {
            selectedLocations = toggleFilter(selectedLocations, it)
            currentPage = 1
        },
        filteredInfluencers = filteredInfluencers,
        allInfluencers = influencers,
        isLoading = isLoading,
        onBackClick = { navController.popBackStack() },
        onInfluencerClick = { id -> navController.navigate("brand_influencer_detail/$id") },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandSearchPageContent(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedPlatforms: Set<String>,
    onPlatformToggle: (String) -> Unit,
    selectedCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    selectedFollowerRanges: Set<String>,
    onFollowerRangeToggle: (String) -> Unit,
    selectedGenders: Set<String>,
    onGenderToggle: (String) -> Unit,
    selectedMotherTongues: Set<String>,
    onMotherTongueToggle: (String) -> Unit,
    selectedLanguagesKnown: Set<String>,
    onLanguagesKnownToggle: (String) -> Unit,
    selectedLocations: Set<String>,
    onLocationToggle: (String) -> Unit,
    filteredInfluencers: List<InfluencerProfile>,
    allInfluencers: List<InfluencerProfile>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onInfluencerClick: (String) -> Unit,
    navController: NavController,
    unreadCount: Int,
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    wishlistedInfluencers: List<InfluencerProfile> = emptyList(),
    onWishlistToggle: (InfluencerProfile) -> Unit = {}
) {
    val itemsPerPage = 10
    val totalPages = ((filteredInfluencers.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)
    val paginatedInfluencers = filteredInfluencers
        .drop((currentPage - 1) * itemsPerPage)
        .take(itemsPerPage)
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }

    val suggestions = remember(searchQuery, allInfluencers) {
        if (searchQuery.length < 2) emptyList()
        else {
            val names = allInfluencers.map { it.name }.filter { it.contains(searchQuery, ignoreCase = true) }
            val cats = allInfluencers.flatMap { it.categories?.map { c -> c.category } ?: emptyList() }
                .distinct()
                .filter { it.contains(searchQuery, ignoreCase = true) }
            (names + cats).distinct().take(5)
        }
    }

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "Search",
                onItemSelected = { /* Handled in component */ },
                navController = navController,
                isBrand = true
            )
        },
        floatingActionButton = { AiChatFab(navController) }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(Color(0xFFF8F9FE)),
            contentPadding = PaddingValues(bottom = 8.dp)
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
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(40.dp).clickable { onBackClick() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }

                            Row {
                                IconBubbleSearch(Icons.Default.Favorite, Color.Red, contentDescription = "View wishlist") { navController.navigate("brand_wishlist") }
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
                        Text("Find influencers for your campaign", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(10.dp))

                        Box {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
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
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                            if (searchQuery.isEmpty()) {
                                                Text("Search influencers...", color = Color.Gray, fontSize = 14.sp)
                                            }
                                            innerTextField()
                                        }
                                    }
                                }
                            )

                            SearchSuggestionsPopup(
                                suggestions = suggestions,
                                onSuggestionClick = { 
                                    onSearchQueryChange(it)
                                    focusManager.clearFocus()
                                },
                                isVisible = isSearchFocused && suggestions.isNotEmpty(),
                                modifier = Modifier.padding(top = 48.dp)
                            )
                        }
                    }
                }
            }

            // ---------------- FILTERS SECTION ----------------
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
                        val categoriesList = listOf(
                            "All", "Agriculture", "Arts & Creativity", "Automotive", "Business & Entrepreneurship", 
                            "Education", "Entertainment", "Fashion & Beauty", "Finance", "Fitness", "Food", 
                            "Gaming", "Health & Wellness", "Lifestyle", "Music", "Parenting & Family", 
                            "Spirituality & Religion", "Sports", "Technology", "Travel"
                        )
                        val categoryOptions = categoriesList.map { cat -> cat to if (cat == "All") allInfluencers.size else allInfluencers.count { it.categories?.any { c -> c.category.equals(cat, ignoreCase = true) } == true } }

                        val platformsList = listOf("All", "INSTAGRAM", "YOUTUBE", "FACEBOOK", "TIKTOK")
                        val platformOptions = platformsList.map { plat -> plat to if (plat == "All") allInfluencers.size else allInfluencers.count { it.platforms?.any { p -> p.platform.equals(plat, ignoreCase = true) } == true } }

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

                        val gendersList = listOf("All", "Male", "Female", "Other")
                        val genderOptions = gendersList.map { g -> g to if (g == "All") allInfluencers.size else allInfluencers.count { it.gender.equals(g, ignoreCase = true) } }

                        val languagesList = listOf(
                            "All", "Assamese", "Bengali", "Bodo", "Dogri", "Gujarati", "Hindi", "Kannada", "Kashmiri", 
                            "Konkani", "Maithili", "Malayalam", "Manipuri (Meitei)", "Marathi", "Nepali", "Odia", 
                            "Punjabi", "Sanskrit", "Santali", "Sindhi", "Tamil", "Telugu", "Urdu"
                        )
                        val mtOptions = languagesList.map { l -> l to if (l == "All") allInfluencers.size else allInfluencers.count { it.motherTongue.equals(l, ignoreCase = true) } }
                        val lkOptions = languagesList.map { l -> l to if (l == "All") allInfluencers.size else allInfluencers.count { it.languagesKnown?.any { lk -> lk.equals(l, ignoreCase = true) } == true } }

                        val locationsList = listOf(
                            "All", "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Tirunelveli", 
                            "Tiruppur", "Erode", "Vellore", "Thoothukudi", "Dindigul", "Thanjavur", "Kanchipuram", 
                            "Nagercoil", "Karur", "Cuddalore", "Hosur", "Nagapattinam", "Kumbakonam", "Sivakasi", 
                            "Namakkal", "Dharmapuri", "Krishnagiri", "Villupuram", "Tenkasi", "Pudukkottai", 
                            "Ramanathapuram", "Virudhunagar", "Ariyalur", "Perambalur", "Mayiladuthurai", 
                            "Pollachi", "Mettur", "Udumalaipettai", "Ranipet", "Ambur", "Tiruvannamalai", 
                            "Karaikudi", "Rajapalayam", "Bodinayakanur", "Coonoor", "Udhagamandalam (Ooty)", 
                            "Valparai", "Chengalpattu", "Tiruvallur", "Kallakurichi"
                        )
                        val locationOptions = locationsList.map { loc -> loc to if (loc == "All") allInfluencers.size else allInfluencers.count { it.location.equals(loc, ignoreCase = true) } }

                        FilterDropdown("Category", selectedCategories, categoryOptions, onCategoryToggle, Modifier.width(120.dp), searchable = true)
                        FilterDropdown("Platform", selectedPlatforms, platformOptions, onPlatformToggle, Modifier.width(110.dp), searchable = false)
                        FilterDropdown("Followers", selectedFollowerRanges, followerOptions, onFollowerRangeToggle, Modifier.width(110.dp), searchable = true)
                        FilterDropdown("Gender", selectedGenders, genderOptions, onGenderToggle, Modifier.width(100.dp), searchable = false)
                        FilterDropdown("Mother Tongue", selectedMotherTongues, mtOptions, onMotherTongueToggle, Modifier.width(140.dp), searchable = true)
                        FilterDropdown("Languages", selectedLanguagesKnown, lkOptions, onLanguagesKnownToggle, Modifier.width(120.dp), searchable = true)
                        FilterDropdown("Location", selectedLocations, locationOptions, onLocationToggle, Modifier.width(120.dp), searchable = true)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${filteredInfluencers.size} Influencers Found", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        if (!selectedPlatforms.contains("All") || !selectedCategories.contains("All") || !selectedFollowerRanges.contains("All") || 
                            !selectedGenders.contains("All") || !selectedMotherTongues.contains("All") || !selectedLanguagesKnown.contains("All") || 
                            !selectedLocations.contains("All") || searchQuery.isNotEmpty()) {
                            Text("Clear All", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {
                                onPlatformToggle("All")
                                onCategoryToggle("All")
                                onFollowerRangeToggle("All")
                                onGenderToggle("All")
                                onMotherTongueToggle("All")
                                onLanguagesKnownToggle("All")
                                onLocationToggle("All")
                                onSearchQueryChange("")
                            })
                        }
                    }
                }
            }

            // ---------------- RESULTS SECTION ----------------
            if (isLoading) {
                items(4) {
                    SkeletonCard(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), height = 120.dp)
                }
            } else if (filteredInfluencers.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No influencers found",
                        subtitle = "Try adjusting your filters."
                    )
                }
            } else {
                items(paginatedInfluencers) { influencer ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        BrandCardBrand(
                            influencer = influencer,
                            isWishlisted = wishlistedInfluencers.any { it.id == influencer.id },
                            onWishlistToggle = { onWishlistToggle(influencer) },
                            modifier = Modifier.fillMaxWidth(),
                            onCardClick = { onInfluencerClick(influencer.id) },
                            selectedPlatform = if (selectedPlatforms.contains("All")) null else selectedPlatforms.firstOrNull { it != "All" }
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
                                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                                enabled = currentPage > 1,
                                modifier = Modifier.size(36.dp)
                            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous") }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$currentPage / $totalPages", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                                enabled = currentPage < totalPages,
                                modifier = Modifier.size(36.dp)
                            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next") }
                        }
                    }
                }
            }
        }
    }
}
