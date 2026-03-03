package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel

data class DiscoverBrand(
    val name: String,
    val description: String,
    val category: String,
    val location: String,
    val logo: Int, // Assuming drawable resource ID
    val isFavorite: Boolean,
    val isVerified: Boolean
)

val discoverBrands = listOf(
    DiscoverBrand("L'Oréal", "Beauty and personal care", "Beauty", "Paris, France", R.drawable.splash1, false, true),
    DiscoverBrand("Samsung", "Multinational conglomerate", "Tech", "Seoul, South Korea", R.drawable.splash1, true, true),
    DiscoverBrand("Dior", "Luxury goods company", "Fashion", "Paris, France", R.drawable.splash1, false, false),
    DiscoverBrand("Starbucks", "Coffeehouse chain", "Food", "Seattle, USA", R.drawable.splash1, true, true),
    DiscoverBrand("Louis Vuitton", "Luxury fashion house", "Fashion", "Paris, France", R.drawable.splash1, false, true),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverBrandsScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel
) {
    var selectedPlatform by remember { mutableStateOf("Platform") }
    val platforms = listOf("YouTube", "Instagram", "Facebook")
    var selectedCategory by remember { mutableStateOf("Category") }
    val categories = listOf("Food", "Fashion", "Tech")
    var selectedBudget by remember { mutableStateOf("Budget") }
    val budgets = listOf("$1000", "$5000", "$10000+")
    val themeColor = Color(0xFFFF8383)
    var selectedBottomNavItem by remember { mutableStateOf("Search") }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val unreadCount by notificationViewModel.unreadCount.observeAsState(0)

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            val userId = currentUser.uid
            if (token != null) {
                notificationViewModel.fetchUnreadCount(userId, token)
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
                                Text("Discover", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            Row {
                                IconButton(onClick = { navController.navigate("wishlist") }) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = Color.White)
                                }
                                Box {
                                    IconButton(onClick = { navController.navigate("notifications") }) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                                    }
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
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Find Brands for your next campaign",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        SearchBar("Search")
                        Spacer(modifier = Modifier.height(16.dp))

                        // Filter Chips Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                label = "Platform",
                                options = platforms,
                                selectedOption = selectedPlatform,
                                onOptionSelected = { selectedPlatform = it }
                            )
                            FilterChip(
                                label = "Category",
                                options = categories,
                                selectedOption = selectedCategory,
                                onOptionSelected = { selectedCategory = it }
                            )
                            FilterChip(
                                label = "Budget",
                                options = budgets,
                                selectedOption = selectedBudget,
                                onOptionSelected = { selectedBudget = it }
                            )
                            Spacer(Modifier.weight(1f))
                            // Filter Icon Button
                            FilledIconButton(
                                onClick = { /*TODO: Implement filter logic*/ },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.splash1),
                                    contentDescription = "Filter",
                                    tint = themeColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
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
                            Icon(Icons.Default.Add, contentDescription = "Create Proposal", tint = Color.White)
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = screenWidth / 2 - 24.dp),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(discoverBrands) { brand ->
                BrandDiscoverCard(brand = brand, navController = navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(placeholder: String) {
    var text by remember { mutableStateOf("") }
    TextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(placeholder, color = Color.Gray) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
        singleLine = true
    )
}

@Composable
fun FilterChip(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            color = if (selectedOption != label) Color(0xFFFF8383).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedOption != label) selectedOption else label,
                    color = Color.White,
                    fontSize = 12.sp
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandDiscoverCard(brand: DiscoverBrand, navController: NavController) {
    Card(
        onClick = { /* Navigate to brand detail */ },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(120.dp)) {
                Image(
                    painter = painterResource(id = brand.logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { /* Toggle favorite */ },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (brand.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (brand.isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = brand.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    if (brand.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = brand.category,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = brand.location,
                        color = Color.Gray,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
