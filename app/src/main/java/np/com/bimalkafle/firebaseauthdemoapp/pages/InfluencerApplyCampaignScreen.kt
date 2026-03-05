package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.widget.Toast
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.components.UnifiedDeliverableItem
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfluencerApplyCampaignScreen(
    campaignId: String,
    onBack: () -> Unit,
    onApplySuccess: () -> Unit,
    campaignViewModel: CampaignViewModel,
    influencerViewModel: InfluencerViewModel
) {
    val campaignDetail by campaignViewModel.campaign.observeAsState()
    val isLoading by influencerViewModel.loading.observeAsState(false)
    val error by influencerViewModel.error.observeAsState()
    val context = LocalContext.current

    var message by remember { mutableStateOf("") }
    
    // Structure: mapOf(platform to mapOf(deliverable to quantity))
    var platformDeliverableQuantities by remember { mutableStateOf(emptyMap<String, Map<String, String>>()) }
    var pricingMap by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }

    LaunchedEffect(campaignId) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            if (token != null) {
                campaignViewModel.fetchCampaignById(campaignId, token)
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    // Responsive header height calculation
    val headerHeight = if (screenHeight < 600.dp) 220.dp else screenHeight * 0.32f
    val overlapHeight = 40.dp
    val formPaddingTop = headerHeight - overlapHeight

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
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
                onClick = onBack, 
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            // Brand info container - explicitly positioned to be visible
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight - overlapHeight) // Only use visible area
                    .statusBarsPadding()
                    .padding(top = 48.dp), // Below back button
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(if (screenHeight < 600.dp) 60.dp else 80.dp)
                        .shadow(8.dp, CircleShape)
                ) {
                    AsyncImage(
                        model = campaignDetail?.brand?.logoUrl,
                        contentDescription = "Brand Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = campaignDetail?.brand?.name ?: "Brand",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (screenHeight < 600.dp) 18.sp else 22.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        // Form
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                "APPLY FOR CAMPAIGN",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            // Campaign Name (Read Only)
            Text("Campaign", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = campaignDetail?.title ?: "Loading...",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF8383),
                    unfocusedBorderColor = Color(0xFFEEEEEE),
                    focusedContainerColor = Color(0xFFFAFAFA),
                    unfocusedContainerColor = Color(0xFFFAFAFA)
                )
            )

            // Message
            Text("Message", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                placeholder = { Text("Describe why you are a good fit...") },
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF8383),
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )

            // Unified Deliverables & Pricing section
            if (campaignDetail != null && !campaignDetail?.platforms.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Deliverables & Pricing", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                
                campaignDetail?.platforms?.forEach { campaignPlatform ->
                    val platformName = campaignPlatform.platform
                    val formats = campaignPlatform.formats ?: emptyList()
                    
                    if (formats.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val icon = when (platformName.lowercase()) {
                                    "instagram" -> painterResource(id = R.drawable.ic_instagram)
                                    "facebook" -> painterResource(id = R.drawable.ic_facebook)
                                    "youtube" -> painterResource(id = R.drawable.ic_youtube)
                                    else -> null
                                }
                                if (icon != null) {
                                    Image(painter = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    platformName.uppercase(),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when(platformName.lowercase()) {
                                        "youtube" -> Color(0xFFCC0000)
                                        "instagram" -> Color(0xFFE4405F)
                                        else -> Color(0xFFFF8383)
                                    },
                                    fontSize = 14.sp
                                )
                            }
                            
                            Surface(
                                color = Color(0xFFF0F0F5),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${formats.size} items",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        formats.forEach { deliverable ->
                            val currentCount = platformDeliverableQuantities[platformName]?.get(deliverable) ?: ""
                            val currentPrice = pricingMap[platformName]?.get(deliverable) ?: ""
                            
                            UnifiedDeliverableItem(
                                deliverable = deliverable,
                                count = currentCount,
                                onCountChange = { newCount ->
                                    val newQuantities = platformDeliverableQuantities.toMutableMap()
                                    val platformMap = newQuantities[platformName]?.toMutableMap() ?: mutableMapOf()
                                    platformMap[deliverable] = newCount
                                    newQuantities[platformName] = platformMap
                                    platformDeliverableQuantities = newQuantities
                                },
                                price = currentPrice,
                                onPriceChange = { newPrice ->
                                    val newPricing = pricingMap.toMutableMap()
                                    val platformPricing = newPricing[platformName]?.toMutableMap() ?: mutableMapOf()
                                    platformPricing[deliverable] = newPrice
                                    newPricing[platformName] = platformPricing
                                    pricingMap = newPricing
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalPricing = mutableListOf<Map<String, Any>>()
                    pricingMap.forEach { (platform, deliverables) ->
                        deliverables.forEach { (deliverable, price) ->
                            val quantityStr = platformDeliverableQuantities[platform]?.get(deliverable) ?: "0"
                            val quantity = quantityStr.toIntOrNull() ?: 0
                            if (price.isNotEmpty() && quantity > 0) {
                                finalPricing.add(
                                    mapOf(
                                        "platform" to platform.uppercase(),
                                        "deliverable" to deliverable,
                                        "price" to (price.toIntOrNull() ?: 0),
                                        "count" to quantity,
                                        "currency" to "INR"
                                    )
                                )
                            }
                        }
                    }

                    if (message.isEmpty()) {
                        Toast.makeText(context, "Please write a message", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (finalPricing.isEmpty()) {
                        Toast.makeText(context, "Please enter pricing and quantity for at least one deliverable", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                        val token = result.token
                        if (token != null) {
                            influencerViewModel.applyToCampaign(
                                token = token,
                                campaignId = campaignId,
                                message = message,
                                pricing = finalPricing,
                                onComplete = { success ->
                                    if (success) {
                                        Toast.makeText(context, "Application submitted successfully!", Toast.LENGTH_LONG).show()
                                        onApplySuccess()
                                    }
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("SUBMIT PROPOSAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            if (error != null) {
                Text(
                    text = error ?: "Unknown error",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
