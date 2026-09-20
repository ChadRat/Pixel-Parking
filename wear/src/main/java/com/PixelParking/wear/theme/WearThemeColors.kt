package com.PixelParking.wear.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class WearAppThemeColors(
    val scallopColor: Color,
    val arrowColor: Color,
    val mapsContainerColor: Color,
    val mapsContentColor: Color,
    val foundContainerColor: Color,
    val foundContentColor: Color
)

object WearColorProvider {
    /**
     * Resolves theme colors matching the main companion app's Material 3 palette.
     * - Scallop shape & arrow use the primary theme colors (same as the phone app's CompassRadarNeedle).
     * - Google Maps button uses a distinct secondary container/accent from the system's dynamic wallpaper palette.
     * - Found Car button uses the vibrant primary/emerald action theme colors.
     */
    @Composable
    fun rememberAppThemeColors(): WearAppThemeColors {
        val context = LocalContext.current
        return remember(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val dynamicScheme = dynamicDarkColorScheme(context)
                    WearAppThemeColors(
                        // Same colors as main app's CompassRadarNeedle (primary & onPrimary)
                        scallopColor = dynamicScheme.primary,
                        arrowColor = dynamicScheme.onPrimary,
                        // Google Maps button uses secondary color from wallpaper palette
                        mapsContainerColor = dynamicScheme.secondaryContainer,
                        mapsContentColor = dynamicScheme.onSecondaryContainer,
                        foundContainerColor = Color(0xFF00E676),
                        foundContentColor = Color(0xFF00391F)
                    )
                } catch (_: Exception) {
                    fallbackColors()
                }
            } else {
                fallbackColors()
            }
        }
    }

    private fun fallbackColors(): WearAppThemeColors {
        return WearAppThemeColors(
            scallopColor = Color(0xFFA8C7FA),        // PixelBluePrimaryDark (matching main app)
            arrowColor = Color(0xFF041E49),          // PixelBlueOnContainerLight (matching main app)
            mapsContainerColor = Color(0xFF005143),  // PixelTealContainerDark (Secondary palette)
            mapsContentColor = Color(0xFF66DBB9),    // PixelTealSecondaryDark (Secondary palette)
            foundContainerColor = Color(0xFF00E676), // Vibrant emerald
            foundContentColor = Color(0xFF00391F)
        )
    }
}
