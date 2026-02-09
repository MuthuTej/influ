package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

private val brandThemeColor = Color(0xFFFF8383)

@Composable
fun BrandBottomNavigationBar(
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    onCreateCampaign: () -> Unit,
    navController: NavController
) {
    val items = listOf("Home", "Search", "Connect", "History", "Profile")
    val icons = mapOf(
        "Home" to Icons.Default.Home,
        "Search" to Icons.Default.Search,
        "Connect" to Icons.Default.Chat,
        "History" to Icons.Default.History,
        "Profile" to Icons.Default.Person
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        Icon(icons[item] ?: Icons.Default.Home, contentDescription = item)
                    },
                    label = { Text(item) },
                    selected = selectedItem == item,
                    onClick = {
                        onItemSelected(item)
                        when (item) {
                            "Home" -> {
                                navController.navigate("brand_home") {
                                    popUpTo("brand_home") { inclusive = true }
                                }
                            }
                            "Search" -> navController.navigate("brand_search")
                            "Connect" -> navController.navigate("chatList")
                            "History" -> navController.navigate("brand_history")
                            "Profile" -> navController.navigate("brand_profile")
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = brandThemeColor,
                        unselectedIconColor = Color.Gray,
                        selectedTextColor = brandThemeColor,
                        unselectedTextColor = Color.Gray,
                        indicatorColor = brandThemeColor.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}
