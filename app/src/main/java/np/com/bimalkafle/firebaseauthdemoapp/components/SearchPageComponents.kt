package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White).widthIn(min = 150.dp)
        ) {
            options.forEach { (option, count) ->
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
