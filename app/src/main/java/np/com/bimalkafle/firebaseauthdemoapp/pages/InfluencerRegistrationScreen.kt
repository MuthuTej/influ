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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import np.com.bimalkafle.firebaseauthdemoapp.utils.IndianLocations
import np.com.bimalkafle.firebaseauthdemoapp.utils.IndianLanguages

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfluencerRegistrationScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var motherTongue by remember { mutableStateOf("") }
    var selectedLanguages by remember { mutableStateOf(setOf<String>()) }
    var logoUrl by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val prefsManager = remember { PrefsManager(context) }

    var availableCategories by remember { mutableStateOf(listOf<String>()) }

    val platformsList = listOf("YouTube", "Instagram", "Facebook")
    val platformFormatsMap = mapOf(
        "Facebook" to listOf("reels/shorts", "post", "video", "story"),
        "Instagram" to listOf("reels/shorts", "post", "story"),
        "YouTube" to listOf("reels/shorts", "post", "video")
    )

    var selectedCategories by remember { mutableStateOf(setOf<String>()) }
    var selectedPlatforms by remember { mutableStateOf(setOf<String>()) }
    var platformDeliverables by remember { mutableStateOf(mapOf<String, Set<String>>()) }
    var deliverablePricing by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }

    // --- YouTube Connection State ---
    var isYouTubeConnecting by remember { mutableStateOf(false) }
    var isYouTubeConnected by remember { mutableStateOf(false) }
    var youtubeAuthCode by remember { mutableStateOf<String?>(null) }
    
    // --- Instagram Connection State ---
    val instagramEntries = remember { mutableStateListOf(Pair("", false)) }
    var connectingIndex by remember { mutableStateOf<Int?>(null) }

    // --- Facebook Connection State ---
    var isFacebookConnecting by remember { mutableStateOf(false) }
    var isFacebookConnected by remember { mutableStateOf(false) }
    var facebookUrl by remember { mutableStateOf("") }

    // --- Google Sign-In Launcher ---
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

    LaunchedEffect(Unit) {
        auth.currentUser?.getIdToken(false)?.addOnSuccessListener { res ->
            res.token?.let { token ->
                coroutineScope.launch {
                    BackendRepository.getDistinctInfluencerCategories(token).onSuccess {
                        availableCategories = it
                    }
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.35f
    val formPaddingTop = headerHeight - 40.dp

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
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
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            var stateExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = stateExpanded, onExpandedChange = { stateExpanded = !stateExpanded }) {
                OutlinedTextField(
                    value = selectedState,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("State") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
                    IndianLocations.states.forEach { state ->
                        DropdownMenuItem(text = { Text(state) }, onClick = {
                            selectedState = state
                            selectedCity = ""
                            stateExpanded = false
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            var cityExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { if (selectedState.isNotEmpty()) cityExpanded = !cityExpanded }) {
                OutlinedTextField(
                    value = selectedCity,
                    onValueChange = { },
                    readOnly = true,
                    enabled = selectedState.isNotEmpty(),
                    label = { Text("City") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                    IndianLocations.citiesFor(selectedState).forEach { city ->
                        DropdownMenuItem(text = { Text(city) }, onClick = {
                            selectedCity = city
                            cityExpanded = false
                        })
                    }
                }
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
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                ExposedDropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false }) {
                    IndianLanguages.genders.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            gender = option
                            genderExpanded = false
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            var motherTongueExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = motherTongueExpanded, onExpandedChange = { motherTongueExpanded = !motherTongueExpanded }) {
                OutlinedTextField(
                    value = motherTongue,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Mother Tongue") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = motherTongueExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                ExposedDropdownMenu(expanded = motherTongueExpanded, onDismissRequest = { motherTongueExpanded = false }) {
                    IndianLanguages.languages.forEach { language ->
                        DropdownMenuItem(text = { Text(language) }, onClick = {
                            motherTongue = language
                            motherTongueExpanded = false
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            var languagesExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = languagesExpanded, onExpandedChange = { languagesExpanded = !languagesExpanded }) {
                OutlinedTextField(
                    value = if (selectedLanguages.isEmpty()) "" else "${selectedLanguages.size} selected",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Languages Known") },
                    placeholder = { Text("Choose languages") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languagesExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )
                ExposedDropdownMenu(expanded = languagesExpanded, onDismissRequest = { languagesExpanded = false }) {
                    IndianLanguages.languages.forEach { language ->
                        val isSelected = selectedLanguages.contains(language)
                        DropdownMenuItem(
                            text = { Text(language) },
                            leadingIcon = { Checkbox(checked = isSelected, onCheckedChange = null) },
                            onClick = {
                                selectedLanguages = if (isSelected) selectedLanguages - language else selectedLanguages + language
                            }
                        )
                    }
                }
            }
            if (selectedLanguages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedLanguages.forEach { language ->
                        FilterChip(
                            selected = true,
                            onClick = { selectedLanguages = selectedLanguages - language },
                            label = { Text(language) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short Bio / About") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = logoUrl,
                onValueChange = { logoUrl = it },
                label = { Text("Logo URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Select Categories *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                    shape = RoundedCornerShape(12.dp)
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
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) }
                        )
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
                            .background(if (isPlatformSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
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
                            "YouTube" -> R.drawable.youtube_logo
                            "Instagram" -> R.drawable.instagram_logo
                            "Facebook" -> R.drawable.ic_facebook
                            else -> R.drawable.youtube_logo
                        }
                        Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(platform, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Checkbox(checked = isPlatformSelected, onCheckedChange = null)
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
                                    label = { Text(format, fontSize = 12.sp) }
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
                        Text(platform, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.primary)
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
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
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
                Text("Instagram Profiles", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFFE1306C))
                Spacer(modifier = Modifier.height(8.dp))
                instagramEntries.forEachIndexed { index, (url, connected) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = { instagramEntries[index] = Pair(it, false) },
                            label = { Text("Profile URL ${index + 1}") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !connected && connectingIndex != index,
                            singleLine = true,
                            trailingIcon = {
                                if (connected) Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                        )
                        if (!connected) {
                            Button(
                                onClick = {
                                    if (url.isBlank()) return@Button
                                    connectingIndex = index
                                    auth.currentUser?.getIdToken(false)?.addOnSuccessListener { res ->
                                        res.token?.let { token ->
                                            coroutineScope.launch {
                                                BackendRepository.scrapeInstagramProfile(url.trim(), auth.currentUser!!.uid, token)
                                                    .onSuccess { ok -> if (ok) instagramEntries[index] = Pair(url, true) }
                                                connectingIndex = null
                                            }
                                        }
                                    }
                                },
                                enabled = connectingIndex == null,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C))
                            ) {
                                if (connectingIndex == index) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Text("Link", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (instagramEntries.size < 5) {
                    TextButton(onClick = { instagramEntries.add(Pair("", false)) }, enabled = connectingIndex == null) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFE1306C))
                        Spacer(Modifier.width(4.dp))
                        Text("Add another Instagram profile", color = Color(0xFFE1306C), fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isEmpty() || selectedState.isEmpty() || selectedCity.isEmpty() || bio.isEmpty() || logoUrl.isEmpty() || selectedCategories.isEmpty() || selectedPlatforms.isEmpty()) {
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
                                        put("location", "$selectedCity, $selectedState")
                                        put("gender", gender)
                                        put("motherTongue", motherTongue)
                                        put("languagesKnown", JSONArray(selectedLanguages.toList()))
                                        put("availability", true)
                                        put("logoUrl", logoUrl)

                                        val categoriesJson = JSONArray()
                                        selectedCategories.forEach { cat ->
                                            categoriesJson.put(JSONObject().apply {
                                                put("category", cat)
                                            })
                                        }
                                        put("categories", categoriesJson)

                                        val platformsJson = JSONArray()
                                        selectedPlatforms.forEach { plat ->
                                            platformsJson.put(JSONObject().apply {
                                                put("platform", plat)
                                                put("profileUrl", if (plat == "Instagram") instagramEntries.firstOrNull()?.first ?: "" else "")
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
                                    }

                                    BackendRepository.setupInfluencerProfile(input, token).onSuccess {
                                        prefsManager.saveProfileCompleted(user.uid, true)
                                        Toast.makeText(context, "Profile setup successful!", Toast.LENGTH_SHORT).show()
                                        // Navigate directly to home and clear stack
                                        navController.navigate("influencer_home") {
                                            popUpTo(0) { inclusive = true }
                                        }
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("NEXT", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InfluencerRegistrationScreenPreview() {
    InfluencerRegistrationScreen(NavController(androidx.compose.ui.platform.LocalContext.current))
}
