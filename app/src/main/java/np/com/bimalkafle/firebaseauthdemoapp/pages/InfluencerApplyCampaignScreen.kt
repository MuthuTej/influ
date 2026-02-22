package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
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
    
    // Map of platform -> Map of deliverable -> price string
    var pricingMap by remember { mutableStateOf(mapOf<String, MutableMap<String, String>>()) }

    LaunchedEffect(campaignId) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            if (token != null) {
                campaignViewModel.fetchCampaignById(campaignId, token)
            }
        }
    }

    LaunchedEffect(campaignDetail) {
        campaignDetail?.brand?.preferredPlatforms?.forEach { pref ->
            val platformPricing = pricingMap.toMutableMap()
            val priceMap = platformPricing[pref.platform] ?: mutableMapOf()
            pref.formats?.forEach { format ->
                if (!priceMap.containsKey(format)) {
                    priceMap[format] = ""
                }
            }
            platformPricing[pref.platform] = priceMap
            pricingMap = platformPricing
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.3f
    val formPaddingTop = headerHeight - 80.dp

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
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
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(80.dp)
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
                    fontSize = 20.sp
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
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                color = Color(0xFFFF8383)
            )

            // Campaign Name (Read Only)
            Text("Campaign", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(
                value = campaignDetail?.title ?: "Loading...",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF8383),
                    unfocusedBorderColor = Color.LightGray
                )
            )

            // Message
            Text("Your Proposal Message", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 4.dp, bottom = 16.dp),
                placeholder = { Text("Describe why you are a good fit...") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pricing for each deliverable defined in campaign's platforms
            Text("Your Pricing", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            campaignDetail?.brand?.preferredPlatforms?.forEach { pref ->
                Text(
                    text = pref.platform.uppercase(),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color(0xFFFF8383),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                pref.formats?.forEach { format ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(format, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = pricingMap[pref.platform]?.get(format) ?: "",
                                onValueChange = { newVal ->
                                    val pMap = pricingMap.toMutableMap()
                                    val dPriceMap = pMap[pref.platform]?.toMutableMap() ?: mutableMapOf()
                                    dPriceMap[format] = newVal
                                    pMap[pref.platform] = dPriceMap
                                    pricingMap = pMap
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Price for $format (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                prefix = { Text("₹ ") },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val finalPricing = mutableListOf<Map<String, Any>>()
                    pricingMap.forEach { (platform, deliverables) ->
                        deliverables.forEach { (deliverable, price) ->
                            if (price.isNotEmpty()) {
                                finalPricing.add(
                                    mapOf(
                                        "platform" to platform,
                                        "deliverable" to deliverable,
                                        "price" to (price.toIntOrNull() ?: 0),
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
                        Toast.makeText(context, "Please enter pricing for at least one deliverable", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(context, "Proposal submitted successfully!", Toast.LENGTH_LONG).show()
                                        onApplySuccess()
                                    }
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("SUBMIT PROPOSAL", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 1.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
