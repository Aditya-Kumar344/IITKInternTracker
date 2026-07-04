package com.internmail.tracker.mail

import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.internmail.tracker.data.SecurePrefs
import com.internmail.tracker.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps a live IMAP IDLE connection open so new qualifying emails are
 * detected within seconds instead of waiting for the next 15-minute
 * WorkManager pass. Runs as a foreground service with a low-priority
 * "watching for new mail" notification (separate from the deadline
 * reminder notification).
 */
class MailSyncForegroundService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val prefs = SecurePrefs(this)
        if (!prefs.isLoggedIn) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NotificationHelper.SYNC_SERVICE_NOTIFICATION_ID, NotificationHelper.buildSyncServiceNotification(this))

        lifecycleScope.launch(Dispatchers.IO) {
            val client = ImapClient(prefs)
            while (isActive) {
                try {
                    client.connect()
                    // Do one sync pass in case anything arrived while we were disconnected.
                    MailSyncEngine.syncOnce(applicationContext)
                    // Then idle-wait for push notifications from the server.
                    client.idleWait(20 * 60 * 1000)
                } catch (e: Exception) {
                    // network hiccup / server closed idle — back off and retry
                    kotlinx.coroutines.delay(30_000)
                } finally {
                    client.disconnect()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
