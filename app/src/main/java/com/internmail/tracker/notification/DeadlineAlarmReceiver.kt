package com.internmail.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.internmail.tracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeadlineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        if (eventId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).eventDao()
                val event = dao.getById(eventId)
                if (event != null && !event.opened) {
                    NotificationHelper.ensureChannels(context)
                    val notification = NotificationHelper.buildDeadlineNotification(
                        context, event.id, event.companyName, event.link
                    )
                    NotificationManagerCompat.from(context)
                        .notify((NotificationHelper.DEADLINE_SERVICE_NOTIFICATION_ID_BASE + event.id).toInt(), notification)
                    dao.update(event.copy(deadlineNotificationActive = true))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
    }
}
