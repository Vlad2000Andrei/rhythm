package com.rhythm.habittracker.ui.addhabit

import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.data.local.DraftHabitStore
import com.rhythm.habittracker.data.local.HabitDraft
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.HabitEditScope
import com.rhythm.habittracker.domain.model.TargetTimeSlot
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
class AddEditHabitViewModelTest {

    @get:Rule
    val mainDispatcherRule = AddEditMainDispatcherRule()

    private val fixedToday = LocalDate.of(2026, 5, 15)

    @Test
    fun commitBlankTitle_setsValidationErrorAndDoesNotSave() = runTest {
        val repo = InMemoryHabitRepo()
        val draftStore = InMemoryDraftStore()
        val viewModel = AddEditHabitViewModel(
            editingHabitId = null,
            repository = repo,
            draftStore = draftStore,
            clockTodayProvider = { fixedToday },
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
            clockTodayProvider = { fixedToday },
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
        assertEquals(CadenceType.WEEKLY, repo.savedHabits.first().latestSchedule.cadenceType)
        assertEquals(1, scheduler.scheduled.size)
        assertNull(draftStore.currentDraft)
        assertTrue(emittedEvents.single() is AddEditHabitEvent.Committed)
    }

    @Test
    fun existingHabit_stagesEditsAndRequiresSaveBeforePersisting() = runTest {
        val existing = Habit.singleSchedule(
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
            clockTodayProvider = { fixedToday },
        )
        advanceUntilIdle()

        viewModel.applyMonthlyPreset(MonthlyDatePreset.BI_MONTHLY)
        advanceUntilIdle()

        // Staged in memory only—Room not overwritten yet
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)
        assertEquals(setOf(1, 15, 30), repo.savedHabits.single().latestSchedule.monthlyDates)

        // Commit prompts scope dialog because monthly dates changed
        viewModel.commitHabit()
        advanceUntilIdle()
        val scopeDialog = viewModel.uiState.value.pendingSaveScopeDialog
        assertNotNull(scopeDialog)
        assertEquals(fixedToday, scopeDialog!!.futureEffectiveDate)
        assertFalse(scopeDialog.isEffectiveTomorrowBecauseCompletedToday)

        val committedEvents = mutableListOf<AddEditHabitEvent>()
        val commitJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            committedEvents += viewModel.events.first()
        }
        viewModel.confirmSaveWithScope(HabitEditScope.ALL_OCCURRENCES)
        advanceUntilIdle()
        commitJob.join()

        assertEquals(setOf(1, 15), repo.savedHabits.single().latestSchedule.monthlyDates)
        assertEquals(1, repo.savedHabits.single().schedules.size)
        assertTrue(committedEvents.single() is AddEditHabitEvent.Committed)

