package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Prompts the current user to rate the other party of a just-completed
 * collaboration. Intentionally has no close/cancel action — the only way
 * out is submitting a rating — after which it shows a brief thank-you
 * message and closes itself.
 */
@Composable
fun RatingPromptDialog(
    revieweeName: String,
    onSubmit: suspend (rating: Int, comment: String) -> Result<Unit>,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var submitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(submitted) {
        if (submitted) {
            delay(2000)
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = { /* not dismissible except by submitting a rating */ },
        title = {
            Text(
                text = if (submitted) "Thank you!" else "Rate your experience",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (submitted) {
                Text("Thank you for your time", fontSize = 14.sp)
            } else {
                Column {
                    Text(
                        text = "How was your collaboration with $revieweeName?",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { index ->
                            val filled = index < rating
                            Icon(
                                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Rate ${index + 1} star${if (index == 0) "" else "s"}",
                                tint = if (filled) Color(0xFFFFC107) else Color.LightGray,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable(enabled = !submitting) { rating = index + 1 }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Add a comment (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting,
                        minLines = 2
                    )
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error ?: "", color = Color.Red, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (!submitted) {
                Button(
                    enabled = rating > 0 && !submitting,
                    onClick = {
                        error = null
                        submitting = true
                        scope.launch {
                            val result = onSubmit(rating, comment)
                            submitting = false
                            if (result.isSuccess) {
                                submitted = true
                            } else {
                                error = "Couldn't submit your rating. Please try again."
                            }
                        }
                    }
                ) {
                    if (submitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Submit")
                    }
                }
            }
        }
    )
}
