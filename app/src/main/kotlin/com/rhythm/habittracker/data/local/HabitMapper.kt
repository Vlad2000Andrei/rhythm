package com.rhythm.habittracker.data.local

import com.rhythm.habittracker.data.local.entity.HabitEntity
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.CustomCadenceArchetype
import com.rhythm.habittracker.domain.model.EndOfMonthBehavior
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.IntervalUnit
import com.rhythm.habittracker.domain.model.MonthlySubMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

private val mapperJson = Json { ignoreUnknownKeys = true }

fun HabitEntity.toDomain(): Habit {
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

    val times = runCatching {
        mapperJson.decodeFromString<List<TargetTimeSlot>>(targetTimesJson)
    }.getOrDefault(emptyList())

    val rescheduledFromDates = parseEpochDaysCsv(rescheduledFromEpochDaysCsv)
    val rescheduledToDates = parseEpochDaysCsv(rescheduledToEpochDaysCsv)

    return Habit(
        id = id,
        title = title,
        description = description,
        domain = domain,
        subTag = subTag,
        colorHex = colorHex,
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
        skipReminderIfCompleted = skipReminderIfCompleted,
        timeOfDayBucket = enumValueOrDefault(timeOfDayBucket, TimeOfDayBucket.MORNING),
        sortOrder = sortOrder,
        anchorDate = LocalDate.ofEpochDay(anchorDateEpochDay),
        createdAtEpochDay = createdAtEpochDay,
        rescheduledFromDates = rescheduledFromDates,
        rescheduledToDates = rescheduledToDates,
    )
}

fun Habit.toEntity(): HabitEntity {
    return HabitEntity(
        id = id,
        title = title,
        description = description,
        domain = domain,
        subTag = subTag,
        colorHex = colorHex,
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
        skipReminderIfCompleted = skipReminderIfCompleted,
        timeOfDayBucket = timeOfDayBucket.name,
        sortOrder = sortOrder,
        anchorDateEpochDay = anchorDate.toEpochDay(),
        createdAtEpochDay = createdAtEpochDay,
        rescheduledFromEpochDaysCsv = rescheduledFromDates.map { it.toEpochDay() }.sorted().joinToString(","),
        rescheduledToEpochDaysCsv = rescheduledToDates.map { it.toEpochDay() }.sorted().joinToString(","),
    )
}

private fun parseEpochDaysCsv(csv: String): Set<LocalDate> =
    csv.split(",")
        .mapNotNull { it.trim().toLongOrNull()?.let(LocalDate::ofEpochDay) }
        .toSet()

private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: fallback
