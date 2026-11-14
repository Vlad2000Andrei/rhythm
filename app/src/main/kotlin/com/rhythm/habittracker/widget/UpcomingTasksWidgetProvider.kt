package com.rhythm.habittracker.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.rhythm.habittracker.RhythmApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class UpcomingTasksWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RhythmWidgetUpdater.updateUpcomingTasksWidgets(context, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RhythmWidgetUpdater.updateUpcomingTasksWidgets(context, intArrayOf(appWidgetId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_TASK) {
            val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
            val instanceIndex = intent.getIntExtra(EXTRA_INSTANCE_INDEX, 0)
            if (habitId <= 0L) return

            val pendingResult = goAsync()
            val app = context.applicationContext as? RhythmApplication
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = app?.container?.habitRepository
                    if (repo != null) {
                        val today = LocalDate.now()
                        val habit = repo.getHabitById(habitId)
                        if (habit != null && today !in habit.rescheduledFromDates) {
                            repo.toggleCompletion(habitId, today, instanceIndex)
                        }
                    }
                    RhythmWidgetUpdater.updateAllWidgets(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetPreferencesStore.deleteConfig(context, it) }
    }

    companion object {
        const val ACTION_TOGGLE_TASK = "com.rhythm.habittracker.widget.ACTION_TOGGLE_TASK"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_INSTANCE_INDEX = "extra_instance_index"
    }
}
