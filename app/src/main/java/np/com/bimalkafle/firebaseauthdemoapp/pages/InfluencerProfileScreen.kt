package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.model.InfluencerProfile
import np.com.bimalkafle.firebaseauthdemoapp.model.Category
import np.com.bimalkafle.firebaseauthdemoapp.model.Platform
import np.com.bimalkafle.firebaseauthdemoapp.model.PricingInfo
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.components.CmnBottomNavigationBar
import np.com.bimalkafle.firebaseauthdemoapp.components.ProfileSectionTitle
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun InfluencerProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    influencerViewModel: InfluencerViewModel
) {
    val influencerProfile by influencerViewModel.influencerProfile.observeAsState()
    val isLoading by influencerViewModel.loading.observeAsState(initial = false)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val firebaseToken = result.token
            if (firebaseToken != null) {
                influencerViewModel.fetchInfluencerDetails(firebaseToken)
            }
        }
    }

    InfluencerProfileContent(
        modifier = modifier,
        influencerProfile = influencerProfile,
        isLoading = isLoading,
        onSignOut = {
            authViewModel.signout()
            navController.navigate("login") {
                popUpTo(0)
            }
        },
        onUpdateProfile = { updatedProfile ->
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                val token = result.token
                if (token != null) {
                    val input = JSONObject().apply {
                        put("name", updatedProfile.name)
                        put("bio", updatedProfile.bio ?: "")
                        put("location", updatedProfile.location ?: "")
                        put("availability", updatedProfile.availability ?: true)
                        put("logoUrl", updatedProfile.logoUrl ?: "")

                        val categoriesJson = JSONArray()
                        updatedProfile.categories?.forEach { cat ->
                            categoriesJson.put(JSONObject().apply {
                                put("category", cat.category)
                                put("subCategory", cat.subCategory)
                            })
                        }
                        put("categories", categoriesJson)

                        val platformsJson = JSONArray()
                        updatedProfile.platforms?.forEach { plat ->
                            platformsJson.put(JSONObject().apply {
                                put("platform", plat.platform)
                                put("profileUrl", plat.profileUrl)
                                put("followers", plat.followers ?: 0)
                                put("avgViews", plat.avgViews ?: 0)
                                put("engagement", (plat.engagement ?: 0f).toDouble())
                                val formatsArr = JSONArray()
                                plat.formats?.forEach { formatsArr.put(it) }
                                put("formats", formatsArr)
                            })
                        }
                        put("platforms", platformsJson)

                        val pricingJson = JSONArray()
                        updatedProfile.pricing?.forEach { pr ->
                            pricingJson.put(JSONObject().apply {
                                put("platform", pr.platform)
                                put("deliverable", pr.deliverable)
                                put("price", pr.price)
                                put("currency", pr.currency)
                            })
                        }
                        put("pricing", pricingJson)

                        val strengthsJson = JSONArray()
                        updatedProfile.strengths?.forEach { strengthsJson.put(it) }
                        put("strengths", strengthsJson)
                    }

                    influencerViewModel.updateInfluencerProfile(token, input) { success ->
                        if (success) {
                            Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
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
                isBrand = false
            )
        }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InfluencerProfileContent(
    modifier: Modifier = Modifier,
    influencerProfile: InfluencerProfile?,
    isLoading: Boolean,
    onSignOut: () -> Unit,
    onUpdateProfile: (InfluencerProfile) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    var isEditMode by remember { mutableStateOf(false) }

    // State for editable fields
    var name by remember(influencerProfile) { mutableStateOf(influencerProfile?.name ?: "") }
    var email by remember(influencerProfile) { mutableStateOf(influencerProfile?.email ?: "") }
    var role by remember(influencerProfile) { mutableStateOf(influencerProfile?.role ?: "") }
    var bio by remember(influencerProfile) { mutableStateOf(influencerProfile?.bio ?: "") }
    var location by remember(influencerProfile) { mutableStateOf(influencerProfile?.location ?: "") }
    var logoUrl by remember(influencerProfile) { mutableStateOf(influencerProfile?.logoUrl ?: "") }
    var availability by remember(influencerProfile) { mutableStateOf(influencerProfile?.availability ?: true) }
    
    var categoriesStr by remember(influencerProfile) { 
        mutableStateOf(influencerProfile?.categories?.joinToString(", ") { "${it.category}:${it.subCategory}" } ?: "") 
    }
    var strengthsStr by remember(influencerProfile) {
        mutableStateOf(influencerProfile?.strengths?.joinToString(", ") ?: "")
    }
    var pricingList by remember(influencerProfile) {
        mutableStateOf(influencerProfile?.pricing ?: emptyList())
    }

    Scaffold(
        bottomBar = bottomBar
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
                            if (!logoUrl.isNullOrEmpty()) {
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
                                    Icons.Default.Person,
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
                                label = { Text(stringResource(id = R.string.label_influencer_name)) },
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = role.ifEmpty { stringResource(id = R.string.default_role_influencer) },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = name.ifEmpty { stringResource(id = R.string.label_influencer_name) },
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = role.ifEmpty { stringResource(id = R.string.default_role_influencer) },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Profile Details
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

                    ProfileSectionTitle(stringResource(id = R.string.section_bio))
                    if (isEditMode) {
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(stringResource(id = R.string.label_bio)) }
                        )
                    } else {
                        Text(
                            text = bio.ifEmpty { stringResource(id = R.string.msg_no_bio_available) },
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_location))
                    if (isEditMode) {
                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(stringResource(id = R.string.label_location)) }
                        )
                    } else {
                        Text(text = location.ifEmpty { stringResource(id = R.string.not_available) }, color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_categories))
                    if (isEditMode) {
                        OutlinedTextField(
                            value = categoriesStr,
                            onValueChange = { categoriesStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(stringResource(id = R.string.section_categories)) }
                        )
                    } else {
                        Text(text = categoriesStr.ifEmpty { stringResource(id = R.string.not_available) }, color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_strengths))
                    if (isEditMode) {
                        OutlinedTextField(
                            value = strengthsStr,
                            onValueChange = { strengthsStr = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            label = { Text(stringResource(id = R.string.label_strengths)) }
                        )
                    } else {
                        Text(text = strengthsStr.ifEmpty { stringResource(id = R.string.not_available) }, color = Color.DarkGray, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle(stringResource(id = R.string.section_availability))
                    if (isEditMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = availability,
                                onCheckedChange = { availability = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF8383))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (availability) stringResource(id = R.string.label_available) else stringResource(id = R.string.label_not_available))
                        }
                    } else {
                        Text(
                            text = if (availability) stringResource(id = R.string.label_available) else stringResource(id = R.string.label_not_available),
                            color = Color.DarkGray,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Platforms & Pricing
                    ProfileSectionTitle(stringResource(id = R.string.section_platforms_pricing))
                    influencerProfile?.platforms?.forEach { platform ->
                        InfluencerPlatformCard(platform)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Pricing Section
                    if (isEditMode) {
                        pricingList.forEachIndexed { index, pricing ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "${pricing.platform} - ${pricing.deliverable}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = pricing.price.toString(),
                                        onValueChange = { newValue ->
                                            val newPrice = newValue.toIntOrNull() ?: 0
                                            pricingList = pricingList.toMutableList().apply {
                                                this[index] = this[index].copy(price = newPrice)
                                            }
                                        },
                                        label = { Text("Price (${pricing.currency})") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        pricingList.forEach { pricing ->
                            InfluencerPricingCard(pricing)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Edit/Save Button
                    Button(
                        onClick = {
                            if (isEditMode) {
                                val parsedCategories = categoriesStr.split(",").mapNotNull {
                                    val parts = it.trim().split(":")
                                    if (parts.size == 2) Category(parts[0].trim(), parts[1].trim()) else null
                                }
                                val parsedStrengths = strengthsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                                val updatedProfile = influencerProfile?.copy(
                                    name = name,
                                    email = email,
                                    bio = bio,
                                    location = location,
                                    logoUrl = logoUrl,
                                    availability = availability,
                                    categories = parsedCategories,
                                    strengths = parsedStrengths,
                                    pricing = pricingList
                                )
                                if (updatedProfile != null) {
                                    onUpdateProfile(updatedProfile)
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

@Composable
fun InfluencerPlatformCard(platform: Platform) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = platform.platform, fontWeight = FontWeight.Bold, color = Color(0xFFFF8383))
            Text(text = "${stringResource(id = R.string.label_followers)}: ${platform.followers ?: 0}", fontSize = 12.sp)
            Text(text = "${stringResource(id = R.string.label_engagement)}: ${platform.engagement ?: 0}%", fontSize = 12.sp)
            Text(text = "${stringResource(id = R.string.label_avg_views)}: ${platform.avgViews ?: 0}", fontSize = 12.sp)
        }
    }
}

@Composable
fun InfluencerPricingCard(pricing: PricingInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = pricing.platform, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = pricing.deliverable, fontSize = 12.sp, color = Color.Gray)
            }
            Text(text = "${pricing.currency} ${pricing.price}", fontWeight = FontWeight.Bold, color = Color(0xFFFF8383))
        }
    }
}


@Preview(showBackground = true)
@Composable
fun InfluencerProfilePreview() {
    FirebaseAuthDemoAppTheme {
        InfluencerProfileContent(
            influencerProfile = null,
            isLoading = false,
            onSignOut = {},
            onUpdateProfile = {}
        )
    }
}
