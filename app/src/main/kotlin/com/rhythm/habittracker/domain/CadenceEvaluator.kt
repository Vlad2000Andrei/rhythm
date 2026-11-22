package com.rhythm.habittracker.domain

import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.CustomCadenceArchetype
import com.rhythm.habittracker.domain.model.EndOfMonthBehavior
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitEditScope
import com.rhythm.habittracker.domain.model.HabitPauseWindow
import com.rhythm.habittracker.domain.model.HabitSchedule
import com.rhythm.habittracker.domain.model.IntervalUnit
import com.rhythm.habittracker.domain.model.MonthlySubMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.floor
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
    val isPaused: Boolean = false,
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

    val isPausedEmpty: Boolean
        get() = isPaused && !isScheduled && (isFuture || completedCount <= 0)

    val isUnscheduledEmpty: Boolean
        get() = !isPausedEmpty && !isScheduled && (isFuture || completedCount <= 0)
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

    private data class WeekModeRun(
        val isFlex: Boolean,
        val days: List<LocalDate>,
    )

    fun isHabitScheduledOn(habit: Habit, date: LocalDate): Boolean {
        if (habit.isPausedOn(date)) return false
        if (date in habit.rescheduledFromDates) return false
        if (date in habit.rescheduledToDates) return true
        return isBaseCadenceScheduledOn(habit.scheduleForDate(date), date)
    }

    /**
     * Removes redundant, out-of-cadence, or overlapping reschedule overrides so
     * [Habit.rescheduledFromDates] and [Habit.rescheduledToDates] remain mutually consistent
     * across effective-dated schedules.
     */
    fun sanitizeRescheduleOverrides(habit: Habit): Habit {
        val validFrom = habit.rescheduledFromDates.filterTo(mutableSetOf()) { date ->
            val schedule = habit.scheduleForDate(date)
            schedule.isFlexibleWeeklyPool || isBaseCadenceScheduledOn(schedule, date)
        }
        val validTo = habit.rescheduledToDates.filterNotTo(mutableSetOf()) { date ->
            isBaseCadenceScheduledOn(habit.scheduleForDate(date), date)
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
     * Returns true when any cadence-defining property active for the current cadence mode differs
     * between [old] and [new]. Ignores fields belonging to inactive tabs/sub-modes.
     */
    fun hasCadenceChanged(old: HabitSchedule, new: HabitSchedule): Boolean {
        if (old.cadenceType != new.cadenceType) return true
        return when (old.cadenceType) {
            CadenceType.DAILY ->
                old.scheduledDays.ifEmpty { DayOfWeek.entries.toSet() } !=
                    new.scheduledDays.ifEmpty { DayOfWeek.entries.toSet() }

            CadenceType.WEEKLY ->
                old.scheduledDays.ifEmpty { DayOfWeek.entries.toSet() } !=
                    new.scheduledDays.ifEmpty { DayOfWeek.entries.toSet() } ||
                    old.weeklyTargetMode != new.weeklyTargetMode

            CadenceType.MONTHLY -> {
                if (old.monthlySubMode != new.monthlySubMode) return true
                when (old.monthlySubMode) {
                    MonthlySubMode.SPECIFIC_DATES ->
                        old.monthlyDates.ifEmpty { setOf(1) } != new.monthlyDates.ifEmpty { setOf(1) } ||
                            old.endOfMonthBehavior != new.endOfMonthBehavior

                    MonthlySubMode.ORDINAL_DAYS ->
                        old.ordinalWeek != new.ordinalWeek ||
                            old.ordinalDayOfWeek != new.ordinalDayOfWeek
                }
            }

            CadenceType.CUSTOM -> {
                if (old.customArchetype != new.customArchetype || old.anchorDate != new.anchorDate) {
                    return true
                }
                when (old.customArchetype) {
                    CustomCadenceArchetype.INTERVAL ->
                        old.customInterval.coerceAtLeast(1) != new.customInterval.coerceAtLeast(1) ||
                            old.customIntervalUnit != new.customIntervalUnit

                    CustomCadenceArchetype.WEEKLY_FIXED ->
                        old.scheduledDays.ifEmpty { DayOfWeek.entries.toSet() } !=
                            new.scheduledDays.ifEmpty { DayOfWeek.entries.toSet() }

                    CustomCadenceArchetype.CYCLE_ON_OFF ->
                        old.cycleDaysOn.coerceAtLeast(1) != new.cycleDaysOn.coerceAtLeast(1) ||
                            old.cycleDaysOff.coerceAtLeast(0) != new.cycleDaysOff.coerceAtLeast(0)
                }
            }
        }
    }

    /**
     * Returns true when cadence rules, daily target instance count, or any target time
     * (hour, minute) changed—ignoring slot IDs and reminder enabled flags.
     */
    fun hasScheduleOrTimesChanged(old: HabitSchedule, new: HabitSchedule): Boolean {
        if (hasCadenceChanged(old, new)) return true
        if (old.dailyTargetCount != new.dailyTargetCount) return true
        val oldTimes = old.targetTimes.map { it.hour to it.minute }
        val newTimes = new.targetTimes.map { it.hour to it.minute }
        return oldTimes != newTimes
    }

    /**
     * Returns true when any schedule timeline entry or pause window differs between [old] and [new].
     */
    fun hasHabitScheduleOrPauseChanged(old: Habit, new: Habit): Boolean {
        val oldSchedules = old.schedules.sortedWith(HabitSchedule.CHRONOLOGICAL_ORDER)
        val newSchedules = new.schedules.sortedWith(HabitSchedule.CHRONOLOGICAL_ORDER)
        if (oldSchedules.size != newSchedules.size) return true
        for (i in oldSchedules.indices) {
            if (oldSchedules[i].effectiveFrom != newSchedules[i].effectiveFrom ||
                hasScheduleOrTimesChanged(oldSchedules[i], newSchedules[i])
            ) {
                return true
            }
        }
        val oldPauses = HabitPauseWindow.normalize(old.pauseWindows).map { it.startDate to it.endDate }
        val newPauses = HabitPauseWindow.normalize(new.pauseWindows).map { it.startDate to it.endDate }
        return oldPauses != newPauses
    }

    /**
     * Applies a habit edit using either [HabitEditScope.FUTURE_ONLY] (starting at [effectiveFrom])
     * or [HabitEditScope.ALL_OCCURRENCES] (collapsing schedule history to a single baseline).
     */
    fun applyScheduleEdit(
        existing: Habit,
        updatedTitle: String,
        updatedDescription: String,
        updatedDomain: String,
        updatedColorHex: String,
        updatedSkipReminderIfCompleted: Boolean,
        updatedSchedule: HabitSchedule,
        scope: HabitEditScope,
        effectiveFrom: LocalDate,
    ): Habit {
        val normalizedUpdated = updatedSchedule.copy(
            scheduledDays = updatedSchedule.scheduledDays.ifEmpty { DayOfWeek.entries.toSet() },
            monthlyDates = updatedSchedule.monthlyDates.ifEmpty { setOf(1) },
            customInterval = updatedSchedule.customInterval.coerceAtLeast(1),
            cycleDaysOn = updatedSchedule.cycleDaysOn.coerceAtLeast(1),
            cycleDaysOff = updatedSchedule.cycleDaysOff.coerceAtLeast(0),
            timeOfDayBucket = TimeOfDayBucket.fromTimes(updatedSchedule.targetTimes),
        )

        val updatedBase = existing.copy(
            title = updatedTitle.trim(),
            description = updatedDescription.trim(),
            domain = updatedDomain.trim(),
            colorHex = updatedColorHex,
            skipReminderIfCompleted = updatedSkipReminderIfCompleted,
        )

        return when (scope) {
            HabitEditScope.ALL_OCCURRENCES -> {
                val cadenceChanged = existing.schedules.size > 1 ||
                    hasCadenceChanged(existing.latestSchedule, normalizedUpdated)
                val collapsed = updatedBase.copy(
                    schedules = listOf(normalizedUpdated.copy(id = 0L, effectiveFrom = null)),
                    rescheduledFromDates = if (cadenceChanged) emptySet() else existing.rescheduledFromDates,
                    rescheduledToDates = if (cadenceChanged) emptySet() else existing.rescheduledToDates,
                )
                sanitizeRescheduleOverrides(collapsed)
            }

            HabitEditScope.FUTURE_ONLY -> {
                val sortedExisting = existing.schedules.sortedWith(HabitSchedule.CHRONOLOGICAL_ORDER)
                val priorRaw = sortedExisting.filter {
                    it.effectiveFrom == null || it.effectiveFrom.isBefore(effectiveFrom)
                }
                val prior = if (priorRaw.isEmpty()) {
                    listOf(
                        (sortedExisting.firstOrNull() ?: normalizedUpdated)
                            .copy(id = 0L, effectiveFrom = null),
                    )
                } else {
                    priorRaw.mapIndexed { idx, s ->
                        if (idx == 0 && s.effectiveFrom != null) s.copy(effectiveFrom = null) else s
                    }
                }.toMutableList()

                val predecessor = prior.last()
                val nextSchedules = if (!hasScheduleOrTimesChanged(predecessor, normalizedUpdated)) {
                    // Coalesce redundant split while keeping the latest reminder enabled flags
                    prior[prior.lastIndex] = predecessor.copy(
                        targetTimes = normalizedUpdated.targetTimes,
                        timeOfDayBucket = normalizedUpdated.timeOfDayBucket,
                    )
                    prior
                } else {
                    prior + normalizedUpdated.copy(id = 0L, effectiveFrom = effectiveFrom)
                }

                val cadenceAtCutoffChanged = hasCadenceChanged(
                    existing.scheduleForDate(effectiveFrom),
                    normalizedUpdated,
                )
                val updatedFrom = if (cadenceAtCutoffChanged) {
                    existing.rescheduledFromDates.filterTo(mutableSetOf()) { it.isBefore(effectiveFrom) }
                } else {
                    existing.rescheduledFromDates
                }
                val updatedTo = if (cadenceAtCutoffChanged) {
                    existing.rescheduledToDates.filterTo(mutableSetOf()) { it.isBefore(effectiveFrom) }
                } else {
                    existing.rescheduledToDates
                }

                sanitizeRescheduleOverrides(
                    updatedBase.copy(
                        schedules = nextSchedules,
                        rescheduledFromDates = updatedFrom,
                        rescheduledToDates = updatedTo,
                    ),
                )
            }
        }
    }

    /**
     * Applies habit-wide identity and reminder-toggle edits without splitting schedule history
     * or disturbing reschedule overrides.
     */
    fun applyIdentityAndReminderOnlyEdit(
        existing: Habit,
        updatedTitle: String,
        updatedDescription: String,
        updatedDomain: String,
        updatedColorHex: String,
        updatedSkipReminderIfCompleted: Boolean,
        updatedTargetTimes: List<TargetTimeSlot>,
        today: LocalDate,
    ): Habit {
        val sortedSchedules = existing.schedules
            .sortedWith(HabitSchedule.CHRONOLOGICAL_ORDER)
            .ifEmpty { listOf(HabitSchedule()) }
        val latest = sortedSchedules.last()
        val todayScheduleIndex = sortedSchedules.indexOfLast {
            it.effectiveFrom == null || !today.isBefore(it.effectiveFrom)
        }
        val enabledBySlotId = updatedTargetTimes.associate { it.id to it.enabled }

        val updatedSchedules = sortedSchedules.mapIndexed { index, schedule ->
            if (index == sortedSchedules.lastIndex) {
                val mergedSlots = schedule.targetTimes.mapIndexed { slotIdx, slot ->
                    val matchingNew = updatedTargetTimes.getOrNull(slotIdx)
                    val newEnabled = enabledBySlotId[slot.id] ?: matchingNew?.enabled ?: slot.enabled
                    slot.copy(enabled = newEnabled)
                }
                schedule.copy(targetTimes = mergedSlots.ifEmpty { updatedTargetTimes })
            } else if (latest.effectiveFrom?.isAfter(today) == true && index == todayScheduleIndex) {
                val mergedTodaySlots = schedule.targetTimes.map { slot ->
                    val newEnabled = enabledBySlotId[slot.id]
                    if (newEnabled != null) slot.copy(enabled = newEnabled) else slot
                }
                schedule.copy(targetTimes = mergedTodaySlots)
            } else {
                schedule
            }
        }

        return existing.copy(
            title = updatedTitle.trim(),
            description = updatedDescription.trim(),
            domain = updatedDomain.trim(),
            colorHex = updatedColorHex,
            skipReminderIfCompleted = updatedSkipReminderIfCompleted,
            schedules = updatedSchedules,
        )
    }

    fun addOrUpdatePauseWindow(
        existing: List<HabitPauseWindow>,
        startDate: LocalDate,
        endDate: LocalDate,
        editingWindowId: Long? = null,
    ): List<HabitPauseWindow> {
        require(!endDate.isBefore(startDate)) { "Pause end date cannot precede start date" }
        val filtered = if (editingWindowId != null && editingWindowId != 0L) {
            existing.filterNot { it.id == editingWindowId }
        } else {
            existing
        }
        return HabitPauseWindow.normalize(
            filtered + HabitPauseWindow(
                id = 0L,
                startDate = startDate,
                endDate = endDate,
            ),
        )
    }

    /**
     * Ends an active pause starting today, truncates an earlier-started active pause at yesterday
     * so past paused days remain protected, or cancels the next upcoming scheduled pause.
     */
    fun resumeOrCancelPause(
        existing: List<HabitPauseWindow>,
        today: LocalDate,
    ): List<HabitPauseWindow> {
        val normalized = HabitPauseWindow.normalize(existing)
        val active = normalized.firstOrNull { it.contains(today) }
        if (active != null) {
            val updated = normalized.mapNotNull { window ->
                if (window != active) {
                    window
                } else if (window.startDate.isBefore(today)) {
                    window.copy(endDate = today.minusDays(1))
                } else {
                    null
                }
            }
            return HabitPauseWindow.normalize(updated)
        }

        val upcoming = normalized.filter { it.startDate.isAfter(today) }.minByOrNull { it.startDate }
        return if (upcoming != null) {
            HabitPauseWindow.normalize(normalized.filterNot { it == upcoming })
        } else {
            normalized
        }
    }

    /**
     * Shifts a scheduled occurrence of [habit] from [fromDate] to [toDate].
     * Returns null when [fromDate] == [toDate], either date is paused,
     * [fromDate] is not currently scheduled/due, or [toDate] already has a scheduled instance.
     */
    fun rescheduleHabitDate(habit: Habit, fromDate: LocalDate, toDate: LocalDate): Habit? {
        val sanitized = sanitizeRescheduleOverrides(habit)
        if (fromDate == toDate || sanitized.isPausedOn(fromDate) || sanitized.isPausedOn(toDate)) {
            return null
        }
        val fromSchedule = sanitized.scheduleForDate(fromDate)
        val toSchedule = sanitized.scheduleForDate(toDate)
        val isSourceFlexiblePool = fromSchedule.isFlexibleWeeklyPool
        val isSourceValid = if (isSourceFlexiblePool) {
            fromDate !in sanitized.rescheduledFromDates
        } else {
            isHabitScheduledOn(sanitized, fromDate)
        }
        if (!isSourceValid || isHabitScheduledOn(sanitized, toDate)) {
            return null
        }

        val updatedFromDates = sanitized.rescheduledFromDates.toMutableSet()
        val updatedToDates = sanitized.rescheduledToDates.toMutableSet()

        updatedToDates.remove(fromDate)
        if (isBaseCadenceScheduledOn(fromSchedule, fromDate) || isSourceFlexiblePool) {
            updatedFromDates.add(fromDate)
        } else {
            updatedFromDates.remove(fromDate)
        }

        updatedFromDates.remove(toDate)
        if (!isBaseCadenceScheduledOn(toSchedule, toDate)) {
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

    fun isBaseCadenceScheduledOn(habit: Habit, date: LocalDate): Boolean =
        isBaseCadenceScheduledOn(habit.scheduleForDate(date), date)

    fun isBaseCadenceScheduledOn(schedule: HabitSchedule, date: LocalDate): Boolean {
        return when (schedule.cadenceType) {
            CadenceType.DAILY -> date.dayOfWeek in schedule.scheduledDays

            CadenceType.WEEKLY -> date.dayOfWeek in schedule.scheduledDays

            CadenceType.MONTHLY -> when (schedule.monthlySubMode) {
                MonthlySubMode.SPECIFIC_DATES -> {
                    val day = date.dayOfMonth
                    val lastDayOfMonth = date.lengthOfMonth()
                    if (day in schedule.monthlyDates) {
                        true
                    } else if (
                        day == lastDayOfMonth &&
                        schedule.endOfMonthBehavior == EndOfMonthBehavior.EXECUTE_ON_LAST_DAY &&
                        schedule.monthlyDates.any { it > lastDayOfMonth }
                    ) {
                        true
                    } else {
                        false
                    }
                }

                MonthlySubMode.ORDINAL_DAYS -> {
                    if (date.dayOfWeek != schedule.ordinalDayOfWeek) {
                        false
                    } else if (schedule.ordinalWeek == -1) {
                        date.plusWeeks(1).month != date.month
                    } else {
                        val weekOfMonth = ((date.dayOfMonth - 1) / 7) + 1
                        weekOfMonth == schedule.ordinalWeek
                    }
                }
            }

            CadenceType.CUSTOM -> when (schedule.customArchetype) {
                CustomCadenceArchetype.INTERVAL -> {
                    val interval = schedule.customInterval.coerceAtLeast(1)
                    val anchor = schedule.anchorDate
                    if (date.isBefore(anchor)) {
                        false
                    } else {
                        when (schedule.customIntervalUnit) {
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
                    !date.isBefore(schedule.anchorDate) && date.dayOfWeek in schedule.scheduledDays

                CustomCadenceArchetype.CYCLE_ON_OFF -> {
                    val anchor = schedule.anchorDate
                    val onDays = schedule.cycleDaysOn.coerceAtLeast(1)
                    val offDays = schedule.cycleDaysOff.coerceAtLeast(0)
                    val cycleLen = onDays + offDays
                    val daysBetween = ChronoUnit.DAYS.between(anchor, date)
                    if (daysBetween < 0) false else (daysBetween % cycleLen) < onDays
                }
            }
        }
    }

    /**
     * Computes the effective Flexible Pool weekly due target for the week starting at [weekStart],
     * prorated by active (non-paused) days in that week.
     */
    fun flexiblePoolDueTargetForWeek(habit: Habit, weekStart: LocalDate, date: LocalDate): Int {
        val baseTarget = habit.scheduleForDate(date).scheduledDays.size.coerceAtLeast(1)
        val pausedDays = (0L..6L).count { habit.isPausedOn(weekStart.plusDays(it)) }
        val activeDays = (7 - pausedDays).coerceIn(0, 7)
        return when {
            activeDays == 0 -> 0
            pausedDays == 0 -> baseTarget
            else -> ((baseTarget * activeDays) / 7.0)
                .roundToInt()
                .coerceIn(1, minOf(baseTarget, activeDays))
        }
    }

    fun isHabitDueOn(
        habit: Habit,
        date: LocalDate,
        completedDates: Set<LocalDate>,
    ): Boolean {
        if (date in habit.rescheduledFromDates) return false
        if (date in completedDates) return true
        if (habit.isPausedOn(date)) return false
        if (date in habit.rescheduledToDates) return true
        val schedule = habit.scheduleForDate(date)
        if (schedule.isFlexibleWeeklyPool) {
            val weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val weekEnd = weekStart.plusDays(6)
            val completionsThisWeek = completedDates.count { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }
            return completionsThisWeek < flexiblePoolDueTargetForWeek(habit, weekStart, date)
        }
        return isBaseCadenceScheduledOn(schedule, date)
    }

    fun calculateStreak(
        habit: Habit,
        completedDates: Set<LocalDate>,
        referenceDate: LocalDate,
    ): Int {
        if (completedDates.isEmpty()) return 0

        val earliestCompletion = completedDates.minOrNull() ?: return 0
        val minBound = maxOf(
            earliestCompletion.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            LocalDate.ofEpochDay(habit.createdAtEpochDay).minusYears(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
        )
        val refWeekMonday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        var streak = 0
        var weekStart = refWeekMonday
        var weeksScanned = 0

        while (weeksScanned < 157 && !weekStart.isBefore(minBound)) {
            val weekEnd = minOf(weekStart.plusDays(6), referenceDate)
            val runs = partitionWeekRuns(habit, weekStart, weekEnd)
            for (runIdx in runs.indices.reversed()) {
                val run = runs[runIdx]
                if (run.isFlex) {
                    val runCompletions = run.days.count { it in completedDates }
                    val isTrailingCurrentWeek =
                        weekStart == refWeekMonday && run.days.lastOrNull() == referenceDate
                    if (isTrailingCurrentWeek || flexRunPasses(habit, weekStart, run.days, completedDates, runCompletions)) {
                        streak += runCompletions
                    } else {
                        return streak
                    }
                } else {
                    for (dayIdx in run.days.indices.reversed()) {
                        val cursor = run.days[dayIdx]
                        if (cursor == referenceDate && cursor !in completedDates) {
                            continue
                        }
                        if (cursor in completedDates) {
                            streak++
                        } else if (isHabitScheduledOn(habit, cursor)) {
                            return streak
                        }
                    }
                }
            }
            weekStart = weekStart.minusWeeks(1)
            weeksScanned++
        }
        return streak
    }

    fun calculateBestStreak(
        habit: Habit,
        completedDates: Set<LocalDate>,
        referenceDate: LocalDate,
    ): Int {
        if (completedDates.isEmpty()) return 0
        val currentStreak = calculateStreak(habit, completedDates, referenceDate)

        val earliestCompletion = completedDates.minOrNull() ?: referenceDate
        val rawStart = minOf(LocalDate.ofEpochDay(habit.createdAtEpochDay), earliestCompletion)
        val endDate = maxOf(referenceDate, completedDates.maxOrNull() ?: referenceDate)
        val startDate = if (ChronoUnit.DAYS.between(rawStart, endDate) > 1095L) {
            endDate.minusDays(1095)
        } else {
            rawStart
        }

        val endWeekMonday = endDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var weekStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        var running = 0
        var best = 0
        var weeksGuard = 0

        while (!weekStart.isAfter(endWeekMonday) && weeksGuard < 260) {
            val clippedStart = maxOf(weekStart, startDate)
            val clippedEnd = minOf(weekStart.plusDays(6), endDate)
            val runs = partitionWeekRuns(habit, clippedStart, clippedEnd)

            for (run in runs) {
                if (run.isFlex) {
                    val runCompletions = run.days.count { it in completedDates }
                    val isTrailingEndWeek =
                        weekStart == endWeekMonday && run.days.lastOrNull() == endDate
                    if (isTrailingEndWeek) {
                        if (runCompletions > 0) {
                            running += runCompletions
                            best = maxOf(best, running)
                        }
                    } else if (flexRunPasses(habit, weekStart, run.days, completedDates, runCompletions)) {
                        running += runCompletions
                        best = maxOf(best, running)
                    } else {
                        running = 0
                    }
                } else {
                    for (cursor in run.days) {
                        if (cursor in completedDates) {
                            running++
                            best = maxOf(best, running)
                        } else if (isHabitScheduledOn(habit, cursor) && cursor != referenceDate) {
                            running = 0
                        }
                    }
                }
            }
            weekStart = weekStart.plusWeeks(1)
            weeksGuard++
        }
        return maxOf(best, currentStreak)
    }

    private fun partitionWeekRuns(
        habit: Habit,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
    ): List<WeekModeRun> {
        if (rangeStart.isAfter(rangeEnd)) return emptyList()
        val runs = mutableListOf<WeekModeRun>()
        var currentDays = mutableListOf<LocalDate>()
        var currentIsFlex: Boolean? = null

        var cursor = rangeStart
        while (!cursor.isAfter(rangeEnd)) {
            val isFlex = habit.scheduleForDate(cursor).isFlexibleWeeklyPool
            if (currentIsFlex != null && currentIsFlex != isFlex) {
                runs += WeekModeRun(isFlex = currentIsFlex, days = currentDays)
                currentDays = mutableListOf()
            }
            currentIsFlex = isFlex
            currentDays += cursor
            cursor = cursor.plusDays(1)
        }
        if (currentIsFlex != null && currentDays.isNotEmpty()) {
            runs += WeekModeRun(isFlex = currentIsFlex, days = currentDays)
        }
        return runs
    }

    private fun flexRunPasses(
        habit: Habit,
        weekStart: LocalDate,
        runDays: List<LocalDate>,
        completedDates: Set<LocalDate>,
        runCompletions: Int,
    ): Boolean {
        val unpausedRunDays = runDays.filterNot { habit.isPausedOn(it) }
        if (unpausedRunDays.isEmpty()) return true

        val minFlexTarget = unpausedRunDays.minOf {
            habit.scheduleForDate(it).scheduledDays.size.coerceAtLeast(1)
        }
        val runRequired = if (runDays.size == 7 && unpausedRunDays.size == 7) {
            minFlexTarget
        } else {
            floor((minFlexTarget * unpausedRunDays.size) / 7.0)
                .toInt()
                .coerceAtMost(unpausedRunDays.size)
        }
        if (runCompletions >= runRequired) return true

        val weekEnd = weekStart.plusDays(6)
        val weekUnpausedDays = (0L..6L).count { !habit.isPausedOn(weekStart.plusDays(it)) }
        val weekRequired = if (weekUnpausedDays == 7) {
            minFlexTarget
        } else {
            floor((minFlexTarget * weekUnpausedDays) / 7.0)
                .toInt()
                .coerceAtMost(weekUnpausedDays)
        }
        val totalWeekCompletions = completedDates.count { !it.isBefore(weekStart) && !it.isAfter(weekEnd) }
        return totalWeekCompletions >= weekRequired
    }

    fun formatHabitScheduleSummary(habit: Habit): String =
        formatHabitScheduleSummary(habit.latestSchedule)

    fun formatHabitScheduleSummary(schedule: HabitSchedule): String {
        val primaryTime = schedule.targetTimes.minByOrNull { it.minuteOfDay }?.formatted() ?: "08:00 AM"
        return when (schedule.cadenceType) {
            CadenceType.DAILY -> when {
                schedule.dailyTargetCount > 1 -> "DAILY · ${schedule.dailyTargetCount} CUES · $primaryTime"
                schedule.scheduledDays.size in 1..6 -> "DAILY · ${schedule.scheduledDays.size} DAYS/WK · $primaryTime"
                else -> "DAILY · $primaryTime"
            }

            CadenceType.WEEKLY -> {
                if (schedule.weeklyTargetMode == WeeklyTargetMode.FLEXIBLE_POOL) {
                    val target = schedule.scheduledDays.size.coerceAtLeast(1)
                    "WEEKLY · ${target}X POOL · $primaryTime"
                } else {
                    val days = schedule.scheduledDays.sortedBy { it.value }
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

            CadenceType.MONTHLY -> when (schedule.monthlySubMode) {
                MonthlySubMode.SPECIFIC_DATES -> {
                    val sortedDates = schedule.monthlyDates.sorted()
                    val preview = sortedDates.take(3).joinToString(", ") {
                        formatOrdinal(it).uppercase(Locale.ENGLISH)
                    }
                    val suffix = if (sortedDates.size > 3) " +${sortedDates.size - 3}" else ""
                    "MONTHLY · ${preview.ifEmpty { "1ST" }}$suffix · $primaryTime"
                }

                MonthlySubMode.ORDINAL_DAYS -> {
                    val ordLabel = if (schedule.ordinalWeek == -1) {
                        "LAST"
                    } else {
                        formatOrdinal(schedule.ordinalWeek).uppercase(Locale.ENGLISH)
                    }
                    val dayShort = schedule.ordinalDayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                        .uppercase(Locale.ENGLISH)
                    "MONTHLY · $ordLabel $dayShort · $primaryTime"
                }
            }

            CadenceType.CUSTOM -> when (schedule.customArchetype) {
                CustomCadenceArchetype.INTERVAL ->
                    "CUSTOM · EVERY ${schedule.customInterval} ${schedule.customIntervalUnit.label.uppercase(Locale.ENGLISH)}"

                CustomCadenceArchetype.WEEKLY_FIXED ->
                    "CUSTOM · ${schedule.scheduledDays.size} DAYS/WK · $primaryTime"

                CustomCadenceArchetype.CYCLE_ON_OFF ->
                    "CUSTOM · ${schedule.cycleDaysOn}D ON / ${schedule.cycleDaysOff}D OFF"
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
                val allPaused = habits.isNotEmpty() && habits.all { it.isPausedOn(date) }
                val scheduledHabits = habits.filter { isHabitScheduledOn(it, date) }
                val totalScheduled = scheduledHabits.sumOf { it.scheduleForDate(date).dailyTargetCount }
                if (isFuture) {
                    MatrixCell(
                        date = date,
                        intensity = MatrixIntensity.NONE,
                        isFuture = true,
                        scheduledCount = totalScheduled,
                        completedCount = 0,
                        isPaused = allPaused,
                    )
                } else {
                    val completedIds = completionsByDate[date].orEmpty()
                    val instancesOnDate = completedInstancesByDate[date]
                    val completedScheduled = scheduledHabits.sumOf { habit ->
                        val targetCount = habit.scheduleForDate(date).dailyTargetCount
                        if (instancesOnDate != null) {
                            instancesOnDate[habit.id].orEmpty().size.coerceAtMost(targetCount)
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
                            instancesOnDate.filterKeys { it in habitIds }.values.sumOf { it.size }
                        } else {
                            completedIds.count { it in habitIds }
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
                        isPaused = allPaused,
                    )
                }
            }
        }

        val consistencyPercent = if (scheduledInstances > 0) {
            ((completedInstances.toFloat() / scheduledInstances.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        val overallStreak = calculateOverallStreak(habits, completionsByDate, today, habitIds)

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
        habitIds: Set<Long>,
    ): Int {
        if (habits.isEmpty()) return 0
        val todayCompleted = completionsByDate[today].orEmpty().any { it in habitIds }
        val start = if (todayCompleted) today else today.minusDays(1)
        var streak = 0
        var cursor = start
        for (i in 0 until 365) {
            val completedIds = completionsByDate[cursor].orEmpty()
            if (completedIds.any { it in habitIds }) {
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
