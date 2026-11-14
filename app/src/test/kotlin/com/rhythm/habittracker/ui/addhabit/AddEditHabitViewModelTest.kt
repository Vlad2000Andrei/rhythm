package com.rhythm.habittracker.ui.addhabit

import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.data.local.DraftHabitStore
import com.rhythm.habittracker.data.local.HabitDraft
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.reminders.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditHabitViewModelTest {

    @get:Rule
    val mainDispatcherRule = AddEditMainDispatcherRule()

    @Test
    fun commitBlankTitle_setsValidationErrorAndDoesNotSave() = runTest {
        val repo = InMemoryHabitRepo()
        val draftStore = InMemoryDraftStore()
        val viewModel = AddEditHabitViewModel(
            editingHabitId = null,
            repository = repo,
            draftStore = draftStore,
        )
        advanceUntilIdle()

        viewModel.commitHabit()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.titleError)
        assertTrue(repo.savedHabits.isEmpty())
    }

    @Test
    fun newHabit_autosavesDraftAndClearsOnCommit() = runTest {
        val repo = InMemoryHabitRepo()
        val draftStore = InMemoryDraftStore()
        val scheduler = RecordingScheduler()

        val viewModel = AddEditHabitViewModel(
            editingHabitId = null,
            repository = repo,
            draftStore = draftStore,
            reminderScheduler = scheduler,
        )
        advanceUntilIdle()

        viewModel.onTitleChanged("Strength & Mobility")
        viewModel.selectDomain("Fitness")
        viewModel.selectColor(HabitAccentColor.EMERALD_GREEN)
        viewModel.selectCadenceType(CadenceType.WEEKLY)
        viewModel.applyWeeklyPreset(WeeklyPreset.WEEKDAYS)
        advanceUntilIdle()

        assertEquals("Strength & Mobility", draftStore.currentDraft?.title)
        assertEquals(WeeklyPreset.WEEKDAYS.days, viewModel.uiState.value.scheduledDays)

        val emittedEvents = mutableListOf<AddEditHabitEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            emittedEvents += viewModel.events.first()
        }

        viewModel.commitHabit()
        advanceUntilIdle()
        job.join()

        assertFalse(viewModel.uiState.value.titleError)
        assertEquals(1, repo.savedHabits.size)
        assertEquals("Strength & Mobility", repo.savedHabits.first().title)
        assertEquals(CadenceType.WEEKLY, repo.savedHabits.first().cadenceType)
        assertEquals(1, scheduler.scheduled.size)
        assertNull(draftStore.currentDraft)
        assertTrue(emittedEvents.single() is AddEditHabitEvent.Committed)
    }

    @Test
    fun monthlyPresetAndEditDelete_workEndToEnd() = runTest {
        val existing = Habit(
            id = 42L,
            title = "Monthly Review",
            domain = "Focus",
            cadenceType = CadenceType.MONTHLY,
            monthlyDates = setOf(1, 15, 30),
        )
        val repo = InMemoryHabitRepo(listOf(existing))
        val scheduler = RecordingScheduler()

        val viewModel = AddEditHabitViewModel(
            editingHabitId = 42L,
            repository = repo,
            draftStore = null,
            reminderScheduler = scheduler,
        )
        advanceUntilIdle()

        assertEquals("Monthly Review", viewModel.uiState.value.title)
        viewModel.applyMonthlyPreset(MonthlyDatePreset.BI_MONTHLY)
        advanceUntilIdle()
        assertEquals(setOf(1, 15), viewModel.uiState.value.monthlyDates)
        assertEquals("1st, 15th of every month", viewModel.uiState.value.activeDatesSummary)
        // Editing an existing habit auto-saves immediately without requiring commitHabit()
        assertEquals(setOf(1, 15), repo.savedHabits.first().monthlyDates)

        val deletedEvents = mutableListOf<AddEditHabitEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            deletedEvents += viewModel.events.first()
        }
        viewModel.deleteHabit()
        advanceUntilIdle()
        job.join()

        assertTrue(repo.savedHabits.isEmpty())
        assertEquals(1, scheduler.cancelled.size)
        assertEquals(AddEditHabitEvent.Deleted, deletedEvents.single())
    }

    @Test
    fun domainsStartEmpty_andPersistAdditionAndRemoval() = runTest {
        val repo = InMemoryHabitRepo()
        val viewModel = AddEditHabitViewModel(
            editingHabitId = null,
            repository = repo,
            draftStore = null,
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.availableDomains.isEmpty())
        assertEquals("", viewModel.uiState.value.selectedDomain)

        viewModel.addCustomDomain("Recovery")
        advanceUntilIdle()
        assertEquals(listOf("Recovery"), viewModel.uiState.value.availableDomains)
        assertEquals("Recovery", viewModel.uiState.value.selectedDomain)

        viewModel.removeDomainChip("Recovery")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.availableDomains.isEmpty())
        assertEquals("", viewModel.uiState.value.selectedDomain)
    }

    @Test
    fun customStartDate_updatesDraftAndSavesAnchorDateOnCommit() = runTest {
        val repo = InMemoryHabitRepo()
        val draftStore = InMemoryDraftStore()
        val chosenStart = LocalDate.of(2026, 6, 1)

        val viewModel = AddEditHabitViewModel(
            editingHabitId = null,
            repository = repo,
            draftStore = draftStore,
        )
        advanceUntilIdle()

        viewModel.onTitleChanged("Interval Sprint")
        viewModel.selectCadenceType(CadenceType.CUSTOM)
        viewModel.selectCustomStartDate(chosenStart)
        advanceUntilIdle()

        assertEquals(chosenStart, viewModel.uiState.value.anchorDate)
        assertEquals(chosenStart.toEpochDay(), draftStore.currentDraft?.anchorDateEpochDay)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.first()
        }
        viewModel.commitHabit()
        advanceUntilIdle()
        job.join()

        assertEquals(chosenStart, repo.savedHabits.single().anchorDate)
    }

    @Test
    fun editingTitleAndColor_preservesValidRescheduleOverrides() = runTest {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val existing = Habit(
            id = 51L,
            title = "Strength",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            colorHex = HabitAccentColor.ROYAL_BLUE.hex,
            rescheduledFromDates = setOf(monday),
            rescheduledToDates = setOf(tuesday),
        )
        val repo = InMemoryHabitRepo(listOf(existing))
        val viewModel = AddEditHabitViewModel(
            editingHabitId = 51L,
            repository = repo,
            draftStore = null,
        )
        advanceUntilIdle()

        viewModel.onTitleChanged("Strength Training")
        viewModel.selectColor(HabitAccentColor.EMERALD_GREEN)
        advanceUntilIdle()

        val saved = repo.savedHabits.single()
        assertEquals("Strength Training", saved.title)
        assertEquals(HabitAccentColor.EMERALD_GREEN.hex, saved.colorHex)
        assertEquals(setOf(monday), saved.rescheduledFromDates)
        assertEquals(setOf(tuesday), saved.rescheduledToDates)
    }

    @Test
    fun editingCadenceSchedule_clearsStaleRescheduleOverrides() = runTest {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val existing = Habit(
            id = 52L,
            title = "Tempo Run",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            rescheduledFromDates = setOf(monday),
            rescheduledToDates = setOf(tuesday),
        )
        val repo = InMemoryHabitRepo(listOf(existing))
        val viewModel = AddEditHabitViewModel(
            editingHabitId = 52L,
            repository = repo,
            draftStore = null,
        )
        advanceUntilIdle()

        viewModel.selectCadenceType(CadenceType.DAILY)
        advanceUntilIdle()

        val saved = repo.savedHabits.single()
        assertEquals(CadenceType.DAILY, saved.cadenceType)
        assertTrue(saved.rescheduledFromDates.isEmpty())
        assertTrue(saved.rescheduledToDates.isEmpty())
    }

    @Test
    fun discardDraftAndExit_whileEditingAfterLiveAutosave_restoresInitialSnapshot() = runTest {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val existing = Habit(
            id = 53L,
            title = "Morning Walk",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            colorHex = HabitAccentColor.ROSE_CORAL.hex,
            rescheduledFromDates = setOf(monday),
            rescheduledToDates = setOf(tuesday),
        )
        val repo = InMemoryHabitRepo(listOf(existing))
        val scheduler = RecordingScheduler()
        val viewModel = AddEditHabitViewModel(
            editingHabitId = 53L,
            repository = repo,
            draftStore = null,
            reminderScheduler = scheduler,
        )
        advanceUntilIdle()

        viewModel.onTitleChanged("Edited Walk")
        viewModel.selectColor(HabitAccentColor.VIOLET_PURPLE)
        viewModel.onTitleChanged("")
        advanceUntilIdle()

        var exited = false
        viewModel.discardDraftAndExit { exited = true }
        advanceUntilIdle()

        assertTrue(exited)
        val restored = repo.savedHabits.single()
        assertEquals("Morning Walk", restored.title)
        assertEquals(HabitAccentColor.ROSE_CORAL.hex, restored.colorHex)
        assertEquals(setOf(monday), restored.rescheduledFromDates)
        assertEquals(setOf(tuesday), restored.rescheduledToDates)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditMainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class InMemoryDraftStore : DraftHabitStore {
    var currentDraft: HabitDraft? = null
    override suspend fun loadDraft(): HabitDraft? = currentDraft
    override suspend fun saveDraft(draft: HabitDraft) {
        currentDraft = draft
    }
    override suspend fun clearDraft() {
        currentDraft = null
    }
}

private class RecordingScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Habit>()
    val cancelled = mutableListOf<Habit>()
    override fun scheduleHabitReminders(habit: Habit) {
        scheduled += habit
    }
    override fun cancelHabitReminders(habit: Habit) {
        cancelled += habit
    }
}

