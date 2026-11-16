package com.rhythm.habittracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.data.RescheduleUndoToken
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.CadenceMatrixData
import com.rhythm.habittracker.domain.WeekHorizonDay
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitSortMode
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import com.rhythm.habittracker.domain.model.TimeOfDayBucket
import com.rhythm.habittracker.reminders.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

data class BucketProgress(
    val bucket: TimeOfDayBucket,
    val completed: Int,
    val total: Int,
) {
    val fraction: Float
        get() = if (total > 0) completed.toFloat() / total.toFloat() else 0f
}

data class HabitCardItem(
    val habit: Habit,
    val isCompleted: Boolean,
    val streakDays: Int,
    val instanceIndex: Int = 0,
    val totalInstances: Int = 1,
    val timeSlot: TargetTimeSlot? = habit.timeSlotForInstance(instanceIndex),
) {
    val bucket: TimeOfDayBucket
        get() = habit.bucketForInstance(instanceIndex)
}

data class RescheduleBanner(
    val habitTitle: String,
    val fromDate: LocalDate,
    val toDate: LocalDate,
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val dateLabel: String = "",
    val weekLabel: String = "",
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val bucketProgress: List<BucketProgress> = emptyList(),
    val cadenceMatrix: CadenceMatrixData? = null,
    val weeklyHorizon: List<WeekHorizonDay> = emptyList(),
    val domainFilters: List<String> = listOf("All Habits"),
    val selectedDomain: String = "All Habits",
    val sortMode: HabitSortMode = HabitSortMode.FLOW,
    val pendingHabits: List<HabitCardItem> = emptyList(),
    val completedHabits: List<HabitCardItem> = emptyList(),
    val momentumInsightLead: String = "",
    val momentumInsightHighlight: String = "",
    val momentumInsightTail: String = "",
    val totalActiveHabits: Int = 0,
    val longestStreakOverall: Int = 0,
    val rescheduleBanner: RescheduleBanner? = null,
)

