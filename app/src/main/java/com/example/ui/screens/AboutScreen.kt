package com.example.ui.screens

import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.components.SquigglyLoadingInline
import com.example.ui.components.WearSealIconBadge
import com.example.ui.components.WearSideloadContent
import com.example.ui.components.getSettingsIconContainerShapeForIndex
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ProvideAdaptiveDimensions
import com.example.ui.viewmodel.ParkingViewModel
import com.example.util.UpdateCheckResult
import com.example.util.UpdateManager
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private enum class AboutSubPage {
    MAIN,
    LICENSE,
    WEAR_SIDELOAD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    viewModel: ParkingViewModel,
    onDismiss: () -> Unit
) {
    ProvideAdaptiveDimensions {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()
        var currentSubPage by remember { mutableStateOf(AboutSubPage.MAIN) }
        val isInSubPage = currentSubPage != AboutSubPage.MAIN

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

        val showWhatsNew by viewModel.showWhatsNew.collectAsStateWithLifecycle()
        val sheetBlurRadius by animateDpAsState(
            targetValue = if (showWhatsNew) 24.dp else 0.dp,
            animationSpec = tween(durationMillis = 300),
            label = "about_sheet_blur"
        )

        BackHandler(enabled = isInSubPage) {
            currentSubPage = AboutSubPage.MAIN
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = !isInSubPage
            ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = sheetContainerColor,
            scrimColor = Color.Black.copy(alpha = 0.68f),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 12.dp)
                        .then(if (sheetBlurRadius > 0.dp) Modifier.blur(sheetBlurRadius) else Modifier),
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
            modifier = Modifier.testTag("about_bottom_sheet")
        ) {
            val view = LocalView.current
            val dialogDispatcherOwner: OnBackPressedDispatcherOwner? = remember(view) {
                view.findViewTreeOnBackPressedDispatcherOwner()
            }

            val backCallback = remember {
                object : OnBackPressedCallback(isInSubPage) {
                    override fun handleOnBackPressed() {
                        currentSubPage = AboutSubPage.MAIN
                    }
                }
            }

            SideEffect {
                backCallback.isEnabled = isInSubPage
            }

            DisposableEffect(dialogDispatcherOwner) {
                val dispatcher = dialogDispatcherOwner?.onBackPressedDispatcher
                dispatcher?.addCallback(backCallback)
                onDispose {
                    backCallback.remove()
                }
            }

            val contentWithDispatcher = @Composable {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = sheetHeight)
                        .then(if (sheetBlurRadius > 0.dp) Modifier.blur(sheetBlurRadius) else Modifier)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 6.dp)
                ) {
                    AboutContent(
                        viewModel = viewModel,
                        scrollState = scrollState,
                        showLicenseDetail = currentSubPage == AboutSubPage.LICENSE,
                        onToggleLicenseDetail = { show ->
                            currentSubPage = if (show) AboutSubPage.LICENSE else AboutSubPage.MAIN
                        },
                        showWearSideloadDetail = currentSubPage == AboutSubPage.WEAR_SIDELOAD,
                        onToggleWearSideloadDetail = { show ->
                            currentSubPage = if (show) AboutSubPage.WEAR_SIDELOAD else AboutSubPage.MAIN
                        },
                        onClose = {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        }
                    )
                }
            }

            if (dialogDispatcherOwner != null) {
                CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides dialogDispatcherOwner) {
                    BackHandler(enabled = isInSubPage) {
                        currentSubPage = AboutSubPage.MAIN
                    }
                    contentWithDispatcher()
                }
            } else {
                contentWithDispatcher()
            }
        }
    }
}

@Composable
fun AboutScreen(
    viewModel: ParkingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AboutSheet(
        viewModel = viewModel,
        onDismiss = onBack
    )
}

