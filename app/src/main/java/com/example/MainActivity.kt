package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.absoluteValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.components.AnimatedNavIcon
import com.example.ui.components.ParkingAlarmAlertDialog
import com.example.ui.components.ParkingTimerDialog
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.BluetoothSettingsScreen
import com.example.ui.screens.DeveloperOptionsScreen
import com.example.ui.screens.CompassRadarScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.ParkingViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ParkingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureHighRefreshRate()

        com.example.util.UpdateManager.scheduleDailyUpdateCheck(this)
        com.example.util.UpdateManager.checkForUpdates(this, isAutomatic = true, language = viewModel.appLanguage.value)

        handleIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val oledMode by viewModel.oledMode.collectAsStateWithLifecycle()
            val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
            val autoSunTheme by viewModel.autoSunTheme.collectAsStateWithLifecycle()
            val isDaytime by viewModel.isDaytime.collectAsStateWithLifecycle()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val strings = remember(appLanguage) { getAppStrings(appLanguage) }

            CompositionLocalProvider(LocalAppStrings provides strings) {
                MyApplicationTheme(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    oledMode = oledMode,
                    autoSunTheme = autoSunTheme,
                    isDaytime = isDaytime
                ) {
                    PixelParkingApp(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        configureHighRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            configureHighRefreshRate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        configureHighRefreshRate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun configureHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = display
                val modes = display?.supportedModes ?: emptyArray()
                val highestRefreshMode = modes.maxByOrNull { it.refreshRate }
                val maxRate = highestRefreshMode?.refreshRate ?: 120f

                val params = window.attributes
                if (highestRefreshMode != null && maxRate > 60f) {
                    params.preferredDisplayModeId = highestRefreshMode.modeId
                }
                params.preferredRefreshRate = maxRate.coerceAtLeast(120f)
                window.attributes = params
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                val modes = display?.supportedModes ?: emptyArray()
                val highestRefreshMode = modes.maxByOrNull { it.refreshRate }
                if (highestRefreshMode != null && highestRefreshMode.refreshRate > 60f) {
                    val params = window.attributes
                    params.preferredDisplayModeId = highestRefreshMode.modeId
                    params.preferredRefreshRate = highestRefreshMode.refreshRate
                    window.attributes = params
                }
            }
        } catch (_: Exception) {}
    }

    private fun handleIntent(intent: Intent?) {
        val action = intent?.getStringExtra("action")
        if (action == "open_radar") {
            viewModel.selectTab(AppTab.COMPASS_RADAR)
        }
        
        if (intent?.action == android.service.quicksettings.TileService.ACTION_QS_TILE_PREFERENCES) {
            viewModel.handleQsTileLongPress()
        }
    }
}

