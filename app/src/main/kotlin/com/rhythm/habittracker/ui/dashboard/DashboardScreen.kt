package com.rhythm.habittracker.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.CadenceMatrixData
import com.rhythm.habittracker.domain.MatrixCell
import com.rhythm.habittracker.domain.MatrixIntensity
import com.rhythm.habittracker.domain.MatrixMonthLabel
import com.rhythm.habittracker.domain.WeekDayDotState
import com.rhythm.habittracker.domain.WeekHorizonDay
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.HabitSortMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import com.rhythm.habittracker.ui.components.RhythmBrandMark
import com.rhythm.habittracker.ui.components.RhythmConfettiOverlay
import com.rhythm.habittracker.ui.components.RhythmMatrixCell
import com.rhythm.habittracker.ui.components.RhythmProfileAvatar
import com.rhythm.habittracker.ui.components.rhythmMatrixCellColor
import com.rhythm.habittracker.ui.theme.ConsistencyEmerald
import com.rhythm.habittracker.ui.theme.JetBrainsMonoFontFamily
import com.rhythm.habittracker.ui.theme.RhythmDeepGraphite
import com.rhythm.habittracker.ui.theme.RhythmError
import com.rhythm.habittracker.ui.theme.RhythmOnPrimary
import com.rhythm.habittracker.ui.theme.RhythmOnSurface
import com.rhythm.habittracker.ui.theme.RhythmOnSurfaceVariant
import com.rhythm.habittracker.ui.theme.RhythmOutlineVariant
import com.rhythm.habittracker.ui.theme.RhythmPrimary
import com.rhythm.habittracker.ui.theme.RhythmSecondary
import com.rhythm.habittracker.ui.theme.RhythmSurface
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainer
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerHigh
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerHighest
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLow
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLowest
import com.rhythm.habittracker.ui.theme.RhythmTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCreateHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onOpenAllHabits: () -> Unit = {},
    onOpenHabitDetail: (Long) -> Unit = onEditHabit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreenContent(
        uiState = uiState,
        onSelectDate = viewModel::selectDate,
        onSelectDomain = viewModel::selectDomain,
        onCycleSortMode = viewModel::cycleSortMode,
        onSelectSortMode = viewModel::setSortMode,
        onToggleHabit = viewModel::toggleHabitCompletion,
        onRescheduleHabit = viewModel::rescheduleHabit,
        onUndoReschedule = viewModel::undoLastReschedule,
        onDismissRescheduleBanner = viewModel::dismissRescheduleBanner,
        onEditHabit = onEditHabit,
        onCreateHabit = onCreateHabit,
        onOpenAllHabits = onOpenAllHabits,
        onOpenHabitDetail = onOpenHabitDetail,
        modifier = modifier,
    )
}

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectDomain: (String) -> Unit,
    onCycleSortMode: () -> Unit,
    onSelectSortMode: (HabitSortMode) -> Unit,
    onToggleHabit: (Long, Int, LocalDate) -> Unit,
    onEditHabit: (Long) -> Unit,
    onCreateHabit: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAllHabits: () -> Unit = {},
    onOpenHabitDetail: (Long) -> Unit = onEditHabit,
    onRescheduleHabit: (Long, LocalDate, LocalDate) -> Unit = { _, _, _ -> },
    onUndoReschedule: () -> Unit = {},
    onDismissRescheduleBanner: () -> Unit = {},
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var reschedulingTarget by remember { mutableStateOf<Pair<Long, LocalDate>?>(null) }
    var confettiTrigger by remember { mutableIntStateOf(0) }
    var prevDate by remember { mutableStateOf(uiState.selectedDate) }
    var prevCompletedCount by remember { mutableIntStateOf(uiState.completedCount) }
    var prevTotalCount by remember { mutableIntStateOf(uiState.totalCount) }
    var prevWasLoading by remember { mutableStateOf(uiState.isLoading) }
    var prevBucketSnapshots by remember { mutableStateOf<Map<TimeOfDayBucket, Pair<Int, Int>>>(emptyMap()) }
    val activeCelebrations = remember { mutableStateListOf<DayPartCelebrationEvent>() }
    var nextCelebrationId by remember { mutableIntStateOf(0) }
    var overlayRootOffset by remember { mutableStateOf(Offset.Zero) }
    var overlayRootSize by remember { mutableStateOf(IntSize.Zero) }
    val bucketBarRects = remember { mutableStateMapOf<TimeOfDayBucket, Rect>() }

    LaunchedEffect(uiState.selectedDate) {
        isFabMenuExpanded = false
    }

    BackHandler(enabled = isFabMenuExpanded) {
        isFabMenuExpanded = false
    }

    LaunchedEffect(uiState.rescheduleBanner) {
        if (uiState.rescheduleBanner != null) {
            delay(4500)
            onDismissRescheduleBanner()
        }
    }

    LaunchedEffect(
        uiState.isLoading,
        uiState.selectedDate,
        uiState.completedCount,
        uiState.totalCount,
        uiState.bucketProgress,
    ) {
        if (!uiState.isLoading) {
            val sameDate = !prevWasLoading && prevDate == uiState.selectedDate
            if (!sameDate) {
                activeCelebrations.clear()
            } else {
                if (
                    uiState.totalCount > 0 &&
                    prevTotalCount == uiState.totalCount &&
                    prevCompletedCount in 0 until uiState.totalCount &&
                    uiState.completedCount == uiState.totalCount
                ) {
                    confettiTrigger++
                }
                uiState.bucketProgress.forEach { bucketItem ->
                    val prev = prevBucketSnapshots[bucketItem.bucket]
                    if (
                        prev != null &&
                        bucketItem.total > 0 &&
                        prev.second == bucketItem.total &&
                        prev.first in 0 until bucketItem.total &&
                        bucketItem.completed == bucketItem.total
                    ) {
                        activeCelebrations.removeAll { it.bucket == bucketItem.bucket }
                        activeCelebrations.add(
                            DayPartCelebrationEvent(
                                id = ++nextCelebrationId,
                                bucket = bucketItem.bucket,
                                originRect = bucketBarRects[bucketItem.bucket],
                            ),
                        )
                    } else if (bucketItem.total == 0 || bucketItem.completed < bucketItem.total) {
                        activeCelebrations.removeAll { it.bucket == bucketItem.bucket }
                    }
                }
            }
            prevDate = uiState.selectedDate
            prevCompletedCount = uiState.completedCount
            prevTotalCount = uiState.totalCount
            prevBucketSnapshots = uiState.bucketProgress.associate { it.bucket to (it.completed to it.total) }
            prevWasLoading = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayRootOffset = coords.positionInRoot()
                overlayRootSize = coords.size
            },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = RhythmSurface,
            topBar = {
                DashboardTopBar(
                    isSelectedToday = uiState.selectedDate == uiState.today,
                    sortMode = uiState.sortMode,
                    showSortMenu = showSortMenu,
                    onToggleSortMenu = { showSortMenu = !showSortMenu },
                    onDismissSortMenu = { showSortMenu = false },
                    onSelectSortMode = {
                        onSelectSortMode(it)
                        showSortMenu = false
                    },
                    onProfileClick = { showProfileDialog = true },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 104.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "progress_header") {
                    ProgressAnchorSection(uiState = uiState)
                }

                if (uiState.cadenceMatrix != null) {
                    item(key = "cadence_matrix") {
                        CadenceMatrixCard(
                            matrix = uiState.cadenceMatrix,
                            selectedDate = uiState.selectedDate,
                            onDateSelected = onSelectDate,
                        )
                    }
                }

                item(key = "weekly_horizon") {
                    WeeklyHorizonStrip(
                        days = uiState.weeklyHorizon,
                        onSelectDate = onSelectDate,
                    )
                }

                item(key = "day_part_progress") {
                    DayPartProgressSection(
                        bucketProgress = uiState.bucketProgress,
                        onBarBoundsChanged = { bucket, rootBounds ->
                            bucketBarRects[bucket] = Rect(
                                left = rootBounds.left - overlayRootOffset.x,
                                top = rootBounds.top - overlayRootOffset.y,
                                right = rootBounds.right - overlayRootOffset.x,
                                bottom = rootBounds.bottom - overlayRootOffset.y,
                            )
                        },
                    )
                }

                item(key = "filter_strip") {
                    FilterContextStrip(
                        domains = uiState.domainFilters,
                        selectedDomain = uiState.selectedDomain,
                        sortMode = uiState.sortMode,
                        onSelectDomain = onSelectDomain,
                        onCycleSortMode = onCycleSortMode,
                    )
                }

                if (uiState.pendingHabits.isNotEmpty()) {
                    item(key = "pending_header") {
                        HabitSectionHeader(
                            title = "TO DO TODAY (${uiState.pendingHabits.size})",
                            trailingLabel = "PENDING",
                            titleColor = RhythmOnSurface,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(220),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                                fadeOutSpec = tween(180),
                            ),
                        )
                    }

                    items(
                        items = uiState.pendingHabits,
                        key = { "habit_${uiState.selectedDate}_${it.habit.id}_${it.instanceIndex}" },
                    ) { item ->
                        val cardDate = uiState.selectedDate
                        HabitCard(
                            item = item,
                            selectedDate = cardDate,
                            onToggle = { onToggleHabit(item.habit.id, item.instanceIndex, cardDate) },
                            onClick = { onOpenHabitDetail(item.habit.id) },
                            onRequestReschedule = { reschedulingTarget = item.habit.id to cardDate },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(250),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                                fadeOutSpec = tween(200),
                            ),
                        )
                    }
                }

                if (uiState.completedHabits.isNotEmpty()) {
                    item(key = "completed_header") {
                        Column(
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(220),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                                fadeOutSpec = tween(180),
                            ),
                        ) {
                            Spacer(modifier = Modifier.height(2.dp))
                            HabitSectionHeader(
                                title = "COMPLETED (${uiState.completedHabits.size})",
                                trailingLabel = "DONE",
                                titleColor = RhythmOnSurfaceVariant,
                            )
                        }
                    }

                    items(
                        items = uiState.completedHabits,
                        key = { "habit_${uiState.selectedDate}_${it.habit.id}_${it.instanceIndex}" },
                    ) { item ->
                        val cardDate = uiState.selectedDate
                        HabitCard(
                            item = item,
                            selectedDate = cardDate,
                            onToggle = { onToggleHabit(item.habit.id, item.instanceIndex, cardDate) },
                            onClick = { onOpenHabitDetail(item.habit.id) },
                            onRequestReschedule = { reschedulingTarget = item.habit.id to cardDate },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(250),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow,
                                ),
                                fadeOutSpec = tween(200),
                            ),
                        )
                    }
                }

                if (!uiState.isLoading && uiState.pendingHabits.isEmpty() && uiState.completedHabits.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyDayCard(
                            onCreateHabit = onCreateHabit,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(250),
                                fadeOutSpec = tween(180),
                            ),
                        )
                    }
                }

                item(key = "daily_momentum") {
                    Column(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(220),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            fadeOutSpec = tween(180),
                        ),
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        DailyMomentumCard(
                            lead = uiState.momentumInsightLead,
                            highlight = uiState.momentumInsightHighlight,
                            tail = uiState.momentumInsightTail,
                        )
                    }
                }
            }
        }

        var lastBanner by remember { mutableStateOf<RescheduleBanner?>(null) }
        uiState.rescheduleBanner?.let { lastBanner = it }
        val bannerDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH) }
        AnimatedVisibility(
            visible = uiState.rescheduleBanner != null,
            enter = slideInVertically { it / 2 } + fadeIn(tween(200)),
            exit = slideOutVertically { it / 2 } + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 88.dp, bottom = 16.dp),
        ) {
            lastBanner?.let { banner ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RhythmDeepGraphite,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Rescheduled to ${banner.toDate.format(bannerDateFormatter)}",
                                style = RhythmTheme.tokens.labelMd,
                                fontWeight = FontWeight.SemiBold,
                                color = RhythmOnPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = banner.habitTitle,
                                style = RhythmTheme.tokens.bodySm,
                                color = RhythmOnPrimary.copy(alpha = 0.72f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ConsistencyEmerald.copy(alpha = 0.18f),
                            modifier = Modifier.clickable { onUndoReschedule() },
                        ) {
                            Text(
                                text = "UNDO",
                                style = RhythmTheme.tokens.labelMd,
                                fontWeight = FontWeight.Bold,
                                color = ConsistencyEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isFabMenuExpanded,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.22f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { isFabMenuExpanded = false },
            )
        }

        ExpandableDashboardFab(
            expanded = isFabMenuExpanded,
            onToggleExpanded = { isFabMenuExpanded = !isFabMenuExpanded },
            onAddHabit = {
                isFabMenuExpanded = false
                onCreateHabit()
            },
            onAllHabits = {
                isFabMenuExpanded = false
                onOpenAllHabits()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp),
        )

        RhythmConfettiOverlay(
            triggerKey = confettiTrigger,
            modifier = Modifier.fillMaxSize(),
        )

        activeCelebrations.forEach { event ->
            key(event.id) {
                DayPartCelebrationOverlay(
                    event = event,
                    overlayRootSize = overlayRootSize,
                    onFinished = { finishedId ->
                        activeCelebrations.removeAll { it.id == finishedId }
                    },
                )
            }
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = RhythmSurfaceContainerLowest,
            shape = RoundedCornerShape(12.dp),
            title = {
                Text(
                    text = "RHYTHM · ARCHITECT",
                    style = RhythmTheme.tokens.labelMd,
                    color = RhythmOnSurface,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Active Habits: ${uiState.totalActiveHabits}",
                        style = RhythmTheme.tokens.bodyMd,
                        color = RhythmOnSurface,
                    )
                    Text(
                        text = "Longest Active Streak: ${uiState.longestStreakOverall} days",
                        style = RhythmTheme.tokens.bodyMd,
                        color = RhythmOnSurface,
                    )
                    Text(
                        text = "98-Day Consistency: ${uiState.cadenceMatrix?.consistencyPercent ?: 0}%",
                        style = RhythmTheme.tokens.bodyMd,
                        color = RhythmOnSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close", color = RhythmPrimary)
                }
            },
        )
    }

    val activeRescheduleItem = reschedulingTarget?.let { (habitId, targetDate) ->
        if (uiState.selectedDate != targetDate) {
            null
        } else {
            (uiState.pendingHabits + uiState.completedHabits).firstOrNull { it.habit.id == habitId }
        }
    }
    LaunchedEffect(reschedulingTarget, activeRescheduleItem) {
        if (reschedulingTarget != null && activeRescheduleItem == null) {
            reschedulingTarget = null
        }
    }

    activeRescheduleItem?.let { item ->
        val dialogDate = reschedulingTarget?.second ?: uiState.selectedDate
        RescheduleHabitDialog(
            item = item,
            currentDate = dialogDate,
            onDismiss = { reschedulingTarget = null },
            onConfirmReschedule = { targetDate ->
                onRescheduleHabit(item.habit.id, dialogDate, targetDate)
                reschedulingTarget = null
            },
        )
    }
}