private class InMemoryHabitRepo(initial: List<Habit> = emptyList()) : HabitRepository {
    val savedHabits = initial.toMutableList()
    private var nextId = 100L
    private val habitsFlow = MutableStateFlow(initial)
    private val domainsFlow = MutableStateFlow<List<String>>(emptyList())

    override fun observeHabits(): Flow<List<Habit>> = habitsFlow
    override fun observeCompletionsByDate(): Flow<Map<LocalDate, Set<Long>>> = MutableStateFlow(emptyMap())
    override fun observeCustomDomains(): Flow<List<String>> = domainsFlow
    override suspend fun getHabitById(habitId: Long): Habit? = savedHabits.firstOrNull { it.id == habitId }
    override suspend fun saveHabit(habit: Habit): Long {
        val assigned = if (habit.id == 0L) habit.copy(id = nextId++) else habit
        savedHabits.removeAll { it.id == assigned.id }
        savedHabits += assigned
        habitsFlow.value = savedHabits.toList()
        return assigned.id
    }
    override suspend fun deleteHabit(habitId: Long) {
        savedHabits.removeAll { it.id == habitId }
        habitsFlow.value = savedHabits.toList()
    }
    override suspend fun toggleCompletion(habitId: Long, date: LocalDate, instanceIndex: Int) = Unit
    override suspend fun isCompletedOn(habitId: Long, date: LocalDate): Boolean = false
    override suspend fun addCustomDomain(name: String) {
        domainsFlow.value = (domainsFlow.value + name).distinct()
    }
    override suspend fun removeCustomDomain(name: String) {
        domainsFlow.value = domainsFlow.value.filterNot { it == name }
    }
}
