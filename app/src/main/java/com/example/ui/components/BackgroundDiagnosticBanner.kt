package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.i18n.LocalAppStrings
import kotlin.math.cos
import kotlin.math.sin

/**
 * Redesigned Material 3 Expressive Background Auto-Save Setup card.
 *
 * Matches the "What's new" window and "Fun settings icons" aesthetic:
 * - Top Hero Card with a 12-pointed scalloped seal badge (with 3-sparkle cutout),
 *   bold centered "Background Auto-Save Setup" title, status pill badge, and description.
 * - Individual / segmented rounded step cards below the Hero Card, each featuring a
 *   distinctive non-repeating Fun Settings Icon shape (WAVY_FLOWER_6, CLOVER_4, ROUNDED_PENTAGON)
 *   and alternating warm brown / pastel olive palettes so two adjacent steps never share
 *   the same shape.
 */
@Composable
fun BackgroundDiagnosticBanner(
    modifier: Modifier = Modifier,
    showWhenAllGranted: Boolean = false,
    dismissible: Boolean = true,
    onOpenSetupSheet: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isDismissed by rememberSaveable { mutableStateOf(false) }
    var isExpandedWhenAllGranted by rememberSaveable { mutableStateOf(false) }

    // Diagnostic states
    var isBatteryOptimizationIgnored by remember { mutableStateOf(checkBatteryExemption(context)) }
    var hasBackgroundLocation by remember { mutableStateOf(checkBackgroundLocation(context)) }
    var hasFineLocation by remember { mutableStateOf(checkFineLocation(context)) }
    var hasBluetoothConnect by remember { mutableStateOf(checkBluetoothConnect(context)) }

    // Re-check automatically whenever the user returns to the app from System Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimizationIgnored = checkBatteryExemption(context)
                hasBackgroundLocation = checkBackgroundLocation(context)
                hasFineLocation = checkFineLocation(context)
                hasBluetoothConnect = checkBluetoothConnect(context)
                if (com.example.service.AutoParkLocationService.isMonitoringEnabled(context)) {
                    com.example.service.AutoParkLocationService.startMonitoring(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasFineLocation = granted
        hasBackgroundLocation = checkBackgroundLocation(context)
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocation = granted
    }

    val bluetoothConnectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBluetoothConnect = granted
    }

    val isBluetoothNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isBluetoothGranted = !isBluetoothNeeded || hasBluetoothConnect
    val isAllGranted = isBatteryOptimizationIgnored && hasBackgroundLocation && isBluetoothGranted

    val pendingBattery = !isBatteryOptimizationIgnored
    val pendingLocation = !hasBackgroundLocation
    val pendingBluetooth = isBluetoothNeeded && !hasBluetoothConnect

    val totalSteps = if (isBluetoothNeeded) 3 else 2
    val pendingCount = (if (pendingBattery) 1 else 0) +
            (if (pendingLocation) 1 else 0) +
            (if (pendingBluetooth) 1 else 0)
    val completedCount = (totalSteps - pendingCount).coerceAtLeast(0)

    val shouldShow = !isDismissed && (!isAllGranted || showWhenAllGranted)

    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

    // Shared What's New + Fun Settings Icons palette
    val outerCardBg = if (isDark) Color(0xFF131416) else scheme.surfaceContainerLow
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
    val itemCardBg = if (isDark) Color(0xFF1C1D20) else scheme.surfaceContainerHigh

    // Alternating icon container palettes matching the Fun Settings Icons screenshots
    val warmIconBoxBg = if (isDark) {
        lerp(Color(0xFF262119), heroAccentColor, 0.25f)
    } else {
        lerp(scheme.surfaceContainerHighest, scheme.primaryContainer, 0.7f)
    }
    val warmIconTint = if (isDark) {
        lerp(heroAccentColor, Color.White, 0.22f)
    } else {
        scheme.primary
    }
    val oliveIconBoxBg = if (isDark) Color(0xFFE6EE9C) else Color(0xFFDEE78E)
    val oliveIconTint = Color(0xFF3E4415)

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn(tween(300)) + expandVertically(tween(350)),
        exit = fadeOut(tween(250)) + shrinkVertically(tween(300))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                .testTag("background_diagnostic_banner"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = outerCardBg
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 1. Hero Header Card (matches the What's New & About windows)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = heroCardBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp)
                    ) {
                        // Top-right Dismiss or Expand/Collapse button
                        Row(
                            modifier = Modifier.align(Alignment.TopEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isAllGranted && showWhenAllGranted) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.22f))
                                        .clickable { isExpandedWhenAllGranted = !isExpandedWhenAllGranted }
                                        .testTag("toggle_expand_diagnostic_banner")
                                ) {
                                    Icon(
                                        imageVector = if (isExpandedWhenAllGranted) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isExpandedWhenAllGranted) "Collapse" else "Expand",
                                        tint = heroTitleColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else if (dismissible) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.22f))
                                        .clickable { isDismissed = true }
                                        .testTag("dismiss_diagnostic_banner")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = strings.cancel,
                                        tint = heroTitleColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AutoSaveSealBadge(
                                badgeColor = heroAccentColor,
                                cutoutColor = heroCardBg,
                                badgeSize = 62.dp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = strings.backgroundDiagnosticTitle,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = heroTitleColor,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Status Pill matching the What's New version pill
                            Surface(
                                shape = RoundedCornerShape(percent = 50),
                                color = heroAccentColor
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                ) {
                                    if (isAllGranted) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = heroPillTextColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isAllGranted) {
                                            strings.backgroundSetupComplete
                                        } else {
                                            "$completedCount / $totalSteps ${strings.backgroundSetupStatusReady}"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = heroPillTextColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isAllGranted) strings.backgroundSetupActiveDesc else strings.backgroundDiagnosticDesc,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = heroTitleColor.copy(alpha = 0.88f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 2. Setup Steps List (using distinct Fun Settings Icon shapes per row)
                val showSteps = !isAllGranted || isExpandedWhenAllGranted

                AnimatedVisibility(
                    visible = showSteps,
                    enter = expandVertically(tween(300)) + fadeIn(tween(250)),
                    exit = shrinkVertically(tween(250)) + fadeOut(tween(200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Step 1: Unrestricted Battery (Shape: WAVY_FLOWER_6)
                        if (pendingBattery) {
                            SetupStepRow(
                                title = strings.backgroundSetupStepBatteryTitle,
                                description = strings.backgroundSetupStepBatteryDesc,
                                icon = Icons.Default.BatteryChargingFull,
                                iconShape = getSettingsIconContainerShape(
                                    enabled = true,
                                    shapeType = FunSettingsIconShapeType.WAVY_FLOWER_6
                                ),
                                iconBoxBg = warmIconBoxBg,
                                iconTint = warmIconTint,
                                cardBg = itemCardBg,
                                actionBtnBg = heroAccentColor,
                                actionBtnContent = heroPillTextColor,
                                isGranted = false,
                                actionLabel = strings.backgroundSetupStatusAction,
                                readyLabel = strings.backgroundSetupStatusReady,
                                onClickAction = {
                                    requestBatteryExemption(context)
                                },
                                onGrantedTap = {},
                                testTag = "setup_step_battery"
                            )
                        }

                        // Step 2: Background Location (Shape: CLOVER_4)
                        if (pendingLocation) {
                            SetupStepRow(
                                title = strings.backgroundSetupStepLocationTitle,
                                description = strings.backgroundSetupStepLocationDesc,
                                icon = Icons.Default.LocationOn,
                                iconShape = getSettingsIconContainerShape(
                                    enabled = true,
                                    shapeType = FunSettingsIconShapeType.CLOVER_4
                                ),
                                iconBoxBg = oliveIconBoxBg,
                                iconTint = oliveIconTint,
                                cardBg = itemCardBg,
                                actionBtnBg = heroAccentColor,
                                actionBtnContent = heroPillTextColor,
                                isGranted = false,
                                actionLabel = strings.backgroundSetupStatusAction,
                                readyLabel = strings.backgroundSetupStatusReady,
                                onClickAction = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        if (!hasFineLocation) {
                                            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            try {
                                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                            } catch (_: Throwable) {
                                                openAppSettings(context)
                                            }
                                        } else {
                                            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                        }
                                    } else {
                                        fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                onGrantedTap = {},
                                testTag = "setup_step_location"
                            )
                        }

                        // Step 3: Nearby Devices / Bluetooth (Shape: ROUNDED_PENTAGON)
                        if (pendingBluetooth) {
                            SetupStepRow(
                                title = strings.backgroundSetupStepBluetoothTitle,
                                description = strings.backgroundSetupStepBluetoothDesc,
                                icon = Icons.Default.Bluetooth,
                                iconShape = getSettingsIconContainerShape(
                                    enabled = true,
                                    shapeType = FunSettingsIconShapeType.ROUNDED_PENTAGON
                                ),
                                iconBoxBg = warmIconBoxBg,
                                iconTint = warmIconTint,
                                cardBg = itemCardBg,
                                actionBtnBg = heroAccentColor,
                                actionBtnContent = heroPillTextColor,
                                isGranted = false,
                                actionLabel = strings.backgroundSetupStatusAction,
                                readyLabel = strings.backgroundSetupStatusReady,
                                onClickAction = {
                                    bluetoothConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                },
                                onGrantedTap = {},
                                testTag = "setup_step_bluetooth"
                            )
                        }

                        // Fallback when explicitly viewed with showWhenAllGranted = true
                        if (isAllGranted) {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = itemCardBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_all_configured")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        shape = getSettingsIconContainerShape(
                                            enabled = true,
                                            shapeType = FunSettingsIconShapeType.STARBURST_12
                                        ),
                                        color = oliveIconBoxBg,
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = oliveIconTint,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = strings.backgroundSetupActiveDesc,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconShape: Shape,
    iconBoxBg: Color,
    iconTint: Color,
    cardBg: Color,
    actionBtnBg: Color,
    actionBtnContent: Color,
    isGranted: Boolean,
    actionLabel: String,
    readyLabel: String,
    onClickAction: () -> Unit,
    onGrantedTap: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .testTag(testTag)
            .clickable(onClick = if (isGranted) onGrantedTap else onClickAction)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Expressive Fun Settings Icon shape container
                Surface(
                    shape = iconShape,
                    color = iconBoxBg,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Action Button or Ready Badge
            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = actionBtnBg.copy(alpha = 0.16f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = actionBtnBg,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = readyLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionBtnBg
                        )
                    }
                }
            } else {
                Button(
                    onClick = onClickAction,
                    shape = RoundedCornerShape(percent = 50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionBtnBg,
                        contentColor = actionBtnContent
                    ),
                    contentPadding = PaddingValues(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("${testTag}_action_btn")
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 12-pointed wavy scalloped seal badge with a 3-sparkle cutout in the center,
 * matching the What's New and About Hero Cards.
 */
@Composable
private fun AutoSaveSealBadge(
    badgeColor: Color,
    cutoutColor: Color,
    badgeSize: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(badgeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = size.minDimension * 0.44f

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

            drawPath(
                path = sealPath,
                color = badgeColor,
                style = Fill
            )

            drawAutoSaveThreeSparklesCluster(
                cx = cx,
                cy = cy,
                clusterRadius = size.minDimension * 0.22f,
                color = cutoutColor
            )
        }
    }
}

private fun DrawScope.drawAutoSaveThreeSparklesCluster(
    cx: Float,
    cy: Float,
    clusterRadius: Float,
    color: Color
) {
    val mainStarPath = buildAutoSaveFourPointSparklePath(
        cx = cx - clusterRadius * 0.24f,
        cy = cy + clusterRadius * 0.04f,
        outerRadius = clusterRadius * 0.64f,
        innerRadius = clusterRadius * 0.17f
    )
    drawPath(path = mainStarPath, color = color, style = Fill)

    val topRightStarPath = buildAutoSaveFourPointSparklePath(
        cx = cx + clusterRadius * 0.46f,
        cy = cy - clusterRadius * 0.44f,
        outerRadius = clusterRadius * 0.34f,
        innerRadius = clusterRadius * 0.095f
    )
    drawPath(path = topRightStarPath, color = color, style = Fill)

    val bottomRightStarPath = buildAutoSaveFourPointSparklePath(
        cx = cx + clusterRadius * 0.42f,
        cy = cy + clusterRadius * 0.48f,
        outerRadius = clusterRadius * 0.32f,
        innerRadius = clusterRadius * 0.09f
    )
    drawPath(path = bottomRightStarPath, color = color, style = Fill)
}

private fun buildAutoSaveFourPointSparklePath(
    cx: Float,
    cy: Float,
    outerRadius: Float,
    innerRadius: Float
): Path {
    val path = Path()
    val d = innerRadius * 0.72f
    path.moveTo(cx, cy - outerRadius)
    path.quadraticTo(cx + d, cy - d, cx + outerRadius, cy)
    path.quadraticTo(cx + d, cy + d, cx, cy + outerRadius)
    path.quadraticTo(cx - d, cy + d, cx - outerRadius, cy)
    path.quadraticTo(cx - d, cy - d, cx, cy - outerRadius)
    path.close()
    return path
}

private fun checkBatteryExemption(context: Context): Boolean {
    return try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    } catch (_: Throwable) {
        true
    }
}

private fun checkBackgroundLocation(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkFineLocation(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun checkBluetoothConnect(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun requestBatteryExemption(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (_: Throwable) {
        try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Throwable) {
            openAppSettings(context)
        }
    }
}

private fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {}
}
