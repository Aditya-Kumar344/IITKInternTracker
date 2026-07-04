package com.internmail.tracker

import android.app.Application
import com.internmail.tracker.notification.NotificationHelper

class InternApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
