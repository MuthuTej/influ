package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import np.com.bimalkafle.firebaseauthdemoapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(navController: NavController) {
    val themeColor = Color(0xFFFF8383)
    var selectedBottomNavItem by remember { mutableStateOf("Home") }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

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
                                Text("Wishlist", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        },
                        colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent),
                        actions = {
                            IconButton(onClick = { /*TODO: Implement notification logic*/ }) {
                                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Saved brands you're interested in collaborating with",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { /*TODO*/ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Saved 14", color = themeColor)
                        }
                        Button(
                            onClick = { /*TODO*/ },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Recently viewed 13", color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                            onClick = { navController.navigate("influencer_create_proposal") },
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
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Recently viewed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    repeat(5) {
                        Image(
                            painter = painterResource(id = R.drawable.splash1),
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Saved brands ❤️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = screenWidth / 2 - 24.dp),
                    modifier = Modifier
                        .height(600.dp) // Adjust height as needed
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
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun WishlistScreenPreview() {
    WishlistScreen(navController = rememberNavController())
}
