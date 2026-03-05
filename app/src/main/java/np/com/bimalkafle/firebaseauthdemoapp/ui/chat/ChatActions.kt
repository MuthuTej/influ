package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration

@Composable
fun RestrictedActionPanel(
    status: String?,
    collaborationId: String?,
    isBrand: Boolean,
    onSend: (String, String, Map<String, Any>) -> Unit,
    onSendUpload: (String, String) -> Unit = { _, _ -> },
    onStatusUpdate: (String) -> Unit = {},
    collaboration: Collaboration? = null
) {
    var showNegotiation by remember { mutableStateOf(false) }
    var showBrief by remember { mutableStateOf(false) }
    var showScript by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showUpload by remember { mutableStateOf(false) }

    if (status == null) return

    val statusMessage = when (status) {
        "PENDING" -> "Waiting for proposal acceptance"
        "NEGOTIATION" -> "Negotiation in progress"
        "ACCEPTED" -> "Waiting for campaign brief"
        "BRIEF_SENT" -> "Brief sent, waiting for approval"
        "BRIEF_FINALIZED" -> "Brief finalized, waiting for script"
        "SCRIPT_SENT" -> "Script sent, waiting for approval"
        "IN_PROGRESS" -> "Collaboration in progress"
        "WAITING_FOR_PAYMENT" -> "Waiting for payment"
        "COMPLETED" -> "Collaboration completed"
        "REJECTED" -> "Proposal rejected"
        "REVOKED" -> "Proposal withdrawn"
        else -> status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    Surface(
        color = Color.White,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Surface(
                    color = Color(0xFFFF8383).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF8383),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Negotiation Phase
                if (status == "PENDING" || status == "NEGOTIATION") {
                    item { ActionCard("Negotiate", Icons.Default.AttachMoney) { showNegotiation = true } }
                }

                // Briefing Phase
                if (status == "ACCEPTED" && isBrand) {
                    item { ActionCard("Send Brief", Icons.Default.Description) { showBrief = true } }
                }
                if (status == "BRIEF_SENT" && !isBrand) {
                    item { ActionCard("Finalize Brief", Icons.Default.CheckCircle) { onStatusUpdate("BRIEF_FINALIZED") } }
                }

                // Scripting Phase
                if (status == "BRIEF_FINALIZED" && !isBrand) {
                    item { ActionCard("Submit Script", Icons.Default.EditNote) { showScript = true } }
                }
                if (status == "SCRIPT_SENT" && isBrand) {
                    item { ActionCard("Approve Script", Icons.Default.CheckCircle) { onStatusUpdate("IN_PROGRESS") } }
                }
                
                // Implementation Phase
                if (status == "IN_PROGRESS" && !isBrand) {
                    item { ActionCard("Complete Work", Icons.Default.CloudUpload) { showUpload = true } }
                }
                
                if (status == "WAITING_FOR_PAYMENT" && isBrand) {
                    item { ActionCard("Release Payment", Icons.Default.Payments) { onStatusUpdate("COMPLETED") } }
                }

                // Always available (except when completed)
                if (status != "COMPLETED" && status != "REJECTED" && status != "REVOKED") {
                    item { ActionCard("Feedback", Icons.Default.Feedback) { showFeedback = true } }
                }
            }
        }
    }

    // Dialogs
    if (showNegotiation) {
        NegotiationDialog(
            onDismiss = { showNegotiation = false },
            collaboration = collaboration,
            onSend = { amount, platform, deliverables ->
                val delStr = deliverables.entries.joinToString { "${it.key} (x${it.value})" }
                onSend("Negotiated Proposal: ₹$amount on $platform - $delStr", "NEGOTIATION", mapOf("amount" to amount, "platform" to platform, "items" to deliverables))
                onStatusUpdate("NEGOTIATION")
                showNegotiation = false
            }
        )
    }

    if (showBrief) {
        TextInputDialog(
            title = "Share Campaign Brief",
            label = "Brief Link",
            onDismiss = { showBrief = false },
            onSend = { link ->
                onSend("Campaign Brief Shared", "BRIEF", mapOf("link" to link))
                onStatusUpdate("BRIEF_SENT")
                showBrief = false
            }
        )
    }

    if (showScript) {
        TextInputDialog(
            title = "Submit Script",
            label = "Script Content",
            multiline = true,
            onDismiss = { showScript = false },
            onSend = { content ->
                onSend("Script Submitted", "SCRIPT", mapOf("content" to content))
                onStatusUpdate("SCRIPT_SENT")
                showScript = false
            }
        )
    }

    if (showFeedback) {
        TextInputDialog(
            title = "Feedback",
            label = "Enter your feedback",
            multiline = true,
            onDismiss = { showFeedback = false },
            onSend = { feedback ->
                onSend("Feedback Provided", "FEEDBACK", mapOf("feedback" to feedback))
                showFeedback = false
            }
        )
    }

    if (showUpload) {
        ContentUploadDialog(
            onDismiss = { showUpload = false },
            onSend = { links, platform ->
                links.forEach { link ->
                    onSendUpload(link, platform)
                }
                onStatusUpdate("WAITING_FOR_PAYMENT")
                showUpload = false
            }
        )
    }
}

