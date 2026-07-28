package np.com.bimalkafle.firebaseauthdemoapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Downscales the picked image to at most 512px on the long edge and compresses it to
 * JPEG ~80% quality before base64-encoding — keeps the GraphQL request body small and
 * keeps Firebase Storage bandwidth/cost down regardless of the original photo's size.
 */
suspend fun encodeImageForUpload(context: Context, uri: Uri): Pair<String, String>? =
    withContext(Dispatchers.IO) {
        try {
            val original = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            val maxDimension = 512
            val largestSide = maxOf(original.width, original.height)
            val scale = if (largestSide > maxDimension) maxDimension.toFloat() / largestSide else 1f
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                original
            }

            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            base64 to "image/jpeg"
        } catch (e: Exception) {
            null
        }
    }
