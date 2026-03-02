package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.imePadding
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

    val deliverables = listOf("Reels/Shorts", "Story", "Post", "Video")
    val selectedDeliverables = remember { mutableStateListOf<String>() }

    val categoryOptions = listOf("Fashion", "Tech", "Lifestyle", "Gaming", "Food", "Beauty", "Travel")
    val selectedCategories = remember { mutableStateListOf<String>() }

    val platforms = listOf("Instagram", "Facebook", "YouTube")
    val selectedPlatforms = remember { mutableStateListOf<String>() }

    var profileUrls by remember { mutableStateOf(mapOf<String, String>()) }
    var pricing by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }

    // --- YouTube Connection State ---
    var isYouTubeConnecting by remember { mutableStateOf(false) }
    var isYouTubeConnected by remember { mutableStateOf(false) }
    var youtubeAuthCode by remember { mutableStateOf<String?>(null) }

    // --- Google Sign-In Launcher for Activity Result ---
    val youtubeAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("InfluencerRegistration", "YouTube Connect launcher result code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Get the signed-in account details
                val account = task.getResult(ApiException::class.java)
                val authCode = account?.serverAuthCode
                Log.d("InfluencerRegistration", "Account email: ${account?.email}")
                Log.d("InfluencerRegistration", "Server Auth Code: $authCode")

                if (authCode != null) {
                    youtubeAuthCode = authCode

                    coroutineScope.launch {
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        Log.d("InfluencerRegistration", "Firebase User UID: ${firebaseUser?.uid}")
                        firebaseUser?.getIdToken(true)?.addOnSuccessListener { result ->
                            val idToken = result.token
                            Log.d("InfluencerRegistration", "ID Token retrieved: ${if (idToken != null) "Success" else "Null"}")

                            if (idToken != null) {
                                coroutineScope.launch {
                                    Log.d("InfluencerRegistration", "Calling BackendRepository.connectYouTube...")
                                    val response = BackendRepository.connectYouTube(authCode, idToken)

                                    response.onSuccess {
                                        Log.d("InfluencerRegistration", "connectYouTube SUCCESS")
                                        isYouTubeConnected = true
                                        Toast.makeText(
                                            context,
                                            "YouTube Connected & Saved",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }.onFailure {
                                        Log.e("InfluencerRegistration", "connectYouTube FAILURE: ${it.message}", it)
                                        Toast.makeText(
                                            context,
                                            "Failed: ${it.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }?.addOnFailureListener {
                            Log.e("InfluencerRegistration", "Failed to get Firebase ID Token", it)
                        }
                    }
                } else {
                    Log.w("InfluencerRegistration", "Auth Code is NULL")
                    Toast.makeText(context, "Failed to get server auth code", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                // Handling API exception during Google Sign In
                Log.e("InfluencerRegistration", "Google Sign-In ApiException: status code ${e.statusCode}, message: ${e.message}", e)
                Toast.makeText(context, "YouTube Connect failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
             Log.d("InfluencerRegistration", "YouTube Connect cancelled or failed by user")
            Toast.makeText(context, "YouTube Connect cancelled", Toast.LENGTH_SHORT).show()
        }
        isYouTubeConnecting = false
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.4f
    val formPaddingTop = headerHeight - 80.dp

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
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.2f),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.popBackStack()
                },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_profile), // Replace with actual influencer image
                    contentDescription = "Influencer Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Creator Profile Setup",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Let's get started by filling out your details",
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
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
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
            Spacer(modifier = Modifier.height(16.dp))
            Text("Category", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryOptions.forEach { category ->
                    FilterChip(
                        selected = selectedCategories.contains(category),
                        onClick = { if (selectedCategories.contains(category)) selectedCategories.remove(category) else selectedCategories.add(category) },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Deliverables", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deliverables.forEach { deliverable ->
                    FilterChip(
                        selected = selectedDeliverables.contains(deliverable),
                        onClick = { if (selectedDeliverables.contains(deliverable)) selectedDeliverables.remove(deliverable) else selectedDeliverables.add(deliverable) },
                        label = { Text(deliverable) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Platform", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                platforms.forEach { platform ->
                    val isSelected = selectedPlatforms.contains(platform)
                    val icon = when (platform) {
                        "Instagram" -> painterResource(id = R.drawable.ic_instagram)
                        "Facebook" -> painterResource(id = R.drawable.ic_facebook)
                        "YouTube" -> painterResource(id = R.drawable.ic_youtube)
                        else -> null
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (isSelected) selectedPlatforms.remove(platform) else selectedPlatforms.add(platform) },
                        label = { Text(platform) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "$platform selected",
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (icon != null) {
                                Image(
                                    painter = icon,
                                    contentDescription = "$platform logo",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                }
            }

            if (selectedPlatforms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Profile URLs", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                selectedPlatforms.forEach { platform ->
                    OutlinedTextField(
                        value = profileUrls[platform] ?: "",
                        onValueChange = {
                            profileUrls = profileUrls.toMutableMap().apply { this[platform] = it }
                        },
                        label = { Text("$platform Profile URL") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Pricing", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

            if (selectedPlatforms.isNotEmpty() && selectedDeliverables.isNotEmpty()) {
                selectedPlatforms.forEach { platform ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (platform) {
                                "Instagram" -> painterResource(id = R.drawable.ic_instagram)
                                "Facebook" -> painterResource(id = R.drawable.ic_facebook)
                                "YouTube" -> painterResource(id = R.drawable.ic_youtube)
                                else -> null
                            }
                            if (icon != null) {
                                Image(painter = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(platform, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val platformDeliverables = when (platform) {
                            "Instagram" -> selectedDeliverables.filter { it != "Video" }
                            "YouTube" -> selectedDeliverables.filter { it != "Story" && it != "Post" }
                            else -> selectedDeliverables
                        }

                        if (platformDeliverables.isNotEmpty()) {
                            platformDeliverables.forEach { deliverable ->
                                OutlinedTextField(
                                    value = pricing[platform]?.get(deliverable) ?: "",
                                    onValueChange = { price ->
                                        val newPricing = pricing.toMutableMap()
                                        val platformPricing =
                                            newPricing[platform]?.toMutableMap() ?: mutableMapOf()
                                        platformPricing[deliverable] = price
                                        newPricing[platform] = platformPricing
                                        pricing = newPricing
                                    },
                                    label = { Text("$deliverable Price") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                                )
                            }
                        } else {
                            Text(
                                text = "No applicable deliverables selected for this platform.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "no platform or deliverables are selected",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- CONNECT YOUTUBE BUTTON ---
            Button(
                onClick = {
                    isYouTubeConnecting = true
                    // Configure Google Sign-In options with required scopes
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail() // Common requirement
                        .requestScopes(
                            Scope("https://www.googleapis.com/auth/yt-analytics.readonly"),
                            Scope("https://www.googleapis.com/auth/youtube.readonly")
                        )
                        // Request server auth code using Web Client ID
                        // Replace YOUR_WEB_CLIENT_ID with the actual Google Cloud Web Client ID
                        .requestServerAuthCode("60831940637-pgkdgu5qe3htquot95fddf50rljm6er0.apps.googleusercontent.com", true)
                        .build()



                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    // Launch the intent using the modern Activity Result API launcher
                    youtubeAuthLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                // Disable button if it's already connected or currently connecting
                enabled = !isYouTubeConnected && !isYouTubeConnecting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isYouTubeConnected) Color(0xFF4CAF50) else Color(0xFFFF0000), // Green when connected, Red otherwise
                    disabledContainerColor = if (isYouTubeConnected) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color.LightGray
                )
            ) {
                // Display different content based on connection state
                if (isYouTubeConnecting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting...", color = Color.White, fontWeight = FontWeight.Bold)
                } else if (isYouTubeConnected) {
                    Icon(Icons.Default.Check, contentDescription = "Checked", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("YouTube Connected ✓", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text("Connect YouTube", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isEmpty() || location.isEmpty() || bio.isEmpty() || logoUrl.isEmpty() || selectedCategories.isEmpty() || selectedPlatforms.isEmpty() || pricing.isEmpty()) {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val user = auth.currentUser
                            if (user == null) {
                                Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                                isLoading = false
                                return@launch
                            }

                            user.getIdToken(true).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val token = task.result?.token
                                    if (token != null) {
                                        coroutineScope.launch {
                                            val input = JSONObject().apply {
                                                put("name", name)
                                                put("bio", bio)
                                                put("location", location)
                                                put("availability", true) // Default as discussed
                                                put("logoUrl", logoUrl)

                                                val categoriesJson = JSONArray()
                                                selectedCategories.forEach { cat ->
                                                    categoriesJson.put(JSONObject().apply {
                                                        put("category", cat)
                                                        put("subCategory", "General") // Default
                                                    })
                                                }
                                                put("categories", categoriesJson)

                                                val platformsJson = JSONArray()
                                                selectedPlatforms.forEach { plat ->
                                                    platformsJson.put(JSONObject().apply {
                                                        put("platform", plat)
                                                        put("profileUrl", profileUrls[plat] ?: "")
                                                        put("followers", 0) // Default
                                                        put("avgViews", 0) // Default
                                                        put("engagement", 0.0) // Default
                                                        put("formats", JSONArray())
                                                    })
                                                }
                                                put("platforms", platformsJson)

                                                val pricingJson = JSONArray()
                                                pricing.forEach { (plat, deliverableMap) ->
                                                    deliverableMap.forEach { (deliverable, price) ->
                                                        pricingJson.put(JSONObject().apply {
                                                            put("platform", plat)
                                                            put("deliverable", deliverable)
                                                            put("price", price.toIntOrNull() ?: 0)
                                                            put("currency", "INR") // Default
                                                        })
                                                    }
                                                }
                                                put("pricing", pricingJson)

                                                put("strengths", JSONArray())
                                                put("audienceInsights", JSONObject.NULL)
                                            }

                                            val result = BackendRepository.setupInfluencerProfile(input, token)
                                            isLoading = false
                                            result.onSuccess {
                                                user.uid.let { uid ->
                                                    prefsManager.saveProfileCompleted(uid, true)
                                                }
                                                Toast.makeText(context, "Profile setup successful!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("influencer_detail")
                                            }.onFailure {
                                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Failed to get auth token", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Token error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isLoading) Color.Gray else Color(0xFFFF8383)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
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
fun InfluencerRegistrationScreenPreview() {
    InfluencerRegistrationScreen(NavController(androidx.compose.ui.platform.LocalContext.current))
}
