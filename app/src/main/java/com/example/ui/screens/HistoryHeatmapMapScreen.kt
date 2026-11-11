package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.entity.ParkingSpot
import com.example.data.entity.isBluetoothEntry
import com.example.data.entity.isCustomFarAwayEntry
import com.example.data.entity.isGeofenceEntry
import com.example.data.entity.isUndergroundFallback
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.viewmodel.ParkingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

private data class TileViewportState(
    val tileZoom: Int,
    val centerCol: Int,
    val centerRow: Int,
    val centerLat: Double,
    val centerLng: Double,
    val exactZoom: Float
)

/**
 * Google Photos-style interactive dark map showing all saved parking locations on the History tab.
 * Engineered for zero-lag 120fps gestures and instant high-definition tile loading.
 */
@Composable
fun HistoryHeatmapMapScreen(
    viewModel: ParkingViewModel,
    spots: List<ParkingSpot>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isGreek = appLanguage == AppLanguage.GREEK
    val currentLocation by viewModel.currentLocation.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val densityForInit = LocalDensity.current
    val maxBarPxForInit = with(densityForInit) { 110.dp.toPx() }.coerceAtLeast(100f)

    // Upon opening the map each time, center on where the user is located at a height of 10 km
    val initialCamera = remember {
        val loc = currentLocation
            ?: com.example.sensor.LocationHelper.lastKnownLocation
            ?: com.example.sensor.LocationHelper.getPersistedLocation(context)
        val initLat = loc?.latitude?.takeIf { it != 0.0 && it.isFinite() }
            ?: spots.firstOrNull { it.isActive }?.latitude
            ?: spots.maxByOrNull { it.timestamp }?.latitude
            ?: 39.6650
        val initLng = loc?.longitude?.takeIf { it != 0.0 && it.isFinite() }
            ?: spots.firstOrNull { it.isActive }?.longitude
            ?: spots.maxByOrNull { it.timestamp }?.longitude
            ?: 20.8537
        val targetMetersPerPixel10km = 13000.0 / maxBarPxForInit
        val cosLat = cos(Math.toRadians(initLat.coerceIn(-80.0, 80.0))).coerceAtLeast(0.1)
        val zoom10km = (ln(156543.03392 * cosLat / targetMetersPerPixel10km) / ln(2.0))
            .toFloat()
            .coerceIn(9.5f, 13.5f)
        Triple(initLat, initLng, zoom10km)
    }

    var hasUserInteractedWithMap by remember { mutableStateOf(false) }
    var hasCenteredOnLiveLocation by remember {
        mutableStateOf(currentLocation != null && currentLocation?.latitude != 0.0)
    }

    // State objects read ONLY inside DrawScope (Canvas) and snapshotFlow coroutines
    // so pinch-to-zoom and panning NEVER recompose the Composable hierarchy!
    val centerLatState = remember { mutableDoubleStateOf(initialCamera.first) }
    val centerLngState = remember { mutableDoubleStateOf(initialCamera.second) }
    val zoomState = remember { mutableFloatStateOf(initialCamera.third) }
    val tileLoadGenerationState = remember { mutableIntStateOf(0) }

    LaunchedEffect(currentLocation) {
        val loc = currentLocation
        if (!hasCenteredOnLiveLocation && !hasUserInteractedWithMap &&
            loc != null && loc.latitude != 0.0 && loc.latitude.isFinite() && loc.longitude.isFinite()
        ) {
            hasCenteredOnLiveLocation = true
            val targetMetersPerPixel10km = 13000.0 / maxBarPxForInit
            val cosLat = cos(Math.toRadians(loc.latitude.coerceIn(-80.0, 80.0))).coerceAtLeast(0.1)
            val zoom10km = (ln(156543.03392 * cosLat / targetMetersPerPixel10km) / ln(2.0))
                .toFloat()
                .coerceIn(9.5f, 13.5f)
            centerLatState.doubleValue = loc.latitude
            centerLngState.doubleValue = loc.longitude
            zoomState.floatValue = zoom10km
        }
    }

    var showPinsOverlay by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var selectedSpot by remember { mutableStateOf<ParkingSpot?>(null) }
    var isSheetExpanded by remember { mutableStateOf(false) }

    val currentSpots by rememberUpdatedState(spots)

    // Dark map color palette matching the reference screenshots
    val waterColor = Color(0xFF142338)
    val landColor = Color(0xFF232936)
    val borderColor = Color(0xFF3A4252)
    val roadMinorColor = Color(0xFF48566E)
    val roadMajorColor = Color(0xFF8E6B48)
    val labelCityColor = Color(0xFFD99B66)
    val labelCountryColor = Color(0xFF9C8974)
    val labelWaterColor = Color(0xFF5A7694)
    val controlBgColor = Color(0xFF1E1A16)
    val controlIconColor = Color(0xFFF3DFC1)
    val sheetBgColor = Color(0xFF1A1410)
    val sheetDividerColor = Color(0xFF332A22)

    var cameraAnimJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val isCameraAnimatingState = remember { mutableStateOf(false) }

    // Smooth 120fps camera flight helper with scale-integrated Mercator trajectory & parabolic zoom arc
    fun animateCameraTo(targetLat: Double, targetLng: Double, targetZoom: Float, isMyLocationFlight: Boolean = false) {
        if (!targetLat.isFinite() || !targetLng.isFinite() || !targetZoom.isFinite()) return

        // Pre-schedule high-definition raster tiles and vector HD cells for the target destination immediately!
        MapTileEngine.prewarmTargetDestination(
            cacheDir = context.cacheDir,
            targetLat = targetLat,
            targetLng = targetLng,
            targetZoom = targetZoom
        )
        if (targetZoom >= 14.0f) {
            HighZoomVectorEngine.scheduleViewport(
                cacheDir = context.cacheDir,
                centerLat = targetLat,
                centerLng = targetLng,
                zoom = targetZoom,
                onCellLoaded = { MapTileEngine.requestRedraw() }
            )
        }

        cameraAnimJob?.cancel()
        cameraAnimJob = coroutineScope.launch {
            val startLat = centerLatState.doubleValue.let { if (it.isFinite()) it else targetLat }
            val startLng = centerLngState.doubleValue.let { if (it.isFinite()) it else targetLng }
            val startZoom = zoomState.floatValue.let { if (it.isFinite()) it else targetZoom }

            val distMeters = com.example.sensor.LocationHelper.calculateDistanceMeters(startLat, startLng, targetLat, targetLng)
            val absZoomDelta = abs(targetZoom - startZoom)
            if (distMeters < 0.15f && absZoomDelta < 0.008f) {
                centerLatState.doubleValue = targetLat
                centerLngState.doubleValue = targetLng
                zoomState.floatValue = targetZoom
                return@launch
            }

            // Calculate how many screen widths the pan covers at the higher zoom level
            val maxZ = max(startZoom, targetZoom).toDouble()
            val metersPerPxAtMaxZ = (156543.03392 * cos(Math.toRadians((startLat + targetLat) * 0.5)).coerceAtLeast(0.1)) / 2.0.pow(maxZ)
            val pixelTravelAtMaxZ = distMeters / metersPerPxAtMaxZ.coerceAtLeast(0.01)

            // When panning across a large distance while zoomed in, dip zoom slightly mid-flight for a graceful arc
            val arcDipZoom = if (absZoomDelta < 2.2f && distMeters > 120f && startZoom >= 15.0f && targetZoom >= 15.0f) {
                (ln((pixelTravelAtMaxZ / 420.0).coerceAtLeast(1.0)) / ln(2.0)).toFloat().coerceIn(0f, 3.2f)
            } else {
                0f
            }

            val baseDuration = if (isMyLocationFlight) {
                780f + absZoomDelta * 58f + (ln((distMeters + 1f).toDouble()).toFloat() * 28f) + arcDipZoom * 95f
            } else {
                680f + absZoomDelta * 52f + (ln((distMeters + 1f).toDouble()).toFloat() * 24f) + arcDipZoom * 80f
            }
            val durationMs = baseDuration.toInt().coerceIn(if (isMyLocationFlight) 760 else 640, 1280)
            val durationNanos = durationMs * 1_000_000L
            val zoomDiff = (targetZoom - startZoom).toDouble()

            isCameraAnimatingState.value = true
            try {
                var startNanos = 0L
                var lastPrewarmStep = -1
                while (true) {
                    val frameNanos = androidx.compose.runtime.withFrameNanos { it }
                    if (startNanos == 0L) startNanos = frameNanos
                    val elapsed = (frameNanos - startNanos).coerceAtLeast(0L)
                    val rawLinear = (elapsed.toDouble() / durationNanos.toDouble()).coerceIn(0.0, 1.0)

                    // C2-continuous quintic smoothstep (6t^5 - 15t^4 + 10t^3): zero velocity & zero acceleration at both ends
                    val u = rawLinear * rawLinear * rawLinear * (rawLinear * (rawLinear * 6.0 - 15.0) + 10.0)

                    // Scale-coupled pan progression so the target glides smoothly without end-of-zoom whip
                    val panT = if (abs(zoomDiff) > 0.35 && distMeters > 2.0f) {
                        val effectiveZDiff = zoomDiff * 0.72
                        val num = 1.0 - 2.0.pow(-effectiveZDiff * u)
                        val den = 1.0 - 2.0.pow(-effectiveZDiff)
                        val scaleCoupled = if (abs(den) > 1e-5) (num / den).coerceIn(0.0, 1.0) else u
                        (scaleCoupled * 0.78 + u * 0.22).coerceIn(0.0, 1.0)
                    } else {
                        u
                    }

                    val currentArcOffset = if (arcDipZoom > 0.05f) {
                        val bell = kotlin.math.sin(u * Math.PI).toFloat()
                        -arcDipZoom * bell * bell
                    } else {
                        0f
                    }

                    val interpLat = startLat + (targetLat - startLat) * panT
                    val interpLng = startLng + (targetLng - startLng) * panT
                    val interpZoom = (startZoom + (targetZoom - startZoom) * u.toFloat() + currentArcOffset).coerceIn(2f, 21.5f)

                    centerLatState.doubleValue = interpLat
                    centerLngState.doubleValue = interpLng
                    zoomState.floatValue = interpZoom

                    // Periodically schedule intermediate tiles along the flight path so multi-zoom flights never hitch
                    val stepIdx = (rawLinear * 4.0).toInt()
                    if (stepIdx > lastPrewarmStep && stepIdx in 1..3) {
                        lastPrewarmStep = stepIdx
                        val stepTileZ = computeEffectiveTileZoom(interpZoom)
                        val stepN = 1 shl stepTileZ
                        val stepCol = floor(lngToTileX(interpLng, stepTileZ)).toInt().coerceIn(0, stepN - 1)
                        val stepRow = floor(latToTileY(interpLat, stepTileZ)).toInt().coerceIn(0, stepN - 1)
                        MapTileEngine.scheduleViewport(
                            cacheDir = context.cacheDir,
                            tileZoom = stepTileZ,
                            exactZoom = interpZoom,
                            centerCol = stepCol,
                            centerRow = stepRow,
                            centerLat = interpLat,
                            centerLng = interpLng,
                            screenWidthPx = 1080f,
                            screenHeightPx = 1920f,
                            spots = currentSpots
                        )
                    }

                    if (rawLinear >= 1.0) break
                }
                centerLatState.doubleValue = targetLat
                centerLngState.doubleValue = targetLng
                zoomState.floatValue = targetZoom
            } finally {
                isCameraAnimatingState.value = false
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val resumeGenerationState = remember { mutableIntStateOf(0) }

    // Activate map engines synchronously while HistoryHeatmapMapScreen is in composition
    remember {
        MapTileEngine.onMapOpened()
        HighZoomVectorEngine.onMapOpened()
        true
    }

    // Coalesce background tile arrival events into at most 1 redraw tick per 16ms frame
    LaunchedEffect(Unit) {
        MapTileEngine.tileInvalidationFlow.collect {
            tileLoadGenerationState.intValue++
        }
    }

    DisposableEffect(lifecycleOwner) {
        MapTileEngine.onMapOpened()
        HighZoomVectorEngine.onMapOpened()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    MapTileEngine.onMapOpened()
                    HighZoomVectorEngine.onMapOpened()
                    resumeGenerationState.intValue++
                }
                Lifecycle.Event.ON_STOP -> {
                    MapTileEngine.onMapClosed(appInBackground = true)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            MapTileEngine.onMapClosed(appInBackground = false)
        }
    }

    // Clean up legacy tile cache directories once and prewarm existing binary vector HD cells
    LaunchedEffect(spots.size, currentLocation != null) {
        MapTileEngine.cleanupLegacyCacheOnce(context.cacheDir)
        HighZoomVectorEngine.prewarmSpots(
            cacheDir = context.cacheDir,
            spots = spots,
            userLat = currentLocation?.latitude,
            userLng = currentLocation?.longitude,
            onCellLoaded = { MapTileEngine.requestRedraw() }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(waterColor)
            .testTag("history_heatmap_map_screen")
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val screenHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        val collapsedSheetHeight = 128.dp
        val expandedSheetHeight = (maxHeight * 0.58f).coerceAtLeast(320.dp)
        val sheetHeight by animateDpAsState(
            targetValue = if (isSheetExpanded) expandedSheetHeight else collapsedSheetHeight,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "history_map_sheet_height"
        )

        val visibleMapHeightPx = (screenHeightPx - with(density) { sheetHeight.toPx() }).coerceAtLeast(120f)
        val mapCenterX = screenWidthPx / 2f
        val mapCenterY = screenHeightPx / 2f
        val currentMapCenterX by rememberUpdatedState(mapCenterX)
        val currentMapCenterY by rememberUpdatedState(mapCenterY)

        // Throttled off-main-thread calculation of visible spots for the bottom sheet
        // Uses snapshotFlow so camera gestures NEVER recompose BoxWithConstraints!
        var visibleSpots by remember(spots) {
            val initLat = centerLatState.doubleValue
            val initLng = centerLngState.doubleValue
            val initZoom = zoomState.floatValue
            mutableStateOf(
                spots.filter { spot ->
                    val pt = latLngToScreenPx(
                        lat = spot.latitude,
                        lng = spot.longitude,
                        centerLat = initLat,
                        centerLng = initLng,
                        zoom = initZoom,
                        viewportCenterX = mapCenterX,
                        viewportCenterY = mapCenterY
                    )
                    pt.x in -24f..(screenWidthPx + 24f) && pt.y in -24f..(visibleMapHeightPx + 24f)
                }.sortedByDescending { it.timestamp }
            )
        }

        LaunchedEffect(spots, screenWidthPx, visibleMapHeightPx) {
            snapshotFlow {
                Triple(centerLatState.doubleValue, centerLngState.doubleValue, zoomState.floatValue)
            }.collectLatest { (snapLat, snapLng, snapZoom) ->
                delay(90)
                val filtered = withContext(Dispatchers.Default) {
                    spots.filter { spot ->
                        val pt = latLngToScreenPx(
                            lat = spot.latitude,
                            lng = spot.longitude,
                            centerLat = snapLat,
                            centerLng = snapLng,
                            zoom = snapZoom,
                            viewportCenterX = mapCenterX,
                            viewportCenterY = mapCenterY
                        )
                        pt.x in -24f..(screenWidthPx + 24f) && pt.y in -24f..(visibleMapHeightPx + 24f)
                    }.sortedByDescending { it.timestamp }
                }
                visibleSpots = filtered
            }
        }

        // Instant zero-debounce tile & vector HD scheduling via snapshotFlow without recomposing UI
        LaunchedEffect(screenWidthPx, screenHeightPx, spots, resumeGenerationState.intValue) {
            MapTileEngine.onMapOpened()
            HighZoomVectorEngine.onMapOpened()
            snapshotFlow {
                val animating = isCameraAnimatingState.value
                val z = zoomState.floatValue
                val tZoom = computeEffectiveTileZoom(z)
                val cLat = centerLatState.doubleValue
                val cLng = centerLngState.doubleValue
                val cCol = floor(lngToTileX(cLng, tZoom)).toInt()
                val cRow = floor(latToTileY(cLat, tZoom)).toInt()
                Pair(
                    animating,
                    TileViewportState(
                        tileZoom = tZoom,
                        centerCol = cCol,
                        centerRow = cRow,
                        centerLat = cLat,
                        centerLng = cLng,
                        exactZoom = z
                    )
                )
            }.distinctUntilChanged { pairA, pairB ->
                val a = pairA.second
                val b = pairB.second
                pairA.first == pairB.first &&
                    a.tileZoom == b.tileZoom &&
                    a.centerCol == b.centerCol &&
                    a.centerRow == b.centerRow &&
                    (a.exactZoom * 12f).toInt() == (b.exactZoom * 12f).toInt() &&
                    (a.exactZoom < 13.2f) == (b.exactZoom < 13.2f) &&
                    (a.centerLat * 250.0).toInt() == (b.centerLat * 250.0).toInt() &&
                    (a.centerLng * 250.0).toInt() == (b.centerLng * 250.0).toInt()
            }.collect { (animating, vp) ->
                if (animating) return@collect
                MapTileEngine.scheduleViewport(
                    cacheDir = context.cacheDir,
                    tileZoom = vp.tileZoom,
                    exactZoom = vp.exactZoom,
                    centerCol = vp.centerCol,
                    centerRow = vp.centerRow,
                    centerLat = vp.centerLat,
                    centerLng = vp.centerLng,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    spots = currentSpots
                )
                if (vp.exactZoom >= 13.2f) {
                    HighZoomVectorEngine.scheduleViewport(
                        cacheDir = context.cacheDir,
                        centerLat = vp.centerLat,
                        centerLng = vp.centerLng,
                        zoom = vp.exactZoom,
                        onCellLoaded = { MapTileEngine.requestRedraw() }
                    )
                }
            }
        }

        // Interactive Map Canvas (supports pan, pinch-to-zoom, double-tap/tap on spots)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        hasUserInteractedWithMap = true
                        if (cameraAnimJob?.isActive == true) {
                            cameraAnimJob?.cancel()
                            isCameraAnimatingState.value = false
                        }
                        if (!gestureZoom.isFinite() || gestureZoom <= 0f ||
                            !centroid.x.isFinite() || !centroid.y.isFinite() ||
                            !pan.x.isFinite() || !pan.y.isFinite()
                        ) {
                            return@detectTransformGestures
                        }
                        val safeGestureZoom = gestureZoom.coerceIn(0.25f, 4.0f)
                        val oldZoom = zoomState.floatValue.let { if (it.isFinite()) it else 12.5f }
                        val zoomDelta = (ln(safeGestureZoom.toDouble()) / ln(2.0)).toFloat()
                        if (!zoomDelta.isFinite()) return@detectTransformGestures
                        val newZoom = (oldZoom + zoomDelta).coerceIn(2.2f, 22.5f)

                        val cLat = centerLatState.doubleValue.let { if (it.isFinite()) it else 39.6650 }
                        val cLng = centerLngState.doubleValue.let { if (it.isFinite()) it else 20.8537 }
                        val mCx = currentMapCenterX
                        val mCy = currentMapCenterY

                        // Keep the pinched centroid anchored while zooming and panning
                        val beforeLatLng = screenPxToLatLng(
                            screenX = centroid.x - pan.x,
                            screenY = centroid.y - pan.y,
                            centerLat = cLat,
                            centerLng = cLng,
                            zoom = oldZoom,
                            viewportCenterX = mCx,
                            viewportCenterY = mCy
                        )
                        zoomState.floatValue = newZoom
                        val afterScreen = latLngToScreenPx(
                            lat = beforeLatLng.first,
                            lng = beforeLatLng.second,
                            centerLat = cLat,
                            centerLng = cLng,
                            zoom = newZoom,
                            viewportCenterX = mCx,
                            viewportCenterY = mCy
                        )
                        val adjustedCenter = screenPxToLatLng(
                            screenX = mCx + (afterScreen.x - centroid.x),
                            screenY = mCy + (afterScreen.y - centroid.y),
                            centerLat = cLat,
                            centerLng = cLng,
                            zoom = newZoom,
                            viewportCenterX = mCx,
                            viewportCenterY = mCy
                        )
                        if (adjustedCenter.first.isFinite() && adjustedCenter.second.isFinite()) {
                            centerLatState.doubleValue = adjustedCenter.first.coerceIn(-80.0, 80.0)
                            centerLngState.doubleValue = normalizeLng(adjustedCenter.second)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            val cLat = centerLatState.doubleValue
                            val cLng = centerLngState.doubleValue
                            val cZoom = zoomState.floatValue
                            val mCx = currentMapCenterX
                            val mCy = currentMapCenterY
                            val activeSpots = currentSpots

                            // Find closest spot within 44dp of tap
                            val hitRadiusPx = 44.dp.toPx()
                            val tappedSpot = activeSpots.minByOrNull { spot ->
                                val pt = latLngToScreenPx(
                                    lat = spot.latitude,
                                    lng = spot.longitude,
                                    centerLat = cLat,
                                    centerLng = cLng,
                                    zoom = cZoom,
                                    viewportCenterX = mCx,
                                    viewportCenterY = mCy
                                )
                                val dx = pt.x - tapOffset.x
                                val dy = pt.y - tapOffset.y
                                dx * dx + dy * dy
                            }?.takeIf { spot ->
                                val pt = latLngToScreenPx(
                                    lat = spot.latitude,
                                    lng = spot.longitude,
                                    centerLat = cLat,
                                    centerLng = cLng,
                                    zoom = cZoom,
                                    viewportCenterX = mCx,
                                    viewportCenterY = mCy
                                )
                                val dx = pt.x - tapOffset.x
                                val dy = pt.y - tapOffset.y
                                (dx * dx + dy * dy) <= hitRadiusPx * hitRadiusPx
                            }

                            if (tappedSpot != null) {
                                selectedSpot = tappedSpot
                                val targetZ = max(cZoom + 2.0f, 19.0f).coerceAtMost(22.0f)
                                animateCameraTo(tappedSpot.latitude, tappedSpot.longitude, targetZ)
                            } else {
                                selectedSpot = null
                            }
                        },
                        onDoubleTap = { tapOffset ->
                            val cLat = centerLatState.doubleValue
                            val cLng = currentMapCenterX.let { centerLngState.doubleValue }
                            val cZoom = zoomState.floatValue
                            val mCx = currentMapCenterX
                            val mCy = currentMapCenterY

                            val tappedLatLng = screenPxToLatLng(
                                screenX = tapOffset.x,
                                screenY = tapOffset.y,
                                centerLat = cLat,
                                centerLng = cLng,
                                zoom = cZoom,
                                viewportCenterX = mCx,
                                viewportCenterY = mCy
                            )
                            animateCameraTo(
                                tappedLatLng.first,
                                tappedLatLng.second,
                                (cZoom + 1.6f).coerceAtMost(22.5f)
                            )
                        }
                    )
                }
                .testTag("history_heatmap_canvas")
        ) {
            // Read camera & tile states ONLY inside DrawScope so gestures run at 120fps without UI recomposition
            val drawLat = centerLatState.doubleValue
            val drawLng = centerLngState.doubleValue
            val drawZoom = zoomState.floatValue
            val gen = tileLoadGenerationState.intValue
            if (gen >= 0) {
                drawRect(color = waterColor)
            }

            // 1. Real Slippy Map Raster Tiles (512x512 Edge-Directed Super-Resolution & Smooth Anti-Aliased Dark Theme)
            val drewRealTiles = drawSlippyMapTiles(
                tileCache = MapTileEngine.memoryCache,
                centerLat = drawLat,
                centerLng = drawLng,
                zoom = drawZoom,
                viewportCenterX = mapCenterX,
                viewportCenterY = mapCenterY
            )

            // 2. Infinite-Resolution Vector HD Street/Building/POI Overlay when zoomed in (zoom >= 16.5f)
            val drewVectorHd = with(HighZoomVectorEngine) {
                drawVectorHdOverlay(
                    centerLat = drawLat,
                    centerLng = drawLng,
                    zoom = drawZoom,
                    viewportCenterX = mapCenterX,
                    viewportCenterY = mapCenterY,
                    isGreek = isGreek
                )
            }

            // 3. Offline Vector Fallback (only rendered if no real map tiles or vector HD cells are available offline)
            if (!drewRealTiles && !drewVectorHd) {
                drawOfflineVectorBasemap(
                    centerLat = drawLat,
                    centerLng = drawLng,
                    zoom = drawZoom,
                    viewportCenterX = mapCenterX,
                    viewportCenterY = mapCenterY,
                    landColor = landColor,
                    borderColor = borderColor,
                    roadMinorColor = roadMinorColor,
                    roadMajorColor = roadMajorColor,
                    labelCityColor = labelCityColor,
                    labelCountryColor = labelCountryColor,
                    labelWaterColor = labelWaterColor,
                    isGreek = isGreek
                )
            }

            // 4. Current User GPS Dot (subtle blue/white beacon if available)
            currentLocation?.let { loc ->
                val userPt = latLngToScreenPx(
                    lat = loc.latitude,
                    lng = loc.longitude,
                    centerLat = drawLat,
                    centerLng = drawLng,
                    zoom = drawZoom,
                    viewportCenterX = mapCenterX,
                    viewportCenterY = mapCenterY
                )
                if (userPt.x in -40f..(size.width + 40f) && userPt.y in -40f..(size.height + 40f)) {
                    drawCircle(
                        color = Color(0xFF4285F4).copy(alpha = 0.28f),
                        radius = 18.dp.toPx(),
                        center = userPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = userPt
                    )
                    drawCircle(
                        color = Color(0xFF4285F4),
                        radius = 5.dp.toPx(),
                        center = userPt
                    )
                }
            }

            // 5. Dynamic Google Photos Purple-Dot & Rainbow Metaball Heatmap Layer
            if (showPinsOverlay && spots.isNotEmpty()) {
                with(GooglePhotosHeatmapRenderer) {
                    drawDynamicSpotsHeatmap(
                        spots = spots,
                        centerLat = drawLat,
                        centerLng = drawLng,
                        zoom = drawZoom,
                        viewportCenterX = mapCenterX,
                        viewportCenterY = mapCenterY
                    )
                }
            }

            // 6. High-Precision Ground Resolution Scale Bar (bottom-left above sheet)
            drawHighZoomScaleBar(
                centerLat = drawLat,
                zoom = drawZoom,
                bottomY = visibleMapHeightPx - 18.dp.toPx()
            )
        }

        // TOP FLOATING CONTROLS (Back Button on Left, 3-Dot Menu Button on Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = controlBgColor,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("history_map_back_btn")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = controlIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Box {
                Surface(
                    onClick = { showMoreMenu = true },
                    shape = CircleShape,
                    color = controlBgColor,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("history_map_more_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = controlIconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    containerColor = Color(0xFF241E19),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isGreek) "Προβολή όλων των τοποθεσιών" else "Fit all saved locations",
                                color = controlIconColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FitScreen,
                                contentDescription = null,
                                tint = controlIconColor
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            val cam = computeInitialCamera(spots, currentLocation?.latitude, currentLocation?.longitude)
                            animateCameraTo(cam.first, cam.second, cam.third)
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (showPinsOverlay) {
                                    if (isGreek) "Απόκρυψη δεικτών" else "Hide pins"
                                } else {
                                    if (isGreek) "Εμφάνιση δεικτών" else "Show pins"
                                },
                                color = controlIconColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = controlIconColor
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            showPinsOverlay = !showPinsOverlay
                        }
                    )
                }
            }
        }

        // FLOATING GPS RECENTER / FIT BUTTON (Bottom-Right just above the Bottom Sheet)
        val myLocationSpinAnim = remember { Animatable(0f) }
        val myLocationScaleAnim = remember { Animatable(1f) }
        Surface(
            onClick = {
                coroutineScope.launch {
                    launch {
                        myLocationScaleAnim.snapTo(0.86f)
                        myLocationScaleAnim.animateTo(
                            targetValue = 1f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            )
                        )
                    }
                    launch {
                        myLocationSpinAnim.snapTo(0f)
                        myLocationSpinAnim.animateTo(
                            targetValue = 360f,
                            animationSpec = tween(
                                durationMillis = 680,
                                easing = androidx.compose.animation.core.CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
                            )
                        )
                    }
                }
                val loc = currentLocation
                    ?: com.example.sensor.LocationHelper.lastKnownLocation
                    ?: com.example.sensor.LocationHelper.getPersistedLocation(context)
                val targetLat = loc?.latitude?.takeIf { it != 0.0 && it.isFinite() }
                    ?: spots.firstOrNull { it.isActive }?.latitude
                    ?: spots.firstOrNull()?.latitude
                val targetLng = loc?.longitude?.takeIf { it != 0.0 && it.isFinite() }
                    ?: spots.firstOrNull { it.isActive }?.longitude
                    ?: spots.firstOrNull()?.longitude

                if (targetLat != null && targetLng != null) {
                    val maxBarPx = with(density) { 110.dp.toPx() }.coerceAtLeast(100f)
                    val targetMetersPerPixel = 65.0 / maxBarPx
                    val cosLat = cos(Math.toRadians(targetLat)).coerceAtLeast(0.1)
                    val zoom50m = (kotlin.math.ln(156543.03392 * cosLat / targetMetersPerPixel) / kotlin.math.ln(2.0))
                        .toFloat()
                        .coerceIn(16.5f, 19.5f)
                    animateCameraTo(targetLat, targetLng, zoom50m, isMyLocationFlight = true)
                } else {
                    val cam = computeInitialCamera(spots, null, null)
                    animateCameraTo(cam.first, cam.second, cam.third, isMyLocationFlight = true)
                }
            },
            shape = CircleShape,
            color = controlBgColor,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = sheetHeight + 18.dp)
                .size(50.dp)
                .testTag("history_map_my_location_btn")
        ) {
            Box(contentAlignment = Alignment.Center) {
                val scaleVal = myLocationScaleAnim.value
                val rotVal = myLocationSpinAnim.value
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = controlIconColor,
                    modifier = Modifier
                        .size((23f * scaleVal).dp)
                        .let { mod ->
                            if (rotVal > 0.1f && rotVal < 359.9f) {
                                mod.then(Modifier)
                            } else mod
                        }
                )
            }
        }

        // GOOGLE PHOTOS-STYLE BOTTOM SHEET (Shows visible spot count, date range, and grouped list when expanded)
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = sheetBgColor,
            shadowElevation = 16.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetHeight)
                .testTag("history_map_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                // Draggable Header Area (Pill Handle + Count Title + Divider + Date Range)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSheetExpanded = !isSheetExpanded }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -12f) {
                                    isSheetExpanded = true
                                } else if (dragAmount > 12f) {
                                    isSheetExpanded = false
                                }
                            }
                        }
                        .padding(top = 10.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle pill
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(4.5.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color(0xFF8E8276))
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val countText = remember(visibleSpots.size, isGreek) {
                        val formattedCount = String.format(
                            if (isGreek) Locale("el", "GR") else Locale.US,
                            "%,d",
                            visibleSpots.size
                        )
                        if (isGreek) {
                            if (visibleSpots.size == 1) "1 τοποθεσία" else "$formattedCount τοποθεσίες"
                        } else {
                            if (visibleSpots.size == 1) "1 saved location" else "$formattedCount saved locations"
                        }
                    }

                    Text(
                        text = countText,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFF6EFE8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("history_map_visible_count_text")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalDivider(
                        color = sheetDividerColor,
                        thickness = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val dateRangeSubtitle = remember(visibleSpots, isGreek) {
                        formatSpotDateRange(visibleSpots, isGreek)
                    }

                    if (!isSheetExpanded || visibleSpots.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = dateRangeSubtitle,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD6CBC0),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Expanded Spot List Grouped by Date (like the photo grid grouped by day in the screenshots)
                AnimatedVisibility(
                    visible = isSheetExpanded && visibleSpots.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val groupedByDay = remember(visibleSpots, isGreek) {
                        groupSpotsByDayLabel(visibleSpots, isGreek)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        groupedByDay.forEach { (dayLabel, daySpots) ->
                            item(key = "day_header_$dayLabel") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dayLabel,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFF6EFE8)
                                    )
                                    IconButton(
                                        onClick = {
                                            val cam = computeInitialCamera(daySpots, null, null)
                                            animateCameraTo(cam.first, cam.second, max(cam.third, 15.2f))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircleOutline,
                                            contentDescription = "Focus day locations",
                                            tint = Color(0xFFD6CBC0),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            items(daySpots, key = { it.id }) { spot ->
                                HistoryMapSpotSheetCard(
                                    spot = spot,
                                    isSelected = selectedSpot?.id == spot.id,
                                    isGreek = isGreek,
                                    onFocusOnMap = {
                                        selectedSpot = spot
                                        animateCameraTo(spot.latitude, spot.longitude, max(zoomState.floatValue, 16.0f))
                                    },
                                    onNavigateWaypoint = {
                                        viewModel.setActiveNavigationTarget(spot)
                                        onBack()
                                    },
                                    onOpenGoogleMaps = {
                                        viewModel.openGoogleMapsNavigation(context, spot)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMapSpotSheetCard(
    spot: ParkingSpot,
    isSelected: Boolean,
    isGreek: Boolean,
    onFocusOnMap: () -> Unit,
    onNavigateWaypoint: () -> Unit,
    onOpenGoogleMaps: () -> Unit
) {
    val strings = LocalAppStrings.current
    val timeStr = remember(spot.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(spot.timestamp))
    }

    val cardBg = if (isSelected) Color(0xFF2E241C) else Color(0xFF241C16)
    val borderColor = if (isSelected) Color(0xFFD99B66) else Color.Transparent

    Surface(
        onClick = onFocusOnMap,
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .testTag("history_map_spot_card_${spot.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (spot.isActive) Color(0xFF4A3828) else Color(0xFF332920),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                spot.isBluetoothEntry -> Icons.Default.Bluetooth
                                spot.isGeofenceEntry -> Icons.Default.Place
                                else -> Icons.Default.DirectionsCar
                            },
                            contentDescription = null,
                            tint = Color(0xFFF3DFC1),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = spot.spotName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF6EFE8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = timeStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBBAE9F)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = spot.address.ifBlank {
                            String.format(Locale.US, "%.5f, %.5f", spot.latitude, spot.longitude)
                        },
                        fontSize = 12.5.sp,
                        color = Color(0xFFC4B6A6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onNavigateWaypoint,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF382D24),
                        contentColor = Color(0xFFF3DFC1)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(strings.waypointRadar, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onOpenGoogleMaps,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD99B66),
                        contentColor = Color(0xFF23170B)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(strings.googleMaps, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Real-time 120fps Google Photos-style Dynamic Purple-Dot & Rainbow Metaball Heatmap Renderer.
 * Dynamically transitions saved locations as the user zooms in and out:
 * - Deep Street Zoom (z >= 17.0): Small, soft-edged translucent violet-purple dots (#744DAC) that
 *   smoothly coalesce into purple capsules when adjacent.
 * - Neighborhood / City Zoom (z in 13.2..17.0): Isolated locations remain soft violet-purple circles,
 *   2-3 overlapping locations develop a luminous cyan-teal center (#3494A4), and 4+ overlapping
 *   locations transition through chartreuse/yellow-green (#8ABE36 -> #D0B82A) to a warm rose-magenta
 *   core (#BA3E6A).
 * - Regional / Country / Continent Zoom (z < 13.2): Isolated locations display a soft violet-purple
 *   halo with a subtle cyan-teal center, while regional/city clusters form smooth organic metaball
 *   shapes with a broad rose-magenta core ringed by golden-amber, chartreuse-green, cyan-teal, and
 *   feathered violet-purple.
 */
private object GooglePhotosHeatmapRenderer {
    private const val LUT_SIZE = 1024
    private const val FIXED_GRID_W = 224
    private const val FIXED_GRID_H = 384
    private const val MAX_TRACKED_SPOTS = 512

    private val HEATMAP_LUT = IntArray(LUT_SIZE)

    private var gridW = 0
    private var gridH = 0
    private var densityGrid = FloatArray(0)
    private var pixelBuffer = IntArray(0)
    private var offscreenBitmap: Bitmap? = null
    private var cachedImageBitmap: ImageBitmap? = null

    // Lazily allocated scratch arrays for per-frame projection, Nadaraya-Watson normalization, and coalescing (freed when map closes)
    private var projX = FloatArray(0)
    private var projY = FloatArray(0)
    private var normWeights = FloatArray(0)
    private var coalescedX = FloatArray(0)
    private var coalescedY = FloatArray(0)
    private var coalescedW = FloatArray(0)

    private var prevDirtyMinX = 0
    private var prevDirtyMaxX = -1
    private var prevDirtyMinY = 0
    private var prevDirtyMaxY = -1

    private var lastCenterLat = Double.NaN
    private var lastCenterLng = Double.NaN
    private var lastZoom = Float.NaN
    private var lastSpotsSize = -1
    private var lastFirstSpotId = -1L
    private var lastCanvasW = -1
    private var lastCanvasH = -1

    init {
        // Control knots: (t * 1023, R, G, B, Alpha) matching the exact Google Photos Map color ramp
        val knots = arrayOf(
            intArrayOf(0, 108, 70, 162, 0),       // 0.000: Transparent outer edge
            intArrayOf(46, 110, 72, 165, 42),     // 0.045: Feathered violet-purple outer rim (#6E48A5)
            intArrayOf(128, 114, 75, 169, 128),   // 0.125: Soft violet-purple aura (#724BA9)
            intArrayOf(246, 116, 77, 172, 180),   // 0.240: Core translucent violet-purple (#744DAC - single dot at street zoom)
            intArrayOf(317, 102, 86, 170, 190),   // 0.310: Violet-indigo transition rim
            intArrayOf(430, 52, 149, 164, 206),   // 0.420: Luminous cyan-teal (#3495A4 - 2-spot cluster center)
            intArrayOf(542, 64, 174, 120, 214),   // 0.530: Seafoam-emerald transition (#40AE78)
            intArrayOf(655, 142, 192, 52, 222),   // 0.640: Vibrant chartreuse-green (#8EC034 - 3-spot cluster center)
            intArrayOf(757, 212, 184, 42, 229),   // 0.740: Warm yellow-gold (#D4B82A)
            intArrayOf(849, 220, 142, 44, 235),   // 0.830: Golden-amber / orange (#DC8E2C)
            intArrayOf(936, 186, 62, 106, 241),   // 0.915: Vibrant rose-magenta (#BA3E6A - dense cluster core)
            intArrayOf(1023, 166, 52, 108, 245)   // 1.000: Deep rose-magenta core (#A6346C)
        )

        for (k in 0 until knots.size - 1) {
            val i0 = knots[k][0]
            val i1 = knots[k + 1][0]
            val span = (i1 - i0).coerceAtLeast(1)
            for (idx in i0..i1) {
                val u = (idx - i0).toFloat() / span.toFloat()
                // Smoothstep interpolation on alpha for the first segment so outer edges feather seamlessly
                val smoothU = if (k == 0) u * u * (3f - 2f * u) else u
                val r = (knots[k][1] + (knots[k + 1][1] - knots[k][1]) * u).roundToInt().coerceIn(0, 255)
                val g = (knots[k][2] + (knots[k + 1][2] - knots[k][2]) * u).roundToInt().coerceIn(0, 255)
                val b = (knots[k][3] + (knots[k + 1][3] - knots[k][3]) * u).roundToInt().coerceIn(0, 255)
                val a = (knots[k][4] + (knots[k + 1][4] - knots[k][4]) * smoothU).roundToInt().coerceIn(0, 255)
                HEATMAP_LUT[idx] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    fun DrawScope.drawDynamicSpotsHeatmap(
        spots: List<ParkingSpot>,
        centerLat: Double,
        centerLng: Double,
        zoom: Float,
        viewportCenterX: Float,
        viewportCenterY: Float
    ) {
        if (!zoom.isFinite() || !centerLat.isFinite() || !centerLng.isFinite()) return
        val canvasW = safeRoundToInt(size.width).coerceAtLeast(1)
        val canvasH = safeRoundToInt(size.height).coerceAtLeast(1)

        val firstId = spots.firstOrNull()?.id ?: -1L
        val cachedImg = cachedImageBitmap
        if (cachedImg != null &&
            canvasW == lastCanvasW &&
            canvasH == lastCanvasH &&
            spots.size == lastSpotsSize &&
            firstId == lastFirstSpotId &&
            abs(zoom - lastZoom) < 0.002f &&
            abs(centerLat - lastCenterLat) < 1e-7 &&
            abs(centerLng - lastCenterLng) < 1e-7
        ) {
            drawImage(
                image = cachedImg,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(canvasW, canvasH),
                filterQuality = FilterQuality.Low
            )
            return
        }

        ensureBuffers(FIXED_GRID_W, FIXED_GRID_H, spots.size)

        // Clear only the previous frame's dirty bounding box in densityGrid and pixelBuffer
        val prevMinY = prevDirtyMinY.coerceIn(0, gridH - 1)
        val prevMaxY = prevDirtyMaxY.coerceIn(-1, gridH - 1)
        if (prevDirtyMaxX >= prevDirtyMinX && prevMaxY >= prevMinY) {
            val cMinX = prevDirtyMinX.coerceIn(0, gridW - 1)
            val cMaxX = (prevDirtyMaxX + 1).coerceIn(cMinX, gridW)
            for (gy in prevMinY..prevMaxY) {
                val rowStart = gy * gridW + cMinX
                val rowEnd = gy * gridW + cMaxX
                java.util.Arrays.fill(densityGrid, rowStart, rowEnd, 0f)
                java.util.Arrays.fill(pixelBuffer, rowStart, rowEnd, 0)
            }
        }

        // Compute zoom-dependent screen-space kernel radius (in dp), single-spot peak weight, and tone cap.
        // Crucially, radiusDp is strictly capped at 15.2.dp (matching the lobe width in Screenshot 1 at 10 km)
        // and tapers slightly to 13.2.dp at macro continent zoom (z <= 5.5) so merged clusters NEVER bloat
        // into a huge circle when zooming out.
        val radiusDp: Float
        val singleSpotPeak: Float
        val maxToneCap: Float

        when {
            zoom >= 17.0f -> {
                // Deep Street Zoom:
                // Small soft violet-purple dots that coalesce into soft purple capsules when adjacent
                val tDeep = ((zoom - 17.0f) / 2.2f).coerceIn(0f, 1f)
                radiusDp = 9.2f - tDeep * 2.2f // 9.2.dp at z=17.0 -> 7.0.dp at z>=19.2
                singleSpotPeak = 0.245f
                maxToneCap = if (zoom >= 17.6f) {
                    0.295f
                } else {
                    0.295f + ((17.6f - zoom) / 0.6f) * 0.085f
                }
            }
            zoom >= 15.2f -> {
                // Neighborhood Zoom:
                // 1 spot = soft purple circle; 2-3 overlapping spots = purple halo + cyan-teal core
                val u = ((17.0f - zoom) / 1.8f).coerceIn(0f, 1f)
                radiusDp = 9.2f + u * 4.2f       // 9.2.dp -> 13.4.dp
                singleSpotPeak = 0.245f + u * 0.045f // 0.245 -> 0.290
                maxToneCap = 0.38f + u * 0.44f   // 0.38 -> 0.82
            }
            zoom >= 13.2f -> {
                // City / District Zoom:
                // 1 spot = purple circle; 2 spots = teal core; 3 spots = green-yellow; 5+ spots = rose-magenta core
                val u = ((15.2f - zoom) / 2.0f).coerceIn(0f, 1f)
                radiusDp = 13.4f + u * 1.8f      // 13.4.dp -> 15.2.dp (strictly capped!)
                singleSpotPeak = 0.290f + u * 0.055f // 0.290 -> 0.345
                maxToneCap = (0.82f + u * 0.18f).coerceAtMost(1.0f)
            }
            else -> {
                // Regional / Country / Continent Zoom (10 km .. 200 km+):
                // Maximum single-cluster radius is capped at 15.2.dp around z=11..13.2 (Screenshot 1)
                // and gently tightens to 13.2.dp at z <= 5.5 so when all entries merge into one spot,
                // its maximum size matches the compact lobe in Screenshot 1 rather than a giant circle.
                val uMacro = ((11.5f - zoom) / 6.0f).coerceIn(0f, 1f)
                radiusDp = 15.2f - uMacro * 2.0f // 15.2.dp at z>=11.5 -> 13.2.dp at z<=5.5
                singleSpotPeak = 0.345f + uMacro * 0.045f // 0.345 -> 0.390
                maxToneCap = 1.0f
            }
        }

        val scaleX = gridW.toFloat() / canvasW.toFloat()
        val scaleY = gridH.toFloat() / canvasH.toFloat()
        val radiusScreenPx = radiusDp.dp.toPx()
        val marginPx = radiusScreenPx + 8f

        // 1. Project visible spots into screen coordinates
        var visCount = 0
        for (i in spots.indices) {
            val spot = spots[i]
            val pt = latLngToScreenPx(
                lat = spot.latitude,
                lng = spot.longitude,
                centerLat = centerLat,
                centerLng = centerLng,
                zoom = zoom,
                viewportCenterX = viewportCenterX,
                viewportCenterY = viewportCenterY
            )
            if (pt.x.isFinite() && pt.y.isFinite() &&
                pt.x >= -marginPx && pt.x <= canvasW + marginPx &&
                pt.y >= -marginPx && pt.y <= canvasH + marginPx
            ) {
                projX[visCount] = pt.x
                projY[visCount] = pt.y
                visCount++
            }
        }

        lastCenterLat = centerLat
        lastCenterLng = centerLng
        lastZoom = zoom
        lastSpotsSize = spots.size
        lastFirstSpotId = firstId
        lastCanvasW = canvasW
        lastCanvasH = canvasH

        if (visCount == 0) {
            if (prevDirtyMaxX >= prevDirtyMinX && prevMaxY >= prevMinY) {
                val bmp = offscreenBitmap
                if (bmp != null) {
                    val clearRows = prevMaxY - prevMinY + 1
                    bmp.setPixels(pixelBuffer, prevMinY * gridW, gridW, 0, prevMinY, gridW, clearRows)
                    cachedImageBitmap = bmp.asImageBitmap()
                }
            }
            prevDirtyMinX = 0
            prevDirtyMaxX = -1
            prevDirtyMinY = 0
            prevDirtyMaxY = -1
            return
        }

        // 2. Nadaraya-Watson Self-Normalized Cluster Weights:
        //    For each visible spot i, compute its continuous kernel overlap sum M_i = sum_j (1 - d_ij^2 / R^2)^2.
        //    Normalizing each spot's weight by M_i guarantees that even when 85+ spots collapse onto a single
        //    pixel at 200 km zoom, their summed weight at the cluster center is strictly <= maxToneCap (1.0),
        //    preserving the exact (1 - r^2)^2 radial rainbow falloff and preventing radial bloat!
        val invRadScreen2 = 1f / (radiusScreenPx * radiusScreenPx).coerceAtLeast(1f)
        val spanAboveSingle = (maxToneCap - singleSpotPeak).coerceAtLeast(0.01f)
        for (i in 0 until visCount) {
            val xi = projX[i]
            val yi = projY[i]
            var mSum = 1f
            for (j in 0 until visCount) {
                if (i == j) continue
                val dx = xi - projX[j]
                val dy = yi - projY[j]
                val r2 = (dx * dx + dy * dy) * invRadScreen2
                if (r2 < 1f) {
                    val oneMinusR2 = 1f - r2
                    mSum += oneMinusR2 * oneMinusR2
                }
            }
            val targetPeak = if (mSum <= 1.001f) {
                singleSpotPeak
            } else {
                (singleSpotPeak + spanAboveSingle * (1f - kotlin.math.exp(-(mSum - 1f) * 0.58f)))
                    .coerceIn(singleSpotPeak, maxToneCap)
            }
            normWeights[i] = targetPeak / mSum
        }

        // 3. Coalesce sub-pixel / nearly-coincident spots (within 1.35 px) so zoomed-out clusters
        //    rasterize in 1-5 passes instead of 85 redundant passes over the same grid cells.
        val mergeDistSq = 1.35f * 1.35f
        var coalCount = 0
        for (i in 0 until visCount) {
            val xi = projX[i]
            val yi = projY[i]
            val wi = normWeights[i]
            var merged = false
            for (c in 0 until coalCount) {
                val dx = xi - coalescedX[c]
                val dy = yi - coalescedY[c]
                if (dx * dx + dy * dy <= mergeDistSq) {
                    val oldW = coalescedW[c]
                    val newW = oldW + wi
                    if (newW > 0f) {
                        coalescedX[c] = (coalescedX[c] * oldW + xi * wi) / newW
                        coalescedY[c] = (coalescedY[c] * oldW + yi * wi) / newW
                        coalescedW[c] = newW
                    }
                    merged = true
                    break
                }
            }
            if (!merged) {
                coalescedX[coalCount] = xi
                coalescedY[coalCount] = yi
                coalescedW[coalCount] = wi
                coalCount++
            }
        }

        val radiusGridX = (radiusScreenPx * scaleX).coerceAtLeast(2.0f)
        val radiusGridY = (radiusScreenPx * scaleY).coerceAtLeast(2.0f)
        val invRadX2 = 1f / (radiusGridX * radiusGridX)
        val invRadY2 = 1f / (radiusGridY * radiusGridY)
        val radCellsX = ceilToInt(radiusGridX)
        val radCellsY = ceilToInt(radiusGridY)

        var dirtyMinX = gridW
        var dirtyMaxX = -1
        var dirtyMinY = gridH
        var dirtyMaxY = -1
        var maxDensity = 0f

        for (c in 0 until coalCount) {
            val gxCenter = coalescedX[c] * scaleX
            val gyCenter = coalescedY[c] * scaleY
            val weight = coalescedW[c]

            val x0 = ((gxCenter - radCellsX).toInt()).coerceAtLeast(0)
            val x1 = ((gxCenter + radCellsX).toInt()).coerceAtMost(gridW - 1)
            val y0 = ((gyCenter - radCellsY).toInt()).coerceAtLeast(0)
            val y1 = ((gyCenter + radCellsY).toInt()).coerceAtMost(gridH - 1)

            if (x0 > x1 || y0 > y1) continue
            if (x0 < dirtyMinX) dirtyMinX = x0
            if (x1 > dirtyMaxX) dirtyMaxX = x1
            if (y0 < dirtyMinY) dirtyMinY = y0
            if (y1 > dirtyMaxY) dirtyMaxY = y1

            for (gy in y0..y1) {
                val dy = (gy + 0.5f) - gyCenter
                val dyNorm2 = (dy * dy) * invRadY2
                if (dyNorm2 >= 1f) continue
                val rowOff = gy * gridW
                for (gx in x0..x1) {
                    val dx = (gx + 0.5f) - gxCenter
                    val r2 = dyNorm2 + (dx * dx) * invRadX2
                    if (r2 < 1f) {
                        val oneMinusR2 = 1f - r2
                        val kVal = weight * (oneMinusR2 * oneMinusR2)
                        val newD = densityGrid[rowOff + gx] + kVal
                        densityGrid[rowOff + gx] = newD
                        if (newD > maxDensity) {
                            maxDensity = newD
                        }
                    }
                }
            }
        }

        if (dirtyMaxX < dirtyMinX || dirtyMaxY < dirtyMinY || maxDensity <= 0.002f) {
            prevDirtyMinX = 0
            prevDirtyMaxX = -1
            prevDirtyMinY = 0
            prevDirtyMaxY = -1
            return
        }

        // 4. Direct LUT mapping over dirty region (zero exp() calls in the 2D pixel loop)
        val lutMaxIdx = (LUT_SIZE - 1).toFloat()
        for (gy in dirtyMinY..dirtyMaxY) {
            val rowOff = gy * gridW
            for (gx in dirtyMinX..dirtyMaxX) {
                val rawD = densityGrid[rowOff + gx]
                if (rawD <= 0.004f) continue
                val tone = rawD.coerceIn(0f, maxToneCap)
                val lutIdx = safeRoundToInt(tone * lutMaxIdx).coerceIn(0, LUT_SIZE - 1)
                pixelBuffer[rowOff + gx] = HEATMAP_LUT[lutIdx]
            }
        }

        val bmp = offscreenBitmap ?: return
        // Upload only the union of previous and current dirty row bands instead of the full bitmap
        val uploadMinY = if (prevMaxY >= prevMinY) min(dirtyMinY, prevMinY) else dirtyMinY
        val uploadMaxY = if (prevMaxY >= prevMinY) max(dirtyMaxY, prevMaxY) else dirtyMaxY
        val uploadRows = (uploadMaxY - uploadMinY + 1).coerceIn(1, gridH - uploadMinY)
        bmp.setPixels(pixelBuffer, uploadMinY * gridW, gridW, 0, uploadMinY, gridW, uploadRows)

        prevDirtyMinX = dirtyMinX
        prevDirtyMaxX = dirtyMaxX
        prevDirtyMinY = dirtyMinY
        prevDirtyMaxY = dirtyMaxY

        val imgBmp = cachedImageBitmap ?: bmp.asImageBitmap().also { cachedImageBitmap = it }
        // Prepare bitmap for redraw
        bmp.prepareToDraw()

        drawImage(
            image = imgBmp,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(canvasW, canvasH),
            filterQuality = FilterQuality.Low
        )
    }

    fun releaseMemory() {
        gridW = 0
        gridH = 0
        densityGrid = FloatArray(0)
        pixelBuffer = IntArray(0)
        cachedImageBitmap = null
        offscreenBitmap = null
        projX = FloatArray(0)
        projY = FloatArray(0)
        normWeights = FloatArray(0)
        coalescedX = FloatArray(0)
        coalescedY = FloatArray(0)
        coalescedW = FloatArray(0)
        prevDirtyMinX = 0
        prevDirtyMaxX = -1
        prevDirtyMinY = 0
        prevDirtyMaxY = -1
        lastCenterLat = Double.NaN
        lastCenterLng = Double.NaN
        lastZoom = Float.NaN
        lastSpotsSize = -1
        lastFirstSpotId = -1L
        lastCanvasW = -1
        lastCanvasH = -1
    }

    private fun ensureBuffers(w: Int, h: Int, spotsCount: Int) {
        if (spotsCount > projX.size) {
            val newCap = max(spotsCount + 32, max(64, projX.size * 2))
            projX = FloatArray(newCap)
            projY = FloatArray(newCap)
            normWeights = FloatArray(newCap)
            coalescedX = FloatArray(newCap)
            coalescedY = FloatArray(newCap)
            coalescedW = FloatArray(newCap)
        }
        if (w == gridW && h == gridH && offscreenBitmap != null) return
        gridW = w
        gridH = h
        val total = w * h
        densityGrid = FloatArray(total)
        pixelBuffer = IntArray(total)
        offscreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        cachedImageBitmap = null
        prevDirtyMinX = 0
        prevDirtyMaxX = -1
        prevDirtyMinY = 0
        prevDirtyMaxY = -1
    }
}

private val scaleBarTextPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    color = android.graphics.Color.rgb(216, 226, 240)
}

/**
 * Draws a live metric ground-resolution scale bar (e.g., 2 m, 5 m, 10 m, 20 m, 50 m, 100 m)
 * so high-zoom street/building precision is immediately clear.
 */
private fun DrawScope.drawHighZoomScaleBar(
    centerLat: Double,
    zoom: Float,
    bottomY: Float
) {
    val metersPerPixel = (156543.03392 * cos(Math.toRadians(centerLat)) / 2.0.pow(zoom.toDouble())).coerceAtLeast(0.005)
    val maxBarPx = 110.dp.toPx()
    val maxMeters = metersPerPixel * maxBarPx

    val niceSteps = doubleArrayOf(
        1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0, 200.0, 500.0,
        1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0, 100000.0, 200000.0, 500000.0
    )
    var chosenMeters = niceSteps.first()
    for (step in niceSteps) {
        if (step <= maxMeters) {
            chosenMeters = step
        } else {
            break
        }
    }

    val barPx = (chosenMeters / metersPerPixel).toFloat().coerceIn(28.dp.toPx(), maxBarPx)
    val label = if (chosenMeters >= 1000.0) {
        "${(chosenMeters / 1000.0).toInt()} km"
    } else {
        "${chosenMeters.toInt()} m"
    }

    val startX = 16.dp.toPx()
    val barY = bottomY.coerceIn(48.dp.toPx(), size.height - 20.dp.toPx())
    val pillW = barPx + 18.dp.toPx()
    val pillH = 24.dp.toPx()

    drawRoundRect(
        color = Color(0xCC151D2A),
        topLeft = Offset(startX - 9.dp.toPx(), barY - pillH + 5.dp.toPx()),
        size = Size(pillW, pillH),
        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )

    val lineStroke = 1.8.dp.toPx()
    val tickH = 5.dp.toPx()
    val barColor = Color(0xFFD8E2F0)

    // Horizontal scale line + left/right ticks
    drawLine(
        color = barColor,
        start = Offset(startX, barY),
        end = Offset(startX + barPx, barY),
        strokeWidth = lineStroke
    )
    drawLine(
        color = barColor,
        start = Offset(startX, barY - tickH),
        end = Offset(startX, barY),
        strokeWidth = lineStroke
    )
    drawLine(
        color = barColor,
        start = Offset(startX + barPx, barY - tickH),
        end = Offset(startX + barPx, barY),
        strokeWidth = lineStroke
    )

    scaleBarTextPaint.textSize = 10.sp.toPx()
    drawContext.canvas.nativeCanvas.drawText(
        label,
        startX + 4.dp.toPx(),
        barY - 4.5.dp.toPx(),
        scaleBarTextPaint
    )
}

/**
 * Computes the effective slippy-map integer zoom level [2..20].
 * Biased by +0.30f so scaledTileSize is always >= 208px, keeping tile count per screen compact
 * while maintaining crisp detail across all zoom levels.
 */
private fun computeEffectiveTileZoom(zoom: Float): Int {
    return (zoom + 0.30f).toInt().coerceIn(2, 20)
}

/**
 * Draws loaded slippy map tiles onto the Canvas with native GPU hardware bilinear (FilterQuality.Low)
 * sampling for 120fps zero-lag pinch-zooming, resolution-independent (256px/512px) ancestor fallback,
 * and immediate child-tile compositing when zooming out.
 */
private fun DrawScope.drawSlippyMapTiles(
    tileCache: Map<String, ImageBitmap>,
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    viewportCenterX: Float,
    viewportCenterY: Float
): Boolean {
    if (tileCache.isEmpty() || !zoom.isFinite() || !centerLat.isFinite() || !centerLng.isFinite()) return false
    val safeZoom = zoom.coerceIn(2.2f, 22.5f)
    val tileZ = computeEffectiveTileZoom(safeZoom)
    val scale = 2.0.pow((safeZoom - tileZ).toDouble()).toFloat().coerceIn(0.25f, 32f)
    val scaledTileSize = (256f * scale).coerceAtLeast(64f)
    val n = 1 shl tileZ

    val centerTileX = lngToTileX(centerLng, tileZ)
    val centerTileY = latToTileY(centerLat, tileZ)

    val halfCols = (ceilToInt((size.width / scaledTileSize) / 2f) + 1).coerceIn(1, 6)
    val halfRows = (ceilToInt((size.height / scaledTileSize) / 2f) + 1).coerceIn(1, 9)
    val baseCol = floor(centerTileX).toInt()
    val baseRow = floor(centerTileY).toInt()

    // FilterQuality.Low maps to single-pass hardware GL_LINEAR filtering on Android GPU,
    // avoiding slow software/shader cubic convolution during rapid pinch-to-zoom gestures.
    val primaryQuality = FilterQuality.Low

    var drewAny = false
    val dstTileSide = (ceilToInt(scaledTileSize) + 1).coerceAtLeast(1)
    val dstSize = IntSize(dstTileSide, dstTileSide)

    for (dy in -halfRows..halfRows) {
        val ty = baseRow + dy
        if (ty !in 0 until n) continue
        for (dx in -halfCols..halfCols) {
            val rawCol = baseCol + dx
            val tx = ((rawCol % n) + n) % n
            val key = "$tileZ/$tx/$ty"

            val screenX = viewportCenterX + ((rawCol - centerTileX) * scaledTileSize).toFloat()
            val screenY = viewportCenterY + ((ty - centerTileY) * scaledTileSize).toFloat()
            if (!screenX.isFinite() || !screenY.isFinite()) continue
            // Add +1px overlap to prevent hairline seams between adjacent scaled tiles
            val dstOffset = IntOffset(safeRoundToInt(screenX), safeRoundToInt(screenY))

            val exactBmp = tileCache[key]
            if (exactBmp != null) {
                drawImage(
                    image = exactBmp,
                    dstOffset = dstOffset,
                    dstSize = dstSize,
                    alpha = 1.0f,
                    filterQuality = primaryQuality
                )
                drewAny = true
            } else {
                // 1. Search up to 8 ancestor zoom levels so zooming in never blanks out while keeping lookups fast.
                val maxStep = min(8, tileZ - 2)
                for (step in 1..maxStep) {
                    val parentZ = tileZ - step
                    val px = tx shr step
                    val py = ty shr step
                    val parentBmp = tileCache["$parentZ/$px/$py"]
                    if (parentBmp != null && parentBmp.width > 0 && parentBmp.height > 0) {
                        val pDim = parentBmp.width
                        val mask = (1 shl step) - 1
                        val subSize = (pDim shr step).coerceIn(1, pDim)
                        val maxOff = (pDim - subSize).coerceAtLeast(0)
                        val srcX = ((tx and mask) * subSize).coerceIn(0, maxOff)
                        val srcY = ((ty and mask) * subSize).coerceIn(0, maxOff)
                        drawImage(
                            image = parentBmp,
                            srcOffset = IntOffset(srcX, srcY),
                            srcSize = IntSize(subSize, subSize),
                            dstOffset = dstOffset,
                            dstSize = dstSize,
                            alpha = 1.0f,
                            filterQuality = primaryQuality
                        )
                        drewAny = true
                        break
                    }
                }

                // 2. Overlay any already-cached immediate child tiles (tileZ + 1) when zooming out
                if (tileZ < 20) {
                    val childZ = tileZ + 1
                    val halfScaled = scaledTileSize / 2f
                    val childSide = (ceilToInt(halfScaled) + 1).coerceAtLeast(1)
                    val childDstSize = IntSize(childSide, childSide)
                    for (cy in 0..1) {
                        for (cx in 0..1) {
                            val childTx = tx * 2 + cx
                            val childTy = ty * 2 + cy
                            val childBmp = tileCache["$childZ/$childTx/$childTy"]
                            if (childBmp != null) {
                                val childX = screenX + cx * halfScaled
                                val childY = screenY + cy * halfScaled
                                drawImage(
                                    image = childBmp,
                                    dstOffset = IntOffset(safeRoundToInt(childX), safeRoundToInt(childY)),
                                    dstSize = childDstSize,
                                    alpha = 1.0f,
                                    filterQuality = primaryQuality
                                )
                                drewAny = true
                            }
                        }
                    }
                }
            }
        }
    }
    return drewAny
}

private val offlineBasemapPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
    textAlign = android.graphics.Paint.Align.CENTER
    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
}

private val offlineReusablePath = Path()

/**
 * Offline vector basemap rendering coastlines, borders, highways, route shields, and bilingual
 * country/city/sea labels matching the dark navy & amber style in the screenshots.
 */
private fun DrawScope.drawOfflineVectorBasemap(
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    viewportCenterX: Float,
    viewportCenterY: Float,
    landColor: Color,
    borderColor: Color,
    roadMinorColor: Color,
    roadMajorColor: Color,
    labelCityColor: Color,
    labelCountryColor: Color,
    labelWaterColor: Color,
    isGreek: Boolean
) {
    fun project(lat: Double, lng: Double): Offset = latLngToScreenPx(
        lat = lat,
        lng = lng,
        centerLat = centerLat,
        centerLng = centerLng,
        zoom = zoom,
        viewportCenterX = viewportCenterX,
        viewportCenterY = viewportCenterY
    )

    val path = offlineReusablePath

    // Simplified landmasses so water/land contrast is always visible even offline
    for (poly in OFFLINE_LAND_POLYGONS) {
        path.reset()
        for (i in poly.indices) {
            val pt = project(poly[i].first, poly[i].second)
            if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        path.close()
        drawPath(path = path, color = landColor, style = Fill)
        drawPath(path = path, color = borderColor, style = Stroke(width = 1.2.dp.toPx()))
    }

    // Major regional highways (visible when zoom >= 6.2f)
    if (zoom >= 6.2f) {
        val roadWidth = if (zoom >= 10f) 2.2.dp.toPx() else 1.4.dp.toPx()
        for (road in OFFLINE_HIGHWAYS) {
            path.reset()
            for (i in road.points.indices) {
                val pt = project(road.points[i].first, road.points[i].second)
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            drawPath(
                path = path,
                color = if (road.isMajor) roadMajorColor else roadMinorColor,
                style = Stroke(width = roadWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }

    val paint = offlineBasemapPaint

    // Highway route shields (e.g., E92, E951, E75, E90 matching screenshots 3, 6, 8)
    if (zoom >= 8.2f) {
        paint.textSize = 10.5.sp.toPx()
        for (shield in OFFLINE_ROUTE_SHIELDS) {
            val pt = project(shield.lat, shield.lng)
            if (pt.x in 20f..(size.width - 20f) && pt.y in 20f..(size.height - 20f)) {
                val textWidth = paint.measureText(shield.code)
                val boxW = textWidth + 10.dp.toPx()
                val boxH = 17.dp.toPx()
                val bgColor = if (shield.isEuropean) Color(0xFF4F7A4E) else Color(0xFF3E6796)
                val strokeColor = if (shield.isEuropean) Color(0xFFA7CCA4) else Color(0xFF9BC0EA)

                drawRoundRect(
                    color = bgColor,
                    topLeft = Offset(pt.x - boxW / 2f, pt.y - boxH / 2f),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(pt.x - boxW / 2f, pt.y - boxH / 2f),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
                paint.color = android.graphics.Color.rgb(235, 242, 235)
                drawContext.canvas.nativeCanvas.drawText(
                    shield.code,
                    pt.x,
                    pt.y + 3.5.dp.toPx(),
                    paint
                )
            }
        }
    }

    // Sea / Lake Labels
    for (sea in OFFLINE_WATER_LABELS) {
        if (zoom in sea.minZoom..sea.maxZoom) {
            val pt = project(sea.lat, sea.lng)
            if (pt.x in 0f..size.width && pt.y in 0f..size.height) {
                paint.textSize = 12.sp.toPx()
                paint.color = android.graphics.Color.argb(
                    215,
                    (labelWaterColor.red * 255).toInt(),
                    (labelWaterColor.green * 255).toInt(),
                    (labelWaterColor.blue * 255).toInt()
                )
                val label = if (isGreek) sea.nameEl else sea.nameEn
                drawContext.canvas.nativeCanvas.drawText(label, pt.x, pt.y, paint)
            }
        }
    }

    // Country & City Labels
    for (place in OFFLINE_MAP_PLACES) {
        if (zoom in place.minZoom..place.maxZoom) {
            val pt = project(place.lat, place.lng)
            if (pt.x in 16f..(size.width - 16f) && pt.y in 16f..(size.height - 16f)) {
                val label = if (isGreek) place.nameEl else place.nameEn
                if (place.isCountry) {
                    paint.textSize = 13.5.sp.toPx()
                    paint.color = android.graphics.Color.argb(
                        220,
                        (labelCountryColor.red * 255).toInt(),
                        (labelCountryColor.green * 255).toInt(),
                        (labelCountryColor.blue * 255).toInt()
                    )
                    drawContext.canvas.nativeCanvas.drawText(label, pt.x, pt.y, paint)
                } else {
                    // City dot + warm peach label matching screenshots
                    drawCircle(
                        color = Color(0xFF152338),
                        radius = 3.5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color(0xFFEAE0D5),
                        radius = 2.dp.toPx(),
                        center = pt
                    )
                    paint.textSize = 13.sp.toPx()
                    paint.color = android.graphics.Color.argb(
                        245,
                        (labelCityColor.red * 255).toInt(),
                        (labelCityColor.green * 255).toInt(),
                        (labelCityColor.blue * 255).toInt()
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        pt.x + 8.dp.toPx() + paint.measureText(label) / 2f,
                        pt.y + 4.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}

private data class TileRequestJob(
    val z: Int,
    val x: Int,
    val y: Int,
    val key: String,
    val isUrgent: Boolean
)

/**
 * High-performance, memory-bounded singleton tile engine:
 * 1. Strictly bounded 48 MB memory cache across all zoom levels (evicts distant zoom/viewport tiles first).
 * 2. Smooth Continuous Anti-Aliased Cartographic Color Shader (preserves 100% of sub-pixel anti-aliased
 *    diagonal road edges, building walls, and Greek/Latin text strokes without stair-stepped color jumps).
 * 3. Diagonal-Aware 2x Edge-Directed Super-Resolution (512x512 HD tiles at z >= 18, and multi-pass
 *    contour-smoothed quadrant super-resolution at z = 20..21) isolated onto a 4-thread compute pool.
 */
internal object MapTileEngine {
    private const val DISK_CACHE_FOLDER = "osm_google_photos_hd_v10"
    private const val RAW19_CACHE_FOLDER = "osm_raw19_v9"
    private const val MAX_CACHE_BYTES = 64L * 1024L * 1024L // 64 MB cap (~250 256x256 tiles)
    private const val MAX_CACHE_TILES = 250
    private const val MAX_RAW19_TILES = 12

    private val isMapOpen = AtomicBoolean(false)
    private val cacheLock = Any()
    val memoryCache = ConcurrentHashMap<String, ImageBitmap>(64)
    private val memoryCacheBytesByKey = ConcurrentHashMap<String, Int>(64)
    private val protectedViewportKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>(128))
    private val currentCacheBytes = AtomicLong(0L)
    // Thread-safe immutable 256x256 pixel arrays for z=19 tiles so multiple threads never touch a shared Bitmap
    private val raw19PixelsCache = ConcurrentHashMap<String, IntArray>(16)
    private val inFlightNetwork = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>(64))
    private val inFlightDisk = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>(64))
    private val diskCheckedMissing = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>(128))
    private val legacyCacheCleaned = AtomicBoolean(false)

    private val diskQueue = ConcurrentLinkedDeque<TileRequestJob>()
    private val diskQueuedKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>(128))
    private val activeDiskWorkers = AtomicInteger(0)

    private val networkQueue = ConcurrentLinkedDeque<TileRequestJob>()
    private val queuedKeys = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>(128))
    private val latestDesiredJobs = AtomicReference<List<TileRequestJob>>(emptyList())
    private val activeNetworkWorkers = AtomicInteger(0)
    private val pendingDiskWrites = AtomicInteger(0)
    private val latestTargetZoom = AtomicInteger(12)
    private val latestCenterCol = AtomicInteger(0)
    private val latestCenterRow = AtomicInteger(0)
    private val invalidationPending = AtomicBoolean(false)
    private var viewportScheduleJob: kotlinx.coroutines.Job? = null
    private var viewportRecoveryJob: kotlinx.coroutines.Job? = null

    val tileInvalidationFlow = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private fun createAutoExpiringDispatcher(maxThreads: Int, threadName: String, threadPriority: Int) =
        java.util.concurrent.ThreadPoolExecutor(
            maxThreads,
            maxThreads,
            2L,
            java.util.concurrent.TimeUnit.SECONDS,
            java.util.concurrent.LinkedBlockingQueue()
        ) { r ->
            Thread(r, threadName).apply {
                isDaemon = true
                priority = threadPriority
            }
        }.apply {
            allowCoreThreadTimeOut(true)
        }.asCoroutineDispatcher()

    private val diskReadDispatcher = createAutoExpiringDispatcher(3, "MapTileDiskRead", Thread.NORM_PRIORITY + 1)
    private val networkDispatcher = createAutoExpiringDispatcher(6, "MapTileNetWorker", Thread.NORM_PRIORITY)
    private val tileComputeDispatcher = createAutoExpiringDispatcher(4, "MapTileCompute", Thread.NORM_PRIORITY)
    private val diskWriteDispatcher = createAutoExpiringDispatcher(2, "MapTileDiskWrite", Thread.MIN_PRIORITY + 1)

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Reusable ThreadLocal 256x256 pixel buffers confined to tileComputeDispatcher (freed when threads expire after 2s)
    private val threadLocalCrop128 = object : ThreadLocal<IntArray>() {
        override fun initialValue(): IntArray = IntArray(128 * 128)
    }
    private val threadLocalPixels256 = object : ThreadLocal<IntArray>() {
        override fun initialValue(): IntArray = IntArray(256 * 256)
    }
    private val threadLocalLum256 = object : ThreadLocal<IntArray>() {
        override fun initialValue(): IntArray = IntArray(256 * 256)
    }
    private val threadLocalScratch256 = object : ThreadLocal<IntArray>() {
        override fun initialValue(): IntArray = IntArray(256 * 256)
    }

    // Precomputed smooth continuous 256-entry luminance ramps for Neutral (roads/casings/text)
    // and Warm (land/buildings/outlines/labels) so anti-aliased edges NEVER jump across hard color buckets!
    private val NEUTRAL_RAMP_R = IntArray(256)
    private val NEUTRAL_RAMP_G = IntArray(256)
    private val NEUTRAL_RAMP_B = IntArray(256)

    private val WARM_RAMP_R = IntArray(256)
    private val WARM_RAMP_G = IntArray(256)
    private val WARM_RAMP_B = IntArray(256)

    init {
        val neutralKnots = arrayOf(
            intArrayOf(0, 240, 244, 252),
            intArrayOf(75, 212, 220, 234),
            intArrayOf(135, 136, 148, 166),
            intArrayOf(175, 76, 88, 108),
            intArrayOf(215, 52, 62, 78),
            intArrayOf(240, 62, 74, 94),
            intArrayOf(255, 72, 86, 110)
        )
        val warmKnots = arrayOf(
            intArrayOf(0, 236, 214, 184),
            intArrayOf(75, 206, 182, 148),
            intArrayOf(135, 142, 128, 108),
            intArrayOf(172, 106, 100, 88),
            intArrayOf(202, 54, 62, 76),
            intArrayOf(224, 44, 52, 66),
            intArrayOf(240, 34, 40, 52),
            intArrayOf(255, 36, 42, 54)
        )

        fun fillRamp(knots: Array<IntArray>, outR: IntArray, outG: IntArray, outB: IntArray) {
            for (k in 0 until knots.size - 1) {
                val l0 = knots[k][0]
                val l1 = knots[k + 1][0]
                val span = (l1 - l0).coerceAtLeast(1)
                for (l in l0..l1) {
                    val t = l - l0
                    outR[l] = knots[k][1] + ((knots[k + 1][1] - knots[k][1]) * t) / span
                    outG[l] = knots[k][2] + ((knots[k + 1][2] - knots[k][2]) * t) / span
                    outB[l] = knots[k][3] + ((knots[k + 1][3] - knots[k][3]) * t) / span
                }
            }
        }

        fillRamp(neutralKnots, NEUTRAL_RAMP_R, NEUTRAL_RAMP_G, NEUTRAL_RAMP_B)
        fillRamp(warmKnots, WARM_RAMP_R, WARM_RAMP_G, WARM_RAMP_B)
    }

    fun cleanupLegacyCacheOnce(cacheDir: File) {
        if (legacyCacheCleaned.compareAndSet(false, true)) {
            engineScope.launch(diskWriteDispatcher) {
                try {
                    File(cacheDir, "map_tiles_dark").deleteRecursively()
                    File(cacheDir, "osm_google_photos_dark_v3").deleteRecursively()
                    File(cacheDir, "osm_google_photos_dark_v4").deleteRecursively()
                    File(cacheDir, "osm_google_photos_dark_v5").deleteRecursively()
                    File(cacheDir, "osm_google_photos_hd_v6").deleteRecursively()
                    File(cacheDir, "osm_google_photos_hd_v7").deleteRecursively()
                    File(cacheDir, "osm_google_photos_hd_v8").deleteRecursively()
                    File(cacheDir, "osm_google_photos_hd_v9").deleteRecursively()
                    File(cacheDir, "osm_raw19_v8").deleteRecursively()
                    File(cacheDir, "osm_vector_hd_v3").deleteRecursively()
                    File(cacheDir, "osm_vector_hd_bin_v4").deleteRecursively()
                    File(cacheDir, "osm_vector_hd_bin_v5").deleteRecursively()
                    File(cacheDir, "osm_vector_hd_bin_v6").deleteRecursively()
                    File(cacheDir, "osm_vector_hd_bin_v7").deleteRecursively()
                    File(cacheDir, "osm_vector_hd_bin_v8").deleteRecursively()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun onMapOpened() {
        isMapOpen.set(true)
    }

    fun onMapClosed(appInBackground: Boolean = false) {
        isMapOpen.set(false)
        viewportScheduleJob?.cancel()
        viewportScheduleJob = null
        viewportRecoveryJob?.cancel()
        viewportRecoveryJob = null
        diskQueue.clear()
        diskQueuedKeys.clear()
        networkQueue.clear()
        queuedKeys.clear()
        raw19PixelsCache.clear()
        protectedViewportKeys.clear()
        diskCheckedMissing.clear()
        latestDesiredJobs.set(emptyList())
        synchronized(cacheLock) {
            memoryCache.clear()
            memoryCacheBytesByKey.clear()
            currentCacheBytes.set(0L)
        }
        offlineReusablePath.reset()
        GooglePhotosHeatmapRenderer.releaseMemory()
        HighZoomVectorEngine.onMapClosed()
        reclaimMapMemoryAsync(appInBackground)
    }

    private fun reclaimMapMemoryAsync(appInBackground: Boolean) {
        engineScope.launch(Dispatchers.IO) {
            try {
                delay(120)
                if (isMapOpen.get()) return@launch
                synchronized(cacheLock) {
                    memoryCache.clear()
                    memoryCacheBytesByKey.clear()
                    currentCacheBytes.set(0L)
                }
                raw19PixelsCache.clear()
                trimHardwareUiMemory(if (appInBackground) 80 else 60)
                System.runFinalization()
                Runtime.getRuntime().gc()
                delay(180)
                if (isMapOpen.get()) return@launch
                trimHardwareUiMemory(if (appInBackground) 80 else 60)
                System.runFinalization()
                Runtime.getRuntime().gc()
            } catch (_: Throwable) {
            }
        }
    }

    private fun trimHardwareUiMemory(level: Int) {
        try {
            val wmgClass = Class.forName("android.view.WindowManagerGlobal")
            val getInstance = wmgClass.getDeclaredMethod("getInstance")
            val instance = getInstance.invoke(null)
            val trimMemory = wmgClass.getDeclaredMethod("trimMemory", Int::class.javaPrimitiveType)
            trimMemory.invoke(instance, level)
        } catch (_: Throwable) {
        }
    }

    fun trimMemoryOnExit() {
        onMapClosed(appInBackground = false)
    }

    fun clearMemoryCache() {
        onMapClosed(appInBackground = true)
    }

    fun requestRedraw() {
        notifyTileLoaded()
    }

    private fun notifyTileLoaded() {
        if (invalidationPending.compareAndSet(false, true)) {
            engineScope.launch {
                delay(16)
                invalidationPending.set(false)
                tileInvalidationFlow.tryEmit(Unit)
            }
        }
    }

    private fun removeTileFromMemoryLocked(key: String) {
        memoryCache.remove(key)
        val removedBytes = memoryCacheBytesByKey.remove(key)
        if (removedBytes != null) {
            val updated = (currentCacheBytes.get() - removedBytes.toLong()).coerceAtLeast(0L)
            currentCacheBytes.set(updated)
        }
    }

    private fun tileEvictionScore(key: String, currentZ: Int, centerCol: Int, centerRow: Int): Int {
        if (protectedViewportKeys.contains(key)) return -100000
        val firstSlash = key.indexOf('/')
        val secondSlash = if (firstSlash > 0) key.indexOf('/', firstSlash + 1) else -1
        if (firstSlash <= 0 || secondSlash <= firstSlash) return 999999
        val kz = key.substring(0, firstSlash).toIntOrNull() ?: return 999999
        val kx = key.substring(firstSlash + 1, secondSlash).toIntOrNull() ?: 0
        val ky = key.substring(secondSlash + 1).toIntOrNull() ?: 0
        val zDiff = abs(kz - currentZ)
        val step = (currentZ - kz).coerceIn(0, 20)
        val shiftUp = (kz - currentZ).coerceIn(0, 20)
        val expectedCol = if (kz <= currentZ) (centerCol shr step) else (centerCol shl shiftUp)
        val expectedRow = if (kz <= currentZ) (centerRow shr step) else (centerRow shl shiftUp)
        val dist = abs(kx - expectedCol) + abs(ky - expectedRow)
        return zDiff * 120 + dist * 20
    }

    private fun evictMemoryCacheToBudgetLocked(maxBytes: Long = MAX_CACHE_BYTES, maxTiles: Int = MAX_CACHE_TILES) {
        if (currentCacheBytes.get() <= maxBytes && memoryCache.size <= maxTiles) return
        val currentZ = latestTargetZoom.get()
        val cCol = latestCenterCol.get()
        val cRow = latestCenterRow.get()
        val keysSnapshot = memoryCache.keys.toList()
        val sortedToEvict = keysSnapshot.sortedByDescending { k ->
            tileEvictionScore(k, currentZ, cCol, cRow)
        }
        for (k in sortedToEvict) {
            if (currentCacheBytes.get() <= maxBytes && memoryCache.size <= maxTiles) break
            if (protectedViewportKeys.contains(k)) continue
            removeTileFromMemoryLocked(k)
        }
        // Hard safety ceiling in case protected keys ever exceed maxTiles + 32
        if (memoryCache.size > maxTiles + 32) {
            for (k in sortedToEvict) {
                if (memoryCache.size <= maxTiles + 16) break
                removeTileFromMemoryLocked(k)
            }
        }
    }

    private fun storeInMemory(key: String, bmp: ImageBitmap) {
        if (!isMapOpen.get()) return
        synchronized(cacheLock) {
            if (!isMapOpen.get()) return
            val bmpBytes = (bmp.width * bmp.height * 4).coerceAtLeast(262144)
            val prevBytes = memoryCacheBytesByKey.put(key, bmpBytes) ?: 0
            currentCacheBytes.addAndGet((bmpBytes - prevBytes).toLong())
            memoryCache[key] = bmp
            if (currentCacheBytes.get() > MAX_CACHE_BYTES || memoryCache.size > MAX_CACHE_TILES) {
                evictMemoryCacheToBudgetLocked(MAX_CACHE_BYTES, MAX_CACHE_TILES)
            }
        }
        notifyTileLoaded()
    }

    private fun enqueueDiskJob(job: TileRequestJob, front: Boolean) {
        if (!isMapOpen.get() || memoryCache.containsKey(job.key) || inFlightDisk.contains(job.key) || inFlightNetwork.contains(job.key)) return
        if (diskQueuedKeys.add(job.key)) {
            if (front) {
                diskQueue.addFirst(job)
            } else {
                diskQueue.addLast(job)
            }
        }
    }

    private fun enqueueNetworkJob(job: TileRequestJob, front: Boolean) {
        if (!isMapOpen.get() || memoryCache.containsKey(job.key) || inFlightNetwork.contains(job.key)) return
        if (queuedKeys.add(job.key)) {
            if (front) {
                networkQueue.addFirst(job)
            } else {
                networkQueue.addLast(job)
            }
        }
    }

    /**
     * Immediately pushes target destination high-definition tiles to the very front of the queue
     * when double-tapping or focusing a location, so tiles download while the camera animates.
     */
    fun prewarmTargetDestination(
        cacheDir: File,
        targetLat: Double,
        targetLng: Double,
        targetZoom: Float
    ) {
        if (!isMapOpen.get() || !targetLat.isFinite() || !targetLng.isFinite() || !targetZoom.isFinite()) return
        engineScope.launch {
            if (!isMapOpen.get()) return@launch
            val targetTileZ = computeEffectiveTileZoom(targetZoom)
            latestTargetZoom.set(targetTileZ)
            val urgentList = ArrayList<TileRequestJob>(18)

            val zLevels = intArrayOf(
                targetTileZ,
                (targetTileZ - 1).coerceAtLeast(2),
                (targetTileZ - 2).coerceAtLeast(2)
            )

            for (z in zLevels.distinct()) {
                val n = 1 shl z
                val cCol = floor(lngToTileX(targetLng, z)).toInt()
                val cRow = floor(latToTileY(targetLat, z)).toInt()
                for (dy in -1..1) {
                    val ty = cRow + dy
                    if (ty !in 0 until n) continue
                    for (dx in -1..1) {
                        val tx = ((cCol + dx) % n + n) % n
                        val key = "$z/$tx/$ty"
                        protectedViewportKeys.add(key)
                        if (!memoryCache.containsKey(key)) {
                            urgentList.add(TileRequestJob(z, tx, ty, key, isUrgent = (dx == 0 && dy == 0)))
                        }
                    }
                }
            }

            for (i in urgentList.indices.reversed()) {
                val job = urgentList[i]
                if (!diskCheckedMissing.contains(job.key)) {
                    enqueueDiskJob(job, front = true)
                } else {
                    enqueueNetworkJob(job, front = true)
                }
            }
            ensureDiskWorkers(cacheDir)
            ensureNetworkWorkers(cacheDir)
        }
    }

    fun scheduleViewport(
        cacheDir: File,
        tileZoom: Int,
        exactZoom: Float,
        centerCol: Int,
        centerRow: Int,
        centerLat: Double,
        centerLng: Double,
        screenWidthPx: Float,
        screenHeightPx: Float,
        spots: List<ParkingSpot>
    ) {
        if (!isMapOpen.get() || !exactZoom.isFinite() || !centerLat.isFinite() || !centerLng.isFinite()) return
        val safeTileZoom = tileZoom.coerceIn(2, 20)
        latestTargetZoom.set(safeTileZoom)
        latestCenterCol.set(centerCol)
        latestCenterRow.set(centerRow)
        viewportScheduleJob?.cancel()
        viewportScheduleJob = engineScope.launch {
            if (!isMapOpen.get()) return@launch
            if (diskCheckedMissing.size > 2048) {
                diskCheckedMissing.clear()
            }
            val scale = 2.0.pow((exactZoom - safeTileZoom).toDouble()).toFloat().coerceIn(0.25f, 32f)
            val effectiveTilePx = (256f * scale).coerceIn(128f, 512f)
            val halfCols = (ceilToInt((screenWidthPx / effectiveTilePx) / 2f) + 1).coerceIn(1, 6)
            val halfRows = (ceilToInt((screenHeightPx / effectiveTilePx) / 2f) + 1).coerceIn(1, 9)

            val n = 1 shl safeTileZoom
            val coordsByDist = ArrayList<Pair<Int, Int>>((halfCols * 2 + 1) * (halfRows * 2 + 1))
            for (dy in -halfRows..halfRows) {
                for (dx in -halfCols..halfCols) {
                    coordsByDist.add(dx to dy)
                }
            }
            coordsByDist.sortBy { (dx, dy) -> dx * dx + dy * dy }

            val newProtected = HashSet<String>(coordsByDist.size * 2)
            val desiredJobs = ArrayList<TileRequestJob>(coordsByDist.size + 32)
            val seenKeys = HashSet<String>(coordsByDist.size + 32)

            fun addCandidate(z: Int, tx: Int, ty: Int, isUrgent: Boolean, protectInViewport: Boolean) {
                val key = "$z/$tx/$ty"
                if (protectInViewport) {
                    newProtected.add(key)
                }
                if (seenKeys.add(key) && !memoryCache.containsKey(key)) {
                    desiredJobs.add(TileRequestJob(z, tx, ty, key, isUrgent))
                }
            }

            // 1. Exact current zoom viewport tiles strictly from center outwards (all protected from eviction)
            for ((index, offset) in coordsByDist.withIndex()) {
                val (dx, dy) = offset
                val ty = centerRow + dy
                if (ty !in 0 until n) continue
                val tx = ((centerCol + dx) % n + n) % n
                addCandidate(safeTileZoom, tx, ty, isUrgent = index < 9, protectInViewport = true)
            }

            // 2. Full-viewport ancestor fallback tiles at tileZoom - 1 and tileZoom - 2 so edges NEVER show blank cutouts
            for (step in 1..2) {
                val ancZ = safeTileZoom - step
                if (ancZ < 2) continue
                val ancN = 1 shl ancZ
                for ((dx, dy) in coordsByDist) {
                    val ty = centerRow + dy
                    if (ty !in 0 until n) continue
                    val tx = ((centerCol + dx) % n + n) % n
                    val px = (tx shr step).coerceIn(0, ancN - 1)
                    val py = (ty shr step).coerceIn(0, ancN - 1)
                    addCandidate(ancZ, px, py, isUrgent = false, protectInViewport = true)
                }
            }

            // Also protect existing z - 3 ancestor tiles if already cached
            if (safeTileZoom - 3 >= 2) {
                val ancZ3 = safeTileZoom - 3
                val ancN3 = 1 shl ancZ3
                for ((dx, dy) in coordsByDist) {
                    val ty = centerRow + dy
                    if (ty !in 0 until n) continue
                    val tx = ((centerCol + dx) % n + n) % n
                    val px = (tx shr 3).coerceIn(0, ancN3 - 1)
                    val py = (ty shr 3).coerceIn(0, ancN3 - 1)
                    newProtected.add("$ancZ3/$px/$py")
                }
            }

            protectedViewportKeys.clear()
            protectedViewportKeys.addAll(newProtected)

            // 3. Next zoom-in level center prefetch for zero-lag pinch-zooming
            if (safeTileZoom < 20) {
                val nextZ = safeTileZoom + 1
                val nextN = 1 shl nextZ
                val nextCenterCol = floor(lngToTileX(centerLng, nextZ)).toInt()
                val nextCenterRow = floor(latToTileY(centerLat, nextZ)).toInt()
                val nextOffsets = arrayOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1)
                for ((idx, pair) in nextOffsets.withIndex()) {
                    val ty = nextCenterRow + pair.second
                    if (ty !in 0 until nextN) continue
                    val tx = ((nextCenterCol + pair.first) % nextN + nextN) % nextN
                    addCandidate(nextZ, tx, ty, isUrgent = idx == 0 && safeTileZoom >= 15, protectInViewport = false)
                }
            }

            // 4. Only when already at street zoom (tileZoom >= 15), prefetch 1 tile for the single closest spot
            if (safeTileZoom in 15..18 && spots.isNotEmpty()) {
                val closestSpot = spots.minByOrNull { s ->
                    val dLat = s.latitude - centerLat
                    val dLng = s.longitude - centerLng
                    dLat * dLat + dLng * dLng
                }
                if (closestSpot != null) {
                    val spotZ = (safeTileZoom + 1).coerceAtMost(19)
                    val sN = 1 shl spotZ
                    val sCol = floor(lngToTileX(closestSpot.longitude, spotZ)).toInt().coerceIn(0, sN - 1)
                    val sRow = floor(latToTileY(closestSpot.latitude, spotZ)).toInt().coerceIn(0, sN - 1)
                    addCandidate(spotZ, sCol, sRow, isUrgent = false, protectInViewport = false)
                }
            }

            latestDesiredJobs.set(desiredJobs)

            // Replace both disk and network queues with current priority order so stale intermediate zoom requests never pile up!
            diskQueue.clear()
            diskQueuedKeys.clear()
            networkQueue.clear()
            queuedKeys.clear()
            for (job in desiredJobs) {
                if (!diskCheckedMissing.contains(job.key)) {
                    enqueueDiskJob(job, front = false)
                } else {
                    enqueueNetworkJob(job, front = false)
                }
            }

            ensureDiskWorkers(cacheDir)
            ensureNetworkWorkers(cacheDir)
            scheduleViewportRecoverySweep(cacheDir)
        }
    }

    /**
     * Self-healing viewport sweep: if any visible tile was in-flight on disk during a rapid queue clear
     * or suffered a transient network timeout during fast pinch-zooming, automatically re-enqueues it
     * so no unloaded square can ever persist on screen.
     */
    private fun scheduleViewportRecoverySweep(cacheDir: File) {
        viewportRecoveryJob?.cancel()
        viewportRecoveryJob = engineScope.launch {
            for (pass in 1..2) {
                delay(if (pass == 1) 500L else 1200L)
                val currentDesired = latestDesiredJobs.get()
                var queuedDisk = false
                var queuedNet = false
                for (job in currentDesired) {
                    if (!memoryCache.containsKey(job.key) &&
                        !inFlightDisk.contains(job.key) &&
                        !inFlightNetwork.contains(job.key) &&
                        !diskQueuedKeys.contains(job.key) &&
                        !queuedKeys.contains(job.key)
                    ) {
                        if (!diskCheckedMissing.contains(job.key)) {
                            enqueueDiskJob(job, front = job.isUrgent)
                            queuedDisk = true
                        } else {
                            enqueueNetworkJob(job, front = job.isUrgent)
                            queuedNet = true
                        }
                    }
                }
                if (queuedDisk) ensureDiskWorkers(cacheDir)
                if (queuedNet) ensureNetworkWorkers(cacheDir)
            }
        }
    }

    private fun ensureDiskWorkers(cacheDir: File) {
        if (!isMapOpen.get()) {
            diskQueue.clear()
            diskQueuedKeys.clear()
            return
        }
        while (isMapOpen.get() && diskQueue.isNotEmpty() && activeDiskWorkers.get() < 3) {
            if (activeDiskWorkers.incrementAndGet() <= 3) {
                engineScope.launch(diskReadDispatcher) {
                    try {
                        while (isMapOpen.get()) {
                            val job = diskQueue.pollFirst() ?: break
                            diskQueuedKeys.remove(job.key)
                            if (!isMapOpen.get() || memoryCache.containsKey(job.key)) continue
                            val targetZ = latestTargetZoom.get()
                            if (!job.isUrgent && (job.z < targetZ - 2 || job.z > targetZ + 1)) {
                                continue
                            }
                            if (!inFlightDisk.add(job.key)) continue
                            try {
                                val tileFile = File(cacheDir, "$DISK_CACHE_FOLDER/${job.z}/${job.x}/${job.y}.webp")
                                if (tileFile.exists() && tileFile.length() > 160) {
                                    val opts = BitmapFactory.Options().apply {
                                        inPreferredConfig = Bitmap.Config.ARGB_8888
                                        inPremultiplied = true
                                    }
                                    val cached = BitmapFactory.decodeFile(tileFile.absolutePath, opts)
                                    if (cached != null) {
                                        if (isMapOpen.get()) {
                                            storeInMemory(job.key, cached.asImageBitmap())
                                        } else {
                                            try { cached.recycle() } catch (_: Throwable) {}
                                        }
                                        continue
                                    }
                                }
                                if (isMapOpen.get()) {
                                    diskCheckedMissing.add(job.key)
                                    enqueueNetworkJob(job, front = job.isUrgent)
                                    ensureNetworkWorkers(cacheDir)
                                }
                            } catch (_: Throwable) {
                                if (isMapOpen.get()) {
                                    diskCheckedMissing.add(job.key)
                                    enqueueNetworkJob(job, front = job.isUrgent)
                                    ensureNetworkWorkers(cacheDir)
                                }
                            } finally {
                                inFlightDisk.remove(job.key)
                            }
                        }
                    } finally {
                        activeDiskWorkers.decrementAndGet()
                        if (isMapOpen.get() && diskQueue.isNotEmpty() && activeDiskWorkers.get() == 0) {
                            ensureDiskWorkers(cacheDir)
                        }
                    }
                }
            } else {
                activeDiskWorkers.decrementAndGet()
                break
            }
        }
    }

    private fun ensureNetworkWorkers(cacheDir: File) {
        if (!isMapOpen.get()) {
            networkQueue.clear()
            queuedKeys.clear()
            return
        }
        while (isMapOpen.get() && networkQueue.isNotEmpty() && activeNetworkWorkers.get() < 6) {
            if (activeNetworkWorkers.incrementAndGet() <= 6) {
                engineScope.launch(networkDispatcher) {
                    try {
                        while (isMapOpen.get()) {
                            val job = networkQueue.pollFirst() ?: break
                            queuedKeys.remove(job.key)
                            if (!isMapOpen.get() || memoryCache.containsKey(job.key)) continue
                            val targetZ = latestTargetZoom.get()
                            if (!job.isUrgent && (job.z < targetZ - 2 || job.z > targetZ + 1)) {
                                continue
                            }
                            if (!inFlightNetwork.add(job.key)) continue
                            try {
                                val styledBmp = downloadAndStyleTile(cacheDir, job)
                                if (styledBmp != null) {
                                    if (isMapOpen.get()) {
                                        storeInMemory(job.key, styledBmp.asImageBitmap())
                                        saveTileToDiskBounded(cacheDir, job, styledBmp)
                                    } else {
                                        try { styledBmp.recycle() } catch (_: Throwable) {}
                                    }
                                }
                            } catch (_: Throwable) {
                            } finally {
                                inFlightNetwork.remove(job.key)
                            }
                        }
                    } finally {
                        activeNetworkWorkers.decrementAndGet()
                        if (isMapOpen.get() && networkQueue.isNotEmpty() && activeNetworkWorkers.get() == 0) {
                            ensureNetworkWorkers(cacheDir)
                        }
                    }
                }
            } else {
                activeNetworkWorkers.decrementAndGet()
                break
            }
        }
    }

    private fun saveTileToDiskBounded(cacheDir: File, job: TileRequestJob, styledBmp: Bitmap) {
        // Bound pending background disk writes to at most 16 so Bitmaps never queue up in RAM during fast panning
        if (pendingDiskWrites.get() >= 16) return
        val w = styledBmp.width
        val h = styledBmp.height
        if (w <= 0 || h <= 0) return
        val pixelsCopy = try {
            val arr = IntArray(w * h)
            styledBmp.getPixels(arr, 0, w, 0, 0, w, h)
            arr
        } catch (_: Throwable) {
            return
        }
        pendingDiskWrites.incrementAndGet()
        engineScope.launch(diskWriteDispatcher) {
            var tempBmp: Bitmap? = null
            try {
                tempBmp = Bitmap.createBitmap(pixelsCopy, w, h, Bitmap.Config.ARGB_8888)
                val tileFile = File(cacheDir, "$DISK_CACHE_FOLDER/${job.z}/${job.x}/${job.y}.webp")
                tileFile.parentFile?.mkdirs()
                tileFile.outputStream().buffered(8192).use { out ->
                    @Suppress("DEPRECATION")
                    val format = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        Bitmap.CompressFormat.WEBP
                    }
                    tempBmp.compress(format, 85, out)
                }
                diskCheckedMissing.remove(job.key)
            } catch (_: Throwable) {
            } finally {
                try {
                    tempBmp?.recycle()
                } catch (_: Throwable) {}
                pendingDiskWrites.decrementAndGet()
            }
        }
    }

    private fun cacheRaw19PixelsInMemory(key19: String, pixels256: IntArray) {
        if (!isMapOpen.get()) return
        while (raw19PixelsCache.size >= MAX_RAW19_TILES) {
            val evictKey = raw19PixelsCache.keys.firstOrNull() ?: break
            raw19PixelsCache.remove(evictKey)
        }
        raw19PixelsCache[key19] = pixels256
    }

    private fun getOrFetchRaw19Pixels(cacheDir: File, x19: Int, y19: Int, osmSub: String): IntArray? {
        val key19 = "19/$x19/$y19"
        raw19PixelsCache[key19]?.let { return it }

        val rawFile = File(cacheDir, "$RAW19_CACHE_FOLDER/$x19/$y19.png")
        if (rawFile.exists() && rawFile.length() > 160) {
            try {
                val opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inPremultiplied = true
                }
                val diskBmp = BitmapFactory.decodeFile(rawFile.absolutePath, opts)
                if (diskBmp != null) {
                    if (diskBmp.width == 256 && diskBmp.height == 256) {
                        val arr = IntArray(65536)
                        diskBmp.getPixels(arr, 0, 256, 0, 0, 256, 256)
                        diskBmp.recycle()
                        cacheRaw19PixelsInMemory(key19, arr)
                        return arr
                    }
                    diskBmp.recycle()
                }
            } catch (_: Throwable) {
            }
        }

        val fetched = fetchRawTileFromUrl("https://$osmSub.tile.openstreetmap.org/19/$x19/$y19.png", timeoutMs = 1900)
            ?: fetchRawTileFromUrl("https://tile.openstreetmap.org/19/$x19/$y19.png", timeoutMs = 2100)

        if (fetched != null) {
            if (fetched.width == 256 && fetched.height == 256) {
                val arr = IntArray(65536)
                fetched.getPixels(arr, 0, 256, 0, 0, 256, 256)
                cacheRaw19PixelsInMemory(key19, arr)
                if (pendingDiskWrites.get() < 16) {
                    pendingDiskWrites.incrementAndGet()
                    engineScope.launch(diskWriteDispatcher) {
                        try {
                            rawFile.parentFile?.mkdirs()
                            rawFile.outputStream().buffered(8192).use { out ->
                                fetched.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                        } catch (_: Throwable) {
                        } finally {
                            try {
                                fetched.recycle()
                            } catch (_: Throwable) {}
                            pendingDiskWrites.decrementAndGet()
                        }
                    }
                } else {
                    try {
                        fetched.recycle()
                    } catch (_: Throwable) {}
                }
                return arr
            } else {
                try {
                    fetched.recycle()
                } catch (_: Throwable) {}
            }
        }
        return null
    }

    private suspend fun downloadAndStyleTile(cacheDir: File, job: TileRequestJob): Bitmap? {
        val z = job.z
        val x = job.x
        val y = job.y
        val targetZ = latestTargetZoom.get()
        if (!job.isUrgent && (z < targetZ - 2 || z > targetZ + 1)) return null

        val shard = (x + y + z).and(0x7FFFFFFF)
        val osmSub = when (shard % 3) {
            0 -> "a"
            1 -> "b"
            else -> "c"
        }
        val altSub = when ((shard + 1) % 3) {
            0 -> "a"
            1 -> "b"
            else -> "c"
        }

        if (z >= 20) {
            val stepFrom19 = (z - 19).coerceIn(1, 2)
            val px19 = x shr stepFrom19
            val py19 = y shr stepFrom19
            val raw19Pixels = getOrFetchRaw19Pixels(cacheDir, px19, py19, osmSub) ?: return null
            return withContext(tileComputeDispatcher) {
                styleAndSuperResolveFromRaw19Pixels(raw19Pixels, x, y, stepFrom19)
            }
        }

        if (z == 19) {
            val raw19Pixels = getOrFetchRaw19Pixels(cacheDir, x, y, osmSub) ?: return null
            return withContext(tileComputeDispatcher) {
                styleOsmPixels256HighDef(raw19Pixels, 19)
            }
        }

        val urls = arrayOf(
            "https://$osmSub.tile.openstreetmap.org/$z/$x/$y.png",
            "https://tile.openstreetmap.org/$z/$x/$y.png",
            "https://$altSub.tile.openstreetmap.org/$z/$x/$y.png"
        )
        for ((attempt, urlStr) in urls.withIndex()) {
            if (!job.isUrgent && abs(z - latestTargetZoom.get()) > 2) return null
            val raw = fetchRawTileFromUrl(urlStr, timeoutMs = 1900)
            if (raw != null) {
                return withContext(tileComputeDispatcher) {
                    try {
                        styleOsmBitmapHighDef(raw, z)
                    } finally {
                        try {
                            raw.recycle()
                        } catch (_: Throwable) {}
                    }
                }
            }
            if (attempt < urls.lastIndex) {
                delay(60L * (attempt + 1))
            }
        }
        return null
    }

    private fun fetchRawTileFromUrl(urlStr: String, timeoutMs: Int): Bitmap? {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                useCaches = true
                setRequestProperty("User-Agent", "PixelParking/0.75 (Android; HighDef Slippy Map)")
                setRequestProperty("Accept", "image/webp,image/png,image/*;q=0.8")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val decodeOptions = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inPremultiplied = true
                }
                return conn.inputStream.buffered(16384).use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                }
            }
        } catch (_: Throwable) {
        } finally {
            conn?.disconnect()
        }
        return null
    }

    /**
     * Styles the full 256x256 z=19 tile with 3x3 Halo-Aware Local-Contrast Cartographic Shader,
     * crops the target z=20 (128x128) quadrant, and runs Diagonal Edge-Directed 2x Super-Resolution
     * + Contrast Edge-Tightening up to 256x256.
     * Reuses threadLocalCrop128/256 buffers so zero temporary IntArrays are allocated on the heap.
     */
    private fun styleAndSuperResolveFromRaw19Pixels(
        raw19Pixels: IntArray,
        tileX: Int,
        tileY: Int,
        stepFrom19: Int
    ): Bitmap {
        val full256 = threadLocalPixels256.get() ?: IntArray(65536)
        System.arraycopy(raw19Pixels, 0, full256, 0, 65536)
        styleOsmPixelsHaloAware(full256, 256, 256)

        val mask = (1 shl stepFrom19) - 1
        val cropSize = (256 shr stepFrom19).coerceAtLeast(64)
        val qx = ((tileX and mask) * cropSize).coerceIn(0, 256 - cropSize)
        val qy = ((tileY and mask) * cropSize).coerceIn(0, 256 - cropSize)

        var w = cropSize
        var h = cropSize
        var pixels = threadLocalCrop128.get() ?: IntArray(128 * 128)
        for (row in 0 until h) {
            System.arraycopy(full256, (qy + row) * 256 + qx, pixels, row * w, w)
        }

        while (w < 256 && h < 256) {
            val nextW = w shl 1
            val nextH = h shl 1
            val nextPixels = if (nextW == 128 && nextH == 128) {
                threadLocalLum256.get() ?: IntArray(65536)
            } else {
                threadLocalPixels256.get() ?: IntArray(65536)
            }
            upscaleEdgeDirected2x(pixels, w, h, nextPixels)
            tightenEdgesInPlace(nextPixels, nextW, nextH)
            pixels = nextPixels
            w = nextW
            h = nextH
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun styleOsmPixels256HighDef(raw256: IntArray, zoomLevel: Int): Bitmap {
        val pixels256 = threadLocalPixels256.get() ?: IntArray(65536)
        System.arraycopy(raw256, 0, pixels256, 0, 65536)
        styleOsmPixelsHaloAware(pixels256, 256, 256)
        if (zoomLevel >= 17) {
            tightenEdgesInPlace(pixels256, 256, 256)
        }
        return Bitmap.createBitmap(pixels256, 256, 256, Bitmap.Config.ARGB_8888)
    }

    /**
     * 3x3 Halo-Aware Local-Contrast Cartographic Shader + Edge-Tightening at z >= 17 (256x256).
     */
    private fun styleOsmBitmapHighDef(source: Bitmap, zoomLevel: Int): Bitmap {
        val w = source.width
        val h = source.height
        val total = w * h
        val pixels256 = if (total == 65536) {
            threadLocalPixels256.get() ?: IntArray(65536)
        } else {
            IntArray(total)
        }
        source.getPixels(pixels256, 0, w, 0, 0, w, h)

        styleOsmPixelsHaloAware(pixels256, w, h)

        if (zoomLevel >= 17 && w == 256 && h == 256) {
            tightenEdgesInPlace(pixels256, 256, 256)
        }

        return Bitmap.createBitmap(pixels256, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * 3x3 Neighborhood Halo-Aware & Local-Contrast Cartographic Shader:
     * 1. Preserves & boosts ALL foreground details (forest tree icons, swimming/sports icons, benches,
     *    waste bins, Greek/Latin text strokes, building outlines, and road casings) by measuring local
     *    3x3 luminance dips (`localDip = ringAvgLum - lum`).
     * 2. Inverts bright white text/icon halos (`lum >= 205` bordering dark cores `minLum3x3 < 150`) into
     *    crisp dark navy halos (`#0E141E`) so every letter and icon pops out with high contrast.
     * 3. Uses 100% smooth continuous blending with zero hard `r >= 155` or `lum >= 115` jumps so
     *    diagonal roads (`Θεμιστοκλή Σοφούλη`) have zero speckled dots.
     */
    private fun styleOsmPixelsHaloAware(pixels: IntArray, w: Int, h: Int) {
        val total = w * h
        val srcLum = if (total == 65536) {
            threadLocalLum256.get() ?: IntArray(65536)
        } else {
            IntArray(total)
        }

        var idx = 0
        while (idx < total) {
            val c = pixels[idx]
            val r = (c ushr 16) and 0xFF
            val g = (c ushr 8) and 0xFF
            val b = c and 0xFF
            srcLum[idx] = (r * 77 + g * 150 + b * 29) shr 8
            idx++
        }

        val nR = NEUTRAL_RAMP_R
        val nG = NEUTRAL_RAMP_G
        val nB = NEUTRAL_RAMP_B
        val wR = WARM_RAMP_R
        val wG = WARM_RAMP_G
        val wB = WARM_RAMP_B
        val maxX = w - 1
        val maxY = h - 1

        var y = 0
        while (y < h) {
            val rowOff = y * w
            val prevRow = if (y > 0) (y - 1) * w else rowOff
            val nextRow = if (y < maxY) (y + 1) * w else rowOff

            var x = 0
            while (x < w) {
                val i = rowOff + x
                val c = pixels[i]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                val lum = srcLum[i]

                val prevX = if (x > 0) x - 1 else 0
                val nextX = if (x < maxX) x + 1 else maxX

                val lUL = srcLum[prevRow + prevX]
                val lU = srcLum[prevRow + x]
                val lUR = srcLum[prevRow + nextX]
                val lL = srcLum[rowOff + prevX]
                val lR = srcLum[rowOff + nextX]
                val lDL = srcLum[nextRow + prevX]
                val lD = srcLum[nextRow + x]
                val lDR = srcLum[nextRow + nextX]

                val ringSum = lUL + lU + lUR + lL + lR + lDL + lD + lDR
                val ringAvgLum = ringSum shr 3
                val localDip = ringAvgLum - lum // > 0 when pixel is darker than its surroundings (stroke/icon/tree/casing)

                var minLum3x3 = lum
                if (lUL < minLum3x3) minLum3x3 = lUL
                if (lU < minLum3x3) minLum3x3 = lU
                if (lUR < minLum3x3) minLum3x3 = lUR
                if (lL < minLum3x3) minLum3x3 = lL
                if (lR < minLum3x3) minLum3x3 = lR
                if (lDL < minLum3x3) minLum3x3 = lDL
                if (lD < minLum3x3) minLum3x3 = lD
                if (lDR < minLum3x3) minLum3x3 = lDR

                // Boost effective foreground darkness when pixel is locally darker than neighbors (e.g. anti-aliased
                // letters, small POI icons, road casings, building borders) so they map to high-visibility bright tones
                val effectiveLum = if (localDip > 4 && lum < 210) {
                    (lum - localDip * 2).coerceIn(0, 255)
                } else {
                    lum
                }

                // 1. Base continuous blend between Neutral ramp and Warm ramp
                val warmWeight = ((r - b - 2) * 24).coerceIn(0, 256)
                val invWarm = 256 - warmWeight

                var outR = (nR[effectiveLum] * invWarm + wR[effectiveLum] * warmWeight) shr 8
                var outG = (nG[effectiveLum] * invWarm + wG[effectiveLum] * warmWeight) shr 8
                var outB = (nB[effectiveLum] * invWarm + wB[effectiveLum] * warmWeight) shr 8

                // 2. Smooth Chromatic Feature Shading (Water, Parks & Forest Trees/Labels, Warm Arterial Roads)
                val blueExcess = max(b - max(r, g - 10), (b + g) / 2 - r - 12)
                val greenExcess = g - max(r + 3, b + 2)
                val warmExcess = r - b

                if (blueExcess > 6) {
                    val wBlue = ((blueExcess - 6) * 14).coerceIn(0, 256)
                    if (lum >= 155 && localDip <= 8) {
                        // Water body background fill — matches exact Canvas waterColor RGB(26, 38, 57) (#1A2639)
                        // so sea tiles and the background canvas blend with zero square seams!
                        val targetR = 26
                        val targetG = 38
                        val targetB = 57
                        outR = (outR * (256 - wBlue) + targetR * wBlue) shr 8
                        outG = (outG * (256 - wBlue) + targetG * wBlue) shr 8
                        outB = (outB * (256 - wBlue) + targetB * wBlue) shr 8
                    } else {
                        // Blue transit/parking/water label or icon -> luminous ice-sky blue
                        val fgBoost = ((175 - effectiveLum) * 2).coerceIn(0, 110)
                        val targetR = (135 + fgBoost).coerceIn(135, 235)
                        val targetG = (185 + (fgBoost * 2) / 3).coerceIn(185, 248)
                        val targetB = 252
                        outR = (outR * (256 - wBlue) + targetR * wBlue) shr 8
                        outG = (outG * (256 - wBlue) + targetG * wBlue) shr 8
                        outB = (outB * (256 - wBlue) + targetB * wBlue) shr 8
                    }
                } else if (greenExcess > 5) {
                    val wGreen = ((greenExcess - 5) * 14).coerceIn(0, 256)
                    // Distinguish background park/forest fill vs green foreground (tree icons #689956, swimmer icon, green park text)
                    // Forest background in OSM has lum ~ 188..235 and localDip <= 4.
                    // Tree icons (#689956) have lum ~ 132..162 and localDip >= 6.
                    // Green park/pool labels have lum ~ 55..155 and localDip >= 8.
                    val isGreenForeground = lum < 168 || localDip >= 6
                    if (!isGreenForeground) {
                        val shade = ((lum - 195) * 16) / 100
                        val targetR = (24 + shade).coerceIn(18, 40)
                        val targetG = (50 + shade).coerceIn(38, 68)
                        val targetB = (43 + shade).coerceIn(32, 58)
                        outR = (outR * (256 - wGreen) + targetR * wGreen) shr 8
                        outG = (outG * (256 - wGreen) + targetG * wGreen) shr 8
                        outB = (outB * (256 - wGreen) + targetB * wGreen) shr 8
                    } else {
                        // Crisp luminous mint-emerald for forest tree icons (lum ~ 138) and bright mint-white for green text (lum < 125)
                        val fgStrength = ((174 - lum) + localDip * 3).coerceIn(12, 180)
                        val targetR = (84 + (fgStrength * 85) / 100).coerceIn(92, 236)
                        val targetG = (176 + (fgStrength * 44) / 100).coerceIn(184, 254)
                        val targetB = (142 + (fgStrength * 55) / 100).coerceIn(150, 240)
                        outR = (outR * (256 - wGreen) + targetR * wGreen) shr 8
                        outG = (outG * (256 - wGreen) + targetG * wGreen) shr 8
                        outB = (outB * (256 - wGreen) + targetB * wGreen) shr 8
                    }
                } else if (warmExcess > 16 && lum >= 120 && localDip < 18) {
                    // Smooth continuous arterial highway / secondary road / tan area shading (ZERO step cutoffs!)
                    val chromaW = ((warmExcess - 16) * 9).coerceIn(0, 256)
                    val lumW = ((lum - 120) * 4).coerceIn(0, 256)
                    val wRoad = (chromaW * lumW) shr 8
                    // Smoothly vary warmth with (r - g) and luminance so anti-aliased road borders never speckle
                    val orangeTone = ((r - g - 6) * 3).coerceIn(0, 32)
                    val lumMod = ((lum - 190) * 22) / 100
                    val targetR = (126 + orangeTone + lumMod).coerceIn(96, 168)
                    val targetG = (100 + orangeTone / 2 + lumMod).coerceIn(76, 132)
                    val targetB = (72 + lumMod).coerceIn(54, 96)
                    outR = (outR * (256 - wRoad) + targetR * wRoad) shr 8
                    outG = (outG * (256 - wRoad) + targetG * wRoad) shr 8
                    outB = (outB * (256 - wRoad) + targetB * wRoad) shr 8
                }

                // 3. White Text/Icon Halo Inversion & Casing Contrast Enhancer:
                // In OSM tiles, dark text strokes and POI icons (minLum3x3 < 152) are surrounded by a bright halo (lum >= 204).
                // Darkening this halo to a deep navy-slate outline (#101622) makes every letter and icon razor-sharp!
                if (lum >= 202 && minLum3x3 < 152 && (lum - minLum3x3) >= 38) {
                    val haloStrength = (((lum - 200) * 5).coerceIn(0, 256) * ((156 - minLum3x3) * 3).coerceIn(0, 256)) shr 8
                    val clampedHalo = haloStrength.coerceIn(0, 220)
                    outR = (outR * (256 - clampedHalo) + 16 * clampedHalo) shr 8
                    outG = (outG * (256 - clampedHalo) + 22 * clampedHalo) shr 8
                    outB = (outB * (256 - clampedHalo) + 32 * clampedHalo) shr 8
                }

                pixels[i] = (0xFF shl 24) or (outR.coerceIn(0, 255) shl 16) or (outG.coerceIn(0, 255) shl 8) or outB.coerceIn(0, 255)
                x++
            }
            y++
        }
    }

    /**
     * Fast 3x3 Local Min/Max Edge-Tightening Pass (Shock / Sigmoidal Contrast Filter without ringing).
     * Pulls blurry upscaled transition pixels on letters, icons, road casings, and building walls
     * toward the local 3x3 minimum or maximum, eliminating bilinear softness at high zoom.
     */
    private fun tightenEdgesInPlace(pixels: IntArray, w: Int, h: Int) {
        val total = w * h
        val src = if (total <= 256 * 256) {
            val buf = threadLocalScratch256.get() ?: IntArray(total)
            System.arraycopy(pixels, 0, buf, 0, total)
            buf
        } else {
            pixels.clone()
        }

        val maxX = w - 1
        val maxY = h - 1
        var y = 1
        while (y < maxY) {
            val rowOff = y * w
            val prevRow = (y - 1) * w
            val nextRow = (y + 1) * w
            var x = 1
            while (x < maxX) {
                val i = rowOff + x
                val c = src[i]
                val r = (c ushr 16) and 0xFF
                val g = (c ushr 8) and 0xFF
                val b = c and 0xFF
                val lum = (r * 77 + g * 150 + b * 29) shr 8

                val cU = src[prevRow + x]
                val cD = src[nextRow + x]
                val cL = src[rowOff + x - 1]
                val cR = src[rowOff + x + 1]
                val cUL = src[prevRow + x - 1]
                val cUR = src[prevRow + x + 1]
                val cDL = src[nextRow + x - 1]
                val cDR = src[nextRow + x + 1]

                var minC = c
                var maxC = c
                var minLum = lum
                var maxLum = lum

                fun checkNeighbor(nc: Int) {
                    val nr = (nc ushr 16) and 0xFF
                    val ng = (nc ushr 8) and 0xFF
                    val nb = nc and 0xFF
                    val nLum = (nr * 77 + ng * 150 + nb * 29) shr 8
                    if (nLum < minLum) {
                        minLum = nLum
                        minC = nc
                    }
                    if (nLum > maxLum) {
                        maxLum = nLum
                        maxC = nc
                    }
                }

                checkNeighbor(cU)
                checkNeighbor(cD)
                checkNeighbor(cL)
                checkNeighbor(cR)
                checkNeighbor(cUL)
                checkNeighbor(cUR)
                checkNeighbor(cDL)
                checkNeighbor(cDR)

                val span = maxLum - minLum
                if (span >= 22) {
                    val distToMin = lum - minLum
                    val distToMax = maxLum - lum
                    val targetC = if (distToMax <= distToMin) maxC else minC
                    // Pull transition pixel 44% toward the sharper side of the edge (zero overshoot!)
                    val tr = (targetC ushr 16) and 0xFF
                    val tg = (targetC ushr 8) and 0xFF
                    val tb = targetC and 0xFF
                    val outR = (r * 144 + tr * 112) shr 8
                    val outG = (g * 144 + tg * 112) shr 8
                    val outB = (b * 144 + tb * 112) shr 8
                    pixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
                }
                x++
            }
            y++
        }
    }

    /**
     * Diagonal-Aware 2x Edge-Directed Contour Upscaler (w x h -> 2w x 2h).
     * Eliminates 90-degree pixel stair-stepping on diagonal streets, building outlines, and text glyphs
     * by detecting 45-degree contours and interpolating along the diagonal edge rather than across it.
     */
    private fun upscaleEdgeDirected2x(src: IntArray, w: Int, h: Int, dst: IntArray) {
        val dstW = w shl 1
        val maxX = w - 1
        val maxY = h - 1

        fun colorDiff(c1: Int, c2: Int): Int {
            val dr = abs(((c1 ushr 16) and 0xFF) - ((c2 ushr 16) and 0xFF))
            val dg = abs(((c1 ushr 8) and 0xFF) - ((c2 ushr 8) and 0xFF))
            val db = abs((c1 and 0xFF) - (c2 and 0xFF))
            return dr + dg + db
        }

        fun avg2(c1: Int, c2: Int): Int {
            val r = (((c1 ushr 16) and 0xFF) + ((c2 ushr 16) and 0xFF)) shr 1
            val g = (((c1 ushr 8) and 0xFF) + ((c2 ushr 8) and 0xFF)) shr 1
            val b = ((c1 and 0xFF) + (c2 and 0xFF)) shr 1
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        fun avg4(c1: Int, c2: Int, c3: Int, c4: Int): Int {
            val r = (((c1 ushr 16) and 0xFF) + ((c2 ushr 16) and 0xFF) + ((c3 ushr 16) and 0xFF) + ((c4 ushr 16) and 0xFF)) shr 2
            val g = (((c1 ushr 8) and 0xFF) + ((c2 ushr 8) and 0xFF) + ((c3 ushr 8) and 0xFF) + ((c4 ushr 8) and 0xFF)) shr 2
            val b = ((c1 and 0xFF) + (c2 and 0xFF) + (c3 and 0xFF) + (c4 and 0xFF)) shr 2
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        fun blendCorner(main: Int, n1: Int, n2: Int): Int {
            // 50% main + 25% n1 + 25% n2 to round off 90-degree diagonal stair-steps (EPX/Scale2x smoothing)
            val r = (((main ushr 16) and 0xFF) * 2 + ((n1 ushr 16) and 0xFF) + ((n2 ushr 16) and 0xFF)) shr 2
            val g = (((main ushr 8) and 0xFF) * 2 + ((n1 ushr 8) and 0xFF) + ((n2 ushr 8) and 0xFF)) shr 2
            val b = ((main and 0xFF) * 2 + (n1 and 0xFF) + (n2 and 0xFF)) shr 2
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        var y = 0
        while (y < h) {
            val rowOff = y * w
            val prevRowOff = if (y > 0) (y - 1) * w else rowOff
            val nextRowOff = if (y < maxY) (y + 1) * w else rowOff
            val dstRow0 = (y shl 1) * dstW
            val dstRow1 = dstRow0 + dstW

            var x = 0
            while (x < w) {
                val prevX = if (x > 0) x - 1 else 0
                val nextX = if (x < maxX) x + 1 else maxX

                val a = src[rowOff + x]
                val b = src[rowOff + nextX]
                val c = src[nextRowOff + x]
                val d = src[nextRowOff + nextX]
                val u = src[prevRowOff + x]
                val l = src[rowOff + prevX]

                val dstX = x shl 1

                // 1. Top-Left (2x, 2y): Smooth diagonal corner if Up and Left agree and differ from A
                val diffUL = colorDiff(u, l)
                val diffAU = colorDiff(a, u)
                val diffAL = colorDiff(a, l)
                dst[dstRow0 + dstX] = if (diffUL * 3 < (diffAU + diffAL) && diffAU > 18 && diffAL > 18) {
                    blendCorner(a, u, l)
                } else {
                    a
                }

                // 2. Top-Right (2x + 1, 2y): Horizontal edge interpolation with diagonal corner rounding
                val diffUR = colorDiff(u, b)
                val diffAB = colorDiff(a, b)
                dst[dstRow0 + dstX + 1] = if (diffUR * 3 < (diffAU + diffAB) && diffAU > 18 && diffAB > 18) {
                    blendCorner(avg2(a, b), u, b)
                } else {
                    avg2(a, b)
                }

                // 3. Bottom-Left (2x, 2y + 1): Vertical edge interpolation with diagonal corner rounding
                val diffDL = colorDiff(c, l)
                val diffAC = colorDiff(a, c)
                dst[dstRow1 + dstX] = if (diffDL * 3 < (diffAC + diffAL) && diffAC > 18 && diffAL > 18) {
                    blendCorner(avg2(a, c), c, l)
                } else {
                    avg2(a, c)
                }

                // 4. Bottom-Right (2x + 1, 2y + 1): Diagonal Edge-Directed Interpolation (AD vs BC contour)
                val diagAD = colorDiff(a, d)
                val diagBC = colorDiff(b, c)
                dst[dstRow1 + dstX + 1] = when {
                    diagAD * 4 < diagBC * 3 -> avg2(a, d)
                    diagBC * 4 < diagAD * 3 -> avg2(b, c)
                    else -> avg4(a, b, c, d)
                }

                x++
            }
            y++
        }
    }
}

// Web Mercator projection helpers
private fun lngToTileX(lng: Double, zoom: Int): Double {
    val n = (1 shl zoom).toDouble()
    return ((lng + 180.0) / 360.0) * n
}

private fun latToTileY(lat: Double, zoom: Int): Double {
    val clampedLat = lat.coerceIn(-85.0511, 85.0511)
    val rad = clampedLat * PI / 180.0
    val n = (1 shl zoom).toDouble()
    return (1.0 - ln(tan(rad) + 1.0 / cos(rad)) / PI) / 2.0 * n
}

internal fun latLngToScreenPx(
    lat: Double,
    lng: Double,
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    viewportCenterX: Float,
    viewportCenterY: Float
): Offset {
    val worldSize = 256.0 * 2.0.pow(zoom.toDouble())
    val cx = ((centerLng + 180.0) / 360.0) * worldSize
    val cLatRad = centerLat.coerceIn(-85.0511, 85.0511) * PI / 180.0
    val cy = (1.0 - ln(tan(cLatRad) + 1.0 / cos(cLatRad)) / PI) / 2.0 * worldSize

    val px = ((lng + 180.0) / 360.0) * worldSize
    val latRad = lat.coerceIn(-85.0511, 85.0511) * PI / 180.0
    val py = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * worldSize

    return Offset(
        x = viewportCenterX + (px - cx).toFloat(),
        y = viewportCenterY + (py - cy).toFloat()
    )
}

internal fun screenPxToLatLng(
    screenX: Float,
    screenY: Float,
    centerLat: Double,
    centerLng: Double,
    zoom: Float,
    viewportCenterX: Float,
    viewportCenterY: Float
): Pair<Double, Double> {
    val worldSize = 256.0 * 2.0.pow(zoom.toDouble())
    val cx = ((centerLng + 180.0) / 360.0) * worldSize
    val cLatRad = centerLat.coerceIn(-85.0511, 85.0511) * PI / 180.0
    val cy = (1.0 - ln(tan(cLatRad) + 1.0 / cos(cLatRad)) / PI) / 2.0 * worldSize

    val wx = cx + (screenX - viewportCenterX)
    val wy = (cy + (screenY - viewportCenterY)).coerceIn(0.0, worldSize)

    val lng = (wx / worldSize) * 360.0 - 180.0
    val n = PI - 2.0 * PI * wy / worldSize
    val lat = 180.0 / PI * atan(sinh(n))

    return Pair(lat.coerceIn(-85.0, 85.0), normalizeLng(lng))
}

private fun normalizeLng(lng: Double): Double {
    if (!lng.isFinite()) return 20.8537
    var result = lng % 360.0
    if (result > 180.0) result -= 360.0
    if (result < -180.0) result += 360.0
    return if (result.isFinite()) result else 20.8537
}

private fun safeRoundToInt(v: Float): Int {
    if (!v.isFinite()) return 0
    return v.coerceIn(-1_000_000f, 1_000_000f).roundToInt()
}

private fun ceilToInt(v: Float): Int {
    if (!v.isFinite()) return 1
    return kotlin.math.ceil(v.coerceIn(-1_000_000f, 1_000_000f).toDouble()).toInt()
}

internal fun computeInitialCamera(
    spots: List<ParkingSpot>,
    userLat: Double?,
    userLng: Double?
): Triple<Double, Double, Float> {
    if (spots.isEmpty()) {
        if (userLat != null && userLng != null) {
            return Triple(userLat, userLng, 12.5f)
        }
        // Default overview over Greece / Southeastern Europe matching screenshots
        return Triple(39.6650, 20.8537, 6.2f)
    }
    if (spots.size == 1) {
        return Triple(spots[0].latitude, spots[0].longitude, 13.5f)
    }

    var minLat = Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    var minLng = Double.MAX_VALUE
    var maxLng = -Double.MAX_VALUE

    for (spot in spots) {
        minLat = min(minLat, spot.latitude)
        maxLat = max(maxLat, spot.latitude)
        minLng = min(minLng, spot.longitude)
        maxLng = max(maxLng, spot.longitude)
    }

    val centerLat = (minLat + maxLat) / 2.0
    val centerLng = (minLng + maxLng) / 2.0
    val latSpan = (maxLat - minLat).coerceAtLeast(0.002)
    val lngSpan = (maxLng - minLng).coerceAtLeast(0.002)
    val maxSpan = max(latSpan, lngSpan)

    val computedZoom = when {
        maxSpan > 25.0 -> 3.8f
        maxSpan > 10.0 -> 5.0f
        maxSpan > 4.0 -> 6.2f
        maxSpan > 1.5 -> 7.6f
        maxSpan > 0.5 -> 9.2f
        maxSpan > 0.15 -> 11.0f
        maxSpan > 0.04 -> 12.6f
        else -> 14.2f
    }

    return Triple(centerLat, centerLng, computedZoom)
}

private fun formatSpotDateRange(spots: List<ParkingSpot>, isGreek: Boolean): String {
    if (spots.isEmpty()) {
        return if (isGreek) "Δεν υπάρχουν τοποθεσίες σε αυτή την περιοχή" else "No saved locations in this area"
    }
    val minTime = spots.minOf { it.timestamp }
    val maxTime = spots.maxOf { it.timestamp }
    val locale = if (isGreek) Locale("el", "GR") else Locale.US

    val calMin = Calendar.getInstance().apply { timeInMillis = minTime }
    val calMax = Calendar.getInstance().apply { timeInMillis = maxTime }

    val sameYear = calMin.get(Calendar.YEAR) == calMax.get(Calendar.YEAR)
    val sameMonth = sameYear && calMin.get(Calendar.MONTH) == calMax.get(Calendar.MONTH)
    val sameDay = sameMonth && calMin.get(Calendar.DAY_OF_MONTH) == calMax.get(Calendar.DAY_OF_MONTH)

    return when {
        sameDay -> SimpleDateFormat("LLLL d, yyyy", locale).format(Date(maxTime))
            .replaceFirstChar { it.titlecase(locale) }
        sameMonth -> {
            val monthName = SimpleDateFormat("LLLL", locale).format(Date(minTime))
                .replaceFirstChar { it.titlecase(locale) }
            val d1 = calMin.get(Calendar.DAY_OF_MONTH)
            val d2 = calMax.get(Calendar.DAY_OF_MONTH)
            val yr = calMax.get(Calendar.YEAR)
            "$monthName $d1 - $d2, $yr"
        }
        else -> {
            val fmt = SimpleDateFormat("LLLL yyyy", locale)
            val startStr = fmt.format(Date(minTime)).replaceFirstChar { it.titlecase(locale) }
            val endStr = fmt.format(Date(maxTime)).replaceFirstChar { it.titlecase(locale) }
            "$startStr - $endStr"
        }
    }
}

private fun groupSpotsByDayLabel(
    spots: List<ParkingSpot>,
    isGreek: Boolean
): List<Pair<String, List<ParkingSpot>>> {
    val locale = if (isGreek) Locale("el", "GR") else Locale.US
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val dayFmt = SimpleDateFormat("EEE d MMM yyyy", locale)

    val map = LinkedHashMap<String, MutableList<ParkingSpot>>()
    for (spot in spots) {
        val cal = Calendar.getInstance().apply { timeInMillis = spot.timestamp }
        val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
        val isYesterday = cal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

        val label = when {
            isToday -> if (isGreek) "Σήμερα" else "Today"
            isYesterday -> if (isGreek) "Χτες" else "Yesterday"
            else -> dayFmt.format(Date(spot.timestamp)).replaceFirstChar { it.titlecase(locale) }
        }
        map.getOrPut(label) { mutableListOf() }.add(spot)
    }
    return map.entries.map { it.key to it.value }
}

private data class OfflineMapPlace(
    val nameEl: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double,
    val isCountry: Boolean,
    val minZoom: Float,
    val maxZoom: Float
)

private data class OfflineWaterLabel(
    val nameEl: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double,
    val minZoom: Float,
    val maxZoom: Float
)

private data class OfflineHighway(
    val isMajor: Boolean,
    val points: List<Pair<Double, Double>>
)

private data class OfflineRouteShield(
    val code: String,
    val lat: Double,
    val lng: Double,
    val isEuropean: Boolean
)

private val OFFLINE_LAND_POLYGONS: List<List<Pair<Double, Double>>> = listOf(
    // Continental Europe + Balkans + Greece mainland outline
    listOf(
        36.40 to 22.45, 36.80 to 21.65, 38.25 to 21.70, 38.95 to 20.75,
        39.90 to 19.95, 40.90 to 19.40, 42.20 to 18.80, 44.50 to 14.90,
        45.55 to 13.60, 44.40 to 8.90, 43.30 to 5.40, 43.30 to -1.80,
        36.10 to -5.50, 38.70 to -9.20, 43.30 to -8.40, 48.40 to -4.50,
        51.00 to 1.80, 53.50 to 8.50, 57.50 to 10.20, 54.50 to 18.60,
        59.90 to 30.20, 46.50 to 30.70, 44.20 to 28.60, 41.02 to 28.97,
        40.85 to 26.00, 40.90 to 24.40, 40.63 to 22.94, 40.00 to 22.60,
        39.35 to 22.95, 38.45 to 23.60, 37.95 to 23.65, 37.40 to 23.15,
        36.40 to 22.45
    ),
    // Italian Peninsula
    listOf(
        45.55 to 13.60, 42.50 to 14.20, 40.15 to 18.45, 39.80 to 18.35,
        40.40 to 16.80, 38.20 to 15.65, 41.20 to 13.60, 44.40 to 8.90
    ),
    // Anatolia / Asia Minor
    listOf(
        41.00 to 29.10, 41.30 to 36.30, 41.00 to 40.50, 36.80 to 36.15,
        36.85 to 30.70, 37.03 to 27.42, 38.42 to 27.14, 40.15 to 26.40
    ),
    // North Africa
    listOf(
        35.80 to -5.80, 36.80 to 3.05, 36.80 to 10.18, 32.90 to 13.18,
        32.10 to 20.07, 31.20 to 29.92, 30.05 to 32.55, 22.00 to 35.00,
        22.00 to -16.00, 35.80 to -5.80
    )
)

private val OFFLINE_HIGHWAYS: List<OfflineHighway> = listOf(
    // E90 Egnatia Odos (Igoumenitsa -> Ioannina -> Metsovo -> Kozani -> Veria -> Thessaloniki -> Kavala)
    OfflineHighway(
        isMajor = true,
        points = listOf(
            39.503 to 20.265, 39.615 to 20.853, 39.770 to 21.182,
            40.300 to 21.788, 40.524 to 22.202, 40.640 to 22.944,
            40.790 to 23.850, 40.939 to 24.414
        )
    ),
    // E951 / Ionia Odos (Ioannina -> Filippiada -> Arta -> Agrinio -> Patras)
    OfflineHighway(
        isMajor = true,
        points = listOf(
            39.750 to 20.780, 39.665 to 20.853, 39.500 to 20.870,
            39.230 to 20.880, 39.160 to 20.985, 38.620 to 21.407, 38.246 to 21.734
        )
    ),
    // E75 (Belgrade -> Skopje -> Thessaloniki -> Katerini -> Larissa -> Athens)
    OfflineHighway(
        isMajor = true,
        points = listOf(
            44.786 to 20.448, 41.998 to 21.425, 40.640 to 22.944,
            40.270 to 22.505, 39.639 to 22.419, 38.890 to 22.430, 37.983 to 23.727
        )
    ),
    // E853 (Ioannina -> Kalpaki -> Konitsa / Kakavia)
    OfflineHighway(
        isMajor = false,
        points = listOf(
            39.665 to 20.853, 39.705 to 20.790, 39.885 to 20.625, 40.045 to 20.750
        )
    )
)

private val OFFLINE_ROUTE_SHIELDS: List<OfflineRouteShield> = listOf(
    OfflineRouteShield("E92", 39.725, 20.995, isEuropean = true),
    OfflineRouteShield("E951", 39.535, 20.865, isEuropean = true),
    OfflineRouteShield("E853", 39.780, 20.700, isEuropean = true),
    OfflineRouteShield("E75", 40.780, 22.640, isEuropean = true),
    OfflineRouteShield("E90", 40.420, 21.960, isEuropean = true),
    OfflineRouteShield("17", 39.585, 20.790, isEuropean = false),
    OfflineRouteShield("30", 39.370, 21.080, isEuropean = false)
)

private val OFFLINE_WATER_LABELS: List<OfflineWaterLabel> = listOf(
    OfflineWaterLabel("Μεσόγειος Θάλασσα", "Mediterranean Sea", 35.20, 19.50, 2.5f, 8.5f),
    OfflineWaterLabel("Αιγαίο Πέλαγος", "Aegean Sea", 38.50, 25.20, 5.0f, 9.5f),
    OfflineWaterLabel("Ιόνιο Πέλαγος", "Ionian Sea", 38.20, 19.20, 5.0f, 9.5f),
    OfflineWaterLabel("Μαύρη Θάλασσα", "Black Sea", 43.30, 34.00, 3.5f, 8.0f),
    OfflineWaterLabel("Λίμνη Παμβώτιδα", "Lake Pamvotida", 39.662, 20.892, 10.0f, 16.5f)
)

private val OFFLINE_MAP_PLACES: List<OfflineMapPlace> = listOf(
    // Countries
    OfflineMapPlace("Ελλάδα", "Greece", 39.30, 21.80, isCountry = true, minZoom = 2.5f, maxZoom = 8.2f),
    OfflineMapPlace("Ιταλία", "Italy", 42.50, 12.60, isCountry = true, minZoom = 2.5f, maxZoom = 7.8f),
    OfflineMapPlace("Αλβανία", "Albania", 41.15, 20.15, isCountry = true, minZoom = 4.5f, maxZoom = 8.5f),
    OfflineMapPlace("Βόρεια Μακεδονία", "North Macedonia", 41.60, 21.70, isCountry = true, minZoom = 5.0f, maxZoom = 8.5f),
    OfflineMapPlace("Βουλγαρία", "Bulgaria", 42.73, 25.48, isCountry = true, minZoom = 3.5f, maxZoom = 8.2f),
    OfflineMapPlace("Ρουμανία", "Romania", 45.94, 24.96, isCountry = true, minZoom = 3.5f, maxZoom = 8.0f),
    OfflineMapPlace("Σερβία", "Serbia", 44.01, 20.90, isCountry = true, minZoom = 3.5f, maxZoom = 8.0f),
    OfflineMapPlace("Ουγγαρία", "Hungary", 47.16, 19.50, isCountry = true, minZoom = 3.5f, maxZoom = 7.8f),
    OfflineMapPlace("Αυστρία", "Austria", 47.51, 14.55, isCountry = true, minZoom = 3.5f, maxZoom = 7.8f),
    OfflineMapPlace("Τουρκία", "Turkey", 39.00, 35.00, isCountry = true, minZoom = 2.5f, maxZoom = 7.8f),
    OfflineMapPlace("Αίγυπτος", "Egypt", 26.82, 30.80, isCountry = true, minZoom = 2.5f, maxZoom = 7.5f),
    // Cities & Regional Towns (matching screenshots)
    OfflineMapPlace("Αθήνα", "Athens", 37.9838, 23.7275, isCountry = false, minZoom = 4.2f, maxZoom = 15.0f),
    OfflineMapPlace("Θεσσαλονίκη", "Thessaloniki", 40.6401, 22.9444, isCountry = false, minZoom = 5.0f, maxZoom = 15.0f),
    OfflineMapPlace("Ιωάννινα", "Ioannina", 39.6650, 20.8537, isCountry = false, minZoom = 6.2f, maxZoom = 16.5f),
    OfflineMapPlace("Μέτσοβο", "Metsovo", 39.7697, 21.1828, isCountry = false, minZoom = 8.0f, maxZoom = 16.5f),
    OfflineMapPlace("Κόνιτσα", "Konitsa", 40.0447, 20.7486, isCountry = false, minZoom = 8.2f, maxZoom = 16.5f),
    OfflineMapPlace("Ανατολή", "Anatoli", 39.6356, 20.8658, isCountry = false, minZoom = 9.0f, maxZoom = 17.5f),
    OfflineMapPlace("Ελεούσα", "Eleousa", 39.7064, 20.7925, isCountry = false, minZoom = 9.0f, maxZoom = 17.5f),
    OfflineMapPlace("Πεδινή", "Pedini", 39.6050, 20.8420, isCountry = false, minZoom = 9.2f, maxZoom = 17.5f),
    OfflineMapPlace("Φιλιππιάδα", "Filippiada", 39.2028, 20.8839, isCountry = false, minZoom = 8.5f, maxZoom = 16.5f),
    OfflineMapPlace("Καλαμαριά", "Kalamaria", 40.5822, 22.9547, isCountry = false, minZoom = 7.5f, maxZoom = 16.5f),
    OfflineMapPlace("Πανόραμα", "Panorama", 40.5878, 23.0315, isCountry = false, minZoom = 9.0f, maxZoom = 17.0f),
    OfflineMapPlace("Πυλαία", "Pylaia", 40.6003, 22.9881, isCountry = false, minZoom = 9.0f, maxZoom = 17.0f),
    OfflineMapPlace("Θέρμη", "Thermi", 40.5478, 23.0194, isCountry = false, minZoom = 8.8f, maxZoom = 17.0f),
    OfflineMapPlace("Εύοσμος", "Evosmos", 40.6689, 22.9086, isCountry = false, minZoom = 8.8f, maxZoom = 17.0f),
    OfflineMapPlace("Κορδελιό", "Kordelio", 40.6652, 22.8938, isCountry = false, minZoom = 9.2f, maxZoom = 17.0f),
    OfflineMapPlace("Σταυρούπολη", "Stavroupoli", 40.6638, 22.9296, isCountry = false, minZoom = 9.2f, maxZoom = 17.0f),
    OfflineMapPlace("Πολίχνη", "Polichni", 40.6612, 22.9488, isCountry = false, minZoom = 9.4f, maxZoom = 17.0f),
    OfflineMapPlace("Αμπελόκηποι", "Ampelokipoi", 40.6534, 22.9218, isCountry = false, minZoom = 9.5f, maxZoom = 17.0f),
    OfflineMapPlace("Μενεμένη", "Menemeni", 40.6518, 22.9094, isCountry = false, minZoom = 9.5f, maxZoom = 17.0f),
    OfflineMapPlace("Νεάπολη", "Neapoli", 40.6532, 22.9408, isCountry = false, minZoom = 9.5f, maxZoom = 17.0f),
    OfflineMapPlace("Συκιές", "Sykies", 40.6494, 22.9558, isCountry = false, minZoom = 9.5f, maxZoom = 17.0f),
    OfflineMapPlace("Σίνδος", "Sindos", 40.6708, 22.8054, isCountry = false, minZoom = 9.0f, maxZoom = 17.0f),
    OfflineMapPlace("Τούμπα", "Toumba", 40.6145, 22.9715, isCountry = false, minZoom = 9.5f, maxZoom = 17.0f),
    OfflineMapPlace("Άνω Πόλη", "Ano Poli", 40.6435, 22.9545, isCountry = false, minZoom = 10.0f, maxZoom = 17.0f),
    OfflineMapPlace("Ωραιόκαστρο", "Oraiokastro", 40.7278, 22.9178, isCountry = false, minZoom = 8.8f, maxZoom = 17.0f),
    OfflineMapPlace("Πεύκα", "Pefka", 40.6584, 22.9938, isCountry = false, minZoom = 9.4f, maxZoom = 17.0f),
    OfflineMapPlace("Ευκαρπία", "Efkarpia", 40.6884, 22.9538, isCountry = false, minZoom = 9.2f, maxZoom = 17.0f),
    OfflineMapPlace("Φίλυρο", "Filiro", 40.6924, 23.0038, isCountry = false, minZoom = 9.4f, maxZoom = 17.0f),
    OfflineMapPlace("Ασβεστοχώρι", "Asvestochori", 40.6468, 23.0238, isCountry = false, minZoom = 9.5f, maxZoom = 17.0f),
    OfflineMapPlace("Λαγκαδάς", "Lagkadas", 40.7504, 23.0668, isCountry = false, minZoom = 8.8f, maxZoom = 17.0f),
    OfflineMapPlace("Περαία", "Peraia", 40.5025, 22.9264, isCountry = false, minZoom = 8.8f, maxZoom = 17.0f),
    OfflineMapPlace("Πειραιάς", "Piraeus", 37.9430, 23.6469, isCountry = false, minZoom = 7.0f, maxZoom = 16.5f),
    OfflineMapPlace("Μαρούσι", "Marousi", 38.0562, 23.8049, isCountry = false, minZoom = 8.8f, maxZoom = 17.0f),
    OfflineMapPlace("Χαλάνδρι", "Chalandri", 38.0218, 23.7988, isCountry = false, minZoom = 9.0f, maxZoom = 17.0f),
    OfflineMapPlace("Κηφισιά", "Kifisia", 38.0734, 23.8128, isCountry = false, minZoom = 8.8f, maxZoom = 17.0f),
    OfflineMapPlace("Γλυφάδα", "Glyfada", 37.8628, 23.7538, isCountry = false, minZoom = 8.5f, maxZoom = 17.0f),
    OfflineMapPlace("Καλλιθέα", "Kallithea", 37.9542, 23.7014, isCountry = false, minZoom = 9.0f, maxZoom = 17.0f),
    OfflineMapPlace("Περιστέρι", "Peristeri", 38.0114, 23.6918, isCountry = false, minZoom = 8.8f, maxZoom = 17.0f),
    OfflineMapPlace("Κατερίνη", "Katerini", 40.2696, 22.5061, isCountry = false, minZoom = 7.2f, maxZoom = 16.0f),
    OfflineMapPlace("Βέροια", "Veria", 40.5244, 22.2024, isCountry = false, minZoom = 7.2f, maxZoom = 16.0f),
    OfflineMapPlace("Κοζάνη", "Kozani", 40.3006, 21.7889, isCountry = false, minZoom = 7.2f, maxZoom = 16.0f),
    OfflineMapPlace("Σέρρες", "Serres", 41.0909, 23.5413, isCountry = false, minZoom = 7.2f, maxZoom = 16.0f),
    OfflineMapPlace("Κιλκίς", "Kilkis", 40.9937, 22.8754, isCountry = false, minZoom = 7.5f, maxZoom = 16.0f),
    OfflineMapPlace("Λάρισα", "Larissa", 39.6390, 22.4191, isCountry = false, minZoom = 6.0f, maxZoom = 16.5f),
    OfflineMapPlace("Γιάννουλη", "Giannouli", 39.6654, 22.3962, isCountry = false, minZoom = 9.5f, maxZoom = 17.0f),
    OfflineMapPlace("Νίκαια Λάρισας", "Nikaia Larissa", 39.5664, 22.4662, isCountry = false, minZoom = 9.5f, maxZoom = 17.0f),
    OfflineMapPlace("Αγιά", "Agia", 39.7186, 22.7581, isCountry = false, minZoom = 8.5f, maxZoom = 17.0f),
    OfflineMapPlace("Μελιβοία", "Melivoia", 39.7495, 22.7955, isCountry = false, minZoom = 8.5f, maxZoom = 18.0f),
    OfflineMapPlace("Κωνσταντινούπολη", "Istanbul", 41.0082, 28.9784, isCountry = false, minZoom = 4.0f, maxZoom = 14.0f),
    OfflineMapPlace("Βουκουρέστι", "Bucharest", 44.4268, 26.1025, isCountry = false, minZoom = 4.5f, maxZoom = 14.0f),
    OfflineMapPlace("Βελιγράδι", "Belgrade", 44.7866, 20.4489, isCountry = false, minZoom = 4.5f, maxZoom = 14.0f),
    OfflineMapPlace("Βουδαπέστη", "Budapest", 47.4979, 19.0402, isCountry = false, minZoom = 4.5f, maxZoom = 14.0f)
)
