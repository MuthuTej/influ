package np.com.bimalkafle.firebaseauthdemoapp.pages

import coil.compose.AsyncImage

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import androidx.compose.ui.tooling.preview.Preview
import np.com.bimalkafle.firebaseauthdemoapp.R

@Composable
fun BrandHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel
) {

    val authState = authViewModel.authState.observeAsState()
    val collaborations by brandViewModel.collaborations.observeAsState(initial = emptyList())
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val error by brandViewModel.error.observeAsState()

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val firebaseToken = result.token
                if (firebaseToken != null) {
                    brandViewModel.fetchCollaborations(firebaseToken)
                }
            }
    }

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo("brand_home") { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNavigationBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_campaign")},
                containerColor = Color(0xFFFF8383)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF5F5F5)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item { HeaderSection() }
                item { ActiveCampaignSection(collaborations) }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = { authViewModel.signout() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection() {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.32f
    val contentPaddingTop = headerHeight - 40.dp

    Box(
    ) {

        // 🔥 Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFFF8383),
                            Color(0xFF6C63FF)
                        )
                    )
                )
        ) {

            Image(
                painter = painterResource(R.drawable.vector),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.15f),
                contentScale = ContentScale.Crop
            )

            // Top Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Hello, Myntra 👋",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            // 💎 Floating Stats Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 40.dp)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFF4FACFE),
                                    Color(0xFF6C63FF)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text(
                            text = "Total Reach",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "2.4 M",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OverlayStatItem("Engagement", "6.1%")
                            OverlayStatItem("Leads", "1.2K")
                            OverlayStatItem("Spent", "₹18.4K")
                        }
                    }
                }
            }
        }

    }
}
@Composable
private fun OverlayStatItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActiveCampaignSection(collaborations: List<np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration>) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = "Active Campaigns",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (collaborations.isEmpty()) {
            Text(
                text = "No active campaigns found.",
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        } else {
             // Height logic: Show roughly 3 items if possible.
             // Note: Currently using Box with calculated height was for scroll within scroll,
             // but here we are in a parent LazyColumn. Ideally, we shouldn't nest LazyColumn.
             // But the original code had it. To avoid crash "Vertically scrollable component was measured with an infinity maximum height constraints",
             // we either use a fixed height or just loop items in the parent lazy column.
             // The original code used a Box with height and inner LazyColumn, which works but isn't ideal.
             // Let's stick to the original design but map the data.

            val itemHeight = 160.dp // Approximate height of card + padding
            val visibleItems = minOf(collaborations.size, 3)
            val boxHeight = if (visibleItems > 0) itemHeight * visibleItems else 100.dp
            fun calculateDaysAgo(updatedAt: String?): String {
                return try {
                    val instant = Instant.parse(updatedAt)
                    val updatedDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                    val today = LocalDateTime.now().toLocalDate()
                    val days = ChronoUnit.DAYS.between(updatedDate, today)

                    when {
                        days == 0L -> "Today"
                        days == 1L -> "1 day ago"
                        else -> "$days days ago"
                    }
                } catch (e: Exception) {
                    "Recently"
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boxHeight)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(collaborations) { collaboration ->
                        val pricing = collaboration.pricing?.firstOrNull()
                        val updatedAt = collaboration.influencer.updatedAt
                        val timeAgo = calculateDaysAgo(updatedAt)

                        CampaignItem(
                            influencerName = collaboration.influencer.name,
                            influencerLogo = collaboration.influencer.logoUrl,
                            campaignTitle = collaboration.campaign.title,
                            status = collaboration.status,
                            deliverable = pricing?.deliverable ?: "N/A",
                            platform = pricing?.platform ?: "N/A",
                            price = pricing?.price ?: 0,
                            currency = pricing?.currency ?: "USD",
                            time = timeAgo
                        )
                    }
                }
            }
        }
    }
}

// Data class moved/used from model, or mapped directly.
// We are using the domain model directly in the composable for simplicity.

@Composable
fun CampaignItem(
    influencerName: String,
    campaignTitle: String,
    status: String,
    deliverable: String,
    platform: String,
    price: Int,
    currency: String,
    time: String,
    influencerLogo: String?
) {

    val primaryColor = Color(0xFF4CAF50)

    val statusColor = when (status) {
        "ACCEPTED" -> primaryColor
        "PENDING" -> Color(0xFFFFB74D)
        "REJECTED" -> Color(0xFFE57373)
        "IN_PROGRESS" -> Color(0xFF42A5F5)
        "COMPLETED" -> Color(0xFF66BB6A)
        else -> Color.Gray
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!influencerLogo.isNullOrEmpty()) {
                            AsyncImage(
                                model = influencerLogo,
                                contentDescription = influencerName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(
                                text = if (influencerName.isNotEmpty()) influencerName.first().uppercase() else "?",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = influencerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = campaignTitle,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f))

            Text(
                text = "$deliverable • $platform",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currency $price",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = primaryColor
                )
                Text(
                    text = time,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
@Composable
fun BottomNavigationBar() {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Search, null) },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.History, null) },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Profile") }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BrandHomePagePreview() {
    val sampleCollaborations = listOf(
        np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration(
            id = "1",
            status = "ACCEPTED",
            message = "Excited to collaborate!",
            createdAt = "",
            initiatedBy = "BRAND",
            campaign = np.com.bimalkafle.firebaseauthdemoapp.model.Campaign(
                id = "c1",
                title = "Summer Launch 2024"
            ),
            pricing = listOf(
                np.com.bimalkafle.firebaseauthdemoapp.model.Pricing(
                    currency = "USD",
                    deliverable = "Reel",
                    platform = "INSTAGRAM",
                    price = 600
                )
            ),
            influencer = np.com.bimalkafle.firebaseauthdemoapp.model.Influencer(
                name = "testinfluencer",
                bio = null,
                logoUrl = null,
                updatedAt = "2026-02-07T11:35:52.789Z"
            )
        )
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            HeaderSection()
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            ActiveCampaignSection(sampleCollaborations)

        }
    }
}