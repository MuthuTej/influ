package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
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
import np.com.bimalkafle.firebaseauthdemoapp.components.AiChatFab
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.LoadingState
import np.com.bimalkafle.firebaseauthdemoapp.components.StarRatingDisplay
import java.io.File
import java.io.FileOutputStream

@Composable
fun BrandProfilePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel
) {
    val brandProfile by brandViewModel.brandProfile.observeAsState(initial = null)
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
        onSubmitVerification = { gstNumber, method, file, onResult ->
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                ?.addOnSuccessListener { result ->
                    val firebaseToken = result.token
                    if (firebaseToken == null) {
                        onResult("Not authenticated. Please sign in again.")
                        return@addOnSuccessListener
                    }
                    brandViewModel.uploadVerificationDocument(firebaseToken, file) { url, uploadError ->
                        if (url != null) {
                            brandViewModel.submitBrandVerification(firebaseToken, gstNumber, method, url) { _, submitError ->
                                onResult(submitError)
                            }
                        } else {
                            onResult(uploadError ?: "Failed to upload document. Please try again.")
                        }
                    }
                }
                ?.addOnFailureListener {
                    onResult("Not authenticated. Please sign in again.")
                }
        },
        onUpdateProfile = { updatedBrand: Brand ->
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
                        // The edit screen already validates both fields parse before building updatedBrand;
                        // the fallback values only guard against that invariant ever changing.
                        ageMin = updatedBrand.targetAudience?.ageMin ?: 18,
                        ageMax = updatedBrand.targetAudience?.ageMax ?: 25,
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
        },
        floatingActionButton = { AiChatFab(navController) }
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
    // (gstNumber, method, document file, onResult: error message or null on success)
    onSubmitVerification: (String?, String, File, (String?) -> Unit) -> Unit = { _, _, _, onResult -> onResult("Not available.") },
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {}
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
    var saveError by remember { mutableStateOf<String?>(null) }


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

    val themeColor = MaterialTheme.colorScheme.primary
    Scaffold(
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingState(message = "Loading your profile…")
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .imePadding()
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = name.ifEmpty { "Brand Name" },
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (brandProfile?.isVerified == true) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified brand",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
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

                    if (!isEditMode) {
                        RatingsSection(brandProfile = brandProfile)
                        BusinessVerificationSection(
                            brandProfile = brandProfile,
                            onSubmitVerification = onSubmitVerification
                        )
                    }

                    if (saveError != null) {
                        Text(
                            text = saveError ?: "",
                            color = Color.Red,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (saveError != null) 0.dp else 32.dp))

                    // Redesigned Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isEditMode) {
                                    val parsedAgeMin = ageMin.toIntOrNull()
                                    val parsedAgeMax = ageMax.toIntOrNull()
                                    if (parsedAgeMin == null || parsedAgeMax == null) {
                                        saveError = "Enter a valid Min Age and Max Age (numbers only)."
                                        return@Button
                                    }
                                    saveError = null
                                    val updatedBrand = brandProfile?.copy(
                                        name = name,
                                        email = email,
                                        role = role,
                                        about = about,
                                        brandCategories = listOf(BrandCategory(category, listOf(subCategory))),
                                        profileUrl = profileUrl,
                                        logoUrl = logoUrl,
                                        targetAudience = TargetAudience(
                                            ageMin = parsedAgeMin,
                                            ageMax = parsedAgeMax,
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
fun RatingsSection(brandProfile: Brand?) {
    val reviewCount = brandProfile?.reviews?.size ?: 0
    DetailInfoSection(icon = Icons.Default.Star, title = "Overall Rating") {
        if (reviewCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%.1f".format(brandProfile?.averageRating ?: 0.0),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                StarRatingDisplay(rating = brandProfile?.averageRating ?: 0.0, starSize = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(from $reviewCount review${if (reviewCount == 1) "" else "s"})",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        } else {
            Text(
                text = "No reviews yet — your rating appears here once influencers review completed collaborations.",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

/**
 * GST/business-document verification section (submitBrandVerification mutation,
 * src/graphql/modules/brand/index.js). Deliberately offers only GST_DOCUMENT and
 * BUSINESS_REGISTRATION_DOCUMENT — AADHAAR_EKYC exists as a backend enum value
 * but is rejected server-side (only UIDAI-licensed AUA/KUA entities may collect
 * Aadhaar data), so it's never surfaced here.
 */
@Composable
fun BusinessVerificationSection(
    brandProfile: Brand?,
    onSubmitVerification: (String?, String, File, (String?) -> Unit) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    val verificationRequest = brandProfile?.verificationRequest

    DetailInfoSection(icon = Icons.Default.VerifiedUser, title = "Business Verification") {
        when {
            brandProfile?.isVerified == true -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Verified — influencers can see your verified badge.",
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            verificationRequest?.status == "PENDING" -> {
                Text(
                    text = "Submitted — pending admin review. This usually takes a couple of days.",
                    color = Color(0xFF8A6D00),
                    fontSize = 14.sp
                )
            }
            verificationRequest?.status == "REJECTED" -> {
                Column {
                    val note = verificationRequest.adminNote?.takeIf { it.isNotBlank() }
                    Text(
                        text = note ?: "Your last submission was rejected. Please review and resubmit.",
                        color = Color(0xFFC62828),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(onClick = { showDialog = true }) {
                        Text("Resubmit")
                    }
                }
            }
            else -> {
                Column {
                    Text(
                        text = "Get a verified badge so influencers know your brand is legitimate.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Verified", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDialog) {
        VerificationSubmitDialog(
            onDismiss = { showDialog = false },
            onSubmit = { gstNumber, method, file, onResult ->
                onSubmitVerification(gstNumber, method, file, onResult)
            }
        )
    }
}

private val VERIFICATION_METHOD_OPTIONS = listOf(
    "GST_DOCUMENT" to "GST Certificate",
    "BUSINESS_REGISTRATION_DOCUMENT" to "Business Registration Document"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerificationSubmitDialog(
    onDismiss: () -> Unit,
    onSubmit: (gstNumber: String?, method: String, file: File, onResult: (String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var method by remember { mutableStateOf(VERIFICATION_METHOD_OPTIONS.first().first) }
    var methodMenuExpanded by remember { mutableStateOf(false) }
    var gstNumber by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "Selected document"
        }
    }

    val methodLabel = VERIFICATION_METHOD_OPTIONS.first { it.first == method }.second
    val canSubmit = selectedUri != null && (method != "GST_DOCUMENT" || gstNumber.isNotBlank()) && !isSubmitting

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Get Verified") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = methodMenuExpanded,
                    onExpandedChange = { if (!isSubmitting) methodMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = methodLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = methodMenuExpanded,
                        onDismissRequest = { methodMenuExpanded = false }
                    ) {
                        VERIFICATION_METHOD_OPTIONS.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    method = value
                                    methodMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                if (method == "GST_DOCUMENT") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = gstNumber,
                        onValueChange = { gstNumber = it.uppercase() },
                        label = { Text("GST Number") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        documentPickerLauncher.launch(
                            arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")
                        )
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedFileName ?: "Attach Document",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = it, color = Color(0xFFC62828), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canSubmit,
                onClick = {
                    val uri = selectedUri ?: return@Button
                    isSubmitting = true
                    errorMessage = null
                    val file = copyUriToCacheFile(context, uri)
                    if (file == null) {
                        isSubmitting = false
                        errorMessage = "Couldn't read the selected file. Please try again."
                        return@Button
                    }
                    val gstNumberArg = gstNumber.takeIf { method == "GST_DOCUMENT" && it.isNotBlank() }
                    onSubmit(gstNumberArg, method, file) { error ->
                        isSubmitting = false
                        if (error != null) {
                            errorMessage = error
                        } else {
                            onDismiss()
                        }
                    }
                }
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * SAF document picks hand back a content:// Uri, not a filesystem path — copy
 * it into the app's cache dir so uploadVerificationDocument has a real
 * java.io.File to attach as multipart form data.
 */
private fun copyUriToCacheFile(context: Context, uri: Uri): File? {
    return try {
        val mimeType = context.contentResolver.getType(uri)
        val extension = when (mimeType) {
            "application/pdf" -> "pdf"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val outputFile = File(context.cacheDir, "verification_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outputFile).use { output -> input.copyTo(output) }
        } ?: return null
        outputFile
    } catch (e: Exception) {
        null
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
                    tint = MaterialTheme.colorScheme.primary,
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
