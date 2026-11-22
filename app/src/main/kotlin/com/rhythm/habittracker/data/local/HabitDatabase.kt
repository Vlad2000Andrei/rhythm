package com.rhythm.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rhythm.habittracker.data.local.dao.HabitDao
import com.rhythm.habittracker.data.local.entity.CustomDomainEntity
import com.rhythm.habittracker.data.local.entity.DashboardPreferencesEntity
import com.rhythm.habittracker.data.local.entity.HabitCompletionEntity
import com.rhythm.habittracker.data.local.entity.HabitEntity
import com.rhythm.habittracker.data.local.entity.HabitPauseEntity
import com.rhythm.habittracker.data.local.entity.HabitScheduleEntity
import com.rhythm.habittracker.domain.model.HabitSchedule

@Database(
    entities = [
        HabitEntity::class,
        HabitScheduleEntity::class,
        HabitPauseEntity::class,
        HabitCompletionEntity::class,
        CustomDomainEntity::class,
        DashboardPreferencesEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: HabitDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_completions_new` (
                        `habitId` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `instanceIndex` INTEGER NOT NULL DEFAULT 0,
                        `completedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`habitId`, `dateEpochDay`, `instanceIndex`),
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `habit_completions_new` (`habitId`, `dateEpochDay`, `instanceIndex`, `completedAtMillis`)
                    SELECT `habitId`, `dateEpochDay`, 0, `completedAtMillis` FROM `habit_completions`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `habit_completions`")
                db.execSQL("ALTER TABLE `habit_completions_new` RENAME TO `habit_completions`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_dateEpochDay` ON `habit_completions` (`dateEpochDay`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `habits` ADD COLUMN `rescheduledFromEpochDaysCsv` TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE `habits` ADD COLUMN `rescheduledToEpochDaysCsv` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        internal val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Capture sqlite_sequence before rebuilding `habits` so deleted IDs are never reused
                var previousHabitsSeq = 0L
                db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'sqlite_sequence'")
                    .use { seqTableCursor ->
                        if (seqTableCursor.moveToFirst()) {
                            db.query("SELECT seq FROM `sqlite_sequence` WHERE `name` = 'habits'")
                                .use { seqCursor ->
                                    if (seqCursor.moveToFirst()) {
                                        previousHabitsSeq = seqCursor.getLong(0)
                                    }
                                }
                        }
                    }

                // 1. Back up and drop `habit_completions` so no FK references `habits` during rebuild
                db.execSQL(
                    """
                    CREATE TABLE `habit_completions_tmp` (
                        `habitId` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `instanceIndex` INTEGER NOT NULL,
                        `completedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `habit_completions_tmp` (`habitId`, `dateEpochDay`, `instanceIndex`, `completedAtMillis`)
                    SELECT `habitId`, `dateEpochDay`, `instanceIndex`, `completedAtMillis` FROM `habit_completions`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `habit_completions`")

                // 2. Stage existing schedule columns from v4 `habits` into `habit_schedules_tmp`
                db.execSQL(
                    """
                    CREATE TABLE `habit_schedules_tmp` (
                        `habitId` INTEGER NOT NULL,
                        `effectiveFromEpochDay` INTEGER NOT NULL,
                        `cadenceType` TEXT NOT NULL,
                        `scheduledDaysCsv` TEXT NOT NULL,
                        `weeklyTargetMode` TEXT NOT NULL,
                        `monthlySubMode` TEXT NOT NULL,
                        `monthlyDatesCsv` TEXT NOT NULL,
                        `ordinalWeek` INTEGER NOT NULL,
                        `ordinalDayOfWeek` INTEGER NOT NULL,
                        `endOfMonthBehavior` TEXT NOT NULL,
                        `customArchetype` TEXT NOT NULL,
                        `customInterval` INTEGER NOT NULL,
                        `customIntervalUnit` TEXT NOT NULL,
                        `cycleDaysOn` INTEGER NOT NULL,
                        `cycleDaysOff` INTEGER NOT NULL,
                        `targetPerPeriod` INTEGER NOT NULL,
                        `targetTimesJson` TEXT NOT NULL,
                        `timeOfDayBucket` TEXT NOT NULL,
                        `anchorDateEpochDay` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `habit_schedules_tmp` (
                        `habitId`, `effectiveFromEpochDay`, `cadenceType`, `scheduledDaysCsv`,
                        `weeklyTargetMode`, `monthlySubMode`, `monthlyDatesCsv`, `ordinalWeek`,
                        `ordinalDayOfWeek`, `endOfMonthBehavior`, `customArchetype`, `customInterval`,
                        `customIntervalUnit`, `cycleDaysOn`, `cycleDaysOff`, `targetPerPeriod`,
                        `targetTimesJson`, `timeOfDayBucket`, `anchorDateEpochDay`
                    )
                    SELECT
                        `id`, ?, `cadenceType`, `scheduledDaysCsv`,
                        `weeklyTargetMode`, `monthlySubMode`, `monthlyDatesCsv`, `ordinalWeek`,
                        `ordinalDayOfWeek`, `endOfMonthBehavior`, `customArchetype`, `customInterval`,
                        `customIntervalUnit`, `cycleDaysOn`, `cycleDaysOff`, `targetPerPeriod`,
                        `targetTimesJson`, `timeOfDayBucket`, `anchorDateEpochDay`
                    FROM `habits`
                    """.trimIndent(),
                    arrayOf(HabitSchedule.BASELINE_EPOCH_DAY),
                )

                // 3. Rebuild `habits` with lean identity & reschedule-override columns
                db.execSQL(
                    """
                    CREATE TABLE `habits_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `subTag` TEXT,
                        `colorHex` TEXT NOT NULL,
                        `skipReminderIfCompleted` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAtEpochDay` INTEGER NOT NULL,
                        `rescheduledFromEpochDaysCsv` TEXT NOT NULL,
                        `rescheduledToEpochDaysCsv` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `habits_new` (
                        `id`, `title`, `description`, `domain`, `subTag`, `colorHex`,
                        `skipReminderIfCompleted`, `sortOrder`, `createdAtEpochDay`,
                        `rescheduledFromEpochDaysCsv`, `rescheduledToEpochDaysCsv`
                    )
                    SELECT
                        `id`, `title`, `description`, `domain`, `subTag`, `colorHex`,
                        `skipReminderIfCompleted`, `sortOrder`, `createdAtEpochDay`,
                        `rescheduledFromEpochDaysCsv`, `rescheduledToEpochDaysCsv`
                    FROM `habits`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `habits`")
                db.execSQL("ALTER TABLE `habits_new` RENAME TO `habits`")

                // Preserve monotonic sqlite_sequence across `habits` rebuild
                if (previousHabitsSeq > 0L) {
                    db.query("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'sqlite_sequence'")
                        .use { seqTableCursor ->
                            if (seqTableCursor.moveToFirst()) {
                                val maxId = db.query("SELECT COALESCE(MAX(`id`), 0) FROM `habits`")
                                    .use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
                                val restoredSeq = maxOf(previousHabitsSeq, maxId)
                                db.execSQL("DELETE FROM `sqlite_sequence` WHERE `name` IN ('habits', 'habits_new')")
                                db.execSQL(
                                    "INSERT INTO `sqlite_sequence` (`name`, `seq`) VALUES ('habits', ?)",
                                    arrayOf(restoredSeq),
                                )
                            }
                        }
                }

                // 4. Create `habit_schedules` and restore staged baseline schedules
                db.execSQL(
                    """
                    CREATE TABLE `habit_schedules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitId` INTEGER NOT NULL,
                        `effectiveFromEpochDay` INTEGER NOT NULL,
                        `cadenceType` TEXT NOT NULL,
                        `scheduledDaysCsv` TEXT NOT NULL,
                        `weeklyTargetMode` TEXT NOT NULL,
                        `monthlySubMode` TEXT NOT NULL,
                        `monthlyDatesCsv` TEXT NOT NULL,
                        `ordinalWeek` INTEGER NOT NULL,
                        `ordinalDayOfWeek` INTEGER NOT NULL,
                        `endOfMonthBehavior` TEXT NOT NULL,
                        `customArchetype` TEXT NOT NULL,
                        `customInterval` INTEGER NOT NULL,
                        `customIntervalUnit` TEXT NOT NULL,
                        `cycleDaysOn` INTEGER NOT NULL,
                        `cycleDaysOff` INTEGER NOT NULL,
                        `targetPerPeriod` INTEGER NOT NULL,
                        `targetTimesJson` TEXT NOT NULL,
                        `timeOfDayBucket` TEXT NOT NULL,
                        `anchorDateEpochDay` INTEGER NOT NULL,
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `habit_schedules` (
                        `habitId`, `effectiveFromEpochDay`, `cadenceType`, `scheduledDaysCsv`,
                        `weeklyTargetMode`, `monthlySubMode`, `monthlyDatesCsv`, `ordinalWeek`,
                        `ordinalDayOfWeek`, `endOfMonthBehavior`, `customArchetype`, `customInterval`,
                        `customIntervalUnit`, `cycleDaysOn`, `cycleDaysOff`, `targetPerPeriod`,
                        `targetTimesJson`, `timeOfDayBucket`, `anchorDateEpochDay`
                    )
                    SELECT
                        `habitId`, `effectiveFromEpochDay`, `cadenceType`, `scheduledDaysCsv`,
                        `weeklyTargetMode`, `monthlySubMode`, `monthlyDatesCsv`, `ordinalWeek`,
                        `ordinalDayOfWeek`, `endOfMonthBehavior`, `customArchetype`, `customInterval`,
                        `customIntervalUnit`, `cycleDaysOn`, `cycleDaysOff`, `targetPerPeriod`,
                        `targetTimesJson`, `timeOfDayBucket`, `anchorDateEpochDay`
                    FROM `habit_schedules_tmp`
                    WHERE `habitId` IN (SELECT `id` FROM `habits`)
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `habit_schedules_tmp`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_schedules_habitId_effectiveFromEpochDay` ON `habit_schedules` (`habitId`, `effectiveFromEpochDay`)",
                )

                // 5. Create `habit_pauses`
                db.execSQL(
                    """
                    CREATE TABLE `habit_pauses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitId` INTEGER NOT NULL,
                        `startEpochDay` INTEGER NOT NULL,
                        `endEpochDay` INTEGER NOT NULL,
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_habit_pauses_habitId` ON `habit_pauses` (`habitId`)",
                )

                // 6. Recreate `habit_completions` with FK + index and restore all rows
                db.execSQL(
                    """
                    CREATE TABLE `habit_completions` (
                        `habitId` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `instanceIndex` INTEGER NOT NULL,
                        `completedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`habitId`, `dateEpochDay`, `instanceIndex`),
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `habit_completions` (`habitId`, `dateEpochDay`, `instanceIndex`, `completedAtMillis`)
                    SELECT `habitId`, `dateEpochDay`, `instanceIndex`, `completedAtMillis`
                    FROM `habit_completions_tmp`
                    WHERE `habitId` IN (SELECT `id` FROM `habits`)
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `habit_completions_tmp`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_habit_completions_dateEpochDay` ON `habit_completions` (`dateEpochDay`)",
                )
            }
        }

        fun getInstance(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "rhythm_habits.db",
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
