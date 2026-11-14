package com.rhythm.habittracker.widget

import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WidgetDataBuilderTest {

    private val today = LocalDate.of(2026, 10, 24) // Saturday

    private fun sampleHabit(
        id: Long,
        title: String,
        color: HabitAccentColor = HabitAccentColor.ROYAL_BLUE,
        hour: Int = 8,
        minute: Int = 0,
        targetPerPeriod: Int = 1,
        targetTimes: List<TargetTimeSlot> = listOf(TargetTimeSlot(id = "t_$id", hour = hour, minute = minute)),
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        sortOrder: Int = 0,
    ): Habit = Habit(
        id = id,
        title = title,
        description = "$title desc",
        domain = "Focus",
        colorHex = color.hex,
        cadenceType = CadenceType.DAILY,
        scheduledDays = days,
        targetPerPeriod = targetPerPeriod,
        targetTimes = targetTimes,
        timeOfDayBucket = TimeOfDayBucket.fromTimes(targetTimes),
        sortOrder = sortOrder,
        createdAtEpochDay = today.minusDays(30).toEpochDay(),
    )

    @Test
    fun `buildSingleHabitData selects configured habit and falls back cleanly`() {
        val h1 = sampleHabit(id = 1L, title = "Deep Work", color = HabitAccentColor.ROYAL_BLUE, hour = 8, minute = 30)
        val h2 = sampleHabit(id = 2L, title = "Cold Plunge", color = HabitAccentColor.EMERALD_GREEN, hour = 7, minute = 0)

        val completions = mapOf(
            today to mapOf(2L to setOf(0)),
            today.minusDays(1) to mapOf(2L to setOf(0), 1L to setOf(0)),
            today.minusDays(2) to mapOf(2L to setOf(0)),
        )

        val selectedSecond = WidgetDataBuilder.buildSingleHabitData(
            habits = listOf(h1, h2),
            completedInstancesByDate = completions,
            configuredHabitId = 2L,
            today = today,
            maxWeeks = 4,
        )
        assertEquals(2L, selectedSecond.habit?.id)
        assertEquals(3, selectedSecond.currentStreak)
        assertEquals("07:00 AM", selectedSecond.primaryTargetLabel)
        assertTrue(selectedSecond.isCompletedToday)
        assertEquals("Completed Today", selectedSecond.statusLabel)
        assertEquals(4, selectedSecond.matrixColumns.size)

        // Missing configured ID falls back to first habit
        val fallbackFirst = WidgetDataBuilder.buildSingleHabitData(
            habits = listOf(h1, h2),
            completedInstancesByDate = completions,
            configuredHabitId = 999L,
            today = today,
            maxWeeks = 4,
        )
        assertEquals(1L, fallbackFirst.habit?.id)
        assertEquals(1, fallbackFirst.currentStreak)
        assertFalse(fallbackFirst.isCompletedToday)
        assertEquals("Scheduled for 08:30 AM", fallbackFirst.statusLabel)
    }

    @Test
    fun `buildSingleHabitData handles empty habit list cleanly`() {
        val emptyData = WidgetDataBuilder.buildSingleHabitData(
            habits = emptyList(),
            completedInstancesByDate = emptyMap(),
            configuredHabitId = null,
            today = today,
        )
        assertNull(emptyData.habit)
        assertEquals("NO HABIT CONFIGURED", emptyData.domainBadgeLabel)
        assertEquals("TAP TO CONFIGURE", emptyData.statusLabel)
    }

    @Test
    fun `buildOverallMatrixData aggregates morning afternoon evening buckets and next pending task`() {
        val morningHabit = sampleHabit(id = 1L, title = "Meditation", hour = 7, minute = 30)
        val multiHydration = sampleHabit(
            id = 2L,
            title = "Hydrate",
            color = HabitAccentColor.WARM_AMBER,
            targetPerPeriod = 2,
            targetTimes = listOf(
                TargetTimeSlot(id = "h1", hour = 10, minute = 0),
                TargetTimeSlot(id = "h2", hour = 14, minute = 0),
            ),
        )
        val eveningHabit = sampleHabit(id = 3L, title = "Journal", hour = 21, minute = 0)

        val completions = mapOf(
            today to mapOf(
                1L to setOf(0),
                2L to setOf(0),
            ),
        )

        val data = WidgetDataBuilder.buildOverallMatrixData(
            habits = listOf(morningHabit, multiHydration, eveningHabit),
            completedInstancesByDate = completions,
            today = today,
            maxWeeks = 8,
        )

        assertEquals(2, data.morningCompleted)
        assertEquals(2, data.morningTotal)
        assertEquals(0, data.afternoonCompleted)
        assertEquals(1, data.afternoonTotal)
        assertEquals(0, data.eveningCompleted)
        assertEquals(1, data.eveningTotal)
        assertTrue(data.hasPendingTasksToday)
        assertEquals("02:00 PM · HYDRATE", data.nextTaskLabel)
        assertEquals(8, data.matrixColumns.size)
        assertEquals(1, data.overallStreakDays)
    }

    @Test
    fun `buildUpcomingTasksData orders pending before completed, marks NOW target, and builds color segments`() {
        val morningDone = sampleHabit(
            id = 1L,
            title = "Cold Plunge",
            color = HabitAccentColor.ROYAL_BLUE,
            hour = 7,
            minute = 0,
        )
        val afternoonPending = sampleHabit(
            id = 2L,
            title = "Deep Work",
            color = HabitAccentColor.EMERALD_GREEN,
            hour = 13,
            minute = 30,
        )
        val earlyPending = sampleHabit(
            id = 3L,
            title = "Read 10 Pages",
            color = HabitAccentColor.WARM_AMBER,
            hour = 9,
            minute = 0,
        )

        val completions = mapOf(
            today to mapOf(1L to setOf(0)),
        )

        val data = WidgetDataBuilder.buildUpcomingTasksData(
            habits = listOf(morningDone, afternoonPending, earlyPending),
            completedInstancesByDate = completions,
            today = today,
        )

        assertEquals(1, data.completedCount)
        assertEquals(3, data.totalCount)
        assertEquals(33, data.completionPercent)
        assertEquals("NEXT UP: 09:00 AM", data.nextUpFooterLabel)

        // First segment colored by completed item's accent, remaining 2 segments null
        assertEquals(listOf(HabitAccentColor.ROYAL_BLUE.hex, null, null), data.segmentColorHexes)

        // Pending sorted by time first, then completed
        assertEquals(3, data.orderedTasks.size)
        assertEquals("Read 10 Pages", data.orderedTasks[0].title)
        assertTrue(data.orderedTasks[0].isNowTarget)
        assertFalse(data.orderedTasks[0].isCompleted)

        assertEquals("Deep Work", data.orderedTasks[1].title)
        assertFalse(data.orderedTasks[1].isNowTarget)
        assertFalse(data.orderedTasks[1].isCompleted)

        assertEquals("Cold Plunge", data.orderedTasks[2].title)
        assertTrue(data.orderedTasks[2].isCompleted)
        assertFalse(data.orderedTasks[2].isNowTarget)
    }
}
