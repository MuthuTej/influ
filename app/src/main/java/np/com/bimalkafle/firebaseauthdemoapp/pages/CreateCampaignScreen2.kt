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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCampaignScreen2(
    onBack: () -> Unit = {}, 
    onNext: () -> Unit = {},
    campaignViewModel: CampaignViewModel = CampaignViewModel(),
    authViewModel: AuthViewModel = AuthViewModel()
) {
    val locations = listOf("India", "USA", "Sri Lanka", "Maldives", "UK")
    
    val loading by campaignViewModel.loading.observeAsState(false)
    val error by campaignViewModel.error.observeAsState()
    val success by campaignViewModel.createCampaignSuccess.observeAsState(false)

    LaunchedEffect(success) {
        if (success) {
            onNext()
            // clear state if needed, or keep it for review
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.4f
    val formPaddingTop = headerHeight - 40.dp

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
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_profile),
                    contentDescription = "Brand Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Budget and Audience",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fill your budget and the target audience for your campaign",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Stepper can be added here
            }
        }

        // Form Content
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                .padding(horizontal = 24.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Budget Range
            Text("Budget Range", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "₹${formatBudgetValue(campaignViewModel.budgetMin.toFloat())} - ${formatBudgetValue(campaignViewModel.budgetMax.toFloat())}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            RangeSlider(
                value = campaignViewModel.budgetMin.toFloat()..campaignViewModel.budgetMax.toFloat(),
                onValueChange = { 
                    campaignViewModel.budgetMin = it.start.toInt()
                    campaignViewModel.budgetMax = it.endInclusive.toInt()
                },
                valueRange = 1f..500f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF8383),
                    activeTrackColor = Color(0xFFFF8383)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Target Location
            Text("Target Location", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                locations.forEach { location ->
                    val isSelected = campaignViewModel.selectedLocations.contains(location)
                    LocationChip(location, isSelected) {
                        campaignViewModel.selectedLocations = if (isSelected) {
                            campaignViewModel.selectedLocations - location
                        } else {
                            campaignViewModel.selectedLocations + location
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Age Group
            Text("Age group", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${campaignViewModel.ageMin} - ${campaignViewModel.ageMax}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            RangeSlider(
                value = campaignViewModel.ageMin.toFloat()..campaignViewModel.ageMax.toFloat(),
                onValueChange = { 
                    campaignViewModel.ageMin = it.start.toInt()
                    campaignViewModel.ageMax = it.endInclusive.toInt()
                },
                valueRange = 1f..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF8383),
                    activeTrackColor = Color(0xFFFF8383)
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("min", fontWeight =FontWeight.SemiBold)
                Text("max", fontWeight =FontWeight.SemiBold)
            }


            Spacer(modifier = Modifier.height(24.dp))

            // Gender Split
            Text("Gender Split", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenderButton("Male", campaignViewModel.selectedGender == "Male", { campaignViewModel.selectedGender = "Male" }, Modifier.weight(1f))
                GenderButton("Female", campaignViewModel.selectedGender == "Female", { campaignViewModel.selectedGender = "Female" }, Modifier.weight(1f))
                GenderButton("Any", campaignViewModel.selectedGender == "Any", { campaignViewModel.selectedGender = "Any" }, Modifier.weight(1f))
            }


            Spacer(modifier = Modifier.height(32.dp))

            // Next Button
            if (error != null) {
                Text(
                    text = error!!,
                    color = Color.Red,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Next Button (Submit)
            Button(
                onClick = {
                    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { result ->
                        result.token?.let { token ->
                            campaignViewModel.createCampaign(token)
                        }
                    }
                },
                enabled = !loading,
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
                        .background(if (loading) Color.Gray else Color(0xFFFF8383)),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SUBMIT", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatBudgetValue(value: Float): String {
    return if (value >= 100) {
        val lakhs = value / 100f
        if (lakhs.rem(1) == 0f) {
            "${lakhs.toInt()}L"
        } else {
            String.format("%.1f", lakhs) + "L"
        }
    } else {
        "${value.toInt()}K"
    }
}

@Composable
fun LocationChip(name: String, isSelected: Boolean, onSelected: () -> Unit) {
    val countryIcon = when (name) {
        "India" -> "🇮🇳"
        "USA" -> "🇺🇸"
        "Sri Lanka" -> "🇱🇰"
        "Maldives" -> "🇲🇻"
        "UK" -> "🇬🇧"
        else -> ""
    }

    FilterChip(
        selected = isSelected,
        onClick = onSelected,
        label = { Text(name) },
        leadingIcon = {
            Text(countryIcon, fontSize = 20.sp)
        },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFF8383).copy(alpha = 0.2f),
        ),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF8383) else Color.LightGray)
    )
}

@Composable
fun GenderButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) Color(0xFFFF8383).copy(alpha = 0.2f) else Color.Transparent,
            contentColor = Color.Black
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFFF8383) else Color.LightGray
        )
    ) {
        Text(text)
    }
}


@Preview(showBackground = true)
@Composable
fun CreateCampaignScreen2Preview() {
    CreateCampaignScreen2()
}
