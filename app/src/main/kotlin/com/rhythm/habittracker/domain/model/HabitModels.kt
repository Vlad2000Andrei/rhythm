package com.rhythm.habittracker.domain.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class HabitAccentColor(
    val displayName: String,
    val hex: String,
    val colorValue: Long,
) {
    ROYAL_BLUE("Royal Blue", "#2563EB", 0xFF2563EB),
    EMERALD_GREEN("Emerald Green", "#10B981", 0xFF10B981),
    WARM_AMBER("Warm Amber", "#D97706", 0xFFD97706),
    SKY_BLUE("Sky Blue", "#0284C7", 0xFF0284C7),
    ROSE_CORAL("Rose Coral", "#F43F5E", 0xFFF43F5E),
    VIOLET_PURPLE("Violet Purple", "#8B5CF6", 0xFF8B5CF6),
    SLATE_GRAPHITE("Slate Graphite", "#475569", 0xFF475569);

    val color: Color
        get() = Color(colorValue)

    companion object {
        fun fromHex(hex: String): HabitAccentColor =
            entries.firstOrNull { it.hex.equals(hex, ignoreCase = true) } ?: ROYAL_BLUE
    }
}

enum class CadenceType(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom"),
}

enum class WeeklyTargetMode {
    FIXED_SCHEDULE,
    FLEXIBLE_POOL,
}

enum class MonthlySubMode {
    SPECIFIC_DATES,
    ORDINAL_DAYS,
}

enum class EndOfMonthBehavior {
    EXECUTE_ON_LAST_DAY,
    SKIP_OCCURRENCE,
}

enum class CustomCadenceArchetype {
    INTERVAL,
    WEEKLY_FIXED,
    CYCLE_ON_OFF,
}

enum class IntervalUnit(val label: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months"),
}

enum class TimeOfDayBucket(val label: String) {
    MORNING("Morning"),
    DURING_THE_DAY("During the day"),
    EVENING("Evening");

    companion object {
        fun fromSlot(slot: TargetTimeSlot): TimeOfDayBucket = when {
            slot.hour < 12 -> MORNING
            slot.hour < 17 -> DURING_THE_DAY
            else -> EVENING
        }

        fun fromTimes(times: List<TargetTimeSlot>): TimeOfDayBucket {
            val first = times.minByOrNull { it.minuteOfDay } ?: return MORNING
            return fromSlot(first)
        }
    }
}

enum class HabitSortMode(val label: String) {
    FLOW("FLOW"),
    STREAK("STREAK"),
    DOMAIN("DOMAIN"),
}

enum class HabitEditScope {
    FUTURE_ONLY,
    ALL_OCCURRENCES,
}

@Serializable
data class TargetTimeSlot(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
) {
    val minuteOfDay: Int
        get() = hour * 60 + minute

    fun toLocalTime(): LocalTime = LocalTime.of(hour, minute)

    fun formatted(): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%02d:%02d %s".format(displayHour, minute, period)
    }

    /**
     * Matches Stitch mock formatting like `08:00 AM`, `13:30 PM`, `20:00 PM` when 24h style is requested,
     * or clean `08:00 AM` / `01:30 PM` / `08:00 PM`.
     */
    fun formatted24WithMeridiem(): String {
        val period = if (hour < 12) "AM" else "PM"
        return "%02d:%02d %s".format(hour, minute, period)
    }
}

