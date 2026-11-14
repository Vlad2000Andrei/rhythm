package com.rhythm.habittracker

import android.app.Application
import android.content.Context
import com.rhythm.habittracker.data.HabitRepository
import com.rhythm.habittracker.data.RoomHabitRepository
import com.rhythm.habittracker.data.local.DataStoreDraftHabitStore
import com.rhythm.habittracker.data.local.DraftHabitStore
import com.rhythm.habittracker.data.local.HabitDatabase
import com.rhythm.habittracker.reminders.AlarmHabitReminderScheduler
import com.rhythm.habittracker.reminders.ReminderScheduler
import com.rhythm.habittracker.widget.RhythmWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val database: HabitDatabase by lazy { HabitDatabase.getInstance(context) }

    val habitRepository: HabitRepository by lazy {
        RoomHabitRepository(database.habitDao())
    }

    val draftHabitStore: DraftHabitStore by lazy {
        DataStoreDraftHabitStore(context)
    }

    val reminderScheduler: ReminderScheduler by lazy {
        AlarmHabitReminderScheduler(context)
    }
}

class RhythmApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        applicationScope.launch {
            combine(
                container.habitRepository.observeHabits(),
                container.habitRepository.observeCompletedInstancesByDate(),
            ) { habits, completedInstancesByDate ->
                habits to completedInstancesByDate
            }.collect { (habits, completedInstancesByDate) ->
                RhythmWidgetUpdater.updateAllWidgetsWithSnapshot(
                    context = this@RhythmApplication,
                    habits = habits,
                    completedInstancesByDate = completedInstancesByDate,
                )
            }
        }
    }
}
