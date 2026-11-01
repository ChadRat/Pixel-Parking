package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ParkingSpot
import com.example.data.entity.isBluetoothEntry
import com.example.data.entity.isCustomFarAwayEntry
import com.example.data.entity.isGeofenceEntry
import com.example.data.entity.isUndergroundFallback
import com.example.ui.components.CustomFarAwayIcon
import com.example.ui.components.EditSpotDialog
import com.example.ui.components.GeofenceIcon
import com.example.ui.components.UndergroundParkingIcon
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.PixelRed
import com.example.ui.viewmodel.ParkingViewModel
import com.example.util.HapticHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    viewModel: ParkingViewModel,
    spots: List<ParkingSpot>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    val isSearchExpanded = isSearchVisible || searchQuery.isNotBlank()
    val searchButtonRotation by animateFloatAsState(
        targetValue = if (isSearchExpanded) 90f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
        ),
        label = "search_button_rotation"
    )
    var showClearDialog by remember { mutableStateOf(false) }
    var editingSpot by remember { mutableStateOf<ParkingSpot?>(null) }

    val pinnedSpotIds by viewModel.pinnedSpotIds.collectAsStateWithLifecycle()
    val isDevFunWalkAwayIcon by viewModel.isDevFunWalkAwayIcon.collectAsStateWithLifecycle()

    val filteredSpots = remember(spots, searchQuery, pinnedSpotIds) {
        spots.filter {
            it.spotName.contains(searchQuery, ignoreCase = true) ||
                    it.address.contains(searchQuery, ignoreCase = true) ||
                    it.floorLevel.contains(searchQuery, ignoreCase = true) ||
                    it.note.contains(searchQuery, ignoreCase = true)
        }.sortedWith(
            compareByDescending<ParkingSpot> { pinnedSpotIds.contains(it.id) }
                .thenByDescending { it.timestamp }
        )
    }

    // 30-item windowing to eliminate lagging even with 500+ entries
    val pageSize = 30
    val unloadCount = 20
    var startIndex by remember(searchQuery) { mutableIntStateOf(0) }
    val totalCount = filteredSpots.size
    val endIndex = minOf(startIndex + pageSize, totalCount)
    val windowSpots = remember(filteredSpots, startIndex, endIndex) {
        if (totalCount == 0) emptyList()
        else filteredSpots.subList(startIndex.coerceIn(0, totalCount), endIndex.coerceIn(0, totalCount))
    }

    val isSheetOpen = editingSpot != null
    val carBadgeStyle by viewModel.carBadgeStyle.collectAsState()
    val capyVariant by viewModel.capyVariant.collectAsState()
    val isTimerRunning by viewModel.timerIsRunning.collectAsStateWithLifecycle()
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsStateWithLifecycle()
    val showTimerDialog by viewModel.showTimerDialog.collectAsStateWithLifecycle()

    val smoothFling = com.example.util.rememberSmoothFlingBehavior()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Sliding window scroll monitor: load next 30, unload top 20 when close to end
    LaunchedEffect(listState, totalCount) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visible = layoutInfo.visibleItemsInfo
            val lastVisible = visible.lastOrNull()?.index ?: 0
            val firstVisible = visible.firstOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            Triple(firstVisible, lastVisible, totalItems)
        }.collect { (firstVisible, lastVisible, totalItems) ->
            if (totalCount > pageSize) {
                // User scrolled close enough to end of current batch (within 4 items of bottom)
                if (lastVisible >= totalItems - 4 && endIndex < totalCount) {
                    val nextStart = (startIndex + unloadCount).coerceAtMost(maxOf(0, totalCount - pageSize))
                    if (nextStart > startIndex) {
                        val shift = nextStart - startIndex
                        startIndex = nextStart
                        val curFirst = listState.firstVisibleItemIndex
                        val curOffset = listState.firstVisibleItemScrollOffset
                        listState.scrollToItem(maxOf(0, curFirst - shift), curOffset)
                    }
                } else if (firstVisible <= 2 && startIndex > 0) {
                    // User scrolled back near top of current batch
                    val prevStart = maxOf(0, startIndex - unloadCount)
                    if (prevStart < startIndex) {
                        val shift = startIndex - prevStart
                        startIndex = prevStart
                        val curFirst = listState.firstVisibleItemIndex
                        val curOffset = listState.firstVisibleItemScrollOffset
                        listState.scrollToItem(curFirst + shift, curOffset)
                    }
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .testTag("history_screen"),
        contentPadding = PaddingValues(top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        flingBehavior = smoothFling
    ) {
        // Header & Animated Search Bar
        item(key = "history_header") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.historyTitle,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (totalCount > pageSize) {
                                "$totalCount spots (${startIndex + 1}–$endIndex loaded)"
                            } else {
                                "${spots.size} ${strings.historySubtitle}"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Small Search Button (squircle shape with well-rounded corners, smaller than the delete button, smooth 1/4 rotation)
                        Surface(
                            onClick = {
                                val nextState = !isSearchVisible
                                isSearchVisible = nextState
                                if (!nextState) {
                                    searchQuery = ""
                                }
                                HapticHelper.performSearchQuarterTurnHaptic(context)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    rotationZ = searchButtonRotation
                                }
                                .testTag("toggle_history_search_btn")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Entries",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer {
                                            rotationZ = -searchButtonRotation
                                        },
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (spots.isNotEmpty()) {
                            Surface(
                                onClick = { showClearDialog = true },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_history_btn")
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = "Clear History",
                                        modifier = Modifier.size(20.dp),
                                        tint = PixelRed
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar smoothly expands downwards and retracts upwards in sync with the 1/4 rotation
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandVertically(
                        animationSpec = tween(durationMillis = 800, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)),
                        expandFrom = Alignment.Top
                    ) + fadeIn(animationSpec = tween(durationMillis = 400)),
                    exit = shrinkVertically(
                        animationSpec = tween(durationMillis = 800, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(animationSpec = tween(durationMillis = 350))
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(strings.searchSpots) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("history_search_input")
                        )
                    }
                }
            }
        }

        // Unloaded Earlier Entries Banner
        if (startIndex > 0) {
            item(key = "unloaded_banner") {
                Surface(
                    onClick = {
                        coroutineScope.launch {
                            startIndex = 0
                            listState.animateScrollToItem(0)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_to_top_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$startIndex older entries unloaded • Tap to jump to top",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (filteredSpots.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No spots match '$searchQuery'" else strings.noSpotsSubtitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(windowSpots, key = { it.id }) { spot ->
                val isPinned = pinnedSpotIds.contains(spot.id)
                Box(modifier = Modifier.animateItem()) {
                    SwipeableHistorySpotRow(
                        spot = spot,
                        isPinned = isPinned,
                        onTogglePin = { viewModel.togglePinSpot(spot.id) },
                        onNavigateMaps = { viewModel.openGoogleMapsNavigation(context, spot) },
                        onNavigateCompass = { viewModel.setActiveNavigationTarget(spot) },
                        onShare = { viewModel.shareParkingLocation(context, spot, linkOnly = true) },
                        onEdit = { editingSpot = spot },
                        onDelete = { viewModel.deleteSpot(spot.id) }
                    )
                }
            }

            // Loading Next Entries Indicator
            if (endIndex < totalCount) {
                item(key = "loading_more_banner") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Loading next entries ($endIndex of $totalCount)...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Comprehensive Edit Spot Dialog
    if (editingSpot != null) {
        EditSpotDialog(
            initialSpot = editingSpot,
            carBadgeStyle = carBadgeStyle,
            capyVariant = capyVariant,
            isTimerRunning = isTimerRunning,
            timerRemainingSeconds = timerRemainingSeconds,
            isTimerOpen = showTimerDialog,
            viewModel = viewModel,
            onOpenTimer = { viewModel.openParkingTimer() },
            onDismiss = { editingSpot = null },
            onSave = { name, floor, note, customLat, customLng, customAddress ->
                editingSpot?.let { spot ->
                    viewModel.updateHistorySpot(
                        id = spot.id,
                        name = name,
                        floor = floor,
                        note = note,
                        customLat = customLat,
                        customLng = customLng,
                        customAddress = customAddress
                    )
                }
                editingSpot = null
            },
            onRefreshGps = { viewModel.refreshCurrentLocation() }
        )
    }

    if (showClearDialog) {
        var confirmationInput by remember { mutableStateOf("") }
        var deletePinned by remember { mutableStateOf(false) }
        val isConfirmed = confirmationInput.trim().equals("DELETE", ignoreCase = true)
        val pinnedCount = pinnedSpotIds.size

        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = strings.clearAllConfirmTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = strings.clearAllConfirmMsg,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // Delete Pinned Entries Toggle (Disabled by default)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    tint = if (deletePinned) PixelRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Delete pinned entries as well",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (deletePinned) {
                                            if (pinnedCount > 0) "All $pinnedCount pinned spot(s) will also be deleted" else "Pinned spots will also be deleted"
                                        } else {
                                            if (pinnedCount > 0) "$pinnedCount pinned spot(s) will be preserved" else "Preserves any pinned spots"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = deletePinned,
                                onCheckedChange = { deletePinned = it },
                                modifier = Modifier.testTag("toggle_delete_pinned_switch")
                            )
                        }
                    }

                    // Confirmation Text Box Requirement: type DELETE
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Type DELETE to confirm:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = confirmationInput,
                            onValueChange = { confirmationInput = it },
                            placeholder = { Text("DELETE") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PixelRed,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_all_confirmation_input")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isConfirmed) {
                            viewModel.clearHistory(keepPinned = !deletePinned)
                            showClearDialog = false
                            HapticHelper.performHoldStepTick(context)
                        }
                    },
                    enabled = isConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PixelRed,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("confirm_clear_history_btn")
                ) {
                    Text(
                        text = strings.clearAll,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearDialog = false },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(strings.cancel)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

/**
 * Swipeable History Row:
 * - Beside each box: small Notes / Edit button matching recent spots button styling.
 * - Swipe right: smoothly reveals Delete action on the left side.
 * - Swipe left: smoothly reveals Edit action on the right side.
 */
@Composable
fun SwipeableHistorySpotRow(
    spot: ParkingSpot,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onNavigateMaps: () -> Unit,
    onNavigateCompass: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val maxSwipePx = with(LocalDensity.current) { 72.dp.toPx() }
    val deleteRed = Color(0xFF92312E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("history_spot_row_${spot.id}")
    ) {
        // Background Action on Left: Delete (revealed when swiped right)
        if (offsetX.value > 0f) {
            val leftProgress = (offsetX.value / maxSwipePx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = deleteRed,
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            scaleX = 0.5f + 0.5f * leftProgress
                            scaleY = 0.5f + 0.5f * leftProgress
                            alpha = leftProgress
                        }
                        .clickable {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                            }
                            onDelete()
                        }
                        .testTag("swipe_delete_btn_${spot.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Spot",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Background Action on Right: Edit (revealed when swiped left)
        if (offsetX.value < 0f) {
            val rightProgress = (-offsetX.value / maxSwipePx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            scaleX = 0.5f + 0.5f * rightProgress
                            scaleY = 0.5f + 0.5f * rightProgress
                            alpha = rightProgress
                        }
                        .clickable {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                            }
                            onEdit()
                        }
                        .testTag("swipe_edit_btn_${spot.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Spot Details",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Foreground History Card
        HistorySpotCardContent(
            spot = spot,
            isPinned = isPinned,
            onTogglePin = onTogglePin,
            onNavigateMaps = onNavigateMaps,
            onNavigateCompass = onNavigateCompass,
            onShare = onShare,
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(spot.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val target = (offsetX.value + dragAmount).coerceIn(-maxSwipePx, maxSwipePx)
                            coroutineScope.launch {
                                offsetX.snapTo(target)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value > maxSwipePx * 0.45f) {
                                    offsetX.animateTo(maxSwipePx, spring(dampingRatio = 0.8f, stiffness = 400f))
                                } else if (offsetX.value < -maxSwipePx * 0.45f) {
                                    offsetX.animateTo(-maxSwipePx, spring(dampingRatio = 0.8f, stiffness = 400f))
                                } else {
                                    offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                            }
                        }
                    )
                }
        )
    }
}

@Composable
fun HistorySpotCardContent(
    spot: ParkingSpot,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onNavigateMaps: () -> Unit,
    onNavigateCompass: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val dateStr = remember(spot.timestamp) {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(spot.timestamp))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("history_spot_${spot.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (spot.isActive) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Main Top Row: Icon Capsule + Spot Name & Address + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular car, underground, geofence, bluetooth, or custom far away icon
                val isUnderground = spot.isUndergroundFallback
                val isGeo = spot.isGeofenceEntry
                val isBt = spot.isBluetoothEntry
                val isFarAway = spot.isCustomFarAwayEntry
                Surface(
                    shape = CircleShape,
                    color = if (spot.isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isUnderground) {
                            UndergroundParkingIcon(
                                modifier = Modifier.size(20.dp),
                                tint = if (spot.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        } else if (isFarAway) {
                            CustomFarAwayIcon(
                                modifier = Modifier.size(20.dp),
                                tint = if (spot.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        } else if (isGeo) {
                            GeofenceIcon(
                                modifier = Modifier.size(20.dp),
                                tint = if (spot.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        } else if (isBt) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = "Bluetooth Saved Spot",
                                tint = if (spot.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (spot.isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = spot.spotName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dateStr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = spot.address.ifBlank { String.format(Locale.US, "GPS: %.5f, %.5f", spot.latitude, spot.longitude) },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Slimmer Pixel-style tonal action pills + simple pin & share buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = onNavigateCompass,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("history_compass_btn_${spot.id}")
                    ) {
                        Icon(Icons.Default.NearMe, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.waypointRadar, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onNavigateMaps,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("history_nav_btn_${spot.id}")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.googleMaps, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Small simple pin button
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("history_pin_btn_${spot.id}")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (isPinned) "Unpin entry" else "Pin entry to top",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("history_share_btn_${spot.id}")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
