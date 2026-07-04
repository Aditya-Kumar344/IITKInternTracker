package com.internmail.tracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.internmail.tracker.MainActivity

object NotificationHelper {

    const val CHANNEL_NEW_MAIL = "new_mail_channel"
    const val CHANNEL_DEADLINE = "deadline_channel"
    const val CHANNEL_SYNC_SERVICE = "sync_service_channel"

    const val SYNC_SERVICE_NOTIFICATION_ID = 9001
    const val DEADLINE_SERVICE_NOTIFICATION_ID_BASE = 20000 // + eventId

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        val newMail = NotificationChannel(
            CHANNEL_NEW_MAIL, "New internship emails", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notifies when a new internship-related email is detected" }

        val deadline = NotificationChannel(
            CHANNEL_DEADLINE, "Deadline reminders", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Persistent reminder on the day an internship deadline is due"
            enableVibration(true)
            setBypassDnd(true)
        }

        val sync = NotificationChannel(
            CHANNEL_SYNC_SERVICE, "Background email watching", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Keeps checking your inbox for new opportunities" }

        nm.createNotificationChannel(newMail)
        nm.createNotificationChannel(deadline)
        nm.createNotificationChannel(sync)
    }

    fun showNewOpportunityNotification(context: Context, eventId: Long, companyName: String, subject: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_EVENT
            putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, eventId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_MAIL)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("New internship opportunity: $companyName")
            .setContentText(subject)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subject))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(eventId.toInt(), notification)
    }

    fun buildSyncServiceNotification(context: Context) =
        NotificationCompat.Builder(context, CHANNEL_SYNC_SERVICE)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Watching your inbox")
            .setContentText("Checking for new internship emails")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    /**
     * Builds the persistent deadline-day reminder. It's posted via a foreground
     * service (see DeadlineForegroundService) with setOngoing(true), which Android
     * will not let the user swipe away while the service is alive — it only goes
     * away when the user taps "Open" (which stops the service) or opens the app
     * and marks the event as opened.
     */
    fun buildDeadlineNotification(context: Context, eventId: Long, companyName: String, link: String?): android.app.Notification {
        val openInAppIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_EVENT
            putExtra(MainActivity.EXTRA_EVENT_ID, eventId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, (eventId + 100000).toInt(), openInAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_OPEN_LINK
            putExtra(NotificationActionReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(NotificationActionReceiver.EXTRA_LINK, link)
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context, (eventId + 200000).toInt(), actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_DEADLINE)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Deadline today: $companyName")
            .setContentText("Tap to open the application link. This reminder stays until you do.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Today is the deadline for $companyName. Tap to open the application link. This reminder stays until you open it."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Open link & dismiss", actionPendingIntent)
            .build()
    }
}
