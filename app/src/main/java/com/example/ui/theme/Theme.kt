package com.example.ui.theme

import android.app.Activity
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

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

/**
 * Warms the light color scheme and infuses the wallpaper's dynamic palette into the background and surfaces.
 * Prevents sterile, harsh #FFFFFF backgrounds while creating a softer, warmer feel that adapts to wallpaper colors.
 */
fun ColorScheme.toWarmLightColorScheme(): ColorScheme {
    // Warm, welcoming foundation (soft organic cream/sand tone instead of harsh stark white)
    val warmBase = Color(0xFFF9F6F0)
    val warmSurface = Color(0xFFFFFDF9)
    val warmContainer = Color(0xFFF3EFE6)
    val warmContainerHigh = Color(0xFFECE7DC)
    val warmContainerHighest = Color(0xFFE5DFD2)

    // Wallpaper dynamic accent tint (extracted from the Material wallpaper dynamic theme)
    val wallpaperAccent = this.primary

    // Infuse a gentle tint of the wallpaper color into the warm base
    val dynamicWarmBackground = warmBase.blendWith(wallpaperAccent, 0.05f)
    val dynamicWarmSurface = warmSurface.blendWith(wallpaperAccent, 0.025f)
    val dynamicWarmContainer = warmContainer.blendWith(wallpaperAccent, 0.05f)
    val dynamicWarmContainerHigh = warmContainerHigh.blendWith(wallpaperAccent, 0.06f)
    val dynamicWarmContainerHighest = warmContainerHighest.blendWith(wallpaperAccent, 0.07f)

    return this.copy(
        background = dynamicWarmBackground,
        surface = dynamicWarmSurface,
        surfaceDim = dynamicWarmContainerHigh,
        surfaceBright = dynamicWarmSurface,
        surfaceContainerLowest = dynamicWarmSurface,
        surfaceContainerLow = dynamicWarmContainer,
        surfaceContainer = dynamicWarmContainer,
        surfaceContainerHigh = dynamicWarmContainerHigh,
        surfaceContainerHighest = dynamicWarmContainerHighest
    )
}

private fun Color.blendWith(other: Color, ratio: Float): Color {
    val r = this.red * (1f - ratio) + other.red * ratio
    val g = this.green * (1f - ratio) + other.green * ratio
    val b = this.blue * (1f - ratio) + other.blue * ratio
    return Color(red = r, green = g, blue = b, alpha = this.alpha)
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
    autoSunTheme: Boolean = false,
    isDaytime: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> if (autoSunTheme) !isDaytime else systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    // Always follow wallpaper color palette when dynamicColor is enabled (Android 12+ / API 31+)
    val baseScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isDark) PixelDarkColorScheme else PixelLightColorScheme
    }

    // OLED mode sets background & surface to pure pitch black #000000 (only active when dark mode is enabled)
    // Light mode applies a warmer background infused with the dynamic Material wallpaper palette
    val targetColorScheme = when {
        oledMode && isDark -> baseScheme.toOledColorScheme()
        !isDark -> baseScheme.toWarmLightColorScheme()
        else -> baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(isDark) {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
            onDispose { }
        }
    }

    MaterialTheme(
        colorScheme = targetColorScheme,
        typography = Typography,
        shapes = PixelExpressiveShapes,
        content = content
    )
}
