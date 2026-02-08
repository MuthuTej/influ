package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme

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
fun DiscoverBrandsScreen(navController: NavController) {
    var selectedPlatform by remember { mutableStateOf("Platform") }
    val platforms = listOf("YouTube", "Instagram", "Facebook")
    var selectedCategory by remember { mutableStateOf("Category") }
    val categories = listOf("Food", "Fashion", "Tech")
    var selectedBudget by remember { mutableStateOf("Budget") }
    val budgets = listOf("$1000", "$5000", "$10000+")
    val themeColor = Color(0xFFFF8383)
    var selectedBottomNavItem by remember { mutableStateOf("Search") }

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
                                IconButton(onClick = { /*TODO: Implement notification logic*/ }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
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
                            onClick = { /*TODO*/ },
                            containerColor = themeColor,
                            shape = CircleShape,
                            modifier = Modifier.offset(y = (-16).dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    } else {
                        NavigationBarItem(
                            icon = { Icon(icons[item]!!, contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedBottomNavItem == item,
                            onClick = { 
                                selectedBottomNavItem = item 
                                if(item == "Home")
                                    navController.navigate("influencer_home")
                                else if(item == "Search")
                                    navController.navigate("discover")
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
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            items(discoverBrands.chunked(2)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    it.forEach { brand ->
                        BrandDiscoverCard(brand = brand, modifier = Modifier.weight(1f), navController = navController)
                    }
                    if (it.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        colors = TextFieldDefaults.textFieldColors(
            containerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun FilterChip(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(1.dp, Color.White)
        ) {
            Text(selectedOption)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onOptionSelected(it); expanded = false })
            }
        }
    }
}

@Composable
fun BrandDiscoverCard(brand: DiscoverBrand, modifier: Modifier = Modifier, navController: NavController) {
    var isFavorite by remember { mutableStateOf(brand.isFavorite) }
    val themeColor = Color(0xFFFF8383)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Column {
            Box(contentAlignment = Alignment.TopEnd) {
                Image(
                    painter = painterResource(id = brand.logo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                IconButton(onClick = { 
                    isFavorite = !isFavorite 
                    if(isFavorite) navController.navigate("wishlist")
                }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else themeColor
                    )
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(brand.name, fontWeight = FontWeight.Bold)
                    if (brand.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(brand.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                Text(brand.category, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
                Text(brand.location, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DiscoverBrandsScreenPreview() {
    FirebaseAuthDemoAppTheme {
        DiscoverBrandsScreen(navController = rememberNavController())
    }
}
