package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.R
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import org.json.JSONArray
import org.json.JSONObject
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfluencerRegistrationScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val prefsManager = remember { PrefsManager(context) }

    val categoriesMap = mapOf(
        "Fashion" to listOf("Clothing", "Footwear", "Accessories"),
        "Tech" to listOf("Gadgets", "Software", "Hardware"),
        "Food" to listOf("Organic", "Fast Food", "Dining"),
        "Beauty" to listOf("Skincare", "Makeup", "Haircare"),
        "Health" to listOf("Fitness", "Supplements", "Wellness")
    )

    val platformsList = listOf("YouTube", "Instagram", "Facebook")
    val platformFormatsMap = mapOf(
        "Facebook" to listOf("reels/shorts", "post", "video", "story"),
        "Instagram" to listOf("reels/shorts", "post", "story"),
        "YouTube" to listOf("reels/shorts", "post", "video")
    )

    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var selectedSubCategories by remember { mutableStateOf(mapOf<String, Set<String>>()) }
    var selectedPlatforms by remember { mutableStateOf(setOf<String>()) }
    var platformDeliverables by remember { mutableStateOf(mapOf<String, Set<String>>()) }
    var deliverablePricing by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }

    // --- YouTube Connection State ---
    var isYouTubeConnecting by remember { mutableStateOf(false) }
    var isYouTubeConnected by remember { mutableStateOf(false) }
    var youtubeAuthCode by remember { mutableStateOf<String?>(null) }
    
    // --- Instagram Connection State ---
    var instagramUrl by remember { mutableStateOf("") }
    var isInstagramConnecting by remember { mutableStateOf(false) }
    var isInstagramConnected by remember { mutableStateOf(false) }

    // --- Facebook Connection State (Dummy) ---
    var isFacebookConnecting by remember { mutableStateOf(false) }
    var isFacebookConnected by remember { mutableStateOf(false) }
    var facebookUrl by remember { mutableStateOf("") }

    // --- Google Sign-In Launcher for Activity Result ---
    val youtubeAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account?.serverAuthCode
                if (authCode != null) {
                    youtubeAuthCode = authCode
                    coroutineScope.launch {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        firebaseUser?.getIdToken(true)?.addOnSuccessListener { res ->
                            val idToken = res.token
                            if (idToken != null) {
                                coroutineScope.launch {
                                    val response = BackendRepository.connectYouTube(authCode, idToken)
                                    response.onSuccess {
                                        isYouTubeConnected = true
                                        Toast.makeText(context, "YouTube Connected & Saved", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "YouTube Connect failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        isYouTubeConnecting = false
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.35f
    val formPaddingTop = headerHeight - 40.dp

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
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
                    navController.popBackStack()
                },
                modifier = Modifier.statusBarsPadding().padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_profile),
                    contentDescription = "Influencer Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(70.dp).clip(CircleShape).background(Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Creator Profile Setup", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(text = "Let's get started by filling out your details", color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
            }
        }

        // Form
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                .padding(horizontal = 24.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text("General Information", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Creator Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                trailingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short Bio / About") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
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

            Spacer(modifier = Modifier.height(24.dp))
            
            // Multiselect Category Chips
            Text("Select Categories *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                Text("Select Sub-Categories *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
            Text("Platforms & Deliverables *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            
            platformsList.forEach { platform ->
                val isPlatformSelected = selectedPlatforms.contains(platform)
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPlatformSelected) Color(0xFFFF8383).copy(alpha = 0.1f) else Color(0xFFF5F5F5))
                            .clickable {
                                selectedPlatforms = if (isPlatformSelected) selectedPlatforms - platform else selectedPlatforms + platform
                                if (isPlatformSelected) {
                                    platformDeliverables = platformDeliverables - platform
                                    deliverablePricing = deliverablePricing - platform
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
                        Text("Select Deliverables for $platform", fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 8.dp))
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
                                        if (isFormatSelected) {
                                            val newPricing = deliverablePricing.toMutableMap()
                                            val platPricing = newPricing[platform]?.toMutableMap() ?: mutableMapOf()
                                            platPricing.remove(format)
                                            newPricing[platform] = platPricing
                                            deliverablePricing = newPricing
                                        }
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
            Text("Pricing", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            if (selectedPlatforms.any { platformDeliverables[it]?.isNotEmpty() == true }) {
                selectedPlatforms.forEach { platform ->
                    val selectedFormats = platformDeliverables[platform] ?: emptySet()
                    if (selectedFormats.isNotEmpty()) {
                        Text(platform, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp), color = Color(0xFFFF8383))
                        selectedFormats.forEach { deliverable ->
                            OutlinedTextField(
                                value = deliverablePricing[platform]?.get(deliverable) ?: "",
                                onValueChange = { price ->
                                    val newPricing = deliverablePricing.toMutableMap()
                                    val platformPricing = newPricing[platform]?.toMutableMap() ?: mutableMapOf()
                                    platformPricing[deliverable] = price
                                    newPricing[platform] = platformPricing
                                    deliverablePricing = newPricing
                                },
                                label = { Text("$deliverable Price (INR)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                            )
                        }
                    }
                }
            } else {
                Text(text = "Please select platforms and deliverables to set pricing", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Connect Platforms", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            if (selectedPlatforms.contains("YouTube")) {
                Button(
                    onClick = {
                        isYouTubeConnecting = true
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestScopes(Scope("https://www.googleapis.com/auth/yt-analytics.readonly"), Scope("https://www.googleapis.com/auth/youtube.readonly"))
                            .requestServerAuthCode("60831940637-pgkdgu5qe3htquot95fddf50rljm6er0.apps.googleusercontent.com", true)
                            .build()
                        youtubeAuthLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isYouTubeConnected && !isYouTubeConnecting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isYouTubeConnected) Color(0xFF4CAF50) else Color(0xFFFF0000))
                ) {
                    if (isYouTubeConnecting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text(if (isYouTubeConnected) "YouTube Connected ✓" else "Connect YouTube", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedPlatforms.contains("Instagram")) {
                OutlinedTextField(
                    value = instagramUrl,
                    onValueChange = { instagramUrl = it },
                    label = { Text("Instagram Profile URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383)),
                    enabled = !isInstagramConnected && !isInstagramConnecting
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (instagramUrl.isEmpty()) return@Button
                        isInstagramConnecting = true
                        auth.currentUser?.getIdToken(false)?.addOnSuccessListener { res ->
                            res.token?.let { token ->
                                coroutineScope.launch {
                                    BackendRepository.scrapeInstagramProfile(instagramUrl, auth.currentUser!!.uid, token)
                                        .onSuccess { isInstagramConnected = it }
                                    isInstagramConnecting = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isInstagramConnected && !isInstagramConnecting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isInstagramConnected) Color(0xFF4CAF50) else Color(0xFFE1306C))
                ) {
                    if (isInstagramConnecting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text(if (isInstagramConnected) "Instagram Connected ✓" else "Connect Instagram", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (selectedPlatforms.contains("Facebook")) {
                OutlinedTextField(
                    value = facebookUrl,
                    onValueChange = { facebookUrl = it },
                    label = { Text("Facebook Profile URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383)),
                    enabled = !isFacebookConnected && !isFacebookConnecting
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (facebookUrl.isEmpty()) return@Button
                        isFacebookConnecting = true
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1000)
                            isFacebookConnected = true
                            isFacebookConnecting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isFacebookConnected && !isFacebookConnecting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isFacebookConnected) Color(0xFF4CAF50) else Color(0xFF1877F2))
                ) {
                    if (isFacebookConnecting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text(if (isFacebookConnected) "Facebook Connected ✓" else "Connect Facebook", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isEmpty() || location.isEmpty() || bio.isEmpty() || logoUrl.isEmpty() || selectedCategories.isEmpty() || selectedPlatforms.isEmpty()) {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val user = auth.currentUser ?: return@launch
                            user.getIdToken(true).addOnSuccessListener { res ->
                                val token = res.token ?: return@addOnSuccessListener
                                coroutineScope.launch {
                                    val input = JSONObject().apply {
                                        put("name", name)
                                        put("bio", bio)
                                        put("location", location)
                                        put("availability", true)
                                        put("logoUrl", logoUrl)

                                        val categoriesJson = JSONArray()
                                        selectedCategories.forEach { cat ->
                                            val subCats = selectedSubCategories[cat] ?: emptySet()
                                            categoriesJson.put(JSONObject().apply {
                                                put("category", cat)
                                                put("subCategories", JSONArray(if (subCats.isEmpty()) listOf("General") else subCats.toList()))
                                            })
                                        }
                                        put("categories", categoriesJson)

                                        val platformsJson = JSONArray()
                                        selectedPlatforms.forEach { plat ->
                                            platformsJson.put(JSONObject().apply {
                                                put("platform", plat)
                                                put("profileUrl", if (plat == "Instagram") instagramUrl else if (plat == "Facebook") facebookUrl else "")
                                                put("followers", 0)
                                                put("avgViews", 0)
                                                put("engagement", 0.0)
                                                put("formats", JSONArray((platformDeliverables[plat] ?: emptySet()).toList()))
                                            })
                                        }
                                        put("platforms", platformsJson)

                                        val pricingJson = JSONArray()
                                        deliverablePricing.forEach { (plat, deliverableMap) ->
                                            deliverableMap.forEach { (deliverable, price) ->
                                                if (price.isNotBlank()) {
                                                    pricingJson.put(JSONObject().apply {
                                                        put("platform", plat)
                                                        put("deliverable", deliverable)
                                                        put("price", price.toIntOrNull() ?: 0)
                                                        put("currency", "INR")
                                                    })
                                                }
                                            }
                                        }
                                        put("pricing", pricingJson)
                                        put("strengths", JSONArray())
                                        put("audienceInsights", JSONObject.NULL)
                                    }

                                    BackendRepository.setupInfluencerProfile(input, token).onSuccess {
                                        prefsManager.saveProfileCompleted(user.uid, true)
                                        Toast.makeText(context, "Profile setup successful!", Toast.LENGTH_SHORT).show()
                                        navController.navigate("influencer_detail")
                                    }.onFailure {
                                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                                    isLoading = false
                                }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(if (isLoading) Color.Gray else Color(0xFFFF8383)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("NEXT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InfluencerRegistrationScreenPreview() {
    InfluencerRegistrationScreen(NavController(androidx.compose.ui.platform.LocalContext.current))
}
