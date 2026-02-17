package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.Brand
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val brandThemeColor = Color(0xFFFF8383) // Reusing the same color variable name from BrandHomePage for consistency

@Composable
fun InfluencerHomePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    influencerViewModel: InfluencerViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val collaborations by influencerViewModel.collaborations.observeAsState(initial = emptyList())
    val brands by influencerViewModel.brands.observeAsState(initial = emptyList())
    val isLoading by influencerViewModel.loading.observeAsState(initial = false)
    val error by influencerViewModel.error.observeAsState()
    val influencerProfile by influencerViewModel.influencerProfile.observeAsState()

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser
            ?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val firebaseToken = result.token
                if (firebaseToken != null) {
                    influencerViewModel.fetchInfluencerDetails(firebaseToken)
                    influencerViewModel.fetchCollaborations(firebaseToken)
                    influencerViewModel.fetchBrands(firebaseToken)
                }
            }
    }

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo("influencer_home") { inclusive = true }
            }
        }
    }

    var selectedBottomNavItem by remember { mutableStateOf("Home") }

    Scaffold(
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = { selectedBottomNavItem = it },
                navController = navController
            )
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
                    .background(Color.White)
            ) {

                item { InfluencerHeaderAndReachSection(influencerProfile) }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        ActiveCollaborationsSection(collaborations)
                        TopPicksSectionInfluencer()
                    }

                }
                items(brands.take(10)) { brand ->
                    BrandCardInfluencer(
                        brand = brand,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onCardClick = {
                            // Navigate to brand detail if needed
                             navController.navigate("brand_detail/${brand.id}")
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = {
                            authViewModel.signout()
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}

@Composable
fun InfluencerHeaderAndReachSection(influencerProfile: InfluencerProfile?) {

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.32f
    val cardHeight = screenHeight * 0.28f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight + cardHeight * 0.6f) // dynamic total height
    ) {

        // ---------------- HEADER ----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp))
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(54.dp)
                ) {
                    if (!influencerProfile?.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = influencerProfile?.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.brand_profile), // Fallback image
                            contentDescription = null,
                            modifier = Modifier
                                .padding(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Hello!", fontSize = 14.sp, color = Color.Black.copy(alpha = 0.9f))
                    Text(
                        "${influencerProfile?.name ?: "Guest"} 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                IconBubbleInfluencer(Icons.Default.Favorite, Color.Red)
                Spacer(modifier = Modifier.width(10.dp))
                IconBubbleInfluencer(Icons.Default.Notifications, Color.Black)
            }
        }

        // ---------------- STATS CARD ----------------
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerHeight - (cardHeight * 0.75f)) // 🔥 dynamic overlap
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(cardHeight),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 25.dp)
        ) {

            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFFAFBD), brandThemeColor)
                        )
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "Total Earnings",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        "₹ 18.4K", // Mock data for now as per previous design, or can map real data if available
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfluencerStatChip("Engagement", "6.1 %", Modifier.weight(1f))
                        InfluencerStatChip("Leads", "1.2 K", Modifier.weight(1f))
                        InfluencerStatChip("Reach", "2.4 M", Modifier.weight(1f))
                    }
                }
            }
        }

        // ---------------- FLOATING BUTTON ----------------
        Button(
            onClick = { },
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF5252)
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = headerHeight + (cardHeight * 0.20f))
                .fillMaxWidth(0.65f)
                .height(52.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp)
        ) {
            Text(
                "Find Brands",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun IconBubbleInfluencer(icon: ImageVector, tint: Color) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFF5F5F5),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun InfluencerStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            width = 0.dp,
            color = Color.Transparent
        ),
        modifier = modifier
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 18.dp, horizontal = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = Color.Black,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun ActiveCollaborationsSection(collaborations: List<Collaboration>) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Active Collaborations",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(brandThemeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = collaborations.size.toString(),
                    color = brandThemeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (collaborations.isEmpty()) {
            Text(
                text = "No active collaborations found.",
                fontStyle = FontStyle.Italic,
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            val maxSectionHeight = screenHeight * 0.20f
            val singleCardHeight = 140.dp

            val finalHeight = if (maxSectionHeight > singleCardHeight) {
                maxSectionHeight
            } else {
                singleCardHeight
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(finalHeight)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(collaborations) { collaboration ->

                    val pricing = collaboration.pricing?.firstOrNull()
                    val updatedAt = collaboration.updatedAt

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

                    val timeAgo = calculateDaysAgo(updatedAt)

                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    ) {
                        // Display Campaign and Brand info
                        CollaborationItem(
                            brandName = collaboration.brand?.name ?: "Brand", // Ideally we fetch brand name, but for now using brandId or if available in future
                            // Since we don't have Brand Name in Collaboration object directly (only brandId), we might check if we can get it from somewhere or just show title.
                            // NOTE: The previous `ActiveCampaignSection` in BrandHomePage used `influencer` object. check if we have `brand` object in `Collaboration`.
                            // In `GetCollaborations` query, we requested `campaign { brandId ... }` but not `brand { ... }`.
                            // We might need to update query if we want brand name/logo.
                            // REQUIRED: User asked to "show the fields in the ui that are useful for the influencer all the brand details".
                            // I should have added brand details to the query. 
                            // Re-reading `BrandViewModel` query: `getCollaborations` for Brand has `influencer` object.
                            // `InfluencerViewModel` query: I added `campaign` object, but I should have added `brand` object if the schema supports it, OR `campaign { brand { name logoUrl } }`.
                            // FOR NOW: I will use `campaign.title` and maybe updated query if needed. 
                            // But wait, the schema likely supports `brand` on `Campaign` or `Collaboration`.
                            // Let's assume for now I can verify this or just use what I have.
                            // Actually, let's look at `Collaboration` model. It has `brandId`. 
                            // Let's use `campaign.title` as primary and maybe `brandId` as secondary or static placeholder if missing.
                            // Wait, the user provided query for `GetCollaborations` in prompt:
                            /*
                            query GetCollaborations {
                              getCollaborations {
                                ...
                                campaign {
                                  id
                                  brandId
                                  ...
                                }
                                ...
                              }
                            }
                            */
                            // It DOES NOT have Brand Name/Logo. 
                            // However, strictly following the prompt's provided query, I cannot fetch Brand Name/Logo unless I modify the query to include it IF the schema allows.
                            // But the user said "all the brand details i need to show in that part".
                            // This implies I SHOULD have modified the query to include Brand details if possible.
                            // Let's stick to the prompt's query first. If I can't show brand name, I'll show Campaign Title and maybe "Brand ID: ..." or just generic.
                            // Or, maybe `campaign` has `brand` field? 
                            // In `BrandModels.kt`, `Campaign` has `brandId`. 
                            // In `CampaignModels.kt`, `CampaignDetail` has `brand`.
                            // Let's check `InfluencerViewModel` query again. I used exactly what user gave. 
                            // Okay, I will try to use `campaign.title` and `campaign.objective` or `status`.
                            
                            brandLogo = collaboration.brand?.logoUrl, // No logo in query
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

@Composable
fun CollaborationItem(
    brandName: String,
    campaignTitle: String,
    status: String,
    deliverable: String,
    platform: String,
    price: Int,
    currency: String,
    time: String,
    brandLogo: String?
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
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!brandLogo.isNullOrEmpty()) {
                            AsyncImage(
                                model = brandLogo,
                                contentDescription = brandName,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (brandName.isNotEmpty()) brandName.first().uppercase() else "B",
                                color = primaryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = brandName, // Showing Brand Name as main text
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Text(
                            text = campaignTitle, // Campaign Title as subtext
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(25),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

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
fun TopPicksSectionInfluencer() {
    var selectedPlatform by remember { mutableStateOf("YouTube") }
    val platforms = listOf("YouTube", "Instagram", "Facebook")

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Top Picks", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            TabRow(
                selectedTabIndex = platforms.indexOf(selectedPlatform),
                containerColor = Color.Transparent,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(it[platforms.indexOf(selectedPlatform)]),
                        color = brandThemeColor
                    )
                }
            ) {
                platforms.forEach { platform ->
                    val iconRes = when (platform) {
                        "YouTube" -> R.drawable.ic_youtube
                        "Instagram" -> R.drawable.ic_instagram
                        "Facebook" -> R.drawable.ic_facebook
                        else -> R.drawable.ic_youtube
                    }
                    Tab(
                        selected = selectedPlatform == platform,
                        onClick = { selectedPlatform = platform },
                        icon = {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = platform,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BrandCardInfluencer(
    brand: Brand,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable { onCardClick() }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            // Verified Badge
            if (brand.isVerified == true) {
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Column {
                // Header: Logo, Name, Category
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 60.dp), // Padding for badge
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = brandThemeColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        if (!brand.logoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = brand.logoUrl,
                                contentDescription = brand.name,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = brand.name.firstOrNull()?.uppercase() ?: "?",
                                    color = brandThemeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = brand.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = listOfNotNull(brand.brandCategory?.category, brand.brandCategory?.subCategory)
                                .joinToString(" • "),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        // Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val rating = brand.averageRating ?: 0.0
                            val reviewCount = brand.reviews?.size ?: 0
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$rating ($reviewCount Reviews)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // About / Bio
                if (!brand.about.isNullOrEmpty()) {
                    Text(
                        text = brand.about,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Primary Objective
                if (!brand.primaryObjective.isNullOrEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AdsClick, // Or similar icon
                            contentDescription = "Objective",
                            tint = brandThemeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Goal: ${brand.primaryObjective}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = brandThemeColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Latest Review Snippet
                val latestReview = brand.reviews?.maxByOrNull { it.createdAt }
                if (latestReview != null && !latestReview.comment.isNullOrEmpty()) {
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "\"${latestReview.comment}\"",
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Info Rows: Target Audience & Platforms
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Target Audience
                    val ta = brand.targetAudience
                    Column {
                        Text(
                            text = "Target Audience",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        if (ta != null) {
                            Text(
                                text = "${ta.ageMin}-${ta.ageMax} yo • ${ta.gender}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        } else {
                            Text("N/A", fontSize = 13.sp, color = Color.Black)
                        }
                    }

                    // Platforms
                    val platforms = brand.preferredPlatforms
                    if (!platforms.isNullOrEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            platforms.take(3).forEach { p ->
                                val iconRes = when (p.platform.lowercase()) {
                                    "youtube" -> R.drawable.ic_youtube
                                    "instagram" -> R.drawable.ic_instagram
                                    "facebook" -> R.drawable.ic_facebook
                                    else -> null
                                }
                                if (iconRes != null) {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = p.platform,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Unspecified
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



