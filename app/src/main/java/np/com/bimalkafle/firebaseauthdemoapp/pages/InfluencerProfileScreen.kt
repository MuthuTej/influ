package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.model.InstagramMetrics
import np.com.bimalkafle.firebaseauthdemoapp.model.YouTubeInsights
import np.com.bimalkafle.firebaseauthdemoapp.model.Platform
import np.com.bimalkafle.firebaseauthdemoapp.model.YoutubeDemographics
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import np.com.bimalkafle.firebaseauthdemoapp.model.Category
import np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo

@Composable
fun InfluencerProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    influencerViewModel: InfluencerViewModel
) {
    val influencerProfile by influencerViewModel.influencerProfile.observeAsState()
    val isLoading by influencerViewModel.loading.observeAsState(initial = false)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val firebaseToken = result.token
            if (firebaseToken != null) {
                influencerViewModel.fetchInfluencerDetails(firebaseToken)
                // Clear Coil cache
                clearCoilCache(context)
            }
        }
    }

    InfluencerProfileContent(
        modifier = modifier,
        influencerProfile = influencerProfile,
        isLoading = isLoading,
        onSignOut = {
            authViewModel.signout()
            navController.navigate("login") {
                popUpTo(0)
            }
        },
        onUpdateProfile = { updatedProfile ->
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                val firebaseToken = result.token
                if (firebaseToken != null) {
                    influencerViewModel.updateInfluencerProfile(
                        token = firebaseToken,
                        name = updatedProfile.name,
                        bio = updatedProfile.bio ?: "",
                        location = updatedProfile.location ?: "",
                        logoUrl = updatedProfile.logoUrl ?: "",
                        categories = updatedProfile.categories ?: emptyList(),
                        platforms = updatedProfile.platforms ?: emptyList(),
                        pricing = updatedProfile.pricing ?: emptyList(),
                        availability = updatedProfile.availability ?: true
                    ) { success ->
                        if (success) {
                            influencerViewModel.fetchInfluencerDetails(firebaseToken)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.navigationBarsPadding()) {
                    CmnBottomNavigationBar(
                        selectedItem = "Profile",
                        onItemSelected = { /* Handled in the component */ },
                        navController = navController,
                        isBrand = false
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InfluencerProfileContent(
    modifier: Modifier = Modifier,
    influencerProfile: InfluencerProfile?,
    isLoading: Boolean,
    onSignOut: () -> Unit,
    onUpdateProfile: (InfluencerProfile) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var isEditMode by remember { mutableStateOf(false) }

    // State for editable fields
    var name by remember(influencerProfile) { mutableStateOf(influencerProfile?.name ?: "") }
    var email by remember(influencerProfile) { mutableStateOf(influencerProfile?.email ?: "") }
    var bio by remember(influencerProfile) { mutableStateOf(influencerProfile?.bio ?: "") }
    var location by remember(influencerProfile) { mutableStateOf(influencerProfile?.location ?: "") }
    var logoUrl by remember(influencerProfile) { mutableStateOf(influencerProfile?.logoUrl ?: "") }
    var availability by remember(influencerProfile) { mutableStateOf(influencerProfile?.availability ?: true) }
    
    // Structured Categories State
    val editableCategories = remember(influencerProfile) { 
        mutableStateListOf<Category>().apply {
            addAll(influencerProfile?.categories ?: emptyList()) 
        } 
    }
    
    var platforms by remember(influencerProfile) { mutableStateOf(influencerProfile?.platforms ?: emptyList()) }
    
    // Services setup
    val availablePlatforms = listOf("Instagram", "YouTube", "Facebook")
    val servicesByPlatform = mapOf(
        "Instagram" to listOf("Story", "Reel", "Post"),
        "YouTube" to listOf("Video", "Shorts", "Community Post"),
        "Facebook" to listOf("Post", "Story", "Video", "Shorts")
    )
    
    var selectedPricing = remember(influencerProfile) { 
        mutableStateMapOf<String, MutableMap<String, String>>().apply {
            influencerProfile?.pricing?.forEach { info ->
                val platformKey = availablePlatforms.find { it.equals(info.platform, ignoreCase = true) } ?: info.platform
                val platformMap = getOrPut(platformKey) { mutableMapOf() }
                
                // Match deliverable name with servicesByPlatform keys if possible
                val serviceKey = servicesByPlatform[platformKey]?.find { it.equals(info.deliverable, ignoreCase = true) } ?: info.deliverable
                platformMap[serviceKey] = info.price.toString()
            }
        }
    }

    val themeColor = Color(0xFFFF8383)
    val detailSoftGray = Color(0xFFF8F9FA)
    val detailDarkerGray = Color(0xFF6C757D)
    val platformsColors = mapOf(
        "INSTAGRAM" to Color(0xFFF8CA43),
        "YOUTUBE" to Color(0xFFFA4A4A),
        "X" to Color(0xFF000000),
        "TWITTER" to Color(0xFF1DA1F2)
    )

    Scaffold(
        bottomBar = bottomBar
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = themeColor)
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                // Reduced and elegant header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(themeColor)
                        .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vector),
                        contentDescription = null,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.15f),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 24.dp, bottom = 32.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(90.dp),
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            if (!logoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = themeColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isEditMode) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                ),
                                label = { Text("Influencer Name") },
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            )
                        } else {
                            Text(
                                text = name.ifEmpty { "Influencer Name" },
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = influencerProfile?.role ?: "INFLUENCER",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Attractive Details Section
                Column(modifier = Modifier.padding(20.dp)) {
                    
                    InfluencerDetailSection(icon = Icons.Default.Email, title = "Email Address") {
                        if (isEditMode) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                label = { Text("Email") }
                            )
                        } else {
                            Text(text = email.ifEmpty { "N/A" }, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    InfluencerDetailSection(icon = Icons.Default.Info, title = "Bio") {
                        if (isEditMode) {
                            OutlinedTextField(
                                value = bio,
                                onValueChange = { bio = it },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                                shape = RoundedCornerShape(12.dp),
                                label = { Text("About You") }
                            )
                        } else {
                            Text(
                                text = bio.ifEmpty { "No bio available." },
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    InfluencerDetailSection(icon = Icons.Default.LocationOn, title = "Location") {
                        if (isEditMode) {
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                label = { Text("Location") }
                            )
                        } else {
                            Text(text = location.ifEmpty { "N/A" }, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    InfluencerDetailSection(icon = Icons.Default.EventAvailable, title = "Availability Status") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (availability) "Currently Available" else "Currently Busy",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isEditMode) {
                                Switch(
                                    checked = availability,
                                    onCheckedChange = { availability = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = themeColor,
                                        checkedTrackColor = Color.White,
                                        checkedBorderColor = themeColor
                                    )
                                )
                            }
                        }
                    }

                    InfluencerDetailSection(icon = Icons.Default.Category, title = "Categories") {
                        if (isEditMode) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                editableCategories.forEachIndexed { index, cat ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = cat.category,
                                            onValueChange = { newVal -> editableCategories[index] = cat.copy(category = newVal) },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Category") },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        OutlinedTextField(
                                            value = cat.subCategories.firstOrNull() ?: "",
                                            onValueChange = { newVal -> editableCategories[index] = cat.copy(subCategories = listOf(newVal)) },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Sub-category") },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        IconButton(onClick = { editableCategories.removeAt(index) }) {
                                            Icon(Icons.Default.RemoveCircle, contentDescription = "Remove", tint = Color.Red)
                                        }
                                    }
                                }
                                Button(
                                    onClick = { editableCategories.add(Category("", emptyList())) },
                                    modifier = Modifier.align(Alignment.Start),
                                    colors = ButtonDefaults.buttonColors(containerColor = themeColor.copy(alpha = 0.1f), contentColor = themeColor)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Text("Add Category")
                                }
                            }
                        } else {
                            if (editableCategories.isEmpty()) {
                                Text("No categories specified", color = Color.Gray, fontSize = 15.sp)
                            } else {
                                androidx.compose.foundation.layout.FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    editableCategories.forEach { category ->
                                        Surface(
                                            color = themeColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                Text(
                                                    text = category.category,
                                                    color = themeColor,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = category.subCategories.joinToString(", "),
                                                    color = themeColor.copy(alpha = 0.7f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    influencerProfile?.youtubeInsights?.let { ytInsights ->
                        YouTubeInsightsSection(ytInsights, platformsColors, detailDarkerGray)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    influencerProfile?.instagramMetrics?.let { instaMetrics ->
                        InstagramInsightsSection(instaMetrics, platformsColors, detailDarkerGray, detailSoftGray)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    InfluencerDetailSection(icon = Icons.Default.Payments, title = "Services & Pricing") {
                        if (isEditMode) {
                            availablePlatforms.forEach { platform ->
                                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                                    Text(text = platform, fontWeight = FontWeight.Bold, color = themeColor, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    servicesByPlatform[platform]?.forEach { service ->
                                        val currentPrice = selectedPricing[platform]?.get(service) ?: ""
                                        OutlinedTextField(
                                            value = currentPrice,
                                            onValueChange = { newVal ->
                                                if (newVal.isEmpty()) {
                                                    selectedPricing[platform]?.remove(service)
                                                } else {
                                                    val platformMap = selectedPricing.getOrPut(platform) { mutableMapOf() }
                                                    platformMap[service] = newVal
                                                }
                                            },
                                            label = { Text("$service Price (INR)") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                        )
                                    }
                                }
                            }
                        } else {
                            if (influencerProfile?.pricing?.isEmpty() != false) {
                                Text("No pricing information", color = Color.Gray, fontSize = 15.sp)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    influencerProfile.pricing.forEach { pricing ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = themeColor.copy(alpha = 0.1f),
                                                    shape = CircleShape,
                                                    modifier = Modifier.size(8.dp)
                                                ) {}
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = "${pricing.platform} - ${pricing.deliverable}", color = Color.Black, fontSize = 14.sp)
                                            }
                                            Text(text = "₹${pricing.price}", fontWeight = FontWeight.ExtraBold, color = themeColor, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isEditMode) {
                                    val updatedPricing = mutableListOf<PricingInfo>()
                                    selectedPricing.forEach { (platform, serviceMap) ->
                                        serviceMap.forEach { (service, price) ->
                                            if (price.isNotEmpty()) {
                                                updatedPricing.add(PricingInfo(platform, service, null, price.toIntOrNull() ?: 0, "INR"))
                                            }
                                        }
                                    }

                                    val updatedProfile = influencerProfile?.copy(
                                        name = name,
                                        email = email,
                                        bio = bio,
                                        location = location,
                                        logoUrl = logoUrl,
                                        categories = editableCategories.toList(),
                                        availability = availability,
                                        platforms = platforms,
                                        pricing = updatedPricing
                                    )
                                    if (updatedProfile != null) {
                                        onUpdateProfile(updatedProfile)
                                    }
                                }
                                isEditMode = !isEditMode
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(if (isEditMode) Icons.Default.Save else Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = if (isEditMode) "Save Changes" else "Edit Profile", fontWeight = FontWeight.Bold)
                        }

                        if (!isEditMode) {
                            IconButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(16.dp))
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = "Log Out", tint = Color.Red)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun InfluencerDetailSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFF8383),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1000000 -> "${String.format("%.1f", count / 1000000.0)}M"
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}

@Composable
private fun YouTubeInsightsSection(insights: np.com.bimalkafle.firebaseauthdemoapp.model.YouTubeInsights, platformsColors: Map<String, Color>, detailDarkerGray: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("YouTube Insights", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Channel: ${insights.title ?: "N/A"}", color = detailDarkerGray, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            YouTubeStatCard(label = "Subscribers", value = formatCount(insights.subscribers ?: 0), icon = Icons.Default.People, modifier = Modifier.weight(1f), platformsColors = platformsColors, detailDarkerGray = detailDarkerGray)
            YouTubeStatCard(label = "Total Views", value = formatCount(insights.totalViews?.toInt() ?: 0), icon = Icons.Default.Visibility, modifier = Modifier.weight(1f), platformsColors = platformsColors, detailDarkerGray = detailDarkerGray)
            YouTubeStatCard(label = "Videos", value = (insights.totalVideos ?: 0).toString(), icon = Icons.Default.VideoLibrary, modifier = Modifier.weight(1f), platformsColors = platformsColors, detailDarkerGray = detailDarkerGray)
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (!insights.demographics.isNullOrEmpty()) { YouTubeDemographicsCard(insights.demographics!!, detailDarkerGray) }
        if (!insights.lastSynced.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Last Synced: ${insights.lastSynced}", color = detailDarkerGray, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun YouTubeStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, platformsColors: Map<String, Color>, detailDarkerGray: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = platformsColors["YOUTUBE"] ?: Color.Red, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, fontSize = 11.sp, color = detailDarkerGray)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YouTubeDemographicsCard(demographics: List<np.com.bimalkafle.firebaseauthdemoapp.model.YoutubeDemographics>, detailDarkerGray: Color) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("YouTube Audience Demographics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Age Groups", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val ageData = demographics.groupBy { d -> d.ageGroup ?: "Other" }.mapValues { entry -> entry.value.sumOf { (it.percentage ?: 0f).toDouble() }.toFloat() }
                    val values = ageData.values.toList()
                    val labels = ageData.keys.toList()
                    val colors = listOf(Color(0xFF6C63FF), Color(0xFFFF8383), Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFF2196F3), Color(0xFF9C27B0)).take(values.size)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        InfluencerDonutChart(values, colors, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp)
                        Text("${values.sum().toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.foundation.layout.FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEachIndexed { index, label ->
                            Row(modifier = Modifier.fillMaxWidth(0.45f).padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(colors[index], CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label.removePrefix("age"), fontSize = 10.sp, color = detailDarkerGray)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gender", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val genderData = demographics.groupBy { d -> d.gender ?: "Other" }.mapValues { entry -> entry.value.sumOf { (it.percentage ?: 0f).toDouble() }.toFloat() }
                    val values = genderData.values.toList()
                    val labels = genderData.keys.toList()
                    val colors = listOf(Color(0xFF64B5F6), Color(0xFFF06292), Color(0xFF9E9E9E)).take(values.size)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                        InfluencerDonutChart(values, colors, modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp)
                        Text("${values.sum().toInt()}%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        labels.forEachIndexed { index, label ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(colors[index], CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, color = detailDarkerGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstagramInsightsSection(metrics: np.com.bimalkafle.firebaseauthdemoapp.model.InstagramMetrics, platformsColors: Map<String, Color>, detailDarkerGray: Color, detailSoftGray: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Instagram Insights", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Detailed Platform Analytics", color = detailDarkerGray, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InstagramStatCard(
                label = "Avg Likes",
                value = formatCount(metrics.avgLikes?.toInt() ?: 0),
                icon = Icons.Default.Favorite,
                modifier = Modifier.weight(1f),
                platformsColors = platformsColors,
                detailDarkerGray = detailDarkerGray
            )
            InstagramStatCard(
                label = "Avg Comments",
                value = formatCount(metrics.avgComments?.toInt() ?: 0),
                icon = Icons.Default.Comment,
                modifier = Modifier.weight(1f),
                platformsColors = platformsColors,
                detailDarkerGray = detailDarkerGray
            )
            InstagramStatCard(
                label = "Avg Views",
                value = formatCount(metrics.avgViews?.toInt() ?: 0),
                icon = Icons.Default.Visibility,
                modifier = Modifier.weight(1f),
                platformsColors = platformsColors,
                detailDarkerGray = detailDarkerGray
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Posting Frequency", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Average days between posts", color = detailDarkerGray, fontSize = 12.sp)
                    }
                    Text(
                        text = "${metrics.postingFrequencyDays ?: "N/A"} Days",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = platformsColors["INSTAGRAM"] ?: Color.Red
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = detailSoftGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Posts Analyzed", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Sample size for metrics", color = detailDarkerGray, fontSize = 12.sp)
                    }
                    Text(
                        text = (metrics.totalPostsAnalyzed ?: 0).toString(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                }
            }
        }

        if (!metrics.updatedAt.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Last Updated: ${metrics.updatedAt}", color = detailDarkerGray, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InstagramStatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, platformsColors: Map<String, Color>, detailDarkerGray: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = platformsColors["INSTAGRAM"] ?: Color.Yellow, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, fontSize = 11.sp, color = detailDarkerGray)
        }
    }
}

@Composable
private fun InfluencerDonutChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 20.dp
) {
    val total = values.sum()
    if (total == 0f) return
    var startAngle = -90f
    Canvas(modifier = modifier) {
        values.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            drawArc(color = colors[index], startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round))
            startAngle += sweepAngle
        }
    }
}

// Function to clear Coil cache
fun clearCoilCache(context: Context) {
    val imageLoader = ImageLoader.Builder(context).build()
    imageLoader.memoryCache?.clear() // Corrected line
    imageLoader.diskCache?.clear()
    Log.d("InfluencerProfileScreen", "Coil cache cleared")
}