@Composable
fun AboutContent(
    viewModel: ParkingViewModel,
    scrollState: ScrollState,
    showLicenseDetail: Boolean,
    onToggleLicenseDetail: (Boolean) -> Unit,
    showWearSideloadDetail: Boolean = false,
    onToggleWearSideloadDetail: (Boolean) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isGreek = appLanguage == AppLanguage.GREEK

    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

    // Shared palette combining the Hero Card from Screenshot 1 and Project Cards + Info Banner from Screenshot 2
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

    // Bottom info card warm pastel olive/yellow-green matching Screenshot 2
    val infoBannerBg = if (isDark) Color(0xFFECEFB4) else Color(0xFFE6EAA2)
    val infoBannerContent = Color(0xFF3F4416)

    val isCheckingUpdates by viewModel.isCheckingForUpdates.collectAsState()
    val updateCheckState by viewModel.updateCheckState.collectAsState()
    val isDevFunSettingsIcons by viewModel.isDevFunSettingsIcons.collectAsState()

    val activeSubPage = when {
        showLicenseDetail -> AboutSubPage.LICENSE
        showWearSideloadDetail -> AboutSubPage.WEAR_SIDELOAD
        else -> AboutSubPage.MAIN
    }

    BackHandler(enabled = activeSubPage != AboutSubPage.MAIN) {
        if (showLicenseDetail) onToggleLicenseDetail(false)
        if (showWearSideloadDetail) onToggleWearSideloadDetail(false)
    }

    Crossfade(
        targetState = activeSubPage,
        animationSpec = tween(durationMillis = 300),
        label = "AboutViewCrossfade"
    ) { subPage ->
        when (subPage) {
            AboutSubPage.MAIN -> {
            val uriHandler = LocalUriHandler.current
            val versionLabel = BuildConfig.VERSION_NAME

            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Top Hero Card (App Icon, Title, Version Pill) - STATIONARY
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = heroCardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_about_hero")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AboutSealBadge(
                            badgeColor = heroAccentColor,
                            badgeSize = 64.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = strings.aboutSection,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = heroTitleColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = heroAccentColor,
                            modifier = Modifier
                                .testTag("pill_about_version")
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable {
                                    viewModel.showWhatsNewWindow(force = true)
                                }
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

                // 2. Scrollable Body containing PROJECT Header & Option Cards (THESE MOVE)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // "PROJECT" Section Header (from Screenshot 2)
                    Text(
                        text = if (isGreek) "ΕΡΓΟ" else "PROJECT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        color = heroAccentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, bottom = 10.dp)
                    )

                // 3. Segmented Project Option Cards (from Screenshot 2)
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // Item 1: Check for updates
                    Surface(
                        shape = topCardShape,
                        color = itemCardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(topCardShape)
                            .clickable(enabled = !isCheckingUpdates) {
                                viewModel.checkForUpdates(context)
                            }
                            .testTag("about_item_check_updates")
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
                                AboutProjectIconBox(
                                    icon = Icons.Default.SystemUpdate,
                                    containerColor = iconBoxBg,
                                    iconTint = iconBoxTint,
                                    iconShape = getSettingsIconContainerShapeForIndex(
                                        enabled = isDevFunSettingsIcons,
                                        index = 0,
                                        defaultCornerDp = 14
                                    )
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.checkForUpdates,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = scheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isCheckingUpdates) {
                                            if (isGreek) "Έλεγχος για ενημερώσεις..." else "Checking for updates..."
                                        } else if (isGreek) {
                                            "Δείτε την εγκατεστημένη έκδοση, την τελευταία κυκλοφορία και το συμβατό APK"
                                        } else {
                                            "See the installed build, latest release and compatible APK"
                                        },
                                        fontSize = 13.5.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = scheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                if (isCheckingUpdates) {
                                    SquigglyLoadingInline(
                                        color = heroAccentColor,
                                        width = 28.dp,
                                        height = 14.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Inline expandable update check result
                            AnimatedVisibility(
                                visible = updateCheckState != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                ) {
                                    when (val result = updateCheckState) {
                                        is UpdateCheckResult.UpdateAvailable -> {
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = heroCardBg.copy(alpha = 0.75f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(
                                                        text = "${strings.updateAvailable} (v${result.latestVersion})",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = heroTitleColor
                                                    )
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                viewModel.downloadAndInstallUpdate(context, result.downloadUrl)
                                                            },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = heroAccentColor,
                                                                contentColor = heroPillTextColor
                                                            ),
                                                            modifier = Modifier.weight(1f),
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Text(strings.download, fontWeight = FontWeight.Bold)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                UpdateManager.openInBrowser(context, result.downloadUrl)
                                                            },
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Text("Browser")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        is UpdateCheckResult.UpToDate -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                Text(
                                                    text = if (isGreek) {
                                                        "Έχετε την τελευταία έκδοση (v${result.currentVersion})"
                                                    } else {
                                                        "You are running the latest version (v${result.currentVersion})"
                                                    },
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = heroAccentColor
                                                )
                                                if (result.downloadUrl != null) {
                                                    TextButton(
                                                        onClick = {
                                                            viewModel.downloadAndInstallUpdate(context, result.downloadUrl)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "Re-download APK (v${result.currentVersion})",
                                                            fontSize = 12.sp,
                                                            color = heroAccentColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        is UpdateCheckResult.Error -> {
                                            Text(
                                                text = "Check failed: ${result.message}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = scheme.error,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                        null -> {}
                                    }
                                }
                            }
                        }
                    }

                    // Item 2: Source code
                    AboutProjectRowCard(
                        title = if (isGreek) "Πηγαίος κώδικας" else "Source code",
                        subtitle = if (isGreek) {
                            "Διαβάστε, επιθεωρήστε ή συνεισφέρετε στο ${strings.appName} στο GitHub"
                        } else {
                            "Read, inspect or contribute to ${strings.appName} on GitHub"
                        },
                        icon = Icons.Default.Code,
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
                            try {
                                uriHandler.openUri("https://github.com/ChadRat/Pixel-Parking")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        testTag = "about_item_source_code"
                    )

                    // Item 3: License
                    AboutProjectRowCard(
                        title = if (isGreek) "Άδεια χρήσης" else "License",
                        subtitle = "GNU General Public License v3.0",
                        icon = Icons.Default.Gavel,
                        iconShape = getSettingsIconContainerShapeForIndex(
                            enabled = isDevFunSettingsIcons,
                            index = 2,
                            defaultCornerDp = 14
                        ),
                        shape = middleCardShape,
                        cardBg = itemCardBg,
                        iconBoxBg = iconBoxBg,
                        iconTint = iconBoxTint,
                        onClick = {
                            onToggleLicenseDetail(true)
                        },
                        testTag = "about_item_license"
                    )

                    // Item: Check Whats New
                    AboutProjectRowCard(
                        title = strings.checkWhatsNew,
                        subtitle = strings.checkWhatsNewSubtitle,
                        icon = Icons.Default.AutoAwesome,
                        iconShape = getSettingsIconContainerShapeForIndex(
                            enabled = isDevFunSettingsIcons,
                            index = 3,
                            defaultCornerDp = 14
                        ),
                        shape = middleCardShape,
                        cardBg = itemCardBg,
                        iconBoxBg = iconBoxBg,
                        iconTint = iconBoxTint,
                        onClick = {
                            viewModel.showWhatsNewWindow(force = true)
                        },
                        testTag = "about_item_check_whats_new"
                    )

                    // Item 4: Wear OS Companion APK
                    AboutProjectRowCard(
                        title = strings.wearOsApkOptionTitle,
                        subtitle = strings.wearOsApkOptionSubtitle,
                        icon = Icons.Default.Watch,
                        iconShape = getSettingsIconContainerShapeForIndex(
                            enabled = isDevFunSettingsIcons,
                            index = 4,
                            defaultCornerDp = 14
                        ),
                        shape = middleCardShape,
                        cardBg = itemCardBg,
                        iconBoxBg = iconBoxBg,
                        iconTint = iconBoxTint,
                        onClick = {
                            onToggleWearSideloadDetail(true)
                        },
                        testTag = "about_item_wear_os"
                    )

                    // Item 5: Report a bug
                    AboutProjectRowCard(
                        title = if (isGreek) "Αναφορά σφάλματος" else "Report a bug",
                        subtitle = if (isGreek) {
                            "Ανοίξτε ένα νέο ζήτημα στο GitHub"
                        } else {
                            "Open a new issue on GitHub"
                        },
                        icon = Icons.Default.BugReport,
                        iconShape = getSettingsIconContainerShapeForIndex(
                            enabled = isDevFunSettingsIcons,
                            index = 5,
                            defaultCornerDp = 14
                        ),
                        shape = bottomCardShape,
                        cardBg = itemCardBg,
                        iconBoxBg = iconBoxBg,
                        iconTint = iconBoxTint,
                        onClick = {
                            try {
                                uriHandler.openUri("https://github.com/ChadRat/Pixel-Parking/issues")
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        testTag = "about_item_report_bug"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
            }
            }
            AboutSubPage.WEAR_SIDELOAD -> {
                WearSideloadContent(
                    isDevFunSettingsIcons = isDevFunSettingsIcons,
                    onBack = { onToggleWearSideloadDetail(false) }
                )
            }
            AboutSubPage.LICENSE -> {
                // License Detail View styled identically to the About Window UI
                val clipboardManager = LocalClipboardManager.current
                val uriHandler = LocalUriHandler.current
                val licenseScrollState = rememberScrollState()

                val topCardShape = RoundedCornerShape(
                    topStart = 22.dp,
                    topEnd = 22.dp,
                    bottomStart = 6.dp,
                    bottomEnd = 6.dp
                )
                val bottomCardShape = RoundedCornerShape(
                    topStart = 6.dp,
                    topEnd = 6.dp,
                    bottomStart = 22.dp,
                    bottomEnd = 22.dp
                )

                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Top Hero Card matching About Window UI - STATIONARY
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = heroCardBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_license_hero")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                        ) {
                            IconButton(
                                onClick = { onToggleLicenseDetail(false) },
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
                                    icon = Icons.Default.Gavel,
                                    iconTint = heroPillTextColor,
                                    badgeSize = 68.dp
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (isGreek) "Άδεια Χρήσης" else "Open Source License",
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
                                        text = "GNU GPL v3.0",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = heroPillTextColor,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Scrollable Body for License Actions & Full Text (THESE MOVE)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(licenseScrollState)
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Section Header matching About Window UI
                        Text(
                            text = if (isGreek) "ΕΝΕΡΓΕΙΕΣ ΑΔΕΙΑΣ" else "LICENSE ACTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        color = heroAccentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, bottom = 10.dp)
                    )

                    // 3. Segmented Action Cards matching About Window UI
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        AboutProjectRowCard(
                            title = strings.copyButton,
                            subtitle = if (isGreek) {
                                "Αντιγραφή του πλήρους κειμένου της άδειας GPL-3.0 στο πρόχειρο"
                            } else {
                                "Copy the full GPL-3.0 license text to your clipboard"
                            },
                            icon = Icons.Default.ContentCopy,
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
                                clipboardManager.setText(AnnotatedString(gplv3LicenseText))
                                Toast.makeText(
                                    context,
                                    if (isGreek) "Η άδεια αντιγράφηκε στο πρόχειρο" else "License copied to clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            testTag = "license_item_copy"
                        )

                        AboutProjectRowCard(
                            title = strings.gnuOrgButton,
                            subtitle = if (isGreek) {
                                "Προβολή της επίσημης άδειας GNU General Public License v3.0 στον περιηγητή"
                            } else {
                                "Open the official GNU General Public License v3.0 at gnu.org"
                            },
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            iconShape = getSettingsIconContainerShapeForIndex(
                                enabled = isDevFunSettingsIcons,
                                index = 1,
                                defaultCornerDp = 14
                            ),
                            shape = bottomCardShape,
                            cardBg = itemCardBg,
                            iconBoxBg = iconBoxBg,
                            iconTint = iconBoxTint,
                            onClick = {
                                try {
                                    uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            testTag = "license_item_gnu_org"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Open-Source GPL-3.0 Olive/Green Info Banner with natural, meaningful Greek & English text
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = infoBannerBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_about_info_banner")
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
                                text = if (isGreek) {
                                    "Το ${strings.appName} είναι ελεύθερο λογισμικό ανοιχτού κώδικα που διανέμεται υπό την άδεια GNU General Public License v3.0 (GPL-3.0). Έχετε το δικαίωμα να χρησιμοποιείτε, να μελετάτε, να μοιράζεστε και να τροποποιείτε τον πηγαίο κώδικα ελεύθερα."
                                } else {
                                    "${strings.appName} is free and open-source software distributed under the GNU General Public License v3.0 (GPL-3.0). You are free to use, study, share, and modify the source code under the terms of this license."
                                },
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = infoBannerContent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. Full License Text Section matching About Window Card UI
                    Text(
                        text = if (isGreek) "ΚΕΙΜΕΝΟ ΑΔΕΙΑΣ" else "LICENSE DOCUMENT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        color = heroAccentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, bottom = 10.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = itemCardBg),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "GNU GENERAL PUBLIC LICENSE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = scheme.onSurface
                            )
                            Text(
                                text = "Version 3, 29 June 2007",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            Text(
                                text = gplv3LicenseText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}
}

@Composable
private fun AboutProjectRowCard(
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
            AboutProjectIconBox(
                icon = icon,
                containerColor = iconBoxBg,
                iconTint = iconTint,
                iconShape = iconShape
            )

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
private fun AboutProjectIconBox(
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    iconShape: Shape = RoundedCornerShape(14.dp)
) {
    Surface(
        shape = iconShape,
        color = containerColor,
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
}

/**
 * 12-pointed wavy scalloped seal shape that rotates clockwise for half a turn (180 degrees)
 * slowly and transitions smoothly to spin anticlockwise for half a turn (and vice versa),
 * with the app icon foreground (without its white background) centered inside.
 */
@Composable
private fun AboutSealBadge(
    badgeColor: Color,
    badgeSize: Dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "AboutSealRotation"
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
        label = "AboutSealHalfTurnRotation"
    )

    Box(
        modifier = modifier
            .size(badgeSize)
            .testTag("about_app_icon"),
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

        // App icon foreground only (no white background), centered inside the scalloped seal shape
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Icon",
            modifier = Modifier
                .fillMaxSize()
                .scale(1.06f)
        )
    }
}

@Composable
fun WavyDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray.copy(alpha = 0.5f),
    waveLength: Float = 36f,
    amplitude: Float = 9f,
    thickness: Float = 6f
) {
    com.example.ui.components.WavyDivider(
        modifier = modifier,
        color = color,
        waveLength = waveLength,
        amplitude = amplitude,
        thickness = thickness
    )
}
