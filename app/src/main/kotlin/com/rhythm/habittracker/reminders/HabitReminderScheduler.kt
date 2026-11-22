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
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val todaySchedule = habit.scheduleForDate(today)
        val isPausedToday = habit.isPausedOn(today)

        for (index in 0 until MAX_MANAGED_SLOTS) {
            val todaySlot = todaySchedule.targetTimes.getOrNull(index)
            val todayTrigger = if (!isPausedToday && todaySlot != null && todaySlot.enabled) {
                today.atTime(todaySlot.hour.coerceIn(0, 23), todaySlot.minute.coerceIn(0, 59))
            } else {
                null
            }

            if (todayTrigger != null && todaySlot != null && todayTrigger.isAfter(now.plusSeconds(30))) {
                scheduleSlotAt(manager, habit, index, todaySlot, todayTrigger)
                continue
            }

            val nextDate = nextUnpausedDateFrom(habit, tomorrow)
            val nextSlot = habit.scheduleForDate(nextDate).targetTimes.getOrNull(index)
            if (nextSlot != null && nextSlot.enabled) {
                val nextTrigger = nextDate.atTime(
                    nextSlot.hour.coerceIn(0, 23),
                    nextSlot.minute.coerceIn(0, 59),
                )
                scheduleSlotAt(manager, habit, index, nextSlot, nextTrigger)
            } else {
                cancelSlotIfExists(manager, habit.id, index)
            }
        }
    }

    override fun cancelHabitReminders(habit: Habit) {
        val manager = alarmManager ?: return
        for (index in 0 until MAX_MANAGED_SLOTS) {
            cancelSlotIfExists(manager, habit.id, index)
        }
    }

    private fun scheduleSlotAt(
        manager: AlarmManager,
        habit: Habit,
        slotIndex: Int,
        slot: TargetTimeSlot,
        triggerTime: LocalDateTime,
    ) {
        val pendingIntent = buildPendingIntent(
            habitId = habit.id,
            slotId = slot.id,
            slotIndex = slotIndex,
            habitTitle = habit.title,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        val triggerMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        manager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent,
        )
    }

    private fun cancelSlotIfExists(manager: AlarmManager, habitId: Long, slotIndex: Int) {
        val existing = buildPendingIntent(
            habitId = habitId,
            slotId = "",
            slotIndex = slotIndex,
            habitTitle = "",
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (existing != null) {
            manager.cancel(existing)
            existing.cancel()
        }
    }

    private fun nextUnpausedDateFrom(habit: Habit, startDate: LocalDate): LocalDate {
        var cursor = startDate
        repeat(64) {
            val activePause = habit.pauseWindows.firstOrNull { it.contains(cursor) } ?: return cursor
            cursor = activePause.endDate.plusDays(1)
        }
        return cursor
    }

    private fun buildPendingIntent(
        habitId: Long,
        slotId: String,
        slotIndex: Int,
        habitTitle: String,
        flags: Int,
    ): PendingIntent? {
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
            flags,
        )
    }

    companion object {
        const val MAX_MANAGED_SLOTS = 20
        const val ACTION_HABIT_REMINDER = "com.rhythm.habittracker.ACTION_HABIT_REMINDER"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_SLOT_ID = "extra_slot_id"
        const val EXTRA_SLOT_INDEX = "extra_slot_index"
        const val EXTRA_HABIT_TITLE = "extra_habit_title"
    }
}