@Composable
private fun ExpandableDashboardFab(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddHabit: () -> Unit,
    onAllHabits: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fabRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "fabRotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ) { it / 2 } +
                scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                ),
            exit = fadeOut(tween(140)) +
                slideOutVertically(tween(140)) { it / 3 } +
                scaleOut(targetScale = 0.9f, animationSpec = tween(140)),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FabNavigationOptionPill(
                    label = "ADD HABIT",
                    icon = Icons.Rounded.Add,
                    onClick = onAddHabit,
                )
                FabNavigationOptionPill(
                    label = "ALL HABITS",
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    onClick = onAllHabits,
                )
            }
        }

        FloatingActionButton(
            onClick = onToggleExpanded,
            shape = RoundedCornerShape(16.dp),
            containerColor = RhythmPrimary,
            contentColor = RhythmOnPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp,
            ),
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = if (expanded) "Close navigation options" else "Open habit actions",
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { rotationZ = fabRotation },
            )
        }
    }
}

@Composable
private fun FabNavigationOptionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = RhythmDeepGraphite,
        contentColor = Color.White,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = label,
                style = RhythmTheme.tokens.labelMd,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    isSelectedToday: Boolean,
    sortMode: HabitSortMode,
    showSortMenu: Boolean,
    onToggleSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    onSelectSortMode: (HabitSortMode) -> Unit,
    onProfileClick: () -> Unit,
) {
    Surface(
        color = RhythmSurface.copy(alpha = 0.94f),
        shadowElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    RhythmBrandMark(size = 32.dp)
                    Column {
                        Text(
                            text = "RHYTHM",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                        )
                        AnimatedContent(
                            targetState = if (isSelectedToday) "Today" else "Horizon",
                            transitionSpec = {
                                (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith
                                    (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(180)))
                            },
                            label = "topBarTitle",
                        ) { titleText ->
                            Text(
                                text = titleText,
                                style = RhythmTheme.tokens.headlineMd,
                                color = RhythmOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box {
                        IconButton(
                            onClick = onToggleSortMenu,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Quick filter and sort",
                                tint = RhythmOnSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = onDismissSortMenu,
                            containerColor = RhythmSurfaceContainerLowest,
                        ) {
                            HabitSortMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "SORT: ${mode.label}",
                                            style = RhythmTheme.tokens.labelMd,
                                            fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Medium,
                                            color = if (mode == sortMode) RhythmPrimary else RhythmOnSurfaceVariant,
                                        )
                                    },
                                    onClick = { onSelectSortMode(mode) },
                                )
                            }
                        }
                    }

                    RhythmProfileAvatar(onClick = onProfileClick)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = RhythmSurfaceContainer)
        }
    }
}

