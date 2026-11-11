package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.LocalAppStrings
import com.example.util.WearApkManager
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearSideloadSheet(
    onDismiss: () -> Unit,
    isDevFunSettingsIcons: Boolean = false,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val sheetHeight = (configuration.screenHeightDp.dp * 0.88f).coerceAtLeast(360.dp)

    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val sheetContainerColor = if (isDark) Color(0xFF111111) else scheme.surfaceContainerLow
    val dragHandleColor = if (isDark) {
        Color.White.copy(alpha = 0.45f)
    } else {
        scheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        scrimColor = Color.Black.copy(alpha = 0.68f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = sheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            WearSideloadContent(
                isDevFunSettingsIcons = isDevFunSettingsIcons,
                onBack = {
                    coroutineScope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                }
            )
        }
    }
}

@Composable
fun WearSideloadContent(
    isDevFunSettingsIcons: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val strings = LocalAppStrings.current
    val isGreek = strings.languageSection == "Γλώσσα"
    val scrollState = rememberScrollState()

    var showGuide by remember { mutableStateOf(false) }
    val apkSize = remember { WearApkManager.getApkSizeString(context) }
    val wearVersion = remember { WearApkManager.getWearAppVersion(context) }

    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

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

    val itemCardBg = if (isDark) Color(0xFF1A1B1E) else scheme.surfaceContainerHigh
    val iconBoxBg = if (isDark) {
        lerp(Color(0xFF25211A), heroAccentColor, 0.22f)
    } else {
        lerp(scheme.surfaceContainerHighest, scheme.primaryContainer, 0.7f)
    }
    val iconBoxTint = if (isDark) {
        lerp(heroAccentColor, Color.White, 0.2f)
    } else {
        scheme.primary
    }

    val infoBannerBg = if (isDark) Color(0xFFECEFB4) else Color(0xFFE6EAA2)
    val infoBannerContent = Color(0xFF3F4416)

    val topCardShape = RoundedCornerShape(
        topStart = 22.dp,
        topEnd = 22.dp,
        bottomStart = 6.dp,
        bottomEnd = 6.dp
    )
    val middleCardShape = RoundedCornerShape(6.dp)
    val bottomCardShape = RoundedCornerShape(
        topStart = 6.dp,
        topEnd = 6.dp,
        bottomStart = 22.dp,
        bottomEnd = 22.dp
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Top Hero Card matching About Window UI
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = heroCardBg,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_wear_sideload_hero")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconBoxBg,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = iconBoxTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WearSealIconBadge(
                        badgeColor = heroAccentColor,
                        icon = Icons.Default.Watch,
                        iconTint = heroPillTextColor,
                        badgeSize = 68.dp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.wearOsApkDialogTitle,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = heroTitleColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(percent = 50),
                        color = heroAccentColor
                    ) {
                        Text(
                            text = "v$wearVersion • $apkSize",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = heroPillTextColor,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2. Section Header matching About Window UI
        Text(
            text = if (isGreek) "ΕΓΚΑΤΑΣΤΑΣΗ ΣΕ SMARTWATCH" else "WATCH INSTALLATION",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.1.sp,
            color = heroAccentColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, bottom = 10.dp)
        )

        // 3. Segmented Option Cards matching About Window UI
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Item 1: Save to Downloads
            WearSegmentedRowCard(
                title = strings.wearOsApkDownloadButton,
                subtitle = if (isGreek) {
                    "Εξαγωγή του αρχείου APK στον φάκελο Λήψεις του τηλεφώνου σας"
                } else {
                    "Export the companion watch APK to your phone's Downloads folder"
                },
                icon = Icons.Default.Download,
                iconShape = getSettingsIconContainerShapeForIndex(
                    enabled = isDevFunSettingsIcons,
                    index = 0,
                    defaultCornerDp = 14
                ),
                shape = topCardShape,
                cardBg = itemCardBg,
                iconBoxBg = iconBoxBg,
                iconTint = iconBoxTint,
                onClick = {
                    val (success, message) = WearApkManager.exportApkToDownloads(context)
                    if (success) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error: $message", Toast.LENGTH_SHORT).show()
                    }
                },
                testTag = "wear_sideload_download_card"
            )

            // Item 2: Share to Sideload App
            WearSegmentedRowCard(
                title = strings.wearOsApkShareButton,
                subtitle = if (isGreek) {
                    "Απευθείας αποστολή σε Easy Fire Tools, Bugjaeger ή Geminiman"
                } else {
                    "Send directly to Easy Fire Tools, Bugjaeger, or Geminiman"
                },
                icon = Icons.Default.Share,
                iconShape = getSettingsIconContainerShapeForIndex(
                    enabled = isDevFunSettingsIcons,
                    index = 1,
                    defaultCornerDp = 14
                ),
                shape = middleCardShape,
                cardBg = itemCardBg,
                iconBoxBg = iconBoxBg,
                iconTint = iconBoxTint,
                onClick = {
                    WearApkManager.shareApkForSideload(context)
                },
                testTag = "wear_sideload_share_card"
            )

            // Item 3: Expandable How to Sideload Guide
            Surface(
                shape = middleCardShape,
                color = itemCardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(middleCardShape)
                    .clickable { showGuide = !showGuide }
                    .testTag("wear_sideload_guide_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = getSettingsIconContainerShapeForIndex(
                                enabled = isDevFunSettingsIcons,
                                index = 2,
                                defaultCornerDp = 14
                            ),
                            color = iconBoxBg,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = null,
                                    tint = iconBoxTint,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.wearOsApkHowToSideload,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = scheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isGreek) {
                                    "Βήμα-προς-βήμα οδηγός ασύρματης εγκατάστασης στο Wear OS"
                                } else {
                                    "Step-by-step wireless ADB installation guide for Wear OS"
                                },
                                fontSize = 13.5.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = if (showGuide) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showGuide,
                        enter = fadeIn() + expandVertically(tween(250)),
                        exit = fadeOut() + shrinkVertically(tween(200))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GuideStep(
                                step = "1",
                                title = if (isGreek) "Ενεργοποίηση Επιλογών Προγραμματιστή" else "Enable Developer Options",
                                description = if (isGreek) {
                                    "Στο ρολόι σας, μεταβείτε στις Ρυθμίσεις > Σύστημα > Σχετικά > Εκδόσεις και πατήστε 7 φορές τον Αριθμό κατασκευής."
                                } else {
                                    "On your watch, go to Settings > System > About > Versions and tap Build number 7 times."
                                },
                                badgeColor = iconBoxBg,
                                badgeTextColor = iconBoxTint
                            )
                            GuideStep(
                                step = "2",
                                title = if (isGreek) "Ενεργοποίηση Ασύρματου Εντοπισμού Σφαλμάτων" else "Enable Wireless Debugging",
                                description = if (isGreek) {
                                    "Στις Ρυθμίσεις > Επιλογές προγραμματιστή του ρολογιού, ενεργοποιήστε το «Εντοπισμός σφαλμάτων ADB» και το «Ασύρματος εντοπισμός σφαλμάτων»."
                                } else {
                                    "In watch Settings > Developer options, toggle on 'ADB debugging' and 'Wireless debugging'."
                                },
                                badgeColor = iconBoxBg,
                                badgeTextColor = iconBoxTint
                            )
                            GuideStep(
                                step = "3",
                                title = if (isGreek) "Χρήση Εφαρμογής Sideload στο Τηλέφωνο" else "Use Sideload App on Phone",
                                description = if (isGreek) {
                                    "Εγκαταστήστε το «Easy Fire Tools» ή το «Bugjaeger» από το Google Play, εισαγάγετε τη διεύθυνση IP του ρολογιού σας και επιλέξτε το APK."
                                } else {
                                    "Install 'Easy Fire Tools' or 'Bugjaeger' from Google Play on your phone, enter your watch's IP address, and select the exported APK."
                                },
                                badgeColor = iconBoxBg,
                                badgeTextColor = iconBoxTint
                            )
                            GuideStep(
                                step = "4",
                                title = if (isGreek) "Εκκίνηση του Pixel Parking στο Ρολόι" else "Launch Pixel Parking on Watch",
                                description = if (isGreek) {
                                    "Το ρολόι σας θα εμφανίζει πλέον την πυξίδα πλοήγησης κάθε φορά που παρκάρετε!"
                                } else {
                                    "Your watch will now show the glanceable Waypoint compass whenever you park!"
                                },
                                badgeColor = iconBoxBg,
                                badgeTextColor = iconBoxTint
                            )
                        }
                    }
                }
            }

            // Item 4: GitHub Releases
            WearSegmentedRowCard(
                title = if (isGreek) "Κυκλοφορίες στο GitHub" else "GitHub Releases",
                subtitle = if (isGreek) {
                    "Λήψη του ανεξάρτητου Wear OS APK από το αποθετήριο GitHub"
                } else {
                    "Download the standalone Wear OS APK from GitHub Releases"
                },
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                iconShape = getSettingsIconContainerShapeForIndex(
                    enabled = isDevFunSettingsIcons,
                    index = 3,
                    defaultCornerDp = 14
                ),
                shape = bottomCardShape,
                cardBg = itemCardBg,
                iconBoxBg = iconBoxBg,
                iconTint = iconBoxTint,
                onClick = {
                    try {
                        uriHandler.openUri("https://github.com/ChadRat/Pixel-Parking/releases")
                    } catch (_: Exception) {
                        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                    }
                },
                testTag = "wear_sideload_github_card"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Bottom Olive/Green Info Banner matching About & License Windows
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = infoBannerBg,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_wear_sideload_info_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = infoBannerContent,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = strings.wearOsApkDialogDesc,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = infoBannerContent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WearSegmentedRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconShape: Shape,
    shape: Shape,
    cardBg: Color,
    iconBoxBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = shape,
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = iconShape,
                color = iconBoxBg,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun WearSealIconBadge(
    badgeColor: Color,
    icon: ImageVector,
    iconTint: Color,
    badgeSize: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "WearSealRotation"
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
        label = "WearSealHalfTurnRotation"
    )

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
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(badgeSize * 0.44f)
        )
    }
}

@Composable
private fun GuideStep(
    step: String,
    title: String,
    description: String,
    badgeColor: Color,
    badgeTextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = badgeColor,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = step,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeTextColor
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }
    }
}
