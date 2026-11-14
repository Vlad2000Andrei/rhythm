package com.rhythm.habittracker.ui.allhabits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.rhythm.habittracker.ui.theme.RhythmDeepGraphite
import com.rhythm.habittracker.ui.theme.RhythmOnPrimary
import com.rhythm.habittracker.ui.theme.RhythmOnSurface
import com.rhythm.habittracker.ui.theme.RhythmOnSurfaceVariant
import com.rhythm.habittracker.ui.theme.RhythmPrimary
import com.rhythm.habittracker.ui.theme.RhythmSecondary
import com.rhythm.habittracker.ui.theme.RhythmSurface
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainer
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerHigh
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLow
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLowest
import com.rhythm.habittracker.ui.theme.RhythmTheme
import java.time.DayOfWeek
import java.util.Locale

@Composable
fun AllHabitsScreen(
    viewModel: AllHabitsViewModel,
    onNavigateBack: () -> Unit,
    onOpenHabitDetail: (Long) -> Unit,
    onEditHabit: (Long) -> Unit,
    onCreateHabit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AllHabitsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onClearSearch = viewModel::clearSearch,
        onSelectDomain = viewModel::selectDomain,
        onResetFilters = viewModel::resetFilters,
        onOpenHabitDetail = onOpenHabitDetail,
        onEditHabit = onEditHabit,
        onCreateHabit = onCreateHabit,
        modifier = modifier,
    )
}

@Composable
fun AllHabitsScreenContent(
    uiState: AllHabitsUiState,
    onNavigateBack: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectDomain: (String) -> Unit,
    onResetFilters: () -> Unit,
    onOpenHabitDetail: (Long) -> Unit,
    onEditHabit: (Long) -> Unit,
    onCreateHabit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RhythmSurface,
        topBar = {
            AllHabitsTopBar(onNavigateBack = onNavigateBack)
        },
        floatingActionButton = {
            Surface(
                onClick = onCreateHabit,
                shape = CircleShape,
                color = RhythmPrimary,
                contentColor = RhythmOnPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(48.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "NEW HABIT",
                        style = RhythmTheme.tokens.labelMd,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "inventory_telemetry") {
                SystemInventoryHeader(
                    totalHabits = uiState.totalHabits,
                    meanStreakDays = uiState.meanStreakDays,
                    completionRatePercent = uiState.completionRatePercent,
                )
            }

            item(key = "query_tools") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HabitSearchBar(
                        query = uiState.searchQuery,
                        onQueryChanged = onSearchQueryChanged,
                        onClearSearch = onClearSearch,
                    )

                    if (uiState.filterChips.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                        ) {
                            items(
                                items = uiState.filterChips,
                                key = { it.key },
                            ) { chip ->
                                val isSelected = chip.key == uiState.selectedDomainKey
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) RhythmDeepGraphite else RhythmSurfaceContainerLow,
                                    modifier = Modifier
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onSelectDomain(chip.key) },
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                    ) {
                                        Text(
                                            text = chip.label,
                                            style = RhythmTheme.tokens.labelMd,
                                            color = if (isSelected) Color.White else RhythmOnSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.filteredHabits.isNotEmpty()) {
                items(
                    items = uiState.filteredHabits,
                    key = { it.habit.id },
                ) { item ->
                    AllHabitCard(
                        item = item,
                        onOpenDetail = { onOpenHabitDetail(item.habit.id) },
                        onEdit = { onEditHabit(item.habit.id) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(220),
                            placementSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            fadeOutSpec = tween(180),
                        ),
                    )
                }
            } else if (!uiState.isLoading) {
                item(key = "empty_inventory") {
                    ZeroCadencesFoundCard(
                        hasAnyHabits = uiState.totalHabits > 0,
                        onResetFilters = onResetFilters,
                        onCreateHabit = onCreateHabit,
                    )
                }
            }
        }
    }
}

