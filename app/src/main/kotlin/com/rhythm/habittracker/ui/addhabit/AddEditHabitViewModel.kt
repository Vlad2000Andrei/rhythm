package com.rhythm.habittracker.ui.addhabit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.data.local.DraftHabitStore
import com.rhythm.habittracker.data.local.HabitDraft
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.CustomCadenceArchetype
import com.rhythm.habittracker.domain.model.EndOfMonthBehavior
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitAccentColor
import com.rhythm.habittracker.domain.model.HabitEditScope
import com.rhythm.habittracker.domain.model.HabitSchedule
import com.rhythm.habittracker.domain.model.IntervalUnit
import com.rhythm.habittracker.domain.model.MonthlySubMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import com.rhythm.habittracker.domain.model.WeeklyTargetMode
import com.rhythm.habittracker.reminders.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

enum class WeeklyPreset(val label: String, val days: Set<DayOfWeek>) {
    WEEKDAYS("Weekdays (Mon-Fri)", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)),
    WEEKENDS("Weekends (Sat-Sun)", setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
    EVERY_OTHER_DAY("Every other day", setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
    ALL("Select All", DayOfWeek.entries.toSet()),
}

enum class MonthlyDatePreset(val title: String, val badge: String, val dates: Set<Int>) {
    FIRST_OF_MONTH("1st of month", "1x/mo", setOf(1)),
    BI_MONTHLY("1st & 15th", "2x/mo", setOf(1, 15)),
    LAST_DAY("Last day of month", "EOM", setOf(31)),
    QUARTER_START("Quarter start", "QTR", setOf(1)),
}

data class SaveScopeDialogState(
    val futureEffectiveDate: LocalDate,
    val isEffectiveTomorrowBecauseCompletedToday: Boolean,
    val hasExistingScheduleHistory: Boolean,
)

data class AddEditHabitUiState(
    val editingHabitId: Long? = null,
    val title: String = "",
    val titleError: Boolean = false,
    val description: String = "",
    val availableDomains: List<String> = emptyList(),
    val selectedDomain: String = "",
    val selectedColor: HabitAccentColor = HabitAccentColor.ROYAL_BLUE,
    val cadenceType: CadenceType = CadenceType.DAILY,
    val scheduledDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val weeklyTargetMode: WeeklyTargetMode = WeeklyTargetMode.FIXED_SCHEDULE,
    val monthlySubMode: MonthlySubMode = MonthlySubMode.SPECIFIC_DATES,
    val monthlyDates: Set<Int> = setOf(1, 15, 30),
    val ordinalWeek: Int = 1,
    val ordinalDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val endOfMonthBehavior: EndOfMonthBehavior = EndOfMonthBehavior.EXECUTE_ON_LAST_DAY,
    val customArchetype: CustomCadenceArchetype = CustomCadenceArchetype.INTERVAL,
    val customInterval: Int = 3,
    val customIntervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val cycleDaysOn: Int = 5,
    val cycleDaysOff: Int = 2,
    val targetPerPeriod: Int = 1,
    val targetTimes: List<TargetTimeSlot> = DEFAULT_TARGET_TIMES,
    val skipReminderIfCompleted: Boolean = true,
    val anchorDate: LocalDate = LocalDate.now(),
    val draftSavedIndicator: Boolean = true,
    val hasUnsavedChanges: Boolean = false,
    val pendingSaveScopeDialog: SaveScopeDialogState? = null,
    val showUnsavedChangesDialog: Boolean = false,
    val isSaving: Boolean = false,
) {
    val isEditing: Boolean
        get() = editingHabitId != null

    val matchingWeeklyPreset: WeeklyPreset?
        get() = WeeklyPreset.entries.firstOrNull { it.days == scheduledDays }

    val activeDatesSummary: String
        get() = if (monthlyDates.isEmpty()) {
            "No dates selected"
        } else {
            val formatted = monthlyDates.sorted().joinToString(", ") { CadenceEvaluator.formatOrdinal(it) }
            "$formatted of every month"
        }

    companion object {
        val DEFAULT_TARGET_TIMES = listOf(
            TargetTimeSlot(id = "slot_1", hour = 8, minute = 0, enabled = true),
        )
    }
}

sealed interface AddEditHabitEvent {
    data class Committed(val message: String) : AddEditHabitEvent
    data object Deleted : AddEditHabitEvent
}

class AddEditHabitViewModel(
    private val editingHabitId: Long?,
    private val repository: HabitRepository,
    private val draftStore: DraftHabitStore?,
    private val reminderScheduler: ReminderScheduler? = null,
    private val clockTodayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditHabitUiState(
            editingHabitId = editingHabitId,
            anchorDate = clockTodayProvider(),
        ),
    )
    val uiState: StateFlow<AddEditHabitUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditHabitEvent>()
    val events: SharedFlow<AddEditHabitEvent> = _events.asSharedFlow()

    private var initialHabitSnapshot: Habit? = null
    private var draftSaveJob: Job? = null
    private var isInitialLoadComplete: Boolean = editingHabitId == null
    private var hasUserPickedAnchorDate: Boolean = false

    init {
        viewModelScope.launch {
            repository.observeCustomDomains().collect { custom ->
                val merged = custom.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                _uiState.update { state ->
                    val withSelected = if (state.selectedDomain.isNotBlank() && state.selectedDomain !in merged) {
                        merged + state.selectedDomain
                    } else {
                        merged
                    }
                    state.copy(availableDomains = withSelected)
                }
            }
        }

        viewModelScope.launch {
            if (editingHabitId != null) {
                val habit = repository.getHabitById(editingHabitId)
                if (habit != null) {
                    initialHabitSnapshot = habit
                    val latest = habit.latestSchedule
                    _uiState.update { state ->
                        val domains = (state.availableDomains + habit.domain)
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                        state.copy(
                            title = habit.title,
                            description = habit.description,
                            availableDomains = domains,
                            selectedDomain = habit.domain.trim(),
                            selectedColor = habit.accentColor,
                            cadenceType = latest.cadenceType,
                            scheduledDays = latest.scheduledDays,
                            weeklyTargetMode = latest.weeklyTargetMode,
                            monthlySubMode = latest.monthlySubMode,
                            monthlyDates = latest.monthlyDates,
                            ordinalWeek = latest.ordinalWeek,
                            ordinalDayOfWeek = latest.ordinalDayOfWeek,
                            endOfMonthBehavior = latest.endOfMonthBehavior,
                            customArchetype = latest.customArchetype,
                            customInterval = latest.customInterval,
                            customIntervalUnit = latest.customIntervalUnit,
                            cycleDaysOn = latest.cycleDaysOn,
                            cycleDaysOff = latest.cycleDaysOff,
                            targetPerPeriod = latest.targetPerPeriod,
                            targetTimes = latest.targetTimes.ifEmpty { AddEditHabitUiState.DEFAULT_TARGET_TIMES },
                            skipReminderIfCompleted = habit.skipReminderIfCompleted,
                            anchorDate = latest.anchorDate,
                            hasUnsavedChanges = false,
                        )
                    }
                }
                isInitialLoadComplete = true
            } else {
                val savedDraft = draftStore?.loadDraft()
                if (savedDraft != null) {
                    applyDraft(savedDraft)
                }
            }
        }
    }

    private fun applyDraft(draft: HabitDraft) {
        val days = draft.scheduledDayValues
            .mapNotNull { v -> DayOfWeek.entries.firstOrNull { it.value == v } }
            .toSet()
            .ifEmpty { DayOfWeek.entries.toSet() }
        val ordinalDay = DayOfWeek.entries.firstOrNull { it.value == draft.ordinalDayOfWeek } ?: DayOfWeek.MONDAY

        _uiState.update { state ->
            val domains = (state.availableDomains + draft.domain)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            state.copy(
                title = draft.title,
                description = draft.description,
                availableDomains = domains,
                selectedDomain = draft.domain.trim(),
                selectedColor = HabitAccentColor.fromHex(draft.colorHex),
                cadenceType = draft.cadenceType,
                scheduledDays = days,
                weeklyTargetMode = draft.weeklyTargetMode,
                monthlySubMode = draft.monthlySubMode,
                monthlyDates = draft.monthlyDates.filter { it in 1..31 }.toSet(),
                ordinalWeek = draft.ordinalWeek,
                ordinalDayOfWeek = ordinalDay,
                endOfMonthBehavior = draft.endOfMonthBehavior,
                customArchetype = draft.customArchetype,
                customInterval = draft.customInterval,
                customIntervalUnit = draft.customIntervalUnit,
                cycleDaysOn = draft.cycleDaysOn,
                cycleDaysOff = draft.cycleDaysOff,
                targetPerPeriod = draft.targetPerPeriod,
                targetTimes = draft.targetTimes.ifEmpty { AddEditHabitUiState.DEFAULT_TARGET_TIMES },
                skipReminderIfCompleted = draft.skipReminderIfCompleted,
                anchorDate = draft.anchorDateEpochDay?.let(LocalDate::ofEpochDay) ?: clockTodayProvider(),
            )
        }
    }

    private fun updateAndPersistDraft(transform: (AddEditHabitUiState) -> AddEditHabitUiState) {
        _uiState.update { current ->
            val transformed = transform(current)
            if (transformed.isEditing) {
                val dirty = if (isInitialLoadComplete) computeHasUnsavedChanges(transformed) else false
                transformed.copy(hasUnsavedChanges = dirty)
            } else {
                persistDraftAsync(transformed)
                transformed
            }
        }
    }

    private fun computeHasUnsavedChanges(state: AddEditHabitUiState): Boolean {
        val snapshot = initialHabitSnapshot ?: return false
        val latest = snapshot.latestSchedule
        val hasIdentityChanged = state.title.trim() != snapshot.title.trim() ||
            state.description.trim() != snapshot.description.trim() ||
            state.selectedDomain.trim() != snapshot.domain.trim() ||
            state.selectedColor.hex != snapshot.colorHex

        val baselineTimes = latest.targetTimes.ifEmpty { AddEditHabitUiState.DEFAULT_TARGET_TIMES }
        val hasReminderSettingsChanged =
            state.skipReminderIfCompleted != snapshot.skipReminderIfCompleted ||
                state.targetTimes.map { it.enabled } != baselineTimes.map { it.enabled }

        val stagedSchedule = buildScheduleFromState(state)
        val normalizedLatest = latest.copy(targetTimes = baselineTimes)
        val hasScheduleOrTimesChanged = CadenceEvaluator.hasScheduleOrTimesChanged(normalizedLatest, stagedSchedule)

        return hasIdentityChanged || hasReminderSettingsChanged || hasScheduleOrTimesChanged
    }

    private fun buildScheduleFromState(state: AddEditHabitUiState): HabitSchedule {
        val safeScheduledDays = state.scheduledDays.ifEmpty { DayOfWeek.entries.toSet() }
        val safeMonthlyDates = state.monthlyDates.ifEmpty { setOf(1) }
        val safeTimes = state.targetTimes.ifEmpty { AddEditHabitUiState.DEFAULT_TARGET_TIMES }
        return HabitSchedule(
            id = 0L,
            effectiveFrom = null,
            cadenceType = state.cadenceType,
            scheduledDays = safeScheduledDays,
            weeklyTargetMode = state.weeklyTargetMode,
            monthlySubMode = state.monthlySubMode,
            monthlyDates = safeMonthlyDates,
            ordinalWeek = state.ordinalWeek,
            ordinalDayOfWeek = state.ordinalDayOfWeek,
            endOfMonthBehavior = state.endOfMonthBehavior,
            customArchetype = state.customArchetype,
            customInterval = state.customInterval.coerceAtLeast(1),
            customIntervalUnit = state.customIntervalUnit,
            cycleDaysOn = state.cycleDaysOn.coerceAtLeast(1),
            cycleDaysOff = state.cycleDaysOff.coerceAtLeast(0),
            targetPerPeriod = state.targetPerPeriod.coerceAtLeast(safeTimes.size),
            targetTimes = safeTimes,
            timeOfDayBucket = TimeOfDayBucket.fromTimes(safeTimes),
            anchorDate = state.anchorDate,
        )
    }

    private fun buildNewHabitFromState(state: AddEditHabitUiState, today: LocalDate): Habit {
        val baselineSchedule = buildScheduleFromState(state).copy(effectiveFrom = null)
        return Habit(
            id = 0L,
            title = state.title.trim(),
            description = state.description.trim(),
            domain = state.selectedDomain.trim(),
            subTag = null,
            colorHex = state.selectedColor.hex,
            skipReminderIfCompleted = state.skipReminderIfCompleted,
            sortOrder = 0,
            createdAtEpochDay = today.toEpochDay(),
            rescheduledFromDates = emptySet(),
            rescheduledToDates = emptySet(),
            schedules = listOf(baselineSchedule),
            pauseWindows = emptyList(),
        )
    }

    private fun persistDraftAsync(state: AddEditHabitUiState) {
        val store = draftStore ?: return
        val previousJob = draftSaveJob
        draftSaveJob = viewModelScope.launch {
            previousJob?.cancelAndJoin()
            store.saveDraft(
                HabitDraft(
                    title = state.title,
                    description = state.description,
                    domain = state.selectedDomain,
                    colorHex = state.selectedColor.hex,
                    cadenceType = state.cadenceType,
                    scheduledDayValues = state.scheduledDays.map { it.value }.sorted(),
                    weeklyTargetMode = state.weeklyTargetMode,
                    monthlySubMode = state.monthlySubMode,
                    monthlyDates = state.monthlyDates.sorted(),
                    ordinalWeek = state.ordinalWeek,
                    ordinalDayOfWeek = state.ordinalDayOfWeek.value,
                    endOfMonthBehavior = state.endOfMonthBehavior,
                    customArchetype = state.customArchetype,
                    customInterval = state.customInterval,
                    customIntervalUnit = state.customIntervalUnit,
                    cycleDaysOn = state.cycleDaysOn,
                    cycleDaysOff = state.cycleDaysOff,
                    targetPerPeriod = state.targetPerPeriod,
                    targetTimes = state.targetTimes,
                    skipReminderIfCompleted = state.skipReminderIfCompleted,
                    anchorDateEpochDay = state.anchorDate.toEpochDay(),
                ),
            )
        }
    }

    fun onTitleChanged(raw: String) {
        val clipped = raw.take(48)
        updateAndPersistDraft { it.copy(title = clipped, titleError = false) }
    }

    fun onDescriptionChanged(raw: String) {
        updateAndPersistDraft { it.copy(description = raw) }
    }

    fun selectDomain(domain: String) {
        updateAndPersistDraft { state ->
            val next = if (state.selectedDomain == domain) "" else domain
            state.copy(selectedDomain = next)
        }
    }

    fun removeDomainChip(domain: String) {
        val trimmed = domain.trim()
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch {
                repository.removeCustomDomain(trimmed)
            }
            initialHabitSnapshot?.let { snap ->
                if (snap.domain.trim().equals(trimmed, ignoreCase = true)) {
                    initialHabitSnapshot = snap.copy(domain = "")
                }
            }
        }
        updateAndPersistDraft { state ->
            val remaining = state.availableDomains.filterNot { it == domain }
            val nextSelected = if (state.selectedDomain == domain) "" else state.selectedDomain
            state.copy(availableDomains = remaining, selectedDomain = nextSelected)
        }
    }

    fun addCustomDomain(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addCustomDomain(trimmed)
        }
        updateAndPersistDraft { state ->
            val updated = (state.availableDomains + trimmed).distinct()
            state.copy(availableDomains = updated, selectedDomain = trimmed)
        }
    }

    fun selectColor(color: HabitAccentColor) {
        updateAndPersistDraft { it.copy(selectedColor = color) }
    }

    fun selectCadenceType(cadenceType: CadenceType) {
        updateAndPersistDraft { state ->
            val adjustedDays = when (cadenceType) {
                CadenceType.DAILY -> if (state.scheduledDays.size < 7 && state.cadenceType != CadenceType.DAILY) {
                    DayOfWeek.entries.toSet()
                } else {
                    state.scheduledDays
                }
                CadenceType.WEEKLY -> if (state.scheduledDays.size == 7) {
                    setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
                } else {
                    state.scheduledDays
                }
                else -> state.scheduledDays
            }
            val adjustedAnchorDate = if (
                state.isEditing &&
                cadenceType == CadenceType.CUSTOM &&
                state.cadenceType != CadenceType.CUSTOM &&
                !hasUserPickedAnchorDate
            ) {
                val initialLatest = initialHabitSnapshot?.latestSchedule
                if (initialLatest != null && initialLatest.cadenceType != CadenceType.CUSTOM) {
                    clockTodayProvider()
                } else {
                    initialLatest?.anchorDate ?: state.anchorDate
                }
            } else {
                state.anchorDate
            }
            state.copy(
                cadenceType = cadenceType,
                scheduledDays = adjustedDays,
                anchorDate = adjustedAnchorDate,
            )
        }
    }

    fun toggleScheduledDay(day: DayOfWeek) {
        updateAndPersistDraft { state ->
            val next = if (day in state.scheduledDays) {
                state.scheduledDays - day
            } else {
                state.scheduledDays + day
            }
            state.copy(scheduledDays = next)
        }
    }

    fun applyWeeklyPreset(preset: WeeklyPreset) {
        updateAndPersistDraft { it.copy(scheduledDays = preset.days) }
    }

    fun selectWeeklyTargetMode(mode: WeeklyTargetMode) {
        updateAndPersistDraft { it.copy(weeklyTargetMode = mode) }
    }

    fun selectMonthlySubMode(mode: MonthlySubMode) {
        updateAndPersistDraft { it.copy(monthlySubMode = mode) }
    }

    fun applyMonthlyPreset(preset: MonthlyDatePreset) {
        updateAndPersistDraft { it.copy(monthlyDates = preset.dates, monthlySubMode = MonthlySubMode.SPECIFIC_DATES) }
    }

    fun toggleMonthlyDate(dayOfMonth: Int) {
        if (dayOfMonth !in 1..31) return
        updateAndPersistDraft { state ->
            val next = if (dayOfMonth in state.monthlyDates) {
                state.monthlyDates - dayOfMonth
            } else {
                state.monthlyDates + dayOfMonth
            }
            state.copy(monthlyDates = next)
        }
    }

    fun selectOrdinalWeek(week: Int) {
        updateAndPersistDraft { it.copy(ordinalWeek = week) }
    }

    fun selectOrdinalDayOfWeek(day: DayOfWeek) {
        updateAndPersistDraft { it.copy(ordinalDayOfWeek = day) }
    }

    fun selectEndOfMonthBehavior(behavior: EndOfMonthBehavior) {
        updateAndPersistDraft { it.copy(endOfMonthBehavior = behavior) }
    }

    fun selectCustomArchetype(archetype: CustomCadenceArchetype) {
        updateAndPersistDraft { it.copy(customArchetype = archetype) }
    }

    fun adjustCustomInterval(delta: Int) {
        updateAndPersistDraft { state ->
            state.copy(customInterval = (state.customInterval + delta).coerceIn(1, 365))
        }
    }

    fun selectCustomIntervalUnit(unit: IntervalUnit) {
        updateAndPersistDraft { it.copy(customIntervalUnit = unit) }
    }

    fun adjustCycleOn(delta: Int) {
        updateAndPersistDraft { state ->
            state.copy(cycleDaysOn = (state.cycleDaysOn + delta).coerceIn(1, 90))
        }
    }

    fun adjustCycleOff(delta: Int) {
        updateAndPersistDraft { state ->
            state.copy(cycleDaysOff = (state.cycleDaysOff + delta).coerceIn(0, 90))
        }
    }

    fun selectCustomStartDate(date: LocalDate) {
        hasUserPickedAnchorDate = true
        updateAndPersistDraft { it.copy(anchorDate = date) }
    }

    fun adjustTargetPerPeriod(delta: Int) {
        updateAndPersistDraft { state ->
            val currentCount = maxOf(state.targetPerPeriod, state.targetTimes.size)
            val nextTarget = (currentCount + delta).coerceIn(1, 20)
            val adjustedTimes = when {
                nextTarget > state.targetTimes.size -> {
                    val toAdd = nextTarget - state.targetTimes.size
                    val extraSlots = (1..toAdd).map { offset ->
                        val baseHour = ((state.targetTimes.lastOrNull()?.hour ?: 8) + (offset * 4)) % 24
                        TargetTimeSlot(
                            id = UUID.randomUUID().toString(),
                            hour = baseHour,
                            minute = 0,
                            enabled = true,
                        )
                    }
                    state.targetTimes + extraSlots
                }
                nextTarget < state.targetTimes.size -> state.targetTimes.take(nextTarget)
                else -> state.targetTimes
            }
            state.copy(
                targetPerPeriod = nextTarget,
                targetTimes = adjustedTimes,
            )
        }
    }

    fun toggleTimeSlotEnabled(slotId: String) {
        updateAndPersistDraft { state ->
            val next = state.targetTimes.map { slot ->
                if (slot.id == slotId) slot.copy(enabled = !slot.enabled) else slot
            }
            state.copy(targetTimes = next)
        }
    }

    fun updateTimeSlot(slotId: String, hour: Int, minute: Int) {
        updateAndPersistDraft { state ->
            val next = state.targetTimes.map { slot ->
                if (slot.id == slotId) slot.copy(hour = hour, minute = minute) else slot
            }
            state.copy(targetTimes = next)
        }
    }

    fun addTimeSlot(hour: Int, minute: Int) {
        updateAndPersistDraft { state ->
            val newSlot = TargetTimeSlot(
                id = UUID.randomUUID().toString(),
                hour = hour,
                minute = minute,
                enabled = true,
            )
            val updatedTimes = state.targetTimes + newSlot
            state.copy(
                targetTimes = updatedTimes,
                targetPerPeriod = updatedTimes.size,
            )
        }
    }

    fun removeTimeSlot(slotId: String) {
        updateAndPersistDraft { state ->
            if (state.targetTimes.size <= 1) return@updateAndPersistDraft state
            val updatedTimes = state.targetTimes.filterNot { it.id == slotId }
            state.copy(
                targetTimes = updatedTimes,
                targetPerPeriod = updatedTimes.size,
            )
        }
    }

    fun toggleSkipReminderIfCompleted() {
        updateAndPersistDraft { it.copy(skipReminderIfCompleted = !it.skipReminderIfCompleted) }
    }

    fun commitHabit() {
        val current = _uiState.value
        if (current.isSaving) return
        val trimmedTitle = current.title.trim()
        if (trimmedTitle.isEmpty()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }

        if (!current.isEditing) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true) }
                draftSaveJob?.cancelAndJoin()
                val today = clockTodayProvider()
                val habit = buildNewHabitFromState(current, today)
                val savedId = repository.saveHabit(habit)
                reminderScheduler?.scheduleHabitReminders(habit.copy(id = savedId))
                draftStore?.clearDraft()

                val subtitle = cadenceCommittedSubtitle(habit.latestSchedule)
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(AddEditHabitEvent.Committed(subtitle))
            }
            return
        }

        val id = editingHabitId ?: return
        if (!current.hasUnsavedChanges) {
            viewModelScope.launch {
                _events.emit(AddEditHabitEvent.Committed("No changes"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val existing = repository.getHabitById(id) ?: initialHabitSnapshot
            if (existing == null) {
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            val stagedSchedule = buildScheduleFromState(current)
            val baselineLatest = existing.latestSchedule.let {
                it.copy(targetTimes = it.targetTimes.ifEmpty { AddEditHabitUiState.DEFAULT_TARGET_TIMES })
            }
            val today = clockTodayProvider()

            if (!CadenceEvaluator.hasScheduleOrTimesChanged(baselineLatest, stagedSchedule)) {
                val updated = CadenceEvaluator.applyIdentityAndReminderOnlyEdit(
                    existing = existing,
                    updatedTitle = current.title,
                    updatedDescription = current.description,
                    updatedDomain = current.selectedDomain,
                    updatedColorHex = current.selectedColor.hex,
                    updatedSkipReminderIfCompleted = current.skipReminderIfCompleted,
                    updatedTargetTimes = current.targetTimes,
                    today = today,
                )
                val savedId = repository.saveHabit(updated)
                reminderScheduler?.scheduleHabitReminders(updated.copy(id = savedId))
                initialHabitSnapshot = updated.copy(id = savedId)
                _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
                _events.emit(AddEditHabitEvent.Committed("Habit details updated"))
            } else {
                val completedToday = repository.getCompletedInstancesOn(id, today)
                val effectiveDate = if (completedToday.isEmpty()) today else today.plusDays(1)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        pendingSaveScopeDialog = SaveScopeDialogState(
                            futureEffectiveDate = effectiveDate,
                            isEffectiveTomorrowBecauseCompletedToday = completedToday.isNotEmpty(),
                            hasExistingScheduleHistory = existing.schedules.size > 1,
                        ),
                    )
                }
            }
        }
    }

    fun confirmSaveWithScope(scope: HabitEditScope) {
        val current = _uiState.value
        if (current.isSaving || !current.isEditing) return
        val id = editingHabitId ?: return
        if (current.title.trim().isEmpty()) {
            _uiState.update { it.copy(titleError = true, pendingSaveScopeDialog = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, pendingSaveScopeDialog = null) }
            val existing = repository.getHabitById(id) ?: initialHabitSnapshot
            if (existing == null) {
                _uiState.update { it.copy(isSaving = false) }
                return@launch
            }
            val today = clockTodayProvider()
            val completedToday = repository.getCompletedInstancesOn(id, today)
            val effectiveDate = if (completedToday.isEmpty()) today else today.plusDays(1)
            val stagedSchedule = buildScheduleFromState(current)

            val updated = CadenceEvaluator.applyScheduleEdit(
                existing = existing,
                updatedTitle = current.title,
                updatedDescription = current.description,
                updatedDomain = current.selectedDomain,
                updatedColorHex = current.selectedColor.hex,
                updatedSkipReminderIfCompleted = current.skipReminderIfCompleted,
                updatedSchedule = stagedSchedule,
                scope = scope,
                effectiveFrom = effectiveDate,
            )
            val savedId = repository.saveHabit(updated)
            reminderScheduler?.scheduleHabitReminders(updated.copy(id = savedId))
            initialHabitSnapshot = updated.copy(id = savedId)

            val dateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
            val subtitle = when (scope) {
                HabitEditScope.FUTURE_ONLY -> "Updated from ${effectiveDate.format(dateFormatter)} onward"
                HabitEditScope.ALL_OCCURRENCES -> "Updated all occurrences"
            }
            _uiState.update { it.copy(isSaving = false, hasUnsavedChanges = false) }
            _events.emit(AddEditHabitEvent.Committed(subtitle))
        }
    }

    fun dismissSaveScopeDialog() {
        _uiState.update { it.copy(pendingSaveScopeDialog = null) }
    }

    fun requestNavigateBack(onExit: () -> Unit) {
        val current = _uiState.value
        if (current.isSaving) return
        if (current.isEditing && current.hasUnsavedChanges) {
            _uiState.update { it.copy(showUnsavedChangesDialog = true) }
        } else {
            onExit()
        }
    }

    fun dismissUnsavedChangesDialog() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
    }

    fun saveFromUnsavedChangesDialog() {
        _uiState.update { it.copy(showUnsavedChangesDialog = false) }
        commitHabit()
    }

    fun discardDraftAndExit(onExit: () -> Unit) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            draftSaveJob?.cancelAndJoin()
            if (editingHabitId == null) {
                draftStore?.clearDraft()
            } else {
                _uiState.update {
                    it.copy(
                        pendingSaveScopeDialog = null,
                        showUnsavedChangesDialog = false,
                    )
                }
            }
            onExit()
        }
    }

    fun deleteHabit() {
        val id = editingHabitId ?: return
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val habit = repository.getHabitById(id)
            if (habit != null) {
                reminderScheduler?.cancelHabitReminders(habit)
            }
            repository.deleteHabit(id)
            _uiState.update { it.copy(isSaving = false) }
            _events.emit(AddEditHabitEvent.Deleted)
        }
    }

    private fun cadenceCommittedSubtitle(schedule: HabitSchedule): String = when (schedule.cadenceType) {
        CadenceType.DAILY -> "Daily cadence active"
        CadenceType.WEEKLY -> "Weekly cadence active (${schedule.scheduledDays.size}x/week)"
        CadenceType.MONTHLY -> "Monthly cadence active"
        CadenceType.CUSTOM -> "Custom cadence active"
    }

    class Factory(
        private val editingHabitId: Long?,
        private val repository: HabitRepository,
        private val draftStore: DraftHabitStore?,
        private val reminderScheduler: ReminderScheduler? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddEditHabitViewModel(
                editingHabitId = editingHabitId,
                repository = repository,
                draftStore = draftStore,
                reminderScheduler = reminderScheduler,
            ) as T
        }
    }
}
