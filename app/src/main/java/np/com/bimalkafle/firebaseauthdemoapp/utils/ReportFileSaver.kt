package np.com.bimalkafle.firebaseauthdemoapp.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Writes a generated report's bytes either straight into the device's public
 * Downloads folder (silent "Save"), or into the app's cache dir for handing
 * off through the system share sheet ("Share"). Save needs two code paths:
 * `MediaStore.Downloads` on API 29+ needs no permission, but this app's
 * minSdk (26) predates that API, so 26-28 falls back to writing the legacy
 * public directory directly, which does need WRITE_EXTERNAL_STORAGE.
 */
object ReportFileSaver {

    /** True if a save was written successfully. */
    fun saveToDownloads(context: Context, fileName: String, mimeType: String, bytes: ByteArray): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, fileName, mimeType, bytes)
        } else {
            saveLegacy(context, fileName, bytes)
        }
    }

    private fun saveViaMediaStore(context: Context, fileName: String, mimeType: String, bytes: ByteArray): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        return resolver.openOutputStream(uri)?.use { it.write(bytes) } != null
    }

    private fun saveLegacy(context: Context, fileName: String, bytes: ByteArray): Boolean {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) return false
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(), null)
        return true
    }

    /** Writes into the app's cache dir and launches the system share sheet. */
    fun share(context: Context, fileName: String, mimeType: String, bytes: ByteArray) {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, fileName)
        FileOutputStream(file).use { it.write(bytes) }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share report"))
    }
}
