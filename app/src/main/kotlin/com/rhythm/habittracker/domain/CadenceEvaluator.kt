package com.rhythm.habittracker.domain

import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.CustomCadenceArchetype
import com.rhythm.habittracker.domain.model.EndOfMonthBehavior
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.IntervalUnit
import com.rhythm.habittracker.domain.model.MonthlySubMode
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

enum class MatrixIntensity {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}

data class MatrixCell(
    val date: LocalDate,
    val intensity: MatrixIntensity,
    val isFuture: Boolean,
    val scheduledCount: Int = 0,
    val completedCount: Int = 0,
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

data class MatrixMonthLabel(
    val weekColumnIndex: Int,
    val label: String,
)

data class CadenceMatrixData(
    val columns: List<List<MatrixCell>>,
    val monthLabels: List<MatrixMonthLabel>,
    val consistencyPercent: Int,
    val overallStreakDays: Int,
    val completionsThisMonth: Int,
)

data class WeekHorizonDay(
    val date: LocalDate,
    val dayLetter: String,
    val dayOfMonth: Int,
    val isSelected: Boolean,
    val isToday: Boolean,
    val dotState: WeekDayDotState,
)

enum class WeekDayDotState {
    ACTIVE_COMPLETED,
    PENDING_OR_FUTURE,
}

object CadenceEvaluator {

    fun isHabitScheduledOn(habit: Habit, date: LocalDate): Boolean {
        if (date in habit.rescheduledFromDates) return false
        if (date in habit.rescheduledToDates) return true
        return isBaseCadenceScheduledOn(habit, date)
    }

    /**
     * Removes redundant, out-of-cadence, or overlapping reschedule overrides so
     * [Habit.rescheduledFromDates] and [Habit.rescheduledToDates] remain mutually consistent.
     */
    fun sanitizeRescheduleOverrides(habit: Habit): Habit {
        val isFlexiblePool = habit.cadenceType == CadenceType.WEEKLY &&
            habit.weeklyTargetMode == WeeklyTargetMode.FLEXIBLE_POOL
        val validFrom = if (isFlexiblePool) {
            habit.rescheduledFromDates
        } else {
            habit.rescheduledFromDates.filterTo(mutableSetOf()) { isBaseCadenceScheduledOn(habit, it) }
        }
        val validTo = habit.rescheduledToDates.filterNotTo(mutableSetOf()) {
            isBaseCadenceScheduledOn(habit, it)
        }
        val overlap = validFrom intersect validTo
        return if (overlap.isNotEmpty() || validFrom != habit.rescheduledFromDates || validTo != habit.rescheduledToDates) {
            habit.copy(
                rescheduledFromDates = validFrom - overlap,
                rescheduledToDates = validTo - overlap,
            )
        } else {
            habit
        }
    }

    /**
     * Returns true when any cadence-defining property differs between [old] and [new].
     */
    fun hasCadenceChanged(old: Habit, new: Habit): Boolean {
        return old.cadenceType != new.cadenceType ||
            old.scheduledDays != new.scheduledDays ||
            old.weeklyTargetMode != new.weeklyTargetMode ||
            old.monthlySubMode != new.monthlySubMode ||
            old.monthlyDates != new.monthlyDates ||
            old.ordinalWeek != new.ordinalWeek ||
            old.ordinalDayOfWeek != new.ordinalDayOfWeek ||
            old.endOfMonthBehavior != new.endOfMonthBehavior ||
            old.customArchetype != new.customArchetype ||
            old.customInterval != new.customInterval ||
            old.customIntervalUnit != new.customIntervalUnit ||
            old.cycleDaysOn != new.cycleDaysOn ||
            old.cycleDaysOff != new.cycleDaysOff ||
            old.anchorDate != new.anchorDate
    }

    /**
     * Shifts a scheduled occurrence of [habit] from [fromDate] to [toDate].
     * Returns null when [fromDate] == [toDate], [fromDate] is not currently scheduled/due,
     * or [toDate] already has a scheduled instance.
     */
    fun rescheduleHabitDate(habit: Habit, fromDate: LocalDate, toDate: LocalDate): Habit? {
        val sanitized = sanitizeRescheduleOverrides(habit)
        val isFlexiblePool = sanitized.cadenceType == CadenceType.WEEKLY &&
            sanitized.weeklyTargetMode == WeeklyTargetMode.FLEXIBLE_POOL
        val isSourceValid = if (isFlexiblePool) {
            fromDate !in sanitized.rescheduledFromDates
        } else {
            isHabitScheduledOn(sanitized, fromDate)
        }
        if (fromDate == toDate || !isSourceValid || isHabitScheduledOn(sanitized, toDate)) {
            return null
        }

        val updatedFromDates = sanitized.rescheduledFromDates.toMutableSet()
        val updatedToDates = sanitized.rescheduledToDates.toMutableSet()

        updatedToDates.remove(fromDate)
        if (isBaseCadenceScheduledOn(sanitized, fromDate) || isFlexiblePool) {
            updatedFromDates.add(fromDate)
        } else {
            updatedFromDates.remove(fromDate)
        }

        updatedFromDates.remove(toDate)
        if (!isBaseCadenceScheduledOn(sanitized, toDate)) {
            updatedToDates.add(toDate)
        } else {
            updatedToDates.remove(toDate)
        }

        return sanitizeRescheduleOverrides(
            sanitized.copy(
                rescheduledFromDates = updatedFromDates,
                rescheduledToDates = updatedToDates,
            ),
        )
    }

    fun isBaseCadenceScheduledOn(habit: Habit, date: LocalDate): Boolean {
        return when (habit.cadenceType) {
            CadenceType.DAILY -> date.dayOfWeek in habit.scheduledDays

            CadenceType.WEEKLY -> date.dayOfWeek in habit.scheduledDays

            CadenceType.MONTHLY -> when (habit.monthlySubMode) {
                MonthlySubMode.SPECIFIC_DATES -> {
                    val day = date.dayOfMonth
                    val lastDayOfMonth = date.lengthOfMonth()
                    if (day in habit.monthlyDates) {
                        true
                    } else if (
                        day == lastDayOfMonth &&
                        habit.endOfMonthBehavior == EndOfMonthBehavior.EXECUTE_ON_LAST_DAY &&
                        habit.monthlyDates.any { it > lastDayOfMonth }
                    ) {
                        true
                    } else {
                        false
                    }
                }

                MonthlySubMode.ORDINAL_DAYS -> {
                    if (date.dayOfWeek != habit.ordinalDayOfWeek) {
                        false
                    } else if (habit.ordinalWeek == -1) {
                        date.plusWeeks(1).month != date.month
                    } else {
                        val weekOfMonth = ((date.dayOfMonth - 1) / 7) + 1
                        weekOfMonth == habit.ordinalWeek
                    }
                }
            }

            CadenceType.CUSTOM -> when (habit.customArchetype) {
                CustomCadenceArchetype.INTERVAL -> {
                    val interval = habit.customInterval.coerceAtLeast(1)
                    val anchor = habit.anchorDate
                    if (date.isBefore(anchor)) {
                        false
                    } else {
                        when (habit.customIntervalUnit) {
                            IntervalUnit.DAYS -> ChronoUnit.DAYS.between(anchor, date) % interval == 0L
                            IntervalUnit.WEEKS -> ChronoUnit.DAYS.between(anchor, date) % (interval * 7L) == 0L
                            IntervalUnit.MONTHS -> {
                                val monthsBetween = ChronoUnit.MONTHS.between(
                                    anchor.withDayOfMonth(1),
                                    date.withDayOfMonth(1),
                                )
                                val targetDay = minOf(anchor.dayOfMonth, date.lengthOfMonth())
                                monthsBetween % interval == 0L && date.dayOfMonth == targetDay
                            }
                        }
                    }
                }

                CustomCadenceArchetype.WEEKLY_FIXED ->
                    !date.isBefore(habit.anchorDate) && date.dayOfWeek in habit.scheduledDays

                CustomCadenceArchetype.CYCLE_ON_OFF -> {
                    val anchor = habit.anchorDate
                    val onDays = habit.cycleDaysOn.coerceAtLeast(1)
                    val offDays = habit.cycleDaysOff.coerceAtLeast(0)
                    val cycleLen = onDays + offDays
                    val daysBetween = ChronoUnit.DAYS.between(anchor, date)
                    if (daysBetween < 0) false else (daysBetween % cycleLen) < onDays
                }
            }
        }
    }

    fun isHabitDueOn(
        habit: Habit,
        date: LocalDate,
        completedDates: Set<LocalDate>,
    ): Boolean {
        if (date in habit.rescheduledFromDates) return false
        if (date in habit.rescheduledToDates) return true
        if (date in completedDates) return true
        if (habit.cadenceType == CadenceType.WEEKLY && habit.weeklyTargetMode == WeeklyTargetMode.FLEXIBLE_POOL) {
            val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = weekStart.plusDays(6)
            val completionsThisWeek = completedDates.count { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }
            val weeklyTarget = habit.scheduledDays.size.coerceAtLeast(1)
            return completionsThisWeek < weeklyTarget
        }
        return isHabitScheduledOn(habit, date)
    }

    fun calculateStreak(
        habit: Habit,
        completedDates: Set<LocalDate>,
        referenceDate: LocalDate,
    ): Int {
        if (completedDates.isEmpty()) return 0

        if (habit.cadenceType == CadenceType.WEEKLY && habit.weeklyTargetMode == WeeklyTargetMode.FLEXIBLE_POOL) {
            return calculateFlexibleWeeklyStreak(habit, completedDates, referenceDate)
        }

        val startCursor = if (referenceDate in completedDates) {
            referenceDate
        } else {
            referenceDate.minusDays(1)
        }

        var streak = 0
        var cursor = startCursor
        var scanned = 0
        val minCreatedDate = LocalDate.ofEpochDay(habit.createdAtEpochDay).minusYears(1)

        while (scanned < 1095 && !cursor.isBefore(minCreatedDate)) {
            if (cursor in completedDates) {
                streak++
            } else if (isHabitScheduledOn(habit, cursor)) {
                break
            }
            cursor = cursor.minusDays(1)
            scanned++
        }
        return streak
    }

    private fun calculateFlexibleWeeklyStreak(
        habit: Habit,
        completedDates: Set<LocalDate>,
        referenceDate: LocalDate,
    ): Int {
        val target = habit.scheduledDays.size.coerceAtLeast(1)
        var totalCompleted = 0
        var weekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        // Count current week completions first
        val currentWeekCount = completedDates.count {
            !it.isBefore(weekStart) && !it.isAfter(weekStart.plusDays(6)) && !it.isAfter(referenceDate)
        }
        totalCompleted += currentWeekCount

        // Walk previous weeks while target was met
        var prevWeekStart = weekStart.minusWeeks(1)
        var weeksChecked = 0
        while (weeksChecked < 104) {
            val weekEnd = prevWeekStart.plusDays(6)
            val count = completedDates.count { !it.isBefore(prevWeekStart) && !it.isAfter(weekEnd) }
            if (count >= target) {
                totalCompleted += count
                prevWeekStart = prevWeekStart.minusWeeks(1)
                weeksChecked++
            } else {
                break
            }
        }
        return totalCompleted
    }

    fun calculateBestStreak(
        habit: Habit,
        completedDates: Set<LocalDate>,
        referenceDate: LocalDate,
    ): Int {
        if (completedDates.isEmpty()) return 0
        val currentStreak = calculateStreak(habit, completedDates, referenceDate)

        if (habit.cadenceType == CadenceType.WEEKLY && habit.weeklyTargetMode == WeeklyTargetMode.FLEXIBLE_POOL) {
            val earliestCompletion = completedDates.minOrNull() ?: referenceDate
            val startDate = minOf(LocalDate.ofEpochDay(habit.createdAtEpochDay), earliestCompletion)
            var weekCursor = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val endWeekMonday = maxOf(referenceDate, completedDates.maxOrNull() ?: referenceDate)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val target = habit.scheduledDays.size.coerceAtLeast(1)
            var running = 0
            var best = 0
            var weeksGuard = 0
            while (!weekCursor.isAfter(endWeekMonday) && weeksGuard < 260) {
                val weekEnd = weekCursor.plusDays(6)
                val count = completedDates.count { !it.isBefore(weekCursor) && !it.isAfter(weekEnd) }
                if (!weekCursor.isBefore(endWeekMonday)) {
                    if (count > 0) {
                        running += count
                        best = maxOf(best, running)
                    }
                } else if (count >= target) {
                    running += count
                    best = maxOf(best, running)
                } else {
                    running = 0
                }
                weekCursor = weekCursor.plusWeeks(1)
                weeksGuard++
            }
            return maxOf(best, currentStreak)
        }

        val earliestCompletion = completedDates.minOrNull() ?: referenceDate
        val rawStart = minOf(LocalDate.ofEpochDay(habit.createdAtEpochDay), earliestCompletion)
        val endDate = maxOf(referenceDate, completedDates.maxOrNull() ?: referenceDate)
        val startDate = if (ChronoUnit.DAYS.between(rawStart, endDate) > 1095L) {
            endDate.minusDays(1095)
        } else {
            rawStart
        }

        var cursor = startDate
        var running = 0
        var best = 0
        while (!cursor.isAfter(endDate)) {
            if (cursor in completedDates) {
                running++
                best = maxOf(best, running)
            } else if (isHabitScheduledOn(habit, cursor) && cursor != referenceDate) {
                running = 0
            }
            cursor = cursor.plusDays(1)
        }
        return maxOf(best, currentStreak)
    }

    fun formatHabitScheduleSummary(habit: Habit): String {
        val primaryTime = habit.targetTimes.minByOrNull { it.minuteOfDay }?.formatted() ?: "08:00 AM"
        return when (habit.cadenceType) {
            CadenceType.DAILY -> when {
                habit.dailyTargetCount > 1 -> "DAILY · ${habit.dailyTargetCount} CUES · $primaryTime"
                habit.scheduledDays.size in 1..6 -> "DAILY · ${habit.scheduledDays.size} DAYS/WK · $primaryTime"
                else -> "DAILY · $primaryTime"
            }

            CadenceType.WEEKLY -> {
                if (habit.weeklyTargetMode == WeeklyTargetMode.FLEXIBLE_POOL) {
                    val target = habit.scheduledDays.size.coerceAtLeast(1)
                    "WEEKLY · ${target}X POOL · $primaryTime"
                } else {
                    val days = habit.scheduledDays.sortedBy { it.value }
                    val dayText = when (days.size) {
                        0 -> "UNSCHEDULED"
                        1 -> days.first().getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)
                        else -> days.joinToString(", ") {
                            it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(Locale.ENGLISH)
                        }
                    }
                    "WEEKLY · $dayText · $primaryTime"
                }
            }

            CadenceType.MONTHLY -> when (habit.monthlySubMode) {
                MonthlySubMode.SPECIFIC_DATES -> {
                    val sortedDates = habit.monthlyDates.sorted()
                    val preview = sortedDates.take(3).joinToString(", ") {
                        formatOrdinal(it).uppercase(Locale.ENGLISH)
                    }
                    val suffix = if (sortedDates.size > 3) " +${sortedDates.size - 3}" else ""
                    "MONTHLY · ${preview.ifEmpty { "1ST" }}$suffix · $primaryTime"
                }

                MonthlySubMode.ORDINAL_DAYS -> {
                    val ordLabel = if (habit.ordinalWeek == -1) {
                        "LAST"
                    } else {
                        formatOrdinal(habit.ordinalWeek).uppercase(Locale.ENGLISH)
                    }
                    val dayShort = habit.ordinalDayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        .uppercase(Locale.ENGLISH)
                    "MONTHLY · $ordLabel $dayShort · $primaryTime"
                }
            }

            CadenceType.CUSTOM -> when (habit.customArchetype) {
                CustomCadenceArchetype.INTERVAL ->
                    "CUSTOM · EVERY ${habit.customInterval} ${habit.customIntervalUnit.label.uppercase(Locale.ENGLISH)}"

                CustomCadenceArchetype.WEEKLY_FIXED ->
                    "CUSTOM · ${habit.scheduledDays.size} DAYS/WK · $primaryTime"

                CustomCadenceArchetype.CYCLE_ON_OFF ->
                    "CUSTOM · ${habit.cycleDaysOn}D ON / ${habit.cycleDaysOff}D OFF"
            }
        }
    }

    fun buildCadenceMatrix(
        habits: List<Habit>,
        completionsByDate: Map<LocalDate, Set<Long>>,
        today: LocalDate,
        weeks: Int = 14,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>> = emptyMap(),
    ): CadenceMatrixData {
        val currentWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val firstMonday = currentWeekMonday.minusWeeks((weeks - 1).toLong())

        var scheduledInstances = 0
        var completedInstances = 0
        var completionsThisMonth = 0
        val monthLabels = mutableListOf<MatrixMonthLabel>()
        var lastMonthAdded = -1

        val habitIds = habits.mapTo(mutableSetOf()) { it.id }

        val columns = (0 until weeks).map { weekIndex ->
            val weekMonday = firstMonday.plusWeeks(weekIndex.toLong())
            val monthValue = weekMonday.monthValue
            if (monthValue != lastMonthAdded) {
                monthLabels.add(
                    MatrixMonthLabel(
                        weekColumnIndex = weekIndex,
                        label = weekMonday.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    )
                )
                lastMonthAdded = monthValue
            }

            (0 until 7).map { dayOffset ->
                val date = weekMonday.plusDays(dayOffset.toLong())
                val isFuture = date.isAfter(today)
                val scheduledHabits = habits.filter { isHabitScheduledOn(it, date) }
                val totalScheduled = scheduledHabits.sumOf { it.dailyTargetCount }
                if (isFuture) {
                    MatrixCell(
                        date = date,
                        intensity = MatrixIntensity.NONE,
                        isFuture = true,
                        scheduledCount = totalScheduled,
                        completedCount = 0,
                    )
                } else {
                    val completedIds = completionsByDate[date].orEmpty()
                    val instancesOnDate = completedInstancesByDate[date]
                    val completedScheduled = scheduledHabits.sumOf { habit ->
                        if (instancesOnDate != null) {
                            instancesOnDate[habit.id].orEmpty().size.coerceAtMost(habit.dailyTargetCount)
                        } else if (habit.id in completedIds) {
                            1
                        } else {
                            0
                        }
                    }

                    if (totalScheduled > 0) {
                        scheduledInstances += totalScheduled
                        completedInstances += completedScheduled
                    }
                    if (date.year == today.year && date.month == today.month) {
                        completionsThisMonth += if (instancesOnDate != null) {
                            instancesOnDate.values.sumOf { it.size }
                        } else {
                            completedIds.size
                        }
                    }

                    val effectiveCompleted = if (totalScheduled > 0) {
                        completedScheduled
                    } else if (instancesOnDate != null) {
                        instancesOnDate.filterKeys { it in habitIds }.values.sumOf { it.size }
                    } else {
                        completedIds.count { it in habitIds }
                    }

                    val ratio = if (totalScheduled > 0) {
                        (effectiveCompleted.toFloat() / totalScheduled.toFloat()).coerceIn(0f, 1f)
                    } else if (effectiveCompleted > 0) {
                        1f
                    } else {
                        0f
                    }

                    val intensity = when {
                        ratio <= 0f -> MatrixIntensity.NONE
                        ratio < 0.5f -> MatrixIntensity.LOW
                        ratio < 1f -> MatrixIntensity.MEDIUM
                        else -> MatrixIntensity.HIGH
                    }
                    MatrixCell(
                        date = date,
                        intensity = intensity,
                        isFuture = false,
                        scheduledCount = totalScheduled,
                        completedCount = effectiveCompleted,
                    )
                }
            }
        }

        val consistencyPercent = if (scheduledInstances > 0) {
            ((completedInstances.toFloat() / scheduledInstances.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        val overallStreak = calculateOverallStreak(habits, completionsByDate, today)

        return CadenceMatrixData(
            columns = columns,
            monthLabels = monthLabels,
            consistencyPercent = consistencyPercent,
            overallStreakDays = overallStreak,
            completionsThisMonth = completionsThisMonth,
        )
    }

    private fun calculateOverallStreak(
        habits: List<Habit>,
        completionsByDate: Map<LocalDate, Set<Long>>,
        today: LocalDate,
    ): Int {
        if (habits.isEmpty()) return 0
        val start = if (completionsByDate[today].orEmpty().isNotEmpty()) today else today.minusDays(1)
        var streak = 0
        var cursor = start
        for (i in 0 until 365) {
            val completedIds = completionsByDate[cursor].orEmpty()
            if (completedIds.isNotEmpty()) {
                streak++
                cursor = cursor.minusDays(1)
            } else {
                val anyScheduled = habits.any { isHabitScheduledOn(it, cursor) }
                if (!anyScheduled) {
                    cursor = cursor.minusDays(1)
                } else {
                    break
                }
            }
        }
        return streak
    }

    fun buildWeeklyHorizon(
        habits: List<Habit>,
        completionsByDate: Map<LocalDate, Set<Long>>,
        selectedDate: LocalDate,
        today: LocalDate,
    ): List<WeekHorizonDay> {
        val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val letters = listOf("M", "T", "W", "T", "F", "S", "S")
        return (0 until 7).map { offset ->
            val date = monday.plusDays(offset.toLong())
            val completedIds = completionsByDate[date].orEmpty()
            val dotState = if (!date.isAfter(today) && completedIds.isNotEmpty()) {
                WeekDayDotState.ACTIVE_COMPLETED
            } else {
                WeekDayDotState.PENDING_OR_FUTURE
            }
            WeekHorizonDay(
                date = date,
                dayLetter = letters[offset],
                dayOfMonth = date.dayOfMonth,
                isSelected = date == selectedDate,
                isToday = date == today,
                dotState = dotState,
            )
        }
    }

    fun formatOrdinal(number: Int): String {
        if (number in 11..13) return "${number}th"
        return when (number % 10) {
            1 -> "${number}st"
            2 -> "${number}nd"
            3 -> "${number}rd"
            else -> "${number}th"
        }
    }
}
