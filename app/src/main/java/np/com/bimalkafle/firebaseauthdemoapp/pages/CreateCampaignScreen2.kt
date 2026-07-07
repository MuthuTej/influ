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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.utils.IndianLocations

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateCampaignScreen2(
    onBack: () -> Unit = {}, 
    onNext: () -> Unit = {},
    campaignViewModel: CampaignViewModel = CampaignViewModel()
) {
    var selectedState by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf("") }

    val loading by campaignViewModel.loading.observeAsState(false)
    val error by campaignViewModel.error.observeAsState()
    val success by campaignViewModel.createCampaignSuccess.observeAsState(false)

    LaunchedEffect(success) {
        if (success) {
            onNext()
            campaignViewModel.clearState() // Clear form for the next use
        }
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.35f
    val formPaddingTop = headerHeight - 40.dp

    // Validation
    val isFormValid = campaignViewModel.budgetMin > 0 && 
                      campaignViewModel.budgetMax >= campaignViewModel.budgetMin &&
                      campaignViewModel.selectedLocations.isNotEmpty() &&
                      campaignViewModel.selectedGender.isNotBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(MaterialTheme.colorScheme.primary)
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
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_profile),
                    contentDescription = "Brand Logo",
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 14.sp
                )
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
            Text("Budget Range *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${campaignViewModel.budgetMin} - ${campaignViewModel.budgetMax}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            RangeSlider(
                value = campaignViewModel.budgetMin.toFloat()..campaignViewModel.budgetMax.toFloat(),
                onValueChange = { 
                    campaignViewModel.budgetMin = it.start.toInt()
                    campaignViewModel.budgetMax = it.endInclusive.toInt()
                },
                valueRange = 0f..1000000f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Target Location
            Text("Target Location *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    if (selectedState.isNotEmpty() && selectedCity.isNotEmpty()) {
                        val entry = "$selectedCity, $selectedState"
                        if (!campaignViewModel.selectedLocations.contains(entry)) {
                            campaignViewModel.selectedLocations = campaignViewModel.selectedLocations + entry
                        }
                        selectedCity = ""
                    }
                },
                enabled = selectedState.isNotEmpty() && selectedCity.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Location")
            }

            if (campaignViewModel.selectedLocations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    campaignViewModel.selectedLocations.forEach { location ->
                        LocationChip(location, isSelected = true) {
                            campaignViewModel.selectedLocations = campaignViewModel.selectedLocations - location
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Age Group
            Text("Age group *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${campaignViewModel.ageMin} - ${campaignViewModel.ageMax} years",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
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
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("min", fontSize = 12.sp, color = Color.Gray)
                Text("max", fontSize = 12.sp, color = Color.Gray)
            }


            Spacer(modifier = Modifier.height(24.dp))

            // Gender Split
            Text("Gender Split *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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

            if (error != null) {
                Text(
                    text = error!!,
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener { result ->
                        result.token?.let { token ->
                            campaignViewModel.createCampaign(token)
                        }
                    }
                },
                enabled = !loading && isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) MaterialTheme.colorScheme.primary else Color.LightGray
                )
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

@Composable
fun LocationChip(name: String, isSelected: Boolean, onSelected: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onSelected,
        label = { Text(name) },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun GenderButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White,
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
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