@Composable
private fun AllHabitsTopBar(
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
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(44.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Go back",
                        tint = RhythmOnSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "HABITS",
                    style = RhythmTheme.tokens.headlineMd,
                    color = RhythmOnSurface,
                    maxLines = 1,
                )
            }
            HorizontalDivider(color = RhythmSurfaceContainerHigh.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun SystemInventoryHeader(
    totalHabits: Int,
    meanStreakDays: Int,
    completionRatePercent: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "SYSTEM INVENTORY",
                style = RhythmTheme.tokens.labelSm,
                color = RhythmOnSurfaceVariant,
            )
            Text(
                text = "ACTIVE CADENCES · %02d REGISTERED".format(totalHabits),
                style = RhythmTheme.tokens.labelSm,
                fontWeight = FontWeight.SemiBold,
                color = RhythmSecondary,
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = RhythmSurfaceContainer,
            border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TelemetryMetricTile(
                    label = "TOTAL",
                    value = "%02d".format(totalHabits),
                    unit = "RUT",
                    modifier = Modifier.weight(1f),
                )
                TelemetryMetricTile(
                    label = "MEAN STREAK",
                    value = "%02d".format(meanStreakDays),
                    unit = "DAYS",
                    modifier = Modifier.weight(1f),
                )
                TelemetryMetricTile(
                    label = "COMPLETION",
                    value = "$completionRatePercent",
                    unit = "%",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = RhythmSurfaceContainerLowest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = RhythmTheme.tokens.labelSm,
                color = RhythmOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = RhythmTheme.tokens.counterHeroMobile,
                    color = RhythmOnSurface,
                )
                Text(
                    text = unit,
                    style = RhythmTheme.tokens.labelSm,
                    color = RhythmSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HabitSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = RhythmSecondary,
                modifier = Modifier.size(18.dp),
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Filter by cadence, title, or cue...",
                        style = RhythmTheme.tokens.bodySm,
                        color = RhythmOnSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    textStyle = RhythmTheme.tokens.bodySm.copy(color = RhythmOnSurface),
                    cursorBrush = SolidColor(RhythmPrimary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(120)),
            ) {
                Surface(
                    shape = CircleShape,
                    color = RhythmSurfaceContainerHigh,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onClearSearch() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear query",
                            tint = RhythmOnSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllHabitCard(
    item: AllHabitListItem,
    onOpenDetail: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = item.habit.accentColor.color
    val headerLabel = item.habit.badgeLabel.ifBlank { item.habit.cadenceType.label }
        .uppercase(Locale.ENGLISH)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenDetail() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = headerLabel,
                        style = RhythmTheme.tokens.labelSm,
                        color = RhythmSecondary,
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
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "%02d".format(item.streakDays),
                            style = RhythmTheme.tokens.counterMd,
                            color = RhythmOnSurface,
                        )
                        Text(
                            text = "D",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmSecondary,
                        )
                    }
                }

                Text(
                    text = item.habit.title,
                    style = RhythmTheme.tokens.bodyLg,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (item.habit.description.isNotBlank()) {
                    val trimmedQuote = item.habit.description.trim('"', '\u201C', '\u201D', ' ')
                    Text(
                        text = "\"$trimmedQuote\"",
                        style = RhythmTheme.tokens.bodySm,
                        fontStyle = FontStyle.Italic,
                        color = RhythmOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = RhythmSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = item.scheduleSummary,
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = RhythmSurfaceContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onEdit() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = RhythmOnSurface,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "EDIT",
                                style = RhythmTheme.tokens.labelSm,
                                fontWeight = FontWeight.SemiBold,
                                color = RhythmOnSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZeroCadencesFoundCard(
    hasAnyHabits: Boolean,
    onResetFilters: () -> Unit,
    onCreateHabit: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.FolderOff,
                contentDescription = null,
                tint = RhythmSecondary,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = if (hasAnyHabits) "ZERO CADENCES FOUND" else "NO CADENCES REGISTERED",
                style = RhythmTheme.tokens.headlineMd,
                color = RhythmOnSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (hasAnyHabits) {
                    "No registered habits correspond to your current query parameters."
                } else {
                    "Create your first habit architecture to start building your cadence inventory."
                },
                style = RhythmTheme.tokens.bodySm,
                color = RhythmOnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = if (hasAnyHabits) onResetFilters else onCreateHabit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RhythmPrimary,
                    contentColor = RhythmOnPrimary,
                ),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(38.dp),
            ) {
                Text(
                    text = if (hasAnyHabits) "RESET QUERY FILTER" else "CREATE FIRST HABIT",
                    style = RhythmTheme.tokens.labelMd,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FB)
@Composable
fun AllHabitsScreenPreview() {
    RhythmTheme {
        AllHabitsScreenContent(
            uiState = AllHabitsUiState(
                isLoading = false,
                totalHabits = 3,
                meanStreakDays = 29,
                completionRatePercent = 86,
                searchQuery = "",
                selectedDomainKey = "ALL",
                filterChips = listOf(
                    AllHabitFilterChip("ALL", "ALL (3)", 3),
                    AllHabitFilterChip("FOCUS", "FOCUS (1)", 1),
                    AllHabitFilterChip("FITNESS", "FITNESS (1)", 1),
                    AllHabitFilterChip("MIND", "MIND (1)", 1),
                ),
                filteredHabits = listOf(
                    AllHabitListItem(
                        habit = Habit(
                            id = 1L,
                            title = "Deep Work",
                            description = "After brewing double-shot, terminal is open, phone locked in drawer.",
                            domain = "Focus",
                            subTag = "Block 01",
                            colorHex = HabitAccentColor.ROYAL_BLUE.hex,
                            cadenceType = CadenceType.DAILY,
                            targetTimes = listOf(TargetTimeSlot("t1", 8, 30, true)),
                        ),
                        streakDays = 42,
                        scheduleSummary = "DAILY · 08:30 AM",
                    ),
                    AllHabitListItem(
                        habit = Habit(
                            id = 2L,
                            title = "Tempo Run",
                            description = "Shoes set at doorway the prior evening. Target zone: 4:45/km.",
                            domain = "Fitness",
                            subTag = "Endurance",
                            colorHex = HabitAccentColor.EMERALD_GREEN.hex,
                            cadenceType = CadenceType.WEEKLY,
                            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                            targetTimes = listOf(TargetTimeSlot("t2", 7, 0, true)),
                        ),
                        streakDays = 18,
                        scheduleSummary = "WEEKLY · MON, WED, FRI · 07:00 AM",
                    ),
                    AllHabitListItem(
                        habit = Habit(
                            id = 3L,
                            title = "Read Literature",
                            description = "Kindle or paper copy only. No back-lit screens post 21:30.",
                            domain = "Mind",
                            subTag = "Cognition",
                            colorHex = HabitAccentColor.WARM_AMBER.hex,
                            cadenceType = CadenceType.DAILY,
                            targetTimes = listOf(TargetTimeSlot("t3", 21, 45, true)),
                        ),
                        streakDays = 29,
                        scheduleSummary = "DAILY · 09:45 PM",
                    ),
                ),
            ),
            onNavigateBack = {},
            onSearchQueryChanged = {},
            onClearSearch = {},
            onSelectDomain = {},
            onResetFilters = {},
            onOpenHabitDetail = {},
            onEditHabit = {},
            onCreateHabit = {},
        )
    }
}
