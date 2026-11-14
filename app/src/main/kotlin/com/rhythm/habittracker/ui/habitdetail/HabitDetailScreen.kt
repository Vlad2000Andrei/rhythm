package com.rhythm.habittracker.ui.habitdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import com.rhythm.habittracker.ui.components.RhythmMatrixCell
import com.rhythm.habittracker.ui.components.rhythmMatrixCellColor
import com.rhythm.habittracker.ui.theme.RhythmDeepGraphite
import com.rhythm.habittracker.ui.theme.RhythmOnPrimary
import com.rhythm.habittracker.ui.theme.RhythmOnSurface
import com.rhythm.habittracker.ui.theme.RhythmOnSurfaceVariant
import com.rhythm.habittracker.ui.theme.RhythmPrimary
import com.rhythm.habittracker.ui.theme.RhythmSurface
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerHigh
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLow
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLowest
import com.rhythm.habittracker.ui.theme.RhythmTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun HabitDetailScreen(
    viewModel: HabitDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditHabit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isLoading, uiState.habit) {
        if (!uiState.isLoading && uiState.habit == null) {
            onNavigateBack()
        }
    }

    HabitDetailScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEditHabit = onEditHabit,
        modifier = modifier,
    )
}

@Composable
fun HabitDetailScreenContent(
    uiState: HabitDetailUiState,
    onNavigateBack: () -> Unit,
    onEditHabit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit = uiState.habit
    val accent = habit?.accentColor?.color ?: HabitAccentColor.ROYAL_BLUE.color

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RhythmSurface,
        topBar = {
            HabitDetailTopBar(onNavigateBack = onNavigateBack)
        },
    ) { innerPadding ->
        if (habit == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            HabitOverviewCard(
                habit = habit,
                accent = accent,
                currentStreak = uiState.currentStreak,
                bestStreak = uiState.bestStreak,
                completionRatePercent = uiState.completionRatePercent,
                completedScheduledDays = uiState.completedScheduledDays,
                totalScheduledDays = uiState.totalScheduledDays,
                scheduleShortLabel = uiState.scheduleShortLabel,
            )

            HabitCompletionMatrixCard(
                accent = accent,
                cadenceDisciplineLabel = uiState.cadenceDisciplineLabel,
                cadenceBadgeLabel = uiState.cadenceBadgeLabel,
                currentStreak = uiState.currentStreak,
                recentSkips = uiState.recentSkips,
                matrixColumns = uiState.matrixColumns,
            )

            TargetScheduleReadOnlyCard(
                habit = habit,
                accent = accent,
                scheduleShortLabel = uiState.scheduleShortLabel,
            )

            RecentRunWeekCard(
                accent = accent,
                recentWeekDays = uiState.recentWeekDays,
                completedThisWeek = uiState.completedThisWeek,
                scheduledThisWeek = uiState.scheduledThisWeek,
            )

            Button(
                onClick = { onEditHabit(habit.id) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RhythmPrimary,
                    contentColor = RhythmOnPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Habit",
                    style = RhythmTheme.tokens.bodyMd,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HabitDetailTopBar(
    onNavigateBack: () -> Unit,
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
                    .height(64.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Go back",
                        tint = RhythmOnSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Text(
                    text = "HABIT DETAIL",
                    style = RhythmTheme.tokens.labelMd,
                    color = RhythmOnSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.size(44.dp))
            }
            HorizontalDivider(color = RhythmSurfaceContainerHigh.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun HabitOverviewCard(
    habit: Habit,
    accent: Color,
    currentStreak: Int,
    bestStreak: Int,
    completionRatePercent: Int,
    completedScheduledDays: Int,
    totalScheduledDays: Int,
    scheduleShortLabel: String,
) {
    val badgeText = habit.badgeLabel.ifBlank { habit.cadenceType.label }
        .uppercase(Locale.ENGLISH)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = RhythmSurfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accent),
                        )
                        Text(
                            text = badgeText,
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "ACTIVE CADENCE",
                        style = RhythmTheme.tokens.labelSm,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = habit.title,
                    style = RhythmTheme.tokens.displayMobile,
                    color = RhythmOnSurface,
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RhythmSurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val intentionCopy = if (habit.description.isNotBlank()) {
                        "“${habit.description.trim('"', '\u201C', '\u201D', ' ')}”"
                    } else {
                        "“Scheduled cadence: $scheduleShortLabel.”"
                    }
                    Text(
                        text = intentionCopy,
                        style = RhythmTheme.tokens.bodyMd,
                        fontStyle = FontStyle.Italic,
                        color = RhythmOnSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RhythmSurfaceContainerLow,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "CURRENT",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "$currentStreak",
                                style = RhythmTheme.tokens.counterHeroMobile,
                                color = accent,
                            )
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier
                                    .padding(bottom = 4.dp)
                                    .size(18.dp),
                            )
                        }
                        Text(
                            text = "DAYS CONTINUOUS",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RhythmSurfaceContainerLow,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "BEST",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                        )
                        Text(
                            text = "$bestStreak",
                            style = RhythmTheme.tokens.counterHeroMobile,
                            color = RhythmOnSurface,
                        )
                        Text(
                            text = "DAYS RECORD",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RhythmSurfaceContainerLow,
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "RATE",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "$completionRatePercent",
                                style = RhythmTheme.tokens.counterHeroMobile,
                                color = RhythmOnSurface,
                            )
                            Text(
                                text = "%",
                                style = RhythmTheme.tokens.counterMd,
                                color = RhythmOnSurfaceVariant,
                                modifier = Modifier.padding(bottom = 3.dp),
                            )
                        }
                        Text(
                            text = "$completedScheduledDays / $totalScheduledDays DAYS",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitCompletionMatrixCard(
    accent: Color,
    cadenceDisciplineLabel: String,
    cadenceBadgeLabel: String,
    currentStreak: Int,
    recentSkips: Int,
    matrixColumns: List<List<HabitDetailMatrixCell>>,
) {
    val matrixScrollState = rememberScrollState()
    LaunchedEffect(matrixColumns.size) {
        if (matrixScrollState.maxValue > 0) {
            matrixScrollState.scrollTo(matrixScrollState.maxValue)
        }
    }

    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "COMPLETION MATRIX",
                        style = RhythmTheme.tokens.labelMd,
                        fontWeight = FontWeight.SemiBold,
                        color = RhythmOnSurface,
                    )
                    Text(
                        text = "PAST ${matrixColumns.size} WEEKS · $cadenceDisciplineLabel",
                        style = RhythmTheme.tokens.labelSm,
                        color = RhythmOnSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accent.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = cadenceBadgeLabel,
                        style = RhythmTheme.tokens.labelSm,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    dayLabels.forEach { letter ->
                        Box(
                            modifier = Modifier.height(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = letter,
                                style = RhythmTheme.tokens.labelSm,
                                color = RhythmOnSurfaceVariant,
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(134.dp)
                        .background(RhythmSurfaceContainerHigh),
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(matrixScrollState)
                        .padding(start = 10.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    matrixColumns.forEach { column ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            column.forEach { cell ->
                                RhythmMatrixCell(
                                    completionRatio = cell.completionRatio,
                                    isUnscheduledEmpty = cell.isUnscheduledEmpty,
                                    activeColor = accent,
                                    modifier = Modifier.size(14.dp),
                                    emptyColor = RhythmSurfaceContainerHigh,
                                    futureColor = RhythmSurfaceContainerHigh.copy(alpha = 0.45f),
                                    isFuture = cell.isFuture,
                                    cornerRadius = 2.dp,
                                    borderWidth = if (cell.isToday) 1.5.dp else 0.dp,
                                    borderColor = if (cell.isToday) RhythmDeepGraphite else Color.Transparent,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Current: $currentStreak-day active run · $recentSkips skips",
                    style = RhythmTheme.tokens.labelSm,
                    fontWeight = FontWeight.Medium,
                    color = RhythmOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
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
                                        activeColor = accent,
                                        emptyColor = RhythmSurfaceContainerHigh,
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
private fun TargetScheduleReadOnlyCard(
    habit: Habit,
    accent: Color,
    scheduleShortLabel: String,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TARGET SCHEDULE",
                    style = RhythmTheme.tokens.labelMd,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurface,
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = RhythmSurfaceContainerHigh,
                ) {
                    Text(
                        text = "READ ONLY",
                        style = RhythmTheme.tokens.labelSm,
                        fontWeight = FontWeight.Medium,
                        color = RhythmOnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                habit.targetTimes.forEach { slot ->
                    val windowName = TimeOfDayBucket.fromSlot(slot).label
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = RhythmSurfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accent.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = slot.formatted(),
                                        style = RhythmTheme.tokens.counterMd,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RhythmOnSurface,
                                    )
                                    Text(
                                        text = "$windowName Window · $scheduleShortLabel",
                                        style = RhythmTheme.tokens.bodySm,
                                        color = RhythmOnSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (slot.enabled) {
                                    accent.copy(alpha = 0.12f)
                                } else {
                                    RhythmSurfaceContainerHigh
                                },
                            ) {
                                Text(
                                    text = if (slot.enabled) "ACTIVE" else "PAUSED",
                                    style = RhythmTheme.tokens.labelSm,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (slot.enabled) accent else RhythmOnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RhythmSurfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RhythmSurfaceContainerHigh),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    tint = RhythmOnSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Auto-Mute on Early Finish",
                                    style = RhythmTheme.tokens.bodyMd,
                                    fontWeight = FontWeight.Medium,
                                    color = RhythmOnSurface,
                                )
                                Text(
                                    text = "Skips reminder if session completed beforehand",
                                    style = RhythmTheme.tokens.bodySm,
                                    color = RhythmOnSurfaceVariant,
                                )
                            }
                        }

                        Text(
                            text = if (habit.skipReminderIfCompleted) "ENABLED" else "DISABLED",
                            style = RhythmTheme.tokens.labelSm,
                            fontWeight = FontWeight.Medium,
                            color = RhythmOnSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRunWeekCard(
    accent: Color,
    recentWeekDays: List<HabitWeekDayStatus>,
    completedThisWeek: Int,
    scheduledThisWeek: Int,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "RECENT RUN (THIS WEEK)",
                    style = RhythmTheme.tokens.labelMd,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurface,
                )
                Text(
                    text = "$completedThisWeek / $scheduledThisWeek LOGGED",
                    style = RhythmTheme.tokens.labelSm,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                recentWeekDays.forEach { day ->
                    val cellBg = if (day.isToday) {
                        accent.copy(alpha = 0.10f)
                    } else {
                        RhythmSurfaceContainerLow
                    }
                    val cellAlpha = if (day.isFuture && !day.isCompleted) 0.6f else 1f
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = cellBg,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(cellAlpha),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = day.dayLetter,
                                style = RhythmTheme.tokens.labelSm,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (day.isToday) accent else RhythmOnSurfaceVariant,
                            )

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (day.isCompleted) {
                                            accent
                                        } else {
                                            RhythmSurfaceContainerHigh
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (day.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Completed",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(if (day.isScheduled) 6.dp else 4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (day.isScheduled) {
                                                    RhythmOnSurfaceVariant
                                                } else {
                                                    RhythmOnSurfaceVariant.copy(alpha = 0.4f)
                                                },
                                            ),
                                    )
                                }
                            }

                            Text(
                                text = "${day.dayOfMonth}",
                                style = RhythmTheme.tokens.counterSm,
                                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (day.isToday) accent else RhythmOnSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FB)
@Composable
fun HabitDetailScreenPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val previewHabit = Habit(
        id = 1L,
        title = "Deep Work",
        description = "After brewing double-shot espresso, terminal is open, phone in drawer. 90 minutes uninterrupted focus.",
        domain = "Focus",
        subTag = "Block 01",
        colorHex = HabitAccentColor.ROYAL_BLUE.hex,
        cadenceType = CadenceType.DAILY,
        targetTimes = listOf(TargetTimeSlot("t1", 8, 30, true)),
        skipReminderIfCompleted = true,
    )
    val sampleColumns = (0 until 20).map { col ->
        (0 until 7).map { row ->
            val date = monday.minusWeeks((19 - col).toLong()).plusDays(row.toLong())
            val isFuture = date.isAfter(today)
            val scheduledCount = if (row == 6) 0 else 2
            val completedCount = when {
                isFuture || scheduledCount == 0 -> 0
                else -> (col + row) % 3
            }
            HabitDetailMatrixCell(
                date = date,
                scheduledCount = scheduledCount,
                completedCount = completedCount,
                isFuture = isFuture,
                isToday = date == today,
            )
        }
    }
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    val sampleWeek = (0 until 7).map { idx ->
        val date = monday.plusDays(idx.toLong())
        HabitWeekDayStatus(
            date = date,
            dayLetter = letters[idx],
            dayOfMonth = date.dayOfMonth,
            isToday = date == today,
            isFuture = date.isAfter(today),
            isCompleted = !date.isAfter(today),
            isScheduled = true,
        )
    }

    RhythmTheme {
        HabitDetailScreenContent(
            uiState = HabitDetailUiState(
                isLoading = false,
                habit = previewHabit,
                today = today,
                currentStreak = 42,
                bestStreak = 56,
                completionRatePercent = 94,
                completedScheduledDays = 128,
                totalScheduledDays = 136,
                matrixWeeks = 20,
                matrixColumns = sampleColumns,
                recentSkips = 0,
                cadenceDisciplineLabel = "DAILY DISCIPLINE",
                cadenceBadgeLabel = "DAILY",
                scheduleShortLabel = "DAILY · 08:30 AM",
                recentWeekDays = sampleWeek,
                completedThisWeek = 6,
                scheduledThisWeek = 7,
            ),
            onNavigateBack = {},
            onEditHabit = {},
        )
    }
}
