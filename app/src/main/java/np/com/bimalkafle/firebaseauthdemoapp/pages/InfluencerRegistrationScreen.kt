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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfluencerRegistrationScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }

    val deliverables = listOf("Reels/Shorts", "Story", "Post", "Video")
    val selectedDeliverables = remember { mutableStateListOf<String>() }

    val categoryOptions = listOf("Fashion", "Tech", "Lifestyle", "Gaming", "Food", "Beauty", "Travel")
    val selectedCategories = remember { mutableStateListOf<String>() }

    val platforms = listOf("Instagram", "Facebook", "Twitter", "YouTube")
    val selectedPlatforms = remember { mutableStateListOf<String>() }

    var profileUrls by remember { mutableStateOf(mapOf<String, String>()) }
    var pricing by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }


    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.4f
    val formPaddingTop = headerHeight - 80.dp

    Box(modifier = Modifier.fillMaxSize()) {
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
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)) {
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
                        "Twitter" -> painterResource(id = R.drawable.ic_twitter)
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
                                "Twitter" -> painterResource(id = R.drawable.ic_twitter)
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

            Button(
                onClick = { navController.navigate("influencer_detail") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFF8383)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("NEXT", color = Color.White, fontWeight = FontWeight.Bold)
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
