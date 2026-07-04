package com.internmail.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single tracked internship opportunity: which company, when it's due,
 * and a link back to the original email (or any link the user wants to open).
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val companyName: String,

    // Deadline stored as epoch millis (UTC) at the deadline's local midnight-to-midnight day.
    val deadlineEpochMillis: Long,

    val link: String?,

    val emailMessageId: String? = null,   // IMAP Message-ID header, used to open original mail
    val emailSubject: String? = null,
    val emailSnippet: String? = null,

    val source: EventSource = EventSource.EMAIL_AUTO,

    val createdAtEpochMillis: Long = System.currentTimeMillis(),

    // Whether the "new mail" notification has already been shown for this event
    val newMailNotified: Boolean = false,

    // Whether the user has opened this event's link / marked it done
    val opened: Boolean = false,

    // Whether the persistent deadline-day notification is currently active
    val deadlineNotificationActive: Boolean = false
)

enum class EventSource {
    EMAIL_AUTO,   // detected automatically by parsing an email
    MANUAL        // added by hand in the app
}
