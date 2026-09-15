package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

// Google Pixel Material 3 Expressive Light Color Scheme
private val PixelLightColorScheme = lightColorScheme(
    primary = PixelBluePrimary,
    onPrimary = PixelLightSurface,
    primaryContainer = PixelBlueContainerLight,
    onPrimaryContainer = PixelBlueOnContainerLight,
    secondary = PixelTealSecondary,
    onSecondary = PixelLightSurface,
    secondaryContainer = PixelTealContainerLight,
    onSecondaryContainer = PixelTealSecondary,
    tertiary = PixelCoralTertiary,
    onTertiary = PixelLightSurface,
    tertiaryContainer = PixelCoralContainerLight,
    onTertiaryContainer = PixelCoralTertiary,
    background = PixelLightBackground,
    onBackground = PixelTextPrimaryLight,
    surface = PixelLightSurface,
    onSurface = PixelTextPrimaryLight,
    surfaceVariant = PixelLightSurfaceContainer,
    onSurfaceVariant = PixelTextSecondaryLight,
    surfaceContainer = PixelLightSurfaceContainer,
    surfaceContainerHigh = PixelLightSurfaceContainerHigh,
    outline = PixelLightOutline,
    outlineVariant = PixelLightOutlineVariant,
    error = PixelError,
    onError = PixelLightSurface
)

// Google Pixel Material 3 Expressive Dark Color Scheme
private val PixelDarkColorScheme = darkColorScheme(
    primary = PixelBluePrimaryDark,
    onPrimary = PixelBlueOnContainerLight,
    primaryContainer = PixelBlueContainerDark,
    onPrimaryContainer = PixelBlueOnContainerDark,
    secondary = PixelTealSecondaryDark,
    onSecondary = PixelDarkBackground,
    secondaryContainer = PixelTealContainerDark,
    onSecondaryContainer = PixelTealSecondaryDark,
    tertiary = PixelCoralTertiaryDark,
    onTertiary = PixelDarkBackground,
    tertiaryContainer = PixelCoralContainerDark,
    onTertiaryContainer = PixelCoralTertiaryDark,
    background = PixelDarkBackground,
    onBackground = PixelTextPrimaryDark,
    surface = PixelDarkSurface,
    onSurface = PixelTextPrimaryDark,
    surfaceVariant = PixelDarkSurfaceVariant,
    onSurfaceVariant = PixelTextSecondaryDark,
    surfaceContainer = PixelDarkSurfaceContainer,
    surfaceContainerHigh = PixelDarkSurfaceContainerHigh,
    outline = PixelDarkOutline,
    outlineVariant = PixelDarkOutlineVariant,
    error = PixelError,
    onError = PixelDarkBackground
)

/**
 * Creates an OLED Pitch Black variant from any base ColorScheme (including dynamic wallpaper color scheme),
 * keeping all dynamic accent/brand colors intact and purely setting the background and main surfaces to true #000000.
 */
fun ColorScheme.toOledColorScheme(): ColorScheme {
    return this.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        surfaceDim = Color(0xFF000000),
        surfaceContainerLowest = Color(0xFF000000),
        surfaceContainerLow = Color(0xFF080808),
        surfaceContainer = Color(0xFF101010),
        surfaceContainerHigh = Color(0xFF161616),
        surfaceContainerHighest = Color(0xFF202020)
    )
}

// Material 3 Expressive Shape Tokens (organic pills, squircles & expressive rounded corners)
val PixelExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

// Expressive Shape Constants for components
val PixelPillShape = RoundedCornerShape(percent = 50)
val PixelSquircleCardShape = RoundedCornerShape(24.dp)
val PixelHeroCardShape = RoundedCornerShape(32.dp)
val PixelBadgeShape = RoundedCornerShape(16.dp)
val PixelChipShape = RoundedCornerShape(18.dp)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    oledMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    // Always follow wallpaper color palette when dynamicColor is enabled (Android 12+ / API 31+)
    val baseScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) PixelDarkColorScheme else PixelLightColorScheme
    }

    // OLED mode does not change the app's accent colors, it just sets the background & surface to pure pitch black #000000
    // OLED theme is strictly not available in light mode (only active when dark mode is enabled)
    val colorScheme = if (oledMode && isDark) {
        baseScheme.toOledColorScheme()
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PixelExpressiveShapes,
        content = content
    )
}
