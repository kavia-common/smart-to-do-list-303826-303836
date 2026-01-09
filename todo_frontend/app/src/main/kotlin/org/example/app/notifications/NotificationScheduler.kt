package org.example.app.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.example.app.data.TaskEntity

object NotificationScheduler {

    private const val CHANNEL_ID = "due_tasks"
    private const val CHANNEL_NAME = "Due tasks"
    private const val CHANNEL_DESC = "Notifications for tasks that are due"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = CHANNEL_DESC
        }
        nm.createNotificationChannel(channel)
    }

    fun scheduleOrCancelForTask(context: Context, task: TaskEntity) {
        // Only schedule if has future due date and not completed
        val due = task.dueAtMillis
        if (due == null || task.isCompleted) {
            cancelForTask(context, task.id)
            return
        }
        if (due <= System.currentTimeMillis()) {
            // If already due, we still schedule immediate-ish to deliver.
            schedule(context, task.id, System.currentTimeMillis() + 1000L)
        } else {
            schedule(context, task.id, due)
        }
    }

    fun cancelForTask(context: Context, taskId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntentForTask(context, taskId))
    }

    private fun schedule(context: Context, taskId: Long, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntentForTask(context, taskId)

        // Exact alarms may require special permission on some OEMs; use setExactAndAllowWhileIdle when available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun pendingIntentForTask(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, DueTaskAlarmReceiver::class.java).apply {
            action = DueTaskAlarmReceiver.ACTION_NOTIFY_DUE_TASK
            putExtra(DueTaskAlarmReceiver.EXTRA_TASK_ID, taskId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, taskId.toInt(), intent, flags)
    }

    fun channelId(): String = CHANNEL_ID
}
