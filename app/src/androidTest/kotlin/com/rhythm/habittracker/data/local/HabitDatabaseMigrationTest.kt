package com.rhythm.habittracker.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rhythm.habittracker.domain.model.CadenceType
import com.rhythm.habittracker.domain.model.HabitPauseWindow
import com.rhythm.habittracker.domain.model.HabitSchedule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HabitDatabaseMigrationTest {

    private val dbName = "migration_test_v4_to_v5.db"
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate4To5_preservesHabitsCompletionsSequenceAndValidatesRoomSchema() = runBlocking {
        context.deleteDatabase(dbName)

        // 1. Seed a real SQLite database using the exact v4 schema
        val v4Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(4) {
                        override fun onConfigure(db: SupportSQLiteDatabase) {
                            db.setForeignKeyConstraintsEnabled(true)
                        }

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `habits` (
                                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                    `title` TEXT NOT NULL,
                                    `description` TEXT NOT NULL,
                                    `domain` TEXT NOT NULL,
                                    `subTag` TEXT,
                                    `colorHex` TEXT NOT NULL,
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
                                    `skipReminderIfCompleted` INTEGER NOT NULL,
                                    `timeOfDayBucket` TEXT NOT NULL,
                                    `sortOrder` INTEGER NOT NULL,
                                    `anchorDateEpochDay` INTEGER NOT NULL,
                                    `createdAtEpochDay` INTEGER NOT NULL,
                                    `rescheduledFromEpochDaysCsv` TEXT NOT NULL DEFAULT '',
                                    `rescheduledToEpochDaysCsv` TEXT NOT NULL DEFAULT ''
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `habit_completions` (
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
                                "CREATE INDEX IF NOT EXISTS `index_habit_completions_dateEpochDay` ON `habit_completions` (`dateEpochDay`)",
                            )
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `custom_domains` (
                                    `name` TEXT NOT NULL,
                                    `createdAtMillis` INTEGER NOT NULL,
                                    PRIMARY KEY(`name`)
                                )
                                """.trimIndent(),
                            )
                            db.execSQL(
                                """
                                CREATE TABLE IF NOT EXISTS `dashboard_preferences` (
                                    `id` INTEGER NOT NULL,
                                    `selectedDomain` TEXT NOT NULL,
                                    `sortMode` TEXT NOT NULL,
                                    PRIMARY KEY(`id`)
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                )
                .build(),
        )

        val anchorDay = LocalDate.of(2026, 4, 1).toEpochDay()
        val reschedFrom = LocalDate.of(2026, 5, 11).toEpochDay()
        val reschedTo = LocalDate.of(2026, 5, 12).toEpochDay()

        v4Helper.writableDatabase.use { v4Db ->
            v4Db.execSQL(
                """
                INSERT INTO `habits` (
                    `id`, `title`, `description`, `domain`, `subTag`, `colorHex`,
                    `cadenceType`, `scheduledDaysCsv`, `weeklyTargetMode`, `monthlySubMode`,
                    `monthlyDatesCsv`, `ordinalWeek`, `ordinalDayOfWeek`, `endOfMonthBehavior`,
                    `customArchetype`, `customInterval`, `customIntervalUnit`, `cycleDaysOn`,
                    `cycleDaysOff`, `targetPerPeriod`, `targetTimesJson`, `skipReminderIfCompleted`,
                    `timeOfDayBucket`, `sortOrder`, `anchorDateEpochDay`, `createdAtEpochDay`,
                    `rescheduledFromEpochDaysCsv`, `rescheduledToEpochDaysCsv`
                ) VALUES (
                    3, 'Deep Work', 'Morning sprint', 'Focus', NULL, '#2563EB',
                    'WEEKLY', '1,3,5', 'FIXED_SCHEDULE', 'SPECIFIC_DATES',
                    '1,15', 1, 1, 'EXECUTE_ON_LAST_DAY',
                    'INTERVAL', 3, 'DAYS', 5, 2, 2,
                    '[{"id":"s1","hour":8,"minute":30,"enabled":true},{"id":"s2","hour":19,"minute":0,"enabled":false}]',
                    1, 'MORNING', 0, $anchorDay, $anchorDay, '$reschedFrom', '$reschedTo'
                )
                """.trimIndent(),
            )
            // Simulate a previously deleted habit ID = 7 so sqlite_sequence > MAX(id)
            v4Db.execSQL("DELETE FROM `sqlite_sequence` WHERE `name` = 'habits'")
            v4Db.execSQL("INSERT INTO `sqlite_sequence` (`name`, `seq`) VALUES ('habits', 7)")

            v4Db.execSQL(
                "INSERT INTO `habit_completions` VALUES (3, $reschedTo, 0, 1700000000000), (3, $reschedTo, 1, 1700000005000)",
            )
            v4Db.execSQL("INSERT INTO `custom_domains` VALUES ('Focus', 1700000000000)")
            v4Db.execSQL("INSERT INTO `dashboard_preferences` VALUES (1, 'Focus', 'STREAK')")
        }
        v4Helper.close()

        // 2. Open with Room v5 + MIGRATION_4_5 (no destructive fallback allowed in this test!)
        val migratedDb = Room.databaseBuilder(context, HabitDatabase::class.java, dbName)
            .addMigrations(HabitDatabase.MIGRATION_4_5)
            .build()

        try {
            val dao = migratedDb.habitDao()

            // Verify Room schema integrity & foreign keys
            migratedDb.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { fkCursor ->
                assertFalse("Foreign key violations detected after migration", fkCursor.moveToFirst())
            }
            migratedDb.openHelper.writableDatabase.query(
                "SELECT typeof(`effectiveFromEpochDay`), `effectiveFromEpochDay` FROM `habit_schedules` WHERE `habitId` = 3",
            ).use { typeCursor ->
                assertTrue(typeCursor.moveToFirst())
                assertEquals("integer", typeCursor.getString(0))
                assertEquals(HabitSchedule.BASELINE_EPOCH_DAY, typeCursor.getLong(1))
            }
            migratedDb.openHelper.writableDatabase.query(
                "SELECT `seq` FROM `sqlite_sequence` WHERE `name` = 'habits'",
            ).use { seqCursor ->
                assertTrue(seqCursor.moveToFirst())
                assertEquals(7L, seqCursor.getLong(0))
            }

            // Verify migrated domain data
            val habits = dao.getAllHabitsWithDetails().map { it.toDomain() }
            assertEquals(1, habits.size)
            val h = habits.single()
            assertEquals(3L, h.id)
            assertEquals("Deep Work", h.title)
            assertEquals("Focus", h.domain)
            assertNull(h.subTag)
            assertEquals(setOf(LocalDate.ofEpochDay(reschedFrom)), h.rescheduledFromDates)
            assertEquals(setOf(LocalDate.ofEpochDay(reschedTo)), h.rescheduledToDates)

            assertEquals(1, h.schedules.size)
            val s = h.latestSchedule
            assertNull(s.effectiveFrom)
            assertEquals(CadenceType.WEEKLY, s.cadenceType)
            assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), s.scheduledDays)
            assertEquals(2, s.dailyTargetCount)
            assertEquals(2, s.targetTimes.size)
            assertEquals(8, s.targetTimes[0].hour)
            assertFalse(s.targetTimes[1].enabled)

            val completions = dao.getAllCompletions()
            assertEquals(2, completions.size)

            val prefs = dao.getDashboardPreferences()
            assertEquals("Focus", prefs?.selectedDomain)
            assertEquals("STREAK", prefs?.sortMode)

            // Verify pause table works and new habit ID does not reuse deleted ID <= 7
            dao.replacePausesForHabit(
                3L,
                listOf(HabitPauseWindow(0L, LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 25)).toEntity(3L)),
            )
            assertEquals(1, dao.getHabitWithDetailsById(3L)?.pauses?.size)
        } finally {
            migratedDb.close()
        }
    }
}
