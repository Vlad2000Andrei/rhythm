package com.rhythm.habittracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.rhythm.habittracker.domain.model.Habit
import com.rhythm.habittracker.domain.model.TargetTimeSlot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

interface ReminderScheduler {
    fun scheduleHabitReminders(habit: Habit)
    fun cancelHabitReminders(habit: Habit)
}

class AlarmHabitReminderScheduler(
    private val context: Context,
) : ReminderScheduler {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun scheduleHabitReminders(habit: Habit) {
        val manager = alarmManager ?: return
        habit.targetTimes.forEachIndexed { index, slot ->
            val pendingIntent = buildPendingIntent(habit.id, slot.id, index, habit.title)
            if (!slot.enabled) {
                manager.cancel(pendingIntent)
                return@forEachIndexed
            }
            val triggerMillis = computeNextTriggerMillis(slot)
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent,
            )
        }
    }

    override fun cancelHabitReminders(habit: Habit) {
        val manager = alarmManager ?: return
        habit.targetTimes.forEachIndexed { index, slot ->
            val pendingIntent = buildPendingIntent(habit.id, slot.id, index, habit.title)
            manager.cancel(pendingIntent)
        }
    }

    private fun computeNextTriggerMillis(slot: TargetTimeSlot): Long {
        val now = LocalDateTime.now()
        var candidate = LocalDate.now().atTime(slot.hour.coerceIn(0, 23), slot.minute.coerceIn(0, 59))
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun buildPendingIntent(
        habitId: Long,
        slotId: String,
        slotIndex: Int,
        habitTitle: String,
    ): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_SLOT_ID, slotId)
            putExtra(EXTRA_SLOT_INDEX, slotIndex)
            putExtra(EXTRA_HABIT_TITLE, habitTitle)
        }
        val requestCode = ((habitId * 100L) + slotIndex).toInt()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.rhythm.habittracker.ACTION_HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_SLOT_ID = "extra_slot_id"
        const val EXTRA_SLOT_INDEX = "extra_slot_index"
        const val EXTRA_HABIT_TITLE = "extra_habit_title"
    }
}
