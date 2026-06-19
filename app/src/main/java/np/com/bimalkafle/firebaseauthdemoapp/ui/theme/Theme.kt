package np.com.bimalkafle.firebaseauthdemoapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Extended semantic colors (Success/Warning) that Material3's ColorScheme has no
 * native slot for. Read via LocalAppColors.current inside any @Composable.
 */
val LocalAppColors = staticCompositionLocalOf { LightAppColors }

private val LightColorScheme = lightColorScheme(
    primary = LightAppColors.brandPrimary,
    onPrimary = LightAppColors.onBrandPrimary,
    secondary = LightAppColors.brandPrimary,
    onSecondary = LightAppColors.onBrandPrimary,
    background = LightAppColors.surfaceSubtle,
    onBackground = LightAppColors.textPrimary,
    surface = LightAppColors.surfaceElevated,
    onSurface = LightAppColors.textPrimary,
    surfaceVariant = LightAppColors.surfaceSubtle,
    onSurfaceVariant = LightAppColors.textSecondary,
    error = LightAppColors.error,
    onError = LightAppColors.onError,
    outline = LightAppColors.divider,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAppColors.brandPrimary,
    onPrimary = DarkAppColors.onBrandPrimary,
    secondary = DarkAppColors.brandPrimary,
    onSecondary = DarkAppColors.onBrandPrimary,
    background = DarkAppColors.surfaceSubtle,
    onBackground = DarkAppColors.textPrimary,
    surface = DarkAppColors.surfaceElevated,
    onSurface = DarkAppColors.textPrimary,
    surfaceVariant = DarkAppColors.surfaceSubtle,
    onSurfaceVariant = DarkAppColors.textSecondary,
    error = DarkAppColors.error,
    onError = DarkAppColors.onError,
    outline = DarkAppColors.divider,
)

/**
 * darkTheme defaults to false rather than following the system setting: most
 * screens still hardcode light-only colors (Color.White/Color.Black literals)
 * and would render unreadable if dark mode turned on before that screen-by-screen
 * migration is complete. Both Light and Dark token values already exist above —
 * flip this default once the migration sweep is done and spot-checked (see the
 * implementation plan's dark mode release gate).
 */
@Composable
fun FirebaseAuthDemoAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
