package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * System font family following the user's Android device system font.
 */
val SimpleAppFontFamily: FontFamily = FontFamily.Default

// Backward-compatible alias for any callers expecting GoogleSansFontFamily
val GoogleSansFontFamily: FontFamily = FontFamily.Default

/**
 * CompositionLocal providing the current developer-selected font weight.
 */
val LocalAppTextWeight = compositionLocalOf { FontWeight.Normal }

/**
 * Creates Material 3 Typography following the user's system font,
 * with text weight dynamically scaled based on [baseWeight] (100..900).
 */
fun createAppTypography(baseWeight: Int = 400): Typography {
    val clamped = baseWeight.coerceIn(100, 900)
    val body = FontWeight(clamped)
    val label = FontWeight((clamped + 100).coerceAtMost(900))
    val title = FontWeight((clamped + 150).coerceAtMost(900))
    val headline = FontWeight((clamped + 200).coerceAtMost(900))
    val display = FontWeight((clamped + 250).coerceAtMost(900))

    return Typography(
        displayLarge = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = display,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = display,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = display,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = headline,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = headline,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = headline,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = title,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = title,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = label,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = body,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = body,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = body,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = label,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = label,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = SimpleAppFontFamily,
            fontWeight = label,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

/**
 * Default Material 3 Typography matching the user's system font.
 */
val Typography = createAppTypography(400)
