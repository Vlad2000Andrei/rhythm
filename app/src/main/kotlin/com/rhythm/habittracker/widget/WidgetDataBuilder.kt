package com.rhythm.habittracker.widget

import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

object WidgetDataBuilder {

    private data class DueInstance(
        val habit: Habit,
        val instanceIndex: Int,
        val totalInstances: Int,
        val isCompleted: Boolean,
        val timeSlot: TargetTimeSlot?,
        val bucket: TimeOfDayBucket,
    )

    fun buildSingleHabitData(
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        configuredHabitId: Long?,
        today: LocalDate = LocalDate.now(),
        maxWeeks: Int = 26,
    ): SingleHabitWidgetData {
        val habit = configuredHabitId?.let { id -> habits.firstOrNull { it.id == id } }
            ?: habits.firstOrNull()
            ?: return SingleHabitWidgetData(
                habit = null,
                today = today,
                domainBadgeLabel = "NO HABIT CONFIGURED",
                subtitleLabel = "Open Rhythm or configure widget to pick a habit",
                statusLabel = "TAP TO CONFIGURE",
            )

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

        val completionsByDate = completedInstancesByDate.mapValues { (_, habitMap) ->
            habitMap.filterValues { it.isNotEmpty() }.keys
        }
        val matrix = CadenceEvaluator.buildCadenceMatrix(
            habits = listOf(habit),
            completionsByDate = completionsByDate,
            today = today,
            weeks = maxWeeks,
            completedInstancesByDate = completedInstancesByDate,
        )

        val primaryTargetLabel = habit.targetTimes
            .minByOrNull { it.minuteOfDay }
            ?.formatted()
            ?: "08:00 AM"

        val subtitleLabel = habit.description.ifBlank {
            CadenceEvaluator.formatHabitScheduleSummary(habit)
        }

        val domainBadgeLabel = habit.badgeLabel
            .uppercase(Locale.ENGLISH)
            .ifBlank { habit.cadenceType.label.uppercase(Locale.ENGLISH) }

        val isScheduledToday = CadenceEvaluator.isHabitScheduledOn(habit, today)
        val completedTodayCount = completedInstancesByDate[today]?.get(habit.id).orEmpty().size
        val isCompletedToday = completedTodayCount >= habit.dailyTargetCount

        val consistencyPercent = if (matrix.columns.flatten().any { !it.isFuture && it.isScheduled }) {
            matrix.consistencyPercent
        } else if (isCompletedToday) {
            100
        } else {
            0
        }

        val statusLabel = when {
            isCompletedToday -> "Completed Today"
            isScheduledToday && habit.dailyTargetCount > 1 ->
                "Progress $completedTodayCount/${habit.dailyTargetCount} · Next $primaryTargetLabel"
            isScheduledToday -> "Scheduled for $primaryTargetLabel"
            else -> "Rest day · Next due at $primaryTargetLabel"
        }

        return SingleHabitWidgetData(
            habit = habit,
            today = today,
            currentStreak = currentStreak,
            consistencyPercent = consistencyPercent,
            primaryTargetLabel = primaryTargetLabel,
            subtitleLabel = subtitleLabel,
            domainBadgeLabel = domainBadgeLabel,
            statusLabel = statusLabel,
            isCompletedToday = isCompletedToday,
            isScheduledToday = isScheduledToday,
            matrixColumns = matrix.columns,
        )
    }

    fun buildOverallMatrixData(
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        today: LocalDate = LocalDate.now(),
        maxWeeks: Int = 26,
    ): OverallMatrixWidgetData {
        val dueInstances = expandDueInstancesForDate(habits, completedInstancesByDate, today)

        val morning = dueInstances.filter { it.bucket == TimeOfDayBucket.MORNING }
        val afternoon = dueInstances.filter { it.bucket == TimeOfDayBucket.DURING_THE_DAY }
        val evening = dueInstances.filter { it.bucket == TimeOfDayBucket.EVENING }

        val completionsByDate = completedInstancesByDate.mapValues { (_, habitMap) ->
            habitMap.filterValues { it.isNotEmpty() }.keys
        }
        val matrix = CadenceEvaluator.buildCadenceMatrix(
            habits = habits,
            completionsByDate = completionsByDate,
            today = today,
            weeks = maxWeeks,
            completedInstancesByDate = completedInstancesByDate,
        )

        val dateShortFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
        val dateLongFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH)
        val weekNum = today.get(WeekFields.ISO.weekOfWeekBasedYear())

        val pendingSorted = dueInstances
            .filter { !it.isCompleted }
            .sortedWith(
                compareBy<DueInstance> { it.timeSlot?.minuteOfDay ?: Int.MAX_VALUE }
                    .thenBy { it.habit.sortOrder }
                    .thenBy { it.instanceIndex },
            )
        val nextPending = pendingSorted.firstOrNull()
        val nextTaskLabel = when {
            habits.isEmpty() -> "ADD YOUR FIRST HABIT"
            nextPending != null -> {
                val timeText = nextPending.timeSlot?.formatted() ?: "TODAY"
                val titleText = nextPending.habit.title.uppercase(Locale.ENGLISH)
                "$timeText · $titleText"
            }
            dueInstances.isNotEmpty() -> "ALL ${dueInstances.size} COMPLETED TODAY"
            else -> "REST DAY · NO TASKS SCHEDULED"
        }

