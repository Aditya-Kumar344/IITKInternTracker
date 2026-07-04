package com.internmail.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.internmail.tracker.data.SecurePrefs
import com.internmail.tracker.ui.InternTrackerApp
import com.internmail.tracker.ui.theme.InternTrackerTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        val openEventId = intent?.getLongExtra(EXTRA_EVENT_ID, -1L)?.takeIf { it != -1L }

        setContent {
            InternTrackerTheme {
                InternTrackerApp(
                    isLoggedIn = SecurePrefs(this).isLoggedIn,
                    initialEventId = openEventId
                )
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        const val ACTION_OPEN_EVENT = "com.internmail.tracker.ACTION_OPEN_EVENT"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }
}
