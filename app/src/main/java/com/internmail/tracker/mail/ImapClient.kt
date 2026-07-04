package com.internmail.tracker.mail

import android.util.Log
import com.internmail.tracker.data.SecurePrefs
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import javax.mail.*
import javax.mail.event.MessageCountEvent
import javax.mail.internet.MimeMultipart
import java.util.*

data class FetchedMail(
    val messageId: String,
    val subject: String,
    val fromAddress: String,
    val fromName: String?,
    val bodyText: String,
    val receivedEpochMillis: Long
)

/**
 * Thin wrapper around JavaMail's IMAP support, configured for IITK webmail
 * (or any standard IMAPS server). Credentials come from SecurePrefs and never
 * leave the device except to authenticate directly against the mail server.
 */
class ImapClient(private val prefs: SecurePrefs) {

    private var store: IMAPStore? = null
    private var folder: IMAPFolder? = null

    @Throws(Exception::class)
    fun connect() {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", prefs.imapHost)
            put("mail.imaps.port", prefs.imapPort.toString())
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.connectiontimeout", "15000")
            put("mail.imaps.timeout", "20000")
        }
        val session = Session.getInstance(props)
        val s = session.store as IMAPStore
        s.connect(prefs.imapHost, prefs.imapPort, prefs.email, prefs.password)
        store = s
        val f = s.getFolder("INBOX") as IMAPFolder
        f.open(Folder.READ_ONLY)
        folder = f
    }

    fun disconnect() {
        try { folder?.close(false) } catch (_: Exception) {}
        try { store?.close() } catch (_: Exception) {}
        folder = null
        store = null
    }

    /** Fetch the most recent [count] messages (newest first). */
    @Throws(Exception::class)
    fun fetchRecent(count: Int = 30): List<FetchedMail> {
        val f = folder ?: throw IllegalStateException("Not connected")
        val total = f.messageCount
        if (total == 0) return emptyList()
        val start = maxOf(1, total - count + 1)
        val messages = f.getMessages(start, total)
        return messages.reversed().mapNotNull { toFetchedMail(it) }
    }

    /**
     * Blocks using IMAP IDLE until the server signals new mail, or until [timeoutMs]
     * elapses (IDLE connections should be refreshed periodically anyway).
     * Returns true if new mail arrived.
     */
    @Throws(Exception::class)
    fun idleWait(timeoutMs: Long = 25 * 60 * 1000): Boolean {
        val f = folder ?: throw IllegalStateException("Not connected")
        var newMailArrived = false
        val listener = MessageCountListener(object : MessageCountAdapter() {
            override fun messagesAdded(e: MessageCountEvent) {
                newMailArrived = true
            }
        })
        f.addMessageCountListener(listener.delegate)
        val idleThread = Thread {
            try {
                f.idle()
            } catch (e: Exception) {
                Log.w("ImapClient", "IDLE ended: ${e.message}")
            }
        }
        idleThread.start()
        idleThread.join(timeoutMs)
        try { f.removeMessageCountListener(listener.delegate) } catch (_: Exception) {}
        return newMailArrived
    }

    private fun toFetchedMail(m: Message): FetchedMail? {
        return try {
            val subject = m.subject ?: ""
            val from = (m.from?.firstOrNull() as? javax.mail.internet.InternetAddress)
            val fromAddress = from?.address ?: ""
            val fromName = from?.personal
            val body = extractText(m) ?: ""
            val msgId = (m as? javax.mail.internet.MimeMessage)?.messageID ?: "$fromAddress-$subject-${m.sentDate?.time}"
            FetchedMail(
                messageId = msgId,
                subject = subject,
                fromAddress = fromAddress,
                fromName = fromName,
                bodyText = body,
                receivedEpochMillis = (m.receivedDate ?: m.sentDate ?: Date()).time
            )
        } catch (e: Exception) {
            Log.w("ImapClient", "Failed to read message: ${e.message}")
            null
        }
    }

    private fun extractText(part: Part): String? {
        return try {
            when {
                part.isMimeType("text/plain") -> part.content as? String
                part.isMimeType("text/html") -> (part.content as? String)?.let { stripHtml(it) }
                part.isMimeType("multipart/*") -> {
                    val mp = part.content as MimeMultipart
                    val sb = StringBuilder()
                    for (i in 0 until mp.count) {
                        extractText(mp.getBodyPart(i))?.let { sb.append(it).append('\n') }
                    }
                    sb.toString()
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
    }

    // Small adapter class kept private; javax.mail's MessageCountListener is an interface
    // with two methods, so we implement both but only care about messagesAdded.
    private class MessageCountListener(private val inner: MessageCountAdapter) {
        val delegate = object : javax.mail.event.MessageCountListener {
            override fun messagesAdded(e: MessageCountEvent) = inner.messagesAdded(e)
            override fun messagesRemoved(e: MessageCountEvent) {}
        }
    }
    private open class MessageCountAdapter {
        open fun messagesAdded(e: MessageCountEvent) {}
    }
}
