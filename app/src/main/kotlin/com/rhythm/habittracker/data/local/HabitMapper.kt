package com.rhythm.habittracker.data.local

import com.rhythm.habittracker.data.local.entity.HabitEntity
import com.rhythm.habittracker.data.local.entity.HabitPauseEntity
import com.rhythm.habittracker.data.local.entity.HabitScheduleEntity
import com.rhythm.habittracker.data.local.entity.HabitWithDetails
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.CustomCadenceArchetype
import com.rhythm.habittracker.domain.model.EndOfMonthBehavior
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitPauseWindow
import com.rhythm.habittracker.domain.model.HabitSchedule
import com.rhythm.habittracker.domain.model.IntervalUnit
import com.rhythm.habittracker.domain.model.MonthlySubMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

private val mapperJson = Json { ignoreUnknownKeys = true }

fun HabitScheduleEntity.toDomain(): HabitSchedule {
    val days = scheduledDaysCsv
        .split(",")
        .mapNotNull { it.trim().toIntOrNull()?.let { v -> DayOfWeek.entries.firstOrNull { d -> d.value == v } } }
        .toSet()
        .ifEmpty { DayOfWeek.entries.toSet() }

    val dates = monthlyDatesCsv
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..31 }
        .toSet()
        .ifEmpty { setOf(1) }

    val times = runCatching {
        mapperJson.decodeFromString<List<TargetTimeSlot>>(targetTimesJson)
    }.getOrDefault(emptyList())

    val effectiveFrom = if (effectiveFromEpochDay == HabitSchedule.BASELINE_EPOCH_DAY) {
        null
    } else {
        LocalDate.ofEpochDay(effectiveFromEpochDay)
    }

    return HabitSchedule(
        id = id,
        effectiveFrom = effectiveFrom,
        cadenceType = enumValueOrDefault(cadenceType, CadenceType.DAILY),
        scheduledDays = days,
        weeklyTargetMode = enumValueOrDefault(weeklyTargetMode, WeeklyTargetMode.FIXED_SCHEDULE),
        monthlySubMode = enumValueOrDefault(monthlySubMode, MonthlySubMode.SPECIFIC_DATES),
        monthlyDates = dates,
        ordinalWeek = ordinalWeek,
        ordinalDayOfWeek = DayOfWeek.entries.firstOrNull { it.value == ordinalDayOfWeek } ?: DayOfWeek.MONDAY,
        endOfMonthBehavior = enumValueOrDefault(endOfMonthBehavior, EndOfMonthBehavior.EXECUTE_ON_LAST_DAY),
        customArchetype = enumValueOrDefault(customArchetype, CustomCadenceArchetype.INTERVAL),
        customInterval = customInterval,
        customIntervalUnit = enumValueOrDefault(customIntervalUnit, IntervalUnit.DAYS),
        cycleDaysOn = cycleDaysOn,
        cycleDaysOff = cycleDaysOff,
        targetPerPeriod = targetPerPeriod,
        targetTimes = times,
        timeOfDayBucket = enumValueOrDefault(timeOfDayBucket, TimeOfDayBucket.MORNING),
        anchorDate = LocalDate.ofEpochDay(anchorDateEpochDay),
    )
}

fun HabitPauseEntity.toDomain(): HabitPauseWindow = HabitPauseWindow(
    id = id,
    startDate = LocalDate.ofEpochDay(startEpochDay),
    endDate = LocalDate.ofEpochDay(endEpochDay),
)

fun HabitWithDetails.toDomain(): Habit {
    val rescheduledFromDates = parseEpochDaysCsv(habit.rescheduledFromEpochDaysCsv)
    val rescheduledToDates = parseEpochDaysCsv(habit.rescheduledToEpochDaysCsv)
    val mappedSchedules = schedules
        .map { it.toDomain() }
        .sortedWith(HabitSchedule.CHRONOLOGICAL_ORDER)
        .ifEmpty { listOf(HabitSchedule()) }
    val mappedPauses = HabitPauseWindow.normalize(pauses.map { it.toDomain() })

    return Habit(
        id = habit.id,
        title = habit.title,
        description = habit.description,
        domain = habit.domain,
        subTag = habit.subTag,
        colorHex = habit.colorHex,
        skipReminderIfCompleted = habit.skipReminderIfCompleted,
        sortOrder = habit.sortOrder,
        createdAtEpochDay = habit.createdAtEpochDay,
        rescheduledFromDates = rescheduledFromDates,
        rescheduledToDates = rescheduledToDates,
        schedules = mappedSchedules,
        pauseWindows = mappedPauses,
    )
}

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    title = title,
    description = description,
    domain = domain,
    subTag = subTag,
    colorHex = colorHex,
    skipReminderIfCompleted = skipReminderIfCompleted,
    sortOrder = sortOrder,
    createdAtEpochDay = createdAtEpochDay,
    rescheduledFromEpochDaysCsv = rescheduledFromDates.map { it.toEpochDay() }.sorted().joinToString(","),
    rescheduledToEpochDaysCsv = rescheduledToDates.map { it.toEpochDay() }.sorted().joinToString(","),
)

fun HabitSchedule.toEntity(habitId: Long): HabitScheduleEntity = HabitScheduleEntity(
    id = id,
    habitId = habitId,
    effectiveFromEpochDay = effectiveFrom?.toEpochDay() ?: HabitSchedule.BASELINE_EPOCH_DAY,
    cadenceType = cadenceType.name,
    scheduledDaysCsv = scheduledDays.map { it.value }.sorted().joinToString(","),
    weeklyTargetMode = weeklyTargetMode.name,
    monthlySubMode = monthlySubMode.name,
    monthlyDatesCsv = monthlyDates.sorted().joinToString(","),
    ordinalWeek = ordinalWeek,
    ordinalDayOfWeek = ordinalDayOfWeek.value,
    endOfMonthBehavior = endOfMonthBehavior.name,
    customArchetype = customArchetype.name,
    customInterval = customInterval,
    customIntervalUnit = customIntervalUnit.name,
    cycleDaysOn = cycleDaysOn,
    cycleDaysOff = cycleDaysOff,
    targetPerPeriod = targetPerPeriod,
    targetTimesJson = mapperJson.encodeToString(targetTimes),
    timeOfDayBucket = timeOfDayBucket.name,
    anchorDateEpochDay = anchorDate.toEpochDay(),
)

fun HabitPauseWindow.toEntity(habitId: Long): HabitPauseEntity = HabitPauseEntity(
    id = id,
    habitId = habitId,
    startEpochDay = startDate.toEpochDay(),
    endEpochDay = endDate.toEpochDay(),
)

private fun parseEpochDaysCsv(csv: String): Set<LocalDate> =
    csv.split(",")
        .mapNotNull { it.trim().toLongOrNull()?.let(LocalDate::ofEpochDay) }
        .toSet()

private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: fallback
