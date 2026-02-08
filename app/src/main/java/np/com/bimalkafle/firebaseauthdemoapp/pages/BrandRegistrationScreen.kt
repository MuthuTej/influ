package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import np.com.bimalkafle.firebaseauthdemoapp.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
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
    var brandCategory by remember { mutableStateOf("E-commerce") }
    var subCategory by remember { mutableStateOf("Fashion") }
    var description by remember { mutableStateOf("") }
    var campaignObjective by remember { mutableStateOf("Brand Awareness") }
    val platformOptions = listOf("Instagram", "YouTube", "TikTok", "Twitter", "Facebook")
    val selectedPlatforms = remember { mutableStateListOf<String>() }
    var ageMin by remember { mutableStateOf("") }
    var ageMax by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Any") }
    var profileUrl by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.4f
    val formPaddingTop = headerHeight - 80.dp
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefsManager = PrefsManager(context)
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid by androidx.compose.runtime.derivedStateOf {
        brandName.isNotBlank() &&
                description.isNotBlank() &&
                selectedPlatforms.isNotEmpty() &&
                ageMin.toIntOrNull() != null &&
                ageMax.toIntOrNull() != null &&
                ageMin.toIntOrNull()!! <= ageMax.toIntOrNull()!!
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
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
                modifier = Modifier.fillMaxSize()
                    .alpha(0.2f),
                contentScale = ContentScale.Crop

            )
            Spacer(modifier = Modifier.height(16.dp))

            IconButton(onClick = {
                FirebaseAuth.getInstance().signOut()
                onBack()
            }, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_profile), // Placeholder
                    contentDescription = "Brand Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Brand Profile Setup",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Let's get started by filling out your brand details",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Form
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                .fillMaxSize()
                .padding(horizontal = 16.dp) // Added horizontal padding
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = brandName,
                onValueChange = { brandName = it },
                label = { Text("Brand Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            Spacer(modifier = Modifier.height(16.dp))

            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                OutlinedTextField(
                    value = brandCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Brand Category / Industry") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                )
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    DropdownMenuItem(text = { Text("E-commerce") }, onClick = { brandCategory = "E-commerce"; categoryExpanded = false })
                    DropdownMenuItem(text = { Text("Fashion") }, onClick = { brandCategory = "Fashion"; categoryExpanded = false })
                    DropdownMenuItem(text = { Text("Technology") }, onClick = { brandCategory = "Technology"; categoryExpanded = false })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var subCategoryExpanded by remember { mutableStateOf(false) }
            val subCategoryOptions = when (brandCategory) {
                "E-commerce" -> listOf("Fashion", "Electronics", "Home Goods")
                "Fashion" -> listOf("Apparel", "Footwear", "Accessories")
                "Technology" -> listOf("SaaS", "Hardware", "Mobile Apps")
                else -> listOf()
            }
            ExposedDropdownMenuBox(expanded = subCategoryExpanded, onExpandedChange = { subCategoryExpanded = !subCategoryExpanded }) {
                OutlinedTextField(
                    value = subCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sub-Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                )
                ExposedDropdownMenu(expanded = subCategoryExpanded, onDismissRequest = { subCategoryExpanded = false }) {
                    subCategoryOptions.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = { subCategory = option; subCategoryExpanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Short Brand Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            Spacer(modifier = Modifier.height(16.dp))

            var objectiveExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = objectiveExpanded, onExpandedChange = { objectiveExpanded = !objectiveExpanded }) {
                OutlinedTextField(
                    value = campaignObjective,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Primary Campaign Objective") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = objectiveExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                )
                ExposedDropdownMenu(expanded = objectiveExpanded, onDismissRequest = { objectiveExpanded = false }) {
                    DropdownMenuItem(text = { Text("Brand Awareness") }, onClick = { campaignObjective = "Brand Awareness"; objectiveExpanded = false })
                    DropdownMenuItem(text = { Text("Lead Generation") }, onClick = { campaignObjective = "Lead Generation"; objectiveExpanded = false })
                    DropdownMenuItem(text = { Text("Sales") }, onClick = { campaignObjective = "Sales"; objectiveExpanded = false })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Preferred Platforms", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            Spacer(modifier = Modifier.height(16.dp))
            Text("Target Audience", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = ageMin,
                    onValueChange = { ageMin = it },
                    label = { Text("Min Age") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                )
                OutlinedTextField(
                    value = ageMax,
                    onValueChange = { ageMax = it },
                    label = { Text("Max Age") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            var genderExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = genderExpanded, onExpandedChange = { genderExpanded = !genderExpanded }) {
                OutlinedTextField(
                    value = gender,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Gender") },
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = profileUrl,
                onValueChange = { profileUrl = it },
                label = { Text("Profile URL (Website)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = logoUrl,
                onValueChange = { logoUrl = it },
                label = { Text("Logo Url") },
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
                            Log.d("BRAND_DEBUG", "Token received: ${firebaseToken.take(20)}...")
                            Log.d("BRAND_DEBUG", "Sending data -> name=$brandName, category=$brandCategory, subCategory=$subCategory")
                            Log.d("UID" , result.toString())

                            coroutineScope.launch {
                                Log.d("BRAND_DEBUG", "Calling setupBrandProfile mutation...")
                                val success = BrandRepository.setupBrandProfile(
                                    token = firebaseToken,
                                    name = brandName,
                                    brandCategory = brandCategory,
                                    subCategory = subCategory,
                                    about = description,
                                    primaryObjective = campaignObjective,
                                    preferredPlatforms = selectedPlatforms,
                                    ageMin = ageMin.toIntOrNull(),
                                    ageMax = ageMax.toIntOrNull(),
                                    gender = gender,
                                    profileUrl = profileUrl,
                                    logoUrl=logoUrl
                                )
                                Log.d("BRAND_DEBUG", "Mutation result: $success")
                                isLoading = false

                                if (success) {
                                    Log.d("BRAND_DEBUG", "Brand profile created successfully")
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                                    if (uid != null) {
                                        prefsManager.saveProfileCompleted(uid, true)
                                    }
                                    onNext()
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
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
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
                            if (isFormValid && !isLoading)
                                Color(0xFFFF8383)
                            else
                                Color.Gray
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text("NEXT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BrandRegistrationScreenPreview() {
    BrandRegistrationScreen(onBack = {}, onNext = {})
}