@Composable
fun PixelParkingApp(viewModel: ParkingViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val activeSpot by viewModel.activeSpot.collectAsStateWithLifecycle()
    val allSpots by viewModel.allSpots.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val compassState by viewModel.compassState.collectAsStateWithLifecycle()
    val telemetry by viewModel.navigationTelemetry.collectAsStateWithLifecycle()
    val showTimerDialog by viewModel.showTimerDialog.collectAsStateWithLifecycle()
    val isAlarmActive by viewModel.isAlarmActive.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val mainTabs = remember {
        listOf(
            AppTab.DASHBOARD,
            AppTab.COMPASS_RADAR,
            AppTab.HISTORY,
            AppTab.SETTINGS
        )
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { mainTabs.size }
    )

    // Handle system device back button to return to previous page/tab instead of exiting app
    BackHandler(enabled = viewModel.canNavigateBack || selectedTab != AppTab.DASHBOARD) {
        viewModel.navigateBack()
    }

    var isNavBarVisible by rememberSaveable { mutableStateOf(true) }
    var waypointInteractionCounter by remember { mutableLongStateOf(0L) }

    val isWaypointTabActive by remember {
        derivedStateOf {
            if (pagerState.isScrollInProgress) {
                mainTabs.getOrNull(pagerState.targetPage) == AppTab.COMPASS_RADAR
            } else {
                mainTabs.getOrNull(pagerState.currentPage) == AppTab.COMPASS_RADAR
            }
        }
    }

    fun registerWaypointInteraction() {
        isNavBarVisible = true
        waypointInteractionCounter++
    }

    // Auto-hide navigation bar after 3 seconds of the user not clicking anything while in Waypoint tab
    LaunchedEffect(isWaypointTabActive, waypointInteractionCounter) {
        if (isWaypointTabActive) {
            isNavBarVisible = true
            delay(3000L)
            isNavBarVisible = false
        } else {
            isNavBarVisible = true
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isWaypointTabActive) {
                    registerWaypointInteraction()
                    return Offset.Zero
                }
                val delta = available.y
                // User scrolls downwards (content moves up, negative delta) -> retract downwards smoothly
                if (delta < -18f && isNavBarVisible) {
                    isNavBarVisible = false
                }
                // User scrolls upwards (content moves down, positive delta) -> reappear smoothly
                else if (delta > 18f && !isNavBarVisible) {
                    isNavBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val pagerFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapPositionalThreshold = 0.50f,
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )

    // Sync pager when selectedTab is changed programmatically
    LaunchedEffect(selectedTab) {
        val targetIndex = mainTabs.indexOf(selectedTab)
        if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
            pagerState.animateScrollToPage(
                page = targetIndex,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Sync ViewModel selectedTab ONLY when user swipe gesture has completely settled
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { settledIndex ->
                isNavBarVisible = true
                val settledTab = mainTabs.getOrNull(settledIndex)
                if (settledTab == AppTab.COMPASS_RADAR) {
                    registerWaypointInteraction()
                }
                if (settledTab != null && selectedTab != settledTab && selectedTab != AppTab.BLUETOOTH_AUTO) {
                    viewModel.selectTab(settledTab)
                }
            }
    }

    // Request Required Permissions on launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshCurrentLocation()
        viewModel.syncSystemPairedDevices()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val backgroundBlur by animateDpAsState(
        targetValue = if (selectedTab == AppTab.BLUETOOTH_AUTO || selectedTab == AppTab.ABOUT || showTimerDialog) 20.dp else 0.dp,
        label = "bt_bg_blur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backgroundBlur > 0.5.dp) Modifier.blur(backgroundBlur) else Modifier
                )
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { mainTabs[it].name },
                beyondViewportPageCount = 1,
                flingBehavior = pagerFlingBehavior
            ) { pageIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageOffset = (
                                (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                            ).absoluteValue
                            if (pageOffset > 0.005f) {
                                val pageScale = (1f - 0.035f * pageOffset).coerceIn(0.93f, 1f)
                                val pageAlpha = (1f - 0.35f * pageOffset).coerceIn(0.65f, 1f)
                                scaleX = pageScale
                                scaleY = pageScale
                                alpha = pageAlpha
                            } else {
                                scaleX = 1f
                                scaleY = 1f
                                alpha = 1f
                            }
                        }
                ) {
                    when (mainTabs[pageIndex]) {
                        AppTab.DASHBOARD -> {
                            HomeScreen(
                                viewModel = viewModel,
                                activeSpot = activeSpot,
                                recentSpots = allSpots,
                                telemetry = telemetry,
                                onNavigateTab = { viewModel.selectTab(it) }
                            )
                        }

                        AppTab.COMPASS_RADAR -> {
                            CompassRadarScreen(
                                viewModel = viewModel,
                                activeSpot = activeSpot,
                                telemetry = telemetry,
                                compassState = compassState,
                                isNavBarVisible = isNavBarVisible,
                                onBack = { viewModel.selectTab(AppTab.DASHBOARD) },
                                onUserInteraction = { registerWaypointInteraction() }
                            )
                        }

                        AppTab.HISTORY -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                spots = allSpots
                            )
                        }

                        AppTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateTab = { viewModel.selectTab(it) }
                            )
                        }

                        else -> Unit
                    }
                }
            }

            val currentNavTab by remember {
                derivedStateOf {
                    if (pagerState.isScrollInProgress) {
                        mainTabs.getOrElse(pagerState.targetPage) { selectedTab }
                    } else {
                        mainTabs.getOrElse(pagerState.currentPage) { selectedTab }
                    }
                }
            }

            AnimatedVisibility(
                visible = isNavBarVisible,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { fullHeight -> fullHeight + 150 } + fadeIn(
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { fullHeight -> fullHeight + 150 } + fadeOut(
                    animationSpec = tween(180)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PixelFloatingBottomNavBar(
                    selectedTab = currentNavTab,
                    onSelectTab = { tab ->
                        registerWaypointInteraction()
                        viewModel.selectTab(tab)
                    },
                    hasActiveSpot = activeSpot != null,
                    modifier = Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) {
                                    registerWaypointInteraction()
                                }
                            }
                        }
                    }
                )
            }
        }

        if (selectedTab == AppTab.BLUETOOTH_AUTO) {
            BluetoothSettingsScreen(
                viewModel = viewModel,
                devices = allDevices,
                onBack = { viewModel.selectTab(AppTab.DASHBOARD) }
            )
        }

        if (selectedTab == AppTab.DEVELOPER_OPTIONS) {
            DeveloperOptionsScreen(
                viewModel = viewModel,
                onBack = { viewModel.selectTab(AppTab.DASHBOARD) }
            )
        }

        if (selectedTab == AppTab.ABOUT) {
            AboutScreen(
                viewModel = viewModel,
                onBack = { viewModel.selectTab(AppTab.SETTINGS) }
            )
        }

        if (showTimerDialog) {
            ParkingTimerDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeParkingTimer() }
            )
        }

        if (isAlarmActive) {
            ParkingAlarmAlertDialog(
                onDismiss = { viewModel.dismissTimerAlarm() },
                onSnooze = { viewModel.snoozeTimerAlarm(5) }
            )
        }
    }
    }

