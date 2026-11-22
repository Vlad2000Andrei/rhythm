package com.rhythm.habittracker.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import com.rhythm.habittracker.ui.theme.RhythmTheme
import java.time.LocalDate

private fun samplePreviewData(): Pair<List<Habit>, Map<LocalDate, Map<Long, Set<Int>>>> {
    val today = LocalDate.of(2026, 10, 24)
    val deepWork = Habit.singleSchedule(
        id = 1L,
        title = "Deep Work",
        description = "90 min uninterrupted session",
        domain = "Focus",
        subTag = "Block 01",
        colorHex = HabitAccentColor.ROYAL_BLUE.hex,
        cadenceType = CadenceType.DAILY,
        targetTimes = listOf(TargetTimeSlot(id = "t1", hour = 8, minute = 30)),
        timeOfDayBucket = TimeOfDayBucket.MORNING,
        createdAtEpochDay = today.minusDays(90).toEpochDay(),
    )
    val reading = Habit.singleSchedule(
        id = 2L,
        title = "10-Page Reading",
        description = "Non-fiction chapters",
        domain = "Mind",
        colorHex = HabitAccentColor.EMERALD_GREEN.hex,
        cadenceType = CadenceType.DAILY,
        targetTimes = listOf(TargetTimeSlot(id = "t2", hour = 8, minute = 0)),
        timeOfDayBucket = TimeOfDayBucket.MORNING,
        createdAtEpochDay = today.minusDays(90).toEpochDay(),
    )
    val hydration = Habit.singleSchedule(
        id = 3L,
        title = "Hydration Target",
        description = "2.5L electrolyte water",
        domain = "Health",
        colorHex = HabitAccentColor.WARM_AMBER.hex,
        cadenceType = CadenceType.DAILY,
        targetPerPeriod = 2,
        targetTimes = listOf(
            TargetTimeSlot(id = "t3a", hour = 9, minute = 0),
            TargetTimeSlot(id = "t3b", hour = 13, minute = 30),
        ),
        timeOfDayBucket = TimeOfDayBucket.DURING_THE_DAY,
        createdAtEpochDay = today.minusDays(90).toEpochDay(),
    )
    val journal = Habit.singleSchedule(
        id = 4L,
        title = "Evening Reflection",
        description = "Daily debrief & journal",
        domain = "Mind",
        colorHex = HabitAccentColor.VIOLET_PURPLE.hex,
        cadenceType = CadenceType.DAILY,
        targetTimes = listOf(TargetTimeSlot(id = "t4", hour = 21, minute = 0)),
        timeOfDayBucket = TimeOfDayBucket.EVENING,
        createdAtEpochDay = today.minusDays(90).toEpochDay(),
    )

    val completions = buildMap<LocalDate, Map<Long, Set<Int>>> {
        for (i in 0..84) {
            val d = today.minusDays(i.toLong())
            if (i == 0) {
                put(
                    d,
                    mapOf(
                        2L to setOf(0),
                        3L to setOf(0),
                    ),
                )
            } else if (i % 7 != 6) {
                put(
                    d,
                    mapOf(
                        1L to setOf(0),
                        2L to setOf(0),
                        3L to if (i % 3 == 0) setOf(0) else setOf(0, 1),
                        4L to setOf(0),
                    ),
                )
            }
        }
    }
    return listOf(deepWork, reading, hydration, journal) to completions
}

@Composable
private fun WidgetBitmapPreviewContainer(
    themeMode: WidgetThemeMode,
    widthDp: Int,
    heightDp: Int,
    render: (android.content.Context) -> android.graphics.Bitmap,
) {
    val context = LocalContext.current
    val bmp = remember(themeMode, widthDp, heightDp) { render(context) }
    RhythmTheme {
        Box(
            modifier = Modifier
                .background(if (themeMode == WidgetThemeMode.LIGHT) Color(0xFF121417) else Color(0xFF23272F))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .width(widthDp.dp)
                    .fillMaxWidth()
                    .aspectRatio(widthDp.toFloat() / heightDp.toFloat()),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Preview(name = "Single Habit 4x2 Light")
@Composable
fun SingleHabitWideLightPreview() {
    val (habits, completions) = remember { samplePreviewData() }
    val data = remember { WidgetDataBuilder.buildSingleHabitData(habits, completions, 1L, LocalDate.of(2026, 10, 24)) }
    WidgetBitmapPreviewContainer(WidgetThemeMode.LIGHT, 330, 180) { ctx ->
        WidgetBitmapRenderer.renderSingleHabitWidget(ctx, data, WidgetThemeMode.LIGHT, 330, 180)
    }
}

@Preview(name = "Single Habit 4x4 Dark")
@Composable
fun SingleHabitSquareDarkPreview() {
    val (habits, completions) = remember { samplePreviewData() }
    val data = remember { WidgetDataBuilder.buildSingleHabitData(habits, completions, 1L, LocalDate.of(2026, 10, 24)) }
    WidgetBitmapPreviewContainer(WidgetThemeMode.DARK, 330, 330) { ctx ->
        WidgetBitmapRenderer.renderSingleHabitWidget(ctx, data, WidgetThemeMode.DARK, 330, 330)
    }
}

@Preview(name = "Overall Matrix 4x4 Light")
@Composable
fun OverallMatrixSquareLightPreview() {
    val (habits, completions) = remember { samplePreviewData() }
    val data = remember { WidgetDataBuilder.buildOverallMatrixData(habits, completions, LocalDate.of(2026, 10, 24)) }
    WidgetBitmapPreviewContainer(WidgetThemeMode.LIGHT, 330, 330) { ctx ->
        WidgetBitmapRenderer.renderOverallMatrixWidget(ctx, data, WidgetThemeMode.LIGHT, 330, 330)
    }
}

@Preview(name = "Overall Matrix 2x4 Dark")
@Composable
fun OverallMatrixTallDarkPreview() {
    val (habits, completions) = remember { samplePreviewData() }
    val data = remember { WidgetDataBuilder.buildOverallMatrixData(habits, completions, LocalDate.of(2026, 10, 24)) }
    WidgetBitmapPreviewContainer(WidgetThemeMode.DARK, 190, 340) { ctx ->
        WidgetBitmapRenderer.renderOverallMatrixWidget(ctx, data, WidgetThemeMode.DARK, 190, 340)
    }
}

@Preview(name = "Upcoming Tasks 4x4 Light")
@Composable
fun UpcomingTasksSquareLightPreview() {
    val (habits, completions) = remember { samplePreviewData() }
    val data = remember { WidgetDataBuilder.buildUpcomingTasksData(habits, completions, LocalDate.of(2026, 10, 24)) }
    WidgetBitmapPreviewContainer(WidgetThemeMode.LIGHT, 330, 330) { ctx ->
        WidgetBitmapRenderer.renderUpcomingTasksFullPreview(ctx, data, WidgetThemeMode.LIGHT, 330, 330)
    }
}

@Preview(name = "Upcoming Tasks 2x4 Dark")
@Composable
fun UpcomingTasksTallDarkPreview() {
    val (habits, completions) = remember { samplePreviewData() }
    val data = remember { WidgetDataBuilder.buildUpcomingTasksData(habits, completions, LocalDate.of(2026, 10, 24)) }
    WidgetBitmapPreviewContainer(WidgetThemeMode.DARK, 190, 340) { ctx ->
        WidgetBitmapRenderer.renderUpcomingTasksFullPreview(ctx, data, WidgetThemeMode.DARK, 190, 340)
    }
}