@Composable
fun ContentUploadDialog(
    onDismiss: () -> Unit,
    onSend: (List<String>, String) -> Unit
) {
    val links = remember { mutableStateListOf("") }
    var selectedPlatform by remember { mutableStateOf("YouTube") }
    var showPlatformDropdown by remember { mutableStateOf(false) }
    val platforms = listOf("YouTube", "Instagram")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Work") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text("Select Platform", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showPlatformDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedPlatform, color = Color.Black)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                        }
                    }
                    DropdownMenu(
                        expanded = showPlatformDropdown,
                        onDismissRequest = { showPlatformDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        platforms.forEach { platform ->
                            DropdownMenuItem(
                                text = { Text(platform) },
                                onClick = {
                                    selectedPlatform = platform
                                    showPlatformDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Final Content Links", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                
                links.forEachIndexed { index, link ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        TextField(
                            value = link,
                            onValueChange = { links[index] = it },
                            placeholder = { Text("Enter link...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color(0xFFFF8383),
                                cursorColor = Color(0xFFFF8383)
                            )
                        )
                        if (links.size > 1) {
                            IconButton(onClick = { links.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = { links.add("") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Another Link", color = Color(0xFFFF8383))
                }

                if (selectedPlatform == "Instagram") {
                    Text(
                        "Note: Entering Instagram links will trigger automated verification of post statistics.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val filteredLinks = links.filter { it.isNotBlank() }
                    if (filteredLinks.isNotEmpty()) onSend(filteredLinks, selectedPlatform) 
                },
                enabled = links.any { it.isNotBlank() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(100.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8F9FA),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9ECEF))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF8383).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFFFF8383),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NegotiationDialog(
    initialAmount: Int = 100,
    initialPlatform: String = "Instagram",
    initialDeliverables: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onSend: (Int, String, Map<String, Int>) -> Unit,
    collaboration: Collaboration? = null
) {
    val allowedPlatforms = remember(collaboration) {
        collaboration?.pricing?.map { it.platform }?.distinct() ?: listOf(initialPlatform)
    }
    val allowedDeliverables = remember(collaboration) {
        collaboration?.pricing?.map { it.deliverable }?.distinct() ?: listOf("Post", "Reel", "Story", "Video")
    }

    var budget by remember { mutableStateOf(initialAmount.toString()) }
    var selectedPlatform by remember { mutableStateOf(if (allowedPlatforms.contains(initialPlatform)) initialPlatform else allowedPlatforms.firstOrNull() ?: "") }
    var showPlatformDropdown by remember { mutableStateOf(false) }

    val selectedDeliverables = remember { mutableStateMapOf<String, Int>().apply { putAll(initialDeliverables) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Propose Negotiation") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Select Platform", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Box {
                    OutlinedButton(
                        onClick = { showPlatformDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedPlatform, color = Color.Black)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                        }
                    }
                    DropdownMenu(
                        expanded = showPlatformDropdown,
                        onDismissRequest = { showPlatformDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        allowedPlatforms.forEach { platform ->
                            DropdownMenuItem(
                                text = { Text(platform) },
                                onClick = {
                                    selectedPlatform = platform
                                    showPlatformDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Proposed Budget (₹)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF8383),
                        cursorColor = Color(0xFFFF8383)
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(16.dp))

                Text("Select Deliverables & Counts", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                allowedDeliverables.forEach { option ->
                    val count = selectedDeliverables[option] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = count > 0,
                                onCheckedChange = {
                                    if (it) selectedDeliverables[option] = 1 else selectedDeliverables.remove(option)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF8383))
                            )
                            Text(text = option, fontSize = 14.sp)
                        }

                        if (count > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (count > 1) selectedDeliverables[option] = count - 1 else selectedDeliverables.remove(option) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "$count",
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { selectedDeliverables[option] = count + 1 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBudget = budget.toIntOrNull() ?: initialAmount
                    onSend(finalBudget, selectedPlatform, selectedDeliverables.toMap())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Send Proposal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun DeliverablesDialog(
    initialDeliverables: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onSend: (Map<String, Int>) -> Unit,
    collaboration: Collaboration? = null
) {
    val allowedDeliverables = remember(collaboration) {
        collaboration?.pricing?.map { it.deliverable }?.distinct() ?: listOf("Post", "Reel", "Story", "Video")
    }

    val selectedDeliverables = remember { mutableStateMapOf<String, Int>().apply { putAll(initialDeliverables) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Deliverables") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Select Deliverables & Counts", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                allowedDeliverables.forEach { option ->
                    val count = selectedDeliverables[option] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = count > 0,
                                onCheckedChange = {
                                    if (it) selectedDeliverables[option] = 1 else selectedDeliverables.remove(option)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF8383))
                            )
                            Text(text = option, fontSize = 14.sp)
                        }

                        if (count > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (count > 1) selectedDeliverables[option] = count - 1 else selectedDeliverables.remove(option) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "$count",
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { selectedDeliverables[option] = count + 1 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSend(selectedDeliverables.toMap())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update Deliverables", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    multiline: Boolean = false,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = !multiline,
                minLines = if (multiline) 3 else 1,
                maxLines = if (multiline) 5 else 1,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color(0xFFFF8383),
                    cursorColor = Color(0xFFFF8383)
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (text.isNotBlank()) onSend(text) 
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
            ) {
                Text("Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