@Composable
private fun ProgressAnchorSection(uiState: DashboardUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = uiState.dateLabel,
                transitionSpec = {
                    (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(180)))
                },
                label = "dateLabel",
            ) { dateLabel ->
                Text(
                    text = dateLabel,
                    style = RhythmTheme.tokens.labelSm,
                    color = RhythmOnSurfaceVariant,
                )
            }
            Text(
                text = uiState.weekLabel,
                style = RhythmTheme.tokens.labelMd,
                fontWeight = FontWeight.SemiBold,
                color = RhythmPrimary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (uiState.selectedDate == uiState.today) "Today's Progress" else "Day's Progress",
                style = RhythmTheme.tokens.headlineLgMobile,
                color = RhythmOnSurface,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                val allDone = uiState.totalCount > 0 && uiState.completedCount == uiState.totalCount
                val counterColor by animateColorAsState(
                    targetValue = if (allDone) ConsistencyEmerald else RhythmOnSurface,
                    animationSpec = tween(300),
                    label = "completedCounterColor",
                )
                AnimatedContent(
                    targetState = uiState.completedCount,
                    transitionSpec = {
                        if (targetState >= initialState) {
                            (slideInVertically(tween(240)) { it } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(200)) { -it } + fadeOut(tween(180)))
                        } else {
                            (slideInVertically(tween(240)) { -it } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(200)) { it } + fadeOut(tween(180)))
                        }
                    },
                    label = "completedCountOdometer",
                ) { count ->
                    Text(
                        text = count.toString(),
                        style = RhythmTheme.tokens.counterMd,
                        fontWeight = FontWeight.Bold,
                        color = counterColor,
                    )
                }
                Text(
                    text = " / ${uiState.totalCount}",
                    style = RhythmTheme.tokens.counterMd,
                    fontWeight = FontWeight.Normal,
                    color = RhythmOnSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayPartProgressSection(
    bucketProgress: List<BucketProgress>,
    onBarBoundsChanged: (TimeOfDayBucket, Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        bucketProgress.forEach { bucketItem ->
            val chipWeight = if (bucketItem.bucket == TimeOfDayBucket.DURING_THE_DAY) 1.24f else 0.88f
            TimeBucketCard(
                item = bucketItem,
                onBarBoundsChanged = { bounds -> onBarBoundsChanged(bucketItem.bucket, bounds) },
                modifier = Modifier.weight(chipWeight),
            )
        }
    }
}

@Composable
private fun TimeBucketCard(
    item: BucketProgress,
    onBarBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = item.fraction.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "bucketProgress",
    )
    val isBucketComplete = item.total > 0 && item.completed >= item.total
    val barColor by animateColorAsState(
        targetValue = if (isBucketComplete) ConsistencyEmerald else RhythmPrimary,
        animationSpec = tween(300),
        label = "bucketBarColor",
    )
    Surface(
        modifier = modifier,
        color = RhythmSurfaceContainerLowest,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 0.5.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.bucket.label.uppercase(Locale.ENGLISH),
                    style = RhythmTheme.tokens.labelSm.copy(
                        fontSize = 9.5.sp,
                        letterSpacing = 0.15.sp,
                    ),
                    color = RhythmOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(4.dp))
                AnimatedContent(
                    targetState = "${item.completed}/${item.total}",
                    transitionSpec = {
                        (slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(160)))
                    },
                    label = "bucketCount",
                ) { fractionText ->
                    Text(
                        text = fractionText,
                        style = RhythmTheme.tokens.counterSm,
                        color = if (isBucketComplete) ConsistencyEmerald else RhythmOnSurface,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .onGloballyPositioned { coords ->
                        onBarBoundsChanged(coords.boundsInRoot())
                    }
                    .clip(CircleShape)
                    .background(RhythmSurfaceContainer),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(barColor),
                )
            }
        }
    }
}

private data class DayPartCelebrationEvent(
    val id: Int,
    val bucket: TimeOfDayBucket,
    val originRect: Rect?,
)

@Composable
private fun DayPartCelebrationOverlay(
    event: DayPartCelebrationEvent,
    overlayRootSize: IntSize,
    onFinished: (Int) -> Unit,
) {
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(event.id) {
        delay(2250)
        currentOnFinished(event.id)
    }

    val emitters = remember(event.id, overlayRootSize) {
        val w = overlayRootSize.width.toFloat()
        val h = overlayRootSize.height.toFloat()
        val rect = event.originRect
        if (rect != null && w > 0f && h > 0f) {
            val yFrac = (rect.center.y / h).coerceIn(0.06f, 0.92f)
            val leftFrac = ((rect.left + rect.width * 0.18f) / w).coerceIn(0.04f, 0.96f)
            val midFrac = (rect.center.x / w).coerceIn(0.04f, 0.96f)
            val rightFrac = ((rect.right - rect.width * 0.18f) / w).coerceIn(0.04f, 0.96f)
            listOf(leftFrac to yFrac, midFrac to yFrac, rightFrac to yFrac)
        } else {
            val fallbackX = when (event.bucket) {
                TimeOfDayBucket.MORNING -> 0.18f
                TimeOfDayBucket.DURING_THE_DAY -> 0.50f
                TimeOfDayBucket.EVENING -> 0.82f
            }
            listOf(
                (fallbackX - 0.05f) to 0.52f,
                fallbackX to 0.52f,
                (fallbackX + 0.05f) to 0.52f,
            )
        }
    }

    RhythmConfettiOverlay(
        triggerKey = event.id,
        modifier = Modifier.fillMaxSize(),
        emitterOriginFractions = emitters,
        particleCount = 68,
        spreadJitterX = 0.03f,
        spreadJitterY = 0.012f,
        extraUpwardImpulse = 0.18f,
    )
}

