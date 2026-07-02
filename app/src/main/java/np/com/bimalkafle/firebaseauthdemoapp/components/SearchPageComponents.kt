package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
fun IconBubbleSearch(
    icon: ImageVector,
    tint: Color,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        modifier = Modifier
            .size(42.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    selectedOptions: Set<String>,
    options: List<Pair<String, Int?>>,
    onOptionToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchable: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("") }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFEAEA), // Light pinkish for the chip
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayText = if (selectedOptions.contains("All") || selectedOptions.isEmpty()) {
                    label
                } else if (selectedOptions.size == 1) {
                    selectedOptions.first()
                } else {
                    "${selectedOptions.size} Selected"
                }

                Text(
                    text = displayText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
            }
        }

        // Dummy box at the bottom of the chip to act as anchor for DropdownMenu
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomStart)) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    filterText = ""
                },
                offset = DpOffset(x = 0.dp, y = 4.dp),
                modifier = Modifier
                    .background(Color.White)
                    .widthIn(min = 90.dp, max = 150.dp)
            ) {
                if (searchable) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Search...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }

                val displayedOptions = if (searchable && filterText.isNotBlank()) {
                    options.filter { it.first.contains(filterText, ignoreCase = true) || it.first == "All" }
                } else {
                    options
                }

                Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    displayedOptions.forEach { (option, count) ->
                        val isSelected = selectedOptions.contains(option)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (option == "All" || count == null) option else "$option ($count)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onOptionToggle(option)
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(
                        onClick = {
                            expanded = false
                            filterText = ""
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text("OK", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AudienceFilterDropdown(
    selectedGenders: Set<String>,
    onGenderToggle: (String) -> Unit,
    genderOptions: List<Pair<String, Int?>>,
    selectedAgeRanges: Set<String>,
    onAgeToggle: (String) -> Unit,
    ageOptions: List<Pair<String, Int?>>,
    selectedLocations: Set<String>,
    onLocationToggle: (String) -> Unit,
    locationOptions: List<Pair<String, Int?>>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var locationSearchText by remember { mutableStateOf("") }
    
    var isGenderExpanded by remember { mutableStateOf(false) }
    var isAgeExpanded by remember { mutableStateOf(false) }
    var isLocationExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFFFEAEA),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val totalSelected = (if(selectedGenders.contains("All")) 0 else selectedGenders.size) +
                                    (if(selectedAgeRanges.contains("All")) 0 else selectedAgeRanges.size) +
                                    (if(selectedLocations.contains("All")) 0 else selectedLocations.size)
                
                val displayText = if (totalSelected == 0) "Audience" else "Audience ($totalSelected)"

                Text(
                    text = displayText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomStart)) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { 
                    expanded = false 
                    isGenderExpanded = false
                    isAgeExpanded = false
                    isLocationExpanded = false
                },
                offset = DpOffset(x = 0.dp, y = 4.dp),
                modifier = Modifier
                    .background(Color.White)
                    .widthIn(min = 250.dp, max = 300.dp)
            ) {
                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // --- GENDER SECTION ---
                        AudienceSubHeader("Audience Gender", isGenderExpanded) { 
                            isGenderExpanded = !isGenderExpanded
                            if (isGenderExpanded) { isAgeExpanded = false; isLocationExpanded = false }
                        }
                        if (isGenderExpanded) {
                            genderOptions.forEach { (option, count) ->
                                val isSelected = selectedGenders.contains(option)
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = if(count != null) "$option ($count)" else option, fontSize = 13.sp)
                                            if(isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = { onGenderToggle(option) }
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                        // --- AGE SECTION ---
                        AudienceSubHeader("Audience Age", isAgeExpanded) { 
                            isAgeExpanded = !isAgeExpanded
                            if (isAgeExpanded) { isGenderExpanded = false; isLocationExpanded = false }
                        }
                        if (isAgeExpanded) {
                            ageOptions.forEach { (option, count) ->
                                val isSelected = selectedAgeRanges.contains(option)
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = if(count != null) "$option ($count)" else option, fontSize = 13.sp)
                                            if(isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = { onAgeToggle(option) }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))

                        // --- LOCATION SECTION ---
                        AudienceSubHeader("Audience Location", isLocationExpanded) { 
                            isLocationExpanded = !isLocationExpanded
                            if (isLocationExpanded) { isGenderExpanded = false; isAgeExpanded = false }
                        }
                        if (isLocationExpanded) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                OutlinedTextField(
                                    value = locationSearchText,
                                    onValueChange = { locationSearchText = it },
                                    placeholder = { Text("Search location...", fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(fontSize = 12.sp)
                                )
                            }
                            
                            val filteredLocs = if (locationSearchText.isBlank()) locationOptions else locationOptions.filter { it.first.contains(locationSearchText, ignoreCase = true) || it.first == "All" }
                            
                            filteredLocs.take(10).forEach { (option, count) ->
                                val isSelected = selectedLocations.contains(option)
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = if(count != null) "$option ($count)" else option, fontSize = 13.sp)
                                            if(isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = { onLocationToggle(option) }
                                )
                            }
                        }
                    }
                }
                
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { expanded = false }) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudienceSubHeader(title: String, isExpanded: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Gray
        )
    }
}

@Composable
fun SearchSuggestionsPopup(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (isVisible && suggestions.isNotEmpty()) {
        Popup(
            alignment = Alignment.TopCenter,
            onDismissRequest = { },
            properties = PopupProperties(focusable = false)
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(max = 250.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Suggestions",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(suggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSuggestionClick(suggestion) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = suggestion,
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            if (suggestion != suggestions.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = Color.LightGray.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
