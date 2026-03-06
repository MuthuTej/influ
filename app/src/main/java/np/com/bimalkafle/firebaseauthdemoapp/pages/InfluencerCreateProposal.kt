package np.com.bimalkafle.firebaseauthdemoapp.pages

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.components.UnifiedDeliverableItem
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfluencerCreateProposal(
    influencerId: String,
    onBack: () -> Unit,
    onCreateProposal: () -> Unit,
    brandViewModel: BrandViewModel,
    authViewModel: AuthViewModel
) {
    val brandProfile by brandViewModel.brandProfile.observeAsState()
    val myCampaigns by brandViewModel.myCampaigns.observeAsState(emptyList())
    val isLoading by brandViewModel.loading.observeAsState(false)
    val error by brandViewModel.error.observeAsState()
    var selectedCampaignId by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    // Get target platforms from the selected campaign
    val selectedCampaign = remember(selectedCampaignId, myCampaigns) {
        myCampaigns.find { it.id == selectedCampaignId }
    }
    
    // Structure: mapOf(platform to mapOf(deliverable to quantity))
    var platformDeliverableQuantities by remember { mutableStateOf(emptyMap<String, Map<String, String>>()) }
    var pricing by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }

    // Synchronize states when campaign changes
    LaunchedEffect(selectedCampaignId) {
        platformDeliverableQuantities = emptyMap()
        pricing = emptyMap()
    }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            if (token != null) {
                brandViewModel.fetchMyCampaigns(token)
                brandViewModel.fetchBrandDetails(token)
            }
        }
    }

    LaunchedEffect(myCampaigns) {
        if (selectedCampaignId.isEmpty() && myCampaigns.isNotEmpty()) {
            selectedCampaignId = myCampaigns.first().id
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.3f
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
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!brandProfile?.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = brandProfile?.logoUrl,
                        contentDescription = "Brand Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.brand_profile),
                        contentDescription = "Brand Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
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

            Text(
                "CREATE PROPOSAL",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Text("Select Campaign", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            var campaignExpanded by remember { mutableStateOf(false) }
            val selectedCampaignTitle = selectedCampaign?.title ?: "Select Campaign"
            
            ExposedDropdownMenuBox(expanded = campaignExpanded, onExpandedChange = { campaignExpanded = !campaignExpanded }) {
                OutlinedTextField(
                    value = selectedCampaignTitle,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = campaignExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                )
                ExposedDropdownMenu(expanded = campaignExpanded, onDismissRequest = { campaignExpanded = false }) {
                    myCampaigns.forEach { campaign ->
                        DropdownMenuItem(text = { Text(campaign.title) }, onClick = { selectedCampaignId = campaign.id; campaignExpanded = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Message", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Write a message to the influencer...") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            // Unified Deliverables & Pricing section
            if (selectedCampaign != null && !selectedCampaign.platforms.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Deliverables & Pricing", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                
                selectedCampaign.platforms?.forEach { campaignPlatform ->
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
                            val currentPrice = pricing[platformName]?.get(deliverable) ?: ""
                            
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
                                    val newPricing = pricing.toMutableMap()
                                    val platformPricing = newPricing[platformName]?.toMutableMap() ?: mutableMapOf()
                                    platformPricing[deliverable] = newPrice
                                    newPricing[platformName] = platformPricing
                                    pricing = newPricing
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
                    pricing.forEach { (platform, deliverables) ->
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

                    if (selectedCampaignId.isNotEmpty() && finalPricing.isNotEmpty()) {
                        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                            val token = result.token
                            if (token != null) {
                                brandViewModel.inviteInfluencer(
                                    token = token,
                                    influencerId = influencerId,
                                    campaignId = selectedCampaignId,
                                    message = message,
                                    pricing = finalPricing,
                                    onComplete = { success ->
                                        if (success) {
                                            onCreateProposal()
                                        }
                                    }
                                )
                            }
                        }
                    }
                },
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
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("CREATE PROPOSAL", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (error != null) {
                Text(
                    text = error ?: "Unknown error",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InfluencerCreateProposalPreview() {
    // Note: This preview requires dummy data for the ViewModels and influencerId.
}
