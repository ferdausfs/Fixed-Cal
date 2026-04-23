package com.ftt.signal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class FttApp : Application() {

    companion object {
        const val NOTIF_CHANNEL_SIGNALS = "ftt_signals"
        const val NOTIF_CHANNEL_SCAN    = "ftt_scan"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Signal alert channel — HIGH importance so heads-up shows
            nm.createNotificationChannel(
                NotificationChannel(
                    NOTIF_CHANNEL_SIGNALS,
                    "Signal Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "BUY/SELL signal notifications"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 150, 80, 150)
                }
            )

            // Ongoing scan channel — LOW importance (no sound)
            nm.createNotificationChannel(
                NotificationChannel(
                    NOTIF_CHANNEL_SCAN,
                    "Watchlist Scanning",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Background watchlist scan status"
                    setShowBadge(false)
                }
            )
        }
    }
}
