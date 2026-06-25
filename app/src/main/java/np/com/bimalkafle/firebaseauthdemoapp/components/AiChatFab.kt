package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * The single entry point into the AI chat, mounted on every main tab screen.
 * Uses a distinct blue accent (not the brand coral) so it never reads as the
 * same action as the create-campaign FAB it sometimes stacks above.
 * Takes the plain `NavController` type since that's what every screen's own
 * `navController` parameter is already declared as.
 */
@Composable
fun AiChatFab(
    navController: NavController,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = { navController.navigate("ai_chat") },
        containerColor = Color(0xFF2D6CDF),
        shape = CircleShape,
        modifier = modifier.size(size)
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "Ask AI assistant",
            tint = Color.White
        )
    }
}
