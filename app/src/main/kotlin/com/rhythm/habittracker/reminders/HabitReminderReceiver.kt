package com.rhythm.habittracker.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rhythm.habittracker.MainActivity
import com.rhythm.habittracker.R
import com.rhythm.habittracker.RhythmApplication
import com.rhythm.habittracker.domain.CadenceEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(AlarmHabitReminderScheduler.EXTRA_HABIT_ID, -1L)
        val slotIndex = intent.getIntExtra(AlarmHabitReminderScheduler.EXTRA_SLOT_INDEX, 0)
        if (habitId <= 0L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as? RhythmApplication
        if (app == null) {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = app.container.habitRepository
                val habit = repo.getHabitById(habitId) ?: return@launch
                val today = LocalDate.now()
                val isScheduledToday = CadenceEvaluator.isHabitScheduledOn(habit, today)
                val completedInstances = repo.getCompletedInstancesOn(habitId, today)
                val isCompletedToday = slotIndex in completedInstances ||
                    completedInstances.size >= habit.dailyTargetCount

                if (isScheduledToday && !(habit.skipReminderIfCompleted && isCompletedToday)) {
                    showNotification(context, habit.id, slotIndex, habit.title, habit.description)
                }

                // Schedule next day's reminder slot
                app.container.reminderScheduler.scheduleHabitReminders(habit)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        habitId: Long,
        slotIndex: Int,
        title: String,
        description: String,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Habit Target Cues",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            this.description = "Scheduled reminders for your active habits"
        }
        manager.createNotificationChannel(channel)

        val notificationId = AlarmHabitReminderScheduler.notificationIdFor(habitId, slotIndex)
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val extras = Bundle().apply {
            putLong(AlarmHabitReminderScheduler.EXTRA_HABIT_ID, habitId)
            putInt(AlarmHabitReminderScheduler.EXTRA_SLOT_INDEX, slotIndex)
        }
        val bodyText = description.ifBlank { "Scheduled session cue" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addExtras(extras)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    companion object {
        private const val CHANNEL_ID = "rhythm_habit_cues"
    }
}
