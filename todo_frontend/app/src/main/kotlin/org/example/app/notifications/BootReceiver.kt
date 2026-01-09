package org.example.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.app.data.TodoDatabase

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = TodoDatabase.getInstance(context)
            val tasks = db.taskDao().getAll()
            tasks.forEach { task ->
                NotificationScheduler.scheduleOrCancelForTask(context, task)
            }
        }
    }
}
