package com.rhythm.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val domain: String,
    val subTag: String?,
    val colorHex: String,
    val cadenceType: String,
    val scheduledDaysCsv: String,
    val weeklyTargetMode: String,
    val monthlySubMode: String,
    val monthlyDatesCsv: String,
    val ordinalWeek: Int,
    val ordinalDayOfWeek: Int,
    val endOfMonthBehavior: String,
    val customArchetype: String,
    val customInterval: Int,
    val customIntervalUnit: String,
    val cycleDaysOn: Int,
    val cycleDaysOff: Int,
    val targetPerPeriod: Int,
    val targetTimesJson: String,
    val skipReminderIfCompleted: Boolean,
    val timeOfDayBucket: String,
    val sortOrder: Int,
    val anchorDateEpochDay: Long,
    val createdAtEpochDay: Long,
    val rescheduledFromEpochDaysCsv: String = "",
    val rescheduledToEpochDaysCsv: String = "",
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "dateEpochDay", "instanceIndex"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dateEpochDay")],
)
data class HabitCompletionEntity(
    val habitId: Long,
    val dateEpochDay: Long,
    val instanceIndex: Int = 0,
    val completedAtMillis: Long,
)

@Entity(tableName = "custom_domains")
data class CustomDomainEntity(
    @PrimaryKey
    val name: String,
    val createdAtMillis: Long,
)

@Entity(tableName = "dashboard_preferences")
data class DashboardPreferencesEntity(
    @PrimaryKey
    val id: Int = 1,
    val selectedDomain: String = "All Habits",
    val sortMode: String = "FLOW",
)
