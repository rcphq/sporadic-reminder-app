package com.sporadic.reminder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sporadic.reminder.ui.settings.ThemeMode

// Catppuccin Latte
private val CatppuccinLatteColors = lightColorScheme(
    primary = Color(0xFF8839EF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECDCFF),
    onPrimaryContainer = Color(0xFF2B0057),
    secondary = Color(0xFF179299),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC8F5F0),
    onSecondaryContainer = Color(0xFF003834),
    tertiary = Color(0xFFFE640B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE0CC),
    onTertiaryContainer = Color(0xFF3D1600),
    error = Color(0xFFD20F39),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFEFF1F5),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFCCD0DA),
    onSurfaceVariant = Color(0xFF6C6F85),
    outline = Color(0xFF8C8FA1),
    outlineVariant = Color(0xFFBCC0CC),
    inverseSurface = Color(0xFF1E1E2E),
    inverseOnSurface = Color(0xFFCDD6F4),
    inversePrimary = Color(0xFFCBA6F7),
)

// Catppuccin Mocha
private val CatppuccinMochaColors = darkColorScheme(
    primary = Color(0xFFCBA6F7),
    onPrimary = Color(0xFF3A0078),
    primaryContainer = Color(0xFF5B1DAB),
    onPrimaryContainer = Color(0xFFECDCFF),
    secondary = Color(0xFF94E2D5),
    onSecondary = Color(0xFF003830),
    secondaryContainer = Color(0xFF005048),
    onSecondaryContainer = Color(0xFFB8F8EC),
    tertiary = Color(0xFFFAB387),
    onTertiary = Color(0xFF4A1E00),
    tertiaryContainer = Color(0xFF6D3A00),
    onTertiaryContainer = Color(0xFFFFDCC6),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF690016),
    errorContainer = Color(0xFF930026),
    onErrorContainer = Color(0xFFFFD9DF),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFA6ADC8),
    outline = Color(0xFF6C7086),
    outlineVariant = Color(0xFF45475A),
    inverseSurface = Color(0xFFEFF1F5),
    inverseOnSurface = Color(0xFF4C4F69),
    inversePrimary = Color(0xFF8839EF),
)

private val SporadicTypography = Typography(
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Medium),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.Medium),
    titleSmall = Typography().titleSmall.copy(fontWeight = FontWeight.Medium),
)

private val SporadicShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun SporadicTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) CatppuccinMochaColors else CatppuccinLatteColors
    val sporadicColors = if (darkTheme) DarkSporadicColors else LightSporadicColors

    CompositionLocalProvider(LocalSporadicColors provides sporadicColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SporadicTypography,
            shapes = SporadicShapes,
            content = content
        )
    }
}
