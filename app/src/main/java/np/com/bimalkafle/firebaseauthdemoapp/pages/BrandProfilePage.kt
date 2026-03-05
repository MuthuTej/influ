package np.com.bimalkafle.firebaseauthdemoapp.pages

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.model.Brand
import np.com.bimalkafle.firebaseauthdemoapp.model.BrandCategory
import np.com.bimalkafle.firebaseauthdemoapp.model.PreferredPlatform
import np.com.bimalkafle.firebaseauthdemoapp.model.TargetAudience
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar

@Composable
fun BrandProfilePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel
) {
    val brandProfile by brandViewModel.brandProfile.observeAsState()
    val isLoading by brandViewModel.loading.observeAsState(initial = false)

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val firebaseToken = result.token
            if (firebaseToken != null) {
                brandViewModel.fetchBrandDetails(firebaseToken)
            }
        }
    }

    BrandProfileContent(
        modifier = modifier,
        brandProfile = brandProfile,
        isLoading = isLoading,
        onSignOut = {
            authViewModel.signout()
            navController.navigate("login") {
                popUpTo(0)
            }
        },
        onNavigateToCreateCampaign = { navController.navigate("create_campaign") },
        onUpdateProfile = { updatedBrand ->
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                val firebaseToken = result.token
                if (firebaseToken != null) {
                    val firstCategory = updatedBrand.brandCategories?.firstOrNull()
                    brandViewModel.updateBrandProfile(
                        token = firebaseToken,
                        name = updatedBrand.name,
                        brandCategory = firstCategory?.category ?: "",
                        subCategory = firstCategory?.subCategories?.firstOrNull() ?: "",
                        about = updatedBrand.about ?: "",
                        preferredPlatforms = updatedBrand.preferredPlatforms?.map { it.platform } ?: emptyList(),
                        ageMin = updatedBrand.targetAudience?.ageMin,
                        ageMax = updatedBrand.targetAudience?.ageMax,
                        gender = updatedBrand.targetAudience?.gender ?: "Any",
                        profileUrl = updatedBrand.profileUrl,
                        logoUrl = updatedBrand.logoUrl ?: ""
                    ) { success ->
                        if (success) {
                            brandViewModel.fetchBrandDetails(firebaseToken)
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
                isBrand = true
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrandProfileContent(
    modifier: Modifier = Modifier,
    brandProfile: Brand?,
    isLoading: Boolean,
    onSignOut: () -> Unit,
    onNavigateToCreateCampaign: () -> Unit,
    onUpdateProfile: (Brand) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var isEditMode by remember { mutableStateOf(false) }

    // State for ALL editable fields
    var name by remember(brandProfile) { mutableStateOf(brandProfile?.name ?: "") }
    var email by remember(brandProfile) { mutableStateOf(brandProfile?.email ?: "") }
    var role by remember(brandProfile) { mutableStateOf(brandProfile?.role ?: "") }
    var about by remember(brandProfile) { mutableStateOf(brandProfile?.about ?: "") }
    var category by remember(brandProfile) { mutableStateOf(brandProfile?.brandCategories?.firstOrNull()?.category ?: "") }
    var subCategory by remember(brandProfile) { mutableStateOf(brandProfile?.brandCategories?.firstOrNull()?.subCategories?.firstOrNull() ?: "") }
    var profileUrl by remember(brandProfile) { mutableStateOf(brandProfile?.profileUrl ?: "") }
    var logoUrl by remember(brandProfile) { mutableStateOf(brandProfile?.logoUrl ?: "") }
    var ageMin by remember(brandProfile) { mutableStateOf(brandProfile?.targetAudience?.ageMin?.toString() ?: "") }
    var ageMax by remember(brandProfile) { mutableStateOf(brandProfile?.targetAudience?.ageMax?.toString() ?: "") }
    var gender by remember(brandProfile) { mutableStateOf(brandProfile?.targetAudience?.gender ?: "Any") }


    val platformOptions = listOf("Instagram", "YouTube", "Facebook")
    val selectedPlatforms = remember(brandProfile) {
        mutableStateListOf<String>().apply {
            brandProfile?.preferredPlatforms?.forEach { pref ->
                val normalizedName = platformOptions.find { it.equals(pref.platform, ignoreCase = true) } ?: pref.platform
                if (!contains(normalizedName)) {
                    add(normalizedName)
                }
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
                // Reduced and more elegant header
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
                            if (logoUrl.isNotEmpty()) {
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
                                        Icons.Default.Business,
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
                                label = { Text("Brand Name") },
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            )
                        } else {
                            Text(
                                text = name.ifEmpty { "Brand Name" },
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
                                text = role.ifEmpty { "BRAND" },
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
                    
                    DetailInfoSection(icon = Icons.Default.Email, title = "Email Address") {
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

                    DetailInfoSection(icon = Icons.Default.Info, title = "About Brand") {
                        if (isEditMode) {
                            OutlinedTextField(
                                value = about,
                                onValueChange = { about = it },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                                shape = RoundedCornerShape(12.dp),
                                label = { Text("Brand Description") }
                            )
                        } else {
                            Text(
                                text = about.ifEmpty { "No information available." },
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    DetailInfoSection(icon = Icons.Default.Category, title = "Category") {
                        if (isEditMode) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { category = it },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text("Category") }
                                )
                                OutlinedTextField(
                                    value = subCategory,
                                    onValueChange = { subCategory = it },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text("Sub-Category") }
                                )
                            }
                        } else {
                            Text(text = "${category.ifEmpty { "N/A" }} - ${subCategory.ifEmpty { "N/A" }}", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    DetailInfoSection(icon = Icons.Default.Groups, title = "Target Audience") {
                        if (isEditMode) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = ageMin,
                                        onValueChange = { ageMin = it },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        label = { Text("Min Age") }
                                    )
                                    OutlinedTextField(
                                        value = ageMax,
                                        onValueChange = { ageMax = it },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        label = { Text("Max Age") }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = gender,
                                    onValueChange = { gender = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text("Target Gender") }
                                )
                            }
                        } else {
                            Text(text = "Age: ${ageMin.ifEmpty { "N/A" }} - ${ageMax.ifEmpty { "N/A" }} | Gender: ${gender.ifEmpty { "N/A" }}", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    DetailInfoSection(icon = Icons.Default.Public, title = "Preferred Platforms") {
                        if (isEditMode) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                platformOptions.forEach { platform ->
                                    val isSelected = selectedPlatforms.contains(platform)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) selectedPlatforms.remove(platform)
                                            else selectedPlatforms.add(platform)
                                        },
                                        label = { Text(platform) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColor.copy(alpha = 0.1f),
                                            selectedLabelColor = themeColor,
                                            selectedLeadingIconColor = themeColor
                                        )
                                    )
                                }
                            }
                        } else {
                            if (selectedPlatforms.isEmpty()) {
                                Text(text = "No platforms selected.", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    selectedPlatforms.forEach { platform ->
                                        Surface(
                                            color = themeColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = platform,
                                                color = themeColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Redesigned Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isEditMode) {
                                    val updatedBrand = brandProfile?.copy(
                                        name = name,
                                        email = email,
                                        role = role,
                                        about = about,
                                        brandCategories = listOf(BrandCategory(category, listOf(subCategory))),
                                        profileUrl = profileUrl,
                                        logoUrl = logoUrl,
                                        targetAudience = TargetAudience(
                                            ageMin = ageMin.toIntOrNull(),
                                            ageMax = ageMax.toIntOrNull(),
                                            gender = gender,
                                            locations = brandProfile.targetAudience?.locations
                                        ),
                                        preferredPlatforms = selectedPlatforms.map { platformName ->
                                            PreferredPlatform(
                                                platform = platformName,
                                                profileUrl = null,
                                                followers = null,
                                                avgViews = null,
                                                engagement = null,
                                                formats = null,
                                                connected = null,
                                                minFollowers = null,
                                                minEngagement = null
                                            )
                                        }
                                    )
                                    if (updatedBrand != null) {
                                        onUpdateProfile(updatedBrand)
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
fun DetailInfoSection(
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
