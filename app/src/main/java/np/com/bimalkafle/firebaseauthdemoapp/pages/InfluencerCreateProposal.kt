package np.com.bimalkafle.firebaseauthdemoapp.pages

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
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
    
    val platforms = listOf("Instagram", "Facebook", "Twitter", "YouTube")
    val selectedPlatforms = remember { mutableStateListOf<String>() }
    val deliverables = listOf("Post", "Reels", "Story", "Videos")
    var deliverableQuantities by remember { mutableStateOf(deliverables.associateWith { 0 }) }
    var pricing by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }

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

            Text("Campaign", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            var campaignExpanded by remember { mutableStateOf(false) }
            val selectedCampaignTitle = myCampaigns.find { it.id == selectedCampaignId }?.title ?: "Select Campaign"
            
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

            Spacer(modifier = Modifier.height(16.dp))
            Text("Deliverables Offered", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                deliverables.forEach { deliverable ->
                    DeliverableRow(
                        deliverable = deliverable,
                        quantity = deliverableQuantities[deliverable] ?: 0,
                        onQuantityChange = { quantity ->
                            deliverableQuantities = deliverableQuantities.toMutableMap().apply {
                                this[deliverable] = quantity
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Pricing", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))

            val selectedDeliverables = deliverableQuantities.filter { it.value > 0 }.keys.toList()

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
                                    label = { Text("Price per $deliverable") },
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
                onClick = {
                    val finalPricing = mutableListOf<Map<String, Any>>()
                    pricing.forEach { (platform, deliverables) ->
                        deliverables.forEach { (deliverable, price) ->
                            if (price.isNotEmpty()) {
                                finalPricing.add(
                                    mapOf(
                                        "platform" to platform.uppercase(),
                                        "deliverable" to deliverable,
                                        "price" to (price.toIntOrNull() ?: 0),
                                        "currency" to "INR" // Default currency
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliverableRow(deliverable: String, quantity: Int, onQuantityChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val quantityOptions = (0..10).toList()

    val iconRes = when (deliverable) {
        "Post" -> R.drawable.brand2 // Replace with actual icons
        "Reels" -> R.drawable.brand2
        "Story" -> R.drawable.brand2
        "Videos" -> R.drawable.brand2
        else -> R.drawable.brand2
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(deliverable)
        }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = quantity.toString(),
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .width(80.dp)
                    .menuAnchor(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                quantityOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option.toString()) }, onClick = { onQuantityChange(option); expanded = false })
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun InfluencerCreateProposalPreview() {
    InfluencerCreateProposal(
        onBack = {}, onCreateProposal = {},
        influencerId = TODO(),
        brandViewModel = TODO(),
        authViewModel = TODO()
    )
}