class DashboardViewModel(
    private val repository: HabitRepository,
    private val reminderScheduler: ReminderScheduler? = null,
    private val clockTodayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(clockTodayProvider())
    private val selectedDomainFlow = MutableStateFlow("All Habits")
    private val sortModeFlow = MutableStateFlow(HabitSortMode.FLOW)
    private val undoTokenFlow = MutableStateFlow<RescheduleUndoToken?>(null)

    init {
        viewModelScope.launch {
            repository.observeDashboardPreferences().collect { prefs ->
                selectedDomainFlow.value = prefs.selectedDomain
                sortModeFlow.value = prefs.sortMode
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeHabits(),
        repository.observeCompletedInstancesByDate(),
        repository.observeCustomDomains(),
        selectedDateFlow,
        combine(selectedDomainFlow, sortModeFlow, undoTokenFlow) { domain, sort, undoToken ->
            Triple(domain, sort, undoToken)
        },
    ) { habits, completedInstancesByDate, customDomains, selectedDate, (selectedDomain, sortMode, undoToken) ->
        buildUiState(
            habits = habits,
            completedInstancesByDate = completedInstancesByDate,
            customDomains = customDomains,
            today = clockTodayProvider(),
            selectedDate = selectedDate,
            selectedDomain = selectedDomain,
            sortMode = sortMode,
            undoToken = undoToken,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true),
    )

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
    }

    fun selectDomain(domain: String) {
        selectedDomainFlow.value = domain
        persistPreferences(selectedDomain = domain, sortMode = sortModeFlow.value)
    }

    fun cycleSortMode() {
        val modes = HabitSortMode.entries
        val next = modes[(modes.indexOf(sortModeFlow.value) + 1) % modes.size]
        sortModeFlow.value = next
        persistPreferences(selectedDomain = selectedDomainFlow.value, sortMode = next)
    }

    fun setSortMode(mode: HabitSortMode) {
        sortModeFlow.value = mode
        persistPreferences(selectedDomain = selectedDomainFlow.value, sortMode = mode)
    }

    private fun persistPreferences(selectedDomain: String, sortMode: HabitSortMode) {
        viewModelScope.launch {
            repository.saveDashboardPreferences(selectedDomain, sortMode)
        }
    }

    fun toggleHabitCompletion(
        habitId: Long,
        instanceIndex: Int = 0,
        date: LocalDate = selectedDateFlow.value,
    ) {
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId) ?: return@launch
            if (date in habit.rescheduledFromDates) return@launch
            repository.toggleCompletion(habitId, date, instanceIndex)
        }
    }

    fun rescheduleHabit(habitId: Long, fromDate: LocalDate, toDate: LocalDate) {
        viewModelScope.launch {
            val outcome = repository.rescheduleHabitWithUndo(habitId, fromDate, toDate)
            if (outcome != null) {
                undoTokenFlow.value = outcome.undoToken
                reminderScheduler?.scheduleHabitReminders(outcome.updatedHabit)
            }
        }
    }

    fun undoLastReschedule() {
        val token = undoTokenFlow.value ?: return
        undoTokenFlow.value = null
        viewModelScope.launch {
            val restored = repository.undoReschedule(token)
            if (restored != null) {
                reminderScheduler?.scheduleHabitReminders(restored)
            }
        }
    }

    fun dismissRescheduleBanner() {
        undoTokenFlow.value = null
    }

    fun deleteHabit(habitId: Long) {
        if (undoTokenFlow.value?.habitBefore?.id == habitId) {
            undoTokenFlow.value = null
        }
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId)
            if (habit != null) {
                reminderScheduler?.cancelHabitReminders(habit)
            }
            repository.deleteHabit(habitId)
        }
    }

    private fun buildUiState(
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        customDomains: List<String>,
        today: LocalDate,
        selectedDate: LocalDate,
        selectedDomain: String,
        sortMode: HabitSortMode,
        undoToken: RescheduleUndoToken?,
    ): DashboardUiState {
        val habitById = habits.associateBy { it.id }

        val completionsByDate: Map<LocalDate, Set<Long>> = completedInstancesByDate.mapValues { (_, habitMap) ->
            habitMap.filterValues { it.isNotEmpty() }.keys
        }

        val fullyCompletedByHabit: Map<Long, Set<LocalDate>> = buildMap<Long, MutableSet<LocalDate>> {
            completedInstancesByDate.forEach { (date, habitMap) ->
                habitMap.forEach { (id, instances) ->
                    val target = habitById[id]?.dailyTargetCount ?: 1
                    if (instances.size >= target) {
                        getOrPut(id) { mutableSetOf() }.add(date)
                    }
                }
            }
        }

        val completedInstancesOnSelectedDate: Map<Long, Set<Int>> =
            completedInstancesByDate[selectedDate].orEmpty()

        val dueOnSelectedDate = habits.filter { habit ->
            val fullyCompletedDates = fullyCompletedByHabit[habit.id].orEmpty()
            val hasAnyOnSelected = completedInstancesOnSelectedDate[habit.id].orEmpty().isNotEmpty()
            val datesForDueCheck = if (hasAnyOnSelected) {
                fullyCompletedDates + selectedDate
            } else {
                fullyCompletedDates
            }
            CadenceEvaluator.isHabitDueOn(habit, selectedDate, datesForDueCheck)
        }

        val allDueCardItems = dueOnSelectedDate.flatMap { habit ->
            val datesForHabit = fullyCompletedByHabit[habit.id].orEmpty()
            val streak = CadenceEvaluator.calculateStreak(
                habit = habit,
                completedDates = datesForHabit,
                referenceDate = selectedDate,
            )
            val completedInstances = completedInstancesOnSelectedDate[habit.id].orEmpty()
            val totalInstances = maxOf(
                habit.dailyTargetCount,
                (completedInstances.maxOrNull() ?: -1) + 1,
            )
            (0 until totalInstances).map { idx ->
                HabitCardItem(
                    habit = habit,
                    isCompleted = idx in completedInstances,
                    streakDays = streak,
                    instanceIndex = idx,
                    totalInstances = totalInstances,
                    timeSlot = habit.timeSlotForInstance(idx),
                )
            }
        }

        val bucketProgress = TimeOfDayBucket.entries.map { bucket ->
            val bucketItems = allDueCardItems.filter { it.bucket == bucket }
            val done = bucketItems.count { it.isCompleted }
            BucketProgress(bucket = bucket, completed = done, total = bucketItems.size)
        }

        val distinctDomains = (customDomains + habits.map { it.domain })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val domainFilters = listOf("All Habits") + distinctDomains
        val effectiveDomain = if (domainFilters.any { it.equals(selectedDomain, ignoreCase = true) }) {
            domainFilters.first { it.equals(selectedDomain, ignoreCase = true) }
        } else {
            "All Habits"
        }

        val cardItems = if (effectiveDomain == "All Habits") {
            allDueCardItems
        } else {
            allDueCardItems.filter { it.habit.domain.equals(effectiveDomain, ignoreCase = true) }
        }

        val dueTimeComparator = compareBy<HabitCardItem> { it.timeSlot?.minuteOfDay ?: Int.MAX_VALUE }
            .thenBy { it.habit.sortOrder }
            .thenBy { it.instanceIndex }
            .thenBy { it.habit.id }

        val sortedCards = when (sortMode) {
            HabitSortMode.FLOW -> cardItems.sortedWith(dueTimeComparator)
            HabitSortMode.STREAK -> cardItems.sortedWith(
                compareByDescending<HabitCardItem> { it.streakDays }
                    .then(dueTimeComparator)
            )
            HabitSortMode.DOMAIN -> cardItems.sortedWith(
                compareBy<HabitCardItem> { it.habit.domain }
                    .then(dueTimeComparator)
            )
        }

        val pending = sortedCards.filter { !it.isCompleted }
        val completed = sortedCards.filter { it.isCompleted }

        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH)
        val weekNumber = selectedDate.get(WeekFields.ISO.weekOfWeekBasedYear())

        val matrix = CadenceEvaluator.buildCadenceMatrix(
            habits = habits,
            completionsByDate = completionsByDate,
            today = today,
            weeks = 32,
            completedInstancesByDate = completedInstancesByDate,
        )

        val horizon = CadenceEvaluator.buildWeeklyHorizon(
            habits = habits,
            completionsByDate = completionsByDate,
            selectedDate = selectedDate,
            today = today,
        )

        val allStreaks = habits.map { habit ->
            CadenceEvaluator.calculateStreak(habit, fullyCompletedByHabit[habit.id].orEmpty(), today)
        }
        val longestStreak = allStreaks.maxOrNull() ?: 0

        val totalCompletedOnSelected = allDueCardItems.count { it.isCompleted }
        val totalScheduledOnSelected = allDueCardItems.size
        val firstPending = pending.firstOrNull()
        val firstCompleted = completed.firstOrNull()
        val (lead, highlight, tail) = when {
            allDueCardItems.isEmpty() -> Triple(
                "Rest day aligned — ",
                "0 habits",
                " scheduled for this date. Tap + to architect a new rhythm.",
            )
            pending.isEmpty() -> Triple(
                "All ",
                "${dueOnSelectedDate.size} habits",
                " logged today — 100% daily cadence score achieved.",
            )
            firstCompleted != null -> Triple(
                "Completing ",
                firstCompleted.habit.title.substringBefore(" ("),
                " early sets you on track for a 100% cadence score this week.",
            )
            firstPending != null -> Triple(
                "Starting with ",
                firstPending.habit.title.substringBefore(" ("),
                " preserves your ${firstPending.streakDays}-day streak momentum.",
            )
            else -> Triple("Build consistency ", "one session", " at a time.")
        }

        val activeBanner = undoToken?.let { token ->
            val currentHabit = habitById[token.habitBefore.id]
            if (currentHabit != null && !CadenceEvaluator.hasCadenceChanged(token.habitBefore, currentHabit)) {
                RescheduleBanner(
                    habitTitle = currentHabit.title,
                    fromDate = token.fromDate,
                    toDate = token.toDate,
                )
            } else {
                null
            }
        }

        return DashboardUiState(
            isLoading = false,
            today = today,
            selectedDate = selectedDate,
            dateLabel = selectedDate.format(dateFormatter).uppercase(Locale.ENGLISH),
            weekLabel = "WEEK $weekNumber · CADENCE",
            completedCount = totalCompletedOnSelected,
            totalCount = totalScheduledOnSelected,
            bucketProgress = bucketProgress,
            cadenceMatrix = matrix,
            weeklyHorizon = horizon,
            domainFilters = domainFilters,
            selectedDomain = effectiveDomain,
            sortMode = sortMode,
            pendingHabits = pending,
            completedHabits = completed,
            momentumInsightLead = lead,
            momentumInsightHighlight = highlight,
            momentumInsightTail = tail,
            totalActiveHabits = habits.size,
            longestStreakOverall = longestStreak,
            rescheduleBanner = activeBanner,
        )
    }

    class Factory(
        private val repository: HabitRepository,
        private val reminderScheduler: ReminderScheduler? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(repository, reminderScheduler) as T
        }
    }
}
