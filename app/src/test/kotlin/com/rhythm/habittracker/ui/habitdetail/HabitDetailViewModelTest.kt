package com.rhythm.habittracker.ui.habitdetail

import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.TargetTimeSlot
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = HabitDetailMainDispatcherRule()

    @Test
    fun habitDetail_computesStreaksRate20WeekMatrixAndThisWeekRun() = runTest {
        // Friday 2026-05-15 (week started Monday 2026-05-11)
        val today = LocalDate.of(2026, 5, 15)
        val habit = Habit(
            id = 42L,
            title = "Deep Work",
            description = "Phone in drawer, terminal open.",
            domain = "Focus",
            subTag = "Block 01",
            colorHex = HabitAccentColor.ROYAL_BLUE.hex,
            cadenceType = CadenceType.DAILY,
            targetTimes = listOf(TargetTimeSlot("t1", 8, 30, true)),
            skipReminderIfCompleted = true,
            createdAtEpochDay = today.minusDays(9).toEpochDay(), // 10 days inclusive (May 6 .. May 15)
        )

        // Prior streak: May 6, 7, 8 (3 days); skipped May 9; completed May 11..15 (5 days this week) -> 8/10 = 80%
        val completedDates = listOf(
            today.minusDays(9), // May 6
            today.minusDays(8), // May 7
            today.minusDays(7), // May 8
            // skipped May 9 and May 10
            LocalDate.of(2026, 5, 11), // Mon
            LocalDate.of(2026, 5, 12), // Tue
            LocalDate.of(2026, 5, 13), // Wed
            LocalDate.of(2026, 5, 14), // Thu
            LocalDate.of(2026, 5, 15), // Fri (today)
        )
        val instancesMap = completedDates.associateWith { mapOf(42L to setOf(0)) }

        val repo = FakeHabitDetailRepository(
            habits = listOf(habit),
            completedInstances = instancesMap,
        )
        val viewModel = HabitDetailViewModel(
            habitId = 42L,
            repository = repo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.habit)
        assertEquals(5, state.currentStreak)
        assertEquals(5, state.bestStreak)
        assertEquals(8, state.completedScheduledDays)
        assertEquals(10, state.totalScheduledDays)
        assertEquals(80, state.completionRatePercent)
        assertEquals(2, state.recentSkips)
        assertEquals(20, state.matrixColumns.size)
        assertTrue(state.matrixColumns.all { it.size == 7 })
        assertEquals(5, state.completedThisWeek)
        assertEquals(7, state.scheduledThisWeek)
        assertEquals(1, state.recentWeekDays.count { it.isToday })
        assertEquals(5, state.recentWeekDays.count { it.isCompleted })
    }

    @Test
    fun habitDetail_emitsNullHabitWhenDeletedOrMissing() = runTest {
        val today = LocalDate.of(2026, 5, 15)
        val repo = FakeHabitDetailRepository(
            habits = listOf(
                Habit(id = 1L, title = "Hydrate", createdAtEpochDay = today.toEpochDay()),
            ),
            completedInstances = emptyMap(),
        )
        val viewModel = HabitDetailViewModel(
            habitId = 1L,
            repository = repo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.habit)

        repo.deleteHabit(1L)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.habit)
    }

    @Test
    fun habitDetail_matrixScalesRelativeToDailyTargetAndMarksUnscheduledDays() = runTest {
        // Friday 2026-05-15 (week Mon May 11 .. Sun May 17)
        val today = LocalDate.of(2026, 5, 15)
        val mon = LocalDate.of(2026, 5, 11)
        val tue = LocalDate.of(2026, 5, 12)
        val wed = LocalDate.of(2026, 5, 13)
        val thu = LocalDate.of(2026, 5, 14)
        val sat = LocalDate.of(2026, 5, 16)

        val habit = Habit(
            id = 42L,
            title = "Hydrate",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            targetTimes = listOf(
                TargetTimeSlot("t1", 9, 0, true),
                TargetTimeSlot("t2", 18, 0, true),
            ),
            createdAtEpochDay = today.minusDays(14).toEpochDay(),
        )
        val unrelatedHabit = Habit(
            id = 999L,
            title = "Read",
            cadenceType = CadenceType.DAILY,
            createdAtEpochDay = today.minusDays(14).toEpochDay(),
        )

        val instancesMap = mapOf(
            // Mon omitted -> 0/2 completed (scheduled, empty, NO X)
            tue to mapOf(999L to setOf(0)), // only unrelated habit completed on unscheduled day
            wed to mapOf(42L to setOf(0)), // 1/2 completed -> 0.5f
            today to mapOf(42L to setOf(0, 1)), // 2/2 completed -> 1.0f
        )

        val repo = FakeHabitDetailRepository(
            habits = listOf(habit, unrelatedHabit),
            completedInstances = instancesMap,
        )
        val viewModel = HabitDetailViewModel(
            habitId = 42L,
            repository = repo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val cellsByDate = viewModel.uiState.value.matrixColumns.flatten().associateBy { it.date }

        val monCell = cellsByDate.getValue(mon)
        assertEquals(2, monCell.scheduledCount)
        assertEquals(0, monCell.completedCount)
        assertEquals(0f, monCell.completionRatio, 0.001f)
        assertFalse(monCell.isUnscheduledEmpty)

        val tueCell = cellsByDate.getValue(tue)
        assertEquals(0, tueCell.scheduledCount)
        assertEquals(0, tueCell.completedCount)
        assertEquals(0f, tueCell.completionRatio, 0.001f)
        assertTrue(tueCell.isUnscheduledEmpty)

        val wedCell = cellsByDate.getValue(wed)
        assertEquals(2, wedCell.scheduledCount)
        assertEquals(1, wedCell.completedCount)
        assertEquals(0.5f, wedCell.completionRatio, 0.001f)
        assertFalse(wedCell.isUnscheduledEmpty)

        val thuCell = cellsByDate.getValue(thu)
        assertEquals(0, thuCell.scheduledCount)
        assertTrue(thuCell.isUnscheduledEmpty)

        val friCell = cellsByDate.getValue(today)
        assertEquals(2, friCell.scheduledCount)
        assertEquals(2, friCell.completedCount)
        assertEquals(1f, friCell.completionRatio, 0.001f)
        assertFalse(friCell.isUnscheduledEmpty)
        assertTrue(friCell.isToday)

        val satCell = cellsByDate.getValue(sat)
        assertTrue(satCell.isFuture)
        assertTrue(satCell.isUnscheduledEmpty)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeHabitDetailRepository(
    habits: List<Habit>,
    completedInstances: Map<LocalDate, Map<Long, Set<Int>>> = emptyMap(),
) : HabitRepository {
    private val habitsFlow = MutableStateFlow(habits)
    private val completedInstancesFlow = MutableStateFlow(completedInstances)

    override fun observeHabits(): Flow<List<Habit>> = habitsFlow
    override fun observeCompletionsByDate(): Flow<Map<LocalDate, Set<Long>>> = MutableStateFlow(emptyMap())
    override fun observeCompletedInstancesByDate(): Flow<Map<LocalDate, Map<Long, Set<Int>>>> = completedInstancesFlow
    override fun observeCustomDomains(): Flow<List<String>> = MutableStateFlow(emptyList())
    override suspend fun getHabitById(habitId: Long): Habit? = habitsFlow.value.firstOrNull { it.id == habitId }
    override suspend fun saveHabit(habit: Habit): Long = habit.id
    override suspend fun deleteHabit(habitId: Long) {
        habitsFlow.value = habitsFlow.value.filterNot { it.id == habitId }
    }

    override suspend fun toggleCompletion(habitId: Long, date: LocalDate, instanceIndex: Int) = Unit
    override suspend fun isCompletedOn(habitId: Long, date: LocalDate): Boolean = false
    override suspend fun addCustomDomain(name: String) = Unit
}
