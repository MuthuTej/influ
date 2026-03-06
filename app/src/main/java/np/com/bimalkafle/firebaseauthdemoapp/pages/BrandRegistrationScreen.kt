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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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

    var ageMin by remember { mutableStateOf("18") }
    var ageMax by remember { mutableStateOf("25") }
    var gender by remember { mutableStateOf("Any") }
    var profileUrl by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }

    // --- Multi-select Categories Logic ---
    val categoriesMap = mapOf(
        "Fashion" to listOf("Clothing", "Footwear", "Accessories"),
        "Tech" to listOf("Gadgets", "Software", "Hardware"),
        "Food" to listOf("Organic", "Fast Food", "Dining"),
        "Beauty" to listOf("Skincare", "Makeup", "Haircare"),
        "Health" to listOf("Fitness", "Supplements", "Wellness"),
        "E-commerce" to listOf("Marketplace", "Logistics", "Customer Service")
    )
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var selectedSubCategories by remember { mutableStateOf(mapOf<String, Set<String>>()) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.35f
    val formPaddingTop = headerHeight - 40.dp
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefsManager = PrefsManager(context)
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid by remember {
        derivedStateOf {
            brandName.isNotBlank() &&
                    description.isNotBlank() &&
                    selectedCategories.isNotEmpty() &&
                    selectedPlatforms.isNotEmpty() &&
                    selectedPlatforms.all { platformDeliverables[it]?.isNotEmpty() == true } &&
                    ageMin.toIntOrNull() != null &&
                    ageMax.toIntOrNull() != null
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
                .background(Color(0xFFFF8383))
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
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Multiselect Category Chips
            Text("Industry Categories *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoriesMap.keys.forEach { category ->
                    val isSelected = selectedCategories.contains(category)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCategories = if (isSelected) selectedCategories - category else selectedCategories + category
                            if (isSelected) selectedSubCategories = selectedSubCategories - category
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF8383),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (selectedCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Focus Areas *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                selectedCategories.forEach { category ->
                    Text(category, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesMap[category]?.forEach { subCat ->
                            val currentSubCats = selectedSubCategories[category] ?: emptySet()
                            val isSubSelected = currentSubCats.contains(subCat)
                            FilterChip(
                                selected = isSubSelected,
                                onClick = {
                                    val newSubCats = if (isSubSelected) currentSubCats - subCat else currentSubCats + subCat
                                    selectedSubCategories = selectedSubCategories + (category to newSubCats)
                                },
                                label = { Text(subCat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF8383).copy(alpha = 0.7f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
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
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
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
                            .background(if (isPlatformSelected) Color(0xFFFF8383).copy(alpha = 0.1f) else Color(0xFFF5F5F5))
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
                            "YouTube" -> R.drawable.ic_youtube
                            "Instagram" -> R.drawable.ic_instagram
                            "Facebook" -> R.drawable.ic_facebook
                            else -> R.drawable.ic_youtube
                        }
                        Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(platform, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = isPlatformSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF8383))
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
                                        selectedContainerColor = Color(0xFFFF8383).copy(alpha = 0.7f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Target Audience", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AgeInput(label = "Min Age", value = ageMin, onValueChange = { ageMin = it }, modifier = Modifier.weight(1f))
                AgeInput(label = "Max Age", value = ageMax, onValueChange = { ageMax = it }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            
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
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
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
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = logoUrl,
                onValueChange = { logoUrl = it },
                label = { Text("Logo URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
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
                                val success = BrandRepository.setupBrandProfile(
                                    token = firebaseToken,
                                    name = brandName,
                                    categories = selectedCategories.map { cat ->
                                        mapOf(
                                            "category" to cat,
                                            "subCategories" to (selectedSubCategories[cat]?.toList() ?: listOf("General"))
                                        )
                                    },
                                    about = description,
                                    preferredPlatforms = selectedPlatforms.map { plat ->
                                        mapOf(
                                            "platform" to plat,
                                            "formats" to (platformDeliverables[plat]?.toList() ?: emptyList<String>()),
                                            "minFollowers" to 1000,
                                            "minEngagement" to 2.5
                                        )
                                    },
                                    ageMin = ageMin.toIntOrNull(),
                                    ageMax = ageMax.toIntOrNull(),
                                    gender = gender,
                                    profileUrl = profileUrl,
                                    logoUrl = logoUrl
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
                            if (isFormValid && !isLoading) Color(0xFFFF8383) else Color.Gray
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

@Composable
fun AgeInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val currentValue = value.toIntOrNull() ?: 0
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383)),
        trailingIcon = {
            Column {
                Icon(
                    imageVector = Icons.Default.ArrowDropUp,
                    contentDescription = "Increment",
                    modifier = Modifier.clickable { onValueChange((currentValue + 1).toString()) }
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Decrement",
                    modifier = Modifier.clickable { onValueChange((currentValue - 1).toString()) }
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun BrandRegistrationScreenPreview() {
    BrandRegistrationScreen(onBack = {}, onNext = {})
}
