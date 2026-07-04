package com.internmail.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import com.internmail.tracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_OPEN_LINK) return
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val link = intent.getStringExtra(EXTRA_LINK)

        // Cancel the ongoing notification — an app can always cancel its own
        // notifications programmatically, even ones marked setOngoing(true);
        // that flag only blocks the *user's* swipe-to-dismiss gesture.
        if (eventId != -1L) {
            NotificationManagerCompat.from(context)
                .cancel((NotificationHelper.DEADLINE_SERVICE_NOTIFICATION_ID_BASE + eventId).toInt())
        }

        if (!link.isNullOrBlank()) {
            val openLink = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(openLink)
        }

        if (eventId != -1L) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getInstance(context).eventDao()
                    dao.getById(eventId)?.let {
                        dao.update(it.copy(opened = true, deadlineNotificationActive = false))
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_OPEN_LINK = "com.internmail.tracker.ACTION_OPEN_LINK"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_LINK = "extra_link"
    }
}
