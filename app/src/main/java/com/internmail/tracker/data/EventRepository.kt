package com.internmail.tracker.data

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.internmail.tracker.mail.MailSyncForegroundService
import com.internmail.tracker.mail.MailSyncWorker
import com.internmail.tracker.notification.AlarmScheduler
import com.internmail.tracker.notification.NotificationHelper
import kotlinx.coroutines.flow.Flow

class EventRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).eventDao()
    val prefs = SecurePrefs(context)

    fun observeEvents(): Flow<List<Event>> = dao.observeAll()

    suspend fun getById(id: Long) = dao.getById(id)

    /** Add or update an event (manual entry from the UI) and (re)schedule its deadline alarm. */
    suspend fun saveEvent(event: Event): Long {
        val id = dao.insert(event)
        val saved = event.copy(id = if (event.id != 0L) event.id else id)
        AlarmScheduler.scheduleForEvent(context, saved)
        return id
    }

    suspend fun deleteEvent(event: Event) {
        AlarmScheduler.cancelForEvent(context, event)
        NotificationManagerCompat.from(context)
            .cancel((NotificationHelper.DEADLINE_SERVICE_NOTIFICATION_ID_BASE + event.id).toInt())
        dao.delete(event)
    }

    /** Called when the user opens the link from within the app (dashboard/detail screen). */
    suspend fun markOpened(event: Event) {
        AlarmScheduler.cancelForEvent(context, event)
        NotificationManagerCompat.from(context)
            .cancel((NotificationHelper.DEADLINE_SERVICE_NOTIFICATION_ID_BASE + event.id).toInt())
        dao.update(event.copy(opened = true, deadlineNotificationActive = false))
    }

    // --- Login / logout ---

    fun login(email: String, password: String, imapHost: String, imapPort: Int) {
        prefs.email = email
        prefs.password = password
        prefs.imapHost = imapHost
        prefs.imapPort = imapPort
        NotificationHelper.ensureChannels(context)
        MailSyncWorker.schedule(context)
        context.startForegroundService(
            android.content.Intent(context, MailSyncForegroundService::class.java)
        )
    }

    fun logout() {
        MailSyncWorker.cancel(context)
        context.stopService(android.content.Intent(context, MailSyncForegroundService::class.java))
        prefs.clear()
    }
}