        // Delete works cleanly
        val deletedEvents = mutableListOf<AddEditHabitEvent>()
        val deleteJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            deletedEvents += viewModel.events.first()
        }
        viewModel.deleteHabit()
        advanceUntilIdle()
        deleteJob.join()

        assertTrue(repo.savedHabits.isEmpty())
        assertEquals(1, scheduler.cancelled.size)
        assertEquals(AddEditHabitEvent.Deleted, deletedEvents.single())
    }

    @Test
    fun existingHabit_identityAndReminderOnlyChangesSaveImmediatelyWithoutPrompt() = runTest {
        val monday = LocalDate.of(2026, 5, 11)
        val tuesday = LocalDate.of(2026, 5, 12)
        val existing = Habit.singleSchedule(
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
            clockTodayProvider = { fixedToday },
        )
        advanceUntilIdle()

        viewModel.onTitleChanged("Strength Training")
        viewModel.selectColor(HabitAccentColor.EMERALD_GREEN)
        viewModel.toggleTimeSlotEnabled("t1")
        viewModel.toggleSkipReminderIfCompleted()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        val events = mutableListOf<AddEditHabitEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            events += viewModel.events.first()
        }
        viewModel.commitHabit()
        advanceUntilIdle()
        job.join()

        // No scope prompt shown
        assertNull(viewModel.uiState.value.pendingSaveScopeDialog)
        val saved = repo.savedHabits.single()
        assertEquals("Strength Training", saved.title)
        assertEquals(HabitAccentColor.EMERALD_GREEN.hex, saved.colorHex)
        assertFalse(saved.skipReminderIfCompleted)
        assertFalse(saved.latestSchedule.targetTimes.first().enabled)
        assertEquals(setOf(monday), saved.rescheduledFromDates)
        assertEquals(setOf(tuesday), saved.rescheduledToDates)
        assertTrue(events.single() is AddEditHabitEvent.Committed)
    }

    @Test
    fun existingHabit_completedTodaySchedulesFutureOnlyStartingTomorrow() = runTest {
        val existing = Habit.singleSchedule(
            id = 52L,
            title = "Tempo Run",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        )
        val repo = InMemoryHabitRepo(
            initial = listOf(existing),
            completedToday = mapOf(52L to setOf(0)),
        )
        val viewModel = AddEditHabitViewModel(
            editingHabitId = 52L,
            repository = repo,
            draftStore = null,
            clockTodayProvider = { fixedToday },
        )
        advanceUntilIdle()

        viewModel.selectCadenceType(CadenceType.DAILY)
        viewModel.commitHabit()
        advanceUntilIdle()

        val dialog = viewModel.uiState.value.pendingSaveScopeDialog!!
        assertTrue(dialog.isEffectiveTomorrowBecauseCompletedToday)
        assertEquals(fixedToday.plusDays(1), dialog.futureEffectiveDate)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.first() }
        viewModel.confirmSaveWithScope(HabitEditScope.FUTURE_ONLY)
        advanceUntilIdle()
        job.join()

        val saved = repo.savedHabits.single()
        assertEquals(2, saved.schedules.size)
        assertNull(saved.schedules[0].effectiveFrom)
        assertEquals(CadenceType.WEEKLY, saved.schedules[0].cadenceType)
        assertEquals(fixedToday.plusDays(1), saved.schedules[1].effectiveFrom)
        assertEquals(CadenceType.DAILY, saved.schedules[1].cadenceType)
    }

    @Test
    fun existingHabit_browsingCustomTabAndBackWithoutEditsDoesNotFlagUnsavedChanges() = runTest {
        val oldAnchor = LocalDate.of(2026, 1, 1)
        val existing = Habit.singleSchedule(
            id = 54L,
            title = "Read",
            cadenceType = CadenceType.DAILY,
            anchorDate = oldAnchor,
        )
        val repo = InMemoryHabitRepo(listOf(existing))
        val viewModel = AddEditHabitViewModel(
            editingHabitId = 54L,
            repository = repo,
            draftStore = null,
            clockTodayProvider = { fixedToday },
        )
        advanceUntilIdle()

        // Tap Custom -> resets anchorDate to today for clean Custom UX
        viewModel.selectCadenceType(CadenceType.CUSTOM)
        assertEquals(fixedToday, viewModel.uiState.value.anchorDate)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        // Tap back to Daily -> zero false dirty state!
        viewModel.selectCadenceType(CadenceType.DAILY)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        var exited = false
        viewModel.requestNavigateBack { exited = true }
        assertTrue(exited)
        assertFalse(viewModel.uiState.value.showUnsavedChangesDialog)
    }

    @Test
    fun existingHabit_backWithUnsavedEditsShowsPromptAndDiscardExitsWithoutMutatingRepo() = runTest {
        val existing = Habit.singleSchedule(
            id = 53L,
            title = "Morning Walk",
            cadenceType = CadenceType.WEEKLY,
            scheduledDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            colorHex = HabitAccentColor.ROSE_CORAL.hex,
        )
        val repo = InMemoryHabitRepo(listOf(existing))
        val viewModel = AddEditHabitViewModel(
            editingHabitId = 53L,
            repository = repo,
            draftStore = null,
            clockTodayProvider = { fixedToday },
        )
        advanceUntilIdle()

        viewModel.onTitleChanged("Edited Walk")
        viewModel.selectColor(HabitAccentColor.VIOLET_PURPLE)

        var exited = false
        viewModel.requestNavigateBack { exited = true }
        assertFalse(exited)
        assertTrue(viewModel.uiState.value.showUnsavedChangesDialog)

        viewModel.discardDraftAndExit { exited = true }
        advanceUntilIdle()

        assertTrue(exited)
        assertEquals("Morning Walk", repo.savedHabits.single().title)
        assertEquals(HabitAccentColor.ROSE_CORAL.hex, repo.savedHabits.single().colorHex)
    }

    @Test
    fun domainsStartEmpty_andPersistAdditionAndRemoval() = runTest {
        val repo = InMemoryHabitRepo()
        val viewModel = AddEditHabitViewModel(
            editingHabitId = null,
            repository = repo,
            draftStore = null,
            clockTodayProvider = { fixedToday },
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
            clockTodayProvider = { fixedToday },
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

        assertEquals(chosenStart, repo.savedHabits.single().latestSchedule.anchorDate)
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

private class InMemoryHabitRepo(
    initial: List<Habit> = emptyList(),
    private val completedToday: Map<Long, Set<Int>> = emptyMap(),
) : HabitRepository {
    val savedHabits = initial.toMutableList()
    private var nextId = 100L
    private val habitsFlow = MutableStateFlow(initial)
    private val domainsFlow = MutableStateFlow<List<String>>(emptyList())

    override fun observeHabits(): Flow<List<Habit>> = habitsFlow
    override fun observeCompletionsByDate(): Flow<Map<LocalDate, Set<Long>>> = MutableStateFlow(emptyMap())
    override fun observeCustomDomains(): Flow<List<String>> = domainsFlow
    override suspend fun getHabitById(habitId: Long): Habit? = savedHabits.firstOrNull { it.id == habitId }
    override suspend fun getCompletedInstancesOn(habitId: Long, date: LocalDate): Set<Int> =
        completedToday[habitId].orEmpty()
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
    override suspend fun isCompletedOn(habitId: Long, date: LocalDate): Boolean =
        completedToday[habitId].orEmpty().isNotEmpty()
    override suspend fun addCustomDomain(name: String) {
        domainsFlow.value = (domainsFlow.value + name).distinct()
    }
    override suspend fun removeCustomDomain(name: String) {
        domainsFlow.value = domainsFlow.value.filterNot { it == name }
    }
}
