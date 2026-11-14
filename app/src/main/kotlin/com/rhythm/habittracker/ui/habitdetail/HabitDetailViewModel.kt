package com.rhythm.habittracker.ui.habitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.model.Habit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

data class HabitDetailMatrixCell(
    val date: LocalDate,
    val scheduledCount: Int,
    val completedCount: Int,
    val isFuture: Boolean,
    val isToday: Boolean,
) {
    val isScheduled: Boolean
        get() = scheduledCount > 0

    val completionRatio: Float
        get() = when {
            isFuture -> 0f
            scheduledCount > 0 -> (completedCount.toFloat() / scheduledCount.toFloat()).coerceIn(0f, 1f)
            completedCount > 0 -> 1f
            else -> 0f
        }

    val isUnscheduledEmpty: Boolean
        get() = !isScheduled && (isFuture || completedCount <= 0)
}

data class HabitWeekDayStatus(
    val date: LocalDate,
    val dayLetter: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isCompleted: Boolean,
    val isScheduled: Boolean,
)

data class HabitDetailUiState(
    val isLoading: Boolean = true,
    val habit: Habit? = null,
    val today: LocalDate = LocalDate.now(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completionRatePercent: Int = 0,
    val completedScheduledDays: Int = 0,
    val totalScheduledDays: Int = 0,
    val matrixWeeks: Int = 20,
    val matrixColumns: List<List<HabitDetailMatrixCell>> = emptyList(),
    val recentSkips: Int = 0,
    val cadenceDisciplineLabel: String = "",
    val cadenceBadgeLabel: String = "",
    val scheduleShortLabel: String = "",
    val recentWeekDays: List<HabitWeekDayStatus> = emptyList(),
    val completedThisWeek: Int = 0,
    val scheduledThisWeek: Int = 0,
)

class HabitDetailViewModel(
    private val habitId: Long,
    private val repository: HabitRepository,
    private val clockTodayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    val uiState: StateFlow<HabitDetailUiState> = combine(
        repository.observeHabits(),
        repository.observeCompletedInstancesByDate(),
    ) { habits, completedInstancesByDate ->
        buildUiState(
            habits = habits,
            completedInstancesByDate = completedInstancesByDate,
            today = clockTodayProvider(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitDetailUiState(isLoading = true),
    )

    private fun buildUiState(
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        today: LocalDate,
    ): HabitDetailUiState {
        val habit = habits.firstOrNull { it.id == habitId }
            ?: return HabitDetailUiState(isLoading = false, habit = null, today = today)

        val fullyCompletedDates = buildSet<LocalDate> {
            completedInstancesByDate.forEach { (date, habitMap) ->
                val instances = habitMap[habit.id].orEmpty()
                if (instances.size >= habit.dailyTargetCount) {
                    add(date)
                }
            }
        }

        val currentStreak = CadenceEvaluator.calculateStreak(
            habit = habit,
            completedDates = fullyCompletedDates,
            referenceDate = today,
        )
        val bestStreak = CadenceEvaluator.calculateBestStreak(
            habit = habit,
            completedDates = fullyCompletedDates,
            referenceDate = today,
        )

        val createdDate = LocalDate.ofEpochDay(habit.createdAtEpochDay)
        val earliestCompletion = fullyCompletedDates.minOrNull() ?: createdDate
        val rawEvalStart = minOf(createdDate, earliestCompletion, today)
        val evalStart = if (ChronoUnit.DAYS.between(rawEvalStart, today) > 1095L) {
            today.minusDays(1095)
        } else {
            rawEvalStart
        }

        var completedScheduledDays = 0
        var totalScheduledDays = 0
        var cursor = evalStart
        while (!cursor.isAfter(today)) {
            val isScheduled = CadenceEvaluator.isHabitScheduledOn(habit, cursor)
            val isCompleted = cursor in fullyCompletedDates
            if (isScheduled || isCompleted) {
                totalScheduledDays++
            }
            if (isCompleted) {
                completedScheduledDays++
            }
            cursor = cursor.plusDays(1)
        }

        val completionRatePercent = if (totalScheduledDays > 0) {
            ((completedScheduledDays * 100f) / totalScheduledDays.toFloat())
                .roundToInt()
                .coerceIn(0, 100)
        } else {
            0
        }

        val matrixWeeks = 20
        val currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val completionsByDate = completedInstancesByDate.mapValues { (_, habitMap) ->
            habitMap.filterValues { it.isNotEmpty() }.keys
        }
        val detailMatrix = CadenceEvaluator.buildCadenceMatrix(
            habits = listOf(habit),
            completionsByDate = completionsByDate,
            today = today,
            weeks = matrixWeeks,
            completedInstancesByDate = completedInstancesByDate,
        )
        val matrixColumns = detailMatrix.columns.map { weekColumn ->
            weekColumn.map { cell ->
                HabitDetailMatrixCell(
                    date = cell.date,
                    scheduledCount = cell.scheduledCount,
                    completedCount = cell.completedCount,
                    isFuture = cell.isFuture,
                    isToday = cell.date == today,
                )
            }
        }

        val skipWindowStart = maxOf(evalStart, today.minusDays(29))
        var recentSkips = 0
        if (skipWindowStart.isBefore(today)) {
            var d = skipWindowStart
            while (d.isBefore(today)) {
                if (CadenceEvaluator.isHabitScheduledOn(habit, d) && d !in fullyCompletedDates) {
                    recentSkips++
                }
                d = d.plusDays(1)
            }
        }

        val letters = listOf("M", "T", "W", "T", "F", "S", "S")
        val recentWeekDays = (0 until 7).map { offset ->
            val date = currentWeekMonday.plusDays(offset.toLong())
            HabitWeekDayStatus(
                date = date,
                dayLetter = letters[offset],
                dayOfMonth = date.dayOfMonth,
                isToday = date == today,
                isFuture = date.isAfter(today),
                isCompleted = date in fullyCompletedDates,
                isScheduled = CadenceEvaluator.isHabitScheduledOn(habit, date),
            )
        }
        val completedThisWeek = recentWeekDays.count { it.isCompleted }
        val scheduledThisWeek = maxOf(completedThisWeek, recentWeekDays.count { it.isScheduled })

        val cadenceLabelUpper = habit.cadenceType.label.uppercase(Locale.ENGLISH)
        val cadenceBadgeLabel = if (habit.dailyTargetCount > 1) {
            "${habit.dailyTargetCount}X $cadenceLabelUpper"
        } else {
            cadenceLabelUpper
        }

        return HabitDetailUiState(
            isLoading = false,
            habit = habit,
            today = today,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            completionRatePercent = completionRatePercent,
            completedScheduledDays = completedScheduledDays,
            totalScheduledDays = totalScheduledDays,
            matrixWeeks = matrixWeeks,
            matrixColumns = matrixColumns,
            recentSkips = recentSkips,
            cadenceDisciplineLabel = "$cadenceLabelUpper DISCIPLINE",
            cadenceBadgeLabel = cadenceBadgeLabel,
            scheduleShortLabel = CadenceEvaluator.formatHabitScheduleSummary(habit),
            recentWeekDays = recentWeekDays,
            completedThisWeek = completedThisWeek,
            scheduledThisWeek = scheduledThisWeek,
        )
    }

    class Factory(
        private val habitId: Long,
        private val repository: HabitRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HabitDetailViewModel(habitId = habitId, repository = repository) as T
        }
    }
}
