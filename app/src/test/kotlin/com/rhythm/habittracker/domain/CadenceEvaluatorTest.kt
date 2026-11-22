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
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class CadenceEvaluatorTest {

    @Test
    fun dailyHabit_evaluatesScheduledDaysOnly() {
        val weekdaysHabit = Habit.singleSchedule(
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
        val flexHabit = Habit.singleSchedule(
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
        val rollBackHabit = Habit.singleSchedule(
            id = 3L,
            title = "Monthly Review",
            cadenceType = CadenceType.MONTHLY,
            monthlySubMode = MonthlySubMode.SPECIFIC_DATES,
            monthlyDates = setOf(1, 15, 30),
            endOfMonthBehavior = EndOfMonthBehavior.EXECUTE_ON_LAST_DAY,
        )
        val skipHabit = rollBackHabit.copy(
            schedules = listOf(
                rollBackHabit.latestSchedule.copy(endOfMonthBehavior = EndOfMonthBehavior.SKIP_OCCURRENCE),
            ),
        )

        val febLastNonLeap = LocalDate.of(2026, 2, 28)
        val feb15 = LocalDate.of(2026, 2, 15)

        assertTrue(CadenceEvaluator.isHabitScheduledOn(rollBackHabit, feb15))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(rollBackHabit, febLastNonLeap))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(skipHabit, febLastNonLeap))
    }

    @Test
    fun monthlyHabit_ordinalDaysSupportsFirstMondayAndLastFriday() {
        val firstMondayHabit = Habit.singleSchedule(
            id = 4L,
            title = "Retrospective",
            cadenceType = CadenceType.MONTHLY,
            monthlySubMode = MonthlySubMode.ORDINAL_DAYS,
            ordinalWeek = 1,
            ordinalDayOfWeek = DayOfWeek.MONDAY,
        )
        val lastFridayHabit = firstMondayHabit.copy(
            schedules = listOf(
                firstMondayHabit.latestSchedule.copy(
                    ordinalWeek = -1,
                    ordinalDayOfWeek = DayOfWeek.FRIDAY,
                ),
            ),
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
        val intervalHabit = Habit.singleSchedule(
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
            schedules = listOf(
                intervalHabit.latestSchedule.copy(
                    customArchetype = CustomCadenceArchetype.CYCLE_ON_OFF,
                    cycleDaysOn = 5,
                    cycleDaysOff = 2,
                ),
            ),
        )
        assertTrue(CadenceEvaluator.isHabitScheduledOn(cycleHabit, anchor.plusDays(4)))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(cycleHabit, anchor.plusDays(5)))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(cycleHabit, anchor.plusDays(6)))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(cycleHabit, anchor.plusDays(7)))
    }

    @Test
    fun calculateStreak_skipsUnscheduledDaysWithoutBreakingStreak() {
        val mwfHabit = Habit.singleSchedule(
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
        val habit = Habit.singleSchedule(
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
        val habit = Habit.singleSchedule(
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
        val mwfHabit = Habit.singleSchedule(
            id = 8L,
            title = "Strength",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )

        val conflictResult = CadenceEvaluator.rescheduleHabitDate(mwfHabit, monday, wednesday)
        assertNull(conflictResult)

        val shifted = CadenceEvaluator.rescheduleHabitDate(mwfHabit, monday, tuesday)!!
        assertFalse(CadenceEvaluator.isHabitScheduledOn(shifted, monday))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(shifted, tuesday))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(shifted, wednesday))

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
        val mwfHabit = Habit.singleSchedule(
            id = 9L,
            title = "Mobility",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )

        val shiftedOnce = CadenceEvaluator.rescheduleHabitDate(mwfHabit, monday, tuesday)!!
        assertNull(CadenceEvaluator.rescheduleHabitDate(shiftedOnce, monday, thursday))
        assertNull(CadenceEvaluator.rescheduleHabitDate(shiftedOnce, thursday, saturday))

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

        val mwfWithStale = Habit.singleSchedule(
            id = 10L,
            title = "Piano",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            rescheduledFromDates = setOf(monday, tuesday),
            rescheduledToDates = setOf(tuesday, wednesday, thursday),
        )
        val sanitizedFixed = CadenceEvaluator.sanitizeRescheduleOverrides(mwfWithStale)
        assertEquals(setOf(monday), sanitizedFixed.rescheduledFromDates)
        assertEquals(setOf(tuesday, thursday), sanitizedFixed.rescheduledToDates)

        val flexWithOverlap = Habit.singleSchedule(
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
        val habit = Habit.singleSchedule(
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
        val flexHabit = Habit.singleSchedule(
            id = 13L,
            title = "Pool Workout",
            cadenceType = CadenceType.WEEKLY,
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        )

        assertNull(CadenceEvaluator.rescheduleHabitDate(flexHabit, tuesday, wednesday))

        val shifted = CadenceEvaluator.rescheduleHabitDate(flexHabit, tuesday, thursday)!!
        assertFalse(CadenceEvaluator.isHabitDueOn(shifted, tuesday, completedDates = emptySet()))
        assertTrue(CadenceEvaluator.isHabitDueOn(shifted, thursday, completedDates = emptySet()))
        assertEquals(setOf(tuesday), shifted.rescheduledFromDates)
        assertEquals(setOf(thursday), shifted.rescheduledToDates)
    }

    @Test
    fun calculateBestStreak_tracksHistoricalPeakSeparatelyFromCurrentStreak() {
        val today = LocalDate.of(2026, 5, 20)
        val dailyHabit = Habit.singleSchedule(
            id = 14L,
            title = "Deep Work",
            cadenceType = CadenceType.DAILY,
            createdAtEpochDay = today.minusDays(20).toEpochDay(),
        )
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
        val flexHabit = Habit.singleSchedule(
            id = 15L,
            title = "Gym",
            cadenceType = CadenceType.WEEKLY,
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), // 2x/week
            createdAtEpochDay = monday.minusWeeks(4).toEpochDay(),
        )
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
        val dailyHabit = Habit.singleSchedule(
            id = 16L,
            title = "Read",
            cadenceType = CadenceType.DAILY,
            targetTimes = listOf(TargetTimeSlot("t1", 21, 45, true)),
        )
        assertEquals("DAILY · 09:45 PM", CadenceEvaluator.formatHabitScheduleSummary(dailyHabit))

        val weeklyHabit = Habit.singleSchedule(
            id = 17L,
            title = "Run",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            targetTimes = listOf(TargetTimeSlot("t2", 7, 0, true)),
        )
        assertEquals("WEEKLY · MON, WED, FRI · 07:00 AM", CadenceEvaluator.formatHabitScheduleSummary(weeklyHabit))

        val monthlyHabit = Habit.singleSchedule(
            id = 18L,
            title = "Budget",
            cadenceType = CadenceType.MONTHLY,
            monthlySubMode = MonthlySubMode.SPECIFIC_DATES,
            monthlyDates = setOf(1, 15),
            targetTimes = listOf(TargetTimeSlot("t3", 9, 0, true)),
        )
        assertEquals("MONTHLY · 1ST, 15TH · 09:00 AM", CadenceEvaluator.formatHabitScheduleSummary(monthlyHabit))

        val customInterval = Habit.singleSchedule(
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
        val today = LocalDate.of(2026, 5, 15)
        val monWedFri = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val slot1 = TargetTimeSlot("s1", 8, 0, true)
        val slot2 = TargetTimeSlot("s2", 20, 0, true)
        val habitA = Habit.singleSchedule(
            id = 1L,
            title = "Hydrate",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = monWedFri,
            targetTimes = listOf(slot1, slot2),
            createdAtEpochDay = today.minusDays(21).toEpochDay(),
        )
        val habitB = Habit.singleSchedule(
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
            prevFri to mapOf(1L to setOf(0, 1), 2L to setOf(0, 1)),
            wed to mapOf(1L to setOf(0)),
            thu to mapOf(1L to setOf(0)),
            today to mapOf(1L to setOf(0, 1), 2L to setOf(0)),
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
        assertEquals(MatrixIntensity.NONE, monCell.intensity)
        assertFalse(monCell.isUnscheduledEmpty)

        val tueCell = cellsByDate.getValue(tue)
        assertEquals(0, tueCell.scheduledCount)
        assertEquals(MatrixIntensity.NONE, tueCell.intensity)
        assertTrue(tueCell.isUnscheduledEmpty)

        val wedCell = cellsByDate.getValue(wed)
        assertEquals(4, wedCell.scheduledCount)
        assertEquals(1, wedCell.completedCount)
        assertEquals(0.25f, wedCell.completionRatio, 0.001f)
        assertEquals(MatrixIntensity.LOW, wedCell.intensity)

        val thuCell = cellsByDate.getValue(thu)
        assertEquals(0, thuCell.scheduledCount)
        assertEquals(1, thuCell.completedCount)
        assertEquals(MatrixIntensity.HIGH, thuCell.intensity)
        assertFalse(thuCell.isUnscheduledEmpty)

        val friCell = cellsByDate.getValue(today)
        assertEquals(4, friCell.scheduledCount)
        assertEquals(3, friCell.completedCount)
        assertEquals(MatrixIntensity.MEDIUM, friCell.intensity)

        val satCell = cellsByDate.getValue(sat)
        assertTrue(satCell.isFuture)
        assertTrue(satCell.isUnscheduledEmpty)
    }

    // =========================================================================
    // Effective-dated Non-Retroactive Schedule Edits (All 8 Cadence Types)
    // =========================================================================

    @Test
    fun futureOnlyEdit_dailyToWeekdaysPreservesPastWeekendScheduleAndStreak() {
        val wednesday = LocalDate.of(2026, 5, 13)
        val initial = Habit.singleSchedule(
            id = 20L,
            title = "Meditate",
            cadenceType = CadenceType.DAILY,
            scheduledDays = DayOfWeek.entries.toSet(),
            createdAtEpochDay = wednesday.minusDays(10).toEpochDay(),
        )
        val weekdaysOnly = initial.latestSchedule.copy(
            scheduledDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
        )
        val edited = CadenceEvaluator.applyScheduleEdit(
            existing = initial,
            updatedTitle = initial.title,
            updatedDescription = initial.description,
            updatedDomain = initial.domain,
            updatedColorHex = initial.colorHex,
            updatedSkipReminderIfCompleted = initial.skipReminderIfCompleted,
            updatedSchedule = weekdaysOnly,
            scope = HabitEditScope.FUTURE_ONLY,
            effectiveFrom = wednesday,
        )

        val pastSaturday = LocalDate.of(2026, 5, 9)
        val pastSunday = LocalDate.of(2026, 5, 10)
        val futureSaturday = LocalDate.of(2026, 5, 16)

        assertTrue(CadenceEvaluator.isHabitScheduledOn(edited, pastSaturday))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(edited, pastSunday))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(edited, futureSaturday))

        // Missed future Saturday does not break streak; completed past Sat/Sun count
        val completed = setOf(pastSaturday, pastSunday, LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 12), wednesday, LocalDate.of(2026, 5, 14), LocalDate.of(2026, 5, 15))
        assertEquals(7, CadenceEvaluator.calculateStreak(edited, completed, futureSaturday))
    }

    @Test
    fun futureOnlyEdit_weeklyFixedMidWeekChangePreservesContinuousStreak() {
        val thu = LocalDate.of(2026, 5, 14)
        val initial = Habit.singleSchedule(
            id = 21L,
            title = "Strength",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            createdAtEpochDay = LocalDate.of(2026, 5, 1).toEpochDay(),
        )
        val tueThuSat = initial.latestSchedule.copy(
            scheduledDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
        )
        val edited = CadenceEvaluator.applyScheduleEdit(
            existing = initial,
            updatedTitle = initial.title,
            updatedDescription = initial.description,
            updatedDomain = initial.domain,
            updatedColorHex = initial.colorHex,
            updatedSkipReminderIfCompleted = initial.skipReminderIfCompleted,
            updatedSchedule = tueThuSat,
            scope = HabitEditScope.FUTURE_ONLY,
            effectiveFrom = thu,
        )

        // Transition week: Mon, Wed (old) + Thu, Sat (new); Tue (old rest) and Fri (new rest) unscheduled
        assertTrue(CadenceEvaluator.isHabitScheduledOn(edited, LocalDate.of(2026, 5, 11))) // Mon
        assertFalse(CadenceEvaluator.isHabitScheduledOn(edited, LocalDate.of(2026, 5, 12))) // Tue
        assertTrue(CadenceEvaluator.isHabitScheduledOn(edited, LocalDate.of(2026, 5, 13))) // Wed
        assertTrue(CadenceEvaluator.isHabitScheduledOn(edited, thu)) // Thu
        assertFalse(CadenceEvaluator.isHabitScheduledOn(edited, LocalDate.of(2026, 5, 15))) // Fri
        assertTrue(CadenceEvaluator.isHabitScheduledOn(edited, LocalDate.of(2026, 5, 16))) // Sat

        val completed = setOf(
            LocalDate.of(2026, 5, 11),
            LocalDate.of(2026, 5, 13),
            thu,
            LocalDate.of(2026, 5, 16),
        )
        assertEquals(4, CadenceEvaluator.calculateStreak(edited, completed, LocalDate.of(2026, 5, 16)))
    }

    @Test
    fun futureOnlyEdit_flexiblePoolMidWeekRaiseUsesForgivingStreakAndImmediateDue() {
        val wed = LocalDate.of(2026, 5, 13)
        val initial = Habit.singleSchedule(
            id = 22L,
            title = "Cardio",
            cadenceType = CadenceType.WEEKLY,
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), // 3x
            createdAtEpochDay = LocalDate.of(2026, 5, 1).toEpochDay(),
        )
        val raisedToFive = initial.latestSchedule.copy(
            scheduledDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
        )
        val edited = CadenceEvaluator.applyScheduleEdit(
            existing = initial,
            updatedTitle = initial.title,
            updatedDescription = initial.description,
            updatedDomain = initial.domain,
            updatedColorHex = initial.colorHex,
            updatedSkipReminderIfCompleted = initial.skipReminderIfCompleted,
            updatedSchedule = raisedToFive,
            scope = HabitEditScope.FUTURE_ONLY,
            effectiveFrom = wed,
        )

        // Prior week: 3 completions; transition week: 3 completions (Mon, Tue, Fri); next week: 5 completions
        val completed = setOf(
            LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 6), LocalDate.of(2026, 5, 8),
            LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15),
            LocalDate.of(2026, 5, 18),
        )
        // On Thu May 14 with 3 completions so far, habit IS still due under raised 5x target
        assertTrue(
            CadenceEvaluator.isHabitDueOn(
                edited,
                LocalDate.of(2026, 5, 14),
                setOf(LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 13)),
            ),
        )
        // Forgiving transition week target min(3, 5) = 3 preserves streak across transition week into next Monday
        assertEquals(7, CadenceEvaluator.calculateStreak(edited, completed, LocalDate.of(2026, 5, 18)))
    }

    @Test
    fun futureOnlyEdit_fixedToFlexiblePoolMidWeekPreservesStreakWhenWeekTargetMetEarly() {
        // Mon..Thu fixed completed (4x), Thu evening switches FUTURE_ONLY cutoff=Fri to Flex 4x/week.
        // Dashboard hides Fri..Sun because 4 >= 4. Next Monday streak must NOT break!
        val fri = LocalDate.of(2026, 5, 15)
        val initial = Habit.singleSchedule(
            id = 23L,
            title = "Write",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
            ),
            createdAtEpochDay = LocalDate.of(2026, 5, 1).toEpochDay(),
        )
        val flexFour = initial.latestSchedule.copy(
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
            scheduledDays = setOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
            ),
        )
        val edited = CadenceEvaluator.applyScheduleEdit(
            existing = initial,
            updatedTitle = initial.title,
            updatedDescription = initial.description,
            updatedDomain = initial.domain,
            updatedColorHex = initial.colorHex,
            updatedSkipReminderIfCompleted = initial.skipReminderIfCompleted,
            updatedSchedule = flexFour,
            scope = HabitEditScope.FUTURE_ONLY,
            effectiveFrom = fri,
        )

        val completedMonToThu = setOf(
            LocalDate.of(2026, 5, 11),
            LocalDate.of(2026, 5, 12),
            LocalDate.of(2026, 5, 13),
            LocalDate.of(2026, 5, 14),
        )
        // Friday should not be due since week already has 4 completions
        assertFalse(CadenceEvaluator.isHabitDueOn(edited, fri, completedMonToThu))

        // On next Monday (after transition week ended), streak remains 4!
        val nextMon = LocalDate.of(2026, 5, 18)
        assertEquals(4, CadenceEvaluator.calculateStreak(edited, completedMonToThu, nextMon))
    }

    @Test
    fun futureOnlyEdit_multiTransitionWeekFixedToFlexToFixedEvaluatesEachRun() {
        val tue = LocalDate.of(2026, 5, 12)
        val fri = LocalDate.of(2026, 5, 15)
        val habit = Habit(
            id = 24L,
            title = "Switchy",
            createdAtEpochDay = LocalDate.of(2026, 5, 1).toEpochDay(),
            schedules = listOf(
                HabitSchedule(
                    effectiveFrom = null,
                    cadenceType = CadenceType.DAILY,
                    scheduledDays = setOf(DayOfWeek.MONDAY),
                ),
                HabitSchedule(
                    effectiveFrom = tue,
                    cadenceType = CadenceType.WEEKLY,
                    weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
                    scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
                ),
                HabitSchedule(
                    effectiveFrom = fri,
                    cadenceType = CadenceType.DAILY,
                    scheduledDays = DayOfWeek.entries.toSet(),
                ),
            ),
        )

        // Mon done, Tue-Thu Flex 2 done (passes floor(3*3/7)=1), Fri done, Sat MISSED (Fixed), Sun done
        val completed = setOf(
            LocalDate.of(2026, 5, 11), // Mon
            LocalDate.of(2026, 5, 12), // Tue
            LocalDate.of(2026, 5, 13), // Wed
            LocalDate.of(2026, 5, 15), // Fri
            // Sat May 16 missed!
            LocalDate.of(2026, 5, 17), // Sun
        )
        assertEquals(1, CadenceEvaluator.calculateStreak(habit, completed, LocalDate.of(2026, 5, 17)))
        assertEquals(4, CadenceEvaluator.calculateBestStreak(habit, completed, LocalDate.of(2026, 5, 17)))
    }

    @Test
    fun futureOnlyEdit_monthlySpecificAndOrdinalAndCustomModesSwitchCleanlyAtCutoff() {
        val may10 = LocalDate.of(2026, 5, 10)
        val monthlyHabit = Habit.singleSchedule(
            id = 25L,
            title = "Audit",
            cadenceType = CadenceType.MONTHLY,
            monthlySubMode = MonthlySubMode.SPECIFIC_DATES,
            monthlyDates = setOf(1, 31),
            endOfMonthBehavior = EndOfMonthBehavior.EXECUTE_ON_LAST_DAY,
            createdAtEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
        )
        val ordinalSwitch = monthlyHabit.latestSchedule.copy(
            monthlySubMode = MonthlySubMode.ORDINAL_DAYS,
            ordinalWeek = 3,
            ordinalDayOfWeek = DayOfWeek.FRIDAY,
        )
        val editedMonthly = CadenceEvaluator.applyScheduleEdit(
            existing = monthlyHabit,
            updatedTitle = monthlyHabit.title,
            updatedDescription = monthlyHabit.description,
            updatedDomain = monthlyHabit.domain,
            updatedColorHex = monthlyHabit.colorHex,
            updatedSkipReminderIfCompleted = monthlyHabit.skipReminderIfCompleted,
            updatedSchedule = ordinalSwitch,
            scope = HabitEditScope.FUTURE_ONLY,
            effectiveFrom = may10,
        )

        assertTrue(CadenceEvaluator.isHabitScheduledOn(editedMonthly, LocalDate.of(2026, 2, 28))) // old EOM rollback
        assertTrue(CadenceEvaluator.isHabitScheduledOn(editedMonthly, LocalDate.of(2026, 5, 1)))  // old May 1
        assertFalse(CadenceEvaluator.isHabitScheduledOn(editedMonthly, LocalDate.of(2026, 5, 31))) // replaced by ordinal
        assertTrue(CadenceEvaluator.isHabitScheduledOn(editedMonthly, LocalDate.of(2026, 5, 15)))  // new 3rd Fri of May

        // Custom Interval -> Cycle On/Off
        val intervalHabit = Habit.singleSchedule(
            id = 26L,
            title = "Cycle",
            cadenceType = CadenceType.CUSTOM,
            customArchetype = CustomCadenceArchetype.INTERVAL,
            customInterval = 2,
            customIntervalUnit = IntervalUnit.DAYS,
            anchorDate = LocalDate.of(2026, 5, 1),
        )
        val cycleNew = intervalHabit.latestSchedule.copy(
            customArchetype = CustomCadenceArchetype.CYCLE_ON_OFF,
            cycleDaysOn = 2,
            cycleDaysOff = 1,
            anchorDate = LocalDate.of(2026, 5, 10),
        )
        val editedCustom = CadenceEvaluator.applyScheduleEdit(
            existing = intervalHabit,
            updatedTitle = intervalHabit.title,
            updatedDescription = intervalHabit.description,
            updatedDomain = intervalHabit.domain,
            updatedColorHex = intervalHabit.colorHex,
            updatedSkipReminderIfCompleted = intervalHabit.skipReminderIfCompleted,
            updatedSchedule = cycleNew,
            scope = HabitEditScope.FUTURE_ONLY,
            effectiveFrom = LocalDate.of(2026, 5, 10),
        )
        assertTrue(CadenceEvaluator.isHabitScheduledOn(editedCustom, LocalDate.of(2026, 5, 5))) // old every 2 days
        assertFalse(CadenceEvaluator.isHabitScheduledOn(editedCustom, LocalDate.of(2026, 5, 6)))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(editedCustom, LocalDate.of(2026, 5, 10))) // cycle ON day 0
        assertTrue(CadenceEvaluator.isHabitScheduledOn(editedCustom, LocalDate.of(2026, 5, 11))) // cycle ON day 1
        assertFalse(CadenceEvaluator.isHabitScheduledOn(editedCustom, LocalDate.of(2026, 5, 12))) // cycle OFF day 2
    }

    @Test
    fun futureOnlyEdit_multiInstanceTargetCountChangePreservesHistoricalHeatmapAndStreak() {
        val cutoff = LocalDate.of(2026, 5, 14)
        val yesterday = cutoff.minusDays(1)
        val twoDaysAgo = cutoff.minusDays(2)

        val oneCueHabit = Habit.singleSchedule(
            id = 27L,
            title = "Hydrate",
            cadenceType = CadenceType.DAILY,
            targetPerPeriod = 1,
            targetTimes = listOf(TargetTimeSlot("s1", 8, 0, true)),
            createdAtEpochDay = twoDaysAgo.toEpochDay(),
        )
        val twoCueSchedule = oneCueHabit.latestSchedule.copy(
            targetPerPeriod = 2,
            targetTimes = listOf(
                TargetTimeSlot("s1", 8, 0, true),
                TargetTimeSlot("s2", 20, 0, true),
            ),
        )

        val futureEdited = CadenceEvaluator.applyScheduleEdit(
            existing = oneCueHabit,
            updatedTitle = oneCueHabit.title,
            updatedDescription = oneCueHabit.description,
            updatedDomain = oneCueHabit.domain,
            updatedColorHex = oneCueHabit.colorHex,
            updatedSkipReminderIfCompleted = oneCueHabit.skipReminderIfCompleted,
            updatedSchedule = twoCueSchedule,
            scope = HabitEditScope.FUTURE_ONLY,
            effectiveFrom = cutoff,
        )

        val instances = mapOf(
            twoDaysAgo to mapOf(27L to setOf(0)),
            yesterday to mapOf(27L to setOf(0)),
            cutoff to mapOf(27L to setOf(0)), // only 1 of 2 done on cutoff day
        )
        val matrixFuture = CadenceEvaluator.buildCadenceMatrix(
            habits = listOf(futureEdited),
            completionsByDate = instances.mapValues { it.value.keys },
            today = cutoff,
            weeks = 4,
            completedInstancesByDate = instances,
        )
        val cellsFuture = matrixFuture.columns.flatten().associateBy { it.date }
        assertEquals(1f, cellsFuture.getValue(yesterday).completionRatio, 0.001f)
        assertEquals(1, cellsFuture.getValue(yesterday).scheduledCount)
        assertEquals(0.5f, cellsFuture.getValue(cutoff).completionRatio, 0.001f)
        assertEquals(2, cellsFuture.getValue(cutoff).scheduledCount)

        // All occurrences retroactively turns yesterday into 50%
        val allEdited = CadenceEvaluator.applyScheduleEdit(
            existing = oneCueHabit,
            updatedTitle = oneCueHabit.title,
            updatedDescription = oneCueHabit.description,
            updatedDomain = oneCueHabit.domain,
            updatedColorHex = oneCueHabit.colorHex,
            updatedSkipReminderIfCompleted = oneCueHabit.skipReminderIfCompleted,
            updatedSchedule = twoCueSchedule,
            scope = HabitEditScope.ALL_OCCURRENCES,
            effectiveFrom = cutoff,
        )
        val matrixAll = CadenceEvaluator.buildCadenceMatrix(
            habits = listOf(allEdited),
            completionsByDate = instances.mapValues { it.value.keys },
            today = cutoff,
            weeks = 4,
            completedInstancesByDate = instances,
        )
        val cellsAll = matrixAll.columns.flatten().associateBy { it.date }
        assertEquals(0.5f, cellsAll.getValue(yesterday).completionRatio, 0.001f)
        assertEquals(2, cellsAll.getValue(yesterday).scheduledCount)
    }

    @Test
    fun futureOnlyEdit_sameCutoffReplacesAndRevertingCoalescesCleanly() {
        val today = LocalDate.of(2026, 5, 15)
        val initial = Habit.singleSchedule(
            id = 28L,
            title = "Focus",
            cadenceType = CadenceType.DAILY,
            scheduledDays = DayOfWeek.entries.toSet(),
        )
        val weekdays = initial.latestSchedule.copy(
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
        )
        val weekends = initial.latestSchedule.copy(
            scheduledDays = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        )

        val firstEdit = CadenceEvaluator.applyScheduleEdit(
            initial, initial.title, "", "", initial.colorHex, true, weekdays, HabitEditScope.FUTURE_ONLY, today,
        )
        assertEquals(2, firstEdit.schedules.size)

        // Re-edit same cutoff replaces in place (still 2 versions)
        val secondEdit = CadenceEvaluator.applyScheduleEdit(
            firstEdit, initial.title, "", "", initial.colorHex, true, weekends, HabitEditScope.FUTURE_ONLY, today,
        )
        assertEquals(2, secondEdit.schedules.size)
        assertEquals(weekends.scheduledDays, secondEdit.latestSchedule.scheduledDays)

        // Reverting back to baseline rule on same cutoff coalesces back to 1 schedule
        val revertedEdit = CadenceEvaluator.applyScheduleEdit(
            secondEdit, initial.title, "", "", initial.colorHex, true, initial.latestSchedule, HabitEditScope.FUTURE_ONLY, today,
        )
        assertEquals(1, revertedEdit.schedules.size)
        assertNull(revertedEdit.schedules.single().effectiveFrom)
    }

    @Test
    fun futureOnlyEdit_preservesPastReschedulesAndClearsFutureReschedulesOnCadenceChange() {
        val pastMon = LocalDate.of(2026, 5, 4)
        val pastTue = LocalDate.of(2026, 5, 5)
        val cutoff = LocalDate.of(2026, 5, 11)
        val futureFri = LocalDate.of(2026, 5, 15)
        val futureSat = LocalDate.of(2026, 5, 16)

        val habit = Habit.singleSchedule(
            id = 29L,
            title = "Run",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            rescheduledFromDates = setOf(pastMon, futureFri),
            rescheduledToDates = setOf(pastTue, futureSat),
        )

        val edited = CadenceEvaluator.applyScheduleEdit(
            existing = habit,
            updatedTitle = habit.title,
            updatedDescription = "",
            updatedDomain = "",
            updatedColorHex = habit.colorHex,
            updatedSkipReminderIfCompleted = true,
            updatedSchedule = habit.latestSchedule.copy(scheduledDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)),
            scope = HabitEditScope.FUTURE_ONLY,
            effectiveFrom = cutoff,
        )

        assertEquals(setOf(pastMon), edited.rescheduledFromDates)
        assertEquals(setOf(pastTue), edited.rescheduledToDates)
    }

    // =========================================================================
    // Pixel-Clock Pausing, Truncation, Proration & Heatmap Markers
    // =========================================================================

    @Test
    fun pauseWindow_protectsStreakPreservesCustomRhythmAndBlocksRescheduleTarget() {
        val anchor = LocalDate.of(2026, 5, 1)
        val pauseStart = LocalDate.of(2026, 5, 5)
        val pauseEnd = LocalDate.of(2026, 5, 9)

        val intervalHabit = Habit.singleSchedule(
            id = 30L,
            title = "Sauna",
            cadenceType = CadenceType.CUSTOM,
            customArchetype = CustomCadenceArchetype.INTERVAL,
            customInterval = 2,
            customIntervalUnit = IntervalUnit.DAYS,
            anchorDate = anchor,
            createdAtEpochDay = anchor.toEpochDay(),
            pauseWindows = listOf(HabitPauseWindow(1L, pauseStart, pauseEnd)),
        )

        // May 1, 3 active; May 5, 7, 9 skipped inside pause; May 11 resumes on original calendar rhythm
        assertTrue(CadenceEvaluator.isHabitScheduledOn(intervalHabit, LocalDate.of(2026, 5, 3)))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(intervalHabit, LocalDate.of(2026, 5, 5)))
        assertFalse(CadenceEvaluator.isHabitScheduledOn(intervalHabit, LocalDate.of(2026, 5, 7)))
        assertTrue(CadenceEvaluator.isHabitScheduledOn(intervalHabit, LocalDate.of(2026, 5, 11)))

        val completed = setOf(
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 3),
            LocalDate.of(2026, 5, 11),
        )
        assertEquals(3, CadenceEvaluator.calculateStreak(intervalHabit, completed, LocalDate.of(2026, 5, 11)))

        // Cannot reschedule onto a paused date
        assertNull(
            CadenceEvaluator.rescheduleHabitDate(
                intervalHabit,
                fromDate = LocalDate.of(2026, 5, 11),
                toDate = LocalDate.of(2026, 5, 6),
            ),
        )

        // Heatmap sets isPausedEmpty=true and isUnscheduledEmpty=false on paused empty days
        val matrix = CadenceEvaluator.buildCadenceMatrix(
            habits = listOf(intervalHabit),
            completionsByDate = emptyMap(),
            today = LocalDate.of(2026, 5, 11),
            weeks = 3,
        )
        val cellsByDate = matrix.columns.flatten().associateBy { it.date }
        val pausedCell = cellsByDate.getValue(LocalDate.of(2026, 5, 7))
        assertTrue(pausedCell.isPausedEmpty)
        assertFalse(pausedCell.isUnscheduledEmpty)
    }

    @Test
    fun pauseWindow_earlyResumeTruncatesBeforeTodayOrDeletesTodayStart() {
        val today = LocalDate.of(2026, 5, 15)
        val startedEarlier = listOf(HabitPauseWindow(1L, today.minusDays(3), today.plusDays(3)))
        val truncated = CadenceEvaluator.resumeOrCancelPause(startedEarlier, today)
        assertEquals(1, truncated.size)
        assertEquals(today.minusDays(3), truncated.single().startDate)
        assertEquals(today.minusDays(1), truncated.single().endDate)

        val startedToday = listOf(HabitPauseWindow(2L, today, today.plusDays(2)))
        val deleted = CadenceEvaluator.resumeOrCancelPause(startedToday, today)
        assertTrue(deleted.isEmpty())

        val futureOnly = listOf(HabitPauseWindow(3L, today.plusDays(2), today.plusDays(5)))
        val cancelled = CadenceEvaluator.resumeOrCancelPause(futureOnly, today)
        assertTrue(cancelled.isEmpty())
    }

    @Test
    fun pauseWindow_flexiblePoolProratesPartialWeekQuotaAndProtectsStreak() {
        val monday = LocalDate.of(2026, 5, 11)
        val flexHabit = Habit.singleSchedule(
            id = 31L,
            title = "Gym",
            cadenceType = CadenceType.WEEKLY,
            weeklyTargetMode = WeeklyTargetMode.FLEXIBLE_POOL,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), // 3x/week
            createdAtEpochDay = monday.minusWeeks(2).toEpochDay(),
            pauseWindows = listOf(
                HabitPauseWindow(
                    id = 1L,
                    startDate = LocalDate.of(2026, 5, 14), // Thu..Sun (4 days paused -> 3 active days)
                    endDate = LocalDate.of(2026, 5, 17),
                ),
            ),
        )

        // 3 active days / 7 * 3 = 1.28 -> Dashboard due target = 1, streak min = 1
        assertEquals(1, CadenceEvaluator.flexiblePoolDueTargetForWeek(flexHabit, monday, monday))
        assertTrue(CadenceEvaluator.isHabitDueOn(flexHabit, LocalDate.of(2026, 5, 12), emptySet()))
        // Once 1 completed in that partially paused week, no longer due on Wednesday
        assertFalse(CadenceEvaluator.isHabitDueOn(flexHabit, LocalDate.of(2026, 5, 13), setOf(monday)))

        // Streak survives from previous full week (3x) through partial paused week (1x) into next week (1x)
        val completed = setOf(
            monday.minusWeeks(1), monday.minusWeeks(1).plusDays(2), monday.minusWeeks(1).plusDays(4),
            monday,
            monday.plusWeeks(1),
        )
        assertEquals(5, CadenceEvaluator.calculateStreak(flexHabit, completed, monday.plusWeeks(1)))
    }
}
