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
import np.com.bimalkafle.firebaseauthdemoapp.model.Category
import np.com.bimalkafle.firebaseauthdemoapp.model.Platform
import np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar

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
                                FlowRow(
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

                    InfluencerDetailSection(icon = Icons.Default.Public, title = "Platforms") {
                        if (platforms.isEmpty()) {
                            Text("No platforms added", color = Color.Gray, fontSize = 15.sp)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                platforms.forEach { platform ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = platform.platform.uppercase(), 
                                                fontWeight = FontWeight.ExtraBold, 
                                                color = themeColor,
                                                fontSize = 14.sp,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Followers", fontSize = 11.sp, color = Color.Gray)
                                                    Text(text = formatCount(platform.followers ?: 0), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Avg Views", fontSize = 11.sp, color = Color.Gray)
                                                    Text(text = formatCount(platform.avgViews ?: 0), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Engagement", fontSize = 11.sp, color = Color.Gray)
                                                    Text(text = "${platform.engagement ?: 0}%", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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

// Function to clear Coil cache
fun clearCoilCache(context: Context) {
    val imageLoader = ImageLoader.Builder(context).build()
    imageLoader.memoryCache?.clear() // Corrected line
    imageLoader.diskCache?.clear()
    Log.d("InfluencerProfileScreen", "Coil cache cleared")
}
