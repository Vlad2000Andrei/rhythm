package com.rhythm.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rhythm.habittracker.data.local.entity.CustomDomainEntity
import com.rhythm.habittracker.data.local.entity.DashboardPreferencesEntity
import com.rhythm.habittracker.data.local.entity.HabitCompletionEntity
import com.rhythm.habittracker.data.local.entity.HabitEntity
import com.rhythm.habittracker.data.local.entity.HabitPauseEntity
import com.rhythm.habittracker.data.local.entity.HabitScheduleEntity
import com.rhythm.habittracker.data.local.entity.HabitWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Transaction
    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    fun observeHabitsWithDetails(): Flow<List<HabitWithDetails>>

    @Transaction
    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllHabitsWithDetails(): List<HabitWithDetails>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitWithDetailsById(habitId: Long): HabitWithDetails?

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getHabitCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET rescheduledFromEpochDaysCsv = :fromCsv, rescheduledToEpochDaysCsv = :toCsv WHERE id = :habitId")
    suspend fun updateRescheduleOverrides(habitId: Long, fromCsv: String, toCsv: String)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSchedules(schedules: List<HabitScheduleEntity>)

    @Query("DELETE FROM habit_schedules WHERE habitId = :habitId")
    suspend fun deleteSchedulesForHabit(habitId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPauses(pauses: List<HabitPauseEntity>)

    @Query("DELETE FROM habit_pauses WHERE habitId = :habitId")
    suspend fun deletePausesForHabit(habitId: Long)

    @Transaction
    suspend fun upsertHabitWithDetails(
        habit: HabitEntity,
        schedules: List<HabitScheduleEntity>,
        pauses: List<HabitPauseEntity>,
    ): Long {
        val savedId = if (habit.id == 0L) {
            insertHabit(habit)
        } else {
            updateHabit(habit)
            habit.id
        }
        deleteSchedulesForHabit(savedId)
        if (schedules.isNotEmpty()) {
            insertSchedules(schedules.map { it.copy(id = 0L, habitId = savedId) })
        }
        deletePausesForHabit(savedId)
        if (pauses.isNotEmpty()) {
            insertPauses(pauses.map { it.copy(id = 0L, habitId = savedId) })
        }
        return savedId
    }

    @Transaction
    suspend fun replacePausesForHabit(
        habitId: Long,
        pauses: List<HabitPauseEntity>,
    ) {
        deletePausesForHabit(habitId)
        if (pauses.isNotEmpty()) {
            insertPauses(pauses.map { it.copy(id = 0L, habitId = habitId) })
        }
    }

    @Query("SELECT * FROM habit_completions")
    fun observeAllCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllCompletions(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay = :epochDay")
    suspend fun getCompletionsForDate(epochDay: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay")
    suspend fun getCompletionsForHabitOnDate(habitId: Long, epochDay: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    suspend fun getCompletionsForHabitInRange(
        habitId: Long,
        startEpochDay: Long,
        endEpochDay: Long,
    ): List<HabitCompletionEntity>

    @Transaction
    suspend fun updateRescheduleAndClearCompletionsForDates(
        habitId: Long,
        fromCsv: String,
        toCsv: String,
        fromEpochDay: Long,
        toEpochDay: Long,
    ) {
        updateRescheduleOverrides(habitId, fromCsv, toCsv)
        deleteCompletion(habitId, fromEpochDay)
        deleteCompletion(habitId, toEpochDay)
    }

    @Transaction
    suspend fun restoreRescheduleAndCompletionsForDates(
        habitId: Long,
        fromCsv: String,
        toCsv: String,
        fromEpochDay: Long,
        toEpochDay: Long,
        restoredCompletionsOnFromDate: List<HabitCompletionEntity>,
    ) {
        updateRescheduleOverrides(habitId, fromCsv, toCsv)
        deleteCompletion(habitId, fromEpochDay)
        deleteCompletion(habitId, toEpochDay)
        if (restoredCompletionsOnFromDate.isNotEmpty()) {
            insertCompletions(restoredCompletionsOnFromDate)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: HabitCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletions(completions: List<HabitCompletionEntity>)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay")
    suspend fun deleteCompletion(habitId: Long, epochDay: Long)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay AND instanceIndex = :instanceIndex")
    suspend fun deleteCompletionInstance(habitId: Long, epochDay: Long, instanceIndex: Int)

    @Query("SELECT * FROM custom_domains ORDER BY createdAtMillis ASC")
    fun observeCustomDomains(): Flow<List<CustomDomainEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomDomain(domain: CustomDomainEntity)

    @Query("DELETE FROM custom_domains WHERE name = :name")
    suspend fun deleteCustomDomain(name: String)

    @Query("UPDATE habits SET domain = '' WHERE domain = :name")
    suspend fun clearDomainFromHabits(name: String)

    @Query("SELECT * FROM dashboard_preferences WHERE id = 1 LIMIT 1")
    fun observeDashboardPreferences(): Flow<DashboardPreferencesEntity?>

    @Query("SELECT * FROM dashboard_preferences WHERE id = 1 LIMIT 1")
    suspend fun getDashboardPreferences(): DashboardPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDashboardPreferences(prefs: DashboardPreferencesEntity)
}
