package com.internmail.tracker.mail

import android.content.Context
import android.util.Log
import com.internmail.tracker.data.AppDatabase
import com.internmail.tracker.data.Event
import com.internmail.tracker.data.EventSource
import com.internmail.tracker.data.SecurePrefs
import com.internmail.tracker.notification.AlarmScheduler
import com.internmail.tracker.notification.NotificationHelper

/**
 * One sync pass: connect to IMAP, look at recent mail, parse for internship
 * opportunities, insert new Event rows, and fire "new mail" notifications
 * for anything we haven't seen before.
 */
object MailSyncEngine {

    private const val TAG = "MailSyncEngine"

    suspend fun syncOnce(context: Context): Int {
        val prefs = SecurePrefs(context)
        if (!prefs.isLoggedIn) return 0

        val dao = AppDatabase.getInstance(context).eventDao()
        val client = ImapClient(prefs)
        var newCount = 0
        try {
            client.connect()
            val mails = client.fetchRecent(40)
            for (mail in mails) {
                val existing = dao.findByMessageId(mail.messageId)
                if (existing != null) continue

                val parsed = EmailParser.tryParse(
                    subject = mail.subject,
                    body = mail.bodyText,
                    fromAddress = mail.fromAddress,
                    fromName = mail.fromName
                ) ?: continue

                val deadline = parsed.deadlineEpochMillis ?: continue // skip if we can't find a deadline

                val event = Event(
                    companyName = parsed.companyName,
                    deadlineEpochMillis = deadline,
                    link = parsed.link,
                    emailMessageId = mail.messageId,
                    emailSubject = mail.subject,
                    emailSnippet = mail.bodyText.take(200),
                    source = EventSource.EMAIL_AUTO
                )
                val id = dao.insert(event)
                AlarmScheduler.scheduleForEvent(context, event.copy(id = id))
                newCount++
                NotificationHelper.showNewOpportunityNotification(
                    context,
                    eventId = id,
                    companyName = parsed.companyName,
                    subject = mail.subject
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
        } finally {
            client.disconnect()
        }
        return newCount
    }
}
