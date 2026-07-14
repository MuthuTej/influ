package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val StarFilledColor = Color(0xFFFFC107)

/**
 * Read-only star row for a rating that's already been given (as opposed to
 * [RatingPromptDialog], which collects a new one). Fractional ratings are
 * rounded to the nearest whole star since Material has no built-in half-star
 * glyph.
 */
@Composable
fun StarRatingDisplay(
    rating: Double,
    modifier: Modifier = Modifier,
    starSize: Dp = 16.dp
) {
    val filledStars = rating.roundToInt().coerceIn(0, 5)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < filledStars) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (index < filledStars) StarFilledColor else Color.LightGray,
                modifier = Modifier.height(starSize)
            )
        }
    }
}

/** One review entry: reviewer name, star row, comment, and date. */
@Composable
fun ReviewCard(
    reviewerName: String,
    rating: Double,
    comment: String?,
    createdAt: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = reviewerName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            StarRatingDisplay(rating = rating)
            if (!comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = comment, fontSize = 12.sp, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = formatReviewDate(createdAt), fontSize = 10.sp, color = Color.Gray)
        }
    }
}

private fun formatReviewDate(iso: String): String {
    return runCatching { iso.substringBefore("T") }.getOrDefault(iso)
}
