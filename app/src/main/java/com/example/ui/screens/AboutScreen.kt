package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.ParkingViewModel
import com.example.util.UpdateCheckResult
import com.example.util.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    viewModel: ParkingViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var showLicenseDetail by remember { mutableStateOf(false) }

    // Opens at 75% of screen height by default, smoothly enlarges to 95% when viewing license
    val targetFraction = if (showLicenseDetail) 0.95f else 0.75f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(
            durationMillis = 380,
            easing = FastOutSlowInEasing
        ),
        label = "aboutSheetHeightFraction"
    )

    BackHandler(enabled = showLicenseDetail) {
        showLicenseDetail = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = !showLicenseDetail
        ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("about_bottom_sheet")
    ) {
        val view = LocalView.current
        val dialogDispatcherOwner: OnBackPressedDispatcherOwner? = remember(view) {
            view.findViewTreeOnBackPressedDispatcherOwner()
        }

        val backCallback = remember {
            object : OnBackPressedCallback(showLicenseDetail) {
                override fun handleOnBackPressed() {
                    showLicenseDetail = false
                }
            }
        }

        SideEffect {
            backCallback.isEnabled = showLicenseDetail
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
                    .fillMaxHeight(animatedFraction)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
            ) {
                AboutContent(
                    viewModel = viewModel,
                    scrollState = scrollState,
                    showLicenseDetail = showLicenseDetail,
                    onToggleLicenseDetail = { showLicenseDetail = it },
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
                BackHandler(enabled = showLicenseDetail) {
                    showLicenseDetail = false
                }
                contentWithDispatcher()
            }
        } else {
            contentWithDispatcher()
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
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val isCheckingUpdates by viewModel.isCheckingForUpdates.collectAsState()
    val updateCheckState by viewModel.updateCheckState.collectAsState()

    val carBadgeColor by animateColorAsState(
        if (isDark) Color(0xFF382F13) else MaterialTheme.colorScheme.primaryContainer,
        tween(450),
        label = "about_car_badge_bg"
    )
    val carBadgeTint by animateColorAsState(
        if (isDark) Color(0xFFFFD54F) else MaterialTheme.colorScheme.primary,
        tween(450),
        label = "about_car_badge_tint"
    )
    val codeIconBg by animateColorAsState(
        if (isDark) Color(0xFF1A365D) else Color(0xFFD0E4FF),
        tween(450),
        label = "about_code_icon_bg"
    )
    val codeIconText by animateColorAsState(
        if (isDark) Color.White else Color(0xFF001D36),
        tween(450),
        label = "about_code_icon_text"
    )
    val issueIconBg by animateColorAsState(
        if (isDark) Color(0xFF2A3439) else Color(0xFFD8E3EA),
        tween(450),
        label = "about_issue_icon_bg"
    )
    val issueIconTint by animateColorAsState(
        if (isDark) Color.White else Color(0xFF2B373C),
        tween(450),
        label = "about_issue_icon_tint"
    )
    val licenseIconBg by animateColorAsState(
        if (isDark) Color(0xFF233E3B) else Color(0xFFCCE8E3),
        tween(450),
        label = "about_license_icon_bg"
    )
    val licenseIconTint by animateColorAsState(
        if (isDark) Color.White else Color(0xFF003831),
        tween(450),
        label = "about_license_icon_tint"
    )
    val updateCardBg by animateColorAsState(
        if (isDark) Color(0xFF1B2824) else MaterialTheme.colorScheme.surfaceContainerHigh,
        tween(450),
        label = "about_update_card_bg"
    )
    val updateBtnBg by animateColorAsState(
        if (isDark) Color(0xFF8BD4C9) else MaterialTheme.colorScheme.primary,
        tween(450),
        label = "about_update_btn_bg"
    )
    val updateBtnContent by animateColorAsState(
        if (isDark) Color(0xFF04483E) else MaterialTheme.colorScheme.onPrimary,
        tween(450),
        label = "about_update_btn_content"
    )

    BackHandler(enabled = showLicenseDetail) {
        onToggleLicenseDetail(false)
    }

    Crossfade(
        targetState = showLicenseDetail,
        animationSpec = tween(durationMillis = 300),
        label = "AboutViewCrossfade"
    ) { isLicense ->
        if (!isLicense) {
            // Main About View
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Big centered car icon with app name and version number below it
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = carBadgeColor,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = "App Icon",
                            tint = carBadgeTint,
                            modifier = Modifier.size(62.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = strings.appName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Main options Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val uriHandler = LocalUriHandler.current

                    // 1. Source Code Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    uriHandler.openUri("https://github.com/ChadRat/Pixel-Parking")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(codeIconBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Code",
                                color = codeIconText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.sourceCodeGitHub,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.sourceCodeSubtitle,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Wavy Divider
                    WavyDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        waveLength = 36f,
                        amplitude = 8f,
                        thickness = 5f
                    )

                    // 2. Report an Issue Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    uriHandler.openUri("https://github.com/ChadRat/Pixel-Parking/issues")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(issueIconBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = issueIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.reportAnIssueTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.reportAnIssueUrl,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Wavy Divider
                    WavyDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        waveLength = 36f,
                        amplitude = 8f,
                        thickness = 5f
                    )

                    // 3. License Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleLicenseDetail(true)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(licenseIconBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = licenseIconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.licenseTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.licenseSubtitle,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Check for Updates Button and Results
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = updateCardBg
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isCheckingUpdates) {
                                viewModel.checkForUpdates(context)
                            }
                        },
                        enabled = !isCheckingUpdates,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = updateBtnBg,
                            contentColor = updateBtnContent
                        ),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = updateBtnContent
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Checking for updates...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = strings.checkForUpdates,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.checkForUpdates,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }

                    // Display Result Details when available
                    when (val result = updateCheckState) {
                        is UpdateCheckResult.UpdateAvailable -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Text(
                                        text = "${strings.updateAvailable} (v${result.latestVersion})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "✓ You are running the latest version (v${result.currentVersion})",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (result.downloadUrl != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TextButton(
                                        onClick = {
                                            viewModel.downloadAndInstallUpdate(context, result.downloadUrl)
                                        }
                                    ) {
                                        Text("Re-download APK (v${result.currentVersion})", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        is UpdateCheckResult.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Check failed: ${result.message}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        null -> {
                            // Idle state, no result yet
                        }
                    }
                }
            }
        }
    } else {
        // License Detail View inside Sheet
        val clipboardManager = LocalClipboardManager.current
        val uriHandler = LocalUriHandler.current

        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.licenseTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "GNU General Public License v3.0",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = { onToggleLicenseDetail(false) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons Row (Copy + GNU.org)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(gplv3LicenseText))
                        Toast.makeText(context, "License copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.copyButton, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.gnuOrgButton, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable License Text Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "GNU GENERAL PUBLIC LICENSE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version 3, 29 June 2007",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Text(
                        text = gplv3LicenseText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        val width = size.width
        val midY = size.height / 2f
        val path = Path().apply {
            moveTo(0f, midY)
            var x = 0f
            while (x < width) {
                val nextX = x + waveLength / 2f
                val ctrlX = x + waveLength / 4f
                val targetY = if ((x / (waveLength / 2f)).toInt() % 2 == 0) midY + amplitude else midY - amplitude
                quadraticTo(ctrlX, targetY, nextX, midY)
                x = nextX
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = thickness, cap = StrokeCap.Round)
        )
    }
}
