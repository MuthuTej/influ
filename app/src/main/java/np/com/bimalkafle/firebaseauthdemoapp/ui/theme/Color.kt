package np.com.bimalkafle.firebaseauthdemoapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Semantic color roles for the app, defined for both Light and Dark so the values
 * already exist when dark mode is enabled later (see Theme.kt) — no new design
 * work needed at that point, just wiring + a per-screen verification pass.
 *
 * Values are derived from the colors already in real use across the app (e.g.
 * 0xFFFF8383 is the brand accent used ~140 times inline) rather than the
 * previously-unused Material default palette, so adopting these tokens in P3
 * should not visibly change the app's current look.
 */
data class AppColorRoles(
    val brandPrimary: Color,
    val onBrandPrimary: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val divider: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val overlay: Color,
)

val LightAppColors = AppColorRoles(
    brandPrimary = Color(0xFFFF8383),
    onBrandPrimary = Color.White,
    surfaceElevated = Color.White,
    surfaceSubtle = Color(0xFFF5F5F5),
    textPrimary = Color(0xFF1D1D1F),
    textSecondary = Color(0xFF6C757D),
    textDisabled = Color(0xFFB0B0C0),
    divider = Color(0xFFEEEEEE),
    success = Color(0xFF4CAF50),
    onSuccess = Color.White,
    warning = Color(0xFFFFC107),
    onWarning = Color(0xFF1D1D1F),
    error = Color(0xFFFF5252),
    onError = Color.White,
    overlay = Color.Black.copy(alpha = 0.5f),
)

val DarkAppColors = AppColorRoles(
    brandPrimary = Color(0xFFFF9E90),
    onBrandPrimary = Color(0xFF1D1D1F),
    surfaceElevated = Color(0xFF1E1E1E),
    surfaceSubtle = Color(0xFF121212),
    textPrimary = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFB0B0B0),
    textDisabled = Color(0xFF6C6C6C),
    divider = Color(0xFF2C2C2C),
    success = Color(0xFF66BB6A),
    onSuccess = Color(0xFF1D1D1F),
    warning = Color(0xFFFFCA28),
    onWarning = Color(0xFF1D1D1F),
    error = Color(0xFFEF5350),
    onError = Color(0xFF1D1D1F),
    overlay = Color.Black.copy(alpha = 0.6f),
)

// Kept for WaveBackground.kt's gradient; aligned to the brand color actually in use.
val Coral = LightAppColors.brandPrimary
val CoralDark = Color(0xFFFF6F61)
