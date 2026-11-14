package com.rhythm.habittracker.ui.allhabits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

data class AllHabitFilterChip(
    val key: String,
    val label: String,
    val count: Int,
)

data class AllHabitListItem(
    val habit: Habit,
    val streakDays: Int,
    val scheduleSummary: String,
)

data class AllHabitsUiState(
    val isLoading: Boolean = true,
    val totalHabits: Int = 0,
    val meanStreakDays: Int = 0,
    val completionRatePercent: Int = 0,
    val searchQuery: String = "",
    val selectedDomainKey: String = "ALL",
    val filterChips: List<AllHabitFilterChip> = emptyList(),
    val filteredHabits: List<AllHabitListItem> = emptyList(),
)

class AllHabitsViewModel(
    private val repository: HabitRepository,
    private val clockTodayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val selectedDomainKeyFlow = MutableStateFlow("ALL")

    val uiState: StateFlow<AllHabitsUiState> = combine(
        repository.observeHabits(),
        repository.observeCompletedInstancesByDate(),
        searchQueryFlow,
        selectedDomainKeyFlow,
    ) { habits, completedInstancesByDate, searchQuery, selectedDomainKey ->
        buildUiState(
            habits = habits,
            completedInstancesByDate = completedInstancesByDate,
            today = clockTodayProvider(),
            searchQuery = searchQuery,
            selectedDomainKey = selectedDomainKey,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AllHabitsUiState(isLoading = true),
    )

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    fun clearSearch() {
        searchQueryFlow.value = ""
    }

    fun selectDomain(key: String) {
        selectedDomainKeyFlow.value = key
    }

    fun resetFilters() {
        searchQueryFlow.value = ""
        selectedDomainKeyFlow.value = "ALL"
    }

    private fun buildUiState(
        habits: List<Habit>,
        completedInstancesByDate: Map<LocalDate, Map<Long, Set<Int>>>,
        today: LocalDate,
        searchQuery: String,
        selectedDomainKey: String,
    ): AllHabitsUiState {
        val sortedHabits = habits.sortedBy { it.sortOrder }
        val habitById = sortedHabits.associateBy { it.id }

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

        val items = sortedHabits.map { habit ->
            val completedDates = fullyCompletedByHabit[habit.id].orEmpty()
            AllHabitListItem(
                habit = habit,
                streakDays = CadenceEvaluator.calculateStreak(
                    habit = habit,
                    completedDates = completedDates,
                    referenceDate = today,
                ),
                scheduleSummary = CadenceEvaluator.formatHabitScheduleSummary(habit),
            )
        }

        val meanStreakDays = if (items.isNotEmpty()) {
            items.map { it.streakDays }.average().roundToInt()
        } else {
            0
        }

        val matrix = CadenceEvaluator.buildCadenceMatrix(
            habits = sortedHabits,
            completionsByDate = completionsByDate,
            today = today,
            weeks = 14,
            completedInstancesByDate = completedInstancesByDate,
        )

        val countsByDomainKey = linkedMapOf<String, Int>()
        sortedHabits.forEach { habit ->
            val domainKey = habit.domain.trim().uppercase(Locale.ENGLISH)
            if (domainKey.isNotEmpty()) {
                countsByDomainKey[domainKey] = (countsByDomainKey[domainKey] ?: 0) + 1
            }
        }

        val filterChips = buildList {
            add(
                AllHabitFilterChip(
                    key = "ALL",
                    label = "ALL (${sortedHabits.size})",
                    count = sortedHabits.size,
                ),
            )
            countsByDomainKey.forEach { (domainKey, count) ->
                add(
                    AllHabitFilterChip(
                        key = domainKey,
                        label = "$domainKey ($count)",
                        count = count,
                    ),
                )
            }
        }

        val effectiveDomainKey = if (filterChips.any { it.key == selectedDomainKey }) {
            selectedDomainKey
        } else {
            "ALL"
        }

        val query = searchQuery.trim().lowercase(Locale.ENGLISH)
        val filteredHabits = items.filter { item ->
            val matchesDomain = effectiveDomainKey == "ALL" ||
                item.habit.domain.trim().uppercase(Locale.ENGLISH) == effectiveDomainKey
            val matchesQuery = query.isEmpty() ||
                item.habit.title.lowercase(Locale.ENGLISH).contains(query) ||
                item.habit.description.lowercase(Locale.ENGLISH).contains(query) ||
                item.habit.domain.lowercase(Locale.ENGLISH).contains(query) ||
                item.habit.subTag.orEmpty().lowercase(Locale.ENGLISH).contains(query) ||
                item.habit.cadenceType.label.lowercase(Locale.ENGLISH).contains(query) ||
                item.scheduleSummary.lowercase(Locale.ENGLISH).contains(query)
            matchesDomain && matchesQuery
        }

        return AllHabitsUiState(
            isLoading = false,
            totalHabits = sortedHabits.size,
            meanStreakDays = meanStreakDays,
            completionRatePercent = matrix.consistencyPercent,
            searchQuery = searchQuery,
            selectedDomainKey = effectiveDomainKey,
            filterChips = filterChips,
            filteredHabits = filteredHabits,
        )
    }

    class Factory(
        private val repository: HabitRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AllHabitsViewModel(repository) as T
        }
    }
}