data class NavItem(
    val tab: AppTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Realistic drop shadow for the floating navigation components.
 * - Hardware-accelerated Gaussian penumbra with zero rough, stepped, or sharp edges.
 * - Directionally cast downwards below the navigation bar with subtle, natural side penumbra.
 * - Layered ambient and spot occlusion for a noticeably darker, deeper, and grounded presence.
 * - Active in both light mode and dark mode.
 */
private fun Modifier.navBarRealisticShadow(
    isDark: Boolean,
    elevation: Dp = 12.dp
): Modifier = this
    .shadow(
        elevation = 5.dp,
        shape = CircleShape,
        spotColor = if (isDark) Color.Black else Color.Black.copy(alpha = 0.88f),
        ambientColor = if (isDark) Color.Black.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.50f),
        clip = false
    )
    .shadow(
        elevation = elevation,
        shape = CircleShape,
        spotColor = if (isDark) Color.Black else Color.Black.copy(alpha = 0.95f),
        ambientColor = if (isDark) Color.Black.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.55f),
        clip = false
    )

@Composable
fun PixelFloatingBottomNavBar(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    hasActiveSpot: Boolean,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val navItems = listOf(
        NavItem(AppTab.DASHBOARD, strings.tabHome, Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar, "nav_dashboard"),
        NavItem(AppTab.COMPASS_RADAR, strings.tabRadar, Icons.Filled.NearMe, Icons.Outlined.NearMe, "nav_waypoint"),
        NavItem(AppTab.HISTORY, strings.tabHistory, Icons.Filled.History, Icons.Outlined.History, "nav_history")
    )

    val isSettingsSelected = selectedTab == AppTab.SETTINGS
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isOled = isDark && MaterialTheme.colorScheme.background == Color(0xFF000000)

    val animTriggerDashboard = remember { mutableIntStateOf(0) }
    val animTriggerRadar = remember { mutableIntStateOf(0) }
    val animTriggerHistory = remember { mutableIntStateOf(0) }
    val animTriggerSettings = remember { mutableIntStateOf(0) }

    var previousTab by remember { mutableStateOf<AppTab?>(null) }
    LaunchedEffect(selectedTab) {
        if (previousTab != null && previousTab != selectedTab) {
            when (selectedTab) {
                AppTab.DASHBOARD -> animTriggerDashboard.intValue++
                AppTab.COMPASS_RADAR -> animTriggerRadar.intValue++
                AppTab.HISTORY -> animTriggerHistory.intValue++
                AppTab.SETTINGS -> animTriggerSettings.intValue++
                else -> {}
            }
        }
        previousTab = selectedTab
    }

    val navBarContainerBg = if (isOled) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceContainerHigh
    val navSelectedBg = MaterialTheme.colorScheme.primary
    val navSelectedContent = if (!isDark) Color(0xFF000000) else MaterialTheme.colorScheme.onPrimary
    val navUnselectedContent = MaterialTheme.colorScheme.onSurfaceVariant
    val navBorder = if (isDark) {
        if (isOled) BorderStroke(1.dp, Color(0xFF242424)) else null
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 12.dp, top = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main Pill Capsule Container: Opaque background with smooth realistic drop shadow in light & dark mode
            Surface(
                shape = CircleShape,
                color = navBarContainerBg,
                border = navBorder,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .navBarRealisticShadow(isDark = isDark)
                    .wrapContentWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { item ->
                        val isSelected = selectedTab == item.tab
                        val interactionSource = remember { MutableInteractionSource() }

                        val animatedBgColor by animateColorAsState(
                            targetValue = if (isSelected) navSelectedBg else Color.Transparent,
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                            label = "navItemBg_${item.label}"
                        )
                        val animatedContentColor by animateColorAsState(
                            targetValue = if (isSelected) navSelectedContent else navUnselectedContent,
                            animationSpec = tween(220),
                            label = "navItemContent_${item.label}"
                        )
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.94f,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
                            label = "navItemScale_${item.label}"
                        )

                        Surface(
                            shape = CircleShape,
                            color = animatedBgColor,
                            modifier = Modifier
                                .height(38.dp)
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = true, color = if (isSelected) navSelectedContent else navSelectedBg)
                                ) {
                                    when (item.tab) {
                                        AppTab.DASHBOARD -> animTriggerDashboard.intValue++
                                        AppTab.COMPASS_RADAR -> animTriggerRadar.intValue++
                                        AppTab.HISTORY -> animTriggerHistory.intValue++
                                        else -> {}
                                    }
                                    onSelectTab(item.tab)
                                }
                                .testTag(item.testTag)
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = 400f
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isSelected) 14.dp else 10.dp,
                                    vertical = 6.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val itemTriggerCount = when (item.tab) {
                                    AppTab.DASHBOARD -> animTriggerDashboard.intValue
                                    AppTab.COMPASS_RADAR -> animTriggerRadar.intValue
                                    AppTab.HISTORY -> animTriggerHistory.intValue
                                    else -> 0
                                }

                                if (item.tab == AppTab.COMPASS_RADAR && hasActiveSpot && !isSelected) {
                                    BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) }) {
                                        AnimatedNavIcon(
                                            tab = item.tab,
                                            icon = item.unselectedIcon,
                                            contentDescription = item.label,
                                            tint = animatedContentColor,
                                            triggerCount = itemTriggerCount,
                                            size = 20.dp
                                        )
                                    }
                                } else {
                                    AnimatedNavIcon(
                                        tab = item.tab,
                                        icon = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        tint = animatedContentColor,
                                        triggerCount = itemTriggerCount,
                                        size = 20.dp
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(tween(160, delayMillis = 30)) + expandHorizontally(
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                                    ),
                                    exit = fadeOut(tween(100)) + shrinkHorizontally(
                                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = animatedContentColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Standalone Detached Circular Settings Button: Animated background, icon tint and rotation
            val settingsBg by animateColorAsState(
                targetValue = if (isSettingsSelected) navSelectedBg else navBarContainerBg,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
                label = "settingsBgColor"
            )
            val settingsIconTint by animateColorAsState(
                targetValue = if (isSettingsSelected) navSelectedContent else navUnselectedContent,
                animationSpec = tween(220),
                label = "settingsIconTint"
            )
            Surface(
                shape = CircleShape,
                color = settingsBg,
                border = if (isSettingsSelected) null else navBorder,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .navBarRealisticShadow(isDark = isDark)
                    .size(48.dp)
                    .clickable {
                        animTriggerSettings.intValue++
                        onSelectTab(AppTab.SETTINGS)
                    }
                    .testTag("nav_settings")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedNavIcon(
                        tab = AppTab.SETTINGS,
                        icon = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = settingsIconTint,
                        triggerCount = animTriggerSettings.intValue,
                        size = 20.dp
                    )
                }
            }
        }
    }
}

