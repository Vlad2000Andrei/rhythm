package com.rhythm.habittracker.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rhythm.habittracker.RhythmApplication
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.ui.components.RhythmBrandMark
import com.rhythm.habittracker.ui.theme.RhythmBackground
import com.rhythm.habittracker.ui.theme.RhythmDeepGraphite
import com.rhythm.habittracker.ui.theme.RhythmOnPrimary
import com.rhythm.habittracker.ui.theme.RhythmOnSurface
import com.rhythm.habittracker.ui.theme.RhythmOnSurfaceVariant
import com.rhythm.habittracker.ui.theme.RhythmPrimary
import com.rhythm.habittracker.ui.theme.RhythmSecondary
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainer
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerHigh
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLow
import com.rhythm.habittracker.ui.theme.RhythmSurfaceContainerLowest
import com.rhythm.habittracker.ui.theme.RhythmTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class PreviewSizeOption(
    val label: String,
    val widthDp: Int,
    val heightDp: Int,
) {
    TALL_2X4("2×4 Tall", 190, 340),
    WIDE_4X2("4×2 / 4×3", 320, 210),
    SQUARE_4X4("4×4 Square", 320, 320),
}

class WidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val manager = AppWidgetManager.getInstance(this)
        val providerName = manager.getAppWidgetInfo(appWidgetId)?.provider?.className
        val widgetKind = when (providerName) {
            HabitCadenceWidgetProvider::class.java.name -> WidgetKind.SINGLE_HABIT_MATRIX
            OverallCadenceWidgetProvider::class.java.name -> WidgetKind.OVERALL_CADENCE_MATRIX
            UpcomingTasksWidgetProvider::class.java.name -> WidgetKind.UPCOMING_TASKS
            else -> WidgetKind.OVERALL_CADENCE_MATRIX
        }

        val initialThemeMode = WidgetPreferencesStore.getThemeMode(this, appWidgetId)
        val initialHabitId = WidgetPreferencesStore.getHabitId(this, appWidgetId)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )

        val app = applicationContext as RhythmApplication
        val repository = app.container.habitRepository

        setContent {
            RhythmTheme {
                val habits by repository.observeHabits().collectAsStateWithLifecycle(initialValue = emptyList())
                val completedInstancesByDate by repository.observeCompletedInstancesByDate()
                    .collectAsStateWithLifecycle(initialValue = emptyMap())
                val coroutineScope = rememberCoroutineScope()

                var selectedThemeMode by remember { mutableStateOf(initialThemeMode) }
                var selectedHabitId by remember { mutableStateOf(initialHabitId) }
                var previewSize by remember {
                    mutableStateOf(
                        if (widgetKind == WidgetKind.SINGLE_HABIT_MATRIX) {
                            PreviewSizeOption.WIDE_4X2
                        } else {
                            PreviewSizeOption.SQUARE_4X4
                        },
                    )
                }

                LaunchedEffect(habits) {
                    if (widgetKind == WidgetKind.SINGLE_HABIT_MATRIX &&
                        habits.isNotEmpty() &&
                        (selectedHabitId == null || habits.none { it.id == selectedHabitId })
                    ) {
                        selectedHabitId = habits.first().id
                    }
                }

                WidgetConfigScreen(
                    widgetKind = widgetKind,
                    selectedThemeMode = selectedThemeMode,
                    onSelectThemeMode = { selectedThemeMode = it },
                    habits = habits,
                    selectedHabitId = selectedHabitId,
                    onSelectHabit = { selectedHabitId = it },
                    previewSize = previewSize,
                    onSelectPreviewSize = { previewSize = it },
                    completedInstancesByDate = completedInstancesByDate,
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    onSave = {
                        WidgetPreferencesStore.saveConfig(
                            context = this@WidgetConfigActivity,
                            appWidgetId = appWidgetId,
                            themeMode = selectedThemeMode,
                            habitId = selectedHabitId,
                        )
                        coroutineScope.launch {
                            when (widgetKind) {
                                WidgetKind.SINGLE_HABIT_MATRIX ->
                                    RhythmWidgetUpdater.updateSingleHabitWidgets(this@WidgetConfigActivity, intArrayOf(appWidgetId))
                                WidgetKind.OVERALL_CADENCE_MATRIX ->
                                    RhythmWidgetUpdater.updateOverallWidgets(this@WidgetConfigActivity, intArrayOf(appWidgetId))
                                WidgetKind.UPCOMING_TASKS ->
                                    RhythmWidgetUpdater.updateUpcomingTasksWidgets(this@WidgetConfigActivity, intArrayOf(appWidgetId))
                            }
                            val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    widgetKind: WidgetKind,
    selectedThemeMode: WidgetThemeMode,
    onSelectThemeMode: (WidgetThemeMode) -> Unit,
    habits: List<Habit>,
    selectedHabitId: Long?,
    onSelectHabit: (Long) -> Unit,
    previewSize: PreviewSizeOption,
    onSelectPreviewSize: (PreviewSizeOption) -> Unit,
    completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    val previewWidthDp = when {
        widgetKind == WidgetKind.SINGLE_HABIT_MATRIX && previewSize == PreviewSizeOption.WIDE_4X2 -> 320
        else -> previewSize.widthDp
    }
    val previewHeightDp = when {
        widgetKind == WidgetKind.SINGLE_HABIT_MATRIX && previewSize == PreviewSizeOption.WIDE_4X2 -> 185
        previewSize == PreviewSizeOption.WIDE_4X2 -> 240
        else -> previewSize.heightDp
    }

    val previewBitmap: Bitmap = remember(
        widgetKind,
        selectedThemeMode,
        selectedHabitId,
        previewWidthDp,
        previewHeightDp,
        habits,
        completedInstancesByDate,
    ) {
        when (widgetKind) {
            WidgetKind.SINGLE_HABIT_MATRIX -> {
                val data = WidgetDataBuilder.buildSingleHabitData(
                    habits = habits,
                    completedInstancesByDate = completedInstancesByDate,
                    configuredHabitId = selectedHabitId,
                    today = today,
                )
                WidgetBitmapRenderer.renderSingleHabitWidget(
                    context = context,
                    data = data,
                    themeMode = selectedThemeMode,
                    widthDp = previewWidthDp,
                    heightDp = previewHeightDp,
                )
            }

            WidgetKind.OVERALL_CADENCE_MATRIX -> {
                val data = WidgetDataBuilder.buildOverallMatrixData(
                    habits = habits,
                    completedInstancesByDate = completedInstancesByDate,
                    today = today,
                )
                WidgetBitmapRenderer.renderOverallMatrixWidget(
                    context = context,
                    data = data,
                    themeMode = selectedThemeMode,
                    widthDp = previewWidthDp,
                    heightDp = previewHeightDp,
                )
            }

            WidgetKind.UPCOMING_TASKS -> {
                val data = WidgetDataBuilder.buildUpcomingTasksData(
                    habits = habits,
                    completedInstancesByDate = completedInstancesByDate,
                    today = today,
                )
                WidgetBitmapRenderer.renderUpcomingTasksFullPreview(
                    context = context,
                    data = data,
                    themeMode = selectedThemeMode,
                    widthDp = previewWidthDp,
                    heightDp = previewHeightDp,
                )
            }
        }
    }

    Scaffold(
        containerColor = RhythmBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RhythmSurfaceContainerLowest)
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RhythmSurfaceContainerLow),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Cancel",
                                tint = RhythmOnSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "WIDGET CONFIGURATION",
                                style = RhythmTheme.tokens.labelSm,
                                color = RhythmSecondary,
                            )
                            Text(
                                text = widgetKind.title,
                                style = RhythmTheme.tokens.headlineMd,
                                color = RhythmOnSurface,
                            )
                        }
                    }
                    RhythmBrandMark(size = 28.dp)
                }
                HorizontalDivider(color = RhythmSurfaceContainerHigh, thickness = 1.dp)
            }
        },
        bottomBar = {
            Surface(
                color = RhythmSurfaceContainerLowest,
                shadowElevation = 8.dp,
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(color = RhythmSurfaceContainerHigh, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RhythmOnSurface),
                        ) {
                            Text(
                                text = "CANCEL",
                                style = RhythmTheme.tokens.labelMd,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .weight(1.6f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RhythmPrimary,
                                contentColor = RhythmOnPrimary,
                            ),
                        ) {
                            Text(
                                text = "APPLY WIDGET",
                                style = RhythmTheme.tokens.labelMd,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Live Preview Stage
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmSecondary,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PreviewSizeOption.entries.forEach { option ->
                                val selected = option == previewSize
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) RhythmPrimary else RhythmSurfaceContainerLowest,
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
                                            shape = RoundedCornerShape(8.dp),
                                        )
                                        .clickable { onSelectPreviewSize(option) }
                                        .padding(horizontal = 9.dp, vertical = 5.dp),
                                ) {
                                    Text(
                                        text = option.label,
                                        style = RhythmTheme.tokens.labelSm,
                                        color = if (selected) RhythmOnPrimary else RhythmOnSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                if (selectedThemeMode == WidgetThemeMode.LIGHT) {
                                    RhythmDeepGraphite
                                } else {
                                    Color(0xFF23272F)
                                },
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Widget live preview",
                            modifier = Modifier
                                .widthIn(max = previewWidthDp.dp)
                                .fillMaxWidth()
                                .aspectRatio(previewWidthDp.toFloat() / previewHeightDp.toFloat()),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

            // Light / Dark Appearance Mode
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SURFACE APPEARANCE",
                        style = RhythmTheme.tokens.labelSm,
                        color = RhythmSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WidgetThemeOptionCard(
                            title = "Light Mode",
                            subtitle = "Alabaster Card",
                            icon = Icons.Outlined.LightMode,
                            swatchColor = Color.White,
                            swatchBorder = RhythmSurfaceContainerHigh,
                            selected = selectedThemeMode == WidgetThemeMode.LIGHT,
                            onClick = { onSelectThemeMode(WidgetThemeMode.LIGHT) },
                            modifier = Modifier.weight(1f),
                        )
                        WidgetThemeOptionCard(
                            title = "Dark Mode",
                            subtitle = "Obsidian Graphite",
                            icon = Icons.Outlined.DarkMode,
                            swatchColor = RhythmDeepGraphite,
                            swatchBorder = Color(0xFF2D323B),
                            selected = selectedThemeMode == WidgetThemeMode.DARK,
                            onClick = { onSelectThemeMode(WidgetThemeMode.DARK) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Habit Picker for Single-Habit Matrix Widget
            if (widgetKind == WidgetKind.SINGLE_HABIT_MATRIX) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "TARGET HABIT TO DISPLAY",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmSecondary,
                        )
                        Text(
                            text = "${habits.size} AVAILABLE",
                            style = RhythmTheme.tokens.labelSm,
                            color = RhythmSecondary,
                        )
                    }
                }

                if (habits.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = RhythmSurfaceContainerLowest,
                            border = BorderStroke(1.dp, RhythmSurfaceContainerHigh),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "No habits created yet",
                                    style = RhythmTheme.tokens.bodyLg,
                                    fontWeight = FontWeight.SemiBold,
                                    color = RhythmOnSurface,
                                )
                                Text(
                                    text = "Apply this widget now, then create a habit in Rhythm to populate your matrix automatically.",
                                    style = RhythmTheme.tokens.bodySm,
                                    color = RhythmOnSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        val isSelected = habit.id == selectedHabitId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectHabit(habit.id) },
                            shape = RoundedCornerShape(16.dp),
                            color = RhythmSurfaceContainerLowest,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) RhythmPrimary else RhythmSurfaceContainerHigh,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(habit.accentColor.color),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = habit.title,
                                        style = RhythmTheme.tokens.bodyLg,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RhythmOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = buildString {
                                            if (habit.badgeLabel.isNotBlank()) {
                                                append(habit.badgeLabel.uppercase())
                                                append(" · ")
                                            }
                                            append(CadenceEvaluator.formatHabitScheduleSummary(habit))
                                        },
                                        style = RhythmTheme.tokens.labelSm,
                                        color = RhythmSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(RhythmPrimary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Selected",
                                            tint = RhythmOnPrimary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetThemeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    swatchColor: Color,
    swatchBorder: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = RhythmSurfaceContainerLowest,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) RhythmPrimary else RhythmSurfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(swatchColor)
                    .border(1.dp, swatchBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (swatchColor == Color.White) RhythmOnSurface else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = RhythmTheme.tokens.bodyMd,
                    fontWeight = FontWeight.SemiBold,
                    color = RhythmOnSurface,
                )
                Text(
                    text = subtitle,
                    style = RhythmTheme.tokens.labelSm,
                    color = RhythmSecondary,
                )
            }
        }
    }
}
