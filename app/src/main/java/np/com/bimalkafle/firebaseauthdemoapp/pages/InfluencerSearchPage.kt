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
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfluencerSearchPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    influencerViewModel: InfluencerViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val brands by influencerViewModel.brands.observeAsState(initial = emptyList())
    val isLoading by influencerViewModel.loading.observeAsState(initial = false)

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val firebaseToken = result.token
                if (firebaseToken != null) {
                    influencerViewModel.fetchBrands(firebaseToken)
                }
            }
    }

    var selectedPlatform by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedFollowerRange by remember { mutableStateOf("All") }

    // Filter brands based on search query and selected filters
    val filteredBrands = brands.filter { brand ->
        val matchesSearch = brand.name.contains(searchQuery, ignoreCase = true) ||
                (brand.brandCategory?.category?.contains(searchQuery, ignoreCase = true) == true)

        val matchesPlatform = selectedPlatform == "All" ||
                brand.preferredPlatforms?.any { it.platform.equals(selectedPlatform, ignoreCase = true) } == true

        val matchesCategory = selectedCategory == "All" ||
                brand.brandCategory?.category.equals(selectedCategory, ignoreCase = true)

        val matchesFollowers = when (selectedFollowerRange) {
            "All" -> true
            "0-10K" -> brand.preferredPlatforms?.any { (it.followers ?: 0) < 10000 } == true
            "10K-100K" -> brand.preferredPlatforms?.any { (it.followers ?: 0) in 10000..100000 } == true
            "100K-1M" -> brand.preferredPlatforms?.any { (it.followers ?: 0) in 100000..1000000 } == true
            "1M+" -> brand.preferredPlatforms?.any { (it.followers ?: 0) > 1000000 } == true
            else -> true
        }

        matchesSearch && matchesPlatform && matchesCategory && matchesFollowers
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
                            IconBubbleSearch(Icons.Default.Favorite, Color.Red)
                            Spacer(modifier = Modifier.width(10.dp))
                            IconBubbleSearch(Icons.Default.Notifications, Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Discover Brands",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Find brands to collaborate with",
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
                        placeholder = { Text("Search Brands", color = Color.Gray) },
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
                    onOptionSelected = { selectedPlatform = it },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Category",
                    selectedOption = selectedCategory,
                    options = listOf("All", "Tech", "Fashion", "Food", "Lifestyle", "Beauty", "Sports"),
                    onOptionSelected = { selectedCategory = it },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Followers",
                    selectedOption = selectedFollowerRange,
                    options = listOf("All", "0-10K", "10K-100K", "100K-1M", "1M+"),
                    onOptionSelected = { selectedFollowerRange = it },
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
                    items(filteredBrands) { brand ->
                        BrandCardInfluencer(
                            brand = brand,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp), // Adjusted padding
                            onCardClick = {
                                 navController.navigate("brand_detail/${brand.id}")
                            }
                        )
                    }
                    
                    if (filteredBrands.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("No brands found matching your search.", color = Color.Gray, fontWeight = FontWeight.Medium)
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
}}