        return OverallMatrixWidgetData(
            today = today,
            dateShortLabel = today.format(dateShortFormatter).uppercase(Locale.ENGLISH),
            dateLongLabel = today.format(dateLongFormatter).uppercase(Locale.ENGLISH),
            weekShortLabel = "W$weekNum",
            consistencyPercent = matrix.consistencyPercent,
            overallStreakDays = matrix.overallStreakDays,
            completionsThisMonth = matrix.completionsThisMonth,
            morningCompleted = morning.count { it.isCompleted },
            morningTotal = morning.size,
            afternoonCompleted = afternoon.count { it.isCompleted },
            afternoonTotal = afternoon.size,
            eveningCompleted = evening.count { it.isCompleted },
            eveningTotal = evening.size,
            nextTaskLabel = nextTaskLabel,
            hasPendingTasksToday = nextPending != null,
            matrixColumns = matrix.columns,
            monthLabels = matrix.monthLabels,
        )
    }

    fun buildUpcomingTasksData(
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        today: LocalDate = LocalDate.now(),
    ): UpcomingTasksWidgetData {
        val dueInstances = expandDueInstancesForDate(habits, completedInstancesByDate, today)
        val comparator = compareBy<DueInstance> { it.timeSlot?.minuteOfDay ?: Int.MAX_VALUE }
            .thenBy { it.habit.sortOrder }
            .thenBy { it.instanceIndex }

        val pendingRaw = dueInstances.filter { !it.isCompleted }.sortedWith(comparator)
        val completedRaw = dueInstances.filter { it.isCompleted }.sortedWith(comparator)

        val pendingItems = pendingRaw.mapIndexed { idx, instance ->
            instance.toWidgetTaskItem(isNowTarget = idx == 0)
        }
        val completedItems = completedRaw.map { instance ->
            instance.toWidgetTaskItem(isNowTarget = false)
        }

        val orderedTasks = pendingItems + completedItems
        val completedCount = completedItems.size
        val totalCount = orderedTasks.size
        val completionPercent = if (totalCount > 0) {
            ((completedCount.toFloat() / totalCount.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        } else {
            0
        }

        val segmentColors: List<String?> = buildList {
            completedItems.forEach { add(it.accentColorHex) }
            repeat((totalCount - completedCount).coerceAtLeast(0)) { add(null) }
        }

        val dateShort = today.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH))
            .uppercase(Locale.ENGLISH)

        val nextUpFooterLabel = when {
            habits.isEmpty() -> "TAP TO ADD FIRST HABIT"
            pendingItems.isNotEmpty() -> "NEXT UP: ${pendingItems.first().timeShortLabel}"
            totalCount > 0 -> "ALL TASKS DONE"
            else -> "REST DAY ALIGNED"
        }

        return UpcomingTasksWidgetData(
            today = today,
            dateShortLabel = dateShort,
            dateHeaderLabel = "TODAY · $dateShort",
            completedCount = completedCount,
            totalCount = totalCount,
            completionPercent = completionPercent,
            segmentColorHexes = segmentColors,
            orderedTasks = orderedTasks,
            nextUpFooterLabel = nextUpFooterLabel,
        )
    }

    private fun expandDueInstancesForDate(
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        date: LocalDate,
    ): List<DueInstance> {
        val habitById = habits.associateBy { it.id }
        val fullyCompletedByHabit: Map<Long, Set<LocalDate>> = buildMap<Long, MutableSet<LocalDate>> {
            completedInstancesByDate.forEach { (d, habitMap) ->
                habitMap.forEach { (id, instances) ->
                    val target = habitById[id]?.dailyTargetCount ?: 1
                    if (instances.size >= target) {
                        getOrPut(id) { mutableSetOf() }.add(d)
                    }
                }
            }
        }

        val completedOnDate = completedInstancesByDate[date].orEmpty()
        val dueHabits = habits.filter { habit ->
            val fullyCompletedDates = fullyCompletedByHabit[habit.id].orEmpty()
            val hasAnyOnDate = completedOnDate[habit.id].orEmpty().isNotEmpty()
            val datesForDueCheck = if (hasAnyOnDate) {
                fullyCompletedDates + date
            } else {
                fullyCompletedDates
            }
            CadenceEvaluator.isHabitDueOn(habit, date, datesForDueCheck)
        }

        return dueHabits.flatMap { habit ->
            val completedInstances = completedOnDate[habit.id].orEmpty()
            val totalInstances = maxOf(
                habit.dailyTargetCount,
                (completedInstances.maxOrNull() ?: -1) + 1,
            )
            (0 until totalInstances).map { idx ->
                DueInstance(
                    habit = habit,
                    instanceIndex = idx,
                    totalInstances = totalInstances,
                    isCompleted = idx in completedInstances,
                    timeSlot = habit.timeSlotForInstance(idx),
                    bucket = habit.bucketForInstance(idx),
                )
            }
        }
    }

    private fun DueInstance.toWidgetTaskItem(isNowTarget: Boolean): WidgetTaskItem {
        val timeText = timeSlot?.formatted() ?: "08:00 AM"
        val subtitleText = when {
            isCompleted && totalInstances > 1 ->
                "$timeText · ${instanceIndex + 1}/$totalInstances COMPLETED"
            isCompleted ->
                "$timeText · COMPLETED"
            totalInstances > 1 ->
                "$timeText · SLOT ${instanceIndex + 1}/$totalInstances"
            habit.description.isNotBlank() ->
                "$timeText · ${habit.description.uppercase(Locale.ENGLISH)}"
            habit.domain.isNotBlank() ->
                "$timeText · ${habit.domain.uppercase(Locale.ENGLISH)}"
            else ->
                "$timeText · ${habit.cadenceType.label.uppercase(Locale.ENGLISH)}"
        }

        return WidgetTaskItem(
            habitId = habit.id,
            instanceIndex = instanceIndex,
            totalInstances = totalInstances,
            title = habit.title,
            subtitle = subtitleText,
            timeShortLabel = timeText,
            accentColorHex = habit.colorHex,
            isCompleted = isCompleted,
            isNowTarget = isNowTarget,
        )
    }
}
