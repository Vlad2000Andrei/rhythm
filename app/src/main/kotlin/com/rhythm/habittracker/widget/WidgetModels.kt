package com.rhythm.habittracker.widget

import com.rhythm.habittracker.domain.MatrixCell
import com.rhythm.habittracker.domain.MatrixMonthLabel
import com.rhythm.habittracker.domain.model.Habit
import java.time.LocalDate

enum class WidgetKind(
    val title: String,
    val subtitle: String,
) {
    SINGLE_HABIT_MATRIX(
        title = "Habit Cadence Matrix",
        subtitle = "Track streak, consistency & matrix for one habit",
    ),
    OVERALL_CADENCE_MATRIX(
        title = "Overall Cadence Matrix",
        subtitle = "Time-block progress & multi-week rhythm heatmap",
    ),
    UPCOMING_TASKS(
        title = "Upcoming Tasks",
        subtitle = "Interactive daily checklist & segmented progress",
    ),
}

enum class WidgetThemeMode(val label: String) {
    LIGHT("Light Mode"),
    DARK("Dark Mode"),
}

data class SingleHabitWidgetData(
    val habit: Habit? = null,
    val today: LocalDate = LocalDate.now(),
    val currentStreak: Int = 0,
    val consistencyPercent: Int = 0,
    val primaryTargetLabel: String = "08:00 AM",
    val subtitleLabel: String = "",
    val domainBadgeLabel: String = "",
    val statusLabel: String = "",
    val isCompletedToday: Boolean = false,
    val isScheduledToday: Boolean = false,
    val matrixColumns: List<List<MatrixCell>> = emptyList(),
)

data class OverallMatrixWidgetData(
    val today: LocalDate = LocalDate.now(),
    val dateShortLabel: String = "",
    val dateLongLabel: String = "",
    val weekShortLabel: String = "",
    val consistencyPercent: Int = 0,
    val overallStreakDays: Int = 0,
    val completionsThisMonth: Int = 0,
    val morningCompleted: Int = 0,
    val morningTotal: Int = 0,
    val afternoonCompleted: Int = 0,
    val afternoonTotal: Int = 0,
    val eveningCompleted: Int = 0,
    val eveningTotal: Int = 0,
    val nextTaskLabel: String = "",
    val hasPendingTasksToday: Boolean = false,
    val matrixColumns: List<List<MatrixCell>> = emptyList(),
    val monthLabels: List<MatrixMonthLabel> = emptyList(),
)

data class WidgetTaskItem(
    val habitId: Long,
    val instanceIndex: Int,
    val totalInstances: Int,
    val title: String,
    val subtitle: String,
    val timeShortLabel: String,
    val accentColorHex: String,
    val isCompleted: Boolean,
    val isNowTarget: Boolean,
)

data class UpcomingTasksWidgetData(
    val today: LocalDate = LocalDate.now(),
    val dateShortLabel: String = "",
    val dateHeaderLabel: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val completionPercent: Int = 0,
    val segmentColorHexes: List<String?> = emptyList(),
    val orderedTasks: List<WidgetTaskItem> = emptyList(),
    val nextUpFooterLabel: String = "",
)