data class HabitSchedule(
    val id: Long = 0L,
    val effectiveFrom: LocalDate? = null,
    val cadenceType: CadenceType = CadenceType.DAILY,
    val scheduledDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val weeklyTargetMode: WeeklyTargetMode = WeeklyTargetMode.FIXED_SCHEDULE,
    val monthlySubMode: MonthlySubMode = MonthlySubMode.SPECIFIC_DATES,
    val monthlyDates: Set<Int> = setOf(1, 15, 30),
    val ordinalWeek: Int = 1, // 1..4 or -1 for last
    val ordinalDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val endOfMonthBehavior: EndOfMonthBehavior = EndOfMonthBehavior.EXECUTE_ON_LAST_DAY,
    val customArchetype: CustomCadenceArchetype = CustomCadenceArchetype.INTERVAL,
    val customInterval: Int = 3,
    val customIntervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val cycleDaysOn: Int = 5,
    val cycleDaysOff: Int = 2,
    val targetPerPeriod: Int = 1,
    val targetTimes: List<TargetTimeSlot> = listOf(
        TargetTimeSlot(id = "t1", hour = 8, minute = 0, enabled = true),
    ),
    val timeOfDayBucket: TimeOfDayBucket = TimeOfDayBucket.MORNING,
    val anchorDate: LocalDate = LocalDate.now(),
) {
    val dailyTargetCount: Int
        get() = maxOf(targetTimes.size, targetPerPeriod).coerceAtLeast(1)

    val isFlexibleWeeklyPool: Boolean
        get() = cadenceType == CadenceType.WEEKLY && weeklyTargetMode == WeeklyTargetMode.FLEXIBLE_POOL

    fun timeSlotForInstance(instanceIndex: Int): TargetTimeSlot? =
        targetTimes.getOrNull(instanceIndex) ?: targetTimes.firstOrNull()

    fun bucketForInstance(instanceIndex: Int): TimeOfDayBucket {
        val slot = timeSlotForInstance(instanceIndex) ?: return timeOfDayBucket
        return TimeOfDayBucket.fromSlot(slot)
    }

    companion object {
        const val BASELINE_EPOCH_DAY: Long = Long.MIN_VALUE

        internal val CHRONOLOGICAL_ORDER: Comparator<HabitSchedule> =
            compareBy<HabitSchedule>(
                { it.effectiveFrom != null },
                { it.effectiveFrom ?: LocalDate.MIN },
                { it.id },
            )
    }
}

data class HabitPauseWindow(
    val id: Long = 0L,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    fun contains(date: LocalDate): Boolean = !date.isBefore(startDate) && !date.isAfter(endDate)

    val durationDays: Int
        get() = (endDate.toEpochDay() - startDate.toEpochDay() + 1L).toInt().coerceAtLeast(1)

    val resumesOnDate: LocalDate
        get() = endDate.plusDays(1)

    companion object {
        fun normalize(windows: List<HabitPauseWindow>): List<HabitPauseWindow> {
            val validSorted = windows
                .filter { !it.endDate.isBefore(it.startDate) }
                .sortedWith(compareBy<HabitPauseWindow>({ it.startDate }, { it.endDate }, { it.id }))
            if (validSorted.isEmpty()) return emptyList()

            val merged = mutableListOf<HabitPauseWindow>()
            for (window in validSorted) {
                val last = merged.lastOrNull()
                if (last == null || window.startDate.isAfter(last.endDate.plusDays(1))) {
                    merged += window
                } else {
                    val extendedEnd = maxOf(last.endDate, window.endDate)
                    merged[merged.lastIndex] = last.copy(endDate = extendedEnd)
                }
            }
            return merged
        }
    }
}

