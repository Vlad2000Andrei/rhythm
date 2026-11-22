package com.rhythm.habittracker.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String,
    val domain: String,
    val subTag: String?,
    val colorHex: String,
    val skipReminderIfCompleted: Boolean,
    val sortOrder: Int,
    val createdAtEpochDay: Long,
    val rescheduledFromEpochDaysCsv: String = "",
    val rescheduledToEpochDaysCsv: String = "",
)

@Entity(
    tableName = "habit_schedules",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habitId", "effectiveFromEpochDay"], unique = true),
    ],
)
data class HabitScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val habitId: Long,
    val effectiveFromEpochDay: Long,
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
    val timeOfDayBucket: String,
    val anchorDateEpochDay: Long,
)

@Entity(
    tableName = "habit_pauses",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["habitId"]),
    ],
)
data class HabitPauseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val habitId: Long,
    val startEpochDay: Long,
    val endEpochDay: Long,
)

data class HabitWithDetails(
    @Embedded val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId",
    )
    val schedules: List<HabitScheduleEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId",
    )
    val pauses: List<HabitPauseEntity>,
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
