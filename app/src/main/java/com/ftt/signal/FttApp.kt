package com.ftt.signal

import android.app.Application
import com.ftt.signal.util.NotificationHelper

class FttApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
    }
}