data class Habit(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val domain: String = "",
    val subTag: String? = null,
    val colorHex: String = HabitAccentColor.ROYAL_BLUE.hex,
    val skipReminderIfCompleted: Boolean = true,
    val sortOrder: Int = 0,
    val createdAtEpochDay: Long = LocalDate.now().toEpochDay(),
    val rescheduledFromDates: Set<LocalDate> = emptySet(),
    val rescheduledToDates: Set<LocalDate> = emptySet(),
    val schedules: List<HabitSchedule> = listOf(HabitSchedule()),
    val pauseWindows: List<HabitPauseWindow> = emptyList(),
) {
    val accentColor: HabitAccentColor
        get() = HabitAccentColor.fromHex(colorHex)

    val badgeLabel: String
        get() = if (!subTag.isNullOrBlank()) "$domain · $subTag" else domain

    val latestSchedule: HabitSchedule
        get() = schedules.maxWithOrNull(HabitSchedule.CHRONOLOGICAL_ORDER) ?: HabitSchedule()

    fun scheduleForDate(date: LocalDate): HabitSchedule {
        return schedules
            .filter { it.effectiveFrom == null || !date.isBefore(it.effectiveFrom) }
            .maxWithOrNull(HabitSchedule.CHRONOLOGICAL_ORDER)
            ?: schedules.minWithOrNull(HabitSchedule.CHRONOLOGICAL_ORDER)
            ?: HabitSchedule()
    }

    fun isPausedOn(date: LocalDate): Boolean =
        pauseWindows.any { it.contains(date) }

    fun activePause(today: LocalDate): HabitPauseWindow? =
        pauseWindows.firstOrNull { it.contains(today) }

    fun upcomingPauses(today: LocalDate): List<HabitPauseWindow> =
        pauseWindows.filter { it.startDate.isAfter(today) }.sortedBy { it.startDate }

    fun nextScheduledPause(today: LocalDate): HabitPauseWindow? =
        upcomingPauses(today).firstOrNull()

    fun currentOrUpcomingPause(today: LocalDate): HabitPauseWindow? =
        activePause(today) ?: nextScheduledPause(today)

    companion object {
        fun singleSchedule(
            id: Long = 0L,
            title: String,
            description: String = "",
            domain: String = "",
            subTag: String? = null,
            colorHex: String = HabitAccentColor.ROYAL_BLUE.hex,
            cadenceType: CadenceType = CadenceType.DAILY,
            scheduledDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
            weeklyTargetMode: WeeklyTargetMode = WeeklyTargetMode.FIXED_SCHEDULE,
            monthlySubMode: MonthlySubMode = MonthlySubMode.SPECIFIC_DATES,
            monthlyDates: Set<Int> = setOf(1, 15, 30),
            ordinalWeek: Int = 1,
            ordinalDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
            endOfMonthBehavior: EndOfMonthBehavior = EndOfMonthBehavior.EXECUTE_ON_LAST_DAY,
            customArchetype: CustomCadenceArchetype = CustomCadenceArchetype.INTERVAL,
            customInterval: Int = 3,
            customIntervalUnit: IntervalUnit = IntervalUnit.DAYS,
            cycleDaysOn: Int = 5,
            cycleDaysOff: Int = 2,
            targetPerPeriod: Int = 1,
            targetTimes: List<TargetTimeSlot> = listOf(
                TargetTimeSlot(id = "t1", hour = 8, minute = 0, enabled = true),
            ),
            skipReminderIfCompleted: Boolean = true,
            timeOfDayBucket: TimeOfDayBucket = TimeOfDayBucket.fromTimes(targetTimes),
            sortOrder: Int = 0,
            anchorDate: LocalDate = LocalDate.now(),
            createdAtEpochDay: Long = LocalDate.now().toEpochDay(),
            rescheduledFromDates: Set<LocalDate> = emptySet(),
            rescheduledToDates: Set<LocalDate> = emptySet(),
            pauseWindows: List<HabitPauseWindow> = emptyList(),
        ): Habit = Habit(
            id = id,
            title = title,
            description = description,
            domain = domain,
            subTag = subTag,
            colorHex = colorHex,
            skipReminderIfCompleted = skipReminderIfCompleted,
            sortOrder = sortOrder,
            createdAtEpochDay = createdAtEpochDay,
            rescheduledFromDates = rescheduledFromDates,
            rescheduledToDates = rescheduledToDates,
            schedules = listOf(
                HabitSchedule(
                    id = 0L,
                    effectiveFrom = null,
                    cadenceType = cadenceType,
                    scheduledDays = scheduledDays,
                    weeklyTargetMode = weeklyTargetMode,
                    monthlySubMode = monthlySubMode,
                    monthlyDates = monthlyDates,
                    ordinalWeek = ordinalWeek,
                    ordinalDayOfWeek = ordinalDayOfWeek,
                    endOfMonthBehavior = endOfMonthBehavior,
                    customArchetype = customArchetype,
                    customInterval = customInterval,
                    customIntervalUnit = customIntervalUnit,
                    cycleDaysOn = cycleDaysOn,
                    cycleDaysOff = cycleDaysOff,
                    targetPerPeriod = targetPerPeriod,
                    targetTimes = targetTimes,
                    timeOfDayBucket = timeOfDayBucket,
                    anchorDate = anchorDate,
                ),
            ),
            pauseWindows = pauseWindows,
        )
    }
}

data class DomainChip(
    val name: String,
    val iconName: String,
    val isCustom: Boolean = false,
)
