package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.draw.rotate
import com.example.ui.components.TimerRemindersSelectorRow
import com.example.util.HapticHelper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import com.example.ui.components.GeofenceIcon
import com.example.ui.components.WalkAwayIcon
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.components.AnimatedNavIcon
import com.example.ui.components.BabyBottleIcon
import com.example.ui.components.CarBadgeStyle
import com.example.ui.components.CapyVariant
import com.example.ui.components.FullGearNavIcon
import com.example.sensor.DistanceUnit
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.ParkingViewModel
import com.example.util.AlarmSoundHelper

@Composable
fun SettingsScreen(
    viewModel: ParkingViewModel,
    onNavigateTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val isOledMode by viewModel.oledMode.collectAsState()
    val isDynamicColor by viewModel.dynamicColor.collectAsState()
    val autoSunTheme by viewModel.autoSunTheme.collectAsState()
    val isDaytime by viewModel.isDaytime.collectAsState()
    val currentCarBadgeStyle by viewModel.carBadgeStyle.collectAsState()
    val currentCapyVariant by viewModel.capyVariant.collectAsState()
    val isDevCapyCarsEnabled by viewModel.isDevCapyCarsEnabled.collectAsState()
    val currentLanguage by viewModel.appLanguage.collectAsState()
    val distanceUnit by viewModel.distanceUnit.collectAsState()
    val isParkingTimerEnabled by viewModel.isParkingTimerFeatureEnabled.collectAsState()
    val timerReminders by viewModel.timerRemindersMinutes.collectAsState()
    val activeSpot by viewModel.activeSpot.collectAsState()
    val telemetry by viewModel.navigationTelemetry.collectAsState()
    val isCrownOnRight by viewModel.wearCrownOnRight.collectAsState()
    val isGeofenceAutoPark by viewModel.isGeofenceAutoParkEnabled.collectAsState()
    val isDevFunWalkAwayIcon by viewModel.isDevFunWalkAwayIcon.collectAsState()

    var isCrownOptionsExpanded by remember { mutableStateOf(false) }
    var isMilesExpanded by rememberSaveable { mutableStateOf(distanceUnit == DistanceUnit.IMPERIAL) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                viewModel.updateAlarmSoundUri(uri)
            }
        }
    }

    val geofencePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION]
            ?: (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED)

        val activityRecognitionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[android.Manifest.permission.ACTIVITY_RECOGNITION]
                ?: (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED)
        } else true

        if (fineLocationGranted && activityRecognitionGranted) {
            viewModel.setGeofenceAutoParkEnabled(true)
        }
    }

    var selectedReminderIndex by remember { mutableIntStateOf(-1) }
    var activeReminderBounds by remember { mutableStateOf<Rect?>(null) }

    BackHandler(enabled = selectedReminderIndex != -1) {
        selectedReminderIndex = -1
        activeReminderBounds = null
    }

    val smoothFling = com.example.util.rememberSmoothFlingBehavior()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(selectedReminderIndex, activeReminderBounds) {
                if (selectedReminderIndex != -1 && activeReminderBounds != null) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                        if (!activeReminderBounds!!.contains(down.position)) {
                            selectedReminderIndex = -1
                            activeReminderBounds = null
                        }
                    }
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .testTag("settings_screen"),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            flingBehavior = smoothFling
        ) {
        // Top Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            text = strings.settingsTitle,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.settingsSubtitle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ==========================================
        // 1. LANGUAGE SELECTION / ΕΠΙΛΟΓΗ ΓΛΩΣΣΑΣ
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("language_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Language,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Text(
                                text = strings.languageTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (currentLanguage == AppLanguage.ENGLISH) {
                                val arrowRotation by animateFloatAsState(
                                    targetValue = if (isMilesExpanded) 180f else 0f,
                                    label = "miles_arrow_rot"
                                )
                                IconButton(
                                    onClick = { isMilesExpanded = !isMilesExpanded },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("distance_unit_arrow_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Miles setting",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer { rotationZ = arrowRotation }
                                    )
                                }
                            }

                            Switch(
                                checked = currentLanguage == AppLanguage.GREEK,
                                onCheckedChange = { isGreek ->
                                    viewModel.setAppLanguage(if (isGreek) AppLanguage.GREEK else AppLanguage.ENGLISH)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // "Miles" toggle similar to the rest of the settings ui (without an icon)
                    AnimatedVisibility(
                        visible = currentLanguage == AppLanguage.ENGLISH && isMilesExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))

                            WavyDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                waveLength = 36f,
                                amplitude = 9f,
                                thickness = 6f
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newUnit = if (distanceUnit == DistanceUnit.IMPERIAL) DistanceUnit.METRIC else DistanceUnit.IMPERIAL
                                        viewModel.setDistanceUnit(newUnit)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Miles",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = "Change the metric system (kilometres) to imperial miles",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Switch(
                                    checked = distanceUnit == DistanceUnit.IMPERIAL,
                                    onCheckedChange = { isMiles ->
                                        viewModel.setDistanceUnit(if (isMiles) DistanceUnit.IMPERIAL else DistanceUnit.METRIC)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("miles_switch")
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            WavyDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                waveLength = 36f,
                amplitude = 9f,
                thickness = 6f
            )
        }

        // ==========================================
        // 2. APPEARANCE & THEME
        // ==========================================
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = strings.appearanceSection,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3-button mode switcher: Light, Dark, System
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            title = strings.lightTheme,
                            icon = Icons.Default.LightMode,
                            isSelected = currentThemeMode == AppThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(AppThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            title = strings.darkTheme,
                            icon = Icons.Default.DarkMode,
                            isSelected = currentThemeMode == AppThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(AppThemeMode.DARK) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            title = strings.systemTheme,
                            icon = Icons.Default.AutoAwesome,
                            isSelected = currentThemeMode == AppThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(AppThemeMode.SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Sun Schedule Settings (Only appears when System theme is selected)
                    AnimatedVisibility(
                        visible = currentThemeMode == AppThemeMode.SYSTEM,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            WavyDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                waveLength = 36f,
                                amplitude = 9f,
                                thickness = 6f
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setAutoSunTheme(!autoSunTheme) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.BrightnessAuto,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = strings.sunScheduleTitle,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = strings.sunScheduleSubtitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Switch(
                                    checked = autoSunTheme,
                                    onCheckedChange = { viewModel.setAutoSunTheme(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    WavyDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        waveLength = 36f,
                        amplitude = 9f,
                        thickness = 6f
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic Wallpaper Palette toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setDynamicColor(!isDynamicColor) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Wallpaper,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = strings.dynamicColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                                Text(
                                    text = strings.dynamicColorSubtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Switch(
                            checked = isDynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    val systemInDark = isSystemInDarkTheme()
                    val isOledAvailable = when (currentThemeMode) {
                        AppThemeMode.DARK -> true
                        AppThemeMode.SYSTEM -> if (autoSunTheme) !isDaytime else systemInDark
                        AppThemeMode.LIGHT -> false
                    }

                    // OLED Pure Pitch Black background toggle (available only when dark theme or system theme on dark is enabled)
                    AnimatedVisibility(
                        visible = isOledAvailable,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(18.dp))

                            WavyDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                waveLength = 36f,
                                amplitude = 9f,
                                thickness = 6f
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setOledMode(!isOledMode) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.DarkMode,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = strings.oledMode,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = strings.oledSubtitle,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = isOledMode,
                                    onCheckedChange = { viewModel.setOledMode(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                }
            }
        }

        item {
            WavyDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                waveLength = 36f,
                amplitude = 9f,
                thickness = 6f
            )
        }

        // ==========================================
        // GEOFENCE WALKING AUTO-PARK (50m)
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("geofence_autopark_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    WalkAwayIcon(
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                        isFun = isDevFunWalkAwayIcon
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = strings.devGeofenceAutoParkTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Text(
                                            text = "βeta",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = strings.devGeofenceAutoParkSubtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isGeofenceAutoPark,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    val perms = mutableListOf<String>()
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.ACCESS_FINE_LOCATION
                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        perms.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                                context,
                                                android.Manifest.permission.ACTIVITY_RECOGNITION
                                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                        ) {
                                            perms.add(android.Manifest.permission.ACTIVITY_RECOGNITION)
                                        }
                                    }
                                    if (perms.isNotEmpty()) {
                                        geofencePermLauncher.launch(perms.toTypedArray())
                                    } else {
                                        viewModel.setGeofenceAutoParkEnabled(true)
                                    }
                                } else {
                                    viewModel.setGeofenceAutoParkEnabled(false)
                                }
                            },
                            modifier = Modifier.testTag("geofence_autopark_switch")
                        )
                    }
                }
            }
        }

        item {
            WavyDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                waveLength = 36f,
                amplitude = 9f,
                thickness = 6f
            )
        }

        // ==========================================
        // 3. CAR ARTWORK STYLE / ΣΤΙΛ ΓΡΑΦΙΚΩΝ ΑΥΤΟΚΙΝΗΤΩΝ
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("car_style_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.carBadgeStyleTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.carBadgeStyleSubtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Chip switcher: Material Style vs Cinematic vs Capy (if enabled in dev settings)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            title = strings.carBadgeStyleMaterial,
                            icon = Icons.Default.Palette,
                            isSelected = currentCarBadgeStyle == CarBadgeStyle.MATERIAL,
                            onClick = { viewModel.setCarBadgeStyle(CarBadgeStyle.MATERIAL) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            title = strings.carBadgeStyleCinematic,
                            icon = Icons.Default.AutoAwesome,
                            isSelected = currentCarBadgeStyle == CarBadgeStyle.CINEMATIC,
                            onClick = { viewModel.setCarBadgeStyle(CarBadgeStyle.CINEMATIC) },
                            modifier = Modifier.weight(1f)
                        )
                        if (isDevCapyCarsEnabled) {
                            ThemeOptionChip(
                                title = strings.carBadgeStyleCapy,
                                icon = Icons.Default.Pets,
                                isSelected = currentCarBadgeStyle == CarBadgeStyle.CAPY,
                                onClick = { viewModel.setCarBadgeStyle(CarBadgeStyle.CAPY) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Capybara Character Variant selector (Baby vs Adult) - appears only if Capy style is enabled
                    AnimatedVisibility(
                        visible = currentCarBadgeStyle == CarBadgeStyle.CAPY,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            WavyDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                waveLength = 36f,
                                amplitude = 9f,
                                thickness = 6f
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Pets,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.capyVariantTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.capyVariantSubtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeOptionChip(
                                    title = strings.capyVariantBaby,
                                    iconContent = { tint ->
                                        BabyBottleIcon(
                                            tint = tint,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    isSelected = currentCapyVariant == CapyVariant.BABY,
                                    onClick = { viewModel.setCapyVariant(CapyVariant.BABY) },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    title = strings.capyVariantAdult,
                                    icon = Icons.Default.Pets,
                                    isSelected = currentCapyVariant == CapyVariant.ADULT,
                                    onClick = { viewModel.setCapyVariant(CapyVariant.ADULT) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            WavyDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                waveLength = 36f,
                amplitude = 9f,
                thickness = 6f
            )
        }

        // ==========================================
        // 4. BATTERY OPTIMIZATION
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable {
                        viewModel.showBackgroundAutoSaveSetupSheet()
                    }
                    .testTag("battery_optimization_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Left squircle icon container matching provided screenshot
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = strings.ignoreBatteryOptimizations,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Title & Subtitle matching layout exactly
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.ignoreBatteryOptimizations,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = strings.ignoreBatteryOptimizationsDesc,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        item {
            WavyDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                waveLength = 36f,
                amplitude = 9f,
                thickness = 6f
            )
        }

        // ==========================================
        // 4b. ALWAYS-ON BLUETOOTH DETECTION
        // ==========================================
        item {
            var isAutoMonitorOn by remember {
                mutableStateOf(com.example.service.AutoParkLocationService.isMonitoringEnabled(context))
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("always_on_bluetooth_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = strings.persistentMonitorTitle,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.persistentMonitorTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = strings.persistentMonitorDesc,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isAutoMonitorOn,
                        onCheckedChange = { checked ->
                            isAutoMonitorOn = checked
                            com.example.service.AutoParkLocationService.setMonitoringEnabled(context, checked)
                        },
                        modifier = Modifier.testTag("always_on_bluetooth_switch")
                    )
                }
            }
        }

        item {
            WavyDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                waveLength = 36f,
                amplitude = 9f,
                thickness = 6f
            )
        }

        // ==========================================
        // 5. PARKING & CHARGING ALARM SOUND
        // ==========================================
        if (isParkingTimerEnabled) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_sound_settings_card")
                        .clickable {
                            val currentUri = AlarmSoundHelper.getAlarmUri(context)
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Parking Timer Alarm")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = strings.timerAlarmSound,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = strings.timerAlarmSound,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = strings.timerAlarmSoundDesc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                val currentUri = AlarmSoundHelper.getAlarmUri(context)
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Parking Timer Alarm")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                }
                                ringtonePickerLauncher.launch(intent)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("change_alarm_sound_gear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            item {
                WavyDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    waveLength = 36f,
                    amplitude = 9f,
                    thickness = 6f
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("timer_notifications_reminders_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = strings.earlyReminderAlerts,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = strings.earlyReminderAlerts,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.reminderMinutesBefore,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Squiggly divider line between title/description and timer reminder pills
                        WavyDivider(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            waveLength = 36f,
                            amplitude = 8f,
                            thickness = 5f
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3-pill timer notification reminder options matching reference screenshot
                        TimerRemindersSelectorRow(
                            reminders = timerReminders,
                            onAdjust = { index, delta ->
                                if (viewModel.adjustReminderMinutes(index, delta)) {
                                    HapticHelper.performTimerDigitChangeHaptic(context)
                                }
                            },
                            selectedIndex = selectedReminderIndex,
                            onSelectedIndexChange = { selectedReminderIndex = it },
                            onSelectedPillBoundsChanged = { activeReminderBounds = it }
                        )
                    }
                }
            }
        }

        item {
            WavyDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                waveLength = 36f,
                amplitude = 9f,
                thickness = 6f
            )
        }

        // ==========================================
        // 5. WEAR OS COMPANION / ΣΥΝΟΔΟΣ WEAR OS
        // ==========================================
        item {
            val arrowRotation by animateFloatAsState(
                targetValue = if (isCrownOptionsExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 250),
                label = "arrow_rotation"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wear_os_companion_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isCrownOptionsExpanded = !isCrownOptionsExpanded
                                viewModel.triggerWearSync()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Watch,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = strings.wearOsCompanionTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.wearOsCompanionDesc,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isCrownOptionsExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(arrowRotation)
                        )
                    }

                    AnimatedVisibility(
                        visible = isCrownOptionsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(14.dp))

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Crown orientation option header
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = strings.wearOsCrownOrientationTitle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = strings.wearOsCrownOrientationDesc,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Crown Left vs Crown Right selection chips matching ThemeOptionChip UI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThemeOptionChip(
                                    title = strings.wearOsCrownRight,
                                    icon = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    isSelected = isCrownOnRight,
                                    onClick = { viewModel.setWearCrownOnRight(true) },
                                    modifier = Modifier.weight(1f)
                                )
                                ThemeOptionChip(
                                    title = strings.wearOsCrownLeft,
                                    icon = Icons.AutoMirrored.Filled.ArrowBackIos,
                                    isSelected = !isCrownOnRight,
                                    onClick = { viewModel.setWearCrownOnRight(false) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Note stating that APK can be accessed from About page
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.wearOsApkDirectAboutNote,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            WavyDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                waveLength = 36f,
                amplitude = 9f,
                thickness = 6f
            )
        }

        // ==========================================
        // 6. ABOUT
        // ==========================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab(AppTab.ABOUT) }
                    .testTag("about_settings_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = strings.aboutSection,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
            }
        }
    }
    }
}
}

@Composable
private fun ThemeOptionChip(
    title: String,
    icon: ImageVector? = null,
    iconContent: (@Composable (tint: Color) -> Unit)? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .height(68.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (iconContent != null) {
                iconContent(contentColor)
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
