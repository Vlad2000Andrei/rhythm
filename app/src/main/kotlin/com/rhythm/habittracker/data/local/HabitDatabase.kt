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

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        CustomDomainEntity::class,
        DashboardPreferencesEntity::class,
    ],
    version = 4,
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

        fun getInstance(context: Context): HabitDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "rhythm_habits.db",
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
