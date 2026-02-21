package np.com.bimalkafle.firebaseauthdemoapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun RestrictedActionPanel(
    status: String?,
    isBrand: Boolean,
    onSend: (String, String, Map<String, Any>) -> Unit
) {
    var showNegotiation by remember { mutableStateOf(false) }
    var showDeliverables by remember { mutableStateOf(false) }
    var showBrief by remember { mutableStateOf(false) }
    var showScript by remember { mutableStateOf(false) }
    var showFeedback by remember { mutableStateOf(false) }
    var showUpload by remember { mutableStateOf(false) }

    if (status == null) return // No collaboration selected, no actions shown

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.White)
    ) {
        Text(
            text = "Workflow Actions (${status})",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            // Pending/Negotiation - Both can negotiate price and deliverables
            if (status == "PENDING" || status == "NEGOTIATION") {
                ActionIcon("Negotiate", Icons.Default.AttachMoney) { showNegotiation = true }
                ActionIcon("Deliverables", Icons.Default.List) { showDeliverables = true }
            }

            // Accepted - Brand sends brief
            if (status == "ACCEPTED" && isBrand) {
                ActionIcon("Send Brief", Icons.Default.Description) { showBrief = true }
            }

            // Brief Sent - Influencer sends script
            if (status == "BRIEF_SENT" && !isBrand) {
                ActionIcon("Submit Script", Icons.Default.EditNote) { showScript = true }
            }
            
            // In Progress - Influencer uploads content
            if (status == "IN_PROGRESS" && !isBrand) {
                ActionIcon("Upload", Icons.Default.CloudUpload) { showUpload = true }
            }

            // Feedback - Always show
            ActionIcon("Feedback", Icons.Default.Feedback) { showFeedback = true }
        }
    }

    if (showNegotiation) {
        NegotiationDialog(
            onDismiss = { showNegotiation = false },
            onSend = { amount ->
                onSend("Proposed Budget: $$amount", "NEGOTIATION", mapOf("amount" to amount))
                showNegotiation = false
            }
        )
    }

    if (showDeliverables) {
        DeliverablesDialog(
            onDismiss = { showDeliverables = false },
            onSend = { deliverables ->
                val text = "Deliverables: ${deliverables.entries.joinToString { "${it.key} (x${it.value})" }}"
                onSend(text, "DELIVERABLES", mapOf("items" to deliverables))
                showDeliverables = false
            }
        )
    }

    if (showBrief) {
        TextInputDialog(
            title = "Share Campaign Brief",
            label = "Brief Link (Google Doc/Drive)",
            onDismiss = { showBrief = false },
            onSend = { link ->
                onSend("Campaign Brief Shared", "BRIEF", mapOf("link" to link))
                showBrief = false
            }
        )
    }

    if (showScript) {
        TextInputDialog(
            title = "Submit Script",
            label = "Script Content or Link",
            multiline = true,
            onDismiss = { showScript = false },
            onSend = { content ->
                onSend("Script Submitted", "SCRIPT", mapOf("content" to content))
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
        TextInputDialog(
            title = "Upload Content",
            label = "Final Content Link",
            onDismiss = { showUpload = false },
            onSend = { link ->
                onSend("Content Uploaded", "UPLOAD", mapOf("link" to link))
                showUpload = false
            }
        )
    }
}

@Composable
fun ActionIcon(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFFF8383),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            fontSize = 10.sp
        )
    }
}

@Composable
fun NegotiationDialog(
    initialAmount: Int = 100,
    onDismiss: () -> Unit,
    onSend: (Int) -> Unit
) {
    var budget by remember { mutableFloatStateOf(initialAmount.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Propose Budget") },
        text = {
            Column {
                Text("Budget: $${budget.toInt()}")
                Slider(
                    value = budget,
                    onValueChange = { budget = it },
                    valueRange = 0f..5000f,
                    steps = 49
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(budget.toInt()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
            ) {
                Text("Send Proposal")
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
fun DeliverablesDialog(
    initialDeliverables: Map<String, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onSend: (Map<String, Int>) -> Unit
) {
    val options = listOf("Reel", "IG Story", "YouTube Short", "TikTok", "Post")
    val selected = remember { mutableStateMapOf<String, Int>().apply { putAll(initialDeliverables) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Deliverables") },
        text = {
            LazyColumn {
                items(options) { option ->
                    val count = selected[option] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = count > 0,
                                onCheckedChange = { 
                                    if (it) selected[option] = 1 else selected.remove(option)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF8383))
                            )
                            Text(text = option)
                        }

                        if (count > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (count > 1) selected[option] = count - 1 else selected.remove(option) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text(
                                    text = "$count",
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { selected[option] = count + 1 },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
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
                    if (selected.isNotEmpty()) onSend(selected.toMap()) 
                },
                enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
            ) {
                Text("Confirm")
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
