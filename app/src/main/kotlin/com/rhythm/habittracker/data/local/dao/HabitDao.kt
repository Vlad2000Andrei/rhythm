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
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllHabits(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Long): HabitEntity?

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getHabitCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>): List<Long>

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Query("SELECT * FROM habit_completions")
    fun observeAllCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions")
    suspend fun getAllCompletions(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay = :epochDay")
    suspend fun getCompletionsForDate(epochDay: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :epochDay")
    suspend fun getCompletionsForHabitOnDate(habitId: Long, epochDay: Long): List<HabitCompletionEntity>

    @Transaction
    suspend fun updateHabitAndClearCompletionsForDates(
        habit: HabitEntity,
        fromEpochDay: Long,
        toEpochDay: Long,
    ) {
        updateHabit(habit)
        deleteCompletion(habit.id, fromEpochDay)
        deleteCompletion(habit.id, toEpochDay)
    }

    @Transaction
    suspend fun restoreHabitAndCompletionsForDates(
        habit: HabitEntity,
        fromEpochDay: Long,
        toEpochDay: Long,
        restoredCompletionsOnFromDate: List<HabitCompletionEntity>,
    ) {
        updateHabit(habit)
        deleteCompletion(habit.id, fromEpochDay)
        deleteCompletion(habit.id, toEpochDay)
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
