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

data class Habit(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val domain: String = "",
    val subTag: String? = null,
    val colorHex: String = HabitAccentColor.ROYAL_BLUE.hex,
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
    val skipReminderIfCompleted: Boolean = true,
    val timeOfDayBucket: TimeOfDayBucket = TimeOfDayBucket.MORNING,
    val sortOrder: Int = 0,
    val anchorDate: LocalDate = LocalDate.now(),
    val createdAtEpochDay: Long = LocalDate.now().toEpochDay(),
    val rescheduledFromDates: Set<LocalDate> = emptySet(),
    val rescheduledToDates: Set<LocalDate> = emptySet(),
) {
    val accentColor: HabitAccentColor
        get() = HabitAccentColor.fromHex(colorHex)

    val badgeLabel: String
        get() = if (!subTag.isNullOrBlank()) "$domain · $subTag" else domain

    val dailyTargetCount: Int
        get() = maxOf(targetTimes.size, targetPerPeriod).coerceAtLeast(1)

    fun timeSlotForInstance(instanceIndex: Int): TargetTimeSlot? =
        targetTimes.getOrNull(instanceIndex) ?: targetTimes.firstOrNull()

    fun bucketForInstance(instanceIndex: Int): TimeOfDayBucket {
        val slot = timeSlotForInstance(instanceIndex) ?: return timeOfDayBucket
        return TimeOfDayBucket.fromSlot(slot)
    }
}

data class DomainChip(
    val name: String,
    val iconName: String,
    val isCustom: Boolean = false,
)
