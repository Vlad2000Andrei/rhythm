package com.rhythm.habittracker.data

import com.rhythm.habittracker.data.local.dao.HabitDao
import com.rhythm.habittracker.data.local.entity.CustomDomainEntity
import com.rhythm.habittracker.data.local.entity.DashboardPreferencesEntity
import com.rhythm.habittracker.data.local.entity.HabitCompletionEntity
import com.rhythm.habittracker.data.local.toDomain
import com.rhythm.habittracker.data.local.toEntity
import com.rhythm.habittracker.domain.CadenceEvaluator
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.HabitSortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

data class DashboardPreferences(
    val selectedDomain: String = "All Habits",
    val sortMode: HabitSortMode = HabitSortMode.FLOW,
)

data class RescheduleUndoToken(
    val habitBefore: Habit,
    val removedCompletionsOnFromDate: Set<Int>,
    val fromDate: LocalDate,
    val toDate: LocalDate,
)

data class RescheduleOutcome(
    val updatedHabit: Habit,
    val undoToken: RescheduleUndoToken,
)

interface HabitRepository {
    fun observeHabits(): Flow<List<Habit>>
    fun observeCompletionsByDate(): Flow<Map<LocalDate, Set<Long>>>
    fun observeCompletedInstancesByDate(): Flow<Map<LocalDate, Map<Long, Set<Int>>>> =
        observeCompletionsByDate().map { byDate ->
            byDate.mapValues { (_, habitIds) ->
                habitIds.associateWith { setOf(0) }
            }
        }
    fun observeCustomDomains(): Flow<List<String>>
    fun observeDashboardPreferences(): Flow<DashboardPreferences> = flowOf(DashboardPreferences())
    suspend fun getHabitById(habitId: Long): Habit?
    suspend fun saveHabit(habit: Habit): Long
    suspend fun deleteHabit(habitId: Long)
    suspend fun toggleCompletion(habitId: Long, date: LocalDate, instanceIndex: Int = 0)
    suspend fun isCompletedOn(habitId: Long, date: LocalDate): Boolean
    suspend fun getCompletedInstancesOn(habitId: Long, date: LocalDate): Set<Int> =
        if (isCompletedOn(habitId, date)) setOf(0) else emptySet()
    suspend fun addCustomDomain(name: String)
    suspend fun removeCustomDomain(name: String) = Unit
    suspend fun saveDashboardPreferences(selectedDomain: String, sortMode: HabitSortMode) = Unit
    suspend fun rescheduleHabit(habitId: Long, fromDate: LocalDate, toDate: LocalDate): Boolean =
        rescheduleHabitWithUndo(habitId, fromDate, toDate) != null
    suspend fun rescheduleHabitWithUndo(habitId: Long, fromDate: LocalDate, toDate: LocalDate): RescheduleOutcome? = null
    suspend fun undoReschedule(token: RescheduleUndoToken): Habit? = null
}

