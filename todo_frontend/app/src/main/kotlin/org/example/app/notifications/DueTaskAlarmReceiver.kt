package org.example.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import org.example.app.MainActivity
import org.example.app.R
import org.example.app.data.TodoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DueTaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NOTIFY_DUE_TASK) return

        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        NotificationScheduler.ensureNotificationChannel(context)

        // Fetch task from DB (receiver must return quickly; use coroutine on IO).
        CoroutineScope(Dispatchers.IO).launch {
            val db = TodoDatabase.getInstance(context)
            val task = db.taskDao().getById(taskId) ?: return@launch
            if (task.isCompleted) return@launch

            val contentIntent = Intent(context, MainActivity::class.java)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (android.os.Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
            val pi = PendingIntent.getActivity(context, 0, contentIntent, flags)

            val notification = NotificationCompat.Builder(context, NotificationScheduler.channelId())
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Task due")
                .setContentText(task.title)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(taskId.toInt(), notification)
        }
    }

    companion object {
        const val ACTION_NOTIFY_DUE_TASK = "org.example.app.notifications.ACTION_NOTIFY_DUE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
