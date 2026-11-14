package com.rhythm.habittracker.domain

import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.CustomCadenceArchetype
import com.rhythm.habittracker.domain.model.EndOfMonthBehavior
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.IntervalUnit
import com.rhythm.habittracker.domain.model.MonthlySubMode
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CadenceEvaluatorTest {

    @Test
    fun dailyHabit_evaluatesScheduledDaysOnly() {
        val weekdaysHabit = Habit(
            id = 1L,
            title = "Deep Work",
            cadenceType = CadenceType.DAILY,
            scheduledDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
        )

        val monday = LocalDate.of(2026, 5, 11)
        val saturday = LocalDate.of(2026, 5, 16)

        assertTrue(CadenceEvaluator.isHabitScheduledOn(weekdaysHabit, monday))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(weekdaysHabit, saturday))
    }

    @Test
    fun weeklyHabit_flexiblePoolRemainsDueUntilWeeklyQuotaMet() {
        val flexHabit = Habit(
            id = 2L,
            title = "Strength",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
        )

        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val saturday = LocalDate.of(2026, 5, 16)
        val sunday = LocalDate.of(2026, 5, 17)

        val partialWeek = setOf(monday, tuesday)
        assertTrue(CadenceEvaluator.isHabitDueOn(flexHabit, saturday, partialWeek))

        val completedWeek = setOf(monday, tuesday, saturday)
        assertFalse(CadenceEvaluator.isHabitDueOn(flexHabit, sunday, completedWeek))
        assertTrue(CadenceEvaluator.isHabitDueOn(flexHabit, monday, completedWeek))
    }

    @Test
    fun monthlyHabit_shortMonthHandles30And31AccordingToBehavior() {
        val rollBackHabit = Habit(
            id = 3L,
            title = "Monthly Review",
            cadenceType = CadenceType.MONTHLY,
            monthlySubMode = MonthlySubMode.SPECIFIC_DATES,
            monthlyDates = setOf(1, 15, 30),
            endOfMonthBehavior = EndOfMonthBehavior.EXECUTE_ON_LAST_DAY,
        )
        val skipHabit = rollBackHabit.copy(endOfMonthBehavior = EndOfMonthBehavior.SKIP_OCCURRENCE)

        val febLastNonLeap = LocalDate.of(2026, 2, 28)
        val feb15 = LocalDate.of(2026, 2, 15)

        assertTrue(CadenceEvaluator.isHabitScheduledOn(rollBackHabit, feb15))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(rollBackHabit, febLastNonLeap))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(skipHabit, febLastNonLeap))
    }

    @Test
    fun monthlyHabit_ordinalDaysSupportsFirstMondayAndLastFriday() {
        val firstMondayHabit = Habit(
            id = 4L,
            title = "Retrospective",
            cadenceType = CadenceType.MONTHLY,
            monthlySubMode = MonthlySubMode.ORDINAL_DAYS,
            ordinalWeek = 1,
            ordinalDayOfWeek = DayOfWeek.MONDAY,
        )
        val lastFridayHabit = firstMondayHabit.copy(
            ordinalWeek = -1,
            ordinalDayOfWeek = DayOfWeek.FRIDAY,
        )

        // May 2026: May 4 is first Monday, May 29 is last Friday
        assertTrue(CadenceEvaluator.isHabitScheduledOn(firstMondayHabit, LocalDate.of(2026, 5, 4)))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(firstMondayHabit, LocalDate.of(2026, 5, 11)))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(lastFridayHabit, LocalDate.of(2026, 5, 29)))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(lastFridayHabit, LocalDate.of(2026, 5, 22)))
    }

    @Test
    fun customCadence_intervalAndCycleOnOffEvaluateAccurately() {
        val anchor = LocalDate.of(2026, 5, 1)
        val intervalHabit = Habit(
            id = 5L,
            title = "Sauna",
            cadenceType = CadenceType.CUSTOM,
            customArchetype = CustomCadenceArchetype.INTERVAL,
            customInterval = 3,
            customIntervalUnit = IntervalUnit.DAYS,
            anchorDate = anchor,
        )
        assertTrue(CadenceEvaluator.isHabitScheduledOn(intervalHabit, anchor))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(intervalHabit, anchor.plusDays(2)))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(intervalHabit, anchor.plusDays(6)))

        val cycleHabit = intervalHabit.copy(
            customArchetype = CustomCadenceArchetype.CYCLE_ON_OFF,
            cycleDaysOn = 5,
            cycleDaysOff = 2,
        )
        assertTrue(CadenceEvaluator.isHabitScheduledOn(cycleHabit, anchor.plusDays(4)))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(cycleHabit, anchor.plusDays(5)))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(cycleHabit, anchor.plusDays(6)))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(cycleHabit, anchor.plusDays(7)))
    }

    @Test
    fun calculateStreak_skipsUnscheduledDaysWithoutBreakingStreak() {
        val mwfHabit = Habit(
            id = 6L,
            title = "Run",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            createdAtEpochDay = LocalDate.of(2026, 4, 1).toEpochDay(),
        )
        val completed = setOf(
            LocalDate.of(2026, 5, 11), // Mon
            LocalDate.of(2026, 5, 13), // Wed
            LocalDate.of(2026, 5, 15), // Fri
        )

        val streakOnFriday = CadenceEvaluator.calculateStreak(
            habit = mwfHabit,
            completedDates = completed,
            referenceDate = LocalDate.of(2026, 5, 15),
        )
        assertEquals(3, streakOnFriday)
    }

    @Test
    fun buildCadenceMatrix_returns14ColumnsAndValidConsistency() {
        val today = LocalDate.of(2026, 5, 15)
        val habit = Habit(
            id = 1L,
            title = "Hydrate",
            cadenceType = CadenceType.DAILY,
            createdAtEpochDay = today.minusDays(90).toEpochDay(),
        )
        val completions = mapOf(
            today to setOf(1L),
            today.minusDays(1) to setOf(1L),
            today.minusDays(2) to setOf(1L),
        )

        val matrix = CadenceEvaluator.buildCadenceMatrix(
            habits = listOf(habit),
            completionsByDate = completions,
            today = today,
            weeks = 14,
        )

        assertEquals(14, matrix.columns.size)
        assertTrue(matrix.columns.all { it.size == 7 })
        assertEquals(3, matrix.overallStreakDays)
        assertTrue(matrix.consistencyPercent in 1..100)
        assertEquals(3, matrix.completionsThisMonth)
    }

    @Test
    fun customCadence_weeklyFixedRespectsFutureAnchorDate() {
        val anchor = LocalDate.of(2026, 5, 18) // Monday
        val habit = Habit(
            id = 7L,
            title = "Future Sprint",
            cadenceType = CadenceType.CUSTOM,
            customArchetype = CustomCadenceArchetype.WEEKLY_FIXED,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            anchorDate = anchor,
        )

        assertFalse(CadenceEvaluator.isHabitScheduledOn(habit, LocalDate.of(2026, 5, 15))) // Friday before start
        assertTrue(CadenceEvaluator.isHabitScheduledOn(habit, LocalDate.of(2026, 5, 18)))  // Monday on start
        assertTrue(CadenceEvaluator.isHabitScheduledOn(habit, LocalDate.of(2026, 5, 20)))  // Wednesday after start
    }

    @Test
    fun rescheduleHabitDate_shiftsDateAndBlocksConflicts() {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val wednesday = LocalDate.of(2026, 5, 13)
        val mwfHabit = Habit(
            id = 8L,
            title = "Strength",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )

        // Cannot reschedule to Wednesday since Wednesday already has a scheduled instance
        val conflictResult = CadenceEvaluator.rescheduleHabitDate(mwfHabit, monday, wednesday)
        assertEquals(null, conflictResult)

        // Can reschedule Monday -> Tuesday
        val shifted = CadenceEvaluator.rescheduleHabitDate(mwfHabit, monday, tuesday)!!
        assertFalse(CadenceEvaluator.isHabitScheduledOn(shifted, monday))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(shifted, tuesday))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(shifted, wednesday))

        // Shifting Tuesday back to Monday restores base schedule cleanly
        val shiftedBack = CadenceEvaluator.rescheduleHabitDate(shifted, tuesday, monday)!!
        assertTrue(CadenceEvaluator.isHabitScheduledOn(shiftedBack, monday))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(shiftedBack, tuesday))
        assertTrue(shiftedBack.rescheduledFromDates.isEmpty())
        assertTrue(shiftedBack.rescheduledToDates.isEmpty())
    }

    @Test
    fun rescheduleHabitDate_rejectsDoubleRescheduleAndAvoidsDuplicateOverrides() {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val thursday = LocalDate.of(2026, 5, 14)
        val saturday = LocalDate.of(2026, 5, 16)
        val mwfHabit = Habit(
            id = 9L,
            title = "Mobility",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )

        val shiftedOnce = CadenceEvaluator.rescheduleHabitDate(mwfHabit, monday, tuesday)!!
        // Source Monday was already shifted away, so trying to shift Monday again fails
        assertEquals(null, CadenceEvaluator.rescheduleHabitDate(shiftedOnce, monday, thursday))
        // Unscheduled Thursday cannot be used as a source
        assertEquals(null, CadenceEvaluator.rescheduleHabitDate(shiftedOnce, thursday, saturday))

        // Chaining Tuesday -> Thursday moves the target override without leaving Tuesday behind
        val shiftedTwice = CadenceEvaluator.rescheduleHabitDate(shiftedOnce, tuesday, thursday)!!
        assertEquals(setOf(monday), shiftedTwice.rescheduledFromDates)
        assertEquals(setOf(thursday), shiftedTwice.rescheduledToDates)
        assertFalse(CadenceEvaluator.isHabitScheduledOn(shiftedTwice, tuesday))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(shiftedTwice, thursday))
    }

    @Test
    fun sanitizeRescheduleOverrides_prunesInvalidAndOverlappingDates() {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val wednesday = LocalDate.of(2026, 5, 13)
        val thursday = LocalDate.of(2026, 5, 14)

        val mwfWithStale = Habit(
            id = 10L,
            title = "Piano",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            // Tuesday is not base-scheduled (invalid in fromDates); Wednesday IS base-scheduled (invalid in toDates)
            rescheduledFromDates = setOf(monday, tuesday),
            rescheduledToDates = setOf(tuesday, wednesday, thursday),
        )
        val sanitizedFixed = CadenceEvaluator.sanitizeRescheduleOverrides(mwfWithStale)
        assertEquals(setOf(monday), sanitizedFixed.rescheduledFromDates)
        assertEquals(setOf(tuesday, thursday), sanitizedFixed.rescheduledToDates)

        // For flexible pool, same date in both from and to cancels out as overlap
        val flexWithOverlap = Habit(
            id = 11L,
            title = "Journal",
            cadenceType = CadenceType.WEEKLY,
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
            scheduledDays = setOf(DayOfWeek.MONDAY),
            rescheduledFromDates = setOf(tuesday, thursday),
            rescheduledToDates = setOf(tuesday),
        )
        val sanitizedFlex = CadenceEvaluator.sanitizeRescheduleOverrides(flexWithOverlap)
        assertEquals(setOf(thursday), sanitizedFlex.rescheduledFromDates)
        assertTrue(sanitizedFlex.rescheduledToDates.isEmpty())
    }

    @Test
    fun isHabitDueOn_ignoresRescheduledFromDatesEvenIfStrayCompletionExists() {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val habit = Habit(
            id = 12L,
            title = "Read",
            cadenceType = CadenceType.DAILY,
            scheduledDays = DayOfWeek.entries.toSet(),
            rescheduledFromDates = setOf(monday),
            rescheduledToDates = setOf(tuesday),
        )

        assertFalse(CadenceEvaluator.isHabitDueOn(habit, monday, completedDates = setOf(monday)))
        assertTrue(CadenceEvaluator.isHabitDueOn(habit, tuesday, completedDates = setOf(monday)))
    }

    @Test
    fun rescheduleHabitDate_flexiblePoolMovesPoolDayAndBlocksBaseConflict() {
        val tuesday = LocalDate.of(2026, 5, 12)
        val wednesday = LocalDate.of(2026, 5, 13)
        val thursday = LocalDate.of(2026, 5, 14)
        val flexHabit = Habit(
            id = 13L,
            title = "Pool Workout",
            cadenceType = CadenceType.WEEKLY,
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )

        // Cannot move onto Wednesday since base schedule includes Wednesday
        assertEquals(null, CadenceEvaluator.rescheduleHabitDate(flexHabit, tuesday, wednesday))

        // Can move Tuesday -> Thursday
        val shifted = CadenceEvaluator.rescheduleHabitDate(flexHabit, tuesday, thursday)!!
        assertFalse(CadenceEvaluator.isHabitDueOn(shifted, tuesday, completedDates = emptySet()))
        assertTrue(CadenceEvaluator.isHabitDueOn(shifted, thursday, completedDates = emptySet()))
        assertEquals(setOf(tuesday), shifted.rescheduledFromDates)
        assertEquals(setOf(thursday), shifted.rescheduledToDates)
    }

    @Test
    fun calculateBestStreak_tracksHistoricalPeakSeparatelyFromCurrentStreak() {
        val today = LocalDate.of(2026, 5, 20)
        val dailyHabit = Habit(
            id = 14L,
            title = "Deep Work",
            cadenceType = CadenceType.DAILY,
            createdAtEpochDay = today.minusDays(20).toEpochDay(),
        )
        // Past 5-day run (days -18..-14), missed day -13, current 2-day run (days -1..0)
        val completed = setOf(
            today.minusDays(18),
            today.minusDays(17),
            today.minusDays(16),
            today.minusDays(15),
            today.minusDays(14),
            today.minusDays(1),
            today,
        )

        assertEquals(2, CadenceEvaluator.calculateStreak(dailyHabit, completed, today))
        assertEquals(5, CadenceEvaluator.calculateBestStreak(dailyHabit, completed, today))
    }

    @Test
    fun calculateBestStreak_flexiblePoolAccumulatesHistoricalQualifyingWeeks() {
        val monday = LocalDate.of(2026, 5, 18)
        val flexHabit = Habit(
            id = 15L,
            title = "Gym",
            cadenceType = CadenceType.WEEKLY,
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), // 2x/week
            createdAtEpochDay = monday.minusWeeks(4).toEpochDay(),
        )
        // Week -4 (2x) + Week -3 (2x) = 4; Week -2 missed (1x resets); Current week (1x)
        val completed = setOf(
            monday.minusWeeks(4),
            monday.minusWeeks(4).plusDays(2),
            monday.minusWeeks(3),
            monday.minusWeeks(3).plusDays(3),
            monday.minusWeeks(2),
            monday,
        )

        assertEquals(1, CadenceEvaluator.calculateStreak(flexHabit, completed, monday))
        assertEquals(4, CadenceEvaluator.calculateBestStreak(flexHabit, completed, monday))
    }

    @Test
    fun formatHabitScheduleSummary_formatsAllCadenceTypes() {
        val dailyHabit = Habit(
            id = 16L,
            title = "Read",
            cadenceType = CadenceType.DAILY,
            targetTimes = listOf(com.rhythm.habittracker.domain.model.TargetTimeSlot("t1", 21, 45, true)),
        )
        assertEquals("DAILY · 09:45 PM", CadenceEvaluator.formatHabitScheduleSummary(dailyHabit))

        val weeklyHabit = Habit(
            id = 17L,
            title = "Run",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            targetTimes = listOf(com.rhythm.habittracker.domain.model.TargetTimeSlot("t2", 7, 0, true)),
        )
        assertEquals("WEEKLY · MON, WED, FRI · 07:00 AM", CadenceEvaluator.formatHabitScheduleSummary(weeklyHabit))

        val monthlyHabit = Habit(
            id = 18L,
            title = "Budget",
            cadenceType = CadenceType.MONTHLY,
            monthlySubMode = MonthlySubMode.SPECIFIC_DATES,
            monthlyDates = setOf(1, 15),
            targetTimes = listOf(com.rhythm.habittracker.domain.model.TargetTimeSlot("t3", 9, 0, true)),
        )
        assertEquals("MONTHLY · 1ST, 15TH · 09:00 AM", CadenceEvaluator.formatHabitScheduleSummary(monthlyHabit))

        val customInterval = Habit(
            id = 19L,
            title = "Sauna",
            cadenceType = CadenceType.CUSTOM,
            customArchetype = CustomCadenceArchetype.INTERVAL,
            customInterval = 3,
            customIntervalUnit = IntervalUnit.DAYS,
        )
        assertEquals("CUSTOM · EVERY 3 DAYS", CadenceEvaluator.formatHabitScheduleSummary(customInterval))
    }

    @Test
    fun buildCadenceMatrix_scalesRatioByScheduledTasksAndFlagsUnscheduledEmptyDays() {
        // Friday 2026-05-15; Mon=May 11 .. Sun=May 17
        val today = LocalDate.of(2026, 5, 15)
        val monWedFri = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val slot1 = com.rhythm.habittracker.domain.model.TargetTimeSlot("s1", 8, 0, true)
        val slot2 = com.rhythm.habittracker.domain.model.TargetTimeSlot("s2", 20, 0, true)
        val habitA = Habit(
            id = 1L,
            title = "Hydrate",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = monWedFri,
            targetTimes = listOf(slot1, slot2),
            createdAtEpochDay = today.minusDays(21).toEpochDay(),
        )
        val habitB = Habit(
            id = 2L,
            title = "Mobility",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = monWedFri,
            targetTimes = listOf(slot1, slot2),
            createdAtEpochDay = today.minusDays(21).toEpochDay(),
        )

        val prevFri = LocalDate.of(2026, 5, 8)
        val mon = LocalDate.of(2026, 5, 11)
        val tue = LocalDate.of(2026, 5, 12)
        val wed = LocalDate.of(2026, 5, 13)
        val thu = LocalDate.of(2026, 5, 14)
        val sat = LocalDate.of(2026, 5, 16)

        val instances = mapOf(
            prevFri to mapOf(1L to setOf(0, 1), 2L to setOf(0, 1)), // 4/4 -> 1.0f HIGH
            // mon omitted -> 0/4 -> 0f NONE, isUnscheduledEmpty=false
            // tue omitted -> unscheduled, 0 completed -> isUnscheduledEmpty=true
            wed to mapOf(1L to setOf(0)), // 1/4 -> 0.25f LOW
            thu to mapOf(1L to setOf(0)), // off-schedule completion on unscheduled day -> 1f HIGH, not empty
            today to mapOf(1L to setOf(0, 1), 2L to setOf(0)), // 3/4 -> 0.75f MEDIUM (not HIGH!)
        )
        val completionsByDate = instances.mapValues { (_, habitMap) -> habitMap.keys }

        val matrix = CadenceEvaluator.buildCadenceMatrix(
            habits = listOf(habitA, habitB),
            completionsByDate = completionsByDate,
            today = today,
            weeks = 14,
            completedInstancesByDate = instances,
        )

        val cellsByDate = matrix.columns.flatten().associateBy { it.date }

        val prevFriCell = cellsByDate.getValue(prevFri)
        assertEquals(4, prevFriCell.scheduledCount)
        assertEquals(4, prevFriCell.completedCount)
        assertEquals(1f, prevFriCell.completionRatio, 0.001f)
        assertEquals(MatrixIntensity.HIGH, prevFriCell.intensity)
        assertFalse(prevFriCell.isUnscheduledEmpty)

        val monCell = cellsByDate.getValue(mon)
        assertEquals(4, monCell.scheduledCount)
        assertEquals(0, monCell.completedCount)
        assertEquals(0f, monCell.completionRatio, 0.001f)
        assertEquals(MatrixIntensity.NONE, monCell.intensity)
        assertFalse(monCell.isUnscheduledEmpty)

        val tueCell = cellsByDate.getValue(tue)
        assertEquals(0, tueCell.scheduledCount)
        assertEquals(0, tueCell.completedCount)
        assertEquals(0f, tueCell.completionRatio, 0.001f)
        assertEquals(MatrixIntensity.NONE, tueCell.intensity)
        assertTrue(tueCell.isUnscheduledEmpty)

        val wedCell = cellsByDate.getValue(wed)
        assertEquals(4, wedCell.scheduledCount)
        assertEquals(1, wedCell.completedCount)
        assertEquals(0.25f, wedCell.completionRatio, 0.001f)
        assertEquals(MatrixIntensity.LOW, wedCell.intensity)
        assertFalse(wedCell.isUnscheduledEmpty)

        val thuCell = cellsByDate.getValue(thu)
        assertEquals(0, thuCell.scheduledCount)
        assertEquals(1, thuCell.completedCount)
        assertEquals(1f, thuCell.completionRatio, 0.001f)
        assertEquals(MatrixIntensity.HIGH, thuCell.intensity)
        assertFalse(thuCell.isUnscheduledEmpty)

        val friCell = cellsByDate.getValue(today)
        assertEquals(4, friCell.scheduledCount)
        assertEquals(3, friCell.completedCount)
        assertEquals(0.75f, friCell.completionRatio, 0.001f)
        assertEquals(MatrixIntensity.MEDIUM, friCell.intensity)
        assertFalse(friCell.isUnscheduledEmpty)

        val satCell = cellsByDate.getValue(sat)
        assertTrue(satCell.isFuture)
        assertTrue(satCell.isUnscheduledEmpty)
    }
}