class RoomHabitRepository(
    private val dao: HabitDao,
) : HabitRepository {

    private val rescheduleMutex = Mutex()

    override fun observeHabits(): Flow<List<Habit>> =
        dao.observeHabits().map { list -> list.map { it.toDomain() } }

    override fun observeCompletionsByDate(): Flow<Map<LocalDate, Set<Long>>> =
        dao.observeAllCompletions().map { entries ->
            entries
                .groupBy { LocalDate.ofEpochDay(it.dateEpochDay) }
                .mapValues { (_, list) -> list.map { it.habitId }.toSet() }
        }

    override fun observeCompletedInstancesByDate(): Flow<Map<LocalDate, Map<Long, Set<Int>>>> =
        dao.observeAllCompletions().map { entries ->
            entries
                .groupBy { LocalDate.ofEpochDay(it.dateEpochDay) }
                .mapValues { (_, list) ->
                    list.groupBy { it.habitId }
                        .mapValues { (_, habitEntries) -> habitEntries.map { it.instanceIndex }.toSet() }
                }
        }

    override fun observeCustomDomains(): Flow<List<String>> =
        dao.observeCustomDomains().map { list -> list.map { it.name } }

    override fun observeDashboardPreferences(): Flow<DashboardPreferences> =
        dao.observeDashboardPreferences().map { entity ->
            if (entity == null) {
                DashboardPreferences()
            } else {
                val mode = HabitSortMode.entries.firstOrNull { it.name == entity.sortMode } ?: HabitSortMode.FLOW
                DashboardPreferences(
                    selectedDomain = entity.selectedDomain.ifBlank { "All Habits" },
                    sortMode = mode,
                )
            }
        }

    override suspend fun getHabitById(habitId: Long): Habit? =
        dao.getHabitById(habitId)?.toDomain()

    override suspend fun saveHabit(habit: Habit): Long {
        val trimmedDomain = habit.domain.trim()
        if (trimmedDomain.isNotEmpty()) {
            dao.insertCustomDomain(
                CustomDomainEntity(
                    name = trimmedDomain,
                    createdAtMillis = System.currentTimeMillis(),
                )
            )
        }
        val nextSortOrder = if (habit.id == 0L) {
            (dao.getAllHabits().maxOfOrNull { it.sortOrder } ?: -1) + 1
        } else {
            habit.sortOrder
        }
        val entity = habit.copy(domain = trimmedDomain, sortOrder = nextSortOrder).toEntity()
        return if (habit.id == 0L) {
            dao.insertHabit(entity)
        } else {
            dao.updateHabit(entity)
            habit.id
        }
    }

    override suspend fun deleteHabit(habitId: Long) {
        dao.deleteHabitById(habitId)
    }

    override suspend fun toggleCompletion(habitId: Long, date: LocalDate, instanceIndex: Int) {
        val epochDay = date.toEpochDay()
        val existing = dao.getCompletionsForDate(epochDay).any {
            it.habitId == habitId && it.instanceIndex == instanceIndex
        }
        if (existing) {
            dao.deleteCompletionInstance(habitId, epochDay, instanceIndex)
        } else {
            dao.insertCompletion(
                HabitCompletionEntity(
                    habitId = habitId,
                    dateEpochDay = epochDay,
                    instanceIndex = instanceIndex,
                    completedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    override suspend fun isCompletedOn(habitId: Long, date: LocalDate): Boolean {
        val epochDay = date.toEpochDay()
        return dao.getCompletionsForDate(epochDay).any { it.habitId == habitId }
    }

    override suspend fun getCompletedInstancesOn(habitId: Long, date: LocalDate): Set<Int> {
        val epochDay = date.toEpochDay()
        return dao.getCompletionsForDate(epochDay)
            .filter { it.habitId == habitId }
            .map { it.instanceIndex }
            .toSet()
    }

    override suspend fun addCustomDomain(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            dao.insertCustomDomain(CustomDomainEntity(name = trimmed, createdAtMillis = System.currentTimeMillis()))
        }
    }

    override suspend fun removeCustomDomain(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            dao.deleteCustomDomain(trimmed)
            dao.clearDomainFromHabits(trimmed)
            val currentPrefs = dao.getDashboardPreferences()
            if (currentPrefs != null && currentPrefs.selectedDomain.equals(trimmed, ignoreCase = true)) {
                dao.upsertDashboardPreferences(currentPrefs.copy(selectedDomain = "All Habits"))
            }
        }
    }

    override suspend fun saveDashboardPreferences(selectedDomain: String, sortMode: HabitSortMode) {
        dao.upsertDashboardPreferences(
            DashboardPreferencesEntity(
                id = 1,
                selectedDomain = selectedDomain.ifBlank { "All Habits" },
                sortMode = sortMode.name,
            )
        )
    }

    override suspend fun rescheduleHabit(habitId: Long, fromDate: LocalDate, toDate: LocalDate): Boolean =
        rescheduleHabitWithUndo(habitId, fromDate, toDate) != null

    override suspend fun rescheduleHabitWithUndo(
        habitId: Long,
        fromDate: LocalDate,
        toDate: LocalDate,
    ): RescheduleOutcome? = rescheduleMutex.withLock {
        val current = getHabitById(habitId) ?: return null
        val updated = CadenceEvaluator.rescheduleHabitDate(current, fromDate, toDate) ?: return null
        val fromEpoch = fromDate.toEpochDay()
        val toEpoch = toDate.toEpochDay()
        val existingOnFrom = dao.getCompletionsForHabitOnDate(habitId, fromEpoch)
            .map { it.instanceIndex }
            .toSet()

        dao.updateHabitAndClearCompletionsForDates(
            habit = updated.toEntity(),
            fromEpochDay = fromEpoch,
            toEpochDay = toEpoch,
        )
        RescheduleOutcome(
            updatedHabit = updated,
            undoToken = RescheduleUndoToken(
                habitBefore = current,
                removedCompletionsOnFromDate = existingOnFrom,
                fromDate = fromDate,
                toDate = toDate,
            ),
        )
    }

    override suspend fun undoReschedule(token: RescheduleUndoToken): Habit? = rescheduleMutex.withLock {
        val current = getHabitById(token.habitBefore.id) ?: return null
        if (CadenceEvaluator.hasCadenceChanged(token.habitBefore, current)) {
            return null
        }
        val restoredHabit = CadenceEvaluator.sanitizeRescheduleOverrides(
            current.copy(
                rescheduledFromDates = token.habitBefore.rescheduledFromDates,
                rescheduledToDates = token.habitBefore.rescheduledToDates,
            ),
        )
        val fromEpoch = token.fromDate.toEpochDay()
        val toEpoch = token.toDate.toEpochDay()
        val nowMillis = System.currentTimeMillis()
        val restoredEntities = token.removedCompletionsOnFromDate.sorted().map { idx ->
            HabitCompletionEntity(
                habitId = restoredHabit.id,
                dateEpochDay = fromEpoch,
                instanceIndex = idx,
                completedAtMillis = nowMillis,
            )
        }
        dao.restoreHabitAndCompletionsForDates(
            habit = restoredHabit.toEntity(),
            fromEpochDay = fromEpoch,
            toEpochDay = toEpoch,
            restoredCompletionsOnFromDate = restoredEntities,
        )
        restoredHabit
    }
}
