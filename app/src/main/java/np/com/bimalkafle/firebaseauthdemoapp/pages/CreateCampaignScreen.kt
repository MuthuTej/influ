package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateCampaignScreen(
    onBack: () -> Unit = {}, 
    onNext: () -> Unit = {},
    campaignViewModel: CampaignViewModel = CampaignViewModel()
) {
    val platformsList = listOf("Youtube", "Instagram", "Facebook")
    val platformFormatsMap = mapOf(
        "Facebook" to listOf("reels/shorts", "post", "video", "story"),
        "Instagram" to listOf("reels/shorts", "post", "story"),
        "Youtube" to listOf("reels/shorts", "post", "video")
    )

    val categoriesMap = mapOf(
        "Fashion" to listOf("Clothing", "Footwear", "Accessories"),
        "Tech" to listOf("Gadgets", "Software", "Hardware"),
        "Food" to listOf("Organic", "Fast Food", "Dining"),
        "Beauty" to listOf("Skincare", "Makeup", "Haircare"),
        "Health" to listOf("Fitness", "Supplements", "Wellness")
    )

    var showDatePicker by remember { mutableStateOf(false) }
    var dateField by remember { mutableStateOf<String?>(null) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.35f
    val formPaddingTop = headerHeight - 40.dp

    // Validation
    val isFormValid = campaignViewModel.title.isNotBlank() &&
            campaignViewModel.description.isNotBlank() &&
            campaignViewModel.selectedCategories.isNotEmpty() &&
            campaignViewModel.selectedSubCategories.isNotEmpty() &&
            campaignViewModel.selectedPlatforms.isNotEmpty() &&
            campaignViewModel.selectedPlatforms.all { platform -> 
                campaignViewModel.platformFormats[platform]?.isNotEmpty() == true 
            } &&
            campaignViewModel.startDate != null

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
                    text = "Campaign Details",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Let's get started by filling out your campaign details",
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
            Text("General Information", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = campaignViewModel.title,
                onValueChange = { campaignViewModel.title = it },
                label = { Text("Campaign Name *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = campaignViewModel.description,
                onValueChange = { campaignViewModel.description = it },
                label = { Text("Campaign Brief *") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF8383))
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            // Multiselect Category Chips
            Text("Select Categories *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoriesMap.keys.forEach { category ->
                    val isSelected = campaignViewModel.selectedCategories.contains(category)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            campaignViewModel.selectedCategories = if (isSelected) {
                                campaignViewModel.selectedCategories - category
                            } else {
                                campaignViewModel.selectedCategories + category
                            }
                            // Cleanup subcategories if category unselected
                            if (isSelected) {
                                campaignViewModel.selectedSubCategories = campaignViewModel.selectedSubCategories - category
                            }
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF8383),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (campaignViewModel.selectedCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Sub-Categories *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                campaignViewModel.selectedCategories.forEach { category ->
                    Text(category, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoriesMap[category]?.forEach { subCat ->
                            val selectedSubCats = campaignViewModel.selectedSubCategories[category] ?: emptySet()
                            val isSelected = selectedSubCats.contains(subCat)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newSubCats = if (isSelected) selectedSubCats - subCat else selectedSubCats + subCat
                                    campaignViewModel.selectedSubCategories = campaignViewModel.selectedSubCategories + (category to newSubCats)
                                },
                                label = { Text(subCat, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFF8383).copy(alpha = 0.7f),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Target Platforms & Formats *", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            platformsList.forEach { platform ->
                val isPlatformSelected = campaignViewModel.selectedPlatforms.contains(platform)
                
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPlatformSelected) Color(0xFFFF8383).copy(alpha = 0.1f) else Color(0xFFF5F5F5))
                            .clickable {
                                campaignViewModel.selectedPlatforms = if (isPlatformSelected) {
                                    campaignViewModel.selectedPlatforms - platform
                                } else {
                                    campaignViewModel.selectedPlatforms + platform
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlatformIcon(platform)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(platform, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = isPlatformSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF8383))
                        )
                    }

                    if (isPlatformSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select format for $platform (min 1):", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            platformFormatsMap[platform]?.forEach { format ->
                                val selectedFormats = campaignViewModel.platformFormats[platform] ?: emptySet()
                                val isFormatSelected = selectedFormats.contains(format)
                                
                                FilterChip(
                                    selected = isFormatSelected,
                                    onClick = {
                                        val newFormats = if (isFormatSelected) selectedFormats - format else selectedFormats + format
                                        campaignViewModel.platformFormats = campaignViewModel.platformFormats + (platform to newFormats)
                                    },
                                    label = { Text(format, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFF8383),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Campaign Timeline", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            DateSelector(
                label = "Start Date *", 
                date = campaignViewModel.startDate, 
                modifier = Modifier.fillMaxWidth(), 
                onClick = { dateField = "start"; showDatePicker = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            DateSelector(
                label = "End Date (Optional)", 
                date = campaignViewModel.endDate, 
                modifier = Modifier.fillMaxWidth(), 
                onClick = { dateField = "end"; showDatePicker = true }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNext,
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFormValid) Color(0xFFFF8383) else Color.LightGray
                )
            ) {
                Text("NEXT", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (showDatePicker) {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val minDate = if (dateField == "end" && campaignViewModel.startDate != null) {
                campaignViewModel.startDate!!.time
            } else {
                today
            }

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = if (dateField == "start") (campaignViewModel.startDate?.time ?: today) else (campaignViewModel.endDate?.time ?: minDate),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= minDate
                    }
                }
            )
            
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let {
                            if (dateField == "start") {
                                campaignViewModel.startDate = Date(it)
                                // Reset end date if it's now before start date
                                if (campaignViewModel.endDate != null && campaignViewModel.endDate!!.before(campaignViewModel.startDate)) {
                                    campaignViewModel.endDate = null
                                }
                            } else {
                                campaignViewModel.endDate = Date(it)
                            }
                        }
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                }
            ) { DatePicker(state = datePickerState) }
        }
    }
}

@Composable
fun PlatformIcon(name: String) {
    val icon = when (name) {
        "Youtube" -> painterResource(id = R.drawable.ic_youtube)
        "Instagram" -> painterResource(id = R.drawable.ic_instagram)
        "Facebook" -> painterResource(id = R.drawable.ic_facebook)
        else -> painterResource(id = R.drawable.splash1)
    }
    Image(painter = icon, contentDescription = name, modifier = Modifier.size(24.dp))
}

@Composable
fun DateSelector(label: String, date: Date?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateString = date?.let { dateFormatter.format(it) } ?: "Select Date"

    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(dateString, fontSize = 14.sp)
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateCampaignScreenPreview() {
    CreateCampaignScreen()
}