@Composable
private fun CadenceMatrixCard(
    matrix: CadenceMatrixData,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    val animatedConsistency by animateIntAsState(
        targetValue = matrix.consistencyPercent,
        animationSpec = tween(durationMillis = 360),
        label = "consistencyPercent",
    )

    Surface(
        color = RhythmSurfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarViewMonth,
                        contentDescription = null,
                        tint = RhythmPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "CADENCE MATRIX",
                        style = RhythmTheme.tokens.labelMd,
                        fontWeight = FontWeight.SemiBold,
                        color = RhythmOnSurface,
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ConsistencyEmerald.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ConsistencyEmerald),
                    )
                    Text(
                        text = "$animatedConsistency% CONSISTENCY",
                        style = RhythmTheme.tokens.labelSm,
                        fontWeight = FontWeight.SemiBold,
                        color = ConsistencyEmerald,
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val dayAxisWidth = 16.dp
                val cellSize = 14.dp
                val cellGap = 4.dp
                val gridAvailable = (maxWidth - dayAxisWidth - 4.dp).coerceAtLeast(cellSize)
                val visibleColumns = ((gridAvailable + cellGap) / (cellSize + cellGap))
                    .toInt()
                    .coerceAtLeast(1)
                val minColumnsToFill = (visibleColumns + 4).coerceAtLeast(24)
                val extraPastWeeks = (minColumnsToFill - matrix.columns.size).coerceAtLeast(0)

                val displayColumns = remember(matrix.columns, extraPastWeeks) {
                    if (extraPastWeeks <= 0) {
                        matrix.columns
                    } else {
                        val earliestMonday = matrix.columns.firstOrNull()?.firstOrNull()?.date
                        if (earliestMonday == null) {
                            matrix.columns
                        } else {
                            val prependedColumns = (extraPastWeeks downTo 1).map { weeksBack ->
                                val weekMonday = earliestMonday.minusWeeks(weeksBack.toLong())
                                (0 until 7).map { dayOffset ->
                                    MatrixCell(
                                        date = weekMonday.plusDays(dayOffset.toLong()),
                                        intensity = MatrixIntensity.NONE,
                                        isFuture = false,
                                        scheduledCount = 0,
                                        completedCount = 0,
                                    )
                                }
                            }
                            prependedColumns + matrix.columns
                        }
                    }
                }

                val displayMonthLabels = remember(displayColumns, extraPastWeeks, matrix.monthLabels) {
                    if (extraPastWeeks == 0 && matrix.monthLabels.isNotEmpty()) {
                        buildList {
                            var lastPlacedIndex = -3
                            matrix.monthLabels.forEach { month ->
                                if (month.weekColumnIndex - lastPlacedIndex >= 2) {
                                    add(month)
                                    lastPlacedIndex = month.weekColumnIndex
                                }
                            }
                        }
                    } else {
                        buildList {
                            var lastMonth = -1
                            var lastPlacedIndex = -3
                            displayColumns.forEachIndexed { index, week ->
                                val weekMonday = week.firstOrNull()?.date ?: return@forEachIndexed
                                val monthVal = weekMonday.monthValue
                                if (monthVal != lastMonth) {
                                    lastMonth = monthVal
                                    if (index - lastPlacedIndex >= 2) {
                                        add(
                                            MatrixMonthLabel(
                                                weekColumnIndex = index,
                                                label = weekMonday.month.getDisplayName(
                                                    TextStyle.SHORT,
                                                    Locale.ENGLISH,
                                                ),
                                            )
                                        )
                                        lastPlacedIndex = index
                                    }
                                }
                            }
                        }
                    }
                }

                val totalColumns = displayColumns.size.coerceAtLeast(1)
                val totalGridWidth = (cellSize * totalColumns) + (cellGap * (totalColumns - 1))
                val matrixScrollState = rememberScrollState()
                val latestGridDate = displayColumns.lastOrNull()?.lastOrNull()?.date
                var hasAnchoredToCurrentWeek by remember(latestGridDate) { mutableStateOf(false) }
                LaunchedEffect(matrixScrollState.maxValue, latestGridDate) {
                    if (!hasAnchoredToCurrentWeek && matrixScrollState.maxValue > 0) {
                        matrixScrollState.scrollTo(matrixScrollState.maxValue)
                        hasAnchoredToCurrentWeek = true
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Fixed day-of-week vertical axis (M..S)
                    Column(
                        modifier = Modifier.width(dayAxisWidth),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier.width(dayAxisWidth),
                            verticalArrangement = Arrangement.spacedBy(cellGap),
                        ) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { letter ->
                                Box(
                                    modifier = Modifier
                                        .width(dayAxisWidth)
                                        .height(cellSize),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Text(
                                        text = letter,
                                        style = RhythmTheme.tokens.labelSm,
                                        color = RhythmOnSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // Scrollable week columns anchored on the current week at the right edge (past-only scroll)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(matrixScrollState),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(totalGridWidth)
                                .height(14.dp),
                        ) {
                            displayMonthLabels.forEach { month ->
                                val xOffset = (cellSize + cellGap) * month.weekColumnIndex
                                Text(
                                    text = month.label,
                                    style = RhythmTheme.tokens.labelSm,
                                    color = RhythmOnSurfaceVariant,
                                    modifier = Modifier.offset(x = xOffset),
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(cellGap),
                        ) {
                            displayColumns.forEach { weekColumn ->
                                Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                                    weekColumn.forEach { cell ->
                                        val isSelectedCell = cell.date == selectedDate
                                        RhythmMatrixCell(
                                            completionRatio = cell.completionRatio,
                                            isUnscheduledEmpty = cell.isUnscheduledEmpty,
                                            activeColor = ConsistencyEmerald,
                                            modifier = Modifier.size(cellSize),
                                            emptyColor = RhythmSurfaceContainer,
                                            isFuture = cell.isFuture,
                                            cornerRadius = 2.dp,
                                            borderWidth = if (isSelectedCell) 1.dp else 0.dp,
                                            borderColor = if (isSelectedCell) {
                                                RhythmPrimary.copy(alpha = 0.65f)
                                            } else {
                                                Color.Transparent
                                            },
                                            onClick = { onDateSelected(cell.date) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = RhythmSurfaceContainer)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${matrix.overallStreakDays}-day streak · ${matrix.completionsThisMonth} this month",
                    style = RhythmTheme.tokens.labelSm,
                    color = RhythmOnSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Less",
                        style = RhythmTheme.tokens.labelSm,
                        color = RhythmOnSurfaceVariant,
                    )
                    listOf(0f, 0.35f, 0.70f, 1f).forEach { ratio ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    rhythmMatrixCellColor(
                                        completionRatio = ratio,
                                        activeColor = ConsistencyEmerald,
                                        emptyColor = RhythmSurfaceContainer,
                                    )
                                ),
                        )
                    }
                    Text(
                        text = "More",
                        style = RhythmTheme.tokens.labelSm,
                        color = RhythmOnSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyHorizonStrip(
    days: List<WeekHorizonDay>,
    onSelectDate: (LocalDate) -> Unit,
) {
    Surface(
        color = RhythmSurfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            days.forEach { day ->
                val bgColor by animateColorAsState(
                    targetValue = if (day.isSelected) RhythmPrimary else Color.Transparent,
                    animationSpec = tween(durationMillis = 220),
                    label = "dayBg",
                )
                val letterColor by animateColorAsState(
                    targetValue = if (day.isSelected) RhythmOnPrimary.copy(alpha = 0.8f) else RhythmOnSurfaceVariant,
                    animationSpec = tween(durationMillis = 220),
                    label = "dayLetterColor",
                )
                val numberColor by animateColorAsState(
                    targetValue = if (day.isSelected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                    animationSpec = tween(durationMillis = 220),
                    label = "dayNumberColor",
                )
                val dotColor by animateColorAsState(
                    targetValue = when {
                        day.isSelected -> RhythmOnPrimary
                        day.dotState == WeekDayDotState.ACTIVE_COMPLETED -> ConsistencyEmerald
                        else -> RhythmSurfaceContainerHighest
                    },
                    animationSpec = tween(durationMillis = 260),
                    label = "dayDotColor",
                )
                val pillScale by animateFloatAsState(
                    targetValue = if (day.isSelected) 1.03f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "dayPillScale",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            scaleX = pillScale
                            scaleY = pillScale
                        }
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable { onSelectDate(day.date) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = day.dayLetter,
                        style = RhythmTheme.tokens.labelSm,
                        color = letterColor,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = if (day.isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = numberColor,
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterContextStrip(
    domains: List<String>,
    selectedDomain: String,
    sortMode: HabitSortMode,
    onSelectDomain: (String) -> Unit,
    onCycleSortMode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(domains, key = { it }) { domain ->
                val isSelected = domain.equals(selectedDomain, ignoreCase = true)
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) RhythmPrimary else RhythmSurfaceContainer,
                    animationSpec = tween(durationMillis = 200),
                    label = "domainChipBg",
                )
                val chipTextColor by animateColorAsState(
                    targetValue = if (isSelected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "domainChipText",
                )
                Box(
                    modifier = Modifier
                        .animateItem()
                        .clip(RoundedCornerShape(4.dp))
                        .background(chipBg)
                        .clickable { onSelectDomain(domain) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = domain.uppercase(Locale.ENGLISH),
                        style = RhythmTheme.tokens.labelMd,
                        color = chipTextColor,
                    )
                }
            }
        }

        AnimatedContent(
            targetState = sortMode.label,
            transitionSpec = {
                (slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(160)))
            },
            label = "sortModeLabel",
        ) { modeLabel ->
            Text(
                text = "SORTED: $modeLabel",
                style = RhythmTheme.tokens.labelSm,
                color = RhythmOnSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onCycleSortMode)
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun HabitSectionHeader(
    title: String,
    trailingLabel: String,
    titleColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = title,
            transitionSpec = {
                fadeIn(tween(180)) togetherWith fadeOut(tween(140))
            },
            label = "sectionHeaderTitle",
        ) { animatedTitle ->
            Text(
                text = animatedTitle,
                style = RhythmTheme.tokens.labelSm,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
            )
        }
        Text(
            text = trailingLabel,
            style = RhythmTheme.tokens.labelSm,
            color = RhythmOnSurfaceVariant,
        )
    }
}

private data class WaveColorPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val highlight: Color,
)

private fun HabitAccentColor.toWavePalette(): WaveColorPalette = when (this) {
    HabitAccentColor.ROYAL_BLUE -> WaveColorPalette(
        primary = Color(0xFF2563EB),
        secondary = Color(0xFF06B6D4),
        tertiary = Color(0xFF8B5CF6),
        highlight = Color(0xFF60A5FA),
    )
    HabitAccentColor.EMERALD_GREEN -> WaveColorPalette(
        primary = Color(0xFF10B981),
        secondary = Color(0xFF06B6D4),
        tertiary = Color(0xFFA3E635),
        highlight = Color(0xFF34D399),
    )
    HabitAccentColor.WARM_AMBER -> WaveColorPalette(
        primary = Color(0xFFD97706),
        secondary = Color(0xFFF43F5E),
        tertiary = Color(0xFFFACC15),
        highlight = Color(0xFFFB923C),
    )
    HabitAccentColor.SKY_BLUE -> WaveColorPalette(
        primary = Color(0xFF0284C7),
        secondary = Color(0xFF2DD4BF),
        tertiary = Color(0xFF6366F1),
        highlight = Color(0xFF38BDF8),
    )
    HabitAccentColor.ROSE_CORAL -> WaveColorPalette(
        primary = Color(0xFFF43F5E),
        secondary = Color(0xFFF59E0B),
        tertiary = Color(0xFFD946EF),
        highlight = Color(0xFFFB7185),
    )
    HabitAccentColor.VIOLET_PURPLE -> WaveColorPalette(
        primary = Color(0xFF8B5CF6),
        secondary = Color(0xFFEC4899),
        tertiary = Color(0xFF06B6D4),
        highlight = Color(0xFFA78BFA),
    )
    HabitAccentColor.SLATE_GRAPHITE -> WaveColorPalette(
        primary = Color(0xFF475569),
        secondary = Color(0xFF3B82F6),
        tertiary = Color(0xFF10B981),
        highlight = Color(0xFF8B5CF6),
    )
}

@Composable
private fun HabitCard(
    item: HabitCardItem,
    selectedDate: LocalDate,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onRequestReschedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val accent = item.habit.accentColor.color
    val palette = remember(item.habit.accentColor) { item.habit.accentColor.toWavePalette() }

    val currentOnToggle by rememberUpdatedState(onToggle)
    val currentOnRequestReschedule by rememberUpdatedState(onRequestReschedule)

    var isWaveAnimating by remember(item.habit.id, item.instanceIndex, selectedDate) {
        mutableStateOf(false)
    }
    var waveJob by remember(item.habit.id, item.instanceIndex, selectedDate) {
        mutableStateOf<Job?>(null)
    }
    var cancelResetJob by remember(item.habit.id, item.instanceIndex, selectedDate) {
        mutableStateOf<Job?>(null)
    }
    val waveProgress = remember(item.habit.id, item.instanceIndex, selectedDate) { Animatable(0f) }
    val cardBounceScale = remember(item.habit.id, item.instanceIndex, selectedDate) { Animatable(1f) }
    val checkboxPopScale = remember(item.habit.id, item.instanceIndex, selectedDate) { Animatable(1f) }
    val swipeOffsetPx = remember(item.habit.id, item.instanceIndex, selectedDate) { Animatable(0f) }

    val thresholdPx = with(density) { 96.dp.toPx() }
    val maxRawDragPx = with(density) { 148.dp.toPx() }

    LaunchedEffect(item.isCompleted, selectedDate) {
        if (item.isCompleted && (isWaveAnimating || waveJob?.isActive == true)) {
            waveJob?.cancel()
            waveJob = null
            cancelResetJob?.cancel()
            cancelResetJob = null
            isWaveAnimating = false
            waveProgress.snapTo(0f)
            cardBounceScale.snapTo(1f)
            checkboxPopScale.snapTo(1f)
        }
    }

    val effectiveCompleted = item.isCompleted || isWaveAnimating

    val checkboxBg by animateColorAsState(
        targetValue = if (effectiveCompleted) accent else RhythmSurfaceContainerHighest,
        animationSpec = tween(durationMillis = 220),
        label = "checkboxBg",
    )
    val checkboxScale by animateFloatAsState(
        targetValue = if (effectiveCompleted) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "checkboxScale",
    )
    val titleColor by animateColorAsState(
        targetValue = if (effectiveCompleted) RhythmOnSurface.copy(alpha = 0.72f) else RhythmOnSurface,
        animationSpec = tween(durationMillis = 220),
        label = "habitTitleColor",
    )
    val barColor by animateColorAsState(
        targetValue = if (effectiveCompleted) accent.copy(alpha = 0.65f) else accent,
        animationSpec = tween(durationMillis = 220),
        label = "habitBarColor",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardBounceScale.value
                scaleY = cardBounceScale.value
            }
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(item.habit.id, item.instanceIndex, selectedDate) {
                var rawDragOffset = 0f
                var crossedThreshold = false
                var dragAnimJob: Job? = null
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragAnimJob?.cancel()
                        cancelResetJob?.cancel()
                        cancelResetJob = null
                        if (isWaveAnimating || waveJob?.isActive == true) {
                            waveJob?.cancel()
                            waveJob = null
                            isWaveAnimating = false
                            scope.launch {
                                waveProgress.snapTo(0f)
                                cardBounceScale.snapTo(1f)
                                checkboxPopScale.snapTo(1f)
                            }
                        }
                        rawDragOffset = swipeOffsetPx.value
                        crossedThreshold = abs(rawDragOffset) >= thresholdPx
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        rawDragOffset = (rawDragOffset + dragAmount).coerceIn(-maxRawDragPx, maxRawDragPx)
                        val visualOffset = if (abs(rawDragOffset) <= thresholdPx) {
                            rawDragOffset
                        } else {
                            val extra = abs(rawDragOffset) - thresholdPx
                            val sign = if (rawDragOffset >= 0f) 1f else -1f
                            sign * (thresholdPx + extra * 0.48f)
                        }
                        val isNowPastThreshold = abs(rawDragOffset) >= thresholdPx
                        if (isNowPastThreshold && !crossedThreshold) {
                            crossedThreshold = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (!isNowPastThreshold && crossedThreshold) {
                            crossedThreshold = false
                        }
                        dragAnimJob?.cancel()
                        dragAnimJob = scope.launch {
                            swipeOffsetPx.snapTo(visualOffset)
                        }
                    },
                    onDragEnd = {
                        val shouldTriggerReschedule = abs(rawDragOffset) >= thresholdPx
                        rawDragOffset = 0f
                        crossedThreshold = false
                        if (shouldTriggerReschedule) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnRequestReschedule()
                        }
                        dragAnimJob?.cancel()
                        dragAnimJob = scope.launch {
                            swipeOffsetPx.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        rawDragOffset = 0f
                        crossedThreshold = false
                        dragAnimJob?.cancel()
                        dragAnimJob = scope.launch {
                            swipeOffsetPx.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    },
                )
            },
    ) {
        if (abs(swipeOffsetPx.value) > 0.5f) {
            val isSwipingRight = swipeOffsetPx.value >= 0f
            val dragFraction = (abs(swipeOffsetPx.value) / thresholdPx).coerceIn(0f, 1f)
            val isPastThreshold = abs(swipeOffsetPx.value) >= thresholdPx
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(accent.copy(alpha = 0.06f + 0.14f * dragFraction))
                    .padding(horizontal = 18.dp),
                contentAlignment = if (isSwipingRight) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.graphicsLayer {
                        alpha = 0.45f + 0.55f * dragFraction
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = if (isPastThreshold) "RELEASE TO RESCHEDULE" else "SWIPE TO RESCHEDULE",
                        style = RhythmTheme.tokens.labelMd,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
            }
        }

        Surface(
            color = RhythmSurfaceContainerLowest,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = if (isWaveAnimating) 6.dp else 0.5.dp,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(swipeOffsetPx.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = !isWaveAnimating, onClick = onClick),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .drawWithContent {
                        drawContent()
                        val progress = waveProgress.value
                        if (progress > 0f) {
                            val origin = Offset(
                                x = size.width - 32.dp.toPx(),
                                y = size.height / 2f,
                            )
                            val maxRadius = hypot(size.width, size.height) * 1.18f
                            val fadeIn = (progress / 0.08f).coerceIn(0f, 1f)
                            val fadeOut = if (progress > 0.80f) {
                                ((1f - progress) / 0.20f).coerceIn(0f, 1f)
                            } else {
                                1f
                            }
                            val envelope = fadeIn * fadeOut

                            // Full-card prismatic aurora wash
                            val washStrength = sin((progress * PI).toFloat()).coerceIn(0f, 1f) * 0.85f + 0.15f * fadeIn
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        palette.primary.copy(alpha = 0.26f * washStrength),
                                        palette.secondary.copy(alpha = 0.22f * washStrength),
                                        palette.tertiary.copy(alpha = 0.20f * washStrength),
                                    ),
                                    start = Offset(size.width, 0f),
                                    end = Offset(0f, size.height),
                                ),
                            )

                            // Primary wave front with elastic oscillation
                            val p1 = (progress / 0.84f).coerceIn(0f, 1f)
                            if (p1 > 0f) {
                                val bounceMod1 = 1f + sin(progress * PI.toFloat() * 3.5f) * 0.045f * (1f - progress)
                                val r1 = (maxRadius * p1 * bounceMod1).coerceAtLeast(4f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colorStops = arrayOf(
                                            0f to palette.tertiary.copy(alpha = 0.16f * envelope),
                                            0.45f to palette.secondary.copy(alpha = 0.34f * envelope),
                                            0.76f to palette.primary.copy(alpha = 0.64f * envelope),
                                            0.92f to palette.highlight.copy(alpha = 0.78f * envelope),
                                            1f to Color.Transparent,
                                        ),
                                        center = origin,
                                        radius = r1,
                                    ),
                                    radius = r1,
                                    center = origin,
                                )
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            palette.highlight.copy(alpha = 0.86f * envelope),
                                            palette.secondary.copy(alpha = 0.86f * envelope),
                                            palette.tertiary.copy(alpha = 0.86f * envelope),
                                            palette.primary.copy(alpha = 0.86f * envelope),
                                            palette.highlight.copy(alpha = 0.86f * envelope),
                                        ),
                                        center = origin,
                                    ),
                                    radius = r1,
                                    center = origin,
                                    style = Stroke(width = 3.5.dp.toPx()),
                                )
                            }

                            // Secondary harmonic wave staggered behind primary
                            val p2 = ((progress - 0.15f) / 0.75f).coerceIn(0f, 1f)
                            if (p2 > 0f) {
                                val bounceMod2 = 1f + sin(p2 * PI.toFloat() * 3f) * 0.055f * (1f - p2)
                                val r2 = (maxRadius * p2 * bounceMod2).coerceAtLeast(4f)
                                val tailAlpha2 = envelope * (1f - ((p2 - 0.82f) / 0.18f).coerceIn(0f, 1f))
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Transparent,
                                            0.55f to palette.secondary.copy(alpha = 0.25f * tailAlpha2),
                                            0.84f to palette.tertiary.copy(alpha = 0.68f * tailAlpha2),
                                            1f to Color.Transparent,
                                        ),
                                        center = origin,
                                        radius = r2,
                                    ),
                                    radius = r2,
                                    center = origin,
                                )
                                drawCircle(
                                    color = palette.secondary.copy(alpha = 0.75f * tailAlpha2),
                                    radius = r2,
                                    center = origin,
                                    style = Stroke(width = 3.dp.toPx()),
                                )
                            }

                            // Tertiary echo ripple
                            val p3 = ((progress - 0.30f) / 0.70f).coerceIn(0f, 1f)
                            if (p3 > 0f) {
                                val r3 = (maxRadius * p3).coerceAtLeast(4f)
                                val tailAlpha3 = envelope * (1f - p3 * 0.22f)
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Transparent,
                                            0.68f to palette.primary.copy(alpha = 0.22f * tailAlpha3),
                                            0.90f to palette.highlight.copy(alpha = 0.56f * tailAlpha3),
                                            1f to Color.Transparent,
                                        ),
                                        center = origin,
                                        radius = r3,
                                    ),
                                    radius = r3,
                                    center = origin,
                                )
                                drawCircle(
                                    color = palette.tertiary.copy(alpha = 0.72f * tailAlpha3),
                                    radius = r3,
                                    center = origin,
                                    style = Stroke(width = 2.5.dp.toPx()),
                                )
                            }

                            // Celebratory fizzy color orbs sweeping across the card
                            val orbColors = listOf(
                                palette.primary,
                                palette.secondary,
                                palette.tertiary,
                                palette.highlight,
                            )
                            for (i in 0 until 10) {
                                val orbDelay = i * 0.042f
                                val orbProgress = ((progress - orbDelay) / 0.66f).coerceIn(0f, 1f)
                                if (orbProgress > 0f && orbProgress < 1f) {
                                    val angleDeg = 158f + i * 4.6f + if (i % 2 == 0) 3f else -3f
                                    val angleRad = (angleDeg * PI / 180.0).toFloat()
                                    val speedFactor = 0.45f + (i % 3) * 0.22f
                                    val dist = maxRadius * speedFactor * orbProgress
                                    val wobble = sin(orbProgress * PI.toFloat() * 4f) * 5.dp.toPx() * (1f - orbProgress)
                                    val cx = origin.x + cos(angleRad) * dist
                                    val cy = origin.y + sin(angleRad) * (size.height * 0.92f) * orbProgress + wobble
                                    val popScale = sin(orbProgress * PI.toFloat()).coerceIn(0f, 1f)
                                    val orbRadius = (4.5f + (i % 3) * 2.2f).dp.toPx() * popScale
                                    if (orbRadius > 0.5f) {
                                        drawCircle(
                                            color = orbColors[i % orbColors.size].copy(alpha = 0.85f * popScale * envelope),
                                            radius = orbRadius,
                                            center = Offset(cx, cy),
                                        )
                                    }
                                }
                            }

                            // Glowing prismatic border highlight
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        palette.primary.copy(alpha = 0.85f * envelope),
                                        palette.secondary.copy(alpha = 0.85f * envelope),
                                        palette.tertiary.copy(alpha = 0.85f * envelope),
                                    ),
                                ),
                                cornerRadius = CornerRadius(12.dp.toPx()),
                                style = Stroke(width = 2.5.dp.toPx()),
                            )
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(barColor),
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (item.habit.badgeLabel.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accent.copy(alpha = 0.12f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = item.habit.badgeLabel.uppercase(Locale.ENGLISH),
                                        style = RhythmTheme.tokens.labelSm,
                                        fontWeight = FontWeight.Medium,
                                        color = accent,
                                    )
                                }
                            }

                            val resolvedTimeLabel = (item.timeSlot ?: item.habit.targetTimes.firstOrNull())?.formatted()
                            val scheduleChipLabel = when {
                                item.totalInstances > 1 && resolvedTimeLabel != null ->
                                    "${item.instanceIndex + 1}/${item.totalInstances} · $resolvedTimeLabel"
                                item.totalInstances > 1 ->
                                    "${item.instanceIndex + 1}/${item.totalInstances}"
                                else -> resolvedTimeLabel
                            }
                            if (!scheduleChipLabel.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(RhythmSurfaceContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = scheduleChipLabel,
                                        style = RhythmTheme.tokens.labelSm,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RhythmOnSurfaceVariant,
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(13.dp),
                                )
                                AnimatedContent(
                                    targetState = item.streakDays,
                                    transitionSpec = {
                                        (slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200))) togetherWith
                                            (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(160)))
                                    },
                                    label = "streakDays",
                                ) { streak ->
                                    Text(
                                        text = streak.toString(),
                                        style = RhythmTheme.tokens.counterSm,
                                        color = RhythmOnSurface,
                                    )
                                }
                                Text(
                                    text = "DAYS",
                                    style = RhythmTheme.tokens.labelSm,
                                    color = RhythmOnSurfaceVariant,
                                )
                            }
                        }

                        Text(
                            text = item.habit.title,
                            style = RhythmTheme.tokens.bodyLg,
                            fontWeight = FontWeight.SemiBold,
                            color = titleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (item.habit.description.isNotBlank()) {
                            Text(
                                text = item.habit.description,
                                style = RhythmTheme.tokens.bodySm,
                                color = RhythmOnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                role = Role.Checkbox,
                                enabled = true,
                                onClick = {
                                    if (isWaveAnimating) {
                                        waveJob?.cancel()
                                        waveJob = null
                                        isWaveAnimating = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        cancelResetJob?.cancel()
                                        cancelResetJob = scope.launch {
                                            launch {
                                                waveProgress.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = tween(durationMillis = 180),
                                                )
                                            }
                                            launch {
                                                cardBounceScale.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium,
                                                    ),
                                                )
                                            }
                                            launch {
                                                checkboxPopScale.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMedium,
                                                    ),
                                                )
                                            }
                                        }
                                        return@clickable
                                    }
                                    cancelResetJob?.cancel()
                                    cancelResetJob = null
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!item.isCompleted) {
                                        isWaveAnimating = true
                                        waveJob = scope.launch {
                                            launch {
                                                cardBounceScale.snapTo(1f)
                                                cardBounceScale.animateTo(
                                                    targetValue = 0.94f,
                                                    animationSpec = tween(durationMillis = 85),
                                                )
                                                cardBounceScale.animateTo(
                                                    targetValue = 1.045f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessMediumLow,
                                                    ),
                                                )
                                                cardBounceScale.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioHighBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                                )
                                            }
                                            launch {
                                                checkboxPopScale.snapTo(1f)
                                                checkboxPopScale.animateTo(
                                                    targetValue = 0.78f,
                                                    animationSpec = tween(durationMillis = 70),
                                                )
                                                checkboxPopScale.animateTo(
                                                    targetValue = 1.38f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioHighBouncy,
                                                        stiffness = Spring.StiffnessMedium,
                                                    ),
                                                )
                                                checkboxPopScale.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioHighBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                                )
                                            }
                                            waveProgress.snapTo(0f)
                                            waveProgress.animateTo(
                                                targetValue = 1f,
                                                animationSpec = tween(
                                                    durationMillis = 1050,
                                                    easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f),
                                                ),
                                            )
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            delay(120)
                                            currentOnToggle()
                                            delay(150)
                                            isWaveAnimating = false
                                            waveJob = null
                                            waveProgress.snapTo(0f)
                                            cardBounceScale.snapTo(1f)
                                            checkboxPopScale.snapTo(1f)
                                        }
                                    } else {
                                        currentOnToggle()
                                    }
                                },
                            )
                            .semantics {
                                contentDescription = if (item.totalInstances > 1) {
                                    "Toggle ${item.habit.title} (${item.instanceIndex + 1} of ${item.totalInstances})"
                                } else {
                                    "Toggle ${item.habit.title}"
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = checkboxScale * checkboxPopScale.value
                                    scaleY = checkboxScale * checkboxPopScale.value
                                }
                                .size(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(checkboxBg)
                                .then(
                                    if (!effectiveCompleted) {
                                        Modifier.border(
                                            width = 2.dp,
                                            color = accent.copy(alpha = 0.42f),
                                            shape = RoundedCornerShape(4.dp),
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = effectiveCompleted,
                                enter = scaleIn(
                                    initialScale = 0.25f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioHighBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                ) + fadeIn(animationSpec = tween(160)),
                                exit = scaleOut(animationSpec = tween(110)) + fadeOut(animationSpec = tween(110)),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
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
private fun RescheduleHabitDialog(
    item: HabitCardItem,
    currentDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirmReschedule: (LocalDate) -> Unit,
) {
    val tokens = RhythmTheme.tokens
    var selectedTargetDate by remember(item.habit.id, currentDate) {
        mutableStateOf(currentDate.plusDays(1))
    }
    var visibleMonth by remember(item.habit.id, currentDate) {
        mutableStateOf(YearMonth.from(currentDate.plusDays(1)))
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) }

    val isSameDate = selectedTargetDate == currentDate
    val isAlreadyScheduled = !isSameDate && CadenceEvaluator.isHabitScheduledOn(item.habit, selectedTargetDate)
    val canConfirm = !isSameDate && !isAlreadyScheduled

    val quickOptions = remember(currentDate) {
        listOf(
            "Tomorrow" to currentDate.plusDays(1),
            "+2 Days" to currentDate.plusDays(2),
            "Next Week" to currentDate.plusWeeks(1),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RhythmSurfaceContainerLowest,
        shape = RoundedCornerShape(14.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "RESCHEDULE SESSION",
                    style = tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurfaceVariant,
                )
                Text(
                    text = item.habit.title,
                    style = tokens.headlineMd,
                    color = RhythmOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Moving from ${currentDate.format(dateFormatter)}",
                    style = tokens.bodySm,
                    color = RhythmOnSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    quickOptions.forEach { (label, optionDate) ->
                        val selected = selectedTargetDate == optionDate
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selected) RhythmPrimary else RhythmSurfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedTargetDate = optionDate
                                    visibleMonth = YearMonth.from(optionDate)
                                },
                        ) {
                            Text(
                                text = label,
                                style = tokens.labelSm,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            )
                        }
                    }
                }

                // Month navigation header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            tint = RhythmOnSurface,
                        )
                    }
                    Text(
                        text = visibleMonth.format(monthFormatter).uppercase(Locale.ENGLISH),
                        style = tokens.labelMd,
                        fontWeight = FontWeight.SemiBold,
                        color = RhythmOnSurface,
                    )
                    IconButton(
                        onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next month",
                            tint = RhythmOnSurface,
                        )
                    }
                }

                // Weekday header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLetter ->
                        Text(
                            text = dayLetter,
                            style = tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Calendar grid
                val firstOfMonth = visibleMonth.atDay(1)
                val leadingBlanks = firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value
                val totalDays = visibleMonth.lengthOfMonth()
                val cells = buildList<LocalDate?> {
                    repeat(leadingBlanks) { add(null) }
                    for (day in 1..totalDays) {
                        add(visibleMonth.atDay(day))
                    }
                    while (size % 7 != 0) add(null)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    cells.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            week.forEach { date ->
                                if (date == null) {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val isSelected = date == selectedTargetDate
                                    val isCurrent = date == currentDate
                                    val hasScheduledInstance = CadenceEvaluator.isHabitScheduledOn(item.habit, date)
                                    val bg = when {
                                        isSelected && isAlreadyScheduled -> RhythmError
                                        isSelected -> RhythmPrimary
                                        isCurrent -> RhythmSurfaceContainerHigh
                                        else -> RhythmSurfaceContainerLow
                                    }
                                    val fg = when {
                                        isSelected -> RhythmOnPrimary
                                        isCurrent -> RhythmOnSurface
                                        else -> RhythmOnSurface
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bg)
                                            .border(
                                                1.dp,
                                                if (isCurrent && !isSelected) RhythmOutlineVariant else Color.Transparent,
                                                RoundedCornerShape(6.dp),
                                            )
                                            .clickable { selectedTargetDate = date },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = fg,
                                            )
                                            if (hasScheduledInstance) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) RhythmOnPrimary else item.habit.accentColor.color,
                                                        ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isAlreadyScheduled) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RhythmError.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, RhythmError.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = RhythmError,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "A scheduled instance of \"${item.habit.title}\" already exists on ${selectedTargetDate.format(dateFormatter)}. Shifts to dates with an existing scheduled instance are not allowed.",
                                style = tokens.bodySm,
                                color = RhythmError,
                            )
                        }
                    }
                } else if (isSameDate) {
                    Text(
                        text = "Select a different date from ${currentDate.format(dateFormatter)}.",
                        style = tokens.bodySm,
                        color = RhythmOnSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReschedule(selectedTargetDate) },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RhythmPrimary,
                    contentColor = RhythmOnPrimary,
                    disabledContainerColor = RhythmSurfaceContainerHigh,
                    disabledContentColor = RhythmOnSurfaceVariant,
                ),
            ) {
                Text("Reschedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = RhythmOnSurfaceVariant)
            }
        },
    )
}

