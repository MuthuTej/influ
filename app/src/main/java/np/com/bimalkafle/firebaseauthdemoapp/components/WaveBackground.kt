package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.Coral
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.CoralDark

@Composable
fun WaveBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        // Draw the Wave
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight * 0.45f) // Occupy ~45% of screen
        ) {
            val width = size.width
            val height = size.height

            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, height * 0.75f)
                // First curve
                cubicTo(
                    width * 0.25f, height * 0.9f,
                    width * 0.45f, height * 0.55f,
                    width * 0.7f, height * 0.75f
                )
                // Second curve to end
                quadraticBezierTo(
                    width * 0.9f, height * 0.95f,
                    width, height * 0.8f
                )
                lineTo(width, 0f)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(Coral, CoralDark)
                )
            )
        }
        
        // Content
        content()
    }
}
