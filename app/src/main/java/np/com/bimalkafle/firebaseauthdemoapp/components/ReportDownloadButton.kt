package np.com.bimalkafle.firebaseauthdemoapp.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import np.com.bimalkafle.firebaseauthdemoapp.utils.ReportFileSaver

private enum class ReportFormat(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    CSV("csv", "text/csv")
}

/**
 * Download icon that expands into Save/Share x PDF/CSV. Generation runs off
 * the main thread. Saving straight to Downloads needs no permission on API
 * 29+ (MediaStore.Downloads), but this app's minSdk (26) predates that API,
 * so 26-28 requests WRITE_EXTERNAL_STORAGE at runtime before writing.
 */
@Composable
fun ReportDownloadButton(
    enabled: Boolean,
    fileBaseName: String,
    generatePdf: () -> ByteArray,
    generateCsv: () -> ByteArray,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun runGeneration(format: ReportFormat, save: Boolean) {
        isBusy = true
        coroutineScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    if (format == ReportFormat.PDF) generatePdf() else generateCsv()
                }
                val fileName = "$fileBaseName.${format.extension}"
                if (save) {
                    val saved = withContext(Dispatchers.IO) {
                        ReportFileSaver.saveToDownloads(context, fileName, format.mimeType, bytes)
                    }
                    Toast.makeText(
                        context,
                        if (saved) "Saved to Downloads" else "Couldn't save the file. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    ReportFileSaver.share(context, fileName, format.mimeType, bytes)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Couldn't generate the report. Please try again.", Toast.LENGTH_SHORT).show()
            } finally {
                isBusy = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pendingAction
        pendingAction = null
        if (granted && action != null) {
            action()
        } else {
            isBusy = false
            Toast.makeText(context, "Storage permission is needed to save on this Android version.", Toast.LENGTH_SHORT).show()
        }
    }

    fun onSelect(format: ReportFormat, save: Boolean) {
        expanded = false
        val needsLegacyPermission = save &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsLegacyPermission) {
            isBusy = true
            pendingAction = { runGeneration(format, save) }
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            runGeneration(format, save)
        }
    }

    Box(modifier = modifier) {
        IconButton(
            enabled = enabled && !isBusy,
            onClick = { expanded = true }
        ) {
            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = tint, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Download, contentDescription = "Download report", tint = tint)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Save as PDF") }, onClick = { onSelect(ReportFormat.PDF, save = true) })
            DropdownMenuItem(text = { Text("Save as CSV (Excel)") }, onClick = { onSelect(ReportFormat.CSV, save = true) })
            DropdownMenuItem(text = { Text("Share as PDF") }, onClick = { onSelect(ReportFormat.PDF, save = false) })
            DropdownMenuItem(text = { Text("Share as CSV (Excel)") }, onClick = { onSelect(ReportFormat.CSV, save = false) })
        }
    }
}
