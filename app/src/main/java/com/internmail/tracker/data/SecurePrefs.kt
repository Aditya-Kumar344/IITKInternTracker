package com.internmail.tracker.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the user's IITK webmail credentials and IMAP/SMTP server settings
 * locally, encrypted via the Android Keystore. Nothing here is ever sent
 * anywhere except directly to the mail server itself over IMAPS/SMTPS.
 */
class SecurePrefs(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_intern_tracker_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var password: String?
        get() = prefs.getString(KEY_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    // Defaults are IIT Kanpur's Computer Centre IMAP/SMTP hosts.
    // Change here (or expose in Settings UI) if your account uses different hosts,
    // e.g. CSE department mail (cse.iitk.ac.in) or a hall/department server.
    var imapHost: String
        get() = prefs.getString(KEY_IMAP_HOST, "newmailhost.cc.iitk.ac.in") ?: "newmailhost.cc.iitk.ac.in"
        set(value) = prefs.edit().putString(KEY_IMAP_HOST, value).apply()

    var imapPort: Int
        get() = prefs.getInt(KEY_IMAP_PORT, 993)
        set(value) = prefs.edit().putInt(KEY_IMAP_PORT, value).apply()

    var isLoggedIn: Boolean
        get() = !email.isNullOrBlank() && !password.isNullOrBlank()
        set(_) {}

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
        private const val KEY_IMAP_HOST = "imap_host"
        private const val KEY_IMAP_PORT = "imap_port"
    }
}
