package com.internmail.tracker.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.internmail.tracker.data.AppDatabase
import com.internmail.tracker.data.SecurePrefs
import com.internmail.tracker.mail.MailSyncForegroundService
import com.internmail.tracker.mail.MailSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = SecurePrefs(context)
        if (!prefs.isLoggedIn) return

        MailSyncWorker.schedule(context)
        context.startForegroundService(Intent(context, MailSyncForegroundService::class.java))

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).eventDao()
                val upcoming = dao.getUpcoming(System.currentTimeMillis())
                upcoming.forEach { AlarmScheduler.scheduleForEvent(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
