package com.internmail.tracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.internmail.tracker.data.Event
import java.util.Calendar

object AlarmScheduler {

    /** Schedules an exact alarm to fire at 00:00 on the local day of [event]'s deadline. */
    fun scheduleForEvent(context: Context, event: Event) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = event.deadlineEpochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerAt = cal.timeInMillis
        val now = System.currentTimeMillis()

        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            putExtra(DeadlineAlarmReceiver.EXTRA_EVENT_ID, event.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, event.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (triggerAt <= now) {
            // Deadline day already started (e.g. app just parsed a same-day-deadline email,
            // or device rebooted mid-day) — fire immediately instead of scheduling for the past.
            context.sendBroadcast(intent)
            return
        }

        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (e: SecurityException) {
            // Exact alarm permission not granted on Android 12+: fall back to inexact.
            am.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancelForEvent(context: Context, event: Event) {
        val am = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, DeadlineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, event.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pendingIntent)
    }
}
