package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import np.com.bimalkafle.firebaseauthdemoapp.network.BrandRepository
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrandRegistrationScreen(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var brandName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val platformOptions = listOf("YouTube", "Instagram", "Facebook")
    val selectedPlatforms = remember { mutableStateListOf<String>() }
    var platformDeliverables by remember { mutableStateOf(mapOf<String, Set<String>>()) }
    
    val platformFormatsMap = mapOf(
        "Facebook" to listOf("reels/shorts", "post", "video", "story"),
        "Instagram" to listOf("reels/shorts", "post", "story"),
        "YouTube" to listOf("reels/shorts", "post", "video")
    )

    var hostingSelected by remember { mutableStateOf(false) }

    var gender by remember { mutableStateOf("Any") }
    var profileUrl by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // --- Multi-select Categories Logic ---
    var availableCategories by remember { mutableStateOf(listOf<String>()) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.35f
    val formPaddingTop = headerHeight - 40.dp
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefsManager = PrefsManager(context)
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { res ->
            res.token?.let { token ->
                coroutineScope.launch {
                    BackendRepository.getDistinctInfluencerCategories(token).onSuccess {
                        availableCategories = it
                    }
                }
            }
        }
    }

    val isFormValid by remember {
        derivedStateOf {
            brandName.isNotBlank() &&
                    description.isNotBlank() &&
                    selectedCategories.isNotEmpty() &&
                    (selectedPlatforms.isNotEmpty() || hostingSelected) &&
                    selectedPlatforms.all { platformDeliverables[it]?.isNotEmpty() == true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Image(
                painter = painterResource(id = R.drawable.vector),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.2f),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onBack()
                },
                modifier = Modifier.statusBarsPadding().padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_profile),
                    contentDescription = "Brand Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Brand Profile Setup",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Let's get started by filling out your brand details",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }

        // Form
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text("General Information", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = brandName,
                onValueChange = { brandName = it },
                label = { Text("Brand Name *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Brand Location") },
                placeholder = { Text("City, Country") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Multiselect Category Dropdown
            Text("Industry Categories *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            var categoriesExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = categoriesExpanded, onExpandedChange = { categoriesExpanded = !categoriesExpanded }) {
                OutlinedTextField(
                    value = if (selectedCategories.isEmpty()) "" else "${selectedCategories.size} selected",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Categories") },
                    placeholder = { Text("Choose categories") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                ExposedDropdownMenu(expanded = categoriesExpanded, onDismissRequest = { categoriesExpanded = false }) {
                    availableCategories.forEach { category ->
                        val isSelected = selectedCategories.contains(category)
                        DropdownMenuItem(
                            text = { Text(category) },
                            leadingIcon = {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                            },
                            onClick = {
                                selectedCategories = if (isSelected) selectedCategories - category else selectedCategories + category
                            }
                        )
                    }
                }
            }

            if (selectedCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCategories.forEach { category ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedCategories = selectedCategories - category },
                            label = { Text(category) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Brand Description *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Updated Platforms & Deliverables Section ---
            Text("Preferred Platforms & Formats *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            platformOptions.forEach { platform ->
                val isPlatformSelected = selectedPlatforms.contains(platform)
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPlatformSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
                            .clickable {
                                if (isPlatformSelected) {
                                    selectedPlatforms.remove(platform)
                                    platformDeliverables = platformDeliverables - platform
                                } else {
                                    selectedPlatforms.add(platform)
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconRes = when (platform) {
                            "YouTube" -> R.drawable.youtube_logo
                            "Instagram" -> R.drawable.instagram_logo
                            "Facebook" -> R.drawable.ic_facebook
                            else -> R.drawable.youtube_logo
                        }
                        Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(platform, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = isPlatformSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (isPlatformSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select preferred formats for $platform", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            platformFormatsMap[platform]?.forEach { format ->
                                val currentFormats = platformDeliverables[platform] ?: emptySet()
                                val isFormatSelected = currentFormats.contains(format)
                                FilterChip(
                                    selected = isFormatSelected,
                                    onClick = {
                                        val newFormats = if (isFormatSelected) currentFormats - format else currentFormats + format
                                        platformDeliverables = platformDeliverables + (platform to newFormats)
                                    },
                                    label = { Text(format, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Hosting Section
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hostingSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
                        .clickable {
                            hostingSelected = !hostingSelected
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Hosting",
                        modifier = Modifier.size(24.dp),
                        tint = if (hostingSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Hosting", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = hostingSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Target Audience", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

            var genderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Gender Focus") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                    DropdownMenuItem(text = { Text("Any") }, onClick = { gender = "Any"; genderExpanded = false })
                    DropdownMenuItem(text = { Text("Male") }, onClick = { gender = "Male"; genderExpanded = false })
                    DropdownMenuItem(text = { Text("Female") }, onClick = { gender = "Female"; genderExpanded = false })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = profileUrl,
                onValueChange = { profileUrl = it },
                label = { Text("Website URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = logoUrl,
                onValueChange = { logoUrl = it },
                label = { Text("Logo URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (isLoading) return@Button
                    isLoading = true
                    FirebaseAuth.getInstance().currentUser
                        ?.getIdToken(true)
                        ?.addOnSuccessListener { result ->
                            val firebaseToken = result.token ?: return@addOnSuccessListener
                            
                            coroutineScope.launch {
                                val platformsData = selectedPlatforms.map { plat ->
                                    mapOf(
                                        "platform" to plat,
                                        "formats" to (platformDeliverables[plat]?.toList() ?: emptyList<String>()),
                                        "minFollowers" to 1000,
                                        "minEngagement" to 2.5
                                    )
                                }.toMutableList()

                                if (hostingSelected) {
                                    platformsData.add(mapOf(
                                        "platform" to "Hosting",
                                        "formats" to listOf("Hosting"),
                                        "minFollowers" to 0,
                                        "minEngagement" to 0.0
                                    ))
                                }

                                val success = BrandRepository.setupBrandProfile(
                                    token = firebaseToken,
                                    name = brandName,
                                    categories = selectedCategories.map { cat ->
                                        mapOf("category" to cat)
                                    },
                                    about = description,
                                    preferredPlatforms = platformsData,
                                    // Age targeting is not collected in this form; the server requires
                                    // ageMin/ageMax so a broad default range is sent instead.
                                    ageMin = 13,
                                    ageMax = 99,
                                    gender = gender,
                                    profileUrl = profileUrl,
                                    logoUrl = logoUrl,
                                    location = location
                                )
                                isLoading = false

                                if (success) {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                                    if (uid != null) {
                                        prefsManager.saveProfileCompleted(uid, true)
                                    }
                                    onNext()
                                } else {
                                    Toast.makeText(context, "Failed to save profile", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        ?.addOnFailureListener { e ->
                            isLoading = false
                            Log.e("BRAND_DEBUG", "Failed to get Firebase token", e)
                        }
                },
                enabled = isFormValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isFormValid && !isLoading) MaterialTheme.colorScheme.primary else Color.Gray
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    } else {
                        Text("COMPLETE SETUP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BrandRegistrationScreenPreview() {
    BrandRegistrationScreen(onBack = {}, onNext = {})
}
