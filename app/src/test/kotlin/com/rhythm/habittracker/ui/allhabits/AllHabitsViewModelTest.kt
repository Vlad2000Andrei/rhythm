package com.rhythm.habittracker.ui.allhabits

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AllHabitsViewModelTest {

    @get:Rule
    val mainDispatcherRule = AllHabitsMainDispatcherRule()

    @Test
    fun allHabits_computesInventoryTelemetryAndDomainChipCounts() = runTest {
        val today = LocalDate.of(2026, 5, 15)
        val repo = FakeAllHabitsRepository(
            habits = listOf(
                Habit(
                    id = 1L,
                    title = "Deep Work",
                    description = "Double-shot espresso, terminal open.",
                    domain = "Focus",
                    colorHex = HabitAccentColor.ROYAL_BLUE.hex,
                    cadenceType = CadenceType.DAILY,
                    targetTimes = listOf(TargetTimeSlot("t1", 8, 30, true)),
                    sortOrder = 0,
                    createdAtEpochDay = today.minusDays(10).toEpochDay(),
                ),
                Habit(
                    id = 2L,
                    title = "Read Literature",
                    description = "Paper copy before sleep.",
                    domain = "Mind",
                    colorHex = HabitAccentColor.WARM_AMBER.hex,
                    cadenceType = CadenceType.DAILY,
                    targetTimes = listOf(TargetTimeSlot("t2", 21, 45, true)),
                    sortOrder = 1,
                    createdAtEpochDay = today.minusDays(10).toEpochDay(),
                ),
                Habit(
                    id = 3L,
                    title = "Evening Reflection",
                    domain = "Mind",
                    colorHex = HabitAccentColor.SLATE_GRAPHITE.hex,
                    cadenceType = CadenceType.WEEKLY,
                    scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                    sortOrder = 2,
                    createdAtEpochDay = today.minusDays(10).toEpochDay(),
                ),
            ),
            completedInstances = mapOf(
                today to mapOf(1L to setOf(0), 2L to setOf(0)),
                today.minusDays(1) to mapOf(1L to setOf(0)),
                today.minusDays(2) to mapOf(1L to setOf(0)),
            ),
        )

        val viewModel = AllHabitsViewModel(
            repository = repo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.totalHabits)
        // Streaks: Deep Work = 3, Read Literature = 1, Evening Reflection = 0 -> average(3,1,0)=1.33 -> 1
        assertEquals(1, state.meanStreakDays)
        assertTrue(state.completionRatePercent > 0)
        assertEquals(listOf("ALL (3)", "FOCUS (1)", "MIND (2)"), state.filterChips.map { it.label })
        assertEquals(listOf("Deep Work", "Read Literature", "Evening Reflection"), state.filteredHabits.map { it.habit.title })
    }

    @Test
    fun allHabits_filtersByDomainChipAndSearchQueryAndResetsCleanly() = runTest {
        val today = LocalDate.of(2026, 5, 15)
        val repo = FakeAllHabitsRepository(
            habits = listOf(
                Habit(
                    id = 1L,
                    title = "Deep Work",
                    description = "Terminal open, phone locked in drawer.",
                    domain = "Focus",
                    cadenceType = CadenceType.DAILY,
                    sortOrder = 0,
                ),
                Habit(
                    id = 2L,
                    title = "Tempo Run",
                    description = "Shoes set at doorway.",
                    domain = "Fitness",
                    cadenceType = CadenceType.WEEKLY,
                    scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                    sortOrder = 1,
                ),
                Habit(
                    id = 3L,
                    title = "Read Literature",
                    description = "Kindle or paper copy only.",
                    domain = "Mind",
                    cadenceType = CadenceType.DAILY,
                    sortOrder = 2,
                ),
            ),
        )

        val viewModel = AllHabitsViewModel(
            repository = repo,
            clockTodayProvider = { today },
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.selectDomain("FITNESS")
        advanceUntilIdle()
        assertEquals(listOf("Tempo Run"), viewModel.uiState.value.filteredHabits.map { it.habit.title })

        viewModel.selectDomain("ALL")
        viewModel.onSearchQueryChanged("kindle")
        advanceUntilIdle()
        assertEquals(listOf("Read Literature"), viewModel.uiState.value.filteredHabits.map { it.habit.title })

        viewModel.onSearchQueryChanged("nonexistent")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filteredHabits.isEmpty())

        viewModel.resetFilters()
        advanceUntilIdle()
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals("ALL", viewModel.uiState.value.selectedDomainKey)
        assertEquals(3, viewModel.uiState.value.filteredHabits.size)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AllHabitsMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeAllHabitsRepository(
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
    override suspend fun deleteHabit(habitId: Long) = Unit
    override suspend fun toggleCompletion(habitId: Long, date: LocalDate, instanceIndex: Int) = Unit
    override suspend fun isCompletedOn(habitId: Long, date: LocalDate): Boolean = false
    override suspend fun addCustomDomain(name: String) = Unit
}
