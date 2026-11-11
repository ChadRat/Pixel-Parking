package com.example.ui.components

import android.os.Build
import android.view.ViewParent
import android.view.WindowManager
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.example.BuildConfig
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ProvideAdaptiveDimensions
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern Material 3 Expressive "What's new" bottom sheet.
 *
 * Characteristics:
 * - Scrim and window-level background blur covering the uncovered screen behind it.
 * - Stationary hero card at the top (seal badge, title, version pill) that remains
 *   fixed in place while only the bullet point cards scroll underneath it.
 * - Unique dedicated icon for every changelog bullet point representing what it is.
 * - Pinned full-width rounded "Got it" confirmation button at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    message: String,
    onDismiss: () -> Unit,
    isFunIcons: Boolean = true,
    modifier: Modifier = Modifier
) {
    ProvideAdaptiveDimensions {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val strings = LocalAppStrings.current
        val configuration = LocalConfiguration.current
        val maxSheetHeight = (configuration.screenHeightDp.dp * 0.90f).coerceAtLeast(340.dp)

        val scheme = MaterialTheme.colorScheme
        val isDark = scheme.background.luminance() < 0.5f

        val sheetContainerColor = if (isDark) Color(0xFF111111) else scheme.surfaceContainerLow
        val heroCardBg = if (isDark) {
            lerp(Color(0xFF1E1B16), scheme.primary, 0.36f)
        } else {
            lerp(scheme.surfaceContainerHigh, scheme.primaryContainer, 0.65f)
        }
        val heroAccentColor = if (isDark) {
            lerp(scheme.primary, Color(0xFFF3C382), 0.25f)
        } else {
            scheme.primary
        }
        val heroTitleColor = if (isDark) {
            lerp(heroAccentColor, Color.White, 0.35f)
        } else {
            scheme.onPrimaryContainer
        }
        val heroPillTextColor = if (heroAccentColor.luminance() > 0.4f) {
            Color(0xFF2B1B06)
        } else {
            Color.White
        }
        val itemCardBg = if (isDark) Color(0xFF1D1E21) else scheme.surfaceContainerHigh
        val dragHandleColor = if (isDark) {
            Color.White.copy(alpha = 0.45f)
        } else {
            scheme.onSurfaceVariant.copy(alpha = 0.4f)
        }

        val items = remember(message) {
            val parsed = message
                .lines()
                .map { line ->
                    line.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim()
                }
                .filter { it.isNotBlank() }
            if (parsed.isEmpty()) listOf(message.trim()) else parsed
        }

        val versionLabel = remember(strings.appName) {
            "${strings.appName} ${BuildConfig.VERSION_NAME}"
        }

        val view = LocalView.current
        SideEffect {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    var current: ViewParent? = view.parent
                    while (current != null) {
                        if (current is DialogWindowProvider) {
                            val win = current.window
                            win.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                            win.attributes = win.attributes.apply {
                                blurBehindRadius = 45
                            }
                            break
                        }
                        current = current.parent
                    }
                } catch (_: Throwable) {}
            }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = sheetContainerColor,
            scrimColor = Color.Black.copy(alpha = 0.72f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = dragHandleColor,
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                    ) {}
                }
            },
            modifier = modifier.testTag("whats_new_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = maxSheetHeight)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp)
            ) {
                // 1. Stationary Hero Header Card (remains static while bullet points scroll underneath)
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = heroCardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_whats_new_hero")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        WhatsNewSealBadge(
                            badgeColor = heroAccentColor,
                            cutoutColor = heroCardBg,
                            badgeSize = 64.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = strings.whatsNewTitle,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = heroTitleColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = heroAccentColor,
                            modifier = Modifier.testTag("pill_whats_new_version")
                        ) {
                            Text(
                                text = versionLabel,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = heroPillTextColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Scrollable Bullet Points Container (THESE MOVE UNDER THE HERO CARD)
                val bulletPointsScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(bulletPointsScrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEachIndexed { index, itemText ->
                        val itemIcon = getWhatsNewIconForItem(itemText, index)
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = itemCardBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (index == 0) Modifier.testTag("card_whats_new_message")
                                    else Modifier
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iconShape = getSettingsIconContainerShapeForIndex(
                                    enabled = isFunIcons,
                                    index = index,
                                    defaultCornerDp = 12
                                )
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(iconShape)
                                        .background(heroAccentColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = itemIcon,
                                        contentDescription = null,
                                        tint = heroAccentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = itemText,
                                    fontSize = 15.5.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(
                                            if (index == 0) Modifier.testTag("text_whats_new_message")
                                            else Modifier
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Pinned Bottom "Got it" Confirmation Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = heroAccentColor,
                        contentColor = heroPillTextColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("button_whats_new_dismiss")
                ) {
                    Text(
                        text = strings.whatsNewGotIt,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Resolves a unique icon for each bullet point based on what feature it represents.
 */
private fun getWhatsNewIconForItem(itemText: String, index: Int): ImageVector {
    val lower = itemText.lowercase()
    return when {
        // Map / Location in History tab
        lower.contains("map") || lower.contains("χάρτη") || lower.contains("location") || lower.contains("τοποθεσ") || lower.contains("history") || lower.contains("ιστορικ") -> {
            Icons.Default.Map
        }
        // Walk Away / Walking / Driving
        lower.contains("walk") || lower.contains("απομάκρυν") || lower.contains("περπάτ") || lower.contains("οδήγ") || lower.contains("drive") || lower.contains("car") || lower.contains("αυτοκίν") -> {
            Icons.AutoMirrored.Filled.DirectionsWalk
        }
        // Haptics & Animations / Vibration
        lower.contains("haptic") || lower.contains("animation") || lower.contains("απτικ") || lower.contains("κίνηση") || lower.contains("σχέδια") || lower.contains("δόνηση") || lower.contains("vibrat") -> {
            Icons.Default.Vibration
        }
        // Bug fixes / Tweaks
        lower.contains("bug") || lower.contains("σφάλμα") || lower.contains("διορθώ") || lower.contains("fix") || lower.contains("patch") || lower.contains("issue") -> {
            Icons.Default.BugReport
        }
        // Wear OS
        lower.contains("wear") || lower.contains("watch") || lower.contains("ρολόι") -> {
            Icons.Default.Watch
        }
        // Bluetooth
        lower.contains("bluetooth") || lower.contains("bt") -> {
            Icons.Default.Bluetooth
        }
        // Capybara
        lower.contains("capy") || lower.contains("fun") -> {
            Icons.Default.Pets
        }
        // License
        lower.contains("license") || lower.contains("άδεια") -> {
            Icons.Default.Gavel
        }
        // Fallback by index ensuring every item has a unique icon
        else -> when (index % 6) {
            0 -> Icons.Default.Map
            1 -> Icons.AutoMirrored.Filled.DirectionsWalk
            2 -> Icons.Default.Vibration
            3 -> Icons.Default.BugReport
            4 -> Icons.Default.AutoAwesome
            else -> Icons.Default.Stars
        }
    }
}

/**
 * 12-pointed wavy scalloped starburst/seal badge with a 3-sparkle cluster cut out in the center.
 * Slowly rotates clockwise for half a turn (180 degrees) and smoothly transitions to spin
 * anticlockwise for half a turn (and vice versa).
 */
@Composable
private fun WhatsNewSealBadge(
    badgeColor: Color,
    cutoutColor: Color,
    badgeSize: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "WhatsNewSealRotation"
    )
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5500,
                easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WhatsNewSealHalfTurnRotation"
    )

    Box(
        modifier = modifier.size(badgeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = size.minDimension * 0.44f

            // 12-pointed smooth wavy seal path
            val sealPath = Path()
            val points = 12
            val steps = 360
            val twoPi = (2.0 * Math.PI).toFloat()
            for (i in 0 until steps) {
                val theta = i * (twoPi / steps) - (Math.PI / 2.0).toFloat()
                val wave = 1f + 0.115f * cos(points * theta) - 0.018f * cos(2 * points * theta)
                val r = baseRadius * wave
                val x = cx + r * cos(theta)
                val y = cy + r * sin(theta)
                if (i == 0) sealPath.moveTo(x, y) else sealPath.lineTo(x, y)
            }
            sealPath.close()

            rotate(
                degrees = rotationDegrees,
                pivot = Offset(cx, cy)
            ) {
                drawPath(
                    path = sealPath,
                    color = badgeColor,
                    style = Fill
                )
            }

            // Draw the 3 four-pointed concave sparkles in the center using cutoutColor
            drawThreeSparklesCluster(
                cx = cx,
                cy = cy,
                clusterRadius = size.minDimension * 0.22f,
                color = cutoutColor
            )
        }
    }
}

/**
 * Draws a cluster of 3 four-pointed concave sparkles:
 * - 1 main sparkle on the middle-left
 * - 1 smaller sparkle on the top-right
 * - 1 smaller sparkle on the bottom-right
 */
private fun DrawScope.drawThreeSparklesCluster(
    cx: Float,
    cy: Float,
    clusterRadius: Float,
    color: Color
) {
    // Main left 4-point star
    val mainStarPath = buildFourPointSparklePath(
        cx = cx - clusterRadius * 0.24f,
        cy = cy + clusterRadius * 0.04f,
        outerRadius = clusterRadius * 0.64f,
        innerRadius = clusterRadius * 0.17f
    )
    drawPath(path = mainStarPath, color = color, style = Fill)

    // Top-right small 4-point star
    val topRightStarPath = buildFourPointSparklePath(
        cx = cx + clusterRadius * 0.46f,
        cy = cy - clusterRadius * 0.44f,
        outerRadius = clusterRadius * 0.34f,
        innerRadius = clusterRadius * 0.095f
    )
    drawPath(path = topRightStarPath, color = color, style = Fill)

    // Bottom-right small 4-point star
    val bottomRightStarPath = buildFourPointSparklePath(
        cx = cx + clusterRadius * 0.42f,
        cy = cy + clusterRadius * 0.48f,
        outerRadius = clusterRadius * 0.32f,
        innerRadius = clusterRadius * 0.09f
    )
    drawPath(path = bottomRightStarPath, color = color, style = Fill)
}

private fun buildFourPointSparklePath(
    cx: Float,
    cy: Float,
    outerRadius: Float,
    innerRadius: Float
): Path {
    val path = Path()
    val d = innerRadius * 0.72f
    // Top tip
    path.moveTo(cx, cy - outerRadius)
    // Top-right concave curve to Right tip
    path.quadraticTo(cx + d, cy - d, cx + outerRadius, cy)
    // Bottom-right concave curve to Bottom tip
    path.quadraticTo(cx + d, cy + d, cx, cy + outerRadius)
    // Bottom-left concave curve to Left tip
    path.quadraticTo(cx - d, cy + d, cx - outerRadius, cy)
    // Top-left concave curve back to Top tip
    path.quadraticTo(cx - d, cy - d, cx, cy - outerRadius)
    path.close()
    return path
}
