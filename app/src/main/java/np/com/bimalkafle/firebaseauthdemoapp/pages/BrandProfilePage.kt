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
import androidx.compose.ui.res.stringResource
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
import np.com.bimalkafle.firebaseauthdemoapp.model.Brand
import np.com.bimalkafle.firebaseauthdemoapp.model.BrandCategory
import np.com.bimalkafle.firebaseauthdemoapp.model.PreferredPlatform
import np.com.bimalkafle.firebaseauthdemoapp.model.TargetAudience
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.ProfileSectionTitle

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
        onNavigateToCreateCampaign = { navController.navigate(navController.context.getString(R.string.create_campaign_route)) },
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
                selectedItem = stringResource(id = R.string.nav_profile),
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

    val platformOptions = listOf(
        stringResource(id = R.string.platform_instagram),
        stringResource(id = R.string.platform_youtube),
        stringResource(id = R.string.platform_twitter),
        stringResource(id = R.string.platform_facebook)
    )

    Scaffold(
        bottomBar = bottomBar,
        floatingActionButton = {
            if (!isEditMode) {
                FloatingActionButton(
                    onClick = onNavigateToCreateCampaign,
                    containerColor = Color(0xFFFF8383),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.cd_create_campaign), tint = Color.White)
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
                    .padding(padding)
            ) {
                // Profile Header
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
                                label = { Text(stringResource(id = R.string.label_brand_name)) },
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Role is no longer editable as requested
                            Text(
                                text = role.ifEmpty { stringResource(id = R.string.default_role) },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = name.ifEmpty { stringResource(id = R.string.label_brand_name) },
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = role.ifEmpty { stringResource(id = R.string.default_role) },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Profile Details (All Editable)
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileSectionTitle(stringResource(id = R.string.section_email_address))
                    if (isEditMode) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(stringResource(id = R.string.label_email)) }
                        )
                    } else {
                        Text(text = email.ifEmpty { stringResource(id = R.string.not_available) }, color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_about_brand))
                    if (isEditMode) {
                        OutlinedTextField(
                            value = about,
                            onValueChange = { about = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(stringResource(id = R.string.label_brand_description)) }
                        )
                    } else {
                        Text(
                            text = about.ifEmpty { stringResource(id = R.string.msg_no_info_available) },
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_primary_objective))
                    if (isEditMode) {
                        OutlinedTextField(
                            value = primaryObjective,
                            onValueChange = { primaryObjective = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(stringResource(id = R.string.label_objective)) }
                        )
                    } else {
                        Text(text = primaryObjective.ifEmpty { stringResource(id = R.string.not_available) }, color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_category))
                    if (isEditMode) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                label = { Text(stringResource(id = R.string.label_category)) }
                            )
                            OutlinedTextField(
                                value = subCategory,
                                onValueChange = { subCategory = it },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                label = { Text(stringResource(id = R.string.label_sub_category)) }
                            )
                        }
                    } else {
                        Text(text = "${category.ifEmpty { stringResource(id = R.string.not_available) }} - ${subCategory.ifEmpty { stringResource(id = R.string.not_available) }}", color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_target_audience))
                    if (isEditMode) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ageMin,
                                    onValueChange = { ageMin = it },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text(stringResource(id = R.string.label_min_age)) }
                                )
                                OutlinedTextField(
                                    value = ageMax,
                                    onValueChange = { ageMax = it },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    label = { Text(stringResource(id = R.string.label_max_age)) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = gender,
                                onValueChange = { gender = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                label = { Text(stringResource(id = R.string.label_target_gender)) }
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(
                                id = R.string.text_age_gender,
                                ageMin.ifEmpty { stringResource(id = R.string.not_available) },
                                ageMax.ifEmpty { stringResource(id = R.string.not_available) },
                                gender.ifEmpty { stringResource(id = R.string.not_available) }
                            ),
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_preferred_platforms))
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
                                        selectedContainerColor = Color(0xFFFF8383).copy(alpha = 0.1f),
                                        selectedLabelColor = Color(0xFFFF8383),
                                        selectedLeadingIconColor = Color(0xFFFF8383)
                                    )
                                )
                            }
                        }
                    } else {
                        if (selectedPlatforms.isEmpty()) {
                            Text(text = stringResource(id = R.string.msg_no_platforms_selected), color = Color.DarkGray, fontSize = 14.sp)
                        } else {
                            Text(text = selectedPlatforms.joinToString(", "), color = Color.DarkGray, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Edit/Save Button (Moved to bottom)
                    Button(
                        onClick = {
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
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isEditMode) Color(0xFF4CAF50) else Color(0xFFFF8383))
                    ) {
                        Icon(
                            if (isEditMode) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditMode) stringResource(id = R.string.btn_save) else stringResource(id = R.string.btn_edit),
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
                        Text(stringResource(id = R.string.btn_log_out), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun BrandProfilePreview() {
    FirebaseAuthDemoAppTheme {
        BrandProfileContent(
            brandProfile = null,
            isLoading = false,
            onSignOut = {},
            onNavigateToCreateCampaign = {},
            onUpdateProfile = {}
        )
    }
}
