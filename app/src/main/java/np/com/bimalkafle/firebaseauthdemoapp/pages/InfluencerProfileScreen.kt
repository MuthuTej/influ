package np.com.bimalkafle.firebaseauthdemoapp.pages

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.model.Category
import np.com.bimalkafle.firebaseauthdemoapp.model.Platform
import np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.ProfileSectionTitle

@Composable
fun InfluencerProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    influencerViewModel: InfluencerViewModel
) {
    val influencerProfile by influencerViewModel.influencerProfile.observeAsState()
    val isLoading by influencerViewModel.loading.observeAsState(initial = false)

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val firebaseToken = result.token
            if (firebaseToken != null) {
                influencerViewModel.fetchInfluencerDetails(firebaseToken)
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
                        pricing = updatedProfile.pricing ?: emptyList()
                    ) { success ->
                        if (success) {
                            influencerViewModel.fetchInfluencerDetails(firebaseToken)
                        }
                    }
                }
            }
        },
        bottomBar = {
            CmnBottomNavigationBar(
                selectedItem = "Profile",
                onItemSelected = { /* Handled in the component */ },
                navController = navController,
                isBrand = false
            )
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
    var categoriesText by remember(influencerProfile) { mutableStateOf(influencerProfile?.categories?.joinToString(";") { "${it.category},${it.subCategory}" } ?: "") }
    var availability by remember(influencerProfile) { mutableStateOf(influencerProfile?.availability ?: true) }
    
    // Using simple lists for platforms and pricing as they are read-only or handled as a whole
    var platforms by remember(influencerProfile) { mutableStateOf(influencerProfile?.platforms ?: emptyList()) }
    
    // State for services/pricing
    val availablePlatforms = listOf("Instagram", "YouTube", "Facebook")
    val servicesByPlatform = mapOf(
        "Instagram" to listOf("Story", "Reel", "Post"),
        "YouTube" to listOf("Video", "Shorts", "Community Post"),
        "Facebook" to listOf("Post", "Story", "Video")
    )
    
    var selectedPricing = remember(influencerProfile) { 
        mutableStateMapOf<String, MutableMap<String, String>>().apply {
            influencerProfile?.pricing?.forEach { info ->
                val platformMap = getOrPut(info.platform) { mutableStateMapOf() }
                platformMap[info.deliverable] = info.price.toString()
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
                    .padding(padding)
            ) {
                // Profile Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(themeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vector),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.15f),
                        contentScale = ContentScale.Crop
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color.White
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
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(24.dp),
                                    tint = themeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            )
                        } else {
                            Text(
                                text = name.ifEmpty { "Influencer Name" },
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = influencerProfile?.role ?: "INFLUENCER",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }

                // Profile Details
                Column(modifier = Modifier.padding(16.dp)) {
                    
                    // Email Section
                    ProfileSectionTitle("Email Address")
                    if (isEditMode) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text("Email") }
                        )
                    } else {
                        Text(text = email.ifEmpty { "N/A" }, color = Color.DarkGray, fontSize = 14.sp)
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Bio")
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
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Location")
                    if (isEditMode) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text("Location") }
                        )
                    } else {
                        Text(text = location.ifEmpty { "N/A" }, color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Logo URL")
                    if (isEditMode) {
                        OutlinedTextField(
                            value = logoUrl,
                            onValueChange = { logoUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text("Logo URL") }
                        )
                    } else {
                        Text(text = logoUrl.ifEmpty { "N/A" }, color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Availability Status")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (availability) "Currently Available" else "Currently Busy",
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                        if (isEditMode) {
                            Switch(
                                checked = availability,
                                onCheckedChange = { availability = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = themeColor,
                                    checkedTrackColor = Color.White,
                                    checkedBorderColor = themeColor,
                                    uncheckedThumbColor = Color.Gray,
                                    uncheckedTrackColor = Color.White,
                                    uncheckedBorderColor = Color.Gray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Categories")
                    if (isEditMode) {
                        OutlinedTextField(
                            value = categoriesText,
                            onValueChange = { categoriesText = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text("Categories (Format: category,subCategory;...)") }
                        )
                    } else {
                        if (influencerProfile?.categories?.isEmpty() != false) {
                            Text("No categories specified", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            influencerProfile.categories.forEach { category ->
                                Text(
                                    text = "${category.category} - ${category.subCategory}",
                                    color = Color.DarkGray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Platforms
                    ProfileSectionTitle("Platforms")
                    if (platforms.isEmpty()) {
                        Text("No platforms added", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        platforms.forEach { platform ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = platform.platform, fontWeight = FontWeight.Bold, color = themeColor)
                                    Text(text = "Followers: ${platform.followers ?: 0}", fontSize = 12.sp)
                                    Text(text = "Avg Views: ${platform.avgViews ?: 0}", fontSize = 12.sp)
                                    Text(text = "Engagement: ${platform.engagement ?: 0}%", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pricing Details
                    ProfileSectionTitle("Services & Pricing")
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
                            Text("No pricing information", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            influencerProfile.pricing.forEach { pricing ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "${pricing.platform} - ${pricing.deliverable}", color = Color.DarkGray, fontSize = 14.sp)
                                    Text(text = "${pricing.currency} ${pricing.price}", fontWeight = FontWeight.Bold, color = themeColor)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Edit/Save Button
                    Button(
                        onClick = {
                            if (isEditMode) {
                                val updatedCategories = categoriesText.split(";").mapNotNull { cat ->
                                    val parts = cat.split(",")
                                    if (parts.size == 2) {
                                        Category(parts[0].trim(), parts[1].trim())
                                    } else {
                                        null
                                    }
                                }

                                val updatedPricing = mutableListOf<PricingInfo>()
                                selectedPricing.forEach { (platform, serviceMap) ->
                                    serviceMap.forEach { (service, price) ->
                                        if (price.isNotEmpty()) {
                                            updatedPricing.add(PricingInfo(platform, service, price.toIntOrNull() ?: 0, "INR"))
                                        }
                                    }
                                }

                                val updatedProfile = influencerProfile?.copy(
                                    name = name,
                                    email = email,
                                    bio = bio,
                                    location = location,
                                    logoUrl = logoUrl,
                                    categories = updatedCategories,
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
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Icon(
                            if (isEditMode) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditMode) "Save Changes" else "Edit Profile",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Log Out Button
                    Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InfluencerProfilePreview() {
    FirebaseAuthDemoAppTheme {
        InfluencerProfileContent(
            influencerProfile = null,
            isLoading = false,
            onSignOut = {},
            onUpdateProfile = {}
        )
    }
}
