package np.com.bimalkafle.firebaseauthdemoapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radius scale, replacing the 11+ distinct radius values (8/10/12/14/16/20/
 * 24/25/26/28/30/32dp) found scattered across screens. Reference via
 * MaterialTheme.shapes.<role> rather than a literal RoundedCornerShape(Ndp).
 */
object Radius {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val extraLarge = 24.dp
    val pill = 999.dp
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(Radius.small),
    medium = RoundedCornerShape(Radius.medium),
    large = RoundedCornerShape(Radius.large),
    extraLarge = RoundedCornerShape(Radius.extraLarge)
)
