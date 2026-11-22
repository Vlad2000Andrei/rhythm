package com.rhythm.habittracker.ui.dashboard

import com.rhythm.habittracker.data.DashboardPreferences
import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.data.RescheduleOutcome
import com.rhythm.habittracker.data.RescheduleUndoToken
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.HabitPauseWindow
import com.rhythm.habittracker.domain.model.HabitSchedule
import com.rhythm.habittracker.domain.model.HabitSortMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = DashboardMainDispatcherRule()

    @Test
    fun dashboard_splitsPendingAndCompletedAndUpdatesQuotaOnToggle() = runTest {
        val today = LocalDate.of(2026, 5, 15)
        val fakeRepo = FakeDashboardRepository(
            initialHabits = listOf(
                Habit.singleSchedule(
                    id = 1L,
                    title = "Deep Work",
                    domain = "Focus",
                    colorHex = HabitAccentColor.ROYAL_BLUE.hex,
                    cadenceType = CadenceType.DAILY,
                    sortOrder = 0,
                    createdAtEpochDay = today.minusDays(30).toEpochDay(),
                ),
                Habit.singleSchedule(
                    id = 2L,
                    title = "Hydrate",
                    domain = "Health",
                    colorHex = HabitAccentColor.EMERALD_GREEN.hex,
                    cadenceType = CadenceType.DAILY,
                    sortOrder = 1,
                    createdAtEpochDay = today.minusDays(30).toEpochDay(),
                ),
            ),
            initialCompletions = mapOf(
                today to setOf(2L),
            ),
        )

        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initial = viewModel.uiState.value
        assertFalse(initial.isLoading)
        assertEquals(1, initial.completedCount)
        assertEquals(2, initial.totalCount)
        assertEquals(listOf("Deep Work"), initial.pendingHabits.map { it.habit.title })
        assertEquals(listOf("Hydrate"), initial.completedHabits.map { it.habit.title })

        viewModel.toggleHabitCompletion(1L)
        advanceUntilIdle()

        val afterToggle = viewModel.uiState.value
        assertEquals(2, afterToggle.completedCount)
        assertTrue(afterToggle.pendingHabits.isEmpty())
        assertEquals(2, afterToggle.completedHabits.size)
    }

    @Test
    fun dashboard_domainFilterAndSortModeWorkAccurately() = runTest {
        val today = LocalDate.of(2026, 5, 15)
        val fakeRepo = FakeDashboardRepository(
            initialHabits = listOf(
                Habit.singleSchedule(
                    id = 1L,
                    title = "Deep Work",
                    domain = "Focus",
                    sortOrder = 0,
                    createdAtEpochDay = today.minusDays(10).toEpochDay(),
                ),
                Habit.singleSchedule(
                    id = 2L,
                    title = "Evening Reflection",
                    domain = "Mind",
                    sortOrder = 1,
                    createdAtEpochDay = today.minusDays(10).toEpochDay(),
                ),
            ),
            initialCompletions = mapOf(
                today.minusDays(1) to setOf(2L),
                today.minusDays(2) to setOf(2L),
            ),
        )

        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.setSortMode(HabitSortMode.STREAK)
        advanceUntilIdle()
        assertEquals("Evening Reflection", viewModel.uiState.value.pendingHabits.first().habit.title)
        assertEquals(HabitSortMode.STREAK, fakeRepo.savedPrefs.value.sortMode)

        viewModel.selectDomain("Focus")
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.pendingHabits.size)
        assertEquals("Deep Work", viewModel.uiState.value.pendingHabits.first().habit.title)
        assertEquals("Focus", fakeRepo.savedPrefs.value.selectedDomain)

        viewModel.selectDomain("All Habits")
        viewModel.selectDate(today.minusDays(1))
        advanceUntilIdle()
        assertEquals(today.minusDays(1), viewModel.uiState.value.selectedDate)
        assertEquals(1, viewModel.uiState.value.completedCount)
    }

    @Test
    fun dashboard_showsAllInstancesForMultiInstanceHabitAndTracksPartialCompletion() = runTest {
        val today = LocalDate.of(2026, 5, 15)
        val multiHabit = Habit.singleSchedule(
            id = 10L,
            title = "Brush Teeth",
            domain = "Health",
            colorHex = HabitAccentColor.EMERALD_GREEN.hex,
            cadenceType = CadenceType.DAILY,
            targetPerPeriod = 2,
            targetTimes = listOf(
                TargetTimeSlot(id = "morning", hour = 8, minute = 0, enabled = true),
                TargetTimeSlot(id = "evening", hour = 21, minute = 0, enabled = true),
            ),
            sortOrder = 0,
            createdAtEpochDay = today.minusDays(10).toEpochDay(),
        )
        val fakeRepo = FakeDashboardRepository(
            initialHabits = listOf(multiHabit),
            initialCompletions = emptyMap(),
        )

        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val initial = viewModel.uiState.value
        assertEquals(0, initial.completedCount)
        assertEquals(2, initial.totalCount)
        assertEquals(2, initial.pendingHabits.size)
        assertEquals(0, initial.completedHabits.size)
        assertEquals(listOf(0, 1), initial.pendingHabits.map { it.instanceIndex })
        assertEquals(listOf(2, 2), initial.pendingHabits.map { it.totalInstances })

        val morningBucket = initial.bucketProgress.first { it.bucket == TimeOfDayBucket.MORNING }
        val eveningBucket = initial.bucketProgress.first { it.bucket == TimeOfDayBucket.EVENING }
        assertEquals(0, morningBucket.completed)
        assertEquals(1, morningBucket.total)
        assertEquals(0, eveningBucket.completed)
        assertEquals(1, eveningBucket.total)

        viewModel.toggleHabitCompletion(habitId = 10L, instanceIndex = 0)
        advanceUntilIdle()

        val afterFirst = viewModel.uiState.value
        assertEquals(1, afterFirst.completedCount)
        assertEquals(2, afterFirst.totalCount)
        assertEquals(1, afterFirst.pendingHabits.size)
        assertEquals(1, afterFirst.pendingHabits.single().instanceIndex)
        assertEquals(1, afterFirst.completedHabits.size)
        assertEquals(0, afterFirst.completedHabits.single().instanceIndex)
        assertEquals(1, afterFirst.bucketProgress.first { it.bucket == TimeOfDayBucket.MORNING }.completed)
        assertEquals(0, afterFirst.bucketProgress.first { it.bucket == TimeOfDayBucket.EVENING }.completed)

        viewModel.toggleHabitCompletion(habitId = 10L, instanceIndex = 1)
        advanceUntilIdle()

        val afterSecond = viewModel.uiState.value
        assertEquals(2, afterSecond.completedCount)
        assertEquals(2, afterSecond.totalCount)
        assertTrue(afterSecond.pendingHabits.isEmpty())
        assertEquals(2, afterSecond.completedHabits.size)
        assertEquals(1, afterSecond.StreakForHabit(10L))
    }

    @Test
    fun dashboard_rescheduleMovesTaskToUnscheduledDayAndBlocksScheduledConflicts() = runTest {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val wednesday = LocalDate.of(2026, 5, 13)
        val mwfHabit = Habit.singleSchedule(
            id = 20L,
            title = "Tempo Run",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            createdAtEpochDay = monday.minusDays(14).toEpochDay(),
        )
        val fakeRepo = FakeDashboardRepository(
            initialHabits = listOf(mwfHabit),
            initialCompletions = emptyMap(),
        )
        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            clockTodayProvider = { monday },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertEquals(listOf("Tempo Run"), viewModel.uiState.value.pendingHabits.map { it.habit.title })

        viewModel.rescheduleHabit(20L, monday, wednesday)
        advanceUntilIdle()
        assertEquals(listOf("Tempo Run"), viewModel.uiState.value.pendingHabits.map { it.habit.title })

        viewModel.rescheduleHabit(20L, monday, tuesday)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.pendingHabits.isEmpty())

        viewModel.selectDate(tuesday)
        advanceUntilIdle()
        assertEquals(listOf("Tempo Run"), viewModel.uiState.value.pendingHabits.map { it.habit.title })
    }

    @Test
    fun dashboard_rescheduleClearsCompletionsAndUndoRestoresHabitAndInstances() = runTest {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val mwfStretch = Habit.singleSchedule(
            id = 21L,
            title = "Stretch",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            targetTimes = listOf(
                TargetTimeSlot(id = "morning", hour = 8, minute = 0, enabled = true),
                TargetTimeSlot(id = "evening", hour = 20, minute = 0, enabled = true),
            ),
            createdAtEpochDay = monday.minusDays(7).toEpochDay(),
        )
        val fakeRepo = FakeDashboardRepository(
            initialHabits = listOf(mwfStretch),
            initialCompletions = emptyMap(),
        )
        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            clockTodayProvider = { monday },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.toggleHabitCompletion(21L, 0, monday)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.completedHabits.size)
        assertEquals(1, viewModel.uiState.value.pendingHabits.size)

        viewModel.rescheduleHabit(21L, monday, tuesday)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.pendingHabits.isEmpty())
        assertTrue(viewModel.uiState.value.completedHabits.isEmpty())
        assertTrue(fakeRepo.getCompletedInstancesOn(21L, monday).isEmpty())
        val banner = viewModel.uiState.value.rescheduleBanner
        assertTrue(banner != null)
        assertEquals("Stretch", banner!!.habitTitle)

        viewModel.selectDate(tuesday)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.pendingHabits.size)
        assertTrue(viewModel.uiState.value.completedHabits.isEmpty())

        viewModel.undoLastReschedule()
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.rescheduleBanner)
        assertTrue(viewModel.uiState.value.pendingHabits.isEmpty())

        viewModel.selectDate(monday)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.completedHabits.size)
        assertEquals(1, viewModel.uiState.value.pendingHabits.size)
        assertEquals(setOf(0), fakeRepo.getCompletedInstancesOn(21L, monday))
    }

    @Test
    fun dashboard_toggleHabitCompletionOnRescheduledFromDateIsIgnored() = runTest {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val habit = Habit.singleSchedule(
            id = 22L,
            title = "Focus Block",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            rescheduledFromDates = setOf(monday),
            rescheduledToDates = setOf(tuesday),
            createdAtEpochDay = monday.minusDays(7).toEpochDay(),
        )
        val fakeRepo = FakeDashboardRepository(
            initialHabits = listOf(habit),
            initialCompletions = emptyMap(),
        )
        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            clockTodayProvider = { monday },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.toggleHabitCompletion(22L, 0, monday)
        advanceUntilIdle()
        assertTrue(fakeRepo.getCompletedInstancesOn(22L, monday).isEmpty())
    }

    @Test
    fun dashboard_populatesTimeSlotOnAllCardsAnd32WeekMatrixEndingCurrentWeek() = runTest {
        val today = LocalDate.of(2026, 5, 15)
        val singleSlotHabit = Habit.singleSchedule(
            id = 31L,
            title = "Morning Journal",
            cadenceType = CadenceType.DAILY,
            targetTimes = listOf(TargetTimeSlot("s1", 7, 15, true)),
            sortOrder = 0,
            createdAtEpochDay = today.minusDays(5).toEpochDay(),
        )
        val multiSlotHabit = Habit.singleSchedule(
            id = 32L,
            title = "Hydrate",
            cadenceType = CadenceType.DAILY,
            targetTimes = listOf(
                TargetTimeSlot("s2", 9, 0, true),
                TargetTimeSlot("s3", 14, 30, true),
            ),
            sortOrder = 1,
            createdAtEpochDay = today.minusDays(5).toEpochDay(),
        )
        val completedSingleHabit = Habit.singleSchedule(
            id = 33L,
            title = "Read",
            cadenceType = CadenceType.DAILY,
            targetTimes = listOf(TargetTimeSlot("s4", 21, 0, true)),
            sortOrder = 2,
            createdAtEpochDay = today.minusDays(5).toEpochDay(),
        )

        val fakeRepo = FakeDashboardRepository(
            initialHabits = listOf(singleSlotHabit, multiSlotHabit, completedSingleHabit),
            initialCompletions = mapOf(today to setOf(33L)),
        )
        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val pendingSingle = state.pendingHabits.first { it.habit.id == 31L }
        assertEquals(1, pendingSingle.totalInstances)
        assertEquals(TargetTimeSlot("s1", 7, 15, true), pendingSingle.timeSlot)

        val pendingMulti = state.pendingHabits.filter { it.habit.id == 32L }
        assertEquals(2, pendingMulti.size)
        assertEquals("09:00 AM", pendingMulti[0].timeSlot?.formatted())
        assertEquals("02:30 PM", pendingMulti[1].timeSlot?.formatted())

        val completedSingle = state.completedHabits.first { it.habit.id == 33L }
        assertEquals(1, completedSingle.totalInstances)
        assertEquals("09:00 PM", completedSingle.timeSlot?.formatted())

        val matrix = state.cadenceMatrix!!
        assertEquals(32, matrix.columns.size)
        assertTrue(matrix.columns.all { it.size == 7 })
        val lastColumnDates = matrix.columns.last().map { it.date }
        assertEquals(LocalDate.of(2026, 5, 11), lastColumnDates.first())
        assertEquals(LocalDate.of(2026, 5, 17), lastColumnDates.last())
    }

    @Test
    fun dashboard_usesHistoricalScheduleForPastDateAndHandlesPausedWithoutDuplication() = runTest {
        val today = LocalDate.of(2026, 5, 15)
        val yesterday = today.minusDays(1)
        val versionedHabit = Habit(
            id = 40L,
            title = "Water",
            createdAtEpochDay = today.minusDays(10).toEpochDay(),
            schedules = listOf(
                HabitSchedule(
                    effectiveFrom = null,
                    cadenceType = CadenceType.DAILY,
                    targetPerPeriod = 1,
                    targetTimes = listOf(TargetTimeSlot("m", 8, 0, true)),
                    timeOfDayBucket = TimeOfDayBucket.MORNING,
                ),
                HabitSchedule(
                    effectiveFrom = today,
                    cadenceType = CadenceType.DAILY,
                    targetPerPeriod = 2,
                    targetTimes = listOf(
                        TargetTimeSlot("m", 8, 0, true),
                        TargetTimeSlot("e", 20, 0, true),
                    ),
                    timeOfDayBucket = TimeOfDayBucket.MORNING,
                ),
            ),
            pauseWindows = listOf(HabitPauseWindow(1L, today, today.plusDays(2))),
        )
        // Instance 0 completed today BEFORE pausing today
        val fakeRepo = FakeDashboardRepository(
            initialHabits = listOf(versionedHabit),
            initialCompletions = mapOf(
                yesterday to setOf(40L),
                today to setOf(40L),
            ),
        )
        val viewModel = DashboardViewModel(
            repository = fakeRepo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        // Today is paused with 1 completed instance (0): ONLY completed instance 0 appears, NO pending instance 1, NO duplicate in pausedHabits!
        val todayState = viewModel.uiState.value
        assertTrue(todayState.pendingHabits.isEmpty())
        assertEquals(1, todayState.completedHabits.size)
        assertEquals(0, todayState.completedHabits.single().instanceIndex)
        assertTrue(todayState.pausedHabits.isEmpty())

        // Uncompleting instance 0 on paused today succeeds and moves habit cleanly to pausedHabits
        viewModel.toggleHabitCompletion(40L, 0, today)
        advanceUntilIdle()
        val afterUncheck = viewModel.uiState.value
        assertTrue(afterUncheck.completedHabits.isEmpty())
        assertEquals(1, afterUncheck.pausedHabits.size)

        // Attempting to check on paused date is ignored
        viewModel.toggleHabitCompletion(40L, 0, today)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.completedHabits.isEmpty())

        // Viewing yesterday shows historical 1x schedule (Morning bucket, 1 card)
        viewModel.selectDate(yesterday)
        advanceUntilIdle()
        val yesterdayState = viewModel.uiState.value
        assertEquals(1, yesterdayState.completedHabits.size)
        assertEquals(1, yesterdayState.completedHabits.single().totalInstances)
        assertEquals(TimeOfDayBucket.MORNING, yesterdayState.completedHabits.single().bucket)
    }

    private fun DashboardUiState.StreakForHabit(habitId: Long): Int =
        (pendingHabits + completedHabits).first { it.habit.id == habitId }.streakDays
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeDashboardRepository(
    initialHabits: List<Habit>,
    initialCompletions: Map<LocalDate, Set<Long>>,
) : HabitRepository {
    private val habitsFlow = MutableStateFlow(initialHabits)
    private val completedInstancesFlow = MutableStateFlow<Map<LocalDate, Map<Long, Set<Int>>>>(
        initialCompletions.mapValues { (_, ids) -> ids.associateWith { setOf(0) } },
    )
    private val domainsFlow = MutableStateFlow<List<String>>(emptyList())
    val savedPrefs = MutableStateFlow(DashboardPreferences())

    override fun observeHabits(): Flow<List<Habit>> = habitsFlow
    override fun observeCompletionsByDate(): Flow<Map<LocalDate, Set<Long>>> =
        MutableStateFlow(emptyMap())
    override fun observeCompletedInstancesByDate(): Flow<Map<LocalDate, Map<Long, Set<Int>>>> =
        completedInstancesFlow
    override fun observeCustomDomains(): Flow<List<String>> = domainsFlow
    override fun observeDashboardPreferences(): Flow<DashboardPreferences> = savedPrefs
    override suspend fun getHabitById(habitId: Long): Habit? = habitsFlow.value.firstOrNull { it.id == habitId }
    override suspend fun saveHabit(habit: Habit): Long = habit.id
    override suspend fun deleteHabit(habitId: Long) {
        habitsFlow.value = habitsFlow.value.filterNot { it.id == habitId }
    }

    override suspend fun toggleCompletion(habitId: Long, date: LocalDate, instanceIndex: Int) {
        val currentByDate = completedInstancesFlow.value.toMutableMap()
        val currentForDate = currentByDate[date].orEmpty().toMutableMap()
        val currentInstances = currentForDate[habitId].orEmpty()
        val updatedInstances = if (instanceIndex in currentInstances) {
            currentInstances - instanceIndex
        } else {
            currentInstances + instanceIndex
        }
        if (updatedInstances.isEmpty()) {
            currentForDate.remove(habitId)
        } else {
            currentForDate[habitId] = updatedInstances
        }
        currentByDate[date] = currentForDate
        completedInstancesFlow.value = currentByDate
    }

    override suspend fun isCompletedOn(habitId: Long, date: LocalDate): Boolean =
        completedInstancesFlow.value[date]?.get(habitId).orEmpty().isNotEmpty()

    override suspend fun getCompletedInstancesOn(habitId: Long, date: LocalDate): Set<Int> =
        completedInstancesFlow.value[date]?.get(habitId).orEmpty()

    override suspend fun addCustomDomain(name: String) {
        domainsFlow.value = (domainsFlow.value + name).distinct()
    }

    override suspend fun saveDashboardPreferences(selectedDomain: String, sortMode: HabitSortMode) {
        savedPrefs.value = DashboardPreferences(selectedDomain, sortMode)
    }

    override suspend fun rescheduleHabitWithUndo(
        habitId: Long,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): RescheduleOutcome? {
        val current = getHabitById(habitId) ?: return null
        val updated = CadenceEvaluator.rescheduleHabitDate(current, fromDate, toDate) ?: return null
        val currentByDate = completedInstancesFlow.value.toMutableMap()
        val removedFrom = currentByDate[fromDate]?.get(habitId).orEmpty()

        for (date in listOf(fromDate, toDate)) {
            val mapForDate = currentByDate[date]?.toMutableMap() ?: continue
            mapForDate.remove(habitId)
            if (mapForDate.isEmpty()) currentByDate.remove(date) else currentByDate[date] = mapForDate
        }
        completedInstancesFlow.value = currentByDate
        habitsFlow.value = habitsFlow.value.map { if (it.id == habitId) updated else it }
        return RescheduleOutcome(
            updatedHabit = updated,
            undoToken = RescheduleUndoToken(
                habitBefore = current,
                removedCompletionsOnFromDate = removedFrom,
                fromDate = fromDate,
                toDate = toDate,
            ),
        )
    }

    override suspend fun undoReschedule(token: RescheduleUndoToken): Habit? {
        val currentByDate = completedInstancesFlow.value.toMutableMap()
        val toMap = currentByDate[token.toDate]?.toMutableMap()
        if (toMap != null) {
            toMap.remove(token.habitBefore.id)
            if (toMap.isEmpty()) currentByDate.remove(token.toDate) else currentByDate[token.toDate] = toMap
        }
        if (token.removedCompletionsOnFromDate.isNotEmpty()) {
            val fromMap = currentByDate[token.fromDate].orEmpty().toMutableMap()
            fromMap[token.habitBefore.id] = token.removedCompletionsOnFromDate
            currentByDate[token.fromDate] = fromMap
        }
        completedInstancesFlow.value = currentByDate
        habitsFlow.value = habitsFlow.value.map {
            if (it.id == token.habitBefore.id) token.habitBefore else it
        }
        return token.habitBefore
    }

    override suspend fun rescheduleHabit(habitId: Long, fromDate: LocalDate, toDate: LocalDate): Boolean =
        rescheduleHabitWithUndo(habitId, fromDate, toDate) != null
}
