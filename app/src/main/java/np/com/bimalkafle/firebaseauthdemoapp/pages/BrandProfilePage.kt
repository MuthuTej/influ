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
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.components.BrandBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.model.Brand
import np.com.bimalkafle.firebaseauthdemoapp.model.BrandCategory
import np.com.bimalkafle.firebaseauthdemoapp.model.PreferredPlatform
import np.com.bimalkafle.firebaseauthdemoapp.model.TargetAudience
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
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
        onSignOut = { authViewModel.signout() },
        onNavigateToCreateCampaign = { navController.navigate("create_campaign") },
        onUpdateProfile = { updatedBrand ->
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                val firebaseToken = result.token
                if (firebaseToken != null) {
                    brandViewModel.updateBrandProfile(
                        token = firebaseToken,
                        name = updatedBrand.name,
                        brandCategory = updatedBrand.brandCategory?.category ?: "",
                        subCategory = updatedBrand.brandCategory?.subCategory ?: "",
                        about = updatedBrand.about ?: "",
                        primaryObjective = updatedBrand.primaryObjective ?: "",
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
    var primaryObjective by remember(brandProfile) { mutableStateOf(brandProfile?.primaryObjective ?: "") }
    var category by remember(brandProfile) { mutableStateOf(brandProfile?.brandCategory?.category ?: "") }
    var subCategory by remember(brandProfile) { mutableStateOf(brandProfile?.brandCategory?.subCategory ?: "") }
    var profileUrl by remember(brandProfile) { mutableStateOf(brandProfile?.profileUrl ?: "") }
    var logoUrl by remember(brandProfile) { mutableStateOf(brandProfile?.logoUrl ?: "") }
    var ageMin by remember(brandProfile) { mutableStateOf(brandProfile?.targetAudience?.ageMin?.toString() ?: "") }
    var ageMax by remember(brandProfile) { mutableStateOf(brandProfile?.targetAudience?.ageMax?.toString() ?: "") }
    var gender by remember(brandProfile) { mutableStateOf(brandProfile?.targetAudience?.gender ?: "Any") }
    
    val selectedPlatforms = remember(brandProfile) {
        mutableStateListOf<String>().apply {
            brandProfile?.preferredPlatforms?.forEach { add(it.platform) }
        }
    }

    val platformOptions = listOf("Instagram", "YouTube", "Twitter", "Facebook")

    Scaffold(
        bottomBar = bottomBar,
        floatingActionButton = {
            if (!isEditMode) {
                FloatingActionButton(
                    onClick = onNavigateToCreateCampaign,
                    containerColor = Color(0xFFFF8383),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Campaign", tint = Color.White)
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF8383))
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Header (Editable)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .background(Color(0xFFFF8383)),
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

                    // Edit/Save Toggle with Text and Icon
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clickable {
                                if (isEditMode) {
                                    val updatedBrand = brandProfile?.copy(
                                        name = name,
                                        email = email,
                                        role = role,
                                        about = about,
                                        primaryObjective = primaryObjective,
                                        brandCategory = BrandCategory(category, subCategory),
                                        profileUrl = profileUrl,
                                        logoUrl = logoUrl,
                                        targetAudience = TargetAudience(ageMin.toIntOrNull(), ageMax.toIntOrNull(), gender),
                                        preferredPlatforms = selectedPlatforms.map { PreferredPlatform(it, null, null, null) }
                                    )
                                    if (updatedBrand != null) {
                                        onUpdateProfile(updatedBrand)
                                    }
                                }
                                isEditMode = !isEditMode
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditMode) "Save" else "Edit",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            if (isEditMode) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color.White
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
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    modifier = Modifier.padding(24.dp),
                                    tint = Color(0xFFFF8383)
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
                                label = { Text("Brand Name") },
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = role,
                                onValueChange = { role = it },
                                modifier = Modifier.padding(horizontal = 64.dp).height(50.dp).fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedLabelColor = Color.White,
                                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                                ),
                                label = { Text("Role") },
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp)
                            )
                        } else {
                            Text(
                                text = name.ifEmpty { "Brand Name" },
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = role.ifEmpty { "BRAND" },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Profile Details (All Editable)
                Column(modifier = Modifier.padding(16.dp)) {
                    // Note: profileCompleted and updatedAt fields removed as requested.

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

                    ProfileSectionTitle("About Brand")
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
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ProfileSectionTitle("Primary Campaign Objective")
                    if (isEditMode) {
                        OutlinedTextField(
                            value = primaryObjective,
                            onValueChange = { primaryObjective = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text("Objective") }
                        )
                    } else {
                        Text(
                            text = primaryObjective.ifEmpty { "No objective set." },
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Brand Category & Sub-Category")
                    if (isEditMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Category") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = subCategory,
                                onValueChange = { subCategory = it },
                                label = { Text("Sub-category") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    } else {
                        if (category.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF8383).copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "$category • $subCategory",
                                    color = Color(0xFFFF8383),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text("Not specified", color = Color.Gray, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Website URL")
                    if (isEditMode) {
                        OutlinedTextField(
                            value = profileUrl,
                            onValueChange = { profileUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text("Website") }
                        )
                    } else {
                        Text(
                            text = profileUrl.ifEmpty { "N/A" },
                            color = if (profileUrl.isNotEmpty()) Color(0xFF2196F3) else Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { /* browser logic */ }
                        )
                    }

                    if (isEditMode) {
                        Spacer(modifier = Modifier.height(24.dp))
                        ProfileSectionTitle("Logo Image URL")
                        OutlinedTextField(
                            value = logoUrl,
                            onValueChange = { logoUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text("Logo URL") }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Preferred Platforms")
                    if (isEditMode) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            platformOptions.forEach { platform ->
                                val isSelected = selectedPlatforms.contains(platform)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedPlatforms.remove(platform) else selectedPlatforms.add(platform)
                                    },
                                    label = { Text(platform) }
                                )
                            }
                        }
                    } else {
                        if (selectedPlatforms.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedPlatforms.forEach { platform ->
                                    AssistChip(onClick = {}, label = { Text(platform) })
                                }
                            }
                        } else {
                            Text("None specified", color = Color.Gray, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Target Audience Focus")
                    if (isEditMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = ageMin,
                                onValueChange = { ageMin = it },
                                label = { Text("Min Age") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Text("to", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = ageMax,
                                onValueChange = { ageMax = it },
                                label = { Text("Max Age") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        var genderExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = genderExpanded,
                            onExpandedChange = { genderExpanded = !genderExpanded }
                        ) {
                            OutlinedTextField(
                                value = gender,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Gender Focus") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = genderExpanded,
                                onDismissRequest = { genderExpanded = false }
                            ) {
                                listOf("Any", "Male", "Female").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = { gender = option; genderExpanded = false }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Ages: ${ageMin.ifEmpty { "?" }} - ${ageMax.ifEmpty { "?" }} • Gender Focus: $gender",
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun BrandProfilePagePreview() {
    val sampleBrand = Brand(
        id = "1",
        email = "contact@nike.com",
        name = "Nike",
        role = "BRAND",
        profileCompleted = true,
        updatedAt = "2023-10-27",
        brandCategory = BrandCategory("Sports", "Footwear"),
        about = "Nike, Inc. is an American multinational corporation that is engaged in the design, development, manufacturing, and worldwide marketing and sales of footwear, apparel, equipment, accessories, and services.",
        primaryObjective = "Increase brand awareness and sales of new running shoes.",
        profileUrl = "https://www.nike.com",
        logoUrl = "https://upload.wikimedia.org/wikipedia/commons/a/a6/Logo_NIKE.svg",
        preferredPlatforms = listOf(
            PreferredPlatform("Instagram", null, null, null),
            PreferredPlatform("YouTube", null, null, null)
        ),
        targetAudience = TargetAudience(18, 35, "Any")
    )

    FirebaseAuthDemoAppTheme {
        BrandProfileContent(
            brandProfile = sampleBrand,
            isLoading = false,
            onSignOut = {},
            onNavigateToCreateCampaign = {},
            onUpdateProfile = {},
            bottomBar = {
                BrandBottomNavigationBar(
                    selectedItem = "Profile",
                    onItemSelected = {},
                    onCreateCampaign = {},
                    navController = rememberNavController()
                )
            }
        )
    }
}