@Composable
private fun EmptyDayCard(
    onCreateHabit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = RhythmSurfaceContainerLowest,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCreateHabit),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "NO HABITS SCHEDULED",
                style = RhythmTheme.tokens.labelMd,
                color = RhythmOnSurface,
            )
            Text(
                text = "Tap + to add a habit for this cadence window.",
                style = RhythmTheme.tokens.bodySm,
                color = RhythmOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DailyMomentumCard(
    lead: String,
    highlight: String,
    tail: String,
) {
    Surface(
        color = RhythmSurfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(RhythmSurfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoGraph,
                    contentDescription = null,
                    tint = RhythmOnSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "DAILY MOMENTUM",
                    style = RhythmTheme.tokens.labelSm,
                    color = RhythmOnSurfaceVariant,
                )
                AnimatedContent(
                    targetState = Triple(lead, highlight, tail),
                    transitionSpec = {
                        fadeIn(tween(240)) togetherWith fadeOut(tween(180))
                    },
                    label = "momentumInsightCopy",
                ) { (l, h, t) ->
                    Text(
                        text = buildAnnotatedString {
                            append(l)
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = RhythmOnSurface)) {
                                append(h)
                            }
                            append(t)
                        },
                        style = RhythmTheme.tokens.bodySm,
                        color = RhythmOnSurface,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    val today = LocalDate.of(2024, 10, 24)
    val totalWeeks = 32
    val sampleColumns = List(totalWeeks) { col ->
        List(7) { row ->
            val isFuture = col == totalWeeks - 1 && row > 3
            val scheduledCount = if (row == 6 && col % 2 == 0) 0 else 4
            val completedCount = when {
                isFuture -> 0
                scheduledCount == 0 -> 0
                (col + row) % 4 == 0 -> 0
                (col + row) % 4 == 1 -> 1
                (col + row) % 4 == 2 -> 3
                else -> 4
            }
            MatrixCell(
                date = today.minusDays(((totalWeeks - 1 - col) * 7 + (6 - row)).toLong()),
                intensity = when {
                    completedCount <= 0 -> MatrixIntensity.NONE
                    completedCount < 2 -> MatrixIntensity.LOW
                    completedCount < 4 -> MatrixIntensity.MEDIUM
                    else -> MatrixIntensity.HIGH
                },
                isFuture = isFuture,
                scheduledCount = scheduledCount,
                completedCount = completedCount,
            )
        }
    }
    val previewState = DashboardUiState(
        isLoading = false,
        today = today,
        selectedDate = today,
        dateLabel = "THURSDAY, OCT 24",
        weekLabel = "WEEK 43 · CADENCE",
        completedCount = 4,
        totalCount = 6,
        bucketProgress = listOf(
            BucketProgress(TimeOfDayBucket.MORNING, 2, 2),
            BucketProgress(TimeOfDayBucket.DURING_THE_DAY, 1, 2),
            BucketProgress(TimeOfDayBucket.EVENING, 1, 2),
        ),
        cadenceMatrix = CadenceMatrixData(
            columns = sampleColumns,
            monthLabels = listOf(
                MatrixMonthLabel(0, "Mar"),
                MatrixMonthLabel(4, "Apr"),
                MatrixMonthLabel(9, "May"),
                MatrixMonthLabel(13, "Jun"),
                MatrixMonthLabel(17, "Jul"),
                MatrixMonthLabel(22, "Aug"),
                MatrixMonthLabel(26, "Sep"),
                MatrixMonthLabel(30, "Oct"),
            ),
            consistencyPercent = 86,
            overallStreakDays = 14,
            completionsThisMonth = 42,
        ),
        weeklyHorizon = listOf("M", "T", "W", "T", "F", "S", "S").mapIndexed { idx, l ->
            WeekHorizonDay(
                date = today.minusDays((3 - idx).toLong()),
                dayLetter = l,
                dayOfMonth = 21 + idx,
                isSelected = idx == 3,
                isToday = idx == 3,
                dotState = if (idx <= 3) WeekDayDotState.ACTIVE_COMPLETED else WeekDayDotState.PENDING_OR_FUTURE,
            )
        },
        domainFilters = listOf("All Habits", "Mind", "Health", "Focus"),
        pendingHabits = listOf(
            HabitCardItem(
                habit = Habit(
                    id = 5,
                    title = "Evening Reflection",
                    description = "3 gratitudes + friction review",
                    domain = "Mind",
                    subTag = "Reflection",
                    colorHex = HabitAccentColor.ROSE_CORAL.hex,
                    targetTimes = listOf(TargetTimeSlot("p1", 21, 30, true)),
                ),
                isCompleted = false,
                streakDays = 19,
            ),
            HabitCardItem(
                habit = Habit(
                    id = 6,
                    title = "Screen-free by 10:30 PM",
                    description = "Analog wind-down · Dim room lighting",
                    domain = "Rest",
                    subTag = "Sleep",
                    colorHex = HabitAccentColor.VIOLET_PURPLE.hex,
                    targetTimes = listOf(TargetTimeSlot("p2", 22, 30, true)),
                ),
                isCompleted = false,
                streakDays = 3,
            ),
        ),
        completedHabits = listOf(
            HabitCardItem(
                habit = Habit(
                    id = 1,
                    title = "Deep Work (90 min)",
                    description = "08:00 - 09:30 AM · Distraction-free block",
                    domain = "Focus",
                    colorHex = HabitAccentColor.ROYAL_BLUE.hex,
                    cadenceType = CadenceType.DAILY,
                    targetTimes = listOf(TargetTimeSlot("p3", 8, 0, true)),
                ),
                isCompleted = true,
                streakDays = 24,
            ),
        ),
        momentumInsightLead = "Completing ",
        momentumInsightHighlight = "Deep Work",
        momentumInsightTail = " early sets you on track for a 100% cadence score this week.",
    )
    RhythmTheme {
        DashboardScreenContent(
            uiState = previewState,
            onSelectDate = {},
            onSelectDomain = {},
            onCycleSortMode = {},
            onSelectSortMode = {},
            onToggleHabit = { _, _, _ -> },
            onEditHabit = {},
            onCreateHabit = {},
        )
    }
}
