package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateCampaignScreen(onBack: () -> Unit = {}, onNext: () -> Unit = {}) {
    var campaignName by remember { mutableStateOf("") }
    var campaignBrief by remember { mutableStateOf("") }
    val platforms = listOf("Youtube", "Instagram", "Facebook", "Twitter", "Spotify", "Discord")
    var selectedPlatforms by remember { mutableStateOf(setOf<String>()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var dateField by remember { mutableStateOf<String?>(null) }


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
                modifier = Modifier.fillMaxSize(),
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
                    painter = painterResource(id = R.drawable.splash1),
                    contentDescription = "Brand Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
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
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Stepper


            }
        }

        // Form Content
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                .padding(horizontal = 24.dp) // Adds margin to the left and right of the card
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .padding(24.dp) // Adds padding inside the card
                .verticalScroll(rememberScrollState())
        ) {
            Text("Name of the Campaign", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = campaignName,
                onValueChange = { campaignName = it },
                placeholder = { Text("Enter campaign name...") },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedBorderColor = Color(0xFFFF8383),
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Campaign Brief", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = campaignBrief,
                onValueChange = { campaignBrief = it },
                placeholder = { Text("Short description...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedBorderColor = Color(0xFF4B4CED),
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Required Platforms", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                platforms.forEach { platform ->
                    val isSelected = selectedPlatforms.contains(platform)
                    PlatformChip(platform, isSelected) {
                        selectedPlatforms = if (isSelected) {
                            selectedPlatforms - platform
                        } else {
                            selectedPlatforms + platform
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Optional Campaign Timeline", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DateSelector(label = "Start Date", date = selectedDate, modifier = Modifier.weight(1f), onClick = { dateField = "start"; showDatePicker = true })
                DateSelector(label = "End Date", date = selectedDate, modifier = Modifier.weight(1f), onClick = { dateField = "end"; showDatePicker = true })
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onNext,
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

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = System.currentTimeMillis(),
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= Calendar.getInstance().timeInMillis
                    }
                }
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Date(it)
                        }
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState
                )
            }
        }
    }
}

@Composable
fun PlatformChip(name: String, isSelected: Boolean, onSelected: () -> Unit) {
    val icon = when (name) {
        "Youtube" -> painterResource(id = R.drawable.ic_youtube) // Replace with actual icons
        "Instagram" -> painterResource(id = R.drawable.ic_instagram)
        "Facebook" -> painterResource(id = R.drawable.ic_facebook)
        "Twitter" -> painterResource(id = R.drawable.ic_twitter)
        "Spotify" -> painterResource(id = R.drawable.ic_spotify)
        "Discord" -> painterResource(id = R.drawable.ic_discord)
        else -> painterResource(id = R.drawable.splash1)
    }
    FilterChip(
        selected = isSelected,
        onClick = onSelected,
        label = { Text(name) },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "$name selected",
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Image(
                    painter = icon,
                    contentDescription = "$name logo",
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFFF8383).copy(alpha = 0.2f),
            labelColor = Color.Black,
            selectedLabelColor = Color.Black
        ),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF8383) else Color.LightGray)
    )
}


@Composable
fun DateSelector(label: String, date: Date?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateString = date?.let { dateFormatter.format(it) } ?: "Select Date"

    Column(modifier = modifier) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                .padding(12.dp)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(dateString)
            Icon(Icons.Default.CalendarToday, contentDescription = "Select Date", tint = Color.Gray)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CreateCampaignScreenPreview() {
    CreateCampaignScreen()
}
